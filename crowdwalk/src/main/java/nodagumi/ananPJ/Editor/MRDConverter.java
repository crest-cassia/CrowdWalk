package nodagumi.ananPJ.Editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Point2D;
import org.osgeo.proj4j.CoordinateTransform;

import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.Itk.*;

/**
 * MAPPLE 道路ネットワークデータをマップデータに変換する.
 *
 * 基本道路ノードの接続リンクに付いての仕様の補足:
 * ・通行可能かどうかがリンクの交通規制種別コードで判断できる場合には、通行可/不可にかかわらず交差点通行コードは 1 となる。
 * ・リンクの交通規制種別コードだけでは「通行不可」と判断できない時のみ交差点通行コードは 2 となる。
 * ・カーナビではUターン指示など出さないため、Uターン禁止かどうかの情報はない。Uターンの交差点通行コードは必ず 1 となる。
 * ・隣接メッシュとの接合ノードは必ず末端ノードとなり、接続リンクは一本だけである。
 */
public class MRDConverter {
    /**
     * 旧日本測地系の経緯度座標
     */
    public static final String EPSG = "EPSG:4301";

    /**
     * 接合ノードを示すタグ
     */
    public static final String JOINT_NODE_TAG = "JOINT_NODE";

    /**
     * 道路幅員区分コード表
     */
    public static final double[] WIDTH_TABLE = {
        1.0,        // 0 未調査
        14.0,       // 1 幅員13.0ｍ以上
        9.0,        // 2 幅員5.5ｍ以上～13.0ｍ未満
        5.0,        // 3 幅員4.0ｍ以上～5.5ｍ未満
        3.0         // 4 幅員4.0ｍ未満
    };

    /**
     * 基本道路ノード
     */
    private class BasicRoadNode {
        public String nodeNumber;
        public String nextMeshCode;
        public String nextNodeNumber;
        public int x;
        public int y;
        public ArrayList<String> connectionNodeNumbers = new ArrayList();
        public ArrayList<String> passCodes = new ArrayList();
        public ArrayList<Integer> connectionAngles = new ArrayList();
        public HashMap<String, Object> properties = new HashMap();

        public BasicRoadNode(MRDReader reader) throws IOException {
            nodeNumber = reader.getString("ノード番号");
            x = reader.getInt("X 座標");
            y = reader.getInt("Y 座標");
            nextMeshCode = reader.getString("隣接２次メッシュコード");
            nextNodeNumber = reader.getString("接合ノード番号");
            properties.put("ノード種別コード", reader.getInteger("ノード種別コード"));
            properties.put("接続リンク本数", reader.getInteger("接続リンク本数"));
            for (int index = 1; index <= (Integer)properties.get("接続リンク本数"); index++) {
                connectionNodeNumbers.add(reader.getString("接続ノード番号[" + index + "]"));
                passCodes.add(reader.getString("交差点通行コード[" + index + "]"));
                connectionAngles.add(reader.getInteger("接続角度[" + index + "]"));
            }
            properties.put("信号機有無フラグ", reader.getInteger("信号機有無フラグ"));
        }

        public String getID() {
            return String.format("MRD_%s_%s", meshCode, nodeNumber);
        }
    }

    /**
     * 基本道路リンク
     */
    private class BasicRoadLink {
        public String linkNumber;
        public int length;
        public int widthCode;
        public int nInterpolationPoints;
        public ArrayList<Point2D> interpolationPoints = new ArrayList();
        public HashMap<String, Object> properties = new HashMap();

        public BasicRoadLink(MRDReader reader) throws IOException {
            linkNumber = reader.getString("リンク番号");
            length = reader.getInt("リンク長（計算値）");
            widthCode = reader.getInt("道路幅員区分コード");
            nInterpolationPoints = reader.getInt("補間点総数");
            properties.put("道路種別コード", reader.getInteger("道路種別コード"));
            properties.put("路線番号", reader.getInteger("路線番号"));
            properties.put("リンク種別コード", reader.getInteger("リンク種別コード"));
            properties.put("自動車専用道路コード", reader.getInteger("自動車専用道路コード"));
            properties.put("有料道路コード", reader.getInteger("有料道路コード"));
            properties.put("リンク通行可・不可コード", reader.getInteger("リンク通行可・不可コード"));
            properties.put("補間点総数", reader.getInteger("補間点総数"));
            properties.put("車線数コード", reader.getInteger("車線数コード"));
            properties.put("交通規制種別コード", reader.getInteger("交通規制種別コード"));
            properties.put("分離帯フラグ", reader.getInteger("分離帯フラグ"));
        }

        public void readInterpolationPoints(MRDReader reader) throws IOException {
            int n = nInterpolationPoints - interpolationPoints.size();
            if (n < 0) {
                throw new IOException("Basic road link: " + linkNumber + " Incorrect total number of interpolation points: " + n);
            }
            if (n > 16) {
                n = 16;
            }
            for (int index = 1; index <= n; index++) {
                int x = reader.getInt("X 座標[" + index + "]");
                int y = reader.getInt("Y 座標[" + index + "]");
                interpolationPoints.add(new Point2D(x, y));
            }
        }

        public String getID() {
            return String.format("MRD_%s_%s", meshCode, linkNumber);
        }

        public String getNode1ID() {
            return String.format("MRD_%s_%s", meshCode, getNodeNumber1());
        }

        public String getNode2ID() {
            return String.format("MRD_%s_%s", meshCode, getNodeNumber2());
        }

        public String getNodeNumber1() {
            return linkNumber.substring(0, 4);
        }

        public String getNodeNumber2() {
            return linkNumber.substring(4, 8);
        }
    }

    /**
     * 基本道路リンク、全道路リンク対応データ
     */
    private class BasicAndAllRoadLinkMapping {
        public String linkNumber;
        public int nAllRoadLinkNumbers;
        public ArrayList<String> allRoadLinkNumbers = new ArrayList();

        public BasicAndAllRoadLinkMapping(MRDReader reader) throws IOException {
            linkNumber = reader.getString("基本道路リンク番号");
            nAllRoadLinkNumbers = reader.getInt("全道路リンク総数");
        }

        public void readAllRoadLinkNumbers(MRDReader reader) throws IOException {
            int n = nAllRoadLinkNumbers - allRoadLinkNumbers.size();
            if (n < 0) {
                throw new IOException("BasicAndAllRoadLinkMapping: " + linkNumber + " Incorrect total number of all road link numbers: " + n);
            }
            if (n > 24) {
                n = 24;
            }
            for (int index = 1; index <= n; index++) {
                allRoadLinkNumbers.add(reader.getString("全道路リンク番号[" + index + "]"));
            }
        }

        public String getID() {
            return String.format("MRD_%s_%s", meshCode, linkNumber);
        }

        public String getNode1ID() {
            return String.format("MRD_%s_%s", meshCode, getNodeNumber1());
        }

        public String getNode2ID() {
            return String.format("MRD_%s_%s", meshCode, getNodeNumber2());
        }

        public String getNodeNumber1() {
            return linkNumber.substring(0, 4);
        }

        public String getNodeNumber2() {
            return linkNumber.substring(4, 8);
        }
    }

    /**
     * 全道路ノード
     */
    private class AllRoadNode {
        public String nodeNumber;
        public String nextMeshCode;
        public String nextNodeNumber;
        public int x;
        public int y;
        public HashMap<String, Object> properties = new HashMap();

        public AllRoadNode(MRDReader reader) throws IOException {
            nodeNumber = reader.getString("ノード番号");
            x = reader.getInt("X 座標");
            y = reader.getInt("Y 座標");
            nextMeshCode = reader.getString("隣接２次メッシュコード");
            nextNodeNumber = reader.getString("接合ノード番号");
            properties.put("ノード種別コード", reader.getInteger("ノード種別コード"));
        }

        public String getID() {
            return String.format("MRD_%s_%s", meshCode, nodeNumber);
        }

        public String getBasicNodeID() {
            if (nodeNumber.startsWith("0")) {
                return String.format("MRD_%s_%s", meshCode, nodeNumber.substring(1));
            }
            return null;
        }
    }

    /**
     * 全道路リンク
     */
    private class AllRoadLink {
        public String linkNumber;
        public String basicLinkNumber;
        public int length;
        public int widthCode;
        public int nInterpolationPoints;
        public ArrayList<Point2D> interpolationPoints = new ArrayList();
        public HashMap<String, Object> properties = new HashMap();

        public AllRoadLink(MRDReader reader) throws IOException {
            linkNumber = reader.getString("リンク番号");
            basicLinkNumber = reader.getString("対応基本道リンク番号");
            length = reader.getInt("リンク長（計算値）");
            widthCode = reader.getInt("道路幅員区分コード");
            nInterpolationPoints = reader.getInt("補間点総数");
            properties.put("道路種別コード", reader.getInteger("道路種別コード"));
            properties.put("交通規制種別コード", reader.getInteger("交通規制種別コード"));
            properties.put("補間点総数", reader.getInteger("補間点総数"));
            properties.put("リンク種別コード", reader.getInteger("リンク種別コード"));
            properties.put("経路ランク", reader.getInteger("経路ランク"));
        }

        public void readInterpolationPoints(MRDReader reader) throws IOException {
            int n = nInterpolationPoints - interpolationPoints.size();
            if (n < 0) {
                throw new IOException("All road link: " + linkNumber + " Incorrect total number of interpolation points: " + n);
            }
            if (n > 21) {
                n = 21;
            }
            for (int index = 1; index <= n; index++) {
                int x = reader.getInt("X 座標[" + index + "]");
                int y = reader.getInt("Y 座標[" + index + "]");
                interpolationPoints.add(new Point2D(x, y));
            }
        }

        public String getID() {
            return String.format("MRD_%s_%s", meshCode, linkNumber);
        }

        public String getNode1ID() {
            return String.format("MRD_%s_%s", meshCode, getNodeNumber1());
        }

        public String getNode2ID() {
            return String.format("MRD_%s_%s", meshCode, getNodeNumber2());
        }

        public String getNodeNumber1() {
            return linkNumber.substring(0, 5);
        }

        public String getNodeNumber2() {
            return linkNumber.substring(5, 10);
        }

        public String getBasicLinkID() {
            if (getNodeNumber1().startsWith("0") && getNodeNumber2().startsWith("0")) {
                return String.format("MRD_%s_%s%s", meshCode, linkNumber.substring(1, 5), linkNumber.substring(6, 10));
            }
            return null;
        }
    }

    private String meshCode;
    private Point2D meshPoint;
    private CoordinateTransform transform;
    private HashMap<String, BasicRoadNode> basicRoadNodes = new HashMap();
    private HashMap<String, BasicRoadLink> basicRoadLinks = new HashMap();
    private HashMap<String, BasicAndAllRoadLinkMapping> basicAndAllRoadLinkMappings = new HashMap();
    private HashMap<String, String> basicAndAllRoadLinkMap = new HashMap();
    private HashMap<String, AllRoadNode> allRoadNodes = new HashMap();
    private HashMap<String, AllRoadLink> allRoadLinks = new HashMap();

    /**
     * コンストラクタ
     */
    public MRDConverter(File file, int zone) throws IOException {
        // 全レコードを読んでデータの種類ごとのハッシュマップに登録する
        MRDReader reader = new MRDReader(file);
        for (int position = 0; position < reader.getRecordSize(); position++) {
            reader.setRecordPosition(position);
            int recordID = reader.getInt("レコードID");
            switch (recordID) {
            case 11: // 管理データ（その１）
                meshCode = reader.getString("２次メッシュコード");
                meshPoint = meshCodeToCoordinate(meshCode);
                break;
            case 21: // 基本道路ノードデータ
                BasicRoadNode basicRoadNode = new BasicRoadNode(reader);
                basicRoadNodes.put(basicRoadNode.nodeNumber, basicRoadNode);
                break;
            case 22: // 基本道路リンクデータ
                BasicRoadLink basicRoadLink = basicRoadLinks.get(reader.getString("リンク番号"));
                if (basicRoadLink == null) {
                    basicRoadLink = new BasicRoadLink(reader);
                    basicRoadLinks.put(basicRoadLink.linkNumber, basicRoadLink);
                }
                basicRoadLink.readInterpolationPoints(reader);
                break;
            case 24: // 基本道路リンク、全道路リンク対応データ
                BasicAndAllRoadLinkMapping linkMapping = basicAndAllRoadLinkMappings.get(reader.getString("基本道路リンク番号"));
                if (linkMapping == null) {
                    linkMapping = new BasicAndAllRoadLinkMapping(reader);
                    basicAndAllRoadLinkMappings.put(linkMapping.linkNumber, linkMapping);
                }
                linkMapping.readAllRoadLinkNumbers(reader);
                break;
            case 31: // 全道路ノードデータ
                AllRoadNode allRoadNode = new AllRoadNode(reader);
                allRoadNodes.put(String.format("%d,%d", allRoadNode.x, allRoadNode.y), allRoadNode);
                break;
            case 32: // 全道路リンクデータ
                AllRoadLink allRoadLink = allRoadLinks.get(reader.getString("リンク番号"));
                if (allRoadLink == null) {
                    allRoadLink = new AllRoadLink(reader);
                    allRoadLinks.put(allRoadLink.linkNumber, allRoadLink);
                }
                allRoadLink.readInterpolationPoints(reader);
                break;
            }
        }
        reader.close();

        for (BasicAndAllRoadLinkMapping linkMapping : basicAndAllRoadLinkMappings.values()) {
            for (String allRoadLinkNumber : linkMapping.allRoadLinkNumbers) {
                basicAndAllRoadLinkMap.put(allRoadLinkNumber, linkMapping.linkNumber);
            }
        }

        // 経緯度(旧日本測地系)を平面直角座標に換算する
        transform = GsiTile.createCoordinateTransform(EPSG, GsiTile.JGD2000_JPR_EPSG_NAMES[zone]);
    }

    /**
     * リンクを座標リストに変換する(簡易描画用).
     *
     * 基本道路リンクと全道路リンクの重複が多少発生する
     */
    public ArrayList<ArrayList<Point2D>> convertToLines(boolean allRoadIncluding) {
        ArrayList<ArrayList<Point2D>> lines = new ArrayList();

        for (BasicRoadLink basicRoadLink : basicRoadLinks.values()) {
            ArrayList<Point2D> line = new ArrayList();
            for (Point2D point : basicRoadLink.interpolationPoints) {
                line.add(toCrowdWalkPoint(point));
            }
            if (! line.isEmpty()) {
                lines.add(line);
            }
        }

        if (allRoadIncluding) {
            for (AllRoadLink allRoadLink : allRoadLinks.values()) {
                if (basicAndAllRoadLinkMap.containsKey(allRoadLink.linkNumber)) {
                    continue;
                }
                ArrayList<Point2D> line = new ArrayList();
                BasicRoadLink basicRoadLink = basicRoadLinks.get(allRoadLink.basicLinkNumber);
                if (basicRoadLink == null) {
                    for (Point2D point : allRoadLink.interpolationPoints) {
                        line.add(toCrowdWalkPoint(point));
                    }
                    if (! line.isEmpty()) {
                        lines.add(line);
                    }
                }
            }
        }

        return lines;
    }

    /**
     * ノードとリンクをマップデータに変換してネットワークマップに追加する
     */
    public void addToNetworkMap(MapEditor editor, MapPartGroup group, int zone, boolean allRoadIncluding) throws Exception {
        NetworkMap networkMap = editor.getMap();
        HashMap<String, MapNode> nodes = new HashMap<>();
        HashMap<BasicRoadNode, MapNode> basicNodes = new HashMap<>();

        // 基本道路ノードを MapNode として NetworkMap に追加する
        for (BasicRoadNode basicRoadNode : basicRoadNodes.values()) {
            String nextID = String.format("MRD_%s_%s", basicRoadNode.nextMeshCode, basicRoadNode.nextNodeNumber);
            MapNode node = (MapNode)networkMap.getObject(nextID);
            if (node == null) {
                node = addNode(editor, basicRoadNode.getID(), group, toCrowdWalkPoint(basicRoadNode.x, basicRoadNode.y), 0.0);
                setProperties(editor, node, basicRoadNode.properties);
                if (! basicRoadNode.nextNodeNumber.equals("0000")) {
                    addJointNodeTag(editor, node);
                }
            }
            nodes.put(basicRoadNode.getID(), node);
            basicNodes.put(basicRoadNode, node);
        }

        // 基本道路リンクを MapLink として NetworkMap に追加する
        // 補間点は MapNode として NetworkMap に追加する
        for (BasicRoadLink basicRoadLink : basicRoadLinks.values()) {
            double width = WIDTH_TABLE[basicRoadLink.widthCode];

            // 通行止・一方通行タグの準備
            int trafficRegulation = (Integer)basicRoadLink.properties.get("交通規制種別コード");
            String regulationTag = null;
            if (trafficRegulation == 2 || trafficRegulation == 3) {
                regulationTag = MapLink.Tag_RoadClosed;
            } else if (trafficRegulation == 4 || trafficRegulation == 6) {
                regulationTag = MapLink.Tag_OneWayForward;
            } else if (trafficRegulation == 5 || trafficRegulation == 7) {
                regulationTag = MapLink.Tag_OneWayBackward;
            }

            MapNode from = null;
            for (int index = 0; index < basicRoadLink.nInterpolationPoints; index++) {
                MapNode node = null;
                if (index == 0) {
                    node = nodes.get(basicRoadLink.getNode1ID());
                    if (node == null) {
                        throw new Exception("Node 1 of BasicRoadLink not found: " + basicRoadLink.getNode1ID());
                    }
                } else if (index == basicRoadLink.nInterpolationPoints - 1) {
                    node = nodes.get(basicRoadLink.getNode2ID());
                    if (node == null) {
                        throw new Exception("Node 2 of BasicRoadLink not found: " + basicRoadLink.getNode2ID());
                    }
                } else {
                    Point2D iPoint = basicRoadLink.interpolationPoints.get(index);
                    AllRoadNode allRoadNode = null;
                    boolean hasIntersection = false;
                    if (allRoadIncluding) {
                        allRoadNode = allRoadNodes.get(String.format("%d,%d", (int)iPoint.getX(), (int)iPoint.getY()));
                        if (allRoadNode != null) {
                            hasIntersection = ((Integer)allRoadNode.properties.get("ノード種別コード") == 1);   // 交差点ノード
                        }
                    }
                    // 該当する全道路ノードがあれば補間点ノードに流用する
                    if (allRoadIncluding && hasIntersection) {
                        node = addNode(editor, allRoadNode.getID(), group, toCrowdWalkPoint(iPoint), 0.0);
                        setProperties(editor, node, allRoadNode.properties);
                        if (! allRoadNode.nextNodeNumber.equals("00000")) {
                            addJointNodeTag(editor, node);
                        }
                        nodes.put(allRoadNode.getID(), node);
                    }
                    // 補間点ノードを生成する
                    else {
                        node = addNode(editor, null, group, toCrowdWalkPoint(iPoint), 0.0);
                        // この node が交差点になることはないので nodes には追加しない
                    }
                }

                if (from != null) {
                    String id = String.format("%s_%03d", basicRoadLink.getID(), index);
                    double length = from.getPosition().distance(node.getPosition());
                    MapLink link = addLink(editor, id, from, node, length, width);
                    if (regulationTag != null) {
                        AddTag _command = new AddTag(link, regulationTag);
                        if (! editor.invoke(_command)) {
                            throw new Exception("Edit command error: " + _command.toString());
                        }
                    }
                    setProperties(editor, link, basicRoadLink.properties);
                }
                from = node;
            }
        }

        // 接続リンクごとの属性情報をノードにセットする
        String[] names = {"接続リンクID", "交差点通行コード", "接続角度"};
        for (Map.Entry<BasicRoadNode, MapNode> entry : basicNodes.entrySet()) {
            HashMap<String, Object> properties = new LinkedHashMap();
            BasicRoadNode basicRoadNode = entry.getKey();
            MapNode node = entry.getValue();
            // 今回初めて登録されたノードの場合
            if (getPropertyAsString(node, "接続リンクID\\[1\\]") == null) {
                for (int index = 0; index < basicRoadNode.connectionNodeNumbers.size(); index++) {
                    String id = getMapLink(networkMap, basicRoadNode, index).getID();
                    String passCode = basicRoadNode.passCodes.get(index);
                    Integer connectionAngle = basicRoadNode.connectionAngles.get(index);
                    properties.put(String.format("接続リンクID[%d]", index + 1), id);
                    properties.put(String.format("交差点通行コード[%d]", index + 1), passCode);
                    properties.put(String.format("接続角度[%d]", index + 1), connectionAngle);
                }
            }
            // 隣接メッシュの既存ノードと接合する場合は接続リンク情報をマージする
            else {
                // 接合するノード側の接続リンク本数のチェック
                if (basicRoadNode.connectionNodeNumbers.size() != 1) {
                    throw new Exception("Incorrect number of connected links: " + basicRoadNode.nodeNumber);
                }
                // 既存ノードの接続リンク本数のチェック
                String str = getPropertyAsString(node, "接続リンク本数");
                if (str == null || Integer.parseInt(str) != 1) {
                    throw new Exception("Incorrect number of connected links: " + node.toShortInfo());
                }
                removeProperty(editor, node, "接続リンク本数");
                properties.put("接続リンク本数", Integer.valueOf(2));

                // 既存ノードに登録されている属性情報を取得後に削除する
                String linkIdOfExistingNode = null;
                Integer connectionAngleOfExistingNode = null;
                for (int n = 0; n < names.length; n++) {
                    String name = String.format("%s\\[1\\]", names[n]);
                    String value = getPropertyAsString(node, name);
                    if (value != null) {
                        if (n == 0) {           // 接続リンクID
                            linkIdOfExistingNode = value;
                        } else if (n == 2) {    // 接続角度
                            connectionAngleOfExistingNode = Integer.parseInt(value);
                        }
                        removeProperty(editor, node, name);
                    }
                }

                String linkID = getMapLink(networkMap, basicRoadNode, 0).getID();
                String passCode = "11000000";
                Integer connectionAngle = basicRoadNode.connectionAngles.get(0);
                if (connectionAngle < connectionAngleOfExistingNode) {
                    properties.put("接続リンクID[1]", linkID);
                    properties.put("交差点通行コード[1]", passCode);
                    properties.put("接続角度[1]", connectionAngle);
                    properties.put("接続リンクID[2]", linkIdOfExistingNode);
                    properties.put("交差点通行コード[2]", passCode);
                    properties.put("接続角度[2]", connectionAngleOfExistingNode);
                } else {
                    properties.put("接続リンクID[1]", linkIdOfExistingNode);
                    properties.put("交差点通行コード[1]", passCode);
                    properties.put("接続角度[1]", connectionAngleOfExistingNode);
                    properties.put("接続リンクID[2]", linkID);
                    properties.put("交差点通行コード[2]", passCode);
                    properties.put("接続角度[2]", connectionAngle);
                }
            }
            setProperties(editor, node, properties);
        }

        // root グループの zone を更新する
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();
        if (root.getZone() != zone) {
            SetZone command = new SetZone(root, zone);
            if (! editor.invoke(command)) {
                throw new Exception("Edit command error: " + command.toString());
            }
        }

        if (! allRoadIncluding) {
            return;
        }

        // 全道路ノードを MapNode として NetworkMap に追加する
        for (AllRoadNode allRoadNode : allRoadNodes.values()) {
            if (nodes.containsKey(allRoadNode.getID())) {
                continue;
            }
            String nextID = String.format("MRD_%s_%s", allRoadNode.nextMeshCode, allRoadNode.nextNodeNumber);
            MapNode node = (MapNode)networkMap.getObject(nextID);
            if (node == null && allRoadNode.getBasicNodeID() != null) {
                node = (MapNode)networkMap.getObject(allRoadNode.getBasicNodeID());
            }
            if (node == null) {
                node = addNode(editor, allRoadNode.getID(), group, toCrowdWalkPoint(allRoadNode.x, allRoadNode.y), 0.0);
                setProperties(editor, node, allRoadNode.properties);
                if (! allRoadNode.nextNodeNumber.equals("00000")) {
                    addJointNodeTag(editor, node);
                }
            }
            nodes.put(allRoadNode.getID(), node);
        }

        // 全道路リンクを MapLink として NetworkMap に追加する
        // 補間点は MapNode として NetworkMap に追加する
        for (AllRoadLink allRoadLink : allRoadLinks.values()) {
            if (basicAndAllRoadLinkMap.containsKey(allRoadLink.linkNumber)) {
                continue;
            }
            String basicLinkID = allRoadLink.getBasicLinkID();
            if (basicLinkID != null) {
                MapLink link = (MapLink)networkMap.getObject(basicLinkID + "_001");
                if (link != null) {
                    continue;
                }
            }

            double width = WIDTH_TABLE[allRoadLink.widthCode];
            MapNode from = null;
            for (int index = 0; index < allRoadLink.nInterpolationPoints; index++) {
                MapNode node = null;
                if (index == 0) {
                    node = nodes.get(allRoadLink.getNode1ID());
                    if (node == null) {
                        throw new Exception("Node 1 of AllRoadLink not found: " + allRoadLink.getNode1ID());
                    }
                } else if (index == allRoadLink.nInterpolationPoints - 1) {
                    node = nodes.get(allRoadLink.getNode2ID());
                    if (node == null) {
                        throw new Exception("Node 2 of AllRoadLink not found: " + allRoadLink.getNode2ID());
                    }
                } else {
                    Point2D point = toCrowdWalkPoint(allRoadLink.interpolationPoints.get(index));
                    String pointStr = String.format("%d,%d", (int)point.getX(), (int)point.getY());
                    node = nodes.get(pointStr);
                    if (node == null) {
                        // 補間点ノードを生成する
                        node = addNode(editor, null, group, point, 0.0);
                        nodes.put(pointStr, node);
                    }
                }

                if (from != null) {
                    String id = String.format("%s_%03d", allRoadLink.getID(), index);
                    double length = from.getPosition().distance(node.getPosition());
                    MapLink link = addLink(editor, id, from, node, length, width);
                    setProperties(editor, link, allRoadLink.properties);
                }
                from = node;
            }
        }

        // 孤立しているノードがあれば削除する
        for (MapNode node : nodes.values()) {
            if (node.getLinks().isEmpty()) {
                RemoveNode command = new RemoveNode(node);
                if (! editor.invoke(command)) {
                    throw new Exception("Edit command error: " + command.toString());
                }
            }
        }
    }

    /**
     * 基本道路ノードの接続リンクに対応する MapLink を返す
     */
    public MapLink getMapLink(NetworkMap networkMap, BasicRoadNode basicRoadNode, int index) throws Exception {
        String connectionNodeNumber = basicRoadNode.connectionNodeNumbers.get(index);
        String linkNumber = makeLinkNumber(basicRoadNode.nodeNumber, connectionNodeNumber);
        BasicRoadLink basicRoadLink = basicRoadLinks.get(linkNumber);
        if (basicRoadLink == null) {
            throw new Exception("BasicRoadLink not found: " + linkNumber);
        }
        int n = basicRoadLink.getNodeNumber1().equals(basicRoadNode.nodeNumber) ? 1 : basicRoadLink.nInterpolationPoints - 1;
        String id = String.format("%s_%03d", basicRoadLink.getID(), n);
        MapLink link = (MapLink)networkMap.getObject(id);
        if (link == null) {
            throw new Exception("MapLink not found: " + id);
        }
        return link;
    }

    /**
     * メッシュコードを経度緯度に変換する.
     *
     * ※二次メッシュコードまで対応
     */
    public static Point2D meshCodeToCoordinate(String meshCode) {
        double lat = Integer.parseInt(meshCode.substring(0, 2)) / 1.5;
        double lon = Integer.parseInt(meshCode.substring(2, 4)) + 100.0;
        if (meshCode.length() > 4) {
            lat += ((Integer.parseInt(meshCode.substring(4, 5)) * 300) / 3600.0);
            lon += ((Integer.parseInt(meshCode.substring(5, 6)) * 450) / 3600.0);
        }
        return new Point2D(lon, lat);
    }

    /**
     * MRD フォーマットの正規化座標を本来の座標に変換する
     */
    public static Point2D toOriginalCoordinate(Point2D meshPoint, Point2D point) {
        double lat = meshPoint.getY() + point.getY() * 300 / 36000000.0;
        double lon = meshPoint.getX() + point.getX() * 450 / 36000000.0;
        return new Point2D(lon, lat);
    }

    /**
     * MRD フォーマットの正規化座標を本来の座標に変換する
     */
    public static Point2D toOriginalCoordinate(Point2D meshPoint, int x, int y) {
        double lat = meshPoint.getY() + y * 300 / 36000000.0;
        double lon = meshPoint.getX() + x * 450 / 36000000.0;
        return new Point2D(lon, lat);
    }

    /**
     * 正規化座標を CrowdWalk 座標に変換する.
     *
     * 座標は小数点以下第4位で丸める。
     */
    public Point2D toCrowdWalkPoint(int x, int y) {
        Point2D point = toOriginalCoordinate(meshPoint, x, y);
        java.awt.geom.Point2D jpr = GsiTile.transformCoordinate(transform, point.getX(), point.getY());
        java.awt.geom.Point2D cwPoint = GsiTile.convertJPR2CW(jpr.getY(), jpr.getX());
        return new Point2D(MapEditor.roundValue(cwPoint.getX(), 4), MapEditor.roundValue(cwPoint.getY(), 4));
    }

    /**
     * 正規化座標を CrowdWalk 座標に変換する.
     *
     * 座標は小数点以下第4位で丸める。
     */
    public Point2D toCrowdWalkPoint(Point2D xyPoint) {
        Point2D point = toOriginalCoordinate(meshPoint, xyPoint);
        java.awt.geom.Point2D jpr = GsiTile.transformCoordinate(transform, point.getX(), point.getY());
        java.awt.geom.Point2D cwPoint = GsiTile.convertJPR2CW(jpr.getY(), jpr.getX());
        return new Point2D(MapEditor.roundValue(cwPoint.getX(), 4), MapEditor.roundValue(cwPoint.getY(), 4));
    }

    /**
     * リンク番号を生成する
     */
    public static String makeLinkNumber(String nodeNumberA, String nodeNumberB) {
        if (nodeNumberA.compareTo(nodeNumberB) < 0) {
            return nodeNumberA + nodeNumberB;
        }
        return nodeNumberB + nodeNumberA;
    }

    /**
     * ネットワークマップにノードを追加する
     */
    public static MapNode addNode(MapEditor editor, String id, MapPartGroup group, Point2D coordinates, double height) throws Exception {
        AddNode command = null;
        if (id == null) {
            command = new AddNode(group, coordinates, height);
        } else {
            command = new AddNode(id, group, coordinates, height);
        }
        if (! editor.invoke(command)) {
            throw new Exception("Edit command error: " + command.toString());
        }
        return (MapNode)editor.getMap().getObject(command.getId());
    }

    /**
     * ネットワークマップにリンクを追加する
     */
    public static MapLink addLink(MapEditor editor, String id, MapNode fromNode, MapNode toNode, double length, double width) throws Exception {
        AddLink command = new AddLink(id, fromNode, toNode, length, width);
        if (! editor.invoke(command)) {
            throw new Exception("Edit command error: " + command.toString());
        }
        return (MapLink)editor.getMap().getObject(command.getId());
    }

    /**
     * ノード・リンクに属性情報を設定する.
     *
     * ※利用方法が未定のため、この実装では先頭に "property:" を付けたタグを付加している
     */
    public static void setProperties(MapEditor editor, OBNode obNode, HashMap<String, Object> properties) throws Exception {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String tag = String.format("property:%s=%s", entry.getKey(), entry.getValue().toString());
            AddTag command = new AddTag(obNode, tag);
            if (! editor.invoke(command)) {
                throw new Exception("Edit command error: " + command.toString());
            }
        }
    }

    /**
     * ノード・リンクの属性情報を文字列化して取得する.
     *
     * ※この実装では先頭に "property:" を付けたタグと照合している
     */
    public static String getPropertyAsString(OBNode obNode, String name) {
        Matcher matcher = obNode.matchTag("property:" + name + "=(.*)");
        return matcher == null ?  null : matcher.group(1);
    }

    /**
     * ノード・リンクの属性情報を削除する.
     *
     * ※この実装では先頭に "property:" を付けたタグと照合している
     */
    public static boolean removeProperty(MapEditor editor, OBNode obNode, String name) throws Exception {
        String value = getPropertyAsString(obNode, name);
        if (value == null) {
            return false;
        }
        String tag = String.format("property:%s=%s", name.replaceAll("\\\\", ""), value);
        RemoveTag command = new RemoveTag(obNode, tag);
        if (! editor.invoke(command)) {
            throw new Exception("Edit command error: " + command.toString());
        }
        return true;
    }

    /**
     * このノードが隣接メッシュとの接合ノードであることを示すタグを付加する.
     */
    public static boolean addJointNodeTag(MapEditor editor, MapNode node) throws Exception {
        if (node.hasTag(JOINT_NODE_TAG)) {
            return false;
        }
        AddTag command = new AddTag(node, JOINT_NODE_TAG);
        if (! editor.invoke(command)) {
            throw new Exception("Edit command error: " + command.toString());
        }
        return true;
    }
}
