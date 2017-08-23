package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * リンクの幅をセットする編集コマンド
 */
public class SetWidth extends EditCommandBase {
    private String id;
    private double width;
    private double originalWidth;

    /**
     * コンストラクタ
     */
    public SetWidth(MapLink link, double width) {
        changeType = ChangeType.LINK_PARAM;
        id = link.getID();
        this.width = width;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapLink link = getLink(editor, id);
        if (link == null) {
            return false;
        }

        originalWidth = link.getWidth();
        link.setWidth(width);

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

        link.setWidth(originalWidth);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", width=" + width + "]";
    }
}
