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
     * 現在閉じている（通行止め）かどうか？
     */
    public boolean closed ;

    //----------------------------------------
    /**
     * コンストラクタ
     */
    public GateBase(String _tag, GateEvent _event, boolean _closed) {
	tag = _tag ;
	event = _event ;
	closed = _closed ;
    }

    //----------------------------------------
    /**
     * 閉じているかどうか？
     * 拡張のために、時刻とエージェントを受け取る。
     * @param currnetTime : シミュレーション時刻
     * @param agent: 対象となるエージェント
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
    public GateBase switchGate(boolean _closed) {
	closed = _closed ;
	return this ;
    }

    //----------------------------------------
    /**
     * ゲートを閉じる
     */
    public GateBase close() {
	return switchGate(true) ;
    }

    //----------------------------------------
    /**
     * ゲートを開ける
     */
    public GateBase open() {
	return switchGate(false) ;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class GateBase

