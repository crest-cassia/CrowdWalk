// -*- mode: java; indent-tabs-mode: nil -*-
/** PauseBase.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2017/12/03 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2017/12/03]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.ananPJ.BasicSimulationLauncher;

import nodagumi.Itk.* ;

//============================================================
/**
 * Pause Event。
 * An event to pause the running simulation.
 * This is effective only in Gui mode.
 * In Cui mode, it will be ignored.
 * <pre>
 *  { "type" : "Pause",
 *    "atTime" : __Time__ }
 *    ... }
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 */
public class PauseEvent extends EventBase {
    //----------------------------------------
    /**
     * Pause イベント発生処理
     * Guiモードの時、シミュレーションを一旦中断。
     * Cuiモードでは何もしない。
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
        getScenario().getSimulationLauncher().pause() ;
	return true ;
    }

    //----------------------------------------
    /**
     * Start イベント発生逆処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(SimTime currentTime, NetworkMap map) {
	return true ;
    }
} // class NullEvent

