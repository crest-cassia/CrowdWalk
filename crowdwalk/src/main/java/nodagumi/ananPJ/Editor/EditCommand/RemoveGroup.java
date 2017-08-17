package nodagumi.ananPJ.Editor.EditCommand;

import java.awt.geom.Point2D;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.Itk.Itk;

/**
 * グループを削除する編集コマンド
 */
public class RemoveGroup extends EditCommandBase {
    private String id;
    private String tag;
    private double defaultHeight;
    private double scale;
    private int zone;
    private String imageFileName;
    private double tx;
    private double ty;
    private double sx;
    private double sy;
    private double r;
    private Point2D pNorthWest = new Point2D.Double();
    private Point2D pSouthEast = new Point2D.Double();
    private double minHeight;
    private double maxHeight;

    /**
     * コンストラクタ
     */
    public RemoveGroup(MapPartGroup group) {
        changeType = ChangeType.GROUP_VOLUME;
        id = group.getID();
        tag = Itk.intern(group.getTagString());
        defaultHeight = group.getDefaultHeight();
        scale = group.getScale();
        zone = group.getZone();
        imageFileName = group.getImageFileName();
        tx = group.tx;
        ty = group.ty;
        sx = group.sx;
        sy = group.sy;
        r = group.r;
        pNorthWest.setLocation(group.getWest(), group.getNorth());
        pSouthEast.setLocation(group.getEast(), group.getSouth());
        minHeight = group.getMinHeight();
        maxHeight = group.getMaxHeight();
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapPartGroup group = getGroup(editor, id);
        if (group == null) {
            return false;
        }

        NetworkMap networkMap = editor.getMap();
        networkMap.removeOBNode((MapPartGroup)networkMap.getRoot(), group);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        NetworkMap networkMap = editor.getMap();
        if (networkMap.checkObjectId(id)) {
            editor.displayMessage("Error", getName(), "Group ID conflicted: ID=" + id);
            invalid = true;
            return false;
        }
        MapPartGroup group = new MapPartGroup(id, pNorthWest, pSouthEast, 0.0, imageFileName);
        group.setDefaultHeight(defaultHeight);
        group.setScale(scale);
        group.setZone(zone);
        group.tx = tx;
        group.ty = ty;
        group.sx = sx;
        group.sy = sy;
        group.r = r;
        group.setMinHeight(minHeight);
        group.setMaxHeight(maxHeight);
        networkMap.insertOBNode((MapPartGroup)networkMap.getRoot(), group);
        group.addTag(tag);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", tag=" + tag + ", defaultHeight=" + defaultHeight + ", scale=" + scale + ", zone=" + zone + ", imageFileName=" + imageFileName + ", tx=" + tx + ", ty=" + ty + ", sx=" + sx + ", sy=" + sy + ", r=" + r + "]";
    }
}
