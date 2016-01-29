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

public class CalcPath {
    //======================================================================
    /**
     * あるノードからの距離とルートの情報。
     * fromNode から alongLink に沿って towardNode をたどると、
     * distance で、最終的なゴールに達する、という情報。
     */
    public static class PathGuideInfo {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /** 起点ノード。 */
        public MapNode fromNode ;
        /** 辿るリンク。 */
        public MapLink traceLink ;
        /** リンク上での向かう方向のノード。 */
        public MapNode toNode ;
        /** 最終ゴールまでの距離。 */
        public double distance ; 

        //--------------------------------------------------
        /**
         * constructor
         */ 
        PathGuideInfo(MapNode _fromNode, MapLink _traceLink,
                      MapNode _toNode, double _distance) {
            set(_fromNode, _traceLink, _toNode, _distance) ;
        }

        //--------------------------------------------------
        /**
         * constructor
         */
        public PathGuideInfo set(MapNode _fromNode, MapLink _traceLink,
                                 MapNode _toNode, double _distance) {
            fromNode = _fromNode ;
            traceLink = _traceLink ;
            toNode = _toNode ;
            distance = _distance ;
            return this ;
        }

    }

    public interface PathChooser {
        abstract public double evacuationPathCost(MapLink link);
        abstract public double initialCost(MapNode node);
    }

}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
