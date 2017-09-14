package nodagumi.ananPJ.Gui.AgentAppearance.model;

import java.awt.Color;

import nodagumi.ananPJ.Gui.Color2D;

/**
 * エージェントの状態変化を色で示す
 */
public abstract class ColorModel extends AgentAppearanceModel {
    /**
     * 表示色のRGB値
     */
    protected int rgb = Color2D.DEFAULT_AGENT_COLOR.getRGB();

    /**
     * appearance value を返す
     */
    public Object getValue() {
        return new Color(rgb);
    }

    /**
     * 表示色の青成分(0～255)を返す
     */
    public int getBlue() {
        return rgb & 0xFF;
    }

    /**
     * 表示色の緑成分(0～255)を返す
     */
    public int getGreen() {
        return (rgb >> 8) & 0xFF;
    }

    /**
     * 表示色の赤成分(0～255)を返す
     */
    public int getRed() {
        return (rgb >> 16) & 0xFF;
    }
}
