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
    private boolean isAllAgentSpeedZeroBreak = false;

    private SpeedCalculationModel speedModel = null;

    protected NetworkMap networkMap;
    public NetworkMap getMap() { return networkMap; }

    /* the constructor without arguments are for to be used as
     *  a base class for classes launching simulations */
    public SimulationLauncher(Random _random) {
        super(_random) ;
        networkMap = new NetworkMap(_random);
        settings = Settings.load("NetworkMapEditor.ini");
    }

    private transient JLabel directory_label = new JLabel();
    private transient JButton model_file_button = new JButton();
    private transient JLabel model_filename_label = new JLabel();
    private transient JButton generation_file_button = new JButton();
    private transient JLabel generation_filename_label = new JLabel();
    private transient JButton scenario_file_button = new JButton();
    private transient JLabel scenario_filename_label = new JLabel();

    private Boolean run_thread = false;
    private transient Runnable run_simulation = null;

    protected void simulate(boolean isDeserialized) {
        if ((!isDeserialized) && (!finished)) {
            JOptionPane.showMessageDialog(null,
                    "Previous simulation not finished?",
                    "Could not start simulation",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isDeserialized) {
            System.out.println("SimulationLauncher.simulate:finished: " +
                    finished);
            finished = false;
            simulator.begin(true, isDeserialized);
        } else {
	    /* [2015-02-06 I.Noda]
	     * ここで読み込むのが正しいか、不明
	     */
	    networkMap.scanFallbackFile(true) ;

            simulator = new EvacuationSimulator(networkMap, this, random);
            finished = false;
            simulator.setup();
            buildModel();
            simulator.buildDisplay();
	    simulator.setIsAllAgentSpeedZeroBreak(isAllAgentSpeedZeroBreak);
        }
        if (isTimerEnabled) {
            timer = new NetmasTimer(10, timerPath);
            timer.start();
        }
        run_simulation = new Runnable() {
            public void run() {
                while(!finished && run_thread) {
                    simulateOneStepBare() ;
                }
                synchronized (run_thread) {
                    run_thread = false;
                }
                if (isTimerEnabled) {
                    timer.writeElapsed();
                    timer.stop();
                }
            }
        };
    }

    protected boolean buildModel() {
	simulator.begin(true, false, null);
        return true;
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
        synchronized (run_thread) {
            if (run_thread == false) {
                run_thread = true;
                Thread thread = new Thread(run_simulation);
                thread.start();
            }
        }
    }

    @Override
    public void pause() {
        synchronized (run_thread) {
            run_thread = false;
        }
    }

    @Override
    public void step() {
        synchronized (run_thread) {
            finished = simulator.updateEveryTick();
            if (isTimeSeriesLog) {
                simulator.saveGoalLog(timeSeriesLogPath, false);    // GUIモードでは出力対象外なので無意味
                if (((int) simulator.getSecond()) % timeSeriesLogInterval == 0)
                    simulator.saveTimeSeriesLog(timeSeriesLogPath);
            }
            if (isTimerEnabled) {
                timer.tick();
                timer.writeInterval();
                if ((simulator.getSecond() % 60) == 0)
                    timer.writeElapsed();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return run_thread;
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

    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        isAllAgentSpeedZeroBreak = _isAllAgentSpeedZeroBreak;
    }

    public void setRandom(Random _random) {
        random = _random;
        networkMap.setRandom(_random);
        simulator.setRandom(_random);
    }

    public void setSpeedModel(SpeedCalculationModel _speedModel) {
        speedModel = _speedModel;
    }

    public SpeedCalculationModel getSpeedModel() {
        return speedModel;
    }

    /**
     * Set isTimeSeriesLog.
     * @param _isTimeSeriesLog the value to set.
     */
    public void setIsTimeSeriesLog(boolean _isTimeSeriesLog) {
        isTimeSeriesLog = _isTimeSeriesLog;
    }

    /**
     * Get isTimeSeriesLog.
     * @return isTimeSeriesLog as boolean.
     */
    public boolean getIsTimeSeriesLog() {
        return isTimeSeriesLog;
    }

    /**
     * Set timeSeriesLogPath.
     * @param _timeSeriesLogPath the value to set.
     */
    public void setTimeSeriesLogPath(String _timeSeriesLogPath) {
        timeSeriesLogPath = _timeSeriesLogPath;
    }

    /**
     * Get timeSeriesLogPath.
     * @return timeSeriesLogPath as String.
     */
    public String getTimeSeriesLogPath() {
        return timeSeriesLogPath;
    }

    /**
     * Set timeSeriesLogInterval.
     * @param _timeSeriesLogInterval the value to set.
     */
    public void setTimeSeriesLogInterval(int _timeSeriesLogInterval) {
        timeSeriesLogInterval = _timeSeriesLogInterval;
    }

    /**
     * Get timeSeriesLogInterval.
     * @return timeSeriesLogInterval as int.
     */
    public int getTimeSeriesLogInterval() {
        return timeSeriesLogInterval;
    }

    public void setIsTimerEnabled(boolean _isTimerEnabled) {
        isTimerEnabled = _isTimerEnabled;
    }

    public boolean getIsTimerEnabled() {
        return isTimerEnabled;
    }

    public void setTimerFile(String _timerPath) {
        timerPath = _timerPath;
    }

    public String getTimerFile() {
        return timerPath;
    }
}
