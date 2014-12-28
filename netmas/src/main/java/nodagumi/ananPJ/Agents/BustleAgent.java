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
    public static String typeString =
        ClassFinder.alias("BustleAgent",
                          Itk.currentClassName()) ;

    public static String getAgentTypeString() { return typeString ;}
    public static String getTypeName() { return typeString ;}

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * せわしない度合い（link の crowdness に乗ずる値）の規定値
     */
    static final public double DefaultBustleWeight = 100.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * せわしない度合い（link の crowdness に乗ずる値）
     */
    public double bustleWeight = DefaultBustleWeight ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 一度通った道をどれくらい避けるかの規定値
     */
    static final public double DefaultTrailWeight = 100.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 一度通った道をどれくらい避けるか（link の trailCount に乗ずる値）
     */
    public double trailWeight = DefaultTrailWeight ;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public BustleAgent(){}

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public BustleAgent(int _id,
                           double speed, double _confidence,
                           double allowance, double time, Random _random) {
        init(_id, speed,  _confidence, allowance, time, _random) ;
    }

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public BustleAgent(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * 複製
     */
    @Override
    public EvacuationAgent copyAndInitialize() {
        BustleAgent r = new BustleAgent(0, random) ;
        return copyAndInitializeBody(r) ;
    }

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    public EvacuationAgent copyAndInitializeBody(BustleAgent r) {
        super.copyAndInitializeBody(r) ;
	r.bustleWeight = bustleWeight ;
        return r ;
    }

    /**
     * Conf による初期化。
     */
    public void initByConf(Term conf) {
        super.initByConf(conf) ;

        if(config.hasArg("weight")) {
            //Itk.dbgMsg("weight", config.get("weight")) ;
            bustleWeight = config.getArgDouble("weight") ;
        }
        if(config.hasArg("trail")) {
            trailWeight = config.getArgDouble("trail") ;
        }
    } ;
    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、ランダム要素を加味する。
     */
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) {
        MapNode other = _way.getOther(_node);
        double cost = other.getDistance(_target) ;
        cost += _way.length;
	double crowdness= bustleWeight * _way.realCrowdness() ;
        double trailCount = trailWeight * trailCountTable.get(_node, _way) ;
        return cost + crowdness + trailCount;
    }

} // class BustleAgent

