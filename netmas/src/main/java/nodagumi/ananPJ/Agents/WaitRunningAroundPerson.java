// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.misc.RoutePlan;

import nodagumi.Itk.*;

public class WaitRunningAroundPerson extends RunningAroundPerson
    implements Serializable {
    private static final long serialVersionUID = -6498240875020862791L;

    protected boolean waiting = false;

    /**
     * Agent の詳細設定情報を格納しているもの
     */
    public Term config ;

    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public WaitRunningAroundPerson() {} ;

    public WaitRunningAroundPerson(int _id,
            double speed, double _confidence,
            double allowance, double time, Random _random) {
        init(_id, speed,  _confidence, allowance, time, _random) ;
    }

    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id,
            double speed, double _confidence,
            double allowance, double time, Random _random) {
        super.init(_id, speed, _confidence, allowance, time, _random);
    }

    public WaitRunningAroundPerson(int _id, Random _random) {
        init(_id, _random) ;
    }

    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id, Random _random) {
        super.init(_id, _random);
    }

    /**
     * Conf による初期化。
     * 継承しているクラスの設定のため。
     * @param conf json の連想配列形式を scan した Map
     */
    public void initByConf(Term conf) {
        if(conf != null) {
            config = conf ;
        } else {
            config = new Term() ;
        }
    } ;
    /**
     * Conf による初期化。(obsolete)
     * 継承しているクラスの設定のため。
     * @param confString json で書かれたAgentのconfigulation。
     */
    public void _initByConf(String confString) {
        Term conf = Term.newByJson(confString) ;
        initByConf(conf) ;
    }

    /**
     * 複製操作のメイン
     */
    @Override
    public EvacuationAgent copyAndInitializeBody(EvacuationAgent _r) {
        WaitRunningAroundPerson r = (WaitRunningAroundPerson)_r ;
        super.copyAndInitializeBody(r) ;

        return r;
    }

    //----------------------------------------------------------------------
    /**
     * WAITING の処理
     * @return もしここで preUpdate から return するなら true。
     */
    protected boolean doWait(Term how, double time) {
        if (how.equals("SCATTER")) {
            waiting = true;
            scatter(time);
            return true;
        } else if (how.equals("PACK")) {
            waiting = true;
            pack(time);
            return true;
        } else {
            System.err.println("WARNING: how to move not stated!");
            return false ;
        }
    }


    protected boolean scatter(double time) {
        final ArrayList<EvacuationAgent> agents = current_link.getAgents();
        int index = Collections.binarySearch(agents, this) ;
        /* [2015.01.07 I.Noda] bug!!!
         * 本来、index が負になる（agents に存在しない）ことは
         * 生じないはずだが、sort_order を front_first にすると、なぜか起きる。
         * [解決] 上記の Comparator を指定することで、問題解消。
         * これまで、なぜ動いていたのかは不明。
         */
        if(index < 0) {
            Itk.dbgWrn("the agent can not found in the agent queue") ;
            Itk.dbgVal("agent", this) ;
            Itk.dbgVal("queue", agents) ;
            index = 0 ;
        }

        /* [2015.01.07 I.Noda] bug!!!
         * ここでは、なぜか、エージェントの進行方向を無視して計算している。
         * しかし、本来、無視してはいけないはず。
         */
        double front_space = current_link.length - position;
        double rear_space = position;
        int width = (int)current_link.width;

        int rear_agent_i = index - width;
        if (rear_agent_i > 0) {
            rear_space = position - agents.get(rear_agent_i).position;
        }
        int front_agent_i = index + width;
        if (front_agent_i < agents.size()) {
            front_space = agents.get(front_agent_i).position - position;
        }

        calc_speed(time);
        double d = (front_space - rear_space) * 0.3; 
        if (Math.abs(d) > Math.abs(speed)) {
            d = Math.signum(d) * Math.abs(speed);
        }
        return move_set(d, time, false);
    }
    
    protected boolean pack(double time) {
        calc_speed(time);

        double d = speed * direction;
        return move_set(d, time, false);
    }

    static final double NOT_WAITING = -100.0;
    private double wait_time = NOT_WAITING;
    private double wait_time_start = NOT_WAITING;

    //------------------------------------------------------------
    @Override
    public void preUpdate(double time) {
        waiting = false;
        //生成前かroutePlanがすでに空なら super へ。
        if (time <= generatedTime || routePlan.isEmpty()) {
            super.preUpdate(time);
            return;
        }

        // WAIT directive かどうかのチェック。
        Term tag = routePlan.top() ;
        WaitDirective.Type waitType = WaitDirective.isDirective(tag) ;
        if(waitType != null) {
            Term target = tag.getArgTerm("target") ;
            Term how = tag.getArgTerm("how") ;
            Term until = tag.getArgTerm("until") ;

            try {
                switch(waitType) {
                case WAIT_UNTIL:
                    if(doWaitUntil(target, how, until, time)) return ;
                    break ;
                case WAIT_FOR:
                    if(doWaitFor(target, how, until, time)) return ;
                    break ;
                }
            } catch (Exception ex) {
                ex.printStackTrace() ;
            }
        }
        super.preUpdate(time);
    }

    //------------------------------------------------------------
    /**
     * WAIT_UNTIL の場合の preUpdate 処理
     * @return 処理が終わればすぐに return すべきかどうか。
     *         (true なら super のpreUpdateを呼ばない)
     */
    protected boolean doWaitUntil(Term target, Term how, Term until, double time) {
        if (current_link.hasTag(target)) {
            // until は、待っているリンクで生じるイベント
            // until イベントが生じていたら、WAIT解除
            if (current_link.hasTag(until.getString())) {
                routePlan.shift() ;
                return false ;
            } else {
                return doWait(how, time) ;
            }
        } else {
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * WAIT_FOR の場合の preUpdate 処理
     * @return 処理が終わればすぐに return すべきかどうか。
     *         (super のpreUpdateがいらないかどうか)
     */
    protected boolean doWaitFor(Term target, Term how, Term until, double time) {
        // until は待つ時間(?)
        if (current_link.hasTag(target)) {
            // 待ち始めた時刻と待ち時間を記録(初回のみ)
            if (wait_time == NOT_WAITING) {
                wait_time = until.getDouble() ;
                wait_time_start = time;
            }

            // wait_time (untilに記録) 時間過ぎたらWAIT解除
            if (time - wait_time_start > wait_time) {
                wait_time = NOT_WAITING;
                routePlan.shift() ;
            } else {
                boolean returnP = doWait(how, time) ;
                if(returnP) return true;
            }
        } else if (wait_time != NOT_WAITING) {
            System.err.println("WARNING: agent " + agentNumber
                               + "pushed out from " + target);
            wait_time = NOT_WAITING;
            routePlan.shift() ;
        }
        return false ;
    }

    @Override
    public boolean update(double time) {
        return super.update(time);
    }

    /* look up our route plan and give our next goal
     * [2014.12.30 I.Noda] analysis
     * 次に来る、hint に記載されている route target を取り出す。
     * 今、top の target が現在のノードのタグにある場合、
     * route は１つ進める。(ここまでは RunningAroundPerson の機能)
     * ただし、WaitDirective の場合は、routePlan のポインタは
     * WaitDirective に留め、返すタグは、hint に存在する次の
     * 通常のタグ。(山下さんの説明)
     * ただし、下記のアルゴリズムにはおそらくバグがある。
     */
    @Override
    protected Term calcNextTarget(MapNode node) {
        if (on_node &&
            !routePlan.isEmpty() &&
            node.hasTag(routePlan.top())){
            routePlan.shift() ;
        }
        int next_check_point_index = routePlan.getIndex() ;
        while (next_check_point_index < routePlan.totalLength()) {
            Term candidate = routePlan.getRoute().get(next_check_point_index) ;
            /* [2014.12.27 I.Noda]
             * 読み込み時点で、directive はすでに1つのタグに集約されているはず。
             * (in "AgentGenerationFile.java")
             */
            WaitDirective.Type waitType = WaitDirective.isDirective(candidate) ;
            if (waitType != null) {
                routePlan.setIndex(next_check_point_index);
                next_check_point_index++ ;
            } else if (node.hasTag(candidate)){
                /* [2014.12.29 I.Noda] question
                 * ここのアルゴリズム、正しいのか？
                 * WAIT の場合は、そのWAITが書かれているところを
                 * setRouteIndex している。
                 * そうでなければ、今みている次を setRouteIndex している。
                 */
                next_check_point_index++;
                routePlan.setIndex(next_check_point_index);
            } else if (node.getHint(candidate) != null) {
                return candidate;
            } else {
                System.err.println("no mid-goal set for " + candidate);
                next_check_point_index++;
                routePlan.setIndex(next_check_point_index);
            }
        }

        return goal;
    }

    public ArrayList<Term> getPlannedRoute() {
        ArrayList<Term> goal_tags = new ArrayList<Term>();

        int delta = 0;
        while (delta < routePlan.length()) {
            Term candidate = routePlan.top(delta) ;
            /* [2014.12.27 I.Noda]
             * 読み込み時点で、directive はすでに1つのタグに集約されているはず。
             * (in "AgentGenerationFile.java")
             */
            WaitDirective.Type type = 
                WaitDirective.isDirective(candidate) ;
            if(type != null) {
                goal_tags.add(candidate.getArgTerm("target")) ;
            } else {
                goal_tags.add(candidate);
            }
            delta++ ;
        }
        return goal_tags;
    }

    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "WaitRunningAroundPerson" ;
    public static String getTypeName() {
        return typeString ;
    }

	//------------------------------------------------------------
	/**
	 * 知っている directive かどうかのチェック
	 */
	public boolean isKnownDirective(Term term) {
        WaitDirective.Type type =
            WaitDirective.isDirective(term) ;
        if(type != null)
            return true ;
        else
            return super.isKnownDirective(term) ;
    }

	//------------------------------------------------------------
	/**
	 * directive の中から経由地点tagを取り出し
	 * @return pushした数
	 */
	public int pushPlaceTagInDirective(Term directive,
                                       ArrayList<Term> goalList) {
        WaitDirective.Type type =
            WaitDirective.isDirective(directive) ;
        if(type != null) {
            Term _goal = directive.getArgTerm("target") ;
            if(_goal != null) {
                goalList.add(_goal) ;
                return 1 ;
            } else {
                Itk.dbgErr("target arg is missing in the directive:") ;
                Itk.dbgMsg("directive",directive) ;
                return 0 ;
            }
        } else {
            return super.pushPlaceTagInDirective(directive, goalList) ;
        }
	}

    //============================================================
    //============================================================
    //============================================================
    /**
     * WAIT directive 解釈
     */
    static public class WaitDirective {
        //==============================
        //::::::::::::::::::::::::::::::
        /**
         * enum for WAIT directive
         */
        static public enum Type {
            WAIT_FOR,
            WAIT_UNTIL
        }
        //::::::::::::::::::::::::::::::
        /**
         * Lexicon for WAIT directive
         */
        static public Lexicon lexicon = new Lexicon() ;
        static {
            // Rule で定義された名前をそのまま文字列で Lexicon を
            // 引けるようにする。
            // 例えば、 WaitDirective.WAIT_FOR は、"WAIT_FOR" で引けるようになる。
            lexicon.registerEnum(Type.class) ;
        }

        //==============================
        //------------------------------
        static public Type isDirective(Term term) {
            return (Type)lexicon.lookUp(term.getHeadString()) ;
        }
        //==============================
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * route 中の WAIT 命令の解釈パターン
         */
        static public Pattern FullPattern =
            Pattern.compile("(\\w+)\\((\\w+),(\\w+),(\\w+)\\)") ;
        static public Pattern HeadPattern =
            Pattern.compile("(\\w+)\\((\\w+)") ;
        static public Pattern TailPattern =
            Pattern.compile("(\\w+)\\)") ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * route 中の WAIT 命令の解釈パターン
         */
        public Type type ;
        public String head ;
        public String target ; // arg1
        public String how ;    // arg2
        public String untilStr ; // arg3

        //------------------------------
        public WaitDirective(Type _type, String _head, String _target, 
                             String _how, String _untilStr) {
            type = _type ;
            head = _head ;
            target = _target ;
            how = _how ;
            untilStr = _untilStr ;
        }

        //------------------------------
        public Term targetTerm() {
            return new Term(target) ;
        }

        //------------------------------
        public Term toTerm() {
            Term term = new Term(head) ;
            term.setArg("target", new Term(target)) ;
            term.setArg("how", new Term(how)) ;
            switch(type) {
            case WAIT_UNTIL:
                term.setArg("until", new Term(untilStr)) ;
                break ;
            case WAIT_FOR:
                term.setArg("until", new Term(Double.parseDouble(untilStr))) ;
                break ;
            }

            return term ;
        }

        //==============================
        //------------------------------
        /**
         * atom String 状態の wait directive を解析する。
         *   _directive_ ::= _head_(_target_,_how_,_until_)
         *   _head_      ::= "WAIT_UNTIL" | "WAIT_FOR"
         *   _how_       ::= ???
         *   _until_     ::= _eventTag_ | _duration_
         */
        static public WaitDirective scanDirective(Term directive) {
            return scanDirective(directive.getString()) ;
        }
        //==============================
        //------------------------------
        static public WaitDirective scanDirective(String directive) {
            Matcher matchFull = FullPattern.matcher(directive) ;
            if(! matchFull.matches()) {
                return null ;
            }

            String head = matchFull.group(1) ;
            Type waitType = (Type)lexicon.lookUp(head) ;
            if(waitType == null) {
                return null ;
            }

            WaitDirective _directive = 
                new WaitDirective(waitType, head, matchFull.group(2),
                                  matchFull.group(3), matchFull.group(4)) ;

            return _directive ;
        }
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
