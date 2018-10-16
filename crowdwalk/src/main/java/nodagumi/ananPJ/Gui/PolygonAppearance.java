package nodagumi.ananPJ.Gui;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.misc.JsonicHashMapGetter;
import nodagumi.Itk.Itk;

/**
 * ポリゴン表示スタイルの定義情報
 */
public class PolygonAppearance extends JsonicHashMapGetter {
    /**
     * このスタイルを適用するタグ
     */
    protected String tag;

    /**
     * タグの正規表現パターン
     */
    protected Pattern tagPattern = null;

    protected String drawMode = "FILL";
    protected String colorValue = null;
    protected double transparency = 0.0;
    protected String fileName = null;
    protected String faceCulling = "NONE";
    protected float[] texCoords = null;
    protected int[] faces = null;

    /**
     * コンストラクタ
     */
    public PolygonAppearance(HashMap parameters) throws Exception {
        setParameters(parameters);

        // 適用対象となるタグ
        tag = getStringParameter("tag", "*");
        Matcher matcher = Pattern.compile("^\\/(.*)\\/$").matcher(tag);
        // タグが正規表現か?
        if (matcher.find()) {
            tagPattern = Pattern.compile(matcher.group(1));
        }

        drawMode = getStringParameter("drawMode", drawMode).toUpperCase();
        faceCulling = getStringParameter("faceCulling", faceCulling).toUpperCase();
        HashMap<String, Object> color = getHashMapParameter("color", null);
        HashMap<String, Object> texture = getHashMapParameter("texture", null);

        if (color != null) {
            setParameters(color);
            colorValue = getStringParameter("value", null);
            if (colorValue == null) {
                throw new Exception("Polygon appearance file format error.");
            }
            transparency = getDoubleParameter("transparency", transparency);
            if (transparency < 0.0) {
                transparency = 0.0;
            } else if (transparency > 1.0) {
                transparency = 1.0;
            }
            ArrayList<BigDecimal> list = getBigDecimalArrayList("faces", null);
            if (list != null) {
                faces = new int[list.size()];
                for (int index = 0; index < list.size(); index++) {
                    faces[index] = list.get(index).intValue();
                }
            }
        }

        if (texture != null) {
            setParameters(texture);
            fileName = getStringParameter("fileName", null);
            if (fileName == null) {
                throw new Exception("Polygon appearance file format error.");
            }

            ArrayList<BigDecimal> list = getBigDecimalArrayList("texCoords", null);
            if (list == null) {
                throw new Exception("Polygon appearance file format error.");
            }
            texCoords = new float[list.size()];
            for (int index = 0; index < list.size(); index++) {
                texCoords[index] = list.get(index).floatValue();
            }

            list = getBigDecimalArrayList("faces", null);
            if (list == null) {
                throw new Exception("Polygon appearance file format error.");
            }
            faces = new int[list.size()];
            for (int index = 0; index < list.size(); index++) {
                faces[index] = list.get(index).intValue();
            }
        }
    }

    public String getTag() {
        return tag;
    }

    /**
     * タグに該当する文字列か?
     */
    public boolean isTagApplied(String text) {
        if (tagPattern == null) {
            if (tag.equals("*") || text.indexOf(tag) != -1) {
                return true;
            }
        } else {
            return tagPattern.matcher(text).find();
        }
        return false;
    }

    public HashMap getHashMapSection(String name) {
        HashMap section = null;
        try {
            section = getHashMapParameter(name, null);
        } catch (Exception e) {
	    Itk.quitWithStackTrace(e) ;
        }
        return section;
    }

    public HashMap getColor() {
        return getHashMapSection("color");
    }

    public HashMap getTexture() {
        return getHashMapSection("texture");
    }

    public String getDrawMode() {
        return drawMode;
    }

    public String getColorValue() {
        return colorValue;
    }

    public double getTransparency() {
        return transparency;
    }
    
    public double getOpacity() {
        return 1.0 - transparency;
    }

    public String getTextureFileName() {
        return fileName;
    }

    public String getFaceCulling() {
        return faceCulling;
    }

    public float[] getTexCoords() {
        return texCoords;
    }

    public int[] getFaces() {
        return faces;
    }

    /**
     * polygon appearance file を読み込む
     */
    public static ArrayList<PolygonAppearance> load(String fileName) throws Exception {
        JSON json = new JSON(JSON.Mode.TRADITIONAL);
        ArrayList<PolygonAppearance> appearances = new ArrayList();
        if (fileName != null && ! fileName.isEmpty()) {
            InputStream is = new FileInputStream(fileName);
            for (HashMap parameters : (ArrayList<HashMap>)json.parse(is)) {
                appearances.add(new PolygonAppearance(parameters));
            }
        }
        InputStream is = PolygonAppearance.class.getResourceAsStream("/polygon_appearance.json");
        for (HashMap parameters : (ArrayList<HashMap>)json.parse(is)) {
            appearances.add(new PolygonAppearance(parameters));
        }
        return appearances;
    }
}
