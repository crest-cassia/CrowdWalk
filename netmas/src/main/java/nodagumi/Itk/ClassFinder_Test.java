// -*- mode: java -*-
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

import nodagumi.Itk.ClassFinder;

import static org.junit.Assert.*;

import org.junit.Test;
import junit.framework.TestCase;

//======================================================================
/**
 * description of Test.
 */
public class ClassFinder_Test extends TestCase {
    //------------------------------------------------------------
    /**
     * simple get test
     */
    @Test
    public void test_get() {
	Itk.dbgMsgMethodInfo() ;
	try {
	    String name = "java.lang.String" ;
	    Class<?> klass = ClassFinder.get(name) ;
	    Itk.dbgMsg("name=", name) ;
	    Itk.dbgMsg("class=", klass.toString()) ;
	} catch (Exception ex) {
	    ex.printStackTrace() ;
	}
    }

    //------------------------------------------------------------
    /**
     * alias test
     */
    @Test
    public void test_alias() {
	Itk.dbgMsgMethodInfo() ;
	try {
	    ClassFinder.alias("MyString","java.lang.String") ;
	    String name = "MyString" ;
	    String fullname = ClassFinder.fullname(name) ;
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
    @Test
    public void test_new() {
	Itk.dbgMsgMethodInfo() ;
	try {
	    ClassFinder.alias("MyString","java.lang.String") ;
	    String name = "MyString" ;
	    String str = (String)ClassFinder.newByName(name) ;
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
    @Test
    public void test_json() {
	Itk.dbgMsgMethodInfo() ;
	try {
	    ClassFinder.aliasByJson(" { " +
				    "'MyString' : 'java.lang.String'," +
				    "'Me' : 'nodagumi.Itk.ClassFinder_Test'," +
				    "'You' : 'nodagumi.Itk.ClassFinder'," +
				    "}") ;
	    String str = (String)ClassFinder.newByName("MyString") ;
	    str += "foo" ;
	    Itk.dbgMsg("str=", str) ;
	    Itk.dbgMsg("Me=", ClassFinder.get("Me")) ;
	    Itk.dbgMsg("You=", ClassFinder.get("You")) ;
	} catch (Exception ex) {
	    ex.printStackTrace() ;
	}
    }

} // class ClassFinder_Test

