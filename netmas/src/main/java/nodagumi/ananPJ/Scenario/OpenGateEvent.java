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
 * OpenGate Event (REMOVE_STOP)
 * ゲートを開く。
 */
public class OpenGateEvent extends GateEvent {
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
		    link.openGate(gateTag.getString()) ;
		} else {
		    link.closeGate(gateTag.getString()) ;
		}
	    }
	}
	for(MapNode node : map.getNodes()) {
	    if(node.hasTag(placeTag)) {
		if(!inverse) {
		    node.openGate(gateTag.getString()) ;
		} else {
		    node.closeGate(gateTag.getString()) ;
		}
	    }
	}
	return true ;
    }
} // class OpenGateEvent

