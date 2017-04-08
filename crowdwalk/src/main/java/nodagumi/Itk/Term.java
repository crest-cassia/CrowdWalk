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
import java.lang.Exception;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import java.math.BigDecimal;
import java.math.BigInteger;

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
 *     {"":_head_, _slot1_:_value1_, _slot2_:_value2_...} 
 * と表記する。
 * 引数のない場合は、単なる_head_だけの文字列と等価。
 * すなわち、
 *     {"":_head_} == _head_
 * 配列は、
 *     [_value_,_value_,...]
 *
 * CSV の中での表記では、
 *     _head_(_slot1_:_value1_,_slot2_:_value2_...)
 * とする。ただしこの表記方法は obsolete。将来の保証はない。
 *
 * また、特例として、slot 名のない表記
 *     _head_(_value1_,_value2_...)
 * は以下と同じとみなす。
 *     _head_("1":_value1_,"2":_value2_...)
 * CSV形式で配列はない。
 * 上と同じく、引数なしは引数ゼロと同じ。
 *     _head_() == _head_
 *
 * _head_ の無い (nullである) Objectを許す。
 *
 * _head_ も _body_ もない項は null と同じとみなす。
 */
public class Term {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * head を示すスロット名。
     */
    static private final String HeadSlot = ("").intern() ;

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
    // コンストラクタ
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
     * コンストラクタ（int の headのみ）
     * うまく行かない場合があるので、タイプにより分類。
     */
    public Term(int _head) {
        setHead(new Integer(_head)) ;
    } ;

    //------------------------------------------------------------
    /**
     * コンストラクタ（double の headのみ）
     * うまく行かない場合があるので、タイプにより分類。
     */
    public Term(double _head) {
        setHead(new Double(_head)) ;
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
     * new Object Term
     */
    static public Term newObjectTerm() {
        Term term = new Term() ;
        term.allocBody() ;
        return term ;
    }

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
    // 構造基本操作
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
            Itk.logWarn("set/alloc Head/Body to Term with Array.") ;
            Itk.logWarn_("Discard the Array.") ;
            Itk.logWarn_("array",array) ;
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
                Itk.logWarn("set or alloc array to Term with Head.");
                Itk.logWarn_("Discard the Head.") ;
                Itk.logWarn_("head",head) ;
            }
            if(!isNullBody()) {
                Itk.logWarn("set or alloc array to Term with Body.") ;
                Itk.logWarn_("Discard the Body.") ;
                Itk.logWarn_("body",body) ;
            }
        }
        head = null ;
        body = null ;
    }

    //------------------------------------------------------------
    // 判定
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
    // Head 操作
    //------------------------------------------------------------
    /**
     * head 取得
     */
    public Object getHead() {
        return head ;
    }

    //------------------------------------------------------------
    /**
     * head 取得(String)
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
            if(_head instanceof String) {
                head = ((String)_head).intern() ;
            } else {
                head = _head ;
            }

            if(setInBody) setHeadInBody(head) ;

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
    // Body/Arg 操作
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
     * arg チェック(fallback をたどる)
     */
    public boolean hasArg(String slot, String fallbackSlot) {
        if(isNullBody()) {
            return false ;
        } else if(body.containsKey(slot)) {
            return true ;
        } else if(body.containsKey(fallbackSlot)) {
            return getArgTerm(fallbackSlot).hasArg(slot,fallbackSlot) ;
        } else {
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * arg のslot名集合の取得。
     */
    public Set<String> getArgSlotSet() {
        if(isObject()) {
            return getBody().keySet() ;
        } else {
            return null ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * arg 集合の取得。
     */
    public Set<Map.Entry<String,Object>> getArgSet() {
        if(isObject()) {
            return getBody().entrySet() ;
        } else {
            return null ;
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
     * arg 取得(fallback 付き)
     */
    public Object fetchArg(String slot, String fallbackSlot, Object fallback) {
        if(hasArg(slot)) {
            return getArg(slot) ;
        } else if(hasArg(fallbackSlot)) {
            Term fallbackTerm = getArgTerm(fallbackSlot) ;
            return fallbackTerm.fetchArg(slot, fallbackSlot, fallback) ;
        } else {
            return fallback ;
        }
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (Term)
     */
    public Term getArgTerm(String slot) {
        return convertValueToTerm(getArg(slot)) ;
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (Term) (fallback 付き)
     */
    public Term fetchArgTerm(String slot, String fallbackSlot, Term fallback) {
        return convertValueToTerm(fetchArg(slot, fallbackSlot, fallback)) ;
    }

    //------------------------------
    /**
     * Term への変換
     */
    private Term convertValueToTerm(Object val) {
        if(val instanceof Term)
            return (Term)val ;
        else if(val == null)
            return null ;
        else {
            Itk.logWarn("can not cast to Term:" + val) ;
            Itk.logWarn_("generage new Term.") ;
            return new Term(val) ;
        }
    }

    //------------------------------------------------------------
    /**
     * arg を filter(fallback 付き) して取り出す。
     * 例えば、
     * <pre>
     * { "a" : { "x" : 1, "y" : 2 },
     *   "b" : { "u" : 3, "v" : 4 },
     *   "_fallback" : { "a" : { "y" : 5, "z" : 6 }, 
     *                   "b" : { "u" : 7, "w" : 8 }}}
     * </pre>
     * から、"a" で filter すると、以下を構築して返す。
     * <pre>
     * { "x" : 1, "y" : 2, "_fallback" : { "y" : 5, "z" : 6 } }
     * </pre>
     * @param slot : filter するスロット
     * @param fallbackSlot : fallback で潜っていくスロット
     * @return filter された結果。必ずコピーされる。
     */
    public Term filterArgTerm(String slot, String fallbackSlot) {
        Term filteredTerm = Term.newObjectTerm() ;
        if(hasArg(slot)) {
            filteredTerm.updateObjectFacile(getArgTerm(slot),false) ;
        }
        if(hasArg(fallbackSlot)) {
            Term fallbackTerm = getArgTerm(fallbackSlot) ;
            Term filteredFallback =
                fallbackTerm.filterArgTerm(slot, fallbackSlot) ;
            filteredTerm.setArg(fallbackSlot, filteredFallback) ;
        }
        return filteredTerm ;
    }

    //------------------------------------------------------------
    /**
     * fallback 部分について、Term を単一化（フラット化）する。
     * 例えば、
     * <pre>
     * { "a" : { "x" : 1, "y" : 2 },
     *   "b" : { "u" : 3, "v" : 4 },
     *   "_fallback" : { "a" : { "y" : 5, "z" : 6 }, 
     *                   "b" : { "u" : 7, "w" : 8 }}}
     * </pre>
     * を、"_fallback" を fallbackSlot として単一化すると、
     * <pre>
     * { "a" : { "x" : 1, "y" : 2, "z" : 6 },
     *   "b" : { "u" : 3, "v" : 4, "w" : 8 }}
     * </pre>
     * となる。
     * @param fallbackSlot : fallback で潜っていくスロット。
     * @return 作成された Term。
     */
    public Term unifyFallbacks(String fallbackSlot) {
        Term unifiedTerm = Term.newObjectTerm() ;
        unifyFallbacks_Body(unifiedTerm, this, fallbackSlot) ;
        return unifiedTerm ;
    }

    /**
     * fallback 部分について、Term を単一化（フラット化）する。
     * 再帰されるメイン処理。
     * @param unifiedTerm : 単一化の受け皿の Term。
     * @param originalTerm : 単一化の元になる Term。
     * @param fallbackSlot : fallback として潜っていくスロット名。
     */
    private void unifyFallbacks_Body(Term unifiedTerm,
                                     Term originalTerm,
                                     String fallbackSlot) {
        if(originalTerm.hasArg(fallbackSlot)) {
            unifyFallbacks_Body(unifiedTerm,
                                originalTerm.getArgTerm(fallbackSlot),
                                fallbackSlot) ;
        }
        unifiedTerm.updateObjectFacile(originalTerm, true) ;
        unifiedTerm.clearArg(fallbackSlot) ;
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (String)
     */
    public String getArgString(String slot) {
        return convertValueToString(getArg(slot)) ;
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (String) (fallback 付き)
     */
    public String fetchArgString(String slot, String fallbackSlot, String fallback) {
        return convertValueToString(fetchArg(slot, fallbackSlot, fallback)) ;
    }

    //------------------------------
    /**
     * String への変換
     */
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

    //------------------------------------------------------------
    /**
     * arg 取得 (boolean) (fallback 付き)
     */
    public boolean fetchArgBoolean(String slot, String fallbackSlot, boolean fallback) {
        return convertValueToBoolean(fetchArg(slot, fallbackSlot, fallback)) ;
    }

    //------------------------------
    /**
     * boolean への変換
     */
    private boolean convertValueToBoolean(Object val) {
        if(val instanceof Term) {
            Term term = ((Term)val) ;
            if(term.isAtom()) {
                return convertValueToBoolean(term.getHead()) ;
            }
        } else if(val instanceof Boolean) {
            return (Boolean)val ;
        } else if(val instanceof String) {
            return Boolean.valueOf((String)val) ;
        } else if(val == null) {
            return false ;
        }
        Thread.dumpStack() ;
        Itk.logError("can not convert to boolean:" + this.toString()) ;
        Itk.logError_("use false") ;
        return false ;
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (int)
     */
    public int getArgInt(String slot) {
        return convertValueToInt(getArg(slot)) ;
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (int) (fallback 付き)
     */
    public int fetchArgInt(String slot, String fallbackSlot, int fallback) {
        return convertValueToInt(fetchArg(slot, fallbackSlot, fallback)) ;
    }

    //------------------------------
    /**
     * Int への変換
     */
    private int convertValueToInt(Object val) {
        if(val instanceof Term) {
            return ((Term)val).getInt() ;
        } else if(val instanceof Integer) {
            return ((Integer)val).intValue() ;
        } else if(val == null) {
            Thread.dumpStack() ;
            Itk.logError("can not convert null to int.") ;
            Itk.logError_("use zero") ;
            return 0 ;
        } else {
            Thread.dumpStack() ;
            Itk.logError("can not convert to int:" + this.toString()) ;
            Itk.logError_("use zero") ;
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

    //------------------------------------------------------------
    /**
     * arg 取得 (double) (fallback 付き)
     */
    public double fetchArgDouble(String slot, String fallbackSlot, double fallback) {
        return convertValueToDouble(fetchArg(slot, fallbackSlot, fallback)) ;
    }

    //------------------------------
    /**
     * Double への変換
     */
    private double convertValueToDouble(Object val) {
        if(val instanceof Term) {
            return ((Term)val).getDouble() ;
        } else if(val instanceof Double) {
            return ((Double)val).doubleValue() ;
        } else if(val == null) {
            Thread.dumpStack() ;
            Itk.logError("can not convert null to double.") ;
            Itk.logError_("use zero") ;
            return 0.0 ;
        } else {
            Thread.dumpStack() ;
            Itk.logError("can not convert to double:" + this.toString()) ;
            Itk.logError_("use zero") ;
            return 0.0 ;
        } 
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (BigDecimal)
     */
    public BigDecimal getArgBigDecimal(String slot) {
        return convertValueToBigDecimal(getArg(slot)) ;
    }

    //------------------------------------------------------------
    /**
     * arg 取得 (BigDecimal) (fallback 付き)
     */
    public BigDecimal fetchArgBigDecimal(String slot, String fallbackSlot,
                                         int fallback) {
        return convertValueToBigDecimal(fetchArg(slot, fallbackSlot, fallback)) ;
    }

    //------------------------------
    /**
     * BigDecimal への変換
     */
    private BigDecimal convertValueToBigDecimal(Object val) {
        if(val instanceof Term) {
            return ((Term)val).getBigDecimal() ;
        } else if(val instanceof Integer) {
            return new BigDecimal((Integer)val) ;
        } else if(val instanceof Double) {
            return new BigDecimal((Double)val) ;
        } else if(val == null) {
            Thread.dumpStack() ;
            Itk.logError("can not convert null to BigDecimal.") ;
            Itk.logError_("use zero") ;
            return BigDecimal.ZERO ;
        } else {
            Thread.dumpStack() ;
            Itk.logError("can not convert to BigDecimal:" + this.toString()) ;
            Itk.logError_("use zero") ;
            return BigDecimal.ZERO ;
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
                    setArg(entry.getKey().intern(),entry.getValue(), deepP) ;
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

        body.put(slot.intern(), value) ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * arg クリア
     */
    public Term clearArg(String slot) {
        if(hasArg(slot)) {
            body.remove(slot) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    // Array 操作
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
     * array 取得
     */
    public int getArraySize() {
        return array.size() ;
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
     * array nth 取得 (BigDecimal)
     */
    public BigDecimal getNthBigDecimal(int index) {
        return convertValueToBigDecimal(getNth(index)) ;
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
     * array arg 設定
     */
    public Term setNth(int index, Object value){
        return setNth(index, value, true) ;
    }

    //------------------------------------------------------------
    /**
     * array arg 設定
     */
    public Term setNth(int index, Object value, boolean deepP) {
        if(isNullArray()) allocArray() ;

        if(deepP){ value = letTermedValue(value, deepP) ; }

        array.set(index, value) ;

        return this ;
    }

    //------------------------------------------------------------
    /**
     * array arg 追加
     */
    public Term addNth(Object value){
        return addNth(value, true) ;
    }

    //------------------------------------------------------------
    /**
     * array arg 追加
     */
    public Term addNth(Object value, boolean deepP) {
        if(isNullArray()) allocArray() ;

        if(deepP){ value = letTermedValue(value, deepP) ; }

        array.add(value) ;

        return this ;
    }

    //------------------------------------------------------------
    // Term 変換
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
    // 比較
    //------------------------------------------------------------
    /**
     * hash code
     */
    @Override
    public int hashCode() {
        if(getHead() == null) {
            return 0 ;
        } else {
            return head.hashCode() ;
        }
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
    // タイプチェック
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
        return (((head instanceof BigDecimal &&
                  ((BigDecimal)head).scale() <= 0) ||
                 head instanceof BigInteger ||
                 head instanceof Integer ||
                 head instanceof Long) &&
                isAtom()) ;
    }

    //------------------------------------------------------------
    /**
     * double となるか？
     */
    public boolean isDouble() {
        return ((head instanceof BigDecimal ||
                 head instanceof Double ||
                 head instanceof Float) && isAtom()) ;
    }

    //------------------------------------------------------------
    /**
     * number となるかどうか？
     */
    public boolean isNumber() {
        return isInt() || isDouble() ;
    }

    //------------------------------------------------------------
    /**
     * BigDecimal となるかどうか？
     */
    public boolean isBigDecimal() {
        return isNumber() ;
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
        return (!isArray()) && (!isZeroArgs()) ;
    }

    //------------------------------------------------------------
    // 型変換
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
            Itk.logError("can not convert to String:" + this.toString()) ;
            Itk.logError_("use null.") ;
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
        Itk.logError("can not convert to int:" + this.toString()) ;
        Itk.logError_("use 0.") ;
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
        Itk.logError("can not convert to double:" + this.toString()) ;
        Itk.logError_("use 0.0") ;
        return 0.0 ;
    }

    //------------------------------------------------------------
    /**
     * big decimal としての値。
     */
    public BigDecimal getBigDecimal() {
        if(isNumber()) {
            if (head instanceof BigDecimal) {
                return ((BigDecimal)head) ;
            } else if(isInt()) {
                return new BigDecimal(getInt()) ;
            } else if(isDouble()) {
                return new BigDecimal(getDouble()) ;
            }
        }
        Thread.dumpStack() ;
        Itk.logError("can not convert to BigDecimal:" + this.toString()) ;
        Itk.logError_("use 0") ;
        return BigDecimal.ZERO ;
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

    //------------------------------------------------------------
    // Object 型のアップデート
    //------------------------------------------------------------
    /**
     * 部分 Object を使って、内部の値を更新
     * Object でないものが渡ると、エラーで落ちる。
     * (安直版)
     * @param patch : update する部分 Object
     * @param recursive : 深くまでの update を再帰的に行うか？
     */
    public Term updateObjectFacile(Term patch, boolean recursive) {
        try {
            return updateObject(patch, recursive) ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("Illegal updateObject for Term.") ;
            Itk.logError_("this", this) ;
            Itk.logError_("patch", patch) ;
            System.exit(1) ;
        }
        return this ; // never reach here
    }

    //------------------------------------------------------------
    /**
     * 部分 Object を使って、内部の値を更新
     * @param patch : update する部分 Object
     * @param recursive : 深くまでの update を再帰的に行うか？
     */
    public Term updateObject(Term patch, boolean recursive)
        throws Exception
    {
        if(!this.isObject() && !this.isZeroArgs())
            throw new Exception("Can't update non-Object Term:" + this) ;
        if(!patch.isObject() && !patch.isZeroArgs())
            throw new Exception("Can't update by non-Object Term:" + patch) ;

        if(!patch.isZeroArgs()) {
            for(String slot : patch.getBody().keySet()) {
                Term value = patch.getArgTerm(slot) ;
                Term originalValue = this.getArgTerm(slot) ;
                if(recursive &&
                   value != null && value.isObject() &&
                   originalValue != null && originalValue.isObject()) {
                    originalValue.updateObject(value, recursive) ;
                } else {
                    this.setArg(slot, value) ;
                }
            }
        }
        return this ;
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
            Itk.logError("error in converting to JSON.") ;
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

    //============================================================
    //------------------------------------------------------------
    /**
     * ensure Term object
     */
    static public Term ensureTerm(Object object) {
        if(object instanceof Term) {
            return (Term)object ;
        } else {
            return new Term(object) ;
        }
    }

} // class Term
