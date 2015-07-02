// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
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
import java.awt.event.WindowAdapter;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import java.util.Random;

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

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.Editor.EditorPanel3D;
import nodagumi.ananPJ.Editor.Panel.BrowserPanel;
import nodagumi.ananPJ.Editor.Panel.ScenarioPanel;

import nodagumi.ananPJ.Editor.Panel.LinkPanel;
import nodagumi.ananPJ.Editor.Panel.NodePanel;
import nodagumi.ananPJ.Editor.Panel.PollutionPanel;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.Area.MapArea;
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

import nodagumi.Itk.Itk;
import nodagumi.Itk.ItkXmlUtility;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class NetworkMapEditor extends SimulationLauncher
    implements ActionListener, WindowListener, SimulationController {

    private String dir_name = null;

    // Properties
    public static final String[] SHOW_STATUS_VALUES = {"none", "top", "bottom"};
    public static final String[] IMAGE_TYPES = {"bmp", "gif", "jpg", "png"};
    protected int deferFactor = 0;
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

    transient private MenuBar menuBar;
    private boolean modified = false;
    transient private MenuItem calcExitPathMenu;
    transient private MenuItem calcTagPathMenu;

    transient private JTabbedPane tabbedPane = null;
    transient private NodePanel nodePanel = null;
    transient public LinkPanel linkPanel = null;
    transient public PollutionPanel pollutionPanel = null;
    transient public ScenarioPanel scenarioPanel = null;
    transient public BrowserPanel browserPanel = null;


    /* used in the object browser */
    public enum TabTypes {
        NODE, LINK, POLLUTION, SCENARIO, BROWSER //FRAME
    };
    //TODO edit group and edit pollution are still unimplemented
    public enum EditorMode {
        EDIT_NODE, EDIT_LINK, EDIT_GROUP, EDIT_POLLUTION,
        PLACE_NODE, PLACE_LINK, PLACE_NODE_LINK, PLACE_GROUP,
        PLACE_POLLUTION, BROWSE
    };

    private EditorMode mode = EditorMode.EDIT_NODE;
    transient protected JFrame frame;
    private JButton runButton = null;

    protected NetworkMapEditor(Random _random) {
        super(_random);

        frame = new JFrame("Network Map Editor");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        nodePanel = new NodePanel(this);
        tabbedPane.add("Nodes", nodePanel);

        linkPanel = new LinkPanel(this);
        tabbedPane.add("Links", linkPanel);

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
        frame.setSize(800, 700);

        frame.repaint();
    }

    public JFrame getFrame() { return frame; }

    public void setModified(boolean b) {
        updateAll();
        modified = b;
    }

    public void _setModified(boolean b) {
        modified = b;
    }

    public void updateAll() {
        nodePanel.refresh();
        linkPanel.refresh();
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

    @Override
    public void setMapPath(String _mapPath) {
        super.setMapPath(_mapPath) ;
        openMapWithName();
    }

    public void switchTab(TabTypes tt) {
        switch(tt) {
        case NODE:
            tabbedPane.setSelectedComponent(nodePanel);
            break;
        case LINK:
            tabbedPane.setSelectedComponent(linkPanel);
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

    public static String getVersion() {
        Properties prop = new Properties();
        try {
            prop.load(NetworkMapEditor.class.getResourceAsStream("/netmas.properties"));
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
        if (properties != null) {
            // NetworkMap の生成時に random オブジェクトを初期化する
            // (CUIモードとGUIモードでシミュレーション結果を一致させるため)
            random.setSeed(properties.getRandseed());
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
            networkMap = readMapWithName(mapPath) ;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, e.getStackTrace(),
                    "ファイルを開けません",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (networkMap == null) return false;

        // tkokada
        setupNetworkMap() ;

        networkMap.setupAfterLoad(true);
        updateAll();
        return true;
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
        networkMap.prepareForSave(true);

        try {
            FileOutputStream fos = new FileOutputStream(mapPath);

            Document doc = ItkXmlUtility.singleton.newDocument() ;
            networkMap.toDOM(doc);
            boolean result =
                ItkXmlUtility.singleton.docToStream(doc, fos);
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

    // simulator.begin() をバックグラウンドで実行するためのモーダルダイアログ
    private class WaitDialog extends JDialog {
        public boolean canceled = false;

        public WaitDialog(Frame owner, String title, String message, final Thread thread) {
            super(owner, title, true);

            JPanel panel = new JPanel(new FlowLayout());
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancelButton.setEnabled(false);
                    canceled = true;
                    threadCancel(thread);
                }
            });
            panel.add(cancelButton);

            Container c = getContentPane();
            c.add(new JLabel(message), BorderLayout.CENTER);
            c.add(panel, BorderLayout.PAGE_END);

            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                public void windowOpened(WindowEvent e) {
                    thread.start();
                }

                public void windowClosing(WindowEvent e) {
                    if (canceled) {
                        return;
                    }
                    canceled = true;
                    cancelButton.setEnabled(false);
                    threadCancel(thread);
                }
            });

            setSize(300, 128);
            setLocationRelativeTo(owner);
        }

        public void threadCancel(Thread thread) {
            // ※ビルドの中断機能は未実装のため、現状では処理終了を待つだけ
            try {
                thread.join();
            } catch(InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private WaitDialog waitDialog;

    public boolean buildModel() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                simulator.begin() ;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // ここでダイアログを閉じる
                        waitDialog.setVisible(false);
                        waitDialog.dispose();
                    }
                });
            }
        });
        waitDialog = new WaitDialog(frame, "Information", " Simulation model building...", thread);
        waitDialog.setVisible(true);
        return ! waitDialog.canceled;
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
    public void simulationWindowOpenedOperation(SimulationPanel3D panel, final EvacuationSimulator simulator) {
        // プロパティファイルに設定された情報に従ってシミュレーションウィンドウの各コントロールの初期設定をおこなう
        if (properties == null) {
            return;
        }
        boolean successful = true;
        if (properties.isDefined("defer_factor")) {
            simulator.getAgentHandler().setSimulationDeferFactor(deferFactor);
        }
        if (cameraPath != null) {
            if (panel.loadCameraworkFromFile(cameraPath)) {
                panel.setReplay(true);
            } else {
                System.err.println("Camera file の読み込みに失敗しました: " + cameraPath);
                successful = false;
            }
        }
        if (properties.isDefined("vertical_scale")) {
            panel.setVerticalScale(verticalScale);
        }
        if (properties.isDefined("record_simulation_screen")) {
            if (recordSimulationScreen != panel.getRecordSnapshots().isSelected()) {
                panel.getRecordSnapshots().doClick();
            }
        }
        if (properties.isDefined("screenshot_dir")) {
            panel.setScreenshotDir(screenshotDir);
        }
        if (properties.isDefined("clear_screenshot_dir")) {
            if (recordSimulationScreen && clearScreenshotDir) {
                FilePathManipulation.deleteFiles(screenshotDir, imageFileFilter);
            }
        }
        if (properties.isDefined("screenshot_image_type")) {
            panel.setScreenshotImageType(screenshotImageType);
        }
        if (properties.isDefined("hide_links")) {
            if (hideLinks != panel.getHideNormalLink().isSelected()) {
                panel.getHideNormalLink().doClick();
            }
        }
        if (properties.isDefined("density_mode")) {
            if (densityMode != panel.getDensityMode().isSelected()) {
                panel.getDensityMode().doClick();
            }
        }
        if (properties.isDefined("change_agent_color_depending_on_speed")) {
            if (changeAgentColorDependingOnSpeed != panel.getChangeAgentColorDependingOnSpeed().isSelected()) {
                panel.getChangeAgentColorDependingOnSpeed().doClick();
            }
        }
        if (properties.isDefined("show_status")) {
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
        if (properties.isDefined("show_logo")) {
            if (showLogo != panel.getShowLogo().isSelected()) {
                panel.getShowLogo().doClick();
            }
        }
        if (properties.isDefined("show_3D_polygon")) {
            if (show3dPolygon != panel.getShow3dPolygon().isSelected()) {
                panel.getShow3dPolygon().doClick();
            }
        }
        if (properties.isDefined("auto_simulation_start")) {
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
								Itk.logInfo("auto simulation start");
                                simulator.getAgentHandler().getStartButton().doClick();
                            }
                        });
                    }
                });
                thread.start();
            }
        }
    }

    // SimulationPanel3D を生成した直後に呼び出される(simulationWindowOpenedOperation ではうまく対処できない分の処理)
    public void initSimulationPanel3D(SimulationPanel3D panel) {
        if (properties == null) {
            return;
        }
        if (properties.isDefined("agent_size")) {
            panel.setAgentSize(agentSize);
        }
        if (properties.isDefined("zoom")) {
            panel.setScaleOnReplay(zoom);
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

    @Override
    public void setPropertiesFromFile(String _propertiesFile) {
        super.setPropertiesFromFile(_propertiesFile) ;

        setPropertiesForDisplay() ;
    }

    //------------------------------------------------------------
    /**
     * 画面出力用properties設定
     */
    private void setPropertiesForDisplay() {
        try {
            deferFactor = properties.getInteger("defer_factor", deferFactor);
            if (deferFactor < 0 || deferFactor > 299) {
                throw new Exception("Property error - 設定値が範囲(0～299)外です: defer_factor:" + deferFactor);
            }
            verticalScale = properties.getDouble("vertical_scale", verticalScale);
            if (verticalScale < 0.1 || verticalScale > 49.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～49.9)外です: vertical_scale:" + verticalScale);
            }
            agentSize = properties.getDouble("agent_size", agentSize);
            if (agentSize < 0.1 || agentSize > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～9.9)外です: agent_size:" + agentSize);
            }
            zoom = properties.getDouble("zoom", zoom);
            if (zoom < 0.0 || zoom > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.0～9.9)外です: zoom:" + zoom);
            }
            cameraPath = properties.getFilePath("camera_file", null);
            recordSimulationScreen = properties.getBoolean("record_simulation_screen", recordSimulationScreen);
            screenshotDir = properties.getDirectoryPath("screenshot_dir", screenshotDir).replaceFirst("[/\\\\]+$", "");
            clearScreenshotDir = properties.getBoolean("clear_screenshot_dir", clearScreenshotDir);
            if (clearScreenshotDir && ! properties.isDefined("screenshot_dir")) {
                throw new Exception("Property error - clear_screenshot_dir を有効にするためには screenshot_dir の設定が必要です。");
            }
            if (recordSimulationScreen && ! clearScreenshotDir && new File(screenshotDir).list(imageFileFilter).length > 0) {
                throw new Exception("Property error - スクリーンショットディレクトリに画像ファイルが残っています: screenshot_dir:" + screenshotDir);
            }
            screenshotImageType = properties.getString("screenshot_image_type", screenshotImageType, IMAGE_TYPES);
            hideLinks = properties.getBoolean("hide_links", hideLinks);
            densityMode = properties.getBoolean("density_mode", densityMode);
            changeAgentColorDependingOnSpeed =
                properties.getBoolean("change_agent_color_depending_on_speed", changeAgentColorDependingOnSpeed);
            String show_status = properties.getString("show_status", "none", SHOW_STATUS_VALUES);
            if (show_status.equals("none")) {
                showStatus = false;
            } else {
                showStatus = true;
                showStatusPosition = show_status;
            }
            showLogo = properties.getBoolean("show_logo", showLogo);
            show3dPolygon = properties.getBoolean("show_3D_polygon", show3dPolygon);
            simulationWindowOpen = properties.getBoolean("simulation_window_open", simulationWindowOpen);
            autoSimulationStart = properties.getBoolean("auto_simulation_start", autoSimulationStart);
        } catch(Exception e) {
            //System.err.printf("Property file error: %s\n%s\n", _propertiesFile, e.getMessage());
            System.err.println(e.getMessage());
            System.exit(1);
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

    private static NetworkMapEditor instance = null;
    public static NetworkMapEditor getInstance() {
        if (instance == null) {
            instance = new NetworkMapEditor(new Random());
        }
        return instance;
    }

    public NodePanel getNodePanel() { return nodePanel; }

    public LinkPanel getLinkPanel() { return linkPanel; }

    public PollutionPanel getPollutionPanel() { return pollutionPanel; }

    public ScenarioPanel getScenarioPanel() { return scenarioPanel; }

    public BrowserPanel getBrowserPanel() { return browserPanel; }

}
