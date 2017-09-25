// -*- mode: java; indent-tabs-mode: nil -*-
/** AgentFactoryConfig.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2017/09/25 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2017/09/25]: separate from AgentFactory. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents.Factory;

import java.util.List;

import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;

import nodagumi.Itk.*;

//======================================================================
/**
 * エージェント生成用設定情報用クラス
 * あまりに引数が多いので、整理。
 */
public class AgentFactoryConfig {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントのクラス名
     */
    public String agentClassName = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント設定情報 (JSON Object)
     */
    public Term agentConf = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 出発場所
     */
    public OBNode startPlace = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成条件
     */
    public String[] conditions = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 目的地
     */
    public Term goal = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 経路
     */
    public List<Term> plannedRoute ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 開始時刻
     */
    public SimTime startTime = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 持続時間
     */
    public double duration = 0.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成数
     */
    public int total = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * スピードモデル
     */
    public SpeedCalculationModel speedModel ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback 情報
     */
    public Term fallbackParameters = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 設定文字列（generation file 中の設定情報の文字列）
     */
    public String originalInfo = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成ルール名
     */
    public String ruleName = null ;
        
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 個別パラメータ
     */
    public IndividualConfigList individualConfigList = null;
        
    //------------------------------
    /**
     * JSONへの変換用
     */
    public Term toTerm() {
	Term jTerm = new Term() ;
	{ // agentType
	    Term agentType = new Term() ;
	    agentType.setArg("className", agentClassName) ;
	    agentType.setArg("config", agentConf) ;
	    jTerm.setArg("agentType", agentType) ;
	}
	jTerm.setArg("startPlace",startPlace) ;
	jTerm.setArg("conditions",conditions);
	jTerm.setArg("goal",goal);
	jTerm.setArg("plannedRoute",plannedRoute) ;
	jTerm.setArg("startTime",startTime.getAbsoluteTimeString()) ;
	jTerm.setArg("duration",duration) ;
	jTerm.setArg("total",total) ;
	jTerm.setArg("speedModel", speedModel) ;
	jTerm.setArg("name", ruleName) ;
	jTerm.setArg("individualConfig", individualConfigList.toTerm()) ;

	return jTerm ;
    }
} // end class AgentFactoryConfig

