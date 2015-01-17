// -*- mode: java; indent-tabs-mode: nil -*-
/** Term (項) utility 
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/26 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/26]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.lang.StringBuffer;
import java.lang.Thread;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.math.BigDecimal;

import net.arnx.jsonic.JSON ;
import net.arnx.jsonic.JSONWriter;
import nodagumi.Itk.*;

//======================================================================
/**
 * 項 クラス。
 * 項(Term) は、アトム（シンボルおよび数値）・配列(Array)もしくは
 * 連想配列(Object)からなる。
 * Prolog の項に相当。ただし文字列はシンボルと同じとする。
 * Object のうち、空文字列("")  をキー(slot)とする値(value)があるとき、
 * それを head とする。head 以外の slot がない場合、それはアトムと同じ
 * 同じ扱いとする。
 *
 * JSON で表すときには、
 *     {"":<head>, <slot1>:<value1>, <slot2>:<value2>...} 
 * と表記する。
 * 引数のない場合は、単なる<head>だけの文字列と等価。
 * すなわち、
 *     {"":<head>} == <head>
 * 配列は、
 *     [<value>,<value>,...]
 *
 * CSV の中での表記では、
 *     <head>(<slot1>:<value1>,<slot2>:<value2>...)
 * とする。ただしこの表記方法は obsolete。将来の保証はない。
 *
 * また、特例として、slot 名のない表記
 *     <head>(<value1>,<value2>...)
 * は以下と同じとみなす。
 *     <head>("1":<value1>,"2":<value2>...)
 * CSV形式で配列はない。
 * 上と同じく、引数なしは引数ゼロと同じ。
 *     <head>() == <head>
 *
 * <head> の無い (nullである) Objectを許す。
 *
 * <head> も <body> もない項は null と同じとみなす。
 */
public class Term {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * head を示すスロット名。
     */
    static private final String HeadSlot = "" ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * body の始まりと終わり、区切り、スロットと値の境
     */
    static private final String BodyBeginChar = "(" ;
    static private final String BodyEndChar = ")" ;
    static private final String ArgSepChar = "," ;
    static private final String SlotSepChar = ":" ;
    static private final String ArrayBeginChar = "[" ;
    static private final String ArrayEndChar = "]" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * head of term
     */
    private Object head = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * body
     */
    private HashMap<String, Object> body = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * array
     */
    private List<Object> array = null ;

    //------------------------------------------------------------
    /**
     * コンストラクタ（無引数）
     */
    public Term() {
        clear() ;
    } ;

    //------------------------------------------------------------
    /**
     * コンストラクタ（headのみ）
     * うまく行かない場合があるので、タイプにより分類。
     */
    public Term(Object _head) {
        if(_head instanceof List) {
            setArray((List<Object>)_head) ;
        } else if (_head instanceof HashMap) {
            setBody((HashMap<String,Object>)_head) ;
        } else {
            setHead(_head) ;
        }
    } ;

    //------------------------------------------------------------
    /**
     * コンストラクタ（bodyのみ）
     */
    public Term(HashMap<String, Object> _body) {
        setBody(_body) ;
    } ;

    //------------------------------------------------------------
    /**
     * コンストラクタ（head,args）
     */
    public Term(Object _head, HashMap<String, Object> _body) {
        setBody(_body) ;
        setHead(_head) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ（head,args）
     */
    public Term(List<Object> _array) {
        setArray(_array) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * new Array Term
     */
    static public Term newArrayTerm() {
        Term term = new Term() ;
        term.allocArray() ;
        return term ;
    }

    //------------------------------------------------------------
    /**
     * 全クリア
     */
    public Term clear() {
        clearHeadBody(false) ;
        clearArray(false) ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * body の確保
     */
    public HashMap<String, Object> allocBody() {
        body = new HashMap<String, Object>() ;

        if(!isNullHead()) setHeadInBody(head) ;

        clearArray(true) ;

        return body ;
    }

    //------------------------------------------------------------
    /**
     * body/head のために array をクリア。
     */
    private void clearArray(boolean warningP) {
        if(warningP && !isNullArray()) {
            Itk.dbgWrn("set/alloc Head/Body to Term with Array.") ;
            Itk.dbgMsg("Discard the Array.") ;
            Itk.dbgMsg("array",array) ;
        }

        array = null ;
    }

    //------------------------------------------------------------
    /**
     * array の確保
     */
    public List<Object> allocArray() {
        array = new ArrayList<Object>() ;

        clearHeadBody(true) ;

        return array ;
    }

    //------------------------------------------------------------
    /**
     * array のために head と body をクリア。
     */
    private void clearHeadBody(boolean warningP) {
        if(warningP) {
            if(!isNullHead()) {
                Itk.dbgWrn("set or alloc array to Term with Head.");
                Itk.dbgMsg("Discard the Head.") ;
                Itk.dbgMsg("head",head) ;
            }
            if(!isNullBody()) {
                Itk.dbgWrn("set or alloc array to Term with Body.") ;
                Itk.dbgMsg("Discard the Body.") ;
                Itk.dbgMsg("body",body) ;
            }
        }
        head = null ;
        body = null ;
    }

    //------------------------------------------------------------
    /**
     * null head 判定
     */
    public boolean isNullHead() {
        return (head == null) ;
    }

    //------------------------------------------------------------
    /**
     * null body 判定 (body が null かどうか)
     */
    public boolean isNullBody() {
        return (body == null) ;
    }

    //------------------------------------------------------------
    /**
     * null array 判定 (array が null かどうか)
     */
    public boolean isNullArray() {
        return (array == null) ;
    }

    //------------------------------------------------------------
    /**
     * zero args 判定 (引数がゼロかどうか)
     * Array の場合は false 
     */
    public boolean isZeroArgs() {
        return (isNullArray() &&
                (isNullBody() ||
                 (body.size() == 0) ||
                 (body.size() == 1 && getArg(HeadSlot) != null))) ;
    }

    //------------------------------------------------------------
    /**
     * head 取得
     */
    public Object getHead() {
        return head ;
    }

    //------------------------------------------------------------
    /**
     * head 取得
     */
    public String getHeadString() {
        return (String)getHead() ;
    }

    //------------------------------------------------------------
    /**
     * head 取得 (in body)
     */
    private Object getHeadInBody() {
        return body.get(HeadSlot) ;
    }

    //------------------------------------------------------------
    /**
     * head 設定
     */
    public Term setHead(Object _head) {
        return setHead(_head, true) ;
    }

    //------------------------------------------------------------
    /**
     * head 設定
     */
    public Term setHead(Object _head, boolean setInBody) {
        if(_head instanceof Term) {
            setHead(((Term)_head).getHead(), setInBody) ;
        } else {
            head = _head ;

            if(setInBody) setHeadInBody(_head) ;

            clearArray(true) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * head 設定 (in body)
     */
    public Term setHeadInBody(Object _head) {
        if(!isNullBody()) {
            if(checkBareValue(_head)) _head = new Term(_head) ;
            body.put(HeadSlot, _head) ;
            return this ;
        } else {
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * body 取得
     */
    public HashMap<String,Object> getBody() {
        return body ;
    }

    //------------------------------------------------------------
    /**
     * arg チェック
     */
    public boolean hasArg(String slot) {
        if(isNullBody()) {
            return false ;
        } else {
            return body.containsKey(slot) ;
        }
    }

    //------------------------------------------------------------
    /**
     * arg 取得
     */
    public Object getArg(String slot) {
        if(isNullBody()) {
            return null ;
        } else {
            return body.get(slot) ;
        }
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (Term)
     */
    public Term getArgTerm(String slot) {
        return convertValueToTerm(getArg(slot)) ;
    }

    private Term convertValueToTerm(Object val) {
        if(val instanceof Term)
            return (Term)val ;
        else if(val == null)
            return null ;
        else {
            Itk.dbgWrn("can not cast to Term:" + val) ;
            Itk.dbgMsg("generage new Term.") ;
            return new Term(val) ;
        }
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (String)
     */
    public String getArgString(String slot) {
        return convertValueToString(getArg(slot)) ;
    }

    private String convertValueToString(Object val) {
        if(val == null) {
            return null ;
        } else if(val instanceof Term) {
            return ((Term)val).getString() ;
        } else {
            return val.toString() ;
        }
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (boolean)
     */
    public boolean getArgBoolean(String slot) {
        return convertValueToBoolean(getArg(slot)) ;
    }

    private boolean convertValueToBoolean(Object val) {
        if(val instanceof Term) {
            return Boolean.valueOf(((Term)val).getString()) ;
        } else if(val instanceof Boolean) {
            return (Boolean)val ;
        } else if(val instanceof String) {
            return Boolean.valueOf((String)val) ;
        } else if(val == null) {
            return false ;
        } else {
            Thread.dumpStack() ;
            Itk.dbgErr("can not convert to boolean:" + this.toString()) ;
            Itk.dbgMsg("use false") ;
            return false ;
        } 
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (int)
     */
    public int getArgInt(String slot) {
        return convertValueToInt(getArg(slot)) ;
    }

    private int convertValueToInt(Object val) {
        if(val instanceof Term) {
            return ((Term)val).getInt() ;
        } else if(val == null) {
            Thread.dumpStack() ;
            Itk.dbgErr("can not convert null to int.") ;
            Itk.dbgMsg("use zero") ;
            return 0 ;
        } else {
            Thread.dumpStack() ;
            Itk.dbgErr("can not convert to int:" + this.toString()) ;
            Itk.dbgMsg("use zero") ;
            return 0 ;
        } 
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (double)
     */
    public double getArgDouble(String slot) {
        return convertValueToDouble(getArg(slot)) ;
    }

    private double convertValueToDouble(Object val) {
        if(val instanceof Term) {
            return ((Term)val).getDouble() ;
        } else if(val == null) {
            Thread.dumpStack() ;
            Itk.dbgErr("can not convert null to double.") ;
            Itk.dbgMsg("use zero") ;
            return 0.0 ;
        } else {
            Thread.dumpStack() ;
            Itk.dbgErr("can not convert to double:" + this.toString()) ;
            Itk.dbgMsg("use zero") ;
            return 0.0 ;
        } 
    }

    //------------------------------------------------------------
    /**
     * body 設定
     */
    public Term setBody(HashMap<String,Object> _body) {
        return setBody(_body, true) ;
    }

     //------------------------------------------------------------
     /**
      * body 設定
      */
    public Term setBody(HashMap<String,Object> _body, boolean deepP) {
        body = _body ;

        if(!isNullBody()) {
            if(deepP) {
                for(Map.Entry<String,Object> entry : body.entrySet()) {
                    setArg(entry.getKey(),entry.getValue(), deepP) ;
                }
            }
            clearArray(true) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * arg 設定
     */
    public Term setArg(String slot, Object value){
        return setArg(slot, value, true) ;
    }

    //------------------------------------------------------------
    /**
     * arg 設定
     */
    public Term setArg(String slot, Object value, boolean deepP) {
        if(isNullBody()) allocBody() ;

        if(deepP){ value = letTermedValue(value, deepP) ; }

        if(HeadSlot.equals(slot)) { setHead(value) ; }

        body.put(slot, value) ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * array 取得
     */
    public List<Object> getArray() {
        return array ;
    }

    //------------------------------------------------------------
    /**
     * 型指定の array 取得 (copy が生じる)
     */
    public <T> List<T> getTypedArray() {
        List<T> ret = new ArrayList<T>() ;
        for(Object element  : array) {
            ret.add((T)element) ;
        }
        return ret ;
    }

    //------------------------------------------------------------
    /**
     * array nth 取得
     */
    public Object getNth(int index) {
        if(isNullArray()) {
            return null ;
        } else {
            return (Object)array.get(index) ;
        }
    }

    //------------------------------------------------------------
    /**
     * array nth 取得 (Term)
     */
    public Term getNthTerm(int index) {
        return convertValueToTerm(getNth(index)) ;
    }

    //------------------------------------------------------------
    /**
     * array nth 取得 (String)
     */
    public String getNthString(int index) {
        return convertValueToString(getNth(index)) ;
    }

    //------------------------------------------------------------
    /**
     * array nth 取得 (int)
     */
    public int getNthInt(int index) {
        return convertValueToInt(getNth(index)) ;
    }

    //------------------------------------------------------------
    /**
     * array nth 取得 (double)
     */
    public double getNthDouble(int index) {
        return convertValueToDouble(getNth(index)) ;
    }

    //------------------------------------------------------------
    /**
     * array 設定
     */
    public Term setArray(List<Object> _array) {
        return setArray(_array, true) ;
    }


    //------------------------------------------------------------
    /**
     * array 設定
     */
    public Term setArray(List<Object> _array, boolean deepP) {
        array = _array ;

        if(!isNullArray()) {
            if(deepP) {
                for(int index = 0 ; index < array.size() ; index++) {
                    setNth(index, getNth(index), deepP) ;
                }
            }
            clearHeadBody(true) ;
        }

        return this ;
    }

    //------------------------------------------------------------
    /**
     * arg 設定
     */
    public Term setNth(int index, Object value){
        return setNth(index, value, true) ;
    }

    //------------------------------------------------------------
    /**
     * arg 設定
     */
    public Term setNth(int index, Object value, boolean deepP) {
        if(isNullArray()) allocArray() ;

        if(deepP){ value = letTermedValue(value, deepP) ; }

        array.set(index, value) ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * check the value is bare data (not Term or null)
     */
    private boolean checkBareValue(Object value) {
        return !((value instanceof Term) || value == null) ;
    }

    //------------------------------------------------------------
    /**
     * Term 準拠の Value にする。
     */
    public Object letTermedValue(Object value, boolean deepP) {
        if(checkBareValue(value)) {
            value = newByScannedJson(value, deepP) ;
        }
        return value ;
    }

    //------------------------------------------------------------
    /**
     * 等価判定
     * head も body も null なら null と等しい。
     * head = null, body = {} は、null とは等しくなく、 {} と等しい。
     */
    @Override
    public boolean equals(Object object) {
        if(object == null) {
            return isNull() ;
        } else if(object instanceof HashMap) {
            return equalToBody(object) ;
        } else if(object instanceof List) {
            return equalToArray(object) ;
        } else if(object instanceof Term) {
            Term term = (Term)object ;
            if(equalToHead(term.getHead())){
                if(isArray()) {
                    return equalToArray(term.getArray()) ;
                } else {
                    return equalToBody(term.getBody()) ;
                }
            } else {
                return false ;
            }
        } else {
            return (isAtom() && equalToHead(object)) ;
        }
    }

    //------------------------------------------------------------
    /**
     * Headとの比較
     */
    public boolean equalToHead(Object _head) {
        return (isNullHead() ?
                _head == null :
                getHead().equals(_head)) ;
    }

    //------------------------------------------------------------
    /**
     * Bodyとの比較
     */
    public boolean equalToBody(Object _body) {
        return (isNullBody() ?
                _body == null :
                getBody().equals(_body)) ;
    }

    //------------------------------------------------------------
    /**
     * Bodyとの比較
     */
    public boolean equalToArray(Object _array) {
        return (isNullArray() ?
                _array == null :
                getArray().equals(_array)) ;
    }

    //------------------------------------------------------------
    /**
     * nullかどうか？
     */
    public boolean isNull() {
        return (!isArray() && isNullHead() && isNullBody()) ;
    }

    //------------------------------------------------------------
    /**
     * String となるか？
     */
    public boolean isString() {
        return ((head instanceof String) && isAtom()) ;
    }

    //------------------------------------------------------------
    /**
     * int となるか？
     */
    public boolean isInt() {
        return (((head instanceof Number) || (head instanceof BigDecimal)) &&
                isAtom()) ;
    }

    //------------------------------------------------------------
    /**
     * double となるか？
     */
    public boolean isDouble() {
        return (((head instanceof Number) || (head instanceof BigDecimal)) &&
                isAtom()) ;
    }

    //------------------------------------------------------------
    /**
     * 実効的なbodyのない Term (= Atom) か？
     */
    public boolean isAtom() {
        return isZeroArgs() ;
    }

    //------------------------------------------------------------
    /**
     * 実効的なbodyのない Term (= Atom) か？
     */
    public boolean isArray() {
        return array != null ;
    }

    //------------------------------------------------------------
    /**
     * 実効的なbodyのない Term (= Atom) か？
     */
    public boolean isObject() {
        return hasBody() ;
    }

    //------------------------------------------------------------
    /**
     * 実効的なbody を持つか？
     */
    public boolean hasBody() {
        return !isZeroArgs() ;
    }

    //------------------------------------------------------------
    /**
     * String としての値。
     */
    public String getString() {
        if(isString()) {
            return (String)getHead() ;
        } else if (isNull()) {
            return null ;
        } else {
            Thread.dumpStack() ;
            Itk.dbgErr("can not convert to String:" + this.toString()) ;
            Itk.dbgMsg("use null.") ;
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * int としての値。
     */
    public int getInt() {
        if(isInt()) {
            if (head instanceof Number) {
                return ((Number)head).intValue() ;
            } else if (head instanceof BigDecimal) {
                return ((BigDecimal)head).intValue() ;
            }
        }
        Thread.dumpStack() ;
        Itk.dbgErr("can not convert to int:" + this.toString()) ;
        Itk.dbgMsg("use 0.") ;
        return 0 ;
    }

    //------------------------------------------------------------
    /**
     * double としての値。
     */
    public double getDouble() {
        if(isDouble()) {
            if (head instanceof Number) {
                return ((Number)head).doubleValue() ;
            } else if(head instanceof BigDecimal) {
                return ((BigDecimal)head).doubleValue() ;
            }
        }
        Thread.dumpStack() ;
        Itk.dbgErr("can not convert to double:" + this.toString()) ;
        Itk.dbgMsg("use 0.0") ;
        return 0.0 ;
    }

    //------------------------------------------------------------
    /**
     * 文字列変換
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer() ;
        if(isArray()) {
            buffer.append(ArrayBeginChar) ;
            for(int index = 0 ; index < array.size() ; index++) {
                if(index > 0) buffer.append(ArgSepChar) ;
                buffer.append(getNth(index)) ;
            }
            buffer.append(ArrayEndChar) ;
        } else {
            if(!isNullHead()) buffer.append(getHead()) ;
            if(!isZeroArgs()) {
                buffer.append(BodyBeginChar) ;
                int argn = 0 ;
                for(Map.Entry<String,Object> entry : getBody().entrySet()) {
                    if(!HeadSlot.equals(entry.getKey())) {
                        if(argn > 0) buffer.append(ArgSepChar) ;
                        argn++ ;
                        buffer.append(entry.getKey()) ;
                        buffer.append(SlotSepChar) ;
                        buffer.append(entry.getValue()) ;
                    }
                }
                buffer.append(BodyEndChar) ;
            }
        }
        return buffer.toString() ;
    }

    //============================================================
    /**
     * JSON 変換用のプロセッサー。
     * 将来、multithread で読み込み・書き出しする場合、
     * Thread ごとに用意する必要があるかもしれない。
     */
    static private JSON jsonProcessor = new JSON() ;

    //------------------------------------------------------------
    /**
     * JSON文字列変換 (1行)
     */
    public String toJson() { return toJson(false) ; }

    //------------------------------------------------------------
    /**
     * JSON 文字列への変換 （prity print 可能）
     */
    public String toJson(boolean pprintP) {
        try {
            jsonProcessor.setPrettyPrint(pprintP) ;
            StringBuffer buffer = new StringBuffer() ;
            JSONWriter writer = jsonProcessor.getWriter(buffer) ;

            toJson_Body(writer) ;
            writer.flush() ;

            return buffer.toString() ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.dbgErr("error in converting to JSON.") ;
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     */
    private JSONWriter toJson_Body(JSONWriter writer) throws Exception {
        if(isNull()) {
            writer.value(null) ;
        } else if(isAtom()) {
            writer.value(getHead()) ;
        } else if(isArray()) {
            toJson_List(writer, getArray()) ;
        } else { // 実効的な body がある場合。
            toJson_Object(writer) ;
        }
        return writer ;
    }

    //------------------------------------------------------------
    /**
     */
    private JSONWriter toJson_Object(JSONWriter writer)
        throws Exception 
    {
        writer.beginObject() ;
        for(Map.Entry<String,Object> entry : getBody().entrySet()) {
            Object value = entry.getValue() ;
            writer.name(entry.getKey()) ;
            toJson_Any(writer, value) ;
        }
        writer.endObject() ;
        return writer ;
    }

    //------------------------------------------------------------
    /**
     */
    private JSONWriter toJson_Any(JSONWriter writer, Object value)
        throws Exception 
    {
        if(value instanceof Term) {
            Term term = (Term)value ;
            if(term.isArray())
                toJson_List(writer, term.getArray()) ;
            else
                term.toJson_Body(writer) ;
        } else if (value instanceof List) {
            toJson_List(writer, (List)value) ;
        } else {
            writer.value(value) ;
        }
        return writer ;
    }

    //------------------------------------------------------------
    /**
     */
    private JSONWriter toJson_List(JSONWriter writer, List list) 
        throws Exception 
    {
        writer.beginArray() ;
        for(Object element : list) {
            toJson_Any(writer, element) ;
        }
        writer.endArray() ;
        return writer ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * new Term from JSON
     */
    static public Term newByJson(String jsonString) {
        return newByJson(jsonString, true) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * new Term from JSON
     */
    static public Term newByJson(String jsonString, boolean deepP) {
        Term term = new Term() ;
        term.scanJson(jsonString, deepP) ;
        return term ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * new Term from scanned JSON
     */
    static public Term newByScannedJson(Object json, boolean deepP) {
        Term term = new Term() ;
        term.setScannedJson(json, deepP) ;
        return term ;
    }

    //------------------------------------------------------------
    /**
     * scan JSON
     */
    public Term scanJson(String jsonString, boolean deepP) {
        Object json = JSON.decode(jsonString) ;
        setScannedJson(json, deepP) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * set scanned JSON
     */
    public Term setScannedJson(Object json, boolean deepP) {
        clear() ;

        if(json == null) {
        } else if(json instanceof HashMap) {
            setBody((HashMap<String, Object>)json, deepP) ;
        } else if(json instanceof List) {
            setArray((List<Object>)json, deepP) ;
        } else {
            setHead(json) ;
        }
        return this ;
    }


} // class Term
