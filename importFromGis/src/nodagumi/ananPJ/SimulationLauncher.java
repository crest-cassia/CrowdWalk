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
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;


public class SimulationLauncher extends BasicSimulationLauncher
    implements SimulationController, Serializable {

    private static final long serialVersionUID = 3014238657739616307L;

    protected transient Settings settings;
    private transient JFrame main_frame;
    String scenario_name = new String();
    private Random random = null;
    private boolean randomNavigation = false;
    private boolean isAllAgentSpeedZeroBreak = false;
    private NetmasTimer timer = null;
    private boolean isTimerEnabled = false;
    private String timerPath = null;
    private boolean isTimeSeriesLog = false;
    private String timeSeriesLogPath = null;
    private int timeSeriesLogInterval = -1;

    private SpeedCalculationModel speedModel = null;
    private boolean isExpectedDensitySpeedModel = false;
    private int expectedDensityMacroTimeStep = 300;
    private boolean expectedDensityVisualizeMicroTimeStep = true;

    protected NetworkMap networkMap;
    public NetworkMap getMap() { return networkMap; }

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

    /* the constructor with arguments are for to be used as
     *  a stand alone application */
    protected SimulationLauncher(String[] args) throws Exception {
        parse_args(args);
        main_frame = new JFrame("NetMAS Simulation Launhcer");
        settings = Settings.load("NetworkMapEditor.ini");
        setup_frame();
        main_frame.setVisible(true);
    }

    private transient JLabel directory_label = new JLabel();
    private transient JButton model_file_button = new JButton();
    private transient JLabel model_filename_label = new JLabel();
    private transient JButton generation_file_button = new JButton();
    private transient JLabel generation_filename_label = new JLabel();
    private transient JButton response_file_button = new JButton();
    private transient JLabel response_filename_label = new JLabel();

    private void setup_frame() {
        int width = settings.get("launcher_width", 640);
        int height = settings.get("launcher_height", 100);
        main_frame.setSize(width, height);
        main_frame.setLayout(new GridLayout(5, 2));

        /* files */
        String model_file = settings.get("mapfile", "");
        String generation_file = "";
        String response_file = "";
        String model_path = settings.get("inputdir", "") + model_file;
        try {
            NetworkMap network_map = readMapWithName(model_path, random);
            if (network_map != null) {
                generation_file = network_map.getGenerationFile();
                response_file = network_map.getResponseFile();
            } else {
                model_file = "";
            }
        } catch (IOException e) {
            /* Do nothing */
        }


        /* directory */
        main_frame.add(new JLabel());
        directory_label.setText(settings.get("inputdir"));
        main_frame.add(directory_label);
        /* model file */
        model_file_button.setText("Model");
        model_file_button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String filename = get_filename("Open map", "mapfile", true);
                if (filename != null) {
                    model_filename_label.setText(filename);
                }
            }
        });
        main_frame.add(model_file_button);
        model_filename_label.setText(model_file);
        main_frame.add(model_filename_label);

        /* generation scenario file */
        generation_file_button.setText("Agents");
        generation_file_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String filename = get_filename("Open agent generation", "generationfile", false);
                if (filename != null) {
                    generation_filename_label.setText(filename);
                }
            }
        });
        main_frame.add(generation_file_button);
        generation_file = settings.get("generationfile", generation_file);
        generation_filename_label.setText(generation_file);
        main_frame.add(generation_filename_label);

        /* response scenario file */
        response_file_button.setText("Response");
        response_file_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String filename = get_filename("Open response scenario",
                    "responsefile", false);
                if (filename != null) {
                    response_filename_label.setText(filename);
                }
            }
        });
        main_frame.add(response_file_button);
        response_file = settings.get("responsefile", response_file);
        response_filename_label.setText(response_file);
        main_frame.add(response_filename_label);

        /* Other buttons */
        JButton quit_button = new JButton("Quit");
        quit_button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {quit();}}); 
        main_frame.add(quit_button);

        JButton simulate_button = new JButton("Simulate");
        simulate_button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (setup_model()) {
                    simulate(false);
                }
                }}); 
        main_frame.add(simulate_button);
    }

    private boolean finished = true;
    private Boolean run_thread = false;
    private transient Runnable run_simulation = null;
    protected EvacuationSimulator model = null;

    private boolean setup_model() {
        /* setting up model */
        String inputdir = settings.get("inputdir", "");
        String model_path = inputdir + model_filename_label.getText();
        String generation_path = inputdir + generation_filename_label.getText();
        String response_path = inputdir + response_filename_label.getText();

        try {
            networkMap = readMapWithName(model_path, random);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(main_frame, e.getStackTrace(),
                    "構造物ファイルを開けません", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        networkMap.setGenerationFile(generation_path);
        networkMap.setResponseFile(response_path);
        return true;

    }

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
            model = new EvacuationSimulator(networkMap, this, scenario_name,
                    random);
            finished = false;
            model.setup();
            model.begin(true, isDeserialized, null);  // tkokada
            model.setRandomNavigation(randomNavigation);
            model.setIsAllAgentSpeedZeroBreak(isAllAgentSpeedZeroBreak);

            if (speedModel == SpeedCalculationModel.ExpectedDensityModel) {
                ((AgentHandler) model.getAgentHandler())
                    .setIsExpectedDensitySpeedModel(true);
                ((AgentHandler) model.getAgentHandler())
                    .setExpectedDensityMacroTimeStep(
                    expectedDensityMacroTimeStep);
                ((AgentHandler) model.getAgentHandler())
                    .setIsExpectedDensityVisualizeMicroTimeStep(
                            expectedDensityVisualizeMicroTimeStep);
                model.setTimeScale(expectedDensityMacroTimeStep);
            } else {
                System.err.println("S Launcher speedmodel: " + speedModel +
                        ", timestep: " + expectedDensityMacroTimeStep +
                        ", vis?: " + expectedDensityVisualizeMicroTimeStep);
            }
        }
        if (isTimerEnabled) {
            timer = new NetmasTimer(10, timerPath);
            timer.start();
        }
        run_simulation = new Runnable() {
            public void run() {
                while(!finished && run_thread) {
                    dump_state.preUpdate();
                    finished = model.updateEveryTick();
                    dump_state.postUpdate();
                    if (isTimeSeriesLog) {
                        if (((int) model.getSecond()) % timeSeriesLogInterval
                                == 0)
                            model.saveTimeSeriesLog(timeSeriesLogPath);
                    }
                    if (isTimerEnabled) {
                        timer.tick();
                        timer.writeInterval();
                        if ((model.getSecond() % 60) == 0)
                            timer.writeElapsed();
                    }
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

    private String get_filename(String message,
            String type,
            boolean set_directory) {
        FileDialog fd = new FileDialog(main_frame, message, FileDialog.LOAD);
        fd.setFile(settings.get(type, ""));
        String inputdir = settings.get("inputdir", "");
        fd.setDirectory(inputdir);
        fd.setVisible (true);

        if (fd.getFile() == null) return null;

        String file = fd.getDirectory() + fd.getFile();

        if (set_directory) {
            settings.put(type, fd.getFile());
            System.err.println("setting " + type + " to " + fd.getFile());
            settings.put("inputdir", fd.getDirectory ());
            System.err.println("setting inputdir to " + fd.getDirectory());
            directory_label.setText(fd.getDirectory());
            return fd.getFile();
        } else {
            String rpath = FilePathManipulation.getRelativePath(inputdir, file);
            settings.put(type, rpath);
            System.err.println("setting " + type + " to " + rpath);
            return rpath;
        }
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

    private void parse_args(String[] args) throws Exception {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) == '-' && args[i].charAt(1) == '-') {
                if (i == args.length - 1) {
                    throw new Exception("ERROR in parsing command line options:\n No value given for option " + args[i]);
                }
                final String key = args[i].substring(2);
                final String val = args[++i];
                settings.put(key, val);
            } else {
                throw new Exception("ERROR in parsing command line options:\n Cannot understand " + args[i]);
            }
        }
    }

    /* @override */
    public void windowClosing(WindowEvent e) {
        quit();
    }

    public static void main(String[] args) throws Exception {
        new SimulationLauncher(args);
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
            if (isTimeSeriesLog) {
                if (((int) model.getSecond()) % timeSeriesLogInterval == 0)
                    model.saveTimeSeriesLog(timeSeriesLogPath);
            }
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
    public SimulationPanel3D setupFrame(EvacuationModelBase model) {
        simulation_frame = new JFrame("Simulation Preview");

        simulation_frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {           }
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
    public SimulationPanel3D setupFrame(EvacuationModelBase model,
            SimulationPanel3D _panel) {
        if (model == null)
            return null;
        if (_panel == null) {
            simulation_frame = new JFrame("Simulation Preview");
            simulation_frame.addWindowListener(new WindowListener() {
                public void windowOpened(WindowEvent e) {           }
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

    @Override
    public void notifyViewChange(SimulationPanel3D panel) {
        /* do nothing */
    }

    public boolean getRandomNavigation() {
        return randomNavigation;
    }

    public void setRandomNavigation(boolean _navigation) {
        randomNavigation = _navigation;
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

    public void setIsExpectedDensitySpeedModel(boolean
            _isExpectedDensitySpeedModel) {
        isExpectedDensitySpeedModel = _isExpectedDensitySpeedModel;
    }

    public boolean getIsExpectedDensitySpeedModel() {
        return isExpectedDensitySpeedModel;
    }

    public void setExpectedDensityMacroTimeStep(int
            _expectedDensityMacroTimeStep) {
        expectedDensityMacroTimeStep = _expectedDensityMacroTimeStep;
    }

    public int getExpectedDensityMacroTimeStep() {
        return expectedDensityMacroTimeStep;
    }

    public void setExpectedDensityVisualizeMicroTimeStep(boolean
            _expectedDensityVisualizeMicroTimeStep) {
        expectedDensityVisualizeMicroTimeStep =
            _expectedDensityVisualizeMicroTimeStep;
    }

    public boolean getExpectedDensityVisualizeMicroTimeStep() {
        return expectedDensityVisualizeMicroTimeStep;
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
}
