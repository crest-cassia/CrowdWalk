package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;

/**
 * グループの defaultHeight をセットする編集コマンド
 */
public class SetDefaultHeight extends EditCommandBase {
    private String id = null;
    private double defaultHeight;
    private double originalDefaultHeight;

    /**
     * コンストラクタ
     */
    public SetDefaultHeight(MapPartGroup group, double defaultHeight) {
        changeType = ChangeType.GROUP_PARAM;
        id = group.getID();
        this.defaultHeight = defaultHeight;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, id);
        if (group == null) {
            return false;
        }

        originalDefaultHeight = group.getDefaultHeight();
        group.setDefaultHeight(defaultHeight);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        MapPartGroup group = getGroup(editor, id);
        if (group == null) {
            return false;
        }

        group.setDefaultHeight(originalDefaultHeight);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", defaultHeight=" + defaultHeight + "]";
    }
}
