package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;

/**
 * シンボリックリンクを生成してマップに追加する編集コマンド
 */
public class AddSymbolicLink extends EditCommandBase {
    private String groupId;
    private String id = null;
    private String originalId = null;

    /**
     * コンストラクタ
     */
    public AddSymbolicLink(MapPartGroup group, OBNode original) {
        changeType = ChangeType.SYMBOLIC_LINK_VOLUME;
        groupId = group.getID();
        originalId = original.getID();
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        NetworkMap networkMap = editor.getMap();
        if (id == null) {
            id = networkMap.assignNewId();
        } else {
            // redo の時は生成済み ID を再利用するので重複チェック
            if (networkMap.checkObjectId(id)) {
                editor.displayMessage("Error", getName(), "Symbolic link ID conflicted: ID=" + id);
                invalid = true;
                return false;
            }
        }
        OBNode original = getOBNode(editor, originalId);
        if (original == null) {
            return false;
        }
        OBNodeSymbolicLink symlink = new OBNodeSymbolicLink(id, original);
        networkMap.insertOBNode(group, symlink);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        OBNodeSymbolicLink symlink = getSymbolicLink(editor, id);
        if (symlink == null) {
            return false;
        }
        editor.getMap().removeOBNode(group, symlink);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", original=" + originalId + "]";
    }
}
