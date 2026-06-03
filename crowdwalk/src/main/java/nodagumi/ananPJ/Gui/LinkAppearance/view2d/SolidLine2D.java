package nodagumi.ananPJ.Gui.LinkAppearance.view2d;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * 実線リンクの 2D 表示
 */
public class SolidLine2D extends LinkViewBase2D {
    /**
     * コンストラクタ
     */
    public SolidLine2D() {}

    /**
     * 最終的なリンクの幅を取得する
     */
    public double getWidth(MapLink link) {
        double scale = panel.getDrawingScale();
        return 1.0 / scale;
    }
}
