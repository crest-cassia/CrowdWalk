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

import static org.junit.Assert.*;
import org.junit.Test;
import junit.framework.TestCase;

import net.arnx.jsonic.JSON ;
import net.arnx.jsonic.JSONException ;

import java.util.Map ;
import java.util.HashMap ;
import nodagumi.Itk.Itk ;

//======================================================================
/**
 * Itk Test
 */
public class Itk_Test extends TestCase {
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

    @Test
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

    @Test
    public void test_enum() {
        Foo10 foo = Foo10.val0 ;
        Foo10 bar = null ;

        Itk.dbgMsg("foo", foo) ;
        Itk.dbgMsg("bar", bar) ;
    }


} // class Itk_Test

