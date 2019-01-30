package nodagumi.ananPJ.Gui.LinkAppearance.view2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;

import nodagumi.ananPJ.Gui.Color2D;
import nodagumi.ananPJ.Gui.LinkAppearance.EdgePoints;
import nodagumi.ananPJ.Gui.SimulationFrame2D;
import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;

/**
 * リンク 2D 描画のベースクラス
 */
public abstract class LinkViewBase2D extends JsonicHashMapGetter {
    /**
     * リンクの描画方法
     */
    public enum Method {
        FILLING,
        BORDERING
    };

    /**
     * 2D シミュレーションパネル
     */
    protected SimulationPanel2D panel;

    /**
     * リンクの 2D 表示の定義情報
     */
    protected LinkAppearance2D linkAppearance;

    /**
     * リンクの色
     */
    protected Color color = Color2D.DEFAULT_LINK_COLOR;

    /**
     * リンクの描画方法
     */
    protected Method method = Method.FILLING;

    /**
     * 初期設定
     */
    public void init(SimulationPanel2D panel, HashMap parameters, LinkAppearance2D linkAppearance) throws Exception {
        this.panel = panel;
        this.linkAppearance = linkAppearance;
        setParameters(parameters);

        String colorName = getStringParameter("color", "DEFAULT_LINK_COLOR");
        color = Color2D.getColor(colorName);
        if (color == null) {
            color = Color2D.DEFAULT_LINK_COLOR;
        }
        float transparency = getFloatParameter("transparency", 0.0f);
        float[] compArray = color.getComponents(null);
        color = new Color(compArray[0], compArray[1], compArray[2], 1.0f - transparency);

        String methodName = getStringParameter("method", method.name());
        method = Method.valueOf(methodName.toUpperCase());
    }

    /**
     * 最終的なリンクの幅を取得する
     */
    public abstract double getWidth(MapLink link);

    /**
     * リンクの Line2D を生成する
     */
    public Line2D[] createLinkLines(MapLink link) {
        Line2D[] lines = null;
        switch (method) {
        case FILLING:
            lines = new Line2D[1];
            lines[0] = new Line2D.Double(panel.getRotatedPoint(link.getFrom()), panel.getRotatedPoint(link.getTo()));
            break;
        case BORDERING:
            EdgePoints.Points points = linkAppearance.getEdgePoints(link);
            if (points == null) {
                break;
            }
            javafx.geometry.Point2D a1 = points.fromNodeLeftPoint;
            javafx.geometry.Point2D a2 = points.toNodeRightPoint;
            javafx.geometry.Point2D b1 = points.fromNodeRightPoint;
            javafx.geometry.Point2D b2 = points.toNodeLeftPoint;
            if (a1 == null || a2 == null || b1 == null || b2 == null) {
                break;
            }
            lines = new Line2D[2];
            lines[0] = new Line2D.Double(panel.calcRotatedPoint(a1.getX(), a1.getY()), panel.calcRotatedPoint(a2.getX(), a2.getY()));
            lines[1] = new Line2D.Double(panel.calcRotatedPoint(b1.getX(), b1.getY()), panel.calcRotatedPoint(b2.getX(), b2.getY()));
            break;
        }
        return lines;
    }

    /**
     * リンクとラベルを描画する
     */
    public void draw(MapLink link, Graphics2D g2, boolean showLabel) {
        draw(link, g2);
        if (showLabel) {
            drawLabel(link, g2, Color.darkGray, panel.getBackground());
        }
    }

    /**
     * リンクを描画する
     */
    public void draw(MapLink link, Graphics2D g2) {
        g2.setColor(color);
        Line2D[] lines = null;
        switch (method) {
        case FILLING:
            lines = linkAppearance.getLinkLines(link);
            if (lines == null) {
                break;
            }
            g2.setStroke(new BasicStroke((float)getWidth(link), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.draw(lines[0]);
            break;
        case BORDERING:
            lines = linkAppearance.getLinkLines(link);
            if (lines == null) {
                break;
            }
            double scale = panel.getDrawingScale();
            g2.setStroke(new BasicStroke((float)(1.0 / scale), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.draw(lines[0]);
            g2.draw(lines[1]);
            break;
        }
    }

    /**
     * リンクのホバーを描画する
     */
    public void drawHover(MapLink link, Graphics2D g2) {
        double scale = panel.getDrawingScale();

        g2.setColor(panel.HOVER_COLOR);
        g2.fill(getLinkRect(link, scale));
        drawLabel(link, g2, Color.darkGray, panel.HOVER_BG_COLOR);
    }

    /**
     * ラベルを描画する
     */
    public void drawLabel(MapLink link, Graphics2D g2, Color color, Color bgColor) {
        String text = link.getTagString();
        if (! text.isEmpty()) {
            Point2D middlePoint = link.getMiddlePoint();
            double scale = panel.getDrawingScale();
            double cx = middlePoint.getX();
            double cy = middlePoint.getY() - getWidth(link) / scale / 2.0;
            g2.setColor(color);
            panel.drawText(g2, cx, cy, SimulationPanel2D.TextPosition.UPPER_CENTER, text, bgColor);
        }
    }

    /**
     * ホバー描画用の Path2D を生成する
     */
    public GeneralPath getLinkRect(MapLink link, double scale) {
        MapNode fromNode = link.getFrom();
        MapNode toNode = link.getTo();
        double x1 = panel.getRotatedX(fromNode);
        double y1 = panel.getRotatedY(fromNode);
        double x2 = panel.getRotatedX(toNode);
        double y2 = panel.getRotatedY(toNode);
        double fwidth = Math.max(getWidth(link), 4.0) / scale;

        double dx = (x2 - x1);
        double dy = (y2 - y1);
        double a = Math.sqrt(dx*dx + dy*dy);

        double edx = fwidth * dx / a / 2;
        double edy = fwidth * dy / a / 2;

        GeneralPath p = new GeneralPath();
        p.moveTo(x1 - edy, y1 + edx);
        p.lineTo(x1 + edy, y1 - edx);
        p.lineTo(x2 + edy, y2 - edx);
        p.lineTo(x2 - edy, y2 + edx);
        p.lineTo(x1 - edy, y1 + edx);
        p.closePath();

        return p;
    }
}
