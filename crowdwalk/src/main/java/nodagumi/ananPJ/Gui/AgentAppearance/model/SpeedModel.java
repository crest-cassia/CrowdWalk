package nodagumi.ananPJ.Gui.AgentAppearance.model;

import java.awt.Color;

import nodagumi.ananPJ.Agents.AgentBase;

/**
 * エージェントの歩行速度に応じて色を変える
 */
public class SpeedModel extends ColorModel {
    /**
     * 計算パラメータとデフォルト値
     */
    private double coefficientOfHue = 0.35;
    private double exponent = 5.0;
    private double saturation = 0.8588;
    private double brightness = 0.698;

    /**
     * コンストラクタ
     */
    public SpeedModel() {}

    /**
     * 初期設定
     */
    public void init() throws Exception {
        coefficientOfHue = getDoubleParameter("coefficientOfHue", coefficientOfHue);
        exponent = getDoubleParameter("exponent", exponent);
        saturation = getDoubleParameter("saturation", saturation);
        brightness = getDoubleParameter("brightness", brightness);
    }

    /**
     * エージェントをセットする
     */
    public SpeedModel setAgent(AgentBase agent) {
        double hue = Math.pow(agent.getSpeed(), exponent) * coefficientOfHue;
        rgb = Color.HSBtoRGB((float)hue, (float)saturation, (float)brightness);
        return this;
    }
}
