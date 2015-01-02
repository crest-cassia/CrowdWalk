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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import nodagumi.Itk.ClassFinder;
import nodagumi.Itk.Term;

import static org.junit.Assert.*;
import org.junit.Test;
//import junit.framework.TestCase;

//======================================================================
/**
 * description of Test.
 */
public class ClassFinder_Test {
    //------------------------------------------------------------
    /**
     * newInstance 
     */
    @Test
    public void test_methodCall() {
        Itk.dbgMsgMethodInfo() ;

        ClassFinder.primal.registerClassDummy(Foo10a.class) ;
        ClassFinder.primal.registerAlias("Foo10a",Foo10a.class) ;
        Itk.dbgVal("Foo10a",ClassFinder.primal.getClassDummy(Foo10a.class)) ;

        try {
            ClassFinder.primal.callMethodForClass("Foo10a","bar0",false,"100") ;
        } catch (Exception ex){
            ex.printStackTrace() ;
        }
    }

    //------------------------------------------------------------
    /**
     * newInstance 
     */
    static public class Foo10a {
        public Foo10a(){}
        public Foo10a(int i) { Itk.dbgMsg("new(int)",i) ;}
        public Foo10a(Object obj){ Itk.dbgMsg("new(Object)", obj);}
        public void bar0() { Itk.dbgMsg("Foo10a","bar0()") ; }
        public void bar0(String s) { Itk.dbgMsg("Foo10a","bar0() " + s) ; }
    }

    //@Test
    public void test_newInstance() {
        Itk.dbgMsgMethodInfo() ;

        try {
            Foo10a foo = new Foo10a() ;
            //String foo = "aaa" ;
            //Term foo = new Term("foo's foo") ;
            Itk.dbgVal("foo",foo) ;

            Class<?> klass = foo.getClass() ;
            Itk.dbgVal("klass",klass) ;

            Constructor<?> constructor = 
                klass.getConstructor(Object.class) ;
            Itk.dbgVal("constructor",constructor) ;


            Object obj = klass.newInstance() ;
            Itk.dbgVal("obj",obj) ;

            Object obj2 = constructor.newInstance(3.14) ;
            Itk.dbgVal("obj2",obj2) ;

            Class<?> klass2 = Itk.class ;
            Itk.dbgVal("klass2",klass2) ;

            Method method2 = klass2.getMethod("dbgMsg",Object.class) ;
            Itk.dbgVal("method2",method2) ;

            method2.invoke(null,"test") ;
            
        } catch (Exception ex) {
            ex.printStackTrace() ;
        }
    }

    //------------------------------------------------------------
    /**
     * simple get test
     */
    //@Test
    public void test_get() {
        Itk.dbgMsgMethodInfo() ;
        try {
            String name = "java.lang.String" ;
            Class<?> klass = ClassFinder.primal.get(name) ;
            Itk.dbgMsg("name=", name) ;
            Itk.dbgMsg("class=", klass.toString()) ;

            Itk.dbgMsg("true=", ClassFinder.primal.isClassName(name)) ;
            Itk.dbgMsg("false=", ClassFinder.primal.isClassName("hogehoge")) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
        }
    }

    //------------------------------------------------------------
    /**
     * alias test
     */
    //@Test
    public void test_alias() {
        Itk.dbgMsgMethodInfo() ;
        try {
            ClassFinder.primal.alias("MyString","java.lang.String") ;
            String name = "MyString" ;
            String fullname = ClassFinder.primal.fullname(name) ;
            Itk.dbgMsg("name=", name) ;
            Itk.dbgMsg("fullname=", fullname) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
        }
    }

    //------------------------------------------------------------
    /**
     * new test
     */
    //@Test
    public void test_new() {
        Itk.dbgMsgMethodInfo() ;
        try {
            ClassFinder.primal.alias("MyString","java.lang.String") ;
            String name = "MyString" ;
            String str = (String)ClassFinder.primal.newByName(name) ;
            str += "foo" ;
            Itk.dbgMsg("str=", str) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
        }
    }

    //------------------------------------------------------------
    /**
     * new test
     */
    //@Test
    public void test_json() {
        Itk.dbgMsgMethodInfo() ;
        try {
            ClassFinder.primal.aliasByJson(" { " +
                                           "'MyString' : 'java.lang.String'," +
                                           "'Me' : 'nodagumi.Itk.ClassFinder_Test'," +
                                           "'You' : 'nodagumi.Itk.ClassFinder'" +
                                           "}") ;
            String str = (String)ClassFinder.primal.newByName("MyString") ;
            str += "foo" ;
            Itk.dbgMsg("str=", str) ;
            Itk.dbgMsg("Me=", ClassFinder.primal.get("Me")) ;
            Itk.dbgMsg("You=", ClassFinder.primal.get("You")) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
        }
    }

} // class ClassFinder_Test

