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

import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.BustleAgent ;
import nodagumi.ananPJ.Agents.Think.ThinkEngine;
import nodagumi.ananPJ.misc.SimTime;

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
 * 思考ルールの記述法については {@link nodagumi.ananPJ.Agents.Think.ThinkEngine ThinkEngine} 参照。
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
    static final public double Fallback_CapriciousMargin = 200.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 気まぐれ度合い（cost への random の度合い）
     */
    public double capriciousMargin = Fallback_CapriciousMargin ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * その場Uターンをするかどうかのフラグ。
     * これが true になると、
     * calcSpeed() で speed = 0.0 とし、
     * advanceNextPlace で、対向レーンに移る。
     */
    private boolean turnAroundP = false ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 推論エンジン
     */
    public ThinkEngine thinkEngine = new ThinkEngine() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * alert された message
     */
    public HashMap<Term, SimTime> alertedMessageTable =
        new HashMap<Term, SimTime>() ;

    //------------------------------------------------------------
    // コンストラクタ
    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public RationalAgent(){}

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
    public double calcCostFromNodeViaLink(MapLink _link, MapNode _node, Term _target) throws TargetNotFoundException {
        double cost = super.calcCostFromNodeViaLink(_link, _node, _target) ;
        double noise = capriciousMargin * getRandomDouble() ;
        return cost + noise;
    }

    //------------------------------------------------------------
    // alertMessage
    //------------------------------------------------------------
    /**
     * Alert 関係
     */
    public void alertMessage(Term message, SimTime alertTime) {
        alertedMessageTable.put(message, alertTime) ;
        Itk.logInfo("hear Alert Message", this, message, alertTime,
                    currentPlace) ;
    }

    //------------------------------------------------------------
    // 推論
    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    @Override
    public void preUpdate(SimTime currentTime) {
        this.currentTime = currentTime ;
        thinkCycle() ;
        super.preUpdate(currentTime) ;
    }

    //------------------------------------------------------------
    /**
     * 思考ルーチン
     * 状態が変わる毎に呼ばれるべき。
     */
    public Term thinkCycle() {
        return thinkEngine.think() ;
    }

    //------------------------------------------------------------
    // Uターン関連
    //------------------------------------------------------------
    /**
     * Uターンフラグを on
     */
    public boolean setTurnAround() {
        return setTurnAround(true) ;
    }
    
    /**
     * Uターンフラグを on/off
     */
    public boolean setTurnAround(boolean _turnAroundP) {
        return turnAroundP = _turnAroundP ;
    }

    //------------------------------------------------------------
    /**
     * 速度計算(turn around を考慮)
     */
    @Override
    protected double calcSpeed(double previousSpeed, SimTime currentTime) {
        if(turnAroundP) {
            return 0.0 ;
        } else {
            return super.calcSpeed(previousSpeed, currentTime) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * 次の位置の計算(turn around を考慮)
     */
    @Override
    protected boolean advanceNextPlace(double _speed, SimTime currentTime,
                                       boolean stayOnLink) {
        boolean _turnAroundP = turnAroundP ;
        turnAroundP = false ;
        if(_turnAroundP && canTurnAround()) {
            nextPlace.set(currentPlace) ;
            nextPlace.turnAround() ;
            return false ;
        } else {
            return super.advanceNextPlace(_speed, currentTime, stayOnLink) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 現在地でturn around できるかどうか。
     */
    protected boolean canTurnAround() {
        return currentPlace.canTurnAround() ;
    }
    
} // class RationalAgent

