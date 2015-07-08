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
import nodagumi.ananPJ.misc.SimClock;

import nodagumi.Itk.* ;

//============================================================
/**
 * EventBase class
 */
abstract public class EventBase {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 所属するシナリオ。
     */
    public Scenario scenario = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ID。
     * シナリオ内のイベント相互で参照するためのID。
     * null でも良いが、他から参照できない。
     */
    public String id = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * イベント発生時刻文字列
     */
    public String atTimeString = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * イベント発生時刻 (絶対時刻)
     */
    public SimClock atTime = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * コメント
     */
    public String comment = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 過去の（既に完了した）ものか？
     */
    public boolean isCompleted = false ;

    //----------------------------------------
    /**
     * JSON Term による setup
     */
    public void setupByJson(Scenario _scenario,
                            Term eventDef) {
        scenario = _scenario ;

        id = eventDef.getArgString("id") ;
        atTimeString = eventDef.getArgString("atTime") ;
    }

    //----------------------------------------
    /**
     * CSV による setup
     */
    public void setupByCsvColumns(Scenario _scenario,
                                  ShiftingStringList columns) {
        scenario = _scenario ;

        if(columns.nth(0).length() > 0) {
            id = columns.nth(0) ;
        }

        atTimeString = columns.nth(4) ;
    }

    //----------------------------------------
    /**
     * originClock によるイベント発生時刻の設定。
     */
    public void setupAtTime(SimClock originClock) {
        atTime = originClock.newClockByString(atTimeString) ;
    }

    //----------------------------------------
    /**
     * id を持つかどうか
     * @return 持てば true
     */
    public boolean hasId() {
        return id != null ;
    }

    //----------------------------------------
    /**
     * 発生チェックと実行(以前の checkIfHappend)
     * @param clock : 現在の絶対時刻
     * @return 実施したら true
     */
    public boolean tryOccur(SimClock clock, NetworkMap map) {
        if(shouldOccurAt(clock)) {
            isCompleted = occur(clock, map) ;
            return true ;
        } else {
            return false ;
        }
    }

    //----------------------------------------
    /**
     * 発生時刻チェック
     * @param clock : 現在の絶対時刻
     * @return 発生時刻なら true
     */
    public boolean shouldOccurAt(SimClock clock) {
        return !isCompleted && clock.isAfterOrAt(atTime) ;
    }

    //----------------------------------------
    /**
     * イベント発生処理 (以前の setEnabled(true))
     * @param clock : 現在の絶対時刻
     * @param map : 地図データ
     * @return : 完了したら true を返す。false を返すと、延々呼び出される。
     */
    abstract public boolean occur(SimClock clock, NetworkMap map) ;

    //----------------------------------------
    /**
     * イベント発生逆処理 (以前の setEnabled(false))
     * @param clock : 現在の絶対時刻
     * @param map : 地図データ
     * @return : 完了したら true を返す。
     */
    abstract public boolean unoccur(SimClock clock, NetworkMap map) ;

    //----------------------------------------
    /**
     * 文字列化
     */
    public String toString() {
        return (getClass().getSimpleName() +
                "[" + id + ":" +
                "," + "@" + atTimeString +
                toStringTail() + "]") ;
    }

    //----------------------------------------
    /**
     * 文字列化 後半
     */
    public String toStringTail() {
        return "" ;
    }

} // class EventBase

