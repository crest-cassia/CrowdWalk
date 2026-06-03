package nodagumi.ananPJ.Gui.AgentAppearance.view2d;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * 四角いエージェントの 2D 表示
 */
public class SquareAgent extends RoundAgent {
    /**
     * コンストラクタ
     */
    public SquareAgent() {}

    /**
     * エージェント形状を描画する
     */
    protected void drawShape(Graphics2D g2, double x, double y, double size) {
        AffineTransform at = g2.getTransform();
        g2.rotate(Math.toRadians(panel.getAngle()), x, y);
        g2.fill(new Rectangle2D.Double(x - size / 2.0, y - size / 2.0, size, size));
        g2.setTransform(at);
    }

    /**
     * ホバー形状を描画する
     */
    protected void drawHoverShape(Graphics2D g2, double x, double y, double size) {
        AffineTransform at = g2.getTransform();
        g2.rotate(Math.toRadians(panel.getAngle()), x, y);
        g2.draw(new Rectangle2D.Double(x - size / 2.0, y - size / 2.0, size, size));
        g2.setTransform(at);
    }
}

