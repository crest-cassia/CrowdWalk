// -*- mode: java; indent-tabs-mode: nil -*-
/** PlacedEvent.java
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

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
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
    public String placeId = "" ;

    //----------------------------------------
    /**
     * JSON Term による setup
     */
    public void setupByJson(Scenario _scenario,
                            Term eventDef) {
        super.setupByJson(_scenario, eventDef) ;

        placeTag = eventDef.getArgTerm("placeTag") ;
        if(placeTag == null) {
            placeId = eventDef.getArgString("placeId") ;
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

	placeTag = new Term(columns.nth(2), true) ;
    }

    //----------------------------------------
    /**
     * placeTag もしくは placeId を持つか調べる。
     */
    public boolean checkTagOrId(OBNode mapObject) {
        if(placeTag == null) {
            return mapObject.ID.equals(placeId) ;
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

