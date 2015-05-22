// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.Nodes;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;

public class Dijkstra {
    static public class Result extends HashMap<MapNode, NodeLinkLen> {}

    static public Result calc(Nodes subgoals,
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
                // System.err.println("Dijkstra frontierNode: " +
                        // frontierNode.getTags());
                for (MapLink nextLink : frontierNode.getPathwaysReverse()) {
                //for (MapLink nextLink : frontierNode.getPathways()) {
                    MapNode other_node = nextLink.getOther(frontierNode);
                    // System.err.println("Dijkstra nextLink " + nextLink.ID +
                            // " other: " + other_node.getTags() +
                            // " frontierNode: " + frontierNode.getTags());
                    if (frontier.containsKey(other_node)) continue;
                    double len = frontier.get(frontierNode).len + 
                        nextLink.length * chooser.evacuationRouteCost(nextLink);
                    // System.err.println("Dijkstra nextLink " + nextLink.ID +
                            // " other: " + other_node.getTags() +
                            // " frontierNode: " + frontierNode.getTags() +
                            // " len: " + len);
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
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
