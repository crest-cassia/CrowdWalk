package nodagumi.ananPJ.misc;

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
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;

import nodagumi.ananPJ.misc.CommunicationHandler;
import nodagumi.ananPJ.misc.CommunicationHandler.CommunicationType;
import nodagumi.ananPJ.misc.NetMASIOHandler;
import nodagumi.ananPJ.misc.NetmasTimer;
import nodagumi.ananPJ.misc.Snapshot;
import nodagumi.ananPJ.Simulator.EvacuationModelBase;

import nodagumi.ananPJ.network.DaRuMaClient;

import nodagumi.ananPJ.misc.NetmasPropertiesHandler;


/** NetmasSimulationAnalyzer manages NetmasCuiSimulator with rules, executes
 * it in the loop and analyzes the results.
 * The rule are given in property file and 
 *
 *
 *
 */
public class NetmasPatternGenerator implements Serializable {

    private static final long serialVersionUID = 50121231L;

    private boolean isSaveMap = false;

    private String resultPath = null;

    private String propertiesPath = null;    // java.util.Properties
    //private NetmasPropertiesHandler propertiesHandler = null;
    private String patternType = null;
    private String keyTag = null;
    private int numberOfTags = -1;
    private String[] setTags = null;

    public NetmasPatternGenerator(String _propertiesPath) {
        super();
        // load properties
        Properties prop = new Properties();
        try {
            prop.loadFromXML(new FileInputStream(_propertiesPath));
            // pattern type
            patternType = NetmasPropertiesHandler.getStringProperty(prop,
                    "type");
            if (patternType == null) {
                System.err.println("NetmasPatternGenerator invalid type");
                return;
            }
            keyTag = NetmasPropertiesHandler.getStringProperty(prop,
                    "key_tag");
            if (keyTag == null) {
                System.err.println("NetmasPatternGenerator invalid key tag");
                return;
            }
            numberOfTags = NetmasPropertiesHandler.getIntegerProperty(prop,
                    "tag_number");
            if (numberOfTags <= 0) {
                System.err.println("NetmasPatternGenerator invalid tag " +
                        "number");
                return;
            }
            setTags = new String[numberOfTags];
            for (int i = 0; i < numberOfTags; i++) {
                String setTag = NetmasPropertiesHandler.getStringProperty(prop,
                        "set_tag" + i);
                if (setTag == null) {
                    System.err.println("NetmasPatternGenerator invalid set " +
                            "tag");
                    return;
                }
                setTags[i] = setTag;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public boolean setOnewayBridgeTagPattern(NetworkMap map, int index) {
        MapLinkTable links = map.getLinks();
        MapLinkTable matchLinks = new MapLinkTable();
        for (MapLink link : links) {
            if (link.hasTag(keyTag))
                matchLinks.add(link);
        }
        int[] patterns = generatePattern(index, numberOfTags,
                matchLinks.size());
        if (patterns == null || patterns.length != matchLinks.size()) {
            System.err.println("NetmasPatternGenerator." +
                    "setOnewayBridgeTagPattern invalid pattern.");
            return false;
        }
        MapLinkTable pathways = null;
        String onewayBridgeString = "";
        for (int i = 0; i < matchLinks.size(); i++) {
            String patternString;
            if (patternType.equals("set-tag-for-tag")) {
                patternString = setTags[patterns[i]];
                if (patternString == null) {
                    System.err.println("NetmasPatternGenerator." +
                            "setOnewayBridgeTagPattern null patternString: " +
                            setTags);
                }
                ((MapLink) matchLinks.get(i)).addTag(patternString);
            } else if (patternType.equals("set-tag-for-tags")) {
                patternString = setTags[patterns[i]];
                ((MapLink) matchLinks.get(i)).addTag(patternString);
                if (patterns[i] == 1) {
                    MapNode fromNode = ((MapLink) matchLinks.get(i)).getFrom();
                    pathways = fromNode.getPathways();
                    for (MapLink link : pathways) {
                        if (link.hasTag("JOINT-BRIDGE")) {
                            link.addTag(patternString);
                            break;
                        }
                    }
                    MapNode toNode = ((MapLink) matchLinks.get(i)).getTo();
                    pathways = toNode.getPathways();
                    for (MapLink link : pathways) {
                        if (link.hasTag("JOINT-BRIDGE")) {
                            link.addTag(patternString);
                            break;
                        }
                    }
                    patternString += "3";
                } else if (patterns[i] == 2) {
                    MapNode fromNode = ((MapLink) matchLinks.get(i)).getFrom();
                    pathways = fromNode.getPathways();
                    for (MapLink link : pathways) {
                        if (link.hasTag("JOINT-BRIDGE")) {
                            link.addTag(patternString);
                            break;
                        }
                    }
                    MapNode toNode = ((MapLink) matchLinks.get(i)).getTo();
                    pathways = toNode.getPathways();
                    for (MapLink link : pathways) {
                        if (link.hasTag("JOINT-BRIDGE")) {
                            link.addTag(patternString);
                            break;
                        }
                    }
                    patternString += "3";
                }
            } else {
                System.err.println("NetmasPatternGenerator" +
                    ".setOnewayBridgeTagPattern invalid type!");
                return false;
            }
            onewayBridgeString += "Node: " + matchLinks.get(i).ID + " " +
                patternString + "\n";
        }
        if (isSaveMap) {
            File onewayBridgeFile = new File(getResultPath() +
                    "/oneway-bridge-setting.txt");
            // add codes
            File onewayDirectory = onewayBridgeFile.getParentFile();
            if (onewayDirectory != null && !onewayDirectory.exists())
                onewayDirectory.mkdirs();
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(
                                new FileOutputStream(onewayBridgeFile, false),
                                "utf-8")), true);
                writer.write(onewayBridgeString);
                writer.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return true;
    }

    /**
     * generate int array which contain pattern values.
     * the values of int array are 0, 1, 2..., kinds - 1. Each value means the
     * pattern. This function just convert index to the int array. After using
     * this function, user program has to change the int array to patterns.
     */
    private int[] generatePattern(int index, int kinds, int number) {
        int maxIndex = (int) Math.pow(kinds, number) - 2;
        if (index >= maxIndex) {
            System.err.println("NetmasSimulationAnalyzer.generatePattern " +
                    "inputted index is too big.");
            return null;
        }

        int numberOfException = 0;
        int[] retVals = new int[number];
        int generatedIndex = 0;
        while (generatedIndex <= index) {
            int calcIndex = generatedIndex + numberOfException;
            boolean generated = false;
            while (!generated) {
                calcIndex = generatedIndex + numberOfException;
                for (int j = 0; j < number; j++) {
                    retVals[j] = (int) calcIndex % kinds;
                    calcIndex = (calcIndex - retVals[j]) / kinds;
                }
                if (isException(retVals)) {
                    numberOfException += 1;
                } else
                    generated = true;
            }
            generatedIndex += 1;
        }
        System.err.print("NetmasPatternGenerator.setOnewayBridgeTagPattern" +
                " index: " + index);
        for (int i = 0; i < number; i++) {
            System.err.print(" " + retVals[i]);
        }
        System.err.print("\n");
        return retVals;
    }

    private boolean isException(int[] patterns) {
        int[] exceptionValues = new int[2];
        exceptionValues[0] = 1;
        exceptionValues[1] = 2;
        int[] checkValues = new int[patterns.length];
        boolean exceptionEnable = true;
        if (exceptionEnable) {
            /*
            for (int i = 0; i < exceptionValues.length; i++) {
                boolean existException = true;
                for (int j = 0; j < checkValues.length; j++)
                    checkValues[i] = exceptionValues[i];
                for (int j = 0; j < patterns.length; j++) {
                    if (patterns[j] != checkValues[j]) {
                        existException = false;
                        break;
                    }
                }
                if (existException)
                    return true;
            }
            */
            if (patterns[0] == 1 && patterns[1] == 1 &&
                    patterns[2] == 1 && patterns[3] == 1)
                return true;
            if (patterns[0] == 2 && patterns[1] == 2 &&
                    patterns[2] == 2 && patterns[3] == 2)
                return true;
        }
        /*
        boolean notException = false;
        for (int i = 0; i < exceptionValues.length; i++) {
            for (int j = 0; j < patterns.length; j++) {
                if (patterns[j] != exceptionValues[i]) {
                    notException = true;
                    break;
                }
            }
            if (!notException)
                return true;
        }*/
        return false;
    }

    private static String getProperty(Properties prop, String key) {
        if (prop.containsKey(key)) {
            return prop.getProperty(key);
        } else {
            return null;
        }
    }

    public String getResultPath() {
        return resultPath;
    }

    public void setResultPath(String _resultPath) {
        resultPath = _resultPath;
    }

    public boolean getIsSaveMap() {
        return isSaveMap;
    }

    public void setIsSaveMap(boolean _isSaveMap) {
        isSaveMap = _isSaveMap;
    }
}

