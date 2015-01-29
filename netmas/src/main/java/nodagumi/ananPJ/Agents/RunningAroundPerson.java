// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3d;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.misc.RoutePlan ;
import nodagumi.ananPJ.misc.Place;
import nodagumi.ananPJ.misc.SpecialTerm;

import nodagumi.Itk.*;

/* TODOs:
 * - make each junction a waiting queue
 * - agents change their directions in pathway
 * - agents should not go back to the same path many times
 */

/* effect of damage, for Chloropicrin (minutes):
 *  20000, BLACK,  STOP #100% Dead
 *  2000,  BLACK,  STOP #50% Dead
 *  1000,  RED,    STOP #Cannot breathe
 *  200,   YELLOW, 25%  #Cannot walk, can breath
 *  1,     GREEN,  50%  #Can walk, cannot open eyes
 *  0,     GREEN,  100% #Normal
 */

public class RunningAroundPerson extends EvacuationAgent implements Serializable {
    private static final long serialVersionUID = -6313717005123377059L;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "RunningAroundPerson" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 速度計算関係
	 * emptyspeed : ??? Initial values 
	 * time_scale : シミュレーションステップ
     * MAX_SPEED : 自由速度
     * ZERO_SPEED : ???
     * density : 人口密度らしい
     * order_in_row : lane の中での順番。
     *                set_swing でセットされ、calc_speed_lane で参照。
     *                ***** 間違いの元になりそう。 *****
	 */
    protected double emptyspeed = V_0;
    protected double time_scale = 1.0;//0.5; simulation time step 
    public static double MAX_SPEED = 0.96;
    public static double ZERO_SPEED = 0.1;
    //protected double density;
    //protected int order_in_row;

    //============================================================
    /**
     * 経由点の通過情報
     */
    class CheckPoint implements Serializable {
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

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * sane_navigation_from_node の不要な呼び出し回避用
     */
    private boolean sane_navigation_from_node_forced = true;
    private MapLink sane_navigation_from_node_current_link;
    private MapLink sane_navigation_from_node_link;
    private MapNode sane_navigation_from_node_node;
    private MapLink sane_navigation_from_node_result;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * next_link_candidate : 次に移るリンクの候補
     *       preUpdate()でリセット、
     *       move_commit()でnavigation()の結果をセット、
     *       tryToPassNode()でsetCurrentLink()される。
     */
    //MapLink next_link_candidate = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンク上の次の位置。
     * 0 以下あるいはリンク長より大きい場合、次のリンクに移っていることになる。
     * move_set() でセット。
     * move_commit() のなかで、setPosition() される。
     */
    //protected double next_position = 0.0;
    protected Place nextPlace = new Place();

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * swing を更新するかどうか（表示用？）
     */
    boolean update_swing_flag = true;//false;

    //============================================================
    /**
     * 速度モデル
     * DensityModel:
     * calc speed with density (number of persion / m^2).
     *  v: m/sec, rho: number of person / m^2
     *  sinplex road: v = 1.2 - 0.25 * rho
     *  duplex road : v = 1.27 * 10 ^ (-0.22 * rho)
     *  duplex road : v = 1.25 - 0.476 * rho
     */
    public static enum SpeedCalculationModel {
        LaneModel,
        StraitModel,
        DensityModel
    }

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 速度モデル
     */
    private SpeedCalculationModel calculation_model =
        SpeedCalculationModel.LaneModel;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 速度計算の定数
     */
    protected static double A_0 = 0.962;//1.05;//0.5;
    protected static double A_1 = 0.869;//1.25;//0.97;//2.0;
    protected static double A_2 = 4.682;//0.81;//1.5;
    protected static double V_0 = 1.02265769054586;
    
    protected static double PERSONAL_SPACE = 2.0 * 0.522;//0.75;//0.8;

    /* 同方向/逆方向のレーンでの単位距離
     * 0.7 だとほとんど進まなくなる。
     * 1.0 あたりか？
     */
    protected static double WidthUnit_SameLane = 0.9 ; //0.7;
    protected static double WidthUnit_OtherLane = 0.9 ; //0.7;

    /* [2015.01.17 I.Noda]
     *以下は、density model で使われる。
     */
    /* minimum distance between agents */
    protected static final double MIN_DISTANCE_BETWEEN_AGENTS = 0.3;
    /* mininum distance that agent can walk with max speed */
    protected static final double DISTANCE_MAX_SPEED =
        MAX_SPEED + MIN_DISTANCE_BETWEEN_AGENTS;    // 0.96 + 0.3
    /* minimum speed to break dead lock state */
    protected static final double MIN_SPEED_DEADLOCK = 0.3;

    /* [2015.01.29 I.Noda]
     *以下は、strait model で使われる。
     */
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Strait モデルで、超過密状態から抜け出すため、
     * 対向流のエージェントで、最低間隔を決めておく。
     * これをある程度大きくしておかないと、
     * 対抗流から過大な力を受け、全く抜け出せなくなる。
     */
    protected static double InsensitiveDistanceInCounterFlow =
        PERSONAL_SPACE * 0.5 ;

/*
    protected static double A_0 = 0.5*0.962;//1.05;//0.5;
    protected static double A_1 = 1.5*0.869;//1.25;//0.97;//2.0;
    protected static double A_2 = 4.682;//0.81;//1.5;
    protected static double V_0 = 1.24504634565416;
    
    protected static double PERSONAL_SPACE = 2.0 * 0.522;//0.75;//0.8;
    protected static double STAIR_SPEED_CO = 0.6;//0.7;
*/
    
    
/* 2010/9/16 23:53 noda
 *    * 速度誤差最適化 (expF)
 *
 *     * 9900 回目の学習結果
 *
 *       * c0 = 0.962331091566513
 *         c1 = 0.869327852313837
 *         c2 = 4.68258910604962
 *         vStar = 1.02265769054586
 *         rStar = 0.522488010351651
 *
 *     * 傾向：渋滞の状況はなんとなく再現している。ただし、戻りがある。
 *       最高速度 (vStar) は低くなり勝ち。
 *
 *   * 位置誤差最適化 (expG)
 *
 *     * 9900 回目の学習結果
 *
 *       * c0 = 1.97989178714465
 *         c1 = 1.12202742329362
 *         c2 = 0.95466478370757
 *         vStar = 1.24504634565416
 *         rStar = 0.805446866507348
 */
    
    /*
    protected static double A_0 = 1.97989178714465;
    protected static double A_1 = 1.12202742329362;
    protected static double A_2 = 0.95466478370757;
    
    protected static double V_0 = 1.24504634565416;
    protected static double PERSONAL_SPACE = 0.805446866507348;
    */
    
    /*
     protected static double A_0 = 0.962331091566513;
     
    protected static double A_1 = 0.869327852313837;
    protected static double A_2 = 4.68258910604962;
    
    protected static double V_0 = 1.02265769054586;
    protected static double PERSONAL_SPACE = 0.522488010351651;
    
    protected static double STAIR_SPEED_CO = 1.0;//0.7;
    */
    
	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * 推論理由を格納。
	 * [I.Noda]
	 * 効率のため、あまりメモリを消費しない方法に切り替え。
	 */
	ReasonTray navigation_reason = new ReasonTray() ;

	//############################################################
	/**
	 * 初期化関連
	 */
    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public RunningAroundPerson() {} ;
    
    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public RunningAroundPerson(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * constractor
     */
    public RunningAroundPerson(int _id,
            double _emptySpeed,
            double _confidence,
            double _maxAllowedDamage,
            double _generatedTime,
            Random _random) {
        init(_id, _emptySpeed, _confidence, _maxAllowedDamage, _generatedTime,
             _random) ;
    } ;

    //------------------------------------------------------------
    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id, Random _random) {
        super.init(_id, _random);
        update_swing_flag = true;
        route = new ArrayList<CheckPoint>();
    }

    //------------------------------------------------------------
    /**
     * 初期化。constractorから分離。
     */
    public void init(int _id,
            double _emptySpeed,
            double _confidence,
            double _maxAllowedDamage,
            double _generatedTime,
            Random _random) {
        init(_id, _random);
        
        generatedTime = _generatedTime;
        emptyspeed = _emptySpeed;
    }

    //------------------------------------------------------------
	/**
	 * 与えられたエージェントインスタンスに内容をコピーし、初期化。
     * 差分プログラミングにする。
	 */
    @Override
    public EvacuationAgent copyAndInitializeBody(EvacuationAgent _r) {
        RunningAroundPerson r = (RunningAroundPerson)_r ;
        super.copyAndInitializeBody(r) ;
        r.emptyspeed = emptyspeed;

        return r;
    }

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
     * [2015.01.10 I.Noda]
     * (should be obsolete)
     */
    public double getDensity() {
        //Itk.dbgWrn("getDensity() is obsolete.") ;
        return 0.0 ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setEmergency() {
        setGoal(SpecialTerm.Emergency) ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public boolean isEmergency() {
        return goal.equals(SpecialTerm.Emergency) ;
    }

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
        return emptyspeed;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setEmptySpeed(double s) {
        emptyspeed = s;
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
        /* tkokada: modified to apply deserialize method */
        if (!isEvacuated()) {
            if(currentPlace.isBeforeStartFromLink()) { // リンクが初期位置
                prepareForSimulation_FromLink() ;
            } else if (currentPlace.isBeforeStartFromNode()) { // ノードが初期位置
                prepareForSimulation_FromNode() ;
            }
            speed = 0;

            renavigate(routePlan);
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
            calcCostFromPlaceTo(currentPlace, routePlan) ;

        // backwardDirection に変更。
        currentPlace.turnAround() ;
        double costOfBackwardDirection =
            calcCostFromPlaceTo(currentPlace, routePlan) ;

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
            double dist = calcCostFromPlaceTo(currentPlace, routePlan) ;
            if(dist < bestDist) {
                bestLink = link ;
                bestDist = dist ;
            }
        }
        if(bestLink == null) { // もし見つからない場合
            Itk.dbgErr("currentPlace has no way for routePlan.") ;
            Itk.dbgMsg("currentPlace", currentPlace) ;
            Itk.dbgMsg("routePlan", routePlan) ;
            finalizeEvacuation(0, false) ;
        }
        currentPlace.setLink(bestLink) ;
    }


    //------------------------------------------------------------
    /**
     * preUpdate
     */
    @Override
    public void preUpdate(double time) {
        //next_link_candidate = null;

        calc_speed(time);
        move_set(speed, time, true);
    }

    //------------------------------------------------------------
    /**
     * set_swing
     */
    private void set_swing() {
        /* agent drawing */
        //TODO should no call here, as lane should be set up properly
        MapLink currentLink = currentPlace.getLink() ;
        currentLink.setup_lanes();

        int w = currentPlace.getLaneWidth() ;
        int index = currentPlace.getIndexInLane(this) ;
        if (isBackwardDirection())
            index = currentPlace.getLane().size() - index;

        if (isForwardDirection()) {
            if (index >= 0) {
                swing_width = (2.0 * ((currentPlace.getLane().size() - index) % w)) / currentLink.width - 1.0;
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
     * 次の位置を計算。(obsolete) [2015.01.10 I.Noda]
     * [2015.01.10 I.Noda]
     * ここでの navigate などの処理は、単に流入制限の計算に使うためだけにある。
     * しかも其の計算やその後の実装には、いろいろ不備がある。
     * なので、このメソッドは obsolete にしておく。
     * @param d : speed に相当する大きさ。単位時間に進める長さ。
     *     [2015.01.10 I.Noda] direction はかかっていないものとする。
     * @param time : 時間ステップ。1.0 が1秒。
     * @param will_move_out : 次のリンクに進むかどうか。WaitDirective 用。
     */
    protected boolean move_set_obsolete(double d, double time, boolean will_move_out) {
        nextPlace.set(currentPlace) ;
        nextPlace.makeAdvance(d * time_scale) ;

        RoutePlan workingRoutePlan = routePlan.duplicate() ;
        while (!nextPlace.isOnLink()) {
            if (will_move_out) {
                /* schedule moving out */
                MapLink next_link = navigate(time, nextPlace,
                                             workingRoutePlan, true);
                if (nextPlace.isRestrictedLink() && next_link == null) {
                    // 現在の道が一方通行か閉鎖で、
                    // 先の道路が見つからなかったらアウト
                    /* [2015.01.10 I.Noda] bug
                     * おそらく単純に終わるのはおかしい */
                    break;
                }
                nextPlace.transitTo(next_link) ;
            } else {
                // WAIT_FOR, WAIT_UNTIL によるエージェントの停止は下記でおこなう
                /* [2015.01.10 I.Noda]
                 * 本来なら、headingNode 上なので、リンクを抜けているはずだが、
                 * 特例として、とどまることにする。
                 */
                nextPlace.setAdvancingDistance(nextPlace.getLinkLength()) ;
                break;
            }
        }
        return false;
    }
    //------------------------------------------------------------
    /**
     * 次の位置を計算。(new) [2015.01.10 I.Noda]
     * @param d : speed に相当する大きさ。単位時間に進める長さ。
     *     [2015.01.10 I.Noda] direction はかかっていないものとする。
     * @param time : 時間ステップ。1.0 が1秒。
     * @param will_move_out : 次のリンクに進むかどうか。WaitDirective 用。
     */
    protected boolean move_set(double d, double time, boolean will_move_out) {
        nextPlace.set(currentPlace) ;
        double distToMove = d * time_scale ;
        nextPlace.makeAdvance(d * time_scale, !will_move_out) ;
        return false ;
    }

    //------------------------------------------------------------
    /**
     * move_commit
     */
    protected boolean move_commit(double time) {
        currentPlace.set(nextPlace) ;
        while (!currentPlace.isOnLink()) {

            /* [2015.01.14 I.Noda]
             * もし、リンクを通り過ぎていて、そのリンクの終点が goal なら
             * 避難完了
             */
            if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
                currentPlace.getHeadingNode().hasTag(goal)){
                finalizeEvacuation(time, true) ;

                return true;
            }

            sane_navigation_from_node_forced = true;
            MapLink nextLink = navigate(time, currentPlace, routePlan, true) ;
            sane_navigation_from_node_forced = true;

            tryToPassNode(time, routePlan, nextLink) ;

            /* [2015.01.14 I.Noda]
             * もし、渡った先のリンクがゴールなら、避難完了
             */
            if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
                currentPlace.getLink().hasTag(goal)){
                finalizeEvacuation(time, true) ;

                return true ;
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
            finalizeEvacuation(time, true) ;
            return true;
        }

        boolean ret = move_commit(time);
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
    protected void calc_speed(double time) {
        switch (calculation_model) {
        case LaneModel:
            calc_speed_lane_generic(time,calculation_model) ;
            break;
        case StraitModel:
            calc_speed_lane_generic(time,calculation_model);
            break;
        case DensityModel:
            //calc_speed_density(time);
            calc_speed_density_reviced(time);
            break;
        default:
            Itk.dbgErr("Unknown Speed Model") ;
            Itk.dbgMsg("calculation_model",calculation_model) ;
            break;
        }

        /* [2015.01.09 I.Noda]
         * リンクの交通規制など (Gate)
         */
        currentPlace.getLink().applyRestrictionToAgent(this, time) ;

        /* [2015.01.09 I.Noda]
         * リンクを踏破しているなら、headingNode での規制
         * ノードの交通規制など (STOP)
         */
        if (!currentPlace.isBeyondLinkWithAdvance(speed)) {
            currentPlace.getHeadingNode().applyRestrictionToAgent(this, time) ;
        }

        pollution.effect(this);
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
        double maxDistance = (PERSONAL_SPACE + emptyspeed) * (time_scale + 1.0) ;

        //前方のエージェントを探している場所
        Place workingPlace = currentPlace.duplicate() ;
        workingPlace.makeAdvance(maxDistance) ;

        //???
        //MapNode node_to_navigate = currentPlace.getHeadingNode() ;
        // 現在のリンク中での相対位置
        int indexInLane = workingPlace.getIndexInLane(this) ;
        //作業用の routePlan
        RoutePlan workingRoutePlan = routePlan.duplicate() ;

        while (workingPlace.getAdvancingDistance() > 0) {
            ArrayList<EvacuationAgent> agents = workingPlace.getLane() ;
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
                    sane_navigation_from_node(time, workingPlace,
                                              workingRoutePlan, true) ;
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
     * lane および strait による速度計算
     */
    private void calc_speed_lane_generic(double time,
                                         SpeedCalculationModel model) {
        /* base speed */
        double baseSpeed =
            currentPlace.getLink().calcEmptySpeedForAgent(emptyspeed,
                                                          this, time) ;
        //自由速度に向けた加速
        dv = A_0 * (baseSpeed - speed) ;
        //social force による減速
        switch (model) {
        case LaneModel:
            double distToPredecessor = calcDistanceToPredecessor(time) ;
            dv += calcSocialForce(distToPredecessor) ;
            break;
        case StraitModel:
            dv += accumulateSocialForces(time) ;
            break;
        default:
            Itk.dbgErr("Unknown Speed Model") ;
            Itk.dbgMsg("calculation_model",model) ;
            break;
        }

        //時間積分
        dv *= time_scale;
        speed += dv;

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
     * @return 力
     */
    private double accumulateSocialForces(double time) {
        //求める力
        double totalForce = 0.0 ;

        //探す範囲
        double maxDistance = (PERSONAL_SPACE + emptyspeed) * (time_scale + 1.0) ;

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
            //順方向探索
            ArrayList<EvacuationAgent> sameLane = workingPlace.getLane() ;
            int laneWidth = workingPlace.getLaneWidth() ;
            for(EvacuationAgent agent : sameLane) {
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
                        (WidthUnit_SameLane *
                         ((laneWidth - (count % laneWidth)) % laneWidth)) ;
                    double force = calcSocialForceToHeading(dx,dy) ;
                    totalForce += force ;
                }
            }
            //逆方向探索
            ArrayList<EvacuationAgent> otherLane = workingPlace.getOtherLane() ;
            int laneWidthOther = workingPlace.getOtherLaneWidth() ;
            double linkLength = workingPlace.getLinkLength() ;
            double insensitivePos = 0.0 ;
            for(int i = 0 ; i < otherLane.size() ; i++) {
                EvacuationAgent agent = otherLane.get(otherLane.size() - i - 1);
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
                        WidthUnit_OtherLane *
                        (((laneWidthOther - (countOther % laneWidthOther))
                          % laneWidthOther) + 1) ;
                    double force = calcSocialForceToHeading(dx,dy) ;
                    totalForce += force ;
                    if(countOther % laneWidthOther == 0) {
                        insensitivePos = (agentPos + 
                                          InsensitiveDistanceInCounterFlow) ;
                    }
                }
            }
            //次のリンクへ進む
            relativePos -= workingPlace.getLinkLength() ;
            MapLink nextLink =
                sane_navigation_from_node(time, workingPlace,
                                          workingRoutePlan, true) ;
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
    protected static double calcSocialForce(double dist) {
        return - A_1  * Math.exp(A_2 * (PERSONAL_SPACE - dist)) ;
    }

    //------------------------------------------------------------
    /**
     * social force のうち、進行方向に沿った力のみを計算する。
     * @param dx : 進行方向に沿った距離
     * @param dy : 横方向の距離
     * @return x 方向の力
     */
    protected static double calcSocialForceToHeading(double dx, double dy) {
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
     * density による速度計算(改良版？）
     * tkokada
     */
    private void calc_speed_density_reviced(double time) {
        /* the range to calculate the density */
        double DENSITY_RANGE = DISTANCE_MAX_SPEED * time_scale;

        ArrayList<EvacuationAgent> currentLinkAgents
            = currentPlace.getLink().getAgents();
        // 隣のリンクも含めた自分と同じ位置に存在するエージェント
        ArrayList<EvacuationAgent> samePlaceAgents =
            new ArrayList<EvacuationAgent>();

        /* the number of agents in the range */
        int inRangeSameDirectionAgents = 0;
        int inRangeOppositeDirectionAgents = 0;

        /* the number of agents in front of myself in range */
        int inFrontSameDirectionAgents = 0;
        int inFrontOppositeDirectionAgents = 0;

        /* in range agents on current link is calculated 
         * in between maxRange & minRange */
        double maxRange = 0.0;
        double minRange = 0.0;
        if (isForwardDirection()) {
            minRange = currentPlace.getPositionOnLink() ;
            maxRange = Math.min(currentPlace.getPositionOnLink() + DENSITY_RANGE,
                                currentPlace.getLinkLength()) ;
        } else {
            minRange = Math.max(currentPlace.getPositionOnLink() - DENSITY_RANGE,
                                0.0);
            maxRange = currentPlace.getPositionOnLink();
        }

        RoutePlan workingRoutePlan = routePlan.duplicate() ;
        MapLink nextLink =
            sane_navigation_from_node(time, currentPlace,
                                      workingRoutePlan, true) ;

        ArrayList<EvacuationAgent> nextLinkAgents = null;
        if (nextLink != null)
            nextLinkAgents = nextLink.getAgents();

        // 自分の位置よりも前を歩いている/歩いて来る一番近い agent
        EvacuationAgent frontAgent = null;

        // カレントリンク上の全エージェントについて
        for (EvacuationAgent agent : currentLinkAgents) {
            if (agent == this || agent.isEvacuated()) {
                continue;
            }
            if (agent.currentPlace.getPositionOnLink()
                == currentPlace.getPositionOnLink()) {
                samePlaceAgents.add(agent);
            }
            // agent が (minRange..maxRange) 内に位置する
            if (agent.currentPlace.getPositionOnLink() >= minRange &&
                agent.currentPlace.getPositionOnLink() <= maxRange) {
                if (getDirection() == agent.getDirection()) {
                    inRangeSameDirectionAgents += 1;
                } else {
                    inRangeOppositeDirectionAgents += 1;
                }
                // 以下、自分の隣か前を歩いている(歩いて来る) agent について(direction ごと)
                // agent is placed in front of this agent.
                if (isForwardDirection() &&
                    agent.currentPlace.getPositionOnLink()
                    >= currentPlace.getPositionOnLink()) {
                    if (agent.isForwardDirection()) {
                        inFrontSameDirectionAgents += 1;
                    } else {
                        inFrontOppositeDirectionAgents += 1;
                    }
                    if (agent.currentPlace.getPositionOnLink()
                        != currentPlace.getPositionOnLink()) {
                        if (frontAgent == null) {
                            frontAgent = agent;
                        } else if (frontAgent.currentPlace.getPositionOnLink()
                                   > agent.currentPlace.getPositionOnLink()) {
                            frontAgent = agent;
                        }
                    }
                // agent is placed in front of this agent.
                } else if (isBackwardDirection() &&
                           agent.currentPlace.getPositionOnLink()
                           <= currentPlace.getPositionOnLink()) {
                    if (agent.isBackwardDirection()) {
                        inFrontSameDirectionAgents += 1;
                    } else {
                        inFrontOppositeDirectionAgents += 1;
                    }
                    if (agent.currentPlace.getPositionOnLink()
                        != currentPlace.getPositionOnLink()) {
                        if (frontAgent == null) {
                            frontAgent = agent;
                        } else if (frontAgent.currentPlace.getPositionOnLink()
                                   < agent.currentPlace.getPositionOnLink()) {
                            frontAgent = agent;
                        }
                    }
                }
            }
        }

        if (nextLinkAgents != null && nextLink != null) {
            // 次に進むリンク上に存在する全エージェントについて
            for (EvacuationAgent agent : nextLinkAgents) {
                if (agent.isEvacuated()) {
                    continue;
                }
                double distance = getDistanceNeighborAgent(agent);
                if (distance < 0.0)
                    continue;
                if (distance == 0.0)
                    samePlaceAgents.add(agent);
                // agent が密度計算距離内に存在している
                if (distance <= DENSITY_RANGE) {
                    if (isSameDirectionNeighborAgent(agent)) {
                        inRangeSameDirectionAgents += 1;
                        if (isFrontNeighborAgent(agent)) {
                            inFrontSameDirectionAgents += 1;
                        }
                    } else {
                        inRangeOppositeDirectionAgents += 1;
                        if (isFrontNeighborAgent(agent))
                            inFrontOppositeDirectionAgents += 1;
                    }
                    if (distance != 0.0 && isFrontNeighborAgent(agent)) {
                        if (frontAgent == null) {
                            frontAgent = agent;
                        } else if (nextLinkAgents.contains(frontAgent)) {
                            if (getDistanceNeighborAgent(frontAgent) > distance) {
                                frontAgent = agent;
                            }
                        }
                    }
                }
            }
        }
        double density_range = Math.max(maxRange - minRange, DENSITY_RANGE);
        double density = (inRangeSameDirectionAgents + inRangeOppositeDirectionAgents + 1) / (currentPlace.getLinkWidth() * density_range);
        if (density <= 0.0) {
            speed = 0.0;
        } else if (inRangeOppositeDirectionAgents > 0) {
            // 前方から歩いて来る agent が一人でもいる場合
            speed = MAX_SPEED - 0.476 * density; //speed = 1.25 - 0.476 * density; modified by goto in 2014.09.11
        } else {
            speed = MAX_SPEED - 0.240 * density; //speed = 1.2 - 0.25 * density;modified by goto in 2014.09.11
        }

        /* this agent is head of current link */
        if (inFrontSameDirectionAgents + inFrontOppositeDirectionAgents == 0) {
            speed = MAX_SPEED;
            if (frontAgent != null)
                System.err.println("RunningAroundPerson.calc_speed_density_reviced in front agent but front agent exist!");
        // 2012.10.01 tkokada update.
        // The lane number of agents are assumed as the head.
        } else if (inFrontSameDirectionAgents + inFrontOppositeDirectionAgents
                   < (int)currentPlace.getLinkWidth()) {
            speed = MAX_SPEED;
        } else if (frontAgent != null) {
            double distance;
            if (currentLinkAgents.contains(frontAgent))
                distance = Math.abs(frontAgent.currentPlace.getPositionOnLink()
                                    - currentPlace.getPositionOnLink());
            else
                distance = getDistanceNeighborAgent(frontAgent);
            // 前を歩いている人にぶつからない(近づきすぎない)速度まで speed を落とす
            if (distance <= MIN_DISTANCE_BETWEEN_AGENTS) {
                speed = 0.0;
            } else if (distance <= DISTANCE_MAX_SPEED * time_scale) {
                if (speed * time_scale > distance - MIN_DISTANCE_BETWEEN_AGENTS) {
                    speed = (distance - MIN_DISTANCE_BETWEEN_AGENTS) / time_scale;
                }
            }
        /* check dead lock state with duplex link */
        }

        // 全員が同じ方向に並んで歩いていて、対向者もいない場合は、道幅に収まる人数まで MAX_SPEED になれる
        // (余裕があれば自分を MAX_SPEED にする)
        if (inFrontSameDirectionAgents == samePlaceAgents.size() &&
                inFrontOppositeDirectionAgents == 0 &&
                inFrontSameDirectionAgents > 0) {
            int numberLane = (int)currentPlace.getLinkWidth() ;
            int counter = 0;
            for (EvacuationAgent agent : samePlaceAgents) {
                if (agent.getSpeed() >= MAX_SPEED)
                    counter += 1;
            }
            if (counter < numberLane) {
                speed = MAX_SPEED;
            }
        }

        if (speed <= ZERO_SPEED && speed > 0.0)
            speed = 0.0;

        if (speed <= 0.0) {
            speed = 0.0;
            // ※以下は削除検討対象
            boolean existPlusSpeed = false;
            boolean enterIfStatement = false;
            if (
                ( (inFrontSameDirectionAgents + inFrontOppositeDirectionAgents == samePlaceAgents.size())
                    && (inFrontSameDirectionAgents + inFrontOppositeDirectionAgents > 0) )
                || (inFrontSameDirectionAgents == samePlaceAgents.size() && inFrontSameDirectionAgents > 0)
            ) {
                int numberLane = (int)currentPlace.getLinkWidth() ;
                int counter = 0;
                for (EvacuationAgent agent : samePlaceAgents) {
                    if (agent.getSpeed() >= MAX_SPEED * 0.8)
                        counter += 1;
                }
                if (counter < numberLane) {
                    speed = MAX_SPEED * 0.8;
                }
                enterIfStatement = true;
            }
            if (inFrontSameDirectionAgents == 0) {
                speed = MIN_SPEED_DEADLOCK;
                enterIfStatement = true;
            }
        }

        if (speed <= 0.0) {
            speed = ZERO_SPEED;
        } else if (speed >= MAX_SPEED) {
            speed = MAX_SPEED;
        }
    }

    //------------------------------------------------------------
    /**
     * get distance between this agent and neighbor agent
     * 隣のリンク上にいる agent との距離
     */
    private double getDistanceNeighborAgent(EvacuationAgent agent) {
        double distance = 0.0;
        MapLink currentLink = currentPlace.getLink() ;
        MapLink neighborLink = agent.currentPlace.getLink() ;

        if (currentLink.getFrom() == neighborLink.getFrom()) {
            distance
                = currentPlace.getPositionOnLink()
                + agent.currentPlace.getPositionOnLink() ;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            distance
                = currentPlace.getPositionOnLink()
                + neighborLink.length
                - agent.currentPlace.getPositionOnLink();
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            distance
                = currentLink.length
                - currentPlace.getPositionOnLink()
                + agent.currentPlace.getPositionOnLink();
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            distance
                = currentLink.length
                - currentPlace.getPositionOnLink()
                + neighborLink.length
                - agent.currentPlace.getPositionOnLink();
        } else {
            System.err.println("\tRunningAroundPerson.getDistanceNeighborAgent inputted neighbor link is not neighbor!");
            // distance = -1.0;
            System.exit(1);
        }
        return distance;
    }

    //------------------------------------------------------------
    /**
     * is a neighbor agent same direction with this agent
     */
    private boolean isSameDirectionNeighborAgent(EvacuationAgent agent) {
        MapLink currentLink = currentPlace.getLink();
        MapLink neighborLink = agent.currentPlace.getLink();

        // 重複したリンク
        if (currentLink.getFrom() == neighborLink.getFrom() && currentLink.getTo() == neighborLink.getTo()) {
            if (isForwardDirection() && agent.isBackwardDirection())
                return true;
            else if (isBackwardDirection() && agent.isForwardDirection())
                return true;
            else
                return false;
        }
        // ループ状の重複したリンク
        if (currentLink.getFrom() == neighborLink.getTo() && currentLink.getTo() == neighborLink.getFrom()) {
            if (isForwardDirection() && agent.isForwardDirection())
                return true;
            else if (isBackwardDirection() && agent.isBackwardDirection())
                return true;
            else
                return false;
        }
        if (currentLink.getFrom() == neighborLink.getFrom()) {
            if (isForwardDirection() && agent.isBackwardDirection())
                return true;
            else if (isBackwardDirection() && agent.isForwardDirection())
                return true;
            else
                return false;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            if (isForwardDirection() && agent.isForwardDirection())
                return true;
            else if (isBackwardDirection() && agent.isBackwardDirection())
                return true;
            else
                return false;
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            if (isForwardDirection() && agent.isForwardDirection())
                return true;
            else if (isBackwardDirection() && agent.isBackwardDirection())
                return true;
            else
                return false;
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            if (isForwardDirection() && agent.isBackwardDirection())
                return true;
            else if (isBackwardDirection() && agent.isForwardDirection())
                return true;
            else
                return false;
        } else {
            System.err.println("\tRunningAroundPerson.isSameDirectionNeighborAgent inputted neighbor link is not neighbor!");
            return false;
        }
    }

    //------------------------------------------------------------
    /**
     * is a neighbor agent place in front of this agent
     */
    private boolean isFrontNeighborAgent(EvacuationAgent agent) {
        MapLink currentLink = currentPlace.getLink() ;
        MapLink neighborLink = agent.currentPlace.getLink() ;

        if (currentLink.getFrom() == neighborLink.getFrom()) {
            if (isBackwardDirection())
                return true;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            if (isBackwardDirection())
                return true;
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            if (isForwardDirection())
                return true;
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            if (isForwardDirection())
                return true;
        } else {
            System.err.println("\tRunningAroundPerson." +
                    "isFrontNeighborAgent inputted neighbor link is " +
                    "not neighbor!");
        }
        return false;
    }

    //------------------------------------------------------------
    /**
     * is a neighbor agent place in same node
     * ※未使用メソッド
     */
    private boolean isSamePlaceNeighborAgent(EvacuationAgent agent) {
        MapLink currentLink = currentPlace.getLink();
        MapLink neighborLink = agent.currentPlace.getLink();

        if (currentLink.getFrom() == neighborLink.getFrom()) {
            if (currentPlace.getPositionOnLink() == 0.0 &&
                agent.currentPlace.getPositionOnLink() == 0.0)
                return true;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            if (currentPlace.getPositionOnLink() == 0.0 &&
                agent.currentPlace.getPositionOnLink() == neighborLink.length)
                return true;
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            if (currentPlace.getPositionOnLink() == currentLink.length && 
                agent.currentPlace.getPositionOnLink() == 0.0)
                return true;
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            if (currentPlace.getPositionOnLink() == currentLink.length &&
                agent.currentPlace.getPositionOnLink() == neighborLink.length)
                return true;
        }
        return false;
    }

    //------------------------------------------------------------
    /**
     * Calculate a distance between myself and the front agent in current link.
     * @return the distance
     */
    public double calcDistanceToFront() {
        double distance = 0.0;
        for (EvacuationAgent agent : currentPlace.getLink().getAgents()) {
            double tmp_distance = 0.0;
            if (isForwardDirection()) {
                if (agent.currentPlace.getPositionOnLink()
                    > currentPlace.getPositionOnLink()) {
                    tmp_distance
                        = agent.currentPlace.getPositionOnLink()
                        - currentPlace.getPositionOnLink();
                }
            } else {
                if (agent.currentPlace.getPositionOnLink()
                    < currentPlace.getPositionOnLink()) {
                    tmp_distance
                        = currentPlace.getPositionOnLink()
                        - agent.currentPlace.getPositionOnLink();
                }
            }
            if (tmp_distance > distance) {
                distance = tmp_distance;
            }
        }
        return distance;
    }

	//############################################################
	/**
	 * 移動計画及び移動
	 */
    //------------------------------------------------------------
    /**
     * try to pass a node, and enter next link 
     * change: navigation_reason, route, prev_node, previous_link,
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
        double direction_orig = currentPlace.getDirectionValue() ;

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
            passingNode.getPathways().size() == 2) {
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
                                 time, navigation_reason.toString()));
    }

	//############################################################
	/**
	 * 経路計画
	 */
    //------------------------------------------------------------
    /**
     * ノード上で、次の道を探す。
     */
    protected MapLink navigate(double time,
                               Place passingPlace,
                               RoutePlan workingRoutePlan,
                               boolean on_node) {
        final MapLinkTable way_candidates
            = passingPlace.getHeadingNode().getPathways();

        /* trapped? */
        if (way_candidates.size() == 0) {
            System.err.println("Warning: Agent trapped!");
            return null;
        }

        /* only one way to choose */
        if (way_candidates.size() == 1) {
            return way_candidates.get(0);
        }

        /* if not in navigation mode, go back to path */
        /* [2015.01.04 I.Noda]
         * Agent の goal が失われることはないはずなので、
         * エラーで落ちるようにしておく。
         */
        if (goal == null) {
            Itk.dbgErr("An agent lost its goal.") ;
            Itk.dbgMsg("agent.ID", this.ID) ;
            System.exit(1) ;
        }

        MapLink target =
            sane_navigation_from_node(time, passingPlace,
                                      workingRoutePlan, on_node) ;
        if (target != null) {
            return target;
        }

        /* choose randomly */
        int i = 0;
        if (way_candidates.size() > 1) {
            i = (int)(random.nextDouble() * (way_candidates.size() - 1));
        }
        return way_candidates.get(i);
    }

    //------------------------------------------------------------
    /**
     * for call outside
     */
    public void renavigate() {
        renavigate(routePlan) ;
    }
    //------------------------------------------------------------
    /**
     *
     */
    public void renavigate(RoutePlan workingRoutePlan) {
        if (goal != null){
            calcNextTarget(currentPlace.getHeadingNode(),
                           workingRoutePlan, false);
        }
    }

    //------------------------------------------------------------
    /**
     * ノードにおいて、次の道を選択するルーチン
     */
    protected MapLink sane_navigation_from_node(double time,
                                                Place passingPlace,
                                                RoutePlan workingRoutePlan,
                                                boolean on_node) {
        // 前回の呼び出し時と同じ結果になる場合は不要な処理を回避する
        if (isSameSituationForSaneNavigationFromNode(passingPlace))
            return sane_navigation_from_node_result;
        backupSituationForSaneNavigationFromNodeBefore(passingPlace);

        MapLinkTable way_candidates =
            passingPlace.getHeadingNode().getPathways();
        double min_cost = Double.MAX_VALUE;
        double min_cost_second = Double.MAX_VALUE;
        MapLink way = null;
        MapLink way_second = null;

        MapLinkTable way_samecost = null;

        final Term next_target =
            calcNextTarget(passingPlace.getHeadingNode(),
                           workingRoutePlan, on_node) ;

		navigation_reason.clear().add("for").add(next_target).add("\n");
        for (MapLink way_candidate : way_candidates) {
            // tkokada
            /* ゴールもしくは経由点のチェック。あるいは、同じ道を戻らない */
            if (workingRoutePlan.isEmpty() && way_candidate.hasTag(goal)) {
                /* finishing up */
                way = way_candidate;
				navigation_reason.add("found goal").add(goal);
                break;
            } else if (way_candidate.hasTag(next_target)) {
                /* reached mid_goal */
                way = way_candidate;
				navigation_reason.add("found mid-goal in").add(way_candidate) ;
                if(!isKnownDirective(workingRoutePlan.top())) {
                    // directive でない場合のみ shiftする。
                    // そうでなければ、directive の操作待ち。
                    workingRoutePlan.shift() ;
                }
                break;
            } 

            // 現在の way_candidate を選択した場合の next_target までのコスト計算
            double cost = calcWayCostTo(way_candidate,
                                        passingPlace.getHeadingNode(),
                                        next_target) ;

            if (cost < min_cost) { // 最小cost置き換え
                min_cost = cost;
                way = way_candidate;
                way_samecost = null;
            } else if (cost == min_cost) { // 最小コストが同じ時の処理
                if (way_samecost == null) {
                    way_samecost = new MapLinkTable();
                    way_samecost.add(way) ;
                }
                way_samecost.add(way_candidate);
            }
        }

        if (way_samecost != null && way_samecost.size()>0) {
            way = way_samecost.get(random.nextInt(way_samecost.size())) ;
        }

        if (way == null) {
            way = way_second;
        }
        backupSituationForSaneNavigationFromNodeAfter(way) ;

        if (way == null) {
            return null;
        }

		navigation_reason.add("\n -> chose")
            .add(way.getOther(passingPlace.getHeadingNode())) ;
        return way;
    }

    //------------------------------------------------------------
    /**
     * 前回の呼び出し時と同じ条件かどうかのチェック
     */
    private boolean isSameSituationForSaneNavigationFromNode(Place passingPlace) {
        boolean forced = sane_navigation_from_node_forced ;
        sane_navigation_from_node_forced = false ;
        return (!forced &&
                sane_navigation_from_node_current_link == currentPlace.getLink() &&
                sane_navigation_from_node_link == passingPlace.getLink() &&
                sane_navigation_from_node_node == passingPlace.getHeadingNode() &&
                emptyspeed < currentPlace.getRemainingDistance()) ;
    }

    //------------------------------------------------------------
    /**
     * 上記のチェックのための状態バックアップ(before)
     */
    private void backupSituationForSaneNavigationFromNodeBefore(Place passingPlace) {
        sane_navigation_from_node_current_link = currentPlace.getLink() ;
        sane_navigation_from_node_link = passingPlace.getLink();
        sane_navigation_from_node_node = passingPlace.getHeadingNode() ;
    }

    //------------------------------------------------------------
    /**
     * 上記のチェックのための状態バックアップ(after)
     */
    private void backupSituationForSaneNavigationFromNodeAfter(MapLink result) {
        sane_navigation_from_node_result = result ;
    }

    //------------------------------------------------------------
    /**
     * 指定された RoutePlan で、次のターゲットを得る。
     * @param node : このノードにおけるターゲットを探す。
     * @param workingPlan : 指定された RoutePlan。shiftする可能性がある。
     * @return : workingPlan の index 以降の次のターゲット、もしくは goal。
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
                    Itk.dbgWrn("no sub-goal hint for " + subgoal);
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
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) {
        MapNode other = _way.getOther(_node);
        double cost = other.getDistance(_target) ;
        cost += _way.length;
        return cost ;
    }

    //------------------------------------------------------------
    /**
     * あるplaceから現在のroutePlanの次の目的地までのコスト。
     * @param workingPlace : 現在地を示す Place
     * @param workingRoutePlan : 現在の経路計画。保存される。
     * @return コスト
     */
    public double calcCostFromPlaceTo(Place _place,
                                      final RoutePlan _routePlan) {
        RoutePlan workingRoutePlan = _routePlan.duplicate() ;
        Term target = calcNextTarget(_place.getHeadingNode(),
                                     workingRoutePlan,false) ;
        double costFromEnteringNode =
            calcWayCostTo(_place.getLink(), _place.getEnteringNode(), target) ;
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
        out.print("" + getTriage() + ",");
        out.print("" + accumulatedExposureAmount);
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
    public void draw(Graphics2D g, 
            boolean experiment) {
        if (experiment && ((displayMode & 2) != 2)) return;
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

    //------------------------------------------------------------
    /**
     * (for editor)
     */
    @Override
    public JPanel paramSettingPanel(NetworkMap network_map)  {
        class ParamSettingPanel extends JPanel
        implements ChangeListener, ItemListener {
            private static final long serialVersionUID = -2502949408346819443L;

            RunningAroundPerson agent;

            JSpinner time, speed;
            JComboBox navigationMode;
            public ParamSettingPanel(NetworkMap _networkMap,
                    RunningAroundPerson _agent) {
                super();
                setLayout(new GridLayout(3, 2));
                agent = _agent;

                add(new JLabel("Generated Time"));
                time = new JSpinner(new SpinnerNumberModel(agent.generatedTime,
                        0.0, 100.0,
                        0.1));
                time.addChangeListener(this);
                add(time);

                add(new JLabel("Speed"));
                speed = new JSpinner(new SpinnerNumberModel(agent.emptyspeed,
                        0.0, 10.0,
                        0.1));
                speed.addChangeListener(this);
                add(speed);

                add(new JLabel("Target Node/Link"));
                ArrayList<String> all_tags = _networkMap.getAllTags();
                all_tags.add("");
                navigationMode = new JComboBox(all_tags.toArray());
                navigationMode.addItemListener(this);
                add(navigationMode);
                updateAgent();
            }

            @Override
            public void stateChanged(ChangeEvent e) {
                updateAgent();
            }

            @Override
            public void itemStateChanged(ItemEvent e) {
                updateAgent();
            }
            
            private void updateAgent() {
                agent.generatedTime = ((Double)(time.getValue())).doubleValue();
                agent.emptyspeed = ((Double)(speed.getValue())).doubleValue();
                agent.speed = agent.emptyspeed;
                final String goalString = (String)navigationMode.getSelectedItem();
                agent.setGoal(new Term(goalString));
            }
        };
        return new ParamSettingPanel(network_map, this); 
    }
    
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
