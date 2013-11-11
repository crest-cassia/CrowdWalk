package nodagumi.ananPJ.NetworkParts.Pollution;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;   // tkokada

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.ArrayList; // tkokada

import javax.media.j3d.Appearance;
import javax.media.j3d.BadTransformException;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4d;   // tkokada

import nodagumi.ananPJ.NetworkParts.OBNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.j3d.utils.geometry.Box;

public class PollutedAreaRectangle extends PollutedArea
    implements Serializable {
    private static final long serialVersionUID = -8958754706816120068L;

    public PollutedAreaRectangle(int id) {
        super(id);
    }

    private Rectangle2D bounds;
    private double minHeight, maxHeight;
    private double angle;   // tkokada

    public boolean selected;
    public boolean view;
    public PollutedAreaRectangle(int _id,
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
    
    @Override
    public boolean contains(Vector3f point) {
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

    @Override
    public boolean contains(Point2D point) {
        // tkokada
        return bounds.contains(rotatePoint(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), point, -angle));
    }
    
    @Override
    public Shape getShape() {
        return bounds;
    }
    
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
    
    @Override // tkokada
    public double getAngle() {
        return angle;
    }

    @Override
    protected void getAttributesFromDom(Element element) {
        super.getAttributesFromDom(element);

        ID = Integer.parseInt(element.getAttribute("id"));

        double x1 = Double.parseDouble(element.getAttribute("pWestX"));
        double x2 = Double.parseDouble(element.getAttribute("pEastX"));
        double y1 = Double.parseDouble(element.getAttribute("pSouthY"));
        double y2 = Double.parseDouble(element.getAttribute("pNorthY"));
        bounds = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);

        minHeight = Double.parseDouble(element.getAttribute("minHeight"));
        maxHeight = Double.parseDouble(element.getAttribute("maxHeight"));
        try {
            angle = Double.parseDouble(element.getAttribute("angle"));
        } catch (Exception e) {
            angle = 0;
        }
    }

    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());

        element.setAttribute("id", "" + ID);
        element.setAttribute("pWestX", "" + bounds.getMinX());
        element.setAttribute("pEastX", "" + bounds.getMaxX());
        element.setAttribute("pSouthY", "" + bounds.getMinY());
        element.setAttribute("pNorthY", "" + bounds.getMaxY());
        element.setAttribute("minHeight", "" + minHeight);
        element.setAttribute("maxHeight", "" + maxHeight);
        element.setAttribute("angle", "" + angle);

        return element;
    }

    // tkokada added
    private Point2D rotatePoint(Point2D origin, Point2D point, double angle) {
        return new Point2D.Double(
                origin.getX() + (point.getX() - origin.getX()) * Math.cos(angle) - (point.getY() - origin.getY()) * Math.sin(angle),
                origin.getY() + (point.getX() - origin.getX()) * Math.sin(angle) + (point.getY() - origin.getY()) * Math.cos(angle));
    }
    
    static Color gray8050 = new Color(0.8f, 0.8f, 0.8f, 0.5f);
    @Override
    public void draw(Graphics2D g, 
            boolean experiment) {
                
        if (experiment) return;

        if (selected) {
            g.setColor(Color.YELLOW);
        } else {
            g.setColor(gray8050);
        }
        
        g.fill(bounds);
        g.setColor(Color.BLACK);
        g.drawString(this.getTagString(),
                (float)bounds.getMinX(),
                (float)bounds.getMaxY());
    }

    @Override
    public TransformGroup get3DShape(Appearance app) {
        float x = (float)bounds.getCenterX();
        float y = (float)bounds.getCenterY();
        float z = (float) ((minHeight + maxHeight) / 2);
        float dx = (float)bounds.getWidth() / 2;
        float dy = (float)bounds.getHeight() / 2;
        float dz = (float) ((maxHeight - minHeight) / 2);
        
        Transform3D trans3d = new Transform3D();
        trans3d.setTranslation(new Vector3d(x, y, z));
        trans3d.setRotation(new AxisAngle4d(0, 0, 1.0, angle)); // tkokada
        TransformGroup pollutionTransforms = null;
        try {
            pollutionTransforms = new TransformGroup(trans3d);
        } catch (BadTransformException e){
            pollutionTransforms = new TransformGroup();
            System.err.println("PollutedAreaRectangle.get3DShape: catch BadTransformException!");
            return null;
        }

        Box box = new Box(dx, dy, dz, app);
        pollutionTransforms.addChild(box);
        
        return pollutionTransforms;
    }
    /* to/from DOM codes */
    static public String getNodeTypeString() {
        return "Room";
    }

    @Override
    public NType getNodeType() {
        return NType.ROOM;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    public static OBNode fromDom(Element element) {
        PollutedAreaRectangle room = new PollutedAreaRectangle(0);
        room.getAttributesFromDom(element);
        //System.err.println(room.toString());
        return room;

    }

    @Override   
    public String toString() {
        return getTagString() + bounds.toString();
    }

    @Override
    public String getHintString() {
        return "" + bounds.toString();
    }
    
    @Override
    public double getDensity() {
        Object o = getUserObject();
        if (o != null && o instanceof Double) return ((Double)o).doubleValue();
        return 0.0;
    }

    @Override
    public double distance(Vector3f point) {
        // TODO Auto-generated method stub
        return Double.MAX_VALUE;
    }

    @Override
    public boolean getContactOfAgents() {return view;}
    @Override
    public void setContactOfAgents(boolean _view) {view = _view;}
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
