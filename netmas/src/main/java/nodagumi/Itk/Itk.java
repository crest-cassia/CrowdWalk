// -*- mode: java -*-
/** Itk Utility
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/12 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/12]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.lang.Thread ;
import java.lang.StackTraceElement ;

import java.util.UUID;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Arrays ;

import java.text.DateFormat ;
import java.text.SimpleDateFormat ;

//======================================================================
/**
 * Itk utility
 */
public class Itk {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デバッグ出力のヘッダ
     */
    final static public String DbgTag = "ITKDBG" ;
    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * @param object 出力するオブジェクト
     */
    static public void dbgMsg(Object object) {
	try {
	    if(object == null) {
		dbgMsg("(null)") ;
	/* XML は当面使わないので、コメントアウト
	    } else if(object instanceof Node) {
		dbgMsg(XMLFormatConverter.toString((Node)object)) ;
	*/
	    } else {
		System.out.println(DbgTag + ":" + objectToString(object)) ;
	    }
	} catch(/*TransformerException*/ Exception ex) {
	    ex.printStackTrace();
	}
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * @param label 先頭に出すラベル
     * @param object 出力するオブジェクト
     */
    static public void dbgMsg(String label, Object object) {
	try {
	    if(object == null) {
		dbgMsg(label, "(null)") ;
	/* XML は当面使わないので、コメントアウト
	    } else if(object instanceof Node) {
		dbgMsg(label, XMLFormatConverter.toString((Node)object)) ;
	*/
	    } else {
		System.out.println(DbgTag + "[" + label + "]:" 
				   + objectToString(object)) ;
	    }
	} catch(/*TransformerException*/ Exception ex) {
	    ex.printStackTrace();
	}
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * 現在のメソッド名を出力する。
     */
    static public void dbgMsgMethodInfo() {
	dbgMsg("=====", currentMethod(1)) ;
    }

    //------------------------------------------------------------
    /**
     * 配列の中身の文字列化
     * @param object 配列オブジェクト
     */
    static public String objectToString(Object object) {
	if(object.getClass().isArray()) {
	    final String className = object.getClass().getName() ;
	    if(className.startsWith("[Z")) {
		return Arrays.toString((boolean[])object) ;
	    } else if (className.startsWith("[B")) {
		return Arrays.toString((byte[])object) ;
	    } else if (className.startsWith("[C")) {
		return Arrays.toString((char[])object) ;
	    } else if (className.startsWith("[D")) {
		return Arrays.toString((double[])object) ;
	    } else if (className.startsWith("[F")) {
		return Arrays.toString((float[])object) ;
	    } else if (className.startsWith("[I")) {
		return Arrays.toString((int[])object) ;
	    } else if (className.startsWith("[J")) {
		return Arrays.toString((long[])object) ;
	    } else if (className.startsWith("[S")) {
		return Arrays.toString((short[])object) ;
	    } else if (className.startsWith("[[")) {
		String ret = null;
		for(Object innerArray : (Object[]) object) {
		    if(ret == null) {
			ret = "" ;
		    } else {
			ret += "," ;
		    }
		    ret += objectToString(innerArray) ;
		}
		ret = "[" + ret + "]" ;
		return ret ;
	    } else {
		return Arrays.toString((Object[])object) ;
	    }
	} else {
	    return object.toString() ;
	}
	//	return object.getClass().getName() ;
    }

    //------------------------------------------------------------
    /**
     * UUID によりランダムな URI を生成
     */
    static public String genUriRandom() {
	UUID uuid = UUID.randomUUID() ;
	return "uri:uuid:" + uuid.toString() ;
    }

    //------------------------------------------------------------
    /**
     * 現在時刻をCalendarで取得
     */
    /*
    static public Calendar getCurrentTimeCalendar() {
	String tz = "JST" ;
	return Calendar.getInstance(TimeZone.getTimeZone(tz)) ;
    }
    */

    //------------------------------------------------------------
    /**
     * 現在時刻をDateで取得
     */
    static public Date getCurrentTimeDate() {
	return new Date() ;
    }

    //------------------------------------------------------------
    /**
     * 現在時刻をTimeで取得
     * (Time class は、java.sql.Time で、使わないので、とりあえず排除)
     */
    /*
    static public Time getCurrentTime() {
	return new Time(getCurrentTimeDate().getTime()) ;
    }
    */

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 時刻文字列のフォーマット
     */
    static public final String DefaultTimeStrFormatPattern 
	= "yyyy-MM-dd HH:mm:ss.SSS" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 時刻フォーマット
     */
    static public final DateFormat DefaultTimeStrFormat 
	= new SimpleDateFormat(DefaultTimeStrFormatPattern) ;

    //------------------------------------------------------------
    /**
     * 現在時刻をStringで取得
     */
    static public String getCurrentTimeStr() {
	return getCurrentTimeStr(DefaultTimeStrFormat) ;
    }

    //------------------------------------------------------------
    /**
     * 現在時刻をStringで取得
     * @param formatPattern 指定のフォーマット
     */
    static public String getCurrentTimeStr(String formatPattern) {
	return getCurrentTimeStr(new SimpleDateFormat(formatPattern)) ;
    }

    //------------------------------------------------------------
    /**
     * 現在時刻をStringで取得
     * @param form 指定のフォーマット
     */
    static public String getCurrentTimeStr(DateFormat form) {
	Date date = getCurrentTimeDate() ;
	return form.format(date) ;
    }

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド情報
     */
    static public StackTraceElement currentCall() {
	return currentCall(1) ;
    }

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド情報
     */
    static public StackTraceElement currentCall(int offset) {
	StackTraceElement[] trace = Thread.currentThread().getStackTrace() ;
	return trace[2 + offset] ;
    }

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド名
     */
    static public String currentMethod() {
	return currentMethod(1) ;
    }

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド名
     */
    static public String currentMethod(int offset) {
	return currentCall(offset + 1).toString() ;
    }

} // class Itk

