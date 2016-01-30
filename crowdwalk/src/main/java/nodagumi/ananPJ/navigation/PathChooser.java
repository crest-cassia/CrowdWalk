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
 *     "subjective_map_rules" : { "modeName1" : __Rule__,
 *                                "modeName2" : __Rule__,
 *                                ... },
 *   ...}
 * </pre>
 *   __Rule__ は、Agent の ThinkEngine の ThinkFormula に準拠。
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
