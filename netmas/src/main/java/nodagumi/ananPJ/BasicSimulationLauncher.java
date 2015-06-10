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

import nodagumi.Itk.*;

//======================================================================
/**
 * GUI/CUI 共通の部分を記述する。
 */
public abstract class BasicSimulationLauncher {
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

    //------------------------------------------------------------
    /**
     * constructor
     */
    public BasicSimulationLauncher(Random _random) {
        random = _random ;
    }

    //------------------------------------------------------------
    /**
     * シミュレータ実体の取り出し
     */
    public EvacuationSimulator getSimulator() { return simulator; }

    //------------------------------------------------------------
    /**
     * マップ取得
     */
    public NetworkMap getMap() { return networkMap; }

    //------------------------------------------------------------
    /**
     * 地図の読み込み
     */
    protected NetworkMap readMapWithName(String file_name, Random _random)
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
        if (properties != null) {
            // NetworkMap の生成時に random オブジェクトを初期化する
            // (CUIモードとGUIモードでシミュレーション結果を一致させるため)
            _random.setSeed(properties.getRandseed());
        }
        // NetMAS based map
        NetworkMap network_map = new NetworkMap(_random);
        if (false == network_map.fromDOM(doc))
            return null;
        Itk.logInfo("Load Map File", file_name) ;
        network_map.setFileName(file_name);
        return network_map;
    }

    //------------------------------------------------------------
    /**
     * プロパティへの橋渡し。
     */
    public NetmasPropertiesHandler getProperties() {
        return properties;
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
