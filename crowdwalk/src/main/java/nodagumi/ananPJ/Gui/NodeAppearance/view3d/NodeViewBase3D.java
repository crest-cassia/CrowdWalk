package nodagumi.ananPJ.Gui.NodeAppearance.view3d;

import java.util.HashMap;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;

import nodagumi.ananPJ.Gui.FxColor;
import nodagumi.ananPJ.Gui.SimulationPanel3D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;

/**
 * ノード 3D 描画のベースクラス
 */
public abstract class NodeViewBase3D extends JsonicHashMapGetter {
    /**
     * シェイプの高さ(10cm)
     */
    public static double NODE_THICKNESS = 0.1;

    /**
     * シェイプを水平に表示するための回転変換オブジェクト
     */
    public static Rotate NODE_ROTATE = new Rotate(90, Rotate.X_AXIS);

    /**
     * 3D シミュレーションパネル
     */
    protected SimulationPanel3D panel;

    /**
     * ノードの 3D 表示の定義情報
     */
    protected NodeAppearance3D nodeAppearance;

    /**
     * ノードのマテリアル
     */
    protected PhongMaterial material;

    /**
     * 初期設定
     */
    public void init(SimulationPanel3D panel, HashMap parameters, NodeAppearance3D nodeAppearance) throws Exception {
        this.panel = panel;
        this.nodeAppearance = nodeAppearance;
        setParameters(parameters);

        String colorName = getStringParameter("color", "BLACK2");
        Color color = FxColor.getColor(colorName);
        if (color == null) {
            color = FxColor.BLACK2;
        }
        double transparency = getDoubleParameter("transparency", 0.75);
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 1.0 - transparency);

        material = new PhongMaterial();
        material.setDiffuseColor(color);
    }

    /**
     * ノードのシェイプを生成する
     */
    public abstract Shape3D[] createShapes(MapNode node);
}
