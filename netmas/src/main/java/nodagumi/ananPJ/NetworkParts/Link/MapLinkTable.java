// -*- mode: java; indent-tabs-mode: nil -*-
/** MapLink のテーブル
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/18 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/18]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.NetworkParts.Link;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkParts.Link.MapLink ;

import nodagumi.Itk.*;

//======================================================================
/**
 * MapLink のテーブル。 
 * ArrayList<MapLink> を置き換え、各種機能を提供する。
 */
public class MapLinkTable extends ArrayList<MapLink> {
    //------------------------------------------------------------
    /**
     * Constructor with no args
     */
    public MapLinkTable() { 
        super() ; 
    }

    //------------------------------------------------------------
    /**
     * Constructor from Collection
     */
    public MapLinkTable(Collection<MapLink> origin) {
        this() ;
        for(MapLink link : origin) {
            add(link) ;
        }
    }
    //------------------------------------------------------------
    /**
     * description of method foo
     * @param bar:: about argument bar
     * @return about return value
     */
    //    public void foo(int bar) {
    //        baz = bar ;
    //    }

} // class MapLinkTable

