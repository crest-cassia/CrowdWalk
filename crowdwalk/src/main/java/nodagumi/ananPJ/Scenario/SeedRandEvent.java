// -*- mode: java; indent-tabs-mode: nil -*-
/** SeedRandEvent.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2019/06/23 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2019/06/23]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.Scenario.Scenario;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.* ;

//======================================================================
/**
 * This event cause to set random seed.
 * <pre>
 *  { "type" : "SeedRand" 
 *    "atTime" : __Time__,
 *    "seed" : __Integer__
 *  }
 * </pre>
 * <p>
 * "seed" で指定した値を CrowdWalkSimulator の random にセットする。
 * </p><p>
 * "Dump" イベントなどで状態をセーブした後、再スタートで全く同じ現象を
 * 発生させたい場合、
 * "Dump" イベントの時刻（および再スタートする際のruleのstart time）
 * の１秒後を、"atTime" に指定するする。
 * "Dump" 側のシナリオと、再スタート側のシナリオ両方に同じように記述すること。
 * </p><p>
 * サンプル：
 * "./sample/generatedTown/gridTown02d.scnr.json" および
 * "./sample/generatedTown/gridTown02ds.scnr.json". 
 * </p>
 */
public class SeedRandEvent extends EventBase {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * seed value
     */
    public long seed ;
    
    //------------------------------------------------------------
    /**
     * JSON Term による setup.
     * format と filename を設定する。
     */
    public void setupByJson(Scenario _scenario,
                            Term _eventDef) {
        super.setupByJson(_scenario, _eventDef) ;
    }

    //------------------------------------------------------------
    /**
     * SeedRand イベント発生処理。
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(SimTime currentTime, NetworkMap map) {
        if(eventDef.hasArg("seed")) {
            seed = eventDef.getArgInt("seed") ;
        } else {
            Itk.logError("SeedRandEvent", "must specify 'seed' value.") ;
            Itk.quitByError() ;
        }

        getSimulator().getRandom().setSeed(seed) ;
        
	return true ;
    }

    //------------------------------------------------------------
    /**
     * SeedRand イベント発生逆処理。
     * 何もしない。
     * @param currentTime : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(SimTime currentTime, NetworkMap map) {
	return true ;
    }

    //------------------------------------------------------------
    /**
     * 文字列化 後半
     */
    public String toStringTail() {
        return (super.toStringTail() +
                "," + "seed=" + seed) ;
    }
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class RubyEvent

