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
 *   <LI> typeString を static で定義。これにクラス名(short name)をいれる。
 *   <LI> getTypeName() も定義しておく </LI>
 *   <LI> コンストラクタは、引数なし、2引数、6引数のものを定義しておく <LI>
 *   <LI> calcWayCostTo() を定義する。
 *        ここを工夫すると、経路選択をいろいろいじれる</LI>
 *   <LI> Agents/AgentFactory.java の、AgentFactory._dummyAgents に、
 *        新しいクラスのインスタンスを追加しておく。
 *        これがないと、このクラスが JVM にロードされない。 </LI>
 *   <LI> ジェネレーションファイルのモードライン（先頭行）で、
 *        version を 1 にしておく。</LI>
 *   <LI> ジェネレーションファイルの各行の先頭に、クラス名を書く。
 *        クラス名は fullpath でも、上記の short name でもよい。</LI>
 * </UL>
 */

package nodagumi.ananPJ.Agents;

import java.util.Random;
import java.util.Map;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Node.TargetNotFoundException;
import nodagumi.ananPJ.Agents.NaiveAgent;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 気まぐれエージェント
 *
 * <h3> config, fallbackResources に書ける設定 </h3>
 * {@link NaiveAgent} に加えて、
 * <pre>
 *  {
 *    "margin" : __double__ // 距離コストに重畳するノイズの大きさ。一様乱数。
 * }
 * </pre>
 */
public class CapriciousAgent extends NaiveAgent {
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
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        capriciousMargin = getDoubleFromConfig("margin", capriciousMargin) ;
    } ;

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、ランダム要素を加味する。
     */
    @Override
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) throws TargetNotFoundException {
        double cost = super.calcWayCostTo(_way, _node, _target) ;
        double noise = capriciousMargin * random.nextDouble() ;
        return cost + noise;
    }

} // class CapriciousAgent

