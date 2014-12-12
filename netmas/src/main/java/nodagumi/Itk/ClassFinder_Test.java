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
	System.out.println("test_get") ;
	try {
	    String name = "java.lang.String" ;
	    Class<?> klass = ClassFinder.get(name) ;
	    System.out.println("name=" + name) ;
	    System.out.println("class=" + klass.toString()) ;
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
	System.out.println("test_alias") ;
	try {
	    ClassFinder.alias("MyString","java.lang.String") ;
	    String name = "MyString" ;
	    String fullname = ClassFinder.fullname(name) ;
	    System.out.println("name=" + name) ;
	    System.out.println("fullname=" + fullname) ;
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
	System.out.println("test_new") ;
	try {
	    ClassFinder.alias("MyString","java.lang.String") ;
	    String name = "MyString" ;
	    String str = (String)ClassFinder.newByName(name) ;
	    str += "foo" ;
	    System.out.println("str=" + str) ;
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
	System.out.println("test_json") ;
	try {
	    ClassFinder.aliasByJson(" { " +
				    "'MyString' : 'java.lang.String'," +
				    "'Me' : 'nodagumi.Itk.ClassFinder_Test'," +
				    "'You' : 'nodagumi.Itk.ClassFinder'," +
				    "}") ;
	    String str = (String)ClassFinder.newByName("MyString") ;
	    str += "foo" ;
	    System.out.println("str=" + str + "\n" +
			       "Me=" + ClassFinder.get("Me").toString() + "\n" +
			       "You=" + ClassFinder.get("You").toString() + "\n") ;
	} catch (Exception ex) {
	    ex.printStackTrace() ;
	}
    }

} // class ClassFinder_Test

