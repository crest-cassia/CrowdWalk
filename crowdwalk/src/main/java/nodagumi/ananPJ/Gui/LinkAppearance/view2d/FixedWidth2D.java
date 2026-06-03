package nodagumi.ananPJ.Gui.LinkAppearance.view2d;

import java.util.HashMap;

import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * 固定幅リンクの 2D 表示
 */
public class FixedWidth2D extends LinkViewBase2D {
    /**
     * リンクの幅
     */
    protected double width = 1.0;

    /**
     * コンストラクタ
     */
    public FixedWidth2D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel2D panel, HashMap parameters, LinkAppearance2D linkAppearance) throws Exception {
        super.init(panel, parameters, linkAppearance);
        width = getDoubleParameter("width", width);
    }

    /**
     * 最終的なリンクの幅を取得する
     */
    public double getWidth(MapLink link) {
        return width;
    }
}
