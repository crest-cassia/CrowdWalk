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
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;

import nodagumi.Itk.* ;
import nodagumi.Itk.Itk ;

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

        ThinkFormula.register("null", 	singleton, false, lexicon) ;
        ThinkFormula.register("quote", 	singleton, false, lexicon) ;
        ThinkFormula.register("log", 	singleton, false, lexicon) ;

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
    public Term call(String head, Term expr,
                     ThinkEngine engine, Object env) {
	if(head.equals("null")) {
	    return Term_Null ;
        } else if(head.equals("quote")) {
            return call_quote(head, expr, engine, env) ;
        } else if(head.equals("log")) {
            return call_log(head, expr, engine, env) ;
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
    public Term call_quote(String head, Term expr,
                           ThinkEngine engine, Object env) {
            return expr.getArgTerm("value") ;
    }

    //------------------------------------------------------------
    /**
     * 推論(log)。
     * <pre>
     *   {"":"log",
     *    "special": ("alertMessages" | "tags" | ...),
     *    ["level" : ("Trace" | "Debug" | "Info" | "Warn" | "Error" | "Fatal"}]
     *   }
     *  OR
     *   {"":"log",
     *    "tag": _StringForTag_,
     *    "value": _expr_,
     *    ["level" : ("Trace" | "Debug" | "Info" | "Warn" | "Error" | "Fatal"}]
     *   }
     * </pre>
     * "special":"alertMessages" の場合は、
     * エージェントが取得している alert message のリストが表示される。
     * "special":"tags" の場合は、
     * エージェントが保持している tag のリストが表示される。
     * "level" が省略された場合、規定値は "Info"。
     */
    public Term call_log(String head, Term expr,
                         ThinkEngine engine, Object env) {
        Term special = expr.getArgTerm("special") ;
        Itk.LogLevel level = getLogLevel(expr.getArgTerm("level")) ; 

        if(special != null) {
            if(special.equals("alertMessages")) {
                call_logAlertMessages(level, engine, env) ;
            } else if(special.equals("tags")) {
                call_logTags(level, engine, env) ;
            } else {
                Itk.logError("unknown log special:", expr) ;
            }
        } else {
            String tag = expr.getArgString("tag") ;
            Term value = expr.getArgTerm("value") ;
            Term result = ((value != null) ? engine.think(value, env) : null) ;

            if(result == null) {
                logGenericWithLevel(engine, level, true, tag) ;
            } else {
                logGenericWithLevel(engine, level, true, tag, ":", result) ;
            }
        }
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(log alertMessages)。
     */
    public void call_logAlertMessages(Itk.LogLevel level,
                                      ThinkEngine engine, Object env) {
        if(engine.isNullAgent()) {
            logGenericWithLevel(engine, level, true, "no agent") ;
        } else {
            logGenericWithLevel(engine, level, true, "alertMessages",
                                "time=", engine.getAgent().currentTime) ;
            for(Map.Entry<Term, SimTime> entry :
                    engine.getAlertedMessageTable().entrySet()) {
                Term message = entry.getKey() ;
                SimTime alertTime = entry.getValue() ;
                logGenericWithLevel(engine, level, true, message, alertTime) ;
            }
            logGenericWithLevel(engine, level, true, "-----------------") ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(log tags)。
     */
    public void call_logTags(Itk.LogLevel level,
                             ThinkEngine engine, Object env) {
        if(engine.isNullAgent()) {
            logGenericWithLevel(engine, level, false, "(null agent)") ;
        } else {
            logGenericWithLevel(engine, level, false,
                                "tags=", engine.getAgent().getTags()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * ログ出力(level付き)
     */
    public void logGenericWithLevel(ThinkEngine engine,
                                    Itk.LogLevel level,
                                    boolean contP,
                                    Object... objects) {
        engine.logInThinkWithLevel(level, contP, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ出力 level 解析
     */
    public Itk.LogLevel getLogLevel(Term term) {
        if(term == null) { // default
            return Itk.LogLevel.Info ;
        } else {
            return Itk.getLogLevel(term.getString()) ;
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaMisc

