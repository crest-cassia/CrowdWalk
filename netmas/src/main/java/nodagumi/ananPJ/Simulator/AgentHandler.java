// -*- mode: java; indent-tabs-mode: nil -*-
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
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Formatter;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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

import javax.vecmath.Vector3d;

import org.w3c.dom.Document;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;
import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.misc.AgentGenerationFile;
import nodagumi.ananPJ.misc.GenerateAgent;
import nodagumi.ananPJ.misc.Scenario ;
import nodagumi.ananPJ.network.DaRuMaClient;
import nodagumi.ananPJ.network.FusionViewerConnector;

import nodagumi.Itk.*;

public class AgentHandler implements Serializable {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agent の queue を処理する際に、先頭・末尾のどちらから処理するか
     * を示すフラグ。
     * MapLink の中でのソートでは、エージェントの進んだ距離に対して、
     * 昇順になるようになっている。
     * つまり、進んでいないエージェント（列の末尾にいるエージェント）が
     * queue (agents という変数)の先頭に来る。
     * このフラグは、update, preupdate において、
     * この queue を前から処理するか、後ろから処理するかを指定する。
     * true の場合、後ろから（つまり agent の列として先頭から）処理する。
     * 本来、これはかならず true であるべきだが、
     * 2014年12月までの実装での状態を残すため、
     * フラグで切り替えられるようにしておく。
     */
    // static private boolean usingFrontFirstOrderQueue = false ;
    static private boolean isUsingFrontFirstOrderQueue = true ;

    //============================================================
    //------------------------------------------------------------
    /**
     * switching queue order
     * @param flag : if true, use front_first order.
     *               else, use rear_first order.
     */
    static public void useFrontFirstOrderQueue(boolean flag) {
        isUsingFrontFirstOrderQueue = flag ;
    }

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
    private int maxAgentCount = 0;
    private boolean isAllAgentSpeedZeroBreak = false;
    private boolean isAllAgentSpeedZero = false;

    private transient JButton start_button = null;
    private transient JButton pause_button = null;
    private transient JButton step_button = null;

    private int simulation_weight = 0;
    private transient JScrollBar simulation_weight_control;
    private transient JLabel simulation_weight_value;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シナリオ情報
     */
    Scenario scenario = new Scenario();

    private double startTime = 0.0;

    private HashMap<MapNode, Integer> evacuatedAgentCountByExit;
    private AgentGenerationFile generate_agent = null;

    boolean has_display;
    private Random random = null;
    private FusionViewerConnector fusionViewerConnector = null;

    // プログラム制御用環境変数
    public String envSwitch = "";
    // エージェントが存在するリンクのリスト
    private MapLinkTable effectiveLinks = null;
    private boolean effectiveLinksEnabled = false;

    private Logger agentMovementHistoryLogger = null;
    private Logger individualPedestriansLogger = null;

    public AgentHandler (ArrayList<EvacuationAgent> _agents,
            String generationFile,
            String responseFile,
            String scenario_number,
            NetworkMapBase map,
            EvacuationModelBase _model,
            boolean _has_display,
            double linerGenerateAgentRatio,
            Random _random) {
        model = _model;
        has_display = _has_display;
        random = _random;

        evacuatedAgentCountByExit = new LinkedHashMap<MapNode, Integer>();

        /* clone all agents already on board */
        agents = new ArrayList<EvacuationAgent>();
        for (final EvacuationAgent agent : _agents) {
            agents.add(agent.copyAndInitialize());
        }
        generated_agents = new ArrayList<EvacuationAgent>();
        evacuated_agents = new ArrayList<EvacuationAgent>();

        for (EvacuationAgent agent : agents) {
            MapLink link = agent.getCurrentLink(); 
            link.agentEnters(agent);
        }

        try {
            /* [I.Noda] generation file の読み込みはここ */
            generate_agent = new AgentGenerationFile(generationFile,
                                                     model.getMap(), has_display,
                                                     linerGenerateAgentRatio, 
                                                     random);
        } catch(Exception ex) {
            ex.printStackTrace() ;
            System.err.printf("Illegal AgentGenerationFile: %s\n%s", generationFile, ex.getMessage());
            System.exit(1);
        }
        if (generate_agent != null) {
            for (GenerateAgent factory : generate_agent) {
                maxAgentCount += factory.getMaxGeneration();
            }
        }
        parseResponseFile(responseFile);

        if (has_display) {
            setup_control_panel(generationFile,
                    responseFile,
                    scenario_number,
                    map);
        }
        fusionViewerConnector = new FusionViewerConnector();

        envSwitch = System.getenv("NETMAS");
        if (envSwitch == null)
            envSwitch = "";
        if (envSwitch.indexOf("effectiveLinks") != -1)
            effectiveLinksEnabled = true;
    }

    public void deserialize(String generationFile, String responseFile,
            String scenario_number, NetworkMapBase map) {
        control_panel = null;
        clock_label = new JLabel("NOT STARTED");
        time_label = new JLabel("NOT STARTED!");
        evacuatedCount_label = new JLabel("NOT STARTED");
        message = new JTextArea("UNMaps Version 1.9.5\n");
        setup_control_panel(generationFile,
                responseFile,
                scenario_number,
                map);
    }

    public void prepareForSimulation() {
        for (EvacuationAgent agent : agents) {
            agent.prepareForSimulation(model.getTimeScale());
        }
        // 初回は全リンクを対象とする
        effectiveLinks = (MapLinkTable)model.getLinks().clone();
    }

    private void setup_default_scenario() {
        startTime = 0;
    }

    private void parseResponseFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            setup_default_scenario();
            return;
        }

        scenario.scanCsvFile(filename) ;
        scenario.describe() ;
        startTime = scenario.getOriginTime() ;
    }

    /* need stable design to assign id */
    private int manualId = 1024;
    public void update(NetworkMapBase map, double time) {
        update_buttons();

        scenario.advance(time, map) ;

        ArrayList<EvacuationAgent> generated_agents_step = new
            ArrayList<EvacuationAgent>();

        if (generate_agent != null) {
            for (GenerateAgent factory : generate_agent) {
                factory.tryUpdateAndGenerate(time + startTime,
                        model.getTimeScale(),
                        time, model, generated_agents_step);
            }
        }

        if (! generated_agents_step.isEmpty()) {
            agents.addAll(generated_agents_step);
            generated_agents.addAll(generated_agents_step);
            if (effectiveLinksEnabled) {
                for (EvacuationAgent agent : generated_agents_step) {
                    if (agent.isEvacuated() || effectiveLinks.contains(agent.getCurrentLink())) continue;
                    effectiveLinks.add(agent.getCurrentLink());
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
        if (scenario.isFinished()) {
            Itk.dbgMsg("finished by the end of scenario.") ;
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

        if (true) {
            synchronized (model) {
                for (MapLink link : effectiveLinksEnabled ? effectiveLinks : model.getLinks()) {
                    ArrayList<EvacuationAgent> pos_agents = link.getLane(1.0);
                    for(int i = 0 ; i < pos_agents.size() ; i++) {
                        EvacuationAgent agent =
                            (isUsingFrontFirstOrderQueue ?
                             pos_agents.get(pos_agents.size() - i - 1) :
                             pos_agents.get(i)) ;

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
        /* [2015.01.08 I.Noda]
         * agents の queue は、現状で逆順に並んでいるので、
         * 念の為、逆順に処理をすることにする。
         * 過去の順番での処理にも switch できるように、
         * isUsingFrontFirstOrderQueue を参照する。
         */
        for(int k = 0 ; k < agents.size() ; k++) {
            EvacuationAgent agent =
                (isUsingFrontFirstOrderQueue ?
                 agents.get(agents.size() - k - 1) :
                 agents.get(k)) ;

            if (agent.isEvacuated())
                continue;

            agent.update(time);

            if (agent.isEvacuated()) {
                final MapNode exit = agent.getLastNode() ;
                Integer i = evacuatedAgentCountByExit.get(exit);
                if (i == null)
                    i = new Integer(0);
                i += 1;
                evacuatedAgentCountByExit.put(exit, i);
                evacuated_agents.add(agent);
                if (has_display) {
                    updateEvacuatedCount();
                }
                if (agentMovementHistoryLogger != null) {
                    agentMovementHistoryLogger.info(String.format("%s,%d,%s,%d,%s,%d,%s,%d", agent.getConfigLine().replaceAll(",", " "), agent.ID, timeToString(startTime + agent.generatedTime, true), (int)agent.generatedTime, timeToString(startTime + time, true), (int)time, timeToString(time - agent.generatedTime, true), (int)(time - agent.generatedTime)));
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
            logIndividualPedestrians(time, agent);
        }
        // tkokada
        if (existNonZeroSpeedAgent)
            isAllAgentSpeedZero = false;
        else
            isAllAgentSpeedZero = true;
        averageSpeed = speedTotal / count;
    }

    public static String[] TRIAGE_LABELS = {"GREEN", "YELLOW", "RED", "BLACK"};

    private void logIndividualPedestrians(double time, EvacuationAgent agent) {
        if (individualPedestriansLogger != null) {
            StringBuilder buff = new StringBuilder();
            if (agent.isEvacuated()) {
                buff.append(agent.ID); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(-1); buff.append(",");
                buff.append(-1); buff.append(",");
                buff.append(agent.getLastNode().ID); buff.append(",");
                buff.append(0.0); buff.append(",");
                buff.append(0); buff.append(",");
                buff.append((int)agent.generatedTime); buff.append(",");
                buff.append((int)time); buff.append(",");
                buff.append(agent.currentExposureAmount); buff.append(",");
                buff.append(agent.accumulatedExposureAmount); buff.append(",");
                buff.append(TRIAGE_LABELS[agent.getTriage()]); buff.append(",");
            } else {
                buff.append(agent.ID); buff.append(",");

                buff.append(agent.getPos().getX()); buff.append(",");
                buff.append(agent.getPos().getY()); buff.append(",");
                buff.append(agent.getHeight()); buff.append(",");

                Vector3d swing = agent.getSwing();
                double height = agent.getHeight() / ((MapPartGroup)agent.getCurrentLink().getParent()).getScale();
                buff.append(agent.getPos().getX() + swing.x); buff.append(",");
                buff.append(agent.getPos().getY() + swing.y); buff.append(",");
                buff.append(height + swing.z); buff.append(",");

                buff.append(agent.getAcceleration()); buff.append(",");
                buff.append(agent.getSpeed()); buff.append(",");

                buff.append(agent.getCurrentLink().ID); buff.append(",");
                buff.append(agent.getNextNode().ID); buff.append(",");
                buff.append(agent.getPrevNode().ID); buff.append(",");
                buff.append(agent.getRemainingDistance()); buff.append(",");
                buff.append((int)agent.getDirection()); buff.append(",");

                buff.append((int)agent.generatedTime); buff.append(",");
                buff.append((int)time); buff.append(",");

                buff.append(agent.currentExposureAmount); buff.append(",");
                buff.append(agent.accumulatedExposureAmount); buff.append(",");
                buff.append(TRIAGE_LABELS[agent.getTriage()]); buff.append(",");

                buff.append(((RunningAroundPerson)agent).getNextCandidateString());
            }
            individualPedestriansLogger.info(buff.toString());
        }
    }

    private void updateEvacuatedCount() {
        String evacuatedCount_string = String.format("Walking: %d  Generated: %d  Evacuated: %d / %d", 
            getAgents().size() - evacuated_agents.size(), getAgents().size(), evacuated_agents.size(),
            getMaxAgentCount());
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
                totalDamage += agent.accumulatedExposureAmount;
                if (agent.accumulatedExposureAmount > maxDamage) maxDamage = agent.accumulatedExposureAmount;
            }
        }
    }

    public void dumpAgentCurrent(PrintStream out) {
        for (int i = 0; i < agents.size(); ++i) {
            EvacuationAgent agent = agents.get(i);
            if (!agent.finished()) {
                out.printf("%d,%f,%f,%f,%f\n",
                        i,
                        agent.accumulatedExposureAmount,
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
            Term goal_tag = agent.getGoal();
            if (goal_tag != null &&
                !all_goal_tags.contains(goal_tag.getString())) {
                all_goal_tags.add(goal_tag.getString());
            }
            for (Term mid_goal : agent.getPlannedRoute()) {
                if (!all_goal_tags.contains(mid_goal.getString())) {
                    all_goal_tags.add(mid_goal.getString());
                }
            }
        }

        for (GenerateAgent factory : generate_agent) {
            Term goal_tag = factory.goal;
            if (goal_tag != null &&
                !all_goal_tags.contains(goal_tag.getString())) {
                all_goal_tags.add(goal_tag.getString());
            }
            for (Term mid_goal : factory.getPlannedRoute()) {
                if (!all_goal_tags.contains(mid_goal.getString())) {
                    all_goal_tags.add(mid_goal.getString());
                }
            }
        }
        return all_goal_tags;
    }

    public int getEvacuatedCount(MapNode node) {
        return evacuatedAgentCountByExit.get(node);
    }

    public HashMap<MapNode, Integer> getExitNodesMap() {
        return evacuatedAgentCountByExit;
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

    public String timeToString(double clock_time, boolean trunc) {
        double sec = clock_time % 60;
        int time = (int)clock_time / 60;
        int min = time % 60;
        time /= 60;
        if (trunc) {
            time %= 24;
        }
        return String.format("%02d:%02d:%02.0f", time, min, sec);
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

    public Logger initLogger(String name, Level level, java.util.logging.Formatter formatter, String filePath) {
        Logger logger = Logger.getLogger(name);
        try {
            // 「ロックファイルが残っているとログファイルに連番が振られて増え続けてしまう」不具合を回避する
            File file = new File(filePath + ".lck");
            if (file.exists()) {
                System.err.println("delete lock file: " + filePath + ".lck");
                file.delete();
            }

            FileHandler handler = new FileHandler(filePath);
            handler.setFormatter(formatter);
            logger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.setLevel(level);
        return logger;
    }

    public void closeLogger(Logger logger) {
        if (logger != null) {
            for (Handler handler : logger.getHandlers()) {
                handler.close();
            }
        }
    }

    public void initAgentMovementHistorLogger(String name, String filePath) {
        agentMovementHistoryLogger = initLogger(name, Level.INFO, new java.util.logging.Formatter() {
            public String format(final LogRecord record) {
                return formatMessage(record) + "\n";
            }
        }, filePath);
        agentMovementHistoryLogger.setUseParentHandlers(false); // コンソールには出力しない
        agentMovementHistoryLogger.info("GenerationFileの情報,エージェントID,発生時刻1,発生時刻2,到着時刻1,到着時刻2,移動時間1,移動時間2");
    }

    public void closeAgentMovementHistorLogger() {
        closeLogger(agentMovementHistoryLogger);
    }

    private String individualPedestriansLogDir = null;
    public void initIndividualPedestriansLogger(String name, String dirPath) {
        individualPedestriansLogDir = dirPath;
        individualPedestriansLogger = initLogger(name, Level.INFO, new java.util.logging.Formatter() {
            public String format(final LogRecord record) {
                return formatMessage(record) + "\n";
            }
        }, dirPath + "/log_individual_pedestrians.csv");
        individualPedestriansLogger.setUseParentHandlers(false); // コンソールには出力しない
        individualPedestriansLogger.info("pedestrianID,current_position_in_model_x,current_position_in_model_y,current_position_in_model_z,current_position_for_drawing_x,current_position_for_drawing_y,current_position_for_drawing_z,current_acceleration,current_velocity,current_linkID,current_nodeID_of_forward_movement,current_nodeID_of_backward_movement,current_distance_from_node_of_forward_movement,current_moving_direction,generated_time,current_traveling_period,current_exposure,amount_exposure,current_status_by_exposure,next_assigned_passage_node");
    }

    public void closeIndividualPedestriansLogger() {
        if (individualPedestriansLogger == null) {
            return;
        }
        closeLogger(individualPedestriansLogger);

        // log_individual_pedestrians_initial.csv
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(individualPedestriansLogDir + "/log_individual_pedestrians_initial.csv"), "utf-8"));
            writer.write("pedestrianID,pedestrian_moving_model,generated_time,current_traveling_period,distnation_nodeID,assigned_passage_nodes\n");
            for (EvacuationAgent agent : agents) {
                StringBuilder buff = new StringBuilder();
                buff.append(agent.ID); buff.append(",");
                buff.append(((RunningAroundPerson)agent).getSpeedCalculationModel().toString().replaceFirst("Model$", "")); buff.append(",");
                buff.append((int)agent.generatedTime); buff.append(",");
                buff.append(model.getTimeScale()); buff.append(",");
                buff.append(agent.getLastNode().ID); buff.append(",");
                int idx = 0;
                for (Term route : agent.getPlannedRoute()) {
                    if (idx > 0) {
                        buff.append(" ");
                    }
                    idx++;
                    buff.append(route);
                }
                buff.append("\n");
                writer.write(buff.toString());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected transient JPanel control_panel = null;
    private transient JLabel clock_label = new JLabel("NOT STARTED1");
    private transient JLabel time_label = new JLabel("NOT STARTED2");
    private transient JLabel evacuatedCount_label = new JLabel("NOT STARTED3");
    private ArrayList<ButtonGroup> toggle_scenario_button_groups = new
        ArrayList<ButtonGroup>();
    private transient JTextArea message = new JTextArea("UNMaps Version 1.9.5\n") {
        @Override
        public void append(String str) {
            super.append(str);
            message.setCaretPosition(message.getDocument().getLength());
        }
    };

    public JPanel getControlPanel() {
        return control_panel;
    }

    public JButton getStartButton() {
        return start_button;
    }

    // GridBagLayout のパネルにラベルを追加する
    private void addJLabel(JPanel panel, int x, int y, int width, int height, int anchor, JLabel label) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = anchor;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.insets = new Insets(0, 12, 0, 12);
        ((GridBagLayout)panel.getLayout()).setConstraints(label, gbc);
        panel.add(label);
    }

    private void addJLabel(JPanel panel, int x, int y, int width, int height, JLabel label) {
        addJLabel(panel, x, y, width, height, GridBagConstraints.WEST, label);
    }

    private void setup_control_panel(String generationFileName,
            String responseFileName,
            String scenario_number,
            NetworkMapBase map) {
        control_panel = new JPanel();
        control_panel.setName("Control");
        control_panel.setLayout(new BorderLayout());

        JPanel top_panel = new JPanel();
        top_panel.setLayout(new BorderLayout());

        /* title & clock */
        JPanel titlepanel = new JPanel(new GridBagLayout());
        addJLabel(titlepanel, 0, 0, 1, 1, GridBagConstraints.EAST, new JLabel("Map"));
        if (model.getMap().getFileName() != null) {
            File map_file = new File(model.getMap().getFileName());
            addJLabel(titlepanel, 1, 0, 1, 1, new JLabel(map_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 0, 1, 1, new JLabel("No map file"));
        }

        addJLabel(titlepanel, 0, 1, 1, 1, GridBagConstraints.EAST, new JLabel("Agent"));
        if (generationFileName != null) {
            File generation_file = new File(generationFileName);
            addJLabel(titlepanel, 1, 1, 1, 1, new JLabel(generation_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 1, 1, 1, new JLabel("No generation file"));
        }

        addJLabel(titlepanel, 0, 2, 1, 1, GridBagConstraints.EAST, new JLabel("Scenario"));
        if (responseFileName != null) {
            File response_file = new File(responseFileName);
            addJLabel(titlepanel, 1, 2, 1, 1, new JLabel(response_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 2, 1, 1, new JLabel("No response file"));
        }

        addJLabel(titlepanel, 0, 3, 1, 1, GridBagConstraints.EAST, new JLabel("Pollution"));
        if (model.getMap().getPollutionFile() != null) {
            File pollution_file = new File(model.getMap().getPollutionFile());
            addJLabel(titlepanel, 1, 3, 1, 1, new JLabel(pollution_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 3, 1, 1, new JLabel("No pollution file"));
        }

        addJLabel(titlepanel, 0, 4, 1, 1, GridBagConstraints.EAST, new JLabel("ID"));
        if (scenario_number != null) {
            addJLabel(titlepanel, 1, 4, 1, 1, new JLabel(scenario_number));
        } else {
            JLabel sl = new JLabel("(NOT DEFINED)");
            sl.setFont(new Font(null, Font.ITALIC, 9));
            addJLabel(titlepanel, 1, 4, 1, 1, sl);
        }

        clock_label.setHorizontalAlignment(JLabel.CENTER);
        clock_label.setFont(new Font("Lucida", Font.BOLD, 18));
        addJLabel(titlepanel, 0, 5, 1, 1, GridBagConstraints.CENTER, clock_label);

        time_label.setHorizontalAlignment(JLabel.LEFT);
        time_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJLabel(titlepanel, 1, 5, 1, 1, time_label);

        evacuatedCount_label.setHorizontalAlignment(JLabel.LEFT);
        evacuatedCount_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJLabel(titlepanel, 0, 6, 2, 1, GridBagConstraints.CENTER, evacuatedCount_label);
        top_panel.add(titlepanel, BorderLayout.NORTH);

        /* scenarios */
        JPanel label_toggle_panel = new JPanel(new GridBagLayout());
        label_toggle_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        GridBagConstraints c = null;

        int max_events = 1;

        int y = 0;
        for (Scenario.EventBase event : scenario.eventList) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.fill = GridBagConstraints.WEST;

            ButtonGroup bgroup = new ButtonGroup();
            toggle_scenario_button_groups.add(bgroup);
            class RadioButtonListener implements ActionListener {
                int index;
                Scenario.EventBase event ;
                NetworkMapBase map ;
                public RadioButtonListener(Scenario.EventBase _event,
                        int _index,
                        NetworkMapBase _map) {
                    event = _event;
                    index = _index;
                    map = _map;
                }
                public void actionPerformed(ActionEvent e) {
                    if (index == -2) {
                        event.occur(0, map) ;
                    } else if (index == -1) {
                        event.unoccur(0, map) ;
                    } else {
                        Itk.dbgErr("wrong index") ;
                    }
                }
            }

            if (false) {
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y;
                c.gridwidth = max_events + 3;
                c.fill = GridBagConstraints.EAST;
                label_toggle_panel.add(new JLabel(timeToString(event.getAbsoluteTime())), c);
            } else {
                JRadioButton radio_button;
                radio_button = new JRadioButton("enabled");
                radio_button.addActionListener(new RadioButtonListener(event, -2, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button = new JRadioButton("disabled | auto:");
                radio_button.addActionListener(new RadioButtonListener(event, -1, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button = new JRadioButton(timeToString(event.getAbsoluteTime())) ;
                radio_button.addActionListener(new RadioButtonListener(event, 0, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 3;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button.setSelected(true);
            }
            y++;
        }
        JScrollPane scroller = new JScrollPane(label_toggle_panel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(400, 230));
        top_panel.add(scroller, BorderLayout.CENTER);
        
        JPanel control_button_panel = new JPanel(new FlowLayout());
        ImageIcon start_icon = new ImageIcon(getClass().getResource("/img/start.png"));
        ImageIcon pause_icon = new ImageIcon(getClass().getResource("/img/pause.png"));
        ImageIcon step_icon = new ImageIcon(getClass().getResource("/img/step.png"));

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
        simulation_weight_control = new JScrollBar(JScrollBar.HORIZONTAL, simulation_weight, 1, 0, 1000);
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
        control_panel.add(top_panel, BorderLayout.CENTER);
            
        /* text message */
        message.setEditable(false);
        message.setAutoscrolls(true);
        JScrollPane message_scroller = new JScrollPane(message,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        message_scroller.setPreferredSize(new Dimension(300, 160));

        control_panel.add(message_scroller, BorderLayout.SOUTH);
    }
    
    private void change_simulation_weight() {
        simulation_weight = simulation_weight_control.getValue();
        simulation_weight_value.setText("" + simulation_weight);
    }

    public void setSimulationWeight(int weight) {
        simulation_weight = weight;
        simulation_weight_control.setValue(simulation_weight);
    }

    public void update_buttons() {
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

    public int getMaxAgentCount() {
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
                        agent.getLastNode().getTagString()
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
                for (Term route : ((RunningAroundPerson)agent).getPlannedRoute()) {
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

    public void setLinerGenerateAgentRatio(double _ratio) {
        generate_agent.setLinerGenerateAgentRatio(_ratio);
    }
}
