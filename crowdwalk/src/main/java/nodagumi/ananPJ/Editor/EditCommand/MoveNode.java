package nodagumi.ananPJ.Editor.EditCommand;

import javafx.geometry.Point2D;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * ノードの位置を変更する編集コマンド
 */
public class MoveNode extends EditCommandBase {
    private String id;
    private double x;
    private double y;
    private double originalX;
    private double originalY;

    /**
     * コンストラクタ
     */
    public MoveNode(MapNode node, double x, double y) {
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
        node.setPosition(new java.awt.geom.Point2D.Double(x, y));

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

        node.setPosition(new java.awt.geom.Point2D.Double(originalX, originalY));

        invoked = false;
        return true;
    }

    /**
     * 続けて実行したコマンドとマージすることは可能か?
     *
     * @param command このコマンドの直後に実行されたコマンド
     */
    public boolean isMergeable(EditCommandBase command) {
        if (command.getTime() - time > 1000L || command.getClass() != this.getClass()) {
            return false;
        }
        return ((MoveNode)command).id.equals(id);
    }

    /**
     * 続けて実行したコマンドとマージする
     *
     * @param command このコマンドの直後に実行されたコマンド
     */
    public void mergeTo(EditCommandBase command) {
        MoveNode _command = (MoveNode)command;
        time = _command.getTime();
        x = _command.x;
        y = _command.y;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", x=" + x + ", y=" + y + "]";
    }
}
