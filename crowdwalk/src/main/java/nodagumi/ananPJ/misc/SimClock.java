// -*- mode: java; indent-tabs-mode: nil -*-
/** Clock in Simulation
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/07/07 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/07/07]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.misc;

import java.lang.Math;
import java.lang.Integer;

import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.Itk;

//======================================================================
/**
 * シミュレーション内での時計
 */
public class SimClock extends SimTime {
    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public SimClock() { super() ;}

    /**
     * コンストラクタ
     */
    public SimClock(String originTimeString) {
        super(originTimeString) ;
    }

    /**
     * コンストラクタ
     */
    public SimClock(String originTimeString, double tickUnit) {
        super(originTimeString, tickUnit) ;
    }

    /**
     * コンストラクタ
     */
    public SimClock(SimClock origin) {
        super(origin) ;
    }

    //------------------------------------------------------------
    // 初期化関連。
    //------------------------------------------------------------
    // 操作
    //------------------------------------------------------------
    /**
     * 時刻前進。１tick。
     */
    public SimClock advance() {
        advanceTick(1) ;
        return this ;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class SimClock
