// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk Test
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

import java.util.*;

import static org.junit.Assert.*;
import org.junit.Test;
//import junit.framework.TestCase;

import net.arnx.jsonic.JSON ;
import net.arnx.jsonic.JSONException ;

import java.util.Map ;
import java.util.HashMap ;
import java.util.ArrayList;
import nodagumi.Itk.Itk ;

//======================================================================
/**
 * Itk Test
 */
public class Itk_Test {
    //------------------------------------------------------------
    /**
     * String Array の Array.contains と intern による == のスピード比較。
     * 結果：
     * prefix = "abcdefg" の場合。
     * ITKDBG[Lap:Itk.containsItself]: 2.84 [sec]
     * ITKDBG[Lap:String.contains]: 13.876 [sec]
     * prefix = "a" の場合。
     * ITKDBG[Lap:Itk.containsItself]: 2.715 [sec]
     * ITKDBG[Lap:String.contains]: 9.329 [sec]
     */
    @Test
    public void test_StringArray_contains() {
        //String prefix = "abcdefg" ;
        String prefix = "a" ;
        int n = 10 ;
        ArrayList<String> array = new ArrayList<String>(n) ;
        
        for(int i = 0 ; i < n ; i++) {
            String val = (prefix + i).intern() ;
            array.add(val) ;
        }

        int m = 100000000 ;
        int c = 0 ;
        String key = (prefix + (n-1)).intern() ;
        Itk.timerStart("Itk.containsItself") ;
        for(int i = 0 ; i < m ; i++) {
            if(Itk.containsItself(array, key)) { c++ ; }
        }
        Itk.timerShowLap("Itk.containsItself") ;
        Itk.dbgVal("c=",c) ;
        
        c = 0 ;
        Itk.timerStart("String.contains") ;
        for(int i = 0 ; i < m ; i++) {
            if(array.contains(key)) { c++ ; }
        }
        Itk.timerShowLap("String.contains") ;
        Itk.dbgVal("c=",c) ;
        
    }
    //------------------------------------------------------------
    /**
     * String の即値の intern の速さ
     * 結果： n = 100000000 の場合
     * ITKDBG[Lap:new String]: 0.526 [sec]
     * ITKDBG[Lap:inline String]: 0.015 [sec]
     * ITKDBG[Lap:inline intern String]: 57.639 [sec]
     * ITKDBG[Lap:stored intern String]: 0.016 [sec]
     * ITKDBG[Lap:stored intern String ==]: 0.015 [sec]
     */
    //@Test
    public void test_String_inline_intern() {
        String d = Itk.intern("aabbccddeeffggaabbccddeeffggaabbccddeeffgg") ;
        int n = 100000000 ;

        Itk.timerStart("new String") ;
        int c = 0 ;
        for(int i = 0 ; i < n ; i++) {
            final String x = new String("aabbccddeeffggaabbccddeeffggaabbccddeeffgg") ;
            if(d.equals(x)) { c++ ; }
        }
        Itk.timerShowLap("new String") ;
        Itk.dbgVal("c=",c) ;
        
        Itk.timerStart("inline String") ;
        c = 0 ;
        for(int i = 0 ; i < n ; i++) {
            final String x = "aabbccddeeffggaabbccddeeffggaabbccddeeffgg" ;
            if(d.equals(x)) { c++ ; }
        }
        Itk.timerShowLap("inline String") ;
        Itk.dbgVal("c=",c) ;
            
        Itk.timerStart("inline intern String") ;
        c = 0 ;
        for(int i = 0 ; i < n ; i++) {
            String x = Itk.intern("aabbccddeeffggaabbccddeeffggaabbccddeeffgg");
            if(d.equals(x)) { c++ ; }
        }
        Itk.timerShowLap("inline intern String") ;
        Itk.dbgVal("c=",c) ;

        Itk.timerStart("stored intern String") ;
        c = 0 ;
        {
            final String x = Itk.intern("aabbccddeeffggaabbccddeeffggaabbccddeeffgg") ;
            for(int i = 0 ; i < n ; i++) {
                if(d.equals(x)) { c++ ; }
            }
        }
        Itk.timerShowLap("stored intern String") ;
        Itk.dbgVal("c=",c) ;

        final String x = Itk.intern("aabbccddeeffggaabbccddeeffggaabbccddeeffgg") ;
        Itk.timerStart("stored intern String ==") ;
        c = 0 ;
        for(int i = 0 ; i < n ; i++) {
            if(d == x) { c++ ; }
        }
        Itk.timerShowLap("stored intern String ==") ;
        Itk.dbgVal("c=",c) ;
    }
    
    //------------------------------------------------------------
    /**
     * static へのアクセス
     */
    static class Test_static_access0 {
        public static String foo = "Test_static_access0.foo" ;
        public String bar() {
            return this.foo ;
        }
    }
    static class Test_static_access1 extends Test_static_access0 {
        public static String foo = "Test_static_access1.foo" ;
    }
    //@Test
    public void test_static_access() {
        Test_static_access0 tsa0 = new Test_static_access0() ;
        Test_static_access1 tsa1 = new Test_static_access1() ;
        Test_static_access0 tsa2 = (Test_static_access0)tsa1 ;
        Itk.dbgVal("tsa0.foo", tsa0.foo) ;
        Itk.dbgVal("tsa1.foo", tsa1.foo) ;
        Itk.dbgVal("tsa2.foo", tsa2.foo) ;
        Itk.dbgVal("tsa0.bar()", tsa0.bar()) ;
        Itk.dbgVal("tsa1.bar()", tsa1.bar()) ;
        Itk.dbgVal("tsa2.bar()", tsa2.bar()) ;
    }
        
    //------------------------------------------------------------
    /**
     * String のハッシュと null
     */
    //@Test
    public void test_permitNullAsString(){
        HashMap<String, Object> map = new HashMap<String, Object>() ;
        map.put("hoge",new Integer(2)) ;
        Itk.dbgVal("map.get('hoge')",map.get("hoge")) ;
        map.put(null, new Integer(3)) ;
        Itk.dbgVal("map.get(null)",map.get(null)) ;
        map.put("foo", null) ;
        Itk.dbgVal("map.get('foo')",map.get("foo")) ;
    }
        
    //------------------------------------------------------------
    /**
     * dumpStackTraceN
     */
    //@Test
    public void test_dumpStackTraceN() {
        Itk.dbgMsg("all dump") ;
        Itk.dumpStackTrace() ;

        Itk.dbgMsg("3 dump") ;
        Itk.dumpStackTraceN(3) ;

        Itk.dbgMsg("5 dump") ;
        Itk.dumpStackTraceN(5) ;

        Itk.dbgMsg("10 dump") ;
        Itk.dumpStackTraceN(10) ;
    }

    //------------------------------------------------------------
    /**
     * String test
     * 結果として、k=4 の時には、intern すると約2倍、
     * k=13 の時には、intern すると300-400倍の高速化。
     */
    //@Test
    public void test_StringEqual() {
        String prefix = "a" ;
        //int k = 13 ;
        int k = 4 ;
        for(int i = 0 ; i < k ; i++) {
            prefix = prefix + prefix ;
        }

        int n = 1000000 ;
        ArrayList<String> array = new ArrayList<String>() ;
        for(int i = 0 ; i < n ; i++) {
            String data = prefix + i ;
            array.add(data.intern()) ;
        }

        String key = (prefix + (n-1)).intern() ;

        for(int i = 0 ; i < 10 ; i++) {
            Itk.timerStart("array.contains") ;
            Itk.dbgVal("contains",array.contains(key)) ;
            Itk.timerShowLap("array.contains") ;

            Itk.timerStart("array.intern") ;
            for(String data : array) if(data == key) break ;
            Itk.timerShowLap("array.intern") ;
        }
    }
    //------------------------------------------------------------
    /**
     * initialization test
     */
    static class Bar {
        static int x = 0 ;
        public int y = x++ ;
    }
    //@Test
    public void test_Init(){
        Bar b0 = new Bar() ;
        Itk.dbgVal("b0.y", b0.y) ;
        Bar b1 = new Bar() ;
        Itk.dbgVal("b1.y", b1.y) ;
        Bar b2 = new Bar() ;
        Itk.dbgVal("b2.y", b2.y) ;
    }
    //------------------------------------------------------------
    /**
     * float test
     */
    //@Test
    public void test_FloatMulti() {
        float f = 0.3f ;
        Itk.dbgVal("f", f) ;
        Itk.dbgVal("0.5f", 0.5f) ;
        Itk.dbgVal("2.0f", 2.0f) ;
        Itk.dbgVal("3f", 3f) ;
    }
    //------------------------------------------------------------
    /**
     * test dump call stack
     */
    //@Test
    public void test_DumpStackTrace() {
        Itk.dumpStackTrace() ;
    }
    //------------------------------------------------------------
    /**
     * test Array
     */
    //@Test
    public void test_Array() {
        ArrayList<Integer> a2 = new ArrayList<Integer>() ;
        for(int i = 0 ; i < 10 ; i++) { a2.add(i) ; }
        for(int i = 0 ; i < a2.size() ; i++) {
            Integer k = a2.get(a2.size() - i - 1) ;
            if(k == 3) { a2.remove(k) ; }
            Itk.dbgVal("k",k) ;
            Itk.dbgVal("a2", a2) ;
        }

        ArrayList<Integer> a1 = new ArrayList<Integer>() ;
        for(int i = 0 ; i < 10 ; i++) { a1.add(i) ; }
        for(Integer k : a1) {
            if(k == 3) { a1.remove(k) ; }
            Itk.dbgVal("k",k) ;
            Itk.dbgVal("a1", a1) ;
        }
    }

    //------------------------------------------------------------
    /**
     * test dbgVal
     */
    //@Test
    public void test_dbgVal() {
        int x = 1 ;
        Itk.dbgVal("x",x) ;
        double y = 2.3 ;
        Itk.dbgVal("y", y) ;
        String s = "abc" ;
        Itk.dbgVal("s",s) ;
        int[] v = {1, 2, 3} ;
        Itk.dbgVal("v",v) ;
        Object n = null ;
        Itk.dbgVal("n",n) ;
    }

    //------------------------------------------------------------
    /**
     * test JSON
     */
    //@Test
    public void test_Json() {
        Itk.dbgMsgMethodInfo() ;

        Object[] str = {"ab\"c", 1.2, 3} ;
        Itk.dbgMsg("str",str) ;
        Itk.dbgMsg("str(JSON)",JSON.encode(str)) ;

    }

    //------------------------------------------------------------
    /**
     * test dbgMsg
     */
    //@Test
    public void test_dbgMsg() {
        Itk.dbgMsgMethodInfo() ;
        int x = 1 ;
        String y = "foo bar baz" ;
        int z[] = {1, 2, 3} ;
        int w[][] = {{1,2},{3,4}} ;
        Itk.dbgMsg(x) ;
        Itk.dbgMsg("x", x) ;
        Itk.dbgMsg("y", y) ;
        Itk.dbgMsg("z", z) ;
        Itk.dbgMsg("w", w) ;
    }

    //------------------------------------------------------------
    /**
     * test random URI
     */
    //@Test
    public void test_randomURI() {
        Itk.dbgMsgMethodInfo() ;
        Itk.dbgMsg("uri=", Itk.genUriRandom()) ;
    }

    //------------------------------------------------------------
    /**
     * test CurrentTimeStr
     */
    //@Test
    public void test_getCurrentTimeStr() {
        Itk.dbgMsgMethodInfo() ;
        Itk.dbgMsg("time=", Itk.getCurrentTimeStr()) ;
    }

    //------------------------------------------------------------
    /**
     * test CurrentStack
     */
    //@Test
    public void test_currentCall() {
        Itk.dbgMsgMethodInfo() ;
        Itk.dbgMsg("---","currentCall()") ;
        Itk.dbgMsg("stack=", Itk.currentCall()) ;
    }

    //------------------------------------------------------------
    /**
     * test working
     */
    //@Test
    public void test_working00() {
        Itk.dbgMsgMethodInfo() ;
        String json = "{ 'abc' : 123, 'xyz' : '123' }" ;
        //String json = "[ 'abc', 123, 'xyz', '123' ]" ; // -> error
        //String json = " " ;
        Map<String, Object> map = (Map<String, Object>)JSON.decode(json);
        Itk.dbgMsg("map", map) ;
        Itk.dbgMsg("abc", map.get("abc")) ;
        Itk.dbgMsg("xyz", map.get("xyz")) ;
        Itk.dbgMsg("def", map.get("def")) ;
    }

    //------------------------------------------------------------
    /**
     * test working
     */
    static class Foo00 {
        public static String bar = "Foo00's bar" ;
        public static String baz() { return bar ; }
    }
    static class Foo01 extends Foo00 {
        public static String bar = "Foo01's bar" ;
    }
    static class Foo02 extends Foo01 {
        public static String bar = "Foo02's bar" ;
    }

    //@Test
    public void test_working01() {
        Itk.dbgMsgMethodInfo() ;
        Itk.dbgMsg("class", Foo00.class) ;
        Itk.dbgMsg("Foo00", Foo00.baz()) ;
        Itk.dbgMsg("Foo01", Foo01.baz()) ;
        Itk.dbgMsg("Foo02", Foo02.baz()) ;
    }

    //------------------------------------------------------------
    /**
     * test enum
     */
    public enum Foo10 { val0, val2 }

    //@Test
    public void test_enum() {
        Itk.dbgMsgMethodInfo() ;
        Foo10 foo = Foo10.val0 ;
        Foo10 bar = null ;

        Itk.dbgMsg("foo", foo) ;
        Itk.dbgMsg("bar", bar) ;
    }

    //------------------------------------------------------------
    /**
     * test error and warning
     */
    //@Test
    public void test_ErrWrn() {
        Itk.dbgMsgMethodInfo() ;

        Itk.dbgErr("foooooo") ;
        Itk.dbgWrn("barrrrr") ;
        Itk.dbgErr("serious","hogehoge") ;
        Itk.dbgWrn("normal","fugafuga") ;
    }

    //------------------------------------------------------------
    /**
     * test bit operation
     */
    //@Test
    public void test_bitOperation() {
        int x = 4 ;
        Itk.dbgVal("x&1",x & 1) ;
        Itk.dbgVal("x&2",x & 2) ;
        Itk.dbgVal("x&4",x & 4) ;
    }

    //------------------------------------------------------------
    /**
     * test bit String intern
     */
    //@Test
    public void test_stringIntern() {
        int n = 100000000 ;
        //int m = 10 ;
        int m = 3 ;
        String data0 = "foobarbaz" ;
        for(int i = 0 ; i < m ; i++) {
            data0 = data0 + data0 ;
        }
        String data = data0.intern() ;

        Itk.dbgVal("data",data) ;

        ArrayList<String> listCopy = new ArrayList<String>() ;
        ArrayList<String> listSame = new ArrayList<String>() ;
        ArrayList<String> listIntern = new ArrayList<String>() ;

        for(int i = 0 ; i < m ; i++) {
            String newData = "" + data0 ;
            listCopy.add(newData) ;
            listSame.add(data) ;
            listIntern.add(newData.intern()) ;
        }
        int c ;
        // copy
        Itk.timerStart("copy") ;
        c = 0 ;
        for(int i = 0 ; i < n ; i++) {
            if(listCopy.get(i % m).equals(data)) {
                c += 1 ;
            } ;
        }
        Itk.timerShowLap("copy") ;
        Itk.dbgVal("c",c) ;
        // intern
        Itk.timerStart("intern") ;
        c = 0 ;
        for(int i = 0 ; i < n ; i++) {
            if(listIntern.get(i % m).equals(data)) {
            //if(listIntern.get(i % m) == data) {
                c += 1 ;
            } ;
        }
        Itk.timerShowLap("intern") ;
        Itk.dbgVal("c",c) ;
        // same
        Itk.timerStart("same") ;
        c = 0 ;
        for(int i = 0 ; i < n ; i++) {
            if(listSame.get(i % m).equals(data)) {
                c += 1 ;
            } ;
        }
        Itk.timerShowLap("same") ;
        Itk.dbgVal("c",c) ;
    }

} // class Itk_Test

