package nodagumi.ananPJ.Editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Point2D;

import org.jruby.Ruby;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.osgeo.proj4j.CoordinateTransform;

import net.arnx.jsonic.JSON;

import de.topobyte.osm4j.core.access.DefaultOsmHandler;
import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.xml.dynsax.OsmXmlReader;

import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.Itk.Itk;
import nodagumi.Itk.Term;

/**
 * OpenStreetMap データを読み込んでマップエディタの NetworkMap にセットする
 */
public class OsmReader extends JsonicHashMapGetter {
    /**
     * OSM API を提供する Web サービスの URL
     */
    public static String API_ADDRESS = "https://api.openstreetmap.org/api/0.6/map";

    /**
     * 規定のタグフィルタ
     */
    public static String DEFAULT_TAG_FILTER = "(highway|lanes|name|oneway)=.*";

    /**
     * 幅員を表すパターン1
     */
    public static Pattern WIDTH_VALUE_PATTERN1 = Pattern.compile("(\\d+\\.\\d+)m〜(\\d+\\.\\d+)m");

    /**
     * 幅員を表すパターン2
     */
    public static Pattern WIDTH_VALUE_PATTERN2 = Pattern.compile("(\\d+\\.\\d+)m未満");

    /**
     * 幅員の規定値(m)
     */
    public static double DEFAULT_WIDTH = 5.0;

    /**
     * マップエディタ
     */
    private MapEditor editor;

    /**
     * 有効な highway の値
     */
    private ArrayList<String> highwayValues = new ArrayList();

    /**
     * highway ごとの width の計算式
     */
    private HashMap<String, String> expressions = new HashMap();

    /**
     * ruby 実行系。
     */
    private static Ruby rubyEngine = null ;

    /**
     * コンストラクタ
     */
    public OsmReader(MapEditor editor) throws Exception {
        this.editor = editor;

        // width の計算式の準備
        String filePath = editor.getProperties().getFilePath("osm_conversion_file", null);
        if (filePath == null) {
            JSON json = new JSON(JSON.Mode.TRADITIONAL);
            InputStream is = getClass().getResourceAsStream("/osm_conversion.json");
            setParameters(json.parse(is));
        } else {
            setParameters(JSON.decode(new FileInputStream(filePath)));
        }
        ArrayList<HashMap> highways = (ArrayList<HashMap>)getArrayListParameter("highways", null);
        if (highways == null) {
            throw new Exception("OSM conversion file parsing error - \"" + filePath + "\" : \"highways\" element is missing.");
        }
        for (HashMap highway : highways) {
            setParameters(highway);
            if (getBooleanParameter("reject", false)) {
                continue;
            }
            String value = getStringParameter("value", "");
            highwayValues.add(value);
            if (! value.equals("----")) {
                String expression = getStringParameter("expressionForWidth", null);
                expressions.put(value, expression);
            }
        }
    }

    /**
     * ファイルから OpenStreetMap データを読み込む
     */
    public void read(File file, MapPartGroup group, ArrayList<String> validHighways, String tagFilter, boolean reject, int zone) throws Exception {
        Itk.logInfo("Read OSM", file.getAbsolutePath());
        OsmXmlReader reader = new OsmXmlReader(file, false);
        HashMap<Long, OsmNode> osmNodes = new HashMap();
        HashMap<Long, OsmWay> osmWays = new HashMap();
        reader.setHandler(createNodeWayHandler(osmNodes, osmWays, true, validHighways));
        reader.read();
        buildRubyEngine();
        editor.startOfCommandBlock();
        try {
            convertToNodesAndLinks(osmNodes, osmWays, group, tagFilter, reject, zone);
        } catch (Exception e) {
            editor.endOfCommandBlock();
            throw new Exception(e);
        }
        editor.endOfCommandBlock();
        Itk.logInfo("Read OSM", "completed");
    }

    /**
     * Web API を利用して OpenStreetMap データを読み込む
     */
    public void read(double ulLongitude, double ulLatitude, double lrLongitude, double lrLatitude, MapPartGroup group, ArrayList<String> validHighways, String tagFilter, boolean reject, int zone) throws Exception {
        String query = API_ADDRESS + "?bbox=" + ulLongitude + "," + lrLatitude + "," + lrLongitude + "," + ulLatitude;
        Itk.logInfo("Read OSM", query);
        InputStream input = new URL(query).openStream();
        OsmXmlReader reader = new OsmXmlReader(input, false);
        HashMap<Long, OsmNode> osmNodes = new HashMap();
        HashMap<Long, OsmWay> osmWays = new HashMap();
        reader.setHandler(createNodeWayHandler(osmNodes, osmWays, true, validHighways));
        reader.read();
        buildRubyEngine();
        editor.startOfCommandBlock();
        try {
            convertToNodesAndLinks(osmNodes, osmWays, group, tagFilter, reject, zone);
        } catch (Exception e) {
            editor.endOfCommandBlock();
            throw new Exception(e);
        }
        editor.endOfCommandBlock();
        Itk.logInfo("Read OSM", "completed");
    }

    /**
     * Node 要素と Way 要素を保存するハンドラを生成する
     */
    private OsmHandler createNodeWayHandler(HashMap<Long, OsmNode> osmNodes, HashMap<Long, OsmWay> osmWays, final boolean roadOnly, ArrayList<String> validHighways) {
        return new DefaultOsmHandler() {
            @Override
            public void handle(OsmNode node) throws IOException {
                osmNodes.put(new Long(node.getId()), node);
            }

            @Override
            public void handle(OsmWay way) throws IOException {
                if (roadOnly) {
                    for (int index = 0; index < way.getNumberOfTags(); index++) {
                        OsmTag tag = way.getTag(index);
                        if (tag.getKey().equals("highway")) {
                            String value = tag.getValue();
                            for (String acceptedHighway : validHighways) {
                                if (value.equals(acceptedHighway)) {
                                    osmWays.put(new Long(way.getId()), way);
                                    return;
                                }
                            }
                        }
                    }
                } else {
                    osmWays.put(new Long(way.getId()), way);
                }
            }

            @Override
            public void complete() throws IOException {
            }
        };
    }

    /**
     * Node 要素と Way 要素を MapNode と MapLink に変換して NetworkMap にセットする
     */
    private void convertToNodesAndLinks(HashMap<Long, OsmNode> osmNodes, HashMap<Long, OsmWay> osmWays, MapPartGroup group, String tagFilter, boolean reject, int zone) throws Exception {
        ScriptingContainer container = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);

        NetworkMap networkMap = editor.getMap();
        CoordinateTransform transform = GsiTile.createCoordinateTransform("EPSG:4326", GsiTile.JGD2000_JPR_EPSG_NAMES[zone]);
        Pattern pattern = null;
        if (tagFilter != null && ! tagFilter.isEmpty()) {
            pattern = Pattern.compile(tagFilter);
        }

        for (OsmWay way : osmWays.values()) {
            double width = calcWidth(way, container);

            // リンクタグのリストを作っておく
            ArrayList<String> linkTags = new ArrayList();
            // linkTags.add("wayID=" + way.getId());
            for (int index = 0; index < way.getNumberOfTags(); index++) {
                OsmTag tag = way.getTag(index);
                if (tag.getKey().equals("source")) {
                    continue;
                }
                String tagString = tag.toString();  // "key=value"
                if (pattern != null) {
                    Matcher matcher = pattern.matcher(tagString);
                    if (matcher.matches() ? reject : ! reject) {
                        continue;
                    }
                }
                linkTags.add(tagString);
            }

            MapNode lastNode = null;
            for (int index = 0; index < way.getNumberOfNodes(); index++) {
                Long id = new Long(way.getNodeId(index));
                String nodeId = "osmn_" + id;
                MapNode node = (MapNode)networkMap.getObject(nodeId);
                // TODO: 既存の MapNode とOSMノードの内容を比較して、更新もしくはエラーで中断する
                if (node == null) {
                    OsmNode osmNode = osmNodes.get(id);
                    if (osmNode == null) {
                        lastNode = null;
                        continue;
                    }

                    // ノードを生成する
                    java.awt.geom.Point2D jpr = GsiTile.transformCoordinate(transform, osmNode.getLongitude(), osmNode.getLatitude());
                    java.awt.geom.Point2D point = GsiTile.convertJPR2CW(jpr.getY(), jpr.getX());
                    AddNode command = new AddNode(nodeId, group, new Point2D(point.getX(), point.getY()), 0.0);
                    if (! editor.invoke(command)) {
                        throw new Exception("AddNode command error");
                    }
                    node = (MapNode)networkMap.getObject(command.getId());
                    for (int idx = 0; idx < osmNode.getNumberOfTags(); idx++) {
                        OsmTag tag = osmNode.getTag(idx);
                        if (tag.getKey().equals("source")) {
                            continue;
                        }
                        String tagString = tag.toString();  // "key=value"
                        if (pattern != null) {
                            Matcher matcher = pattern.matcher(tagString);
                            if (matcher.matches() ? reject : ! reject) {
                                continue;
                            }
                        }
                        if (! editor.invoke(new AddTag(node, tagString))) {
                            throw new Exception("AddTag command error");
                        }
                    }
                }

                if (lastNode != null && lastNode.connectedTo(node) == null) {
                    // リンクを生成する
                    double length = lastNode.getPosition().distance(node.getPosition()) * group.getScale();
                    // TODO: length が 50cm 未満ならば redundant node を取り除く
                    AddLink command = new AddLink(lastNode, node, length, width);
                    if (! editor.invoke(command)) {
                        throw new Exception("AddLink command error");
                    }
                    MapLink link = (MapLink)networkMap.getObject(command.getId());
                    for (String tag : linkTags) {
                        if (! editor.invoke(new AddTag(link, tag))) {
                            throw new Exception("AddTag command error");
                        }
                    }
                }
                lastNode = node;
            }
        }
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();
        if (root.getZone() != zone) {
            if (! editor.invoke(new SetZone(root, zone))) {
                throw new Exception("SetZone command error");
            }
        }
    }

    /**
     * 幅員を求める
     */
    private double calcWidth(OsmWay way, ScriptingContainer container) {
        String highway = "";
        int lanes = 1;

        for (int index = 0; index < way.getNumberOfTags(); index++) {
            OsmTag tag = way.getTag(index);
            if (tag.getKey().equals("width")) {
                return Double.parseDouble(tag.getValue().replaceAll("[ m]", ""));
            }
        }
        StringBuilder tags = new StringBuilder("tags = {");
        for (int index = 0; index < way.getNumberOfTags(); index++) {
            OsmTag tag = way.getTag(index);
            String key = tag.getKey();
            String value = tag.getValue();

            if (key.equals("yh:WIDTH")) {
                // "yh:WIDTH=1.5m〜3.0m" 形式のタグが存在する場合
                Matcher matcher = WIDTH_VALUE_PATTERN1.matcher(value);
                if (matcher.matches()) {
                    double width1 = Double.parseDouble(matcher.group(1));
                    double width2 = Double.parseDouble(matcher.group(2));
                    return Math.round((width1 + width2) * 10.0 / 2.0) / 10.0;
                }
                // "yh:WIDTH=3.0m未満" 形式のタグが存在する場合
                matcher = WIDTH_VALUE_PATTERN2.matcher(value);
                if (matcher.matches()) {
                    return Double.parseDouble(matcher.group(1));
                }
            }

            if (key.equals("highway")) {
                highway = value;
            } else if (key.equals("lanes")) {
                lanes = Integer.parseInt(value);
            }
            tags.append(String.format("\"%s\"=>\"%s\",", key, value));
        }
        tags.append("}");
        container.runScriptlet("highway='" + highway + "'");
        container.runScriptlet("lanes=" + lanes);
        // tag の key, value セットを tags にセットする
        container.runScriptlet(tags.toString());

        String expression = expressions.get(highway);
        if (expression == null) {
            return DEFAULT_WIDTH;
        }
        return ((Double)container.runScriptlet(expression)).doubleValue();
    }

    /**
     * Rubyエンジン準備。
     */
    private void buildRubyEngine() {
        if (rubyEngine == null) {
            rubyEngine = Ruby.newInstance();
        }
    }

    /**
     * 有効な highway 値のリストを取得する(セパレータ "----" 入り)
     */
    public ArrayList<String> getHighwayValues() {
        return highwayValues;
    }
}
