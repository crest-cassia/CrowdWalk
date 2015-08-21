// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap.Link;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBMapPart;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;


public class MapLink extends OBMapPart implements Comparable<MapLink> {

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 順・逆方向を示すもの
     */
    static public enum Direction {
        Forward(1.0),
        Backward(-1.0),
        None(0.0);

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /** 方向の値 */
        private final double _value ;

        //------------------------------
        /** コンストラクタ */
        private Direction(final double value) {
            this._value = value ;
        }

        //------------------------------
        /** 値の取得 */
        public double value() {
            return _value ;
        }

        //------------------------------
        /** 逆方向 */
        public Direction opposite() {
            switch(this) {
            case Forward : return Direction.Backward ;
            case Backward : return Direction.Forward ;
            default : return Direction.None ;
            }

        }
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Special Tags
     */
    static public final String Tag_OneWayForward = "ONE-WAY-FORWARD" ;
    static public final String Tag_OneWayBackward = "ONE-WAY-BACKWARD" ;
    static public final String Tag_RoadClosed = "ROAD-CLOSED" ;
    static public final String Tag_Stair = "STAIR" ;
    static public enum SpecialTagId { OneWayForward,
                                      OneWayBackward,
                                      RoadClosed,
                                      Stair
    } ;
    static public Lexicon tagLexicon = new Lexicon() ;
    static {
        tagLexicon.registerMulti(new Object[][]
            {{Tag_OneWayForward, SpecialTagId.OneWayForward},
             {Tag_OneWayBackward, SpecialTagId.OneWayBackward},
             {Tag_RoadClosed, SpecialTagId.RoadClosed},
             {Tag_Stair, SpecialTagId.Stair}
            }) ;
    } ;

    //============================================================
    //------------------------------------------------------------
    /**
     * 共通パラメータを、fallbackParameterから設定。
     */
    public static void setupCommonParameters(Term wholeFallbacks) {
        fallbackParameters =
            wholeFallbacks.filterArgTerm("link",
                                         SetupFileInfo.FallbackSlot) ;
        speedRestrictRule =
            fallbackParameters.fetchArgTerm("speedRestrictRule",
                                            SetupFileInfo.FallbackSlot,
                                            Term.newArrayTerm()) ;
        emptySpeedRestrictRule =
            fallbackParameters.fetchArgTerm("emptySpeedRestrictRule",
                                            SetupFileInfo.FallbackSlot,
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

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクによる自由速度に制約を加える時のルール
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
    public static Term emptySpeedRestrictRule = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    public double length;
    public double width;
    protected MapNode fromNode, toNode;

    static protected double MAX_INPUT = 1.47; 

    /* place holder for values used in simulation */
    public ArrayList<AgentBase> agents;
    private boolean nodes_are_set = false;

    //------------------------------------------------------------
    /**
     * alert message
     */

    public void addAlertMessage(Term message, SimTime currentTime,
                                boolean onoff) {
        if(onoff) {
            alertMessageTable.put(message, currentTime) ;
            for(AgentBase agent : getAgents()) {
                agent.alertMessage(message, currentTime) ;
            }
        } else {
            alertMessageTable.remove(message) ;
        }
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント生成禁止かどうかのフラグ
     */
    private boolean shutOffP = false;

    //------------------------------------------------------------
    /**
     * 生成禁止フラグの操作
     */
    public void letShutOff(boolean onoff) { shutOffP = onoff; }

    //------------------------------------------------------------
    /**
     * 生成禁止かどうかのチェック。
     */
    public boolean isShutOff() { return shutOffP; }

    // このリンクが現在 pollution level > 0.0 な MapArea 内を通っているかのフラグ
    protected boolean polluted = false;
    public void setPolluted(boolean b) { polluted = b; }
    public boolean isPolluted() { return polluted; }

    // このリンク上にかかっている MapArea のリスト
    protected ArrayList<MapArea> intersectedMapAreas = new ArrayList<MapArea>();
    public void addIntersectedMapArea(MapArea area) { intersectedMapAreas.add(area); }
    public ArrayList<MapArea> getIntersectedMapAreas() { return intersectedMapAreas; }

    /* some values used for drawing */
    public static final BasicStroke broad = new BasicStroke(9.0f);
    public static final BasicStroke narrow = new BasicStroke(5.0f);
    public static final Color LINK_RED = new Color(1.0f, 0.3f, 0.3f);
    public static final Color LIGHT_BLUE = new Color(0.4f, 0.4f, 1.0f);

    //------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------
    /**
     */
    public MapLink(String _id,
                   double _length, double _width) {
        super(_id);

        agents = new ArrayList<AgentBase>();
        length = _length;
        width = _width;

        selected = false;
    }
    /**
     */
    public MapLink(String _id, 
            MapNode _from, MapNode _to,
            double _length, double _width
            ) {
        this(_id, _length, _width);
        nodes_are_set = true;
        fromNode = _from;
        toNode = _to;
    }

    //------------------------------------------------------------
    // accessor
    //------------------------------------------------------------
    /**
     * 長さの取得
     */
    public double getLength() {
        return length ;
    }

    //------------------------------------------------------------
    /**
     * 長さの設定
     */
    public void setLength(double _length) {
        length = _length ;
    }
    //------------------------------------------------------------
    /**
     * 幅の取得
     */
    public double getWidth() {
        return width ;
    }

    //------------------------------------------------------------
    /**
     * 幅の設定
     */
    public void setWidth(double _width) {
        width = _width ;
    }

    /*public String getType() {
        return "Link";
    }*/

    public void prepareForSimulation() {
        setup_lanes();
    }

    public void prepareAdd() {
        fromNode.addLink(this);
        toNode.addLink(this);
    }

    public void prepareRemove() {
        fromNode.removeLink(this);
        toNode.removeLink(this);
    }

    public MapNode getOther(final MapNode node) {
        if (toNode == node) return fromNode;
        return toNode;
    }

    public double getAverageHeight() {
        return (fromNode.getHeight() + toNode.getHeight()) / 2.0;
    }

    public void preUpdate(SimTime currentTime) {
        if(agents.isEmpty()) return ;

        Collections.sort(agents) ;
        setup_lanes();
    }

    /**
     * リンクの処理。
     * [2015.07.04 I.Noda]
     * preUpdate と同じだが、表示のためにここでもやらないといけない。
     * これまでは、agent ごとに setup_lanes を呼んでしまっていた。
     * これを避けるためにここでまとめて処理してみる。
     * 本来は、sort すべきか？
     */
    public void update(SimTime currentTime) {
        if(agents.isEmpty()) return ;

        // Collections.sort(agents) ;
        setup_lanes();
    }

    /**
     * 真の混雑度 [2014.12.15 I.Noda]
     * たいがい 1.0 以下。
     */
    public double realCrowdness() {
        return agents.size() / width / length;
    }

    /**
     * 混雑度の元の定義 [2014.12.15 I.Noda]
     */
    public double crowdness() {
        double phi = realCrowdness() ;
        
        if (phi < 1.0) return 1.0;
        return phi;
    }
    
    public boolean isBetweenHeight(double min_h, double max_h) {
        if (!getFrom().isBetweenHeight(min_h, max_h)) return false;
        if (!getTo().isBetweenHeight(min_h, max_h)) return false;
        return true;
    }

    private ArrayList<AgentBase> forwardLane =
        new ArrayList<AgentBase>() ;
    private ArrayList<AgentBase> backwardLane =
        new ArrayList<AgentBase>() ;
    public void setup_lanes() {
        forwardLane.clear() ;
        backwardLane.clear() ;
        for (AgentBase agent : agents) {
            if (agent.isForwardDirection()) {
                forwardLane.add(agent);
            } else {
                backwardLane.add(agent);
            }
        }
        /* [2015.01.03 I.Noda]
         * 以下の sort は、agents がすでに sort されているので無駄なはず。
         * なので排除。
         */
        //Collections.sort(forwardLane);
        //Collections.sort(backwardLane);
    }

    private void add_agent_to_lane(AgentBase agent) {
        if (agent.isForwardDirection()) {
            forwardLane.add(agent);
            Collections.sort(forwardLane);    
        } else {
            backwardLane.add(agent);
            Collections.sort(backwardLane);
        }
    }

    public ArrayList<AgentBase> getLane(Direction dir) {
        switch(dir) {
        case Forward : return forwardLane;
        case Backward : return backwardLane ;
        default : return null ;
        }
    }

    public int getLaneWidth(Direction dir) {
        int d = getLane(dir).size() ;
        int lane_width = (int)(d * width / (forwardLane.size() +
                    backwardLane.size()));
        if (lane_width == 0) {
            lane_width = 1;
        }
        return lane_width ;
    }

    public void drawInEditor(Graphics2D g,
                             boolean show_label,
                             boolean isSymbolic,
                             boolean showScaling){
        //float fwidth = (float)(width / ((MapPartGroup)(parent)).getScale());
        //g.setStroke(new BasicStroke((float)width));
        double scale = 1.0;
        if (!showScaling)
            scale = g.getTransform().getScaleX();
        g.setStroke(new BasicStroke(2.0f / ((float)scale)));
        boolean oneWayForward = this.isOneWayForward() ;
        boolean oneWayBackward = this.isOneWayBackward() ;
        boolean roadClosed = this.isRoadClosed() ;


        if (isSymbolic) {
            g.setColor(Color.GRAY);
            if (oneWayForward)
                g.fill(getArrow(0.0, scale, true));
            else if (oneWayBackward)
                g.fill(getArrow(0.0, scale, false));
            else
                g.fill(getRect(0.0, scale, false));
            g.setColor(Color.YELLOW);
            if (oneWayForward)
                g.draw(getArrow(0.0, scale, true));
            else if (oneWayBackward)
                g.draw(getArrow(0.0, scale, false));
            else
                g.draw(getRect(0.0, scale, false));
        } else if (selected) {
            g.setColor(Color.BLACK);
            if (oneWayForward)
                g.fill(getArrow(0.0, scale, true));
            else if (oneWayBackward)
                g.fill(getArrow(0.0, scale, false));
            else
                g.fill(getRect(0.0, scale, false));
            g.setColor(Color.RED);
            if (oneWayForward)
                g.draw(getArrow(0.0, scale, true));
            else if (oneWayBackward)
                g.draw(getArrow(0.0, scale, false));
            else
                g.draw(getRect(0.0, scale, false));
        } else if (width == 0) {
            g.setColor(Color.YELLOW);
            if (oneWayForward)
                g.fill(getArrow(0.0, scale, true));
            else if (oneWayBackward)
                g.fill(getArrow(0.0, scale, false));
            else
                g.fill(getRect(0.0, scale, false));
        } else {
            if (oneWayForward) {
                g.setColor(Color.MAGENTA);
                g.fill(getArrow(0.0, scale, true));
            } else if (oneWayBackward) {
                g.setColor(LIGHT_BLUE);
                g.fill(getArrow(0.0, scale, false));
            } else if (roadClosed) {
                g.setColor(LINK_RED);
                g.fill(getRect(0.0, scale, false));
            } else {
                g.fill(getRect(0.0, scale, false));
            }
            g.setColor(Color.BLACK);
            if (oneWayForward)
                g.draw(getArrow(0.0, scale, true));
            else if (oneWayBackward)
                g.draw(getArrow(0.0, scale, false));
            else
                g.draw(getRect(0.0, scale, false));
        }
        if (show_label) {
            g.drawString(getTagString(),
                     (float)calcAgentPos(0.5).getX(),
                     (float)calcAgentPos(0.5).getY());
        }
    }

    public void drawLabel(Graphics2D g, boolean showScaling) {
        double scale = showScaling ? g.getTransform().getScaleX() : 1.0;
        g.setStroke(new BasicStroke(2.0f / (float)scale));
        g.drawString(getTagString(), (float)calcAgentPos(0.5).getX(), (float)calcAgentPos(0.5).getY());
    }

    public Color getColorFromDensity() {
        double d = getAgents().size() / (width * length);
        float f;
        if (d >= 1) {
            f = 0.0f;
        } else {
            f = (1.0f - (float)d) / 2.0f * 0.65f;
        }
        if (d == 0) return Color.WHITE;
        return new Color(Color.HSBtoRGB(f, 0.8f, 0.8f ));
    }

    public Point2D calcAgentPos(double position) {
        double x = fromNode.getX() + (toNode.getX() - fromNode.getX()) * 
            position / length;
        double y = fromNode.getY() + (toNode.getY() - fromNode.getY()) * 
            position / length;

        return new Point2D.Double(x,y);
    }

    public double calcAgentHeight(double position) {
        return fromNode.getHeight() + (toNode.getHeight() - fromNode
                .getHeight()) * position / length;
    }

    public void agentEnters(AgentBase agent) {
        agents.add(agent);
        /* [2015.01.09 I.Noda]
         * 以下の処理はおそらく無駄。
         * しかもいちいち sort しているので、計算時間の無駄のはず。
        if (agent.isForwardDirection() || agent.isBackwardDirection()) {
            add_agent_to_lane(agent);
        }
        */

        /* alert message を新しいエージェントに伝える */
        for(HashMap.Entry<Term, SimTime> entry : alertMessageTable.entrySet()) {
            agent.alertMessage(entry.getKey(), entry.getValue()) ;
        }
    }

    public void agentExits (AgentBase agent) {
        assert(agents.contains(agent));
        agents.remove(agent);
        if (! forwardLane.remove(agent)) {
            backwardLane.remove(agent);
        }
    }

    public void setFromTo(MapNode from, MapNode to) throws Exception {
        /* should only be called on setup */
        if (nodes_are_set) {
            throw new Exception("From and To already set.");
        }
        fromNode = from;
        toNode = to;
        nodes_are_set = true;
    }

    public MapNode getFrom() {
        return fromNode;
    }

    public MapNode getTo() {
        return toNode;
    }

    public Boolean equals(MapLink rhs){
        return hasSameEndsForward(rhs) || hasSameEndsBackward(rhs) ;
    }

    //------------------------------------------------------------
    /**
     * 同じ終端ノードを持っているか (forward)
     */
    public boolean hasSameEndsForward(MapLink otherLink) {
        return (fromNode == otherLink.fromNode && toNode == otherLink.toNode) ;
    }

    //------------------------------------------------------------
    /**
     * 同じ終端ノードを持っているか (backward)
     */
    public boolean hasSameEndsBackward(MapLink otherLink) {
        return (toNode == otherLink.fromNode && fromNode == otherLink.toNode) ;
    }

    public void clear() {
        agents.clear();
    }

    public GeneralPath getRect(double border, double scale,
            boolean in_simulation) {
        double x1 = fromNode.getX();
        double y1 = fromNode.getY();
        double x2 = toNode.getX();
        double y2 = toNode.getY();
        //double fwidth = width / (((MapPartGroup)(parent)).getScale());
        //double fwidth = width;
        //fwidth +=  border;
        //double fwidth = 2.0 / (((MapPartGroup)(parent)).getScale());
        double fwidth = 4.0 / scale;
        //double fwidth = 4.0;

        double dx = (x2 - x1);
        double dy = (y2 - y1);
        double a = Math.sqrt(dx*dx + dy*dy);

        double edx = fwidth * dx / a / 2;
        double edy = fwidth * dy / a / 2;
        //double edx = fwidth / Math.sqrt(2.0);
        //double edy = fwidth / Math.sqrt(2.0);

        GeneralPath p = new GeneralPath();
        p.moveTo(x1 - edy, y1 + edx);
        p.lineTo(x1 + edy, y1 - edx);
        p.lineTo(x2 + edy, y2 - edx);
        p.lineTo(x2 - edy, y2 + edx);
        p.lineTo(x1 - edy, y1 + edx);
        p.closePath();

        return p;
    }

    // create arrow polygon consist of seven coordinates. forward means the 
    // direction of arrow: true(from-to), false(to-from)
    public GeneralPath getArrow(double border, double scale,
            boolean forward) {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 0.0;
        double y2 = 0.0;
        if (forward) {
            x1 = getFrom().getX();
            y1 = getFrom().getY();
            x2 = getTo().getX();
            y2 = getTo().getY();
        } else {
            x1 = getTo().getX();
            y1 = getTo().getY();
            x2 = getFrom().getX();
            y2 = getFrom().getY();
        }
        //double fwidth = width / (((MapPartGroup)(parent)).getScale());
        //double fwidth = width;
        double fwidth = 4.0 / scale;
        fwidth +=  border;

        double dx = (x2 - x1);
        double dy = (y2 - y1);
        double a = Math.sqrt(dx*dx + dy*dy);

        double edx = fwidth * dx / a / 2;
        double edy = fwidth * dy / a / 2;

        double arrowHeight = 8.0 / scale;
        double arrowWidth = 8.0 / scale;
        double bx = x2 - arrowHeight * dx / a;
        double by = y2 - arrowHeight * dy / a;
        double ax = arrowWidth * dy / a /2;
        double ay = arrowWidth * dx / a /2;
        //double bx = x1 + (a - arrowHeight * fwidth) / a * (x2 - x1);
        //double by = y1 + (a - arrowHeight * fwidth) / a * (y2 - y1);

        GeneralPath p = new GeneralPath();
        p.moveTo(x1 - edy, y1 + edx);
        p.lineTo(x1 + edy, y1 - edx);
        p.lineTo(bx + edy, by - edx);
        p.lineTo(bx + edy + ax, by - edx - ay);
        p.lineTo(x2, y2);
        p.lineTo(bx - edy - ax, by + edx + ay);
        p.lineTo(bx - edy, by + edx);
        p.lineTo(x1 - edy, y1 + edx);
        p.closePath();

        return p;
    }
/*
    public Line2D getLine2D() {
        Point2D from = fromNode.getAbsoluteCoordinates();
        Point2D to = toNode.getAbsoluteCoordinates();
        double fwidth = width / (((MapPartGroup)(parent)).getScale());
        if (from.getX() == to.getX()) {
            double y1 = from.getY();
            double y2 = to.getY();
            if (y2 > y1) {
                from = new Point2D.Double(from.getX(), y1 + fwidth);
                to = new Point2D.Double(to.getX(), y2 - fwidth);
            } else {
                from = new Point2D.Double(from.getX(), y1 - fwidth);
                to = new Point2D.Double(to.getX(), y2 + fwidth);
            }
            return new Line2D.Double(from, to);
        } else {
            double x1 = from.getX();
            double x2 = to.getX();
            double y1 = from.getY();
            double y2 = to.getY();
            double dx = (x2 - x1);
            double dy = (y2 - y1);
            double a = Math.sqrt(dx*dx + dy*dy);

            double edx = fwidth * dx / a / 2;
            double edy = fwidth * dy / a / 2;

            x1 += edx;
            y1 += edy;
            x2 -= edx;
            y2 -= edy;

            return new Line2D.Double(x1, y1, x2, y2);
        }
    }
*/
    // 上のメソッドは使われていなかったのでコメントにした(斉藤)
    public Line2D getLine2D() {
        return new Line2D.Double(fromNode.getX(), fromNode.getY(), toNode.getX(), toNode.getY());
    }

    public static class AttributePanel  extends JPanel 
    implements ActionListener, ChangeListener {
        private JSpinner length, width;
        private boolean detectChange = true;
        static public interface Listener {
            public abstract void valueChanged();
        }
        ArrayList<Listener> listeners = new ArrayList<Listener>();

        public AttributePanel() {
            /* labels and text fields */
            setLayout(new GridLayout(2, 2));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    "line attributes"));

            add(new JLabel("Length:"));
            length = new JSpinner(
                    new SpinnerNumberModel(1.0, 0.0, 10000.0, 0.1));
            length.addChangeListener(this);
            add(length);

            add(new JLabel("Width:"));
            width = new JSpinner(
                    new SpinnerNumberModel(1.0, 0.0, 10000.0, 0.1));
            width.addChangeListener(this);
            add(width);
        }

        public void setLinkLength(double l) {
            length.setValue(new Double(l));
        }
        public double getLinkLength() {
            return ((Double)(length.getValue())).doubleValue();
        }
        public void setLinkWidth(double w) {
            width.setValue(new Double(w));
        }
        public double getLinkWidth() {
            return ((Double)(width.getValue())).doubleValue();
        }
        public void setLengthEnabled(boolean b) {
            length.setEnabled(b);
        }

        public void addListner(Listener l) {
            listeners.add(l);
        }

        public void setDetectChange(boolean b) {
            detectChange = b;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!detectChange) return;
            for (Listener l : listeners) {
                l.valueChanged();
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!detectChange) return;
            for (Listener l : listeners) {
                l.valueChanged();
            }
        }

    }

    public static AttributePanel getAttributePanel() {
        return new MapLink.AttributePanel();
    }

    public static void showAttributeDialog(MapLinkTable links) {
        /* Set attributes with a dialog */
        class AttributeSetDialog extends JDialog  
        implements ActionListener {
            private MapNode from = null;
            private MapNode to = null;

            private MapLinkTable links = null;
            AttributePanel attributes = null;  
            private Boolean single;

            public AttributeSetDialog(MapLinkTable _links) {
                super();

                this.setModal(true);
                links = _links;

                double length = 0.0, width = 0.0;
                int count = 0;
                for (MapLink link : links) {
                    if (link.selected) {
                        length += link.length;
                        width += link.width;
                        count++;
                        from = link.getFrom();
                        to = link.getTo();
                    }
                }
                single = (count == 1);
                length /= count;
                width /= count;
                setup_panel();
                attributes.setLinkLength(length);
                attributes.setLinkWidth(width);
            }

            private void setup_panel() {
                Container contentPane = getContentPane();
                contentPane.setLayout(new BorderLayout());

                JPanel length_panel = new JPanel(new BorderLayout());
                attributes = new AttributePanel();
                length_panel.add(attributes, BorderLayout.CENTER);

                /* Buttons */
                JPanel button_panel = new JPanel(new GridBagLayout());

                /* calculate length button */
                JButton calc = new JButton("Calc length from scale");
                calc.addActionListener(this);
                calc.setEnabled(single);
                GridBagConstraints c;
                c = new GridBagConstraints();
                button_panel.add(calc, c);

                c = new GridBagConstraints();
                c.gridx = 2;
                button_panel.add(new JLabel(" "));

                /* ok and cancel button */
                JButton ok = new JButton("OK");
                ok.addActionListener(this);
                c = new GridBagConstraints();
                c.gridx = 3;
                button_panel.add(ok, c);
                JButton cancel_button = new JButton("Cancel");
                cancel_button.addActionListener(this);
                c = new GridBagConstraints();
                c.gridx = 4;
                button_panel.add(cancel_button, c);
                length_panel.add(button_panel, BorderLayout.SOUTH);
                contentPane.add(length_panel, BorderLayout.NORTH);

                /* tags */
                contentPane.add(OBNode.setupTagPanel(links, this), 
                        BorderLayout.CENTER);

                this.pack();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    for (MapLink link : links) {
                        if (!link.selected) continue; 
                        link.length = attributes.getLinkLength();
                        link.width = attributes.getLinkWidth();
                    }
                    dispose();
                } else if (e.getActionCommand().equals("Cancel")) {
                    dispose();
                } else if (e.getActionCommand().
                        equals("Calc length from scale")) {
                    attributes.setLinkLength(from.getAbsoluteCoordinates()
                            .distance(to.getAbsoluteCoordinates()));
                    repaint();
                }
            }
        }

        AttributeSetDialog dialog = new AttributeSetDialog(links);
        dialog.setLocation(MouseInfo.getPointerInfo().getLocation());
        dialog.setVisible(true);
    }

    public ArrayList<AgentBase> getAgents() {
        return agents;
    }

    @Override
    public NType getNodeType() {
        return NType.LINK;
    }

    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());
        element.setAttribute("id", ID);
        element.setAttribute("length", "" + length);
        element.setAttribute("width", "" + width);
        element.setAttribute("from", fromNode.ID);
        element.setAttribute("to", toNode.ID);
        return element;
    }
    
    public static String getNodeTypeString() {
        return "Link";
    }

    public static MapLink fromDom(Element element) {
        String id = element.getAttribute("id");
        double length = Double.parseDouble(element.getAttribute("length"));
        double width = Double.parseDouble(element.getAttribute("width"));
        String nodes[] = new String[2];
        nodes[0] = element.getAttribute("from");
        nodes[1] =element.getAttribute("to");

        /* used in NetworkMap.setupNetwork */
        MapLink link = new MapLink(id, length, width);
        link.getAttributesFromDom(element);

        link.setUserObject(nodes);
        return link;
    }

    /**
     * タグをクリア。
     * ノードのcacheもクリア。
     */
    @Override
    public void allTagsClear() {
        boolean tagsIsEmpty = tags.isEmpty();
        super.allTagsClear() ;
        clearCacheInNodes() ;
        if (! tagsIsEmpty && networkMap != null) {
            networkMap.getNotifier().linkTagRemoved(this);
        }
    }

    /**
     * タグを追加。
     * ノードのcacheもクリア。
     */
    @Override
    public boolean addTag(String _tag) {
        boolean result = super.addTag(_tag) ;
        if (result) {
            clearCacheInNodes() ;
            if (networkMap != null) {
                networkMap.getNotifier().linkTagAdded(this, _tag);
            }
        }
        return result ;
    }

    /**
     * タグを削除。
     * ノードのcacheもクリア。
     */
    @Override
    public void removeTag(String _tag) {
        int tagsSize = tags.size();
        super.removeTag(_tag) ;
        clearCacheInNodes() ;
        if (networkMap != null && tags.size() < tagsSize) {
            networkMap.getNotifier().linkTagRemoved(this);
        }
    }

    /**
     * ノードのcacheをクリア。
     */
    private void clearCacheInNodes() {
        if(fromNode != null) fromNode.clearUsableLinkTableCache() ;
        if(toNode != null) toNode.clearUsableLinkTableCache() ;
    }

    public String toString() {
        return getTagString();
    }

    //------------------------------------------------------------
    /**
     * リンクの情報を短く示す文字列。
     * @return 文字列
     */
    public String toShortInfo() {
        return ("Link[" + ID
                + ",tag:" + getTags()
                + ",from:" + (fromNode == null ?
                              "(null)" : String.valueOf(fromNode.ID))
                + ",to:" + (toNode == null ?
                            "(null)" : String.valueOf(toNode.ID))
                + "]" ) ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードから見て正方向かどうかのチェック
     * @param originNode エージェントが入る側のノード
     * @return originNode が fromNode と同じなら true
     */
    public boolean isForwardDirectionFrom(MapNode originNode) {
        return originNode == getFrom() ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て正方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @return destinationNode が toNode と同じなら true
     */
    public boolean isForwardDirectionTo(MapNode destinationNode) {
        return destinationNode == getTo() ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードから見た時の direction の値
     * @param originNode エージェントが入る側のノード
     * @return 順方向なら Direction.Forward。逆なら Direction.Backward。
     */
    public Direction directionValueFrom(MapNode originNode) {
        return (isForwardDirectionFrom(originNode) ?
                Direction.Forward :
                Direction.Backward) ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て正方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @return 順方向なら Direction.Forward。逆なら Direction.Backward。
     */
    public Direction directionValueTo(MapNode destinationNode) {
        return (isForwardDirectionTo(destinationNode) ?
                Direction.Forward :
                Direction.Backward) ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードから見た時の direction の値
     * @param originNode エージェントが入る側のノード
     * @param relativePos エージェント進行方向から見た位置
     * @return 順方向なら relativePos。逆なら length-relativePos。
     */
    public double calcAbstractPositionByDirectionFrom(MapNode originNode,
                                                      double relativePos) {
        return (isForwardDirectionFrom(originNode) ?
                relativePos :
                length - relativePos) ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て正方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @param relativePos エージェント進行方向から見た位置
     * @return 順方向なら relativePos。逆なら length-relativePos。
     */
    public double calcAbstractPositionByDirectionTo(MapNode destinationNode,
                                                    double relativePos) {
        return (isForwardDirectionTo(destinationNode) ?
                relativePos :
                length - relativePos) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (forward) のチェック
     * @return OneWayForward なら true
     */
    public boolean isOneWayForward() {
        return hasTag(Tag_OneWayForward) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (forward) にする。
     * @return 変化があったなら true
     */
    public boolean setOneWayForward() {
        return setOneWayForward(true) ;
    }
    //------------------------------------------------------------
    /**
     * OneWay (forward) にする。
     * @param flag もし false なら OneWay 解除。
     * @return 変化があったなら true
     */
    public boolean setOneWayForward(boolean flag) {
        return setSpecialTag(Tag_OneWayForward, flag) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (backward) のチェック
     * @return OneWayBackward なら true
     */
    public boolean isOneWayBackward() {
        return hasTag(Tag_OneWayBackward) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (backward) にする。
     * @return 変化があったなら true
     */
    public boolean setOneWayBackward() {
        return setOneWayBackward(true) ;
    }
    //------------------------------------------------------------
    /**
     * OneWay (backward) にする。
     * @return 変化があったなら true
     */
    public boolean setOneWayBackward(boolean flag) {
        return setSpecialTag(Tag_OneWayBackward, flag) ;
    }

    //------------------------------------------------------------
    /**
     * RoadClosed のチェック
     * @return RoadClosed なら true
     */
    public boolean isRoadClosed() {
        return hasTag(Tag_RoadClosed) ;
    }

    //------------------------------------------------------------
    /**
     * RoadClosed にする。
     * @return 変化があったなら true
     */
    public boolean setRoadClosed() {
        return setRoadClosed(true) ;
    }
    //------------------------------------------------------------
    /**
     * RoadClosed にする。
     * @return 変化があったなら true
     */
    public boolean setRoadClosed(boolean flag) {
        return setSpecialTag(Tag_RoadClosed, flag) ;
    }
    //------------------------------------------------------------
    /**
     * Special Tag の set/reset
     * @param specialTag 指定するタグ
     * @param flag set/reset を指定。
     * @return 変化があったなら true
     */
    public boolean setSpecialTag(String specialTag, boolean flag) {
        if(hasTag(specialTag)) {
            if(flag) {
                return false ;
            } else {
                removeTag(specialTag) ;
                return true ;
            }
        } else {
            if(flag) {
                addTag(specialTag) ;
                return true ;
            } else {
                return false ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 交通規制処理
     * @param agent: 規制を加えるエージェント。リンク上にいる。
     * @param currentTime: 現在時刻
     * @return 規制が適用されたら true
     */
    public double calcRestrictedSpeed(double speed, AgentBase agent,
                                      SimTime currentTime) {
        /* 分断制御 */
        if(isGateClosed(agent, currentTime)) {
            speed = 0 ;
        }

        /* 制約ルール適用 */
        for(int i = 0 ; i < speedRestrictRule.getArraySize() ; i++) {
            Term rule = speedRestrictRule.getNthTerm(i) ;
            speed = applyRestrictionRule(speed, rule, agent, currentTime) ;
        }

        return speed ;
    }

    //------------------------------------------------------------
    /**
     * そのリンクにおける自由速度
     * @param emptySpeed: 元になるスピード
     * @param agent: 対象となるエージェント。リンク上にいる。
     * @param currentTime: 現在時刻
     * @return 自由速度
     */
    public double calcEmptySpeedForAgent(double emptySpeed,
                                         AgentBase agent,
                                         SimTime currentTime) {
        /* 制約ルール適用 */
        for(int i = 0 ; i < emptySpeedRestrictRule.getArraySize() ; i++) {
            Term rule = emptySpeedRestrictRule.getNthTerm(i) ;
            emptySpeed = applyRestrictionRule(emptySpeed, rule, agent,
                                              currentTime) ;
        }

        return emptySpeed ;
    }

    public int compareTo(MapLink link) {
        return ID.compareTo(link.ID);
    }
}
