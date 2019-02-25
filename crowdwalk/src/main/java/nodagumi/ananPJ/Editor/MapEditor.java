package nodagumi.ananPJ.Editor;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Affine;

import org.w3c.dom.Document;

import org.osgeo.proj4j.CoordinateTransform;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.NetworkMap.Polygon.OuterBoundary;
import nodagumi.ananPJ.NetworkMap.Polygon.InnerBoundary;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.navigation.Dijkstra;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.Settings;

import nodagumi.Itk.Itk;
import nodagumi.Itk.Itk.LogLevel;
import nodagumi.Itk.ItkXmlUtility;

/**
 * マップエディタのメイン処理
 */
public class MapEditor {
    /**
     * テキストの描画位置
     */
    public static enum TextPosition { CENTER, UPPER, LOWER, LEFT, RIGHT }

    /**
     * ウィンドウフレーム
     */
    private EditorFrameFx frame;

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
    private Random random = new Random();

    /**
     * 設定ファイルの取りまとめ。
     */
    private SetupFileInfo setupFileInfo = null;

    /**
     * コマンドラインで指定された fallback 設定
     */
    private ArrayList<String> commandLineFallbacks = null;

    /**
     * マップファイルを読み書きするディレクトリ
     */
    private File dir = new File(".");

    /**
     * 地図データ。
     */
    private NetworkMap networkMap;

    /**
     * 地図データの最低標高
     */
    private double minHeight;

    /**
     * 地図データの最大標高
     */
    private double maxHeight;

    /**
     * グループごとの背景画像
     */
    private HashMap<MapPartGroup, Image> backgroundImages = new HashMap();

    /**
     * 地理院タイル
     */
    private ArrayList<GsiTile> gsiTiles = new ArrayList();

    /**
     * JavaFX 用の地理院タイル画像
     */
    private HashMap<GsiTile, Image> gsiTileImages = new HashMap();

    /**
     * 編集中の Group
     */
    private MapPartGroup currentGroup = null; 

    /**
     * Group zone または Group tag が正規化のために修正された
     */
    private boolean normalized = false;

    /**
     * 編集コマンドの履歴管理用変数
     */
    private ArrayList<EditCommandBase> commandHistory = new ArrayList();
    private int historyIndex;
    private boolean isFirstCommand = false;
    private EditCommandBase lastCommand = null;

    /**
     * 表示更新管理用の変数
     */
    private boolean groupTagChanged = false;
    private boolean nodeParamChanged = false;
    private boolean linkParamChanged = false;
    private boolean areaParamChanged = false;
    private boolean polygonParamChanged = false;
    private boolean nodeTagChanged = false;
    private boolean linkTagChanged = false;
    private boolean areaTagChanged = false;
    private boolean polygonTagChanged = false;
    private boolean groupVolumeChanged = false;
    private boolean nodeVolumeChanged = false;
    private boolean linkVolumeChanged = false;
    private boolean areaVolumeChanged = false;
    private boolean polygonVolumeChanged = false;

    /**
     * コンストラクタ
     */
    public MapEditor(Settings settings) {
        JFXPanel fxPanel = new JFXPanel();  // JavaFX アプリケーションスレッドを起動するために必要
        this.settings = settings;
    }

    /**
     * 初期設定
     */
    private void init() {
        // 初期選択グループのセット
        initCurrentGroup();

        updateHeight();
    }

    /**
     * マップデータを初期化する
     */
    public void initNetworkMap() {
        networkMap = new NetworkMap();
        backgroundImages.clear();
        gsiTiles.clear();
        gsiTileImages.clear();
        initCommandHistory();
        normalized = false;
    }

    /**
     * カレントグループを初期化する
     */
    public void initCurrentGroup() {
        currentGroup = (MapPartGroup)networkMap.getRoot();
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group != currentGroup && group.getTags().size() != 0) {
                currentGroup = group;
                break;
            }
        }
    }

    /**
     * マップデータを読み込む
     */
    public boolean loadNetworkMap() {
        String fileName = getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            Itk.logError("Map file name is empty.");
            return false;
        }
        initNetworkMap();

        // シミュレーション結果の同一性を確保するため
        // TODO: これは既に不要かもしれない
        random.setSeed(properties.getRandseed());

        Document doc = null;
        try {
            doc = ItkXmlUtility.singleton.streamToDoc(new FileInputStream(fileName));
            if (doc == null) {
                Itk.logError("Could not read map.", fileName);
                return false;
            }
        } catch (IOException e) {
            Itk.logError("Can't Open Map File", fileName);
            return false;
        }
        if (doc.getChildNodes() == null) {
            Itk.logError("Invalid inputted DOM object.", fileName);
            return false;
        }

        if (! networkMap.fromDOM(doc)) {
            return false;
        }
        // tag を持たない Group があれば適当な名前で tag を付加しておく
        appendGroupTag();
        // Group の zone を正規化する(networkMap.getGroups() を使用しているため appendGroupTag() の後に呼ぶこと)
        normalizeZone();

        if (! loadBackgroundImage()) {
            return false;
        }
        if (! loadGsiTiles()) {
            return false;
        }

        Itk.logInfo("Load Map File", fileName);

        return true;
    }

    /**
     * tag を持たない Group があれば適当な名前で tag を付加しておく
     */
    private void appendGroupTag() {
        ArrayList<String> names = new ArrayList();
        for (MapPartGroup group : networkMap.getGroups()) {
            names.add(group.getTagString());
        }

        int n = 1;
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();
        for (OBNode node : networkMap.getOBCollection()) {
            if (node.getNodeType() != OBNode.NType.GROUP) {
                continue;
            }
            MapPartGroup group = (MapPartGroup)node;
            if (group.getTagString().isEmpty()) {
                group.allTagsClear();
                if (group == root) {
                    group.addTag("root");
                    normalized = true;
                } else {
                    String name = "GROUP" + n;
                    while (names.contains(name)) {
                        n++;
                        name = "GROUP" + n;
                    }
                    group.addTag(name);
                    names.add(name);
                    normalized = true;
                }
            }
        }
    }

    /**
     * Group の zone を正規化する
     */
    private void normalizeZone() {
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();

        // 異常な値があれば 0 にする
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group.getZone() != 0 && ! GsiTile.isCorrectZone(group.getZone())) {
                Itk.logWarn("Group " + group.getTagString(), "delete zone value: " + group.getZone());
                group.setZone(0);
                normalized = true;
            }
        }

        // サブグループ側で設定されている場合は root にも設定する
        if (root.getZone() == 0) {
            HashSet<Integer> zones = new HashSet();
            for (MapPartGroup group : networkMap.getGroups()) {
                if (group.getZone() != 0) {
                    zones.add(new Integer(group.getZone()));
                }
            }
            switch (zones.size()) {
            case 0:     // 全グループの zone が 0
                return;
            case 1:     // サブグループで zone が設定
                zones.forEach(zone -> {
                    Itk.logWarn("Group " + root.getTagString(), "set zone value: " + zone);
                    root.setZone(zone.intValue());
                    normalized = true;
                });
                break;
            default:    // サブグループで異なる複数の zone が設定
                break;
            }
        }

        // サブグループの zone はすべて 0 にする
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group != root && group.getZone() != 0) {
                group.setZone(0);
                normalized = true;
            }
        }
    }

    /**
     * マップデータを保存する
     */
    public boolean saveMap() {
        String fileName = getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            Itk.logError("Map file name is empty.");
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            Document doc = ItkXmlUtility.singleton.newDocument();
            networkMap.toDOM(doc);
            if (! ItkXmlUtility.singleton.docToStream(doc, fos)) {
                Itk.logError("Map file could not be saved", fileName);
                return false;
            }
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e);
            Itk.logError("Save map file failed", fileName);
            return false;
        }
        Itk.logInfo("Map file has been saved", fileName);
        initCommandHistory();
        normalized = false;

        return true;
    }

    /**
     * 背景画像を読み込む
     */
    private boolean loadBackgroundImage() {
        backgroundImages.clear();
        for (MapPartGroup group : networkMap.getGroups()) {
            String imageFileName = group.getImageFileName();
            if (imageFileName == null || imageFileName.isEmpty()) {
                continue;
            }
            File file = new File(getDir(), imageFileName);
            String filePath = file.toURI().toString();
            if (! file.exists()) {
                Itk.logWarn("Background image file not exists", filePath);
                continue;
            }
            Image image = new Image(filePath);
            if (image.isError()) {
                Itk.logError("Illegal image file", filePath);
                return false;
            }
            setBackgroundImage(group, image);
        }
        return true;
    }

    /**
     * 地理院タイル画像を読み込む
     */
    private boolean loadGsiTiles() {
        gsiTiles.clear();
        gsiTileImages.clear();
        try {
            String tileName = properties.getString("gsi_tile_name", GsiTile.DATA_ID_PALE);
            int zoom = properties.getInteger("gsi_tile_zoom", 14);
            MapPartGroup root = (MapPartGroup)networkMap.getRoot();
            int zone = properties.getInteger("zone", root.getZone());
            if (zone != 0) {
                gsiTiles = GsiTile.loadGsiTiles(networkMap, tileName, zoom, zone);
                for (GsiTile gsiTile : gsiTiles) {
                    BufferedImage image = gsiTile.getImage();
                    WritableImage imageFx = new WritableImage(image.getWidth(null), image.getHeight(null));
                    gsiTileImages.put(gsiTile, SwingFXUtils.toFXImage(image, imageFx));
                }
            }
        } catch(Exception e) {
            Itk.logError("Background map loading error", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * マップデータが変更されているか?
     */
    public boolean isModified() {
        return historyIndex > 0;
    }

    /**
     * マップデータが正規化のため修正されているか?
     */
    public boolean isNormalized() {
        return normalized;
    }

    /**
     * コマンド履歴を初期化する
     */
    private void initCommandHistory() {
        commandHistory.clear();
        commandHistory.add(new DummyCommand());
        historyIndex = 0;
    }

    /**
     * 編集コマンドブロックの開始指定
     */
    public synchronized void startOfCommandBlock() {
        isFirstCommand = true;
    }

    /**
     * 編集コマンドブロックの終了指定
     */
    public synchronized void endOfCommandBlock() {
        if (lastCommand != null) {
            lastCommand.setLast(true);
            lastCommand = null;
        }
        refresh();
    }

    /**
     * 編集コマンドを実行する(ブロックの開始と終了を伴う)
     */
    public synchronized boolean invokeSingleCommand(EditCommandBase command) {
        startOfCommandBlock();
        boolean result = invoke(command);
        endOfCommandBlock();
        return result;
    }

    /**
     * 編集コマンドを実行する
     */
    public synchronized boolean invoke(EditCommandBase command) {
        command.setTime();
        if (isFirstCommand) {
            command.setFirst(true);
            initStatusOfEdit();
            isFirstCommand = false;
        } else {
            command.setFirst(false);
        }

        boolean result = command.invoke(this);
        if (command.isInvalid()) {
            initCommandHistory();
            displayMessage("Warn", "System information", "The map data may have been broken.");
            lastCommand = null;
        } else {
            updateStatusOfEdit(command);
            frame.setStatusText(command.toString());

            // undo した分のコマンド履歴は削除する
            if (historyIndex + 1 < commandHistory.size()) {
                commandHistory.subList(historyIndex + 1, commandHistory.size()).clear();
            }

            EditCommandBase previousCommand = commandHistory.get(historyIndex);
            if (previousCommand.isMergeable(command)) {
                previousCommand.mergeTo(command);
            } else {
                commandHistory.add(command);
                historyIndex++;
            }
            lastCommand = command;
        }

        return result;
    }

    /**
     * 最後に実行した編集コマンドを取り消して実行前の状態に戻す
     */
    public synchronized void undo() {
        if (historyIndex == 0) {
            beep();
            return;
        }

        initStatusOfEdit();
        boolean heightChangeable = false;
        while (true) {
            EditCommandBase command = commandHistory.get(historyIndex);
            command.undo(this);
            if (command.isInvalid()) {
                initCommandHistory();
                displayMessage("Warn", "System information", "The map data may have been broken.");
                break;
            }
            heightChangeable = heightChangeable || command.isHeightChangeable();
            updateStatusOfEdit(command);
            frame.setStatusText("Undo: " + command.toString());
            historyIndex--;

            if (command.isFirst()) {
                break;
            }
        }
        if (heightChangeable) {
            updateHeight();
        }

        frame.getCanvas().clearEditingStates();
        refresh();
    }

    /**
     * 最後に undo した編集コマンドを再び実行する
     */
    public synchronized void redo() {
        if (historyIndex + 1 == commandHistory.size()) {
            beep();
            return;
        }

        initStatusOfEdit();
        boolean heightChangeable = false;
        while (true) {
            historyIndex++;
            EditCommandBase command = commandHistory.get(historyIndex);
            command.invoke(this);
            if (command.isInvalid()) {
                initCommandHistory();
                displayMessage("Warn", "System information", "The map data may have been broken.");
                break;
            }
            heightChangeable = heightChangeable || command.isHeightChangeable();
            updateStatusOfEdit(command);
            frame.setStatusText("Redo: " + command.toString());

            if (command.isLast()) {
                break;
            }
        }
        if (heightChangeable) {
            updateHeight();
        }

        frame.getCanvas().clearEditingStates();
        refresh();
    }

    /**
     * 変更点を初期化する
     */
    public void initStatusOfEdit() {
        groupTagChanged = false;
        nodeParamChanged = false;
        linkParamChanged = false;
        areaParamChanged = false;
        polygonParamChanged = false;
        nodeTagChanged = false;
        linkTagChanged = false;
        areaTagChanged = false;
        polygonTagChanged = false;
        groupVolumeChanged = false;
        nodeVolumeChanged = false;
        linkVolumeChanged = false;
        areaVolumeChanged = false;
        polygonVolumeChanged = false;
    }

    /**
     * 変更点を更新する
     */
    public void updateStatusOfEdit(EditCommandBase command) {
        switch (command.getChangeType()) {
        case GROUP_PARAM:
            // 今のところ更新は不要
            break;
        case GROUP_TAG:
            groupTagChanged = true;
            break;
        case GROUP_VOLUME:
            groupVolumeChanged = true;
            break;
        case NODE_PARAM:
            nodeParamChanged = true;
            break;
        case NODE_TAG:
            nodeTagChanged = true;
            break;
        case NODE_VOLUME:
            nodeVolumeChanged = true;
            break;
        case LINK_PARAM:
            linkParamChanged = true;
            break;
        case LINK_TAG:
            linkTagChanged = true;
            break;
        case LINK_VOLUME:
            linkVolumeChanged = true;
            break;
        case AREA_PARAM:
            areaParamChanged = true;
            break;
        case AREA_TAG:
            areaTagChanged = true;
            break;
        case AREA_VOLUME:
            areaVolumeChanged = true;
            break;
        case POLYGON_PARAM:
            polygonParamChanged = true;
            break;
        case POLYGON_TAG:
            polygonTagChanged = true;
            break;
        case POLYGON_VOLUME:
            polygonVolumeChanged = true;
            break;
        case SYMBOLIC_LINK_VOLUME:
            // Canvas の更新だけで十分
            break;
        }
    }

    /**
     * 表示を更新する
     */
    public void refresh() {
        boolean scenarioPanelUpdated = false;

        frame.getCanvas().repaintLater();

        if (groupVolumeChanged) {
            // root 以外にグループがあればそのグループに移る
            if (networkMap.getGroups().size() == 2) {
                initCurrentGroup();
            }

            // グループボタンを再構築する
            frame.updateShowBackgroundGroupMenu();
            frame.updateGroupSelectionPane();

            // シンボリックリンク追加サブメニューを再構築する
            frame.updateAddSymbolicLinkMenu();

            // マップツリーを再構築する
            frame.getGroupPanel().clear();
            frame.getGroupPanel().construct();
        } else if (groupTagChanged) {
            frame.getGroupPanel().refresh();

            // グループタグ表示を更新する
            for (MapPartGroup group : networkMap.getGroups()) {
                frame.updateGroupTag(group);
            }

            // シンボリックリンク追加サブメニューを再構築する
            frame.updateAddSymbolicLinkMenu();
        }

        if (nodeTagChanged || nodeVolumeChanged) {
            frame.getCanvas().updateNodePoints();
            // フィルタ処理があるため nodeTagChanged でも reset() が必要
            frame.getNodePanel().reset();
            frame.getScenarioPanel().reset();
            scenarioPanelUpdated = true;
        } else if (nodeParamChanged) {
            frame.getCanvas().updateNodePoints();
            frame.getNodePanel().refresh();
        }

        if (linkTagChanged || linkVolumeChanged) {
            // フィルタ処理があるため linkTagChanged でも reset() が必要
            frame.getLinkPanel().reset();
            if (! scenarioPanelUpdated) {
                frame.getScenarioPanel().reset();
            }
        } else if (linkParamChanged) {
            frame.getLinkPanel().refresh();
        }

        if (areaTagChanged || areaVolumeChanged) {
            // フィルタ処理があるため areaTagChanged でも reset() が必要
            frame.getAreaPanel().reset();
        } else if (areaParamChanged) {
            frame.getAreaPanel().refresh();
        }

        if (polygonTagChanged || polygonVolumeChanged) {
            // フィルタ処理があるため polygonTagChanged でも reset() が必要
            frame.getPolygonPanel().reset();
        } else if (polygonParamChanged) {
            frame.getPolygonPanel().refresh();
        }
    }

    /**
     * ウィンドウを表示する
     */
    public void show() {
        init();
        initCommandHistory();

        final MapEditor editor = this;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                MapPartGroup root = (MapPartGroup)networkMap.getRoot();
                if (networkMap.getGroups().size() > 1 && ! root.getChildNodes().isEmpty()) {
                    // サブグループが存在する場合にはルートグループにタグ以外の要素が存在してはいけない
                    Alert alert = new Alert(AlertType.ERROR, "Invalid map data with nodes in root group.\nCan not edit this map.", ButtonType.OK);
                    alert.initOwner(frame.getStage());
                    alert.showAndWait();
                    return;
                }
                frame = new EditorFrameFx(editor, "Network Map Editor", settings);
                frame.show();
            }
        });
    }

    /**
     * マップの中心点を求める
     */
    public Point2D calcCenter() {
        Rectangle2D bounds = networkMap.calcRectangle();
        double x = bounds.getMinX() + bounds.getWidth() / 2.0;
        double y = bounds.getMinY() + bounds.getHeight() / 2.0;
        return new Point2D(x, y);
    }

    /**
     * メッセージを表示する
     */
    public void displayMessage(String logLevelName, String label, String message) {
        Itk.LogLevel logLevel = Itk.LogLevel.Info;
        for (Map.Entry<Itk.LogLevel, String> entry : Itk.LogTag.entrySet()) {
            if (entry.getValue().equals(Itk.LogTagPrefix + logLevelName)) {
                logLevel = entry.getKey();
                break;
            }
        }

        // ステータスラインとコンソールに表示する
        if (message == null) {
            frame.setStatusText(String.format("%s: %s", logLevelName, label));
            Itk.logOutput(logLevel, label);
        } else {
            frame.setStatusText(String.format("%s: [%s] %s", logLevelName, label, message));
            Itk.logOutput(logLevel, label, message);
        }

        // Warning 以上ならばダイアログでも表示する
        AlertType alertType = null;
        String alertLevel = null;
        switch (logLevel) {
        case Warn:
            alertType = AlertType.WARNING;
            alertLevel = "Warning";
            break;
        case Error:
            alertType = AlertType.ERROR;
            alertLevel = "Error";
            break;
        case Fatal:
            alertType = AlertType.ERROR;
            alertLevel = "Fatal error";
            break;
        }
        if (alertType != null) {
            Alert alert = new Alert(alertType, message == null ? "" : message, ButtonType.OK);
            alert.initOwner(frame.getStage());
            alert.setTitle(label);
            alert.getDialogPane().setHeaderText(alertLevel);
            beep();
            alert.showAndWait();
        }
    }

    /**
     * アラート音を鳴らす
     */
    public static final void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /**
     * カレントグループのフィルタリングされたノードを取得する
     */
    public ArrayList<MapNode> getOperableNodes(boolean selected) {
        ArrayList<MapNode> nodes = new ArrayList();
        for (MapNode node : currentGroup.getChildNodes()) {
            if (frame.getNodePanel().getFilteredSet().contains(node)) {
                if (selected && ! node.selected) {
                    continue;
                }
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * 単一のグループで構成されているか?
     */
    public boolean isSingleGroup(ArrayList<? extends OBNode> obNodes) {
        if (obNodes.isEmpty()) {
            return true;
        }
        MapPartGroup group = (MapPartGroup)obNodes.get(0).getParent();
        for (OBNode node : obNodes) {
            if (node.getParent() != group) {
                return false;
            }
        }
        return true;
    }

    /**
     * 選択中ノードをすべて取得する
     */
    public ArrayList<MapNode> getSelectedNodes() {
        ArrayList<MapNode> nodes = new ArrayList();
        for (MapNode node : networkMap.getNodes()) {
            if (node.selected) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * ノードの選択をすべて解除する
     */
    public int deselectNodes() {
        int countOfselected = 0;
        for (MapNode node : networkMap.getNodes()) {
            if (node.selected) {
                node.selected = false;
                countOfselected++;
            }
        }
        return countOfselected;
    }

    /**
     * 選択中のノード数を取得する
     */
    public int getCountOfSelectedNodes() {
        int count = 0;
        for (MapNode node : networkMap.getNodes()) {
            if (node.selected) {
                count++;
            }
        }
        return count;
    }

    /**
     * 選択中リンクをすべて取得する
     */
    public MapLinkTable getSelectedLinks() {
        MapLinkTable links = new MapLinkTable();
        for (MapLink link : networkMap.getLinks()) {
            if (link.selected) {
                links.add(link);
            }
        }
        return links;
    }

    /**
     * リンクの選択をすべて解除する
     */
    public int deselectLinks() {
        int countOfselected = 0;
        for (MapLink link : networkMap.getLinks()) {
            if (link.selected) {
                link.selected = false;
                countOfselected++;
            }
        }
        return countOfselected;
    }

    /**
     * 選択中のリンク数を取得する
     */
    public int getCountOfSelectedLinks() {
        int count = 0;
        for (MapLink link : networkMap.getLinks()) {
            if (link.selected) {
                count++;
            }
        }
        return count;
    }

    /**
     * 選択中のエリアをすべて取得する
     */
    public ArrayList<MapArea> getSelectedAreas() {
        ArrayList<MapArea> areas = new ArrayList();
        for (MapArea area : networkMap.getAreas()) {
            if (area.selected) {
                areas.add(area);
            }
        }
        return areas;
    }

    /**
     * エリアの選択をすべて解除する
     */
    public void deselectAreas() {
        for (MapArea area : networkMap.getAreas()) {
            area.selected = false;
        }
    }

    /**
     * 選択中のエリア数を取得する
     */
    public int getCountOfSelectedAreas() {
        int count = 0;
        for (MapArea area : networkMap.getAreas()) {
            if (area.selected) {
                count++;
            }
        }
        return count;
    }

    /**
     * 選択中のポリゴンをすべて取得する
     */
    public ArrayList<MapPolygon> getSelectedPolygons() {
        ArrayList<MapPolygon> polygons = new ArrayList();
        for (MapPolygon polygon : networkMap.getPolygons()) {
            if (polygon.selected) {
                polygons.add(polygon);
            }
        }
        return polygons;
    }

    /**
     * ポリゴンの選択をすべて解除する
     */
    public int deselectPolygons() {
        int countOfselected = 0;
        for (MapPolygon polygon : networkMap.getPolygons()) {
            if (polygon.selected) {
                polygon.selected = false;
                countOfselected++;
            }
        }
        return countOfselected;
    }

    /**
     * 選択中のポリゴン数を取得する
     */
    public int getCountOfSelectedPolygons() {
        int count = 0;
        for (MapPolygon polygon : networkMap.getPolygons()) {
            if (polygon.selected) {
                count++;
            }
        }
        return count;
    }

    /**
     * タグを追加する
     */
    public void addTag(ArrayList<? extends OBNode> obNodes, String tag) {
        if (obNodes.isEmpty()) {
            return;
        }
        startOfCommandBlock();
        for (OBNode node : obNodes) {
            if (! node.hasTag(tag)) {
                if (! invoke(new AddTag(node, tag))) {
                    break;
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * タグを削除する
     */
    public void removeTag(ArrayList<? extends OBNode> obNodes, String tag) {
        if (obNodes.isEmpty()) {
            return;
        }
        startOfCommandBlock();
        for (OBNode node : obNodes) {
            if (node.hasTag(tag)) {
                if (! invoke(new RemoveTag(node, tag))) {
                    break;
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * シンボリックリンクを追加する
     */
    public void addSymbolicLink(ArrayList<? extends OBNode> obNodes, MapPartGroup group) {
        if (obNodes.isEmpty()) {
            return;
        }

        ArrayList<OBNode> originals = new ArrayList();
        for (OBNodeSymbolicLink symbolicLink : group.getSymbolicLinks()) {
            originals.add(symbolicLink.getOriginal());
        }

        startOfCommandBlock();
        for (OBNode node : obNodes) {
            if (! originals.contains(node)) {
                if (! invoke(new AddSymbolicLink(group, node))) {
                    break;
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * シンボリックリンクを削除する
     */
    public void removeSymbolicLink(ArrayList<? extends OBNode> obNodes) {
        if (obNodes.isEmpty()) {
            return;
        }
        startOfCommandBlock();
        _removeSymbolicLink(obNodes);
        endOfCommandBlock();
    }

    /**
     * シンボリックリンクを削除する
     */
    private boolean _removeSymbolicLink(ArrayList<? extends OBNode> obNodes) {
        for (MapPartGroup group : networkMap.getGroups()) {
            for (OBNodeSymbolicLink symbolicLink : group.getSymbolicLinks()) {
                if (obNodes.contains(symbolicLink.getOriginal())) {
                    if (! invoke(new RemoveSymbolicLink(group, symbolicLink))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * シェープファイルを読み込んで座標リストに変換する
     */
    public ArrayList<ArrayList<Point2D>> shapefileToLines(String srcEpsg, int zone, String shapefilePath) throws Exception {
        for (char c : shapefilePath.toCharArray()) {
            if (c > '\u007e') {
                throw new Exception("The file path contains characters that can not be used in \"ogr2ogr\" (Japanese characters can not be used): " + shapefilePath);
            }
        }

        String tmpDir = System.getProperty("java.io.tmpdir");
        if (! tmpDir.endsWith(File.separator)) {
            tmpDir += File.separator;
        }

        // シェープファイルを平面直角座標系で GeoJSON に変換する
        List<String> commandLine = Arrays.asList("ogr2ogr", "-s_srs", srcEpsg, "-t_srs", GsiTile.JGD2000_JPR_EPSG_NAMES[zone], "-f", "geoJSON", tmpDir + "__GEOJSON__.json", shapefilePath);
        Itk.logInfo("External process", String.join(" ", commandLine));
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.inheritIO();
        Process process = pb.start();
        int ret = process.waitFor();
        if (ret != 0) {
            throw new Exception("Execution of external program \"ogr2ogr\" failed.");
        }

        // GeoJSON ファイルを読み込んで座標リストに変換する
        return geoJSONtoLines(tmpDir + "__GEOJSON__.json");
    }

    /**
     * GeoJSON ファイルを読み込んで座標リストに変換する
     */
    public ArrayList<ArrayList<Point2D>> geoJSONtoLines(String filePath) throws Exception {
        JsonicHashMapGetter jsonMap = new JsonicHashMapGetter();

        FileInputStream is = new FileInputStream(filePath);
        jsonMap.setParameters(JSON.decode(is));
        is.close();

        ArrayList<HashMap> features = (ArrayList<HashMap>)jsonMap.getArrayListParameter("features", null);
        if (features == null) {
            throw new Exception("File parsing error - \"" + filePath + "\" : \"features\" elements are missing.");
        }
        ArrayList<ArrayList<Point2D>> lines = new ArrayList();
        for (HashMap feature : features) {
            jsonMap.setParameters(feature);

            HashMap geometry = jsonMap.getHashMapParameter("geometry", null);
            if (geometry == null) {
                Itk.logWarn_("File parsing error", "\"" + filePath + "\" : \"geometry\" elements are missing.");
                continue;
            }
            jsonMap.setParameters(geometry);

            ArrayList<ArrayList<BigDecimal>> coordinates = (ArrayList<ArrayList<BigDecimal>>)jsonMap.getArrayListParameter("coordinates", null);
            if (coordinates == null) {
                Itk.logWarn_("File parsing error", "\"" + filePath + "\" : \"coordinates\" elements are missing.");
                continue;
            }

            ArrayList<Point2D> line = new ArrayList();
            for (ArrayList<BigDecimal> coordinate : coordinates) {
                if (coordinate.size() < 2) {
                    throw new Exception("File parsing error - \"" + filePath + "\" : invalid coordinates: " + coordinate);
                }
                double east = coordinate.get(0).doubleValue();
                double north = coordinate.get(1).doubleValue();
                // CrowdWalk 座標系に変換
                Point2D point = new Point2D(east, -north);
                line.add(point);
            }
            if (! line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * シェープファイルを読み込んでマップデータに追加する
     */
    public void readShapefile(String srcEpsg, int scaleOfRoundOff, String lengthName, boolean useGeodeticLength, String widthName, Double correctionFactor, ArrayList<Double> referenceTable, MapPartGroup group, int zone, List<String> shapefileList) throws Exception {
        if (shapefileList.isEmpty()) {
            return;
        }

        String tmpDir = System.getProperty("java.io.tmpdir");
        if (! tmpDir.endsWith(File.separator)) {
            tmpDir += File.separator;
        }

        // シェープファイルを結合する
        String dst = "__SHAPEFILE__";
        for (int index = 0; index < shapefileList.size(); index++) {
            String shapefilePath = shapefileList.get(index);
            for (char c : shapefilePath.toCharArray()) {
                if (c > '\u007e') {
                    throw new Exception("The file path contains characters that can not be used in \"ogr2ogr\" (Japanese characters can not be used): " + shapefilePath);
                }
            }

            List<String> commandLine = null;
            if (index == 0) {
                commandLine = Arrays.asList("ogr2ogr", "-lco", "ENCODING=UTF-8", tmpDir + dst + ".shp", shapefilePath);
            } else {
                commandLine = Arrays.asList("ogr2ogr", "-update", "-append", tmpDir + dst + ".shp", shapefilePath, "-nln", dst);
            }
            Itk.logInfo("External process", String.join(" ", commandLine));
            ProcessBuilder pb = new ProcessBuilder(commandLine);
            pb.inheritIO();
            Process process = pb.start();
            int ret = process.waitFor();
            if (ret != 0) {
                throw new Exception("Execution of external program \"ogr2ogr\" failed.");
            }
        }

        // 結合されたシェープファイルを GeoJSON に変換する
        List<String> commandLine = Arrays.asList("ogr2ogr", "-f", "geoJSON", tmpDir + "__GEOJSON__.json", tmpDir + dst + ".shp");
        Itk.logInfo("External process", String.join(" ", commandLine));
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.inheritIO();
        Process process = pb.start();
        int ret = process.waitFor();
        if (ret != 0) {
            throw new Exception("Execution of external program \"ogr2ogr\" failed.");
        }

        // GeoJSON ファイルを読み込んでマップデータに追加する
        readGeoJSON(tmpDir + "__GEOJSON__.json", srcEpsg, scaleOfRoundOff, lengthName, useGeodeticLength, widthName, correctionFactor, referenceTable, group, zone);
    }

    /**
     * GeoJSON ファイルを読み込んでマップデータに追加する
     */
    public void readGeoJSON(String filePath, String srcEpsg, int scaleOfRoundOff, String lengthName, boolean useGeodeticLength, String widthName, Double correctionFactor, ArrayList<Double> referenceTable, MapPartGroup group, int zone) throws Exception {
        CoordinateTransform transform = GsiTile.createCoordinateTransform(srcEpsg, GsiTile.JGD2000_JPR_EPSG_NAMES[zone]);
        JsonicHashMapGetter jsonMap = new JsonicHashMapGetter();

        FileInputStream is = new FileInputStream(filePath);
        jsonMap.setParameters(JSON.decode(is));
        is.close();

        ArrayList<HashMap> features = (ArrayList<HashMap>)jsonMap.getArrayListParameter("features", null);
        if (features == null) {
            throw new Exception("File parsing error - \"" + filePath + "\" : \"features\" elements are missing.");
        }

        startOfCommandBlock();
        HashMap<String, MapNode> nodes = new HashMap<>();
        HashMap<String, MapLink> links = new HashMap<>();
        for (HashMap feature : features) {
            jsonMap.setParameters(feature);
            HashMap _properties = jsonMap.getHashMapParameter("properties", null);
            if (_properties == null) {
                Itk.logWarn_("File parsing error", "\"" + filePath + "\" : \"properties\" elements are missing.");
                continue;
            }
            HashMap geometry = jsonMap.getHashMapParameter("geometry", null);
            if (geometry == null) {
                Itk.logWarn_("File parsing error", "\"" + filePath + "\" : \"geometry\" elements are missing.");
                continue;
            }

            jsonMap.setParameters(_properties);
            double lineLength = jsonMap.getDoubleParameter(lengthName, 0.0);
            double width = jsonMap.getDoubleParameter(widthName, 0.0);
            if (referenceTable != null && ! referenceTable.isEmpty()) {
                width = referenceTable.get((int)width);
            } else if (correctionFactor != null) {
                width *= correctionFactor.doubleValue();
            }

            jsonMap.setParameters(geometry);
            ArrayList<ArrayList<BigDecimal>> coordinates = (ArrayList<ArrayList<BigDecimal>>)jsonMap.getArrayListParameter("coordinates", null);
            if (coordinates == null) {
                Itk.logWarn_("File parsing error", "\"" + filePath + "\" : \"coordinates\" elements are missing.");
                continue;
            }

            double[] ratio = new double[coordinates.size() - 1];
            int ratioIndex = 0;
            if (useGeodeticLength) {
                java.awt.geom.Point2D lastPoint = null;
                double totalDistance = 0.0;
                for (ArrayList<BigDecimal> coordinate : coordinates) {
                    if (coordinate.size() < 2) {
                        endOfCommandBlock();
                        updateHeight();
                        throw new Exception("File parsing error - \"" + filePath + "\" : invalid coordinates: " + coordinate);
                    }
                    double x = coordinate.get(0).doubleValue();
                    double y = coordinate.get(1).doubleValue();
                    java.awt.geom.Point2D point = new java.awt.geom.Point2D.Double(x, y);

                    if (lastPoint != null) {
                        double d = point.distance(lastPoint);
                        ratio[ratioIndex] = d;
                        totalDistance += d;
                        ratioIndex++;
                    }
                    lastPoint = point;
                }
                for (int index = 0; index < ratio.length; index++) {
                    ratio[index] /= totalDistance;
                }
            }

            ratioIndex = 0;
            MapNode from = null;
            for (ArrayList<BigDecimal> coordinate : coordinates) {
                if (coordinate.size() < 2) {
                    endOfCommandBlock();
                    updateHeight();
                    throw new Exception("File parsing error - \"" + filePath + "\" : invalid coordinates: " + coordinate);
                }
                double x = roundValue(coordinate.get(0).doubleValue(), scaleOfRoundOff);
                double y = roundValue(coordinate.get(1).doubleValue(), scaleOfRoundOff);
                double height = 0.0;
                // 経緯度を CrowdWalk 座標に変換する
                java.awt.geom.Point2D jpr = GsiTile.transformCoordinate(transform, x, y);
                Point2D point = new Point2D(roundValue(jpr.getX(), 4), roundValue(-jpr.getY(), 4));
                String pointStr = point.toString().replaceAll(" ", "");

                MapNode node = null;
                if (nodes.containsKey(pointStr)) {
                    node = nodes.get(pointStr);
                } else {
                    AddNode command = new AddNode(group, point, height);
                    if (! invoke(command)) {
                        endOfCommandBlock();
                        updateHeight();
                        throw new Exception("Edit command error: " + command.toString());
                    }
                    node = (MapNode)networkMap.getObject(command.getId());
                    nodes.put(pointStr, node);
                }

                if (from == node) {
                    Itk.logWarn_("readGeoJSON", "from === node");
                    ratioIndex++;
                } else if (from != null) {
                    String nodeIdPair = makeNodeIdPair(from, node);
                    if (links.containsKey(nodeIdPair)) {
                        Itk.logWarn_("readGeoJSON", "Duplicate link found at node[" + nodeIdPair + "]. Ignore this.");
                    } else {
                        double length = 0.0;
                        if (useGeodeticLength) {
                            length = roundValue(lineLength * ratio[ratioIndex], 4);
                        } else {
                            length = from.getPosition().distance(node.getPosition());
                        }
                        AddLink command = new AddLink(from, node, length, width);
                        if (! invoke(command)) {
                            endOfCommandBlock();
                            updateHeight();
                            throw new Exception("Edit command error: " + command.toString());
                        }
                        MapLink link = (MapLink)networkMap.getObject(command.getId());
                        links.put(nodeIdPair, link);
                    }
                    ratioIndex++;
                }
                from = node;
            }
        }

        // root グループの zone にセットする
        MapPartGroup root = (MapPartGroup)networkMap.getRoot();
        if (root.getZone() != zone) {
            if (! invoke(new SetZone(root, zone))) {
                ;   // do nothing
            }
        }

        endOfCommandBlock();
        updateHeight();
    }

    /**
     * 実数値 value を小数点以下第 scale 位で四捨五入する
     */
    public static double roundValue(double value, int scale) {
        if (scale >= 0) {
            BigDecimal bd = new BigDecimal(String.valueOf(value));
            return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return value;
    }

    /**
     * 2つのノード ID を小さい順に並べた文字列を返す
     */
    public static String makeNodeIdPair(MapNode node1, MapNode node2) {
        if (node1.ID.compareTo(node2.ID) < 0) {
            return node1.ID + " " + node2.ID;
        } else {
            return node2.ID + " " + node1.ID;
        }
    }

    /**
     * 高低差を反映してリンク長を計算する
     */
    public double calculateLinkLength(MapLink link) {
        MapPartGroup group = (MapPartGroup)link.getParent();
        double scale = group.getScale();
        MapNode fromNode = link.getFrom();
        MapNode toNode = link.getTo();
        if (fromNode.getHeight() == toNode.getHeight()) {
            return fromNode.getPosition().distance(toNode.getPosition()) * scale;
        }
        Point3D point0 = new Point3D(fromNode.getX(), fromNode.getY(), fromNode.getHeight());
        Point3D point1 = new Point3D(toNode.getX(), toNode.getY(), toNode.getHeight());
        return point0.distance(point1) * scale;
    }

    /**
     * リンク長を再計算する
     */
    public void recalculateLinkLength(ArrayList<MapLink> links, boolean reflectHeight) {
        startOfCommandBlock();
        for (MapLink link : links) {
            MapPartGroup group = (MapPartGroup)link.getParent();
            double scale = group.getScale();
            MapNode fromNode = link.getFrom();
            MapNode toNode = link.getTo();
            double length = 0.0;
            if (reflectHeight) {
                Point3D point0 = new Point3D(fromNode.getX(), fromNode.getY(), fromNode.getHeight());
                Point3D point1 = new Point3D(toNode.getX(), toNode.getY(), toNode.getHeight());
                length = point0.distance(point1) * scale;
            } else {
                length = fromNode.getPosition().distance(toNode.getPosition()) * scale;
            }
            if (! invoke(new SetLength(link, length))) {
                break;
            }
        }
        endOfCommandBlock();
    }

    /**
     * スケールの設定とリンク長の再計算
     */
    public void setScaleAndRecalculateLinkLength(MapPartGroup group, double scale, boolean recalcLength, boolean allGroups) {
        startOfCommandBlock();
        if (allGroups) {
            for (MapPartGroup _group : networkMap.getGroups()) {
                if (scale != _group.getScale()) {
                    if (! invoke(new SetScale(_group, scale))) {
                        endOfCommandBlock();
                        return;
                    }
                }
            }
            if (recalcLength) {
                for (MapLink link : networkMap.getLinks()) {
                    double length = link.getFrom().getPosition().distance(link.getTo().getPosition()) * scale;
                    if (! invoke(new SetLength(link, length))) {
                        endOfCommandBlock();
                        return;
                    }
                }
            }
        } else {
            if (scale != group.getScale()) {
                if (! invoke(new SetScale(group, scale))) {
                    endOfCommandBlock();
                    return;
                }
            }
            if (recalcLength) {
                for (MapLink link : group.getChildLinks()) {
                    double length = link.getFrom().getPosition().distance(link.getTo().getPosition()) * scale;
                    if (! invoke(new SetLength(link, length))) {
                        endOfCommandBlock();
                        return;
                    }
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * フロアを複製する
     */
    public void duplicateFloor(MapPartGroup orgGroup, int direction, int numberOfFloors, double heightDiff) throws Exception {
        if (orgGroup == networkMap.getRoot()) {
            throw new Exception("Root group can not be duplicated.");
        }

        Matcher match = orgGroup.matchTag("^(B?)(\\d+)F$");
        if (match == null) {
            throw new Exception("No floor number given for this group.");
        }
        String floorName = match.group(0);
        int floor = Integer.parseInt(match.group(2)) * (match.group(1).isEmpty() ? 1 : -1);

        int nextFloor = floor + direction;
        if (nextFloor == 0) {
            nextFloor += direction;
        }
        String nextFloorName = ((nextFloor < 0) ? "B" : "") + Math.abs(nextFloor) + "F";

        // next floor が衝突しないかのチェック
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group.getTagString().equals(nextFloorName)) {
                throw new Exception("The generated floor names conflict: " + nextFloorName);
            }
        }

        startOfCommandBlock();
        for (int n = 1; n <= numberOfFloors; n++) {
            // グループの複製
            double defaultHeight = orgGroup.getDefaultHeight() + heightDiff * direction * n;
            AddGroup _command = new AddGroup(nextFloorName, defaultHeight, orgGroup.getZone());
            if (! invoke(_command)) {
                break;
            }
            MapPartGroup newGroup = (MapPartGroup)networkMap.getObject(_command.getId());
            if (! invoke(new SetScale(newGroup, orgGroup.getScale()))) {
                break;
            }

            // ノードの複製
            HashMap<MapNode, MapNode> nodeToNode = new HashMap<MapNode, MapNode>();
            boolean failed = false;
            for (MapNode node : orgGroup.getChildNodes()) {
                double height = node.getHeight() - orgGroup.getDefaultHeight() + defaultHeight;
                AddNode command = new AddNode(newGroup, new Point2D(node.getX(), node.getY()), height);
                if (! invoke(command)) {
                    failed = true;
                    break;
                }
                MapNode newNode = (MapNode)networkMap.getObject(command.getId());
                for (String tag : node.getTags()) {
                    if (! invoke(new AddTag(newNode, tag.replaceFirst(floorName, nextFloorName)))) {
                        failed = true;
                        break;
                    }
                }
                if (failed) {
                    break;
                }
                nodeToNode.put(node, newNode);
            }
            updateHeight();
            if (failed) {
                break;
            }

            // リンクの複製
            for (MapLink link : orgGroup.getChildLinks()) {
                MapNode fromNode = nodeToNode.get(link.getFrom());
                MapNode toNode = nodeToNode.get(link.getTo());
                if (fromNode != null && toNode != null) {
                    AddLink command = new AddLink(fromNode, toNode, link.getLength(), link.getWidth());
                    if (! invoke(command)) {
                        endOfCommandBlock();
                        return;
                    }
                    MapLink newLink = (MapLink)networkMap.getObject(command.getId());
                    for (String tag : link.getTags()) {
                        if (! invoke(new AddTag(newLink, tag.replaceFirst(floorName, nextFloorName)))) {
                            endOfCommandBlock();
                            return;
                        }
                    }
                }
            }

            // TODO: 階段の複製

            nextFloor += direction;
            if (nextFloor == 0) {
                nextFloor += direction;
            }
            nextFloorName = ((nextFloor < 0) ? "B" : "") + Math.abs(nextFloor) + "F";
            for (MapPartGroup group : networkMap.getGroups()) {
                if (group.getTagString().equals(nextFloorName)) {
                    endOfCommandBlock();
                    return;
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * 選択中のノードを水平に整列させる
     */
    public void alignNodesHorizontally() {
        ArrayList<MapNode> nodes = getOperableNodes(true);
        if (nodes.size() < 2) {
            return;
        }
        MapNode pointedNode = frame.getCanvas().getPointedNode();
        if (pointedNode == null || ! pointedNode.selected) {
            Alert alert = new Alert(AlertType.WARNING, "Point to the base node and then execute.", ButtonType.OK);
            alert.initOwner(frame.getStage());
            alert.showAndWait();
            return;
        }

        double y = frame.getCanvas().getRotatedY(pointedNode);
        startOfCommandBlock();
        for (MapNode node : nodes) {
            if (node != pointedNode) {
                double x = frame.getCanvas().getRotatedX(node);
                Point2D point = frame.getCanvas().convertToOriginal(x, y);
                if (! invoke(new MoveNode(node, point.getX(), point.getY()))) {
                    break;
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * 選択中のノードを垂直に整列させる
     */
    public void alignNodesVertically() {
        ArrayList<MapNode> nodes = getOperableNodes(true);
        if (nodes.size() < 2) {
            return;
        }
        MapNode pointedNode = frame.getCanvas().getPointedNode();
        if (pointedNode == null || ! pointedNode.selected) {
            Alert alert = new Alert(AlertType.WARNING, "Point to the base node and then execute.", ButtonType.OK);
            alert.initOwner(frame.getStage());
            alert.showAndWait();
            return;
        }

        double x = frame.getCanvas().getRotatedX(pointedNode);
        startOfCommandBlock();
        for (MapNode node : nodes) {
            if (node != pointedNode) {
                double y = frame.getCanvas().getRotatedY(node);
                Point2D point = frame.getCanvas().convertToOriginal(x, y);
                if (! invoke(new MoveNode(node, point.getX(), point.getY()))) {
                    break;
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * ノードを移動する
     */
    public void moveNodes(ArrayList<MapNode> nodes, double x, double y, double z) {
        // TODO: 既存のノードと座標が重なる場合は失敗させる
        startOfCommandBlock();
        for (MapNode node : nodes) {
            if (x != 0.0 || y != 0.0) {
                if (! invoke(new MoveNode(node, node.getX() + x, node.getY() + y))) {
                    break;
                }
            }
            if (z != 0.0) {
                if (! invoke(new SetHeight(node, node.getHeight() + z))) {
                    break;
                }
            }
        }
        if (z != 0.0) {
            updateHeight();
        }
        endOfCommandBlock();
    }

    /**
     * ノードを複製して移動する
     */
    public void duplicateAndMoveNodes(ArrayList<MapNode> nodes, double x, double y, double z, MapPartGroup toGroup, boolean withoutLinks, boolean withNodeTags, boolean withLinkTags) {
        MapPartGroup group = (MapPartGroup)nodes.get(0).getParent();

        startOfCommandBlock();
        HashMap<MapNode, MapNode> nodeToNode = new HashMap<MapNode, MapNode>();
        boolean failed = false;
        for (MapNode node : nodes) {
            Point2D point = new Point2D(node.getX(), node.getY());
            double heightDiff = node.getHeight() - group.getDefaultHeight();
            AddNode command = new AddNode(toGroup, point.add(x, y), toGroup.getDefaultHeight() + heightDiff + z);
            if (! invoke(command)) {
                failed = true;
                break;
            }
            MapNode newNode = (MapNode)networkMap.getObject(command.getId());
            if (withNodeTags) {
                for (String tag : node.getTags()) {
                    if (! invoke(new AddTag(newNode, tag))) {
                        failed = true;
                        break;
                    }
                }
                if (failed) {
                    break;
                }
            }
            nodeToNode.put(node, newNode);
        }
        updateHeight();
        if (failed) {
            endOfCommandBlock();
            return;
        }

        if (! withoutLinks) {
            failed = false;
            for (MapLink link : (List<MapLink>)(networkMap.getLinks().clone())) {
                if (link.getFrom().selected && link.getTo().selected) {
                    MapNode fromNode = nodeToNode.get(link.getFrom());
                    MapNode toNode = nodeToNode.get(link.getTo());
                    AddLink command = new AddLink(fromNode, toNode, link.getLength(), link.getWidth());
                    if (! invoke(command)) {
                        failed = true;
                        break;
                    }
                    if (withLinkTags) {
                        MapLink newLink = (MapLink)networkMap.getObject(command.getId());
                        for (String tag : link.getTags()) {
                            if (! invoke(new AddTag(newLink, tag))) {
                                failed = true;
                                break;
                            }
                        }
                        if (failed) {
                            break;
                        }
                    }
                }
            }
        }
        endOfCommandBlock();

        deselectNodes();
        for (MapNode newNode : nodeToNode.values()) {
            newNode.selected = true;
        }
    }

    /**
     * 移動後に座標が一致するノードを取得する.
     *
     * 10cm 以内にあれば一致と見なす
     */
    public ArrayList<MapNode> getCollisionNodes(ArrayList<MapNode> nodes, double x, double y, double z, MapPartGroup toGroup) {
        MapPartGroup group = (MapPartGroup)nodes.get(0).getParent();
        ArrayList<MapNode> collisionNodes = new ArrayList();
        double heightDiff = toGroup.getDefaultHeight() - group.getDefaultHeight();
        for (MapNode node : nodes) {
            double height = node.getHeight() + heightDiff + z;
            Point3D point = new Point3D(node.getX() + x, node.getY() + y, height);
            for (MapNode _node : toGroup.getChildNodes()) {
                if (point.distance(_node.getX(), _node.getY(), _node.getHeight()) <= 0.1) {
                    collisionNodes.add(_node);
                }
            }
        }
        return collisionNodes;
    }

    /**
     * 複数ノードの拡大縮小と回転
     */
    public void rotateAndScaleNodes(ArrayList<MapNode> nodes, double scaleX, double scaleY, double angle) {
        double cx = 0.0;
        double cy = 0.0;
        for (MapNode node : nodes) {
            cx += node.getX();
            cy += node.getY();
        }
        cx /= nodes.size();
        cy /= nodes.size();

        double r = -angle * 2.0 * Math.PI / 360.0;
        double cosR = Math.cos(r);
        double sinR = Math.sin(r);

        startOfCommandBlock();
        for (MapNode node : nodes) {
            double dx = (node.getX() - cx) * scaleX;
            double dy = (node.getY() - cy) * scaleY;
            double x = cx + dx * cosR + dy * sinR;
            double y = cy - dx * sinR + dy * cosR;
            if (! invoke(new MoveNode(node, x, y))) {
                break;
            }
        }
        endOfCommandBlock();
    }

    /**
     * マップ座標の正規化
     */
    public void normalizeCoordinates(MapNode node1, MapNode node2, int zone, double node1Longitude, double node1Latitude, double node2Longitude, double node2Latitude, String scaleName, boolean rotationEnabled, boolean lengthCalculating, boolean heightReflecting) {
        // 経緯度を CrowdWalk 座標に変換する
        CoordinateTransform transform = GsiTile.createCoordinateTransform("EPSG:4326", GsiTile.JGD2000_JPR_EPSG_NAMES[zone]);
        java.awt.geom.Point2D jpr1 = GsiTile.transformCoordinate(transform, node1Longitude, node1Latitude);
        java.awt.geom.Point2D jpr2 = GsiTile.transformCoordinate(transform, node2Longitude, node2Latitude);
        Point2D point1 = toFxPoint2D(GsiTile.convertJPR2CW(jpr1.getY(), jpr1.getX()));
        Point2D point2 = toFxPoint2D(GsiTile.convertJPR2CW(jpr2.getY(), jpr2.getX()));

        Point2D nodePoint1 = toFxPoint2D(node1.getPosition());
        Point2D nodePoint2 = toFxPoint2D(node2.getPosition());

        double angularDifference = 0.0;
        if (rotationEnabled) {
            double angle = getAngle(nodePoint1, nodePoint2);
            double correctAngle = getAngle(point1, point2);
            angularDifference = correctAngle - angle;
            Affine affine = new Affine();
            affine.prependRotation(angularDifference, nodePoint1);
            nodePoint2 = affine.transform(nodePoint2);
            Itk.logInfo("Normalize coordinates", "angular_difference = " + angularDifference);
        }

        Affine affine = new Affine();
        affine.prependTranslation(-nodePoint1.getX(), -nodePoint1.getY());
        if (rotationEnabled) {
            affine.prependRotation(angularDifference);
        }

        double scaleX = (point2.getX() - point1.getX()) / (nodePoint2.getX() - nodePoint1.getX());
        double scaleY = (point2.getY() - point1.getY()) / (nodePoint2.getY() - nodePoint1.getY());
        Itk.logInfo("Normalize coordinates", "scaleX = " + scaleX + ", scaleY = " + scaleY);
        switch (scaleName) {
        case "separately":
            affine.prependScale(scaleX, scaleY);
            Itk.logInfo("Normalize coordinates", "applied scale = " + scaleName + ", value = " + scaleX + ", " + scaleY);
            break;
        case "average":
            double scale = (scaleX + scaleY) / 2.0;
            affine.prependScale(scale, scale);
            Itk.logInfo("Normalize coordinates", "applied scale = " + scaleName + ", value = " + scale);
            break;
        case "X":
            affine.prependScale(scaleX, scaleX);
            Itk.logInfo("Normalize coordinates", "applied scale = " + scaleName + ", value = " + scaleX);
            break;
        case "Y":
            affine.prependScale(scaleY, scaleY);
            Itk.logInfo("Normalize coordinates", "applied scale = " + scaleName + ", value = " + scaleY);
            break;
        }

        affine.prependTranslation(point1.getX(), point1.getY());

        startOfCommandBlock();
        for (MapNode node : networkMap.getNodes()) {
            Point2D point = affine.transform(node.getX(), node.getY());
            if (! invoke(new MoveNode(node, point.getX(), point.getY()))) {
                endOfCommandBlock();
                return;
            }
        }
        if (lengthCalculating) {
            for (MapLink link : networkMap.getLinks()) {
                double length = 0.0;
                if (heightReflecting) {
                    MapNode fromNode = link.getFrom();
                    MapNode toNode = link.getTo();
                    Point3D fromPosition = new Point3D(fromNode.getX(), fromNode.getY(), fromNode.getHeight());
                    length = fromPosition.distance(toNode.getX(), toNode.getY(), toNode.getHeight());
                } else {
                    length = link.getFrom().getPosition().distance(link.getTo().getPosition());
                }
                if (! invoke(new SetLength(link, length))) {
                    endOfCommandBlock();
                    return;
                }
            }
        }
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group.getZone() != zone) {
                if (! invoke(new SetZone(group, zone))) {
                    break;
                }
            }
            if (group.getScale() != 1.0) {
                if (! invoke(new SetScale(group, 1.0))) {
                    break;
                }
            }
        }
        endOfCommandBlock();
    }

    public static Point2D toFxPoint2D(java.awt.geom.Point2D point) {
        return new Point2D(point.getX(), point.getY());
    }

    public static double getAngle(Point2D point1, Point2D point2) {
        double radian = Math.atan2(point2.getY() - point1.getY(), point2.getX() - point1.getX());
        double angle = Math.toDegrees(radian);
        return angle < 0.0 ? 360.0 + angle : angle;
    }

    /**
     * 選択中のノードを接続リンクと共に削除する
     */
    public void removeNodes(boolean commandBlocking) {
        ArrayList<MapNode> nodes = (ArrayList<MapNode>)(getSelectedNodes().clone());

        if (commandBlocking) {
            startOfCommandBlock();
        }
        if (! _removeSymbolicLink(nodes)) {
            if (commandBlocking) {
                endOfCommandBlock();
            }
            return;
        }
        for (MapNode node : nodes) {
            boolean failed = false;
            ArrayList<MapLink> links = (ArrayList<MapLink>)(node.getLinks().clone());
            if (! _removeSymbolicLink(links)) {
                if (commandBlocking) {
                    endOfCommandBlock();
                }
                updateHeight();
                return;
            }
            for (MapLink link : links) {
                if (! invoke(new RemoveLink(link))) {
                    failed = true;
                    break;
                }
            }
            if (failed) {
                break;
            }
            if (! invoke(new RemoveNode(node))) {
                break;
            }
        }
        if (commandBlocking) {
            endOfCommandBlock();
        }
        updateHeight();
    }

    /**
     * 末端ノードか判定する。
     *
     * candidateLinkSet の中で node に接続されるリンクが link のみならば末端となる
     */
    private boolean isTerminalNode(MapNode node, MapLink link, HashSet<MapLink> candidateLinkSet) {
        for (MapLink _link : node.getLinks()) {
            if (_link != link && candidateLinkSet.contains(_link)) {
                return false;
            }
        }
        return true;
    }

    /**
     * candidateLinks をリンクが連なる順に並べ替えたものを両端のノードと共に返す。
     *
     * (candidateLinks に含まれないリンクは接続されていないものと見なす)
     * 条件:
     * ・閉じたリンクを含んでいないこと。
     * ・リンクが分岐している箇所がないこと。
     * ・末端ノードを除き、すべてのリンクが途切れなく接続されていること。
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
            // 選択したリンクはループしているため、一方通行の設定ができません。
            throw new Exception("Since the selected link is looped,\nit is not possible to set one way.");
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
                        // 選択したリンクは分岐を含んでいるため、一方通行の設定ができません。
                        throw new Exception("Since the selected link contains a branch,\nit is not possible to set one way.");
                    }
                    nextLink = _link;
                }
            }
            if (nextLink == null) {
                if (seriesLinks.size() < candidateLinks.size()) {
                    // 選択したリンクは分岐を含んでいるため、一方通行の設定ができません。
                    throw new Exception("Since the selected link contains a branch,\nit is not possible to set one way.");
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
        if (Math.abs(node.getX() - link.getOther(node).getX()) < Math.abs(node.getY() - link.getOther(node).getY())) {
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
     * 選択中のリンクを通行止めに設定する。
     */
    public void setRoadClosed() {
        startOfCommandBlock();
        for (MapLink link : getSelectedLinks()) {
            if (! invoke(new SetTrafficRestriction(link, false, false, true))) {
                break;
            }
        }
        deselectLinks();
        endOfCommandBlock();
    }

    /**
     * 選択中のリンクの一方通行と通行止めを解除する。
     */
    public void resetOneWayRoadClosed() {
        startOfCommandBlock();
        for (MapLink link : getSelectedLinks()) {
            if (link.isOneWayForward() || link.isOneWayBackward() || link.isRoadClosed()) {
                if (! invoke(new SetTrafficRestriction(link, false, false, false))) {
                    break;
                }
            }
        }
        deselectLinks();
        endOfCommandBlock();
    }

    /**
     * 選択中のリンクを削除する
     */
    public void removeLinks() {
        ArrayList<MapLink> links = (ArrayList<MapLink>)(getSelectedLinks().clone());

        startOfCommandBlock();
        if (_removeSymbolicLink(links)) {
            for (MapLink link : links) {
                if (! invoke(new RemoveLink(link))) {
                    break;
                }
            }
        }
        endOfCommandBlock();
    }

    /**
     * 選択中のエリアを削除する
     */
    public void removeAreas() {
        startOfCommandBlock();
        for (MapArea area : (List<MapArea>)(getSelectedAreas().clone())) {
            if (! invoke(new RemoveArea(area))) {
                break;
            }
        }
        endOfCommandBlock();
    }

    /**
     * リング状の連なりを構成するリンクを取得する
     */
    public static ArrayList<MapLink> getRingedLinks(MapLink startLink) {
        ArrayList<MapLink> ring = new ArrayList();
        MapNode firstNode = startLink.getFrom();
        if (firstNode.getLinks().size() != 2) {
            return null;
        }
        MapNode lastNode = startLink.getTo();
        MapLink lastLink = startLink;
        ring.add(lastLink);
        while (lastNode != firstNode) {
            if (lastNode.getLinks().size() != 2) {
                return null;
            }
            if (lastNode.getLinks().indexOf(lastLink) == 0) {
                lastLink = lastNode.getLinks().get(1);
            } else {
                lastLink = lastNode.getLinks().get(0);
            }
            ring.add(lastLink);
            lastNode = lastLink.getOther(lastNode);
        }
        return ring;
    }

    /**
     * リンクリストをノードリストに変換する.
     *
     * links はリング状に連なったリンクであること
     */
    public static ArrayList<MapNode> getBoundaryNodes(ArrayList<MapLink> links) {
        ArrayList<MapNode> nodes = new ArrayList();
        MapLink firstLink = links.get(0);
        MapLink secondLink = links.get(1);
        MapNode lastNode = firstLink.getFrom();
        if (lastNode == secondLink.getFrom() || lastNode == secondLink.getTo()) {
            lastNode = firstLink.getTo();
        }
        for (MapLink link : links) {
            nodes.add(lastNode);
            lastNode = link.getOther(lastNode);
        }
        return nodes;
    }

    /**
     * リンクリストを Path2D に変換する
     */
    public static Path2D convertToPath2D(ArrayList<MapLink> links) {
        Path2D path = new Path2D.Double();
        for (MapNode node : getBoundaryNodes(links)) {
            if (path.getCurrentPoint() == null) {
                path.moveTo(node.getX(), node.getY());
            } else {
                path.lineTo(node.getX(), node.getY());
            }
        }
        path.closePath();
        return path;
    }

    /**
     * リンクをポリゴンに変換する
     */
    public void convertToPolygon(ArrayList<MapLink> links) throws Exception {
        if (links.isEmpty()) {
            return;
        }

        // links の中からリング状に連なったリンクを探す(一部でもあれば対象)
        ArrayList<MapLink> _links = (ArrayList<MapLink>)links.clone();
        ArrayList<ArrayList<MapLink>> boundaries = new ArrayList();
        while (! _links.isEmpty()) {
            ArrayList<MapLink> boundary = getRingedLinks(_links.remove(0));
            if (boundary == null) {
                throw new Exception("Unclosed links found.");
            }
            boundaries.add(boundary);
            for (MapLink link : boundary) {
                _links.remove(link);
            }
        }

        // 標高値とタグに不整合がないかチェックする
        double height = boundaries.get(0).get(0).getFrom().getHeight();
        ArrayList<String> tags = null;
        String tagString = null;
        HashMap<ArrayList, Path2D> paths = new HashMap();
        for (ArrayList<MapLink> boundary : boundaries) {
            for (MapLink link : boundary) {
                if (link.getFrom().getHeight() != height || link.getTo().getHeight() != height) {
                    throw new Exception("Height is not constant.");
                }
                if (! link.getTags().isEmpty()) {
                    if (tagString == null) {
                        tags = (ArrayList<String>)link.getTags().clone();
                        Collections.sort(tags);
                        tagString = String.join(",", tags);
                    } else {
                        ArrayList<String> linkTags = (ArrayList<String>)link.getTags().clone();
                        Collections.sort(linkTags);
                        if (! String.join(",", linkTags).equals(tagString)) {
                            throw new Exception("Link tag is not constant.");
                        }
                    }
                }
            }
            paths.put(boundary, convertToPath2D(boundary));
        }

        // outerBoundary を特定する
        ArrayList<MapLink> _outerBoundary = boundaries.get(0);
        ArrayList<MapNode> outerNodes = getBoundaryNodes(_outerBoundary);
        for (ArrayList<MapLink> boundary : boundaries) {
            if (boundary == _outerBoundary) {
                continue;
            }
            Path2D path = paths.get(boundary);
            for (MapNode node : outerNodes) {
                if (path.contains(node.getX(), node.getY())) {
                    _outerBoundary = boundary;
                    outerNodes = getBoundaryNodes(_outerBoundary);
                    break;
                }
            }
        }
        boundaries.remove(_outerBoundary);   // == innerBoundaries

        // outerBoundary からはみ出している innerBoundary がないかチェックする(念のため)
        Path2D outerBoundaryPath = paths.get(_outerBoundary);
        for (ArrayList<MapLink> boundary : boundaries) {
            for (MapNode node : getBoundaryNodes(boundary)) {
                if (! outerBoundaryPath.contains(node.getX(), node.getY())) {
                    throw new Exception("There are multiple outer boundaries.");
                }
            }
        }

        // innerBoundary が重なったり入れ子になっていないかチェックする
        for (ArrayList<MapLink> boundary : boundaries) {
            for (MapNode node : getBoundaryNodes(boundary)) {
                for (ArrayList<MapLink> _boundary : boundaries) {
                    if (_boundary == boundary) {
                        continue;
                    }
                    Path2D path = paths.get(_boundary);
                    if (path.contains(node.getX(), node.getY())) {
                        throw new Exception("Inner boundaries overlap.");
                    }
                }
            }
        }

        ArrayList<java.awt.geom.Point2D> coordinates = new ArrayList();
        for (MapNode node : outerNodes) {
            coordinates.add(new java.awt.geom.Point2D.Double(node.getX(), node.getY()));
        }
        OuterBoundary outerBoundary = new OuterBoundary(height, coordinates);

        ArrayList<InnerBoundary> innerBoundaries = new ArrayList();
        for (ArrayList<MapLink> boundary : boundaries) {
            coordinates.clear();
            for (MapNode node : getBoundaryNodes(boundary)) {
                coordinates.add(new java.awt.geom.Point2D.Double(node.getX(), node.getY()));
            }
            innerBoundaries.add(new InnerBoundary(coordinates));
        }

        int zIndex = 0;
        if (tags != null) {
            for (String tag : (List<String>)tags.clone()) {
                if (tag.startsWith("Z_INDEX_")) {
                    zIndex = Integer.parseInt(tag.substring(8));
                    tags.remove(tag);
                    break;
                }
            }
        }

        startOfCommandBlock();
        AddPolygon command = new AddPolygon(currentGroup, zIndex, outerBoundary, innerBoundaries);
        if (invoke(command)) {
            if (tags != null) {
                MapPolygon polygon = (MapPolygon)networkMap.getObject(command.getId());
                for (String tag : tags) {
                    if (! invoke(new AddTag(polygon, tag))) {
                        endOfCommandBlock();
                        return;
                    }
                }
            }

            // ポリゴンの元になったノードを削除する(連動してリンクも削除される)
            frame.getNodePanel().clearSelection();
            frame.getLinkPanel().clearSelection();
            for (MapNode node : getBoundaryNodes(_outerBoundary)) {
                node.selected = true;
            }
            for (ArrayList<MapLink> boundary : boundaries) {
                for (MapNode node : getBoundaryNodes(boundary)) {
                    node.selected = true;
                }
            }
            removeNodes(false);
        }
        endOfCommandBlock();
    }

    /**
     * ポリゴンをノードとリンクに変換する
     */
    private boolean convertToNodesAndLinks(int zIndex, double height, ArrayList<java.awt.geom.Point2D> coordinates, ArrayList<String> tags) {
        ArrayList<MapNode> nodes = new ArrayList();
        for (java.awt.geom.Point2D point : coordinates) {
            AddNode command = new AddNode(currentGroup, new Point2D(point.getX(), point.getY()), height);
            if (! invoke(command)) {
                return false;
            }
            MapNode node = (MapNode)networkMap.getObject(command.getId());
            for (String tag : tags) {
                if (! invoke(new AddTag(node, tag))) {
                    return false;
                }
            }
            if (zIndex != 0) {
                if (! invoke(new AddTag(node, "Z_INDEX_" + zIndex))) {
                    return false;
                }
            }
            nodes.add(node);
        }
        nodes.add(nodes.get(0));
        for (int index = 0; index < nodes.size() - 1; index++) {
            MapNode fromNode = nodes.get(index);
            MapNode toNode = nodes.get(index + 1);
            double length = fromNode.getPosition().distance(toNode.getPosition()) * currentGroup.getScale();
            AddLink command = new AddLink(fromNode, toNode, length, 1.0);
            if (! invoke(command)) {
                return false;
            }
            MapLink link = (MapLink)networkMap.getObject(command.getId());
            for (String tag : tags) {
                if (! invoke(new AddTag(link, tag))) {
                    return false;
                }
            }
            if (zIndex != 0) {
                if (! invoke(new AddTag(link, "Z_INDEX_" + zIndex))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * ポリゴンをノードとリンクに変換する
     */
    public void convertToNodesAndLinks(ArrayList<MapPolygon> polygons) {
        if (polygons.isEmpty()) {
            return;
        }

        startOfCommandBlock();
        for (MapPolygon polygon : (List<MapPolygon>)polygons.clone()) {
            if (polygon.isTriangleMeshes()) {
                continue;
            }

            // outerBoundary をノードとリンクに変換する
            int zIndex = polygon.getZIndex();
            ArrayList<String> tags = polygon.getTags();
            OuterBoundary outerBoundary = polygon.getOuterBoundary();
            double height = outerBoundary.getHeight();
            if (! convertToNodesAndLinks(zIndex, height, outerBoundary.getPoints(), tags)) {
                break;
            }

            // innerBoundary をノードとリンクに変換する
            for (InnerBoundary innerBoundary : polygon.getInnerBoundaries()) {
                if (! convertToNodesAndLinks(zIndex, height, innerBoundary.getPoints(), tags)) {
                    endOfCommandBlock();
                    return;
                }
            }

            // 変換が済んだポリゴンを削除する
            if (! invoke(new RemovePolygon(polygon))) {
                break;
            }
        }
        endOfCommandBlock();
    }

    /**
     * 選択中のポリゴンを削除する
     */
    public void removePolygons() {
        startOfCommandBlock();
        for (MapPolygon polygon : (List<MapPolygon>)getSelectedPolygons().clone()) {
            if (! invoke(new RemovePolygon(polygon))) {
                break;
            }
        }
        endOfCommandBlock();
    }

    /**
     * 経路探索をおこなって、その結果求められた各ノードのゴールまでの距離をタグとしてセットする
     */
    public boolean calcTagPaths(String goalTag) {
        Dijkstra.Result result = networkMap.calcGoalPath(NavigationHint.DefaultMentalMode, goalTag);
        if (result != null) {
            startOfCommandBlock();
            for (MapNode node : result.keySet()) {
                NavigationHint hint = result.get(node);
                String tag = String.format("%s: %.3f", goalTag, hint.distance);
                if (! node.hasTag(tag)) {
                    if (! invoke(new AddTag(node, tag))) {
                        break;
                    }
                }
            }
            endOfCommandBlock();
            return true;
        }
        return false;
    }

    /**
     * 他のノードと座標が重複しているノードを取得する
     * TODO: MapChecker に移動
     */
    public ArrayList<MapNode> getPiledNodes() {
        HashMap<Point3D, Boolean> collisions = new HashMap();
        for (MapNode node : networkMap.getNodes()) {
            java.awt.geom.Point2D coordinates = node.getPosition();
            Point3D point = new Point3D(coordinates.getX(), coordinates.getY(), node.getHeight());
            if (collisions.get(point) == null) {
                collisions.put(point, Boolean.FALSE);
            } else {
                collisions.put(point, Boolean.TRUE);
            }
        }
        ArrayList<MapNode> piledNodes = new ArrayList();
        for (MapNode node : networkMap.getNodes()) {
            java.awt.geom.Point2D coordinates = node.getPosition();
            Point3D point = new Point3D(coordinates.getX(), coordinates.getY(), node.getHeight());
            if (collisions.get(point)) {
                piledNodes.add(node);
            }
        }
        return piledNodes;
    }

    /**
     * ループしたリンクを取得する
     */
    public ArrayList<MapLink> getLoopedLinks() {
        ArrayList<MapLink> links = new ArrayList();
        for (MapLink link : networkMap.getLinks()) {
            if (link.getFrom() == link.getTo()) {
                links.add(link);
            }
        }
        return links;
    }

    /**
     * 長さ 0 (以下)のリンクを取得する
     */
    public ArrayList<MapLink> get0LengthLinks() {
        ArrayList<MapLink> links = new ArrayList();
        for (MapLink link : networkMap.getLinks()) {
            if (link.getLength() <= 0.0) {
                links.add(link);
            }
        }
        return links;
    }

    /**
     * 重複したリンクを取得する
     */
    public HashMap<String, ArrayList<MapLink>> getDuplicatedLinks() {
        HashMap<String, ArrayList<MapLink>> duplicatedLinks = new HashMap();
        for (MapLink link : networkMap.getLinks()) {
            MapNode fromNode = link.getFrom();
            MapNode toNode = link.getTo();
            for (MapLink _link : fromNode.getLinks()) {
                if (_link != link && _link.getOther(fromNode) == toNode) {
                    String key = null;
                    if (fromNode.getID().compareTo(toNode.getID()) < 0) {
                        key = fromNode.getID() + " " + toNode.getID();
                    } else {
                        key = toNode.getID() + " " + fromNode.getID();
                    }
                    ArrayList<MapLink> links = duplicatedLinks.get(key);
                    if (links == null) {
                        links = new ArrayList();
                        links.add(link);
                        links.add(_link);
                        duplicatedLinks.put(key, links);
                    } else {
                        if (! links.contains(link)) {
                            links.add(link);
                        }
                        if (! links.contains(_link)) {
                            links.add(_link);
                        }
                    }
                }
            }
        }
        return duplicatedLinks;
    }

    /**
     * 乱数オブジェクトを取得する
     */
    public Random getRandom() {
        return random;
    }

    /**
     * ウィンドウフレームを取得する
     */
    public EditorFrameFx getFrame() {
        return frame;
    }

    /**
     * マップファイルを読み書きするディレクトリをセットする
     */
    public void setDir(File file) {
        dir = file;
    }

    /**
     * マップファイルを読み書きするディレクトリを取得する
     */
    public File getDir() {
        return dir;
    }

    /**
     * マップファイルを読み書きするディレクトリのパス文字列を取得する
     */
    public String getPath() {
        try {
            return dir.getCanonicalPath();
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e) ;
        }
        return dir.getPath();
    }

    /**
     * 地図データを取得する
     */
    public NetworkMap getMap() {
        return networkMap;
    }

    /**
     * 現在編集中の Group をセットする
     */
    public void setCurrentGroup(MapPartGroup group) {
        currentGroup = group;
    }

    /**
     * 現在編集中の Group を取得する
     */
    public MapPartGroup getCurrentGroup() {
        return currentGroup;
    }

    /**
     * 背景画像をセットする
     */
    public void setBackgroundImage(MapPartGroup group, Image image) {
        backgroundImages.put(group, image);
    }

    /**
     * 背景画像を取得する
     */
    public Image getBackgroundImage(MapPartGroup group) {
        return backgroundImages.get(group);
    }

    /**
     * 地理院タイルを取得する
     */
    public ArrayList<GsiTile> getBackgroundMapTiles() {
        return gsiTiles;
    }

    /**
     * JavaFX 用の地理院タイル画像を取得する
     */
    public HashMap<GsiTile, Image> getGsiTileImages() {
        return gsiTileImages;
    }

    /**
     * 最低標高を取得する
     */
    public double getMinHeight() {
        return minHeight;
    }

    /**
     * 最大標高を取得する
     */
    public double getMaxHeight() {
        return maxHeight;
    }

    /**
     * 最低標高・最大標高を更新する
     */
    public void updateHeight() {
        minHeight = Double.MAX_VALUE;
        maxHeight = Double.MIN_VALUE;
        for (MapNode node : networkMap.getNodes()) {
            double height = node.getHeight();
            if (height < minHeight) {
                minHeight = height;
            }
            if (height > maxHeight) {
                maxHeight = height;
            }
        }
        if (minHeight == Double.MAX_VALUE) {
            MapPartGroup root = (MapPartGroup)networkMap.getRoot();
            minHeight = root.getMinHeight();
            maxHeight = root.getMaxHeight();
        }
    }

    /**
     * 設定ファイル関連の処理
     */

    public String getPropertiesFile() {
        return propertiesFile;
    }

    public void initSetupFileInfo() {
        setupFileInfo = new SetupFileInfo();
    }

    public SetupFileInfo getSetupFileInfo() { return setupFileInfo; }

    public void setNetworkMapFile(String _mapPath) {
        setupFileInfo.setNetworkMapFile(_mapPath);
        if (_mapPath == null || _mapPath.isEmpty()) {
            settings.put("mapDir", "");
            settings.put("mapFile", "");
        } else {
            File file = new File(_mapPath);
            String dirPath = file.getParent();
            settings.put("mapDir", (dirPath == null ? "." : dirPath) + File.separator);
            settings.put("mapFile", file.getName());
        }
    }

    public String getNetworkMapFile() {
        return setupFileInfo.getNetworkMapFile();
    }

    public void setPollutionFile(String _pollutionFile) {
        setupFileInfo.setPollutionFile(_pollutionFile);
        if (_pollutionFile == null || _pollutionFile.isEmpty()) {
            settings.put("obstructerDir", "");
            settings.put("obstructerFile", "");
        } else {
            File file = new File(_pollutionFile);
            String dirPath = file.getParent();
            settings.put("obstructerDir", (dirPath == null ? "." : dirPath) + File.separator);
            settings.put("obstructerFile", file.getName());
        }
    }

    public String getPollutionFile() {
        return setupFileInfo.getPollutionFile();
    }

    public void setGenerationFile(String _generationFile) {
        setupFileInfo.setGenerationFile(_generationFile);
        if (_generationFile == null || _generationFile.isEmpty()) {
            settings.put("generationDir", "");
            settings.put("generationFile", "");
        } else {
            File file = new File(_generationFile);
            String dirPath = file.getParent();
            settings.put("generationDir", (dirPath == null ? "." : dirPath) + File.separator);
            settings.put("generationFile", file.getName());
        }
    }

    public String getGenerationFile() {
        return setupFileInfo.getGenerationFile();
    }

    public void setScenarioFile(String _scenarioFile) {
        setupFileInfo.setScenarioFile(_scenarioFile);
        if (_scenarioFile == null || _scenarioFile.isEmpty()) {
            settings.put("scenarioDir", "");
            settings.put("scenarioFile", "");
        } else {
            File file = new File(_scenarioFile);
            String dirPath = file.getParent();
            settings.put("scenarioDir", (dirPath == null ? "." : dirPath) + File.separator);
            settings.put("scenarioFile", file.getName());
        }
    }

    public String getScenarioFile() {
        return setupFileInfo.getScenarioFile();
    }

    public void setFallbackFile(String _fallbackFile, ArrayList<String> _commandLineFallbacks) {
        setupFileInfo.setFallbackFile(_fallbackFile);
        setupFileInfo.scanFallbackFile(_commandLineFallbacks, true);
        if (_fallbackFile == null || _fallbackFile.isEmpty()) {
            settings.put("fallbackDir", "");
            settings.put("fallbackFile", "");
        } else {
            File file = new File(_fallbackFile);
            String dirPath = file.getParent();
            settings.put("fallbackDir", (dirPath == null ? "." : dirPath) + File.separator);
            settings.put("fallbackFile", file.getName());
        }
    }

    public String getFallbackFile() {
        return setupFileInfo.getFallbackFile();
    }

    /**
     * プロパティへの橋渡し。
     */
    public CrowdWalkPropertiesHandler getProperties() {
        return properties;
    }

    /**
     * ファイルからプロパティの読み込み。
     */
    public void setPropertiesFromFile(String propertiesFile) {
        setPropertiesFromFile(propertiesFile, commandLineFallbacks);
    }

    /**
     * ファイルからプロパティの読み込み。
     */
    public void setPropertiesFromFile(String _propertiesFile, ArrayList<String> _commandLineFallbacks) {
        properties = new CrowdWalkPropertiesHandler(_propertiesFile);
        propertiesFile = _propertiesFile;
        commandLineFallbacks = _commandLineFallbacks;

        // random
        random.setSeed(properties.getRandseed());
        // files
        initSetupFileInfo();
        setNetworkMapFile(properties.getNetworkMapFile());
        setPollutionFile(properties.getPollutionFile());
        setGenerationFile(properties.getGenerationFile());
        setScenarioFile(properties.getScenarioFile());
        setFallbackFile(properties.getFallbackFile(), commandLineFallbacks);

        try {
            dir = new File(properties.getPropertiesDirAbs()).getCanonicalFile();
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e);
        }
        File file = new File(propertiesFile);
        settings.put("propertiesDir", file.getParent() + File.separator);
        settings.put("propertiesFile", file.getName());
    }

    /**
     * プロパティの初期化。
     */
    public void initProperties(ArrayList<String> _commandLineFallbacks) {
        properties = new CrowdWalkPropertiesHandler();
        commandLineFallbacks = _commandLineFallbacks;

        // random
        random.setSeed(properties.getRandseed());
        // files
        initSetupFileInfo();
        setupFileInfo.scanFallbackFile(commandLineFallbacks, true);
    }

    /**
     * カレントディレクトリからの相対パスを返す
     */
    public String getRelativePath(File file) {
        String path = null;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e);
            return path;
        }
        String pwd = new File(".").getAbsoluteFile().getParent();
        if (path.startsWith(pwd)) {
            return path.substring(pwd.length() + 1);
        }
        return path;
    }
}
