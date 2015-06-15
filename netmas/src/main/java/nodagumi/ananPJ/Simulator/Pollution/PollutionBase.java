// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator.Pollution;

import java.util.HashMap;

import nodagumi.ananPJ.Agents.AgentBase;

import nodagumi.Itk.*;

public abstract class PollutionBase {
    public abstract void expose(AgentBase agent, double pollutionLevel);
    public abstract void effect(AgentBase agent);
    public abstract int getTriage(AgentBase agent);
    public abstract boolean isDead(AgentBase agent);
    public abstract PollutionEffectInfo newEffectInfo(AgentBase agent) ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    protected static HashMap<String, PollutionBase> instanceTable =
        new HashMap<String, PollutionBase>();

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    protected static ClassFinder classFinder = new ClassFinder() ;

    //============================================================
    //------------------------------------------------------------
    /**
     * サブクラスのインスタンスを取得(必要なら生成)する。
     */
    public static PollutionBase getInstance(String className) {
        PollutionBase pollution = instanceTable.get(className);
        if (pollution == null) {
            pollution = createInstance(className);
            instanceTable.put(className, pollution);
        }
        return pollution;
    }

    //============================================================
    //------------------------------------------------------------
    // サブクラスのインスタンスを生成する
    protected static PollutionBase createInstance(String className) {
        try {
            return (PollutionBase)classFinder.newByName(className) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("pollution_type の設定が間違っています。",
                         className) ;
            System.exit(1);
        }
        return null;
    }

    //============================================================
    //------------------------------------------------------------
    // 別名登録。
    protected static void register(String className, Class<?> klass) {
        classFinder.registerAlias(className, klass) ;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * デフォルトのPollutionクラス登録
     */
    static {
        register("Accumulated", AccumulatedPollution.class) ;
        register("NonAccumulated", NonAccumulatedPollution.class) ;
    }

    //============================================================
    //============================================================
    /**
     * Pollution の状態をエージェント毎に保持する構造体。
     */
    abstract public static class PollutionEffectInfo {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * エージェントへのリンク
         */
        public AgentBase agent = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * pollution の影響計算するところ。
         */
        public PollutionBase pollution = null ;

        //--------------------------------------------------
        /**
         * コンストラクタ
         */
        public PollutionEffectInfo(AgentBase _agent, PollutionBase _pollution) {
            agent = _agent ;
            pollution = _pollution ;
        }

        //--------------------------------------------------
        /**
         * export
         */
        public void expose(double pollutionLevel) {
            pollution.expose(agent, pollutionLevel) ;
        }

        //--------------------------------------------------
        /**
         * effect
         */
        public void effect() {
            pollution.effect(agent) ;
        }

        //--------------------------------------------------
        /**
         * effect
         */
        public int getTriage() {
            return pollution.getTriage(agent) ;
        }

        //--------------------------------------------------
        /**
         * is Dead
         */
        public boolean isDead() {
            return pollution.isDead(agent) ;
        }

        //--------------------------------------------------
        /**
         * dumpResult 用の値。(現在値)
         */
        abstract public double currentValueForLog() ;

        //--------------------------------------------------
        /**
         * dumpResult 用の値。(累積値)
         */
        abstract public double accumulatedValueForLog() ;

    }

}
