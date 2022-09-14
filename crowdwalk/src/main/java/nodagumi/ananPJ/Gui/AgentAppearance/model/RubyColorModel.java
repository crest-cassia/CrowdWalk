package nodagumi.ananPJ.Gui.AgentAppearance.model;

import java.awt.Color;
import java.util.ArrayList;
import java.lang.Integer;
import java.lang.Long;
import org.jruby.RubyArray;

import nodagumi.ananPJ.Agents.AgentBase;

import nodagumi.Itk.*;

/**
 * エージェントの歩行速度に応じて色を変える
 */
public class RubyColorModel extends ColorModel {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デフォルトのRuby Colorクラスの名前。
     * Ruby の中でのクラス名。
     */
    static final public String Fallback_RubyColorClass = "RubyColorBase" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 実際のエージェントクラスの名前。
     * Ruby の中でのクラス名。
     */
    private String rubyColorClass = Fallback_RubyColorClass ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 実際のエージェントクラスの名前。
     * Ruby の中でのクラス名。
     */
    private Object rubyColor = null;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public RubyColorModel() {}

    /**
     * 初期設定
     */
    public void init() throws Exception {
	Itk.logInfo("Ruby Color Model", "initializing...") ;
	if(getRubyEngine() == null) {
	    throw new Exception("Can not use RubyColor model without rubyEngine.") ;
	}
	
        rubyColorClass = getStringParameter("rubyColorClass", rubyColorClass) ;
	rubyColor = getRubyEngine().newInstanceOfClass(rubyColorClass) ;
    }

    /**
     * 初期設定
     */
    public ItkRuby getRubyEngine() {
	return getSimulator().getRubyEngine() ;
    }
	    
    /**
     * エージェントをセットする
     */
    public RubyColorModel setAgent(AgentBase agent) {
	RubyArray colorList = 
	    (RubyArray)(getRubyEngine().callMethod(rubyColor,
						   "getAgentColorRGB",
						   agent)) ;
	Itk.dbgVal("color", colorList) ;
	Itk.dbgVal("color.length", colorList.length()) ;

	int r = ((Long)colorList.get(0)).intValue() ;
	int g = ((Long)colorList.get(1)).intValue() ;
	int b = ((Long)colorList.get(2)).intValue() ;

	setPackedRGBValue(r, g, b) ;

        return this;
    }
}
