package nodagumi.ananPJ.Gui.NodeAppearance.view3d;

import java.util.HashMap;

import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;

import nodagumi.ananPJ.Gui.SimulationPanel3D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 丸いノードの 3D 表示
 */
public class RoundNode3D extends NodeViewBase3D {
    /**
     * 直径(m)
     */
    protected double diameter = 1.5;

    /**
     * 高さ(m)
     */
    protected double height = NODE_THICKNESS;

    /**
     * コンストラクタ
     */
    public RoundNode3D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel3D panel, HashMap parameters, NodeAppearance3D nodeAppearance) throws Exception {
        super.init(panel, parameters, nodeAppearance);

        diameter = getDoubleParameter("diameter", diameter);
        height = getDoubleParameter("height", height);
    }

    /**
     * ノードのシェイプを生成する
     */
    public Shape3D[] createShapes(MapNode node) {
        Cylinder shape = new Cylinder(diameter / 2.0, height);
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
