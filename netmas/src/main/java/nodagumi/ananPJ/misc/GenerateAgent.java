// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

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

import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkMapBase;

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
public abstract class GenerateAgent {
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
            ex.printStackTrace() ;
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
            ex.printStackTrace() ;
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

    //============================================================
    //============================================================
    /**
     * エージェント生成用設定情報用クラス
     * あまりに引数が多いので、整理。
     */
    static public class Config {
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
        public double startTime = 0.0 ;

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

        //------------------------------
        /**
         * JSONへの変換用
         */
        public Term toTerm() {
            Term jObject = new Term() ;
            { // agentType
                Term agentType = new Term() ;
                /* [2014.12.28 I.Noda] obsolete
                if(agentConf == null && agentConfString != null)
                    agentConf = JSON.decode(agentConfString) ;
                */
                agentType.setArg("className", agentClassName) ;
                agentType.setArg("config", agentConf) ;
                jObject.setArg("agentType", agentType) ;
            }
            jObject.setArg("startPlace",startPlace) ;
            jObject.setArg("conditions",conditions);
            jObject.setArg("goal",goal);
            jObject.setArg("plannedRoute",plannedRoute) ;
            jObject.setArg("startTime",Itk.formatSecTime((int)startTime)) ;
            jObject.setArg("duration",duration) ;
            jObject.setArg("total",total) ;
            jObject.setArg("speedModel", speedModel) ;

            return jObject ;
        }
    } // end class Config

    public Term goal;
    public List<Term> planned_route;
    double start_time, duration;
    int total;
    public SpeedCalculationModel speedModel = null;
    Random random = null;
    public List<String> tags = new ArrayList<String>();

    public boolean enabled = true;

    /**
     * 設定文字列（generation file 中の設定情報の文字列
     */
    public String configLine;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * エージェントクラスの規定値
     */
    static public String DefaultAgentClassName = "NaiveAgent" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントクラスの名前を格納。
     */
    public String agentClassName = DefaultAgentClassName ;
    public Term agentConf = null ; // config in json Term

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback parameters
     */
    public Term fallbackParameters = null ;

    //------------------------------------------------------------
    /**
     *  Config によるコンストラクタ
     */
    public GenerateAgent(Config config, Random _random) {
        if(config.agentClassName != null && config.agentClassName.length() > 0) {
            agentClassName = config.agentClassName ;
            agentConf = config.agentConf ;
        }
        goal = config.goal ;
        planned_route = config.plannedRoute ;
        start_time = config.startTime ;
        duration = config.duration ;
        total = config.total ;
        speedModel = config.speedModel ;
        fallbackParameters = config.fallbackParameters ;
        random = _random;
        configLine = config.originalInfo ;

        parse_conditions(config.conditions);
    }

    protected void parse_conditions(String[] conditions) {
        if (conditions == null) return;
        for (int i = 0; i < conditions.length; i++) {
            final String condition = conditions[i];
            tags.add(condition);
        }
    }

    protected boolean finished(double time) {
        return time > start_time + duration && generated >= total;
    }

    abstract protected void place_agent(AgentBase agent);

    /* [2014.12.24 I.Noda] should fixed
     * 以下のアルゴリズム、正確に正しく total のエージェントを生成しない
     * 可能性がある。
     * 予め total 個の乱数時刻を発生させ、それをソートしておき、
     * 指定した時間までの分を生成するようにすべき。
     */
    int generated = 0;

    /* TODO must wait finish until generate &
     * must control here with scenario */
    //------------------------------------------------------------
    /**
     * エージェント生成
     */
    public void tryUpdateAndGenerate(EvacuationSimulator simulator,
                                     double relTime,
                                     List<AgentBase> agents) {
        double absTime = simulator.calcAbsoluteTime(relTime) ;
        double timeScale = simulator.getTimeScale() ;

        if (!enabled) return;

        if (finished(absTime)) {
            enabled = false;
            return;
        }

        if (absTime < start_time) {
            /* not yet time to start generating agents */
            return;
        }

        double duration_left = start_time + duration - absTime;
        int agent_to_gen = total - generated;
        if (duration_left > 0) {
            double r = (double)(agent_to_gen) / duration_left * timeScale;
            // tkokada: free timeScale update
            if (((int) r) > agent_to_gen)
                r = agent_to_gen;
            agent_to_gen = (int)r;
            if (random.nextDouble() < r - (double)agent_to_gen) {
                agent_to_gen++;
            }
        }
        /* else, no time left, must generate all remains */

        // fallbacks
        Term fallbackForAgent =
            ((fallbackParameters != null) ?
             fallbackParameters.filterArgTerm("agent",
                                              NetworkMap.FallbackSlot) :
             null) ;

        /* [I.Noda] ここで Agent 生成 */
        for (int i = 0; i < agent_to_gen; ++i) {
            generated++;
            AgentBase agent = null;
            try {
                agent = newAgentByName(agentClassName) ;
                agent.init(random, simulator, this, relTime);
                agent.initByConf(agentConf, fallbackForAgent) ;
            } catch (Exception ex ) {
                Itk.logError("class name not found") ;
                Itk.logError_("agentClassName", agentClassName) ;
                ex.printStackTrace();
                System.exit(1) ;
            }

            agents.add(agent);
            place_agent(agent); // この時点では direction が 0.0 のため、add_agent_to_lane で agent は登録されない

            agent.prepareForSimulation(timeScale);
            agent.getCurrentLink().agentEnters(agent);  // ここで add_agent_to_lane させる
        }
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * 扱い可能な directive かのチェック
     */
    public boolean isKnownDirectiveInAgentClass(Term directive) {
        return isKnownDirectiveInAgentClass(agentClassName, directive) ;
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
                (boolean)
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
            ex.printStackTrace() ;
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
                                                   ArrayList<Term> goalList) {
        return pushPlaceTagInDirectiveByAgentClass(agentClassName,
                                                   directive,
                                                   goalList) ;
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * directive の中の経由場所tagの取り出し
     */
    static public int pushPlaceTagInDirectiveByAgentClass(String className,
                                                          Term directive,
                                                          ArrayList<Term> goalList)
    {
        try {
            return
                (int)
                classFinder
                .callMethodForClass(className, "pushPlaceTagInDirective", false,
                                    directive, goalList) ;
            /*
            Class<?> klass = classFinder.get(agentClassName) ;
            Object agent = getDummyAgent(klass) ;
            Method method =
                klass.getMethod("pushPlaceTagInDirective",
                                Term.class,
                                ArrayList.class) ;
            return (int)method.invoke(agent,directive,goalList) ;
            */
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("can not pushPlaceTag.") ;
            Itk.logError_("directive", directive) ;
            Itk.logError_("agentClass", className) ;
            return 0 ;
        }
    }

    public ArrayList<Term> getPlannedRoute() {
        ArrayList<Term> goal_tags = new ArrayList<Term>();

        int next_check_point_index = 0;
        while (planned_route.size() > next_check_point_index) {
            Term candidate = planned_route.get(next_check_point_index);

            if(isKnownDirectiveInAgentClass(candidate)) {
                pushPlaceTagInDirectiveByAgentClass(candidate, goal_tags) ;
            } else {
                goal_tags.add(candidate);
            }
            next_check_point_index += 1;
        }
        return goal_tags;
    }

    abstract public String getStart();
    abstract public OBNode getStartObject();

    public int getMaxGeneration() {
        return total;
    }

    public abstract void dumpAgentToGenerate(PrintWriter pw);

    public void setRandom(Random _random) {
        random = _random;
    }

    public void setDuration(double _duration) {
        duration = _duration;
    }

    public double getDuration() {
        return duration;
    }
}

class GenerateAgentFromLink extends GenerateAgent {
    MapLink start_link;

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public GenerateAgentFromLink(Config config, Random random) {
        super(config, random) ;
        start_link = (MapLink)config.startPlace ;
    }

    @Override
    protected boolean finished(double time) {
        if (super.finished(time) || start_link.isShutOff()) return true;
        return false;
    }

    @Override
    protected void place_agent(AgentBase agent) {
        agent.placeAtRandomPosition(start_link) ;
        //start_link.agentEnters(agent);
    }

    @Override
    public String getStart() {
        return start_link.getTagString() + 
        "(" +  start_link.ID + ")" +
        " on " + start_time +
        " ("  + total +
        " in " + duration + "s)";
    }
    @Override
    public OBNode getStartObject() { return start_link; }

    @Override
    public void dumpAgentToGenerate(PrintWriter pw) {
        pw.print("gen_link");

        pw.print("," + start_link.ID);
        pw.print("," + start_time);
        pw.print("," + duration);
        
        pw.print("," + generated);
        pw.print("," + total);

        pw.print("," + goal);
        pw.print("," + planned_route.size());
        for (Term checkpoint : planned_route) {
            pw.print("," + checkpoint);
        }
        pw.print("," + tags.size());
        for (String tag : tags) {
            pw.print("," + tag);
        }
        pw.println();
    }
}


class GenerateAgentFromNode extends GenerateAgent {
    MapNode start_node;

    //------------------------------------------------------------
    /**
     * Config によるコンストラクタ
     */
    public GenerateAgentFromNode(Config config, Random random) {
        super(config, random) ;
        start_node = (MapNode)config.startPlace ;
    }

    //------------------------------------------------------------
    /**
     * エージェントを初期位置に置く。
     */
    @Override
    protected void place_agent(AgentBase agent) {
        agent.place(null, start_node, 0.0) ;
    }
    //------------------------------------------------------------
    /**
     * エージェントを初期位置に置く。 (obsolete)
     * [2015.01.10 I.Noda]
     * おそらく、WalkAgent あたりから取ってきた、古いコード。
     * 効率悪く、意味不明の操作が多い。
     * 上記の適宜に置き換え
     */
    protected void place_agent_obsolete(AgentBase agent) throws Exception {
        /*
         */
        MapLinkTable way_candidates = start_node.getPathways(); 
        double min_cost = Double.MAX_VALUE;
        double min_cost_second = Double.MAX_VALUE;
        MapLink way = null;
        MapLink way_second = null;
        MapLinkTable way_samecost = null;
        final Term next_target;
        List<Term> plannedRoute = getPlannedRoute();
        if (plannedRoute.size() == 0)
            next_target = goal;
        else
            next_target = getPlannedRoute().get(0);

        for (MapLink way_candidate : way_candidates) {
            if (way_candidate.hasTag(goal)){
                /* finishing up */
                way = way_candidate;
                break;
            } else if (way_candidate.hasTag(next_target)){
                /* reached mid_goal */
                way = way_candidate;
                break;
            }

            MapNode other = way_candidate.getOther(start_node);
            double cost = other.getDistance(next_target) ;
            cost += way_candidate.length;

            if (cost < min_cost) {
                min_cost = cost;
                way = way_candidate;
                way_samecost = null;
            } else if (cost == min_cost) {
                if (way_samecost == null)
                    way_samecost = new MapLinkTable();
                way_samecost.add(way_candidate);
                if (cost < min_cost) min_cost = cost;
            }
        }

        if (way_samecost != null) {
            int i = (int)(random.nextDouble() * way_samecost.size());
            if (i != way_samecost.size()) {
                way = way_samecost.get(i);
            }
        }
        
        if (way == null) {
            way = way_second;
        }

        MapLink link = null;
        if (way == null) {
            link = start_node.getPathways().get(0);
        } else {
            link = way;
        }
        for (MapLink way_candidate : way_candidates) {
            if (way != way_candidate) {
                link = way_candidate;
                break;
            }
        }
        //MapLink link = start_node.getPathways().get(0);
        link.setup_lanes();
        agent.place(link, start_node, 0.0) ;
        //link.agentEnters(agent);
    }

    @Override
    public String getStart() {
        return start_node.getTagString() +
        "(" +  start_node.ID + ")" +
        " from " + start_time +
        " ("  + total +
        " in " + duration + "s)";
    }
    @Override
    public OBNode getStartObject() { return start_node; }

    @Override
    public void dumpAgentToGenerate(PrintWriter pw) {
        pw.print("gen_node");

        pw.print("," + start_node.ID);
        pw.print("," + start_time);
        pw.print("," + duration);
        
        pw.print("," + generated);
        pw.print("," + total);

        pw.print("," + goal);
        pw.print("," + planned_route.size());
        for (Term checkpoint : planned_route) {
            pw.print("," + checkpoint);
        }
        pw.print("," + tags.size());
        for (String tag : tags) {
            pw.print("," + tag);
        }
        pw.println();
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
