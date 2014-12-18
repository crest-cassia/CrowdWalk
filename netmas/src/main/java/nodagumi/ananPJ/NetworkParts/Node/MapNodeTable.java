// -*- mode: java; indent-tabs-mode: nil -*-
/** MapNode のテーブル
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/18 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/18]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.NetworkParts.Node;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkParts.Node.MapNode ;

import nodagumi.Itk.*;

//======================================================================
/**
 * MapNode のテーブル。 
 * ArrayList<MapNode> を置き換え、各種機能を提供する。
 */
public class MapNodeTable extends ArrayList<MapNode> {
    //------------------------------------------------------------
    /**
     * Constructor with no args
     */
    public MapNodeTable() { 
        super() ; 
    }

    //------------------------------------------------------------
    /**
     * Constructor from Collection
     */
    public MapNodeTable(Collection<MapNode> origin) {
        this() ;
        for(MapNode node : origin) {
            add(node) ;
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

} // class MapNodeTable

