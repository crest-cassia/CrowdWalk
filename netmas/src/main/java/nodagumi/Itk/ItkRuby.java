// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk jRuby Utility
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/06/20 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/06/20]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;
import org.jruby.RubyInstanceConfig.CompileMode;

import nodagumi.Itk.Itk;

//======================================================================
/**
 * jruby を呼び出すための簡易ルーチン
 */
public class ItkRuby {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby の実行系。
     * 基本、1インスタンスあればいいようなので、１つだけにしておく。
     */
    private static Ruby rubyEngine = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby Scrupt の評価実行系。
     */
    public ScriptingContainer container ;

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     */
    public ItkRuby() {
        ensureRubyEngine() ;
        container = new ScriptingContainer() ;
        container.setCompileMode(CompileMode.JIT) ;
    }

    //------------------------------------------------------------
    /**
     * script 実行(1行)
     */
    public Object eval(String script) {
        return container.runScriptlet(script) ;
    }

    /**
     * script 実行(複数行)
     */
    public Object eval(String script0, String script1, String... scriptList) {
        String script = "" + script0 + "\n" + script1 ;
        for(String scriptN : scriptList) {
            script = script + "\n" + scriptN ;
        }
        return eval(script) ;
    }

    //------------------------------------------------------------
    /**
     * script 実行(1行)して Boolean にキャスト
     */
    public Boolean evalBoolean(String script) {
        return (Boolean)eval(script) ;
    }

    /**
     * script 実行(複数行)して Boolean にキャスト
     */
    public Boolean evalBoolean(String script0, String script1,
                               String... scriptList) {
        return (Boolean)eval(script0, script1, scriptList) ;
    }

    //------------------------------------------------------------
    /**
     * script 実行(1行)して Integer にキャスト
     */
    public Integer evalInteger(String script) {
        return (Integer)eval(script) ;
    }

    /**
     * script 実行(複数行)して Integer にキャスト
     */
    public Integer evalInteger(String script0, String script1,
                               String... scriptList) {
        return (Integer)eval(script0, script1, scriptList) ;
    }

    //------------------------------------------------------------
    /**
     * script 実行(1行)して Double にキャスト
     */
    public Double evalDouble(String script) {
        return (Double)eval(script) ;
    }

    /**
     * script 実行(複数行)して Double にキャスト
     */
    public Double evalDouble(String script0, String script1,
                             String... scriptList) {
        return (Double)eval(script0, script1, scriptList) ;
    }

    //------------------------------------------------------------
    /**
     * script 実行(1行)して String にキャスト
     */
    public String evalString(String script) {
        return (String)eval(script) ;
    }

    /**
     * script 実行(複数行)して String にキャスト
     */
    public String evalString(String script0, String script1,
                             String... scriptList) {
        return (String)eval(script0, script1, scriptList) ;
    }

    //------------------------------------------------------------
    /**
     * 変数設定(トップレベル)
     */
    public Object setVariable(String varName, Object value) {
        return container.put(varName, value) ;
    }

    /**
     * 変数設定(オブジェクト内)
     */
    public Object setVariable(Object object, String varName, Object value) {
        return container.put(object, varName, value) ;
    }

    //------------------------------------------------------------
    /**
     * 変数値取得(トップレベル)
     */
    public Object getVariable(String varName){
        return container.get(varName) ;
    }

    /**
     * 変数値取得(オブジェクト内)
     */
    public Object getVariable(Object object, String varName) {
        return container.get(object, varName) ;
    }

    //------------------------------------------------------------
    /**
     * 変数値取得(トップレベル)してBooleanにキャスト
     */
    public Boolean getVariableBoolean(String varName){
        return (Boolean)getVariable(varName) ;
    }

    /**
     * 変数値取得(オブジェクト内)してBooleanにキャスト
     */
    public Boolean getVariableBoolean(Object object, String varName) {
        return (Boolean)getVariable(object, varName) ;
    }

    //------------------------------------------------------------
    /**
     * 変数値取得(トップレベル)してIntegerにキャスト
     */
    public Integer getVariableInteger(String varName){
        return (Integer)getVariable(varName) ;
    }

    /**
     * 変数値取得(オブジェクト内)してIntegerにキャスト
     */
    public Integer getVariableInteger(Object object, String varName) {
        return (Integer)getVariable(object, varName) ;
    }

    //------------------------------------------------------------
    /**
     * 変数値取得(トップレベル)してDoubleにキャスト
     */
    public Double getVariableDouble(String varName){
        return (Double)getVariable(varName) ;
    }

    /**
     * 変数値取得(オブジェクト内)してDoubleにキャスト
     */
    public Double getVariableDouble(Object object, String varName) {
        return (Double)getVariable(object, varName) ;
    }

    //------------------------------------------------------------
    /**
     * 変数値取得(トップレベル)してStringにキャスト
     */
    public String getVariableString(String varName){
        return (String)getVariable(varName) ;
    }

    /**
     * 変数値取得(オブジェクト内)してStringにキャスト
     */
    public String getVariableString(Object object, String varName) {
        return (String)getVariable(object, varName) ;
    }

    //------------------------------------------------------------
    /**
     * メソッド呼び出し(トップレベル)
     */
    public Object callTopMethod(String methodName, Object... args) {
        return container.callMethod(null, methodName, args) ;
    }

    /**
     * メソッド呼び出し(オブジェクト内)
     */
    public Object callMethod(Object object, String methodName,
                             Object... args) {
        return container.callMethod(object, methodName, args) ;
    }

    //------------------------------------------------------------
    /**
     * メソッド呼び出し(トップレベル)してBooleanにキャスト
     */
    public Boolean callTopMethodBoolean(String methodName, Object... args) {
        return (Boolean)callTopMethod(methodName, args) ;
    }

    /**
     * メソッド呼び出し(オブジェクト内)してBooleanにキャスト
     */
    public Boolean callMethodBoolean(Object object, String methodName,
                                     Object... args) {
        return (Boolean)callMethod(object, methodName, args) ;
    }

    //------------------------------------------------------------
    /**
     * メソッド呼び出し(トップレベル)してIntegerにキャスト
     */
    public Integer callTopMethodInteger(String methodName, Object... args) {
        return (Integer)callTopMethod(methodName, args) ;
    }

    /**
     * メソッド呼び出し(オブジェクト内)してIntegerにキャスト
     */
    public Integer callMethodInteger(Object object, String methodName,
                                     Object... args) {
        return (Integer)callMethod(object, methodName, args) ;
    }

    //------------------------------------------------------------
    /**
     * メソッド呼び出し(トップレベル)してDoubleにキャスト
     */
    public Double callTopMethodDouble(String methodName, Object... args) {
        return (Double)callTopMethod(methodName, args) ;
    }

    /**
     * メソッド呼び出し(オブジェクト内)してDoubleにキャスト
     */
    public Double callMethodDouble(Object object, String methodName,
                                   Object... args) {
        return (Double)callMethod(object, methodName, args) ;
    }

    //------------------------------------------------------------
    /**
     * メソッド呼び出し(トップレベル)してStringにキャスト
     */
    public String callTopMethodString(String methodName, Object... args) {
        return (String)callTopMethod(methodName, args) ;
    }

    /**
     * メソッド呼び出し(オブジェクト内)してStringにキャスト
     */
    public String callMethodString(Object object, String methodName,
                                   Object... args) {
        return (String)callMethod(object, methodName, args) ;
    }

    //------------------------------------------------------------
    /**
     * クラスのインスタンス作成
     */
    public Object newInstanceOfClass(String className, Object... args) {
        Object klass = this.eval(className) ;
        return container.callMethod(klass, "new", args) ;
    }

    //------------------------------------------------------------
    /**
     * script のパース。
     * 返された EvalUnit に対して、run() メソッドを呼び出すと、実行される。
     * @return EvalUnit。
     */
    public EvalUnit parseScript(String script) {
        return container.parse(script) ;
    }

    //------------------------------------------------------------
    /**
     * Ruby の LoadPath。
     * @return path のリスト。
     * Note: container.getLoadPaths() というのがあるが、なぜか正しく動かない。
     * なので、ruby の環境での値を直接取るようにする。
     */
    public List<String> getLoadPaths() {
        return (List<String>)this.eval("$LOAD_PATH") ;
    }

    //------------------------------------------------------------
    /**
     * Ruby の LoadPath を設定。
     * @param pathList path のリスト。
     * Note: container.setLoadPaths() というのがあるが、なぜか正しく動かない。
     * なので、ruby の環境で値をセットするようにする。
     */
    public void setLoadPaths(List<String> pathList) {
        this.eval("$LOAD_PATH=[]");
        for(String path : pathList) {
            pushLoadPath(path) ;
        }
    }

    //------------------------------------------------------------
    /**
     * Ruby の LoadPath の末尾に追加。
     * 追加するかどうかチェックする。
     * @param path 追加するpath。
     * @return 追加されれば true。すでに存在して追加されなかった場合は false。
     * Note: container.setLoadPaths() というのがあるが、なぜか正しく動かない。
     * なので、ruby の環境で値をセットするようにする。
     */
    public boolean pushLoadPath(String path) {
        List<String> pathList = this.getLoadPaths() ;
        if(pathList.contains(path)) {
            return false ;
        } else {
            container.callMethod(this.eval("$LOAD_PATH"), "push", path) ;
            return true ;
        }
    }

    //------------------------------------------------------------
    /**
     * Ruby の current directory を取得。
     * @return current directory。
     */
    public String getCurrentDirectory() {
        return container.getCurrentDirectory() ;
    }

    //------------------------------------------------------------
    /**
     * Ruby の current directory を取得。
     * @return current directory。
     */
    public void setCurrentDirectory(String directory) {
        container.setCurrentDirectory(directory) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine がインスタンスを持つか確認。
     * @return 新たにインスタンス生成されたら true。それ以外は false。
     */
    public static boolean ensureRubyEngine() {
        if(rubyEngine == null) {
            rubyEngine = Ruby.newInstance() ;
            return true ;
        } else {
            return false ;
        }
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine 上で直接実行。(一行)
     */
    public static Object evalOnEngine(String script) {
        ensureRubyEngine() ;
        return rubyEngine.evalScriptlet(script) ;
    }

    /**
     * rubyEngine 上で直接実行。(複数行)
     */
    public static Object evalOnEngine(String script0, String script1,
                                      String... scriptList) {
        String script = "" + script0 + "\n" + script1 ;
        for(String scriptN : scriptList) {
            script = script + "\n" + scriptN ;
        }
        return evalOnEngine(script) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine 上で直接実行(一行)してBooleanにキャスト。
     */
    public static Boolean evalBooleanOnEngine(String script) {
        return (Boolean)evalOnEngine(script) ;
    }

    /**
     * rubyEngine 上で直接実行(複数行)してBooleanにキャスト。
     */
    public static Boolean evalBooleanOnEngine(String script0, String script1,
                                              String... scriptList) {
        return (Boolean)evalOnEngine(script0, script1, scriptList) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine 上で直接実行(一行)してIntegerにキャスト。
     */
    public static Integer evalIntegerOnEngine(String script) {
        return (Integer)evalOnEngine(script) ;
    }

    /**
     * rubyEngine 上で直接実行(複数行)してIntegerにキャスト。
     */
    public static Integer evalIntegerOnEngine(String script0, String script1,
                                              String... scriptList) {
        return (Integer)evalOnEngine(script0, script1, scriptList) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine 上で直接実行(一行)してDoubleにキャスト。
     */
    public static Double evalDoubleOnEngine(String script) {
        return (Double)evalOnEngine(script) ;
    }

    /**
     * rubyEngine 上で直接実行(複数行)してDoubleにキャスト。
     */
    public static Double evalDoubleOnEngine(String script0, String script1,
                                              String... scriptList) {
        return (Double)evalOnEngine(script0, script1, scriptList) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine 上で直接実行(一行)してStringにキャスト。
     */
    public static String evalStringOnEngine(String script) {
        return (String)evalOnEngine(script) ;
    }

    /**
     * rubyEngine 上で直接実行(複数行)してStringにキャスト。
     */
    public static String evalStringOnEngine(String script0, String script1,
                                              String... scriptList) {
        return (String)evalOnEngine(script0, script1, scriptList) ;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class Foo

