// -*- mode: java; indent-tabs-mode: nil -*-
/** Shifting String List
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/12 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/23]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.io.IOException;
import com.opencsv.CSVParser ;

import nodagumi.Itk.Itk;

//======================================================================
/**
 * String 配列を、先頭から順番に使っていく時のためのクラス。
 */
public class ShiftingStringList {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 現在位置
     */
    private int index = 0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * カラムの列
     */
    private String[] stringList = null ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ShiftingStringList(String[] _stringList) {
        index = 0 ;
        stringList = _stringList ;
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
        return stringList.length ;
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
        return nth(0) ;
    }

    //------------------------------------------------------------
    /**
     * 現在のindexよりn番目を取り出す。
     */
    public String nth(int n) {
        return stringList[index + n] ;
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

    //============================================================
    // static definitions

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * shared CSV parser
     */
    static private CSVParser csvParser = new CSVParser() ;

    //------------------------------------------------------------
    /**
     * CSV parser の特殊文字の変更
     */
    static public void setCsvSpecialChars(char separatorChar,
                                          char quoteChar,
                                          char escapeChar) {
        csvParser = new CSVParser(separatorChar, quoteChar, escapeChar) ;
    }

    //------------------------------------------------------------
    /**
     * CSV の一行 (row)  を読み込んで、ShiftingStringList をひとつ作る。
     */
    static public ShiftingStringList newFromCsvRow(String csvRow) 
        throws IOException
    {
        return new ShiftingStringList(csvParser.parseLine(csvRow)) ;
    }

} // class ShitingStringList.java


