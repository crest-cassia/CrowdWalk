// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Engine
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

package nodagumi.ananPJ.Agents.Think;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.RationalAgent;
import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考エンジン.
 *
 * ルールは、以下で定義される形式
 * <pre>
 *   _rule_ ::= _expr_
 *   _expr_ ::= _null_ | [_expr_,_expr_,...] | _headedTerm_
 *   _null_ ::= null | {}
 *   _headedTerm_ ::= {"" : _head_, (_argKey_ : _expr_)*}
 *   _head_ ::= _String_
 *   _argKey_ ::= _String_
 * </pre>
 *
 * <h3> 組み込み Forms </h3>
 * <ul>
 *  <li> _null_ は, NOP (No Operation)扱い。
 *  <li> [_expr_, ...] の形式は、{"" : "proc", "body" : [_expr_, ...]} と同じ。
 * </ul>
 * <h3> 実処理用の _headedTerm_</h3>
 * <ul>
 *   <li>{@link ThinkFormula "ThinkFormula"}</li>
 * </ul>
 * また、上記にマッチしないアトム（配列でもオブジェクトでもないデータ）は、
 * その値を直接返す。（リテラル扱い）
 */
public class ThinkEngine {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント
     */
    private AgentBase agent = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ルールセット
     */
    private Term rule = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * log tag
     */
    static public String LogTagPrefix = "Think:" ;
    public String logTag() {
        if(isNullAgent()) {
            return LogTagPrefix + "(null)" ;
        } else {
            return LogTagPrefix + agent.toString() ;
        }
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(){
        setAgent(null) ;
        setRule(null) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(AgentBase _agent){
        setAgent(_agent) ;
        setRule(null) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(AgentBase _agent, Term _rule){
        setAgent(_agent) ;
        setRule(_rule) ;
    }

    //------------------------------------------------------------
    // アクセス
    //------------------------------------------------------------
    /**
     * set agent
     */
    public AgentBase setAgent(AgentBase _agent) {
        agent = _agent ;
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * get agent
     */
    public AgentBase getAgent() {
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * check agent is null
     */
    public boolean isNullAgent() {
        return agent == null;
    }

    //------------------------------------------------------------
    /**
     * get agent's alerted message table.
     */
    public HashMap<Term, Double> getAlertedMessageTable() {
        RationalAgent agent = (RationalAgent)getAgent() ;
        return agent.alertedMessageTable ;
    }

    //------------------------------------------------------------
    /**
     * set rule
     */
    public Term setRule(Term _rule) {
        rule = _rule ;
        return rule ;
    }

    //------------------------------------------------------------
    /**
     * get rule
     */
    public Term getRule() {
        return rule ;
    }

    //------------------------------------------------------------
    /**
     * 推論(top)
     */
    public Term think() {
        return think(rule) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(本体)
     */
    public Term think(Term expr) {
        if(expr == null || expr.isNull()) {
            return ThinkFormula.Term_Null ;
        } else if(expr.isArray()) {
            return (((ThinkFormulaLogical)(ThinkFormulaLogical.singleton))
                    .call_procBody(expr, this)) ;
        } else if(!(expr.getHead() instanceof String)) {
            // maybe, true or false or numbers
            return expr ;
        } else {
            String head = expr.getHeadString() ;
            ThinkFormula formula = ThinkFormula.findFormula(head) ;
            if(formula != null) {
                return formula.call(head, expr, this) ;
            } else if(expr.isAtom()) {
                // expr がアトムで、かつ予約語でなければ、そのまま返す。
                return expr ;
            } else {
                Itk.logError("unknown expression for ThinkEngine.",
                             "expr=", expr) ;
                return ThinkFormula.Term_Null ;
            }
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkEngine

