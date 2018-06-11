package nodagumi.ananPJ.NetworkMap.Polygon;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import nodagumi.ananPJ.NetworkMap.OBMapPart;

/**
 * 表示用のポリゴン
 */
public class MapPolygon extends OBMapPart {
    /**
     * Z インデックス
     */
    private int z_index = 0;

    /**
     * TriangleMeshes 型の座標データ
     */
    private TriangleMeshes triangleMeshes = null;

    /**
     * OuterBoundary 型の座標データ
     */
    private OuterBoundary outerBoundary = null;

    /**
     * InnerBoundary 型の座標データ
     */
    private ArrayList<InnerBoundary> innerBoundaries = new ArrayList();

    /**
     * コンストラクタ
     */
    public MapPolygon(String id) {
        super(id);
    }

    /**
     * コンストラクタ
     */
    public MapPolygon(String id, int z_index, TriangleMeshes triangleMeshes) {
        super(id);
        this.z_index = z_index;
        this.triangleMeshes = triangleMeshes;
    }

    /**
     * コンストラクタ
     */
    public MapPolygon(String id, int z_index, OuterBoundary outerBoundary, ArrayList<InnerBoundary> innerBoundaries) {
        super(id);
        this.z_index = z_index;
        this.outerBoundary = outerBoundary;
        this.innerBoundaries = innerBoundaries;
    }

    /**
     * このオブジェクトのコピーを作成して返す
     */
    public MapPolygon clone() {
        MapPolygon newPolygon = null;
        if (isTriangleMeshes()) {
            TriangleMeshes triangleMeshes = new TriangleMeshes(getTriangleMeshes().getCoordinates().getValue());
            newPolygon = new MapPolygon(ID, z_index, triangleMeshes);
        } else {
            ArrayList<InnerBoundary> innerBoundaries = new ArrayList();
            for (InnerBoundary innerBoundary : this.innerBoundaries) {
                innerBoundaries.add(innerBoundary);
            }
            newPolygon = new MapPolygon(ID, z_index, outerBoundary.clone(), innerBoundaries);
        }
        for (String tag : getTags()) {
            newPolygon.getTags().add(tag);
        }
        return newPolygon;
    }

    public NType getNodeType() {
        return NType.POLYGON;
    }

    public static String getNodeTypeString() {
        return "Polygon";
    }

    /**
     * Z インデックスをセットする
     */
    public void setZIndex(int z_index) {
        this.z_index = z_index;
    }

    /**
     * Z インデックスを取得する
     */
    public int getZIndex() {
        return z_index;
    }

    /**
     * 三角形メッシュ型か?
     */
    public boolean isTriangleMeshes() {
        return triangleMeshes != null;
    }

    /**
     * 平面ポリゴン型か?
     */
    public boolean isPlanePolygon() {
        return outerBoundary != null;
    }

    /**
     * 頂点座標を Path2D に変換する
     */
    public static Path2D convertToPath2D(Coordinates coordinates) {
        Path2D path = new Path2D.Double();
        if (! coordinates.getPoints2D().isEmpty()) {
            for (Point2D point : coordinates.getPoints2D()) {
                if (path.getCurrentPoint() == null) {
                    path.moveTo(point.getX(), point.getY());
                } else {
                    path.lineTo(point.getX(), point.getY());
                }
            }
            path.closePath();
        }
        return path;
    }

    /**
     * 座標がポリゴン境界の内側にあるか?(平面ポリゴン型専用)
     */
    public boolean contains(double x, double y) {
        if (isPlanePolygon() && outerBoundary.contains(x, y)) {
            for (InnerBoundary innerBoundary : innerBoundaries) {
                if (innerBoundary.contains(x, y)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * TriangleMeshes 型の座標データを取得する
     */
    public TriangleMeshes getTriangleMeshes() {
        return triangleMeshes;
    }

    /**
     * OuterBoundary 型の座標データを取得する
     */
    public OuterBoundary getOuterBoundary() {
        return outerBoundary;
    }

    /**
     * InnerBoundary 型の座標データリストを取得する
     */
    public ArrayList<InnerBoundary> getInnerBoundaries() {
        return innerBoundaries;
    }

    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());
        element.setAttribute("id", ID);
        element.setAttribute("z-index", "" + z_index);
        if (isTriangleMeshes()) {
            element.appendChild(triangleMeshes.createElement(dom));
        }
        if (isPlanePolygon()) {
            element.appendChild(outerBoundary.createElement(dom));
            for (InnerBoundary innerBoundary : innerBoundaries) {
                element.appendChild(innerBoundary.createElement(dom));
            }
        }
        return element;
    }

    public static MapPolygon fromDom(Element element) {
        String id = element.getAttribute("id");
        MapPolygon polygon = null;
        try {
            int z_index = 0;
            String attr = element.getAttribute("z-index");
            if (attr != null) {
                z_index = Integer.parseInt(attr);
            }

            NodeList triangleMeshesElements = element.getElementsByTagName("TriangleMeshes");
            NodeList outerBoundaryElements = element.getElementsByTagName("outerBoundaryIs");
            NodeList innerBoundaryElements = element.getElementsByTagName("innerBoundaryIs");

            // 要素の組み合わせの検証
            if (triangleMeshesElements.getLength() > 0 && outerBoundaryElements.getLength() > 0) {
                throw new Exception("\"TriangleMeshes\" element and \"outerBoundaryIs\" element are mixed");
            }
            if (triangleMeshesElements.getLength() > 0 && innerBoundaryElements.getLength() > 0) {
                throw new Exception("\"TriangleMeshes\" element and \"innerBoundaryIs\" element are mixed");
            }
            if (outerBoundaryElements.getLength() == 0 && innerBoundaryElements.getLength() > 0) {
                throw new Exception("There is innerBoundaryIs without outerBoundaryIs");
            }
            if (triangleMeshesElements.getLength() == 0 && outerBoundaryElements.getLength() == 0) {
                throw new Exception("Invalid Polygon element");
            }
            if (triangleMeshesElements.getLength() > 1) {
                throw new Exception("Too many \"TriangleMeshes\" elements");
            }
            if (outerBoundaryElements.getLength() > 1) {
                throw new Exception("Too many \"outerBoundaryIs\" elements");
            }

            if (triangleMeshesElements.getLength() == 1) {
                TriangleMeshes triangleMeshes = new TriangleMeshes((Element)triangleMeshesElements.item(0));
                polygon = new MapPolygon(id, z_index, triangleMeshes);
            } else {
                OuterBoundary outerBoundary = new OuterBoundary((Element)outerBoundaryElements.item(0));
                ArrayList<InnerBoundary> innerBoundaries = new ArrayList();
                for (int index = 0; index < innerBoundaryElements.getLength(); index++) {
                    innerBoundaries.add(new InnerBoundary((Element)innerBoundaryElements.item(index)));
                }
                polygon = new MapPolygon(id, z_index, outerBoundary, innerBoundaries);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        polygon.getAttributesFromDom(element);  // tag が存在していたらセットする
        return polygon; 
    }
}
