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
import java.util.Collection ;

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
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * NetworkParts の prefix の規定値
     */
    public static String DefaultPartIdPrefix = "_p" ;

    /**
     * NetworkParts の id 部の桁数の規定値
     */
    public static int DefaultPartIdDigit = 5 ;

    /**
     * NetworkParts の suffix の規定値
     */
    public static String DefaultPartIdSuffix = "" ;

    /**
     * NetworkParts の id counter
     */
    public static int PartIdCounter = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ID から NetworkParts を取り出すための table.
     */
    private UniqIdObjectTable<OBNode> partTable =
        new UniqIdObjectTable<OBNode>(DefaultPartIdPrefix,
                                      DefaultPartIdDigit,
                                      DefaultPartIdSuffix) ;

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
     * IDテーブルのidの重複チェック
     * @param id : part の id.
     * @return id がすでに登録されていれば true
     */
    public boolean checkObjectId(String id) {
        return !partTable.isUniqId(id) ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の取得
     * @return 新しい id
     */
    protected String assignNewId() {
        return partTable.getUniqId() ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の取得
     * @param prefix : id の prefix
     * @return 新しい id
     */
    protected String assignNewId(String prefix) {
        return partTable.getUniqId(prefix) ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の取得
     * @param prefix : id の prefix
     * @param digit : id 数値部分の桁数
     * @param suffix : id の suffix
     * @return 新しい id
     */
    protected String assignNewId(String prefix, String suffix) {
        return partTable.getUniqId(prefix, suffix) ;
    }

    //------------------------------------------------------------
    /**
     * IDテーブルへのNetworkParts(OBNode)の登録
     * @param id : part の id.
     * @param part : 登録するpart.
     */
    public void addObject(String id, OBNode part) {
        partTable.put(id, part);
    }

    //------------------------------------------------------------
    /**
     * IDテーブルからのNetworkParts(OBNode)の削除
     * @param id : 削除するpart の id.
     */
    public void removeObject(String id) {
        partTable.remove(id) ;
    }

    //------------------------------------------------------------
    /**
     * IDテーブルからNetworkPartsの取り出し。
     * @param id : 取り出す part の id.
     * @return 取り出した part. もしなければ、null。
     */
    public OBNode getObject(String id) {
        return partTable.get(id);
    }

    //------------------------------------------------------------
    /**
     * IDテーブルの中身(NetworkParts)を取り出す。
     * NetworkMap から移動。
     * @return NetworkParts の ArrayList。
     */
    public ArrayList<OBNode> getOBElements() {
        return new ArrayList<OBNode>(partTable.values());
    }

    //------------------------------------------------------------
    /**
     * IDテーブルの中身(NetworkParts)を取り出す。Collectionとして返す。
     * @return NetworkParts の Collection
     */
    public Collection<OBNode> getOBCollection() {
        return partTable.values() ;
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

    //------------------------------------------------------------
    /**
     * マップに整合性があるかチェック。
     * @return 整合性がなければ false。
     */
    public boolean checkConsistency() {
        int counter = 0;
        for (MapLink link : getLinks()) {
            if (!getNodes().contains(link.getFrom())) {
                Itk.logWarn("link's fromNode does not exist.",
                            link, link.getFrom()) ;
                counter += 1;
            } else if (!getNodes().contains(link.getTo())) {
                Itk.logWarn("link's toNode does not exist.",
                            link, link.getTo()) ;
                counter += 1;
            }
        }
        if (counter > 0) {
            Itk.logWarn("EvacuationSimulator invalid links: ", counter);
            return false ;
        } else {
            return true ;
        }
    }

} // class NetworkMapBase

