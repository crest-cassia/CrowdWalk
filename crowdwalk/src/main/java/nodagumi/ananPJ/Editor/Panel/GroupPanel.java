package nodagumi.ananPJ.Editor.Panel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.Gui.GsiTile;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;

/**
 * マップエディタの Groups タブ用のペイン
 */
public class GroupPanel extends TreeView<OBNode> {
    private Image groupImage = new Image(getClass().getResourceAsStream("/img/group_icon.png"));

    /**
     * マップエディタ
     */
    private MapEditor editor;

    private HashMap<OBNode, TreeItem<OBNode>> treeItemMap = new HashMap();
    private ContextMenu rootGroupMenu;
    private ContextMenu groupMenu;
    private MapPartGroup selectedGroup = null;

    /**
     * コンストラクタ
     */
    public GroupPanel(MapEditor editor) {
        this.editor = editor;

        // コンテキストメニューの生成
        createContextMenu();

        this.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
                TreeItem<OBNode> item = getSelectedItem();
                if (item != null) {
                    selectedGroup = (MapPartGroup)item.getValue();
                    if (selectedGroup != editor.getCurrentGroup() && selectedGroup != editor.getMap().getRoot()) {
                        editor.getFrame().clickGroupButton(selectedGroup);
                    }
                }
            }
            if (event.getButton() == MouseButton.SECONDARY) {
                showContextMenu(event.getScreenX(), event.getScreenY());
            }
        });

        construct();
    }

    /**
     * コンテキスト・メニューを生成する
     */
    private void createContextMenu() {
        // ルートグループメニュー

        MenuItem miSetRootGroupAttributes = new MenuItem("Set group attributes");
        miSetRootGroupAttributes.setOnAction(e -> openGroupAttributesDialog(selectedGroup));

        MenuItem miAddGroup = new MenuItem("Add group");
        miAddGroup.setOnAction(e -> {
            MapPartGroup root = (MapPartGroup)editor.getMap().getRoot();
            if (editor.getMap().getGroups().size() == 1 && ! root.getChildNodes().isEmpty()) {
                // ルートグループにマップが存在する場合にはサブグループを作ることは出来ない
                Alert alert = new Alert(AlertType.WARNING, "Can not create subgroup.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            openGroupAdditionDialog();
        });

        rootGroupMenu = new ContextMenu();
        rootGroupMenu.getItems().addAll(miSetRootGroupAttributes, miAddGroup);

        // グループメニュー

        MenuItem miSetGroupAttributes = new MenuItem("Set group attributes");
        miSetGroupAttributes.setOnAction(e -> openGroupAttributesDialog(selectedGroup));

        MenuItem miDuplicateFloor = new MenuItem("Duplicate floor");
        miDuplicateFloor.setOnAction(e -> editor.getFrame().openDuplicateFloorDialog());

        MenuItem miAddTagToAllNodes = new MenuItem("Add tag to all nodes");
        miAddTagToAllNodes.setOnAction(e -> addTagTo(selectedGroup.getChildNodes(), "Add tag to all nodes", "node"));

        MenuItem miAddTagToAllLinks = new MenuItem("Add tag to all links");
        miAddTagToAllLinks.setOnAction(e -> addTagTo(selectedGroup.getChildLinks(), "Add tag to all links", "link"));

        MenuItem miAddTagToAllAreas = new MenuItem("Add tag to all areas");
        miAddTagToAllAreas.setOnAction(e -> addTagTo(selectedGroup.getChildMapAreas(), "Add tag to all areas", "area"));

        MenuItem miRemoveTagFromAllNodes = new MenuItem("Remove tag from all nodes");
        miRemoveTagFromAllNodes.setOnAction(e -> removeTagFrom(selectedGroup.getChildNodes(), "Remove tag from all nodes", "node"));

        MenuItem miRemoveTagFromAllLinks = new MenuItem("Remove tag from all links");
        miRemoveTagFromAllLinks.setOnAction(e -> removeTagFrom(selectedGroup.getChildLinks(), "Remove tag from all links", "link"));

        MenuItem miRemoveTagFromAllAreas = new MenuItem("Remove tag from all areas");
        miRemoveTagFromAllAreas.setOnAction(e -> removeTagFrom(selectedGroup.getChildMapAreas(), "Remove tag from all areas", "area"));

        MenuItem miRemoveGroup = new MenuItem("Remove group");
        miRemoveGroup.setOnAction(e -> {
            if (selectedGroup.children().hasMoreElements()) {
                Alert alert = new Alert(AlertType.WARNING, "It can not be removed until the contents are empty.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            editor.invokeSingleCommand(new RemoveGroup(selectedGroup));
        });

        groupMenu = new ContextMenu();
        groupMenu.getItems().addAll(miSetGroupAttributes, miDuplicateFloor, miAddTagToAllNodes, miAddTagToAllLinks, miAddTagToAllAreas, miRemoveTagFromAllNodes, miRemoveTagFromAllLinks, miRemoveTagFromAllAreas, miRemoveGroup);
    }

    /**
     * コンテキスト・メニューを表示する
     */
    private void showContextMenu(double x, double y) {
        TreeItem<OBNode> item = getSelectedItem();
        if (item == null) {
            return;
        }
        selectedGroup = (MapPartGroup)item.getValue();
        if (selectedGroup == editor.getMap().getRoot()) {
            setContextMenu(rootGroupMenu);
        } else {
            setContextMenu(groupMenu);
        }
    }

    /**
     * マウスがクリックしているアイテムを返す
     */
    private TreeItem<OBNode> getSelectedItem() {
        if (this.getSelectionModel().getSelectedItems().size() != 1) {
            return null;
        }
        return this.getSelectionModel().getSelectedItems().get(0);
    }

    /**
     * 複数の OBNode に指定したタグを付加する
     */
    private void addTagTo(ArrayList<? extends OBNode> obNodes, String title, String objectName) {
        if (obNodes.isEmpty()) {
            Alert alert = new Alert(AlertType.WARNING, "There is no " + objectName + ".", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().setPrefWidth(250);
        dialog.setTitle(title);
        dialog.setHeaderText("Enter the tag");
        String tag = dialog.showAndWait().orElse("").trim();
        if (! tag.isEmpty()) {
            editor.addTag(obNodes, tag);
        }
    }

    /**
     * 複数の OBNode から指定したタグを削除する
     */
    private void removeTagFrom(ArrayList<? extends OBNode> obNodes, String title, String objectName) {
        if (obNodes.isEmpty()) {
            Alert alert = new Alert(AlertType.WARNING, "There is no " + objectName + ".", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().setPrefWidth(250);
        dialog.setTitle(title);
        dialog.setHeaderText("Enter the tag");
        String tag = dialog.showAndWait().orElse("").trim();
        if (! tag.isEmpty()) {
            editor.removeTag(obNodes, tag);
        }
    }

    /**
     * グループ設定ダイアログを開く
     */
    private void openGroupAttributesDialog(MapPartGroup group) {
        // TODO: この処理はマップを読み込んだ時点でおこなってもよい
        if (group.getTags().size() != 1) {
            Alert alert = new Alert(AlertType.ERROR, "Tags: " + group.getTagString() + "\n\nOnly one group tag is allowed.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("Set group attributes");

        Label paramLabel = new Label("Parameters");
        paramLabel.setFont(Font.font("Arial", FontWeight.BOLD, paramLabel.getFont().getSize()));

        // defaultHeight field
        Label defaultHeightLabel = new Label("Default height");
        defaultHeightLabel.setPadding(new Insets(0, 0, 0, 4));
        TextField defaultHeightField = new TextField("" + group.getDefaultHeight());
        defaultHeightField.setMinWidth(100);
        Button defaultHeightButton = new Button("Set");
        EventHandler defaultHeightHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = editor.getFrame().convertToDouble(defaultHeightField.getText());
                if (value != null) {
                    double defaultHeight = value;
                    if (defaultHeight != group.getDefaultHeight()) {
                        editor.invokeSingleCommand(new SetDefaultHeight(group, defaultHeight));
                    }
                    dialog.close();
                }
            }
        };
        defaultHeightField.setOnAction(defaultHeightHandler);
        defaultHeightButton.setOnAction(defaultHeightHandler);

        // zone ChoiceBox
        Label zoneLabel = new Label("Zone");
        zoneLabel.setPadding(new Insets(0, 0, 0, 4));
        ArrayList<String> zones = new ArrayList();
        for (int zone = 0; zone <= GsiTile.JGD2000_JPR_EPSG_NAMES.length - 1; zone++) {
            zones.add("" + zone);
        }
        ChoiceBox<String> zoneChoiceBox = new ChoiceBox(FXCollections.observableArrayList(zones));
        zoneChoiceBox.setValue("" + group.getZone());
        Button zoneButton = new Button("Set");
        zoneButton.setOnAction(e -> {
            int zone = Integer.parseInt((String)zoneChoiceBox.getValue());
            if (zone != group.getZone()) {
                editor.invokeSingleCommand(new SetZone(group, zone));
            }
            dialog.close();
        });
        if (group != editor.getMap().getRoot()) {
            zoneLabel.setDisable(true);
            zoneChoiceBox.setDisable(true);
            zoneButton.setDisable(true);
        }

        // scale field
        Label scaleLabel = new Label("Scale");
        scaleLabel.setPadding(new Insets(0, 0, 0, 4));
        TextField scaleField = new TextField("" + group.getScale());
        scaleField.setMinWidth(100);
        Button scaleButton = new Button("Set");
        EventHandler scaleHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = editor.getFrame().convertToDouble(scaleField.getText());
                if (value != null) {
                    double scale = value;
                    Alert alert = new Alert(AlertType.CONFIRMATION, "Recalculate link lengths of this group?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent()) {
                        if (result.get() == ButtonType.YES) {
                            editor.setScaleAndRecalculateLinkLength(group, scale, true, false);
                        } else if (result.get() == ButtonType.NO) {
                            editor.setScaleAndRecalculateLinkLength(group, scale, false, false);
                        }
                    }
                    dialog.close();
                }
            }
        };
        scaleField.setOnAction(scaleHandler);
        scaleButton.setOnAction(scaleHandler);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.add(defaultHeightLabel, 1, 1);
        grid.add(defaultHeightField, 2, 1);
        grid.add(defaultHeightButton, 3, 1);
        grid.add(zoneLabel, 1, 2);
        grid.add(zoneChoiceBox, 2, 2);
        grid.add(zoneButton, 3, 2);
        grid.add(scaleLabel, 1, 3);
        grid.add(scaleField, 2, 3);
        grid.add(scaleButton, 3, 3);

        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 8, 0));

        // Tag field
        Label tagLabel = new Label("Tag");
        tagLabel.setFont(Font.font("Arial", FontWeight.BOLD, tagLabel.getFont().getSize()));
        tagLabel.setPadding(new Insets(0, 0, 8, 0));
        TextField tagField = new TextField(group.getTagString());
        Button tagButton = new Button("Set");
        tagField.setMinWidth(250);
        tagField.setOnKeyReleased(e -> {
            String text = tagField.getText();
            tagButton.setDisable(text.isEmpty() || text.indexOf(" ") != -1 || sameTagExists(null, text));
        });
        tagField.setOnAction(e -> {
            String text = tagField.getText();
            if (! text.equals(group.getTagString())) {
                if (text.isEmpty() || text.indexOf(" ") != -1 || sameTagExists(group, text)) {
                    Alert alert = new Alert(AlertType.WARNING, "\"" + text + "\" is an invalid tag.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                editor.invokeSingleCommand(new SetGroupTag(group, text));
            }
            dialog.close();
        });
        tagButton.setDisable(true);
        tagButton.setOnAction(e -> {
            editor.invokeSingleCommand(new SetGroupTag(group, tagField.getText()));
            dialog.close();
        });
        FlowPane tagFlowPane = new FlowPane();
        tagFlowPane.setHgap(8);
        tagFlowPane.getChildren().addAll(tagField, tagButton);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(paramLabel, grid, separator, tagLabel, tagFlowPane);

        dialog.getDialogPane().setContent(vBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    /**
     * グループ追加ダイアログを開く
     */
    private void openGroupAdditionDialog() {
        // 初期値として利用するグループ(root 以外の方が参考になる)
        MapPartGroup root = (MapPartGroup)editor.getMap().getRoot();
        MapPartGroup group = root;
        for (MapPartGroup _group : editor.getMap().getGroups()) {
            if (_group != group) {
                group = _group;
                break;
            }
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("Add group");
        DialogPane pane = dialog.getDialogPane();

        Label paramLabel = new Label("Parameters");
        paramLabel.setFont(Font.font("Arial", FontWeight.BOLD, paramLabel.getFont().getSize()));

        // defaultHeight field
        Label defaultHeightLabel = new Label("Default height");
        defaultHeightLabel.setPadding(new Insets(0, 0, 0, 4));
        TextField defaultHeightField = new TextField("" + group.getDefaultHeight());
        defaultHeightField.setMinWidth(100);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.add(defaultHeightLabel, 1, 1);
        grid.add(defaultHeightField, 2, 1);

        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 8, 0));

        // Tag field
        Label tagLabel = new Label("Tag");
        tagLabel.setFont(Font.font("Arial", FontWeight.BOLD, tagLabel.getFont().getSize()));
        tagLabel.setPadding(new Insets(0, 0, 8, 0));
        TextField tagField = new TextField();
        tagField.setMinWidth(250);
        tagField.setOnKeyReleased(e -> {
            String text = tagField.getText();
            pane.lookupButton(ButtonType.OK).setDisable(text.isEmpty() || text.indexOf(" ") != -1 || sameTagExists(null, text));
        });
        FlowPane tagFlowPane = new FlowPane();
        tagFlowPane.getChildren().add(tagField);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(paramLabel, grid, separator, tagLabel, tagFlowPane);
        pane.setContent(vBox);

        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        pane.lookupButton(ButtonType.OK).setDisable(true);
        pane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, e -> {
            String tag = tagField.getText();
            Double defaultHeight = editor.getFrame().convertToDouble(defaultHeightField.getText());
            if (defaultHeight == null) {
                e.consume();
                return;
            }
            editor.invokeSingleCommand(new AddGroup(tag, defaultHeight, 0));
        });

        dialog.showAndWait();
    }

    /**
     * グループタグの重複チェック
     */
    public boolean sameTagExists(MapPartGroup group, String tag) {
        for (MapPartGroup _group : editor.getMap().getGroups()) {
            if (_group != group && _group.getTagString().equals(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ツリー・ビューを構築する
     */
    public void construct() {
        // 最初に root を登録する
        MapPartGroup root = (MapPartGroup)editor.getMap().getRoot();
        TreeItem<OBNode> rootItem = new TreeItem<OBNode>(root, new ImageView(groupImage));
        rootItem.setExpanded(true);
        setRoot(rootItem);
        treeItemMap.put(root, rootItem);

        // 残りのグループを登録する
        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == root) {
                continue;
            }
            String tag = String.join(", ", group.getTags());
            TreeItem<OBNode> groupItem = new TreeItem<OBNode>(group, new ImageView(groupImage));
            rootItem.getChildren().add(groupItem);
            treeItemMap.put(group, groupItem);
        }
    }

    /**
     * ツリー・ビューを削除する
     */
    public void clear() {
        TreeItem<OBNode> rootItem = this.getRoot();
        if (rootItem != null) {
            for (TreeItem<OBNode> groupItem : rootItem.getChildren()) {
                groupItem.getChildren().clear();
            }
            rootItem.getChildren().clear();
        }
        treeItemMap.clear();
    }
}
