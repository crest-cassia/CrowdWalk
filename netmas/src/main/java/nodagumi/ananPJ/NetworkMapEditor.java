package nodagumi.ananPJ;

import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3d;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.MissingOptionException;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.Editor.EditorPanel3D;
import nodagumi.ananPJ.Editor.Panel.AgentPanel;
import nodagumi.ananPJ.Editor.Panel.BrowserPanel;
import nodagumi.ananPJ.Editor.Panel.ScenarioPanel;

import nodagumi.ananPJ.Editor.Panel.LinkPanel;
import nodagumi.ananPJ.Editor.Panel.NodePanel;
import nodagumi.ananPJ.Editor.Panel.PollutionPanel;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.Lift;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Simulator.EvacuationModelBase;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationController;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.misc.FilePathManipulation;
import nodagumi.ananPJ.misc.MapChecker;
import nodagumi.ananPJ.misc.NetmasPropertiesHandler;
import nodagumi.ananPJ.navigation.CalcPath;
import nodagumi.ananPJ.navigation.Dijkstra;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;
import nodagumi.ananPJ.navigation.CalcPath.PathChooserFactory;
import nodagumi.ananPJ.network.DaRuMaClient;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class NetworkMapEditor extends SimulationLauncher
    implements ActionListener, WindowListener, SimulationController,
           Serializable {
    private static final long serialVersionUID = -1922412587784875240L;

    private String dir_name = null;

    private static String propertiesPath = null;
    private static String mapPath = null;
    private static String pollutionPath = null; // path to pollution file
    private static String generationPath = null; // path to generation file
    private static String scenarioPath = null; // path to scenario file
    private static boolean randomNavigation = false;
    private static SpeedCalculationModel speedModel = null;
    private static int exitCount = 0;
    private static long randseed = 0;
    private int loopCount = -1;
    private static boolean isDebug = false; // debug mode
    private static boolean isTimerEnabled = false;
    private static String timerPath = null;         // path to timer log file
    private static String serializePath = null;    // path to serialized file
    private static String deserializePath = null;
    private static boolean isAllAgentSpeedZeroBreak = false;
    private int expectedDensityMacroTimeStep = 300;
    private boolean expectedDensityVisualizeMicroTimeStep = true;
    protected static boolean isTimeSeriesLog = false;
    protected static String timeSeriesLogPath = null;
    protected static int timeSeriesLogInterval = -1;
    transient boolean isDeserialized = false;

    // Properties
    public static final String[] SHOW_STATUS_VALUES = {"none", "top", "bottom"};
    public static final String[] IMAGE_TYPES = {"bmp", "gif", "jpg", "png"};
    protected int weight = 0;
    protected double verticalScale = 1.0;
    protected double agentSize = 1.0;
    protected String cameraPath = null;
    protected double zoom = 1.0;
    protected boolean recordSimulationScreen = false;
    protected String screenshotDir = "screenshots";
    protected boolean clearScreenshotDir = false;
    protected String screenshotImageType = "png";
    protected boolean simulationWindowOpen = false;
    protected boolean autoSimulationStart = false;
    protected boolean hideLinks = false;
    protected boolean densityMode = false;
    protected boolean changeAgentColorDependingOnSpeed = true;
    protected boolean showStatus = false;
    protected String showStatusPosition = "top";
    protected boolean showLogo = false;
    protected boolean show3dPolygon = true;
    protected String agentMovementHistoryPath = null;
    protected String individualPedestriansLogDir = null;

    /* copy from CUI simulator */
    private static NetmasPropertiesHandler propertiesHandler = null;

    transient private MenuBar menuBar;
    private boolean modified = false;
    transient private MenuItem calcExitPathMenu;
    transient private MenuItem calcTagPathMenu;
    transient private MenuItem loadMapFromDarumaMenuItem;
    transient private MenuItem saveMapFromDarumaMenuItem;

    transient private JTabbedPane tabbedPane = null;
    transient private NodePanel nodePanel = null;
    transient public LinkPanel linkPanel = null;
    transient public AgentPanel agentPanel = null;
    transient public PollutionPanel pollutionPanel = null;
    transient public ScenarioPanel scenarioPanel = null;
    transient public BrowserPanel browserPanel = null;


    private DaRuMaClient darumaClient = DaRuMaClient.getInstance();

    /* used in the object browser */
    public enum TabTypes {
        NODE, LINK, AGENT, POLLUTION, SCENARIO, BROWSER //FRAME 
    };
    //TODO edit group and edit pollution are still unimplemented
    public enum EditorMode {
        EDIT_NODE, EDIT_LINK, EDIT_AGENT, EDIT_GROUP, EDIT_POLLUTION,
        PLACE_NODE, PLACE_LINK, PLACE_NODE_LINK, PLACE_AGENT, PLACE_GROUP,
        PLACE_POLLUTION, BROWSE
    };

    private EditorMode mode = EditorMode.EDIT_NODE;
    transient protected JFrame frame;
    private JButton runButton = null;

    protected NetworkMapEditor(Random _random) {
        super(_random);
        random = _random;
        super.setRandomNavigation(randomNavigation);

        frame = new JFrame("Network Map Editor");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        nodePanel = new NodePanel(this);
        tabbedPane.add("Nodes", nodePanel);

        linkPanel = new LinkPanel(this);
        tabbedPane.add("Links", linkPanel);

        agentPanel = new AgentPanel(this, random);
        tabbedPane.add("Agents", agentPanel);

        pollutionPanel = new PollutionPanel(this);
        tabbedPane.add("Pollution", pollutionPanel);

        scenarioPanel = new ScenarioPanel(this, random);
        tabbedPane.add("Scenario", scenarioPanel);

        browserPanel = new BrowserPanel(this);
        tabbedPane.add("Browser", browserPanel);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JTabbedPane pane = (JTabbedPane)e.getSource();
                final JPanel selectedTab = (JPanel)pane.getSelectedComponent();
                final String panelName = selectedTab.getName();

                if (panelName == "NodePanel") {
                    if (((NodePanel)selectedTab).getPlaceCheckBox()) {
                        mode = EditorMode.PLACE_NODE;
                    } else {
                        mode = EditorMode.EDIT_NODE;
                    }
                } else if (panelName == "LinkPanel") {
                    if (((LinkPanel)selectedTab).getPlaceCheckBox()) {
                        mode = EditorMode.PLACE_LINK;
                    } else {
                        mode = EditorMode.EDIT_LINK;
                    }
                } else if (panelName == "PollutionPanel") {
                    if (((PollutionPanel)selectedTab).getPlaceCheckBox()) {
                        mode = EditorMode.PLACE_POLLUTION;
                    } else {
                        mode = EditorMode.EDIT_POLLUTION;
                    }
                } else if (panelName == "AgentPanel") {
                    mode = EditorMode.EDIT_AGENT;
                } else if (panelName == "BrowserPanel"){
                    mode = EditorMode.BROWSE;
                }
            }
        });

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.addWindowListener(this);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(new JLabel());
        runButton = new JButton("Simulate");
        runButton.addActionListener(this);
        buttonPanel.add(runButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        setup_menu();
        frame.pack();

        frame.repaint();
    }

    public void deserialize() {
        super.deserialize();

        frame = new JFrame("Network Map Editor");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        nodePanel = new NodePanel(this);
        tabbedPane.add("Nodes", nodePanel);

        linkPanel = new LinkPanel(this);
        tabbedPane.add("Links", linkPanel);

        agentPanel = new AgentPanel(this, random);
        tabbedPane.add("Agents", agentPanel);

        pollutionPanel = new PollutionPanel(this);
        tabbedPane.add("Pollution", pollutionPanel);

        scenarioPanel = new ScenarioPanel(this, random);
        tabbedPane.add("Scenario", scenarioPanel);

        browserPanel = new BrowserPanel(this);
        tabbedPane.add("Browser", browserPanel);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JTabbedPane pane = (JTabbedPane)e.getSource();
                final JPanel selectedTab = (JPanel)pane.getSelectedComponent();
                final String panelName = selectedTab.getName();

                if (panelName == "NodePanel") {
                    if (((NodePanel)selectedTab).getPlaceCheckBox()) {
                        mode = EditorMode.PLACE_NODE;
                    } else {
                        mode = EditorMode.EDIT_NODE;
                    }
                } else if (panelName == "LinkPanel") {
                    if (((LinkPanel)selectedTab).getPlaceCheckBox()) {
                        mode = EditorMode.PLACE_LINK;
                    } else {
                        mode = EditorMode.EDIT_LINK;
                    }
                } else if (panelName == "PollutionPanel") {
                    if (((PollutionPanel)selectedTab).getPlaceCheckBox()) {
                        mode = EditorMode.PLACE_POLLUTION;
                    } else {
                        mode = EditorMode.EDIT_POLLUTION;
                    }
                } else if (panelName == "AgentPanel") {
                    mode = EditorMode.EDIT_AGENT;
                } else if (panelName == "BrowserPanel"){
                    mode = EditorMode.BROWSE;
                }
            }
        });

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.addWindowListener(this);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(new JLabel());
        runButton = new JButton("Simulate");
        runButton.addActionListener(this);
        buttonPanel.add(runButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        setup_menu();
        frame.pack();

        frame.repaint();
    }
    public JFrame getFrame() { return frame; }

    public void setModified(boolean b) {
        updateAll();
        modified = b;
    }

    public void updateAll() {
        nodePanel.refresh();
        linkPanel.refresh();
        agentPanel.refresh();
        pollutionPanel.refresh();
        scenarioPanel.refresh();
        browserPanel.refresh();
        for (EditorFrame frame : networkMap.getFrames()) {
            frame.repaint();
        }
        frame.repaint();
    }

    private void update_dirname() {
        if (mapPath != null) {
            File file = new File(mapPath);
            dir_name = file.getParent() + File.separator;
        } else {
            dir_name = "";
        }
    }
    public String getDirName() {
        update_dirname();
        return dir_name;
    }

    public void setDirName(String _dir_name) {
        dir_name = _dir_name;
    }

    public void switchTab(TabTypes tt) {
        switch(tt) {
        case NODE:
            tabbedPane.setSelectedComponent(nodePanel);
            break;
        case LINK:
            tabbedPane.setSelectedComponent(linkPanel);
            break;
        case AGENT:
            tabbedPane.setSelectedComponent(agentPanel);
            break;
        case POLLUTION:
            tabbedPane.setSelectedComponent(pollutionPanel);
            break;
        case BROWSER:
            tabbedPane.setSelectedComponent(browserPanel);
            break;
        }
    }

    /* setters/getters */
    public void setMode(EditorMode _mode) {
        mode = _mode;
        switch(mode) {
        case EDIT_NODE:
            switchTab(TabTypes.NODE);
            nodePanel.setPlaceCheckBox(false);
            break;
        case PLACE_NODE:
            switchTab(TabTypes.NODE);
            nodePanel.setPlaceCheckBox(true);
            break;
        case EDIT_LINK:
            linkPanel.setPlaceCheckBox(false);
            switchTab(TabTypes.LINK);
            break;
        case PLACE_NODE_LINK:
            break;
        case PLACE_LINK:
            linkPanel.setPlaceCheckBox(true);
            switchTab(TabTypes.LINK);
            break;
        case EDIT_AGENT:
            agentPanel.setPlaceCheckBox(false);
            switchTab(TabTypes.AGENT);
            break;
        case PLACE_AGENT:
            agentPanel.setPlaceCheckBox(true);
            switchTab(TabTypes.AGENT);
            break;
        case EDIT_POLLUTION:
            pollutionPanel.setPlaceCheckBox(false);
            switchTab(TabTypes.POLLUTION);
            break;
        case PLACE_POLLUTION:
            pollutionPanel.setPlaceCheckBox(true);
            switchTab(TabTypes.POLLUTION);
            break;
        }
    }

    public EditorMode getMode() {
        return mode;
    }

    public ArrayList<MapNode> getNodes() {
        return networkMap.getNodes();
    }

    public ArrayList<MapLink> getLinks() {
        return networkMap.getLinks();
    }

    public ArrayList<EvacuationAgent> getAgents() {
        return networkMap.getAgents();
    }

    public ArrayList<OBNode> getOBElements() {
        return networkMap.getOBElements();
    }

    /* private methods */
    private void setup_menu () {
        menuBar = new MenuBar();

        /* File menu */
        Menu fileMenu = new Menu ("File");

        MenuItem mi = null;
        MenuShortcut ms = null;

        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_N);
        mi = new MenuItem("New", ms);
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        /*
        fileMenu.add(new MenuItem("-"));

        mi = new MenuItem("Connect DaRuMa");
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        loadMapFromDarumaMenuItem = new MenuItem("Load map from DaRuMa");
        loadMapFromDarumaMenuItem.addActionListener((ActionListener) this);
        loadMapFromDarumaMenuItem.setEnabled(false);        
        fileMenu.add(loadMapFromDarumaMenuItem);

        saveMapFromDarumaMenuItem = new MenuItem("Save map to DaRuMa");
        saveMapFromDarumaMenuItem.addActionListener((ActionListener) this);
        saveMapFromDarumaMenuItem.setEnabled(false);
        fileMenu.add(saveMapFromDarumaMenuItem);
        */
        fileMenu.add(new MenuItem("-"));

        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_O);
        mi = new MenuItem("Open map", ms);
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_S);
        mi = new MenuItem("Save map", ms);
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        mi = new MenuItem("Save map as");
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        fileMenu.add(new MenuItem("-"));

        mi = new MenuItem("Import nodes from file");
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        fileMenu.add(new MenuItem("-"));
        mi = new MenuItem("Serialize to file");
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        // mi = new MenuItem("Make rooms from FV-based nodes");
        // mi.addActionListener((ActionListener) this);
        // fileMenu.add(mi);

        fileMenu.add(new MenuItem("-"));
        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_Q);
        mi = new MenuItem ("Quit", ms);
        mi.addActionListener((ActionListener) this);
        fileMenu.add(mi);

        menuBar.add(fileMenu);

        Menu actionMenu = new PopupMenu("Actions");

        mi = new MenuItem("Show 3D");
        mi.addActionListener(this);
        actionMenu.add(mi);

        /*
        calcExitPathMenu = new MenuItem("Calculate exit paths");
        calcExitPathMenu.addActionListener(this);
        actionMenu.add(calcExitPathMenu);
        */

        calcTagPathMenu = new MenuItem("Calculate tag paths");
        calcTagPathMenu.addActionListener(this);
        actionMenu.add(calcTagPathMenu);

        mi = new MenuItem("Make stairs");
        mi.addActionListener(this);
        actionMenu.add(mi);
        actionMenu.add(new MenuItem("-"));

        mi = new MenuItem("Check for node in same position");
        mi.addActionListener(this);
        actionMenu.add(mi);

        mi = new MenuItem("Check reachability");
        mi.addActionListener(this);
        actionMenu.add(mi);

        actionMenu.add(new MenuItem("-"));

        mi = new MenuItem ("Simulate");
        mi.addActionListener (this);
        actionMenu.add (mi);

        menuBar.add(actionMenu);

        Menu help = new Menu("Help");
        mi = new MenuItem("Version " + getVersion());
        mi.setEnabled(false);
        help.add(mi);
        menuBar.add(help);
        frame.setMenuBar(menuBar);
    }

    public String getVersion() {
        Properties prop = new Properties();
        try {
            prop.load(this.getClass().getResourceAsStream("/netmas.properties"));
            return String.format("%s.%s.%s-%s", prop.getProperty("version"), prop.getProperty("branch"), prop.getProperty("revision"), prop.getProperty("commit_hash"));
        } catch(IOException e) {
            return "";
        }
    }

    private boolean clearAll() {
        Object[] options = {"Save", "Don't save", "Cancel"};
        if (modified) {
            int n = JOptionPane.showOptionDialog(frame,
                    "Modified files exists.\nProceed without saving?",
                    "Really clear?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2]);

            switch (n) {
            case 0:
                if (!saveMap ()) return false;
                break;
            case 1:
                break;
            case 2:
                return false;
            }
        }
        networkMap = new NetworkMap(random); 
        mapPath = null;
        updateAll();
        setModified(false);
        // System.gc();

        return true;
    }

    private boolean openMap() {
        FileDialog fd = new FileDialog(frame, "Open map", FileDialog.LOAD);
        fd.setFile(settings.get("mapfile", ""));
        fd.setDirectory(settings.get("inputdir", ""));
        fd.setVisible (true);

        if (fd.getFile() == null) return false;

        mapPath = fd.getDirectory() + fd.getFile();
        dir_name = fd.getDirectory();
        settings.put ("mapfile", fd.getFile ());
        settings.put ("inputdir", fd.getDirectory ());

        return openMapWithName();
    }

    private boolean openMapWithName() {
        if (mapPath == null)
            return false;
        String tmp = mapPath;
        clearAll();
        mapPath = tmp;
        try {
            networkMap = readMapWithName(mapPath, random);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, e.getStackTrace(),
                    "ファイルを開けません",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (networkMap == null) return false;
        // tkokada
        networkMap.setGenerationFile(generationPath);
        networkMap.setPollutionFile(pollutionPath);
        networkMap.setResponseFile(scenarioPath);
        networkMap.setupAfterLoad();
        updateAll();
        return true;
    }

    Document mapToDom() {
        String title_str = JOptionPane.showInputDialog(frame, "Title",
                mapPath);
        if (title_str == null) return null;

        // String comments_str = JOptionPane.showInputDialog(this,
        // "Comments?", "");
        Document doc = darumaClient.newDocument();
        if (!networkMap.toDOM(doc)) return null;

        return doc;
    }

    private void saveMapToDaRuMa() {
        Document doc = mapToDom();
        if (doc == null) return;

        String title_str = doc.getElementsByTagName("title").item(0)
            .getTextContent();
        System.err.println(title_str);

        /* sending to DaRuMa server */
        System.err.println(darumaClient.docToString(doc));
        Document result = darumaClient.insert(
                "http://staff.aist.go.jp/shunsuke.soeda/nodagumi/ananPJ/Network",
                "network", title_str, doc);
        if (result == null) {
            JOptionPane.showMessageDialog(frame, "Could not send message to " +
                    "DaRuMa server.", "failure", JOptionPane.ERROR_MESSAGE);
            return;
        }
        NodeList resultNodeList = result.getElementsByTagName("misp:Status");
        if (resultNodeList.getLength() == 0) {
            JOptionPane.showMessageDialog(frame,
                    "Message returned from DaRuMa server does not contain a " +
                    "Status tag\n" + darumaClient.docToString(result),
                    "failure", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Node resultNode = resultNodeList.item(0);
        if (resultNode.getTextContent().equals("FAILURE")) {
            Node errorMessageNode = result.getElementsByTagName("misp:Error")
                .item(0);
            JOptionPane.showMessageDialog(frame, errorMessageNode
                    .getTextContent(), "failure", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(frame, darumaClient.docToString(result),
                "result", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean saveMapAs() {
        FileDialog fd = new FileDialog(frame, "Export map", FileDialog.SAVE);
        fd.setFile(settings.get("mapfile", ""));
        fd.setDirectory(settings.get("inputdir", ""));
        fd.setVisible (true);

        if (fd.getFile() == null) return false;

        mapPath = fd.getDirectory() + fd.getFile();
        settings.put ("mapfile", fd.getFile ());
        settings.put ("inputdir", fd.getDirectory ());

        return saveMapWithName();
    }

    private boolean saveMap() {
        if (mapPath == null) {
            return saveMapAs();
        } else {
            return saveMapWithName();
        }
    }

    private boolean saveMapWithName() {
        if (mapPath == null) {
            JOptionPane.showMessageDialog(frame,
                    "Could no save to:\n" + mapPath,
                    "Save failed",
                    JOptionPane.ERROR_MESSAGE);

            return false;
        }

        update_dirname();
        networkMap.prepareForSave();

        try {
            FileOutputStream fos = new FileOutputStream(mapPath);

            Document doc = darumaClient.newDocument();
            networkMap.toDOM(doc);
            boolean result = darumaClient.docToStream(doc, fos);
            if (false == result) {
                JOptionPane.showMessageDialog(frame,
                        "Could no save to:\n" + mapPath
                        + "\nAn error occured while actually writing to file.",
                        "Save failed",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                    "Could no save to:\n" + mapPath,
                    "Save failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        System.err.println("saved to:" + mapPath);
        setModified(false);
        return true;

    }

    private boolean serializeToFile() {
        FileDialog fd = new FileDialog(frame, "Serialize to file",
                FileDialog.SAVE);
        fd.setFile("serialized.xml");
        fd.setDirectory("/tmp");
        fd.setVisible (true);

        if (fd.getFile() == null) return false;

        mapPath = settings.get("inputdir", "") +
            settings.get("mapfile", "");

        String serializedFile = fd.getDirectory() + fd.getFile();
        System.out.println("NetworkMapEditor.serializeToFile: " +
                serializedFile);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(serializedFile));
            oos.writeObject(this);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.out.println("NetworkMapEditor.serializeToFile: DONE!");

        return true;
    }

    private void quit () {
        if (!clearAll())
            return;

        settings.put("width", frame.getWidth());
        settings.put("height", frame.getHeight());
        Settings.save ();
        frame.dispose ();
    }

    private void show3D() {
        JFrame frame = new JFrame("3D preview of Structure");
        NetworkPanel3D panel3d= EditorPanel3D.createPanel(this, frame);
        if (panel3d == null) return;
        frame.add(panel3d);
        frame.setMenuBar(panel3d.getMenuBar());
        frame.pack();
        frame.setVisible(true);
    }

    protected void simulate() {
        make_fv_rooms();
        super.simulate(isDeserialized);
    }

    private void importFromFv() {
        FileDialog fd = new FileDialog(frame, "Import pollution grids",
                FileDialog.LOAD);
        fd.setDirectory(settings.get("inputdir", ""));
        fd.setVisible (true);

        if (fd.getFile() == null) return;

        String fv_file = fd.getDirectory() + fd.getFile();
        openFvFile(fv_file);
    }

    private void openFvFile(String filename) {
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            String line;

            /* Nodes */
            line = br.readLine();
            Pattern headPattern = Pattern.compile("Nodes");
            Matcher headMatcher = headPattern.matcher(line);
            if (!headMatcher.matches()) {
                JOptionPane.showMessageDialog(frame,
                        "Might not be a fv-based file.",
                        "Import failed",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }

            MapPartGroup group = networkMap.createGroupNode((MapPartGroup)
                    networkMap.getRoot());
            group.addTag("RoomFromFV");

            Pattern timePattern = Pattern.compile("time.+");
            Pattern nodePattern = Pattern.compile("(.+),(.+),(.+)");

            Vector3d point_min = new Vector3d(Double.MAX_VALUE,
                    Double.MAX_VALUE, Double.MAX_VALUE);
            Vector3d point_max = new Vector3d(Double.MIN_VALUE,
                    Double.MIN_VALUE, Double.MIN_VALUE);
            ArrayList<Vector3d> node_points = new ArrayList<Vector3d>();
            Map<Integer, MapPartGroup> height_group = new HashMap<Integer,
                MapPartGroup>();

            while (true) {
                line = br.readLine();
                if (line == null) break;
                Matcher timeMatcher = timePattern.matcher(line);
                if (timeMatcher.matches()) break;

                Matcher nodeMatcher = nodePattern.matcher(line);
                if (!nodeMatcher.matches()) {
                    System.err.println("?" + line);
                    continue;
                }

                double x = Double.parseDouble(nodeMatcher.group(1));
                double y = -Double.parseDouble(nodeMatcher.group(2));
                double z = Double.parseDouble(nodeMatcher.group(3));

                Integer height = (int)z;
                if (!height_group.containsKey(height)) {
                    MapPartGroup child_group = networkMap.createGroupNode(
                            group);
                    height_group.put(height, child_group);
                    child_group.addTag(""+height);
                }

                node_points.add(new Vector3d(x, y, z));

                point_min.x = Math.min(point_min.x, x);
                point_min.y = Math.min(point_min.y, y);
                point_min.z = Math.min(point_min.z, z);
                point_max.x = Math.max(point_max.x, x);
                point_max.y = Math.max(point_max.y, y);
                point_max.z = Math.max(point_max.z, z);
            }
            double width = Math.max(point_max.x - point_min.x,
                    Math.max(point_max.y - point_min.y,
                            (point_max.z - point_min.z)));

            int count = 0;
            for (Vector3d v : node_points) {
                count += 1;
                double x = (v.x  - point_min.x) / width * 1000;
                double y = (v.y  - point_min.y) / width * 1000;
                double z = (v.z  - point_min.z) / width * 1000;
                Point2D xy = new Point2D.Double(x, y);
                Integer height = (int)v.z;
                MapPartGroup child_group = height_group.get(height);
                MapNode node = networkMap.createMapNode(child_group, xy, z);
                node.addTag("FVNODE_"+count);
            }
            System.err.println(point_min.z);
            System.err.println(1/width * 1000);
        } catch (IOException e) {
            System.err.println(e);
            return;
        }
    }

    private void make_fv_rooms() {
        if (isDeserialized)
            return;

        MapPartGroup group = networkMap.createGroupNode((MapPartGroup)
                networkMap.getRoot());
        group.addTag("ROOMS");
        for (MapNode node : getNodes()) {
            Matcher match = node.matchTag("FVNODE_(\\d+)");
            if (match == null) continue;

            int count = Integer.parseInt(match.group(1));
            PollutedArea area = networkMap.createPollutedAreaPoint(group,
                    node, count);
            group.setScale(((MapPartGroup)node.getParent()).getScale());
            area.addTag("" + count);
        }
    }

    private void calcExitPaths() {
        calcExitPathMenu.setEnabled(false);
        for (MapNode node : getNodes()) {
            node.clearHints();
        }

        class DownStairsFactory implements PathChooserFactory {
            class DownStairs implements CalcPath.PathChooser {
                double height;
                public DownStairs(double _height) {
                    super();
                    height = _height;
                }
                @Override
                public double evacuationRouteCost(MapLink link) {
                    if (link.getAverageHeight() == height) return 1.0;
                    return 1000000.0;
                }

                @Override
                public boolean isExit(MapLink link) {
                    return link.getAverageHeight() < height;  
                }

                @Override
                public double initialCost(MapNode node) {
                    return 0.0;
                }
            }

            @Override
            public PathChooser generate(double height) {
                return new DownStairs(height);
            }

            @Override
            public String hintName() {
                return "Down stairs";
            }
        }

        CalcPath.calc(getNodes(), new DownStairsFactory());
        JOptionPane.showMessageDialog(frame,
                "Calculation of paths finished.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        calcExitPathMenu.setEnabled(true);
    }

    private void calcTagPaths() {
        calcTagPathMenu.setEnabled(false);
        String tag = JOptionPane.showInputDialog("tag name");
        if (tag == null) {
            calcTagPathMenu.setEnabled(true);

            return;
        }
        CalcPath.Nodes goals = new CalcPath.Nodes();
        for (MapNode node : networkMap.getNodes()) {
            if (node.hasTag(tag)) goals.add(node);
        }
        for (MapLink link : networkMap.getLinks()) {
            if (link.hasTag(tag)) {
                goals.add(link.getFrom());
                goals.add(link.getTo());
            }
        }

        if (goals.size() == 0) {
            JOptionPane.showMessageDialog(frame,
                    "no goal with tag " + tag,
                    "Failed",
                    JOptionPane.INFORMATION_MESSAGE);
            calcTagPathMenu.setEnabled(true);
            return;
        }

        Dijkstra.Result result = Dijkstra.calc(goals,
                new PathChooser() {
            public double evacuationRouteCost(MapLink link) {
                //if (link.isStair()) return 5.0;
                return 1.0;
            }
            public boolean isExit(MapLink link) {
                return false;
            }
            public double initialCost(MapNode node) {
                return 0.0;
            }
        });

        for (MapNode node : result.keySet()) {
            NodeLinkLen nll = result.get(node);
            node.addTag(tag + ":" + String.format("%10.3f",nll.len));
        }
        JOptionPane.showMessageDialog(frame,
                "Calculation of paths finished.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        calcTagPathMenu.setEnabled(true);
    }

    private void checkForPiledNodes() {
        MapChecker.checkForPiledNodes(getNodes());
    }

    private void checkForReachability() {
        ArrayList<MapLink> reachableLinks = MapChecker.getReachableLinks(
                getNodes());

        int notConnectedCount = getLinks().size() - reachableLinks.size();
        if (notConnectedCount > 0) {
            int ret = JOptionPane.showConfirmDialog(frame,
                    "There were " + notConnectedCount +
                    "links not leading to exit!\n" +
                    "Should select REACHABLE links?",
                    "Isolated links\n",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (ret == JOptionPane.YES_OPTION) {
                for (MapLink link : reachableLinks) {
                    link.selected = true;
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame,
                    "Calculation of paths finished.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        }
    }

    public void placeAgentsRandomly(String tag) {
        if (!agentPanel.agentFactory.placeAgentsRandomly(tag)) return;
        setModified(true);
        agentPanel.refresh();
    }

    public void placeAgentsEvenly(String tag) {
        if (!agentPanel.agentFactory.placeAgentsEvenly(tag)) return;
        setModified(true);
        agentPanel.refresh();
    }

    public void makeLifts() {
        MapNode selectedNode = null;
        MapPartGroup parent = null;
        for (MapNode node : getNodes()) {
            if (node.selected) {
                if (selectedNode != null) {
                    JOptionPane.showMessageDialog(frame,
                            "Only one node should be selected",
                            "Failed",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                selectedNode = node;
                parent = (MapPartGroup)node.getParent();
            }
        }

        if (selectedNode == null) {
            JOptionPane.showMessageDialog(frame,
                    "One node should be selected",
                    "Failed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        ArrayList<MapNode> nodeCandidatesToConnect = new ArrayList<MapNode>();
        final Point2D pos = selectedNode.getAbsoluteCoordinates();
        for (MapNode node : getNodes()) {
            if (node.getAbsoluteCoordinates().equals(pos)) {
                nodeCandidatesToConnect.add(node);
            }
        }

        ArrayList<MapPartGroup> nodesToAdd = new ArrayList<MapPartGroup>();
        for (MapPartGroup floor : networkMap.getGroups()) {
            final double height = floor.getDefaultHeight();
            boolean found = false;
            if (selectedNode.isOffspring(floor)) {
                continue;
            }
            for (MapNode node : nodeCandidatesToConnect) {
                if (node.getHeight() == height) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                nodesToAdd.add(floor);
            }
        }

        class CreateLifts extends JDialog 
        implements ActionListener {
            private static final long serialVersionUID = -5056607959020407478L;
            public ArrayList<JCheckBox> checkBoxes = new
                ArrayList<JCheckBox>();
            public JSpinner length, width;
            public JTextField label;

            /* Constructor */
            public CreateLifts(Point2D pos,
                    ArrayList<MapPartGroup> toAdd,
                    ArrayList<MapNode> toConnect) {
                Container container = getContentPane();
                container.setLayout(new GridLayout(toConnect.size() +
                            toAdd.size() + 6, 1));
                container.add(new JLabel("New floors"));
                for (MapPartGroup floor : toAdd) {
                    JCheckBox cb = new JCheckBox(floor.getTagString());
                    checkBoxes.add(cb);
                    cb.setSelected(true);
                    container.add(cb);
                }
                container.add(new JLabel("Existing floors"));
                for (MapNode node : toConnect) {
                    JCheckBox cb = new JCheckBox(node.getTagString()
                            + "( at " + node.getHeight() + ")");
                    checkBoxes.add(cb);
                    cb.setSelected(true);
                    container.add(cb);
                }

                JPanel lenPanel = new JPanel(new GridLayout(1, 2));
                lenPanel.add(new JLabel("Time (per floor)"));
                length = new JSpinner(new SpinnerNumberModel(10.0, 0.0,
                            1000.0, 1.0));
                lenPanel.add(length);
                container.add(lenPanel);

                JPanel widPanel = new JPanel(new GridLayout(1, 2));
                widPanel.add(new JLabel("Capacity (per floor)"));
                width = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 1000.0,
                            1.0));
                widPanel.add(width);
                container.add(widPanel);

                JPanel labelPanel = new JPanel(new GridLayout(1, 2));
                labelPanel.add(new JLabel("Label"));
                label = new JTextField("LIFT");
                labelPanel.add(label);
                container.add(labelPanel);

                JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
                JButton ok = new JButton("OK");
                ok.addActionListener(this);
                buttonPanel.add(ok);
                JButton cancel = new JButton("Cancel");
                cancel.addActionListener(this);
                buttonPanel.add(cancel);
                ok.setSelected(true);
                container.add(buttonPanel);

                setModal(true);

                pack();
            }

            public boolean accepted = false;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    accepted = true;
                } else if (e.getActionCommand().equals("Cancel")) {
                    accepted = false;
                } else {
                    /* should not come here */
                }
                this.dispose();
            }
        }
        //TODO should check floor candidate count?
        CreateLifts createLifts = new CreateLifts(pos, nodesToAdd,
                nodeCandidatesToConnect);
        createLifts.setVisible(true);
        if (!createLifts.accepted) return;

        MapPartGroup lift_group = networkMap.createGroupNode((MapPartGroup)
                networkMap.getRoot());
        lift_group.addTag("LIFT");
        lift_group.setScale(parent.getScale());
        ArrayList<MapNode> nodesToConnect = new ArrayList<MapNode>();
        int index = 0;
        for (MapPartGroup floor : nodesToAdd) {
            boolean selected = createLifts.checkBoxes.get(index)
                .getSelectedObjects() != null;
            if (selected) {
                MapNode node = networkMap.createMapNode(lift_group,
                        pos,
                        floor.getDefaultHeight());
                for (final String tag : createLifts.label.getText().split(",")) {
                    node.addTag(tag);
                }
                networkMap.createSymLink(floor, node);
                nodesToConnect.add(node);
            }
            ++index;
        }

        for (MapNode node : nodeCandidatesToConnect) {
            boolean selected = createLifts.checkBoxes.get(index)
                .getSelectedObjects() != null;
            if (selected) {
                nodesToConnect.add(node);
            }
            ++index;
        }

        Collections.sort(nodesToConnect, new Comparator<MapNode>() {
            @Override
            public int compare(MapNode lhs, MapNode rhs) {
                return (int)((lhs.getHeight() - rhs.getHeight())*10);
            }

        });

        final double length = (Double)(createLifts.length.getValue()); 
        final double width = (Double)(createLifts.width.getValue());
        final String label = createLifts.label.getText();

        int numLifts = 0;
        while (numLifts < nodesToConnect.size() - 1) {
            Lift lift = networkMap.createLift(lift_group,
                    nodesToConnect.get(numLifts),
                    nodesToConnect.get(numLifts + 1),
                    length, width);
            for (final String tag : label.split(" ")) {
                lift.addTag(tag);
            }
            setModified(true);
            ++numLifts;
        }
        JOptionPane.showMessageDialog(frame,
                "Placed lifts on " + numLifts + " floors.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        linkPanel.refresh();
    }

    public void setVisible(boolean b) {
        frame.setVisible(b);
    }

    /* Listeners */
    /* Window action listeners*/
    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
        System.exit(0);
    }

    public void windowClosing(WindowEvent e) {
        quit();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
    }

    public void windowOpened(WindowEvent arg0) {
        if (simulationWindowOpen || autoSimulationStart) {
            // TODO: ファイルが正常に読み込まれたかどうかのチェックも必要
            if (networkMap == null || mapPath == null || generationPath == null || scenarioPath == null) {
                System.err.println("プロパティファイルの設定が足りないためシミュレーションを開始することが出来ません。");
                return;
            }
            runButton.doClick();
        }
    }

    // シミュレーションウィンドウが最初に表示された時に呼び出される
    public void simulationWindowOpenedOperation(SimulationPanel3D panel, final EvacuationModelBase model) {
        // プロパティファイルに設定された情報に従ってシミュレーションウィンドウの各コントロールの初期設定をおこなう
        if (propertiesHandler == null) {
            return;
        }
        boolean successful = true;
        if (propertiesHandler.isDefined("weight")) {
            model.getAgentHandler().setSimulationWeight(weight);
        }
        if (cameraPath != null) {
            if (panel.loadCameraworkFromFile(cameraPath)) {
                panel.setReplay(true);
            } else {
                System.err.println("Camera file の読み込みに失敗しました: " + cameraPath);
                successful = false;
            }
        }
        if (propertiesHandler.isDefined("vertical_scale")) {
            panel.setVerticalScale(verticalScale);
        }
        if (propertiesHandler.isDefined("record_simulation_screen")) {
            if (recordSimulationScreen != panel.getRecordSnapshots().isSelected()) {
                panel.getRecordSnapshots().doClick();
            }
        }
        if (propertiesHandler.isDefined("screenshot_dir")) {
            panel.setScreenshotDir(screenshotDir);
        }
        if (propertiesHandler.isDefined("clear_screenshot_dir")) {
            if (recordSimulationScreen && clearScreenshotDir) {
                FilePathManipulation.deleteFiles(screenshotDir, imageFileFilter);
            }
        }
        if (propertiesHandler.isDefined("screenshot_image_type")) {
            panel.setScreenshotImageType(screenshotImageType);
        }
        if (propertiesHandler.isDefined("debug")) {
            if (isDebug != panel.getDebugMode().isSelected()) {
                panel.getDebugMode().doClick();
            }
        }
        if (propertiesHandler.isDefined("hide_links")) {
            if (hideLinks != panel.getHideNormalLink().isSelected()) {
                panel.getHideNormalLink().doClick();
            }
        }
        if (propertiesHandler.isDefined("density_mode")) {
            if (densityMode != panel.getDensityMode().isSelected()) {
                panel.getDensityMode().doClick();
            }
        }
        if (propertiesHandler.isDefined("change_agent_color_depending_on_speed")) {
            if (changeAgentColorDependingOnSpeed != panel.getChangeAgentColorDependingOnSpeed().isSelected()) {
                panel.getChangeAgentColorDependingOnSpeed().doClick();
            }
        }
        if (propertiesHandler.isDefined("show_status")) {
            if (showStatus != panel.getShowStatus().isSelected()) {
                panel.getShowStatus().doClick();
            }
            if (showStatusPosition.equals("top") && panel.getBottom().isSelected()) {
                panel.getTop().doClick();
            }
            if (showStatusPosition.equals("bottom") && panel.getTop().isSelected()) {
                panel.getBottom().doClick();
            }
        }
        if (propertiesHandler.isDefined("show_logo")) {
            if (showLogo != panel.getShowLogo().isSelected()) {
                panel.getShowLogo().doClick();
            }
        }
        if (propertiesHandler.isDefined("show_3D_polygon")) {
            if (show3dPolygon != panel.getShow3dPolygon().isSelected()) {
                panel.getShow3dPolygon().doClick();
            }
        }
        if (propertiesHandler.isDefined("auto_simulation_start")) {
            if (successful && autoSimulationStart) {
                // ※スクリーンショットの1枚目が真っ黒になるのを防ぐため
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            // シミュレーション画面が表示されるのを待つ
                            Thread.sleep(1000);
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                System.err.println("auto simulation start");
                                model.getAgentHandler().getStartButton().doClick();
                            }
                        });
                    }
                });
                thread.start();
            }
        }
        if (propertiesHandler.isDefined("agent_movement_history_file")) {
            model.getAgentHandler().initAgentMovementHistorLogger("agent_movement_history", agentMovementHistoryPath);
        }
        if (propertiesHandler.isDefined("individual_pedestrians_log_dir")) {
            model.getAgentHandler().initIndividualPedestriansLogger("individual_pedestrians_log", individualPedestriansLogDir);
        }
    }

    // SimulationPanel3D を生成した直後に呼び出される(simulationWindowOpenedOperation ではうまく対処できない分の処理)
    public void initSimulationPanel3D(SimulationPanel3D panel) {
        if (propertiesHandler == null) {
            return;
        }
        if (propertiesHandler.isDefined("agent_size")) {
            panel.setAgentSize(agentSize);
        }
        if (propertiesHandler.isDefined("zoom")) {
            panel.setScaleOnReplay(zoom);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "New") clearAll();
        else if (e.getActionCommand() == "Quit") quit ();

        // else if (e.getActionCommand() == "Connect DaRuMa") connectDaRuMa();
        // else if (e.getActionCommand() == "Load map from DaRuMa")
            // loadMapFromDaRuMa();
        else if (e.getActionCommand() == "Save map to DaRuMa")
            saveMapToDaRuMa();
        else if (e.getActionCommand() == "Open map") openMap();
        else if (e.getActionCommand() == "Save map") saveMap();
        else if (e.getActionCommand() == "Save map as") saveMapAs();
        //else if (e.getActionCommand() == "Merge map") mergeMap();
        else if (e.getActionCommand() == "Import nodes from file")
            importFromFv();
        else if (e.getActionCommand() == "Serialize to file")
            serializeToFile();
        else if (e.getActionCommand() == "Make rooms from FV-based nodes")
            make_fv_rooms();
        else if (e.getActionCommand() == "Calculate exit paths")
            calcExitPaths();
        else if (e.getActionCommand() == "Calculate tag paths")
            calcTagPaths();
        else if (e.getActionCommand() == "Check for node in same position")
            checkForPiledNodes();
        else if (e.getActionCommand() == "Check reachability")
            checkForReachability();
        else if (e.getActionCommand() == "Place agents randomly (no tags)")
            placeAgentsRandomly(null);
        else if (e.getActionCommand() == "Place agents evenly (no tags)")
            placeAgentsRandomly(null);
        else if (e.getActionCommand() == "Simulate") simulate();
        else if (e.getActionCommand() == "Dump(test)")
            networkMap.testDumpNodes();
        else if (e.getActionCommand() == "Show 3D") show3D();
    }

    public void setIsDebug(boolean _isDebug) {
        isDebug = _isDebug;
    }

    public boolean getIsDebug() {
        return isDebug;
    }

    public void setMapFileName(String _mapPath) {
        mapPath = _mapPath;
        openMapWithName();
    }

    public String getMapFilename() {
        return mapPath;
    }

    public void setPollutionPath(String _pollutionPath) {
        pollutionPath = _pollutionPath;
        if (networkMap != null) {
            networkMap.setPollutionFile(pollutionPath);
        }
    }

    public String getPollutionPath() {
        return pollutionPath;
    }

    public void setGenerationPath(String _generationPath) {
        generationPath = _generationPath;
        if (networkMap != null) {
            networkMap.setGenerationFile(generationPath);
        }
    }

    public String getGenerationpath() {
        return generationPath;
    }

    public void setScenarioPath(String _scenarioPath) {
        scenarioPath = _scenarioPath;
        if (networkMap != null) {
            networkMap.setResponseFile(scenarioPath);
        }
    }

    public String getScenarioPath() {
        return scenarioPath;
    }

    /**
     * Set isTimerEnabled.
     * @param isTimerEnabled the value to set.
     */
    public void setIsTimerEnabled(boolean _isTimerEnabled) {
        isTimerEnabled = _isTimerEnabled;
    }
    /**
     * Get isTimerEnabled.
     * @return isTimerEnabled as boolean.
     */
    public boolean getIsTimerEnabled() {
        return isTimerEnabled;
    }
    public void setTimerFile(String _timerPath) {
        timerPath = _timerPath;
    }

    public String getTimerFile() {
        return timerPath;
    }

    public void setSerializeFile(String _serializePath) {
        serializePath = _serializePath;
    }

    public String getSerializeFile() {
        return serializePath;
    }

    public void setDeserializePath(String _deserializePath) {
        deserializePath = _deserializePath;
    }

    public String getDeserializePath() {
        return deserializePath;
    }

    public void setRandseed(long _randseed) {
        randseed = _randseed;
    }

    public long getRandseed() {
        return randseed;
    }

    public void setRandom(Random _random) {
        random = _random;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandomNavigation(boolean _randomNavigation) {
        randomNavigation = _randomNavigation;
    }

    public boolean getRandomNavigation() {
        return randomNavigation;
    }

    public void setSpeedModel(SpeedCalculationModel _speedModel) {
        speedModel = _speedModel;
    }

    public SpeedCalculationModel getSpeedModel() {
        return speedModel;
    }

    public void setExitCount(int _exitCount) {
        exitCount = _exitCount;
    }

    public int getExitCount() {
        return exitCount;
    }

    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        isAllAgentSpeedZeroBreak = _isAllAgentSpeedZeroBreak;
    }

    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
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

    public void setProperties(String _propertiesFile) {
        propertiesHandler = new NetmasPropertiesHandler(_propertiesFile);

        setIsDebug(propertiesHandler.getIsDebug());
        // I/O handler ?
        setMapFileName(propertiesHandler.getMapPath());
        setPollutionPath(propertiesHandler.getPollutionPath());
        setGenerationPath(propertiesHandler.getGenerationPath());
        setScenarioPath(propertiesHandler.getScenarioPath());
        setIsTimerEnabled(propertiesHandler.getIsTimerEnabled());
        setTimerFile(propertiesHandler.getTimerPath());
        setSerializeFile(propertiesHandler.getSerializePath());
        setDeserializePath(propertiesHandler.getDeserializePath());
        setIsTimeSeriesLog(propertiesHandler.getIsTimeSeriesLog());
        setTimeSeriesLogPath(propertiesHandler.getTimeSeriesLogPath());
        setTimeSeriesLogInterval(propertiesHandler.getTimeSeriesLogInterval());
        setRandseed(propertiesHandler.getRandseed());
        // random はコンストラクタ引数でのみセットされるべき(randseed を反映した再現性確保のため)
        assert random != null;
        setRandomNavigation(propertiesHandler.getRandomNavigation());
        super.setRandomNavigation(randomNavigation);
        setSpeedModel(propertiesHandler.getSpeedModel());
        int tmpExitCount = propertiesHandler.getExitCount();
        if (tmpExitCount <= 0)
            tmpExitCount = 0;
        setExitCount(tmpExitCount);
        setIsAllAgentSpeedZeroBreak(propertiesHandler
                .getIsAllAgentSpeedZeroBreak());
        setExpectedDensityMacroTimeStep(propertiesHandler
                .getExpectedDensityMacroTimeStep());
        setExpectedDensityVisualizeMicroTimeStep(propertiesHandler
                .getExpectedDensityVisualizeMicroTimeStep());

        try {
            weight = propertiesHandler.getInteger("weight", weight);
            if (weight < 0 || weight > 999) {
                throw new Exception("Property error - 設定値が範囲(0～999)外です: weight:" + weight);
            }
            verticalScale = propertiesHandler.getDouble("vertical_scale", verticalScale);
            if (verticalScale < 0.1 || verticalScale > 49.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～49.9)外です: vertical_scale:" + verticalScale);
            }
            agentSize = propertiesHandler.getDouble("agent_size", agentSize);
            if (agentSize < 0.1 || agentSize > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～9.9)外です: agent_size:" + agentSize);
            }
            zoom = propertiesHandler.getDouble("zoom", zoom);
            if (zoom < 0.0 || zoom > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.0～9.9)外です: zoom:" + zoom);
            }
            cameraPath = propertiesHandler.getFilePath("camera_file", null);
            recordSimulationScreen = propertiesHandler.getBoolean("record_simulation_screen", recordSimulationScreen);
            screenshotDir = propertiesHandler.getDirectoryPath("screenshot_dir", screenshotDir).replaceFirst("[/\\\\]+$", "");
            clearScreenshotDir = propertiesHandler.getBoolean("clear_screenshot_dir", clearScreenshotDir);
            if (clearScreenshotDir && ! propertiesHandler.isDefined("screenshot_dir")) {
                throw new Exception("Property error - clear_screenshot_dir を有効にするためには screenshot_dir の設定が必要です。");
            }
            if (recordSimulationScreen && ! clearScreenshotDir && new File(screenshotDir).list(imageFileFilter).length > 0) {
                throw new Exception("Property error - スクリーンショットディレクトリに画像ファイルが残っています: screenshot_dir:" + screenshotDir);
            }
            screenshotImageType = propertiesHandler.getString("screenshot_image_type", screenshotImageType, IMAGE_TYPES);
            hideLinks = propertiesHandler.getBoolean("hide_links", hideLinks);
            densityMode = propertiesHandler.getBoolean("density_mode", densityMode);
            changeAgentColorDependingOnSpeed =
                propertiesHandler.getBoolean("change_agent_color_depending_on_speed", changeAgentColorDependingOnSpeed);
            String show_status = propertiesHandler.getString("show_status", "none", SHOW_STATUS_VALUES);
            if (show_status.equals("none")) {
                showStatus = false;
            } else {
                showStatus = true;
                showStatusPosition = show_status;
            }
            showLogo = propertiesHandler.getBoolean("show_logo", showLogo);
            show3dPolygon = propertiesHandler.getBoolean("show_3D_polygon", show3dPolygon);
            simulationWindowOpen = propertiesHandler.getBoolean("simulation_window_open", simulationWindowOpen);
            autoSimulationStart = propertiesHandler.getBoolean("auto_simulation_start", autoSimulationStart);
            agentMovementHistoryPath = propertiesHandler.getFilePath("agent_movement_history_file", null, false);
            individualPedestriansLogDir = propertiesHandler.getDirectoryPath("individual_pedestrians_log_dir", null);
            if (individualPedestriansLogDir != null) {
                individualPedestriansLogDir = individualPedestriansLogDir.replaceFirst("[/\\\\]+$", "");
            }
        } catch(Exception e) {
            //System.err.printf("Property file error: %s\n%s\n", _propertiesFile, e.getMessage());
            System.err.println(e.getMessage());
            System.exit(1);
        }

        openMapWithName();

        if (isTimerEnabled) {
            super.setIsTimerEnabled(true);
            super.setTimerFile(timerPath);
        }

        if (isTimeSeriesLog) {
            super.setIsTimeSeriesLog(true);
            super.setTimeSeriesLogPath(timeSeriesLogPath);
            super.setTimeSeriesLogInterval(timeSeriesLogInterval);
        }
        if (propertiesHandler.getIsDamageSpeedZero()) {
            super.setIsDamageSpeedZeroNumberLog(true);
            super.setDamageSpeedZeroNumberLogPath(propertiesHandler
                    .getDamageSpeedZeroPath());
        }
        if (speedModel == SpeedCalculationModel.ExpectedDensityModel) {
            super.setIsExpectedDensitySpeedModel(true);
            super.setSpeedModel(speedModel);
            super.setExpectedDensityMacroTimeStep(
                    expectedDensityMacroTimeStep);
            super.setExpectedDensityVisualizeMicroTimeStep(
                            expectedDensityVisualizeMicroTimeStep);
        }
    }

    // 画像ファイルか?
    private FilenameFilter imageFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            for (String suffix : IMAGE_TYPES) {
                if (name.endsWith("." + suffix)) {
                    return true;
                }
            }
            return false;
        }
    };

    //private static Random random = new Random();
    private static NetworkMapEditor instance = null;
    public static NetworkMapEditor getInstance() {
        if (instance == null) {
            if (randseed == 0) {
                instance = new NetworkMapEditor(new Random());
            } else {
                instance = new NetworkMapEditor(new Random(randseed));
            }
        }
        return instance;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Options options = new Options();

        options.addOption(OptionBuilder.withArgName("properties_file")
                .hasArg(true).withDescription("Path of properties file")
                .isRequired(false).create("p"));

        CommandLineParser parser = new BasicParser();
        CommandLine cli = null;

        try {
            cli = parser.parse(options, args);
        } catch (MissingOptionException moe) {
            moe.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NetworkMapEditor", options, true);
            System.exit(1);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        propertiesPath = cli.getOptionValue("p");

        // load properties
        Properties prop = new Properties();
        if (propertiesPath == null) {
            //random = new Random(randseed);
        } else {
            propertiesHandler = new NetmasPropertiesHandler(propertiesPath);

            isDebug = propertiesHandler.getIsDebug();
            mapPath = propertiesHandler.getMapPath();
            pollutionPath = propertiesHandler.getPollutionPath();
            generationPath = propertiesHandler.getGenerationPath();
            scenarioPath = propertiesHandler.getScenarioPath();
            isTimerEnabled = propertiesHandler.getIsTimerEnabled();
            timerPath = propertiesHandler.getTimerPath();
            serializePath = propertiesHandler.getSerializePath();
            deserializePath = propertiesHandler.getDeserializePath();
            randseed = propertiesHandler.getRandseed();
            //random = new Random(randseed);
            randomNavigation = propertiesHandler.getRandomNavigation();
            speedModel = propertiesHandler.getSpeedModel();
            exitCount = propertiesHandler.getExitCount();
            if (exitCount <= 0)
                exitCount = 0;
            isAllAgentSpeedZeroBreak =
                propertiesHandler.getIsAllAgentSpeedZeroBreak();
        }

        if (deserializePath != null) {
            try {
                FileInputStream fis = new FileInputStream(deserializePath);
                ObjectInputStream ois = new ObjectInputStream(fis);
                NetworkMapEditor win = (NetworkMapEditor) ois.readObject();
                win.deserialize();
                win.isDeserialized = true;
                win.setVisible(true);
                win.updateAll();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
        } else {
            NetworkMapEditor win = NetworkMapEditor.getInstance();
            win.setVisible(true);
        }
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
