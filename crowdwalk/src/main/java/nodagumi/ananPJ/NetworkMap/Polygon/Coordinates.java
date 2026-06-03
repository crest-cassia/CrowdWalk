package nodagumi.ananPJ.NetworkMap.Polygon;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import math.geom3d.Point3D;

/**
 * MapPolygon 用の座標データリスト
 */
public class Coordinates {
    /**
     * DOM 要素のタグ名
     */
    public static final String TAG_NAME = "coordinates";

    /**
     * 座標の次元数
     */
    private int numberOfDimensions;

    /**
     * 座標リスト(次元数によらず Point3D を使用する)
     */
    private ArrayList<Point3D> coordinates = new ArrayList<Point3D>();

    /**
     * コンストラクタ
     */
    public Coordinates(int numberOfDimensions) {
        this.numberOfDimensions = numberOfDimensions;
    }

    /**
     * コンストラクタ
     */
    public Coordinates(ArrayList<Point3D> coordinates, int numberOfDimensions) {
        this.coordinates = (ArrayList<Point3D>)coordinates.clone();
        this.numberOfDimensions = numberOfDimensions;
    }

    /**
     * コンストラクタ
     */
    public Coordinates(ArrayList<Point2D> points) {
        numberOfDimensions = 2;
        for (Point2D point : points) {
            coordinates.add(new Point3D(point.getX(), point.getY(), 0.0));
        }
    }

    /**
     * コンストラクタ
     */
    public Coordinates(String coordinatesText, int numberOfDimensions) throws Exception {
        if (coordinatesText == null) {
            throw new Exception("Coordinates text is null");
        }
        if (coordinatesText.trim().isEmpty()) {
            throw new Exception("Coordinates text is empty");
        }

        this.numberOfDimensions = numberOfDimensions;
        for (String text : coordinatesText.trim().split("\\s+")) {
            String xyz[] = text.split(",");
            if (xyz.length != numberOfDimensions) {
                throw new Exception("Coordinate value dimension error: " + text);
            }
            double x = Double.parseDouble(xyz[0]);
            double y = Double.parseDouble(xyz[1]);
            double z = (xyz.length == 2 ? 0.0 : Double.parseDouble(xyz[2]));
            coordinates.add(new Point3D(x, y, z));
        }
    }

    /**
     * コンストラクタ
     *
     * @param element DOM の coordinates 要素
     */
    public Coordinates(Element element, int numberOfDimensions) throws Exception {
        this.numberOfDimensions = numberOfDimensions;

        String tagName = element.getTagName();
        if (! tagName.equals(TAG_NAME)) {
            throw new Exception("Tag name is not \"" + TAG_NAME + "\" : " + tagName);
        }

        for (String text : element.getTextContent().trim().split("\\s+")) {
            String xyz[] = text.split(",");
            if (xyz.length < 2 || xyz.length > 3) {
                throw new Exception("Polygon coordinate value format error: " + text);
            }
            double x = Double.parseDouble(xyz[0]);
            double y = Double.parseDouble(xyz[1]);
            double z = (xyz.length == 2 ? 0.0 : Double.parseDouble(xyz[2]));
            coordinates.add(new Point3D(x, y, z));
        }
    }

    /**
     * 座標の次元数を取得する
     */
    public int getNumberOfDimensions() {
        return numberOfDimensions;
    }

    /**
     * 座標リストをセットする
     */
    public void setCoordinates(ArrayList<Point3D> coordinates) {
        this.coordinates = (ArrayList<Point3D>)coordinates.clone();
    }

    /**
     * 座標リストを取得する
     */
    public ArrayList<Point3D> getValue() {
        return coordinates;
    }

    /**
     * 座標リストを取得する
     */
    public ArrayList<Point2D> getPoints2D() {
        ArrayList<Point2D> points = new ArrayList();
        for (Point3D point : coordinates) {
            points.add(new Point2D.Double(point.getX(), point.getY()));
        }
        return points;
    }

    /**
     * 座標リストを文字列化して取得する
     */
    public String getCoordinatesText() {
        StringBuilder buff = new StringBuilder();
        for (Point3D point : coordinates) {
            if (buff.length() > 0) {
                buff.append(" ");
            }
            buff.append(point.getX());
            buff.append(",");
            buff.append(point.getY());
            if (numberOfDimensions == 3) {
                buff.append(",");
                buff.append(point.getZ());
            }
        }
        return buff.toString();
    }

    /**
     * DOM 要素を生成する
     */
    public Element createElement(Document dom) {
        Element element = dom.createElement(TAG_NAME);
        element.appendChild(dom.createTextNode(getCoordinatesText()));
        return element;
    }
}
