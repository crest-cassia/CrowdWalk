// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents.Factory;

import java.lang.reflect.Method;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.Agents.AwaitAgent;
import nodagumi.ananPJ.Agents.*;

import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;

import nodagumi.Itk.*;


//======================================================================
/**
 * エージェント生成機構。
 * <h3>現在利用可能なエージェントリスト</h3>
 * Generation File に書ける config の内容は、以下のリンク先参照。
 * <ul>
 *  <li> {@link nodagumi.ananPJ.Agents.WalkAgent WalkAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.AwaitAgent AwaitAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.NaiveAgent NaiveAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.CapriciousAgent CapriciousAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.BustleAgent BustleAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.RationalAgent RationalAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.RubyAgent RubyAgent} </li>
 * </ul>
 */
public abstract class AgentFactory {
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * エージェントの短縮名の登録
     */
    static public ClassFinder classFinder = new ClassFinder() ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * デフォルトのエージェント登録
     */
    static {
        registerAgentClass(WalkAgent.class) ;
        registerAgentClass(AwaitAgent.class) ;
        registerAgentClass(NaiveAgent.class) ;
        registerAgentClass(CapriciousAgent.class) ;
        registerAgentClass(BustleAgent.class) ;
        registerAgentClass(RationalAgent.class) ;
        registerAgentClass(RubyAgent.class) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * 使用出来るエージェントタイプの登録
     */
    static public void registerAgentClass(Class<?> agentClass) {
        try {
            classFinder.registerClassDummy(agentClass) ;
            String aliasName =
                (String)
                classFinder.callMethodForClass(agentClass, "getTypeName", true) ;
            classFinder.alias(aliasName, agentClass) ;
        } catch (Exception ex) {
            Itk.dumpStackTraceOf(ex) ;
            Itk.logError("cannot process registerAgentClass()") ;
            Itk.logError_("agentClass",agentClass) ;
        }
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * エージェントクラスのインスタンス生成（ゼロ引数で）
     */
    static public AgentBase newAgentByName(String agentClassName) {
        try {
            AgentBase agent =
                (AgentBase)classFinder.newByName(agentClassName) ;
            return agent ;
        } catch (Exception ex) {
            Itk.dumpStackTraceOf(ex) ;
            Itk.logError("can not find the class") ;
            Itk.logError_("agentClassName", agentClassName) ;
            return null ;
        }
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * エージェントクラスのダミーエージェントの取得
     */
    static public Object getDummyAgent(Class<?> agentClass) {
        return classFinder.getClassDummy(agentClass) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * 使用可能なエージェントクラスのリスト
     */
    static public String[] getKnownAgentClassNameList() {
        return classFinder.aliasTable.keySet().toArray(new String[0]) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** Config */
    public AgentFactoryConfig config ;
    /** 目的地 */
    final public Term getGoal() { return config.goal ; }
    final public Term setGoal(Term goal) {
        return config.goal = goal ;
    }

    /** 経由地 */
    final public List<Term> getPlannedRoute() { return config.plannedRoute ; }
    final public List<Term> setPlannedRoute(List<Term> route) {
        return config.plannedRoute = route ;
    }
    /** スピードモデル */
    final public SpeedCalculationModel getSpeedModel() {
        return config.speedModel ;
    }
    /** start Time
     * TIME EVERY が config.startTime 共通にならないため個別に記憶する。
     */
    private SimTime startTime ;
    final public SimTime getStartTime() { return startTime ; }
    /** duration */
    final public double getDuration() { return config.duration ; }
    
    /** total 
     * rule の total と factory の totalを分けておかないと、おかしくなる。
     * 
     */
    private int totalInFactory ;
    final public int getTotalInFactory() { return totalInFactory ; }
    final public int getRuleTotal() { return config.total ; }
    
    /** fallback parameters */
    final public Term getFallbackParameters() {
        return config.fallbackParameters ;
    }
    /** 設定文字列（generation file 中の設定情報の文字列)。 */
    final public String getConfigLine() { return config.originalInfo ; }
    /** 
     * ルールの名前。
     * CSV 形式では、ルールの順番の数字の文字列。
     * Json 形式で、名前 "name" が与えられている場合は、その名前（文字列）。
     * "name" が与えられていなければ、ルールの順番の数字の文字列。
     * ログ出力で使用。
     */
    final public String getRuleName() { return config.ruleName ; } ;
    
    
    /*********/
    /** エージェントに付与するタグ */
    private List<String> tags = new ArrayList<String>();
    public List<String> getTags() { return tags ; }
    //------------------------------------------------------------
    /**
     *  生成ルールの conditions の処理。
     *  これは単純に、生成したエージェントへのタグとなる。
     */
    protected void parse_conditions(String[] conditions) {
        if (conditions == null) return;
        for (int i = 0; i < conditions.length; i++) {
            tags.add(conditions[i]);
        }
    }

    protected boolean enabled = true;
    public boolean isEnabled() { return enabled ; }

    Random random = null;
    
    //------------------------------------------------------------
    /**
     *  Config によるコンストラクタ
     */
    public AgentFactory(AgentFactoryConfig config,
                        OBNode _startPlace,
                        int _totalInFactory,
                        Random _random) {
        init(config, _startPlace, _totalInFactory, _random) ;
    }

    //------------------------------------------------------------
    /**
     *  初期化。他で Override できるように。
     */
    public void init(AgentFactoryConfig _config,
                     OBNode _startPlace,
                     int _totalInFactory,
                     Random _random) {
        config = _config ;
        random = _random;
        startTime = config.startTime.newSimTime();

        /** config.total は、途中で変更になるので、コピーしておく */
        totalInFactory = _totalInFactory ;

        parse_conditions(config.conditions);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * エージェントクラスの規定値
     */
    static public String DefaultAgentClassName = "NaiveAgent" ;

    //------------------------------------------------------------
    /**
     *  エージェントクラスの名前を取得格納。
     */
    final public String getAgentClassName() {
        if(config.agentClassName == null) {
            return DefaultAgentClassName ;
        } else {
            return config.agentClassName ;
        }
    }
    
    /** agent config */
    public Term getAgentConf() { return config.agentConf ; }
    
    public Term setAgentConf(Term newConf) {
        config.agentConf = newConf ;
        return config.agentConf ;
    }

    //------------------------------------------------------------
    /**
     *  終了判定。
     *  tryUpdateAndGenerate だけから参照。
     */
    protected boolean isFinished(SimTime currentTime) {
        return (currentTime.calcDifferenceFrom(getStartTime()) > getDuration()
                &&
                getNAgentsRemain() <= 0) ;
    }

    //------------------------------------------------------------
    /**
     * エージェントを初期化。
     * すぐにエージェントに制御を戻す。
     * （ruby generation rule への対応のため）
     */
    public void initAgent(AgentBase agent, Term fallback) {
        agent.initByFactory(this, fallback) ;
    }
    
    //------------------------------------------------------------
    /**
     * エージェントを初期位置(startPlace)に置く。
     */
    abstract protected void placeAgent(AgentBase agent);

    /* [2014.12.24 I.Noda] should fixed
     * 以下のアルゴリズム、正確に正しく total のエージェントを生成しない
     * 可能性がある。
     * 予め total 個の乱数時刻を発生させ、それをソートしておき、
     * 指定した時間までの分を生成するようにすべき。
     */
    int nAgentsGenerated = 0;

    //------------------------------------------------------------
    /**
     *  生成されたエージェント数。
     */
    final public int getNAgentsGenerated() { return nAgentsGenerated ; }

    //------------------------------------------------------------
    /**
     *  残りのエージェント数。
     */
    public int getNAgentsRemain() {
        return getTotalInFactory() - getNAgentsGenerated() ;
    }

    /* TODO must wait finish until generate &
     * must control here with scenario */
    //------------------------------------------------------------
    /**
     * エージェント生成
     */
    public void tryUpdateAndGenerate(EvacuationSimulator simulator,
                                     SimTime currentTime,
                                     List<AgentBase> agents) {
        if (!isEnabled()) return;

        if (isFinished(currentTime)) {
            enabled = false;
            return;
        }

        if (currentTime.isBefore(getStartTime())) {
            /* not yet time to start generating agents */
            return;
        }

        // 生成するエージェントの数を求める。

        int nAgentsToGen = 0 ;
        if(config.ruleType == AgentFactoryList.RuleType.INDIVIDUAL) {
            nAgentsToGen =
                getIndividualConfigList().remainSizeBefore(currentTime) ;
        } else {
            nAgentsToGen = getNAgentsRemain() ;
            double duration_left =
                getStartTime().getAbsoluteTime() + getDuration()
                - currentTime.getAbsoluteTime() ;
            if (duration_left > 0) {
                double r
                    = ((double)(nAgentsToGen) /
                       duration_left * currentTime.getTickUnit()) ;
                // tkokada: free timeScale update
                if (((int) r) > nAgentsToGen)
                    r = nAgentsToGen;
                nAgentsToGen = (int)r;
                if (random.nextDouble() < r - (double)nAgentsToGen) {
                    nAgentsToGen++;
                }
            }
        }
        /* else, no time left, must generate all remains */

        // fallbacks
        Term fallbackForAgent = getFallbackForAgent() ;

        /* [I.Noda] ここで Agent 生成 */
        for (int i = 0; i < nAgentsToGen; ++i) {
            nAgentsGenerated++;
            launchAgent(getAgentClassName(),
                        simulator,
                        currentTime,
                        agents,
                        fallbackForAgent) ;
        }
    }

    //------------------------------------------------------------
    /**
     * agent 一人を生成。
     * @param agentClassName : エージェントクラス名。
     */
    public AgentBase launchAgent(String agentClassName,
                                 EvacuationSimulator simulator,
                                 SimTime currentTime,
                                 List<AgentBase> agents,
                                 Term fallbackForAgent) {
        AgentBase agent = null ;
        try {
            agent = newAgentByName(agentClassName) ;
        } catch (Exception ex) {
            Itk.logError("class name not found") ;
            Itk.logError_("agentClassName", agentClassName) ;
            Itk.quitWithStackTrace(ex);
        }
        try {
            agent.init(random, simulator, this, currentTime,
                       fallbackForAgent) ;
        } catch (Exception ex) {
            Itk.logError("initialization error for agent") ;
            Itk.logError_("agent=", agent) ;
            Itk.logError_("agentConfig=", getAgentConf()) ;
            Itk.logError_("fallback=", fallbackForAgent) ;
            Itk.quitWithStackTrace(ex) ;
        }

        agents.add(agent);
        placeAgent(agent); // この時点では direction が 0.0 のため、add_agent_to_lane で agent は登録されない
        setupAgentByIndividualConfig(agent) ;
        agent.prepareForSimulation() ;
        agent.getCurrentLink().agentEntersOnLane(agent); // ここで add_agent_to_lane させる

        return agent ;
    }

    //------------------------------------------------------------
    /**
     * agent 用の fallback を取得。
     */
    
    public Term getFallbackForAgent() {
        if (getFallbackParameters() != null) {
            return SetupFileInfo.filterFallbackTerm(getFallbackParameters(),
                                                    "agent") ;
        } else {
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * 扱い可能な directive かのチェック
     */
    public boolean isKnownDirectiveInAgentClass(Term directive) {
        return isKnownDirectiveInAgentClass(getAgentClassName(), directive) ;
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * 扱い可能な directive かのチェック
     */
    static public boolean isKnownDirectiveInAgentClass(String className,
                                                       Term directive) {
        try {
            return
                (Boolean)
                classFinder
                .callMethodForClass(className, "isKnownDirective", false,
                                    directive) ;
            /*
            Class<?> klass = classFinder.get(className) ;
            Object agent = getDummyAgent(klass) ;
            Method method = klass.getMethod("isKnownDirective",Term.class) ;
            return (boolean)method.invoke(agent,directive) ;
            */
        } catch(Exception ex) {
            Itk.dumpStackTraceOf(ex) ;
            Itk.logError("can not check the directive") ;
            Itk.logError_("directive", directive) ;
            Itk.logError_("agentClass", className) ;
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * directive の中の経由場所tagの取り出し
     */
    public int pushPlaceTagInDirectiveByAgentClass(Term directive,
                                                   ArrayList<Term> nodeTagList,
                                                   ArrayList<Term> linkTagList) {
        return pushPlaceTagInDirectiveByAgentClass(getAgentClassName(),
                                                   directive,
                                                   nodeTagList,
                                                   linkTagList) ;
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * directive の中の経由場所tagの取り出し
     */
    static public int pushPlaceTagInDirectiveByAgentClass(String className,
                                                          Term directive,
                                                          ArrayList<Term> nodeTagList,
                                                          ArrayList<Term> linkTagList)
    {
        try {
            return
                (Integer)
                classFinder
                .callMethodForClass(className, "pushPlaceTagInDirective", false,
                                    directive, nodeTagList, linkTagList) ;
        } catch (Exception ex) {
            Itk.dumpStackTraceOf(ex) ;
            Itk.logError("can not pushPlaceTag.") ;
            Itk.logError_("directive", directive) ;
            Itk.logError_("agentClass", className) ;
            return 0 ;
        }
    }

    //------------------------------------------------------------
    /**
     * planned route の中で、経由地点の tag を取り出す。
     * directive の中の経由場所tagも Agent Class に応じて取り出す。
     */
    public ArrayList<Term> getNakedPlannedRoute() {
        ArrayList<Term> routeTags = new ArrayList<Term>();

        for(Term candidate : getPlannedRoute()) {
            if(isKnownDirectiveInAgentClass(candidate)) {
                pushPlaceTagInDirectiveByAgentClass(candidate, routeTags, routeTags) ;
            } else {
                routeTags.add(candidate);
            }
        }
        return routeTags;
    }

    //------------------------------------------------------------
    /**
     * planned route のクローンを作る。
     * agent の generation で利用。
     */
    public List<Term> clonePlannedRoute() {
        if(getPlannedRoute() == null) {
            return new ArrayList<Term>() ;
        } else {
            return new ArrayList<Term>(getPlannedRoute()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント生成ルールの情報を文字列で返す。
     * パネル表示用。
     */
    abstract public String getStartInfo();

    //------------------------------------------------------------
    /**
     * エージェント生成の出発地点のオブジェクトを返す。
     * パネル用。
     */
    abstract public OBNode getStartObject();

    public int getMaxGeneration() {
        return getTotalInFactory();
    }

    public void setRandom(Random _random) {
        random = _random;
    }

    //------------------------------
    /**
     * 個別パラメータを持っているかどうか？
     */
    final public IndividualConfigList getIndividualConfigList() {
        return config.individualConfigList ;
    }
    //------------------------------
    /**
     * 個別パラメータを持っているかどうか？
     */
    public boolean hasIndividualConfig() {
        return (getIndividualConfigList() != null  &&
                getIndividualConfigList().isAvailable()) ;
    }
    
    //------------------------------
    /**
     * 個別パラメータでエージェントを設定。
     */
    public void setupAgentByIndividualConfig(AgentBase agent) {
        if(hasIndividualConfig()) {
            Term config = getIndividualConfigList().getNext() ;
            agent.setupByIndividualConfig(config) ;
        }
    }
    
}

//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
