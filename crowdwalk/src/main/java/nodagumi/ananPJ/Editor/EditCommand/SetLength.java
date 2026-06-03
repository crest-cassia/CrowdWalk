package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * リンクの長さをセットする編集コマンド
 */
public class SetLength extends EditCommandBase {
    private String id;
    private double length;
    private double originalLength;

    /**
     * コンストラクタ
     */
    public SetLength(MapLink link, double length) {
        changeType = ChangeType.LINK_PARAM;
        id = link.getID();
        this.length = length;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapLink link = getLink(editor, id);
        if (link == null) {
            return false;
        }

        originalLength = link.getLength();
        link.setLength(length);

        invoked = true;
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        MapLink link = getLink(editor, id);
        if (link == null) {
            return false;
        }

        link.setLength(originalLength);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", length=" + length + "]";
    }
}
