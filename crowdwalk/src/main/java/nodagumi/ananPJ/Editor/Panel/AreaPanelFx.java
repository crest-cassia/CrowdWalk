package nodagumi.ananPJ.Editor.Panel;

import java.io.File;
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
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.Editor.EditorFrameFx;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;

/**
 * マップエディタの Areas タブ用のペイン
 */
public class AreaPanelFx extends BorderPane {
    /**
     * データ・モデル
     */
    public static class AreaModel {
        private MapArea area;

        public AreaModel(MapArea area) {
            this.area = area;
        }

        public MapArea getArea() { 
            return area;
        }

        public String getId() {
            return area.getID();
        }

        public String getTags() {
            return String.join(", ", area.getTags());
        }

        public Double getWest() {
            return area.getShape().getBounds2D().getMinX();
        }

        public Double getNorth() {
            return area.getShape().getBounds2D().getMinY();
        }

        public Double getEast() {
            return area.getShape().getBounds2D().getMaxX();
        }

        public Double getSouth() {
            return area.getShape().getBounds2D().getMaxY();
        }

        public String getGroup() {
            return area.getParent().toString();
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

    private TableView<AreaModel> tableView;
    private ObservableList<AreaModel> areaList = FXCollections.observableArrayList();
    private FilteredList<AreaModel> filteredData;
    private TableView.TableViewSelectionModel<AreaModel> selectionModel;
    private String tag = "";
    private TextField tagField = new TextField();
    private Button selectButton = new Button("Select");
    private ToggleButton filterButton = new ToggleButton("Filter");
    private CheckBox regexpCheckBox = new CheckBox("regexp");
    private boolean eventDisabled = false;
    private HashSet<MapArea> filteredSet = new HashSet();

    private Label obstructerFileLabel;
    private Button openButton = new Button("Open");

    /**
     * コンストラクタ
     */
    public AreaPanelFx(MapEditor editor, EditorFrameFx frame) {
        this.editor = editor;
        this.frame = frame;

        // Obstructer ファイルペイン

        String fileName = editor.getPollutionFile();
        obstructerFileLabel = new Label("Obstructer file: " + (fileName == null ? "None" : fileName));
        obstructerFileLabel.setPadding(new Insets(4, 0, 0, 0));
        obstructerFileLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        openButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Obstructer File");
            frame.setInitialPath(fileChooser, editor.getPollutionFile());
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("All", "*.*")
            );
            File file = fileChooser.showOpenDialog(frame.getStage());
            if (file != null) {
                editor.setPollutionFile(editor.getRelativePath(file));
                obstructerFileLabel.setText("Obstructer file: " + editor.getRelativePath(file));
            }
        });
        BorderPane obstructerFilePane = new BorderPane();
        obstructerFilePane.setPadding(new Insets(4, 20, 4, 8));
        obstructerFilePane.setLeft(obstructerFileLabel);
        obstructerFilePane.setRight(openButton);
        this.setTop(obstructerFilePane);

        // テーブル

        tableView = createTableView();
        this.setCenter(tableView);

        // タグ検索ペイン

        tagField.setMinWidth(250);

        selectButton.setOnAction(e -> {
            selectionModel.clearSelection();
            editor.deselectAreas();

            tag = tagField.getText().trim();
            if (! tag.isEmpty()) {
                ArrayList<Integer> indices = new ArrayList();
                int lastIndex = -1;
                for (int index = 0; index < areaList.size(); index++) {
                    AreaModel area = areaList.get(index);
                    if (matching(area.getArea(), tag)) {
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
            editor.deselectAreas();
            frame.getCanvas().repaintLater();

            if (filterButton.isSelected()) {
                selectButton.setDisable(true);
                tag = tagField.getText().trim();
            } else {
                selectButton.setDisable(false);
                tag = "";
            }
            filteredSet.clear();
            filteredData.setPredicate(area -> {
                if (! filterButton.isSelected() || tag.isEmpty() || matching(area.getArea(), tag)) {
                    filteredSet.add((MapArea)editor.getMap().getObject(area.getId()));
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
    private TableView<AreaModel> createTableView() {
        TableView<AreaModel> tableView = new TableView<>();
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<AreaModel, String> idCol = new TableColumn("ID");
        TableColumn<AreaModel, String> tagsCol = new TableColumn("Tags");
        TableColumn<AreaModel, Double> westCol = new TableColumn("West");
        TableColumn<AreaModel, Double> northCol = new TableColumn("North");
        TableColumn<AreaModel, Double> eastCol = new TableColumn("East");
        TableColumn<AreaModel, Double> southCol = new TableColumn("South");
        TableColumn<AreaModel, String> groupCol = new TableColumn("Group");

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        westCol.setCellValueFactory(new PropertyValueFactory<>("west"));
        northCol.setCellValueFactory(new PropertyValueFactory<>("north"));
        eastCol.setCellValueFactory(new PropertyValueFactory<>("east"));
        southCol.setCellValueFactory(new PropertyValueFactory<>("south"));
        groupCol.setCellValueFactory(new PropertyValueFactory<>("group"));

        reset();
        filteredData = new FilteredList<>(areaList, p -> true);
        tableView.setItems(filteredData);
        tableView.getColumns().addAll(idCol, tagsCol, westCol, northCol, eastCol, southCol, groupCol);

        selectionModel.getSelectedItems().addListener(new ListChangeListener<AreaModel>() {
            public void onChanged(ListChangeListener.Change<? extends AreaModel> changed) {
                if (isEventDisabled()) {
                    return;
                }
                editor.deselectAreas();
                // 選択状態の要素すべて
                for (AreaModel model : changed.getList()) {
                    int index = getIndex(model);
                    MapArea area = (MapArea)editor.getMap().getObject(model.getId());
                    area.selected = selectionModel.isSelected(index);
                }
                frame.getCanvas().repaintLater();
            }
        });

        tableView.setContextMenu(frame.getEditAreaMenu());

        return tableView;
    }

    /**
     * テーブルの要素を再設定する
     */
    public void reset() {
        String fileName = editor.getPollutionFile();
        obstructerFileLabel.setText("Obstructer file: " + (fileName == null ? "None" : fileName));

        ArrayList<AreaModel> areaModels = new ArrayList();
        filteredSet.clear();
        for (MapArea area : editor.getMap().getAreas()) {
            areaModels.add(new AreaModel(area));
            if (! filterButton.isSelected() || tag.isEmpty() || matching(area, tag)) {
                filteredSet.add(area);
            }
        }
        areaList.setAll(areaModels);
    }

    /**
     * スプリットペインのディバイダが移動した時に呼び出されるコールバック
     */
    public void widthChanged(double width) {
        obstructerFileLabel.setPrefWidth(width - 28 - openButton.getWidth());
    }

    /**
     * タグの照合
     */
    private boolean matching(MapArea area, String tag) {
        if (regexpCheckBox.isSelected()) {
            return area.matchTag(tag) != null;
        }
        return area.hasSubTag(tag);
    }

    /**
     * 選択をすべてクリアしてから指定された要素を選択状態にする
     */
    public boolean clearAndSelect(MapArea area) {
        int index = getIndex(area);
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
    public void clearSelection(MapArea area) {
        int index = getIndex(area);
        if (index != -1) {
            selectionModel.clearSelection(index);
        }
    }

    /**
     * 指定された要素を選択状態にする
     */
    public boolean select(MapArea area) {
        int index = getIndex(area);
        if (index != -1) {
            selectionModel.select(index);
            return true;
        }
        return false;
    }

    /**
     * 指定された要素を選択状態にする
     */
    public void select(ArrayList<MapArea> areas) {
        if (areas.isEmpty()) {
            return;
        }
        MapArea lastObject = areas.get(areas.size() - 1);
        setEventDisabled(true);     // 無駄なイベント処理をスキップさせる
        for (MapArea area : areas) {
            if (area == lastObject) {
                setEventDisabled(false);
            }
            selectionModel.select(getIndex(area));
        }
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(MapArea area) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index).getId().equals(area.getID())) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(AreaModel area) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index) == area) {
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
    public HashSet<MapArea> getFilteredSet() {
        return filteredSet;
    }

    private synchronized void setEventDisabled(boolean eventDisabled) {
        this.eventDisabled = eventDisabled;
    }

    private synchronized boolean isEventDisabled() {
        return eventDisabled;
    }
}
