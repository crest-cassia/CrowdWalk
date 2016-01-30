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

package nodagumi.ananPJ.NetworkMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap ;
import java.util.LinkedHashMap ;
import java.util.Collection ;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.Enumeration;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.JOptionPane;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.GuiSimulationEditorLauncher;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkMap.OBNode.NType;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle;
import nodagumi.ananPJ.NetworkMap.NetworkMapPartsNotifier;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.navigation.Dijkstra;
import nodagumi.ananPJ.navigation.NavigationHint;

import nodagumi.Itk.*;

//======================================================================
/**
 * link と node のテーブルのみを持つクラス。
 * links と nodes をセットで受け渡すメソッドが非常に多い。
 * なので、まとめておくものを作っておく。
 */
public class NetworkMap extends DefaultTreeModel {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * NetworkMapParts の prefix の規定値
     */
    public static String DefaultPartIdPrefix = "_p" ;

    /**
     * NetworkMapParts の id 部の桁数の規定値
     */
    public static int DefaultPartIdDigit = 5 ;

    /**
     * NetworkMapParts の suffix の規定値
     */
    public static String DefaultPartIdSuffix = "" ;

    /**
     * NetworkMapParts の id counter
     */
    public static int PartIdCounter = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ID から NetworkMapParts を取り出すための table.
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

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 画面変更の notifier
     */
    private NetworkMapPartsNotifier notifier;

    //============================================================
    /**
     * class for undo related stuff
     */
    class UndoInformation {
        public boolean addition;
        public OBNode parent;
        public OBNode node;

        public UndoInformation(OBNode _parent,
                               OBNode _node,
                               boolean _addition) {
            parent = _parent;
            node = _node;
            addition = _addition;
        }
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * updo stack
     */
    private ArrayList<UndoInformation> undo_list =
        new ArrayList<UndoInformation>();

    /**
     * check updo stack
     */
    public boolean canUndo() {
        return undo_list.size() > 0;

    }

    /**
     * exec undo
     */
    public void undo(GuiSimulationEditorLauncher editor) {
        if (undo_list.size() == 0) return;
        int i = undo_list.size() - 1;
        UndoInformation info = undo_list.remove(i);
        if (info.addition) {
            removeOBNode(info.parent, info.node, false);
        } else {
            insertOBNode(info.parent, info.node, false);
        }
        editor.updateAll();
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 編集用画面関係
     */
    private ArrayList<EditorFrame> frames = new ArrayList<EditorFrame>();

    //------------------------------------------------------------
    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NetworkMap() {
        super(null, true);
        String id = assignNewId();
        root = new MapPartGroup(id);
        ((MapPartGroup)root).addTag("root");
        addObject(id, (OBNode)root);

        setRoot((DefaultMutableTreeNode)root);

        validRouteKeys = new HashMap<String, Boolean>();;

        notifier = new NetworkMapPartsNotifier(this);
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
     * @param suffix : id の suffix
     * @return 新しい id
     */
    protected String assignNewId(String prefix, String suffix) {
        return partTable.getUniqId(prefix, suffix) ;
    }

    //------------------------------------------------------------
    /**
     * IDテーブルへのNetworkMapParts(OBNode)の登録
     * NetworkMap での処理に加え、part に NetworkMap 自身を登録する。
     * @param id : part の id.
     * @param part : 登録するpart.
     */
    public void addObject(String id, OBNode part) {
        partTable.put(id, part);
        part.setNetworkMap(this);
    }

    //------------------------------------------------------------
    /**
     * IDテーブルからのNetworkMapParts(OBNode)の削除
     * @param id : 削除するpart の id.
     */
    public void removeObject(String id) {
        partTable.remove(id) ;
    }

    //------------------------------------------------------------
    /**
     * IDテーブルからNetworkMapPartsの取り出し。
     * @param id : 取り出す part の id.
     * @return 取り出した part. もしなければ、null。
     */
    public OBNode getObject(String id) {
        return partTable.get(id);
    }

    //------------------------------------------------------------
    /**
     * OBNode の挿入
     */
    private void insertOBNode(OBNode parent, OBNode node,
                              boolean can_undo) {
        insertOBNode(parent, node) ;
        if (can_undo) {
            undo_list.add(new UndoInformation(parent, node, true));
        }
    }

    /**
     * OBNode の挿入
     */
    private void insertOBNode (OBNode parent, OBNode node) {
        addObject(node.ID, node);
        insertNodeInto(node, parent, parent.getChildCount());

        OBNode.NType type = node.getNodeType();
        if (type == OBNode.NType.NODE) {
            nodesCache.add((MapNode)node);
        } else if (type == OBNode.NType.LINK) {
            linksCache.add((MapLink)node);
        } else if (type == OBNode.NType.AGENT) {
            Itk.logWarn("insert Agent OBNode is obsolete.", node) ;
        } else if (type == OBNode.NType.GROUP) {
            /* no operation */
        } else if (type == OBNode.NType.AREA) {
            /* no operation */
        } else if (type == OBNode.NType.SYMLINK) {
            /* no operation */
        } else {
            Itk.logError("unkown type added");
        }

    }

    //------------------------------------------------------------
    /**
     * OBノード削除
     */
    public void removeOBNode(OBNode parent,
                             OBNode node,
                             boolean can_undo) {
        removeOBNode(parent, node) ;
        if (can_undo) {
            undo_list.add(new UndoInformation(parent, node, false));
        }
    }

    /**
     * OBノード削除
     */
    public void removeOBNode (OBNode parent, OBNode node) {
        removeObject(node.ID);

        OBNode.NType type = node.getNodeType();
        if (type != OBNode.NType.SYMLINK) {
            clearSymlinks(node);
        }

        boolean linkRemoved = false;
        switch (type) {
        case NODE:
            nodesCache.remove((MapNode)node);
            break;
        case LINK:
            linksCache.remove((MapLink)node);
            linkRemoved = true;
            break;
        case AGENT:
            Itk.logWarn("remove Agent OBNode is obsolete") ;
            break;
        case GROUP:
            while (node.getChildCount() > 0) {
                removeOBNode(node, (OBNode)node.getChildAt(0), true);
            }
            break;
        case AREA:
            break;
        case SYMLINK:
            break;
        default:
            System.err.println(type);
            System.err.println("unkown type removed");
        }
        removeNodeFromParent(node);

        if (linkRemoved) {
            notifier.linkRemoved((MapLink)node);
        }
    }

    //------------------------------------------------------------
    /**
     * ノード作成
     */
    public MapNode createMapNode(MapPartGroup parent,
                                 Point2D _coordinates,
                                 double _height) {
        String id = assignNewId();
        MapNode node = new MapNode(id,
                                   _coordinates, _height);
        insertOBNode(parent, node, true);
        return node;
    }

    //------------------------------------------------------------
    /**
     * リンク作成
     */
    public MapLink createMapLink(MapPartGroup parent,
                                 MapNode _from,
                                 MapNode _to,
                                 double _length,
                                 double _width) {
        String id = assignNewId();
        MapLink link = new MapLink(id, _from, _to, _length, _width);
        link.prepareAdd();
        insertOBNode(parent, link, true);
        return link;
    }

    //------------------------------------------------------------
    /**
     * エリア作成
     */
    public MapArea createMapAreaRectangle(
                                          MapPartGroup parent,
                                          Rectangle2D bounds,
                                          double min_height,
                                          double max_height,
                                          double angle) {
        String id = assignNewId();
        MapArea area = new MapAreaRectangle(id,
                                            bounds, min_height, max_height, angle);
        insertOBNode(parent, area, true);
        return area;
    }

    //------------------------------------------------------------
    /**
     * グループノード作成
     */
    public MapPartGroup createGroupNode(MapPartGroup parent){
        String id = assignNewId();
        MapPartGroup group = new MapPartGroup(id);
        insertOBNode(parent, group, true);
        return group;
    }

    /**
     * グループノード作成
     */
    private MapPartGroup createGroupNode(){
        return createGroupNode((MapPartGroup)root);
    }

    //------------------------------------------------------------
    /**
     * SymLink
     */
    public OBNodeSymbolicLink createSymLink(MapPartGroup parent,
                                            OBNode orig){
        String id = assignNewId();
        OBNodeSymbolicLink symlink = new OBNodeSymbolicLink(id, orig);
        insertOBNode(parent, symlink, true);
        return symlink;
    }

    /**
     * SymLink
     */
    public void clearSymlinks(final OBNode orig) {
        applyToAllChildrenRec((OBNode)root,
                              null,
                              new OBTreeCrawlFunctor() {
                                  @Override
                                  public void apply(OBNode node, OBNode parent) {
                                      if (node.getNodeType() ==
                                          OBNode.NType.SYMLINK) {
                                          OBNodeSymbolicLink symlink =
                                              (OBNodeSymbolicLink)node;
                                          if (symlink.getOriginal() == orig) {
                                              Itk.logInfo("deleted!");
                                              removeOBNode(parent, node, true);
                                          }
                                      }
                                  }
                              });
    }

    //============================================================
    /**
     * tree のクローラ操作クラス
     */
    protected static abstract class OBTreeCrawlFunctor {
        public abstract void apply(OBNode node,
                                   OBNode parent);
    }

    //------------------------------------------------------------
    /**
     * tree のクローラ操作
     */
    private void applyToAllChildrenRec(OBNode node,
                                       OBNode parent,
                                       OBTreeCrawlFunctor func) {
        func.apply(node, parent);
        for (int i = 0; i < node.getChildCount(); ++i) {
            OBNode child = (OBNode)node.getChildAt(i);
            if (child instanceof OBNode) {
                applyToAllChildrenRec(child, node, func);
            }
        }
    }


    //------------------------------------------------------------
    /**
     * IDテーブルの中身(NetworkMapParts)を取り出す。
     * NetworkMap から移動。
     * @return NetworkMapParts の ArrayList。
     */
    public ArrayList<OBNode> getOBElements() {
        return new ArrayList<OBNode>(partTable.values());
    }

    //------------------------------------------------------------
    /**
     * IDテーブルの中身(NetworkMapParts)を取り出す。Collectionとして返す。
     * @return NetworkMapParts の Collection
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
     * グループ
     */
    public ArrayList<MapPartGroup> getGroups() {
        ArrayList<MapPartGroup> groups = new ArrayList<MapPartGroup>();
        for (OBNode node : getOBCollection()) {
            if (node.getNodeType() == OBNode.NType.GROUP &&
                !node.getTags().isEmpty()) {
                groups.add((MapPartGroup)node);
            }
        }
        Collections.sort(groups, new Comparator<MapPartGroup>() {
                public int compare(MapPartGroup lhs, MapPartGroup rhs) {
                    Matcher f1 = lhs.matchTag("((B?)\\d+)F");
                    Matcher f2 = rhs.matchTag("((B?)\\d+)F");

                    if (f1 != null && f2 != null) {
                        int i1;
                        if (f1.group(1).startsWith("B")) {
                            i1 = -Integer.parseInt(f1.group(1).substring(1));
                        } else {
                            i1 = Integer.parseInt(f1.group(1));
                        }
                        int i2;
                        if (f2.group(1).startsWith("B")) {
                            i2 = -Integer.parseInt(f2.group(1).substring(1));
                        } else {
                            i2 = Integer.parseInt(f2.group(1));
                        }

                        return i1 - i2;
                    } else if (f1 != null) {
                        return -1;
                    } else if (f2 != null) {
                        return 1;
                    }

                    String tc1Name = lhs.getTagString();
                    String tc2Name = rhs.getTagString();
                    if (tc1Name.compareTo(tc2Name) < 0) {
                        return -1;
                    } else if (tc1Name.compareTo(tc2Name) > 0) {
                        return 1;
                    }
                    return 0;
                }
            });
        return groups;
    }

    //------------------------------------------------------------
    /**
     * 領域
     */
    public ArrayList<MapArea> getAreas() {
        ArrayList<MapArea> areas = new ArrayList<MapArea>();
        findAreaRec((OBNode)root, areas);
        return areas;
    }

    /**
     * 領域
     */
    private void findAreaRec(OBNode node, ArrayList<MapArea> areas) {
        if (node.getNodeType() == NType.AREA) {
            areas.add((MapArea)node);
        }
        for (int i = 0; i < node.getChildCount(); ++i) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof OBNode) findAreaRec((OBNode)child, areas);
        }
    }

    //------------------------------------------------------------
    /**
     * 階段作成
     */
    public void makeStairs() {
        MapNodeTable selected_nodes = new MapNodeTable();
        for (MapNode node : getNodes()) {
            if (node.selected) selected_nodes.add(node);
        }
        if (selected_nodes.size() != 2) {
            JOptionPane.showMessageDialog(null,
                                          "Number of selected nodes:"
                                          + selected_nodes.size() + "\nwhere it should be 2",
                                          "Fail to make stair",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        MapNode from_node = selected_nodes.get(0);
        MapNode to_node = selected_nodes.get(1);

        if (from_node.getHeight() < to_node.getHeight()) {
            from_node = selected_nodes.get(1);
            to_node = selected_nodes.get(0);
        }
        MapLink link = createMapLink((MapPartGroup)from_node.getParent(),
                                     from_node, to_node,    100, 6);
        link.addTag("GENERATED_STAIR");

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
     * @return 探索成功した結果。すでにノードには情報は格納されている。
     */
    public Dijkstra.Result calcGoalPath(String goal_tag) {
        MapNodeTable goals = new MapNodeTable();
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
            return null ;
        }
        Itk.logInfo("Found Goal", goal_tag) ;

        Dijkstra.Result result =
            Dijkstra.calc(goals,
                          Dijkstra.DefaultPathChooser) ;

        synchronized(getNodes()) {
            validRouteKeys.put(goal_tag, true);
            for (MapNode node : result.keySet()) {
                NavigationHint hint = result.get(node);
                node.addNavigationHint(goal_tag, hint) ;
            }
        }
        return result ;
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
                return (null != calcGoalPath(goal_tag)) ;
            }
        }
    }

    /**
     * 経路探索結果を JSON 形式でファイルに保存する.
     *
     * <pre>
     * 保存ファイル名: {@code "routes_<ジェネレーションファイル名>@<マップファイル名>.json"}
     * 書式:
     * {
     *     "mapFile": {
     *         "fileName": 経路探索時に使用したマップファイル名,
     *         "md5": ファイルの MD5(Hex形式)
     *     },
     *     "generationFile": {
     *         "fileName": 経路探索時に使用したジェネレーションファイル名,
     *         "md5": ファイルの MD5(Hex形式)
     *     },
     *     "validRouteKeys": {
     *         ノードタグ/リンクタグ: true/false, ...
     *     },
     *     "nodeHints": [
     *         {
     *             "ID": ノードID,
     *             "hints": {
     *                 ノードタグ/リンクタグ: {
     *                     "exit": ノードID,
     *                     "way": リンクID,
     *                     "distance": 距離(m)
     *                 }, ...
     *             }
     *         }, ...
     *     ]
     * }
     * </pre>
     */
    public void saveRoutes(CrowdWalkPropertiesHandler properties) {
        LinkedHashMap routes = new LinkedHashMap();
        String mapFileName = new File(properties.getNetworkMapFile()).getName();
        String generationFileName = new File(properties.getGenerationFile()).getName();

        HashMap mapFile = new HashMap();
        mapFile.put("fileName", mapFileName);
        mapFile.put("md5", md5Hex(properties.getNetworkMapFile()));
        routes.put("mapFile", mapFile);

        HashMap generationFile = new HashMap();
        generationFile.put("fileName", generationFileName);
        generationFile.put("md5", md5Hex(properties.getGenerationFile()));
        routes.put("generationFile", generationFile);

        routes.put("validRouteKeys", validRouteKeys);

        ArrayList<LinkedHashMap> nodeHints = new ArrayList();
        for (MapNode node : getNodes()) {
            LinkedHashMap nodeHint = new LinkedHashMap();
            nodeHint.put("ID", node.ID);
            LinkedHashMap hints = new LinkedHashMap();
            for (String goal_tag : node.getHints().keySet()) {
                LinkedHashMap navigationHint = new LinkedHashMap();
                NavigationHint hint = node.getHints().get(goal_tag);
                if (hint.toNode == null) {
                    navigationHint.put("exit", null);
                    navigationHint.put("way", null);
                    navigationHint.put("distance", 0.0);
                } else {
                    navigationHint.put("exit", hint.toNode.ID);
                    navigationHint.put("way", hint.viaLink.ID);
                    navigationHint.put("distance", hint.distance);
                }
                hints.put(goal_tag, navigationHint);
            }
            nodeHint.put("hints", hints);
            nodeHints.add(nodeHint);
        }
        routes.put("nodeHints", nodeHints);

        String dirPath = properties.getPropertiesDir().replaceAll("\\\\", "/");
        String filePath = dirPath + "/routes_" + generationFileName + "@" + mapFileName + ".json";
        Itk.logInfo("Save Routes File", filePath) ;
        try {
            JSON.encode(routes, new FileWriter(filePath), true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * ファイルに保存された経路探索結果を読み込んでセットする
     */
    public void loadRoutes(CrowdWalkPropertiesHandler properties) {
        String routesFilePath = properties.getRoutesFilePath();
        Itk.logInfo("Load Routes", routesFilePath) ;
        HashMap routes = null;
        try {
            routes = JSON.decode(new FileReader(routesFilePath), HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // マップファイル、ジェネレーションファイルの内容が一致しない場合は警告を出す
        HashMap<String, Object> fileElement = (HashMap<String, Object>)routes.get("mapFile");
        checkModified(fileElement, properties.getNetworkMapFile());
        fileElement = (HashMap<String, Object>)routes.get("generationFile");
        checkModified(fileElement, properties.getGenerationFile());

        validRouteKeys.clear();
        HashMap<String, Boolean> _validRouteKeys = (HashMap<String, Boolean>)routes.get("validRouteKeys");
        for (Map.Entry<String, Boolean> entry : _validRouteKeys.entrySet()) {
            validRouteKeys.put(entry.getKey(), entry.getValue());
        }

        // アクセス高速化のため
        HashMap<String, MapNode> nodes = new HashMap();
        for (MapNode node : getNodes()) {
            nodes.put(node.ID, node);
        }
        HashMap<String, MapLink> links = new HashMap();
        for (MapLink link : getLinks()) {
            links.put(link.ID, link);
        }

        ArrayList<HashMap<String, Object>> nodeHints = (ArrayList<HashMap<String, Object>>)routes.get("nodeHints");
        for (HashMap<String, Object> nodeHint : nodeHints) {
            String id = (String)nodeHint.get("ID");
            MapNode node = nodes.get(id);
            if (node == null) {
                Itk.logError("Load Routes", "Unknown node ID: " + id);
                System.exit(1);
            }
            node.clearHints();
            HashMap<String, Object> hints = (HashMap<String, Object>)nodeHint.get("hints");
            for (Map.Entry<String, Object> entry : hints.entrySet()) {
                String goal_tag = entry.getKey();
                HashMap<String, Object> hint = (HashMap<String, Object>)entry.getValue();
                MapNode exitNode = null;
                MapLink wayLink = null;
                double distance = 0.0;
                if (hint.get("exit") != null) {
                    exitNode = nodes.get(hint.get("exit"));
                    wayLink = links.get(hint.get("way"));
                    distance = ((java.math.BigDecimal)hint.get("distance")).doubleValue();
                }
                node.addNavigationHint(goal_tag,
                                       new NavigationHint(null, null, null,
                                                          wayLink, exitNode, distance));
            }
        }
    }

    /**
     * ファイルの MD5 を Hex 形式で返す
     */
    public String md5Hex(String filePath) {
        String md5 = null;
        try {
            FileInputStream is = new FileInputStream(filePath);
            md5 = DigestUtils.md5Hex(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return md5;
    }

    /**
     * 経路探索で使用したファイルが同じものであるかどうかのチェック
     */
    public boolean checkModified(HashMap<String, Object> element, String filePath) {
        if (! md5Hex(filePath).equals((String)element.get("md5"))) {
            Itk.logWarn_(
                    coloration("Load Routes", 31),
                    coloration("file modified: " + element.get("fileName") + ", " + filePath, 31));
            return false;
        }
        return true;
    }

    /**
     * コンソール出力用の文字列に色を付ける(ANSI escape code)
     * @param colorCode 30:Black, 31:Red, 32:Green, 33:Yellow, 34:Blue, 35:Magenta, 36:Cyan, 37:White
     */
    public String coloration(String text, int colorCode) {
        return String.format("\033[1;%dm%s\033[00m", colorCode, text);
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

    //------------------------------------------------------------
    /**
     * マップ中の全タグを収集。
     */
    public ArrayList<String> getAllTags() {
        ArrayList<String> all_tags = new ArrayList<String>();
        for (MapNode node : getNodes()) {
            for (String tag : node.getTags()) {
                if (!all_tags.contains(tag))
                    all_tags.add(tag);
            }
        }
        for (MapLink link : getLinks()) {
            for (String tag : link.getTags()) {
                if (!all_tags.contains(tag))
                    all_tags.add(tag);
            }
        }
        return all_tags;
    }

    //------------------------------------------------------------
    /**
     * converting to DOM
     */
    public boolean toDOM(Document doc) {
        Element dom_root =((OBNode)this.root).toDom(doc, "root");

        doc.appendChild(dom_root);

        return true;
    }

    /**
     * converting from DOM
     */
    public boolean fromDOM(Document doc) {
        NodeList toplevel = doc.getChildNodes();
        if (toplevel.getLength() != 1) {
            JOptionPane.showMessageDialog(null,
                                          "The number of networks in the dom was "
                                          + toplevel.getLength(),
                                          "Fail to convert from DOM",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        Element dom_root = (Element) toplevel.item(0);

        setRoot(OBNode.fromDom(dom_root));
        setupNetwork((OBNode)this.root);
        return true;
    }

    //------------------------------------------------------------
    /**
     * ネットワーク設定。
     */
    private void setupNetwork(OBNode ob_node) {
        setupNodes(ob_node);
        setupLinks(ob_node);
        setupOthers(ob_node);

        checkDanglingSymlinks(ob_node);
    }

    //------------------------------------------------------------
    /**
     * ノード設定。
     */
    @SuppressWarnings("unchecked")
    private void setupNodes(OBNode ob_node) {
        if (OBNode.NType.NODE == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            MapNode node = (MapNode) ob_node;

            nodesCache.add(node);
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupNodes(child);
            }
        }
    }

    //------------------------------------------------------------
    /**
     * リンク設定。
     */
    @SuppressWarnings("unchecked")
    private void setupLinks(OBNode ob_node) {
        if (OBNode.NType.LINK == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            MapLink link = (MapLink) ob_node;
            linksCache.add(link);

            String[] nodes = (String[])ob_node.getUserObject();
            MapNode from_node = (MapNode)getObject(nodes[0]);
            MapNode to_node = (MapNode)getObject(nodes[1]);

            if (from_node == null) {
                Itk.logWarn("from_node is null:", nodes[0]) ;
            }
            if (to_node == null) {
                Itk.logWarn("to_node is null:", nodes[1]) ;
            }
            from_node.addLink(link);
            to_node.addLink(link);
            try {
                link.setFromTo(from_node, to_node);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                                              "Try to set from/to of a link, which alread has it setted\n"
                                              + "link ID: " + link.ID,
                                              "Warning setting up network",
                                              JOptionPane.WARNING_MESSAGE);
            }
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupLinks(child);
            }
        }
    }

    //------------------------------------------------------------
    /**
     * その他設定。
     */
    @SuppressWarnings("unchecked")
    private void setupOthers(OBNode ob_node) {
        if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            for (Enumeration<OBNode> e = ob_node.children(); e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupOthers(child);
            }
        } else if (ob_node.getNodeType() == OBNode.NType.SYMLINK) {
            addObject(ob_node.ID, ob_node);
            String orig_id = (String)ob_node.getUserObject();
            OBNode original = (OBNode)getObject(orig_id);

            OBNodeSymbolicLink symlink = (OBNodeSymbolicLink)ob_node;
            symlink.setOriginal(original);
        } else if (ob_node.getNodeType() == OBNode.NType.AREA){
            addObject(ob_node.ID, ob_node);
        } else if (ob_node.getNodeType() == OBNode.NType.NODE ||
                   ob_node.getNodeType() == OBNode.NType.LINK ||
                   ob_node.getNodeType() == OBNode.NType.AGENT
                   ){
        } else {
            Itk.logError("unknown type " + ob_node.getNodeType() + " in setting up network");
        }
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    private void checkDanglingSymlinks(OBNode node) {
        class CheckSymlink extends OBTreeCrawlFunctor {
            public int dangling_symlink_count = 0;
            boolean found = false;
            @Override
            public void apply(OBNode node,
                              OBNode parent) {
                if (node.getNodeType() == OBNode.NType.SYMLINK) {
                    OBNode original = ((OBNodeSymbolicLink)node).getOriginal();
                    if (original == null) {
                        dangling_symlink_count++;
                        found = true;
                        removeOBNode(parent, node, false);
                    }
                }
            }

            public boolean mustCheckMore() {
                boolean ret = found;
                found = false;
                return ret;
            }
        }
        CheckSymlink checker = new CheckSymlink();
        do {
            applyToAllChildrenRec(node, null, checker);
        } while(checker.mustCheckMore());
        if (checker.dangling_symlink_count != 0) {
            JOptionPane.showMessageDialog(null,
                                          "Removed " + checker.dangling_symlink_count
                                          + " dangling symlink(s) on loading",
                                          "Corrupt file",
                                          JOptionPane.WARNING_MESSAGE
                                          );
        }

    }

    //------------------------------------------------------------
    /**
     * NetworkMap の構成要素の状態変化を監視・通知するオブジェクトを返す.
     */
    public NetworkMapPartsNotifier getNotifier() { return notifier; }

    //------------------------------------------------------------
    /**
     * Editor Frame
     */
    public boolean existNodeEditorFrame(MapPartGroup _obiNode){
        for (EditorFrame frame : getFrames()) {
            if (_obiNode.equals(frame)) return true;
        }
        return false;
    }

    /**
     * Editor Frame
     */
    public EditorFrame openEditorFrame(GuiSimulationEditorLauncher editor,
                                       MapPartGroup obinode) {
        EditorFrame frame = new EditorFrame(editor, obinode);

        obinode.setUserObject(frame);

        getFrames().add(frame);
        frame.setVisible(true);

        return frame;
    }

    /**
     * Editor Frame
     */
    public void removeEditorFrame(MapPartGroup _obinode){
        getFrames().remove(_obinode.getUserObject());
        _obinode.setUserObject(null);
    }

    //------------------------------------------------------------
    /**
     * フレームセット
     */
    public void setFrames(ArrayList<EditorFrame> frames) {
        this.frames = frames;
    }
    /**
     * フレーム取得
     */
    public ArrayList<EditorFrame> getFrames() {
        return frames;
    }

} // class NetworkMap

