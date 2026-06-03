package nodagumi.ananPJ.Editor.EditCommand;

import nodagumi.ananPJ.Editor.MapEditor;

/**
 * ダミーコマンド
 */
public class DummyCommand extends EditCommandBase {
    /**
     * コンストラクタ
     */
    public DummyCommand() {}

    /**
     * 編集を実行する
     */
    public boolean invoke(MapEditor editor) {
        return true;
    }

    /**
     * 編集を取り消す
     */
    public boolean undo(MapEditor editor) {
        return true;
    }
}
