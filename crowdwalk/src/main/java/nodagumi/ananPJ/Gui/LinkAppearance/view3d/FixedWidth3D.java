package nodagumi.ananPJ.Gui.LinkAppearance.view3d;

import java.util.HashMap;

import nodagumi.ananPJ.Gui.SimulationPanel3D;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * 固定幅リンクの 3D 表示
 */
public class FixedWidth3D extends LinkViewBase3D {
    /**
     * リンクの幅
     */
    protected double width = 1.0;

    /**
     * コンストラクタ
     */
    public FixedWidth3D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel3D panel, HashMap parameters, LinkAppearance3D linkAppearance) throws Exception {
        super.init(panel, parameters, linkAppearance);

        if (method != Method.FILLING) {
            throw new Exception("Link appearance error: only \"filling\" method can be specified with FixedWidth3D.");
        }
        width = getDoubleParameter("width", width);
    }

    /**
     * 最終的なリンクの幅を取得する
     */
    public double getWidth(MapLink link) {
        return width;
    }
}
