package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.Itk.Itk;

/**
 * グループタグをセットする編集コマンド
 */
public class SetGroupTag extends EditCommandBase {
    private String id = null;
    private String tag;
    private String originalTag;

    /**
     * コンストラクタ
     */
    public SetGroupTag(MapPartGroup group, String tag) {
        changeType = ChangeType.GROUP_TAG;
        id = group.getID();
        this.tag = Itk.intern(tag);
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, id);
        if (group == null) {
            return false;
        }

        originalTag = Itk.intern(group.getTagString());
        group.allTagsClear();
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

        group.allTagsClear();
        group.addTag(originalTag);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", tag=" + tag + "]";
    }
}
