// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents.Factory;

import java.io.PrintWriter;
import java.util.Random;

import nodagumi.ananPJ.Agents.*;
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
    MapLink startLink;

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public AgentFactoryFromLink(AgentFactoryConfig config, Random random) {
        super(config, random) ;
        startLink = (MapLink)config.startPlace ;
    }

    @Override
    protected boolean isFinished(SimTime currentTime) {
        if (super.isFinished(currentTime) || startLink.isShutOff()) return true;
        return false;
    }

    @Override
    protected void placeAgent(AgentBase agent) {
        agent.placeAtRandomPosition(startLink) ;
        //startLink.agentEnters(agent);
    }

    //------------------------------------------------------------
    /**
     * エージェント生成ルールの情報を文字列で返す。
     * パネル表示用。
     */
    @Override
    public String getStartInfo() {
        return (startLink.getTagString() +
                "(" +  startLink.ID + ")" +
                " on " + getStartTime().getAbsoluteTime() +
                " ("  + getTotal() +
                " in " + getDuration() + " sec)");
    }

    //------------------------------------------------------------
    /**
     * エージェント生成の出発地点のオブジェクトを返す。
     * パネル用。
     */
    @Override
    public OBNode getStartObject() { return startLink; }

}


//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
