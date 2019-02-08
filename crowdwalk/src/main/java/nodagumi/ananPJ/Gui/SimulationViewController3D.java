package nodagumi.ananPJ.Gui;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javafx.application.Platform;
import javafx.geometry.Point3D;

import math.geom3d.Vector3D;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.NetworkMapPartsListener;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase.TriageLevel;
import nodagumi.Itk.*;

/**
 * 3D シミュレーション画面の更新を制御するコントローラ.
 */
public class SimulationViewController3D {
    private class ChangedMapLink {
        public MapLink link;
        public ArrayList<String> tags;

        public ChangedMapLink(MapLink link) {
            this.link = link;
            tags = (ArrayList<String>)link.getTags().clone();
        }
    }

    private class ChangedMapNode {
        public MapNode node;
        public ArrayList<String> tags;

        public ChangedMapNode(MapNode node) {
            this.node = node;
            tags = (ArrayList<String>)node.getTags().clone();
        }
    }

    private class ChangedMapArea {
        public MapArea area;
        public double currentLevel;
        public double normalizedLevel;

        public ChangedMapArea(MapArea area) {
            this.area = area;
            currentLevel = area.getPollutionLevel().getCurrentLevel();
            normalizedLevel = area.getPollutionLevel().getNormalizedLevel();
        }
    }

    private class AddedAgent {
        public AgentBase agent;
        public Point3D position;
        public TriageLevel triage;
        public double speed;

        public AddedAgent(AgentBase agent) {
            this.agent = agent;
            Vector3D swing = agent.getSwing();
            Point2D pos = agent.getPosition();
            position = new Point3D(pos.getX() + swing.getX(), pos.getY() + swing.getY(), agent.getHeight());
            triage = agent.getTriage();
            speed = agent.getSpeed();
        }
    }

    private class MovedAgent {
        public AgentBase agent;
        public Point3D position;

        public MovedAgent(AgentBase agent) {
            this.agent = agent;
            Vector3D swing = agent.getSwing();
            Point2D pos = agent.getPosition();
            position = new Point3D(pos.getX() + swing.getX(), pos.getY() + swing.getY(), agent.getHeight());
        }
    }

    private class ColorChangedAgent {
        public AgentBase agent;
        public TriageLevel triage;
        public double speed;

        public ColorChangedAgent(AgentBase agent) {
            this.agent = agent;
            triage = agent.getTriage();
            speed = agent.getSpeed();
        }
    }

    /**
     * シミュレーションウィンドウ
     */
    private SimulationFrame3D frame;

    /**
     * シミュレーションパネル
     */
    private SimulationPanel3D panel;

    /**
     * 描画中かどうか
     */
    private boolean active = false;

    /**
     * エージェントイベント処理の保留中かどうか
     */
    private boolean suspended = false;

    /**
     * イベントキュー
     */

    private HashSet<MapLink> removedLinks = new HashSet();
    private HashMap<MapLink, ChangedMapLink> changedLinks = new HashMap();
    private HashMap<MapNode, ChangedMapNode> changedNodes = new HashMap();
    private HashMap<MapArea, ChangedMapArea> changedMapAreas = new HashMap();
    private HashMap<AgentBase, AddedAgent> addedAgents = new HashMap();
    private HashMap<AgentBase, MovedAgent> movedAgents = new HashMap();
    private HashMap<AgentBase, ColorChangedAgent> colorChangedAgents = new HashMap();
    private HashSet<AgentBase> evacuatedAgents = new HashSet();
    private HashMap<String, Object> changedStatuses = new HashMap();

    /**
     * コンストラクタ
     */
    public SimulationViewController3D(SimulationFrame3D frame) {
        this.frame = frame;
        panel = frame.getSimulationPanel();
    }

    /**
     * NetworkMap に構成要素の状態変化を監視するリスナを登録する.
     */
    public void addNetworkMapPartsListener(NetworkMap networkMap) {
        networkMap.getNotifier().addListener(new NetworkMapPartsListener() {
            /**
             * リンクが削除された.
             */
            public void linkRemoved(MapLink link) {
                Itk.logInfo("Link Removed");
                synchronized (removedLinks) {
                    removedLinks.add(link);
                }
                updatePanelViewLater();
            }

            /**
             * リンクタグが追加された.
             */
            public void linkTagAdded(MapLink link, String tag) {
                Itk.logInfo("Link Tag Added", tag);
                synchronized (changedLinks) {
                    changedLinks.put(link, new ChangedMapLink(link));
                }
                updatePanelViewLater();
            }

            /**
             * リンクタグが削除された.
             */
            public void linkTagRemoved(MapLink link) {
                Itk.logInfo("Link Tag Removed");
                synchronized (changedLinks) {
                    changedLinks.put(link, new ChangedMapLink(link));
                }
                updatePanelViewLater();
            }

            /**
             * ノードタグが追加された.
             */
            public void nodeTagAdded(MapNode node, String tag) {
                Itk.logInfo("Node Tag Added", tag);
                synchronized (changedNodes) {
                    changedNodes.put(node, new ChangedMapNode(node));
                }
                updatePanelViewLater();
            }

            /**
             * ノードタグが削除された.
             */
            public void nodeTagRemoved(MapNode node) {
                Itk.logInfo("Node Tag Removed");
                synchronized (changedNodes) {
                    changedNodes.put(node, new ChangedMapNode(node));
                }
                updatePanelViewLater();
            }

            /**
             * Pollution レベルが変化した.
             */
            public void pollutionLevelChanged(MapArea area) {
                synchronized (changedMapAreas) {
                    changedMapAreas.put(area, new ChangedMapArea(area));
                }
                updatePanelViewLater();
            }

            /**
             * エージェントが追加された.
             */
            public void agentAdded(AgentBase agent) {
                synchronized (addedAgents) {
                    addedAgents.put(agent, new AddedAgent(agent));
                }
                if (! suspended) {
                    updatePanelViewLater();
                }
            }

            /**
             * エージェントが移動した(swing も含む).
             */
            public void agentMoved(AgentBase agent) {
                synchronized (movedAgents) {
                    movedAgents.put(agent, new MovedAgent(agent));
                }
                if (! suspended) {
                    updatePanelViewLater();
                }
            }

            /**
             * エージェントのスピードが変化した.
             */
            public void agentSpeedChanged(AgentBase agent) {
                synchronized (colorChangedAgents) {
                    colorChangedAgents.put(agent, new ColorChangedAgent(agent));
                }
                if (! suspended) {
                    updatePanelViewLater();
                }
            }

            /**
             * エージェントのトリアージレベルが変化した.
             */
            public void agentTriageChanged(AgentBase agent) {
                synchronized (colorChangedAgents) {
                    colorChangedAgents.put(agent, new ColorChangedAgent(agent));
                }
                if (! suspended) {
                    updatePanelViewLater();
                }
            }

            /**
             * エージェントの避難が完了した.
             */
            public void agentEvacuated(AgentBase agent) {
                synchronized (evacuatedAgents) {
                    evacuatedAgents.add(agent);
                }
                if (! suspended) {
                    updatePanelViewLater();
                }
            }
        });
    }

    /**
     * ステータスを更新待ちにする
     */
    public void statusChanged(String name, Object object) {
        synchronized (changedStatuses) {
            changedStatuses.put(name, object);
        }
    }

    /**
     * シミュレーション画面を最新の状態に更新する
     */
    public synchronized void updatePanelViewLater() {
        if (! active) {
            active = true;
            Platform.runLater(() -> updatePanelView());
        }
    }

    /**
     * シミュレーション画面を最新の状態に更新する.
     *
     * ※JavaFX アプリケーションスレッドから呼び出すこと。
     */
    public void updatePanelView() {
        synchronized (this) {
            active = true;
        }

        HashSet<MapLink> _removedLinks = new HashSet();
        HashMap<MapLink, ChangedMapLink> _changedLinks = new HashMap();
        HashMap<MapNode, ChangedMapNode> _changedNodes = new HashMap();
        HashMap<MapArea, ChangedMapArea> _changedMapAreas = new HashMap();
        HashMap<AgentBase, AddedAgent> _addedAgents = new HashMap();
        HashMap<AgentBase, MovedAgent> _movedAgents = new HashMap();
        HashMap<AgentBase, ColorChangedAgent> _colorChangedAgents = new HashMap();
        HashSet<AgentBase> _evacuatedAgents = new HashSet();
        HashMap<String, Object> _changedStatuses = new HashMap();

        // キューが全て空になるまで溜まったイベントの処理を続ける
        // TODO: 処理が100ms以上続いたら一旦抜けて updatePanelViewLater() する
        while (true) {
            boolean eventAvailable = false;
            synchronized (removedLinks) {
                if (! removedLinks.isEmpty()) {
                    _removedLinks.addAll(removedLinks);
                    removedLinks.clear();
                    eventAvailable = true;
                }
            }
            synchronized (changedLinks) {
                if (! changedLinks.isEmpty()) {
                    _changedLinks.putAll(changedLinks);
                    changedLinks.clear();
                    eventAvailable = true;
                }
            }
            synchronized (changedNodes) {
                if (! changedNodes.isEmpty()) {
                    _changedNodes.putAll(changedNodes);
                    changedNodes.clear();
                    eventAvailable = true;
                }
            }
            synchronized (changedMapAreas) {
                if (! changedMapAreas.isEmpty()) {
                    _changedMapAreas.putAll(changedMapAreas);
                    changedMapAreas.clear();
                    eventAvailable = true;
                }
            }
            if (! suspended) {
                synchronized (addedAgents) {
                    if (! addedAgents.isEmpty()) {
                        _addedAgents.putAll(addedAgents);
                        addedAgents.clear();
                        eventAvailable = true;
                    }
                }
                synchronized (evacuatedAgents) {
                    if (! evacuatedAgents.isEmpty()) {
                        _evacuatedAgents.addAll(evacuatedAgents);
                        evacuatedAgents.clear();
                        eventAvailable = true;
                    }
                }
                synchronized (movedAgents) {
                    if (! movedAgents.isEmpty()) {
                        _movedAgents.putAll(movedAgents);
                        movedAgents.clear();
                        eventAvailable = true;
                    }
                }
                synchronized (colorChangedAgents) {
                    if (! colorChangedAgents.isEmpty()) {
                        _colorChangedAgents.putAll(colorChangedAgents);
                        colorChangedAgents.clear();
                        eventAvailable = true;
                    }
                }
            }
            synchronized (changedStatuses) {
                if (! changedStatuses.isEmpty()) {
                    _changedStatuses.putAll(changedStatuses);
                    changedStatuses.clear();
                    eventAvailable = true;
                }
            }
            if (! eventAvailable) {
                synchronized (this) {
                    active = false;
                }
                return;
            }

            // リンクをシーングラフから削除する
            if (! _removedLinks.isEmpty()) {
                for (MapLink link : _removedLinks) {
                    panel.removeLink(link);
                }
                _removedLinks.clear();
            }

            // タグが更新されたリンクを再表示する
            if (! _changedLinks.isEmpty()) {
                for (ChangedMapLink changedLink : _changedLinks.values()) {
                    if (panel.removeLink(changedLink.link)) {   // 削除済みのリンクは再表示しない
                        panel.addPickingLink(changedLink.link);
                        panel.addLink(changedLink.link, changedLink.tags);
                    }
                }
                _changedLinks.clear();
            }

            // タグが更新されたノードを再表示する
            if (! _changedNodes.isEmpty()) {
                for (ChangedMapNode changedNode : _changedNodes.values()) {
                    panel.removeNode(changedNode.node);
                    panel.addNode(changedNode.node);
                }
                _changedNodes.clear();
            }

            // Pollution レベルが変化した MapArea を再表示する.
            if (! _changedMapAreas.isEmpty()) {
                for (ChangedMapArea changedArea : _changedMapAreas.values()) {
                    panel.changeAreaLevel(changedArea.area, changedArea.currentLevel, changedArea.normalizedLevel);
                }
                _changedMapAreas.clear();
            }

            // エージェントをシーングラフに追加する
            if (! _addedAgents.isEmpty()) {
                double size = frame.getAgentSize();
                for (AddedAgent addedAgent : _addedAgents.values()) {
                    panel.addAgent(addedAgent.agent, size, addedAgent.position, addedAgent.triage, addedAgent.speed);
                }
                _addedAgents.clear();
            }

            // エージェントをシーングラフから削除する
            if (! _evacuatedAgents.isEmpty()) {
                for (AgentBase agent : _evacuatedAgents) {
                    panel.removeAgent(agent);
                }
                _evacuatedAgents.clear();
            }

            // エージェントを移動する
            if (! _movedAgents.isEmpty()) {
                double size = frame.getAgentSize();
                for (MovedAgent movedAgent : _movedAgents.values()) {
                    panel.moveAgent(movedAgent.agent, size, movedAgent.position);
                }
                _movedAgents.clear();
            }

            // エージェントの表示色を変更する
            if (! _colorChangedAgents.isEmpty()) {
                for (ColorChangedAgent colorChangedAgent : _colorChangedAgents.values()) {
                    panel.changeAgentColor(colorChangedAgent.agent, colorChangedAgent.triage, colorChangedAgent.speed);
                }
                _colorChangedAgents.clear();
            }

            // ステータス表示を更新する
            if (! _changedStatuses.isEmpty()) {
                String text = (String)_changedStatuses.get("statusText");
                if (text != null) {
                    frame.setStatusText(text);
                    panel.setStatusText(text);
                }
                SimTime currentTime = (SimTime)_changedStatuses.get("displayClock");
                if (currentTime != null) {
                    frame.displayClock(currentTime);
                }
                text = (String)_changedStatuses.get("evacuatedCount");
                if (text != null) {
                    frame.updateEvacuatedCount(text);
                }
                _changedStatuses.clear();
            }
        }
    }

    /**
     * エージェントイベント処理が保留中か?
     */
    public synchronized boolean isSuspended() {
        return suspended;
    }

    /**
     * エージェントイベント処理を保留する
     */
    public synchronized void suspend() {
        suspended = true;
    }

    /**
     * 保留中のエージェントイベント処理を再開する
     */
    public synchronized void resume() {
        suspended = false;
        updatePanelViewLater();
    }
}
