package nodagumi.ananPJ.Editor;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import net.arnx.jsonic.JSON;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;

import nodagumi.ananPJ.CrowdWalkLauncher;
import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Gui.FxColor;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.Gui.MapViewFrame;
import nodagumi.ananPJ.GuiSimulationLauncher;
import nodagumi.ananPJ.Editor.MapEditor.TextPosition;
import nodagumi.ananPJ.Editor.Panel.AreaPanelFx;
import nodagumi.ananPJ.Editor.Panel.GroupPanel;
import nodagumi.ananPJ.Editor.Panel.LinkPanelFx;
import nodagumi.ananPJ.Editor.Panel.NodePanelFx;
import nodagumi.ananPJ.Editor.Panel.PolygonPanel;
import nodagumi.ananPJ.Editor.Panel.ScenarioPanelFx;
import nodagumi.ananPJ.Editor.Panel.TagSetupPane;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.NetworkMap.Polygon.TriangleMeshes;
import nodagumi.ananPJ.NetworkMap.Polygon.Coordinates;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.Settings;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.Itk.Itk;
import nodagumi.Itk.Term;

/**
 * マップエディタのウィンドウ構築と GUI コントロール
 */
public class EditorFrameFx {
    /**
     * ヘルプ表示用コンテンツのアドレス
     */
    public static final String HTML_TEMPLATE = "/doc/template.html";
    public static final String QUICK_REFERENCE = "/doc/quick_reference_map_editor.md";
    public static final String PROPERTIES_PATH = "./doc/javadoc/nodagumi/ananPJ/misc/CrowdWalkPropertiesHandler.html";
    public static final String TUTORIAL_URI = "https://github.com/crest-cassia/CrowdWalk/wiki/CrowdWalk-%E3%83%81%E3%83%A5%E3%83%BC%E3%83%88%E3%83%AA%E3%82%A2%E3%83%AB";
    public static final String ZONE_REFERENCE_URI = "http://www.gsi.go.jp/sokuchikijun/jpc.html";
    public static final String GITHUB_REPOSITORY_URI = "https://github.com/crest-cassia/CrowdWalk";

    /**
     * 編集モード
     */
    public static enum EditorMode {
        ADD_NODE, ADD_LINK, ADD_NODE_LINK, ADD_AREA, EDIT_NODE, EDIT_LINK, EDIT_AREA, EDIT_POLYGON, BACKGROUND_IMAGE
    };
    private EditorMode mode = EditorMode.EDIT_NODE;

    /**
     * マップエディタ
     */
    private MapEditor editor;

    /**
     * GUI の設定情報
     */
    private Settings settings;

    /**
     * ウィンドウフレーム
     */
    private Stage frame;

    /**
     * マップエディタのキャンバス
     */
    private EditorCanvas canvas;

    /**
     * 編集エリアとタブエリアの仕切り
     */
    private SplitPane splitPane;

    /**
     * File メニュー
     */
    private MenuItem miNew;
    private MenuItem miOpenMap;
    private MenuItem miSaveMap;
    private MenuItem miSaveMapAs;
    private MenuItem miOpenProperty;
    private MenuItem miQuit;

    /**
     * マップの回転角度リセットメニュー
     */
    private MenuItem miResetRotation = new MenuItem("Reset rotation");

    /**
     * 背景画像の色の濃さ設定メニュー
     */
    private Menu menuColorDepthOfBackgroundImage = new Menu("Set color depth of background image");

    /**
     * 背景地図の色の濃さ設定メニュー
     */
    private Menu menuColorDepthOfBackgroundMap = new Menu("Set color depth of background map");

    /**
     * 背景グループ表示メニュー
     */
    private Menu menuShowBackgroundGroup = new Menu("Show background group");

    /**
     * グループ選択パネル
     */
    private FlowPane groupSelectionPane = new FlowPane();
    private ToggleGroup groupToggleGroup = new ToggleGroup();
    private HashMap<MapPartGroup, ToggleButton> groupButtonMap = new HashMap();
    private ArrayList<ToggleButton> groupButtons = new ArrayList();

    /**
     * ステータスバー
     */
    private HBox statusPane = new HBox();
    private HBox linkAttributesPane;
    private TextField linkLengthField = new TextField("0.0");
    private Label linkScaleLabel = new Label("(scale: 1.0)");
    private TextField linkWidthField = new TextField("1.0");
    private Label statusLabel = new Label("Unedited");
    private Button simulate2dButton = new Button("2D Simulate");
    private Button simulate3dButton = new Button("3D Simulate");

    /**
     * コンテキスト・メニュー
     */
    private ContextMenu editNodeMenu = new ContextMenu();
    private ContextMenu editLinkMenu = new ContextMenu();
    private ContextMenu editAreaMenu = new ContextMenu();
    private ContextMenu editPolygonMenu = new ContextMenu();
    private ContextMenu addTriangleMesheMenu = new ContextMenu();
    private ContextMenu bgImageMenu = new ContextMenu();
    private Menu menuAddSymbolicLinkOfNode = new Menu("Add symbolic link");
    private Menu menuAddSymbolicLinkOfLink = new Menu("Add symbolic link");
    private MenuItem miSetBackgroundImage;
    private MenuItem miSetBackgroundImageAttributes;
    private MenuItem miRemoveBackgroundImage;

    /**
     * タブパネル
     */
    private TabPane tabPane = new TabPane();
    private GroupPanel groupPanel;
    private NodePanelFx nodePanel;
    private LinkPanelFx linkPanel;
    private AreaPanelFx areaPanel;
    private PolygonPanel polygonPanel;
    private ScenarioPanelFx scenarioPanel;

    /**
     * シェープファイル読み込みダイアログのプリセット情報
     */
    private Map<String, Object> shapefileSpecs;

    /**
     * ヘルプ表示用
     */
    private Parser parser;
    private HtmlRenderer renderer;
    private Stage helpStage = new Stage();
    private WebView webView = new WebView();
    private double helpZoom = 1.0;

    /**
     * 編集モード選択ボタンのリスト
     */
    private ArrayList<ToggleButton> editModeButtons = new ArrayList();

    /**
     * JavaFX アプリケーション・スレッド
     */
    private Thread fxThread = null;

    /**
     * コンストラクタ
     */
    public EditorFrameFx() {}

    /**
     * コンストラクタ
     */
    public EditorFrameFx(MapEditor editor, String title, Settings settings) {
        frame = new Stage();
        frame.setTitle(title);
        this.editor = editor;
        this.settings = settings;

        int x = settings.get("_editorPositionX", 0);
        int y = settings.get("_editorPositionY", 0);
        int width = settings.get("_editorWidth", 1400);
        int height = settings.get("_editorHeight", 768);
        frame.setX(x);
        frame.setY(y);
        frame.setWidth(width);
        frame.setHeight(height);

        init();

        // メニューの構築

        Node menuBar = createMainMenu();
        updateShowBackgroundGroupMenu();
        createContextMenus();
        updateAddSymbolicLinkMenu();

        // editor canvas の構築

        Group root = new Group();
        canvas = new EditorCanvas(editor, this);
        root.getChildren().add(canvas);
        updateRotationMenu();                       // canvas のメソッドを呼び出すためここで実行する
        updateColorDepthOfBackgroundImageMenu();    //                  〃
        updateColorDepthOfBackgroundMapMenu();      //                  〃
        StackPane canvasPane = new StackPane();
        String image = getClass().getResource("/img/canvas_bg.png").toExternalForm();
        canvasPane.setStyle(
            "-fx-background-image: url('" + image + "'); " +
            "-fx-background-position: center center; " +
            "-fx-background-repeat: repeat;"
        );
        canvasPane.getChildren().add(root);
        canvasPane.setMinWidth(800);
        canvasPane.setMinHeight(600);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        // 左側ペインの構築

        BorderPane leftPane = new BorderPane();
        groupSelectionPane.getStyleClass().add("custom-color-pane");
        groupSelectionPane.setPadding(new Insets(4));
        groupSelectionPane.setHgap(8);
        groupSelectionPane.setAlignment(Pos.CENTER);
        leftPane.setCenter(canvasPane);     // これを先にセットしないと rotation した時に他の Pane に被ってしまう
        leftPane.setTop(updateGroupSelectionPane());
        leftPane.setBottom(createModeSelectionPane());

        // タブパネルの構築

        groupPanel = new GroupPanel(editor);
        Tab groupsTab = new Tab("Groups");
        groupsTab.setContent(groupPanel);
        groupsTab.setClosable(false);

        nodePanel = new NodePanelFx(editor, this);
        Tab nodesTab = new Tab("Nodes");
        nodesTab.setContent(nodePanel);
        nodesTab.setClosable(false);

        linkPanel = new LinkPanelFx(editor, this);
        Tab linksTab = new Tab("Links");
        linksTab.setContent(linkPanel);
        linksTab.setClosable(false);

        areaPanel = new AreaPanelFx(editor, this);
        Tab areasTab = new Tab("Areas");
        areasTab.setContent(areaPanel);
        areasTab.setClosable(false);

        polygonPanel = new PolygonPanel(editor, this);
        Tab polygonsTab = new Tab("Polygons");
        polygonsTab.setContent(polygonPanel);
        polygonsTab.setClosable(false);

        scenarioPanel = new ScenarioPanelFx(editor, this);
        Tab scenarioTab = new Tab("Scenario");
        scenarioTab.setContent(scenarioPanel);
        scenarioTab.setClosable(false);

        tabPane.getTabs().addAll(groupsTab, nodesTab, linksTab, areasTab, polygonsTab, scenarioTab);

        // 下側ペインの構築

        BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);

        statusPane.setAlignment(Pos.CENTER_LEFT);
        statusPane.setSpacing(8);
        linkAttributesPane = createLinkAttributesPane();
        statusPane.getChildren().addAll(statusLabel);

        simulate2dButton.setOnAction(e -> simulate("GuiSimulationLauncher2D"));
        simulate3dButton.setOnAction(e -> simulate("GuiSimulationLauncher3D"));

        FlowPane simulationButtonPane = new FlowPane();
        simulationButtonPane.setHgap(8);
        simulationButtonPane.setMaxWidth(230);
        simulationButtonPane.setAlignment(Pos.CENTER);
        simulationButtonPane.getChildren().addAll(simulate2dButton, simulate3dButton);

        BorderPane bottomPane = new BorderPane();
        bottomPane.setPadding(new Insets(4));
        bottomPane.setCenter(statusPane);
        bottomPane.setRight(simulationButtonPane);

        // スプリットペインの構築
        splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, tabPane);
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            double tabWidth = splitPane.getWidth() * (1.0 - newVal.doubleValue());
            areaPanel.widthChanged(tabWidth);
            scenarioPanel.widthChanged(tabWidth);
        });
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String pos = settings.get("dividerPosition", "0.625");
                if (! pos.isEmpty()) {
                    splitPane.setDividerPositions(Double.valueOf(pos));
                }
            }
        });

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(menuBar);
        borderPane.setCenter(splitPane);
        borderPane.setBottom(bottomPane);

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().add("stylesheet.css");
        frame.setScene(scene);

        // ウィンドウイベントのハンドリング
        frame.setOnShown(e -> {
            try {
                boolean simulationWindowOpen = editor.getProperties().getBoolean("simulation_window_open", false);
                boolean autoSimulationStart = editor.getProperties().getBoolean("auto_simulation_start", false);

                String filePath = editor.getProperties().getFurnishedPath("camera_2d_file", null);
                if (filePath != null) {
                    if (! checkCameraworkVersion(filePath)) {
                        exit();
                    }
                }
                if (! autoSimulationStart) {
                    totalValidation(false, null);
                }

                if (simulationWindowOpen || autoSimulationStart) {
                    if (editor.getNetworkMapFile() == null || editor.getGenerationFile() == null || editor.getScenarioFile() == null) {
                        // プロパティファイルの設定が足りないためシミュレーションを開始することが出来ません
                        Alert alert = new Alert(AlertType.WARNING, "The simulation can not be started because the properties file setting is insufficient.", ButtonType.OK);
                        alert.initOwner(frame);
                        alert.showAndWait();
                        return;
                    }
                    if (CrowdWalkLauncher.use2dSimulator) {
                        simulate2dButton.fire();
                    } else {
                        simulate3dButton.fire();
                    }
                }
            } catch(Exception ex) {
		Itk.logError(ex.getMessage()) ;
		Itk.quitByError() ;
            }
        });
        frame.setOnCloseRequest(e -> closing(e));
        frame.setOnHidden(e -> {
            if (! Platform.isImplicitExit()) {
                Platform.runLater(() -> Platform.exit());
            }
        });
    }

    /**
     * 初期設定
     */
    private void init() {
        // シェープファイル読み込みダイアログのプリセット情報を読み込む
        JSON json = new JSON(JSON.Mode.TRADITIONAL);
        try {
            shapefileSpecs = json.parse(getClass().getResourceAsStream("/shapefile_specs.json"));
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e);
            shapefileSpecs = new HashMap();
        }

        // ヘルプ画面の準備

        MutableDataSet options = new MutableDataSet();
        options.setFrom(ParserEmulationProfile.MARKDOWN);
        options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create()
        ));
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        webView.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.W) {
                    helpStage.close();
                } else if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.UP) {
                    helpZoom += 0.1;
                    webView.setZoom(helpZoom);
                } else if (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.DOWN) {
                    helpZoom -= 0.1;
                    webView.setZoom(helpZoom);
                } else if (event.getCode() == KeyCode.DIGIT0) {
                    helpZoom = 1.0;
                    webView.setZoom(helpZoom);
                }
            }
        });

        Button okButton = new Button("  OK  ");
        okButton.setOnAction(event -> helpStage.close());
        BorderPane buttonPane = new BorderPane();
        buttonPane.setPadding(new Insets(4, 8, 4, 8));
        buttonPane.setRight(okButton);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(webView);
        borderPane.setBottom(buttonPane);

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().add("stylesheet.css");
        helpStage.setScene(scene);
    }

    /**
     * 終了処理
     */
    public boolean closing(WindowEvent event) {
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data has been modified.\n    Do you want to quit anyway?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() == ButtonType.NO) {
                if (event != null) {
                    event.consume();
                }
                return false;
            }
        }
        settings.put("_editorPositionX", (int)frame.getX());
        settings.put("_editorPositionY", (int)frame.getY());
        settings.put("_editorWidth", (int)frame.getWidth());
        settings.put("_editorHeight", (int)frame.getHeight());
        settings.put("dividerPosition", "" + splitPane.getDividerPositions()[0]);
        return true;
    }

    /**
     * アプリケーションを終了する
     */
    public void exit() {
        Platform.runLater(() -> {
            fxThread = Thread.currentThread();
            Platform.exit();
            new Thread(() -> {
                if (fxThread.isAlive()) {
                    try {
                        fxThread.join();
                    } catch (InterruptedException ex) {
			Itk.dumpStackTraceOf(ex);
                    }
                }
		Itk.quitSafely() ;
            }).start();
        });
    }

    /**
     * メインメニューの生成
     */
    private Node createMainMenu() {
        MenuBar menuBar = new MenuBar();

        /* File menu */

        Menu fileMenu = new Menu("File");

        miNew = new MenuItem("New");
        miNew.setOnAction(e -> clearMapData());

        miOpenMap = new MenuItem("Open map");
        miOpenMap.setOnAction(e -> openMap());
        miOpenMap.setAccelerator(KeyCombination.valueOf("Ctrl+O"));

        miSaveMap = new MenuItem("Save map");
        miSaveMap.setOnAction(e -> saveMap());
        miSaveMap.setAccelerator(KeyCombination.valueOf("Ctrl+S"));

        miSaveMapAs = new MenuItem("Save map as");
        miSaveMapAs.setOnAction(e -> saveMapAs());

        miOpenProperty = new MenuItem("Open properties");
        miOpenProperty.setOnAction(e -> openProperties());

        // MenuItem miSaveProperty = new MenuItem("Save properties");
        // miSaveProperty.setOnAction(e -> {
        //     Itk.logInfo("Save properties: under construction");
        // });

        miQuit = new MenuItem("Quit");
        miQuit.setOnAction(e -> {
            if (closing(null)) {
                frame.close();
            }
        });
        miQuit.setAccelerator(KeyCombination.valueOf("Ctrl+Q"));

        fileMenu.getItems().addAll(miNew, miOpenMap, miSaveMap, miSaveMapAs, miOpenProperty, /* miSaveProperty, */ miQuit);

        /* Edit menu */

        Menu editMenu = new Menu("Edit");

        MenuItem miUndo = new MenuItem("Undo");
        miUndo.setOnAction(e -> editor.undo());
        miUndo.setAccelerator(KeyCombination.valueOf("Ctrl+Z"));

        MenuItem miRedo = new MenuItem("Redo");
        miRedo.setOnAction(e -> editor.redo());
        miRedo.setAccelerator(KeyCombination.valueOf("Ctrl+Y"));

        MenuItem miReadShapefile = new MenuItem("Read shapefile");
        miReadShapefile.setOnAction(e -> readShapefile());

        MenuItem miReadMrdFile = new MenuItem("Read MRD file");
        miReadMrdFile.setOnAction(e -> readMrdFile());

        MenuItem miReadOsm = new MenuItem("Read OpenStreetMap");
        miReadOsm.setOnAction(e -> readOpenStreetMap());

        editMenu.getItems().addAll(miUndo, miRedo, miReadShapefile, miReadMrdFile, miReadOsm);

        /* View menu */

        Menu viewMenu = new Menu("View");

        MenuItem miShow3d = new MenuItem("Show 3D");
        miShow3d.setOnAction(e -> {
            MapViewFrame mapViewer = new MapViewFrame("3D preview of Structure", 800, 600, editor.getMap(), editor.getProperties());
            mapViewer.show();
        });

        MenuItem miCentering = new MenuItem("Centering");
        miCentering.setOnAction(e -> {
            canvas.centering(false);
            canvas.repaintLater();
        });
        miCentering.setAccelerator(KeyCombination.valueOf("Ctrl+C"));

        MenuItem miCenteringWithScaling = new MenuItem("Centering with scaling");
        miCenteringWithScaling.setOnAction(e -> {
            canvas.centering(true);
            canvas.repaintLater();
        });
        miCenteringWithScaling.setAccelerator(KeyCombination.valueOf("Ctrl+Shift+C"));

        MenuItem miToTheOrigin = new MenuItem("To the origin");
        miToTheOrigin.setOnAction(e -> {
            canvas.setTranslate(0.0, 0.0);
            canvas.repaintLater();
        });

        MenuItem miSetRotation = new MenuItem("Set rotation");
        miSetRotation.setOnAction(e -> setRotation());

        miResetRotation.setOnAction(e -> {
            canvas.rotate(canvas.getWidth() / 2.0, canvas.getHeight() / 2.0, 0.0);
        });

        CheckMenuItem cmiGridShowing = new CheckMenuItem("Show grid");
        cmiGridShowing.setSelected(false);
        cmiGridShowing.setOnAction(e -> {
            canvas.setGridShowing(cmiGridShowing.isSelected());
            canvas.repaintLater();
        });
        cmiGridShowing.setAccelerator(KeyCombination.valueOf("Ctrl+G"));

        MenuItem miSetGrid = new MenuItem("Set grid");
        miSetGrid.setOnAction(e -> setGrid());
        miSetGrid.setAccelerator(KeyCombination.valueOf("Ctrl+Shift+G"));

        CheckMenuItem cmiShowNodes = new CheckMenuItem("Show nodes");
        cmiShowNodes.setSelected(true);
        cmiShowNodes.setOnAction(e -> {
            canvas.setNodesShowing(cmiShowNodes.isSelected());
            canvas.repaintLater();
        });
        cmiShowNodes.setAccelerator(KeyCombination.valueOf("Ctrl+N"));

        CheckMenuItem cmiShowNodeLabels = new CheckMenuItem("Show node labels");
        cmiShowNodeLabels.setSelected(false);
        cmiShowNodeLabels.setOnAction(e -> {
            canvas.setNodeLabelsShowing(cmiShowNodeLabels.isSelected());
            canvas.repaintLater();
        });
        cmiShowNodeLabels.setAccelerator(KeyCombination.valueOf("Ctrl+Shift+N"));

        CheckMenuItem cmiShowLinks = new CheckMenuItem("Show links");
        CheckMenuItem cmiShowLinkLabels = new CheckMenuItem("Show link labels");
        cmiShowLinks.setSelected(true);
        cmiShowLinks.setOnAction(e -> {
            if (cmiShowLinks.isSelected()) {
                cmiShowLinkLabels.setDisable(false);
                canvas.setLinksShowing(true);
            } else {
                cmiShowLinkLabels.setDisable(true);
                canvas.setLinksShowing(false);
            }
            canvas.repaintLater();
        });
        cmiShowLinks.setAccelerator(KeyCombination.valueOf("Ctrl+L"));

        cmiShowLinkLabels.setSelected(false);
        cmiShowLinkLabels.setOnAction(e -> {
            canvas.setLinkLabelsShowing(cmiShowLinkLabels.isSelected());
            canvas.repaintLater();
        });
        cmiShowLinkLabels.setAccelerator(KeyCombination.valueOf("Ctrl+Shift+L"));

        CheckMenuItem cmiShowAreas = new CheckMenuItem("Show areas");
        CheckMenuItem cmiShowAreaLabels = new CheckMenuItem("Show area labels");
        cmiShowAreas.setSelected(true);
        cmiShowAreas.setOnAction(e -> {
            if (cmiShowAreas.isSelected()) {
                cmiShowAreaLabels.setDisable(false);
                canvas.setAreasShowing(true);
            } else {
                cmiShowAreaLabels.setDisable(true);
                canvas.setAreasShowing(false);
            }
            canvas.repaintLater();
        });
        cmiShowAreas.setAccelerator(KeyCombination.valueOf("Ctrl+R"));

        cmiShowAreaLabels.setSelected(false);
        cmiShowAreaLabels.setOnAction(e -> {
            canvas.setAreaLabelsShowing(cmiShowAreaLabels.isSelected());
            canvas.repaintLater();
        });
        cmiShowAreaLabels.setAccelerator(KeyCombination.valueOf("Ctrl+Shift+R"));

        CheckMenuItem cmiShowPolygons = new CheckMenuItem("Show polygons");
        CheckMenuItem cmiShowPolygonLabels = new CheckMenuItem("Show polygon labels");
        cmiShowPolygons.setSelected(true);
        cmiShowPolygons.setOnAction(e -> {
            if (cmiShowPolygons.isSelected()) {
                cmiShowPolygonLabels.setDisable(false);
                canvas.setPolygonsShowing(true);
            } else {
                cmiShowPolygonLabels.setDisable(true);
                canvas.setPolygonsShowing(false);
            }
            canvas.repaintLater();
        });
        cmiShowPolygons.setAccelerator(KeyCombination.valueOf("Ctrl+P"));

        cmiShowPolygonLabels.setSelected(false);
        cmiShowPolygonLabels.setOnAction(e -> {
            canvas.setPolygonLabelsShowing(cmiShowPolygonLabels.isSelected());
            canvas.repaintLater();
        });
        cmiShowPolygonLabels.setAccelerator(KeyCombination.valueOf("Ctrl+Shift+P"));

        CheckMenuItem cmiShowBackgroundImage = new CheckMenuItem("Show background image");
        cmiShowBackgroundImage.setSelected(true);
        cmiShowBackgroundImage.setOnAction(e -> {
            canvas.setBackgroundImageShowing(cmiShowBackgroundImage.isSelected());
            if (cmiShowBackgroundImage.isSelected()) {
                menuColorDepthOfBackgroundImage.setDisable(false);
                canvas.setBackgroundImageShowing(true);
            } else {
                menuColorDepthOfBackgroundImage.setDisable(true);
                canvas.setBackgroundImageShowing(false);
            }
            canvas.repaintLater();
        });
        cmiShowBackgroundImage.setAccelerator(KeyCombination.valueOf("Ctrl+B"));

        CheckMenuItem cmiShowBackgroundMap = new CheckMenuItem("Show background map");
        cmiShowBackgroundMap.setOnAction(e -> {
            if (cmiShowBackgroundMap.isSelected()) {
                menuColorDepthOfBackgroundMap.setDisable(false);
                canvas.setBackgroundMapShowing(true);
            } else {
                menuColorDepthOfBackgroundMap.setDisable(true);
                canvas.setBackgroundMapShowing(false);
            }
            canvas.repaintLater();
        });
        cmiShowBackgroundMap.setAccelerator(KeyCombination.valueOf("Ctrl+M"));

        CheckMenuItem cmiShowMapCoordinates = new CheckMenuItem("Show map coordinates on the cursor");
        cmiShowMapCoordinates.setOnAction(e -> {
            canvas.setMapCoordinatesShowing(cmiShowMapCoordinates.isSelected());
        });

        viewMenu.getItems().addAll(miShow3d, new SeparatorMenuItem(), miCentering, miCenteringWithScaling, miToTheOrigin, miSetRotation, miResetRotation, cmiGridShowing, miSetGrid, new SeparatorMenuItem(), cmiShowNodes, cmiShowNodeLabels, cmiShowLinks, cmiShowLinkLabels, cmiShowAreas, cmiShowAreaLabels, cmiShowPolygons, cmiShowPolygonLabels, new SeparatorMenuItem(), cmiShowBackgroundImage, menuColorDepthOfBackgroundImage, cmiShowBackgroundMap, menuColorDepthOfBackgroundMap, menuShowBackgroundGroup, cmiShowMapCoordinates);

        /* Validation menu */

        Menu actionMenu = new Menu("Validation");

        MenuItem miTotalValidation = new MenuItem("Total validation");
        miTotalValidation.setOnAction(e -> totalValidation(true, null));

        MenuItem miCheckForPiledNodes = new MenuItem("Check for node in same position");
        miCheckForPiledNodes.setOnAction(e -> checkForPiledNodes());

        MenuItem miCheckLinksWhereNodeDoesNotExist = new MenuItem("Check links where node does not exist");
        miCheckLinksWhereNodeDoesNotExist.setOnAction(e -> checkLinksWhereNodeDoesNotExist());

        MenuItem miCheckLoopedLinks = new MenuItem("Check for looped link");
        miCheckLoopedLinks.setOnAction(e -> checkLoopedLinks());

        MenuItem miCheck0LengthLinks = new MenuItem("Check for zero length link");
        miCheck0LengthLinks.setOnAction(e -> check0LengthLinks());

        MenuItem miCheckDuplicateLinks = new MenuItem("Check for duplicate link");
        miCheckDuplicateLinks.setOnAction(e -> checkDuplicateLinks());

        MenuItem miCalculateTagPaths = new MenuItem("Calculate tag paths");
        miCalculateTagPaths.setOnAction(e -> openCalculateTagPathsDialog());

        MenuItem miCheckReachability = new MenuItem("Check reachability");
        miCheckReachability.setOnAction(e -> checkForReachability());
        // TODO: 正常に機能していないので無効にした
        miCheckReachability.setDisable(true);

        actionMenu.getItems().addAll(miTotalValidation, miCheckForPiledNodes, miCheckLinksWhereNodeDoesNotExist, miCheckLoopedLinks, miCheck0LengthLinks, miCheckDuplicateLinks, miCalculateTagPaths, miCheckReachability);

        /* Help menu */

        Menu helpMenu = new Menu("Help");

        MenuItem miKeyboardShortcuts = new MenuItem("Quick reference");
        miKeyboardShortcuts.setOnAction(e -> {
            helpStage.setTitle("Help - Quick reference");
            helpStage.setWidth(980);
            helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
            String template = ObstructerBase.resourceToString(HTML_TEMPLATE);
            com.vladsch.flexmark.ast.Node document = parser.parse(ObstructerBase.resourceToString(QUICK_REFERENCE));
            String html = template.replace("__TITLE__", "クイック・リファレンス").replace("__HTML_BODY__", renderer.render(document));
            webView.getEngine().loadContent(html);
            helpStage.show();
            helpStage.toFront();
        });

        MenuItem miPropertiesSetting = new MenuItem("Properties setting");
        miPropertiesSetting.setOnAction(e -> {
            File file = new File(PROPERTIES_PATH);
            if (file.exists()) {
                helpStage.setTitle("Help - Properties setting");
                helpStage.setWidth(900);
                helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
                try {
                    webView.getEngine().load("file:///" + file.getCanonicalPath().replace('\\', '/'));
                    helpStage.show();
                    helpStage.toFront();
                } catch (Exception ex) {
		    Itk.dumpStackTraceOf(ex);
                }
            } else {
                Alert alert = new Alert(AlertType.WARNING, "You need to execute \"sh make_javadoc.sh\" on the command line to generate javadoc files.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
            }
        });

        MenuItem miTutorialManual = new MenuItem("Tutorial manual (Open web browser)");
        miTutorialManual.setOnAction(e -> {
            new Thread(() -> {
                try {
                    URI uri = new URI(TUTORIAL_URI);
                    Desktop.getDesktop().browse(uri);
                } catch (IOException | URISyntaxException ex) {
                    Itk.dumpStackTraceOf(ex);
                }
            }).start();
        });

        MenuItem miZoneReference = new MenuItem("Zone of plane rectangular coordinate system");
        miZoneReference.setOnAction(e -> {
            helpStage.setTitle("Help - Zone of plane rectangular coordinate system");
            helpStage.setWidth(900);
            helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
            webView.getEngine().load(ZONE_REFERENCE_URI);
            helpStage.show();
            helpStage.toFront();
        });

        MenuItem miGitHub = new MenuItem("Browse GitHub repository (Open web browser)");
        miGitHub.setOnAction(e -> {
            new Thread(() -> {
                try {
                    URI uri = new URI(GITHUB_REPOSITORY_URI);
                    Desktop.getDesktop().browse(uri);
                } catch (IOException | URISyntaxException ex) {
                    Itk.dumpStackTraceOf(ex);
                }
            }).start();
        });

        if (CrowdWalkLauncher.offline) {
            miTutorialManual.setDisable(true);
            miZoneReference.setDisable(true);
            miGitHub.setDisable(true);
        }

        MenuItem miVersion = new MenuItem("About version");
        miVersion.setOnAction(e -> {
            Alert alert = new Alert(AlertType.NONE, CrowdWalkLauncher.getVersion(), ButtonType.OK);
            alert.setTitle("About version");
            alert.setHeaderText("CrowdWalk Version");
            alert.setResizable(true);
            alert.getDialogPane().setPrefSize(600, 240);
            alert.initOwner(frame);
            alert.showAndWait();
        });

        helpMenu.getItems().addAll(miKeyboardShortcuts, miPropertiesSetting, miTutorialManual, miZoneReference, miGitHub, miVersion);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, actionMenu, helpMenu);

        return menuBar;
    }

    /**
     * シミュレーションボタンの有効/無効設定
     */
    public void setDisableSimulationButtons(boolean value) {
        simulate2dButton.setDisable(value);
        simulate3dButton.setDisable(value);
    }

    /**
     * マップの回転角と回転角度リセットメニューの状態を更新する
     */
    public void updateRotationMenu() {
        try {
            double angle = editor.getProperties().getDouble("rotation_angle", 0.0);
            if (angle < -180.0 || angle > 180) {
                Itk.logWarn("Parameter out of range", "rotation_angle", "" + angle);
                angle %= 360.0;
                if (angle < -180.0) {
                    angle = (angle % 180.0) + 180.0;
                } else if (angle > 180.0) {
                    angle = (angle % 180.0) - 180.0;
                }
            }
            canvas.rotate(canvas.getWidth() / 2.0, canvas.getHeight() / 2.0, angle);

            boolean angleLocking = editor.getProperties().getBoolean("rotation_angle_locking", false);
            miResetRotation.setDisable(angleLocking);
            canvas.setAngleLocking(angleLocking);
        } catch (Exception e) {
	    Itk.quitWithStackTrace(e);
        }
    }

    /**
     * 背景画像の色の濃さ設定サブメニューを更新する
     */
    public void updateColorDepthOfBackgroundImageMenu() {
        menuColorDepthOfBackgroundImage.getItems().clear();
        try {
            double colorDepthOfBackgroundImage = editor.getProperties().getDouble("color_depth_of_background_image", 1.0);
            ToggleGroup group = new ToggleGroup();
            for (int index = 1; index <= 10; index++) {
                double colorDepth = index / 10.0;
                RadioMenuItem menuItem = new RadioMenuItem("" + (index * 10) + "%");
                menuItem.setToggleGroup(group);
                menuItem.setOnAction(e -> {
                    canvas.setColorDepthOfBackgroundImage(colorDepth);
                    canvas.repaintLater();
                });
                menuColorDepthOfBackgroundImage.getItems().add(menuItem);
                if (colorDepth == colorDepthOfBackgroundImage) {
                    menuItem.setSelected(true);
                    if (canvas != null) {
                        canvas.setColorDepthOfBackgroundImage(colorDepth);
                    }
                }
            }
        } catch (Exception e) {
	    Itk.quitWithStackTrace(e);
        }
    }

    /**
     * 背景地図の色の濃さ設定サブメニューを更新する
     */
    public void updateColorDepthOfBackgroundMapMenu() {
        menuColorDepthOfBackgroundMap.getItems().clear();
        try {
            double colorDepthOfBackgroundMap = editor.getProperties().getDouble("color_depth_of_background_map", 1.0);
            ToggleGroup group = new ToggleGroup();
            for (int index = 1; index <= 10; index++) {
                double colorDepth = index / 10.0;
                RadioMenuItem menuItem = new RadioMenuItem("" + (index * 10) + "%");
                menuItem.setToggleGroup(group);
                menuItem.setOnAction(e -> {
                    canvas.setColorDepthOfBackgroundMap(colorDepth);
                    canvas.repaintLater();
                });
                menuColorDepthOfBackgroundMap.getItems().add(menuItem);
                if (colorDepth == colorDepthOfBackgroundMap) {
                    menuItem.setSelected(true);
                    if (canvas != null) {
                        canvas.setColorDepthOfBackgroundMap(colorDepth);
                    }
                }
            }
        } catch (Exception e) {
	    Itk.quitWithStackTrace(e);
        }
    }

    /**
     * 背景グループ表示サブメニューを更新する
     */
    public void updateShowBackgroundGroupMenu() {
        menuShowBackgroundGroup.getItems().clear();
        ToggleGroup backgroundGroup = new ToggleGroup();
        if (canvas != null) {
            canvas.setBackgroundGroup(null);
        }

        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == editor.getMap().getRoot() || group.getTags().size() == 0) {
                continue;
            }
            RadioMenuItem menuItem = new RadioMenuItem(group.getTagString());
            menuItem.setToggleGroup(backgroundGroup);
            menuItem.setOnAction(e -> {
                canvas.setBackgroundGroup(group);
                canvas.repaintLater();
            });
            menuShowBackgroundGroup.getItems().add(menuItem);
        }
        RadioMenuItem menuItem = new RadioMenuItem("Do not show");
        menuItem.setToggleGroup(backgroundGroup);
        menuItem.setSelected(true);
        menuItem.setOnAction(e -> {
            canvas.setBackgroundGroup(null);
            canvas.repaintLater();
        });
        menuShowBackgroundGroup.getItems().addAll(new SeparatorMenuItem(), menuItem);
    }

    /**
     * コンテキスト・メニューを生成する
     */
    private void createContextMenus() {
        // EDIT_NODE モード
        // ・Set node attributes
        // ・Align nodes horizontally
        // ・Align nodes vertically
        // ・Move
        // ・Duplicate and move
        // ・Make stairs
        // ・Rotate and scale
        // ・Add symbolic link
        // ・Clear symbolic link
        // ・Remove nodes

        MenuItem miSetNodeAttributes = new MenuItem("Set node attributes");
        miSetNodeAttributes.setOnAction(e -> openNodeAttributesDialog());

        MenuItem miHorizontally = new MenuItem("Align nodes horizontally");
        miHorizontally.setOnAction(e -> editor.alignNodesHorizontally());

        MenuItem miVertically = new MenuItem("Align nodes vertically");
        miVertically.setOnAction(e -> editor.alignNodesVertically());

        MenuItem miCopyOrMove = new MenuItem("Copy or move");
        miCopyOrMove.setOnAction(e -> copyOrMoveNodes(0.0, 0.0, 0.0));

        MenuItem miMakeStairs = new MenuItem("Make stairs");
        miMakeStairs.setOnAction(e -> openMakeStairsDialog());

        MenuItem miRotateAndScale = new MenuItem("Rotate and scale");
        miRotateAndScale.setOnAction(e -> openRotateAndScaleNodesDialog());

        MenuItem miNormalizeCoordinates = new MenuItem("Normalize coordinates");
        miNormalizeCoordinates.setOnAction(e -> openNormalizeCoordinatesDialog());

        MenuItem miClearSymbolicLinkOfNode = new MenuItem("Clear symbolic link");
        miClearSymbolicLinkOfNode.setOnAction(e -> editor.removeSymbolicLink(editor.getSelectedNodes()));

        MenuItem miRemoveNode = new MenuItem("Remove nodes");
        miRemoveNode.setOnAction(e -> editor.removeNodes(true));

        editNodeMenu.getItems().addAll(miSetNodeAttributes, miHorizontally, miVertically, miCopyOrMove, miMakeStairs, miRotateAndScale, miNormalizeCoordinates, menuAddSymbolicLinkOfNode, miClearSymbolicLinkOfNode, miRemoveNode);

        // EDIT_LINK モード
        // ・Set link attributes
        // ・Set one-way
        // ・Set road closed
        // ・Reset one-way / road closed
        // ・Add symbolic link
        // ・Clear symbolic link
        // ・Recalculate link length
        // ・Calculate scale and recalculate link length
        // ・Remove links

        MenuItem miSetLinkAttributes = new MenuItem("Set link attributes");
        miSetLinkAttributes.setOnAction(e -> openLinkAttributesDialog());

        MenuItem miSetOneWay = new MenuItem("Set one-way");
        miSetOneWay.setOnAction(e -> setOneWay());

        MenuItem miSetRoadClosed = new MenuItem("Set road closed");
        miSetRoadClosed.setOnAction(e -> editor.setRoadClosed());

        MenuItem miResetOneWayAndRoadClosed = new MenuItem("Reset one-way / road closed");
        miResetOneWayAndRoadClosed.setOnAction(e -> editor.resetOneWayRoadClosed());

        MenuItem miRecalculateLinkLength = new MenuItem("Recalculate link length");
        miRecalculateLinkLength.setOnAction(e -> openRecalculateLinkLengthDialog());

        MenuItem miCalculateScale = new MenuItem("Calculate scale and recalculate link length");
        miCalculateScale.setOnAction(e -> openCalculateScaleDialog());

        MenuItem miClearSymbolicLinkOfLink = new MenuItem("Clear symbolic link");
        miClearSymbolicLinkOfLink.setOnAction(e -> editor.removeSymbolicLink(editor.getSelectedLinks()));

        MenuItem miConvertToPolygon = new MenuItem("Convert to polygon");
        miConvertToPolygon.setOnAction(e -> {
            try {
                editor.convertToPolygon(editor.getSelectedLinks());
            } catch (Exception ex) {
                Alert alert = new Alert(AlertType.WARNING, ex.getMessage(), ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
            }
        });

        MenuItem miRemoveLink = new MenuItem("Remove links");
        miRemoveLink.setOnAction(e -> editor.removeLinks());

        editLinkMenu.getItems().addAll(miSetLinkAttributes, miSetOneWay, miSetRoadClosed, miResetOneWayAndRoadClosed, menuAddSymbolicLinkOfLink, miClearSymbolicLinkOfLink, miRecalculateLinkLength, miCalculateScale, miConvertToPolygon, miRemoveLink);

        // EDIT_AREA モード
        // ・Set area attributes
        // ・Remove areas

        MenuItem miSetAreaAttributes = new MenuItem("Set area attributes");
        miSetAreaAttributes.setOnAction(e -> openAreaAttributesDialog());

        MenuItem miRemoveArea = new MenuItem("Remove areas");
        miRemoveArea.setOnAction(e -> editor.removeAreas());

        editAreaMenu.getItems().addAll(miSetAreaAttributes, miRemoveArea);

        // EDIT_POLYGON モード
        // ・Set polygon attributes
        // ・Convert to nodes & links
        // ・Remove polygons

        MenuItem miSetPolygonAttributes = new MenuItem("Set polygon attributes");
        miSetPolygonAttributes.setOnAction(e -> openPolygonAttributesDialog());

        MenuItem miConvertToNodesAndLinks = new MenuItem("Convert to nodes & links");
        miConvertToNodesAndLinks.setOnAction(e -> editor.convertToNodesAndLinks(editor.getSelectedPolygons()));

        MenuItem miRemovePolygon = new MenuItem("Remove polygons");
        miRemovePolygon.setOnAction(e -> editor.removePolygons());

        editPolygonMenu.getItems().addAll(miSetPolygonAttributes, miConvertToNodesAndLinks, miRemovePolygon);

        // ・Add triangle mesh polygon
        MenuItem miAddTriangleMesh = new MenuItem("Add triangle mesh polygon");
        miAddTriangleMesh.setOnAction(e -> openAddTriangleMeshDialog());

        addTriangleMesheMenu.getItems().addAll(miAddTriangleMesh);

        // BACKGROUND_IMAGE モード
        // ・Set background image
        // ・Set background image attributes
        // ・Remove background image

        miSetBackgroundImage = new MenuItem("Set background image");
        miSetBackgroundImage.setOnAction(e -> setBackgroundImage());

        miSetBackgroundImageAttributes = new MenuItem("Set background image attributes");
        miSetBackgroundImageAttributes.setOnAction(e -> openBackgroundImageAttributesDialog());

        miRemoveBackgroundImage = new MenuItem("Remove background image");
        miRemoveBackgroundImage.setOnAction(e -> removeBackgroundImage());

        bgImageMenu.getItems().addAll(miSetBackgroundImage, miSetBackgroundImageAttributes, miRemoveBackgroundImage);
    }

    /**
     * シンボリックリンク追加サブメニューを更新する
     */
    public void updateAddSymbolicLinkMenu() {
        menuAddSymbolicLinkOfNode.getItems().clear();
        menuAddSymbolicLinkOfLink.getItems().clear();
        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == editor.getMap().getRoot() || group.getTags().size() == 0) {
                continue;
            }
            MenuItem menuItem = new MenuItem("Symbolic link to " + group.getTagString());
            menuItem.setOnAction(e -> addSymbolicLinkOfNode(group));
            menuAddSymbolicLinkOfNode.getItems().add(menuItem);

            menuItem = new MenuItem("Symbolic link to " + group.getTagString());
            menuItem.setOnAction(e -> addSymbolicLinkOfLink(group));
            menuAddSymbolicLinkOfLink.getItems().add(menuItem);
        }
    }

    /**
     * グループ選択パネルを更新する
     */
    public FlowPane updateGroupSelectionPane() {
        NetworkMap networkMap = editor.getMap();
        groupButtonMap.clear();
        groupButtons.clear();
        groupSelectionPane.getChildren().clear();
        groupToggleGroup.getToggles().clear();

        Label label = new Label("Group");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        groupSelectionPane.getChildren().add(label);
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group.getTags().size() == 0) {
                continue;
            }
            ToggleButton groupButton = new ToggleButton(group.getTagString());
            groupButton.setToggleGroup(groupToggleGroup);
            if (networkMap.getGroups().size() > 1 && group == networkMap.getRoot()) {
                groupButton.setDisable(true);
            } else {
                groupButton.setSelected(group == editor.getCurrentGroup());
                groupButton.setOnAction(e -> {
                    if (groupButton.isSelected()) {
                        editor.setCurrentGroup(group);
                        canvas.repaintLater();
                    }
                });
            }
            groupButtonMap.put(group, groupButton);
            groupButtons.add(groupButton);
            groupSelectionPane.getChildren().add(groupButton);
        }
        return groupSelectionPane;
    }

    /**
     * 編集モード選択パネルを構築する
     */
    private FlowPane createModeSelectionPane() {
        FlowPane flowPane = new FlowPane();
        flowPane.getStyleClass().add("custom-color-pane");
        flowPane.setPadding(new Insets(4));
        flowPane.setHgap(8);
        ToggleGroup group = new ToggleGroup();

        Label label = new Label("Mode");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));

        ToggleButton tbAddNode = new ToggleButton("Add Node");
        tbAddNode.setToggleGroup(group);
        tbAddNode.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.ADD_NODE);
        });

        ToggleButton tbAddLink = new ToggleButton("Add Link");
        tbAddLink.setToggleGroup(group);
        tbAddLink.setOnAction(e -> {
            addLinkAttributesPane();
            canvas.setMode(EditorMode.ADD_LINK);
        });

        ToggleButton tbAddNodeAndLink = new ToggleButton("Add Node & Link");
        tbAddNodeAndLink.setToggleGroup(group);
        tbAddNodeAndLink.setOnAction(e -> {
            addLinkAttributesPane();
            canvas.setMode(EditorMode.ADD_NODE_LINK);
        });

        ToggleButton tbEditNode = new ToggleButton("Edit Node");
        tbEditNode.setToggleGroup(group);
        tbEditNode.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.EDIT_NODE);
        });

        ToggleButton tbEditLink = new ToggleButton("Edit Link");
        tbEditLink.setToggleGroup(group);
        tbEditLink.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.EDIT_LINK);
        });

        ToggleButton tbEditArea = new ToggleButton("Edit Area");
        tbEditArea.setToggleGroup(group);
        tbEditArea.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.EDIT_AREA);
        });

        ToggleButton tbEditPolygon = new ToggleButton("Edit Polygon");
        tbEditPolygon.setToggleGroup(group);
        tbEditPolygon.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.EDIT_POLYGON);
        });

        ToggleButton tbBgImage = new ToggleButton("Background Image");
        tbBgImage.setToggleGroup(group);
        tbBgImage.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.BACKGROUND_IMAGE);
        });

        editModeButtons.add(tbAddNode);
        editModeButtons.add(tbAddLink);
        editModeButtons.add(tbAddNodeAndLink);
        editModeButtons.add(tbEditNode);
        editModeButtons.add(tbEditLink);
        editModeButtons.add(tbEditArea);
        editModeButtons.add(tbEditPolygon);
        editModeButtons.add(tbBgImage);
        flowPane.getChildren().addAll(label, tbAddNode, tbAddLink, tbAddNodeAndLink, tbEditNode, tbEditLink, tbEditArea, tbEditPolygon, tbBgImage);
        tbEditNode.setSelected(true);

        return flowPane;
    }

    /**
     * 編集モードを選択する
     */
    public void selectEditMode(int index) {
        if (index >= 0 && index < editModeButtons.size() && ! editModeButtons.get(index).isSelected()) {
            editModeButtons.get(index).fire();
        }
    }

    /**
     * リンク情報パネルを構築する
     */
    private HBox createLinkAttributesPane() {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setMinWidth(512);
        hbox.setSpacing(8);

        Label lengthLabel = new Label("length");
        linkLengthField.setDisable(true);
        linkLengthField.setPrefWidth(160);

        Label widthLabel = new Label("width");
        linkWidthField.setPrefWidth(160);

        hbox.getChildren().addAll(lengthLabel, linkLengthField, linkScaleLabel, widthLabel, linkWidthField);
        hbox.setMargin(linkScaleLabel, new Insets(0, 8, 0, 0));

        return hbox;
    }

    /**
     * ステータスバーにリンク情報パネルを追加する
     */
    private void addLinkAttributesPane() {
        linkLengthField.setText("0.0");
        linkScaleLabel.setText("(scale: " + editor.getCurrentGroup().getScale() + ")");
        if (statusPane.getChildren().size() == 1) {
            statusPane.getChildren().add(0, linkAttributesPane);
            statusPane.getChildren().add(1, new Separator(Orientation.VERTICAL));
        }
    }

    /**
     * ステータスバーからリンク情報パネルを削除する
     */
    private void removeLinkAttributesPane() {
        if (statusPane.getChildren().size() == 3) {
            statusPane.getChildren().remove(0, 2);
        }
    }

    /**
     * リンク長フィールドに値をセットする
     */
    public void setCurrentLinkLength(double length) {
        linkLengthField.setText("" + length);
    }

    /**
     * リンク幅フィールドの値を取得する
     */
    public double getCurrentLinkWidth() {
        Double width = convertToDouble(linkWidthField.getText());
        if (width == null) {
            return 0.0;
        }
        if (width <= 0.0) {
            alertInvalidInputValue("Incorrect width.");
        }
        return width;
    }

    /**
     * 現在のマップデータで GUI を再設定する
     */
    public void resetGui() {
        editor.initCurrentGroup();
        editor.updateHeight();
        groupPanel.clear();
        groupPanel.construct();
        nodePanel.reset();
        linkPanel.reset();
        areaPanel.reset();
        polygonPanel.reset();
        scenarioPanel.reset();
        updateRotationMenu();
        updateColorDepthOfBackgroundImageMenu();
        updateColorDepthOfBackgroundMapMenu();
        updateShowBackgroundGroupMenu();
        updateAddSymbolicLinkMenu();
        updateGroupSelectionPane();
        canvas.repaintLater();
    }

    /**
     * マップデータを消去して新規状態にする
     */
    public void clearMapData() {
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data has been modified.\n    Do you wish to continue anyway?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() == ButtonType.NO) {
                return;
            }
        }
        editor.initSetupFileInfo();
        editor.initNetworkMap();
        canvas.clearEditingStates();
        resetGui();
    }

    /**
     * ファイル選択ダイアログを開いてマップファイルを読み込む
     */
    public void openMap() {
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data has been modified.\n    Do you wish to continue anyway?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() == ButtonType.NO) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open map");
        String fileName = editor.getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            String dirName = settings.get("mapDir", "");
            if (dirName.isEmpty() || ! new File(dirName).exists()) {
                dirName = "./";
            }
            fileChooser.setInitialDirectory(new File(dirName));
            fileChooser.setInitialFileName(settings.get("mapFile", ""));     // TODO: 現状では無効
        } else {
            setInitialPath(fileChooser, fileName);
        }
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML", "*.xml"),
            new FileChooser.ExtensionFilter("All", "*.*")
        );
        File file = fileChooser.showOpenDialog(frame);
        if (file == null) {
            return;
        }
        try {
            editor.setDir(file.getParentFile().getCanonicalFile());
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e);
            Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }
        editor.setNetworkMapFile(editor.getRelativePath(file));
        if (! editor.loadNetworkMap()) {
            Alert alert = new Alert(AlertType.WARNING, "Map file open error.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            editor.initNetworkMap();
        }
        canvas.clearEditingStates();
        resetGui();
    }

    /**
     * マップファイルを保存する
     */
    public void saveMap() {
        if (! editor.isModified() && ! editor.isNormalized()) {
            Alert alert = new Alert(AlertType.INFORMATION, "Map data is not modified.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        String fileName = editor.getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            saveMapAs();
            return;
        }

        if (! totalValidation(false, "Do you wish to continue anyway?")) {
            return;
        }

        if (! editor.saveMap()) {
            Alert alert = new Alert(AlertType.ERROR, "Save map file failed: " + fileName, ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
        }
    }

    /**
     * マップファイルに名前を付けて保存する
     */
    public void saveMapAs() {
        if (! totalValidation(false, "Do you wish to continue anyway?")) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save map as");
        String fileName = editor.getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            String dirName = settings.get("mapDir", "");
            if (dirName.isEmpty() || ! new File(dirName).exists()) {
                dirName = "./";
            }
            fileChooser.setInitialDirectory(new File(dirName));
            fileChooser.setInitialFileName(settings.get("mapFile", ""));     // TODO: 現状では無効
        } else {
            setInitialPath(fileChooser, fileName);
        }
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML", "*.xml"),
            new FileChooser.ExtensionFilter("All", "*.*")
        );
        File file = fileChooser.showSaveDialog(frame);
        if (file == null) {
            return;
        }

        try {
            fileName = file.getCanonicalPath();
            editor.setDir(file.getParentFile().getCanonicalFile());
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e);
            Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }
        editor.setNetworkMapFile(fileName);
        if (! editor.saveMap()) {
            Alert alert = new Alert(AlertType.ERROR, "Save map file failed: " + fileName, ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
        }
    }

    /**
     * ファイル選択ダイアログを開いてプロパティファイルを読み込む
     */
    public void openProperties() {
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data has been modified.\n    Do you wish to continue anyway?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() == ButtonType.NO) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open properties");
        String fileName = editor.getPropertiesFile();
        if (fileName == null || fileName.isEmpty()) {
            String dirName = settings.get("propertiesDir", "");
            if (dirName.isEmpty() || ! new File(dirName).exists()) {
                dirName = "./";
            }
            fileChooser.setInitialDirectory(new File(dirName));
            fileChooser.setInitialFileName(settings.get("propertiesFile", ""));     // TODO: 現状では無効
        } else {
            setInitialPath(fileChooser, fileName);
        }
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON", "*.json"),
            new FileChooser.ExtensionFilter("XML", "*.xml")
        );
        File file = fileChooser.showOpenDialog(frame);
        if (file == null) {
            return;
        }

        editor.setPropertiesFromFile(editor.getRelativePath(file));
        if (! editor.loadNetworkMap()) {
            Alert alert = new Alert(AlertType.WARNING, "Map file open error.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            editor.initSetupFileInfo();
            editor.initNetworkMap();
        }
        canvas.clearEditingStates();
        resetGui();
    }

    /**
     * GUI シミュレータを起動する
     */
    private void simulate(String simulator) {
        if (editor.getNetworkMapFile() == null || editor.getNetworkMapFile().isEmpty() || editor.getGenerationFile() == null || editor.getGenerationFile().isEmpty() || editor.getScenarioFile() == null || editor.getScenarioFile().isEmpty()) {
            Alert alert = new Alert(AlertType.INFORMATION, "There are not enough files for simulation.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }
        setDisableSimulationButtons(true);
        GuiSimulationLauncher launcher = GuiSimulationLauncher.createInstance(simulator);
        launcher.init(editor, settings);
        launcher.simulate();
    }

    /**
     * リンクを表すライン群表示用の Canvas
     */
    private class LinkViewCanvas extends Canvas {
        private HashMap<String, ArrayList<ArrayList<Point2D>>> linesOfMeshCode;
        private Rectangle2D boundary = Rectangle2D.EMPTY;

        public LinkViewCanvas(HashMap<String, ArrayList<ArrayList<Point2D>>> linesOfMeshCode) {
            this.linesOfMeshCode = linesOfMeshCode;
            widthProperty().addListener(evt -> draw());
            heightProperty().addListener(evt -> draw());
        }

        public void update() {
            boolean passing = false;
            double minX = 0.0;
            double maxX = 0.0;
            double minY = 0.0;
            double maxY = 0.0;
            for (ArrayList<ArrayList<Point2D>> lines : linesOfMeshCode.values()) {
                for (ArrayList<Point2D> line : lines) {
                    for (Point2D point : line) {
                        if (passing) {
                            if (point.getX() < minX) {
                                minX = point.getX();
                            }
                            if (point.getX() > maxX) {
                                maxX = point.getX();
                            }
                            if (point.getY() < minY) {
                                minY = point.getY();
                            }
                            if (point.getY() > maxY) {
                                maxY = point.getY();
                            }
                        } else {
                            minX = point.getX();
                            maxX = point.getX();
                            minY = point.getY();
                            maxY = point.getY();
                            passing = true;
                        }
                    }
                }
            }
            boundary = new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
            draw();
        }

        private void draw() {
            double width = getWidth();
            double height = getHeight();

            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, width, height);
            if (boundary.getHeight() == 0.0) {
                return;
            }

            double k = Math.min(width / boundary.getWidth(), height / boundary.getHeight());
            double offsetX = (width - boundary.getWidth() * k) / 2.0;
            double offsetY = (height - boundary.getHeight() * k) / 2.0;

            gc.setStroke(FxColor.DEFAULT_LINK_COLOR);
            for (ArrayList<ArrayList<Point2D>> lines : linesOfMeshCode.values()) {
                for (ArrayList<Point2D> line : lines) {
                    Point2D lastPoint = null;
                    for (Point2D point : line) {
                        Point2D _point = new Point2D((point.getX() - boundary.getMinX()) * k + offsetX, (point.getY() - boundary.getMinY()) * k + offsetY);
                        if (lastPoint != null) {
                            gc.strokeLine(lastPoint.getX(), lastPoint.getY(), _point.getX(), _point.getY());
                        }
                        lastPoint = _point;
                    }
                }
            }
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public double prefWidth(double height) {
            return getWidth();
        }

        @Override
        public double prefHeight(double width) {
            return getHeight();
        }
    }

    /**
     * シェープファイルを読み込む
     */
    private void readShapefile() {
        NetworkMap networkMap = editor.getMap();
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();

        // root グループには読み込めない
        if (networkMap.getGroups().size() == 1) {
            Alert alert = new Alert(AlertType.WARNING, "Please create a new group.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        // 全グループの scale が 1.0 である事
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group.getScale() != 1.0) {
                Alert alert = new Alert(AlertType.WARNING, "Group scale must be all 1.0", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
        }

        // 外部プログラム "ogr2ogr" が使える事を確認する
        ProcessBuilder pb = new ProcessBuilder("ogr2ogr", "--version");
        pb.inheritIO();
        try {
            Process process = pb.start();
            int ret = process.waitFor();
            if (ret != 0) {
                Alert alert = new Alert(AlertType.WARNING, "External program \"ogr2ogr\" can not be executed.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
        } catch (IOException e) {
            Alert alert = new Alert(AlertType.WARNING, "External program \"ogr2ogr\" not found.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        } catch (InterruptedException e) {
	    Itk.dumpStackTraceOf(e);
            return;
        }

        String[] shapefileSpecNames = shapefileSpecs.keySet().toArray(new String[0]);
        HashMap<String, ArrayList<ArrayList<Point2D>>> linesOfMeshCode = new HashMap();

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Read shapefile");
        dialog.setResizable(true);

        LinkViewCanvas canvas = new LinkViewCanvas(linesOfMeshCode);
        StackPane canvasPane = new StackPane();
        canvasPane.getChildren().add(canvas);
        canvasPane.setMinWidth(800);
        canvasPane.setMinHeight(600);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        // Preset
        ChoiceBox<String> presetChoiceBox = new ChoiceBox(FXCollections.observableArrayList(shapefileSpecNames));

        // Geodetic datum
        ChoiceBox<String> datumChoiceBox = new ChoiceBox();
        datumChoiceBox.getItems().addAll("Tokyo", "WGS84");
        datumChoiceBox.setValue("Tokyo");

        // Round off coordinate to
        CheckBox roundOffCheckBox = new CheckBox("Round off coordinate to");
        TextField roundOffField = new TextField("");
        roundOffCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            roundOffField.setDisable(! newValue);
        });

        Label lengthLabel = new Label("length:");
        lengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, lengthLabel.getFont().getSize()));
        lengthLabel.setPadding(new Insets(12, 0, 0, 0));

        TextField lengthNameField = new TextField("");
        CheckBox useGeodeticLengthCheckBox = new CheckBox("Use geodetic length");

        Label widthLabel = new Label("width:");
        widthLabel.setFont(Font.font("Arial", FontWeight.BOLD, widthLabel.getFont().getSize()));
        widthLabel.setPadding(new Insets(12, 0, 0, 0));

        TextField widthNameField = new TextField("");

        // Correction factor
        CheckBox factorCheckBox = new CheckBox("Correction factor");
        TextField factorField = new TextField("");
        factorCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            factorField.setDisable(! newValue);
        });

        // Use reference table
        CheckBox referenceTableCheckBox = new CheckBox("Use reference table");
        TextField referenceTableField = new TextField("");
        referenceTableField.setPrefWidth(360);
        referenceTableCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            referenceTableField.setDisable(! newValue);
        });

        presetChoiceBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            Map<String, Object> shapefileSpec = (Map<String, Object>)shapefileSpecs.get(newValue);
            if (shapefileSpec == null) {
                return;
            }
            datumChoiceBox.setValue((String)shapefileSpec.get("geodetic_datum"));
            Object object = shapefileSpec.get("round_off_coordinate_to");
            if (object == null) {
                roundOffField.setText("");
                roundOffCheckBox.setSelected(false);
            } else {
                roundOffField.setText(object.toString());
                roundOffCheckBox.setSelected(true);
            }

            Map<String, Object> lengthObjects = (Map<String, Object>)shapefileSpec.get("length");
            lengthNameField.setText((String)lengthObjects.get("attribute_name"));

            Map<String, Object> widthObjects = (Map<String, Object>)shapefileSpec.get("width");
            widthNameField.setText((String)widthObjects.get("attribute_name"));
            object = widthObjects.get("correction_factor");
            if (object == null) {
                factorField.setText("");
                factorCheckBox.setSelected(false);
            } else {
                factorField.setText(object.toString());
                factorCheckBox.setSelected(true);
            }
            object = widthObjects.get("reference_table");
            if (object == null) {
                referenceTableField.setText("");
                referenceTableCheckBox.setSelected(false);
            } else {
                referenceTableField.setText(((ArrayList<Object>)object).toString().replaceAll("\\[|\\]", ""));
                referenceTableCheckBox.setSelected(true);
            }
        });

        // Destination group
        HashMap<String, MapPartGroup> groups = new HashMap();
        ArrayList<String> groupNames = new ArrayList();
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group == root || group.getTags().size() == 0) {
                continue;
            }
            groups.put(group.getTagString(), group);
            groupNames.add(group.getTagString());
        }
        ChoiceBox<String> groupChoiceBox = new ChoiceBox(FXCollections.observableArrayList(groupNames));
        groupChoiceBox.setValue(editor.getCurrentGroup().getTagString());

        // Zone
        ArrayList<String> zones = new ArrayList();
        for (int zone = 1; zone <= GsiTile.JGD2000_JPR_EPSG_NAMES.length - 1; zone++) {
            zones.add("" + zone);
        }
        ChoiceBox<String> zoneChoiceBox = new ChoiceBox(FXCollections.observableArrayList(zones));
        zoneChoiceBox.setValue(root.getZone() == 0 ? zones.get(0) : ("" + root.getZone()));
        Button zoneReference = new Button("Reference");
        zoneReference.setOnAction(e -> {
            helpStage.setTitle("Help - Zone of plane rectangular coordinate system");
            helpStage.setWidth(900);
            helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
            webView.getEngine().load(ZONE_REFERENCE_URI);
            helpStage.show();
            helpStage.toFront();
        });
        if (CrowdWalkLauncher.offline) {
            zoneReference.setDisable(true);
        }
        FlowPane zonePane = new FlowPane();
        zonePane.setPrefWidth(160);
        zonePane.setHgap(8);
        zonePane.setAlignment(Pos.CENTER_LEFT);
        zonePane.getChildren().addAll(zoneChoiceBox, zoneReference);

        // Shapefiles
        Label shapefilesLabel = new Label("Shapefiles");
        shapefilesLabel.setFont(Font.font("Arial", FontWeight.BOLD, shapefilesLabel.getFont().getSize()));

        ListView<String> shapefileList = new ListView<>();
        shapefileList.setPrefHeight(180);

        StringBuilder path = new StringBuilder();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open shapefile");
        setInitialPath(fileChooser, path.toString());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("shp", "*.shp")
        );

        Button addButton = new Button("Add");
        addButton.setOnAction(e -> {
            String srcEpsg = "EPSG:4326";   // WGS84
            if (datumChoiceBox.getValue().equals("Tokyo")) {
                srcEpsg = "EPSG:4301";      // 旧日本測地系
            }
            int zone = Integer.parseInt(zoneChoiceBox.getValue());
            setInitialPath(fileChooser, path.toString());
            File file = fileChooser.showOpenDialog(frame);
            if (file != null) {
                try {
                    path.setLength(0);
                    path.append(file.getCanonicalPath());
                    String pathName = path.toString();
                    if (linesOfMeshCode.keySet().contains(pathName)) {
                        return;
                    }
                    ArrayList<ArrayList<Point2D>> lines = null;
                    try {
                        lines = editor.shapefileToLines(srcEpsg, zone, pathName);
                    } catch (Exception ex) {
                        Itk.logError("Read shapefile", ex.getMessage());
                        Alert alert = new Alert(AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                        alert.initOwner(frame);
                        alert.showAndWait();
                        return;
                    }
                    shapefileList.getItems().add(pathName);
                    linesOfMeshCode.put(pathName, lines);
                    // refresh
                    canvas.update();
                } catch (IOException ex) {
		    Itk.dumpStackTraceOf(ex);
                }
            }
        });

        Button removeButton = new Button("Remove");
        removeButton.setOnAction(e -> {
            ObservableList<String> list = shapefileList.getSelectionModel().getSelectedItems();
            if (! list.isEmpty()) {
                String pathName = list.get(0);
                shapefileList.getItems().remove(pathName);
                linesOfMeshCode.remove(pathName);
                // refresh
                canvas.update();
            }
        });

        FlowPane buttonPane = new FlowPane();
        buttonPane.setHgap(8);
        buttonPane.setAlignment(Pos.CENTER_RIGHT);
        buttonPane.getChildren().addAll(addButton, removeButton);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(8);
        grid.setVgap(6);
        int row = 0;
        grid.add(new Label("Preset"), 0, row, 2, 1);
        grid.add(presetChoiceBox, 2, row++);

        grid.add(new Separator(), 0, row++, 3, 1);

        grid.add(new Label("Geodetic datum"), 0, row, 2, 1);
        grid.add(datumChoiceBox, 2, row++);

        grid.add(roundOffCheckBox, 0, row, 2, 1);
        grid.add(roundOffField, 2, row++);

        grid.add(lengthLabel, 0, row++, 3, 1);
        grid.add(new Label("attribute name"), 1, row, 1, 1);
        grid.add(lengthNameField, 2, row++, 1, 1);
        grid.add(useGeodeticLengthCheckBox, 1, row++, 2, 1);

        grid.add(widthLabel, 0, row++, 3, 1);
        grid.add(new Label("attribute name"), 1, row, 1, 1);
        grid.add(widthNameField, 2, row++, 1, 1);
        grid.add(factorCheckBox, 1, row, 1, 1);
        grid.add(factorField, 2, row++, 1, 1);
        grid.add(referenceTableCheckBox, 1, row++, 2, 1);
        grid.add(referenceTableField, 1, row++, 2, 1);

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        grid.add(separator, 0, row++, 3, 1);

        grid.add(new Label("Destination group"), 0, row, 2, 1);
        grid.add(groupChoiceBox, 2, row++);
        grid.add(new Label("Zone"), 0, row, 2, 1);
        grid.add(zonePane, 2, row++);

        grid.add(shapefilesLabel, 0, row++, 3, 1);
        grid.add(shapefileList, 0, row++, 3, 1);
        grid.add(buttonPane, 0, row++, 3, 1);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(canvasPane);
        borderPane.setRight(grid);
        dialog.getDialogPane().setContent(borderPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 1番目のプリセットで初期化する
        presetChoiceBox.setValue(shapefileSpecNames[0]);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String srcEpsg = "EPSG:4326";   // WGS84
            if (datumChoiceBox.getValue().equals("Tokyo")) {
                srcEpsg = "EPSG:4301";      // 旧日本測地系
            }
            Integer scaleOfRoundOff = null;
            if (roundOffCheckBox.isSelected()) {
                scaleOfRoundOff = convertToInteger(roundOffField.getText());
                if (scaleOfRoundOff == null) {
                    return;
                }
            }
            String lengthName = lengthNameField.getText().trim();
            if (lengthName.isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING, "\"length\" attribute name is empty.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
            String widthName = widthNameField.getText().trim();
            if (widthName.isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING, "\"width\" attribute name is empty.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
            Double correctionFactor = null;
            if (factorCheckBox.isSelected()) {
                correctionFactor = convertToDouble(factorField.getText());
                if (correctionFactor == null) {
                    return;
                }
            }
            ArrayList<Double> referenceTable = new ArrayList();
            if (referenceTableCheckBox.isSelected()) {
                String[] values = referenceTableField.getText().trim().split("\\s*,\\s*");
                for (String value : values) {
                    Double doubleValue = convertToDouble(value);
                    if (doubleValue == null) {
                        return;
                    }
                    referenceTable.add(doubleValue);
                }
            }
            MapPartGroup group = groups.get(groupChoiceBox.getValue());
            int zone = Integer.parseInt(zoneChoiceBox.getValue());

            try {
                editor.readShapefile(srcEpsg, scaleOfRoundOff == null ? -1 : scaleOfRoundOff.intValue(), lengthName, useGeodeticLengthCheckBox.isSelected(), widthName, correctionFactor, referenceTable, group, zone, shapefileList.getItems());
            } catch (Exception e) {
                Itk.logError("Read shapefile", e.getMessage());
                Alert alert = new Alert(AlertType.ERROR, e.getMessage() + "\n\nMap data is over on the way.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
            }
        }
    }

    /**
     * MRD ファイルを読み込む
     */
    private void readMrdFile() {
        NetworkMap networkMap = editor.getMap();
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();

        // root グループには読み込めない
        if (networkMap.getGroups().size() == 1) {
            Alert alert = new Alert(AlertType.WARNING, "Please create a new group.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        // 全グループの scale が 1.0 である事
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group.getScale() != 1.0) {
                Alert alert = new Alert(AlertType.WARNING, "Group scale must be all 1.0", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
        }

        HashMap<String, ArrayList<ArrayList<Point2D>>> linesOfMeshCode = new HashMap();

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Read MRD file");
        dialog.setResizable(true);

        LinkViewCanvas canvas = new LinkViewCanvas(linesOfMeshCode);
        StackPane canvasPane = new StackPane();
        canvasPane.getChildren().add(canvas);
        canvasPane.setMinWidth(800);
        canvasPane.setMinHeight(600);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        // Destination group
        HashMap<String, MapPartGroup> groups = new HashMap();
        ArrayList<String> groupNames = new ArrayList();
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group == root || group.getTags().size() == 0) {
                continue;
            }
            groups.put(group.getTagString(), group);
            groupNames.add(group.getTagString());
        }
        ChoiceBox<String> groupChoiceBox = new ChoiceBox(FXCollections.observableArrayList(groupNames));
        groupChoiceBox.setValue(editor.getCurrentGroup().getTagString());

        // Zone
        ArrayList<String> zones = new ArrayList();
        for (int zone = 1; zone <= GsiTile.JGD2000_JPR_EPSG_NAMES.length - 1; zone++) {
            zones.add("" + zone);
        }
        ChoiceBox<String> zoneChoiceBox = new ChoiceBox(FXCollections.observableArrayList(zones));
        zoneChoiceBox.setValue(root.getZone() == 0 ? zones.get(0) : ("" + root.getZone()));
        Button zoneReference = new Button("Reference");
        zoneReference.setOnAction(e -> {
            helpStage.setTitle("Help - Zone of plane rectangular coordinate system");
            helpStage.setWidth(900);
            helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
            webView.getEngine().load(ZONE_REFERENCE_URI);
            helpStage.show();
            helpStage.toFront();
        });
        if (CrowdWalkLauncher.offline) {
            zoneReference.setDisable(true);
        }
        FlowPane zonePane = new FlowPane();
        zonePane.setPrefWidth(160);
        zonePane.setHgap(8);
        zonePane.setAlignment(Pos.CENTER_LEFT);
        zonePane.getChildren().addAll(zoneChoiceBox, zoneReference);

        CheckBox allRoadIncludingCheckBox = new CheckBox("Include \"All road node / All road link\"");
        allRoadIncludingCheckBox.setPadding(new Insets(0, 0, 8, 0));
        allRoadIncludingCheckBox.setSelected(true);

        // MRD files
        Label mrdFilesLabel = new Label("MRD files");
        mrdFilesLabel.setFont(Font.font("Arial", FontWeight.BOLD, mrdFilesLabel.getFont().getSize()));

        ListView<String> mrdFileList = new ListView<>();
        mrdFileList.setPrefHeight(460);

        StringBuilder path = new StringBuilder();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open MRD file");
        setInitialPath(fileChooser, path.toString());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("mrd", "*.mrd")
        );

        Button addButton = new Button("Add");
        addButton.setOnAction(e -> {
            int zone = Integer.parseInt(zoneChoiceBox.getValue());
            setInitialPath(fileChooser, path.toString());
            File file = fileChooser.showOpenDialog(frame);
            if (file != null) {
                try {
                    path.setLength(0);
                    path.append(file.getCanonicalPath());
                    String pathName = path.toString();
                    if (linesOfMeshCode.keySet().contains(pathName)) {
                        return;
                    }
                    ArrayList<ArrayList<Point2D>> lines = null;
                    try {
                        MRDConverter mrd = new MRDConverter(file, zone);
                        lines = mrd.convertToLines(allRoadIncludingCheckBox.isSelected());
                    } catch (Exception ex) {
                        Itk.logError("Read shapefile", ex.getMessage());
                        Alert alert = new Alert(AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                        alert.initOwner(frame);
                        alert.showAndWait();
                        return;
                    }
                    mrdFileList.getItems().add(pathName);
                    linesOfMeshCode.put(pathName, lines);
                    // refresh
                    canvas.update();
                } catch (IOException ex) {
                    Itk.dumpStackTraceOf(ex);
                }
            }
        });

        Button removeButton = new Button("Remove");
        removeButton.setOnAction(e -> {
            ObservableList<String> list = mrdFileList.getSelectionModel().getSelectedItems();
            if (! list.isEmpty()) {
                String pathName = list.get(0);
                mrdFileList.getItems().remove(pathName);
                linesOfMeshCode.remove(pathName);
                // refresh
                canvas.update();
            }
        });

        FlowPane buttonPane = new FlowPane();
        buttonPane.setHgap(8);
        buttonPane.setAlignment(Pos.CENTER_RIGHT);
        buttonPane.getChildren().addAll(addButton, removeButton);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(8);
        grid.setVgap(6);
        int row = 0;

        grid.add(new Label("Destination group"), 0, row, 2, 1);
        grid.add(groupChoiceBox, 2, row++);
        grid.add(new Label("Zone"), 0, row, 2, 1);
        grid.add(zonePane, 2, row++);
        grid.add(allRoadIncludingCheckBox, 0, row++);

        grid.add(mrdFilesLabel, 0, row++, 3, 1);
        grid.add(mrdFileList, 0, row++, 3, 1);
        grid.add(buttonPane, 0, row++, 3, 1);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(canvasPane);
        borderPane.setRight(grid);
        dialog.getDialogPane().setContent(borderPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            MapPartGroup group = groups.get(groupChoiceBox.getValue());
            int zone = Integer.parseInt(zoneChoiceBox.getValue());

            try {
                editor.startOfCommandBlock();
                for (String pathName : mrdFileList.getItems()) {
                    MRDConverter mrd = new MRDConverter(new File(pathName), zone);
                    mrd.addToNetworkMap(editor, group, zone, allRoadIncludingCheckBox.isSelected());
                }
            } catch (Exception e) {
                Itk.dumpStackTraceOf(e);
                Itk.logError("Read MRD file", e.getMessage());
                Alert alert = new Alert(AlertType.ERROR, e.getMessage() + "\n\nMap data is over on the way.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
            } finally {
                editor.endOfCommandBlock();
                editor.updateHeight();
            }
        }
    }

    /**
     * OpenStreetMap データを読み込む
     */
    private void readOpenStreetMap() {
        NetworkMap networkMap = editor.getMap();
        if (networkMap.getGroups().size() < 2) {
            Alert alert = new Alert(AlertType.WARNING, "Please create a new group.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();

        OsmReader osmReader = null;
        try {
            osmReader = new OsmReader(editor);
        } catch (Exception e) {
            Itk.logError("Read OSM", e.getMessage());
            Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Read OpenStreetMap");
        dialog.getDialogPane().setPrefWidth(416);
        VBox paramPane = new VBox();
        paramPane.setPadding(new Insets(16, 24, 12, 24));
        paramPane.setSpacing(8);

        // Destination group
        HashMap<String, MapPartGroup> groups = new HashMap();
        ArrayList<String> groupNames = new ArrayList();
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group == networkMap.getRoot() || group.getTags().size() == 0) {
                continue;
            }
            groups.put(group.getTagString(), group);
            groupNames.add(group.getTagString());
        }
        ChoiceBox groupChoiceBox = new ChoiceBox(FXCollections.observableArrayList(groupNames));
        groupChoiceBox.setValue(editor.getCurrentGroup().getTagString());

        // Zone
        ArrayList<String> zones = new ArrayList();
        for (int zone = 1; zone <= GsiTile.JGD2000_JPR_EPSG_NAMES.length - 1; zone++) {
            zones.add("" + zone);
        }
        ChoiceBox<String> zoneChoiceBox = new ChoiceBox(FXCollections.observableArrayList(zones));
        zoneChoiceBox.setValue(root.getZone() == 0 ? zones.get(0) : ("" + root.getZone()));
        Button zoneReference = new Button("Reference");
        zoneReference.setOnAction(e -> {
            helpStage.setTitle("Help - Zone of plane rectangular coordinate system");
            helpStage.setWidth(900);
            helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
            webView.getEngine().load(ZONE_REFERENCE_URI);
            helpStage.show();
            helpStage.toFront();
        });
        if (CrowdWalkLauncher.offline) {
            zoneReference.setDisable(true);
        }
        FlowPane zonePane = new FlowPane();
        zonePane.setPrefWidth(160);
        zonePane.setHgap(8);
        zonePane.setAlignment(Pos.CENTER_LEFT);
        zonePane.getChildren().addAll(zoneChoiceBox, zoneReference);

        // Highway value to convert to link
        Label highwayLabel = new Label("Highway value to convert to link");
        highwayLabel.setFont(Font.font("Arial", FontWeight.BOLD, highwayLabel.getFont().getSize()));

        VBox highwayCheckBoxesPane = new VBox();
        highwayCheckBoxesPane.setPadding(new Insets(8));
        highwayCheckBoxesPane.setSpacing(4);

        ArrayList<CheckBox> highwayCheckBoxes = new ArrayList();
        for (String highwayValue : osmReader.getHighwayValues()) {
            if (highwayValue.equals("----")) {
                Separator separator = new Separator();
                separator.setPadding(new Insets(3, 0, 2, 0));
                highwayCheckBoxesPane.getChildren().add(separator);
            } else {
                CheckBox checkBox = new CheckBox(highwayValue);
                checkBox.setSelected(true);
                highwayCheckBoxes.add(checkBox);
                highwayCheckBoxesPane.getChildren().add(checkBox);
            }
        }

        ScrollPane scroller = new ScrollPane();
        scroller.setPrefSize(360, 144);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        scroller.setContent(highwayCheckBoxesPane);

        Button checkAllButton = new Button("Check all");
        checkAllButton.setOnAction(e -> {
            highwayCheckBoxes.forEach(checkBox -> checkBox.setSelected(true));
        });

        Button uncheckAllButton = new Button("Uncheck all");
        uncheckAllButton.setOnAction(e -> {
            highwayCheckBoxes.forEach(checkBox -> checkBox.setSelected(false));
        });

        FlowPane checkAllPane = new FlowPane();
        checkAllPane.setHgap(8);
        checkAllPane.setAlignment(Pos.CENTER_RIGHT);
        checkAllPane.getChildren().addAll(checkAllButton, uncheckAllButton);

        // Tag filter
        TextArea tagFilterArea = new TextArea(OsmReader.DEFAULT_TAG_FILTER);
        tagFilterArea.setPrefSize(360, 60);
        tagFilterArea.setWrapText(true);
        CheckBox rejectCheckBox = new CheckBox("Reject");

        // OSM data from
        Label dataFromLabel = new Label("OSM data from");
        dataFromLabel.setFont(Font.font("Arial", FontWeight.BOLD, dataFromLabel.getFont().getSize()));
        RadioButton selectFileButton = new RadioButton("file");
        RadioButton selectWebButton = new RadioButton("web service");
        ToggleGroup toggleGroup = new ToggleGroup();
        selectFileButton.setToggleGroup(toggleGroup);
        selectWebButton.setToggleGroup(toggleGroup);
        selectFileButton.setSelected(true);
        FlowPane selectPane = new FlowPane();
        selectPane.setHgap(8);
        selectPane.getChildren().addAll(dataFromLabel, selectFileButton, selectWebButton);

        Label fileNameLabel = new Label("File name");
        TextField fileNameField = new TextField("");
        Button openButton = new Button("Open");
        openButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open OSM file");
            setInitialPath(fileChooser, "");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OSM", "*.osm")
            );
            File file = fileChooser.showOpenDialog(frame);
            if (file != null) {
                try {
                    fileNameField.setText(file.getCanonicalPath());
                } catch (IOException ex) {
		    Itk.dumpStackTraceOf(ex);
                }
            }
        });

        Label upperLeftLabel = new Label("Upper left coordinate");
        upperLeftLabel.setFont(Font.font("Arial", FontWeight.BOLD, upperLeftLabel.getFont().getSize()));
        Label longitudeLabel = new Label("Longitude");
        TextField ulLongitudeField = new TextField("");
        ulLongitudeField.setPrefWidth(200);
        Label latitudeLabel = new Label("Latitude");
        TextField ulLatitudeField = new TextField("");
        ulLatitudeField.setPrefWidth(200);

        Label lowerRightLabel = new Label("Lower right coordinate");
        lowerRightLabel.setFont(Font.font("Arial", FontWeight.BOLD, lowerRightLabel.getFont().getSize()));
        Label lrLongitudeLabel = new Label("Longitude");
        Label lrLatitudeLabel = new Label("Latitude");
        TextField lrLongitudeField = new TextField("");
        lrLongitudeField.setPrefWidth(200);
        TextField lrLatitudeField = new TextField("");
        lrLatitudeField.setPrefWidth(200);

        List<Node> fromFileControls = Arrays.asList(fileNameLabel, fileNameField, openButton);
        List<Node> fromWebControls = Arrays.asList(upperLeftLabel, longitudeLabel, latitudeLabel, ulLongitudeField, ulLatitudeField, lowerRightLabel, lrLongitudeLabel, lrLatitudeLabel, lrLongitudeField, lrLatitudeField);
        fromWebControls.forEach(node -> node.setDisable(true));

        selectFileButton.setOnAction(e -> {
            fromFileControls.forEach(node -> node.setDisable(false));
            fromWebControls.forEach(node -> node.setDisable(true));
        });
        selectWebButton.setOnAction(e -> {
            fromFileControls.forEach(node -> node.setDisable(true));
            fromWebControls.forEach(node -> node.setDisable(false));
        });

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        int row = 0;
        grid.add(new Label("Destination group"), 0, row, 2, 1);
        grid.add(groupChoiceBox, 2, row++);
        grid.add(new Label("Zone"), 0, row, 2, 1);
        grid.add(zonePane, 2, row++);

        grid.add(highwayLabel, 0, row++, 3, 1);
        grid.add(scroller, 0, row++, 3, 1);
        grid.add(checkAllPane, 0, row++, 3, 1);

        grid.add(new Label("Tag filter"), 0, row++, 3, 1);
        grid.add(tagFilterArea, 0, row++, 3, 1);
        grid.add(rejectCheckBox, 1, row++, 2, 1);

        grid.add(new Separator(), 0, row++, 3, 1);

        grid.add(selectPane, 0, row++, 3, 1);
        grid.add(fileNameLabel, 0, row++, 3, 1);
        grid.add(fileNameField, 0, row++, 3, 1);
        grid.add(openButton, 1, row++, 2, 1);

        grid.add(upperLeftLabel, 0, row++, 3, 1);
        grid.add(longitudeLabel, 0, row, 2, 1);
        grid.add(ulLongitudeField, 2, row++);
        grid.add(latitudeLabel, 0, row, 2, 1);
        grid.add(ulLatitudeField, 2, row++);

        grid.add(lowerRightLabel, 0, row++, 3, 1);
        grid.add(lrLongitudeLabel, 0, row, 2, 1);
        grid.add(lrLongitudeField, 2, row++);
        grid.add(lrLatitudeLabel, 0, row, 2, 1);
        grid.add(lrLatitudeField, 2, row++);

        paramPane.getChildren().addAll(grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            MapPartGroup group = groups.get(groupChoiceBox.getValue());
            int zone = Integer.parseInt((String)zoneChoiceBox.getValue());
            ArrayList<String> validHighways = new ArrayList();
            for (CheckBox checkBox : highwayCheckBoxes) {
                if (checkBox.isSelected()) {
                    validHighways.add(checkBox.getText());
                }
            }
            try {
                if (selectFileButton.isSelected()) {
                    String fileName = fileNameField.getText().trim();
                    if (fileName.isEmpty()) {
                        Alert alert = new Alert(AlertType.WARNING, "File name is empty.", ButtonType.OK);
                        alert.initOwner(frame);
                        alert.showAndWait();
                        return;
                    }
                    File file = new File(fileName);
                    if (! file.exists()) {
                        Alert alert = new Alert(AlertType.WARNING, "File not found.", ButtonType.OK);
                        alert.initOwner(frame);
                        alert.showAndWait();
                        return;
                    }
                    osmReader.read(file, group, validHighways, tagFilterArea.getText(), rejectCheckBox.isSelected(), zone);
                } else {
                    Double ulLongitude = convertToDouble(ulLongitudeField.getText());
                    Double ulLatitude = convertToDouble(ulLatitudeField.getText());
                    Double lrLongitude = convertToDouble(lrLongitudeField.getText());
                    Double lrLatitude = convertToDouble(lrLatitudeField.getText());
                    if (ulLongitude == null || ulLatitude == null || lrLongitude == null || lrLatitude == null) {
                        return;
                    }
                    osmReader.read(ulLongitude.doubleValue(), ulLatitude.doubleValue(), lrLongitude.doubleValue(), lrLatitude.doubleValue(), group, validHighways, tagFilterArea.getText(), rejectCheckBox.isSelected(), zone);
                }
            } catch (Exception e) {
                Itk.logError("Read OSM", e.getMessage());
                Alert alert = new Alert(AlertType.ERROR, e.getMessage() + "\n\nMap data is over on the way.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
            }
        }
    }

    /**
     * マップの回転角と角度ロックを設定する
     */
    private void setRotation() {
        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Set rotation");
        VBox paramPane = new VBox();

        Label label = new Label("Rotation angle (degree)");
        TextField angleField = new TextField("" + canvas.getAngle());
        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(8, 0, 8, 0));
        flowPane.setHgap(8);
        flowPane.getChildren().addAll(label, angleField);

        CheckBox lockingCheckBox = new CheckBox("Angle locking");
        lockingCheckBox.setSelected(canvas.isAngleLocking());

        paramPane.getChildren().addAll(flowPane, lockingCheckBox);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double value = convertToDouble(angleField.getText());
            if (value != null) {
                double angle = value;
                if (angle < -180.0 || angle > 180) {
                    Alert alert = new Alert(AlertType.WARNING, "Angle range is -180.0 to 180.0", ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
                canvas.rotate(canvas.getWidth() / 2.0, canvas.getHeight() / 2.0, angle);
            }
            miResetRotation.setDisable(lockingCheckBox.isSelected());
            canvas.setAngleLocking(lockingCheckBox.isSelected());
        }
    }

    /**
     * グリッドパラメータを設定する
     */
    private void setGrid() {
        if (editor.getCurrentGroup().getScale() != 1.0) {
            Alert alert = new Alert(AlertType.WARNING, "If the group scale is not 1.0, the grid can not be used.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Set grid");

        // 横幅(m)
        Label widthLabel = new Label("Grid width(m)");
        TextField widthField = new TextField("" + canvas.getGridWidth());
        widthField.setMaxWidth(160);

        // 縦幅(m)
        Label heightLabel = new Label("Grid height(m)");
        TextField heightField = new TextField("" + canvas.getGridHeight());
        heightField.setMaxWidth(160);

        // 横オフセット(m)
        Label xOffsetLabel = new Label("X offset(m)");
        TextField xOffsetField = new TextField("" + canvas.getGridOffsetX());
        xOffsetField.setMaxWidth(160);

        // 縦オフセット(m)
        Label yOffsetLabel = new Label("Y offset(m)");
        TextField yOffsetField = new TextField("" + canvas.getGridOffsetY());
        yOffsetField.setMaxWidth(160);

        CheckBox showSizeCheckBox = new CheckBox("Show size");
        showSizeCheckBox.setSelected(canvas.isGridSizeShowing());

        FlowPane flowPane = new FlowPane();
        CheckBox snapCheckBox = new CheckBox("Snap");
        snapCheckBox.setSelected(canvas.isGridSnapping());
        snapCheckBox.setOnAction(e -> flowPane.setDisable(! snapCheckBox.isSelected()));
        TextField pixelField = new TextField("" + canvas.getGridSnapSize());
        pixelField.setMaxWidth(100);
        flowPane.setHgap(8);
        flowPane.getChildren().addAll(pixelField, new Label("pixel"));
        flowPane.setDisable(! snapCheckBox.isSelected());

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(12, 0, 12, 20));
        gridPane.setHgap(12);
        gridPane.setVgap(8);
        gridPane.add(widthLabel, 1, 1);
        gridPane.add(widthField, 2, 1);
        gridPane.add(heightLabel, 1, 2);
        gridPane.add(heightField, 2, 2);
        gridPane.add(xOffsetLabel, 1, 3);
        gridPane.add(xOffsetField, 2, 3);
        gridPane.add(yOffsetLabel, 1, 4);
        gridPane.add(yOffsetField, 2, 4);
        gridPane.add(showSizeCheckBox, 1, 5, 2, 1);
        gridPane.add(snapCheckBox, 1, 6);
        gridPane.add(flowPane, 2, 6);

        dialog.getDialogPane().setContent(gridPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double value = convertToDouble(widthField.getText());
            if (value != null) {
                if (value < 0.1) {
                    Alert alert = new Alert(AlertType.WARNING, "Invalid grid width.", ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
                canvas.setGridWidth(value);
            }
            value = convertToDouble(heightField.getText());
            if (value != null) {
                if (value < 0.1) {
                    Alert alert = new Alert(AlertType.WARNING, "Invalid grid height.", ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
                canvas.setGridHeight(value);
            }
            value = convertToDouble(xOffsetField.getText());
            if (value != null) {
                canvas.setGridOffsetX(value);
            }
            value = convertToDouble(yOffsetField.getText());
            if (value != null) {
                canvas.setGridOffsetY(value);
            }
            Integer intValue = convertToInteger(pixelField.getText());
            if (intValue != null) {
                canvas.setGridSnapSize(intValue);
            }
            canvas.setGridSizeShowing(showSizeCheckBox.isSelected());
            canvas.setGridSnapping(snapCheckBox.isSelected());
            canvas.repaintLater();
        }
    }

    /**
     * フロアの複製ダイアログを開く
     */
    public void openDuplicateFloorDialog() {
        MapPartGroup currentGroup = editor.getCurrentGroup();
        if (currentGroup == editor.getMap().getRoot()) {
            Alert alert = new Alert(AlertType.WARNING, "Root group can not be duplicated.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }
        Matcher match = currentGroup.matchTag("^(B?)(\\d+)F$");
        if (match == null) {
            Alert alert = new Alert(AlertType.WARNING, "No floor number given for this group.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Duplicate floor");
        VBox paramPane = new VBox();

        Label label = new Label("Duplicate " + currentGroup.getTagString());
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        Label directionLabel = new Label("Direction");
        RadioButton upButton = new RadioButton("Up");
        RadioButton downButton = new RadioButton("Down");
        ToggleGroup toggleGroup = new ToggleGroup();
        upButton.setToggleGroup(toggleGroup);
        downButton.setToggleGroup(toggleGroup);
        upButton.setSelected(true);
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(8);
        flowPane.getChildren().addAll(upButton, downButton);

        Label numberLabel = new Label("Number of floors");
        Spinner<Integer> numberSpinner = new Spinner<>(1, 100, 1);

        Label heightDiffLabel = new Label("Height difference");
        TextField heightDiffField = new TextField("" + 0.0);
        heightDiffField.setMaxWidth(150);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(directionLabel, 1, 1);
        grid.add(flowPane, 2, 1);
        grid.add(numberLabel, 1, 2);
        grid.add(numberSpinner, 2, 2);
        grid.add(heightDiffLabel, 1, 3);
        grid.add(heightDiffField, 2, 3);

        paramPane.getChildren().addAll(label, grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double value = convertToDouble(heightDiffField.getText());
            if (value != null) {
                double heightDiff = value;
                if (heightDiff < 1.0) {
                    Alert alert = new Alert(AlertType.WARNING, "Invalid height difference.", ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
                int floor = Integer.parseInt(match.group(2));
                if (! match.group(1).isEmpty()) {
                    floor = -floor;
                }
                try {
                    editor.duplicateFloor(currentGroup, upButton.isSelected() ? 1 : -1, numberSpinner.getValue(), heightDiff);
                } catch(Exception e) {
                    Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                }
            }
        }
    }

    /**
     * Calculate tag paths ダイアログを開く
     */
    public void openCalculateTagPathsDialog() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.initOwner(frame);
        dialog.setTitle("Calculate tag paths");
        dialog.setHeaderText("Enter a goal tag");
        String tag = dialog.showAndWait().orElse("").trim();
        if (! tag.isEmpty()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Tags are added to all nodes that can reach the goal.\n    Do you want to continue?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                if (editor.calcTagPaths(tag)) {
                    Alert _alert = new Alert(AlertType.NONE, "Calculation of paths finished.", ButtonType.OK);
                    _alert.initOwner(frame);
                    _alert.showAndWait();
                } else {
                    Alert _alert = new Alert(AlertType.WARNING, "No goal with tag " + tag, ButtonType.OK);
                    _alert.initOwner(frame);
                    _alert.showAndWait();
                }
            }
        }
    }

    /**
     * 詳細表示機能付きのアラートダイアログを表示する
     */
    public Optional<ButtonType> alert(AlertType alertType, String title, String headerText, String contentText, String expandableContentText, ButtonType... buttonTypes) {
        Alert alert = new Alert(alertType);
        alert.initOwner(frame);
        if (buttonTypes.length > 0) {
            alert.getButtonTypes().clear();
            for (ButtonType buttonType : buttonTypes) {
                alert.getButtonTypes().add(buttonType);
            }
        }
        if (title != null && ! title.isEmpty()) {
            alert.setTitle(title);
        }
        if (headerText != null && ! headerText.isEmpty()) {
            alert.setHeaderText(headerText);
        }
        if (contentText != null && ! contentText.isEmpty()) {
            alert.setContentText(contentText + (expandableContentText != null && ! expandableContentText.isEmpty() ? "\n\n" : ""));
        }
        if (expandableContentText != null && ! expandableContentText.isEmpty()) {
            TextArea textArea = new TextArea(expandableContentText);
            textArea.setEditable(false);
            alert.getDialogPane().setExpandableContent(textArea);
        }
        return alert.showAndWait();
    }

    /**
     * ノード座標の重複をチェックする
     */
    public void checkForPiledNodes() {
        String title = "Check for node in same position";
        for (MapNode node : editor.getMap().getNodes()) {
            if (node.selected) {
                alert(AlertType.WARNING, title, "Cancel all node selections before executing.", null, null, ButtonType.OK);
                return;
            }
            if (! nodePanel.getFilteredSet().contains(node)) {
                alert(AlertType.WARNING, title, "Cancel node filter before executing.", null, null, ButtonType.OK);
                return;
            }
        }

        ArrayList<MapNode> piledNodes = editor.getPiledNodes();
        if (piledNodes.isEmpty()) {
            alert(AlertType.INFORMATION, title, "No nodes with the same position.", null, null, ButtonType.OK);
            return;
        }

        StringBuilder buff = new StringBuilder();
        for (MapNode node : piledNodes) {
            Itk.logWarn_("Piled node found", node.toShortInfo());
            buff.append(String.format("(%s, %s, %s) ", node.getX(), node.getY(), node.getHeight()));
            buff.append(node.toShortInfo());
            buff.append("\n");
        }
        Optional<ButtonType> result = alert(AlertType.WARNING, title, "" + piledNodes.size() + " piled nodes found.", "Resolve them?", buff.toString(), ButtonType.YES, ButtonType.NO);
        if (result.isPresent() && result.get() == ButtonType.YES) {
            nodePanel.select(piledNodes);
        }
    }

    /**
     * ノードが null のリンクがないかをチェックする
     */
    public void checkLinksWhereNodeDoesNotExist() {
        if (editor.getMap().checkConsistency()) {
            alert(AlertType.INFORMATION, "Check links where node does not exist", "There is no problem.", null, null, ButtonType.OK);
        } else {
            alert(AlertType.WARNING, "Check links where node does not exist", "There are links where the node does not exist.\nLook at the command line message.", null, null, ButtonType.OK);
        }
    }

    /**
     * ループしたリンクがないかをチェックする
     */
    public void checkLoopedLinks() {
        String title = "Check for looped link";
        for (MapLink link : editor.getMap().getLinks()) {
            if (link.selected) {
                alert(AlertType.WARNING, title, "Cancel all link selections before executing.", null, null, ButtonType.OK);
                return;
            }
            if (! linkPanel.getFilteredSet().contains(link)) {
                alert(AlertType.WARNING, title, "Cancel link filter before executing.", null, null, ButtonType.OK);
                return;
            }
        }

        ArrayList<MapLink> loopedLinks = editor.getLoopedLinks();
        if (loopedLinks.isEmpty()) {
            alert(AlertType.INFORMATION, title, "Looped link does not exist.", null, null, ButtonType.OK);
            return;
        }

        StringBuilder buff = new StringBuilder();
        for (MapLink link : loopedLinks) {
            Itk.logWarn_("Looped link found", link.toShortInfo());
            buff.append(link.toShortInfo());
            buff.append("\n");
        }
        Optional<ButtonType> result = alert(AlertType.WARNING, title, "" + loopedLinks.size() + " looped links found.", "Resolve them?", buff.toString(), ButtonType.YES, ButtonType.NO);
        if (result.isPresent() && result.get() == ButtonType.YES) {
            linkPanel.select(loopedLinks);
        }
    }

    /**
     * 長さ 0 (以下)のリンクがないかをチェックする
     */
    public void check0LengthLinks() {
        String title = "Check for zero length link";
        for (MapLink link : editor.getMap().getLinks()) {
            if (link.selected) {
                alert(AlertType.WARNING, title, "Cancel all link selections before executing.", null, null, ButtonType.OK);
                return;
            }
            if (! linkPanel.getFilteredSet().contains(link)) {
                alert(AlertType.WARNING, title, "Cancel link filter before executing.", null, null, ButtonType.OK);
                return;
            }
        }

        ArrayList<MapLink> zeroLengthLinks = editor.get0LengthLinks();
        if (zeroLengthLinks.isEmpty()) {
            alert(AlertType.INFORMATION, title, "Zero length link does not exist.", null, null, ButtonType.OK);
            return;
        }

        StringBuilder buff = new StringBuilder();
        for (MapLink link : zeroLengthLinks) {
            Itk.logWarn_("Zero length link found", link.toShortInfo());
            buff.append(link.toShortInfo());
            buff.append("\n");
        }
        Optional<ButtonType> result = alert(AlertType.WARNING, title, "" + zeroLengthLinks.size() + " zero length links found.", "Resolve them?", buff.toString(), ButtonType.YES, ButtonType.NO);
        if (result.isPresent() && result.get() == ButtonType.YES) {
            linkPanel.select(zeroLengthLinks);
        }
    }

    /**
     * 重複したリンクがないかをチェックする
     */
    public void checkDuplicateLinks() {
        String title = "Check for duplicate link";
        for (MapLink link : editor.getMap().getLinks()) {
            if (link.selected) {
                alert(AlertType.WARNING, title, "Cancel all link selections before executing.", null, null, ButtonType.OK);
                return;
            }
            if (! linkPanel.getFilteredSet().contains(link)) {
                alert(AlertType.WARNING, title, "Cancel link filter before executing.", null, null, ButtonType.OK);
                return;
            }
        }

        HashMap<String, ArrayList<MapLink>> duplicateLinks = editor.getDuplicateLinks();
        if (duplicateLinks.isEmpty()) {
            alert(AlertType.INFORMATION, title, "Duplicate link does not exist.", null, null, ButtonType.OK);
            return;
        }

        ArrayList<MapLink> links = new ArrayList();
        int n = 1;
        StringBuilder buff = new StringBuilder();
        for (String key : duplicateLinks.keySet()) {
            for (MapLink link : duplicateLinks.get(key)) {
                Itk.logWarn_("Duplicate link found at place #" + n, link.toShortInfo());
                links.add(link);
                buff.append(String.format("Place #%d ", n));
                buff.append(link.toShortInfo());
                buff.append("\n");
            }
            n++;
        }
        Optional<ButtonType> result = alert(AlertType.WARNING, title, "Duplicate links found at " + duplicateLinks.size() + " places.", "Resolve them?", buff.toString(), ButtonType.YES, ButtonType.NO);
        if (result.isPresent() && result.get() == ButtonType.YES) {
            linkPanel.select(links);
        }
    }

    /**
     * 2D カメラワークデータのバージョンをチェックする
     */
    public boolean checkCameraworkVersion(String filePath) {
        File file = new File(filePath);
        if (! file.exists()) {
            Alert alert = new Alert(AlertType.WARNING, "Camerawork file does not exist: " + filePath, ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return false;
        }

        Itk.logInfo("Load camerawork file", filePath);
        try {
            JSON json = new JSON(JSON.Mode.TRADITIONAL);
            ArrayList<Map<String, Object>> jsonObject = json.parse(new FileReader(filePath));
            for (Map<String, Object> object : jsonObject) {
                if (object.get("angle") == null) {
                    Alert alert = new Alert(AlertType.WARNING, "Camerawork file format is old: " + filePath, ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return false;
                }
            }
        } catch (IOException e) {
            Itk.quitWithStackTrace(e);
        }
        return true;
    }

    /**
     * マップデータの総合的な検証
     */
    public boolean totalValidation(boolean alwaysDisplayDialog, String confirmingMessage) {
        StringBuilder contentBuff = new StringBuilder();
        StringBuilder buff = new StringBuilder();

        // ノード座標の重複チェック
        ArrayList<MapNode> piledNodes = editor.getPiledNodes();
        if (! piledNodes.isEmpty()) {
            contentBuff.append(piledNodes.size());
            contentBuff.append(" piled nodes found.");

            buff.append(piledNodes.size());
            buff.append(" piled nodes found:\n");
            for (MapNode node : piledNodes) {
                Itk.logWarn_("Piled node found", node.toShortInfo());
                buff.append(String.format("(%s, %s, %s) ", node.getX(), node.getY(), node.getHeight()));
                buff.append(node.toShortInfo());
                buff.append("\n");
            }
            buff.append("\n");
        }

        // ノードが null のリンクがないかチェック
        if (! editor.getMap().checkConsistency()) {
            if (contentBuff.length() > 0) {
                contentBuff.append("\n");
            }
            contentBuff.append("There are links where the node does not exist.");
        }

        // ループしたリンクのチェック
        ArrayList<MapLink> loopedLinks = editor.getLoopedLinks();
        if (! loopedLinks.isEmpty()) {
            if (contentBuff.length() > 0) {
                contentBuff.append("\n");
            }
            contentBuff.append(loopedLinks.size());
            contentBuff.append(" looped links found.");

            buff.append(loopedLinks.size());
            buff.append(" looped links found:\n");
            for (MapLink link : loopedLinks) {
                Itk.logWarn_("Looped link found", link.toShortInfo());
                buff.append(link.toShortInfo());
                buff.append("\n");
            }
            buff.append("\n");
        }

        // 長さ 0 (以下)のリンクのチェック
        ArrayList<MapLink> zeroLengthLinks = editor.get0LengthLinks();
        if (! zeroLengthLinks.isEmpty()) {
            if (contentBuff.length() > 0) {
                contentBuff.append("\n");
            }
            contentBuff.append(zeroLengthLinks.size());
            contentBuff.append(" zero length links found.");

            buff.append(zeroLengthLinks.size());
            buff.append(" zero length links found:\n");
            for (MapLink link : zeroLengthLinks) {
                Itk.logWarn_("Zero length link found", link.toShortInfo());
                buff.append(link.toShortInfo());
                buff.append("\n");
            }
            buff.append("\n");
        }

        // 重複したリンクのチェック
        HashMap<String, ArrayList<MapLink>> duplicateLinks = editor.getDuplicateLinks();
        if (! duplicateLinks.isEmpty()) {
            if (contentBuff.length() > 0) {
                contentBuff.append("\n");
            }
            contentBuff.append("Duplicate links found at ");
            contentBuff.append(duplicateLinks.size());
            contentBuff.append(" places.");

            buff.append("Duplicate links found at ");
            buff.append(duplicateLinks.size());
            buff.append(" places:\n");
            int n = 1;
            for (String key : duplicateLinks.keySet()) {
                for (MapLink link : duplicateLinks.get(key)) {
                    Itk.logWarn_("Duplicate link found at place #" + n, link.toShortInfo());
                    buff.append(String.format("Place #%d ", n));
                    buff.append(link.toShortInfo());
                    buff.append("\n");
                }
                n++;
            }
        }

        // "OCEAN" および "STRUCTURE" タグ使用のチェック
        int oceanTagCount = 0;
        int structureTagCount = 0;
        for (MapLink link : editor.getMap().getLinks()) {
            for (String tag : link.getTags()) {
                if (tag.contains("OCEAN")) {
                    oceanTagCount++;
                } else if (tag.contains("STRUCTURE")) {
                    structureTagCount++;
                }
            }
        }
        if (oceanTagCount > 0) {
            if (contentBuff.length() > 0) {
                contentBuff.append("\n");
            }
            contentBuff.append("Tags including \"OCEAN\" is used. This tag is deprecated.");
            Itk.logWarn_("Tags including \"OCEAN\" is used. This tag is deprecated.");
        }
        if (structureTagCount > 0) {
            if (contentBuff.length() > 0) {
                contentBuff.append("\n");
            }
            contentBuff.append("Tags including \"STRUCTURE\" is used. This tag is deprecated.");
            Itk.logWarn_("Tags including \"STRUCTURE\" is used. This tag is deprecated.");
        }

        String title = "Total validation of map data";
        if (contentBuff.length() == 0) {
            if (alwaysDisplayDialog) {
                alert(AlertType.INFORMATION, title, "There is no problem.", null, null, ButtonType.OK);
            }
            // 何も問題がなければ true
            return true;
        }

        if (confirmingMessage == null || confirmingMessage.isEmpty()) {
            alert(AlertType.WARNING, title, "There are some problems with map data.", contentBuff.toString(), buff.toString(), ButtonType.OK);
            // 問題があり confirmingMessage が指定されていなければ false
            return false;
        }
        Optional<ButtonType> result = alert(AlertType.WARNING, title, "There are some problems with map data.", contentBuff.toString() + "\n\n" + confirmingMessage, buff.toString(), ButtonType.YES, ButtonType.NO);
        // 問題があり confirmingMessage が指定されていた場合は YES ならば true、NO ならば false
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    /**
     * ターゲットに到達できないリンクがないかチェックする
     */
    public void checkForReachability() {
        NetworkMap networkMap = editor.getMap();
        for (MapLink link : networkMap.getLinks()) {
            if (link.selected) {
                Alert alert = new Alert(AlertType.WARNING, "Cancel all link selections before executing.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
            if (! linkPanel.getFilteredSet().contains(link)) {
                Alert alert = new Alert(AlertType.WARNING, "Cancel link filter before executing.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
        }

        TextInputDialog dialog = new TextInputDialog("");
        dialog.initOwner(frame);
        dialog.setTitle("Check reachability");
        dialog.setHeaderText("Enter a target tag");
        String tag = dialog.showAndWait().orElse("").trim();
        if (tag.isEmpty()) {
            return;
        }

        MapLinkTable reachableLinks = getReachableLinks(networkMap.getNodes(), tag);
        int notConnectedCount = networkMap.getLinks().size() - reachableLinks.size();
        if (notConnectedCount > 0) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "There were " + notConnectedCount + " links not leading to target!\nShould select REACHABLE links?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                linkPanel.select(reachableLinks);
            }
        } else {
            Alert alert = new Alert(AlertType.INFORMATION, "Calculation of paths finished.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
        }
    }

    /**
     * TODO: 正常に機能しないためデバッグが必要
     */
    public MapLinkTable getReachableLinks(MapNodeTable nodes, String targetTag) {
        final MapNodeTable exits = new MapNodeTable();
        for (final MapNode node : nodes) {
            if (node.hasTag(targetTag)) exits.add(node);
        }

        Stack<MapNode> path = new Stack<MapNode>();
        Stack<Integer> selection = new Stack<Integer>();
        MapLinkTable reachableLinks = new MapLinkTable();

        for (MapNode exit : exits) {
            path.push(exit);
            selection.push(0);

            while(!path.empty()) {
                MapNode node = path.peek();
                Integer index = selection.pop();

                final MapLinkTable linkList  = node.getUsableLinkTable();
                if (index == linkList.size()) {
                    path.pop();
                    continue;
                }

                final MapLink link = linkList.get(index);
                ++index;
                selection.push(index);

                if (!reachableLinks.contains(link)) {
                    reachableLinks.add(link);
                    path.push(link.getOther(node));
                    selection.push(0);
                }
            }
        }
        return reachableLinks;
    }

    /**
     * 選択オブジェクトが複数グループにまたがっている場合の継続確認
     */
    public boolean multipleGroupConfirmation(ArrayList<? extends OBNode> obNodes, String message) {
        if (! editor.isSingleGroup(obNodes)) {
            Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.YES;
        }
        return true;
    }

    /**
     * ノード設定ダイアログを開く
     */
    public void openNodeAttributesDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(nodes, "Warning:\n    Nodes of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        double averageHeight = 0.0;
        for (MapNode node : nodes) {
            averageHeight += node.getHeight();
        }
        averageHeight /= nodes.size();

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Set node attributes");
        VBox paramPane = new VBox();

        Label label = null;
        if (nodes.size() == 1) {
            label = new Label("ID: " + nodes.get(0).ID);
        } else {
            label = new Label("" + nodes.size() + " nodes selected");
        }
        label.setPadding(new Insets(0, 0, 12, 0));
        paramPane.getChildren().addAll(label);

        label = new Label("Parameters");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 0, 0, 10));
        grid.setHgap(8);
        grid.setVgap(8);
        int row = 1;

        if (nodes.size() == 1) {
            MapNode node = nodes.get(0);

            // X
            Label xLabel = new Label("X");
            TextField xField = new TextField("" + node.getX());
            xField.setPrefWidth(176);
            Button xButton = new Button("Set");
            EventHandler xHandler = new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    Double value = convertToDouble(xField.getText());
                    if (value != null) {
                        double x = value;
                        if (x != node.getX()) {
                            editor.invokeSingleCommand(new SetCoordinates(node, x, node.getY()));
                        }
                        dialog.close();
                    }
                }
            };
            xField.setOnAction(xHandler);
            xButton.setOnAction(xHandler);
            grid.add(xLabel, 1, row);
            grid.add(xField, 2, row);
            grid.add(xButton, 3, row);
            row++;

            // Y
            Label yLabel = new Label("Y");
            TextField yField = new TextField("" + node.getY());
            yField.setPrefWidth(176);
            Button yButton = new Button("Set");
            EventHandler yHandler = new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    Double value = convertToDouble(yField.getText());
                    if (value != null) {
                        double y = value;
                        if (y != node.getY()) {
                            editor.invokeSingleCommand(new SetCoordinates(node, node.getX(), y));
                        }
                        dialog.close();
                    }
                }
            };
            yField.setOnAction(yHandler);
            yButton.setOnAction(yHandler);
            grid.add(yLabel, 1, row);
            grid.add(yField, 2, row);
            grid.add(yButton, 3, row);
            row++;
        }

        // height field
        Label itemInfo = new Label("height(" + averageHeight + ")");
        TextField textField = new TextField("" + averageHeight);
        textField.setMinWidth(100);
        Button button = new Button("Set");
        EventHandler heightHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(textField.getText());
                if (value != null) {
                    double height = value;
                    editor.startOfCommandBlock();
                    for (MapNode node : nodes) {
                        if (! editor.invoke(new SetHeight(node, height))) {
                            break;
                        }
                    }
                    editor.endOfCommandBlock();
                    editor.updateHeight();
                    dialog.close();
                }
            }
        };
        textField.setOnAction(heightHandler);
        button.setOnAction(heightHandler);
        grid.add(itemInfo, 1, row);
        grid.add(textField, 2, row);
        grid.add(button, 3, row);

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        TagSetupPane pane = new TagSetupPane(editor, nodes, dialog);
        paramPane.getChildren().addAll(label, grid, separator, pane);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * ノードの移動またはコピーダイアログを開く
     */
    public void copyOrMoveNodes(double x, double y, double z) {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.isEmpty()) {
            canvas.repaintLater();
            return;
        }
        if (! multipleGroupConfirmation(nodes, "Warning:\n    Nodes of multiple groups were selected.\n    Do you want to continue?")) {
            canvas.repaintLater();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Copy or move nodes");
        VBox paramPane = new VBox();

        if (nodes.size() > 1) {
            Label label = new Label("" + nodes.size() + " nodes selected");
            label.setPadding(new Insets(4, 0, 12, 0));
            paramPane.getChildren().addAll(label);
        }

        Label xyzLabel = new Label("Moving distance");
        xyzLabel.setFont(Font.font("Arial", FontWeight.BOLD, xyzLabel.getFont().getSize()));
        xyzLabel.setPadding(new Insets(0, 0, 8, 0));

        // X
        Label xLabel = new Label("X (m)");
        TextField xField = new TextField("" + x);
        xField.setMinWidth(176);

        // Y
        Label yLabel = new Label("Y (m)");
        TextField yField = new TextField("" + y);
        yField.setMinWidth(176);

        // Z
        Label zLabel = new Label("Z (m)");
        TextField zField = new TextField("" + z);
        zField.setMinWidth(176);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 0, 8, 10));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(xLabel, 1, 1);
        grid.add(xField, 2, 1);
        grid.add(yLabel, 1, 2);
        grid.add(yField, 2, 2);
        grid.add(zLabel, 1, 3);
        grid.add(zField, 2, 3);

        Label label = new Label("Copy attributes (No effect on the Move)");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(8, 0, 4, 0));

        Label groupLabel = new Label("destination group");
        HashMap<String, MapPartGroup> groups = new HashMap();
        ArrayList<String> groupNames = new ArrayList();
        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == editor.getMap().getRoot() || group.getTags().size() == 0) {
                continue;
            }
            groups.put(group.getTagString(), group);
            groupNames.add(group.getTagString());
        }
        ChoiceBox groupChoiceBox = new ChoiceBox(FXCollections.observableArrayList(groupNames));
        groupChoiceBox.setValue(editor.getCurrentGroup().getTagString());
        HBox destGroupPane = new HBox(8);
        destGroupPane.setAlignment(Pos.CENTER_LEFT);
        destGroupPane.setPadding(new Insets(0, 0, 0, 8));
        destGroupPane.getChildren().addAll(groupLabel, groupChoiceBox);

        CheckBox wlCheckBox = new CheckBox("without links");
        wlCheckBox.setPadding(new Insets(10, 0, 0, 8));

        CheckBox withNodeTagsCheckBox = new CheckBox("copy with node tags");
        withNodeTagsCheckBox.setPadding(new Insets(10, 0, 0, 8));

        CheckBox withLinkTagsCheckBox = new CheckBox("copy with link tags");
        withLinkTagsCheckBox.setPadding(new Insets(0, 0, 12, 8));

        paramPane.getChildren().addAll(xyzLabel, grid, label, destGroupPane, wlCheckBox, withNodeTagsCheckBox, withLinkTagsCheckBox);
        dialog.getDialogPane().setContent(paramPane);

        ButtonType buttonTypeCopy = new ButtonType("Copy");
        ButtonType buttonTypeMove = new ButtonType("Move");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeCopy, buttonTypeMove, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() != ButtonType.CANCEL) {
            Double _x = convertToDouble(xField.getText());
            Double _y = convertToDouble(yField.getText());
            Double _z = convertToDouble(zField.getText());
            if (_x != null) {
                x = _x;
            }
            if (_y != null) {
                y = _y;
            }
            if (_z != null) {
                z = _z;
            }
            Point2D distance = canvas.convertToOriginal(x, y);
            x = distance.getX();
            y = distance.getY();
            MapPartGroup toGroup = groups.get(groupChoiceBox.getValue());
            ArrayList<MapNode> collisionNodes = editor.getCollisionNodes(nodes, x, y, z, toGroup);
            if (! collisionNodes.isEmpty()) {
                Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    " + collisionNodes.size() + " nodes are place on nodes already existing.\n    Do you want to continue?", ButtonType.YES, ButtonType.NO);
                alert.initOwner(frame);
                Optional<ButtonType> _result = alert.showAndWait();
                if (! _result.isPresent() || _result.get() != ButtonType.YES) {
                    canvas.repaintLater();
                    return;
                }
            }

            if (result.get() == buttonTypeCopy) {
                editor.duplicateAndMoveNodes(nodes, x, y, z, toGroup, wlCheckBox.isSelected(), withNodeTagsCheckBox.isSelected(), withLinkTagsCheckBox.isSelected());
            } else if (result.get() == buttonTypeMove) {
                editor.moveNodes(nodes, x, y, z);
            }
        }
        canvas.repaintLater();
    }

    /**
     * 階段を作成するダイアログを開く.
     *
     * ノードを二つ指定して、標高が高いノード側のグループにリンクを作る
     */
    public void openMakeStairsDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.size() != 2) {
            Alert alert = new Alert(AlertType.WARNING, "Select only 2 nodes.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }
        if (nodes.get(0).getParent() == nodes.get(1).getParent()) {
            Alert alert = new Alert(AlertType.WARNING, "Can not make stairs in the same group.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        int fromIndex = 0;
        int toIndex = 1;
        if (nodes.get(0).getHeight() < nodes.get(1).getHeight()) {
            fromIndex = 1;
            toIndex = 0;
        }
        final MapNode fromNode = nodes.get(fromIndex);
        final MapNode toNode = nodes.get(toIndex);

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Make stairs");
        VBox paramPane = new VBox();

        Label label = new Label("Make stairs between " + ((MapPartGroup)fromNode.getParent()).getTagString() + " and " + ((MapPartGroup)toNode.getParent()).getTagString());
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 12, 0));

        Label lengthLabel = new Label("Length");
        lengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, lengthLabel.getFont().getSize()));
        TextField lengthField = new TextField("" + 1.0);
        lengthField.setPrefWidth(160);

        Button calcLengthButton = new Button("Calc length");
        CheckBox heightDiffCheckBox = new CheckBox("Reflect the height difference");
        calcLengthButton.setOnAction(e -> {
            double distance = fromNode.getPosition().distance(toNode.getPosition()) * editor.getCurrentGroup().getScale();
            if (heightDiffCheckBox.isSelected()) {
                Point3D point0 = new Point3D(fromNode.getX(), fromNode.getY(), fromNode.getHeight());
                Point3D point1 = new Point3D(toNode.getX(), toNode.getY(), toNode.getHeight());
                distance = point0.distance(point1) * editor.getCurrentGroup().getScale();
            }
            lengthField.setText("" + distance);
        });

        Label widthLabel = new Label("Width");
        widthLabel.setFont(Font.font("Arial", FontWeight.BOLD, widthLabel.getFont().getSize()));
        TextField widthField = new TextField("" + 1.0);
        widthField.setPrefWidth(160);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(lengthLabel, 1, 1);
        grid.add(lengthField, 2, 1);
        grid.add(calcLengthButton, 3, 1);
        grid.add(heightDiffCheckBox, 4, 1);
        grid.add(widthLabel, 1, 2);
        grid.add(widthField, 2, 2);

        paramPane.getChildren().addAll(label, grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double length = convertToDouble(lengthField.getText());
            Double width = convertToDouble(widthField.getText());
            if (length != null && width != null) {
                if (length <= 0.0 || width <= 0.0) {
                    Alert alert = new Alert(AlertType.WARNING, "Invalid value.", ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
                editor.startOfCommandBlock();
                if (editor.invoke(new AddLink(fromNode, toNode, length, width))) {
                    MapLink link = fromNode.connectedTo(toNode);
                    editor.invoke(new AddTag(link, "GENERATED_STAIR"));
                }
                editor.endOfCommandBlock();
            }
        }
    }

    /**
     * 複数ノードの拡大縮小と回転ダイアログを開く
     */
    public void openRotateAndScaleNodesDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.size() < 2) {
            return;
        }
        if (! editor.isSingleGroup(nodes)) {
            Alert alert = new Alert(AlertType.WARNING, "Nodes of multiple groups were selected.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Rotate and Scale");
        dialog.getDialogPane().setPrefWidth(360);
        VBox paramPane = new VBox();

        if (nodes.size() > 1) {
            Label label = new Label("" + nodes.size() + " nodes selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().addAll(label);
        }

        Label scaleXLabel = new Label("Scale X");
        scaleXLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleXLabel.getFont().getSize()));
        TextField scaleXField = new TextField("" + 1.0);
        scaleXField.setPrefWidth(100);

        Label scaleYLabel = new Label("Scale Y");
        scaleYLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleYLabel.getFont().getSize()));
        TextField scaleYField = new TextField("" + 1.0);
        scaleYField.setPrefWidth(100);

        Label angleLabel = new Label("Rotation angle (0.0 .. 360.0)");
        angleLabel.setFont(Font.font("Arial", FontWeight.BOLD, angleLabel.getFont().getSize()));
        TextField angleField = new TextField("" + 0.0);
        angleField.setPrefWidth(100);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(scaleXLabel, 1, 1);
        grid.add(scaleXField, 2, 1);
        grid.add(scaleYLabel, 1, 2);
        grid.add(scaleYField, 2, 2);
        grid.add(angleLabel, 1, 3);
        grid.add(angleField, 2, 3);

        paramPane.getChildren().add(grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double _scaleX = convertToDouble(scaleXField.getText());
            Double _scaleY = convertToDouble(scaleYField.getText());
            Double _angle = convertToDouble(angleField.getText());
            double scaleX = 1.0;
            double scaleY = 1.0;
            double angle = 0.0;
            if (_scaleX != null) {
                scaleX = _scaleX;
            }
            if (_scaleY != null) {
                scaleY = _scaleY;
            }
            if (_angle != null) {
                angle = _angle;
            }

            StringBuilder buff = new StringBuilder();
            if (scaleX <= 0.0) {
                buff.append("Scale X : " + scaleX);
                buff.append("\n");
            }
            if (scaleY <= 0.0) {
                buff.append("Scale Y : " + scaleY);
                buff.append("\n");
            }
            if (angle < 0.0 || angle > 360.0) {
                buff.append("Rotation angle : " + angle);
                buff.append("\n");
            }
            if (buff.length() > 0) {
                alertInvalidInputValue(buff.toString());
                return;
            }

            if (scaleX != 1.0 || scaleY != 1.0 || (angle > 0.0 && angle < 360.0)) {
                editor.rotateAndScaleNodes(nodes, scaleX, scaleY, angle);
            }
        }
    }

    /**
     * マップ座標の正規化
     */
    public void openNormalizeCoordinatesDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.size() != 2) {
            Alert alert = new Alert(AlertType.WARNING, "Select only 2 nodes.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }
        MapNode node1 = nodes.get(0);
        MapNode node2 = nodes.get(1);

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Normalize coordinates");
        dialog.getDialogPane().setPrefWidth(360);
        VBox paramPane = new VBox();
        paramPane.setPadding(new Insets(16, 24, 12, 24));
        paramPane.setSpacing(8);

        ArrayList<String> zones = new ArrayList();
        for (int zone = 1; zone <= GsiTile.JGD2000_JPR_EPSG_NAMES.length - 1; zone++) {
            zones.add("" + zone);
        }
        ChoiceBox<String> zoneChoiceBox = new ChoiceBox(FXCollections.observableArrayList(zones));
        zoneChoiceBox.setValue(zones.get(0));

        Label node1Label = new Label("Node 1");
        node1Label.setFont(Font.font("Arial", FontWeight.BOLD, node1Label.getFont().getSize()));

        TextField node1LongitudeField = new TextField("");
        node1LongitudeField.setPrefWidth(200);

        TextField node1LatitudeField = new TextField("");
        node1LatitudeField.setPrefWidth(200);

        Label node2Label = new Label("Node 2");
        node2Label.setFont(Font.font("Arial", FontWeight.BOLD, node2Label.getFont().getSize()));

        TextField node2LongitudeField = new TextField("");
        node2LongitudeField.setPrefWidth(200);

        TextField node2LatitudeField = new TextField("");
        node2LatitudeField.setPrefWidth(200);

        RadioButton separatelyButton = new RadioButton("separately");
        RadioButton averageButton = new RadioButton("average");
        RadioButton scaleXButton = new RadioButton("X");
        RadioButton scaleYButton = new RadioButton("Y");
        ToggleGroup toggleGroup = new ToggleGroup();
        separatelyButton.setToggleGroup(toggleGroup);
        averageButton.setToggleGroup(toggleGroup);
        scaleXButton.setToggleGroup(toggleGroup);
        scaleYButton.setToggleGroup(toggleGroup);
        separatelyButton.setSelected(true);
        FlowPane scalePane = new FlowPane();
        scalePane.setHgap(8);
        scalePane.getChildren().addAll(new Label("Scale"), separatelyButton, averageButton, scaleXButton, scaleYButton);

        CheckBox rotaionCheckBox = new CheckBox("Rotation");
        rotaionCheckBox.setSelected(true);

        CheckBox calcLengthCheckBox = new CheckBox("Recalc length");
        CheckBox reflectHeightCheckBox = new CheckBox("Reflect height");
        reflectHeightCheckBox.setDisable(true);
        calcLengthCheckBox.setOnAction(e -> reflectHeightCheckBox.setDisable(! calcLengthCheckBox.isSelected()));
        FlowPane calcLengthPane = new FlowPane();
        calcLengthPane.setHgap(8);
        calcLengthPane.getChildren().addAll(calcLengthCheckBox, reflectHeightCheckBox);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.add(new Label("Zone"), 0, 0, 2, 1);
        grid.add(zoneChoiceBox, 2, 0);

        grid.add(new Separator(), 0, 1, 3, 1);

        grid.add(node1Label, 0, 2, 3, 1);
        grid.add(new Label("ID"), 1, 3);
        grid.add(new Label(node1.getID()), 2, 3);
        grid.add(new Label("Tags"), 1, 4);
        grid.add(new Label(node1.getTagString()), 2, 4);
        grid.add(new Label("X"), 1, 5);
        grid.add(new Label("" + node1.getX()), 2, 5);
        grid.add(new Label("Y"), 1, 6);
        grid.add(new Label("" + node1.getY()), 2, 6);
        grid.add(new Label("Longitude"), 1, 7);
        grid.add(node1LongitudeField, 2, 7);
        grid.add(new Label("Latitude"), 1, 8);
        grid.add(node1LatitudeField, 2, 8);

        grid.add(new Separator(), 0, 9, 3, 1);

        grid.add(node2Label, 0, 10, 3, 1);
        grid.add(new Label("ID"), 1, 11);
        grid.add(new Label(node2.getID()), 2, 11);
        grid.add(new Label("Tags"), 1, 12);
        grid.add(new Label(node2.getTagString()), 2, 12);
        grid.add(new Label("X"), 1, 13);
        grid.add(new Label("" + node2.getX()), 2, 13);
        grid.add(new Label("Y"), 1, 14);
        grid.add(new Label("" + node2.getY()), 2, 14);
        grid.add(new Label("Longitude"), 1, 15);
        grid.add(node2LongitudeField, 2, 15);
        grid.add(new Label("Latitude"), 1, 16);
        grid.add(node2LatitudeField, 2, 16);

        grid.add(new Separator(), 0, 17, 3, 1);

        paramPane.getChildren().addAll(grid, scalePane, rotaionCheckBox, calcLengthPane);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double node1Longitude = convertToDouble(node1LongitudeField.getText());
            Double node1Latitude = convertToDouble(node1LatitudeField.getText());
            Double node2Longitude = convertToDouble(node2LongitudeField.getText());
            Double node2Latitude = convertToDouble(node2LatitudeField.getText());
            if (node1Longitude != null && node1Latitude != null && node2Longitude != null && node2Latitude != null) {
                int zone = Integer.parseInt((String)zoneChoiceBox.getValue());
                RadioButton scaleButton = (RadioButton)toggleGroup.getSelectedToggle();
                editor.normalizeCoordinates(node1, node2, zone, node1Longitude.doubleValue(), node1Latitude.doubleValue(), node2Longitude.doubleValue(), node2Latitude.doubleValue(), scaleButton.getText(), rotaionCheckBox.isSelected(), calcLengthCheckBox.isSelected(), reflectHeightCheckBox.isSelected());
            }
        }
    }

    /**
     * ノードのシンボリックリンクを追加する
     */
    public void addSymbolicLinkOfNode(MapPartGroup group) {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        MapNodeTable groupNodes = group.getChildNodes();
        for (MapNode node : nodes) {
            if (groupNodes.contains(node)) {
                Alert alert = new Alert(AlertType.WARNING, "Can not symbolic link to own group.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
        }
        editor.addSymbolicLink(nodes, group);
    }

    /**
     * リンクのシンボリックリンクを追加する
     */
    public void addSymbolicLinkOfLink(MapPartGroup group) {
        MapLinkTable links = editor.getSelectedLinks();
        if (links.isEmpty()) {
            return;
        }
        MapLinkTable groupLinks = group.getChildLinks();
        for (MapLink link : links) {
            if (groupLinks.contains(link)) {
                Alert alert = new Alert(AlertType.WARNING, "Can not symbolic link to own group.", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                return;
            }
        }
        editor.addSymbolicLink(links, group);
    }

    /**
     * リンク設定ダイアログを開く
     */
    public void openLinkAttributesDialog() {
        MapLinkTable links = editor.getSelectedLinks();
        if (links.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(links, "Warning:\n    Links of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        double averageLength = 0.0;
        double averageWidth = 0.0;
        for (MapLink link : links) {
            averageLength += link.getLength();
            averageWidth += link.getWidth();
        }
        averageLength /= links.size();
        averageWidth /= links.size();

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Set link attributes");
        dialog.getDialogPane().setPrefWidth(536);
        VBox paramPane = new VBox();

        Label label = null;
        if (links.size() == 1) {
            label = new Label("ID: " + links.get(0).ID);
        } else {
            label = new Label("" + links.size() + " links selected");
        }
        label.setPadding(new Insets(0, 0, 12, 0));
        paramPane.getChildren().addAll(label);

        label = new Label("Parameters");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        // length field
        Label lengthLabel = new Label("length");
        lengthLabel.setPadding(new Insets(0, 0, 0, 4));
        TextField lengthField = new TextField("" + averageLength);
        lengthField.setMinWidth(160);
        Button calcLengthButton = new Button("Calc length");
        CheckBox heightDiffCheckBox = new CheckBox("Reflect height");
        calcLengthButton.setDisable(links.size() != 1);
        calcLengthButton.setOnAction(e -> {
            MapLink link = links.get(0);
            MapNode fromNode = link.getFrom();
            MapNode toNode = link.getTo();
            double distance = fromNode.getPosition().distance(toNode.getPosition()) * editor.getCurrentGroup().getScale();
            if (heightDiffCheckBox.isSelected()) {
                Point3D point0 = new Point3D(fromNode.getX(), fromNode.getY(), fromNode.getHeight());
                Point3D point1 = new Point3D(toNode.getX(), toNode.getY(), toNode.getHeight());
                distance = point0.distance(point1) * editor.getCurrentGroup().getScale();
            }
            lengthField.setText("" + distance);
        });
        Button lengthButton = new Button("Set");
        EventHandler lengthHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(lengthField.getText());
                if (value != null) {
                    double length = value;
                    editor.startOfCommandBlock();
                    for (MapLink link : links) {
                        if (link.getLength() != length) {
                            if (! editor.invoke(new SetLength(link, length))) {
                                break;
                            }
                        }
                    }
                    editor.endOfCommandBlock();
                    dialog.close();
                }
            }
        };
        lengthField.setOnAction(lengthHandler);
        lengthButton.setOnAction(lengthHandler);
        FlowPane lengthFlowPane = new FlowPane();
        lengthFlowPane.setHgap(8);
        lengthFlowPane.setPadding(new Insets(0, 0, 8, 0));
        lengthFlowPane.getChildren().addAll(lengthLabel, lengthField, calcLengthButton, heightDiffCheckBox, lengthButton);

        // width field
        Label widthLabel = new Label("width");
        widthLabel.setPadding(new Insets(0, 0, 0, 4));
        TextField widthField = new TextField("" + averageWidth);
        widthField.setMinWidth(160);
        Button widthButton = new Button("Set");
        EventHandler widthHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(widthField.getText());
                if (value != null) {
                    double width = value;
                    editor.startOfCommandBlock();
                    for (MapLink link : links) {
                        if (link.getWidth() != width) {
                            if (! editor.invoke(new SetWidth(link, width))) {
                                break;
                            }
                        }
                    }
                    editor.endOfCommandBlock();
                    dialog.close();
                }
            }
        };
        widthField.setOnAction(widthHandler);
        widthButton.setOnAction(widthHandler);
        FlowPane widthFlowPane = new FlowPane();
        widthFlowPane.setHgap(8);
        widthFlowPane.getChildren().addAll(widthLabel, widthField, widthButton);

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        TagSetupPane pane = new TagSetupPane(editor, links, dialog);
        paramPane.getChildren().addAll(label, lengthFlowPane, widthFlowPane, separator, pane);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * リンク長再計算ダイアログを開く
     */
    public void openRecalculateLinkLengthDialog() {
        MapLinkTable links = editor.getSelectedLinks();
        if (links.isEmpty()) {
            return;
        }

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Recalculate link length");
        dialog.getDialogPane().setPrefWidth(400);
        VBox paramPane = new VBox();

        Label label = new Label("" + links.size() + " links selected");
        label.setPadding(new Insets(0, 0, 12, 0));

        CheckBox reflectHeightCheckBox = new CheckBox("Reflect height");
        reflectHeightCheckBox.setFont(Font.font("Arial", FontWeight.BOLD, reflectHeightCheckBox.getFont().getSize()));

        paramPane.getChildren().addAll(label, reflectHeightCheckBox);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            editor.recalculateLinkLength(links, reflectHeightCheckBox.isSelected());
        }
    }

    /**
     * スケール計算とリンク長再計算ダイアログを開く
     */
    public void openCalculateScaleDialog() {
        if (editor.getCountOfSelectedLinks() != 1) {
            Alert alert = new Alert(AlertType.WARNING, "Please select only one link for calculation.", ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        MapPartGroup group = editor.getCurrentGroup();
        MapLink link = editor.getSelectedLinks().get(0);
        double actualDistance = link.getFrom().getPosition().distance(link.getTo().getPosition());

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Calculate scale and recalculate link length");
        dialog.getDialogPane().setPrefWidth(440);
        VBox paramPane = new VBox();

        Label scaleLabel = new Label("Current scale of this group");
        Label scaleValue = new Label("" + group.getScale());
        Label distanceLabel = new Label("Actual distance between nodes");
        Label distanceValue = new Label("" + actualDistance);
        Label lengthLabel = new Label("Link length");
        Label lengthValue = new Label("" + link.getLength());

        Label requiredLengthLabel = new Label("Required link length");
        requiredLengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, requiredLengthLabel.getFont().getSize()));
        TextField requiredLengthField = new TextField("" + link.getLength());
        requiredLengthField.setPrefWidth(170);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 0, 16, 0));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(scaleLabel, 1, 1);
        grid.add(scaleValue, 2, 1);
        grid.add(distanceLabel, 1, 2);
        grid.add(distanceValue, 2, 2);
        grid.add(lengthLabel, 1, 3);
        grid.add(lengthValue, 2, 3);
        grid.add(requiredLengthLabel, 1, 4);
        grid.add(requiredLengthField, 2, 4);

        paramPane.getChildren().add(grid);

        CheckBox recalcLengthCheckBox = new CheckBox("Recalculate link lengths with new scale");
        recalcLengthCheckBox.setPadding(new Insets(8, 0, 0, 8));
        recalcLengthCheckBox.setFont(Font.font("Arial", FontWeight.BOLD, recalcLengthCheckBox.getFont().getSize()));
        recalcLengthCheckBox.setSelected(true);

        CheckBox updateGroupCheckBox = new CheckBox("Update all groups");
        updateGroupCheckBox.setPadding(new Insets(8, 0, 0, 8));
        updateGroupCheckBox.setFont(Font.font("Arial", FontWeight.BOLD, updateGroupCheckBox.getFont().getSize()));
        updateGroupCheckBox.setSelected(true);

        paramPane.getChildren().addAll(recalcLengthCheckBox, updateGroupCheckBox);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double _requiredLength = convertToDouble(requiredLengthField.getText());
            if (_requiredLength == null || _requiredLength <= 0.0) {
                return;
            }
            double scale = _requiredLength / actualDistance;

            StringBuilder buff = new StringBuilder("Scale : ");
            buff.append(scale);
            buff.append("\n");
            if (recalcLengthCheckBox.isSelected()) {
                buff.append("Recalculate link lengths.\n");
            } else {
                buff.append("Do not recalculate link lengths.\n");
            }
            if (updateGroupCheckBox.isSelected()) {
                buff.append("Update all groups.\n");
            } else {
                buff.append("Update current group.\n");
            }
            buff.append("\nDo you want to continue?");

            Alert alert = new Alert(AlertType.CONFIRMATION, buff.toString(), ButtonType.YES, ButtonType.NO);
            alert.initOwner(frame);
            result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                editor.setScaleAndRecalculateLinkLength(group, scale, recalcLengthCheckBox.isSelected(), updateGroupCheckBox.isSelected());
            }
        }
    }

    /**
     * 選択中のリンクを一方通行に設定する。
     */
    public void setOneWay() {
        MapLinkTable seriesLinks = null;
        MapNode firstNode = null;
        MapNode lastNode = null;
        MapNode enteringNode = null;

        // 選択中のリンクを連続したリンクに変換する
        try {
            HashMap result = editor.getSeriesLinks(editor.getSelectedLinks());
            seriesLinks = (MapLinkTable)result.get("linkTable");
            firstNode = (MapNode)result.get("firstNode");
            lastNode = (MapNode)result.get("lastNode");
        } catch(Exception e) {
            Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
            alert.initOwner(frame);
            alert.showAndWait();
            return;
        }

        // 両端のノードに "A", "B" ラベルを表示する
        TextPosition positionA = editor.getClearPosition(seriesLinks.get(0), firstNode);
        TextPosition positionB = editor.getClearPosition(seriesLinks.get(seriesLinks.size() - 1), lastNode);
        canvas.setOneWayIndicator(true, firstNode, positionA, lastNode, positionB);
        canvas.repaintLater();

        // 方向を選択する
        ArrayList<String> directions = new ArrayList();
        directions.add("A -> B");
        directions.add("B -> A");
        ChoiceDialog<String> dialog = new ChoiceDialog<String>(directions.get(0), directions);
        dialog.initOwner(frame);
        dialog.setTitle("One-way setting");
        dialog.setHeaderText("Please select one-way direction");
        String direction = dialog.showAndWait().orElse("");
        if (direction.equals("A -> B")) {
            enteringNode = firstNode;
        } else if (direction.equals("B -> A")) {
            Collections.reverse(seriesLinks);
            enteringNode = lastNode;
        } else {
            canvas.setOneWayIndicator(false, null, TextPosition.CENTER, null, TextPosition.CENTER);
            canvas.repaintLater();
            return;
        }

        // リンクに一方通行タグを振る
        editor.startOfCommandBlock();
        for (MapLink link : seriesLinks) {
            if (link.isForwardDirectionFrom(enteringNode)) {
                if (! editor.invoke(new SetTrafficRestriction(link, true, false, false))) {
                    break;
                }
            } else {
                if (! editor.invoke(new SetTrafficRestriction(link, false, true, false))) {
                    break;
                }
            }
            enteringNode = link.getOther(enteringNode);
        }

        canvas.setOneWayIndicator(false, null, TextPosition.CENTER, null, TextPosition.CENTER);
        editor.deselectLinks();
        editor.endOfCommandBlock();
    }

    /**
     * エリア設定ダイアログを開く
     */
    public void openAreaAttributesDialog() {
        ArrayList<MapArea> areas = editor.getSelectedAreas();
        if (areas.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(areas, "Warning:\n    Areas of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Set area attributes");
        VBox paramPane = new VBox();

        if (areas.size() > 1) {
            Label label = new Label("" + areas.size() + " areas selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().add(label);
        }

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        TagSetupPane pane = new TagSetupPane(editor, areas, dialog);
        paramPane.getChildren().add(pane);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * ポリゴン設定ダイアログを開く
     */
    public void openPolygonAttributesDialog() {
        ArrayList<MapPolygon> polygons = editor.getSelectedPolygons();
        if (polygons.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(polygons, "Warning:\n    Polygons of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        double averageHeight = 0.0;
        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;
        int planePolygonCount = 0;
        for (MapPolygon polygon : polygons) {
            minIndex = Math.min(polygon.getZIndex(), minIndex);
            maxIndex = Math.max(polygon.getZIndex(), maxIndex);
            if (polygon.isPlanePolygon()) {
                averageHeight += polygon.getOuterBoundary().getHeight();
                planePolygonCount++;
            }
        }
        averageHeight /= planePolygonCount;

        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Set polygon attributes");
        VBox paramPane = new VBox();

        Label label = null;
        if (polygons.size() == 1) {
            label = new Label("ID: " + polygons.get(0).ID);
        } else {
            label = new Label("" + polygons.size() + " polygons selected");
        }
        label.setPadding(new Insets(0, 0, 12, 0));
        paramPane.getChildren().addAll(label);

        label = new Label("Parameters");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 0, 0, 10));
        grid.setHgap(8);
        grid.setVgap(8);
        int row = 1;

        // Z-index
        Label zIndexLabel = new Label(polygons.size() > 1 ? String.format("Z-index(%d...%d)", minIndex, maxIndex) : "Z-index");
        TextField zIndexField = new TextField("" + minIndex);
        zIndexField.setMinWidth(100);
        Button button = new Button("Set");
        EventHandler zIndexHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Integer value = convertToInteger(zIndexField.getText());
                if (value != null) {
                    int zIndex = value;
                    editor.startOfCommandBlock();
                    for (MapPolygon polygon : polygons) {
                        if (! editor.invoke(new SetZIndex(polygon, zIndex))) {
                            break;
                        }
                    }
                    editor.endOfCommandBlock();
                    dialog.close();
                }
            }
        };
        zIndexField.setOnAction(zIndexHandler);
        button.setOnAction(zIndexHandler);
        grid.add(zIndexLabel, 1, row);
        grid.add(zIndexField, 2, row);
        grid.add(button, 3, row);
        row++;

        if (planePolygonCount > 0) {
            // height field
            Label heightLabel = new Label("height(" + averageHeight + ")");
            TextField heightField = new TextField("" + averageHeight);
            heightField.setMinWidth(100);
            Button heightButton = new Button("Set");
            EventHandler heightHandler = new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    Double value = convertToDouble(heightField.getText());
                    if (value != null) {
                        double height = value;
                        editor.startOfCommandBlock();
                        for (MapPolygon polygon : polygons) {
                            if (polygon.isPlanePolygon()) {
                                if (! editor.invoke(new SetPolygonHeight(polygon, height))) {
                                    break;
                                }
                            }
                        }
                        editor.endOfCommandBlock();
                        dialog.close();
                    }
                }
            };
            heightField.setOnAction(heightHandler);
            heightButton.setOnAction(heightHandler);
            grid.add(heightLabel, 1, row);
            grid.add(heightField, 2, row);
            grid.add(heightButton, 3, row);
        }

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        TagSetupPane pane = new TagSetupPane(editor, polygons, dialog);

        if (polygons.size() == 1 && polygons.get(0).isTriangleMeshes()) {
            // Coordinates
            MapPolygon polygon = polygons.get(0);
            TriangleMeshes triangleMeshes = polygon.getTriangleMeshes();
            Label coordinatesLabel = new Label("Coordinates (Format: X1,Y1,Z1<space>X2,Y2,Z2<space>X3,Y3,Z3<space> ...)");
            coordinatesLabel.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
            coordinatesLabel.setPadding(new Insets(10, 0, 4, 0));
            TextArea coordinatesArea = new TextArea(triangleMeshes.getCoordinates().getCoordinatesText());
            coordinatesArea.setWrapText(true);

            Button coordinatesButton = new Button("Set");
            coordinatesButton.setOnAction(e -> {
                Coordinates coordinates = null;
                try {
                    coordinates = new Coordinates(coordinatesArea.getText(), 3);
                } catch (Exception ex) {
                    Alert alert = new Alert(AlertType.WARNING, ex.getMessage(), ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
                if (coordinates.getValue().size() < 3) {
                    Alert alert = new Alert(AlertType.WARNING, "Lack of coordinates", ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
                editor.invokeSingleCommand(new SetTriangleMeshesCoordinates(polygon, coordinates));
                dialog.close();
            });
            FlowPane flowPane = new FlowPane();
            flowPane.setAlignment(Pos.CENTER_RIGHT);
            flowPane.setPadding(new Insets(4, 0, 0, 0));
            flowPane.getChildren().addAll(coordinatesButton);

            dialog.setResizable(true);
            paramPane.getChildren().addAll(label, grid, coordinatesLabel, coordinatesArea, flowPane, separator, pane);
        } else {
            paramPane.getChildren().addAll(label, grid, separator, pane);
        }

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    private static FlowPane flowWrap(Node node) {
        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(2, 0, 2, 0));
        flowPane.getChildren().add(node);
        return flowPane;
    }

    /**
     * 三角形メッシュポリゴン追加ダイアログを開く
     */
    public void openAddTriangleMeshDialog() {
        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Add triangle mesh polygon");
        dialog.getDialogPane().setPrefWidth(600);
        dialog.setResizable(true);
        VBox paramPane = new VBox();

        // Z-index
        Label zIndexLabel = new Label("Z-index");
        TextField zIndexField = new TextField("0");

        // Coordinates
        Point2D point = canvas.convertToOriginal(canvas.getMapPointOnTheMouseCursor());
        Label coordinatesLabel = new Label("Coordinates (Format: X1,Y1,Z1<space>X2,Y2,Z2<space>X3,Y3,Z3<space> ...)");
        TextArea coordinatesArea = new TextArea("" + point.getX() + "," + point.getY() + ",0.0 ");
        coordinatesArea.setWrapText(true);

        // Tags
        Label tagsLabel = new Label("Tags (Format: TAG1<line feed>TAG2<line feed> ...)");
        TextArea tagsArea = new TextArea();
        tagsArea.setWrapText(true);

        paramPane.getChildren().addAll(flowWrap(zIndexLabel), flowWrap(zIndexField), flowWrap(coordinatesLabel), coordinatesArea, flowWrap(flowWrap(tagsLabel)), tagsArea);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        while (result.isPresent() && result.get() == ButtonType.OK) {
            int zIndex = 0;
            Integer value = convertToInteger(zIndexField.getText().trim());
            if (value == null) {
                result = dialog.showAndWait();
                continue;
            }
            zIndex = value;

            Coordinates coordinates = null;
            try {
                coordinates = new Coordinates(coordinatesArea.getText(), 3);
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                result = dialog.showAndWait();
                continue;
            }
            if (coordinates.getValue().size() < 3) {
                Alert alert = new Alert(AlertType.WARNING, "Lack of coordinates", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                result = dialog.showAndWait();
                continue;
            }

            String tagsText = tagsArea.getText().trim();
            String[] tags = tagsText.split("\\r\\n|\\n|\\r");
            if (tagsText.isEmpty() || tags.length == 0) {
                Alert alert = new Alert(AlertType.WARNING, "There is no tag", ButtonType.OK);
                alert.initOwner(frame);
                alert.showAndWait();
                result = dialog.showAndWait();
                continue;
            }

            editor.startOfCommandBlock();
            AddPolygon command = new AddPolygon(editor.getCurrentGroup(), zIndex, new TriangleMeshes(coordinates));
            if (editor.invoke(command)) {
                MapPolygon polygon = (MapPolygon)editor.getMap().getObject(command.getId());
                for (String tag : tags) {
                    if (! editor.invoke(new AddTag(polygon, tag))) {
                        editor.endOfCommandBlock();
                        return;
                    }
                }
            }
            editor.endOfCommandBlock();
            break;
        }
    }

    /**
     * 背景画像をセットする
     */
    public void setBackgroundImage() {
        Point2D point = canvas.convertToOriginal(canvas.getMapPointOnTheMouseCursor());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open background image file");
        fileChooser.setInitialDirectory(editor.getDir());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image", "*.bmp", "*.gif", "*.jpg", "*.png"),
            new FileChooser.ExtensionFilter("All", "*.*")
        );
        File file = fileChooser.showOpenDialog(frame);
        if (file != null) {
            try {
                if (! file.getParentFile().getCanonicalPath().equals(editor.getPath())) {
                    Alert alert = new Alert(AlertType.WARNING, "Directory can not be changed.", ButtonType.OK);
                    alert.initOwner(frame);
                    alert.showAndWait();
                    return;
                }
            } catch (IOException ex) {
		Itk.dumpStackTraceOf(ex);
                return;
            }
            MapPartGroup group = editor.getCurrentGroup();
            editor.invokeSingleCommand(new SetBackgroundImage(group, file.getName(), point.getX(), point.getY()));
        }
    }

    /**
     * 背景画像設定ダイアログを開く
     */
    public void openBackgroundImageAttributesDialog() {
        MapPartGroup group = editor.getCurrentGroup();
        Dialog dialog = new Dialog();
        dialog.initOwner(frame);
        dialog.setTitle("Set background image attributes");
        VBox paramPane = new VBox();

        Label label = new Label("Parameters");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        // sx
        Label scaleXLabel = new Label("Scale X");
        scaleXLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleXLabel.getFont().getSize()));
        TextField scaleXField = new TextField("" + group.sx);
        scaleXField.setPrefWidth(128);
        Button scaleXButton = new Button("Set");
        EventHandler scaleXHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(scaleXField.getText());
                if (value != null) {
                    double scaleX = value;
                    if (scaleX <= 0.0) {
                        alertInvalidInputValue("Scale X : " + scaleX);
                        return;
                    }
                    if (scaleX != group.sx) {
                        editor.invokeSingleCommand(new ScaleTheBackgroundImage(group, scaleX, group.sy));
                    }
                    dialog.close();
                }
            }
        };
        scaleXField.setOnAction(scaleXHandler);
        scaleXButton.setOnAction(scaleXHandler);

        // sy
        Label scaleYLabel = new Label("Scale Y");
        scaleYLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleYLabel.getFont().getSize()));
        TextField scaleYField = new TextField("" + group.sy);
        scaleYField.setPrefWidth(128);
        Button scaleYButton = new Button("Set");
        EventHandler scaleYHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(scaleYField.getText());
                if (value != null) {
                    double scaleY = value;
                    if (scaleY <= 0.0) {
                        alertInvalidInputValue("Scale Y : " + scaleY);
                        return;
                    }
                    if (scaleY != group.sy) {
                        editor.invokeSingleCommand(new ScaleTheBackgroundImage(group, group.sx, scaleY));
                    }
                    dialog.close();
                }
            }
        };
        scaleYField.setOnAction(scaleYHandler);
        scaleYButton.setOnAction(scaleYHandler);

        // r
        Label angleLabel = new Label("Rotation angle");
        angleLabel.setFont(Font.font("Arial", FontWeight.BOLD, angleLabel.getFont().getSize()));
        TextField angleField = new TextField("" + group.r);
        angleField.setPrefWidth(128);
        Button angleButton = new Button("Set");
        EventHandler angleHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(angleField.getText());
                if (value != null) {
                    double angle = value;
                    if (angle < 0.0 || angle > 360.0) {
                        alertInvalidInputValue("Rotation angle : " + angle);
                        return;
                    }
                    if (angle == 360.0) {
                        angle = 0.0;
                    }
                    if (angle != group.r) {
                        editor.invokeSingleCommand(new RotateBackgroundImage(group, angle * Math.PI / 180.0));
                    }
                    dialog.close();
                }
            }
        };
        angleField.setOnAction(angleHandler);
        angleButton.setOnAction(angleHandler);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.add(scaleXLabel, 1, 1);
        grid.add(scaleXField, 2, 1);
        grid.add(scaleXButton, 3, 1);
        grid.add(scaleYLabel, 1, 2);
        grid.add(scaleYField, 2, 2);
        grid.add(scaleYButton, 3, 2);
        grid.add(angleLabel, 1, 3);
        grid.add(angleField, 2, 3);
        grid.add(angleButton, 3, 3);

        paramPane.getChildren().addAll(label, grid);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * 背景画像を削除する
     */
    public void removeBackgroundImage() {
        Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Do you really want to delete?", ButtonType.YES, ButtonType.NO);
        alert.initOwner(frame);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            editor.invokeSingleCommand(new RemoveBackgroundImage(editor.getCurrentGroup()));
        }
    }

    /**
     * Invalid input value ダイアログを表示する
     */
    public void alertInvalidInputValue(String message) {
        Alert alert = new Alert(AlertType.WARNING, message, ButtonType.OK);
        alert.initOwner(frame);
        alert.getDialogPane().setHeaderText("Invalid input value");
        alert.showAndWait();
    }

    /**
     * 文字列を検証して整数に変換する
     */
    public Integer convertToInteger(String str) {
        try {
            return Integer.valueOf(str);
        } catch(NumberFormatException e) {
            alertInvalidInputValue(e.getMessage());
        }
        return null;
    }

    /**
     * 文字列を検証して実数に変換する
     */
    public Double convertToDouble(String str) {
        try {
            return Double.valueOf(str);
        } catch(NumberFormatException e) {
            alertInvalidInputValue(e.getMessage());
        }
        return null;
    }
    
    /**
     * ファイル選択ダイアログの初期パスを設定する
     */
    public void setInitialPath(FileChooser fileChooser, String path) {
        if (path == null || path.isEmpty()) {
            fileChooser.setInitialDirectory(new File("./"));
        } else {
            File file = new File(path);
            fileChooser.setInitialDirectory(file.getParentFile());
            fileChooser.setInitialFileName(file.getName());     // TODO: 現状では無効
        }
    }

    /**
     * ウィンドウを表示する
     */
    public void show() {
        frame.show();
    }

    /**
     * ステータスラインにメッセージを表示する
     */
    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    /**
     * BACKGROUND_IMAGE モードのコンテキスト・メニューの有効状態を設定する
     */
    public void setBgImageMenuStatus() {
        boolean imageExisting = (editor.getBackgroundImage(editor.getCurrentGroup()) != null);
        miSetBackgroundImage.setDisable(imageExisting);
        miSetBackgroundImageAttributes.setDisable(! imageExisting);
        miRemoveBackgroundImage.setDisable(! imageExisting);
    }

    /**
     * グループボタンをクリックする
     */
    public void clickGroupButton(MapPartGroup group) {
        groupButtonMap.get(group).fire();
    }

    /**
     * グループ表示を更新する
     */
    public void updateGroupTag(MapPartGroup group) {
        ToggleButton groupButton = groupButtonMap.get(group);
        groupButton.setText(group.getTagString());
    }

    /**
     * グループを選択する
     */
    public void selectGroup(int index) {
        if (index >= 0 && index < groupButtons.size() && ! groupButtons.get(index).isSelected()) {
            groupButtons.get(index).fire();
        }
    }

    public Stage getStage() {
        return frame;
    }

    public EditorCanvas getCanvas() {
        return canvas;
    }

    public GroupPanel getGroupPanel() {
        return groupPanel;
    }

    public NodePanelFx getNodePanel() {
        return nodePanel;
    }

    public LinkPanelFx getLinkPanel() {
        return linkPanel;
    }

    public AreaPanelFx getAreaPanel() {
        return areaPanel;
    }

    public PolygonPanel getPolygonPanel() {
        return polygonPanel;
    }

    public ScenarioPanelFx getScenarioPanel() {
        return scenarioPanel;
    }

    public ContextMenu getEditNodeMenu() {
        return editNodeMenu;
    }

    public ContextMenu getEditLinkMenu() {
        return editLinkMenu;
    }

    public ContextMenu getEditAreaMenu() {
        return editAreaMenu;
    }

    public ContextMenu getEditPolygonMenu() {
        return editPolygonMenu;
    }

    public ContextMenu getAddTriangleMeshMenu() {
        return addTriangleMesheMenu;
    }

    public ContextMenu getBgImageMenu() {
        return bgImageMenu;
    }

    public boolean isContextMenuShowing() {
        return editNodeMenu.isShowing() || editLinkMenu.isShowing() || editAreaMenu.isShowing() || editPolygonMenu.isShowing() || addTriangleMesheMenu.isShowing() || bgImageMenu.isShowing();
    }
}
