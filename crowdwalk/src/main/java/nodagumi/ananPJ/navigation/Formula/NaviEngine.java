// -*- mode: java; indent-tabs-mode: nil -*-
/** Navi Formula Engine
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

import nodagumi.ananPJ.Agents.Think.ThinkEngine;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.Itk.* ;

//======================================================================
/**
 * Navigation 用 主観距離計算エンジン.
 *
 * ルールは、Agent の ThinkEngine の記法に準拠。
 * <ul>
 *   <li>{@link NaviFormula NaviFormula: 式要素 トップ}</li>
 * </ul>
 * また、上記にマッチしないアトム（配列でもオブジェクトでもないデータ）は、
 * その値を直接返す。（リテラル扱い）
 */
public class NaviEngine extends ThinkEngine {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * log tag
     */
    static final public String LogTagPrefix = "Navi:" ;
    public String logTag() {
        return LogTagPrefix ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NaviEngine(){
        setLexicon(NaviFormula.lexicon) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(top)
     */
    public double calc(MapLink _link, MapNode _fromNode, Term _rule) {
        NaviFormula.NaviFormulaEnv env =
            new NaviFormula.NaviFormulaEnv(_link, _fromNode) ;
	Term result = think(_rule, env) ;
        return result.getDouble() ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class naviEngine

