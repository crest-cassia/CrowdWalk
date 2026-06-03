// -*- mode: java; indent-tabs-mode: nil -*-
/** Ruby Agent
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
 * Ruby によりエージェント毎の通過制御できる Gate。
 * {@link nodagumi.ananPJ.Scenario.Scenario} の
 * {@link nodagumi.ananPJ.Scenario.CloseGateEvent} の記述において、
 * 以下のように指定する。
 * <pre>
 *  { "type":"CloseGate",
 *    "gateClass":"RubyGate",
 *    "rubyClass":__GateClassInRuby__,
 *    "atTime":__Time__,
 *    "placeTag":__Tag__,
 *    "gateTag":__Tag__,
 *    __otherItem__:__Value__,
 *    ...}
 *  
 *  __Time__ ::= "hh:mm:ss"
 *  __GateClassInRuby__ : Ruby側の対応するクラス。
 * </pre>
 * <p>
 * __GateClassInRuby__ は、RubyGateBase クラスの子クラスとして
 * 読み込まれる Ruby プログラムの中で定義されている必要がある。
 * エージェントがこのゲートを通過する際には毎サイクル、
 * {@code isClosed(agent, currentTime)} メソッドが呼び出される。
 * このメソッドの返り値が {@code true} の場合は、エージェントは通過できず、
 * 足止めとなる。
 * 返り値が{@code false} の場合は通過できると判断される。
 * このメソッドの {@code super} は、
 * 通常の GateBase の {@code isClosed()}、つまり、
 * Gate が closed 状態なら {@code true}、open 状態なら {@code false} を返す。
 * __GateClassInRuby__ で {@code isClosed()} が定義されない場合、この
 * {@code super} がそのまま呼び出される。
 * </p><p>
 * {@code __otherItem__} を含め、上記のシナリオで指定したイベント定義情報は、
 * __GateClassInRuby__ から {@code getEventDef()} で参照できる。
 * {@code getEventDef()} は、イベント定義情報を {@link nodagumi.Itk.Term} の形式で
 * 返す。
 * </p><p>
 * 一度作られた gate は、__PlaceTag__ 毎に __GateTag__ で管理されるため、
 * 同じ __GateTag__ に対する {@link nodagumi.ananPJ.Scenario.GateEvent} は、
 * 新たにインスタンスを生成せず、もとのインスタンスが利用される。
 * よって、{@code getEventDef()} で取得できる情報は、初出の 
 * {@link nodagumi.ananPJ.Scenario.CloseGateEvent} に指定された情報となる。
 */
public class RubyGate extends GateBase {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby Engine
     */
    public ItkRuby rubyEngine = null ;
    
    /**
     * Ruby Object
     */
    public Object rubyGate = null ;
    
    /**
     * ルビークラス名
     */
    public String rubyClass ;

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     * @param _tag:: gate tag.
     * @param _event:: event definition in Term.
     * @param _closed:: initial state of closed value.
     */
    public RubyGate(String _tag, GateEvent _event, boolean _closed,
                    OBNode _place) {
	super(_tag, _event, _closed, _place) ;
	
	rubyClass = event.eventDef.getArgString("rubyClass") ;
	if(rubyClass == null) {
	    Itk.logError("ruby class is not specified in RubyGate.",
			 _tag, _event, _closed) ;
	    Itk.quitByError() ;
	}
	Itk.logDebug("RubyGate:rubyClass", rubyClass) ;

	setupRubyEngine(event.getSimulator().getRubyEngine()) ;
    }

    //------------------------------------------------------------
    /**
     * Ruby Engine の設定
     */
    public void setupRubyEngine(ItkRuby _rubyEngine) {
        if(_rubyEngine == null) {
            Itk.logError("RubyGate require Ruby Engine") ;
            Itk.quitByError() ;
        }
        
        rubyEngine = _rubyEngine ;

        rubyGate =
	    rubyEngine.newInstanceOfClass(rubyClass, this) ;
    }

    //------------------------------------------------------------
    /**
     * 閉じているかどうか？
     * @param agent: 対象となるエージェント
     * @param currentTime : シミュレーション時刻
     * @return このゲートが閉じているかどうか
     */
    public boolean isClosed(AgentBase agent, SimTime currentTime) {
	Object ret =
	    rubyEngine.callMethod(rubyGate, "isClosed",
				  agent, currentTime) ;
	return (boolean)ret ;
    }

    //------------------------------------------------------------
    /**
     * ゲートの開閉
     */
    public GateBase switchGate(GateEvent _event, boolean _closed) {
        rubyEngine.callMethod(rubyGate, "switchGate",
                              _event, _closed) ;
	return this ;
    }

    //----------------------------------------
    /**
     * ゲートの開閉の元定義。（Ruby CallBack 用）
     */
    public GateBase super_switchGate(GateEvent _event, boolean _closed) {
        super.switchGate(_event, _closed) ;
	return this ;
    }
    
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class RubyGate

