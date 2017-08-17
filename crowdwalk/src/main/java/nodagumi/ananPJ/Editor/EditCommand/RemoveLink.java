package nodagumi.ananPJ.Editor.EditCommand;

import java.util.ArrayList;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.Itk.Itk;

/**
 * リンクを削除する編集コマンド
 */
public class RemoveLink extends EditCommandBase {
    private String groupId;
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private double length;
    private double width;
    private ArrayList<String> tags;

    /**
     * コンストラクタ
     */
    public RemoveLink(MapLink link) {
        changeType = ChangeType.LINK_VOLUME;
        groupId = ((MapPartGroup)link.getParent()).getID();
        id = link.getID();
        fromNodeId = link.getFrom().getID();
        toNodeId = link.getTo().getID();
        length = link.getLength();
        width = link.getWidth();
        tags = new ArrayList<String>(link.getTags());
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
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
        MapNode fromNode = getNode(editor, fromNodeId);
        MapNode toNode = getNode(editor, toNodeId);
        if (fromNode == null || toNode == null) {
            editor.displayMessage("Error", getName(), "\"from node\" or \"to node\" not found: ID=" + fromNodeId + ", " + toNodeId);
            invalid = true;
            return false;
        }

        NetworkMap networkMap = editor.getMap();
        if (networkMap.checkObjectId(id)) {
            editor.displayMessage("Error", getName(), "Link ID conflicted: ID=" + id);
            invalid = true;
            return false;
        }
        MapLink link = new MapLink(id, fromNode, toNode, length, width);
        for (String tag : tags) {
            link.addTag(Itk.intern(tag));
        }
        link.prepareAdd();
        networkMap.insertOBNode(group, link);

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
