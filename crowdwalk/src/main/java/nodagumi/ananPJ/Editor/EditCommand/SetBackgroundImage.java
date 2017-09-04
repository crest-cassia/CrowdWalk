package nodagumi.ananPJ.Editor.EditCommand;

import java.io.File;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.Editor.EditorCanvas;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;

/**
 * 背景画像をセットする編集コマンド
 */
public class SetBackgroundImage extends EditCommandBase {
    private String groupId;
    private String fileName;
    private double tx;
    private double ty;
    private double sx;
    private double sy;
    private double r;
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
    public SetBackgroundImage(MapPartGroup group, String fileName, double tx, double ty) {
        groupId = group.getID();
        this.fileName = fileName;
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

        String filePath = new File(editor.getDir(), fileName).toURI().toString();
        Image image = new Image(filePath);
        if (image.isError()) {
            editor.displayMessage("Error", getName(), "Illegal image file: " + fileName);
            invalid = true;
            return false;
        }

        // 画像の幅が画面の幅の半分になるスケール
        EditorCanvas canvas = editor.getFrame().getCanvas();
        Point2D upperLeft = canvas.pointConvertCanvasToMap(0.0, 0.0);
        Point2D upperRight = canvas.pointConvertCanvasToMap(canvas.getWidth(), 0.0);
        sx = Math.abs(upperRight.getX() - upperLeft.getX()) / 2.0 / image.getWidth();
        sy = sx;
        r = 0.0;

        originalFileName = group.getImageFileName();
        originalTx = group.tx;
        originalTy = group.ty;
        originalSx = group.sx;
        originalSy = group.sy;
        originalR = group.r;
        originalImage = editor.getBackgroundImage(group);   // これは常に null になるはず

        group.setImageFileName(fileName);
        group.tx = tx;
        group.ty = ty;
        group.sx = sx;
        group.sy = sy;
        group.r = r;

        editor.setBackgroundImage(group, image);

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
        return getName() + "[group=" + groupId + ", fileName=\"" + fileName + "\", tx=" + tx + ", ty=" + ty + ", sx=" + sx + ", sy=" + sy + ", r=" + r + "]";
    }
}
