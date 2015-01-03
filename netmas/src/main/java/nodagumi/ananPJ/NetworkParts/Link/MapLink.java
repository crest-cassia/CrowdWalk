// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts.Link;

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
import java.io.Serializable;
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

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBMapPart;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.misc.SpecialTerm;

import nodagumi.Itk.*;


public class MapLink extends OBMapPart implements Serializable {
    private static final long serialVersionUID = 4960899670982905174L;

    public double length;
    public double width;
    protected MapNode fromNode, toNode;

    static protected double MAX_INPUT = 1.47; 

    /* place holder for values used in simulation */
    protected ArrayList<EvacuationAgent> agents;
    public int displayMode = 0;
    protected double timeScale = 1.0;
    private boolean nodes_are_set = false;

    /* for simulation*/
    private boolean emergency = false;
    public void setEmergency(boolean b) { 
        emergency = b;
        if (!b) return;

        for (EvacuationAgent agent : getAgents()) {
            RunningAroundPerson rp = (RunningAroundPerson)agent;
            rp.setGoal(SpecialTerm.Emergency);
            rp.renavigate();
        }
    }
    public boolean getEmergency() { return emergency; }

    private boolean stop = false;
    public void setStop(boolean b) { stop = b; }
    public boolean getStop() { return stop; }

    // このリンクが現在 pollution level > 0.0 な PollutedArea 内を通っているかのフラグ
    protected boolean polluted = false;
    public void setPolluted(boolean b) { polluted = b; }
    public boolean isPolluted() { return polluted; }

    // このリンク上にかかっている PollutedArea のリスト
    protected ArrayList<PollutedArea> intersectedPollutionAreas = new ArrayList<PollutedArea>();
    public void addIntersectedPollutionArea(PollutedArea area) { intersectedPollutionAreas.add(area); }
    public ArrayList<PollutedArea> getIntersectedPollutionAreas() { return intersectedPollutionAreas; }

    /* some values used for drawing */
    public static final BasicStroke broad = new BasicStroke(9.0f);
    public static final BasicStroke narrow = new BasicStroke(5.0f);
    public static final Color LINK_RED = new Color(1.0f, 0.3f, 0.3f);
    public static final Color LIGHT_BLUE = new Color(0.4f, 0.4f, 1.0f);

    /* Constructors */
    public MapLink(int _id,
            double _length, double _width) {
        super(_id);

        agents = new ArrayList<EvacuationAgent>();
        length = _length;
        width = _width;

        selected = false;
    }
    public MapLink(int _id, 
            MapNode _from, MapNode _to,
            double _length, double _width
            ) {
        this(_id, _length, _width);
        nodes_are_set = true;
        fromNode = _from;
        toNode = _to;
    }

    /*public String getType() {
        return "Link";
    }*/

    public void prepareForSimulation(double _timeScale, int _displayMode) {
        timeScale = _timeScale;
        displayMode = _displayMode;
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

    class QueingAgent {
        public EvacuationAgent agent;
        public MapLink fromLink;

        public QueingAgent(EvacuationAgent _agent,
                MapLink _from_link) {
            agent = _agent;
            fromLink = _from_link;
        }
    }
    private ArrayList<QueingAgent> enteringQue =
        new ArrayList<QueingAgent>();
    private HashMap<MapLink, Double> fromLinksCapacity =
        new HashMap<MapLink, Double>();

    /* calculation of agent passing doors */
    public void registerEnter(EvacuationAgent agent,
            MapLink from_link) {
        enteringQue.add(new QueingAgent(agent, from_link));
    }

    private void update_from_links_capacity() {
        /* See if agents can enter the link */
        if (enteringQue.size() == 0) {
            return;
        }

        /* first, count number of agents from each from-link */
        HashMap<MapLink, Integer> fromLinks =
            new HashMap<MapLink, Integer>();
        for (QueingAgent agent : enteringQue) {
            MapLink from = agent.fromLink;
            if (!fromLinks.containsKey(from)) {
                fromLinks.put(from, 1);
            } else {
                int from_count = fromLinks.get(from) + 1;
                if (from_count > from.width) from_count = (int)from.width;
                fromLinks.put(from, from_count);
            }
        }

        if (fromLinks.size() == 0) {
            return;
        }

        /* then the calculation:
         */
        int total = 0;
        for (MapLink from : fromLinks.keySet()) {
            total += fromLinks.get(from);
        }

        double all_exit_capacity = MAX_INPUT * width * timeScale;
        for (MapLink from : fromLinks.keySet()) {
            double  capacity_for_link = all_exit_capacity * 
                fromLinks.get(from) / total;
            if (!fromLinksCapacity.containsKey(from)) {
                fromLinksCapacity.put(from, capacity_for_link); 
            } else {
                fromLinksCapacity.put(from,
                        fromLinksCapacity.get(from) + capacity_for_link);
            }
        }
    }

    // agent の絶対 position によるソート用
    private Comparator<EvacuationAgent> absoluteComparator = new Comparator<EvacuationAgent>() {
        public int compare(EvacuationAgent agent1, EvacuationAgent agent2) {
            double position1 = agent1.absolutePosition();
            double position2 = agent2.absolutePosition();

            if (position1 == position2) {
                // position が同じなら agentNumber が小さい順にする
                if (agent1.getAgentNumber() == agent2.getAgentNumber()) {
                    return 0;
                } else if (agent1.getAgentNumber() > agent2.getAgentNumber()) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (position1 > position2) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    private int total_agent_triage_level;
    public void preUpdate(double time) {
        //Collections.sort(agents);
        Collections.sort(agents, absoluteComparator);
        setup_lanes();
        /* calculate the  total triage level */
        total_agent_triage_level = 0; 
        for (EvacuationAgent agent : agents) {
            total_agent_triage_level += agent.getTriage();
        }
    }

    public void update(double time) {
        update_from_links_capacity();
        enteringQue.clear();
    }

    public int getTotalTriageLevel() {
        return total_agent_triage_level;
    }

    public boolean spaceLeft(MapNode node, double space_required) {
        if (agents.size() == 0) return true;

        double clearance = length;
        int w = (int)width;
        if (w == 0) w = 1;
        if (isForwardDirectionFrom(node)) {
            int index = w;
            if (index < getLane(1.0).size()) {
                clearance = getLane(1.0).get(index).getPosition();
            }
        } else {
            int index = getLane(-1.0).size() - w;
            if (index > 0) {
                clearance = length - getLane(-1.0).get(index).getPosition();
            }
        }

        return clearance >= space_required;
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
    
    public boolean isStair() {
        return (fromNode.getHeight() != toNode.getHeight());
    }
    
    public boolean isBetweenHeight(double min_h, double max_h) {
        if (!getFrom().isBetweenHeight(min_h, max_h)) return false;
        if (!getTo().isBetweenHeight(min_h, max_h)) return false;
        return true;
    }

    private ArrayList<EvacuationAgent> positive_lane, negative_lane;
    public void setup_lanes() {
        positive_lane = new ArrayList<EvacuationAgent>();
        negative_lane = new ArrayList<EvacuationAgent>();
        for (EvacuationAgent agent : agents) {
            if (agent.getDirection() > 0.0) {
                positive_lane.add(agent);
            } else {
                negative_lane.add(agent);
            }
        }
        Collections.sort(positive_lane);
        Collections.sort(negative_lane);
    }

    private void add_agent_to_lane(EvacuationAgent agent) {
        if (agent.getDirection() > 0.0) {
            positive_lane.add(agent);
            Collections.sort(positive_lane);    
        } else {
            negative_lane.add(agent);
            Collections.sort(negative_lane);
        }
    }


    public ArrayList<EvacuationAgent> getLane(double speed) {
        if (speed > 0) return positive_lane;
        else return negative_lane;
    }

    public int getLaneWidth(double speed) {
        int d;
        if (speed > 0) d = positive_lane.size();
        else d = negative_lane.size();

        int lane_width = (int)(d * width / (positive_lane.size() +
                    negative_lane.size()));
        if (lane_width == 0) {
            lane_width = 1;
            /*
            System.err.print("WARNING: lane width 0 on " + getTagString());
            System.err.println(" (" + width + "*" + d + " / ("
                    + positive_lane.size() + " + "
                    + negative_lane.size() + ")");
            if (width < 2.0) {
                System.err.println("ERROR: path width " + width);
            }
            */
        }
        return lane_width ;
    }

    public void draw(Graphics2D g,
            boolean in_simulation,
            boolean show_label,
            boolean isSymbolic,
            boolean showScaling) {
        if (in_simulation) drawSimulation(g, show_label, showScaling);
        else drawEdit(g, show_label, isSymbolic, showScaling);
    }

    private void drawEdit(Graphics2D g,
            boolean show_label,
            boolean isSymbolic,
            boolean showScaling){
        //float fwidth = (float)(width / ((MapPartGroup)(parent)).getScale());
        //g.setStroke(new BasicStroke((float)width));
        double scale = 1.0;
        if (!showScaling)
            scale = g.getTransform().getScaleX();
        g.setStroke(new BasicStroke(2.0f / ((float)scale)));
        boolean oneWayPositive = this.hasTag("ONE-WAY-POSITIVE");
        boolean oneWayNegative = this.hasTag("ONE-WAY-NEGATIVE");
        boolean roadClosed = this.hasTag("ROAD-CLOSED");


        if (isSymbolic) {
            g.setColor(Color.GRAY);
            if (oneWayPositive)
                g.fill(getArrow(0.0, scale, true));
            else if (oneWayNegative)
                g.fill(getArrow(0.0, scale, false));
            else
                g.fill(getRect(0.0, scale, false));
            g.setColor(Color.YELLOW);
            if (oneWayPositive)
                g.draw(getArrow(0.0, scale, true));
            else if (oneWayNegative)
                g.draw(getArrow(0.0, scale, false));
            else
                g.draw(getRect(0.0, scale, false));
        } else if (selected) {
            g.setColor(Color.BLACK);
            if (oneWayPositive)
                g.fill(getArrow(0.0, scale, true));
            else if (oneWayNegative)
                g.fill(getArrow(0.0, scale, false));
            else
                g.fill(getRect(0.0, scale, false));
            g.setColor(Color.RED);
            if (oneWayPositive)
                g.draw(getArrow(0.0, scale, true));
            else if (oneWayNegative)
                g.draw(getArrow(0.0, scale, false));
            else
                g.draw(getRect(0.0, scale, false));
        } else if (width == 0) {
            g.setColor(Color.YELLOW);
            if (oneWayPositive)
                g.fill(getArrow(0.0, scale, true));
            else if (oneWayNegative)
                g.fill(getArrow(0.0, scale, false));
            else
                g.fill(getRect(0.0, scale, false));
        } else {
            if (oneWayPositive) {
                g.setColor(Color.MAGENTA);
                g.fill(getArrow(0.0, scale, true));
            } else if (oneWayNegative) {
                g.setColor(LIGHT_BLUE);
                g.fill(getArrow(0.0, scale, false));
            } else if (roadClosed) {
                g.setColor(LINK_RED);
                g.fill(getRect(0.0, scale, false));
            } else {
                g.fill(getRect(0.0, scale, false));
            }
            g.setColor(Color.BLACK);
            if (oneWayPositive)
                g.draw(getArrow(0.0, scale, true));
            else if (oneWayNegative)
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

    private void drawSimulation(Graphics2D g,
            boolean show_label, boolean showScaling) {
        double scale = 1.0;
        if (!showScaling)
            scale = g.getTransform().getScaleX();

        if (length == 0) return;

        if ((displayMode & 1) == 1) {
            /* Line changing color with the number of agents */
            //g.setStroke(new BasicStroke((float)width));
            Color c = getColorFromDensity();
            g.setColor(c);
            if (this.hasTag("ONE-WAY-POSITIVE"))
                g.draw(getArrow(0.0, scale, true));
            else if (this.hasTag("ONE-WAY-NEGATIVE"))
                g.draw(getArrow(0.0, scale, false));
            else
                g.draw(getRect(0.0, scale, true));
            //g.fill(getRect(0.0));

            if (show_label) {
                g.drawString("" + agents.size(),
                         (float)calcAgentPos(0.5).getX(),
                         (float)calcAgentPos(0.5).getY());
            }
        }
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

    public boolean agentCanEnter(EvacuationAgent agent,
            MapNode node) {
        /* Confluence */
        final MapLink lastPath = agent.getCurrentLink();
        if (lastPath == null) {
            System.err.println("ERROR: no path? " + agent.isEvacuated());
        }
        Double capacityLeft = fromLinksCapacity.get(lastPath);
        if (capacityLeft == null) {
            System.err.println("ERROR: path not registered?");
            for (MapLink link : fromLinksCapacity.keySet()) {
                System.err.println(link.getTagString());
            }
        }
        //System.err.print(getTagString() + " " + capacityLeft);
        if (capacityLeft <= 1.0) {
            //System.err.print(" over capacity (ignoring)");
            return false;
        }

        /* Crowdness */
        if (agents.size() > 0) {
            double clearance = length;
            if (isForwardDirectionFrom(node)) {
                int index = (int) width;
                if (index < getLane(1.0).size()) {
                    clearance = getLane(1.0).get(index).getPosition();
                }
            } else {
                //node is "to"
                int index = getLane(-1.0).size() - 1 - (int) width;
                if (index > 0) {
                    clearance = length - getLane(-1.0).get(index).getPosition();
                }
            }

            if (clearance < 0.20) {
                return false;
            }
        }

        /* Success */
        capacityLeft -= 1.0;
        fromLinksCapacity.put(agent.getCurrentLink(), capacityLeft);
        return true;
    }

    public void agentEnters(EvacuationAgent agent) {
        agents.add(agent);
        if (agent.getDirection() == 1.0 || agent.getDirection() == -1.0) {
            add_agent_to_lane(agent);
        }

        /* emergency mode? */
        if (getEmergency() 
                && agent instanceof RunningAroundPerson) {
            RunningAroundPerson rp = (RunningAroundPerson) agent;
            if (!rp.getGoal().equals(SpecialTerm.Emergency)) {
                rp.setGoal(SpecialTerm.Emergency) ;
                rp.renavigate();
            }
        }
    }

    public void agentExits (EvacuationAgent agent) {
        assert(agents.contains(agent));
        agents.remove(agent);
        if (! positive_lane.remove(agent)) {
            negative_lane.remove(agent);
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

    // create arrow polygon consist of seven coordinates. positive means the 
    // direction of arrow: true(from-to), false(to-from)
    public GeneralPath getArrow(double border, double scale,
            boolean positive) {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 0.0;
        double y2 = 0.0;
        if (positive) {
            x1 = getNegativeNode().getX();
            y1 = getNegativeNode().getY();
            x2 = getPositiveNode().getX();
            y2 = getPositiveNode().getY();
        } else {
            x1 = getPositiveNode().getX();
            y1 = getPositiveNode().getY();
            x2 = getNegativeNode().getX();
            y2 = getNegativeNode().getY();
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
        private static final long serialVersionUID = -6573997617890390259L;
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
            private static final long serialVersionUID = 7977802020577822078L;
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

    public ArrayList<EvacuationAgent> getAgents() {
        return agents;
    }

    // One-way link implementation:
    //  currenly, one-way link is supported to navigate complex routes with
    //  one-way road. One-way link is defined by the two type tags 
    //  "ONE-WAY-POSITIVE" and "ONE-WAY-NEGATIVE". These positive and negative 
    //  means direction whether fromNode -> toNode or toNode -> fromNode.
    //  To determine default direction, set North direction is positive, and 
    //  ofcourse South is negative (if the link direct East or West, East is
    //  assumed as positive and West is negative).

    /**
     * return North side (positive) node
     */
    public MapNode getPositiveNode() {
        double x1 = getFrom().getX() - getTo().getX();
        double y1 = getFrom().getY() - getTo().getY();
        double s = Math.acos(x1/Math.sqrt(x1*x1+y1*y1));

        if (y1 < 0.0)
            s = Math.PI * 2 - s;
        if (s >= 0.0 && s < Math.PI)
            return getTo();
        else
            return getFrom();
    }

    /**
     * return South side (positive) node
     */
    public MapNode getNegativeNode() {
        double x1 = getFrom().getX() - getTo().getX();
        double y1 = getFrom().getY() - getTo().getY();
        double s = Math.acos(x1/Math.sqrt(x1*x1+y1*y1));

        if (y1 < 0.0)
            s = Math.PI * 2 - s;
        if (s >= 0.0 && s < Math.PI)
            return getFrom();
        else
            return getTo();
    }

    @Override
    public NType getNodeType() {
        return NType.LINK;
    }

    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());
        element.setAttribute("id", "" + ID);
        element.setAttribute("length", "" + length);
        element.setAttribute("width", "" + width);
        element.setAttribute("from", "" + fromNode.ID);
        element.setAttribute("to", "" + toNode.ID);
        return element;
    }
    
    public static String getNodeTypeString() {
        return "Link";
    }

    public static MapLink fromDom(Element element) {
        int id = Integer.parseInt(element.getAttribute("id"));
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
        super.allTagsClear() ;
        clearCacheInNodes() ;
    }

    /**
     * タグを追加。
     * ノードのcacheもクリア。
     */
    @Override
    public boolean addTag(String _tag) {
        boolean result = super.addTag(_tag) ;
        if(result) 
            clearCacheInNodes() ;
        return result ;
    }

    /**
     * タグを削除。
     * ノードのcacheもクリア。
     */
    @Override
    public void removeTag(String _tag) {
        super.removeTag(_tag) ;
        clearCacheInNodes() ;
    }

    /**
     * ノードのcacheをクリア。
     */
    private void clearCacheInNodes() {
        if(fromNode != null) fromNode.clearCache() ;
        if(toNode != null) toNode.clearCache() ;
    }

    public String toString() {
        return getTagString();
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
     * @return 順方向なら 1.0。逆なら -1.0。
     */
    public double directionValueFrom(MapNode originNode) {
        return directionValueFrom(originNode, 1.0) ;
    }
    //------------------------------------------------------------
    /**
     * リンクが指定されたノードから見た時の direction の値
     * @param originNode エージェントが入る側のノード
     * @param baseValue direction の元になる値
     * @return 順方向なら baseValue。逆なら -baseValue。
     */
    public double directionValueFrom(MapNode originNode,
                                     double baseValue) {
        return (isForwardDirectionFrom(originNode) ?
                baseValue :
                -baseValue) ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て正方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @return 順方向なら 1.0。逆なら -1.0。
     */
    public double directionValueTo(MapNode destinationNode) {
        return directionValueTo(destinationNode, 1.0) ;
    }
    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て正方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @param baseValue direction の元になる値
     * @return 順方向なら baseValue。逆なら -baseValue。
     */
    public double directionValueTo(MapNode destinationNode,
                                   double baseValue) {
        return (isForwardDirectionTo(destinationNode) ?
                baseValue :
                -baseValue) ;
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


}
