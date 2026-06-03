package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashMap;

import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 丸いノードの 2D 表示
 */
public class RoundNode2D extends NodeViewBase2D {
    /**
     * 直径
     */
    protected double diameter = 1.5;

    /**
     * コンストラクタ
     */
    public RoundNode2D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel2D panel, HashMap parameters, NodeAppearance2D nodeAppearance) throws Exception {
        super.init(panel, parameters, nodeAppearance);
        diameter = getDoubleParameter("diameter", diameter);
    }

    /**
     * ノードの半径を取得する
     */
    public double getRadius() {
        return diameter / 2.0;
    }

    /**
     * ノードを描画する
     */
    public void draw(MapNode node, Graphics2D g2) {
        g2.setColor(color);
        Point2D point = panel.getRotatedPoint(node);
        double x = point.getX() - diameter / 2.0;
        double y = point.getY() - diameter / 2.0;
        g2.fill(new Ellipse2D.Double(x, y, diameter, diameter));
    }
}
