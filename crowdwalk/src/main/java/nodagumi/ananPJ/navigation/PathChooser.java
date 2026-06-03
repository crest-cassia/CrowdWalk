// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.navigation.Formula.*;

import nodagumi.Itk.Term;


//======================================================================
/**
 * 探索において、各リンクおよびゴールノードのコストを計算する。
 * 変更ルールの記述は、property.json の中で以下のように行う。
 * <pre>
 *   { ...
 *     "mental_map_rules" : { "modeName1" : __Rule__,
 *                            "modeName2" : __Rule__,
 *                            ... },
 *   ...}
 * </pre>
 *   __Rule__ は、Agent の ThinkEngine の {@link nodagumi.ananPJ.Agents.Think.ThinkFormula ThinkFormula}に準拠。
 */
public class PathChooser {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** 主観モードのタグ。デフォルトは null */
    Term mentalMode ;
    /** ルールオブジェクト (これから拡張するため)*/
    Term modifyRule ;

    /** ルールオブジェクト (これから拡張するため)*/
    NaviEngine engine ; 
    
    //----------------------------------------------------------------------
    /**
     * constractor.
     */
    public PathChooser() {
        mentalMode = null ;
        modifyRule = null ;
        engine = null ;
    }

    /**
     * constractor.
     */
    public PathChooser(String _mentalMode, Term _modifyRule) {
        this(new Term(_mentalMode, false), _modifyRule) ;
    }
    
    /**
     * constractor.
     */
    public PathChooser(Term _mentalMode, Term _modifyRule) {
        mentalMode = _mentalMode ;
        modifyRule = _modifyRule ;
        engine = new NaviEngine() ;
    }
    
    //----------------------------------------------------------------------
    /**
     * リンクコストの計算。
     */
    public double calcLinkCost(MapLink link, MapNode fromNode) {
        if(!link.isAvailableFrom(fromNode)) {
            return Double.POSITIVE_INFINITY ;
        } else if(mentalMode == null) {
            return 1.0 * link.getLength() ;
        } else {
            synchronized(link){
                if(link.hasMentalLength(mentalMode, fromNode)) {
                    return link.getMentalLength(mentalMode, fromNode) ;
                } else {
                    double cost = engine.calc(link, fromNode, modifyRule) ;
                    link.setMentalLength(mentalMode, fromNode, cost) ;
                    return cost ;
                }
            }
        }
    }

    //----------------------------------------------------------------------
    /**
     * リンクコストの計算。
     */
    public double calcGoalNodeCost(MapNode node) {
        return 0.0 ;
    }
}

// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
