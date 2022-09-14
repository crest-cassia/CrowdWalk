package nodagumi.ananPJ.Gui.AgentAppearance.model;

import java.util.HashMap;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.GuiSimulationLauncher;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;


/**
 * エージェントの状態変化を表示に反映させる
 */
public abstract class AgentAppearanceModel extends JsonicHashMapGetter {
    /**
     * GUI シミュレーションランチャー
     */
    protected GuiSimulationLauncher launcher;

    /**
     * 初期設定 1
     */
    public final void _init(GuiSimulationLauncher launcher, HashMap parameters) {
        this.launcher = launcher;
        setParameters(parameters);
    }

    /**
     * 初期設定 2
     */
    public void init() throws Exception {}

    /**
     * エージェントをセットする
     */
    public abstract AgentAppearanceModel setAgent(AgentBase agent);

    /**
     * appearance value を返す
     */
    public abstract Object getValue();

    /**
     * simulator object を返す。
     */
    public EvacuationSimulator getSimulator() {
	return this.launcher.getSimulator() ;
    }
    
}
