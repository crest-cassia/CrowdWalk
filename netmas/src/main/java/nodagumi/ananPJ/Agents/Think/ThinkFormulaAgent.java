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
    static public boolean registerFormulas() {
        try {
            Class currentClass =
                new Object(){}.getClass().getEnclosingClass() ;
            singleton =
                (ThinkFormula)(currentClass.newInstance()) ;
        } catch(Exception ex) {
            Itk.logError("wrong class definition") ;
            ex.printStackTrace() ;
        }

        ThinkFormula.register("getFallback", singleton) ;
        ThinkFormula.register("getParam", singleton) ;
        ThinkFormula.register("setParam", singleton) ;
        ThinkFormula.register("agentHasTag", singleton) ;
        ThinkFormula.register("placeHasTag", singleton) ;
        ThinkFormula.register("listenAlert", singleton) ;
        ThinkFormula.register("clearAlert", singleton) ;
        ThinkFormula.register("clearAlertAll", singleton) ;
        ThinkFormula.register("changeGoal", singleton) ;
        ThinkFormula.register("clearPlannedRoute", singleton) ;
        ThinkFormula.register("insertRoute", singleton) ;

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
     *   <li>{@link #call_listenAlert "listenAlert"}</li>
     *   <li>{@link #call_clearAlert "clearAlert"}</li>
     *   <li>{@link #call_clearAllAlert "clearAllAlert"}</li>
     *   <li>{@link #call_changeGoal "changeGoal"}</li>
     *   <li>{@link #call_clearPlannedRoute "clearPlannedRoute"}</li>
     *   <li>{@link #call_insertRoute "insertRoute"}</li>
     * </ul>
     */
    @Override
    public Term call(String head, Term expr, ThinkEngine engine) {
	if(head.equals("getFallback")) {
            return call_getFallback(head, expr, engine) ;
        } else if(head.equals("getParam")) {
            return call_getParam(head, expr, engine) ;
        } else if(head.equals("setParam")) {
            return call_setParam(head, expr, engine) ;
        } else if(head.equals("agentHasTag")) {
            return call_agentHasTag(head, expr, engine) ;
        } else if(head.equals("placeHasTag")) {
            return call_placeHasTag(head, expr, engine) ;
        } else if(head.equals("listenAlert")) {
            return call_listenAlert(head, expr, engine) ;
        } else if(head.equals("clearAlert")) {
            return call_clearAlert(head, expr, engine) ;
        } else if(head.equals("clearAllAlert")) {
            return call_clearAllAlert(head, expr, engine) ;
        } else if(head.equals("changeGoal")) {
            return call_changeGoal(head, expr, engine) ;
        } else if(head.equals("clearPlannedRoute")) {
            return call_clearPlannedRoute(head, expr, engine) ;
        } else if(head.equals("insertRoute")) {
            return call_insertRoute(head, expr, engine) ;
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
    public Term call_getFallback(String head, Term expr, ThinkEngine engine) {
        String name = expr.getArgString("name") ;
        RationalAgent agent = (RationalAgent)engine.getAgent() ;

        return agent.getTermFromConfig(name, Term_Null) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(getParam)。
     * <pre>
     * { "" : "getParam",
     *   "name" : _nameOfValue_,
     * }
     *  _nameOfValue_ ::= "currentTime" | "agentId" | "linkId" |
     *                    "speed" | "emptySpeed" | "goal"
     * </pre>
     */
    public Term call_getParam(String head, Term expr, ThinkEngine engine) {
        String name = expr.getArgString("name") ;
        RationalAgent agent = (RationalAgent)engine.getAgent() ;

        if(name.equals("currentTime")) {
            return new Term(agent.currentTime) ;
        } else if(name.equals("agentId")) {
            return new Term(agent.ID) ;
        } else if(name.equals("linkId")) {
	    return new Term(agent.getCurrentLink().ID) ;
        } else if(name.equals("speed")) {
	    return new Term(agent.getSpeed()) ;
        } else if(name.equals("emptySpeed")) {
	    return new Term(agent.getEmptySpeed()) ;
        } else if(name.equals("goal")) {
	    return agent.getGoal() ;
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
     * </pre>
     */
    public Term call_setParam(String head, Term expr, ThinkEngine engine) {
        String name = expr.getArgString("name") ;
        Term value = engine.think(expr.getArgTerm("value")) ;
        RationalAgent agent = (RationalAgent)engine.getAgent() ;
        if(name.equals("emptySpeed")) {
            agent.setEmptySpeed(value.getDouble()) ;
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
    public Term call_agentHasTag(String head, Term expr, ThinkEngine engine) {
        String tag = expr.getArgString("tag") ;
        if(engine.getAgent().hasTag(tag)) {
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
    public Term call_placeHasTag(String head, Term expr, ThinkEngine engine) {
        String tag = expr.getArgString("tag") ;
        if(engine.getAgent().getCurrentLink().hasTag(tag)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(listenAlert)。
     * <pre>
     * { "" : "listenAlert",
     *   "message" : _alertMessage_ }
     * _alertMessage_ ::= _String_
     * </pre>
     * {@code "message"} は、
     * {@link nodagumi.ananPJ.Scenario.Scenario Scenario} の中の
     * {@link nodagumi.ananPJ.Scenario.AlertEvent AlertEvent} 参照。
     */
    public Term call_listenAlert(String head, Term expr, ThinkEngine engine) {
        Term message = expr.getArgTerm("message") ;
        Double time = engine.getAlertedMessageTable().get(message) ;
        if(time != null) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(clearAlert)。
     * <pre>
     * { "" : "clearAlert",
     *   "message" : _alertMessage_ }
     * _alertMessage_ ::= _String_
     * </pre>
     */
    public Term call_clearAlert(String head, Term expr, ThinkEngine engine) {
        Term message = expr.getArgTerm("message") ;
        engine.getAlertedMessageTable().remove(message) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(clearAllAlert)。
     * <pre>
     * { "" : "clearAllAlert" }
     * </pre>
     */
    public Term call_clearAllAlert(String head, Term expr, ThinkEngine engine) {
        engine.getAlertedMessageTable().clear() ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(changeGoal)。
     * <pre>
     * { "" : "changeGoal",
     *   "goal" : _goalTag_ }
     * _goalTag_ ::= _String_
     * </pre>
     */
    public Term call_changeGoal(String head, Term expr, ThinkEngine engine) {
        Term goalTag = expr.getArgTerm("goal") ;
        engine.getAgent().changeGoal(goalTag) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(clear PlannedRoute)。
     * <pre>
     * { "" : "clearPlannedRoute" }
     * </pre>
     */
    public Term call_clearPlannedRoute(String head, Term expr,
                                       ThinkEngine engine) {
        engine.getAgent().setPlannedRoute(new ArrayList<Term>(), true) ;
        return Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(insert route)。
     * <pre>
     * { "" : "insertRoute",
     *   "route" : _route_ }
     * _route_ ::= _tag_ || [_tag_, _tag_, ...]
     * </pre>
     */
    public Term call_insertRoute(String head, Term expr, ThinkEngine engine) {
        Term route = expr.getArgTerm("route") ;
        if(route.isArray()) {
            for(int i = route.getArray().size() ; i > 0 ; i--) {
                engine.getAgent().insertRouteTagSafely(route.getNthTerm(i-1)) ;
            }
        } else {
            engine.getAgent().insertRouteTagSafely(route) ;
        }
        return Term_True ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaAgent
