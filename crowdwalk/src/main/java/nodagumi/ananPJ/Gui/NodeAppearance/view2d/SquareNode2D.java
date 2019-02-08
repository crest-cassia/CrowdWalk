package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 正方形ノードの 2D 表示
 */
public class SquareNode2D extends NodeViewBase2D {
    /**
     * 辺の長さ
     */
    protected double sideLength = 1.5;

    /**
     * コンストラクタ
     */
    public SquareNode2D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel2D panel, HashMap parameters, NodeAppearance2D nodeAppearance) throws Exception {
        super.init(panel, parameters, nodeAppearance);
        sideLength = getDoubleParameter("sideLength", sideLength);
    }

    /**
     * ノードの半径を取得する
     */
    public double getRadius() {
        return sideLength / 2.0;
    }

    /**
     * ノードを描画する
     */
    public void draw(MapNode node, Graphics2D g2) {
        Point2D point = panel.getRotatedPoint(node);
        double x = point.getX() - sideLength / 2.0;
        double y = point.getY() - sideLength / 2.0;

        g2.setColor(color);
        AffineTransform at = g2.getTransform();
        g2.rotate(Math.toRadians(panel.getAngle()), point.getX(), point.getY());
        g2.fill(new Rectangle2D.Double(x, y, sideLength, sideLength));
        g2.setTransform(at);
    }
}
