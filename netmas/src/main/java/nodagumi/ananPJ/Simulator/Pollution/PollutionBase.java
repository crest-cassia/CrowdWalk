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


}
