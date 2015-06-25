// -*- mode: java; indent-tabs-mode: nil -*-
/** Rational Agent
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/02/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/02/15]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents;

import java.util.Random;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.BustleAgent ;
import nodagumi.ananPJ.Agents.Think.ThinkEngine;

import nodagumi.Itk.*;

//======================================================================
/**
 * 理性のある、ルールに基づいて行動するエージェント
 *
 * <h3> config, fallbackResources に書ける設定 </h3>
 * {@link BustleAgent} に加えて、
 * <pre>
 *  {
 *    "margin" : __double__ // 距離コストに重畳するノイズの大きさ。一様乱数。
 *    "rule" : __ThinkRule__ // 思考ルール
 * }
 * </pre>
 * 思考ルールの記述法については {@link Think.ThinkEngine} 参照。
 */
public class RationalAgent extends BustleAgent {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "RationalAgent" ;
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

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 推論エンジン
     */
    public ThinkEngine thinkEngine = new ThinkEngine() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * alert された message
     */
    public HashMap<Term, Double> alertedMessageTable =
        new HashMap<Term, Double>() ;

    //------------------------------------------------------------
    // コンストラクタ
    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public RationalAgent(){}

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public RationalAgent(Random _random) {
        init(_random) ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        capriciousMargin = getDoubleFromConfig("margin", capriciousMargin) ;

        thinkEngine.setAgent(this) ;
        thinkEngine.setRule(getTermFromConfig("rule", null)) ;
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

    //------------------------------------------------------------
    // alertMessage
    //------------------------------------------------------------
    /**
     * Alert 関係
     */
    public void alertMessage(Term message, double time) {
        alertedMessageTable.put(message, time) ;
        Itk.logInfo("hear Alert Message", this, message, time, currentPlace) ;
    }

    //------------------------------------------------------------
    // 推論
    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    @Override
    public void preUpdate(double time) {
        currentTime = time ;
        thinkCycle() ;
        super.preUpdate(time) ;
    }

    //------------------------------------------------------------
    /**
     * 思考ルーチン
     * 状態が変わる毎に呼ばれるべき。
     */
    public Term thinkCycle() {
        return thinkEngine.think() ;
    }

} // class RationalAgent

