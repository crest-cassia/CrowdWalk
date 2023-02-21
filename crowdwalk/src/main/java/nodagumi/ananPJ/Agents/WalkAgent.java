// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.RoutePlan ;
import nodagumi.ananPJ.misc.Place;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.Agents.Factory.AgentFactory;
import nodagumi.ananPJ.navigation.NavigationHint;

import nodagumi.Itk.*;

/* TODOs:
 * - make each junction a waiting queue
 * - agents change their directions in valid links
 * - agents should not go back to the same path many times
 */

//======================================================================
/**
 * ソーシャルフォースモデルにより歩行するエージェント。
 *
 * <h3> config, fallbackResources に書ける設定 </h3>
 * <pre>
 *  {
 *    "A_0" : __double__, // social force の A_0 (default=0.962)
 *    "A_1" : __double__, // social force の A_1 (default=0.869)
 *    "A_2" : __double__, // social force の A_2 (default=4.682)
 *    "emptySpeed" : __double__, // 自由速度 (default=1.02265769054586)
 *    "personalSpace" : __double__, // 個人スペース。排他領域。 (default=2*0.522)
 *    "widthUnit_SameLane" : __double__, // 同方向流の隣レーンの間隔 (default=0.9)
 *    "widthUnit_OtehrLane" : __double__, // 対向流のレーンまでの距離 (default=0.9)
 *    "insensitiveDistanceInCounterFlow" : __double__, // 対向流の影響範囲  (default=0.522)
 *    "nodeCrossingForceFactor" : __double__, // 前方ノードの横切りからの影響 (default=10.0)
 *    "nodeCrossingForceTimeMargin" : __double__, // 前方ノードの横切り考慮の時間幅 (default=1.5)
 *    "mentalMode" : __string__ // マップの主観的距離の主観モード
 * }
 * </pre>
 */
public class WalkAgent extends AgentBase {

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "WalkAgent" ;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 速度計算の定数。
     *
     * <pre>
     * ソーシャルフォースモデルのパラメータ学習 [2010/9/16 23:53 noda]
     * * 速度誤差最適化 (expF)
     * * 9900 回目の学習結果
     *
     * * c0 = 0.962331091566513      // A_0
     *   c1 = 0.869327852313837      // A_1
     *   c2 = 4.68258910604962       // A_2
     *   vStar = 1.02265769054586    // emptySpeed
     *   rStar = 0.522488010351651   // personalSpace の半分
     *
     * * 傾向：渋滞の状況はなんとなく再現している。ただし、戻りがある。
     *   最高速度 (vStar) は低くなり勝ち。
     *
     * * 位置誤差最適化 (expG)
     *
     * * 9900 回目の学習結果
     *
     * * c0 = 1.97989178714465
     *   c1 = 1.12202742329362
     *   c2 = 0.95466478370757
     *   vStar = 1.24504634565416
     *   rStar = 0.805446866507348
     * </pre>
     * [2017-08-13 I.Noda]
     * emptySpeed が小さいと、social force の方が大きくなりがちである。
     * この事態を避けるために、A_1 を、デフォルトの emptySpeed で正規化し、
     * social force を、emptySpeed に比例させることとする。
     */
    protected static double Fallback_A_0 = 0.962;//1.05;//0.5;
    //protected static double Fallback_A_1_orig = 0.869;//1.25;//0.97;//2.0;
    protected static double Fallback_A_1 = 0.8497467021796484659; // = A_1_orig / EmptySpeed
    protected static double Fallback_A_2 = 4.682;//0.81;//1.5;
    
    protected static double Fallback_EmptySpeed = 1.02265769054586;
    protected static double Fallback_PersonalSpace = 2.0 * 0.522;//0.75;//0.8;

    protected double A_0 = Fallback_A_0 ;
    protected double A_1 = Fallback_A_1 ;
    protected double A_2 = Fallback_A_2 ;
    protected double emptySpeed = Fallback_EmptySpeed;
    protected double personalSpace = Fallback_PersonalSpace ;

    /* 同方向/逆方向のレーンでの単位距離
     * 0.7 だとほとんど進まなくなる。
     * 1.0 あたりか？
     */
    protected static double Fallback_WidthUnit_SameLane = 0.9 ; //0.7;
    protected static double Fallback_WidthUnit_OtherLane = 0.9 ; //0.7;

    protected double widthUnit_SameLane = Fallback_WidthUnit_SameLane ;
    protected double widthUnit_OtherLane = Fallback_WidthUnit_OtherLane ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ノードを直前に交差したエージェントから受ける力を考慮する
     * 時間幅。
     */
    protected double nodeCrossingForceTimeMargin =
        Fallback_NodeCrossingForceTimeMargin ;
    protected static double Fallback_NodeCrossingForceTimeMargin = 1.5 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ノードを直前に交差したエージェントから受ける力の
     * 距離に対する係数。大きいほど力が強くなる。
     * [2016.02.22 I.Noda] 
     * 現状の値は、試行錯誤で適当に決めたもの。
     * 特に根拠はない。
     */
    protected double nodeCrossingForceFactor =
        Fallback_NodeCrossingForceFactor ;
    protected static double Fallback_NodeCrossingForceFactor = 10.0 ;

    /* [2015.01.29 I.Noda]
     *以下は、plain model で使われる。
     */
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Plain モデルで、超過密状態から抜け出すため、
     * 対向流のエージェントで、最低間隔を決めておく。
     * これをある程度大きくしておかないと、
     * 対抗流から過大な力を受け、全く抜け出せなくなる。
     */
    protected static double Fallback_insensitiveDistanceInCounterFlow =
        Fallback_PersonalSpace * 0.5 ;

    protected double insensitiveDistanceInCounterFlow =
        Fallback_insensitiveDistanceInCounterFlow ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンク上の次の位置。
     * 0 以下あるいはリンク長より大きい場合、次のリンクに移っていることになる。
     * advanceNextPlace() でセット。
     * moveToNextPlace() のなかで、setPosition() される。
     */
    protected Place nextPlace = new Place();

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * swing を更新するかどうか（表示用？）
     */
    boolean update_swing_flag = true;//false;

    //============================================================
    /**
     * 速度モデル
     * LaneModel: 前方一人の SocialForce しか考えない。
     * PlainModel: ある程度前方までの順方向・逆方向の SocialForce を計算。
     * CrossingModel: PlainModel に加え、node における交差する人流の force も計算。
     */
    public static enum SpeedCalculationModel {
        LaneModel,
        PlainModel,
        CrossingModel,
    }

	//============================================================
    static private class ChooseNextLinkCache {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /** キャッシュを無視するかどうか */
        public boolean forced = true ;
        /** 現在地のリンク */
        public MapLink currentLink = null ;
        /** 超えようとしているノードの直前のリンク */
        public MapLink link = null ;
        /** 今、超えようとしているノード */
        public MapNode node = null ;

        /* [2015.07.05 I.Noda] 現在のサブゴールを記録しておくべきではないか？*/

        /** 候補となった次のリンク */
        public MapLink resultLink = null ;

        //----------------------------------------
        /** 前回と同じ条件かチェック。 */
        public boolean isSameAsPrevious(WalkAgent agent,
                                        Place passingPlace) {
            boolean isForced = forced ;
            forced = false ;

            Place currentPlace = agent.currentPlace ;

            boolean result =
                (!isForced &&
                 currentLink == currentPlace.getLink() &&
                 link == passingPlace.getLink() &&
                 node == passingPlace.getHeadingNode() &&
                 agent.emptySpeed < currentPlace.getRemainingDistance()) ;

            return result ;
        }

        //----------------------------------------
        /** 前回と同じ条件なら前回の結果を使う。 */
        public MapLink getResultInCache(WalkAgent agent,
                                        Place passingPlace) {
            if(isSameAsPrevious(agent, passingPlace)) {
                return resultLink ;
            } else {
                return null ;
            }
        }

        //----------------------------------------
        /** 条件の記録。 */
        public void recordSituation(WalkAgent agent,
                                    Place passingPlace) {
            Place currentPlace = agent.currentPlace ;
            currentLink = currentPlace.getLink() ;
            link = passingPlace.getLink();
            node = passingPlace.getHeadingNode() ;
        }

        //----------------------------------------
        /** 結果の記録。 */
        public void recordResult(MapLink nextLink) {
            resultLink = nextLink ;
        }

        //----------------------------------------
        /** 結果の破棄。 */
        public void clear() {
            recordResult(null) ;
        }
    }
    
	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
     * chooseNextLink のキャッシュ
     */
    private ChooseNextLinkCache chooseNextLinkCache
        = new ChooseNextLinkCache() ;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * 推論理由を格納。
	 * [I.Noda]
	 * 効率のため、あまりメモリを消費しない方法に切り替え。
	 */
	ReasonTray navigationReason = new ReasonTray() ;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * 主観モード。
	 * 地図の探索の際の、知識や選好性を表すのに用いる。
	 */
	public Term mentalMode = Fallback_MentalMode ;
    protected static Term Fallback_MentalMode =
        NavigationHint.DefaultMentalMode ;

	//############################################################
	/**
	 * 初期化関連
	 */
    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public WalkAgent() {} ;

    //------------------------------------------------------------
    /**
     * 初期化。
     */
    @Override
    public void init(Random _random, EvacuationSimulator simulator,
                     AgentFactory factory, SimTime currentTime,
                     Term fallback) {
        super.init(_random, simulator, factory, currentTime, fallback);
        update_swing_flag = true;
        speed = 0.0 ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        A_0 = getDoubleFromConfig("A_0", A_0) ;
        A_1 = getDoubleFromConfig("A_1", A_1) ;
        A_2 = getDoubleFromConfig("A_2", A_2) ;
        emptySpeed = getDoubleFromConfig("emptySpeed", emptySpeed) ;
        personalSpace = getDoubleFromConfig("personalSpace", personalSpace) ;

        widthUnit_SameLane =
            getDoubleFromConfig("widthUnit_SameLane", widthUnit_SameLane) ;
        widthUnit_OtherLane =
            getDoubleFromConfig("widthUnit_OtherLane", widthUnit_OtherLane) ;
        insensitiveDistanceInCounterFlow =
            getDoubleFromConfig("insensitiveDistanceInCounterFlow",
                                insensitiveDistanceInCounterFlow) ;
        mentalMode =
            getTermFromConfig("mentalMode", mentalMode) ;

        nodeCrossingForceTimeMargin =
            getDoubleFromConfig("nodeCrossingForceTimeMargin",
                                nodeCrossingForceTimeMargin) ;
        nodeCrossingForceFactor =
            getDoubleFromConfig("nodeCrossingForceFactor",
                                nodeCrossingForceFactor) ;
    } ;

    //------------------------------------------------------------
    /**
     *
     */
    @Override
    public NType getNodeType() {
        return NType.AGENT;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public static String getTypeName() {
        return typeString ;
    }

	//############################################################
	/**
	 * 変数アクセス関連
	 */
    //------------------------------------------------------------
    /**
     *
     */
    public double getEmptySpeed() {
        return emptySpeed;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setEmptySpeed(double s) {
        emptySpeed = s;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setA_0(double a) {
        A_0 = a ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setA_1(double a) {
        A_1 = a ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setA_2(double a) {
        A_2 = a ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setPersonalSpace(double s) {
        personalSpace = s ;
    }

    //------------------------------------------------------------
    /**
     * メンタルモードの設定
     * @param mode: メンタルモード。
     */
    public void setMentalMode(Term mode) {
        mentalMode = mode ;
    }

    //------------------------------------------------------------
    /**
     * ゴールを変更。
     * シミュレーション途中でゴールを変更する場合に、
     * 経路のリスケジュールが必要なので、その処理を追加。
     */
    @Override
    public void changeGoal(Term _goal) {
        Term oldGoal = getGoal() ;
        super.setGoal(_goal) ;

        if(!oldGoal.equals(_goal))
            prepareRoutePlan() ;
    }

	//############################################################
	/**
	 * シミュレーションステップ
	 */
    //------------------------------------------------------------
    /**
     * シミュレーション準備
     */
    @Override
    public void prepareForSimulation() {
        if (!isEvacuated()) {
            if(currentPlace.isBeforeStartFromLink()) { // リンクが初期位置
                prepareForSimulation_FromLink() ;
            } else if (currentPlace.isBeforeStartFromNode()) { // ノードが初期位置
                prepareForSimulation_FromNode() ;
            }
            /* speed の初期値を与えられるようにするため、 ここでは初期化せず、
               init で行うようにする。*/
            //speed = 0;
            
            prepareRoutePlan() ;
        }
    }

    //------------------------------------------------------------
    /**
     * シミュレーション準備 (from Link)
     */
    public void prepareForSimulation_FromLink() {
        // [2021.04.09 S.Takami] 一方通行のリンクに配置されている場合は，進行方向は固定．
        if(currentPlace.getLink().isOneWayForward()) {
            currentPlace.setEnteringNode(currentPlace.getFromNode());
            return;
        } else if(currentPlace.getLink().isOneWayBackward()) {
            currentPlace.setEnteringNode(currentPlace.getToNode());
            return;
        }

        // 仮に、forwardDirection と仮定。
        MapNode fromNode = currentPlace.getFromNode();
        currentPlace.setEnteringNode(fromNode) ;
        double costOfForwardDirection =
            calcCostFromPlace(currentPlace, routePlan) ;

        // backwardDirection に変更。
        currentPlace.turnAround() ;
        double costOfBackwardDirection =
            calcCostFromPlace(currentPlace, routePlan) ;

        //もし forward の方が低コストなら、再度 turnAround
        if(costOfForwardDirection < costOfBackwardDirection) {
            currentPlace.turnAround() ;
        } else {
            // do nothing
        }
    }

    //------------------------------------------------------------
    /**
     * シミュレーション準備 (from Node)
     */
    public void prepareForSimulation_FromNode() {
        currentPlace.setAdvancingDistance(0.0) ;
        MapNode startNode = currentPlace.getEnteringNode() ;
        double bestDist = Double.MAX_VALUE ;
        MapLink bestLink = null ;
        for(MapLink link : startNode.getLinks()) {
            currentPlace.setLink(link) ;
            double dist = calcCostFromPlace(currentPlace, routePlan) ;
            if(dist < bestDist) {
                bestLink = link ;
                bestDist = dist ;
            }
        }
        // 進行可能なリンクが一つも見つからない場合はエージェントをスタックさせる
        if(bestLink == null) {
            Itk.logError("currentPlace has no link for routePlan.") ;
            Itk.logError_("currentPlace", currentPlace) ;
            Itk.logError_("routePlan", routePlan) ;
            finalizeEvacuation(SimTime.Zero, false, true) ;
        }
        currentPlace.setLink(bestLink) ;
    }


    //------------------------------------------------------------
    /**
     * preUpdate
     */
    @Override
    public void preUpdate(SimTime currentTime) {
        super.preUpdate(currentTime) ;

        updateSpeed(currentTime) ;

        advanceNextPlace(speed, currentTime, false);
    }

    //------------------------------------------------------------
    /**
     * speed を更新する。同時に、再描画必要かのチェック。
     */
    protected void updateSpeed(SimTime currentTime) {
        double previousSpeed = speed ;
        speed = calcSpeed(previousSpeed, currentTime) ;
        checkSpeedChange(previousSpeed, speed) ;
    }

    //------------------------------------------------------------
    /**
     * speed 変化による再描画必要かのチェック。
     */
    protected void checkSpeedChange(double previousSpeed, double currentSpeed) {
        if (getMap() != null &&
            currentSpeed != previousSpeed &&
            !isEvacuated()) {
            getMap().getNotifier().agentSpeedChanged(this);
        }
    }

    //------------------------------------------------------------
    /**
     * リンク上の表示上の横ずれ幅の量を計算する。
     */
    private void calcSwingWidth() {
        /* agent drawing */
        //TODO should no call here, as lane should be set up properly
        MapLink currentLink = currentPlace.getLink() ;

        /* [2015.07.04 I.Noda]
         * おそらくここでいちいち setup_lanes() を呼ぶのはおかしい。
         * MapLink の update() で処理することにする。
         */
        //currentLink.setup_lanes();

        int w = currentPlace.getLaneWidth() ;
        int index = currentPlace.getIndexInLane(this) ;
        if (isBackwardDirection())
            index = currentPlace.getLane().size() - index;

        if (isForwardDirection()) {
            if (index >= 0) {
                swing_width = (2.0 * ((currentPlace.getLane().size() - index) % w)) / currentLink.getWidth() - 1.0;
            }  else {
                swing_width = 0.0;
            }
        } else {
            if (index >= 0) {
                swing_width = 1.0 - (2.0 * (index % w)) / currentLink.getWidth();
            }  else {
                swing_width = 0.0;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 次の位置を計算。(new) [2015.01.10 I.Noda]
     * ただし、nextPlace での距離を進めるだけ。
     * 次のリンクへは移らず、はみ出した距離ははみ出したまま。
     * @param _speed : 速度に相当する大きさ。単位時間に進める長さ。
     *     [2015.01.10 I.Noda] direction はかかっていないものとする。
     * @param currentTime : 現在時刻
     * @param stayOnLink : 現在のリンクにとどまるかどうか。WaitDirective 用。
     *    true なら、link をはみ出す場合、はみ出さない距離に調整する。
     */
    protected boolean advanceNextPlace(double _speed, SimTime currentTime,
                                       boolean stayOnLink) {
        nextPlace.set(currentPlace) ;

        double distToMove = _speed * currentTime.getTickUnit() ;
        nextPlace.makeAdvance(distToMove, stayOnLink) ;

        return false ;
    }

    //------------------------------------------------------------
    /**
     * currentPlace を nextPlace まで進める。
     * @return 避難完了もしくはスタックした場合に true。それ以外はfalse。
     */
    protected boolean moveToNextPlace(SimTime currentTime) {
        currentPlace.set(nextPlace) ;
        while (!currentPlace.isOnLink()) {

            /* もし、リンクを通り過ぎていて、そのリンクの終点が goal なら
             * 避難完了 */
            if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
                currentPlace.getHeadingNode().hasTag(goal)){
                recordTrail(currentTime, currentPlace, null) ;
                finalizeEvacuation(currentTime, true, false) ;
                return true;
            }

            chooseNextLinkCache.forced = true;
            MapLink nextLink
                = chooseNextLink(currentTime, currentPlace, routePlan, true) ;
            chooseNextLinkCache.forced = true;

            // 進行可能なリンクが見つからなければスタックさせる
            if (nextLink == null) {
                Itk.logInfo("Agent stuck",
                            String.format("%s ID: %s, time: %.1f, linkID: %s",
                                          getTypeName(), this.ID,
                                          currentTime.getRelativeTime(),
                                          currentPlace.getLink().ID)) ;
                finalizeEvacuation(currentTime, false, true) ;
                return true;
            }

            tryToPassNode(currentTime, routePlan, nextLink) ;

            /* もし、渡った先のリンクがゴールなら、避難完了 */
            if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
                currentPlace.getLink().hasTag(goal)){
                finalizeEvacuation(currentTime, true, false) ;
                return true ;
            }
        }

        // 進行可能なリンクが見つからず停止している状態ならばスタックさせる
        if (speed == 0.0) {
            MapLinkTable nextLinkList
                = currentPlace.getHeadingNode().getUsableLinkTable();
            if (nextLinkList.size() == 0) {
                Itk.logInfo("Agent stuck", 
                            String.format("%s ID: %s, time: %.1f, linkID: %s",
                                          getTypeName(), this.ID,
                                          currentTime.getRelativeTime(),
                                          currentPlace.getLink().ID)) ;
                finalizeEvacuation(currentTime, false, true) ;
                return true;
            }
        }

        return false;
    }

    //------------------------------------------------------------
    /**
     * update
     */
    @Override
    public boolean update(SimTime currentTime) {
        /* [2015.01.10 I.Noda] 生成前なら処理しない */
        if (currentTime.isBefore(generatedTime)) {
            return false;
        }

        if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
            getPrevNode().hasTag(goal)){
            finalizeEvacuation(currentTime, true, false) ;
            return true;
        }

        boolean ret = moveToNextPlace(currentTime);
        if(currentPlace.isWalking()) lastPlace.set(currentPlace) ;

        return ret ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    @Override
    public void updateViews() {
        if (update_swing_flag) {
            update_swing_flag = false;
            calcSwingWidth();
        }
        calcSwing();
    }

	//############################################################
	/**
	 * 速度計算関連
	 */
    //------------------------------------------------------------
    /**
     * 速度計算
     */
    protected double calcSpeed(double previousSpeed, SimTime currentTime) {
        double _speed = calcSpeedBody(previousSpeed, currentTime) ;

        /* [2015.01.09 I.Noda]
         * リンクの交通規制など (Gate)
         * [2018.10.18 I.Noda]
         * Link での Gate 制御はやめる。 (MapLink で変更)
         */
        _speed =
            currentPlace.getLink().calcRestrictedSpeed(_speed, this, currentTime) ;

        /* [2015.01.09 I.Noda]
         * リンクを踏破しているなら、headingNode での規制
         * ノードの交通規制など (STOP)
         */
        double deltaDistance = _speed * currentTime.getTickUnit() ;
        //        if (!currentPlace.isBeyondLinkWithAdvance(deltaDistance)) {
        if (currentPlace.isBeyondLinkWithAdvance(deltaDistance)) {
            _speed =
                currentPlace.getHeadingNode().calcRestrictedSpeed(_speed,
                                                                  this,
                                                                  currentTime) ;
        }

        _speed = obstructer.calcAffectedSpeed(_speed) ;

        return _speed ;
    }

    //------------------------------------------------------------
    /**
     * 前方にいるエージェントまでの距離計算
     */
    private double calcDistanceToPredecessor(SimTime currentTime) {
        //前方のエージェントまでの距離の作業変数
        double distToPredecessor = -currentPlace.getAdvancingDistance();
        /* [2015.01.10 I.Noda]
         * 余裕を持って探索するため、長めにとってみる。
         */
        double maxDistance
            = (personalSpace + emptySpeed) * (currentTime.getTickUnit() + 1.0) ;

        //前方のエージェントを探している場所
        Place workingPlace = currentPlace.duplicate() ;
        workingPlace.makeAdvance(maxDistance) ;

        // 現在のリンク中での相対位置
        int indexInLane = workingPlace.getIndexInLane(this) ;
        //作業用の routePlan
        RoutePlan workingRoutePlan = routePlan.duplicate() ;

        while (workingPlace.getAdvancingDistance() > 0) {
            ArrayList<AgentBase> agents = workingPlace.getLane() ;
            int currentWidth = workingPlace.getLaneWidth() ;
            int predecessorIndex = indexInLane + currentWidth ;
            if(agents.size() > 0 && predecessorIndex < agents.size()) {
                // 現在のworkingPlace に前の人がいる場合
                // indexが負の場合は、最後尾の人が直前の人
                if(predecessorIndex < 0) predecessorIndex = 0 ;
                distToPredecessor +=
                    agents.get(predecessorIndex).currentPlace.getAdvancingDistance() ;
                break ;
            } else {
                //次以降のリンクに前の人がいる場合
                distToPredecessor += workingPlace.getLinkLength() ;
                indexInLane -= agents.size() ;
                MapLink nextLink =
                    chooseNextLinkBody(currentTime, workingPlace,
                                       workingRoutePlan, true) ;
                if (nextLink == null) {
                    break;
                }
                workingPlace.transitTo(nextLink) ;
            }
        }
        //最大を超えていれば、それで頭打ち
        if(distToPredecessor > maxDistance) {
            distToPredecessor = maxDistance ;
        }

        return distToPredecessor ;
    }

    //------------------------------------------------------------
    /**
     * lane および plain による速度計算
     */
    private double calcSpeedBody(double previousSpeed, SimTime currentTime) {
            /* base speed */
        double baseSpeed =
            currentPlace.getLink().calcEmptySpeedForAgent(emptySpeed,
                                                          this, currentTime) ;
        //自由速度に向けた加速
        accel = calcAccel(baseSpeed, previousSpeed, currentTime) ;

        //時間積分
        double deltaSpeed = accel * currentTime.getTickUnit();
        double _speed = previousSpeed + deltaSpeed ;

        //速度幅制限
        if (_speed > baseSpeed) {
            _speed = baseSpeed ;
        } else if (_speed < 0) {
            _speed = 0;
        }

        //出口直前の場合で最前列の場合は、最大速にしておく。
        int w = currentPlace.getLaneWidth() ;
        int indexInLane = currentPlace.getIndexFromHeadingInLane(this) ;
        if (indexInLane < w && currentPlace.getHeadingNode().hasTag(goal)) {
            _speed = baseSpeed;
        }

        return _speed ;
    }

    //------------------------------------------------------------
    /**
     * lane および plain による速度計算
     */
    protected double calcAccel(double baseSpeed, double previousSpeed,
                             SimTime currentTime) {
        // 自由速度に向けた加速
        double _accel = A_0 * (baseSpeed - previousSpeed) ;

        // social force による減速
        switch (getSpeedModel()) {
        case LaneModel:
            double distToPredecessor = calcDistanceToPredecessor(currentTime) ;
            _accel += calcSocialForce(distToPredecessor) ;
            break;
        case PlainModel:
        case CrossingModel:
            double lowerBound =
                -((baseSpeed / currentTime.getTickUnit()) + _accel) ;
            _accel += accumulateSocialForces(currentTime, lowerBound) ;
            break;
        default:
            Itk.logError("Unknown Speed Model") ;
            Itk.logError_("speedModel",getSpeedModel()) ;
            break;
        }
        return _accel ;
    }

    //------------------------------------------------------------
    /**
     * 前方エージェントからの social force を集める
     * social force は、進行方向に沿った要素のみを扱う。
     * 横方向距離は、距離計算の際に用いる。
     * 横方向距離の計算は以下の通り。
     *  順方向：エージェントから数えて n 番目のエージェントは、
     *          ((w - (n % w)) % w) * u 横にいるとする。
     *          つまり、w(レーン幅) = 3 の時、
     *          1,2,3,4,5 番目のエージェントの横ずれ幅は、
     *          2u, 1u, 0, 2u, 1u となる。
     *  逆方向：エージェントから数えて n 番目のエージェントは、
     *          ((w - (n % w)) % w + 1) * u 横にいるとする。
     *          つまり、w(レーン幅) = 3 の時、
     *          1,2,3,4,5 番目のエージェントの横ずれ幅は、
     *          3u, 2u, 1u, 3u, 2u となる。
     * @param currentTime : 時刻
     * @param lowerBound : 計算上の力の下限。余計な計算しないために。
     * @return 力
     */
    private double accumulateSocialForces(SimTime currentTime, double lowerBound) {
        //求める力
        double totalForce = 0.0 ;

        //探す範囲
        double maxDistance
            = (personalSpace + emptySpeed) * (currentTime.getTickUnit() + 1.0) ;

        //作業用の場所と経路計画
        Place workingPlace = currentPlace.duplicate() ;
        RoutePlan workingRoutePlan = routePlan.duplicate() ;

        //当該エージェントの位置（注目しているリンクからの相対位置）
        double relativePos = workingPlace.getAdvancingDistance() ;

        //探索範囲終端まで場所を進めておく
        workingPlace.makeAdvance(maxDistance) ;

        //当該エージェントからのカウント
        int count = 0 ; //(順方向)
        int countOther = 0 ; //(逆方向)

        //探索開始
        while(workingPlace.getAdvancingDistance() > 0) {
            if(totalForce < lowerBound) { break ; }

            //逆方向探索
            ArrayList<AgentBase> otherLane = workingPlace.getOtherLane() ;
            int laneWidthOther = workingPlace.getOtherLaneWidth() ;
            double linkLength = workingPlace.getLinkLength() ;
            double insensitivePos = 0.0 ;
            for(int i = 0 ; i < otherLane.size() ; i++) {
                if(totalForce < lowerBound) { break ; }
                AgentBase agent = otherLane.get(otherLane.size() - i - 1);
                double agentPos = linkLength - agent.getAdvancingDistance() ;
                if(agentPos > workingPlace.getAdvancingDistance()) {
                    // 探索範囲外
                    break ; // for からの脱出
                } else if(agentPos <= relativePos) {
                    // 当該エージェントの後方なので無視
                    continue  ; // 次の for へ
                } else if(agentPos <= insensitivePos) {
                    // 直前のエージェントに近すぎる。（超過密状態用）
                    continue ; // 次の for へ
                } else {
                    countOther++ ;
                    double dx = agentPos - relativePos ;
                    double dy =
                        widthUnit_OtherLane *
                        (((laneWidthOther - (countOther % laneWidthOther))
                          % laneWidthOther) + 1) ;
                    double force = calcSocialForceToHeading(dx,dy) ;
                    totalForce += force ;
                    if(countOther % laneWidthOther == 0) {
                        insensitivePos = (agentPos +
                                          insensitiveDistanceInCounterFlow) ;
                    }
                }
            }

            //順方向探索
            ArrayList<AgentBase> sameLane = workingPlace.getLane() ;
            int laneWidth = workingPlace.getLaneWidth() ;
            boolean myTernIsOver = false ;
            for(AgentBase agent : sameLane) {
                if(totalForce < lowerBound) { break ; }
                double agentPos = agent.getAdvancingDistance() ;
                if(agent == this) {
                    myTernIsOver = true ;
                    continue ;
                } else if(agentPos > workingPlace.getAdvancingDistance()) {
                    // 探索範囲外
                    break ; // for からの脱出
//              } else if(agentPos <= relativePos) {
                } else if(agentPos < relativePos) { // 同一の場合は次で判断
                    // 当該エージェントの後方なので無視
                    continue ; // 次の for へ
                } else if(agentPos == relativePos && myTernIsOver) {
                    // 同一位置で、かつ、自分より順番が後ろの場合は無視。
                    // （つまり、同一位置で自分が先頭の時のみ、影響受けない）
                    continue ;
                } else {
                    count++ ;
                    double dx = agentPos - relativePos ;
                    double dy =
                        (widthUnit_SameLane *
                         ((laneWidth - (count % laneWidth)) % laneWidth)) ;
                    double force = calcSocialForceToHeading(dx,dy) ;
                    totalForce += force ;
                }
            }
            //次のリンクへ進む準備。
            relativePos -= workingPlace.getLinkLength() ;
            MapLink nextLink =
                chooseNextLinkBody(currentTime, workingPlace,
                                   workingRoutePlan, true) ;
            if (nextLink == null) {
                break;
            }
            //（直前のターンで）次のノードを交差して渡っている人の影響
            if(getSpeedModel() == SpeedCalculationModel.CrossingModel) {
                totalForce +=
                    calcNodeCrossingForce(currentTime,
                                          workingPlace.getLink(),
                                          nextLink,
                                          workingPlace.getHeadingNode(),
                                          -relativePos) ;
            }

            // 次のリンクへ乗り移り。
            workingPlace.transitTo(nextLink) ;
        }

        return totalForce ;
    }

    //------------------------------------------------------------
    /**
     * social force
     * @param dist : 他のエージェントまでの距離
     * @return 力
     */
    protected double calcSocialForce(double dist) {
        return - emptySpeed * A_1  * Math.exp(A_2 * (personalSpace - dist)) ;
    }

    //------------------------------------------------------------
    /**
     * social force のうち、進行方向に沿った力のみを計算する。
     * @param dx : 進行方向に沿った距離
     * @param dy : 横方向の距離
     * @return x 方向の力
     */
    protected double calcSocialForceToHeading(double dx, double dy) {
        double dist = Math.sqrt(dx * dx + dy * dy) ;
        double force = calcSocialForce(dist) ;
        if(dist == 0.0) {
            return force ;
        } else {
            return force * (dx / dist) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 向かっている交差点の交差する人流から受ける social force。
     * @param currentTime : 現在時刻。
     * @param node : 交差点。
     * @param distance : 交差点までの距離
     * @return social force。
     */
    public double calcNodeCrossingForce(SimTime currentTime,
                                        MapLink fromLink,
                                        MapLink toLink,
                                        MapNode node,
                                        double distance) {
        RingBuffer<MapNode.PassingAgentRecord> buffer =
            node.getPassingAgentRecordBuffer() ;
        // 前方、バッファがなければ、まだ誰も交差してないので、
        // おしまい。
        if( buffer == null) { return 0.0 ; } 

        // バッファを順にチェック。
        double force = 0.0 ; /* force の小計 */
        double dw = MapLink.dWidth ; /* 道幅を考慮するときの、
                                          距離の減少分 */
        for(MapNode.PassingAgentRecord record : buffer) {
            if(currentTime.calcDifferenceFrom(record.time)
               > nodeCrossingForceTimeMargin) {
                // 時間マージンを過ぎていたら、それ以降無視。
                break ;
            }
            if(record.isCrossing(fromLink, toLink)) {
                double dCross = 
                    ((record.fromLink.getWidth() + record.toLink.getWidth()) /
                     2.0) ; /* 交差する道の両幅の平均 */
                /* 交差分だけ幅減少 */
                dCross -= dw ;
                dCross /= nodeCrossingForceFactor ;
                dw += MapLink.dWidth ; /* 交差した分を増やす */
                if(dCross < 0.0) { dCross = 0.0 ; } ; /* 負の値は避ける */
                double dist = distance + dCross ;
                force += calcSocialForce(dist) ;
            }
        }
        return force ;
    }
                                        
	//############################################################
	/**
	 * 移動計画及び移動
	 */
    //------------------------------------------------------------
    /**
     * try to pass a node, and enter next link
     * change: navigationReason, route, prev_node, previous_link,
     * current_link, position, evacuated, link.agentExists
     */
    protected boolean tryToPassNode(SimTime currentTime,
                                    RoutePlan workingRoutePlan,
                                    MapLink nextLink) {
        /* [2014.12.19 I.Noda]
         * NaiveAgent への経路記録の入り口のため,
         * recordTrail を導入。
         */
        recordTrail(currentTime, currentPlace, nextLink) ;

        MapNode passingNode = currentPlace.getHeadingNode();
        MapLink previousLink = currentPlace.getLink() ;
        Direction direction_orig = currentPlace.getDirection() ;

        /* agent exits the previous link */
        currentPlace.getLink().agentExits(this);
        // passingNode から出て行ったエージェント数のカウント
        currentPlace.getLink().incrementPassCounter(passingNode, false);

        /* transit to new link */
        currentPlace.transitTo(nextLink) ;
        calcNextTarget(passingNode, workingRoutePlan, false) ;
        /* register agent to new link */
        currentPlace.getLink().agentEnters(this);
        // passingNode から入ってきたエージェント数のカウント
        currentPlace.getLink().incrementPassCounter(passingNode, true);
        /* record agent pass the passing node */
        passingNode.recordPassingAgent(currentTime, this,
                                       previousLink, nextLink) ;

        //update_swing_flag = true;
        //2011年6月7日修正
        /*
         * この部分の修正では、歩行者がリンクの変更を伴う移動をおこなう場合に
         * swing_width を変更するか、しないかの処理を変更しています。
         * 従来はリンクの変更をする場合には、必ずswing_widthを更新していました。
         * そのため、ある歩行者がリンクに流入する際に、連続する歩行者が同一のレーンを移動するという
         * 不自然な描画が発生しています。(本来は異なるレーンで平行して移動するように描画されるべき)
         * 修正後は、
         * 条件分岐文1　現在のリンクと移動先のリンクの幅が同じ(レーン数が同じ)、他のリンクの流入出がない、移動先のリンクが出口を含む
         * 条件分岐文2　現在のリンクと移動先のリンクの幅が同じ(レーン数が同じ)、他のリンクの流入出がない
         * という二つの条件を満たした場合、swing_width の更新をおこないません。
         * この修正によって、swing_width が更新されないため、不自然な描画の発生は防がれています。
         */
        if (currentPlace.getLink().getWidth() == previousLink.getWidth() &&
            passingNode.getUsableLinkTable().size() == 2) {
            if (direction_orig != getDirection()) { swing_width *= -1; }
            update_swing_flag = false;
        } else {
            update_swing_flag = true;
        }

        return true;
    }

    //------------------------------------------------------------
    /**
     * 最終決定したルート、足跡情報の記録
     * [2014.12.19 I.Noda] tryToPassNode() より移動
     */
    protected void recordTrail(SimTime currentTime, Place passingPlace,
                               MapLink nextLink) {
        if(doesRecordTrail()) {
            if(getHandler().agentTrailLogHasTrailAux()) {
                HashMap<String, Object> auxInfo =
                    getHandler()
                    .getAgentTrailLogTrailAuxFormatter()
                    .outputRecordToNewHashMap(this, currentTime,
                                              passingPlace, nextLink) ;
                trail.add(currentTime, passingPlace.getTrailContent(nextLink,
                                                                    auxInfo)) ;
            } else {
                trail.add(currentTime, passingPlace.getTrailContent(nextLink)) ;
            }
        }
    }

	//############################################################
	/**
	 * 経路計画
	 */
    //------------------------------------------------------------
    /**
     * ノード上で、次の道を選ぶ。
     * 実体は chooseNextLinkBody() だが、
     * 候補が1つしか無い場合などのショートカットと、
     * エラー処理・例外処理を行う。
     */
    protected MapLink chooseNextLink(SimTime currentTime,
                                     Place passingPlace,
                                     RoutePlan workingRoutePlan,
                                     boolean on_node) {
        final MapLinkTable candidateLinkList
            = passingPlace.getHeadingNode().getUsableLinkTable();

        /* trapped? */
        if (candidateLinkList.size() == 0) {
            Itk.logTrace("Agent trapped!");
            return null;
        }

        /* only one way to choose */
        if (candidateLinkList.size() == 1) {
            return candidateLinkList.get(0);
        }

        /* if not in navigation mode, go back to path */
        /* [2015.01.04 I.Noda]
         * Agent の goal が失われることはないはずなので、
         * エラーで落ちるようにしておく。
         */
        if (goal == null) {
            Itk.logError("An agent lost its goal.") ;
            Itk.logError_("agent.ID", this.ID) ;
            Itk.quitByError();
        }

        MapLink nextLink =
            chooseNextLinkBody(currentTime, passingPlace,
                               workingRoutePlan, on_node) ;

        // もし target が見つかっていなかったら、ランダムに選ぶ。
        if(nextLink == null) {
            nextLink =
                candidateLinkList.get(random
                                      .nextInt(candidateLinkList.size())) ;
        }

        return nextLink ;
    }

    //------------------------------------------------------------
    /**
     * 新しい routePlan の準備。
     * 一度 calcNextTarget を呼び出しておいて、
     * ゴール・サブゴールなどのチェックを行う。
     * すでに達成しているサブゴールなどはスキップ。
     */
    protected void prepareRoutePlan() {
        if (goal != null){
            calcNextTarget(currentPlace.getHeadingNode(),
                           routePlan, false);
        }
    }

    //------------------------------------------------------------
    /**
     * ノードにおいて、次の道を選択するルーチン
     */
    protected MapLink chooseNextLinkBody(SimTime currentTime,
                                         Place passingPlace,
                                         RoutePlan workingRoutePlan,
                                         boolean on_node) {
        // 前回の呼び出し時と同じ結果になる場合は不要な処理を回避する
        MapLink nextLink
            = chooseNextLinkCache.getResultInCache(this, passingPlace) ;
        if(nextLink != null) { return nextLink ; }

        chooseNextLinkCache.recordSituation(this, passingPlace) ;

        final Term nextTarget =
            calcNextTarget(passingPlace.getHeadingNode(),
                           workingRoutePlan, on_node) ;
        navigationReason.clear().add("for").add(nextTarget).add("\n");

        MapLinkTable candidateLinkList =
            passingPlace.getHeadingNode().getUsableLinkTable();
        double nextLinkCost = Double.MAX_VALUE;
        MapLinkTable nextLinksWithSameCost = null;

        for (MapLink link : candidateLinkList) {
            /* ゴールもしくは経由点のチェック。あるいは、同じ道を戻らない */
            if (workingRoutePlan.isEmpty() && link.hasTag(goal)) {
                /* finishing up */
                nextLink = link ;
                nextLinkCost = 0.0 ;
                navigationReason.add("found goal").add(goal);
                break;
            } else if (link.hasTag(nextTarget)) {
                /* reached sub goal */
                nextLink = link ;
                nextLinkCost = 0.0 ;
                navigationReason.add("found mid-goal in").add(link) ;
                if(!isKnownDirective(workingRoutePlan.top())) {
                    // directive でない場合のみ shiftする。
                    // そうでなければ、directive の操作待ち。
                    workingRoutePlan.shift() ;
                }
                break;
            }

            // 現在の link を選択した場合の nextTarget までのコスト計算
            try {
                double cost =
                    calcCostFromNodeViaLink(link,
                                            passingPlace.getHeadingNode(),
                                            nextTarget) ;
                if (cost < nextLinkCost) { // 最小cost置き換え
                    nextLink = link ;
                    nextLinkCost = cost;
                    nextLinksWithSameCost = null ;
                } else if (cost == nextLinkCost) { // 最小コストが同じ時の処理
                    if (nextLinksWithSameCost == null) {
                        nextLinksWithSameCost = new MapLinkTable();
                        nextLinksWithSameCost.add(nextLink) ;
                    }
                    nextLinksWithSameCost.add(link) ;
                }
            } catch(TargetNotFoundException e) {
                // この way_candidate からは next_target にたどり着けない
                Itk.logTrace(e.getMessage());
                continue;
            }
        }

        if (nextLinksWithSameCost != null && nextLinksWithSameCost.size()>0) {
            nextLink
                = nextLinksWithSameCost.get(random.
                                           nextInt(nextLinksWithSameCost.size())) ;
        }

        if (nextLink != null) {
            navigationReason
                .add("\n -> chose")
                .add(nextLink.getOther(passingPlace.getHeadingNode())) ;
        }

        chooseNextLinkCache.recordResult(nextLink) ;

        return nextLink;
    }

    //------------------------------------------------------------
    /**
     * chooseNextLinkCache のクリア。
     * (for RationalAgent and RubyAgent)
     *
     */
    public void clearNextLinkCache() {
        chooseNextLinkCache.clear() ;
    }
    
    //------------------------------------------------------------
    /**
     * 指定された RoutePlan で、次のターゲットを得る。
     * @param node : このノードにおけるターゲットを探す。
     * @param workingRoutePlan : 指定された RoutePlan。shiftする可能性がある。
     * @return : workingRoutePlan の index 以降の次のターゲット、もしくは goal。
     * [2014.12.30 I.Noda] analysis
     * 次に来る、hint に記載されている route target を取り出す。
     * 今、top の target が現在のノードのタグにある場合、
     * route は１つ進める。
     */
    protected Term calcNextTarget(MapNode node,
                                  RoutePlan workingRoutePlan,
                                  boolean on_node) {
        if (on_node && !workingRoutePlan.isEmpty() &&
            node.hasTag(workingRoutePlan.top())){
            /* [2015.01.10 I.Noda] memo
             * on_node で、そのノードが subgoal なら、
             * routePlan をシフトして再度探索。
             * 2つ以上 subgoal を消化する場合を考え、再帰呼び出し。
             */
            workingRoutePlan.shift() ;
            return calcNextTarget(node, workingRoutePlan, on_node) ;
        } else {
            /* [2015.01.10 I.Noda] memo
             * routePlan のトップが、現在のノードの hint に入っているかどうか
             * 確認する。
             * 疑問：routePlan に directive が入っていても大丈夫か？
             */
            while (!workingRoutePlan.isEmpty()) {
                Term subgoal = nakedTargetFromRoutePlan(workingRoutePlan) ;
                if (node.hasTag(subgoal)) {
                    workingRoutePlan.shift() ;
                } else {
                    confirmCheckedRouteKey(subgoal, true) ;
                    if (node.hasHint(mentalMode, subgoal)) {
                        return subgoal;
                    } else {
                        Itk.logWarn("no sub-goal hint for " + subgoal);
                        workingRoutePlan.shift() ;
                    }
                }
            }
            return goal ;
        }
    }

    //------------------------------------------------------------
    /**
     * ある _node においてあるwayを選択した場合の目的地(_target)までのコスト。
     * ここを変えると、経路選択の方法が変えられる。
     */
    public double calcCostFromNodeViaLink(MapLink _link, MapNode _node,
                                          Term _target)
        throws TargetNotFoundException
    {
        /* [2015.04.14 I.Noda]
         * もし新しい target なら、経路探査する。
         * [2016.01.31 I.Noda]
         * 本来なら、mentalMode ごとに探査すべきかもしれない。
         * そのためには、isCheckedRouteKey の拡張が必要。
         */
        confirmCheckedRouteKey(_target, true) ;

        MapNode other = _link.getOther(_node);
        double cost = other.getDistance(mentalMode, _target) ;
        cost += _link.getMentalLength(mentalMode, _node);
        return cost ;
    }

    //------------------------------------------------------------
    /**
     * 次のターゲットがすでにルート探索されているかどうかのチェックし、
     * されていなければ、指定により、探索する。
     * @param _target : 次のターゲット。
     * @param _calcP : true なら、探索する。
     * @return すでに探索していたら true。していなければ、false。
     */

    public boolean confirmCheckedRouteKey(Term _target, boolean _calcP) {
        String targetTag = _target.getString() ;
        if(!getMap().isCheckedRouteKey(targetTag)) {
            if(_calcP) {
                Itk.logInfo("New Target", "find path.", "tag=", targetTag) ;
                getMap().calcGoalPathAllWithSync(targetTag) ;
            } else {
                Itk.logWarn("Unknown Target", "tag=", targetTag) ;
            }
            return false ;
        } else {
            return true ;
        }
    }
     
    //------------------------------------------------------------
    /**
     * あるplaceから現在のroutePlanの次の目的地までのコスト。
     * @param _place : 現在地を示す Place
     * @param _routePlan : 現在の経路計画。保存される。
     * @return コスト<br>次の目的地へのルートが見つからない場合は Double.MAX_VALUE を返す
     */
    protected double calcCostFromPlace(Place _place,
                                       final RoutePlan _routePlan) {
        RoutePlan workingRoutePlan = _routePlan.duplicate() ;
        Term target = calcNextTarget(_place.getHeadingNode(),
                                     workingRoutePlan,false) ;
        double costFromEnteringNode;
        try {
            costFromEnteringNode =
                calcCostFromNodeViaLink(_place.getLink(),
                                        _place.getEnteringNode(), target) ;
        } catch(TargetNotFoundException e) {
            Itk.logTrace(e.getMessage());
            return Double.MAX_VALUE;
        }
        double costFromPlace =
            costFromEnteringNode - _place.getAdvancingDistance() ;
        return costFromPlace ;
    }

    //------------------------------------------------------------
    /**
     * AgentFactory の individualConfig によりエージェントを設定。
     * <br>
     * Format:
     * <pre>
     *   { 
     *     ...
     *     "emptySpeed" : __SpeedValue__,
     *     "speed" : __SpeedValue_,
     *     "mentalModel" : __MentalModel__,
     *     ...
     *   }
     * </pre>
     */
    public void setupByIndividualConfig(Term config) {
        super.setupByIndividualConfig(config) ;
        emptySpeed =
            SetupFileInfo.fetchFallbackDouble(config,
                                              "emptySpeed",
                                              emptySpeed) ;
        //Itk.dbgVal("emptySpeed", emptySpeed) ;
        speed =
            SetupFileInfo.fetchFallbackDouble(config,
                                              "speed",
                                              speed) ;
        //Itk.dbgVal("speed", speed) ;
        mentalMode =
            SetupFileInfo.fetchFallbackTerm(config,
                                            "mentalMode",
                                            mentalMode) ;
        //Itk.dbgVal("mentalMode", mentalMode) ;
    }

    //------------------------------
    /**
     * individualConfig 用にエージェント状態をTermにdump。
     * Format は、{@link #setupByIndividualConfig(Term)} に準拠。 
     */
    public Term dumpTermForIndividualConfig(SimTime startTime) {
        Term config = super.dumpTermForIndividualConfig(startTime) ;
        
        config.setArg("emptySpeed", emptySpeed) ;
        config.setArg("speed", speed) ;
        config.setArg("mentalMode", mentalMode) ;
            
        return config ;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
