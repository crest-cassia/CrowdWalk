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
     * 速度計さん関係
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
    protected double density;
    protected int order_in_row;

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
     * on_node : 現在、ノード通過中かどうか？
     *       move_set()の navigate() を呼び出す前後でon-off。
     *       calcNextTarget() で参照。routePlanをshiftするかどうか判断。
     */
    MapLink next_link_candidate = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンク上の次の位置。
     * 0 以下あるいはリンク長より大きい場合、次のリンクに移っていることになる。
     * move_set() でセット。
     * move_commit() のなかで、setPosition() される。
     */
    protected double next_position = 0.0;

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
    protected static double STAIR_SPEED_CO = 0.6;//0.7;

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
        r.direction = direction;
        r.speed = 0;
        r.goal = goal;
        r.routePlan = new RoutePlan(routePlan) ;
        r.routePlan.resetIndex() ;

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
    public double getDensity() {
        return density;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void setDensity(double _density) {
        density = _density;
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
     * prepareForSimulation
     */
    @Override
    public void prepareForSimulation(double _timeScale) {
        /* tkokada: modified to apply deserialize method */
        if (!isEvacuated()) {
            MapNode fromNode = current_link.getFrom();
            MapNode toNode = current_link.getTo();
            if ((fromNode.getDistance(calcNextTarget(fromNode, 
                                                     routePlan.duplicate(), 
                                                     false))
                 + position)
                >= (toNode.getDistance(calcNextTarget(toNode, 
                                                      routePlan.duplicate(), 
                                                      false)) +
                            current_link.length - position)) {
                prev_node = current_link.getFrom();
                next_node = current_link.getTo();
                direction = 1.0;
            } else {
                prev_node = current_link.getTo();
                next_node = current_link.getFrom();
                direction = -1.0;
            }
            speed = 0;
            routePlan.resetIndex() ;

            renavigate(routePlan); 
        }
    }

    //------------------------------------------------------------
    /**
     * preUpdate
     */
    @Override
    public void preUpdate(double time) {
        next_link_candidate = null;

        calc_speed(time);
        move_set(speed * direction, time, true);
    }

    //------------------------------------------------------------
    /**
     * set_swing
     */
    private void set_swing() {
        /* agent drawing */
        //TODO should no call here, as lane should be set up properly
        current_link.setup_lanes();

        int w = current_link.getLaneWidth(direction);
        int index = Collections.binarySearch(current_link.getLane(direction),
                                             this);
        if (isBackwardDirection())
            index = current_link.getLane(direction).size() - index;

        order_in_row = index;
        if (isForwardDirection()) {
            if (index >= 0) {
                swing_width = (2.0 * ((current_link.getLane(direction).size() - index) % w)) / current_link.width - 1.0;
            }  else {
                swing_width = 0.0;
            }
        } else {
            if (index >= 0) {
                swing_width = 1.0 - (2.0 * (index % w)) / current_link.width;
            }  else {
                swing_width = 0.0;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 次の位置を計算。
     * @param d : speed に相当する大きさ。単位時間に進める長さ。
     * @param time : 時間ステップ。1.0 が1秒。
     * @param will_move_out : 次のリンクに進むかどうか。WaitDirective 用。
     */
    protected boolean move_set(double d, double time, boolean will_move_out) {
        // 2013.02.21 tkokada ScenarioEvent STOP_TIMES
        if (current_link.isStopTimesEnabled()) {
            for (String tag : current_link.getTags()) {
                if (current_link.isStop(tag, time)) {
                    speed = 0;
                    next_position = position;
                    return true;
                }
            }
        }
        if (next_node.isStopTimesEnabled()) {
            for (String tag : next_node.getTags()) {
                if (next_node.isStop(tag, time)) {
                    if ((isForwardDirection() && 
                         speed >= (current_link.length - position)) ||
                            (isBackwardDirection() && speed >= position) ||
                            (speed == 0.0)) {
                        speed = 0.;
                        next_position = position;
                        return true;
                    }
                }
            }
        }

        double distance_to_move = d * time_scale;

        next_position = position + distance_to_move;
        double next_position_tmp = next_position;
        MapLink link = current_link;
        MapNode node = next_node;
        RoutePlan workingRoutePlan = routePlan.duplicate() ;

        while (next_position_tmp < 0 ||
                next_position_tmp > link.length) {
            if (will_move_out) {
                /* schedule moving out */
                MapLink next_link = navigate(time, link, node, 
                                             workingRoutePlan, true);
                if ((link.isOneWayPositive() ||
                     link.isOneWayNegative() ||
                     link.isRoadClosed())
                    && next_link == null) {
                    // 現在の道が一方通行か閉鎖で、先の道路が見つからなかったらアウト
                    break;
                }
                if (next_position_tmp < 0.0) {
                    distance_to_move = -next_position_tmp;
                } else {
                    distance_to_move = next_position_tmp - link.length;
                }
                node = next_link.getOther(node);
                link = next_link;

                if (link.isForwardDirectionTo(node)) {
                    next_position_tmp = distance_to_move;
                } else {
                    next_position_tmp = link.length - distance_to_move;
                }
            } else {
                // WAIT_FOR, WAIT_UNTIL によるエージェントの停止は下記でおこなう
                if (next_position < 0){
                    next_position = 0;
                } else {
                    next_position = current_link.length;
                }

                break;
            }
        }
        /* [2015.01.09 I.Noda]
         * 上記の registerEnter から移動。
         * ただし、current_link を渡すことが正しいかどうかは不明
         */
        if(link != current_link) {
            link.registerEnter(this, current_link);
        }
        return false;
    }

    //------------------------------------------------------------
    /**
     * move_commit
     */
    protected boolean move_commit(double time) {
        setPosition(next_position);

        while (position < 0.0 ||
                position > current_link.length) {
            if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
                next_node.hasTag(goal)){
                consumePlannedRoute();
                /* exit! */
                setEvacuated(true, time);
                prev_node = next_node;
                next_node = null;
                current_link.agentExits(this);
                current_link = null;

                return true;
            }
            double distance_to_move;

            if (position < 0.0) {
                distance_to_move = -position;
            } else {
                distance_to_move = position - current_link.length;
            }

            sane_navigation_from_node_forced = true;
            next_link_candidate = navigate(time, current_link, next_node,
                                           routePlan, false);
            sane_navigation_from_node_forced = true;

            tryToPassNode(time);

            setPosition(current_link
                        .calcAbstractPositionByDirectionTo(next_node,
                                                           distance_to_move)) ;
        }
        return false;
    }

    //------------------------------------------------------------
    /**
     * update
     */
    @Override
    public boolean update(double time) {
        if (time < generatedTime) {
            return false;
        }

        // tkokada
        // if (getPrevNode().hasTag(goal)) {
        //if (planned_route.size() <= getRouteIndex() &&
        //        getPrevNode().hasTag(goal)) {
        if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
            getPrevNode().hasTag(goal)){
            consumePlannedRoute();
            setEvacuated(true, time);
            return true;
        }
        // if (current_link.getTotalTriageLevel() > 5) {
        //     setEmergency();
        // }

        return move_commit(time);
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
            calc_speed_lane(time);
            break;
        case DensityModel:
            //calc_speed_density(time);
            calc_speed_density_reviced(time);
            break;
        default:
            break;
        }
        pollution.effect(this);
    }
    
    //------------------------------------------------------------
    /**
     * lane による速度計算
     */
    private void calc_speed_lane(double time) {

        double diff = 0;    // distance between myself and front of me
        double diff_base = 0;   // distance to next node.
        int passed_agent_count = 0;

        // possible link that fron agent is placed.
        MapLink link_to_find_agent = current_link;
        // is used to update link_to_fin_agent
        MapNode node_to_navigate = next_node;
        double distance_to_go = emptyspeed * time_scale; 
        RoutePlan workingRoutePlan = routePlan.duplicate() ;
        double direction_orig = direction;

        // N-th of this agents in current lane
        int index = 
            Collections.binarySearch(current_link.getLane(direction),
                                     this) ;

        while (diff == 0 && (distance_to_go > 0)) {
            ArrayList<EvacuationAgent> agents = link_to_find_agent.
                getLane(direction);

            if (agents.size() >= 1) {
                /* 今まで通過してきたリンク上にいた人を計算 */
                /* ??? */
                int w = link_to_find_agent.getLaneWidth(direction);
                int index_front = index;
                index_front += w - passed_agent_count;

                if (index_front < 0) {
                    /* リンクの幅が狭くなって，仮想レーン数が今まで通過してきた
                     * エージェント数よりも小さい
                     * この場合には，diff は diff_base */
                    diff = diff_base;
                    break;
                } else if (index_front < agents.size() && index_front >= 0) {
                    /* 今いる仮想レーン上にエージェントがいる */
                    EvacuationAgent agent_in_front = agents.get(index_front);

                    if (current_link == link_to_find_agent) {
                        /* 最初のリンクの場合 */
                        diff = Math.abs(agent_in_front.position - position);
                    } else {
                        /* 先読み */
                        if (isForwardDirection()) {
                            diff = agent_in_front.position;
                        } else {
                            diff = link_to_find_agent.length -
                                agent_in_front.position;
                        }
                        diff += diff_base;
                    }
                    break;
                }
                /* 繰り返すのは (index_front > agents.size) の場合
                 *  つまり，仮想レーン内には誰もいない状態 
                 */

            } // if agents.size >= 1

            /* 距離等の変数の更新 */
            if (link_to_find_agent == current_link) {
                if (isForwardDirection()) {
                    diff_base = current_link.length - position;
                } else { /* isBackwardDirection() */
                    diff_base = position;
                }
                passed_agent_count = agents.size() - index - 1;
                distance_to_go -= diff_base;
            } else {
                distance_to_go -= link_to_find_agent.length;
                diff_base += link_to_find_agent.length;
                passed_agent_count += agents.size();
            }

            /* update next link */
            link_to_find_agent = 
                sane_navigation_from_node(time, 
                                          link_to_find_agent, node_to_navigate,
                                          workingRoutePlan, false) ;
            if (link_to_find_agent == null) break;
            node_to_navigate = link_to_find_agent.getOther(node_to_navigate);

            /* direction の update */
            direction = 
                link_to_find_agent.directionValueTo(node_to_navigate, 1.0) ;
            index = -1;/* 次からは最後尾な気分で */
        }
        direction = direction_orig;

        /* calculation of speed, based on diff */
        double base_speed = emptyspeed;
        if (current_link.isStair() || getCurrentLink().hasTag("STAIR")) {
            base_speed *= STAIR_SPEED_CO;
        }

        dv = A_0 * (base_speed - speed);

        if (diff != 0) {

            //dv -= A_1 * Math.exp((PERSONAL_SPACE - diff)*A_2);

            dv -= A_1 * Math.exp(A_2 * (PERSONAL_SPACE - diff));

        }

        dv *= time_scale;
        speed += dv;

        if (speed > emptyspeed) {
            speed = emptyspeed;
        } else if (speed < 0) {
            speed = 0;
        }

        //******************
        //** 渋谷駅周辺の帰宅困難者再現用の信号
        //** かなり無茶な変更なので、すぐに撤去のこと

        if (getCurrentLink().hasTag("SIGNAL_WAITING")) {
            if (time % 60 < 30)
                speed = 0;
        }

        int w = current_link.getLaneWidth(direction);
        if (order_in_row < w) {
            if (isForwardDirection()) {
                if (((MapNode) current_link.getTo()).hasTag(goal)){
                    speed = emptyspeed;
                }
            }
            if (isBackwardDirection()) {
                if (((MapNode) current_link.getFrom()).hasTag(goal)){
                    speed = emptyspeed;
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * density による速度計算(改良版？）
     * tkokada
     */
    private void calc_speed_density_reviced(double time) {
        /* minimum distance between agents */
        double MIN_DISTANCE_BETWEEN_AGENTS = 0.3;
        /* mininum distance that agent can walk with max speed */
        double DISTANCE_MAX_SPEED =
            MAX_SPEED + MIN_DISTANCE_BETWEEN_AGENTS;    // 0.96 + 0.3
        /* the range to calculate the density */
        double DENSITY_RANGE = DISTANCE_MAX_SPEED * time_scale;
        /* minimum speed to break dead lock state */
        double MIN_SPEED_DEADLOCK = 0.3;

        ArrayList<EvacuationAgent> currentLinkAgents = current_link.getAgents();
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
            minRange = position;
            maxRange = Math.min(position + DENSITY_RANGE, current_link.length);
        } else {
            minRange = Math.max(position - DENSITY_RANGE, 0.0);
            maxRange = position;
        }

        /* in range agents on next link is calculated 
         * in between maxRange & minRange */
        //double nextMaxRange = 0;
        //double nextMinRange = 0;

        RoutePlan workingRoutePlan = routePlan.duplicate() ;
        MapLink nextLink = 
            sane_navigation_from_node(time, current_link, next_node,
                                      workingRoutePlan, false) ;

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
            if (agent.position == position) {
                samePlaceAgents.add(agent);
            }
            // agent が (minRange..maxRange) 内に位置する
            if (agent.position >= minRange && agent.position <= maxRange) {
                if (direction == agent.getDirection()) {
                    inRangeSameDirectionAgents += 1;
                } else {
                    inRangeOppositeDirectionAgents += 1;
                }
                // 以下、自分の隣か前を歩いている(歩いて来る) agent について(direction ごと)
                // agent is placed in front of this agent.
                if (isForwardDirection() && agent.position >= position) {
                    if (agent.isForwardDirection()) {
                        inFrontSameDirectionAgents += 1;
                    } else {
                        inFrontOppositeDirectionAgents += 1;
                    }
                    if (agent.position != position) {
                        if (frontAgent == null) {
                            frontAgent = agent;
                        } else if (frontAgent.position > agent.position) {
                            frontAgent = agent;
                        }
                    }
                // agent is placed in front of this agent.
                } else if (isBackwardDirection() && agent.position <= position) {
                    if (agent.isBackwardDirection()) {
                        inFrontSameDirectionAgents += 1;
                    } else {
                        inFrontOppositeDirectionAgents += 1;
                    }
                    if (agent.position != position) {
                        if (frontAgent == null) {
                            frontAgent = agent;
                        } else if (frontAgent.position < agent.position) {
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
        density = (inRangeSameDirectionAgents + inRangeOppositeDirectionAgents + 1) / (current_link.width * density_range);
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
        } else if (inFrontSameDirectionAgents + inFrontOppositeDirectionAgents < (int)current_link.width) {
            speed = MAX_SPEED;
        } else if (frontAgent != null) {
            double distance;
            if (currentLinkAgents.contains(frontAgent))
                distance = Math.abs(frontAgent.position - position);
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
            int numberLane = (int)current_link.width;
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
                int numberLane = (int)current_link.width;
                int counter = 0;
                for (EvacuationAgent agent : samePlaceAgents) {
                    if (agent.getSpeed() >= MAX_SPEED * 0.8)
                        counter += 1;
                }
                if (counter < numberLane) {
                    speed = MAX_SPEED * 0.8;
                }
                /*
                for (EvacuationAgent agent : samePlaceAgents) {
                    if (agent.getSpeed() > 0.0) {
                        existPlusSpeed = true;
                        break;
                    }
                }
                if (!existPlusSpeed) {
                    //speed = MIN_SPEED_DEADLOCK;
                    speed = MAX_SPEED * 0.8;
                }
                */
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
        MapLink currentLink = getCurrentLink();
        MapLink neighborLink = agent.getCurrentLink();

        if (currentLink.getFrom() == neighborLink.getFrom()) {
            distance = position + agent.position;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            distance = position + neighborLink.length - agent.position;
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            distance = currentLink.length - position + agent.position;
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            distance = currentLink.length - position + neighborLink.length - agent.position;
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
        MapLink currentLink = getCurrentLink();
        MapLink neighborLink = agent.getCurrentLink();

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
        MapLink currentLink = getCurrentLink();
        MapLink neighborLink = agent.getCurrentLink();

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
        MapLink currentLink = getCurrentLink();
        MapLink neighborLink = agent.getCurrentLink();

        if (currentLink.getFrom() == neighborLink.getFrom()) {
            if (position == 0.0 && agent.getPosition() == 0.0)
                return true;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            if (position == 0.0 && agent.getPosition() == neighborLink.length)
                return true;
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            if (position == currentLink.length && agent.getPosition() == 0.0)
                return true;
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            if (position == currentLink.length &&
                    agent.getPosition() == neighborLink.length)
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
        for (EvacuationAgent agent : current_link.getAgents()) {
            double tmp_distance = 0.0;
            if (isForwardDirection()) {
                if (agent.position > position) {
                    tmp_distance = agent.position - position;
                }
            } else {
                if (agent.position < position) {
                    tmp_distance = position - agent.position;
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
    protected boolean tryToPassNode(double time) {
        // [2015.01.02 I.Noda] ループの検出（検出だけでなにもしない）
        // 過去の route と同じかどうかのチェック。
        for (final CheckPoint point : route) {
            if (point.node == next_node) {
                if (navigation_reason != null) {
					navigation_reason.add("LOOP HERE!\n");
                }
                break;
            }
        }

        /* [2014.12.19 I.Noda] 
         * NaiveAgent への経路記録の入り口のため,
         * recordTrail を導入。
         */
        recordTrail(time) ;

        MapNode passingNode = next_node ;

        /* agent exits the previous link */
        getCurrentLink().agentExits(this);
        setPrevNode(passingNode) ;

        //2011年6月7日追加
        MapLink previous_link = current_link;

        /* agent enters the next link */
        setCurrentLink(next_link_candidate);

        // 2011年6月7日追加

        double direction_orig = direction;
        // tkokada
        calcNextTarget(passingNode, routePlan, false) ;
        if (next_link_candidate.isForwardDirectionFrom(passingNode)) {
            next_node = current_link.getTo();
            setPosition(random.nextDouble() / 100);
            direction = 1.0;
        } else {
            next_node = current_link.getFrom();
            setPosition(current_link.length - random.nextDouble() / 100);
            direction = -1.0;
        }
        getCurrentLink().agentEnters(this);
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
        if (next_link_candidate.width == previous_link.width &&
            passingNode.getPathways().size() == 2) {
            if (direction_orig == direction) {
                update_swing_flag = false;
            } else {
                update_swing_flag = false;
                swing_width *= -1;
            }
        } else {
            update_swing_flag = true;
        }
        if ((isPlannedRouteCompleted() || isRestAllRouteDirective()) &&
            current_link.hasTag(goal)){
            consumePlannedRoute();
            setEvacuated(true, time);
            current_link.agentExits(this);
        }

        return true;
    }

    //------------------------------------------------------------
    /**
     * 最終決定したルート、足跡情報の記録
     * [2014.12.19 I.Noda] tryToPassNode() より移動
     */
    protected void recordTrail(double time) {
		route.add(new CheckPoint(next_node, time, navigation_reason.toString()));
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
                               MapLink link_now,
                               MapNode node_now,
                               RoutePlan workingRoutePlan,
                               boolean on_node) {
        final MapLinkTable way_candidates = node_now.getPathways();

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

        MapLink target = sane_navigation(time, workingRoutePlan, on_node) ;
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
            calcNextTarget(next_node, workingRoutePlan, false);
        }
    }

    //------------------------------------------------------------
    /**
     * 
     */
    protected MapLink sane_navigation(double time,
                                      RoutePlan workingRoutePlan,
                                      boolean on_node) {
        MapLink way = 
            sane_navigation_from_node(time, current_link, next_node,
                                      workingRoutePlan, on_node) ;
        return way;
    }

    //------------------------------------------------------------
    /**
     * ノードにおいて、次の道を選択するルーチン
     */
    protected MapLink sane_navigation_from_node(double time, 
                                                MapLink link, MapNode node,
                                                RoutePlan workingRoutePlan,
                                                boolean on_node) {
        // 前回の呼び出し時と同じ結果になる場合は不要な処理を回避する
        if (isSameSituationForSaneNavigationFromNode(link,node))
            return sane_navigation_from_node_result;
        backupSituationForSaneNavigationFromNodeBefore(link, node) ;

        MapLinkTable way_candidates = node.getPathways();
        double min_cost = Double.MAX_VALUE;
        double min_cost_second = Double.MAX_VALUE;
        MapLink way = null;
        MapLink way_second = null;

        boolean monitor = next_node.hasTag("MONITOR-NAVIGATION");

        MapLinkTable way_samecost = null;

        final Term next_target = calcNextTarget(node, workingRoutePlan, on_node) ;

        if (monitor)
            System.err.println("navigating at " + node.getTagString() + " for " + next_target);
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
                workingRoutePlan.shift() ;
				navigation_reason.add("found mid-goal in").add(way_candidate) ;
                break;
            } 

            // 現在の way_candidate を選択した場合の next_target までのコスト計算
            double cost = calcWayCostTo(way_candidate, node, next_target) ;

            if (monitor)
                System.err.print(way_candidate.getTagString() +
                    "\t" + cost + "\t" +cost +
                    "(" + way_candidate.crowdness() + ")\t"
                    + way_candidate.spaceLeft(node, 1.0));
			navigation_reason.add(way_candidate.getOther(node))
				.add("(").add(cost).add(") ");

            if (cost < min_cost) { // 最小cost置き換え
                min_cost = cost;
                way = way_candidate;
                way_samecost = null;
            } else if (cost == min_cost) { // 最小コストが同じ時の処理
                if (way_samecost == null)
                    way_samecost = new MapLinkTable();
                way_samecost.add(way_candidate);
            }
        }

        if (way_samecost != null) {
            //int i = (int)(Math.random() * way_samecost.size());
            int i = (int)(random.nextDouble() * way_samecost.size());
            if (i != way_samecost.size()) {
                way = way_samecost.get(i);
            }
        }

        if (way == null) {
            way = way_second;
        }
        backupSituationForSaneNavigationFromNodeAfter(way) ;

        if (way == null) {
            return null;
        }

		navigation_reason.add("\n -> chose").add(way.getOther(node)) ;
        return way;
    }

    //------------------------------------------------------------
    /**
     * 前回の呼び出し時と同じ条件かどうかのチェック
     */
    private boolean isSameSituationForSaneNavigationFromNode(MapLink link, 
                                                             MapNode node) {
        boolean forced = sane_navigation_from_node_forced ;
        sane_navigation_from_node_forced = false ;
        return (!forced &&
                sane_navigation_from_node_current_link == current_link &&
                sane_navigation_from_node_link == link &&
                sane_navigation_from_node_node == node &&
                emptyspeed < (isForwardDirection() ?
                              current_link.length - position :
                              position)) ;
    }

    //------------------------------------------------------------
    /**
     * 上記のチェックのための状態バックアップ(before)
     */
    private void backupSituationForSaneNavigationFromNodeBefore(MapLink link,
                                                                MapNode node) {
        sane_navigation_from_node_current_link = current_link;
        sane_navigation_from_node_link = link;
        sane_navigation_from_node_node = node;
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
    protected Term calcNextTarget(MapNode node, RoutePlan workingRoutePlan,
                                  boolean on_node) {
        if (on_node &&
            !workingRoutePlan.isEmpty() &&
            node.hasTag(workingRoutePlan.top())){
            /* reached mid-goal */
            workingRoutePlan.shift() ;
        }

        while (!workingRoutePlan.isEmpty()) {
            Term candidate = workingRoutePlan.top() ;
            if (node.getHint(candidate) != null) {
                return candidate;
            }
            Itk.dbgWrn("no mid-goal set for " + candidate);
            workingRoutePlan.shift() ;
        }

        return goal;
    }

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * ここを変えると、経路選択の方法が変えられる。
     */
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) {
        MapNode other = _way.getOther(_node);
        double cost = other.getDistance(_target) ;
        cost += _way.length;
        return cost ;
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
        if (current_link == null) return;

        Point2D p = getPos();
        final double minHight = ((MapPartGroup)current_link.getParent()).getMinHeight();
        final double maxHight = ((MapPartGroup)current_link.getParent()).getMaxHeight();
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
