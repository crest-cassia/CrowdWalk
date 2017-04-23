// -*- mode: java; indent-tabs-mode: nil -*-
/** Agent Factory controlled by Ruby script
 * @author:: Itsuki Noda
 * @version:: 0.0 2017/04/23 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2017/04/23]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents;

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
 * エージェント生成機構(Ruby版)。
 */
public class AgentFactoryByRuby extends AgentFactory {
    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public AgentFactoryByRuby(Config config, Random random) {
        super(config, random) ;
    }

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    @Override
    protected boolean isFinished(SimTime currentTime) {
        return false ;
    }

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    @Override
    protected void placeAgent(AgentBase agent) {
        agent.placeAtRandomPosition(null) ;
    }

    //------------------------------------------------------------
    /**
     * エージェント生成ルールの情報を文字列で返す。
     * パネル表示用。
     */
    @Override
    public String getStartInfo() {
        return ("Ruby Agent Factory:") ;
    }

    //------------------------------------------------------------
    /**
     * エージェント生成の出発地点のオブジェクトを返す。
     * パネル用。
     */
    @Override
    public OBNode getStartObject() {
        return null ;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class AgentFactoryByRuby


//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
