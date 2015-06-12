// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator.Pollution;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Simulator.Pollution.PollutionBase;
import nodagumi.ananPJ.misc.SpecialTerm;
import nodagumi.Itk.*;

// 蓄積型(ガス等)
public class AccumulatedPollution extends PollutionBase {
    // 参考データ: 三井化学アグロ株式会社 / クロルピクリンの物性・分解性 / 気中濃度と人体への影響
    //   http://www.mitsui-agro.com/tabid/147/Default.aspx#04a4
    //
    // 致死
    public static final double DEADLY_LEVEL = 297.6 * 10 * 60;          // 178,560
//  public static final double DEADLY_LEVEL = 119.0 * 30 * 60;          // 214,200
    // 不耐
    public static final double UNBEARABLE_LEVEL = 15.0 * 1 * 60;        // 900
//  public static final double UNBEARABLE_LEVEL = 7.5 * 10 * 60;        // 4,500
    // 催涙により眼を開けていられない
    public static final double INCAPACITATED_LEVEL = 0.3 * 0.5 * 60;    // 9
    // 最低刺激
    public static final double IRRITANT_LEVEL = 1.3;

    private int lastTriageLevel = 0;    // 更新チェック用

    public AccumulatedPollution() {}

    public PollutionBase.PollutionEffectInfo newEffectInfo(AgentBase agent) {
        return new PollutionEffectInfo(agent, this) ;
    }

    public void expose(AgentBase agent, double pollutionLevel) {
        PollutionEffectInfo effectInfo =
            (PollutionEffectInfo)agent.pollutionEffect ;
        if (! agent.isEvacuated()) {
            effectInfo.currentExposureAmount = pollutionLevel;
            effectInfo.accumulatedExposureAmount += pollutionLevel;
        }

        int triageLevel = getTriage(agent);
        if (triageLevel != 0) {
	    agent.setGoal(SpecialTerm.Emergency) ;
        }
        if (triageLevel != lastTriageLevel) {
            agent.getNetworkMap().getNotifier().agentTriageChanged(agent);
            lastTriageLevel = triageLevel;
        }
    }

    // 呼び出し元で speed を更新した後に呼ばれる
    public void effect(AgentBase agent) {
        PollutionEffectInfo effectInfo =
            (PollutionEffectInfo)agent.pollutionEffect ;
        if (effectInfo.accumulatedExposureAmount >= INCAPACITATED_LEVEL) {
            agent.setSpeed(0.0);
        } else if (effectInfo.accumulatedExposureAmount >= IRRITANT_LEVEL) {
            agent.setSpeed(agent.getSpeed() * (INCAPACITATED_LEVEL - effectInfo.accumulatedExposureAmount) / INCAPACITATED_LEVEL);
        }
    }

    /* the state of the agent */
    public int getTriage(AgentBase agent) {
        PollutionEffectInfo effectInfo =
            (PollutionEffectInfo)agent.pollutionEffect ;
        if (effectInfo.accumulatedExposureAmount >= DEADLY_LEVEL)
            return 3;
        else if (effectInfo.accumulatedExposureAmount >= UNBEARABLE_LEVEL)
            return 2;
        else if (effectInfo.accumulatedExposureAmount >= INCAPACITATED_LEVEL)
            return 1;
        else
            return 0;
    }

    /**
     * エージェントの死亡判定
     */
    public boolean isDead(AgentBase agent) {
        return getTriage(agent) >= 3;
    }

    //============================================================
    //============================================================
    /**
     * Pollution の状態をエージェント毎に保持する構造体。
     */
    public static class PollutionEffectInfo
        extends PollutionBase.PollutionEffectInfo {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 現状の暴露量
         */
        public double currentExposureAmount = 0.0 ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 累積暴露量
         */
        public double accumulatedExposureAmount = 0.0;

        //--------------------------------------------------
        /**
         * コンストラクタ
         */
        public PollutionEffectInfo(AgentBase _agent, PollutionBase _pollution) {
            super(_agent, _pollution) ;
            currentExposureAmount = 0.0 ;
            accumulatedExposureAmount = 0.0;
        }

        //--------------------------------------------------
        /**
         * dumpResult 用の値。
         */
        public double currentValueForLog() {
            return currentExposureAmount ;
        }

        //--------------------------------------------------
        /**
         * dumpResult 用の値。
         */
        public double accumulatedValueForLog() {
            return accumulatedExposureAmount ;
        }

    }

}
