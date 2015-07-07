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

import nodagumi.Itk.Itk;

//======================================================================
/**
 * シミュレーション内での時刻を様々な形で表す。
 */
public class SimClock {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 基準となる絶対時刻の文字列標記。
     * "HH:MM:SS" もしくは、"HH:MM" でなければならない。
     */
    private String originTimeString = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 基準となる絶対時刻の整数値。
     * 未定値は -1。
     */
    private int originTimeInt = -1 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 刻み回数。
     */
    private int tickCount = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 時刻の刻み幅。
     * advance() で進む幅。
     */
    private double tickUnit = 1.0 ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public SimClock() {
	this(null) ;
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

    //------------------------------------------------------------
    // 初期化関連。
    //------------------------------------------------------------
    /**
     * 初期化。
     */
    public void init(String originTimeString, double tickUnit) {
	if(originTimeString != null) {
	    setupOriginByString(originTimeString) ;
	}
	this.tickUnit = tickUnit ;
	reset() ;
    }

    //------------------------------------------------------------
    /**
     * 文字列による基準時刻設定。
     */
    public int setupOriginByString(String originTimeString) {
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
	    this.originTimeInt = -1 ;
	}
	return this.originTimeInt ;
    }

    //------------------------------------------------------------
    /**
     * 整数値による基準時刻設定。
     */
    public int setupOriginByInt(int originTimeInt) {
	this.originTimeInt = originTimeInt ;
	this.originTimeString = null ;
	return this.originTimeInt ;
    }

    //------------------------------------------------------------
    /**
     * 相対時刻のリセット
     */
    public void reset() {
	this.tickCount = 0 ;
    }

    //------------------------------------------------------------
    /**
     * 時刻のコピー。
     */
    public SimClock duplicate() {
	SimClock newClock = new SimClock() ;
	newClock.originTimeString = this.originTimeString ;
	newClock.originTimeInt = this.originTimeInt ;
	newClock.tickCount = this.tickCount ;
	newClock.tickUnit = this.tickUnit ;

	return newClock ;
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
	    return "(not_a_time)" ;
	}
    }

    /**
     * 基準時刻(文字列)。
     */
    public void setOriginTimeString(String timeString) {
	setupOriginByString(timeString) ;
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
    public void setOriginTimeInt(int timeInSec) {
	setupOriginByInt(timeInSec) ;
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
    public void getTickCount(int count) {
	tickCount = count ;
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
    public void setTickUnit(double tick) {
	tickUnit = tick ;
    }

    //------------------------------------------------------------
    /**
     * 相対時刻(実数)。
     */
    public double getRelativeTime() {
	return (double)tickCount * tickUnit ;
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
     * 時刻前進。
     */
    public int advance() {
	tickCount += 1 ;
	return tickCount ;
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

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
} // class SimClock
