// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Gui;

import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.AdjustmentListener ;
import java.awt.event.AdjustmentEvent ;
import java.awt.event.ItemEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.*;

import javax.vecmath.Vector3d;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.GuiSimulationLauncher2D;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.Hover;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.PollutionHandler.*;
import nodagumi.Itk.*;

/**
 * 2D シミュレーションウィンドウ
 */
public class SimulationFrame2D extends JFrame
    implements MouseListener, MouseWheelListener, MouseMotionListener {
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
    private boolean viewSynchronized = false;
    private boolean exitWithSimulationFinished = false;
    private boolean marginAdded = false;

    /* メニュー構成変数 */

    private CheckboxMenuItem showNodes = null;
    private CheckboxMenuItem showNodeLabels = null;
    private CheckboxMenuItem showLinks = null;
    private CheckboxMenuItem showLinkLabels = null;
    // private CheckboxMenuItem showGroups = null;
    private CheckboxMenuItem showArea = null;
    private CheckboxMenuItem showAreaLabels = null;

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
    private transient JTextArea message = new JTextArea("UNMaps Version 1.9.5\n") {
        @Override
        public void append(String str) {
            super.append(str);
            message.setCaretPosition(message.getDocument().getLength());
        }
    };

    /* View タブ構成変数 */

    private boolean showAreaLocation = false;
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

    /* Status タブ構成変数 */

    public enum StatusMode {
        NO_EFFECT, NODE, LINK, AREA, AGENT
    };
    private StatusMode mode = StatusMode.NO_EFFECT;
    private transient JTextArea indicationArea = new JTextArea("Information");

    /* マウス操作情報 */

    public Point2D mousePoint = new Point2D.Double(0.0, 0.0);
    private boolean scrolling = false;
    private int dragStartX = 0;
    private int dragStartY = 0;

    /* スクリーンショット保存用スレッド数を管理するためのカウンタ制御 */

    private int saveThreadCount = 0;
    synchronized public int getSaveThreadCount() { return saveThreadCount; }
    synchronized public void incSaveThreadCount() { saveThreadCount++; }
    synchronized public void decSaveThreadCount() { saveThreadCount--; }

    public SimulationFrame2D(String title, int simulationPanelWidth, int simulationPanelHeight,
            GuiSimulationLauncher2D launcher, CrowdWalkPropertiesHandler properties) {
        super(title);

        frame = this;
        this.launcher = launcher;
        if (properties != null) {
            launcher.setGuiValues(this);
        }
        areas = launcher.getMap().getAreas();
        // 実在の地図ベースのマップでない時はセンタリングマージンを付加する
        setMarginAdded(getLinks().size() < MINIMUM_REAL_MAP_LINKS);

        setupMenu();
        setupContents(simulationPanelWidth, simulationPanelHeight, properties);

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(new Runnable (){
                        @Override
                        public void run() {
                            launcher.simulationWindowOpenedOperation(frame) ;
                        }});
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
     * メニューの配置
     */
    private void setupMenu() {
        MenuBar menuBar = new MenuBar();

        //// File menu ////

        Menu fileMenu = new PopupMenu("File");

        MenuShortcut shortcut = new MenuShortcut(java.awt.event.KeyEvent.VK_W);
        MenuItem mi = new MenuItem("Close", shortcut);
        mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (launcher.isRunning()) {
                        launcher.pause();
                    }
                    launcher.saveSimulatorPosition(frame);
                    frame.dispose();
                }});
        fileMenu.add(mi);
        
        fileMenu.add(fileMenu);

        menuBar.add(fileMenu);

        //// View menu ////

        Menu viewMenu = new PopupMenu("View");

        mi = new MenuItem("Centering");
        mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.centering(false);
                    panel.repaint();
                }});
        viewMenu.add(mi);

        mi = new MenuItem("Centering with scaling");
        mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.centering(true);
                    panel.repaint();
                }});
        viewMenu.add(mi);
        
        mi = new MenuItem("To the origin");
        mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.setPosition(0, 0);
                    panel.repaint();
                }});
        viewMenu.add(mi);

        viewMenu.addSeparator();

        showNodes = new CheckboxMenuItem("Show nodes");
        showNodes.setState(true);
        showNodes.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    showNodesCheckBox.setSelected(showNodes.getState());
                    showNodeLabelsCheckBox.setEnabled(showNodes.getState());
                    update();
                }});
        viewMenu.add(showNodes);

        showNodeLabels = new CheckboxMenuItem(" (Show node labels)");
        showNodeLabels.setState(false);
        showNodeLabels.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    showNodeLabelsCheckBox.setSelected(showNodeLabels.getState());
                    update();
                }});
        viewMenu.add(showNodeLabels);

        showLinks = new CheckboxMenuItem("Show links");
        showLinks.setState(true);
        showLinks.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    showLinksCheckBox.setSelected(showLinks.getState());
                    showLinkLabelsCheckBox.setEnabled(showLinks.getState());
                    update();
                }});
        viewMenu.add(showLinks);

        showLinkLabels = new CheckboxMenuItem(" (Show link labels)");
        showLinkLabels.setState(false);
        showLinkLabels.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    showLinkLabelsCheckBox.setSelected(showLinkLabels.getState());
                    update();
                }});
        viewMenu.add(showLinkLabels);
        
        // showGroups = new CheckboxMenuItem("Show groups");
        // showGroups.setState(true);
        // showGroups.addItemListener(e -> update());
        // viewMenu.add(showGroups);

        showArea = new CheckboxMenuItem("Show area");
        showArea.setState(true);
        showArea.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    showAreaCheckBox.setSelected(showArea.getState());
                    showAreaLabelsCheckBox.setEnabled(showArea.getState());
                    update();
                }});
        viewMenu.add(showArea);

        showAreaLabels = new CheckboxMenuItem(" (Show area labels)");
        showAreaLabels.setState(false);
        showAreaLabels.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    showAreaLabelsCheckBox.setSelected(showAreaLabels.getState());
                    update();
                }});
        viewMenu.add(showAreaLabels);
        
        menuBar.add(viewMenu);

        setMenuBar(menuBar);
    }

    /**
     * 各表示モードの ON/OFF をシミュレーションパネルの変数に反映する
     */
    public void update() {
        panel.setShowNodes(showNodes.getState());
        showNodeLabels.setEnabled(showNodes.getState());
        panel.setShowNodeNames(showNodeLabels.getState());

        panel.setShowLinks(showLinks.getState());
        showLinkLabels.setEnabled(showLinks.getState());
        panel.setShowLinkNames(showLinkLabels.getState());
        panel.setShowArea(showArea.getState());
        showAreaLabels.setEnabled(showArea.getState());
        panel.setShowAreaNames(showAreaLabels.getState());
        panel.setShowAgents(showAgentCheckBox.isSelected());
        panel.setShowAgentNames(showAgentLabelsCheckBox.isSelected());
        clearSelection();
        repaint();
    }

    /**
     * パネルの配置
     */
    private void setupContents(int simulationPanelWidth, int simulationPanelHeight,
            CrowdWalkPropertiesHandler properties) {
        setLayout(new BorderLayout());
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(400, simulationPanelHeight));

        //// 2D シミュレーションパネル ////

        panel = new SimulationPanel2D(this, launcher.getMap(), properties);
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
        tabs.add(createViewPanel());
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
                        launcher.start();
                    } else {
                        launcher.pause();
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
        addJLabel(titlepanel, 0, 0, 1, 1, GridBagConstraints.EAST, "Map");
        if (launcher.getNetworkMapFile() != null) {
            File map_file = new File(launcher.getNetworkMapFile());
            addJLabel(titlepanel, 1, 0, 1, 1, map_file.getName());
        } else {
            addJLabel(titlepanel, 1, 0, 1, 1, "No map file");
        }

        addJLabel(titlepanel, 0, 1, 1, 1, GridBagConstraints.EAST, "Agent");
        if (launcher.getGenerationFile() != null) {
            File generation_file = new File(launcher.getGenerationFile());
            addJLabel(titlepanel, 1, 1, 1, 1, generation_file.getName());
        } else {
            addJLabel(titlepanel, 1, 1, 1, 1, "No generation file");
        }

        addJLabel(titlepanel, 0, 2, 1, 1, GridBagConstraints.EAST, "Scenario");
        if (launcher.getScenarioFile() != null) {
            File scenario_file = new File(launcher.getScenarioFile());
            addJLabel(titlepanel, 1, 2, 1, 1, scenario_file.getName());
        } else {
            addJLabel(titlepanel, 1, 2, 1, 1, "No scenario file");
        }

        addJLabel(titlepanel, 0, 3, 1, 1, GridBagConstraints.EAST, "Pollution");
        if (launcher.getPollutionFile() != null) {
            File pollution_file = new File(launcher.getPollutionFile());
            addJLabel(titlepanel, 1, 3, 1, 1, pollution_file.getName());
        } else {
            addJLabel(titlepanel, 1, 3, 1, 1, "No pollution file");
        }

        clock_label.setHorizontalAlignment(JLabel.CENTER);
        clock_label.setFont(new Font("Lucida", Font.BOLD, 18));
        addJComponent(titlepanel, 0, 4, 1, 1, GridBagConstraints.CENTER, clock_label);

        time_label.setHorizontalAlignment(JLabel.LEFT);
        time_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJComponent(titlepanel, 1, 4, 1, 1, time_label);

        evacuatedCount_label.setHorizontalAlignment(JLabel.LEFT);
        evacuatedCount_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJComponent(titlepanel, 0, 5, 2, 1, GridBagConstraints.WEST, evacuatedCount_label);

        JScrollPane scroller = new JScrollPane(titlepanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(400, 160));
        top_panel.add(scroller, BorderLayout.NORTH);

        /* scenarios */
        JPanel label_toggle_panel = new JPanel(new GridBagLayout());
        label_toggle_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        GridBagConstraints c = null;

        int max_events = 1;

        int y = 0;
        for (EventBase event : launcher.getSimulator().getScenario().eventList) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.fill = GridBagConstraints.WEST;

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
            y++;
        }
        scroller = new JScrollPane(label_toggle_panel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(400, 220));
        top_panel.add(scroller, BorderLayout.CENTER);

        control_panel.add(top_panel, BorderLayout.CENTER);

        /* text message */
        message.setEditable(false);
        message.setAutoscrolls(true);
        JScrollPane message_scroller = new JScrollPane(message,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        message_scroller.setPreferredSize(new Dimension(300, 150));

        control_panel.add(message_scroller, BorderLayout.SOUTH);

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
    private JPanel createViewPanel() {
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

        JScrollBar agent_size_control = new JScrollBar(JScrollBar.HORIZONTAL, (int)(agent_size * 10), 1, 1, 100);
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
        showNodesCheckBox.setSelected(showNodes.getState());
        showNodesCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showNodeLabelsCheckBox.setEnabled(showNodesCheckBox.isSelected());
                    showNodes.setState(showNodesCheckBox.isSelected());
                    update();
                }});
        showNodesPanel.add(showNodesCheckBox);

        // ノードラベル表示の ON/OFF
        showNodeLabelsCheckBox = new JCheckBox("Show node labels");
        showNodeLabelsCheckBox.setSelected(showNodeLabels.getState());
        showNodeLabelsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showNodeLabels.setState(showNodeLabelsCheckBox.isSelected());
                    update();
                }});
        showNodesPanel.add(showNodeLabelsCheckBox);
        checkbox_panel.add(showNodesPanel);

        JPanel showLinksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showLinksPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // リンク表示の ON/OFF
        showLinksCheckBox = new JCheckBox("Show links");
        showLinksCheckBox.setSelected(showLinks.getState());
        showLinksCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showLinkLabelsCheckBox.setEnabled(showLinksCheckBox.isSelected());
                    showLinks.setState(showLinksCheckBox.isSelected());
                    update();
                }});
        showLinksPanel.add(showLinksCheckBox);

        // リンクラベル表示の ON/OFF
        showLinkLabelsCheckBox = new JCheckBox("Show link labels");
        showLinkLabelsCheckBox.setSelected(showLinkLabels.getState());
        showLinkLabelsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showLinkLabels.setState(showLinkLabelsCheckBox.isSelected());
                    update();
                }});
        showLinksPanel.add(showLinkLabelsCheckBox);
        checkbox_panel.add(showLinksPanel);

        JPanel showAreaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        showAreaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // エリア表示の ON/OFF
        showAreaCheckBox = new JCheckBox("Show area");
        showAreaCheckBox.setSelected(showArea.getState());
        showAreaCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAreaLabelsCheckBox.setEnabled(showAreaCheckBox.isSelected());
                    showArea.setState(showAreaCheckBox.isSelected());
                    update();
                }});
        showAreaPanel.add(showAreaCheckBox);

        // エリアラベル表示の ON/OFF
        showAreaLabelsCheckBox = new JCheckBox("Show area labels");
        showAreaLabelsCheckBox.setSelected(showAreaLabels.getState());
        showAreaLabelsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAreaLabels.setState(showAreaLabelsCheckBox.isSelected());
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
                    panel.setPosition(0, 0);
                    panel.repaint();
                }});
        locationPanel.add(toTheOriginButton);

        checkbox_panel.add(locationPanel);

        view_control.add(checkbox_panel, BorderLayout.SOUTH);

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
    public void statusIndication(String str) {
        indicationArea.setText(str);
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
        int i = e.getWheelRotation();
        panel.zoom(i);
        panel.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        if (scrolling) {
            panel.scroll((e.getX() - dragStartX), (e.getY() - dragStartY));
            dragStartX = e.getX();
            dragStartY = e.getY();
            panel.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        Point2D p = panel.revCalcPos(e.getX(), e.getY());
        mousePoint = p;     // これはCrowdWalk座標値
        switch (mode) {
        case NODE:
            if (updateHoverNode(p)) {
                panel.updateHoverNode(hoverNode);
                panel.repaint();
            }
            break;
        case LINK:
            if (updateHoverLink(p)) {
                panel.updateHoverLink(hoverLink);               
                panel.repaint();
            }
            break;
        case AREA:
            if (updateHoverArea(p)) {
                panel.updateHoverArea(hoverArea);
                panel.repaint();
            }
            break;
        case AGENT:
            if (updateHoverAgent(p)) {
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
                    e.printStackTrace();
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
            double dist = p.distance(node.getLocalCoordinates());
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
            statusIndication(getNodeInformation(hoverNode));
        }
    }

    /**
     * ノード情報テキスト(情報表示エリア用)
     */
    public String getNodeInformation(MapNode node) {
        StringBuilder buff = new StringBuilder();
        buff.append(" Node ID: ").append(node.ID).append("\n");
        buff.append(" x: ").append(node.getX()).append("\n");
        buff.append(" y: ").append(node.getY()).append("\n");
        buff.append(" height: ").append(node.getHeight()).append("\n");
        buff.append(" tags: ").append(node.getTagString()).append("\n");
        HashMap<String, NavigationHint> hints
            = node.getHints(NavigationHint.DefaultMentalMode) ;
        if (! hints.isEmpty()) {
            buff.append(" ---- Navigation hints ----\n");
            ArrayList<String> hintKeys = new ArrayList(hints.keySet());
            Collections.sort(hintKeys);
            for (String key : hintKeys) {
                NavigationHint hint = hints.get(key);
                buff.append(" key: ").append(key).append("\n");
                if (hint.toNode == null) {
                    buff.append("     toNode: null\n");
                } else {
                    buff.append("     toNode: ").append(hint.toNode.ID).append("(").append(hint.toNode.getTagString()).append(")\n");
                }
                if (hint.viaLink == null) {
                    buff.append("     viaLink: null\n");
                } else {
                    buff.append("     viaLink: ").append(hint.viaLink.ID).append("\n");
                }
                buff.append("     distance: ").append(hint.distance).append("\n");
            }
        }
        return buff.toString();
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
            Line2D line = new Line2D.Double(from.getLocalCoordinates(), to.getLocalCoordinates());
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
        statusIndication(getLinkInformation(hoverLink));
    }

    /**
     * リンク情報テキスト(情報表示エリア用)
     */
    public String getLinkInformation(MapLink link) {
        StringBuilder buff = new StringBuilder();
        buff.append(" Link ID: ").append(link.ID).append("\n");
        buff.append(" length: ").append(link.getLength()).append("\n");
        buff.append(" width: ").append(link.getWidth()).append("\n");
        buff.append(" laneWidth(Forward): ").append(link.getLaneWidth(Direction.Forward)).append("\n");
        buff.append(" laneWidth(Backward): ").append(link.getLaneWidth(Direction.Backward)).append("\n");
        buff.append(" tags: ").append(link.getTagString()).append("\n");
        buff.append(" agents: ").append(link.getAgents().size()).append("\n");
        MapNode fromNode = link.getFrom();
        if (fromNode == null) {
            buff.append(" from Node: null\n");
        } else {
            buff.append(" from Node:").append("\n");
            buff.append("     Node ID: ").append(fromNode.ID).append("\n");
            buff.append("     x: ").append(fromNode.getX()).append("\n");
            buff.append("     y: ").append(fromNode.getY()).append("\n");
            buff.append("     height: ").append(fromNode.getHeight()).append("\n");
            buff.append("     tags: ").append(fromNode.getTagString()).append("\n");
        }
        MapNode toNode = link.getTo();
        if (toNode == null) {
            buff.append(" to Node: null\n");
        } else {
            buff.append(" to Node:").append("\n");
            buff.append("     Node ID: ").append(toNode.ID).append("\n");
            buff.append("     x: ").append(toNode.getX()).append("\n");
            buff.append("     y: ").append(toNode.getY()).append("\n");
            buff.append("     height: ").append(toNode.getHeight()).append("\n");
            buff.append("     tags: ").append(toNode.getTagString()).append("\n");
        }
        return buff.toString();
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
        statusIndication(getAreaInformation((MapAreaRectangle)hoverArea));
    }

    /**
     * マップエリア情報テキスト(情報表示エリア用)
     */
    public String getAreaInformation(MapAreaRectangle area) {
        Rectangle2D bounds = (Rectangle2D)area.getShape();
        PollutionLevelInfo pollutionLevel = area.getPollutionLevel();
        StringBuilder buff = new StringBuilder();
        buff.append(" Area ID: ").append(area.ID).append("\n");
        buff.append(" pWestX: ").append(bounds.getMinX()).append("\n");
        buff.append(" pEastX: ").append(bounds.getMaxX()).append("\n");
        buff.append(" pNorthY: ").append(bounds.getMinY()).append("\n");
        buff.append(" pSouthY: ").append(bounds.getMaxY()).append("\n");
        buff.append(" current level: ").append(pollutionLevel.getCurrentLevel()).append("\n");
        buff.append(" normalized level: ").append(pollutionLevel.getNormalizedLevel()).append("\n");
        buff.append(" tags: ").append(area.getTagString()).append("\n");
        return buff.toString();
    }

    /**
     * マウスカーソル上のエージェントを調査対象として選択する
     */
    private boolean updateHoverAgent(Point2D p) {
        boolean updated = false;
        hoverAgent = null;
        double mindist = Double.POSITIVE_INFINITY;
        for (AgentBase agent : getWalkingAgents()) {
            Point2D pos = agent.getPos();
            Vector3d swing = agent.getSwing();
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
        statusIndication(getAgentInformation(hoverAgent));
    }
    
    /**
     * エージェント情報テキスト(情報表示エリア用)
     */
    public String getAgentInformation(AgentBase agent) {
        StringBuilder buff = new StringBuilder();
        buff.append(" Agent ID: ").append(agent.ID).append("\n");
        buff.append(" config: ").append(agent.getConfigLine()).append("\n");
        buff.append(" type: ").append(agent.getClass().getSimpleName()).append("\n");
        buff.append(" goal: ").append(agent.getGoal()).append("\n");
        buff.append(" generated time: ").append(agent.generatedTime.getAbsoluteTimeString()).append("\n");
        buff.append(" position X: ").append(agent.getPos().getX()).append("\n");
        buff.append(" position Y: ").append(agent.getPos().getY()).append("\n");
        buff.append(" position Z: ").append(agent.getHeight()).append("\n");
        buff.append(" drawing position X: ").append(agent.getPos().getX() + agent.getSwing().x).append("\n");
        buff.append(" drawing position Y: ").append(agent.getPos().getY() + agent.getSwing().y).append("\n");
        buff.append(" drawing position Z: ").append(
            agent.getHeight() / ((MapPartGroup)agent.getCurrentLink().getParent()).getScale()
        ).append("\n");
        buff.append(" velocity: ").append(agent.getSpeed()).append("\n");
        buff.append(" acceleration: ").append(agent.getAcceleration()).append("\n");
        buff.append(" previous node: ").append(agent.getPrevNode().ID).append("\n");
        buff.append(" next node: ").append(agent.getNextNode().ID).append("\n");
        buff.append(" current link: ").append(agent.getCurrentLink().ID).append("\n");
        buff.append(" advancing distance: ").append(agent.getAdvancingDistance()).append("\n");
        buff.append(" direction: ").append(agent.isForwardDirection() ? "Forward" : "Backward").append("\n");
        buff.append(" waiting: ").append(agent.isWaiting()).append("\n");
        buff.append(" current exposure: ").append(agent.obstructer.currentValueForLog()).append("\n");
        buff.append(" amount exposure: ").append(agent.obstructer.accumulatedValueForLog()).append("\n");
        buff.append(" triage: ").append(agent.getTriageName()).append("\n");
        return buff.toString();
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
        viewSynchronized = b;
    }

    public boolean isViewSynchronized() {
        return viewSynchronized;
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
}
