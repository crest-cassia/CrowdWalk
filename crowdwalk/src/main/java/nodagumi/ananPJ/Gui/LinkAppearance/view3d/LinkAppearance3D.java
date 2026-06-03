package nodagumi.ananPJ.Gui.LinkAppearance.view3d;

import java.util.HashMap;

import nodagumi.ananPJ.Gui.LinkAppearance.EdgePoints;
import nodagumi.ananPJ.Gui.LinkAppearance.LinkAppearanceBase;
import nodagumi.ananPJ.Gui.SimulationPanel3D;

import nodagumi.Itk.Itk;

/**
 * リンク 3D 表示の定義情報を扱う
 */
public class LinkAppearance3D extends LinkAppearanceBase {
    /**
     * リンクを 3D 描画するオブジェクト
     */
    private LinkViewBase3D view;

    /**
     * コンストラクタ
     */
    public LinkAppearance3D(SimulationPanel3D panel, HashMap parameters, EdgePoints edgePoints) {
        super(parameters, edgePoints);

        if (isValidFor3D()) {
            try {
                view = (LinkViewBase3D)classFinder.newByName(viewClassName3D);
                view.init(panel, viewParameters3D, this);
            } catch (Exception e) {
                Itk.quitWithStackTrace(e);
            }
        }
    }

    /**
     * 描画クラスのインスタンスを取得する
     */
    public LinkViewBase3D getView() {
        return view;
    }
}
