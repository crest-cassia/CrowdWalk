// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap.Area;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList; // tkokada
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import math.geom3d.Point3D;

import nodagumi.ananPJ.NetworkMap.OBNode;

//======================================================================
/**
 * 地図上の方形エリア。
 */
public class MapAreaRectangle extends MapArea {
    /**
     * エリアの表示色
     */
    public static enum ObstructerDisplay {
        NONE, HSV, RED, BLUE, ORANGE;

        public static String[] getNames() {
            String[] names = new String[values().length];
            int index = 0;
            for (ObstructerDisplay value : values()) {
                names[index++] = value.toString();
            }
            return names;
        }
    };

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 境界
     */
    private Rectangle2D bounds;

    /**
     * 高さの範囲。
     */
    private double minHeight, maxHeight;

    /**
     * 角度。
     */
    private double angle;   // tkokada

    /**
     * ??
     */
    protected static boolean nsWarned = false;

    /**
     * ??
     */
    protected static boolean weWarned = false;

    /**
     * ??
     */
    public boolean selected;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public MapAreaRectangle(String id) {
        super(id);
    }

    /**
     * コンストラクタ
     */
    public MapAreaRectangle(String _id,
            Rectangle2D _bounds,
            double _min_height,
            double _max_height,
            double _angle) {
        this(_id);
        bounds = _bounds;
        minHeight = _min_height;
        maxHeight = _max_height;
        angle = _angle * Math.PI / 180; // tkokada
    }
    
    //------------------------------------------------------------
    /**
     * 包含判定
     */
    @Override
    public boolean contains(Point3D point) {
        if (point.getZ() < minHeight || point.getZ() > maxHeight) {
            //System.err.println("height not match");
            return false;
        }
        if (!bounds.contains(rotatePoint(
                new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
                new Point2D.Double(point.getX(), point.getY()),
                -angle))) {
            return false;
        }
        return true;
    }

    //------------------------------------------------------------
    /**
     * 包含判定
     */
    @Override
    public boolean contains(Point2D point) {
        // tkokada
        return bounds.contains(rotatePoint(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), point, -angle));
    }
    
    //------------------------------------------------------------
    /**
     * 交差判定
     */
    @Override
	public boolean intersectsLine(Line2D line) {
        return bounds.intersectsLine(line);
    }

    //------------------------------------------------------------
    /**
     * 形状。
     */
    @Override
    public Shape getShape() {
        return bounds;
    }
    
    //------------------------------------------------------------
    /**
     * 高さの範囲の最小値を取得。
     */
    @Override
    public double getMinHeight() {
        return minHeight;
    }

    //------------------------------------------------------------
    /**
     * 高さの範囲の最大値を取得。
     */
    @Override
    public double getMaxHeight() {
        return maxHeight;
    }

    //------------------------------------------------------------
    /**
     * 頂点リスト。
     */
    @Override // tkokada
    public ArrayList<Point2D> getAllVertices() {
        ArrayList<Point2D> allVertices = new ArrayList<Point2D>();
        Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        allVertices.add(rotatePoint(center, new Point2D.Double(bounds.getMinX(), bounds.getMinY()), angle));
        allVertices.add(rotatePoint(center, new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), angle));
        allVertices.add(rotatePoint(center, new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), angle));
        allVertices.add(rotatePoint(center, new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), angle));
        
        return allVertices;
    }
    
    //------------------------------------------------------------
    /**
     * 角度。
     */
    @Override // tkokada
    public double getAngle() {
        return angle;
    }

    //------------------------------------------------------------
    /**
     * DOM からの取得。
     */
    @Override
    protected void getAttributesFromDom(Element element) {
        super.getAttributesFromDom(element);

        ID = element.getAttribute("id");

        double x1 = Double.parseDouble(element.getAttribute("pWestX"));
        double x2 = Double.parseDouble(element.getAttribute("pEastX"));
        double y1 = Double.parseDouble(element.getAttribute("pNorthY"));
        double y2 = Double.parseDouble(element.getAttribute("pSouthY"));
        if (y1 > y2) {
            if (! nsWarned) {
                System.err.println(String.format("MapArea coordinate error: pNorthY = %s > pSouthY = %s", y1, y2));
                nsWarned = true;
                // Itk.quitByError() ;
            }
            double y = y1;
            y1 = y2;
            y2 = y;
        }
        if (x1 > x2) {
            if (! weWarned) {
                System.err.println(String.format("MapArea coordinate error: pWestX = %s > pEastX = %s", x1, x2));
                weWarned = true;
                // Itk.quitByError() ;
            }
            double x = x1;
            x1 = x2;
            x2 = x;
        }
        bounds = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);

        minHeight = Double.parseDouble(element.getAttribute("minHeight"));
        maxHeight = Double.parseDouble(element.getAttribute("maxHeight"));
        try {
            angle = Double.parseDouble(element.getAttribute("angle"));
        } catch (Exception e) {
            angle = 0;
        }
    }

    //------------------------------------------------------------
    /**
     * DOM へ。
     */
    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());

        element.setAttribute("id", ID);
        element.setAttribute("pWestX", "" + bounds.getMinX());
        element.setAttribute("pEastX", "" + bounds.getMaxX());
        element.setAttribute("pNorthY", "" + bounds.getMinY());
        element.setAttribute("pSouthY", "" + bounds.getMaxY());
        element.setAttribute("minHeight", "" + minHeight);
        element.setAttribute("maxHeight", "" + maxHeight);
        element.setAttribute("angle", "" + angle);

        return element;
    }

    //------------------------------------------------------------
    /**
     * 回転。
     */
    // tkokada added
    private Point2D rotatePoint(Point2D origin, Point2D point, double angle) {
        return new Point2D.Double(
                origin.getX() + (point.getX() - origin.getX()) * Math.cos(angle) - (point.getY() - origin.getY()) * Math.sin(angle),
                origin.getY() + (point.getX() - origin.getX()) * Math.sin(angle) + (point.getY() - origin.getY()) * Math.cos(angle));
    }

    //------------------------------------------------------------
    /**
     * DOM
     */
    public static OBNode fromDom(Element element) {
        MapAreaRectangle area = new MapAreaRectangle(null);
        area.getAttributesFromDom(element);
        return area;
    }

    //------------------------------------------------------------
    /**
     * 文字列化。
     */
    @Override   
    public String toString() {
        return getTagString() + bounds.toString();
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    @Override
    public String getHintString() {
        return "" + bounds.toString();
    }
    
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
