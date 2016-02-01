// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Formula
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/04/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/04/15]: Create This File. </LI>
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
import nodagumi.ananPJ.Agents.Think.*;
import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考を表す式の処理系。
 * 組み込まれている Formula リスト
 * <ul>
 *  <li>{@link ThinkFormulaLogical#call Logical: 論理形式および実行制御}</li>
 *  <li>{@link ThinkFormulaArithmetic#call Arithmetic: 数値処理など}</li>
 *  <li>{@link ThinkFormulaAgent#call Agent: エージェント制御関係}</li>
 *  <li>{@link ThinkFormulaMisc#call Misc: その他}</li>
 * </ul>
 *
 * <h3>新しい Formula の作成方法 </h3>
 * <a name="addToExistingThinkFormula"></a>
 * <h4> すでにある ThinkFormula*** への追加 </h4>
 * 新しい Formula の head 名を、{@code "fooBar"} とする。
 * また、それに対応するメソッド名を、{@code call_fooBar()} とする。
 * <ol>
 *   <li> {@code static public boolean registerFormulas()} の定義の中に、
 *        {@code ThinkFormula.register("fooBar", singleton);} を追加。
 *   </li>
 *   <li> {@code public Term call(...)} のところの JavaDoc の記述に、
 *        {@code <li>{@link #call_fooBar "fooBar"}</li>} を追加。
 *        (javadoc でちゃんと参照できるようにしておくため。)
 *   </li>
 *   <li> {@code public Term call(...)} の定義の {@code if / else if} の並び
 *        のところで、
 *        {@code else if(head.equals("fooBar")) { return call_fooBar(head, expr, engine);}} 
 *        を追加する。
 *   </li>
 *   <li> {@code public Term call_fooBar(...)} を定義する。
 *        このメソッドの引数は、
 *        <ul>
 *          <li> {@code String head}: "fooBar" が渡ってくるはず。 </li>
 *          <li> {@code Term expr}: formula そのものが渡される。
 *               formula の引数などをアクセスする場合には、
 *               {@code expr.getArgInt("argName")} などを呼び出す。
 *          </li>
 *          <li> {@code ThinkEngine engine}: formula を処理する ThinkEngine。
 *               再帰的な呼び出しの場合は、{@code engine.think(...)} を
 *               使う。
 *               対象となっているエージェントは、{@code engine.getAgent()} で
 *               参照できる。
 *          </li>
 *        </ul>
 *        このメソッドの戻り値は、そのまま formula の戻り値として扱われる。
 *   </li>
 * </ol>
 * 
 * <h4> 新たに ThinkFormula*** を作成する場合 </h4>
 * 新たに作るものを、{@code ThinkFormulaBazBaz.java} とする。
 * <ol>
 *   <li> {@code ThinkFormulaMisc.java} などを参考に
 *        {@code ThinkFormulaBazBaz.java} の雛形を作る。
 *   </li>
 *   <li> {@code ThinkFormulaBazBaz.java} の雛形で必須のなのは、
 *        代表インスタンスの {@code static ThinkFormula singleton;} と、
 *        {@code public boolean registerFormulas(){...}}。
 *   </li>
 *   <li> <a href="#addToExistingThinkFormula">「すでにある ThinkFormula*** への追加」</a> 
 *        に従い、formulaを追加。
 *   </li>
 *   <li> {@code "ThinkFormula.java"} の {@code static public Lexicon lexicon} の
 *        宣言の後にある {@code static { ... }} 中に、
 *        {@code ThinkFormulaBazBaz.registerFormulas();} を追加。
 *   </li>
 *   <li> さらに、{@code abstract public class ThinkFormula} の javadoc のところに、
 *        {@code <li>{@link ThinkFormulaBazBaz#call BazBaz: ???}</li>} を追加。
 *   </li>
 * </ol>
 */
abstract public class ThinkFormula {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Formula Lexicon
     */
    static public Lexicon lexicon = new Lexicon() ;
    static {
        ThinkFormulaMisc.registerFormulas(ThinkFormula.lexicon) ;
        ThinkFormulaLogical.registerFormulas(ThinkFormula.lexicon) ;
        ThinkFormulaArithmetic.registerFormulas(ThinkFormula.lexicon) ;
        ThinkFormulaAgent.registerFormulas(ThinkFormula.lexicon) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * Formula を登録
     */
    static public void register(String name, ThinkFormula formula) {
	lexicon.register(name, formula) ;
    }

    //============================================================
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
    static public Term Term_True = new Term(true) ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * false
     */
    static public Term Term_False = new Term(false) ;

    //============================================================
    //------------------------------------------------------------
    /**
     * boolean term.
     */
    static public Term booleanTerm(boolean v) {
        return (v ? Term_True : Term_False) ;
    }


    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkFormula(){}

    //------------------------------------------------------------
    /**
     * 呼び出し
     */
    abstract public Term call(String head, Term expr,
                              ThinkEngine engine, Object env) ;

    //------------------------------------------------------------
    /**
     * False かどうかのチェック
     */
    public boolean checkFalse(Term expr) {
        return (expr == null ||
                expr.equals(Term_Null) ||
                expr.equals(Term_False)) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormula

