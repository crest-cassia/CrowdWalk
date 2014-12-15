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
 *   <LI> class 定義で WaitRunningAroundPerson を extends </LI>
 *   <LI> class 定義で Serializable を implements </LI>
 *   <LI> typeString を static で定義。これにクラス名(short name)をいれる
 *        その際、ClassFinder.alias() を使う。これをすれば、short name で
 *        クラス名を指定できるようになる。</LI>
 *   <LI> getAgentTypeString() と getTypeName() も定義しておく </LI>
 *   <LI> コンストラクタは、引数なし、2引数、6引数のものを定義しておく <LI>
 *   <LI> copyAndInitialize()、 copyAndInitializeBody() も定義しておく。</LI> 
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
import nodagumi.ananPJ.Agents.WaitRunningAroundPerson ;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 気まぐれエージェント
 */
public class CapriciousAgent
    extends WaitRunningAroundPerson
    implements Serializable {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString =
        ClassFinder.alias("CapriciousAgent",
                          Itk.currentClassName()) ;

    public static String getAgentTypeString() { return typeString ;}
    public static String getTypeName() { return typeString ;}

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 気まぐれ度合い（cost への random の度合い）の規定値
     */
    //static final public double DefaultCapriciousMargin = 10000.0 ;
    static final public double DefaultCapriciousMargin = 200.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 気まぐれ度合い（cost への random の度合い）
     */
    public double capriciousMargin = DefaultCapriciousMargin ;

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
     * 複製
     */
    @Override
    public EvacuationAgent copyAndInitialize() {
        CapriciousAgent r = new CapriciousAgent(0, random) ;
        return copyAndInitializeBody(r) ;
    }

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    public EvacuationAgent copyAndInitializeBody(CapriciousAgent r) {
        super.copyAndInitializeBody(r) ;
        r.capriciousMargin = capriciousMargin ;
        return r ;
    }

    /**
     * Conf による初期化。
     */
    public void initByConf(Map<String, Object> conf) {
        super.initByConf(conf) ;
        if(config.containsKey("margin")) {
            //Itk.dbgMsg("margin", config.get("margin")) ;
            capriciousMargin = new Double(config.get("margin").toString()) ;
        }
    } ;
    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、ランダム要素を加味する。
     */
    public double calcWayCostTo(MapLink _way, MapNode _node, String _target) {
        MapNode other = _way.getOther(_node);
        double cost = other.getDistance(_target);
        cost += _way.length;
        double noise = capriciousMargin * random.nextDouble() ;
        return cost + noise;
    }

} // class CapriciousAgent

