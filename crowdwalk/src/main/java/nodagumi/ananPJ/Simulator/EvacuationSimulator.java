// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.BasicSimulationLauncher;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.navigation.NavigationHint;

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
     * シミュレーション内の時計
     */
    public SimClock clock = new SimClock() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 最新時刻（clock によるタイムスタンプ）
     */
    public SimTime currentTime = null ;

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
    private Object simulationPauseLocker = new Object();

    /* [2021.11.19 S.Takami]
     * Boolean(value-based class)では
     * Synchronizeは正常に動作しないためロック用Objectを追加．
     * stop_simulationにはシミュレーション停止中のステートを入れていて，
     * 参照はされていないが，使用する可能性が捨てきれないため残した．
     */

    /**
     * start/pause のステート
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
    private Scenario scenario ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シミュレーションのサイクル間のスリープ。
     */
    private int simulationDeferFactor = 0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シミュレーション設定ファイルを扱う。
     */
    private CrowdWalkPropertiesHandler properties = null;

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

    /**
     * jar における ruby lib のパス
     */
    static public String RubyLibDirInJar = "uri:classloader:/ruby/CrowdWalk" ;

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
     * preCycle(currentTime), postCycle(currentTime) が呼び出される。
     */
    private Object rubyWrapper = null ;

    /**
     * irb 用 thread。
     */
    private Thread irbThread = null ;

    /**
     * irb 用 Semaphore。
     */
    public Semaphore irbSemaphore = null ;
    
    /**
     * irb 用 Interrupt Reason。
     */
    public String irbKillReason = null ;
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Event などにより遅延で random の setSeed する場合のフラグ
     */
    private SeedRandEvent.Timing delayedSetSeedTiming = null ;

    /**
     * Event などにより遅延で random の setSeed する場合のseedの値。
     */
    private long delayedSetSeedValue =  0 ;
    
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
    /**
     * 遅延 set seed の予約。
     * @param timing : set seed するタイミング。enum SeedRandEvent.Timing。
     * @param seed : set する seed 値。
     */
    public void reserveDelayedSetSeed(SeedRandEvent.Timing timing, long seed) {
        delayedSetSeedTiming = timing ;
        delayedSetSeedValue = seed ;
        Itk.logInfo("reserveDelayedSetSeed",
                    "timing=" + timing, "seed=" + seed);
    }
    
    /**
     * 遅延 set seed のチェックと実行
     * @param timing : try するタイミング。
     *                 これが deleydSetSeedTiming と一致すれば、実行される。
     * @return set seed が実行されたら、true。
     */
    public boolean tryDelayedSetSeed(SeedRandEvent.Timing timing) {
        if(delayedSetSeedTiming == timing) {
            random.setSeed(delayedSetSeedValue) ;
            delayedSetSeedTiming = SeedRandEvent.Timing.none ;
            delayedSetSeedValue = 0 ;
            Itk.logInfo("succeed to tryDelayedSetSeed",
                        "timing=" + timing, "seed=" + delayedSetSeedValue) ;
            return true ;
        } else {
            return false ;
        }
    }
    
    //------------------------------------------------------------
    // メンバ変数アクセス。
    //------------------------------------------------------------
    /**
     * マップ取得。
     * @return ネットワークマップ。
     */
    public NetworkMap getMap() {
        return networkMap;
    }

    //------------------------------------------------------------
    /**
     * リンク取得。
     * @return リンクのテーブル。
     */
    public MapLinkTable getLinks() {
	return networkMap.getLinks();
    }

    //------------------------------------------------------------
    /**
     * ノード取得。
     * @return ノードのテーブル。
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
     * 最後のサイクルに避難完了したエージェントリストを返す.
     *
     * ※更新タイミングに注意
     */
    public ArrayList<AgentBase> getEvacuatedAgentsInStep() {
        return agentHandler.getEvacuatedAgentsInStep() ;
    }

    //------------------------------------------------------------
    /**
     * エージェントハンドラ取得。
     * @return エージェントハンドラ。
     */
    public AgentHandler getAgentHandler() {
        return agentHandler;
    }

    //------------------------------------------------------------
    /**
     * Pollution ハンドラ取得。
     * @return 汚染エリアのリスト。
     */
    public ArrayList<MapArea> getPollutions() {
        return pollutionHandler.getPollutions();
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

    /**
     * Properties のデータを Term で取得。
     */
    public Term getPropertiesTerm() {
        return getProperties().getPropertiesTerm() ;
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
     * ruby Wrapper を使っているかどうか
     */
    public boolean useRubyWrapper() {
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

    /**
     * Ruby Wrapper 実行の呼び出し。
     */
    public Object callRubyWrapper(String methodName, Object... args) {
        return rubyEngine.callMethod(rubyWrapper, methodName, args) ;
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
     * SimulationLauncher を取得する。
     */
    public BasicSimulationLauncher getSimulationLauncher() {
        return launcher;
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

    /**
     * fallback parameter を filter する。
     */
    public Term filterFallbackTerm(String tag) {
        return SetupFileInfo.filterFallbackTerm(getFallbackParameters(), tag) ;
    }

    /**
     * fallback parameter から fetch する。
     */
    public Term fetchFallbackTerm(Term fallbacks, String tag,
                                  Term fallbackValue) {
        return SetupFileInfo.fetchFallbackTerm(fallbacks, tag, fallbackValue) ;
    }

    /**
     * fallback parameter から fetch する。
     */
    public String fetchFallbackString(Term fallbacks, String tag,
                                      String fallbackValue) {
        return SetupFileInfo.fetchFallbackString(fallbacks, tag, fallbackValue) ;
    }

    /**
     * fallback parameter から fetch する。
     */
    public double fetchFallbackDouble(Term fallbacks, String tag,
                                      Double fallbackValue) {
        return SetupFileInfo.fetchFallbackDouble(fallbacks, tag, fallbackValue) ;
    }

    /**
     * fallback parameter から fetch する。
     */
    public int fetchFallbackInt(Term fallbacks, String tag,
                                int fallbackValue) {
        return SetupFileInfo.fetchFallbackInt(fallbacks, tag, fallbackValue) ;
    }

    /**
     * fallback parameter から fetch する。
     */
    public boolean fetchFallbackBoolean(Term fallbacks, String tag,
                                        boolean fallbackValue) {
        return SetupFileInfo.fetchFallbackBoolean(fallbacks, tag, fallbackValue) ;
    }

    /**
     * fallback parameter から filter して fetch する。
     */
    public Term filterFetchFallbackTerm(String filterTag, String fetchTag,
                                        Term fallbackValue) {
        return fetchFallbackTerm(filterFallbackTerm(filterTag),
                                 fetchTag,
                                 fallbackValue) ;
    }

    /**
     * fallback parameter から filter して fetch する。
     */
    public String filterFetchFallbackString(String filterTag, String fetchTag,
                                            String fallbackValue) {
        return fetchFallbackString(filterFallbackTerm(filterTag),
                                   fetchTag,
                                   fallbackValue) ;
    }

    /**
     * fallback parameter から filter して fetch する。
     */
    public Double filterFetchFallbackDouble(String filterTag, String fetchTag,
                                            Double fallbackValue) {
        return fetchFallbackDouble(filterFallbackTerm(filterTag),
                                   fetchTag,
                                   fallbackValue) ;
    }

    /**
     * fallback parameter から filter して fetch する。
     */
    public int filterFetchFallbackInt(String filterTag, String fetchTag,
                                      int fallbackValue) {
        return fetchFallbackInt(filterFallbackTerm(filterTag),
                                fetchTag,
                                fallbackValue) ;
    }

    /**
     * fallback parameter から filter して fetch する。
     */
    public boolean filterFetchFallbackBoolean(String filterTag, String fetchTag,
                                              boolean fallbackValue) {
        return fetchFallbackBoolean(filterFallbackTerm(filterTag),
                                    fetchTag,
                                    fallbackValue) ;
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

        scenario = new Scenario(this);
    }

    //------------------------------------------------------------
    /**
     * ロガーのセットアップ
     */
    private void initLoggers() {
        getAgentHandler().initSimulationLoggers() ;
    }

    //------------------------------------------------------------
    /**
     * ロガーの finalize
     */
    private void finalizeLoggers() {
        getAgentHandler().finalizeSimulationLoggers() ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーションの準備。（メイン）
     */
    public void begin() {
        buildMap() ;
        buildScenario() ;
        buildPollution() ;
        buildAgentHandler() ;
        buildRoutes() ;
        buildRubyEngine() ;

        //prepare for simulation
        agentHandler.prepareForSimulation();

        //logger
        if(useRubyWrapper())
            callRubyWrapper("setupSimulationLoggers") ;
        initLoggers() ;

        if(useRubyWrapper())
            callRubyWrapper("prepareForSimulation") ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーションの終了処理。（メイン）
     */
    public void finish() {
        finalizeLoggers() ;

        if(irbThread != null) irbKillThread("finalizeSimulation") ;

        if(useRubyWrapper()) callRubyWrapper("finalizeSimulation") ;
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
            link.prepareForSimulation() ;
        }
        // リンク上にかかるMapAreaのリストをリンクにセットする
        for (MapArea area : networkMap.getAreas()) {
	    for (MapLink link : getLinks()) {
                if (area.intersectsLine(link.getLine2D())) {
                    link.addIntersectedMapArea(area);
                }
            }
        }
        /* 主観的距離計さんルールの設定 */
        networkMap.setMentalMapRules(properties.getMentalMapRules()) ;
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
                                     currentTime, interval);
        } catch (Exception e) {
            Itk.quitWithStackTrace(e);
        }
    }

    //------------------------------------------------------------
    /**
     * シナリオ読み込み。
     * この中で、clock の originTime がセットされる。
     */
    private void buildScenario() {
        String filename = getSetupFileInfo().getScenarioFile() ;

        // ファイル読み込み。
        scenario.scanFile(filename, clock) ;

        // クロックがセットされたので、最新時刻のタイムスタンプを撮る。
        currentTime = clock.newSimTime() ;

        scenario.describe() ;
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
            Itk.quitWithStackTrace(e);
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
     * ルート探索情報の再構築。
     */
    public void rebuildRoutes() {
        networkMap.clearNavigationHints() ;
        buildRoutes() ;
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
                if (node.getHint(NavigationHint.DefaultMentalMode, goal, false)
                    == null) {
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
        synchronized (simulationPauseLocker) {
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
                // default load path on jar
                rubyEngine.pushLoadPath(RubyLibDirInJar) ;
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
                rubyEngine
                    .setCurrentDirectory(properties.getPropertiesDirAbs()) ;
                Itk.logInfo("Ruby Engine",
                            "current dir=", rubyEngine.eval("Dir::pwd")) ;
                // init script
                Term initScriptTerm =
                    properties.getTerm("ruby_init_script", null) ;
                if(initScriptTerm != null) {
                    String initScript = "" ;
                    if(initScriptTerm.isArray()) {
                        for(Object item : initScriptTerm.getArray()) {
                            initScript += "\n" ;
                            initScript += ((Term)item).getString() ;
                        }
                    } else {
                        initScript = initScriptTerm.getString() ;
                    }
                    
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
                // AgentFactoryByRuby 用の処理
                agentHandler.setupAgentFactoryByRuby(rubyEngine) ;
                
                // irb 用処理
                if(properties.getBoolean("use_irb", false)) {
                    Itk.logInfo("Setup Irb Thread.") ;
                    irbSemaphore = new Semaphore(1) ;
                    irbThread = new Thread(() -> {
                            this.irbRepLoop() ;
                        }) ;
                    irbAcquireSemaphore() ;
                    irbThread.start() ;
                }
                
                // final.
                Itk.logInfo("Ruby Engine", "loading... Done.") ;
            }
        } catch (Exception ex) {
            Itk.logError("Exception in buildRubyEngine") ;
            Itk.quitWithStackTrace(ex) ;
        }
    }

    //------------------------------------------------------------
    // Irb setup
    /**
     * Irb Rep Loop.
     */
    private void irbRepLoop() {
        try {
            // wait until thread is ready.
            while(irbSemaphore == null) {
                Thread.yield() ;
            }
            // initialize
            irbAcquireSemaphore() ;
            rubyEngine.eval("require 'irb'") ;
            rubyEngine.setVariable("$crowdwalk", this) ;
            //        rubyEngine.eval("pp IRB.conf[:PROMPT]") ;
            //        rubyEngine.eval("IRB.conf[:PROMPT][:DEFAULT][:PROMPT_I] = 'CrowdWalk> '") ;
        
            // run
            rubyEngine.eval("IRB.start") ;
            irbReleaseSemaphore() ;
        } catch(Exception ex) {
            // do nothing ;
        }
    }

    /**
     * Irb kill thread.
     */
    private void irbKillThread(String killReason) {
        if(irbThread != null) {
            irbKillReason = killReason ;
            irbThread.interrupt() ;
        }
    }
    

    /**
     * irb semaphor aquire.
     */
    public void irbAcquireSemaphore() throws InterruptedException {
        if(irbSemaphore != null) {
            irbSemaphore.acquire() ;
        }
    }
    
    /**
     * irb semaphor release.
     */
    public void irbReleaseSemaphore() throws InterruptedException {
        if(irbSemaphore != null) {
            irbSemaphore.release() ;
            Thread.yield() ;
            Thread.sleep(1) ;
        }
    }

    /**
     * irb yield thread.
     */
    public void irbYield() throws InterruptedException {
        if(irbThread != null) {
            irbReleaseSemaphore() ;
            irbAcquireSemaphore() ;
        }
    }
    
    /**
     * Irb wait N cycle. (called from Ruby)
     */
    public Object irbWaitCycleN(Object n) {
        try {
            Long nTick = (Long)n ;
            SimTime cycleUntil =
                currentTime.newSimTimeWithAdvanceTick(nTick.intValue()) ;
            while(currentTime.isBefore(cycleUntil)) {
                irbYield() ;
            }
        } catch(InterruptedException ex) {
            return irbKillReason ;
        }
        return null ;
    }

    //------------------------------------------------------------
    /**
     * String の intern. Rubyからの呼び出し用
     */
    final public String intern(String str) {
        return rubyEngine.intern(str) ;
    }
    
    //------------------------------------------------------------
    /**
     * 各値のリセット。
     */
    void resetValues() {
        // System.gc();

        clock.reset() ;
        currentTime = clock.newSimTime() ;

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
     * エージェント登録
     */
    public void registerAgent(AgentBase agent) {
        launcher.registerAgent(agent);

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
        agent.setMap(getMap()) ;
    }

    //------------------------------------------------------------
    /**
     * 汎用 updateEveryTick() ;
     */
    public boolean updateEveryTick() {
        synchronized (simulationPauseLocker) {
            deferSimulation() ;

            // irb とのやり取り。
            try {
                irbYield() ;
            } catch(Exception ex) {} ;
            
            // ruby wrapper の preUpdate()
            if(useRubyWrapper())
                callRubyWrapper("preUpdate", currentTime) ;

            // pollution 計算。
            if (usePollution())
                pollutionHandler.updateAll(currentTime, networkMap,
                                           getWalkingAgentCollection());

            scenario.update(currentTime, networkMap) ;
            // 実行本体
            agentHandler.update(currentTime);

            // ruby wrapper の postUpdate()
            if(useRubyWrapper())
                callRubyWrapper("postUpdate", currentTime) ;

            // 描画のための呼び出し
            launcher.updateEveryTick(currentTime);

            // カウンタを進める。
            clock.advance() ;
            currentTime = clock.newSimTime() ;
        }
        return isFinished() ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション遅延
     */
    /* [2024-06-22 S.Takami]
     * この実装は描画側で行うべき？
     * とりあえず，CUIで止めたい可能性は無くはないかもしれないので保留．
     * そのため，display_intervalを変更した場合にも，同じ時間待つことになる．
     */
    private void deferSimulation() {
        if (simulationDeferFactor > 0) {
            try {
                Thread.sleep(simulationDeferFactor);
            } catch (InterruptedException e) {
                Itk.dumpStackTraceOf(e);
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
     * evacuation count 表示用の文字列を生成して返す
     */
    public String getEvacuatedCountStatus() {
        if (agentHandler.numOfStuckAgents() == 0) {
            return String.format(
                    "Walking: %d  Generated: %d  Evacuated: %d / %d",
                    agentHandler.numOfWalkingAgents(),
                    agentHandler.numOfAllAgents(),
                    agentHandler.numOfEvacuatedAgents(), agentHandler.getMaxAgentCount());
        } else {
            return String.format(
                    "Walking: %d  Generated: %d  Evacuated(Stuck): %d(%d) / %d",
                    agentHandler.numOfWalkingAgents(),
                    agentHandler.numOfAllAgents(),
                    agentHandler.numOfEvacuatedAgents() - agentHandler.numOfStuckAgents(),
                    agentHandler.numOfStuckAgents(),
                    agentHandler.getMaxAgentCount());
        }
    }

    /**
     * ステータスライン表示用の文字列を生成して返す
     */
    public String getStatusLine() {
        return String.format("Time: %s  Elapsed: %5.2fsec  %s",
                currentTime.getAbsoluteTimeString(),
                currentTime.getRelativeTime(),
                getEvacuatedCountStatus());
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
            goalCalculated =
                calcGoalPath(goalTag);
        }

        //----------------------------------------
        /**
         * 探索の本体。
         */
        private boolean calcGoalPath(String goalTag) {
            return networkMap.calcGoalPathAll(goalTag) ;
        }
    }

}
