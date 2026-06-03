package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;

import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 画像ノードの 2D 表示
 */
public class ImageNode2D extends FixedScaleImageNode2D {
    /**
     * マップ上の画像の幅(m)
     */
    protected double widthOnMap;

    /**
     * マップ上の画像の高さ(m)
     */
    protected double heightOnMap;

    /**
     * コンストラクタ
     */
    public ImageNode2D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel2D panel, HashMap parameters, NodeAppearance2D nodeAppearance) throws Exception {
        super.init(panel, parameters, nodeAppearance);

        widthOnMap = getDoubleParameter("width", 0.0);
        heightOnMap = getDoubleParameter("height", 0.0);
        if (widthOnMap <= 0.0 || heightOnMap <= 0.0) {
            throw new Exception("Node appearance error: width or height is not specified correctly with ImageNode2D.");
        }
    }

    /**
     * ノードの半径を取得する
     */
    public double getRadius() {
        return Math.max(widthOnMap, heightOnMap) / 2.0;
    }

    /**
     * ノードを描画する
     */
    public void draw(MapNode node, Graphics2D g2) {
        AffineTransform at = g2.getTransform();
        Point2D point = panel.getRotatedPoint(node);
        g2.translate(point.getX(), point.getY());
        g2.rotate(Math.toRadians(panel.getAngle()));
        g2.scale(widthOnMap / width, heightOnMap / height);
        g2.drawImage(image, -width / 2, -height / 2, width, height, panel);
        g2.setTransform(at);
    }
}
