// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Formula, Agent related functions
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/04/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/04/15]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents.Think;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.lang.reflect.InvocationTargetException ;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.RationalAgent;
import nodagumi.ananPJ.Agents.Think.ThinkEngine;
import nodagumi.ananPJ.Agents.Think.ThinkFormula;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考を表す式の処理系 (agent functions)
 */
public class ThinkFormulaAgent extends ThinkFormula {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 代表インスタンス
     * (複数あっても無駄なので、単一である方が良い)
     * registerFormulas で登録。
     */
    static ThinkFormula singleton ;

    //============================================================
    //------------------------------------------------------------
    /**
     * 登録
     */
    static public boolean registerFormulas(Lexicon lexicon)
    {
        try {
            Class currentClass =
                new Object(){}.getClass().getEnclosingClass() ;
            singleton =
                (ThinkFormula)(currentClass.newInstance()) ;
        } catch(Exception ex) {
            Itk.logError("wrong class definition") ;
            Itk.dumpStackTraceOf(ex) ;
        }

        ThinkFormula.register("getFallback", 	singleton, false, lexicon) ;
        ThinkFormula.register("getParam", 	singleton, false, lexicon) ;
        ThinkFormula.register("setParam", 	singleton, false, lexicon) ;
        ThinkFormula.register("agentHasTag", 	singleton, false, lexicon) ;
        ThinkFormula.register("placeHasTag", 	singleton, false, lexicon) ;
        ThinkFormula.register("addAgentTag", 	singleton, false, lexicon) ;
        ThinkFormula.register("addPlaceTag", 	singleton, false, lexicon) ;
        ThinkFormula.register("removeAgentTag", singleton, false, lexicon) ;
        ThinkFormula.register("removePlaceTag", singleton, false, lexicon) ;
        ThinkFormula.register("listenAlert", 	singleton, false, lexicon) ;
        ThinkFormula.register("saveAlert", 	singleton, false, lexicon) ;
        ThinkFormula.register("announceAlert", 	singleton, false, lexicon) ;
        ThinkFormula.register("clearAlert", 	singleton, false, lexicon) ;
        ThinkFormula.register("clearAlertAll", 	singleton, false, lexicon) ;
        ThinkFormula.register("changeGoal", 	singleton, false, lexicon) ;
        ThinkFormula.register("clearPlannedRoute", singleton, false, lexicon) ;
        ThinkFormula.register("insertRoute", 	singleton, false, lexicon) ;
        ThinkFormula.register("turnAround", 	singleton, false, lexicon) ;
        ThinkFormula.register("clearNextLinkCache", singleton, false, lexicon) ;

        return true ;
    }

    //------------------------------------------------------------
    /**
     * 呼び出し.
     * 以下のフォーマット
     * <ul>
     *   <li>{@link #call_getFallback "getFallback"}</li>
     *   <li>{@link #call_getParam "getParam"}</li>
     *   <li>{@link #call_setParam "setParam"}</li>
     *   <li>{@link #call_agentHasTag "agentHasTag"}</li>
     *   <li>{@link #call_placeHasTag "placeHasTag"}</li>
     *   <li>{@link #call_addAgentTag "addAgentTag"}</li>
     *   <li>{@link #call_addPlaceTag "addPlaceTag"}</li>
     *   <li>{@link #call_removeAgentTag "removeAgentTag"}</li>
     *   <li>{@link #call_removePlaceTag "removePlaceTag"}</li>
     *   <li>{@link #call_listenAlert "listenAlert"}</li>
     *   <li>{@link #call_saveAlert "saveAlert"}</li>
     *   <li>{@link #call_announceAlert "announceAlert"}</li>
     *   <li>{@link #call_clearAlert "clearAlert"}</li>
     *   <li>{@link #call_clearAllAlert "clearAllAlert"}</li>
     *   <li>{@link #call_changeGoal "changeGoal"}</li>
     *   <li>{@link #call_clearPlannedRoute "clearPlannedRoute"}</li>
     *   <li>{@link #call_insertRoute "insertRoute"}</li>
     *   <li>{@link #call_turnAround "turnAround"}</li>
     * </ul>
     */
    @Override
    public Term call(String head, Term expr,
                     ThinkEngine engine, Object env) {
	if(head.equals("getFallback")) {
            return call_getFallback(head, expr, engine, env) ;
        } else if(head.equals("getParam")) {
            return call_getParam(head, expr, engine, env) ;
        } else if(head.equals("setParam")) {
            return call_setParam(head, expr, engine, env) ;
        } else if(head.equals("agentHasTag")) {
            return call_agentHasTag(head, expr, engine, env) ;
        } else if(head.equals("placeHasTag")) {
            return call_placeHasTag(head, expr, engine, env) ;
        } else if(head.equals("addAgentTag")) {
            return call_addAgentTag(head, expr, engine, env) ;
        } else if(head.equals("addPlaceTag")) {
            return call_addPlaceTag(head, expr, engine, env) ;
        } else if(head.equals("removeAgentTag")) {
            return call_removeAgentTag(head, expr, engine, env) ;
        } else if(head.equals("removePlaceTag")) {
            return call_removePlaceTag(head, expr, engine, env) ;
        } else if(head.equals("listenAlert")) {
            return call_listenAlert(head, expr, engine, env) ;
        } else if(head.equals("saveAlert")) {
            return call_saveAlert(head, expr, engine, env) ;
        } else if(head.equals("announceAlert")) {
            return call_announceAlert(head, expr, engine, env) ;
        } else if(head.equals("clearAlert")) {
            return call_clearAlert(head, expr, engine, env) ;
        } else if(head.equals("clearAllAlert")) {
            return call_clearAllAlert(head, expr, engine, env) ;
        } else if(head.equals("changeGoal")) {
            return call_changeGoal(head, expr, engine, env) ;
        } else if(head.equals("clearPlannedRoute")) {
            return call_clearPlannedRoute(head, expr, engine, env) ;
        } else if(head.equals("insertRoute")) {
            return call_insertRoute(head, expr, engine, env) ;
        } else if(head.equals("turnAround")) {
            return call_turnAround(head, expr, engine, env) ;
        } else if(head.equals("clearNextLinkCache")) {
            return call_clearNextLinkCache(head, expr, engine, env) ;
        } else {
            Itk.logWarn("unknown expression", "expr=", expr) ;
	    return Term_Null ;
	}
    }

    //------------------------------------------------------------
    /**
     * 推論(getFallback)。
     * <pre>
     * { "" : "getFallback",
     *   "name" : _nameOfValue_,
     * }
     *  _nameOfValue_ ::= any Fallback Parameters
     * </pre>
     */
    public Term call_getFallback(String head, Term expr,
                                 ThinkEngine engine, Object env) {
        String name = expr.getArgString("name") ;
        RationalAgent agent = engine.getRationalAgent() ;

        return agent.getTermFromConfig(name, Term_Null) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(getParam)。
     * <pre>
     * { "" : "getParam",
     *   "name" : _nameOfValue_,
     * }
     *  _nameOfValue_ ::= "currentTime" | "agentId" | "linkId" 
     *                  | "speed" | "emptySpeed" | "goal" 
     *                  | "triageLevelInt" | "triageLevelName" 
     *                  | "mentalMode"
     * </pre>
     */
    public Term call_getParam(String head, Term expr,
                              ThinkEngine engine, Object env) {
        String name = expr.getArgString("name") ;
        RationalAgent agent = engine.getRationalAgent() ;

        if(name.equals("currentTime")) {
            return new Term(agent.currentTime.getRelativeTime()) ;
        } else if(name.equals("agentId")) {
            return new Term(agent.ID, false) ;
        } else if(name.equals("linkId")) {
	    return new Term(agent.getCurrentLink().ID, false) ;
        } else if(name.equals("speed")) {
	    return new Term(agent.getSpeed()) ;
        } else if(name.equals("emptySpeed")) {
	    return new Term(agent.getEmptySpeed()) ;
        } else if(name.equals("goal")) {
	    return agent.getGoal() ;
        } else if(name.equals("triageLevelInt")) {
            return new Term(agent.getTriageInt()) ;
        } else if(name.equals("triageLevelName")) {
            return new Term(agent.getTriageName(), false) ;
        } else if(name.equals("mentalMode")) {
            return agent.mentalMode ;
        } else {
            Itk.logError("unknown parameter name for getParam.", "name=",name) ;
            return Term_Null ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(setParam)。
     * <pre>
     * { "" : "setParam",
     *   "name" : _nameOfValue_,
     *   "value" : _Term_,
     * }
     *  _nameOfValue_ ::= "emptySpeed" 
     *                  | "mentalMode"
     * </pre>
     */
    public Term call_setParam(String head, Term expr,
                              ThinkEngine engine, Object env) {
        String name = expr.getArgString("name") ;
        Term value = engine.think(expr.getArgTerm("value"), env) ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;

        if(name.equals("emptySpeed")) {
            agent.setEmptySpeed(value.getDouble()) ;
            return value ;
        } else if(name.equals("mentalMode")) {
            agent.setMentalMode(value) ;
            return value ;
        } else {
            Itk.logError("unknown parameter name for getParam.", "name=",name) ;
            return Term_Null ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(agentHasTag)。
     * エージェント自身があるタグを持っているか？
     * <pre>
     * { "" : "agentHasTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term call_agentHasTag(String head, Term expr,
                                 ThinkEngine engine, Object env) {
        String tag = expr.getArgString("tag") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;
        
        if(agent.hasTag(tag)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(placeHasTag)。
     * 現在いるリンクがあるタグを持っているか？
     * <pre>
     * { "" : "placeHasTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term call_placeHasTag(String head, Term expr,
                                 ThinkEngine engine, Object env) {
        String tag = expr.getArgString("tag") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;
        
        if(agent.getCurrentLink().hasTag(tag)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(addAgentTag)。
     * エージェント自身にあるタグを追加。
     * <pre>
     * { "" : "addAgentTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term call_addAgentTag(String head, Term expr,
                                 ThinkEngine engine, Object env) {
        String tag = expr.getArgString("tag") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;
        
        if(agent.addTag(tag)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(addPlaceTag)。
     * 現在いるリンクにあるタグを追加。
     * <pre>
     * { "" : "addPlaceTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term call_addPlaceTag(String head, Term expr,
                                 ThinkEngine engine, Object env) {
        String tag = expr.getArgString("tag") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;

        if(agent.getCurrentLink().addTag(tag)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(removeAgentTag)。
     * エージェント自身からあるタグを削除。
     * <pre>
     * { "" : "removeAgentTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term call_removeAgentTag(String head, Term expr,
                                    ThinkEngine engine, Object env) {
        String tag = expr.getArgString("tag") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;

        agent.removeTag(tag) ;
        return Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(removePlaceTag)。
     * 現在いるリンクからあるタグを削除。
     * <pre>
     * { "" : "removePlaceTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term call_removePlaceTag(String head, Term expr,
                                    ThinkEngine engine, Object env) {
        String tag = expr.getArgString("tag") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;

        agent.getCurrentLink().removeTag(tag) ;
        return Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(listenAlert)。
     * {@code message} という alert を聴いたかどうかのチェック。
     * <pre>
     * { "" : "listenAlert",
     *   "message" : _alertMessage_ }
     * _alertMessage_ ::= _String_
     * </pre>
     * {@code "message"} は、
     * {@link nodagumi.ananPJ.Scenario.Scenario Scenario} の中の
     * {@link nodagumi.ananPJ.Scenario.AlertEvent AlertEvent} 参照。
     */
    public Term call_listenAlert(String head, Term expr,
                                 ThinkEngine engine, Object env) {
        Term message = expr.getArgTerm("message") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;

        SimTime alertTime =
            ((ThinkEngine)env).getAlertedMessageTable().get(message) ;
        if(alertTime != null) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(saveAlert)。
     * 現時刻に、ある {@code message} を聴いたことにする。
     * <pre>
     * { "" : "saveAlert",
     *   "message" : _alertMessage_,
     *  ("redundant" : _Boolean_) }
     * _alertMessage_ ::= _String_
     * </pre>
     * "redundant" は、重複を許すかどうか。true だと重複の追加を許す。
     * デフォルトは false。
     *
     * @return Alert 追加に成功したら Term_True を返す。
     */
    public Term call_saveAlert(String head, Term expr,
                               ThinkEngine engine, Object env) {
        Term message = expr.getArgTerm("message") ;
        boolean redundant = expr.getArgBoolean("redundant") ;
        SimTime alertTime =
            ((ThinkEngine)env).getAlertedMessageTable().get(message) ;

         if(redundant || (alertTime == null)) {
             ((ThinkEngine)env)
             .getAlertedMessageTable()
             .put(message,
                  ((ThinkEngine)env).getAgent().currentTime) ;
            return ThinkFormula.Term_True ;
        } else {
            return ThinkFormula.Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(announceAlert)。
     * 現在地に Alert を追加（発表）する。
     * <pre>
     * { "" : "announceAlert",
     *   "message" : _alertMessage_,
     *  ("redundant" : _Boolean_) }
     * _alertMessage_ ::= _String_
     * </pre>
     * "redundant" は、重複を許すかどうか。true だと重複の追加を許す。
     * デフォルトは false。
     *
     * @return Alert 追加に成功したら Term_True を返す。
     */
    public Term call_announceAlert(String head, Term expr,
                                   ThinkEngine engine, Object env) {
        Term message = expr.getArgTerm("message") ;
        boolean redundant = expr.getArgBoolean("redundant") ;
        RationalAgent agent = ((ThinkEngine)env).getRationalAgent() ;

        MapLink currentLink = agent.getCurrentLink() ;
        SimTime alertTime = agent.currentTime ;

        currentLink.addAlertMessage(message, alertTime, true) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(clearAlert)。
     * 現在Agent自身が保持している {@code message} を消去する。
     * <pre>
     * { "" : "clearAlert",
     *   "message" : _alertMessage_ }
     * _alertMessage_ ::= _String_
     * </pre>
     */
    public Term call_clearAlert(String head, Term expr,
                                ThinkEngine engine, Object env) {
        Term message = expr.getArgTerm("message") ;
        ((ThinkEngine)env).getAlertedMessageTable().remove(message) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(clearAllAlert)。
     * 現在Agent自身が保持している全メッセージを消去する。
     * <pre>
     * { "" : "clearAllAlert" }
     * </pre>
     */
    public Term call_clearAllAlert(String head, Term expr,
                                   ThinkEngine engine, Object env) {
        ((ThinkEngine)env).getAlertedMessageTable().clear() ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(changeGoal)。
     * 最終ゴールを変更。
     * <pre>
     * { "" : "changeGoal",
     *   "goal" : _goalTag_ }
     * _goalTag_ ::= _String_
     * </pre>
     */
    public Term call_changeGoal(String head, Term expr,
                                ThinkEngine engine, Object env) {
        Term goalTag = expr.getArgTerm("goal") ;
        ((ThinkEngine)env).getAgent().changeGoal(goalTag) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(clear PlannedRoute)。
     * 現在のルートを全てクリア。
     * <pre>
     * { "" : "clearPlannedRoute" }
     * </pre>
     */
    public Term call_clearPlannedRoute(String head, Term expr,
                                       ThinkEngine engine, Object env) {
        ((ThinkEngine)env).getAgent().setPlannedRoute(new ArrayList<Term>(), true) ;
        return Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(insert route)。
     * これからのルートの先頭にサブゴールを追加。
     * <pre>
     * { "" : "insertRoute",
     *   "route" : _route_ }
     * _route_ ::= _tag_ || [_tag_, _tag_, ...]
     * </pre>
     */
    public Term call_insertRoute(String head, Term expr,
                                 ThinkEngine engine, Object env) {
        Term route = expr.getArgTerm("route") ;
        if(route.isArray()) {
            for(int i = route.getArray().size() ; i > 0 ; i--) {
                ((ThinkEngine)env).getAgent().insertRouteTagSafely(route.getNthTerm(i-1)) ;
            }
        } else {
            ((ThinkEngine)env).getAgent().insertRouteTagSafely(route) ;
        }
        return Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(turn around)。
     * 現在位置のリンクで、反対向きに方向変換(turnAround)を行う。
     * 次の preUpdate() で反映される。
     * 2回やっても、元には戻らない。（turn around がセットされるだけなので）
     * <pre>
     * { "" : "turnAround" }
     * </pre>
     */
    public Term call_turnAround(String head, Term expr,
                                ThinkEngine engine, Object env) {
        ((ThinkEngine)env).getRationalAgent().setTurnAround() ;
        return Term_True ;
    }

    //------------------------------------------------------------
    /**
     * nextLinkCache をクリアする。
     * link の重みやゴールを変更した時に、間違った参照しないように、
     * 呼び出しておくべきである。
     * <pre>
     * { "" : "clearNextLinkCache" }
     * </pre>
     */
    public Term call_clearNextLinkCache(String head, Term expr,
                                        ThinkEngine engine, Object env) {
        ((ThinkEngine)env).getRationalAgent().clearNextLinkCache() ;
        return Term_True ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaAgent

