// -*- mode: java; indent-tabs-mode: nil -*-
/** CSV Formatter テスト
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/06/13 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/06/13]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.Test;
import java.util.logging.Logger;

import nodagumi.Itk.*;

//======================================================================
/**
 * CSV Formatter Test
 */
public class CsvFormatter_Test {
    //============================================================
    class Foo {
	public int x ;
	public Foo(int _x) { x = _x ; } ;
    }

    static public CsvFormatter<Foo> formatter = new CsvFormatter<Foo>() ;
    static {
	formatter
	    .addColumn(formatter.new Column("x") {
		    public String value(Foo f) {
			return Integer.toString(f.x) ; }})
	    .addColumn(formatter.new Column("xx") {
		    public String value(Foo f) {
			return Integer.toString(f.x * f.x) ; }})
	    .addColumn(formatter.new Column("xxx") {
		    public String value(Foo f) {
			return Integer.toString(f.x * f.x * f.x) ; }})
	    .addColumn(formatter.new Column("1/x") {
		    public String value(Foo f) {
			return Double.toString(1.0 / f.x) ; }}) ;
    }

    //------------------------------------------------------------
    /**
     */
    @Test
    public void test1() {
	Itk.dbgVal(formatter.outputHeaderToBuffer()) ;
	Foo f1 = new Foo(1) ;
	Foo f2 = new Foo(2) ;
	Foo f3 = new Foo(3) ;
	Itk.dbgVal(formatter.outputValueToBuffer(f1)) ;
	Itk.dbgVal(formatter.outputValueToBuffer(f2)) ;
	Itk.dbgVal(formatter.outputValueToBuffer(f3)) ;
    }

    //------------------------------------------------------------
    /**
     */
    @Test
    public void test2() {
	Logger logger = Logger.getLogger("foo") ;
	ArrayList<Foo> ff = new ArrayList<Foo>() ;
	ff.add(new Foo(1)) ;
	ff.add(new Foo(2)) ;
	ff.add(new Foo(3)) ;

	formatter.outputHeaderToLoggerInfo(logger) ;
	formatter.outputAllValueToLoggerInfo(logger, ff) ;
    }

    //------------------------------------------------------------
    /**
     */
    @Test
    public void test3() {
	ArrayList<Foo> ff = new ArrayList<Foo>() ;
	ff.add(new Foo(1)) ;
	ff.add(new Foo(2)) ;
	ff.add(new Foo(3)) ;

	formatter.outputHeaderToStream(System.out) ;
	formatter.outputAllValueToStream(System.out, ff) ;
    }

} // class CsvFormatter_Test

