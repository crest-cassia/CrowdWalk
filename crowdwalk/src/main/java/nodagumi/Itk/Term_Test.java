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
import java.util.ArrayList ;
import java.util.List ;
import java.util.Arrays ;

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
    public void test_updateObject() {
        String str0 = "{'a':'foo', 'b':'def', 'c':1, 'jkl':null, 'x':[1,2,{},'234',null,{'xx':'yy','zz':'123'}], 'y':{'':'bar', 'a':3.21, 'b':[]}, 'zzz':'kkkkk'}" ;
        str0 = str0.replaceAll("'","\"") ;
        Itk.dbgVal("str0",str0) ;
        Term term0 = Term.newByJson(str0) ;
        Itk.dbgVal("term0",term0) ;

        String str1 = "{'a':1, 'x':[3,4,null],'y':{'b':[10,20,30],'c':2.718}, 'z':{'a':1}}" ;
        str1 = str1.replaceAll("'","\"") ;
        Itk.dbgVal("str1",str1) ;
        Term term1 = Term.newByJson(str1) ;
        Itk.dbgVal("term1",term1) ;

        term0.updateObjectFacile(term1, true) ;
        Itk.dbgVal("term0",term0) ;
    }
    //------------------------------------------------------------
    /**
     */
    //@Test
    public void test_scanJsonComment() {
        String str0 = "{'_':'foo', '_':'def', '_':1, 'jkl':null, 'x':[1,2,{},'234',null,{'xx':'yy','zz':'123'}], 'y':{'':'bar', 'a':3.21, 'b':[]}, '_':'kkkkk'}" ;
        str0 = str0.replaceAll("'","\"") ;
        Itk.dbgVal("str0",str0) ;
        Term term0 = Term.newByJson(str0) ;
        Itk.dbgVal("term0",term0) ;
    }
    //------------------------------------------------------------
    /**
     */
    //@Test
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
    //@Test
    public void test_array() {
        Itk.dbgMsgMethodInfo() ;

        List<String> array0 = Arrays.asList("foo","bar","baz") ;
        Itk.dbgVal("array0",array0) ;
        Term term0 = new Term(array0, false) ;
        Itk.dbgVal("term0",term0) ;
        Itk.dbgVal("term0(JSON)",term0.toJson()) ;

        String str1 = "['a',1,2.3,{'':'foo','bar':[],'baz':{}}]" ;
        str1 = str1.replaceAll("'","\"") ;
        Itk.dbgVal("str1",str1) ;
        Term term1 = Term.newByJson(str1) ;
        Itk.dbgVal("term1",term1) ;
        Itk.dbgVal("term1(JSON)",term1.toJson()) ;
    }

    //------------------------------------------------------------
    /**
     */
    //@Test
    public void test_construct() {
        Itk.dbgMsgMethodInfo() ;

        Term term0 = new Term() ;
        Itk.dbgVal("term0",term0) ;
        Itk.dbgVal("term0(JSON)",term0.toJson()) ;

        Term term1 = new Term("foo", true) ;
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
        Term term4 = new Term("foo",b4, true) ;
        Itk.dbgVal("term4",term4) ;
        Itk.dbgVal("term4(JSON)",term4.toJson()) ;

        Term term5 = new Term("foo", true) ;
        term5.setArg("bar","aho") ;
        term5.setArg("baz",2.34) ;
        Itk.dbgVal("term5", term5) ;
        Itk.dbgVal("term5(JSON)", term5.toJson()) ;


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

    //------------------------------------------------------------
    /**
     */
    //@Test
    public void test_isIntDouble() {
        String str = "{'a':123, 'b':0, 'c': 12.34, 'd': 0.0, 'e':'123'}" ;
        str = str.replaceAll("'","\"") ;
        Itk.dbgVal("str",str) ;
        Term term = Term.newByJson(str) ;
        Itk.dbgVal("term", term) ;
        Itk.dbgVal("term.getArg('a')",term.getArg("a")) ;
        Itk.dbgVal("term.getArg('b')",term.getArg("b")) ;
        Itk.dbgVal("term.getArg('c')",term.getArg("c")) ;
        Itk.dbgVal("term.getArg('d')",term.getArg("d")) ;
        Itk.dbgVal("term.getArg('e')",term.getArg("e")) ;
        Itk.dbgVal("term.getArg('a').isInt()",term.getArgTerm("a").isInt()) ;
        Itk.dbgVal("term.getArg('b').isInt()",term.getArgTerm("b").isInt()) ;
        Itk.dbgVal("term.getArg('c').isInt()",term.getArgTerm("c").isInt()) ;
        Itk.dbgVal("term.getArg('d').isInt()",term.getArgTerm("d").isInt()) ;
        Itk.dbgVal("term.getArg('e').isInt()",term.getArgTerm("e").isInt()) ;
        Itk.dbgVal("term.getArg('a').isDouble()",term.getArgTerm("a").isDouble()) ;
        Itk.dbgVal("term.getArg('b').isDouble()",term.getArgTerm("b").isDouble()) ;
        Itk.dbgVal("term.getArg('c').isDouble()",term.getArgTerm("c").isDouble()) ;
        Itk.dbgVal("term.getArg('d').isDouble()",term.getArgTerm("d").isDouble()) ;
        Itk.dbgVal("term.getArg('e').isDouble()",term.getArgTerm("e").isDouble()) ;
    }

} // class ClassFinderTest

