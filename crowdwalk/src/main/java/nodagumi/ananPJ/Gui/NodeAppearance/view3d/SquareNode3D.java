package nodagumi.ananPJ.Gui.NodeAppearance.view3d;

import java.util.HashMap;

import javafx.scene.shape.Box;
import javafx.scene.shape.Shape3D;

import nodagumi.ananPJ.Gui.SimulationPanel3D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 正方形ノードの 3D 表示
 */
public class SquareNode3D extends NodeViewBase3D {
    /**
     * 辺の長さ(m)
     */
    protected double sideLength = 1.5;

    /**
     * 高さ(m)
     */
    protected double height = NODE_THICKNESS;

    /**
     * コンストラクタ
     */
    public SquareNode3D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel3D panel, HashMap parameters, NodeAppearance3D nodeAppearance) throws Exception {
        super.init(panel, parameters, nodeAppearance);

        sideLength = getDoubleParameter("sideLength", sideLength);
        height = getDoubleParameter("height", height);
    }

    /**
     * ノードのシェイプを生成する
     */
    public Shape3D[] createShapes(MapNode node) {
        Box shape = new Box(sideLength, sideLength, height);
        shape.setMaterial(material);
        java.awt.geom.Point2D pos = node.getPosition();
        shape.setTranslateX(pos.getX());
        shape.setTranslateY(pos.getY());
        shape.setTranslateZ(-node.getHeight() * panel.getVerticalScale() - (height / 2.0));
        shape.getTransforms().add(NODE_ROTATE);

        Shape3D[] shapes = { shape };
        return shapes;
    }
}
