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
 * ShutOff Event (STOP)。
 * 指定した場所のエージェント生成をやめる。
 * <pre>
 *  { "type" : "ShutOff",
 *    "atTime" : __Time__,
 *    ("placeTag" : __Tag__, |
 *     "placeId" : __PlaceId__,)
 *  }
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 * "placeTag" もしくは "placeId" のいずれかを指定する。
 * <p>
 * [2015.01.21 I.Noda] 現状では生成停止だけだが、
 * いろいろな機能を実施できるようにしたほうが良いかもしれない。
 * その場合は、ControlEvent などに名前を買えるべきか？
 */
public class ShutOffEvent extends PlacedEvent {
    //----------------------------------------
    /**
     * 終了イベント発生処理
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
     * 終了イベント発生逆処理
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
     * 終了イベント発生処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    public boolean occur(SimTime currentTime, NetworkMap map, boolean inverse) {
	for(MapLink link : map.getLinks()) {
            if(checkTagOrId(link)) {
		link.letShutOff(!inverse) ;
	    }
	}
	return true ;
    }
} // class ShutOffEvent

