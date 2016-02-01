// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Formula, Arithmetic functions
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
import java.math.BigDecimal;

import java.lang.reflect.InvocationTargetException ;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.RationalAgent;
import nodagumi.ananPJ.Agents.Think.ThinkEngine;
import nodagumi.ananPJ.Agents.Think.ThinkFormula;
import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考を表す式の処理系 (arithmetic. functions)
 */
public class ThinkFormulaArithmetic extends ThinkFormula {
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

        lexicon.register("add", singleton) ;
        lexicon.register("+", singleton) ;
        lexicon.register("sub", singleton) ;
        lexicon.register("-", singleton) ;
        lexicon.register("mul", singleton) ;
        lexicon.register("*", singleton) ;
        lexicon.register("div", singleton) ;
        lexicon.register("/", singleton) ;
        lexicon.register("mod", singleton) ;
        lexicon.register("%", singleton) ;

        lexicon.register("equal", singleton) ;
        lexicon.register("==", singleton) ;
        lexicon.register(">", singleton) ;
        lexicon.register(">=", singleton) ;
        lexicon.register("=>", singleton) ;
        lexicon.register("<", singleton) ;
        lexicon.register("<=", singleton) ;
        lexicon.register("=<", singleton) ;

        lexicon.register("random", singleton) ;

        return true ;
    }

    //------------------------------------------------------------
    /**
     * 呼び出し.
     * <ul>
     *   <li>{@link #call_add "add"}</li>
     *   <li>{@link #call_sub "sub"}</li>
     *   <li>{@link #call_mul "mul"}</li>
     *   <li>{@link #call_div "div"}</li>
     *   <li>{@link #call_mod "mod"}</li>
     *   <li>{@link #call_equal "equal"}</li>
     *   <li>{@link #call_greaterThan "greater"}</li>
     *   <li>{@link #call_greaterThan "greaterOrEqual"}</li>
     *   <li>{@link #call_greaterThan "less"}</li>
     *   <li>{@link #call_greaterThan "lessOrEqual"}</li>
     *   <li>{@link #call_random "random"}</li>
     * </ul>
     */
    @Override
    public Term call(String head, Term expr,
                     ThinkEngine engine, Object env) {
	if(head.equals("add") || head.equals("+")) {
            return call_add(head, expr, engine, env) ;
	} else if(head.equals("sub") || head.equals("-")) {
            return call_sub(head, expr, engine, env) ;
	} else if(head.equals("mul") || head.equals("*")) {
            return call_mul(head, expr, engine, env) ;
	} else if(head.equals("div") || head.equals("/")) {
            return call_div(head, expr, engine, env) ;
	} else if(head.equals("mod") || head.equals("%")) {
            return call_mod(head, expr, engine, env) ;
	} else if(head.equals("equal") || head.equals("==")) {
            return call_equal(head, expr, engine, env) ;
	} else if(head.equals(">")) {
	    return call_greaterThan(head, expr, engine, env) ;
	} else if(head.equals(">=") || head.equals("=>")) {
	    return call_greaterOrEqual(head, expr, engine, env) ;
	} else if(head.equals("<")) {
	    return call_lessThan(head, expr, engine, env) ;
	} else if(head.equals("<=") || head.equals("=<")) {
	    return call_lessOrEqual(head, expr, engine, env) ;
        } else if(head.equals("random")) {
	    return call_random(head, expr, engine, env) ;
        } else {
            Itk.logWarn("unknown expression", "expr=", expr) ;
	    return Term_Null ;
	}
    }

    //------------------------------------------------------------
    /**
     * 足し算.
     * <pre>
     *   {"":("add" || "+"),
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * もしくは
     * <pre>
     *   {"":("add" || "+"),
     *    "values": [_Term_, _Term_, _Term_, ...] }
     * </pre>
     * _Term_ の和を返す。
     */
    public Term call_add(String head, Term expr,
                         ThinkEngine engine, Object env) {
	BigDecimal result = null ;
	if(expr.hasArg("values")) {
	    Term values = expr.getArgTerm("values") ;
	    if(values.isArray()) {
		for(int i = 0 ; i < values.getArraySize() ; i++) {
		    Term val = engine.think(values.getNthTerm(i), env) ;
		    if(result == null) {
			result = val.getBigDecimal() ;
		    } else {
			result = result.add(val.getBigDecimal()) ;
		    }
		}
	    }
	} else if(expr.hasArg("left") && expr.hasArg("right")) {
	    Term left = engine.think(expr.getArgTerm("left"), env) ;
	    Term right = engine.think(expr.getArgTerm("right"), env) ;
	    result = left.getBigDecimal().add(right.getBigDecimal()) ;
	}
	if(result == null) {
	    Itk.logError("add formula should have 'left' and 'right' slots, or 'values' slot.") ;
	    Itk.logError_("Or values should be an array of numbers.") ;
	    Itk.logError_("expr=", expr) ;
	    System.exit(1) ;
	}
	return new Term(result) ;
    }

    //------------------------------------------------------------
    /**
     * 引き算.
     * <pre>
     *   {"":("sub" || "-"),
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の差を返す。
     */
    public Term call_sub(String head, Term expr,
                         ThinkEngine engine, Object env) {
	BigDecimal result = null ;
	if(expr.hasArg("left") && expr.hasArg("right")) {
	    Term left = engine.think(expr.getArgTerm("left"), env) ;
	    Term right = engine.think(expr.getArgTerm("right"), env) ;
	    result = left.getBigDecimal().subtract(right.getBigDecimal()) ;
	}
	if(result == null) {
	    Itk.logError("sub formula should have 'left' and 'right' slots.") ;
	    Itk.logError_("expr=", expr) ;
	    System.exit(1) ;
	}
	return new Term(result) ;
    }

    //------------------------------------------------------------
    /**
     * 掛け算.
     * <pre>
     *   {"":("mul" || "*"),
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * もしくは
     * <pre>
     *   {"":("mul" || "*"),
     *    "values": [_Term_, _Term_, _Term_, ...] }
     * </pre>
     * _Term_ の和を返す。
     */
    public Term call_mul(String head, Term expr,
                         ThinkEngine engine, Object env) {
	BigDecimal result = null ;
	if(expr.hasArg("values")) {
	    Term values = expr.getArgTerm("values") ;
	    if(values.isArray()) {
		for(int i = 0 ; i < values.getArraySize() ; i++) {
		    Term val = engine.think(values.getNthTerm(i), env) ;
		    if(result == null) {
			result = val.getBigDecimal() ;
		    } else {
			result = result.multiply(val.getBigDecimal()) ;
		    }
		}
	    }
	} else if(expr.hasArg("left") && expr.hasArg("right")) {
	    Term left = engine.think(expr.getArgTerm("left"), env) ;
	    Term right = engine.think(expr.getArgTerm("right"), env) ;
	    result = left.getBigDecimal().multiply(right.getBigDecimal()) ;
	}
	if(result == null) {
	    Itk.logError("mul formula should have 'left' and 'right' slots, or 'values' slot.") ;
	    Itk.logError_("Or values should be an array of numbers.") ;
	    Itk.logError_("expr=", expr) ;
	    System.exit(1) ;
	}
	return new Term(result) ;
    }

    //------------------------------------------------------------
    /**
     * 割り算.
     * <pre>
     *   {"":("div" || "/"),
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の差を返す。
     */
    public Term call_div(String head, Term expr,
                         ThinkEngine engine, Object env) {
	BigDecimal result = null ;
	if(expr.hasArg("left") && expr.hasArg("right")) {
	    Term left = engine.think(expr.getArgTerm("left"), env) ;
	    Term right = engine.think(expr.getArgTerm("right"), env) ;
	    result = left.getBigDecimal().divide(right.getBigDecimal()) ;
	}
	if(result == null) {
	    Itk.logError("sub formula should have 'left' and 'right' slots.") ;
	    Itk.logError_("expr=", expr) ;
	    System.exit(1) ;
	}
	return new Term(result) ;
    }

    //------------------------------------------------------------
    /**
     * 剰余系.
     * <pre>
     *   {"":("mod" || "%"),
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の差を返す。
     */
    public Term call_mod(String head, Term expr,
                         ThinkEngine engine, Object env) {
	BigDecimal result = null ;
	if(expr.hasArg("left") && expr.hasArg("right")) {
	    Term left = engine.think(expr.getArgTerm("left"), env) ;
	    Term right = engine.think(expr.getArgTerm("right"), env) ;
	    result = left.getBigDecimal().remainder(right.getBigDecimal()) ;
	}
	if(result == null) {
	    Itk.logError("mod formula should have 'left' and 'right' slots.") ;
	    Itk.logError_("expr=", expr) ;
	    System.exit(1) ;
	}
	return new Term(result) ;
    }

    //------------------------------------------------------------
    /**
     * 等しい.
     * <pre>
     *   {"":"==",
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の等価チェック。
     */
    public Term call_equal(String head, Term expr,
                           ThinkEngine engine, Object env) {
	Term result = null ;
	if(expr.hasArg("left") && expr.hasArg("right")) {
	    Term left = engine.think(expr.getArgTerm("left"), env) ;
	    Term right = engine.think(expr.getArgTerm("right"), env) ;
	    if(left.equals(right)) {
		result = Term_True ;
	    } else {
		result = Term_False ;
	    }
	}
	if(result == null) {
	    Itk.logError("equal formula should have 'left' and 'right' slots.") ;
	    Itk.logError_("expr=", expr) ;
	    System.exit(1) ;
	}
	return new Term(result) ;
    }

    //------------------------------------------------------------
    /**
     * 大なり.
     * <pre>
     *   {"":{@literal ">"},
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の等価チェック。
     */
    public Term call_greaterThan(String head, Term expr,
                                 ThinkEngine engine, Object env) {
	int comp = compareLeftRightValues(expr, engine, env) ;
	if(comp > 0) {
	    return Term_True ;
	} else {
	    return Term_False ;
	}
    }

    //------------------------------------------------------------
    /**
     * 大なりイコール.
     * <pre>
     *   {"":{@literal (">=" || "=>")},
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の等価チェック。
     */
    public Term call_greaterOrEqual(String head, Term expr,
                                    ThinkEngine engine, Object env) {
	int comp = compareLeftRightValues(expr, engine, env) ;
	if(comp >= 0) {
	    return Term_True ;
	} else {
	    return Term_False ;
	}
    }

    //------------------------------------------------------------
    /**
     * 小なり.
     * <pre>
     *   {"":{@literal "<"},
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の等価チェック。
     */
    public Term call_lessThan(String head, Term expr,
                              ThinkEngine engine, Object env) {
	int comp = compareLeftRightValues(expr, engine, env) ;
	if(comp < 0) {
	    return Term_True ;
	} else {
	    return Term_False ;
	}
    }

    //------------------------------------------------------------
    /**
     * 小なりイコール.
     * <pre>
     *   {"":{@literal ("<=" || "=<")},
     *    "left": _Term_,
     *    "right": _Term_}
     * </pre>
     * _Term_ の等価チェック。
     */
    public Term call_lessOrEqual(String head, Term expr,
                                 ThinkEngine engine, Object env) {
	int comp = compareLeftRightValues(expr, engine, env) ;
	if(comp <= 0) {
	    return Term_True ;
	} else {
	    return Term_False ;
	}
    }

    //------------------------------------------------------------
    /**
     * 比較一般.
     */
    public int compareLeftRightValues(Term expr,
                                      ThinkEngine engine, Object env) {
	if(expr.hasArg("left") && expr.hasArg("right")) {
	    Term left = engine.think(expr.getArgTerm("left"), env) ;
	    Term right = engine.think(expr.getArgTerm("right"), env) ;
	    if(left.isNumber() && right.isNumber()) {
		return left.getBigDecimal().compareTo(right.getBigDecimal()) ;
	    }
	}
	Itk.logError("comparison formula should have 'left' and 'right' slots.") ;
	Itk.logError_("expr=", expr) ;
	System.exit(1) ;

	return 0 ; // never reach here.
    }

    //------------------------------------------------------------
    /**
     * 乱数.
     * <pre>
     *   {"":"random",
     *    "type" : _RandomType_,
     *    "max" : _integer_ ;;;  "int" の場合のみ
     *   }
     *  _RandomType_ ::= "int" | "double"
     * </pre>
     */
    public Term call_random(String head, Term expr,
                            ThinkEngine engine, Object env) {
        String type = expr.getArgString("type") ;
        if(type == null || type.equals("int")) {
            if(expr.hasArg("max")) {
                return new Term(engine.getAgent()
                                .getRandomInt(expr.getArgInt("max"))) ;
            } else {
                return new Term(engine.getAgent().getRandomInt()) ;
            }
        } else if(type.equals("double")) {
            return new Term(engine.getAgent().getRandomDouble()) ;
        } else {
            Itk.logError("unknown type for random in getValue.",
                         "type=", type) ;
            return Term_Null ;
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaArithmetic

