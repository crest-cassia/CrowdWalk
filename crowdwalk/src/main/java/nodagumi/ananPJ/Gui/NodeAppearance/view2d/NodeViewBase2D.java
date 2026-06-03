package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashMap;

import nodagumi.ananPJ.Gui.Color2D;
import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;

/**
 * ノード 2D 描画のベースクラス
 */
public abstract class NodeViewBase2D extends JsonicHashMapGetter {
    /**
     * 2D シミュレーションパネル
     */
    protected SimulationPanel2D panel;

    /**
     * ノードの 2D 表示の定義情報
     */
    protected NodeAppearance2D nodeAppearance;

    /**
     * ノードの色
     */
    protected Color color = Color2D.BLACK2;

    /**
     * 初期設定
     */
    public void init(SimulationPanel2D panel, HashMap parameters, NodeAppearance2D nodeAppearance) throws Exception {
        this.panel = panel;
        this.nodeAppearance = nodeAppearance;
        setParameters(parameters);

        String colorName = getStringParameter("color", "BLACK2");
        color = Color2D.getColor(colorName);
        if (color == null) {
            color = Color2D.BLACK2;
        }
        float transparency = getFloatParameter("transparency", 0.0f);
        float[] compArray = color.getComponents(null);
        color = new Color(compArray[0], compArray[1], compArray[2], 1.0f - transparency);
    }

    /**
     * ノードの半径を取得する
     */
    public abstract double getRadius();

    /**
     * ノードとラベルを描画する
     */
    public void draw(MapNode node, Graphics2D g2, boolean showLabel) {
        draw(node, g2);
        if (showLabel) {
            drawLabel(node, g2, Color.darkGray, panel.getBackground());
        }
    }

    /**
     * ノードを描画する
     */
    public abstract void draw(MapNode node, Graphics2D g2);

    /**
     * ノードのホバーを描画する
     */
    public void drawHover(MapNode node, Graphics2D g2) {
        double scale = panel.getDrawingScale();
        double diameter = 8.0 / scale;
        Point2D point = panel.getRotatedPoint(node);
        double x = point.getX() - diameter / 2.0;
        double y = point.getY() - diameter / 2.0;

        g2.setColor(panel.HOVER_COLOR);
        g2.setStroke(new BasicStroke((float)(3.0 / scale)));
        g2.draw(new Ellipse2D.Double(x, y, diameter, diameter));
        drawLabel(node, g2, Color.darkGray, panel.HOVER_BG_COLOR);
    }

    /**
     * ラベルを描画する
     */
    public void drawLabel(MapNode node, Graphics2D g2, Color color, Color bgColor) {
        String text = node.getTagString();
        if (! text.isEmpty()) {
            double cx = node.getX();
            double cy = node.getY() - getRadius();
            g2.setColor(color);
            panel.drawText(g2, cx, cy, SimulationPanel2D.TextPosition.UPPER_CENTER, text, bgColor);
        }
    }
}
