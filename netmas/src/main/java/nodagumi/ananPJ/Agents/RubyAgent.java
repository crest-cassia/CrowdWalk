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

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.BustleAgent ;

import nodagumi.Itk.*;

//======================================================================
/**
 * 制御に、Ruby の script を呼び出すエージェント
 */
public class RubyAgent extends WalkAgent {
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
     * constractor。
     */
    public RubyAgent(Random _random) {
        init(_random) ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        rubyAgentClass = getStringFromConfig("rubyAgentClass", rubyAgentClass) ;
    } ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class RubyAgent

