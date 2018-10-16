package nodagumi.ananPJ.Gui.AgentAppearance.view3d;

import java.util.HashMap;

import nodagumi.ananPJ.Gui.AgentAppearance.AgentAppearanceBase;
import nodagumi.ananPJ.Gui.SimulationFrame3D;
import nodagumi.ananPJ.GuiSimulationLauncher3D;

import nodagumi.Itk.Itk;

/**
 * エージェントの 3D 表示の定義情報を扱う
 */
public class AgentAppearance3D extends AgentAppearanceBase {
    /**
     * エージェントを 3D 描画するオブジェクト
     */
    private AgentViewBase3D view;

    /**
     * コンストラクタ
     */
    public AgentAppearance3D(GuiSimulationLauncher3D launcher, SimulationFrame3D frame, HashMap parameters) {
        super(launcher, parameters);

        if (isValidFor3D()) {
            try {
                view = (AgentViewBase3D)classFinder.newByName(viewClassName3D);
                view._init(launcher, frame, model, viewParameters3D);
                view.init();
            } catch (Exception e) {
		Itk.quitWithStackTrace(e) ;
            }
        }
    }

    public AgentViewBase3D getView() {
        return view;
    }
}
