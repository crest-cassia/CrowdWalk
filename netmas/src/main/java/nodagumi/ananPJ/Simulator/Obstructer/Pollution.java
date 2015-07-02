// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator.Obstructer;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.Agents.AgentBase;

import nodagumi.Itk.*;

/**
 * ガスによるエージェントへの影響.
 */
public class Pollution extends ObstructerBase {
    // 参考データ: 三井化学アグロ株式会社 / クロルピクリンの物性・分解性 / 気中濃度と人体への影響
    //   http://www.mitsui-agro.com/tabid/147/Default.aspx#04a4

    /**
     * トリアージレベル:Black の下限累積曝露量
     *
     * 致死
     */
    private double deadlyLevel = 297.6 * 10 * 60;          // 178,560

    /**
     * トリアージレベル:Red の下限累積曝露量
     *
     * 不耐
     */
    private double unbearableLevel = 15.0 * 1 * 60;        // 900

    /**
     * トリアージレベル:Yellow の下限累積曝露量
     *
     * 催涙により眼を開けていられない
     */
    private double incapacitatedLevel = 0.3 * 0.5 * 60;    // 9

    /**
     * 歩行速度に影響を与える最低累積曝露量
     *
     * 最低刺激
     */
    private double irritantLevel = 1.3;

    /**
     * 歩行不可能になる最低累積曝露量
     */
    private double nonAmbulatoryLevel = 9.0;    // = incapacitatedLevel

    /**
     * 現状の暴露量
     */
    private double currentExposureAmount = 0.0 ;

    /**
     * 累積暴露量
     */
    private double accumulatedExposureAmount = 0.0;

    /**
     * Pollution 用の fallback パラメータ
     */
    private static Term config = null;

    /**
     * fallback パラメータからの値の取得(double)
     */
    public static double getConfigParameter(String slot, double fallback) {
        return config.fetchArgDouble(slot, NetworkMap.FallbackSlot, fallback);
    }

    //--------------------------------------------------
    /**
     * コンストラクタ
     */
    public Pollution() {}

    /**
     * 初期化.
     */
    public void init(AgentBase agent) {
        this.agent = agent;
        if (config == null) {
            config = fallbackParameters.fetchArgTerm(getClass().getSimpleName(), NetworkMap.FallbackSlot,
                    Term.newArrayTerm());
        }
        deadlyLevel = getConfigParameter("deadlyLevel", deadlyLevel);
        unbearableLevel = getConfigParameter("unbearableLevel", unbearableLevel);
        incapacitatedLevel = getConfigParameter("incapacitatedLevel", incapacitatedLevel);
        irritantLevel = getConfigParameter("irritantLevel", irritantLevel);
        nonAmbulatoryLevel = getConfigParameter("nonAmbulatoryLevel", nonAmbulatoryLevel);
    }

    /**
     * エージェントを暴露させる
     */
    public void expose(double exposureAmount) {
        if (dead || agent.isEvacuated()) {
            return;
        }

        currentExposureAmount = exposureAmount;
        accumulatedExposureAmount += exposureAmount;
        currentTriageLevel = calcTriage();
        dead = calcDead();

        /* [2015.07.02 I.Noda]
         * Emergency は obsolete なので、使わない。
         * ここの部分は、なにか別の方策が必要。
         * 一番適切なのは、RationalAgent か RubyAgent で処理する。
         */
        /*
        if (currentTriageLevel != TriageLevel.GREEN) {
            agent.setGoal(SpecialTerm.Emergency) ;
        }
        */

        if (currentTriageLevel != lastTriageLevel) {
            agent.getNetworkMap().getNotifier().agentTriageChanged(agent);
            lastTriageLevel = currentTriageLevel;
        }
    }

    /**
     * 歩行速度に影響を与える
     *
     * ※呼び出し元で speed を更新した後に呼ばれる
     */
    public void effect() {
        if (accumulatedExposureAmount >= nonAmbulatoryLevel) {
            agent.setSpeed(0.0);
        } else if (accumulatedExposureAmount >= irritantLevel) {
            agent.setSpeed(agent.getSpeed() * (nonAmbulatoryLevel - accumulatedExposureAmount) / nonAmbulatoryLevel);
        }
    }

    /**
     * 現状の累積曝露量によるエージェントのトリアージレベルを求める
     */
    protected TriageLevel calcTriage() {
        if (accumulatedExposureAmount >= deadlyLevel)
            return TriageLevel.BLACK;
        else if (accumulatedExposureAmount >= unbearableLevel)
            return TriageLevel.RED;
        else if (accumulatedExposureAmount >= incapacitatedLevel)
            return TriageLevel.YELLOW;
        else
            return TriageLevel.GREEN;
    }

    /**
     * dumpResult 用の値。
     */
    public double currentValueForLog() {
        return currentExposureAmount ;
    }

    /**
     * dumpResult 用の値。
     */
    public double accumulatedValueForLog() {
        return accumulatedExposureAmount ;
    }
}
