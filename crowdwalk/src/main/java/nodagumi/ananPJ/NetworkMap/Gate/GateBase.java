// -*- mode: java; indent-tabs-mode: nil -*-
/** GateBase
 * @author:: Itsuki Noda
 * @version:: 0.0 2018/09/21 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2018/09/21]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */
package nodagumi.ananPJ.NetworkMap.Gate;

import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Scenario.GateEvent;
import nodagumi.ananPJ.misc.SimTime;


import nodagumi.Itk.*;


//======================================================================
/**
 * 通行規制制御用クラス
 */
public class GateBase {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * OBNode内でこのゲートを参照するためのタグ
     */
    public String tag;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * もととなる初出Event 
     */
    public GateEvent event ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Gate が設置された場所
     */
    public OBNode place ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 現在閉じている（通行止め）かどうか？
     */
    public boolean closed ;

    //----------------------------------------
    /**
     * コンストラクタ
     */
    public GateBase(String _tag, GateEvent _event, boolean _closed,
                    OBNode _place) {
	tag = _tag ;
	event = _event ;
	closed = _closed ;
        place = _place ;
    }

    //----------------------------------------
    /**
     * 閉じているかどうか？
     * 拡張のために、時刻とエージェントを受け取る。
     * @param agent: 対象となるエージェント
     * @param currentTime : シミュレーション時刻
     * @return デフォルトでは、単にこのゲートが閉じているかどうか
     */
    public boolean isClosed(AgentBase agent, SimTime currentTime) {
	return isClosed() ;
    }

    //----------------------------------------
    /**
     * 閉じているかどうか？
     * 拡張のために、時刻とエージェントを受け取る。
     * @param currentTime : シミュレーション時刻
     * @param agent: 対象となるエージェント
     * @return デフォルトでは、単にこのゲートが閉じているかどうか
     */
    public boolean isOpened(AgentBase agent, SimTime currentTime) {
	return !isClosed(agent, currentTime) ;
    }

    //----------------------------------------
    /**
     * 閉じているかどうか？
     */
    public boolean isClosed() {
	return closed ;
    }

    //----------------------------------------
    /**
     * 開いているかどうか？
     */
    public boolean isOpened() {
	return !isClosed() ;
    }

    //----------------------------------------
    /**
     * ゲートの開閉
     */
    public GateBase switchGate(GateEvent _event, boolean _closed) {
	closed = _closed ;
	return this ;
    }

    //----------------------------------------
    /**
     * ゲートを閉じる
     */
    public GateBase close() {
	return switchGate(null, true) ;
    }

    //----------------------------------------
    /**
     * ゲートを開ける
     */
    public GateBase open() {
	return switchGate(null, false) ;
    }

    //------------------------------------------------------------
    // アクセスメソッド。（主としてrubyより)
    //----------------------------------------
    /**
     * closed の値取得。
     */
    public boolean getClosed() {
	return closed ;
    }

    //----------------------------------------
    /**
     * tag の値取得。
     */
    public String getTag() {
	return tag ;
    }

    //----------------------------------------
    /**
     * イベント定義の取得。
     */
    public GateEvent getEvent() {
	return event ;
    } ;
    
    //----------------------------------------
    /**
     * 場所の取得。
     */
    public OBNode getPlace() {
	return place ;
    } ;
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class GateBase

