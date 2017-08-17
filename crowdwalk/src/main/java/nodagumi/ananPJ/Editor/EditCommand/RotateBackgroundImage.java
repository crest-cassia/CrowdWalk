package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;

/**
 * 背景画像の角度を変更する編集コマンド
 */
public class RotateBackgroundImage extends EditCommandBase {
    private String groupId;
    private double r;
    private double originalR;

    /**
     * コンストラクタ
     */
    public RotateBackgroundImage(MapPartGroup group, double r) {
        groupId = group.getID();
        this.r = r;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        originalR = group.r;
        group.r = r;

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

        group.r = originalR;

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
        return ((RotateBackgroundImage)command).groupId.equals(groupId);
    }

    /**
     * 続けて実行したコマンドとマージする
     *
     * @param command このコマンドの直後に実行されたコマンド
     */
    public void mergeTo(EditCommandBase command) {
        RotateBackgroundImage _command = (RotateBackgroundImage)command;
        time = _command.getTime();
        r = _command.r;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", r=" + r + "]";
    }
}
