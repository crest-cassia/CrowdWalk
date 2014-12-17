// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;

import nodagumi.Itk.*;

public class WaitRunningAroundPerson extends RunningAroundPerson
    implements Serializable {
    private static final long serialVersionUID = -6498240875020862791L;

    /**
     * Agent の詳細設定情報を格納しているもの
     */
    public Map<String, Object> config ;

    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public WaitRunningAroundPerson() {} ;

    public WaitRunningAroundPerson(int _id,
            double speed, double _confidence,
            double allowance, double time, Random _random) {
        init(_id, speed,  _confidence, allowance, time, _random) ;
    }

    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id,
            double speed, double _confidence,
            double allowance, double time, Random _random) {
        super.init(_id, speed, _confidence, allowance, time, _random);
    }

    public WaitRunningAroundPerson(int _id, Random _random) {
        init(_id, _random) ;
    }

    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id, Random _random) {
        super.init(_id, _random);
    }

    @Override
    public EvacuationAgent copyAndInitialize() {
        WaitRunningAroundPerson r = new WaitRunningAroundPerson(0, random);
        return copyAndInitializeBody(r) ;
    }

    /**
     * Conf による初期化。
     * 継承しているクラスの設定のため。
     * @param conf json の連想配列形式を scan した Map
     */
    public void initByConf(Map<String, Object> conf) {
        if(conf != null) {
            config = conf ;
        } else {
            config = new HashMap<String, Object>() ;
        }
    } ;
    /**
     * Conf による初期化。
     * 継承しているクラスの設定のため。
     * @param confString json で書かれたAgentのconfigulation。
     */
    public void initByConf(String confString) {
        Map<String, Object> conf =
            (Map<String, Object>)JSON.decode(confString) ;
        initByConf(conf) ;
    }

    /**
     * 複製操作のメイン
     */
    public EvacuationAgent copyAndInitializeBody(WaitRunningAroundPerson r) {
        r.ID = ID;
        r.generatedTime = generatedTime;
        r.emptyspeed = emptyspeed;
        r.prev_node = prev_node;
        r.next_node = next_node;
        r.current_link = current_link;
        r.position = position;
        r.direction = direction;
        r.speed = speed;
        r.goal = goal;
        r.planned_route = planned_route;
        r.setRouteIndex(0);

        return r;
    }
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("wait");

        buffer.append("," + this.ID);
        buffer.append("," + this.agentNumber);
        buffer.append("," + this.emptyspeed);
        buffer.append("," + this.time_scale);
        // tkokada
        if (this.current_link == null) {
            System.err.println("WaitRunningAroundPerson.toString link null.");
            System.err.println("\tgoal: " + this.goal);
            System.err.println("\tplanned route size: " + this.planned_route.size());
            for (String r : this.planned_route)
                System.err.println("\t\troute: " + r);
        }
        // 避難が完了すると current_link は null になる
        // buffer.append("," + this.current_link.ID);
        buffer.append("," + (this.current_link == null ? "_LINK_NULL_" : this.current_link.ID));
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
        if (!items[0].equals("wait")) { return null; }
        
        RunningAroundPerson agent = new RunningAroundPerson(
                Integer.parseInt(items[1]), _random);
        agent.agentNumber = Integer.parseInt(items[2]);
        agent.emptyspeed = Double.parseDouble(items[3]);
        agent.time_scale = Double.parseDouble(items[4]);
        
        MapLink link = (MapLink)map.getObject(Integer.parseInt(items[5]));
        agent.current_link = link;
        agent.position = Double.parseDouble(items[6]);
        agent.direction = Double.parseDouble(items[7]);
        if (agent.direction > 0.0) {
            agent.prev_node = link.getFrom();
            agent.next_node = link.getTo();
        } else {
            agent.prev_node = link.getTo();
            agent.next_node = link.getFrom();
        }
        agent.speed = Double.parseDouble(items[8]);
        agent.accumulatedExposureAmount = Double.parseDouble(items[9]);
        agent.goal = items[10];
        agent.routeIndex = Integer.parseInt(items[11]);
        int route_size = Integer.parseInt(items[12]);
        for (int i = 0; i < route_size; i++) {
            agent.planned_route.add(items[13 + i]);
        }
        int tags_base = route_size + 13;
        int tags_size = Integer.parseInt(items[tags_base]);
        for (int i = 0; i < tags_size; i++) {
            agent.addTag(items[tags_base + i + 1]);
        }
        return agent;
    }


    protected boolean scatter(double time) {
        final ArrayList<EvacuationAgent> agents = current_link.getAgents();
        int index = Collections.binarySearch(agents, this);

        double front_space = current_link.length - position;
        double rear_space = position;
        int width = (int)current_link.width;
        int rear_agent_i = index - width;
        if (rear_agent_i > 0) {
            rear_space = position - agents.get(rear_agent_i).position;
        }
        int front_agent_i = index + width;
        if (front_agent_i < agents.size()) {
            front_space = agents.get(front_agent_i).position - position;
        }
        
        calc_speed(time);
        double d = (front_space - rear_space) * 0.3; 
        if (Math.abs(d) > Math.abs(speed)) {
            d = Math.signum(d) * Math.abs(speed);
        }
        return move_set(d, time, false);
    }
    
    protected boolean pack(double time) {
        calc_speed(time);

        double d = speed * direction;
        return move_set(d, time, false);
    }

    static final double NOT_WAITING = -100.0;
    private double wait_time = NOT_WAITING;
    private double wait_time_start = NOT_WAITING;

    @Override
    public void preUpdate(double time) {
        waiting = false;
        if (time <= generatedTime ||
                getRouteIndex() >= planned_route.size()) {
            super.preUpdate(time);
            return;
        }

        if (planned_route.get(getRouteIndex()).length() > 10 &&
                planned_route.get(getRouteIndex()).substring(0, 10).equals("WAIT_UNTIL")) {
            String target = planned_route.get(getRouteIndex()).substring(11);
            String until = planned_route.get(getRouteIndex() + 2);
            until = until.substring(0, until.length() - 1);

            if (current_link.hasTag(target)) {
                String how = planned_route.get(getRouteIndex() + 1);
                
                if (current_link.hasTag(until)) {
                    setRouteIndex(getRouteIndex() + 3);
                } else if (how.equals("SCATTER")) {
                    waiting = true;
                    scatter(time);
                    return;
                } else if (how.equals("PACK")) {
                    waiting = true;
                    pack(time);
                    return;
                } else {
                    System.err.println("WARNING: how to move not stated!");
                }
                super.preUpdate(time);
            }
        } else if (planned_route.get(getRouteIndex()).length() > 8 &&
                planned_route.get(getRouteIndex()).substring(0, 8).equals("WAIT_FOR")) {
            String target = planned_route.get(getRouteIndex()).substring(9);

            if (current_link.hasTag(target)) {
                if (wait_time == NOT_WAITING) {
                    String until_str = planned_route.get(getRouteIndex() + 2);
                    until_str = until_str.substring(0, until_str.length() - 1);
                    wait_time = Double.parseDouble(until_str);
                    wait_time_start = time;
                }
                String how = planned_route.get(getRouteIndex() + 1);
                
                if (time - wait_time_start > wait_time) {
                    wait_time = NOT_WAITING;
                    setRouteIndex(getRouteIndex() + 3);
                } else if (how.equals("SCATTER")) {
                    waiting = true;
                    scatter(time);
                    return;
                } else if (how.equals("PACK")) {
                    waiting = true;
                    pack(time);
                    return;
                } else {
                    System.err.println("WARNING: how to move not stated!");
                }
            } else if (wait_time != NOT_WAITING) {
                System.err.println("WARNING: agent " + agentNumber
                        + "pushed out from " + target);
                wait_time = NOT_WAITING;
                setRouteIndex(getRouteIndex() + 3);
            }
        }
        super.preUpdate(time);
    }
    
    @Override
    public boolean update(double time) {
        return super.update(time);
    }

    /* look up our route plan and give our next goal  
     */
    @Override
    protected String calc_next_target(MapNode node) {
        if (on_node &&
                planned_route.size() > getRouteIndex() &&
                node.hasTag(planned_route.get(getRouteIndex()))) {
            setRouteIndex(getRouteIndex() + 1);
        }
        int next_check_point_index = getRouteIndex();
        while (planned_route.size() > next_check_point_index) {
            String candidate = planned_route.get(next_check_point_index);
            if (candidate.length() > 10 &&
                    candidate.substring(0, 10).equals("WAIT_UNTIL")) {
                setRouteIndex(next_check_point_index);
                next_check_point_index += 3;
            } else if (candidate.length() > 8 &&
                    candidate.substring(0, 8).equals("WAIT_FOR")) {
                setRouteIndex(next_check_point_index);
                next_check_point_index += 3;
            } else if (node.hasTag(candidate)) {
                next_check_point_index++;
                setRouteIndex(next_check_point_index);
            } else if (node.getHint(candidate) != null) {
                return candidate;
            } else {
                System.err.println("no mid-goal set for " + candidate);
                next_check_point_index++;
                setRouteIndex(next_check_point_index);
            }
        }

        return goal;
    }

    public ArrayList<String> getPlannedRoute() {
        ArrayList<String> goal_tags = new ArrayList<String>();

        int next_check_point_index = 0;
        while (planned_route.size() > next_check_point_index) {
            String candidate = planned_route.get(next_check_point_index);

            if (candidate.length() > 10 &&
                    candidate.substring(0, 10).equals("WAIT_UNTIL")) {
                final String tag = candidate.substring(11);
                goal_tags.add(tag);
                next_check_point_index += 3;
            } else if (candidate.length() > 8 &&
                    candidate.substring(0, 8).equals("WAIT_FOR")) {
                final String tag = candidate.substring(9);
                goal_tags.add(tag);
                next_check_point_index += 3;
            } else {
                goal_tags.add(candidate);
                ++next_check_point_index;
            }
        }
        return goal_tags;
    }

    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString =
        ClassFinder.alias("WaitRunningAroundPerson",
                          Itk.currentClassName()) ;
    public static String getAgentTypeString() {
        return typeString ;
    }

    public static String getTypeName() {
        return typeString ;
    }

    @Override
    public Element toDom(Document dom, String nodetype) {
        Element element = super.toDom(dom, getNodeTypeString());
        element.setAttribute("AgentType", WaitRunningAroundPerson.getAgentTypeString());

        for (String via : planned_route) {
              Element tnode = dom.createElement("route");
              Text via_tag_text = dom.createTextNode(via);
              tnode.appendChild(via_tag_text);
              element.appendChild(tnode);
        }
        return element;
    }

    public static OBNode fromDom(Element element) {
        return fromDom(element, new Random());
    }

    public static OBNode fromDom(Element element, Random _random) {
        int id = Integer.parseInt(element.getAttribute("id"));
        WaitRunningAroundPerson agent = new WaitRunningAroundPerson(id,
                _random);
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
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
