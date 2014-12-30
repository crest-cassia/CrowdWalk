package nodagumi.ananPJ.Agents;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.vecmath.Vector3d;

import org.w3c.dom.Element;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBMapPart;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.misc.AgentGenerationFile;
import nodagumi.ananPJ.Simulator.Pollution;

import nodagumi.Itk.* ;

public abstract class EvacuationAgent extends OBMapPart
implements Comparable<EvacuationAgent>, Serializable {
    private static final long serialVersionUID = 2580480798262915926L;
    protected MapNode prev_node;
    protected MapNode next_node;
    protected MapLink current_link;

    private boolean evacuated = false;
    protected boolean waiting = false;
    public double generatedTime;
    public double finishedTime;
    protected String configLine = "none";

    public int displayMode = 0;

    protected double swing_width;

    /* tkokada: enable random navigation depending on the cost */
    protected boolean randomNavigation = false;
    /** The distance of how much the agent has moved in the current pathway. */
    protected double position;
    public double currentExposureAmount = 0.0;
    public double accumulatedExposureAmount = 0.0;

    protected Random random = null;

    protected static String pollutionType = "NonAccumulated";
    protected Pollution pollution = null;

    /* constructor */
    static int agent_count = 0;
    public int agentNumber;

	/**
	 * 引数なしconstractor。 ClassFinder.newByName で必要。
	 */
	public EvacuationAgent() {} ;
    public EvacuationAgent(int _id, Random _random) {
		init(_id, _random) ;
	}

	/**
	 * 初期化。constractorから分離。
	 */
    public void init(int _id, Random _random) {
        super.init(_id);
        random = _random;
        agentNumber = agent_count++;
        //swing_width = Math.random() * 2.0 - 1.0;
        swing_width = random.nextDouble() * 2.0 - 1.0;
        // Pollution のサブクラスのインスタンスを取得
        pollution = Pollution.getInstance(pollutionType + "Pollution");
    }

	public EvacuationAgent copyAndInitialize() {
		try {
			EvacuationAgent r = (EvacuationAgent)this.getClass().newInstance() ;
			return copyAndInitializeBody(r) ;
		} catch(Exception ex) {
			ex.printStackTrace() ;
			Itk.dbgErr("can not make a new instance from an agent.") ;
			Itk.dbgMsg("agent", this) ;
			return null ;
		}
	}

	/**
	 * 与えられたエージェントインスタンスに内容をコピーし、初期化。
	 */
	public EvacuationAgent copyAndInitializeBody(EvacuationAgent r) {
        r.ID = ID;
        r.generatedTime = generatedTime;
        r.prev_node = prev_node;
        r.next_node = next_node;
        r.current_link = current_link;
        r.position = position;
        r.random = random;
        for (String tag : tags) {
            r.addTag(tag);
        }

		return r ;
	}
		

    public static OBNode fromDom(Element element) {
        String tag_name = element.getAttribute("AgentType");
        if (tag_name.equals(RunningAroundPerson.getAgentTypeString())) {
            // TODO: 多分バグ(再びこのメソッドが呼ばれる)
            return RunningAroundPerson.fromDom(element);
        } else if (tag_name.equals(WaitRunningAroundPerson.getAgentTypeString())) {
            return WaitRunningAroundPerson.fromDom(element);
        } else {
            //TODO must implement other types of agents
            return null;
        }
    }

    public static EvacuationAgent fromString(String str) {
        EvacuationAgent agent = null;
        agent = RunningAroundPerson.fromString(str);
        if (agent != null) { return agent; }
        agent = WaitRunningAroundPerson.fromString(str);
        if (agent != null) { return agent; }
        agent = Staff.fromString(str);
        if (agent != null) { return agent; }

        return null;
    }

    public void place(MapLink link, double _position) {
        prev_node = link.getFrom();
        next_node = link.getTo();
        setCurrentLink(link);
        position = _position;
    }

    public static String getTypeName() { return null; };

    abstract public double getEmptySpeed();
    abstract public void setEmptySpeed(double s);
    abstract public void setGoal(Term _goal);

    abstract public double getSpeed();
    abstract public void setSpeed(double speed);
    abstract public double getDirection();
    abstract public boolean isPositiveDirection();
    abstract public boolean isNegativeDirection();
    abstract public double getAcceleration();
    
    abstract public void prepareForSimulation(double _ts);
    abstract public void preUpdate(double time);
    abstract public boolean update(double time);
    abstract public void updateViews();

    // tkokada
    abstract public MapLinkTable getReachableLinks(double d, double time,
            double duration);

    public int getTriage() {
        return pollution.getTriage(this);
    }

    public boolean finished() {
        return pollution.finished(this);
    }

    abstract public void draw(Graphics2D g, boolean experiment);
    abstract public void dumpResult(PrintStream out);
    
    public abstract String toString(); 

    public MapNode getNextNode() {
        return next_node;
    }
    
    public void setPrevNode(MapNode node) {
        this.prev_node = node;
    }

    public MapNode getPrevNode() {
        return prev_node;
    }

    protected void setCurrentLink(MapLink currentPathway) {
        this.current_link = currentPathway;
    }

    public MapLink getCurrentLink() {
        return current_link;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public double getPosition() {
        return position;
    }

    public Vector3d getSwing() {
        if (null == current_link) {
            return new Vector3d(0, 0, 0);
        }

        double scale = ((MapPartGroup)(current_link.getParent())).getScale();
        double fwidth = current_link.width / 2 / scale;
        double x1 = current_link.getFrom().getX();
        double x2 = current_link.getTo().getX();
        double y1 = current_link.getFrom().getY();
        double y2 = current_link.getTo().getY();

        Vector3d v1 = new Vector3d(x2 - x1, y2-y1, 0);
        v1.normalize();
        Vector3d v2 = new Vector3d(0, 0, fwidth * swing_width);
        Vector3d v3 = new Vector3d();
        v3.cross(v1, v2);
        return v3;
    }

    public Point2D getPos() {
        if (getCurrentLink() != null) {
            return getCurrentLink().calcAgentPos(position);
        } else if (prev_node != null){
            return prev_node.getAbsoluteCoordinates();
        } else {
            return null;
        }
    }

    public double getHeight() {
        if (getCurrentLink() != null) {
            return getCurrentLink().calcAgentHeight(position);
        } else {
            return Double.NaN;
        }
    }
    
    /* Setting the agent's attributes
     */
    public static void showAttributeDialog(NetworkMap networkMap,
            ArrayList<EvacuationAgent> agents) {
        /* Set attributes with a dialog */
        class AttributeSetDialog  extends JDialog implements ActionListener {
            private static final long serialVersionUID = -5975770541398630L;

            private NetworkMap networkMap;
            private ArrayList<EvacuationAgent> agents;
            private JTextField[] textFields; 

            public AttributeSetDialog(NetworkMap _networkMap,
                    ArrayList<EvacuationAgent> _agents) {
                super();

                networkMap = _networkMap;
                this.setModal(true);
                agents = _agents;

                int count = 0;
                for (EvacuationAgent agent : agents) {
                    if (agent.selected) {
                        ++count;
                    }
                }
                if (count == 0) return;
                setUpPanel();
            }
            
            JComboBox target;
            private void setUpPanel() {
                Container contentPane = getContentPane();

                JPanel panel = null;

                textFields = new JTextField[1];

                /* labels and text fields */
                panel = new JPanel(new GridLayout(1, 2));

                panel.add(new JLabel("Type:"));
                //TODO should be something like: RunningAroundPerson.getNavModeLabels()
                target = new JComboBox(networkMap.getAllTags().toArray());
                panel.add(target);
                contentPane.add(panel, BorderLayout.NORTH);

                /* ok and cancel button */
                textFields[0] = new JTextField("type");
                panel = new JPanel(new GridLayout(1, 3));
                panel.add(new JLabel());
                JButton ok = new JButton("OK");
                ok.addActionListener(this);
                panel.add(ok);
                JButton cancel = new JButton("Cancel");
                cancel.addActionListener(this);
                panel.add(cancel);

                contentPane.add(panel, BorderLayout.SOUTH);
                this.pack();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    final String goalString = (String)target.getSelectedItem();
                    for (EvacuationAgent agent : agents) {
                        if (agent instanceof RunningAroundPerson) {
                            RunningAroundPerson rp = (RunningAroundPerson)agent;
							if (rp.selected) rp.setGoal(new Term(goalString));
                        } 
                    }
                    this.dispose();
                } else if (e.getActionCommand().equals("Cancel")) {
                    this.dispose();
                }
            }

        }
        AttributeSetDialog dialog = new AttributeSetDialog(networkMap, agents);
        dialog.setVisible(true);
    }

    public static void showRouteDialog(NetworkMap networkMap,
            ArrayList<EvacuationAgent> agents) {
        /* Set attributes with a dialog */
        class AttributeSetDialog  extends JDialog implements ActionListener {
            private static final long serialVersionUID = -6560704811897168475L;

            private ArrayList<EvacuationAgent> agents;

            public AttributeSetDialog(NetworkMap networkMap,
                    ArrayList<EvacuationAgent> _agents) {
                super();

                this.setModal(true);
                agents = _agents;

                int count = 0;
                for (EvacuationAgent agent : agents) {
                    if (agent.selected) {
                        ++count;
                    }
                }
                if (count == 0) return;
                setUpPanel(networkMap);
            }
            
            JComboBox[] routes;
            private void setUpPanel(NetworkMap networkMap) {
                Container contentPane = getContentPane();

                String route_length_str = JOptionPane.showInputDialog("Route length?");
                if (route_length_str == null) {
                    this.dispose();
                    return;
                }

                int route_length = Integer.parseInt(route_length_str);
                JPanel panel = null;

                routes = new JComboBox[route_length];

                /* labels and text fields */
                panel = new JPanel(new GridLayout(route_length, 2));

                for (int i = 0; i < route_length; ++i) {
                    panel.add(new JLabel("Via " + i));
                    routes[i] = new JComboBox(networkMap.getAllTags().toArray());
                    panel.add(routes[i]);
                }
                //TODO should be something like: RunningAroundPerson.getNavModeLabels()
                contentPane.add(panel, BorderLayout.NORTH);

                /* ok and cancel button */
                panel = new JPanel(new GridLayout(1, 3));
                panel.add(new JLabel());
                JButton ok = new JButton("OK");
                ok.addActionListener(this);
                panel.add(ok);
                JButton cancel = new JButton("Cancel");
                cancel.addActionListener(this);
                panel.add(cancel);

                contentPane.add(panel, BorderLayout.SOUTH);
                this.pack();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    ArrayList<Term> planned_route = new ArrayList<Term>();
                    for (int i = 0; i < routes.length; i++) {
						Term tag = new Term((String)routes[i].getSelectedItem());
						planned_route.add(tag) ;
                    }
                    for (EvacuationAgent agent : agents) {
                        if (agent instanceof RunningAroundPerson) {
                            RunningAroundPerson rp = (RunningAroundPerson)agent;
                            if (rp.selected) rp.setPlannedRoute(planned_route);
                        } 
                    }
                    this.dispose();
                } else if (e.getActionCommand().equals("Cancel")) {
                    this.dispose();
                }
            }

        }
        AttributeSetDialog dialog = new AttributeSetDialog(networkMap, agents);
        dialog.setVisible(true);
    }

    abstract public JPanel paramSettingPanel(NetworkMap networkMap);

    public static String[] getAgentTypes() {
        String ret[] = {"NOT SELECTED",
                RunningAroundPerson.getTypeName(),
                WaitRunningAroundPerson.getTypeName(),
                Staff.getTypeName()};
        return ret;
    }
    
    public static EvacuationAgent createEmptyAgent(String type,
            Random _random) {
        if (type.equals("NOT SELECTED")) {
            return null;
        } else if (type.equals(WaitRunningAroundPerson.getTypeName())) {
            return new WaitRunningAroundPerson(0, _random);
        } else if (type.equals(RunningAroundPerson.getTypeName())) {
            return new RunningAroundPerson(0, _random);
        } else if (type.equals(Staff.getTypeName())) {
            return new Staff(_random);
        }else {
            return null;
        }
    }
    
    public final static String getNodeTypeString() {
        return "Agent";
    }
    
    public void exposed(double c) {
        pollution.expose(this, c);
    }

    public void setEvacuated(boolean evacuated, double time) {
        this.evacuated = evacuated;
    }

    public boolean isEvacuated() {
        return evacuated;
    }

    public abstract void setEmergency();
    public abstract boolean isEmergency();
    public abstract Term getGoal();
    public abstract List<Term> getPlannedRoute();

    // current_link 上における絶対 position
    public double absolutePosition() {
        return isPositiveDirection() ? position : current_link.length - position;
    }

    public int compareTo(EvacuationAgent rhs) {
        double h1 = this.position;
        double h2 = rhs.position;

        // tkokada modified
        if (h1 == h2) {
            //return (int)((agentNumber - rhs.agentNumber) * getDirection());
            // m.saito modified
            if (agentNumber == rhs.agentNumber) {
                return 0;
            } else if (agentNumber > rhs.agentNumber) {
                return (int)(1 * getDirection());
            } else {
                return (int)(-1 * getDirection());
            }
            //return 0;
        } else if (h1 > h2) {
            return (int)(1 * getDirection());
            //return 1;
        } else {
            return (int)(-1 * getDirection());
            //return -1;
        }
    }

    public void setRandom(Random _random) {
        random = _random;
    }

    public boolean getRandomNavigation() {
        return randomNavigation;
    }

    public void setRandomNavigation(boolean _randomNavigation) {
        randomNavigation = _randomNavigation;
    }

    public int getAgentNumber() {
        return agentNumber;
    }

    public String getConfigLine() {
        return configLine;
    }

    public void setConfigLine(String str) {
        configLine = str;
    }

    public static void setPollutionType(String s) {
        pollutionType = s;
    }

	//------------------------------------------------------------
    /* [2014.12.29 I.Noda]
     * directive を増やす場合は、継承するクラスで以下２つを再定義していく。
     */
	/**
	 * 知っている directive かどうかのチェック
	 */
	public boolean isKnownDirective(Term term) {
		return false ;
	}

	//------------------------------------------------------------
	/**
	 * 知っている directive かどうかのチェック
	 * @return pushした数
	 */
	public int pushPlaceTagInDirective(Term directive,
									   ArrayList<Term> goalList) {
		return 0 ;
	}

}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
