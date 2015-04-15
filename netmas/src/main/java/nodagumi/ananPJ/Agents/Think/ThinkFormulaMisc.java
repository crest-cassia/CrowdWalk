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
import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考を表す式の処理系 (misc. functions)
 */
public class ThinkFormulaMisc extends ThinkFormula {
    //============================================================
    //------------------------------------------------------------
    /**
     * 登録
     */
    static public boolean registerFormulas() {
	Class currentClass =
	    new Object(){}.getClass().getEnclosingClass() ;

	try {
	    ThinkFormula thisFormula =
		(ThinkFormula)(currentClass.newInstance()) ;
	    ThinkFormula.register("log", thisFormula) ;
	} catch(Exception ex) {
	    Itk.logError("wrong class definition") ;
	    ex.printStackTrace() ;
	}

	return true ;
    }

    //------------------------------------------------------------
    /**
     * 呼び出し
     */
    @Override
    public Term call(String head, Term expr, ThinkEngine engine) {
	if(head.equals("null")) {
	    return engine.Term_Null ;
	} else {
	    Itk.logWarn("unknown expression", "expr=", expr) ;
	    return engine.Term_Null ;
	}
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaMisc

