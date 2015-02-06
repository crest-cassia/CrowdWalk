package nodagumi.ananPJ.misc;

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.Document;

import org.apache.commons.cli.*;
import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.BasicSimulationLauncher;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.Pollution;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.misc.CommunicationHandler;
import nodagumi.ananPJ.misc.CommunicationHandler.CommunicationType;
import nodagumi.ananPJ.network.DaRuMaClient;
import nodagumi.ananPJ.Simulator.AgentHandler;

import nodagumi.Itk.*;


public class NetmasPropertiesHandler implements Serializable {

    private static final long serialVersionUID = 50125012L;

    public static final List cuiPropList = Arrays.asList(
            "debug",
            "io_handler_type",
            "map_file",
            "pollution_file",
            "scenario_file",
            "generation_file",
            "timer_enable",
            "timer_file",
            "interval",
            "addr",
            "port",
            "serialize_file",
            "serialize_interval",
            "deserialized_file",
            "randseed",
            "speed_model",
            "time_series_log",
            "time_series_log_path",
            "time_series_log_interval",
            "loop_count",
            "exit_count",
	    "all_agent_speed_zero_break",
	    /* [2015.01.07 I.Noda] to switch agent queue in the link directions.*/
	    "queue_order" // "front_first" or "rear_first"
            );

    public static final String[] DEFINITION_FILE_ITEMS = {"map_file", "generation_file", "scenario_file", "camera_file", "pollution_file", "link_appearance_file", "node_appearance_file"};

    protected String propertiescenarioPath = null;
    protected Properties prop = null;

    /**
     * Get a properties file name.
     * @return Property file name.
     */
    public String getPropertiescenarioPath() {
        return propertiescenarioPath;
    }

    /**
     * Set a properties file name.
     * @param _path a properties file name.
     */
    public void setPropertiescenarioPath(String _path) {
        propertiescenarioPath = _path;
    }

    protected boolean isDebug = false; /** debug mode */
    /**
     * Get a debug mode.
     * @return wether debug mode is enable or not
     */
    public boolean getIsDebug() {
        return isDebug;
    }

    protected CommunicationType communicationType = null; /** file or pipe or 
                                                          network */
    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    protected String mapPath = null; // path to map file (required)
    public String getMapPath() {
        return mapPath;
    }

    protected String pollutionPath = null; // path to pollution file
    public String getPollutionPath() {
        return pollutionPath;
    }

    protected String generationPath = null; // path to generation file
    public String getGenerationPath() {
        return generationPath;
    }

    protected String scenarioPath = null; // path to scenario file
    public String getScenarioPath() {
        return scenarioPath;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback file
     */
    protected String fallbackPath = null;

    //------------------------------------------------------------
    /**
     * fallback file を取得
     */
    public String getFallbackPath() {
	return fallbackPath ;
    }

    protected boolean isTimerEnabled = false;
    public boolean getIsTimerEnabled() {
        return isTimerEnabled;
    }

    protected String timerPath = null;         // path to timer log file
    public String getTimerPath() {
        return timerPath;
    }

    protected int interval = -1;       // sleep time(msec) during loop
    public int getInterval() {
        return interval;
    }

    protected String serializePath = null;    // path to serialized file
    public String getSerializePath() {
        return serializePath;
    }

    protected int serializeInterval = -1;  // interval of serialize
    public int getSerializeInterval() {
        return serializeInterval;
    }

    protected String addr = null;      // IP address of destination host
    public String getAddr() {
        return addr;
    }

    protected int port = -1;           // port number of destination host
    public int getPort() {
        return port;
    }

    protected String deserializePath = null;
    public String getDeserializePath() {
        return deserializePath;
    }

    protected long randseed = 0;
    public long getRandseed() {
        return randseed;
    }

    protected static SpeedCalculationModel speedModel = null;
    public SpeedCalculationModel getSpeedModel() {
        return speedModel;
    }

    // whether call NetworkMap.saveTimeSeriesLog in loop
    protected boolean isTimeSeriesLog = false;
    public boolean getIsTimeSeriesLog() {
        return isTimeSeriesLog;
    }
    protected String timeSeriesLogPath = null;
    public String getTimeSeriesLogPath() {
        return timeSeriesLogPath;
    }
    protected int timeSeriesLogInterval = -1;
    public int getTimeSeriesLogInterval() {
        return timeSeriesLogInterval;
    }

    // 
    protected boolean isDamageSpeedZero = false;
    public boolean getIsDamageSpeedZero() {
        return isDamageSpeedZero;
    }
    protected String damageSpeedZeroPath = null;
    public String getDamageSpeedZeroPath() {
        return damageSpeedZeroPath;
    }

    // End condition of simulation
    protected int exitCount = 0;
    public int getExitCount() {
        return exitCount;
    }

    protected int loopCount = -1;
    public int getLoopCount() {
        return loopCount;
    }

    protected boolean isAllAgentSpeedZeroBreak = false;
    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    protected boolean isDeserialized = false;

    public NetmasPropertiesHandler(String _propertiescenarioPath) {
        // load properties
        prop = new Properties();
        propertiescenarioPath = _propertiescenarioPath;
        try {
            System.err.println(_propertiescenarioPath);
            String path = _propertiescenarioPath.toLowerCase();
            if (path.endsWith(".xml")) {
                prop.loadFromXML(new FileInputStream(_propertiescenarioPath));
            } else if (path.endsWith(".json")) {
                HashMap<String, Object> map = (HashMap<String, Object>)JSON.decode(new FileInputStream(_propertiescenarioPath));
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    prop.setProperty(entry.getKey(), entry.getValue().toString());
                }
            } else {
                System.err.println("Property file error - 拡張子が不正です: " + _propertiescenarioPath);
                System.exit(1);
            }
            isDebug = getBooleanProperty(prop, "debug");
            String typestr = getProperty(prop, "io_handler_type");
            if (typestr == null)
                communicationType = CommunicationType.SND_FILE;
            else if (typestr.equals("buffer"))
                communicationType = CommunicationType.SND_BUFFER;
            else if (typestr.equals("file"))
                communicationType = CommunicationType.SND_FILE;
            else if (typestr.equals("pipe"))
                communicationType = CommunicationType.SND_PIPE;
            else if (typestr.equals("network"))
                communicationType = CommunicationType.SND_NETWORK;
            else if (typestr.equals("server"))
                communicationType = CommunicationType.RCV_NETWORK;
            else if (typestr.equals("none"))
                communicationType = CommunicationType.NONE;
            else {
                System.err.println("NetmasPropertiesHandler: invalid " +
                        "inputted type:" + typestr);
                communicationType = CommunicationType.NONE;
            }

            // パス指定がファイル名のみならばプロパティファイルのディレクトリパスを付加する
            File propertyFile = new File(_propertiescenarioPath);
            String propertyDirPath = propertyFile.getParent();
            if (propertyDirPath == null) {
                propertyDirPath = ".";
            }
            for (String property_item : DEFINITION_FILE_ITEMS) {
                String filePath = getString(property_item, null);
                if (filePath != null) {
                    File file = new File(filePath);
                    if (file.getParent() == null) {
                        prop.setProperty(property_item, propertyDirPath.replaceAll("\\\\", "/") + "/" + filePath);
                        //System.err.println(property_item + ": " + getString(property_item, ""));
                    }
                }
            }

            // input files
            mapPath = getStringProperty(prop, "map_file");
            pollutionPath = getStringProperty(prop, "pollution_file");
            generationPath = getStringProperty(prop, "generation_file");
            scenarioPath = getProperty(prop, "scenario_file");
	    fallbackPath = getProperty(prop, "fallback_file") ;
            // timer enabled or not
            isTimerEnabled = getBooleanProperty(prop, "timer_enable");
            if (isTimerEnabled)
                timerPath = getStringProperty(prop, "timer_file");

            // interval during main loop
            interval = getIntegerProperty(prop, "interval");
            // destination address if I/O handler is network mode
            addr = getStringProperty(prop, "addr");
            // port number
            port = getIntegerProperty(prop, "port");

            // scerialize file
            serializePath = getStringProperty(prop, "serialize_file");
            // interval of serialize (loop count)
            if (serializePath != null)
                serializeInterval = getIntegerProperty(prop,
                        "serialize_interval");
            // descerialize file
            deserializePath = getStringProperty(prop, "deserialized_file");
            // create random with seed
            randseed = getIntegerProperty(prop, "randseed");
            // speed model
            String speedModelString = getStringProperty(prop, "speed_model");
            if (speedModelString.equals("strait")) {
                speedModel = SpeedCalculationModel.StraitModel;
            } else {
                speedModel = SpeedCalculationModel.LaneModel;
                System.err.println("NetmasCuiSimulator speed model: lane");
            }
            // time series log
            isTimeSeriesLog = getBooleanProperty(prop, "time_series_log");
            if (isTimeSeriesLog) {
                timeSeriesLogPath = getStringProperty(prop,
                        "time_series_log_path");
                timeSeriesLogInterval = getIntegerProperty(prop,
                        "time_series_log_interval");
            }
            // the number of agents with damaged speed zero
            isDamageSpeedZero = getBooleanProperty(prop,
                                                   "damage_speed_zero_log");
            if (isDamageSpeedZero) {
                damageSpeedZeroPath = getStringProperty(prop,
                        "damage_speed_zero_log_path");
            }
            // loop count
            loopCount = getIntegerProperty(prop, "loop_count");
            // exit count
            exitCount = getIntegerProperty(prop, "exit_count");
            isAllAgentSpeedZeroBreak = getBooleanProperty(prop,
                    "all_agent_speed_zero_break");

            // 早い内に設定ミスをユーザーに知らせるための検査
            String pollutionType = getString("pollution_type", null);
            if (pollutionType != null) {
                AgentBase.setPollutionType(pollutionType);
                Pollution.getInstance(pollutionType + "Pollution");
            }
            getString("pollution_color", "RED", SimulationPanel3D.gas_display.getNames());
            getDouble("pollution_color_saturation", 0.0);

	    /* [2015.01.07 I.Noda] to switch agent queue in the link directions.*/
	    String queueOrderStr = getProperty(prop, "queue_order") ;
	    if(queueOrderStr != null) {
		if(queueOrderStr.equals("front_first")) {
		    AgentHandler.useFrontFirstOrderQueue(true) ;
		    Itk.dbgMsg("use front_first order to sort agent queue.") ;
		} else if (queueOrderStr.equals("rear_first")) {
		    AgentHandler.useFrontFirstOrderQueue(false) ;
		    Itk.dbgMsg("use rear_first order to sort agent queue.") ;
		} else {
		    Itk.dbgErr("unknown queue_order:" + queueOrderStr) ;
		    Itk.dbgMsg("use default order (rear_first)") ;
		}
	    }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        // check property options
        if (mapPath == null) {
            System.err.println("NetmasCuiSimulator: map file is " +
                    "required.");
            return;
        } else if (!((File) new File(mapPath)).exists()) {
            System.err.println("NetmasCuiSimulator: specified map file does " +
                    "not exist.");
            return;
        } else if (communicationType == CommunicationType.SND_FILE &&
                serializePath == null) {
            System.err.println("NetmasCuiSimulator: file mode requires" +
                    " path to log.");
            return;
        } else if (communicationType == CommunicationType.SND_NETWORK &&
                addr == null) {
            System.err.println("NetmasCuiSimulator: network mode " +
                    "requires destination address.");
            return;
        }
    }

    public static String getProperty(Properties prop, String key) {
        if (prop.containsKey(key)) {
            return prop.getProperty(key);
        } else {
            return null;
        }
    }

    public static String getStringProperty(Properties prop, String key) {
        String stringProp = getProperty(prop, key);
        if (stringProp != null && !stringProp.equals(""))
            return stringProp;
        else {
            //System.err.println("string prop null: " + key);
            return null;
        }
    }

    public static boolean getBooleanProperty(Properties prop, String key) {
        String stringProp = getStringProperty(prop, key);
        if (stringProp == null) {
            //System.err.println("null: ");
            return false;
        } else if (stringProp.toLowerCase().equals("true") || stringProp.toLowerCase().equals("on"))
            return true;
        else
            return false;
    }

    public static int getIntegerProperty(Properties prop, String key) {
        String stringProp = getStringProperty(prop, key);
        if (stringProp == null)
            return -1;
        else
            return Integer.parseInt(stringProp);
    }

    public boolean isDefined(String key) {
        String value = prop.getProperty(key);
        return ! (value == null || value.trim().isEmpty());
    }

    public String getString(String key, String defaultValue) {
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public String getString(String key, String defaultValue, String pattern[]) throws Exception {
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        value = value.toLowerCase();
        for (String str : pattern) {
            if (str.toLowerCase().equals(value)) {
                return value;
            }
        }
        throw new Exception("Property error - 設定値が不正です: " + key + ":" + value);
    }

    public String getDirectoryPath(String key, String defaultValue) throws Exception {
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        File file = new File(value);
        if (! file.exists()) {
            throw new Exception("Property error - 指定されたディレクトリが存在しません: " + key + ":" + value);
        }
        if (! file.isDirectory()) {
            throw new Exception("Property error - 指定されたパスがディレクトリではありません: " + key + ":" + value);
        }
        return value;
    }

    public String getFilePath(String key, String defaultValue) throws Exception {
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        File file = new File(value);
        if (! file.exists()) {
            throw new Exception("Property error - 指定されたファイルが存在しません: " + key + ":" + value);
        }
        if (! file.isFile()) {
            throw new Exception("Property error - 指定されたパスがファイルではありません: " + key + ":" + value);
        }
        return value;
    }

    public String getFilePath(String key, String defaultValue, boolean existing) throws Exception {
        if (existing) {
            return getFilePath(key, defaultValue);
        }
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        File file = new File(value);
        if (file.exists() && ! file.isFile()) {
            throw new Exception("Property error - 指定されたパスがファイルではありません: " + key + ":" + value);
        }
        return value;
    }

    public boolean getBoolean(String key, boolean defaultValue) throws Exception {
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        value = value.toLowerCase();
        if (value.equals("true") || value.equals("on")) {
            return true;
        } else if (value.equals("false") || value.equals("off")) {
            return false;
        } else {
            throw new Exception("Property error - 設定値が不正です: " + key + ":" + value);
        }
    }

    public int getInteger(String key, int defaultValue) throws Exception {
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch(NumberFormatException e) {
            throw new Exception("Property error - 設定値が不正です: " + key + ":" + value);
        }
    }

    public double getDouble(String key, double defaultValue) throws Exception {
        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch(NumberFormatException e) {
            throw new Exception("Property error - 設定値が不正です: " + key + ":" + value);
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
            formatter.printHelp("NetmasPropertiesHandler", options, true);
            System.exit(1);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        String propertiescenarioPath = cli.getOptionValue("p");

        NetmasPropertiesHandler nph =
            new NetmasPropertiesHandler(propertiescenarioPath);
    }
}
