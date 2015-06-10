// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.*;
import java.util.*;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Agents.*;
import nodagumi.ananPJ.Agents.WalkAgent.*;
import nodagumi.ananPJ.Simulator.*;
import nodagumi.ananPJ.misc.*;

import nodagumi.Itk.*;

public class NetmasCuiSimulator extends BasicSimulationLauncher {

    protected NetworkMap networkMap;

    protected static boolean isDebug = false; // debug mode
    protected static int interval = 0;        // sleep time(msec) during loop

    protected static SpeedCalculationModel speed_model = null;
    // End condition of simulation
    protected static int exitCount = 0;

    protected static String propertiesPath = null;    // java.util.Properties
    protected static String mPath = null; // path to map file (required)
    protected static String pPath = null; // path to pollution file
    protected static String gPath = null; // path to generation file
    protected static String sPath = null; // path to scenario file
    protected static String fallbackPath = null; //
    protected String agentMovementHistoryPath = null;
    protected String individualPedestriansLogDir = null;

    protected boolean isAllAgentSpeedZeroBreak = false;

    protected int loopCount = -1;
    protected double linerGenerateAgentRatio = 1.0;

    protected NetmasPropertiesHandler propertiesHandler = null;

    protected static Random random = null;

    public NetmasCuiSimulator(Random _random) {
        super(_random);
    }

    public NetmasCuiSimulator(String _propertiesPath) {
        this(_propertiesPath, false, -1);
    }

    public NetmasCuiSimulator(String _propertiesPath, int randseed) {
        this(_propertiesPath, true, randseed);
    }

    public NetmasCuiSimulator(String _propertiesPath, boolean withSeed, int randseed) {
        super(null) ;
        // load properties
        Properties prop = new Properties();
        propertiesHandler = new NetmasPropertiesHandler(_propertiesPath);

        // create random with seed
        if (! withSeed) {
            randseed = (int)propertiesHandler.getRandseed();
        }
        random = new Random(randseed);

        // debug mode
        isDebug = propertiesHandler.getIsDebug();
        // input files
        mPath = propertiesHandler.getMapPath();
        pPath = propertiesHandler.getPollutionPath();
        gPath = propertiesHandler.getGenerationPath();
        sPath = propertiesHandler.getScenarioPath();
	fallbackPath = propertiesHandler.getFallbackPath();
        // timer enabled or not
        isTimerEnabled = propertiesHandler.getIsTimerEnabled();
        timerPath = propertiesHandler.getTimerPath();
        // interval during main loop
        interval = propertiesHandler.getInterval();
        // speed model
        speed_model = propertiesHandler.getSpeedModel();
        // time series log
        isTimeSeriesLog = propertiesHandler.getIsTimeSeriesLog();
        timeSeriesLogPath = propertiesHandler.getTimeSeriesLogPath();
        timeSeriesLogInterval = propertiesHandler.
            getTimeSeriesLogInterval();
        // loop count
        loopCount = propertiesHandler.getLoopCount();
        // exit count
        exitCount = propertiesHandler.getExitCount();
        isAllAgentSpeedZeroBreak =
            propertiesHandler.getIsAllAgentSpeedZeroBreak();
        // check property options
        if (mPath == null) {
            System.err.println("NetmasCuiSimulator: map file is " +
                    "required.");
            return;
        } else if (!((File) new File(mPath)).exists()) {
            System.err.println("NetmasCuiSimulator: specified map file does " +
                    "not exist.");
            return;
        }
        properties = propertiesHandler;
        try {
            networkMap = readMapWithName(mPath, random);
            networkMap.setHasDisplay(false);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        try {
            agentMovementHistoryPath = propertiesHandler.getFilePath("agent_movement_history_file", null, false);
            individualPedestriansLogDir = propertiesHandler.getDirectoryPath("individual_pedestrians_log_dir", null);
            if (individualPedestriansLogDir != null) {
                individualPedestriansLogDir = individualPedestriansLogDir.replaceFirst("[/\\\\]+$", "");
            }
        } catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        // プロパティファイルで指定されたパスを使用する(以下が無いとマップファイルの設定が使われる)
        networkMap.setPollutionFile(pPath);
        networkMap.setGenerationFile(gPath);
        networkMap.setScenarioFile(sPath);
	networkMap.setFallbackFile(fallbackPath);
	networkMap.scanFallbackFile(true) ;
    }

    public void initialize() {
        simulator = new EvacuationSimulator(networkMap, null, random) ;
        // this method just set 0 to model.tick_count
        simulator.setProperties(properties);
        simulator.setup();
        // model.begin set files (pol, gen, sce) to networkMap
        simulator.setLinerGenerateAgentRatio(linerGenerateAgentRatio);
        simulator.begin(false, false, null);
        simulator.setIsAllAgentSpeedZeroBreak(isAllAgentSpeedZeroBreak);

        if (isTimerEnabled)
            timer = new NetmasTimer(10, timerPath);
        counter = 0;
        if (isTimerEnabled)
            timer.start();
    }

    public void start() {
        if (agentMovementHistoryPath != null) {
            simulator.getAgentHandler().initAgentMovementHistorLogger("agent_movement_history", agentMovementHistoryPath);
        }
        if (individualPedestriansLogDir != null) {
            simulator.getAgentHandler().initIndividualPedestriansLogger("individual_pedestrians_log", individualPedestriansLogDir);
        }
	Itk.logDebug("NetmasCuiSimulator start!");
        finished = false;
        while (!finished) {
            finished = simulator.updateEveryTickCui();
            if (isTimerEnabled) {
                timer.tick();
                timer.writeInterval();
                if ((counter % 60) == 0)
                    timer.writeElapsed();
            }
            counter++;
            if (isTimeSeriesLog)
                if (counter % timeSeriesLogInterval == 0)
                    simulator.saveGoalLog(timeSeriesLogPath, false);
                    //simulator.saveTimeSeriesLog(timeSeriesLogPath);
	    if ((counter % 100) == 0)
		Itk.logDebug("Cycle", 
                             "count:", counter,
                             "time:", Itk.getCurrentTimeStr(),
                             "walking:", 
                             simulator.getAgentHandler().numOfWalkingAgents()) ;
            if (exitCount > 0 && counter > exitCount) {
                finished = true;
                break;
            }
            // この interval 機能は本当に必要なのか?
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        if (isTimeSeriesLog) {
            // flush log file
            simulator.saveGoalLog(timeSeriesLogPath, true);
        }
        if (individualPedestriansLogDir != null) {
            simulator.getAgentHandler().closeIndividualPedestriansLogger();
        }
        if (isTimerEnabled) {
            timer.writeElapsed();
            timer.stop();
        }
    }

    public NetworkMap getMap() {
        return networkMap;
    }

}

