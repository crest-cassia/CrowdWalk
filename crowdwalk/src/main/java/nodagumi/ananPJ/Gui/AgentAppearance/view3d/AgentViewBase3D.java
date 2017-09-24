package nodagumi.ananPJ.Gui.AgentAppearance.view3d;

import java.util.HashMap;

import javafx.scene.shape.Shape3D;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.AgentAppearance.model.AgentAppearanceModel;
import nodagumi.ananPJ.Gui.SimulationFrame3D;
import nodagumi.ananPJ.Gui.SimulationPanel3D;
import nodagumi.ananPJ.GuiSimulationLauncher;
import nodagumi.ananPJ.GuiSimulationLauncher3D;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;

/**
 * エージェント 3D 描画のベースクラス
 */
public abstract class AgentViewBase3D extends JsonicHashMapGetter {
    /**
     * 3D シミュレーションランチャー
     */
    protected GuiSimulationLauncher3D launcher;

    /**
     * 3D シミュレーションウィンドウ
     */
    protected SimulationFrame3D frame;

    /**
     * 3D シミュレーションパネル
     */
    protected SimulationPanel3D panel;

    /**
     * エージェントの状態変化を表示に反映させるオブジェクト
     */
    protected AgentAppearanceModel model;

    /**
     * 初期設定 1
     */
    public final void _init(GuiSimulationLauncher launcher, SimulationFrame3D frame, AgentAppearanceModel model, HashMap parameters) {
        this.launcher = (GuiSimulationLauncher3D)launcher;
        this.frame = frame;
        panel = frame.getSimulationPanel();
        this.model = model;
        setParameters(parameters);
    }

    /**
     * 初期設定 2
     */
    public void init() throws Exception {}

    /**
     * エージェントのシェープを生成する
     */
    public abstract Shape3D createShape(AgentBase agent, double size);

    /**
     * エージェントが移動した
     */
    public void agentMoved(AgentBase agent, Shape3D shape) {}

    /**
     * エージェントの表示を更新する
     */
    public abstract void updateAgent(AgentBase agent, Shape3D shape);

    /**
     * エージェントのサイズを変更する
     */
    public abstract void changeAgentSize(AgentBase agent, Shape3D shape, double size);

    /**
     * エージェントをホバー表示に切り替える
     */
    public abstract void switchToHover(AgentBase agent, Shape3D shape);

    /**
     * エージェントを通常表示に切り替える
     */
    public abstract void switchToNormal(AgentBase agent, Shape3D shape);
}
