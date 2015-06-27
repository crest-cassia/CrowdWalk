// -*- mode: java; indent-tabs-mode: nil -*-
/** Ruby Agent
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/06/25 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/06/25]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents;

import java.util.Random;
import java.util.HashMap;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.BustleAgent ;
import nodagumi.ananPJ.Agents.AgentFactory;

import nodagumi.Itk.*;

//======================================================================
/**
 * 制御に、Ruby の script を呼び出すエージェント。
 *
 * <h3> config, fallbackResources に書ける設定 </h3>
 * {@link RationalAgent} に加えて、
 * <pre>
 *  {
 *    "rubyAgentClass" : __String__ // Ruby 内でのクラス名
 * }
 * </pre>
 * 上記で指定されたクラスのインスタンスが生成され、java 内で以下のメソッドが
 * 呼ばれた時に、ruby 側の対応する同名のメソッドに制御が移る。
 * <UL>
 *  <LI> {@code preUpdate(double time)} : update cycle の前半。次の位置を決める。
 *  <LI> {@code update(double time)} : update cycle の後半。実際に動かす。
 *  <LI> {@code calcWayCostTo(...)} : 分岐点で、各経路の目的地までのコストを計算。
 *       これを元に、最小コストの経路を選ぶ。
 * </UL>
 *
 * このRubyのクラスは、RubyAgentBase クラスを継承しているべきである。
 */
public class RubyAgent extends RationalAgent {
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "RubyAgent" ;
    public static String getTypeName() { return typeString ;}

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby 実行系のリンク
     */
    private ItkRuby rubyEngine = null ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デフォルトのRuby エージェントクラスの名前。
     * Ruby の中でのクラス名。
     */
    static final public String FallBack_RubyAgentClass = "RubyAgentBase" ;

    /**
     * 実際のエージェントクラスの名前。
     * Ruby の中でのクラス名。
     */
    public String rubyAgentClass = FallBack_RubyAgentClass ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby エージェントへのリンク
     */
    private Object rubyAgent = null ;

    //------------------------------------------------------------
    // コンストラクタ
    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public RubyAgent(){}

    //------------------------------------------------------------
    /**
     * 初期化。
     */
    @Override
    public void init(Random _random, EvacuationSimulator simulator,
                     AgentFactory factory, double time) {
        super.init(_random, simulator, factory, time);
        rubyEngine = simulator.getRubyEngine() ;
        if(rubyEngine == null) {
            Itk.logError("ruby engine is not available.",
                         "should specify 'use_ruby' property to be 'true'.") ;
            System.exit(1) ;
        }
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        rubyAgentClass = getStringFromConfig("rubyAgentClass", rubyAgentClass) ;
        rubyAgent = rubyEngine.newInstanceOfClass(rubyAgentClass, this) ;
    } ;

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    @Override
    public void preUpdate(double time) {
        rubyEngine.callMethod(rubyAgent, "preUpdate", time) ;
    }

    /**
     * 上位の preUpdate を呼び出す。
     * ruby からの戻り用。
     */
    public void super_preUpdate(double time) {
        super.preUpdate(time) ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの後半に呼ばれる。
     */
    @Override
    public boolean update(double time) {
        return (boolean)rubyEngine.callMethod(rubyAgent, "update", time) ;
    }

    /**
     * 上位の update を呼び出す。
     * ruby からの戻り用。
     */
    public boolean super_update(double time) {
        return super.update(time) ;
    }

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     */
    @Override
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target)
        throws TargetNotFoundException {
        return (double)rubyEngine.callMethod(rubyAgent, "calcWayCostTo",
                                             _way, _node, _target) ;
    }

    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。super を呼ぶ。
     */
    public double super_calcWayCostTo(MapLink _way, MapNode _node, Term _target)
        throws TargetNotFoundException {
        return super.calcWayCostTo(_way, _node, _target) ;
    }

    //------------------------------------------------------------
    /**
     * エージェントのIDを返す。 Ruby からの呼び出し用。
     */
    public String getID() {
        return ID ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class RubyAgent

