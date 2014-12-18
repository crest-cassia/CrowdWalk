package nodagumi.ananPJ.Simulator.pollution;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Simulator.Pollution;

// 非蓄積型(洪水等)
public class NonAccumulatedPollution extends Pollution {
    public NonAccumulatedPollution() {}

    public void expose(EvacuationAgent agent, double pollutionLevel) {
        if (! agent.isEvacuated()) {
            agent.currentExposureAmount = pollutionLevel;
            agent.accumulatedExposureAmount = pollutionLevel;
        }

        // if (getTriage(agent) != 0) {
        //     agent.setGoal("EMERGENCY");
        // }
    }

    /* effect of flood, this damage does not increase */
    // 呼び出し元で speed を更新した後に呼ばれる
    public void effect(EvacuationAgent agent) {
        if (agent.currentExposureAmount > 10.0) {
            agent.currentExposureAmount = 10.0;
        }
        // 水深1.2mで歩行不可能
        // ※スピードがマイナスになるのを避ける
        agent.setSpeed(agent.getSpeed() * (1.2 - Math.min(agent.currentExposureAmount, 1.2)) / 1.2);
    }

    /* the state of the agent */
    public int getTriage(EvacuationAgent agent) {
        if (agent.accumulatedExposureAmount >= 1.5)
            return 3;
        else if (agent.accumulatedExposureAmount >= 0.8)
            return 2;
        else if (agent.accumulatedExposureAmount >= 0.3)
            return 1;
        else
            return 0;
    }

    public boolean finished(EvacuationAgent agent) {
        return agent.isEvacuated() || getTriage(agent) >= 3;
    }
}