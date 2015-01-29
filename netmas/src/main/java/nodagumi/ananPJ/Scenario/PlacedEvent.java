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
     * 文字列化 後半
     */
    public String toStringTail() {
	return (super.toStringTail() + "," + "place=" + placeTag);
    }
} // class PlacedEvent

