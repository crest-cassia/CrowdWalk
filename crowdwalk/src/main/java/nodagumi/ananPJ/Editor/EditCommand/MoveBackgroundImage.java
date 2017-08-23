package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;

/**
 * 背景画像の位置を変更する編集コマンド
 */
public class MoveBackgroundImage extends EditCommandBase {
    private String groupId;
    private double tx;
    private double ty;
    private double originalTx;
    private double originalTy;

    /**
     * コンストラクタ
     */
    public MoveBackgroundImage(MapPartGroup group, double tx, double ty) {
        groupId = group.getID();
        this.tx = tx;
        this.ty = ty;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        originalTx = group.tx;
        originalTy = group.ty;
        group.tx = tx;
        group.ty = ty;

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

        group.tx = originalTx;
        group.ty = originalTy;

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
        return ((MoveBackgroundImage)command).groupId.equals(groupId);
    }

    /**
     * 続けて実行したコマンドとマージする
     *
     * @param command このコマンドの直後に実行されたコマンド
     */
    public void mergeTo(EditCommandBase command) {
        MoveBackgroundImage _command = (MoveBackgroundImage)command;
        time = _command.getTime();
        tx = _command.tx;
        ty = _command.ty;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", tx=" + tx + ", ty=" + ty + "]";
    }
}
