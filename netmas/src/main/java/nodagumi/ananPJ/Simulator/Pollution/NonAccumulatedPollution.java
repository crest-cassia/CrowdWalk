// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator.Pollution;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Simulator.Pollution.PollutionBase;
import nodagumi.Itk.*;

// 非蓄積型(洪水等)
public class NonAccumulatedPollution extends PollutionBase {
    private int lastTriageLevel = 0;    // 更新チェック用

    public NonAccumulatedPollution() {}

    public PollutionBase.PollutionEffectInfo newEffectInfo(AgentBase agent) {
        return new PollutionEffectInfo(agent, this) ;
    }

    public void expose(AgentBase agent, double pollutionLevel) {
        PollutionEffectInfo effectInfo =
            (PollutionEffectInfo)agent.pollutionEffect ;

        if (! agent.isEvacuated()) {
            effectInfo.currentExposureAmount = pollutionLevel;
        }

        int triageLevel = getTriage(agent);
        if (triageLevel != lastTriageLevel) {
            agent.getNetworkMap().getNotifier().agentTriageChanged(agent);
            lastTriageLevel = triageLevel;
        }
    }

    /* effect of flood, this damage does not increase */
    // 呼び出し元で speed を更新した後に呼ばれる
    public void effect(AgentBase agent) {
        PollutionEffectInfo effectInfo =
            (PollutionEffectInfo)agent.pollutionEffect ;
        if (effectInfo.currentExposureAmount > 10.0) {
            effectInfo.currentExposureAmount = 10.0;
        }
        // 水深1.2mで歩行不可能
        // ※スピードがマイナスになるのを避ける
        agent.setSpeed(agent.getSpeed() * (1.2 - Math.min(effectInfo.currentExposureAmount, 1.2)) / 1.2);
    }

    /* the state of the agent */
    public int getTriage(AgentBase agent) {
        PollutionEffectInfo effectInfo =
            (PollutionEffectInfo)agent.pollutionEffect ;
        if (effectInfo.currentExposureAmount >= 1.5)
            return 3;
        else if (effectInfo.currentExposureAmount >= 0.8)
            return 2;
        else if (effectInfo.currentExposureAmount >= 0.3)
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

        //--------------------------------------------------
        /**
         * コンストラクタ
         */
        public PollutionEffectInfo(AgentBase _agent, PollutionBase _pollution) {
            super(_agent, _pollution) ;
            currentExposureAmount = 0.0 ;
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
            return currentExposureAmount ;
        }

    }

}
