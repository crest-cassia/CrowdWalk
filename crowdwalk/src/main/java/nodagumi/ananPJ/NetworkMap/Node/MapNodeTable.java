// -*- mode: java; indent-tabs-mode: nil -*-
/** MapNode のテーブル
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/18 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/18]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.NetworkMap.Node;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkMap.OBNodeTable ;
import nodagumi.ananPJ.NetworkMap.Node.MapNode ;

import nodagumi.Itk.*;

//======================================================================
/**
 * MapNode のテーブル。 
 * {@code ArrayList<MapNode>} を置き換え、各種機能を提供する。
 */
public class MapNodeTable extends OBNodeTable<MapNode> {
    //------------------------------------------------------------
    /**
     * Constructor with no args
     */
    public MapNodeTable() { 
        super() ; 
    }

    //------------------------------------------------------------
    /**
     * Constructor from Collection
     * これがなぜ必要なのかわからない。
     * Generic 型ではコンストラクタは継承されない？
     */
    public MapNodeTable(Collection<MapNode> origin) {
        super(origin) ;
    }

    //------------------------------------------------------------
    /**
     * 指定された tag を持つノードを取り出す。
     * @param tag tag名
     * @return tag 名を持つノードを集めた MapNodeTable
     */
    public MapNodeTable findTaggedNodes(String tag){
        return findTaggedNodes(tag, new MapNodeTable()) ;
    }

    //------------------------------------------------------------
    /**
     * 指定された tag を持つノードを取り出す。
     * @param tag tag名
     * @param table 格納用テーブル
     * @return tag 名を持つノードを集めた MapNodeTable。table がそのまま返る。
     */
    public MapNodeTable findTaggedNodes(String tag, MapNodeTable table){
        return (MapNodeTable)super.findTaggedOBNodes(tag, table) ;
    }

    //------------------------------------------------------------
    /**
     * 指定された tag をもつノードの、指定したprefixを持つタグを集める。
     * @param tag tag名
     * @param tag prefix
     * @return tag 名を持つノードのタグを集めた MapNodeTable。
     */
    public ArrayList<String> findPrefixedTagsOfTaggedNodes(String tag,
                                                           String prefix) {
        ArrayList<String> table = new ArrayList<String>() ;

        for(MapNode node : this) {
            if(node.hasTag(tag)) {
                for(String nodeTag : node.getTags()) {
                    if(nodeTag.contains(prefix)) {
                        table.add(nodeTag) ;
                    }
                }
            }
        }

        return table ;
    }

} // class MapNodeTable

