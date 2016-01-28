// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;

import nodagumi.Itk.Itk;

public class Dijkstra {
    static public class Result extends LinkedHashMap<MapNode, NodeLinkLen> {}

    static public Result calc(MapNodeTable subgoals,
            PathChooser chooser) {
        Result frontier = new Result();

        int count = 0;
        for (MapNode subgoal : subgoals) {
            double cost = chooser.initialCost(subgoal);
            frontier.put(subgoal, new NodeLinkLen(null, null, cost));
            //System.err.print(subgoal + ".");
            ++count;
        }
        //System.err.println(count + "subgoals");

        while (true) {
            double minLength = Double.POSITIVE_INFINITY;
            MapNode bestNode = null;
            MapNode pred = null;
            MapLink bestNext = null;
            for (MapNode frontierNode : frontier.keySet()) {
                for (MapLink nextLink : frontierNode.getValidReverseLinkTable()) {
                    MapNode other_node = nextLink.getOther(frontierNode);
                    if (frontier.containsKey(other_node)) continue;
                    double len = frontier.get(frontierNode).len + 
                        nextLink.getLength() * chooser.evacuationRouteCost(nextLink);
                    if (len < minLength) {
                        minLength = len;
                        bestNode =  other_node;
                        bestNext = nextLink;
                        pred = frontierNode;
                    }
                }
            }
            if (null == bestNode) {
                break;
            }
            frontier.put(bestNode, new NodeLinkLen(pred, bestNext, minLength));
            // System.err.println("Dijkstra new frontier: " +
                        // bestNode.getTags());
            // System.err.print(".");
            ++count;
        }
        //System.err.print(count);
        return frontier;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 標準の PathChooser。
     * これ以外の Chooser を使う機会があるのか、不明。
     */
    public static PathChooser DefaultPathChooser =
        new PathChooser() {
            public double evacuationRouteCost(MapLink link) {
                //if (link.isStair()) return 5.0;
                return 1.0;
            }
            public boolean isExit(MapLink link) {
                return false;
            }
            public double initialCost(MapNode node) {
                return 0.0;
            }
        } ;
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
