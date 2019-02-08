package nodagumi.ananPJ.Gui.NodeAppearance.view2d;

import java.util.HashMap;

import nodagumi.ananPJ.Gui.NodeAppearance.NodeAppearanceBase;
import nodagumi.ananPJ.Gui.SimulationPanel2D;

import nodagumi.Itk.Itk;

/**
 * ノード 2D 表示の定義情報を扱う
 */
public class NodeAppearance2D extends NodeAppearanceBase {
    /**
     * ノードを 2D 描画するオブジェクト
     */
    private NodeViewBase2D view;

    /**
     * コンストラクタ
     */
    public NodeAppearance2D(SimulationPanel2D panel, HashMap parameters) {
        super(parameters);

        if (isValidFor2D()) {
            try {
                view = (NodeViewBase2D)classFinder.newByName(viewClassName2D);
                view.init(panel, viewParameters2D, this);
            } catch (Exception e) {
                Itk.quitWithStackTrace(e);
            }
        }
    }

    /**
     * 描画クラスのインスタンスを取得する
     */
    public final NodeViewBase2D getView() {
        return view;
    }
}
