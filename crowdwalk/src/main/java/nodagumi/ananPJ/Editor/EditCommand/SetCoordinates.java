package nodagumi.ananPJ.Editor.EditCommand;

import java.awt.geom.Point2D;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * ノードの座標値をセットする編集コマンド
 */
public class SetCoordinates extends EditCommandBase {
    private String id;
    private double x;
    private double y;
    private double originalX;
    private double originalY;

    /**
     * コンストラクタ
     */
    public SetCoordinates(MapNode node, double x, double y) {
        heightChangeable = true;
        changeType = ChangeType.NODE_PARAM;
        id = node.getID();
        this.x = x;
        this.y = y;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapNode node = getNode(editor, id);
        if (node == null) {
            return false;
        }

        originalX = node.getX();
        originalY = node.getY();
        node.setPosition(new Point2D.Double(x, y));

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        MapNode node = getNode(editor, id);
        if (node == null) {
            return false;
        }

        node.setPosition(new Point2D.Double(originalX, originalY));

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", x=" + x + ", y=" + y + "]";
    }
}
