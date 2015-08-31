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

import java.util.Random;
import java.util.Map;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.TargetNotFoundException;
import nodagumi.ananPJ.Agents.NaiveAgent ;

import nodagumi.Itk.* ;

//======================================================================
/**
 * せわしないエージェント
 *
 * <h3> config, fallbackResources に書ける設定 </h3>
 * {@link NaiveAgent} に加えて、
 * <pre>
 *  {
 *    "weight" : __double__ // 混雑度合いの重視度を表す値。
 *    "trail" : __double__ // すでに通った道を避ける度合い。
 * }
 * </pre>
 */
public class BustleAgent extends NaiveAgent {
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
    static final public double Fallback_BustleWeight = 100.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * せわしない度合い（link の crowdness に乗ずる値）
     */
    public double bustleWeight = Fallback_BustleWeight ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 一度通った道をどれくらい避けるかの規定値
     */
    static final public double Fallback_TrailWeight = 100.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 一度通った道をどれくらい避けるか（link の trailCount に乗ずる値）
     */
    public double trailWeight = Fallback_TrailWeight ;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public BustleAgent(){}

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
     * あるlinkを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、混雑度合いと既知道路回避ファクタを加える。
     */
    @Override
    public double calcCostFromNodeViaLink(MapLink _link, MapNode _node, Term _target) throws TargetNotFoundException {
        double cost = super.calcCostFromNodeViaLink(_link, _node, _target) ;
        double crowdness= bustleWeight * _link.realCrowdness() ;
        double trailCount = trailWeight * trailCountTable.get(_node, _link) ;
        return cost + crowdness + trailCount;
    }

} // class BustleAgent

