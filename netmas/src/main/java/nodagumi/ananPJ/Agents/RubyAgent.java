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
import java.util.ArrayList;
import java.util.Arrays;

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

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デフォルトのRuby エージェントクラスの名前。
     * Ruby の中でのクラス名。
     */
    static final public String FallBack_RubyAgentClass = "RubyAgentBase" ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 各メソッドに於いてルビー側のメソッドを呼び出すかどうかの
     * チェックを行うフィルタのテーブル。
     * ルビー側のクラスごとに定義する。
     */
    static protected HashMap<String, HashMap<String, Boolean>> triggerFilterTable
        = new HashMap<String, HashMap<String, Boolean>>() ;

    /**
     * triggerFilter でチェックするメソッド名リスト。
     */
    static protected ArrayList<String> triggeredMethodList
        = new ArrayList<String>(Arrays.asList("preUpdate",
                                              "update",
                                              "calcWayCostTo")) ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby 実行系のリンク
     */
    private ItkRuby rubyEngine = null ;

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

    /**
     * ruby 側呼び出しのための triggerFilter。
     */
    private HashMap<String, Boolean> triggerFilter = null ;

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
        setupTriggerFilter(rubyAgentClass) ;
    } ;

    //------------------------------------------------------------
    /**
     * 指定した ruby の AgentClass のtriggerFilter を取得。
     * もし新しいものであれば、新たに作る。
     * @param rubyClassName : ruby での AgentClass 名
     * @return 対応する triggerFilter
     */
    protected void setupTriggerFilter(String rubyClassName) {
        if(!triggerFilterTable.containsKey(rubyClassName)) {
            triggerFilterTable.put(rubyClassName,
                                   prepareTriggerFilterFor(rubyClassName)) ;
        }
        triggerFilter = triggerFilterTable.get(rubyClassName) ;
    }

    /**
     * 指定した ruby の AgentClass のtriggerFilter を作成。
     * @param rubyClassName : ruby での AgentClass 名
     * @return 対応する triggerFilter
     */
    private HashMap<String, Boolean> prepareTriggerFilterFor(String rubyClassName){
        HashMap<String, Boolean> filter = new HashMap<String, Boolean>() ;
        for(String methodName : triggeredMethodList) {
            String script = String.format("%s.checkTriggerFilter('%s')",
                                          rubyClassName, methodName) ;
            filter.put(methodName, rubyEngine.evalBoolean(script)) ;
        }
        return filter ;
    }

    /**
     * トリガされているかのチェック。
     * @param rubyClassName : ruby での AgentClass 名
     * @return trigger されていれば true
     */
    protected boolean isTriggered(String methodName) {
        return triggerFilter.get(methodName) ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    @Override
    public void preUpdate(double time) {
        if(isTriggered("preUpdate")) {
            rubyEngine.callMethod(rubyAgent, "preUpdate", time) ;
        } else {
            super.preUpdate(time) ;
        }
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
        if(isTriggered("update")){
            return rubyEngine.callMethodBoolean(rubyAgent, "update", time) ;
        } else {
            return super.update(time) ;
        }
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
        if(isTriggered("calcWayCostTo")) {
            return rubyEngine.callMethodDouble(rubyAgent, "calcWayCostTo",
                                               _way, _node, _target) ;
        } else {
            return super.calcWayCostTo(_way, _node, _target) ;
        }
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

