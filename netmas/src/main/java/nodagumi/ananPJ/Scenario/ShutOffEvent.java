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
 * ShutOff Event (STOP)
 * 指定した場所のエージェント生成をやめる。
 * [2015.01.21 I.Noda] 現状では生成停止だけだが、
 * いろいろな機能を実施できるようにしたほうが良いかもしれない。
 * その場合は、ControlEvent などに名前を買えるべきか？
 */
public class ShutOffEvent extends PlacedEvent {
    //----------------------------------------
    /**
     * 終了イベント発生処理
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
     * 終了イベント発生逆処理
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
     * 終了イベント発生処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    public boolean occur(double time, NetworkMapBase map, boolean inverse) {
	for(MapLink link : map.getLinks()) {
	    if(link.hasTag(placeTag)) {
		link.letShutOff(!inverse) ;
	    }
	}
	return true ;
    }
} // class ShutOffEvent

