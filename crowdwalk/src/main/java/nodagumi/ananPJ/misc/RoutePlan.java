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

import java.util.ArrayList;
import java.util.List;

import nodagumi.Itk.Itk;
import nodagumi.Itk.Term;

//======================================================================
/**
 * Route の プランを、先頭から順番に使っていく時のためのクラス。
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
    private List<Term> route = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * route が share されているかどうか？
     */
    private boolean shared = true ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public RoutePlan() {
        route = new ArrayList<Term>() ;
        index = 0 ;
    } ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public RoutePlan(RoutePlan origin) {
        copyFrom(origin);
    } ;

    //------------------------------------------------------------
    /**
     * route を取得
     */
    public List<Term> getRoute() {
        return route ;
    }

    //------------------------------------------------------------
    /**
     * route をセット
     */
    public RoutePlan setRoute(List<Term> _route) {
        return setRoute(_route, false) ;
    }

    //------------------------------------------------------------
    /**
     * route をセット
     */
    public RoutePlan setRoute(List<Term> _route, boolean copy) {
        if(copy) {
            route = new ArrayList<Term>(_route) ;
            shared = false ;
        } else {
            route = _route ;
            shared = true ;
        }
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
     * index をリセッt
     */
    public RoutePlan resetIndex() {
        setIndex(0) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * index を最後に持っていく
     */
    public RoutePlan makeCompleted() {
        setIndex(totalLength()) ;
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
    public Term top() {
        return top(0) ;
    }

    //------------------------------------------------------------
    /**
     * 現在のindexよりn番目を取り出す。
     */
    public Term top(int n) {
        return route.get(index + n) ;
    }

    //------------------------------------------------------------
    /**
     * 現在の先頭を見る。（取り除く）
     */
    public Term get() {
        Term string = top() ;
        shift() ;
        return string ;
    }

    //------------------------------------------------------------
    /**
     * プランの追加(末尾)
     */
    public RoutePlan add(Term tag) {
        route.add(tag) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * プランの追加(末尾)
     */
    public RoutePlan addSafely(Term tag) {
        if(shared) {
            route = new ArrayList<Term>(route) ;
            shared = false ;
        }
        add(tag) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * プランの追加
     */
    public RoutePlan insertSafely(Term tag) {
        if(shared) {
            route = new ArrayList<Term>(route) ;
            shared = false ;
        }
        insert(tag) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * プランの追加
     */
    public RoutePlan insert(Term tag) {
        route.add(null) ;
        for(int i = route.size() -1 ; i > index ; i--) {
            route.set(i, route.get(i-1)) ;
        }
        route.set(index, tag) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * プランの追加
     */
    public RoutePlan duplicate() {
        return new RoutePlan(this) ;
    }

    //------------------------------------------------------------
    /**
     * プランの追加
     */
    public RoutePlan copyFrom(RoutePlan origin) {
        setRoute(origin.getRoute()) ;
        setIndex(origin.getIndex()) ;
        shared = origin.shared ;
        return this ;
    }

} // class RoutePlan.java


