// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;

/**
 * NetworkMap の構成要素の状態変化を監視するリスナ用インターフェイス.
 *
 * シミュレーション画面の描画更新用。<br>
 * 通知元のスレッドから直接呼ばれるため、時間の掛かる処理はしないこと。
 */
public interface NetworkMapPartsListener {
    /**
     * リンクが削除された.
     */
    public void linkRemoved(MapLink link);

    /**
     * リンクタグが追加された.
     */
    public void linkTagAdded(MapLink link, String tag);

    /**
     * リンクタグが削除された.
     */
    public void linkTagRemoved(MapLink link);

    /**
     * ノードタグが追加された.
     */
    public void nodeTagAdded(MapNode node, String tag);

    /**
     * ノードタグが削除された.
     */
    public void nodeTagRemoved(MapNode node);

    /**
     * Pollution レベルが変化した.
     */

    public void pollutionLevelChanged(MapArea area);

    /**
     * エージェントが追加された.
     */
    public void agentAdded(AgentBase agent);

    /**
     * エージェントが移動した(swing も含む).
     */
    public void agentMoved(AgentBase agent);

    /**
     * エージェントのスピードが変化した.
     */
    public void agentSpeedChanged(AgentBase agent);

    /**
     * エージェントのトリアージレベルが変化した.
     */
    public void agentTriageChanged(AgentBase agent);

    /**
     * エージェントの避難が完了した.
     */
    public void agentEvacuated(AgentBase agent);
}
