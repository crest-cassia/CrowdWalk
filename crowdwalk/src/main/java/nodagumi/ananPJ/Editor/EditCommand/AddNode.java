package nodagumi.ananPJ.Editor.EditCommand;

import javafx.geometry.Point2D;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * ノードを生成してマップに追加する編集コマンド
 */
public class AddNode extends EditCommandBase {
    private String groupId;
    private String id = null;
    private Point2D coordinates;
    private double height;

    /**
     * コンストラクタ
     * @param id: リンクID。
     * @param group: MapPartGroup。
     * @param coordinates: 座標。
     * @param height: ノードの高さ。
     */
    public AddNode(String id, MapPartGroup group, Point2D coordinates, double height) {
        this.id = id;
        heightChangeable = true;
        changeType = ChangeType.NODE_VOLUME;
        groupId = group.getID();
        this.coordinates = coordinates;
        this.height = height;
    }

    /**
     * コンストラクタ
     * @param group: MapPartGroup。
     * @param coordinates: 座標。
     * @param height: ノードの高さ。
     */
    public AddNode(MapPartGroup group, Point2D coordinates, double height) {
        heightChangeable = true;
        changeType = ChangeType.NODE_VOLUME;
        groupId = group.getID();
        this.coordinates = coordinates;
        this.height = height;
    }

    /**
     * ID を取得する
     * @return ノードのID。
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

        NetworkMap networkMap = editor.getMap();
        if (id == null) {
            id = networkMap.assignNewId();
        } else {
            // redo の時は生成済み ID を再利用するので重複チェック
            if (networkMap.checkObjectId(id)) {
                editor.displayMessage("Error", getName(), "Node ID conflicted: ID=" + id);
                invalid = true;
                return false;
            }
        }
        java.awt.geom.Point2D point = new java.awt.geom.Point2D.Double(coordinates.getX(), coordinates.getY());
        MapNode node = new MapNode(id, point, height);
        networkMap.insertOBNode(group, node);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     * @param editor: MapEditor。
     * @return undoできたら true。
     */
    public boolean undo(MapEditor editor) {
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
