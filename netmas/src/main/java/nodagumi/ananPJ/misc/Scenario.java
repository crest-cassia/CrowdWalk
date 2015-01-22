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
        Initiate, Outbreak, Finish,
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
        eventLexicon.register("OUTBREAK", EventType.Outbreak) ;
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
    static public class EventBase {
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
         * イベント発生時刻
         */
        public double atTime = Double.MAX_VALUE ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * コメント
         */
        public String comment = null ;
    }

    //============================================================
    /**
     * Initiate Event (START)
     */
    static public class InitiateEvent extends EventBase {
    }

    //============================================================
    /**
     * Finish Event
     */
    static public class FinishEvent extends EventBase {
    }

    //============================================================
    /**
     * Outbreak Event
     */
    static public class OutbreakEvent extends EventBase {
    }

    //============================================================
    /**
     * Placed Event
     * PlaceTag により、発生場所を指定できるイベント
     */
    static public class PlacedEvent extends EventBase {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 場所を指定するタグ
         */
        public Term placeTag = null ;
    }

    //============================================================
    /**
     * Alert Event (EVACUATE)
     * 指定した場所のエージェントが避難に入る
     * [2015.01.21 I.Noda] 現状では避難だけだが、
     * いろいろな指示（エージェントの意思変更）・情報提供(エージェントへの条件付与)
     * できるようにしたほうが良い。
     */
    static public class AlertEvent extends PlacedEvent {
    }

    //============================================================
    /**
     * ShutOff Event (STOP)
     * 指定した場所のエージェント生成をやめる。
     * [2015.01.21 I.Noda] 現状では生成停止だけだが、
     * いろいろな機能を実施できるようにしたほうが良いかもしれない。
     * その場合は、ControlEvent などに名前を買えるべきか？
     */
    static public class ShutOffEvent extends PlacedEvent {
    }

    //============================================================
    /**
     * PeriodicGate Event (STOP_TIMES)
     * 間欠的なゲートを開始する。
     */
    static public class PeriodicGateEvent extends PlacedEvent {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ゲートを示す tag
         * デフォルトでは、placeTag と同じもの。
         */
        public Term gateTag = null ;

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
    }

    //============================================================
    /**
     * OpenGate Event (REMOVE_STOP)
     * ゲートを開く。
     */
    static public class OpenGateEvent extends PeriodicGateEvent {
    }

    //============================================================
    /**
     * CloseGate Event (ADD_STOP)
     * ゲートを開く。
     */
    static public class CloseGateEvent extends PeriodicGateEvent {
    }

    //============================================================
    /**
     * Set Tag Event 
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
    }

} // class Foo

