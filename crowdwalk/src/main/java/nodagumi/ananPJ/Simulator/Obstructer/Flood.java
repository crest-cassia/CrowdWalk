// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator.Obstructer;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.misc.SetupFileInfo;

import nodagumi.Itk.*;

/**
 * 洪水によるエージェントへの影響.
 */
public class Flood extends ObstructerBase {
    // 引用元: 千葉県ホームページ / 浸水深のランク分け
    //   http://www.pref.chiba.lg.jp/bousaik/tsunamityosa/documents/gaiyo3.pdf

    /**
     * トリアージレベル:Black の下限浸水深(m)
     *
     * 道路歩行中の人は、ほぼ絶望である。(木など高いものに登るしかない)
     */
    private double waterDepthThreshold3 = 1.0;

    /**
     * トリアージレベル:Red の下限浸水深(m)
     *
     * 流速が遅く、路面の状況が良い場合には歩行が可能ではあるが、人的被害発生の可能性は非常に高い。
     */
    private double waterDepthThreshold2 = 0.8;

    /**
     * トリアージレベル:Yellow の下限浸水深(m)
     *
     * 歩くにはかなり困難であるが、大人であれば生命の危険性は少ない。
     * ただし、流速が速い場合や子供や路面に障害物がある場合には人的被害発生の可能性が高い。
     */
    private double waterDepthThreshold1 = 0.3;

    /**
     * 歩行不可能になる浸水深(m)
     */
    private double nonAmbulatoryDepth = 1.0;

    /**
     * 現状の浸水深
     */
    private double currentDepth = 0.0;

    /**
     * Flood 用の fallback パラメータ
     */
    private static Term config = null;

    /**
     * fallback パラメータからの値の取得(double)
     */
    public static double getConfigParameter(String slot, double fallback) {
        return SetupFileInfo.fetchFallbackDouble(config, slot, fallback);
    }

    //--------------------------------------------------
    /**
     * コンストラクタ
     */
    public Flood() {}

    /**
     * 初期化.
     */
    public void init(AgentBase agent) {
        this.agent = agent;
        if (config == null) {
            config =
                SetupFileInfo.filterFallbackTerm(fallbackParameters,
                                                 getClass().getSimpleName()) ;
        }
        waterDepthThreshold3 =
            getConfigParameter("waterDepthThreshold3", waterDepthThreshold3);
        waterDepthThreshold2 =
            getConfigParameter("waterDepthThreshold2", waterDepthThreshold2);
        waterDepthThreshold1 =
            getConfigParameter("waterDepthThreshold1", waterDepthThreshold1);
        nonAmbulatoryDepth =
            getConfigParameter("nonAmbulatoryDepth", nonAmbulatoryDepth);
    }

    /**
     * エージェントを冠水させる
     */
    public void expose(double depth) {
        if (dead || agent.isEvacuated()) {
            return;
        }

        currentDepth = depth;
        currentTriageLevel = calcTriage();
        dead = calcDead();

        if (currentTriageLevel != lastTriageLevel) {
            agent.getMap().getNotifier().agentTriageChanged(agent);
            lastTriageLevel = currentTriageLevel;
        }
    }

    /* effect of flood, this damage does not increase */
    // 呼び出し元でもとめた speed に影響を加える。
    public double calcAffectedSpeed(double originalSpeed) {
        if (dead) {
            return 0.0 ;
        }

        // 歩行速度への影響
        // ※スピードがマイナスになるのを避ける
        double speed =
            (originalSpeed *
             (nonAmbulatoryDepth - Math.min(currentDepth, nonAmbulatoryDepth))
             / nonAmbulatoryDepth);

        return speed ;
    }

    /**
     * 現状の浸水深によるエージェントのトリアージレベルを求める
     */
    protected TriageLevel calcTriage() {
        if (currentDepth >= waterDepthThreshold3)
            return TriageLevel.BLACK;
        else if (currentDepth >= waterDepthThreshold2)
            return TriageLevel.RED;
        else if (currentDepth >= waterDepthThreshold1)
            return TriageLevel.YELLOW;
        else
            return TriageLevel.GREEN;
    }

    /**
     * dumpResult 用の値。
     */
    public double currentValueForLog() {
        return currentDepth ;
    }

    /**
     * dumpResult 用の値。
     */
    public double accumulatedValueForLog() {
        return currentDepth ;
    }
}
