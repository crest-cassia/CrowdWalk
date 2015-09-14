// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.Insets;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationController;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.FilePathManipulation;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;

import nodagumi.Itk.*;

public class GuiSimulationLauncher extends BasicSimulationLauncher
    implements SimulationController {

    /**
     * GUI の設定情報
     */
    private Settings settings;

    /**
     * ウィンドウクローズと共にアプリケーションを終了するか?
     */
    private boolean exitOnClose = false;

    /**
     * Properties
     */
    public static final String[] SHOW_STATUS_VALUES = {"none", "top", "bottom"};
    public static final String[] IMAGE_TYPES = {"bmp", "gif", "jpg", "png"};
    private int deferFactor = 0;
    private double verticalScale = 1.0;
    private double agentSize = 1.0;
    private String cameraFile = null;
    private double zoom = 1.0;
    private boolean recordSimulationScreen = false;
    private String screenshotDir = "screenshots";
    private boolean clearScreenshotDir = false;
    private String screenshotImageType = "png";
    private boolean simulationWindowOpen = false;
    private boolean autoSimulationStart = false;
    private boolean hideLinks = false;
    private boolean densityMode = false;
    private boolean changeAgentColorDependingOnSpeed = true;
    private boolean showStatus = false;
    private String showStatusPosition = "top";
    private boolean showLogo = false;
    private boolean show3dPolygon = true;

    /**
     * 画面パーツ類。
     */
    private transient JButton start_button = null;
    private transient JButton pause_button = null;
    private transient JButton step_button = null;

    private transient JScrollBar simulationDeferFactorControl;
    private transient JLabel simulationDeferFactorValue;

    private transient JPanel control_panel = null;
    private transient JLabel clock_label = new JLabel("NOT STARTED1");
    private transient JLabel time_label = new JLabel("NOT STARTED2");
    private transient JLabel evacuatedCount_label = new JLabel("NOT STARTED3");
    private ArrayList<ButtonGroup> toggle_scenario_button_groups = new
        ArrayList<ButtonGroup>();
    private transient JTextArea message = new JTextArea("UNMaps Version 1.9.5\n") {
        @Override
        public void append(String str) {
            super.append(str);
            message.setCaretPosition(message.getDocument().getLength());
        }
    };

    /**
     * アプリ起動時にシミュレーションを開始する時に用いるコンストラクタ.
     */
    public GuiSimulationLauncher(String _propertiesPath, Settings _settings,
            ArrayList<String> commandLineFallbacks) {
        super(null) ;
        // load properties
        setPropertiesFromFile(_propertiesPath, commandLineFallbacks) ;
        setPropertiesForDisplay();

        // check property options
        if (getNetworkMapFile() == null) {
            System.err.println("GuiSimulationLauncher: map file is " +
                    "required.");
            return;
        } else if (!((File) new File(getNetworkMapFile())).exists()) {
            System.err.println("GuiSimulationLauncher: specified map file does " +
                    "not exist.");
            return;
        }
        try {
            networkMap = readMapWithName(getNetworkMapFile()) ;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        settings = _settings;
        exitOnClose = true;
    }

    /**
     * マップエディタからシミュレーションを開始する時に用いるコンストラクタ.
     */
    public GuiSimulationLauncher(Random random, CrowdWalkPropertiesHandler _properties,
            SetupFileInfo _setupFileInfo, NetworkMap _networkMap, Settings _settings) {
        super(random) ;
        properties = _properties;
        setPropertiesForDisplay();
        setupFileInfo = _setupFileInfo;
        networkMap = _networkMap;
        settings = _settings;
        // ending condition
        setExitCount(properties.getExitCount()) ;
        setIsAllAgentSpeedZeroBreak(properties.getIsAllAgentSpeedZeroBreak());
    }

    private transient Runnable simulationRunnable = null;

    protected void simulate() {
        // 既に終わっていたら、警告メッセージ
        if (!finished) {
            JOptionPane.showMessageDialog(null,
                    "Previous simulation not finished?",
                    "Could not start simulation",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.gc();

        // シミュレータの実体の初期化
        initializeSimulatorEntity() ;

        System.gc();

        // メインループの Runnable 作成。
        simulationRunnable = new Runnable() {
            public void run() {
                simulateMainLoop() ;
                synchronized (simulationRunnable) {
                    paused = true ;
                }
            }
        };
    }

    private void quit() {
        Itk.logInfo("Simulation window closed.") ;
        // TODO: 
        // settings.put("launcher_width", simulation_frame.getWidth());
        // settings.put("launcher_height", simulation_frame.getHeight());
        // if (panel != null) {
        //     settings.put("3dpanel_width", panel.getWidth());
        //     settings.put("3dpanel_height", panel.getHeight());
        // }
        if (exitOnClose) {
            System.exit(0);
        }
    }

    @Override
    public void start() {
        synchronized (simulationRunnable) {
            if (paused) {
                paused = false ;
                Thread thread = new Thread(simulationRunnable);
                thread.start();
            }
        }
    }

    @Override
    public void pause() {
        synchronized (simulationRunnable) {
            paused = true ;
        }
    }

    @Override
    public void step() {
        synchronized (simulationRunnable) {
            simulateOneStepBare() ;
        }
    }

    @Override
    public boolean isRunning() {
        return !paused;
    }

    protected transient SimulationPanel3D panel = null;
    protected transient JFrame simulation_frame = null;

    @Override
    public SimulationPanel3D setupFrame(final EvacuationSimulator simulator) {
        simulation_frame = new JFrame("Simulation Preview");

        simulation_frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
                simulationWindowOpenedOperation(panel, simulator);
            }
            public void windowIconified(WindowEvent e) {            }
            public void windowDeiconified(WindowEvent e) {          }
            public void windowDeactivated(WindowEvent e) {          }
            public void windowClosing(WindowEvent e) {
                simulation_frame.dispose();
            }
            public void windowActivated(WindowEvent e) {            }
            public void windowClosed(WindowEvent e) {
                quit();
            }
        });

        setup_control_panel(getGenerationFile(), getScenarioFile(), networkMap);

        panel = new SimulationPanel3D(simulator, simulation_frame);
        initSimulationPanel3D(panel);
        int w = settings.get("3dpanel_width", 800);
        int h = settings.get("3dpanel_height", 600);
        panel.setCanvasSize(w, h);
        panel.initialize();
        simulation_frame.setLayout(new BorderLayout());
        simulation_frame.add(panel, BorderLayout.WEST);
        JTabbedPane tabs = new JTabbedPane();
        simulation_frame.add(tabs, BorderLayout.CENTER);

        tabs.add(control_panel);
        tabs.add(panel.getControlPanel());
        simulation_frame.setMenuBar(panel.getMenuBar());
        simulation_frame.setResizable(false);   // ※setResizable は pack の前に置かないとサイズがおかしくなる。
        simulation_frame.pack();
        simulation_frame.setVisible(true);
        return panel;
    }

    //------------------------------------------------------------
    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJLabel(JPanel panel, int x, int y, int width, int height, int anchor, JLabel label) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = anchor;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.insets = new Insets(0, 12, 0, 12);
        ((GridBagLayout)panel.getLayout()).setConstraints(label, gbc);
        panel.add(label);
    }

    //------------------------------------------------------------
    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJLabel(JPanel panel, int x, int y, int width, int height, JLabel label) {
        addJLabel(panel, x, y, width, height, GridBagConstraints.WEST, label);
    }

    //------------------------------------------------------------
    /**
     * シミュレーションウィンドウの Control タブの準備
     */
    private void setup_control_panel(String generationFileName,
                                     String scenarioFileName,
                                     NetworkMap map) {
        control_panel = new JPanel();
        control_panel.setName("Control");
        control_panel.setLayout(new BorderLayout());

        JPanel top_panel = new JPanel();
        top_panel.setLayout(new BorderLayout());

        /* title & clock */
        JPanel titlepanel = new JPanel(new GridBagLayout());
        addJLabel(titlepanel, 0, 0, 1, 1, GridBagConstraints.EAST, new JLabel("Map"));
        if (getNetworkMapFile() != null) {
            File map_file = new File(getNetworkMapFile());
            addJLabel(titlepanel, 1, 0, 1, 1, new JLabel(map_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 0, 1, 1, new JLabel("No map file"));
        }

        addJLabel(titlepanel, 0, 1, 1, 1, GridBagConstraints.EAST, new JLabel("Agent"));
        if (generationFileName != null) {
            File generation_file = new File(generationFileName);
            addJLabel(titlepanel, 1, 1, 1, 1, new JLabel(generation_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 1, 1, 1, new JLabel("No generation file"));
        }

        addJLabel(titlepanel, 0, 2, 1, 1, GridBagConstraints.EAST,
                  new JLabel("Scenario"));
        if (scenarioFileName != null) {
            File scenario_file = new File(scenarioFileName);
            addJLabel(titlepanel, 1, 2, 1, 1, new JLabel(scenario_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 2, 1, 1, new JLabel("No scenario file"));
        }

        addJLabel(titlepanel, 0, 3, 1, 1, GridBagConstraints.EAST,
                  new JLabel("Pollution"));
        if (getPollutionFile() != null) {
            File pollution_file = new File(getPollutionFile());
            addJLabel(titlepanel, 1, 3, 1, 1,
                      new JLabel(pollution_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 3, 1, 1, new JLabel("No pollution file"));
        }

        addJLabel(titlepanel, 0, 4, 1, 1, GridBagConstraints.EAST,
                  new JLabel("ID"));
        /* [2015.02.10 I.Noda] remove scenario_numbers */
        JLabel sl = new JLabel("(NOT DEFINED)");
        sl.setFont(new Font(null, Font.ITALIC, 9));
        addJLabel(titlepanel, 1, 4, 1, 1, sl);

        clock_label.setHorizontalAlignment(JLabel.CENTER);
        clock_label.setFont(new Font("Lucida", Font.BOLD, 18));
        addJLabel(titlepanel, 0, 5, 1, 1, GridBagConstraints.CENTER, clock_label);

        time_label.setHorizontalAlignment(JLabel.LEFT);
        time_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJLabel(titlepanel, 1, 5, 1, 1, time_label);

        evacuatedCount_label.setHorizontalAlignment(JLabel.LEFT);
        evacuatedCount_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJLabel(titlepanel, 0, 6, 2, 1, GridBagConstraints.CENTER, evacuatedCount_label);
        top_panel.add(titlepanel, BorderLayout.NORTH);

        /* scenarios */
        JPanel label_toggle_panel = new JPanel(new GridBagLayout());
        label_toggle_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        GridBagConstraints c = null;

        int max_events = 1;

        int y = 0;
        for (EventBase event : simulator.getScenario().eventList) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.fill = GridBagConstraints.WEST;

            ButtonGroup bgroup = new ButtonGroup();
            toggle_scenario_button_groups.add(bgroup);
            class RadioButtonListener implements ActionListener {
                int index;
                EventBase event ;
                NetworkMap map ;
                public RadioButtonListener(EventBase _event,
                                           int _index,
                                           NetworkMap _map) {
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

            if (false) {
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y;
                c.gridwidth = max_events + 3;
                c.fill = GridBagConstraints.EAST;
                label_toggle_panel.add(new JLabel(event.atTime.getAbsoluteTimeString()), c);
            } else {
                JRadioButton radio_button;
                radio_button = new JRadioButton("enabled");
                radio_button.addActionListener(new RadioButtonListener(event, -2, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button = new JRadioButton("disabled | auto:");
                radio_button.addActionListener(new RadioButtonListener(event, -1, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button = new JRadioButton(event.atTime.getAbsoluteTimeString()) ;
                radio_button.addActionListener(new RadioButtonListener(event, 0, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 3;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button.setSelected(true);
            }
            y++;
        }
        JScrollPane scroller = new JScrollPane(label_toggle_panel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(400, 230));
        top_panel.add(scroller, BorderLayout.CENTER);

        JPanel control_button_panel = new JPanel(new FlowLayout());
        ImageIcon start_icon = new ImageIcon(getClass().getResource("/img/start.png"));
        ImageIcon pause_icon = new ImageIcon(getClass().getResource("/img/pause.png"));
        ImageIcon step_icon = new ImageIcon(getClass().getResource("/img/step.png"));

        start_button = new JButton(start_icon);
        start_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { simulator.start(); update_buttons(); }
        });
        control_button_panel.add(start_button);
        pause_button = new JButton(pause_icon);
        pause_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { simulator.pause(); update_buttons(); }
        });
        control_button_panel.add(pause_button);
        step_button = new JButton(step_icon);
        step_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { simulator.step(); update_buttons(); }
        });
        control_button_panel.add(step_button);
        update_buttons();

        control_button_panel.add(new JLabel("wait: "));
        simulationDeferFactorControl =
            new JScrollBar(JScrollBar.HORIZONTAL,
                           simulator.getSimulationDeferFactor(),
                           1, 0, 300);
        simulationDeferFactorControl.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) { changeSimulationDeferFactor(); }
        });
        simulationDeferFactorControl.setPreferredSize(new Dimension(150, 20));
        control_button_panel.add(simulationDeferFactorControl);
        simulationDeferFactorValue = new JLabel();
        simulationDeferFactorValue.setHorizontalAlignment(JLabel.RIGHT);
        simulationDeferFactorValue.setPreferredSize(new Dimension(30, 10));
        simulationDeferFactorValue.setText("" + simulator.getSimulationDeferFactor());
        control_button_panel.add(simulationDeferFactorValue);

        top_panel.add(control_button_panel, BorderLayout.SOUTH);
        control_panel.add(top_panel, BorderLayout.CENTER);

        /* text message */
        message.setEditable(false);
        message.setAutoscrolls(true);
        JScrollPane message_scroller = new JScrollPane(message,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        message_scroller.setPreferredSize(new Dimension(300, 160));

        control_panel.add(message_scroller, BorderLayout.SOUTH);
    }

    //------------------------------------------------------------
    /**
     * スタートボタン
     */
    public JButton getStartButton() {
        return start_button;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション遅延の制御（画面）
     */
    private void changeSimulationDeferFactor() {
        simulator.setSimulationDeferFactor(simulationDeferFactorControl.getValue());
        simulationDeferFactorValue.setText("" + simulator.getSimulationDeferFactor());
    }

    //------------------------------------------------------------
    /**
     * シミュレーション遅延の制御（画面）
     */
    public void setSimulationDeferFactor(int deferFactor) {
        simulator.setSimulationDeferFactor(deferFactor);
        simulationDeferFactorControl.setValue(simulator.getSimulationDeferFactor());
    }

    //------------------------------------------------------------
    /**
     * ボタン類のアップデート
     */
    public void update_buttons() {
        boolean is_running = simulator.isRunning();
        if (start_button != null) {
            start_button.setEnabled(!is_running);
        }
        if (pause_button != null) {
            pause_button.setEnabled(is_running);
        }
        if (step_button != null) {
            step_button.setEnabled(!is_running);
        }
    }

    //------------------------------------------------------------
    /**
     * 時計表示
     */
    public void displayClock(SimTime currentTime) {
        String time_string = String.format("Elapsed: %5.2fsec",
                                           currentTime.getRelativeTime());
        time_label.setText(time_string);
        String clock_string = currentTime.getAbsoluteTimeString() ;
        clock_label.setText(clock_string);
    }

    //------------------------------------------------------------
    /**
     * evacuation count の計算
     */
    public void updateEvacuatedCount(AgentHandler agentHandler) {
        String evacuatedCount_string;
        if (agentHandler.numOfStuckAgents() == 0) {
            evacuatedCount_string = String.format(
                    "Walking: %d  Generated: %d  Evacuated: %d / %d",
                    agentHandler.numOfWalkingAgents(),
                    agentHandler.numOfAllAgents(),
                    agentHandler.numOfEvacuatedAgents(), agentHandler.getMaxAgentCount());
        } else {
            evacuatedCount_string = String.format(
                    "Walking: %d  Generated: %d  Evacuated(Stuck): %d(%d) / %d",
                    agentHandler.numOfWalkingAgents(),
                    agentHandler.numOfAllAgents(),
                    agentHandler.numOfEvacuatedAgents() - agentHandler.numOfStuckAgents(),
                    agentHandler.numOfStuckAgents(),
                    agentHandler.getMaxAgentCount());
        }
        evacuatedCount_label.setText(evacuatedCount_string);
        SimulationPanel3D.updateEvacuatedCount(evacuatedCount_string);
    }

    // シミュレーションウィンドウが最初に表示された時に呼び出される
    public void simulationWindowOpenedOperation(SimulationPanel3D panel, final EvacuationSimulator simulator) {
        // プロパティファイルに設定された情報に従ってシミュレーションウィンドウの各コントロールの初期設定をおこなう
        if (properties == null) {
            return;
        }
        boolean successful = true;
        if (properties.isDefined("defer_factor")) {
            setSimulationDeferFactor(deferFactor);
        }
        if (cameraFile != null) {
            if (panel.loadCameraworkFromFile(cameraFile)) {
                panel.setReplay(true);
            } else {
                System.err.println("Camera file の読み込みに失敗しました: " + cameraFile);
                successful = false;
            }
        }
        if (properties.isDefined("vertical_scale")) {
            panel.setVerticalScale(verticalScale);
        }
        if (properties.isDefined("record_simulation_screen")) {
            if (recordSimulationScreen != panel.getRecordSnapshots().isSelected()) {
                panel.getRecordSnapshots().doClick();
            }
        }
        if (properties.isDefined("screenshot_dir")) {
            panel.setScreenshotDir(screenshotDir);
        }
        if (properties.isDefined("clear_screenshot_dir")) {
            if (recordSimulationScreen && clearScreenshotDir) {
                FilePathManipulation.deleteFiles(screenshotDir, imageFileFilter);
            }
        }
        if (properties.isDefined("screenshot_image_type")) {
            panel.setScreenshotImageType(screenshotImageType);
        }
        if (properties.isDefined("hide_links")) {
            if (hideLinks != panel.getHideNormalLink().isSelected()) {
                panel.getHideNormalLink().doClick();
            }
        }
        if (properties.isDefined("density_mode")) {
            if (densityMode != panel.getDensityMode().isSelected()) {
                panel.getDensityMode().doClick();
            }
        }
        if (properties.isDefined("change_agent_color_depending_on_speed")) {
            if (changeAgentColorDependingOnSpeed != panel.getChangeAgentColorDependingOnSpeed().isSelected()) {
                panel.getChangeAgentColorDependingOnSpeed().doClick();
            }
        }
        if (properties.isDefined("show_status")) {
            if (showStatus != panel.getShowStatus().isSelected()) {
                panel.getShowStatus().doClick();
            }
            if (showStatusPosition.equals("top") && panel.getBottom().isSelected()) {
                panel.getTop().doClick();
            }
            if (showStatusPosition.equals("bottom") && panel.getTop().isSelected()) {
                panel.getBottom().doClick();
            }
        }
        if (properties.isDefined("show_logo")) {
            if (showLogo != panel.getShowLogo().isSelected()) {
                panel.getShowLogo().doClick();
            }
        }
        if (properties.isDefined("show_3D_polygon")) {
            if (show3dPolygon != panel.getShow3dPolygon().isSelected()) {
                panel.getShow3dPolygon().doClick();
            }
        }
        if (properties.isDefined("auto_simulation_start")) {
            if (successful && autoSimulationStart) {
                // ※スクリーンショットの1枚目が真っ黒になるのを防ぐため
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            // シミュレーション画面が表示されるのを待つ
                            Thread.sleep(1000);
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Itk.logInfo("auto simulation start");
                                getStartButton().doClick();
                            }
                        });
                    }
                });
                thread.start();
            }
        }
    }

    // SimulationPanel3D を生成した直後に呼び出される(simulationWindowOpenedOperation ではうまく対処できない分の処理)
    public void initSimulationPanel3D(SimulationPanel3D panel) {
        if (properties == null) {
            return;
        }
        if (properties.isDefined("agent_size")) {
            panel.setAgentSize(agentSize);
        }
        if (properties.isDefined("zoom")) {
            panel.setScaleOnReplay(zoom);
        }
    }

    //------------------------------------------------------------
    /**
     * 画面出力用properties設定
     */
    private void setPropertiesForDisplay() {
        try {
            deferFactor = properties.getInteger("defer_factor", deferFactor);
            if (deferFactor < 0 || deferFactor > 299) {
                throw new Exception("Property error - 設定値が範囲(0～299)外です: defer_factor:" + deferFactor);
            }
            verticalScale = properties.getDouble("vertical_scale", verticalScale);
            if (verticalScale < 0.1 || verticalScale > 49.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～49.9)外です: vertical_scale:" + verticalScale);
            }
            agentSize = properties.getDouble("agent_size", agentSize);
            if (agentSize < 0.1 || agentSize > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～9.9)外です: agent_size:" + agentSize);
            }
            zoom = properties.getDouble("zoom", zoom);
            if (zoom < 0.0 || zoom > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.0～9.9)外です: zoom:" + zoom);
            }
            cameraFile = properties.getFilePath("camera_file", null);
            recordSimulationScreen = properties.getBoolean("record_simulation_screen", recordSimulationScreen);
            screenshotDir = properties.getDirectoryPath("screenshot_dir", screenshotDir).replaceFirst("[/\\\\]+$", "");
            clearScreenshotDir = properties.getBoolean("clear_screenshot_dir", clearScreenshotDir);
            if (clearScreenshotDir && ! properties.isDefined("screenshot_dir")) {
                throw new Exception("Property error - clear_screenshot_dir を有効にするためには screenshot_dir の設定が必要です。");
            }
            if (recordSimulationScreen && ! clearScreenshotDir && new File(screenshotDir).list(imageFileFilter).length > 0) {
                throw new Exception("Property error - スクリーンショットディレクトリに画像ファイルが残っています: screenshot_dir:" + screenshotDir);
            }
            screenshotImageType = properties.getString("screenshot_image_type", screenshotImageType, IMAGE_TYPES);
            hideLinks = properties.getBoolean("hide_links", hideLinks);
            densityMode = properties.getBoolean("density_mode", densityMode);
            changeAgentColorDependingOnSpeed =
                properties.getBoolean("change_agent_color_depending_on_speed", changeAgentColorDependingOnSpeed);
            String show_status = properties.getString("show_status", "none", SHOW_STATUS_VALUES);
            if (show_status.equals("none")) {
                showStatus = false;
            } else {
                showStatus = true;
                showStatusPosition = show_status;
            }
            showLogo = properties.getBoolean("show_logo", showLogo);
            show3dPolygon = properties.getBoolean("show_3D_polygon", show3dPolygon);
            simulationWindowOpen = properties.getBoolean("simulation_window_open", simulationWindowOpen);
            autoSimulationStart = properties.getBoolean("auto_simulation_start", autoSimulationStart);
        } catch(Exception e) {
            //System.err.printf("Property file error: %s\n%s\n", _propertiesFile, e.getMessage());
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    // 画像ファイルか?
    private FilenameFilter imageFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            for (String suffix : IMAGE_TYPES) {
                if (name.endsWith("." + suffix)) {
                    return true;
                }
            }
            return false;
        }
    };

    /**
     * simulator.begin() をバックグラウンドで実行するためのモーダルダイアログ
     */
    private class WaitDialog extends JDialog {
        public boolean canceled = false;

        public WaitDialog(Frame owner, String title, String message, final Thread thread) {
            super(owner, title, true);

            JPanel panel = new JPanel(new FlowLayout());
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancelButton.setEnabled(false);
                    canceled = true;
                    threadCancel(thread);
                }
            });
            panel.add(cancelButton);

            Container c = getContentPane();
            c.add(new JLabel(message), BorderLayout.CENTER);
            c.add(panel, BorderLayout.PAGE_END);

            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                public void windowOpened(WindowEvent e) {
                    thread.start();
                }

                public void windowClosing(WindowEvent e) {
                    if (canceled) {
                        return;
                    }
                    canceled = true;
                    cancelButton.setEnabled(false);
                    threadCancel(thread);
                }
            });

            setSize(300, 128);
            setLocationRelativeTo(owner);
        }

        public void threadCancel(Thread thread) {
            // ※ビルドの中断機能は未実装のため、現状では処理終了を待つだけ
            try {
                thread.join();
            } catch(InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private WaitDialog waitDialog;

    public boolean buildModel() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                simulator.begin() ;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // ここでダイアログを閉じる
                        waitDialog.setVisible(false);
                        waitDialog.dispose();
                    }
                });
            }
        });
        waitDialog = new WaitDialog(simulation_frame, "Information", " Simulation model building...", thread);
        waitDialog.setVisible(true);
        return ! waitDialog.canceled;
    }
}
