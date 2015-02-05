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

    //============================================================
    //------------------------------------------------------------
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "WaitRunningAroundPerson" ;
    public static String getTypeName() {
        return typeString ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント同士の最小の距離。
     * scatter での適用範囲を定めるのに使用。
     */
    protected static final double FallBack_minDistanceBetweenAgents = 0.3;
    protected double minDistanceBetweenAgents
        = FallBack_minDistanceBetweenAgents ;

    protected boolean waiting = false;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public WaitRunningAroundPerson() {} ;

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public WaitRunningAroundPerson(int _id,
            double speed, double _confidence,
            double allowance, double time, Random _random) {
        init(_id, speed,  _confidence, allowance, time, _random) ;
    }

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public WaitRunningAroundPerson(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id,
            double speed, double _confidence,
            double allowance, double time, Random _random) {
        super.init(_id, speed, _confidence, allowance, time, _random);
    }

    //------------------------------------------------------------
    /**
     * 初期化。constractorから分離。
     */
    @Override
    public void init(int _id, Random _random) {
        super.init(_id, _random);
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf) {
        super.initByConf(conf) ;

        minDistanceBetweenAgents =
            getDoubleFromConfig("minDistanceBetweenAgents",
                                minDistanceBetweenAgents) ;
    } ;

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    @Override
    public AgentBase copyAndInitializeBody(AgentBase _r) {
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

    //----------------------------------------------------------------------
    /**
     * WAIT_FOR/WAIT_UNTIL 中のエージェントをレーンごとに均等な間隔で配置する。
     * ※制限事項
     * ・同じレーン上に scatter 以外のエージェントが存在すると間隔が詰まり均等にならなくなる。
     * ・対向流が発生すると laneWidth が変化するため均等な間隔にならない事がある。
     */
    protected boolean scatter(double time) {
        ArrayList<AgentBase> agents = currentPlace.getLane();
        int laneWidth = currentPlace.getLaneWidth();
        double space = currentPlace.getLinkLength() / ((agents.size() - 1) / laneWidth + 2);

        // scatter メソッドが適用できないほど超過密状態の場合には pack を使用する
        if (space <= minDistanceBetweenAgents) {
            return pack(time);
        }

        int index = currentPlace.getIndexFromHeadingInLane(this) ;
        double stopPosition = currentPlace.getLinkLength() - space * ((index / laneWidth) + 1);
        double d = stopPosition - getAdvancingDistance();
        // エージェントが停止予定位置より先にいた場合はその位置に留まらせる
        if (d < 0.0) {
            d = 0.0;
        }
        calc_speed(time);
        if (d > speed) {
            d = speed;
        }
        // scatter 制御を破綻させないため move_set() 内の d * time_scale を無効化する
        return move_set(d / time_scale, time, false);
    }
    
    //----------------------------------------------------------------------
    /**
     * できるだけ過密に並ぶ
     */
    protected boolean pack(double time) {
        calc_speed(time);

        return move_set(speed, time, false);
    }

    static final double NOT_WAITING = -100.0;
    private double wait_time = NOT_WAITING;
    private double wait_time_start = NOT_WAITING;

    //------------------------------------------------------------
    /**
     * cycle 前処理。
     * Wait directive の処理を呼び出す。
     */
    @Override
    public void preUpdate(double time) {
        waiting = false;
        if (time <= generatedTime || routePlan.isEmpty()) {
            //生成前かroutePlanがすでに空なら
            // do nothing
        } else {
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
        if (currentPlace.getLink().hasTag(target)) {
            // until は、待っているリンクで生じるイベント
            // until イベントが生じていたら、WAIT解除
            if (currentPlace.getLink().hasTag(until.getString())) {
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
        if (currentPlace.getLink().hasTag(target)) {
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

    //------------------------------------------------------------
    /**
     */
    @Override
    public boolean update(double time) {
        return super.update(time);
    }

    //------------------------------------------------------------
    /**
     */
    @Override
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

	//------------------------------------------------------------
	/**
	 * 知っている directive かどうかのチェック
	 */
    @Override
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
     * Directive のなかの代表的目的地の取得
     * @param directive : 調べる directive。通常の place tag の場合もある。
     *    もし directive が isKnownDirective() なら、なにか返すべき。
     * @return もし directive なら代表的目的地。そうでないなら null
     */
    @Override
    public Term getPrimalTargetPlaceInDirective(Term directive) {
        WaitDirective.Type type =
            WaitDirective.isDirective(directive) ;
        if(type != null) {
            return directive.getArgTerm("target") ;
        } else {
            return null ;
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
