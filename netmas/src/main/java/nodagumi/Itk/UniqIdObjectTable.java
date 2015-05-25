// -*- mode: java; indent-tabs-mode: nil -*-
/** Unique Id Object Table
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/05/25 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/05/25]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: ... </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.util.ArrayList;
import java.util.HashMap ;
import java.util.Collection ;

//======================================================================
/**
 * ユニークな id の生成機能を持つ id object table
 */
public class UniqIdObjectTable<Klass> extends HashMap<String, Klass> {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Prefix の既定値
     */
    final static public String DefaultIdPrefix = "_" ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Suffix の既定値
     */
    final static public String DefaultIdSuffix = "" ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * ID の数値部分の桁数の既定値
     */
    final static public int DefaultIdDigit = 5 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * id の prefix
     */
    private String idPrefix = DefaultIdPrefix ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * id の suffix
     */
    private String idSuffix = DefaultIdSuffix ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * id の 数値部分の桁数
     */
    private int idDigit = DefaultIdDigit ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * id のフォーマット
     */
    private String idFormatCache = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * id のフォーマット
     */
    private int idCounter = 0 ;

    //------------------------------------------------------------
    /**
     * 初期化
     * @param _baz: about argument baz.
     */
    public UniqIdObjectTable() {
	super() ;
    }

    //------------------------------------------------------------
    /**
     * 初期化
     * @param _baz: about argument baz.
     */
    public UniqIdObjectTable(String prefix, int digit, String suffix) {
	this() ;
	setIdPrefix(prefix) ;
	setIdDigit(digit) ;
	setIdSuffix(suffix) ;
    }

    //------------------------------------------------------------
    /**
     * idPrefix の設定
     * @param prefix : id prefix
     */
    public void setIdPrefix(String prefix) {
	idPrefix = prefix ;
	idFormatCache = null ;
    }

    //------------------------------------------------------------
    /**
     * idSuffix の設定
     * @param suffix : id suffix
     */
    public void setIdSuffix(String suffix) {
	idSuffix = suffix ;
	idFormatCache = null ;
    }

    //------------------------------------------------------------
    /**
     * idDigit の設定
     * @param digit : id 桁数
     */
    public void setIdDigit(int digit) {
	idDigit = digit ;
	idFormatCache = null ;
    }

    //------------------------------------------------------------
    /**
     * id format 生成
     * @param prefix : 前置文字列
     * @param digit : 数字部桁数
     * @param suffix : 後置文字列
     * @return 生成したフォーマット
     */
    public String genIdFormat(String prefix, int digit, String suffix) {
	return (prefix + String.format("%%0%dd", digit) + suffix) ;
    }

    //------------------------------------------------------------
    /**
     * id format の準備
     * @return 最新のフォーマット
     */
    public String idFormat(String prefix, String suffix) {
	if(prefix == null && suffix == null) {
	    // prefix, suffix ともに null なら、cache を使う。
	    if(idFormatCache == null) {
		idFormatCache = genIdFormat(idPrefix, idDigit, idSuffix) ;
	    }
	    return idFormatCache ;
	} else {
	    // prefix, suffix いずれかが non-null なら、一時的なフォーマット
	    prefix = (prefix == null ? idPrefix : prefix) ;
	    suffix = (suffix == null ? idSuffix : suffix) ;
	    return genIdFormat(prefix, idDigit, suffix) ;
	}
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の生成（thread safeでない）
     * @return ユニークな id
     */
    public String getUniqId_Bare() {
	return getUniqId_Bare(null, null) ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の生成（thread safeでない）
     * @param prefix : 一時的な prefix
     * @return ユニークな id
     */
    public String getUniqId_Bare(String prefix) {
	return getUniqId_Bare(prefix, null) ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の生成（thread safeでない）
     * @param prefix : 一時的な prefix
     * @param suffix : 一時的な suffix
     * @return ユニークな id
     */
    public String getUniqId_Bare(String prefix, String suffix) {
	String format = idFormat(prefix, suffix) ;
	String newId ;
	do {
	    newId = String.format(format, idCounter) ;
	    idCounter++ ;
	} while(!isUniqId(newId)) ;
	return newId ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の生成（thread safe)
     * @return ユニークな id
     */
    public String getUniqId() {
	return getUniqId(null, null) ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の生成（thread safe)
     * @param prefix : 一時的な prefix
     * @return ユニークな id
     */
    public String getUniqId(String prefix) {
	return getUniqId(prefix, null) ;
    }

    //------------------------------------------------------------
    /**
     * ユニークな id の生成（thread safe)
     * @param prefix : 一時的な prefix
     * @param suffix : 一時的な suffix
     * @return ユニークな id
     */
    public String getUniqId(String prefix, String suffix) {
	synchronized(this) {
	    return getUniqId_Bare(prefix, suffix) ;
	}
    }

    //------------------------------------------------------------
    /**
     * ユニークな id チェック
     * @param id : 調べる id
     * @return ユニークならば true ;
     */
    public boolean isUniqId(String id) {
	return !(this.containsKey(id)) ;
    }

    //------------------------------------------------------------
    /**
     * オブジェクト登録。
     * 重複があれば警告。
     * @param id : キーとなる id。
     * @param object : 登録するデータ。
     * @return object を返す。
     */
    public Klass put(String id, Klass object) {
	if(!isUniqId(id)) {
	    Itk.logWarn("duplicated ID", "id=", id, "object=", object) ;
	}
	return super.put(id, object) ;
    }

    //------------------------------------------------------------
    /**
     * unique な id でオブジェクト登録。
     * @param object : 登録するデータ。
     * @return id を返す。
     */
    public String putWithUniqId(Klass object) {
	return putWithUniqId(object, null, null) ;
    }

    //------------------------------------------------------------
    /**
     * unique な id でオブジェクト登録。
     * @param object : 登録するデータ。
     * @param prefix : id prefix
     * @return id を返す。
     */
    public String putWithUniqId(Klass object, String prefix) {
	return putWithUniqId(object, prefix, null) ;
    }

    //------------------------------------------------------------
    /**
     * unique な id でオブジェクト登録。
     * @param object : 登録するデータ。
     * @param prefix : id prefix
     * @param suffix : id suffix
     * @return id を返す。
     */
    public String putWithUniqId(Klass object, String prefix, String suffix) {
	synchronized(this) {
	    String id = getUniqId_Bare(prefix, suffix) ;
	    put(id, object) ;
	    return id ;
	}
    }

} // class UniqIdObjectTable

