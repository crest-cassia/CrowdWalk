// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.*;
import java.util.*;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Agents.*;
import nodagumi.ananPJ.Agents.WalkAgent.*;
import nodagumi.ananPJ.Simulator.*;
import nodagumi.ananPJ.misc.*;

import nodagumi.Itk.*;

//======================================================================
/**
 * コマンドライン用シミュレーションランチャー
 */
public class CuiSimulationLauncher extends BasicSimulationLauncher {

    //------------------------------------------------------------
    /**
     * constructor
     */
    public CuiSimulationLauncher(Random _random) {
        super(_random);
    }

    //------------------------------------------------------------
    /**
     * constructor
     */
    public CuiSimulationLauncher(String _propertiesPath,
                                 ArrayList<String> commandLineFallbacks) {
        super(null) ;
        // load properties
        setPropertiesFromFile(_propertiesPath, commandLineFallbacks) ;

        if(!setupNetworkMap()) {
            Itk.logFatal("can not read map file.") ;
            Itk.quitByError() ;
        }
    }

    //------------------------------------------------------------
    /**
     * マップファイルチェック
     */
    private boolean setupNetworkMap() {
        // check property options
        if (getNetworkMapFile() == null) {
            Itk.logError("No map file is specified.") ;
            return false;
        } else if (!((File) new File(getNetworkMapFile())).exists()) {
            Itk.logError("Map file does not exist.", getNetworkMapFile()) ;
            return false;
        }
        try {
            map = readMapWithName(getNetworkMapFile()) ;
        } catch (IOException ioe) {
            Itk.logError("error in reading map file:", getNetworkMapFile()) ;
            Itk.dumpStackTraceOf(ioe);
            return false ;
        }
        return true ;
    }

    //------------------------------------------------------------
    /**
     * 初期化
     */
    public void initialize() {
        initializeSimulatorEntity() ;
    }

    //------------------------------------------------------------
    /**
     * シミュレータ開始
     */
    public void start() {
        Itk.logDebug("CuiSimulationLauncher start!");
        paused = false ;
        simulateMainLoop() ;
    }

    /**
     * ディスプレーを持つかどうか。
     */
    public boolean hasDisplay() {
        return false;
    }

    /**
     * EvacuationSimulator のステップ処理が終了してカウントアップされる直前に呼び出される。
     */
    public void updateEveryTick(SimTime currentTime) {
        // 何もしない
    }

    /**
     * エージェント登録
     */
    public void registerAgent(AgentBase agent) {
        // 何もしない
    }
}
