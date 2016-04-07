package nodagumi.ananPJ;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.w3c.dom.*;

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

import com.vividsolutions.jts.geom.*;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Settings;
import nodagumi.ananPJ.NetworkMap.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Link.*;

import nodagumi.Itk.Itk;
import nodagumi.Itk.ItkXmlUtility;

public class ImportGis {
    /**
     * Shapefile 形式の地図を読み込んでモデルを作成する． 
     * 以下の2種類のデータ形式に対応
     *
     * 1. ナビゲーション道路地図2004 （shape版） アルプス社
     *
     * 読み込みの対象ファイル：
     *   RNxxxxxxA.shp 道路リンク（高速・有料道路・国道）
     *   RNxxxxxxB.shp 主要地方道、一般県道、幅員13.0m以上の道路のリンクデータ（道路区間データ）
     *   RNxxxxxxC.shp 全道路のノードデータ（道路点データ）
     *
     * 読み込みデータ：
     *   ノード１番号 nd1 文字 5 あり 半角数字
     *   ノード２番号 nd2 文字 5 あり 半角数字
     *   リンク長 lk_length 整数
     *   道路幅員区分コード width_tpcd 文字
     *     1 幅員13．0ｍ以上
     *     2 幅員 5．5ｍ以上～13．0ｍ未満
     *     3 幅員 3．0ｍ以上～ 5．5ｍ未満
     *     4 幅員 3．0ｍ未満
     *     0 未調査
     *
     * 2. 拡張版全国デジタル道路地図データベース （shape版） 2013 住友電気工業株式会社・住友電工システムソリューション株式会社
     *
     * 読み込みの対象ファイル：
     *   全道路リンクデータ(32)
     *   or
     *   全道路リンク拡張属性データ(39)
     *
     * 読み込みデータ：
     *   ノード１番号リンク番号nd1 C 5
     *   ノード２番号リンク番号nd2 C 5
     *   リンク長(計算値) length C
     *   道路幅員区分コードrdwdcd C 1
     *     1 幅員 13.0m以上
     *     2 幅員 5.5m以上~13.0m未満
     *     3 幅員 3.0m以上～ 5.5ｍ未満
     *     4 幅員 3.0ｍ未満
     *     0 未調査
     */
    private static final long serialVersionUID = 7346682140815565547L;
    private static final String VERSION = "Version 1.10 (April 7, 2016)";

    private static final boolean REVERSE_Y = false;
    // 国土地理院: 平面直角座標系（平成十四年国土交通省告示第九号）
    public static final String REFERENCE_URL = "http://www.gsi.go.jp/LAW/heimencho.html";

    /**
     * JGD2000 の平面直角座標系の系番号に対応する EPSG コード
     */
    public static String[] JGD2000_JPR_EPSG_CODES = {
        null,
        "EPSG:2443",    // 系番号 I
        "EPSG:2444",    // 系番号 II
        "EPSG:2445",    // 系番号 III
        "EPSG:2446",    // 系番号 IV
        "EPSG:2447",    // 系番号 V
        "EPSG:2448",    // 系番号 VI
        "EPSG:2449",    // 系番号 VII
        "EPSG:2450",    // 系番号 VIII
        "EPSG:2451",    // 系番号 IX
        "EPSG:2452",    // 系番号 X
        "EPSG:2453",    // 系番号 XI
        "EPSG:2454",    // 系番号 XII
        "EPSG:2455",    // 系番号 XIII
        "EPSG:2456",    // 系番号 XIV
        "EPSG:2457",    // 系番号 XV
        "EPSG:2458",    // 系番号 XVI
        "EPSG:2459",    // 系番号 XVII
        "EPSG:2460",    // 系番号 XVIII
        "EPSG:2461"     // 系番号 XIX
    };

    /**
     * Tokyo Datum の平面直角座標系の系番号に対応する EPSG コード
     */
    public static String[] TOKYO_JPR_EPSG_CODES = {
        null,
        "EPSG:30161",    // 系番号 I
        "EPSG:30162",    // 系番号 II
        "EPSG:30163",    // 系番号 III
        "EPSG:30164",    // 系番号 IV
        "EPSG:30165",    // 系番号 V
        "EPSG:30166",    // 系番号 VI
        "EPSG:30167",    // 系番号 VII
        "EPSG:30168",    // 系番号 VIII
        "EPSG:30169",    // 系番号 IX
        "EPSG:30170",    // 系番号 X
        "EPSG:30171",    // 系番号 XI
        "EPSG:30172",    // 系番号 XII
        "EPSG:30173",    // 系番号 XIII
        "EPSG:30174",    // 系番号 XIV
        "EPSG:30175",    // 系番号 XV
        "EPSG:30176",    // 系番号 XVI
        "EPSG:30177",    // 系番号 XVII
        "EPSG:30178",    // 系番号 XVIII
        "EPSG:30179"     // 系番号 XIX
    };

    private MapContext map;
    private JMapFrame map_frame = null;
    // tkokada
    //private double DEFAULT_LATITUDE = 36.00;
    //private double DEFAULT_LONGITUDE = 139.83333333;

    protected Settings settings;

    protected NetworkMap networkMap = new NetworkMap();
    // public NetworkMap getMap() { return networkMap; }

    public ImportGis()  {
        settings = Settings.load("NetworkMapEditor.ini");
        map = new DefaultMapContext();

        if (!promptAndAddMap()) {
            return;
        }

        JMapFrame mapFrame = setupMapFrame();
        mapFrame.setVisible(true);
    }

    private JMapFrame setupMapFrame() {
        map_frame = new JMapFrame(map);
        map_frame.setTitle("Import GIS file - " + VERSION);

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
        File source_file = JFileDataStoreChooser.showOpenFile("shp", getGisFile(), null);
        if (source_file == null) {
            return false;
        }
        setGisFile(source_file);

        try {
            FileDataStore store;
            store = FileDataStoreFinder.getDataStore(source_file);
            System.err.println("source file name: " + source_file.getName());
            if (store == null) {
                System.err.println("store is null!!");
                System.exit(1);
            }
            String fileName = source_file.getName();
            if (!(fileName.endsWith("A.shp") || fileName.endsWith("B.shp") || fileName.endsWith("C.shp") || fileName.endsWith("_32.shp") || fileName.endsWith("_l.shp"))) {
                System.err.println("WRNING! irregular file name: " + fileName);
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
            super("Convert");
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

        // 系番号選択ダイアログ
        public class ChooseDialog extends JDialog implements ActionListener, WindowListener {
            private Integer[] systemNumbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};

            private JComboBox<Integer> combo = null;
            private boolean canceled = false;

            public ChooseDialog(Frame owner) {
                super(owner);

                setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                addWindowListener(this);

                GridBagLayout layout = new GridBagLayout();
                GridBagConstraints gbc = new GridBagConstraints();
                getContentPane().setLayout(layout);

                JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel label = new JLabel("Choose a Zone number ");
                label.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
                inputPanel.add(label);
                combo = new JComboBox<Integer>(systemNumbers);
                combo.setSelectedIndex(8);  // デフォルト系番号は 9
                inputPanel.add(combo);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton okButton = new JButton("Ok");
                okButton.setActionCommand("ok");
                okButton.addActionListener(this);
                buttonPanel.add(okButton);

                JButton cancelButton = new JButton("Cancel");
                cancelButton.setActionCommand("cancel");
                cancelButton.addActionListener(this);
                buttonPanel.add(cancelButton);

                JEditorPane editor = new JEditorPane("text/html", "<html><a href='" + REFERENCE_URL + "'>by 平面直角座標系(平成十四年国土交通省告示第九号)</a>");
                editor.setOpaque(false);
                editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                editor.setEditable(false);
                editor.addHyperlinkListener(new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(new URI(REFERENCE_URL));
                            } catch(Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });

                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.insets = new Insets(12, 20, 0, 20);
                layout.setConstraints(inputPanel, gbc);

                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.insets = new Insets(0, 60, 0, 20);
                layout.setConstraints(editor, gbc);

                gbc.anchor = GridBagConstraints.SOUTHEAST;
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.insets = new Insets(12, 20, 12, 20);
                layout.setConstraints(buttonPanel, gbc);

                getContentPane().add(inputPanel);
                getContentPane().add(buttonPanel);
                getContentPane().add(editor);

                pack();
                setLocationRelativeTo(owner);
            }

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("cancel")) {
                    canceled = true;
                }
                setVisible(false);
            }

            public void windowActivated(WindowEvent e) {}

            public void windowClosed(WindowEvent e) {}

            public void windowClosing(WindowEvent e) {
                canceled = true;
            }

            public void windowDeactivated(WindowEvent e) {}

            public void windowDeiconified(WindowEvent e) {}

            public void windowIconified(WindowEvent e) {}

            public void windowOpened(WindowEvent e) {}

            public int getNumber() {
                return canceled ? -1 : (Integer)combo.getSelectedItem();
            }
        }

        public int chooseSystemNumber() {
            ChooseDialog dlg = new ChooseDialog(map_frame);
            dlg.setModal(true);
            dlg.setVisible(true);
            return dlg.getNumber();
        }

        @Override
        public void action(ActionEvent arg0) throws Throwable {
            int number = chooseSystemNumber();
            if (number == -1)
                return;
            int type = JOptionPane.showConfirmDialog(map_frame,
                "World Geodetic System(Y) or Tokyo Datum(N) ?", "",
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (type == JOptionPane.CANCEL_OPTION)
                return;
            int make_precise = JOptionPane.showConfirmDialog(map_frame,
                    "Make precise model?", "",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (make_precise == JOptionPane.CANCEL_OPTION)
                return;
            int crowdwalk_coordinate = JOptionPane.showConfirmDialog(map_frame,
                "CrowdWalk Coordinate(Y) or Plane Rectangular Coordinate(N) ?", "",
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (crowdwalk_coordinate == JOptionPane.CANCEL_OPTION)
                return;
            JOptionPane.showMessageDialog(map_frame, "GisToCrowdWalk Version 1.10 uses Proj4J (Java library) to transform point coordinates.", "", JOptionPane.INFORMATION_MESSAGE);

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

                    // ナビゲーション道路地図2004 (shape版) アルプス社の場合
                    Object lengthObject = feature.getAttribute("LK_LENGTH");
                    if (lengthObject == null) {
                        // 拡張版全国デジタル道路地図データベース (shape版) 2013 住友電工の場合
                        lengthObject = feature.getAttribute("length");
                        if (lengthObject == null) {
                            // MAPPLEルーティングデータ（SHAPE版） 昭文社の場合
                            lengthObject = feature.getAttribute("link_len");
                        }
                    }
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
                        System.err.println("Illegal lengthObject: " + lengthObject);
                        System.exit(1);
                    }

                    // ナビゲーション道路地図2004 (shape版) アルプス社の場合
                    double width;
                    Object rdwdcdObject = feature.getAttribute("WIDTH_TPCD");
                    if (rdwdcdObject == null) {
                        // 拡張版全国デジタル道路地図データベース (shape版) 2013 住友電工の場合
                        rdwdcdObject = feature.getAttribute("rdwdcd");
                    }
                    if (rdwdcdObject == null) {
                        // MAPPLEルーティングデータ（SHAPE版） 昭文社の場合
                        rdwdcdObject = feature.getAttribute("width");
                        width = (double)(Long)rdwdcdObject / 10.0;
                    } else {
                        int tpcd = Integer.parseInt((String)rdwdcdObject);
                        width = width_array[tpcd];
                    }

                    if (make_precise == JOptionPane.YES_OPTION) {
                        make_nodes_precise(the_geom, feature, group, nodes,
                            length, width, base_x, base_y, scale_x, scale_y,
                            crowdwalk_coordinate == JOptionPane.YES_OPTION, number, type == JOptionPane.YES_OPTION);
                    } else {
                        make_nodes_simple(the_geom, feature, group, nodes,
                            length, width, base_x, base_y, scale_x, scale_y,
                            crowdwalk_coordinate == JOptionPane.YES_OPTION, number, type == JOptionPane.YES_OPTION);
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

    /**
     * 実数値 value を小数点以下第 scale 位で四捨五入する
     */
    public static double roundValue(double value, int scale) {
        if (scale >= 0 && ! Double.isNaN(value)) {
            BigDecimal bd = new BigDecimal(String.valueOf(value));
            return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return value;
    }

    /**
     * Proj4J の座標変換オブジェクトを生成する
     */
    public static CoordinateTransform createCoordinateTransform(String srcEpsgName, String targetEpsgName) {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem sourceCRS = crsFactory.createFromName(srcEpsgName);
        CoordinateReferenceSystem targetCRS = crsFactory.createFromName(targetEpsgName);
        CoordinateTransformFactory transformFactory = new CoordinateTransformFactory();
        return transformFactory.createTransform(sourceCRS, targetCRS);
    }

    /**
     * 座標変換
     */
    public static ProjCoordinate transformCoordinate(CoordinateTransform transform, Coordinate c) {
        ProjCoordinate gcsPoint = new ProjCoordinate(c.x, c.y);
        ProjCoordinate pcsPoint = new ProjCoordinate();
        pcsPoint = transform.transform(gcsPoint, pcsPoint);
        return pcsPoint;
    }

    private void make_nodes_precise(Object the_geom, SimpleFeature feature,
            MapPartGroup parent_group, HashMap<String, MapNode> nodes,
            double length, double width, double base_x, double base_y,
            double scale_x, double scale_y, boolean crowdwalk_coordinate, int number, boolean type) {
        if (!(the_geom instanceof MultiLineString))
            return;
        MultiLineString line = (MultiLineString) the_geom;
        MapNode from = null;
        Coordinate[] points = line.getCoordinates();

        /* two-pass, first get ratio of each segments */
        double[] ratio = new double[points.length - 1];

        String targetEpsg = null;
        if (type) {     // 世界測地系
            targetEpsg = JGD2000_JPR_EPSG_CODES[number];
        } else {
            targetEpsg = TOKYO_JPR_EPSG_CODES[number];
        }

        // 旧日本測地系の地理座標系から targetEpsg の平面直角座標系に変換する
        CoordinateTransform transform = createCoordinateTransform("EPSG:4301", targetEpsg);

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
            ProjCoordinate coordinate = transformCoordinate(transform, c);
            double x = roundValue(coordinate.y, 4);
            double y = roundValue(coordinate.x, 4);
            //System.err.println("\t[" + c.x + ", " + c.y + "] -> [" + x + ", " + y + "]");
            if (REVERSE_Y)
                y *= -1.0;

            Point2D point = null;
            if (crowdwalk_coordinate) {
                point = new Point2D.Double(y, -x);
            } else {
                point = new Point2D.Double(x, y);
            }

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
                //node.addTag("" + c.x + "_" + c.y);
                nodes.put(point_str, node);
            }

            if (from == node) {
                System.err.println("from  === node");
            } else if (from != null) {
                if (j == 0)
                    System.err.println("\tj 0 but from is not null!");
                networkMap.createMapLink(parent_group, from, node,
                        roundValue(length * ratio[j - 1], 4), width);
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
            double scale_x, double scale_y, boolean crowdwalk_coordinate, int number, boolean type) {
        if (!(the_geom instanceof MultiLineString)) return;

        MultiLineString line = (MultiLineString)the_geom;
        MapNode from = null;
        Coordinate[] points = line.getCoordinates();

        String targetEpsg = null;
        if (type) {     // 世界測地系
            targetEpsg = JGD2000_JPR_EPSG_CODES[number];
        } else {
            targetEpsg = TOKYO_JPR_EPSG_CODES[number];
        }

        // 旧日本測地系の地理座標系から targetEpsg の平面直角座標系に変換する
        CoordinateTransform transform = createCoordinateTransform("EPSG:4301", targetEpsg);

        for (int j = 0; j < 2; j++) {
            if (j == 1) j = points.length - 1;
            Coordinate c = points[j];
            ProjCoordinate coordinate = transformCoordinate(transform, c);
            double x = roundValue(coordinate.y, 4);
            double y = roundValue(coordinate.x, 4);
            //System.err.println("\t[" + c.x + ", " + c.y + "] -> [" + x + ", " + y + "]");
            if (REVERSE_Y)
                y *= -1.0;

            Point2D point = null;
            if (crowdwalk_coordinate) {
                point = new Point2D.Double(y, -x);
            } else {
                point = new Point2D.Double(x, y);
            }

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
                //node.addTag(point_str);
                nodes.put(point_str, node);
            }

            if (from != null) {
                networkMap.createMapLink(parent_group, from, node, roundValue(length, 4),
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
        // networkMap.prepareForSave();

        try {
            FileOutputStream fos = new FileOutputStream(filename);

            // DaRuMaClient daruma_client = new DaRuMaClient();
            // Document doc = daruma_client.newDocument();
            Document doc = ItkXmlUtility.singleton.newDocument() ;
            networkMap.toDOM(doc);
            // boolean result = daruma_client.docToStream(doc, fos);
            boolean result = ItkXmlUtility.singleton.docToStream(doc, fos);
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
