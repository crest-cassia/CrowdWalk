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

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Scenario.GateEvent;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;


//======================================================================
/**
 * Ruby によりエージェント毎の通過制御できる Gate
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
     * @param _eventDef:: event definition in Term.
     * @param _closed:: initial state of closed value.
     */
    public RubyGate(String _tag, GateEvent _event, boolean _closed) {
	super(_tag, _event, _closed) ;
	
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
            Itk.logError("Generation Rule by Ruby require Ruby Engine") ;
            System.exit(1) ;
        }
        
        rubyEngine = _rubyEngine ;

        rubyGate =
	    rubyEngine.newInstanceOfClass(rubyClass, this) ;
    }

    //------------------------------------------------------------
    /**
     * ruby からのアクセスメソッド。
     */
    public GateEvent getEvent() {
	return event ;
    } ;
    
    //------------------------------------------------------------

    /**
     * 閉じているかどうか？
     * @param currnetTime : シミュレーション時刻
     * @param agent: 対象となるエージェント
     * @return このゲートが閉じているかどうか
     */
    public boolean isClosed(AgentBase agent, SimTime currentTime) {
	Object ret =
	    rubyEngine.callMethod(rubyGate, "isClosed",
				  agent, currentTime) ;
	return (boolean)ret ;
    }
	
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class RubyGate

