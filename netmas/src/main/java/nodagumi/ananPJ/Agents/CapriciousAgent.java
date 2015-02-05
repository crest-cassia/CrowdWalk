// -*- mode: java; indent-tabs-mode: nil -*-
/** 
 * 気まぐれな経路選択をするエージェント
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/14 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/14]: Create This File. </LI>
 * </UL>
 * <B>Memo:</B>
 * <P>新たな Agent のクラスを作る方法。</P>
 * <UL>
 *   <LI> 以下の package と import をすべていれる。 </LI>
 *   <LI> class 定義で AwaitAgent を extends </LI>
 *   <LI> class 定義で Serializable を implements </LI>
 *   <LI> typeString を static で定義。これにクラス名(short name)をいれる。
 *   <LI> getTypeName() も定義しておく </LI>
 *   <LI> コンストラクタは、引数なし、2引数、6引数のものを定義しておく <LI>
 *   <LI> copyAndInitializeBody() も定義しておく。</LI> 
 *   <LI> calcWayCostTo() を定義する。
 *        ここを工夫すると、経路選択をいろいろいじれる</LI>
 *   <LI> misc/GenerateAgent.java の、GenerateAgent._dummyAgents に、
 *        新しいクラスのインスタンスを追加しておく。
 *        これがないと、このクラスが JVM にロードされない。 </LI>
 *   <LI> ジェネレーションファイルのモードライン（先頭行）で、
 *        version を 1 にしておく。</LI>
 *   <LI> ジェネレーションファイルの各行の先頭に、クラス名を書く。
 *        クラス名は fullpath でも、上記の short name でもよい。</LI>
 * </UL>
 */

package nodagumi.ananPJ.Agents;

import java.io.Serializable;
import java.util.Random;
import java.util.Map;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.Agents.NaiveAgent;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 気まぐれエージェント
 */
public class CapriciousAgent extends NaiveAgent
    implements Serializable {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "CapriciousAgent" ;
    public static String getTypeName() { return typeString ;}

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 気まぐれ度合い（cost への random の度合い）の規定値
     */
    static final public double FallBack_CapriciousMargin = 200.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 気まぐれ度合い（cost への random の度合い）
     */
    public double capriciousMargin = FallBack_CapriciousMargin ;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public CapriciousAgent(){}

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public CapriciousAgent(int _id,
                           double speed, double _confidence,
                           double allowance, double time, Random _random) {
        init(_id, speed,  _confidence, allowance, time, _random) ;
    }

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public CapriciousAgent(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    @Override
    public AgentBase copyAndInitializeBody(AgentBase _r) {
        CapriciousAgent r = (CapriciousAgent)_r ;
        super.copyAndInitializeBody(r) ;
        r.capriciousMargin = capriciousMargin ;
        return r ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf) {
        super.initByConf(conf) ;

        capriciousMargin = getDoubleFromConfig("margin", capriciousMargin) ;
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
        double noise = capriciousMargin * random.nextDouble() ;
        return cost + noise;
    }

} // class CapriciousAgent

