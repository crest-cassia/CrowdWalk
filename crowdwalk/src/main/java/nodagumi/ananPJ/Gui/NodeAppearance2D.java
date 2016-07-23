package nodagumi.ananPJ.Gui;

import java.awt.Color;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.NetworkMap.Node.*;

/**
 * ノードの表示スタイル(直径, 色, 透明度)をタグ別に指定するために使用するクラス
 */
public class NodeAppearance2D {
    public static final String NONE_TAG = "__NONE__";

    public double diameter = 1.5;
    public Color color = Color2D.BLACK2;
    public float transparency = 0.75f;

    public NodeAppearance2D(BigDecimal _diameter, String colorName,
            BigDecimal _transparency, NodeAppearance2D defaultValue) {
        if (defaultValue != null) {
            diameter = defaultValue.diameter;
            color = defaultValue.color;
            transparency = defaultValue.transparency;
        }
        if (_diameter != null) {
            diameter = _diameter.doubleValue();
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
            LinkedHashMap<String, NodeAppearance2D> nodeAppearances) throws Exception {
	/* [2014.12.27] I.Noda. to adapt new version os JSONIC. */
	//Map<String, Object> map = (Map<String, Object>)JSON.decode(is);
	JSON json = new JSON(JSON.Mode.TRADITIONAL);
	Map<String, Object> map = (Map<String, Object>)json.parse(is);
        NodeAppearance2D noneTagAppearance = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String tag = entry.getKey();
            Map<String, Object> items = (Map<String, Object>)entry.getValue();
            NodeAppearance2D appearance = new NodeAppearance2D(
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

    public static NodeAppearance2D getAppearance(
            LinkedHashMap<String, NodeAppearance2D> nodeAppearances, MapNode node) {
        for (Map.Entry<String, NodeAppearance2D> entry : nodeAppearances.entrySet()) {
            String tag = entry.getKey();
            if (tag.equals(NONE_TAG) || node.hasTag(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
