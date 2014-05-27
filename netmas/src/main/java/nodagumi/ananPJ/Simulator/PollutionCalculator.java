package nodagumi.ananPJ.Simulator;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;


public class PollutionCalculator implements Serializable {
    private static final long serialVersionUID = 29847234890234908L;
    static double AGENT_HEIGHT = 1.5;

    String scheduleFileName = null;
    transient BufferedReader scheduleFile = null;
    //BufferedReader scheduleFile = null;

    double nextEvent = 0;
    double timeScale;
    String nextLine;

    public static boolean debug = false;

    HashMap<Integer, PollutedArea> polluted_area_sorted;

    public ArrayList<PollutedArea> pollutedAreaFromNodes(ArrayList<MapNode>
            nodes) {
        ArrayList <PollutedArea> areas = new ArrayList<PollutedArea>();
        return areas;
    }

    public PollutionCalculator(String _scheduleFileName,
            ArrayList<PollutedArea> _pollution, double _timeScale) {
        scheduleFileName = _scheduleFileName;
        if (scheduleFileName == null || scheduleFileName.isEmpty()) {
            nextEvent = -1.0;
        } else {
            try {
                FileReader fr = new FileReader(scheduleFileName);
                scheduleFile = new BufferedReader(fr);

                nextLine = getNextLine();
                if (nextLine == null) { // tkokada: avoid null pointer exception
                    nextEvent = -1.0;
                } else {
                    String[] items = nextLine.split(",");

                //System.out.println("in PollutionCalculator() items: "+items[0]+" "+items[1]+" "+items[2]);


                nextEvent = Double.parseDouble(items[0]);
                //System.err.println("the first update on pollution will be on " + nextEvent);
                //System.out.println("the first update on pollution will be on " + nextEvent);
                }
            } catch (IOException e) {
                System.err.print("WARNING: No pollution scenario given.");
                nextEvent = -1.0;
            }       
        }
        
        setup_polluted_areas(_pollution);
        timeScale = _timeScale;
        
        //System.out.println("timeScale "+timeScale);
    }
    
    public void updateNodesLinksAgents(double time,
            ArrayList<MapNode> nodes,
            ArrayList<MapLink> links,
            List<EvacuationAgent> agents) {
        //if (debug) System.err.println("PC update: " + time + ", next: " + nextEvent);

        if (nextEvent != -1.0 && nextEvent <= time) {
            // System.out.println("  PC update next event: " + time);
            update_pollution();
        }

        for (EvacuationAgent agent : agents) {
            if (agent.isEvacuated())
                continue;
            double min_distance = Double.MAX_VALUE;
            PollutedArea best_area = null;
            Vector3f point = new Vector3f((float)agent.getPos().getX(),
                    (float)agent.getPos().getY(),
                    (float)(agent.getHeight() + AGENT_HEIGHT));
            for (PollutedArea area : polluted_area_sorted.values()) {
                if (area.contains(point)) {
                    best_area = area;
                    break;
                }

                double d = area.distance(point);
                //System.err.println("  area : " + area + ", point: " + point +
                //        ", d: " + d);

                // if (d < min_distance) {
                    // min_distance = d;
                    // best_area = area;
                // }
            }

            if (best_area != null) {
                Double d = (Double)best_area.getUserObject();
                if (debug) System.err.println(agent.agentNumber + " " + d);

                // System.err.printf("in pollution calculator agent: %04d, " + 
                        // "d: %.4f, speed: %.4f, %s\n", agent.ID, d,
                        // agent.getSpeed(), best_area.getTags());
                if (d != null) {
                    agent.exposed(d * timeScale);
                    best_area.setContactOfAgents(true);
                }
            } else {
                agent.exposed(0.);
            }
        }
    }

    class AreaGroups extends PollutedArea implements Serializable {
        private static final long serialVersionUID = -2438038596474793650L;
        private ArrayList<PollutedArea> subgroups;
        private boolean view;
        public AreaGroups(
                double from_x, double from_y, double from_z,
                double to_x, double to_y, double to_z,
                ArrayList<PollutedArea> subgroup_candidates) {
            super(0);
            view = false;
            subgroups.addAll(subgroup_candidates);
        }

        @Override
        public boolean contains(Vector3f point) {
            return false;
        }

        @Override
        public boolean contains(Point2D point) {
            return false;
        }

        @Override
        public void draw(Graphics2D g, boolean experiment) { /* do nothing */}
        @Override
        public TransformGroup get3DShape(Appearance app) {
            /* do nothing */
            return null;
        }

        @Override
        public double getDensity() {
            /* do nothing */
            return 0;
        }

        public double getDensity(Point2D point, double height) {
            
            return 0;
        }

        @Override
        public Shape getShape() {
            /* do nothing */
            return null;
        }
        
        @Override   // tkokada
        public ArrayList<Point2D> getAllVertices() {
            return null;
        }
        
        @Override // tkokada
        public double getAngle() {
            double d = Double.NaN;
            return d;
        }

        @Override
        public NType getNodeType() {
            /* do nothing */
            return null;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public double distance(Vector3f point) {
            return 0;
        }

        @Override
        public boolean getContactOfAgents() {
            return view;
        }

        @Override
        public void setContactOfAgents(boolean _view) {
            view = _view;
        }

        private void writeObject(ObjectOutputStream stream) {
            try {
                stream.defaultWriteObject();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        private void readObject(ObjectInputStream stream) {
            try {
                stream.defaultReadObject();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
        }
    }

    private void setup_polluted_areas(ArrayList<PollutedArea> areas) {
        polluted_area_sorted = new HashMap<Integer, PollutedArea>();

        //System.out.println("in setup_polluted_areas");

        for (PollutedArea area : areas) {
            //System.out.println("in setup_polluted_areas"+areas);

            Matcher m = area.matchTag("^(\\d+)$");
            if (m != null) {
                int index = Integer.parseInt(m.group(0));

                //System.out.println("index "+index);

                polluted_area_sorted.put(index, area);
            }
        }
    }

    private Double correct_density(double d) {
        /* 2010/02/18 for HCN gas
         *  2.28 \times 10^{-13} * d^{4.56}
         */
        /* reduce density */
        //d *= 10E-2;

        //d *= 10E6;/* the values are given in 10^{-6} */
        //return Math.pow(d, 4.56) * 2.28 * 10E-13;
        return d * 10E-4;
    }

    private void update_pollution() {
        if (debug) System.err.println("PC: updating pollution " + nextEvent);
        
        String[] items = nextLine.split(",");
    
        //System.out.println("in update_pollution() items: "+items[0]+" "+items[1]+" "+items[2]+" "+items[3]+" "+items[4]);
        
        
        for (Integer index : polluted_area_sorted.keySet()) {
            PollutedArea area = polluted_area_sorted.get(index);
            
            //System.out.println("index "+index+"index.intValue() "+index.intValue());
                
            double _d = Double.parseDouble(items[index.intValue()]);
            Double d = correct_density(_d);
            
            if (debug) System.err.println("(" + index + "=" + d + ") ");
            
            area.setUserObject(d);
        }

        try {
            nextLine = getNextLine();
            if (nextLine == null) nextEvent = -1;
            else nextEvent = Double.parseDouble(nextLine.split(",")[0]);
        } catch (Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getNextLine() {
        String line = null;
        do {
            try {
                line = scheduleFile.readLine();
                if (line == null) return null;
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        } while (line.charAt(0) == '#');
        return line;
    }

    public ArrayList<PollutedArea> getPollutions() {
        return new ArrayList<PollutedArea>(polluted_area_sorted.values());
    }
}
