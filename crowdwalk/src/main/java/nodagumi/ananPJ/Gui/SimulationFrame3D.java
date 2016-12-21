package nodagumi.ananPJ.Gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.GuiSimulationLauncher3D;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNode.NType;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.Simulator.PollutionHandler.*;
import nodagumi.Itk.*;

/**
 * 3D シミュレーションウィンドウ
 */
public class SimulationFrame3D extends Stage implements Observer {
    /**
     * シミュレーションウィンドウ
     */
    private Stage frame;

    /**
     * シミュレーションパネル
     */
    private SimulationPanel3D panel;

    /**
     * ランチャー
     */
    private GuiSimulationLauncher3D launcher;

    /**
     * 地図データ。
     */
    private NetworkMap networkMap;

    /**
     * 領域
     */
    private ArrayList<MapArea> areas;

    /**
     * カメラワーク
     */
    private ArrayList<CameraShot> camerawork = new ArrayList<CameraShot>();

    /**
     * シミュレーションのサイクル間のスリープ値
     */
    private int deferFactor = 0;

    /**
     * 読み込むカメラワークデータのファイル名
     */
    private String cameraworkFile = null;

    /**
     * リンク表示を実際の道幅でおこなうかどうか
     */
    private boolean atActualWidth = false;

    /* コントロールボタンパネルを構成する UI コントロール */

    private ImageView startIcon;
    private ImageView pauseIcon;
    private ToggleButton start_button = null;
    private Button step_button = null;
    private Label simulationDeferFactorValue;

    /**
     * タブパネル
     */
    private TabPane tabPane = new TabPane();

    /* Control タブを構成する UI コントロール */

    private Tab controlTab = new Tab("Control");
    private Label clock_label = new Label("00:00:00");
    private Label time_label = new Label("NOT STARTED");
    private Label evacuatedCount_label = new Label("NOT STARTED");
    private TextArea messageArea = new TextArea("UNMaps Version 1.9.5\n");

    /* View タブを構成する UI コントロール */

    private Tab viewTab = new Tab("View");
    private ScrollBar verticalScaleControl = new ScrollBar();
    private Label verticalScaleValue;
    private ScrollBar agent_size_control = new ScrollBar();
    private Label agent_size_value;
    private CheckBox record_snapshots = new CheckBox("Record simulation screen");
    private CheckBox viewSynchronizedCheckBox = new CheckBox("View-calculation synchronized");
    private CheckBox exit_with_simulation_finished_cb = new CheckBox("Exit with simulation finished");
    private Button centerinButton = new Button("Centering");
    private Button centerinWithScalingButton = new Button("Centering with scaling");

    /* Camera タブを構成する UI コントロール */

    private Tab cameraTab = new Tab("Camera");
    private boolean forceUpdateCamerawork = true;
    private boolean viewpointChangeInhibited = false;
    private CheckBox replayCheckBox = new CheckBox();
    private TextArea cameraworkArea = new TextArea();
    private Button recordButton = new Button("Record");
    private Button loadCameraworkButton = new Button("Load");
    private Button saveCameraworkButton = new Button("Save as");

    /* Status タブを構成する UI コントロール */

    private Tab statusTab = new Tab("Status");
    private TextArea indicationArea = new TextArea("Information");

    /* ステータスバーを構成する UI コントロール */

    private Label status = new Label("NOT STARTED");

    /* スクリーンショット保存用スレッド数を管理するためのカウンタ制御 */

    private int saveThreadCount = 0;
    synchronized public int getSaveThreadCount() { return saveThreadCount; }
    synchronized public void incSaveThreadCount() { saveThreadCount++; }
    synchronized public void decSaveThreadCount() { saveThreadCount--; }

    /**
     * コンストラクタ
     */
    public SimulationFrame3D(String title, int width, int height,
            GuiSimulationLauncher3D launcher, final CrowdWalkPropertiesHandler properties) {
        frame = this;
        this.launcher = launcher;
        networkMap = launcher.getMap();

        init(properties);

        // メニューの準備
        Node menuBar = createMenu();

        // シミュレーションパネルの準備
        panel = new SimulationPanel3D(width, height, networkMap, atActualWidth, launcher.getVerticalScale(), properties);
        panel.setPrefSize(width, height);
        panel.setShow3dPolygon(launcher.isShow3dPolygon());
        panel.setChangeAgentColorDependingOnSpeed(launcher.isChangeAgentColorDependingOnSpeed());
        panel.setShowStatusPosition(launcher.getShowStatusPosition().toLowerCase());
        panel.setShowStatus(launcher.isShowStatus());
        panel.setShowLogo(launcher.isShowLogo());
        panel.addObserver(this);
        ChangeListener listener = new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                String label = String.format("Record simulation screen (%d x %d)",
                        ((Double)panel.getWidth()).intValue(), ((Double)panel.getHeight()).intValue());
                record_snapshots.setText(label);
            }
        };
        panel.widthProperty().addListener(listener);
        panel.heightProperty().addListener(listener);

        // タブパネルの準備
        controlTab.setContent(createControlPane());
        controlTab.setClosable(false);
        viewTab.setContent(createViewPane());
        viewTab.setClosable(false);
        cameraTab.setContent(createCameraPane());
        cameraTab.setClosable(false);
        statusTab.setContent(createStatusPane());
        statusTab.setClosable(false);
        tabPane.getTabs().addAll(controlTab, viewTab, cameraTab, statusTab);

        // コントロールボタンパネルの準備
        Pane controlButtonPane = createControlButtonPane();

        // ステータスバーの準備
        status.setPadding(new Insets(4));

        BorderPane rightPane = new BorderPane();
        rightPane.setCenter(tabPane);
        rightPane.setBottom(controlButtonPane);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(menuBar);
        borderPane.setCenter(panel);
        borderPane.setRight(rightPane);
        borderPane.setBottom(status);

        Scene scene = new Scene(borderPane);
        setTitle(title);
        setScene(scene);

        // ウィンドウイベントのハンドリング
        setOnShown(e -> {
            if (launcher.isAutoSimulationStart()) {
                Platform.runLater(() -> {
                    if (! getStartButton().isSelected()) {
                        Itk.logInfo("auto simulation start");
                        getStartButton().fire();
                    }
                });
            }
        });
        setOnHiding(e -> {
            if (launcher.isRunning()) {
                launcher.pause();
            }
            launcher.saveSimulatorPosition((int)getX(), (int)getY());
        });
        setOnHidden(e -> {
            launcher.quit();
        });
    }

    /**
     * 初期設定
     */
    private void init(CrowdWalkPropertiesHandler properties) {
        deferFactor = launcher.getDeferFactor();
        areas = launcher.getMap().getAreas();

        if (properties == null || properties.getPropertiesFile() == null) {
            return;
        }

        String filePath = properties.getString("camera_file", "");
        if (filePath.toLowerCase().endsWith(".json")) {
            loadCamerawork(filePath);
        }
    }

    /**
     * メニューを作成する
     */
    private Node createMenu() {
        MenuBar menuBar = new MenuBar();

        //// File menu ////

        Menu fileMenu = new Menu("File");

        MenuItem miClose = new MenuItem("Close");
        miClose.setOnAction(e -> {
            if (launcher.isRunning()) {
                launcher.pause();
            }
            launcher.saveSimulatorPosition((int)getX(), (int)getY());
            frame.close();
        });
        miClose.setAccelerator(KeyCombination.valueOf("Ctrl+W"));

        fileMenu.getItems().addAll(miClose);

        menuBar.getMenus().addAll(fileMenu);

        return menuBar;
    }

    /**
     * コントロールボタンパネルを作成する
     */
    private Pane createControlButtonPane() {
        Image start_icon = new Image(getClass().getResourceAsStream("/img/start.png"));
        startIcon = new ImageView(start_icon);
        startIcon.setViewport(new Rectangle2D(-10, 0, 32, 12));
        Image pause_icon = new Image(getClass().getResourceAsStream("/img/pause.png"));
        pauseIcon = new ImageView(pause_icon);
        pauseIcon.setViewport(new Rectangle2D(-10, 0, 32, 12));
        Image step_icon = new Image(getClass().getResourceAsStream("/img/step.png"));
        ImageView stepIcon = new ImageView(step_icon);
        stepIcon.setViewport(new Rectangle2D(-7, 0, 32, 12));

        start_button = new ToggleButton();
        start_button.setGraphic(startIcon);
        start_button.setOnAction(e -> {
            if (start_button.isSelected()) {
                start_button.setGraphic(pauseIcon);
                verticalScaleControl.setDisable(true);
                launcher.start();
            } else {
                start_button.setGraphic(startIcon);
                launcher.pause();
                verticalScaleControl.setDisable(false);
            }
            update_buttons();
        });

        step_button = new Button();
        step_button.setGraphic(stepIcon);
        step_button.setOnAction(e -> {
            start_button.setDisable(true);
            step_button.setDisable(true);
            // ボタンクリックでいきなりプログラムが終了することがないようにするため
            if (exit_with_simulation_finished_cb.isSelected()) {
                exit_with_simulation_finished_cb.fire();
            }

            // 別スレッドでステップ実行
            Task<Boolean> task = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    launcher.step();
                    Platform.runLater(() -> start_button.setDisable(false));
                    return true; 
                }
            };
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        Label label = new Label("wait");

        ScrollBar deferFactorControl = new ScrollBar();
        deferFactorControl.setPrefSize(200, 20);
        deferFactorControl.setMax(300);
        deferFactorControl.setValue(deferFactor);
        deferFactorControl.valueProperty().addListener((ov, oldValue, newValue) -> {
            setSimulationDeferFactor(newValue.intValue());
            simulationDeferFactorValue.setText("" + deferFactor);
        });

        simulationDeferFactorValue = new Label("" + deferFactor);
        simulationDeferFactorValue.setAlignment(Pos.CENTER_RIGHT);

        FlowPane innerPane = new FlowPane(12, 0);
        BorderStroke borderStroke = new BorderStroke(Color.LIGHTGREY, BorderStrokeStyle.SOLID, new CornerRadii(2), BorderWidths.DEFAULT);
        innerPane.setBorder(new Border(borderStroke));
        innerPane.setPadding(new Insets(8, 0, 8, 0));
        innerPane.setAlignment(Pos.CENTER);
        innerPane.getChildren().addAll(start_button, step_button, label, deferFactorControl, simulationDeferFactorValue);

        StackPane pane = new StackPane();
        pane.setPadding(new Insets(0, 8, 0, 8));
        pane.getChildren().add(innerPane);

        return pane;
    }

    /**
     * スタートボタンを返す
     */
    public ToggleButton getStartButton() {
        return start_button;
    }

    /**
     * ボタン類のアップデート
     */
    public void update_buttons() {
        step_button.setDisable(launcher.isRunning());
    }

    /**
     * シミュレーション遅延の制御（画面）
     */
    public void setSimulationDeferFactor(int deferFactor) {
        this.deferFactor = deferFactor;
        launcher.getSimulator().setSimulationDeferFactor(deferFactor);
    }

    /**
     * Control タブを作成する
     */
    private Pane createControlPane() {
        /* title & clock */

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(0, 0, 8, 0));
        Insets insets = new Insets(3, 8, 3, 12);

        Label label_Map = createLabel("Map", insets);
        gridPane.add(label_Map, 0, 0);
        gridPane.setHalignment(label_Map, HPos.RIGHT);
        if (launcher.getNetworkMapFile() != null) {
            File map_file = new File(launcher.getNetworkMapFile());
            gridPane.add(createLabel(map_file.getName(), insets), 1, 0);
        } else {
            gridPane.add(createLabel("No map file", insets), 1, 0);
        }

        Label label_Agent = createLabel("Agent", insets);
        gridPane.add(label_Agent, 0, 1);
        gridPane.setHalignment(label_Agent, HPos.RIGHT);
        if (launcher.getGenerationFile() != null) {
            File generation_file = new File(launcher.getGenerationFile());
            gridPane.add(createLabel(generation_file.getName(), insets), 1, 1);
        } else {
            gridPane.add(createLabel("No generation file", insets), 1, 1);
        }

        Label label_Scenario = createLabel("Scenario", insets);
        gridPane.add(label_Scenario, 0, 2);
        gridPane.setHalignment(label_Scenario, HPos.RIGHT);
        if (launcher.getScenarioFile() != null) {
            File scenario_file = new File(launcher.getScenarioFile());
            gridPane.add(createLabel(scenario_file.getName(), insets), 1, 2);
        } else {
            gridPane.add(createLabel("No scenario file", insets), 1, 2);
        }

        Label label_Pollution = createLabel("Pollution", insets);
        gridPane.add(label_Pollution, 0, 3);
        gridPane.setHalignment(label_Pollution, HPos.RIGHT);
        if (launcher.getPollutionFile() != null) {
            File pollution_file = new File(launcher.getPollutionFile());
            gridPane.add(createLabel(pollution_file.getName(), insets), 1, 3);
        } else {
            gridPane.add(createLabel("No pollution file", insets), 1, 3);
        }

        clock_label.setPadding(new Insets(4, 8, 4, 12));
        clock_label.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
        gridPane.add(clock_label, 0, 4);
        gridPane.setHalignment(clock_label, HPos.RIGHT);
        time_label.setPadding(new Insets(4, 8, 4, 12));
        gridPane.add(time_label, 1, 4);

        evacuatedCount_label.setPadding(new Insets(2, 8, 3, 12));
        gridPane.add(evacuatedCount_label, 0, 5, 2, 1);

        /* scenarios */

        GridPane scenarioPane = new GridPane();
        scenarioPane.setAlignment(Pos.CENTER);
        scenarioPane.setHgap(10);
        scenarioPane.setVgap(10);
        int row = 0;
        for (final EventBase event : launcher.getSimulator().getScenario().eventList) {
            ToggleGroup group = new ToggleGroup();

            RadioButton radio_button = new RadioButton("enabled");
            radio_button.setToggleGroup(group);
            radio_button.setOnAction(e -> {
                event.occur(SimTime.Ending, networkMap);
            });
            scenarioPane.add(radio_button, 0, row);

            radio_button = new RadioButton("disabled | auto:");
            radio_button.setToggleGroup(group);
            radio_button.setOnAction(e -> {
                event.unoccur(SimTime.Ending, networkMap);
            });
            scenarioPane.add(radio_button, 1, row);

            radio_button = new RadioButton(event.atTime.getAbsoluteTimeString());
            radio_button.setToggleGroup(group);
            radio_button.setSelected(true);
            radio_button.setOnAction(e -> {
                Itk.logError("wrong index");
            });
            scenarioPane.add(radio_button, 2, row);

            row++;
        }

        ScrollPane scroller = new ScrollPane();
        scroller.setPrefSize(400, 160);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        scroller.setContent(scenarioPane);

        /* text message */

        messageArea.setEditable(false);
        messageArea.setWrapText(true);

        ScrollPane messageScroller = new ScrollPane();
        messageScroller.setPrefSize(400, 150);
        messageScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        messageScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        messageScroller.setContent(messageArea);

        BorderPane controlPane = new BorderPane();
        controlPane.setPadding(new Insets(8, 8, 16, 8));
        controlPane.setTop(gridPane);
        controlPane.setCenter(scroller);
        controlPane.setBottom(messageScroller);

        return controlPane;
    }

    /**
     * padding 付きで Label を生成する
     */
    public Label createLabel(String text, Insets insets) {
        Label label = new Label(text);
        label.setPadding(insets);
        label.setFont(Font.font("Verdana", 12));
        return label;
    }

    /**
     * 時計表示
     */
    public void displayClock(SimTime currentTime) {
        time_label.setText(String.format("Elapsed: %5.2fsec", currentTime.getRelativeTime()));
        clock_label.setText(currentTime.getAbsoluteTimeString());
    }

    /**
     * evacuation count ラベルを更新する
     */
    public void updateEvacuatedCount(String text) {
        evacuatedCount_label.setText(text);
    }

    /**
     * View タブを作成する
     */
    private Pane createViewPane() {
        // 垂直スケール

        Label label = new Label("Vertical scale");
        label.setPrefWidth(90);

        verticalScaleControl.setPrefSize(250, 20);
        verticalScaleControl.setMax(10.0);
        verticalScaleControl.setValue(launcher.getVerticalScale());
        verticalScaleControl.setUnitIncrement(0.1);
        verticalScaleControl.setBlockIncrement(1.0);
        verticalScaleControl.valueProperty().addListener((ov, oldValue, newValue) -> {
            verticalScaleValue.setText(String.format("%2.1f", newValue.doubleValue()));
        });
        verticalScaleControl.setOnMouseClicked(event -> {
            panel.changeVerticalScale(verticalScaleControl.getValue(), getAgentSize(), frame);
        });
        verticalScaleControl.setOnMouseReleased(event -> {
            panel.changeVerticalScale(verticalScaleControl.getValue(), getAgentSize(), frame);
        });

        verticalScaleValue = new Label("" + verticalScaleControl.getValue());
        verticalScaleValue.setAlignment(Pos.CENTER_RIGHT);

        FlowPane verticalScalePane = new FlowPane(12, 0);
        verticalScalePane.getChildren().addAll(label, verticalScaleControl, verticalScaleValue);

        // エージェントサイズ

        label = new Label("Agent size");
        label.setPrefWidth(90);

        agent_size_control.setPrefSize(250, 20);
        agent_size_control.setMax(30.0);
        agent_size_control.setValue(launcher.getAgentSize());
        agent_size_control.setUnitIncrement(0.1);
        agent_size_control.setBlockIncrement(1.0);
        agent_size_control.valueProperty().addListener((ov, oldValue, newValue) -> {
            agent_size_value.setText(String.format("%2.1f", newValue.doubleValue()));
            panel.changeAgentSize(newValue.doubleValue());
        });

        agent_size_value = new Label("" + agent_size_control.getValue());
        agent_size_value.setAlignment(Pos.CENTER_RIGHT);

        FlowPane agentSizePane = new FlowPane(12, 0);
        agentSizePane.getChildren().addAll(label, agent_size_control, agent_size_value);

        //// Checkboxes ////

        VBox checkboxPanel = new VBox(14);
        checkboxPanel.setPadding(new Insets(0, 4, 0, 4));
        checkboxPanel.getChildren().addAll(verticalScalePane, agentSizePane, new Separator());

        // ノード表示の ON/OFF
        CheckBox showNodesCheckBox = new CheckBox("Show nodes");
        showNodesCheckBox.setSelected(true);
        showNodesCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShowNodes(newValue);
        });

        FlowPane showNodesPanel = new FlowPane(12, 0);
        showNodesPanel.getChildren().addAll(showNodesCheckBox);
        checkboxPanel.getChildren().add(showNodesPanel);

        // リンク表示の ON/OFF
        CheckBox showLinksCheckBox = new CheckBox("Show links");
        showLinksCheckBox.setSelected(true);
        showLinksCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShowLinks(newValue);
        });

        FlowPane showLinksPanel = new FlowPane(12, 0);
        showLinksPanel.getChildren().addAll(showLinksCheckBox);
        checkboxPanel.getChildren().add(showLinksPanel);

        // エリア表示の ON/OFF
        CheckBox showAreasCheckBox = new CheckBox("Show areas");
        showAreasCheckBox.setSelected(launcher.getPollutionFile() != null);
        showAreasCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShowAreas(newValue);
        });

        // エリアの配置を確認できる様にする
        CheckBox showOutlineCheckBox = new CheckBox("Show outline of areas");
        showOutlineCheckBox.setSelected(false);
        showOutlineCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShowAreasOutline(newValue);
        });

        FlowPane showAreaPanel = new FlowPane(12, 0);
        showAreaPanel.setDisable(launcher.getPollutionFile() == null);
        showAreaPanel.getChildren().addAll(showAreasCheckBox, showOutlineCheckBox);
        checkboxPanel.getChildren().add(showAreaPanel);

        // エージェント表示の ON/OFF
        CheckBox showAgentsCheckBox = new CheckBox("Show agents");
        showAgentsCheckBox.setSelected(true);
        showAgentsCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShowAgents(newValue);
        });

        FlowPane showAgentPanel = new FlowPane(12, 0);
        showAgentPanel.getChildren().addAll(showAgentsCheckBox);
        checkboxPanel.getChildren().add(showAgentPanel);

        // ポリゴン表示の ON/OFF
        CheckBox show3dPolygonCheckBox = new CheckBox("Show 3D polygons");
        show3dPolygonCheckBox.setSelected(! panel.getPolygonLinks().isEmpty() && launcher.isShow3dPolygon());
        show3dPolygonCheckBox.setDisable(panel.getPolygonLinks().isEmpty());
        show3dPolygonCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShow3dPolygon(newValue);
        });
        checkboxPanel.getChildren().add(show3dPolygonCheckBox);

        // スクリーンショットを撮る
        record_snapshots.setSelected(launcher.isRecordSimulationScreen());
        record_snapshots.selectedProperty().addListener((ov, oldValue, newValue) -> {
            launcher.setRecordSimulationScreen(newValue);
        });
        checkboxPanel.getChildren().add(record_snapshots);

        // 歩行速度に応じてエージェントの色を変える
        CheckBox change_agent_color_depending_on_speed_cb = new CheckBox("Change agent color depending on speed");
        change_agent_color_depending_on_speed_cb.setSelected(launcher.isChangeAgentColorDependingOnSpeed());
        change_agent_color_depending_on_speed_cb.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setChangeAgentColorDependingOnSpeed(newValue);
            panel.updateAgentsColor();
        });
        checkboxPanel.getChildren().add(change_agent_color_depending_on_speed_cb);

        // シミュレーションビュー上に進捗状況のテキストを重ねて表示する。及び表示位置の選択

        boolean showStatus = launcher.isShowStatus();
        final CheckBox show_status_cb = new CheckBox("Show status");
        final RadioButton top_rb = new RadioButton("Top");
        final RadioButton bottom_rb = new RadioButton("Bottom");
        show_status_cb.setSelected(showStatus);
        show_status_cb.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShowStatus(newValue);
            top_rb.setDisable(! newValue);
            bottom_rb.setDisable(! newValue);
        });

        ToggleGroup group = new ToggleGroup();
        String showStatusPosition = launcher.getShowStatusPosition().toLowerCase();

        top_rb.setSelected(showStatusPosition.equals("top"));
        top_rb.setDisable(! showStatus);
        top_rb.setToggleGroup(group);
        top_rb.setOnAction(e -> {
            panel.setShowStatusPosition("top");
        });

        bottom_rb.setSelected(! showStatusPosition.equals("top"));
        bottom_rb.setDisable(! showStatus);
        bottom_rb.setToggleGroup(group);
        bottom_rb.setOnAction(e -> {
            panel.setShowStatusPosition("bottom");
        });

        FlowPane showStatusPanel = new FlowPane(12, 0);
        showStatusPanel.getChildren().addAll(show_status_cb, top_rb, bottom_rb);
        checkboxPanel.getChildren().add(showStatusPanel);

        // AIST ロゴの表示
        CheckBox show_logo_cb = new CheckBox("Show logo");
        show_logo_cb.setSelected(launcher.isShowLogo());
        show_logo_cb.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setShowLogo(newValue);
        });
        checkboxPanel.getChildren().add(show_logo_cb);

        // 表示の更新が完了するのを待ってから次のステップに進む
        viewSynchronizedCheckBox.setSelected(true);
        checkboxPanel.getChildren().add(viewSynchronizedCheckBox);

        // シミュレーション終了と同時にプログラムを終了する
        exit_with_simulation_finished_cb.setSelected(launcher.isExitWithSimulationFinished());
        exit_with_simulation_finished_cb.selectedProperty().addListener((ov, oldValue, newValue) -> {
            launcher.setExitWithSimulationFinished(newValue);
        });
        checkboxPanel.getChildren().add(exit_with_simulation_finished_cb);

        // Centering with scaling の際にマージンを加える
        CheckBox marginAddedCheckBox = new CheckBox("Add centering margin");
        marginAddedCheckBox.setSelected(panel.isMarginAdded());
        marginAddedCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            panel.setMarginAdded(newValue);
        });
        checkboxPanel.getChildren().add(marginAddedCheckBox);

        // ロケーションボタン

        centerinButton.setOnAction(e -> {
            panel.centering(false, panel.getWidth(), panel.getHeight());
        });

        centerinWithScalingButton.setOnAction(e -> {
            panel.centering(true, panel.getWidth(), panel.getHeight());
        });

        FlowPane locationPanel = new FlowPane(12, 0);
        locationPanel.setPadding(new Insets(0, 0, 8, 0));
        locationPanel.setAlignment(Pos.CENTER);
        locationPanel.getChildren().addAll(centerinButton, centerinWithScalingButton);

        BorderPane viewPane = new BorderPane();
        viewPane.setPadding(new Insets(8));
        viewPane.setCenter(checkboxPanel);
        viewPane.setBottom(locationPanel);

        return viewPane;
    }

    /**
     * Camera タブを作成する
     */
    private Pane createCameraPane() {
        // Replay ON/OFF

        if (cameraworkFile != null) {
            File file = new File(cameraworkFile);
            replayCheckBox.setText("Replay - " + file.getName());
            replayCheckBox.setSelected(true);
            Platform.runLater(() -> {
                setViewpointChangeInhibited(true);
            });
        } else {
            replayCheckBox.setText("Replay");
            replayCheckBox.setDisable(true);
        }
        replayCheckBox.selectedProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue) {
                forceUpdateCamerawork = true;
                setViewpointChangeInhibited(true);
            } else {
                setViewpointChangeInhibited(false);
            }
        });

        FlowPane replayPanel = new FlowPane();
        replayPanel.setPadding(new Insets(8, 0, 8, 4));
        replayPanel.getChildren().add(replayCheckBox);

        // カメラワークデータ表示

        cameraworkArea.setEditable(false);
        cameraworkArea.setWrapText(true);
        if (cameraworkFile != null) {
            for (CameraShot cameraShot : camerawork) {
                cameraworkArea.appendText(cameraShot.toString());
                cameraworkArea.appendText("\n");
            }
        }

        // コントロールボタン

        recordButton.setTooltip(new Tooltip("Add current camera position to camerawork list"));
        recordButton.setOnAction(e -> {
            recordCamerawork();
        });

        loadCameraworkButton.setTooltip(new Tooltip("Load camerawork list."));
        loadCameraworkButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Camerawork File");
            // TODO: fileChooser.setInitialDirectory(プロパティファイルのディレクトリ);
            fileChooser.getExtensionFilters().addAll(
                    new ExtensionFilter("JSON Files", "*.json"),
                    new ExtensionFilter("All Files", "*.*"));
            File file = fileChooser.showOpenDialog(frame);
            if (file == null) {
                return;
            }
            try {
                loadCamerawork(file.getCanonicalPath());
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
            // TODO: 相対ディレクトリパスを付加する
            replayCheckBox.setText("Replay - " + file.getName());
            replayCheckBox.setDisable(false);
            cameraworkArea.setText("");
            for (CameraShot cameraShot : camerawork) {
                cameraworkArea.appendText(cameraShot.toString());
                cameraworkArea.appendText("\n");
            }
        });

        saveCameraworkButton.setTooltip(new Tooltip("Save current camerawork list."));
        saveCameraworkButton.setOnAction(e -> {
            saveCamerawork();
        });

        FlowPane controlButtonPanel = new FlowPane(12, 0);
        controlButtonPanel.setPadding(new Insets(8, 0, 8, 0));
        controlButtonPanel.setAlignment(Pos.CENTER);
        controlButtonPanel.getChildren().addAll(recordButton, loadCameraworkButton, saveCameraworkButton);

        BorderPane cameraPane = new BorderPane();
        cameraPane.setPadding(new Insets(8));
        cameraPane.setTop(replayPanel);
        cameraPane.setCenter(cameraworkArea);
        cameraPane.setBottom(controlButtonPanel);

        return cameraPane;
    }

    /**
     * Status タブを作成する
     */
    private Pane createStatusPane() {
        // 調査対象選択ラジオボタンパネル

        ToggleGroup group = new ToggleGroup();

        RadioButton noEffectRb = new RadioButton("None");
        noEffectRb.setSelected(true);
        noEffectRb.setToggleGroup(group);
        noEffectRb.setOnAction(e -> {
            panel.hoverOff();
            panel.setHoverMode("None");
        });

        RadioButton nodeRb = new RadioButton("Node");
        nodeRb.setSelected(false);
        nodeRb.setToggleGroup(group);
        nodeRb.setOnAction(e -> {
            panel.hoverOff();
            panel.setHoverMode("Node");
        });

        RadioButton linkRb = new RadioButton("Link");
        linkRb.setSelected(false);
        linkRb.setToggleGroup(group);
        linkRb.setOnAction(e -> {
            panel.hoverOff();
            panel.setHoverMode("Link");
        });

        RadioButton areaRb = new RadioButton("Area");
        areaRb.setSelected(false);
        areaRb.setToggleGroup(group);
        areaRb.setOnAction(e -> {
            panel.hoverOff();
            panel.setHoverMode("Area");
        });
        areaRb.setDisable(launcher.getPollutionFile() == null);

        RadioButton agentRb = new RadioButton("Agent");
        agentRb.setSelected(false);
        agentRb.setToggleGroup(group);
        agentRb.setOnAction(e -> {
            panel.hoverOff();
            panel.setHoverMode("Agent");
        });

        FlowPane targetPanel = new FlowPane(12, 0);
        targetPanel.setPadding(new Insets(8, 0, 8, 0));
        targetPanel.setAlignment(Pos.CENTER);
        targetPanel.getChildren().addAll(new Label("Mode: "), noEffectRb, nodeRb, linkRb, areaRb, agentRb);

        Label attentionLabel = new Label("Attention: the display gets blurred when \"Link\" is selected.");
        attentionLabel.setTextFill(Color.RED);

        FlowPane labelPanel = new FlowPane();
        labelPanel.setPadding(new Insets(0, 0, 8, 0));
        labelPanel.setAlignment(Pos.CENTER);
        labelPanel.getChildren().add(attentionLabel);

        VBox _targetPanel = new VBox();
        _targetPanel.getChildren().addAll(targetPanel, labelPanel);

        // 情報表示エリア

        indicationArea.setEditable(false);
        indicationArea.setWrapText(true);

        BorderPane statusPane = new BorderPane();
        statusPane.setPadding(new Insets(8, 8, 16, 8));
        statusPane.setTop(_targetPanel);
        statusPane.setCenter(indicationArea);

        return statusPane;
    }

    /**
     * シミュレーションパネルでピッキング中のオブジェクトがクリックされた
     */
    public void update(Observable o, Object arg) {
        if (arg == null) {
            System.err.println("Warning: null received");
            return;
        }
        OBNode obNode = (OBNode)arg;
        String text = "";
        NType type = obNode.getNodeType();
        switch (type) {
        case NODE:
            text = getNodeInformation((MapNode)obNode);
            break;
        case LINK:
            text = getLinkInformation((MapLink)obNode);
            break;
        case AGENT:
            text = getAgentInformation((AgentBase)obNode);
            break;
        case AREA:
            text = getAreaInformation((MapAreaRectangle)obNode);
            break;
        default:
            System.err.println("Illegal type object: " + obNode.toString());
            break;
        }
        indicationArea.setText(text);
        Platform.runLater(() -> {
            tabPane.getSelectionModel().select(statusTab);
        });
    }

    /**
     * a shot of camerawork
     */
    public class CameraShot {
        public double time;
        public double agentSize;
        public Point3D translate;
        public Point3D pivot;
        public double rotateX;
        public double rotateZ;
        public double zoom;
        public double[] matrix = new double[16];

        public CameraShot(double time, double agentSize, Point3D translate, Point3D pivot, double rotateX, double rotateZ, double zoom) {
            this.time = time;
            this.agentSize = agentSize;
            this.translate = new Point3D(translate.getX(), translate.getY(), translate.getZ());
            this.pivot = new Point3D(pivot.getX(), pivot.getY(), pivot.getZ());
            this.rotateX = rotateX;
            this.rotateZ = rotateZ;
            this.zoom = zoom;
        }

        public CameraShot(double time, double agentSize, Translate translate, Point3D pivot, double rotateX, double rotateZ, double zoom) {
            this.time = time;
            this.agentSize = agentSize;
            this.translate = new Point3D(translate.getX(), translate.getY(), translate.getZ());
            this.pivot = new Point3D(pivot.getX(), pivot.getY(), pivot.getZ());
            this.rotateX = rotateX;
            this.rotateZ = rotateZ;
            this.zoom = zoom;
        }

        public CameraShot(Map<String, Object> values) {
            time = getDouble(values, "time", 0.0);
            agentSize = getDouble(values, "agentSize", launcher.getAgentSize());
            translate = new Point3D(getDouble(values, "translateX", 0.0), getDouble(values, "translateY", 0.0), getDouble(values, "translateZ", 0.0));
            pivot = new Point3D(getDouble(values, "pivotX", 0.0), getDouble(values, "pivotY", 0.0), getDouble(values, "pivotZ", 0.0));
            rotateX = getDouble(values, "rotateX", 0.0);
            rotateZ = getDouble(values, "rotateZ", 0.0);
            zoom = getDouble(values, "zoom", 1.0);
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null || ! (obj instanceof CameraShot)) {
                return false;
            }
            return obj.hashCode() == this.hashCode();
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public double getDouble(Map<String, Object> values, String key, double defaultValue) {
            Object object = values.get(key);
            if (object != null) {
                return Double.parseDouble(object.toString());
            }
            return defaultValue;
        }

        public Map<String, Object> getMapObject() {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("time", new Double(time));
            map.put("agentSize", new Double(agentSize));
            map.put("translateX", new Double(translate.getX()));
            map.put("translateY", new Double(translate.getY()));
            map.put("translateZ", new Double(translate.getZ()));
            map.put("pivotX", new Double(pivot.getX()));
            map.put("pivotY", new Double(pivot.getY()));
            map.put("pivotZ", new Double(pivot.getZ()));
            map.put("rotateX", new Double(rotateX));
            map.put("rotateZ", new Double(rotateZ));
            map.put("zoom", new Double(zoom));
            return map;
        }

        public String toString() {
            StringBuilder buff = new StringBuilder();
            buff.append("time: ").append(time).append(", ");
            buff.append("agent_size: ").append(agentSize).append(", ");
            buff.append("translate: (").append(translate.getX()).append(", ").append(translate.getY()).append(", ").append(translate.getZ()).append("), ");
            buff.append("pivot: (").append(pivot.getX()).append(", ").append(pivot.getY()).append(", ").append(pivot.getZ()).append("), ");
            buff.append("rotateX: ").append(rotateX).append(", ");
            buff.append("rotateZ: ").append(rotateZ).append(", ");
            buff.append("zoom: ").append(zoom);
            return buff.toString();
        }
    }

    /**
     * カメラワークの記録
     */
    public void recordCamerawork() {
        double currentTime = launcher.getSimulator().currentTime.getRelativeTime();
        int lastIndex = camerawork.size() - 1;
        if (lastIndex >= 0 && camerawork.get(lastIndex).time >= currentTime) {
            Alert alert = new Alert(AlertType.NONE, "Invalid operation", ButtonType.CLOSE);
            alert.setTitle("Warning");
            alert.showAndWait();
            return;
        }
        CameraShot cameraShot = new CameraShot(currentTime, getAgentSize(), panel.getTranslate(), panel.getPivotOfScale(), panel.getRotateX(), panel.getRotateZ(), panel.getZoomScale());
        camerawork.add(cameraShot);
        cameraworkArea.appendText(cameraShot.toString());
        cameraworkArea.appendText("\n");
    }

    /**
     * カメラワークデータを読み込む
     */
    public void loadCamerawork(String filePath) {
        File file = new File(filePath);
        if (! file.exists()) {
            Itk.logError_("Camerawork file does not exist", filePath);
            System.exit(1);
        }

        Itk.logInfo("Load camerawork file", filePath);
        try {
            JSON json = new JSON(JSON.Mode.TRADITIONAL);
            ArrayList<Map<String, Object>> jsonObject = json.parse(new FileReader(filePath));
            camerawork.clear();
            for (Map<String, Object> object : jsonObject) {
                camerawork.add(new CameraShot(object));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        cameraworkFile = filePath;
    }

    /**
     * カメラワークデータを保存する
     */
    public void saveCamerawork() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save camera position list");
        // TODO: fileChooser.setInitialDirectory(プロパティファイルのディレクトリ);
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("JSON Files", "*.json"),
                new ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showSaveDialog(frame);
        if (file == null) {
            return;
        }
        String filePath = null;
        try {
            filePath = file.getCanonicalPath();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        Itk.logInfo("Save camerawork file", filePath);
        ArrayList<Map<String, Object>> jsonObject = new ArrayList<Map<String, Object>>();
        for (CameraShot cameraShot : camerawork) {
            jsonObject.add(cameraShot.getMapObject());
        }
        try {
            JSON.encode(jsonObject, new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * カメラワークを更新する
     */
    public void updateCamerawork(SimTime updateTime) {
        if (replayCheckBox.isSelected() && camerawork.size() > 0) {
            double time = updateTime.getRelativeTime();
            CameraShot last_camera = null;
            CameraShot next_camera = null;
            for (CameraShot camera : camerawork) {
                if (camera.time > time) {
                    next_camera = camera;
                    break;
                }
                last_camera = camera;
            }
            // 最初の CameraShot の time が 0.0 ではなく、まだその time に達していない場合
            if (last_camera == null) {
                if (next_camera.agentSize != getAgentSize()) {
                    agent_size_control.setValue(next_camera.agentSize);
                }
                panel.updateTransform(next_camera.translate, next_camera.pivot, next_camera.rotateX, next_camera.rotateZ, next_camera.zoom);
            }
            // 最後の CameraShot とそれ以降
            else if (next_camera == null) {
                if (last_camera.time < time && ! forceUpdateCamerawork) {
                    return;
                }
                if (last_camera.agentSize != getAgentSize()) {
                    agent_size_control.setValue(last_camera.agentSize);
                }
                panel.updateTransform(last_camera.translate, last_camera.pivot, last_camera.rotateX, last_camera.rotateZ, last_camera.zoom);
            }
            // 上の条件以外
            else {
                if (time > last_camera.time && ! forceUpdateCamerawork) {
                    double nextCameraTime = next_camera.time;   // 一時退避
                    next_camera.time = last_camera.time;
                    if (last_camera.equals(next_camera)) {
                        next_camera.time = nextCameraTime;
                        return;
                    }
                    next_camera.time = nextCameraTime;
                }
                double ratio = (time - last_camera.time) / (next_camera.time - last_camera.time);

                double agent_size = (next_camera.agentSize - last_camera.agentSize) * ratio + last_camera.agentSize;
                if (agent_size != getAgentSize()) {
                    agent_size_control.setValue(agent_size);
                }

                Point3D translate = next_camera.translate.subtract(last_camera.translate).multiply(ratio).add(last_camera.translate);
                Point3D pivot = next_camera.pivot.subtract(last_camera.pivot).multiply(ratio).add(last_camera.pivot);
                double rotateX = (next_camera.rotateX - last_camera.rotateX) * ratio + last_camera.rotateX;
                double rotateZ = (next_camera.rotateZ - last_camera.rotateZ) * ratio + last_camera.rotateZ;
                double zoom_scale = (next_camera.zoom - last_camera.zoom) * ratio + last_camera.zoom;
                panel.updateTransform(translate, pivot, rotateX, rotateZ, zoom_scale);
            }
            forceUpdateCamerawork = false;
        }
    }

    /**
     * マウス操作による視点移動とズーム機能の禁止/解除.
     *
     * true で禁止
     */
    public void setViewpointChangeInhibited(boolean b) {
        if (b) {
            viewpointChangeInhibited = true;
            agent_size_control.setDisable(true);
            centerinButton.setDisable(true);
            centerinWithScalingButton.setDisable(true);
            recordButton.setDisable(true);
            loadCameraworkButton.setDisable(true);
            saveCameraworkButton.setDisable(true);
            panel.setViewPointOperationEnabled(false);
        } else {
            viewpointChangeInhibited = false;
            agent_size_control.setDisable(false);
            centerinButton.setDisable(false);
            centerinWithScalingButton.setDisable(false);
            recordButton.setDisable(false);
            loadCameraworkButton.setDisable(false);
            saveCameraworkButton.setDisable(false);
            panel.setViewPointOperationEnabled(true);
        }
    }

    /**
     * ノード情報テキスト(情報表示エリア用)
     */
    public static String getNodeInformation(MapNode node) {
        StringBuilder buff = new StringBuilder();
        buff.append("Node ID: ").append(node.ID).append("\n");
        buff.append("x: ").append(node.getX()).append("\n");
        buff.append("y: ").append(node.getY()).append("\n");
        buff.append("height: ").append(node.getHeight()).append("\n");
        buff.append("tags: ").append(node.getTagString()).append("\n");
        HashMap<String, NavigationHint> hints
            = node.getHints(NavigationHint.DefaultMentalMode) ;
        if (! hints.isEmpty()) {
            buff.append("---- Navigation hints ----\n");
            ArrayList<String> hintKeys = new ArrayList(hints.keySet());
            Collections.sort(hintKeys);
            for (String key : hintKeys) {
                NavigationHint hint = hints.get(key);
                buff.append("key: ").append(key).append("\n");
                if (hint.toNode == null) {
                    buff.append("    toNode: null\n");
                } else {
                    buff.append("    toNode: ").append(hint.toNode.ID).append("(").append(hint.toNode.getTagString()).append(")\n");
                }
                if (hint.viaLink == null) {
                    buff.append("    viaLink: null\n");
                } else {
                    buff.append("    viaLink: ").append(hint.viaLink.ID).append("\n");
                }
                buff.append("    distance: ").append(hint.distance).append("\n");
            }
        }
        return buff.toString();
    }

    /**
     * リンク情報テキスト(情報表示エリア用)
     */
    public static String getLinkInformation(MapLink link) {
        StringBuilder buff = new StringBuilder();
        buff.append("Link ID: ").append(link.ID).append("\n");
        buff.append("length: ").append(link.getLength()).append("\n");
        buff.append("width: ").append(link.getWidth()).append("\n");
        buff.append("laneWidth(Forward): ").append(link.getLaneWidth(Direction.Forward)).append("\n");
        buff.append("laneWidth(Backward): ").append(link.getLaneWidth(Direction.Backward)).append("\n");
        buff.append("tags: ").append(link.getTagString()).append("\n");
        buff.append("agents: ").append(link.getAgents().size()).append("\n");
        MapNode fromNode = link.getFrom();
        if (fromNode == null) {
            buff.append("from Node: null\n");
        } else {
            buff.append("from Node:").append("\n");
            buff.append("    Node ID: ").append(fromNode.ID).append("\n");
            buff.append("    x: ").append(fromNode.getX()).append("\n");
            buff.append("    y: ").append(fromNode.getY()).append("\n");
            buff.append("    height: ").append(fromNode.getHeight()).append("\n");
            buff.append("    tags: ").append(fromNode.getTagString()).append("\n");
        }
        MapNode toNode = link.getTo();
        if (toNode == null) {
            buff.append("to Node: null\n");
        } else {
            buff.append("to Node:").append("\n");
            buff.append("    Node ID: ").append(toNode.ID).append("\n");
            buff.append("    x: ").append(toNode.getX()).append("\n");
            buff.append("    y: ").append(toNode.getY()).append("\n");
            buff.append("    height: ").append(toNode.getHeight()).append("\n");
            buff.append("    tags: ").append(toNode.getTagString()).append("\n");
        }
        return buff.toString();
    }

    /**
     * マップエリア情報テキスト(情報表示エリア用)
     */
    public static String getAreaInformation(MapAreaRectangle area) {
        java.awt.geom.Rectangle2D bounds = (java.awt.geom.Rectangle2D)area.getShape();
        PollutionLevelInfo pollutionLevel = area.getPollutionLevel();
        StringBuilder buff = new StringBuilder();
        buff.append("Area ID: ").append(area.ID).append("\n");
        buff.append("pWestX: ").append(bounds.getMinX()).append("\n");
        buff.append("pEastX: ").append(bounds.getMaxX()).append("\n");
        buff.append("pNorthY: ").append(bounds.getMinY()).append("\n");
        buff.append("pSouthY: ").append(bounds.getMaxY()).append("\n");
        buff.append("current level: ").append(pollutionLevel.getCurrentLevel()).append("\n");
        buff.append("normalized level: ").append(pollutionLevel.getNormalizedLevel()).append("\n");
        buff.append("tags: ").append(area.getTagString()).append("\n");
        return buff.toString();
    }

    /**
     * エージェント情報テキスト(情報表示エリア用)
     */
    public static String getAgentInformation(AgentBase agent) {
        StringBuilder buff = new StringBuilder();
        buff.append("Agent ID: ").append(agent.ID).append("\n");
        buff.append("config: ").append(agent.getConfigLine()).append("\n");
        buff.append("type: ").append(agent.getClass().getSimpleName()).append("\n");
        buff.append("goal: ").append(agent.getGoal()).append("\n");
        buff.append("generated time: ").append(agent.generatedTime.getAbsoluteTimeString()).append("\n");
        buff.append("position X: ").append(agent.getPos().getX()).append("\n");
        buff.append("position Y: ").append(agent.getPos().getY()).append("\n");
        buff.append("position Z: ").append(agent.getHeight()).append("\n");
        buff.append("drawing position X: ").append(agent.getPos().getX() + agent.getSwing().getX()).append("\n");
        buff.append("drawing position Y: ").append(agent.getPos().getY() + agent.getSwing().getY()).append("\n");
        buff.append("drawing position Z: ").append(
            agent.getHeight() / ((MapPartGroup)agent.getCurrentLink().getParent()).getScale()
        ).append("\n");
        buff.append("velocity: ").append(agent.getSpeed()).append("\n");
        buff.append("acceleration: ").append(agent.getAcceleration()).append("\n");
        buff.append("previous node: ").append(agent.getPrevNode().ID).append("\n");
        buff.append("next node: ").append(agent.getNextNode().ID).append("\n");
        buff.append("current link: ").append(agent.getCurrentLink().ID).append("\n");
        buff.append("advancing distance: ").append(agent.getAdvancingDistance()).append("\n");
        buff.append("direction: ").append(agent.isForwardDirection() ? "Forward" : "Backward").append("\n");
        buff.append("waiting: ").append(agent.isWaiting()).append("\n");
        buff.append("current exposure: ").append(agent.obstructer.currentValueForLog()).append("\n");
        buff.append("amount exposure: ").append(agent.obstructer.accumulatedValueForLog()).append("\n");
        buff.append("triage: ").append(agent.getTriageName()).append("\n");
        return buff.toString();
    }

    /**
     * シミュレーションパネルのスクリーンショットを撮ってファイルに保存する
     */
    public void captureScreenShot(final String path, final String imageType) {
        WritableImage image = panel.snapshot(null, null);
        final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        Thread thread = new Thread(new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                incSaveThreadCount();
                try {
                    ImageIO.write(bufferedImage, imageType, new File(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                decSaveThreadCount();
                return true; 
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * ステータスバーに表示するテキストをセットする
     */
    public void setStatusText(String text) {
        status.setText(text);
    }

    /**
     * シミュレーションパネルを返す
     */
    public SimulationPanel3D getSimulationPanel() {
        return panel;
    }

    /**
     * エージェントの表示サイズ(m)を返す
     */
    public double getAgentSize() {
        return agent_size_control.getValue();
    }

    /**
     * 表示の更新が完了するのを待って次のステップに進むかどうかを返す
     */
    public boolean isViewSynchronized() {
        return viewSynchronizedCheckBox.isSelected();
    }
}
