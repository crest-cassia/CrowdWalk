package nodagumi.ananPJ.Gui;

import javafx.scene.paint.Color;

public class FxColor {
    public static final Color BLACK = Color.color(0.3, 0.3, 0.3);
    public static final Color BLACK2 = Color.color(0.0, 0.0, 0.0);
    public static final Color GRAY = Color.color(0.5, 0.5, 0.5);
    public static final Color GRAY2 = Color.color(0.4, 0.5, 0.6);
    public static final Color LIGHTGRAY = Color.color(0.7, 0.7, 0.8);
    public static final Color RED = Color.color(1.0, 0.0, 0.0);
    public static final Color GREEN = Color.color(0.0, 1.0, 0.0);
    public static final Color BLUE = Color.color(0.0, 0.0, 1.0);
    public static final Color DARKBLUE = Color.color(0.2, 0.2, 0.8);
    public static final Color LIGHTBLUE = Color.color(0.4, 0.4, 0.9);
    public static final Color SLATEBLUE = Color.color(0.0, 0.5, 1.0);
    public static final Color TURQUOISE = Color.color(0.25, 0.878, 0.8157);
    public static final Color YELLOW = Color.color(1.0, 1.0, 0.0);
    public static final Color WHITE = Color.color(1.0, 1.0, 1.0);
    public static final Color PINK = Color.color(1.0, 0.8, 0.8);
    public static final Color LIGHTB = Color.color(0.8, 0.8, 1.0);
    public static final Color APINK = Color.color(1.0, 0.5, 0.5);
    public static final Color ALIGHTB = Color.color(0.5, 0.5, 1.0);
    public static final Color ARED = Color.color(1.0, 0.7, 0.7);
    public static final Color PURPLE = Color.color(1.0, 0.0, 1.0);
    public static final Color PRED = Color.color(0.7, 0.0, 0.0);
    public static final Color AQUABLUE = Color.rgb(144, 205, 241);
    public static final Color AEGEANBLUE = Color.rgb(138, 207, 255);
    public static final Color LINK_RED = Color.color(1.0, 0.3, 0.3);

    public static final Color BACKGROUND_3D_COLOR = Color.color(0.98, 0.98, 0.98);
    public static final Color DEFAULT_LINK_COLOR = Color.DARKGRAY;
    /* agent color */
    public static final double DEFAULT_AGENT_COLOR_R = 0.3333;
    public static final double DEFAULT_AGENT_COLOR_G = 0.8588;
    public static final double DEFAULT_AGENT_COLOR_B = 0.698;
    public static final Color DEFAULT_AGENT_COLOR = Color.color(
            DEFAULT_AGENT_COLOR_R,
            DEFAULT_AGENT_COLOR_G,
            DEFAULT_AGENT_COLOR_B);

    public static final String[] NAMES = {
        "BLACK", "BLACK2", "GRAY", "GRAY2", "LIGHTGRAY", "RED", "GREEN", "BLUE", "DARKBLUE", "LIGHTBLUE",
        "SLATEBLUE", "TURQUOISE", "YELLOW", "WHITE", "PINK", "LIGHTB", "APINK", "ALIGHTB", "ARED", "PURPLE",
        "PRED", "AQUABLUE", "AEGEANBLUE", "BACKGROUND_3D_COLOR", "DEFAULT_LINK_COLOR", "DEFAULT_AGENT_COLOR"
    };
    public static final Color[] COLORS = {
        BLACK, BLACK2, GRAY, GRAY2, LIGHTGRAY, RED, GREEN, BLUE, DARKBLUE, LIGHTBLUE,
        SLATEBLUE, TURQUOISE, YELLOW, WHITE, PINK, LIGHTB, APINK, ALIGHTB, ARED, PURPLE,
        PRED, AQUABLUE, AEGEANBLUE, BACKGROUND_3D_COLOR, DEFAULT_LINK_COLOR, DEFAULT_AGENT_COLOR
    };

    public static Color speedToColor(double speed) {
	//float f = ((float) speed) * 0.3333f;
	/* [2015.01.29 I.Noda]
	 * 歩行速度の性質から、原則はじめると急速に減速する。
	 * なので、speed = 1 付近(自由速度に近い部分)を拡大する。
	 */
	double hue = Math.min(Math.pow(speed, 5) * 0.35, 1.0) * 360.0;
        Color c_rgb = Color.hsb(hue, 0.8588, 0.698);
        return c_rgb;
    }

    public static Color getColor(String name) {
        for (int index = 0; index < NAMES.length; index++) {
            if (NAMES[index].equals(name)) {
                return COLORS[index];
            }
        }
        return null;
    }
}

