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
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.* ;

//============================================================
/**
 * OpenGate Event (REMOVE_STOP)。
 * 指定したタグを持つリンク・ノードの、指定したタグのゲートを開く。
 * <pre>
 *  { "type" : "OpenGate",
 *    "atTime" : __Time__,
 *    ("placeTag" : __Tag__, |
 *     "placeId" : __PlaceId__,)
 *    "gateTag" : __Tag__}
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 * "placeTag" もしくは "placeId" のいずれかで場所を指定する。
 */
public class OpenGateEvent extends GateEvent {
    //----------------------------------------
    /**
     * ゲート開放イベント発生処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
	return occur(currentTime, map, false) ;
    }

    //----------------------------------------
    /**
     * ゲート開放イベント発生逆処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(SimTime currentTime, NetworkMap map) {
	return occur(currentTime, map, true) ;
    }

    //----------------------------------------
    /**
     * ゲート開放イベント発生処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    public boolean occur(SimTime currentTime, NetworkMap map, boolean inverse) {
	for(MapLink link : map.getLinks()) {
            if(checkTagOrId(link)) {
		if(!inverse) {
		    link.openGate(gateTag,this) ;
		} else {
		    link.closeGate(gateTag,this) ;
		}
	    }
	}
	for(MapNode node : map.getNodes()) {
            if(checkTagOrId(node)) {
		if(!inverse) {
		    node.openGate(gateTag,this) ;
		} else {
		    node.closeGate(gateTag,this) ;
		}
	    }
	}
	return true ;
    }
} // class OpenGateEvent

