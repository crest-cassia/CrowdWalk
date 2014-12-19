// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.Map;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Agents.WaitRunningAroundPerson;
import nodagumi.ananPJ.Agents.*;

import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Simulator.EvacuationModelBase;

import nodagumi.Itk.*;


public abstract class GenerateAgent implements Serializable {
    /**
     * 使えるエージェントを予めロードしておくためのダミー
     */
    static private EvacuationAgent[] _dummyAgents = {
        new RunningAroundPerson(),
        new WaitRunningAroundPerson(),
        new Staff(),
        new NaiveAgent(),
        new CapriciousAgent(),
        new BustleAgent(),
    } ;

    public String goal;
    ArrayList<String> planned_route;
    double start_time, duration;
    int total;
    SpeedCalculationModel speed_model = null;
    Random random = null;
    ArrayList<String> tags = new ArrayList<String>(); 
    public String configLine;
    public boolean enabled = true;

    /**
     * エージェントクラスの名前を格納。
     */
    public String agentClassName = "WaitRunningAroundPerson" ;
    public String agentConf = null ; // config in json string.

    public GenerateAgent(String _agentClassName,
                         String _agentConf,
            String[] conditions,
            String _goal,
            ArrayList<String> _planned_route,
            double _start_time,
            double _duration,
            int _total,
            SpeedCalculationModel _speed_model,
            Random _random,
            String _configLine) {
        if(_agentClassName != null && _agentClassName.length() > 0) {
            agentClassName = _agentClassName ;
            agentConf = _agentConf ;
        }
        //Itk.dbgMsg("agentClassName", agentClassName) ;

        goal = _goal;
        planned_route = _planned_route;
        start_time = _start_time;
        duration = _duration;
        total = _total;
        speed_model = _speed_model;
        random = _random;
        configLine = _configLine;

        parse_conditions(conditions);
    }

    protected void parse_conditions(String[] conditions) {
        if (conditions == null) return;
        for (int i = 0; i < conditions.length; i++) {
            final String condition = conditions[i];
            if (condition.startsWith("COLOR=")) {
                tags.add(condition.substring(6));
            } else {
                tags.add(condition);
            }
        }
    }

    protected boolean finished(double time) {
        return time > start_time + duration && generated >= total;
    }

    abstract protected void place_agent(WaitRunningAroundPerson agent);

    int generated = 0;
    /* TODO must wait finish until generate &
     * must control here with scenario */
    public void tryUpdateAndGenerate(double time,
            double timeScale,
            double tick,
            EvacuationModelBase model,
            ArrayList<EvacuationAgent> agents) {
        if (!enabled) return;

        if (finished(time)) {
            enabled = false;
            return;
        }

        if (time < start_time) {
            /* not yet time to start generating agents */
            return;
        }

        double duration_left = start_time + duration - time;
        int agent_to_gen = total - generated;
        if (duration_left > 0) {
            double r = (double)(agent_to_gen) / duration_left * timeScale;
            // tkokada: free timeScale update
            if (((int) r) > agent_to_gen)
                r = agent_to_gen;
            agent_to_gen = (int)r;
            if (random.nextDouble() < r - (double)agent_to_gen) {
                agent_to_gen++;
            }
        }
        /* else, no time left, must generate all remains */

        /* [I.Noda] ここで Agent 生成? */
        for (int i = 0; i < agent_to_gen; ++i) {
            generated++;
            /*
            WaitRunningAroundPerson agent = new WaitRunningAroundPerson(model.getMap().assignUniqueAgentId(),
                    random);
            */
            WaitRunningAroundPerson agent = null;
            try {
                agent = (WaitRunningAroundPerson)ClassFinder.newByName(agentClassName) ;
                agent.init(model.getMap().assignUniqueAgentId(), random);
                if(agentConf != null)
                    agent.initByConf(agentConf) ;
            } catch (Exception ex ) {
                Itk.dbgMsg("class name not found") ;
                Itk.dbgMsg("agentClassName", agentClassName) ;
                ex.printStackTrace();
                System.exit(1) ;
            }

            agent.generatedTime = tick;
            agent.displayMode = model.getDisplayMode();
            ((RunningAroundPerson) agent).setSpeedCalculationModel(
                speed_model);
            agent.setConfigLine(configLine);
            agents.add(agent);
            agent.setGoal(goal);
            agent.setPlannedRoute(planned_route);

            place_agent(agent); // この時点では direction が 0.0 のため、add_agent_to_lane で agent は登録されない

            for (final String tag : tags) {
                agent.addTag(tag);
            }

            agent.prepareForSimulation(timeScale);
            agent.getCurrentLink().agentEnters(agent);  // ここで add_agent_to_lane させる
        }
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
                next_check_point_index += 1;
            }
        }
        return goal_tags;
    }

    abstract public String getStart();
    abstract public OBNode getStartObject();

    public int getMaxGeneration() {
        return total;
    }

    public abstract void dumpAgentToGenerate(PrintWriter pw);

    public void setRandom(Random _random) {
        random = _random;
    }

    public void setDuration(double _duration) {
        duration = _duration;
    }

    public double getDuration() {
        return duration;
    }
}

class GenerateAgentFromLink extends GenerateAgent {
    MapLink start_link;

    public GenerateAgentFromLink(String _agentClassName,
                                 String _agentConf,
            MapLink _start_link,
            String[] conditions,
            String _goal,
            ArrayList<String> _planned_route,
            double _start_time,
            double _duration,
            int _total,
            SpeedCalculationModel _speed_model,
            Random _random,
            String _configLine) {
        super(_agentClassName,
              _agentConf,
              conditions, _goal, _planned_route, _start_time, _duration,
                _total, _speed_model, _random, _configLine);
        start_link = _start_link;
    }

    @Override
    protected boolean finished(double time) {
        if (super.finished(time) || start_link.getStop()) return true;
        return false;
    }

    @Override
    protected void place_agent(WaitRunningAroundPerson agent) {
        double position = random.nextDouble() * start_link.length;
        agent.place(start_link, position);
        //start_link.agentEnters(agent);
    }

    @Override
    public String getStart() {
        return start_link.getTagString() + 
        "(" +  start_link.ID + ")" +
        " on " + start_time +
        " ("  + total +
        " in " + duration + "s)";
    }
    @Override
    public OBNode getStartObject() { return start_link; }

    @Override
    public void dumpAgentToGenerate(PrintWriter pw) {
        pw.print("gen_link");

        pw.print("," + start_link.ID);
        pw.print("," + start_time);
        pw.print("," + duration);
        
        pw.print("," + generated);
        pw.print("," + total);

        pw.print("," + goal);
        pw.print("," + planned_route.size());
        for (String checkpoint : planned_route) {
            pw.print("," + checkpoint);
        }
        pw.print("," + tags.size());
        for (String tag : tags) {
            pw.print("," + tag);
        }
        pw.println();
    }
}


class GenerateAgentFromNode extends GenerateAgent {
    MapNode start_node;

    public GenerateAgentFromNode(String _agentClassName,
                                 String _agentConf,
            MapNode _start_node,
            String[] conditions,
            String _goal,
            ArrayList<String> _planned_route,
            double _start_time,
            double _duration,
            int _total,
            SpeedCalculationModel _speed_model,
            Random _random,
            String _configLine) {
        super(_agentClassName,
              _agentConf,
              conditions, _goal, _planned_route, _start_time, _duration,
                _total, _speed_model, _random, _configLine);
        start_node = _start_node;
        //System.err.println("GenerateAgentFromNode start_node: " + start_node.ID + " goal: " + _goal);
    }

    /*  use finished of super class
    @Override
    protected boolean finished(double time) {
       (not implemented)
    }
     */

    @Override
    protected void place_agent(WaitRunningAroundPerson agent) {
        /*
         */
        MapLinkTable way_candidates = start_node.getPathways(); 
        double min_cost = Double.MAX_VALUE;
        double min_cost_second = Double.MAX_VALUE;
        MapLink way = null;
        MapLink way_second = null;
        MapLinkTable way_samecost = null;
        final String next_target;
        ArrayList<String> plannedRoute = getPlannedRoute();
        if (plannedRoute.size() == 0)
            next_target = goal;
        else
            next_target = getPlannedRoute().get(0);

        for (MapLink way_candidate : way_candidates) {
            if (way_candidate.hasTag(goal)) {
                /* finishing up */
                way = way_candidate;
                break;
            } else if (way_candidate.hasTag(next_target)) {
                /* reached mid_goal */
                way = way_candidate;
                break;
            }

            MapNode other = way_candidate.getOther(start_node);
            double cost = other.getDistance(next_target);
            cost += way_candidate.length;

            if (cost < min_cost) {
                min_cost = cost;
                way = way_candidate;
                way_samecost = null;
            } else if (cost == min_cost) {
                if (way_samecost == null)
                    way_samecost = new MapLinkTable();
                way_samecost.add(way_candidate);
                if (cost < min_cost) min_cost = cost;
            }
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

        MapLink link = null;
        if (way == null) {
            link = start_node.getPathways().get(0);
        } else {
            link = way;
        }
        for (MapLink way_candidate : way_candidates) {
            if (way != way_candidate) {
                link = way_candidate;
                break;
            }
        }
        //MapLink link = start_node.getPathways().get(0);
        link.setup_lanes();
        double position = 0.0;
        if (link.getTo() == start_node) position = link.length;
        agent.place(link, position);
        //link.agentEnters(agent);
    }

    @Override
    public String getStart() {
        return start_node.getTagString() +
        "(" +  start_node.ID + ")" +
        " from " + start_time +
        " ("  + total +
        " in " + duration + "s)";
    }
    @Override
    public OBNode getStartObject() { return start_node; }

    @Override
    public void dumpAgentToGenerate(PrintWriter pw) {
        pw.print("gen_node");

        pw.print("," + start_node.ID);
        pw.print("," + start_time);
        pw.print("," + duration);
        
        pw.print("," + generated);
        pw.print("," + total);

        pw.print("," + goal);
        pw.print("," + planned_route.size());
        for (String checkpoint : planned_route) {
            pw.print("," + checkpoint);
        }
        pw.print("," + tags.size());
        for (String tag : tags) {
            pw.print("," + tag);
        }
        pw.println();
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
