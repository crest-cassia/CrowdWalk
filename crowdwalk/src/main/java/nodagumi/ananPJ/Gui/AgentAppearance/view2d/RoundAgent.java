package nodagumi.ananPJ.Gui.AgentAppearance.view2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import math.geom3d.Vector3D;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.Color2D;

/**
 * 丸いエージェントの 2D 表示
 */
public class RoundAgent extends AgentViewBase2D {
    /**
     * ホバーサイズ
     */
    protected double hoverSize = 8.0;

    /**
     * コンストラクタ
     */
    public RoundAgent() {}

    /**
     * エージェントを描画する
     */
    public void draw(AgentBase agent, Graphics2D g2) {
        double scale = panel.getDrawingScale();
        Vector3D swing = agent.getSwing();
        Point2D pos = panel.calcRotatedPoint(panel.add(agent.getPosition(), swing.getX(), swing.getY()));
        double size = frame.getAgentSize() / scale;

        g2.setColor(getAgentColor(agent));
        drawShape(g2, pos.getX(), pos.getY(), size);
    }

    /**
     * エージェント形状を描画する
     */
    protected void drawShape(Graphics2D g2, double x, double y, double size) {
        g2.fill(new Ellipse2D.Double(x - size / 2.0, y - size / 2.0, size, size));
    }

    /**
     * エージェントのホバーを描画する
     */
    public void drawHover(AgentBase agent, Graphics2D g2) {
        drawLabel(agent, g2, panel.HOVER_COLOR, panel.HOVER_BG_COLOR);

        double scale = panel.getDrawingScale();
        Vector3D swing = agent.getSwing();
        Point2D pos = panel.calcRotatedPoint(panel.add(agent.getPosition(), swing.getX(), swing.getY()));
        double size = hoverSize / scale;

        g2.setStroke(new BasicStroke((float)(3.0 / scale)));
        g2.setColor(panel.HOVER_COLOR);
        drawHoverShape(g2, pos.getX(), pos.getY(), size);
    }

    /**
     * ホバー形状を描画する
     */
    protected void drawHoverShape(Graphics2D g2, double x, double y, double size) {
        g2.draw(new Ellipse2D.Double(x - size / 2.0, y - size / 2.0, size, size));
    }

    /**
     * エージェントの表示色を返す
     */
    protected Color getAgentColor(AgentBase agent) {
        switch (agent.getTriage()) {
        case GREEN:
            if (frame.getChangeAgentColorDependingOnSpeed()) {
                return (Color)model.setAgent(agent).getValue();
            }
            break;
        case YELLOW:
            return Color2D.YELLOW;
        case RED:
            return Color2D.PRED;
        case BLACK:
            return Color2D.BLACK2;
        }
        return Color2D.DEFAULT_AGENT_COLOR;
    }
}

