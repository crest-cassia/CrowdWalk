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

import nodagumi.Itk.Itk;

//======================================================================
/**
 * シミュレーション内での時刻を様々な形で表す。
 */
public class SimClock {
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * ゼロ時刻文字列
     */
    static public String TimeZeroString = "00:00:00" ;

    /**
     * 無指定時刻文字列
     */
    final static public String TimeNoneString = "__:__:__" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 基準となる絶対時刻の文字列標記。
     * "HH:MM:SS" もしくは、"HH:MM" でなければならない。
     */
    protected String originTimeString = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 基準となる絶対時刻の整数値。
     * 未定値は -1。
     */
    protected int originTimeInt = -1 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 刻み回数。
     */
    protected int tickCount = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 時刻の刻み幅。
     * advance() で進む幅。
     */
    protected double tickUnit = 1.0 ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public SimClock() {
        this((String)null) ;
    }

    /**
     * コンストラクタ
     */
    public SimClock(String originTimeString) {
        this(originTimeString, 1.0) ;
    }

    /**
     * コンストラクタ
     */
    public SimClock(String originTimeString, double tickUnit) {
        init(originTimeString, tickUnit) ;
    }

    /**
     * コンストラクタ
     */
    public SimClock(SimClock origin) {
        copyFrom(origin) ;
    }

    //------------------------------------------------------------
    // 初期化関連。
    //------------------------------------------------------------
    /**
     * 初期化。
     */
    public SimClock init(String originTimeString, double tickUnit) {
        if(originTimeString != null) {
            setupOriginByString(originTimeString, false) ;
        }
        this.tickUnit = tickUnit ;
        reset() ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 他の SimClock からのコピー。
     */
    public SimClock copyFrom(SimClock origin) {
        this.originTimeString = origin.originTimeString ;
        this.originTimeInt = origin.originTimeInt ;
        this.tickCount = origin.tickCount ;
        this.tickUnit = origin.tickUnit ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 文字列による基準時刻設定。
     * @param originTimeString : "HH:MM:SS" の形式。
     * @param setZeroIfNull : true であれば、originTimeString が null の時、
     *                        int を 0 とする。
     *                        false の時は、-1。
     */
    public SimClock setupOriginByString(String originTimeString,
                                        boolean setZeroIfNull) {
        if(originTimeString != null) {
            this.originTimeString = originTimeString ;
            try {
                this.originTimeInt
                    = Itk.scanTimeStringToInt(this.originTimeString) ;
            } catch(Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("Wrong Time Format", originTimeString) ;
                this.originTimeInt = -1 ;
            }
        } else {
            this.originTimeString = null ;
            this.originTimeInt = (setZeroIfNull ? 0 : -1) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 整数値による基準時刻設定。
     */
    public SimClock setupOriginByInt(int originTimeInt) {
        this.originTimeInt = originTimeInt ;
        this.originTimeString = null ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 相対時刻のリセット
     */
    public SimClock reset() {
        this.tickCount = 0 ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 時刻のコピー。
     */
    public SimTime newSimTime() {
        //Itk.dbgMsg("SimClock.newSimTime", this) ;
        //Itk.dumpStackTraceN(2,1) ;
        SimTime simTime = new SimTime() ;
        simTime.copyFrom(this);
        return simTime ;
    }

    //------------------------------------------------------------
    /**
     * 指定時間進んだ時刻。
     */
    public SimTime newSimTimeWithAdvance(double advanceTime) {
        return (SimTime)newSimTime().advanceSec(advanceTime) ;
    }

    //------------------------------------------------------------
    /**
     * 指定時間進んだ時刻。
     */
    public SimTime newSimTimeWithAdvanceTick(int advanceTick){
        return (SimTime)newSimTime().advanceTick(advanceTick) ;
    }

    //------------------------------------------------------------
    /**
     * 同じ基準時刻・tickUnitを使って、指定の時刻を作成。
     */
    public SimTime newSimTimeByString(String timeString) {
        SimTime simTime = newSimTime() ;
        int absTime = 0 ;
        if(timeString != null) {
            try {
                absTime = Itk.scanTimeStringToInt(timeString) ;
            } catch(Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("Wrong Time Format", timeString) ;
                this.originTimeInt = -1 ;
            }
        }
        simTime.tickCount =
            (int)Math.round(((double)(absTime - originTimeInt)) / tickUnit) ;
        return simTime ;
    }

    //------------------------------------------------------------
    /**
     * 同じ基準時刻・tickUnitを使って、指定の時刻を作成。
     */
    public SimTime newSimTimeByRelativeTime(double relativeTime) {
        return (SimTime)newSimTime().setRelativeTime(relativeTime) ;
    }

    //------------------------------------------------------------
    // アクセス。
    //------------------------------------------------------------
    /**
     * 基準時刻(文字列)。
     */
    public String getOriginTimeString() {
        if(originTimeString != null) {
            return originTimeString ;
        } else if(originTimeInt >= 0) {
            return Itk.formatSecTime(originTimeInt) ;
        } else {
            return TimeNoneString ;
        }
    }

    /**
     * 基準時刻(文字列)。
     * @param timeString : "HH:MM:SS" の形式。
     * @param setZeroIfNull : もし timeString が null の場合、0 と扱う。
     */
    public SimClock setOriginTimeString(String timeString,
                                    boolean setZeroIfNull) {
        setupOriginByString(timeString, setZeroIfNull) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 基準時刻(整数値)。
     */
    public int getOriginTimeInt() {
        return originTimeInt ;
    }

    /**
     * 基準時刻(整数値)。
     */
    public SimClock setOriginTimeInt(int timeInSec) {
        setupOriginByInt(timeInSec) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 刻み回数。
     */
    public int getTickCount() {
        return tickCount ;
    }

    /**
     * 刻み回数。
     */
    public SimClock setTickCount(int count) {
        tickCount = count ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 時刻刻み幅。
     */
    public double getTickUnit() {
        return tickUnit ;
    }

    /**
     * 時刻刻み幅。
     */
    public SimClock setTickUnit(double tick) {
        tickUnit = tick ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 相対時刻(実数)。
     */
    public double getRelativeTime() {
        return (double)tickCount * tickUnit ;
    }

    /**
     * 相対時刻(実数)。
     */
    public SimClock setRelativeTime(double relativeTime) {
        tickCount = (int)Math.round(relativeTime / tickUnit) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * 絶対時刻(実数)。
     */
    public double getAbsoluteTime() {
        return (double)originTimeInt + getRelativeTime() ;
    }

    //------------------------------------------------------------
    /**
     * 絶対時刻(文字列)。
     * 1秒以下は切り捨て。
     */
    public String getAbsoluteTimeString() {
        return Itk.formatSecTime(originTimeInt + (int)getRelativeTime()) ;
    }

    //------------------------------------------------------------
    /**
     * 文字列化
     */
    public String toString() {
        return ("#SimClock[" + getOriginTimeString() + "+"
                + getRelativeTime() + "s]") ;
    }

    //------------------------------------------------------------
    // 操作
    //------------------------------------------------------------
    /**
     * 時刻前進。１tick。
     */
    public SimClock advance() {
        return advanceTick(1) ;
    }

    /**
     * 時刻前進。N tick。
     */
    public SimClock advanceTick(int delta) {
        tickCount += delta ;
        return this ;
    }

    /**
     * 時刻前進。N seconds。
     */
    public SimClock advanceSec(double sec) {
        return advanceTick((int)Math.round(sec / tickUnit)) ;
    }

    //------------------------------------------------------------
    /**
     * 時間差。
     * @param time : 基準になる時刻。
     * @return 基準からの経過時間。time より後であれば正。
     */
    public double calcDifferenceFrom(SimClock time) {
        if(this.originTimeInt == time.originTimeInt) {
            return this.getRelativeTime() - time.getRelativeTime() ;
        } else {
            return this.getAbsoluteTime() - time.getAbsoluteTime() ;
        }
    }

    /**
     * 時間差。
     * @param time : 基準になる時刻。
     * @return 基準までの経過時間。time より前であれば正。
     */
    public double calcDifferenceTo(SimClock time) {
        return -calcDifferenceFrom(time) ;
    }

    //------------------------------------------------------------
    // 判定
    //------------------------------------------------------------
    /**
     * 基準と単位が同じかどうか。
     */
    public boolean isSameBaseWith(SimClock time) {
        return (this.originTimeInt == time.originTimeInt &&
                this.tickUnit == time.tickUnit) ;
    }

    //------------------------------------------------------------
    /**
     * 時刻の前後判定。
     * @param time : 基準になる時刻。
     * @return 丁度であればtrue。
     */
    public boolean isAt(SimClock time) {
        if(isSameBaseWith(time)) {
            return this.tickCount == time.tickCount ;
        } else {
            return calcDifferenceFrom(time) == 0.0 ;
        }
    }

    /**
     * 時刻の前後判定。
     * @param time : 基準になる時刻。
     * @return time より前であれば true。
     */
    public boolean isBefore(SimClock time) {
        if(isSameBaseWith(time)) {
            return this.tickCount < time.tickCount ;
        } else {
            return calcDifferenceFrom(time) < 0.0 ;
        }
    }

    /**
     * 時刻の前後判定。
     * @param time : 基準になる時刻。
     * @return time より前かちょうどであれば true。
     */
    public boolean isBeforeOrAt(SimClock time) {
        if(isSameBaseWith(time)) {
            return this.tickCount <= time.tickCount ;
        } else {
            return calcDifferenceFrom(time) <= 0.0 ;
        }
    }

    /**
     * 時刻の前後判定。
     * @param time : 基準になる時刻。
     * @return time より後であれば true。
     */
    public boolean isAfter(SimClock time) {
        if(isSameBaseWith(time)) {
            return this.tickCount > time.tickCount ;
        } else {
            return calcDifferenceFrom(time) > 0.0 ;
        }
    }

    /**
     * 時刻の前後判定。
     * @param time : 基準になる時刻。
     * @return time より後かちょうどであれば true。
     */
    public boolean isAfterOrAt(SimClock time) {
        if(isSameBaseWith(time)) {
            return this.tickCount >= time.tickCount ;
        } else {
            return calcDifferenceFrom(time) >= 0.0 ;
        }
    }

    /**
     * Sort 用比較。
     * @param time : 基準になる時刻。
     * @return time より後なら 1, 前なら -1, 同じなら 0
     */
    public int compareTo(SimClock time) {
        if(isAt(time)) return 0 ;
        else if(isAfter(time)) return 1 ;
        else return -1 ;
    }

    //============================================================
    //============================================================
    /**
     * 時間を停められた SimClock を表す。
     */
    static public class SimTime extends SimClock {
        //========================================
        //::::::::::::::::::::::::::::::::::::::::
        /**
         * ゼロ時刻インスタンス。
         */
        static public SimTime Zero =
            new SimTime(TimeZeroString) ;

        /**
         * 無限大未来時刻インスタンス。
         */
        static public SimTime Ending =
            (SimTime)(new SimTime(TimeZeroString).setTickCount(Integer.MAX_VALUE));

        /**
         * 無限大過去時刻インスタンス。
         */
        static public SimTime Beginning =
            (SimTime)(new SimTime(TimeZeroString).setTickCount(Integer.MIN_VALUE));

        //----------------------------------------
        /**
         * コンストラクタ
         */
        public SimTime() { super() ; }
        /** */
        public SimTime(String originTimeString) { super(originTimeString) ; }
        /** */
        public SimTime(String originTimeString, double tickUnit) {
            super(originTimeString, tickUnit) ;
        }
        /** */
        public SimTime(SimClock origin) { super(origin) ; }

        //----------------------------------------
        /**
         * advance() は使えない。
         */
        @Override
        public SimTime advance() {
            Itk.logError("can not call SimTime#advance()", this) ;
            System.exit(1) ;
            return this ; // never reach here
        }

    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class SimClock
