// -*- mode: java; indent-tabs-mode: nil -*-
/** 
 * Reason Tray: 理由や筋道を貯めておくためのもの。
 * あまり無駄なメモリを使わないためのもの
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/15]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.util.ArrayList ;
import java.util.List ;
import java.lang.StringBuilder ;
import java.lang.String ;

import nodagumi.Itk.* ;

//======================================================================
/**
 * 理由や思考の筋道を貯めておくためのクラス。
 */
public class ReasonTray {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 接合する際の文字列
     */
    final public String DefaultJoinSeparator = " " ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 実験用。文字列の場合の効率を探る。
     */
    private List<Object> reasonList ;

    //------------------------------------------------------------
    /**
     * constructor
     */
    public ReasonTray() {
	reasonList = new ArrayList<Object>() ;
    }

    //------------------------------------------------------------
    /**
     * トレーをクリア
     */
    public ReasonTray clear() {
	reasonList.clear() ;
	return this ;
    }

    //------------------------------------------------------------
    /**
     * 理由を追加
     */
    public ReasonTray add(Object reason) {
	reasonList.add(reason) ;
	return this ;
    }

    //------------------------------------------------------------
    /**
     * 文字列化
     */
    public String toString() {
	StringBuilder reasonBuffer = null ;
	for(Object reason : reasonList) {
	    if(reasonBuffer == null) {
		reasonBuffer = new StringBuilder() ;
	    } else {
		reasonBuffer.append(DefaultJoinSeparator) ;
	    }
	    reasonBuffer.append(reason) ;
	}
	return reasonBuffer.toString() ;
    }

} // class ReasonTray

