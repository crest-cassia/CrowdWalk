// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk Template for Java
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/12 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/12]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk ;

import java.lang.ClassNotFoundException ; 
import java.lang.InstantiationException ;
import java.lang.IllegalAccessException ;
import java.lang.reflect.InvocationTargetException ;

import java.lang.reflect.Method;

import java.util.HashMap ;
import java.util.Map ;

import net.arnx.jsonic.JSON ;
import net.arnx.jsonic.JSONException ;

//======================================================================
/**
 * クラスの名前からクラスオブジェクトを探すためのツール
 */
public class ClassFinder {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    static public ClassFinder primal = new ClassFinder() ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * alias を格納しておくテーブル
     */
    public HashMap<String, String> aliasTable =
        new HashMap<String, String>() ;

    //------------------------------------------------------------
    /**
     * クラスオブジェクトを持ってくる。
     * @param className クラスの名前。alias名もしくは fullpath。
     */
    public Class<?> get(String className)
        throws ClassNotFoundException
    {
        return Class.forName(fullname(className)) ;
    }

    //------------------------------------------------------------
    /**
     * クラスオブジェクトを持ってくる。
     * @param className クラスの名前。alias名もしくは fullpath。
     */
    public boolean isClassName(String className) 
    {
        try {
            get(className) ;
            return true ;
        } catch (ClassNotFoundException ex) {
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * alias table を参照しつつ、fullname を探す。
     * もし alias されていなければ、そのまま返す。
     * @param name 探す名前
     */
    public String fullname(String name)
    {
        String fname = aliasTable.get(name) ;
        if(fname == null) {
            return name ;
        } else {
            return fullname(fname) ;
        }
    }

    //------------------------------------------------------------
    /**
     * クラスを見つけて、インスタンスを生成する。
     * @param name クラスの名前
     */
    public Object newByName(String name)
        throws ClassNotFoundException, 
               InstantiationException, 
               IllegalAccessException
    {
        Class<?> klass = get(name) ;
        return klass.newInstance() ;
    }

    //------------------------------------------------------------
    /**
     * alias を登録する。(別名)
     * @param shortName alias 名。
     * @param fullName alias される名前。
     */
    public String alias(String shortName,
                        String fullName) {
        return registerAlias(shortName, fullName) ;
    }

    //------------------------------------------------------------
    /**
     * alias を登録する。
     * @param shortName alias 名。
     * @param fullName alias される名前。
     */
    public String registerAlias(String shortName,
                                String fullName) {
        aliasTable.put(shortName, fullName) ;
        return shortName ;
    }

    //------------------------------------------------------------
    /**
     * alias を登録する。(別名)
     * @param shortName alias 名。
     * @param klass クラスオブジェクト
     */
    public String alias(String shortName,
                        Class<?> klass) {
        return registerAlias(shortName, klass) ;
    }

    //------------------------------------------------------------
    /**
     * alias を登録する。
     * @param shortName alias 名。
     * @param klass クラスオブジェクト
     */
    public String registerAlias(String shortName,
                                Class<?> klass) {
        return registerAlias(shortName, klass.getName()) ;
    }

    //------------------------------------------------------------
    /**
     * JSON で alias をまとめて定義する。
     * @param json JSON 文字列
     */
    public void aliasByJson(String json) {
        Map<String, Object> map = (Map<String, Object>)JSON.decode(json);
        for(Map.Entry<String, Object> entry : map.entrySet()) {
            alias(entry.getKey(), (String)entry.getValue()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * alias table を JSON に治す。
     */
    public String aliasToJson()
        throws JSONException 
    {
        return aliasToJson(false) ;
    }

    //------------------------------------------------------------
    /**
     * alias table を JSON に治す。
     * @param pprint prity print で出力する。
     */
    public String aliasToJson(boolean pprint) 
        throws JSONException 
    {
        return JSON.encode(aliasTable, pprint) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * dummy instance を格納しておくテーブル
     * instance method を呼ぶためのもの。
     * ゼロ引数で Constructor を呼べるものに限る。
     */
    static public HashMap<Class<?>, Object> DummyTable =
        new HashMap<Class<?>, Object>() ;

    //------------------------------------------------------------
    /**
     * class と dummy instance の登録
     */
    public void registerClassDummy(Class<?> klass) {
        _registerClassDummy(klass) ;
    }

    //------------------------------------------------------------
    /**
     * class と dummy instance の登録 (static 版)
     */
    static public void _registerClassDummy(Class<?> klass) {
        try {
            Object object = klass.newInstance() ;
            DummyTable.put(klass, object) ;
        } catch (Exception ex) {
            Itk.dumpStackTraceOf(ex) ;
            Itk.logError("can not register the class:" + klass) ;
        }
    }

    //------------------------------------------------------------
    /**
     * class の dummy instance の取得
     */
    static public Object getClassDummy(Class<?> klass) {
        return _getClassDummy(klass) ;
    }

    //------------------------------------------------------------
    /**
     * class の dummy instance の取得 (static 版)
     */
    static public Object _getClassDummy(Class<?> klass) {
        return DummyTable.get(klass) ;
    }

    //------------------------------------------------------------
    /**
     * クラス名、メソッド名を指定して、dummy object で instance method
     * を呼び出す。
     * (注意)
     * 現状では、引数が primitive type だとうまく行かない。
     * (クラス名が boxing でわからなくなる)
     * @param className クラスの名前。alias 名でも良い。
     * @param methodName メソッド名。
     * @param staticP static メソッドかどうか。
     * @param args 引数。
     */
    public Object callMethodForClass(String className,
                                     String methodName,
                                     boolean staticP,
                                     Object... args)
        throws ClassNotFoundException,
               NoSuchMethodException,
               IllegalAccessException,
               InvocationTargetException
    {
        Class<?> klass = get(className) ;
        return callMethodForClass(klass, methodName, staticP, args) ;
    }
    //------------------------------------------------------------
    /**
     * クラス名、メソッド名を指定して、dummy object で instance method
     * を呼び出す。
     * (注意)
     * 現状では、引数が primitive type だとうまく行かない。
     * (クラス名が boxing でわからなくなる)
     * @param klass クラス。
     * @param methodName メソッド名。
     * @param staticP static メソッドかどうか。
     * @param args 引数。
     */
    public Object callMethodForClass(Class<?> klass,
                                            String methodName,
                                            boolean staticP,
                                            Object... args)
        throws NoSuchMethodException,
               IllegalAccessException,
               InvocationTargetException
    {
        return _callMethodForClass(klass, methodName, staticP, args) ;
    }

    //------------------------------------------------------------
    /**
     * クラス名、メソッド名を指定して、dummy object で instance method
     * を呼び出す。（static 版）
     * (注意)
     * 現状では、引数が primitive type だとうまく行かない。
     * (クラス名が boxing でわからなくなる)
     * @param klass クラス。
     * @param methodName メソッド名。
     * @param staticP static メソッドかどうか。
     * @param args 引数。
     */
    static public Object _callMethodForClass(Class<?> klass,
                                             String methodName,
                                             boolean staticP,
                                             Object... args)
        throws NoSuchMethodException,
               IllegalAccessException,
               InvocationTargetException
    {
        Class<?>[] argClassList = new Class<?>[args.length] ;

        Object instance = (staticP ? null : getClassDummy(klass)) ;

        for(int i = 0 ; i < args.length ; i++) {
            argClassList[i] = args[i].getClass() ;
        }

        Method method = klass.getMethod(methodName,argClassList) ;

        return method.invoke(instance, args) ;
    }

} // class ClassFinder

