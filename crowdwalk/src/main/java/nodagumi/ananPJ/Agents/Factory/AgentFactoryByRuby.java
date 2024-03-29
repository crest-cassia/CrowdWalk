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

package nodagumi.ananPJ.Agents.Factory;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Agents.*;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;

import nodagumi.ananPJ.Agents.Factory.AgentFactoryConfig.AgentFactoryConfig_Ruby;

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
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // 以下、ruby との情報受け渡し用
    /** access to simulator */
    private EvacuationSimulator simulator ;
    
    public EvacuationSimulator getSimulator() { return simulator ; }

    public NetworkMap getMap() { return simulator.getMap() ; }
    
    /** access to currentTime */
    private SimTime currentTime ;

    public SimTime getCurrentTime() { return currentTime ; }
    
    
    /** access to the list of adding agents*/
    private List<AgentBase> agentList ;

    public List<AgentBase> getAgentList() { return agentList ; }

    /** work variable to keep startPlace */
    private OBNode startPlace ;
    
    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public AgentFactoryByRuby(AgentFactoryConfig config,
                              OBNode _startPlace,
                              int _totalInFactory,
                              Random _random) {
        super(config, _startPlace, _totalInFactory, _random) ;
    }

    
    //------------------------------------------------------------
    /**
     * Config による初期化
     */
    @Override
    public void init(AgentFactoryConfig config,
                     OBNode _startPlace,
                     int _totalInFactory,
                     Random random) {
        super.init(config, _startPlace, _totalInFactory, random) ;

        AgentFactoryConfig_Ruby rubyConfig =
            (AgentFactoryConfig_Ruby)config ;
        
        rubyFactoryClassName = rubyConfig.ruleClass ;
        configForRuby = rubyConfig.config ;
    }

    //------------------------------------------------------------
    /**
     * Ruby Engine の設定
     */
    public void setupRubyEngine(ItkRuby _rubyEngine) {
        if(_rubyEngine == null) {
            Itk.logError("Generation Rule by Ruby require Ruby Engine") ;
            Itk.quitByError() ;
        }
        
        rubyEngine = _rubyEngine ;

        rubyFactory = rubyEngine.newInstanceOfClass(rubyFactoryClassName,
                                                    this,
                                                    configForRuby,
                                                    getFallbackParameters()) ;
    }
    
    //------------------------------------------------------------
    /**
     *  enable にする。
     *  （多分、使いみちはないが、makeDisable() の対照のため。)
     */
    public void enable() {
        enabled = true ;
    }

    //------------------------------------------------------------
    /**
     *  disable にする。
     *  （多分、使いみちはないが、makeDistable() の対照のため。)
     */
    public void disable() {
        enabled = false ;
    }

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    @Override
    protected void placeAgent(AgentBase agent) {
        if(startPlace instanceof MapLink) {
            agent.placeAtRandomPosition((MapLink)startPlace) ;
        } else if(startPlace instanceof MapNode) {
            agent.place(null, (MapNode)startPlace, 0.0) ;
        }
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

        if(isEnabled()) {
            rubyEngine.callMethod(rubyFactory, "tryUpdateAndGenerate") ;
        }
    }

    //------------------------------------------------------------
    /**
     * intern
     */
    public Term makeSymbolTerm(String str) {
        return new Term(str, true) ;
    }
    
    //------------------------------------------------------------
    /**
     * 文字列からの SimTime 生成。
     */
    public SimTime getSimTime(String timeStr) {
        return new SimTime(timeStr) ;
    }
    
    //------------------------------------------------------------
    /**
     * タグ名によるリンク取得
     */
    public MapLinkTable getLinkTableByTag(Term tag) {
        MapLinkTable linkTable = new MapLinkTable() ;
        getMap().getLinks()
            .findTaggedLinks(tag.getString(), linkTable) ;
        return linkTable ;
    }
    
    //------------------------------------------------------------
    /**
     * タグ名によるノード取得
     */
    public MapNodeTable getNodeTableByTag(Term tag) {
        MapNodeTable nodeTable = new MapNodeTable() ;
        getMap().getNodes()
            .findTaggedNodes(tag.getString(), nodeTable) ;
        return nodeTable ;
    }
    
    //------------------------------------------------------------
    /**
     * ルート付きでエージェント生成
     */
    public AgentBase launchAgentWithRoute(String agentClassName,
                                          OBNode _startPlace,
                                          Term goalTag,
                                          List<Term> route) {
        return launchAgentWithRoute(agentClassName,
                                    _startPlace,
                                    goalTag,
                                    route,
                                    null) ;
    }
    
    //------------------------------
    /**
     * ルート付きでエージェント生成
     */
    public AgentBase launchAgentWithRoute(String agentClassName,
                                          OBNode _startPlace,
                                          Term goalTag,
                                          List<Term> route,
                                          Term subAgentConfig) {
        startPlace = _startPlace ;
        setGoal(goalTag) ;
        setPlannedRoute(route) ;

        Term fallbackForAgent = getFallbackForAgent() ;

        // set agentConf() for this agent.
        Term agentConfBackup = getAgentConf() ;
        if(subAgentConfig != null && subAgentConfig.isObject()) {
            if(agentConfBackup == null) {
                setAgentConf(subAgentConfig) ;
            } else {
                Term newAgentConf = new Term(agentConfBackup) ;
                newAgentConf.updateObjectFacile(subAgentConfig, true) ;
                setAgentConf(newAgentConf) ;
            }
        }
        
        AgentBase agent =
            launchAgent(agentClassName, simulator, currentTime,
                        agentList, fallbackForAgent) ;

        // set back agentConf()
        setAgentConf(agentConfBackup) ;
        
        return agent ;
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
