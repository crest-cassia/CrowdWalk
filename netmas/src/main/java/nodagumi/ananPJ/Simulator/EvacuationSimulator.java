package nodagumi.ananPJ.Simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.navigation.CalcPath;
import nodagumi.ananPJ.navigation.Dijkstra;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;

public class EvacuationSimulator implements EvacuationModelBase, Serializable {
    public static enum timeSeriesLogTYpe {
        All,        /* save all agents, map information */
        Goal        /* save goal times of agents */
    }

    private ArrayList<MapNode> nodes = null;
    private ArrayList<MapLink> links = null;
    private ArrayList<EvacuationAgent> agents = null;
    private String pollutionFileName = null;

    transient private SimulationPanel3D panel3d = null;
    protected SimulationController controller = null;

    private int screenshotInterval = 0;
    private int outlogInterval = 0;
    /** simulation time step */
    private double timeScale = 1.0; // original value: 1.0
    private String scenario_serial = null;

    protected double tick_count = 0.0;

    private PollutionCalculator pollutionCalculator = null;
    private AgentHandler agentHandler = null;

    static public boolean debug = false;
    private NetworkMap networkMap = null;
    private double linerGenerateAgentRatio = 1.0;
    private Random random = null;
    // saveTimeSeriesLog() が呼ばれた回数
    private int logging_count = 0;

    public EvacuationSimulator(NetworkMap _networkMap,
            SimulationController _controller,
            String _scenario_serial,
            Random _random) {
        networkMap = _networkMap;
        controller = _controller;
        nodes = networkMap.getNodes();
        links = networkMap.getLinks();
        agents = networkMap.getAgents();
        pollutionFileName = networkMap.getPollutionFile();
        scenario_serial = _scenario_serial;
        random = _random;
        //bug check
        //System.err.println("EvacuationSimulator check invalid links");
        int counter = 0;
        for (MapLink link : links) {
            if (!nodes.contains(link.getFrom()))
                counter += 1;
            else if (!nodes.contains(link.getTo()))
                counter += 1;
        }
        if (counter > 0)
            System.err.println("EvacuationSimulator invalid links: " +
                    counter);
    }

    public EvacuationSimulator(NetworkMap networkMap, Random _random) {
        this(networkMap, null, null, _random);
    }

    public void deserialize(NetworkMap _networkMap,
            SimulationController _controller,
            String _scenario_serial) {
        networkMap = _networkMap;
        controller = _controller;
        nodes = networkMap.getNodes();
        links = networkMap.getLinks();
        agents = networkMap.getAgents();
        pollutionFileName = networkMap.getPollutionFile();
        scenario_serial = _scenario_serial;
    }

    public void begin(boolean has_display) {
        buildModel(has_display);
        buildRoutes();
        buildDisplay(null);

        agentHandler.prepareForSimulation();

        add_tags_to_nodes_and_links();

        File log_directory = new File("logs");
        if (!log_directory.isDirectory() &&
                !log_directory.mkdir()) {
            JOptionPane.showMessageDialog(null,
                    "Cannot create directory for log output.",
                    "error creating directory",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void begin(boolean has_display, boolean isDeserialized) {
        if (!isDeserialized) {
            buildModel (has_display);
            buildRoutes ();
        } else {
            String scenario_number = null;
            Pattern numonly = Pattern.compile("^\\d*$");
            Matcher matcher = numonly.matcher(scenario_serial);
            if (matcher.matches()) {
                scenario_number = scenario_serial;
            }
            agentHandler.deserialize(
                    networkMap.getGenerationFile(),
                    networkMap.getResponseFile(),
                    scenario_number,
                    links);
        }
        if (has_display)
            buildDisplay();

        agentHandler.prepareForSimulation();
    }

    public void begin(boolean has_display, boolean isDeserialized,
            SimulationPanel3D _panel3d) {
        if (!isDeserialized) {
            buildModel (has_display);
            buildRoutes ();
        } else {
            String scenario_number = null;
            Pattern numonly = Pattern.compile("^\\d*$");
            Matcher matcher = numonly.matcher(scenario_serial);
            if (matcher.matches()) {
                scenario_number = scenario_serial;
            }
            agentHandler.deserialize(
                    networkMap.getGenerationFile(),
                    networkMap.getResponseFile(),
                    scenario_number,
                    links);
        }
        if (has_display)
            buildDisplay(_panel3d);

        agentHandler.prepareForSimulation();
    }

    void buildModel(boolean has_display) {
        buildMap();
        pollutionCalculator = new PollutionCalculator(
                pollutionFileName,
                networkMap.getRooms(),
                timeScale);
        String scenario_number = null;
        Pattern numonly = Pattern.compile("^\\d*$");
        Matcher matcher = numonly.matcher(scenario_serial);
        if (matcher.matches()) {
            scenario_number = scenario_serial;
        }
        agentHandler = new AgentHandler(agents,
                networkMap.getGenerationFile(),
                networkMap.getResponseFile(),
                scenario_number,
                links,
                this,
                has_display,
                linerGenerateAgentRatio,
                random);

        for (EvacuationAgent agent : getAgents()) {
            agent.displayMode = 4;
        }
    }

    void buildDisplay() {
        panel3d = controller.setupFrame(this);
    }

    void buildDisplay(SimulationPanel3D _panel3d) {
        if (controller != null) {
            if (_panel3d != null) {
                panel3d = controller.setupFrame(this, _panel3d);
                /*
                System.err.println("EvacuationSimulator.buildDisplay " +
                        "nonnull panel3d: " + panel3d);
                */
            } else {
                panel3d = controller.setupFrame(this, _panel3d);
                /*
                System.err.println("EvacuationSimulator.buildDisplay " +
                        "is null panel3d: " + panel3d);
                */
            }
        }
    }

    public SimulationPanel3D getPanel3D() {
        return panel3d;
    }

    public void registerAgent(EvacuationAgent agent) {
        if (panel3d != null) {
            panel3d.registerAgentOnline(agent);
        }
        //agents.add(agent);
        //getMap().addAgent((MapPartGroup) links.get(0).getParent(), agent);
        if (links.size() > 0) {
            getMap().addAgent((MapPartGroup) links.get(0).getParent(), agent);
        //} else {
        //    getMap().addAgent((MapPartGroup) getMap().root, agent);
        }
    }

    Boolean stop_simulation = false;
    public boolean updateEveryTick() {
        synchronized (stop_simulation) {
            // double poltime = getSecond() - agentHandler.getOutbreakTime();
            double poltime = getSecond();
            if (!(pollutionFileName == null || pollutionFileName.isEmpty()))
                pollutionCalculator.updateNodesLinksAgents(poltime, nodes, links,
                        getAgents());
            // Runtime.getRuntime().gc();
            agentHandler.update(getNodes(), getLinks(), getSecond());
            if (panel3d != null) {
                panel3d.updateClock(getSecond());
                if (screenshotInterval != 0) {
                    if ((((int)getTickCount()) % screenshotInterval) == 0) {
                        String filename = String.format("screenshots/" +
                                // "capture%06d.jpg", (int)getTickCount());
                                "capture%06d.png", (int)getTickCount());
                        panel3d.captureNextFrame(filename);
                    }
                }
                if (outlogInterval == 0) {
                    //do nothing
                } else if ((((int)getTickCount()) % outlogInterval) == 0) {
                    try {
                        PrintStream psa = new PrintStream("logs/agents" +
                                getTickCount()+".csv");
                        agentHandler.dumpAgentCurrent(psa);
                        psa.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            tick_count += 1.0;
        }
        if (agentHandler.isFinished()) {
            // output_results();
            return true;
        } else {
            return false;
        }
    }

    public boolean updateEveryTickCui() {
        synchronized (stop_simulation) {
            // double poltime = getSecond() - agentHandler.getOutbreakTime();
            double poltime = getSecond();
            pollutionCalculator.updateNodesLinksAgents(poltime, nodes, links,
                    getAgents());
            // Runtime.getRuntime().gc();
            agentHandler.update(getNodes(), getLinks(), getSecond());
            tick_count += 1.0;
        }
        if (agentHandler.isFinished()) {
            return true;
        } else {
            return false;
        }
    }

    public void updateWithSerialize() {
        synchronized (stop_simulation) {
            // double poltime = getSecond() - agentHandler.getOutbreakTime();
            double poltime = getSecond();
            pollutionCalculator.updateNodesLinksAgents(poltime, nodes, links,
                    getAgents());
            // Runtime.getRuntime().gc();
            agentHandler.update(getNodes(), getLinks(), getSecond());
            if (panel3d != null) {
                panel3d.updateClock(getSecond());
                for (EvacuationAgent ea : getAgents())
                    panel3d.registerAgentOnline(ea);
            }
        }
    }

    protected void output_results() {
        try {
            PrintStream ps = new PrintStream("logs/"+scenario_serial+".log");
            agentHandler.dumpAgentResult(ps);
            ps.close();

            File macro = new File("logs/macro.log");
            PrintWriter pw = new PrintWriter(new FileWriter(macro, true));
            pw.print(scenario_serial + ",");
            pw.println(agentHandler.getStatisticsDescription());
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void add_tags_to_nodes_and_links() {
        for (MapPartGroup group : networkMap.getGroups()) {
            String tag_header = group.getTagString();
            int count = 1;
            for (MapNode node : group.getChildNodes()) {
                node.addTag(tag_header + "-N" + count);
                ++count;
            }
            count = 1;
            for (MapLink link : group.getChildLinks()) {
                link.addTag(tag_header + "-L" + count);
                ++count;
            }
        }
    }
    
    private ArrayList<String> validRouteKeys;
    public boolean isValidRouteKey(String route) { return validRouteKeys.contains(route); }

    class CalcGoalPath implements Runnable {
        public boolean goalCalculated = false;
        public String goalTag;
        
        public CalcGoalPath(String _goal_tag) {
            goalTag = _goal_tag;
        }
        
        public void run() {
            goalCalculated = calc_goal_path(goalTag);
        }

        private boolean calc_goal_path(String goal_tag) {
            CalcPath.Nodes goals = new CalcPath.Nodes();
            for (MapNode node : nodes) {
                if (node.hasTag(goal_tag)) goals.add(node);
            }
            for (MapLink link : links) {
                if (link.hasTag(goal_tag)) {
                    goals.add(link.getFrom());
                    goals.add(link.getTo());
                }
            }
            if (goals.size() == 0) {
                System.err.println("goal:" + goal_tag + " no goal!!");
                return false;
            }

            validRouteKeys.add(goal_tag);

            Dijkstra.Result result = Dijkstra.calc(goals,
                    new PathChooser() {
                public double evacuationRouteCost(MapLink link) {
                    //if (link.isStair()) return 5.0;
                    return 1.0;
                }
                public boolean isExit(MapLink link) {
                    return false;
                }
                public double initialCost(MapNode node) {
                    return 0.0;
                }
            });
            /*
            System.err.println("EvacuationSimulator.calc_goal_path result: ");
            for (MapNode node : result.keySet()) {
                System.err.print("\t" + node.getTags() + " - ");
                if (result.get(node).node != null)
                    System.err.print(result.get(node).node.getTags() + " - ");
                else
                    System.err.print("null - ");
                if (result.get(node).link != null)
                    System.err.print(result.get(node).link.ID + " - ");
                else
                    System.err.print("null - ");
                System.err.println(result.get(node).len);
            }
            */
            for (MapNode node : result.keySet()) {
                NodeLinkLen nll = result.get(node);
                node.addNavigationHint(goal_tag,
                        new NavigationHint(nll.node, nll.link, nll.len));
                /*
                System.err.println("EvacuationSimulator call " +
                        "addNavigationHint node:" + node.ID + " " + 
                        node.getTags() + ", " + goal_tag);
                */
            }
            System.err.println("goal:" + goal_tag);
            return true;
        }
    }

    private void buildRoutes() {
        /* evacuation based on goal tags */
        validRouteKeys = new ArrayList<String>();;
        ArrayList<String> all_goal_tags = agentHandler.getAllGoalTags();

        if (!all_goal_tags.contains("EXIT")) {
            all_goal_tags.add("EXIT");
        }

        ArrayList<String> no_goal_list = new ArrayList<String>();

        for (String goal_tag : all_goal_tags) {
            CalcGoalPath worker = new CalcGoalPath(goal_tag);
            worker.run();
            if (!worker.goalCalculated) {
                no_goal_list.add(worker.goalTag);
            }
        }

        if (no_goal_list.size() != 0) {
            System.err.println("WARNING: no nodes with the following tag " +
                    "was found");
            for (String tag : no_goal_list) {
                System.err.print(" " + tag);
            }
            System.err.println();
        }

        /* emergency evacuation */
        {
            CalcPath.Nodes goals = new CalcPath.Nodes();
            for (MapNode node : nodes) {
                if (node.hasTag("EXIT")) goals.add(node);
            }
            if (goals.size() == 0) {

                System.err.println("no goal for Exit!");
                return;
            }
            validRouteKeys.add("EMERGENCY");
            Dijkstra.Result result = Dijkstra.calc(goals,
                    new PathChooser() {
                public double evacuationRouteCost(MapLink link) {
                    if (link.hasTag("HOTSPOT")) return 10000.0;
                    else if (link.hasTag("WARMSPOT")) return 100.0;
                    return 1.0;
                }
                public boolean isExit(MapLink link) {
                    return false;
                }
                public double initialCost(MapNode node) {
                    return 0.0;
                }
            });

            for (MapNode node : result.keySet()) {
                NodeLinkLen nll = result.get(node);
                node.addNavigationHint("EMERGENCY",
                        new NavigationHint(nll.node, nll.link, nll.len));
            }
        }
        // tkokada: node check which no way to goal
        boolean hasGoal = true;
        ArrayList<Integer> numNoGoal = new ArrayList<Integer>();
        int totalValidNodes = 0;
        for (String goal : all_goal_tags) {
            int count = 0;
            for (MapNode node : nodes) {
                ArrayList<MapLink> pathways = node.getPathways();
                boolean notHasAllLinks = false;
                for (MapLink link : pathways) {
                    if (!link.hasTag("ALL_LINKS")) {
                        notHasAllLinks = true;
                        break;
                    }
                }
                if (notHasAllLinks)
                    continue;
                if (goal.equals("EXIT"))
                    totalValidNodes += 1;
                if (node.getHint(goal) == null) {
                    System.err.println("EvacuationSimulator.buildRoute: " +
                            "node:" + node.ID + " does not have any routes" +
                            " for " + goal);
                    hasGoal = false;
                    count += 1;
                    if (pathways.size() > 0) {
                        //((MapLink) pathways.get(0)).addTag("INVALID_ROUTE");
                        for (MapLink link : pathways) {
                            link.addTag("INVALID_ROUTE");
                        }
                    }
                }
            }
            numNoGoal.add(count);
        }
        // check whether all nodes have complete path to goals
        System.err.println("EvacuationSimulator.buildRoute: bug cheking...");
        for (int i = 0; i < all_goal_tags.size(); i++)
            System.err.println("EvacuationSimulator.buildRoute: goal " +
                    all_goal_tags.get(i) + " includes " + 
                    numNoGoal.get(i) + " no goal nodes / all nodes" +
                    totalValidNodes + ".");

        // check VIEWPOINT
        /*
        int count = 0;
        int totalcount= 0;
        for (MapNode node : nodes) {
            if (!((String) ((MapPartGroup) node.getParent()).toString())
                    .contains("ALL_LINKS"))
                continue;
            totalcount += 1;
            if (node.getHint("VIEWPOINT") == null) {
                System.err.println("EvacuationSimulator.buildRoute: " +
                        "node:" + node.ID + " does not have any routes" +
                        " for VIEWPOINT");
                hasGoal = false;
                count += 1;
            }
        }
        System.err.println("EvacuationSimulator.buildRoute: invalid for " +
                "VIEWPOINT: " + count + " total: " + totalcount);
        */
    }

    void resetValues() {
        // System.gc();

        tick_count = 0.0;
        logging_count = 0;

        for (MapLink link : getLinks()) {
            link.clear();
        }
    }

    void buildMap () {
        /* Nodes */
        for (MapNode node : getNodes()) {
            node.displayMode = 4;
        }
        /* Links */
        for (MapLink link : getLinks()) {
            link.prepareForSimulation(timeScale, 4);
        }
    }

    public double getSecond() {
        return getTickCount() * timeScale;
    }

    @Override
    public NetworkMap getMap() {
        return networkMap;
    }

    public void setScreenshotInterval (int i) {
        screenshotInterval = i;
    }

    public int getScreenshotInterval () {
        return screenshotInterval;
    }

    public void setOutlogInterval (int i) {
        outlogInterval = i;
    }

    public int getOutlogInterval () {
        return outlogInterval;
    }

    public void setTimeScale (double d) {
        timeScale = d;
    }

    public double getTimeScale () {
        return timeScale;
    }

    public SimulationController getController() {
        return controller;
    }

    public String getName() {
        return "Evacuation";
    }

    public void setup() {
        resetValues();
    }

    public ArrayList<MapLink> getLinks() {
        return links;
    }

    public ArrayList<MapNode> getNodes() {
        return nodes;
    }

    //public ArrayList<EvacuationAgent> getAgents() {
    public List<EvacuationAgent> getAgents() {
        return agentHandler.getAgents();
    }

    public AgentHandler getAgentHandler() {
        return agentHandler;
    }

    public ArrayList<PollutedArea> getPollutions() {
        return pollutionCalculator.getPollutions();
    }

    @Override
    public void recalculatePaths() {
        synchronized (stop_simulation) {
            stop_simulation = true;
            buildRoutes();
            stop_simulation = false;
        }
    }

    @Override
    public double getTickCount() {
        return tick_count;
    }

    @Override
    public void start() {
        if (controller != null) {
            controller.start();
            if (panel3d != null && isRunning()) {
                panel3d.setMenuActionStartEnabled(false);
            }
        }
    }

    @Override
    public void pause() {
        if (controller != null) {
            controller.pause();
            if (panel3d != null && ! isRunning()) {
                panel3d.setMenuActionStartEnabled(true);
            }
        }
    }

    @Override
    public void step() {
        if (controller != null) {
            controller.step();
        }
    }

    @Override
    public boolean isRunning() {
        // tkokada
        if (controller != null) {
            return controller.isRunning();
        } else {
            return false;
        }
    }

    @Override
    public int getDisplayMode() {
        return 4;
    }

    public String getScenario_serial() {
        return scenario_serial;
    }

    public void setRandom(Random _random) {
        random = _random;
        if (agentHandler != null)
            agentHandler.setRandom(_random);
    }

    public void setRandomNavigation(boolean _randomNavigation) {
        if (agentHandler != null) {
            agentHandler.setRandomNavigation(_randomNavigation);
        } else {
            System.err.println("EvacuationSimulator.setRandomNavigation " +
                    "agentHandler is null object.");
        }
    }

    public void saveSimpleGoalLog(String dict, boolean createfile) {
        File fileLog = new File(dict + "/goalLog.log");
        if (createfile) {
            File fileLogDirectory = fileLog.getParentFile();
            if (fileLogDirectory != null && !fileLogDirectory.exists())
                fileLogDirectory.mkdirs();
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(fileLog,
                            !createfile) , "utf-8")), true);
            writer.write(((int) getSecond()) + "," +
                    agentHandler.getEvacuated(AgentHandler.EvacuationType.ALL) + "\n");
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /* when agents goal */
    private HashMap<Integer, Double> goalTimes = new HashMap<Integer, Double>();
    // EXITノード毎の避難完了者数(ログのバッファリング用)
    private ArrayList<String> evacuatedAgents = new ArrayList<String>();
    private ArrayList<MapNode> exitNodeList = null;
    /** Save the goal log file to result directory.
     * @param resultDirectory: path to the result directory.
     */
    public void saveGoalLog(String resultDirectory, Boolean finished) {
        // Goal log を記憶
        for (EvacuationAgent agent: agents) {
            if (agent.isEvacuated() && !goalTimes.containsKey(agent.ID)) {
                goalTimes.put(new Integer(agent.ID), new Double(getSecond()));
            }
        }

        // evacuatedAgents log を記憶
        if (logging_count == 0) {
            exitNodeList = new ArrayList<MapNode>(agentHandler.getExitNodesMap().keySet());
        }
        StringBuffer buff = new StringBuffer();
        int index = 0;
        for (MapNode node : exitNodeList) {
            if (index > 0) {
                buff.append(",");
            }
            buff.append(agentHandler.getExitNodesMap().get(node));
            index++;
        }
        evacuatedAgents.add(buff.toString());

        if (finished) {  // finalize process
            for (EvacuationAgent agent : agents) {
                if (!agent.isEvacuated()) {
                    goalTimes.put(new Integer(agent.ID),
                            new Double((getTickCount() + 1) * timeScale));
                }
            }
            List<Map.Entry> entries = new ArrayList<Map.Entry>(goalTimes.entrySet());
            // 避難完了時刻順にソート
            Collections.sort(entries, new Comparator<Map.Entry>() {
                public int compare(Map.Entry ent1, Map.Entry ent2) {
                    return (Double)ent1.getValue() > (Double)ent2.getValue() ? 1 : 0;
                }
            });

            // Goal log を出力
            File fileLog = new File(resultDirectory + "/goalLog.log");
            File fileLogDirectory = fileLog.getParentFile();
            if (fileLogDirectory != null && !fileLogDirectory.exists())
                fileLogDirectory.mkdirs();
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(fileLog,
                                false) , "utf-8")), true);
                writer.write("# agent_id,goal_time\n");
                // for (Integer id : goalTimes.keySet()) {
                    // writer.write(id + "," + goalTimes.get(id) + "\n");
                // }
                for (Map.Entry entry : entries) {
                    writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // EXITノード毎の避難完了者数ログを出力
            File evacuatedAgentsLog = new File(resultDirectory + "/evacuatedAgents.csv");
            try {
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(evacuatedAgentsLog, false), "utf-8")), true);
                index = 0;
                for (MapNode node : exitNodeList) {
                    writer.write((index == 0 ? "" : ",") + node.getTagLabel());
                    index++;
                }
                writer.write("\n");
                for (String line : evacuatedAgents) {
                    writer.write(line + "\n");
                }
                writer.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        logging_count++;
    }

    /** Save the time series log file to result directory.
     * @param resultDirectory: path to result directory.
     * @return returned value: succeed or not.
     */
    public boolean saveTimeSeriesLog(String resultDirectory) {
        int count = (int) getSecond();      // loop counter
        double totalLinkDensity = 0.0;
        double totalAgentDensity = 0.0;
        double totalAgentSpeed = 0.0;
        // time series log file
        //File fileLog = new File(resultDirectory + "/timeSeries/" + count + ".log");
        // ファイル名の左側が必ず6桁になるように"0"を付加する("1.log" -> "000001.log")
        File fileLog = new File(String.format("%s/timeSeries/%06d.log", resultDirectory, count));
        // summary log file
        File summaryLog = new File(resultDirectory + "/summary.log");
        File fileLogDirectory = fileLog.getParentFile();
        File summaryLogDirectory = summaryLog.getParentFile();
        if (fileLogDirectory != null && !fileLogDirectory.exists())
            fileLogDirectory.mkdirs();
        if (summaryLogDirectory != null && !summaryLogDirectory.exists())
            summaryLogDirectory.mkdirs();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(fileLog, false) , "utf-8")),
                    true);
            /*if (resultType == timeSeriesLogType.Goal) {
                for (EvacuationAgent agent : agents) {
                    RunningAroundPerson rap = (RunningAroundPerson) agent;
                    // log format:
                    // agent,ID,evacuated,speed,density,position
                    writer.write("agent," + rap.ID + "," + rap.isEvacuated() +
                            "," + rap.getSpeed() + "," + rap.getDensity() + "," +
                            rap.getPosition() + "\n");
                }
            } else {*/
            for (EvacuationAgent agent : agents) {
                RunningAroundPerson rap = (RunningAroundPerson) agent;
                // agent log format:
                // agent,ID,evacuated,speed,density,position
                writer.write("agent," + rap.ID + "," + rap.isEvacuated() +
                        "," + rap.getSpeed() + "," + rap.getDensity() + "," +
                        rap.getPosition() + "\n");
                totalAgentDensity += rap.getDensity();
                totalAgentSpeed += rap.getSpeed();
            }
            for (MapLink link : links) {
                // link log format:
                // link,ID,positive_agents,negative_agents,density
                double linkDensity = (link.getLane(1.0).size() +
                        link.getLane(-1.0).size()) /
                    (link.length * link.width);
                writer.write("link," + link.ID + "," +
                        link.getLane(1.0).size() + "," +
                        link.getLane(-1.0).size() + "," +
                        linkDensity + "\n");
                totalLinkDensity += linkDensity;
            }
            // EXITノード毎の避難完了者数
            for (Map.Entry<MapNode, Integer> e : agentHandler.getExitNodesMap().entrySet()) {
                writer.write("node," + e.getKey().getTagLabel() + "," + e.getValue() + "\n");
            }
            writer.close();

            writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(summaryLog, true), "utf-8")),
                    true);
            // count,count,average_agent_density,average_agent_speed,
            // average_link_density
            double average_link_density, average_agent_density,
                   average_agent_speed = 0.0;
            if (agents.size() == 0) {
                average_agent_density = 0.0;
                average_agent_speed = 0.0;
            } else {
                average_agent_density = totalAgentDensity / agents.size();
                average_agent_speed = totalAgentSpeed / agents.size();
            }
            if (links.size() == 0)
                average_link_density = 0.0;
            else
                average_link_density = totalLinkDensity / links.size();
            writer.write("count," + count + "," + average_agent_density + "," +
                    average_agent_speed + "," + average_link_density + "\n");
            writer.close();

            // EXITノード毎の避難完了者数ログファイル
            File evacuatedAgentsLog = new File(resultDirectory + "/evacuatedAgents.csv");
            if (logging_count == 0) {
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(evacuatedAgentsLog, false), "utf-8")), true);
                exitNodeList = new ArrayList<MapNode>(agentHandler.getExitNodesMap().keySet());
                int index = 0;
                for (MapNode node : exitNodeList) {
                    writer.write((index == 0 ? "" : ",") + node.getTagLabel());
                    index++;
                }
                writer.write("\n");
            } else {
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(evacuatedAgentsLog, true), "utf-8")), true);
            }
            int index = 0;
            for (MapNode node : exitNodeList) {
                writer.write((index == 0 ? "" : ",") + agentHandler.getExitNodesMap().get(node));
                index++;
            }
            writer.write("\n");
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        logging_count++;

        return true;
    }

    // tkokada
    public void saveDamagedSpeedZeroNumberLog(String filepath,
            boolean createfile) {
        int dszn = 0;   // number of damaged speed zero agents
        // count the number of agents with speed is zero and instantaneous 
        // damage is max value.
        for (EvacuationAgent agent: agents) {
            if (agent.getSpeed() == 0.) {
                // System.err.println("\tanget: " + agent.ID + ", damage: " +
                        // agent.getInstantaneousDamage() + ", density: " +
                        // ((RunningAroundPerson) agent).getDensity());
                if (agent.getInstantaneousDamage() >= 10.) {
                    dszn += 1;
                }
            }
        }
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(filepath, !createfile), "utf-8")),
                        true);
            writer.write(dszn + "\n");
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // tkokada
    public boolean getIsAllAgentSpeedZero() {
        if (agentHandler == null) {
            System.err.println("AgentHandler.isAllAgentsSpeedZero " +
                    "agentHandler is null object.");
            return false;
        } else {
            return agentHandler.getIsAllAgentSpeedZero();
        }
    }

    public boolean getIsAllAgentSpeedZeroBreak() {
        if (agentHandler == null) {
            System.err.println("AgentHandler.getIsAllAgentsSpeedZeroBreak " +
                    "agentHandler is null object.");
            return false;
        } else {
            return agentHandler.getIsAllAgentSpeedZeroBreak();
        }
    }

    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        if (agentHandler == null) {
            System.err.println("AgentHandler.setIsAllAgentsSpeedZeroBreak " +
                    "agentHandler is null object.");
        } else {
            agentHandler.setIsAllAgentSpeedZeroBreak(
                    _isAllAgentSpeedZeroBreak);
        }
    }

    public void setLinerGenerateAgentRatio(double _ratio) {
        linerGenerateAgentRatio = _ratio;
    }
}
