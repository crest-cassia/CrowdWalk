// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.Place;
import nodagumi.ananPJ.misc.RoutePlan;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;

//======================================================================
/**
 * WAIT directive を解するエージェント
 *
 * <h3> config, fallbackResources に書ける設定 </h3>
 * {@link WalkAgent} に加えて、
 * <pre>
 *  {
 *    "minDistanceBetweenAgents" : __double__ // pack する際の最小距離(?)
 * }
 * </pre>
 */
public class AwaitAgent extends WalkAgent {

    //============================================================
    //------------------------------------------------------------
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "AwaitAgent" ;
    public static String getTypeName() {
        return typeString ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント同士の最小の距離。
     * scatter での適用範囲を定めるのに使用。
     */
    protected static final double Fallback_minDistanceBetweenAgents = 0.3;
    protected double minDistanceBetweenAgents
        = Fallback_minDistanceBetweenAgents ;

    protected boolean waiting = false;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public AwaitAgent() {} ;

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        minDistanceBetweenAgents =
            getDoubleFromConfig("minDistanceBetweenAgents",
                                minDistanceBetweenAgents) ;
    } ;

    //----------------------------------------------------------------------
    /**
     * WAITING の処理
     * @return もしここで preUpdate から return するなら true。
     */
    protected boolean doWait(Term how, SimTime currentTime) {
        if (how.equals("SCATTER")) {
            waiting = true;
            scatter(currentTime);
            return true;
        } else if (how.equals("PACK")) {
            waiting = true;
            pack(currentTime);
            return true;
        } else {
            Itk.logWarn("how to move not stated!", this);
            return false ;
        }
    }

    /**
     * WAIT_FOR/WAIT_UNTIL 処理中か?
     */
    public boolean isWaiting() {
        return waiting;
    }

    //----------------------------------------------------------------------
    /**
     * WAIT_FOR/WAIT_UNTIL 中のエージェントをレーンごとに均等な間隔で配置する。
     * ※制限事項
     * ・同じレーン上に scatter 以外のエージェントが存在すると間隔が詰まり均等にならなくなる。
     * ・対向流が発生すると laneWidth が変化するため均等な間隔にならない事がある。
     */
    protected boolean scatter(SimTime currentTime) {
        ArrayList<AgentBase> agents = currentPlace.getLane();
        int laneWidth = currentPlace.getLaneWidth();
        double space = currentPlace.getLinkLength() / ((agents.size() - 1) / laneWidth + 2);

        // scatter メソッドが適用できないほど超過密状態の場合には pack を使用する
        if (space <= minDistanceBetweenAgents) {
            return pack(currentTime);
        }

        int index = currentPlace.getIndexFromHeadingInLane(this) ;
        double stopPosition = currentPlace.getLinkLength() - space * ((index / laneWidth) + 1);
        double d = stopPosition - getAdvancingDistance();

        // エージェントが停止予定位置より先にいた場合はその位置に留まらせる
        if (d < 0.0) d = 0.0; 

        // d が speed より小さい時、d に基づきスピードを決める。 
        double previousSpeed = speed ;
        speed = calcSpeed(speed, currentTime);
        if (d < speed * currentTime.getTickUnit()) {
            speed = d / currentTime.getTickUnit() ;
        }
        checkSpeedChange(previousSpeed, speed) ;

        return advanceNextPlace(speed, currentTime, true) ;
    }
    
    //----------------------------------------------------------------------
    /**
     * できるだけ過密に並ぶ
     */
    protected boolean pack(SimTime currentTime) {
        double previousSpeed = speed;
        speed = calcSpeed(speed, currentTime);

        // currentLink 内に収まるスピードにする
        double deltaDistance = speed * currentTime.getTickUnit() ;
        if (currentPlace.isBeyondLinkWithAdvance(deltaDistance)) {
            double distance = currentPlace.getLinkLength() * Place.AlmostOne
                - currentPlace.getAdvancingDistance();
            speed = distance / currentTime.getTickUnit();
        }

        checkSpeedChange(previousSpeed, speed);

        return advanceNextPlace(speed, currentTime, true) ;
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
    public void preUpdate(SimTime currentTime) {
        waiting = false;
        if (currentTime.isBeforeOrAt(generatedTime) || routePlan.isEmpty()) {
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
                        if(doWaitUntil(target, how, until, currentTime)) return ;
                        break ;
                    case WAIT_FOR:
                        if(doWaitFor(target, how, until, currentTime)) return ;
                        break ;
                    }
                } catch (Exception ex) {
                    Itk.dumpStackTraceOf(ex) ;
                }
            }
        }
        super.preUpdate(currentTime);
    }

    //------------------------------------------------------------
    /**
     * WAIT_UNTIL の場合の preUpdate 処理
     * @return 処理が終わればすぐに return すべきかどうか。
     *         (true なら super のpreUpdateを呼ばない)
     */
    protected boolean doWaitUntil(Term target, Term how, Term until,
                                  SimTime currentTime) {
        if (currentPlace.getLink().hasTag(target)) {
            // until は、待っているリンクで生じるイベント
            // until イベントが生じていたら、WAIT解除
            if (currentPlace.getLink().hasTag(until.getString())) {
                routePlan.shift() ;
                return false ;
            } else {
                return doWait(how, currentTime) ;
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
    protected boolean doWaitFor(Term target, Term how, Term until,
                                SimTime currentTime) {
        // until は待つ時間(?)
        if (currentPlace.getLink().hasTag(target)) {
            // 待ち始めた時刻と待ち時間を記録(初回のみ)
            if (wait_time == NOT_WAITING) {
                wait_time = until.getDouble() ;
                wait_time_start = currentTime.getRelativeTime() ;
            }

            // wait_time (untilに記録) 時間過ぎたらWAIT解除
            if (currentTime.getRelativeTime() - wait_time_start > wait_time) {
                wait_time = NOT_WAITING;
                routePlan.shift() ;
            } else {
                boolean returnP = doWait(how, currentTime) ;
                if(returnP) return true;
            }
        } else if (wait_time != NOT_WAITING) {
            Itk.logWarn("Pushed Out", "agent " + ID + " pushed out from " + target);
            wait_time = NOT_WAITING;
            routePlan.shift() ;
        }
        return false ;
    }

    //------------------------------------------------------------
    /**
     */
    @Override
    public boolean update(SimTime currentTime) {
        return super.update(currentTime);
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
            return new Term(target, false) ;
        }

        //------------------------------
        private static final String Lex_target = Itk.intern("target") ;
        private static final String Lex_how = Itk.intern("how") ;
        private static final String Lex_until = Itk.intern("until") ;
        
        //------------------------------
        public Term toTerm() {
            Term term = new Term(head, false) ;
            term.setArg(Lex_target, new Term(target, false)) ;
            term.setArg(Lex_how, new Term(how, false)) ;
            switch(type) {
            case WAIT_UNTIL:
                term.setArg(Lex_until, new Term(untilStr, false)) ;
                break ;
            case WAIT_FOR:
                term.setArg(Lex_until,
                            new Term(Double.parseDouble(untilStr))) ;
                break ;
            }

            return term ;
        }

        //==============================
        //------------------------------
        /**
         * atom String 状態の wait directive を解析する。
         * <pre>
         *   _directive_ ::= _head_(_target_,_how_,_until_)
         *   _head_      ::= "WAIT_UNTIL" | "WAIT_FOR"
         *   _how_       ::= ???
         *   _until_     ::= _eventTag_ | _duration_
         * </pre>
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

            String head = Itk.intern(matchFull.group(1)) ;
            Type waitType = (Type)lexicon.lookUp(head) ;
            if(waitType == null) {
                return null ;
            }

            WaitDirective _directive = 
                new WaitDirective(waitType, head,
                                  Itk.intern(matchFull.group(2)),
                                  Itk.intern(matchFull.group(3)),
                                  Itk.intern(matchFull.group(4))) ;

            return _directive ;
        }
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
