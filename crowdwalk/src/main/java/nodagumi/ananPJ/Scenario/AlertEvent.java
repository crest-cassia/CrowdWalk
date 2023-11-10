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
 * Alert Event (EVACUATE)。
 * 指定した場所のエージェントにメッセージを伝える。
 * <pre>
 *  { "type" : "Alert",
 *    "atTime" : __Time__,
 *    ("placeTag" : __Tag__, |
 *     "placeId" : __PlaceId__,)
 *    ("onoff" : ( true | false ),)?
 *    "message" : __String__}
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 * "placeTag" もしくは "placeId" のいずれかを指定する。
 * {@code "message"} の利用は、
 * {@link nodagumi.ananPJ.Agents.RationalAgent RationalAgent} の
 * {@link nodagumi.ananPJ.Agents.Think.ThinkFormulaAgent#call_listenAlert listenAlert} を
 * 参照。
 * <p>
 * [2015.01.21 I.Noda] 現状では避難だけだが、
 * いろいろな指示（エージェントの意思変更）・情報提供(エージェントへの条件付与)
 * できるようにしたほうが良い。
 * <p>
 * [2015.0216 I.Noda] 上記を実装。
 */
public class AlertEvent extends PlacedEvent {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 情報を示すフラグ
     */
    public Term message = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 正操作か逆操作かどうか
     * onoff == true ならば、Alert をセット
     * そうでなければ、Alert を clear する。
     */
    public boolean onoff = true ;

    //----------------------------------------
    /**
     * JSON Term による setup
     */
    public void setupByJson(Scenario _scenario,
                            Term eventDef) {
        super.setupByJson(_scenario, eventDef) ;

        message = eventDef.getArgTerm("message") ;
        if(eventDef.hasArg("onoff")) {
            onoff = eventDef.getArgBoolean("onoff") ;
        } else {
            onoff = true ;
        }
    }

    //----------------------------------------
    /**
     * 終了イベント発生処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
        return occur(currentTime, map, !onoff) ;
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
        return occur(currentTime, map, onoff) ;
    } ;

    //----------------------------------------
    /**
     * 終了イベント発生処理
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @param inverse : 逆操作かどうか
     * @return : true を返す。
     */
    public boolean occur(SimTime currentTime, NetworkMap map, boolean inverse) {
        for(MapLink link : map.getLinks()) {
            if(checkTagOrId(link)) {
                link.addAlertMessage(message, currentTime, !inverse) ;
                Itk.logInfo("AlertEvent",onoff,link,message, currentTime) ;
            }
        }
        return true ;
    }
} // class AlertEvent

