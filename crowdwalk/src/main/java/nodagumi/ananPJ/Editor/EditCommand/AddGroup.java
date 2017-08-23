package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.Itk.Itk;

/**
 * 新しいグループを生成してルートグループ下に追加する編集コマンド
 */
public class AddGroup extends EditCommandBase {
    private String id = null;
    private String tag;
    private double defaultHeight;
    private int zone;

    /**
     * コンストラクタ
     */
    public AddGroup(String tag, double defaultHeight, int zone) {
        changeType = ChangeType.GROUP_VOLUME;
        this.tag = Itk.intern(tag);
        this.defaultHeight = defaultHeight;
        this.zone = zone;
    }

    /**
     * ID を取得する
     */
    public String getId() {
        return id;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        NetworkMap networkMap = editor.getMap();
        if (id == null) {
            id = networkMap.assignNewId();
        } else {
            // redo の時は生成済み ID を再利用するので重複チェック
            if (networkMap.checkObjectId(id)) {
                editor.displayMessage("Error", getName(), "Group ID conflicted: ID=" + id);
                invalid = true;
                return false;
            }
        }
        MapPartGroup group = new MapPartGroup(id);
        group.setDefaultHeight(defaultHeight);
        group.setZone(zone);
        networkMap.insertOBNode((MapPartGroup)networkMap.getRoot(), group);
        group.addTag(tag);

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
        editor.getMap().removeOBNode((MapPartGroup)editor.getMap().getRoot(), group);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[tag=" + tag + ", defaultHeight=" + defaultHeight + ", zone=" + zone + "]";
    }
}
