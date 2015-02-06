package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Simulator.DumpState;
import nodagumi.ananPJ.Simulator.EvacuationModelBase;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationController;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.misc.FilePathManipulation;
import nodagumi.ananPJ.misc.NetmasTimer;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;


public class SimulationLauncher extends BasicSimulationLauncher
    implements SimulationController, Serializable {

    private static final long serialVersionUID = 3014238657739616307L;

    protected transient Settings settings;
    private transient JFrame main_frame;
    String scenario_name = new String();
    protected Random random = null;
    private boolean isAllAgentSpeedZeroBreak = false;
    private NetmasTimer timer = null;
    private boolean isTimerEnabled = false;
    private String timerPath = null;
    private boolean isTimeSeriesLog = false;
    private String timeSeriesLogPath = null;
    private int timeSeriesLogInterval = -1;
    private boolean isDamageSpeedZeroNumberLog = false;
    private String damageSpeedZeroNumberLogPath = null;

    private SpeedCalculationModel speedModel = null;
    private String fusionViewerLogPath = "log";
    private boolean saveFusionViewerLogOption = false;
    private boolean sendFusionViewerLogOption = false;

    protected NetworkMap networkMap;
    public NetworkMap getMap() { return networkMap; }
    public EvacuationModelBase getModel() { return model; }

    /* the constructor without arguments are for to be used as
     *  a base class for classes launching simulations */
    public SimulationLauncher(Random _random) {
        random = _random;
        networkMap = new NetworkMap(_random);
        settings = Settings.load("NetworkMapEditor.ini");
    }

    public void deserialize() {
        settings = Settings.load("NetworkMapEditor.ini");
    }

    private transient JLabel directory_label = new JLabel();
    private transient JButton model_file_button = new JButton();
    private transient JLabel model_filename_label = new JLabel();
    private transient JButton generation_file_button = new JButton();
    private transient JLabel generation_filename_label = new JLabel();
    private transient JButton response_file_button = new JButton();
    private transient JLabel response_filename_label = new JLabel();

    private boolean finished = true;
    private Boolean run_thread = false;
    private transient Runnable run_simulation = null;
    protected EvacuationSimulator model = null;

    protected void simulate(boolean isDeserialized) {
        if ((!isDeserialized) && (!finished)) {
            JOptionPane.showMessageDialog(null,
                    "Previous simulation not finished?",
                    "Could not start simulation",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isDeserialized) {
            System.out.println("SimulationLauncher.simulate:scenario_name: " +
                    this.scenario_name);
            System.out.println("SimulationLauncher.simulate:finished: " +
                    finished);
            finished = false;
            model.begin(true, isDeserialized);
        } else {
            Date date = new Date();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
            String model_path = networkMap.getFileName();
            if (model_path == null) { model_path = "tmp"; }
            File model_file = new File(model_path);
            String model_filename = model_file.getName();
            scenario_name = model_filename + format.format(date);
            model = new EvacuationSimulator(networkMap, this, scenario_name, random);
            finished = false;
            model.setup();
            buildModel();
            model.buildDisplay();
            model.setIsAllAgentSpeedZeroBreak(isAllAgentSpeedZeroBreak);
        }
        if (isTimerEnabled) {
            timer = new NetmasTimer(10, timerPath);
            timer.start();
        }
        if (sendFusionViewerLogOption) {
            System.err.println("call waitConnectionFusionViewer");
            ((AgentHandler) model.getAgentHandler())
                .waitConnectionFusionViewer();
        }
        run_simulation = new Runnable() {
            public void run() {
                while(!finished && run_thread) {
                    dump_state.preUpdate();
                    finished = model.updateEveryTick();
                    dump_state.postUpdate();
                    boolean isTimezero = false;
                    if (model.getSecond() == 0)
                        isTimezero = true;
                    if (isTimeSeriesLog) {
                        if (((int) model.getSecond()) % timeSeriesLogInterval
                                == 0)
                            model.saveTimeSeriesLog(timeSeriesLogPath);
                    }
                    if (isDamageSpeedZeroNumberLog) {
                        model.saveSimpleGoalLog("tmp", isTimezero);
                        model.saveDamagedSpeedZeroNumberLog(
                                damageSpeedZeroNumberLogPath, isTimezero);
                    }
                    if (isTimerEnabled) {
                        timer.tick();
                        timer.writeInterval();
                        if ((model.getSecond() % 60) == 0)
                            timer.writeElapsed();
                    }
                    if (saveFusionViewerLogOption)
                        ((AgentHandler) model.getAgentHandler())
                            .saveFusionViewerLog(fusionViewerLogPath,
                                    model.getSecond(), (int) model.getSecond());
                    if (sendFusionViewerLogOption)
                        ((AgentHandler) model.getAgentHandler())
                            .sendFusionViewerLog(model.getSecond(),
                                    (int) model.getSecond());
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
        model.begin(true, false, null);
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
            dump_state.preUpdate();
            finished = model.updateEveryTick();
            dump_state.postUpdate();
            boolean isTimezero = false;
            if (model.getSecond() == 0)
                isTimezero = true;
            if (isTimeSeriesLog) {
                model.saveGoalLog(timeSeriesLogPath, false);    // GUIモードでは出力対象外なので無意味
                if (((int) model.getSecond()) % timeSeriesLogInterval == 0)
                    model.saveTimeSeriesLog(timeSeriesLogPath);
            }
            if (isTimerEnabled) {
                timer.tick();
                timer.writeInterval();
                if ((model.getSecond() % 60) == 0)
                    timer.writeElapsed();
            }
            if (saveFusionViewerLogOption)
                ((AgentHandler) model.getAgentHandler())
                    .saveFusionViewerLog(fusionViewerLogPath, model.getSecond(),
                            (int) model.getSecond());
            if (sendFusionViewerLogOption)
                ((AgentHandler) model.getAgentHandler()).sendFusionViewerLog(
                    model.getSecond(), (int) model.getSecond());
        }
    }

    @Override
    public boolean isRunning() {
        return run_thread;
    }

    protected transient SimulationPanel3D panel = null;
    protected transient JFrame simulation_frame = null;
    protected transient DumpState dump_state = null;

    @Override
    public SimulationPanel3D setupFrame(final EvacuationModelBase model) {
        simulation_frame = new JFrame("Simulation Preview");

        simulation_frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
                simulationWindowOpenedOperation(panel, model);
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

        panel = new SimulationPanel3D(model, simulation_frame);
        initSimulationPanel3D(panel);
        int w = settings.get("3dpanel_width", 800);
        int h = settings.get("3dpanel_height", 600);
        panel.setCanvasSize(w, h);
        panel.initialize();
        simulation_frame.setLayout(new BorderLayout());
        simulation_frame.add(panel, BorderLayout.CENTER);
        JTabbedPane tabs = new JTabbedPane();
        simulation_frame.add(tabs, BorderLayout.EAST);
        panel.addViewChangeListener(this);

        dump_state = new DumpState(model);

        tabs.add(model.getAgentHandler().getControlPanel());
        tabs.add(panel.getControlPanel());
        tabs.add(dump_state.getDumpPanel());
        simulation_frame.setMenuBar(panel.getMenuBar());
        simulation_frame.pack();
        simulation_frame.setVisible(true);

        // tkokada
        simulation_frame.setResizable(false);
        return panel;
    }

    @Override
    public SimulationPanel3D setupFrame(final EvacuationModelBase model,
            SimulationPanel3D _panel) {
        if (model == null)
            return null;
        if (_panel == null) {
            setupFrame(model);
        } else {
            panel = _panel;
            simulation_frame = panel.parent;
            panel.deserialize(model, simulation_frame);
            int w = settings.get("3dpanel_width", 800);
            int h = settings.get("3dpanel_height", 600);
            panel.setCanvasSize(w, h);
            panel.initialize();
            // simulation_frame.setLayout(new BorderLayout());
            // simulation_frame.add(panel, BorderLayout.CENTER);
            // simulation_frame.setMenuBar(panel.getMenuBar());
            // simulation_frame.pack();
            // simulation_frame.setVisible(true);
            //panel.deserialize(model, simulation_frame);
        }

        return panel;
    }

    public void simulationWindowOpenedOperation(SimulationPanel3D panel, final EvacuationModelBase model) {
        // NetworkMapEditor で定義する
    }

    public void initSimulationPanel3D(SimulationPanel3D panel) {
        // NetworkMapEditor で定義する
    }

    @Override
    public void notifyViewChange(SimulationPanel3D panel) {
        /* do nothing */
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
        model.setRandom(_random);
    }

    public void setSpeedModel(SpeedCalculationModel _speedModel) {
        speedModel = _speedModel;
    }

    public SpeedCalculationModel getSpeedModel() {
        return speedModel;
    }

    /**
     * Set isTimeSeriesLog.
     * @param isTimeSeriesLog the value to set.
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
     * @param timeSeriesLogPath the value to set.
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
     * @param timeSeriesLogInterval the value to set.
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

    public boolean getIsDamageSpeedZeroNumberLog() {
        return isDamageSpeedZeroNumberLog;
    }

    public void setIsDamageSpeedZeroNumberLog(boolean
            _isDamageSpeedZeroNumberLog) {
        isDamageSpeedZeroNumberLog = _isDamageSpeedZeroNumberLog;
    }

    public String getDamageSpeedZeroNumberLogPath() {
        return damageSpeedZeroNumberLogPath;
    }

    public void setDamageSpeedZeroNumberLogPath(String
            _damageSpeedZeroNumberLogPath) {
        damageSpeedZeroNumberLogPath = _damageSpeedZeroNumberLogPath;
    }
}
