// -*- mode: java; indent-tabs-mode: nil -*-
/** Bare Network Map (only include tables of links and nodes)
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/26 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/26]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ;

import java.util.ArrayList;

import java.io.Serializable;

import javax.swing.tree.DefaultTreeModel;

import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.navigation.CalcPath;
import nodagumi.ananPJ.navigation.Dijkstra;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;

import nodagumi.Itk.*;

//======================================================================
/**
 * link と node のテーブルのみを持つクラス。
 * links と nodes をセットで受け渡すメソッドが非常に多い。
 * なので、まとめておくものを作っておく。
 */
public class NetworkMapBase extends DefaultTreeModel {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ノードテーブル
     */
    protected MapNodeTable nodesCache = new MapNodeTable();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクテーブル
     */
    protected MapLinkTable linksCache = new MapLinkTable();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 経路探索されているかどうかのテーブル。
     */
    private ArrayList<String> validRouteKeys;

    //------------------------------------------------------------
    /**
     * 経路探索されているかどうかのチェック
     */
    public boolean isValidRouteKey(String route) { 
        return validRouteKeys.contains(route); 
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NetworkMapBase() {
        super(null, true);
        validRouteKeys = new ArrayList<String>();;
    }

    //------------------------------------------------------------
    /**
     * ノードテーブル取得
     * @return node table
     */
    public MapNodeTable getNodes() {
        return nodesCache;
    }

    //------------------------------------------------------------
    /**
     * リンクテーブル取得
     * @return link table
     */
    public MapLinkTable getLinks() {
        return linksCache;
    }

    //------------------------------------------------------------
    /**
     * 経路探索
     * @return 探索成功かどうか
     */
    public boolean calcGoalPath(String goal_tag) {
        CalcPath.Nodes goals = new CalcPath.Nodes();
        for (MapNode node : getNodes()) {
            if (node.hasTag(goal_tag)) goals.add(node);
        }
        for (MapLink link : getLinks()) {
            if (link.hasTag(goal_tag)) {
                goals.add(link.getFrom());
                goals.add(link.getTo());
            }
        }
        if (goals.size() == 0) {
            Itk.logWarn("No Goal", goal_tag) ;
            return false;
        }
        Itk.logInfo("Found Goal", goal_tag) ;

        Dijkstra.Result result =
            Dijkstra.calc(goals,
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
                          });

        synchronized(getNodes()) {
            validRouteKeys.add(goal_tag);
            for (MapNode node : result.keySet()) {
                NodeLinkLen nll = result.get(node);
                node.addNavigationHint(goal_tag,
                                       new NavigationHint(nll.node,
                                                          nll.link, nll.len));
            }
        }
        return true;
    }

    //------------------------------------------------------------
    /**
     * synchronized された経路探索
     * @return 探索成功かどうか。goal_tag が探索済みでも true を返す。
     */
    public boolean calcGoalPathWithSync(String goal_tag) {
        synchronized(validRouteKeys) {
            if(isValidRouteKey(goal_tag)) {
                return true ;
            } else {
                return calcGoalPath(goal_tag) ;
            }
        }
    }

} // class NetworkMapBase

