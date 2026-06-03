package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.NetworkMap.Polygon.OuterBoundary;

/**
 * 平面ポリゴンの標高値をセットする編集コマンド
 */
public class SetPolygonHeight extends EditCommandBase {
    private String id;
    private double height;
    private double originalHeight;

    /**
     * コンストラクタ
     */
    public SetPolygonHeight(MapPolygon polygon, double height) {
        changeType = ChangeType.POLYGON_PARAM;
        id = polygon.getID();
        this.height = height;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPolygon polygon = getPolygon(editor, id);
        if (polygon == null) {
            return false;
        }

        originalHeight = polygon.getOuterBoundary().getHeight();
        polygon.getOuterBoundary().setHeight(height);

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

        polygon.getOuterBoundary().setHeight(originalHeight);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", height=" + height + "]";
    }
}
