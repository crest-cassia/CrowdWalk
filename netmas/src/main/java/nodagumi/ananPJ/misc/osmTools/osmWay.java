package nodagumi.ananPJ.misc.osmTools;

import java.io.Serializable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;


public class osmWay extends MapPartGroup implements Serializable {
    public osmWay(int _id) {
        super(_id, null, null, 0.0);
    }

    public static String getNodeTypeString() {
        return "way";
    }

    public static OBNode fromDom(Element element) {
        MapPartGroup group = new MapPartGroup(0);
        NodeList nodelist = element.getChildNodes();
        if (nodelist.getLength() < 2) {
            System.err.println("osmWay.fromDom invalid way tag.");
            return null;
        }
        int prevId = 0;
        //group.getAttributesFromDom(element);
        NodeList elm_children = element.getChildNodes();
        for (int i = 0; i < elm_children.getLength(); ++i) {
            Element node = (Element) elm_children.item(i);
            OBNode child = null;
            if (prevId != 0) {
                child = OBNode.fromDom(node);
            }
            NamedNodeMap nnm = elm_children.item(i).getAttributes();
            if (nnm.getLength() < 1) {
                continue;
            }
            for (int j = 0; j < nnm.getLength(); j++) {
                if (nnm.item(j).getNodeName().equals("ref")) {
                    prevId = Integer.parseInt(nnm.item(j).getNodeValue());
                    break;
                }
            }
            if (child != null) {
                group.add(child);
            }
        }
        return group;
    }
}
