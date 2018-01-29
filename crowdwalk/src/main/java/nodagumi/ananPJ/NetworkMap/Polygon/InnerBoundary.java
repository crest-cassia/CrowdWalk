package nodagumi.ananPJ.NetworkMap.Polygon;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 平面ポリゴンの内側の境界線
 */
public class InnerBoundary {
    /**
     * DOM 要素のタグ名
     */
    public static final String TAG_NAME = "innerBoundaryIs";

    /**
     * 頂点座標
     */
    private Coordinates coordinates = null;

    /**
     * 頂点座標パス
     */
    private Path2D path = null;

    /**
     * コンストラクタ
     */
    public InnerBoundary(ArrayList<Point2D> points) {
        coordinates = new Coordinates(points);
        path = MapPolygon.convertToPath2D(coordinates);
    }

    /**
     * コンストラクタ.
     *
     * @param element DOM の InnerBoundary 要素
     */
    public InnerBoundary(Element element) throws Exception {
        String tagName = element.getTagName();
        if (! tagName.equals(TAG_NAME)) {
            throw new Exception("Tag name is not \"" + TAG_NAME + "\" : " + tagName);
        }

        NodeList elements = element.getElementsByTagName("coordinates");
        if (elements.getLength() > 1) {
            throw new Exception("Too many \"coordinates\" element");
        } else if (elements.getLength() == 1) {
            coordinates = new Coordinates((Element)elements.item(0), 2);
        } else {
            coordinates = new Coordinates(2);
        }
        path = MapPolygon.convertToPath2D(coordinates);
    }

    /**
     * 座標が境界の内側にあるか?
     */
    public boolean contains(double x, double y) {
        return path.contains(x, y);
    }

    /**
     * 頂点座標リストが空か?
     */
    public boolean isEmpty() {
        return coordinates.getValue().isEmpty();
    }

    /**
     * 頂点座標を取得する
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * 頂点座標リストを取得する
     */
    public ArrayList<Point2D> getPoints() {
        return coordinates.getPoints2D();
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
