// -*- mode: java; indent-tabs-mode: nil -*-
/** Lexicon class
 * @author:: Itsuki Noda
 * @version:: 0.0 2014/12/24 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2014/12/24]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.util.Map ;
import java.util.HashMap ;
import java.util.ArrayList ;

import nodagumi.Itk.Itk ;

//======================================================================
/**
 * description of class Foo.
 */
public class Lexicon {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * look up table
     */
    private HashMap<String, Object> table = 
        new HashMap<String, Object>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * reversed look up table
     */
    private HashMap<Object, ArrayList<String>> reverseTable = 
        new HashMap<Object, ArrayList<String>>() ;

    //------------------------------------------------------------
    /**
     * constructor
     */
    public Lexicon() {
    }

    //------------------------------------------------------------
    /**
     * constructor with initial entries
     */
    public Lexicon(Object[][] entries) {
        registerMulti(entries) ;
    }

    //------------------------------------------------------------
    /**
     * look up
     */
    public Object lookUp(String word) {
        return table.get(word) ;
    }

    //------------------------------------------------------------
    /**
     * 逆引き look up
     */
    public ArrayList<String> lookUpByMeaning(Object meaning) {
        return reverseTable.get(meaning) ;
    }

    //------------------------------------------------------------
    /**
     * register word (forced)
     * @param word 登録する文字列
     * @param meaning 登録内容
     */
    public Lexicon register(String word, Object meaning) {
        return register(word, meaning, false) ;
    }

    //------------------------------------------------------------
    /**
     * register word
     * @param word 登録する文字列
     * @param meaning 登録内容
     * @param forceP 上書きするかどうか
     */
    public Lexicon register(String word, 
                           Object meaning,
                           boolean forceP) {
        if((! forceP) && (table.containsKey(word))) {
            Itk.dbgMsg("Warning:",
                       "duplicated word entry in a lexicon:" +
                       word + "(meaning=" + meaning.toString() + ")" +
                       "\n\t original meaning:" + table.get(word).toString() +
                       "\n\t !!! new registration is ignored.") ;
            return null ;
        } else {
            if(!reverseTable.containsKey(meaning))
                reverseTable.put(meaning, new ArrayList<String>()) ;
            table.put(word, meaning) ;
            reverseTable.get(meaning).add(word) ;
            return this ;
        }
    }

    //------------------------------------------------------------
    /**
     * register word multi
     * @param entries [[文字列, 内容],...] という配列
     */
    public Lexicon registerMulti(Object[][] entries) {
        return registerMulti(entries, false) ;
    }

    //------------------------------------------------------------
    /**
     * register word multi
     * @param entries [[文字列, 内容],...] という配列
     * @param forceP 上書きするかどうか
     */
    public Lexicon registerMulti(Object[][] entries, boolean forceP) {
        for(Object[] entry : entries) {
            String word = (String)entry[0] ;
            Object meaning = entry[1] ;
            register(word, meaning, forceP);
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * register word enum
     * @param enumClass Enum で宣言したクラス (<EnumType>.class) 
     */
    public Lexicon registerEnum(Class<?> enumClass) {
        return registerEnum(enumClass, false) ;
    }

    //------------------------------------------------------------
    /**
     * register word enum
     * @param enumClass Enum で宣言したクラス (<EnumType>.class)
     * @param forceP 上書きするかどうか
     */
    public Lexicon registerEnum(Class<?> enumClass, boolean forceP) {
        Enum<?>[] enumList = (Enum<?>[])enumClass.getEnumConstants() ;
        if(enumList == null) {
            Itk.dbgMsg("Warning:",
                       enumClass.toString() + " is not Enum class.") ;
            return null ;
        } else {
            for(Enum<?> enumItem : enumList) {
                registerEnum(enumItem, forceP) ;
            }
            return this ;
        }
    }

    //------------------------------------------------------------
    /**
     * register word enum
     * @param enumItem Enum データ
     */
    public Lexicon registerEnum(Enum<?> enumItem) {
        return registerEnum(enumItem, false) ;
    }

    //------------------------------------------------------------
    /**
     * register word enum
     * @param enumItem Enum データ
     * @param forceP 上書きするかどうか
     */
    public Lexicon registerEnum(Enum<?> enumItem, boolean forceP) {
        return register(enumItem.toString(), enumItem, forceP) ;
    }

    //------------------------------------------------------------
    /**
     * 文字列化
     */
    public String toString() {
        String contents = null ;
        for(Map.Entry<String, Object> entry : table.entrySet()) {
            if(contents == null) 
                contents = "" ;
            else
                contents += "," ;
            contents += (entry.getKey().toString() + ":" +
                         entry.getValue().toString()) ;
        }

        return ("#" + this.getClass().getSimpleName() + "{" + contents + "}") ;
    }

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * primal lexicon
     */
    static public Lexicon primal = new Lexicon() ;

    //============================================================
    //------------------------------------------------------------
    /**
     * look up in primal
     */
    static public Object _lookUp(String word) {
        return primal.lookUp(word) ;
    }

    //------------------------------------------------------------
    /**
     * 逆引き look up in primal
     */
    static public ArrayList<String> _lookUpByMeaning(Object meaning) {
        return primal.lookUpByMeaning(meaning) ;
    }

    //------------------------------------------------------------
    /**
     * register word (forced) in primal
     * @param word 登録する文字列
     * @param meaning 登録内容
     */
    static public Object _register(String word, Object meaning) {
        return primal.register(word, meaning) ;
    }

    //------------------------------------------------------------
    /**
     * register word in primal
     * @param word 登録する文字列
     * @param meaning 登録内容
     * @param forceP 上書きするかどうか
     */
    static public Object _register(String word,
                                   Object meaning,
                                   boolean forceP) {
        return primal.register(word, meaning, forceP) ;
    }

    //------------------------------------------------------------
    /**
     * register word multi in primal
     * @param array [[文字列, 内容],...] という配列
     */
    static public Lexicon _registerMulti(Object[][] array) {
        return primal.registerMulti(array) ;
    }

    //------------------------------------------------------------
    /**
     * register word multi in primal
     * @param array [[文字列, 内容],...] という配列
     * @param forceP 上書きするかどうか
     */
    static public Lexicon _registerMulti(Object[][] array, boolean forceP) {
        return primal.registerMulti(array, forceP) ;
    }

    //------------------------------------------------------------
    /**
     * register word enum in primal
     * @param enumClass Enum で宣言したクラス (<EnumType>.class) 
     */
    static public Lexicon _registerEnum(Class<?> enumClass) {
        return primal.registerEnum(enumClass) ;
    }

    //------------------------------------------------------------
    /**
     * register word enum in primal
     * @param enumClass Enum で宣言したクラス (<EnumType>.class)
     * @param forceP 上書きするかどうか
     */
    static public Lexicon _registerEnum(Class<?> enumClass, boolean forceP) {
        return primal.registerEnum(enumClass, forceP) ;
    }

    //------------------------------------------------------------
    /**
     * register word enum in primal
     * @param enumItem Enum データ
     */
    static public Lexicon _registerEnum(Enum<?> enumItem) {
        return primal.registerEnum(enumItem) ;
    }

    //------------------------------------------------------------
    /**
     * register word enum in primal
     * @param enumItem Enum データ
     * @param forceP 上書きするかどうか
     */
    static public Lexicon _registerEnum(Enum<?> enumItem, boolean forceP) {
        return primal.registerEnum(enumItem, forceP) ;
    }

} // class Lexicon

