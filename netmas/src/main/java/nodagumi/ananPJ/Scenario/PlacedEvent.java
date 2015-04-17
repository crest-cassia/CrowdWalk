// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk EventBase.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/01/29 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/01/29]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;

import nodagumi.Itk.* ;

//============================================================
/**
 * Placed Event
 * PlaceTag により、発生場所を指定できるイベント
 */
abstract public class PlacedEvent extends EventBase {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 場所を指定するタグ
     */
    public Term placeTag = null ;
    public int placeId = 0 ;

    //----------------------------------------
    /**
     * JSON Term による setup
     */
    public void setupByJson(Scenario _scenario,
                            Term eventDef) {
        super.setupByJson(_scenario, eventDef) ;

        placeTag = eventDef.getArgTerm("placeTag") ;
        if(placeTag == null) {
            placeId = eventDef.getArgInt("placeId") ;
        }
    }

    //----------------------------------------
    /**
     * CSV による setup
     */
    @Override
    public void setupByCsvColumns(Scenario _scenario,
				  ShiftingStringList columns) {
	super.setupByCsvColumns(_scenario, columns) ;

	placeTag = new Term(columns.nth(2)) ;
    }

    //----------------------------------------
    /**
     * placeTag もしくは placeId を持つか調べる。
     */
    public boolean checkTagOrId(OBNode mapObject) {
        if(placeTag == null) {
            return mapObject.ID == placeId ;
        } else {
            return mapObject.hasTag(placeTag) ;
        }
    }

    //----------------------------------------
    /**
     * 文字列化 後半
     */
    public String toStringTail() {
        if(placeTag == null) {
            return (super.toStringTail() + "," + "id=" + placeId);
        } else {
            return (super.toStringTail() + "," + "place=" + placeTag);
        }
    }
} // class PlacedEvent

