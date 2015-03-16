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

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;

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
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(double time, NetworkMapBase map) {
	scenario.letFinished() ;
	return true ;
    }

    //----------------------------------------
    /**
     * 終了イベント発生逆処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(double time, NetworkMapBase map) {
	scenario.letFinished(false) ;
	return true ;
    }
} // class FinishEvent

