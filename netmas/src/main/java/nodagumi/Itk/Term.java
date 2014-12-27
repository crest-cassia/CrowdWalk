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

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import java.math.BigDecimal;

import net.arnx.jsonic.JSON ;
import net.arnx.jsonic.JSONWriter;
import nodagumi.Itk.*;

//======================================================================
/**
 * 項 クラス
 * 項(Term) は、シンボルもしくは引数を持つシンボルとする。
 * 引数は、順序のある配列ではなく、連想配列(slot-value 対)の形とする。
 *
 * JSON で表すときには、
 *     {"":<head>, <slot1>:<value1>, <slot2>:<value2>...} 
 * と表記する。引数のない場合は、単なる<head>だけの文字列と等価とする。
 * すなわち、
 *     {"":<head>} == <head>
 *
 * CSV の中での表記では、
 *     <head>(<slot1>:<value1>,<slot2>:<value2>...)
 * とする。ただしこれは obsolete。
 *
 * また、特例として、slot 名のない表記
 *     <head>(<value1>,<value2>...)
 * は以下と同じとみなす。
 *     <head>("1":<value1>,"2":<value2>...)
 *
 * 上と同じく、引数なしは引数ゼロと同じ。
 *     <head>() == <head>
 *
 * <head> の無い (nullである) 項を許す。
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

    //------------------------------------------------------------
    /**
     * コンストラクタ（無引数）
     */
    public Term() {} ;

    //------------------------------------------------------------
    /**
     * コンストラクタ（headのみ）
     */
    public Term(Object _head) {
        setHead(_head) ;
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
     * head 取得
     */
    public Object getHead() {
        return head ;
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
     * null head 判定
     */
    public boolean isNullHead() {
        return (head == null) ;
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

        if(!isNullBody() && deepP) {
            for(Map.Entry<String,Object> entry : body.entrySet()) {
                setArg(entry.getKey(),entry.getValue(), deepP) ;
            }
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
            if(value instanceof List) {
                if(deepP) {
                    List<Object> list = (List<Object>)value ;
                    for(int i = 0 ; i < list.size() ; i++) {
                        list.set(i, letTermedValue(list.get(i), deepP)) ;
                    }
                }
            } else {
                value = newByScannedJson(value, deepP) ;
            }
        }
        return value ;
    }

    //------------------------------------------------------------
    /**
     * body の確保
     */
    public HashMap<String, Object> allocBody() {
        body = new HashMap<String, Object>() ;
        if(!isNullHead()) setHeadInBody(head) ;
        return body ;
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
     * zero args 判定 (引数がゼロかどうか)
     */
    public boolean isZeroArgs() {
        return (isNullBody() ||
                (body.size() == 0) ||
                (body.size() == 1 && getArg(HeadSlot) != null)) ;
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
            return false ;
        } else if(object instanceof Term) {
            Term term = (Term)object ;
            if(equalToHead(term.getHead())){
                return equalToBody(term.getBody()) ;
            } else {
                return false ;
            }
        } else {
            return (equalToHead(object) && isZeroArgs()) ;
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
     * nullかどうか？
     */
    public boolean isNull() {
        return (isNullHead() && isNullBody()) ;
    }

    //------------------------------------------------------------
    /**
     * String となるか？
     */
    public boolean isString() {
        return ((head instanceof String) && isZeroArgs()) ;
    }

    //------------------------------------------------------------
    /**
     * int となるか？
     */
    public boolean isInt() {
        return (((head instanceof Number) || (head instanceof BigDecimal)) &&
                isZeroArgs()) ;
    }

    //------------------------------------------------------------
    /**
     * double となるか？
     */
    public boolean isDouble() {
        return (((head instanceof Number) || (head instanceof BigDecimal)) &&
                isZeroArgs()) ;
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
        } else {
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
        return buffer.toString() ;
    }

    //============================================================
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
            ((Term)value).toJson_Body(writer) ;
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
        head = null ;
        body = null ;

        if(json == null) {
        } else if(json instanceof HashMap) {
            setBody((HashMap<String, Object>)json, deepP) ;
        } else if(json instanceof List) {
            Itk.dbgErr("Illegal JSON as Term" + json) ;
            return null ;
        } else {
            setHead(json) ;
        }
        return this ;
    }


} // class Term
