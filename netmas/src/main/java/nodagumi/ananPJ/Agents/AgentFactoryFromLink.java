// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.io.PrintWriter;
import java.util.Random;

import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;

import nodagumi.Itk.*;


//======================================================================
/**
 * エージェント生成機構。(fromLink)
 */
public class AgentFactoryFromLink extends AgentFactory {
    MapLink start_link;

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public AgentFactoryFromLink(Config config, Random random) {
        super(config, random) ;
        start_link = (MapLink)config.startPlace ;
    }

    @Override
    protected boolean finished(double time) {
        if (super.finished(time) || start_link.isShutOff()) return true;
        return false;
    }

    @Override
    protected void place_agent(AgentBase agent) {
        agent.placeAtRandomPosition(start_link) ;
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
