// -*- mode: java; indent-tabs-mode: nil -*-
/** Route Plan class
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/28 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/28]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.misc ;

import nodagumi.Itk.Itk;
import java.util.ArrayList;

//======================================================================
/**
 * String 配列を、先頭から順番に使っていく時のためのクラス。
 */
public class RoutePlan {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 現在位置
     */
    private int index = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * タグの列
     */
    private ArrayList<String> route = null ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public RoutePlan() {
	route = new ArrayList<String>() ;
	index = 0 ;
    } ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public RoutePlan(RoutePlan origin) {
	route = origin.getRoute() ;
	index = origin.getIndex() ;
    } ;

    //------------------------------------------------------------
    /**
     * route を取得
     */
    public ArrayList<String> getRoute() {
	return route ;
    }

    //------------------------------------------------------------
    /**
     * route をセット
     */
    public RoutePlan setRoute(ArrayList<String> _route) {
	route = _route ;
	return this ;
    }

    //------------------------------------------------------------
    /**
     * index を取得
     */
    public int getIndex() {
	return index ;
    }

    //------------------------------------------------------------
    /**
     * index をセット
     */
    public RoutePlan setIndex(int _index) {
	index = _index ;
	return this ;
    }

    //------------------------------------------------------------
    /**
     * shift: index をひとつずらす
     */
    public int shift() {
        return shift(1) ;
    }

    //------------------------------------------------------------
    /**
     * shift: index を n ずらす
     */
    public int shift(int n) {
        index += n ;
        return index ;
    }

    //------------------------------------------------------------
    /**
     * 残りの長さ
     */
    public int length() {
        return totalLength() - index ;
    }

    //------------------------------------------------------------
    /**
     * もとの長さ
     */
    public int totalLength() {
	return route.size() ;
    }

    //------------------------------------------------------------
    /**
     * 空かどうかのチェック
     */
    public boolean isEmpty() {
        return length() <= 0 ;
    }

    //------------------------------------------------------------
    /**
     * 現在の先頭を見る。（取り除かない）
     */
    public String top() {
        return top(0) ;
    }

    //------------------------------------------------------------
    /**
     * 現在のindexよりn番目を取り出す。
     */
    public String top(int n) {
	return route.get(index + n) ;
    }

    //------------------------------------------------------------
    /**
     * 現在の先頭を見る。（取り除く）
     */
    public String get() {
        String string = top() ;
        shift() ;
        return string ;
    }

    //------------------------------------------------------------
    /**
     * 現在の先頭を見る。（取り除く）
     */
    public RoutePlan add(String tag) {
	route.add(tag) ;
	return this ;
    }

} // class RoutePlan.java


