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
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.BustleAgent ;
import nodagumi.ananPJ.Agents.AgentFactory;

import nodagumi.ananPJ.Agents.Think.ThinkFormula;

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
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * triggerFilter でチェックするメソッド名の列挙。
     */
    static public enum TriggerEntry {
        preUpdate,
        update,
        calcWayCostTo,
        thinkCycle,
    }

    /**
     * triggerFilter でチェックするメソッド名リスト。
     */
    static protected ArrayList<TriggerEntry> TriggerEntryList
        = new ArrayList<TriggerEntry>(Arrays.asList(TriggerEntry.values())) ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 各メソッドに於いてルビー側のメソッドを呼び出すかどうかの
     * チェックを行うフィルタのテーブル。
     * フィルタは文字列の配列であり、各要素(triggerに対応)の値は、
     * trigger される(ruby を呼び出す) 場合には、ruby のメソッド名が入っている。
     * そうでない場合は、null が入っている。
     * ルビー側のクラスごとに定義する。
     * 構造は、(ruby 風に書くと)
     * <pre>{@code
     *    triggerFilterTable = { 
     *              "RubyClassName0" => [null, "update", null, null],
     *              "RubyClassName1" => ["preUpdate", "update", null, null],
     *              ... }
     * }</pre>
     */
    static protected HashMap<String, ArrayList<String>> triggerFilterTable
        = new HashMap<String, ArrayList<String>>() ;

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
    private ArrayList<String> triggerFilter = null ;

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
        rubyAgent = rubyEngine.newInstanceOfClass(rubyAgentClass, this,
                                                  conf, fallback) ;
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
    private ArrayList<String> prepareTriggerFilterFor(String rubyClassName){
        ArrayList<String> filter = new ArrayList<String>() ;
        for(TriggerEntry trigger : TriggerEntryList) {
            // 配列を拡大。
            while(filter.size() <= trigger.ordinal()) {
                filter.add(null) ;
            }
            String methodName = trigger.toString() ;
            String script = String.format("%s.checkTriggerFilter('%s')",
                                          rubyClassName, methodName) ;
            boolean triggered = rubyEngine.evalBoolean(script) ;
            if(triggered) {
                filter.set(trigger.ordinal(), methodName) ;
            }
        }
        return filter ;
    }

    /**
     * トリガされているかのチェック。
     * @param rubyClassName : ruby での AgentClass 名
     * @return trigger されていれば true
     */
    protected String triggeredMethod(TriggerEntry trigger) {
        return triggerFilter.get(trigger.ordinal()) ;
    }

    //------------------------------------------------------------
    // アクセス関係
    //------------------------------------------------------------
    /**
     * エージェントのIDを返す。 Ruby からの呼び出し用。
     */
    public String getID() {
        return ID ;
    }

    //------------------------------------------------------------
    /**
     * 現在の RoutePlan の先頭に新しいsubgoalを追加。
     */
    public void insertRouteTagSafelyForRuby(Object tag) {
        super.insertRouteTagSafely(Term.ensureTerm(tag)) ;
    }

    //------------------------------------------------------------
    /**
     * alert を聴いた時刻。
     * もし聴いていなければ、null が返る。
     */
    public Double getAlertTimeForRuby(Object message) {
        return alertedMessageTable.get(Term.ensureTerm(message)) ;
    }

    //------------------------------------------------------------
    /**
     * alert を聴いた時刻。
     * もし聴いていなければ、null が返る。
     */
    public boolean hasPlaceTagForRuby(Object tag) {
        String tagString ;
        if(tag instanceof Term) {
            tagString = ((Term)tag).getString() ;
        } else if(tag instanceof String) {
            tagString = (String)tag ;
        } else {
            tagString = tag.toString() ;
        }
        return getCurrentLink().hasTag(tagString) ;
    }

    //------------------------------------------------------------
    // トリガされている wrapper
    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    @Override
    public void preUpdate(double time) {
        currentTime = time ;
        String rubyMethod = triggeredMethod(TriggerEntry.preUpdate) ;
        if(rubyMethod != null) {
            rubyEngine.callMethod(rubyAgent, rubyMethod) ;
        } else {
            super_preUpdate() ;
        }
    }

    /**
     * 上位の preUpdate を呼び出す。
     * ruby からの戻り用。
     */
    public void super_preUpdate() {
        super.preUpdate(currentTime) ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの後半に呼ばれる。
     */
    @Override
    public boolean update(double time) {
        currentTime = time ;
        String rubyMethod = triggeredMethod(TriggerEntry.update) ;
        if(rubyMethod != null) {
            return rubyEngine.callMethodBoolean(rubyAgent, rubyMethod) ;
        } else {
            return super_update() ;
        }
    }

    /**
     * 上位の update を呼び出す。
     * ruby からの戻り用。
     */
    public boolean super_update() {
        return super.update(currentTime) ;
    }

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     */
    @Override
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target)
        throws TargetNotFoundException {
        String rubyMethod = triggeredMethod(TriggerEntry.calcWayCostTo) ;
        if(rubyMethod != null) {
            return rubyEngine.callMethodDouble(rubyAgent, rubyMethod,
                                               _way, _node, _target) ;
        } else {
            return super_calcWayCostTo(_way, _node, _target) ;
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
     * 思考ルーチン
     * 状態が変わる毎に呼ばれるべき。
     */
    @Override
    public Term thinkCycle() {
        String rubyMethod = triggeredMethod(TriggerEntry.thinkCycle) ;
        if(rubyMethod != null) {
            rubyEngine.callMethod(rubyAgent, rubyMethod) ;
            return ThinkFormula.Term_Null ;
        } else {
            return super_thinkCycle() ;
        }
    }

    /**
     * 思考ルーチン
     */
    public Term super_thinkCycle() {
        return super.thinkCycle() ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class RubyAgent

