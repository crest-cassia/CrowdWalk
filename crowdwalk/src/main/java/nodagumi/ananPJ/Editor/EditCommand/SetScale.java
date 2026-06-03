package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;

/**
 * グループの scale をセットする編集コマンド
 */
public class SetScale extends EditCommandBase {
    private String id = null;
    private double scale;
    private double originalScale;

    /**
     * コンストラクタ
     */
    public SetScale(MapPartGroup group, double scale) {
        changeType = ChangeType.GROUP_PARAM;
        id = group.getID();
        this.scale = scale;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, id);
        if (group == null) {
            return false;
        }

        originalScale = group.getScale();
        group.setScale(scale);

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

        group.setScale(originalScale);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", scale=" + scale + "]";
    }
}
