package nodagumi.ananPJ.Editor.EditCommand;

import javafx.scene.image.Image;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;

/**
 * 背景画像を削除する編集コマンド
 */
public class RemoveBackgroundImage extends EditCommandBase {
    private String groupId;
    private String fileName;
    private double tx;
    private double ty;
    private String originalFileName;
    private double originalTx;
    private double originalTy;
    private double originalSx;
    private double originalSy;
    private double originalR;
    private Image originalImage;

    /**
     * コンストラクタ
     */
    public RemoveBackgroundImage(MapPartGroup group) {
        groupId = group.getID();
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        originalFileName = group.getImageFileName();
        originalTx = group.tx;
        originalTy = group.ty;
        originalSx = group.sx;
        originalSy = group.sy;
        originalR = group.r;
        // TODO: 画像オブジェクトをそのまま保存しているのでメモリを消費しすぎる様ならば別の方法を取る
        originalImage = editor.getBackgroundImage(group);

        group.setImageFileName("");
        group.tx = 0.0;
        group.ty = 0.0;
        group.sx = 1.0;
        group.sy = 1.0;
        group.r = 0.0;
        editor.setBackgroundImage(group, null);

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

        group.setImageFileName(originalFileName);
        group.tx = originalTx;
        group.ty = originalTy;
        group.sx = originalSx;
        group.sy = originalSy;
        group.r = originalR;
        editor.setBackgroundImage(group, originalImage);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", fileName=\"" + fileName + "\", tx=" + tx + ", ty=" + ty + "]";
    }
}
