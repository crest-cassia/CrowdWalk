package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNode.NType;
import nodagumi.Itk.Itk;

/**
 * タグを追加する編集コマンド
 */
public class AddTag extends EditCommandBase {
    private String id = null;
    private String tag;
    private boolean hasTag;

    /**
     * コンストラクタ
     * @param obNode: セットする obNode。
     * @param tag: タグ。
     */
    public AddTag(OBNode obNode, String tag) {
        switch (obNode.getNodeType()) {
        case GROUP:
            changeType = ChangeType.GROUP_TAG;
            break;
        case NODE:
            changeType = ChangeType.NODE_TAG;
            break;
        case LINK:
            changeType = ChangeType.LINK_TAG;
            break;
        case AREA:
            changeType = ChangeType.AREA_TAG;
            break;
        case POLYGON:
            changeType = ChangeType.POLYGON_TAG;
            break;
        }
        id = obNode.getID();
        this.tag = Itk.intern(tag);
    }

    /**
     * 編集を実行する
     * @param editor: MapEditor。
     * @return 実行できたら true。
     */
    public boolean invoke(MapEditor editor) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return false;
        }

        hasTag = (! obNode.addTag(tag));

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     * @param editor: MapEditor。
     * @return undoできたら true。
     */
    public boolean undo(MapEditor editor) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return false;
        }

        if (! hasTag) {
            obNode.removeTag(tag);
        }

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     * @return 文字列表現.
     */
    public String toString() {
        return getName() + "[ID=" + id + ", tag=" + tag + "]";
    }
}
