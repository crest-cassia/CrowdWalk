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

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Scenario.*;

import nodagumi.Itk.* ;

//======================================================================
/**
 * Scenario Class
 */
public class Scenario {

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 起点となる時刻(絶対時刻)
     */
    private double originTime = 0.0 ;

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
     * 文字列から数値へ変換
     * 可能なフォーマットは、"HH:MM:SS" もしくは "HH:MM"
     * 返す値の単位は、秒。
     */
    static public double convertToTimeValue(String timeString) {
        try {
            return Itk.scanTimeStringToInt(timeString) ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.dbgErr("Exception",ex) ;
            System.exit(1) ;
        }
        return 0 ; // never reach here.
    }

    //------------------------------------------------------------
    /**
     * 数値から文字列へ変換
     * 秒から "HH:MM:SS" へ。
     * 少数以下は切り捨て。
     */
    static public String convertToTimeString(double timeVal) {
        return Itk.formatSecTime((int)timeVal) ;
    }

    //------------------------------------------------------------
    /**
     * 起点時刻取得
     */
    public double getOriginTime() {
        return originTime ;
    }

    //------------------------------------------------------------
    /**
     * 起点時刻文字列取得
     */
    public String getOriginTimeString() {
        return convertToTimeString(getOriginTime()) ;
    }

    //------------------------------------------------------------
    /**
     * 起点時刻セット
     */
    public double setOriginTime(double _originTime) {
        originTime = _originTime ;
        return originTime ;
    }

    //------------------------------------------------------------
    /**
     * 文字列による起点時刻セット
     */
    public double setOriginTimeByTimeString(String _originTimeInStr) {
        return setOriginTime(convertToTimeValue(_originTimeInStr)) ;
    }

    //------------------------------------------------------------
    /**
     * 相対時刻取得
     */
    public double calcRelativeTime(double absTime) {
        return absTime - getOriginTime() ;
    }

    //------------------------------------------------------------
    /**
     * 文字列からの相対時刻取得
     */
    public double calcRelativeTimeFromTimeString(String timeString) {
        return calcRelativeTime(convertToTimeValue(timeString)) ;
    }

    //------------------------------------------------------------
    /**
     * 絶対時刻取得
     */
    public double calcAbsoluteTime(double relTime) {
        return relTime + getOriginTime() ;
    }

    //------------------------------------------------------------
    /**
     * 文字列からの絶対時刻取得
     */
    public double calcAbsoluteTimeFromTimeString(String timeString) {
        return calcAbsoluteTime(convertToTimeValue(timeString)) ;
    }

    //------------------------------------------------------------
    /**
     * 終了かどうか
     */
    public boolean isFinished() {
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
                Itk.dbgWrn("duplicated event id:", event.id) ;
                Itk.dbgMsg("previous entry:", eventTable.get(event.id)) ;
                Itk.dbgMsg("added entry:", event) ;
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
                                 double time0 = event0.getRelativeTime() ;
                                 double time1 = event1.getRelativeTime() ;
                                 if(time0 > time1) {
                                     return 1 ;
                                 } else if(time0 < time1) {
                                     return -1 ;
                                 } else {
                                     return 0 ;
                                 }
                             }
                         }) ;
    }

    //------------------------------------------------------------
    /**
     * セットアップ最終処理
     */
    public void finalizeSetup() {
        // イベントが登録されて居ない場合のデフォルト
        if(eventList.size() == 0) {
            EventBase defaultEvent = new InitiateEvent() ;
            addEvent(defaultEvent) ;
        }
        //イベントの整列
        sortEvents() ;
        // 最初の InitiateEvent からシミュレーション開始時刻を取り出す。
        boolean findInitiate = false ;
        for(EventBase event : eventList) {
            if(event instanceof InitiateEvent) {
                findInitiate = true ;
                setOriginTime(event.atTime) ;
                break ;
            }
        }
        if(! findInitiate) {
            Itk.dbgWrn("No Initiate Event.") ;
        }
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
    public int advance(double relTime, NetworkMapBase map) {
        double absTime = calcAbsoluteTime(relTime) ;
        int count = 0 ;
        for(int i = eventIndex ; i < eventList.size() ; i++) {
            EventBase event = eventList.get(i) ;
            if(eventList.get(i).tryOccur(absTime, map)) {
                count++ ;
            } else {
                break ;
            }
        }
        eventIndex += count ;
        return count ;
    }

    //------------------------------------------------------------
    // JSON ファイル関連
    //------------------------------------------------------------
    /**
     * JSON ファイル読み込み
     * @param filename : JSON file name
     * @return 読み込んだイベント数
     */
    public int scanJsonFile(String filename) {
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
                finalizeSetup() ;
            } else {
                Itk.dbgErr("Wrong scenario format in the file:", filename) ;
                Itk.dbgMsg("json",json) ;
                System.exit(1) ;
            }
            return nEvent ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.dbgErr("Error in reading JSON file.") ;
            Itk.dbgMsg("filename", filename) ;
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
            Itk.dbgErr("error in Event definition by JSON.") ;
            Itk.dbgMsg("eventDef", eventDef) ;
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
    public int scanCsvFile(String filename) {
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

            finalizeSetup() ;

            return nEvent ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.dbgErr("Error in reading CSV file.") ;
            Itk.dbgMsg("filename", filename) ;
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
            Itk.dbgErr("error in CSV one line.") ;
            Itk.dbgMsg("line", line) ;
            System.exit(1) ;
        }
        return null ; // never reach
    }

    //------------------------------------------------------------
    /**
     * describe
     */
    public void describe() {
        Itk.dbgMsg("Scenario", this) ;
        Itk.dbgMsg("---Events---") ;
        for(EventBase event : eventList) {
            Itk.dbgMsg(">>>>>", event.toString()) ;
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
            Itk.dbgErr("cannot process registerEventClass()") ;
            Itk.dbgMsg("eventClass",eventClass) ;
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

