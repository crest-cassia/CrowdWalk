package nodagumi.ananPJ.Gui.LinkAppearance.view2d;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * 実幅リンクの 2D 表示
 */
public class ActualWidth2D extends LinkViewBase2D {
    /**
     * コンストラクタ
     */
    public ActualWidth2D() {}

    /**
     * 最終的なリンクの幅を取得する
     */
    public double getWidth(MapLink link) {
        return link.getWidth();
    }
}
