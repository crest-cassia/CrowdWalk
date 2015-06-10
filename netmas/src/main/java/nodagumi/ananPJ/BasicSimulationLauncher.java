// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import nodagumi.ananPJ.misc.NetmasPropertiesHandler;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;

import nodagumi.Itk.*;

//======================================================================
/**
 * GUI/CUI 共通の部分を記述する。
 */
public abstract class BasicSimulationLauncher {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 属性を扱うハンドラ
     */
    protected NetmasPropertiesHandler properties = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * simulator の実体
     */
    protected EvacuationSimulator simulator = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器。
     */
    protected Random random = null;

    //------------------------------------------------------------
    /**
     * constructor
     */
    public BasicSimulationLauncher(Random _random) {
        random = _random ;
    }

    //------------------------------------------------------------
    /**
     * シミュレータ実体の取り出し
     */
    public EvacuationSimulator getSimulator() { return simulator; }

    //------------------------------------------------------------
    /**
     * 地図の読み込み
     */
    protected NetworkMap readMapWithName(String file_name, Random _random)
            throws IOException {
        FileInputStream fis = new FileInputStream(file_name);
        Document doc = ItkXmlUtility.singleton.streamToDoc(fis);
        if (doc == null) {
            System.err.println("ERROR Could not read map.");
            return null;
        }
        NodeList toplevel = doc.getChildNodes();
        if (toplevel == null) {
            System.err.println("BasiciSimulationLauncher.readMapWithName " +
                    "invalid inputted DOM object.");
            return null;
        }
        if (properties != null) {
            // NetworkMap の生成時に random オブジェクトを初期化する
            // (CUIモードとGUIモードでシミュレーション結果を一致させるため)
            _random.setSeed(properties.getRandseed());
        }
        // NetMAS based map
        NetworkMap network_map = new NetworkMap(_random);
        if (false == network_map.fromDOM(doc))
            return null;
        Itk.logInfo("Load Map File", file_name) ;
        network_map.setFileName(file_name);
        return network_map;
    }

    public NetmasPropertiesHandler getProperties() {
        return properties;
    }
}
