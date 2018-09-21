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

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;

import nodagumi.Itk.* ;

//============================================================
/**
 * Gate Event
 */
abstract public class GateEvent extends PlacedEvent {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ゲートを示す tag
     * デフォルトでは、placeTag と同じもの。
     */
    public Term gateTag = null ;
    
    /**
     * その他、拡張用の情報。
     * シナリオで与えられる定義情報。json のみ対応。
     */
    public Term eventDef = null ;

    //----------------------------------------
    /**
     * JSON Term による setup
     */
    public void setupByJson(Scenario _scenario,
                            Term _eventDef) {
        super.setupByJson(_scenario, _eventDef) ;

        eventDef = _eventDef ;

        gateTag = _eventDef.getArgTerm("gateTag") ;
        if(gateTag == null) {
            gateTag = placeTag ;
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

	gateTag = placeTag ;
    }

    //----------------------------------------
    /**
     * 文字列化 後半
     */
    public String toStringTail() {
	return (super.toStringTail() + "," + "gate=" + gateTag);
    }
} // class GateEvent

