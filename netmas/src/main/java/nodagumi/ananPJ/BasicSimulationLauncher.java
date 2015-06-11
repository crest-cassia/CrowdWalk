// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.misc.NetmasPropertiesHandler;
import nodagumi.ananPJ.misc.NetmasTimer;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;

import nodagumi.Itk.*;

//======================================================================
/**
 * GUI/CUI 共通の部分を記述する。
 */
public abstract class BasicSimulationLauncher {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デバッグモード
     */
    protected static boolean isDebug = false; // debug mode

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 属性を扱うハンドラ
     */
    protected NetmasPropertiesHandler properties = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * simulator の実体
     */
    protected EvacuationSimulator simulator = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 地図およびエージェント格納体
     */
    protected NetworkMap networkMap;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * スピードモデル
     */
    protected SpeedCalculationModel speedModel = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器。
     */
    protected Random random = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * TimeSeriesLog を取るかどうか
     */
    protected boolean isTimeSeriesLog = false;

    /**
     * TimeSeriesLog へのパス
     */
    protected String timeSeriesLogPath = null;

    /**
     * TimeSeriesLog のインターバル
     */
    protected int timeSeriesLogInterval = -1;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Timer の Log を取るかどうか
     */
    protected boolean isTimerEnabled = false;

    /**
     * Timer そのもの
     */
    protected NetmasTimer timer = null;

    /**
     * Timer ログへのパス
     */
    protected String timerPath = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 実行が終了しているかどうかのフラグ。
     * simulation を走らせて終了条件を迎えた時に true となる。
     */
    protected boolean finished = true;

    /**
     * シミュレーションのサイクルカウント。
     * simulation step 1回終了する毎に、１増える。
     */
    protected int counter = 0 ;

    /**
     * 終了カウント。
     */
    protected static int exitCount = 0;

    /**
     * 終了条件の１つ。
     */
    protected boolean isAllAgentSpeedZeroBreak = false;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 実験設定ファイル。
     */
    protected static String propertiesPath = null;

    /**
     * マップファイル。
     */
    protected static String mapPath = null;

    /**
     * 障害設定ファイル。
     */
    protected static String pollutionPath = null; // path to pollution file

    /**
     * エージェント生成ルールファイル。
     */
    protected static String generationPath = null; // path to generation file

    /**
     * イベントシナリオファイル。
     */
    protected static String scenarioPath = null; // path to scenario file

    /**
     * 規定値ファイル。
     */
    protected static String fallbackPath = null; // path to fallback file

    //------------------------------------------------------------
    /**
     * constructor
     */
    public BasicSimulationLauncher(Random _random) {
        random = _random ;
    }

    //------------------------------------------------------------
    // アクセスメソッド
    //------------------------------------------------------------
    /**
     * シミュレータ実体の取り出し
     */
    public EvacuationSimulator getSimulator() { return simulator; }

    /**
     * 乱数生成器セット。
     */
    public void setRandom(Random _random) {
        random = _random;
    }

    /**
     * 乱数生成器取り出し。
     */
    public Random getRandom() {
        return random;
    }

    /**
     * 終了カウント設定。
     */
    public void setExitCount(int _exitCount) {
        exitCount = _exitCount;
    }

    /**
     * 終了カウント取得。
     */
    public int getExitCount() {
        return exitCount;
    }

    /**
     * マップ取得
     */
    public NetworkMap getMap() { return networkMap; }

    /**
     * スピードモデル設定。
     */
    public void setSpeedModel(SpeedCalculationModel _speedModel) {
        speedModel = _speedModel;
    }

    /**
     * スピードモデル取得。
     */
    public SpeedCalculationModel getSpeedModel() {
        return speedModel;
    }

    /**
     * 終了条件１のセット
     */
    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        isAllAgentSpeedZeroBreak = _isAllAgentSpeedZeroBreak;
    }

    /**
     * 終了条件１
     */
    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    //------------------------------------------------------------
    /**
     * Set isTimeSeriesLog.
     * @param _isTimeSeriesLog the value to set.
     */
    public void setIsTimeSeriesLog(boolean _isTimeSeriesLog) {
        isTimeSeriesLog = _isTimeSeriesLog;
    }

    /**
     * Get isTimeSeriesLog.
     * @return isTimeSeriesLog as boolean.
     */
    public boolean getIsTimeSeriesLog() {
        return isTimeSeriesLog;
    }

    /**
     * Set timeSeriesLogPath.
     * @param _timeSeriesLogPath the value to set.
     */
    public void setTimeSeriesLogPath(String _timeSeriesLogPath) {
        timeSeriesLogPath = _timeSeriesLogPath;
    }

    /**
     * Get timeSeriesLogPath.
     * @return timeSeriesLogPath as String.
     */
    public String getTimeSeriesLogPath() {
        return timeSeriesLogPath;
    }

    /**
     * Set timeSeriesLogInterval.
     * @param _timeSeriesLogInterval the value to set.
     */
    public void setTimeSeriesLogInterval(int _timeSeriesLogInterval) {
        timeSeriesLogInterval = _timeSeriesLogInterval;
    }

    /**
     * Get timeSeriesLogInterval.
     * @return timeSeriesLogInterval as int.
     */
    public int getTimeSeriesLogInterval() {
        return timeSeriesLogInterval;
    }

    /**
     * タイマー有効化。
     */
    public void setIsTimerEnabled(boolean _isTimerEnabled) {
        isTimerEnabled = _isTimerEnabled;
    }

    /**
     * タイマー有効・無効チェック。
     */
    public boolean getIsTimerEnabled() {
        return isTimerEnabled;
    }

    /**
     * タイマーファイルパス設定。
     */
    public void setTimerPath(String _timerPath) {
        timerPath = _timerPath;
    }

    /**
     * タイマーファイルパス取得。
     */
    public String getTimerPath() {
        return timerPath;
    }

    //------------------------------------------------------------
    // Pathへのアクセスメソッド
    //------------------------------------------------------------
    /**
     *
     */
    public void setMapPath(String _mapPath) {
        mapPath = _mapPath;
    }

    /**
     *
     */
    public String getMapPath() {
        return mapPath;
    }

    /**
     *
     */
    public void setPollutionPath(String _pollutionPath) {
        pollutionPath = _pollutionPath;
        if (networkMap != null) {
            networkMap.setPollutionFile(pollutionPath);
        }
    }

    /**
     *
     */
    public String getPollutionPath() {
        return pollutionPath;
    }

    /**
     *
     */
    public void setGenerationPath(String _generationPath) {
        generationPath = _generationPath;
        if (networkMap != null) {
            networkMap.setGenerationFile(generationPath);
        }
    }

    /**
     *
     */
    public String getGenerationPath() {
        return generationPath;
    }

    /**
     *
     */
    public void setScenarioPath(String _scenarioPath) {
        scenarioPath = _scenarioPath;
        if (networkMap != null) {
            networkMap.setScenarioFile(scenarioPath);
        }
    }

    /**
     *
     */
    public String getScenarioPath() {
        return scenarioPath;
    }

    /**
     *
     */
    public void setFallbackPath(String _fallbackPath) {
        fallbackPath = _fallbackPath;
        if (networkMap != null) {
            networkMap.setFallbackFile(fallbackPath) ;
            networkMap.scanFallbackFile(true) ;
        }
    }

    /**
     *
     */
    public String getFallbackPath() {
            return fallbackPath ;
    }

    /**
     * プロパティへの橋渡し。
     */
    public NetmasPropertiesHandler getProperties() {
        return properties;
    }

    //------------------------------------------------------------
    /**
     * 地図の読み込み
     */
    protected NetworkMap readMapWithName(String file_name)
            throws IOException {
        FileInputStream fis = new FileInputStream(file_name);
        Document doc = ItkXmlUtility.singleton.streamToDoc(fis);
        if (doc == null) {
            System.err.println("ERROR Could not read map.");
            return null;
        }
        NodeList toplevel = doc.getChildNodes();
        if (toplevel == null) {
            System.err.println("BasiciSimulationLauncher.readMapWithName " +
                    "invalid inputted DOM object.");
            return null;
        }
        // NetMAS based map
        NetworkMap network_map = new NetworkMap(random);
        if (false == network_map.fromDOM(doc))
            return null;
        Itk.logInfo("Load Map File", file_name) ;
        network_map.setFileName(file_name);
        return network_map;
    }

    //------------------------------------------------------------
    /**
     * 地図の初期設定を、Launcher から渡す。
     */
    protected void setupNetworkMap() {
        networkMap.setPollutionFile(pollutionPath);
        networkMap.setGenerationFile(generationPath);
        networkMap.setScenarioFile(scenarioPath);
        networkMap.setFallbackFile(fallbackPath);
        networkMap.scanFallbackFile(true) ;
    }

    //------------------------------------------------------------
    /**
     * ファイルからプロパティの読み込み。
     */
    public void setPropertiesFromFile(String _propertiesFile) {
        properties = new NetmasPropertiesHandler(_propertiesFile);

        isDebug = properties.getIsDebug();
        // random
        random = new Random(properties.getRandseed()) ;
        // files
        setMapPath(properties.getMapPath());
        setPollutionPath(properties.getPollutionPath());
        setGenerationPath(properties.getGenerationPath());
        setScenarioPath(properties.getScenarioPath());
        setFallbackPath(properties.getFallbackPath()) ;
        // timer & time series
        setIsTimerEnabled(properties.getIsTimerEnabled());
        setTimerPath(properties.getTimerPath());
        setIsTimeSeriesLog(properties.getIsTimeSeriesLog());
        setTimeSeriesLogPath(properties.getTimeSeriesLogPath());
        setTimeSeriesLogInterval(properties.getTimeSeriesLogInterval());
        //models
        setSpeedModel(properties.getSpeedModel());
        setExitCount(properties.getExitCount()) ;
        setIsAllAgentSpeedZeroBreak(properties.getIsAllAgentSpeedZeroBreak());

    }

    //------------------------------------------------------------
    /**
     * シミュレーションの初期化。
     */
    protected void initializeSimulatorEntity(boolean hasDisplay) {
        simulator = new EvacuationSimulator(networkMap, this, random) ;

        simulator.setProperties(properties);
        simulator.setup();

        // model.begin set files (pol, gen, sce) to networkMap
        simulator.begin(hasDisplay) ;
        if(hasDisplay) simulator.buildDisplay();

        simulator.setIsAllAgentSpeedZeroBreak(isAllAgentSpeedZeroBreak);

        // this method just set 0 to model.tick_count
        if (isTimerEnabled) {
            timer = new NetmasTimer(10, timerPath);
            timer.start();
        }
        counter = 0;
        finished = false;
    }

    //------------------------------------------------------------
    /**
     * シミュレーションのステップ（synchronize していない）
     */
    protected void simulateOneStepBare() {
        finished = simulator.updateEveryTick();
        if (isTimeSeriesLog) {
            if (((int) simulator.getSecond()) % timeSeriesLogInterval == 0)
                simulator.saveGoalLog(timeSeriesLogPath, false);
                simulator.saveTimeSeriesLog(timeSeriesLogPath);
        }
        if (isTimerEnabled) {
            timer.tick();
            timer.writeInterval();
            if ((simulator.getSecond() % 60) == 0)
                timer.writeElapsed();
        }
        counter++ ;
        if ((counter % 100) == 0)
            Itk.logDebug("Cycle",
                         "count:", counter,
                         "time:", Itk.getCurrentTimeStr(),
                         "walking:",
                         simulator.getAgentHandler().numOfWalkingAgents()) ;
    }

}
