// -*- mode: java; indent-tabs-mode: nil -*-
/** DumpEvent.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2017/11/29 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2017/11/29]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import java.util.HashMap;
import java.util.ArrayList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.Factory.AgentFactory;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryList;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryConfig;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.* ;

//======================================================================
/**
 * This event cause to interrupt simulation and make a log dump.
 * <pre>
 *  { "type" : "Dump" 
 *    "atTime" : __Time__,
 *    "format" : "GenerationFile",
 *    "filename" : __FileName__}
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 */
public class DumpEvent extends EventBase {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * dump file name
     */
    public String filename = null ;

    //============================================================
    //============================================================
    /**
     * Enum for format of dump file
     */
    static public enum Format {
	GenerationFile
    }
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Lexicon for format of dump file
     */
    static public Lexicon formatLexicon = new Lexicon() ;
    static {
	formatLexicon.registerEnum(Format.class) ;
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Format Type
     */
    public Format format ;

    //------------------------------------------------------------
    /**
     * JSON Term による setup.
     * format と filename を設定する。
     */
    public void setupByJson(Scenario _scenario,
                            Term eventDef) {
        super.setupByJson(_scenario, eventDef) ;
	
	format = (Format)formatLexicon.lookUp(eventDef.getArgString("format")) ;
	if(format == null) {
	    Itk.logError("Wrong format in DumpEvent:", eventDef) ;
	    Itk.quitByError() ;
	}

        String bareFilename = eventDef.getArgString("filename") ;
	if(bareFilename == null) {
	    Itk.logError("Wrong dump filename in DumpEvent:", eventDef) ;
	    Itk.quitByError() ;
	}
	filename =
            getScenario().getProperties()
            .furnishPropertiesDirPath(bareFilename, false, false) ;
    }


    //------------------------------------------------------------
    /**
     * Dump イベント発生処理。
     * エージェントの状態をダンプする。
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
	switch(format){
	case GenerationFile:
	    dumpInGenerationFileFormat(currentTime) ;
	    break ;
	default:
	    Itk.logError("unknown dump format:", format) ;
	    Itk.quitByError() ;
	}
	return true ;
    }

    //------------------------------------------------------------
    /**
     * Dump イベント発生逆処理。
     * 何もしない。
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(SimTime currentTime, NetworkMap map) {
	return true ;
    }

    //------------------------------------------------------------
    /**
     * Dump in GenerationFile format
     */
    public void dumpInGenerationFileFormat(SimTime currentTime) {
	Term ruleList = Term.newArrayTerm() ;

        dumpRuleTermForRemainingRules(currentTime, ruleList) ;
        dumpRuleTermForWalkingAgents(currentTime, ruleList) ;

        outputDumpFileInGenerationFileFormat(ruleList) ;
    }
    
    //------------------------------------------------------------
    /**
     * output dump file
     */
    private void outputDumpFileInGenerationFileFormat(Term ruleList) {
        try {
            File dumpFile = new File(filename) ;
            FileWriter writer = new FileWriter(dumpFile) ;
            writer.write("#{ \"version\" : 2}\n") ;
            writer.write(ruleList.toJson(true)) ;
            writer.close() ;
        } catch (IOException ex){
            Itk.dumpStackTrace() ;
            Itk.logError("IOError",ex) ;
            Itk.quitByError() ;
        }
    }
        
    //------------------------------------------------------------
    /**
     * generate Dump term for remaining generation rules.
     */
    private Term dumpRuleTermForRemainingRules(SimTime currentTime,
                                               Term ruleList) {
	AgentHandler handler = getAgentHandler() ;
        AgentFactoryList factoryList = handler.getAgentFactoryList() ;

        // generate rule
        for(String ruleName :
                factoryList.getAgentFactoryConfigNameTable().keySet()) {
            AgentFactoryConfig factoryConfig =
                factoryList.getAgentFactoryConfigByName(ruleName) ;

            Term rule = factoryConfig.toTermForRemainingAgents(currentTime) ;

            if(rule != null) {
                ruleList.addNth(rule) ;
            }
        }
        return ruleList ;
    }
        
    //------------------------------------------------------------
    /**
     * generate Dump term for walking agents in individual gen rule format
     * in GenerationFile format.
     */
    private Term dumpRuleTermForWalkingAgents(SimTime currentTime,
                                              Term ruleList) {
	AgentHandler handler = getAgentHandler() ;

	// collect agents by each rule
	HashMap<String, ArrayList<AgentBase>> agentTable =
	    new HashMap<String, ArrayList<AgentBase>>() ;

	for(AgentBase agent : handler.getWalkingAgentCollection()) {
            String ruleName = agent.getFactory().getRuleName() ;
	    if(!agentTable.containsKey(ruleName)) {
		agentTable.put(ruleName, new ArrayList<AgentBase>()) ;
	    }
	    agentTable.get(ruleName).add(agent) ;
	}

	// generate rules
	for(String ruleName : agentTable.keySet()) {
            AgentFactoryConfig factoryConfig =
                handler.getAgentFactoryConfigByName(ruleName) ;
	    Term rule =
		dumpTermForAgentsInOneRule(factoryConfig,
                                           agentTable.get(ruleName),
                                           currentTime) ;
	    ruleList.addNth(rule) ;
	}

        return ruleList ;
    }

    //------------------------------------------------------------
    /**
     * dump agents in one rule
     */
    private Term dumpTermForAgentsInOneRule(AgentFactoryConfig factoryConfig,
                                            ArrayList<AgentBase> agentList,
                                            SimTime currentTime) {
	Term indivList = Term.newArrayTerm() ;
	for(AgentBase agent : agentList) {
	    Term agentConf =
                dumpTermForOneAgent(agent, currentTime) ;
	    indivList.addNth(agentConf) ;
	}

	Term rule = factoryConfig.toTermForWalkingAgents(indivList,
                                                         currentTime,
                                                         "_dumped_") ;

	return rule ;
    }
    
    //------------------------------------------------------------
    /**
     * dump one agent
     */
    private Term dumpTermForOneAgent(AgentBase agent, SimTime currentTime) {
	Term agentConf = 
            agent.dumpTermForIndividualConfig(currentTime) ;
	return agentConf ;
    }
    
    //------------------------------------------------------------
    /**
     * 文字列化 後半
     */
    public String toStringTail() {
        return (super.toStringTail() +
                "," + "filename=" + filename +
                "," + "format=" + format);
    }
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class DumpEvent

