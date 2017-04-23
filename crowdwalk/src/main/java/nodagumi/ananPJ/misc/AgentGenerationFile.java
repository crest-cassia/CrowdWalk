// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List ;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.Exception;
import java.lang.Integer;

import javax.swing.JOptionPane;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.Agents.AwaitAgent.WaitDirective;
import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.Agents.AgentFactoryFromLink;
import nodagumi.ananPJ.Agents.AgentFactoryFromNode;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;
import nodagumi.Itk.*;

//======================================================================
/**
 * Generate agents depending on a generation file.
 * Generation File は、Version 0, 1, 2 の3種類。
 * その区別は、先頭行に
 *  <pre>
 *    #{ "version" : 2 }
 *  </pre>
 * という形で記述して区別する。
 * <p>
 * Version 2 では、2行目以降に Generation Rule の配列で JSON 形式で記述される。
 * つまり、
 *  <pre>
 *    #{ "version" : 2 }
 *    [ __Generation_Rule__,
 *      __Generation_Rule__,
 *      ... ]
 *  </pre>
 * となる。
 * <code>__Generation_Rule__</code> には以下のバリエーションがある。
 * <dl>
 *   <dt>EACH</dt>
 *     <dd>
 *       指定したタグを持つすべてのリンクとノードから、
 *       各々指定した数のエージェントが出発する。
 *      <pre>
 *        { "rule" : "EACH",
 *          "ignore" : ("true" | "false")
 *          "agentType" : { "className" : __AgentType__,
 *                          (__AgentConfig__)* },
 *          "total" : __Integer__,
 *          "speedModel" : __SpeedModel__,
 *          "startPlace" : __Tag__,
 *          "conditions" : __Conditions__,
 *          "startTime" : __Time__,
 *          "duration" : __DurationInSec__,
 *          "goal" : __Tag__,
 *          "plannedRoute" : [ __Tag__, __Tag__, ... ]
 *        }
 *      </pre>
 *       __Conditions__ は、生成されたエージェントに付与されるタグの配列。
 *     </dd>
 *   <dt>RANDOM</dt>
 *     <dd>
 *       指定したタグを持つすべてのリンクとノードから、
 *       ランダムにエージェントが出発する。
 *       エージェントの総数は指定した数(total)となる。
 *      <pre>
 *        { "rule" : "RANDOM",
 *          "ignore" : ("true" | "false")
 *          "agentType" : { "className" : __AgentType__,
 *                          (__AgentConfig__)* },
 *          "total" : __Integer__,
 *          "speedModel" : __SpeedModel__,
 *          "startPlace" : __Tag__,
 *          "conditions" : __Conditions__,
 *          "startTime" : __Time__,
 *          "duration" : __DurationInSec__,
 *          "goal" : __Tag__,
 *          "plannedRoute" : [ __Tag__, __Tag__, ... ]
 *        }
 *      </pre>
 *     </dd>
 *   <dt>EACHRANDOM</dt>
 *     <dd>
 *       RANDOMと同じだが、タグを持つ各リンク・ノードからは、
 *       最低1エージェントは出発する。
 *      （エージェントの出発しないリンク・ノードはない）
 *      <pre>
 *        { "rule" : "EACHRANDOM",
 *          "ignore" : ("true" | "false")
 *          "agentType" : { "className" : __AgentType__,
 *                          (__AgentConfig__)* },
 *          "total" : __Integer__,
 *          "maxFromEach" : __Integer__,
 *          "speedModel" : __SpeedModel__,
 *          "startPlace" : __Tag__,
 *          "conditions" : __Conditions__,
 *          "startTime" : __Time__,
 *          "duration" : __DurationInSec__,
 *          "goal" : __Tag__,
 *          "plannedRoute" : [ __Tag__, __Tag__, ... ]
 *        }
 *      </pre>
 *     </dd>
 *   <dt>TIMEEVERY</dt>
 *     <dd>
 *       指定した時間区間のすべてのタイムステップに於いて、
 *       指定したタグを持つすべてのリンクから、
 *       各々指定した数のエージェントが出発する。
 *       EACH を、時間区間全て分コピーしたものと同じ。
 *      <pre>
 *        { "rule" : "TIMEEVERY",
 *          "ignore" : ("true" | "false")
 *          "agentType" : { "className" : __AgentType__,
 *                          (__AgentConfig__)* },
 *          "total" : __Integer__,
 *          "speedModel" : __SpeedModel__,
 *          "startPlace" : __Tag__,
 *          "conditions" : __Conditions__,
 *          "startTime" : __Time__,
 *          "duration" : __DurationInSec__,
 *          "everyEndTime" : __Time__,
 *          "everySeconds" : __IntervalInSec__,
 *          "goal" : __Tag__,
 *          "plannedRoute" : [ __Tag__, __Tag__, ... ]
 *        }
 *      </pre>
 *     </dd>
 *   <dt>RUBY</dt>
 *     <dd>
 *       Ruby script による generation を行う。
 *       "class" で指定された Ruby のクラス(AgentFactoryBase class の派生クラス)
 *       のインスタンスが生成される。
 *       "config" に、このルール専用のパラメータをJSONで指定できる。
 *      <pre>
 *        { "rule" : "RUBY",
 *          "ruleClass" : __RubyClassNameInString__,
 *          "config" : __Config__
 *        }
 *      </pre>
 *     </dd>
 * </dl>
 * {@code "agentType"} の {@code "className"} および {@code "config"} については、
 * {@link nodagumi.ananPJ.Agents.AgentFactory AgentFactory} で説明されている
 * エージェントクラスとその設定パラメータを参照のこと。
 * <p>
 * Version 0, 1 の形式は以下の通り。
 * <pre>
 * format of generation file of one line:
 * [RULE_STRING,][AgentClass,AgentConf,]TAG,START_TIME,DURATION,TOTAL,EXIT_TAG[,ROUTE...]
 * TAG,START_TIME,DURATION,TOTAL[,EXIT_TAG,NAVIGATED_LINK_TAG]*
 *  (memo: [AgentClass,AgentConf,] Part is only for Ver.1 format.
 *  (memo: STAFF は、2014.12.24 に排除することに決定。)
 *
 * descriptions:
 *  RULE_STRING:    EACH or RANDOM or EACHRANDOM 
 *  AgentClass:	    class name of agents (short name or full path name)
 *  AgentConf:      configuration for the agents. JSON format string
 *  TAG:            agents are generated on the links or nodes with this tag.
 *  START_TIME:     starting time which agents are generated
 *  TOTAL:          total number of generated agents
 *  DURATION:       duration time to finish generating agents from START_TIME
 *  EXIT_TAG:       set the goal of generated agents
 *  ROUTE:          routing point
 *  NAVIGATED_LINK_TAG:
 *                  navigated link which agent meets with the staff
 * example1) EACH,LINK_TAG_1,14:00:00,10,1,EXIT_TAG_2
 * example2) RANDOM,LINK_TAG_2,09:00:00,1,10,EXIT_TAG_3
 * example3) EACHRANDOM,LINK_TAG_3,23:44:12,10,140,1,EXIT_TAG4,STATION
 * example6) TIMEEVERY,NaiveAgent,"{}",LINK_TAG_1,18:00:00,18:00:00,60,60,100,LANE,EXIT_1,EXIT_2,EXIT_3
 * </pre>
 */
public class AgentGenerationFile extends ArrayList<AgentFactory> {
    private Random random = null;
    private double liner_generate_agent_ratio = 1.0;
    private LinkedHashMap<String, ArrayList<String>> definitionErrors = new LinkedHashMap();

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * enum for generation rule type
     */
    static public enum RuleType {
        EACH,
        RANDOM,
        EACHRANDOM,
        TIMEEVERY,
        RUBY,
        LINER_GENERATE_AGENT_RATIO
    }
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Lexicon for Generation Rule
     */
    static public Lexicon ruleLexicon = new Lexicon() ;
    static {
        // RuleType で定義された名前をそのまま文字列で Lexicon を
        // 引けるようにする。
        // 例えば、 RuleType.EACH は、"EACH" で引けるようになる。
        ruleLexicon.registerEnum(RuleType.class) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Lexicon for SpeedCalculationModel
     */
    static public Lexicon speedModelLexicon = new Lexicon() ;
    static {
        speedModelLexicon.registerMulti(new Object[][]
            {{"LANE", SpeedCalculationModel.LaneModel},
             {"lane", SpeedCalculationModel.LaneModel},
             {"PLAIN",SpeedCalculationModel.PlainModel},
             {"plain",SpeedCalculationModel.PlainModel},
             {"CROSSING",SpeedCalculationModel.CrossingModel},
             {"crossing",SpeedCalculationModel.CrossingModel}
            }) ;
    }
    static public SpeedCalculationModel Fallback_SpeedModel =
        //        SpeedCalculationModel.LaneModel ;
        SpeedCalculationModel.CrossingModel ;

    //------------------------------------------------------------
    /**
     * SpeedModel の Fallback 値を取得。
     */
    public SpeedCalculationModel getFallbackSpeedModel() {
        SpeedCalculationModel model = null ;
        Term generationFallback =
            SetupFileInfo.fetchFallbackTerm(fallbackParameters,
                                            "generation",
                                            null) ;
        if(generationFallback != null) {
            String modelString =
                SetupFileInfo.fetchFallbackString(generationFallback,
                                                  "speedModel",
                                                  null) ;
            if(modelString != null) {
                model =
                    (SpeedCalculationModel)speedModelLexicon
                    .lookUp(modelString);
            }
        }
        if(model == null) {
            model = Fallback_SpeedModel ;
        }
        return model ;
    }
    
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * enum FileFormat Version
     */
    public enum FileFormat { Ver0, Ver1, Ver2 }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ファイルフォーマットのバージョン
     */
    public FileFormat fileFormat = FileFormat.Ver0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback 情報。
     */
    private Term fallbackParameters = null ;
    public void setFallbackParameters(Term fallback) {
        fallbackParameters = fallback ;
    }
    public Term getFallbackParameters() {
        return fallbackParameters ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * mode を格納している Map
     */
    public Map<String,Object> modeMap ;

    //============================================================
    /**
     * 生成ルール情報格納用クラス(Base)。
     * Generation Rule は以下の形式の JSON (version 2 format).
     *   
     */
    static private class GenerationConfigBase extends AgentFactory.Config {
        //========================================
        //----------------------------------------
        /**
         * ruleType に応じて、GenerationConfig のインスタンスを作る。
         */
        static public GenerationConfigBase
            newConfigByRuleType(RuleType ruleType) {
            
            GenerationConfigBase genConfig = null;
            switch(ruleType) {
            case EACHRANDOM:
                genConfig = new GenerationConfigForEachRandom() ;
                break ;
            case TIMEEVERY:
                genConfig = new GenerationConfigForTimeEvery() ;
                break ;
            case RUBY:
                genConfig = new GenerationConfigForRuby() ;
                break ;
            case EACH:
            case RANDOM:
                genConfig = new GenerationConfigBase() ;
                break ;
            default:
                Itk.logError("AgentGenerationFile include invalid rule type:",
                             ruleType) ;
                return null ;
            }
            genConfig.ruleType = ruleType ;
            return genConfig ;
        }
        
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成ルールのタイプ
         */
        public RuleType ruleType ;

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

        //----------------------------------------
        /**
         * JSON への変換
         */
        public String toJson(boolean pprintP){
            return toTerm().toJson(pprintP) ;
        }

        //----------------------------------------
        /**
         * JSON Object への変換
         */
        public Term toTerm(){
            Term jTerm = super.toTerm() ;

            jTerm.setArg("rule",ruleLexicon.lookUpByMeaning(ruleType).get(0));
            if(startLinkTag != null)
                jTerm.setArg("startPlace", startLinkTag) ;
            jTerm.setArg("speedModel", 
                           speedModelLexicon.lookUpByMeaning(speedModel).get(0));

            return jTerm ;
        }
        
        //----------------------------------------
        /**
         * JSON Object からパラメータ設定
         */
        public GenerationConfigBase scanJson(AgentGenerationFile factoryList,
                                             Term json,
                                             NetworkMap map) {
            originalInfo = json.toJson() ;

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
                total = (int) (total * factoryList.liner_generate_agent_ratio);
                Itk.logInfo("Agent Population (JSON)", total);
            }

            // speedModel
            speedModel =
                (SpeedCalculationModel)
                speedModelLexicon.lookUp(json.getArgString("speedModel")) ;
            if(speedModel == null) {
                speedModel = factoryList.getFallbackSpeedModel() ;
            }
        
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
         * Factory を追加。
         */
        private void addFactories(AgentGenerationFile factoryList,
                                  NetworkMap map) {
            switch(ruleType) {
            case EACH:
                addFactoriesForEach(factoryList) ;
                break ;
            case RANDOM:
                addFactoriesForRandom(factoryList) ;
                break ;
            default:
                Itk.logError("Invalid Generation Rule:" + ruleType) ;
            }
        }

        //----------------------------------------
        /**
         * EACH 用生成ルーチン
         * 各々の link, node で total 個ずつのエージェントが生成。
         */
        private void addFactoriesForEach(AgentGenerationFile factoryList) {
            for (final MapLink startLink : startLinks) {
                startPlace = startLink ;
                factoryList.add(new AgentFactoryFromLink(this,
                                                         factoryList.random)) ;
            }
            for (final MapNode startNode : startNodes) {
                startPlace = startNode ;
                factoryList.add(new AgentFactoryFromNode(this,
                                                         factoryList.random)) ;
            }
        }

        //----------------------------------------
        /**
         * RANDOM 用生成ルーチン
         * 指定された link, node において、
         * 合計で total 個のエージェントが生成。
         */
        private void addFactoriesForRandom(AgentGenerationFile factoryList) {
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
                    factoryList
                        .add(new AgentFactoryFromLink(this, 
                                                      factoryList.random)) ;
                }
            }
            for (int i = 0; i < this.startNodes.size(); i++) {
                if (chosen_nodes[i] > 0) {
                    this.startPlace = this.startNodes.get(i) ;
                    this.total = chosen_nodes[i] ;
                    factoryList
                        .add(new AgentFactoryFromNode(this,
                                                      factoryList.random)) ;
                }
            }
        }

    } // end class GenerationConfigBase

    //============================================================
    /**
     * 生成ルール情報格納用クラス(EachRandom 用)
     */
    static private class GenerationConfigForEachRandom 
        extends GenerationConfigBase {
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
        public GenerationConfigBase scanJson(AgentGenerationFile factoryList,
                                             Term json,
                                             NetworkMap map) {
            GenerationConfigBase r = super.scanJson(factoryList, json, map) ;
            
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
        private void addFactories(AgentGenerationFile factoryList,
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
                    factoryList
                        .add(new AgentFactoryFromLink(this,
                                                      factoryList.random)) ;
                }
            }
            for (int i = 0; i < this.startNodes.size(); i++) {
                if (chosen_nodes[i] > 0) {
                    this.startPlace = this.startNodes.get(i) ;
                    this.total = chosen_nodes[i] ;
                    factoryList
                        .add(new AgentFactoryFromNode(this,
                                                      factoryList.random)) ;
                }
            }
        }
    } // end class GenerationConfigForEachRandom 

    //============================================================
    /**
     * 生成ルール情報格納用クラス(TimeEvery 用)
     */
    static private class GenerationConfigForTimeEvery
        extends GenerationConfigBase {
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
        public GenerationConfigBase scanJson(AgentGenerationFile factoryList,
                                             Term json,
                                             NetworkMap map) {
            GenerationConfigBase r = super.scanJson(factoryList, json, map) ;
            
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
        private void addFactories(AgentGenerationFile factoryList,
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
                        factoryList
                            .add(new AgentFactoryFromLink(this,
                                                          factoryList.random)) ;
                    } else if (this.startNodes.size() > 0) {
                        MapNode start_node = 
                            this.startNodes.chooseRandom(factoryList.random) ;
                        this.startPlace = start_node ;
                        factoryList
                            .add(new AgentFactoryFromNode(this,
                                                          factoryList.random)) ;
                    } else {
                        Itk.logError("no starting place for generation.") ;
                        Itk.logError_("config",this) ;
                    }
                }
                step_time.advanceSec(every_seconds) ;
            }
        }
    } // end class GenerationConfigForTimeEvery

    //============================================================
    /**
     * 生成ルール情報格納用クラス(Ruby 用)
     */
    static private class GenerationConfigForRuby
        extends GenerationConfigBase {
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

            jTerm.setArg("rule",ruleLexicon.lookUpByMeaning(ruleType).get(0));
            jTerm.setArg("ruleClass", ruleClass) ;
            jTerm.setArg("config", config) ;

            return jTerm ;
        }

        //----------------------------------------
        /**
         * JSON Object からパラメータ設定
         */
        public GenerationConfigBase scanJson(AgentGenerationFile factoryList,
                                             Term json,
                                             NetworkMap map) {
            ruleClass = json.getArgString("ruleClass") ;
            config = json.getArgTerm("config") ;
            
            return this ;
        }

        //----------------------------------------
        /**
         * Factory を追加。
         * RUBY 用生成ルーチン
         */
        private void addFactories(AgentGenerationFile factoryList,
                                  NetworkMap map) {
            // ***********
        }
        
    } // end class GenerationConfigForRuby

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public AgentGenerationFile(final String filename,
                               NetworkMap map,
                               Term _fallbackParameters,
                               boolean display,
                               double linerGenerateAgentRatio,
                               Random _random)
        throws Exception 
    {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        setLinerGenerateAgentRatio(linerGenerateAgentRatio);
        setRandom(_random);
        setFallbackParameters(_fallbackParameters) ;

        scanFile(filename, map, display) ;
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン
     */
    public void scanFile(final String filename,
                         NetworkMap map,
                         boolean display)
        throws Exception
    {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (IOException e) {
            Itk.logError(e.toString()) ;
            if (display) {
                JOptionPane.showMessageDialog(null,
                e.toString(),
                "Fail to open a generation file.",
                JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        // モードラインの読み込みを試す。
        // 呼んだ後、read pointer は先頭へ。
        tryScanModeLine(br) ;

        switch(fileFormat) {
        case Ver0:
        case Ver1:
            scanCsvFile(br, map) ;
            Itk.logInfo("Load Generation File (CSV)", filename) ;
            break ;
        case Ver2:
            scanJsonFile(br, map) ;
            Itk.logInfo("Load Generation File (JSON)", filename) ;
            break ;
        default:
            Itk.logError("Unknown Format Version",
                         fileFormat.toString(), "(file=" + filename + ")") ;
        }
        return ;
    }

    //------------------------------------------------------------
    /**
     * try to check mode line
     * [example]
     *   # { 'version' : '1' }
     * @param reader 最初の行を含む Reader
     * @return modelineの形式であれば true を返す。
     */
    public boolean tryScanModeLine(BufferedReader reader) {
        if(!reader.markSupported()) {
            Itk.logWarn("This reader does not support mark()", reader) ;
            return false ;
        } else {
            try {
                reader.mark(BufferedReadMax) ;
                String line = reader.readLine() ;
                if(line == null) {
                    Itk.logWarn("This file is empty:" + reader) ;
                    return false ;
                } else {
                    boolean scanned = scanModeLine(line) ;
                    if(!scanned) reader.reset() ;
                    return scanned ;
                }
            } catch (Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("something wrong to set mark for:" + reader) ;
                Itk.logError_("BufferedReadMax", BufferedReadMax) ;
                return false ;
            }
        }
    }
    static private int BufferedReadMax = 1024 ; // 最大の1行のサイズ

    //------------------------------------------------------------
    /**
     * mode line check
     * [example]
     *   # { 'version' : '1' }
     * @param modeline 最初の行
     * @return modelineの形式であれば true を返す。
     */
    public boolean scanModeLine(String modeline) {
        if(modeline.startsWith("#")) {
            // 先頭の '#' をカット
            String modeString = modeline ;
            while(modeString.startsWith("#")) modeString = modeString.substring(1) ;
            // のこりを JSON として解釈
            modeMap = (Map<String, Object>)JSON.decode(modeString) ;
            String versionString = modeMap.get("version").toString() ;
            if(versionString != null && versionString.equals("2")) {
                fileFormat = FileFormat.Ver2 ;
            } else if(versionString != null && versionString.equals("1")) {
                fileFormat = FileFormat.Ver1 ;
            } else {
                fileFormat = FileFormat.Ver0 ;
            }
            return true ;
        } else {
            fileFormat = FileFormat.Ver0 ;
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン (CSV file) (Ver.0, Ver.1 file format)
     */
    public void scanCsvFile(BufferedReader br,
                            NetworkMap map)
        throws Exception
    {
        String line = null;
        try {
            // 各行をCSVとして解釈するためのパーザ
            // quotation にはシングルクォート(')を用いる。
            // これは、JSON の文字列がダブルクォートのため。
            //[2014.12.23 I.Noda] csvParser は、ShiftingStringList の中へ。
            ShiftingStringList.setCsvSpecialChars(',','\'','\\') ;

            while ((line = br.readLine()) != null) {
                //一行解析
                GenerationConfigBase genConfig =
                    scanCsvFileOneLine(line, map) ;

                if(genConfig == null) continue ;

                // 経路情報に未定義のタグが使用されていないかチェックする
                checkPlannedRouteInConfig(map, genConfig, line) ;

                // ここから、エージェント生成が始まる。
                addFactoriesByConfig(map, genConfig) ;
            }
        } catch (Exception e) {
            System.err.println("Error in agent generation.");
            System.err.println(line);
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        // 経路情報に未定義のタグが使用されていたら例外を発生させる
        raiseExceptionRouteDefinitionError() ;
    }

    //------------------------------------------------------------
    /**
     * scan one line of CSV file
     */
    private GenerationConfigBase scanCsvFileOneLine(String line,
                                                    NetworkMap map)
        throws IOException
    {
        //コメント行読み飛ばし
        if (line.startsWith("#")) return null ;
        if (line.startsWith(",")) return null ;

        // カラムに分割
        // [2014/12/15 I.Noda]
        // CSV Parser を使うように変更。
        // さらに、ShiftingColumns を使うよにする。
        //String items[] = line.split(",");
        //String items[] = csvParser.parseLine(line) ;
        //int index = 0;
        ShiftingStringList columns =
            ShiftingStringList.newFromCsvRow(line) ;

        // 行の長さチェック
        if(fileFormat == FileFormat.Ver1) {
            if (columns.length() < 7 && columns.length() != 4) {
                System.err.println("malformed line: " + line);
                return null ;
            }
        } else {
            if (columns.length() < 5 && columns.length() != 2) {
                System.err.println("malformed line: " + line);
                return null ;
            }
        }

        // check rule strings
        /* [2014.12.20 I.Noda] should obsolete
         * ruleType が指定されないことがあるのか？
         * これは、特に CSV の場合、致命的なバグの原因となる。
         */
        RuleType ruleType = (RuleType)ruleLexicon.lookUp(columns.top()) ;
        if (ruleType == null)
            // if no rule tag, default tag "EACH" is applied.
            ruleType = RuleType.EACH ;
        else
            columns.shift() ;

        // LINER_GENERATE_AGENT_RATIO の場合、
        // lga_ratio が次に来る。
        // で、読み込んだら次の行。（エージェント生成しない）
        if (ruleType == RuleType.LINER_GENERATE_AGENT_RATIO) {
            double lga_ratio = 0;
            lga_ratio = Double.parseDouble(columns.get()) ;
            if (lga_ratio > 0)
                liner_generate_agent_ratio = lga_ratio;
            return null ;
        }

        // 生成条件の格納用データ
        GenerationConfigBase genConfig = newConfigByRuleType(ruleType) ;

        // 生成の設定情報を以下にまとめて保持。
        genConfig.originalInfo = line ;

        /* [I.Noda] Ver1 以降は、ruleType の直後はエージェントクラス名 */
        if(fileFormat == FileFormat.Ver1) {
            genConfig.agentClassName = columns.nth(0) ;
            genConfig.agentConf = Term.newByJson(columns.nth(1)) ;
            columns.shift(2) ;
        } else {
            /* [2014.12.29 I.Noda]
             * agentClassName を埋めておかないと、directive の処理が出来ない。
             * なので、AgentFactory に指定する規定値を埋める。
             */
            genConfig.agentClassName = AgentFactory.DefaultAgentClassName ;
        }

        // read start link
        // もし start link の解析に失敗したら、次の行へ。
        if(! scanStartLinkTag(Itk.intern(columns.get()), map, genConfig))
            return null ;

        // 出発時刻
        try {
            genConfig.startTime = new SimTime(columns.get()) ;
        } catch(Exception ex) {
            return null ;
        }

        // TIMEEVERYの場合は、出発時刻間隔
        if (ruleType == RuleType.TIMEEVERY) {
            try {
                ((GenerationConfigForTimeEvery)genConfig).everyEndTime =
                    new SimTime(columns.get()) ;
            } catch(Exception ex) {
                return null ;
            }
            ((GenerationConfigForTimeEvery)genConfig).everySeconds =
                Integer.parseInt(columns.get()) ;
        }

        // duration
        genConfig.duration = Double.parseDouble(columns.get()) ;

        // total number of generated agents
        genConfig.total = Integer.parseInt(columns.get());
        if (liner_generate_agent_ratio != 1) {
            Itk.logInfo("use liner_generate_agent_ratio",
                        "genConfig.total:", genConfig.total,
                        "ratio:", liner_generate_agent_ratio);
            genConfig.total = (int) (genConfig.total * liner_generate_agent_ratio);
        }

        // speed model
        /* [2014.12.20 I.Noda] should obsolete
         * speed_model を指定しないことはあるのか？
         * 少なくとも CSV で指定する場合、
         * 混乱の原因以外の何物でもない
         * 一番問題なのは、speed_model に相当するタグでなければ、
         * columns を shift しないところ。
         */
        genConfig.speedModel =
            (SpeedCalculationModel)speedModelLexicon.lookUp(columns.top()) ;
        if(genConfig.speedModel == null) {
            genConfig.speedModel = getFallbackSpeedModel() ;
        } else {
            columns.shift() ;
        }

        // EACHRANDOM
        if (genConfig.ruleType == RuleType.EACHRANDOM) {
            ((GenerationConfigForEachRandom)genConfig).maxFromEachPlace =
                Integer.parseInt(columns.get()) ;
        }

        // goal を scan
        genConfig.goal = new Term(columns.top(), true) ;

        // ゴールより後ろの読み取り。
        if(!scanRestColumns(columns, map, genConfig))
            return null ;

        return genConfig ;
    }

    //------------------------------------------------------------
    /**
     * new Generation Config for ruleType
     */
    private GenerationConfigBase newConfigByRuleType(RuleType ruleType) {
        return GenerationConfigBase.newConfigByRuleType(ruleType) ;
    }

    //------------------------------------------------------------
    /**
     */
    public void setLinerGenerateAgentRatio(double _liner_generate_agent_ratio) {
        liner_generate_agent_ratio = _liner_generate_agent_ratio;
    }

    //------------------------------------------------------------
    /**
     */
    public void setRandom(Random _random) {
        random = _random;
        for (AgentFactory factory : this) {
            factory.setRandom(_random);
        }
    }

    //------------------------------------------------------------
    /**
     * start_link_tag の解析パターン
     */
    static private Pattern startpat = Pattern.compile("(.+)\\((.+)\\)");

    //------------------------------------------------------------
    /**
     * start_link_tag の解析
     * start_link_tag は、
     *    Tag | Tag(Cond;Cond;...) 
     * という形式らしい。
     */
    private boolean scanStartLinkTag(String start_link_tag,
                                     NetworkMap map,
                                     GenerationConfigBase genConfig) {
        Matcher tag_match = startpat.matcher(start_link_tag);
        if (tag_match.matches()) {
            start_link_tag = tag_match.group(1);
            genConfig.conditions = tag_match.group(2).split(";");
        }

        genConfig.startLinkTag = start_link_tag ;

        /* get all links with the start_link_tag */
        map.getLinks().findTaggedLinks(start_link_tag, genConfig.startLinks) ;

        /* get all nodes with the start_link_tag */
        map.getNodes().findTaggedNodes(start_link_tag, genConfig.startNodes) ;

        if (genConfig.startLinks.size() == 0 &&
            genConfig.startNodes.size() == 0) {
            Itk.logError("no matching start:" + start_link_tag);
            return false ;
        } else {
            return true ;
        }
    }

    //------------------------------------------------------------
    /**
     * 残りの column の読み込み
     */
    private boolean scanRestColumns(ShiftingStringList columns,
                                    NetworkMap map,
                                    GenerationConfigBase genConfig) {
        genConfig.plannedRoute = new ArrayList<Term>();

        // goal and route plan
        //String goal = items[index];
        if (genConfig.goal.isNull()) {
            System.err.println("no matching link:" + columns.top() +
                               " while reading agent generation rule.");
            return false ;
        }
        columns.shift() ;
        while(!columns.isEmpty()) {
            String tag = Itk.intern(columns.get()) ;
            if (tag != null &&
                !tag.equals("")) {
                Term tagTerm = 
                    tryScanDirectiveAndMakeTerm(tag, columns) ;
                genConfig.plannedRoute.add(tagTerm) ;
            }
        }
        return true ;
    }

    //------------------------------------------------------------
    /**
     * WAIT directive の解釈
     * [2014.12.29 I.Noda]
     * ここだけからはどうしても、WAIT_* 系の処理を、
     * WaitRunningAroundAgent に局所化出来ない。
     * CSV である限り、括弧"()"の位置とか、
     * 一般的に扱う処理方法が見当たらない。
     */
    private Term tryScanDirectiveAndMakeTerm(String head,
                                             ShiftingStringList columns) {
        try {
            Matcher matchFull = 
                WaitDirective.FullPattern.matcher(head) ;
            if(matchFull.matches()) {
                // 引数込みですでにheadに含まれている。
                // directive 用の Term に変換する。
                WaitDirective directive =
                    WaitDirective.scanDirective(head) ;
                if(directive != null) {
                    return directive.toTerm() ;
                } else {
                    return new Term(head, true) ;
                }
            }
            Matcher matchHead =
                WaitDirective.HeadPattern.matcher(head) ;
            if(matchHead.matches()) {
                // CSV 解釈で、カンマで引数が分断されている場合。
                String fullForm = 
                    head + "," + columns.nth(0) + "," + columns.nth(1) ;
                Matcher matchFull2 = 
                    WaitDirective.FullPattern.matcher(fullForm) ;
                if(matchFull2.matches()) {
                    String directive = matchFull2.group(1) ;
                    WaitDirective.Type wait =
                        (WaitDirective.Type)
                        WaitDirective.lexicon.lookUp(directive) ;
                    if(wait != null) {
                        //正しい 3引数 wait directive は、まとめたものを返す。
                        columns.shift(2) ;
                        // 再帰呼び出し
                        return tryScanDirectiveAndMakeTerm(fullForm,
                                                           columns) ;
                    }
                }
                // ここで head に match しているとするならおかしい。
                Itk.logWarn("strange tag form in planned route:" + head) ;
            }
            // それ以外の場合は、もとの head をTerm化して返す。
            return new Term(head, true) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            return new Term(head, true) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン (JSON file) (Ver.2 file format)
     */
    public void scanJsonFile(BufferedReader br,
                             NetworkMap map)
        throws Exception
    {
        Term json = Term.newByScannedJson(JSON.decode(br),true) ;
        if(json.isArray()) {
            for(Object _item : json.getArray()) {
                Term item = (Term)_item ;
                if(item.isObject()) {
                    GenerationConfigBase genConfig = 
                        scanJsonFileOneItem(item, map) ;

                    if(genConfig == null) continue ;

                    // 経路情報に未定義のタグが使用されていないかチェックする
                    checkPlannedRouteInConfig(map, genConfig, item.toJson()) ;

                    // ここから、エージェント生成が始まる。
                    addFactoriesByConfig(map, genConfig) ;
                } else {
                    Itk.logError("wrong json for generation rule:",item.toJson()) ;
                    continue ;
                }
            }
        } else {
            Itk.logError("wrong json for generation file:",json.toJson()) ;
            throw new Exception("wrong json for generation file:" + json.toJson()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン (JSON one line) (Ver.2 file format)
     */
    public GenerationConfigBase scanJsonFileOneItem(Term json,
                                                    NetworkMap map)
    {
        // ignore が true なら、項目を無視する。
        if(json.getArgBoolean("ignore")) { return null ; }

        // rule
        RuleType ruleType = 
            (RuleType)ruleLexicon.lookUp(json.getArgString("rule")) ;
        GenerationConfigBase genConfig = newConfigByRuleType(ruleType) ;

        return genConfig.scanJson(this, json, map) ;

    }

    //------------------------------------------------------------
    /**
     * 経路情報に未定義のタグが使用されていないかチェックする
     */
    private void checkPlannedRouteInConfig(NetworkMap map,
                                           GenerationConfigBase genConfig,
                                           String where) {
        ArrayList<String> routeErrors =
            checkPlannedRoute(genConfig.agentClassName,
                              map, genConfig.plannedRoute);
        if (! routeErrors.isEmpty()) {
            definitionErrors.put(where, routeErrors);
        }
    }

    //------------------------------------------------------------
    /**
     * 経路情報に未定義のタグが使用されていたらその内容を返す
     */
    public ArrayList<String> checkPlannedRoute(String agentClassName,
                                               NetworkMap map,
                                               List<Term> plannedRoute) {
        ArrayList<Term> linkTags = new ArrayList();
        ArrayList<Term> nodeTags = new ArrayList();
        int index = 0;
        while (index < plannedRoute.size()) {
            Term candidate = plannedRoute.get(index);

            if(AgentFactory
               .isKnownDirectiveInAgentClass(agentClassName, candidate)) {
                AgentFactory
                    .pushPlaceTagInDirectiveByAgentClass(agentClassName,
                                                         candidate,
                                                         nodeTags,
                                                         linkTags) ;
            } else {
                nodeTags.add(candidate);
            }
            index += 1 ;
        }

        ArrayList<String> result = new ArrayList();
        for (Term nodeTag : nodeTags) {
            boolean found = false;
            for (MapNode node : map.getNodes()) {
                if (node.hasTag(nodeTag)){
                    found = true;
                    break;
                }
            }
            if (! found) {
                result.add("Undefined Node Tag: " + nodeTag);
            }
        }
        for (Term linkTag : linkTags) {
            if (! map.getLinks().tagExistP(linkTag.getString()))
                result.add("Undefined Link Tag: " + linkTag);
        }
        return result;
    }

    //------------------------------------------------------------
    /**
     * 経路情報に未定義のタグが使用されていたら例外を発生させる
     * エラー情報は、checkPlannedRouteInConfig() で調べて蓄えられている。
     */
    private void raiseExceptionRouteDefinitionError()
        throws Exception
    {
        if (! definitionErrors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            //definitionErrors.forEach((_line, messages) -> {
            //    errorMessage.append("line: ").append(_line).append("\n");
            //    messages.forEach(message -> errorMessage.append("    ").append(message).append("\n"));
            //});
            Iterator it = definitionErrors.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ArrayList<String>> entry = (Map.Entry)it.next();
                String _line = entry.getKey();
                ArrayList<String>messages = entry.getValue();
                errorMessage.append("line: ").append(_line).append("\n");
                for (String message: messages) {
                    errorMessage.append("    ").append(message).append("\n");
                }
            }
            throw new Exception(errorMessage.toString());
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント生成
     */
    private void addFactoriesByConfig(NetworkMap map,
                                      GenerationConfigBase genConfig) {
        genConfig.fallbackParameters = fallbackParameters ;
        genConfig.addFactories(this, map) ;
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
