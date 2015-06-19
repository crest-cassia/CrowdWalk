// -*- mode: java; indent-tabs-mode: nil -*-
/** ItkRuby test
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/06/19 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/06/19]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import static org.junit.Assert.*;
import org.junit.Test;
//import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.embed.ScriptingContainer;

import nodagumi.Itk.Itk;


//======================================================================
/**
 * Jruby Test
 */
public class ItkRuby_Test {
    //------------------------------------------------------------
    /**
     * description of method initialize
     * @param _baz about argument baz.
     */
    //@Test
    public void testA() {
	String script = ("x = 1;\n" +
			 "y = 2;\n" +
			 "x + y") ;
	Ruby ruby = Ruby.newInstance() ;
	IRubyObject result = ruby.evalScriptlet(script) ;
	Itk.dbgVal("result", result) ;
    }

    //------------------------------------------------------------
    /**
     * description of method initialize
     * @param _baz about argument baz.
     */
    //@Test
    public void testB() {
	String script = ("$x = $x.to_i + 1;\n" +
			 "y = 2;\n" +
			 "$x + y") ;
	Ruby ruby = Ruby.newInstance() ;
	Itk.dbgVal("getRubyHome()", ruby.evalScriptlet("p :ahooooooooo")) ;
	Itk.dbgVal("getRubyHome()", ruby.evalScriptlet("system(\"ls\")")) ;
	Itk.dbgVal("result", ruby.evalScriptlet(script)) ;
	Itk.dbgVal("result", ruby.evalScriptlet(script)) ;
	Itk.dbgVal("result", ruby.evalScriptlet(script)) ;
	Itk.dbgVal("result", ruby.evalScriptlet(script)) ;
	Itk.dbgVal("result", ruby.evalScriptlet(script)) ;
    }

    //------------------------------------------------------------
    /**
     * description of method initialize
     * @param _baz about argument baz.
     */
    //@Test
    public void testC() {
	Ruby ruby = Ruby.newInstance() ;
	Itk.dbgVal("class", 
		   ruby.evalScriptlet("class Foo ; attr_accessor :bar ;" +
				      " def baz() ; p [:bar, @bar] ; 1 ; end ;" +
				      "end")) ;
	Itk.dbgVal("new", ruby.evalScriptlet("$x = Foo.new()")) ;
	Itk.dbgVal("bar", ruby.evalScriptlet("$x.bar = 3")) ;
	Itk.dbgVal("baz", ruby.evalScriptlet("$x.baz()")) ;
    }

    //------------------------------------------------------------
    /**
     * description of method initialize
     * @param _baz about argument baz.
     */
    @Test
    public void testD() {
	Ruby ruby = Ruby.newInstance() ;
	ScriptingContainer container = new ScriptingContainer() ;

	Itk.dbgVal("class", 
		   container.runScriptlet("class Foo ; attr_accessor :bar ;" +
				      " def baz() ; p [:bar, @bar] ; 1 ; end ;" +
				      "end")) ;
	Itk.dbgVal("new", container.runScriptlet("$x = Foo.new()")) ;
	Itk.dbgVal("bar", container.runScriptlet("$x.bar = 3")) ;
	Itk.dbgVal("baz", container.runScriptlet("$x.baz()")) ;

	Object foo = container.runScriptlet("Foo.new()") ;
	Itk.dbgVal("foo", foo) ;
	Itk.dbgVal("bar", container.callMethod(foo,"bar=", new Integer(100)));
	Itk.dbgVal("baz", container.callMethod(foo,"baz")) ;
    }

} // class ItkRuby_Test

