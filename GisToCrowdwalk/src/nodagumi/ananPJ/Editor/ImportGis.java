package nodagumi.ananPJ.Editor;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.geotools.data.*;
import org.geotools.data.simple.*;
import org.geotools.factory.*;
import org.geotools.feature.*;
import org.geotools.geometry.jts.*;
import org.geotools.map.*;
import org.geotools.swing.*;
import org.geotools.swing.action.*;
import org.geotools.swing.data.*;
import org.opengis.feature.simple.*;
import org.opengis.feature.type.*;
import org.opengis.filter.*;
import org.w3c.dom.*;

import com.vividsolutions.jts.geom.*;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Settings;
import nodagumi.ananPJ.NetworkParts.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.network.*;


public class ImportGis  {
    /**
     * Shapefile 形式の地図を読み込んでモデルを作成する． 
     */
    private static final long serialVersionUID = 7346682140815565547L;

    private static final boolean REVERSE_Y = true;

    private MapContext map;
    private JMapFrame map_frame = null;
    // tkokada
    private double DEFAULT_LATITUDE = 36.00;
    private double DEFAULT_LONGITUDE = 139.83333333;
    private Ruby ruby = null;

    protected Settings settings;

    protected NetworkMap networkMap = new NetworkMap();
    public NetworkMap getMap() { return networkMap; }

    public ImportGis()  {
        settings = Settings.load("NetworkMapEditor.ini");
        map = new DefaultMapContext();

        java.util.List<String> loadpath = new ArrayList<String>();
        loadpath.add("lib");
        ruby = JavaEmbedUtils.initialize(loadpath);
        if (!promptAndAddMap()) {
            return;
        }

        JMapFrame mapFrame = setupMapFrame();
        mapFrame.setVisible(true);
    }

    private JMapFrame setupMapFrame() {
        map_frame = new JMapFrame(map);
        map_frame.setTitle("Import GIS file");

        //map_frame.enableLayerTable(true);
        map_frame.enableToolBar(true);
        map_frame.enableStatusBar(true);

        int w = settings.get("gis-width", 800);
        int h = settings.get("gis-height", 640);

        map_frame.setSize(w, h);
        map_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setupToolbar(map_frame.getToolBar());

        return map_frame;
    }

    private void setupToolbar(JToolBar toolbar) {
        toolbar.addSeparator();
        toolbar.add(new JButton(new AddGisMapAction()));
        toolbar.add(new JButton(new ExportNodeLinkAction(networkMap)));
    }

    private boolean promptAndAddMap() {
        /* set up map */
        File source_file = JFileDataStoreChooser.showOpenFile("shp",
                getGisFile(), null);
        if (source_file == null) {
            return false;
        }
        setGisFile(source_file);

        try {
            FileDataStore store;
            store = FileDataStoreFinder.getDataStore(source_file);
            System.err.println("source file name: " + source_file.getName());
            if (store == null) {
                System.err.println("store  is null!!");
                System.exit(1);
            }
            SimpleFeatureSource featureSource;
            featureSource = store.getFeatureSource();

            map.addLayer(featureSource, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /* Helper actions 
     */
    class AddGisMapAction extends SafeAction {
        private static final long serialVersionUID = -7174814927091419973L;
        public AddGisMapAction() {
            super("Add map");
            putValue(Action.SHORT_DESCRIPTION, "Adds a gis map.");
        }

        @Override
        public void action(ActionEvent arg0) throws Throwable {
            promptAndAddMap();
        }
    }

    /**
     * @author shnsk
     * Class that converts GIS data to NetworkMap.
     */
    class ExportNodeLinkAction extends SafeAction {

        private static final long serialVersionUID = -4292159606360586326L;

        private NetworkMap exportNetworkMap;

        public ExportNodeLinkAction(NetworkMap _exportNetworkMap) {
            super("to NetMAS");
            putValue(Action.SHORT_DESCRIPTION,
                    "Exports GIS map to node-link data of NetMAS");
            exportNetworkMap = _exportNetworkMap;
        }

        final double[] width_array = new double[] {
            1.0,
            14.0,
            9.0,
            4.0,
            2.5
        };

        @Override
        public void action(ActionEvent arg0) throws Throwable {
            int make_precise = JOptionPane.showConfirmDialog(map_frame,
                    "Make precise model?", "",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (make_precise == JOptionPane.CANCEL_OPTION)
                return;
            ReferencedEnvelope ref = map.getAreaOfInterest();
            MapPartGroup group = exportNetworkMap.createGroupNode((MapPartGroup)
                    exportNetworkMap.getRoot());
            group.addTag("(" + ref.getMinX() + "_" 
                    + ref.getMinY() + ")-(" 
                    + ref.getMaxX() + "_" 
                    + ref.getMaxY() + ")");
            group.setWest(ref.getMinX());
            group.setSouth(ref.getMaxY());
            group.setEast(ref.getMaxX());
            group.setNorth(ref.getMinY());

            double base_x = ref.getMinX();
            double scale_x =15.0* Math.min(800 / ref.getWidth(),
                    800 / ref.getHeight());
            double base_y;
            double scale_y;
            if (ImportGis.REVERSE_Y) {
                base_y = ref.getMaxY();
                scale_y = - scale_x;
            } else {
                base_y = ref.getMinY();
                scale_y = scale_x;
            }

            System.err.println("basex: " + base_x + ", basey: " + base_y +
                    ", scalex: " + scale_x + ", scaley: " + scale_y);
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
                    .getDefaultHints());
            Filter filter = ff.bbox(ff.property("the_geom"), ref);
            HashMap<String, MapNode> nodes = new HashMap<String, MapNode>();
            MapLayer layers[] = map.getLayers();
            for (int i = 0; i < layers.length; ++i) {
                MapLayer layer = layers[i];
                SimpleFeatureSource source = (SimpleFeatureSource) layer
                    .getFeatureSource();
                SimpleFeatureCollection features = source.getFeatures(filter);
                SimpleFeatureType schema = features.getSchema();
                // for (AttributeType type : schema.getTypes()) {
                    // System.err.println(type);
                // }
                FeatureIterator<SimpleFeature> it = features.features();
                while (it.hasNext()) {
                    SimpleFeature feature = it.next();
                    Object the_geom = feature.getAttribute("the_geom");
                    // 2012.11.14 tkokada reviced!
                    // lengthObject has two types: Double or Long
                    //double length = (Long)(feature.getAttribute("LK_LENGTH"));
                    Object lengthObject = feature.getAttribute("LK_LENGTH");
                    double length = 0.0;
                    if (lengthObject instanceof Double) {
                        length = (Double) lengthObject;
                    } else if (lengthObject instanceof Integer) {
                        length = (Integer) lengthObject;
                    } else if (lengthObject instanceof Long) {
                        length = (Long) lengthObject;
                    } else if (lengthObject instanceof String) {
                        length = Double.valueOf((String) lengthObject);
                    } else {
                        //System.err.println("");
                        System.exit(1);
                    }
                    int tpcd = Integer.parseInt(((String) (feature
                                    .getAttribute("WIDTH_TPCD"))));
                    double width = width_array[tpcd];
                    if (make_precise == JOptionPane.YES_OPTION) {
                        make_nodes_precise(the_geom, feature, group, nodes,
                                length, width, base_x, base_y, scale_x,
                                scale_y);
                    } else {
                        make_nodes_simple(the_geom, feature, group, nodes,
                                length, width, base_x, base_y, scale_x,
                                scale_y);
                    }
                }
            }
            // check the nodes that are placed same coordinate.
            ArrayList<MapNode> mapNodes = exportNetworkMap.getNodes();
            System.err.println("MapNode size " + mapNodes.size());
            for (int i = 0; i < mapNodes.size(); i++) {
                for (int j = i + 1; j < mapNodes.size(); j++) {
                    if (mapNodes.get(i).ID == mapNodes.get(j).ID)
                        System.err.println("MapNode " + mapNodes.get(i).ID +
                                " is used by two nodes!");
                }
            }
            ArrayList<MapLink> mapLinks = exportNetworkMap.getLinks();
            System.err.println("MapLink size " + mapLinks.size());
            for (int i = 0; i < mapLinks.size(); i++) {
                for (int j = i + 1; j < mapLinks.size(); j++) {
                    if (mapLinks.get(i).ID == mapLinks.get(j).ID)
                        System.err.println("MapLink " + mapLinks.get(i).ID +
                                " is used by two links!");
                }
            }
            if (save_map()) {
                map.dispose();
                map_frame.dispose();
                quit();
            }
        }
    }

    private void make_nodes_precise(Object the_geom, SimpleFeature feature,
            MapPartGroup parent_group, HashMap<String, MapNode> nodes,
            double length, double width, double base_x, double base_y,
            double scale_x, double scale_y) {
        if (!(the_geom instanceof MultiLineString))
            return;
        MultiLineString line = (MultiLineString) the_geom;
        MapNode from = null;
        Coordinate[] points = line.getCoordinates();

        /* two-pass, first get ratio of each segments */
        double[] ratio = new double[points.length - 1];

        Coordinate last_c = null;
        double total_d = 0.0;
        for (int j = 0; j < points.length; j++) {
            Coordinate c = points[j];
            if (last_c != null) {
                double d = c.distance(last_c);
                ratio[j - 1] = d;
                total_d += d;
            }
            last_c = c;
        }
        for (int j = 0; j < points.length - 1; j++) {
            ratio[j] /= total_d;
        }

        /* two-pass, second actually make links */
        for (int j = 0; j < points.length; j++) {
            Coordinate c = points[j];
            // double x = (c.x - base_x) * scale_x;
            // double y = (c.y - base_y) * scale_y;
            double x = 0.0 , y = 0.0, m = 0.0;
            try {
                ruby.evalScriptlet("require 'gtoc'");
                ruby.evalScriptlet("require 'gtoc'");
                String caller = "Geographic::gtoc(latitude: " + c.y + ", longitude: " + c.x + ", number: 9, default_latitude: " + DEFAULT_LATITUDE + ", default_longitude: " + DEFAULT_LONGITUDE + ", type: \"japan\")";
                IRubyObject out = ruby.evalScriptlet(caller);
                RubyHash h = out.convertToHash();
                for (Object key : h.keySet()) {
                    // System.err.println("  key: " + key.toString() + ", value: " + h.get(key));
                    String keystring = key.toString();
                    if (keystring.equals("x")) {
                        x = (Double) (h.get(key));
                    } else if (keystring.equals("y")) {
                        y = (Double) (h.get(key));
                    } else if (keystring.equals("m")) {
                        m = (Double) (h.get(key));
                    }
                }
                // System.err.println("\t[" + c.x + ", " + c.y + "] -> [" + x + ", " + y + "]");
            } catch (java.lang.NullPointerException npe) {
                System.err.println("\tMap number or default latitude and " +
                        "longitude are too far from the coordinate.");
                System.err.println("\t\tc.x: " + c.x + ", c.y: " + c.y);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                JavaEmbedUtils.terminate(ruby);
            }
            if (REVERSE_Y)
                y *= -1.0;
            Point2D point = new Point2D.Double(x, y);

            // 2012.11.13 tkokada reviced.
            //String point_str = point.toString();
            String point_str = removeSpace(point.toString());
            // if (j == 0) {
                // point_str = feature.getAttribute("ND1").toString();
                // System.err.println("\tND1: " + point_str);
            // } else if (j == points.length - 1) {
                // point_str = feature.getAttribute("ND2").toString();
                // System.err.println("\tND2: " + point_str);
            // } else{
                // System.err.println("\t   : " + point_str);
            // }

            MapNode node = null;
            if (nodes.containsKey(point_str)) {
                node = nodes.get(point_str);
            } else {
                node = networkMap.createMapNode(parent_group, point, 0.0);
                //node.addTag(point_str);
                nodes.put(point_str, node);
            }

            if (from == node) {
                System.err.println("from  === node");
            } else if (from != null) {
                if (j == 0)
                    System.err.println("\tj 0 but from is not null!");
                networkMap.createMapLink(parent_group, from, node,
                        length * ratio[j - 1], width);
                /*
                double dnodes = Math.sqrt(
                        Math.pow(from.getX() - node.getX(), 2.0) +
                        Math.pow(from.getY() - node.getY(), 2.0));
                System.err.println("from " + from.ID + " to " + node.ID +
                    " length " + length + " ratio " + length * ratio[j - 1]
                    + " distance " + dnodes + " j " + j);
                */
            }
            from = node;
        }
    }

    private void make_nodes_simple(Object the_geom, SimpleFeature feature,
            MapPartGroup parent_group, HashMap<String, MapNode> nodes,
            double length, double width, double base_x, double base_y,
            double scale_x, double scale_y) {
        if (!(the_geom instanceof MultiLineString)) return;

        MultiLineString line = (MultiLineString)the_geom;
        MapNode from = null;
        Coordinate[] points = line.getCoordinates();

        for (int j = 0; j < 2; j++) {
            if (j == 1) j = points.length - 1;
            Coordinate c = points[j];
            // double x = (c.x - base_x) * scale_x;
            // double y = (c.y - base_y) * scale_y;
            double x = 0.0 , y = 0.0, m = 0.0;
            try {
                ruby.evalScriptlet("require 'gtoc'");
                ruby.evalScriptlet("require 'gtoc'");
                String caller = "Geographic::gtoc(latitude: " + c.y + ", longitude: " + c.x + ", number: 9, default_latitude: " + DEFAULT_LATITUDE + ", default_longitude: " + DEFAULT_LONGITUDE + ", type: \"japan\")";
                IRubyObject out = ruby.evalScriptlet(caller);
                RubyHash h = out.convertToHash();
                for (Object key : h.keySet()) {
                    String keystring = key.toString();
                    if (keystring.equals("x")) {
                        x = (Double) (h.get(key));
                    } else if (keystring.equals("y")) {
                        y = (Double) (h.get(key));
                    } else if (keystring.equals("m")) {
                        m = (Double) (h.get(key));
                    }
                }
                // System.err.println("\t[" + c.x + ", " + c.y + "] -> [" + x + ", " + y + "]");
            } catch (java.lang.NullPointerException npe) {
                System.err.println("\tMap number or default latitude and " +
                        "longitude are too far from the coordinate.");
                System.err.println("\t\tc.x: " + c.x + ", c.y: " + c.y);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                JavaEmbedUtils.terminate(ruby);
            }
            if (REVERSE_Y)
                y *= -1.0;
            Point2D point = new Point2D.Double(x, y);

            // 2012.11.13 tkokada reviced.
            //String point_str = point.toString();
            String point_str = removeSpace(point.toString());
            // if (j == 0) {
                // point_str = feature.getAttribute("ND1").toString();
            // } else if (j == points.length - 1) {
                // point_str = feature.getAttribute("ND2").toString();
            // }

            MapNode node = null; 
            if (nodes.containsKey(point_str)) {
                node = nodes.get(point_str);
            } else {
                node = networkMap.createMapNode(parent_group, point, 0.0);
                node.addTag(point_str);
                nodes.put(point_str, node);
            }

            if (from != null) {
                networkMap.createMapLink(parent_group, from, node, length,
                        width);
            }
            from = node;
        }
    }

    private boolean save_map() {
        FileDialog fd = new FileDialog(map_frame, "Export map", FileDialog.SAVE);
        fd.setFile(settings.get("gis-output-filename", ""));
        fd.setDirectory(settings.get("inputdir", ""));
        fd.setVisible (true);

        if (fd.getFile() == null) return false;

        String filename = fd.getDirectory() + fd.getFile();
        settings.put("gis-output-filename", fd.getFile());
        networkMap.prepareForSave();

        try {
            FileOutputStream fos = new FileOutputStream(filename);

            DaRuMaClient daruma_client = new DaRuMaClient();
            Document doc = daruma_client.newDocument();
            networkMap.toDOM(doc);
            boolean result = daruma_client.docToStream(doc, fos);
            if (!result) {
                JOptionPane.showMessageDialog(map_frame,
                        "Could no save to:\n" + filename
                        + "\nAn error occured while actually writing to file.",
                        "Save failed",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(map_frame,
                    "Could no save to:\n" + filename,
                    "Save failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }


    public File getGisFile() {
        return new File(settings.get("gisfile", "."));
    }

    public void setGisFile(File file) {
        settings.put("gisfile", file.toString());
    }

    private void quit() {
        settings.put("gis-width", map_frame.getWidth());
        settings.put("gis-height", map_frame.getHeight());
        Settings.save();
        System.exit(0);
    }


    /** Remove white spaces from a inputted string.
     * @author tkokada
     * @param input a string.
     * @return a string that white spaces are removed.
     */
    private static String removeSpace(String input) {
        String parsedString = new String();
        for (String splited : input.split(" ")) {
            parsedString += splited;
        }

        return parsedString;
    }

    /* Interface to the outside
     */
    public static void main(String[] args) {
        new ImportGis();
    }
}
