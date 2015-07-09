// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.io.PrintWriter;
import java.util.Random;

import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;

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
    protected boolean finished(SimTime currentTime) {
        if (super.finished(currentTime) || start_link.isShutOff()) return true;
        return false;
    }

    @Override
    protected void place_agent(AgentBase agent) {
        agent.placeAtRandomPosition(start_link) ;
        //start_link.agentEnters(agent);
    }

    @Override
    public String getStart() {
        return (start_link.getTagString() +
                "(" +  start_link.ID + ")" +
                " on " + startTime.getAbsoluteTime() +
                " ("  + total +
                " in " + duration + " sec)");
    }
    @Override
    public OBNode getStartObject() { return start_link; }

}


//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
