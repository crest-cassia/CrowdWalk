package nodagumi.ananPJ.Gui;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Window;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle.ObstructerDisplay;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNode.NType;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase.TriageLevel;
import nodagumi.Itk.*;

/**
 * 3D シミュレーションパネル
 */
public class SimulationPanel3D extends StackPane {
    /**
     * 実在の地図ベースのマップであると判断する最小リンク数
     */
    private static final int MINIMUM_REAL_MAP_LINKS = 300;

    /**
     * マップをセンタリングする際に付加するマージンドット数
     * (計算誤差の為か実際にはこのサイズ丁度にはならない)
     */
    private static final int CENTERING_MARGIN = 32;

    /**
     * リンクを擬似的な1ドット幅のラインで表示する場合の、三角形の width に当たる辺の長さ(1mm)
     */
    private static double THIN_LINK_WIDTH = 0.001;

    /**
     * ノード表示用の Cylinder オブジェクトの高さ(10cm)
     */
    private static double NODE_THICKNESS = 0.1;

    /**
     * 視野角
     */
    private static double VIEW_ANGLE = 30.0;

    /**
     * マウスドラッグの感度
     */
    private static double DRAG_SENSITIVITY = 4.0;

    /**
     * ノード表示用の Cylinder オブジェクトを水平に表示するための回転変換オブジェクト
     */
    private static Rotate NODE_ROTATE = new Rotate(90, Rotate.X_AXIS);

    /**
     * 地図データ。
     */
    private NetworkMap networkMap;

    /**
     * 最後に検出したマウスカーソルの位置
     */
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;

    /**
     * 視点の高さ制御用 Group ノード
     */
    private Group elevationControl = new Group();

    /**
     * マップの回転制御用 Group ノード
     */
    private Group rotationControl = new Group();

    /**
     * マップのシェイプを構成する Group ノード
     */
    private Group mapGroup = new Group();

    /**
     * マップの背景地図を構成する Group ノード
     */
    private Group backgroundMapGroup = new Group();

    /**
     * マップのポリゴンを構成する Group ノード
     */
    private Group polygonGroup = new Group();

    /**
     * マップの構造物を構成する Group ノード
     */
    private Group structureGroup = new Group();

    /**
     * マップのリンクを構成する Group ノード
     */
    private Group linkGroup = new Group();
    private Group pickingLinkGroup = new Group();
    private Group thinLineGroup = new Group();
    private Group edgeLineGroup = new Group();

    /**
     * マップのノードを構成する Group ノード
     */
    private Group nodeGroup = new Group();

    /**
     * マップのエージェントを構成する Group ノード
     */
    private Group agentGroup = new Group();

    /**
     * マップのエリアを構成する Group ノード
     */
    private Group areaGroup = new Group();

    /**
     * ノード座標の平均値でマップをセンタリングする
     */
    private boolean centeringByNodeAverage = false;

    /**
     * 視点の高さを制御する平行移動オブジェクト
     */
    private Translate elevationTranslate;

    /**
     * マップの表示位置(中心座標)を制御する平行移動オブジェクト
     */
    private Translate mapTranslate;

    /**
     * マップの仰角を制御する回転変換オブジェクト
     */
    private Rotate mapRotateX;

    /**
     * マップの回転角を制御する回転変換オブジェクト
     */
    private Rotate mapRotateZ;

    /**
     * マップの拡大率
     */
    private double zoomScale = 1.0;

    /**
     * マップの拡大率を制御するスケールオブジェクト
     */
    private Scale mapScale;

    /**
     * 垂直スケール
     */
    private double verticalScale = 1.0;

    /**
     * 実際の道幅でリンクを表示する
     */
    private boolean atActualWidth = false;

    /**
     * リンクのデフォルトマテリアル
     */
    private PhongMaterial defaultLinkMaterial;

    /**
     * ホバー用のマテリアル
     */
    private PhongMaterial hoverMaterial;

    /**
     * リンクのピッキング用のマテリアル
     */
    private PhongMaterial pickingMaterial;

    /**
     * ピッキング情報表示用のペイン
     */
    private Pane pickingPane;

    /**
     * ピッキング情報表示用のラベル
     */
    private Label pickedObjectLabel = new Label();

    /**
     * 背景色
     */
    private Color backgroundColor;

    /**
     * エリアの表示色
     */
    private ObstructerDisplay obstructerDisplay = ObstructerDisplay.ORANGE;

    /**
     * エリアのアウトラインの表示色
     */
    private Color outlineColor;

    /**
     * エリア表示色の彩度 100% に相当する Obstructer level
     */
    private double pollutionColorSaturation = 0.0;

    /**
     * 歩行速度に応じてエージェントの色を変えるかどうか
     */
    private boolean changeAgentColorDependingOnSpeed = true;

    /**
     * シミュレーションの進捗状況の表示位置
     */
    private String showStatusPosition = "top";

    /**
     * シミュレーションの進捗状況表示用のラベル(top 位置)
     */
    private Label statusTop = new Label("NOT STARTED");

    /**
     * シミュレーションの進捗状況表示用のラベル(bottom 位置)
     */
    private Label statusBottom = new Label("NOT STARTED");

    /**
     * AIST ロゴ表示用のラベル
     */
    private Label logoLabel = new Label();

    /**
     * マップをセンタリングする際にマージンを付加するかどうか
     */
    private boolean marginAdded = false;

    /**
     * 視点を変える操作を許可するかどうか
     */
    private boolean viewPointOperationEnabled = true;

    /**
     * ポリゴン用のリンク
     */
    private MapLinkTable polygonLinks = new MapLinkTable();

    /**
     * 構造物用のリンク
     */
    private MapLinkTable structureLinks = new MapLinkTable();

    /**
     * ポリゴンと構造物を除いた正規のリンク
     */
    private MapLinkTable regularLinks = new MapLinkTable();

    /**
     * 領域
     */
    private ArrayList<MapArea> areas;

    /**
     * タグ別リンク表示スタイル
     */
    private LinkedHashMap<String, LinkAppearance3D> linkAppearances = new LinkedHashMap();

    /**
     * タグ別ノード表示スタイル
     */
    private LinkedHashMap<String, NodeAppearance3D> nodeAppearances = new LinkedHashMap();

    /**
     * リンクタグ別のポリゴン座標リスト
     */
    private HashMap<String, ArrayList<Point3D>> polygonPoints;

    /**
     * リンクとピッキング用 Shape オブジェクトとの対応付け
     */
    private HashMap<MapLink, Shape3D> pickingLinkShapes = new HashMap();

    /**
     * リンクと表示用 Shape オブジェクトとの対応付け
     */
    private HashMap<MapLink, Shape3D> linkShapes = new HashMap();

    /**
     * リンクと実際の道幅表示用 Shape オブジェクトとの対応付け
     */
    private HashMap<MapLink, Shape3D> edgeLinkShapes = new HashMap();

    /**
     * ノードと表示用 Shape オブジェクトとの対応付け
     */
    private HashMap<MapNode, Cylinder> nodeShapes = new HashMap();

    /**
     * ID でエージェントを参照するハッシュテーブル
     */
    private HashMap<String, AgentBase> agentMap = new HashMap();

    /**
     * エージェントと表示用 Shape オブジェクトとの対応付け
     */
    private HashMap<AgentBase, Shape3D> agentShapes = new HashMap();

    /**
     * 各エージェントのデフォルト表示色
     */
    private HashMap<AgentBase, Color> defaultColorOfAgents = new HashMap();

    /**
     * ID でエリアを参照するハッシュテーブル
     */
    private HashMap<String, MapArea> areaMap = new HashMap();

    /**
     * エリアと表示用 Shape オブジェクトとの対応付け
     */
    private HashMap<MapArea, Box> areaShapes = new HashMap();

    /**
     * ホバー情報
     */
    private String hoverMode = "None";
    private NType hoverType = null;
    private OBNode currentHoverObject = null;
    private Material currentMaterial = null;

    /**
     * ホバー表示用マウスイベントハンドラ
     */
    private EventHandler<MouseEvent> onMouseEntered = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            PickResult res = event.getPickResult();
            if (res.getIntersectedNode() != null && ! res.getIntersectedNode().getId().startsWith(hoverMode)) {
                return;
            }

            hoverOff();
            if (res.getIntersectedNode() != null) {
                hoverOn(res.getIntersectedNode().getId(), event.getScreenX(), event.getScreenY());
            }
            event.consume();
        }
    };

    /**
     * ホバー消去用マウスイベントハンドラ
     */
    private EventHandler<MouseEvent> onMouseExited = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            PickResult res = event.getPickResult();
            if (res.getIntersectedNode() != null && res.getIntersectedNode().getId() != null
                    && ! res.getIntersectedNode().getId().startsWith(hoverMode)) {
                return;
            }

            hoverOff();
            if (res.getIntersectedNode() != null && res.getIntersectedNode().getId() != null) {
                // 重なっている別のオブジェクト上に移動した場合
                hoverOn(res.getIntersectedNode().getId(), event.getScreenX(), event.getScreenY());
            }
            event.consume();
        }
    };

    /**
     * ステータス表示用マウスイベントハンドラ
     */
    private EventHandler<MouseEvent> onMouseClicked = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            PickResult res = event.getPickResult();
            if (res.getIntersectedNode() != null) {
                // ホバーが指しているオブジェクトを通知する
                notifyClicked();
            }
            event.consume();
        }
    };

    /**
     * ステータス表示用サブジェクト
     */
    private Observable observable = new Observable() {
        @Override
        public void notifyObservers(Object arg) {
            setChanged();
            super.notifyObservers(arg);
        }
    };

    /**
     * 3頂点ポリゴン
     */
    public class TrianglePolygon extends TriangleMesh {
        private final float[] texCoords = {
            1, 1,
            1, 0,
            0, 1,
            0, 0
        };
        private final int[] faces = {
            0, 0, 1, 1, 2, 2
        };

        public TrianglePolygon(Point3D[] vertices) {
            float[] points = new float[3 * 3];
            for (int index = 0; index < 3; index++) {
                points[index * 3] = (float)vertices[index].getX();
                points[index * 3 + 1] = (float)vertices[index].getY();
                points[index * 3 + 2] = (float)(vertices[index].getZ() * verticalScale);
            }
            this.getPoints().setAll(points);
            this.getTexCoords().setAll(texCoords);
            this.getFaces().setAll(faces);
        }
    }

    /**
     * 疑似的な4頂点ポリゴン
     */
    public class QuadPolygon extends TriangleMesh {
        private final float[] texCoords = {
            1, 1,
            1, 0,
            0, 1,
            0, 0
        };
        private final int[] faces = {
            2, 3, 0, 2, 1, 0,
            2, 3, 1, 0, 3, 1
        };

        public QuadPolygon(Point3D[] vertices) {
            float[] points = new float[4 * 3];
            for (int index = 0; index < 4; index++) {
                points[index * 3] = (float)vertices[index].getX();
                points[index * 3 + 1] = (float)vertices[index].getY();
                points[index * 3 + 2] = (float)(vertices[index].getZ() * verticalScale);
            }
            this.getPoints().setAll(points);
            this.getTexCoords().setAll(texCoords);
            this.getFaces().setAll(faces);
        }
    }

    /**
     * コンストラクタ
     */
    public SimulationPanel3D(int panelWidth, int panelHeight, NetworkMap networkMap, boolean atActualWidth,
            double verticalScale, CrowdWalkPropertiesHandler properties, ArrayList<GsiTile> mapTiles) {
        this.networkMap = networkMap;
        this.atActualWidth = atActualWidth;
        this.verticalScale = verticalScale;

        init(properties);

        // シーングラフの構築
        SubScene networkMapScene = createNetworkMapScene(panelWidth, panelHeight, mapTiles);
        getChildren().add(networkMapScene);
        getChildren().add(createLogoPane());
        pickingPane = createPickingPane();
        getChildren().add(pickingPane);
        getChildren().add(createStatusPane());

        // StackPane のリサイズに SubScene を追従させる
        // ※高さのリサイズ時にサブシーンのスケールが追従してしまうのは JavaFX の仕様(回避方法は見つからない)
        networkMapScene.heightProperty().bind(this.heightProperty());
        networkMapScene.widthProperty().bind(this.widthProperty());
        networkMapScene.setManaged(false);

        // シミュレーションビューのマウス操作定義
        final SimulationPanel3D pane = this;
        networkMapScene.setOnMousePressed(event -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        });
        networkMapScene.setOnMouseDragged(event -> {
            if (isViewPointOperationEnabled()) {
                double dx = event.getX() - lastMouseX;
                double dy = event.getY() - lastMouseY;
                double sightHeight = Math.tan((VIEW_ANGLE / 2.0) * Math.PI / 180.0) * elevationTranslate.getZ() * 2.0 / zoomScale;
                switch (event.getButton()) {
                case PRIMARY:
                    if (event.isControlDown()) {
                        // 縦横スクロール
                        scroll(dx * sightHeight / pane.getHeight(), dy * sightHeight / pane.getHeight());
                    } else {
                        // マウスの縦移動で俯瞰角度を変える
                        double angle = mapRotateX.getAngle() + dy / DRAG_SENSITIVITY;
                        // 自然な角度で見下ろせる範囲に限定する(Shift キー押下で解除)
                        if (! event.isShiftDown()) {
                            if (angle > 0.0) {
                                angle = 0.0;
                            } else if (angle < -90.0) {
                                angle = -90.0;
                            }
                        }
                        mapRotateX.setAngle(angle);

                        // マウスの横移動で地図を回転させる
                        dx /= DRAG_SENSITIVITY;
                        if (event.getY() > pane.getHeight() / 2) {
                            dx = -dx;
                        }
                        angle = mapRotateZ.getAngle() + dx;
                        if (angle > 180.0) {
                            angle -= 360.0;
                        } else if (angle < -180.0) {
                            angle += 360.0;
                        }
                        mapRotateZ.setAngle(angle);
                    }
                    break;
                case SECONDARY:
                    // 縦横スクロール
                    scroll(dx * sightHeight / pane.getHeight(), dy * sightHeight / pane.getHeight());
                    break;
                }
            }
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        });
        networkMapScene.setOnScroll(event -> {
            // 拡大・縮小
            if (! isViewPointOperationEnabled() || event.getDeltaY() == 0.0) {
                return;
            } else if (event.getDeltaY() > 0.0) {
                zoomScale *= 1.1;
            } else {
                zoomScale *= 0.9;
            }
            updateZoomScale();
        });
    }

    /**
     * 初期設定
     */
    public void init(CrowdWalkPropertiesHandler properties) {
        areas = networkMap.getAreas();

        // リンクの分別
        for (MapLink link : networkMap.getLinks()) {
            if (link.getFrom() == link.getTo()) {
                // TODO: 不正なリンクはマップを読み込んだ直後に削除した方がよい
                Itk.logWarn_("Looped link found", "ID=" + link.ID);
                continue;
            }
            if (link.hasSubTag("POLYGON")) {
                polygonLinks.add(link);
            } else if (link.hasSubTag("STRUCTURE")) {
                structureLinks.add(link);
            } else {
                regularLinks.add(link);
            }
        }

        // 実在の地形ベースのマップでなければセンタリングマージンを付加する
        setMarginAdded(regularLinks.size() < MINIMUM_REAL_MAP_LINKS);

        // リンクのデフォルトマテリアル
        defaultLinkMaterial = new PhongMaterial();
        defaultLinkMaterial.setDiffuseColor(FxColor.DEFAULT_LINK_COLOR);

        // ホバー用のマテリアル
        hoverMaterial = new PhongMaterial();
        hoverMaterial.setDiffuseColor(Color.BLUE);

        // リンクのピッキング用のマテリアル
        pickingMaterial = new PhongMaterial();
        pickingMaterial.setDiffuseColor(Color.TRANSPARENT);

        try {
            if (properties != null && properties.getPropertiesFile() != null) {
                String filePath = properties.getFilePath("link_appearance_file", null);
                if (filePath != null) {
                    LinkAppearance3D.load(new FileInputStream(filePath), linkAppearances);
                }
                LinkAppearance3D.load(getClass().getResourceAsStream("/link_appearance.json"), linkAppearances);
                // 設定ファイルに同じタグの定義が複数存在する場合に、記述が上にある方を優先させるために再ロードが必要
                if (filePath != null) {
                    LinkAppearance3D.load(new FileInputStream(filePath), linkAppearances);
                }

                filePath = properties.getFilePath("node_appearance_file", null);
                if (filePath != null) {
                    NodeAppearance3D.load(new FileInputStream(filePath), nodeAppearances);
                }
                NodeAppearance3D.load(getClass().getResourceAsStream("/node_appearance.json"), nodeAppearances);
                // 設定ファイルに同じタグの定義が複数存在する場合に、記述が上にある方を優先させるために再ロードが必要
                if (filePath != null) {
                    NodeAppearance3D.load(new FileInputStream(filePath), nodeAppearances);
                }

                backgroundColor = Color.web(properties.getString("background_color", "white"));
                obstructerDisplay = ObstructerDisplay.valueOf(properties.getString("pollution_color", "ORANGE",
                            ObstructerDisplay.getNames()).toUpperCase());
                outlineColor = Color.web(properties.getString("outline_color", "lime"));
                pollutionColorSaturation = properties.getDouble("pollution_color_saturation", 0.0);

                centeringByNodeAverage = properties.getBoolean("centering_by_node_average", centeringByNodeAverage);
            } else {
                LinkAppearance3D.load(getClass().getResourceAsStream("/link_appearance.json"), linkAppearances);
                NodeAppearance3D.load(getClass().getResourceAsStream("/node_appearance.json"), nodeAppearances);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * NetworkMap のシーングラフを作成する.
     */
    public SubScene createNetworkMapScene(int panelWidth, int panelHeight, ArrayList<GsiTile> mapTiles) {
        // 背景地図のシーングラフ
        createBackgroundMap(mapTiles);
        mapGroup.getChildren().add(backgroundMapGroup);

        // ポリゴンのシーングラフ
        polygonPoints = createPolygonPoints(polygonLinks);
        for (String tag : polygonPoints.keySet()) {
            addPolygon(tag, polygonPoints.get(tag));
        }
        mapGroup.getChildren().add(polygonGroup);

        // Obstructer のシーングラフ
        for (MapArea area : areas) {
            addMapArea(area);
            areaMap.put(area.getID(), area);
        }
        mapGroup.getChildren().add(areaGroup);

        // エージェントのシーングラフ(空)
        mapGroup.getChildren().add(agentGroup);

        // 構造物のシーングラフ
        for (MapLink link : structureLinks) {
            addStructure(link);
        }

        for (MapLink link : regularLinks) {
            // ピッキング用透明リンクのシーングラフ
            addPickingLink(link);
            // リンクのシーングラフ
            addLink(link, null);
        }
        createEdgeLineOfLinks();
        pickingLinkGroup.setVisible(false);     // TODO: true にセットするとリンクの表示が掠れてしまう
        thinLineGroup.setVisible(! atActualWidth);
        edgeLineGroup.setVisible(atActualWidth);
        linkGroup.getChildren().addAll(structureGroup, thinLineGroup, edgeLineGroup, pickingLinkGroup);
        mapGroup.getChildren().add(linkGroup);

        // ノードのシーングラフ
        for (MapNode node : networkMap.getNodes()) {
            addNode(node, null);
        }
        mapGroup.getChildren().add(nodeGroup);

        rotationControl.getChildren().add(mapGroup);
        elevationControl.getChildren().add(rotationControl);

        // シーングラフの transform 設定
        elevationTranslate = new Translate();           // 視点の高さ
        mapTranslate = new Translate();                 // マップの表示位置(中心座標)
        mapRotateX = new Rotate(0.0, Rotate.X_AXIS);    // 仰角
        mapRotateZ = new Rotate(0.0, Rotate.Z_AXIS);    // マップの回転
        mapScale = new Scale(1.0, 1.0, 1.0);            // 拡大率
        centering(true, panelWidth, panelHeight);
        elevationControl.getTransforms().addAll(elevationTranslate, mapRotateX);
        rotationControl.getTransforms().add(mapRotateZ);
        mapGroup.getTransforms().addAll(mapTranslate, mapScale);

        // カメラの定義(カメラは固定し、マップ側を動かすことで View を変化させる)
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(VIEW_ANGLE);
        camera.setVerticalFieldOfView(true);    // 画面の縦方向のサイズ変更に視野が追従する(視野角は一定)
        camera.setNearClip(1.0);
        camera.setFarClip(100000.0);

        // 照明はアンビエント光のみ(PointLight を使うと視線の角度によってオブジェクトの色(明度)が変わってしまうため)
        AmbientLight ambientLight = new AmbientLight(Color.WHITE);
        elevationControl.getChildren().add(ambientLight);

        SubScene subScene = new SubScene(elevationControl, panelWidth, panelHeight, true, SceneAntialiasing.BALANCED);
        subScene.setFill(backgroundColor);
        subScene.setCamera(camera);

        return subScene;
    }

    /**
     * ポリゴン用のリンクを返す
     */
    public MapLinkTable getPolygonLinks() {
        return polygonLinks;
    }

    /**
     * ポリゴン表示用のリンクを元にタグ別のポリゴン座標リストを作成する
     */
    public HashMap<String, ArrayList<Point3D>> createPolygonPoints(MapLinkTable links) {
        // タグごとにリンクを選別する
        HashMap<String, MapLinkTable> polygonLinksByTag = new HashMap();
        for (MapLink link : links) {
            for (String tag : link.getTags()) {
                if (tag.contains("POLYGON")) {
                    MapLinkTable _links = polygonLinksByTag.get(tag);
                    if (_links == null) {
                        _links = new MapLinkTable();
                        polygonLinksByTag.put(tag, _links);
                    }
                    _links.add(link);
                }
            }
        }
        // 選別したリンクを元に座標リストを作成する
        HashMap<String, ArrayList<Point3D>> polygonPoints = new HashMap();
        for (String tag : polygonLinksByTag.keySet()) {
            // 通過点順に MapNode を収集する
            MapLinkTable _links = polygonLinksByTag.get(tag);
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
            // 収集した MapNode の座標リストを作成する
            ArrayList<Point3D> points = new ArrayList();
            for (MapNode node : polygonNodes) {
                points.add(new Point3D(node.getX(), node.getY(), -node.getHeight()));
            }
            polygonPoints.put(tag, points);
        }
        return polygonPoints;
    }

    /**
     * ステータス表示用のペインを作成する.
     */
    private BorderPane createStatusPane() {
        statusTop.setVisible(false);
        statusTop.setStyle("-fx-background-color:white; -fx-label-padding:0 4 0 4;");
        statusBottom.setVisible(false);
        statusBottom.setStyle("-fx-background-color:white; -fx-label-padding:0 4 0 4;");

        BorderPane borderPane = new BorderPane();
        borderPane.setMouseTransparent(true);
        borderPane.setTop(statusTop);
        borderPane.setBottom(statusBottom);

        return borderPane;
    }

    /**
     * ピッキング情報表示用のペインを作成する.
     */
    private Pane createPickingPane() {
        pickedObjectLabel.setVisible(false);
        pickedObjectLabel.setStyle("-fx-background-color:white; -fx-label-padding:0 4 0 4;");

        Pane pane = new Pane();
        pane.setMouseTransparent(true);
        pane.getChildren().addAll(pickedObjectLabel);

        return pane;
    }

    /**
     * AIST ロゴ表示用のペインを作成する.
     */
    private BorderPane createLogoPane() {
        Image aistLogo = new Image(getClass().getResourceAsStream("/img/aist_logo.png"));
        logoLabel.setGraphic(new ImageView(aistLogo));
        logoLabel.setVisible(false);

        BorderPane borderPane = new BorderPane();
        borderPane.setMouseTransparent(true);
        borderPane.setBottom(logoLabel);
        BorderPane.setAlignment(logoLabel, Pos.CENTER_RIGHT);

        return borderPane;
    }

    /**
     * マップをセンタリングする
     */
    public void centering(boolean withScaling, double paneWidth, double paneHeight) {
        Rectangle2D bounds = networkMap.calcRectangle();
        Point2D centerOfMap = calcCenter(centeringByNodeAverage);

        double length = bounds.getHeight();
        if (bounds.getWidth() / bounds.getHeight() >= paneWidth / paneHeight) {
            length = bounds.getWidth() * paneHeight / paneWidth;
            if (isMarginAdded() && paneWidth > CENTERING_MARGIN) {
                length *= (paneWidth + CENTERING_MARGIN) / paneWidth;
            }
        } else {
            if (isMarginAdded() && paneHeight > CENTERING_MARGIN) {
                length *= (paneHeight + CENTERING_MARGIN) / paneHeight;
            }
        }
        if (isMarginAdded() && paneWidth > CENTERING_MARGIN && paneHeight > CENTERING_MARGIN) {
            double width = Math.min(paneWidth, paneHeight);
            length *= (width + CENTERING_MARGIN) / width;
        }
        double z = (length / 2.0) / Math.tan((VIEW_ANGLE / 2.0) * Math.PI / 180.0);

        elevationTranslate.setZ(z);
        mapRotateX.setAngle(0.0);
        mapRotateZ.setAngle(0.0);
        mapTranslate.setX(-centerOfMap.getX());
        mapTranslate.setY(-centerOfMap.getY());
        mapScale.setPivotX(centerOfMap.getX());
        mapScale.setPivotY(centerOfMap.getY());
        if (withScaling) {
            setZoomScale(1.0);
        }
    }

    /**
     * マップの中心点を求める.
     *
     * TODO: NetworkMap クラスに移動する
     */
    public Point2D calcCenter(boolean nodeAverage) {
        Rectangle2D bounds = networkMap.calcRectangle();
        double x = 0.0;
        double y = 0.0;
        if (nodeAverage) {
            // 元々の CrowdWalk で使われていた方法
            int count = 0;
            for (MapNode node : networkMap.getNodes()) {
                java.awt.geom.Point2D pos = node.getAbsoluteCoordinates();
                x += pos.getX();
                y += pos.getY();
                count++;
            }
            x /= count;
            y /= count;
        } else {
            x = bounds.getMinX() + bounds.getWidth() / 2.0;
            y = bounds.getMinY() + bounds.getHeight() / 2.0;
        }
        // return new Point2D.Double(x, y);
        return new Point2D(x, y);
    }

    /**
     * マップを縦横スクロールする.
     */
    private void scroll(double dx, double dy) {
        double r = Math.toRadians(-mapRotateZ.getAngle());
        double x = dx * Math.cos(r) - dy * Math.sin(r);
        double y = dx * Math.sin(r) + dy * Math.cos(r);

        mapTranslate.setX(mapTranslate.getX() + x);
        mapTranslate.setY(mapTranslate.getY() + y);
        mapScale.setPivotX(mapScale.getPivotX() - x);
        mapScale.setPivotY(mapScale.getPivotY() - y);
    }

    /**
     * マップの表示位置を制御する平行移動オブジェクトを返す
     */
    public Translate getTranslate() { return mapTranslate; }

    /**
     * マップの表示位置を制御する平行移動オブジェクトを設定する
     */
    public void setTranslate(Point3D translate) {
        mapTranslate.setX(translate.getX());
        mapTranslate.setY(translate.getY());
        mapTranslate.setZ(translate.getZ());
    }

    /**
     * スケーリングの中心点を設定する
     */
    public void setPivotOfScale(Point3D pivot) {
        mapScale.setPivotX(pivot.getX());
        mapScale.setPivotY(pivot.getY());
    }

    /**
     * スケーリングの中心点を返す
     */
    public Point3D getPivotOfScale() {
        return new Point3D(mapScale.getPivotX(), mapScale.getPivotY(), 0.0);
    }

    /**
     * マップの仰角を返す
     */
    public double getRotateX() { return mapRotateX.getAngle(); }

    /**
     * マップの仰角を設定する
     */
    public void setRotateX(double angle) { mapRotateX.setAngle(angle); }

    /**
     * マップの回転角を返す
     */
    public double getRotateZ() { return mapRotateZ.getAngle(); }

    /**
     * マップの回転角を設定する
     */
    public void setRotateZ(double angle) { mapRotateZ.setAngle(angle); }

    /**
     * マップの拡大率を返す
     */
    public double getZoomScale() { return zoomScale; }

    /**
     * マップの拡大率を設定する
     */
    public void setZoomScale(double zoomScale) {
        this.zoomScale = zoomScale;
        updateZoomScale();
    }

    /**
     * マップの拡大率を更新する
     */
    public void updateZoomScale() {
        mapScale.setX(zoomScale);
        mapScale.setY(zoomScale);
        mapScale.setZ(zoomScale);
    }

    /**
     * 垂直スケールを変更する
     */
    public void changeVerticalScale(double verticalScale, final double agentSize, Window owner) {
        if (verticalScale == this.verticalScale) {
            return;
        }
        this.verticalScale = verticalScale;
        final Alert alert = new Alert(AlertType.NONE, "Please wait a moment.", ButtonType.CLOSE);
        alert.initOwner(owner);
        alert.setTitle("Message");
        alert.show();
        Platform.runLater(() -> {
            rebuildPolygonGroup();
            rebuildStructureGroup();
            rebuildLinkGroup();
            updateNodesHeight();
            updateAgentsHeight(agentSize);
            alert.hide();
        });
    }

    /**
     * 視点を更新する(カメラワーク用).
     */
    public void updateTransform(Point3D translate, Point3D pivot, double rotateX, double rotateZ, double zoom) {
        if (! pointEquals(translate, getTranslate())) {
            setTranslate(translate);
        }
        if (rotateX != getRotateX()) {
            setRotateX(rotateX);
        }
        if (rotateZ != getRotateZ()) {
            setRotateZ(rotateZ);
        }
        setPivotOfScale(pivot);
        if (zoom != getZoomScale()) {
            setZoomScale(zoom);
        }
    }

    /**
     * 互いの x, y, z 値がすべて等しいか?
     */
    public boolean pointEquals(Point3D p, Translate t) {
        return p.getX() == t.getX() && p.getY() == t.getY() && p.getZ() == t.getZ();
    }

    /**
     * ホバー対象オブジェクトの種類をセットする
     */
    public synchronized void setHoverMode(String hoverMode) {
        this.hoverMode = hoverMode;
        pickingLinkGroup.setVisible(hoverMode.equals("Link"));
    }

    /**
     * ホバーを表示する
     */
    private synchronized void hoverOn(String id, double x, double y) {
        currentHoverObject = null;

        String[] words = id.split(" ");
        if (words[0].equals("Node")) {
            hoverType = NType.NODE;
            currentHoverObject = networkMap.getObject(words[1]);
        } else if (words[0].equals("Link")) {
            hoverType = NType.LINK;
            currentHoverObject = networkMap.getObject(words[1]);
        } else if (words[0].equals("Area")) {
            hoverType = NType.AREA;
            currentHoverObject = areaMap.get(words[1]);
        } else if (words[0].equals("Agent")) {
            hoverType = NType.AGENT;
            currentHoverObject = agentMap.get(words[1]);
        }
        if (currentHoverObject == null) {
            hoverType = null;
            return;
        }

        Shape3D shape = null;
        switch (hoverType) {
        case NODE:
            shape = nodeShapes.get(currentHoverObject);
            break;
        case LINK:
            shape = pickingLinkShapes.get(currentHoverObject);
            break;
        case AREA:
            shape = areaShapes.get(currentHoverObject);
            break;
        case AGENT:
            shape = agentShapes.get(currentHoverObject);
            break;
        }
        if (shape == null) {
            currentHoverObject = null;
            hoverType = null;
            return;
        }

        switch (hoverType) {
        case NODE:
        case LINK:
            currentMaterial = shape.getMaterial();
            shape.setMaterial(hoverMaterial);
            break;
        case AREA:
        case AGENT:
            PhongMaterial material = (PhongMaterial)shape.getMaterial();
            material.setDiffuseColor(Color.BLUE);
            break;
        }

        // ID + タグを表示
        Point2D panePosition = pickingPane.localToScreen(0.0, 0.0);
        pickedObjectLabel.relocate(x - panePosition.getX() + 16, y - panePosition.getY());
        String tag = currentHoverObject.getTagString();
        pickedObjectLabel.setText(currentHoverObject.getID() + (tag.isEmpty() ? "" : " : " + tag));
        pickedObjectLabel.setVisible(true);
    }

    /**
     * ホバー表示を消す
     */
    public synchronized void hoverOff() {
        if (hoverType == null) {
            return;
        }
        Shape3D shape = null;
        switch (hoverType) {
        case NODE:
            shape = nodeShapes.get(currentHoverObject);
            if (shape != null) {
                shape.setMaterial(currentMaterial);
            }
            break;
        case LINK:
            shape = pickingLinkShapes.get(currentHoverObject);
            if (shape != null) {
                shape.setMaterial(currentMaterial);
            }
            break;
        case AREA:
            updateAreaLevel((MapArea)currentHoverObject);
            break;
        case AGENT:
            updateAgentsColor((AgentBase)currentHoverObject);
            break;
        }
        pickedObjectLabel.setVisible(false);
        hoverType = null;
        currentHoverObject = null;
    }

    /**
     * ホバー表示を消した事にする.
     *
     * ※ホバー表示対象オブジェクトがすでに削除されている場合に使用する。
     */
    private synchronized void abortHover(OBNode object) {
        if (object == currentHoverObject) {
            hoverType = null;
            currentHoverObject = null;
        }
    }

    /**
     * ステータス表示用サブジェクトにオブザーバーを追加する
     */
    public void addObserver(Observer observer) {
        observable.addObserver(observer);
    }

    /**
     * ホバー表示中のオブジェクトがクリックされた事をオブザーバーに通知する
     */
    private synchronized void notifyClicked() {
        if (hoverType != null && observable.countObservers() > 0) {
            observable.notifyObservers(currentHoverObject);
        }
    }

    /**
     * 背景地図のシーングラフを作成する
     */
    public void createBackgroundMap(ArrayList<GsiTile> mapTiles) {
        if (mapTiles == null || mapTiles.isEmpty()) {
            return;
        }

        // 使用する地理院タイルを一枚の画像にまとめる
        int minTileNumberX = Integer.MAX_VALUE;
        int minTileNumberY = Integer.MAX_VALUE;
        int maxTileNumberX = 0;
        int maxTileNumberY = 0;
        for (GsiTile mapTile : mapTiles) {
            minTileNumberX = Math.min(mapTile.getTileNumberX(), minTileNumberX);
            maxTileNumberX = Math.max(mapTile.getTileNumberX(), maxTileNumberX);
            minTileNumberY = Math.min(mapTile.getTileNumberY(), minTileNumberY);
            maxTileNumberY = Math.max(mapTile.getTileNumberY(), maxTileNumberY);
        }
        int width = (maxTileNumberX - minTileNumberX + 1) * GsiTile.GSI_TILE_SIZE;
        int height = (maxTileNumberY - minTileNumberY + 1) * GsiTile.GSI_TILE_SIZE;
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (GsiTile mapTile : mapTiles) {
            String filePath = new File(mapTile.getFilePath()).toURI().toString();
            Image tileImage = new Image(filePath);
            PixelReader reader = tileImage.getPixelReader();
            int baseX = (mapTile.getTileNumberX() - minTileNumberX) * GsiTile.GSI_TILE_SIZE;
            int baseY = (mapTile.getTileNumberY() - minTileNumberY) * GsiTile.GSI_TILE_SIZE;
            for (int y = 0; y < GsiTile.GSI_TILE_SIZE; y++) {
                for (int x = 0; x < GsiTile.GSI_TILE_SIZE; x++) {
                    writer.setColor(baseX + x, baseY + y, reader.getColor(x, y));
                }
            }
        }

        float[] texCoords = {
            0, 0,
            0, 1,
            1, 1,
            1, 0
        };
        int[] faces = {
            0, 0, 1, 1, 2, 2,
            2, 2, 3, 3, 0, 0
        };

        java.awt.geom.Point2D[] vertices = new java.awt.geom.Point2D[4];
        for (GsiTile mapTile : mapTiles) {
            if (mapTile.getTileNumberX() == minTileNumberX && mapTile.getTileNumberY() == minTileNumberY) {
                vertices[0] = mapTile.getPoint();
            }
            if (mapTile.getTileNumberX() == minTileNumberX && mapTile.getTileNumberY() == maxTileNumberY) {
                vertices[1] = mapTile.getLowerLeftPoint();
            }
            if (mapTile.getTileNumberX() == maxTileNumberX && mapTile.getTileNumberY() == minTileNumberY) {
                vertices[3] = mapTile.getUpperRightPoint();
            }
            if (mapTile.getTileNumberX() == maxTileNumberX && mapTile.getTileNumberY() == maxTileNumberY) {
                vertices[2] = mapTile.getLowerRightPoint();
            }
        }
        float[] points = new float[4 * 3];
        for (int index = 0; index < 4; index++) {
            points[index * 3] = (float)vertices[index].getX();
            points[index * 3 + 1] = (float)vertices[index].getY();
            points[index * 3 + 2] = 1.0f;   // 標高 -1.0m
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);

        Shape3D shape = new MeshView(mesh);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(image);
        shape.setMaterial(material);

        backgroundMapGroup.getChildren().add(shape);
    }

    /**
     * ポリゴンをシーングラフに追加する
     */
    public void addPolygon(String tag, ArrayList<Point3D> points) {
        Color color = Color.GRAY;
        if (tag.contains("OCEAN")) {
            color = FxColor.SLATEBLUE;
        } else if (tag.contains("STRUCTURE")) {
            color = FxColor.LIGHTGRAY;
        }
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(color);

        Point3D basePoint = null;
        Point3D secondPoint = null;
        Point3D[] vertices = new Point3D[3];
        for (Point3D point : points) {
            if (basePoint == null) {
                basePoint = point;
                vertices[0] = basePoint;
            } else if (secondPoint == null) {
                secondPoint = point;
            } else {
                vertices[1] = secondPoint;
                vertices[2] = point;
                MeshView shape = new MeshView(new TrianglePolygon(vertices));
                shape.setDrawMode(DrawMode.FILL);
                shape.setCullFace(CullFace.NONE);
                shape.setMaterial(material);
                polygonGroup.getChildren().add(shape);
                secondPoint = point;
            }
        }
    }

    /**
     * ポリゴンのシーングラフを再構築する.
     */
    public void rebuildPolygonGroup() {
        polygonGroup.getChildren().clear();
        for (String tag : polygonPoints.keySet()) {
            addPolygon(tag, polygonPoints.get(tag));
        }
    }

    /**
     * ポリゴンの表示/非表示を切り替える
     */
    public void setShow3dPolygon(boolean visible) {
        polygonGroup.setVisible(visible);
    }

    /**
     * 構造物をシーングラフに追加する.
     */
    public void addStructure(MapLink link) {
        double width = link.getWidth();
        PhongMaterial material = defaultLinkMaterial;
        LinkAppearance3D linkAppearance = getLinkAppearance(link);
        if (linkAppearance != null) {
            width = linkAppearance.widthFixed ?
                linkAppearance.widthRatio : link.getWidth() * linkAppearance.widthRatio;
            material = linkAppearance.material;
        }

        // 中を塗りつぶした長方形
        Shape3D shape = new MeshView(new QuadPolygon(calcVertices(link.getFrom(), link.getTo(), width, 0.0)));
        shape.setDrawMode(DrawMode.FILL);
        shape.setMaterial(material);

        structureGroup.getChildren().add(shape);
    }

    /**
     * 構造物のシーングラフを再構築する.
     */
    public void rebuildStructureGroup() {
        structureGroup.getChildren().clear();
        for (MapLink link : structureLinks) {
            addStructure(link);
        }
    }

    /**
     * ノードをシーングラフに追加する
     */
    public boolean addNode(MapNode node, ArrayList<String> tags) {
        Cylinder shape = nodeShapes.get(node);
        if (shape == null) {
            NodeAppearance3D nodeAppearance = null;
            if (tags == null) {
                nodeAppearance = getNodeAppearance(node);
            } else {
                nodeAppearance = getNodeAppearance(tags);
            }
            if (nodeAppearance == null) {
                // 見えないノードは追加不要
                return false;
            }

            // ノード表示用の Shape オブジェクトを作成する
            shape = new Cylinder(nodeAppearance.diameter / 2.0, NODE_THICKNESS);
            shape.setMaterial(nodeAppearance.material);
            java.awt.geom.Point2D pos = node.getAbsoluteCoordinates();
            shape.setTranslateX(pos.getX());
            shape.setTranslateY(pos.getY());
            shape.setTranslateZ(-node.getHeight() * verticalScale);
            shape.getTransforms().add(NODE_ROTATE);

            // ピッキングを有効にする
            shape.setId("Node " + node.getID());
            shape.setOnMouseEntered(onMouseEntered);
            shape.setOnMouseExited(onMouseExited);
            shape.setOnMouseClicked(onMouseClicked);

            nodeGroup.getChildren().add(shape);
            nodeShapes.put(node, shape);
            return true;
        }
        return false;
    }

    /**
     * ノードをシーングラフから削除する
     */
    public boolean removeNode(MapNode node) {
        Cylinder shape = nodeShapes.get(node);
        if (shape != null) {
            abortHover(node);
            nodeShapes.remove(node);
            nodeGroup.getChildren().remove(shape);
            return true;
        }
        return false;
    }

    /**
     * 全ノードの Z 位置を更新する
     */
    public void updateNodesHeight() {
        for (Map.Entry<MapNode, Cylinder> entry : nodeShapes.entrySet()) {
            MapNode node = entry.getKey();
            Cylinder shape = entry.getValue();
            shape.setTranslateZ(-node.getHeight() * verticalScale);
        }
    }

    /**
     * ノードの表示/非表示を切り替える
     */
    public void setShowNodes(boolean showNodes) {
        nodeGroup.setVisible(showNodes);
    }

    /**
     * node に振られたタグにマッチする NodeAppearance3D を返す.
     */
    public NodeAppearance3D getNodeAppearance(MapNode node) {
        return NodeAppearance3D.getAppearance(nodeAppearances, node);
    }

    /**
     * tags にマッチする NodeAppearance3D を返す.
     */
    public NodeAppearance3D getNodeAppearance(ArrayList<String> tags) {
        return NodeAppearance3D.getAppearance(nodeAppearances, tags);
    }

    /**
     * ピッキング用の透明リンクをシーングラフに追加する.
     */
    public boolean addPickingLink(MapLink link) {
        Shape3D shape = pickingLinkShapes.get(link);
        if (shape != null) {
            return false;
        }

        // 中を塗りつぶした長方形
        shape = new MeshView(new QuadPolygon(calcVertices(link, -0.1)));     // 10cm 上方に配置する
        shape.setDrawMode(DrawMode.FILL);
        shape.setMaterial(pickingMaterial);

        // ピッキングを有効にする
        shape.setId("Link " + link.getID());
        shape.setOnMouseEntered(onMouseEntered);
        shape.setOnMouseExited(onMouseExited);
        shape.setOnMouseClicked(onMouseClicked);

        pickingLinkGroup.getChildren().add(shape);
        pickingLinkShapes.put(link, shape);
        return true;
    }

    /**
     * リンクをシーングラフに追加する.
     *
     * 1ドット幅のラインで表示する場合もポリゴンを使用する。(他に方法が見つからないため)
     */
    public boolean addLink(MapLink link, ArrayList<String> tags) {
        Shape3D shape = linkShapes.get(link);
        if (shape != null) {
            return false;
        }

        double width = THIN_LINK_WIDTH;
        PhongMaterial material = defaultLinkMaterial;
        LinkAppearance3D linkAppearance = null;
        if (tags == null) {
            linkAppearance = getLinkAppearance(link);
        } else {
            linkAppearance = getLinkAppearance(tags);
        }
        if (linkAppearance != null) {
            width = linkAppearance.widthFixed ?
                linkAppearance.widthRatio : link.getWidth() * linkAppearance.widthRatio;
            material = linkAppearance.material;
        }
        if (width == THIN_LINK_WIDTH) {
            // 線分と見分けが付かない超鋭角の三角形
            shape = new MeshView(new TrianglePolygon(calcVertices(link.getFrom(), link.getTo(), width, 0.0)));
            shape.setDrawMode(DrawMode.LINE);
            shape.setCullFace(CullFace.NONE);    // これがないと時々表示が消える
        } else {
            // 中を塗りつぶした長方形
            shape = new MeshView(new QuadPolygon(calcVertices(link.getFrom(), link.getTo(), width, 0.0)));
            shape.setDrawMode(DrawMode.FILL);
        }
        shape.setMaterial(material);

        thinLineGroup.getChildren().add(shape);
        linkShapes.put(link, shape);
        return true;
    }

    /**
     * リンクをシーングラフから削除する
     */
    public boolean removeLink(MapLink link) {
        Shape3D shape = pickingLinkShapes.get(link);
        if (shape != null) {
            abortHover(link);
            pickingLinkShapes.remove(link);
            linkShapes.remove(link);
            pickingLinkGroup.getChildren().remove(shape);
            shape = linkShapes.get(link);
            thinLineGroup.getChildren().remove(shape);

            // edgeLineGroup の色付きリンク
            shape = edgeLinkShapes.get(link);
            if (shape != null) {
                edgeLinkShapes.remove(link);
                edgeLineGroup.getChildren().remove(shape);
            }
            return true;
        }
        return false;
    }

    /**
     * リンクのシーングラフを再構築する.
     */
    public void rebuildLinkGroup() {
        hoverOff();
        pickingLinkShapes.clear();
        linkShapes.clear();
        edgeLinkShapes.clear();
        pickingLinkGroup.getChildren().clear();
        thinLineGroup.getChildren().clear();
        edgeLineGroup.getChildren().clear();
        for (MapLink link : regularLinks) {
            addPickingLink(link);
            addLink(link, null);
        }
        createEdgeLineOfLinks();
    }

    /**
     * リンクの表示/非表示を切り替える
     */
    public void setShowLinks(boolean showLinks) {
        linkGroup.setVisible(showLinks);
    }

    /**
     * link に振られたタグにマッチする LinkAppearance3D を返す.
     */
    public LinkAppearance3D getLinkAppearance(MapLink link) {
        return LinkAppearance3D.getAppearance(linkAppearances, link);
    }

    /**
     * tags にマッチする LinkAppearance3D を返す.
     */
    public LinkAppearance3D getLinkAppearance(ArrayList<String> tags) {
        return LinkAppearance3D.getAppearance(linkAppearances, tags);
    }

    /**
     * リンクをポリゴン表示するための頂点座標を求める
     */
    public Point3D[] calcVertices(MapNode from, MapNode to, double width, double dz) {
        Point3D p1 = new Point3D(to.getX() - from.getX(), to.getY() - from.getY(), 0);
        Point3D p2 = p1.normalize().crossProduct(0, 0, width / 2.0);
        double dx = p2.getX();
        double dy = p2.getY();

        Point3D[] vertices = new Point3D[4];
        vertices[0] = new Point3D(from.getX() + dx, from.getY() + dy, -from.getHeight() + dz);
        vertices[1] = new Point3D(from.getX() - dx, from.getY() - dy, -from.getHeight() + dz);
        vertices[3] = new Point3D(to.getX() - dx, to.getY() - dy, -to.getHeight() + dz);
        vertices[2] = new Point3D(to.getX() + dx, to.getY() + dy, -to.getHeight() + dz);
        return vertices;
    }

    /**
     * リンクをポリゴン表示するための頂点座標を求める
     */
    public Point3D[] calcVertices(MapLink link, double dz) {
        return calcVertices(link.getFrom(), link.getTo(), link.getWidth(), dz);
    }

    /**
     * リンクをポリゴン表示するための頂点座標を求める
     */
    public Point3D[] calcVertices(Point3D point1, Point3D point2, double width, double dz) {
        Point3D p1 = new Point3D(point2.getX() - point1.getX(), point2.getY() - point1.getY(), 0);
        Point3D p2 = p1.normalize().crossProduct(0, 0, width / 2.0);
        double dx = p2.getX();
        double dy = p2.getY();

        Point3D[] vertices = new Point3D[4];
        vertices[0] = new Point3D(point1.getX() + dx, point1.getY() + dy, -point1.getZ() + dz);
        vertices[1] = new Point3D(point1.getX() - dx, point1.getY() - dy, -point1.getZ() + dz);
        vertices[3] = new Point3D(point2.getX() - dx, point2.getY() - dy, -point2.getZ() + dz);
        vertices[2] = new Point3D(point2.getX() + dx, point2.getY() + dy, -point2.getZ() + dz);
        return vertices;
    }

    /**
     * 道幅を持ったリンクのシーングラフを作成する.
     */
    private void createEdgeLineOfLinks() {
        // ノードに接続されたリンク(正規のリンクのみ)
        HashMap<MapNode, ArrayList<MapLink>> regularLinksAtNode = new HashMap();
        for (MapNode node : networkMap.getNodes()) {
            ArrayList<MapLink> links = new ArrayList();
            for (MapLink link : node.getLinks()) {
                if (link.getOther(node) == node) {
                    // TODO: ループしたリンクはマップを読み込んだ直後に削除した方がよい
                    continue;
                }
                if (! (link.hasSubTag("POLYGON") || link.hasSubTag("STRUCTURE"))) {
                    links.add(link);
                }
            }
            regularLinksAtNode.put(node, links);
        }

        // 道のかどを示す座標を求める
        HashMap<String, Point2D> points = new HashMap();
        for (MapNode node : networkMap.getNodes()) {
            ArrayList<MapLink> links = regularLinksAtNode.get(node);
            if (links.size() <= 1) {
                continue;
            }

            Point2D nodePoint = new Point2D(node.getX(), node.getY());
            for (int index = 0; index < links.size(); index++) {
                MapLink link = links.get(index);
                MapLink prevLink = links.get((index + links.size() - 1) % links.size());
                MapLink nextLink = links.get((index + 1) % links.size());
                MapNode prevOppositeNode = prevLink.getOther(node);
                MapNode oppositeNode = link.getOther(node);
                MapNode nextOppositeNode = nextLink.getOther(node);
                Point2D oppositeNodePoint = new Point2D(oppositeNode.getX(), oppositeNode.getY());

                Point3D p1 = new Point3D(oppositeNode.getX() - node.getX(), oppositeNode.getY() - node.getY(), 0);
                Point3D p2 = p1.normalize().crossProduct(0, 0, link.getWidth() / 2.0);
                double dx = p2.getX();
                double dy = p2.getY();
                // links は node を中心として -π を起点とした時計回りの順にソートされている
                // link 直線に dx, dy をプラスしたものが道幅の(links 順に見て)起点側の縁を表す
                // よって link 直線に dx, dy をマイナスしたものと、次の link 直線に dx, dy をプラスしたものの交点座標が、かど座標となる
                Point2D a1 = new Point2D(node.getX() - dx, node.getY() - dy);
                Point2D a2 = new Point2D(oppositeNode.getX() - dx, oppositeNode.getY() - dy);

                p1 = new Point3D(nextOppositeNode.getX() - node.getX(), nextOppositeNode.getY() - node.getY(), 0);
                p2 = p1.normalize().crossProduct(0, 0, nextLink.getWidth() / 2.0);
                dx = p2.getX();
                dy = p2.getY();
                Point2D b1 = new Point2D(node.getX() + dx, node.getY() + dy);
                Point2D b2 = new Point2D(nextOppositeNode.getX() + dx, nextOppositeNode.getY() + dy);

                double prevAngle = angle(prevOppositeNode, node, oppositeNode);
                double angle = angle(oppositeNode, node, nextOppositeNode);
                if (
                    // 曲がり角が突き出てしまう
                    angle <= 30.0 && links.size() == 2
                    // 交点座標が遙か彼方になってしまうかもしれない
                    || angle >= 178.0
                    // 道幅の差が大きいためきれいに繋がらない
                    || angle >= 165.0 && (Math.max(link.getWidth(), nextLink.getWidth()) / Math.min(link.getWidth(), nextLink.getWidth())) > 1.5
                ) {
                    points.put(link.ID + " " + node.ID + " L", a1);
                    points.put(nextLink.ID + " " + node.ID + " R", b1);
                } else {
                    Point2D intersectionPoint = intersection(a1, a2, b1, b2);
                    if (
                        // 交点座標が道幅の1.5倍以上ノードから離れている
                        nodePoint.distance(intersectionPoint) > Math.max(link.getWidth(), nextLink.getWidth()) * 1.5
                        // 交点座標がノードよりも先にはみ出ている
                        && nodePoint.distance(intersectionPoint) > nodePoint.distance(oppositeNodePoint)
                    ) {
                        points.put(link.ID + " " + node.ID + " L", a1);
                        points.put(nextLink.ID + " " + node.ID + " R", b1);
                    } else {
                        points.put(link.ID + " " + node.ID + " L", intersectionPoint);
                        points.put(nextLink.ID + " " + node.ID + " R", intersectionPoint);
                    }
                }
                if (regularLinksAtNode.get(oppositeNode).size() == 1) {
                    points.put(link.ID + " " + oppositeNode.ID + " R", a2);
                }
                if (regularLinksAtNode.get(nextOppositeNode).size() == 1) {
                    points.put(nextLink.ID + " " + nextOppositeNode.ID + " L", b2);
                }
            }
        }

        // リンクを描画する
        for (MapLink link : regularLinks) {
            Point2D a1 = points.get(link.ID + " " + link.getFrom().ID + " L");
            Point2D a2 = points.get(link.ID + " " + link.getTo().ID + " R");
            Point2D b1 = points.get(link.ID + " " + link.getFrom().ID + " R");
            Point2D b2 = points.get(link.ID + " " + link.getTo().ID + " L");
            if (a1 == null || a2 == null || b1 == null || b2 == null) {
                Itk.logWarn_("Link can not be rendered", "ID=" + link.ID, link.getTags().toString() + " " + a1 + ", " + a2 + ", " + b1 + ", " + b2);
                continue;
            }

	    Point3D point1 = new Point3D(a1.getX(), a1.getY(), link.getFrom().getHeight());
	    Point3D point2 = new Point3D(a2.getX(), a2.getY(), link.getTo().getHeight());
            Shape3D shape = new MeshView(new TrianglePolygon(calcVertices(point1, point2, THIN_LINK_WIDTH, 0.0)));
            shape.setDrawMode(DrawMode.LINE);
            shape.setCullFace(CullFace.NONE);
            shape.setMaterial(defaultLinkMaterial);
            edgeLineGroup.getChildren().add(shape);

	    Point3D point3 = new Point3D(b1.getX(), b1.getY(), link.getFrom().getHeight());
	    Point3D point4 = new Point3D(b2.getX(), b2.getY(), link.getTo().getHeight());
            shape = new MeshView(new TrianglePolygon(calcVertices(point3, point4, THIN_LINK_WIDTH, 0.0)));
            shape.setDrawMode(DrawMode.LINE);
            shape.setCullFace(CullFace.NONE);
            shape.setMaterial(defaultLinkMaterial);
            edgeLineGroup.getChildren().add(shape);

            addEdgeLink(link);
        }
    }

    /**
     * 道幅を持ったリンクに色を付ける
     */
    public boolean addEdgeLink(MapLink link) {
        Shape3D shape = edgeLinkShapes.get(link);
        if (shape != null) {
            return false;
        }
        LinkAppearance3D linkAppearance = getLinkAppearance(link);
        if (linkAppearance == null) {
            return false;
        }
        shape = new MeshView(new QuadPolygon(calcVertices(link, 0.0)));
        shape.setDrawMode(DrawMode.FILL);
        shape.setMaterial(linkAppearance.material);

        edgeLineGroup.getChildren().add(shape);
        edgeLinkShapes.put(link, shape);
        return true;
    }

    /**
     * 3点のノードがなす角度を求める.
     *
     * 0.0～180.0 を返す
     */
    private double angle(MapNode nodeA, MapNode nodeB, MapNode nodeC) {
        Point2D pointA = new Point2D(nodeA.getX(), nodeA.getY());
        Point2D pointB = new Point2D(nodeB.getX(), nodeB.getY());
        Point2D pointC = new Point2D(nodeC.getX(), nodeC.getY());
        return pointB.angle(pointA, pointC);
    }

    /**
     * 2直線の交点座標を求める.
     *
     * ※直線 a, b は平行ではないこと。
     */
    public Point2D intersection(Point2D a1, Point2D a2, Point2D b1, Point2D b2) {
        double f1 = a2.getX() - a1.getX();
        double g1 = a2.getY() - a1.getY();
        double f2 = b2.getX() - b1.getX();
        double g2 = b2.getY() - b1.getY();
        double dx = b1.getX() - a1.getX();
        double dy = b1.getY() - a1.getY();
        double t1 = (f2 * dy - g2 * dx) / (f2 * g1 - f1 * g2);
        return new Point2D(a1.getX() + f1 * t1, a1.getY() + g1 * t1);
    }

    /**
     * エリアをシーングラフに追加する.
     *
     * エリアは標高 0m に固定された平面とする。(レベルを高さに反映させても見づらいだけだった)
     */
    public boolean addMapArea(MapArea area) {
        Box shape = areaShapes.get(area);
        if (shape != null) {
            return false;
        }
        // エリア表示用の Shape オブジェクトを作成する
        Rectangle2D bounds = (Rectangle2D)area.getShape();
        shape = new Box(bounds.getWidth(), bounds.getHeight(), 0.1);

        PhongMaterial material = new PhongMaterial();
        Color color = getAreaColor(area.getPollutionLevel().getCurrentLevel(),
                area.getPollutionLevel().getNormalizedLevel());
        material.setDiffuseColor(color);
        shape.setMaterial(material);

        shape.setTranslateX(bounds.getMinX() + bounds.getWidth() / 2.0);
        shape.setTranslateY(bounds.getMinY() + bounds.getHeight() / 2.0);
        shape.setTranslateZ(0.5);   // リンクに被らない様に 50cm 下げる

        // ピッキングを有効にする
        shape.setId("Area " + area.getID());
        shape.setOnMouseEntered(onMouseEntered);
        shape.setOnMouseExited(onMouseExited);
        shape.setOnMouseClicked(onMouseClicked);

        areaGroup.getChildren().add(shape);
        areaShapes.put(area, shape);
        return true;
    }

    /**
     * エリアのレベルを変更する
     */
    public void changeAreaLevel(MapArea area, double currentLevel, double normalizedLevel) {
        Box shape = areaShapes.get(area);
        Color color = getAreaColor(currentLevel, normalizedLevel);
        PhongMaterial material = (PhongMaterial)shape.getMaterial();
        if (! material.getDiffuseColor().equals(color)) {
            material.setDiffuseColor(color);
        }
    }

    /**
     * エリアのレベルを更新する
     */
    public void updateAreaLevel(MapArea area) {
        double currentLevel = area.getPollutionLevel().getCurrentLevel();
        double normalizedLevel = area.getPollutionLevel().getNormalizedLevel();
        changeAreaLevel(area, currentLevel, normalizedLevel);
    }

    /**
     * エリアの表示色を返す
     */
    public Color getAreaColor(double currentLevel, double normalizedLevel) {
        double density = 0.0;
        if (pollutionColorSaturation == 0.0) {
            density = Math.min(normalizedLevel, 1.0);
        } else {
            density = Math.min(currentLevel / pollutionColorSaturation, 1.0);
        }
        double opacity = Math.min(density / 1.5, 1.0);

        Color color = null;
        switch (obstructerDisplay) {
        case RED:
            color = new Color(density, 0.0, 0.0, opacity);
            break;
        case BLUE:
            color = new Color(0.0, 0.0, density, opacity);
            break;
        case HSV:
            double hue = Math.min((1.0 - density) * 0.65, 1.0);
            color = Color.hsb(hue, 1.0, 1.0);
            break;
        case ORANGE:
            color = new Color(1.0, Math.min(1.0 - density / 2.0, 1.0), 0.0, opacity);
            break;
        }
        return color;
    }

    /**
     * エリアのアウトライン表示の ON/OFF
     */
    public void setShowAreasOutline(boolean showOutline) {
        for (MapArea area : areas) {
            Box shape = areaShapes.get(area);
            Color color = null;
            double currentLevel = area.getPollutionLevel().getCurrentLevel();
            if (showOutline && currentLevel == 0.0) {
                color = outlineColor;
            } else {
                double normalizedLevel = area.getPollutionLevel().getNormalizedLevel();
                color = getAreaColor(currentLevel, normalizedLevel);
            }
            PhongMaterial material = (PhongMaterial)shape.getMaterial();
            if (! material.getDiffuseColor().equals(color)) {
                material.setDiffuseColor(color);
            }
        }
    }

    /**
     * エリアの表示/非表示を切り替える
     */
    public void setShowAreas(boolean showAreas) {
        areaGroup.setVisible(showAreas);
    }

    /**
     * エージェントをシーングラフに追加する
     */
    public void addAgent(AgentBase agent, double size, Point3D position, TriageLevel triage, double speed) {
        if (agent.isEvacuated()) {
            return;
        }

        Shape3D shape = agentShapes.get(agent);
        if (shape != null) {
            Itk.logWarn("addAgent",
			"agent already registered:", agent.getID());
            return;
        }
        shape = new Sphere(size / 2.0);

        Color color = FxColor.DEFAULT_AGENT_COLOR;
        if (agent.hasTag("BLUE")){
            color = FxColor.BLUE;
        } else if (agent.hasTag("APINK")){
            color = FxColor.APINK;
        } else if (agent.hasTag("YELLOW")){
            color = FxColor.YELLOW;
        }
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(getAgentColor(triage, speed, color));
        shape.setMaterial(material);

        shape.setTranslateX(position.getX());
        shape.setTranslateY(position.getY());
        shape.setTranslateZ(-(position.getZ() * verticalScale + size / 2.0));

        // ピッキングを有効にする
        shape.setId("Agent " + agent.getID());
        shape.setOnMouseEntered(onMouseEntered);
        shape.setOnMouseExited(onMouseExited);
        shape.setOnMouseClicked(onMouseClicked);

        agentGroup.getChildren().add(shape);
        agentShapes.put(agent, shape);
        agentMap.put(agent.getID(), agent);
        defaultColorOfAgents.put(agent, color);
    }

    /**
     * エージェントをシーングラフから削除する
     */
    public boolean removeAgent(AgentBase agent) {
        Shape3D shape = agentShapes.get(agent);
        if (shape != null) {
            abortHover(agent);
            agentShapes.remove(agent);
            agentMap.remove(agent.getID());
            defaultColorOfAgents.remove(agent);
            shape.setVisible(false);    // 不要かも
            agentGroup.getChildren().remove(shape);
            return true;
        }
        return false;
    }

    /**
     * エージェントを移動する
     */
    public void moveAgent(AgentBase agent, double size, Point3D position) {
        if (agent.isEvacuated()) {
            return;
        }

        Shape3D shape = agentShapes.get(agent);
        if (shape == null) {
            Itk.logWarn("moveAgent",
			"agent not registered yet:", agent.getID());
        } else {
            double x = shape.getTranslateX();
            double y = shape.getTranslateY();
            double z = shape.getTranslateZ();
            if (position.getX() != x) {
                shape.setTranslateX(position.getX());
            }
            if (position.getY() != y) {
                shape.setTranslateY(position.getY());
            }
            if (position.getZ() != z) {
                shape.setTranslateZ(-(position.getZ() * verticalScale + size / 2.0));
            }
        }
    }

    /**
     * エージェントの表示色を変更する
     */
    public void changeAgentColor(AgentBase agent, TriageLevel triage, double speed) {
        if (agent.isEvacuated()) {
            return;
        }

        Shape3D shape = agentShapes.get(agent);
        if (shape == null) {
	    // これは、常時起こりうるので、特に気にしないことにする。
	    /*
            Itk.logInfo("changeAgentColor",
			"agent not registered yet:", agent.getID());
	    */
        } else {
            Color color = getAgentColor(triage, speed, defaultColorOfAgents.get(agent));
            PhongMaterial material = (PhongMaterial)shape.getMaterial();
            if (! material.getDiffuseColor().equals(color)) {
                material.setDiffuseColor(color);
            }
        }
    }

    /**
     * エージェントの表示色を更新する
     */
    public void updateAgentsColor(AgentBase agent) {
        Shape3D shape = agentShapes.get(agent);
        if (shape == null) {
            Itk.logWarn("updateAgentsColor",
			"agent not registered yet:", agent.getID());
        } else {
            Color color = getAgentColor(agent);
            PhongMaterial material = (PhongMaterial)shape.getMaterial();
            if (! material.getDiffuseColor().equals(color)) {
                material.setDiffuseColor(color);
            }
        }
    }

    /**
     * 全エージェントの表示色を更新する
     */
    public void updateAgentsColor() {
        for (AgentBase agent : agentShapes.keySet()) {
            updateAgentsColor(agent);
        }
    }

    /**
     * エージェントの表示色を返す
     */
    public Color getAgentColor(AgentBase agent) {
        return getAgentColor(agent.getTriage(), agent.getSpeed(), defaultColorOfAgents.get(agent));
    }

    /**
     * エージェントの表示色を返す
     */
    public Color getAgentColor(TriageLevel triage, double speed, Color defaultColor) {
        switch (triage) {
        case GREEN:
            if (isChangeAgentColorDependingOnSpeed()) {
                return FxColor.speedToColor(speed);
            }
            break;
        case YELLOW:
            return FxColor.YELLOW;
        case RED:
            return FxColor.PRED;
        case BLACK:
            return FxColor.BLACK2;
        }
        return defaultColor == null ? FxColor.DEFAULT_AGENT_COLOR : defaultColor;
    }

    /**
     * 全エージェントのサイズを変更する
     */
    public void changeAgentSize(double size) {
        for (Shape3D shape : agentShapes.values()) {
            ((Sphere)shape).setRadius(size / 2.0);
        }
    }

    /**
     * 全エージェントの Z 位置を更新する
     */
    public void updateAgentsHeight(double size) {
        for (Map.Entry<AgentBase, Shape3D> entry : agentShapes.entrySet()) {
            AgentBase agent = entry.getKey();
            Shape3D shape = entry.getValue();
            shape.setTranslateZ(-(agent.getHeight() * verticalScale + size / 2.0));
        }
    }

    /**
     * エージェントの表示/非表示を切り替える
     */
    public void setShowAgents(boolean showAgents) {
        agentGroup.setVisible(showAgents);
    }

    /**
     * 歩行速度に応じてエージェントの色を変えるかどうかをセットする
     */
    public void setChangeAgentColorDependingOnSpeed(boolean b) {
        changeAgentColorDependingOnSpeed = b;
    }

    /**
     * 歩行速度に応じてエージェントの色を変えるか?
     */
    public boolean isChangeAgentColorDependingOnSpeed() {
        return changeAgentColorDependingOnSpeed;
    }

    /**
     * シミュレーションの進捗状況を表示するかどうかをセットする
     */
    public void setShowStatus(boolean showStatus) {
        statusTop.setVisible(showStatus && showStatusPosition.equals("top"));
        statusBottom.setVisible(showStatus && showStatusPosition.equals("bottom"));
    }

    /**
     * シミュレーションの進捗状況を表示するか?
     */
    public boolean isShowStatus() {
        return statusTop.isVisible() || statusBottom.isVisible();
    }

    /**
     * シミュレーションの進捗状況の表示位置を設定する
     */
    public void setShowStatusPosition(String showStatusPosition) {
        this.showStatusPosition = showStatusPosition;
        if (isShowStatus()) {
            statusTop.setVisible(showStatusPosition.equals("top"));
            statusBottom.setVisible(showStatusPosition.equals("bottom"));
        }
    }

    /**
     * シミュレーションの進捗状況表示用のテキストをセットする
     */
    public void setStatusText(String text) {
        statusTop.setText(text);
        statusBottom.setText(text);
    }

    /**
     * 背景地図の表示/非表示を切り替える
     */
    public void setShowBackgroundMap(boolean showBackgroundMap) {
        backgroundMapGroup.setVisible(showBackgroundMap);
    }

    /**
     * AIST ロゴを表示するかどうかをセットする
     */
    public void setShowLogo(boolean showLogo) {
        logoLabel.setVisible(showLogo);
    }

    /**
     * AIST ロゴを表示するか?
     */
    public boolean isShowLogo() {
        return logoLabel.isVisible();
    }

    /**
     * マップをセンタリングする際にマージンを付加するかどうかをセットする
     */
    public void setMarginAdded(boolean marginAdded) {
        this.marginAdded = marginAdded;
    }

    /**
     * マップをセンタリングする際にマージンを付加するか?
     */
    public boolean isMarginAdded() {
        return marginAdded;
    }

    /**
     * 視点を変える操作を許可するかどうかをセットする
     */
    public void setViewPointOperationEnabled(boolean viewPointOperationEnabled) {
        this.viewPointOperationEnabled = viewPointOperationEnabled;
    }

    /**
     * 視点を変える操作が許可されているか?
     */
    public boolean isViewPointOperationEnabled() {
        return viewPointOperationEnabled;
    }

    /**
     * 表示するリンクを実際の道幅表示に切り替える
     */
    public void setAtActualWidth(boolean atActualWidth) {
        this.atActualWidth = atActualWidth;
        thinLineGroup.setVisible(! atActualWidth);
        edgeLineGroup.setVisible(atActualWidth);
    }
}
