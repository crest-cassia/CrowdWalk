package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * ノードの標高値をセットする編集コマンド
 */
public class SetHeight extends EditCommandBase {
    private String id;
    private double height;
    private double originalHeight;

    /**
     * コンストラクタ
     */
    public SetHeight(MapNode node, double height) {
        heightChangeable = true;
        changeType = ChangeType.NODE_PARAM;
        id = node.getID();
        this.height = height;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapNode node = getNode(editor, id);
        if (node == null) {
            return false;
        }

        originalHeight = node.getHeight();
        node.setHeight(height);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        MapNode node = getNode(editor, id);
        if (node == null) {
            return false;
        }

        node.setHeight(originalHeight);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", height=" + height + "]";
    }
}
