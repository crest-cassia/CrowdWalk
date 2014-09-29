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
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;

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

    static final boolean DELAY_LOOP = false;
    static final boolean debug_mode = false;

    /* Initial values */
    protected double emptyspeed = V_0;
    protected double time_scale = 1.0;//0.5; simulation time step 
    /* the max speed of agent */
    public static double MAX_SPEED = 0.96;
    public static double ZERO_SPEED = 0.1;

    /* Values used in simulation */
    protected double speed;
    protected double dv = 0.0;
    protected double direction = 0.0;
    protected double density;
    protected double expectedDensityMacroTimeStep = 300;

    protected int order_in_row;

    class CheckPoint implements Serializable {
        public MapNode node;
        public double time;
        public String reason;
        public CheckPoint(MapNode _node, double _time, String _reason) {
            node = _node; time = _time; reason = _reason;
        }
    }

    // 通過したノードの履歴?
    protected ArrayList<CheckPoint> route;

    /* Values used for navigation */
    protected String goal;
    protected ArrayList<String> planned_route = new ArrayList<String>();
    protected int routeIndex;

    // sane_navigation_from_node の不要な呼び出し回避用
    private boolean sane_navigation_from_node_forced = true;
    private MapLink sane_navigation_from_node_current_link;
    private MapLink sane_navigation_from_node_link;
    private MapNode sane_navigation_from_node_node;
    private MapLink sane_navigation_from_node_result;

    public static String getTypeName() {
        return "RunningAroundPerson";
    }

    public RunningAroundPerson(int _id, Random _random) {
        super(_id, _random);
        update_swing_flag = true;
        route = new ArrayList<CheckPoint>();
    }

    @Override
    public EvacuationAgent copyAndInitialize() {
        RunningAroundPerson r = new RunningAroundPerson(0, random);
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

    public RunningAroundPerson(int _id,
            double _emptySpeed,
            double _confidence,
            double _maxAllowedDamage,
            double _generatedTime,
            Random _random) {
        this(_id, _random);
        
        generatedTime = _generatedTime;
        emptyspeed = _emptySpeed;
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("run");

        buffer.append("," + this.ID);
        buffer.append("," + this.agentNumber);
        buffer.append("," + this.emptyspeed);
        buffer.append("," + this.time_scale);

        buffer.append("," + this.generatedTime);
        buffer.append("," + this.finishedTime);

        buffer.append("," + this.current_link.ID);
        buffer.append("," + this.position);
        buffer.append("," + this.direction);
        buffer.append("," + this.speed);
        buffer.append("," + this.accumulatedExposureAmount);
        buffer.append("," + this.goal);
        buffer.append("," + this.routeIndex);
        buffer.append("," + this.planned_route.size());
        for (String r : this.planned_route) {
            buffer.append("," + r);
        }
        buffer.append("," + this.tags.size());
        for (String tag : this.tags) {
            buffer.append("," + tag);
        }
        
        return buffer.toString();
    }

    static public EvacuationAgent fromString(String str, NetworkMap map,
            Random _random) {
        String[] items = str.split(",");
        if (!items[0].equals("run")) { return null; }

        RunningAroundPerson agent = new RunningAroundPerson(
                Integer.parseInt(items[1]), _random);
        agent.agentNumber = Integer.parseInt(items[2]);
        agent.emptyspeed = Double.parseDouble(items[3]);
        agent.time_scale = Double.parseDouble(items[4]);

        agent.generatedTime = Double.parseDouble(items[5]);
        agent.finishedTime = Double.parseDouble(items[6]);

        MapLink link = (MapLink)map.getObject(Integer.parseInt(items[7]));
        agent.current_link = link;
        agent.position = Double.parseDouble(items[9]);
        agent.direction = Double.parseDouble(items[9]);
        if (agent.direction > 0.0) {
            agent.prev_node = link.getFrom();
            agent.next_node = link.getTo();
        } else {
            agent.prev_node = link.getTo();
            agent.next_node = link.getFrom();
        }
        agent.speed = Double.parseDouble(items[10]);
        agent.accumulatedExposureAmount = Double.parseDouble(items[11]);
        agent.goal = items[12];
        agent.routeIndex = Integer.parseInt(items[13]);
        int route_size = Integer.parseInt(items[14]);
        for (int i = 0; i < route_size; i++) {
            agent.planned_route.add(items[15 + i]);
        }
        int tags_base = route_size + 15;
        int tags_size = Integer.parseInt(items[tags_base]);
        for (int i = 0; i < tags_size; i++) {
            agent.addTag(items[tags_base + i + 1]);
        }
        return agent;
    }

    private void turnAround() {
        direction = -direction;
        MapNode tmp = prev_node;
        prev_node = next_node;
        next_node = tmp;
    }
    
    @Override
    public void prepareForSimulation(double _timeScale) {
        // tkokada
        /*
        boolean deserialized = true;
        System.err.println("RunningAroundPerson.prepareForSimulation " +
                ID + " evacuated: " + isEvacuated() + "current_link: " +
                current_link);
        if (!deserialized) {
            prev_node = current_link.getFrom();
            next_node = current_link.getTo();
            speed = 0;
            direction = 1.0;
            setRouteIndex(0);

            renavigate();
        } else {
        */
        /* tkokada: modified to apply deserialize method */
        if (!isEvacuated()) {
            MapNode fromNode = current_link.getFrom();
            MapNode toNode = current_link.getTo();
            if ((fromNode.getDistance(calc_next_target(fromNode)) + position)
                    >= (toNode.getDistance(calc_next_target(toNode)) +
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
            setRouteIndex(0);

            renavigate();   // ※何もしていない(route_index は増える可能性あり)
        }
    }

    MapLink next_link_candidate = null;
    boolean on_node = false;
    @Override

    public void preUpdate(double time) {
        next_link_candidate = null;

        calc_speed(time);
        if (calculation_model == SpeedCalculationModel.ExpectedDensityModel)
            move_set(time, expectedDensityMacroTimeStep);
        else
            move_set(speed * direction, time, true);
    }

    private void set_swing() {
        /* agent drawing */
        //TODO should no call here, as lane should be set up properly
        current_link.setup_lanes();

        int w = current_link.getLaneWidth(direction);
        int index = Collections.binarySearch(current_link.getLane(direction),
                                             this);
        if (isNegativeDirection())
            index = current_link.getLane(direction).size() - index;

        order_in_row = index;
        if (isPositiveDirection()) {
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

    private double next_position = 0.0;
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
            // System.err.println("    stop time enabled");
            for (String tag : next_node.getTags()) {
                // System.err.println("tag: " + tag);
                if (next_node.isStop(tag, time)) {
                    // System.err.println(" stop tag: " + tag + ", time: " + time + ", direction: " + direction + ", speed: " + speed + ", position: " + position + ", link len: " + current_link.length);
                    if ((isPositiveDirection() && speed >= (current_link.length - position)) ||
                            (isNegativeDirection() && speed >= position) ||
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
        int route_index_orig = getRouteIndex();

        while (next_position_tmp < 0 ||
                next_position_tmp > link.length) {
            if (will_move_out) {
                /* schedule moving out */
                on_node = true;
                MapLink next_link = navigate(time, link, node);
                if ((link.hasTag("ONE-WAY-POSITIVE") ||
                            link.hasTag("ONE-WAY-NEGATIVE")) &&
                        next_link == null) {
                    break;
                } else if (link.ID == next_link.ID) {
                    break;
                }
                next_link.registerEnter(this, link);
                on_node = false;
                if (next_position_tmp < 0.0) {
                    distance_to_move = -next_position_tmp;
                } else {
                    distance_to_move = next_position_tmp - link.length;
                }
                node = next_link.getOther(node);
                link = next_link;

                if (node == link.getTo()) {
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
        setRouteIndex(route_index_orig);
        return false;
    }

    HashMap<MapLink, ArrayList<Double>> expectedDensityTimes = new
        HashMap<MapLink,ArrayList<Double>>(); /** ArrayList[0]: speed(m/sec),
                                                [1]: time(sec),
                                                [2]: direction(sec) */
    /** move_set for expected density model
     * @param d speed
     * @param time current time
     * @param duration macro time step
     */
    boolean move_set(double time, double duration) {
        // tkokada *** should be modified ***
        // If the agent is placed the link with ROUND_SPEED_ZERO tag, the speed
        // becomes zero every 60 seconds.
        /*if (current_link.hasTag("ROUND_SPEED_ZERO"))
            if (((int) ((int) time / 60) % 2) == 0) {
                speed = 0;
                next_position = position;
                return true;
            }
        */
        MapLink link = current_link;
        MapNode node = next_node;
        int route_index_orig = getRouteIndex();

        expectedDensityTimes.clear();
        for (MapLink routeLink : reachableLinks) {
            if (link != routeLink) {
                // System.err.println("\tlink: " + link.toString() + " not " +
                        // "match routeLink: " + routeLink.toString());
                // System.err.println("\t" + reachableLinks.toString());
                System.err.println("\tlink: " + link.ID + " not " +
                        "match routeLink: " + routeLink.ID + " current: " +
                        current_link.ID);
                System.err.print("\t");
                for (MapLink linkk : reachableLinks)
                    System.err.print(linkk.ID + " ");
                System.err.println();
                continue;
            } else {
                // System.err.println("\tlink: " + link.toString() +
                // " match " + routeLink.toString());
                // System.err.println("\t\tlink: " + link.ID + " " +
                        // "match routeLink: " + routeLink.ID + " current: " +
                        // current_link.ID);
                // System.err.print("\t\t");
                // for (MapLink linkk : reachableLinks)
                    // System.err.print(linkk.ID + " ");
                // System.err.println();
            }
            ArrayList<Double> speedTimeDirection = new ArrayList<Double>();
            double linkSpeed = ((Double) expectedDensitySpeeds.get(routeLink))
                .doubleValue();
            speedTimeDirection.add(new Double(linkSpeed));
            if (routeLink == current_link) {
                if (next_node == current_link.getFrom()) {
                    speedTimeDirection.add(new Double(position / linkSpeed));
                    speedTimeDirection.add(new Double(-1.0));
                } else if (next_node == current_link.getTo()) {
                    speedTimeDirection.add(new Double(
                                (routeLink.length - position) / linkSpeed));
                    speedTimeDirection.add(new Double(1.0));
                } else {
                    System.err.println("\tmove_set current_link invalid");
                    break;
                }
            } else {
                speedTimeDirection.add(new Double(routeLink.length /
                            linkSpeed));
                if (node == routeLink.getFrom()) {
                    speedTimeDirection.add(new Double(-1.0));
                } else if (node == routeLink.getTo()) {
                    speedTimeDirection.add(new Double(1.0));
                } else {
                    System.err.println("\tmove_set current_link invalid!");
                    break;
                }
            }
            on_node = true;
            MapLink next_link = virtual_navigate(time, link, node);
            //next_link.registerEnter(this, link);
            // if ((link.hasTag("ONE-WAY-POSITIVE") ||
                        // link.hasTag("ONE-WAY-NEGATIVE")) &&
                    // next_link == null) {
                // System.err.println("\t\t\toneway null!");
                // break;
            // } else if (link.ID == next_link.ID) {
                // System.err.println("\t\t\tlink & next is same!");
                // break;
            // } else if (next_link == null) {
                // System.err.println("\t\t\tnext is null!");
                // break;
            // }
            on_node = false;
            node = next_link.getOther(node);
            link = next_link;
            expectedDensityTimes.put(routeLink, speedTimeDirection);
        }
        setRouteIndex(route_index_orig);

        return false;
    }


    /** Reachable links in a duration */
    public ArrayList<MapLink> reachableLinks = new ArrayList<MapLink>();

    /** directions corresponding to reachableLinks */
    public HashMap<MapLink, Double> reachableLinkDirections =
        new HashMap<MapLink, Double>();

    /** Calculate reachable links in specified duration time with the speed
     * @param d free speed
     * @param time current time
     * @param duration simulation step
     * @return reachable links in a duration
     */
    @Override
    public ArrayList<MapLink> getReachableLinks(double d, double time,
            double duration) {

        if (isEvacuated())
            return null;

        reachableLinks.clear();
        reachableLinkDirections.clear();
        if (current_link == null) {
            System.err.println("RunningAroundPerson.getReachableLinks " +
                    "current_link null!");
            return null;
        }
        reachableLinks.add(current_link);
        if (next_node == current_link.getFrom())
            reachableLinkDirections.put(current_link, new Double(-1.0));
        else
            reachableLinkDirections.put(current_link, new Double(1.0));

        double distance_to_move = d * duration;
        MapLink link = current_link;
        MapNode node = next_node;
        // store values to undo
        int route_index_orig = getRouteIndex();
        double direction_orig = direction;
        MapNode prev_node_orig = prev_node;
        MapNode next_node_orig = next_node;
        MapLink current_link_orig = current_link;

        while (distance_to_move > 0) {
            // System.err.println("\t\tdistance_to_move: " + distance_to_move);
            // System.err.println("\t\tlink: " + link + " node: " + node);
            if (link == current_link) {
                if (node == link.getFrom()) {
                    if (position >= distance_to_move)
                        break;
                    distance_to_move -= position;
                } else {
                    if (link.length - position >= distance_to_move)
                        break;
                    distance_to_move -= link.length - position;
                }
            } else {
                if (link.length >= distance_to_move)
                    break;
                distance_to_move -= link.length;
            }
            on_node = true;
            MapLink next_link = virtual_navigate(time, link, node);
            if ((link.hasTag("ONE-WAY-POSITIVE") ||
                        link.hasTag("ONE-WAY-NEGATIVE")) &&
                    next_link == null) {
                break;
            } else if (link.ID == next_link.ID) {
                break;
            } else if (next_link == null) {
                break;
            }
            on_node = false;
            node = next_link.getOther(node);
            link = next_link;
            reachableLinks.add(link);
            if (node == link.getFrom())
                reachableLinkDirections.put(link, new Double(-1.0));
            else
                reachableLinkDirections.put(link, new Double(1.0));
            // System.err.println("\t\tnext_link: " + next_link + " link: " +
            //         link + " node: " + node);
        }
        setRouteIndex(route_index_orig);

        return reachableLinks;
    }

    private boolean passed_node = true;
    protected boolean move_commit(double time) {
        setPosition(next_position);

        while (position < 0.0 ||
                position > current_link.length) {
            /* got out of link */
            // tkokada
            // if (next_node.hasTag(goal)) {
            //if (planned_route.size() <= getRouteIndex() &&
            //        next_node.hasTag(goal)) {
            if ((isPlannedRouteCompleted() || isRemainingRouteWAIT_()) && next_node.hasTag(goal)) {
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
            next_link_candidate = navigate(time, current_link, next_node);
            sane_navigation_from_node_forced = true;

            if (!tryToPassNode(time)) {
                return false;
            }

            if (next_node == current_link.getTo()) {
                setPosition(distance_to_move);
            } else {
                setPosition(current_link.length - distance_to_move);
            }
        }
        return false;
    }

    /** move_commit for expected density speed model */
    protected boolean move_commit(double time, double duration) {
        boolean isDebug = false;

        if (isDebug) {
            System.err.println("RunningAroundPerson.move_commit " +
                    "start current link: " + current_link + " position: " +
                    position + " reachable links: " + reachableLinks);
        }
        // check goal
        // if (planned_route.size() <= getRouteIndex() &&
                // next_node.hasTag(goal)) {
            // if ((position == 0 && current_link.getFrom().hasTag(goal)) ||
                    // (position == current_link.length &&
                     // current_link.getTo().hasTag(goal))) {
                // setEvacuated(true, time);
                // prev_node = next_node;
                // next_node = null;
                // current_link.agentExits(this);
                // current_link = null;

                // return true;
            // }
        // }
        double remain = duration;
        for (MapLink link : reachableLinks) {
            ArrayList<Double> speedTimeDirection = expectedDensityTimes
                .get(link);
            if (speedTimeDirection == null) {
                System.err.println("\tspeedTimeDirection link: " + link +
                        " cannot find in " +
                        expectedDensityTimes);
            }
            if (isDebug) {
                System.err.println("\tspeedTimeDirection link: " +
                        link + " can find: " + speedTimeDirection);
            }
            double linkSpeed = ((Double) speedTimeDirection.get(0))
                .doubleValue();
            double linkTime = ((Double) speedTimeDirection.get(1))
                .doubleValue();
            double linkDirection = ((Double) speedTimeDirection.get(2))
                .doubleValue();
            if (remain < linkTime) {
                if (linkDirection > 0) {
                    setPosition(linkSpeed * remain + position);
                } else {
                    setPosition(position - linkSpeed * remain);
                }
                if (isDebug) {
                    System.err.println("\tRunningAroundPerson.move_commit " +
                            "reach next position link: " + link +
                            " position: " + position + " remain: " + remain);
                }
                break;
            }
            //if (planned_route.size() <= getRouteIndex() &&
            //        next_node.hasTag(goal) &&
            //        remain >= linkTime) {
            if ((isPlannedRouteCompleted() || isRemainingRouteWAIT_()) &&
                    next_node.hasTag(goal) && remain >= linkTime) {
                consumePlannedRoute();
                setEvacuated(true, time);
                prev_node = next_node;
                next_node = null;
                current_link.agentExits(this);
                current_link = null;

                return true;
            }
            remain -= linkTime;
            sane_navigation_from_node_forced = true;
            next_link_candidate = navigate(time, current_link, next_node);
            sane_navigation_from_node_forced = true;
            if (!tryToPassNode(time)) {
                return false;
            }
            if (next_node == current_link.getTo()) {
                setPosition(0);
            } else {
                setPosition(current_link.length);
            }
        }

        return false;
    }


    @Override
    public boolean update(double time) {
        if (time < generatedTime) {
            return false;
        }

        // tkokada
        // if (getPrevNode().hasTag(goal)) {
        //if (planned_route.size() <= getRouteIndex() &&
        //        getPrevNode().hasTag(goal)) {
        if ((isPlannedRouteCompleted() || isRemainingRouteWAIT_()) && getPrevNode().hasTag(goal)) {
            consumePlannedRoute();
            setEvacuated(true, time);
            return true;
        }
        if (current_link.getTotalTriageLevel() > 5) {
            setEmergency();
        }

        if (calculation_model == SpeedCalculationModel.ExpectedDensityModel)
            return move_commit(time, expectedDensityMacroTimeStep);
        else
            return move_commit(time);
    }

    boolean update_swing_flag = true;//false;
    @Override
    public void updateViews() {
        if (update_swing_flag) {
            update_swing_flag = false;
            set_swing();
        }
    }

    private Ellipse2D getCircle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx -r, cy -r, r * 2, r * 2);
    }

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

    protected EvacuationAgent agent_in_front;

    public static enum SpeedCalculationModel {
        LaneModel,
        /* DensityModel:
         * calc speed with density (number of persion / m^2).
         *  v: m/sec, rho: number of person / m^2
         *  sinplex road: v = 1.2 - 0.25 * rho
         *  duplex road : v = 1.27 * 10 ^ (-0.22 * rho)
         *  duplex road : v = 1.25 - 0.476 * rho
         */
        DensityModel,
        ExpectedDensityModel
    }
    private SpeedCalculationModel calculation_model =
        SpeedCalculationModel.LaneModel;
    //private SpeedCalculationModel calculation_model =
    //SpeedCalculationModel.DensityModel;


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
    
    protected void calc_speed(double time) {
        switch (calculation_model) {
        case LaneModel:
            calc_speed_lane(time);
            //System.err.println("Lane");
            break;
        case DensityModel:
            //calc_speed_density(time);
            calc_speed_density_reviced(time);
            //System.err.println("Density");
            break;
        case ExpectedDensityModel:
            calc_speed_expected_density(time);
            //System.err.println("Expected");
            break;
        default:
            break;
        }
        //debug
        // if (speed < 1.2 && speed > 0.3)
            // System.err.println("  agent: " + ID + ", speed; " + speed);
        // pollution の影響による移動速度の低下
        pollution.effect(this);
    }
    
    static final double SPEED_VIEW_RATIO = 10.0;

    private void calc_speed_lane(double time) {

        double diff = 0;    // distance between myself and front of me
        double diff_base = 0;   // distance to next node.
        int passed_agent_count = 0;

        // possible link that fron agent is placed.
        MapLink link_to_find_agent = current_link;
        // is used to update link_to_fin_agent
        MapNode node_to_navigate = next_node;
        double distance_to_go = emptyspeed * time_scale;    // * SPEED_VIEW_RATIO * 10;
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
                        if (isPositiveDirection()) {
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
                if (isPositiveDirection()) {
                    diff_base = current_link.length - position;
                } else { /* isNegativeDirection() */
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
            if (node_to_navigate == link_to_find_agent.getTo()) {
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
        // tkokada
        /*
        System.err.println(
                " link "+current_link.getTags()+
                "\t position "+position+
                "\t diff "+diff+
                "\t speed "+speed+
                "\t order "+order_in_row+
                "\t swing_flag "+update_swing_flag+
                //"\t next_target " +  calc_next_target(next_node) +
                "\t next_target " + goal +
                "\t direction "+direction);
        //*/
        //calc_next_target(next_node);
        int w = current_link.getLaneWidth(direction);
        if (order_in_row < w) {
            if (isPositiveDirection()) {
                if (((MapNode) current_link.getTo()).hasTag(goal)) {
                    speed = emptyspeed;
                    // System.err.println("head agent speed is modified, goal: " + goal);
                }
            }
            if (isNegativeDirection()) {
                if (((MapNode) current_link.getFrom()).hasTag(goal)) {
                    speed = emptyspeed;
                    // System.err.println("head agent speed is modified. goal: " + goal);
                }
            }
        }
    }

    // tkokada
    private void calc_speed_density_reviced(double time) {
        /* minimum distance between agents */
        double MIN_DISTANCE_BETWEEN_AGENTS = 0.3;
        /* mininum distance that agent can walk with max speed */
        double DISTANCE_MAX_SPEED = MAX_SPEED + MIN_DISTANCE_BETWEEN_AGENTS;    // 0.96 + 0.3
        /* the range to calculate the density */
        double DENSITY_RANGE = DISTANCE_MAX_SPEED * time_scale;
        /* minimum speed to break dead lock state */
        double MIN_SPEED_DEADLOCK = 0.3;

        ArrayList<EvacuationAgent> currentLinkAgents = current_link.getAgents();
        // 隣のリンクも含めた自分と同じ位置に存在するエージェント
        ArrayList<EvacuationAgent> samePlaceAgents = new ArrayList<EvacuationAgent>();

        /* the number of agents in the range */
        int inRangeSameDirectionAgents = 0;
        int inRangeOppositeDirectionAgents = 0;

        /* the number of agents in front of myself in range */
        int inFrontSameDirectionAgents = 0;
        int inFrontOppositeDirectionAgents = 0;

        /* in range agents on current link is calculated in between maxRange & minRange */
        double maxRange = 0;
        double minRange = 0;
        if (isPositiveDirection()) {
            minRange = position;
            maxRange = Math.min(position + DENSITY_RANGE, current_link.length);
        } else {
            minRange = Math.max(absolutePosition() - DENSITY_RANGE, 0);
            maxRange = absolutePosition();
        }

        /* in range agents on next link is calculated in between maxRange & minRange */
        //double nextMaxRange = 0;
        //double nextMinRange = 0;

        int route_index_orig = getRouteIndex();
        MapLink nextLink = sane_navigation_from_node(time, current_link, next_node);
        setRouteIndex(route_index_orig);

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
            if (agent.absolutePosition() == absolutePosition()) {
                samePlaceAgents.add(agent);
            }
            // agent が (minRange..maxRange) 内に位置する
            if (agent.absolutePosition() >= minRange && agent.absolutePosition() <= maxRange) {
                if (direction == agent.getDirection()) {
                    inRangeSameDirectionAgents += 1;
                } else {
                    inRangeOppositeDirectionAgents += 1;
                }
                // 以下、自分の隣か前を歩いている(歩いて来る) agent について(direction ごと)
                // agent is placed in front of this agent.
                if (isPositiveDirection() && agent.absolutePosition() >= position) {
                    if (agent.isPositiveDirection()) {
                        inFrontSameDirectionAgents += 1;
                    } else {
                        inFrontOppositeDirectionAgents += 1;
                    }
                    if (frontAgent == null) {
                        if (agent.absolutePosition() != position)
                            frontAgent = agent;
                    } else if (frontAgent.absolutePosition() > agent.absolutePosition()) {
                        frontAgent = agent;
                    }
                // agent is placed in front of this agent.
                } else if (isNegativeDirection() && agent.absolutePosition() <= absolutePosition()) {
                    if (agent.isNegativeDirection()) {
                        inFrontSameDirectionAgents += 1;
                    } else {
                        inFrontOppositeDirectionAgents += 1;
                    }
                    if (frontAgent == null) {
                        if (agent.absolutePosition() != absolutePosition())
                            frontAgent = agent;
                    } else if (frontAgent.absolutePosition() < agent.absolutePosition()) {
                        frontAgent = agent;
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
        /*  sinplex road: v = 1.2 - 0.25 * rho
         *  duplex road : v = 1.25 - 0.476 * rho
         *  duplex road : v = 1.27 * 10 ^ (-0.22 * rho)
         */
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
                distance = Math.abs(frontAgent.absolutePosition() - absolutePosition());
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
        // System.err.println("time: " + (int)time + ", Speed: " + speed + ", Link: " + current_link.ID + ", width: " + current_link.width
        //         + ", Same: " + inFrontSameDirectionAgents + ", Opposite: " + inFrontOppositeDirectionAgents
        //         + ", position: " + position);
    }

    /**
     * get distance between this agent and neighbor agent
     * 隣のリンク上にいる agent との距離
     */
    private double getDistanceNeighborAgent(EvacuationAgent agent) {
        double distance = 0.0;
        MapLink currentLink = getCurrentLink();
        MapLink neighborLink = agent.getCurrentLink();

        if (currentLink.getFrom() == neighborLink.getFrom()) {
            distance = absolutePosition() + agent.absolutePosition();
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            distance = absolutePosition() + neighborLink.length - agent.absolutePosition();
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            distance = currentLink.length - absolutePosition() + agent.absolutePosition();
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            distance = currentLink.length - absolutePosition() + neighborLink.length - agent.absolutePosition();
        } else {
            System.err.println("\tRunningAroundPerson.getDistanceNeighborAgent inputted neighbor link is not neighbor!");
            distance = -1.0;
        }
        return distance;
    }

    /**
     * is a neighbor agent same direction with this agent
     */
    private boolean isSameDirectionNeighborAgent(EvacuationAgent agent) {
        MapLink currentLink = getCurrentLink();
        MapLink neighborLink = agent.getCurrentLink();

        // 重複したリンク
        if (currentLink.getFrom() == neighborLink.getFrom() && currentLink.getTo() == neighborLink.getTo()) {
            if (isPositiveDirection() && agent.isNegativeDirection())
                return true;
            else if (isNegativeDirection() && agent.isPositiveDirection())
                return true;
            else
                return false;
        }
        // ループ状の重複したリンク
        if (currentLink.getFrom() == neighborLink.getTo() && currentLink.getTo() == neighborLink.getFrom()) {
            if (isPositiveDirection() && agent.isPositiveDirection())
                return true;
            else if (isNegativeDirection() && agent.isNegativeDirection())
                return true;
            else
                return false;
        }
        if (currentLink.getFrom() == neighborLink.getFrom()) {
            if (isPositiveDirection() && agent.isNegativeDirection())
                return true;
            else if (isNegativeDirection() && agent.isPositiveDirection())
                return true;
            else
                return false;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            if (isPositiveDirection() && agent.isPositiveDirection())
                return true;
            else if (isNegativeDirection() && agent.isNegativeDirection())
                return true;
            else
                return false;
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            if (isPositiveDirection() && agent.isPositiveDirection())
                return true;
            else if (isNegativeDirection() && agent.isNegativeDirection())
                return true;
            else
                return false;
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            if (isPositiveDirection() && agent.isNegativeDirection())
                return true;
            else if (isNegativeDirection() && agent.isPositiveDirection())
                return true;
            else
                return false;
        } else {
            System.err.println("\tRunningAroundPerson.isSameDirectionNeighborAgent inputted neighbor link is not neighbor!");
            return false;
        }
    }

    /**
     * is a neighbor agent place in front of this agent
     */
    private boolean isFrontNeighborAgent(EvacuationAgent agent) {
        MapLink currentLink = getCurrentLink();
        MapLink neighborLink = agent.getCurrentLink();

        if (currentLink.getFrom() == neighborLink.getFrom()) {
            if (isNegativeDirection())
                return true;
        } else if (currentLink.getFrom() == neighborLink.getTo()) {
            if (isNegativeDirection())
                return true;
        } else if (currentLink.getTo() == neighborLink.getFrom()) {
            if (isPositiveDirection())
                return true;
        } else if (currentLink.getTo() == neighborLink.getTo()) {
            if (isPositiveDirection())
                return true;
        } else {
            System.err.println("\tRunningAroundPerson." +
                    "isFrontNeighborAgent inputted neighbor link is " +
                    "not neighbor!");
        }
        return false;
    }

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

    /** Calculate a distance between myself and the front agent in current link.
     * @return the distance
     */
    public double calcDistanceToFront() {
        double distance = 0.0;
        for (EvacuationAgent agent : current_link.getAgents()) {
            double tmp_distance = 0.0;
            if (isPositiveDirection()) {
                if (agent.absolutePosition() > position) {
                    tmp_distance = agent.absolutePosition() - position;
                }
            } else {
                if (agent.absolutePosition() < absolutePosition()) {
                    tmp_distance = absolutePosition() - agent.absolutePosition();
                }
            }
            if (tmp_distance > distance) {
                distance = tmp_distance;
            }
        }
        return distance;
    }

    public HashMap<MapLink, Double> expectedDensitySpeeds = new
        HashMap<MapLink, Double>();
    private void calc_speed_expected_density(double time) {
        /* get expected density link speed */
        // System.err.println("CALC_SPEED_EXPECTED_DENSITY");
        if (expectedDensitySpeeds.size() == 0) {
            speed = ZERO_SPEED;
            System.err.println("RunningAroundPerson." +
                    "calc_speed_expected_density cannot find match speed!");
        } else {
            speed = ((Double) expectedDensitySpeeds.get(current_link))
                .doubleValue();
        }
        /*System.err.println("expected Speed: " + speed + ", Link: " + current_link.ID +
                ", position: " + position);*/
    }

    @Override
    public double getDirection() {
        return direction;
    }

    @Override
    public boolean isPositiveDirection() { return direction >= 0.0; }

    @Override
    public boolean isNegativeDirection() { return ! isPositiveDirection(); }

    @Override
    public double getSpeed() {
        return speed;
    }

    @Override
    public void setSpeed(double _speed) {
        speed = _speed;
    }

    public double getAcceleration() {
        return dv;
    }

    @Override
    public double getEmptySpeed() {
        return emptyspeed;
    }

    @Override
    public void setEmptySpeed(double s) {
        emptyspeed = s;
    }

    public void setMaxAllowedDamage(double maxAllowedDamage) {
        //this.maxAllowedDamage = maxAllowedDamage;
    }

    public double getMaxAllowedDamage() {
        return 0.0;
        //return maxAllowedDamage;
    }

    public void setGoal(String _goal) {
        //TODO might want to clear path
        goal = _goal;
    }

    public void setPlannedRoute(ArrayList<String> _planned_route) {
        planned_route = _planned_route;
    }

    @Override
    public ArrayList<String> getPlannedRoute() {
        return planned_route;
    }

    /* try to pass a node, and enter next link */
    // change: navigation_reason, route, prev_node, previous_link,
    // current_link, position, evacuated, link.agentExists
    protected boolean tryToPassNode(double time) {
        for (final CheckPoint point : route) {
            if (point.node == next_node) {
                if (navigation_reason != null) {
                    navigation_reason += "LOOP HERE!\n";
                }
                if (debug_mode) {
                    String mid_goal =
                        (getPlannedRoute().size() < getRouteIndex()) ?
                            getPlannedRoute().get(getRouteIndex()) : goal;
                            System.err.println("agent going to the same room "
                                    + next_node.getTagString()
                                    + "(" + next_node.getHeight() + ") on " +
                                    time + " when going for " + mid_goal);
                            for (final CheckPoint passed_points : route) {
                                System.err.print(passed_points.node + "("
                                        + passed_points.time + ") ");
                            }
                            System.err.println();
                }
                break;
            }
        }
        route.add(new CheckPoint(next_node, time, navigation_reason));
        // tkokada.debug
        // System.err.println("next_node: " + next_node.getTagString() +
                // ", time: " + time + ", navigation_reason: " +
                // navigation_reason);

        /* agent exits the previous link */
        getCurrentLink().agentExits(this);
        setPrevNode(next_node);

        //2011年6月7日追加
        MapLink previous_link = current_link;

        /* agent enters the next link */
        setCurrentLink(next_link_candidate);

        // 2011年6月7日追加
        MapNode node_now;
        if (getPrevNode() == next_link_candidate.getFrom()) {
            node_now = current_link.getTo();
        } else {
            node_now = current_link.getFrom();
        }
        final ArrayList<MapLink> way_candidates = node_now.getPathways();
        double direction_orig = direction;
        // tkokada
        calc_next_target(next_node);
        if (getPrevNode() == next_link_candidate.getFrom()) {
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
        MapNode current_link_node_to = current_link.getTo();
        MapNode current_link_node_from = current_link.getFrom();
        if (next_link_candidate.width == previous_link.width &&
                way_candidates.size()== 1 &&
                (current_link_node_to.hasTag("Exit") ||
                 current_link_node_from.hasTag("Exit"))) {
            if (direction_orig==direction) {
                update_swing_flag = false;
            } else {
                update_swing_flag = false;
                swing_width *= -1;
            }
        } else if (next_link_candidate.width == previous_link.width &&
                way_candidates.size() == 2) {
            if (direction_orig == direction) {
                update_swing_flag = false;
            } else {
                update_swing_flag = false;
                swing_width *= -1;
            }
        } else {
            update_swing_flag = true;
        }
        //if (current_link.hasTag(goal)) {
        //if (planned_route.size() <= getRouteIndex() &&
        //        current_link.hasTag(goal)) {
        if ((isPlannedRouteCompleted() || isRemainingRouteWAIT_()) && current_link.hasTag(goal)) {
            consumePlannedRoute();
            // tkokada: temporaly comment out
            //System.err.println("the goal should not be a link!!");
            setEvacuated(true, time);
            current_link.agentExits(this);
        }

        return true;
    }

    /* look up our route plan and give our next goal
     */
    protected String calc_next_target(MapNode node) {
        if (on_node &&
                planned_route.size() > getRouteIndex() &&
                next_node.hasTag(planned_route.get(getRouteIndex()))) {
            /* reached mid-goal */
            setRouteIndex(getRouteIndex() + 1);
        }

        while (planned_route.size() > getRouteIndex()) {
            String candidate = planned_route.get(getRouteIndex());
            if (node.getHint(candidate) != null) {
                return candidate;
            }
            System.err.println("no mid-goal set for " + candidate);
            setRouteIndex(getRouteIndex() + 1);
        }

        return goal;
    }

    String navigation_reason = null;
    protected MapLink sane_navigation(double time, final ArrayList<MapLink> way_candidates) {
        MapLink way = sane_navigation_from_node(time, current_link, next_node);
        return way;
    }

    // route_index
    // navigation_reason
    //
    protected MapLink sane_navigation_from_node(double time, MapLink link, MapNode node) {
        // 前回の呼び出し時と同じ結果になる場合は不要な処理を回避する
        if (
            ! sane_navigation_from_node_forced &&
            sane_navigation_from_node_current_link == current_link &&
            sane_navigation_from_node_link == link && sane_navigation_from_node_node == node &&
            emptyspeed < (isPositiveDirection() ? current_link.length - position : position)
        ) {
            sane_navigation_from_node_forced = false;
            return sane_navigation_from_node_result;
        }
        sane_navigation_from_node_forced = false;
        sane_navigation_from_node_current_link = current_link;
        sane_navigation_from_node_link = link;
        sane_navigation_from_node_node = node;

        ArrayList<MapLink> way_candidates = node.getPathways();
        double min_cost = Double.MAX_VALUE;
        double min_cost_second = Double.MAX_VALUE;
        MapLink way = null;
        MapLink way_second = null;

        boolean monitor = next_node.hasTag("MONITOR-NAVIGATION");

        ArrayList<MapLink> way_samecost = null;
        // tkokada:  add randomness to choose next link
        HashMap<MapLink, Double> resultLinks = new HashMap<MapLink, Double>();

        final String next_target = calc_next_target(node);

        if (monitor)
            System.err.println("navigating at " + node.getTagString() + " for " + next_target);
        navigation_reason = "for " + next_target + "\n";
        for (MapLink way_candidate : way_candidates) {
            //if (way_candidate.hasTag(goal)) {}
            // tkokada
            if (planned_route.size() <= getRouteIndex() && way_candidate.hasTag(goal)) {
                /* finishing up */
                way = way_candidate;
                navigation_reason += " found goal " + goal;
                break;
            } else if (way_candidate.hasTag(next_target)) {
                /* reached mid_goal */
                way = way_candidate;
                setRouteIndex(getRouteIndex() + 1);
                navigation_reason += " found mid-goal in " + way_candidate.getTagString();
                break;
            } else if (way_candidate == link) {
                // don't want to go back, ignoring
                continue;
            }
            boolean loop = false;
            // ※この if 文は常に無効
            if (DELAY_LOOP) {
                for (final CheckPoint cp : route) {
                    if (cp.node == way_candidate.getOther(node)) {
                        loop = true;
                        break;
                    }
                }
            }

            MapNode other = way_candidate.getOther(node);
            double cost = other.getDistance(next_target);
            cost += way_candidate.length;

            if (monitor)
                System.err.print(way_candidate.getTagString() +
                    "\t" + cost + "\t" +cost +
                    "(" + way_candidate.crowdness() + ")\t"
                    + way_candidate.spaceLeft(node, 1.0));
            navigation_reason += way_candidate.getOther(node).getTagString()
                    + "(" + cost + (loop ? " loop" : "") + ") ";

            if (loop) {
                // ※このブロックは常に無効
                if (cost < min_cost_second) {
                    min_cost_second = cost;
                    way_second = way_candidate;
                }
            } else if (cost < min_cost) {
                min_cost = cost;
                way = way_candidate;
                way_samecost = null;
            } else if (cost == min_cost) {
                if (way_samecost == null)
                    way_samecost = new ArrayList<MapLink>();
                way_samecost.add(way_candidate);
                // if (cost < min_cost)
                //     min_cost = cost;
            }
            // tkokada
            if (randomNavigation) {
                resultLinks.put(way_candidate, new Double(cost));
            }
        }

        // tkokada
        double margin = 3.0;
        // ※ randomNavigation は通常は使用しない
        if (randomNavigation) {
            double totalCost = 0.0;
            MapLink minCostLink = null;
            double minCost = Double.MAX_VALUE;
            for (MapLink resultLink : resultLinks.keySet()) {
                double cost = ((Double) resultLinks.get(resultLink)).doubleValue();
                if (cost < minCost) {
                    minCost = cost;
                    minCostLink = resultLink;
                }
            }
            ArrayList<MapLink> linkCandidates = new ArrayList<MapLink>();
            for (MapLink resultLink : resultLinks.keySet()) {
                double cost = ((Double) resultLinks.get(resultLink)).doubleValue();
                if (cost >= min_cost && cost < min_cost + margin)
                    linkCandidates.add(resultLink);
            }
            if (linkCandidates.size() > 0) {
                MapLink chosedLink = linkCandidates.get(random.nextInt(linkCandidates.size()));
                sane_navigation_from_node_result = chosedLink;
                return chosedLink;
            }
            sane_navigation_from_node_result = null;
            return null;
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
        sane_navigation_from_node_result = way;

        if (way == null) {
            return null;
        }

        navigation_reason += "\n -> chose " + way.getOther(node).getTagString();
        /*
        System.err.println("sane_navigation_from_node ID: " + ID + " : " +
                "prev: " + prev_node.ID + " next: " + next_node.ID +
                " current: " + current_link.ID + " goal: " +
                goal + " way: " + way.ID + " reason: " + navigation_reason);
        */
        return way;
    }

    // direction
    // prev_node
    // next_node
    protected MapLink navigate(double time,
            MapLink link_now,
            MapNode node_now) {
        final ArrayList<MapLink> way_candidates = node_now.getPathways();

        /* trapped? */
        if (way_candidates.size() == 0) {
            System.err.println("Warning: Agent trapped!");
            //System.err.println("returned candidates size is 0");
            return null;
        }

        /* only one way to choose */
        if (way_candidates.size() == 1) {
            //System.err.println("returned candidates size is 1");
            return way_candidates.get(0);
        }

        /* if not in navigation mode, go back to path */
        if (goal == null) {
            turnAround();
            //System.err.println("turn around");
            return link_now;
        }

        MapLink target = sane_navigation(time, way_candidates);
        if (target != null) {
            //System.err.println("target null");
            return target;
        }

        /* try follow others */
        double weight_total = 0.0; 
        for (final MapLink link : way_candidates) {
            double w = link.getConfidence(this);
            if (w < 0.0) w = 0.0;
            weight_total += w;
        }

        if (weight_total != 0.0) {
            //double choice = Math.random() *  weight_total;
            double choice = random.nextDouble() *  weight_total;
            for (MapLink link : way_candidates) {
                double w = link.getConfidence(this);
                if (w < 0.0) w = 0.0;
                choice -= w;
                if (choice < 0.0) {
                    //System.err.println("choose with weight");
                    return link;
                }
            }
        }

        /* choose randomly */
        int i = 0;
        if (way_candidates.size() > 1) {
            //i = (int)(Math.random() * (way_candidates.size() - 1));
            i = (int)(random.nextDouble() * (way_candidates.size() - 1));
        }
        //System.err.println("choose randomly");
        return way_candidates.get(i);
    }

    public void renavigate() {
        if (goal != null){
            final String next_target = calc_next_target(next_node);
            /*
            System.out.println("RunningAroundPerson.renavigate planned_route" +
                    ": " + planned_route + "next_target: " + next_target +
                    "next_node: " + next_node.ID);
            */
            double nextDistance = next_node.getDistance(next_target)
            + (current_link.length - position);
            double prevDistance = prev_node.getDistance(next_target)
            + position;
            /*
            double nextDistance = next_node.getDistanceNullAvoid(next_target);
            double prevDistance = prev_node.getDistanceNullAvoid(next_target);
            if (nextDistance >= 0.0)
                nextDistance += (current_link.length - position);
            if (prevDistance >= 0.0)
                prevDistance += position;
            if (prevDistance < nextDistance) {
                turnAround();
            }
            */
        }
    }

    // virtual methods
    //  these methods don't change any local variables.
    protected MapLink virtual_navigate(double time,
            MapLink link_now,
            MapNode node_now) {
        final ArrayList<MapLink> way_candidates = node_now.getPathways();

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
        if (goal == null) {
            // turnAround();
            return link_now;
        }

        MapLink target = virtual_sane_navigation_from_node(time, link_now,
                node_now);
        if (target != null) {
            return target;
        }

        /* try follow others */
        double weight_total = 0.0;
        for (final MapLink link : way_candidates) {
            double w = link.getConfidence(this);
            if (w < 0.0) w = 0.0;
            weight_total += w;
        }

        if (weight_total != 0.0) {
            double choice = random.nextDouble() *  weight_total;
            for (MapLink link : way_candidates) {
                double w = link.getConfidence(this);
                if (w < 0.0) w = 0.0;
                choice -= w;
                if (choice < 0.0) {
                    return link;
                }
            }
        }

        /* choose randomly */
        int i = 0;
        if (way_candidates.size() > 1) {
            i = (int)(random.nextDouble() * (way_candidates.size() - 1));
        }
        return way_candidates.get(i);
    }

    /** pre-navigation, actually does not change any values. */
    protected MapLink virtual_sane_navigation_from_node(double time,
            MapLink link, MapNode node) {

        ArrayList<MapLink> way_candidates = node.getPathways();
        double min_cost = Double.MAX_VALUE;
        double min_cost_second = Double.MAX_VALUE;
        MapLink way = null;
        MapLink way_second = null;

        ArrayList<MapLink> way_samecost = null;
        // tkokada:  add randomness to choose next link
        HashMap<MapLink, Double> resultLinks = new HashMap<MapLink, Double>();

        final String next_target = calc_next_target(node);

        int originalRouteIndex = getRouteIndex();
        for (MapLink way_candidate : way_candidates) {
            if (planned_route.size() <= getRouteIndex() &&
                    way_candidate.hasTag(goal)) {
                way = way_candidate;
                break;
            } else if (way_candidate.hasTag(next_target)) {
                way = way_candidate;
                setRouteIndex(getRouteIndex() + 1);
                break;
            } else if (way_candidate == link) {
                continue;
            }
            boolean loop = false;
            if (DELAY_LOOP) {
                for (final CheckPoint cp : route) {
                    if (cp.node == way_candidate.getOther(node)) {
                        loop = true;
                        break;
                    }
                }
            }
            MapNode other = way_candidate.getOther(node);
            double cost = other.getDistance(next_target);
            cost += way_candidate.length;
            if (loop) {
                if (cost < min_cost_second) {
                    min_cost_second = cost;
                    way_second = way_candidate;
                }
            } else if (cost < min_cost) {
                min_cost = cost;
                way = way_candidate;
                way_samecost = null;
            } else if (cost == min_cost) {
                if (way_samecost == null)
                    way_samecost = new ArrayList<MapLink>();
                way_samecost.add(way_candidate);
                if (cost < min_cost) min_cost = cost;
            }
            if (randomNavigation) {
                resultLinks.put(way_candidate, new Double(cost));
            }
        }
        setRouteIndex(originalRouteIndex);

        double margin = 3.0;
        if (randomNavigation) {
            double totalCost = 0.0;
            MapLink minCostLink = null;
            double minCost = Double.MAX_VALUE;
            for (MapLink resultLink : resultLinks.keySet()) {
                double cost = ((Double) resultLinks.get(resultLink)).
                    doubleValue();
                if (cost < minCost) {
                    minCost = cost;
                    minCostLink = resultLink;
                }
            }
            ArrayList<MapLink> linkCandidates = new ArrayList<MapLink>();
            for (MapLink resultLink : resultLinks.keySet()) {
                double cost = ((Double) resultLinks.get(resultLink)).
                    doubleValue();
                if (cost >= min_cost && cost < min_cost + margin)
                    linkCandidates.add(resultLink);
            }
            if (linkCandidates.size() > 0) {
                MapLink chosedLink = linkCandidates.get(
                        random.nextInt(linkCandidates.size()));
                return chosedLink;
            }
            return null;
        }

        if (way_samecost != null) {
            int i = (int)(random.nextDouble() * way_samecost.size());
            if (i != way_samecost.size()) {
                way = way_samecost.get(i);
            }
        }

        if (way == null) {
            way = way_second;
        }

        if (way == null) {
            return null;
        }

        return way;
    }

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
                agent.setGoal(goalString);
            }
        };
        return new ParamSettingPanel(network_map, this); 
    }
    
    @Override
    public NType getNodeType() {
        return NType.AGENT;
    }
        
    /* for the newer, xml based format */
    public static String getAgentTypeString() {
        return "RunningAroundPerson";
    }

    @Override
    public Element toDom(Document dom, String nodetype) {
        Element element = super.toDom(dom, getNodeTypeString());
        element.setAttribute("id", "" + ID);
        element.setAttribute("AgentType", RunningAroundPerson.getAgentTypeString());

        element.setAttribute("GeneratedTime", "" + generatedTime);
        element.setAttribute("EmptySpeed", "" + emptyspeed);
        element.setAttribute("CurrentPathway", "" + current_link.ID);
        element.setAttribute("PrevRoom", "" + prev_node.ID);
        element.setAttribute("NextRoom", "" + next_node.ID);
        element.setAttribute("Position", "" + position);
        element.setAttribute("Goal", "" + getGoal());

        for (String via : planned_route) {
              Element tnode = dom.createElement("route");
              Text via_tag_text = dom.createTextNode(via);
              tnode.appendChild(via_tag_text);
              element.appendChild(tnode);
        }
        return element;
    }
    
    public static OBNode fromDom(Element element, Random _random) {
        int id = Integer.parseInt(element.getAttribute("id"));
        RunningAroundPerson agent = new RunningAroundPerson(id, _random);
        agent.getAttributesFromDom(element);
        agent.generatedTime = Double.parseDouble(element.getAttribute("GeneratedTime"));
        agent.emptyspeed = Double.parseDouble(element.getAttribute("EmptySpeed"));
        agent.setGoal(element.getAttribute("Goal"));
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            if (children.item(i)  instanceof Element) {
                Element child = (Element)children.item(i);
                if (!child.getTagName().equals("route")) continue;
                agent.planned_route.add(child.getTextContent().toUpperCase());
              }
          }

        agent.position = Double.parseDouble(element.getAttribute("Position"));

        /* used in NetworkMap.setupNetwork */
        String location[] = new String[3];
        location[0] = element.getAttribute("CurrentPathway");
        location[1] = element.getAttribute("PrevRoom");
        location[2] = element.getAttribute("NextRoom");
        agent.setUserObject(location);

        return agent;
    }

    @Override
    public String getGoal() {
        return goal;
    }

    public void setEmergency() {
        setGoal("EMERGENCY"); 
    }

    public boolean isPassedNode() {
        return passed_node;
    }

    public boolean isEmergency() {
        return goal.equals("EMERGENCY"); 
    }

    public void setRouteIndex(int routeIndex) {
        this.routeIndex = routeIndex;
    }

    public int getRouteIndex() {
        return routeIndex;
    }

    public SpeedCalculationModel getSpeedCalculationModel() {
        return calculation_model;
    }

    public void setSpeedCalculationModel(SpeedCalculationModel _model) {
        calculation_model = _model;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double _density) {
        density = _density;
    }

    public void setExpectedDensityMacroTimeStep(int
            _expectedDensityMacroTimeStep) {
        expectedDensityMacroTimeStep = _expectedDensityMacroTimeStep;
        setTimeScale(expectedDensityMacroTimeStep);
    }

    public double getExpectedDensityMacroTimeStep() {
        return expectedDensityMacroTimeStep;
    }

    public void setTimeScale(double _time_scale) {
        time_scale = _time_scale;
    }

    public double getTimeScale() {
        return time_scale;
    }

    // planned_route の残り経路がすべて WAIT_FOR/WAIT_UNTIL ならば true を返す
    public boolean isRemainingRouteWAIT_() {
        if (isPlannedRouteCompleted()) {
            return false;
        }

        int index = routeIndex;
        while (index < planned_route.size()) {
            String candidate = planned_route.get(index);
            if (candidate.startsWith("WAIT_UNTIL")) {
                index += 3;
            } else if (candidate.startsWith("WAIT_FOR")) {
                index += 3;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean isPlannedRouteCompleted() {
        return routeIndex >= planned_route.size();
    }

    public void consumePlannedRoute() {
        routeIndex = planned_route.size();
    }

    public String getNextCandidate() {
        return isPlannedRouteCompleted() ? "" : planned_route.get(routeIndex);
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
