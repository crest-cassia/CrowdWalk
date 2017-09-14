package nodagumi.ananPJ.Gui.AgentAppearance.view2d;

import java.awt.Graphics2D;
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
        g2.fill(new Rectangle2D.Double(x, y, size, size));
    }

    /**
     * ホバー形状を描画する
     */
    protected void drawHoverShape(Graphics2D g2, double x, double y, double size) {
        g2.draw(new Rectangle2D.Double(x, y, size, size));
    }
}

