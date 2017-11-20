package nodagumi.ananPJ.Gui.AgentAppearance.view2d;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import math.geom3d.Vector3D;
import org.apache.batik.ext.awt.geom.Polygon2D;

import nodagumi.ananPJ.Agents.AgentBase;

/**
 * 三角形エージェントの 2D 表示
 */
public class TriangleAgent extends RoundAgent {
    /**
     * コンストラクタ
     */
    public TriangleAgent() {}

    /**
     * エージェントを描画する
     */
    public void draw(AgentBase agent, Graphics2D g2) {
        double scale = g2.getTransform().getScaleX();
        Point2D pos = agent.getPosition();
        Vector3D swing = agent.getSwing();
        double size = frame.getAgentSize() / scale;
        double x = pos.getX() + swing.getX();
        double y = pos.getY() + swing.getY();
        double angle = Math.atan2(agent.getNextNode().getY() - agent.getPrevNode().getY(), agent.getNextNode().getX() - agent.getPrevNode().getX());

        AffineTransform at = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle + Math.PI / 2.0);
        g2.setColor(getAgentColor(agent));
        g2.fill(makeTriangle(size));
        g2.setTransform(at);
    }

    /**
     * エージェントのホバーを描画する
     */
    public void drawHover(AgentBase agent, Graphics2D g2) {
        drawLabel(agent, g2, panel.HOVER_COLOR, panel.HOVER_BG_COLOR);

        double scale = g2.getTransform().getScaleX();
        Point2D pos = agent.getPosition();
        Vector3D swing = agent.getSwing();
        double size = hoverSize / scale;
        double x = pos.getX() + swing.getX();
        double y = pos.getY() + swing.getY();
        double angle = Math.atan2(agent.getNextNode().getY() - agent.getPrevNode().getY(), agent.getNextNode().getX() - agent.getPrevNode().getX());

        AffineTransform at = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle + Math.PI / 2.0);
        g2.setStroke(new BasicStroke((float)(3.0 / scale)));
        g2.setColor(panel.HOVER_COLOR);
        g2.draw(makeTriangle(size));
        g2.setTransform(at);
    }

    /**
     * 三角形ポリゴンを生成する
     */
    protected Polygon2D makeTriangle(double size) {
        double height = Math.sqrt(size * size - Math.pow(size / 2.0, 2.0));
        double x1 = 0.0;
        double y1 = -2.0 * height / 3.0;
        double x2 = -(size / 2.0);
        double y2 = height / 3.0;
        double x3 = size / 2.0;
        double y3 = y2;

        Polygon2D polygon = new Polygon2D();
        polygon.addPoint((float)x1, (float)y1);
        polygon.addPoint((float)x2, (float)y2);
        polygon.addPoint((float)x3, (float)y3);

        return polygon;
    }
}
