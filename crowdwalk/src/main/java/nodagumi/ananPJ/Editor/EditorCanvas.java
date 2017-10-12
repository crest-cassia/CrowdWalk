package nodagumi.ananPJ.Editor;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;

import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Editor.EditorFrameFx.EditorMode;
import nodagumi.ananPJ.Editor.MapEditor.TextPosition;
import nodagumi.ananPJ.Gui.FxColor;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;

/**
 * マップエディタの編集画面の表示と GUI コントロール
 */
public class EditorCanvas extends Canvas {
    /**
     * ポリゴン描画用の座標配列を生成するビルダー
     */
    public class PolygonPointsBuilder {
        private ArrayList<Point2D> points = new ArrayList();
        private double[] xPoints = null;
        private double[] yPoints = null;

        public PolygonPointsBuilder() {}

        public void append(double x, double y) {
            points.add(new Point2D(x, y));
            xPoints = null;
            yPoints = null;
        }

        public double[] getXPoints() {
            if (xPoints == null) {
                xPoints = new double[points.size()];
                int index = 0;
                for (Point2D point : points) {
                    xPoints[index] = point.getX();
                    index++;
                }
            }
            return xPoints;
        }

        public double[] getYPoints() {
            if (yPoints == null) {
                yPoints = new double[points.size()];
                int index = 0;
                for (Point2D point : points) {
                    yPoints[index] = point.getY();
                    index++;
                }
            }
            return yPoints;
        }

        public int size() {
            return points.size();
        }

        public void clear() {
            points.clear();
            xPoints = null;
            yPoints = null;
        }
    }

    /**
     * Affine 変換の初期化データ
     */
    private static final Affine INIT_TRANSFORM = new Affine(1.0, 0.0, 0.0, 0.0, 1.0, 0.0);

    /**
     * Affine のスケールが 2.0 を超えた辺りからフォントの表示位置がずれ始めるため、
     * それを回避するためにスケールの倍率を変更する
     */
    public static final double SCALE_FACTOR = 100.0;

    /**
     * 実在の地図ベースのマップであると判断する最小リンク数
     */
    public static final int MINIMUM_REAL_MAP_LINKS = 300;

    /**
     * マップをセンタリングする際に付加するマージンドット数
     */
    private static final int CENTERING_MARGIN = 30;

    /**
     * ラベル表示に使用するフォント名
     */
    public static final String FONT_FAMILY = "Consolas";

    /**
     * エリアの表示色
     */
    public static Color gray8050 = new Color(0.8, 0.8, 0.8, 0.5);

    /**
     * マップエディタ
     */
    private MapEditor editor;

    /**
     * マップエディタのウィンドウフレーム
     */
    private EditorFrameFx frame;

    /**
     * 現在の編集モード
     */
    private EditorMode mode = EditorMode.EDIT_NODE;

    /**
     * 表示の On/Off フラグ
     */
    private boolean nodesShowing = true;
    private boolean nodeLabelsShowing = false;
    private boolean linksShowing = true;
    private boolean linkLabelsShowing = false;
    private boolean areasShowing = true;
    private boolean areaLabelsShowing = false;
    private boolean backgroundMapShowing = false;
    private boolean backgroundImageShowing = true;
    private boolean mapCoordinatesShowing = false;

    /**
     * 背景表示するグループ
     */
    private MapPartGroup backgroundGroup = null;

    /**
     * 表示スケール
     */
    private double scale = 1.0 / SCALE_FACTOR;  // 標準で 0.01

    /**
     * マップの回転角
     */
    private double angle = 0.0;

    /**
     * マップの回転を反映したノード座標の再計算フラグ
     */
    private boolean nodePointsUpdated = true;

    /**
     * マップの回転を反映したノード座標を保持する
     */
    private HashMap<MapNode, Point2D> rotatedPoints = new HashMap();

    /**
     * マップ座標(現在の scale で縮尺)をキャンバス原点に合わせるための移動量
     */
    private Point2D originTranslate = new Point2D(0, 0);

    /**
     * 表示位置をCanvasの中央に合わせるための調整値(Canvas座標系)
     */
    private Point2D adjustmentTranslate = new Point2D(0, 0);

    /**
     * 画面更新用フラグ
     */
    private boolean repainting = false;
    private volatile boolean redoRepainting = false;
    
    /**
     * 最後に検出したマウスカーソルの位置
     */
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;

    /**
     * マウスドラッグ中フラグ
     */
    private boolean dragging = false;

    /**
     * ノード移動
     */
    private boolean nodeMoving = false;
    private Point2D mousePressedPoint = null;
    private Point2D nodeMovingAmount = null;

    /**
     * ホバー表示対象の各オブジェクト
     */
    private MapNode pointedNode = null;
    private MapLink pointedLink = null;
    private MapArea pointedArea = null;

    /**
     * 仮リンクパラメータ
     */
    private MapNode fromNode = null;
    private MapNode toNode = null;

    /**
     * 一方通行の編集時に表示するラベル関連のパラメータ
     */
    private boolean oneWayIndicatorShowing = false;
    private MapNode oneWayfirstNode = null;
    private MapNode oneWaylastNode = null;
    private TextPosition oneWayLabelPositionA = TextPosition.CENTER;
    private TextPosition oneWayLabelPositionB = TextPosition.CENTER;

    /**
     * 矩形選択の始点座標
     */
    private Point2D selectionRangeStart = null;

    /**
     * 矩形選択範囲
     */
    private javafx.geometry.Rectangle2D selectionRange = null;

    /**
     * 選択範囲ポリゴン
     */
    private ArrayList<Point2D> selectionRangePolygon = new ArrayList();

    /**
     * コンストラクタ
     */
    public EditorCanvas(MapEditor editor, EditorFrameFx frame) {
        this.editor = editor;
        this.frame = frame;

        // サイズが変更されたら再描画する
        widthProperty().addListener(e -> repaintLater());
        heightProperty().addListener(e -> repaintLater());

        // キー操作の設定
        setKeyOperation();

        // マウス操作の設定
        setMouseOperation();
    }

    /**
     * キー操作の設定
     */
    private void setKeyOperation() {
        setFocusTraversable(true);
        addEventFilter(MouseEvent.ANY, e -> requestFocus());

        final ArrayList<KeyCode> functionKeys = new ArrayList();
        functionKeys.add(KeyCode.F1);
        functionKeys.add(KeyCode.F2);
        functionKeys.add(KeyCode.F3);
        functionKeys.add(KeyCode.F4);
        functionKeys.add(KeyCode.F5);
        functionKeys.add(KeyCode.F6);
        functionKeys.add(KeyCode.F7);
        functionKeys.add(KeyCode.F8);
        functionKeys.add(KeyCode.F9);

        // 何かのキーが押された
        setOnKeyPressed(event -> {
            // ESC
            if (event.getCode() == KeyCode.ESCAPE) {
                switch (mode) {
                case ADD_LINK:
                case ADD_NODE_LINK:
                    // ESC: ニュートラル
                    clearEditingStates();
                    repaintLater();
                    break;
                case EDIT_NODE:
                    // ESC: 選択を解除
                    frame.getNodePanel().clearSelection();
                    break;
                case EDIT_LINK:
                    // ESC: 選択を解除
                    frame.getLinkPanel().clearSelection();
                    break;
                case EDIT_AREA:
                    // ESC: 選択を解除
                    frame.getAreaPanel().clearSelection();
                    break;
                }
                event.consume();
            }

            // Ctrl + A
            if (event.isControlDown() && event.getCode() == KeyCode.A) {
                switch (mode) {
                case EDIT_NODE:
                    // Ctrl + A (編集画面): カレントグループの全ノードを選択
                    ArrayList<MapNode> nodes = new ArrayList();
                    for (MapNode node : editor.getCurrentGroup().getChildNodes()) {
                        if (frame.getNodePanel().getFilteredSet().contains(node)) {
                            nodes.add(node);
                        }
                    }
                    frame.getNodePanel().select(nodes);
                    break;
                case EDIT_LINK:
                    // Ctrl + A (編集画面): カレントグループの全リンクを選択
                    ArrayList<MapLink> links = new ArrayList();
                    for (MapLink link : editor.getCurrentGroup().getChildLinks()) {
                        if (frame.getLinkPanel().getFilteredSet().contains(link)) {
                            links.add(link);
                        }
                    }
                    frame.getLinkPanel().select(links);
                    break;
                case EDIT_AREA:
                    // Ctrl + A (編集画面): カレントグループの全エリアを選択
                    ArrayList<MapArea> areas = new ArrayList();
                    for (MapArea area : editor.getCurrentGroup().getChildMapAreas()) {
                        if (frame.getAreaPanel().getFilteredSet().contains(area)) {
                            areas.add(area);
                        }
                    }
                    frame.getAreaPanel().select(areas);
                    break;
                }
                event.consume();
            }

            // アプリケーションキー
            if (event.getCode() == KeyCode.CONTEXT_MENU) {
                if (! frame.isContextMenuShowing()) {
                    popupMenu(lastMouseX, lastMouseY, event.isControlDown());
                    event.consume();
                }
            }

            // ファンクションキー
            int fKeyIndex = functionKeys.indexOf(event.getCode());
            if (fKeyIndex != -1) {
                // Edit mode shortcut
                if (! event.isAltDown() && ! event.isControlDown() && ! event.isMetaDown() && ! event.isShiftDown()) {
                    frame.selectEditMode(fKeyIndex);
                }
                // Group selection shortcut
                else if (! event.isAltDown() && ! event.isControlDown() && ! event.isMetaDown() && event.isShiftDown()) {
                    frame.selectGroup(fKeyIndex);
                }
                event.consume();
            }

            // 何かキーを押したら矩形選択はキャンセルする
            if (selectionRangeStart != null) {
                selectionRangeStart = null;
                selectionRange = null;
                repaintLater();
            }
        });

        // 押されているキーが放された
        setOnKeyReleased(event -> {
            // ポリゴン選択の完了
            if (! event.isAltDown() && ! selectionRangePolygon.isEmpty()) {
                selection();
                selectionRangePolygon.clear();
                repaintLater();
            }
        });
    }

    /**
     * マウス操作の設定
     */
    private void setMouseOperation() {
        // カーソルを移動した時
        setOnMouseMoved(event -> {
            double x = event.getX();
            double y = event.getY();

            switch (mode) {
            case ADD_NODE:
            case ADD_NODE_LINK:
                if (lastMouseX != x || lastMouseY != y) {
                    repaintLater();
                    if (fromNode != null) {
                        MapPartGroup group = editor.getCurrentGroup();
                        Point2D point = pointConvertCanvasToMap(x, y);
                        double length = getRotatedPoint(fromNode).distance(point) * group.getScale();
                        frame.setCurrentLinkLength(length);
                    }
                }
                break;
            case ADD_LINK:
                if (fromNode == null) {
                    // 始点ノード(候補)の更新
                    if (updateTargetNode(x, y)) {
                        repaintLater();
                    }
                } else {
                    MapNode lastNode = toNode;
                    updateTentativeLink(x, y);
                    if (toNode != null && toNode != lastNode) {
                        repaintLater();
                        MapPartGroup group = editor.getCurrentGroup();
                        double length = fromNode.getAbsoluteCoordinates().distance(toNode.getAbsoluteCoordinates()) * group.getScale();
                        frame.setCurrentLinkLength(length);
                    }
                }
                break;
            case ADD_AREA:
                break;
            case EDIT_NODE:
                if (event.isAltDown() && ! selectionRangePolygon.isEmpty()) {
                    repaintLater();
                } else if (updateTargetNode(x, y)) {
                    repaintLater();
                }
                break;
            case EDIT_LINK:
                if (event.isAltDown() && ! selectionRangePolygon.isEmpty()) {
                    repaintLater();
                } else if (updateTargetLink(x, y)) {
                    repaintLater();
                }
                break;
            case EDIT_AREA:
                if (event.isAltDown() && ! selectionRangePolygon.isEmpty()) {
                    repaintLater();
                } else if (updateTargetArea(x, y)) {
                    repaintLater();
                }
                break;
            }
            lastMouseX = x;
            lastMouseY = y;

            if (mapCoordinatesShowing || mode == EditorMode.ADD_NODE) {
                Point2D mapPoint = convertToOriginal(pointConvertCanvasToMap(x, y));
                frame.setStatusText("Map coordinates: " + mapPoint.getX() + ", " + mapPoint.getY());
            }
        });

        // ボタンを押した時
        setOnMousePressed(event -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            mousePressedPoint = pointConvertCanvasToMap(lastMouseX, lastMouseY);
            dragging = false;
        });

        // ボタンを放した時
        setOnMouseReleased(event -> {
            if (selectionRange != null) {
                selection();
                selectionRangeStart = null;
                selectionRange = null;
                repaintLater();
            }
            if (nodeMoving) {
                nodeMoving = false;
                Point2D point = pointConvertCanvasToMap(event.getX(), event.getY());
                nodeMovingAmount = point.subtract(mousePressedPoint);
                if (nodeMovingAmount.getX() == 0.0 && nodeMovingAmount.getY() == 0.0) {
                    repaintLater();
                    return;
                }
                // 選択中の複数ノードを移動またはコピーする
                Point2D movingAmount = point.subtract(mousePressedPoint);
                frame.copyOrMoveNodes(movingAmount.getX(), movingAmount.getY(), 0.0);
            }
        });

        // ドラッグした時
        setOnMouseDragged(event -> {
            dragOperation(event);
            dragging = true;
        });

        // ボタンをクリックした時
        setOnMouseClicked(event -> {
            if (dragging) {
                return;
            }

            MapPartGroup group = editor.getCurrentGroup();
            Point2D mapPoint = pointConvertCanvasToMap(event.getX(), event.getY());
            switch (event.getButton()) {
            case PRIMARY:
                if (mode == EditorMode.ADD_NODE || mode == EditorMode.ADD_NODE_LINK) {
                    NetworkMap networkMap = editor.getMap();
                    if (networkMap.getGroups().size() == 1 && networkMap.getNodes().isEmpty()) {
                        Alert alert = new Alert(AlertType.CONFIRMATION, "Would you like to add the node?", ButtonType.YES, ButtonType.NO);
                        alert.getDialogPane().setHeaderText("If you create a node in the root group,\nyou will not be able to add groups.");
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.NO) {
                            repaintLater();
                            return;
                        }
                    }
                }
                switch (mode) {
                case ADD_NODE:
                    editor.invokeSingleCommand(new AddNode(group, convertToOriginal(mapPoint), group.getDefaultHeight()));
                    editor.updateHeight();
                    break;
                case ADD_LINK:
                    if (fromNode == null) {
                        // 始点ノードの設定
                        if (pointedNode == null) {
                            editor.ding("Point to the start node");
                        } else {
                            fromNode = pointedNode;
                            toNode = null;
                        }
                    } else {
                        // 新規リンク追加
                        if (toNode == null) {
                            editor.ding("Point to the end node");
                        } else {
                            double length = fromNode.getAbsoluteCoordinates().distance(toNode.getAbsoluteCoordinates()) * group.getScale();
                            frame.setCurrentLinkLength(length);
                            double width = frame.getCurrentLinkWidth();
                            if (width <= 0.0) {
                                return;
                            }
                            AddLink command = new AddLink(fromNode, toNode, length, width);
                            fromNode = toNode;
                            toNode = null;
                            editor.invokeSingleCommand(command);
                        }
                    }
                    break;
                case ADD_NODE_LINK:
                    editor.startOfCommandBlock();
                    if (editor.invoke(new AddNode(group, convertToOriginal(mapPoint), group.getDefaultHeight()))) {
                        updateTargetNode(mapPoint);     // pointedNode に今生成したノードをセット
                        if (pointedNode == null) {
                            fromNode = null;
                        } else {
                            if (fromNode == null) {
                                // 始点ノードの設定
                                fromNode = pointedNode;
                            } else {
                                // 新規リンク追加
                                double length = fromNode.getAbsoluteCoordinates().distance(pointedNode.getAbsoluteCoordinates()) * group.getScale();
                                frame.setCurrentLinkLength(length);
                                double width = frame.getCurrentLinkWidth();
                                if (width <= 0.0) {
                                    return;
                                }
                                AddLink command = new AddLink(fromNode, pointedNode, length, width);
                                if (editor.invoke(command)) {
                                    // 始点ノードの更新
                                    fromNode = pointedNode;
                                } else {
                                    clearEditingStates();
                                }
                            }
                        }
                    } else {
                        clearEditingStates();
                    }
                    editor.endOfCommandBlock();
                    editor.updateHeight();
                    break;
                case EDIT_NODE:
                    if (event.isAltDown()) {
                        // Alt + 左クリック: ポリゴンによるノード選択
                        if (! event.isControlDown() && selectionRangePolygon.isEmpty()) {
                            selectionRangePolygon.add(mapPoint);
                            frame.getNodePanel().clearSelection();
                        } else {
                            selectionRangePolygon.add(mapPoint);
                            repaintLater();
                        }
                    } else {
                        if (pointedNode == null) {
                            frame.getNodePanel().clearSelection();
                        } else {
                            if (event.isControlDown()) {
                                // Ctrl + 左クリック: 追加でノード選択 / 選択解除
                                if (pointedNode.selected) {
                                    frame.getNodePanel().clearSelection(pointedNode);
                                } else {
                                    frame.getNodePanel().select(pointedNode);
                                }
                            } else {
                                // 左クリック: ノード選択
                                frame.getNodePanel().clearAndSelect(pointedNode);
                            }
                        }
                    }
                    break;
                case EDIT_LINK:
                    if (event.isAltDown()) {
                        // Alt + 左クリック: ポリゴンによるリンク選択
                        if (! event.isControlDown() && selectionRangePolygon.isEmpty()) {
                            selectionRangePolygon.add(mapPoint);
                            frame.getLinkPanel().clearSelection();
                        } else {
                            selectionRangePolygon.add(mapPoint);
                            repaintLater();
                        }
                    } else {
                        if (pointedLink == null) {
                            frame.getLinkPanel().clearSelection();
                        } else {
                            if (event.isControlDown()) {
                                // Ctrl + 左クリック: 追加でリンク選択 / 選択解除
                                if (pointedLink.selected) {
                                    frame.getLinkPanel().clearSelection(pointedLink);
                                } else {
                                    frame.getLinkPanel().select(pointedLink);
                                }
                            } else {
                                // 左クリック: リンク選択
                                frame.getLinkPanel().clearAndSelect(pointedLink);
                            }
                        }
                    }
                    break;
                case EDIT_AREA:
                    if (event.isAltDown()) {
                        // Alt + 左クリック: ポリゴンによるエリア選択
                        if (! event.isControlDown() && selectionRangePolygon.isEmpty()) {
                            selectionRangePolygon.add(mapPoint);
                            frame.getAreaPanel().clearSelection();
                        } else {
                            selectionRangePolygon.add(mapPoint);
                            repaintLater();
                        }
                    } else {
                        if (pointedArea == null) {
                            frame.getAreaPanel().clearSelection();
                        } else {
                            if (event.isControlDown()) {
                                // Ctrl + 左クリック: 追加でエリア選択 / 選択解除
                                if (pointedArea.selected) {
                                    frame.getAreaPanel().clearSelection(pointedArea);
                                } else {
                                    frame.getAreaPanel().select(pointedArea);
                                }
                            } else {
                                // 左クリック: エリア選択
                                frame.getAreaPanel().clearAndSelect(pointedArea);
                            }
                        }
                    }
                    break;
                }
                break;
            case SECONDARY:
                popupMenu(event);
                break;
            }
        });

        // ホイールを回した時
        setOnScroll(event -> {
            if (event.isShiftDown()) {
                // ※JavaFX のバグ(?)のため Sift キー押下時はスクロール量が取得できない
                return;
            }
            wheelOperation(event);
        });
    }

    /**
     * マウスドラッグ操作
     */
    private void dragOperation(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        switch (event.getButton()) {
        case PRIMARY:
            MapPartGroup group = editor.getCurrentGroup();
            switch (mode) {
            case EDIT_NODE:
                if (event.isAltDown() || event.isMetaDown()) {
                    break;
                }
                if (event.isShiftDown()) {
                    // Shift + 左クリックでドラッグ: ノードの移動
                    if (editor.getCountOfSelectedNodes() >= 2) {
                        // 選択中の複数ノードの移動中
                        if (! nodeMoving) {
                            if (! editor.isSingleGroup(editor.getSelectedNodes())) {
                                Alert alert = new Alert(AlertType.WARNING, "When moving multiple nodes, it must be a single group.", ButtonType.OK);
                                alert.showAndWait();
                                break;
                            }
                            if (editor.getOperableNodes(true).isEmpty()) {
                                Alert alert = new Alert(AlertType.WARNING, "When moving multiple nodes, it must be the current group.", ButtonType.OK);
                                alert.showAndWait();
                                break;
                            }
                            nodeMoving = true;
                        }
                        nodeMovingAmount = pointConvertCanvasToMap(x, y).subtract(mousePressedPoint);
                        repaintLater();
                    } else {
                        if (pointedNode != null) {
                            Point2D mapPoint = convertToOriginal(pointConvertCanvasToMap(x, y));
                            editor.invokeSingleCommand(new MoveNode(pointedNode, mapPoint.getX(), mapPoint.getY()));
                        }
                    }
                } else {
                    // 左クリックでドラッグ: 矩形範囲内のリンク選択
                    updateSelectionRange(x, y, event.isControlDown());
                }
                break;
            case EDIT_LINK:
                if (event.isAltDown() || event.isMetaDown() || event.isShiftDown()) {
                    break;
                }
                // 左クリックでドラッグ: 矩形範囲内のリンク選択
                updateSelectionRange(x, y, event.isControlDown());
                break;
            case EDIT_AREA:
                if (event.isAltDown() || event.isMetaDown() || event.isShiftDown()) {
                    break;
                }
                // 左クリックでドラッグ: 矩形範囲内のエリア選択
                updateSelectionRange(x, y, event.isControlDown());
                break;
            case BACKGROUND_IMAGE:
                if (event.isAltDown() || event.isControlDown() || event.isMetaDown()) {
                    break;
                }
                if (event.isShiftDown() && editor.getBackgroundImage(group) != null) {
                    // Shift + 左クリックでドラッグ: 背景画像の移動
                    double dx = x - lastMouseX;
                    double dy = y - lastMouseY;
                    Point2D movingAmount = convertToOriginal(dx, dy);
                    editor.invokeSingleCommand(new MoveBackgroundImage(group, group.tx + movingAmount.getX() / (scale * SCALE_FACTOR), group.ty + movingAmount.getY() / (scale * SCALE_FACTOR)));
                }
                break;
            default:
                break;
            }
            break;
        case SECONDARY:
            if (event.isAltDown() || event.isControlDown() || event.isMetaDown() || event.isShiftDown()) {
                break;
            }
            // 右クリックでドラッグ: マップのスクロール
            scroll(x, y);
            break;
        }
        lastMouseX = x;
        lastMouseY = y;
    }

    /**
     * 選択範囲を更新する
     */
    private void updateSelectionRange(double x, double y, boolean isControlDown) {
        if (selectionRangeStart == null) {
            selectionRangeStart = pointConvertCanvasToMap(x, y);
            if (! isControlDown) {
                switch (mode) {
                case EDIT_NODE:
                    frame.getNodePanel().clearSelection();
                    break;
                case EDIT_LINK:
                    frame.getLinkPanel().clearSelection();
                    break;
                case EDIT_AREA:
                    frame.getAreaPanel().clearSelection();
                    break;
                }
            }
        } else {
            selectionRange = getNormalizedRectangle(selectionRangeStart, pointConvertCanvasToMap(x, y));
            repaintLater();
        }
    }

    /**
     * 矩形またはポリゴン内のオブジェクトを選択する
     */
    private void selection() {
        Polygon polygon = new Polygon();
        if (selectionRange != null) {
            polygon.addPoint((int)selectionRange.getMinX(), (int)selectionRange.getMinY());
            polygon.addPoint((int)selectionRange.getMaxX(), (int)selectionRange.getMinY());
            polygon.addPoint((int)selectionRange.getMaxX(), (int)selectionRange.getMaxY());
            polygon.addPoint((int)selectionRange.getMinX(), (int)selectionRange.getMaxY());
        } else if (! selectionRangePolygon.isEmpty()) {
            for (Point2D point : selectionRangePolygon) {
                polygon.addPoint((int)point.getX(), (int)point.getY());
            }
        } else {
            return;
        }

        switch (mode) {
        case EDIT_NODE:
            ArrayList<MapNode> nodes = new ArrayList();
            for (MapNode node : editor.getCurrentGroup().getChildNodes()) {
                if (frame.getNodePanel().getFilteredSet().contains(node)) {
                    java.awt.geom.Point2D point = toAwtPoint2D(getRotatedPoint(node));
                    if (polygon.contains(point)) {
                        nodes.add(node);
                    }
                }
            }
            frame.getNodePanel().select(nodes);
            break;
        case EDIT_LINK:
            ArrayList<MapLink> links = new ArrayList();
            for (MapLink link : editor.getCurrentGroup().getChildLinks()) {
                if (frame.getLinkPanel().getFilteredSet().contains(link)) {
                    java.awt.geom.Point2D fromPoint = toAwtPoint2D(getRotatedPoint(link.getFrom()));
                    java.awt.geom.Point2D toPoint = toAwtPoint2D(getRotatedPoint(link.getTo()));
                    if (polygon.contains(fromPoint) && polygon.contains(toPoint)) {
                        links.add(link);
                    }
                }
            }
            frame.getLinkPanel().select(links);
            break;
        case EDIT_AREA:
            ArrayList<MapArea> areas = new ArrayList();
            for (MapArea area : editor.getCurrentGroup().getChildMapAreas()) {
                if (frame.getAreaPanel().getFilteredSet().contains(area)) {
                    if (contains(polygon, area, true)) {
                        areas.add(area);
                    }
                }
            }
            frame.getAreaPanel().select(areas);
            break;
        }
    }

    /**
     * MapArea の頂点座標リストを取得する
     */
    private ArrayList<Point2D> getPointsOfRectangle(MapArea area) {
        Rectangle2D rect = (Rectangle2D)area.getShape();
        ArrayList<Point2D> points = new ArrayList();
        points.add(calcRotatedPoint(rect.getMinX(), rect.getMinY()));
        points.add(calcRotatedPoint(rect.getMaxX(), rect.getMinY()));
        points.add(calcRotatedPoint(rect.getMaxX(), rect.getMaxY()));
        points.add(calcRotatedPoint(rect.getMinX(), rect.getMaxY()));
        return points;
    }

    /**
     * マップ座標の回転を反映した contains
     */
    private boolean contains(Shape shape, MapArea area, boolean andConditional) {
        for (Point2D point : getPointsOfRectangle(area)) {
            if (andConditional) {
                if (! shape.contains(toAwtPoint2D(point))) {
                    return false;
                }
            } else {
                if (shape.contains(toAwtPoint2D(point))) {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * 正規化された Rectangle を返す
     */
    public static javafx.geometry.Rectangle2D getNormalizedRectangle(Point2D p1, Point2D p2) {
        double x = p1.getX();
        double y = p1.getY();
        double width = p2.getX() - x;
        double height = p2.getY() - y;

        if (x > p2.getX()) {
            width = x - p2.getX();
            x = p2.getX();
        }
        if (y > p2.getY()) {
            height = y - p2.getY();
            y = p2.getY();
        }
        return new javafx.geometry.Rectangle2D(x, y, width, height);
    }

    /**
     * マウスホイール操作
     */
    private void wheelOperation(ScrollEvent event) {
        switch (mode) {
        case BACKGROUND_IMAGE:
            MapPartGroup group = editor.getCurrentGroup();
            if (editor.getBackgroundImage(group) == null) {
                break;
            }
            if (event.isAltDown()) {
                double angle = group.r * 180.0 / Math.PI;
                double delta = event.isControlDown() ? 0.1 : 1.0;
                if (event.getDeltaY() > 0.0) {
                    angle -= delta;
                } else if (event.getDeltaY() < 0.0) {
                    angle += delta;
                }
                editor.invokeSingleCommand(new RotateBackgroundImage(group, angle * Math.PI / 180.0));
            } else {
                double scaleX = group.sx;
                double scaleY = group.sy;
                double delta = event.isControlDown() ? 1.01 : 1.1;
                if (event.getDeltaY() > 0.0) {
                    scaleX *= delta;
                    scaleY *= delta;
                } else if (event.getDeltaY() < 0.0) {
                    scaleX /= delta;
                    scaleY /= delta;
                }
                editor.invokeSingleCommand(new ScaleTheBackgroundImage(group, scaleX, scaleY));
            }
            break;
        default:
            if (event.isAltDown()) {
                // Alt + ホイール: 編集画面の回転
                rotate(event.getX(), event.getY(), event.getDeltaY(), event.isControlDown());
            } else {
                // ホイール: マップ表示の拡大・縮小
                zoom(event.getX(), event.getY(), event.getDeltaY(), event.isControlDown());
            }
            break;
        }
        lastMouseX = event.getX();
        lastMouseY = event.getY();
    }

    /**
     * コンテキストメニューを表示する
     */
    private void popupMenu(MouseEvent event) {
        popupMenu(event.getScreenX(), event.getScreenY(), event.isControlDown());
    }

    /**
     * コンテキストメニューを表示する
     */
    private void popupMenu(double x, double y, boolean ctrlDown) {
        switch (mode) {
        case ADD_LINK:
        case ADD_NODE_LINK:
            // 右クリック: ニュートラル
            // コンテキストメニューを表示する代わりにノード選択をクリアする
            clearEditingStates();
            repaintLater();
            break;
        case EDIT_NODE:
            // 右クリック: コンテキストメニュー表示
            if (pointedNode != null) {
                if (! pointedNode.selected) {
                    if (ctrlDown) {
                        frame.getNodePanel().select(pointedNode);
                    } else {
                        frame.getNodePanel().clearAndSelect(pointedNode);
                    }
                }
            }
            if (editor.getCountOfSelectedNodes() > 0) {
                frame.getEditNodeMenu().show(getStage(), x, y);
            }
            break;
        case EDIT_LINK:
            // 右クリック: コンテキストメニュー表示
            if (pointedLink != null) {
                if (! pointedLink.selected) {
                    if (ctrlDown) {
                        frame.getLinkPanel().select(pointedLink);
                    } else {
                        frame.getLinkPanel().clearAndSelect(pointedLink);
                    }
                }
            }
            if (editor.getCountOfSelectedLinks() > 0) {
                frame.getEditLinkMenu().show(getStage(), x, y);
            }
            break;
        case EDIT_AREA:
            // 右クリック: コンテキストメニュー表示
            if (pointedArea != null) {
                if (! pointedArea.selected) {
                    if (ctrlDown) {
                        frame.getAreaPanel().select(pointedArea);
                    } else {
                        frame.getAreaPanel().clearAndSelect(pointedArea);
                    }
                }
            }
            if (editor.getCountOfSelectedAreas() > 0) {
                frame.getEditAreaMenu().show(getStage(), x, y);
            }
            break;
        case BACKGROUND_IMAGE:
            // 右クリック: コンテキストメニュー表示
            frame.setBgImageMenuStatus();
            frame.getBgImageMenu().show(getStage(), x, y);
            break;
        }
    }

    /**
     * マップを縦横にスクロールする
     */
    private void scroll(double x, double y) {
        double dx = x - lastMouseX;
        double dy = y - lastMouseY;
        adjustmentTranslate = adjustmentTranslate.add(dx, dy);
        repaintLater();
    }

    /**
     * マップを回転する
     */
    public void rotate(double x, double y, double angle) {
        Point2D mapPoint = convertToOriginal(pointConvertCanvasToMap(x, y));
        this.angle = angle;
        Point2D point = calcRotatedPoint(mapPoint);
        setTranslate(point.getX(), point.getY());
        adjustmentTranslate = new Point2D(x, y);

        frame.setStatusText("Rotation angle: " + angle);
        nodePointsUpdated = true;
        repaintLater();
    }

    /**
     * マップを回転する
     */
    private void rotate(double x, double y, double deltaY, boolean controlDown) {
        double angle = this.angle;
        double delta = controlDown ? 0.1 : 1.0;     // Control キーが押されていたら 0.1 度刻み

        if (deltaY > 0.0) {
            angle -= delta;
        } else if (deltaY < 0.0) {
            angle += delta;
        }
        rotate(x, y, angle);
    }

    /**
     * マウスカーソル位置を基準にズームイン/ズームアウトする
     */
    private void zoom(double x, double y, double deltaY, boolean controlDown) {
        Point2D _origin = originTranslate.add(adjustmentTranslate).add(-x, -y);
        double ox = _origin.getX() / scale;
        double oy = _origin.getY() / scale;

        if (deltaY > 0.0) {
            scale *= (controlDown ? 1.01 : 1.1);
        } else if (deltaY < 0.0) {
            scale /= (controlDown ? 1.01 : 1.1);
        }
        scale = Math.min(scale, 100.0 / SCALE_FACTOR);
        scale = Math.max(scale, 0.01 / SCALE_FACTOR);
        frame.setStatusText("Scale: " + scale);

        originTranslate = new Point2D(ox * scale, oy * scale);
        adjustmentTranslate = new Point2D(x, y);
        repaintLater();
    }

    /**
     * マップの回転を反映した座標を求める
     */
    private Point2D calcRotatedPoint(double x, double y) {
        double r = Math.toRadians(angle);
        return new Point2D(x * Math.cos(r) - y * Math.sin(r), x * Math.sin(r) + y * Math.cos(r));
    }

    /**
     * マップの回転を反映した座標を求める
     */
    private Point2D calcRotatedPoint(Point2D point) {
        return calcRotatedPoint(point.getX(), point.getY());
    }

    /**
     * マップの回転を反映した座標を求める
     */
    private Point2D calcRotatedPoint(java.awt.geom.Point2D point) {
        return calcRotatedPoint(point.getX(), point.getY());
    }

    /**
     * マップの回転を反映したノード座標を再計算する
     */
    private void recalcRotatedPoints() {
        double r = Math.toRadians(angle);
        NetworkMap networkMap = editor.getMap();
        rotatedPoints.clear();
        for (MapNode node : networkMap.getNodes()) {
            double x = node.getX() * Math.cos(r) - node.getY() * Math.sin(r);
            double y = node.getX() * Math.sin(r) + node.getY() * Math.cos(r);
            rotatedPoints.put(node, new Point2D(x, y));
        }
    }

    /**
     * マップの回転を反映したノード座標を取得する
     */
    public Point2D getRotatedPoint(MapNode node) {
        Point2D point = rotatedPoints.get(node);
        if (point == null) {
            point = calcRotatedPoint(node.getX(), node.getY());
            rotatedPoints.put(node, point);
        }
        return point;
    }

    /**
     * マップの回転を反映したノード座標の X を取得する
     */
    public double getRotatedX(MapNode node) {
        return getRotatedPoint(node).getX();
    }

    /**
     * マップの回転を反映したノード座標の Y を取得する
     */
    public double getRotatedY(MapNode node) {
        return getRotatedPoint(node).getY();
    }

    /**
     * リンクが矩形領域の内部と交差しているか?
     */
    private boolean intersectsLine(Rectangle2D rectangle, MapLink link) {
        return rectangle.intersectsLine(
            getRotatedX(link.getFrom()), getRotatedY(link.getFrom()),
            getRotatedX(link.getTo()), getRotatedY(link.getTo())
        );
    }

    /**
     * マップの回転を反映した座標から元の座標を求める
     */
    public Point2D convertToOriginal(double x, double y) {
        double r = Math.toRadians(-angle);
        return new Point2D(x * Math.cos(r) - y * Math.sin(r), x * Math.sin(r) + y * Math.cos(r));
    }

    /**
     * マップの回転を反映した座標から元の座標を求める
     */
    public Point2D convertToOriginal(Point2D point) {
        return convertToOriginal(point.getX(), point.getY());
    }

    /**
     * マップの回転を反映したノード座標を再計算する
     */
    public void updateNodePoints() {
        nodePointsUpdated = true;
    }

    /**
     * javafx.geometry.Point2D 型の値を java.awt.geom.Point2D 型に変換する
     */
    private java.awt.geom.Point2D toAwtPoint2D(Point2D point) {
        return new java.awt.geom.Point2D.Double(point.getX(), point.getY());
    }

    /**
     * キャンバス座標をマップ座標に変換する
     */
    public Point2D pointConvertCanvasToMap(double x, double y) {
        Point2D origin = originTranslate.add(adjustmentTranslate).add(-x, -y);
        return new Point2D(-origin.getX() / (scale * SCALE_FACTOR), -origin.getY() / (scale * SCALE_FACTOR));
    }

    /**
     * マウスカーソル上のマップ座標を取得する
     */
    public Point2D getMapPointOnTheMouseCursor() {
        return pointConvertCanvasToMap(lastMouseX, lastMouseY);
    }

    /**
     * Canvas の中央に表示する座標をセットする
     */
    public void setTranslate(double tx, double ty) {
        originTranslate = new Point2D(-tx * scale * SCALE_FACTOR, -ty * scale * SCALE_FACTOR);
    }

    /**
     * マップのセンタリングとスケーリング
     */
    public void centering(boolean withScaling) {
        Rectangle2D bounds = calcRotatedMapRectangle();
        double cx = bounds.getMinX() + bounds.getWidth() / 2.0;
        double cy = bounds.getMinY() + bounds.getHeight() / 2.0;
        cx *= SCALE_FACTOR;
        cy *= SCALE_FACTOR;

        // map is empty
        if (cx == 0.0 && cy == 0.0) {
            scale = 1.0 / SCALE_FACTOR;
            originTranslate = new Point2D(0.0, 0.0);
            adjustmentTranslate = new Point2D(getWidth() / 2, getHeight() / 2);
            return;
        }

        if (withScaling) {
            int centeringMargin = 0;
            // リアルマップにはマージンを付加しない
            if (editor.getMap().getLinks().size() < MINIMUM_REAL_MAP_LINKS) {
                centeringMargin = CENTERING_MARGIN;
            }
            double scaleX = (getWidth() - centeringMargin * 2) / (bounds.getWidth() * SCALE_FACTOR);
            double scaleY = (getHeight() - centeringMargin * 2) / (bounds.getHeight() * SCALE_FACTOR);
            scale = Math.min(scaleX, scaleY);
        }

        // -(マップの中心座標に相当するCanvas座標)
        originTranslate = new Point2D(-cx * scale, -cy * scale);
        // 表示位置をCanvasの中央に合わせる
        adjustmentTranslate = new Point2D(getWidth() / 2, getHeight() / 2);
    }

    /**
     * マップに外接する矩形を算出する
     */
    public Rectangle2D calcRotatedMapRectangle() {
        double north = 0.0;
        double south = 0.0;
        double west = 0.0;
        double east = 0.0;
        for (MapNode node : editor.getMap().getNodes()) {
            Point2D point = calcRotatedPoint(node.getX(), node.getY());
            if (north == 0.0 && south == 0.0) {
                north = point.getY();
                south = point.getY();
                west = point.getX();
                east = point.getX();
            }
            if (point.getY() < north) {
                north = point.getY();
            }
            if (point.getY() > south) {
                south = point.getY();
            }
            if (point.getX() < west) {
                west = point.getX();
            }
            if (point.getX() > east) {
                east = point.getX();
            }
        }
        return new Rectangle2D.Double(west, north, east - west, south - north);
    }

    /**
     * マウスカーソル上のノードを pointedNode にセットする
     */
    private boolean updateTargetNode(double x, double y) {
        return updateTargetNode(pointConvertCanvasToMap(x, y));
    }

    /**
     * マップ座標 point から16ピクセル以内にある一番近いノードを pointedNode にセットする
     */
    private boolean updateTargetNode(Point2D point) {
        java.awt.geom.Point2D p = toAwtPoint2D(convertToOriginal(point.getX(), point.getY()));
        double mindist = Double.POSITIVE_INFINITY;
        MapNode pointedNodeCandidate = null;
        double radius = 16.0  / (scale * SCALE_FACTOR);

        for (MapNode node : editor.getCurrentGroup().getChildNodes()) {
            if (mode != EditorMode.ADD_NODE_LINK && ! frame.getNodePanel().getFilteredSet().contains(node)) {
                continue;
            }
            double dist = p.distance(node.getAbsoluteCoordinates());
            if (dist < mindist && dist < radius) {
                pointedNodeCandidate = node;
                mindist = dist;
            }
        }

        boolean updated = (pointedNode != pointedNodeCandidate);
        pointedNode = pointedNodeCandidate;

        return updated;
    }

    /**
     * マウスカーソルから16ピクセル以内にある一番近いリンクを pointedLink にセットする
     */
    private boolean updateTargetLink(double x, double y) {
        Point2D upperLeft = pointConvertCanvasToMap(0.0, 0.0);
        Point2D lowerRight = pointConvertCanvasToMap(getWidth(), getHeight());
        Rectangle2D bounds = new Rectangle2D.Double(upperLeft.getX(), upperLeft.getY(), lowerRight.getX() - upperLeft.getX(), lowerRight.getY() - upperLeft.getY());

        java.awt.geom.Point2D point = toAwtPoint2D(pointConvertCanvasToMap(x, y));
        double mindist = 16.0  / (scale * SCALE_FACTOR);
        MapLink pointedLinkCandidate = null;

        for (MapLink link : editor.getCurrentGroup().getChildLinks()) {
            if (! frame.getLinkPanel().getFilteredSet().contains(link)) {
                continue;
            }
            MapNode from = link.getFrom();
            MapNode to = link.getTo();
            Line2D line = new Line2D.Double(getRotatedX(from), getRotatedY(from), getRotatedX(to), getRotatedY(to));
            if (! bounds.intersectsLine(line)) {
                continue;
            }
            double dist = line.ptSegDist(point);
            if (dist < mindist) {
                pointedLinkCandidate = link;
                mindist = dist;
            }
        }

        boolean updated = (pointedLink != pointedLinkCandidate);
        pointedLink = pointedLinkCandidate;

        return updated;
    }

    /**
     * マウスカーソル上のエリアを pointedArea にセットする
     */
    private boolean updateTargetArea(double x, double y) {
        java.awt.geom.Point2D point = toAwtPoint2D(convertToOriginal(pointConvertCanvasToMap(x, y)));
        MapArea pointedAreaCandidate = null;

        for (MapArea area : editor.getCurrentGroup().getChildMapAreas()) {
            if (! frame.getAreaPanel().getFilteredSet().contains(area)) {
                continue;
            }
            if (area.getShape().contains(point)) {
                pointedAreaCandidate = area;
                break;
            }
        }

        boolean updated = (pointedArea != pointedAreaCandidate);
        pointedArea = pointedAreaCandidate;

        return updated;
    }

    /**
     * 仮リンク(を構成する toNode)を決める
     */
    private void updateTentativeLink(double x, double y) {
        java.awt.geom.Point2D point = toAwtPoint2D(convertToOriginal(pointConvertCanvasToMap(x, y)));
        MapPartGroup group = editor.getCurrentGroup();

        MapNodeTable nodes = group.getChildNodesAndSymlinks(fromNode, fromNode.getQuadrant(point));
        MapLinkTable links = group.getChildLinks();
        double minDistance = -1.0;
        for (MapNode toNode : nodes) {
            Line2D line = new Line2D.Double(fromNode.getAbsoluteCoordinates(), toNode.getAbsoluteCoordinates());
            if (line.ptSegDist(point) < 5.0 / (scale * SCALE_FACTOR)) {
                double distance = line.getP1().distance(line.getP2());
                if (minDistance == -1.0 || distance < minDistance) {
                    /* check for existing links */
                    boolean exists = false;
                    for (MapLink link : links) {
                        if ((link.getFrom() == fromNode && link.getTo() == toNode) ||
                                (link.getTo() == fromNode && link.getFrom() == toNode)) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) {
                        continue;
                    }
                    this.toNode = toNode;
                    minDistance = distance;
                }
            }
        }
    }

    /**
     * 必要な場合のみ再描画する
     */
    public synchronized void repaintLater() {
        if (repainting) {
            redoRepainting = true;
            return;
        }
        repainting = true;
        final EditorCanvas canvas = this;
        Platform.runLater(() -> {
            while (true) {
                repaint();
                synchronized (canvas) {
                    if (! redoRepainting) {
                        repainting = false;
                        break;
                    }
                }
            }
        });
    }

    /**
     * 再描画
     */
    public void repaint() {
        if (nodePointsUpdated) {
            recalcRotatedPoints();
            nodePointsUpdated = false;
        }

        // viewArea を使って表示範囲外の無駄な描画処理を省く
        Point2D upperLeft = pointConvertCanvasToMap(0.0, 0.0);
        Point2D lowerRight = pointConvertCanvasToMap(getWidth(), getHeight());
        Rectangle2D viewArea = new Rectangle2D.Double(upperLeft.getX(), upperLeft.getY(), lowerRight.getX() - upperLeft.getX(), lowerRight.getY() - upperLeft.getY());

        // Canvas のクリア
        GraphicsContext gc = getGraphicsContext2D();
        gc.setTransform(INIT_TRANSFORM);
        gc.setFill(Color.WHITESMOKE /* web("#ececec") */);
        gc.fillRect(0.0, 0.0, getWidth(), getHeight());

        MapPartGroup group = editor.getCurrentGroup();
        if (group == null) {
            return;
        }

        Affine originalAffine = new Affine();
        gc.getTransform(originalAffine);

        Affine affine = new Affine();
        affine.appendTranslation(originTranslate.getX(), originTranslate.getY());
        affine.appendScale(scale, scale);
        // 表示位置をCanvasの中央に合わせる(そのために scale を相殺している)
        affine.appendTranslation(adjustmentTranslate.getX() / scale, adjustmentTranslate.getY() / scale);
        gc.transform(affine);

        Font font = Font.font(14 / scale);
        gc.setFont(font);

        redoRepainting = false;

        // 背景地図の描画
        if (backgroundMapShowing && ! editor.getBackgroundMapTiles().isEmpty()) {
            for (GsiTile gsiTile : editor.getBackgroundMapTiles()) {
                drawBackgroundMapTile(gsiTile, editor.getGsiTileImages().get(gsiTile), gc);
                if (redoRepainting) {
                    return;
                }
            }
        }

        // 背景画像の描画
        if (backgroundImageShowing) {
            drawBackgroundImage(group, gc);
            if (redoRepainting) {
                return;
            }
        }

        // 背景グループの描画
        if (backgroundGroup != null && backgroundGroup != group) {
            drawBackgroundGroup(backgroundGroup, gc, viewArea);
            if (redoRepainting) {
                return;
            }
        }

        /* actual objects */
        if (areasShowing) {
            for (MapArea area : group.getChildMapAreas()) {
                if (frame.getAreaPanel().getFilteredSet().contains(area) && contains(viewArea, area, false)) {
                    drawArea(area, gc, areaLabelsShowing, false);
                    if (redoRepainting) {
                        return;
                    }
                }
            }
        }
        if (linksShowing) {
            for (MapLink link : group.getChildLinks()) {
                if (intersectsLine(viewArea, link) && frame.getLinkPanel().getFilteredSet().contains(link)) {
                    drawLink(link, gc, linkLabelsShowing, false, false);
                    if (redoRepainting) {
                        return;
                    }
                }
            }
        }
        if (nodesShowing) {
            for (MapNode node : group.getChildNodes()) {
                if (viewArea.contains(getRotatedX(node), getRotatedY(node)) && frame.getNodePanel().getFilteredSet().contains(node)) {
                    drawNode(node, gc, nodeLabelsShowing, false, false);
                    if (redoRepainting) {
                        return;
                    }
                }
            }
        }

        /* symbolic links */
        /* first try links */
        ArrayList<OBNodeSymbolicLink> symbolicLinks = group.getSymbolicLinks();
        if (linksShowing) {
            for (OBNodeSymbolicLink symlink : symbolicLinks) {
                OBNode orig = symlink.getOriginal();
                if (orig == null) {
                    continue;
                }
                if (orig.getNodeType() == OBNode.NType.LINK) {
                    MapLink link = (MapLink)orig;
                    if (intersectsLine(viewArea, link)) {
                        drawLink(link, gc, linkLabelsShowing, true, false);
                    }
                }
            }
        }
        if (redoRepainting) {
            return;
        }
        /* then nodes links */
        if (nodesShowing) {
            for (OBNodeSymbolicLink symlink : symbolicLinks) {
                OBNode orig = symlink.getOriginal();
                if (orig.getNodeType() == OBNode.NType.NODE) {
                    MapNode node = (MapNode)orig;
                    if (viewArea.contains(getRotatedX(node), getRotatedY(node))) {
                        drawNode(node, gc, nodeLabelsShowing, true, false);
                    }
                }
            }
        }
        if (redoRepainting) {
            return;
        }

        if (oneWayIndicatorShowing) {
            drawOneWayIndicator(oneWayfirstNode, "A", oneWayLabelPositionA, gc);
            drawOneWayIndicator(oneWaylastNode, "B", oneWayLabelPositionB, gc);
        }

        // 選択フレームの表示
        if (selectionRange != null) {
            drawSelectionRange(gc, selectionRange);
        }
        if (! selectionRangePolygon.isEmpty()) {
            Point2D point = pointConvertCanvasToMap(lastMouseX, lastMouseY);
            drawSelectionRangePolygon(gc, selectionRangePolygon, point);
        }

        // 移動中の複数ノードの仮描画
        if (nodeMoving) {
            for (MapNode node : editor.getSelectedNodes()) {
                drawTemporaryNode(node, gc, nodeMovingAmount.getX(), nodeMovingAmount.getY());
            }
        }

        // ホバーの描画
        switch (mode) {
        case ADD_NODE:
            gc.setTransform(originalAffine);
            drawNodeHoverOnMouseCursor(lastMouseX, lastMouseY, gc);
            break;
        case ADD_LINK:
            if (fromNode != null && toNode != null) {
                drawTentativeLink(fromNode, toNode, gc);
            }
            if (pointedNode != null) {
                drawNodeHover(pointedNode, gc);
            }
            break;
        case ADD_NODE_LINK:
            if (fromNode != null) {
                drawTentativeLink(fromNode, pointConvertCanvasToMap(lastMouseX, lastMouseY), gc);
            }
            if (pointedNode != null) {
                drawNodeHover(pointedNode, gc);
            }
            gc.setTransform(originalAffine);
            drawNodeHoverOnMouseCursor(lastMouseX, lastMouseY, gc);
            break;
        case ADD_AREA:
            break;
        case EDIT_NODE:
            if (pointedNode != null) {
                drawNodeHover(pointedNode, gc);
            }
            break;
        case EDIT_LINK:
            if (! oneWayIndicatorShowing && pointedLink != null) {
                drawLinkHover(pointedLink, gc);
            }
            break;
        case EDIT_AREA:
            if (pointedArea != null) {
                drawAreaHover(pointedArea, gc);
            }
            break;
        }
    }

    /**
     * 地理院タイルを描画する
     */
    private void drawBackgroundMapTile(GsiTile gsiTile, Image image, GraphicsContext gc) {
        Point2D point = calcRotatedPoint(gsiTile.getPoint());
        double x = point.getX() * SCALE_FACTOR;
        double y = point.getY() * SCALE_FACTOR;
        double width = image.getWidth() * gsiTile.getScaleX() * SCALE_FACTOR;
        double height = image.getHeight() * gsiTile.getScaleY() * SCALE_FACTOR;

        Affine originalTransform = gc.getTransform();
        Affine affine = new Affine();
        affine.appendRotation(angle, x, y);
        gc.transform(affine);
        gc.drawImage(image, x, y, width, height);
        gc.setTransform(originalTransform);
    }

    /**
     * 背景画像を描画する
     */
    private void drawBackgroundImage(MapPartGroup group, GraphicsContext gc) {
        Image image = editor.getBackgroundImage(group);
        if (image == null) {
            return;
        }
        Affine originalTransform = gc.getTransform();

        Affine affine = new Affine();
        double angle = group.r * 180.0 / Math.PI + this.angle;
        Point2D point = calcRotatedPoint(group.tx, group.ty);
        affine.appendRotation(angle, point.getX() * SCALE_FACTOR, point.getY() * SCALE_FACTOR);
        gc.transform(affine);
        gc.drawImage(image, point.getX() * SCALE_FACTOR, point.getY() * SCALE_FACTOR, image.getWidth() * group.sx * SCALE_FACTOR, image.getHeight() * group.sy * SCALE_FACTOR);

        gc.setTransform(originalTransform);
    }

    /**
     * 背景グループを描画する
     */
    private void drawBackgroundGroup(MapPartGroup group, GraphicsContext gc, Rectangle2D viewArea) {
        if (areasShowing) {
            for (MapArea area : group.getChildMapAreas()) {
                if (frame.getAreaPanel().getFilteredSet().contains(area) && contains(viewArea, area, false)) {
                    drawArea(area, gc, areaLabelsShowing, true);
                    if (redoRepainting) {
                        return;
                    }
                }
            }
        }
        if (linksShowing) {
            for (MapLink link : group.getChildLinks()) {
                if (intersectsLine(viewArea, link) && frame.getLinkPanel().getFilteredSet().contains(link)) {
                    drawLink(link, gc, linkLabelsShowing, false, true);
                    if (redoRepainting) {
                        return;
                    }
                }
            }
        }
        if (nodesShowing) {
            for (MapNode node : group.getChildNodes()) {
                if (viewArea.contains(getRotatedX(node), getRotatedY(node)) && frame.getNodePanel().getFilteredSet().contains(node)) {
                    drawNode(node, gc, nodeLabelsShowing, false, true);
                    if (redoRepainting) {
                        return;
                    }
                }
            }
        }

        ArrayList<OBNodeSymbolicLink> symbolicLinks = group.getSymbolicLinks();
        if (linksShowing) {
            for (OBNodeSymbolicLink symlink : symbolicLinks) {
                OBNode orig = symlink.getOriginal();
                if (orig == null) {
                    continue;
                }
                if (orig.getNodeType() == OBNode.NType.LINK) {
                    MapLink link = (MapLink)orig;
                    if (intersectsLine(viewArea, link)) {
                        drawLink(link, gc, linkLabelsShowing, true, true);
                    }
                }
            }
        }
        if (redoRepainting) {
            return;
        }
        if (nodesShowing) {
            for (OBNodeSymbolicLink symlink : symbolicLinks) {
                OBNode orig = symlink.getOriginal();
                if (orig.getNodeType() == OBNode.NType.NODE) {
                    MapNode node = (MapNode)orig;
                    if (viewArea.contains(getRotatedX(node), getRotatedY(node))) {
                        drawNode(node, gc, nodeLabelsShowing, true, true);
                    }
                }
            }
        }
    }

    /**
     * 矩形選択フレームを描画する
     */
    private void drawSelectionRange(GraphicsContext gc, javafx.geometry.Rectangle2D bounds) {
        double x = bounds.getMinX() * SCALE_FACTOR;
        double y = bounds.getMinY() * SCALE_FACTOR;
        double width = bounds.getWidth() * SCALE_FACTOR;
        double height = bounds.getHeight() * SCALE_FACTOR;

        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1.0 / scale);
        gc.strokeRect(x, y, width, height);
    }

    /**
     * ポリゴン選択フレームを描画する
     */
    private void drawSelectionRangePolygon(GraphicsContext gc, ArrayList<Point2D> polygon, Point2D cursorPoint) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1.0 / scale);
        gc.beginPath();
        Point2D point = polygon.get(0);
        gc.moveTo(point.getX() * SCALE_FACTOR, point.getY() * SCALE_FACTOR);
        for (int index = 1; index < polygon.size(); index++) {
            point = polygon.get(index);
            gc.lineTo(point.getX() * SCALE_FACTOR, point.getY() * SCALE_FACTOR);
        }
        gc.lineTo(cursorPoint.getX() * SCALE_FACTOR, cursorPoint.getY() * SCALE_FACTOR);
        gc.stroke();
    }

    /**
     * ノードを描画する
     */
    private void drawNode(MapNode node, GraphicsContext gc, boolean labelShowing, boolean isSymbolic, boolean isBg) {
        double radius = 4.0 / scale;
        double cx = getRotatedX(node) * SCALE_FACTOR;
        double cy = getRotatedY(node) * SCALE_FACTOR;

        Color baseColor = isBg ? Color.web("#66ccff") : Color.MEDIUMBLUE;
        double brightness = baseColor.getBrightness();
        double hue = baseColor.getHue();
        double saturation = baseColor.getSaturation();
        if (node.getHeight() >= 0.0) {
            saturation -= (node.getHeight() / editor.getMaxHeight()) * 0.7;
            brightness += (1.0 - brightness) * (node.getHeight() / editor.getMaxHeight());
        } else {
            saturation -= (node.getHeight() / editor.getMinHeight());
            brightness -= brightness * (node.getHeight() / editor.getMinHeight());
        }
        gc.setFill(Color.hsb(hue, Math.max(Math.min(saturation, 1.0), 0.0), Math.max(Math.min(brightness, 1.0), 0.0)));
        gc.fillRect(cx - radius, cy - radius, radius * 2, radius * 2);

        if (isSymbolic) {
            gc.setLineWidth(2.0 / scale);
            gc.setStroke(isBg ? Color.web("#99ff99") : Color.LIME);
            gc.strokeRect(cx - radius, cy - radius, radius * 2, radius * 2);
        } else if (node.selected) {
            double _radius = 5.0 / scale;
            gc.setFill(Color.RED);
            gc.fillRect(cx - _radius, cy - _radius, _radius * 2, _radius * 2);
        }

        if (labelShowing) {
            String text = node.getHintString();
            if (! text.isEmpty()) {
                gc.setFill(isBg ? Color.SILVER : Color.BLACK);
                gc.fillText(text, cx, cy - radius);
            }
        }
    }

    /**
     * ノードを仮描画する
     */
    private void drawTemporaryNode(MapNode node, GraphicsContext gc, double dx, double dy) {
        double radius = 4.0 / scale;
        double cx = (getRotatedX(node) + dx) * SCALE_FACTOR;
        double cy = (getRotatedY(node) + dy) * SCALE_FACTOR;

        if (node.selected) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(2.0 / scale);
            gc.strokeRect(cx - radius, cy - radius, radius * 2, radius * 2);
        } else {
            gc.setFill(Color.MEDIUMBLUE);
            gc.fillRect(cx - radius, cy - radius, radius * 2, radius * 2);
        }
    }

    /**
     * ノードのホバーを描画する
     */
    private void drawNodeHover(MapNode node, GraphicsContext gc) {
        double radius = 5.0 / scale;
        double cx = getRotatedX(node) * SCALE_FACTOR;
        double cy = getRotatedY(node) * SCALE_FACTOR;

        gc.setFill(Color.AQUA);
        gc.fillRect(cx - radius, cy - radius, radius * 2, radius * 2);
        gc.setStroke(Color.RED);
        gc.setLineWidth(0.8 / scale);
        gc.strokeRect(cx - radius, cy - radius, radius * 2, radius * 2);

        // タグ表示
        String text = node.getHintString();
        if (! text.isEmpty()) {
            gc.setFill(Color.BLUE);
            gc.fillText(text, cx, cy - radius);
        }
    }

    /**
     * マウスカーソル上にノードのホバーを描画する
     */
    private void drawNodeHoverOnMouseCursor(double x, double y, GraphicsContext gc) {
        double radius = 5.0;

        gc.setFill(Color.AQUA);
        gc.fillRect(x - radius, y - radius, radius * 2, radius * 2);
        gc.setStroke(Color.RED);
        gc.setLineWidth(0.8);
        gc.strokeRect(x - radius, y - radius, radius * 2, radius * 2);
    }

    /**
     * リンクを描画する
     */
    private void drawLink(MapLink link, GraphicsContext gc, boolean labelShowing, boolean isSymbolic, boolean isBg) {
        PolygonPointsBuilder polygon = null;
        if (link.isOneWayForward()) {
            polygon = getLinkArrow(link, scale, true);
        } else if (link.isOneWayBackward()) {
            polygon = getLinkArrow(link, scale, false);
        } else {
            polygon = getLinkRect(link, scale);
        }

        Color frameColor = isBg ? Color.SILVER : Color.BLACK;
        gc.setLineWidth(1.0 / scale);
        if (isSymbolic) {
            gc.setFill(isBg ? Color.GAINSBORO : Color.GRAY);
            gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
            gc.setStroke(isBg ? Color.web("#ffffcc") : Color.YELLOW);
            gc.strokePolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
        } else if (link.selected) {
            gc.setFill(Color.RED);
            gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
            gc.setStroke(Color.RED);
            gc.strokePolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
        } else if (link.getWidth() == 0.0) {
            gc.setFill(isBg ? Color.web("#ffffcc") : Color.YELLOW);
            gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
        } else {
            if (link.isOneWayForward()) {
                gc.setFill(isBg ? Color.web("#ff99ff") : Color.MAGENTA);
                gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
            } else if (link.isOneWayBackward()) {
                gc.setFill(FxColor.ALIGHTB);
                gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
            } else if (link.isRoadClosed()) {
                gc.setFill(FxColor.LINK_RED);
                gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
            } else if (link.getFrom().getParent() != link.getTo().getParent()) {
                // 階段
                gc.setFill(isBg ? Color.web("#99ffff") : Color.CYAN);
                gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
            } else {
                Color color = isBg ? Color.web("#ffffcc") : Color.YELLOW;
                // length と計算値の差が 10cm を超えていたら警告する
                if (Math.abs(editor.calculateLinkLength(link) - link.getLength()) > 0.1) {
                    color = isBg ? Color.web("#ffcc66") : Color.ORANGE;
                    frameColor = isBg ? Color.web("#ffcc66").brighter() : Color.ORANGE.brighter();
                }
                gc.setFill(color);
                gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
            }
            gc.setStroke(frameColor);
            gc.strokePolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
        }

        if (labelShowing) {
            String text = link.getTagString();
            if (! text.isEmpty()) {
                Point2D middlePoint = calcRotatedPoint(link.getMiddlePoint());
                double cx = middlePoint.getX();
                double cy = middlePoint.getY();
                gc.setFill(isBg ? Color.SILVER : Color.BLACK);
                gc.fillText(text, cx * SCALE_FACTOR, cy * SCALE_FACTOR);
            }
        }
    }

    /**
     * リンクのホバーを描画する
     */
    private void drawLinkHover(MapLink link, GraphicsContext gc) {
        PolygonPointsBuilder polygon = getLinkRect(link, scale);

        gc.setFill(Color.BLUE);
        gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());

        String text = link.getTagString();
        if (! text.isEmpty()) {
            Point2D middlePoint = calcRotatedPoint(link.getMiddlePoint());
            double cx = middlePoint.getX();
            double cy = middlePoint.getY();
            gc.setFill(Color.BLUE);
            gc.fillText(text, cx * SCALE_FACTOR, cy * SCALE_FACTOR);
        }
    }

    /**
     * 仮リンクを描画する
     */
    private void drawTentativeLink(MapNode fromNode, MapNode toNode, GraphicsContext gc) {
        PolygonPointsBuilder polygon = getLinkRect(getRotatedX(fromNode), getRotatedY(fromNode), getRotatedX(toNode), getRotatedY(toNode), scale);

        gc.setFill(Color.BLUE);
        gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
    }

    /**
     * 仮リンクを描画する
     */
    private void drawTentativeLink(MapNode fromNode, Point2D toPoint, GraphicsContext gc) {
        PolygonPointsBuilder polygon = getLinkRect(getRotatedX(fromNode), getRotatedY(fromNode), toPoint.getX(), toPoint.getY(), scale);

        gc.setFill(Color.BLUE);
        gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());
    }

    /**
     * create rectangle polygon.
     */
    private PolygonPointsBuilder getLinkRect(double x1, double y1, double x2, double y2, double scale) {
        x1 *= SCALE_FACTOR;
        y1 *= SCALE_FACTOR;
        x2 *= SCALE_FACTOR;
        y2 *= SCALE_FACTOR;

        double fwidth = 4.0 / scale;
        double dx = (x2 - x1);
        double dy = (y2 - y1);
        double a = Math.sqrt(dx * dx + dy * dy);

        double edx = fwidth * dx / a / 2;
        double edy = fwidth * dy / a / 2;

        PolygonPointsBuilder builder = new PolygonPointsBuilder();
        builder.append(x1 - edy, y1 + edx);
        builder.append(x1 + edy, y1 - edx);
        builder.append(x2 + edy, y2 - edx);
        builder.append(x2 - edy, y2 + edx);
        return builder;
    }

    /**
     * create rectangle polygon.
     */
    private PolygonPointsBuilder getLinkRect(MapLink link, double scale) {
        double x1 = getRotatedX(link.getFrom());
        double y1 = getRotatedY(link.getFrom());
        double x2 = getRotatedX(link.getTo());
        double y2 = getRotatedY(link.getTo());
        return getLinkRect(x1, y1, x2, y2, scale);
    }

    /**
     * create arrow polygon consist of seven coordinates.
     * forward means the direction of arrow: true(from-to), false(to-from)
     */
    private PolygonPointsBuilder getLinkArrow(MapLink link, double scale, boolean forward) {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 0.0;
        double y2 = 0.0;
        if (forward) {
            x1 = getRotatedX(link.getFrom());
            y1 = getRotatedY(link.getFrom());
            x2 = getRotatedX(link.getTo());
            y2 = getRotatedY(link.getTo());
        } else {
            x1 = getRotatedX(link.getTo());
            y1 = getRotatedY(link.getTo());
            x2 = getRotatedX(link.getFrom());
            y2 = getRotatedY(link.getFrom());
        }
        x1 *= SCALE_FACTOR;
        y1 *= SCALE_FACTOR;
        x2 *= SCALE_FACTOR;
        y2 *= SCALE_FACTOR;
        double fwidth = 4.0 / scale;

        double dx = (x2 - x1);
        double dy = (y2 - y1);
        double a = Math.sqrt(dx * dx + dy * dy);

        double edx = fwidth * dx / a / 2;
        double edy = fwidth * dy / a / 2;

        double arrowHeight = 8.0 / scale;
        double arrowWidth = 8.0 / scale;
        double bx = x2 - arrowHeight * dx / a;
        double by = y2 - arrowHeight * dy / a;
        double ax = arrowWidth * dy / a / 2;
        double ay = arrowWidth * dx / a / 2;

        PolygonPointsBuilder builder = new PolygonPointsBuilder();
        builder.append(x1 - edy, y1 + edx);
        builder.append(x1 + edy, y1 - edx);
        builder.append(bx + edy, by - edx);
        builder.append(bx + edy + ax, by - edx - ay);
        builder.append(x2, y2);
        builder.append(bx - edy - ax, by + edx + ay);
        builder.append(bx - edy, by + edx);
        return builder;
    }

    /**
     * エリアを描画する
     */
    private void drawArea(MapArea area, GraphicsContext gc, boolean labelShowing, boolean isBg) {
        PolygonPointsBuilder polygon = new PolygonPointsBuilder();
        for (Point2D point : getPointsOfRectangle(area)) {
            polygon.append(point.getX() * SCALE_FACTOR, point.getY() * SCALE_FACTOR);
        }

        gc.setFill(area.selected ? Color.RED : gray8050);
        gc.fillPolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());

        if (labelShowing) {
            String text = area.getTagString();
            if (! text.isEmpty()) {
                Rectangle2D bounds = (Rectangle2D)area.getShape();
                Point2D point = calcRotatedPoint(bounds.getCenterX(), bounds.getCenterY());
                double x = point.getX() * SCALE_FACTOR;
                double y = point.getY() * SCALE_FACTOR;
                double textWidth = 10 / scale;      // フォントメトリクスが使えないため目分量で決めている
                double textHeight = 16 / scale;     //                  同上
                gc.setFill(isBg ? Color.SILVER : Color.BLACK);
                gc.fillText(text, x - textWidth * 2, y + textHeight / 2);
            }
        }
    }

    /**
     * エリアのホバーを描画する
     */
    private void drawAreaHover(MapArea area, GraphicsContext gc) {
        PolygonPointsBuilder polygon = new PolygonPointsBuilder();
        for (Point2D point : getPointsOfRectangle(area)) {
            polygon.append(point.getX() * SCALE_FACTOR, point.getY() * SCALE_FACTOR);
        }

        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2.0 / scale);
        gc.strokePolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.size());

        // タグ表示
        String text = area.getTagString();
        if (! text.isEmpty()) {
            Rectangle2D bounds = (Rectangle2D)area.getShape();
            Point2D point = calcRotatedPoint(bounds.getCenterX(), bounds.getCenterY());
            double x = point.getX() * SCALE_FACTOR;
            double y = point.getY() * SCALE_FACTOR;
            double textWidth = 10 / scale;      // フォントメトリクスが使えないため目分量で決めている
            double textHeight = 16 / scale;     //                  同上
            gc.setFill(Color.BLUE);
            gc.fillText(text, x - textWidth * 2, y + textHeight / 2);
        }
    }

    /**
     * 一方通行設定時の表示設定
     */
    public void setOneWayIndicator(boolean showing, MapNode firstNode, TextPosition positionA, MapNode lastNode, TextPosition positionB) {
        oneWayIndicatorShowing = showing;
        oneWayfirstNode = firstNode;
        oneWaylastNode = lastNode;
        oneWayLabelPositionA = positionA;
        oneWayLabelPositionB = positionB;
    }

    /**
     * 一方通行設定用のテキストを描画する
     */
    private void drawOneWayIndicator(MapNode node, String text, TextPosition position, GraphicsContext gc) {
        double cx = getRotatedX(node) * SCALE_FACTOR;
        double cy = getRotatedY(node) * SCALE_FACTOR;
        double textWidth = 10 / scale;      // フォントメトリクスが使えないため目分量で決めている
        double textHeight = 20 / scale;     //                  同上

        double dx = 0.0;
        double dy = 0.0;
        switch (position) {
        case CENTER:
            dx = textWidth / -2.0;
            dy = textHeight / 5.0;
            break;
        case UPPER:
            dx = textWidth / -2.0;
            dy = textHeight / -3.0;
            break;
        case LOWER:
            dx = textWidth / -2.0;
            dy = textHeight / 1.3;
            break;
        case LEFT:
            dx = -textWidth - textHeight / 3.0;
            dy = textHeight / 5.0;
            break;
        case RIGHT:
            dx = textHeight / 3.0;
            dy = textHeight / 5.0;
            break;
        }

        Font currentFont = gc.getFont();
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18 / scale));
        gc.setFill(Color.RED);
        gc.fillText(text, cx + dx, cy + dy);
        gc.setFont(currentFont);
    }

    /**
     * 編集状態をクリアする
     */
    public void clearEditingStates() {
        pointedNode = null;
        pointedLink = null;
        pointedArea = null;
        fromNode = null;
        toNode = null;
        selectionRangeStart = null;
        selectionRange = null;
        selectionRangePolygon.clear();
    }

    /**
     * 編集モードを切り替える
     */
    public void setMode(EditorMode mode) {
        this.mode = mode;
        frame.getNodePanel().clearSelection();
        frame.getLinkPanel().clearSelection();
        frame.getAreaPanel().clearSelection();
        clearEditingStates();
    }

    public Stage getStage() {
        return (Stage)getScene().getWindow();
    }

    public void setNodesShowing(boolean nodesShowing) {
        this.nodesShowing = nodesShowing;
    }

    public void setNodeLabelsShowing(boolean nodeLabelsShowing) {
        this.nodeLabelsShowing = nodeLabelsShowing;
    }

    public void setLinksShowing(boolean linksShowing) {
        this.linksShowing = linksShowing;
    }

    public void setLinkLabelsShowing(boolean linkLabelsShowing) {
        this.linkLabelsShowing = linkLabelsShowing;
    }

    public void setAreasShowing(boolean areasShowing) {
        this.areasShowing = areasShowing;
    }

    public void setAreaLabelsShowing(boolean areaLabelsShowing) {
        this.areaLabelsShowing = areaLabelsShowing;
    }

    public void setBackgroundMapShowing(boolean backgroundMapShowing) {
        this.backgroundMapShowing = backgroundMapShowing;
    }

    public void setBackgroundImageShowing(boolean backgroundImageShowing) {
        this.backgroundImageShowing = backgroundImageShowing;
    }

    public void setMapCoordinatesShowing(boolean mapCoordinatesShowing) {
        this.mapCoordinatesShowing = mapCoordinatesShowing;
    }

    public MapNode getPointedNode() {
        return pointedNode;
    }

    public void setBackgroundGroup(MapPartGroup backgroundGroup) {
        this.backgroundGroup = backgroundGroup;
    }
}
