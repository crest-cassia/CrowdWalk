package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Random;

import javax.swing.JFrame;

import org.w3c.dom.Document;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.MissingOptionException;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetmasCuiSimulator;
import nodagumi.ananPJ.SimulationLauncher;
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Simulator.EvacuationModelBase;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.BasicSimulationLauncher;

import nodagumi.ananPJ.misc.CommunicationHandler;
import nodagumi.ananPJ.misc.CommunicationHandler.CommunicationType;
import nodagumi.ananPJ.misc.NetMASIOHandler;
import nodagumi.ananPJ.misc.NetmasTimer;
import nodagumi.ananPJ.misc.Snapshot;

import nodagumi.ananPJ.network.DaRuMaClient;

import nodagumi.ananPJ.NetworkParts.Link.*;

import nodagumi.Itk.*;

public class NetmasPlayer extends SimulationLauncher
        implements Serializable {

    protected int bufsize = 1024;

    private EvacuationSimulator model;

    private static boolean isDebug = false; // debug mode
    private static String addr = null;      // IP address of destination host
    private static int port = -1;           // port number of destination host
    private static int interval = 100;      // sleep time(msec) during loop

    private static String propertiesPath = null;    // java.util.Properties
    private static String serializeFile = null;    // path to serialized file
    private static String timerFile = null;         // path to timer log file
    private static String deserializeFile = null;

    private static ObjectOutputStream oos = null;
    private static boolean isTimerEnabled = false;
    private static int counter;
    private static boolean isDeserialized = false;
    private static boolean finished = false;

    private transient NetMASIOHandler handler = null;

    private static CommunicationType type = null;   // file or pipe or network
    private static NetmasTimer timer = null;
    //private DaRuMaClient darumaClient = DaRuMaClient.getInstance();

    protected transient SimulationPanel3D panel = null;
    protected transient JFrame simulation_frame = null;
    private static long randseed = 0;
    private static Random random = null;

    public NetmasPlayer(String title, Random _random) {
        super(_random);
        random = _random;
    }

    private static NetmasCuiSimulator ncs = null;
    private String scenarioSerial = null;
    //private void envokeSimulator(NetworkMap network_map) {
    private void envokeSimulator() {

        handler = new NetMASIOHandler(type, isDebug, addr, port,
                serializeFile, null);
        handler.start();

        Object obj = null;
        while(true) {
            obj = null;
            obj = handler.readStream();
            if (obj != null) {
                /*
                System.err.println("NetmasPlayer.envokeSimulator obj " +
                        obj);
                */
                ncs = null;
                ncs = (NetmasCuiSimulator) obj;
                model = null;
                model = (EvacuationSimulator) ncs.getModel();
                model.deserialize(model.getMap(), this,
                        model.getScenario_serial());
                model.begin(true, true, panel);
                panel = model.getPanel3D();
                //panel = model.getPanel3D();
                model.updateWithSerialize();
                /*
                System.err.println("NetmasPlayer.envokeSimulator model tick" +
                        " count: " + model.getTickCount() + " panel: " +
                        panel);
                /*/
                //panel.printAgents();
                panel.repaint();
                panel.setIsInitialized(true);
            } else {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    static NetmasPlayer instance = null;
    public static NetmasPlayer getInstance() {
        if (instance == null) {
            instance = new NetmasPlayer("NetmasPlayer", random);
        }
        return instance;
    }

    private void printInputtedOptions() {
        System.err.flush();
        System.out.flush();
        System.out.println("--- NetmasPlayer: inputted options ---");
        System.out.println("  type:        " +
                CommunicationHandler.CommunicationType2String(type));

        System.out.println("  debug mode:  " + isDebug);
        System.out.println("  port:        " + port);
        System.out.println("  serialized:  " + serializeFile);
        System.out.println("  timer log:   " + timerFile);
        System.out.println("--------------------------------------------");
        System.out.flush();
    }

    private static void printHelp(Options options) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NetmasPlayer", options, true);
    }

    private static String getProperty(Properties prop, String key) {
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
            formatter.printHelp("NetmasPlayer", options, true);
            System.exit(1);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        propertiesPath = cli.getOptionValue("p");

        // load properties
        Properties prop = new Properties();
        try {
            prop.loadFromXML(new FileInputStream(propertiesPath));

            String typestr = getProperty(prop, "type");
            if (typestr == null)
                type = CommunicationType.RCV_FILE;
            else if (typestr.equals("buffer"))
                type = CommunicationType.RCV_BUFFER;
            else if (typestr.equals("file"))
                type = CommunicationType.RCV_FILE;
            else if (typestr.equals("pipe"))
                type = CommunicationType.RCV_PIPE;
            else if (typestr.equals("network"))
                type = CommunicationType.RCV_NETWORK;
            else {
                System.err.println("NetmasPlayer: invalid inputted " +
                        "type:" + typestr);
                type = CommunicationType.NONE;
            }
            String debugstr = getProperty(prop, "debug");
            if (debugstr == null)
                isDebug = false;
            else if (debugstr.equals("true"))
                isDebug = true;
            String timerstr = getProperty(prop, "timer");
            if (timerstr == null)
                isTimerEnabled = false;
            else if (timerstr.equals("true"))
                isTimerEnabled = true;
            timerFile = getProperty(prop, "timer_file");
            addr = getProperty(prop, "addr");
            String portString = getProperty(prop, "port");
            if (portString != null)
                port = Integer.parseInt(portString);
            String intervalString = getProperty(prop, "interval");
            if (intervalString != null)
                interval = Integer.parseInt(intervalString);
            serializeFile = getProperty(prop, "serialize_file");
            deserializeFile = getProperty(prop, "deserialized_file");
            // create random with seed
            String randseedString = getProperty(prop, "randseed");
            if (randseedString != null)
                randseed = Integer.parseInt(randseedString);
            random = new Random(randseed);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // check inputted options
        if (type == CommunicationType.NONE) {
            System.err.println("NetmasPlayer: invalid type:" +
                    type);
            printHelp(options);
            System.exit(1);
        } else if (type == CommunicationType.RCV_FILE &&
                deserializeFile == null) {
            System.err.println("NetmasPlayer: file mode requires a " +
                    "deserialize file.");
            printHelp(options);
            System.exit(1);
        } else if (type == CommunicationType.RCV_NETWORK && port <= 0) {
            System.err.println("NetmasPlayer: network mode " +
                    "requires port number.");
            printHelp(options);
            System.exit(1);
        }

        NetmasPlayer sim = NetmasPlayer.getInstance();
        sim.printInputtedOptions();
        sim.start();
        sim.envokeSimulator();
    }
}

