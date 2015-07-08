// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Formula, Misc functions
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
import nodagumi.ananPJ.misc.SimClock;
import nodagumi.ananPJ.misc.SimClock.SimTime;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考を表す式の処理系 (misc. functions)
 */
public class ThinkFormulaMisc extends ThinkFormula {
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

        ThinkFormula.register("null", singleton) ;
        ThinkFormula.register("quote", singleton) ;
        ThinkFormula.register("log", singleton) ;

        return true ;
    }

    //------------------------------------------------------------
    /**
     * 呼び出し.
     * <pre>
     * { "" : "null" } || null
     * </pre>
     * あるいは以下のフォーマット
     * <ul>
     *   <li>{@link #call_quote "quote"}</li>
     *   <li>{@link #call_log "log"}</li>
     * </ul>
     */
    @Override
    public Term call(String head, Term expr, ThinkEngine engine) {
	if(head.equals("null")) {
	    return Term_Null ;
        } else if(head.equals("quote")) {
            return call_quote(head, expr, engine) ;
        } else if(head.equals("log")) {
            return call_log(head, expr, engine) ;
        } else {
            Itk.logWarn("unknown expression", "expr=", expr) ;
	    return Term_Null ;
	}
    }

    //------------------------------------------------------------
    /**
     * quote。
     * <pre>
     *   {"":"quote",
     *    "value": _Term_ }
     * </pre>
     * _Term_ を評価せずに返す。
     */
    public Term call_quote(String head, Term expr, ThinkEngine engine) {
            return expr.getArgTerm("value") ;
    }

    //------------------------------------------------------------
    /**
     * 推論(log)。
     * <pre>
     *   {"":"log",
     *    "special": ("alertMessages" | "tags" | ...)}
     *  OR
     *   {"":"log",
     *    "tag": _StringForTag_,
     *    "value": _expr_}
     * </pre>
     * "special":"alertMessages" の場合は、
     * エージェントが取得している alert message のリストが表示される。
     * "special":"tags" の場合は、
     * エージェントが保持している tag のリストが表示される。
     */
    public Term call_log(String head, Term expr, ThinkEngine engine) {
        Term special = expr.getArgTerm("special") ;
        if(special != null) {
            if(special.equals("alertMessages")) {
                call_logAlertMessages(engine) ;
            } else if(special.equals("tags")) {
                call_logTags(engine) ;
            } else {
                Itk.logError("unknown log special:", expr) ;
            }
        } else {
            String tag = expr.getArgString("tag") ;
            Term value = expr.getArgTerm("value") ;
            Term result = ((value != null) ? engine.think(value) : null) ;

            if(result == null) {
                Itk.logInfo(engine.logTag(), tag) ;
            } else {
                Itk.logInfo(engine.logTag(), tag, ":", result) ;
            }
        }
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(log alertMessages)。
     */
    public void call_logAlertMessages(ThinkEngine engine) {
        if(engine.isNullAgent()) {
            Itk.logInfo(engine.logTag(), "no agent") ;
        } else {
            Itk.logInfo(engine.logTag(), "alertMessages",
                        "time=", engine.getAgent().clock) ;
            for(Map.Entry<Term, SimTime> entry :
                    engine.getAlertedMessageTable().entrySet()) {
                Term message = entry.getKey() ;
                SimTime alertTime = entry.getValue() ;
                Itk.logInfo(engine.LogTagPrefix, message, alertTime) ;
            }
            Itk.logInfo(engine.LogTagPrefix, "-----------------") ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(log tags)。
     */
    public void call_logTags(ThinkEngine engine) {
        if(engine.isNullAgent()) {
            Itk.logInfo(engine.logTag(), "(null agent)") ;
        } else {
            Itk.logInfo(engine.logTag(), "tags=", engine.getAgent().getTags()) ;
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaMisc

