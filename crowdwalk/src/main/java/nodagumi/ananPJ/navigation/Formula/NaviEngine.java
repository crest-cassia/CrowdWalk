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
 *   <li>{@link NaviFormula NaviFormula: 式要素 トップ}</li>
 * </ul>
 * また、上記にマッチしないアトム（配列でもオブジェクトでもないデータ）は、
 * その値を直接返す。（リテラル扱い）
 */
public class NaviEngine extends ThinkEngine {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 対象リンク。
     */
    private MapLink link = null ;

    /**
     * 侵入友ノード
     */
    private MapNode fromNode = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * log tag
     */
    static public String LogTagPrefix = "Navi:" ;
    public String logTag() {
        if(isNullLink()) {
            return LogTagPrefix + "(null)" ;
        } else {
            return LogTagPrefix + link.toString() ;
        }
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NaviEngine(){
        setLink(null) ;
	setFromNode(null) ;
        setRule(null) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NaviEngine(Term _rule){
        setLink(null) ;
	setFromNode(null) ;
        setRule(_rule) ;
    }

    //------------------------------------------------------------
    // アクセス
    //------------------------------------------------------------
    /**
     * set link
     */
    public MapLink setLink(MapLink _link) {
        link = _link ;
        return link ;
    }

    //------------------------------------------------------------
    /**
     * get link
     */
    public MapLink getLink() {
        return link ;
    }

    //------------------------------------------------------------
    /**
     * check agent is null
     */
    public boolean isNullLink() {
        return link == null;
    }

    //------------------------------------------------------------
    /**
     * set link
     */
    public MapNode setFromNode(MapNode _fromNode) {
        fromNode = _fromNode ;
        return fromNode ;
    }

    //------------------------------------------------------------
    /**
     * get link
     */
    public MapNode getFromNode() {
        return fromNode ;
    }

    //------------------------------------------------------------
    /**
     * 推論(top)
     */
    public double calc(MapLink _link, MapNode _fromNode, Term _rule) {
	setLink(_link) ;
	setFromNode(_fromNode) ;
	Term result = think(_rule) ;
        return result.getDouble() ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class naviEngine

