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
import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * マップエディタの Links タブ用のペイン
 */
public class LinkPanelFx extends BorderPane {
    /**
     * データ・モデル
     */
    public static class LinkModel {
        private MapLink link;

        public LinkModel(MapLink link) {
            this.link = link;
        }

        public MapLink getLink() { 
            return link;
        }

        public String getId() {
            return link.getID();
        }

        public String getTags() {
            return String.join(", ", link.getTags());
        }

        public Double getLength() {
            return link.getLength();
        }

        public Double getWidth() {
            return link.getWidth();
        }

        public String getFrom() {
            return link.getFrom().getID();
        }

        public String getTo() {
            return link.getTo().getID();
        }

        public String getGroup() {
            return link.getParent().toString();
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

    private TableView<LinkModel> tableView;
    private ObservableList<LinkModel> linkList = FXCollections.observableArrayList();
    private FilteredList<LinkModel> filteredData;
    private TableView.TableViewSelectionModel<LinkModel> selectionModel;
    private String tag = "";
    private TextField tagField = new TextField();
    private Button selectButton = new Button("Select");
    private ToggleButton filterButton = new ToggleButton("Filter");
    private CheckBox regexpCheckBox = new CheckBox("regexp");
    private boolean eventDisabled = false;
    private HashSet<MapLink> filteredSet = new HashSet();

    /**
     * コンストラクタ
     */
    public LinkPanelFx(MapEditor editor, EditorFrameFx frame) {
        this.editor = editor;
        this.frame = frame;

        tableView = createTableView();
        this.setCenter(tableView);

        // タグ検索ペイン

        tagField.setMinWidth(250);

        selectButton.setOnAction(e -> {
            selectionModel.clearSelection();
            editor.deselectLinks();

            tag = tagField.getText().trim();
            if (! tag.isEmpty()) {
                ArrayList<Integer> indices = new ArrayList();
                int lastIndex = -1;
                for (int index = 0; index < linkList.size(); index++) {
                    LinkModel link = linkList.get(index);
                    if (matching(link.getLink(), tag)) {
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
            editor.deselectLinks();
            frame.getCanvas().repaintLater();

            if (filterButton.isSelected()) {
                selectButton.setDisable(true);
                tag = tagField.getText().trim();
            } else {
                selectButton.setDisable(false);
                tag = "";
            }
            filteredSet.clear();
            filteredData.setPredicate(link -> {
                if (! filterButton.isSelected() || tag.isEmpty() || matching(link.getLink(), tag)) {
                    filteredSet.add((MapLink)editor.getMap().getObject(link.getId()));
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
    private TableView<LinkModel> createTableView() {
        TableView<LinkModel> tableView = new TableView<>();
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<LinkModel, String> idCol = new TableColumn("ID");
        TableColumn<LinkModel, String> tagsCol = new TableColumn("Tags");
        TableColumn<LinkModel, Double> lengthCol = new TableColumn("Length");
        TableColumn<LinkModel, Double> widthCol = new TableColumn("Width");
        TableColumn<LinkModel, String> fromCol = new TableColumn("From");
        TableColumn<LinkModel, String> toCol = new TableColumn("To");
        TableColumn<LinkModel, String> groupCol = new TableColumn("Group");

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        lengthCol.setCellValueFactory(new PropertyValueFactory<>("length"));
        widthCol.setCellValueFactory(new PropertyValueFactory<>("width"));
        fromCol.setCellValueFactory(new PropertyValueFactory<>("from"));
        toCol.setCellValueFactory(new PropertyValueFactory<>("to"));
        groupCol.setCellValueFactory(new PropertyValueFactory<>("group"));

        reset();
        filteredData = new FilteredList<>(linkList, p -> true);
        tableView.setItems(filteredData);
        tableView.getColumns().addAll(idCol, tagsCol, lengthCol, widthCol, fromCol, toCol, groupCol);

        selectionModel.getSelectedItems().addListener(new ListChangeListener<LinkModel>() {
            public void onChanged(ListChangeListener.Change<? extends LinkModel> changed) {
                if (isEventDisabled()) {
                    return;
                }
                editor.deselectLinks();
                // 選択状態の要素すべて
                for (LinkModel model : changed.getList()) {
                    int index = getIndex(model);
                    MapLink link = (MapLink)editor.getMap().getObject(model.getId());
                    link.selected = selectionModel.isSelected(index);
                }
                frame.getCanvas().repaintLater();
            }
        });

        tableView.setContextMenu(frame.getEditLinkMenu());

        return tableView;
    }

    /**
     * テーブルの要素を再設定する
     */
    public void reset() {
        ArrayList<LinkModel> linkModels = new ArrayList();
        filteredSet.clear();
        for (MapLink link : editor.getMap().getLinks()) {
            linkModels.add(new LinkModel(link));
            if (! filterButton.isSelected() || tag.isEmpty() || matching(link, tag)) {
                filteredSet.add(link);
            }
        }
        linkList.setAll(linkModels);
    }

    /**
     * タグの照合
     */
    private boolean matching(MapLink link, String tag) {
        if (regexpCheckBox.isSelected()) {
            return link.matchTag(tag) != null;
        }
        return link.hasSubTag(tag);
    }

    /**
     * 選択をすべてクリアしてから指定された要素を選択状態にする
     */
    public boolean clearAndSelect(MapLink link) {
        int index = getIndex(link);
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
    public void clearSelection(MapLink link) {
        int index = getIndex(link);
        if (index != -1) {
            selectionModel.clearSelection(index);
        }
    }

    /**
     * 指定された要素を選択状態にする
     */
    public boolean select(MapLink link) {
        int index = getIndex(link);
        if (index != -1) {
            selectionModel.select(index);
            return true;
        }
        return false;
    }

    /**
     * 指定された要素を選択状態にする
     */
    public void select(ArrayList<MapLink> links) {
        if (links.isEmpty()) {
            return;
        }
        MapLink lastObject = links.get(links.size() - 1);
        setEventDisabled(true);     // 無駄なイベント処理をスキップさせる
        for (MapLink link : links) {
            if (link == lastObject) {
                setEventDisabled(false);
            }
            selectionModel.select(getIndex(link));
        }
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(MapLink link) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index).getId().equals(link.getID())) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(LinkModel link) {
        for (int index = 0; index < filteredData.size(); index++) {
            if (filteredData.get(index) == link) {
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
    public HashSet<MapLink> getFilteredSet() {
        return filteredSet;
    }

    private synchronized void setEventDisabled(boolean eventDisabled) {
        this.eventDisabled = eventDisabled;
    }

    private synchronized boolean isEventDisabled() {
        return eventDisabled;
    }
}
