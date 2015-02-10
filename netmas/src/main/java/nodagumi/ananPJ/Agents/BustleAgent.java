// -*- mode: java; indent-tabs-mode: nil -*-
/** 
 * せわしないエージェント
 * 混んでいる路をできるだけ避ける。
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/15]: Create This File. </LI>
 * </UL>
 */

package nodagumi.ananPJ.Agents;

import java.io.Serializable;
import java.util.Random;
import java.util.Map;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.Agents.NaiveAgent ;

import nodagumi.Itk.* ;

//======================================================================
/**
 * せわしないエージェント
 */
public class BustleAgent extends NaiveAgent
    implements Serializable {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "BustleAgent" ;
    public static String getTypeName() { return typeString ;}

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * せわしない度合い（link の crowdness に乗ずる値）の規定値
     */
    static final public double FallBack_BustleWeight = 100.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * せわしない度合い（link の crowdness に乗ずる値）
     */
    public double bustleWeight = FallBack_BustleWeight ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 一度通った道をどれくらい避けるかの規定値
     */
    static final public double FallBack_TrailWeight = 100.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 一度通った道をどれくらい避けるか（link の trailCount に乗ずる値）
     */
    public double trailWeight = FallBack_TrailWeight ;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public BustleAgent(){}

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public BustleAgent(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    public AgentBase copyAndInitializeBody(AgentBase _r) {
        BustleAgent r = (BustleAgent)_r ;
        super.copyAndInitializeBody(r) ;
	r.bustleWeight = bustleWeight ;
        return r ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        bustleWeight = getDoubleFromConfig("weight", bustleWeight) ;
        trailWeight = getDoubleFromConfig("trail", trailWeight) ;
    } ;

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、ランダム要素を加味する。
     */
    @Override
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) {
        MapNode other = _way.getOther(_node);
        double cost = other.getDistance(_target) ;
        cost += _way.length;
	double crowdness= bustleWeight * _way.realCrowdness() ;
        double trailCount = trailWeight * trailCountTable.get(_node, _way) ;
        return cost + crowdness + trailCount;
    }

} // class BustleAgent

