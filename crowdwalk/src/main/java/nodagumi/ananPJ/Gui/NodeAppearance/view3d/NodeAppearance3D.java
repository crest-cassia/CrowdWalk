package nodagumi.ananPJ.Gui.NodeAppearance.view3d;

import java.util.HashMap;

import nodagumi.ananPJ.Gui.NodeAppearance.NodeAppearanceBase;
import nodagumi.ananPJ.Gui.SimulationPanel3D;

import nodagumi.Itk.Itk;

/**
 * ノード 3D 表示の定義情報を扱う
 */
public class NodeAppearance3D extends NodeAppearanceBase {
    /**
     * ノードを 3D 描画するオブジェクト
     */
    private NodeViewBase3D view;

    /**
     * コンストラクタ
     */
    public NodeAppearance3D(SimulationPanel3D panel, HashMap parameters) {
        super(parameters);

        if (isValidFor3D()) {
            try {
                view = (NodeViewBase3D)classFinder.newByName(viewClassName3D);
                view.init(panel, viewParameters3D, this);
            } catch (Exception e) {
                Itk.quitWithStackTrace(e);
            }
        }
    }

    /**
     * 描画クラスのインスタンスを取得する
     */
    public NodeViewBase3D getView() {
        return view;
    }
}
