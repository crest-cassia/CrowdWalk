package nodagumi.ananPJ.misc.osmTools;

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;


public class osmLink extends MapLink {

    public osmLink(int _id, double _length, double _height) {
        super(_id, _length, _height);
    }

    public static String getNodeTypeString() {
        return "way";
    }

    public static MapLink fromDom(Element element, String prevId) {
        int id = Integer.parseInt(element.getAttribute("ref"));
        //double length = Double.parseDouble(element.getAttribute("length"));
        //double width = Double.parseDouble(element.getAttribute("width"));
        String nodes[] = new String[2];
        nodes[0] = prevId;
        nodes[1] =element.getAttribute("ref");

        /* used in NetworkMap.setupNetwork */
        MapLink link = new MapLink(id, 0.0, 0.0);
        NodeList nodelist = element.getChildNodes();
        if (nodelist.getLength() < 2) {
            System.err.println("osmLink.fromDom invalid way tag");
            return null;
        }
        for (int i = 0; i < nodelist.getLength(); i++) {
        }
        //MapLink link = new MapLink(id, length, width);
        //link.getAttributesFromDom(element);

        link.setUserObject(nodes);
        return link;
    }
}
