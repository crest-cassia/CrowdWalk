// -*- mode: java; indent-tabs-mode: nil -*-
/** Navigation Formula
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

import nodagumi.ananPJ.Agents.Think.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.Itk.* ;

//======================================================================
/**
 * 主観によるMapの距離計算を表す式の処理系。
 * <h3> Rule Formula の概要 </h3>
 * <ul><li>
 *   {@link nodagumi.ananPJ.Agents.Think パッケージ Think 参照} 
 * </li></ul>
 * <h3>組み込まれている Formula リスト</h3>
 * <ul>
 *  <li>{@link nodagumi.ananPJ.Agents.Think.ThinkFormulaLogical#call Logical: 論理形式および実行制御}</li>
 *  <li>{@link nodagumi.ananPJ.Agents.Think.ThinkFormulaArithmetic#call Arithmetic: 数値処理など}</li>
 *  <li>{@link NaviFormulaMap#call Map: マップパラメータ関係}</li>
 *  <li>{@link nodagumi.ananPJ.Agents.Think.ThinkFormulaMisc#call Misc: その他}</li>
 * </ul>
 *
 * <h3>新しい Formula の作成方法 </h3>
 * <a name="addToExistingThinkFormula"></a>
 * <a href="../../../ananPJ/Agents/Think/ThinkFormula.html#addToExistingThinkFormula">go to ThinkFormula#addToExistingThinkFormula</a>
 */
abstract public class NaviFormula extends ThinkFormula {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Formula Lexicon
     */
    static public Lexicon lexicon = new Lexicon() ;
    static {
        ThinkFormulaMisc.registerFormulas(NaviFormula.lexicon) ;
        ThinkFormulaLogical.registerFormulas(NaviFormula.lexicon) ;
        ThinkFormulaArithmetic.registerFormulas(NaviFormula.lexicon) ;
        NaviFormulaMap.registerFormulas(NaviFormula.lexicon) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * Formula を検索
     */
    static public ThinkFormula findFormula(String head) {
	return (ThinkFormula)(lexicon.lookUp(head)) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * Formula を検索
     */
    static public ThinkFormula findFormula(Term head) {
	return (ThinkFormula)(lexicon.lookUp(head.getString())) ;
    }

    //============================================================
    /** 
     * NaviFormula の計算のための環境情報. 
     */
    public static class NaviFormulaEnv {
        public MapLink link ;
        public MapNode fromNode ;
        public NaviFormulaEnv(MapLink _link, MapNode _fromNode) {
            link = _link ;
            fromNode = _fromNode ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NaviFormula(){}

    //------------------------------------------------------------
    /**
     * engine から link の取り出し。
     */
    public MapLink getLink(Object _env) {
        NaviFormulaEnv env = (NaviFormulaEnv)_env ;
	return env.link ;
    }

    //------------------------------------------------------------
    /**
     * engine から fromNode の取り出し。
     */
    public MapNode getFromNode(Object _env) {
        NaviFormulaEnv env = (NaviFormulaEnv)_env ;
	return env.fromNode ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormula

