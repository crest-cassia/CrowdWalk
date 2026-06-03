package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * 通行制限をセットする編集コマンド
 */
public class SetTrafficRestriction extends EditCommandBase {
    private String id;
    private boolean oneWayForward;
    private boolean oneWayBackward;
    private boolean roadClosed;
    private boolean originalOneWayForward;
    private boolean originalOneWayBackward;
    private boolean originalRoadClosed;

    /**
     * コンストラクタ
     */
    public SetTrafficRestriction(MapLink link, boolean oneWayForward, boolean oneWayBackward, boolean roadClosed) {
        changeType = ChangeType.LINK_PARAM;
        id = link.getID();
        this.oneWayForward = oneWayForward;
        this.oneWayBackward = oneWayBackward;
        this.roadClosed = roadClosed;
    }

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        MapLink link = getLink(editor, id);
        if (link == null) {
            return false;
        }

        originalOneWayForward = link.isOneWayForward();
        originalOneWayBackward = link.isOneWayBackward();
        originalRoadClosed = link.isRoadClosed();
        link.setOneWayForward(oneWayForward);
        link.setOneWayBackward(oneWayBackward);
        link.setRoadClosed(roadClosed);

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

        link.setOneWayForward(originalOneWayForward);
        link.setOneWayBackward(originalOneWayBackward);
        link.setRoadClosed(originalRoadClosed);

        invoked = false;
        return true;
    }

    /**
     * このコマンドの文字列表現を取得する
     */
    public String toString() {
        return getName() + "[id=" + id + ", oneWayForward=" + oneWayForward + ", oneWayBackward=" + oneWayBackward + ", roadClosed=" + roadClosed + "]";
    }
}
