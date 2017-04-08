// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;

/* Parent of agent/node/link (leaf of OB) */
public abstract class OBMapPart extends OBNode {

	/**
	 * 引数なしconstractor。 ClassFinder.newByName で必要。
	 */
	public OBMapPart() {} ;

    public OBMapPart(String _ID){
		init(_ID) ;
	}

	/**
	 * 初期化。constractorから分離。
	 */
    @Override
    public void init(String _ID){
        super.init(_ID);
        this.setAllowsChildren(false);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** 
     * ルールで用いるタグ類
     */
    static private final String Slot_tag  	    = "tag".intern() ;
    static private final String Slot_type 	    = "type".intern() ; 
    static private final String Slot_factor     = "factor".intern() ;
    static private final String Slot_set		= "set".intern() ;
    static private final String Slot_multiply	= "multiply".intern() ;
    static private final String Slot_add		= "add".intern() ;
    
    final public int DefaultOne = 1 ;
    //------------------------------------------------------------
    /**
     * ルールに従い速度等のvalueに制限を加える。
	 * ルールの書き方は、以下の通り。
	 * <pre>
	 *   _rule_ ::= { "type" : _Type_,
	 *                "tag" : _String_,
	 *                "factor" : _Double_ }
	 *   _Type_ ::= "set" | "multiply" | "add"
	 * </pre>
	 * "tag" には、link, node が所有すべきタグを指定する。
	 * このタグを持つlink,node 上のエージェントが受ける影響を求める。
	 * "factor" は、"type" により意味が異なる。
	 * <ul>
	 *   <li> "set" : value を "factor" で置き換え。 </li>
	 *   <li> "multiply" : value に "factor" を乗ずる。 </li>
	 *   <li> "add" : value に "factor" で加える。 </li>
	 * </ul>
     * @param value: 元になる値
     * @param rule: ルール
     * @param agent: 対象となるエージェント。リンク上にいる。
     * @param currnetTime: 現在時刻
     * @return 変更された値
     */
    public double applyRestrictionRule(double value, Term rule,
                                       AgentBase agent, SimTime currnetTime) {
        String tag = rule.getArgString(Slot_tag) ;
        if(hasTag(tag)) {
            String type = rule.getArgString(Slot_type) ;
            double factor = rule.getArgDouble(Slot_factor) ;
            if(type == Slot_set) {
                value = factor ;
            } else if(type == Slot_multiply) {
                value *= factor ;
            } else if(type == Slot_add) {
                value += factor ;
            } else {
                Itk.logError("unknown restriction rule type", "rule=", rule) ;
            }
        }
        return value ;
    }




}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
