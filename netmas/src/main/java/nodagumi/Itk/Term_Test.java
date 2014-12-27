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

import java.util.HashMap ;

import nodagumi.Itk.*;

//======================================================================
/**
 * description of Test.
 */
public class Term_Test {
    //------------------------------------------------------------
    /**
     */
    @Test
    public void test_scanJson() {
        Itk.dbgMsgMethodInfo() ;

        String str0 = "{'':'foo', 'abc':'def', 'ghi':1, 'jkl':null, 'x':[1,2,{},'234',null,{'xx':'yy','zz':'123'}], 'y':{'':'bar', 'a':3.21, 'b':[]}}" ;
        str0 = str0.replaceAll("'","\"") ;
        Itk.dbgVal("str0",str0) ;
        Term term0 = Term.newByJson(str0) ;
        Itk.dbgVal("term0",term0) ;
        Itk.dbgVal("term0(JSON)",term0.toJson()) ;
        Itk.dbgVal("term0(JSON)",term0.toJson(true)) ;

        Itk.dbgVal("term0[x]",term0.getArg("x")) ;
        Itk.dbgVal("term0[y]",term0.getArg("y")) ;
        Itk.dbgVal("term0.head",term0.getHead()) ;
        
    }

    //------------------------------------------------------------
    /**
     */
    @Test
    public void test_construct() {
        Itk.dbgMsgMethodInfo() ;

        Term term0 = new Term() ;
        Itk.dbgVal("term0",term0) ;
        Itk.dbgVal("term0(JSON)",term0.toJson()) ;

        Term term1 = new Term("foo") ;
        Itk.dbgVal("term1",term1) ;
        Itk.dbgVal("term1(JSON)",term1.toJson()) ;


        HashMap<String,Object> b2 = new HashMap() ;
        b2.put("foo",1) ;
        b2.put("bar",2) ;
        Term term2 = new Term(b2) ;
        Itk.dbgVal("term2",term2) ;
        Itk.dbgVal("term2(JSON)",term2.toJson()) ;

        HashMap<String,Object> b3 = new HashMap() ;
        b3.put("foo",1) ;
        b3.put("bar",2) ;
        b3.put("","baz") ;
        Term term3 = new Term(b3) ;
        Itk.dbgVal("term3",term3) ;
        Itk.dbgVal("term3(JSON)",term3.toJson()) ;

        HashMap<String,Object> b4 = new HashMap() ;
        b4.put("bar","aho") ;
        b4.put("baz",2.34) ;
        Term term4 = new Term("foo",b4) ;
        Itk.dbgVal("term4",term4) ;
        Itk.dbgVal("term4(JSON)",term4.toJson()) ;

        Term term5 = new Term("foo") ;
        term5.setArg("bar","aho") ;
        term5.setArg("baz",2.34) ;

        Itk.dbgMsg("term0==null",term0.equals(null)) ;
        Itk.dbgMsg("term0==100",term0.equals(100)) ;
        Itk.dbgMsg("term0==3.14",term0.equals(3.14)) ;
        Itk.dbgMsg("term1==foo",term1.equals("foo")) ;
        Itk.dbgMsg("term1==bar",term1.equals("bar")) ;
        Itk.dbgMsg("term1==term0",term1.equals(term0)) ;
        Itk.dbgMsg("term0==term1",term0.equals(term1)) ;
        Itk.dbgMsg("term4==term1",term4.equals(term1)) ;
        Itk.dbgMsg("term4==term5",term4.equals(term5)) ;
        Itk.dbgMsg("term3==term4",term3.equals(term4)) ;
    }

} // class ClassFinderTest

