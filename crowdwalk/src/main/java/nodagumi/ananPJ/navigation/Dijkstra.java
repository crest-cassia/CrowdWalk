// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;

import nodagumi.Itk.Itk;

//======================================================================
/**
 * Dijkstra法で最短経路探索。
 */
public class Dijkstra {
    //============================================================
    /**
     * 探索結果の格納用クラス。
     */
    static public class Result extends LinkedHashMap<MapNode, NodeLinkLen> {}

    //============================================================
    //------------------------------------------------------------
    /**
     * 探索メインルーチン。(obsolete)
     * @param subgoals 目標とするゴール集合。
     * @param chooser パスを選ぶ際の距離の調整ツール。
     * @return 探索結果。Resule class のインスタンス。
     */
    static public Result calc0(MapNodeTable subgoals,
                              PathChooser chooser) {
        //Itk.timerStart("calc") ;

        Result frontier = new Result();

        // ゴールノードは、initial cost で。
        int count = 0;
        for (MapNode subgoal : subgoals) {
            double cost = chooser.initialCost(subgoal);
            frontier.put(subgoal, new NodeLinkLen(null, null, cost));
            ++count;
        }

        // 探索ループ。
        while (true) {
            double minLength = Double.POSITIVE_INFINITY;
            MapNode bestNode = null;
            MapNode pred = null;
            MapLink bestNext = null;
            for (MapNode frontierNode : frontier.keySet()) {
                for (MapLink nextLink :
                         frontierNode.getValidReverseLinkTable()) {
                    MapNode other_node = nextLink.getOther(frontierNode);
                    if (frontier.containsKey(other_node)) continue;
                    double len =
                        frontier.get(frontierNode).len
                        + (nextLink.getLength()
                           * chooser.evacuationRouteCost(nextLink));
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
            ++count;
        }

        //Itk.timerShowLap("calc") ;
        
        return frontier;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * 探索メインルーチン。by I.Noda
     * 上記の calc は、無駄が多いと思われる。何度も同じノードを展開している。
     * 下記はそれを避ける。
     * @param subgoals 目標とするゴール集合。
     * @param chooser パスを選ぶ際の距離の調整ツール。
     * @return 探索結果。Resule class のインスタンス。
     */
    static public Result calc(MapNodeTable subgoals,
                              PathChooser chooser) {
        //Itk.timerStart("calc") ;

        Result frontier = new Result();
        ArrayList<MapNode> closedList = new ArrayList<MapNode>() ;
        Result result = new Result() ;

        // ゴールノードは、initial cost で。
        int count = 0;
        for (MapNode subgoal : subgoals) {
            double cost = chooser.initialCost(subgoal);
            NodeLinkLen nll = new NodeLinkLen(null, null, cost) ;
            frontier.put(subgoal, nll) ;
            result.put(subgoal, nll) ;
            ++count;
        }

        // 探索ループ。
        while (true) {
            double minLength = Double.POSITIVE_INFINITY;
            MapNode bestNode = null;
            MapNode pred = null;
            MapLink bestNext = null;
            closedList.clear() ;
            for (MapNode frontierNode : frontier.keySet()) {
                int countPerNode = 0 ;
                for (MapLink nextLink :
                         frontierNode.getValidReverseLinkTable()) {
                    MapNode other_node = nextLink.getOther(frontierNode);
                    if (result.containsKey(other_node)) continue;
                    countPerNode ++ ;
                    double len =
                        result.get(frontierNode).len
                        + (nextLink.getLength()
                           * chooser.evacuationRouteCost(nextLink));
                    if (len < minLength) {
                        minLength = len;
                        bestNode =  other_node;
                        bestNext = nextLink;
                        pred = frontierNode;
                    }
                }
                // すべてのリンク先がすでに探査済みであれば、そのノードは close
                if(countPerNode == 0) {
                    closedList.add(frontierNode) ;
                }
            }
            // close されたノードを frontiner から除去。
            for(MapNode node : closedList) {
                frontier.remove(node) ;
            }
            // 新たに付け加えるものがなければ、探査終わり。
            if (null == bestNode) {
                break;
            }
            // 新しノードを frontier に追加。
            NodeLinkLen nll = new NodeLinkLen(pred, bestNext, minLength) ;
            frontier.put(bestNode, nll) ;
            result.put(bestNode, nll) ;
            ++count;
        }

        //Itk.timerShowLap("calc") ;

        return result ;
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
            public double initialCost(MapNode node) {
                return 0.0;
            }
        } ;
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
