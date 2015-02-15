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

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考エンジン
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
     * ルールは、以下で定義される形式
     *   <rule> ::= <expr>
     *   <expr> ::= <null> | [<expr>,<expr>,...] | <headedTerm>
     *   <null> ::= null | {}
     *   <headedTerm> ::= {"" : <head>, (<argKey> : <expr>)*}
     *   <head> ::= <String>
     *   <argKey> ::= <String>
     * 
     * <null> は, NOP (No Operation)扱い。
     * [<expr>, ...] の形式は、{"" : "proc", "body" : [<expr>, ...]} と同じ。
     * <headedTerm> で以下は組み込み。
     *   {"" : "if", "condition" : <expr>, "then" : <expr> (, "else" : <expr>)?}
     *   {"" : "proc", "body" : [<expr>, <expr>, ...]}
     *   {"" : "not", "body" : <expr>}
     *   {"" : "and", "body" : [<expr>, <expr>, ...]}
     *   {"" : "or", "body" : [<expr>, <expr>, ...]}
     *   {"" : "true"}  ："true" も同じ
     *   {"" : "false"} ："false" も同じ
     *   {"" : "null"} => "null" と同じ
     *   {"" : "quote", "value" : <expr>}
     * 実処理用の <headedTerm>
     *   {"" : "log", "tag" : <String>, "value" : <expr>}
     */
    private Term rule = null ;

    
    //------------------------------------------------------------
    // 特殊 Term
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * null
     */
    static public Term Term_Null = new Term() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * true
     */
    static public Term Term_True = new Term("true") ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * false
     */
    static public Term Term_False = new Term("false") ;

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
            return Term_Null ;
        } else if(expr.isArray()) {
            return think_Proc(expr) ;
        } else if(!(expr.getHead() instanceof String)) {
            // maybe, true or false or numbers
            return expr ;
        } else {
            String head = expr.getHeadString() ;
            if(head.equals("null")) {
                return Term_Null ;
            } else if(head.equals("true")) {
                return Term_True ;
            } else if(head.equals("false")) {
                return Term_False ;
            } else if(head.equals("not")) {
                return think_Not(expr.getArgTerm("body")) ;
            } else if(head.equals("and")) {
                return think_And(expr.getArgTerm("body")) ;
            } else if(head.equals("or")) {
                return think_Or(expr.getArgTerm("body")) ;
            } else if(head.equals("proc")) {
                return think_Proc(expr.getArgTerm("body")) ;
            } else {
                return think_Generic(head, expr) ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(not)
     */
    public Term think_Not(Term expr) {
        Term result = think(expr) ;
        if(checkFalse(result)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * False かどうかのチェック
     */
    public boolean checkFalse(Term expr) {
        return (expr == null ||
                expr.equals(Term_Null) ||
                expr.equals(Term_False)) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(And)
     */
    public Term think_And(Term expr) {
        if(!expr.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("expr:", expr) ;
            System.exit(1) ;
        }

        Term result = Term_True ;
        for(int i = 0 ; i < expr.getArraySize() ; i++) {
            Term subExpr = expr.getNthTerm(i) ;
            result = think(subExpr) ;
            if(checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(Or)
     */
    public Term think_Or(Term expr) {
        if(!expr.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("expr:", expr) ;
            System.exit(1) ;
        }

        Term result = Term_False ;
        for(int i = 0 ; i < expr.getArraySize() ; i++) {
            Term subExpr = expr.getNthTerm(i) ;
            result = think(subExpr) ;
            if(!checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(proc)
     */
    public Term think_Proc(Term expr) {
        if(!expr.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("expr:", expr) ;
            System.exit(1) ;
        }

        Term result = Term_Null ;
        for(int i = 0 ; i < expr.getArraySize() ; i++) {
            Term subExpr = expr.getNthTerm(i) ;
            result = think(subExpr) ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(generic)
     */
    public Term think_Generic(String head, Term expr) {
        if(head.equals("log")) {
            return think_Log(head, expr) ;
        } else {
            Itk.logWarn("unknown expression", "expr=", expr) ;
            return Term_Null ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(generic)
     */
    public Term think_Log(String head, Term expr) {
        String tag = expr.getArgString("tag") ;
        Term value = expr.getArgTerm("value") ;
        Term result = think(value) ;
        if(agent == null) {
            Itk.logInfo("ThinkEngine", tag, ":", result) ;
        } else {
            Itk.logInfo("Agent(" + agent + ")", tag, ":", result) ;
        }
        return Term_True ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkEngine

