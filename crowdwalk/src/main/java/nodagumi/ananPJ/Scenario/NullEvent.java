// -*- mode: java; indent-tabs-mode: nil -*-
/** NullBase.java
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

import nodagumi.Itk.* ;

//============================================================
/**
 * Null Event。
 * an event to do nothing.
 * All attributes are ignored.
 * So, it can be used as an commented event.
 * <pre>
 *  { "type" : "None",
 *    ... }
 * </pre>
 */
public class NullEvent extends EventBase {
    //----------------------------------------
    /**
     * Null イベント発生処理
     * 実は何もしない
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
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

