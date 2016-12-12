package nodagumi.ananPJ.Gui;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.NetworkMap.Node.*;

/**
 * ノードの表示スタイル(直径, 色, 透明度)をタグ別に指定するために使用するクラス
 */
public class NodeAppearance3D {
    public static final String NONE_TAG = "__NONE__";

    public double diameter = 1.5;
    public Color color = FxColor.BLACK2;
    public double opacity = 0.25;
    public PhongMaterial material;

    public NodeAppearance3D(BigDecimal _diameter, String colorName,
            BigDecimal _transparency, NodeAppearance3D defaultValue) {
        if (defaultValue != null) {
            diameter = defaultValue.diameter;
            color = defaultValue.color;
            opacity = defaultValue.opacity;
        }
        if (_diameter != null) {
            diameter = _diameter.doubleValue();
        }
        if (colorName != null) {
            color = FxColor.getColor(colorName);
        }
        if (_transparency != null) {
            opacity = 1.0 - _transparency.doubleValue();
        }
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);

        material = new PhongMaterial();
        material.setDiffuseColor(color);
    }

    public static void load(InputStream is,
            LinkedHashMap<String, NodeAppearance3D> nodeAppearances) throws Exception {
	JSON json = new JSON(JSON.Mode.TRADITIONAL);
	Map<String, Object> map = (Map<String, Object>)json.parse(is);
        NodeAppearance3D noneTagAppearance = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String tag = entry.getKey();
            Map<String, Object> items = (Map<String, Object>)entry.getValue();
            NodeAppearance3D appearance = new NodeAppearance3D(
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

    public static NodeAppearance3D getAppearance(
            LinkedHashMap<String, NodeAppearance3D> nodeAppearances, MapNode node) {
        for (Map.Entry<String, NodeAppearance3D> entry : nodeAppearances.entrySet()) {
            String tag = entry.getKey();
            if (tag.equals(NONE_TAG) || node.hasTag(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static NodeAppearance3D getAppearance(
            LinkedHashMap<String, NodeAppearance3D> nodeAppearances, ArrayList<String> tags) {
        for (Map.Entry<String, NodeAppearance3D> entry : nodeAppearances.entrySet()) {
            String tag = entry.getKey();
            if (tag.equals(NONE_TAG) || tags.contains(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
