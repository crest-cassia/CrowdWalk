package nodagumi.ananPJ.Editor;

import java.util.ArrayList;
import nodagumi.ananPJ.Settings;

/**
 * マップエディタのインターフェイス(OpenJDK に JavaFX が同梱されるまでの一時使用)
 */
public interface MapEditorInterface {
    /**
     * 設定データオブジェクトのセット
     */
    public void setSettings(Settings settings);

    /**
     * マップデータを初期化する
     */
    public void initNetworkMap();

    /**
     * マップデータを読み込む
     */
    public boolean loadNetworkMap();

    /**
     * ウィンドウを表示する
     */
    public void show();

    /**
     * ファイルからプロパティの読み込み。
     */
    public void setPropertiesFromFile(String _propertiesFile, ArrayList<String> _commandLineFallbacks);

    /**
     * プロパティの初期化。
     */
    public void initProperties(ArrayList<String> _commandLineFallbacks);
}
