package nodagumi.ananPJ.Simulator.pollution;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Simulator.Pollution;

// 非蓄積型(洪水等)
public class NonAccumulatedPollution extends Pollution {
    private int lastTriageLevel = 0;    // 更新チェック用

    public NonAccumulatedPollution() {}

    public void expose(AgentBase agent, double pollutionLevel) {
        if (! agent.isEvacuated()) {
            agent.currentExposureAmount = pollutionLevel;
            agent.accumulatedExposureAmount = pollutionLevel;
        }

        // if (getTriage(agent) != 0) {
        //     agent.setGoal("EMERGENCY");
        // }

        int triageLevel = getTriage(agent);
        if (triageLevel != lastTriageLevel) {
            agent.getNetworkMap().getNotifier().agentTriageChanged(agent);
            lastTriageLevel = triageLevel;
        }
    }

    /* effect of flood, this damage does not increase */
    // 呼び出し元で speed を更新した後に呼ばれる
    public void effect(AgentBase agent) {
        if (agent.currentExposureAmount > 10.0) {
            agent.currentExposureAmount = 10.0;
        }
        // 水深1.2mで歩行不可能
        // ※スピードがマイナスになるのを避ける
        agent.setSpeed(agent.getSpeed() * (1.2 - Math.min(agent.currentExposureAmount, 1.2)) / 1.2);
    }

    /* the state of the agent */
    public int getTriage(AgentBase agent) {
        if (agent.accumulatedExposureAmount >= 1.5)
            return 3;
        else if (agent.accumulatedExposureAmount >= 0.8)
            return 2;
        else if (agent.accumulatedExposureAmount >= 0.3)
            return 1;
        else
            return 0;
    }

    public boolean finished(AgentBase agent) {
        return agent.isEvacuated() || getTriage(agent) >= 3;
    }
}
