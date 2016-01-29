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
import nodagumi.ananPJ.navigation.CalcPath.PathGuideInfo;
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
    static public class Result extends LinkedHashMap<MapNode, PathGuideInfo> {}

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
        for (MapNode subgoal : subgoals) {
            double cost = chooser.initialCost(subgoal);
            PathGuideInfo pgInfo =
                new PathGuideInfo(subgoal, null, null, cost) ;
            frontier.put(subgoal, pgInfo) ;
            result.put(subgoal, pgInfo) ;
        }

        // 探索ループ。
        while (true) {
            PathGuideInfo bestPgInfo =
                new PathGuideInfo(null, null, null, Double.POSITIVE_INFINITY) ;
            closedList.clear() ;
            for (MapNode frontierNode : frontier.keySet()) {
                int countPerNode = 0 ;
                for (MapLink preLink :
                         frontierNode.getValidReverseLinkTable()) {
                    MapNode preNode = preLink.getOther(frontierNode);
                    if (result.containsKey(preNode)) continue;
                    countPerNode ++ ;
                    double dist =
                        result.get(frontierNode).distance
                        + chooser.evacuationPathCost(preLink) ;
                    if (dist < bestPgInfo.distance) {
                        bestPgInfo.set(preNode, preLink, frontierNode, dist) ;
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
            if (bestPgInfo.fromNode == null) {
                break;
            }
            // 新しノードを frontier に追加。
            frontier.put(bestPgInfo.fromNode, bestPgInfo) ;
            result.put(bestPgInfo.fromNode, bestPgInfo) ;
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
            public double evacuationPathCost(MapLink link) {
                //if (link.isStair()) return 5.0;
                return 1.0 * link.getLength() ;
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
