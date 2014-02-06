package nodagumi.ananPJ.Simulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.*;
import java.lang.ClassNotFoundException;
//import java.lang.System;
import java.text.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Formatter;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;

import org.w3c.dom.Document;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.misc.AgentGenerationFile;
import nodagumi.ananPJ.misc.GenerateAgent;
import nodagumi.ananPJ.network.DaRuMaClient;
import nodagumi.ananPJ.network.FusionViewerConnector;

public class AgentHandler implements Serializable {
    private EvacuationModelBase model;
    private List<EvacuationAgent> agents;
    private ArrayList<EvacuationAgent> generated_agents;
    private ArrayList<EvacuationAgent> evacuated_agents;

    static final double defaultAgentSpeed = 1.4;
    static final double defaultAgentConfidence = 1.0;
    static final double defaultAgentHitPoint = 100.0;

    private int evacuatedAgentCount = 0;
    private int waitingAgentCount = 0;
    private double totalDamage = 0.0;
    private double maxDamage = 0.0;
    private double averageSpeed = 0.0;
    private int evacuatedUsedLiftAgentCount = 0;
    private int evacuatedNoLiftAgentCount = 0;
    private boolean rescueArrived = false;
    private int maxAgentCount = 0;
    private boolean isAllAgentSpeedZeroBreak = false;
    private boolean isAllAgentSpeedZero = false;

    private transient JButton start_button = null;
    private transient JButton pause_button = null;
    private transient JButton step_button = null;

    private long simulation_weight = 0;
    private transient JScrollBar simulation_weight_control;
    private transient JLabel simulation_weight_value;

    private double startTime = 7 * 3600 + 30 * 60;
    private double outbreakTime = 7 * 3600 + 40 * 60;

    private HashMap<MapNode, Integer> evacuatedAgentCountByExit;
    private AgentGenerationFile generate_agent = null;

    boolean has_display;
    private boolean randomNavigation = false;
    private Random random = null;
    private FusionViewerConnector fusionViewerConnector = null;

    // プログラム制御用環境変数
    public String envSwitch = "";
    // エージェントが存在するリンクのリスト
    private ArrayList<MapLink> effectiveLinks = null;
    private boolean effectiveLinksEnabled = false;

    public AgentHandler (ArrayList<EvacuationAgent> _agents,
            String generationFile,
            String responseFile,
            String scenario_number,
            ArrayList<MapLink> links,
            EvacuationModelBase _model,
            boolean _has_display,
            double linerGenerateAgentRatio,
            Random _random) {
        model = _model;
        has_display = _has_display;
        random = _random;

        evacuatedAgentCountByExit = new HashMap<MapNode, Integer>();

        /* clone all agents already on board */
        agents = new ArrayList<EvacuationAgent>();
        for (final EvacuationAgent agent : _agents) {
            agents.add(agent.copyAndInitialize());
        }
        generated_agents = new ArrayList<EvacuationAgent>();
        evacuated_agents = new ArrayList<EvacuationAgent>();

        for (MapNode node : model.getNodes()) {
            if (node.hasTag("Exit")) {
                evacuatedAgentCountByExit.put(node, 0);
            }
        }

        for (EvacuationAgent agent : agents) {
            MapLink link = agent.getCurrentLink(); 
            link.agentEnters(agent);
        }

        generate_agent = new AgentGenerationFile(generationFile,
                model.getNodes(), model.getLinks(), has_display,
                linerGenerateAgentRatio, random);
        if (generate_agent != null) {
            for (GenerateAgent factory : generate_agent) {
                maxAgentCount += factory.getMaxGeneration();
            }
        }
        parseResponseFile(responseFile);

        if (scenario_number != null) {
            for (Integer id : events.keySet()) {
                if (id > scenario_number.length() - 1) {
                    System.err.println("no index for " + id);
                    continue;
                }
                if (events.get(id).time_difference == null) continue;
                int i = Integer.parseInt("" + scenario_number.charAt(id));
                events.get(id).setIndex(i);
            }
        }

        if (has_display) {
            setup_control_panel(generationFile,
                    responseFile,
                    scenario_number,
                    links);
        }
        fusionViewerConnector = new FusionViewerConnector();

        envSwitch = System.getenv("NETMAS");
        if (envSwitch == null)
            envSwitch = "";
        if (envSwitch.indexOf("effectiveLinks") != -1)
            effectiveLinksEnabled = true;
    }

    public void deserialize(String generationFile, String responseFile,
            String scenario_number, ArrayList<MapLink> links) {
        control_panel = null;
        clock_label = new JLabel("NOT STARTED");
        time_label = new JLabel("NOT STARTED!");
        evacuatedCount_label = new JLabel("NOT STARTED");
        message = new JTextArea("UNMaps Version 1.9.5\n");
        setup_control_panel(generationFile,
                responseFile,
                scenario_number,
                links);
    }

    public void prepareForSimulation() {
        for (EvacuationAgent agent : agents) {
            agent.prepareForSimulation(model.getTimeScale());
        }
        // 初回は全リンクを対象とする
        effectiveLinks = (ArrayList<MapLink>)model.getLinks().clone();
    }

    class ScenarioEvent implements Serializable {
        public static final String StopTag = "STOP_TAG";
        ScenarioEvent parent;
        Double time;
        ArrayList<Double> time_difference;
        String tag, command, comment;

        int index = 0;

        public ScenarioEvent(ScenarioEvent _parent) {
            parent = _parent;
            time_difference = new ArrayList<Double>();
        }

        public void setTag(String _tag) { tag = _tag; }
        public void setCommand(String _command) { command = _command; }
        public boolean isAbsolute() { return null == parent; }
        public int getIndex() {return index; }

        public void setTime(double _time) {
            if (parent != null) {
                System.err.println("This event is not absolute timed!");
            } else {
                time = _time;
            }
        }

        public void addTime(double _time) {
            _time *= 60;
            if (parent == null) {
                System.err.println("This event is not relative timed!");
            } else {
                time_difference.add(_time);
            }
        }
        public void setIndex(int i) { index = i; happend = false; }
        public int getMaxIndex() { return time_difference.size(); }

        public void setComment(String _comment) { comment = _comment; } 

        public double getClock() {
            if (parent == null) return time;
            else return parent.getClock() + time_difference.get(index);
        }

        public double getTimeIndex(int i) {
            if (parent == null) return time;
            else return time_difference.get(i);
        }

        /* used while running the simulation
         * modify links so that it affects agents */
        boolean happend = false;
        public void checkIfHappend(double clock,
                ArrayList<MapLink> links,
                double time) {
            if (happend || clock < getClock()) return;
            if (command == null) return;
            if (tag == null) return;

            System.err.println(clock + "\t" + command + "\t" + tag);
            message.append(timeToString(clock) + ":" + comment + "\n");

            setEnabled(true, links);
        }

        public void setEnabled(boolean b,
                ArrayList<MapLink> links) {
            happend = true;
            if (command.length() > 3 && command.substring(0, 4).equals("SET:")) {
                String set_tag = command.substring(4);
                System.err.println("set_tag" + set_tag);
                for (MapLink link : links) {
                    if (!link.hasTag(tag)) continue;
                    link.addTag(set_tag);
                }
            } else if (command.equals("BOTH")) {
                for (MapLink link : links) {
                    if (!link.hasTag(tag)) continue;
                    link.setEmergency(b);
                    link.setStop(b);
                }
            } else if (command.equals("EVACUATE")) {
                for (MapLink link : links) {
                    if (!link.hasTag(tag)) continue;
                    link.setEmergency(b);
                }
            } else if (command.equals("STOP")) {
                for (MapLink link : links) {
                    if (!link.hasTag(tag)) continue;
                    link.setStop(b);
                }
            } else if (command.equals("RESPONSE")) {
                rescueArrived = b;
            } else if (command.equals("STOP_TIMES")) {
                String splited[] = comment.substring(1, comment.length())
                    .split("-");
                if (splited.length != 2)
                    return;
                double moving = Double.valueOf(splited[0]);
                double stopping = Double.valueOf(splited[1]);
                for (MapLink link : links) {
                    if (link.hasTag(tag)) {
                        link.addStopTime(tag, 
                                time_difference.get(index),
                                moving, stopping);
                    }
                    if (((MapNode) link.getFrom()).hasTag(tag)) {
                        ((MapNode) link.getFrom()).addStopTime(tag,
                            time_difference.get(index),
                            moving, stopping);
                    }
                    if (((MapNode) link.getTo()).hasTag(tag)) {
                        ((MapNode) link.getTo()).addStopTime(tag,
                            time_difference.get(index),
                            moving, stopping);
                    }
                }
            } else if (command.equals("ADD_STOP")) {
                // System.err.println("  add stop tag: " + tag + ", time d: " + time_difference.get(index));
                // OBNode p = (OBNode)((javax.swing.tree.DefaultMutableTreeNode) links.get(0)).getParent();
                // while (p != null) {
                    // System.err.println(" OBNode id: " + p.ID + ", tags: " + p.getTags());
                    // p = (OBNode)((javax.swing.tree.DefaultMutableTreeNode) p).getParent();
                // }
                for (MapLink link : links) {
                    // System.err.println("from tags: " + ((MapNode) link.getFrom()).getTags());
                    // System.err.println("to   tags: " + ((MapNode) link.getTo()).getTags());
                    if (link.hasTag(tag)) {
                        link.addStopTime(tag, time_difference.get(index), -1,
                                         2);
                    }
                    if (((MapNode) link.getFrom()).hasTag(tag)) {
                        ((MapNode) link.getFrom()).addStopTime(tag,
                            time_difference.get(index), -1, 2);
                        // System.err.println("    tag is added to From for link " + link.ID);
                    }
                    if (((MapNode) link.getTo()).hasTag(tag)) {
                        ((MapNode) link.getTo()).addStopTime(tag,
                            time_difference.get(index), -1, 2);
                        // System.err.println("    tag is added to To for link " + link.ID);
                    }
                }
            } else if (command.equals("REMOVE_STOP")) {
                // System.err.println("  remove stop tag: " + tag);
                for (MapLink link : links) {
                    if (link.hasTag(tag)) {
                        link.removeStopTime(tag);
                    }
                    if (((MapNode) link.getFrom()).hasTag(tag)) {
                        ((MapNode) link.getFrom()).removeStopTime(tag);
                    }
                    if (((MapNode) link.getTo()).hasTag(tag)) {
                        ((MapNode) link.getTo()).removeStopTime(tag);
                    }
                }
            }
        }
    }

    HashMap<Integer, ScenarioEvent> events = new HashMap<Integer,
        ScenarioEvent>();
    private void setup_default_scenario() {
        startTime = 0;
        /*
        ScenarioEvent event  = new ScenarioEvent(null);
        event.setTime(0);
        event.setCommand("EVACUATE");
        event.setTag("*");
        event.setComment("全員避難開始");
        events.put(0, event);
        */
    }

    private void parseResponseFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            setup_default_scenario();
            return;
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (IOException e) {
            System.err.println(e);
            if (has_display) {
                JOptionPane.showMessageDialog(null,
                        "シナリオを開くのに失敗しました．");
            }
            setup_default_scenario();
            return;
        }
        try {
            /* #ID,親,TAG,COMMAND,TIME,,#備考,
             * #START, OUTBREAK
             * #EVACUATE, STOP
             * 1,0,START,,7:30,,#実験開始,
             * 2,0,OUTBREAK,,7:40,,#テロ災害発生,
             * 3,2,DETECT,,5,10,#散布から検知までの所要時間,
             *
             * 2013.02.21 tkokada newly added command
             * #ID,parent,tag,STOP_TIMES,TIME,LTIME,,#NONSTOPSECONDS-STOPSECONDS
             * 4,3,TRAIN_EXIT,STOP_TIMES,0,,,#10-50
             *   This scenario defines a tag to stop agents in STOPSECONDS. But
             *   after STOPSECONDS, agetns can walk during NONSTOPSECONDS.
             * 2013.05.07 tkokada added a new command
             * #ID,parent,tag,ADD_STOP,TIME,,#comment
             * #ID,parent,tag,REMOVE_STOP,TIME,,#comment
             *   This scenario adds or removes "STOP" tag to nodes or links 
             *   including specified tag.
             */

            String line;
            Pattern timepat;
            timepat = Pattern.compile("(\\d?\\d):(\\d?\\d)");
            Pattern timepat_long;
            timepat_long = Pattern.compile("(\\d?\\d):(\\d?\\d):(\\d?\\d)");
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                line = line.toUpperCase();
                String items[] = line.split(",");

                int id = Integer.parseInt(items[0]);
                int parent_id = Integer.parseInt(items[1]);
                String tag = items[2];
                String command = items[3];
                ScenarioEvent event;

                if (parent_id == 0) {
                    event  = new ScenarioEvent(null);
                    Matcher m = timepat.matcher(items[4]);
                    Matcher m_long = timepat_long.matcher(items[4]);

                    if (m.matches()) {
                        double time = 3600 * Integer.parseInt(m.group(1)) +
                        60 * Integer.parseInt(m.group(2));
                        event.setTime(time);

                        if (tag.equals("START")) startTime = time;
                        else if (tag.equals("OUTBREAK")) outbreakTime = time;
                    } else if (m_long.matches()) {
                        double time = 3600 * Integer.parseInt(m_long.group(1)) +
                        60 * Integer.parseInt(m_long.group(2)) +
                        Integer.parseInt(m_long.group(3));
                        event.setTime(time);

                        if (tag.equals("START")) startTime = time;
                        else if (tag.equals("OUTBREAK")) outbreakTime = time;
                    } else {
                        System.err.println("no matching item:" + items[4] +
                                " while reading scenario.");
                        System.err.println(line);
                        continue;
                    }
                } else if (command.equals("ADD_STOP") ||
                        command.equals("REMOVE_STOP")) {
                    event  = new ScenarioEvent(events.get(parent_id));
                    double ptime = (events.get(parent_id)).getClock();
                    Matcher m = timepat.matcher(items[4]);
                    Matcher m_long = timepat_long.matcher(items[4]);
                    if (m.matches()) {
                        double time = 3600 * Integer.parseInt(m.group(1)) +
                        60 * Integer.parseInt(m.group(2)) - ptime;
                        while (time < 0.) {
                            time += 86400.0;
                        }
                        event.addTime(time / 60.);
                    } else if (m_long.matches()) {
                        double time = 3600 * Integer.parseInt(m_long.group(1)) +
                        60 * Integer.parseInt(m_long.group(2)) +
                        Integer.parseInt(m_long.group(3)) - ptime;
                        while (time < 0.) {
                            time += 86400.0;
                        }
                        event.addTime(time / 60.);
                    } else {
                        System.err.println("no matching item:" + items[4] +
                                " while reading scenario.");
                        System.err.println(line);
                        continue;
                    }
                } else {
                    event  = new ScenarioEvent(events.get(parent_id));
                    /* must have at least one dt */
                    int dt = Integer.parseInt(items[4]);
                    event.addTime(dt);
                }
                event.setCommand(command);
                event.setTag(tag);

                //System.err.print(id + "\t");
                //System.err.print(parent_id + "\t");
                //System.err.print(tag + "\t");
                //System.err.print(command + "\t");

                StringBuffer comment_buffer = new StringBuffer("");

                for (int i = 5; i < items.length; ++i) {
                    if (items[i].equals("")) continue;
                    if (items[i].charAt(0) == '#') {
                        comment_buffer.append(items[i]);
                        continue;
                    }
                    int dt = Integer.parseInt(items[i]);
                    event.addTime(dt);
                }
                event.setIndex(event.getMaxIndex() - 1);
                //event.setIndex(0);
                event.setComment(comment_buffer.toString());
                events.put(id, event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    /* need stable design to assign id */
    private int manualId = 1024;
    public void update(ArrayList<MapNode> nodes, ArrayList<MapLink> links,
            double time) {
        update_buttons();

        ArrayList<EvacuationAgent> generated_agents_step = new
            ArrayList<EvacuationAgent>();
        for (ScenarioEvent event : events.values()) {
            event.checkIfHappend(time + startTime, links, time);
        }

        if (generate_agent != null) {
            for (GenerateAgent factory : generate_agent) {
                factory.tryUpdateAndGenerate(time + startTime,
                        model.getTimeScale(),
                        time, model, generated_agents_step);
            }
        }

        if (! generated_agents_step.isEmpty()) {
            if (randomNavigation) {
                for (EvacuationAgent agent : generated_agents_step)
                    agent.setRandomNavigation(true);
            }
            agents.addAll(generated_agents_step);
            generated_agents.addAll(generated_agents_step);
            if (effectiveLinksEnabled) {
                for (EvacuationAgent agent : generated_agents_step) {
                    if (agent.isEvacuated() || effectiveLinks.contains(agent.getCurrentLink())) continue;
                    effectiveLinks.add(agent.getCurrentLink());
                }
            }
            if (isExpectedDensitySpeedModel) {
                for (EvacuationAgent agent : generated_agents_step) {
                    ((RunningAroundPerson) agent).setExpectedDensityMacroTimeStep(
                        expectedDensityMacroTimeStep);
                    if (agent.ID == 0) {
                        agent.ID = manualId;
                        manualId += 1;
                    }
                }
            }
        }
        preprocessLinks(time);
        preprocessAgents(time);
        updateLinks(time);
        updateAgents(time);
        updatePollution();
        updateAgentViews();

        /* the following must be here, as direction etc. are
         * calculated in the methods call above, such as updateAgents.
         */
        for (EvacuationAgent agent : generated_agents_step) {
            model.registerAgent(agent);
        }

        if (effectiveLinksEnabled) {
            // エージェントが存在するリンクのリストを更新
            effectiveLinks.clear();
            for (EvacuationAgent agent : agents) {
                if (agent.isEvacuated() || effectiveLinks.contains(agent.getCurrentLink())) continue;
                effectiveLinks.add(agent.getCurrentLink());
            }
            //System.err.println("effectiveLinks / modelLinks: " + effectiveLinks.size() + " / " + model.getLinks().size());
        }
    }

    public boolean isFinished() {
        /* finish when rescue has arrived */
        if (rescueArrived) {
            System.err.println("finished:  rescue arrived");
            return true;
        }
        if (isAllAgentSpeedZeroBreak) {
            for (GenerateAgent factory : generate_agent) {
                if (factory.enabled) return false;
            }
            boolean existNotFinished = false;
            for (final EvacuationAgent agent : agents) {
                if (!agent.finished()) {
                    existNotFinished = true;
                    break;
                }
            }
            if (existNotFinished) {
                if (isAllAgentSpeedZero) {
                    System.err.println("finished:  all agents speed zero");
                    return true;
                }
                return false;
            }
        } else {
            for (final EvacuationAgent agent : agents) {
                if (!agent.finished()) return false;
            }
            for (GenerateAgent factory : generate_agent) {
                if (factory.enabled) return false;
            }
        }
        System.err.println("finished:  no more agents to generate");
        return true;
    }

    private void preprocessLinks(double time) {
        synchronized (model) {
            for (MapLink link : model.getLinks()) {
                link.preUpdate(time);
            }
        }
    }

    private void updateLinks(double time) {
        synchronized (model) {
            for (MapLink link : model.getLinks()) {
                link.update(time);
            }
        }
    }

    private void preprocessAgents(double time) {
        int count = 0;

        if (isExpectedDensitySpeedModel) {
            calcExpectedDensity(time);
        }
        if (true) {
            synchronized (model) {
                for (MapLink link : effectiveLinksEnabled ? effectiveLinks : model.getLinks()) {
                    for (EvacuationAgent agent : link.getLane(1.0)) {
                        agent.preUpdate(time);
                        count += 1;
                    }
                    ArrayList<EvacuationAgent> neg_agents = link.getLane(-1.0);
                    for (int i = neg_agents.size() - 1; i >= 0; --i) {
                        EvacuationAgent agent = neg_agents.get(i);
                        agent.preUpdate(time);
                        count += 1;
                    }
                    /*
                    for (EvacuationAgent agent : link.getAgents()) {
                        agent.preUpdate(time);
                        count += 1;
                    }
                    */
                }
            }
        }
    }

    private void updateAgents(double time) {
        try {
            Thread.sleep(simulation_weight);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int count = 0;
        waitingAgentCount = 0;
        double speedTotal = 0.0;

        if (has_display) {
            String time_string = String.format("Elapsed: %5.2fsec",
                    time);
            time_label.setText(time_string);
            String clock_string = getClockString(time);
            clock_label.setText(clock_string);
            updateEvacuatedCount();
        }

        // tkokada
        boolean existNonZeroSpeedAgent = false;
        for (EvacuationAgent agent : agents) {
            if (agent.isEvacuated())
                continue;

            agent.update(time);

            if (agent.isEvacuated()) {
                final MapNode exit = agent.getPrevNode();
                Integer i = evacuatedAgentCountByExit.get(exit);
                if (i == null)
                    i = new Integer(0);
                i += 1;
                evacuatedAgentCountByExit.put(exit, i);
                evacuated_agents.add(agent);
                if (has_display) {
                    updateEvacuatedCount();
                }
            } else {
                ++count;
                speedTotal += agent.getSpeed();
                // tkokada
                if (!existNonZeroSpeedAgent) {
                    if (agent.getSpeed() > 0.0)
                        existNonZeroSpeedAgent = true;
                }
            }
        }
        // tkokada
        if (existNonZeroSpeedAgent)
            isAllAgentSpeedZero = false;
        else
            isAllAgentSpeedZero = true;
        averageSpeed = speedTotal / count;
    }

    private void updateEvacuatedCount() {
        String evacuatedCount_string = "Evacuated: " + evacuated_agents.size()
            + "/" + (int) this.getMaxAgentCount();
        evacuatedCount_label.setText(evacuatedCount_string);
        SimulationPanel3D.updateEvacuatedCount(evacuatedCount_string);
    }

    private void updateAgentViews() {
        for (EvacuationAgent agent : agents) {
            if (agent.isEvacuated())
                continue;
            agent.updateViews();
        }
    }

    private void updatePollution() {
        /* pollution */
        totalDamage = 0.0;
        maxDamage = 0.0;
        evacuatedAgentCount = 0;
        evacuatedUsedLiftAgentCount = 0;
        evacuatedNoLiftAgentCount = 0;

        for (final EvacuationAgent agent : agents) {
            if (agent.isEvacuated()) {
                evacuatedAgentCount++;
            } else {
                totalDamage += agent.getDamage();
                if (agent.getDamage() > maxDamage) maxDamage = agent.getDamage();
            }
        }
    }

    public void dumpAgentCurrent(PrintStream out) {
        for (int i = 0; i < agents.size(); ++i) {
            EvacuationAgent agent = agents.get(i);
            if (!agent.finished()) {
                out.printf("%d,%f,%f,%f,%f\n",
                        i,
                        agent.getDamage(),
                        agent.getPos().getX(),
                        agent.getPos().getY(),
                        agent.getHeight());
            }
        }
    }

    public void dumpAgentResult(PrintStream out) {
        for (final EvacuationAgent agent : agents) {
            agent.dumpResult(out);
        }
    }

    // tkokada
    /* wrappers of fusion viewer connector */
    public void saveFusionViewerLog(String dir, double time, int count) {
        fusionViewerConnector.saveFusionViewerLog(dir, startTime, time, count,
                agents);
    }
    public void sendFusionViewerLog(double time, int count) {
        fusionViewerConnector.sendFusionViewerLog(startTime, time, count,
                agents);
    }
    public void waitConnectionFusionViewer() {
        fusionViewerConnector.waitConnection();
    }

    // tkokada
    /** Calculate existence probability for expected density speed model.
     * @param time current time
     */
    private void calcExpectedDensity(double time) {
        // existence probabilities for each links
        HashMap<MapLink, Double> existenceProbabilities =
            new HashMap<MapLink, Double>();
        // reachable links for each agents
        HashMap<EvacuationAgent, ArrayList<MapLink>> agentReachableLinks =
            new HashMap<EvacuationAgent, ArrayList<MapLink>>();
        // simplex or duplex for each links
        //  1.0: simplex from -> to
        //  -1.0: simplex to -> from
        //  0.0: duplex
        HashMap<MapLink, Double> linkDirections =
            new HashMap<MapLink, Double>();

        /** An algorithm of expected density speed model.
         *
         * v: speed(m/sec)
         * rho: expected density(person/m^2)
         *
         * duplex raod:
         *   v = V_0 - 0.25 * rho
         *   if v < THRESHOLD_1 then v = V_MIN1
         * simplex road:
         *   v = V_0 - 0.476 * rho
         *   if v < THRESHOLD_2 then v = V_MIN2
         */
        // free speed
        double V_0 = RunningAroundPerson.MAX_SPEED;
        // minimum speed of simplex
        double V_MIN_1 = RunningAroundPerson.ZERO_SPEED;
        // minimum speed of duplex
        double V_MIN_2 = RunningAroundPerson.ZERO_SPEED;
        // minimum speed threshold of simplex
        double THRESHOLD_1 = RunningAroundPerson.ZERO_SPEED;
        // minimum speed threshold of duplex
        double THRESHOLD_2 = RunningAroundPerson.ZERO_SPEED;
        for (EvacuationAgent agent : agents) {
            if (agent.isEvacuated())
                continue;
            ArrayList<MapLink> reachableLinks = ((RunningAroundPerson) agent)
                .getReachableLinks(V_0, time, expectedDensityMacroTimeStep);
            if (reachableLinks == null) {
                System.err.println("AgentHandler.calcExpectedDensity " +
                        "reachableLinks are null!");
                continue;
            // } else {
                // System.err.println("calcExpectedDensity agent: " +
                // agent.ID + " reachable links: " +
                // reachableLinks.toString());
            }
            agentReachableLinks.put(agent, reachableLinks);
            // calc weight of reachable links. closer the link is, greater the 
            // weight is.
            double totalWeightLength = 0;
            for (int i = 0; i < reachableLinks.size(); i++) {
                MapLink link = reachableLinks.get(i);
                totalWeightLength += link.length * (reachableLinks.size() - i
                        + 1);
            }
            for (int i = 0; i < reachableLinks.size(); i++) {
                MapLink link = reachableLinks.get(i);
                double existenceProbability = link.length *
                    (reachableLinks.size() - i + 1) / totalWeightLength;

                if (existenceProbabilities.containsKey(link)) {
                    double currentExistenceProbability =
                        existenceProbabilities.get(link);
                    existenceProbabilities.put(link,
                        currentExistenceProbability + existenceProbability);
                } else {
                    existenceProbabilities.put(link, existenceProbability);
                }
                if (((RunningAroundPerson) agent).reachableLinkDirections
                        .containsKey(link)) {
                    double direction = ((RunningAroundPerson) agent)
                        .reachableLinkDirections.get(link)
                        .doubleValue();
                    if (linkDirections.containsKey(link)) {
                        double storedDirection = linkDirections.get(link)
                            .doubleValue();
                        if (storedDirection == 0.0) { // already duplex
                            continue;
                        } else if (storedDirection != direction) { // be duplex
                            linkDirections.put(link, new Double(0.0));
                        }
                    } else {
                        linkDirections.put(link, new Double(direction));
                    }
                } else {
                    System.err.println("AgentHandler.calcExpectedDensity " +
                            "getReachableLinks failed, reachable link " +
                            "directions could not be calculated correctly.");
                    for (MapLink directionLink : linkDirections.keySet())
                        System.err.println("\tlink: " + directionLink +
                                " direction: " + linkDirections
                                .get(directionLink));
                }
            }
        }
        // calc velocity for each link
        HashMap<MapLink, Double> velocities = new HashMap<MapLink, Double>();
        for (MapLink link : existenceProbabilities.keySet()) {
            double direction = 1.0; // default simplex
            if (linkDirections.containsKey(link)) {
                direction = linkDirections.get(link).doubleValue();
            }
            double velocity = 0.0;
            if (direction == 0.0) { // duplex
                velocity = V_0 - 0.238 * existenceProbabilities.get(link) /
                    link.length / link.width;
                if (velocity < THRESHOLD_2)
                    velocity = V_MIN_2;
            } else {    // simplex
                velocity = V_0 - 0.125 * existenceProbabilities.get(link) /
                    link.length / link.width;
                if (velocity < THRESHOLD_1)
                    velocity = V_MIN_1;
            }
            velocities.put(link, velocity);
        }
        // set existence probability
        for (EvacuationAgent agent : agents) {
            HashMap<MapLink, Double> expectedDensitySpeeds = new
                    HashMap<MapLink, Double>();
            ArrayList<MapLink> reachableLinks = agentReachableLinks.get(agent);
            if (reachableLinks == null)
                continue;
            for (MapLink link : reachableLinks) {
                if (velocities.containsKey(link)) {
                    double speed = ((Double) velocities.get(link))
                        .doubleValue();
                    if (speed == 0.0)
                        speed = V_MIN_1;
                    if (speed >= V_0)
                        speed = V_0;
                    // Improve the speed depending on the front agent.
                    if (agent.getCurrentLink() == link) {
                        double distance_to_front =
                            ((RunningAroundPerson) agent).calcDistanceToFront();
                        if (distance_to_front == 0.0) {
                            speed = V_0;
                        } else {
                            double tmp_speed = V_0 * (V_0 *
                                    model.getTimeScale() - distance_to_front) /
                                (V_0 * model.getTimeScale());
                            if (tmp_speed > speed) {
                                speed = tmp_speed;
                            }
                        }
                    }
                    //expectedDensitySpeeds.put(link,
                    //        ((Double) velocities.get(link)).doubleValue());
                    expectedDensitySpeeds.put(link, speed);
                }
                else
                    System.err.println("AgentHandler.calcExpectedDensity " +
                            "velocities don't contain match velocity for " +
                            "link: " + link);
            }
            // ((RunningAroundPerson) agent).reachableLinks = reachableLinks;
            ((RunningAroundPerson) agent).expectedDensitySpeeds =
                expectedDensitySpeeds;
            // System.err.println("calcExpectedDensity : " +
            // expectedDensitySpeeds.toString());
        }
    }

    /* Accessors
     */
    public enum EvacuationType {
        ALL, USED_LIFT, NO_LIFT
    };

    public int getEvacuated(EvacuationType t) {
        switch (t) {
        case ALL:
            return evacuatedAgentCount;
        case USED_LIFT:
            return evacuatedUsedLiftAgentCount;
        case NO_LIFT:
            return evacuatedNoLiftAgentCount;
        default:
            return 0;
        }
    }
    public ArrayList<String> getAllGoalTags() {
        ArrayList<String> all_goal_tags = new ArrayList<String>();
        for (EvacuationAgent agent : agents) {
            String goal_tag = agent.getGoal();
            if (goal_tag != null &&
                    !all_goal_tags.contains(goal_tag)) {
                all_goal_tags.add(goal_tag);
            }
            for (String mid_goal : agent.getPlannedRoute()) {
                if (!all_goal_tags.contains(mid_goal)) {
                    all_goal_tags.add(mid_goal);
                }
            }
        }

        for (GenerateAgent factory : generate_agent) {
            String goal_tag = factory.goal;
            if (goal_tag != null &&
                    !all_goal_tags.contains(goal_tag)) {
                all_goal_tags.add(goal_tag);
            }
            for (String mid_goal : factory.getPlannedRoute()) {
                if (!all_goal_tags.contains(mid_goal)) {
                    all_goal_tags.add(mid_goal);
                }
            }
        }
        return all_goal_tags;
    }

    public double getOutbreakTime() {
        return outbreakTime - startTime;
    }

    public int getEvacuatedCount(MapNode node) {
        return evacuatedAgentCountByExit.get(node);
    }

    public int getWaiting() {
        return waitingAgentCount;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public double getMaxDamage() {
        return maxDamage;
    }

    //public final ArrayList<EvacuationAgent> getAgents() {
    public final List<EvacuationAgent> getAgents() {
        return agents;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public String timeToString(double clock_time) {
        double clock_sec = clock_time % 60;
        clock_time -= clock_sec;
        clock_time /= 60;
        double clock_min = clock_time % 60;
        clock_time -= clock_min;
        clock_time /= 60;

        return String.format("%02d:%02d:%02.0f",
                (int)clock_time, (int)clock_min, clock_sec);
    }

    public String getStatisticsDescription() {
        /* 各被害レベルの人数 (0, 1, 2, 3)
         * 平均避難時間　　
         * 避難終了時間
         */
        int[] each_level = new int[4];
        double finish_total = 0.0, finish_max = Double.NEGATIVE_INFINITY;
        int count_all = 0, count_evacuated = 0;
        for (EvacuationAgent ea : getAgents()) {
            count_all++;
            RunningAroundPerson rp = (RunningAroundPerson)ea;
            each_level[rp.getTriage()]++;
            double t = rp.finishedTime;
            if (t == 0.0) continue;
            count_evacuated++;
            finish_total += t;
            if (t > finish_max) finish_max = t;
        }
        return ""
        + each_level[0] + ","
        + each_level[1] + ","
        + each_level[2] + ","
        + each_level[3] + ","
        + count_evacuated + ","
        + count_all + ","
        + finish_total / count_evacuated + ","
        + finish_max
        ;
    }

    protected transient JPanel control_panel = null;
    private transient JLabel clock_label = new JLabel("NOT STARTED1");
    private transient JLabel time_label = new JLabel("NOT STARTED2");
    private transient JLabel evacuatedCount_label = new JLabel("NOT STARTED3");
    private ArrayList<ButtonGroup> toggle_scenario_button_groups = new
        ArrayList<ButtonGroup>();
    private transient JTextArea message =
        new JTextArea("UNMaps Version 1.9.5\n");

    public JPanel getControlPanel() {
        return control_panel;
    }

    private void setup_control_panel(String generationFileName,
            String responseFileName,
            String scenario_number,
            ArrayList<MapLink> links) {
        control_panel = new JPanel();
        control_panel.setName("Control");
        control_panel.setLayout(new BorderLayout());

        JPanel top_panel = new JPanel();
        top_panel.setLayout(new BorderLayout());

        /* title & clock */
        JPanel titlepanel = new JPanel(new GridLayout(4, 2));
        titlepanel.add(new JLabel("Agent"));
        if (generationFileName != null) {
            File generation_file = new File(generationFileName);
            titlepanel.add(new JLabel(generation_file.getName()));
        } else {
            titlepanel.add(new JLabel("No generation file"));
        }
        titlepanel.add(new JLabel("Scenario"));

        if (responseFileName != null) {
            File response_file = new File(responseFileName);
            titlepanel.add(new JLabel(response_file.getName()));
        } else {
            titlepanel.add(new JLabel("No response file"));
        }

        titlepanel.add(new JLabel("ID"));
        if (scenario_number != null) {
            titlepanel.add(new JLabel(scenario_number));
        } else {
            JLabel sl = new JLabel("(NOT DEFINED)");
            sl.setFont(new Font(null, Font.ITALIC, 9));
            titlepanel.add(sl);
        }

        clock_label.setHorizontalAlignment(JLabel.CENTER);
        clock_label.setFont(new Font("Lucida", Font.BOLD, 18));
        titlepanel.add(clock_label, BorderLayout.CENTER);
        time_label.setHorizontalAlignment(JLabel.LEFT);
        time_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        titlepanel.add(time_label, BorderLayout.CENTER);
        evacuatedCount_label.setHorizontalAlignment(JLabel.LEFT);
        evacuatedCount_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        titlepanel.add(evacuatedCount_label, BorderLayout.CENTER);
        top_panel.add(titlepanel, BorderLayout.NORTH);

        /* scenarios */
        JPanel label_toggle_panel = new JPanel(new GridBagLayout());
        label_toggle_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        GridBagConstraints c = null;

        int max_events = 0;
        for (ScenarioEvent event : events.values()) {
            if (event.getMaxIndex() > max_events) {
                max_events = event.getMaxIndex();
            }
        }

        int y = 0;
        for (ScenarioEvent event : events.values()) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.fill = GridBagConstraints.WEST;

            // label_toggle_panel.add(new JLabel(event.comment), c);
            ButtonGroup bgroup = new ButtonGroup();
            toggle_scenario_button_groups.add(bgroup);
            class RadioButtonListener implements ActionListener {
                int index;
                ScenarioEvent event;
                ArrayList<MapLink> links;
                public RadioButtonListener(ScenarioEvent _event,
                        int _index,
                        ArrayList<MapLink> _links) {
                    event = _event;
                    index = _index;
                    links = _links;
                }
                public void actionPerformed(ActionEvent e) {
                    if (index == -2) {
                        event.setEnabled(true, links);
                    } else if (index == -1) {
                        event.setEnabled(false, links);
                    } else {
                        event.setIndex(index);
                    }
                }
            }

            if (event.isAbsolute()) {
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y;
                c.gridwidth = max_events + 3;
                c.fill = GridBagConstraints.EAST;
                label_toggle_panel.add(new JLabel(timeToString(event.getClock())), c);
            } else {
                JRadioButton radio_button;
                radio_button = new JRadioButton("enabled");
                radio_button.addActionListener(new RadioButtonListener(event, -2, links));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button = new JRadioButton("disabled | auto:");
                radio_button.addActionListener(new RadioButtonListener(event, -1, links));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                
                for (int i = 0; i < event.getMaxIndex(); ++i) {
                    radio_button = new JRadioButton(timeToString(event.getTimeIndex(i)));
                    radio_button.addActionListener(new RadioButtonListener(event, i, links));
                    bgroup.add(radio_button);
                    c = new GridBagConstraints();
                    c.gridx = i + 3;
                    c.gridy = y;
                    label_toggle_panel.add(radio_button, c);

                    if (event.getIndex() == i) radio_button.setSelected(true); 
                }
            }
            y++;
        }
        top_panel.add(label_toggle_panel, BorderLayout.CENTER);
        
        JPanel control_button_panel = new JPanel(new FlowLayout());
        ImageIcon start_icon = new ImageIcon("img/start.png");
        ImageIcon pause_icon = new ImageIcon("img/pause.png");
        ImageIcon step_icon = new ImageIcon("img/step.png");

        start_button = new JButton(start_icon);
        start_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { model.start(); update_buttons(); }
        });
        control_button_panel.add(start_button);
        pause_button = new JButton(pause_icon);
        pause_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { model.pause(); update_buttons(); }
        });
        control_button_panel.add(pause_button);
        step_button = new JButton(step_icon);
        step_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { model.step(); update_buttons(); }
        });
        control_button_panel.add(step_button);
        update_buttons();

        control_button_panel.add(new JLabel("weight: "));
        simulation_weight_control = new JScrollBar(JScrollBar.HORIZONTAL, (int) simulation_weight,
                1, 0, 1000);
        simulation_weight_control.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) { change_simulation_weight(); }
        });
        simulation_weight_control.setPreferredSize(new Dimension(100, 20));
        control_button_panel.add(simulation_weight_control);
        simulation_weight_value = new JLabel();
        simulation_weight_value.setHorizontalAlignment(JLabel.RIGHT);
        simulation_weight_value.setPreferredSize(new Dimension(30, 10));
        simulation_weight_value.setText("" + simulation_weight);
        control_button_panel.add(simulation_weight_value);

        top_panel.add(control_button_panel, BorderLayout.SOUTH);
        control_panel.add(top_panel, BorderLayout.NORTH);
            
        /* text message */
        message.setEditable(false);
        message.setAutoscrolls(true);
        JScrollPane message_scroller = new JScrollPane(message,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        message_scroller.setPreferredSize(new Dimension(300, 84));

        control_panel.add(message_scroller, BorderLayout.CENTER);
    }
    
    private void change_simulation_weight() {
        simulation_weight = simulation_weight_control.getValue();
        simulation_weight_value.setText("" + simulation_weight);
    }

    private void update_buttons() {
        boolean is_running = model.isRunning();
        if (start_button != null) {
            start_button.setEnabled(!is_running);
        }
        if (pause_button != null) {
            pause_button.setEnabled(is_running);
        }
        if (step_button != null) {
            step_button.setEnabled(!is_running);
        }
    }

    public double getMaxAgentCount() {
        return maxAgentCount;
    }

    public String getClockString(double clock) {
        return timeToString(clock + startTime);
    }

    public void dumpState(PrintWriter pw) {
        DaRuMaClient dc = new DaRuMaClient();
        Document doc = dc.newDocument();
        model.getMap().toDOM(doc);
        pw.print(dc.docToString(doc));

        for (EvacuationAgent agent : agents) {
            if (agent.isEvacuated()){
                pw.print("evacuated," + 
                        agent.agentNumber + "," +
                        agent.finishedTime + "," +
                        agent.getPrevNode().getTagString()
                );
            } else {
                pw.print(agent.toString());
            }
            pw.println();
        }
        for (GenerateAgent factory : generate_agent) {
            factory.dumpAgentToGenerate(pw);
        }
    }

    public void dumpStateDiff(PrintWriter pw) {
        for (EvacuationAgent agent : agents) {
            if (agent.isEvacuated()) continue;
            pw.println(agent.toString());
        }

        for (EvacuationAgent agent : generated_agents) {
            pw.print("generated," +
                    agent.agentNumber + "," +
                    agent.getClass().getSimpleName());
            if (agent instanceof RunningAroundPerson) {
                for (String route : ((RunningAroundPerson)agent).getPlannedRoute()) {
                    pw.print("," + route);
                }
            }
            pw.println();
        }
        generated_agents.clear();

        for (EvacuationAgent agent : evacuated_agents) {
            pw.println("evacuated," + agent.agentNumber);
        }
        evacuated_agents.clear();
    }

    public void setRandom(Random _random) {
        random = _random;
        if (generate_agent != null)
            generate_agent.setRandom(_random);
    }

    public boolean getIsAllAgentSpeedZero() {
        return isAllAgentSpeedZero;
    }

    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        this.isAllAgentSpeedZeroBreak = _isAllAgentSpeedZeroBreak;
    }

    public void setRandomNavigation(boolean _randomNavigation) {
        for (EvacuationAgent agent : agents) {
            agent.setRandomNavigation(_randomNavigation);
        }
        for (EvacuationAgent agent : generated_agents) {
            agent.setRandomNavigation(_randomNavigation);
        }
        randomNavigation = _randomNavigation;
    }

    public boolean getRandomNavigation() {
        return randomNavigation;
    }

    protected boolean isExpectedDensitySpeedModel = false;
    public void setIsExpectedDensitySpeedModel(boolean
            _isExpectedDensitySpeedModel) {
        isExpectedDensitySpeedModel = _isExpectedDensitySpeedModel;
    }

    public boolean getIsExpectedDensitySpeedModel() {
        return isExpectedDensitySpeedModel;
    }

    protected int nextExpectedDensityMacroTime = 0;

    protected int expectedDensityMacroTimeStep = 300;
    public void setExpectedDensityMacroTimeStep(int
            _expectedDensityMacroTimeStep) {
        expectedDensityMacroTimeStep = _expectedDensityMacroTimeStep;
        // if (generate_agent != null) {
            // for (GenerateAgent factory : generate_agent) {
                // factory.setDuration((double) expectedDensityMacroTimeStep);
            // }
        // }
    }

    public int getExpectedDensityMacroTimeStep() {
        return expectedDensityMacroTimeStep;
    }

    protected boolean isExpectedDensityVisualizeMicroTimeStep = false;
    public void setIsExpectedDensityVisualizeMicroTimeStep(boolean
            _isExpectedDensityVisualizeMicroTimeStep) {
        isExpectedDensityVisualizeMicroTimeStep =
            _isExpectedDensityVisualizeMicroTimeStep;
    }

    public boolean getIsExpectedDensityVisualizeMicroTimeStep() {
        return isExpectedDensityVisualizeMicroTimeStep;
    }

    public void setLinerGenerateAgentRatio(double _ratio) {
        generate_agent.setLinerGenerateAgentRatio(_ratio);
    }
}
