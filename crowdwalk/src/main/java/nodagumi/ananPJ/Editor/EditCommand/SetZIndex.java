package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;

/**
 * ポリゴンの Z-index をセットする編集コマンド
 */
public class SetZIndex extends EditCommandBase {
    private String id;
    private int zIndex;
    private int originalZIndex;

    /**
     * コンストラクタ
     */
    public SetZIndex(MapPolygon polygon, int zIndex) {
        changeType = ChangeType.POLYGON_PARAM;
        id = polygon.getID();
        this.zIndex = zIndex;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPolygon polygon = getPolygon(editor, id);
        if (polygon == null) {
            return false;
        }

        originalZIndex = polygon.getZIndex();
        polygon.setZIndex(zIndex);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        MapPolygon polygon = getPolygon(editor, id);
        if (polygon == null) {
            return false;
        }

        polygon.setZIndex(originalZIndex);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", Z-index=" + zIndex + "]";
    }
}
