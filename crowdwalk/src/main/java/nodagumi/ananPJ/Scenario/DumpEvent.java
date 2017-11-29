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

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.Factory.AgentFactory;
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

	filename = eventDef.getArgString("filename") ;
	if(filename == null) {
	    Itk.logError("Wrong dump filename in DumpEvent:", eventDef) ;
	    Itk.quitByError() ;
	}
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
	Itk.dbgMsg("DumpEvent","occur") ;
	Itk.dbgVal("currentTime", currentTime) ;
	Itk.dbgVal("agentHandler",getAgentHandler()) ;
	/* ??? */
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
	AgentHandler handler = getAgentHandler() ;

	// collect agents by each rule
	HashMap<AgentFactory, ArrayList<AgentBase>> agentTable =
	    new HashMap<AgentFactory, ArrayList<AgentBase>>() ;

	for(AgentBase agent : handler.getWalkingAgentCollection()) {
	    AgentFactory factory = agent.getFactory() ;
	    if(!agentTable.containsKey(factory)) {
		agentTable.put(factory, new ArrayList<AgentBase>()) ;
	    }
	    agentTable.get(factory).add(agent) ;
	}

	// generate rules
	Term ruleList = Term.newArrayTerm() ;
	for(AgentFactory factory : agentTable.keySet()) {
	    Term rule =
		dumpTermForOneRule(factory, agentTable.get(factory),
				   currentTime) ;
	    ruleList.addNth(rule) ;
	}
	Itk.dbgVal("ruleList", ruleList.toJson(true)) ;
    }
    
    //------------------------------------------------------------
    /**
     * dump one rule
     */
    private Term dumpTermForOneRule(AgentFactory factory,
				    ArrayList<AgentBase> agentList,
				    SimTime currentTime) {
	Term rule = factory.config.toTerm() ;
	rule.setArg("rule","INDIVIDUAL") ;
	rule.setArg("startTime",currentTime.getAbsoluteTimeString()) ;
	rule.setArg("duration", 1.0) ;

	Term indivList = Term.newArrayTerm() ;
	rule.setArg("individualConfig", indivList) ;
	
	for(AgentBase agent : agentList) {
	    Term agentConf = dumpTermForOneAgent(agent) ;
	    indivList.addNth(agentConf) ;
	}

	return rule ;
    }
    
    //------------------------------------------------------------
    /**
     * dump one agent
     */
    private Term dumpTermForOneAgent(AgentBase agent) {
	Term agentConf = Term.newObjectTerm() ;
	agent.dumpTermForIndividualConfig(agentConf) ;
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

