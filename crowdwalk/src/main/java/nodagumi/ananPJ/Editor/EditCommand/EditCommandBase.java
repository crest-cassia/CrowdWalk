package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;

/**
 * Undo が可能な編集コマンドの抽象クラス
 */
public abstract class EditCommandBase {
    /**
     * 変更点の種類
     */
    public static enum ChangeType {
        NONE,
        GROUP_PARAM,
        GROUP_TAG,
        GROUP_VOLUME,
        NODE_PARAM,
        NODE_TAG,
        NODE_VOLUME,
        LINK_PARAM,
        LINK_TAG,
        LINK_VOLUME,
        AREA_PARAM,
        AREA_TAG,
        AREA_VOLUME,
        POLYGON_PARAM,
        POLYGON_TAG,
        POLYGON_VOLUME,
        SYMBOLIC_LINK_VOLUME
    }

    protected long time = 0;
    protected boolean invoked = false;
    protected boolean invalid = false;
    protected boolean first = false;
    protected boolean last = false;
    protected boolean heightChangeable = false;
    protected ChangeType changeType = ChangeType.NONE;

    /**
     * 編集を実行する
     * @param editor: MapEditor。
     * @return 実行できたらtrue。
     */
    public abstract boolean invoke(MapEditor editor);

    /**
     * 編集を取り消す
     * @param editor: MapEditor。
     * @return undo できたら true。
     */
    public abstract boolean undo(MapEditor editor);

    /**
     * このコマンドのクラス名を取得する
     * @return コマンドのクラス名。
     */
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * このコマンドのミリ秒で表される実行時刻をセットする
     */
    public void setTime() {
        time = System.currentTimeMillis();
    }

    /**
     * このコマンドのミリ秒で表される実行時刻を取得する
     * @return 実行時刻。
     */
    public long getTime() {
        return time;
    }

    /**
     * このコマンドは実行済みか?
     * @return 実行済みなら true ;
     */
    public boolean isInvoked() {
        return invoked;
    }

    /**
     * このコマンドは無効か?
     * @return 無効なら true ;
     */
    public boolean isInvalid() {
        return invalid;
    }

    /**
     * 編集コマンドブロックの最初のコマンドかどうかの設定
     * @param first : this.first に設定する値。
     */
    public void setFirst(boolean first) {
        this.first = first;
    }

    /**
     * 編集コマンドブロックの最初のコマンドか?
     * @return 最初なら true。
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * 編集コマンドブロックの最後のコマンドかどうかの設定
     * @param last: true の場合最後とみなされる。
     */
    public void setLast(boolean last) {
        this.last = last;
    }

    /**
     * 編集コマンドブロックの最後のコマンドか?
     * @return 最後なら true。
     */
    public boolean isLast() {
        return last;
    }

    /**
     * 標高可変か?
     * @return 可変なら true。
     */
    public boolean isHeightChangeable() {
        return heightChangeable;
    }

    /**
     * 変更点を取得する
     * @return ChangeType
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * 続けて実行したコマンドとマージすることは可能か?
     *
     * @param command このコマンドの直後に実行されたコマンド
     * @return マージ可能であれば true。
     */
    public boolean isMergeable(EditCommandBase command) {
        return false;
    }

    /**
     * 続けて実行したコマンドとマージする
     *
     * @param command このコマンドの直後に実行されたコマンド
     */
    public void mergeTo(EditCommandBase command) {}

    /**
     * OBNode を取得する
     * @param editor: MapEditor。
     * @param id: OBNode の ID。
     * @return id で指定されたOBNode.
     */
    public OBNode getOBNode(MapEditor editor, String id) {
        OBNode obNode = editor.getMap().getObject(id);
        if (obNode == null) {
            editor.displayMessage("Error", getName(), "OBNode not found: ID=" + id);
            invalid = true;
            return null;
        }
        return obNode;
    }

    /**
     * グループを取得する
     * @param editor: MapEditor
     * @param id: グループのID。
     * @return id をもつグループ。
     */
    public MapPartGroup getGroup(MapEditor editor, String id) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return null;
        }
        if (obNode.getNodeType() != OBNode.NType.GROUP) {
            editor.displayMessage("Error", getName(), "Group was lost: ID=" + id);
            invalid = true;
            return null;
        }
        return (MapPartGroup)obNode;
    }

    /**
     * ノードを取得する
     * @param editor: MapEditor
     * @param id: ノードのID。
     * @return id をもつノード。
     */
    public MapNode getNode(MapEditor editor, String id) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return null;
        }
        if (obNode.getNodeType() != OBNode.NType.NODE) {
            editor.displayMessage("Error", getName(), "Node was lost: ID=" + id);
            invalid = true;
            return null;
        }
        return (MapNode)obNode;
    }

    /**
     * リンクを取得する
     * @param editor: MapEditor
     * @param id: リンクのID。
     * @return id をもつリンク。
     */
    public MapLink getLink(MapEditor editor, String id) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return null;
        }
        if (obNode.getNodeType() != OBNode.NType.LINK) {
            editor.displayMessage("Error", getName(), "Link was lost: ID=" + id);
            invalid = true;
            return null;
        }
        return (MapLink)obNode;
    }

    /**
     * エリアを取得する
     * @param editor: MapEditor。
     * @param id: エリアのID。
     * @return id をもつエリア。
     */
    public MapArea getArea(MapEditor editor, String id) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return null;
        }
        if (obNode.getNodeType() != OBNode.NType.AREA) {
            editor.displayMessage("Error", getName(), "Area was lost: ID=" + id);
            invalid = true;
            return null;
        }
        return (MapArea)obNode;
    }

    /**
     * ポリゴンを取得する
     * @param editor: MapEditor。
     * @param id: ポリゴンのID。
     * @return id をもつポリゴン。
     */
    public MapPolygon getPolygon(MapEditor editor, String id) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return null;
        }
        if (obNode.getNodeType() != OBNode.NType.POLYGON) {
            editor.displayMessage("Error", getName(), "Polygon was lost: ID=" + id);
            invalid = true;
            return null;
        }
        return (MapPolygon)obNode;
    }

    /**
     * シンボリックリンクを取得する
     * @param editor: MapEditor。
     * @param id: シンボリックリンクのID。
     * @return id をもつシンボリックリンク。
     */
    public OBNodeSymbolicLink getSymbolicLink(MapEditor editor, String id) {
        OBNode obNode = getOBNode(editor, id);
        if (obNode == null) {
            return null;
        }
        if (obNode.getNodeType() != OBNode.NType.SYMLINK) {
            editor.displayMessage("Error", getName(), "SymbolicLink was lost: ID=" + id);
            invalid = true;
            return null;
        }
        return (OBNodeSymbolicLink)obNode;
    }

    /**
     * このコマンドの文字列表現を取得する
     * @return コマンドの文字列表現。
     */
    public String toString() {
        return getName();
    }
}
