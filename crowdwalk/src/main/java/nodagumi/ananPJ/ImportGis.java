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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;
import net.arnx.jsonic.JSON;

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
    private static final String VERSION = "Version 1.10 (April 7, 2016)";

    private static final String SHAPEFILE_SPECS_PATH = "shapefile_specs.json";
    private static final String optionsFormat = "[-h] [-p] [-s <SPEC>] [-S] [-t] [-T] [-z <NUMBER>]";
    private static final String commandLineSyntax = String.format("ImportGis %s [Shapefile]", optionsFormat);

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

    /**
     * 平面直角座標系の系番号として有効な値
     */
    private static Integer[] SYSTEM_NUMBERS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};

    private static Map<String, Object> shapefileSpecs = null;
    private static String[] shapefileSpecNames;
    private static String shapefileSpecName = null;
    private static boolean heightEnable = false;
    private static boolean outputByTokyoDatum = false;
    private static boolean outputByPlaneRectangular = false;
    private static boolean makeSimpleModel = false;
    private static boolean addCoordinateValueTag = false;
    private static int zone = -1;

    private MapContext map;
    private JMapFrame map_frame = null;
    private Settings settings;
    private String shapefilePath = null;
    private NetworkMap networkMap;

    // ShapefileSpecs
    private String description = "";
    private String coordinateSystem = null;
    private String geodeticDatum = null;
    private int scaleOfRoundOff = -1;
    private String lengthAttributeName;
    private double lengthCorrectionFactor = 1.0;
    private String widthAttributeName;
    private double widthCorrectionFactor = 1.0;
    private Object[] widthTable = null;

    public ImportGis() {
        settings = Settings.load("NetworkMapEditor.ini");
        map = new DefaultMapContext();
    }

    /**
     * 初期設定
     */
    private void initialize() {
        readShapefileSpecs();

        // Shapefile を読み込んで MapContext にセットする
        File source_file = null;
        if (shapefilePath == null) {
            source_file = selectShapefile();
        } else {
            source_file = new File(shapefilePath);
        }
        if (source_file == null) {
	    Itk.quitSafely() ;
        }
        readShapefile(source_file);
    }

    /**
     * シェープファイル仕様情報を読み込む
     */
    private void readShapefileSpecs() {
        Map<String, Object> shapefileSpec = (Map<String, Object>)shapefileSpecs.get(shapefileSpecName);
        JSON json = new JSON();
	Itk.logInfo("shapeFileSpec", json.encode(shapefileSpec, true));

        coordinateSystem = (String)shapefileSpec.get("coordinate_system");
        if (! coordinateSystem.equals("Geographic")) {
	    Itk.logError("readShapeFileSpecs",
			 "coordinate_system は現在 \"Geographic\" のみ有効です。");
	    Itk.quitByError() ;
        }

        geodeticDatum = (String)shapefileSpec.get("geodetic_datum");

        // 経緯度座標を四捨五入して小数第 scaleOfRoundOff 位に丸める
        Object object = shapefileSpec.get("round_off_coordinate_to");
        if (object != null) {
            scaleOfRoundOff = Integer.parseInt(object.toString());
        }

        // length
        Map<String, Object> lengthObjects = (Map<String, Object>)shapefileSpec.get("length");
        lengthAttributeName = (String)lengthObjects.get("attribute_name");
        object = lengthObjects.get("correction_factor");
        if (object != null) {
            lengthCorrectionFactor = Double.parseDouble(object.toString());
        }

        // width
        Map<String, Object> widthObjects = (Map<String, Object>)shapefileSpec.get("width");
        widthAttributeName = (String)widthObjects.get("attribute_name");
        object = widthObjects.get("correction_factor");
        if (object != null) {
            widthCorrectionFactor = Double.parseDouble(object.toString());
        }
        object = widthObjects.get("reference_table");
        if (object != null) {
            widthTable = ((ArrayList<Object>)object).toArray();
        }
    }

    /**
     * アプリケーションウィンドウを構築する
     */
    private JMapFrame setupMapFrame() {
        map_frame = new JMapFrame(map);
        map_frame.setTitle("Import GIS file - " + VERSION);

        //map_frame.enableLayerTable(true);
        map_frame.enableToolBar(true);
        map_frame.enableStatusBar(true);

        int w = settings.get("gis-width", 800);
        int h = settings.get("gis-height", 640);

        map_frame.setSize(w, h);
        map_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupToolbar(map_frame.getToolBar());

        return map_frame;
    }

    /**
     * ツールバーの設定
     */
    private void setupToolbar(JToolBar toolbar) {
        toolbar.addSeparator();
        toolbar.add(new JButton(new AddGisMapAction()));
        toolbar.add(new JButton(new ExportNodeLinkAction()));
    }

    /**
     * ファイル選択ダイアログでシェープファイルを選択する
     */
    private File selectShapefile() {
        File source_file = JFileDataStoreChooser.showOpenFile("shp", getGisFile(), null);
        if (source_file != null) {
            setGisFile(source_file);
	    Itk.logInfo("source file path",
			source_file.getPath().replaceAll("\\\\", "/"));
        }
        return source_file;
    }

    /**
     * シェープファイルを読み込む
     */
    private void readShapefile(File source_file) {
        /* set up map */
        try {
            FileDataStore store;
            store = FileDataStoreFinder.getDataStore(source_file);
	    Itk.logInfo("source file name", source_file.getName()) ;
            if (store == null) {
		Itk.logError("readShapeFile", "store is null!!");
		Itk.quitByError() ;
            }
            String fileName = source_file.getName();
            if (! fileName.toLowerCase().endsWith(".shp")) {
		Itk.logWarn("Irregular file name", fileName);
            }
            SimpleFeatureSource featureSource;
            featureSource = store.getFeatureSource();

            map.addLayer(featureSource, null);
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e) ;
        }
    }

    /* Helper actions 
     */
    class AddGisMapAction extends SafeAction {
        public AddGisMapAction() {
            super("Add map");
            putValue(Action.SHORT_DESCRIPTION, "Adds a gis map.");
        }

        @Override
        public void action(ActionEvent arg0) throws Throwable {
            File source_file = selectShapefile();
            if (source_file != null) {
                readShapefile(source_file);
            }
        }
    }

    /**
     * @author shnsk
     * Class that converts GIS data to NetworkMap.
     */
    class ExportNodeLinkAction extends SafeAction {

        public ExportNodeLinkAction() {
            super("Convert");
            putValue(Action.SHORT_DESCRIPTION,
                    "Exports GIS map to node-link data of NetMAS");
        }

        // 系番号選択ダイアログ
        public class ChooseDialog extends JDialog implements ActionListener, WindowListener {
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
                combo = new JComboBox<Integer>(SYSTEM_NUMBERS);
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
				Itk.dumpStackTraceOf(ex) ;
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

        public double objectToDouble(Object object) {
            double value = 0.0;
            if (object instanceof Double) {
                value = (Double)object;
            } else if (object instanceof Integer) {
                value = (Integer)object;
            } else if (object instanceof Long) {
                value = (Long)object;
            } else if (object instanceof String) {
                value = Double.valueOf((String)object);
            } else if (object instanceof BigDecimal) {
                value = ((BigDecimal)object).doubleValue();
            } else {
		Itk.logError("Illegal object", object);
		Itk.quitByError() ;
            }
            return value;
        }

        @Override
        public void action(ActionEvent arg0) throws Throwable {
            if (zone == -1) {
                zone = chooseSystemNumber();
                if (zone == -1)
                    return;
            }
            JOptionPane.showMessageDialog(map_frame, "GisToCrowdWalk Version 1.10 uses Proj4J (Java library) to transform point coordinates.", "", JOptionPane.INFORMATION_MESSAGE);

            // シェープファイルの座標系から平面直角座標系に変換する座標変換オブジェクトを生成する
            String srcEpsg = null;
            if (geodeticDatum.equals("Tokyo")) {
                srcEpsg = "EPSG:4301";  // 旧日本測地系
            } else {
                srcEpsg = "EPSG:4326";  // WGS84
            }
            String targetEpsg = null;
            if (outputByTokyoDatum) {
                targetEpsg = TOKYO_JPR_EPSG_CODES[zone];
            } else {
                targetEpsg = JGD2000_JPR_EPSG_CODES[zone];
            }
            CoordinateTransform transform = createCoordinateTransform(srcEpsg, targetEpsg);

            networkMap = new NetworkMap();
            ReferencedEnvelope ref = map.getAreaOfInterest();
            MapPartGroup root = (MapPartGroup)networkMap.getRoot();
            root.setZone(zone);
            MapPartGroup group = networkMap.createGroupNode(root);
            group.addTag("(" + roundCoordinate(ref.getMinX()) + "_"
                    + roundCoordinate(ref.getMinY()) + ")-("
                    + roundCoordinate(ref.getMaxX()) + "_"
                    + roundCoordinate(ref.getMaxY()) + ")");
            group.setWest(roundCoordinate(ref.getMinX()));
            group.setSouth(roundCoordinate(ref.getMaxY()));
            group.setEast(roundCoordinate(ref.getMaxX()));
            group.setNorth(roundCoordinate(ref.getMinY()));
            group.setZone(zone);

            double base_x = roundCoordinate(ref.getMinX());
            double scale_x =15.0* Math.min(800 / roundCoordinate(ref.getWidth()),
                    800 / roundCoordinate(ref.getHeight()));
            double base_y = roundCoordinate(ref.getMinY());
            double scale_y = scale_x;

	    Itk.logInfo("","basex: " + base_x + ", basey: " + base_y +
			", scalex: " + scale_x + ", scaley: " + scale_y);
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
                    .getDefaultHints());
            Filter filter = ff.bbox(ff.property("the_geom"), ref);
            HashMap<String, MapNode> nodes = new HashMap<String, MapNode>();
            HashMap<String, MapLink> links = new HashMap<String, MapLink>();
            MapLayer layers[] = map.getLayers();
            for (int i = 0; i < layers.length; ++i) {
                MapLayer layer = layers[i];
                SimpleFeatureSource source = (SimpleFeatureSource) layer
                    .getFeatureSource();
                SimpleFeatureCollection features = source.getFeatures(filter);
                SimpleFeatureType schema = features.getSchema();
                // for (AttributeType type : schema.getTypes()) {
                    // Itk.dbgVal("type",type);
                // }
                FeatureIterator<SimpleFeature> it = features.features();
                while (it.hasNext()) {
                    SimpleFeature feature = it.next();
                    Object the_geom = feature.getAttribute("the_geom");

                    double length = objectToDouble(feature.getAttribute(lengthAttributeName));
                    if (lengthCorrectionFactor != 1.0) {
                        length *= lengthCorrectionFactor;
                    }

                    double width = objectToDouble(feature.getAttribute(widthAttributeName));
                    if (widthCorrectionFactor != 1.0) {
                        width *= widthCorrectionFactor;
                    }
                    if (widthTable != null) {
                        width = objectToDouble(widthTable[(int)width]);
                    }
                    width = roundValue(width, 4);

                    if (makeSimpleModel) {
                        make_nodes_simple(the_geom, feature, group, nodes, links,
                            length, width, base_x, base_y, scale_x, scale_y,
                            ! outputByPlaneRectangular, transform);
                    } else {
                        make_nodes_precise(the_geom, feature, group, nodes, links,
                            length, width, base_x, base_y, scale_x, scale_y,
                            ! outputByPlaneRectangular, transform);
                    }
                }
            }
            // check the nodes that are placed same coordinate.
            ArrayList<MapNode> mapNodes = networkMap.getNodes();
	    Itk.logInfo("MapNode size", mapNodes.size());
            for (int i = 0; i < mapNodes.size(); i++) {
                for (int j = i + 1; j < mapNodes.size(); j++) {
                    if (mapNodes.get(i).ID == mapNodes.get(j).ID)
			Itk.logWarn("",
				    "MapNode " + mapNodes.get(i).ID +
				    " is used by two nodes!");
                }
            }
            ArrayList<MapLink> mapLinks = networkMap.getLinks();
	    Itk.logInfo("MapLink size", mapLinks.size());
            for (int i = 0; i < mapLinks.size(); i++) {
                for (int j = i + 1; j < mapLinks.size(); j++) {
                    if (mapLinks.get(i).ID == mapLinks.get(j).ID)
			Itk.logWarn("",
				    "MapLink " + mapLinks.get(i).ID +
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
     * 経緯度座標を四捨五入して小数第 scaleOfRoundOff 位に丸める
     */
    public double roundCoordinate(double coordinate) {
        return roundValue(coordinate, scaleOfRoundOff);
    }

    /**
     * 実数値 value を小数点以下第 scale 位で四捨五入する
     */
    public double roundValue(double value, int scale) {
        if (scale >= 0 && ! Double.isNaN(value)) {
            BigDecimal bd = new BigDecimal(String.valueOf(value));
            return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return value;
    }

    /**
     * Proj4J の座標変換オブジェクトを生成する
     */
    public CoordinateTransform createCoordinateTransform(String srcEpsgName, String targetEpsgName) {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem sourceCRS = crsFactory.createFromName(srcEpsgName);
        CoordinateReferenceSystem targetCRS = crsFactory.createFromName(targetEpsgName);
        CoordinateTransformFactory transformFactory = new CoordinateTransformFactory();
        return transformFactory.createTransform(sourceCRS, targetCRS);
    }

    /**
     * 座標変換
     */
    public ProjCoordinate transformCoordinate(CoordinateTransform transform, Coordinate c) {
        ProjCoordinate gcsPoint = new ProjCoordinate(c.x, c.y);
        ProjCoordinate pcsPoint = new ProjCoordinate();
        pcsPoint = transform.transform(gcsPoint, pcsPoint);
        return pcsPoint;
    }

    /**
     * ノード ID を小さい順に並べた文字列
     */
    private String makeNodeIdPair(MapNode node1, MapNode node2) {
        if (node1.ID.compareTo(node2.ID) < 0) {
            return node1.ID + " " + node2.ID;
        } else {
            return node2.ID + " " + node1.ID;
        }
    }

    private void make_nodes_precise(Object the_geom, SimpleFeature feature,
            MapPartGroup parent_group, HashMap<String, MapNode> nodes, HashMap<String, MapLink> links,
            double length, double width, double base_x, double base_y,
            double scale_x, double scale_y, boolean crowdwalk_coordinate, CoordinateTransform transform) {
        if (!(the_geom instanceof MultiLineString))
            return;
        MultiLineString line = (MultiLineString) the_geom;
        MapNode from = null;
        Coordinate[] points = line.getCoordinates();

        if (scaleOfRoundOff >= 0) {
            for (int index = 0; index < points.length; index++) {
                Coordinate c = points[index];
                points[index] = new Coordinate(roundCoordinate(c.x), roundCoordinate(c.y), roundCoordinate(c.z));
            }
        }

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
            ProjCoordinate coordinate = transformCoordinate(transform, c);
            double x = roundValue(coordinate.y, 4);
            double y = roundValue(coordinate.x, 4);
            double z = roundValue(c.z, 4);
            if (! heightEnable || Double.isNaN(z)) {
                z = 0.0;
            }

            Point2D point = null;
            if (crowdwalk_coordinate) {
                point = new Point2D.Double(y, -x);
            } else {
                point = new Point2D.Double(x, y);
            }

            String point_str = point.toString().replaceAll(" ", "");
            // if (j == 0) {
                // point_str = feature.getAttribute("ND1").toString();
                // Itk.dbgVal("ND1", point_str);
            // } else if (j == points.length - 1) {
                // point_str = feature.getAttribute("ND2").toString();
                // Itk.dbgVal("ND2", point_str);
            // } else{
                // Itk.dbgVal("   ", point_str);
            // }

            MapNode node = null;
            if (nodes.containsKey(point_str)) {
                node = nodes.get(point_str);
            } else {
                node = networkMap.createMapNode(parent_group, point, z);
                //node.addTag(point_str);
                if (addCoordinateValueTag) {
                    node.addTag("" + c.x + "_" + c.y);
                }
                nodes.put(point_str, node);
            }

            if (from == node) {
		Itk.logInfo("","from  === node");
            } else if (from != null) {
                if (j == 0)
		    Itk.logWarn("","\tj 0 but from is not null!");
                String nodeIdPair = makeNodeIdPair(from, node);
                if (links.containsKey(nodeIdPair)) {
		    Itk.logWarn("","Duplicate link! node:" + nodeIdPair);
                } else {
                    MapLink link = networkMap.createMapLink(parent_group, from,
                            node, roundValue(length * ratio[j - 1], 4), width);
                    links.put(nodeIdPair, link);
                }
                /*
                double dnodes = Math.sqrt(
                        Math.pow(from.getX() - node.getX(), 2.0) +
                        Math.pow(from.getY() - node.getY(), 2.0));
                Itk.dbgVal("","from " + from.ID + " to " + node.ID +
                    " length " + length + " ratio " + length * ratio[j - 1]
                    + " distance " + dnodes + " j " + j);
                */
            }
            from = node;
        }
    }

    private void make_nodes_simple(Object the_geom, SimpleFeature feature,
            MapPartGroup parent_group, HashMap<String, MapNode> nodes, HashMap<String, MapLink> links,
            double length, double width, double base_x, double base_y,
            double scale_x, double scale_y, boolean crowdwalk_coordinate, CoordinateTransform transform) {
        if (!(the_geom instanceof MultiLineString)) return;

        MultiLineString line = (MultiLineString)the_geom;
        MapNode from = null;
        Coordinate[] points = line.getCoordinates();

        if (scaleOfRoundOff >= 0) {
            for (int index = 0; index < points.length; index++) {
                Coordinate c = points[index];
                points[index] = new Coordinate(roundCoordinate(c.x), roundCoordinate(c.y), roundCoordinate(c.z));
            }
        }

        for (int j = 0; j < 2; j++) {
            if (j == 1) j = points.length - 1;
            Coordinate c = points[j];
            ProjCoordinate coordinate = transformCoordinate(transform, c);
            double x = roundValue(coordinate.y, 4);
            double y = roundValue(coordinate.x, 4);
            double z = roundValue(c.z, 4);
            if (! heightEnable || Double.isNaN(z)) {
                z = 0.0;
            }
            //Itk.dbgVal("\t","[" + c.x + ", " + c.y + "] -> [" + x + ", " + y + "]");

            Point2D point = null;
            if (crowdwalk_coordinate) {
                point = new Point2D.Double(y, -x);
            } else {
                point = new Point2D.Double(x, y);
            }

            String point_str = point.toString().replaceAll(" ", "");
            // if (j == 0) {
                // point_str = feature.getAttribute("ND1").toString();
            // } else if (j == points.length - 1) {
                // point_str = feature.getAttribute("ND2").toString();
            // }

            MapNode node = null;
            if (nodes.containsKey(point_str)) {
                node = nodes.get(point_str);
            } else {
                node = networkMap.createMapNode(parent_group, point, z);
                //node.addTag(point_str);
                if (addCoordinateValueTag) {
                    node.addTag("" + c.x + "_" + c.y);
                }
                nodes.put(point_str, node);
            }

            if (from != null) {
                String nodeIdPair = makeNodeIdPair(from, node);
                if (links.containsKey(nodeIdPair)) {
		    Itk.logWarn("", "Duplicate link! node:" + nodeIdPair);
                } else {
                    MapLink link = networkMap.createMapLink(parent_group, from,
                            node, roundValue(length, 4), width);
                    links.put(nodeIdPair, link);
                }
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

        try {
            FileOutputStream fos = new FileOutputStream(filename);

            Document doc = ItkXmlUtility.singleton.newDocument() ;
            networkMap.toDOM(doc);
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
	    Itk.dumpStackTraceOf(e) ;
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
	Itk.quitSafely() ;
    }

    /**
     * コマンドラインオプションの定義
     */
    private void defineOptions(Options options) {
        options.addOption("h", "help", false, "この使い方を表示して終了する");
        // options.addOption("H", "height-enable", false, "シェープファイルの標高情報を height 属性に反映する");
        options.addOption("p", "output-by-plane-rectangular", false, "平面直角座標系のまま出力する");
        options.addOption(OptionBuilder.withLongOpt("shapefile-spec")
            .withDescription("読み込むシェープファイルの形式を指定する\nSPEC = "
                + String.join(" | ", shapefileSpecNames))
            .hasArg().withArgName("SPEC").create("s"));
        options.addOption("S", "make-simple-model", false, "簡易モデルのマップを出力する");
        options.addOption("t", "add-coordinate-value-tag", false, "経緯度タグをノードに付加する");
        options.addOption("T", "output-by-tokyo-datum", false, "旧日本測地系で出力する");
        options.addOption(OptionBuilder.withLongOpt("zone")
            .withDescription("平面直角座標系の系番号(1～19)を指定する")
            .hasArg().withArgName("NUMBER").create("z"));
    }

    /**
     * コマンドラインオプションを解析して指定された処理を実行する
     * @param args : main メソッドの args 引数
     */
    private void parseCommandLine(String[] args, Options options) {
        try {
            CommandLine commandLine = new PosixParser().parse(options, args);
            // ヘルプ表示オプションもしくはコマンドライン引数エラー
            if (commandLine.hasOption("help") || commandLine.getArgs().length > 1) {
                printHelp(options);
		Itk.quitSafely() ;
            }

            // シェープファイルの指定あり
            if (commandLine.getArgs().length == 1) {
                shapefilePath = commandLine.getArgs()[0];
            }

            // 読み込むシェープファイルの形式
            if (commandLine.hasOption("shapefile-spec")) {
                shapefileSpecName = commandLine.getOptionValue("shapefile-spec");
                boolean found = false;
                for (String name : shapefileSpecNames) {
                    if (name.equals(shapefileSpecName)) {
                        found = true;
                    }
                }
                if (! found) {
                    printHelp(options);
		    Itk.quitByError() ;
                }
            }

            // シェープファイルが標高情報を持っていたら、その値を height 属性に反映する
            // heightEnable = commandLine.hasOption("height-enable");
            // ※標高値に関しては不明な点があるため当面サポートは見送る

            // 経緯度タグをノードに付加する
            addCoordinateValueTag = commandLine.hasOption("add-coordinate-value-tag");

            // 旧日本測地系で出力する
            outputByTokyoDatum = commandLine.hasOption("output-by-tokyo-datum");

            // 平面直角座標系のまま出力する
            outputByPlaneRectangular = commandLine.hasOption("output-by-plane-rectangular");

            // 簡易モデルのマップを出力する
            makeSimpleModel = commandLine.hasOption("make-simple-model");

            // 平面直角座標系の系番号(1～19)を指定する
            if (commandLine.hasOption("zone")) {
                String _zone = commandLine.getOptionValue("zone");
                for (Integer num : SYSTEM_NUMBERS) {
                    if (_zone.equals(num.toString())) {
                        zone = num;
                        break;
                    }
                }
                if (zone == -1) {
                    printHelp(options);
		    Itk.quitByError() ;
                }
            }
        } catch (ParseException e) {
	    Itk.logError("", e.getMessage());
            printHelp(options);
	    Itk.quitByError() ;
        } catch (Exception e) {
	    Itk.quitWithStackTrace(e) ;
        }
    }

    /**
     * コマンドラインヘルプを標準出力に表示する
     */
    public void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        int usageWidth = 7 + commandLineSyntax.length();    // "usege: " + commandLineSyntax
        formatter.setWidth(Math.max(usageWidth, 80));       // 行の折りたたみ最小幅は80桁
        formatter.printHelp(commandLineSyntax, options);
    }

    /**
     * シェープファイルのメーカー別仕様定義ファイルを読み込む
     *
     * ※カレントディレクトリに定義ファイルがあればそちらを優先する
     */
    public void loadShapefileSpecs(String filePath) {
        JSON json = new JSON(JSON.Mode.TRADITIONAL);
        try {
            File file = new File(filePath);
            if (file.exists()) {
                shapefileSpecs = json.parse(new FileReader(filePath));
            } else {
                shapefileSpecs = json.parse(getClass().getResourceAsStream("/shapefile_specs.json"));
            }
        } catch (IOException e) {
	    Itk.quitWithStackTrace(e) ;
        }
        shapefileSpecNames = shapefileSpecs.keySet().toArray(new String[0]);
    }

    /* Interface to the outside
     */
    public static void main(String[] args) {
        ImportGis importGis = new ImportGis();
        importGis.loadShapefileSpecs(SHAPEFILE_SPECS_PATH);

        Options options = new Options();
        importGis.defineOptions(options);
        importGis.parseCommandLine(args, options);

        if (shapefileSpecName == null) {
            int index = JOptionPane.showOptionDialog(null,
                    "読み込むシェープファイルの形式を選択してください",
                    "シェープファイルの形式",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, shapefileSpecNames, shapefileSpecNames[0]);
            if (index == JOptionPane.CLOSED_OPTION) {
		Itk.quitSafely() ;
            }
            shapefileSpecName = shapefileSpecNames[index];
        }

        importGis.initialize();
        JMapFrame mapFrame = importGis.setupMapFrame();
        mapFrame.setVisible(true);
    }
}
