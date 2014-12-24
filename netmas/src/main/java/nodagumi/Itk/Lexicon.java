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
    public Object register(String word, Object meaning) {
        return register(word, meaning, true) ;
    }

    //------------------------------------------------------------
    /**
     * register word
     * @param word 登録する文字列
     * @param meaning 登録内容
     * @param forceP 上書きするかどうか
     */
    public Object register(String word, 
                           Object meaning,
                           boolean forceP) {
        if((! forceP) && (table.containsKey(word))) {
            return null ;
        } else {
            if(!reverseTable.containsKey(meaning))
                reverseTable.put(meaning, new ArrayList<String>()) ;
            table.put(word, meaning) ;
            reverseTable.get(meaning).add(word) ;
            return meaning ;
        }
    }

    //------------------------------------------------------------
    /**
     * register word multi
     * @param entries [[文字列, 内容],...] という配列
     */
    public Lexicon registerMulti(Object[][] entries) {
        return registerMulti(entries, true) ;
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

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * original lexicon
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
    public Lexicon _registerMulti(Object[][] array) {
        return primal.registerMulti(array) ;
    }

    //------------------------------------------------------------
    /**
     * register word multi in primal
     * @param array [[文字列, 内容],...] という配列
     * @param forceP 上書きするかどうか
     */
    public Lexicon _registerMulti(Object[][] array, boolean forceP) {
        return primal.registerMulti(array, forceP) ;
    }

} // class Foo

