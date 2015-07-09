// -*- mode: java; indent-tabs-mode: nil -*-
/** Scenario class
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/01/21 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/01/21]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import java.lang.Double ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.misc.SimClock;
import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.* ;

//======================================================================
/**
 * Scenario Class.
 * <h2> シナリオファイル形式 </h2>
 * シナリオファイルは、イベントの配列で、JSON で記述する。
 * JSON の形式は以下の通り。
 * <pre>
 *   [
 *     __Event__,
 *     __Event__,
 *     ...
 *   ]
 * </pre>
 * <code>__Event__</code> としては、以下のものが用意されている。
 * <ul>
 *  <li>{@link InitiateEvent "Initiate"}</li>
 *  <li>{@link FinishEvent "Finish"}</li>
 *  <li>{@link AlertEvent "Alert"}</li>
 *  <li>{@link SetTagEvent "SetTag", "AddTag", "RemoveTag"}</li>
 *  <li>{@link ShutOffEvent "ShutOff"}</li>
 *  <li>{@link OpenGateEvent "OpenGate"}</li>
 *  <li>{@link CloseGateEvent "CloseGate"}</li>
 *  <li>{@link PeriodicGateEvent "PeriodicGate"}</li>
 * </ul>
 */
public class Scenario {

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 起点となる時刻(絶対時刻)
     */
    private double originTime_obsolete = 0.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シミュレーション内クロック。
     * 本体は、EvacuationSimulation の中の clock。
     */
    private SimTime baseTime = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シナリオが終了したかどうか
     */
    private boolean finishP = false ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * イベント列
     * 時間順序準に並んでいるものとする。
     * （読み込み時にソートされる。）
     */
    public ArrayList<EventBase> eventList =
        new ArrayList<EventBase>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * イベントインデックス
     * イベントがどこまで進んでいるかを示す
     */
    private int eventIndex = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * イベントテーブル
     * event id から引けるようにしておく
     * 現状で、特に用はない。
     */
    private HashMap<String, EventBase> eventTable =
        new HashMap<String, EventBase>() ;

    //------------------------------------------------------------
    // 時刻処理
    //------------------------------------------------------------
    /**
     * クロックをセット。
     * 主として、EvacuationSimulator からセットされるはず。
     */
    public void setBaseTime(SimTime baseTime) {
        this.baseTime = baseTime ;
    }

    //------------------------------------------------------------
    /**
     * 終了かどうか
     * finish when FinishEvent occurs.
     */
    public boolean isFinished() {
        if(finishP)
            Itk.logInfo("finished by the end of scenario.") ;
        return finishP ;
    }

    //------------------------------------------------------------
    /**
     * 終了セット
     */
    public void letFinished() {
        letFinished(true) ;
    }

    //------------------------------------------------------------
    /**
     * 終了セット
     */
    public void letFinished(boolean flag) {
        finishP = flag ;
    }

    //------------------------------------------------------------
    // イベント操作
    //------------------------------------------------------------
    /**
     * イベントの登録
     * 同名イベントの二重登録は許さない
     */
    public Scenario addEvent(EventBase event) {
        eventList.add(event) ;
        if(event.hasId()) {
            if(eventTable.containsKey(event.id)) {
                Itk.logWarn("duplicated event id:", event.id) ;
                Itk.logWarn_("previous entry:", eventTable.get(event.id)) ;
                Itk.logWarn_("added entry:", event) ;
            } else {
                eventTable.put(event.id, event) ;
            }
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * イベントの整列
     */
    public void sortEvents(){
        Collections.sort(eventList,
                         new Comparator<EventBase>() {
                             public int compare(EventBase event0,
                                                EventBase event1) {
                                 SimTime time0 = event0.atTime ;
                                 SimTime time1 = event1.atTime ;
                                 return time0.compareTo(time1) ;
                             }
                         }) ;
    }

    //------------------------------------------------------------
    /**
     * セットアップ最終処理
     */
    public void finalizeSetup(SimClock clock) {
        // イベントが登録されて居ない場合のデフォルト
        if(eventList.size() == 0) {
            EventBase defaultEvent = new InitiateEvent() ;
            addEvent(defaultEvent) ;
        }
        // 最初の InitiateEvent からシミュレーション開始時刻を取り出す。
        int countInitiate = 0 ;
        String initiateTimeString = null ;
        for(EventBase event : eventList) {
            if(event instanceof InitiateEvent) {
                countInitiate += 1 ;
                initiateTimeString = event.atTimeString ;
            }
        }
        if(countInitiate != 1) {
            // Initiate Event はちょうど１回のみでないといけな。
            Itk.logError("No or multiple Initiate Event",
                         "count=", countInitiate,
                         "time=", initiateTimeString) ;
            System.exit(1) ;
        }
        // clock を元に、イベントの発生時刻を再設定。
        clock.setOriginTimeString(initiateTimeString, true) ;
        for(EventBase event : eventList) {
            event.setupAtTime(clock) ;
        }
        //イベントの整列
        sortEvents() ;
    }

    //------------------------------------------------------------
    // イベント発生チェック及び実行
    //------------------------------------------------------------
    /**
     * シナリオを時間まで進める。
     * @param relTime : 相対時刻
     * @param map : マップデータ
     * @return 進んだシナリオの数
     */
    public int update(SimTime currentTime, NetworkMap map) {
        int count = 0 ;
        for(int i = eventIndex ; i < eventList.size() ; i++) {
            EventBase event = eventList.get(i) ;
            if(eventList.get(i).tryOccur(currentTime, map)) {
                count++ ;
            } else {
                break ;
            }
        }
        eventIndex += count ;
        return count ;
    }

    //------------------------------------------------------------
    // ファイル読み込み
    //------------------------------------------------------------
    /**
     * ファイル読み込み（トップレベル）
     */
    public int scanFile(String filename, SimClock clock) {
        if (filename == null || filename.isEmpty()) {
            finalizeSetup(clock) ;
            return 0;
        }

        if(filename.endsWith(".json")) {
            return scanJsonFile(filename, clock) ;
        } else if (filename.endsWith(".csv")) {
            return scanCsvFile(filename, clock) ;
        } else {
            Itk.logError("Unknown scenario file suffix:", filename) ;
            System.exit(1) ;
        }
        return 0 ; // never reach here.
    }

    //------------------------------------------------------------
    // JSON ファイル関連
    //------------------------------------------------------------
    /**
     * JSON ファイル読み込み
     * @param filename : JSON file name
     * @return 読み込んだイベント数
     */
    public int scanJsonFile(String filename, SimClock clock) {
        try {
            int nEvent = 0 ;
            BufferedReader reader = new BufferedReader(new FileReader(filename)) ;
            Term json = Term.newByScannedJson(JSON.decode(reader),true) ;
            if(json.isArray()) {
                for(Object _item : json.getArray()) {
                    Term eventDef = (Term)_item ;
                    if(eventDef.isObject()) { // 正しく構造を持っている場合
                        EventBase event =
                            scanJsonFileOneItem(eventDef) ;
                        if(event != null) {
                            addEvent(event) ;
                            nEvent++ ;
                        }
                    }
                }
                finalizeSetup(clock) ;
                Itk.logInfo("Load Scenario File", filename) ;
            } else {
                Itk.logError("Wrong scenario format in the file", filename) ;
                Itk.logError_("json=",json) ;
                System.exit(1) ;
            }
            return nEvent ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("Error in reading JSON file") ;
            Itk.logError_("filename=", filename) ;
            System.exit(1) ;
        }
        return -1 ; // never reach.
    }

    //------------------------------------------------------------
    /**
     * JSON による定義の1イベント分の読み込み
     * @param json : JSON 文字列 (Term で)
     * @return 生成したイベント
     */
    public EventBase scanJsonFileOneItem(Term eventDef) {
        try {
            String eventType = eventDef.getArgString("type") ;
            EventBase event = newEventByName(eventType) ;
            event.setupByJson(this, eventDef) ;
            return event ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("error in Event definition by JSON.") ;
            Itk.logError_("eventDef", eventDef) ;
            System.exit(1) ;
        }
        return null ; // never reach
    }

    //------------------------------------------------------------
    // CSV ファイル関連
    //------------------------------------------------------------
    /**
     * CSV ファイル読み込み
     * @param filename : CSV file name
     * @return 読み込んだイベント数
     */
    public int scanCsvFile(String filename, SimClock clock) {
        try {
            int nEvent = 0 ;
            BufferedReader reader = new BufferedReader(new FileReader(filename)) ;

            String line ;
            while((line = reader.readLine()) != null) {
                EventBase event = scanCsvOneLine(line) ;
                if(event != null) {
                    addEvent(event) ;
                    nEvent++ ;
                }
            }

            finalizeSetup(clock) ;

            return nEvent ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("Error in reading CSV file.") ;
            Itk.logError_("filename", filename) ;
            System.exit(1) ;
        }
        return -1 ; // never reach.
    }

    //------------------------------------------------------------
    /**
     * CSV 一行読み込み
     * @param line : CSV one line
     * @return 生成されたイベント
     */
    public EventBase scanCsvOneLine(String line) {
        // コメントは読み飛ばし
        if(line.startsWith("#")) return null ;

        try {
            ShiftingStringList columns = ShiftingStringList.newFromCsvRow(line) ;

            String tag = columns.nth(2) ;
            String command = columns.nth(3) ;

            String typeString ;
            if(tag.equals("START") ||
               tag.equals("RESPONSE")) {
                typeString = tag ;
            } else if(command.contains(":")) {
                String subcoms[] = command.split(":") ;
                typeString = subcoms[0] ;
            } else {
                typeString = command ;
            }
            EventBase event = newEventByName(typeString) ;
            event.setupByCsvColumns(this, columns) ;

            return event ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("error in CSV one line.") ;
            Itk.logError_("line", line) ;
            System.exit(1) ;
        }
        return null ; // never reach
    }

    //------------------------------------------------------------
    /**
     * describe
     */
    public void describe() {
        Itk.logInfo("Scenario", this) ;
        Itk.logInfo("---Events---") ;
        for(EventBase event : eventList) {
            Itk.logInfo(">>>>>", event.toString()) ;
        }
    }

    //============================================================
    // Event クラス関連
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Class Finder for Event
     */
    static public ClassFinder eventClassFinder =
        new ClassFinder() ;
    static {
        registerEventClass("Initiate",	InitiateEvent.class) ;
        registerEventClass("Finish",	FinishEvent.class) ;
        registerEventClass("Alert",	 AlertEvent.class) ;
        registerEventClass("ShutOff",	ShutOffEvent.class) ;
        registerEventClass("OpenGate",	OpenGateEvent.class) ;
        registerEventClass("CloseGate",	CloseGateEvent.class) ;
        registerEventClass("SetTag",	SetTagEvent.class) ;
        registerEventClass("AddTag",	SetTagEvent.class) ;
        registerEventClass("RemoveTag",	SetTagEvent.class) ;

        // for old CSV scenario file format
        registerEventClass("START", InitiateEvent.class) ;
        registerEventClass("RESPONSE", FinishEvent.class) ;
        registerEventClass("SET", SetTagEvent.class) ;
        registerEventClass("REMOVE", SetTagEvent.class) ;
        registerEventClass("ADD_STOP", CloseGateEvent.class) ;
        registerEventClass("REMOVE_STOP", OpenGateEvent.class) ;
        registerEventClass("STOP", ShutOffEvent.class) ;
        registerEventClass("EVACUATE", AlertEvent.class) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * 使用出来るイベントタイプの登録
     */
    static public void registerEventClass(Class<?> eventClass) {
        try {
            eventClassFinder.registerClassDummy(eventClass) ;
            String aliasName =
                (String)
                eventClassFinder.callMethodForClass(eventClass,
                                                    "getTypeName", true) ;
            registerEventClass(aliasName, eventClass) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("cannot process registerEventClass()") ;
            Itk.logError_("eventClass",eventClass) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 使用出来るイベントタイプの登録
     */
    static public void registerEventClass(String aliasName,
                                          Class<?> eventClass) {
        eventClassFinder.alias(aliasName, eventClass) ;
    }

    //------------------------------------------------------------
    /**
     * イベントクラスを得る。
     */
    static public Class<?> getEventClass(String className) 
        throws ClassNotFoundException
    {
        return eventClassFinder.get(className) ;
    }

    //------------------------------------------------------------
    /**
     * イベントクラスを見つけて、インスタンスを生成する。
     * @param className クラスの名前
     */
    public EventBase newEventByName(String className)
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException
    {
        return (EventBase)eventClassFinder.newByName(className) ;
    }

} // class Scenario

