package nodagumi.ananPJ;

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.Document;

import org.apache.commons.cli.*;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Agents.*;
import nodagumi.ananPJ.Agents.WalkAgent.*;
import nodagumi.ananPJ.Simulator.*;
import nodagumi.ananPJ.misc.*;
import nodagumi.ananPJ.misc.CommunicationHandler.*;
import nodagumi.ananPJ.network.*;

import nodagumi.Itk.*;

public class NetmasCuiSimulator extends BasicSimulationLauncher
        implements Serializable {

    private static final long serialVersionUID = 5012L;
    protected int bufsize = 1024;

    protected EvacuationSimulator model;
    protected NetworkMap networkMap;

    protected static boolean isDebug = false; // debug mode
    protected static String addr = null;      // IP address of destination host
    protected static int port = -1;           // port number of destination host
    protected static int interval = 0;        // sleep time(msec) during loop
    protected static int serializeInterval = -1;  // interval of serialize

    // whether call NetworkMap.saveTimeSeriesLog in loop
    protected static boolean isTimeSeriesLog = false;
    protected static String timeSeriesLogPath = null;
    protected static int timeSeriesLogInterval = -1;
    protected static SpeedCalculationModel speed_model = null;
    // End condition of simulation
    protected static int exitCount = 0;

    protected static String propertiesPath = null;    // java.util.Properties
    protected static String mPath = null; // path to map file (required)
    protected static String pPath = null; // path to pollution file
    protected static String gPath = null; // path to generation file
    protected static String sPath = null; // path to scenario file
    protected static String fallbackPath = null; //
    protected static String serializeFile = null;    // path to serialized file
    protected static String timerFile = null;         // path to timer log file
    protected static String deserializeFile = null;
    protected String agentMovementHistoryPath = null;
    protected String individualPedestriansLogDir = null;

    protected static ObjectOutputStream oos = null;
    protected static boolean isTimerEnabled = false;
    protected static int counter;
    protected static boolean isDeserialized = false;
    protected boolean isAllAgentSpeedZeroBreak = false;

    protected transient NetMASIOHandler handler = null;
    protected transient NetMASMapServer mapServer = null;

    protected static CommunicationType type = null;   // file or pipe or network
    protected static NetmasTimer timer = null;
    protected DaRuMaClient darumaClient = DaRuMaClient.getInstance();
    protected int loopCount = -1;
    protected double linerGenerateAgentRatio = 1.0;

    protected NetmasPropertiesHandler propertiesHandler = null;

    protected static Random random = null;

    public NetmasCuiSimulator(Random _random) {
        super();
        random = _random;
    }

    public NetmasCuiSimulator(String _propertiesPath) {
        this(_propertiesPath, false, -1);
    }

    public NetmasCuiSimulator(String _propertiesPath, int randseed) {
        this(_propertiesPath, true, randseed);
    }

    public NetmasCuiSimulator(String _propertiesPath, boolean withSeed, int randseed) {
        super();
        // load properties
        Properties prop = new Properties();
        propertiesHandler = new NetmasPropertiesHandler(_propertiesPath);
        // debug mode
        isDebug = propertiesHandler.getIsDebug();
        // type of I/O handler
        type = propertiesHandler.getCommunicationType();
        // input files
        mPath = propertiesHandler.getMapPath();
        pPath = propertiesHandler.getPollutionPath();
        gPath = propertiesHandler.getGenerationPath();
        sPath = propertiesHandler.getScenarioPath();
	fallbackPath = propertiesHandler.getFallbackPath();
        // timer enabled or not
        isTimerEnabled = propertiesHandler.getIsTimerEnabled();
        timerFile = propertiesHandler.getTimerPath();
        // interval during main loop
        interval = propertiesHandler.getInterval();
        // interval of serialize (loop count)
        serializeInterval = propertiesHandler.getSerializeInterval();
        // destination address if I/O handler is network mode
        addr = propertiesHandler.getAddr();
        // port number
        port = propertiesHandler.getPort();
        // scerialize file
        serializeFile = propertiesHandler.getSerializePath();
        // descerialize file
        deserializeFile = propertiesHandler.getDeserializePath();
        // create random with seed
        if (! withSeed) {
            randseed = (int)propertiesHandler.getRandseed();
        }
        random = new Random(randseed);
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
        } else if (type == CommunicationType.SND_FILE &&
                serializeFile == null) {
            System.err.println("NetmasCuiSimulator: file mode requires" +
                    " path to log.");
            return;
        } else if (type == CommunicationType.SND_NETWORK && addr == null) {
            System.err.println("NetmasCuiSimulator: network mode " +
                    "requires destination address.");
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
        networkMap.setResponseFile(sPath);
	networkMap.setFallbackFile(fallbackPath);
	networkMap.scanFallbackFile(true) ;
    }

    protected String scenarioSerial = null;
    public void setScenarioSerial(String _scenarioSerial) {
        scenarioSerial = _scenarioSerial;
    }

    boolean finished = false;
    public void initialize() {
        if (type == CommunicationType.RCV_NETWORK) {
            mapServer = new NetMASMapServer(type, isDebug, addr, port,
                    serializeFile, null);
            mapServer.start();
        } else {
            handler = new NetMASIOHandler(type, isDebug, addr, port,
                    serializeFile, null);
            handler.start();
        }

        if (!isDeserialized) {
            model = new EvacuationSimulator(networkMap, null, scenarioSerial,
                    random);
            // this method just set 0 to model.tick_count
            model.setProperties(properties);
            model.setup();
        }
        // model.begin set files (pol, gen, sce) to networkMap
        model.setLinerGenerateAgentRatio(linerGenerateAgentRatio);
        model.begin(false, isDeserialized, null);
        model.setIsAllAgentSpeedZeroBreak(isAllAgentSpeedZeroBreak);

        if (!isDeserialized) {
            if (isTimerEnabled)
                timer = new NetmasTimer(10, timerFile);
            counter = 0;
            if (isTimerEnabled)
                timer.start();
        }
        // save initial state
        // 不要なのでコメント化(斉藤)
        //if (isTimeSeriesLog)
            //model.saveGoalLog(timeSeriesLogPath, false);
            //model.saveTimeSeriesLog(timeSeriesLogPath);
    }

    public void start() {
        if (agentMovementHistoryPath != null) {
            model.getAgentHandler().initAgentMovementHistorLogger("agent_movement_history", agentMovementHistoryPath);
        }
        if (individualPedestriansLogDir != null) {
            model.getAgentHandler().initIndividualPedestriansLogger("individual_pedestrians_log", individualPedestriansLogDir);
        }
        if (isDebug)
            System.err.println("NetmasCuiSimulator start!");
        while(!finished) {
            finished = model.updateEveryTickCui();
            if (serializeInterval >= 0 &&
                    counter % serializeInterval == 0) {
                model.getMap().prepareForSave();
                Document doc = darumaClient.newDocument();
                model.getMap().toDOM(doc);
                String docString = darumaClient.docToString(doc);
                if (type == CommunicationType.RCV_NETWORK)
                    mapServer.writeStream(docString);
                else
                    handler.writeStream(docString);
            }
            if (isTimerEnabled) {
                timer.tick();
                timer.writeInterval();
                if ((counter % 60) == 0)
                    timer.writeElapsed();
            }
            counter++;
            if (isTimeSeriesLog)
                if (counter % timeSeriesLogInterval == 0)
                    model.saveGoalLog(timeSeriesLogPath, false);
                    //model.saveTimeSeriesLog(timeSeriesLogPath);
            if (isDebug)
                if ((counter % 100) == 0)
                    System.out.println("NetmasCuiSimulator loop: " +
                            loopCount + " count: " + counter);
            if (exitCount > 0 && counter > exitCount) {
                finished = true;
                break;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        if (isTimeSeriesLog) {
            // flush log file
            model.saveGoalLog(timeSeriesLogPath, true);
        }
        if (individualPedestriansLogDir != null) {
            model.getAgentHandler().closeIndividualPedestriansLogger();
        }
        if (isTimerEnabled) {
            timer.writeElapsed();
            timer.stop();
        }
    }

    public NetworkMap getMap() {
        return networkMap;
    }

    public EvacuationModelBase getModel() {
        return model;
    }

    /**
     * This method simply send the map XML to I/O handler.
     */
    protected Document createDomMap() {
        networkMap.prepareForSave();
        Document doc = darumaClient.newDocument();
        networkMap.toDOM(doc);

        return doc;
    }

    static NetmasCuiSimulator instance = null;
    public static NetmasCuiSimulator getInstance() {
        if (instance == null) {
            instance = new NetmasCuiSimulator(random);
        }
        return instance;
    }

    protected static String getProperty(Properties prop, String key) {
        if (prop.containsKey(key)) {
            return prop.getProperty(key);
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {

        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("properties_file")
                .hasArg(true).withDescription("Path of properties file")
                .isRequired(true).create("p"));

        CommandLineParser parser = new BasicParser();
        CommandLine cli = null;

        try {
            cli = parser.parse(options, args);
        } catch (MissingOptionException moe) {
            moe.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NetmasCuiSimulator", options, true);
            System.exit(1);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        propertiesPath = cli.getOptionValue("p");

        if (deserializeFile == null) {
            NetmasCuiSimulator sim = new NetmasCuiSimulator(propertiesPath);
            //sim.printInputtedOptions();
            //NetworkMap networkMap = sim.readMapWithName(mPath, random);
            sim.scenarioSerial = "NetMASNetmasCuiSimulator";
            sim.initialize();
            sim.start();
        } else {
            try {
                FileInputStream fis = new FileInputStream(deserializeFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                NetmasCuiSimulator sim = (NetmasCuiSimulator) ois.readObject();
                sim.isDeserialized = true;
                //sim.printInputtedOptions();
                sim.initialize();
                sim.start();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
        }
    }
}

