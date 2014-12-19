// -*- mode: java; indent-tabs-mode: nil -*-
/** 
 * 素朴なエージェント
 * いろんなエージェントクラスのベースにする予定のもの。
 * WaitRunningAroundPerson がベースだが、
 * いずれ整理する予定
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/19 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/19]: Create This File. </LI>
 * </UL>
 */

package nodagumi.ananPJ.Agents;

import java.io.Serializable;
import java.util.Random;
import java.util.Map;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.Agents.WaitRunningAroundPerson ;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 素朴なエージェント
 * 各種エージェントクラスのベースにするもの。
 */
public class NaiveAgent
    extends WaitRunningAroundPerson
    implements Serializable {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString =
        ClassFinder.alias("NaiveAgent",
                          Itk.currentClassName()) ;

    public static String getAgentTypeString() { return typeString ;}
    public static String getTypeName() { return typeString ;}

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * せわしない度合い（link の crowdness に乗ずる値）の規定値
     */
    static final public double DefaultBustleWeight = 100.0 ;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public NaiveAgent(){}

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public NaiveAgent(int _id,
                      double speed, double _confidence,
                      double allowance, double time, Random _random) {
        init(_id, speed,  _confidence, allowance, time, _random) ;
    }

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public NaiveAgent(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * 複製
     */
    @Override
    public EvacuationAgent copyAndInitialize() {
        NaiveAgent r = new NaiveAgent(0, random) ;
        return copyAndInitializeBody(r) ;
    }

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    public EvacuationAgent copyAndInitializeBody(NaiveAgent r) {
        return super.copyAndInitializeBody(r) ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    public void initByConf(Map<String, Object> conf) {
        super.initByConf(conf) ;
    } ;

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、ランダム要素を加味する。
     */
    public double calcWayCostTo(MapLink _way, MapNode _node, String _target) {
        return super.calcWayCostTo(_way, _node, _target) ;
    }

} // class NaiveAgent

