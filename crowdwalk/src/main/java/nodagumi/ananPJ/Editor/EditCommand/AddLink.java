package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * リンクを生成してマップに追加する編集コマンド
 */
public class AddLink extends EditCommandBase {
    private String groupId;
    private String id = null;
    private String fromNodeId;
    private String toNodeId;
    private double length;
    private double width;

    /**
     * コンストラクタ
     * @param id: リンクID。
     * @param fromNode: 始点ノード。
     * @param toNode: 終点ノード。
     * @param length: リンクの長さ。
     * @param width: リンクの幅。
     */
    public AddLink(String id, MapNode fromNode, MapNode toNode, double length, double width) {
        this.id = id;
        changeType = ChangeType.LINK_VOLUME;
        groupId = ((MapPartGroup)fromNode.getParent()).getID();
        fromNodeId = fromNode.getID();
        toNodeId = toNode.getID();
        this.length = length;
        this.width = width;
    }

    /**
     * コンストラクタ
     * @param fromNode: 始点ノード。
     * @param toNode: 終点ノード。
     * @param length: リンクの長さ。
     * @param width: リンクの幅。
     */
    public AddLink(MapNode fromNode, MapNode toNode, double length, double width) {
        changeType = ChangeType.LINK_VOLUME;
        groupId = ((MapPartGroup)fromNode.getParent()).getID();
        fromNodeId = fromNode.getID();
        toNodeId = toNode.getID();
        this.length = length;
        this.width = width;
    }

    /**
     * ID を取得する
     * @return リンクの id。
     */
    public String getId() {
        return id;
    }

    /**
     * 編集を実行する
     * @param editor: MapEditor。
     * @return 実行できたら true。
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }
        MapNode fromNode = getNode(editor, fromNodeId);
        MapNode toNode = getNode(editor, toNodeId);
        if (fromNode == null || toNode == null) {
            editor.displayMessage("Error", getName(), "\"from node\" or \"to node\" not found: ID=" + fromNodeId + ", " + toNodeId);
            invalid = true;
            return false;
        }

        NetworkMap networkMap = editor.getMap();
        if (id == null) {
            id = networkMap.assignNewId();
        } else {
            // redo の時は生成済み ID を再利用するので重複チェック
            if (networkMap.checkObjectId(id)) {
                editor.displayMessage("Error", getName(), "Link ID conflicted: ID=" + id);
                invalid = true;
                return false;
            }
        }
        MapLink link = new MapLink(id, fromNode, toNode, length, width);
        link.prepareAdd();
        networkMap.insertOBNode(group, link);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     * @param editor: MapEditor。
     * @return undo できたら true。
     */
    public boolean undo(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        MapLink link = getLink(editor, id);
        if (link == null) {
            return false;
        }
        link.prepareRemove();
        editor.getMap().removeOBNode(group, link);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", fromNode=" + fromNodeId + ", toNode=" + toNodeId + ", length=" + length + ", width=" + width + "]";
    }
}
