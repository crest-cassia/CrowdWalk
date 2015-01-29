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
 * Alert Event (EVACUATE)
 * 指定した場所のエージェントが避難に入る
 * [2015.01.21 I.Noda] 現状では避難だけだが、
 * いろいろな指示（エージェントの意思変更）・情報提供(エージェントへの条件付与)
 * できるようにしたほうが良い。
 */
public class AlertEvent extends PlacedEvent {
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
    } ;

    //----------------------------------------
    /**
     * 終了イベント発生処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @param inverse : 逆操作かどうか
     * @return : true を返す。
     */
    public boolean occur(double time, NetworkMapBase map, boolean inverse) {
	for(MapLink link : map.getLinks()) {
	    if(link.hasTag(placeTag)) {
		link.setEmergency(!inverse) ;
	    }
	}
	return true ;
    }
} // class AlertEvent

