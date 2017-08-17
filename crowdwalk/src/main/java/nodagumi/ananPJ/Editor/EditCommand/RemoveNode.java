package nodagumi.ananPJ.Editor.EditCommand;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.Itk.Itk;

/**
 * ノードを削除する編集コマンド
 */
public class RemoveNode extends EditCommandBase {
    private String groupId;
    private String id;
    private Point2D coordinates = new Point2D.Double();
    private double height;
    private ArrayList<String> tags;

    /**
     * コンストラクタ
     */
    public RemoveNode(MapNode node) {
        heightChangeable = true;
        changeType = ChangeType.NODE_VOLUME;
        groupId = ((MapPartGroup)node.getParent()).getID();
        id = node.getID();
        coordinates.setLocation(node.getAbsoluteCoordinates());
        height = node.getHeight();
        tags = new ArrayList<String>(node.getTags());
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        MapNode node = getNode(editor, id);
        if (node == null) {
            return false;
        }
        if (! node.getLinks().isEmpty()) {
            editor.displayMessage("Error", getName(), "An attempt was made to delete a node with links: ID=" + id);
            invalid = true;
            return false;
        }

        editor.getMap().removeOBNode(group, node);

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
            editor.displayMessage("Error", getName(), "Node ID conflicted: ID=" + id);
            invalid = true;
            return false;
        }
        MapNode node = new MapNode(id, coordinates, height);
        for (String tag : tags) {
            node.addTag(Itk.intern(tag));
        }
        networkMap.insertOBNode(group, node);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", x=" + coordinates.getX() + ", y=" + coordinates.getY() + ", height=" + height + "]";
    }
}
