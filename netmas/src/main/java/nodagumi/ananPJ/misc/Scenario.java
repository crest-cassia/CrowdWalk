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

package nodagumi.ananPJ.misc;

import java.lang.Double ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;

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
        registerEventClass("Initiate", InitiateEvent.class) ;
        registerEventClass("Finish", FinishEvent.class) ;
        registerEventClass("Alert", AlertEvent.class) ;
        registerEventClass("ShutOff", ShutOffEvent.class) ;
        registerEventClass("OpenGate", OpenGateEvent.class) ;
        registerEventClass("CloseGate", CloseGateEvent.class) ;
        registerEventClass("SetTag", SetTagEvent.class) ;

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

    //============================================================
    /**
     * EventBase class
     */
    abstract static public class EventBase {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 所属するシナリオ。
         */
        public Scenario scenario = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ID。
         * シナリオ内のイベント相互で参照するためのID。
         * null でも良いが、他から参照できない。
         */
        public String id = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * イベント発生時刻文字列
         */
        public String atTimeString = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * イベント発生時刻 (絶対時刻)
         */
        public double atTime = Double.MAX_VALUE ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * コメント
         */
        public String comment = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 過去の（既に完了した）ものか？
         */
        public boolean isCompleted = false ;

        //----------------------------------------
        /**
         * CSV による setup
         */
        public void setupByCsvColumns(Scenario _scenario,
                                      ShiftingStringList columns) {
            scenario = _scenario ;

            if(columns.nth(0).length() > 0 || !columns.nth(0).equals("0")) {
                id = columns.nth(0) ;
            }

            atTimeString = columns.nth(4) ;
            atTime = Scenario.convertToTimeValue(atTimeString) ;
        }

        //----------------------------------------
        /**
         * id を持つかどうか
         * @return 持てば true
         */
        public boolean hasId() {
            return id != null ;
        }

        //----------------------------------------
        /**
         * 相対時刻
         * @return scenario の origin time からの相対時刻
         */
        public double getRelativeTime() {
            return scenario.calcRelativeTime(atTime) ;
        }

        //----------------------------------------
        /**
         * 相対時刻
         * @return scenario の origin time からの相対時刻
         */
        public double getAbsoluteTime() {
            return atTime ;
        }

        //----------------------------------------
        /**
         * 発生チェックと実行(以前の checkIfHappend)
         * @param time : 現在の絶対時刻
         * @return 実施したら true
         */
        public boolean tryOccur(double time, NetworkMapBase map) {
            if(shouldOccurAt(time)) {
                isCompleted = occur(time, map) ;
                return true ;
            } else {
                return false ;
            }
        }

        //----------------------------------------
        /**
         * 発生時刻チェック
         * @param time : 現在の絶対時刻
         * @return 発生時刻なら true
         */
        public boolean shouldOccurAt(double time) {
            return !isCompleted && time >= atTime ;
        }

        //----------------------------------------
        /**
         * イベント発生処理 (以前の setEnabled(true))
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : 完了したら true を返す。false を返すと、延々呼び出される。
         */
        abstract public boolean occur(double time, NetworkMapBase map) ;

        //----------------------------------------
        /**
         * イベント発生逆処理 (以前の setEnabled(false))
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : 完了したら true を返す。
         */
        abstract public boolean unoccur(double time, NetworkMapBase map) ;

        //----------------------------------------
        /**
         * 文字列化
         */
        public String toString() {
            return (getClass().getSimpleName() +
                    "[" + id + ":" +
                    "," + "@" + atTimeString +
                    toStringTail() + "]") ;
        }

        //----------------------------------------
        /**
         * 文字列化 後半
         */
        public String toStringTail() {
            return "" ;
        }

    } // class EventBase

    //============================================================
    /**
     * Initiate Event (START)
     */
    static public class InitiateEvent extends EventBase {
       //----------------------------------------
        /**
         * Start イベント発生処理
         * 実は何もしない
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            return true ;
        }

       //----------------------------------------
        /**
         * Start イベント発生逆処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean unoccur(double time, NetworkMapBase map) {
            return true ;
        }
    } // class InitiateEvent

    //============================================================
    /**
     * Finish Event
     */
    static public class FinishEvent extends EventBase {
       //----------------------------------------
        /**
         * 終了イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            scenario.letFinished() ;
            return true ;
        }

       //----------------------------------------
        /**
         * 終了イベント発生逆処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean unoccur(double time, NetworkMapBase map) {
            scenario.letFinished(false) ;
            return true ;
        }
    } // class FinishEvent

    //============================================================
    /**
     * Placed Event
     * PlaceTag により、発生場所を指定できるイベント
     */
    abstract static public class PlacedEvent extends EventBase {
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

    //============================================================
    /**
     * Alert Event (EVACUATE)
     * 指定した場所のエージェントが避難に入る
     * [2015.01.21 I.Noda] 現状では避難だけだが、
     * いろいろな指示（エージェントの意思変更）・情報提供(エージェントへの条件付与)
     * できるようにしたほうが良い。
     */
    static public class AlertEvent extends PlacedEvent {
       //----------------------------------------
        /**
         * 終了イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            return occur(time, map, false) ;
        }

       //----------------------------------------
        /**
         * 終了イベント発生逆処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean unoccur(double time, NetworkMapBase map) {
            return occur(time, map, true) ;
        } ;

       //----------------------------------------
        /**
         * 終了イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @param inverse : 逆操作かどうか
         * @return : true を返す。
         */
        public boolean occur(double time, NetworkMapBase map, boolean inverse) {
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    link.setEmergency(!inverse) ;
                }
            }
            return true ;
        }
    } // class AlertEvent

    //============================================================
    /**
     * ShutOff Event (STOP)
     * 指定した場所のエージェント生成をやめる。
     * [2015.01.21 I.Noda] 現状では生成停止だけだが、
     * いろいろな機能を実施できるようにしたほうが良いかもしれない。
     * その場合は、ControlEvent などに名前を買えるべきか？
     */
    static public class ShutOffEvent extends PlacedEvent {
       //----------------------------------------
        /**
         * 終了イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            return occur(time, map, false) ;
        }

       //----------------------------------------
        /**
         * 終了イベント発生逆処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean unoccur(double time, NetworkMapBase map) {
            return occur(time, map, true) ;
        }

       //----------------------------------------
        /**
         * 終了イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        public boolean occur(double time, NetworkMapBase map, boolean inverse) {
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    link.setStop(!inverse) ;
                }
            }
            return true ;
        }
    } // class ShutOffEvent

    //============================================================
    /**
     * Gate Event
     */
    abstract static public class GateEvent extends PlacedEvent {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ゲートを示す tag
         * デフォルトでは、placeTag と同じもの。
         */
        public Term gateTag = null ;

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

    //============================================================
    /**
     * OpenGate Event (REMOVE_STOP)
     * ゲートを開く。
     */
    static public class OpenGateEvent extends GateEvent {
       //----------------------------------------
        /**
         * ゲート開放イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            return occur(time, map, false) ;
        }

       //----------------------------------------
        /**
         * ゲート開放イベント発生逆処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean unoccur(double time, NetworkMapBase map) {
            return occur(time, map, true) ;
        }

       //----------------------------------------
        /**
         * ゲート開放イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        public boolean occur(double time, NetworkMapBase map, boolean inverse) {
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    if(!inverse) {
                        link.openGate(gateTag.getString()) ;
                    } else {
                        link.closeGate(gateTag.getString()) ;
                    }
                }
            }
            for(MapNode node : map.getNodes()) {
                if(node.hasTag(placeTag)) {
                    if(!inverse) {
                        node.openGate(gateTag.getString()) ;
                    } else {
                        node.closeGate(gateTag.getString()) ;
                    }
                }
            }
            return true ;
        }
    } // class OpenGateEvent

    //============================================================
    /**
     * CloseGate Event (ADD_STOP)
     * ゲートを開く。
     */
    static public class CloseGateEvent extends GateEvent {
       //----------------------------------------
        /**
         * ゲート開放イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            return occur(time, map, false) ;
        }

       //----------------------------------------
        /**
         * ゲート開放イベント発生逆処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean unoccur(double time, NetworkMapBase map) {
            return occur(time, map, true) ;
        }

       //----------------------------------------
        /**
         * ゲート開放イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        public boolean occur(double time, NetworkMapBase map, boolean inverse) {
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    if(!inverse) {
                        link.closeGate(gateTag.getString()) ;
                    } else {
                        link.openGate(gateTag.getString()) ;
                    }
                }
            }
            for(MapNode node : map.getNodes()) {
                if(node.hasTag(placeTag)) {
                    if(!inverse) {
                        node.closeGate(gateTag.getString()) ;
                    } else {
                        node.openGate(gateTag.getString()) ;
                    }
                }
            }
            return true ;
        }
    } // class CloseGateEvent

    //============================================================
    /**
     * PeriodicGate Event (STOP_TIMES)
     * 間欠的なゲートを開始する。
     * [2015.01.23 I.Noda] これについては、しばらく実装おいておく。
     */
    abstract static public class PeriodicGateEvent extends GateEvent {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ゲートが開いている時間間隔
         */
        public double openInterval = 0.0 ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ゲートが閉じている時間間隔
         */
        public double closeInterval = 0.0 ;
    } // class PeriodicGateEvent

    //============================================================
    /**
     * Set Tag Event (SET/REMOVE)
     * タグをセットする。
     */
    static public class SetTagEvent extends PlacedEvent {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 付加・削除するタグ
         */
        public Term noticeTag = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 付加・削除の識別
         */
        public boolean onoff = true ;

       //----------------------------------------
        /**
         * CSV による setup
         */
        @Override
        public void setupByCsvColumns(Scenario _scenario,
                                     ShiftingStringList columns) {
            super.setupByCsvColumns(_scenario, columns) ;

            String command = columns.nth(3) ;
            String subcoms[] = command.split(":") ;
            if(subcoms.length < 2 || subcoms.length > 2 ||
               subcoms[1].length() == 0) {
                Itk.dbgWrn("Strange commands for SetTag event.") ;
                Itk.dbgMsg("columns", columns) ;
            } else {
                noticeTag = new Term(subcoms[1]) ;
            }
            if(subcoms[0].equals("SET")) {
                onoff = true ;
            } else if(subcoms[0].equals("REMOVE")) {
                onoff = false ;
            } else {
                Itk.dbgWrn("Strange commands for SetTag event.") ;
                Itk.dbgMsg("columns", columns) ;
            }
        }

       //----------------------------------------
        /**
         * タグ操作イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            return occur(time, map, false) ;
        }

       //----------------------------------------
        /**
         * タグ操作イベント発生逆処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean unoccur(double time, NetworkMapBase map) {
            return occur(time, map, true) ;
        }
       //----------------------------------------
        /**
         * タグ操作イベント発生処理(generic)
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @param inverse : 逆操作かどうか
         * @return : true を返す。
         */
        public boolean occur(double time, NetworkMapBase map, boolean inverse) {
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    if(onoff ^ inverse) {
                        link.addTag(noticeTag.getString()) ;
                    } else {
                        link.removeTag(noticeTag.getString()) ;
                    }
                }
            }
            for(MapNode node : map.getNodes()) {
                if(node.hasTag(placeTag)) {
                    if(onoff ^ inverse) {
                        node.addTag(noticeTag.getString()) ;
                    } else {
                        node.removeTag(noticeTag.getString()) ;
                    }
                }
            }
            return true ;
        }

        //----------------------------------------
        /**
         * 文字列化 後半
         */
        public String toStringTail() {
            return (super.toStringTail() +
                    "," + "notice=" + noticeTag +
                    "," + "on=" + onoff);
        }
    } // class SetTagEvent

} // class Scenario

