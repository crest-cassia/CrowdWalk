// -*- mode: java; indent-tabs-mode: nil -*-
/** Json Formatter テスト
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
 * Json Formatter Test
 */
public class JsonFormatter_Test {
    //============================================================
    class Foo {
	public int x ;
	public Foo(int _x) { x = _x ; } ;
    }

    static public JsonFormatter<Foo> formatter = new JsonFormatter<Foo>() ;
    static {
	formatter
	    .addMember(formatter.new Member("x") {
		    public Object value(Foo f) {
			return Integer.valueOf(f.x) ; }})
	    .addMember(formatter.new Member("xx") {
		    public Object value(Foo f) {
			return Integer.valueOf(f.x * f.x) ; }})
	    .addMember(formatter.new Member("xxx") {
		    public Object value(Foo f) {
			return Integer.valueOf(f.x * f.x * f.x) ; }})
	    .addMember(formatter.new Member("1/x") {
		    public Object value(Foo f) {
			return Double.valueOf(1.0 / f.x) ; }})
            .addMember(formatter.new Member("name") {
                    public Object value(Foo f) {
                        return Integer.toString(f.x) ; }}) ;
        //formatter.setOverallStyle("RecordPerLine") ;
        formatter.setOverallStyle("RecordArray") ;
        //formatter.setOverallStyle("PrettyPrint") ;
    }

    //------------------------------------------------------------
    /**
     */
    @Test
    public void test1() {
	Itk.dbgVal("header", formatter.outputHeaderToBuffer()) ;

	Foo f1 = new Foo(1) ;
	Foo f2 = new Foo(2) ;
	Foo f3 = new Foo(3) ;
	Itk.dbgVal("f1", formatter.outputRecordToBuffer(f1)) ;
	Itk.dbgVal("f2", formatter.outputRecordToBuffer(f2)) ;
	Itk.dbgVal("f3", formatter.outputRecordToBuffer(f3)) ;

	Itk.dbgVal("tailer", formatter.outputTailerToBuffer()) ;
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
	formatter.outputAllRecordToLoggerInfo(logger, ff) ;
	formatter.outputTailerToLoggerInfo(logger) ;
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
	formatter.outputAllRecordToStream(System.out, ff) ;
	formatter.outputTailerToStream(System.out) ;
    }

} // class JsonFormatter_Test

