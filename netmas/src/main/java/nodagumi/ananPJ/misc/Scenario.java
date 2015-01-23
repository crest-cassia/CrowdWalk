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

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;

import nodagumi.Itk.* ;

//======================================================================
/**
 * Scenario Class
 */
public class Scenario {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * イベント型 enum
     */
    static public enum EventType {
        Initiate, Finish,
        SetTag, AddTag, RemoveTag,
        OpenGate, CloseGate,
        ShutOff,
        Alert,
        None
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Lexicon for EventType
     */
    static public Lexicon eventLexicon = new Lexicon() ;
    static {
        eventLexicon.registerEnum(EventType.class) ;
        eventLexicon.register("START", EventType.Initiate) ;
        eventLexicon.register("RESPONSE", EventType.Finish) ;
        eventLexicon.register("SET", EventType.AddTag) ;
        eventLexicon.register("REMOVE", EventType.RemoveTag) ;
        eventLexicon.register("ADD_STOP", EventType.CloseGate) ;
        eventLexicon.register("REMOVE_STOP", EventType.OpenGate) ;
        eventLexicon.register("STOP", EventType.ShutOff) ;
        eventLexicon.register("EVACUATE", EventType.Alert) ;
    }

    //============================================================
    /**
     * EventBase class
     */
    abstract static public class EventBase {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ID。
         * シナリオ内のイベント相互で参照するためのID。
         * null でも良いが、他から参照できない。
         */
        public String id = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * イベントの種類を示す
         */
        public EventType type = null ;

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
         * イベント発生処理 (以前の setEnabled)
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : 完了したら true を返す。false を返すと、延々呼び出される。
         */
        abstract public boolean occur(double time, NetworkMapBase map) ;

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
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    link.setEmergency(true) ;
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
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    link.setStop(true) ;
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
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    link.openGate(gateTag.getString()) ;
                }
            }
            for(MapNode node : map.getNodes()) {
                if(node.hasTag(placeTag)) {
                    node.openGate(gateTag.getString()) ;
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
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    link.closeGate(gateTag.getString()) ;
                }
            }
            for(MapNode node : map.getNodes()) {
                if(node.hasTag(placeTag)) {
                    node.closeGate(gateTag.getString()) ;
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
         * ゲート開放イベント発生処理
         * @param time : 現在の絶対時刻
         * @param map : 地図データ
         * @return : true を返す。
         */
        @Override
        public boolean occur(double time, NetworkMapBase map) {
            for(MapLink link : map.getLinks()) {
                if(link.hasTag(placeTag)) {
                    if(onoff) {
                        link.addTag(noticeTag.getString()) ;
                    } else {
                        link.removeTag(noticeTag.getString()) ;
                    }
                }
            }
            for(MapNode node : map.getNodes()) {
                if(node.hasTag(placeTag)) {
                    if(onoff) {
                        node.addTag(noticeTag.getString()) ;
                    } else {
                        node.removeTag(noticeTag.getString()) ;
                    }
                }
            }
            return true ;
        }
    } // class SetTagEvent

} // class Scenario

