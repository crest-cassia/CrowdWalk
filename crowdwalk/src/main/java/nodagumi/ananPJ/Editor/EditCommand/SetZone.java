package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;

/**
 * グループの系番号をセットする編集コマンド
 */
public class SetZone extends EditCommandBase {
    private String id = null;
    private int zone;
    private int originalZone;

    /**
     * コンストラクタ
     */
    public SetZone(MapPartGroup group, int zone) {
        changeType = ChangeType.GROUP_PARAM;
        id = group.getID();
        this.zone = zone;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, id);
        if (group == null) {
            return false;
        }

        originalZone = group.getZone();
        group.setZone(zone);

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

        group.setZone(originalZone);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", zone=" + zone + "]";
    }
}
