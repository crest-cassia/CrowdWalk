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
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;
import nodagumi.ananPJ.misc.AgentGenerationFile.GenerationConfigForRuby;

import nodagumi.Itk.*;


//======================================================================
/**
 * エージェント生成機構(Ruby版)。
 */
public class AgentFactoryByRuby extends AgentFactory {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby Engine
     */
    public ItkRuby rubyEngine = null ;
    
    /**
     * Ruby Object
     */
    public Object rubyFactory = null ;
    
    /**
     * Ruby Class
     */
    public String rubyFactoryClassName ;
    
    /**
     * configu for Ruby Object
     */
    public Term configForRuby ;
    
    /**
     * fallback
     */
    public Term fallbackParameters ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // 以下、ruby との情報受け渡し用
    /** access to simulator */
    private EvacuationSimulator simulator ;
    
    public EvacuationSimulator getSimulator() { return simulator ; }
    
    /** access to currentTime */
    private SimTime currentTime ;

    public SimTime getCurrentTime() { return currentTime ; }
    
    
    /** access to the list of adding agents*/
    private List<AgentBase> agentList ;

    public List<AgentBase> getAgentList() { return agentList ; }
    
    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public AgentFactoryByRuby(Config _config, Random random) {
        super(_config, random) ;
        
        GenerationConfigForRuby config =
            (GenerationConfigForRuby)_config ;
        
        rubyFactoryClassName = config.ruleClass ;
        configForRuby = config.config ;
        fallbackParameters = config.fallbackParameters ;
    }

    //------------------------------------------------------------
    /**
     * 初期化。(AgentFactory で行われているものを置き換え)
     */
    public void init(Config config, Random random) {
        setPlannedRoute(config.plannedRoute) ;  // 実質意味はないが、エラー回避のため
    }
    //------------------------------------------------------------
    /**
     * Ruby Engine の設定
     */
    public void setupRubyEngine(ItkRuby _rubyEngine) {
        if(_rubyEngine == null) {
            Itk.logError("Generation Rule by Ruby require Ruby Engine") ;
            System.exit(1) ;
        }
        
        rubyEngine = _rubyEngine ;

        rubyFactory = rubyEngine.newInstanceOfClass(rubyFactoryClassName,
                                                    this,
                                                    configForRuby,
                                                    fallbackParameters) ;
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

    //------------------------------------------------------------
    /**
     * エージェント生成
     */
    public void tryUpdateAndGenerate(EvacuationSimulator simulator,
                                     SimTime currentTime,
                                     List<AgentBase> agentList) {
        this.simulator = simulator ;
        this.currentTime = currentTime ;
        this.agentList = agentList ;

        rubyEngine.callMethod(rubyFactory, "tryUpdateAndGenerate") ;
        
        Itk.logInfo("AgentFactoryByRuby is called.") ;
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
