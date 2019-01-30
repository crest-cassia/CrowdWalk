package nodagumi.ananPJ.Gui.LinkAppearance.view3d;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * 実幅リンクの 3D 表示
 */
public class ActualWidth3D extends LinkViewBase3D {
    /**
     * コンストラクタ
     */
    public ActualWidth3D() {}

    /**
     * 最終的なリンクの幅を取得する
     */
    public double getWidth(MapLink link) {
        return link.getWidth();
    }
}
