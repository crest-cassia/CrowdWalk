package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;

/**
 * シンボリックリンクを削除する編集コマンド
 */
public class RemoveSymbolicLink extends EditCommandBase {
    private String groupId;
    private String id;
    private String originalId;

    /**
     * コンストラクタ
     */
    public RemoveSymbolicLink(MapPartGroup group, OBNodeSymbolicLink symlink) {
        changeType = ChangeType.SYMBOLIC_LINK_VOLUME;
        groupId = group.getID();
        id = symlink.getID();
        originalId = symlink.getOriginal().getID();
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        OBNodeSymbolicLink symlink = getSymbolicLink(editor, id);
        if (symlink == null) {
            return false;
        }
        editor.getMap().removeOBNode(group, symlink);

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

        NetworkMap networkMap = editor.getMap();
        if (networkMap.checkObjectId(id)) {
            editor.displayMessage("Error", getName(), "Symbolic link ID conflicted: ID=" + id);
            invalid = true;
            return false;
        }
        OBNode original = getOBNode(editor, originalId);
        if (original == null) {
            return false;
        }
        OBNodeSymbolicLink symlink = new OBNodeSymbolicLink(id, original);
        networkMap.insertOBNode(group, symlink);

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
