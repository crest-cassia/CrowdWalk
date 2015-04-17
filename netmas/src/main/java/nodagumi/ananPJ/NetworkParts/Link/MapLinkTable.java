// -*- mode: java; indent-tabs-mode: nil -*-
/** MapLink のテーブル
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/18 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/18]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.NetworkParts.Link;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkParts.OBNodeTable ;
import nodagumi.ananPJ.NetworkParts.Link.MapLink ;

import nodagumi.Itk.*;

//======================================================================
/**
 * MapLink のテーブル。 
 * {@literal ArrayList<MapLink>} を置き換え、各種機能を提供する。
 */
public class MapLinkTable extends OBNodeTable<MapLink> {
    //------------------------------------------------------------
    /**
     * 指定された tag を持つリンクを取り出す。
     * @param tag tag名
     * @return tag 名を持つリンクを集めた MapLinkTable
     */
    public MapLinkTable findTaggedLinks(String tag){
        return findTaggedLinks(tag, new MapLinkTable()) ;
    }

    //------------------------------------------------------------
    /**
     * 指定された tag を持つリンクを取り出す。
     * @param tag tag名
     * @param table 格納用テーブル
     * @return tag 名を持つリンクを集めた MapLinkTable。table がそのまま返る。
     */
    public MapLinkTable findTaggedLinks(String tag, MapLinkTable table){
        return (MapLinkTable)super.findTaggedOBNodes(tag, table) ;
    }

} // class MapLinkTable

