// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.lang.reflect.Method;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Agents.WaitRunningAroundPerson;
import nodagumi.ananPJ.Agents.*;

import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Simulator.EvacuationModelBase;

import nodagumi.Itk.*;


public abstract class GenerateAgent implements Serializable {
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
        registerAgentClass(RunningAroundPerson.class) ;
        registerAgentClass(WaitRunningAroundPerson.class) ;
        registerAgentClass(NaiveAgent.class) ;
        registerAgentClass(CapriciousAgent.class) ;
        registerAgentClass(BustleAgent.class) ;
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
            Itk.dbgErr("cannot process registerAgentClass()") ;
            Itk.dbgMsg("agentClass",agentClass) ;
        }
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * エージェントクラスのインスタンス生成（ゼロ引数で）
     */
    static public EvacuationAgent newAgentByName(String agentClassName) {
        try {
            EvacuationAgent agent =
                (EvacuationAgent)classFinder.newByName(agentClassName) ;
            return agent ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.dbgErr("can not find the class") ;
            Itk.dbgMsg("agentClassName", agentClassName) ;
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
         * エージェント設定情報 (JSON 文字列) (obsolete)
         */
        //public String agentConfString = null ;

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
         * 設定文字列（変換前の設定情報）
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
    }

    public Term goal;
    List<Term> planned_route;
    double start_time, duration;
    int total;
    SpeedCalculationModel speed_model = null;
    Random random = null;
    List<String> tags = new ArrayList<String>(); 
    public String configLine;
    public boolean enabled = true;

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

    /**
     *  Config によるコンストラクタ
     */
    public GenerateAgent(Config config, Random _random) {
        this(config.agentClassName,
             config.agentConf,
             config.conditions,
             config.goal,
             config.plannedRoute,
             config.startTime,
             config.duration,
             config.total,
             config.speedModel,
             _random,
             config.originalInfo) ;
    }

    public GenerateAgent(String _agentClassName,
                         Term _agentConf,
            String[] conditions,
            Term _goal,
            List<Term> _planned_route,
            double _start_time,
            double _duration,
            int _total,
            SpeedCalculationModel _speed_model,
            Random _random,
            String _configLine) {
        if(_agentClassName != null && _agentClassName.length() > 0) {
            agentClassName = _agentClassName ;
            agentConf = _agentConf ;
        }
        //Itk.dbgMsg("agentClassName", agentClassName) ;

        goal = _goal;
        planned_route = _planned_route;
        start_time = _start_time;
        duration = _duration;
        total = _total;
        speed_model = _speed_model;
        random = _random;
        configLine = _configLine;

        parse_conditions(conditions);
    }

    protected void parse_conditions(String[] conditions) {
        if (conditions == null) return;
        for (int i = 0; i < conditions.length; i++) {
            final String condition = conditions[i];
            if (condition.startsWith("COLOR=")) {
                tags.add(condition.substring(6));
            } else {
                tags.add(condition);
            }
        }
    }

    protected boolean finished(double time) {
        return time > start_time + duration && generated >= total;
    }

    abstract protected void place_agent(WaitRunningAroundPerson agent);

    /* [2014.12.24 I.Noda] should fixed
     * 以下のアルゴリズム、正確に正しく total のエージェントを生成しない
     * 可能性がある。
     * 予め total 個の乱数時刻を発生させ、それをソートしておき、
     * 指定した時間までの分を生成するようにすべき。
     */
    int generated = 0;
    /* TODO must wait finish until generate &
     * must control here with scenario */
    public void tryUpdateAndGenerate(double time,
            double timeScale,
            double tick,
            EvacuationModelBase model,
            List<EvacuationAgent> agents) {
        if (!enabled) return;

        if (finished(time)) {
            enabled = false;
            return;
        }

        if (time < start_time) {
            /* not yet time to start generating agents */
            return;
        }

        double duration_left = start_time + duration - time;
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

        /* [I.Noda] ここで Agent 生成? */
        for (int i = 0; i < agent_to_gen; ++i) {
            generated++;
            /*
            WaitRunningAroundPerson agent = new WaitRunningAroundPerson(model.getMap().assignUniqueAgentId(),
                    random);
            */
            /* [2014.12.24 I.Noda] should fix
             * 以下は、WaitRunningAroundPerson の代わりに、
             * NaiveAgent にしたい。
             */
            WaitRunningAroundPerson agent = null;
            try {
                agent = (WaitRunningAroundPerson)newAgentByName(agentClassName) ;
                agent.init(model.getMap().assignUniqueAgentId(), random);
                if(agentConf != null)
                    agent.initByConf(agentConf) ;
            } catch (Exception ex ) {
                Itk.dbgErr("class name not found") ;
                Itk.dbgErr("agentClassName", agentClassName) ;
                ex.printStackTrace();
                System.exit(1) ;
            }

            agent.generatedTime = tick;
            agent.displayMode = model.getDisplayMode();
            ((RunningAroundPerson) agent).setSpeedCalculationModel(
                speed_model);
            agent.setConfigLine(configLine);
            agents.add(agent);
            agent.setGoal(new Term(goal));
            Term planned_route_in_Term = 
                new Term(new ArrayList<Term>(planned_route)) ;
            agent.setPlannedRoute((List)planned_route_in_Term.getArray());

            place_agent(agent); // この時点では direction が 0.0 のため、add_agent_to_lane で agent は登録されない

            for (final String tag : tags) {
                agent.addTag(tag);
            }

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
            Itk.dbgErr("can not check the directive") ;
            Itk.dbgMsg("directive", directive) ;
            Itk.dbgMsg("agentClass", className) ;
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
            Itk.dbgErr("can not pushPlaceTag.") ;
            Itk.dbgMsg("directive", directive) ;
            Itk.dbgMsg("agentClass", className) ;
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

    /**
     * Config によるコンストラクタ
     */
    public GenerateAgentFromLink(Config config, Random random) {
        super(config, random) ;
        start_link = (MapLink)config.startPlace ;
    }


    public GenerateAgentFromLink(String _agentClassName,
                                 Term _agentConf,
            MapLink _start_link,
            String[] conditions,
            Term _goal,
            List<Term> _planned_route,
            double _start_time,
            double _duration,
            int _total,
            SpeedCalculationModel _speed_model,
            Random _random,
            String _configLine) {
        super(_agentClassName,
              _agentConf,
              conditions, _goal, _planned_route, _start_time, _duration,
                _total, _speed_model, _random, _configLine);
        start_link = _start_link;
    }

    @Override
    protected boolean finished(double time) {
        if (super.finished(time) || start_link.getStop()) return true;
        return false;
    }

    @Override
    protected void place_agent(WaitRunningAroundPerson agent) {
        double position = random.nextDouble() * start_link.length;
        agent.place(start_link, position);
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

    /**
     * Config によるコンストラクタ
     */
    public GenerateAgentFromNode(Config config, Random random) {
        super(config, random) ;
        start_node = (MapNode)config.startPlace ;
    }

    public GenerateAgentFromNode(String _agentClassName,
                                 Term _agentConf,
            MapNode _start_node,
            String[] conditions,
            Term _goal,
            List<Term> _planned_route,
            double _start_time,
            double _duration,
            int _total,
            SpeedCalculationModel _speed_model,
            Random _random,
            String _configLine) {
        super(_agentClassName,
              _agentConf,
              conditions, _goal, _planned_route, _start_time, _duration,
                _total, _speed_model, _random, _configLine);
        start_node = _start_node;
        //System.err.println("GenerateAgentFromNode start_node: " + start_node.ID + " goal: " + _goal);
    }

    /*  use finished of super class
    @Override
    protected boolean finished(double time) {
       (not implemented)
    }
     */

    @Override
    protected void place_agent(WaitRunningAroundPerson agent) {
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
        double position = 0.0;
        if (link.getTo() == start_node) position = link.length;
        agent.place(link, position);
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
