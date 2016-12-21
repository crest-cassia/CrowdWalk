package nodagumi.ananPJ.Gui;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;

/**
 * マップ表示ウィンドウ用インターフェイス
 */
public interface MapViewer {
    public void view(String title, int width, int height, NetworkMap networkMap,
            CrowdWalkPropertiesHandler properties);
}
