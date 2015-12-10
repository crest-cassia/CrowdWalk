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

import java.util.HashMap;
import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;

import nodagumi.Itk.ItkRuby;
import nodagumi.Itk.Itk;
import nodagumi.Itk.Term;


//======================================================================
/**
 * Jruby Test
 */
public class ItkRuby_Test {
    //------------------------------------------------------------
    /**
     * description of method initialize
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
     */
    //@Test
    public void testD() {
        Ruby ruby = Ruby.newInstance() ;
        ScriptingContainer container = new ScriptingContainer() ;

        // getCurrentDirectory()
        Itk.dbgVal("currentDir", container.getCurrentDirectory()) ;

        // runScriptlet
        Itk.dbgVal("class",
                   container.runScriptlet("class Foo ; attr_accessor :bar ;" +
                                          "def baz() ; p [:bar, @bar] ; 1 ; end ;" +
                                          "def foo();@bar += 1;end;" +
                                          "end")) ;
        Itk.dbgVal("new", container.runScriptlet("$x = Foo.new()")) ;
        Itk.dbgVal("bar", container.runScriptlet("$x.bar = 3")) ;
        Itk.dbgVal("baz", container.runScriptlet("$x.baz()")) ;

        // put, get, callMethod
        Object foo = container.runScriptlet("Foo.new()") ;
        Itk.dbgVal("foo", foo) ;
        Itk.dbgVal("put_bar", container.put(foo,"@bar", new Integer(100)));
        Itk.dbgVal("bar", container.get(foo,"@bar")) ;
        Itk.dbgVal("baz", container.callMethod(foo,"baz")) ;

        // parse, EvalUnit
        EvalUnit eunit = container.parse("$x.foo();$x.baz()") ;
        Itk.dbgVal("eunit",eunit) ;
        Itk.dbgVal("eunit.run",eunit.run()) ;
        Itk.dbgVal("eunit.run",eunit.run()) ;
        Itk.dbgVal("eunit.run",eunit.run()) ;

        // get class
        Object fooClass = container.runScriptlet("Foo") ;
        Object foo2 = container.callMethod(fooClass, "new") ;
        container.put(foo2,"@bar",new Integer(10)) ;
        Itk.dbgVal("foo2.baz", container.callMethod(foo2,"baz")) ;
    }

    //------------------------------------------------------------
    /**
     * description of method initialize
     */
    //@Test
    public void testE() {
        Ruby ruby = Ruby.newInstance() ;
        ScriptingContainer container = new ScriptingContainer() ;

        Itk.dbgVal("loadPath", container.getLoadPaths()) ;

        Term term = Term.newByJson("{'':'foo', 'bar':3}".replaceAll("'","\"")) ;
        container.put("@term",term) ;
        container.runScriptlet("p [:term, @term]") ;
        container.runScriptlet("p [:term_head, @term.getHead()]") ;
        container.runScriptlet("p [:term_bar, @term.getArgInt(\"bar\")]") ;
    }

    //------------------------------------------------------------
    /**
     * description of method initialize
     */
    //@Test
    public void testF() {
        Ruby ruby = Ruby.newInstance() ;
        ScriptingContainer container = new ScriptingContainer() ;

        Object strVal = container.runScriptlet(":foo.to_s()") ;
        Itk.dbgVal("strVal", strVal) ;
    }

    //------------------------------------------------------------
    /**
     * description of method initialize
     */
    @Test
    public void testG() {
        ItkRuby ruby = new ItkRuby() ;

        ruby.evalOnEngine("p ['1+2',1+2]") ;
        ruby.eval("p ['2+3',2+3]") ;
        ruby.setVariable("foo","fooString");
        ruby.eval("p [:foo,foo]") ;
        ruby.setVariable("foo",4);
        ruby.eval("p [:foo,foo]") ;
        Itk.dbgVal("LoadPath",ruby.getLoadPaths()) ;
        Itk.dbgVal("push Path",ruby.pushLoadPath("/usr/users/noda/lib/ruby")) ;
        Itk.dbgVal("LoadPath",ruby.getLoadPaths());
        Itk.dbgVal("def",
                   ruby.eval("def bar(x)",
                             "  p [:bar, x, x*x]",
                             "  return x*x",
                             "end")) ;
        Itk.dbgVal("bar(7)",ruby.eval("bar(7)")) ;
    }


} // class ItkRuby_Test

