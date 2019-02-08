package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;

import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 固定スケール画像ノードの 2D 表示
 */
public class FixedScaleImageNode2D extends NodeViewBase2D {
    /**
     * 画像
     */
    protected BufferedImage image;

    /**
     * 画像の幅(Pixel)
     */
    protected int width;

    /**
     * 画像の高さ(Pixel)
     */
    protected int height;

    /**
     * コンストラクタ
     */
    public FixedScaleImageNode2D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel2D panel, HashMap parameters, NodeAppearance2D nodeAppearance) throws Exception {
        super.init(panel, parameters, nodeAppearance);

        String fileName = getStringParameter("fileName", null);
        if (fileName == null) {
            throw new Exception("Node appearance error: Image file name is not specified with " + getClass().getSimpleName() + ".");
        }
        String filePath = panel.getProperties().furnishPropertiesDirPath(fileName, true, false);
        try {
            image = ImageIO.read(new File(filePath));
        } catch (IOException e) {
            throw new Exception("Node appearance error: \"" + filePath + "\" " + e.getMessage() + " with " + getClass().getSimpleName() + ".");
        }
        width = image.getWidth();
        height = image.getHeight();
    }

    /**
     * ノードの半径を取得する
     */
    public double getRadius() {
        double scale = panel.getDrawingScale();
        return Math.max(width, height) / 2.0 / scale;
    }

    /**
     * ノードを描画する
     */
    public void draw(MapNode node, Graphics2D g2) {
        double scale = panel.getDrawingScale();
        Point2D point = panel.getRotatedPoint(node);

        AffineTransform at = g2.getTransform();
        g2.translate(point.getX(), point.getY());
        g2.rotate(Math.toRadians(panel.getAngle()));
        g2.scale(1.0 / scale, 1.0 / scale);
        g2.drawImage(image, -width / 2, -height / 2, width, height, panel);
        g2.setTransform(at);
    }
}
