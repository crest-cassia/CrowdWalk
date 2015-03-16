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
 * CloseGate Event (ADD_STOP)。
 * 指定したタグを持つリンク・ノードの、指定したタグのゲートを閉じる。
 * <pre>
 *  { "type" : "CloseGate",
 *    "atTime" : __Time__,
 *    "placeTag" : __Tag__,
 *    "gateTag" : __Tag__}
 * 
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 */
public class CloseGateEvent extends GateEvent {
    //----------------------------------------
    /**
     * ゲート開放イベント発生処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(double time, NetworkMapBase map) {
	return occur(time, map, false) ;
    }

    //----------------------------------------
    /**
     * ゲート開放イベント発生逆処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(double time, NetworkMapBase map) {
	return occur(time, map, true) ;
    }

    //----------------------------------------
    /**
     * ゲート開放イベント発生処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    public boolean occur(double time, NetworkMapBase map, boolean inverse) {
	for(MapLink link : map.getLinks()) {
	    if(link.hasTag(placeTag)) {
		if(!inverse) {
		    link.closeGate(gateTag.getString()) ;
		} else {
		    link.openGate(gateTag.getString()) ;
		}
	    }
	}
	for(MapNode node : map.getNodes()) {
	    if(node.hasTag(placeTag)) {
		if(!inverse) {
		    node.closeGate(gateTag.getString()) ;
		} else {
		    node.openGate(gateTag.getString()) ;
		}
	    }
	}
	return true ;
    }
} // class CloseGateEvent

