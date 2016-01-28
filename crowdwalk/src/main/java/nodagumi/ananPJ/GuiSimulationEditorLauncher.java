// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.FlowLayout;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import java.util.Random;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nodagumi.ananPJ.Gui.NetworkPanel3D;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.Editor.EditorPanel3D;
import nodagumi.ananPJ.Editor.Panel.BrowserPanel;
import nodagumi.ananPJ.Editor.Panel.ScenarioPanel;
import nodagumi.ananPJ.Editor.Panel.LinkPanel;
import nodagumi.ananPJ.Editor.Panel.NodePanel;
import nodagumi.ananPJ.Editor.Panel.AreaPanel;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.FilePathManipulation;
import nodagumi.ananPJ.misc.MapChecker;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.navigation.CalcPath;
import nodagumi.ananPJ.navigation.Dijkstra;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;
import nodagumi.ananPJ.navigation.CalcPath.PathChooserFactory;

import nodagumi.Itk.Itk;
import nodagumi.Itk.ItkXmlUtility;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * マップエディタ
 */
public class GuiSimulationEditorLauncher
    implements ActionListener, WindowListener {

    private String dir_name = "";

    /**
     * GUI の設定情報
     */
    private Settings settings;

    /**
     * 属性を扱うハンドラ
     */
    private CrowdWalkPropertiesHandler properties = null;

    /**
     * 読み込んだ属性情報のファイル名
     */
    private String propertiesFile = null;

    /**
     * 乱数生成器。
     */
    private Random random = null;

    /**
     * 設定ファイルの取りまとめ。
     */
    private SetupFileInfo setupFileInfo = new SetupFileInfo();

    /**
     * コマンドラインで指定された fallback 設定
     */
    private ArrayList<String> commandLineFallbacks = null;

    /**
     * 地図データ。
     */
    private NetworkMap networkMap;

    private boolean simulationWindowOpen = false;
    private boolean autoSimulationStart = false;

    transient private MenuBar menuBar;
    private boolean modified = false;
    transient private MenuItem calcExitPathMenu;
    transient private MenuItem calcTagPathMenu;

    transient private JTabbedPane tabbedPane = null;
    transient private NodePanel nodePanel = null;
    transient public LinkPanel linkPanel = null;
    transient public AreaPanel areaPanel = null;
    transient public ScenarioPanel scenarioPanel = null;
    transient public BrowserPanel browserPanel = null;

    /* used in the object browser */
    public enum TabTypes {
        NODE, LINK, AREA, SCENARIO, BROWSER //FRAME
    };
    //TODO edit group and edit area are still unimplemented
    public enum EditorMode {
        EDIT_NODE, EDIT_LINK, EDIT_GROUP, EDIT_AREA,
        PLACE_NODE, PLACE_LINK, PLACE_NODE_LINK, PLACE_GROUP,
        PLACE_AREA, BROWSE
    };

    private EditorMode mode = EditorMode.EDIT_NODE;
    transient private JFrame frame;

    /**
     * シミュレーション開始ボタン(2D シミュレータ用)
     */
    private JButton runButton2d = null;

    /**
     * シミュレーション開始ボタン(3D シミュレータ用)
     */
    private JButton runButton3d = null;

    public GuiSimulationEditorLauncher(Random _random, Settings _settings) {
        random = _random ;
        networkMap = new NetworkMap() ;
        settings = _settings;

        frame = new JFrame("Network Map Editor");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        nodePanel = new NodePanel(this);
        tabbedPane.add("Nodes", nodePanel);

        linkPanel = new LinkPanel(this);
        tabbedPane.add("Links", linkPanel);

        areaPanel = new AreaPanel(this);
        tabbedPane.add("Area", areaPanel);

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
                } else if (panelName == "AreaPanel") {
                    if (((AreaPanel)selectedTab).getPlaceCheckBox()) {
                        mode = EditorMode.PLACE_AREA;
                    } else {
                        mode = EditorMode.EDIT_AREA;
                    }
                } else if (panelName == "BrowserPanel"){
                    mode = EditorMode.BROWSE;
                }
            }
        });

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.addWindowListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        runButton2d = new JButton("2D Simulate");
        runButton2d.setEnabled(false);
        runButton2d.addActionListener(e -> {
            runButton2d.setEnabled(false);
            runButton3d.setEnabled(false);
            new GuiSimulationLauncher2D(random, properties, setupFileInfo,
                    networkMap, settings).simulate();
        });
        buttonPanel.add(runButton2d);

        runButton3d = new JButton("3D Simulate");
        runButton3d.setEnabled(false);
        runButton3d.addActionListener(e -> {
            runButton2d.setEnabled(false);
            runButton3d.setEnabled(false);
            new GuiSimulationLauncher3D(random, properties, setupFileInfo,
                    networkMap, settings).simulate();
        });
        buttonPanel.add(runButton3d);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        setup_menu();
        int x = settings.get("editorPositionX", 0);
        int y = settings.get("editorPositionY", 0);
        int width = settings.get("editorWidth", 800);
        int height = settings.get("editorHeight", 700);
        frame.setBounds(x, y, width, height);

        frame.repaint();
    }

    public JFrame getFrame() { return frame; }

    public void setModified(boolean b) {
        modified = b;
    }

    public void updateAll() {
        nodePanel.refresh();
        linkPanel.refresh();
        areaPanel.refresh();
        scenarioPanel.refresh();
        browserPanel.refresh();
        for (EditorFrame frame : networkMap.getFrames()) {
            frame.repaint();
        }
        frame.repaint();
    }

    private void update_dirname() {
        if (getNetworkMapFile() != null) {
            File file = new File(getNetworkMapFile());
            dir_name = file.getParent() + File.separator;
        } else {
            dir_name = "";
        }
    }

    /**
     * マップファイルのディレクトリパス
     *
     * マップファイルの入出力以前なら "" を返す。
     */
    public String getDirName() {
        update_dirname();
        return dir_name;
    }

    public void switchTab(TabTypes tt) {
        switch(tt) {
        case NODE:
            tabbedPane.setSelectedComponent(nodePanel);
            break;
        case LINK:
            tabbedPane.setSelectedComponent(linkPanel);
            break;
        case AREA:
            tabbedPane.setSelectedComponent(areaPanel);
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
        case EDIT_AREA:
            areaPanel.setPlaceCheckBox(false);
            switchTab(TabTypes.AREA);
            break;
        case PLACE_AREA:
            areaPanel.setPlaceCheckBox(true);
            switchTab(TabTypes.AREA);
            break;
        }
    }

    public EditorMode getMode() {
        return mode;
    }

    public MapNodeTable getNodes() {
        return networkMap.getNodes();
    }

    public MapLinkTable getLinks() {
        return networkMap.getLinks();
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

        // fileMenu.add(new MenuItem("-"));
        // mi = new MenuItem("Import nodes from file");
        // mi.addActionListener((ActionListener) this);
        // fileMenu.add(mi);

        //[2015.04.19 I.Noda] remove serialize facilities.
        //fileMenu.add(new MenuItem("-"));
        //mi = new MenuItem("Serialize to file");
        //mi.addActionListener((ActionListener) this);
        //fileMenu.add(mi);

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

    public static String getVersion() {
        Properties prop = new Properties();
        try {
            prop.load(GuiSimulationEditorLauncher.class.getResourceAsStream("/CrowdWalk.properties"));
            return String.format("%s.%s.%s-%s", prop.getProperty("version"), prop.getProperty("branch"), prop.getProperty("revision"), prop.getProperty("commit_hash"));
        } catch(IOException e) {
            return "";
        }
    }

    private boolean clearAll() {
        if (! checkModified()) {
            return false;
        }
        if (properties != null) {
            // NetworkMap の生成時に random オブジェクトを初期化する
            // (CUIモードとGUIモードでシミュレーション結果を一致させるため)
            random.setSeed(properties.getRandseed());
        }
        networkMap = new NetworkMap() ;
        setNetworkMapFile(null);
        updateAll();
        // System.gc();

        return true;
    }

    private boolean openMap() {
        FileDialog fd = new FileDialog(frame, "Open map", FileDialog.LOAD);
        fd.setFile(settings.get("mapFile", ""));
        fd.setDirectory(settings.get("mapDir", ""));
        fd.setVisible (true);

        if (fd.getFile() == null) return false;

        setNetworkMapFile(fd.getDirectory() + fd.getFile());

        return openMapWithName();
    }

    /**
     * マップデータのセーブし忘れを防ぐためのチェック
     */
    private boolean checkModified() {
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
        setModified(false);

        return true;
    }

    private NetworkMap readMapWithName(String file_name)
            throws IOException {
        FileInputStream fis = new FileInputStream(file_name);
        Document doc = ItkXmlUtility.singleton.streamToDoc(fis);
        if (doc == null) {
            System.err.println("ERROR Could not read map.");
            return null;
        }
        NodeList toplevel = doc.getChildNodes();
        if (toplevel == null) {
            System.err.println("readMapWithName invalid inputted DOM object.");
            return null;
        }
        // NetMAS based map
        NetworkMap network_map = new NetworkMap() ;
        if (false == network_map.fromDOM(doc))
            return null;
        Itk.logInfo("Load Map File", file_name) ;
        return network_map;
    }

    private boolean openMapWithName() {
        String mapFileName = getNetworkMapFile();
        if (mapFileName == null)
            return false;
        clearAll();
        setNetworkMapFile(mapFileName);
        try {
            networkMap = readMapWithName(mapFileName) ;
        } catch (IOException e) {
            Itk.logError("Can't Open Map File", mapFileName) ;
            e.printStackTrace() ;
            JOptionPane.showMessageDialog(frame, 
                                          ("Can't Open Map File: " + mapFileName),
                                          "Can't Open Map File",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (networkMap == null) return false;

        updateAll();
        return true;
    }

    private boolean saveMapAs() {
        FileDialog fd = new FileDialog(frame, "Export map", FileDialog.SAVE);
        fd.setFile(settings.get("mapFile", ""));
        fd.setDirectory(settings.get("mapDir", ""));
        fd.setVisible (true);

        if (fd.getFile() == null) return false;

        setNetworkMapFile(fd.getDirectory() + fd.getFile());

        return saveMapWithName();
    }

    private boolean saveMap() {
        if (getNetworkMapFile() == null) {
            return saveMapAs();
        } else {
            return saveMapWithName();
        }
    }

    private boolean saveMapWithName() {
        if (getNetworkMapFile() == null) {
            JOptionPane.showMessageDialog(frame,
                                          "Could no save to:\n"
                                          + getNetworkMapFile(),
                                          "Save failed",
                                          JOptionPane.ERROR_MESSAGE);

            return false;
        }

        try {
            FileOutputStream fos =
                new FileOutputStream(getNetworkMapFile());

            Document doc = ItkXmlUtility.singleton.newDocument() ;
            networkMap.toDOM(doc);
            boolean result =
                ItkXmlUtility.singleton.docToStream(doc, fos);
            if (false == result) {
                JOptionPane.showMessageDialog(frame,
                                              "Could no save to:\n"
                                              + getNetworkMapFile()
                                              + "\nAn error occured while actually writing to file.",
                                              "Save failed",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Could no save to:\n"
                                          + getNetworkMapFile(),
                                          "Save failed",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        System.err.println("saved to:" + getNetworkMapFile());
        setModified(false);
        return true;

    }

    private void quit () {
        if (! checkModified()) {
            return;
        }
        settings.put("editorPositionX", frame.getLocationOnScreen().x);
        settings.put("editorPositionY", frame.getLocationOnScreen().y);
        settings.put("editorWidth", frame.getWidth());
        settings.put("editorHeight", frame.getHeight());
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

	/* [2014.12.26 I.Noda]
	 * "Exit" の代わりにターゲットを指定するようにした。
	 */
    private void checkForReachability(String targetTag) {
        MapLinkTable reachableLinks = 
			MapChecker.getReachableLinks(getNodes(),targetTag) ;

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

    public void setVisible(boolean b) {
        frame.setVisible(b);
    }

    /**
     * シミュレーション開始ボタンの状態を更新する
     *
     * シミュレーションの実行に必要なファイル名が全てセットされていればボタンを有効にする。
     */
    public void updateRunButton() {
        if (getNetworkMapFile() == null || getNetworkMapFile().isEmpty()
                || getGenerationFile() == null || getGenerationFile().isEmpty()
                || getScenarioFile() == null || getScenarioFile().isEmpty()) {
            runButton2d.setEnabled(false);
            runButton3d.setEnabled(false);
        } else {
            runButton2d.setEnabled(true);
            runButton3d.setEnabled(true);
        }
    }

    /**
     * GUI シミュレータを起動する
     */
    public void simulate() {
        GuiSimulationLauncher launcher;
        if (CrowdWalkLauncher.use2dSimulator) {
            launcher = new GuiSimulationLauncher2D(random, properties,
                    setupFileInfo, networkMap, settings);
        } else {
            launcher = new GuiSimulationLauncher3D(random, properties,
                    setupFileInfo, networkMap, settings);
        }
        launcher.simulate();
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
            if (getNetworkMapFile() == null || getGenerationFile() == null || getScenarioFile() == null) {
                System.err.println("プロパティファイルの設定が足りないためシミュレーションを開始することが出来ません。");
                return;
            }
            if (CrowdWalkLauncher.use2dSimulator) {
                runButton2d.doClick();
            } else {
                runButton3d.doClick();
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "New") clearAll();
        else if (e.getActionCommand() == "Quit") quit ();
        else if (e.getActionCommand() == "Open map") openMap();
        else if (e.getActionCommand() == "Save map") saveMap();
        else if (e.getActionCommand() == "Save map as") saveMapAs();
        //else if (e.getActionCommand() == "Merge map") mergeMap();
        // else if (e.getActionCommand() == "Import nodes from file")
        //     importFromFv();
        // else if (e.getActionCommand() == "Make rooms from FV-based nodes")
        //     make_fv_rooms();
        else if (e.getActionCommand() == "Calculate exit paths")
            calcExitPaths();
        else if (e.getActionCommand() == "Calculate tag paths")
            calcTagPaths();
		else if (e.getActionCommand() == "Check for node in same position")
			checkForPiledNodes();
		else if (e.getActionCommand() == "Check reachability")
			/* [2014.12.26 I.Noda]
			 * ターゲットタグが必要だが、現状、指定しようがないので、
			 * コメントアウト
			 */
			//checkForReachability(targetTag);
			Itk.logError("!!! checkForReachability() needs target now !!!") ;
        else if (e.getActionCommand() == "Simulate") simulate();
        else if (e.getActionCommand() == "Show 3D") show3D();
    }

    public NodePanel getNodePanel() { return nodePanel; }

    public LinkPanel getLinkPanel() { return linkPanel; }

    public AreaPanel getAreaPanel() { return areaPanel; }

    public ScenarioPanel getScenarioPanel() { return scenarioPanel; }

    public BrowserPanel getBrowserPanel() { return browserPanel; }

    /**
     * ファイル選択ダイアログの初期値を最適な値にセットする
     */
    public void setFileDialogPath(FileDialog fd, String path, String item) {
        if (path == null || path.isEmpty()) {
            String dir = settings.get(item + "Dir", "");
            String fileName = settings.get(item + "File", "");
            if (dir.isEmpty()) {
                dir = getDirName();
                fileName = "";
            }
            fd.setDirectory(dir);
            fd.setFile(fileName);
        } else {
            File file = new File(path);
            fd.setDirectory(file.getParent());
            fd.setFile(file.getName());
        }
    }

    /**
     * マップ取得
     */
    public NetworkMap getMap() { return networkMap; }

    public SetupFileInfo getSetupFileInfo() { return setupFileInfo; }

    //------------------------------------------------------------
    // Pathへのアクセスメソッド
    //------------------------------------------------------------
    /**
     *
     */
    public void setNetworkMapFile(String _mapPath) {
        setupFileInfo.setNetworkMapFile(_mapPath) ;
        if (_mapPath == null || _mapPath.isEmpty()) {
            settings.put("mapDir", "");
            settings.put("mapFile", "");
        } else {
            File file = new File(_mapPath);
            settings.put("mapDir", file.getParent() + File.separator);
            settings.put("mapFile", file.getName());
        }
        updateRunButton();
    }

    /**
     *
     */
    public String getNetworkMapFile() {
        return setupFileInfo.getNetworkMapFile() ;
    }

    /**
     *
     */
    public void setPollutionFile(String _pollutionFile) {
        setupFileInfo.setPollutionFile(_pollutionFile);
        if (_pollutionFile == null || _pollutionFile.isEmpty()) {
            settings.put("obstructerDir", "");
            settings.put("obstructerFile", "");
        } else {
            File file = new File(_pollutionFile);
            settings.put("obstructerDir", file.getParent() + File.separator);
            settings.put("obstructerFile", file.getName());
        }
    }

    /**
     *
     */
    public String getPollutionFile() {
        return setupFileInfo.getPollutionFile();
    }

    /**
     *
     */
    public void setGenerationFile(String _generationFile) {
        setupFileInfo.setGenerationFile(_generationFile);
        if (_generationFile == null || _generationFile.isEmpty()) {
            settings.put("generationDir", "");
            settings.put("generationFile", "");
        } else {
            File file = new File(_generationFile);
            settings.put("generationDir", file.getParent() + File.separator);
            settings.put("generationFile", file.getName());
        }
        updateRunButton();
    }

    /**
     *
     */
    public String getGenerationFile() {
        return setupFileInfo.getGenerationFile();
    }

    /**
     *
     */
    public void setScenarioFile(String _scenarioFile) {
        setupFileInfo.setScenarioFile(_scenarioFile);
        if (_scenarioFile == null || _scenarioFile.isEmpty()) {
            settings.put("scenarioDir", "");
            settings.put("scenarioFile", "");
        } else {
            File file = new File(_scenarioFile);
            settings.put("scenarioDir", file.getParent() + File.separator);
            settings.put("scenarioFile", file.getName());
        }
        updateRunButton();
    }

    /**
     *
     */
    public String getScenarioFile() {
        return setupFileInfo.getScenarioFile() ;
    }

    /**
     *
     */
    public void setFallbackFile(String _fallbackFile,
                                ArrayList<String> _commandLineFallbacks) {
        setupFileInfo.setFallbackFile(_fallbackFile) ;
        setupFileInfo.scanFallbackFile(_commandLineFallbacks, true) ;
        if (_fallbackFile == null || _fallbackFile.isEmpty()) {
            settings.put("fallbackDir", "");
            settings.put("fallbackFile", "");
        } else {
            File file = new File(_fallbackFile);
            settings.put("fallbackDir", file.getParent() + File.separator);
            settings.put("fallbackFile", file.getName());
        }
    }

    /**
     *
     */
    public String getFallbackFile() {
        return setupFileInfo.getFallbackFile() ;
    }

    /**
     * プロパティへの橋渡し。
     */
    public CrowdWalkPropertiesHandler getProperties() {
        return properties;
    }

    //------------------------------------------------------------
    /**
     * ファイルからプロパティの読み込み。
     */
    public void setPropertiesFromFile(String _propertiesFile,
                                      ArrayList<String> _commandLineFallbacks) {
        properties = new CrowdWalkPropertiesHandler(_propertiesFile);
        propertiesFile = _propertiesFile;
        commandLineFallbacks = _commandLineFallbacks;

        // random
        random = new Random(properties.getRandseed()) ;
        // files
        setNetworkMapFile(properties.getNetworkMapFile());
        openMapWithName();
        setPollutionFile(properties.getPollutionFile());
        setGenerationFile(properties.getGenerationFile());
        setScenarioFile(properties.getScenarioFile());
        setFallbackFile(properties.getFallbackFile(),
                        commandLineFallbacks) ;
    }

    /**
     * プロパティの初期化。
     */
    public void initProperties(ArrayList<String> _commandLineFallbacks) {
        properties = new CrowdWalkPropertiesHandler();
        commandLineFallbacks = _commandLineFallbacks;

        // random
        random = new Random(properties.getRandseed()) ;
        // files
        setupFileInfo.scanFallbackFile(commandLineFallbacks, true) ;
    }

    /**
     * 画面出力用properties設定
     */
    public void setPropertiesForDisplay() {
        try {
            simulationWindowOpen = properties.getBoolean("simulation_window_open", simulationWindowOpen);
            autoSimulationStart = properties.getBoolean("auto_simulation_start", autoSimulationStart);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
