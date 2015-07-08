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
     * dumpStackTraceN
     */
    @Test
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



} // class Itk_Test

