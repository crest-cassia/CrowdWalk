package nodagumi.ananPJ.Gui;

import java.awt.Color;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.NetworkMap.Node.*;

/**
 * ノードの表示スタイル(直径, 色, 透明度)をタグ別に指定するために使用するクラス
 */
public class NodeAppearance {
    public static final String NONE_TAG = "__NONE__";

    public double diameter = 1.5;
    public Color3f color = Colors.BLACK2;
    public float transparency = 0.75f;
    public Appearance appearance = new Appearance();
    public Color awtColor = null;

    public NodeAppearance(BigDecimal _diameter, String colorName,
            BigDecimal _transparency, NodeAppearance defaultValue) {
        if (defaultValue != null) {
            diameter = defaultValue.diameter;
            color = defaultValue.color;
            transparency = defaultValue.transparency;
        }
        if (_diameter != null) {
            diameter = _diameter.doubleValue();
        }
        if (colorName != null) {
            color = Colors.getColor(colorName);
        }
        if (_transparency != null) {
            transparency = _transparency.floatValue();
        }
        appearance.setColoringAttributes(new ColoringAttributes(color, ColoringAttributes.FASTEST));
        appearance.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.FASTEST, transparency));
        awtColor = new Color(color.x, color.y, color.z, 1.0f - transparency);
    }

    public static void loadNodeAppearances(InputStream is,
            LinkedHashMap<String, NodeAppearance> nodeAppearances) throws Exception {
	/* [2014.12.27] I.Noda. to adapt new version os JSONIC. */
	//Map<String, Object> map = (Map<String, Object>)JSON.decode(is);
	JSON json = new JSON(JSON.Mode.TRADITIONAL);
	Map<String, Object> map = (Map<String, Object>)json.parse(is);
        NodeAppearance noneTagAppearance = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String tag = entry.getKey();
            Map<String, Object> items = (Map<String, Object>)entry.getValue();
            NodeAppearance appearance = new NodeAppearance(
                (BigDecimal)items.get("diameter"),
                (String)items.get("color"),
                (BigDecimal)items.get("transparency"),
                nodeAppearances.get(tag)
            );
            if (tag.equals(NONE_TAG)) {
                noneTagAppearance = appearance;
            } else {
                nodeAppearances.put(tag, appearance);
            }
        }
        // LinkedHashMap の最後に追加
        if (noneTagAppearance != null) {
            nodeAppearances.put(NONE_TAG, noneTagAppearance);
        }
    }

    public static NodeAppearance getAppearance(
            LinkedHashMap<String, NodeAppearance> nodeAppearances, MapNode node) {
        for (Map.Entry<String, NodeAppearance> entry : nodeAppearances.entrySet()) {
            String tag = entry.getKey();
            if (tag.equals(NONE_TAG) || node.hasTag(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
