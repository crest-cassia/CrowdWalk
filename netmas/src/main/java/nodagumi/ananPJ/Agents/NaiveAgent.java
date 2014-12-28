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
import java.util.HashMap;

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
     * 一度通った道に加えるカウントの大きさの規定値
     */
    static final public double DefaultTrailCountStep = 1.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 一度通った道に加えるカウントの大きさの規定値
     */
    private double trailCountStep = DefaultTrailCountStep ;

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
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) {
        return super.calcWayCostTo(_way, _node, _target) ;
    }

    //------------------------------------------------------------
    /**
     * 最終決定したルート、足跡情報の記録
     * [2014.12.19 I.Noda] tryToPassNode() より移動
     */
    @Override
    protected void recordTrail(double time) {
        super.recordTrail(time) ;

        if (next_node != prev_node) {
            trailCountTable.add(next_node, next_link_candidate, trailCountStep) ;
        }
    }

} // class NaiveAgent

