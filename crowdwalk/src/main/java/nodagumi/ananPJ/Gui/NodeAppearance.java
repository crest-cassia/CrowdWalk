package nodagumi.ananPJ.Gui;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;

import net.arnx.jsonic.JSON;

/**
 * ノードの表示スタイル(直径, 色, 透明度)をタグ別に指定するために使用するクラス
 */
public class NodeAppearance {
    public double diameter = 1.5;
    public Color3f color = Colors.BLACK2;
    public float transparency = 0.75f;
    public Appearance appearance = new Appearance();

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
    }

    public static void loadNodeAppearances(InputStream is,
            LinkedHashMap<String, NodeAppearance> nodeAppearances) throws Exception {
	/* [2014.12.27] I.Noda. to adapt new version os JSONIC. */
	//Map<String, Object> map = (Map<String, Object>)JSON.decode(is);
	JSON json = new JSON(JSON.Mode.TRADITIONAL);
	Map<String, Object> map = (Map<String, Object>)json.parse(is);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String tag = entry.getKey();
            Map<String, Object> items = (Map<String, Object>)entry.getValue();
            nodeAppearances.put(tag, new NodeAppearance(
                (BigDecimal)items.get("diameter"),
                (String)items.get("color"),
                (BigDecimal)items.get("transparency"),
                nodeAppearances.get(tag)
            ));
        }
    }
}
