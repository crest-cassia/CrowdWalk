// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts.Node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.vecmath.Vector3d;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBMapPart;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.misc.Snapshot;

import nodagumi.Itk.*;

public class MapNode extends OBMapPart implements Serializable {
    private static final long serialVersionUID = -7438706484453707443L;

    /* global coordinates */
    private Point2D absolute_coordinates;
    private double height;

    private MapLinkTable links;

    /* used in simulation */
    public int displayMode = 0;
    private HashMap<String, NavigationHint> hints;

    public double getX() { return absolute_coordinates.getX(); }
    public double getY() { return absolute_coordinates.getY(); }
    public Vector3d getPoint() {
        return new Vector3d(getX(), getY(), getHeight());
    }
    public Point2D getAbsoluteCoordinates(){ return absolute_coordinates; }
    public Point2D getLocalCoordinates(){
        return new Point2D.Double(this.getLocalX(),this.getLocalY());
    }

    public MapNode(int _ID,
            Point2D _absoluteCoordinates,
            double _height) {
        super(_ID);

        absolute_coordinates = _absoluteCoordinates;
        setHeight(_height);

        calc_local_coordinates();

        selected = false;
        hints = new HashMap<String, NavigationHint>();
        links = new MapLinkTable();
    }

    private void calc_local_coordinates() {
        //TODO implement
    }

    //temporary
    public double getLocalX(){ return absolute_coordinates.getX(); }
    public double getLocalY(){ return absolute_coordinates.getY(); }
    public double getAbsoluteX(){ return absolute_coordinates.getX(); }
    public double getAbsoluteY(){ return absolute_coordinates.getY(); }

    public double calcDistance(MapNode other) {
        return Math.sqrt((getX() - other.getX()) * (getX() - other.getX()) +
                (getY() - other.getY()) * (getY() - other.getY()) +
                (getHeight() - other.getHeight()) * (getHeight() - other.getHeight()))
                * ((MapPartGroup)getParent()).getScale();
    }

    /* invert transformation  by parent Affine transform */
    public void setAbsoluteCoordinates(Point2D _absoluteCoorditanes){
        absolute_coordinates = _absoluteCoorditanes;
    }

    public boolean addLink(MapLink link) {
        if (links.contains(link)) return false;
        links.add(link);
        clearCache() ;
        return true;
    }

    public boolean removeLink(MapLink link) {
        if (!links.contains(link)) return false;
        links.remove(link);
        clearCache() ;
        return true;
    }

    public MapLinkTable getLinks() {
        return links;
    }

    /**
     * getPathways などを効率化するための cache を開放する。
     */
    public void clearCache() {
        cachePathways = null ;
        cachePathwaysReverse = null ;
    }

    /**
     * getPathways を効率化するための cache
     * Node とそれに繋がるリンクのタグなどが変化すれば、
     * reset されなければならない。
     * その際には、resetCache() を呼ぶこと。
     */
    private MapLinkTable cachePathways = null ;

    /**
     * 一方通行を考慮して、つながっているリンクを集める。
     */
    public MapLinkTable getPathways () {
        // もしすでに cache があれば、それを返す。
		if(cachePathways != null) return cachePathways ;

        /* modification to apply One-way link */
        cachePathways = new MapLinkTable();
        for (MapLink link : links) {
            if (link.isOneWayPositive() &&
                    (link.getPositiveNode() == this)) {
                continue;
            }
            if (link.isOneWayNegative() &&
                    link.getNegativeNode() == this) {
                continue;
            }
            if (link.isRoadClosed()) {
                continue;
            }
            cachePathways.add(link);
        }

        /* original: return links */
        return cachePathways;
    }

    /**
     * getPathwaysReverse を効率化するための cache
     * Node とそれに繋がるリンクのタグなどが変化すれば、
     * reset されなければならない。
     * その際には、resetCache() を呼ぶこと。
     */
    private MapLinkTable cachePathwaysReverse = null ;

    /**
     * 一方通行を考慮して、つながっているリンクを集める。
     */
    public MapLinkTable getPathwaysReverse () {
		if(cachePathwaysReverse != null) return cachePathwaysReverse ;

        /* modification to apply One-way link */
        MapLinkTable availableLinks = new MapLinkTable();
        for (MapLink link : links) {
            if (link.isOneWayPositive() &&
                    (link.getNegativeNode() == this)) {
                continue;
            }
            if (link.isOneWayNegative() &&
                    link.getPositiveNode() == this) {
                continue;
            }
            if (link.isRoadClosed()) {
                continue;
            }
            availableLinks.add(link);
        }

        /* original: return links */
        return availableLinks;
    }

    public MapLink connectedTo(MapNode node) {
        for (MapLink link : links) {
            if (link.getFrom() == node) {
                return link;
            } else if (link.getTo() == node) {
                return link;
            }
        }
        return null;
    }
    
    public void addNavigationHint(String key,
            NavigationHint hint) {
        hints.put(key, hint);
    }
    
    public void clearHints() {
        hints.clear();
    }
    
    public NavigationHint getHint(Term key) {
        return getHint(key.getString()) ;
    }

    public NavigationHint getHint(final String key) {
        NavigationHint hint = hints.get(key);
        /* [2014.12.26 I.Noda] 
         * "Exit" タグの特別扱いをやめるので、機能ＯＦＦ
         * ただ、hints の動作が不明なので、コメントで保留。
         */
        /*
         * if (hint == null)
         *   hint = hints.get("Exit");
         */
        if (hint == null) {
            for (String _key : hints.keySet()) {
                NavigationHint _hint = hints.get(_key);
            }
        }
        return hint;
    }

    public HashMap<String, NavigationHint> getHints() {
        return hints;
    }

    public MapLink getWay(String key) {
        NavigationHint hint = getHint(key);
        return hint.way;
    }

    public double getDistance(Term target){
        String key = target.getString() ;
        NavigationHint hint = getHint(key);
        if (hint == null) {
            System.err.println(key + " not found for id="
                    + ID + "(" + getTagString() + ")");

            for (String has_key : hints.keySet()) {
                System.err.println(has_key);
            }
        }
        //System.out.println("MapNode.getHint ID: " + ID + "hints: " +
        //        hints.toString());
        return hint.distance;
    }

    // tkokada:
    // to avoid nullpo when agent is placed on invalid link
    public double getDistanceNullAvoid(Term target) {
        String key = target.getString() ;
        NavigationHint hint = getHint(key);
        if (hint == null)
            return -1.0;
        else
            return hint.distance;
    }

    private Rectangle2D getSquare(double cx, double cy, double l, double scale) {
        return new Rectangle2D.Double(getX() - l / scale,
                getY() - l / scale,
                l * 2 / scale,
                l * 2 / scale);

    }

    public void draw(Graphics2D g,
            boolean simulation,
            boolean showLabel,
            boolean isSymbolic) {
        Color c = null;

        if (isSymbolic)
            c = Color.GRAY;
        else
            c = Color.BLUE;

        draw(g, simulation, showLabel, isSymbolic, c);
    }

    public void draw(Graphics2D g,
                boolean simulation,
                boolean showLabel,
                boolean isSymbolic,
                Color c) {
        if (simulation && (displayMode & 2) != 2) return;
        double scale = g.getTransform().getScaleX();
        double cx = getX();
        double cy = getY();

        if (selected) {
            g.setColor(Color.BLACK);
            g.fill(getSquare(cx, cy, 6, scale));
            g.setColor(Color.RED);
            g.fill(getSquare(cx, cy, 5, scale));
        }
        final double minHight = ((MapPartGroup)getParent()).getMinHeight();
        final double maxHight = ((MapPartGroup)getParent()).getMaxHeight();
        float r = (float)((getHeight() - minHight) / (maxHight - minHight));

        if (r < 0) r = 0;
        if (r > 1) r = 1;

        g.setColor(new Color(r, r, r));
        g.fill(getSquare(cx, cy, 4, scale));

        g.setColor(c);
        g.fill(getSquare(cx, cy, 2, scale));

        /* show description text here? */
        if (showLabel) {
            g.setColor(Color.WHITE);
            g.drawString(getHintString(),
                    (float)cx + 6.0f, (float)cy + 5.0f);
            g.setColor(Color.BLACK);
            g.drawString(getHintString(),
                    (float)cx + 5.0f, (float)cy + 5.0f);
        }
        g.setColor(Color.BLACK);
    }
    
    public boolean isBetweenHeight(double minHeight, double maxHeight) {
        if (getHeight() < minHeight || getHeight() > maxHeight) return false;
        return true;
    }

    public static void showAttributeDialog(MapNodeTable nodes) {
        /* Set attributes with a dialog */
        class AttributeSetDialog  extends JDialog {
            private static final long serialVersionUID = 3824609997449144923L;
            private boolean singleNode;
            private MapNodeTable nodes;

            private double height = 0.0;

            public AttributeSetDialog(MapNodeTable _nodes) {
                super();

                this.setModal(true);
                nodes = _nodes;

                int count = 0;
                singleNode = true;
                for (MapNode node : nodes) {
                    if (node.selected) {
                        if (count != 0) {
                            singleNode = false;
                        }
                        ++count;
                        height += node.getHeight();
                    }
                }
                if (count == 0) return;

                height /= count;                
                setUpPanel();
            }
            
            private JTextField height_field;
            
            private void setUpPanel() {
                Container contentPane = getContentPane();

                GridBagConstraints c;
                /* parameters */
                JPanel parameter_panel = new JPanel(new GridBagLayout());
                parameter_panel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.black), "Parameters"));
                JLabel height_panel = new JLabel("height:");
                c = new GridBagConstraints();
                c.gridx = 0; c.gridy = 0;
                parameter_panel.add(height_panel, c);
                JLabel height_orig_label = new JLabel("" + height + "->");
                c = new GridBagConstraints();
                c.gridx = 1; c.gridy = 0;
                parameter_panel.add(height_orig_label, c);
                height_field = new JTextField("" + height);
                height_field.setPreferredSize(new Dimension(40, 20));
                c = new GridBagConstraints();
                c.gridx = 2; c.gridy = 0;
                parameter_panel.add(height_field, c);
                JButton height_update_button = new JButton("update");
                if (!singleNode) {
                    height_update_button.setText("update all");
                }
                height_update_button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { update_height(); }
                });
                c = new GridBagConstraints();
                c.gridx = 3; c.gridy = 0;
                parameter_panel.add(height_update_button, c);
                contentPane.add(parameter_panel, BorderLayout.NORTH);

                /* tags */
                contentPane.add(OBNode.setupTagPanel(nodes, this), BorderLayout.CENTER);

                /* close button */
                JPanel panel = new JPanel(new GridLayout(1, 3));
                panel.add(new JLabel());
                panel.add(new JLabel());
                JButton cancel = new JButton("Cancel");
                cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                });
                panel.add(cancel);
                contentPane.add(panel, BorderLayout.SOUTH);
                this.pack();
            }

            public void update_height() {
                for (MapNode node : nodes) {
                    if (node.selected) {
                        node.setHeight(Double.parseDouble(
                                    height_field.getText()));
                    }
                    //node.selected = false;
                }
                this.dispose();
            }
        }

        AttributeSetDialog dialog = new AttributeSetDialog(nodes);
        dialog.setVisible(true);
    }

    @Override
    public NType getNodeType() {
        return NType.NODE;
    }

    public static String getNodeTypeString() {
        return "Node";
    }

    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());

        element.setAttribute("id", "" + ID);
        element.setAttribute("x", "" + absolute_coordinates.getX());
        element.setAttribute("y", "" + absolute_coordinates.getY());
        element.setAttribute("height", "" + getHeight());
        for (MapLink link : links) {
            Element link_element = dom.createElement("link");
            link_element.setAttribute("id", "" + link.ID);
            element.appendChild(link_element);
        }

        return element;
    }

    public static MapNode fromDom(Element element) {
        int id = Integer.parseInt(element.getAttribute("id"));
        double x = Double.parseDouble(element.getAttribute("x"));
        double y = Double.parseDouble(element.getAttribute("y"));
        Point2D coordinates = new Point2D.Double(x, y);
        double height = Double.parseDouble(element.getAttribute("height"));
        MapNode node = new MapNode(id, coordinates, height);
        node.getAttributesFromDom(element);

        /* used in NetworkMap.setupNetwork */
        ArrayList<String> links = new ArrayList<String>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            if (children.item(i) instanceof Element) {
                Element child = (Element) children.item(i);
                if (child.getTagName().equals("link")) {
                    links.add(child.getAttribute("id"));
                }
            }
        }
        node.setUserObject(links);
        return node; 
    }

    @Override
    public String toString() {
        return getTagString();
    }

    @Override
    public String getHintString() {
        //return "" + getAbsoluteX() + ", " + getAbsoluteY() + ", " + getHeight();
        return getTagString();
    }
    public void setHeight(double height) {
        this.height = height;
    }
    public double getHeight() {
        return height;
    }

    /*
     * タグ一覧を返す
     *   EXITのみ           EXIT
     *   EXIT + その他      その他
     *   EXIT + その他複数  その他1|その他2|...
     */
    public String getTagLabel() {
        StringBuffer buff = new StringBuffer();
        for (final String tag : tags) {
            if (buff.length() > 0) {
                buff.append("|");
            }
            buff.append(tag);
        }
        return buff.toString();
    }

    // p は this 座標を基準とした第何象限にあるのか
    public int getQuadrant(Point2D p) {
        if (p.getY() >= getY()) {
            return p.getX() >= getX() ? 1 : 2;
        } else {
            return p.getX() >= getX() ? 4 : 3;
        }
    }

    // node 座標が this 座標を基準とした quadrant 象限にあれば true を返す
    public boolean include(MapNode node, int quadrant) {
        switch (quadrant) {
        case 1:
            return node.getX() >= getX() && node.getY() >= getY();
        case 2:
            return node.getX() < getX() && node.getY() >= getY();
        case 4:
            return node.getX() >= getX() && node.getY() < getY();
        case 3:
            return node.getX() < getX() && node.getY() < getY();
        }
        return false;   // Exception の代わり
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
