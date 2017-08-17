package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;

/**
 * 背景画像のスケールを変更する編集コマンド
 */
public class ScaleTheBackgroundImage extends EditCommandBase {
    private String groupId;
    private double sx;
    private double sy;
    private double originalSx;
    private double originalSy;

    /**
     * コンストラクタ
     */
    public ScaleTheBackgroundImage(MapPartGroup group, double sx, double sy) {
        groupId = group.getID();
        this.sx = sx;
        this.sy = sy;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        originalSx = group.sx;
        originalSy = group.sy;
        group.sx = sx;
        group.sy = sy;

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

        group.sx = originalSx;
        group.sy = originalSy;

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
        return ((ScaleTheBackgroundImage)command).groupId.equals(groupId);
    }

    /**
     * 続けて実行したコマンドとマージする
     *
     * @param command このコマンドの直後に実行されたコマンド
     */
    public void mergeTo(EditCommandBase command) {
        ScaleTheBackgroundImage _command = (ScaleTheBackgroundImage)command;
        time = _command.getTime();
        sx = _command.sx;
        sy = _command.sy;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", sx=" + sx + ", sy=" + sy + "]";
    }
}
