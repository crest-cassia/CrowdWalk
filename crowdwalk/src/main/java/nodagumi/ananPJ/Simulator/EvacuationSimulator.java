// -*- mode: java; indent-tabs-mode: nil -*-
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
import java.lang.ClassNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.BasicSimulationLauncher;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimClock;
import nodagumi.ananPJ.Simulator.SimulationController;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.ananPJ.Scenario.*;

import nodagumi.Itk.*;

//======================================================================
/**
 * シミュレータの実行部。
 * しかし、実際には、AgentHandler がほとんどのことをしている。
 */
public class EvacuationSimulator {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 共通の乱数生成器。
     */
    private Random random = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * マップおよびその他のデータ管理の構造体。
     */
    private NetworkMap networkMap = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 全エージェントを保持。
     */
    private ArrayList<AgentBase> agents = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントの生成管理から、シミュレーションのほとんどの計算を管理。
     */
    private AgentHandler agentHandler = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント生成の倍率。
     * 今後はできるだけ使わない。
     */
    private double linerGenerateAgentRatio = 1.0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シミュレーション内の時刻管理。
     */
    public SimClock clock = new SimClock() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * simulation time step
     */
    private double timeScale = 1.0; // original value: 1.0

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 何回シミュレーションが回ったかを保持する。
     */
    protected double tick_count = 0.0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 親launcher
     */
    protected BasicSimulationLauncher launcher = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * start/pause の排他制御用。
     * synchronize で使うもの。
     */
    Boolean stop_simulation = false;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Pollution を管理。
     */
    private PollutionHandler pollutionHandler = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Pollution のファイル名。
     */
    private String pollutionFileName = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シナリオ情報
     */
    private Scenario scenario = new Scenario();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 表示画面
     */
    transient private SimulationPanel3D panel3d = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * スクリーンショット取るもの。
     * どう使うのか謎。見るところ、単に 1 にするかどうかだけに見える。
     * それならば、int ではなく boolean にすべき。
     */
    private int screenshotInterval = 0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シミュレーションのサイクル間のスリープ。
     */
    private int simulationDeferFactor = 0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * saveTimeSeriesLog() が呼ばれた回数
     */
    private int logging_count = 0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シミュレーション設定ファイルを扱う。
     */
    private CrowdWalkPropertiesHandler properties = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントがゴールに達した時刻情報。
     * saveGoalLog() 用
     */
    private HashMap<String, Double> goalTimes = new HashMap<String, Double>();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * EXITノード毎の避難完了者数(ログのバッファリング用)
     * saveGoalLog() 用
     */
    private ArrayList<String> evacuatedAgents = new ArrayList<String>();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * EXITノードリスト。
     * saveGoalLog() 用
     */
    private MapNodeTable exitNodeList = null;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * CrowdWalk を起動するスクリプトの場所からの相対パス。
     */
    static public String Fallback_rubyLibDir = "./src/main/ruby" ;

    /**
     * ruby の初期設定ファイル。
     */
    static public String Fallback_rubyInitFile = "initForCrowdWalk.rb" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ruby 実行系。
     */
    private ItkRuby rubyEngine = null ;

    /**
     * ruby 用 LOAD_PATH。
     */
    private ArrayList<String> rubyLoadPath = null ;

    /**
     * ruby 用 ライブラリへのパス。
     */
    private String rubyLibDir = Fallback_rubyLibDir ;

    /**
     * ruby 初期化ファイル。
     */
    private String rubyInitFile = Fallback_rubyInitFile ;

    /**
     * ruby 用 simulation wrapper クラス名。
     */
    private String rubyWrapperClass = null ;

    /**
     * ruby 用 simulation wrapper のインスタンス。
     * シミュレーションの cycle の前後で、
     * preCycle(relTime), postCycle(relTime) が呼び出される。
     */
    private Object rubyWrapper = null ;


    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public EvacuationSimulator(NetworkMap _networkMap,
                               BasicSimulationLauncher _launcher,
                               Random _random) {
        init(_networkMap, _launcher, _random) ;
    }

    //------------------------------------------------------------
    // メンバ変数アクセス。
    //------------------------------------------------------------
    /**
     * マップ取得。
     */
    public NetworkMap getMap() {
        return networkMap;
    }

    //------------------------------------------------------------
    /**
     * リンク取得。
     */
    public MapLinkTable getLinks() {
	return networkMap.getLinks();
    }

    //------------------------------------------------------------
    /**
     * ノード取得。
     */
    public MapNodeTable getNodes() {
	return networkMap.getNodes();
    }

    //------------------------------------------------------------
    /**
     * すべてのエージェントのリスト（Collection）を返す。
     * @return Agent の Collection
     */
    public Collection<AgentBase> getAllAgentCollection() {
        return agentHandler.getAllAgentCollection() ;
    }

    //------------------------------------------------------------
    /**
     * 歩いているエージェントのリスト（Collection）を返す。
     * @return Agent の Collection
     */
    public Collection<AgentBase> getWalkingAgentCollection() {
        return agentHandler.getWalkingAgentCollection() ;
    }

    //------------------------------------------------------------
    /**
     * エージェントハンドラ取得。
     */
    public AgentHandler getAgentHandler() {
        return agentHandler;
    }

    //------------------------------------------------------------
    /**
     * シミュレーションサイクル。
     */
    public double getTickCount() {
        return tick_count;
    }

    //------------------------------------------------------------
    /**
     * time scale 設定。
     */
    public void setTimeScale (double d) {
        timeScale = d;
    }

    /**
     * time scale 取得。
     */
    public double getTimeScale () {
        return timeScale;
    }

    //------------------------------------------------------------
    /**
     * 相対時刻。
     */
    public double getSecond() {
        return getTickCount() * timeScale;
    }

    //------------------------------------------------------------
    /**
     * 相対時刻から絶対時刻への変換。
     */
    public double calcAbsoluteTime(double relTime) {
        return agentHandler.calcAbsoluteTime(relTime) ;
    }

    //------------------------------------------------------------
    /**
     * 絶対時刻から相対時刻への変換。
     */
    public double calcRelativeTime(double absTime) {
        return agentHandler.calcRelativeTime(absTime) ;
    }
    
    //------------------------------------------------------------
    /**
     * Pollution ハンドラ取得。
     */
    public ArrayList<MapArea> getPollutions() {
        return pollutionHandler.getPollutions();
    }

    //------------------------------------------------------------
    /**
     * screen shot インターバルを設定。
     */
    public void setScreenshotInterval (int i) {
        screenshotInterval = i;
    }

    /**
     * screen shot インターバル取得。
     */
    public int getScreenshotInterval () {
        return screenshotInterval;
    }

    //------------------------------------------------------------
    /**
     * 設定ハンドラ設定。
     */
    public void setProperties(CrowdWalkPropertiesHandler _properties) {
        properties = _properties;
    }

    /**
     * 設定ハンドラ取得。
     */
    public CrowdWalkPropertiesHandler getProperties() {
        return properties;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成器
     */
    public void setRandom(Random _random) {
        random = _random;
        if (agentHandler != null)
            agentHandler.setRandom(_random);
    }

    /**
     * 乱数生成器
     */
    public Random getRandom() {
        return random ;
    }

    //------------------------------------------------------------
    /**
     * ruby Wrapper を呼ぶべきかどうか
     */
    private boolean useRubyWrapper() {
        return rubyWrapper != null ;
    }

    //------------------------------------------------------------
    /**
     * pollution を使うかどうか
     */
    private boolean usePollution() {
        return !(pollutionFileName == null || pollutionFileName.isEmpty()) ;
    }

    //------------------------------------------------------------
    /**
     * scenario へのアクセス。
     */
    public Scenario getScenario() {
        return scenario ;
    }

    //------------------------------------------------------------
    /**
     * 全エージェントが止まったらシミュレーションを止めるか？
     */
    public boolean getIsAllAgentSpeedZeroBreak() {
        if (agentHandler == null) {
	    Itk.logWarn("AgentHandler.getIsAllAgentsSpeedZeroBreak",
			"agentHandler is null object.");
            return false;
        } else {
            return agentHandler.getIsAllAgentSpeedZeroBreak();
        }
    }

    /**
     * 全エージェントが止まったらシミュレーションを止めるかをセット。
     */
    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        if (agentHandler == null) {
	    Itk.logWarn("AgentHandler.setIsAllAgentsSpeedZeroBreak",
			"agentHandler is null object.");
        } else {
            agentHandler.setIsAllAgentSpeedZeroBreak(
                    _isAllAgentSpeedZeroBreak);
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント生成の倍率。
     */
    public void setLinerGenerateAgentRatio(double _ratio) {
        linerGenerateAgentRatio = _ratio;
    }

    /**
     * エージェント生成の倍率。
     */
    public double getLinerGenerateAgentRatio() {
        return linerGenerateAgentRatio ;
    }

    //------------------------------------------------------------
    /**
     * Ruby 実行系。
     */
    public ItkRuby getRubyEngine() {
        return rubyEngine ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーションのサイクル間のスリープの取得。
     */
    public int getSimulationDeferFactor() {
        return simulationDeferFactor ;
    }

    /**
     * シミュレーションのサイクル間のスリープの設定。
     */
    public void setSimulationDeferFactor(int factor) {
        simulationDeferFactor = factor;
    }

    //------------------------------------------------------------
    /**
     * 画面を持つかどうかの判定
     */
    public boolean hasDisplay() {
        return launcher.hasDisplay() ;
    }

    //------------------------------------------------------------
    /**
     * 画面制御用 interface
     */
    public SimulationController getController() {
        return (SimulationController)launcher ;
    }

    //------------------------------------------------------------
    /**
     * 設定ファイル情報
     */
    public SetupFileInfo getSetupFileInfo() {
        return launcher.getSetupFileInfo() ;
    }

    /**
     * fallback parameter
     */
    public Term getFallbackParameters() {
        return getSetupFileInfo().fallbackParameters ;
    }

    //------------------------------------------------------------
    // シミュレーションの準備。
    //------------------------------------------------------------
    /**
     * 初期化
     */
    private void init(NetworkMap _networkMap,
                      BasicSimulationLauncher _launcher,
                      Random _random) {
        launcher = _launcher ;

        networkMap = _networkMap ;
        networkMap.checkConsistency() ;

        pollutionFileName = getSetupFileInfo().getPollutionFile();

        random = _random;
    }

    //------------------------------------------------------------
    /**
     * シミュレーションの準備。（メイン）
     */
    public void begin() {
        buildMap() ;
        buildPollution() ;
        buildScenario() ;
        buildAgentHandler() ;
        buildRoutes ();
        buildRubyEngine() ;

        agentHandler.prepareForSimulation();
    }

    //------------------------------------------------------------
    /**
     * マップの準備。
     */
    void buildMap () {
        /* Nodes */
        for (MapNode node : getNodes()) {
            // do nothinkg for node
        }
        /* Links */
        for (MapLink link : getLinks()) {
            link.prepareForSimulation(timeScale) ;
        }
        // リンク上にかかるMapAreaのリストをリンクにセットする
        for (MapArea area : networkMap.getAreas()) {
	    for (MapLink link : getLinks()) {
                if (area.intersectsLine(link.getLine2D())) {
                    link.addIntersectedMapArea(area);
                }
            }
        }

    }

    //------------------------------------------------------------
    /**
     * Pollution の準備。
     */
    void buildPollution() {
        try {
            double interval = (properties == null ?
                               0.0 :
                               properties.getDouble("interpolation_interval", 
                                                    0.0)) ;
            pollutionHandler =
                new PollutionHandler(pollutionFileName,
                                     networkMap.getAreas(),
                                     timeScale, interval);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //------------------------------------------------------------
    /**
     * シナリオ読み込み。
     */
    private void buildScenario() {
        String filename = getSetupFileInfo().getScenarioFile() ;
        if (filename == null || filename.isEmpty()) {
            setupDefaultScenario();
            return;
        }

        if(filename.endsWith(".json")) {
            scenario.scanJsonFile(filename) ;
        } else if (filename.endsWith(".csv")) {
            scenario.scanCsvFile(filename) ;
        } else {
            Itk.logError("Unknown scenario file suffix:", filename) ;
            System.exit(1) ;
        }
        scenario.describe() ;
    }

    //------------------------------------------------------------
    /**
     * デフォルトシナリオをセット。
     * initiateEvent だけ入れる。
     */
    private void setupDefaultScenario() {
        scenario.finalizeSetup() ;
    }

    //------------------------------------------------------------
    /**
     * エージェントハンドラの準備。
     */
    void buildAgentHandler() {
        agentHandler = new AgentHandler(this) ;
    }

    //------------------------------------------------------------
    /**
     * ルート探索の準備
     */
    private void buildRoutes() {
        /* evacuation based on goal tags */
        ArrayList<String> all_goal_tags = agentHandler.getAllGoalTags();

        ArrayList<String> no_goal_list = new ArrayList<String>();
        HashMap<String, CalcGoalPath> workers = new HashMap<String, CalcGoalPath>();
        HashMap<String, Thread> threads = new HashMap<String, Thread>();

        // 経路探索をゴールごとに別スレッドで実行
        for (String goal_tag : all_goal_tags) {
            CalcGoalPath worker = new CalcGoalPath(goal_tag);
            Thread thread = new Thread(worker);
            workers.put(goal_tag, worker);
            threads.put(goal_tag, thread);
            thread.start();
        }
        // スレッド終了を待ってno_goal_list更新
        try {
            for (String goal_tag : all_goal_tags) {
                threads.get(goal_tag).join();
                CalcGoalPath worker = workers.get(goal_tag);
                if (!worker.goalCalculated) {
                    no_goal_list.add(worker.goalTag);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // チェック
        checkMapDefectiveness(all_goal_tags, no_goal_list) ;

        // 不要なメモリを速やかに解放する
        // (メモリ消費量が多いほど実行速度が遅くなる傾向があるため)
        workers = null;
        threads = null;
        System.gc();

    }

    //------------------------------------------------------------
    /**
     * 地図とゴールの完全性チェック。
     * 初期ゴールまで辿り着かないノード・リンクのチェック。
     * 「初期ゴール」とは、開始時点でゴールと認定されているタグのこと。
     * @return ゴールに到達できないノードがあれば true。
     */
    private boolean checkMapDefectiveness(List<String> goalTagList,
                                          List<String> phantomGoalTagList) {
        boolean isDefective = false ;
        // 目的地のないゴールのリスト。
        if (phantomGoalTagList.size() > 0) {
            Itk.logInfo("Found goal tags without places:", 
                        phantomGoalTagList) ;
            isDefective |= true ;
        }

        // ゴールにたどり着かないノードとリンクの数え上げ
        ArrayList<String> unreachableGoalList = new ArrayList<String>() ;
        ArrayList<Integer> unreachableNodeCount = new ArrayList<Integer>() ;
        ArrayList<Integer> unreachableLinkCount = new ArrayList<Integer>() ;
        for (String goal : goalTagList) {
            int nodeCount = 0 ;
            int linkCount = 0 ;
            for (MapNode node : networkMap.getNodes()) {
                if (node.getHint(goal) == null) {
                    nodeCount += 1 ;
                    for (MapLink link : node.getUsableLinkTable()) {
                        //  link.addTag("INVALID_ROUTE");
                        linkCount += 1 ;
                    }
                }
            }
            if(nodeCount + linkCount > 0) {
                unreachableGoalList.add(goal) ;
                unreachableNodeCount.add(nodeCount) ;
                unreachableLinkCount.add(linkCount) ;
                isDefective |= true ;
            }
        }

        // 数え上げ結果報告
        if(unreachableGoalList.size() > 0) {
            for (int i = 0; i < unreachableGoalList.size(); i++) {
                Itk.logInfo("Unreachable",
                            "no route to",
                            unreachableGoalList.get(i),
                            "from",
                            unreachableNodeCount.get(i), "nodes and",
                            unreachableLinkCount.get(i), "links.") ;
            }
        } else {
            Itk.logInfo("Map Check",
                        "all goals can be reachable from whole map.");
        }

        return isDefective ;
    }

    //------------------------------------------------------------
    /**
     * 経路探索再計算。
     */
    public void recalculatePaths() {
        synchronized (stop_simulation) {
            stop_simulation = true;
            buildRoutes();
            stop_simulation = false;
        }
    }

    //------------------------------------------------------------
    /**
     * Rubyエンジン準備。
     * properties file の "use_ruby" が true の場合に有効化。
     * 初期化は以下の順に行われる。
     * <OL>
     *   <LI> CrowdWalk を起動したところの directory からの相対で、
     *        ruby のライブラリのプログラムのあるところ (現状で "src/main/ruby")
     *        を LOAD_PATH に追加。
     *   </LI>
     *   <LI> properties file で指定された load path を追加。
     *   </LI>
     *   <LI> init file をロード。デフォルトは、"initForCrowdWalk.rb"。
     *        "src/main/ruby" に用意してある。
     *   </LI>
     *   <LI> current dir を、properties file があるところへ移す。
     *   </LI>
     *   <LI> properties file で指定した init script の実行。
     *   </LI>
     *   <LI> Ruby の CrowdWalkWrapper のクラスのインスタンスを作る。
     *        "ruby_simulation_wrapper_class" でクラスを指定できる。
     *   </LI>
     * </OL>
     * RubyAgent については、generation file にて指定する。
     */
    private void buildRubyEngine() {
        try {
            if(properties != null && properties.getBoolean("use_ruby", false)) {
                Itk.logInfo("Ruby Engine", "loading...") ;
                // engine
                rubyEngine = new ItkRuby() ;
                // default load path
                String currentDir = (String)rubyEngine.getCurrentDirectory() ;
                String libDir = currentDir + "/" + rubyLibDir ;
                rubyEngine.pushLoadPath(libDir) ;
                // additional load path
                rubyLoadPath = properties.getPathList("ruby_load_path", null) ;
                if(rubyLoadPath != null) {
                    for(String path : rubyLoadPath) {
                        rubyEngine.pushLoadPath(path) ;
                    }
                }
                Itk.logInfo("Ruby Engine",
                            "LOAD_PATH=", rubyEngine.getLoadPaths());
                // load init file
                if(rubyInitFile != null) {
                    Itk.logInfo("Ruby Engine",
                                "load init_file:", rubyInitFile) ;
                    rubyEngine.eval(String.format("require '%s'",rubyInitFile)) ;
                }
                // current dir を properties の directory へ。
                rubyEngine.setCurrentDirectory(properties.getPropertiesDir()) ;
                Itk.logInfo("Ruby Engine",
                            "current dir=", rubyEngine.eval("Dir::pwd")) ;
                // init script
                String initScript = properties.getString("ruby_init_script", null);
                if(initScript != null) {
                    Itk.logInfo("Ruby Engine", 
                                "eval init script=", initScript) ;
                    rubyEngine.eval(initScript) ;
                }
                // wrapperの作成
                rubyWrapperClass =
                    properties.getString("ruby_simulation_wrapper_class", null);
                if(rubyWrapperClass != null) {
                    rubyWrapper =
                        rubyEngine.newInstanceOfClass(rubyWrapperClass, this) ;
                }
                //
                Itk.logInfo("Ruby Engine", "loading... Done.") ;
            }
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("Exception in buildRubyEngine") ;
        }
    }

    //------------------------------------------------------------
    /**
     * 画面の準備。
     */
    public void buildDisplay() {
        panel3d = getController().setupFrame(this);
    }

    /**
     * 画面の準備。
     */
    public void buildDisplay(SimulationPanel3D _panel3d) {
        if (hasDisplay()) {
            if (_panel3d != null) {
                panel3d = getController().setupFrame(this, _panel3d);
            } else {
                panel3d = getController().setupFrame(this, _panel3d);
            }
        }
    }

    /**
     * 3D画面の準備。
     */
    public SimulationPanel3D getPanel3D() {
        return panel3d;
    }

    //------------------------------------------------------------
    /**
     * 各値のリセット。
     */
    void resetValues() {
        // System.gc();

        tick_count = 0.0;
        logging_count = 0;

        for (MapLink link : getLinks()) {
            link.clear();
        }
    }

    //------------------------------------------------------------
    // シミュレーション関連
    //------------------------------------------------------------
    /**
     * シミュレーション初期化。
     */
    public void setup() {
        MapLink.setupCommonParameters(getFallbackParameters()) ;
        MapNode.setupCommonParameters(getFallbackParameters()) ;
        ObstructerBase.setupCommonParameters(getFallbackParameters()) ;
        resetValues();
    }

    //------------------------------------------------------------
    /**
     * シミュレーション開始。
     */
    public void start() {
        if (hasDisplay()) {
            getController().start();
            if (panel3d != null && isRunning()) {
                panel3d.setMenuActionStartEnabled(false);
            }
        }
    }

    //------------------------------------------------------------
    /**
     * シミュレーション中断。
     */
    public void pause() {
        if (hasDisplay()) {
            getController().pause();
            if (panel3d != null && ! isRunning()) {
                panel3d.setMenuActionStartEnabled(true);
            }
        }
    }

    //------------------------------------------------------------
    /**
     * シミュレーション1サイクル実行。
     */
    public void step() {
        if (hasDisplay()) {
            getController().step();
        }
    }

    //------------------------------------------------------------
    /**
     * シミュレーション実行中かどうかの判定。
     */
    public boolean isRunning() {
        // tkokada
        if (hasDisplay()) {
            return getController().isRunning();
        } else {
            return false;
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント登録
     */
    public void registerAgent(AgentBase agent) {
        if (panel3d != null) {
            panel3d.registerAgentOnline(agent);
        }
        /* [2015.05.29 I.Noda]
         * 以下のコード、commit b5c5c85e で一旦消したものの、
         * 渋滞するはずのコードが渋滞しなくなり、おかしい。
         * なので、復活。しかしなぜ必要なのかわからない。
         * agent には、map は、NetworkMap として設定してある。
         * それ以外に必要という事かもしれない。
         * また、ここでないといけないらしい。
         * AgentFactory の tryUpdateAndGenerate() で入れてみたが、
         * おかしくなる。
         */
        agent.setNetworkMap(getMap()) ;
    }

    //------------------------------------------------------------
    /**
     * 汎用 updateEveryTick() ;
     */
    public boolean updateEveryTick() {
        synchronized (stop_simulation) {
            deferSimulation() ;

            double relTime = getSecond();

            // ruby wrapper の preUpdate()
            if(useRubyWrapper())
                rubyEngine.callMethod(rubyWrapper, "preUpdate", relTime) ;

            // pollution 計算。
            if (usePollution())
                pollutionHandler.updateAll(relTime, networkMap,
                                           getWalkingAgentCollection());

            scenario.update(relTime, networkMap) ;
            // 実行本体
            agentHandler.update(relTime);

            // 描画
            updateEveryTickDisplay() ;

            // ruby wrapper の postUpdate()
            if(useRubyWrapper())
                rubyEngine.callMethod(rubyWrapper, "postUpdate", relTime) ;

            // カウンタを進める。
            tick_count += 1.0;
        }
        return isFinished() ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション遅延
     */
    private void deferSimulation() {
        if (simulationDeferFactor > 0) {
            try {
                Thread.sleep(simulationDeferFactor);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 終了判定
     */
    private boolean isFinished() {
        return (scenario.isFinished() || agentHandler.isFinished()) ;
    }

    //------------------------------------------------------------
    /**
     * サイクル毎の画面描画
     */
    private void updateEveryTickDisplay() {
        if (panel3d != null) {
            panel3d.updateClock(getSecond());
            boolean captureScreenShot = (screenshotInterval != 0);
            if (captureScreenShot) {
                panel3d.setScreenShotFileName(String.format("capture%06d", (int)getTickCount()));
            }
            while (! panel3d.notifyViewChange("simulation progressed")) {
                synchronized (this) {
                    try {
                        wait(10);
                    } catch (InterruptedException e) {}
                }
            }
            if (captureScreenShot) {
                // スクリーンショットを撮り終えるまで待つ
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {}
                }
            }
        }
    }

    //------------------------------------------------------------
    // ログ関連
    //------------------------------------------------------------
    /**
     * 結果を出力する。
     * {@code logs/<time>.log} というログと、
     * {@code macro.log} というログ。
     */
    protected void output_results() {
        try {
	    /* [2015.02.10 I.Noda] use timestamp instead of scenario_serial. */
	    String timestamp = Itk.getCurrentTimeStr() ;
            PrintStream ps = new PrintStream("logs/" + timestamp + ".log");
            agentHandler.dumpAgentResult(ps);
            ps.close();

            File macro = new File("logs/macro.log");
            PrintWriter pw = new PrintWriter(new FileWriter(macro, true));
            pw.print(timestamp + ",");
            pw.println(agentHandler.getStatisticsDescription());
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //------------------------------------------------------------
    /** Save the goal log file to result directory.
     * @param resultDirectory: path to the result directory.
     */
    public void saveGoalLog(String resultDirectory, Boolean finished) {
        // Goal log を記憶
        /* [2015.05.27 I.Noda]
         * ここで、AllAgent を使うべきか、WalkingAgent だけでよいか、
         * 不明。
         */
        for (AgentBase agent: getAllAgentCollection()) {
            if (agent.isEvacuated() && !goalTimes.containsKey(agent.ID)) {
                goalTimes.put(agent.ID, new Double(getSecond()));
            }
        }

        if (! finished) {
            // evacuatedAgents log を記憶
            if (logging_count == 0) {
                exitNodeList = new MapNodeTable(agentHandler.getExitNodesMap().keySet());
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
        } else {  // finalize process
            for (AgentBase agent : getWalkingAgentCollection()) {
                if (!agent.isEvacuated()) {
                    goalTimes.put(agent.ID,
                            new Double((getTickCount() + 1) * timeScale));
                }
            }
            List<String> agentIdList = new ArrayList<String>(goalTimes.keySet()) ;
            // 避難完了時刻順にソート
            Collections
                .sort(agentIdList,
                      new Comparator<String>() {
                          public int compare(String id0, String id1) {
                              Double time0 = goalTimes.get(id0) ;
                              Double time1 = goalTimes.get(id1) ;
                              return time0.compareTo(time1) ;
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
                for (String agentId : agentIdList) {
                    writer.write(agentId + "," + goalTimes.get(agentId) + "\n");
                }
                writer.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // EXITノード毎の避難完了者数ログを出力
            File evacuatedAgentsLog = new File(resultDirectory + "/evacuatedAgents.csv");
            try {
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(evacuatedAgentsLog, false), "utf-8")), true);
                int index = 0;
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

    //------------------------------------------------------------
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
            for (AgentBase agent : getAllAgentCollection()) {
                // agent log format:
                // agent,ID,evacuated,speed,density,position
                writer.write("agent," + agent.ID + "," + agent.isEvacuated() +
			     "," + agent.getSpeed() +
			     "," + "0" +
			     "," + agent.getLastPositionOnLink() + "\n");
		totalAgentDensity += 0 ;
                totalAgentSpeed += agent.getSpeed();
            }
	    for (MapLink link : getLinks()) {
                // link log format:
                // link,ID,forward_agents,backward_agents,density
                double linkDensity = (link.getLane(Direction.Forward).size() +
                        link.getLane(Direction.Backward).size()) /
                    (link.length * link.width);
                writer.write("link," + link.ID + "," +
                        link.getLane(Direction.Forward).size() + "," +
                        link.getLane(Direction.Backward).size() + "," +
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
            if (getAllAgentCollection().size() == 0) {
                average_agent_density = 0.0;
                average_agent_speed = 0.0;
            } else {
                average_agent_density = totalAgentDensity / getAllAgentCollection().size();
                average_agent_speed = totalAgentSpeed / getAllAgentCollection().size();
            }
	    if (getLinks().size() == 0)
                average_link_density = 0.0;
            else
		average_link_density = totalLinkDensity / getLinks().size();
            writer.write("count," + count + "," + average_agent_density + "," +
                    average_agent_speed + "," + average_link_density + "\n");
            writer.close();

            // EXITノード毎の避難完了者数ログファイル
            File evacuatedAgentsLog = new File(resultDirectory + "/evacuatedAgents.csv");
            if (logging_count == 0) {
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(evacuatedAgentsLog, false), "utf-8")), true);
                exitNodeList = new MapNodeTable(agentHandler.getExitNodesMap().keySet());
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

    //============================================================
    /**
     * 経路探索の実行系クラス。
     * ダイクストラで経路情報を計算。
     */
    class CalcGoalPath implements Runnable {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 計算終了したかどうか。
         */
        public boolean goalCalculated = false;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 目標とするゴール。
         */
        public String goalTag;

        //----------------------------------------
        /**
         * コンストラクタ。
         */
        public CalcGoalPath(String _goal_tag) {
            goalTag = _goal_tag;
        }

        //----------------------------------------
        /**
         * 探索実行。
         */
        public void run() {
            goalCalculated = calc_goal_path(goalTag);
        }

        //----------------------------------------
        /**
         * 探索の本体。
         */
        private boolean calc_goal_path(String goal_tag) {
            return networkMap.calcGoalPath(goal_tag) ;
        }
    }

}
