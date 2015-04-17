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
import java.util.HashMap ;

import java.io.Serializable;

import javax.swing.tree.DefaultTreeModel;

import nodagumi.ananPJ.NetworkParts.OBNode;
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
     * ID から NetworkParts を取り出すための table.
     */
    protected HashMap<Integer, OBNode> id_part_map =
        new HashMap<Integer, OBNode>();

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
    private HashMap<String, Boolean> validRouteKeys;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NetworkMapBase() {
        super(null, true);
        validRouteKeys = new HashMap<String, Boolean>();;
    }

    //------------------------------------------------------------
    /**
     * IDテーブルへのNetworkParts(OBNode)の登録
     * @param id : part の id.
     * @param part : 登録するpart.
     */
    public void addObject(int id, OBNode part) {
        // 重複登録であれば警告
        if(id_part_map.containsKey(id)) {
            Itk.logWarn("duplicated ID", "id=", id, "part=", part) ;
        }

        id_part_map.put(id, part);
    }

    //------------------------------------------------------------
    /**
     * IDテーブルへのNetworkParts(OBNode)の登録
     * @param id : part の id.
     * @param part : 登録するpart.
     */
    public void removeObject(int id) {
        id_part_map.remove(id) ;
    }

    //------------------------------------------------------------
    /**
     * IDテーブルからNetworkPartsの取り出し。
     * @param id : 取り出す part の id.
     * @return 取り出した part. もしなければ、null。
     */
    public OBNode getObject(int id) {
        return id_part_map.get(id);
    }

    //------------------------------------------------------------
    /**
     * IDテーブルの中身(NetworkParts)を取り出す。
     * NetworkMap から移動。
     * @return NetworkParts の ArrayList。
     */
    public ArrayList<OBNode> getOBElements() {
        return new ArrayList<OBNode>(id_part_map.values());
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
     * 経路探索されているかどうかのチェック
     */
    public boolean isCheckedRouteKey(String target) { 
        return validRouteKeys.containsKey(target); 
    }

    //------------------------------------------------------------
    /**
     * 経路探索されているかどうかのチェック
     */
    public boolean isValidRouteKey(String target) { 
        if(isCheckedRouteKey(target)) {
            return validRouteKeys.get(target);
        } else {
            return false ;
        }
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
            validRouteKeys.put(goal_tag, false) ;
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
            validRouteKeys.put(goal_tag, true);
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

