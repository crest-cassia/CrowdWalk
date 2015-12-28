package nodagumi.ananPJ.Gui;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;

import net.arnx.jsonic.JSON;

/**
 * リンクの表示スタイル(幅, 色, 透明度)をタグ別に指定するために使用するクラス
 */
public class LinkAppearance {
    public boolean widthFixed = false;
    public double widthRatio = 1.0;
    public Color3f color = Colors.DEFAULT_LINK_COLOR;
    public float transparency = 0.75f;
    public Appearance appearance = new Appearance();

    public LinkAppearance(Boolean _widthFixed, BigDecimal _widthRatio,
            String colorName, BigDecimal _transparency, LinkAppearance defaultValue) {
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
            color = Colors.getColor(colorName);
        }
        if (_transparency != null) {
            transparency = _transparency.floatValue();
        }
        appearance.setTransparencyAttributes(
                new TransparencyAttributes(TransparencyAttributes.FASTEST, transparency));
    }

    public static void loadLinkAppearances(InputStream is,
            LinkedHashMap<String, LinkAppearance> linkAppearances) throws Exception {
	/* [2014.12.27] I.Noda. to adapt new version os JSONIC. */
	//Map<String, Object> map = (Map<String, Object>)JSON.decode(is);
	JSON json = new JSON(JSON.Mode.TRADITIONAL);
	Map<String, Object> map = (Map<String, Object>)json.parse(is);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String tag = entry.getKey();
            Map<String, Object> items = (Map<String, Object>)entry.getValue();
            BigDecimal widthRatio = (BigDecimal)items.get("width_ratio");
            if (widthRatio == null) {
                widthRatio = (BigDecimal)items.get("width");
            }
            linkAppearances.put(tag, new LinkAppearance(
                (Boolean)items.get("width_fixed"),
                widthRatio,
                (String)items.get("color"),
                (BigDecimal)items.get("transparency"),
                linkAppearances.get(tag)
            ));
        }
    }
}
