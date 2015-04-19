// -*- mode: java; indent-tabs-mode: nil -*-
/** 
 * 素朴なエージェント
 * いろんなエージェントクラスのベースにする予定のもの。
 * AwaitAgent がベースだが、
 * いずれ整理する予定
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/19 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/19]: Create This File. </LI>
 * </UL>
 */

package nodagumi.ananPJ.Agents;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import nodagumi.ananPJ.misc.Place;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Node.TargetNotFoundException;
import nodagumi.ananPJ.Agents.AwaitAgent ;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 素朴なエージェント
 * 各種エージェントクラスのベースにするもの。
 *
 * <h3> config, fallbackResources に書ける設定 </h3>
 * {@link AwaitAgent} に加えて、
 * <pre>
 *  {
 *    "trailCountStep" : __double__ // 一度通った道のカウントアップの幅。
 * }
 * </pre>
 */
public class NaiveAgent
    extends AwaitAgent {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "NaiveAgent" ;
    public static String getTypeName() { return typeString ;}

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 一度通った道に加えるカウントの大きさの規定値
     */
    static final public double FallBack_TrailCountStep = 1.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 一度通った道に加えるカウントの大きさの規定値
     */
    protected double trailCountStep = FallBack_TrailCountStep ;

    //============================================================
    /**
     * 一度通った道の足跡テーブル
     */
    public class TrailCountTable {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 格納用テーブル
         */
        private HashMap<MapLink, HashMap<MapNode, Double>> table =
            new HashMap<MapLink, HashMap<MapNode, Double>>() ;
        
        //------------------------------
        /**
         * アクセス
         * @param fromNode 出発ノード
         * @param toLink fromNodeからたどるリンク
         */
        public Double get(MapNode fromNode, MapLink toLink) {
            HashMap<MapNode, Double> nodeTab = table.get(toLink) ;
            if(nodeTab != null) {
                Double count = nodeTab.get(fromNode) ;
                if(count != null) {
                    return count ;
                } else {
                    return 0.0 ;
                }
            } else {
                return 0.0 ;
            }
        }

        //------------------------------
        /**
         * 足跡を残す
         * ここでは、単純に、通る毎にcountStepを足しているだけ。
         * もし、減衰するフェロモンのように扱いたい場合は、
         * 以下のようにする必要がある。
         * <UL>
         *  <LI> 開始からの時刻 t がわかっているとする。 </LI>
         *  <LI> あるリンクの値が count とすると、
         *       count = log(1+exp(count-t)) + t </LI>
         *  <LI> 実際の値として用いるのは、
         *       value = exp(count - t) </LI>
         *  <LI> 上記のようにすれば、value は、毎ステップ (1/e) 倍に
         *       していきつつ、使われると 1 増えるとしたのと同じになる</LI>
         * </UL>
         * @param fromNode 出発ノード
         * @param toLink fromNodeからたどるリンク
         * @param countStep 一歩の値
         */
        public Double add(MapNode fromNode, MapLink toLink, double countStep) {
            HashMap<MapNode, Double> nodeTab = table.get(toLink) ;
            if(nodeTab == null) {
                nodeTab = new HashMap<MapNode, Double>() ;
                table.put(toLink, nodeTab) ;
            }

            Double count = nodeTab.get(fromNode) ;
            if(count == null) count = 0.0 ;

            count += countStep ;
            nodeTab.put(fromNode, count) ;

            return count ;
        }
    } // class TrailCountTable

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 一度通った道のカウント（アナログ値）
     * 斉藤さんの実装を参考に再実装
     */
    public TrailCountTable trailCountTable = new TrailCountTable() ;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public NaiveAgent(){}

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public NaiveAgent(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    @Override
    public AgentBase copyAndInitializeBody(AgentBase _r) {
        NaiveAgent r = (NaiveAgent)_r ;
        return super.copyAndInitializeBody(r) ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        trailCountStep = getDoubleFromConfig("trailCountStep", trailCountStep) ;

    } ;

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、ランダム要素を加味する。
     */
    @Override
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) throws TargetNotFoundException {
        return super.calcWayCostTo(_way, _node, _target) ;
    }

    //------------------------------------------------------------
    /**
     * 最終決定したルート、足跡情報の記録
     * [2014.12.19 I.Noda] tryToPassNode() より移動
     */
    @Override
    protected void recordTrail(double time, Place passingPlace, 
                               MapLink nextLink) {
        super.recordTrail(time, passingPlace, nextLink) ;

        trailCountTable.add(passingPlace.getHeadingNode(),
                            nextLink, trailCountStep) ;
    }

} // class NaiveAgent

