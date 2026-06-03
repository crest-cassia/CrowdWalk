package nodagumi.ananPJ.Gui.AgentAppearance.view2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.HashMap;

import math.geom3d.Vector3D;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.AgentAppearance.model.AgentAppearanceModel;
import nodagumi.ananPJ.Gui.SimulationFrame2D;
import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.GuiSimulationLauncher2D;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;

/**
 * エージェント 2D 描画のベースクラス
 */
public abstract class AgentViewBase2D extends JsonicHashMapGetter {
    /**
     * 2D シミュレーションランチャー
     */
    protected GuiSimulationLauncher2D launcher;

    /**
     * 2D シミュレーションウィンドウ
     */
    protected SimulationFrame2D frame;

    /**
     * 2D シミュレーションパネル
     */
    protected SimulationPanel2D panel;

    /**
     * エージェントの状態変化を表示に反映させるオブジェクト
     */
    protected AgentAppearanceModel model;

    /**
     * 初期設定 1
     */
    public final void _init(GuiSimulationLauncher2D launcher, SimulationFrame2D frame, AgentAppearanceModel model, HashMap parameters) {
        this.launcher = launcher;
        this.frame = frame;
        panel = frame.panel;
        this.model = model;
        setParameters(parameters);
    }

    /**
     * 初期設定 2
     */
    public void init() throws Exception {}

    /**
     * エージェントとラベルを描画する
     */
    public void draw(AgentBase agent, Graphics2D g2, boolean showLabel) {
        if (showLabel) {
            drawLabel(agent, g2, Color.RED, panel.getBackground());
        }
        draw(agent, g2);
    }

    /**
     * エージェントを描画する
     */
    public abstract void draw(AgentBase agent, Graphics2D g2);

    /**
     * エージェントのホバーを描画する
     */
    public abstract void drawHover(AgentBase agent, Graphics2D g2);

    /**
     * ラベルを描画する
     */
    public void drawLabel(AgentBase agent, Graphics2D g2, Color color, Color bgColor) {
        double scale = panel.getDrawingScale();
        Point2D pos = agent.getPosition();
        Vector3D swing = agent.getSwing();
        String text = agent.getIdNumber();
        double size = frame.getAgentSize() / scale;
        double cx = pos.getX() + swing.getX();
        double cy = pos.getY() + swing.getY();

        g2.setColor(color);
        panel.drawText(g2, cx, cy, SimulationPanel2D.TextPosition.LOWER_CENTER, text, bgColor, 0.0, size / 2.0);
    }
}
