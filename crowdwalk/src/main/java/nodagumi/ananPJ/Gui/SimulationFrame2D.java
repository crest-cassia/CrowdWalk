// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.AdjustmentListener ;
import java.awt.event.AdjustmentEvent ;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;

import math.geom3d.Vector3D;
import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.CrowdWalkLauncher;
import nodagumi.ananPJ.GuiSimulationLauncher2D;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.ananPJ.Simulator.PollutionHandler.*;
import nodagumi.Itk.*;

/**
 * 2D シミュレーションウィンドウ
 */
public class SimulationFrame2D extends JFrame
    implements MouseListener, MouseWheelListener, MouseMotionListener {
    /**
     * ヘルプ表示用コンテンツのアドレス
     */
    public static final String HTML_TEMPLATE = "/doc/template.html";
    public static final String QUICK_REFERENCE = "/doc/quick_reference_simulator_2d.md";

    /**
     * 実在の地図ベースのマップであると判断する最小リンク数
     */
    public static final int MINIMUM_REAL_MAP_LINKS = 300;

    /**
     * ステータス表示位置: 画面の上端
     */
    public static final int TOP = 1;

    /**
     * ステータス表示位置: 画面の下端
     */
    public static final int BOTTOM = 2;

    /**
     * シミュレーションウィンドウのフレーム
     */
    private SimulationFrame2D frame;

    /**
     * シミュレーションパネル
     */
    public SimulationPanel2D panel;

    /**
     * ランチャー
     */
    private GuiSimulationLauncher2D launcher;

    /**
     * 現在マップ上に存在しているエージェント
     */
    private ArrayList<AgentBase> walkingAgents = new ArrayList();

    /**
     * 領域
     */
    private ArrayList<MapArea> areas;

    /**
     * カメラワーク
     */
    private ArrayList<CameraShot> camerawork = new ArrayList<CameraShot>();

    /**
     * 一時停止させる時刻
     */
    private SimTime pauseTime = new SimTime();

    /* Properties */

    private double agent_size = 5.0;
    private boolean recordSimulationScreen = false;
    private String screenshotDir = "screenshots";
    private boolean clearScreenshotDir = false;
    private String screenshotImageType = "png";
    private boolean changeAgentColorDependingOnSpeed = true;
    private boolean drawingAgentByTriageAndSpeedOrder = true;
    private boolean showStatus = false;
    private int messagePosition = TOP;
    private boolean showLogo = false;
    private boolean exitWithSimulationFinished = false;
    private boolean marginAdded = false;
    private boolean showBackgroundImage = false;
    private boolean showBackgroundMap = false;
    private boolean showTheSea = false;
    private boolean polygonShowing = false;

    /* メニュー構成変数 */

    private JCheckBoxMenuItem showNodes = null;
    private JCheckBoxMenuItem showNodeLabels = null;
    private JCheckBoxMenuItem showLinks = null;
    private JCheckBoxMenuItem showLinkLabels = null;
    private JCheckBoxMenuItem showArea = null;
    private JCheckBoxMenuItem showAreaLabels = null;

    /**
     * グループ別の背景画像表示メニュー
     */
    private HashMap<MapPartGroup, JCheckBoxMenuItem> backgroundImageMenus = new HashMap();

    /* ホバー表示対象 */

    private MapNode hoverNode = null;
    private MapLink hoverLink = null;
    private MapArea hoverArea = null;
    private AgentBase hoverAgent = null;

    /* コントロールボタンパネル構成変数 */

    private transient JToggleButton start_button = null;
    private transient JButton step_button = null;
    private transient JLabel simulationDeferFactorValue;
    private int deferFactor = 0;

    /* ステータスバー構成変数 */

    private JLabel status = null;

    /* Control タブ構成変数 */

    private transient JLabel clock_label = new JLabel("00:00:00");
    private transient JLabel time_label = new JLabel("NOT STARTED");
    private transient JLabel evacuatedCount_label = new JLabel("NOT STARTED");
    private JSpinner intSpinner = null;
    private JSpinner timeSpinner = null;
    private JButton resetButton = new JButton("Reset");

    /* View タブ構成変数 */

    private boolean showAreaLocation = false;
    private JScrollBar agent_size_control = null;
    private JLabel agent_size_value;
    private JCheckBox showNodesCheckBox = null;
    private JCheckBox showNodeLabelsCheckBox = null;
    private JCheckBox showLinksCheckBox = null;
    private JCheckBox showLinkLabelsCheckBox = null;
    private JCheckBox showAreaCheckBox = null;
    private JCheckBox showAreaLabelsCheckBox = null;
    private JCheckBox showAgentCheckBox = null;
    private JCheckBox showAgentLabelsCheckBox = null;
    private JCheckBox record_snapshots = new JCheckBox("Record simulation screen");
    private JCheckBox exit_with_simulation_finished_cb = null;

    /* Camera タブ構成変数 */

    private String cameraworkFile = null;
    private boolean forceUpdateCamerawork = true;
    private boolean viewpointChangeInhibited = false;
    private JCheckBox replayCheckBox = new JCheckBox();
    private transient JTextArea cameraworkArea = new JTextArea();
    private JButton recordButton = new JButton("Record");
    private JButton loadCameraworkButton = new JButton("Load");
    private JButton saveCameraworkButton = new JButton("Save as");

    /* Status タブ構成変数 */

    public enum StatusMode {
        NO_EFFECT, NODE, LINK, AREA, AGENT
    };
    private StatusMode mode = StatusMode.NO_EFFECT;
    private transient JTextArea indicationArea = new JTextArea("Information");

    /* マウス操作情報 */

    private boolean scrolling = false;
    private int dragStartX = 0;
    private int dragStartY = 0;

    /**
     * ヘルプ表示用
     */
    private Parser parser;
    private HtmlRenderer renderer;
    private Stage helpStage;
    private WebView webView;
    private double helpZoom = 1.0;

    /* スクリーンショット保存用スレッド数を管理するためのカウンタ制御 */

    private int saveThreadCount = 0;
    synchronized public int getSaveThreadCount() { return saveThreadCount; }
    synchronized public void incSaveThreadCount() { saveThreadCount++; }
    synchronized public void decSaveThreadCount() { saveThreadCount--; }

    /**
     * 1ショットぶんのカメラワーク
     */
    public class CameraShot {
        public double time;
        public double zoom;
        public double angle;
        public double agentSize;
        public Point2D position;

        public CameraShot(double time, double zoom, double angle, double agentSize, double x, double y) {
            this.time = time;
            this.zoom = zoom;
            this.angle = angle;
            this.agentSize = agentSize;
            position = new Point2D.Double(x, y);
        }

        public CameraShot(double time, double zoom, double angle, double agentSize, Point2D position) {
            this(time, zoom, angle, agentSize, position.getX(), position.getY());
        }

        public CameraShot(Map<String, Object> values) {
            time = getDoubleValue(values, "time", 0.0);
            zoom = getDoubleValue(values, "zoom", 1.0);
            angle = getDoubleValue(values, "angle", 0.0);
            agentSize = getDoubleValue(values, "agentSize", 5.0);
            position = new Point2D.Double(getDoubleValue(values, "x", 0.0), getDoubleValue(values, "y", 0.0));
        }

        public double getDoubleValue(Map<String, Object> values, String key, double defaultValue) {
            Object object = values.get(key);
            if (object != null) {
                return Double.parseDouble(object.toString());
            }
            return defaultValue;
        }

        public Map<String, Object> getMapObject() {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("time", Double.valueOf(time));
            map.put("zoom", Double.valueOf(zoom));
            map.put("angle", Double.valueOf(angle));
            map.put("agentSize", Double.valueOf(agentSize));
            map.put("x", Double.valueOf(position.getX()));
            map.put("y", Double.valueOf(position.getY()));
            return map;
        }

        public String toString() {
            return "time: " + time + ", zoom: " + zoom + ", angle: " + angle + ", agent_size: " + agentSize + ", x: " + position.getX() + ", y: " + position.getY();
        }
    }

    public SimulationFrame2D(String title, int simulationPanelWidth, int simulationPanelHeight,
            GuiSimulationLauncher2D launcher, CrowdWalkPropertiesHandler properties,
            ArrayList<GsiTile> backgroundMapTiles) {
        super(title);

        frame = this;
        this.launcher = launcher;
        if (properties.getPropertiesFile() != null) {
            launcher.setGuiValues(this);
        }
        areas = launcher.getMap().getAreas();
        // 実在の地図ベースのマップでない時はセンタリングマージンを付加する
        setMarginAdded(getLinks().size() < MINIMUM_REAL_MAP_LINKS);

        JFXPanel fxPanel = new JFXPanel();  // Platform.runLater() を有効化するために必要
        Platform.runLater(() -> initHelp());
        setupMenu();
        setupContents(simulationPanelWidth, simulationPanelHeight, properties, backgroundMapTiles);
        if (cameraworkFile != null) {
            setViewpointChangeInhibited(true);
        }

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(new Runnable (){
                        @Override
                        public void run() {
                            launcher.simulationWindowOpenedOperation(frame) ;
                        }});
                // Java 9 ではウィンドウが 8x8 縮小されるためスクロールバーがつぶれてしまう不具合の対策
                if (panel.getWidth() < 800 || panel.getHeight() < 600) {
                    int dw = Math.max(800 - panel.getWidth(), 0);
                    int dh = Math.max(600 - panel.getHeight(), 0);
                    setSize(getWidth() + dw, getHeight() + dh);
                }
                panel.centering(true);
                panel.repaint();
            }
            public void windowClosing(WindowEvent e) {
                if (launcher.isRunning()) {
                    launcher.pause();
                }
                launcher.saveSimulatorPosition(frame);
                frame.dispose();
            }
            public void windowClosed(WindowEvent e) {
                Platform.runLater(() -> helpStage.close());
                launcher.quit();
            }
        });

        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                try {
                    launcher.saveSimulatorPosition(frame);
                } catch(IllegalComponentStateException ex) {}
            }
            public void componentResized(ComponentEvent e) {
            }
        });

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        pack();
    }

    /**
     * ヘルプ画面の準備
     */
    private void initHelp() {
        helpStage = new Stage();

        MutableDataSet options = new MutableDataSet();
        options.setFrom(ParserEmulationProfile.MARKDOWN);
        options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create()
        ));
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        webView = new WebView();
        webView.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                Platform.runLater(() -> {
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
                });
            }
        });

        Button okButton = new Button("  OK  ");
        okButton.setOnAction(event -> Platform.runLater(() -> helpStage.close()));
        BorderPane buttonPane = new BorderPane();
        buttonPane.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));
        buttonPane.setRight(okButton);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(webView);
        borderPane.setBottom(buttonPane);

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().add("stylesheet.css");
        helpStage.setScene(scene);
    }

    /**
     * メニューの配置
     */
    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        //// File menu ////

        JMenu fileMenu = new JMenu("File");

        JMenuItem mi = new JMenuItem("Close");
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (launcher.isRunning()) {
                    launcher.pause();
                }
                launcher.saveSimulatorPosition(frame);
                frame.dispose();
            }
        });
        fileMenu.add(mi);
        
        menuBar.add(fileMenu);

        //// View menu ////

        JMenu viewMenu = new JMenu("View");

        mi = new JMenuItem("Centering");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                panel.centering(false);
                panel.repaint();
            }
        });
        viewMenu.add(mi);

        mi = new JMenuItem("Centering with scaling");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                panel.centering(true);
                panel.repaint();
            }
        });
        viewMenu.add(mi);
        
        mi = new JMenuItem("To the origin");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                panel.setPosition(0.0, 0.0);
                panel.repaint();
            }
        });
        viewMenu.add(mi);

        viewMenu.addSeparator();

        showNodes = new JCheckBoxMenuItem("Show nodes");
        showNodes.setSelected(true);
        showNodes.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                showNodesCheckBox.setSelected(showNodes.isSelected());
                showNodeLabelsCheckBox.setEnabled(showNodes.isSelected());
                update();
            }
        });
        viewMenu.add(showNodes);

        showNodeLabels = new JCheckBoxMenuItem(" (Show node labels)");
        showNodeLabels.setSelected(false);
        showNodeLabels.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                showNodeLabelsCheckBox.setSelected(showNodeLabels.isSelected());
                update();
            }
        });
        viewMenu.add(showNodeLabels);

        showLinks = new JCheckBoxMenuItem("Show links");
        showLinks.setSelected(true);
        showLinks.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                showLinksCheckBox.setSelected(showLinks.isSelected());
                showLinkLabelsCheckBox.setEnabled(showLinks.isSelected());
                update();
            }
        });
        viewMenu.add(showLinks);

        showLinkLabels = new JCheckBoxMenuItem(" (Show link labels)");
        showLinkLabels.setSelected(false);
        showLinkLabels.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                showLinkLabelsCheckBox.setSelected(showLinkLabels.isSelected());
                update();
            }
        });
        viewMenu.add(showLinkLabels);
        
        showArea = new JCheckBoxMenuItem("Show area");
        showArea.setSelected(true);
        showArea.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                showAreaCheckBox.setSelected(showArea.isSelected());
                showAreaLabelsCheckBox.setEnabled(showArea.isSelected());
                update();
            }
        });
        viewMenu.add(showArea);

        showAreaLabels = new JCheckBoxMenuItem(" (Show area labels)");
        showAreaLabels.setSelected(false);
        showAreaLabels.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                showAreaLabelsCheckBox.setSelected(showAreaLabels.isSelected());
                update();
            }
        });
        viewMenu.add(showAreaLabels);
        
        JMenu showBackgroundImage = new JMenu("Show background image");
        for (MapPartGroup group : launcher.getMap().getGroups()) {
            String fileName = group.getImageFileName();
            if (fileName == null || fileName.isEmpty()) {
                continue;
            }
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(group.getTagString());
            menuItem.setSelected(true);
            menuItem.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    panel.repaint();
                }
            });
            showBackgroundImage.add(menuItem);
            backgroundImageMenus.put(group, menuItem);
        }
        viewMenu.add(showBackgroundImage);

        menuBar.add(viewMenu);

        //// Help menu ////

        JMenu helpMenu = new JMenu("Help");

        JMenuItem miQuickReference = new JMenuItem("Quick reference");
        miQuickReference.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Platform.runLater(() -> {
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
            }
        });
        helpMenu.add(miQuickReference);

        JMenuItem miVersion = new JMenuItem("About version");
        miVersion.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, CrowdWalkLauncher.getVersion(), "CrowdWalk Version", JOptionPane.PLAIN_MESSAGE);
            }
        });
        helpMenu.add(miVersion);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * 各表示モードの ON/OFF をシミュレーションパネルの変数に反映する
     */
    public void update() {
        panel.setShowNodes(showNodes.isSelected());
        showNodeLabels.setEnabled(showNodes.isSelected());
        panel.setShowNodeNames(showNodeLabels.isSelected());

        panel.setShowLinks(showLinks.isSelected());
        showLinkLabels.setEnabled(showLinks.isSelected());
        panel.setShowLinkNames(showLinkLabels.isSelected());
        panel.setShowArea(showArea.isSelected());
        showAreaLabels.setEnabled(showArea.isSelected());
        panel.setShowAreaNames(showAreaLabels.isSelected());
        panel.setShowAgents(showAgentCheckBox.isSelected());
        panel.setShowAgentNames(showAgentLabelsCheckBox.isSelected());
        clearSelection();
        repaint();
    }

    /**
     * パネルの配置
     */
    private void setupContents(int simulationPanelWidth, int simulationPanelHeight,
            CrowdWalkPropertiesHandler properties, ArrayList<GsiTile> backgroundMapTiles) {
        setLayout(new BorderLayout());
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(400, simulationPanelHeight));

        boolean backgroundImageEnabled = true;
        int backgroundImageCount = 0;
        for (MapPartGroup group : launcher.getMap().getGroups()) {
            String imageFileName = group.getImageFileName();
            if (imageFileName != null && ! imageFileName.isEmpty()) {
                backgroundImageCount++;
            }
        }
        if (backgroundImageCount == 0) {
            backgroundImageEnabled = false;
            showBackgroundImage = false;
        }

        boolean backgroundMapEnabled = true;
        if (backgroundMapTiles == null || backgroundMapTiles.isEmpty()) {
            backgroundMapEnabled = false;
            showBackgroundMap = false;
        }

        //// 2D シミュレーションパネル ////

        ArrayList<HashMap> linkAppearance = launcher.loadLinkAppearance();
        ArrayList<HashMap> nodeAppearance = launcher.loadNodeAppearance();
        panel = new SimulationPanel2D(this, launcher.getMap(), properties, backgroundMapTiles, linkAppearance, nodeAppearance);
        panel.setPreferredSize(new Dimension(simulationPanelWidth, simulationPanelHeight));
        panel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                String label = String.format("Record simulation screen (%d x %d)",
                        panel.getWidth(), panel.getHeight());
                record_snapshots.setText(label);
            }
        });
        add(panel, BorderLayout.CENTER);

        //// タブパネル ////

        JTabbedPane tabs = new JTabbedPane();
        tabs.add(createControlPanel());
        tabs.add(createViewPanel(backgroundImageEnabled, backgroundMapEnabled));
        tabs.add(createCameraPanel());
        tabs.add(createStatusPanel());
        rightPanel.add(tabs, BorderLayout.CENTER);

        //// コントロールボタンパネル ////

        JPanel controlButtonPanel = createControlButtonPanel();
        rightPanel.add(controlButtonPanel, BorderLayout.SOUTH);
        update_buttons();

        add(rightPanel, BorderLayout.EAST);

        //// ステータスバー ////

        status = new JLabel("NOT STARTED");
        status.setBorder(BorderFactory.createLoweredBevelBorder());
        add(status, BorderLayout.SOUTH);
    }

    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJComponent(JPanel panel, int x, int y, int width, int height, int anchor, int fill, Component comp) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.anchor = anchor;
        gbc.fill = fill;
        gbc.insets = new Insets(0, 12, 0, 12);
        ((GridBagLayout)panel.getLayout()).setConstraints(comp, gbc);
        panel.add(comp);
    }

    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJComponent(JPanel panel, int x, int y, int width, int height, int anchor, Component comp) {
        addJComponent(panel, x, y, width, height, anchor, GridBagConstraints.NONE, comp);
    }

    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJComponent(JPanel panel, int x, int y, int width, int height, Component comp) {
        addJComponent(panel, x, y, width, height, GridBagConstraints.WEST, comp);
    }

    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJLabel(JPanel panel, int x, int y, int width, int height, int anchor, String label) {
        addJComponent(panel, x, y, width, height, anchor, new JLabel(label));
    }

    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJLabel(JPanel panel, int x, int y, int width, int height, String label) {
        addJLabel(panel, x, y, width, height, GridBagConstraints.WEST, label);
    }

    /**
     * コントロールボタンパネルの生成
     */
    private JPanel createControlButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));

        ImageIcon start_icon = new ImageIcon(getClass().getResource("/img/start.png"));
        ImageIcon pause_icon = new ImageIcon(getClass().getResource("/img/pause.png"));
        ImageIcon step_icon = new ImageIcon(getClass().getResource("/img/step.png"));

        start_button = new JToggleButton(start_icon);
        start_button.setSelectedIcon(pause_icon);
        start_button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (start_button.isSelected()) {
                        resetButton.setEnabled(false);
                        launcher.start();
                    } else {
                        launcher.pause();
                        resetButton.setEnabled(true);
                    }
                    update_buttons();
                }});
        panel.add(start_button);

        step_button = new JButton(step_icon);
        step_button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (exit_with_simulation_finished_cb.isSelected()) {
                        // ボタンクリックでいきなりプログラムが終了することがないようにするため
                        exit_with_simulation_finished_cb.doClick();
                    }
                    launcher.step();
                    update_buttons();
                }});
        panel.add(step_button);

        panel.add(new JLabel("wait:"));
        JScrollBar deferFactorControl = new JScrollBar(JScrollBar.HORIZONTAL, deferFactor, 1, 0, 300);
        deferFactorControl.addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    setSimulationDeferFactor(deferFactorControl.getValue());
                    simulationDeferFactorValue.setText("" + deferFactor);
                }});
        deferFactorControl.setPreferredSize(new Dimension(150, 20));
        panel.add(deferFactorControl);
        simulationDeferFactorValue = new JLabel();
        simulationDeferFactorValue.setHorizontalAlignment(JLabel.RIGHT);
        simulationDeferFactorValue.setPreferredSize(new Dimension(30, 20));
        simulationDeferFactorValue.setText("" + deferFactor);
        panel.add(simulationDeferFactorValue);

        return panel;
    }

    /**
     * スタートボタンを返す
     */
    public JToggleButton getStartButton() {
        return start_button;
    }

    /**
     * ボタン類のアップデート
     */
    public void update_buttons() {
        if (step_button != null) {
            step_button.setEnabled(! launcher.isRunning());
        }
    }

    /**
     * シミュレーション遅延の制御（画面）
     */
    public void setSimulationDeferFactor(int deferFactor) {
        this.deferFactor = deferFactor;
        launcher.getSimulator().setSimulationDeferFactor(deferFactor);
    }

    /**
     * Control タブの生成
     */
    private JPanel createControlPanel() {
        JPanel control_panel = new JPanel();
        control_panel.setName("Control");
        control_panel.setLayout(new BorderLayout());

        JPanel top_panel = new JPanel();
        top_panel.setLayout(new BorderLayout());

        /* title & clock */
        JPanel titlepanel = new JPanel(new GridBagLayout());
        addJLabel(titlepanel, 0, 0, 1, 1, GridBagConstraints.EAST, "Properties");
        if (launcher.getPropertiesFile() != null) {
            File propertiesFile = new File(launcher.getPropertiesFile());
            addJLabel(titlepanel, 1, 0, 1, 1, propertiesFile.getName());
        } else {
            addJLabel(titlepanel, 1, 0, 1, 1, "No properties file");
        }

        addJLabel(titlepanel, 0, 1, 1, 1, GridBagConstraints.EAST, "Map");
        if (launcher.getNetworkMapFile() != null) {
            File map_file = new File(launcher.getNetworkMapFile());
            addJLabel(titlepanel, 1, 1, 1, 1, map_file.getName());
        } else {
            addJLabel(titlepanel, 1, 1, 1, 1, "No map file");
        }

        addJLabel(titlepanel, 0, 2, 1, 1, GridBagConstraints.EAST, "Generation");
        if (launcher.getGenerationFile() != null) {
            File generation_file = new File(launcher.getGenerationFile());
            addJLabel(titlepanel, 1, 2, 1, 1, generation_file.getName());
        } else {
            addJLabel(titlepanel, 1, 2, 1, 1, "No generation file");
        }

        addJLabel(titlepanel, 0, 3, 1, 1, GridBagConstraints.EAST, "Scenario");
        if (launcher.getScenarioFile() != null) {
            File scenario_file = new File(launcher.getScenarioFile());
            addJLabel(titlepanel, 1, 3, 1, 1, scenario_file.getName());
        } else {
            addJLabel(titlepanel, 1, 3, 1, 1, "No scenario file");
        }

        addJLabel(titlepanel, 0, 4, 1, 1, GridBagConstraints.EAST, "Pollution");
        if (launcher.getPollutionFile() != null) {
            File pollution_file = new File(launcher.getPollutionFile());
            addJLabel(titlepanel, 1, 4, 1, 1, pollution_file.getName());
        } else {
            addJLabel(titlepanel, 1, 4, 1, 1, "No pollution file");
        }

        clock_label.setHorizontalAlignment(JLabel.CENTER);
        clock_label.setFont(new Font("Lucida", Font.BOLD, 18));
        addJComponent(titlepanel, 0, 5, 1, 1, GridBagConstraints.CENTER, clock_label);

        time_label.setHorizontalAlignment(JLabel.LEFT);
        time_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJComponent(titlepanel, 1, 5, 1, 1, time_label);

        evacuatedCount_label.setHorizontalAlignment(JLabel.LEFT);
        evacuatedCount_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJComponent(titlepanel, 0, 6, 2, 1, GridBagConstraints.WEST, evacuatedCount_label);

        top_panel.add(titlepanel, BorderLayout.NORTH);

        /* scenarios */
        JPanel label_toggle_panel = new JPanel(new GridBagLayout());
        label_toggle_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        GridBagConstraints c = null;

        int max_events = 1;

        int y = 0;
        for (EventBase event : launcher.getSimulator().getScenario().eventList) {
            ButtonGroup bgroup = new ButtonGroup();
            class RadioButtonListener implements ActionListener {
                int index;
                EventBase event ;
                NetworkMap map ;
                public RadioButtonListener(EventBase _event, int _index, NetworkMap _map) {
                    event = _event;
                    index = _index;
                    map = _map;
                }
                public void actionPerformed(ActionEvent e) {
                    if (index == -2) {
                        event.occur(SimTime.Ending, map) ;
                    } else if (index == -1) {
                        event.unoccur(SimTime.Ending, map) ;
                    } else {
                        Itk.logError("wrong index") ;
                    }
                }
            }

            JRadioButton radio_button;
            radio_button = new JRadioButton("enabled");
            radio_button.addActionListener(new RadioButtonListener(event, -2, launcher.getMap()));
            bgroup.add(radio_button);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = y;
            label_toggle_panel.add(radio_button, c);
            radio_button = new JRadioButton("disabled | auto:");
            radio_button.addActionListener(new RadioButtonListener(event, -1, launcher.getMap()));
            bgroup.add(radio_button);
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = y;
            label_toggle_panel.add(radio_button, c);
            radio_button = new JRadioButton(event.atTime.getAbsoluteTimeString()) ;
            radio_button.addActionListener(new RadioButtonListener(event, 0, launcher.getMap()));
            bgroup.add(radio_button);
            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = y;
            label_toggle_panel.add(radio_button, c);
            radio_button.setSelected(true);

            c = new GridBagConstraints();
            c.gridx = 4;
            c.gridy = y;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 4, 0, 0);
            String eventName = event.getClass().getName().replaceAll("^.*\\.|Event$", "");
            label_toggle_panel.add(new JLabel(eventName), c);
            if (eventName.equals("Initiate")) {
                // ここで pauseTime の初期値を設定する
                pauseTime.copyFrom(event.atTime);
            }

            y++;
        }
        JScrollPane scroller = new JScrollPane(label_toggle_panel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        top_panel.add(scroller, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new java.awt.GridLayout(0, 1));

        /* Pause */

        JPanel pausePanel = new JPanel(new FlowLayout());
        pausePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        pausePanel.add(new JLabel("Pause"));
        final CardLayout spinnerLayout = new CardLayout();
        final JPanel spinnerPanel = new JPanel(spinnerLayout);
        final int offset = TimeZone.getDefault().getRawOffset();

        ButtonGroup pauseGroup = new ButtonGroup();
        JRadioButton timeButton = new JRadioButton("time");
        timeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                launcher.setPauseEnabled(true);
                timeSpinner.setEnabled(true);
                spinnerLayout.first(spinnerPanel);
                int time = (int)(((Date)timeSpinner.getValue()).getTime() + offset) / 1000;
                pauseTime.setTickCount(time - pauseTime.getOriginTimeInt());
                intSpinner.setValue(pauseTime.getTickCount());
            }
        });
        pauseGroup.add(timeButton);
        pausePanel.add(timeButton);

        JRadioButton tickCountButton = new JRadioButton("elapsed");
        tickCountButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                launcher.setPauseEnabled(true);
                intSpinner.setEnabled(true);
                spinnerLayout.last(spinnerPanel);
            }
        });
        pauseGroup.add(tickCountButton);
        pausePanel.add(tickCountButton);

        JRadioButton disabledButton = new JRadioButton("disabled");
        disabledButton.setSelected(true);
        disabledButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                launcher.setPauseEnabled(false);
                timeSpinner.setEnabled(false);
                intSpinner.setEnabled(false);
            }
        });
        pauseGroup.add(disabledButton);
        pausePanel.add(disabledButton);

        Date start = new Date(pauseTime.getOriginTimeInt() * 1000L - offset);
        Date end = new Date((60 * 60 * 24 - 1) * 1000L - offset);
        timeSpinner = new JSpinner(new SpinnerDateModel(start, start, end, Calendar.SECOND));
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm:ss"));
        timeSpinner.setEnabled(false);
        timeSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int time = (int)(((Date)timeSpinner.getValue()).getTime() + offset) / 1000;
                pauseTime.setTickCount(time - pauseTime.getOriginTimeInt());
                intSpinner.setValue(pauseTime.getTickCount());
            }
        });

        intSpinner = new JSpinner(new SpinnerNumberModel(pauseTime.getTickCount(), 0, 60 * 60 * 72, 1));
        intSpinner.setEnabled(false);
        intSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                pauseTime.setTickCount((Integer)intSpinner.getValue());
                int secondOfDay = pauseTime.getOriginTimeInt() + pauseTime.getTickCount();
                timeSpinner.setValue(new Date(Math.min(secondOfDay, 60 * 60 * 24 - 1) * 1000L - offset));
            }
        });

        spinnerPanel.add("timeSpinner", timeSpinner);
        spinnerPanel.add("intSpinner", intSpinner);
        pausePanel.add(spinnerPanel);

        // 時刻刻み幅が1秒以外の時は一時停止機能を無効にする
        if (pauseTime.getTickUnit() != 1.0) {
            pausePanel.setEnabled(false);
        }
        bottomPanel.add(pausePanel);

        /* Reset button */

        JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (launcher.getPropertiesFile() == null) {
            // エディタから起動した場合は名前を変える
            resetButton.setText("Reset with latest state");
        }
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launcher.setResetting(true);
                if (launcher.isRunning()) {
                    launcher.pause();
                }
                frame.dispose();
            }});
        resetPanel.add(resetButton);
        bottomPanel.add(resetPanel);

        top_panel.add(bottomPanel, BorderLayout.SOUTH);
        control_panel.add(top_panel, BorderLayout.CENTER);

        return control_panel;
    }

    /**
     * 時計表示
     */
    public void displayClock(SimTime currentTime) {
        time_label.setText(String.format("Elapsed: %5.2fsec", currentTime.getRelativeTime()));
        clock_label.setText(currentTime.getAbsoluteTimeString());
    }

    /**
     * evacuation count の計算
     */
    public void updateEvacuatedCount(EvacuationSimulator simulator) {
        String evacuatedCount_string = simulator.getEvacuatedCountStatus();
        evacuatedCount_label.setText(evacuatedCount_string);
    }

    /**
     * View タブの生成
     */
    private JPanel createViewPanel(boolean backgroundImageEnabled, boolean backgroundMapEnabled) {
        JPanel tabPanel = new JPanel();
        tabPanel.setName("View");
        tabPanel.setLayout(new BorderLayout());

        JPanel view_control = new JPanel(new BorderLayout());
        view_control.setPreferredSize(new Dimension(200, 500));
        tabPanel.add(view_control, BorderLayout.CENTER);

        // Zoom

        JPanel zoom_panel = new JPanel(new GridBagLayout());
        zoom_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Scale"));

        // agent size zoom
        addJLabel(zoom_panel, 0, 1, 1, 1, "agent size");

        agent_size_control = new JScrollBar(JScrollBar.HORIZONTAL, (int)(agent_size * 10), 1, 1, 301);
        agent_size_control.addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    agent_size = agent_size_control.getValue() / 10.0;
                    agent_size_value.setText("" + agent_size);
                    panel.repaint();
                }});
        agent_size_control.setPreferredSize(new Dimension(200, 20));
        addJComponent(zoom_panel, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, agent_size_control);

        agent_size_value = new JLabel();
        agent_size_value.setHorizontalAlignment(JLabel.RIGHT);
        agent_size_value.setText("" + agent_size);
        addJComponent(zoom_panel, 2, 1, 1, 1, GridBagConstraints.EAST, agent_size_value);
        view_control.add(zoom_panel, BorderLayout.CENTER);

        //// Check box ////

        JPanel checkbox_panel = new JPanel();
        checkbox_panel.setBorder(new CompoundBorder(checkbox_panel.getBorder(), new EmptyBorder(0, 4, 0, 4)));
        checkbox_panel.setLayout(new BoxLayout(checkbox_panel, BoxLayout.Y_AXIS));

        JPanel showNodesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showNodesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ノード表示の ON/OFF
        showNodesCheckBox = new JCheckBox("Show nodes");
        showNodesCheckBox.setSelected(showNodes.isSelected());
        showNodesCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showNodeLabelsCheckBox.setEnabled(showNodesCheckBox.isSelected());
                    showNodes.setSelected(showNodesCheckBox.isSelected());
                    update();
                }});
        showNodesPanel.add(showNodesCheckBox);

        // ノードラベル表示の ON/OFF
        showNodeLabelsCheckBox = new JCheckBox("Show node labels");
        showNodeLabelsCheckBox.setSelected(showNodeLabels.isSelected());
        showNodeLabelsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showNodeLabels.setSelected(showNodeLabelsCheckBox.isSelected());
                    update();
                }});
        showNodesPanel.add(showNodeLabelsCheckBox);
        checkbox_panel.add(showNodesPanel);

        JPanel showLinksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showLinksPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // リンク表示の ON/OFF
        showLinksCheckBox = new JCheckBox("Show links");
        showLinksCheckBox.setSelected(showLinks.isSelected());
        showLinksCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showLinkLabelsCheckBox.setEnabled(showLinksCheckBox.isSelected());
                    showLinks.setSelected(showLinksCheckBox.isSelected());
                    update();
                }});
        showLinksPanel.add(showLinksCheckBox);

        // リンクラベル表示の ON/OFF
        showLinkLabelsCheckBox = new JCheckBox("Show link labels");
        showLinkLabelsCheckBox.setSelected(showLinkLabels.isSelected());
        showLinkLabelsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showLinkLabels.setSelected(showLinkLabelsCheckBox.isSelected());
                    update();
                }});
        showLinksPanel.add(showLinkLabelsCheckBox);
        checkbox_panel.add(showLinksPanel);

        JPanel showAreaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showAreaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // エリア表示の ON/OFF
        showAreaCheckBox = new JCheckBox("Show area");
        showAreaCheckBox.setSelected(showArea.isSelected());
        showAreaCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAreaLabelsCheckBox.setEnabled(showAreaCheckBox.isSelected());
                    showArea.setSelected(showAreaCheckBox.isSelected());
                    update();
                }});
        showAreaPanel.add(showAreaCheckBox);

        // エリアラベル表示の ON/OFF
        showAreaLabelsCheckBox = new JCheckBox("Show area labels");
        showAreaLabelsCheckBox.setSelected(showAreaLabels.isSelected());
        showAreaLabelsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAreaLabels.setSelected(showAreaLabelsCheckBox.isSelected());
                    update();
                }});
        showAreaPanel.add(showAreaLabelsCheckBox);

        // エリアの配置を確認できる様にする
        JCheckBox showLocationCheckBox = new JCheckBox("outline");
        showLocationCheckBox.setSelected(isShowAreaLocation());
        showLocationCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setShowAreaLocation(showLocationCheckBox.isSelected());
                    panel.repaint();
                }});
        showAreaPanel.add(showLocationCheckBox);

        checkbox_panel.add(showAreaPanel);

        JPanel showAgentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showAgentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // エージェント表示の ON/OFF
        showAgentCheckBox = new JCheckBox("Show agents");
        showAgentCheckBox.setSelected(true);
        showAgentCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAgentLabelsCheckBox.setEnabled(showAgentCheckBox.isSelected());
                    update();
                }});
        showAgentPanel.add(showAgentCheckBox);

        // エージェントラベル表示の ON/OFF
        showAgentLabelsCheckBox = new JCheckBox("Show agent labels");
        showAgentLabelsCheckBox.setSelected(false);
        showAgentLabelsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    update() ;
                }});
        showAgentPanel.add(showAgentLabelsCheckBox);
        checkbox_panel.add(showAgentPanel);

        JPanel showMapPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showMapPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 背景画像表示の ON/OFF
        JCheckBox showBackgroundImageCheckBox = new JCheckBox("Show background image");
        showBackgroundImageCheckBox.setSelected(backgroundImageEnabled && showBackgroundImage);
        showBackgroundImageCheckBox.setEnabled(backgroundImageEnabled);
        showBackgroundImageCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showBackgroundImage = showBackgroundImageCheckBox.isSelected();
                    panel.repaint();
                }});
        showMapPanel.add(showBackgroundImageCheckBox);

        // 背景地図表示の ON/OFF
        JCheckBox showBackgroundMapCheckBox = new JCheckBox("Show background map");
        showBackgroundMapCheckBox.setSelected(backgroundMapEnabled && showBackgroundMap);
        showBackgroundMapCheckBox.setEnabled(backgroundMapEnabled);
        showBackgroundMapCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showBackgroundMap = showBackgroundMapCheckBox.isSelected();
                    panel.repaint();
                }});
        showMapPanel.add(showBackgroundMapCheckBox);
        checkbox_panel.add(showMapPanel);

        JPanel showPolygonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showPolygonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ポリゴン表示の ON/OFF
        JCheckBox showPolygonCheckBox = new JCheckBox("Show polygons");
        boolean polygonExists = false;
        for (MapPolygon polygon : launcher.getMap().getPolygons()) {
            if (polygon.isPlanePolygon()) {
                polygonExists = true;
                polygonShowing = true;
                break;
            }
        }
        showPolygonCheckBox.setEnabled(polygonExists);
        showPolygonCheckBox.setSelected(polygonExists);
        showPolygonCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    polygonShowing = showPolygonCheckBox.isSelected();
                    panel.repaint();
                }});
        showPolygonPanel.add(showPolygonCheckBox);

        // 海面表示の ON/OFF
        JCheckBox showTheSeaCheckBox = new JCheckBox("Show the sea");
        Coastline coastline = launcher.getCoastline();
        if (coastline == null || coastline.getOuterBoundaries().isEmpty()) {
            showTheSea = false;
            showTheSeaCheckBox.setEnabled(false);
        }
        showTheSeaCheckBox.setSelected(showTheSea);
        showTheSeaCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showTheSea = showTheSeaCheckBox.isSelected();
                    panel.repaint();
                }});
        showPolygonPanel.add(showTheSeaCheckBox);
        checkbox_panel.add(showPolygonPanel);

        // 仕切り線
        checkbox_panel.add(new JSeparator(SwingConstants.HORIZONTAL));

        // スクリーンショットを撮る
        record_snapshots.setSelected(isRecordSimulationScreen());
        record_snapshots.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setRecordSimulationScreen(record_snapshots.isSelected()) ;
                }});
        checkbox_panel.add(record_snapshots);

        // 歩行速度に応じてエージェントの色を変える
        JCheckBox change_agent_color_depending_on_speed_cb = new JCheckBox("Change agent color depending on speed");
        change_agent_color_depending_on_speed_cb.setSelected(changeAgentColorDependingOnSpeed);
        change_agent_color_depending_on_speed_cb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    changeAgentColorDependingOnSpeed = ((JCheckBox)e.getSource()).isSelected();
                    panel.repaint();
                }});
        checkbox_panel.add(change_agent_color_depending_on_speed_cb);

        // エージェントを重要性が高い順にソートしてから表示する
        JCheckBox sortAgentsCheckBox = new JCheckBox("Drawing agent by triage and speed order");
        sortAgentsCheckBox.setSelected(isDrawingAgentByTriageAndSpeedOrder());
        sortAgentsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setDrawingAgentByTriageAndSpeedOrder(sortAgentsCheckBox.isSelected()) ;
                }});
        checkbox_panel.add(sortAgentsCheckBox);

        // シミュレーションビュー上に進捗状況をテキスト表示する、及び表示位置の選択
        JPanel show_status_panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        show_status_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JCheckBox show_status_cb = new JCheckBox("Show status", showStatus);
        JRadioButton top_rb = new JRadioButton("Top", (messagePosition & TOP) == TOP);
        JRadioButton bottom_rb = new JRadioButton("Bottom", (messagePosition & BOTTOM) == BOTTOM);
        top_rb.setEnabled(showStatus);
        bottom_rb.setEnabled(showStatus);
        show_status_cb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showStatus = show_status_cb.isSelected();
                    top_rb.setEnabled(showStatus);
                    bottom_rb.setEnabled(showStatus);
                    panel.repaint();
                }});
        show_status_panel.add(show_status_cb);
        top_rb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    messagePosition = TOP;
                    panel.repaint();
                }});
        bottom_rb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    messagePosition = BOTTOM;
                    panel.repaint();
                }});
        ButtonGroup bg = new ButtonGroup();
        bg.add(top_rb);
        bg.add(bottom_rb);
        show_status_panel.add(top_rb);
        show_status_panel.add(bottom_rb);
        checkbox_panel.add(show_status_panel);

        // AIST ロゴの表示
        JCheckBox show_logo_cb = new JCheckBox("Show logo");
        show_logo_cb.setSelected(showLogo);
        show_logo_cb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showLogo = show_logo_cb.isSelected();
                    panel.repaint();
                }});
        checkbox_panel.add(show_logo_cb);

        // 表示の更新が完了するのを待ってから次のステップに進む
        JCheckBox viewSynchronizedCheckBox = new JCheckBox("View-calculation synchronized");
        viewSynchronizedCheckBox.setSelected(isViewSynchronized());
        viewSynchronizedCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setViewSynchronized(viewSynchronizedCheckBox.isSelected());
                }});
        checkbox_panel.add(viewSynchronizedCheckBox);

        // シミュレーション終了と同時にプログラムを終了する
        exit_with_simulation_finished_cb = new JCheckBox("Exit with simulation finished");
        exit_with_simulation_finished_cb.setSelected(exitWithSimulationFinished);
        checkbox_panel.add(exit_with_simulation_finished_cb);

        // Centering with scaling の際にマージンを加える
        JCheckBox marginAddedCheckBox = new JCheckBox("Add centering margin");
        marginAddedCheckBox.setSelected(isMarginAdded());
        marginAddedCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setMarginAdded(marginAddedCheckBox.isSelected()) ;
                }});
        checkbox_panel.add(marginAddedCheckBox);

        // ロケーションボタン
        JPanel locationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        locationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton centerinButton = new JButton("Centering");
        centerinButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.centering(false);
                    panel.repaint();
                }});
        locationPanel.add(centerinButton);

        JButton centerinWithScalingButton = new JButton("Centering with scaling");
        centerinWithScalingButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.centering(true);
                    panel.repaint();
                }});
        locationPanel.add(centerinWithScalingButton);

        JButton toTheOriginButton = new JButton("To the origin");
        toTheOriginButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.setPosition(0.0, 0.0);
                    panel.repaint();
                }});
        locationPanel.add(toTheOriginButton);

        checkbox_panel.add(locationPanel);

        view_control.add(checkbox_panel, BorderLayout.SOUTH);

        return tabPanel;
    }

    /**
     * カメラワークデータを読み込む
     */
    public void loadCamerawork(String filePath) {
        File file = new File(filePath);
        if (! file.exists()) {
            Itk.logError_("Camerawork file does not exist", filePath);
            Itk.quitByError() ;
        }

        Itk.logInfo("Load camerawork file", filePath);
        try {
            JSON json = new JSON(JSON.Mode.TRADITIONAL);
            ArrayList<Map<String, Object>> jsonObject = json.parse(new FileReader(filePath));
            camerawork.clear();
            for (Map<String, Object> object : jsonObject) {
                if (object.get("angle") == null) {
                    Itk.logError_("Camerawork file format is old", filePath);
                    Itk.quitByError() ;
                }
                camerawork.add(new CameraShot(object));
            }
        } catch (IOException e) {
            Itk.quitWithStackTrace(e) ;
        }
        cameraworkFile = filePath;
    }

    /**
     * カメラワークデータを保存する
     */
    public void saveCamerawork() {
        FileDialog fd = new FileDialog(frame, "Save camera position list", FileDialog.SAVE);
        fd.setVisible (true);
        if (fd.getFile() == null) {
            return;
        }
        String filePath = fd.getDirectory() + fd.getFile();

        Itk.logInfo("Save camerawork file", filePath);
        ArrayList<Map<String, Object>> jsonObject = new ArrayList<Map<String, Object>>();
        for (CameraShot cameraShot : camerawork) {
            jsonObject.add(cameraShot.getMapObject());
        }
        try {
            JSON.encode(jsonObject, new FileWriter(filePath));
        } catch (IOException e) {
            Itk.dumpStackTraceOf(e) ;
        }
    }

    /**
     * カメラワークの記録
     */
    public void recordCamerawork() {
        double currentTime = launcher.getSimulator().currentTime.getRelativeTime();
        int lastIndex = camerawork.size() - 1;
        if (lastIndex >= 0 && camerawork.get(lastIndex).time >= currentTime) {
            JOptionPane.showMessageDialog(frame, "Invalid operation", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CameraShot cameraShot = new CameraShot(currentTime, panel.getDrawingScale(), panel.getAngle(), agent_size, panel.getViewPosition());
        camerawork.add(cameraShot);
        cameraworkArea.append(cameraShot.toString());
        cameraworkArea.append("\n");
    }

    /**
     * マウス操作による視点移動とズーム機能の禁止/解除.
     *
     * true で禁止
     */
    public void setViewpointChangeInhibited(boolean b) {
        if (b) {
            viewpointChangeInhibited = true;
            agent_size_control.setEnabled(false);
            recordButton.setEnabled(false);
            loadCameraworkButton.setEnabled(false);
            saveCameraworkButton.setEnabled(false);
        } else {
            viewpointChangeInhibited = false;
            agent_size_control.setEnabled(true);
            // agent_size が変更されていてもスクロールバーへの反映はこの時点でおこなう
            agent_size_control.setValue((int)(agent_size * 10));
            recordButton.setEnabled(true);
            loadCameraworkButton.setEnabled(true);
            saveCameraworkButton.setEnabled(true);
        }
    }

    /**
     * Camera タブの生成.
     *
     * camera_2d_file の指定あり:
     *    Replay チェックボックスは selected
     *    Record, Load, Save as ボタンは無効
     * camera_2d_file の指定なし:
     *    Replay チェックボックスは無効
     *    Record, Load, Save as ボタンは有効
     */
    private JPanel createCameraPanel() {
        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.setName("Camera");
        tabPanel.setPreferredSize(new Dimension(200, 500));

        // Replay ON/OFF

        JPanel replayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        replayPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (cameraworkFile != null) {
            replayCheckBox.setText("Replay - " + cameraworkFile);
            replayCheckBox.setSelected(true);
        } else {
            replayCheckBox.setText("Replay");
            replayCheckBox.setEnabled(false);
        }
        replayCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (replayCheckBox.isSelected()) {
                    forceUpdateCamerawork = true;
                    setViewpointChangeInhibited(true);
                } else {
                    setViewpointChangeInhibited(false);
                }
            }
        });
        replayPanel.add(replayCheckBox);

        tabPanel.add(replayPanel, BorderLayout.NORTH);

        // カメラワークデータ表示

        cameraworkArea.setEditable(false);
        cameraworkArea.setAutoscrolls(true);
        cameraworkArea.setLineWrap(true);
        JScrollPane message_scroller = new JScrollPane(cameraworkArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        if (cameraworkFile != null) {
            for (CameraShot cameraShot : camerawork) {
                cameraworkArea.append(cameraShot.toString());
                cameraworkArea.append("\n");
            }
        }

        tabPanel.add(message_scroller, BorderLayout.CENTER);

        // コントロールボタン

        JPanel controlButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlButtonPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        recordButton.setToolTipText("Add current camera position to camerawork list");
        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recordCamerawork();
            }
        });
        controlButtonPanel.add(recordButton);

        loadCameraworkButton.setToolTipText("Load camerawork list.");
        loadCameraworkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(frame, "Open camerawork file", FileDialog.LOAD);
                fd.setVisible (true);
                if (fd.getFile() == null) {
                    return;
                }
                loadCamerawork(fd.getDirectory() + fd.getFile());
                // TODO: 相対ディレクトリパスを付加する
                replayCheckBox.setText("Replay - " + fd.getFile());
                replayCheckBox.setEnabled(true);
                cameraworkArea.setText("");
                for (CameraShot cameraShot : camerawork) {
                    cameraworkArea.append(cameraShot.toString());
                    cameraworkArea.append("\n");
                }
            }
        });
        controlButtonPanel.add(loadCameraworkButton);

        saveCameraworkButton.setToolTipText("Save current camerawork list.");
        saveCameraworkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCamerawork();
            }
        });
        controlButtonPanel.add(saveCameraworkButton);

        tabPanel.add(controlButtonPanel, BorderLayout.SOUTH);

        return tabPanel;
    }

    /**
     * Status タブの生成
     */
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel();
        statusPanel.setName("Status");
        statusPanel.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(200, 500));
        statusPanel.add(mainPanel, BorderLayout.CENTER);

        // 調査対象選択ラジオボタンパネル

        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        ButtonGroup bg = new ButtonGroup();
        targetPanel.add(new JLabel("Mode: "));

        JRadioButton noEffectRb = new JRadioButton("None", true);
        noEffectRb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mode = StatusMode.NO_EFFECT;
                    clearSelection();
                    panel.repaint();
                }});
        bg.add(noEffectRb);
        targetPanel.add(noEffectRb);

        JRadioButton nodeRb = new JRadioButton("Node", false);
        nodeRb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mode = StatusMode.NODE;
                    clearSelection();
                    panel.repaint();
                }});
        bg.add(nodeRb);
        targetPanel.add(nodeRb);

        JRadioButton linkRb = new JRadioButton("Link", false);
        linkRb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mode = StatusMode.LINK;
                    clearSelection();
                    panel.repaint();
                }});
        bg.add(linkRb);
        targetPanel.add(linkRb);

        JRadioButton areaRb = new JRadioButton("Area", false);
        areaRb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mode = StatusMode.AREA;
                    clearSelection();
                    panel.repaint();
                }});
        bg.add(areaRb);
        targetPanel.add(areaRb);

        JRadioButton agentRb = new JRadioButton("Agent", false);
        agentRb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mode = StatusMode.AGENT;
                    clearSelection();
                    panel.repaint();
                }});
        bg.add(agentRb);
        targetPanel.add(agentRb);

        mainPanel.add(targetPanel, BorderLayout.NORTH);

        // 情報表示エリア

        indicationArea.setEditable(false);
        indicationArea.setAutoscrolls(true);
        JScrollPane message_scroller = new JScrollPane(indicationArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(message_scroller, BorderLayout.CENTER);

        return statusPanel;
    }

    /**
     * テキストを情報表示エリアに表示する
     */
    public void statusIndication(String text) {
        StringBuilder buff = new StringBuilder();
        for (String line : text.split("\\n")) {
            buff.append("  ").append(line).append("\n");
        }
        indicationArea.setText(buff.toString());
        indicationArea.setCaretPosition(0);
    }

    /**
     * ステータスバーの表示テキストを返す
     */
    public String getStatusText() {
        return status.getText();
    }

    /**
     * ステータスバーに表示するテキストをセットする
     */
    public void setStatusText(String text) {
        status.setText(text);
    }

    /* Mouse listeners */

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            switch (mode) {
            case NODE:
                selectNode(e);
                break;
            case LINK:
                selectLink(e);
                break;
            case AREA:
                selectArea(e);
                break;
            case AGENT:
                selectAgent(e);
                break;
            }
        }
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}
 
    public void mousePressed(MouseEvent e) {
        if (viewpointChangeInhibited) {
            return;
        }
        // 右ドラッグまたは左ドラッグでマップをスクロールする
        if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
            scrolling = true;
            dragStartX = e.getX();
            dragStartY = e.getY();
        } 
    }

    public void mouseReleased(MouseEvent e) {
        scrolling = false;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (viewpointChangeInhibited) {
            return;
        }
        Point point = SwingUtilities.convertPoint(null, e.getX(), e.getY(), panel);
        if (e.isAltDown() || e.isShiftDown()) {
            panel.rotate(point.getX(), point.getY(), e.getWheelRotation(), e.isControlDown());
        } else {
            panel.zoom(point.getX(), point.getY(), e.getWheelRotation(), e.isControlDown());
        }
        panel.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        if (viewpointChangeInhibited) {
            return;
        }
        if (scrolling) {
            panel.scroll((e.getX() - dragStartX), (e.getY() - dragStartY));
            dragStartX = e.getX();
            dragStartY = e.getY();
            panel.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        Point canvasPoint = SwingUtilities.convertPoint(null, e.getX(), e.getY(), panel);
        Point2D mapPoint = panel.convertToOriginal(panel.pointConvertCanvasToMap(canvasPoint.getX(), canvasPoint.getY()));
        switch (mode) {
        case NODE:
            if (updateHoverNode(mapPoint)) {
                panel.updateHoverNode(hoverNode);
                panel.repaint();
            }
            break;
        case LINK:
            if (updateHoverLink(mapPoint)) {
                panel.updateHoverLink(hoverLink);               
                panel.repaint();
            }
            break;
        case AREA:
            if (updateHoverArea(mapPoint)) {
                panel.updateHoverArea(hoverArea);
                panel.repaint();
            }
            break;
        case AGENT:
            if (updateHoverAgent(mapPoint)) {
                panel.updateHoverAgent(hoverAgent);
                panel.repaint();
            }
            break;
        }
    }
 
    /**
     * シミュレーションパネルのスクリーンショットを撮ってファイルに保存する
     */
    public void captureScreenShot(final String filename) {
        int width = panel.getWidth();
        int height = panel.getHeight();
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        Thread thread = new Thread(new Runnable() {
            public void run() {
                incSaveThreadCount();
                try {
                    String path = screenshotDir + "/" + filename + "." + screenshotImageType;
                    ImageIO.write(image, screenshotImageType, new File(path));
                } catch (IOException e) {
                    Itk.dumpStackTraceOf(e) ;
                }
                decSaveThreadCount();
            }
        });
        thread.start();
    }

    /**
     * 調査対象の選択を解除する
     */
    private void clearSelection() {
        for (MapNode node : getNodes()) {
            node.selected = false;
        }
        for (MapLink link : getLinks()) {
            link.selected = false;
        }
        for (AgentBase agent : getWalkingAgents()) {
            agent.selected = false;
        }
        for (MapArea area : getMapAreas()) {
            area.selected = false;
        }
        hoverNode = null;
        hoverLink = null;
        hoverArea = null;
        hoverAgent = null;
        panel.updateHoverNode(null);
        panel.updateHoverLink(null);
        panel.updateHoverArea(null);
        panel.updateHoverAgent(null);
    }

    /**
     * マウスカーソル上のノードを調査対象として選択する
     */
    private boolean updateHoverNode(Point2D p) {
        double mindist = Double.POSITIVE_INFINITY;
        MapNode hoverNodeCandidate = null;
        for (MapNode node : getNodes()) {
            double dist = p.distance(node.getPosition());
            if (dist < mindist && dist < (10.0 / panel.getDrawingScale())) {
                hoverNodeCandidate = node;
                mindist = dist;
            }
        }

        boolean updated = (hoverNode != hoverNodeCandidate);
        hoverNode = hoverNodeCandidate;

        return updated;
    }

    /**
     * 選択中のノードの情報を表示する
     */
    private void selectNode(MouseEvent e) {
        if (hoverNode != null) {
            statusIndication(hoverNode.getStatusText());
        }
    }

    /**
     * マウスカーソル上のリンクを調査対象として選択する
     */
    private boolean updateHoverLink(Point2D p) {
        MapLink hoverLinkCandidate = null;
        double mindist = Double.MAX_VALUE;
        for (MapLink link : getLinks()) {
            MapNode from = (MapNode)link.getFrom();
            MapNode to = (MapNode)link.getTo();
            Line2D line = new Line2D.Double(from.getPosition(), to.getPosition());
            double dist = line.ptSegDist(p);
            if (dist < mindist) {
                hoverLinkCandidate = link;
                mindist = dist;
            }
        }

        boolean updated = (hoverLink != hoverLinkCandidate);
        hoverLink = hoverLinkCandidate;
        return updated;
    }

    /**
     * 選択中のリンクの情報を表示する
     */
    private void selectLink(MouseEvent e) {
        if (hoverLink == null) {
            return;
        }
        hoverLink.selected ^= true;
        statusIndication(hoverLink.getStatusText());
    }

    /**
     * マウスカーソル上のマップエリアを調査対象として選択する
     */
    private boolean updateHoverArea(Point2D p) {
        /* Find an existing agent */
        for (MapArea area : getMapAreas()) {
            if (area.contains(p)) {
                boolean updated = (!area.equals(hoverArea));
                hoverArea = area;
                return updated;
            }
        }
        boolean updated = (hoverArea != null);
        hoverArea = null;
        return updated;
    }

    /**
     * 選択中のマップエリアの情報を表示する
     */
    private void selectArea(MouseEvent e) {
        if (hoverArea == null) {
            return;
        }
        statusIndication(hoverArea.getStatusText());
    }

    /**
     * マウスカーソル上のエージェントを調査対象として選択する
     */
    private boolean updateHoverAgent(Point2D p) {
        boolean updated = false;
        hoverAgent = null;
        double mindist = Double.POSITIVE_INFINITY;
        for (AgentBase agent : getWalkingAgents()) {
            Point2D pos = agent.getPosition();
            Vector3D swing = agent.getSwing();
            double dist = p.distance(pos.getX() + swing.getX(), pos.getY() + swing.getY());
            if (dist < mindist && dist < (10.0 / panel.getDrawingScale())) {
                hoverAgent = agent;
                mindist = dist;
                updated = true;
            }
        }
        return updated;
    }

    /**
     * 選択中のエージェントの情報を表示する
     */
    private void selectAgent(MouseEvent e) {
        if (hoverAgent == null) {
            return;
        }
        statusIndication(hoverAgent.getStatusText());
    }
    
    /* getters/setters */
    
    public GuiSimulationLauncher2D getLauncher() { return launcher; }

    public void setAgentSize(double d) {
        agent_size = d;
    }

    public double getAgentSize() {
        return agent_size;
    }

    public void setShowAreaLocation(boolean showAreaLocation) {
        this.showAreaLocation = showAreaLocation;
    }

    public boolean isShowAreaLocation() {
        return showAreaLocation;
    }

    public void setRecordSimulationScreen(boolean b) {
        recordSimulationScreen = b;
    }

    public boolean isRecordSimulationScreen() {
        return recordSimulationScreen;
    }

    public void setScreenshotDir(String str) {
        screenshotDir = str;
    }

    public String getScreenshotDir() {
        return screenshotDir;
    }

    public void setClearScreenshotDir(boolean b) {
        clearScreenshotDir = b;
    }

    public boolean isClearScreenshotDir() {
        return clearScreenshotDir;
    }

    public void setScreenshotImageType(String str) {
        screenshotImageType = str;
    }

    public String getScreenshotImageType() {
        return screenshotImageType;
    }

    public void setChangeAgentColorDependingOnSpeed(boolean b) {
        changeAgentColorDependingOnSpeed = b;
    }

    public boolean getChangeAgentColorDependingOnSpeed() {
        return changeAgentColorDependingOnSpeed;
    }

    public void setDrawingAgentByTriageAndSpeedOrder(boolean b) {
        drawingAgentByTriageAndSpeedOrder = b;
    }

    public boolean isDrawingAgentByTriageAndSpeedOrder() {
        return drawingAgentByTriageAndSpeedOrder;
    }

    public void setShowStatus(boolean b) {
        showStatus = b;
    }

    public boolean isShowStatus() {
        return showStatus;
    }

    public void setStatusPosition(int position) {
        messagePosition = position;
    }

    public int getStatusPosition() {
        return messagePosition;
    }

    public void setShowLogo(boolean b) {
        showLogo = b;
    }

    public boolean isShowLogo() {
        return showLogo;
    }

    public void setViewSynchronized(boolean b) {
        launcher.setViewSynchronized(b);
    }

    public boolean isViewSynchronized() {
        return launcher.isViewSynchronized();
    }

    public JCheckBox getExitWithSimulationFinishedCheckBox() {
        return exit_with_simulation_finished_cb;
    }

    public void setExitWithSimulationFinished(boolean b) {
        exitWithSimulationFinished = b;
    }

    public void setMarginAdded(boolean marginAdded) {
        this.marginAdded = marginAdded;
    }

    public boolean isMarginAdded() {
        return marginAdded;
    }

    public void setShowBackgroundImage(boolean showBackgroundImage) {
        this.showBackgroundImage = showBackgroundImage;
    }

    public boolean isShowBackgroundImage() {
        return showBackgroundImage;
    }

    public void setShowBackgroundMap(boolean showBackgroundMap) {
        this.showBackgroundMap = showBackgroundMap;
    }

    public boolean isShowBackgroundImage(MapPartGroup group) {
        JCheckBoxMenuItem menuItem = backgroundImageMenus.get(group);
        if (menuItem == null) {
            return false;
        }
        return menuItem.isSelected();
    }

    public boolean isShowBackgroundMap() {
        return showBackgroundMap;
    }

    public void setShowTheSea(boolean showTheSea) {
        this.showTheSea = showTheSea;
    }

    public boolean isShowTheSea() {
        return showTheSea;
    }

    public void setPolygonShowing(boolean polygonShowing) {
        this.polygonShowing = polygonShowing;
    }

    public boolean isPolygonShowing() {
        return polygonShowing;
    }

    /* access to the object(nodes, links, agents, sub-groups)
     * that are managed under this frame */
    public MapNodeTable getNodes() {
        return launcher.getMap().getNodes();
    }

    public MapLinkTable getLinks() {
        return launcher.getMap().getLinks();
    }

    public ArrayList<MapArea> getMapAreas() {
        return areas;
    }

    /**
     * 一時停止時刻を返す
     */
    public SimTime getPauseTime() {
        return pauseTime;
    }

    /**
     * 現在マップ上に存在しているエージェントをセットする
     */
    synchronized public void setWalkingAgents(Collection<AgentBase> walkingAgentCollection) {
        walkingAgents = new ArrayList<AgentBase>(walkingAgentCollection);
        if (isDrawingAgentByTriageAndSpeedOrder()) {
            Collections.sort(walkingAgents, agentComparator);
        }
    }

    /**
     * 現在マップ上に存在しているエージェントを返す
     */
    synchronized public ArrayList<AgentBase> getWalkingAgents() {
        return walkingAgents;
    }

    /**
     * 状況が深刻なエージェントほど後ろになる Comparator
     */
    private Comparator<AgentBase> agentComparator = new Comparator<AgentBase>() {
        public int compare(AgentBase agent1, AgentBase agent2) {
            if (agent1 == agent2) {
                return 0;
            }
            if (agent1.getTriageInt() > agent2.getTriageInt()) {
                return 1;
            } else if (agent1.getTriageInt() < agent2.getTriageInt()) {
                return -1;
            } else {
                if (agent1.getSpeed() < agent2.getSpeed()) {
                    return 1;
                } else if (agent1.getSpeed() > agent2.getSpeed()) {
                    return -1;
                } else {
                    return agent1.ID.compareTo(agent2.ID);
                }
            }
        }
    };

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
                agent_size = next_camera.agentSize;
                panel.setDrawingScale(next_camera.zoom);
                panel.setAngle(next_camera.angle);
                panel.setPosition(next_camera.position.getX(), next_camera.position.getY());
            }
            // 最後の CameraShot とそれ以降
            else if (next_camera == null) {
                if (last_camera.time < time && ! forceUpdateCamerawork) {
                    return;
                }
                agent_size = last_camera.agentSize;
                panel.setDrawingScale(last_camera.zoom);
                panel.setAngle(last_camera.angle);
                panel.setPosition(last_camera.position.getX(), last_camera.position.getY());
            }
            // 上の条件以外
            else {
                if (time > last_camera.time &&
                        last_camera.zoom == next_camera.zoom &&
                        last_camera.agentSize == next_camera.agentSize &&
                        last_camera.position.equals(next_camera.position) && ! forceUpdateCamerawork) {
                    return;
                }
                double ratio = (time - last_camera.time) / (next_camera.time - last_camera.time);
                agent_size = (next_camera.agentSize - last_camera.agentSize) * ratio + last_camera.agentSize;
                panel.setDrawingScale((next_camera.zoom - last_camera.zoom) * ratio + last_camera.zoom);

                double angularDifference = next_camera.angle - last_camera.angle;
                if (angularDifference <= -180.0) {
                    angularDifference += 360.0;
                } else if (angularDifference > 180.0) {
                    angularDifference -= 360.0;
                }
                double angle = angularDifference * ratio + last_camera.angle;
                if (angle < 0.0) {
                    angle += 360.0;
                } else if (angle >= 360.0) {
                    angle -= 360.0;
                }
                panel.setAngle(angle);

                double last_x = last_camera.position.getX();
                double last_y = last_camera.position.getY();
                double next_x = next_camera.position.getX();
                double next_y = next_camera.position.getY();
                panel.setPosition((next_x - last_x) * ratio + last_x, (next_y - last_y) * ratio + last_y);
            }
            forceUpdateCamerawork = false;
        }
    }
}
