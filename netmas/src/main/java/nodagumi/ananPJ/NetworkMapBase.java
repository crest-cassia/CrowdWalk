// -*- mode: java; indent-tabs-mode: nil -*-
/** Bare Network Map (only include tables of links and nodes)
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/26 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/26]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ;

import java.io.Serializable;

import javax.swing.tree.DefaultTreeModel;

import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;

//======================================================================
/**
 * link と node のテーブルのみを持つクラス。
 * links と nodes をセットで受け渡すメソッドが非常に多い。
 * なので、まとめておくものを作っておく。
 */
public class NetworkMapBase extends DefaultTreeModel {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ノードテーブル
     */
    protected MapNodeTable nodesCache = new MapNodeTable();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * リンクテーブル
     */
    protected MapLinkTable linksCache = new MapLinkTable();

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public NetworkMapBase() {
        super(null, true);
    }

    //------------------------------------------------------------
    /**
     * ノードテーブル取得
     * @return node table
     */
    public MapNodeTable getNodes() {
        return nodesCache;
    }

    //------------------------------------------------------------
    /**
     * リンクテーブル取得
     * @return link table
     */
    public MapLinkTable getLinks() {
        return linksCache;
    }

} // class Foo

