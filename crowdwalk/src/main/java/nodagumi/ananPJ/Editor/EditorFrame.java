// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Editor;

import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;   // tkokada
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import nodagumi.ananPJ.GuiSimulationEditorLauncher;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Editor.EditorFramePanel.TextPosition;
import nodagumi.ananPJ.GuiSimulationEditorLauncher.EditorMode;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkMap.OBNode; // tkokada
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.misc.GetDoublesDialog;
import nodagumi.ananPJ.misc.GridMapAreaDialog;    // tkokada
import nodagumi.ananPJ.misc.Hover;
import nodagumi.ananPJ.navigation.CalcPath.Nodes;

public class EditorFrame
    extends JFrame
    implements ActionListener,
           MouseListener, MouseWheelListener, MouseMotionListener,
           KeyListener, WindowListener, WindowFocusListener
{

    CheckboxMenuItem showNodes = null;
    CheckboxMenuItem showNodeLabels = null;
    CheckboxMenuItem showLinks = null;
    CheckboxMenuItem showLinkLabels = null;
    CheckboxMenuItem showGroups = null;
    //CheckboxMenuItem showSubGroups = null;
    CheckboxMenuItem showArea = null;
    CheckboxMenuItem showScaling = null;
    MenuItem backgroundImageScaleMenu = null;
    Menu background_group = null;
    MenuItem editNodeMode = null;
    MenuItem editLinkMode = null;
    MenuItem editAreaMode = null;
    MenuItem placeNodeMode = null;
    MenuItem placeLinkMode = null;
    MenuItem placeNodeLinkMode = null;
    MenuItem placeAgentMode = null;
    MenuItem placeAreaMode = null;
    boolean background_read = false;

    JPanel menuPanel;

    public EditorFramePanel panel = null;
    private MapPartGroup current_group = null; 

    private MenuBar menuBar = null;
    private JLabel status = null;
    private ArrayList<MapPartGroupButton> groupButtons = null;

    /* these values are used, only when the frame is not related with any OB group */

    private String label;
    /* actual length / length on display */
    public Point2D mousePoint = new Point2D.Double(0.0, 0.0);
 
    private Hover hoverNode = null;
    private MapNode selectedNode = null;
    private MapNode currentNode = null;     // マウスボタンクリック時の選択ノード(selectedNode はコロコロ変わって使いにくいので)
    private boolean draggingNode = false;
    private Hover hoverLink = null;
    private MapNode hoverLinkFromCandidate = null;
    private MapArea hoverArea = null;
    // used on placeNodeLink method
    private MapNode initialNode = null;     // first placed node
    private MapNode prevNode = null;        // to make link from

    private boolean localscrolling = false;
    private boolean localzooming = false;
    private boolean scrolling = false;
    private boolean zooming = false;
    
    private int maxAreaTag = 0;    // tkokada
 
    private int dragStartX = 0, dragStartY = 0;

    private Boolean areaSelection = false;
    private Rectangle2D selectedArea = null;

    private Point2D startpos = new Point2D.Double(0.0, 0.0);

    public GuiSimulationEditorLauncher editor = null;

    public EditorFrame(GuiSimulationEditorLauncher _editor,
                       MapPartGroup _ob_node) {
        super(_ob_node.getTagString());

        editor = _editor;
        current_group = _ob_node;

        addKeyListener(this);
        addWindowListener(this);
        addWindowFocusListener(this);

        setupMenu();
        setupContents();

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        setLocation(322, 0);
        setSize(635, 745);
    }

    private void setupMenu () {
        menuBar = new MenuBar();

        Menu fileMenu = new PopupMenu ("File");

        MenuItem mi = new MenuItem ("Set background file");
        mi.addActionListener(this);
        fileMenu.add(mi);

        mi = new MenuItem("Remove background file");
        mi.addActionListener(this);
        fileMenu.add(mi);

        fileMenu.addSeparator();

        MenuShortcut ms = new MenuShortcut (java.awt.event.KeyEvent.VK_W);
        mi = new MenuItem ("Close", ms);
        mi.addActionListener(this);
        fileMenu.add(mi);
        
        fileMenu.add(fileMenu);

        menuBar.add(fileMenu);

        Menu editMenu = new PopupMenu ("Edit");
        ms = new MenuShortcut(java.awt.event.KeyEvent.VK_D);
        mi = new MenuItem("Duplicate and move", ms);
        mi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { duplicateAndMoveNodes(); }
        });
        editMenu.add(mi);

        mi = new MenuItem("Duplicate floor");
        mi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { duplicateFloor(); }
        });
        editMenu.add(mi);

        ms = new MenuShortcut(java.awt.event.KeyEvent.VK_M);
        mi = new MenuItem("Move", ms);
        mi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { moveNodes(); }
        });
        editMenu.add(mi);

        editMenu.addSeparator();

        mi = new MenuItem("Rotate and Scale");
        mi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { rotateAndScaleNodes(); }
        });
        editMenu.add(mi);

        mi = new MenuItem("Set Scale");
        mi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { setScale(); }
        });
        editMenu.add(mi);

        backgroundImageScaleMenu = new MenuItem("Set Background Image Scale");
        backgroundImageScaleMenu.setEnabled(false);
        final EditorFrame frame = this;
        backgroundImageScaleMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BGImageScaleDialog dialog = new BGImageScaleDialog(frame, "Set Background Image Scale", true);
                dialog.setVisible(true);
            }
        });
        editMenu.add(backgroundImageScaleMenu);

        menuBar.add(editMenu);

        class ViewOrganizer implements ActionListener, ItemListener {
            EditorFrame editor_frame;
            ViewOrganizer(EditorFrame _editor) {
                editor_frame = _editor;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Set link color")) {
                    Color c = JColorChooser.showDialog(editor_frame,
                            "Color for link", Color.BLUE);
                    if (c != null) {
                        panel.setLinkColor(c);
                    }
                    update();
                } else if (e.getActionCommand().equals("Centering")) {
                    panel.centering(false);
                    panel.repaint();
                } else if (e.getActionCommand().equals("Centering with scaling")) {
                    panel.centering(true);
                    panel.repaint();
                } else if (e.getActionCommand().equals("To the origin")) {
                    panel.setPosition(0, 0);
                    panel.repaint();
                }
            }

            @Override
            public void itemStateChanged(ItemEvent e) {
                update();
            }

            public void update() {
                panel.setShowNodes(showNodes.getState());
                showNodeLabels.setEnabled(showNodes.getState());
                panel.setShowNodeNames(showNodeLabels.getState());

                panel.setShowLinks(showLinks.getState());
                showLinkLabels.setEnabled(showLinks.getState());
                panel.setShowLinkNames(showLinkLabels.getState());
                panel.setShowGroups(showGroups.getState());
                //panel.setShowSubGroups(showSubGroups.getState());
                panel.setShowArea(showArea.getState());
                panel.setShowScaling(showScaling.getState());
                editor_frame.clearSelection();
                editor_frame.repaint();
            }

        };
        ViewOrganizer vo = new ViewOrganizer(this);
        Menu viewMenu = new PopupMenu ("View");

        {
            Menu background_menu = new PopupMenu ("Background");
            MenuItem bmi = new MenuItem("100%");
            bmi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    panel.setImageStrength(1.0);
                    panel.repaint();
                }
            });
            background_menu.add(bmi);

            bmi = new MenuItem("75%");
            bmi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    panel.setImageStrength(0.75);
                    panel.repaint();
                }
            });
            background_menu.add(bmi);

            bmi = new MenuItem("50%");
            bmi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    panel.setImageStrength(0.5);
                    panel.repaint();
                }
            });
            background_menu.add(bmi);

            bmi = new MenuItem("25%");
            bmi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    panel.setImageStrength(0.25);
                    panel.repaint();
                }
            });
            background_menu.add(bmi);
            
            bmi = new MenuItem("0%");
            bmi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    panel.setImageStrength(0.0);
                    panel.repaint();
                }
            });
            background_menu.add(bmi);

            viewMenu.add(background_menu);
        }

        mi = new MenuItem("Set link color");
        mi.addActionListener(vo);
        viewMenu.add(mi);

        mi = new MenuItem("Centering");
        mi.addActionListener(vo);
        viewMenu.add(mi);

        mi = new MenuItem("Centering with scaling");
        mi.addActionListener(vo);
        viewMenu.add(mi);
        
        mi = new MenuItem("To the origin");
        mi.addActionListener(vo);
        viewMenu.add(mi);

        viewMenu.addSeparator();

        showNodes = new CheckboxMenuItem("Show nodes");
        showNodes.setState(true);
        showNodes.addItemListener(vo);
        viewMenu.add(showNodes);

        showNodeLabels = new CheckboxMenuItem(" (Show node labels)");
        showNodeLabels.setState(false);
        showNodeLabels.addItemListener(vo);
        viewMenu.add(showNodeLabels);

        viewMenu.addSeparator();

        showLinks = new CheckboxMenuItem("Show links");
        showLinks.setState(true);
        showLinks.addItemListener(vo);
        viewMenu.add(showLinks);

        showLinkLabels = new CheckboxMenuItem(" (Show link labels)");
        showLinkLabels.setState(false);
        showLinkLabels.addItemListener(vo);
        viewMenu.add(showLinkLabels);
        
        viewMenu.addSeparator();

        showGroups = new CheckboxMenuItem("Show groups");
        showGroups.setState(true);
        showGroups.addItemListener(vo);
        viewMenu.add(showGroups);

        //showSubGroups = new CheckboxMenuItem("Show subgroups");
        //showSubGroups.setState(true);
        //showSubGroups.addItemListener(vo);
        //viewMenu.add(showSubGroups);
        showArea = new CheckboxMenuItem("Show map area");
        showArea.setState(true);
        showArea.addItemListener(vo);
        viewMenu.add(showArea);

        showScaling = new CheckboxMenuItem("Show objects with scaling mode");
        showScaling.setState(false);
        showScaling.addItemListener(vo);
        viewMenu.add(showScaling);

        background_group = new PopupMenu ("Background group");
        setup_background_group_menu();
        viewMenu.add(background_group);

        menuBar.add(viewMenu);

        Menu modeMenu = new PopupMenu ("Mode");
        
        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_A);
        placeNodeMode = new MenuItem ("Place Nodes", ms);
        placeNodeMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSelection();
                editor.setMode(EditorMode.PLACE_NODE);
                setStatus();
            }
        });
        modeMenu.add(placeNodeMode);
        
        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_C);
        placeLinkMode = new MenuItem ("Place Links", ms);
        placeLinkMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.setMode(EditorMode.PLACE_LINK);
                setStatus();
            }
        });
        modeMenu.add(placeLinkMode);
        
        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_B);
        placeNodeLinkMode = new MenuItem ("Place Nodes and Links");
        placeNodeLinkMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSelection();
                editor.setMode(EditorMode.PLACE_NODE_LINK);
                setStatus();
            }
        });
        modeMenu.add(placeNodeLinkMode);
        
        placeAreaMode = new MenuItem ("Place Map Areas");
        placeAreaMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.setMode(EditorMode.PLACE_AREA);
                setStatus();
            }
        });
        modeMenu.add(placeAreaMode);

        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_E);
        editNodeMode = new MenuItem ("Edit Nodes", ms);
        editNodeMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.setMode(EditorMode.EDIT_NODE);
                setStatus();
            }
        });
        modeMenu.add(editNodeMode);

        ms = new MenuShortcut (java.awt.event.KeyEvent.VK_L);
        editLinkMode = new MenuItem ("Edit Links", ms);
        editLinkMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.setMode(EditorMode.EDIT_LINK);
                setStatus();
            }
        });
        modeMenu.add(editLinkMode);

        editAreaMode = new MenuItem ("Edit Map Area");
        editAreaMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.setMode(EditorMode.EDIT_AREA);
                setStatus();
            }
        });
        modeMenu.add(editAreaMode);

        menuBar.add(modeMenu);
        
        Menu actionMenu = new Menu("Action");
        
        mi = new MenuItem ("Calc link length by distance");
        mi.addActionListener (this);
        actionMenu.add (mi);

        mi = new MenuItem ("Make stairs");
        mi.addActionListener (this);
        actionMenu.add (mi);

        mi = new MenuItem ("Set height");
        mi.addActionListener(this);
        actionMenu.add (mi);

        menuBar.add(actionMenu);

        setMenuBar(menuBar);
    }

    class SwitchBackgroundGroup implements ActionListener {
        private MapPartGroup group;
        public SwitchBackgroundGroup(MapPartGroup _group) {
            group = _group; 
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            panel.setBackgroundGroup(group);
            panel.repaint();
        }
    }

    private void setup_background_group_menu() {
        for (MapPartGroup group : editor.getMap().getGroups()) {
            MenuItem gmi = new MenuItem(group.getTagString());
            gmi.addActionListener(new SwitchBackgroundGroup(group));
            background_group.add(gmi);
        }
        MenuItem gmi = new MenuItem("no background group");
        gmi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.setBackgroundGroup(null);
                panel.repaint();
            }
        });
        background_group.add(gmi);
    }

    class MapPartGroupButton extends JButton implements ActionListener {
        private MapPartGroup node;
        private EditorFrame frame;

        public MapPartGroupButton(EditorFrame _frame, MapPartGroup _node) {
            super(_node.getTagString());
            frame = _frame;
            node = _node;
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            for (MapPartGroupButton button : groupButtons) {
                button.setEnabled(button != this);
            }
            frame.current_group = node;
            frame.refreshBackground();
            frame.repaint();
        }
    }

    private void setupContents () {
        menuPanel = new JPanel();

        groupButtons = new ArrayList<MapPartGroupButton>();
        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group.getTags().size() == 0) continue;
            MapPartGroupButton button = new MapPartGroupButton(this, group);
            button.setEnabled(group != current_group);
            menuPanel.add(button);
            groupButtons.add(button);
        }

        add(menuPanel, BorderLayout.NORTH);

        panel = new EditorFramePanel(this, current_group);
        add(panel, BorderLayout.CENTER);

        status = new JLabel("");
        status.setBorder(BorderFactory.createLoweredBevelBorder());
        add(status, BorderLayout.SOUTH);
    }

    private boolean setBackgroundFile() {
        FileDialog fd = new FileDialog(this, "Set background image", FileDialog.LOAD);
        String dirName = null, fileName = null;
        if (current_group.getImageFileName() != null) {
            File file = new File(current_group.getImageFileName());
            dirName = file.getPath();
            fileName = file.getName();
        } else {
            dirName = editor.getDirName();
            fileName = "";
        }
        fd.setFile(fileName);
        fd.setDirectory(dirName);
        fd.setVisible (true);

        if (fd.getFile() == null) return false;

        current_group.setImageFileName(fd.getFile());
        return readBackgroundWithName();
    }

    /*
     * カレントグループの背景画像をマップデータから削除する。
     *
     * sx, sy 値は利便性のため残しておく。
     */
    private boolean removeBackgroundFile() {
        String imageFileName = current_group.getImageFileName();
        if (imageFileName == null || imageFileName.isEmpty()) {
            return false;
        }

        int res = JOptionPane.showConfirmDialog(this, "Remove background file: " + imageFileName,
                "", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.CANCEL_OPTION) {
            return false;
        }

        current_group.setImageFileName("");
        panel.addBackground(null);
        background_read = false;
        backgroundImageScaleMenu.setEnabled(false);

        repaint();
        return true;
    }

    private void refreshBackground() {
        readBackgroundWithName();
    }

    /*
     * カレントグループの背景画像を読み込んで画面に表示する。
     *
     * 画像ファイルはマップファイルと同じディレクトリに存在しなければならない。
     */
    private boolean readBackgroundWithName() {
        String imageFileName = current_group.getImageFileName();
        if (imageFileName == null || imageFileName.isEmpty()) {
            return false;
        }

        String imageFilePath = editor.getDirName() + imageFileName;
        File imageFile = new File(imageFilePath);
        if (!imageFile.exists()) {
            status.setText("Failed to load " + imageFile + ".");
            return false;
        }
        
        Image image = getToolkit().getImage(imageFilePath);
        panel.addBackground(image);
        background_read = true;
        backgroundImageScaleMenu.setEnabled(true);

        repaint();
        return true;
    }
    
    public void clearSelection() {
        //Thread.dumpStack();
        for (MapNode node : getChildNodes()) {
            node.selected = false;
        }
        for (MapLink link : getChildLinks()) {
            link.selected = false;
        }
        for (MapPartGroup group : getChildGroups()) {
            group.selected = false;
        }
        for (AgentBase agent : getChildAgents()) {
            agent.selected = false;
        }
        for (MapArea area : getChildMapAreas()) {
            area.selected = false;
        }
        hoverNode = null;
        hoverLink = null;
        hoverArea = null;
        panel.updateHoverNode(null);
        panel.updateHoverLink(null);
        panel.updateHoverArea(null);
    }

    private boolean placeHoverNode(Point2D p) {
        hoverNode = new Hover(null, p);
        return true;
    }

    private void placeNode(MouseEvent e) {
        Point2D p = panel.revCalcPos(e.getX(), e.getY());
        editor.getMap().createMapNode(current_group,
                p, getDefaultHeight());
        editor.setModified(true);
        editor.getNodePanel().refresh();
        clearSelection();
    }

    private boolean updateHoverNode(Point2D p) {
        double mindist = Double.POSITIVE_INFINITY;
        MapNode hoverNodeCandidate = null;
        for (final MapNode node : getChildNodes()) {
            double dist = p.distance(node.getLocalCoordinates());
            if (dist < mindist &&
                    dist < (10.0 / panel.getDrawingScale())) {
                hoverNodeCandidate = node;
                mindist = dist;
            }
        }

        final boolean updated = (selectedNode != hoverNodeCandidate);
        selectedNode = hoverNodeCandidate;
        if (selectedNode != null) hoverNode = new Hover(selectedNode,
                selectedNode.getLocalCoordinates());
        else hoverNode = null;

        return updated;
    }

    private void selectNode(MouseEvent e) {
        if ((e.getModifiers() & MouseEvent.CTRL_MASK) == 0) {
            clearSelection();
        }

        if (selectedNode != null) {
            selectedNode.selected = true;
        }

        editor.getNodePanel().refresh();
    }
    
    private Menu make_symlink_menu() {
        Menu addSymLinkMenu = new Menu("Add Symlink");
        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == current_group) {
                continue;
            }
            MenuItem mi = new MenuItem("Symlink to " + group.toString());
            mi.addActionListener(this);
            addSymLinkMenu.add(mi);
        }
        addSymLinkMenu.addSeparator();
        MenuItem mi = new MenuItem("Clear Symlinks");
        mi.addActionListener(this);
        addSymLinkMenu.add(mi);

        return addSymLinkMenu;
    }
    
    private void manipulateNode(MouseEvent e) {
        final int c = countSelectedNode();
        if (c == 0) return;

        PopupMenu menu = new PopupMenu ("Manipulate");
        MenuItem mi = null;
            
        mi = new MenuItem ("Align Nodes Horizontally");
        mi.addActionListener (this);
        mi.setEnabled(c >= 2);
        menu.add (mi);

        mi = new MenuItem ("Align Nodes Vertically");
        mi.addActionListener (this);
        mi.setEnabled(c >= 2);
        menu.add (mi);

        menu.addSeparator();

        mi = new MenuItem ("Remove nodes");
        mi.addActionListener (this);
        mi.setEnabled(c >= 1);
        menu.add (mi);

        menu.addSeparator();
        
        mi = new MenuItem ("Set node attribute");
        mi.addActionListener (this);
        mi.setEnabled(c >= 1);
        menu.add (mi);

        menu.addSeparator();

        mi = new MenuItem ("Change to Exit");
        mi.setEnabled(c >= 1);
        mi.addActionListener (this);
        menu.add (mi);

        menu.addSeparator();
        menu.add(make_symlink_menu());
        
        add(menu);

        menu.show (this, e.getX(), e.getY());
    }
    
    private int countSelectedNode() {
        int count = 0;
        for (final MapNode node : getChildNodes()) {
            if (node.selected) ++count;
        }
        return count;
    }

    private boolean placeHoverLink(Point2D p) {
        Hover candidate = null;

        assert (hoverLinkFromCandidate != null);
        final MapNode from = hoverLinkFromCandidate;

        MapNodeTable childNodesAndSymlinks = current_group.getChildNodesAndSymlinks(from, from.getQuadrant(p));
        MapLinkTable childLinks = getChildLinks();
        for (int tindex = 0; tindex < childNodesAndSymlinks.size(); ++tindex) {
            final MapNode to = childNodesAndSymlinks.get(tindex);
            Line2D line = new Line2D.Double (from.getLocalCoordinates(), to.getLocalCoordinates());
            if (line.ptSegDist(p) < 5.0 / panel.getDrawingScale()) {
                double lineLength = line.getP1().distance(line.getP2());
                if (candidate == null || lineLength < candidate.length) {
                    /* check for existing links */
                    boolean exists = false;
                    for (final MapLink link : childLinks) {
                        if ((link.getFrom() == from && link.getTo() == to) ||
                                (link.getTo() == from && link.getFrom() == to)) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) continue;

                    editor.linkPanel.attributePanel.setLinkLength(lineLength);
                    candidate = new Hover(from, to, editor.linkPanel.attributePanel.getLinkWidth(), lineLength);
                }
            }
        }

        if (candidate == null) {
            final Boolean updated = (hoverLink == null); 
            hoverLink = null;
            return updated;
        }

        hoverLink = candidate;

        return true;
    }

    private boolean placeHoverNodeLink(Point2D p) {
        Hover candidate = null;
        final MapNode from = prevNode;
        hoverLinkFromCandidate = prevNode;
        MapNodeTable childNodesAndSymlinks = getChildNodesAndSymlinks();
        for (int tindex = 0; tindex < childNodesAndSymlinks.size(); ++tindex) {
            final MapNode to = childNodesAndSymlinks.get(tindex);
            Line2D line = new Line2D.Double (from.getLocalCoordinates(),
                    to.getLocalCoordinates());
            if (line.ptSegDist(p) < 5.0 / panel.getDrawingScale()) {
                /* check for existing links */
                boolean exists = false;
                for (final MapLink link : getChildLinks()) {
                    if ((link.getFrom() == from && link.getTo() == to) ||
                            (link.getTo() == from && link.getFrom() == to)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) continue;

                double lineLength = line.getP1().distance(line.getP2());
                if (candidate == null ||
                        lineLength < candidate.length) {
                    editor.linkPanel.attributePanel.setLinkLength(lineLength);
                    candidate = new Hover(from,
                            to,
                            editor.linkPanel.attributePanel.getLinkWidth(),
                            lineLength);
                }
            }
        }

        if (candidate != null) {
            hoverLink = candidate;
            return true;
        }

        final MapNode tmpNode = new MapNode(null, p, getDefaultHeight());
        Line2D line = new Line2D.Double (from.getLocalCoordinates(),
                tmpNode.getLocalCoordinates());
        double lineLength = line.getP1().distance(line.getP2());

        editor.linkPanel.attributePanel.setLinkLength(lineLength);
        candidate = new Hover(from, tmpNode,
                editor.linkPanel.attributePanel.getLinkWidth(), lineLength);
        if (candidate == null) {
            final Boolean updated = (hoverLink == null);
            hoverLink = null;
            return updated;
        }
        hoverLink = candidate;

        return true;
    }

    private void placeLink(MouseEvent e) {
        // from node クリック時
        if (hoverLinkFromCandidate == null) {
            hoverLinkFromCandidate = selectedNode;
        // to node を確定してリンク生成 or キャンセル
        } else {
            hoverLinkFromCandidate = null;
            if (hoverLink == null) {
                return;
            }
            for (final MapLink link : getChildLinks()) {
                if (link.equals(hoverLink)) return;
            }

            MapLink link = editor.getMap().createMapLink(current_group,
                    hoverLink.from, hoverLink.to,
                    hoverLink.length, hoverLink.width);
            
            clearSelection();
            link.selected =true;
            editor.setModified(true);
            editor.getLinkPanel().refresh();
        }
    }

    private boolean updateHoverLink(Point2D p) {
        Hover hoverLinkCandidate = null;
        double mindist = Double.MAX_VALUE;
        for (final MapLink link : getChildLinks()) {
            MapNode from = (MapNode)link.getFrom();
            MapNode to = (MapNode)link.getTo();
            Line2D line = new Line2D.Double(from.getLocalCoordinates(), to.getLocalCoordinates());
            double dist = line.ptSegDist(p);
            if (dist  < mindist) {
                hoverLinkCandidate = new Hover(from,
                        to,
                        editor.linkPanel.attributePanel.getLinkLength(),
                        editor.linkPanel.attributePanel.getLinkWidth());
                hoverLinkCandidate.setDummyHoverLink(link);
                hoverLinkCandidate.orig_link = link;
                mindist = dist;
            }
        }

        final boolean updated = (hoverLink != hoverLinkCandidate);
        hoverLink = hoverLinkCandidate;
        return updated;
    }

    private void selectLink(MouseEvent e) {
        if (hoverLink == null ||
                hoverLink.getDummyHoverLink() == null) {
            return;
        }
        Hover hover_link_backup = hoverLink;
        if ((e.getModifiers() & MouseEvent.CTRL_MASK) == 0) {
            clearSelection();
        }
        hover_link_backup.getDummyHoverLink().selected ^= true;
        editor.getLinkPanel().refresh();
    }
/*
    public void repaint() {
        super.repaint();
        panel.repaint(100);
    }
*/
    private void manipulateLink(MouseEvent e) {
        class ManipulateLinkListener implements ActionListener {
            EditorFrame editor = null;
            ManipulateLinkListener(EditorFrame _editor) {
                editor = _editor;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Set link attribute")) {
                    editor.setLinkAttribute();
                } else if (e.getActionCommand().equals("Remove links")) {
                    editor.removeSelectedLinks();
                } else if (e.getActionCommand().equals("Set One-way")) {
                    editor.setOneWayLinks();
                } else if (e.getActionCommand().equals(
                            "Set Road Closed")) {
                    editor.setRoadClosedLinks();
                } else if (e.getActionCommand().equals(
                            "Remove One-way / Road Closed")) {
                    editor.removeSelectedOneWayTag();
                }
            }
        };
        ManipulateLinkListener listner = new ManipulateLinkListener(this);
        PopupMenu menu = new PopupMenu ("Manipulate");
        MenuItem mi = null;
        final int c = countSelectedLinks();

        mi = new MenuItem ("Set link attribute");
        mi.addActionListener (listner);
        mi.setEnabled(c >= 1);
        menu.add (mi);

        menu.addSeparator();
        mi = new MenuItem ("Remove links");
        mi.addActionListener (listner);
        mi.setEnabled(c >= 1);
        menu.add (mi);

        menu.addSeparator();
        mi = new MenuItem("Set One-way");
        mi.addActionListener (listner);
        mi.setEnabled(c >= 1);
        menu.add(mi);

        mi = new MenuItem("Set Road Closed");
        mi.addActionListener (listner);
        mi.setEnabled(c >= 1);
        menu.add(mi);

        mi = new MenuItem("Remove One-way / Road Closed");
        mi.addActionListener (listner);
        mi.setEnabled(c >= 1);
        menu.add(mi);

        menu.addSeparator();
        menu.add(make_symlink_menu());

        add (menu);
        menu.show (this, e.getX(), e.getY());
    }

    private int countSelectedLinks() {
        return getSelectedLinks().size();
    }

    // Place Nodes and LInks mode
    //  see README 4.2. to know detail
    private void placeNodeLink(MouseEvent e) {
        // 右クリックは次点ノード生成待ち状態の解除 or 何もしない
        if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
            initialNode = null;
            prevNode = null;
            clearSelection();
            panel.repaint();
        } else if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
            // 左ダブルクリックで始点ノードを生成 or 次点ノード生成待ち状態の解除
            if (e.getClickCount() == 2) {
                if (initialNode == null) {  // place initian node
                    Point2D p = panel.revCalcPos(e.getX(), e.getY());
                    initialNode = editor.getMap().createMapNode(current_group,
                            p, getDefaultHeight());
                    prevNode = initialNode;
                    editor.setModified(true);
                    editor.getNodePanel().refresh();
                    clearSelection();
                    panel.repaint();
                } else {                    // place last node and exit mode
                    initialNode = null;
                    prevNode = null;
                }
            // 左シングルクリック(ダブルクリックの1クリック目含む)で次点(終点)ノードとリンクを生成
            } else if (e.getClickCount() == 1) {
                if (initialNode != null) { // place node and link from prevNode
                    if (hoverLinkFromCandidate == null) {
                        if (prevNode != null)
                            hoverLinkFromCandidate = prevNode;
                        else
                            hoverLinkFromCandidate = selectedNode;
                    } else {
                        hoverLinkFromCandidate = null;
                        if (hoverLink == null) {
                            panel.repaint();
                            return;
                        }
                        boolean nodeExist = false;
                        MapNode node = null;
                        for (final MapNode _node : getChildNodes()) {
                            if (_node.equals(hoverLink.to)) {
                                nodeExist = true;
                                node = _node;
                                break;
                            }
                        }
                        boolean linkExist = false;
                        MapLink link = null;
                        for (final MapLink _link : getChildLinks()) {
                            if (hoverLink.from == _link.getFrom() &&
                                    hoverLink.to == _link.getTo()) {
                                linkExist = true;
                                link = _link;
                                link.selected = true;
                                break;
                            }
                        }
                        if (!nodeExist)
                            node = editor.getMap().createMapNode(current_group,
                                    hoverLink.to.getAbsoluteCoordinates()
                                    , getDefaultHeight());
                        if (!linkExist) {
                            link = editor.getMap().createMapLink(
                                    current_group, hoverLink.from,
                                    node, hoverLink.length,
                                    hoverLink.width);
                            link.selected = true;
                        }
                        prevNode = node;
                        clearSelection();
                    }
                    editor.setModified(true);
                    editor.getNodePanel().refresh();
                    editor.getLinkPanel().refresh();
                    panel.repaint();
                }
            }
        }
    }

    private boolean updateHoverArea(Point2D p) {
        /* Find an existing agent */
        for (MapArea area : getChildMapAreas()) {
            if (area.contains(p)) {
                final boolean updated = (!area.equals(hoverArea));
                hoverArea = area;
                return updated;
            }
        }
        final boolean updated = (hoverArea != null);
        hoverArea = null;
        return updated;
    }

    private void selectArea(MouseEvent e) {
        MapArea a = hoverArea;
        if ((e.getModifiers() & MouseEvent.CTRL_MASK) == 0) {
            clearSelection();
        }
        if (a == null) {
            return;
        }
        a.selected ^= true;
        editor.getAreaPanel().refresh();
    }
 
    // reviced by tkokada
    private void placeMapArea () {
        String labels[] = {"min height", "max height", "vertical division", "horizontal division",
                "rotation"};
        double range[][] = {
                {-1000.0, 1000.0, getMinHeight()},
                {-1000.0, 1000.0, getMaxHeight()},
                {0.0, 360.0, getRotation()}};
        int division[][] = {
                {1, 1000, getVerticalDivision()},
                {1, 1000, getHorizontalDivision()}};

        double height[] = GridMapAreaDialog.showDialog("Height", labels,
                range[0], range[1], division[0], division[1], range[2]);
        
        // tkokada height[2]: vertical_division, height[3]: horizontal_division, height[4]: angle
        for (int i = 0; i < (int) height[2]; i++) {
            for (int j = 0; j < (int) height[3]; j++) {
                double ocx = selectedArea.getX() + selectedArea.getWidth() / height[3] * (j + 0.5);
                double ocy = selectedArea.getY() + selectedArea.getHeight() / height[2] * (i + 0.5);
                //System.out.println("ocx:" + ocx + ", ocy:" + ocy);
                Point2D centroid = rotatePoint2D(new Point2D.Double(ocx, ocy), new Point2D.Double(selectedArea.getCenterX(), selectedArea.getCenterY()), height[4]);
                //System.out.println("centroid x:" + centroid.getX() + ", centroid y:" + centroid.getY());
                Rectangle2D bounds = new Rectangle2D.Double(centroid.getX() - selectedArea.getWidth() / height[3] / 2,
                        centroid.getY() - selectedArea.getHeight() / height[2] / 2,
                        selectedArea.getWidth() / height[3],
                        selectedArea.getHeight() / height[2]);
                editor.getMap().createMapAreaRectangle(current_group,
                    bounds, height[0], height[1], height[4]);
            //System.out.println("created bounds cx:" + bounds.getCenterX() + ", cy:" + bounds.getCenterY());
            }
        }
        updateMapAreaTag();
        editor.setModified(true);
        editor.getAreaPanel().refresh();
        clearSelection();
        panel.repaint();
    }
    
    private Point2D rotatePoint2D(Point2D point, Point2D origin, double _angle) {
        double angle = _angle * Math.PI / 180;
        Point2D rotatedPoint = new Point2D.Double(
                origin.getX() + (point.getX() - origin.getX()) * Math.cos(angle) - (point.getY() - origin.getY()) * Math.sin(angle),
                origin.getY() + (point.getX() - origin.getX()) * Math.sin(angle) + (point.getY() - origin.getY()) * Math.cos(angle));
        //System.out.println("rotatePoint2D point(" + point.getX() + "," + point.getY() + "), to point(" + rotatedPoint.getX() + "," + rotatedPoint.getY() + "), with angle:" + angle);
        return rotatedPoint;
    }
    
    private Rectangle2D rotateRectangle2D(Rectangle2D rect, Point2D origin, double angle) {
        Point2D rotatedCenter = rotatePoint2D(new Point2D.Double(rect.getCenterX(), rect.getCenterY()), origin, angle);
        Rectangle2D rotatedRect = new Rectangle2D.Double(
                rotatedCenter.getX() - rect.getWidth() / 2,
                rotatedCenter.getY() - rect.getHeight() /2,
                rect.getWidth(),
                rect.getHeight());
        return rotatedRect;
    }
    
    private void manipulateArea(MouseEvent e) {
        class ManipulateAreaListener implements ActionListener {
            int x,y;
            EditorFrame editor = null;
            ManipulateAreaListener(EditorFrame _editor, int _x, int _y) {
                editor = _editor;
                x = _x;
                y = _y;
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Set area attribute")) {
                    editor.setAreaAttribute(x, y);
                } else if (e.getActionCommand().equals("Remove area")) {
                    editor.removeSelectedArea();
                }
            }
        };

        if (hoverArea != null) hoverArea.selected = true;
        ManipulateAreaListener listner = new ManipulateAreaListener(this,
                e.getXOnScreen(), e.getYOnScreen());
        PopupMenu menu = new PopupMenu ("Manipulate");
        MenuItem mi = null;
            
        mi = new MenuItem ("Remove area");
        mi.addActionListener (listner);
        menu.add (mi);

        menu.addSeparator();
        
        mi = new MenuItem ("Set area attribute");
        mi.addActionListener (listner);
        menu.add (mi);

        add (menu);
        menu.show(this, e.getX(), e.getY());
    }
    
    public Rectangle2D getRegion() {
        return current_group.getRegion();
    }

    // tkokada
    // this method should be called when map area tag is added/removed/modified.
    // currently, map area has initial index as a tag with int number.
    // if you want to modify this tag, following "" + i, this becomes the tag.
    public void updateMapAreaTag() {
        int i = 1;
        ArrayList<String> tagToRemove = new ArrayList<String>();
        for (i = 1; i < maxAreaTag; i++) {
            tagToRemove.add("" + i);
        }
        i = 1;
        for (MapArea area : getChildMapAreas()) {
            for (String tag : tagToRemove)
                area.removeTag(tag);
            area.addTag("" + i);
            i++;
        }
        maxAreaTag = i;
    }

    private void calcLinkLength() {
        String numStr = "";
        try {
            numStr = JOptionPane.showInputDialog("scale", "" + current_group.getScale());
            if (numStr == null) return;
            current_group.setScale(Double.parseDouble(numStr));
        } catch (NumberFormatException e){
            JOptionPane.showMessageDialog(this,
                    "Could not parse:\n" + numStr,
                    "Number format error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (MapLink link : getChildLinks()) {
            if (link.width == 0) continue;
            MapNode from = (MapNode)link.getFrom();
            MapNode to = (MapNode)link.getTo();
            link.length = from.getLocalCoordinates().distance(to.getLocalCoordinates()) * current_group.getScale();
        }
    }

    private void setNodeAttribute() {
        MapNode.showAttributeDialog(getChildNodes());
        clearSelection();
        repaint();
    }
    
    private void changeToExit() {
        for (MapNode node : getChildNodes()) {
            if (node.selected) {
                node.addTag("EXIT");
            }
        }
    }
    
    private void setLinkAttribute() {
        MapLink.showAttributeDialog(getChildLinks());
        clearSelection();
        repaint();
    }
    
    /**
     * 末端に当たるノードかどうかを返す。
     * getSeriesLinks() 用。
     */
    public boolean isTerminalNode(MapNode node, MapLink link, HashSet<MapLink> candidateLinkSet) {
        for (MapLink _link : node.getLinks()) {
            if (_link != link && candidateLinkSet.contains(_link)) {
                return false;
            }
        }
        // link (自分)以外に candidateLinks に含まれるリンクが node に接続されていなければ末端とする
        return true;
    }

    /**
     * candidateLinks をリンクが連なる順に並べ替えたリストを両端のノードと共に返す。
     * (candidateLinks に含まれないリンクは接続されていないものと見なす)
     * 条件:
     * ・閉じたリンクを含んでいないこと。
     * ・リンクが分岐している箇所がないこと。
     * ・すべてのリンクが途切れなく接続されていること。
     */
    public HashMap getSeriesLinks(MapLinkTable candidateLinks) throws Exception {
        MapLinkTable seriesLinks = new MapLinkTable();
        HashSet<MapLink> candidateLinkSet = new HashSet<MapLink>(candidateLinks);
        HashMap result = new HashMap();
        result.put("linkTable", seriesLinks);

        // 端のリンクを見つける
        MapLink terminalLink = null;
        MapNode terminalNode = null;
        for (MapLink link : candidateLinks) {
            if (isTerminalNode(link.getFrom(), link, candidateLinkSet)) {
                terminalNode = link.getFrom();
                terminalLink = link;
                break;
            }
            if (isTerminalNode(link.getTo(), link, candidateLinkSet)) {
                terminalNode = link.getTo();
                terminalLink = link;
                break;
            }
        }
        // 見つからなかった(閉じたリンクのみだった)
        if (terminalLink == null) {
            throw new Exception("選択したリンクはループしているため、一方通行の設定ができません。");
        }
        result.put("firstNode", terminalNode);

        // リンクをたどりながら、もう一方の端を探す
        MapLink link = terminalLink;
        MapNode node = link.getOther(terminalNode);
        while (true) {
            seriesLinks.add(link);
            MapLink nextLink = null;
            for (MapLink _link : node.getLinks()) {
                if (_link != link && candidateLinkSet.contains(_link)) {
                    if (nextLink != null) {
                        throw new Exception("選択したリンクは分岐を含んでいるため、一方通行の設定ができません。");
                    }
                    nextLink = _link;
                }
            }
            if (nextLink == null) {
                if (seriesLinks.size() < candidateLinks.size()) {
                    throw new Exception("選択したリンクは接続されていないリンクを含んでいるため、\n一方通行の設定ができません。");
                }
                result.put("lastNode", node);
                return result;
            }
            link = nextLink;
            node = link.getOther(node);
        }
    }

    /**
     * node から見て link の反対側に当たる位置を返す。
     */
    public TextPosition getClearPosition(MapLink link, MapNode node) {
        if (Math.abs(node.getX() - link.getOther(node).getX())
                < Math.abs(node.getY() - link.getOther(node).getY())) {
            if (node.getY() < link.getOther(node).getY()) {
                return TextPosition.UPPER;
            } else {
                return TextPosition.LOWER;
            }
        } else {
            if (node.getX() < link.getOther(node).getX()) {
                return TextPosition.LEFT;
            } else {
                return TextPosition.RIGHT;
            }
        }
    }

    /**
     * 選択中のリンクを一方通行に設定する。
     */
    private void setOneWayLinks() {
        panel.updateHoverLink(null);
        editor.setModified(true);

        MapLinkTable seriesLinks = null;
        MapNode firstNode = null;
        MapNode lastNode = null;
        MapNode enteringNode = null;

        // 選択中のリンクを連続したリンクに変換する
        try {
            HashMap result = getSeriesLinks(getSelectedLinks());
            seriesLinks = (MapLinkTable)result.get("linkTable");
            firstNode = (MapNode)result.get("firstNode");
            lastNode = (MapNode)result.get("lastNode");
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Warning", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 両端のノードに "A", "B" ラベルを表示する
        TextPosition positionA = getClearPosition(seriesLinks.get(0), firstNode);
        TextPosition positionB = getClearPosition(seriesLinks.get(seriesLinks.size() - 1), lastNode);
        panel.setOneWayIndicator(true, firstNode, positionA, lastNode, positionB);
        panel.repaint();

        // 方向を選択する
        int type = JOptionPane.showConfirmDialog(this, "One-way from A to B(Y) or B to A(N) ?", "",
                JOptionPane.YES_NO_CANCEL_OPTION);
        switch (type) {
        case JOptionPane.YES_OPTION:
            enteringNode = firstNode;
            break;
        case JOptionPane.NO_OPTION:
            Collections.reverse(seriesLinks);
            enteringNode = lastNode;
            break;
        case JOptionPane.CANCEL_OPTION:
            panel.setOneWayIndicator(false, null, TextPosition.CENTER, null, TextPosition.CENTER);
            panel.repaint();
            return;
        }

        // リンクに一方通行タグを振る
        for (MapLink link : seriesLinks) {
            if (link.isForwardDirectionFrom(enteringNode)) {
                link.setOneWayForward() ;
                link.setOneWayBackward(false) ;
                link.setRoadClosed(false) ;
            } else {
                link.setOneWayBackward() ;
                link.setOneWayForward(false) ;
                link.setRoadClosed(false) ;
            }
            enteringNode = link.getOther(enteringNode);
        }

        panel.setOneWayIndicator(false, null, TextPosition.CENTER, null, TextPosition.CENTER);
        editor.getLinkPanel().refresh();
        clearSelection();
        repaint();
    }

    private void setRoadClosedLinks() {
        panel.updateHoverLink(null);
        editor.setModified(true);
        for (MapLink link : getChildLinks()) {
            if (link.selected) {
                link.setRoadClosed() ;
                link.setOneWayForward(false) ;
                link.setOneWayBackward(false) ;
            }
        }
        editor.getLinkPanel().refresh();
        clearSelection();
        repaint();
    }

    private void setAreaAttribute(int x, int y) {
        MapArea.showAttributeDialog(getChildMapAreas(), x, y);
        updateMapAreaTag();
        clearSelection();
        repaint();
    }

    private void removeSelectedNodes() {
        panel.updateHoverNode(null);
        editor.setModified(true);
        MapNodeTable nodesToRemove = new MapNodeTable();
        for (final MapNode node : getChildNodes()) {
            if (node.selected) nodesToRemove.add(node);
        }
        MapLinkTable linksToRemove = new MapLinkTable();
        for (final MapLink link : getChildLinks()) {
            if (nodesToRemove.contains(link.getFrom()) ||
                    nodesToRemove.contains(link.getTo())) {
                linksToRemove.add(link);
            }
        }
        for (MapNode node : nodesToRemove) {
            editor.getMap().removeOBNode(current_group, node, true);
        }
        for (MapLink link : linksToRemove) {
            editor.getMap().removeOBNode(current_group, link, true);
        }
        
        editor.getLinkPanel().refresh();
        repaint();
    }
    
    private void removeSelectedLinks() {
        panel.updateHoverLink(null);
        editor.setModified(true);
        MapLinkTable linksToRemove = new MapLinkTable();
        for (final MapLink link : getChildLinks()) {
            if (link.selected) {
                linksToRemove.add(link);
                link.prepareRemove();
            }
        }
        for (MapLink link : linksToRemove) {
            editor.getMap().removeOBNode(current_group, link, true);
        }
        editor.getLinkPanel().refresh();
        clearSelection();
        repaint();
    }
    
    private void removeSelectedOneWayTag() {
        panel.updateHoverLink(null);
        editor.setModified(true);
        for (MapLink link : getChildLinks()) {
                if (link.selected) {
                    link.setOneWayForward(false) ;
                    link.setOneWayBackward(false) ;
                    link.setRoadClosed(false) ;
                }
        }
        editor.getLinkPanel().refresh();
        repaint();
    }

    private void removeSelectedArea() {
        editor.setModified(true);
        ArrayList<MapArea> areasToRemove = new ArrayList<MapArea>();
        for (final MapArea area : getChildMapAreas()) {
            if (area.selected) {
                areasToRemove.add(area);
            }
        }
        for (MapArea area : areasToRemove) {
            editor.getMap().removeOBNode(current_group, area, true);
        }
        updateMapAreaTag();
        editor.getAreaPanel().refresh();
        repaint();
    }

    private void addSymlinks(MapPartGroup group) {
        for (final MapNode node : getChildNodes()) {
            if (node.selected) { 
                editor.getMap().createSymLink(group, node);
            }
        }
        for (final MapLink link : getChildLinks()) {
            if (link.selected) {
                editor.getMap().createSymLink(group, link);
            }
        }
        repaint();
        clearSelection();
    }
    
    private void clearSymlinks() {
        for (final MapNode node : getChildNodes()) {
            if (node.selected) { 
                editor.getMap().clearSymlinks(node);
            }
        }
        for (final MapLink link : getChildLinks()) {
            if (link.selected) {
                editor.getMap().clearSymlinks(link);
            }
        }

    }
    
    /* Group of methods to copy nodes to other groups
     */
    class DuplicateAndMoveNodesDialog
    extends JDialog
    implements ActionListener {

        private JSpinner[] values;
        private JComboBox cb;
        
        private MapPartGroup to_group = null;
        private double[] distance = new double[3];

        final String[] labels = {"X", "Y", "Z"}; 
        public DuplicateAndMoveNodesDialog(String title) {
            this.setModal(true);
            Container panel = getContentPane();
            panel.setLayout(new FlowLayout());
                
            values = new JSpinner[3];
            for (int i = 0; i < 3; ++i) {
                panel.add(new JLabel(labels[i]));
            }
            for (int i = 0; i < 3; ++i) {
                values[i] = new JSpinner(new SpinnerNumberModel(
                        0.0, -1000.0, 1000.0, 0.1));
                panel.add(values[i]);
            }
            panel.add(new JLabel());

            /* which group to duplicate/move */
            cb = new JComboBox();
            cb.addItem("(new group)");

            for (MapPartGroup group : editor.getMap().getGroups()){
                cb.addItem(group);
            }

            cb.setSelectedItem("(new group)");
            panel.add(cb);

            /* OK/cancel */
            JButton ok = new JButton("OK");
            ok.addActionListener(this);
            panel.add(ok);

            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(this);
            panel.add(cancel);

            this.pack();
        }
        
        public MapPartGroup getToGroup() {
            return to_group;
        }
        
        public double[] getDistance() {
            return distance;
        }
    
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "OK") {
                for (int i = 0; i < 3; ++i) {
                    distance[i] = (Double)(this.values[i].getValue());
                }
                if (cb.getSelectedItem() instanceof MapPartGroup) {
                    to_group = (MapPartGroup)cb.getSelectedItem();
                } else {
                    String new_group_name = JOptionPane.showInputDialog(this,
                            "The name for the new group:", "Enter name",
                            JOptionPane.QUESTION_MESSAGE);
                    if (!new_group_name.equals("")) {
                        to_group = editor.getMap().createGroupNode((MapPartGroup)current_group.getParent());
                        to_group.addTag(new_group_name);
                    } else {
                        to_group = null;
                    }
                }
                dispose();
            } else if (e.getActionCommand() == "Cancel") {
                dispose();
            }
        }
    }

    private void duplicateAndMoveNodes() {
        if (countSelectedNode() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No nodes selected!",
                    "Do nothing", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        DuplicateAndMoveNodesDialog dig =
            new DuplicateAndMoveNodesDialog("Copy and Move");
        dig.setVisible(true);
        
        MapPartGroup to_group = dig.getToGroup();
        if (to_group == null) return;

        /* copy some properties of this node */
        final double dx = dig.getDistance()[0];
        final double dy = dig.getDistance()[1];
        final double dz = dig.getDistance()[2];
        to_group.setDefaultHeight(current_group.getDefaultHeight() + dz);
        to_group.setScale(current_group.getScale());
        to_group.setMinHeight(current_group.getMinHeight() + dz);
        to_group.setMaxHeight(current_group.getMaxHeight() + dz);
        to_group.setWest(current_group.getWest() + dx);
        to_group.setSouth(current_group.getSouth() + dy);

        duplicateNodesWithChangeGroup(to_group);
        moveNodes(to_group, dx, dy, dz);
    }

    class StairSet {
        String name;
        MapNode nodes[] = new MapNode[2];
        public StairSet(String _name) {
            name = _name;
        }
        
        public String addNode(MapNode node,
                String direction) {
            if (direction.equals("UP")) {
                if (nodes[1] != null) {
                    node.selected = true;
                    nodes[1].selected = true;
                    return "Multiple up-stairs for " + name;
                } else {
                    nodes[1] = node;
                    return null;
                }
            } else if (direction.equals("DOWN")) {
                if (nodes[0] != null) {
                    node.selected = true;
                    nodes[0].selected = true;
                    return "Multiple down-stairs for " + name;
                } else {
                    nodes[0] = node;
                    return null;
                }
            } else {
                return "direction " + direction + "not known.";
            }
        }
        
        public String isSatisfied() {
            if (nodes[0] == null) {
                nodes[1].selected = true;
                return "Down-stairs missing for " + name;
            } else if (nodes[1] == null) {
                nodes[0].selected = true;
                return "Up-stairs missing for " + name;
            }
            return null;
        }
        
        public double heightDiff() {
            return nodes[1].getHeight() - nodes[0].getHeight();
        }
    }
    
    class AllStairSets extends HashMap<String, StairSet> {
        public double height_diff = 0.0;
    }
    
    private AllStairSets get_stairset(MapPartGroup group,
            String floor_string) {
        AllStairSets stairs = new AllStairSets();

        boolean has_error = false;
        String error_message = "";

        /* find stair nodes in group */
        for (MapNode node : group.getChildNodes()) {
            Matcher match = node.matchTag("STAIR_(UP|DOWN)_(.+)_" + floor_string);
            if (match != null) {
                final String direction = match.group(1);
                final String stair_name = match.group(2);
                
                if (!stairs.containsKey(stair_name)) {
                    stairs.put(stair_name, new StairSet(stair_name));
                }
                StairSet stair_set = stairs.get(stair_name);
                String error_str = stair_set.addNode(node, direction);
                if (error_str != null) {
                    has_error = true;
                    error_message += error_str + "\n";
                }
            }
        }
        /* calculate height and check for missing pairs */
        double max_height_diff = 0.0;
        for (StairSet stair_set : stairs.values()) {
            String error_str = stair_set.isSatisfied();     
            if (error_str != null) {
                has_error = true;
                error_message += error_str + "\n";
            }
            if (stair_set.heightDiff() > max_height_diff) {
                max_height_diff = stair_set.heightDiff();
            }
        }
        stairs.height_diff = max_height_diff;

        if (has_error) {
            JOptionPane.showMessageDialog(this,
                    error_message,
                    "Problems in nodes",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return stairs;
    }

    private void duplicateFloor() {
        class DuplicateFloorCount
        extends JDialog implements ActionListener {
            /**
             * 
             */
            public JSpinner[] values;
            public DuplicateFloorCount() {
                this.setModal(true);
                Container panel = getContentPane();
                panel.setLayout(new FlowLayout());
                    
                values = new JSpinner[3];
                panel.add(new JLabel("UP"));
                values[0] = new JSpinner(new SpinnerNumberModel(
                        0, 0, 100, 1));
                panel.add(values[0]);
                panel.add(new JLabel("DOWN"));
                values[1] = new JSpinner(new SpinnerNumberModel(
                        0, 0, 100, 1));
                panel.add(values[1]);
                panel.add(new JLabel("Stair width"));
                values[2] = new JSpinner(new SpinnerNumberModel(
                        2.0, 0, 100, 0.1));
                panel.add(values[2]);
                panel.add(new JLabel());

                /* OK/cancel */
                JButton ok = new JButton("OK");
                ok.addActionListener(this);
                panel.add(ok);

                JButton cancel = new JButton("Cancel");
                cancel.addActionListener(this);
                panel.add(cancel);

                this.pack();
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == "OK") {
                    dispose();
                } else if (e.getActionCommand() == "Cancel") {
                    values = null;
                    dispose();
                }
            }
        }
        DuplicateFloorCount dfc = new DuplicateFloorCount();
        dfc.setVisible(true);
        
        if (dfc.values == null) return;
        int count = 0;

        MapPartGroup group = current_group;
        for (int i = 0; i < (Integer)(dfc.values[0].getValue()); ++i) {
            group = duplicateFloor(group, 1, (Double)dfc.values[2].getValue());
            if (group == null) return;
            ++count; 
        }
        group = current_group;
        for (int i = 0; i < (Integer)(dfc.values[1].getValue()); ++i) {
            group = duplicateFloor(group, -1, (Double)dfc.values[2].getValue());
            if (group == null) return;
            ++count;
        }
        JOptionPane.showMessageDialog(this,
                "Duplicated " + count + " floors",
                "Success!", JOptionPane.PLAIN_MESSAGE);
    }

    private MapPartGroup duplicateFloor(MapPartGroup group,
            int direction,
            double stair_width) {
        /* see which floor we are in */
        Matcher match = group.matchTag("(B?)(\\d+)F");
        if (match == null) {
            JOptionPane.showMessageDialog(this,
                    "No floor number given for this group",
                    "Do nothing", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String floor_string = match.group(0);
        int floor = Integer.parseInt(match.group(2));
        if (!match.group(1).isEmpty()) { floor = -floor;}
        
        /* get stairs */
        AllStairSets stairs = get_stairset(group, floor_string);
        if (stairs == null) return null;

        /* next floor */
        int next_floor = floor + direction;
        if (next_floor == 0) {
            if (direction > 0) {
                next_floor = 1;
            } else {
                next_floor = -1;
            }
        }
        String next_floor_string =
            ((next_floor < 0) ? "B" : "") + (int)Math.abs(next_floor) + "F";
        
        double dz = stairs.height_diff * direction;
        MapPartGroup next_group = editor.getMap().createGroupNode((MapPartGroup)group.getParent());
        next_group.addTag(next_floor_string);
        next_group.setDefaultHeight(group.getDefaultHeight() + dz);
        next_group.setScale(group.getScale());
        next_group.setMinHeight(group.getMinHeight() + dz);
        next_group.setMaxHeight(group.getMaxHeight() + dz);
        next_group.setWest(group.getWest());
        next_group.setSouth(group.getSouth());
        
        /* copy nodes */
        MapNodeTable nodesToMove = new MapNodeTable();
        HashMap<MapNode, MapNode> nodeToNode = new HashMap<MapNode, MapNode>();
        for (MapNode node : group.getChildNodes()) {
            double height_diff = node.getHeight() - group.getDefaultHeight();
            MapNode newNode = editor.getMap().createMapNode(next_group, 
                    node.getAbsoluteCoordinates(),
                    next_group.getDefaultHeight() + height_diff);
            for (String tag : node.getTags()) {
                tag = tag.replaceFirst(floor_string, next_floor_string);
                newNode.addTag(tag);
            }
            nodesToMove.add(newNode);
            nodeToNode.put(node, newNode);
        }
        
        /* copy links */
        for (MapLink link : group.getChildLinks()) {
            if (group.getChildNodes().contains(link.getFrom()) &&
                    group.getChildNodes().contains(link.getTo())) {
                MapNode fromNode = nodeToNode.get(link.getFrom());
                MapNode toNode = nodeToNode.get(link.getTo());
                MapLink newLink = editor.getMap().createMapLink(next_group,
                        fromNode, toNode,
                        link.length, link.width);
                for (String tag : link.getTags()) {
                    tag = tag.replaceFirst(floor_string, next_floor_string);
                    newLink.addTag(tag);
                }
            }
        }
        
        /* make stairs */
        int orig_index = 1; /* make a symlink of UP to next group */
        int next_index = 0; /* draw a link from the symlink to DOWN of next group */
        if (direction < 0) {
            orig_index = 0;
            next_index = 1;
        }

        for (StairSet stair_set : stairs.values()) {
            MapNode fromNode = stair_set.nodes[orig_index];
            editor.getMap().createSymLink(next_group, fromNode);
            MapNode toNode = nodeToNode.get(stair_set.nodes[next_index]);
            MapLink newLink = editor.getMap().createMapLink(next_group,
                    fromNode, toNode,
                    fromNode.calcDistance(toNode), stair_width);
            newLink.addTag("STAIR_" + stair_set.name
                    + "_" + floor_string + "_" + next_floor_string);
        }
        return next_group;
    }

    private void duplicateNodesWithChangeGroup(MapPartGroup dst_group) {
        /* there should be another function just like this,
         * that copies objects from a group to another group */
        if (countSelectedNode() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No nodes selected!",
                    "Do nothing", JOptionPane.ERROR_MESSAGE);
            return;
        }
        MapNodeTable nodesToMove = new MapNodeTable();
        HashMap<MapNode, MapNode> nodeToNode = new HashMap<MapNode, MapNode>();
        for (MapNode node : getChildNodes()) {
            if (!node.selected) continue;
            double height_diff = node.getHeight() - getDefaultHeight();
            MapNode newNode = editor.getMap().createMapNode(dst_group, 
                    node.getAbsoluteCoordinates(),
                    dst_group.getDefaultHeight() + height_diff);
            nodesToMove.add(newNode);
            nodeToNode.put(node, newNode);
            newNode.selected = true;
        }
        int  pileCount = 0;
        String warning_message = "";
        for (MapNode newNode : nodesToMove) {
            for (MapNode node : getChildNodes()) {
                if (node == newNode) continue;
                if (node.getHeight() != newNode.getHeight()) continue;
                if (!node.getLocalCoordinates().equals(newNode.getLocalCoordinates())) continue;
                ++pileCount;
                String heightStr = "" + node.getHeight();
                
                if (!warning_message.contains(heightStr)) {
                    warning_message += heightStr + " ";
                }
            }
        }
        if (pileCount > 0) {
            JOptionPane.showMessageDialog(this,
                    "" + pileCount + " nodes were placed on\n" +
                    "nodes already existing.\nThis happened on:\n"
                    + warning_message,
                    "Nodes misplaced?",
                    JOptionPane.WARNING_MESSAGE);
        }
        
        for (MapLink link : getChildLinks()) {
            if (link.getFrom().selected && link.getTo().selected) {
                MapNode fromNode = nodeToNode.get(link.getFrom());
                MapNode toNode = nodeToNode.get(link.getTo());
                editor.getMap().createMapLink(dst_group,
                        fromNode, toNode,
                        link.length, link.width);
            }
        }

        /* for the next manipulation */
        clearSelection();
        for (MapNode node : nodesToMove) {
            node.selected = true;
        }
    }
    
    private void moveNodes() {
        if (countSelectedNode() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No nodes selected!",
                    "Do nothing", JOptionPane.ERROR_MESSAGE);
            return;
        }
            
        final String[] labels = {"X", "Y", "Z"}; 
        final double[][] range = {
                {-10000, 10000, 0},
                {-10000, 10000, 0},
                {-10000, 10000, 0.1}}; 
        double[] ret = GetDoublesDialog.showDialog("", labels, range);
        if (ret == null) return;

        moveNodes(ret[0], ret[1], ret[2]);
    }

    private void moveNodes(double dx, double dy, double dz) {
        moveNodes(current_group, dx, dy, dz);
    }

    private void moveNodes(MapPartGroup group,
            double dx, double dy, double dz) {
        for (MapNode node : group.getChildNodes()) {
            if (node.selected) {
                final double x = node.getLocalX() + dx; 
                final double y = node.getLocalY() + dy;

                node.setAbsoluteCoordinates(new Point2D.Double(x, y));
                node.setHeight(node.getHeight() + dz);
            }
        }
    }

    private void rotateAndScaleNodes() {
        final String[] labels = {"ScaleX", "ScaleY", "Rot"}; 
        final double[][] range = {
                {-100, 100, 1},
                {-100, 100, 1},
                {-360, 360, 0}}; 
        double[] ret = GetDoublesDialog.showDialog("RotScale", labels, range);
        if (ret == null) return;

        double sx = ret[0];
        double sy = ret[1];
        double r = ret[2] * 2 * Math.PI / 360.0;

        double cx = 0.0, cy = 0.0;
        int count = 0;
        for (MapNode node : getChildNodes()) {
            if (node.selected) {
                cx += node.getLocalX();
                cy += node.getLocalY();
                ++count; 
            }
        }
        cx /= count;
        cy /= count;
        
        double cosR = Math.cos(r);
        double sinR = Math.sin(r);

        for (MapNode node : getChildNodes()) {
            if (node.selected) {
                double x = node.getLocalX();
                double y = node.getLocalY();
                
                double dx = x -cx;
                double dy = y -cy;
                
                dx *= sx;
                dy *= sy;
                
                x = cx + dx * cosR + dy * sinR;
                y = cy - dx * sinR + dy * cosR;
                
                node.setAbsoluteCoordinates(new Point2D.Double(x, y));
            }
        }
    }

    boolean setScaleMode = false;

    private void setScale() {
        setScaleMode = true;
        panel.setScaleLine(null);
        panel.setScaleMode = true;
    }
    
    private void calcScale() {
        String[] labels = { "the actual length of the line (m)" };
        double len = scaleLine.getP1().distance(scaleLine.getP2());
        double ranges[][] = { {0.0, 100000.0, current_group.getScale() * len} };
        double[] length = GetDoublesDialog.showDialog("Set scale",
                labels, ranges);
        if (length == null) return;
        for (int i = 0; i < length.length; i++)
            System.err.print(length[i] + ", ");
        System.err.print("\n");
        current_group.setScale(length[0] / len);
        panel.setScaleLine(scaleLine);


        int ret = JOptionPane.showConfirmDialog(this,
                "Re-calculate link length based on this ratio for all links?\n" +
                " YES - all links\n" +
                " NO  - only this frame\n", "Confirm",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (ret == JOptionPane.YES_OPTION) {
            for (MapLink link : current_group.getChildLinks()) {
                link.length = link.getFrom().getLocalCoordinates().distance(link.getTo().getLocalCoordinates()) * current_group.getScale();
            }
            for (MapPartGroup group : editor.getMap().getGroups()) {
                group.setScale(current_group.getScale());
            }
        } else if (ret == JOptionPane.NO_OPTION) {
            for (MapLink link : getChildLinks()) {
                link.length = link.getFrom().getLocalCoordinates().distance(link.getTo().getLocalCoordinates()) * current_group.getScale();
            }
        }
    }

    class BGImageScaleDialog extends JDialog implements ActionListener {
        private JSpinner sxSpinner;
        private JSpinner sySpinner;
        private JButton okButton;
        private JButton cancelButton;

        public BGImageScaleDialog(JFrame owner, String title, boolean modal) {
            super(owner, title, modal);

            Container pane = getContentPane();
            pane.setLayout(new FlowLayout());

            pane.add(new JLabel("Scale X"));
            sxSpinner = new JSpinner(new SpinnerNumberModel(current_group.sx, -1000.0, 1000.0, 0.1));
            pane.add(sxSpinner);
            pane.add(new JLabel("Scale Y"));
            sySpinner = new JSpinner(new SpinnerNumberModel(current_group.sy, -1000.0, 1000.0, 0.1));
            pane.add(sySpinner);
            pane.add(new JLabel());

            okButton = new JButton("OK");
            okButton.addActionListener(this);
            pane.add(okButton);

            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(this);
            pane.add(cancelButton);

            pack();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "OK") {
                panel.setLocalZoomX((Double)sxSpinner.getValue());
                panel.setLocalZoomY((Double)sySpinner.getValue());
                panel.repaint();
                dispose();
            } else if (e.getActionCommand() == "Cancel") {
                dispose();
            }
        }
    }

    private void delete() {
        switch (editor.getMode()) {
        case EDIT_NODE:
        case PLACE_NODE:
            removeSelectedNodes();
            break;
        case EDIT_LINK:
        case PLACE_LINK:
            removeSelectedLinks();
            break;
        case PLACE_NODE_LINK:
            removeSelectedNodes();
            removeSelectedLinks();
            break;
        case EDIT_AREA:
        case PLACE_AREA:
            removeSelectedArea();
            break;
        }
    }

    /* Listeners */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand () == "Set background file") setBackgroundFile();
        else if (e.getActionCommand () == "Remove background file") removeBackgroundFile();
        else if (e.getActionCommand () == "Close") setVisible(false);

        else if (e.getActionCommand() == "Align Nodes Horizontally") {
            editor.setModified(true);
            for (MapNode node : getChildNodes()) {
                if (node.selected && node != currentNode) {
                    node.setAbsoluteCoordinates(new Point2D.Double(node.getLocalX(), currentNode.getLocalY()));
                }
            }
            editor.getNodePanel().refresh();
            clearSelection();
            repaint();
        }
        else if (e.getActionCommand () == "Align Nodes Vertically") {
            editor.setModified(true);
            for (MapNode node : getChildNodes()) {
                if (node.selected && node != currentNode) {
                    node.setAbsoluteCoordinates(new Point2D.Double(currentNode.getLocalX(), node.getLocalY()));
                }
            }
            editor.getNodePanel().refresh();
            clearSelection();
            repaint();
        }

        else if (e.getActionCommand() == "Remove nodes") {
            removeSelectedNodes();
        }
        
        else if (e.getActionCommand() == "Set node attribute") {
            setNodeAttribute();
        }

        else if (e.getActionCommand() == "Remove link") {
            removeSelectedLinks();
        }
        
        else if (e.getActionCommand() == "Change to Exit") {
            changeToExit();
        }
        
        else if (e.getActionCommand() == "Calc link length by distance") {
            calcLinkLength();
        }

        else if (e.getActionCommand() == "Make stairs") {
            editor.getMap().makeStairs();
        }

        else if (e.getActionCommand() == "Set height") {
            final String[] labels = {"Min", "Max", "Default"};
            final double[][] range = {
                    {-1000, 1000, getMinHeight()},  
                    {-1000, 1000, getMaxHeight()},  
                    {-1000, 1000, getDefaultHeight()},  
            };
            double[] heights = GetDoublesDialog.showDialog("Height", labels, range);
            if (heights != null) {
                setMinHeight(heights[0]);
                setMaxHeight(heights[1]);
                setDefaultHeight(heights[2]);
            }
        }

        else {
            if (e.getActionCommand().equals("Clear Symlinks")) {
                clearSymlinks();
                return;
            }
            for (MapPartGroup group : editor.getMap().getGroups()) {
                String command_candidate = "Symlink to " + group.toString();
                if (e.getActionCommand().equals(command_candidate)) {
                    addSymlinks(group);
                    return;
                }
            }

            System.err.println("Not implemented, action:" + e.getActionCommand());
        }
    }
    
    /* Mouse listeners */
    /* mouse listener helpers */
    static final int SaC = MouseEvent.SHIFT_MASK | MouseEvent.CTRL_MASK;
    private Boolean isLocalScrollingClick(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON2 &&
                ((e.getModifiers() & MouseEvent.SHIFT_MASK)
                        == MouseEvent.SHIFT_MASK)) return true;
        else if (e.getButton() == MouseEvent.BUTTON1 &&
                ((e.getModifiers() & SaC) == SaC)) return true;
        return false;
    }

    private Boolean isScrollingClick(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON2) return true;
        else if (e.getButton() == MouseEvent.BUTTON1 &&
                ((e.getModifiers() & MouseEvent.SHIFT_MASK)
                        == MouseEvent.SHIFT_MASK)) return true;
        return false;
    }

    private Boolean isLocalZoomingClick(MouseEvent e) {
        return (e.getButton() == MouseEvent.BUTTON3 &&
                ((e.getModifiers() & SaC) == SaC));
    }

    private Boolean isZoomingClick(MouseEvent e) {
        return (e.getButton() == MouseEvent.BUTTON3 &&
                ((e.getModifiers() & MouseEvent.SHIFT_MASK)
                        == MouseEvent.SHIFT_MASK));
    }

    public void mouseClicked(MouseEvent e) {
        currentNode = selectedNode;
        if (e.getButton() == MouseEvent.BUTTON1) {
            switch (editor.getMode()) {
            case PLACE_NODE:
                placeNode(e);
                break;
            case EDIT_NODE:
                selectNode(e);
                break;
            case PLACE_LINK:
                placeLink(e);
                break;
            case EDIT_LINK:
                selectLink(e);
                break;
            case PLACE_NODE_LINK:
                placeNodeLink(e);
                return;
            case EDIT_AREA:
                selectArea(e);
                break;
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            switch (editor.getMode()) {
            case PLACE_NODE:
            case EDIT_NODE:
                manipulateNode(e);
                break;
            case PLACE_LINK:
            case EDIT_LINK:
                manipulateLink(e);
                break;
            case PLACE_NODE_LINK:
                placeNodeLink(e);
                return;
            case EDIT_AREA:
            case PLACE_AREA:
                manipulateArea(e);
                break;
            }
        }
        repaint ();
    }

    public void mouseEntered(MouseEvent e) { 
        panel.updateSelectedArea(null);
    }

    public void mouseExited(MouseEvent e) {
        panel.updateSelectedArea(null);
    }
 
    Point2D scaleLineFrom = null;
    Line2D scaleLine = null;;
    public void mousePressed(MouseEvent e) {
        if (setScaleMode) {
            scaleLineFrom = panel.revCalcPos(e.getX(), e.getY());
            panel.repaint();
        } else if (isLocalScrollingClick(e)) {
            localscrolling = true;
            dragStartX = e.getX();
            dragStartY = e.getY();
        } else if (isLocalZoomingClick(e)) {
            localzooming = true;
            dragStartX = e.getX();
            dragStartY = e.getY();
        } else if (isScrollingClick(e)) {
            scrolling = true;
            dragStartX = e.getX();
            dragStartY = e.getY();
        } else if (isZoomingClick(e)) {
            zooming = true;
            dragStartX = e.getX();
            dragStartY = e.getY();
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            if (editor.getMode() == EditorMode.EDIT_NODE &&
                    hoverNode != null) {
                draggingNode = true;
            } else if (editor.getMode() == EditorMode.EDIT_NODE ||
                    editor.getMode() == EditorMode.EDIT_LINK ||
                    editor.getMode() == EditorMode.PLACE_AREA ||
                    editor.getMode() == EditorMode.EDIT_AREA   // tkokada
                    ) {
                areaSelection = true;
                startpos = panel.revCalcPos(e.getX(), e.getY());
                selectedArea = new Rectangle2D.Double(startpos.getX(), startpos.getY(), 0, 0);
            }
        } 
        // この辺りでステータス更新を有効にするのがベストな妥協点(斉藤)
        panel.updateStatusEnabled = true;
    }

    public void mouseReleased(MouseEvent e) {
        draggingNode = false;
        if (setScaleMode) {
            calcScale();
            setScaleMode = false;
            panel.setScaleMode = false;
            panel.setTempLine(null);
        } else if (e.getButton() == MouseEvent.BUTTON1 && areaSelection) {
            areaSelection = false;
            Point2D endpos = panel.revCalcPos(e.getX(), e.getY());
            double startX = Math.min(startpos.getX(), endpos.getX());
            double startY = Math.min(startpos.getY(), endpos.getY());
            double width = Math.abs(startpos.getX() - endpos.getX());
            double height = Math.abs(startpos.getY() - endpos.getY());

            if (width < 10 && height < 10) return;

            if ((e.getModifiers() & MouseEvent.CTRL_MASK) == 0) {
                clearSelection();
            }

            selectedArea = new Rectangle2D.Double(startX, startY,
                    width, height);
            switch (editor.getMode()) {
            case EDIT_NODE:
                for (MapNode node : getChildNodes()) {
                    if (selectedArea.contains(node.getLocalCoordinates()) &&
                            //node.isInLayer(layer, minHeight, maxHeight)
                            node.isBetweenHeight( getMinHeight(), getMaxHeight())

                    ) {
                        node.selected = true;
                    }
                }
                editor.getNodePanel().refresh();
                break;
            case EDIT_LINK:
                for (MapLink link : getChildLinks()) {
                    MapNode from = link.getFrom();
                    MapNode to = link.getTo();
                    if (selectedArea.contains(from.getLocalCoordinates())
                            && selectedArea.contains(to.getLocalCoordinates())
                            && to.isBetweenHeight(getMinHeight(), getMaxHeight())
                            && from.isBetweenHeight(getMinHeight(), getMaxHeight())     
                    ) {
                        link.selected = true;
                    }
                }
                editor.getLinkPanel().refresh();
                break;
            case EDIT_AREA:
                for (MapArea area : getChildMapAreas()) {
                    // tkokada
                    // add an angle parameter to MapAreaRectangle,
                    // apply the angle to this determination.
                    /* old source code
                    if (selectedArea.contains(area.getShape().getBounds())) {
                        area.selected = true;
                    }
                    */
                    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0 && area.selected) {
                        area.selected = true;
                    } else {
                        area.selected = true;
                        for (Point2D point : area.getAllVertices()) {
                            if (!selectedArea.contains(point)) {
                                area.selected = false;
                                break;
                            }
                        }
                    }
                }
                editor.getAreaPanel().refresh();
                break;
            case PLACE_AREA:
                placeMapArea();
                break;
            default:
                break;
            }
            panel.updateSelectedArea(null);
        }
        panel.repaint();
        
        scrolling = false;
        zooming = false;
        localscrolling = false;
        localzooming = false;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        int i = e.getWheelRotation();
        if ((e.getModifiers() & SaC) == SaC) {
            panel.localRotate(i);
        } else if ((e.getModifiers() & MouseEvent.SHIFT_MASK)
                == MouseEvent.SHIFT_MASK) {
            panel.localZoomX(i);
            panel.localZoomY(i);
        } else if ((e.getModifiers() & MouseEvent.CTRL_MASK)
                == MouseEvent.CTRL_MASK) {
            // この機能は不要
            // panel.localZoomY(i);
        } else {
            panel.zoom(i);
        }
        panel.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        if (setScaleMode) {
            Point2D scaleLineTo = panel.revCalcPos(e.getX(), e.getY());
            scaleLine = new Line2D.Double(scaleLineFrom, scaleLineTo);
            panel.setTempLine(scaleLine);
        } else if (localscrolling) {
            panel.localScroll((e.getX() - dragStartX), (e.getY() - dragStartY));

            dragStartX = e.getX();
            dragStartY = e.getY();
        } else if (localzooming) {
            int i = 0;
            int dragValue = (e.getX() - dragStartX + e.getY() - dragStartY) / 10;
            if (dragValue > 0) i = -1;
            else if (dragValue < 0) i = 1;
            
            if (i != 0) {
                panel.localZoomX(i);
                panel.localZoomY(i);

                dragStartX = e.getX();
                dragStartY = e.getY();
            }
        } else if (scrolling) {
            panel.scroll((e.getX() - dragStartX), (e.getY() - dragStartY));

            dragStartX = e.getX();
            dragStartY = e.getY();
        } else if (zooming) {
            int i = 0;
            int dragValue = (e.getX() - dragStartX + e.getY() - dragStartY) / 10;
            if (dragValue > 0) i = -1;
            else if (dragValue < 0) i = 1;
            
            if (i != 0) {
                panel.zoom(i);

                dragStartX = e.getX();
                dragStartY = e.getY();
            }
        } else if (selectedNode != null && draggingNode) {
            hoverNode.setPos(panel.revCalcPos(e.getX(), e.getY()));
            selectedNode.setAbsoluteCoordinates(panel.revCalcPos(e.getX(), e.getY()));
            panel.updateHoverNode(hoverNode);
            editor.setModified(true);
            editor.getNodePanel().refresh();
        } else if (areaSelection &&
                ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK)
                        == MouseEvent.BUTTON1_DOWN_MASK)) {
            Point2D endpos = panel.revCalcPos(e.getX(), e.getY());
            double startX = Math.min(startpos.getX(), endpos.getX());
            double startY = Math.min(startpos.getY(), endpos.getY());
            double width = Math.abs(startpos.getX() - endpos.getX());
            double height = Math.abs(startpos.getY() - endpos.getY());
            
            selectedArea = new Rectangle2D.Double(startX, startY,
                    width, height);
            panel.updateSelectedArea(selectedArea);
        } else {
            return;
        }
        panel.repaint();
    }

    public void mouseMoved(MouseEvent e) {
        Point2D p = panel.revCalcPos(e.getX(), e.getY());
        mousePoint = p;     // これはCrowdWalk座標値
        switch (editor.getMode()) {
        case PLACE_NODE:
            if (placeHoverNode(p)) {
                panel.updateHoverNode(hoverNode);
                panel.repaint();
            }
            break;
        case PLACE_LINK:
            if (hoverLinkFromCandidate == null) {
                if (updateHoverNode(p)) {
                    panel.updateHoverNode(hoverNode);
                    panel.repaint();
                }
            } else if (placeHoverLink(p)) {
                panel.updateHoverNode(new Hover(null,
                        hoverLinkFromCandidate.getLocalCoordinates()));
                panel.updateHoverLink(hoverLink);
                panel.repaint();
            }
            break;
        case PLACE_NODE_LINK:
            // If first node is not placed yet, same with PLACE_NODE.
            // Else, same with PLACE_LINK
            if (placeHoverNode(p)) {
                panel.updateHoverNode(hoverNode);
                panel.repaint();
            }
            if (initialNode != null) {
                if (placeHoverNodeLink(p)) {
                    panel.updateHoverNode(new Hover(null,
                            hoverLinkFromCandidate.getLocalCoordinates()));
                    panel.updateHoverLink(hoverLink);
                    panel.repaint();
                }
            }
            break;
        case EDIT_NODE:
            if (updateHoverNode(p)) {
                panel.updateHoverNode(hoverNode);
                panel.repaint();
            }
            break;
        case EDIT_LINK:
            if (updateHoverLink(p)) {
                panel.updateHoverLink(hoverLink);               
                panel.repaint();
            }
            break;
        case EDIT_AREA:
            if (updateHoverArea(p)) {
                panel.updateHoverArea(hoverArea);
                panel.repaint();
            }
            break;
        }
    }
 
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_LEFT:
            panel.scroll(-5, 0);
            break;
        case KeyEvent.VK_RIGHT:
            panel.scroll(5, 0);
            break;
        case KeyEvent.VK_UP:
            panel.scroll(0, -5);
            break;
        case KeyEvent.VK_PAGE_UP:
            panel.zoom(-1);
            break;
        case KeyEvent.VK_PAGE_DOWN:
            panel.zoom(1);
            break;

        case KeyEvent.VK_BACK_SPACE:
            System.err.println("undo は無効です");
            // 正常動作が保証できないため無効にした(斉藤)
            //editor.getMap().undo(editor);
            break;
        case KeyEvent.VK_DELETE:
            delete();
            break;

        case KeyEvent.VK_QUOTE:
        case KeyEvent.VK_7:
            editor.setMode(EditorMode.EDIT_NODE);
            break;
        case KeyEvent.VK_COMMA:
            editor.setMode(EditorMode.EDIT_LINK);
            break;
        case KeyEvent.VK_P:
            editor.setMode(EditorMode.EDIT_GROUP);
            break;
        case KeyEvent.VK_Y:
            editor.setMode(EditorMode.EDIT_AREA);
            break;
        case KeyEvent.VK_A:
            editor.setMode(EditorMode.PLACE_NODE);
            break;
        case KeyEvent.VK_C:
            editor.setMode(EditorMode.PLACE_LINK);
            break;
        case KeyEvent.VK_B:
            editor.setMode(EditorMode.PLACE_NODE_LINK);
            break;
        case KeyEvent.VK_U:
            editor.setMode(EditorMode.PLACE_GROUP);
            break;
        case KeyEvent.VK_I:
            editor.setMode(EditorMode.PLACE_AREA);
            break;

            /* toggle showing */
        case KeyEvent.VK_SEMICOLON:
            showNodes.setState(!showNodes.getState());
            // TODO: この辺りにあるステータス表示は repaint() で書き換えられてしまうためこのままでは意味がない
            status.setText("Showing nodes: " + showNodes.getState());
            break;
        case KeyEvent.VK_Q:
            showLinks.setState(!showLinks.getState());
            status.setText("Showing links: " + showLinks.getState());
            break;
        case KeyEvent.VK_K:
            showGroups.setState(!showGroups.getState());
            status.setText("Showing groups: " + showGroups.getState());
            break;
        case KeyEvent.VK_X:
            showArea.setState(!showArea.getState());
            status.setText("Showing area: " + showArea.getState());
            break;
        case KeyEvent.VK_S:
            showScaling.setState(!showScaling.getState());
            status.setText("Scaling mode: " + showScaling.getState());
            break;
        default:
            System.err.println("Unhandlesd key: " + e.getKeyCode());
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    /* getters/setters */
    public void setLabel(String _label) {
        label = _label;
        setTitle(label);
    }
    
    public String getLabel() {
        return label;
    }
    
    @Override
    public void windowActivated(WindowEvent e) {}
    @Override
    public void windowClosed(WindowEvent e) {
    }
    @Override
    public void windowClosing(WindowEvent e) {
        editor.getMap().removeEditorFrame(current_group);
        try {
            this.finalize();
        } catch (Throwable e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowOpened(WindowEvent e) {
        if (current_group.getImageFileName() != null
                && !current_group.getImageFileName().equals("")
                && background_read == false) {
            readBackgroundWithName();
        }
        panel.centering(true);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        setStatus();
    }

    @Override
    public void windowLostFocus(WindowEvent e) {}

    /* getters and setters for the ob_node attributes related to the region of the group */
    public void setDefaultHeight(double defaultHeight) {
        current_group.setDefaultHeight(defaultHeight);
    }

    public double getDefaultHeight() {
        return current_group.getDefaultHeight();
    }

    public void setMinHeight(double minHeight) {
        current_group.setMinHeight(minHeight);
    }

    public double getMinHeight() {
        return current_group.getMinHeight();
    }

    public void setMaxHeight(double maxHeight) {
        current_group.setMaxHeight(maxHeight);
    }

    public double getMaxHeight() {
        return current_group.getMaxHeight();
    }
    // tkokada added
    public int getVerticalDivision() {
        return current_group.getVerticalDivision();
    }
    // tkokada added
    public int getHorizontalDivision() {
        return current_group.getHorizontalDivision();
    }
    // tkokada added
    public double getRotation() {
        return current_group.getRotation();
    }
    
    public void setNorth(double north) {
        current_group.setNorth(north);
    }
    public double getNorth() {
        return current_group.getNorth();
    }

    public void setSouth(double south) {
        current_group.setSouth(south);
    }
    public double getSouth() {
        return current_group.getSouth();
    }

    public void setEast(double east) {
        current_group.setEast(east);
    }
    public double getEast() {
        return current_group.getEast();
    }
    
    public void setWest(double west) {
        current_group.setWest(west);
    }
    public double getWest() {
        return current_group.getWest();
    }
    
    public void setStatus(String _mode) {
        status.setText(String.format("Height (Min : %s, Max : %s, Default : %s)    Mode : %s    Mouse Position : (%s, %s)    Map Position : (%s, %s)", getMinHeight(), getMaxHeight(), getDefaultHeight(), _mode, panel.point_on_panel.x, panel.point_on_panel.y, mousePoint.getX(), mousePoint.getY()));
    }
    
    public void setStatus() {
        setStatus(editor.getMode().toString());
    }

    /* access to the object(nodes, links, agents, sub-groups)
     * that are managed under this frame */
    public MapNodeTable getChildNodes() {
        return current_group.getChildNodes();
    }

    public MapNodeTable getChildNodesAndSymlinks() {
        return current_group.getChildNodesAndSymlinks();
    }

    public MapLinkTable getChildLinks() {
        return current_group.getChildLinks();
    }

    public ArrayList<AgentBase> getChildAgents() {
        return current_group.getChildAgents();
    }

    public ArrayList<MapPartGroup> getChildGroups() {
        return current_group.getChildGroups();
    }

    public ArrayList<MapArea> getChildMapAreas() {
        return current_group.getChildMapAreas();
    }

    public ArrayList<OBNodeSymbolicLink> getSymbolicLinks() {
        return current_group.getSymbolicLinks();
    }

    /**
     * 選択中のリンクのリストを返す。
     */
    public MapLinkTable getSelectedLinks() {
        MapLinkTable selectedLinks = new MapLinkTable();
        for (MapLink link : getChildLinks()) {
            if (link.selected) {
                selectedLinks.add(link);
            }
        }
        return selectedLinks;
    }

    //public void setRandom(Random _random) {
    //    random = _random;
    //}
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
