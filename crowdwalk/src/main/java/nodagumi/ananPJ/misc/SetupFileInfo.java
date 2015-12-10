// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.io.InputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import net.arnx.jsonic.JSON ;

import nodagumi.Itk.Itk;
import nodagumi.Itk.Term;

//======================================================================
/**
 * シミュレーションで用いるデータをまとめて保持するクラス。
 */
public class SetupFileInfo {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 実験設定ファイル。
     */
    private static String propertiesFile = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * マップファイル
     */
    private String networkMapFile = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 汚染地域データのファイル
     */
    private String pollutionFile = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント生成ルールのファイル
     */
    private String generationFile = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * シナリオファイル
     */
    private String scenarioFile = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback file
     */
    private String fallbackFile = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback parameter slot name.
     */
    static private final String FallbackSlot = "_fallback" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * resource 中の fallback parameter を収めた JSON ファイル。
     * @see
     */
    static public final String FallbackResource = "/fallbackParameters.json" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback parameter 格納用変数。
     */
    public Term fallbackParameters = null ;

    //------------------------------------------------------------
    /**
     * constructor
     */
    public SetupFileInfo() {
    }

    //------------------------------------------------------------
    /**
     * 実験設定ファイル
     */
    public void setPropertiesFile(String filename) {
        propertiesFile = filename ;
    }

    public String getPropertiesFile() {
        return propertiesFile ;
    }

    //------------------------------------------------------------
    /**
     * マップファイル名
     */
    public String getNetworkMapFile() {
        return networkMapFile;
    }

    /**
     * マップファイル名
     */
    public void setNetworkMapFile(String file_name) {
        networkMapFile = file_name;
    }

    //------------------------------------------------------------
    /**
     * pollutionファイル。
     */
    public String getPollutionFile() {
        if (pollutionFile == null)
            return null;
        File pollution_file = new File(pollutionFile);
        return pollution_file.getPath();
    }

    /**
     * pollutionファイル。
     */
    public void setPollutionFile(String s) {
        pollutionFile = s;
    }

    //------------------------------------------------------------
    /**
     * エージェント生成ファイル。
     */
    public String getGenerationFile() {
        if (generationFile == null)
            return null;
        File generation_file = new File(generationFile);
        return generation_file.getPath();
    }

    /**
     * エージェント生成ファイル。
     */
    public void setGenerationFile(String s) {
        generationFile = s;
    }

    //------------------------------------------------------------
    /**
     * シナリオファイル。
     */
    public String getScenarioFile() {
        if (scenarioFile == null)
            return null;
        File _scenarioFile = new File(scenarioFile) ;
        return _scenarioFile.getPath();
    }

    /**
     * シナリオファイル。
     */
    public void setScenarioFile(String s) {
        scenarioFile = s;
    }

    //------------------------------------------------------------
    /**
     * fallback （デフォルトセッティング）のファイル取得
     */
    public String getFallbackFile() {
        return fallbackFile ;
    }

    /**
     * fallback （デフォルトセッティング）のファイルセット
     */
    public void setFallbackFile(String s) {
        fallbackFile = s ;
    }

    //------------------------------------------------------------
    /**
     * fallback （デフォルトセッティング）の読み込み
     * @param scanResourceP : resource の fallback も読み込むかどうか
     */
    public void scanFallbackFile(ArrayList<String> commandLineFallbacks,
                                 boolean scanResourceP) {
        if(fallbackFile != null) {
            try {
                BufferedReader buffer =
                    new BufferedReader(new FileReader(fallbackFile)) ;
                fallbackParameters = Term.newByScannedJson(JSON.decode(buffer),
                                                           true) ;
                Itk.logInfo("Load Fallback File", fallbackFile) ;
            } catch (Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("Can not scan a fallback parameter file:",
                             fallbackFile) ;
                Itk.logError_("Exception",ex) ;
            }
        } else {
            fallbackParameters = Term.newObjectTerm() ;
        }

        if(scanResourceP) {
            try {
                InputStream istrm =
                    getClass().getResourceAsStream(FallbackResource) ;
                Term finalFallback =
                    Term.newByScannedJson(JSON.decode(istrm),true) ;
                attachFallback(fallbackParameters, finalFallback) ;
            } catch (Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("Can not scan a fallback resource file.") ;
                Itk.logError_("Exception",ex) ;
            }
        }

        // コマンドラインで指定した fallback を先頭に追加していく。
        // コマンドラインでは、後から追加したものほど優先。
        if(commandLineFallbacks != null) {
            for(String fallbackString : commandLineFallbacks) {
                Term newFallback =
                    Term.newByScannedJson(JSON.decode(fallbackString), true) ;
                attachFallback(newFallback, fallbackParameters) ;
                fallbackParameters = newFallback ;
            }
        }

        Itk.logInfo("fallbackParameters",
                    unifiedFallbackParameters().toJson(true)) ;
    }

    //============================================================
    // fallback 関係
    //============================================================
    //------------------------------------------------------------
    /**
     * パラメータ設定に fallback を追加する。
     * 内部的には、fallback は、以下のように格納される。
     * {@code params} の内容が {@literal { "foo" : { "bar" : 1 }, "baz" : 2}}
     * {@code fallback} の内容が {@literal { "foo" : { "bar2" : 2}}}
     * の場合、{@code attachFallback} された結果は、
     * {@literal { "foo" : { "bar" : 1 }, "baz" : 2, "_fallback" : { "foo" : { "bar2" : 2}}}} となる。
     * <br>
     * 最終的に、fallback 情報は、以下の形式となる。
     * <pre>{@literal
     * { ...<コマンドラインの--fallbackで指定したJSON>... ,
     *   "_fallback" : { ...<propertiesの"fallback_file"で指定したファイルのJSON>...,
     *                   "_fallback" : { ..."src/main/resources/fallbackParameters.json"のJSON>...} }
     * }
     * }</pre>
     *
     * @param params : もとになる parameter 設定用 Term
     * @param fallback : params の後に追加する fallback
     * @return fallbackを含んだ 設定Term。
     * @see nodagumi.ananPJ.package-info
     */
    static public Term attachFallback(Term params, Term fallback) {
        params.setArg(FallbackSlot, fallback) ;
        return params ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * fallback のフィルタリング
     * @param fallbacks : filter する fallback
     * @param tag : fallbacks の中から、tag をたどって filter する。
     */
    static public Term filterFallbackTerm(Term fallbacks, String tag) {
        return fallbacks.filterArgTerm(tag, FallbackSlot) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * fallback の fetch (Term)
     * @param fallbacks : filter する fallback
     * @param tag : fallbacks の中から、tag をたどって filter する。
     * @param finalFallbackValue : 最後の値
     */
    static public Term fetchFallbackTerm(Term fallbacks, String tag,
                                         Term finalFallbackValue) {
        return fallbacks.fetchArgTerm(tag, FallbackSlot, finalFallbackValue) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * fallback の fetch (String)
     * @param fallbacks : filter する fallback
     * @param tag : fallbacks の中から、tag をたどって filter する。
     * @param finalFallbackValue : 最後の値
     */
    static public String fetchFallbackString(Term fallbacks, String tag,
                                             String finalFallbackValue) {
        return fallbacks.fetchArgString(tag, FallbackSlot, finalFallbackValue) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * fallback の fetch (double)
     * @param fallbacks : filter する fallback
     * @param tag : fallbacks の中から、tag をたどって filter する。
     * @param finalFallbackValue : 最後の値
     */
    static public double fetchFallbackDouble(Term fallbacks, String tag,
                                             double finalFallbackValue) {
        return fallbacks.fetchArgDouble(tag, FallbackSlot, finalFallbackValue) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * fallback の fetch (int)
     * @param fallbacks : filter する fallback
     * @param tag : fallbacks の中から、tag をたどって filter する。
     * @param finalFallbackValue : 最後の値
     */
    static public int fetchFallbackInt(Term fallbacks, String tag,
                                       int finalFallbackValue) {
        return fallbacks.fetchArgInt(tag, FallbackSlot, finalFallbackValue) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * fallback の fetch (boolean)
     * @param fallbacks : filter する fallback
     * @param tag : fallbacks の中から、tag をたどって filter する。
     * @param finalFallbackValue : 最後の値
     */
    static public boolean fetchFallbackBoolean(Term fallbacks, String tag,
                                               boolean finalFallbackValue) {
        return fallbacks.fetchArgBoolean(tag, FallbackSlot, finalFallbackValue) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * fallback の単一化
     * @param fallbacks : 単一化する fallback
     */
    static public Term unifyFallbacks(Term fallbacks) {
        return fallbacks.unifyFallbacks(FallbackSlot) ;
    }

    //------------------------------------------------------------
    /**
     * 単一化された fallbackParametes を返す。
     */
    public Term unifiedFallbackParameters() {
        return unifyFallbacks(fallbackParameters) ;
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
