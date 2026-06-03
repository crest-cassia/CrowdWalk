package nodagumi.ananPJ.Editor.EditCommand;

import java.util.ArrayList;
import math.geom3d.Point3D;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.NetworkMap.Polygon.Coordinates;

/**
 * 三角形ポリゴンメッシュの座標データをセットする編集コマンド
 */
public class SetTriangleMeshesCoordinates extends EditCommandBase {
    private String id;
    private ArrayList<Point3D> coordinateList = new ArrayList<Point3D>();
    private ArrayList<Point3D> originalCoordinateList = new ArrayList<Point3D>();

    /**
     * コンストラクタ
     */
    public SetTriangleMeshesCoordinates(MapPolygon polygon, Coordinates coordinates) {
        changeType = ChangeType.POLYGON_PARAM;
        id = polygon.getID();
        coordinateList = (ArrayList<Point3D>)coordinates.getValue().clone();
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPolygon polygon = getPolygon(editor, id);
        if (polygon == null) {
            return false;
        }
        if (! polygon.isTriangleMeshes()) {
            editor.displayMessage("Error", getName(), "Polygon is not triangle mesh: ID=" + id);
            invalid = true;
            return false;
        }

        Coordinates coordinates = polygon.getTriangleMeshes().getCoordinates();
        originalCoordinateList = (ArrayList<Point3D>)coordinates.getValue().clone();
        coordinates.setCoordinates(coordinateList);

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

        Coordinates coordinates = polygon.getTriangleMeshes().getCoordinates();
        coordinates.setCoordinates(originalCoordinateList);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + "]";
    }
}
