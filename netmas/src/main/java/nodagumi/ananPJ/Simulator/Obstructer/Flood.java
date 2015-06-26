// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator.Obstructer;

import nodagumi.ananPJ.Agents.AgentBase;

/**
 * 洪水によるエージェントへの影響.
 *
 * TODO: ファイルからパラメータを読み込む機能
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

    //--------------------------------------------------
    /**
     * コンストラクタ
     */
    public Flood() {}

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
            agent.getNetworkMap().getNotifier().agentTriageChanged(agent);
            lastTriageLevel = currentTriageLevel;
        }
    }

    /* effect of flood, this damage does not increase */
    // 呼び出し元で speed を更新した後に呼ばれる
    public void effect() {
        if (dead) {
            agent.setSpeed(0.0);
            return;
        }

        // 歩行速度への影響
        // ※スピードがマイナスになるのを避ける
        agent.setSpeed(agent.getSpeed() *
                (nonAmbulatoryDepth - Math.min(currentDepth, nonAmbulatoryDepth))
                / nonAmbulatoryDepth);
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
