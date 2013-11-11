package nodagumi.ananPJ.misc.osmTools;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;


public class osmNode extends MapNode {

    private int osmID = 0;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String action = null;
    private String visible = null;

    public osmNode(int _id, Point2D _coordinates) {
        super(_id, _coordinates, 0.0);
        this.setOsmId(_id);
        this.setLatitude(_coordinates.getY());
        this.setLongitude(_coordinates.getX());
    }

    public static String getNodeTypeString() {
        return "node";
    }

    public static MapNode fromDom(Element element) {
        int osmID = Integer.parseInt(element.getAttribute("id"));
        double latitude = Double.parseDouble(element.getAttribute("lat"));
        double longitude = Double.parseDouble(element.getAttribute("lon"));
        Point2D coordinates = new Point2D.Double(longitude, latitude);
        osmNode node = new osmNode(osmID, coordinates);
        node.getAttributesFromDom(element);

        return node;
    }

    @Override
    protected void getAttributesFromDom(Element element) {
        super.getAttributesFromDom(element);
        action = element.getAttribute("action");
        visible = element.getAttribute("visible");
    }

    public int getOsmId() {
        return osmID;
    }

    public void setOsmId(int _id) {
        osmID = _id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double _latitude) {
        latitude = _latitude;
    }
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double _longitude) {
        longitude = _longitude;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String _action) {
        action = _action;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String _visible) {
        visible = _visible;
    }
}
