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
 * Set Tag Event (SET/REMOVE)。
 * タグをリンクに追加/除去する。
 * <pre>
 *  { "type" : ( "SetTag" | "AddTag" | "RemoveTag" ),
 *    "atTime" : __Time__,
 *    ("placeTag" : __Tag__, |
 *     "placeId" : __PlaceId__,)
 *    ("onoff" : ( true | false ),)?
 *    "noticeTag" : __Tag__}
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 * "placeTag" もしくは "placeId" のいずれかを指定する。
 */
public class SetTagEvent extends PlacedEvent {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 付加・削除するタグ
     */
    public Term noticeTag = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 付加・削除の識別
     */
    public boolean onoff = true ;

    //----------------------------------------
    /**
     * JSON Term による setup
     */
    public void setupByJson(Scenario _scenario,
                            Term eventDef) {
        super.setupByJson(_scenario, eventDef) ;

        noticeTag = eventDef.getArgTerm("noticeTag") ;
        String eventType = eventDef.getArgString("type") ;
        if(eventType.equals("AddTag")) {
            onoff = true ;
        } else if (eventType.equals("RemoveTag")) {
            onoff = false ;
        } else if(eventDef.hasArg("onoff")) {
            onoff = eventDef.getArgBoolean("onoff") ;
        } else {
            onoff = true ;
        }
    }

    //----------------------------------------
    /**
     * CSV による setup
     */
    @Override
    public void setupByCsvColumns(Scenario _scenario,
                                  ShiftingStringList columns) {
        super.setupByCsvColumns(_scenario, columns) ;

        String command = columns.nth(3) ;
        String subcoms[] = command.split(":") ;
        if(subcoms.length < 2 || subcoms.length > 2 ||
           subcoms[1].length() == 0) {
            Itk.logWarn("Strange commands for SetTag event.") ;
            Itk.logWarn_("columns", columns) ;
        } else {
            noticeTag = new Term(subcoms[1], true) ;
        }
        if(subcoms[0].equals("SET")) {
            onoff = true ;
        } else if(subcoms[0].equals("REMOVE")) {
            onoff = false ;
        } else {
            Itk.logWarn("Strange commands for SetTag event.") ;
            Itk.logWarn_("columns", columns) ;
        }
    }

    //----------------------------------------
    /**
     * タグ操作イベント発生処理
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
     * タグ操作イベント発生逆処理
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
     * タグ操作イベント発生処理(generic)
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @param inverse : 逆操作かどうか
     * @return : true を返す。
     */
    public boolean occur(SimTime currentTime, NetworkMap map, boolean inverse) {
        for(MapLink link : map.getLinks()) {
            if(checkTagOrId(link)) {
                if(onoff ^ inverse) {
                    link.addTag(noticeTag.getString()) ;
                } else {
                    link.removeTag(noticeTag.getString()) ;
                }
            }
        }
        for(MapNode node : map.getNodes()) {
            if(checkTagOrId(node)) {
                if(onoff ^ inverse) {
                    node.addTag(noticeTag.getString()) ;
                } else {
                    node.removeTag(noticeTag.getString()) ;
                }
            }
        }
        return true ;
    }

    //----------------------------------------
    /**
     * 文字列化 後半
     */
    public String toStringTail() {
        return (super.toStringTail() +
                "," + "notice=" + noticeTag +
                "," + "on=" + onoff);
    }
} // class SetTagEvent


