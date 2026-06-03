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
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * マップエディタの Nodes タブ用のペイン
 */
public class NodePanelFx extends BorderPane {
    /**
     * データ・モデル
     */
    public static class NodeModel {
        private MapNode node;

        public NodeModel(MapNode node) {
            this.node = node;
        }

        public MapNode getNode() { 
            return node;
        }

        public String getId() {
            return node.getID();
        }

        public String getTags() {
            return String.join(", ", node.getTags());
        }

        public Double getX() {
            return node.getX();
        }

        public Double getY() {
            return node.getY();
        }

        public Double getHeight() {
            return node.getHeight();
        }

        public String getGroup() {
            return node.getParent().toString();
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

    private TableView<NodeModel> tableView;
    private ObservableList<NodeModel> nodeList = FXCollections.observableArrayList();
    private FilteredList<NodeModel> filteredData;
    private TableView.TableViewSelectionModel<NodeModel> selectionModel;
    private String tag = "";
    private TextField tagField = new TextField();
    private Button selectButton = new Button("Select");
    private ToggleButton filterButton = new ToggleButton("Filter");
    private CheckBox regexpCheckBox = new CheckBox("regexp");
    private boolean eventDisabled = false;
    private HashSet<MapNode> filteredSet = new HashSet();

    /**
     * コンストラクタ
     */
    public NodePanelFx(MapEditor editor, EditorFrameFx frame) {
        this.editor = editor;
        this.frame = frame;

        tableView = createTableView();
        this.setCenter(tableView);

        // タグ検索ペイン

        tagField.setMinWidth(250);

        selectButton.setOnAction(e -> {
            selectionModel.clearSelection();
            editor.deselectNodes();

            tag = tagField.getText().trim();
            if (! tag.isEmpty()) {
                ArrayList<Integer> indices = new ArrayList();
                int lastIndex = -1;
                for (int index = 0; index < nodeList.size(); index++) {
                    NodeModel node = nodeList.get(index);
                    if (matching(node.getNode(), tag)) {
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
            editor.deselectNodes();
            frame.getCanvas().repaintLater();

            if (filterButton.isSelected()) {
                selectButton.setDisable(true);
                tag = tagField.getText().trim();
            } else {
                selectButton.setDisable(false);
                tag = "";
            }
            filteredSet.clear();
            filteredData.setPredicate(node -> {
                if (! filterButton.isSelected() || tag.isEmpty() || matching(node.getNode(), tag)) {
                    filteredSet.add((MapNode)editor.getMap().getObject(node.getId()));
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
    private TableView<NodeModel> createTableView() {
        TableView<NodeModel> tableView = new TableView<>();
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<NodeModel, String> idCol = new TableColumn("ID");
        TableColumn<NodeModel, String> tagsCol = new TableColumn("Tags");
        TableColumn<NodeModel, Double> xCol = new TableColumn("X");
        TableColumn<NodeModel, Double> yCol = new TableColumn("Y");
        TableColumn<NodeModel, Double> heightCol = new TableColumn("Height");
        TableColumn<NodeModel, String> groupCol = new TableColumn("Group");

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        xCol.setCellValueFactory(new PropertyValueFactory<>("x"));
        yCol.setCellValueFactory(new PropertyValueFactory<>("y"));
        heightCol.setCellValueFactory(new PropertyValueFactory<>("height"));
        groupCol.setCellValueFactory(new PropertyValueFactory<>("group"));

        reset();
        filteredData = new FilteredList<>(nodeList, p -> true);
        tableView.setItems(filteredData);
        tableView.getColumns().addAll(idCol, tagsCol, xCol, yCol, heightCol, groupCol);

        selectionModel.getSelectedItems().addListener(new ListChangeListener<NodeModel>() {
            public void onChanged(ListChangeListener.Change<? extends NodeModel> changed) {
                if (isEventDisabled()) {
                    return;
                }
                editor.deselectNodes();
                // 選択状態の要素すべて
                for (NodeModel model : changed.getList()) {
                    int index = getIndex(model);
                    MapNode node = (MapNode)editor.getMap().getObject(model.getId());
                    node.selected = selectionModel.isSelected(index);
                }
                frame.getCanvas().repaintLater();
            }
        });

        tableView.setContextMenu(frame.getEditNodeMenu());

        return tableView;
    }

    /**
     * テーブルの要素を再設定する
     */
    public void reset() {
        ArrayList<NodeModel> nodeModels = new ArrayList();
        filteredSet.clear();
        for (MapNode node : editor.getMap().getNodes()) {
            nodeModels.add(new NodeModel(node));
            if (! filterButton.isSelected() || tag.isEmpty() || matching(node, tag)) {
                filteredSet.add(node);
            }
        }
        nodeList.setAll(nodeModels);
    }

    /**
     * タグの照合
     */
    private boolean matching(MapNode node, String tag) {
        if (regexpCheckBox.isSelected()) {
            return node.matchTag(tag) != null;
        }
        return node.hasSubTag(tag);
    }

    /**
     * 選択をすべてクリアしてから指定された要素を選択状態にする
     */
    public boolean clearAndSelect(MapNode node) {
        int index = getIndex(node);
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
    public void clearSelection(MapNode node) {
        int index = getIndex(node);
        if (index != -1) {
            selectionModel.clearSelection(index);
        }
    }

    /**
     * 指定された要素を選択状態にする
     */
    public boolean select(MapNode node) {
        int index = getIndex(node);
        if (index != -1) {
            selectionModel.select(index);
            return true;
        }
        return false;
    }

    /**
     * 指定された要素を選択状態にする
     */
    public void select(ArrayList<MapNode> nodes) {
        if (nodes.isEmpty()) {
            return;
        }
        MapNode lastObject = nodes.get(nodes.size() - 1);
        setEventDisabled(true);     // 無駄なイベント処理をスキップさせる
        for (MapNode node : nodes) {
            if (node == lastObject) {
                setEventDisabled(false);
            }
            selectionModel.select(getIndex(node));
        }
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(MapNode node) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index).getId().equals(node.getID())) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(NodeModel node) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index) == node) {
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
     * フィルタリングされたノードのセットを取得する
     */
    public HashSet<MapNode> getFilteredSet() {
        return filteredSet;
    }

    private synchronized void setEventDisabled(boolean eventDisabled) {
        this.eventDisabled = eventDisabled;
    }

    private synchronized boolean isEventDisabled() {
        return eventDisabled;
    }
}
