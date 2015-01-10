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

import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;

import nodagumi.Itk.*;

//======================================================================
/**
 * エージェントの存在位置を示すクラス
 */
public class Place {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * forward/backword direction の場合の direction の値
     */
    final static public double DirectionValue_Forward = 1.0 ;
    final static public double DirectionValue_Backward = -1.0 ;
    final static public double DirectionValue_None = 0.0 ;

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
     * @param _node : 入った側のノード
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
        link = _link ;
        if(_entering) {
            setEnteringNode(_node) ;
        } else {
            setEnteringNode(link.getOther(_node)) ;
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
     * コピー生成
     */
    public Place duplicate() {
        Place newPlace = new Place() ;
        newPlace.set(this) ;
        return newPlace ;
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
     * 進入ノード
     */
    public MapNode getEnteringNode() { return enteringNode ; } ;

    //------------------------------------------------------------
    /**
     * 進入ノード
     */
    public Place setEnteringNode(MapNode _node) {
        if(_node != null && !isEitherNode(_node)) {
            Itk.dumpStackTrace() ;
            Itk.dbgWrn("Specified node is not in the link. Instead using null.") ;
            Itk.dbgMsg("node", _node) ;
            Itk.dbgMsg("link", getLink()) ;
        } else {
            enteringNode = _node ;
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 進入ノード
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
     * リンクの from-to と進行方向が逆か?
     */
    public double getDirectionValue() {
        if(isForwardDirection()) {
            return DirectionValue_Forward ;
        } else if(isBackwardDirection()) {
            return DirectionValue_Backward ;
        } else {
            return DirectionValue_None ;
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
        return getLink().length ;
    }

    //------------------------------------------------------------
    /**
     * リンク視点の位置
     */
    public double positionOnLink() {
        if(isForwardDirection()) {
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

    //############################################################
    /**
     * 位置操作
     */
    //------------------------------------------------------------
    /**
     * 前進
     */
    public Place makeAdvance(double distance) {
        setAdvancingDistance(getAdvancingDistance() + distance) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 位置チェック。
     * リンク上にあるか。両端を含む。
     * @return リンク上であれば true。
     */
    public boolean isOnLink() {
        return isOnLink(true) ;
    }

    //------------------------------------------------------------
    /**
     * 位置チェック。
     * リンク上にあるか。両端を含むかはフラグで判別。
     * @param includeEdge : 両端を含むかどうか。true/false。
     * @return リンク上であれば true。
     */
    public boolean isOnLink(boolean includeEdge) {
        return isOnLinkWithAdvance(0.0, includeEdge) ;
    }

    //------------------------------------------------------------
    /**
     * ある程度進んだ地点の位置チェック。
     * @param advance : 現地点からの相対進行距離
     * @return リンク上であれば true。
     */
    public boolean isOnLinkWithAdvance(double advance) {
        return isOnLinkWithAdvance(advance, true) ;
    }

    //------------------------------------------------------------
    /**
     * ある程度進んだ地点の位置チェック。
     * @param advance : 現地点からの相対進行距離
     * @param includeEdge : 両端を含むかどうか。true/false。
     * @return リンク上であれば true。
     */
    public boolean isOnLinkWithAdvance(double advance, boolean includeEdge) {
        double distance = getAdvancingDistance() + advance ;
        if(includeEdge) {
            return distance >= 0.0 && distance <= getLinkLength() ;
        } else {
            return distance > 0.0 && distance < getLinkLength() ;
        }
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

} // class Foo

