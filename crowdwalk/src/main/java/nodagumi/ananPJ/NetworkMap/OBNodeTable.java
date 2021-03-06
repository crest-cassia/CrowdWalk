// -*- mode: java; indent-tabs-mode: nil -*-
/** OBNode のテーブル
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/18 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/18]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.NetworkMap;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import nodagumi.ananPJ.NetworkMap.OBNode;

import nodagumi.Itk.*;

//======================================================================
/**
 * OBNode のテーブル。 
 * {@literal ArrayList<OBNode>} を置き換え、各種機能を提供する。
 */
public class OBNodeTable<T extends OBNode> extends ArrayList<T> {
    //------------------------------------------------------------
    /**
     * Constructor with no args
     */
    public OBNodeTable() { 
        super() ; 
    }

    //------------------------------------------------------------
    /**
     * Constructor from Collection
     */
    public OBNodeTable(Collection<T> origin) {
        this() ;
        for(T obNode  : origin) {
            add(obNode) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 指定された tag を持つリンクの存在チェック
     * @param tag tag名
     * @return 存在したら true。
     */
    public boolean tagExistP(String tag) {
        for(OBNode obNode : this) {
            if(obNode.hasTag(tag)) {
                return true ;
            }
        }
        return false ;
    }

    //------------------------------------------------------------
    /**
     * 指定された id を持つOBNodeの取得
     * @param id tag名
     * @return 存在したらそのOBNode。
     */
    public T findById(String id) {
        for(T obNode : this) {
            if(obNode.getID().equals(id)) {
                return obNode ;
            }
        }
        return null ;
    }

    //------------------------------------------------------------
    /**
     * 指定された tag を持つOBNodeを取り出す。
     * @param tag tag名
     * @return tag 名を持つOBNodeを集めた OBNodeTable
     */
    public OBNodeTable<T> findTaggedOBNodes(String tag){
        return findTaggedOBNodes(tag, new OBNodeTable<T>()) ;
    }

    //------------------------------------------------------------
    /**
     * 指定された tag を持つリンクを取り出す。
     * @param tag tag名
     * @param table 格納用テーブル
     * @return tag 名を持つOBNodeを集めた OBNodeTable。table がそのまま返る。
     */
    public OBNodeTable findTaggedOBNodes(String tag, OBNodeTable table){
        for(OBNode obNode : this) {
            if(obNode.hasTag(tag)) {
                table.add(obNode) ;
            }
        }
        return table ;
    }

    //------------------------------------------------------------
    /**
     * テーブル内の要素をランダムに選択
     * @param random 乱数生成器
     * @return 選択された要素
     */
    public T chooseRandom(Random random) {
        return get(random.nextInt(size())) ;
    }

} // class OBNodeTable<T>

