package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Dimension;
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.BadTransformException;
import javax.media.j3d.Behavior;
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
import javax.media.j3d.QuadArray;
import javax.media.j3d.Raster;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;

//import com.sun.image.codec.jpeg.ImageFormatException;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;
//import com.sun.image.codec.jpeg.JPEGEncodeParam;
//import com.sun.image.codec.jpeg.JPEGCodec;

import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;

import com.sun.j3d.utils.universe.SimpleUniverse;

public abstract class NetworkPanel3DBase extends JPanel 
    implements Serializable {
    /**
     * Basic class for 3D canvas and other controls.
     * - Place canvas
     * - Define basic colors
     * - Drawing of nodes and links
     * - menu and status bar
     * - show logo 
     */
    private static final long serialVersionUID = 6164276270221427488L;

    protected Map<Shape3D, OBNode> canvasobj_to_obnode = new HashMap<Shape3D, OBNode>();

    protected SimpleUniverse universe = null;

    private ArrayList<MapNode> nodes;
    private ArrayList<MapLink> links;

    private boolean isInitialized = false;

    /* not really black */
    static protected final Color3f BLACK = new Color3f(0.3f, 0.3f, 0.3f);
    /* true black */
    static protected final Color3f BLACK2 = new Color3f(0.0f, 0.0f, 0.0f);
    static protected final Color3f GRAY = new Color3f(0.5f, 0.5f, 0.5f);
    static protected final Color3f GRAY2 = new Color3f(0.4f, 0.5f, 0.6f);
    static protected final Color3f RED = new Color3f(1.0f, 0.0f, 0.0f);
    static protected final Color3f GREEN = new Color3f(0.0f, 1.0f, 0.0f);
    static protected final Color3f BLUE = new Color3f(0.0f, 0.0f, 1.0f);
    static protected final Color3f YELLOW = new Color3f(1.0f, 1.0f, 0.0f);
    static protected final Color3f WHITE = new Color3f(1.0f, 1.0f, 1.0f);
    static protected final Color3f PINK = new Color3f(1.0f, 0.8f, 0.8f);
    static protected final Color3f LIGHTB = new Color3f(0.8f, 0.8f, 1.0f);
    static protected final Color3f APINK = new Color3f(1.0f, 0.5f, 0.5f);
    static protected final Color3f ALIGHTB = new Color3f(0.5f, 0.5f, 1.0f);
    static protected final Color3f ARED = new Color3f(1.0f, 0.7f, 0.7f);
    static protected final Color3f PURPLE = new Color3f(1.0f, 0.0f, 1.0f);
    /* Poisonous color */
    static protected final Color3f PRED = new Color3f(0.7f, 0.0f, 0.0f);
    protected Color3f link_color = WHITE;

    BoundingSphere bounds = new BoundingSphere(new Point3d(), 20000.0);

    /* flags to control drawing */
    CheckboxMenuItem menu_item_link_gradation = null;
    protected float link_transparency = 0.5f;
    protected boolean link_transparency_changed_flag = false;

    protected boolean show_logo = true;

    protected float link_width = 1.0f;
    protected boolean link_width_changed_flag = false;
    protected boolean link_draw_density_mode = false;

    /* Canvas class
     * - show logo
     * - capture
     */
    protected class CaptureCanvas3D extends Canvas3D {
        private static final long serialVersionUID = -2962828992621609919L;

        public CaptureCanvas3D(GraphicsConfiguration arg0) {
            super(arg0);
            aist_logo = getToolkit().createImage("img/aist_logo.png");
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

            J3DGraphics2D g = getGraphics2D();
            g.setColor(Color.WHITE);
            g.drawString(message, 12, 12);
            if (show_logo) {
                int x = getWidth() - aist_logo.getWidth(null);
                int y = getHeight() - aist_logo.getHeight(null);
                g.drawImage(aist_logo, x, y, null);
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
        public  synchronized void postSwap() {
            super.postSwap();

            if (filename == null) return;

            // tkokada debug
            //int width = canvas.getSize().width;
            //int height = canvas.getSize().height;
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
            // tkokada debug
            System.err.println("\tScreen shot size: " + width + ", " + height);

            try {
                ImageIO.write(img, "jpg", new File(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
            filename = null;
        }
    };

    protected MenuBar menu_bar = null;
    protected transient CaptureCanvas3D canvas = null;
    protected JFrame parent = null;

    protected NetworkPanel3DBase(ArrayList<MapNode> _nodes,
            ArrayList<MapLink> _links,
            JFrame _parent) {
        nodes = _nodes;
        links = _links;
        parent = _parent;

        //canvas_width = 640;
        //canvas_height = 480;
        canvas_width = 800;
        canvas_height = 600;
    }
    
    protected void deserialize(ArrayList<MapNode> _nodes,
            ArrayList<MapLink> _links,
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
        menu_item_link_gradation = new CheckboxMenuItem(
                "Toggle gradation on links", false);
        menu_view.add(menu_item_link_gradation);

        Menu menu_view_link_transparency = new Menu("Link transparance");
        class ChangeLinkTransparencyActionListner implements ActionListener {
            float t;

            public ChangeLinkTransparencyActionListner(float _t) {
                t = _t;
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                link_transparency = t;
                link_transparency_changed_flag = true;
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
                link_width_changed_flag = true;
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

    protected void setupExtraContents() {
    }

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

        /* light */
        AmbientLight alight = new AmbientLight();
        alight.setInfluencingBounds(bounds);
        objRoot.addChild(alight);
        DirectionalLight dlight = new DirectionalLight(
                new Color3f(1.0f, 1.0f, 1.0f),
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
        register_map_objects();
        Transform3D map_trans = new Transform3D();

        /* centering */
        setViewPoint(calcObjectScale(map_objects));
        map_trans.setTranslation(
                new Vector3d(-point_center.x, point_center.y,
                    -point_center.z));
        map_trans.setScale(new Vector3d(1.0, -1.0, 1.0));
        map_objects.setTransform(map_trans);
        //}
    }

    private Vector3d point_min, point_max, point_center;

    /* scale to transform NetworkMapEditor coordinates to
     * 3D view coordinates */
    protected double calcObjectScale(TransformGroup objects) {
        point_center = new Vector3d(0.0, 0.0, 0.0);
        point_min = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MAX_VALUE);
        point_max = new Vector3d(Double.MIN_VALUE, Double.MIN_VALUE,
                Double.MIN_VALUE);

        Pattern betweenPattern = Pattern
                .compile(".*between\\=(\\d+\\.?\\d+?).*");
        Pattern exitPattern = Pattern.compile(".*EXIT.*");
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

            Transform3D trans = new Transform3D();
            trans.setTranslation(new Vector3d(x, y, z));

            TransformGroup node_group = null;
            try {
                node_group = new TransformGroup(trans);
            } catch (BadTransformException e) {
                node_group = new TransformGroup();
                continue;
            }
            Appearance appearance = new Appearance();
            Matcher betweenMatcher = betweenPattern
                    .matcher(node.getTagString());
            Matcher exitMatcher = exitPattern.matcher(node.getTagString());
            if (betweenMatcher.matches()) {
                final float between = Float.parseFloat(betweenMatcher.group(1)) * 0.7f + 0.3f;
                appearance.setColoringAttributes(new ColoringAttributes(
                        new Color3f(0.0f, between, between),
                        ColoringAttributes.FASTEST));
            }
            if (exitMatcher.matches()) {
                appearance.setColoringAttributes(new ColoringAttributes(GREEN,
                        ColoringAttributes.FASTEST));
            } else {
                /* do not show nodes by default */
                continue;
            }
            appearance.setTransparencyAttributes(new TransparencyAttributes(
                    TransparencyAttributes.FASTEST, 0.1f));
            node_group.addChild(new Sphere(1, appearance));
            objects.addChild(node_group);
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

    /* setting up links */
    protected class UpdateLink extends Behavior {
        WakeupOnElapsedTime won;
        public MapLink link;
        public Shape3D shape;
        public boolean disabled = false;

        public UpdateLink(MapLink _link, Shape3D _shape) {
            won = new WakeupOnElapsedTime(10);
            link = _link;
            shape = _shape;
        }

        public void initialize() {
            wakeupOn(won);
        }

        @Override
        public void processStimulus(java.util.Enumeration criteria) {
            if (!disabled) {
                update_link_geom(this);
                wakeupOn(won);
            }
        }
    }

    protected void update_link_geom(UpdateLink link_geom) {
        MapLink link = link_geom.link;

        LineArray geometory = (LineArray) link_geom.shape.getGeometry();
        Color3f c[] = colors_for_link(link);
        geometory.setColor(0, c[0]);
        geometory.setColor(1, c[1]);
        if (link_transparency_changed_flag) {
            link_transparency_changed_flag = false;
            TransparencyAttributes ta = link_geom.shape.getAppearance()
                    .getTransparencyAttributes();
            ta.setTransparency(link_transparency);
        }
        if (link_width_changed_flag) {
            link_width_changed_flag = false;
            LineAttributes la = link_geom.shape.getAppearance()
                    .getLineAttributes();
            la.setLineWidth(link_width);
        }
    }

    /* draw as line or as a polygon */
    protected boolean draw_as_line(MapLink link) {
        //if (link.hasTag("STAIR")) return false;
        //return !link.hasTag("STRUCTURE");
        
        if (link.hasTag("STRUCTURE")||
                //link.hasTag("ROAD")||
                link.hasTag("MAINROAD")||
                link.hasTag("HIGHWAY")||
                link.hasTag("RAILWAY")||
                link.hasTag("FLOOR")||
                link.hasTag("RIVER")||
                link.hasTag("BRIDGE")||
                link.hasTag("FRAME")||
                link.hasTag("ONE-WAY-POSITIVE")||
                link.hasTag("ONE-WAY-NEGATIVE")||
                link.hasTag("INVISIBLE_STAIR")) 
            return false;
        
        return true;
    }

    protected Color3f[] colors_for_link(MapLink link) {
        Color3f c[] = new Color3f[2];

        if (link.hasTag("STRUCTURE")){//
            c[0] = WHITE;
            c[1] = WHITE;
        } else if (link.hasTag("FLOOR")) {
            c[0] = GRAY;
            c[1] = GRAY;
        }else if (link_draw_density_mode) {
            Color3f c0 = new Color3f(link.getColorFromDensity());
            c[0] = c0;
            c[1] = c0;
        } else if (link.getEmergency()) {
            c[0] = GREEN;
            c[1] = GREEN;
        } else if (link.getStop()) {
            c[0] = YELLOW;
            c[1] = YELLOW;
        } else if (link.hasTag("LIFT")) {
            c[0] = YELLOW;
            c[1] = YELLOW;
        } else if (link.hasTag("RED")) {
            c[0] = RED;
            c[1] = RED;
        } else if (link.hasTag("BLUE")) {
            c[0] = BLUE;
            c[1] = BLUE;
        } else if (link.hasTag("PINK")) {
            c[0] = PINK;
            c[1] = PINK;
        } else if (link.hasTag("LIGHTB")) {
            c[0] = LIGHTB;
            c[1] = LIGHTB;
        
        }else if (link.hasTag("HIGHWAY")) {
            c[0] = WHITE;
            c[1] = WHITE;
            
        }else if (link.hasTag("MAINROAD")) {//
            c[0] = WHITE;
            c[1] = WHITE;
            
        }else if (link.hasTag("RAILWAY")) {
            //c[0] = ARED;
            //c[1] = ARED;
            
            c[0] = GRAY;
            c[1] = GRAY;
            
            //c[0] = WHITE;
            //c[1] = WHITE;
        } else if (link.hasTag("RIVER")) {//
            c[0] = BLUE;
            c[1] = BLUE;
        } else if (link.hasTag("BRIDGE")) {//
            c[0] = GRAY;
            c[1] = GRAY;
        } else if (link.hasTag("FRAME")) {
            c[0] = YELLOW;
            c[1] = YELLOW;
        }
        else if (link.hasTag("ONE-WAY-POSITIVE")||link.hasTag("ONE-WAY-NEGATIVE")) {
            c[0] = ARED;
            c[1] = ARED;
        } else if (link.hasTag("ONEWAY")) {
            c[0] = ARED;
            c[1] = ARED;
        } else if (menu_item_link_gradation.getState()) {
            double scale = ((MapPartGroup) link.getParent()).getScale();
            float f1 = (float) (link.getFrom().getHeight() / scale / point_max.z);
            float f2 = (float) (link.getTo().getHeight() / scale / point_max.z);
            Color3f c1 = new Color3f(f1, 1.0f, 1.0f - f1);
            Color3f c2 = new Color3f(f2, 1.0f, 1.0f - f2);
            c[0] = c1;
            c[1] = c2;
        } else {
            c[0] = link_color;
            c[1] = link_color;
        }

        return c;
    }

    /* Determinate link geometry
     */
    protected ArrayList<UpdateLink> link_geoms = new ArrayList<UpdateLink>();

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

        Appearance structure_appearance = new Appearance();
        structure_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                        TransparencyAttributes.FASTEST, 0.02575f));

        Appearance building_appearance = new Appearance();
        building_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                        TransparencyAttributes.FASTEST, 0.245f));

        Appearance floor_appearance = new Appearance();
        floor_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                        TransparencyAttributes.FASTEST, 0.95f));

        Appearance highway_appearance = new Appearance();
        highway_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                        TransparencyAttributes.FASTEST, 0.2525f));

        Appearance river_appearance = new Appearance();
        river_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                        TransparencyAttributes.NONE, 0.4525f));
                        //TransparencyAttributes.NONE, 0.02525f));

        Appearance bridge_appearance = new Appearance();
        bridge_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                        TransparencyAttributes.FASTEST, 0.875f));

        Appearance mainroad_appearance = new Appearance();
        mainroad_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                        TransparencyAttributes.FASTEST, 0.7525f));
                        //TransparencyAttributes.FASTEST, 0.02525f));

        Appearance oneway_appearance = new Appearance();
        oneway_appearance
                .setTransparencyAttributes(new TransparencyAttributes(
                    TransparencyAttributes.FASTEST, 0.7525f));
                    //TransparencyAttributes.FASTEST, 0.02525f));

        for (final MapLink link : links) {
            final MapNode from = link.getFrom();
            final MapNode to = link.getTo();
            final double scale = ((MapPartGroup) link.getParent()).getScale();

            if (draw_as_line(link)) {
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
                geometory.setColor(0, WHITE);
                geometory.setColor(1, WHITE);

                Shape3D shape = new Shape3D(geometory, path_appearance);
                shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
                TransformGroup linkgroup = new TransformGroup();
                linkgroup.addChild(shape);
                UpdateLink ul = new UpdateLink(link, shape);
                ul.setSchedulingBounds(bounds);
                linkgroup.addChild(ul);
                link_geoms.add(ul);

                map_objects.addChild(linkgroup);
                canvasobj_to_obnode.put(shape, link);
            } else if (link.hasTag("FLOOR")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                Vector3d v2 = new Vector3d(0, 0, link.width * 5.0);//FLOORの幅
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    //グラディエーションの設定
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    Shape3D shape = new Shape3D(geometory, floor_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                }
            }
            else if (link.hasTag("STRUCTURE")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                Vector3d v2 = new Vector3d(0, 0, link.width * 1.0 * 5.0);//STRUCTUREの幅
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    //グラディエーションの設定
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    
                    Shape3D shape = new Shape3D(geometory, building_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                }
            }
            else if (link.hasTag("HIGHWAY")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                Vector3d v2 = new Vector3d(0, 0, link.width * 1.0);//HIGHWAYの幅
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    
                    Shape3D shape = new Shape3D(geometory, highway_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                    
                }
            }
            else if (link.hasTag("MAINROAD")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                Vector3d v2 = new Vector3d(0, 0, link.width * 0.75);//ROADの幅
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    Shape3D shape = new Shape3D(geometory, mainroad_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                }
            }
            else if (link.hasTag("RAILWAY")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                Vector3d v2 = new Vector3d(0, 0, link.width * 1.0);//RAILWAYの幅
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    Shape3D shape = new Shape3D(geometory, highway_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                }
            } else if (link.hasTag("RIVER")) {    // tkokada temporal
            } else if (link.hasTag("BRIDGE")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                //Vector3d v2 = new Vector3d(0, 0, link.width * 4.0);
                // tkokada temporal
                Vector3d v2 = new Vector3d(0, 0, 20.0);
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    //Shape3D shape = new Shape3D(geometory, river_appearance);
                    // tkokada temporal
                    Shape3D shape = new Shape3D(geometory, bridge_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                }
            }
            else if (link.hasTag("FRAME")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                Vector3d v2 = new Vector3d(0, 0, link.width * 8.0);//RAILWAYの幅
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    Shape3D shape = new Shape3D(geometory, highway_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                }
            } else if (link.hasTag("ONE-WAY-POSITIVE")|| link.hasTag(
                        "ONE-WAY-NEGATIVE")) {
                /* Use polygon for structural links */
                Point3d[] vertices = new Point3d[4];

                double x1 = from.getX();
                double x2 = to.getX();
                double y1 = from.getY();
                double y2 = to.getY();

                Vector3d v1 = new Vector3d(x2 - x1, y2 - y1, 0);
                v1.normalize();
                Vector3d v2 = new Vector3d(0, 0, link.width *0.5);//RAILWAYの幅
                if (v2.z == 0)
                    v2.z = 1.0;
                Vector3d v3 = new Vector3d();

                for (int i = 0; i < 2; i++) {
                    if (i == 1)
                        v3.cross(v1, v2);
                    else
                        v3.cross(v2, v1);

                    final double dx = v3.x;
                    final double dy = v3.y;

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

                    Color3f c[] = colors_for_link(link);
                    geometory.setColor(0, c[0]);
                    geometory.setColor(1, c[1]);
                    geometory.setColor(2, c[0]);
                    geometory.setColor(3, c[1]);

                    Shape3D shape = new Shape3D(geometory, oneway_appearance);
                    TransformGroup group = new TransformGroup();
                    group.addChild(shape);

                    structure_group.addChild(group);
                }
            }
        }
        structure_group.setCapability(BranchGroup.ALLOW_DETACH);
        map_objects.addChild(structure_group);
    }

    public void captureNextFrame(String filename) {
        canvas.catpureNextFrame(filename);
    }
    
    public void setLinkDrawWith(int i) {
        link_width = i;
        link_width_changed_flag = true;
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
}
//;;; Local Variables:
//;;; mode:java
//;;; c-basic-offset:4
//;;; tab-width:4
//;;; End:
