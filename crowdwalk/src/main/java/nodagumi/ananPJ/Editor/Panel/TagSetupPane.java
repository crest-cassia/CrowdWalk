package nodagumi.ananPJ.Editor.Panel;

import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.OBNode;

/**
 * タグ設定用サブペイン
 */
public class TagSetupPane extends VBox {
    private Dialog parent = null;
    private ArrayList<? extends OBNode> obNodes;
    private ArrayList<String> tags = new ArrayList<String>();
    private ArrayList<CheckBox> tagCheckBoxes = new ArrayList<CheckBox>();

    /**
     * マップエディタ
     */
    private MapEditor editor;

    /**
     * コンストラクタ
     */
    public TagSetupPane(MapEditor editor, ArrayList<? extends OBNode> obNodes, Dialog parent) {
        this.editor = editor;
        this.obNodes = obNodes;
        this.parent = parent;

        init();

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        getChildren().addAll(createRemoveTagsPane(), separator, createAddTagPane());
    }

    /**
     * 初期設定
     */
    private void init() {
        for (OBNode node : obNodes) {
            for (String tag : node.getTags()) {
                if (! tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
    }

    /**
     * タグ削除ペインの生成
     */
    private Pane createRemoveTagsPane() {
        VBox pane = new VBox();

        Label label = new Label("Remove tags");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 4, 0));
        pane.getChildren().add(label);

        if (tags.isEmpty()) {
            Label noTagsLabel = new Label("no tags to remove");
            noTagsLabel.setPadding(new Insets(0, 0, 0, 4));
            pane.getChildren().add(noTagsLabel);
            return pane;
        }

        final Button button = new Button("Remove");
        button.setDisable(true);
        button.setOnAction(e -> removeTags());

        for (String tag : tags) {
            CheckBox cb = new CheckBox(tag);
            cb.setMnemonicParsing(false);
            cb.setPadding(new Insets(0, 0, 4, 4));
            cb.selectedProperty().addListener((ov, oldValue, newValue) -> {
                boolean selected = false;
                for (CheckBox _cb : tagCheckBoxes) {
                    selected |= _cb.isSelected();
                }
                button.setDisable(! selected);
            });
            tagCheckBoxes.add(cb);
            pane.getChildren().add(cb);
        }

        FlowPane flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER_RIGHT);
        flowPane.getChildren().addAll(button);
        pane.getChildren().add(flowPane);
        return pane;
    }

    /**
     * 選択されたタグを削除する
     */
    private void removeTags() {
        editor.startOfCommandBlock();
        for (CheckBox cb : tagCheckBoxes) {
            if (cb.isSelected()) {
                String tag = cb.getText();
                boolean failed = false;
                for (OBNode node : obNodes) {
                    if (node.hasTag(tag)) {
                        if (! editor.invoke(new RemoveTag(node, tag))) {
                            failed = true;
                            break;
                        }
                    }
                }
                if (failed) {
                    break;
                }
            }
        }
        editor.endOfCommandBlock();

        if (parent != null) {
            parent.close();
        }
    }

    /**
     * タグ追加ペインの生成
     */
    private Pane createAddTagPane() {
        VBox pane = new VBox();

        Label label = new Label("Add tag");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));
        pane.getChildren().add(label);

        TextField textField = new TextField();
        Button button = new Button("Add");
        button.setDisable(true);
        button.setOnAction(e -> addTag(textField.getText()));

        textField.setMinWidth(250);
        textField.setOnKeyReleased(e -> {
            String text = textField.getText();
            if (text == null || text.isEmpty() || text.indexOf(" ") != -1) {
                button.setDisable(true);
            } else {
                button.setDisable(false);
                if (e.getCode() == KeyCode.ENTER) {
                    addTag(textField.getText());
                }
            }
        });

        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(8);
        flowPane.getChildren().addAll(textField, button);

        pane.getChildren().add(flowPane);

        return pane;
    }

    /**
     * タグを追加する
     */
    private void addTag(String tag) {
        editor.addTag(obNodes, tag);
        if (parent != null) {
            parent.close();
        }
    }
}
