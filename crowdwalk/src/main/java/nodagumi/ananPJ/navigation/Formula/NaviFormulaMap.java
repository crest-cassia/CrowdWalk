// -*- mode: java; indent-tabs-mode: nil -*-
/** Navi Formula, Map Parameter functions
 * @author:: Itsuki Noda
 * @version:: 0.0 2016/01/30 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2016/01/30]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.navigation.Formula;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.lang.reflect.InvocationTargetException ;

import nodagumi.ananPJ.Agents.Think.ThinkEngine;
import nodagumi.ananPJ.Agents.Think.ThinkFormula;
import nodagumi.ananPJ.navigation.Formula.NaviFormula;
import nodagumi.ananPJ.navigation.Formula.NaviEngine;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考を表す式の処理系 (misc. functions)
 */
public class NaviFormulaMap extends NaviFormula {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 代表インスタンス
     * (複数あっても無駄なので、単一である方が良い)
     * registerFormulas で登録。
     */
    static ThinkFormula singleton ;

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Lexicon に登録するもの
     */
    static private final String Lex_length = Itk.intern(":length") ;
    static private final String Lex_width = Itk.intern(":width") ;
    static private final String Lex_hasTag = Itk.intern("hasTag") ;
    static private final String Lex_tag = Itk.intern("tag") ;


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

        lexicon.register(Lex_length, singleton) ;
        lexicon.register(Lex_width, singleton) ;
        lexicon.register(Lex_hasTag, singleton) ;

        return true ;
    }

    //------------------------------------------------------------
    /**
     * 呼び出し.
     * <ul>
     *   <li>{@link #call_length "length"}</li>
     *   <li>{@link #call_width "width"}</li>
     *   <li>{@link #call_hasTag "hasTag"}</li>
     * </ul>
     */
    @Override
    public Term call(String head, Term expr,
                     ThinkEngine engine, Object env) {
	if(head == Lex_length) {
	    return call_length(head, expr, engine, env) ;
        } else if(head == Lex_width) {
            return call_width(head, expr, engine, env) ;
        } else if(head == Lex_hasTag) {
            return call_hasTag(head, expr, engine, env) ;
        } else {
            Itk.logWarn("unknown expression", "expr=", expr) ;
	    return Term_Null ;
	}
    }

    //------------------------------------------------------------
    /**
     * length。
     * <pre>
     *   {"":":length"} | ":length"
     * </pre>
     * 対象リンクの長さを返す。
     */
    public Term call_length(String head, Term expr,
                            ThinkEngine engine, Object _env) {
        NaviFormulaEnv env = (NaviFormulaEnv)_env ;
	return new Term(getLink(env).getLength()) ;
    }

    //------------------------------------------------------------
    /**
     * width。
     * <pre>
     *   {"":":width"} | ":width"
     * </pre>
     */
    public Term call_width(String head, Term expr,
                           ThinkEngine engine, Object env) {
	return new Term(getLink(env).getWidth()) ;
    }

    //------------------------------------------------------------
    /**
     * タグチェック。
     * <pre>
     *   {"":"hasTag",
     *    "tag": __TagString__}
     * </pre>
     */
    public Term call_hasTag(String head, Term expr,
                            ThinkEngine engine, Object env) {
	boolean result = false ;
	if(expr.hasArg(Lex_tag)) {
	    Term tag = expr.getArgTerm(Lex_tag) ;
	    result = getLink(env).hasTag(tag) ;
	} else {
	    Itk.logError("hasTag formula should have 'tag' slot.") ;
	    Itk.logError_("expr=", expr) ;
	    System.exit(1) ;
	}
	return ThinkFormula.booleanTerm(result) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class NaviFormulaMap

