// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk Template for Java
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/12 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/03/27]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.misc ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import java.awt.geom.Point2D;

import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.misc.Trail;

import nodagumi.Itk.*;

//======================================================================
/**
 * エージェントの存在位置を示すクラス
 */
public class Place {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * リンクを抜け出る直前を計算するための係数
     */
    final static public double AlmostOne = (1.0 - 1.0e-13) ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントが存在するリンク
     */
    protected MapLink link = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * そのリンクに入った側のノード
     */
    protected MapNode enteringNode = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * enteringNode から移動した距離
     */
    protected double advancingDistance = 0.0 ;

    //------------------------------------------------------------
    /**
     * constractor (no arg)
     */
    public Place() {
        set(null, null, true, 0.0) ;
    }

    //------------------------------------------------------------
    /**
     * constractor
     * @param _link : エージェントのいるリンク
     * @param _enteringNode : 入った側のノード
     */
    public Place(MapLink _link, MapNode _enteringNode) {
        set(_link, _enteringNode, true, 0.0) ;
    }

    //------------------------------------------------------------
    /**
     * constractor
     * @param _link : エージェントのいるリンク
     * @param _enteringNode : 入った側のノード
     * @param _distance : 進行距離
     */
    public Place(MapLink _link, MapNode _enteringNode, double _distance) {
        set(_link, _enteringNode, true, _distance) ;
    }

    //------------------------------------------------------------
    /**
     * constractor
     * @param _link : エージェントのいるリンク
     * @param _node : どちらかのノード
     * @param _entering : _node が入った側かどうか？
     */
    public Place(MapLink _link, MapNode _node, boolean _entering) {
        set(_link, _node, _entering, 0.0) ;
    }

    //------------------------------------------------------------
    /**
     * constractor
     * @param _link : エージェントのいるリンク
     * @param _node : どちらかのノード
     * @param _entering : _node が入った側かどうか？
     * @param _distance : 進行距離
     */
    public Place(MapLink _link, MapNode _node, boolean _entering,
                 double _distance) {
        set(_link, _node, _entering, _distance) ;
    }

    //------------------------------------------------------------
    /**
     * 各値をセット
     * @param _link : エージェントのいるリンク
     * @param _node : どちらかのノード
     * @param _entering : _node が入った側かどうか？
     * @param _advancingDistance : 進行距離
     */
    public Place set(MapLink _link, MapNode _node, boolean _entering,
                     double _advancingDistance) {
        return set(_link, _node, _entering, _advancingDistance, true) ;
    }

    //------------------------------------------------------------
    /**
     * 各値をセット
     * @param _link : エージェントのいるリンク
     * @param _node : どちらかのノード
     * @param _entering : _node が入った側かどうか？
     * @param _advancingDistance : 進行距離
     * @param safely : enteringNode の設定で、整合性チェックするかどうか
     */
    public Place set(MapLink _link, MapNode _node, boolean _entering,
                     double _advancingDistance, boolean safely) {
        link = _link ;
        if(_entering) {
            setEnteringNode(_node, safely) ;
        } else {
            setEnteringNode(link.getOther(_node), safely) ;
        }

        advancingDistance = _advancingDistance ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * 別のPlaceから各値をセット
     * @param _place : コピーする元の値
     */
    public Place set(Place _place) {
        return set(_place.getLink(),
                   _place.getEnteringNode(), true,
                   _place.getAdvancingDistance()) ;
    }

    //------------------------------------------------------------
    /**
     * リンクをセットし、その上にランダムに配置
     * @param _link : エージェントのいるリンク
     */
    public Place setAtRandomPosition(MapLink _link, Random _random) {
        setLink(_link) ;
        setEnteringNode(null) ;
        double dist = _random.nextDouble() * getLinkLength() ;
        setAdvancingDistance(dist) ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * コピー生成
     */
    public Place duplicate() {
        Place newPlace = new Place() ;
        newPlace.set(this) ;

        return newPlace ;
    }

    //------------------------------------------------------------
    /**
     * 空かどうか？
     * リンクもノードも埋まっていない。
     */
    public boolean isEmpty() {
        return (link == null && enteringNode == null) ;
    }

    //------------------------------------------------------------
    /**
     * 行動開始前かどうか？
     * リンクのみ埋まっている。
     */
    public boolean isBeforeStartFromLink() {
        return (link != null && enteringNode == null) ;
    }

    //------------------------------------------------------------
    /**
     * 行動開始前かどうか？
     * ノードのみ埋まっている。
     */
    public boolean isBeforeStartFromNode() {
        return (link == null && enteringNode != null) ;
    }

    //------------------------------------------------------------
    /**
     * 行動中かどうか?
     */
    public boolean isWalking() {
        return (link != null && enteringNode != null) ;
    }

    //------------------------------------------------------------
    /**
     * 避難完了かどうか？
     * ノードのみ埋まっている。
     * 初期配置で、ノードに配置された時もこれと同じ状態とする。
     */
    public boolean isEvacuated() {
        return (link == null && enteringNode != null) ;
    }

    //############################################################
    /**
     * インスタンス変数アクセス
     */
    //------------------------------------------------------------
    /**
     * リンク
     */
    public MapLink getLink() { return link ; } ;

    //------------------------------------------------------------
    /**
     * リンク
     */
    public Place setLink(MapLink _link) {
        link = _link ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * リンク幅
     */
    public double getLinkWidth() { return getLink().getWidth() ; } ;

    //------------------------------------------------------------
    /**
     * 進入ノード取得
     */
    public MapNode getEnteringNode() { return enteringNode ; } ;

    //------------------------------------------------------------
    /**
     * 進入ノードセット
     */
    public Place setEnteringNode(MapNode _node) {
        return setEnteringNode(_node, true) ;
    }

    //------------------------------------------------------------
    /**
     * 進入ノードセット
     */
    public Place setEnteringNode(MapNode _node, boolean safely) {
        if(safely) {
            return setEnteringNodeSafely(_node) ;
        } else {
            return setEnteringNodeDirectly(_node) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 進入ノード
     * 念の為、リンクに含まれるかチェックしている。
     */
    public Place setEnteringNodeSafely(MapNode _node) {
        if(getLink() != null && _node != null && !isEitherNode(_node)) {
            Itk.dumpStackTrace() ;
            Itk.logWarn("Specified node is not in the link. Instead using null.") ;
            Itk.logWarn_("node", _node) ;
            Itk.logWarn_("link", getLink()) ;
        } else {
            setEnteringNodeDirectly(_node) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 進入ノード
     * リンクとの整合チェックはしない。
     */
    public Place setEnteringNodeDirectly(MapNode _node) {
        enteringNode = _node ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * 進行方向ノード
     */
    public MapNode getHeadingNode() {
        if(enteringNode == null) {
            return null ;
        } else {
            return link.getOther(enteringNode) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 進行距離
     */
    public double getAdvancingDistance() { return advancingDistance ; } ;

    //------------------------------------------------------------
    /**
     * 残りの距離
     */
    public double getRemainingDistance() {
        return getLinkLength() - getAdvancingDistance() ;
    } ;

    //------------------------------------------------------------
    /**
     * 進入ノード
     */
    public Place setAdvancingDistance(double _distance) {
        advancingDistance = _distance ;
        return this ;
    }

    //############################################################
    /**
     * ノードのチェック
     */
    //------------------------------------------------------------
    /**
     * 進入ノードチェック
     */
    public boolean isEnteringNode(MapNode _node) {
        return _node == getEnteringNode() ;
    }

    //------------------------------------------------------------
    /**
     * 進行方向ノードチェック
     */
    public boolean isHeadingNode(MapNode _node) {
        return _node == getHeadingNode() ;
    }

    //------------------------------------------------------------
    /**
     * リンクの fromNode かどうか？
     */
    public boolean isFromNode(MapNode _node) {
        return _node == getFromNode() ;
    }

    //------------------------------------------------------------
    /**
     * リンクの toNode かどうか？
     */
    public boolean isToNode(MapNode _node) {
        return _node == getToNode() ;
    }

    //------------------------------------------------------------
    /**
     * いずれかのノードかどうか?
     */
    public boolean isEitherNode(MapNode _node) {
        return isFromNode(_node) || isToNode(_node) ;
    }

    //############################################################
    /**
     * 方向チェック
     */

    //------------------------------------------------------------
    /**
     * リンクの from-to と進行方向が同じか？
     */
    public boolean isForwardDirection() {
        return getEnteringNode() == getFromNode() ;
    }

    //------------------------------------------------------------
    /**
     * リンクの from-to と進行方向が逆か?
     */
    public boolean isBackwardDirection() {
        return getEnteringNode() == getToNode() ;
    }

    //------------------------------------------------------------
    /**
     * リンク方向が未定か
     */
    public boolean isNoneDirection() {
        return getEnteringNode() == null ;
    }

    //------------------------------------------------------------
    /**
     * リンクの from-to と進行方向が逆か?
     */
    public Direction getDirection() {
        if(isForwardDirection()) {
            return Direction.Forward ;
        } else if(isBackwardDirection()) {
            return Direction.Backward ;
        } else {
            return Direction.None ;
        }
    }

    //############################################################
    /**
     * リンク上の操作
     */
    //------------------------------------------------------------
    /**
     * リンクの長さ
     */
    public double getLinkLength() {
        return getLink().getLength() ;
    }

    //------------------------------------------------------------
    /**
     * リンク視点の位置
     * 方向が定まっていないとき(noneDirection)は、
     * advancingDistance を絶対位置とする。
     */
    public double getPositionOnLink() {
        if(isNoneDirection()) {
            return getAdvancingDistance() ;
        } else if(isForwardDirection()) {
            return getAdvancingDistance() ;
        } else if(isBackwardDirection()) {
            return getLinkLength() - getAdvancingDistance() ;
        } else {
            return 0.0 ;
        }
    }

    //------------------------------------------------------------
    /**
     * リンクの fromNode
     */
    public MapNode getFromNode() {
        return getLink().getFrom() ;
    }

    //------------------------------------------------------------
    /**
     * リンクの toNode
     */
    public MapNode getToNode() {
        return getLink().getTo() ;
    }

    //------------------------------------------------------------
    /**
     * 制約付きリンクかどうかの判定
     * OneWay もしくはroad closed かどうかのチェック。
     */
    public boolean isRestrictedLink() {
        MapLink _link = getLink() ;
        return (_link.isOneWayForward() ||
                _link.isOneWayBackward() ||
                _link.isRoadClosed()) ;
    }

    //############################################################
    /**
     * 位置操作
     */
    //------------------------------------------------------------
    /**
     * 前進(単純)
     * @param distance : 進める距離
     * @return Placeそのものを返す。
     */
    public Place makeAdvance(double distance) {
        setAdvancingDistance(getAdvancingDistance() + distance) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 前進（制約付き）
     * @param distance : 進める距離
     * @param limitOnLink : リンク上にとどまるかどうかのチェック
     * @return Placeそのものを返す。
     */
    public Place makeAdvance(double distance, boolean limitOnLink) {
        makeAdvance(distance) ;
        if(limitOnLink) {
            if(isBeforeLink()) {
                setAdvancingDistance(0.0) ;
            }
            if(isBeyondLink()) {
                setAdvancingDistance(getLinkLength() * AlmostOne) ;
            }
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 反転
     */
    public Place turnAround() {
        MapNode newEnteringNode = getHeadingNode() ;
        double distance = getLinkLength() - getAdvancingDistance() ;

        setEnteringNode(newEnteringNode) ;
        setAdvancingDistance(distance) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 位置チェック。
     * リンク上にあるか。
     * @return リンク上であれば true。
     */
    public boolean isOnLink() {
        return isOnLinkWithAdvance(0.0) ;
    }

    //------------------------------------------------------------
    /**
     * ある程度進んだ地点の位置チェック。
     * 起点(enteringNode)上は、リンク上とする。
     * 終点(headingNode)上は、リンク外とする。
     * @param advance : 現地点からの相対進行距離
     * @return リンク上であれば true。
     */
    public boolean isOnLinkWithAdvance(double advance) {
        double distance = getAdvancingDistance() + advance ;
        return distance >= 0.0 && distance < getLinkLength() ;
    }

    //------------------------------------------------------------
    /**
     * 位置チェック。
     * リンクの手前かどうか？
     * @return 手間であれば true。
     */
    public boolean isBeforeLink() {
        return isBeforeLinkWithAdvance(0.0) ;
    }

    //------------------------------------------------------------
    /**
     * ある程度進んだ地点の位置チェック。
     * リンクの手前かどうか？
     * 起点(enteringNode)上は、リンク上とする。
     * @param advance : 現地点からの相対進行距離
     * @return 手前であれば true。
     */
    public boolean isBeforeLinkWithAdvance(double advance) {
        double distance = getAdvancingDistance() + advance ;
        return distance < 0.0 ;
    }

    //------------------------------------------------------------
    /**
     * 位置チェック。
     * リンクを通り過ぎているか？
     * @return 通りすぎていれば true。
     */
    public boolean isBeyondLink() {
        return isBeyondLinkWithAdvance(0.0) ;
    }

    //------------------------------------------------------------
    /**
     * ある程度進んだ地点の位置チェック。
     * 終点より向こう側か？
     * 終点(headingNode)上は、リンク外とする。
     * @param advance : 現地点からの相対進行距離
     * @return 通りすぎていれば true。
     */
    public boolean isBeyondLinkWithAdvance(double advance) {
        double distance = getAdvancingDistance() + advance ;
        return distance >= getLinkLength() ;
    }


    //############################################################
    /**
     * 次のリンクへの移動
     */
    //------------------------------------------------------------
    /**
     * 次のリンクへの移動
     * @param nextLink : 移り先のリンク
     * @return 変更された Place
     */
    public Place transitTo(MapLink nextLink) {
        MapNode passingNode = getHeadingNode() ;
        double newDistance = getAdvancingDistance() - getLinkLength() ;
        set(nextLink, passingNode, true, newDistance) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 新しい Place で次のリンクへの移動
     * @param nextLink : 移り先のリンク
     * @return 変更された Place
     */
    public Place newPlaceTransitTo(MapLink nextLink) {
        return duplicate().transitTo(nextLink) ;
    }

    //------------------------------------------------------------
    /**
     * 最後のリンクを抜ける
     * link には null が入る。
     * 最後に通りすぎたノードは、enteringNode に残る。
     * @return 変更された Place
     */
    public Place quitLastLink() {
        enteringNode = getHeadingNode() ;
        link = null ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 最後のノード
     * link が null が入っているときだけ意味がある。
     * @return 最後のノード
     */
    public MapNode getLastNode() {
        return getEnteringNode() ;
    }

    //############################################################
    /**
     * レーン操作
     */
    //------------------------------------------------------------
    /**
     * レーン取得
     */
    public ArrayList<AgentBase> getLane() {
        return getLink().getLane(getDirection()) ;
    }

    //------------------------------------------------------------
    /**
     * 対向レーン取得
     */
    public ArrayList<AgentBase> getOtherLane() {
        return getLink().getLane(getDirection().opposite()) ;
    }

    //------------------------------------------------------------
    /**
     * レーン幅取得
     */
    public int getLaneWidth() {
        return getLink().getLaneWidth(getDirection()) ;
    }

    //------------------------------------------------------------
    /**
     * レーン幅取得(対向レーン)
     */
    public int getOtherLaneWidth() {
        return getLink().getLaneWidth(getDirection().opposite()) ;
    }

    //------------------------------------------------------------
    /**
     * レーンの中での順序
     * 進んでいないほど index が小さいとする。
     */
    public int getIndexInLane(AgentBase agent) {
        return Collections.binarySearch(getLane(), agent) ;
    }

    //------------------------------------------------------------
    /**
     * レーンの中での順序
     * 進んでいるほど index が小さいとする。
     */
    public int getIndexFromHeadingInLane(AgentBase agent) {
        return getLane().size() - getIndexInLane(agent) - 1 ;
    }

    //############################################################
    /**
     * 表示関係
     */
    //------------------------------------------------------------
    /**
     * 表示上の位置計算
     */
    public Point2D getPosForDisplay() {
        if (getLink() != null) {
            return getLink().calcAgentPos(getPositionOnLink());
        } else if (getLastNode() != null){
            return getLastNode().getAbsoluteCoordinates();
        } else {
            return null;
        }
    }

    //------------------------------------------------------------
    /**
     * 表示上の高さ計算
     */
    public double getHeightForDisplay() {
        if (getLink() != null) {
            return getLink().calcAgentHeight(getPositionOnLink());
        } else {
            return Double.NaN;
        }
    }

    //------------------------------------------------------------
    /**
     * 文字列化
     */
    public String toString() {
        return ("Place[on:" + getLink().toShortInfo() +
                ",enter:" + getEnteringNode().toShortInfo() +
                ",dist:" + getAdvancingDistance() +
                "]") ;
    }

    
    //============================================================
    /**
     * Place 用の Trail.Content のクラス。
     */
    static public class TrailPlaceContent extends Trail.ContentObject {
        public Object atNode ;
        public Object toLink ;
        
        public TrailPlaceContent(Place place) {
            atNode = (place.getEnteringNode() == null ? null :
                      place.getEnteringNode().getJsonObject()) ;
            toLink = (place.getLink() == null ? null :
                      place.getLink().getJsonObject()) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * Trail.Content interface 用 method
     */
    public Trail.Content getTrailContent() {
        return new TrailPlaceContent(this) ;
    }
    

} // class Foo

