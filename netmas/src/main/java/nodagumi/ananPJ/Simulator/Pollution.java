// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator;

import java.util.HashMap;

import nodagumi.ananPJ.Agents.AgentBase;

public abstract class Pollution {
    public abstract void expose(AgentBase agent, double pollutionLevel);
    public abstract void effect(AgentBase agent);
    public abstract int getTriage(AgentBase agent);
    public abstract boolean isDead(AgentBase agent);

    protected static HashMap<String, Pollution> pollutions = new HashMap<String, Pollution>();

    // サブクラスのインスタンスを取得(必要なら生成)する
    public static Pollution getInstance(String className) {
        Pollution pollution = pollutions.get(className);
        if (pollution == null) {
            pollution = createInstance(className);
            pollutions.put(className, pollution);
        }
        return pollution;
    }

    // サブクラスのインスタンスを生成する
    protected static Pollution createInstance(String className) {
        try {
            Class<Pollution> clazz = (Class<Pollution>)Class.forName("nodagumi.ananPJ.Simulator.pollution." + className);
            return clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            System.err.println("Property error - pollution_type の設定が間違っています。");
            System.exit(1);
        }
        return null;
    }
}
