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

import nodagumi.Itk.*;

/** TargetRoundPerson has multiple targets where make a round.
 */
//public class TargetRoundPerson extends EvacuationAgent
public class TargetRoundPerson extends RunningAroundPerson
    implements Serializable {
    private static final long serialVersionUID = -6313717392123377059L;

    static final boolean DELAY_LOOP = false;
    static final boolean debug_mode = false;

    /* Initial values */
    protected double emptyspeed = V_0;
    protected double time_scale = 1.0;//0.5;//1.0;//0.5; simulation time step 

    /* Values used in simulation */
    protected double speed;
    protected double direction;
    protected double density;

    protected int order_in_row;

    class CheckPoint implements Serializable {
        public MapNode node;
        public double time;
        public String reason;
        public CheckPoint(MapNode _node, double _time, String _reason) {
            node = _node; time = _time; reason = _reason;
        }
    }

    protected ArrayList<CheckPoint> route;

    /* Values used for navigation */
    protected Term goal;
    protected ArrayList<Term> planned_route = new ArrayList<Term>();
    protected int routeIndex;
    
    public static String getTypeName() {
        return "TargetRoundPerson";
    }
    
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public TargetRoundPerson() {} ;

    public TargetRoundPerson(int _id, Random _random) {
        init(_id, _random) ;
    }

    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id, Random _random) {
        super.init(_id, _random);
        route = new ArrayList<CheckPoint>();
    }

    @Override
    public EvacuationAgent copyAndInitialize() {
        TargetRoundPerson r = new TargetRoundPerson(0, random);
        r.ID = ID;
        r.generatedTime = generatedTime;
        r.emptyspeed = emptyspeed;
        r.prev_node = prev_node;
        r.next_node = next_node;
        r.current_link = current_link;
        r.position = position;
        r.direction = direction;
        r.speed = 0;
        r.goal = goal;
        r.planned_route = planned_route;
        r.setRouteIndex(0);
        r.random = super.random;
        for (String tag : tags) {
            r.addTag(tag);
        }

        return r;
    }

    public TargetRoundPerson(int _id,
            double _emptySpeed,
            double _confidence,
            double _maxAllowedDamage,
            double _generatedTime,
            Random _random) {
        init(_id, _emptySpeed, _confidence, _maxAllowedDamage, _generatedTime,
             _random) ;
    }

    /**
     * 初期化。constractorから分離。
     */
    @Override
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

    MapLink next_link_candidate = null;
    boolean on_node = false;

    private double next_position = 0.0;

    private boolean passed_node = true;
    protected boolean move_commit(double time) {
        setPosition(next_position);
        while (position < 0.0 ||
                position > current_link.length) {
            /* got out of link */
            if (next_node.hasTag(goal)){
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
            next_link_candidate = navigate(time, current_link, next_node);

            if (!tryToPassNode(time)) {
                return false;
            }
            setPosition(current_link
                        .calcAbstractPositionByDirectionTo(next_node,
                                                           distance_to_move)) ;
        }
        return false;
    }


    @Override
    public boolean update(double time) {
        if (time < generatedTime) {
            return false;
        }

        if (getPrevNode().hasTag(goal)){
            setEvacuated(true, time);
            return true;
        }
        if (current_link.getTotalTriageLevel() > 5) {
            setEmergency();
        }

        return move_commit(time);
    }
    protected EvacuationAgent agent_in_front;
    protected static double A_0 = 0.962;//1.05;//0.5;
    protected static double A_1 = 0.869;//1.25;//0.97;//2.0;
    protected static double A_2 = 4.682;//0.81;//1.5;
    protected static double V_0 = 1.02265769054586;

    protected static double PERSONAL_SPACE = 2.0 * 0.522;//0.75;//0.8;
    protected static double STAIR_SPEED_CO = 0.6;//0.7;

    static final double SPEED_VIEW_RATIO = 10.0;
    private void calc_speed_lane(double time) {

        double dv = 0;
        double diff = 0;    // distance between myself and front of me
        double diff_base = 0;   // 
        int passed_agent_count = 0;

        MapLink link_to_find_agent = current_link;
        MapNode node_to_navigate = next_node; 
        double distance_to_go = emptyspeed * time_scale * SPEED_VIEW_RATIO * 10;

        int route_index_orig = getRouteIndex();
        double direction_orig = direction;

        // N-th of this agents in current lane
        int index = Collections.binarySearch(current_link.getLane(direction),
                this);

        //System.err.println("step = "+time);

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
                    agent_in_front = agents.get(index_front);

                    if (current_link == link_to_find_agent) {
                        /* 最初のリンクの場合 */
                        diff = Math.abs(agent_in_front.position - position);
                        // System.err.print("front " +
                        // agent_in_front.agentNumber);
                    } else {
                        /* 先読み */
                        if (direction > 0) {
                            diff = agent_in_front.position;
                            // System.err.print("front " +
                            // agent_in_front.agentNumber);
                        } else {
                            diff = link_to_find_agent.length -
                                agent_in_front.position;
                            // System.err.print("front " +
                            // agent_in_front.agentNumber);
                        }
                        diff += diff_base;
                    }
                    break;
                }
                /* 繰り返すのは (index > agents.size) の場合
                 *  つまり，仮想レーン内には誰もいない状態 
                 */

            } // if agents.size >= 1

            /* 距離等の変数の更新 */
            if (link_to_find_agent == current_link) {
                if (direction > 0.0) {
                    diff_base = current_link.length - position;
                } else { /* direction < 0.0 */
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
            link_to_find_agent = sane_navigation_from_node(time,
                    link_to_find_agent, node_to_navigate);
            if (link_to_find_agent == null) break;
            node_to_navigate = link_to_find_agent.getOther(node_to_navigate);

            /* direction の update */
            if (link_to_find_agent.isForwardDirectionTo(node_to_navigate)) {
                direction = 1.0;
            } else {
                direction = -1.0;
            }
            
            index = -1;/* 次からは最後尾な気分で */
        }
        setRouteIndex(route_index_orig);
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
        
        /* for debug:
         *  add "TRAFFIC_JAM" tag to link...
         * */
        if (getCurrentLink().hasTag("TRAFFIC_JAM")) {
            System.err.println(agentNumber + "\t" 
                    + position + "\t"
                    + diff + "\t"
                    + dv);
        }

        speed += dv * time_scale;
        
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
        /* tkokada
        System.err.println(
                " link "+current_link.getTags()+
                "\t position "+position+
                "\t diff "+diff+
                "\t speed "+speed+
                "\t order "+order_in_row+
                "\t direction "+direction);
        */
    }

    /* try to pass a node, and enter next link */
    protected boolean tryToPassNode(double time) {
        for (final CheckPoint point : route) {
            if (point.node == next_node) {
                if (navigation_reason != null) {
                    navigation_reason.add("LOOP HERE!\n");
                }
                if (debug_mode) {
                    Term mid_goal =
                        (!routePlan.isEmpty() ?
                         routePlan.top() : goal);
                    System.err.println("agent going to the same " +
                                       "room " + next_node.getTagString() +
                                       "(" + next_node.getHeight() + ") on " +
                                       time + " when going for " + mid_goal);
                    for (final CheckPoint passed_points : route) {
                        System.err.print(passed_points.node + "(" +
                                         passed_points.time + ") ");
                    }
                    System.err.println();
                }
                break;
            }
        }
        route.add(new CheckPoint(next_node, time, navigation_reason.toString()));

        /* agent exits the previous link */
        getCurrentLink().agentExits(this);
        setPrevNode(next_node);

        //2011年6月7日追加
        MapLink previous_link = current_link;
        /* agent enters the next link */
        setCurrentLink(next_link_candidate);

        
        //2011年6月7日追加
        MapNode nodeToward;
        if (next_link_candidate.isForwardDirectionFrom(getPrevNode())) {
            nodeToward = current_link.getTo();
        } else {
            nodeToward = current_link.getFrom();
        }
        final MapLinkTable way_candidates = nodeToward.getPathways();   
        double direction_orig = direction;
        //----
        if (next_link_candidate.isForwardDirectionFrom(getPrevNode())) {
            next_node = current_link.getTo();
            //setPosition(Math.random() / 100);
            setPosition(random.nextDouble() / 100);
            direction = 1.0;
        } else {
            next_node = current_link.getFrom();
            //setPosition(current_link.length - Math.random() / 100);
            setPosition(current_link.length - random.nextDouble() / 100);
            direction = -1.0;
        }
        getCurrentLink().agentEnters(this);
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
        /* [2014.12.26 I.Noda] update_swing_flag を使っていないので、簡単化。
         */
        if(next_link_candidate.width == previous_link.width &&
           way_candidates.size()== 2){
            if(direction_orig!=direction){
                swing_width *= -1;
            }
        }
        /* control the speed of agent */
        //if (currentPathway.isStair()|| getCurrentPathway().hasTag("STAIR")) {
        //  speed *= STAIR_SPEED_CO;
        //}
        if (current_link.hasTag(goal)){
            System.err.println("the goal should not be a link!!");
            setEvacuated(true, time);
            current_link.agentExits(this);
        }

        return true;
    }

    @Override
    public MapLinkTable getReachableLinks(double d, double time, double
            duration) {
        return null;
    }
}
