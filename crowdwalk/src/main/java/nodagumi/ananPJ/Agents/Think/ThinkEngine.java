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
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.* ;
import nodagumi.Itk.Itk ;

//======================================================================
/**
 * 思考エンジン.
 *
 * ルールは、以下で定義される形式
 * <pre>
 *   _rule_ ::= _expr_
 *   _expr_ ::= _null_ | [_expr_,_expr_,...] | _headedTerm_
 *   _null_ ::= null | {}
 *   _headedTerm_ ::= {"" : _head_, (_argKey_ : _value_)*} |
 *                    [_head_, _value_, _value_, ...]
 *   _head_ ::= _String_
 *   _argKey_ ::= _String_
 *   _value_ ::= _expr_ | _literal_
 *   _literal_ ::= _String_ | _Number_ | _null_ | true | false
 * </pre>
 *
 * <h3> 組み込み Forms </h3>
 * <ul>
 *  <li> _null_ は, NOP (No Operation)扱い。
 *  <li> [_expr_, ...] の形式は、最初の _expr_ が _literal_ ならば、
 *       _headedTerm_ として扱い、そうでなければ、
 *       {"" : "proc", "body" : [_expr_, ...]} と同じ。
 * </ul>
 * 具体的な記述例などは
 * {@link nodagumi.ananPJ.Agents.Think Think パッケージ参照}。
 *
 * <h3> 実処理用の _headedTerm_</h3>
 * <ul>
 *   <li>{@link ThinkFormula ThinkFormula: 式要素 トップ}</li>
 * </ul>
 * また、上記にマッチしないアトム（配列でもオブジェクトでもないデータ）は、
 * その値を直接返す。（リテラル扱い）
 */
public class ThinkEngine {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント
     */
    private Lexicon lexicon = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント
     */
    private AgentBase agent = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ルールセット
     */
    protected Term rule = null ;

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
        setLexicon(ThinkFormula.lexicon) ;
        setAgent(null) ;
        setRule(null) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(AgentBase _agent){
        setLexicon(ThinkFormula.lexicon) ;
        setAgent(_agent) ;
        setRule(null) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(AgentBase _agent, Term _rule){
        setLexicon(ThinkFormula.lexicon) ;
        setAgent(_agent) ;
        setRule(_rule) ;
    }

    //------------------------------------------------------------
    // アクセス
    //------------------------------------------------------------
    /**
     * set lexicon
     */
    public Lexicon setLexicon(Lexicon _lexicon) {
        lexicon = _lexicon ;
        return lexicon ;
    }

    //------------------------------------------------------------
    /**
     * get lexicon
     */
    public Lexicon getLexicon() {
        return lexicon ;
    }

    //------------------------------------------------------------
    /**
     * Formula を検索
     */
    public ThinkFormula findFormula(String head) {
	return (ThinkFormula)(lexicon.lookUp(head)) ;
    }

    //------------------------------------------------------------
    /**
     * Formula を検索
     */
    public ThinkFormula findFormula(Term head) {
	return findFormula(head.getString()) ;
    }


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
     * get RationalAgent
     */
    public RationalAgent getRationalAgent() {
        return (RationalAgent)agent ;
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
    public HashMap<Term, SimTime> getAlertedMessageTable() {
        RationalAgent agent = getRationalAgent() ;
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
        return think(rule, this) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(本体)
     */
    public Term think(Term expr, Object env) {
        if(expr == null || expr.isNull()) {
            return ThinkFormula.Term_Null ;
        } else {
            String head = getHeadInHeadedTermArray(expr) ;
            if(expr.isArray() && head == null) {
                return (((ThinkFormulaLogical)(ThinkFormulaLogical.singleton))
                        .call_procBody(expr, this, env, 0)) ;
            } else if(!expr.isArray() && !(expr.getHead() instanceof String)) {
                // maybe, true or false or numbers
                return expr ;
            } else {
                if(head == null) {
                    head = expr.getHeadString() ;
                }
                ThinkFormula formula = findFormula(head) ;
                if(formula != null) {
                    return formula.call(head, expr, this, env) ;
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
    }

    //------------------------------------------------------------
    /**
     * proc 型の配列かどうかのチェックし、その head を返す。
     * _expr_ が配列だった場合、最初の要素が文字列リテラルの場合は、
     * _headedTerm_ とみなす。
     * その _headedTerm_ の head 部分を返す。
     * _headedTerm_ とみなせない場合は、nullを返す。
     * @param expr チェックする _expr_.
     * @return head となる String。_headeTerm_ 出ない場合は、null。
     */
    public String getHeadInHeadedTermArray(Term expr) {
        String head = null ;
        if(expr.isArray()) {
            if(expr.getArraySize() > 0) {
                Object firstObj = expr.getNth(0) ;
                if(firstObj instanceof String) {
                    head = (String)firstObj ;
                } else if(firstObj instanceof Term) {
                    // もし、先頭が arg なしの Term ならば。
                    Term firstTerm = (Term)firstObj ;
                    if(firstTerm.isAtom()) {
                        Object headObj = firstTerm.getHead() ;
                        if(headObj instanceof String) {
                            head = (String)headObj ;
                        }
                    }
                }
            }
        }
        //さらに、配列形式を許されたものか、チェック。
        if(ThinkFormula.doesPermitArrayForm(head)) {
            return head ;
        } else {
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * ログ出力インターフェース.
     * @param level : ログレベル
     * @param contP : true なら、継続行とみなした tag, そうでないなら通常tag.
     * @param objects : ログ内容。
     */
    public void logInThinkWithLevel(Itk.LogLevel level,
                                    boolean contP,
                                    Object... objects) {
        String label = (contP ? logTag() : LogTagPrefix) ;
        logGenericWithLevel(level, label, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ出力インターフェース.
     * @param level : ログレベル
     * @param label : ログラベル。
     * @param objects : ログ内容。
     */
    public void logGenericWithLevel(Itk.LogLevel level,
                                    String label,
                                    Object... objects) {
        Itk.logOutput(level, label, objects) ;
    }
    
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkEngine

