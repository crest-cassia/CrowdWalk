package nodagumi.ananPJ.misc.osmTools;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;


public class osmGroup extends MapPartGroup implements Serializable {

    private String version = null;
    private String generator = null;

    public osmGroup(int _id) {
        super(_id);
    }

    @Override
    public NType getNodeType() {
        return NType.GROUP;
    }

    public static String getNodeTypeString() {
        return "osm";
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public static OBNode fromDom(Element element) {
        osmGroup group = new osmGroup(0);
        /*
        group.getAttributesFromDom(element);
        NodeList elm_children = element.getChildNodes();

        System.err.println("debug len elm_children: " +
                elm_children.getLength());

        // read nodes
        for (int i = 0; i < elm_children.getLength(); i++) {
            OBNode child = null;
            if (elm_children.item(i) instanceof Element) {
                Element node = (Element)elm_children.item(i);
                if (node.getNodeName().equals("node")) {
                    int osmID = Integer.parseInt(node.getAttribute("id"));
                    double latitude = Double.parseDouble(node.getAttribute(
                            "lat"));
                    double longitude = Double.parseDouble(node.getAttribute(
                            "lon"));
                    Point2D coordinates = new Point2D.Double(longitude,
                            latitude);
                    child = (OBNode) new osmNode(osmID, coordinates);
                    ((osmNode) child).getAttributesFromDom(node);
                }
            }
            if (child != null) {
                group.add(child);
            }
        }
        ArrayList<MapNode> nodes = group.getChildNodes();
        // read links
        for (int i = 0; i < elm_children.getLength(); i++) {
            if (elm_children.item(i) instanceof Element) {
                Element node = (Element)elm_children.item(i);
                if (node.getNodeName().equals("way")) {
                    NodeList nodelist = node.getChildNodes();
                    if (nodelist.getLength() < 2) {
                        System.err.println("osmGroup.fromDom invalid way tag");
                        continue;
                    }
                    int prevId = 0;
                    //group.getAttributesFromDom(element);
                    for (int j = 0; j < nodelist.getLength(); j++) {

                        System.err.println("debug i: " + i + ", j: " + j +
                            ", node: " + node.getNodeName() +
                            ", item: " + nodelist.item(j).getNodeName());

                        if (nodelist.item(j) instanceof Element) {
                            Element nd = (Element) nodelist.item(j);
                            if (!nd.getNodeName().equals("nd")) {
                                System.err.println("\tnd: " + nd.getNodeName());
                                continue;
                            }
                            System.err.println("\tlink prevId: " + prevId);
                            OBNode child = null;
                            String nodes_string[] = new String[2];
                            if (prevId != 0) {
                                //child = OBNode.fromDom(node);
                                int id = Integer.parseInt(nd.getAttribute(
                                            "ref"));
                                nodes_string[0] = "" + prevId;
                                nodes_string[1] = "" + id;
                                Point2D from = null;
                                Point2D to = null;
                                for (MapNode osm_node : nodes) {
                                    if (osm_node.ID == id)
                                        to = osm_node.getAbsoluteCoordinates();
                                    if (osm_node.ID == prevId)
                                        from = osm_node
                                            .getAbsoluteCoordinates();
                                    if (to != null && from != null)
                                        break;
                                }
                                child = (OBNode) new osmLink(id, from.distance(
                                            to), 1.0);
                            }
                            //child.getAttributesFromDom();
                            if (child != null) {
                                ((MapLink) child).setUserObject(nodes_string);
                                group.add(child);
                            }
                            prevId = Integer.parseInt(nd.getAttribute("ref"));
                            System.err.println("\t\tlink prevId: " + prevId);
                        }
                    }
                }
            }
        }
        */
        return group;
    }

    @Override
    protected void getAttributesFromDom(Element element) {
        //super.getAttributesFromDom(element);
        version = element.getAttribute("version");
        generator = element.getAttribute("generator");
    }

    /*
    @SuppressWarnings("unchecked")
    public ArrayList<osmNode> getChildNodes() {
        ArrayList<osmNode> children = new ArrayList<osmNode>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.NODE) {
                children.add((osmNode)node);
            }
        }
        return children;
    }*/

    public String getVersion() {
        return version;
    }

    public String getGenerator() {
        return generator;
    }

    /*
    public static MapNode fromDom(Element element) {
        int osmID = Integer.parseInt(element.getAttribute("id"));
        double latitude = Double.parseDouble(element.getAttribute("lat"));
        double longitude = Double.parseDouble(element.getAttribute("lon"));
        Point2D coordinates = new Point2D.Double(longitude, latitude);
        osmNode node = new osmNode(osmID, coordinates);
        node.getAttributesFromDom(element);

        return node;
    }
    */
}
