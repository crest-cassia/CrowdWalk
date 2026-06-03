package nodagumi.ananPJ.Editor.Panel;

import java.util.ArrayList;
import java.util.HashSet;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.Editor.EditorFrameFx;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;

/**
 * マップエディタの Polygon タブ用のペイン
 */
public class PolygonPanel extends BorderPane {
    /**
     * データ・モデル
     */
    public static class PolygonModel {
        private MapPolygon polygon;

        public PolygonModel(MapPolygon polygon) {
            this.polygon = polygon;
        }

        public MapPolygon getPolygon() { 
            return polygon;
        }

        public String getId() {
            return polygon.getID();
        }

        public String getTags() {
            return String.join(", ", polygon.getTags());
        }

        public Integer getZindex() {
            return polygon.getZIndex();
        }

        public String getType() {
            if (polygon.isTriangleMeshes()) {
                return "Triangle Mesh";
            }
            if (polygon.isPlanePolygon()) {
                return String.format("Plane Polygon[height=%s, %d innerBoundaries]", polygon.getOuterBoundary().getHeight(), polygon.getInnerBoundaries().size());
            }
            return "None";
        }

        public String getGroup() {
            return polygon.getParent().toString();
        }
    }

    /**
     * マップエディタ
     */
    private MapEditor editor;

    /**
     * マップエディタのウィンドウフレーム
     */
    private EditorFrameFx frame;

    private TableView<PolygonModel> tableView;
    private ObservableList<PolygonModel> polygonList = FXCollections.observableArrayList();
    private FilteredList<PolygonModel> filteredData;
    private TableView.TableViewSelectionModel<PolygonModel> selectionModel;
    private String tag = "";
    private TextField tagField = new TextField();
    private Button selectButton = new Button("Select");
    private ToggleButton filterButton = new ToggleButton("Filter");
    private CheckBox regexpCheckBox = new CheckBox("regexp");
    private boolean eventDisabled = false;
    private HashSet<MapPolygon> filteredSet = new HashSet();

    /**
     * コンストラクタ
     */
    public PolygonPanel(MapEditor editor, EditorFrameFx frame) {
        this.editor = editor;
        this.frame = frame;

        tableView = createTableView();
        this.setCenter(tableView);

        // タグ検索ペイン

        tagField.setMinWidth(250);

        selectButton.setOnAction(e -> {
            selectionModel.clearSelection();
            editor.deselectPolygons();

            tag = tagField.getText().trim();
            if (! tag.isEmpty()) {
                ArrayList<Integer> indices = new ArrayList();
                int lastIndex = -1;
                for (int index = 0; index < polygonList.size(); index++) {
                    PolygonModel polygon = polygonList.get(index);
                    if (matching(polygon.getPolygon(), tag)) {
                        indices.add(index);
                        lastIndex = index;
                    }
                }
                if (! indices.isEmpty()) {
                    setEventDisabled(true);     // 無駄なイベント処理をスキップさせる
                    for (Integer index : indices) {
                        if (index == lastIndex) {
                            setEventDisabled(false);
                        }
                        selectionModel.select(index.intValue());
                    }
                }
            }
        });

        filterButton.setOnAction(e -> {
            selectionModel.clearSelection();
            editor.deselectPolygons();
            frame.getCanvas().repaintLater();

            if (filterButton.isSelected()) {
                selectButton.setDisable(true);
                tag = tagField.getText().trim();
            } else {
                selectButton.setDisable(false);
                tag = "";
            }
            filteredSet.clear();
            filteredData.setPredicate(polygon -> {
                if (! filterButton.isSelected() || tag.isEmpty() || matching(polygon.getPolygon(), tag)) {
                    filteredSet.add((MapPolygon)editor.getMap().getObject(polygon.getId()));
                    return true;
                }
                return false;
            });
        });

        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(4));
        flowPane.setHgap(8);
        flowPane.setAlignment(Pos.CENTER);
        flowPane.getChildren().addAll(new Label("Tag"), tagField, selectButton, filterButton, regexpCheckBox);
        flowPane.setMargin(regexpCheckBox, new Insets(0, 0, 0, 8));
        this.setBottom(flowPane);
    }

    /**
     * テーブルビューの生成
     */
    private TableView<PolygonModel> createTableView() {
        TableView<PolygonModel> tableView = new TableView<>();
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<PolygonModel, String> idCol = new TableColumn("ID");
        TableColumn<PolygonModel, String> tagsCol = new TableColumn("Tags");
        TableColumn<PolygonModel, Integer> zindexCol = new TableColumn("Z-index");
        TableColumn<PolygonModel, String> typeoCol = new TableColumn("Type");
        TableColumn<PolygonModel, String> groupCol = new TableColumn("Group");

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        zindexCol.setCellValueFactory(new PropertyValueFactory<>("zindex"));
        typeoCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        groupCol.setCellValueFactory(new PropertyValueFactory<>("group"));

        reset();
        filteredData = new FilteredList<>(polygonList, p -> true);
        tableView.setItems(filteredData);
        tableView.getColumns().addAll(idCol, tagsCol, zindexCol, typeoCol, groupCol);

        selectionModel.getSelectedItems().addListener(new ListChangeListener<PolygonModel>() {
            public void onChanged(ListChangeListener.Change<? extends PolygonModel> changed) {
                if (isEventDisabled()) {
                    return;
                }
                editor.deselectPolygons();
                // 選択状態の要素すべて
                for (PolygonModel model : changed.getList()) {
                    int index = getIndex(model);
                    MapPolygon polygon = (MapPolygon)editor.getMap().getObject(model.getId());
                    polygon.selected = selectionModel.isSelected(index);
                }
                frame.getCanvas().repaintLater();
            }
        });

        tableView.setContextMenu(frame.getEditPolygonMenu());

        return tableView;
    }

    /**
     * テーブルの要素を再設定する
     */
    public void reset() {
        ArrayList<PolygonModel> polygonModels = new ArrayList();
        filteredSet.clear();
        for (MapPolygon polygon : editor.getMap().getPolygons()) {
            polygonModels.add(new PolygonModel(polygon));
            if (! filterButton.isSelected() || tag.isEmpty() || matching(polygon, tag)) {
                filteredSet.add(polygon);
            }
        }
        polygonList.setAll(polygonModels);
    }

    /**
     * タグの照合
     */
    private boolean matching(MapPolygon polygon, String tag) {
        if (regexpCheckBox.isSelected()) {
            return polygon.matchTag(tag) != null;
        }
        return polygon.hasSubTag(tag);
    }

    /**
     * 選択をすべてクリアしてから指定された要素を選択状態にする
     */
    public boolean clearAndSelect(MapPolygon polygon) {
        int index = getIndex(polygon);
        if (index != -1) {
            selectionModel.clearAndSelect(index);
            return true;
        }
        return false;
    }

    /**
     * すべての指定された要素の選択状態を解除する
     */
    public void clearSelection() {
        selectionModel.clearSelection();
    }

    /**
     * 指定された要素の選択状態を解除する
     */
    public void clearSelection(MapPolygon polygon) {
        int index = getIndex(polygon);
        if (index != -1) {
            selectionModel.clearSelection(index);
        }
    }

    /**
     * 指定された要素を選択状態にする
     */
    public boolean select(MapPolygon polygon) {
        int index = getIndex(polygon);
        if (index != -1) {
            selectionModel.select(index);
            return true;
        }
        return false;
    }

    /**
     * 指定された要素を選択状態にする
     */
    public void select(ArrayList<MapPolygon> polygons) {
        if (polygons.isEmpty()) {
            return;
        }
        MapPolygon lastObject = polygons.get(polygons.size() - 1);
        setEventDisabled(true);     // 無駄なイベント処理をスキップさせる
        for (MapPolygon polygon : polygons) {
            if (polygon == lastObject) {
                setEventDisabled(false);
            }
            selectionModel.select(getIndex(polygon));
        }
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(MapPolygon polygon) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index).getId().equals(polygon.getID())) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(PolygonModel polygon) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index) == polygon) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 表示を更新する
     */
    public void refresh() {
        tableView.refresh();
    }

    /**
     * フィルタリングされたリンクのセットを取得する
     */
    public HashSet<MapPolygon> getFilteredSet() {
        return filteredSet;
    }

    private synchronized void setEventDisabled(boolean eventDisabled) {
        this.eventDisabled = eventDisabled;
    }

    private synchronized boolean isEventDisabled() {
        return eventDisabled;
    }
}
