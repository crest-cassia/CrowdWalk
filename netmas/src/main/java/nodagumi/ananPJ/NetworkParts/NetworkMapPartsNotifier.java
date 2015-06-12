// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts;

import java.util.ArrayList;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Area.MapArea;

/**
 * NetworkMap の構成要素の状態変化を通知する.
 *
 * イベントは使用せず、通知用のメソッドから直接リスナのメソッドを呼び出している。
 */
public class NetworkMapPartsNotifier {
    private NetworkMap map;
    private ArrayList<NetworkMapPartsListener> listeners = new ArrayList();

    public NetworkMapPartsNotifier(NetworkMap map) {
        this.map = map;
    }

    public NetworkMap getNetworkMap() {
        return map;
    }

    /**
     * リスナを登録する.
     */
    public synchronized void addListener(NetworkMapPartsListener listener) {
        listeners.add(listener);
    }

    /**
     * リンクが削除された事を通知する.
     */
    public void linkRemoved(MapLink link) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.linkRemoved(link);
        }
    }

    /**
     * リンクタグが追加された事を通知する.
     */
    public void linkTagAdded(MapLink link, String tag) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.linkTagAdded(link, tag);
        }
    }

    /**
     * リンクタグが削除された事を通知する.
     */
    public void linkTagRemoved(MapLink link) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.linkTagRemoved(link);
        }
    }

    /**
     * ノードタグが追加された事を通知する.
     */
    public void nodeTagAdded(MapNode node, String tag) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.nodeTagAdded(node, tag);
        }
    }

    /**
     * ノードタグが削除された事を通知する.
     */
    public void nodeTagRemoved(MapNode node) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.nodeTagRemoved(node);
        }
    }

    /**
     * Pollution レベルが変化した事を通知する.
     */
    public void pollutionLevelChanged(MapArea area) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.pollutionLevelChanged(area);
        }
    }

    /**
     * エージェントが追加された事を通知する.
     */
    public void agentAdded(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentAdded(agent);
        }
    }

    /**
     * エージェントが移動(swing も含む)した事を通知する.
     */
    public void agentMoved(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentMoved(agent);
        }
    }

    /**
     * エージェントのスピードが変化した事を通知する.
     */
    public void agentSpeedChanged(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentSpeedChanged(agent);
        }
    }

    /**
     * エージェントのトリアージレベルが変化した事を通知する.
     */
    public void agentTriageChanged(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentTriageChanged(agent);
        }
    }

    /**
     * エージェントの避難が完了した事を通知する.
     */
    public void agentEvacuated(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentEvacuated(agent);
        }
    }
}
