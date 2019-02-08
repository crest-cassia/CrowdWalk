package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.awt.Graphics2D;

import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 見えないノード
 */
public class InvisibleNode2D extends NodeViewBase2D {
    /**
     * コンストラクタ
     */
    public InvisibleNode2D() {}

    /**
     * ノードの半径を取得する
     */
    public double getRadius() {
        return 0.0;
    }

    /**
     * ノードを描画する
     */
    public void draw(MapNode node, Graphics2D g2) {}
}
