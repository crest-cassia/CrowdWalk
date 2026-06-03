package nodagumi.ananPJ.Gui.AgentAppearance.view2d;

import java.util.HashMap;

import nodagumi.ananPJ.Gui.AgentAppearance.AgentAppearanceBase;
import nodagumi.ananPJ.Gui.SimulationFrame2D;
import nodagumi.ananPJ.GuiSimulationLauncher2D;

import nodagumi.Itk.Itk;

/**
 * エージェントの 2D 表示の定義情報を扱う
 */
public class AgentAppearance2D extends AgentAppearanceBase {
    /**
     * エージェントを 2D 描画するオブジェクト
     */
    private AgentViewBase2D view;

    /**
     * コンストラクタ
     * @param launcher: GuiSimulationLauncher2D。
     * @param frame: SimulationFrame2D。
     * @param parameters: パラメータテーブル。
     */
    public AgentAppearance2D(GuiSimulationLauncher2D launcher, SimulationFrame2D frame, HashMap parameters) {
        super(launcher, parameters);

        if (isValidFor2D()) {
            try {
                view = (AgentViewBase2D)classFinder.newByName(viewClassName2D);
                view._init(launcher, frame, model, viewParameters2D);
                view.init();
            } catch (Exception e) {
		Itk.quitWithStackTrace(e) ;
            }
        }
    }

    public AgentViewBase2D getView() {
        return view;
    }
}
