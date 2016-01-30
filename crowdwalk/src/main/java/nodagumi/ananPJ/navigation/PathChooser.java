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

//======================================================================
/**
 * 探索において、各リンクおよびゴールノードのコストを計算する。
 */
public class PathChooser {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** 主観モードのタグ。デフォルトは null */
    String subjectiveMode ;
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
    public double calcLinkCost(MapLink link) {
        return 1.0 * link.getLength() ;
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
