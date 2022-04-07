// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap.Link;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBMapPart;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.navigation.NavigationHint;
import nodagumi.ananPJ.navigation.PathChooser;
import nodagumi.ananPJ.navigation.Dijkstra;

import nodagumi.Itk.*;


public class MapLink extends OBMapPart implements Comparable<MapLink> {

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 順・逆方向を示すもの
     */
    static public enum Direction {
        /** 順方向 */
        Forward(1.0),
        /** 逆方向 */
        Backward(-1.0),
        /** 未定義 */
        None(0.0);

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /** 方向の値 */
        private final double _value ;

        //------------------------------
        /** コンストラクタ */
        private Direction(final double value) {
            this._value = value ;
        }

        //------------------------------
        /** 値の取得 */
        public double value() {
            return _value ;
        }

        //------------------------------
        /** 逆方向 */
        public Direction opposite() {
            switch(this) {
            case Forward : return Direction.Backward ;
            case Backward : return Direction.Forward ;
            default : return Direction.None ;
            }

        }
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Special Tags
     */
    static public final String Tag_OneWayForward =
        Itk.intern("ONE-WAY-FORWARD") ; 
    static public final String Tag_OneWayBackward =
        Itk.intern("ONE-WAY-BACKWARD") ;
    static public final String Tag_RoadClosed =
        Itk.intern("ROAD-CLOSED") ;
    static public final String Tag_Stair =
        Itk.intern("STAIR") ;
    static public enum SpecialTagId { OneWayForward,
                                      OneWayBackward,
                                      RoadClosed,
                                      Stair
    } ;
    static public Lexicon tagLexicon = new Lexicon() ;
    static {
        tagLexicon.registerMulti(new Object[][]
            {{Tag_OneWayForward, SpecialTagId.OneWayForward},
             {Tag_OneWayBackward, SpecialTagId.OneWayBackward},
             {Tag_RoadClosed, SpecialTagId.RoadClosed},
             {Tag_Stair, SpecialTagId.Stair}
            }) ;
    } ;

    //============================================================
    //------------------------------------------------------------
    /**
     * 共通パラメータを、fallbackParameterから設定。
     */
    public static void setupCommonParameters(Term wholeFallbacks) {
        fallbackParameters =
            SetupFileInfo.filterFallbackTerm(wholeFallbacks,"link") ;
        speedRestrictRule =
            SetupFileInfo.fetchFallbackTerm(fallbackParameters,
                                            "speedRestrictRule",
                                            Term.newArrayTerm()) ;
        emptySpeedRestrictRule =
            SetupFileInfo.fetchFallbackTerm(fallbackParameters,
                                            "emptySpeedRestrictRule",
                                            Term.newArrayTerm()) ;
        laneShareDiffuser =
            SetupFileInfo.fetchFallbackDouble(fallbackParameters,
                                            "laneShareDiffuser",
                                            1.0) ;
    } ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンク用の fallback パラメータ
     */
    public static Term fallbackParameters = null ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクによる速度の制約計算で用いるルール。
     * fallback の "link"/"speedRestrictRule" に記述する。
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
     *      "directed" : true,
     *      "tag" : "BAR",
     *      "factor" : [0.9, 1.1] } ]
     * </pre>
     */
    public static Term speedRestrictRule = null ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクによる自由速度に制約を加える時のルール
     * fallback の "link"/"emptySpeedRestrictRule" に記述する。
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
     *      "directed" : true,
     *      "tag" : "BAR",
     *      "factor" : [0.9, 1.1] } ]
     * </pre>
     */
    public static Term emptySpeedRestrictRule = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 対交流とのレーンの共有度(エージェントの認識可能最大数)を決める変数
     * 最大数は(レーン内のエージェント数)の(入力した値の逆数)乗となる．
     * fallback の "link"/"laneShareDiffuser" に記述する。
     * Double 型 で格納される。
     */
    public static Double laneShareDiffuser = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクの長さ
     */
    protected double length;
    /**
     * リンクの幅員
     */
    protected double width;
    /**
     * 始点・終点。リンクの両端。
     */
    protected MapNode fromNode, toNode;

    /**
     * 両端でのリンクの index。
     * 時計回り(?)順にカウントするとする。
     */
    protected int indexAtFromNode, indexAtToNode ;
    
    /**
     * 主観的距離のテーブル。
     * ルールによって可変。
     */
    protected HashMap<String, Double> mentalLengthTable ;
    /**
     * まだ定義されていない主観的距離の値
     */
    final static double UndefinedMentalLength = Double.NEGATIVE_INFINITY ;

    /* place holder for values used in simulation */
    public ArrayList<AgentBase> agents;
    private boolean nodes_are_set = false;

    /**
     * ノードを通過したエージェント数をカウントするためのクラス
     */
    private class PassCounter {
        boolean enabled = false;
        long entryCount = 0;
        long exitCount = 0;
    }

    /**
     * ノードごとの通過エージェントカウンタ
     */
    private HashMap<MapNode, PassCounter> passCounters = null;

    //------------------------------------------------------------
    /**
     * alert message
     */

    public void addAlertMessage(Term message, SimTime currentTime,
                                boolean onoff) {
        if(onoff) {
            alertMessageTable.put(message, currentTime) ;
            for(AgentBase agent : getAgents()) {
                agent.alertMessage(message, currentTime) ;
            }
        } else {
            alertMessageTable.remove(message) ;
        }
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント生成禁止かどうかのフラグ
     */
    private boolean shutOffP = false;

    //------------------------------------------------------------
    /**
     * 生成禁止フラグの操作
     */
    public void letShutOff(boolean onoff) { shutOffP = onoff; }

    //------------------------------------------------------------
    /**
     * 生成禁止かどうかのチェック。
     */
    public boolean isShutOff() { return shutOffP; }

    // このリンクが現在 pollution level > 0.0 な MapArea 内を通っているかのフラグ
    protected boolean polluted = false;
    public void setPolluted(boolean b) { polluted = b; }
    public boolean isPolluted() { return polluted; }

    // このリンク上にかかっている MapArea のリスト
    protected ArrayList<MapArea> intersectedMapAreas = new ArrayList<MapArea>();
    public void addIntersectedMapArea(MapArea area) { intersectedMapAreas.add(area); }
    public ArrayList<MapArea> getIntersectedMapAreas() { return intersectedMapAreas; }

    //------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------
    /**
     */
    public MapLink(String _id,
                   double _length, double _width) {
        super(_id);

        agents = new ArrayList<AgentBase>();
        length = _length;
        width = _width;

        selected = false;

        clearMentalLengthTable() ;
    }
    /**
     */
    public MapLink(String _id, 
            MapNode _from, MapNode _to,
            double _length, double _width
            ) {
        this(_id, _length, _width);
        nodes_are_set = true;
        fromNode = _from;
        toNode = _to;
    }

    //------------------------------------------------------------
    // accessor
    //------------------------------------------------------------
    /**
     * 長さの取得
     */
    public double getLength() {
        return length ;
    }

    //------------------------------------------------------------
    /**
     * 長さの設定
     */
    public void setLength(double _length) {
        length = _length ;
    }

    //------------------------------------------------------------
    /**
     * mentalLengthTable のクリア。
     */
    public void clearMentalLengthTable() {
        mentalLengthTable = new HashMap<String, Double>() ;
    }
    
    //------------------------------------------------------------
    /**
     * 主観的距離を持っているかのチェック。
     * @param mentalMode 主観距離の指定タグ。
     * @param fromNode リンクに侵入するノード。
     *        距離が非対称の時に使う。（[2016-01-30 I.Noda]未実装）
     * @return 距離が定義されていればtrue。
     */
    public boolean hasMentalLength(Term mentalMode, MapNode fromNode) {
        return mentalLengthTable.containsKey(mentalMode.getString()) ;
    }
    
    //------------------------------------------------------------
    /**
     * 主観的距離の取得。
     * 指定された tag の距離が定義されていない時は、
     * UndefinedMentalLength を返す。
     * @param mentalMode 主観距離の指定タグ。
     *        mentalMode が null (NavigationHint.DefaultMentalMode)
     *        なら、もとの length。
     * @param fromNode リンクに侵入するノード。
     *        距離が非対称の時に使う。（[2016-01-30 I.Noda]未実装）
     * @return 距離が定義されていればその値。定義されていなければ、
     */
    public double getMentalLength(Term mentalMode, MapNode fromNode) {
        if(mentalMode == NavigationHint.DefaultMentalMode) {
            return length ;
        } else if(hasMentalLength(mentalMode, fromNode)) {
            return mentalLengthTable.get(mentalMode.getString()) ;
        } else {
            Itk.logWarn("undefined mental length",
                        "[link ID=", this.ID, "] ",
                        "calc on-the-fly.") ;
            PathChooser chooser = Dijkstra.getPathChooser(mentalMode) ;
            if(chooser == null) {
                Itk.logError("unknown mentalMode. ",
                             "mode=", mentalMode) ;
                Itk.quitByError() ;
            } 
            return chooser.calcLinkCost(this, fromNode) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 主観的距離の設定。
     * @param mentalMode 主観距離の指定タグ。tag が null なら、もとの length。
     * @param fromNode リンクに侵入するノード。
     *        距離が非対称の時に使う。（[2016-01-30 I.Noda]未実装）
     * @param _length 主観距離。
     */
    public void setMentalLength(Term mentalMode,
                                    MapNode fromNode,
                                    double _length) {
        if(mentalMode == NavigationHint.DefaultMentalMode) {
            setLength(_length) ;
        } else {
            mentalLengthTable.put(mentalMode.getString(), _length) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * 幅の取得
     */
    public double getWidth() {
        return width ;
    }

    //------------------------------------------------------------
    /**
     * 幅の設定
     */
    public void setWidth(double _width) {
        width = _width ;
    }

    /*public String getType() {
        return "Link";
    }*/

    public void prepareForSimulation() {
        setup_lanes();
    }

    public void prepareAdd() {
        fromNode.addLink(this);
        toNode.addLink(this);
    }

    public void prepareRemove() {
        fromNode.removeLink(this);
        toNode.removeLink(this);
    }

    public MapNode getOther(final MapNode node) {
        if (toNode == node) return fromNode;
        return toNode;
    }

    public double getAverageHeight() {
        return (fromNode.getHeight() + toNode.getHeight()) / 2.0;
    }

    public void preUpdate(SimTime currentTime) {
        setup_lanes();
    }

    /**
     * リンクの処理。
     * [2015.07.04 I.Noda]
     * preUpdate と同じだが、表示のためにここでもやらないといけない。
     * これまでは、agent ごとに setup_lanes を呼んでしまっていた。
     * これを避けるためにここでまとめて処理してみる。
     * 本来は、sort すべきか？
     * [2015.09.07 斉藤]
     * preUpdate 後に agent が移動するため再ソートが必要。
     * ソートは setup_lanes の中でおこなう様にした。
     */
    public void update(SimTime currentTime) {
        setup_lanes();
    }

    /**
     * 真の混雑度 [2014.12.15 I.Noda]
     * たいがい 1.0 以下。
     */
    public double realCrowdness() {
        return agents.size() / width / length;
    }

    /**
     * 混雑度の元の定義 [2014.12.15 I.Noda]
     */
    public double crowdness() {
        double phi = realCrowdness() ;
        
        if (phi < 1.0) return 1.0;
        return phi;
    }
    
    public boolean isBetweenHeight(double min_h, double max_h) {
        if (!getFrom().isBetweenHeight(min_h, max_h)) return false;
        if (!getTo().isBetweenHeight(min_h, max_h)) return false;
        return true;
    }

    private ArrayList<AgentBase> forwardLane  = new ArrayList<AgentBase>() ;
    private ArrayList<AgentBase> backwardLane = new ArrayList<AgentBase>() ;

    /**
     * リンク上の agents のソートと各レーンへの振り分け。
     */
    private void setup_lanes() {
        if (! agents.isEmpty()) {
            Collections.sort(agents) ;
        }
        forwardLane.clear() ;
        backwardLane.clear() ;
        for (AgentBase agent : agents) {
            if (agent.isForwardDirection()) {
                forwardLane.add(agent);
            } else {
                backwardLane.add(agent);
            }
        }
        /* [2015.01.03 I.Noda]
         * 以下の sort は、agents がすでに sort されているので無駄なはず。
         * なので排除。
         */
        //Collections.sort(forwardLane);
        //Collections.sort(backwardLane);
    }

    private void add_agent_to_lane(AgentBase agent) {
        if (agent.isForwardDirection()) {
            forwardLane.add(agent);
            Collections.sort(forwardLane);    
        } else {
            backwardLane.add(agent);
            Collections.sort(backwardLane);
        }
    }

    public ArrayList<AgentBase> getLane(Direction dir) {
        switch(dir) {
        case Forward : return forwardLane;
        case Backward : return backwardLane ;
        default : return null ;
        }
    }

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    static public double dWidth = 1.0 ;

    //------------------------------------------------------------
    /**
     * リンクのレーンの幅。
     * レーンの幅は、forward/backward のレーンに存在し，
     * 視野(認識可能最大数)に入っているエージェント数に
     * 比例して、元の width から割り振られる。
     * この際，視野はfallbackのlink/laneShareDiffuserにより調節される．
     * laneShareDiffuserを1.0にした場合，レーン上すべてが視野に入る．
     * 算出される幅は，1 以下にはしない。
     */
    public int getLaneWidth(Direction dir) {
        int maxSightSize
          = (int)Math.pow(forwardLane.size() + backwardLane.size(),
              1.0 / laneShareDiffuser);
        int d = Math.min(getLane(dir).size(), maxSightSize);
        int forward = Math.min(forwardLane.size(), maxSightSize);
        int backward = Math.min(backwardLane.size(), maxSightSize);
        int laneWidth = (int)(d * (width / dWidth) / (forward + backward));

        if (laneWidth == 0) {
            laneWidth = 1;
        }
        return laneWidth;
    }

    //------------------------------------------------------------
    /**
     * リンクの比較演算子。
     * Hash や BinaryTree、sort 用。
     */
    public int compareTo(MapLink link) {
        return ID.compareTo(link.ID);
    }

    //------------------------------------------------------------
    /**
     * リンク上のある点の位置の計算。リンク上の視点からの距離を指定。
     * ただし、リンクが真っ直ぐであると仮定。
     */
    public Point2D calcPosition(double position) {
        double x = fromNode.getX() + (toNode.getX() - fromNode.getX()) * 
            position / length;
        double y = fromNode.getY() + (toNode.getY() - fromNode.getY()) * 
            position / length;

        return new Point2D.Double(x,y);
    }

    //------------------------------------------------------------
    /**
     * リンク上のある点の高さの計算。リンク上の視点からの距離を指定。
     * ただし、リンクが真っ直ぐであると仮定。
     */
    public double calcHeight(double position) {
        return fromNode.getHeight() + (toNode.getHeight() - fromNode
                .getHeight()) * position / length;
    }

    public Point2D getMiddlePoint() {
        return new Point2D.Double((fromNode.getX() + toNode.getX()) / 2.0, (fromNode.getY() + toNode.getY()) / 2.0);
    }

    public void agentEnters(AgentBase agent) {
        agents.add(agent);
        /* [2015.01.09 I.Noda]
         * 以下の処理はおそらく無駄。
         * しかもいちいち sort しているので、計算時間の無駄のはず。
        if (agent.isForwardDirection() || agent.isBackwardDirection()) {
            add_agent_to_lane(agent);
        }
        */

        /* alert message を新しいエージェントに伝える */
        for(HashMap.Entry<Term, SimTime> entry : alertMessageTable.entrySet()) {
            agent.alertMessage(entry.getKey(), entry.getValue()) ;
        }
    }

    public void agentEntersOnLane(AgentBase agent) {
        agentEnters(agent) ;
        if (agent.isForwardDirection() || agent.isBackwardDirection()) {
            add_agent_to_lane(agent);
        }
    }

    public void agentExits (AgentBase agent) {
        assert(agents.contains(agent));
        agents.remove(agent);
        if (! forwardLane.remove(agent)) {
            backwardLane.remove(agent);
        }
    }

    public void setFromTo(MapNode from, MapNode to) throws Exception {
        /* should only be called on setup */
        if (nodes_are_set) {
            throw new Exception("From and To already set.");
        }
        fromNode = from;
        toNode = to;
        nodes_are_set = true;
    }

    public MapNode getFrom() {
        return fromNode;
    }

    public MapNode getTo() {
        return toNode;
    }

    /**
     * 指定タグを持つノードを取得する
     */
    public MapNode getNode(String tag) {
        if (fromNode.hasTag(tag)) {
            return fromNode;
        }
        if (toNode.hasTag(tag)) {
            return toNode;
        }
        return null;
    }

    public Boolean equals(MapLink rhs){
        return hasSameEndsForward(rhs) || hasSameEndsBackward(rhs) ;
    }

    //------------------------------------------------------------
    /**
     * From 終端での index 取得。
     */
    public int getIndexAtFromNode() {
        return indexAtFromNode ;
    }

    /**
     * From 終端での index セット。
     */
    public void setIndexAtFromNode(int index) {
        indexAtFromNode = index ;
    }

    /**
     * To 終端での index 取得。
     */
    public int getIndexAtToNode() {
        return indexAtToNode ;
    }

    /**
     * To 終端での index セット。
     */
    public void setIndexAtToNode(int index) {
        indexAtToNode = index ;
    }

    /**
     * From/To 終端での index 取得。
     * 終端でないノードの場合は、-1。
     */
    public int getIndexAtNode(MapNode node) {
        if(node == fromNode) {
            return getIndexAtFromNode() ;
        } else if (node == toNode) {
            return getIndexAtToNode() ;
        } else {
            return -1 ;
        }
    }

    /**
     * From/To 終端での index セット。
     * 終端でないノードの場合は、false を返す。
     */
    public boolean setIndexAtNode(MapNode node, int index) {
        if(node == fromNode) {
            setIndexAtFromNode(index) ;
            return true ;
        } else if (node == toNode) {
            setIndexAtToNode(index) ;
            return true ;
        } else {
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * 同じ終端ノードを持っているか (forward)
     */
    public boolean hasSameEndsForward(MapLink otherLink) {
        return (fromNode == otherLink.fromNode && toNode == otherLink.toNode) ;
    }

    //------------------------------------------------------------
    /**
     * 同じ終端ノードを持っているか (backward)
     */
    public boolean hasSameEndsBackward(MapLink otherLink) {
        return (toNode == otherLink.fromNode && fromNode == otherLink.toNode) ;
    }

    public void clear() {
        agents.clear();
    }

    public Line2D getLine2D() {
        return new Line2D.Double(fromNode.getX(), fromNode.getY(), toNode.getX(), toNode.getY());
    }

    public ArrayList<AgentBase> getAgents() {
        return agents;
    }

    @Override
    public NType getNodeType() {
        return NType.LINK;
    }

    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());
        element.setAttribute("id", ID);
        element.setAttribute("length", "" + length);
        element.setAttribute("width", "" + width);
        element.setAttribute("from", fromNode.ID);
        element.setAttribute("to", toNode.ID);
        return element;
    }
    
    public static String getNodeTypeString() {
        return "Link";
    }

    public static MapLink fromDom(Element element) {
        /* used in NetworkMap.setupNetwork */
        String id = element.getAttribute("id");
        if(id == null || id.isEmpty()) {
            Itk.logError("Link element in DOM has no ID" + element) ;
            Itk.quitByError() ;
        }
        /* get length and width */
        double length = Double.parseDouble(element.getAttribute("length"));
        double width = Double.parseDouble(element.getAttribute("width"));
        String nodes[] = new String[2];
        nodes[0] = element.getAttribute("from");
        nodes[1] =element.getAttribute("to");

        /* used in NetworkMap.setupNetwork */
        MapLink link = new MapLink(id, length, width);

        link.getAttributesFromDom(element);

        link.setUserObject(nodes);

        return link;
    }

    /**
     * タグをクリア。
     * ノードのcacheもクリア。
     */
    @Override
    public void allTagsClear() {
        boolean tagsIsEmpty = tags.isEmpty();
        super.allTagsClear() ;
        clearCacheInNodes() ;
        if (! tagsIsEmpty && map != null) {
            map.getNotifier().linkTagRemoved(this);
        }
    }

    /**
     * タグを追加。
     * ノードのcacheもクリア。
     */
    @Override
    public boolean addTag(String _tag) {
        boolean result = super.addTag(_tag) ;
        if (result) {
            clearCacheInNodes() ;
            if (map != null) {
                map.getNotifier().linkTagAdded(this, _tag);
            }
        }
        return result ;
    }

    /**
     * タグを削除。
     * ノードのcacheもクリア。
     */
    @Override
    public void removeTag(String _tag) {
        int tagsSize = tags.size();
        super.removeTag(_tag) ;
        clearCacheInNodes() ;
        if (map != null && tags.size() < tagsSize) {
            map.getNotifier().linkTagRemoved(this);
        }
    }

    /**
     * ノードのcacheをクリア。
     */
    private void clearCacheInNodes() {
        if(fromNode != null) fromNode.clearUsableLinkTableCache() ;
        if(toNode != null) toNode.clearUsableLinkTableCache() ;
    }

    public String toString() {
        return getTagString();
    }

    /**
     * このオブジェクトの状態をテキストで取得する
     */
    public String getStatusText() {
        StringBuilder buff = new StringBuilder();
        buff.append("Link ID: ").append(this.ID).append("\n");
        buff.append("length: ").append(this.getLength()).append("\n");
        buff.append("width: ").append(this.getWidth()).append("\n");
        buff.append("laneWidth(Forward): ").append(this.getLaneWidth(Direction.Forward)).append("\n");
        buff.append("laneWidth(Backward): ").append(this.getLaneWidth(Direction.Backward)).append("\n");
        buff.append("tags: ").append(this.getTagString()).append("\n");
        buff.append("agents: ").append(this.getAgents().size()).append("\n");
        MapNode fromNode = this.getFrom();
        if (fromNode == null) {
            buff.append("from Node: null\n");
        } else {
            buff.append("from Node:").append("\n");
            buff.append("    Node ID: ").append(fromNode.ID).append("\n");
            buff.append("    x: ").append(fromNode.getX()).append("\n");
            buff.append("    y: ").append(fromNode.getY()).append("\n");
            buff.append("    height: ").append(fromNode.getHeight()).append("\n");
            buff.append("    tags: ").append(fromNode.getTagString()).append("\n");
        }
        MapNode toNode = this.getTo();
        if (toNode == null) {
            buff.append("to Node: null\n");
        } else {
            buff.append("to Node:").append("\n");
            buff.append("    Node ID: ").append(toNode.ID).append("\n");
            buff.append("    x: ").append(toNode.getX()).append("\n");
            buff.append("    y: ").append(toNode.getY()).append("\n");
            buff.append("    height: ").append(toNode.getHeight()).append("\n");
            buff.append("    tags: ").append(toNode.getTagString()).append("\n");
        }
        return buff.toString();
    }

    //------------------------------------------------------------
    /**
     * リンクの情報を短く示す文字列。
     * @return 文字列
     */
    public String toShortInfo() {
        return ("Link[" + ID
                + ",tag:" + getTags()
                + ",from:" + (fromNode == null ?
                              "(null)" : String.valueOf(fromNode.ID))
                + ",to:" + (toNode == null ?
                            "(null)" : String.valueOf(toNode.ID))
                + "]" ) ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードから見て正方向かどうかのチェック
     * @param originNode エージェントが入る側のノード
     * @return originNode が fromNode と同じなら true
     */
    public boolean isForwardDirectionFrom(MapNode originNode) {
        return originNode == getFrom() ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て正方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @return destinationNode が toNode と同じなら true
     */
    public boolean isForwardDirectionTo(MapNode destinationNode) {
        return destinationNode == getTo() ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードから見て逆方向かどうかのチェック
     * @param originNode エージェントが入る側のノード
     * @return originNode が fromNode と同じなら true
     */
    public boolean isBackwardDirectionFrom(MapNode originNode) {
        return originNode == getTo() ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て逆方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @return destinationNode が toNode と同じなら true
     */
    public boolean isBackwardDirectionTo(MapNode destinationNode) {
        return destinationNode == getFrom() ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードから見た時の direction の値
     * @param originNode エージェントが入る側のノード
     * @return 順方向なら Direction.Forward。逆なら Direction.Backward。
     */
    public Direction directionValueFrom(MapNode originNode) {
        return (isForwardDirectionFrom(originNode) ?
                Direction.Forward :
                Direction.Backward) ;
    }

    //------------------------------------------------------------
    /**
     * リンクが指定されたノードの方を見て正方向かどうかのチェック
     * @param destinationNode エージェントが向かう方のノード
     * @return 順方向なら Direction.Forward。逆なら Direction.Backward。
     */
    public Direction directionValueTo(MapNode destinationNode) {
        return (isForwardDirectionTo(destinationNode) ?
                Direction.Forward :
                Direction.Backward) ;
    }

    //------------------------------------------------------------
    /**
     * リンク上の相対位置が、絶対方向でリンク上のどの位置にあるか。
     * @param originNode エージェントが入る側のノード
     * @param relativePos エージェント進行方向から見た位置
     * @return 順方向なら relativePos。逆なら length-relativePos。
     */
    public double calcAbstractPositionOnLinkByDirectionFrom(MapNode originNode,
                                                            double relativePos) {
        return (isForwardDirectionFrom(originNode) ?
                relativePos :
                length - relativePos) ;
    }

    //------------------------------------------------------------
    /**
     * リンク上の相対位置が、絶対方向でリンク上のどの位置にあるか。
     * @param destinationNode エージェントが向かう方のノード
     * @param relativePos エージェント進行方向から見た位置
     * @return 順方向なら relativePos。逆なら length-relativePos。
     */
    public double calcAbstractPositionOnLinkByDirectionTo(MapNode destinationNode,
                                                    double relativePos) {
        return (isForwardDirectionTo(destinationNode) ?
                relativePos :
                length - relativePos) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (forward) のチェック
     * @return OneWayForward なら true
     */
    public boolean isOneWayForward() {
        return hasTag(Tag_OneWayForward) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (forward) にする。
     * @return 変化があったなら true
     */
    public boolean setOneWayForward() {
        return setOneWayForward(true) ;
    }
    //------------------------------------------------------------
    /**
     * OneWay (forward) にする。
     * @param flag もし false なら OneWay 解除。
     * @return 変化があったなら true
     */
    public boolean setOneWayForward(boolean flag) {
        return setSpecialTag(Tag_OneWayForward, flag) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (backward) のチェック
     * @return OneWayBackward なら true
     */
    public boolean isOneWayBackward() {
        return hasTag(Tag_OneWayBackward) ;
    }

    //------------------------------------------------------------
    /**
     * OneWay (backward) にする。
     * @return 変化があったなら true
     */
    public boolean setOneWayBackward() {
        return setOneWayBackward(true) ;
    }
    //------------------------------------------------------------
    /**
     * OneWay (backward) にする。
     * @return 変化があったなら true
     */
    public boolean setOneWayBackward(boolean flag) {
        return setSpecialTag(Tag_OneWayBackward, flag) ;
    }

    //------------------------------------------------------------
    /**
     * ある方向に利用可能かチェック。(from側から)
     * @param fromNode リンクに侵入する側のノード
     * @return 利用可能なら true
     */
    public boolean isAvailableFrom(MapNode fromNode) {
        if(isRoadClosed() ||
           (isOneWayForward() && isBackwardDirectionFrom(fromNode)) ||
           (isOneWayBackward() && isForwardDirectionFrom(fromNode))) {
            /* 進入禁止の場合 */
            return false ;
        } else {
            return true ;
        }
    }

    //------------------------------------------------------------
    /**
     * ある方向に利用可能かチェック。(to側から)
     * @param toNode リンクで向かう側のノード
     * @return 利用可能なら true
     */
    public boolean isAvailableTo(MapNode toNode) {
        if(isRoadClosed() ||
           (isOneWayForward() && isBackwardDirectionTo(toNode)) ||
           (isOneWayBackward() && isForwardDirectionTo(toNode))) {
            /* 進入禁止の場合 */
            return false ;
        } else {
            return true ;
        }
    }

    //------------------------------------------------------------
    /**
     * RoadClosed のチェック
     * @return RoadClosed なら true
     */
    public boolean isRoadClosed() {
        return hasTag(Tag_RoadClosed) ;
    }

    //------------------------------------------------------------
    /**
     * RoadClosed にする。
     * @return 変化があったなら true
     */
    public boolean setRoadClosed() {
        return setRoadClosed(true) ;
    }
    //------------------------------------------------------------
    /**
     * RoadClosed にする。
     * @return 変化があったなら true
     */
    public boolean setRoadClosed(boolean flag) {
        return setSpecialTag(Tag_RoadClosed, flag) ;
    }
    //------------------------------------------------------------
    /**
     * Special Tag の set/reset
     * @param specialTag 指定するタグ
     * @param flag set/reset を指定。
     * @return 変化があったなら true
     */
    public boolean setSpecialTag(String specialTag, boolean flag) {
        if(hasTag(specialTag)) {
            if(flag) {
                return false ;
            } else {
                removeTag(specialTag) ;
                return true ;
            }
        } else {
            if(flag) {
                addTag(specialTag) ;
                return true ;
            } else {
                return false ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 交通規制処理
     * @param agent: 規制を加えるエージェント。リンク上にいる。
     * @param currentTime: 現在時刻
     * @return 規制が適用されたら true
     */
    public double calcRestrictedSpeed(double speed, AgentBase agent,
                                      SimTime currentTime) {
        /* 分断制御 
         */
        if(isGateClosed(agent, currentTime)) {
            /*
             * [2018.10.18 I.Noda]
             * リンク上での分断制御をやめてみる。
             * ノード側のバグが判明したので、復活。
             * ゲートのplaceタグを持つノードでもちゃんと止まるようになる。
             */
            speed = 0 ;
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
     * そのリンクにおける自由速度
     * @param emptySpeed: 元になるスピード
     * @param agent: 対象となるエージェント。リンク上にいる。
     * @param currentTime: 現在時刻
     * @return 自由速度
     */
    public double calcEmptySpeedForAgent(double emptySpeed,
                                         AgentBase agent,
                                         SimTime currentTime) {
        /* 制約ルール適用 */
        for(int i = 0 ; i < emptySpeedRestrictRule.getArraySize() ; i++) {
            Term rule = emptySpeedRestrictRule.getNthTerm(i) ;
            emptySpeed = applyRestrictionRule(emptySpeed, rule, agent,
                                              currentTime) ;
        }

        return emptySpeed ;
    }

    //------------------------------------------------------------
    /**
     * そのリンク上の forward 向きのエージェントで、一定速度以下の
     * 人数。絶対スピードで比較。
     * @param upperSpeed: 基準となるスピード。
     * @return 人数
     */
    public int countSlowAgentAbsolute_Forward(double upperSpeed) {
        int count = 0 ;
        for(AgentBase agent : forwardLane) {
            if(agent.getSpeed() <= upperSpeed) count++ ;
        }
        return count ;
    }

    /**
     * そのリンク上の forward 向きのエージェントで、一定速度以下の
     * 人数。自由速度からの相対スピードで比較。
     * @param upperRatio: 基準となるスピード比率。
     * @return 人数
     */
    public int countSlowAgentRelative_Forward(double upperRatio) {
        int count = 0 ;
        for(AgentBase agent : forwardLane) {
            double upperSpeed =
                ((WalkAgent)agent).getEmptySpeed() * upperRatio ;
            if(agent.getSpeed() <= upperSpeed) count++ ;
        }
        return count ;
    }

    /**
     * そのリンク上の backward 向きのエージェントで、一定速度以下の
     * 人数。
     * @param upperSpeed: 基準となるスピード。
     * @return 人数
     */
    public int countSlowAgentAbsolute_Backward(double upperSpeed) {
        int count = 0 ;
        for(AgentBase agent : backwardLane) {
            if(agent.getSpeed() <= upperSpeed) count++ ;
        }
        return count ;
    }

    /**
     * そのリンク上の backward 向きのエージェントで、一定速度以下の
     * 人数。自由速度からの相対スピードで比較。
     * @param upperRatio: 基準となるスピード比率。
     * @return 人数
     */
    public int countSlowAgentRelative_Backward(double upperRatio){
        int count = 0 ;
        for(AgentBase agent : backwardLane) {
            double upperSpeed =
                ((WalkAgent)agent).getEmptySpeed() * upperRatio ;
            if(agent.getSpeed() <= upperSpeed) count++ ;
        }
        return count ;
    }

    /**
     * 指定ノードの通過エージェントカウンタを取得する
     */
    public PassCounter getPassCounter(MapNode node) {
        if (passCounters == null) {
            passCounters = new HashMap<MapNode, PassCounter>();
            return null;
        }
        return passCounters.get(node);
    }

    /**
     * 指定タグを持つノードの通過エージェントカウンタを取得する
     */
    public PassCounter getPassCounter(String tag) {
        return getPassCounter(getNode(tag));
    }

    /**
     * 指定ノードの通過エージェントカウンタを有効化する
     */
    public void enablePassCounter(MapNode node) {
        PassCounter passCounter = getPassCounter(node);
        if (passCounter == null) {
            passCounter = new PassCounter();
            passCounters.put(node, passCounter);
        }
        passCounter.enabled = true;
    }

    /**
     * 指定タグを持つノードの通過エージェントカウンタを有効化する
     */
    public void enablePassCounter(String tag) {
        enablePassCounter(getNode(tag));
    }

    /**
     * 指定ノードの通過エージェントカウンタを無効化する
     */
    public void disablePassCounter(MapNode node) {
        PassCounter passCounter = getPassCounter(node);
        if (passCounter == null) {
            return;
        }
        passCounter.enabled = false;
    }

    /**
     * 指定タグを持つノードの通過エージェントカウンタを無効化する
     */
    public void disablePassCounter(String tag) {
        disablePassCounter(getNode(tag));
    }

    /**
     * 指定ノードの通過エージェントカウンタをリセットする
     */
    public void resetPassCounter(MapNode node) {
        PassCounter passCounter = getPassCounter(node);
        if (passCounter == null) {
            return;
        }
        passCounter.entryCount = 0;
        passCounter.exitCount = 0;
    }

    /**
     * 指定タグを持つノードの通過エージェントカウンタをリセットする
     */
    public void resetPassCounter(String tag) {
        resetPassCounter(getNode(tag));
    }

    /**
     * 指定ノードの通過エージェントカウンタを1増加する
     */
    public void incrementPassCounter(MapNode node, boolean entry) {
        if (passCounters == null) {
            // 無用なオブジェクトを作らない様にするため
            return;
        }
        PassCounter passCounter = getPassCounter(node);
        if (passCounter == null || ! passCounter.enabled) {
            return;
        }
        if (entry) {
            passCounter.entryCount++;
        } else {
            passCounter.exitCount++;
        }
    }

    /**
     * 指定タグを持つノードの通過エージェントカウンタを1増加する
     */
    public void incrementPassCounter(String tag, boolean entry) {
        incrementPassCounter(getNode(tag), entry);
    }

    /**
     * 指定ノードの通過エージェント数を取得する
     */
    public long getPassCount(MapNode node, boolean entry) {
        PassCounter passCounter = getPassCounter(node);
        if (passCounter == null) {
            return 0;
        }
        if (entry) {
            return passCounter.entryCount;
        }
        return passCounter.exitCount;
    }

    /**
     * 指定タグを持つノードの通過エージェント数を取得する
     */
    public long getPassCount(String tag, boolean entry) {
        return getPassCount(getNode(tag), entry);
    }
}
