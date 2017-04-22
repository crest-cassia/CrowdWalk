// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.io.PrintWriter;
import java.util.Random;
import java.util.List;

import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Link.*;

import nodagumi.Itk.*;


//======================================================================
/**
 * エージェント生成機構。(fromNode)
 */
public class AgentFactoryFromNode extends AgentFactory {
    MapNode startNode;

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public AgentFactoryFromNode(Config config, Random random) {
        super(config, random) ;
        startNode = (MapNode)config.startPlace ;
    }

    //------------------------------------------------------------
    /**
     * エージェントを初期位置に置く。
     */
    @Override
    protected void placeAgent(AgentBase agent) {
        agent.place(null, startNode, 0.0) ;
    }

    //------------------------------------------------------------
    /**
     * エージェント生成ルールの情報を文字列で返す。
     * パネル表示用。
     */
    @Override
    public String getStartInfo() {
        return (startNode.getTagString() +
                "(" +  startNode.ID + ")" +
                " from " + startTime.getAbsoluteTime() +
                " ("  + total +
                " in " + duration + " sec)");
    }

    //------------------------------------------------------------
    /**
     * エージェント生成の出発地点のオブジェクトを返す。
     * パネル用。
     */
    @Override
    public OBNode getStartObject() { return startNode; }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
