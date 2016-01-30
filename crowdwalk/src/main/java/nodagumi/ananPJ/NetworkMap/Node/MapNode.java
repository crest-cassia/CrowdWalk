// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap.Node;

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

import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBMapPart;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;

public class MapNode extends OBMapPart {
    //============================================================
    //------------------------------------------------------------
    /**
     * 共通パラメータを、fallbackParameterから設定。
     */
    public static void setupCommonParameters(Term wholeFallbacks) {
        fallbackParameters =
            SetupFileInfo.filterFallbackTerm(wholeFallbacks, "link") ;
        speedRestrictRule =
            SetupFileInfo.fetchFallbackTerm(fallbackParameters,
                                            "speedRestrictRule",
                                            Term.newArrayTerm()) ;
    } ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンク用の fallback パラメータ
     */
    public static Term fallbackParameters = null ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクによる速度の制約計算で用いるルール。
     * Array 型 Term の形で格納される。
     * Array の要素は、ルールを示す ObjectTerm。
     * 以下は例。詳細は、
     * {@link nodagumi.ananPJ.NetworkMap.OBMapPart#applyRestrictionRule applyRestrictionRule} 
     * を参照。
     * <pre>
     *  [ { "type" : "multiply",
     *      "tag" : "FOO",
     *      "factor" : 0.5 },
     *    { "type" : "multiply",
     *      "tag" : "BAR",
     *      "factor" : 0.9 } ]
     * </pre>
     */
    public static Term speedRestrictRule = null ;

    /* global coordinates */
    private Point2D absolute_coordinates;
    private double height;

    private MapLinkTable links;

    /* used in simulation */
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

    public MapNode(String _ID,
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
        clearUsableLinkTableCache() ;
        return true;
    }

    public boolean removeLink(MapLink link) {
        if (!links.contains(link)) return false;
        links.remove(link);
        clearUsableLinkTableCache() ;
        return true;
    }

    public MapLinkTable getLinks() {
        return links;
    }

    //------------------------------------------------------------
    /**
     * 一方通行を考慮して、つながっているリンクを集める。
     */
    public MapLinkTable getUsableLinkTable() {
        if(usableLinkTableCache == null) { // cacheがクリアされていれば作成。
            usableLinkTableCache = new MapLinkTable();
            for (MapLink link : links) {
                if (link.isAvailableFrom(this)) {
                    usableLinkTableCache.add(link);
                }
            }
        }
        return usableLinkTableCache ;
    }

    /**
     * getUsableLinkTable を効率化するための cache
     * Node とそれに繋がるリンクのタグなどが変化すれば、
     * reset されなければならない。
     * その際には、resetUsableLinkTable() を呼ぶこと。
     */
    private MapLinkTable usableLinkTableCache = null ;

    //------------------------------------------------------------
    /**
     * 一方通行を考慮して、つながっているリンクを集める。
     * Dijkstra で使われるだけなので、cache しない。
     */
    public MapLinkTable getValidReverseLinkTable () {
        MapLinkTable usableReverseLinkTable = new MapLinkTable();
        for (MapLink link : links) {
            if (!((link.isOneWayForward() && link.getFrom() == this) ||
                  (link.isOneWayBackward() && link.getTo() == this) ||
                  (link.isRoadClosed()))) {
                usableReverseLinkTable.add(link);
            }
        }
        return usableReverseLinkTable ;
    }

    //------------------------------------------------------------
    /**
     * getUsableLinkTable を効率化するための cache を開放する。
     */
    public void clearUsableLinkTableCache() {
        usableLinkTableCache = null ;
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

    public MapLink getViaLink(String key) {
        NavigationHint hint = getHint(key);
        return hint.viaLink;
    }

    public double getDistance(Term target) throws TargetNotFoundException {
        String key = target.getString() ;
        NavigationHint hint = getHint(key);
        if (hint == null) {
            if(hasTag(key)) { // 自分自身がターゲットの場合
                // do nothing
            } else { // target の情報が見つからない場合。
                Itk.logWarn("Target Not Found", "target:", key) ;
                throw new TargetNotFoundException(key + " not found for id=" + ID + "(" + getTagString() + ")");
            }
            return 0.0 ;
        } else {
            return hint.distance;
        }
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

    /**
     * タグをクリア。
     */
    @Override
    public void allTagsClear() {
        boolean tagsIsEmpty = tags.isEmpty();
        super.allTagsClear() ;
        if (! tagsIsEmpty && networkMap != null) {
            networkMap.getNotifier().nodeTagRemoved(this);
        }
    }

    /**
     * タグを追加。
     */
    @Override
    public boolean addTag(String _tag) {
        boolean result = super.addTag(_tag) ;
        if (result && networkMap != null) {
            networkMap.getNotifier().nodeTagAdded(this, _tag);
        }
        return result ;
    }

    /**
     * タグを削除。
     */
    @Override
    public void removeTag(String _tag) {
        int tagsSize = tags.size();
        super.removeTag(_tag) ;
        if (networkMap != null && tags.size() < tagsSize) {
            networkMap.getNotifier().nodeTagRemoved(this);
        }
    }

    private Rectangle2D getSquare(double cx, double cy, double l, double scale) {
        return new Rectangle2D.Double(getX() - l / scale,
                getY() - l / scale,
                l * 2 / scale,
                l * 2 / scale);

    }

    public void drawInEditor(Graphics2D g,
                             boolean showLabel,
                             boolean isSymbolic) {
        Color c = null;

        if (isSymbolic)
            c = Color.GRAY;
        else
            c = Color.BLUE;

        drawInEditor(g, showLabel, isSymbolic, c);
    }

    public void drawInEditor(Graphics2D g,
                             boolean showLabel,
                             boolean isSymbolic,
                             Color c) {
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

        element.setAttribute("id", ID);
        element.setAttribute("x", "" + absolute_coordinates.getX());
        element.setAttribute("y", "" + absolute_coordinates.getY());
        element.setAttribute("height", "" + getHeight());
        for (MapLink link : links) {
            Element link_element = dom.createElement("link");
            link_element.setAttribute("id", link.ID);
            element.appendChild(link_element);
        }

        return element;
    }

    public static MapNode fromDom(Element element) {
        String id = element.getAttribute("id");
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

    //------------------------------------------------------------
    /**
     * ノードの情報を短く示す文字列。
     * @return 文字列
     */
    public String toShortInfo() {
        return ("Node[" + ID
                + ",tag:" + getTags()
                + "]" ) ;
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

    //------------------------------------------------------------
    /**
     * 交通規制処理
     * @param speed: 速度。
     * @param agent: 規制を加えるエージェント。この単位時間にノードにたどり着く。
     * @param currentTime: 現在時刻
     * @return 規制が適用されたら true
     */
    public double calcRestrictedSpeed(double speed, AgentBase agent,
                                      SimTime currentTime) {
        /* 分担制御 */
        if(isGateClosed(agent, currentTime)) {
            speed = 0.0 ;
        }

        /* 制約ルール適用 */
        for(int i = 0 ; i < speedRestrictRule.getArraySize() ; i++) {
            Term rule = speedRestrictRule.getNthTerm(i) ;
            speed = applyRestrictionRule(speed, rule, agent, currentTime) ;
        }

        return speed ;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
