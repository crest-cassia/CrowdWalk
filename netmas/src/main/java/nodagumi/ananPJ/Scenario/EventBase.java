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
    public double atTime = Double.MAX_VALUE ;

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
        atTime = Scenario.convertToTimeValue(atTimeString) ;
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
	atTime = Scenario.convertToTimeValue(atTimeString) ;
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
     * 相対時刻
     * @return scenario の origin time からの相対時刻
     */
    public double getRelativeTime() {
	return scenario.calcRelativeTime(atTime) ;
    }

    //----------------------------------------
    /**
     * 相対時刻
     * @return scenario の origin time からの相対時刻
     */
    public double getAbsoluteTime() {
	return atTime ;
    }

    //----------------------------------------
    /**
     * 発生チェックと実行(以前の checkIfHappend)
     * @param time : 現在の絶対時刻
     * @return 実施したら true
     */
    public boolean tryOccur(double time, NetworkMapBase map) {
	if(shouldOccurAt(time)) {
	    isCompleted = occur(time, map) ;
	    return true ;
	} else {
	    return false ;
	}
    }

    //----------------------------------------
    /**
     * 発生時刻チェック
     * @param time : 現在の絶対時刻
     * @return 発生時刻なら true
     */
    public boolean shouldOccurAt(double time) {
	return !isCompleted && time >= atTime ;
    }

    //----------------------------------------
    /**
     * イベント発生処理 (以前の setEnabled(true))
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : 完了したら true を返す。false を返すと、延々呼び出される。
     */
    abstract public boolean occur(double time, NetworkMapBase map) ;

    //----------------------------------------
    /**
     * イベント発生逆処理 (以前の setEnabled(false))
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : 完了したら true を返す。
     */
    abstract public boolean unoccur(double time, NetworkMapBase map) ;

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

