package nodagumi.ananPJ.Gui.NodeAppearance.view3d;

import javafx.scene.shape.Shape3D;

import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 見えないノード
 */
public class InvisibleNode3D extends NodeViewBase3D {
    /**
     * コンストラクタ
     */
    public InvisibleNode3D() {}

    /**
     * ノードのシェイプを生成する
     */
    public Shape3D[] createShapes(MapNode node) {
        return null;
    }
}
