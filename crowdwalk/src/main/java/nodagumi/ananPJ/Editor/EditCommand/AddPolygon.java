package nodagumi.ananPJ.Editor.EditCommand;

import java.util.ArrayList;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.NetworkMap.Polygon.OuterBoundary;
import nodagumi.ananPJ.NetworkMap.Polygon.InnerBoundary;
import nodagumi.ananPJ.NetworkMap.Polygon.TriangleMeshes;

/**
 * ポリゴンを生成してマップに追加する編集コマンド
 */
public class AddPolygon extends EditCommandBase {
    private String groupId;
    private String id;
    private int z_index;
    private TriangleMeshes triangleMeshes;
    private OuterBoundary outerBoundary;
    private ArrayList<InnerBoundary> innerBoundaries;

    /**
     * コンストラクタ
     * @param group: MapPartGroup。
     * @param z_index: 
     * @param triangleMeshes: 3角メッシュ。
     */
    public AddPolygon(MapPartGroup group, int z_index, TriangleMeshes triangleMeshes) {
        changeType = ChangeType.POLYGON_VOLUME;
        groupId = group.getID();
        this.z_index = z_index;
        this.triangleMeshes = triangleMeshes;
    }

    /**
     * コンストラクタ
     * @param group: MapPartGroup。
     * @param z_index: 
     * @param outerBoundary: 外枠。
     * @param innerBoundaries: 内枠のリスト。
     * @param triangleMeshes: 3角メッシュ。
     */
    public AddPolygon(MapPartGroup group, int z_index, OuterBoundary outerBoundary, ArrayList<InnerBoundary> innerBoundaries) {
        changeType = ChangeType.POLYGON_VOLUME;
        groupId = group.getID();
        this.z_index = z_index;
        this.outerBoundary = outerBoundary.clone();
        this.innerBoundaries = innerBoundaries;
    }

    /**
     * ID を取得する
     * @return コマンドのID。
     */
    public String getId() {
        return id;
    }

    /**
     * 編集を実行する
     * @param editor: MapEditor。
     * @return 実行できたらtrue。
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        NetworkMap networkMap = editor.getMap();
        if (id == null) {
            id = networkMap.assignNewId();
        } else {
            // redo の時は生成済み ID を再利用するので重複チェック
            if (networkMap.checkObjectId(id)) {
                editor.displayMessage("Error", getName(), "MapPolygon ID conflicted: ID=" + id);
                invalid = true;
                return false;
            }
        }
        MapPolygon polygon = null;
        if (triangleMeshes != null) {
            polygon = new MapPolygon(id, z_index, triangleMeshes);
        } else {
            polygon = new MapPolygon(id, z_index, outerBoundary, innerBoundaries);
        }
        networkMap.insertOBNode(group, polygon);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     * @param editor: MapEditor。
     * @return undoできたらtrue。
     */
    public boolean undo(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        MapPolygon polygon = getPolygon(editor, id);
        if (polygon == null) {
            return false;
        }
        editor.getMap().removeOBNode(group, polygon);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     * @return コマンドの文字列表現
     */
    public String toString() {
        if (triangleMeshes != null) {
            return getName() + "[type=TriangleMeshes, group=" + groupId + ", z_index=" + z_index + "]";
        } else {
            return getName() + "[type=PlanePolygon, group=" + groupId + ", z_index=" + z_index + ", innerBoundaries=" + innerBoundaries.size() + "]";
        }
    }
}
