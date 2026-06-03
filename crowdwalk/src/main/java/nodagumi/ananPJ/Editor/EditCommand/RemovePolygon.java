package nodagumi.ananPJ.Editor.EditCommand;

import java.util.ArrayList;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.NetworkMap.Polygon.OuterBoundary;
import nodagumi.ananPJ.NetworkMap.Polygon.InnerBoundary;
import nodagumi.ananPJ.NetworkMap.Polygon.TriangleMeshes;
import nodagumi.Itk.Itk;

/**
 * ポリゴンを削除する編集コマンド
 */
public class RemovePolygon extends EditCommandBase {
    private String groupId;
    private String id;
    private int z_index;
    private TriangleMeshes triangleMeshes;
    private OuterBoundary outerBoundary;
    private ArrayList<InnerBoundary> innerBoundaries;
    private ArrayList<String> tags;

    /**
     * コンストラクタ
     */
    public RemovePolygon(MapPolygon polygon) {
        changeType = ChangeType.POLYGON_VOLUME;
        groupId = ((MapPartGroup)polygon.getParent()).getID();
        id = polygon.getID();
        z_index = polygon.getZIndex();
        if (polygon.isTriangleMeshes()) {
            triangleMeshes = polygon.getTriangleMeshes();
        } else {
            outerBoundary = polygon.getOuterBoundary();
            innerBoundaries = polygon.getInnerBoundaries();
        }
        tags = new ArrayList<String>(polygon.getTags());
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        MapPolygon polygon = getPolygon(editor, id);
        if (polygon == null) {
            return false;
        }
        editor.getMap().removeOBNode(group, polygon);

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

        NetworkMap networkMap = editor.getMap();
        if (networkMap.checkObjectId(id)) {
            editor.displayMessage("Error", getName(), "Polygon ID conflicted: ID=" + id);
            invalid = true;
            return false;
        }
        MapPolygon polygon = null;
        if (triangleMeshes != null) {
            polygon = new MapPolygon(id, z_index, triangleMeshes);
        } else {
            polygon = new MapPolygon(id, z_index, outerBoundary, innerBoundaries);
        }
        for (String tag : tags) {
            polygon.addTag(Itk.intern(tag));
        }
        networkMap.insertOBNode(group, polygon);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        if (triangleMeshes != null) {
            return getName() + "[type=TriangleMeshes, group=" + groupId + ", z_index=" + z_index + "]";
        } else {
            return getName() + "[type=PlanePolygon, group=" + groupId + ", z_index=" + z_index + ", innerBoundaries=" + innerBoundaries.size() + "]";
        }
    }
}
