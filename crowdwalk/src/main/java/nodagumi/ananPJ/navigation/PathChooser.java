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
import nodagumi.Itk.Term;


//======================================================================
/**
 * 探索において、各リンクおよびゴールノードのコストを計算する。
 * 変更ルールの記述は、property.json の中で以下のように行う。
 * <pre>
 *   { ...
 *     "subjective_mode_rules" : { "modeName1" : __Rule__,
 *                                 "modeName2" : __Rule__,
 *                                 ... },
 *   ...}
 *
 *   __Rule__ ::= __Expr__
 *   __Expr__ ::= __Value__ | __Func__
 *   __Value__ ::= __Number__ | __Boolean__ | __String__
 *   __Func__ ::= { "" : "funcName", ...(args) }
 *
 *   __Func__ categories
 *     [parameter accessor]
 *       {"":"length"}			         	// length of the link
 *       {"":"theLink", "value":"length"} 	// length of the link
 *       {"":"width"}                    	// width of the link
 *       {"":"theLink", "value":"width"}	// width of the link
 *       {"":"hasTag", "tag":__Value__}		// check the link has tag
 *     [logical forms]
 *       {"":"true"}							// true
 *       {"":"false"}							// false
 *       {"":"not", "body":__Expr__}			// negation
 *       {"":"and", "body":[__Expr__, __Expr__,...]}	// and
 *       {"":"or", "body":[__Expr__, __Expr__,...]}	// or
 *       {"":"if", "cond":__Expr__, "then":__Expr__, "else":__Expr__} // if
 *     [arithmetic functions]
 *       {"":"add", "left":__Expr__, "right":__Expr__}
 *       {"":"+", "left":__Expr__, "right":__Expr__}
 *       {"":"sub", "left":__Expr__, "right":__Expr__}
 *       {"":"-", "left":__Expr__, "right":__Expr__}
 *       {"":"mul", "left":__Expr__, "right":__Expr__}
 *       {"":"*", "left":__Expr__, "right":__Expr__}
 *       {"":"div", "left":__Expr__, "right":__Expr__}
 *       {"":"/", "left":__Expr__, "right":__Expr__}
 *     [comparison functions]
 *       {"":"==", "left":__Expr__, "right":__Expr__}
 *       {"":">", "left":__Expr__, "right":__Expr__}
 *       {"":">=", "left":__Expr__, "right":__Expr__}
 *       {"":"=>", "left":__Expr__, "right":__Expr__}
 *       {"":"<", "left":__Expr__, "right":__Expr__}
 *       {"":"<=", "left":__Expr__, "right":__Expr__}
 *       {"":"=<", "left":__Expr__, "right":__Expr__}
 * </pre>
 */
public class PathChooser {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** 主観モードのタグ。デフォルトは null */
    Term subjectiveMode ;
    /** ルールオブジェクト (これから拡張するため)*/
    Object modifyRule ;

    //----------------------------------------------------------------------
    /**
     * constractor.
     */
    public PathChooser() {
        subjectiveMode = null ;
        modifyRule = null ;
    }

    //----------------------------------------------------------------------
    /**
     * リンクコストの計算。
     */
    public double calcLinkCost(MapLink link, MapNode fromNode) {
        if(!link.isAvailableFrom(fromNode)) {
            return Double.POSITIVE_INFINITY ;
        } else {
            return 1.0 * link.getLength() ;
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
