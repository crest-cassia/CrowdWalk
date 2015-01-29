package nodagumi.ananPJ.Gui;

import java.awt.Color;
import java.io.Serializable;
import javax.vecmath.Color3f;


public class Colors implements Serializable {

    private static final long serialVersionUID = 6164276270323927488L;

    static public final Color3f BLACK = new Color3f(0.3f, 0.3f, 0.3f);
    static public final Color3f BLACK2 = new Color3f(0.0f, 0.0f, 0.0f);
    static public final Color3f GRAY = new Color3f(0.5f, 0.5f, 0.5f);
    static public final Color3f GRAY2 = new Color3f(0.4f, 0.5f, 0.6f);
    static public final Color3f LIGHTGRAY = new Color3f(0.7f, 0.7f, 0.8f);
    static public final Color3f RED = new Color3f(1.0f, 0.0f, 0.0f);
    static public final Color3f GREEN = new Color3f(0.0f, 1.0f, 0.0f);
    static public final Color3f BLUE = new Color3f(0.0f, 0.0f, 1.0f);
    static public final Color3f DARKBLUE = new Color3f(0.2f, 0.2f, 0.8f);
    static public final Color3f LIGHTBLUE = new Color3f(0.4f, 0.4f, 0.9f);
    static public final Color3f SLATEBLUE = new Color3f(0.0f, 0.5f, 1.0f);
    static public final Color3f TURQUOISE = new Color3f(0.25f, 0.878f, 0.8157f);
    static public final Color3f YELLOW = new Color3f(1.0f, 1.0f, 0.0f);
    static public final Color3f WHITE = new Color3f(1.0f, 1.0f, 1.0f);
    static public final Color3f PINK = new Color3f(1.0f, 0.8f, 0.8f);
    static public final Color3f LIGHTB = new Color3f(0.8f, 0.8f, 1.0f);
    static public final Color3f APINK = new Color3f(1.0f, 0.5f, 0.5f);
    static public final Color3f ALIGHTB = new Color3f(0.5f, 0.5f, 1.0f);
    static public final Color3f ARED = new Color3f(1.0f, 0.7f, 0.7f);
    static public final Color3f PURPLE = new Color3f(1.0f, 0.0f, 1.0f);
    static public final Color3f PRED = new Color3f(0.7f, 0.0f, 0.0f);

    //static public final Color3f BACKGROUND_3D_COLOR = new Color3f(0.95f, 0.95f, 0.95f);
    static public final Color3f BACKGROUND_3D_COLOR = new Color3f(0.98f, 0.98f, 0.98f);
    static public final Color3f DEFAULT_LINK_COLOR = new Color3f(0.1f, 0.1f, 0.1f);
    /* agent color */
    static public final float DEFAULT_AGENT_COLOR_R = 0.3333f;
    static public final float DEFAULT_AGENT_COLOR_G = 0.8588f;
    static public final float DEFAULT_AGENT_COLOR_B = 0.698f;
    //static public final float DEFAULT_AGENT_COLOR_R = 0.1f;
    //static public final float DEFAULT_AGENT_COLOR_G = 0.7f;
    //static public final float DEFAULT_AGENT_COLOR_B = 0.1f;
    static public final Color3f DEFAULT_AGENT_COLOR = new Color3f(
            DEFAULT_AGENT_COLOR_R,
            DEFAULT_AGENT_COLOR_G,
            DEFAULT_AGENT_COLOR_B);

    public static final String[] NAMES = {
        "BLACK", "BLACK2", "GRAY", "GRAY2", "LIGHTGRAY", "RED", "GREEN", "BLUE", "DARKBLUE", "LIGHTBLUE",
        "SLATEBLUE", "TURQUOISE", "YELLOW", "WHITE", "PINK", "LIGHTB", "APINK", "ALIGHTB", "ARED", "PURPLE",
        "PRED", "BACKGROUND_3D_COLOR", "DEFAULT_LINK_COLOR", "DEFAULT_AGENT_COLOR"
    };
    public static final Color3f[] COLORS = {
        BLACK, BLACK2, GRAY, GRAY2, LIGHTGRAY, RED, GREEN, BLUE, DARKBLUE, LIGHTBLUE,
        SLATEBLUE, TURQUOISE, YELLOW, WHITE, PINK, LIGHTB, APINK, ALIGHTB, ARED, PURPLE,
        PRED, BACKGROUND_3D_COLOR, DEFAULT_LINK_COLOR, DEFAULT_AGENT_COLOR
    };

    public static Color3f speedToColor3f(double speed) {
	//float f = ((float) speed) * 0.3333f;
	/* [2015.01.29 I.Noda]
	 * 歩行速度の性質から、原則はじめると急速に減速する。
	 * なので、speed = 1 付近(自由速度に近い部分)を拡大する。
	 */
	float f = ((float) Math.pow(speed,3)) * 0.4f;
        Color c_rgb = new Color(Color.HSBtoRGB(f, 0.8588f, 0.698f));
        // System.err.println("  speed: " + speed + ", R: " + c_rgb.getRed() +
                // ", G: " + c_rgb.getGreen() + ", B: " + c_rgb.getBlue());
        return new Color3f(c_rgb);
    }

    public static Color3f speedToColor3f(float speed) {
        float f = speed * DEFAULT_AGENT_COLOR_R;
        Color c_rgb = new Color(Color.HSBtoRGB(f, DEFAULT_AGENT_COLOR_G,
                    DEFAULT_AGENT_COLOR_B));
        return new Color3f(c_rgb);
    }

    public static Color3f getColor(String name) {
        for (int index = 0; index < NAMES.length; index++) {
            if (NAMES[index].equals(name)) {
                return COLORS[index];
            }
        }
        return null;
    }
}
