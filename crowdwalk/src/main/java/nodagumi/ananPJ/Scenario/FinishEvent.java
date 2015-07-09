// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk EventBase.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/01/29 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/01/29]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;
import nodagumi.ananPJ.misc.SimClock.SimTime;

import nodagumi.Itk.* ;

//============================================================
/**
 * Finish Event。
 * シミュレーション終了。
 * <pre>
 *  { "type" : "Finish",
 *    "atTime" : __Time__}
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 */
public class FinishEvent extends EventBase {
    //----------------------------------------
    /**
     * 終了イベント発生処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
	scenario.letFinished() ;
	return true ;
    }

    //----------------------------------------
    /**
     * 終了イベント発生逆処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(SimTime currentTime, NetworkMap map) {
	scenario.letFinished(false) ;
	return true ;
    }
} // class FinishEvent

