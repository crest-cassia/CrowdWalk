package nodagumi.ananPJ.Simulator.pollution;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Simulator.Pollution;
import nodagumi.ananPJ.misc.SpecialTerm;
import nodagumi.Itk.*;

// 蓄積型(ガス等)
public class AccumulatedPollution extends Pollution {
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

    public AccumulatedPollution() {}

    public void expose(EvacuationAgent agent, double pollutionLevel) {
        if (! agent.isEvacuated()) {
            agent.currentExposureAmount = pollutionLevel;
            agent.accumulatedExposureAmount += pollutionLevel;
        }

        if (getTriage(agent) != 0) { 
	    agent.setGoal(SpecialTerm.Emergency) ;
        }
    }

    // 呼び出し元で speed を更新した後に呼ばれる
    public void effect(EvacuationAgent agent) {
        if (agent.accumulatedExposureAmount >= INCAPACITATED_LEVEL) {
            agent.setSpeed(0.0);
        } else if (agent.accumulatedExposureAmount >= IRRITANT_LEVEL) {
            agent.setSpeed(agent.getSpeed() * (INCAPACITATED_LEVEL - agent.accumulatedExposureAmount) / INCAPACITATED_LEVEL);
        }
    }

    /* the state of the agent */
    public int getTriage(EvacuationAgent agent) {
        if (agent.accumulatedExposureAmount >= DEADLY_LEVEL)
            return 3;
        else if (agent.accumulatedExposureAmount >= UNBEARABLE_LEVEL)
            return 2;
        else if (agent.accumulatedExposureAmount >= INCAPACITATED_LEVEL)
            return 1;
        else
            return 0;
    }

    public boolean finished(EvacuationAgent agent) {
        return agent.isEvacuated() || getTriage(agent) >= 3;
    }
}
