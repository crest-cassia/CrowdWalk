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

import nodagumi.ananPJ.NetworkMap.Link.*;

/**
 * リンクの表示スタイル(幅, 色, 透明度)をタグ別に指定するために使用するクラス
 */
public class LinkAppearance3D {
    public static final String NONE_TAG = "__NONE__";

    public boolean widthFixed = false;
    public double widthRatio = 1.0;
    public Color color = FxColor.DEFAULT_LINK_COLOR;
    public double opacity = 0.25;
    public PhongMaterial material;

    private LinkAppearance3D() {}

    public LinkAppearance3D(Boolean _widthFixed, BigDecimal _widthRatio,
            String colorName, BigDecimal _transparency, LinkAppearance3D defaultValue) {
        if (defaultValue != null) {
            widthFixed = defaultValue.widthFixed;
            widthRatio = defaultValue.widthRatio;
            color = defaultValue.color;
            opacity = defaultValue.opacity;
        }
        if (_widthFixed != null) {
            widthFixed = _widthFixed;
        }
        if (_widthRatio != null) {
            widthRatio = _widthRatio.doubleValue();
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
            LinkedHashMap<String, LinkAppearance3D> linkAppearances) throws Exception {
	JSON json = new JSON(JSON.Mode.TRADITIONAL);
	Map<String, Object> map = (Map<String, Object>)json.parse(is);
        LinkAppearance3D noneTagAppearance = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String tag = entry.getKey();
            Map<String, Object> items = (Map<String, Object>)entry.getValue();
            BigDecimal widthRatio = (BigDecimal)items.get("width_ratio");
            if (widthRatio == null) {
                widthRatio = (BigDecimal)items.get("width");
            }
            LinkAppearance3D appearance = new LinkAppearance3D(
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

    public static LinkAppearance3D getAppearance(
            LinkedHashMap<String, LinkAppearance3D> linkAppearances, MapLink link) {
        for (Map.Entry<String, LinkAppearance3D> entry : linkAppearances.entrySet()) {
            String tag = entry.getKey();
            if (tag.equals(NONE_TAG) || link.hasTag(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static LinkAppearance3D getAppearance(
            LinkedHashMap<String, LinkAppearance3D> linkAppearances, ArrayList<String> tags) {
        for (Map.Entry<String, LinkAppearance3D> entry : linkAppearances.entrySet()) {
            String tag = entry.getKey();
            if (tag.equals(NONE_TAG) || tags.contains(tag)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
