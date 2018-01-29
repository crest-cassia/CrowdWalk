package nodagumi.ananPJ.NetworkMap.Polygon;

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import math.geom3d.Point3D;

/**
 * ポリゴンを構成する三角形メッシュの頂点座標
 */
public class TriangleMeshes {
    /**
     * DOM 要素のタグ名
     */
    public static final String TAG_NAME = "TriangleMeshes";

    /**
     * coordinates が法線ベクトルを含んでいるか?
     */
    private final boolean normal = false;

    /**
     * 頂点座標
     */
    private Coordinates coordinates = null;

    /**
     * コンストラクタ
     */
    public TriangleMeshes(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * コンストラクタ
     */
    public TriangleMeshes(ArrayList<Point3D> coordinates) {
        this.coordinates = new Coordinates(coordinates, 3);
    }

    /**
     * コンストラクタ.
     *
     * @param element DOM の TriangleMeshes 要素
     */
    public TriangleMeshes(Element element) throws Exception {
        String tagName = element.getTagName();
        if (! tagName.equals(TAG_NAME)) {
            throw new Exception("Tag name is not \"" + TAG_NAME + "\" : " + tagName);
        }

        NodeList elements = element.getElementsByTagName("coordinates");
        if (elements.getLength() > 1) {
            throw new Exception("Too many \"coordinates\" element");
        } else if (elements.getLength() == 1) {
            coordinates = new Coordinates((Element)elements.item(0), 3);
        } else {
            coordinates = new Coordinates(3);
        }
    }

    /**
     * coordinates が法線ベクトルを含んでいるか?
     */
    public boolean isNormal() {
        return normal;
    }

    /**
     * 頂点座標を取得する
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * 頂点座標配列を取得する
     */
    public float[] getPoints() {
        float[] points = new float[coordinates.getValue().size() * 3];
        for (int index = 0; index < coordinates.getValue().size(); index++) {
            Point3D point = coordinates.getValue().get(index);
            points[index * 3] = (float)point.getX();
            points[index * 3 + 1] = (float)point.getY();
            points[index * 3 + 2] = (float)point.getZ();
        }
        return points;
    }

    /**
     * DOM 要素を生成する
     */
    public Element createElement(Document dom) {
        Element element = dom.createElement(TAG_NAME);
        element.appendChild(coordinates.createElement(dom));
        return element;
    }
}
