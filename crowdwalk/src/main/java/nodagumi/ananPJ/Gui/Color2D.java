package nodagumi.ananPJ.Gui;

import java.awt.Color;

import nodagumi.Itk.*;

public class Color2D {
    static public final Color BLACK = new Color(0.3f, 0.3f, 0.3f);
    static public final Color BLACK2 = new Color(0.0f, 0.0f, 0.0f);
    static public final Color GRAY = new Color(0.5f, 0.5f, 0.5f);
    static public final Color GRAY2 = new Color(0.4f, 0.5f, 0.6f);
    static public final Color LIGHTGRAY = new Color(0.7f, 0.7f, 0.8f);
    static public final Color RED = new Color(1.0f, 0.0f, 0.0f);
    static public final Color GREEN = new Color(0.0f, 1.0f, 0.0f);
    static public final Color BLUE = new Color(0.0f, 0.0f, 1.0f);
    static public final Color DARKBLUE = new Color(0.2f, 0.2f, 0.8f);
    static public final Color LIGHTBLUE = new Color(0.4f, 0.4f, 0.9f);
    static public final Color SLATEBLUE = new Color(0.0f, 0.5f, 1.0f);
    static public final Color TURQUOISE = new Color(0.25f, 0.878f, 0.8157f);
    static public final Color YELLOW = new Color(1.0f, 1.0f, 0.0f);
    static public final Color WHITE = new Color(1.0f, 1.0f, 1.0f);
    static public final Color PINK = new Color(1.0f, 0.8f, 0.8f);
    static public final Color LIGHTB = new Color(0.8f, 0.8f, 1.0f);
    static public final Color APINK = new Color(1.0f, 0.5f, 0.5f);
    static public final Color ALIGHTB = new Color(0.5f, 0.5f, 1.0f);
    static public final Color ARED = new Color(1.0f, 0.7f, 0.7f);
    static public final Color PURPLE = new Color(1.0f, 0.0f, 1.0f);
    static public final Color PRED = new Color(0.7f, 0.0f, 0.0f);
    static public final Color AQUABLUE = new Color(144, 205, 241);
    static public final Color AEGEANBLUE = new Color(138, 207, 255);

    static public final Color BACKGROUND_3D_COLOR = new Color(0.98f, 0.98f, 0.98f);
    static public final Color DEFAULT_LINK_COLOR = new Color(0.1f, 0.1f, 0.1f);
    /* agent color */
    static public final float DEFAULT_AGENT_COLOR_R = 0.3333f;
    static public final float DEFAULT_AGENT_COLOR_G = 0.8588f;
    static public final float DEFAULT_AGENT_COLOR_B = 0.698f;
    //static public final float DEFAULT_AGENT_COLOR_R = 0.1f;
    //static public final float DEFAULT_AGENT_COLOR_G = 0.7f;
    //static public final float DEFAULT_AGENT_COLOR_B = 0.1f;
    static public final Color DEFAULT_AGENT_COLOR = new Color(
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

    public static Color getColor(String name) {
        for (int index = 0; index < NAMES.length; index++) {
            if (NAMES[index].equals(name)) {
                return COLORS[index];
            }
        }
        return null;
    }
}
