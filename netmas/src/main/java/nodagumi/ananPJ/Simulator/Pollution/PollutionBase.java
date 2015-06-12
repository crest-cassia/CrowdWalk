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

    protected static HashMap<String, PollutionBase> pollutions =
        new HashMap<String, PollutionBase>();

    // サブクラスのインスタンスを取得(必要なら生成)する
    public static PollutionBase getInstance(String className) {
        PollutionBase pollution = pollutions.get(className);
        if (pollution == null) {
            pollution = createInstance(className);
            pollutions.put(className, pollution);
        }
        return pollution;
    }

    // サブクラスのインスタンスを生成する
    protected static PollutionBase createInstance(String className) {
        try {
            Class<PollutionBase> clazz = (Class<PollutionBase>)Class.forName("nodagumi.ananPJ.Simulator.Pollution." + className);
            return clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            System.err.println("Property error - pollution_type の設定が間違っています。");
            System.exit(1);
        }
        return null;
    }
}
