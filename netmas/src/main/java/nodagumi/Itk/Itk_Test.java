// -*- mode: java -*-
/** Itk Test
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
import junit.framework.TestCase;

import nodagumi.Itk.Itk ;

//======================================================================
/**
 * Itk Test
 */
public class Itk_Test extends TestCase {
    //------------------------------------------------------------
    /**
     * test dbgMsg
     */
    @Test
    public void test_dbgMsg() {
	int x = 1 ;
	String y = "foo bar baz" ;
	int z[] = {1, 2, 3} ;
	int w[][] = {{1,2},{3,4}} ;
	Itk.dbgMsg(x) ;
	Itk.dbgMsg("x", x) ;
	Itk.dbgMsg("y", y) ;
	Itk.dbgMsg("z", z) ;
	Itk.dbgMsg("w", w) ;
    }

    //------------------------------------------------------------
    /**
     * test random URI
     */
    @Test
    public void test_randomURI() {
	Itk.dbgMsg("randomURI()") ;
	Itk.dbgMsg("uri=", Itk.genUriRandom()) ;
    }

    //------------------------------------------------------------
    /**
     * test CurrentTimeStr
     */
    @Test
    public void test_getCurrentTimeStr() {
	Itk.dbgMsg("getCurrentTimeStr()") ;
	Itk.dbgMsg("time=", Itk.getCurrentTimeStr()) ;
    }

} // class Itk_Test

