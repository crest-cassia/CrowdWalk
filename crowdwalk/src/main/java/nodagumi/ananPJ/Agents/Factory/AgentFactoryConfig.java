// -*- mode: java; indent-tabs-mode: nil -*-
/** AgentFactoryConfig.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2017/09/25 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2017/09/25]: separate from AgentFactory. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents.Factory;

import java.util.List;
import java.util.ArrayList;

import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryList.RuleType;

import nodagumi.Itk.*;

//======================================================================
/**
 * エージェント生成用設定情報用クラス。
 * 生成ルール情報格納用クラス(Base)も統合。
 * あまりに引数が多いので、整理。
 * Generation Rule は以下の形式の JSON (version 2 format).
 */
public class AgentFactoryConfig {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成ルールのタイプ
     */
    public RuleType ruleType ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントのクラス名
     */
    public String agentClassName = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント設定情報 (JSON Object)
     */
    public Term agentConf = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 出発場所
     */
    public OBNode startPlace = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成リンクタグ
     */
    public String startLinkTag = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成リンクリスト
     */
    public MapLinkTable startLinks = new MapLinkTable() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成ノードリスト
     */
    public MapNodeTable startNodes = new MapNodeTable() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成条件
     */
    public String[] conditions = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 目的地
     */
    public Term goal = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 経路
     */
    public List<Term> plannedRoute ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 開始時刻
     */
    public SimTime startTime = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 持続時間
     */
    public double duration = 0.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成数
     */
    public int total = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * スピードモデル
     */
    public SpeedCalculationModel speedModel ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback 情報
     */
    public Term fallbackParameters = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 設定文字列（generation file 中の設定情報の文字列）
     */
    public String originalInfo = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成ルール名
     */
    public String ruleName = null ;
        
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 個別パラメータ
     */
    public IndividualConfigList individualConfigList = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 所属する factory リスト
     */
    private ArrayList<AgentFactory> agentFactoryList =
        new ArrayList<AgentFactory>() ;

    /** factoryリスト取得 */
    public ArrayList<AgentFactory> getAgentFactoryList() {
        return agentFactoryList ;
    } ;
    
    /** factoryを追加 */
    public AgentFactory pushToAgentFactoryList(AgentFactory factory) {
        agentFactoryList.add(factory) ;
        return factory ;
    }
    
    //========================================
    //----------------------------------------
    /**
     * ruleType に応じて、GenerationConfig のインスタンスを作る。
     */
    static public AgentFactoryConfig newByRuleType(RuleType ruleType) {
            
	AgentFactoryConfig factoryConfig = null;
	switch(ruleType) {
	case EACHRANDOM:
	    factoryConfig = new AgentFactoryConfig_EachRandom() ;
	    break ;
	case TIMEEVERY:
	    factoryConfig = new AgentFactoryConfig_TimeEvery() ;
	    break ;
	case RUBY:
	    factoryConfig = new AgentFactoryConfig_Ruby() ;
	    break ;
	case EACH:
	case RANDOM:
	case INDIVIDUAL:
	    factoryConfig = new AgentFactoryConfig() ;
	    break ;
	default:
	    Itk.logError("AgentGenerationFile include invalid rule type:",
			 ruleType) ;
	    return null ;
	}
	factoryConfig.ruleType = ruleType ;
	return factoryConfig ;
    }

    //----------------------------------------
    /**
     * JSON への変換
     */
    public String toJson(boolean pprintP){
	return toTerm().toJson(pprintP) ;
    }

    //------------------------------
    /**
     * JSONへの変換用のTerm変換
     */
    public Term toTerm() {
	Term jTerm = new Term() ;
        
	{ // agentType
            if(agentConf != null) { // new gen style.
                Term agentType = agentConf.clone() ;
                SetupFileInfo.clearFallback(agentType) ;
                jTerm.setArg("agentType", agentType) ;
            } else { // old stype gen file
                Term agentType = new Term() ;
                agentType.setArg("className", agentClassName) ;
                jTerm.setArg("agentType", agentType) ;
            }
        }
	jTerm.setArg("startPlace",startPlace) ;
	jTerm.setArg("conditions",conditions);
	jTerm.setArg("goal",goal);
	jTerm.setArg("plannedRoute",plannedRoute) ;
	jTerm.setArg("startTime",startTime.getAbsoluteTimeString()) ;
	jTerm.setArg("duration",duration) ;
	jTerm.setArg("total",total) ;
	jTerm.setArg("speedModel", speedModel) ;
	jTerm.setArg("name", ruleName) ;
	jTerm.setArg("individualConfig", individualConfigList.toTerm()) ;

	jTerm.setArg("rule",
		     AgentFactoryList.ruleLexicon
		     .lookUpByMeaning(ruleType).get(0));
	if(startLinkTag != null)
	    jTerm.setArg("startPlace", startLinkTag) ;
	jTerm.setArg("speedModel", 
		     AgentFactoryList.speedModelLexicon
		     .lookUpByMeaning(speedModel).get(0));

	return jTerm ;
    }

    //----------------------------------------
    /**
     * JSON Object からパラメータ設定
     */
    public AgentFactoryConfig scanJson(AgentFactoryList factoryList,
				       Term json,
				       NetworkMap map) {
	originalInfo = json.toJson() ;

	// rule name。
	// ruleName には、事前に、ルールの順番の番号がうめられている。
	String _ruleName = json.getArgString("name") ;
	if(_ruleName != null) { ruleName = _ruleName ; }

	// agentType & className ;
	Term agentType = json.getArgTerm("agentType") ;
	agentClassName = agentType.getArgString("className") ;
	agentConf = agentType ;

	// startPlace
	if(!factoryList.scanStartLinkTag(json.getArgString("startPlace"),
					 map, this))
	    return null ;

	// conditions
	Term _conditions = json.getArgTerm("conditions") ;
	if(_conditions != null) {
	    if(!_conditions.isArray()) {
		Itk.logError("'conditions' in generation file should be an array of tags.",
			     "conditions=", _conditions) ;
		return null ;
	    }
	    int l = _conditions.getArraySize() ;
	    conditions = new String[l] ;
	    for(int i = 0 ; i < l ; i++) {
		conditions[i] = _conditions.getNthString(i) ;
	    }
	}

	// individualConfig
	individualConfigList =
	    new IndividualConfigList(json.getArgTerm("individualConfig")) ;

	// INDIVIDUAL の時は、いろいろ別扱い。
	if(ruleType == RuleType.INDIVIDUAL) {
	    individualConfigList.sortByStartTime() ;
	    String firstTimeStr =
		individualConfigList.peekFirst().getArgString("startTime") ;
	    String lastTimeStr =
		individualConfigList.peekLast().getArgString("startTime") ;
	    startTime = new SimTime(firstTimeStr) ;
	    duration = startTime.calcDifferenceTo(new SimTime(lastTimeStr));
	    total = individualConfigList.size() ;
	} else {
	    // startTime
	    try {
		startTime = new SimTime(json.getArgString("startTime")) ;
	    } catch(Exception ex) {
		return null ;
	    }

	    // duration
	    duration = json.getArgDouble("duration") ;

	    // total
	    total = json.getArgInt("total") ;
	    if (factoryList.liner_generate_agent_ratio > 0) {
		total =
		    (int)(total * factoryList.liner_generate_agent_ratio);
		Itk.logInfo("Agent Population (JSON)", total);
	    }
	}
                
	// speedModel
	scanJsonForSpeedModel(factoryList, json) ;

	// goal
	goal = json.getArgTerm("goal") ;

	// plannedRoute
	Term plannedRouteTerm = json.getArgTerm("plannedRoute") ;
	plannedRoute = (plannedRouteTerm == null ?
			new ArrayList<Term>() :
			plannedRouteTerm.<Term>getTypedArray()) ;
            
	return this ;
    }

    //----------------------------------------
    /**
     * speed model の取得。
     */
    public void scanJsonForSpeedModel(AgentFactoryList factoryList,
				      Term json) {
	speedModel =
	    (SpeedCalculationModel)
	    AgentFactoryList.speedModelLexicon
	    .lookUp(json.getArgString("speedModel")) ;
	if(speedModel == null) {
	    speedModel = factoryList.getFallbackSpeedModel() ;
	}
    }

    //----------------------------------------
    /**
     * Factory を追加。
     */
    public void addFactories(AgentFactoryList factoryList,
			     NetworkMap map) {
	switch(ruleType) {
	case EACH:
	    addFactoriesForEach(factoryList) ;
	    break ;
	case RANDOM:
	case INDIVIDUAL:
	    addFactoriesForRandom(factoryList) ;
	    break ;
	default:
	    Itk.logError("Invalid Generation Rule:" + ruleType) ;
	}
    }

    //----------------------------------------
    /**
     * AgentFactory の登録。
     */
    protected void registerAgentFactory(AgentFactory factory,
                                        AgentFactoryList factoryList) {
        factoryList.add(factory) ;
        pushToAgentFactoryList(factory) ;
    }
    
    //----------------------------------------
    /**
     * EACH 用生成ルーチン
     * 各々の link, node で total 個ずつのエージェントが生成。
     */
    protected void addFactoriesForEach(AgentFactoryList factoryList) {
	for (final MapLink startLink : startLinks) {
	    startPlace = startLink ;
	    registerAgentFactory(new AgentFactoryFromLink(this,
                                                          factoryList.random),
                                 factoryList) ;
	}
	for (final MapNode startNode : startNodes) {
	    startPlace = startNode ;
	    registerAgentFactory(new AgentFactoryFromNode(this,
                                                          factoryList.random),
                                 factoryList) ;
	}
    }

    //----------------------------------------
    /**
     * RANDOM 用生成ルーチン
     * 指定された link, node において、
     * 合計で total 個のエージェントが生成。
     */
    protected void addFactoriesForRandom(AgentFactoryList factoryList) {
	int _total = this.total ;

	int links_size = this.startLinks.size();
	int size = links_size + this.startNodes.size();// linkとnodeの合計
	int[] chosen_links = new int[this.startLinks.size()];
	int[] chosen_nodes = new int[this.startNodes.size()];
	for (int i = 0; i < _total; i++) {
	    int chosen_index = factoryList.random.nextInt(size);
            if (chosen_index + 1 > links_size)
                chosen_nodes[chosen_index - links_size] += 1;
            else
                chosen_links[chosen_index] += 1;
	}
	for (int i = 0; i < this.startLinks.size(); i++) {
	    if (chosen_links[i] > 0) {
		this.startPlace = this.startLinks.get(i) ;
		this.total = chosen_links[i] ;
                registerAgentFactory(new AgentFactoryFromLink(this, 
                                                              factoryList
                                                              .random),
                                     factoryList) ;
	    }
	}
	for (int i = 0; i < this.startNodes.size(); i++) {
	    if (chosen_nodes[i] > 0) {
		this.startPlace = this.startNodes.get(i) ;
		this.total = chosen_nodes[i] ;
                registerAgentFactory(new AgentFactoryFromNode(this,
                                                              factoryList
                                                              .random),
                                     factoryList) ;
	    }
	}
        this.total = _total ;
    }

    //============================================================
    /**
     * 生成ルール情報格納用クラス(EachRandom 用)
     */
    static public class AgentFactoryConfig_EachRandom 
        extends AgentFactoryConfig {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 各リンク・ノードにおける発生上限数
         */
        public int maxFromEachPlace = 0 ;

        //----------------------------------------
        /**
         * JSON Object への変換
         */
        public Term toTerm(){
            Term jTerm = super.toTerm() ;

            jTerm.setArg("maxFromEach",maxFromEachPlace) ;

            return jTerm ;
        }

        //----------------------------------------
        /**
         * JSON Object からパラメータ設定
         */
        public AgentFactoryConfig scanJson(AgentFactoryList factoryList,
					   Term json,
					   NetworkMap map) {
            AgentFactoryConfig r = super.scanJson(factoryList, json, map) ;
            
            if(r == null) {
                return null ;
            }

            maxFromEachPlace = json.getArgInt("maxFromEach") ;

            return this ;
        }

        //----------------------------------------
        /**
         * Factory を追加。
         * EACH RANDOM 用生成ルーチン
         * RANDOM に、1箇所での生成数の上限を入れたもの。
         * 合計で total 個のエージェントが生成。
         */
        public void addFactories(AgentFactoryList factoryList,
				 NetworkMap map) {

            int maxFromEachPlace = this.maxFromEachPlace ;
            int total = this.total ;

            int links_size = this.startLinks.size() ;
            int nodes_size = this.startNodes.size() ;
            int size = links_size + nodes_size ; // linkとnodeの合計
            int[] chosen_links = new int[this.startLinks.size()];
            int[] chosen_nodes = new int[this.startNodes.size()];

            /* [2014.12.24 I.Noda]
             * アルゴリズムがあまりにまずいので、修正。
             */
            if(total > 0) {
                int population = 0 ;
                //とりあえず、maxFromEachPlace で埋める。
                for(int i = 0 ; i < links_size ; i++) {
                    chosen_links[i] = maxFromEachPlace ;
                    population += maxFromEachPlace ;
                }
                for(int i = 0 ; i < nodes_size ; i++) {
                    chosen_nodes[i] = maxFromEachPlace ;
                    population += maxFromEachPlace ;
                }
                //減らしていく。
                while(population > total){
                    int chosen_index = factoryList.random.nextInt(size) ;
                    if(chosen_index < links_size) {
                        if(chosen_links[chosen_index] > 0) {
                            chosen_links[chosen_index] -= 1 ;
                            population -= 1 ;
                        }
                    } else {
                        if(chosen_nodes[chosen_index - links_size] > 0) {
                            chosen_nodes[chosen_index - links_size] -= 1;
                            population -= 1 ;
                        }
                    }
                }
            }

            for (int i = 0; i < this.startLinks.size(); i++) {
                if (chosen_links[i] > 0) {
                    this.startPlace = this.startLinks.get(i) ;
                    this.total = chosen_links[i] ;
                    registerAgentFactory(new AgentFactoryFromLink(this,
                                                                  factoryList
                                                                  .random),
                                         factoryList) ;
                }
            }
            for (int i = 0; i < this.startNodes.size(); i++) {
                if (chosen_nodes[i] > 0) {
                    this.startPlace = this.startNodes.get(i) ;
                    this.total = chosen_nodes[i] ;
                    registerAgentFactory(new AgentFactoryFromNode(this,
                                                                  factoryList
                                                                  .random),
                                         factoryList) ;
                }
            }
        }
    } // end class AgentFactoryConfig_EachRandom 

    //============================================================
    /**
     * 生成ルール情報格納用クラス(TimeEvery 用)
     */
    static public class AgentFactoryConfig_TimeEvery
        extends AgentFactoryConfig {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成の終了時刻
         */
        public SimTime everyEndTime = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成のインターバル
         */
        public int everySeconds = 0 ;

        //----------------------------------------
        /**
         * JSON Object への変換
         */
        public Term toTerm(){
            Term jTerm = super.toTerm() ;

            jTerm.setArg("everyEndTime",everyEndTime.getAbsoluteTimeString());
            jTerm.setArg("everySeconds",everySeconds) ;

            return jTerm ;
        }

        //----------------------------------------
        /**
         * JSON Object からパラメータ設定
         */
        public AgentFactoryConfig scanJson(AgentFactoryList factoryList,
					   Term json,
					   NetworkMap map) {
            AgentFactoryConfig r = super.scanJson(factoryList, json, map) ;
            
            if(r == null) {
                return null ;
            }

            // everyEndTime と everySecond の設定。
            try {
                String endTimeStr = json.getArgString("everyEndTime") ;
                everyEndTime = new SimTime(endTimeStr) ;
            } catch(Exception ex) {
                return null ;
            }
            everySeconds = json.getArgInt("everySeconds") ;

            return this ;
        }

        //----------------------------------------
        /**
         * Factory を追加。
         * TIME EVERY 用生成ルーチン
         * [2014.12.24 I.Noda]
         * GOAL の部分の処理は他と同じはずなので、
         * 特別な処理をしないようにする。
         * 合計で (total * 生成回数) 個のエージェントが生成。
         */
        public void addFactories(AgentFactoryList factoryList,
				 NetworkMap map) {
            
            SimTime every_end_time = this.everyEndTime ;
            double every_seconds = (double)this.everySeconds ;
            int total = this.total ;

            // [I.Noda] startPlace は下で指定。
            this.startPlace = null ;
            // [I.Noda] startTime も特別な意味
            SimTime start_time = this.startTime ;
            this.startTime = null ;

            SimTime step_time = start_time.newSimTime() ;
            /* let's assume start & goal & plannedRoute candidates
             * are all MapLink!
             */

            while (step_time.isBeforeOrAt(every_end_time)) {
                for (int i = 0; i < total; i++) {
                    this.startTime = step_time.newSimTime() ;
                    this.total = 1 ;
                    if(this.startLinks.size() > 0) {
                        MapLink start_link =
                            this.startLinks.chooseRandom(factoryList.random) ;
                        this.startPlace = start_link ;
                        registerAgentFactory(new
                                             AgentFactoryFromLink(this,
                                                                  factoryList
                                                                  .random),
                                             factoryList) ;
                    } else if (this.startNodes.size() > 0) {
                        MapNode start_node = 
                            this.startNodes.chooseRandom(factoryList.random) ;
                        this.startPlace = start_node ;
                        registerAgentFactory(new
                                             AgentFactoryFromNode(this,
                                                                  factoryList
                                                                  .random),
                                             factoryList) ;
                    } else {
                        Itk.logError("no starting place for generation.") ;
                        Itk.logError_("config",this) ;
                    }
                }
                step_time.advanceSec(every_seconds) ;
            }
        }
    } // end class AgentFactoryConfig_TimeEvery

    //============================================================
    /**
     * 生成ルール情報格納用クラス(Ruby 用)
     */
    static public class AgentFactoryConfig_Ruby
        extends AgentFactoryConfig {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         *  rule class
         */
        public String ruleClass ;
        
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         *  config
         */
        public Term config ;
        
        //----------------------------------------
        /**
         * JSON Object への変換
         */
        public Term toTerm(){
            Term jTerm = new Term() ;

            jTerm.setArg("rule",
			 AgentFactoryList.ruleLexicon
			 .lookUpByMeaning(ruleType).get(0));
            jTerm.setArg("ruleClass", ruleClass) ;
            jTerm.setArg("config", config) ;

            return jTerm ;
        }

        //----------------------------------------
        /**
         * JSON Object からパラメータ設定
         */
        public AgentFactoryConfig scanJson(AgentFactoryList factoryList,
					   Term json,
					   NetworkMap map) {
            ruleClass = json.getArgString("ruleClass") ;
            config = json.getArgTerm("config") ;

            //エラーを避けるために。
            plannedRoute = new ArrayList<Term>() ;

            // speedModel
            scanJsonForSpeedModel(factoryList, json) ;
            
            return this ;
        }

        //----------------------------------------
        /**
         * Factory を追加。
         * RUBY 用生成ルーチン
         */
        public void addFactories(AgentFactoryList factoryList,
				 NetworkMap map) {
            AgentFactory factory =
                new AgentFactoryByRuby(this, factoryList.random) ;
            registerAgentFactory(factory, factoryList) ;
        }
        
    } // end class AgentFactoryConfig_Ruby
    
} // end class AgentFactoryConfig

