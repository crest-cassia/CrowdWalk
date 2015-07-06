// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Vector3d;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.RoutePlan ;
import nodagumi.ananPJ.misc.Place;
import nodagumi.ananPJ.Agents.AgentFactory;

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
 *    "A_0" : __double__, // social force の A_0
 *    "A_1" : __double__, // social force の A_1
 *    "A_2" : __double__, // social force の A_2
 *    "emptySpeed" : __double__, // ??
 *    "personalSpace" : __double__, // 個人スペース。排他領域。
 *    "widthUnit_SameLane" : __double__, // 同方向流の隣レーンの間隔
 *    "widthUnit_OtehrLane" : __double__, // 対向流のレーンまでの距離
 *    "insensitiveDistanceInCounterFlow" : __double__ // 対向流の影響範囲
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
     */
    protected static double FallBack_A_0 = 0.962;//1.05;//0.5;
    protected static double FallBack_A_1 = 0.869;//1.25;//0.97;//2.0;
    protected static double FallBack_A_2 = 4.682;//0.81;//1.5;
    protected static double FallBack_EmptySpeed = 1.02265769054586;
    protected static double FallBack_PersonalSpace = 2.0 * 0.522;//0.75;//0.8;

    protected double A_0 = FallBack_A_0 ;
    protected double A_1 = FallBack_A_1 ;
    protected double A_2 = FallBack_A_2 ;
    protected double emptySpeed = FallBack_EmptySpeed;
    protected double personalSpace = FallBack_PersonalSpace ;

    /* 同方向/逆方向のレーンでの単位距離
     * 0.7 だとほとんど進まなくなる。
     * 1.0 あたりか？
     */
    protected static double FallBack_WidthUnit_SameLane = 0.9 ; //0.7;
    protected static double FallBack_WidthUnit_OtherLane = 0.9 ; //0.7;

    protected double widthUnit_SameLane = FallBack_WidthUnit_SameLane ;
    protected double widthUnit_OtherLane = FallBack_WidthUnit_OtherLane ;

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
    protected static double FallBack_insensitiveDistanceInCounterFlow =
        FallBack_PersonalSpace * 0.5 ;

    protected double insensitiveDistanceInCounterFlow =
        FallBack_insensitiveDistanceInCounterFlow ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 速度計算関係
	 * time_scale : シミュレーションステップ
	 */
    protected double time_scale = 1.0;//0.5; simulation time step

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

    private double lastSpeed = 0.0;     // 更新チェック用

    //============================================================
    /**
     * 速度モデル
     */
    public static enum SpeedCalculationModel {
        LaneModel,
        PlainModel,
    }

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 速度モデル
     */
    private SpeedCalculationModel calculation_model =
        SpeedCalculationModel.LaneModel;

    //============================================================
    /**
     * 経由点の通過情報
     */
    class CheckPoint {
        public MapNode node;
        public double time;
        public String reason;
        public CheckPoint(MapNode _node, double _time, String _reason) {
            node = _node; time = _time; reason = _reason;
        }
    }

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 経由点の通過情報
     */
    protected ArrayList<CheckPoint> route;


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
                     AgentFactory factory, double time) {
        super.init(_random, simulator, factory, time);
        update_swing_flag = true;
        route = new ArrayList<CheckPoint>();
        setSpeedCalculationModel(factory.speedModel) ;
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
    public SpeedCalculationModel getSpeedCalculationModel() {
        return calculation_model;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setSpeedCalculationModel(SpeedCalculationModel _model) {
        calculation_model = _model;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setTimeScale(double _time_scale) {
        time_scale = _time_scale;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public double getTimeScale() {
        return time_scale;
    }

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
    public void prepareForSimulation(double _timeScale) {
        if (!isEvacuated()) {
            if(currentPlace.isBeforeStartFromLink()) { // リンクが初期位置
                prepareForSimulation_FromLink() ;
            } else if (currentPlace.isBeforeStartFromNode()) { // ノードが初期位置
                prepareForSimulation_FromNode() ;
            }
            speed = 0;

            prepareRoutePlan() ;
        }
    }

    //------------------------------------------------------------
    /**
     * シミュレーション準備 (from Link)
     */
    public void prepareForSimulation_FromLink() {
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
            finalizeEvacuation(0, false, true) ;
        }
        currentPlace.setLink(bestLink) ;
    }


    //------------------------------------------------------------
    /**
     * preUpdate
     */
    @Override
    public void preUpdate(double time) {
        super.preUpdate(time) ;
        calcSpeed(time);
        advanceNextPlace(speed, time, false);
    }

    //------------------------------------------------------------
    /**
     * set_swing
     */
    private void set_swing() {
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

        if (isForwardDirection()) {
            if (index >= 0) {
                swing_width = (2.0 * (index % w)) / currentLink.width - 1.0;
            }  else {
                swing_width = 0.0;
            }
        } else {
            if (index >= 0) {
                swing_width = 1.0 - (2.0 * (index % w)) / currentLink.width;
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
     * @param d : speed に相当する大きさ。単位時間に進める長さ。
     *     [2015.01.10 I.Noda] direction はかかっていないものとする。
     * @param time : 時間ステップ。1.0 が1秒。
     * @param stayOnLink : 現在のリンクにとどまるかどうか。WaitDirective 用。
     *    true なら、link をはみ出す場合、はみ出さない距離に調整する。
     */
    protected boolean advanceNextPlace(double d, double time, boolean stayOnLink) {
        nextPlace.set(currentPlace) ;
        double distToMove = d * time_scale ;
        nextPlace.makeAdvance(distToMove, stayOnLink) ;
        return false ;
    }

    //------------------------------------------------------------
    /**
     * currentPlace を nextPlace まで進める。
     * @return 避難完了もしくはスタックした場合に true。それ以外はfalse。
     */
    protected boolean moveToNextPlace(double time) {
        currentPlace.set(nextPlace) ;
        while (!currentPlace.isOnLink()) {

            /* もし、リンクを通り過ぎていて、そのリンクの終点が goal なら
             * 避難完了 */
            if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
                currentPlace.getHeadingNode().hasTag(goal)){
                finalizeEvacuation(time, true, false) ;
                return true;
            }

            chooseNextLinkCache.forced = true;
            MapLink nextLink
                = chooseNextLink(time, currentPlace, routePlan, true) ;
            chooseNextLinkCache.forced = true;

            // 進行可能なリンクが見つからなければスタックさせる
            if (nextLink == null) {
                Itk.logInfo("Agent stuck",
                            String.format("%s ID: %s, time: %.1f, linkID: %s",
                                          getTypeName(), this.ID, time,
                                          currentPlace.getLink().ID)) ;
                finalizeEvacuation(time, false, true) ;
                return true;
            }

            tryToPassNode(time, routePlan, nextLink) ;

            /* もし、渡った先のリンクがゴールなら、避難完了 */
            if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
                currentPlace.getLink().hasTag(goal)){
                finalizeEvacuation(time, true, false) ;
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
                                          getTypeName(), this.ID, time,
                                          currentPlace.getLink().ID)) ;
                finalizeEvacuation(time, false, true) ;
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
    public boolean update(double time) {
        /* [2015.01.10 I.Noda] 生成前なら処理しない */
        if (time < generatedTime) {
            return false;
        }

        if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
            getPrevNode().hasTag(goal)){
            finalizeEvacuation(time, true, false) ;
            return true;
        }

        boolean ret = moveToNextPlace(time);
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
            set_swing();
        }
    }

	//############################################################
	/**
	 * 速度計算関連
	 */
    //------------------------------------------------------------
    /**
     * 速度計算
     */
    protected void calcSpeed(double time) {
        switch (calculation_model) {
        case LaneModel:
            calcSpeedLaneGeneric(time,calculation_model) ;
            break;
        case PlainModel:
            calcSpeedLaneGeneric(time,calculation_model);
            break;
        default:
            Itk.logError("Unknown Speed Model") ;
            Itk.logError_("calculation_model",calculation_model) ;
            break;
        }

        /* [2015.01.09 I.Noda]
         * リンクの交通規制など (Gate)
         */
        speed = currentPlace.getLink().calcRestrictedSpeed(speed, this, time) ;

        /* [2015.01.09 I.Noda]
         * リンクを踏破しているなら、headingNode での規制
         * ノードの交通規制など (STOP)
         */
        if (!currentPlace.isBeyondLinkWithAdvance(speed)) {
            speed = currentPlace.getHeadingNode().calcRestrictedSpeed(speed,
                                                                      this,
                                                                      time) ;
        }

        obstructer.effect() ;

        if (networkMap != null && speed != lastSpeed) {
            if (! isEvacuated()) {
                networkMap.getNotifier().agentSpeedChanged(this);
            }
            lastSpeed = speed;
        }
    }

    //------------------------------------------------------------
    /**
     * 前方にいるエージェントまでの距離計算
     */
    private double calcDistanceToPredecessor(double time) {
        //前方のエージェントまでの距離の作業変数
        double distToPredecessor = -currentPlace.getAdvancingDistance();
        /* [2015.01.10 I.Noda]
         * 余裕を持って探索するため、長めにとってみる。
         */
        double maxDistance = (personalSpace + emptySpeed) * (time_scale + 1.0) ;

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
                    chooseNextLinkBody(time, workingPlace,
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
    private void calcSpeedLaneGeneric(double time,
                                      SpeedCalculationModel model) {
        /* base speed */
        double baseSpeed =
            currentPlace.getLink().calcEmptySpeedForAgent(emptySpeed,
                                                          this, time) ;
        //自由速度に向けた加速
        accel = A_0 * (baseSpeed - speed) ;
        //social force による減速
        switch (model) {
        case LaneModel:
            double distToPredecessor = calcDistanceToPredecessor(time) ;
            accel += calcSocialForce(distToPredecessor) ;
            break;
        case PlainModel:
            double lowerBound = -((baseSpeed / time_scale) + accel) ;
            accel += accumulateSocialForces(time, lowerBound) ;
            break;
        default:
            Itk.logError("Unknown Speed Model") ;
            Itk.logError_("calculation_model",model) ;
            break;
        }

        //時間積分
        accel *= time_scale;
        speed += accel;

        //速度幅制限
        if (speed > baseSpeed) {
            speed = baseSpeed ;
        } else if (speed < 0) {
            speed = 0;
        }

        //出口直前の場合で最前列の場合は、最大速にしておく。
        int w = currentPlace.getLaneWidth() ;
        int indexInLane = currentPlace.getIndexFromHeadingInLane(this) ;
        if (indexInLane < w && currentPlace.getHeadingNode().hasTag(goal)) {
            speed = baseSpeed;
        }
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
     * @param time : 時刻
     * @param lowerBound : 計算上の力の下限。余計な計算しないために。
     * @return 力
     */
    private double accumulateSocialForces(double time, double lowerBound) {
        //求める力
        double totalForce = 0.0 ;

        //探す範囲
        double maxDistance = (personalSpace + emptySpeed) * (time_scale + 1.0) ;

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
            for(AgentBase agent : sameLane) {
                if(totalForce < lowerBound) { break ; }
                double agentPos = agent.getAdvancingDistance() ;
                if(agent == this) {
                    continue ;
                } else if(agentPos > workingPlace.getAdvancingDistance()) {
                    // 探索範囲外
                    break ; // for からの脱出
                } else if(agentPos <= relativePos) {
                    // 当該エージェントの後方なので無視
                    continue ; // 次の for へ
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

            //次のリンクへ進む
            relativePos -= workingPlace.getLinkLength() ;
            MapLink nextLink =
                chooseNextLinkBody(time, workingPlace,
                                   workingRoutePlan, true) ;
            if (nextLink == null) {
                break;
            }
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
        return - A_1  * Math.exp(A_2 * (personalSpace - dist)) ;
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
    protected boolean tryToPassNode(double time,
                                    RoutePlan workingRoutePlan,
                                    MapLink nextLink) {
        /* [2014.12.19 I.Noda]
         * NaiveAgent への経路記録の入り口のため,
         * recordTrail を導入。
         */
        recordTrail(time, currentPlace, nextLink) ;

        MapNode passingNode = currentPlace.getHeadingNode();
        MapLink previousLink = currentPlace.getLink() ;
        Direction direction_orig = currentPlace.getDirection() ;

        /* agent exits the previous link */
        currentPlace.getLink().agentExits(this);

        /* transit to new link */
        currentPlace.transitTo(nextLink) ;
        calcNextTarget(passingNode, workingRoutePlan, false) ;

        currentPlace.getLink().agentEnters(this);
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
        if (currentPlace.getLink().width == previousLink.width &&
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
    protected void recordTrail(double time, Place passingPlace,
                               MapLink nextLink) {
        route.add(new CheckPoint(passingPlace.getHeadingNode(),
                                 time, navigationReason.toString()));
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
    protected MapLink chooseNextLink(double time,
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
            System.exit(1) ;
        }

        MapLink nextLink =
            chooseNextLinkBody(time, passingPlace,
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
    protected MapLink chooseNextLinkBody(double time,
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
                } else if (node.getHint(subgoal) != null) {
                    return subgoal;
                } else {
                    Itk.logWarn("no sub-goal hint for " + subgoal);
                    workingRoutePlan.shift() ;
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
         */
        String targetTag = _target.getString() ;
        if(!map.isCheckedRouteKey(targetTag)) {
            Itk.logInfo("New Target", "find path.", "tag=", targetTag) ;
            map.calcGoalPathWithSync(targetTag) ;
        }

        MapNode other = _link.getOther(_node);
        double cost = other.getDistance(_target) ;
        cost += _link.length;
        return cost ;
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

	//############################################################
	/**
	 * 入出力
	 */
    //------------------------------------------------------------
    /**
     *
     */
    @Override
    public void dumpResult(PrintStream out) {
        out.print("" + generatedTime + ",");
        out.print("" + finishedTime + ",");/* 0.0 if not evacuated */
        out.print("" + getTriageInt() + ",");
        out.print("" + obstructer.accumulatedValueForLog()) ;
        for (final CheckPoint cp : route) {
            if (cp.node.getTags().size() != 0) {
                out.print("," + cp.node.getTagString().replace(',', '-'));
                out.print("("+ String.format("%1.3f", cp.time) +")");
            }
        }
        out.println();
    }

    //------------------------------------------------------------
    /**
     *
     */
    private Ellipse2D getCircle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx -r, cy -r, r * 2, r * 2);
    }

    //------------------------------------------------------------
    /**
     *
     */
    @Override
    public void drawInEditor(Graphics2D g) {
        if (currentPlace.getLink() == null) return;

        Point2D p = getPos();
        final double minHight =
            ((MapPartGroup)currentPlace.getLink().getParent()).getMinHeight();
        final double maxHight =
            ((MapPartGroup)currentPlace.getLink().getParent()).getMaxHeight();
        float r = (float)((getHeight() - minHight) / (maxHight - minHight));
        if (r < 0) r = 0;
        if (r > 1) r = 1;
        g.setColor(new Color(r, r, r));
        double cx = p.getX();
        double cy = p.getY();

        Vector3d vec = getSwing();
        cx += vec.x;
        cy += vec.y;

        g.fill(getCircle(cx, cy, 20));

        if (selected) {
            g.setColor(Color.YELLOW);
            g.fill(getCircle(cx, cy, 10));
        }  else {
            g.setColor(Color.GRAY);
            g.fill(getCircle(cx, cy, 10));
        }
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
