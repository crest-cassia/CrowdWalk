package nodagumi.ananPJ.misc.osmTools;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.misc.osmTools.osmGroup;


/** This class reads OSM map file and converts it to NetworkMap class.
 * OSM (Open Stream Map) is one of open source world map. The functionalities
 * of this class are as follows:
 *  - reads OSM map file and converts it to NetworkMap class.
 *  - converts NetworkMap object to OSM map file.
 */
public class osmNetworkMap extends NetworkMap {

    private double standardLatitude = 0.0;
    private double standardLongitude = 0.0;

    public osmNetworkMap(Random _random) {
        super(_random);
    }

    public osmNetworkMap(Random _random, boolean _hasDisplay) {
        super(_random, _hasDisplay);
    }

    public boolean fromDOM(Document doc) {
        //NetworkMap networkMap = new NetworkMap(null);
        NodeList toplevel = doc.getChildNodes();
        if (toplevel == null) {
            System.err.println("osm2NetworkMap.fromDOM invalid inputted DOM" +
                    " object.");
            return false;
        }
        if (toplevel.getLength() != 1 ||
                !toplevel.item(0).getNodeName().equals("osm")) {
            System.err.println("osmNetworkMap.fromDom is inputted map file " +
                    "is osm map?");
            return false;
        }
        Element dom_root = (Element) toplevel.item(0);
        setRoot(OBNode.fromDom(dom_root));
        setupOsmNetwork((OBNode) this.root, dom_root);
        NodeList nodelist = dom_root.getChildNodes();
        if (nodelist.getLength() < 1) {
            System.err.println("osmNetworkMap.fromDom osm map file does not " +
                    "include any nodes.");
            return false;
        }
        System.err.println("root: " + this.root);
        setupNodes((OBNode) this.root);
        setupLinks((OBNode) this.root);

        return true;
    }

    private void setupOsmNetwork(OBNode _root, Element _element) {
        //group.getAttributesFromDom(element);
        NodeList elm_children = _element.getChildNodes();

        // System.err.println("debug len elm_children: " +
                // elm_children.getLength());

        // read nodes
        for (int i = 0; i < elm_children.getLength(); i++) {
            OBNode child = null;
            if (elm_children.item(i) instanceof Element) {
                Element node = (Element)elm_children.item(i);
                if (node.getNodeName().equals("node")) {
                    int osmID = Math.abs(Integer.parseInt(node.getAttribute(
                                    "id")));
                    double latitude = Double.parseDouble(node.getAttribute(
                            "lat"));
                    double longitude = Double.parseDouble(node.getAttribute(
                            "lon"));
                    //Point2D coordinates = new Point2D.Double(longitude,
                    //        latitude);
                    Point2D coordinates = geomToMeter(latitude, longitude);
                    child = (OBNode) new osmNode(osmID, coordinates);
                    ((osmNode) child).getAttributesFromDom(node);
                }
            }
            if (child != null) {
                _root.add(child);
            }
        }
        MapNodeTable nodes = ((osmGroup) _root).getChildNodes();
        // read links
        for (int i = 0; i < elm_children.getLength(); i++) {
            if (elm_children.item(i) instanceof Element) {
                Element node = (Element)elm_children.item(i);
                if (node.getNodeName().equals("way")) {
                    NodeList nodelist = node.getChildNodes();
                    if (nodelist.getLength() < 2) {
                        //System.err.println("osmGroup.fromDom invalid way tag");
                        continue;
                    }
                    int prevId = 0;
                    //group.getAttributesFromDom(element);
                    for (int j = 0; j < nodelist.getLength(); j++) {

                        // System.err.println("debug i: " + i + ", j: " + j +
                            // ", node: " + node.getNodeName() +
                            // ", item: " + nodelist.item(j).getNodeName());

                        if (nodelist.item(j) instanceof Element) {
                            Element nd = (Element) nodelist.item(j);
                            if (!nd.getNodeName().equals("nd")) {
                                //System.err.println("\tnd: " + nd.getNodeName());
                                continue;
                            }
                            //System.err.println("\tlink prevId: " + prevId);
                            OBNode child = null;
                            String nodes_string[] = new String[2];
                            if (prevId != 0) {
                                //child = OBNode.fromDom(node);
                                int id = Math.abs(Integer.parseInt(nd
                                            .getAttribute("ref")));
                                nodes_string[0] = "" + prevId;
                                nodes_string[1] = "" + id;
                                Point2D from = null;
                                Point2D to = null;
                                MapNode fromNode = null;
                                MapNode toNode = null;
                                for (MapNode osm_node : nodes) {
                                    if (osm_node.ID == id)
                                        toNode = osm_node;
                                    if (osm_node.ID == prevId)
                                        fromNode = osm_node;
                                    if (toNode != null && fromNode != null)
                                        break;
                                }
                                child = (OBNode) createMapLink(
                                        (MapPartGroup) _root,
                                        fromNode, toNode,
                                        fromNode.getAbsoluteCoordinates()
                                        .distance(toNode
                                        .getAbsoluteCoordinates()), 1.0);
                            }
                            //child.getAttributesFromDom();
                            if (child != null) {
                                ((MapLink) child).setUserObject(nodes_string);
                                _root.add(child);
                            }
                            prevId = Math.abs(Integer.parseInt(nd.getAttribute(
                                            "ref")));
                            //System.err.println("\t\tlink prevId: " + prevId);
                        }
                    }
                }
            }
        }
    }

    private Point2D geomToMeter(double latitude, double longitude) {
        if (standardLatitude == 0.0) {
            standardLatitude = latitude;
            standardLongitude = longitude;
        }
        double x = (latitude - standardLatitude) * 911872.08;
        double y = (longitude - standardLongitude) * 1113194.91;
        Point2D coordinates = new Point2D.Double(x, y);

        return coordinates;
    }

    @SuppressWarnings("unchecked")
    private void setupNodes(OBNode ob_node) {
        if (OBNode.NType.NODE == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            MapNode node = (MapNode) ob_node;
            getNodes().add(node);
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();
                    e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupNodes(child);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setupLinks(OBNode ob_node) {
        if (OBNode.NType.LINK == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            MapLink link = (MapLink) ob_node;
            getLinks().add(link);

            String[] nodes = (String[]) ob_node.getUserObject();
            MapNode from_node = (MapNode) getObject(Integer.parseInt(
                        nodes[0]));
            MapNode to_node = (MapNode) getObject(Integer.parseInt(nodes[1]));
            if (from_node == null) {
                System.err.println(Integer.parseInt("from_node is null " +
                            nodes[0]));
            }
            if (to_node == null) {
                System.err.println(Integer.parseInt("to_node is null " +
                            nodes[1]));
            }
            from_node.addLink(link);
            to_node.addLink(link);
            /*
            try {
                link.setFromTo(from_node, to_node);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Try to set from/to of a link, which " +
                        "alread has it setted\nlink ID: " + link.ID +
                        "Warning setting up network");
            }*/
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();
                    e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupLinks(child);
            }
        }
    }

    /*
    public static NetworkMap osm2NetworkMap(String input_file) {
        try {
            // xml parser with DOM
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream input = new URL(input_file).openStream();
            Document doc = builder.parse(input);
            // code from NetworkMap
            NodeList toplevel = doc.getChildNodes();
            if (toplevel.getLength() != 1) {
                System.err.println("osmReader.osm2NetworkMap number of top " +
                        "level node is over one.");
                return false;
            }
            Element dom_root = (Element) toplevel.item(0);

            //pollutionFile = dom_root.getAttribute("PollutionSettings");
            //if (pollutionFile.isEmpty()) pollutionFile = null;

            setRoot(OBNode.fromDom(dom_root));
            setupNetwork((OBNode)this.root);
            // converts node to Node
            NodeList list = doc.getElementsByTagName("node");
            MapNodeTable nodes = new MapNodeTable();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                NamedNodeMap attributes = n.getAttributes();
                String id = null;
                String lat = null;
                String lon = null;
                for (int j = 0; j < attributes.getLength(); j++) {
                    Node attribute = attributes.item(j);
                    if (attributes == null)
                        continue;
                    if (attribute.getNodeName().equals("id")) {
                        id = attribute.getTextContent().trim();
                    } else if (attribute.getNodeName().equals("lat")) {
                        lat = attribute.getTextContent().trim();
                    } else if (attribute.getNodeName().equals("lon")) {
                        lon = attribute.getTextContent().trim();
                    }
                    if (id != null && lat != null && lon != null) {
                        int ID = Integer.valueOf(id);
                        double LAT = Double.valueOf(lat);
                        double LON = Double.valueOf(lon);
                        nodes.add(new MapNode(ID,
                                    new Point2D.Double(LAT, LON)), 0.0);
                        // System.err.println("Node id: " + id + " lat: " +
                        // lat + " lon: " + lon);
                    }
                }
            }
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException saxe) {
            saxe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static MapNode fromDom(Element element) {
        int id = Integer.parseInt(element.getAttribute("id"));
        double x = Double.parseDouble(element.getAttribute("lon"));
        double y = Double.parseDouble(element.getAttribute("lat"));
        Point2D coordinates = new Point2D.Double(x, y);
        //double height = Double.parseDouble(element.getAttribute("height"));
        double height = 0.0;
        MapNode node = new MapNode(id, coordinates, height);
        //node.getAttributesFromDom(element);

        // used in NetworkMap.setupNetwork
        // these functions should be implemented on Link tag.
        ArrayList<String> links = new ArrayList<String>();
        node.setUserObject(links);
        return node;
    }

    public static MapLink fromDom(Element element) {
        int id = Integer.parseInt(element.getAttribute("id"));
        NodeList nlist = element.getChildNodes();
        for (int i = 0; i < nlist.getLength(); i++) {
            String nname = nlist.item(i).getNodeName();
            if (nname.equals("nd")) {
                NamedNodeMap nnm = nlist.item(i).getAttributes();
                if (nnm.getLength() != 1)
                    continue;
                int nodeId = -1;
                if (nnm.item(0).getNodeName().equals("ref"))
                    nodeId = Integer.parseInt(nnm.item(0).getNodeValue());
            } else if (nname.equals("tag")) {
            } else {
                continue;
            }
        }
        // used in NetworkMap.setupNetwork
        MapLink link = new MapLink(id, length, width);
        link.getAttributesFromDom(element);

        link.setUserObject(nodes);
        return link;
    }

    public static void main(String[] args) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        try {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream input = new URL("file:///tmp/sumida.osm").openStream();
            Document doc = builder.parse(input);
            NodeList list = doc.getElementsByTagName("node");
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                NamedNodeMap attributes = n.getAttributes();
                String id = null;
                String lat = null;
                String lon = null;
                for (int j = 0; j < attributes.getLength(); j++) {
                    Node attribute = attributes.item(j);
                    if (attributes == null)
                        continue;
                    if (attribute.getNodeName().equals("id")) {
                        id = attribute.getTextContent().trim();
                    } else if (attribute.getNodeName().equals("lat")) {
                        lat = attribute.getTextContent().trim();
                    } else if (attribute.getNodeName().equals("lon")) {
                        lon = attribute.getTextContent().trim();
                    }
                    if (id != null && lat != null && lon != null) {
                        map.put(id, lat);
                        System.err.println("Node id: " + id + " lat: " + lat +
                                " lon: " + lon);
                    }
                }
            }
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException saxe) {
            saxe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        for (String id : map.keySet()) {
            System.err.println(id + "," + map.get(id));
        }
    }
    */
}
