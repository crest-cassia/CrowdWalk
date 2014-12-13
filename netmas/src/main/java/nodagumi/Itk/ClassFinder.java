// -*- mode: java -*-
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

import java.util.HashMap ;
import java.util.Map ;

import net.arnx.jsonic.JSON ;
import net.arnx.jsonic.JSONException ;

//======================================================================
/**
 * クラスの名前からクラスオブジェクトを探すためのツール
 */
public class ClassFinder {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * alias を格納しておくテーブル
     */
    static public HashMap<String, String> AliasTable =
	new HashMap<String, String>() ;

    //------------------------------------------------------------
    /**
     * クラスオブジェクトを持ってくる。
     * @param className クラスの名前。alias名もしくは fullpath。
     */
    static public Class<?> get(String className) 
    	throws ClassNotFoundException
    {
	return Class.forName(fullname(className)) ;
    }

    //------------------------------------------------------------
    /**
     * クラスオブジェクトを持ってくる。
     * @param className クラスの名前。alias名もしくは fullpath。
     */
    static public boolean isClassName(String className) 
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
     * @param shortName 探す名前
     */
    static public String fullname(String name)
    {
	String fname = AliasTable.get(name) ;
	if(fname == null) {
	    return name ;
	} else {
	    return fullname(fname) ;
	}
    }

    //------------------------------------------------------------
    /**
     * クラスを見つけて、インスタンスを生成する。
     * @param Name クラスの名前
     */
    static public Object newByName(String name)
	throws ClassNotFoundException, 
	       InstantiationException, 
	       IllegalAccessException
    {
	Class<?> klass = get(name) ;
	return klass.newInstance() ;
    }

    //------------------------------------------------------------
    /**
     * alias を登録する。
     * @param shortName alias 名。
     * @param fullName alias される名前。
     */
    static public String alias(String shortName,
			       String fullName)
    {
	AliasTable.put(shortName, fullName) ;
	return shortName ;
    }

    //------------------------------------------------------------
    /**
     * JSON で alias をまとめて定義する。
     * @param json JSON 文字列
     */
    static public void aliasByJson(String json) {
	Map<String, Object> map = (Map<String, Object>)JSON.decode(json);
	for(Map.Entry<String, Object> entry : map.entrySet()) {
	    alias(entry.getKey(), (String)entry.getValue()) ;
	}
    }

    //------------------------------------------------------------
    /**
     * alias table を JSON に治す。
     */
    static public String aliasToJson()
	throws JSONException 
    {
	return aliasToJson(false) ;
    }

    //------------------------------------------------------------
    /**
     * alias table を JSON に治す。
     * @param pprint prity print で出力する。
     */
    static public String aliasToJson(boolean pprint) 
	throws JSONException 
    {
	return JSON.encode(AliasTable, pprint) ;
    }

} // class ClassFinder

