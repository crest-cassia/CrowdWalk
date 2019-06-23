// -*- mode: java; indent-tabs-mode: nil -*-
/** RubyEvent.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2019/06/23 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2019/06/23]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import java.util.HashMap;
import java.util.ArrayList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.Factory.AgentFactory;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryList;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryConfig;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.* ;

//======================================================================
/**
 * This event cause to call RubyEvent object defined by users.
 * <pre>
 *  { "type" : "Ruby" 
 *    "atTime" : __Time__,
 *    "rubyClass" : __EventClassInRuby__,
 *    __otherItem__:__Value__,
 *
 *  __Time__ ::= "hh:mm:ss"
 *  __EventClassInRuby__ : Ruby側の対応するクラス。
 * </pre>
 * <p>
 * __EventClassInRuby__ は、RubyEventBase クラスの子クラスとして
 * 読み込まれる Ruby プログラムの中で定義されている必要がある。
 * "atTime" で指定した時刻に、このイベントの occur() が呼び出される。
 */
public class RubyEvent extends EventBase {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby Engine
     */
    public ItkRuby rubyEngine = null ;
    
    /**
     * Ruby Object
     */
    public Object rubyEvent = null ;
    
    /**
     * ルビークラス名
     */
    public String rubyClass ;

    //------------------------------------------------------------
    /**
     * JSON Term による setup.
     * format と filename を設定する。
     */
    public void setupByJson(Scenario _scenario,
                            Term _eventDef) {
        super.setupByJson(_scenario, _eventDef) ;
	
	rubyClass = eventDef.getArgString("rubyClass") ;
	if(rubyClass == null) {
	    Itk.logError("ruby class is not specified in RubyEvent.",
			 _eventDef) ;
	    Itk.quitByError() ;
	}
	Itk.logDebug("RubyEvent:rubyClass", rubyClass) ;

    }

    //------------------------------------------------------------
    /**
     * Ruby Engine の設定
     */
    public void setupRubyEngine(ItkRuby _rubyEngine) {
        if(_rubyEngine == null) {
            Itk.logError("RubyEvent require Ruby Engine") ;
            Itk.quitByError() ;
        }
        
        rubyEngine = _rubyEngine ;

        rubyEvent =
	    rubyEngine.newInstanceOfClass(rubyClass, this) ;
    }

    //------------------------------------------------------------
    /**
     * Ruby イベント発生処理。
     * エージェントの状態をダンプする。
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
	/* 初回は、ruby のセットアップ */
	if(rubyEvent == null) {
	    setupRubyEngine(getSimulator().getRubyEngine()) ;
	}
	
	Object ret =
	    rubyEngine.callMethod(rubyEvent, "occur",
				  currentTime, map) ;
	return (boolean)ret ;
    }

    //------------------------------------------------------------
    /**
     * Ruby イベント発生逆処理。
     * 何もしない。
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(SimTime currentTime, NetworkMap map) {
	return true ;
    }

    //------------------------------------------------------------
    /**
     * 文字列化 後半
     */
    public String toStringTail() {
        return (super.toStringTail() +
                "," + "rubyClass=" + rubyClass) ;
    }
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class RubyEvent

