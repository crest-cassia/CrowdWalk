package nodagumi.ananPJ.Editor.EditCommand;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.Itk.Itk;

/**
 * エリアを削除する編集コマンド
 */
public class RemoveArea extends EditCommandBase {
    private String groupId;
    private String id;
    private Rectangle2D bounds;
    private double minHeight;
    private double maxHeight;
    private double angle;
    private ArrayList<String> tags;

    /**
     * コンストラクタ
     */
    public RemoveArea(MapArea area) {
        changeType = ChangeType.AREA_VOLUME;
        groupId = ((MapPartGroup)area.getParent()).getID();
        id = area.getID();
        bounds = (Rectangle2D)((Rectangle2D)area.getShape()).clone();
        minHeight = area.getMinHeight();
        maxHeight = area.getMaxHeight();
        angle = area.getAngle();
        tags = new ArrayList<String>(area.getTags());
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, groupId);
        if (group == null) {
            return false;
        }

        MapArea area = getArea(editor, id);
        if (area == null) {
            return false;
        }
        editor.getMap().removeOBNode(group, area);

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
            editor.displayMessage("Error", getName(), "Area ID conflicted: ID=" + id);
            invalid = true;
            return false;
        }
        MapArea area = new MapAreaRectangle(id, bounds, minHeight, maxHeight, angle);
        for (String tag : tags) {
            area.addTag(Itk.intern(tag));
        }
        networkMap.insertOBNode(group, area);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[group=" + groupId + ", westX=" + bounds.getMinX() + ", northY=" + bounds.getMinY() + ", eastX=" + bounds.getMaxX() + ", southY=" + bounds.getMaxY() + "]";
    }
}
