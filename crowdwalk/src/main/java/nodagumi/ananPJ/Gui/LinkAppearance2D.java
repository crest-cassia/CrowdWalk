package nodagumi.ananPJ.Gui;

import java.awt.Color;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.NetworkMap.Link.*;

/**
 * リンクの表示スタイル(幅, 色, 透明度)をタグ別に指定するために使用するクラス
 */
public class LinkAppearance2D {
    public static final String NONE_TAG = "__NONE__";

    public boolean widthFixed = false;
    public double widthRatio = 1.0;
    public Color color = Color2D.DEFAULT_LINK_COLOR;
    public float transparency = 0.75f;

    public LinkAppearance2D(Boolean _widthFixed, BigDecimal _widthRatio,
            String colorName, BigDecimal _transparency, LinkAppearance2D defaultValue) {
        if (defaultValue != null) {
            widthFixed = defaultValue.widthFixed;
            widthRatio = defaultValue.widthRatio;
            color = defaultValue.color;
            transparency = defaultValue.transparency;
        }
        if (_widthFixed != null) {
            widthFixed = _widthFixed;
        }
        if (_widthRatio != null) {
            widthRatio = _widthRatio.doubleValue();
        }
        if (colorName != null) {
            color = Color2D.getColor(colorName);
        }
        if (_transparency != null) {
            transparency = _transparency.floatValue();
        }
        float[] compArray = color.getComponents(null);
        color = new Color(compArray[0], compArray[1], compArray[2], 1.0f - transparency);
    }

    public static void load(InputStream is,
            LinkedHashMap<String, LinkAppearance2D> linkAppearances) throws Exception {
	/* [2014.12.27] I.Noda. to adapt new version os JSONIC. */
	//Map<String, Object> map = (Map<String, Object>)JSON.decode(is);
	JSON json = new JSON(JSON.Mode.TRADITIONAL);
	Map<String, Object> map = (Map<String, Object>)json.parse(is);
        LinkAppearance2D noneTagAppearance = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String tag = entry.getKey();
            Map<String, Object> items = (Map<String, Object>)entry.getValue();
            BigDecimal widthRatio = (BigDecimal)items.get("width_ratio");
            if (widthRatio == null) {
                widthRatio = (BigDecimal)items.get("width");
            }
            LinkAppearance2D appearance = new LinkAppearance2D(
                (Boolean)items.get("width_fixed"),
                widthRatio,
                (String)items.get("color"),
                (BigDecimal)items.get("transparency"),
                linkAppearances.get(tag)
            );
            if (tag.equals(NONE_TAG)) {
                noneTagAppearance = appearance;
            } else {
                linkAppearances.put(tag, appearance);
            }
        }
        // LinkedHashMap の最後に追加
        if (noneTagAppearance != null) {
            linkAppearances.put(NONE_TAG, noneTagAppearance);
        }
    }

    public static LinkAppearance2D getAppearance(
            LinkedHashMap<String, LinkAppearance2D> linkAppearances, MapLink link) {
        for (Map.Entry<String, LinkAppearance2D> entry : linkAppearances.entrySet()) {
            String tag = entry.getKey();
            if (tag.equals(NONE_TAG) || link.hasTag(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
