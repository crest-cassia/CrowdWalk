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

//======================================================================
/**
 * description of class Foo.
 */
public class ClassFinder {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * description of DefaultValues.
     */
    final public int DefaultOne = 1 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * description of attribute baz.
     */
    public int baz ;

    //------------------------------------------------------------
    /**
     * description of method initialize
     * @param _baz about argument baz.
     */
    public ClassFinder(int _baz){
	baz = _baz ;
    }

    //------------------------------------------------------------
    /**
     * description of method foo
     * @param bar about argument bar
     * @return about return value
     */
    public int foo(int bar) {
	baz = bar ;
	return 1 ;
    }

} // class ClassFinder

