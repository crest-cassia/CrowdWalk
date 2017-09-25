// -*- mode: java; indent-tabs-mode: nil -*-
/** IndividualConfigList
 * @author:: Itsuki Noda
 * @version:: 0.0 2017/09/25 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2017/09/25]: separate this class from AgentFactory. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents.Factory;

import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.*;


//======================================================================
/**
 * エージェント個別設定情報格納用クラス
 */
public class IndividualConfigList {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 個別パラメータ
     */
    public Term list = null ;
        
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 個別パラメータ用 index
     */
    public int index = 0 ;

    //------------------------------
    /**
     * constructor
     */
    public IndividualConfigList() {
	setList(null) ;
    }
        
    //------------------------------
    /**
     * constructor
     */
    public IndividualConfigList(Term _list) {
	setList(_list) ;
    }
        
    //------------------------------
    /**
     * list の中身をチェックしてセット
     */
    public Term setList(Term _list) {
	if(_list == null || _list.isArray()) {
	    list = _list ;
	} else {
	    Itk.logError("Illegal IndividualConfig data.",
			 "should be ArrayTerm:", 
			 _list) ;
	    Itk.quitByError() ;
	}
	index = 0 ;
	return list ;
    }
        
    //------------------------------
    /**
     * 利用可能かチェック。
     */
    public boolean isAvailable() {
	return list != null ;
    }

    //------------------------------
    /**
     * すでにオーバーしているかチェック。
     */
    public boolean isOver() {
	return index >= size() ;
    }

    //------------------------------
    /**
     * config の数。
     */
    public int size() {
	if(isAvailable()) {
	    return list.getArraySize() ;
	} else {
	    return 0 ;
	}
    }
        
    //------------------------------
    /**
     * 残っているconfig の数。
     */
    public int remainSize() {
	return size() - index ;
    }

    //------------------------------
    /**
     * startTime で整列。
     */
    public void sortByStartTime() {
	if(size() > 0) {
	    try {
		list.getArray().sort(new Comparator<Object>(){
			@Override
			public int compare(Object xObj, Object yObj) {
			    Term x = (Term)xObj ;
			    Term y = (Term)yObj ;
			    String xTime = x.getArgString("startTime") ;
			    String yTime = y.getArgString("startTime") ;
			    return xTime.compareTo(yTime) ;
			}
		    }) ;
	    } catch(Exception ex) {
		ex.printStackTrace() ;
		Itk.logError("something wrong in sorting IndividualConfigList.") ;
		Itk.logError_("list", list) ;
		Itk.quitByError() ;
	    }
	}
    }

    //------------------------------
    /**
     * N 番目を取ってくる。
     * size() の剰余系なので、ループする。
     */
    public Term getNth(int nth) {
	return list.getNthTerm(nth % size()) ;
    }
            
    //------------------------------
    /**
     * 次のデータを見てみる。
     * index は上げない。
     */
    public Term peekNext() {
	return getNth(index) ;
    }
        
    //------------------------------
    /**
     * 次のデータを見て、index をカウントアップ。
     */
    public Term getNext() {
	Term config = peekNext() ;
	index += 1 ;
	return config ;
    }
        
    //------------------------------
    /**
     * 次のデータを見てみる。
     * index は上げない。
     */
    public Term peekFirst() {
	return getNth(0) ;
    }
        
    //------------------------------
    /**
     * 次のデータを見てみる。
     * index は上げない。
     */
    public Term peekLast() {
	return getNth(size()-1) ;
    }
        
    //------------------------------
    /**
     * 指定時刻まで残っているconfig の数。
     */
    public int remainSizeBefore(SimTime currentTime) {
	int indexBackup = index ;
	int count = 0 ;
	while(!isOver()) {
	    Term config = getNext() ;
	    SimTime startTime =
		new SimTime(config.getArgString("startTime")) ;
	    if(startTime.isAfter(currentTime)) {
		break ;
	    }
	    count += 1;
	}
	index = indexBackup ;
	return count ;
    }

    //------------------------------
    /**
     * JSONへの変換用
     */
    public Term toTerm() {
	return list ;
    }
} // class IndividualConfigList 
    
