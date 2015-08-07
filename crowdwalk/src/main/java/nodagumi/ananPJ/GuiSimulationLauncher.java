// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.Insets;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import javax.swing.border.EtchedBorder;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationController;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.misc.FilePathManipulation;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;

import nodagumi.Itk.*;

public class GuiSimulationLauncher extends BasicSimulationLauncher
    implements SimulationController {

    protected transient Settings settings;

    /* the constructor without arguments are for to be used as
     *  a base class for classes launching simulations */
    public GuiSimulationLauncher(Random _random) {
        super(_random) ;
        networkMap = new NetworkMap() ;
        settings = Settings.load("GuiSimulationLauncher.ini");
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

        // シミュレータの実体の初期化
        initializeSimulatorEntity() ;

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
        settings.put("launcher_width", simulation_frame.getWidth());
        settings.put("launcher_height", simulation_frame.getHeight());
        if (panel != null) {
            settings.put("3dpanel_width", panel.getWidth());
            settings.put("3dpanel_height", panel.getHeight());
        }
        Settings.save();
        simulation_frame.dispose();

        System.exit(0);
    }

    public void windowClosing(WindowEvent e) {
        quit();
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
                finished = true;
            }
            public void windowActivated(WindowEvent e) {            }
            public void windowClosed(WindowEvent e) {           }
        });

        setup_control_panel(getGenerationFile(), getScenarioFile(), networkMap);

        panel = new SimulationPanel3D(simulator, simulation_frame);
        initSimulationPanel3D(panel);
        int w = settings.get("3dpanel_width", 800);
        int h = settings.get("3dpanel_height", 600);
        panel.setCanvasSize(w, h);
        panel.initialize();
        simulation_frame.setLayout(new BorderLayout());
        simulation_frame.add(panel, BorderLayout.CENTER);
        JTabbedPane tabs = new JTabbedPane();
        simulation_frame.add(tabs, BorderLayout.EAST);

        tabs.add(control_panel);
        tabs.add(panel.getControlPanel());
        simulation_frame.setMenuBar(panel.getMenuBar());
        simulation_frame.setResizable(false);   // ※setResizable は pack の前に置かないとサイズがおかしくなる。
        simulation_frame.pack();
        simulation_frame.setVisible(true);
        return panel;
    }

    public void simulationWindowOpenedOperation(SimulationPanel3D panel, 
						final EvacuationSimulator simulator) {
        // GuiSimulationEditorLauncher で定義する
    }

    public void initSimulationPanel3D(SimulationPanel3D panel) {
        // GuiSimulationEditorLauncher で定義する
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

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
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

    //------------------------------------------------------------
    /**
     * 制御パネルの準備
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
}
