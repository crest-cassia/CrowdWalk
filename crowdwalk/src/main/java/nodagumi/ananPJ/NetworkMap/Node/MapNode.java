// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap.Node;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBMapPart;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;
import nodagumi.Itk.RingBuffer.ExpandType;

public class MapNode extends OBMapPart implements Comparable<MapNode> {
    //============================================================
    //------------------------------------------------------------
    /**
     * 共通パラメータを、fallbackParameterから設定。
     */
    public static void setupCommonParameters(Term wholeFallbacks) {
        fallbackParameters =
            SetupFileInfo.filterFallbackTerm(wholeFallbacks, "node") ;
        speedRestrictRule =
            SetupFileInfo.fetchFallbackTerm(fallbackParameters,
                                            "speedRestrictRule",
                                            Term.newArrayTerm()) ;
    } ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ノード用の fallback パラメータ
     */
    public static Term fallbackParameters = null ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクによる速度の制約計算で用いるルール。
     * fallback の "node"/"speedRestrictRule" に記述する。
     * Array 型 Term の形で格納される。
     * Array の要素は、ルールを示す ObjectTerm。
     * 以下は例。詳細は、
     * {@link nodagumi.ananPJ.NetworkMap.OBMapPart#applyRestrictionRule applyRestrictionRule} 
     * を参照。
     * <pre>
     *  [ { "type" : "multiply",
     *      "tag" : "FOO",
     *      "factor" : 0.5 },
     *    { "type" : "multiply",
     *      "tag" : "BAR",
     *      "factor" : 0.9 } ]
     * </pre>
     */
    public static Term speedRestrictRule = null ;

    /* global coordinates */
    private Point2D position ;
    private double height;

    private MapLinkTable links;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ゴールタグ毎の最短経路情報。（物理的距離）
     */
    private HashMap<String, NavigationHint> physicalHints;

    /**
     * ゴールタグ毎の最短経路情報。（主観的距離）
     */
    private HashMap<String, HashMap<String, NavigationHint>>
        mentalHintsTable;

    public double getX() { return position.getX(); }
    public double getY() { return position.getY(); }

    public Point2D getPosition(){ return position; }

    public void setPosition(Point2D _position) {
        position = _position;
    }

    //------------------------------------------------------------
    // 相対位置情報
    /** 相対 X 座標 */
    public double getRelativeXFrom(Point2D origin) {
        return getX() - origin.getX() ;
    }
    /** 相対 Y 座標 */
    public double getRelativeYFrom(Point2D origin) {
        return getY() - origin.getY() ;
    }
    /** 相対 X 座標 from Node */
    public double getRelativeXFrom(MapNode origin) {
        return getX() - origin.getX() ;
    }
    /** 相対 Y 座標 from Node */
    public double getRelativeYFrom(MapNode origin) {
        return getY() - origin.getY() ;
    }
    /** 相対位置の角度 */
    public double getAngleFrom(MapNode origin) {
        double dX = getRelativeXFrom(origin) ;
        double dY = getRelativeYFrom(origin) ;
        
        if(dX == 0.0 && dY == 0.0) { /* dX = dY = 0.0 の場合のエラーを回避 */
            return 0.0 ;
        } else {
            return Math.atan2(dY, dX) ;
        }
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 避難完了（ゴール到達）エージェント情報
     */
    private int numberOfEvacuatedAgents = 0 ;
    
    //------------------------------------------------------------
    /**
     * コンストラクタ。
     */
    public MapNode(String _ID,
                   Point2D _position,
                   double _height) {
        super(_ID);

        setPosition(_position);
        setHeight(_height);

        selected = false;
        clearNavigationHintsAll() ;
        
        links = new MapLinkTable();
    }

    public double calcDistance(MapNode other) {
        return Math.sqrt((getX() - other.getX()) * (getX() - other.getX()) +
                (getY() - other.getY()) * (getY() - other.getY()) +
                (getHeight() - other.getHeight()) * (getHeight() - other.getHeight()))
                * ((MapPartGroup)getParent()).getScale();
    }

    public boolean addLink(MapLink link) {
        if (links.contains(link)) return false;
        links.add(link);
        clearUsableLinkTableCache() ;
        return true;
    }

    public boolean removeLink(MapLink link) {
        if (!links.contains(link)) return false;
        links.remove(link);
        clearUsableLinkTableCache() ;
        return true;
    }

    public MapLinkTable getLinks() {
        return links;
    }

    //------------------------------------------------------------
    /**
     * 一方通行を考慮して、つながっているリンクを集める。
     */
    public MapLinkTable getUsableLinkTable() {
        if(usableLinkTableCache == null) { // cacheがクリアされていれば作成。
            usableLinkTableCache = new MapLinkTable();
            for (MapLink link : links) {
                if (link.isAvailableFrom(this)) {
                    usableLinkTableCache.add(link);
                }
            }
        }
        return usableLinkTableCache ;
    }

    /**
     * getUsableLinkTable を効率化するための cache
     * Node とそれに繋がるリンクのタグなどが変化すれば、
     * reset されなければならない。
     * その際には、resetUsableLinkTable() を呼ぶこと。
     */
    private MapLinkTable usableLinkTableCache = null ;

    //------------------------------------------------------------
    /**
     * 一方通行を考慮して、つながっているリンクを集める。
     * Dijkstra で使われるだけなので、cache しない。
     */
    public MapLinkTable getValidReverseLinkTable () {
        MapLinkTable usableReverseLinkTable = new MapLinkTable();
        for (MapLink link : links) {
            if (!((link.isOneWayForward() && link.getFrom() == this) ||
                  (link.isOneWayBackward() && link.getTo() == this) ||
                  (link.isRoadClosed()))) {
                usableReverseLinkTable.add(link);
            }
        }
        return usableReverseLinkTable ;
    }

    //------------------------------------------------------------
    /**
     * getUsableLinkTable を効率化するための cache を開放する。
     */
    public void clearUsableLinkTableCache() {
        usableLinkTableCache = null ;
    }

    public MapLink connectedTo(MapNode node) {
        for (MapLink link : links) {
            if (link.getFrom() == node) {
                return link;
            } else if (link.getTo() == node) {
                return link;
            }
        }
        return null;
    }

    /**
     * すべての navigationHinst をくりあ。
     */
    public void clearNavigationHintsAll() {
        physicalHints = new HashMap<String, NavigationHint>();
        mentalHintsTable =
            new HashMap<String, HashMap<String, NavigationHint>>() ;
    }

    public void addNavigationHint(Term mentalMode,
                                  String goalTag,
                                  NavigationHint hint) {
        getHints(mentalMode).put(goalTag, hint);
    }
    
    public void clearHints(Term mentalMode) {
        getHints(mentalMode).clear();
    }

    //------------------------------------------------------------
    /**
     * ある mentalMode での goalTag への最短経路ヒントを持っているかチェック。
     */
    public final boolean hasHint(Term mentalMode, Term goalTag) {
        return hasHint(mentalMode, goalTag.getString()) ;
    }
    
    /**
     * ある mentalMode での goalTag への最短経路ヒントを持っているかチェック。
     */
    public final boolean hasHint(Term mentalMode, String goalTag) {
        return getHint(mentalMode, goalTag, false) != null ;
    }
    
    //------------------------------------------------------------
    /**
     * ある mentalMode での goalTag への最短経路ヒントを取得。
     */
    public NavigationHint getHint(Term mentalMode, Term goalTag,
                                  boolean causeErrorP) {
        return getHint(mentalMode, goalTag.getString(), causeErrorP) ;
    }

    /**
     * ある mentalMode での goalTag への最短経路ヒントを取得。
     */
    public NavigationHint getHint(Term mentalMode, String goalTag,
                                  boolean causeErrorP) {
        NavigationHint hint = getHints(mentalMode).get(goalTag);
        if (hint == null) {
            if(causeErrorP) {
                Itk.logFatal("No hint for goal",
                             "toward tag", goalTag,
                             "from node", this.getID(),
                             "in mode", mentalMode, ".") ;
                Itk.dumpStackTrace() ;
                Itk.quitByError() ;
            } else if (! CrowdWalkPropertiesHandler.isDisableNoHintForGoalLog()) {
                Itk.logWarn("No hint for goal",
                            "toward tag", goalTag,
                            "from node", this.getID(),
                            "in mode", mentalMode, ".") ;
            }
        }
        return hint;
    }

    public HashMap<String, NavigationHint> getHints(Term mentalMode) {
        if(mentalMode == null) {
            return getHints((String)null) ;
        } else {
            return getHints(mentalMode.getString()) ;
        }
    }

    public HashMap<String, NavigationHint> getHints(String mentalMode) {
        if(mentalMode == null) {
            return physicalHints ;
        } else {
            HashMap<String, NavigationHint> hints =
                mentalHintsTable.get(mentalMode) ;
            if(hints == null) {
                hints = new HashMap<String, NavigationHint>() ;
                mentalHintsTable.put(mentalMode, hints) ;
            }
            return hints;
        }
    }

    public MapLink getViaLink(Term mentalMode, String goalTag) {
        NavigationHint hint = getHint(mentalMode, goalTag, true);
        return hint.viaLink;
    }

    public double getDistance(Term mentalMode, Term target)
        throws TargetNotFoundException {
        String goalTag = target.getString() ;
        if(hasTag(goalTag)) {// 自分自身がターゲットの場合
            return 0.0 ;
        } else {
            // Hint なしでエラーにならないように。[2022.0809 by I.Noda]
            // NavigationHint hint = getHint(mentalMode, goalTag, true);
            NavigationHint hint = getHint(mentalMode, goalTag, false);
            if (hint == null) { // おそらくここには来ないはず。getHintでエラー。
                Itk.logWarn("Target Not Found", "target:", goalTag) ;
                // Hint なしでエラーにならないように。[2022.0809 by I.Noda]
                // throw new TargetNotFoundException(goalTag + " not found for id=" + ID + "(" + getTagString() + ")");
                return Double.MAX_VALUE ;
            } else {
                return hint.distance;
            }
        }
    }

    /**
     * タグをクリア。
     */
    @Override
    public void allTagsClear() {
        boolean tagsIsEmpty = tags.isEmpty();
        super.allTagsClear() ;
        if (! tagsIsEmpty && map != null) {
            map.getNotifier().nodeTagRemoved(this);
        }
    }

    /**
     * タグを追加。
     */
    @Override
    public boolean addTag(String _tag) {
        boolean result = super.addTag(_tag) ;
        if (result && map != null) {
            map.getNotifier().nodeTagAdded(this, _tag);
        }
        return result ;
    }

    /**
     * タグを削除。
     */
    @Override
    public void removeTag(String _tag) {
        int tagsSize = tags.size();
        super.removeTag(_tag) ;
        if (map != null && tags.size() < tagsSize) {
            map.getNotifier().nodeTagRemoved(this);
        }
    }

    public boolean isBetweenHeight(double minHeight, double maxHeight) {
        if (getHeight() < minHeight || getHeight() > maxHeight) return false;
        return true;
    }

    @Override
    public NType getNodeType() {
        return NType.NODE;
    }

    public static String getNodeTypeString() {
        return "Node";
    }

    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());

        element.setAttribute("id", ID);
        element.setAttribute("x", "" + position.getX());
        element.setAttribute("y", "" + position.getY());
        element.setAttribute("height", "" + getHeight());
        for (MapLink link : links) {
            Element link_element = dom.createElement("link");
            link_element.setAttribute("id", link.ID);
            element.appendChild(link_element);
        }

        return element;
    }

    public static MapNode fromDom(Element element) {
        String id = element.getAttribute("id");
        double x = Double.parseDouble(element.getAttribute("x"));
        double y = Double.parseDouble(element.getAttribute("y"));
        Point2D coordinates = new Point2D.Double(x, y);
        double height = Double.parseDouble(element.getAttribute("height"));
        MapNode node = new MapNode(id, coordinates, height);
        node.getAttributesFromDom(element);

        /* used in NetworkMap.setupNetwork */
        ArrayList<String> links = new ArrayList<String>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            if (children.item(i) instanceof Element) {
                Element child = (Element) children.item(i);
                if (child.getTagName().equals("link")) {
                    links.add(child.getAttribute("id"));
                }
            }
        }
        node.setUserObject(links);
        return node; 
    }

    @Override
    public String toString() {
        return getTagString();
    }

    /**
     * このオブジェクトの状態をテキストで取得する
     */
    public String getStatusText() {
        StringBuilder buff = new StringBuilder();
        buff.append("Node ID: ").append(this.ID).append("\n");
        buff.append("x: ").append(this.getX()).append("\n");
        buff.append("y: ").append(this.getY()).append("\n");
        buff.append("height: ").append(this.getHeight()).append("\n");
        buff.append("tags: ").append(this.getTagString()).append("\n");
        HashMap<String, NavigationHint> hints
            = this.getHints(NavigationHint.DefaultMentalMode) ;
        if (! hints.isEmpty()) {
            buff.append("---- Navigation hints ----\n");
            ArrayList<String> hintKeys = new ArrayList(hints.keySet());
            Collections.sort(hintKeys);
            for (String key : hintKeys) {
                NavigationHint hint = hints.get(key);
                buff.append("key: ").append(key).append("\n");
                if (hint.toNode == null) {
                    buff.append("    toNode: null\n");
                } else {
                    buff.append("    toNode: ").append(hint.toNode.ID).append("(").append(hint.toNode.getTagString()).append(")\n");
                }
                if (hint.viaLink == null) {
                    buff.append("    viaLink: null\n");
                } else {
                    buff.append("    viaLink: ").append(hint.viaLink.ID).append("\n");
                }
                buff.append("    distance: ").append(hint.distance).append("\n");
            }
        }
        return buff.toString();
    }

    //------------------------------------------------------------
    /**
     * ノードの情報を短く示す文字列。
     * @return 文字列
     */
    public String toShortInfo() {
        return ("Node[" + ID
                + ",tag:" + getTags()
                + "]" ) ;
    }

    @Override
    public String getHintString() {
        //return "" + getX() + ", " + getY() + ", " + getHeight();
        return getTagString();
    }
    public void setHeight(double height) {
        this.height = height;
    }
    public double getHeight() {
        return height;
    }

    /*
     * タグ一覧を返す
     *   EXITのみ           EXIT
     *   EXIT + その他      その他
     *   EXIT + その他複数  その他1|その他2|...
     */
    public String getTagLabel() {
        StringBuffer buff = new StringBuffer();
        for (final String tag : tags) {
            if (buff.length() > 0) {
                buff.append("|");
            }
            buff.append(tag);
        }
        return buff.toString();
    }

    // p は this 座標を基準とした第何象限にあるのか
    public int getQuadrant(Point2D p) {
        if (p.getY() >= getY()) {
            return p.getX() >= getX() ? 1 : 2;
        } else {
            return p.getX() >= getX() ? 4 : 3;
        }
    }

    // node 座標が this 座標を基準とした quadrant 象限にあれば true を返す
    public boolean include(MapNode node, int quadrant) {
        switch (quadrant) {
        case 1:
            return node.getX() >= getX() && node.getY() >= getY();
        case 2:
            return node.getX() < getX() && node.getY() >= getY();
        case 4:
            return node.getX() >= getX() && node.getY() < getY();
        case 3:
            return node.getX() < getX() && node.getY() < getY();
        }
        return false;   // Exception の代わり
    }

    //------------------------------------------------------------
    /**
     * 交通規制処理
     * @param speed: 速度。
     * @param agent: 規制を加えるエージェント。この単位時間にノードにたどり着く。
     * @param currentTime: 現在時刻
     * @return 規制が適用されたら true
     */
    public double calcRestrictedSpeed(double speed, AgentBase agent,
                                      SimTime currentTime) {
        /* 分断制御 */
        if(isGateClosed(agent, currentTime)) {
            speed = 0.0 ;
        }

        /* 制約ルール適用 */
        for(int i = 0 ; i < speedRestrictRule.getArraySize() ; i++) {
            Term rule = speedRestrictRule.getNthTerm(i) ;
            speed = applyRestrictionRule(speed, rule, agent, currentTime) ;
        }

        return speed ;
    }

    //------------------------------------------------------------
    /**
     * リンクの比較演算子。
     * Hash や BinaryTree、sort 用。(Comparable にしたため)
     */
    public int compareTo(MapNode node) {
        return ID.compareTo(node.ID);
    }

    //============================================================
    //============================================================
    static public class PassingAgentRecord {
        /** time */
        public SimTime time ;
        /** agent */
        public AgentBase agent ;
        /** from Link */
        public MapLink fromLink ;
        /** to Link */
        public MapLink toLink ;
        /** node */
        public MapNode atNode ;

        //----------------------------------------
        /** constructor. */
        public PassingAgentRecord() {
            set(null, null, null, null, null) ;
        }
        //----------------------------------------
        /** set. */
        public PassingAgentRecord set(SimTime _time, AgentBase _agent,
                                      MapLink _fromLink, MapLink _toLink,
                                      MapNode _atNode) {
            time = _time ;
            agent = _agent ;
            fromLink = _fromLink ;
            toLink = _toLink ;
            atNode = _atNode ;
            return this ;
        }
        //----------------------------------------
        /** 
         * 交差チェック。
         * agent A and B.
         * A は A.from から A.to へ。
         * B は B.from から B.to へ。
         * これらが交差している場合、
         * A.from からカウントした 各々のリンクの index が、
         * {@code B.from <> @code A.to <> B.to} の順になっていると、交差している。
         */
        public boolean isCrossing(MapLink _fromLink, MapLink _toLink) {
            int n = atNode.getLinks().size() ;
            int origin = _fromLink.getIndexAtNode(atNode) ;
            int destIndex = (_toLink.getIndexAtNode(atNode) + n - origin) % n ;
            int fromIndex = (fromLink.getIndexAtNode(atNode) + n - origin) % n ;
            int toIndex = (toLink.getIndexAtNode(atNode) + n - origin) % n ;
            return ((fromIndex - destIndex) * (destIndex - toIndex) > 0) ;
        }
        //----------------------------------------
        /** 文字列化. */
        public String toString() {
            return ("#PassingAgentRecord" +
                    "[time=" + time +
                    ",agent=" + agent +
                    ",from=" + fromLink +
                    ",to=" + toLink +
                    ",at=" + atNode +
                    "]") ;
        }
    }
        
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** ノードを交差したエージェントの記録バッファ */
    private RingBuffer<PassingAgentRecord> passingAgentRecordBuffer ;
    
    /** バッファへのアクセス */
    public RingBuffer<PassingAgentRecord> getPassingAgentRecordBuffer() {
        return passingAgentRecordBuffer ;
    }
    
    //------------------------------------------------------------
    /**
     * エージェントが通過した時刻の記録バッファの確保。
     */
    private void allocatePassingAgentRecordBuffer() {
        /* すべてのリンクの幅の和を求める */
        double wSum = 0.0 ;
        for(MapLink link : links) {
            double w = link.getWidth() ;
            wSum += (w / MapLink.dWidth) ;
            if(w < MapLink.dWidth) {
                /* 非常に細い道の補正 */
                wSum += 2.0 ;
            } else if(w < 2 * MapLink.dWidth) {
                /* 少し細い道の補正 */
                wSum += 1.0 ;
            }
        }
        /* リングバッファの大きさは、その倍+1としておく。*/
        int size = ((int)Math.ceil(wSum)) * 2 + 1 ;
        passingAgentRecordBuffer =
            new RingBuffer<PassingAgentRecord>(size, ExpandType.Recycle, true) ;
        passingAgentRecordBuffer.fillElements(PassingAgentRecord.class);
    }
        
    //------------------------------------------------------------
    /**
     * エージェントが通過した時刻の記録。
     */
    public void recordPassingAgent(SimTime currentTime,
                                   AgentBase agent,
                                   MapLink fromLink,
                                   MapLink toLink) {
        if(passingAgentRecordBuffer == null) {
            allocatePassingAgentRecordBuffer() ;
        }
        /* 記録 */
        passingAgentRecordBuffer.shiftHead().set(currentTime, agent,
                                                 fromLink, toLink, this) ;
    }

    //------------------------------------------------------------
    /**
     * 交差点の混雑度。
     * @param currentTime: 基準となる時刻。
     * @param margin: カウントする時間幅。秒数。
     * @return 時間幅内の通過エージェント数。
     */
    public int countPassingAgent(SimTime currentTime, double margin) {
        if(passingAgentRecordBuffer == null) {
            return 0 ;
        } else {
            int count = 0 ;
            for(PassingAgentRecord record : passingAgentRecordBuffer) {
                if(currentTime.calcDifferenceFrom(record.time) < margin) {
                    count += 1 ;
                } else {
                    break ;
                }
            }
            return count ;
        }
    }

    //------------------------------------------------------------
    /**
     * links の整列。
     * ノードを原点そして、各リンクの反対側のノード位置の角度で整列する。
     * 同時に、各ノードにその順番での index を振る。
     */
    public void sortLinkTableByAngle() {
        // 整列。
        final MapNode pivot = this ;
        Collections.sort(links, new Comparator<MapLink>() {
                @Override
                public int compare(MapLink link0, MapLink link1) {
                    MapNode otherNode0 = link0.getOther(pivot) ;
                    MapNode otherNode1 = link1.getOther(pivot) ;
                    double angle0 = otherNode0.getAngleFrom(pivot) ;
                    double angle1 = otherNode1.getAngleFrom(pivot) ;
                    return Double.compare(angle0,angle1) ;
                }
            }) ;
        // インデックスを振る。
        int index = 0 ;
        for(MapLink link : links) {
            link.setIndexAtNode(this, index) ;
            index += 1;
        }
    }

    //------------------------------------------------------------
    /**
     * 避難完了（ゴール到達）エージェント処理。
     * 現状では、カウントアップしかしない。
     * @param agent : 避難完了したエージェント。
     * @param currentTime : 現在時刻。
     * @return 現在の避難完了数。
     */
    public int acceptEvacuatedAgent(AgentBase agent, SimTime currentTime) {
        numberOfEvacuatedAgents++ ;
        return numberOfEvacuatedAgents ;
    }

    //------------------------------------------------------------
    /**
     * 避難完了（ゴール到達）したエージェント数。
     * @return 現在の避難完了数。
     */
    public int getNumberOfEvacuatedAgents() {
        return numberOfEvacuatedAgents ;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
