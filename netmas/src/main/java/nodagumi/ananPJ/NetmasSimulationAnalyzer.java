package nodagumi.ananPJ;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.w3c.dom.Document;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.MissingOptionException;

import nodagumi.ananPJ.BasicSimulationLauncher;
import nodagumi.ananPJ.NetmasCuiSimulator;
import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;

import nodagumi.ananPJ.misc.CommunicationHandler;
import nodagumi.ananPJ.misc.CommunicationHandler.CommunicationType;
import nodagumi.ananPJ.misc.NetMASIOHandler;
import nodagumi.ananPJ.misc.NetmasTimer;
import nodagumi.ananPJ.misc.Snapshot;
import nodagumi.ananPJ.misc.NetmasPatternGenerator;
import nodagumi.ananPJ.misc.NetmasPropertiesHandler;
import nodagumi.ananPJ.Simulator.EvacuationModelBase;

import nodagumi.ananPJ.network.DaRuMaClient;


/** NetmasSimulationAnalyzer manages NetmasCuiSimulator with rules, executes
 * it in the loop and analyzes the results.
 * The rule are given in property file and 
 */
public class NetmasSimulationAnalyzer implements Serializable {

    private static final long serialVersionUID = 501212L;

    private DaRuMaClient darumaClient = DaRuMaClient.getInstance();

    private String propertiesPath = null;    // java.util.Properties

    private boolean isDebug = false; // debug mode

    private String cuiProperty = null;  // property for NetmasCuiSimulator

    private boolean isSaveProperty = false;

    private boolean isSaveMap = false;

    private boolean isRestart = false;

    private String resultPath = null;

    private int randseed = -1;

    private String origMapPath = null;
    private String origPollutionPath = null;
    private String origScenarioPath = null;
    private String origGenerationPath = null;

    private String rulePath = null;

    private boolean isDeserialized = false;
    private String serializeFile = null;    // path to serialized file

    private String deserializeFile = null;
    private ObjectOutputStream oos = null;

    private boolean isTimerEnabled = false;
    private String timerFile = null;         // path to timer log file
    private NetmasTimer timer = null;

    private NetmasPatternGenerator patternGenerator = null;
    private NetmasPropertiesHandler propertiesHandler = null;
    private int counter;

    public NetmasSimulationAnalyzer(String _propertiesPath) {
        super();
        // load properties
        propertiesHandler = new NetmasPropertiesHandler(_propertiesPath);
        Properties prop = new Properties();
        try {
            prop.loadFromXML(new FileInputStream(_propertiesPath));
            // debug mode
            String debugstr = getProperty(prop, "debug");
            if (debugstr == null)
                isDebug = false;
            else if (debugstr.equals("true"))
                isDebug = true;
            // cui property
            cuiProperty = getProperty(prop, "cui_property");
            // save property
            String isSavePropertyString = getProperty(prop, "save_property");
            if (isSavePropertyString == null)
                isSaveProperty = false;
            else if (isSavePropertyString.equals("true"))
                isSaveProperty = true;
            // save map
            String isSaveMapString = getProperty(prop, "save_map");
            if (isSaveMapString == null)
                isSaveMap = false;
            else if (isSaveMapString.equals("true"))
                isSaveMap = true;
            // result path
            resultPath = getProperty(prop, "result_path");
            // scerialize file
            serializeFile = getProperty(prop, "serialize_file");
            // descerialize file
            deserializeFile = getProperty(prop, "deserialized_file");
            // rule
            rulePath = getProperty(prop, "rule_file");
            if (rulePath == null) {
                System.err.println("NetmasSimulationAnalyzer invalid rule " +
                        "path: " + rulePath);
                return;
            }
            // patternGenerator = new NetmasPatternGenerator(rulePath);
            // patternGenerator.setIsSaveMap(isSaveMap);
            // load original map file path
            // prop.clear();
            // prop.loadFromXML(new FileInputStream(cuiProperty));
            origMapPath = getProperty(prop, "map_file");
            origPollutionPath = getProperty(prop, "pollution_file");
            origScenarioPath = getProperty(prop, "scenario_file");
            origGenerationPath = getProperty(prop, "generation_file");
        } catch (IOException ioe) {

            ioe.printStackTrace();
        }
    }

    private String scenarioSerial = null;
    private void envokeSimulator() {
        if (isTimerEnabled)
            timer = new NetmasTimer(10, timerFile);
        counter = 0;
        if (isTimerEnabled)
            timer.start();
        boolean finished = false;
        // clean result path directory
        deleteFile(new File(resultPath));
        while(true) {
            createPropertyFile();
            copyMapFiles();
            NetmasCuiSimulator ncs = new NetmasCuiSimulator(getPropertyPath());
            patternGenerator.setResultPath(getResultPath());
            if (!patternGenerator.setOnewayBridgeTagPattern(ncs.getMap(),
                        counter))
                break;
            saveMap(ncs);
            ncs.setScenarioSerial("NetmasCuiSimulator" + counter);
            ncs.initialize();

            ncs.start();
            if (isTimerEnabled) {
                timer.tick();
                timer.writeInterval();
                if ((counter % 60) == 0)
                    timer.writeElapsed();
            }
            counter++;
        }
        if (isTimerEnabled) {
            timer.writeElapsed();
            timer.stop();
        }
    }

    private String getResultPath() {
        return resultPath + "/" + counter;
    }

    private String getPropertyPath() {
        return getResultPath() + "/properties.xml";
    }

    private String getMapPath() {
        return getResultPath() + "/map/map.xml";
    }

    private String getPollutionPath() {
        return getResultPath() + "/map/gas.csv";
    }

    private String getScenarioPath() {
        return getResultPath() + "/map/scenario.csv";
    }

    private String getGenerationPath() {
        return getResultPath() + "/map/gen.csv";
    }

    private boolean delteFileChildren(File file) {
        boolean result = true;
        File parent = file.getAbsoluteFile();
        if (!parent.isDirectory()) {
            parent = parent.getParentFile();
        }
        String[] children = parent.list();
        for (int i = 0; i < children.length; i++) {
            File child = new File(parent, children[i]);
            if (!child.delete())
                result = false;
        }

        return result;
    }

    private boolean deleteFile(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                boolean result = deleteFile(new File(file, children[i]));
                if (!result)
                    return false;
            }
        }

        return file.delete();
    }

    private void saveMap(NetmasCuiSimulator ncs) {
        ncs.getMap().prepareForSave();
        try {
            String map_file_path = getMapPath();
            File map_file = new File(map_file_path);
            File map_file_dir = map_file.getParentFile();
            // create directories if not exist
            if (map_file_dir != null && !map_file_dir.exists())
                map_file_dir.mkdirs();
            FileOutputStream fos = new FileOutputStream(map_file_path);
            Document doc = darumaClient.newDocument();
            ncs.getMap().toDOM(doc);
            if (!darumaClient.docToStream(doc, fos))
                System.err.println("NetmasSimulationAnalyzer.copyMapFiles " +
                        "fail to save map.");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void copyMapFiles() {
        String map_file_path = getMapPath();
        File map_file = new File(map_file_path);
        File map_file_dir = map_file.getParentFile();
        // create directories if not exist
        if (map_file_dir != null && !map_file_dir.exists())
            map_file_dir.mkdirs();
        try {
            copyFile(origMapPath, getMapPath());
            copyFile(origGenerationPath, getGenerationPath());
            copyFile(origScenarioPath, getScenarioPath());
            copyFile(origPollutionPath, getPollutionPath());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void copyFile(String src, String dst) throws IOException {
        FileChannel srcChannel = new FileInputStream(src).getChannel();
        FileChannel dstChannel = new FileOutputStream(dst).getChannel();
        try {
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        } finally {
            srcChannel.close();
            dstChannel.close();
        }
    }

    private void createPropertyFile() {
        if (cuiProperty == null)
            return;
        File prop_file = new File(getPropertyPath());
        File prop_file_dir = prop_file.getParentFile();
        // create directories if not exist
        if (prop_file_dir != null && !prop_file_dir.exists())
            prop_file_dir.mkdirs();
        Properties prop = new Properties();
        try {
            PrintWriter writer = new PrintWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(prop_file, false), "utf-8")),
                    true);
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<!DOCTYPE properties SYSTEM \"http://java.sun." +
                    "com/dtd/properties.dtd\">");
            writer.println("<properties>");
            writer.println("\t<comment>NetmasCuiSimulator</comment>");

            prop.loadFromXML(new FileInputStream(cuiProperty));
            // load all properties
            String prop_name, pstr;
            for (int i = 0; i < propertiesHandler.cuiPropList.size(); i++) {
                prop_name = (String) propertiesHandler.cuiPropList.get(i);
                pstr = getProperty(prop, prop_name);
                writer.println("\t<entry key=\"" + prop_name + "\">" + pstr +
                        "</entry>");
            }
            //"map_file", "pollution_file", "scenario_file", "generation_file",
            writer.println("\t<entry key=\"map_file\">" + getMapPath() +
                    "</entry>");
            writer.println("\t<entry key=\"pollution_file\">" +
                    getPollutionPath() + "</entry>");
            writer.println("\t<entry key=\"scenario_file\">" +
                    getScenarioPath() + "</entry>");
            writer.println("\t<entry key=\"generation_file\">" +
                    getGenerationPath() + "</entry>");
            writer.println("\t<entry key=\"time_series_log_path\">" +
                    getResultPath() + "</entry>");
            //, "time_series_log_path", "loop_count",
            writer.println("\t<entry key=\"loop_count\">" + counter +
                    "</entry>");
            writer.println("</properties>");
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static String getProperty(Properties prop, String key) {
        if (prop.containsKey(key)) {
            return prop.getProperty(key);
        } else {
            System.err.println("NetmasSimulationAnalyzer.getProperty invalid" +
                    "key: " + key);
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
            formatter.printHelp("NetmasSimulationAnalyzer", options, true);
            System.exit(1);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        String propertiesPath = cli.getOptionValue("p");

        NetmasSimulationAnalyzer sim =
            new NetmasSimulationAnalyzer(propertiesPath);
        sim.envokeSimulator();
        // try {
            // FileInputStream fis = new FileInputStream(deserializeFile);
            // ObjectInputStream ois = new ObjectInputStream(fis);
            // NetmasSimulationAnalyzer sim =
                // (NetmasSimulationAnalyzer) ois.readObject();
            // sim.isDeserialized = true;
            // sim.envokeSimulator();
        // } catch (IOException ioe) {
            // ioe.printStackTrace();
        // } catch (ClassNotFoundException cnfe) {
            // cnfe.printStackTrace();
        // }
    }
}

