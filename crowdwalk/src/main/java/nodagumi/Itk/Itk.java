// -*- mode: java; indent-tabs-mode: nil -*-
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
import java.lang.StringBuffer;

import java.io.File;
import java.io.OutputStream ;
import java.io.PrintStream;

import java.util.UUID;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Arrays ;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.DateFormat ;
import java.text.SimpleDateFormat ;

import net.arnx.jsonic.JSON ;

//======================================================================
/**
 * General Utility for Itk.
 * デバッグ用の各種ツール群。
 */
public class Itk {
    //------------------------------------------------------------
    // デバッグ出力
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デバッグ出力先
     */
    static public PrintStream dbgOutStream = System.out ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デバッグ出力結合セパレータ
     */
    static public String dbgJoinSeparator = " " ;

    //------------------------------------------------------------
    /**
     * デバッグ用出力汎用コマンド(出力ストリーム指定付)
     * @param strm : 出力用ストリーム
     * @param tag : 行の先頭のタグ
     * @param objects : 出力するオブジェクト
     */
    static public void dbgGeneric(PrintStream strm,
                                  String tag, Object... objects) {
        try {
            StringBuffer buffer = new StringBuffer() ;
            buffer.append(tag).append(":") ;
            int count = 0 ;
            for(Object obj : objects) {
                if(count > 0) buffer.append(dbgJoinSeparator) ;
                count++ ;

                if(obj == null) {
                    buffer.append("(null)") ;
                } 
                /* XML は当面使わないので、コメントアウト
                else if(object instanceof Node) {
                    dbgGeneric(tag, XMLFormatConverter.toString((Node)object)) ;
                }
                */
                else {
                    buffer.append(objectToString(obj)) ;
                }
            }
            strm.println(buffer.toString()) ;
        } catch(/*TransformerException*/ Exception ex) {
            ex.printStackTrace();
        }
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力汎用コマンド
     * @param tag 行の先頭のタグ
     * @param objects 出力するオブジェクト
     */
    static public void dbgGeneric(String tag, Object... objects) {
        dbgGeneric(dbgOutStream, tag, objects) ;
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力汎用コマンド
     * @param tag 行の先頭のタグ
     * @param label 先頭に出すラベル
     * @param objects 出力するオブジェクト
     */
    static public void dbgGeneric(String tag, String label, Object... objects) {
        dbgGeneric(tag + "[" + label + "]", objects) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デバッグ出力のヘッダ
     */
    final static public String DbgMsgTag = "ITKDBG" ;

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * @param object 出力するオブジェクト
     */
    static public void dbgMsg(Object object) {
        dbgGeneric(DbgMsgTag, object) ;
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * @param label 先頭に出すラベル
     * @param object 出力するオブジェクト
     */
    static public void dbgMsg(String label, Object object) {
        dbgGeneric(DbgMsgTag, label, object) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * デバッグ出力値チェックのヘッダ
     */
    final static public String DbgValTag = "ITKVAL" ;

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * @param object 出力するオブジェクト
     */
    static public void dbgVal(Object object) {
        String className = object.getClass().getSimpleName() ;

        dbgGeneric(DbgValTag, (Object)"(", className, ")", object) ;
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * @param label 先頭に出すラベル
     * @param object 出力するオブジェクト
     */
    static public void dbgVal(String label, Object object) {
        String className = (object == null ? "Null" : 
                            object.getClass().getSimpleName()) ;

        dbgGeneric(DbgValTag, label, "(", className, ")", object) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 警告出力のヘッダ
     */
    final static public String DbgWrnTag = "ITKWRN" ;
    final static public String DbgWrnTail = "?????" ;

    //------------------------------------------------------------
    /**
     * 警告用出力コマンド
     * @param object 出力するオブジェクト
     */
    static public void dbgWrn(Object object) {
        dbgGeneric(DbgWrnTag, "@" + currentMethod(1),DbgWrnTail) ;
        dbgGeneric(DbgWrnTag, object) ;
    }

    //------------------------------------------------------------
    /**
     * 警告用出力コマンド
     * @param label 先頭に出すラベル
     * @param object 出力するオブジェクト
     */
    static public void dbgWrn(String label, Object object) {
        dbgGeneric(DbgWrnTag, "@" + currentMethod(1),DbgWrnTail) ;
        dbgGeneric(DbgWrnTag, label, object) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エラー出力のヘッダ
     */
    final static public String DbgErrTag = "ITKERR" ;
    final static public String DbgErrTail = "!!!!!" ;

    //------------------------------------------------------------
    /**
     * エラー用出力コマンド
     * @param object 出力するオブジェクト
     */
    static public void dbgErr(Object object) {
        dbgGeneric(DbgErrTag, "@" + currentMethod(1),DbgErrTail) ;
        dbgGeneric(DbgErrTag, object) ;
    }

    //------------------------------------------------------------
    /**
     * エラー用出力コマンド
     * @param label 先頭に出すラベル
     * @param object 出力するオブジェクト
     */
    static public void dbgErr(String label, Object object) {
        dbgGeneric(DbgErrTag, "@" + currentMethod(1),DbgErrTail) ;
        dbgGeneric(DbgErrTag, label, object) ;
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * 現在のメソッド名を出力する。
     */
    static public void dbgMsgMethodInfo() {
        dbgMsg("@@@@@", currentMethod(1)) ;
    }

    //------------------------------------------------------------
    /**
     * デバッグ用出力コマンド
     * 現在のメソッド名を出力する。
     */
    static public void dbgMsgThreadInfo() {
        dbgMsg("@@@@@", Thread.currentThread()) ;
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
        //      return object.getClass().getName() ;
    }

    //------------------------------------------------------------
    // ログ出力
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ログ出力先
     */
    static public PrintStream logOutStream = System.out ;

    //------------------------------------------------------------
    // ログ出力
    //------------------------------------------------------------
    /**
     * ログ用出力汎用コマンド
     * @param tag 行の先頭のタグ
     * @param objects 出力するオブジェクト
     */
    static public void logGeneric(String tag, Object... objects) {
        dbgGeneric(logOutStream, tag, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ用出力汎用コマンド
     * @param tag 行の先頭のタグ
     * @param label 先頭に出すラベル
     * @param objects 出力するオブジェクト
     */
    static public void logGeneric(String tag, String label, Object... objects) {
        logGeneric(tag + "[" + label + "]", objects) ;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * ログレベル
     */
    static public enum LogLevel {
        None,
        Trace,
        Debug,
        Info,
        Warn,
        Error,
        Fatal
    } ;

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    static public Lexicon logLevelLexicon = new Lexicon() ;
    static {
        logLevelLexicon.registerEnum(LogLevel.class) ;
    }
    
    //============================================================
    //------------------------------------------------------------
    static public LogLevel getLogLevel(String levelString) {
        return (LogLevel)logLevelLexicon.lookUp(levelString) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ログレベルタグ
     */
    static public String LogTagPrefix = "ITK_" ;

    static public HashMap<LogLevel, String> LogTag =
        new HashMap<LogLevel, String>() ;

    static {
        LogLevel[] enumList =
            (LogLevel[])LogLevel.class.getEnumConstants() ;
        for(LogLevel level : enumList) {
            LogTag.put(level, LogTagPrefix + level.toString()) ;
        }
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ログレベル
     */
    //static public LogLevel logLevel = LogLevel.Info ;
    static public LogLevel logLevel = LogLevel.Debug ;

    //------------------------------------------------------------
    /**
     * ログ出力一般
     * @param level : specify log level
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logOutput(LogLevel level, String label,
                                 Object... objects) {
        if(level.ordinal() >= logLevel.ordinal()) {
            String tag = LogTag.get(level) ;
            if(objects == null || objects.length == 0) {
                logGeneric(tag, (Object)label) ;
            } else {
                logGeneric(tag, label, objects) ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * ログ出力（None)。
     * 必ず何も出力しない。コメントアウト代わり。
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logNone(String label, Object... objects) {
    }

    //------------------------------------------------------------
    /**
     * ログ出力（Trace)
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logTrace(String label, Object... objects) {
        logOutput(LogLevel.Trace, label, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ出力（Debug)
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logDebug(String label, Object... objects) {
        logOutput(LogLevel.Debug, label, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ出力（Info)
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logInfo(String label, Object... objects) {
        logOutput(LogLevel.Info, label, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ出力（Warn)
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logWarn(String label, Object... objects) {
        logOutput(LogLevel.Warn, DbgWrnTail, "@", currentMethod(1)) ;
        logOutput(LogLevel.Warn, label, objects) ;
    }

    static public void logWarn_(String label, Object... objects) {
        logOutput(LogLevel.Warn, label, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ出力（Error)
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logError(String label, Object... objects) {
        logOutput(LogLevel.Error, DbgErrTail, "@", currentMethod(1)) ;
        logOutput(LogLevel.Error, label, objects) ;
    }

    static public void logError_(String label, Object... objects) {
        logOutput(LogLevel.Error, label, objects) ;
    }

    //------------------------------------------------------------
    /**
     * ログ出力（Fatal)
     * @param label : label string. quoted by "[" and "]".
     * @param objects : rest data to output.  
     *                  If empty, label becomes the first object.
     */
    static public void logFatal(String label, Object... objects) {
        logOutput(LogLevel.Error, DbgErrTail, "@", currentMethod(1)) ;
        logOutput(LogLevel.Fatal, label, objects) ;
    }

    static public void logFatal_(String label, Object... objects) {
        logOutput(LogLevel.Fatal, label, objects) ;
    }

    //------------------------------------------------------------
    // 時刻・計時関係
    //------------------------------------------------------------
    /**
     * 現在時刻をDateで取得
     */
    static public Date getCurrentTimeDate() {
        return new Date() ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 時刻文字列のフォーマット
     */
    static public final String DefaultTimeStrFormatPattern 
        = "yyyy-MM-dd_HH:mm:ss.SSS" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 時刻フォーマット
     */
    static public final DateFormat DefaultTimeStrFormat 
        = new SimpleDateFormat(DefaultTimeStrFormatPattern) ;

    //------------------------------------------------------------
    /**
     * 現在時刻をStringで取得
     * @return 現在時刻文字列
     */
    static public String getCurrentTimeStr() {
        return getCurrentTimeStr(DefaultTimeStrFormat) ;
    }

    //------------------------------------------------------------
    /**
     * 現在時刻をStringで取得
     * @param formatPattern 指定のフォーマット
     * @return 現在時刻文字列
     */
    static public String getCurrentTimeStr(String formatPattern) {
        return getCurrentTimeStr(new SimpleDateFormat(formatPattern)) ;
    }

    //------------------------------------------------------------
    /**
     * 現在時刻をStringで取得
     * @param form 指定のフォーマット
     * @return 現在時刻文字列
     */
    static public String getCurrentTimeStr(DateFormat form) {
        Date date = getCurrentTimeDate() ;
        return form.format(date) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 計時用テーブル
     */
    static public HashMap<String, Date> timerTable =
        new HashMap<String, Date>() ;

    //------------------------------------------------------------
    /**
     * 計時開始
     */
    static public Date timerStart(String tag) {
        Date timer = new Date() ;
        timerTable.put(tag, timer) ;
        timer.setTime((new Date()).getTime()) ;
        return timer ;
    }

    //------------------------------------------------------------
    /**
     * 計時のラップタイム（秒）
     */
    static public double timerLap(String tag) {
        Date now = new Date() ;
        Date origin = timerTable.get(tag) ;
        if(origin == null) {
            Itk.dumpStackTrace() ;
            Itk.logError("The specified tag is not found in timerTable:" + tag) ;
            return 0 ;
        }
        long diff = now.getTime() - origin.getTime() ;
        return ((double)diff)/1000.0 ;
    }

    //------------------------------------------------------------
    /**
     * 計時のラップタイム表示（秒）
     */
    static public void timerShowLap(String tag) {
        double lap = timerLap(tag) ;
        Itk.dbgMsg("Lap:"+tag, " " + lap + " [sec]") ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    static private Pattern timeStringPatternShort =
        Pattern.compile("(\\d?\\d):(\\d?\\d):?(\\d?\\d)?");
    static private Pattern timeStringPatternLong =
        Pattern.compile("(\\d?\\d):(\\d?\\d):(\\d?\\d)");

    //------------------------------------------------------------
    /**
     * 時間・時刻表示の解析
     * もし解析できなければ、Exception を throw。
     * 時間の形式は、 HH:MM:SS もしくは HH:MM
     * @param timeStr 時間・時刻の文字列
     * @return 時刻・時間を返す。
     */
    static public int scanTimeStringToInt(String timeStr) throws Exception {
        int timeVal = 0 ;

        Matcher matchLong = timeStringPatternLong.matcher(timeStr) ;
        if (matchLong.matches()) {
            timeVal = (3600 * Integer.parseInt(matchLong.group(1)) +
                       60 * Integer.parseInt(matchLong.group(2)) +
                       Integer.parseInt(matchLong.group(3))) ;
        } else {
            Matcher matchShort = timeStringPatternShort.matcher(timeStr) ;
            if (matchShort.matches()) {
                timeVal = (3600 * Integer.parseInt(matchShort.group(1)) +
                           60 * Integer.parseInt(matchShort.group(2))) ;
            } else {
                Itk.logError("Illegal time format:" + timeStr) ;
                throw new Exception("Illegal time format:" + timeStr) ;
            }
        }
        return timeVal ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    static private String timeStringFormatLong = "%02d:%02d:%02d" ;

    //------------------------------------------------------------
    /**
     * 時間・時刻の文字列変換
     * @param time 秒単位の整数
     * @return 時刻・時間の文字列
     */
    static public String formatSecTime(int time) {
        int sec = time % 60 ;
        int restMin = (time - sec) / 60 ;
        int min = restMin % 60 ;
        int hour = (restMin - min) / 60 ;
        return String.format(timeStringFormatLong, hour, min, sec) ;
    }

    //------------------------------------------------------------
    // スタックトレース、現在のクラスやメソッド情報
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * StackTrace出力のヘッダ
     */
    final static public String StackTraceTag = "ITK\t" ;

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド情報
     */
    static public void dumpStackTrace() {
        dumpStackTrace(1) ;
    }

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド情報
     */
    static public void dumpStackTrace(int offset) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace() ;
        for(int i = 2 + offset ; i < trace.length ; i++) {
            dbgGeneric(StackTraceTag, trace[i]) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド情報（直近N段)
     */
    static public void dumpStackTraceN(int n) {
        dumpStackTraceN(n, 1) ;
    }

    //------------------------------------------------------------
    /**
     * 現在実行中のメソッド情報（直近N段)
     */
    static public void dumpStackTraceN(int n, int offset) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace() ;
        int from = 2 + offset ;
        int to = from + n ;
        if(trace.length < to) to = trace.length ;

        for(int i = from ; i < to ; i++) {
            dbgGeneric(StackTraceTag, trace[i]) ;
        }
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

    //------------------------------------------------------------
    /**
     * 現在位置のクラス名
     */
    static public String currentClassName() {
        return currentClassName(1) ;
    }

    //------------------------------------------------------------
    /**
     * 現在位置のクラス名
     */
    static public String currentClassName(int offset) {
        return currentCall(offset + 1).getClassName() ;
    }

    //------------------------------------------------------------
    /**
     * UUID によりランダムな URI を生成
     */
    static public String genUriRandom() {
        UUID uuid = UUID.randomUUID() ;
        return "uri:uuid:" + uuid.toString() ;
    }

    //============================================================
    // Exit from running.
    //========================================
    //----------------------------------------
    /**
     * exit safely.
     */
    static public void quitSafely() {
        System.exit(0) ;
    }

    //----------------------------------------
    /**
     * exit by error.
     */
    static public void quitByError() {
        System.exit(1) ;
    }
    
    //----------------------------------------
    /**
     * exit for test.
     */
    static public void quitForTest() {
        System.exit(1) ;
    }
    
    //============================================================
    /**
     * JSON handling class
     */
    static public class JsonObject extends HashMap<String,Object> {
        //------------------------------
        /**
         * JSON 文字列への変換 （1行）
         */
        public String toJson() { return toJson(false) ; }

        //------------------------------
        /**
         * JSON 文字列への変換 （prity print 可能）
         */
        public String toJson(boolean pprintP){ 
            return JSON.encode(this, pprintP) ; 
        }

        //------------------------------
        /**
         * JSON 文字列の出力 （1行）
         */
        public void toJson(OutputStream ostrm){ toJson(ostrm, false) ; }

        //------------------------------
        /**
         * JSON 文字列の出力 （prity print 可能）
         */
        public void toJson(OutputStream ostrm, boolean pprintP){ 
            try {
                JSON.encode(this, ostrm, pprintP) ;
            } catch(Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("Exception",ex) ;
            }
        }

        //------------------------------
        /**
         * 文字列の取り出し
         */
        static public String pickString(Map<String,Object> object, String key){
            return convertString(object.get(key)) ;
        }

        //------------------------------
        /**
         * intへの変換
         */
        static public int pickInt(Map<String,Object> object, String key){
            return convertInt(object.get(key)) ;
        }

        //------------------------------
        /**
         * doubleへの変換
         */
        static public double pickDouble(Map<String,Object> object, String key){
            return convertDouble(object.get(key)) ;
        }

        //------------------------------
        /**
         * Json Object ({@literal Map<String,Object>}) への変換
         */
        static public Map<String,Object> pickObject(Map<String,Object> object,
                                                    String key){
            return convertObject(object.get(key)) ;
        }

        //------------------------------
        /**
         * 文字列への変換
         */
        static public String convertString(Object object){
            return object.toString() ;
        }

        //------------------------------
        /**
         * intへの変換
         */
        static public int convertInt(Object object){
            return Integer.parseInt(object.toString()) ;
        }

        //------------------------------
        /**
         * doubleへの変換
         */
        static public double convertDouble(Object object){
            return Double.parseDouble(object.toString()) ;
        }

        //------------------------------
        /**
         * Json Object ({@literal Map<String,Object>}) への変換
         */
        static public Map<String,Object> convertObject(Object object){
            return (Map<String,Object>)object ;
        }
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * uniformed String intern operation.
     * currently, just call String::intern()
     */
    final static public String intern(String str) {
        //Itk.dbgVal("intern.str=",str) ;
        //Itk.dumpStackTrace() ;
        //return str ;
        return str.intern() ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * exact contains an item in an array.
     * Check the array has the obj as an object itself.
     * In other word, equality is checked by == instead of equals(). 
     */
    final static public boolean containsItself(ArrayList array, Object obj){
        for(Object item : array) {
            if(item == obj) { return true ; }
        }
        // for test and debug
        //if(array.contains(obj)) {
        //    Itk.logWarn("not interned values in contains.");
        //    Itk.dbgVal("array=", array) ;
        //    Itk.dbgVal("obj=", obj) ;
        //    Itk.dbgVal("(String)obj is interned?:",
        //               ((String)obj).intern() == ((String)obj)) ;
        //    Itk.dumpStackTrace() ;
        //}
        //
        return false ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * check dir / file exists.
     * @param pathName : path of directory / file.
     */
    final static public boolean pathExists(String pathName) {
        File path = new File(pathName) ;
        return path.exists() ;
    }
    
    //============================================================
    //------------------------------------------------------------
    /**
     * ensure dir exists.
     * @param path : path of directory / file.
     * @param fileP : true if path is an actual file path.
     *                If false, path is considered as dir path.
     */
    final static public boolean ensureDirExists(String path,
                                                boolean checkParent) {
        if(checkParent) {
            path = new File(path).getParent() ;
        }
        File dir = new File(path) ;
        if(! dir.exists()) {
            Itk.logWarn("Itk.ensureDirExists()",
                        "The directory is not exists. Create it.:",
                        path) ;
            dir.mkdir() ;
            return true ;
        } else {
            return false ;
        }
    }
    
} // class Itk

