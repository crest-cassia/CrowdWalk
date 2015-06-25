// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.io.PrintWriter;
import java.util.Random;
import java.util.List;

import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.Link.*;

import nodagumi.Itk.*;


//======================================================================
/**
 * エージェント生成機構。(fromNode)
 */
public class AgentFactoryFromNode extends AgentFactory {
    MapNode start_node;

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public AgentFactoryFromNode(Config config, Random random) {
        super(config, random) ;
        start_node = (MapNode)config.startPlace ;
    }

    //------------------------------------------------------------
    /**
     * エージェントを初期位置に置く。
     */
    @Override
    protected void place_agent(AgentBase agent) {
        agent.place(null, start_node, 0.0) ;
    }
    //------------------------------------------------------------
    /**
     * エージェントを初期位置に置く。 (obsolete)
     * [2015.01.10 I.Noda]
     * おそらく、WalkAgent あたりから取ってきた、古いコード。
     * 効率悪く、意味不明の操作が多い。
     * 上記の適宜に置き換え
     */
    protected void place_agent_obsolete(AgentBase agent) throws Exception {
        /*
         */
        MapLinkTable way_candidates = start_node.getPathways(); 
        double min_cost = Double.MAX_VALUE;
        double min_cost_second = Double.MAX_VALUE;
        MapLink way = null;
        MapLink way_second = null;
        MapLinkTable way_samecost = null;
        final Term next_target;
        List<Term> plannedRoute = getPlannedRoute();
        if (plannedRoute.size() == 0)
            next_target = goal;
        else
            next_target = getPlannedRoute().get(0);

        for (MapLink way_candidate : way_candidates) {
            if (way_candidate.hasTag(goal)){
                /* finishing up */
                way = way_candidate;
                break;
            } else if (way_candidate.hasTag(next_target)){
                /* reached mid_goal */
                way = way_candidate;
                break;
            }

            MapNode other = way_candidate.getOther(start_node);
            double cost = other.getDistance(next_target) ;
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
        agent.place(link, start_node, 0.0) ;
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
        for (Term checkpoint : planned_route) {
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
