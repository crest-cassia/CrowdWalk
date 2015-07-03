// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationController;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.misc.FilePathManipulation;
import nodagumi.ananPJ.misc.NetmasTimer;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;

import nodagumi.Itk.*;

public class SimulationLauncher extends BasicSimulationLauncher
    implements SimulationController {

    protected transient Settings settings;
    private transient JFrame main_frame;

    /* the constructor without arguments are for to be used as
     *  a base class for classes launching simulations */
    public SimulationLauncher(Random _random) {
        super(_random) ;
        networkMap = new NetworkMap() ;
        settings = Settings.load("NetworkMapEditor.ini");
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
        settings.put("launcher_width", main_frame.getWidth());
        settings.put("launcher_height", main_frame.getHeight());
        if (panel != null) {
            settings.put("3dpanel_width", panel.getWidth());
            settings.put("3dpanel_height", panel.getHeight());
        }
        Settings.save();
        main_frame.dispose();

        System.exit(0);
    }

    /* @override */
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

        tabs.add(simulator.getAgentHandler().getControlPanel());
        tabs.add(panel.getControlPanel());
        simulation_frame.setMenuBar(panel.getMenuBar());
        simulation_frame.setResizable(false);   // ※setResizable は pack の前に置かないとサイズがおかしくなる。
        simulation_frame.pack();
        simulation_frame.setVisible(true);
        return panel;
    }

    @Override
    public SimulationPanel3D setupFrame(final EvacuationSimulator simulator,
            SimulationPanel3D _panel) {
        if (simulator == null)
            return null;
        if (_panel == null) {
            setupFrame(simulator);
        } else {
            panel = _panel;
            simulation_frame = panel.parent;
            panel.setupFrame(simulator, simulation_frame);
            int w = settings.get("3dpanel_width", 800);
            int h = settings.get("3dpanel_height", 600);
            panel.setCanvasSize(w, h);
            panel.initialize();
            // simulation_frame.setLayout(new BorderLayout());
            // simulation_frame.add(panel, BorderLayout.CENTER);
            // simulation_frame.setMenuBar(panel.getMenuBar());
            // simulation_frame.pack();
            // simulation_frame.setVisible(true);
            //panel.deserialize(simulator, simulation_frame);
        }

        return panel;
    }

    public void simulationWindowOpenedOperation(SimulationPanel3D panel, 
						final EvacuationSimulator simulator) {
        // NetworkMapEditor で定義する
    }

    public void initSimulationPanel3D(SimulationPanel3D panel) {
        // NetworkMapEditor で定義する
    }

}
