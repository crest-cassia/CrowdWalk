// -*- mode: java; indent-tabs-mode: nil -*-
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

package nodagumi.Itk;

import static org.junit.Assert.*;
import org.junit.Test;
//import junit.framework.TestCase;

import nodagumi.Itk.Lexicon;


//======================================================================
/**
 * description of Test.
 */
public class Lexicon_Test {
    //------------------------------------------------------------
    /**
     */
    @Test
    public void testNullLookUp() {
        Itk.dbgMsgMethodInfo() ;

        Lexicon lex = new Lexicon() ;

        lex.registerEnum(FooEnum01.class) ;
        Itk.dbgVal("lex",lex) ;
        Itk.dbgVal("lookUp(Baz0)", lex.lookUp("Baz0")) ;
        Itk.dbgVal("lookUp(Bar0)", lex.lookUp("Bar0")) ;
        Itk.dbgVal("lookUp(null)", lex.lookUp(null)) ;
    }

    //------------------------------------------------------------
    /**
     */
    //@Test
    public void testMultiRegister() {
        Itk.dbgMsgMethodInfo() ;

        Object[][] entryList = { { "bar0", FooEnum00.Bar0 },
                             { "bar1", FooEnum00.Bar1 },
                             { "bar2", FooEnum00.Bar2 } } ;
        Itk.dbgMsg("entryList", entryList) ;

        Lexicon lex = new Lexicon(new Object[][] 
            { { "bar0", FooEnum00.Bar0 },
              { "bar1", FooEnum00.Bar1 },
              { "bar2", FooEnum00.Bar2 } } ) ;

        Itk.dbgMsg("bar0", lex.lookUp("bar0")) ;
        Itk.dbgMsg("bar1", lex.lookUp("bar1")) ;
        Itk.dbgMsg("bar2", lex.lookUp("bar2")) ;
        Itk.dbgMsg("bar2b", lex.lookUp("bar2b")) ;
    }

    //------------------------------------------------------------
    /**
     */
    //@Test
    public void testEnumRegister() {
        Itk.dbgMsgMethodInfo() ;

        Lexicon lex = new Lexicon() ;

        lex.registerEnum(FooEnum01.class) ;
        Itk.dbgMsg("lex",lex) ;
    }

    //------------------------------------------------------------
    enum FooEnum00 { Bar0, Bar1, Bar2 }
    enum FooEnum01 { Baz0, Baz1, Baz2 }
    /**
     */
    //@Test
    public void testEnumLookUp() {
        Itk.dbgMsgMethodInfo() ;

        Lexicon lex = new Lexicon() ;
        lex.register("bar0", FooEnum00.Bar0) ;
        lex.register("bar1", FooEnum00.Bar1) ;
        lex.register("bar2", FooEnum00.Bar2) ;
        lex.register("bar2b", FooEnum00.Bar2) ;

        lex.register("baz0", FooEnum01.Baz0) ;
        lex.register("baz1", FooEnum01.Baz1) ;
        lex.register("baz2", FooEnum01.Baz2) ;

        Itk.dbgMsg("bar0", lex.lookUp("bar0")) ;
        Itk.dbgMsg("bar1", lex.lookUp("bar1")) ;
        Itk.dbgMsg("bar2", lex.lookUp("bar2")) ;
        Itk.dbgMsg("bar2b", lex.lookUp("bar2b")) ;
        Itk.dbgMsg("baz0", lex.lookUp("baz0")) ;
        Itk.dbgMsg("baz1", lex.lookUp("baz1")) ;
        Itk.dbgMsg("baz2", lex.lookUp("baz2")) ;
        //Itk.dbgMsg("baz2", (FooEnum00)lex.lookUp("baz2")) ; -> should error
        Itk.dbgMsg("class check", lex.lookUp("baz2").getClass()) ;
        Itk.dbgMsg("enum check", lex.lookUp("baz2") instanceof FooEnum00) ;
        Itk.dbgMsg("getEnumConstants", FooEnum00.class.getEnumConstants()) ;

        Itk.dbgMsg("r:Bar0", lex.lookUpByMeaning(FooEnum00.Bar0)) ;
        Itk.dbgMsg("r:Bar1", lex.lookUpByMeaning(FooEnum00.Bar1)) ;
        Itk.dbgMsg("r:Bar2", lex.lookUpByMeaning(FooEnum00.Bar2)) ;
    }

} // class LexiconTest

