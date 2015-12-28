// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GraphicsContext3D;
import javax.media.j3d.Group;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Raster;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleFanArray;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.Gui.Colors;
import nodagumi.ananPJ.Gui.Colors.*;
import nodagumi.ananPJ.Gui.LinkAppearance;
import nodagumi.ananPJ.Gui.NodeAppearance;
import nodagumi.ananPJ.Gui.ViewChangeListener;
import nodagumi.ananPJ.Gui.ViewChangeManager;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;

import nodagumi.Itk.*;

import com.sun.j3d.utils.universe.SimpleUniverse;

public abstract class NetworkPanel3DBase extends JPanel {
    /**
     * Basic class for 3D canvas and other controls.
     * - Place canvas
     * - Define basic colors
     * - Drawing of nodes and links
     * - menu and status bar
     * - show logo 
     */
    public static final int TOP = 1;
    public static final int BOTTOM = 2;

    protected Map<Shape3D, OBNode> canvasobj_to_obnode = new HashMap<Shape3D, OBNode>();

    protected SimpleUniverse universe = null;

    protected MapNodeTable nodes;
    protected MapLinkTable links;

    private boolean isInitialized = false;

    BoundingSphere bounds = new BoundingSphere(new Point3d(), 20000.0);
    private ViewChangeManager viewChangeManager;

    /* flags to control drawing */
    protected float link_transparency = 0.5f;

    protected String screenshotDir = "screenshots";
    protected String screenshotImageType = "png";
    protected boolean show_logo = false;
    protected boolean show_message = false;
    protected int messagePosition = TOP;
    // tkokada polygon
    protected boolean show_3d_polygon = true;

    protected float link_width = 1.0f;
    protected boolean link_draw_density_mode = false;

    /* Canvas class
     * - show logo
     * - capture
     */
    protected class CaptureCanvas3D extends Canvas3D {

        public CaptureCanvas3D(GraphicsConfiguration arg0) {
            super(arg0);
            aist_logo = getToolkit().createImage(getClass().getResource("/img/aist_logo.png"));
        }

        private String filename = null;

        public synchronized void catpureNextFrame(String _filename) {
            filename = _filename;
        }

        public synchronized  boolean hasFrameToCapture() {
            return filename != null;
        }

        public String message = " ";
        private Image aist_logo = null;

        @Override
        public void postRender() {
            super.postRender();

            boolean flushRequired = false;
            J3DGraphics2D g = getGraphics2D();
            if (show_logo) {
                int x = getWidth() - aist_logo.getWidth(null);
                int y = getHeight() - aist_logo.getHeight(null);
                g.drawImage(aist_logo, x, y, null);
                flushRequired = true;
            }
            if (show_message) {
                FontMetrics fm = g.getFontMetrics();
                int width = fm.stringWidth(message);
                int height = fm.getHeight();
                int ascent = fm.getAscent();
                int x = 12;     // メッセージの基準表示位置
                int y = 12;     //          〃
                if ((messagePosition & BOTTOM) == BOTTOM) {
                    y += (int)getSize().getHeight() - ascent;
                }
                g.setColor(Colors.BACKGROUND_3D_COLOR.get());           // メッセージの背景色
                g.fillRect(x - 4, y - ascent, width + 7, height - 1);   // メッセージの背景描画
                g.setColor(Color.BLACK);
                g.drawString(message, x, y);
                flushRequired = true;
            }
            if (flushRequired) {
                g.flush(true);
            }
        }

        @Override
        public synchronized void preRender() {
            //int width = 800;
            //int height = 600;
            setCanvasSize(canvas_width, canvas_height);
            super.preRender();
        }

        @Override
        public synchronized void postSwap() {
            super.postSwap();

            if (filename == null) return;

            int width = canvas_width;
            int height = canvas_height;

            ImageComponent2D image = new ImageComponent2D(
                    ImageComponent.FORMAT_RGB, width, height);

            Raster readRaster = new Raster(new Point3f(0.0f, 0.0f, 0.0f),
                    Raster.RASTER_COLOR, 0, 0, width, height, image, null);
            GraphicsContext3D gc = canvas.getGraphicsContext3D();
            gc.readRaster(readRaster);

            BufferedImage img = image.getImage();
            if (img == null) {
                System.err.println("image is null!");
                return;
            }
            try {
                ImageIO.write(img, screenshotImageType,
                    new File(screenshotDir + "/" + filename + "." + screenshotImageType));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            filename = null;
            screenShotCaptured();
        }
    };

    /**
     * スクリーンショットのファイル作成が完了した時に呼ばれる.
     */
    protected void screenShotCaptured() {}

    protected MenuBar menu_bar = null;
    protected transient CaptureCanvas3D canvas = null;
    public JFrame parent = null;
    protected LinkedHashMap<String, LinkAppearance> linkAppearances = new LinkedHashMap<String, LinkAppearance>();
    protected LinkedHashMap<String, NodeAppearance> nodeAppearances = new LinkedHashMap<String, NodeAppearance>();

    protected NetworkPanel3DBase(MapNodeTable _nodes,
                                 MapLinkTable _links,
                                 JFrame _parent,
                                 CrowdWalkPropertiesHandler _properties) {
        nodes = _nodes;
        links = _links;
        parent = _parent;

        canvas_width = 800;
        canvas_height = 600;

        try {
            // 再ロードしているのは、該当するタグが複数あった場合の適用ルールを設定ファイルに記述した順(上が優先)にするため
            String filePath = null;
            if (_properties != null && _properties.isDefined("link_appearance_file")) {
                filePath = _properties.getFilePath("link_appearance_file", null);
                LinkAppearance.loadLinkAppearances(new FileInputStream(filePath), linkAppearances);
            }
            LinkAppearance.loadLinkAppearances(
                    getClass().getResourceAsStream("/link_appearance.json"), linkAppearances);
            if (_properties != null && _properties.isDefined("link_appearance_file")) {
                LinkAppearance.loadLinkAppearances(new FileInputStream(filePath), linkAppearances);
            }

            if (_properties != null && _properties.isDefined("node_appearance_file")) {
                filePath = _properties.getFilePath("node_appearance_file", null);
                NodeAppearance.loadNodeAppearances(new FileInputStream(filePath), nodeAppearances);
            }
            NodeAppearance.loadNodeAppearances(
                    getClass().getResourceAsStream("/node_appearance.json"), nodeAppearances);
            if (_properties != null && _properties.isDefined("node_appearance_file")) {
                NodeAppearance.loadNodeAppearances(new FileInputStream(filePath), nodeAppearances);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        viewChangeManager = new ViewChangeManager() {
            public void jobEntered() {
                // parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }

            public void jobLeaved() {
                // parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        };
        addViewChangeListener("link transparency changed", new ViewChangeListener() {
            public void update() {
                updateLinkTransparency();
            }
        });
        addViewChangeListener("link width changed", new ViewChangeListener() {
            public void update() {
                updateLinkWidth();
            }
        });
    }

    protected void setupFrame(MapNodeTable _nodes,
			      MapLinkTable _links,
			      JFrame _parent) {
	nodes = _nodes;
	links = _links;
	parent = _parent;

	canvas_width = 800;
	canvas_height = 600;
    }

    public void setCanvasSize(int w, int h) {
        canvas_width = w;
        canvas_height = h;
    }

    /* !!! note that initialize must be after construction */
    public void initialize() {
        setupMenu();
        setupContents();
        setupExtraContents();
        setup_control_panel();
    }

    abstract protected void register_map_objects();
    abstract protected void registerOtherObjects();

    protected Menu menu_view = null;
    protected Menu menu_action = null;

    public MenuBar getMenuBar() {
        return menu_bar;
    }

    protected void setupMenu() {
        menu_bar = new MenuBar();

        /* file menu */
        Menu menu_file = new Menu("File");
        MenuShortcut shortcut = new MenuShortcut(java.awt.event.KeyEvent.VK_W);
        MenuItem item = new MenuItem("Close", shortcut);

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.dispose();
            }
        });
        menu_file.add(item);
        menu_bar.add(menu_file);

        /* view menu */
        menu_view = new Menu("View");

        Menu menu_view_link_transparency = new Menu("Link transparance");
        class ChangeLinkTransparencyActionListner implements ActionListener {
            float t;

            public ChangeLinkTransparencyActionListner(float _t) {
                t = _t;
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                link_transparency = t;
                notifyViewChange("link transparency changed");
            }
        }
        MenuItem mi = new MenuItem("not transparent");
        mi.addActionListener(new ChangeLinkTransparencyActionListner(0.0f));
        menu_view_link_transparency.add(mi);
        mi = new MenuItem("half transparent");
        mi.addActionListener(new ChangeLinkTransparencyActionListner(0.5f));
        menu_view_link_transparency.add(mi);
        mi = new MenuItem("hidden");
        mi.addActionListener(new ChangeLinkTransparencyActionListner(1.0f));
        menu_view_link_transparency.add(mi);
        menu_view.add(menu_view_link_transparency);

        Menu menu_view_link_width = new Menu("Link width");
        class ChangeLinkWidthActionListner implements ActionListener {
            float t;

            public ChangeLinkWidthActionListner(float _t) {
                t = _t;
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                link_width = t;
                notifyViewChange("link width changed");
            }
        }
        for (float w = 1.0f; w < 17; w *= 2) {
            mi = new MenuItem("" + w);
            mi.addActionListener(new ChangeLinkWidthActionListner(w));
            menu_view_link_width.add(mi);
        }
        menu_view.add(menu_view_link_width);
        menu_bar.add(menu_view);

        /* action menu */
        menu_action = new Menu("Action");
        menu_bar.add(menu_action);
    }

    protected int canvas_width;
    protected int canvas_height;
    protected BranchGroup scene = null;
    protected void setupContents() {
        if (isInitialized) {
            scene.detach();
            scene = null;
            universe.cleanup();
            universe = null;
        }
        if (!isInitialized) {
            GraphicsConfiguration config = SimpleUniverse
                    .getPreferredConfiguration();
            setLayout(new BorderLayout());

        /*
        if (config != null) // tkokada
            System.out.println(config.toString());
        else
            System.err.println("NetworkPanel3DBase.setupContents: config " +
                    "is null!");
        */
            canvas = new CaptureCanvas3D(config);
            canvas.setSize(new Dimension(canvas_width, canvas_height));
            add(canvas, BorderLayout.CENTER);

        }
        universe = new SimpleUniverse(canvas);
        universe.getViewingPlatform().setNominalViewingTransform();

        /*
        if (isInitialized) {
            scene.detach();
            scene = null;
        }
        */
        scene = createSceneGraph();

        universe.addBranchGraph(scene);
        /*
        } else {
            map_objects_parent.detach();
            setup_network();
        }
        */
    }

    protected void setupExtraContents() {}

    protected void setup_control_panel() {}

    protected TransformGroup view_trans = null;
    protected BranchGroup view_trans_parent = null;
    protected Transform3D trans3d_home = new Transform3D();

    protected double rot_x = -3 * Math.PI / 8;
    protected double rot_y = 0;
    protected double rot_z = Math.PI / 4;
    protected double zoom_scale = 1.0;

    protected Vector3d trans_trans = new Vector3d(-100, -100, -500);

    private BranchGroup createSceneGraph() {
        /*
        if (isInitialized) {
            view_trans_parent.detach();
            view_trans_parent = null;
        }
        */
        BranchGroup objRoot = new BranchGroup();
        objRoot.setCapability(BranchGroup.ALLOW_DETACH);

        /* tkokada: background color */
        Background background = new Background(Colors.BACKGROUND_3D_COLOR);
        background.setApplicationBounds(bounds);
        objRoot.addChild(background);

        /* light */
        AmbientLight alight = new AmbientLight();
        alight.setInfluencingBounds(bounds);
        objRoot.addChild(alight);
        DirectionalLight dlight = new DirectionalLight(Colors.BLACK,
                new Vector3f(0.87f, 0.0f, -0.5f));
        dlight.setInfluencingBounds(bounds);
        objRoot.addChild(dlight);

        /* view control */
        view_trans_parent = new BranchGroup();
        view_trans_parent.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        //view_trans_parent.setCapability(BranchGroup.ALLOW_DETACH);
        view_trans = new TransformGroup();
        view_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        view_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        view_trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
        view_trans_parent.addChild(view_trans);

        //objRoot.addChild(view_trans);
        objRoot.addChild(view_trans_parent);

        /* network */
        setup_network();
        //view_trans.addChild(map_objects);
        view_trans.addChild(map_objects_parent);
        update_viewtrans();
        view_trans.getTransform(trans3d_home);

        /* other objects */
        registerOtherObjects();

        viewChangeManager.setSchedulingBounds(bounds);
        objRoot.addChild(viewChangeManager);

        objRoot.compile();

        return objRoot;
    }

    protected void update_viewtrans() {
        Transform3D trans = new Transform3D();
        Transform3D rotx = new Transform3D();
        rotx.rotX(rot_x);
        trans.mul(rotx);
        Transform3D roty = new Transform3D();
        roty.rotY(rot_y);
        trans.mul(roty);
        Transform3D rotz = new Transform3D();
        rotz.rotZ(rot_z);
        trans.mul(rotz);

        trans.setTranslation(trans_trans);
        trans.setScale(zoom_scale);
        view_trans.setTransform(trans);
    }
    
    protected void setViewPoint(double scale) {
        universe.getViewingPlatform().setNominalViewingTransform();

        //universe.getViewer().getView().setBackClipDistance(scale * 10);
        universe.getViewer().getView().setBackClipDistance(1000.0);
        //universe.getViewer().getView().setFrontClipDistance(scale / 300);
        universe.getViewer().getView().setFrontClipDistance(0.5);
    }

    protected void setViewToHome() {
        rot_x = -3 * Math.PI / 8;
        rot_y = 0;
        rot_z = Math.PI / 4;
        zoom_scale = 1.0;

        trans_trans = new Vector3d(-100, -100, -500);
        zoom_scale = 1.0;
        view_trans.setTransform(trans3d_home);
    }

    protected TransformGroup map_objects = new TransformGroup();
    protected BranchGroup map_objects_parent = new BranchGroup();
    private void setup_network() {
        if (isInitialized)
            map_objects = null;
        map_objects = new TransformGroup();
        map_objects.setCapability(Group.ALLOW_CHILDREN_WRITE);
        map_objects.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        map_objects_parent = new BranchGroup();
        map_objects_parent.setCapability(BranchGroup.ALLOW_DETACH);
        map_objects_parent.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        map_objects_parent.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        map_objects_parent.addChild(map_objects);

        setup_links();
        setup_nodes();
        register_map_objects();
        Transform3D map_trans = new Transform3D();

        /* centering */
        setViewPoint(calcObjectScale(map_objects));
        map_trans.setTranslation(
                new Vector3d(-point_center.x, point_center.y,
                    -point_center.z));
        map_trans.setScale(new Vector3d(1.0, -1.0, 1.0));
        map_objects.setTransform(map_trans);
    }

    private Vector3d point_min, point_max, point_center;

    /* scale to transform GuiSimulationEditorLauncher coordinates to
     * 3D view coordinates */
    protected double calcObjectScale(TransformGroup objects) {
        point_center = new Vector3d(0.0, 0.0, 0.0);
        point_min = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MAX_VALUE);
        point_max = new Vector3d(Double.MIN_VALUE, Double.MIN_VALUE,
                Double.MIN_VALUE);

        int count = 0;
        for (final MapNode node : nodes) {
            ++count;
            final Point2D pos = node.getAbsoluteCoordinates();

            final double x = pos.getX();
            final double y = pos.getY();
            final double z = node.getHeight()
                    / ((MapPartGroup) node.getParent()).getScale();

            point_center.x += x;
            point_center.y += y;
            point_center.z += z;
            point_min.x = Math.min(point_min.x, x);
            point_min.y = Math.min(point_min.y, y);
            point_min.z = Math.min(point_min.z, z);
            point_max.x = Math.max(point_max.x, x);
            point_max.y = Math.max(point_max.y, y);
            point_max.z = Math.max(point_max.z, z);
        }
        point_center.x /= count;
        point_center.y /= count;
        point_center.z /= count;

        double scale = Math.max(point_max.x - point_center.x, Math.max(
                point_max.y - point_center.y,
                Math.max(point_max.z - point_center.z, Math.max(
                        point_center.x - point_min.x,
                        Math.max(point_center.y - point_min.y, point_center.z
                                - point_min.z)))));
        point_center.z = 0;
        return scale;
    }

    /**
     * 描画更新イベント用のリスナを登録する.
     */
    public void addViewChangeListener(String event, ViewChangeListener listener) {
        viewChangeManager.addViewChangeListener(event, listener);
    }

    /**
     * 描画更新イベントの発生を通知する.
     */
    public boolean notifyViewChange(String event) {
        return viewChangeManager.notifyViewChange(event);
    }

    // 全リンクの MapLink と BranchGroup との対応
    protected HashMap<MapLink, BranchGroup> linkGroups = new HashMap<>();

    // 表示対象ノードの MapNode と TransformGroup との対応
    protected HashMap<MapNode, BranchGroup> displayedNodeGroups = new HashMap<>();

    /**
     * すべての通常リンクの透明度を link_transparency 値で更新する.
     */
    public void updateLinkTransparency() {
        for (BranchGroup group : linkGroups.values()) {
            if (group.numChildren() == 1) {
                Shape3D shape = (Shape3D)group.getChild(0);
                TransparencyAttributes ta = shape.getAppearance().getTransparencyAttributes();
                ta.setTransparency(link_transparency);
            }
        }
    }

    /**
     * すべての通常リンクの幅を link_width 値で更新する.
     */
    public void updateLinkWidth() {
        for (BranchGroup group : linkGroups.values()) {
            if (group.numChildren() == 1) {
                Shape3D shape = (Shape3D)group.getChild(0);
                LineAttributes la = shape.getAppearance().getLineAttributes();
                la.setLineWidth(link_width);
            }
        }
    }

    protected Color3f colors_for_link(MapLink link) {
        if (link.hasTag("STRUCTURE")){//
            return linkAppearances.get("STRUCTURE").color;
        } else if (link.hasTag("FLOOR")) {
            return linkAppearances.get("FLOOR").color;
        } else if (link_draw_density_mode) {
            return new Color3f(link.getColorFromDensity());
        } else if (link.isShutOff()) {
            return Colors.YELLOW;
        } else if (link.hasTag("RED")) {
            return Colors.RED;
        } else if (link.hasTag("BLUE")) {
            return Colors.BLUE;
        } else if (link.hasTag("PINK")) {
            return Colors.PINK;
        } else if (link.hasTag("LIGHTB")) {
            return Colors.LIGHTB;
        }

        for (Map.Entry<String, LinkAppearance> entry : linkAppearances.entrySet()) {
            if (link.hasTag(entry.getKey())) {
                return entry.getValue().color;
            }
        }

        return Colors.DEFAULT_LINK_COLOR;
    }

    // tkokada polygon
    protected Color3f colors_for_polygon(String tag) {
        if (tag.contains("OCEAN")) {
            return Colors.SLATEBLUE;
        } else if (tag.contains("STRUCTURE")) {
            return Colors.LIGHTGRAY;
        }
        return Colors.GRAY;
    }

    protected TransparencyAttributes transparency_for_polygon(String tag) {
        if (tag.contains("OCEAN")) {
            return new TransparencyAttributes(TransparencyAttributes.FASTEST,
                    0.0f);
        } else if (tag.contains("STRUCTURE")) {
            return new TransparencyAttributes(TransparencyAttributes.FASTEST,
                    0.8f);
        }
        return new TransparencyAttributes(TransparencyAttributes.FASTEST, 0.5f);
    }

    /* Determinate link geometry
     */

    protected BranchGroup structure_group = new BranchGroup();
    boolean show_structure = true;
    protected void setup_links() {
        if (!isInitialized) {
            structure_group = new BranchGroup();
        } else {
            structure_group.detach();
            structure_group = null;
            structure_group = new BranchGroup();
        }
        path_appearance = createPathAppearance();

        // tkokada polygon
        HashMap<String, MapLinkTable> polygons =
            new HashMap<String, MapLinkTable>();
        for (final MapLink link : links) {
            final MapNode from = link.getFrom();
            final MapNode to = link.getTo();
            final double scale = ((MapPartGroup) link.getParent()).getScale();

            // tkokada polygon
            boolean containPolygon = false;
            for (String tag : link.getTags()) {
                if (tag.contains("POLYGON")) {
                    containPolygon = true;
                    if (!polygons.containsKey(tag)) {
                        MapLinkTable polygonLinks =
                            new MapLinkTable();
                        polygonLinks.add(link);
                        polygons.put(tag, polygonLinks);
                    } else {
                        if (!polygons.get(tag).contains(link)) {
                            polygons.get(tag).add(link);
                        }
                    }
                }
            }
            if (containPolygon) {
                // link は POLYGON 描画用のリンクなので通常のリンクとしては描画しない
                continue;
            }

            BranchGroup linkgroup = new BranchGroup();
            linkgroup.setCapability(BranchGroup.ALLOW_DETACH);
            LinkAppearance linkAppearance = getLinkAppearance(link);
            if (linkAppearance != null) {
                Shape3D[] shapes = createLinkShapes(link, linkAppearance);
                for (Shape3D shape : shapes) {
                    linkgroup.addChild(shape);
                }
            } else {
                Shape3D shape = createLinkShape(link);
                linkgroup.addChild(shape);
                canvasobj_to_obnode.put(shape, link);
            }
            linkGroups.put(link, linkgroup);
            map_objects.addChild(linkgroup);
        }

        // tkokada polygon
        for (String tag : polygons.keySet()) {
            MapLinkTable polygonLinks = polygons.get(tag);
            MapLink currentLink = polygonLinks.get(0);
            MapNode start = currentLink.getFrom();
            MapNode next = currentLink.getTo();
            MapNodeTable polygonNodes = new MapNodeTable();
            polygonNodes.add(start);
            while (next != start) {
                polygonNodes.add(next);
                for (MapLink link : polygonLinks)
                    if (next == link.getFrom() || next == link.getTo()) {
                        if (link != currentLink) {
                            currentLink = link;
                            if (currentLink.getFrom() != next)
                                next = currentLink.getFrom();
                            else
                                next = currentLink.getTo();
                            break;
                        }
                    }
            }
            Point3d[] ceil_vertices = new Point3d[polygonNodes.size()];
            Point3d[] floor_vertices = new Point3d[polygonNodes.size()];
            ArrayList<Point3d[]> wallVertexList = new ArrayList<Point3d[]>();
            for (int i = 0; i < polygonNodes.size(); i++) {
                Point3d[] wall_vertices = new Point3d[4];
                MapNode node = polygonNodes.get(i);
                floor_vertices[i] = new Point3d(node.getAbsoluteX(),
                    node.getAbsoluteY(),
                    ((MapPartGroup) node.getParent()).getDefaultHeight());
                if (tag.contains("STRUCTURE")) {
                    ceil_vertices[i] = new Point3d(node.getAbsoluteX(),
                            node.getAbsoluteY(), node.getHeight());
                    MapNode nnode = null;
                    if (i == polygonNodes.size() - 1) {
                        nnode = polygonNodes.get(0);
                    } else {
                        nnode = polygonNodes.get(i + 1);
                    }
                    wall_vertices[0] = new Point3d(node.getAbsoluteX(),
                            node.getAbsoluteY(), node.getHeight());
                    wall_vertices[1] = new Point3d(nnode.getAbsoluteX(),
                            nnode.getAbsoluteY(), nnode.getHeight());
                    wall_vertices[2] = new Point3d(nnode.getAbsoluteX(),
                            nnode.getAbsoluteY(),
                            ((MapPartGroup) nnode.getParent()).getDefaultHeight());
                    wall_vertices[3] = new Point3d(node.getAbsoluteX(),
                            node.getAbsoluteY(),
                            ((MapPartGroup) node.getParent()).getDefaultHeight());
                    wallVertexList.add(wall_vertices);
                    wallCeilPolygonGroup.addChild(createPolygon(wall_vertices, tag));
                }
            }
            floorPolygonGroup.addChild(createPolygon(floor_vertices, tag));
            if (tag.contains("STRUCTURE")) {
                wallCeilPolygonGroup.addChild(createPolygon(ceil_vertices, tag));
            }
        }
        wallCeilPolygonGroup.setCapability(BranchGroup.ALLOW_DETACH);
        wallCeilPolygonGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        wallCeilPolygonGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        floorPolygonGroup.setCapability(BranchGroup.ALLOW_DETACH);
        structure_group.addChild(wallCeilPolygonGroup);
        structure_group.addChild(floorPolygonGroup);

        structure_group.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        structure_group.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        structure_group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        structure_group.setCapability(BranchGroup.ALLOW_DETACH);
        map_objects.addChild(structure_group);
    }

    // 通常リンク表示用の Appearance
    protected Appearance path_appearance;

    /**
     * 通常リンク表示用の Appearance オブジェクトを生成する.
     */
    protected Appearance createPathAppearance() {
        Appearance path_appearance = new Appearance();
        TransparencyAttributes ta = new TransparencyAttributes(
                TransparencyAttributes.FASTEST, 0.75f);
        ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        path_appearance.setTransparencyAttributes(ta);

        LineAttributes la = new LineAttributes();
        /*
        LineAttributes la = new LineAttributes((float) 3.0,0,true);
        (float)線の太さ,(int)線の種類,(boolean )アンチエリアス処理をするかどうか
        */

        la.setCapability(LineAttributes.ALLOW_WIDTH_WRITE);
        path_appearance.setLineAttributes(la);
        path_appearance
                .setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE
                        | Appearance.ALLOW_LINE_ATTRIBUTES_WRITE);
        return path_appearance;
    }

    /**
     * link に振られているタグにマッチする LinkAppearance を返す.
     */
    public LinkAppearance getLinkAppearance(MapLink link) {
        LinkAppearance linkAppearance = null;
        for (Map.Entry<String, LinkAppearance> entry : linkAppearances.entrySet()) {
            if (link.hasTag(entry.getKey())) {
                linkAppearance = entry.getValue();
                break;
            }
        }
        return linkAppearance;
    }

    /**
     * リンク表示用の Shape3D オブジェクトを生成する(通常リンク用).
     */
    public Shape3D createLinkShape(MapLink link) {
        MapNode from = link.getFrom();
        MapNode to = link.getTo();
        double scale = ((MapPartGroup) link.getParent()).getScale();

        /* path links (a.k.a. normal links) */
        Point3d[] vertices = new Point3d[2];
        vertices[0] = new Point3d(from.getAbsoluteX(),
                from.getAbsoluteY(), from.getHeight() / scale);
        vertices[1] = new Point3d(to.getAbsoluteX(), to.getAbsoluteY(),
                to.getHeight() / scale);
        LineArray geometory = new LineArray(vertices.length,
                GeometryArray.COORDINATES | GeometryArray.COLOR_3);
        geometory.setCapability(GeometryArray.ALLOW_COLOR_WRITE);

        geometory.setCoordinates(0, vertices);
        Color3f color = colors_for_link(link);
        geometory.setColor(0, color);
        geometory.setColor(1, color);

        Shape3D shape = new Shape3D(geometory, path_appearance);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        return shape;
    }

    /**
     * リンク表示用の Shape3D オブジェクトを生成する(色付きリンク用).
     */
    public Shape3D[] createLinkShapes(MapLink link, LinkAppearance linkAppearance) {
        MapNode from = link.getFrom();
        MapNode to = link.getTo();
        double scale = ((MapPartGroup) link.getParent()).getScale();

        /* Use polygon for structural links */
        Point3d[] vertices = new Point3d[4];

        double x1 = from.getX();
        double x2 = to.getX();
        double y1 = from.getY();
        double y2 = to.getY();

        Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
        v1.normalize();
        Vector3d v2 = new Vector3d(0, 0, linkAppearance.widthFixed ?
                linkAppearance.widthRatio : link.width * linkAppearance.widthRatio);
        if (v2.z == 0)
            v2.z = 1.0;
        Vector3d v3 = new Vector3d();

        TransformGroup group = new TransformGroup();
        Shape3D[] shapes = new Shape3D[2];
        for (int i = 0; i < 2; i++) {
            if (i == 1)
                v3.cross(v1, v2);
            else
                v3.cross(v2, v1);

            double dx = v3.x;
            double dy = v3.y;

            vertices[0] = new Point3d(from.getAbsoluteX() + dx,
                    from.getAbsoluteY() + dy, from.getHeight() / scale);
            vertices[1] = new Point3d(from.getAbsoluteX() - dx,
                    from.getAbsoluteY() - dy, from.getHeight() / scale);
            vertices[2] = new Point3d(to.getAbsoluteX() - dx,
                    to.getAbsoluteY() - dy, to.getHeight() / scale);
            vertices[3] = new Point3d(to.getAbsoluteX() + dx,
                    to.getAbsoluteY() + dy, to.getHeight() / scale);

            QuadArray geometory = new QuadArray(vertices.length,
                    GeometryArray.COORDINATES | GeometryArray.COLOR_3);
            geometory.setCoordinates(0, vertices);

            for (int index = 0; index < 4; index++) {
                geometory.setColor(index, linkAppearance.color);
            }

            shapes[i] = new Shape3D(geometory, linkAppearance.appearance);
        }
        return shapes;
    }

    protected void setup_nodes() {
        for (MapNode node : nodes) {
            NodeAppearance nodeAppearance = getNodeAppearance(node);
            if (nodeAppearance != null) {
                BranchGroup nodeGroup = createNodeGroup(node);
                map_objects.addChild(nodeGroup);
                displayedNodeGroups.put(node, nodeGroup);
            }
        }
    }

    /**
     * ノード表示用の3Dオブジェクトを生成する.
     */
    public BranchGroup createNodeGroup(MapNode node) {
        Point2D pos = node.getAbsoluteCoordinates();
        double x = pos.getX();
        double y = pos.getY();
        double z = node.getHeight() / ((MapPartGroup)node.getParent()).getScale();

        Transform3D trans = new Transform3D();
        trans.setTranslation(new Vector3d(x, y, z));

        TransformGroup group = new TransformGroup(trans);
        NodeAppearance nodeAppearance = getNodeAppearance(node);
        // ※API ドキュメントでは Sphere(float radius, Appearance ap) となっているが、実際には直径として扱われる
        group.addChild(new Sphere((float)nodeAppearance.diameter, nodeAppearance.appearance));

        BranchGroup nodeGroup = new BranchGroup();
        nodeGroup.setCapability(BranchGroup.ALLOW_DETACH);
        nodeGroup.addChild(group);
        return nodeGroup;
    }

    /**
     * node に振られているタグにマッチする NodeAppearance を返す.
     */
    public NodeAppearance getNodeAppearance(MapNode node) {
        NodeAppearance nodeAppearance = null;
        for (Map.Entry<String, NodeAppearance> entry : nodeAppearances.entrySet()) {
            if (node.hasTag(entry.getKey())) {
                nodeAppearance = entry.getValue();
                break;
            }
        }
        return nodeAppearance;
    }

    // tkokada polygon
    // Used by show_3d_polygon option.
    private ArrayList<TransformGroup> polygonGroup =
            new ArrayList<TransformGroup>();
    private BranchGroup wallCeilPolygonGroup = new BranchGroup();
    private BranchGroup floorPolygonGroup = new BranchGroup();
    protected TransformGroup createPolygon(Point3d[] vertices, String tag) {
        int[] strip = {vertices.length};
        TriangleFanArray geom = new TriangleFanArray(vertices.length,
                GeometryArray.COORDINATES | GeometryArray.COLOR_3, strip);
        geom.setCoordinates(0, vertices);
        Color3f c = colors_for_polygon(tag);
        for (int i = 0; i < vertices.length; i++)
            geom.setColor(i, c);
        Shape3D shape = new Shape3D();
        shape.removeGeometry(0);
        shape.addGeometry(geom);
        Appearance appr = new Appearance();
        TransparencyAttributes tattr = transparency_for_polygon(tag);
        PolygonAttributes pattr = new PolygonAttributes();
        pattr.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        pattr.setCullFace(PolygonAttributes.CULL_NONE);
        appr.setTransparencyAttributes(tattr);
        appr.setPolygonAttributes(pattr);
        shape.setAppearance(appr);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        TransformGroup group = new TransformGroup();
        group.addChild(shape);
        return group;
    }

    protected void update3dPolygon() {
        if (show_3d_polygon) {
            if (structure_group.indexOfChild(wallCeilPolygonGroup) < 0) {
                structure_group.addChild(wallCeilPolygonGroup);
            }
        } else {
            int index = structure_group.indexOfChild(wallCeilPolygonGroup);
            if (index >= 0) {
                structure_group.removeChild(wallCeilPolygonGroup);
            }
        }
    }

    public void captureNextFrame(String filename) {
        canvas.catpureNextFrame(filename);
    }

    public void setShowStructure(boolean b) {
        if (b == show_structure) return;
        if (b) {
            map_objects.addChild(structure_group);
            show_structure = true;
        } else {
            structure_group.detach();
            show_structure = false;
        }
    }

    public void setIsInitialized(boolean _isInitialized) {
        isInitialized = _isInitialized;
    }

    public boolean getIsInitialized() {
        return isInitialized;
    }

    public void setScreenshotDir(String dirPath) {
        screenshotDir = dirPath;
    }

    public void setScreenshotImageType(String type) {
        screenshotImageType = type;
    }
}
