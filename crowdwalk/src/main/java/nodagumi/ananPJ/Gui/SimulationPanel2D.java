// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;

import org.apache.batik.ext.awt.geom.Polygon2D;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.CrowdWalkLauncher;
import nodagumi.ananPJ.Gui.AgentAppearance.view2d.AgentAppearance2D;
import nodagumi.ananPJ.Gui.AgentAppearance.view2d.AgentViewBase2D;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.Gui.LinkAppearance.EdgePoints;
import nodagumi.ananPJ.Gui.LinkAppearance.view2d.LinkAppearance2D;
import nodagumi.ananPJ.Gui.LinkAppearance.view2d.LinkViewBase2D;
import nodagumi.ananPJ.Gui.NodeAppearance.view2d.NodeAppearance2D;
import nodagumi.ananPJ.Gui.NodeAppearance.view2d.NodeViewBase2D;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle.ObstructerDisplay;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.NetworkMapPartsListener;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Polygon.Coordinates;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.NetworkMap.Polygon.OuterBoundary;
import nodagumi.ananPJ.NetworkMap.Polygon.InnerBoundary;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.GsiAccessor;
import nodagumi.Itk.*;

/**
 * 2D シミュレーションパネル
 */
public class SimulationPanel2D extends JPanel {
    /**
     * マップをセンタリング表示する際に入れるマージンのドット数
     */
    public static final int CENTERING_MARGIN = 30;

    /**
     * ステータスメッセージの背景色
     */
    public static final Color BACKGROUND_COLOR = Color.WHITE;

    /**
     * ホバーの色
     */
    public static final Color HOVER_COLOR = Color.BLUE;

    /**
     * ホバーテキストの背景色
     */
    public static final Color HOVER_BG_COLOR = new Color(0.8f, 0.8f, 0.9f);

    /**
     * ラベル表示に使用するフォント名
     */
    public static final String FONT_NAME = "Consolas";

    /**
     * テキスト描画の際に指定する描画位置
     */
    public static enum TextPosition { UPPER_CENTER, LOWER_CENTER, UPPER_RIGHT, LOWER_RIGHT }

    /**
     * シミュレーションウィンドウのフレーム
     */
    private SimulationFrame2D frame = null;

    /**
     * AIST ロゴ画像
     */
    private Image aistLogo = getToolkit().createImage(getClass().getResource("/img/aist_logo.png"));

    /**
     * グループ別の背景画像
     */
    private HashMap<MapPartGroup, Image> backgroundImages = new HashMap();

    /**
     * 背景画像の色の濃さ
     */
    private double colorDepthOfBackgroundImage = 1.0;

    /**
     * 背景地図用の地理院タイル
     */
    private ArrayList<GsiTile> backgroundMapTiles = null;

    /**
     * 背景地図の色の濃さ
     */
    private double colorDepthOfBackgroundMap = 1.0;

    /**
     * 描画スケール
     */
    private double scale = 1.0;

    /**
     * 回転角度
     */
    private double angle = 0.0;

    /**
     * 画面の回転角度が更新されたフラグ
     */
    private boolean angleUpdated = false;

    /**
     * 描画前に平行移動する距離
     */
    private Point2D translate = new Point2D.Double(0.0, 0.0);

    /**
     * 回転を加えたノード座標を保持する
     */
    private HashMap<MapNode, Point2D> rotatedNodePoints = new HashMap();

    /**
     * 回転を加えたエリアの Path2D を保持する
     */
    private HashMap<MapArea, Path2D> rotatedAreaPaths = new HashMap();

    /**
     * ASCIIキャラクタフォントのデフォルトスケール時の文字幅
     */
    private int[] originalCharsWidths;

    /**
     * フォントのデフォルトスケール時の文字高さ
     */
    private double originalTextHeight = 0.0;

    /**
     * フォントのデフォルトスケール時のascent
     */
    private double originalAscent = 0.0;

    /**
     * フォントのデフォルトスケール時のdescent
     */
    private double originalDescent = 0.0;

    /**
     * フォントのデフォルトスケール時のleading
     */
    private double originalLeading = 0.0;

    /**
     * フォントの現在のスケールでの高さ
     */
    private double fontHeight = 0.0;

    /**
     * フォントの現在のスケールでのascent
     */
    private double fontAscent = 0.0;

    /**
     * フォントの現在のスケールでのdescent
     */
    private double fontDescent = 0.0;

    /**
     * フォントの現在のスケールでのleading
     */
    private double fontLeading = 0.0;

    /* ホバー表示対象 */

    private MapNode hoverNode = null;
    private MapLink hoverLink = null;
    private MapArea hoverArea = null;
    private AgentBase hoverAgent = null;

    /* 表示 ON/OFF フラグ */

    private boolean showNodes = true;
    private boolean showNodeLabels = false;
    private boolean showLinks = true;
    private boolean showLinkLabels = false;
    private boolean showArea = true;
    private boolean showAreaLabels = false;
    private boolean showAgents = true;
    private boolean showAgentLabels = false;

    /**
     * 通常リンクの表示色
     */
    private Color defaultLinkColor = new Color(0.1f, 0.1f, 0.1f, 1.0f - 0.75f);

    /**
     * エリアの表示色
     */
    private ObstructerDisplay show_gas = ObstructerDisplay.ORANGE;

    /**
     * エリア表示色の彩度 100% に相当する Obstructer level
     */
    private double pollutionColorSaturation = 0.0;

    /**
     * 地図データ。
     */
    private NetworkMap networkMap;

    /**
     * 描画対象リンクのリスト
     */
    private MapLinkTable regularLinks = new MapLinkTable();

    /**
     * タグ別ポリゴンオブジェクト
     */
    private HashMap<String, Polygon2D> polygons = new HashMap();

    /**
     * タグ別リンク表示スタイル
     */
    private ArrayList<LinkAppearance2D> linkAppearances = new ArrayList();

    /**
     * 各リンクに対応する LinkAppearance オブジェクト
     */
    private HashMap<MapLink, LinkAppearance2D> linkAppearanceCache = new HashMap();

    /**
     * リンクの Line2D を保持する
     */
    private HashMap<MapLink, Line2D[]> linkLines = new HashMap();

    /**
     * タグ別ノード表示スタイル
     */
    private ArrayList<NodeAppearance2D> nodeAppearances = new ArrayList();

    /**
     * 各ノードに対応する NodeAppearance オブジェクト
     */
    private HashMap<MapNode, NodeAppearance2D> nodeAppearanceCache = new HashMap();

    /**
     * タグ別エージェント表示スタイル
     */
    private ArrayList<AgentAppearance2D> agentAppearances;

    /**
     * 各エージェントに対応する View オブジェクト
     */
    private HashMap<AgentBase, AgentViewBase2D> agentViewCache = new HashMap();

    /**
     * 海面ポリゴンの外側境界座標
     */
    private ArrayList<Path2D> outerCoastlines = new ArrayList();

    /**
     * 海面ポリゴンの内側境界座標
     */
    private ArrayList<Path2D> innerCoastlines = new ArrayList();

    /**
     * 平面ポリゴン
     */
    private ArrayList<MapPolygon> mapPolygons = new ArrayList();

    /**
     * 平面ポリゴンのパス
     */
    private ArrayList<Path2D> planePolygons = new ArrayList();

    /**
     * 平面ポリゴンの表示色
     */
    private ArrayList<Color> planePolygonColors = new ArrayList();

    /**
     * ポリゴン表示スタイル
     */
    private ArrayList<PolygonAppearance> polygonAppearances;

    /**
     * 表示更新済みフラグ
     */
    private boolean updated = false;

    /**
     * 属性を扱うハンドラ
     */
    private CrowdWalkPropertiesHandler properties;

    /**
     * legacy モード
     */
    private boolean legacy = false;

    synchronized public boolean isUpdated() {
        return updated;
    }

    synchronized public void setUpdated(boolean b) {
        updated = b;
    }

    /* constructor
     */
    public SimulationPanel2D(SimulationFrame2D _frame, NetworkMap _networkMap, CrowdWalkPropertiesHandler properties, ArrayList<GsiTile> backgroundMapTiles, ArrayList<HashMap> linkAppearanceConfig, ArrayList<HashMap> nodeAppearanceConfig) {
        super();
        frame = _frame;
        networkMap = _networkMap;
        this.backgroundMapTiles = backgroundMapTiles;
        this.properties = properties;
        legacy = CrowdWalkLauncher.legacy || properties.isLegacy();

        try {
            // Link appearance の準備
            String[] exclusionTags = new String[0];
            if (legacy) {
                exclusionTags = new String[]{"POLYGON"};
            }
            EdgePoints edgePoints = new EdgePoints(networkMap, exclusionTags);
            for (HashMap parameters : linkAppearanceConfig) {
                LinkAppearance2D appearance = new LinkAppearance2D(this, parameters, edgePoints, linkLines);
                if (! appearance.isValidFor2D()) {
                    Itk.logFatal("Link appearance error", "2D view not defined");
                    Itk.quitByError() ;
                }
                linkAppearances.add(appearance);
            }

            // Node appearance の準備
            for (HashMap parameters : nodeAppearanceConfig) {
                NodeAppearance2D appearance = new NodeAppearance2D(this, parameters);
                if (! appearance.isValidFor2D()) {
                    Itk.logFatal("Node appearance error", "2D view not defined");
                    Itk.quitByError() ;
                }
                nodeAppearances.add(appearance);
            }

            if (properties.getPropertiesFile() != null) {
                show_gas = ObstructerDisplay.valueOf(properties.getStringInPattern(
                    "pollution_color", "ORANGE", ObstructerDisplay.getNames()).toUpperCase());
                pollutionColorSaturation =
                    properties.getDouble("pollution_color_saturation", 0.0);

                String filePath = properties.getFilePath("polygon_appearance_file", null);
                polygonAppearances = PolygonAppearance.load(filePath);

                // グループ別の背景画像を読み込む
                File file = new File(properties.getNetworkMapFile());
                String dir = file.getParent() + File.separator;
                for (MapPartGroup group : networkMap.getGroups()) {
                    String imageFileName = group.getImageFileName();
                    if (imageFileName == null || imageFileName.isEmpty()) {
                        continue;
                    }
                    backgroundImages.put(group, getToolkit().getImage(dir + imageFileName));
                }

                double values[] = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
                colorDepthOfBackgroundImage = properties.getDouble("color_depth_of_background_image", colorDepthOfBackgroundImage);
                if (Arrays.binarySearch(values, colorDepthOfBackgroundImage) < 0) {
                    Itk.logWarn("Invalid parameter", "color_depth_of_background_image", "" + colorDepthOfBackgroundImage);
                    colorDepthOfBackgroundImage = 1.0;
                }

                colorDepthOfBackgroundMap = properties.getDouble("color_depth_of_background_map", colorDepthOfBackgroundMap);
                if (Arrays.binarySearch(values, colorDepthOfBackgroundMap) < 0) {
                    Itk.logWarn("Invalid parameter", "color_depth_of_background_map", "" + colorDepthOfBackgroundMap);
                    colorDepthOfBackgroundMap = 1.0;
                }
            } else {
                polygonAppearances = PolygonAppearance.load(null);
            }
        } catch (Exception e) {
            Itk.quitWithStackTrace(e) ;
        }

        // マップの回転を反映したノード座標の準備
        recalcRotatedNodes();

        // マップの回転を反映したエリアの Path2D の準備
        recalcRotatedAreas();

        polygons = createPolygons(networkMap.getLinks());
        // ポリゴンを除いたリンクのリスト
        for (MapLink link : frame.getLinks()) {
            if (link.getFrom() == link.getTo()) {
                // 不正なリンクは無視する
                Itk.logWarn_("Looped link found", "ID=" + link.ID);
                continue;
            }
            if (! legacy || ! link.hasSubTag("POLYGON")) {
                regularLinks.add(link);
            }
        }

        // マップの回転を反映したリンクの Line2D の準備
        recalcRotatedLinks();

        // 海面描画の準備
        Coastline coastline = frame.getLauncher().getCoastline();
        if (coastline != null) {
            for (ArrayList<Point2D> boundary : coastline.getOuterBoundaries()) {
                outerCoastlines.add(Coastline.pointListToPath2D(boundary));
            }
            for (ArrayList<Point2D> boundary : coastline.getIslands()) {
                innerCoastlines.add(Coastline.pointListToPath2D(boundary));
            }
        }

        // 平面ポリゴンの準備
        for (MapPolygon polygon : networkMap.getPolygons()) {
            if (polygon.isPlanePolygon()) {
                mapPolygons.add(polygon);
            }
        }
        // 標高順にソートする
        mapPolygons.sort(new Comparator<MapPolygon>() {
            public int compare(MapPolygon polygon1, MapPolygon polygon2) {
                return (int)Math.signum(polygon1.getOuterBoundary().getHeight() - polygon2.getOuterBoundary().getHeight());
            }
        });
        for (MapPolygon polygon : mapPolygons) {
            PolygonAppearance appearance = getPolygonAppearance(polygon.getTags());
            String colorValue = appearance.getColorValue();
            if (colorValue == null) {
                Itk.logError("Mismatch of appearance of polygon: ID=" + polygon.getID(), "Only single color specification is allowed");
                continue;
            }
            Color color = Color2D.getColor(colorValue);
            if (color == null) {
                Itk.logError("Polygon appearance error: ID=" + polygon.getID(), "Color name is not defined: " + colorValue);
                continue;
            }
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(appearance.getOpacity() * 255));

            planePolygons.add(MapPolygon.convertToPath2D(polygon.getOuterBoundary().getCoordinates()));
            planePolygonColors.add(color);
            for (InnerBoundary innerBoundary : polygon.getInnerBoundaries()) {
                planePolygons.add(MapPolygon.convertToPath2D(innerBoundary.getCoordinates()));
                planePolygonColors.add(Color.WHITE);
            }
        }

        // マップ更新通知リスナの設定
        final SimulationPanel2D panel = this;
        networkMap.getNotifier().addListener(new NetworkMapPartsListener() {
            /**
             * リンクが削除された.
             */
            public void linkRemoved(MapLink link) {
                Itk.logInfo("Link Removed");
            }

            /**
             * リンクタグが追加された.
             */
            public void linkTagAdded(MapLink link, String tag) {
                Itk.logInfo("Link Tag Added", tag);
                synchronized (panel) {
                    linkAppearanceCache.remove(link);
                    getLinkAppearance(link).update(link);
                }
            }

            /**
             * リンクタグが削除された.
             */
            public void linkTagRemoved(MapLink link) {
                Itk.logInfo("Link Tag Removed");
                synchronized (panel) {
                    linkAppearanceCache.remove(link);
                    getLinkAppearance(link).update(link);
                }
            }

            /**
             * ノードタグが追加された.
             */
            public void nodeTagAdded(MapNode node, String tag) {
                Itk.logInfo("Node Tag Added", tag);
                synchronized (panel) {
                    nodeAppearanceCache.remove(node);
                }
            }

            /**
             * ノードタグが削除された.
             */
            public void nodeTagRemoved(MapNode node) {
                Itk.logInfo("Node Tag Removed");
                synchronized (panel) {
                    nodeAppearanceCache.remove(node);
                }
            }

            /**
             * Pollution レベルが変化した.
             */
            public void pollutionLevelChanged(MapArea area) {}

            /**
             * エージェントが追加された.
             */
            public void agentAdded(AgentBase agent) {}

            /**
             * エージェントが移動した(swing も含む).
             */
            public void agentMoved(AgentBase agent) {}

            /**
             * エージェントのスピードが変化した.
             */
            public void agentSpeedChanged(AgentBase agent) {}

            /**
             * エージェントのトリアージレベルが変化した.
             */
            public void agentTriageChanged(AgentBase agent) {}

            /**
             * エージェントの避難が完了した.
             */
            public void agentEvacuated(AgentBase agent) {}
        });
    }

    /**
     * リンクリストからポリゴンデータを生成する.
     *
     * ※ legacy モード時のみ有効
     */
    public HashMap<String, Polygon2D> createPolygons(MapLinkTable links) {
        HashMap<String, Polygon2D> polygons = new HashMap();
        if (! legacy) {
            return polygons;
        }

        // タグごとにリンクを選別する
        HashMap<String, MapLinkTable> polygonLinks = new HashMap();
        for (MapLink link : links) {
            for (String tag : link.getTags()) {
                if (tag.contains("POLYGON")) {
                    MapLinkTable _links = polygonLinks.get(tag);
                    if (_links == null) {
                        _links = new MapLinkTable();
                        polygonLinks.put(tag, _links);
                    }
                    _links.add(link);
                }
            }
        }
        // 選別したリンクでポリゴンを生成する
        for (String tag : polygonLinks.keySet()) {
            // 通過点順に MapNode を収集する
            MapLinkTable _links = polygonLinks.get(tag);
            MapLink currentLink = _links.get(0);
            MapNode start = currentLink.getFrom();
            MapNode next = currentLink.getTo();
            MapNodeTable polygonNodes = new MapNodeTable();
            polygonNodes.add(start);
            while (next != start) {
                polygonNodes.add(next);
                for (MapLink link : _links) {
                    if (link == currentLink) {
                        continue;
                    }
                    if (next == link.getFrom() || next == link.getTo()) {
                        currentLink = link;
                        if (currentLink.getFrom() != next)
                            next = currentLink.getFrom();
                        else
                            next = currentLink.getTo();
                        break;
                    }
                }
            }
            // 収集した MapNode でポリゴンオブジェクトを生成する
            Polygon2D polygon = new Polygon2D();
            for (MapNode node : polygonNodes) {
                polygon.addPoint((float)getRotatedX(node), (float)getRotatedY(node));
            }
            polygons.put(tag, polygon);
        }
        return polygons;
    }

    /**
     * マウスカーソル位置を基準にズームイン/ズームアウトする
     */
    public void zoom(double x, double y, int wheelRotation, boolean controlDown) {
        Point2D origin = add(translate, -x, -y);
        double ox = origin.getX() / scale;
        double oy = origin.getY() / scale;

        if (wheelRotation < 0) {
            scale *= (controlDown ? 1.01 : 1.1);
        } else if (wheelRotation > 0) {
            scale /= (controlDown ? 1.01 : 1.1);
        }
        scale = Math.min(scale, 100.0);
        scale = Math.max(scale, 0.01);
        frame.setStatusText("Scale: " + scale);

        translate = new Point2D.Double(ox * scale + x, oy * scale + y);
    }

    /**
     * マウスカーソル位置を基準に画面を回転する
     */
    public void rotate(double x, double y, double angle) {
        if (angle < 0.0) {
            angle += 360.0;
        } else if (angle >= 360.0) {
            angle -= 360.0;
        }

        Point2D mapPoint = convertToOriginal(pointConvertCanvasToMap(x, y));
        this.angle = angle;
        Point2D point = calcRotatedPoint(mapPoint);
        translate = new Point2D.Double(x - point.getX() * scale, y - point.getY() * scale);

        frame.setStatusText("Rotation angle: " + angle);
        angleUpdated = true;
    }

    /**
     * マウスカーソル位置を基準に画面を回転する
     */
    public void rotate(double x, double y, int wheelRotation, boolean controlDown) {
        double delta = controlDown ? 0.1 : 1.0;     // Control キーが押されていたら 0.1 度刻み

        if (wheelRotation < 0) {
            rotate(x, y, angle - delta);
        } else if (wheelRotation > 0) {
            rotate(x, y, angle + delta);
        }
    }

    /**
     * 描画スケールを指定する
     */
    public void setDrawingScale(double scale) {
        this.scale = scale;
    }
    
    /**
     * 描画スケールを返す
     */
    public double getDrawingScale() {
        return scale;
    }

    /**
     * 回転角度を指定する
     */
    public void setAngle(double angle) {
        if (angle != this.angle) {
            this.angle = angle;
            angleUpdated = true;
        }
    }

    /**
     * 回転角度を返す
     */
    public double getAngle() {
        return angle;
    }

    /**
     * マップをスクロールする
     */
    public void scroll(int dx, int dy) {
        translate = add(translate, (double)dx, (double)dy);
    }

    /**
     * 描画前に平行移動する距離を指定する
     */
    public void setPosition(double x, double y) {
        translate = new Point2D.Double(x * scale, y * scale);
    }

    /**
     * 視点座標を返す
     */
    public Point2D getViewPosition() {
        return new Point2D.Double(translate.getX() / scale, translate.getY() / scale);
    }

    /**
     * マップをセンタリングする
     */
    public void centering(boolean withScaling) {
        if (angle != 0.0) {
            angle = 0.0;
            angleUpdated = true;
        }

        Rectangle2D bounds = calcRotatedMapRectangle();
        double cx = bounds.getMinX() + bounds.getWidth() / 2.0;
        double cy = bounds.getMinY() + bounds.getHeight() / 2.0;

        // map is empty
        if (cx == 0.0 && cy == 0.0) {
            if (withScaling) {
                scale = 1.0;
            }
            translate = new Point2D.Double(getWidth() / 2, getHeight() / 2);
            return;
        }

        if (withScaling) {
            int centeringMargin = 0;
            if (frame.isMarginAdded()) {
                centeringMargin = CENTERING_MARGIN;
            }
            double scaleX = (getWidth() - centeringMargin * 2) / bounds.getWidth();
            double scaleY = (getHeight() - centeringMargin * 2) / bounds.getHeight();
            scale = Math.min(scaleX, scaleY);
        }

        // -(マップの中心座標に相当するCanvas座標)
        translate = new Point2D.Double(-cx * scale, -cy * scale);
        // 表示位置をCanvasの中央に合わせる
        translate = add(translate, getWidth() / 2.0, getHeight() / 2.0);
    }

    /**
     * キャンバス座標をマップ座標に変換する
     */
    public final Point2D pointConvertCanvasToMap(double x, double y) {
        Point2D origin = add(translate, -x, -y);
        return new Point2D.Double(-origin.getX() / scale, -origin.getY() / scale);
    }

    /**
     * 点 p1 に p2 を加算した座標を返す
     */
    public final Point2D add(Point2D p1, Point2D p2) {
        return new Point2D.Double(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }

    /**
     * 点 p1 に x, y を加算した座標を返す
     */
    public final Point2D add(Point2D p1, double x, double y) {
        return new Point2D.Double(p1.getX() + x, p1.getY() + y);
    }

    /**
     * マップの回転を反映した座標を求める
     */
    public final Point2D calcRotatedPoint(double x, double y) {
        double r = Math.toRadians(angle);
        return new Point2D.Double(x * Math.cos(r) - y * Math.sin(r), x * Math.sin(r) + y * Math.cos(r));
    }

    /**
     * マップの回転を反映した座標を求める
     */
    public final Point2D calcRotatedPoint(Point2D point) {
        return calcRotatedPoint(point.getX(), point.getY());
    }

    /**
     * マップの回転を反映した座標から元の座標を求める
     */
    public final Point2D convertToOriginal(double x, double y) {
        double r = Math.toRadians(-angle);
        return new Point2D.Double(x * Math.cos(r) - y * Math.sin(r), x * Math.sin(r) + y * Math.cos(r));
    }

    /**
     * マップの回転を反映した座標から元の座標を求める
     */
    public Point2D convertToOriginal(Point2D point) {
        return convertToOriginal(point.getX(), point.getY());
    }

    /**
     * マップの回転を反映したノード座標を再計算する
     */
    private void recalcRotatedNodes() {
        double r = Math.toRadians(angle);
        rotatedNodePoints.clear();
        for (MapNode node : networkMap.getNodes()) {
            double x = node.getX() * Math.cos(r) - node.getY() * Math.sin(r);
            double y = node.getX() * Math.sin(r) + node.getY() * Math.cos(r);
            rotatedNodePoints.put(node, new Point2D.Double(x, y));
        }
    }

    /**
     * マップの回転を反映したノード座標を取得する
     */
    public final Point2D getRotatedPoint(MapNode node) {
        return rotatedNodePoints.get(node);
    }

    /**
     * マップの回転を反映したノード座標の X を取得する
     */
    public final double getRotatedX(MapNode node) {
        return rotatedNodePoints.get(node).getX();
    }

    /**
     * マップの回転を反映したノード座標の Y を取得する
     */
    public final double getRotatedY(MapNode node) {
        return rotatedNodePoints.get(node).getY();
    }

    /**
     * マップの回転を反映したリンクの Line2D を再計算する
     */
    private synchronized void recalcRotatedLinks() {
        for (MapLink link : regularLinks) {
            getLinkAppearance(link).update(link);
        }
    }

    /**
     * マップの回転を反映したエリアの Path2D を再計算する
     */
    private void recalcRotatedAreas() {
        rotatedAreaPaths.clear();
        for (MapArea area : networkMap.getAreas()) {
            Rectangle2D bounds = (Rectangle2D)area.getShape();
            Path2D path = new Path2D.Double();

            Point2D point = calcRotatedPoint(bounds.getMinX(), bounds.getMinY());
            path.moveTo(point.getX(), point.getY());
            point = calcRotatedPoint(bounds.getMaxX(), bounds.getMinY());
            path.lineTo(point.getX(), point.getY());
            point = calcRotatedPoint(bounds.getMaxX(), bounds.getMaxY());
            path.lineTo(point.getX(), point.getY());
            point = calcRotatedPoint(bounds.getMinX(), bounds.getMaxY());
            path.lineTo(point.getX(), point.getY());
            path.closePath();
            rotatedAreaPaths.put(area, path);
        }
    }

    /**
     * マップの回転を反映した海面座標を再計算する
     */
    private void recalcRotatedCoastlines() {
        Coastline coastline = frame.getLauncher().getCoastline();
        if (coastline != null) {
            outerCoastlines.clear();
            innerCoastlines.clear();
            for (ArrayList<Point2D> boundary : coastline.getOuterBoundaries()) {
                outerCoastlines.add(pointListToPath2D(boundary));
            }
            for (ArrayList<Point2D> boundary : coastline.getIslands()) {
                innerCoastlines.add(pointListToPath2D(boundary));
            }
        }
    }

    /**
     * マップの回転を反映して座標リストから Path2D を作成する.
     *
     * 先頭と末尾の座標が同じならば末尾の座標は除外する
     */
    private Path2D pointListToPath2D(ArrayList<Point2D> coordinates) {
        int length = coordinates.size();
        if (length > 1 && coordinates.get(0).equals(coordinates.get(length - 1))) {
            length--;
        }
        Path2D path = new Path2D.Double();
        for (int index = 0; index < length; index++) {
            Point2D point = calcRotatedPoint(coordinates.get(index));
            if (path.getCurrentPoint() == null) {
                path.moveTo(point.getX(), point.getY());
            } else {
                path.lineTo(point.getX(), point.getY());
            }
        }
        path.closePath();
        return path;
    }

    /**
     * マップの回転を反映した平面ポリゴン座標を再計算する
     */
    private void recalcRotatedPlanePolygons() {
        planePolygons.clear();
        for (MapPolygon polygon : mapPolygons) {
            planePolygons.add(convertToPath2D(polygon.getOuterBoundary().getCoordinates()));
            for (InnerBoundary innerBoundary : polygon.getInnerBoundaries()) {
                planePolygons.add(convertToPath2D(innerBoundary.getCoordinates()));
            }
        }
    }

    /**
     * マップの回転を反映して頂点座標を Path2D に変換する
     */
    private Path2D convertToPath2D(Coordinates coordinates) {
        Path2D path = new Path2D.Double();
        if (! coordinates.getPoints2D().isEmpty()) {
            for (Point2D point : coordinates.getPoints2D()) {
                Point2D rotatedPoint = calcRotatedPoint(point);
                if (path.getCurrentPoint() == null) {
                    path.moveTo(rotatedPoint.getX(), rotatedPoint.getY());
                } else {
                    path.lineTo(rotatedPoint.getX(), rotatedPoint.getY());
                }
            }
            path.closePath();
        }
        return path;
    }

    /**
     * マップに外接する矩形を算出する
     */
    public Rectangle2D calcRotatedMapRectangle() {
        double north = 0.0;
        double south = 0.0;
        double west = 0.0;
        double east = 0.0;
        for (MapNode node : networkMap.getNodes()) {
            Point2D point = calcRotatedPoint(node.getX(), node.getY());
            if (north == 0.0 && south == 0.0) {
                north = point.getY();
                south = point.getY();
                west = point.getX();
                east = point.getX();
            }
            if (point.getY() < north) {
                north = point.getY();
            }
            if (point.getY() > south) {
                south = point.getY();
            }
            if (point.getX() < west) {
                west = point.getX();
            }
            if (point.getX() > east) {
                east = point.getX();
            }
        }
        return new Rectangle2D.Double(west, north, east - west, south - north);
    }

    /**
     * リンクが矩形領域の内部と交差しているか?
     */
    private boolean intersectsLine(Rectangle2D rectangle, MapLink link) {
        return rectangle.intersectsLine(
            getRotatedX(link.getFrom()), getRotatedY(link.getFrom()),
            getRotatedX(link.getTo()), getRotatedY(link.getTo())
        );
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (angleUpdated) {
            recalcRotatedNodes();
            recalcRotatedLinks();
            recalcRotatedAreas();
            polygons = createPolygons(networkMap.getLinks());
            recalcRotatedCoastlines();
            recalcRotatedPlanePolygons();
            angleUpdated = false;
        }

        // viewArea を使って表示範囲外の無駄な描画処理を省く
        Point2D upperLeft = pointConvertCanvasToMap(0.0, 0.0);
        Point2D lowerRight = pointConvertCanvasToMap(getWidth(), getHeight());
        Rectangle2D viewArea = new Rectangle2D.Double(upperLeft.getX(), upperLeft.getY(), lowerRight.getX() - upperLeft.getX(), lowerRight.getY() - upperLeft.getY());

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        AffineTransform saveAT = g2.getTransform();

        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, getWidth(), getHeight());

        if (originalTextHeight == 0.0) {
            g2.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
            FontMetrics fm = g2.getFontMetrics();
            originalTextHeight = (double)fm.getHeight();
            originalAscent = (double)fm.getAscent();
            originalDescent = (double)fm.getDescent();
            originalLeading = (double)fm.getLeading();
            originalCharsWidths = fm.getWidths();
        }
        fontHeight = originalTextHeight / scale;
        fontAscent = originalAscent / scale;
        fontDescent = originalDescent / scale;
        fontLeading = originalLeading / scale;

        g2.translate(translate.getX(), translate.getY());
        g2.scale(scale, scale);

        setScaleFixedFont(g2, FONT_NAME, Font.PLAIN, 14);

        // 海面の描画
        if (frame.isShowTheSea()) {
            g2.setColor(Color2D.AEGEANBLUE);
            for (Path2D boundary : outerCoastlines) {
                g2.fill(boundary);
            }
            g2.setColor(Color.WHITE);
            for (Path2D boundary : innerCoastlines) {
                g2.fill(boundary);
            }
        }

        // 背景地図の描画
        if (frame.isShowBackgroundMap()) {
            Composite originalComposite = g2.getComposite();
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)colorDepthOfBackgroundMap);
            g2.setComposite(ac);
            for (GsiTile mapTile : backgroundMapTiles) {
                drawBackgroundMapTile(mapTile, g2);
            }
            g2.setComposite(originalComposite);
        }

        // 背景画像の描画
        if (frame.isShowBackgroundImage()) {
            Composite originalComposite = g2.getComposite();
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)colorDepthOfBackgroundImage);
            g2.setComposite(ac);
            for (MapPartGroup group : networkMap.getGroups()) {
                Image image = backgroundImages.get(group);
                if (image != null && frame.isShowBackgroundImage(group)) {
                    drawBackgroundImage(group, image, g2);
                }
            }
            g2.setComposite(originalComposite);
        }

        if (showArea) {
            for (MapArea area : frame.getMapAreas()) {
                drawArea(area, g2, showAreaLabels) ;
            }
        }

        // リンクポリゴンの描画
        if (! polygons.isEmpty()) {
            for (String tag : polygons.keySet()) {
                Polygon2D polygon = polygons.get(tag);
                drawPolygon(g2, tag, polygon);
            }
        }

        // 平面ポリゴンの描画(穴あきポリゴンの穴の部分は白く塗り潰しているだけ)
        if (frame.isPolygonShowing() && ! planePolygons.isEmpty()) {
            int index = 0;
            for (Path2D path : planePolygons) {
                g2.setColor(planePolygonColors.get(index));
                g2.fill(path);
                index++;
            }
        }

        /* actual objects */
        if (showLinks) {
            synchronized (this) {
                for (MapLink link : regularLinks) {
                    if (intersectsLine(viewArea, link)) {
                        getLinkAppearance(link).getView().draw(link, g2, showLinkLabels);
                    }
                }
            }
        }
        if (showNodes) {
            synchronized (this) {
                g2.setStroke(new BasicStroke((float)(1.0 / scale)));
                for (MapNode node : frame.getNodes()) {
                    if (viewArea.contains(getRotatedX(node), getRotatedY(node))) {
                        getNodeAppearance(node).getView().draw(node, g2, showNodeLabels);
                    }
                }
            }
        }
        if (showAgents) {
            for (AgentBase agent : frame.getWalkingAgents()) {
                if (! agent.isEvacuated()) {
                    getAgentView(agent).draw(agent, g2, showAgentLabels);
                }
            }
        }

        /* temporary objects */
        if (hoverNode != null) {
            synchronized (this) {
                getNodeAppearance(hoverNode).getView().drawHover(hoverNode, g2);
            }
        }
        if (hoverLink != null) {
            synchronized (this) {
                getLinkAppearance(hoverLink).getView().drawHover(hoverLink, g2);
            }
        }
        if (hoverArea != null) {
            drawHoverArea(hoverArea, g2);
        }
        if (hoverAgent != null && ! hoverAgent.isEvacuated()) {
            getAgentView(hoverAgent).drawHover(hoverAgent, g2);
        }

        // アフィン変換をリセットする
        g2.setTransform(saveAT);

        if (frame.isShowLogo()) {
            drawLogo(g2);
        }

        if (frame.isShowStatus()) {
            drawMessage(g2, frame.getStatusPosition(), frame.getStatusText());
        }

        // 表示更新待ちを解除する
        if (frame.isViewSynchronized()) {
            synchronized (this) {
                setUpdated(true);
                notify();
            }
        }
    }

    /**
     * 現在の1ドットの幅(m)を取得する
     */
    public double getWidthOf1dot() {
        Point2D upperLeft = pointConvertCanvasToMap(0.0, 0.0);
        Point2D lowerRight = pointConvertCanvasToMap(getWidth(), getHeight());
        return (lowerRight.getX() - upperLeft.getX()) / getWidth();
    }

    /**
     * ポリゴンを描画する.
     *
     * ※ 呼ばれるのは legacy モード時のみ
     */
    public void drawPolygon(Graphics2D g2, String tag, Polygon2D polygon) {
        Color color = Color2D.GRAY;
        if (tag.contains("OCEAN")) {
            color = Color2D.SLATEBLUE;
        } else if (tag.contains("STRUCTURE")) {
            color = Color2D.LIGHTGRAY;
        }
        g2.setColor(color);
        g2.fill(polygon);

        // ポリゴン間に隙間が出来てしまうのを防ぐ
        g2.setStroke(new BasicStroke((float)getWidthOf1dot(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g2.draw(polygon);
    }

    /**
     * エリアを描画する
     */
    public void drawArea(MapArea area, Graphics2D g2, boolean showLabel) {
        if (area.getPollutionLevel() == null || ! area.getPollutionLevel().isPolluted()) {
            if (frame.isShowAreaLocation()) {
                g2.setColor(Color.LIGHT_GRAY);
                g2.fill(rotatedAreaPaths.get(area));
            }
            return;
        }

        float density = 0.0f;
        if (pollutionColorSaturation == 0.0) {
            density = (float)area.getPollutionLevel().getNormalizedLevel();
        } else {
            density = (float)(area.getPollutionLevel().getCurrentLevel() / pollutionColorSaturation);
            if (density > 1.0f)
                density = 1.0f;
        }
        density = Math.max(density, 0.0f);   // マイナスの水位はサポートしない
        float alpha = density / 1.5f;

        Color color = null;
        switch (show_gas) {
        case RED:
            color = new Color(density, 0.0f, 0.0f, alpha);
            break;
        case BLUE:
            color = new Color(0.0f, 0.0f, density, alpha);
            break;
        case HSV:
            float f = (1.0f - density) * 0.65f;
            color = new Color(Color.HSBtoRGB(f, 1.0f, 1.0f));
            break;
        case ORANGE:
            color = new Color(1.0f, 1.0f - density / 2.0f, 0.0f, alpha);
            break;
        }

        g2.setColor(color);
        g2.fill(rotatedAreaPaths.get(area));

        if (showLabel) {
            String text = area.getTagString();
            if (! text.isEmpty()) {
                g2.setColor(Color.darkGray);
                double cx = ((Rectangle2D)area.getShape()).getMinX();
                double cy = ((Rectangle2D)area.getShape()).getMinY();
                // TODO: バックグラウンドカラーが何故か微妙に異なってしまう
                drawText(g2, cx, cy, TextPosition.LOWER_RIGHT, text, color);
            }
        }
    }

    /**
     * エリアのホバーを描画する
     */
    public void drawHoverArea(MapArea hoverArea, Graphics2D g2) {
        g2.setStroke(new BasicStroke((float)(3.0 / scale)));
        g2.setColor(Color.BLUE);
        g2.draw(rotatedAreaPaths.get(hoverArea));

        String text = hoverArea.getTagString();
        if (! text.isEmpty()) {
            double cx = ((Rectangle2D)hoverArea.getShape()).getMinX();
            double cy = ((Rectangle2D)hoverArea.getShape()).getMinY();
            drawText(g2, cx, cy, TextPosition.LOWER_RIGHT, text, HOVER_BG_COLOR);
        }
    }

    /**
     * 背景画像を描画する
     */
    public void drawBackgroundImage(MapPartGroup group, Image image, Graphics2D g2) {
        Point2D point = calcRotatedPoint(group.tx, group.ty);
        AffineTransform at = g2.getTransform();
        g2.translate(point.getX(), point.getY());
        double angle = Math.toDegrees(group.r) + this.angle;
        g2.rotate(Math.toRadians(angle));
        g2.scale(group.sx, group.sy);
        g2.drawImage(image, 0, 0, image.getWidth(null), image.getHeight(null), this);
        g2.setTransform(at);
    }

    /**
     * 地理院タイルを描画する
     */
    public void drawBackgroundMapTile(GsiTile gsiTile, Graphics2D g2) {
        AffineTransform at = g2.getTransform();
        BufferedImage image = gsiTile.getImage();
        Point2D point = calcRotatedPoint(gsiTile.getPoint());

        g2.translate(point.getX(), point.getY());
        g2.scale(gsiTile.getScaleX(), gsiTile.getScaleY());
        g2.rotate(Math.toRadians(angle));
        g2.drawImage(image, 0, 0, image.getWidth(null), image.getHeight(null), this);
        g2.setTransform(at);
    }

    /**
     * ステータスメッセージを描画する
     */
    public void drawMessage(Graphics2D g2, int position, String message) {
        Font font = new Font(FONT_NAME, Font.PLAIN, 14);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int width = fm.stringWidth(message);
        int height = fm.getHeight();
        int ascent = fm.getAscent();
        int x = 12;     // メッセージの基準表示位置
        int y = 12;     //          〃
        if ((position & frame.BOTTOM) == frame.BOTTOM) {
            y += getHeight() - height;
        }
        g2.setColor(BACKGROUND_COLOR);           // メッセージの背景色
        g2.fillRect(x - 4, y - ascent, width + 7, height - 1);  // メッセージの背景描画
        g2.setColor(Color.BLACK);
        g2.drawString(message, x, y);
    }

    /**
     * AIST ロゴを描画する
     */
    public void drawLogo(Graphics2D g2) {
        int x = getWidth() - aistLogo.getWidth(null);
        int y = getHeight() - aistLogo.getHeight(null);
        g2.drawImage(aistLogo, x, y, this);
    }

    /**
     * 描画スケールの影響を受けないフォントの指定
     */
    public void setScaleFixedFont(Graphics2D g2, String name, int fontStyle, int fontSize) {
        int size = (int)(fontSize / scale);
        Font font = new Font(name, fontStyle, fontSize);
        g2.setFont(font.deriveFont((float)(fontSize / scale)));
    }

    /**
     * FontMetrics.stringWidth(String str) の代用
     */
    public int getStringWidth(String text) {
        int size = 0;
        for (byte c : text.getBytes()) {
            size += originalCharsWidths[c & 0xFF];
        }
        return size;
    }

    /**
     * 指定された位置に text を描画する。
     */
    public void drawText(Graphics2D g2, double x, double y, TextPosition position, String text, Color bgColor, double dx, double dy) {
        Point2D point = calcRotatedPoint(x, y);
        double textWidth = getStringWidth(text) / scale;

        switch (position) {
        case UPPER_CENTER:
            dx += textWidth / -2.0;
            dy += -(fontDescent + fontLeading);
            break;
        case LOWER_CENTER:
            dx += textWidth / -2.0;
            dy += fontAscent;
            break;
        case UPPER_RIGHT:
            dx += fontHeight / 3.0;
            dy += -(fontDescent + fontLeading);
            break;
        case LOWER_RIGHT:
            dx += fontHeight / 3.0;
            dy += fontAscent;
            break;
        }

        Color color = g2.getColor();
        g2.setColor(bgColor);
        g2.fill(new Rectangle2D.Double(point.getX() + dx - fontHeight / 3.0, point.getY() + dy - fontAscent,
                    textWidth + fontHeight * 2.0 / 3.0, fontHeight));
        g2.setColor(color);
        g2.drawString(text, (float)(point.getX() + dx), (float)(point.getY() + dy));
    }

    /**
     * 指定された位置に text を描画する。
     */
    public final void drawText(Graphics2D g2, double x, double y, TextPosition position, String text, Color bgColor) {
        drawText(g2, x, y, position, text, bgColor, 0.0, 0.0);
    }

    /**
     * 指定された位置に text を描画する。
     */
    public final void drawText(Graphics2D g2, double x, double y, TextPosition position, String text) {
        drawText(g2, x, y, position, text, getBackground());
    }

    /**
     * node のタグにマッチする NodeAppearance2D を返す.
     */
    public NodeAppearance2D getNodeAppearance(MapNode node) {
        NodeAppearance2D nodeAppearance = nodeAppearanceCache.get(node);
        if (nodeAppearance != null) {
            return nodeAppearance;
        }

        for (NodeAppearance2D appearance : nodeAppearances) {
            if (node.getTags().isEmpty()) {
                if (appearance.isTagApplied("")) {  // "*" のみ該当する
                    nodeAppearanceCache.put(node, appearance);
                    return appearance;
                }
            } else {
                for (String tag : node.getTags()) {
                    if (appearance.isTagApplied(tag)) {
                        nodeAppearanceCache.put(node, appearance);
                        return appearance;
                    }
                }
            }
        }
        return null;
    }

    /**
     * link のタグにマッチする LinkAppearance2D を返す.
     */
    public LinkAppearance2D getLinkAppearance(MapLink link) {
        if (linkAppearanceCache.containsKey(link)) {
            return linkAppearanceCache.get(link);
        }

        for (LinkAppearance2D appearance : linkAppearances) {
            if (link.getTags().isEmpty()) {
                if (appearance.isTagApplied("")) {  // "*" のみ該当する
                    linkAppearanceCache.put(link, appearance);
                    return appearance;
                }
            } else {
                for (String tag : link.getTags()) {
                    if (appearance.isTagApplied(tag)) {
                        linkAppearanceCache.put(link, appearance);
                        return appearance;
                    }
                }
            }
        }
        return null;
    }

    /**
     * agent のタグにマッチする AgentView を返す.
     */
    public AgentViewBase2D getAgentView(AgentBase agent) {
        AgentViewBase2D agentView = agentViewCache.get(agent);
        if (agentView != null) {
            return agentView;
        }

        for (AgentAppearance2D appearance : agentAppearances) {
            if (agent.getTags().isEmpty()) {
                if (appearance.isTagApplied("")) {  // "*" のみ該当する
                    agentViewCache.put(agent, appearance.getView());
                    return appearance.getView();
                }
            } else {
                for (String tag : agent.getTags()) {
                    if (appearance.isTagApplied(tag)) {
                        agentViewCache.put(agent, appearance.getView());
                        return appearance.getView();
                    }
                }
            }
        }
        return null;
    }

    /**
     * polygon のタグにマッチする PolygonAppearance を返す.
     */
    public PolygonAppearance getPolygonAppearance(ArrayList<String> tags) {
        for (PolygonAppearance appearance : polygonAppearances) {
            if (tags.isEmpty()) {
                if (appearance.isTagApplied("")) {  // "*" のみ該当する
                    return appearance;
                }
            } else {
                for (String tag : tags) {
                    if (appearance.isTagApplied(tag)) {
                        return appearance;
                    }
                }
            }
        }
        return null;
    }

    /* -- Methods to set how drawn 
     */
    public void updateHoverNode(MapNode _hoverNode) {
        hoverNode = _hoverNode;
    }
    
    public void updateHoverLink(MapLink _hoverLink) {
        hoverLink = _hoverLink;
    }
    
    public void updateHoverArea(MapArea area) {
        hoverArea = area;
    }
    
    public void updateHoverAgent(AgentBase agent) {
        hoverAgent = agent;
    }
    
    public void setShowNodes(boolean showNodes) {
        this.showNodes = showNodes;
    }

    public void setShowNodeNames(boolean showNodeNames) {
        this.showNodeLabels = showNodeNames;
    }
    
    public void setShowLinkNames(boolean showLinkNames) {
        this.showLinkLabels = showLinkNames;
    }

    public void setShowLinks(boolean showLinks) {
        this.showLinks = showLinks;
    }

    public void setShowArea(boolean showArea) {
        this.showArea = showArea;
    }

    public void setShowAreaNames(boolean showAreaNames) {
        this.showAreaLabels = showAreaNames;
    }

    public void setShowAgents(boolean showAgents) {
        this.showAgents = showAgents;
    }

    public void setShowAgentNames(boolean showAgentNames) {
        this.showAgentLabels = showAgentNames;
    }

    public void setAgentAppearances(ArrayList<AgentAppearance2D> agentAppearances) {
        this.agentAppearances = agentAppearances;
    }

    public CrowdWalkPropertiesHandler getProperties() {
        return properties;
    }
}
