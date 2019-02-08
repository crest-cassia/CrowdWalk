package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashMap;

import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 固定スケールの丸いノードの 2D 表示
 */
public class FixedScaleRoundNode2D extends RoundNode2D {
    /**
     * コンストラクタ
     */
    public FixedScaleRoundNode2D() {}

    /**
     * ノードの半径を取得する
     */
    public double getRadius() {
        double scale = panel.getDrawingScale();
        return diameter / 2.0 / scale;
    }

    /**
     * ノードを描画する
     */
    public void draw(MapNode node, Graphics2D g2) {
        double radius = getRadius();
        g2.setColor(color);
        Point2D point = panel.getRotatedPoint(node);
        double x = point.getX() - radius;
        double y = point.getY() - radius;
        g2.fill(new Ellipse2D.Double(x, y, radius * 2.0, radius * 2.0));
    }
}
