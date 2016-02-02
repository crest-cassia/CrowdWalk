// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Formula, Logical and Control functions
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/04/16 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/04/16]: Create This File. </LI>
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
 * 論理演算および実行制御を表す式の処理系 (logica. functions).
 */
public class ThinkFormulaLogical extends ThinkFormula {
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
	    ex.printStackTrace() ;
	}
        
	lexicon.register("true", singleton) ;
	lexicon.register("false", singleton) ;
	lexicon.register("not", singleton) ;
	lexicon.register("and", singleton) ;
	lexicon.register("or", singleton) ;
	lexicon.register("proc", singleton) ;
	lexicon.register("if", singleton) ;

	return true ;
    }

    //------------------------------------------------------------
    /**
     * 呼び出し.
     * <pre>
     * { "" : "true" } || "true"
     * { "" : "false" } || "false"
     * </pre>
     * あるいは以下のフォーマット
     * <ul>
     *   <li>{@link #call_not "not"}</li>
     *   <li>{@link #call_and "and"}</li>
     *   <li>{@link #call_or "or"}</li>
     *   <li>{@link #call_proc "proc"}</li>
     *   <li>{@link #call_if "if"}</li>
     * </ul>
     */
    @Override
    public Term call(String head, Term expr,
                     ThinkEngine engine, Object env) {
	if(head.equals("true")) {
	    return Term_True ;
	} else if(head.equals("false")) {
	    return Term_False ;
	} else if(head.equals("not")) {
	    return call_not(expr, engine, env) ;
	} else if(head.equals("and")) {
	    return call_and(expr, engine, env) ;
	} else if(head.equals("or")) {
	    return call_or(expr, engine, env) ;
	} else if(head.equals("proc")) {
	    return call_proc(expr, engine, env) ;
	} else if(head.equals("if")) {
	    return call_if(expr, engine, env) ;
	} else {
	    Itk.logError("unknown expression", "expr=", expr) ;
	    return Term_Null ;
	}
    }

    //------------------------------------------------------------
    /**
     * 推論(not)。
     * <pre>
     * { "" : "not",
     *   "body" : _expr_ }
     * OR
     * [ "not", _expr ]
     * </pre>
     */
    public Term call_not(Term expr,
                         ThinkEngine engine, Object env) {
        Term body = getArgFromExpr(expr, "body", 1) ;
        Term result = engine.think(body, env) ;
        if(checkFalse(result)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(And)。
     * <pre>
     * { "" : "and",
     *   "body" : [_expr_, _expr_, ...] }
     * OR
     * ["and", _expr_, _expr_, ...]
     * </pre>
     */
    public Term call_and(Term expr,
                         ThinkEngine engine, Object env) {
        Term body = getArrayArgOrItself(expr, "body") ;
        int fromIndex = (expr.isArray() ? 1 : 0) ;

        if(!body.isArray()) {
	    Itk.logError("Illegal 'and' body",
			 "expr=", expr) ;
            System.exit(1) ;
        }

        Term result = Term_True ;
        for(int i = fromIndex ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = engine.think(subExpr, env) ;
            if(checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(Or)。
     * <pre>
     * { "" : "or",
     *   "body" : [ _expr_, _expr_, ...] }
     * OR
     * [ "or", _expr_, _expr, ...]
     * </pre>
     */
    public Term call_or(Term expr,
                        ThinkEngine engine, Object env) {
        Term body = getArrayArgOrItself(expr, "body") ;
        int fromIndex = (expr.isArray() ? 1 : 0) ;

        if(body == null) {
	    Itk.logError("Illegal 'or' body",
			 "expr=", expr) ;
            System.exit(1) ;
        }

        Term result = Term_False ;
        for(int i = fromIndex ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = engine.think(subExpr, env) ;
            if(!checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(proc)。
     * <pre>
     * { "" : "proc",
     *   "body" : [ _expr_, _expr_, ...] }
     * OR
     * [ _expr, _expr_, ...]
     * OR
     * [ "proc", _expr_, _expr_, ...]
     * </pre>
     */
    public Term call_proc(Term expr,
                          ThinkEngine engine, Object env) {
        Term body = getArrayArgOrItself(expr, "body") ;
        int fromIndex = (expr.isArray() ? 1 : 0) ;

        if(body == null) {
	    Itk.logError("Illegal 'proc' body",
			 "expr=", expr) ;
            System.exit(1) ;
        }

	return call_procBody(body, engine, env, fromIndex) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(proc)の本体。
     */
    public Term call_procBody(Term body,
                              ThinkEngine engine, Object env,
                              int fromIndex) {
        if(!body.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("body:", body) ;
            System.exit(1) ;
        }

        Term result = Term_Null ;
        for(int i = fromIndex ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = engine.think(subExpr, env) ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(if)。
     * <pre>
     * { "" : "if",
     *   ("condition" | "cond") : _expr_, 
     *   "then" : _expr_,
     *   "else" : _expr_ }
     * OR
     * [ "if", _cond_expr_, _then_expr_, _else_expr_ ]
     * </pre>
     */
    public Term call_if(Term expr,
                        ThinkEngine engine, Object env) {
        Term condition = getArgFromExpr(expr,"condition", 1) ;
        if(condition == null) {
            condition = getArgFromExpr(expr,"cond", 1) ;
        }
        Term thenExpr = getArgFromExpr(expr,"then", 2) ;
        Term elseExpr = getArgFromExpr(expr,"else", 3) ;

        if(condition == null || thenExpr == null) {
            Itk.logError("wrong if expr", expr) ;
            System.exit(1) ;
        }

        Term cond = engine.think(condition, env) ;

        if(!checkFalse(cond)) {
            return engine.think(thenExpr, env) ;
        } else {
            if(elseExpr != null) {
                return engine.think(elseExpr, env) ;
            } else {
                return Term_False ;
            }
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaLogical

