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
