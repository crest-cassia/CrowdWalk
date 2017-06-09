// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;

//======================================================================
/**
 * GUI/CUI 共通の部分を記述する。
 */
public abstract class BasicSimulationLauncher {
    /*****/
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 属性を扱うハンドラ
     */
    protected CrowdWalkPropertiesHandler properties = null;

    /*****/
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 読み込んだ属性情報のファイル名
     */
    private String propertiesFile = null;

    /*****/
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 設定ファイルの取りまとめ。
     */
    protected SetupFileInfo setupFileInfo = new SetupFileInfo();

    /*****/
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * コマンドラインのfallback
     */
    protected ArrayList<String> commandLineFallbacks = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * simulator の実体
     */
    protected EvacuationSimulator simulator = null;

    /*****/
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 地図データ。
     */
    protected NetworkMap map ;

    /*****/
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器。
     */
    protected Random random = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 実行が終了しているかどうかのフラグ。
     * simulation を走らせて終了条件を迎えた時に true となる。
     */
    protected boolean finished = true;

    /**
     * 実行が中断しているかどうかのフラグ。
     * 画面を持つモードなどで、途中でとめて、mainLoop を抜けたい時に true ;
     */
    protected boolean paused = true ;

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
        if(simulator != null) {
            simulator.setRandom(_random);
        }
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

    /*****/
    /**
     * マップ取得
     */
    public NetworkMap getMap() { return map ; }

    /*****/
    /**
     * 設定取得
     */
    public SetupFileInfo getSetupFileInfo() { return setupFileInfo; }

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
    // Pathへのアクセスメソッド
    //------------------------------------------------------------
    /*****/
    /**
     *
     */
    public void setNetworkMapFile(String _file) {
        setupFileInfo.setNetworkMapFile(_file) ;
    }

    /*****/
    /**
     *
     */
    public String getNetworkMapFile() {
        return setupFileInfo.getNetworkMapFile() ;
    }

    /*****/
    /**
     *
     */
    public void setPollutionFile(String _pollutionFile) {
        setupFileInfo.setPollutionFile(_pollutionFile);
    }

    /*****/
    /**
     *
     */
    public String getPollutionFile() {
        return setupFileInfo.getPollutionFile();
    }

    /*****/
    /**
     *
     */
    public void setGenerationFile(String _generationFile) {
        setupFileInfo.setGenerationFile(_generationFile);
    }

    /*****/
    /**
     *
     */
    public String getGenerationFile() {
        return setupFileInfo.getGenerationFile();
    }

    /*****/
    /**
     *
     */
    public void setScenarioFile(String _scenarioFile) {
        setupFileInfo.setScenarioFile(_scenarioFile);
    }

    /*****/
    /**
     *
     */
    public String getScenarioFile() {
        return setupFileInfo.getScenarioFile() ;
    }

    /*****/
    /**
     *
     */
    public void setFallbackFile(String _fallbackFile,
                                ArrayList<String> commandLineFallbacks) {
        setupFileInfo.setFallbackFile(_fallbackFile) ;
        setupFileInfo.scanFallbackFile(commandLineFallbacks, true) ;
    }

    /*****/
    /**
     *
     */
    public String getFallbackFile() {
        return setupFileInfo.getFallbackFile() ;
    }

    /*****/
    /**
     * プロパティへの橋渡し。
     */
    public CrowdWalkPropertiesHandler getProperties() {
        return properties;
    }

    /*****/
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
        NetworkMap tmpMap = new NetworkMap() ;
        if (false == tmpMap.fromDOM(doc))
            return null;
        Itk.logInfo("Load Map File", file_name) ;
        setupFileInfo.setNetworkMapFile(file_name);
        return tmpMap;
    }

    /*****//*微妙に違う*/
    //------------------------------------------------------------
    /**
     * ファイルからプロパティの読み込み。
     */
    public void setPropertiesFromFile(String _propertiesFile,
                                      ArrayList<String> _commandLineFallbacks) {
        properties = new CrowdWalkPropertiesHandler(_propertiesFile);
        propertiesFile = _propertiesFile;
        commandLineFallbacks = _commandLineFallbacks;

        // random
        random = new Random(properties.getRandseed()) ;
        // files
        setNetworkMapFile(properties.getNetworkMapFile());
        setPollutionFile(properties.getPollutionFile());
        setGenerationFile(properties.getGenerationFile());
        setScenarioFile(properties.getScenarioFile());
        setFallbackFile(properties.getFallbackFile(),
                        commandLineFallbacks) ;
        // ending condition
        setExitCount(properties.getExitCount()) ;
        setIsAllAgentSpeedZeroBreak(properties.getIsAllAgentSpeedZeroBreak());

    }

    //------------------------------------------------------------
    /**
     * ディスプレーを持つかどうか。
     */
    public abstract boolean hasDisplay();

    //------------------------------------------------------------
    /**
     * シミュレーションの初期化。
     */
    protected void initializeSimulatorEntity() {
        simulator = new EvacuationSimulator(map, this, random) ;

        simulator.setProperties(properties);
        simulator.setup();

        // model.begin set files (pol, gen, sce) to map
        simulator.begin() ;

        simulator.setIsAllAgentSpeedZeroBreak(isAllAgentSpeedZeroBreak);

        // rand seed setup
        random.setSeed(properties.getRandseed());

        counter = 0;
        finished = false;
    }

    //------------------------------------------------------------
    /**
     * シミュレーションのステップ（synchronize していない）
     */
    protected void simulateOneStepBare() {
        finished = simulator.updateEveryTick();
        int relativeTime = (int)simulator.clock.getRelativeTime() ;
        counter++ ;
        if ((counter % 100) == 0)
            Itk.logDebug("Cycle",
                         "count:", counter,
                         "time:", Itk.getCurrentTimeStr(),
                         "walking:",
                         simulator.getAgentHandler().numOfWalkingAgents()) ;
    }

    /**
     * EvacuationSimulator のステップ処理が終了してカウントアップされる直前に呼び出される。
     */
    public abstract void updateEveryTick(SimTime currentTime);

    //------------------------------------------------------------
    /**
     * シミュレーションのメインループ。
     * 初期化などは済んでいるものとする。
     * また、pause で止まった後の再開もこれで行う。
     */
    protected void simulateMainLoop() {
        if (finished || paused) {
            return;
        }
        while (!finished && !paused) {
            simulateOneStepBare() ;
            if (exitCount > 0 && counter > exitCount) {
                finished = true;
                break;
            }
        }
        if (finished) {
            finalize() ;
        }
    }

    /**
     * シミュレーションの終了処理。
     */
    protected void finalize() {
        // ログの書き出し。ログは、最後に出力。
        simulator.finalize() ;

        // 終了時ステータスの表示
        // ※注意) Time がプラス1余計にされている
        Itk.logInfo("Status", getStatusLine());
    }

    /**
     * エージェント登録
     */
    public abstract void registerAgent(AgentBase agent);

    /**
     * ステータスライン表示用の文字列
     */
    public String getStatusLine() {
        return simulator.getStatusLine();
    }
}
