// -*- mode: java; indent-tabs-mode: nil -*-
/** Json Formatter
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/06/13 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/06/13]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.lang.Iterable;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.Writer;
import java.io.PrintStream;


//======================================================================
/**
 * Json 形式での Logger へのフォーマット出力を整理するためのライブラリ
 */
public class JsonFormatter<T> {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Member情報
     */
    private ArrayList<Member> memberList = new ArrayList<Member>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Member定義テーブル
     */
    private HashMap<String, Member> memberTable = new HashMap<String, Member>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 区切り記号
     */
    final private String memberSeparator = "," ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Member の Value の Object への再帰的なアクセスを表現するときの
     * key の列のセパレータ。
     */
    private String keyNestSeparator = "/" ;
    
    //============================================================
    //============================================================
    /**
     * 全体の形式設定のタイプ。
     */
    static public enum OverallStyle {
        /** 各々の行で record が完結。 */
        RecordPerLine,
        /** ログ全体で record object の配列になっている。 */
        RecordArray,
        /** ログ全体で record object の配列になっており、各々 pretty print。 */
        PrettyPrint
    } ;

    //========================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 全体の形式設定のタイプ。
     */
    static public Lexicon overallStyleLexicon = new Lexicon() ;
    static {
        overallStyleLexicon.registerEnum(OverallStyle.class) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 全体の形式設定のタイプ。
     */
    public OverallStyle overallStyle = OverallStyle.RecordPerLine ;

    //----------------------------------------
    /**
     * 全体の形式設定のタイプの設定。
     */
    public OverallStyle setOverallStyle(OverallStyle style) {
        overallStyle = style ;
        return overallStyle ;
    }
    //----------------------------------------
    /**
     * 全体の形式設定のタイプの設定
     */
    public OverallStyle setOverallStyle(String styleName) {
        OverallStyle style =
            (OverallStyle)overallStyleLexicon.lookUp(styleName) ;
        if(style != null) {
            return setOverallStyle(style) ;
        } else {
            Itk.logError("Unknown OverallStyle name:", styleName) ;
            return null ;
        }
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 出力した record の数。
     * overall Style で、配列表現をとるときに利用。
     */
    public int recordCounter = 0 ;

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     */
    public JsonFormatter() {
    }

    //------------------------------------------------------------
    /**
     * Member情報の定義の登録。
     * 以下のように記述することを想定。
     *    jsonFormatter.registerMember(jsonFormatter.new Member("foo"){
     *         public String value(T object) { return object.foo() ; }
     *         }) ;
     * この定義のあと、jsonFormatter.addMember("foo") や、
     * jsonFormatter.setMember(["foo", "bar", ...]) などで実際のフォーマット
     * を指定。
     * @param member:: member 情報
     */
    public JsonFormatter registerMember(Member member) {
        memberTable.put(member.key, member) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * Member情報追加。
     * 以下のように記述することを想定。
     * <pre>{@code
     *    jsonFormatter.addMember(jsonFormatter.new Member("foo"){
     *         public String value(T object) { return object.foo() ; }
     *         }) ;
     * }</pre>
     * 同時に、registerMemberも行う。
     * @param member:: member 情報
     */
    public JsonFormatter addMember(Member member) {
        registerMember(member) ;
	memberList.add(member) ;
	return this ;
    }

    //------------------------------------------------------------
    /**
     * key名によるMember情報追加。
     * 以下のように記述することを想定。
     * <pre>{@code
     *    jsonFormatter.addMember("foo") ;
     * }</pre>
     * 指定された memberKey に対応する Member の定義は、すでに registerMember
     * で行われているものとする。
     * @param memberKey:: member のキー
     */
    public JsonFormatter addMember(String memberKey) {
        Member member = memberTable.get(memberKey) ;
        if(member != null) {
            memberList.add(member) ;
            return this ;
        } else {
            Itk.logError("unknown key string for member:", memberKey) ;
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * key名のリストによるMember情報設定。
     * @param memberKeyList:: member の key名のリスト
     */
    public JsonFormatter setMembers(List<String> memberKeyList) {
        memberList.clear() ;
        for(String memberKey : memberKeyList) {
            addMember(memberKey) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * keyのリストによるMember情報設定。(Termによる指定)
     * @param memberKeyList:: member の keyのリスト
     */
    public JsonFormatter setMembersByTerm(List<Term> memberKeyList) {
        memberList.clear() ;
        for(Term memberKey : memberKeyList) {
            addMember(memberKey.getString()) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    // ヘッダー関連
    //------------------------------------------------------------
    /**
     * ヘッダー出力。
     * @param logger : 出力する Logger
     */
    public void outputHeaderToLoggerInfo(Logger logger) {
        StringBuilder buffer = outputHeaderToBuffer() ;
        if(buffer != null) {
            logger.info(buffer.toString()) ;
        }
    }

    //----------------------------------------
    /**
     * ヘッダー出力。
     * @param writer : 出力する writer
     */
    public void outputHeaderToWriter(Writer writer) {
        StringBuilder buffer = outputHeaderToBuffer() ;
        if(buffer != null) {
            buffer.append("\n") ;
            try {
                writer.write(buffer.toString()) ;
            } catch(Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("IOException: ", writer, buffer) ;
            }
        }
    }

    //----------------------------------------
    /**
     * ヘッダー出力。
     * @param stream : 出力する stream
     */
    public void outputHeaderToStream(PrintStream stream) {
        StringBuilder buffer = outputHeaderToBuffer() ;
        if(buffer != null) {
            stream.println(buffer.toString()) ;
        }
    }

    //----------------------------------------
    /**
     * ヘッダー出力。
     * もし、ヘッダー等が必要ない場合は、nullを返す。
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputHeaderToBuffer() {
        if(overallStyle != OverallStyle.RecordPerLine) {
            return outputHeaderToBuffer(new StringBuilder()) ;
        } else {
            return null ;
        }
    }

    //----------------------------------------
    /**
     * ヘッダー出力。
     * @param buffer:: StringBuilder を与える。
     * @return buffer を内容を追加して返す。
     */
    public StringBuilder outputHeaderToBuffer(StringBuilder buffer) {
        if(overallStyle != OverallStyle.RecordPerLine) {
            buffer.append("[") ;
        }
        recordCounter = 0 ;
	return buffer ;
    }

    //------------------------------------------------------------
    // テイル関連
    //------------------------------------------------------------
    /**
     * テイル出力。
     * @param logger : 出力する Logger
     */
    public void outputTailerToLoggerInfo(Logger logger) {
        StringBuilder buffer = outputTailerToBuffer() ;
        if(buffer != null) {
            logger.info(buffer.toString()) ;
        }
    }

    //----------------------------------------
    /**
     * ヘッダー出力。
     * @param writer : 出力する writer
     */
    public void outputTailerToWriter(Writer writer) {
        StringBuilder buffer = outputTailerToBuffer() ;
        if(buffer != null) {
            buffer.append("\n") ;
            try {
                writer.write(buffer.toString()) ;
            } catch(Exception ex) {
                ex.printStackTrace() ;
                Itk.logError("IOException: ", writer, buffer) ;
            }
        }
    }

    //----------------------------------------
    /**
     * ヘッダー出力。
     * @param stream : 出力する stream
     */
    public void outputTailerToStream(PrintStream stream) {
        StringBuilder buffer = outputTailerToBuffer() ;
        if(buffer != null) {
            stream.println(buffer.toString()) ;
        }
    }

    //----------------------------------------
    /**
     * テイル出力。
     * もし、ヘッダー等が必要ない場合は、nullを返す。
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputTailerToBuffer() {
        if(overallStyle != OverallStyle.RecordPerLine) {
            return outputTailerToBuffer(new StringBuilder()) ;
        } else {
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * テイル出力。
     * @param buffer:: StringBuilder を与える。
     * @return buffer を内容を追加して返す。
     */
    public StringBuilder outputTailerToBuffer(StringBuilder buffer) {
        if(overallStyle != OverallStyle.RecordPerLine) {
            buffer.append("]") ;
        }
	return buffer ;
    }

    //------------------------------------------------------------
    // データ出力関連
    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param logger : 出力する Logger
     * @param object : Member#value に引き渡すデータ。
     */
    public void outputRecordToLoggerInfo(Logger logger, T object) {
	StringBuilder buffer = outputRecordToBuffer(object) ;
	logger.info(buffer.toString()) ;
    }

    /**
     */
    public void outputRecordToLoggerInfo(Logger logger, T object1,
                                         Object object2) {
	StringBuilder buffer = outputRecordToBuffer(object1, object2) ;
	logger.info(buffer.toString()) ;
    }

    /**
     */
    public void outputRecordToLoggerInfo(Logger logger, T object1,
					Object object2, Object object3) {
	StringBuilder buffer = outputRecordToBuffer(object1, object2, object3) ;
	logger.info(buffer.toString()) ;
    }

    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param writer : 出力する Writer
     * @param object : Column#value に引き渡すデータ。
     */
    public void outputRecordToWriter(Writer writer, T object) {
	StringBuilder buffer = outputRecordToBuffer(object) ;
	buffer.append("\n") ;
	try {
	    writer.write(buffer.toString()) ;
	} catch(Exception ex) {
	    ex.printStackTrace() ;
	    Itk.logError("IOException: ", writer, buffer) ;
	}
    }

    /**
     */
    public void outputRecordToWriter(Writer writer, T object1,
                                     Object object2) {
	StringBuilder buffer = outputRecordToBuffer(object1, object2) ;
	buffer.append("\n") ;
	try {
	    writer.write(buffer.toString()) ;
	} catch(Exception ex) {
	    ex.printStackTrace() ;
	    Itk.logError("IOException: ", writer, buffer) ;
	}
    }

    /**
     */
    public void outputRecordToWriter(Writer writer, T object1,
				    Object object2, Object object3) {
	StringBuilder buffer = outputRecordToBuffer(object1, object2, object3) ;
	try {
	    writer.write(buffer.toString()) ;
	} catch(Exception ex) {
	    ex.printStackTrace() ;
	    Itk.logError("IOException: ", writer, buffer) ;
	}
    }

    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param stream : 出力する Stream
     * @param object : Member#value に引き渡すデータ。
     */
    public void outputRecordToStream(PrintStream stream, T object) {
	StringBuilder buffer = outputRecordToBuffer(object) ;
	stream.println(buffer.toString()) ;
    }

    /**
     */
    public void outputRecordToStream(PrintStream stream, T object1,
				    Object object2) {
	StringBuilder buffer = outputRecordToBuffer(object1, object2) ;
	stream.println(buffer.toString()) ;
    }

    /**
     */
    public void outputRecordToStream(PrintStream stream, T object1,
				    Object object2, Object object3) {
	StringBuilder buffer = outputRecordToBuffer(object1, object2, object3) ;
	stream.println(buffer.toString()) ;
    }

    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param object : Member#value に引き渡すデータ。
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputRecordToBuffer(T object) {
	return outputRecordToBuffer(new StringBuilder(), object) ;
    }

    /**
     */
    public StringBuilder outputRecordToBuffer(T object1, Object object2) {
	return outputRecordToBuffer(new StringBuilder(), object1, object2) ;
    }

    /**
     */
    public StringBuilder outputRecordToBuffer(T object1, Object object2,
					     Object object3) {
	return outputRecordToBuffer(new StringBuilder(), object1, object2, object3);
    }

    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param buffer : 出力する buffer。
     * @param object : Column#value に引き渡すデータ。
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputRecordToBuffer(StringBuilder buffer, T object) {
        Term term = new Term() ;
	for(Member member : memberList) {
            addMemberInTerm(term, member,
                            member.value(object)) ;
	}

	return outputTermToBuffer(buffer, term) ;
    }

    /**
     */
    public StringBuilder outputRecordToBuffer(StringBuilder buffer, T object1,
					     Object object2) {
        Term term = new Term() ;
	for(Member member : memberList) {
            addMemberInTerm(term, member,
                            member.value(object1, object2)) ;
	}

	return outputTermToBuffer(buffer, term) ;
    }

    /**
     */
    public StringBuilder outputRecordToBuffer(StringBuilder buffer, T object1,
					     Object object2, Object object3) {
        Term term = new Term() ;
	for(Member member : memberList) {
            addMemberInTerm(term, member,
                            member.value(object1, object2, object3)) ;
	}

	return outputTermToBuffer(buffer, term) ;
    }

    //------------------------------------------------------------
    /**
     * output a term to a buffer with format.
     */
    public StringBuilder outputTermToBuffer(StringBuilder buffer,
                                            Term term) {
        // output separater and newline if need.
        boolean prettyPrintP = (overallStyle == OverallStyle.PrettyPrint) ;
        if(overallStyle != OverallStyle.RecordPerLine) {
            if(recordCounter > 0) {
                buffer.append(memberSeparator) ;
                if(prettyPrintP) { buffer.append("\n") ; } ;
            } else {
                if(!prettyPrintP) { buffer.append(" ") ; } ;
            }
        }
        
        // output body
        buffer.append(term.toJson(prettyPrintP)) ;

        recordCounter++ ;

        return buffer ;
    }
                      
    //------------------------------------------------------------
    /**
     * member を Term にセット。
     * @param term : 出力する先の Term。
     * @param member : 出力する member
     * @return 内容を追加した term
     */
    public Term addMemberInTerm(Term term, Member member, Object value) {
        Term targetTerm = term ;
        for(int i = 0 ; i < member.keyNest.size() - 1 ; i++) {
            String key = member.keyNest.get(i) ;
            Object nestedValue = targetTerm.getArg(key) ;
            if(nestedValue == null) {
                nestedValue = new Term() ;
                targetTerm.setArg(key, nestedValue, false) ;
            } else if (!(nestedValue instanceof Term)) {
                Itk.logError("Inconsistent keys in members of JsonFormatter:",
                             "whole key=", member.key,
                             ", current key=", key) ;
                Itk.quitByError() ;
            }
            targetTerm = (Term)nestedValue ;
        }
        targetTerm.setArg(member.lastKey(), value, false) ;
        
        return term ;
    }

    //------------------------------------------------------------
    /**
     * データ行を全て出力。
     * @param logger : 出力する Logger
     * @param objectList : Member#value に引き渡すデータのIterable
     */
    public void outputAllRecordToLoggerInfo(Logger logger,
                                            Iterable<T> objectList) {
	for(T object : objectList) {
	    outputRecordToLoggerInfo(logger, object) ;
	}
    }

    /**
     */
    public void outputAllRecordToLoggerInfo(Logger logger,
                                            Iterable<T> objectList,
                                            Object object2) {
	for(T object : objectList) {
	    outputRecordToLoggerInfo(logger, object, object2) ;
	}
    }

    /**
     */
    public void outputAllRecordToLoggerInfo(Logger logger,
                                            Iterable<T> objectList,
                                            Object object2, Object object3) {
	for(T object : objectList) {
	    outputRecordToLoggerInfo(logger, object, object2, object3) ;
	}
    }

    //------------------------------------------------------------
    /**
     * データ行を全て出力。
     * @param writer : 出力する Writer
     * @param objectList : Member#value に引き渡すデータのIterable
     */
    public void outputAllRecordToWriter(Writer writer,
                                        Iterable<T> objectList) {
	for(T object : objectList) {
	    outputRecordToWriter(writer, object) ;
	}
    }

    /**
     */
    public void outputAllRecordToWriter(Writer writer,
                                        Iterable<T> objectList,
                                        Object object2) {
	for(T object : objectList) {
	    outputRecordToWriter(writer, object, object2) ;
	}
    }

    /**
     */
    public void outputAllRecordToWriter(Writer writer,
                                        Iterable<T> objectList,
                                        Object object2, Object object3) {
	for(T object : objectList) {
	    outputRecordToWriter(writer, object, object2, object3) ;
	}
    }

    //------------------------------------------------------------
    /**
     * データ行を全て出力。
     * @param stream : 出力する Stream
     * @param objectList : Column#value に引き渡すデータのIterable
     */
    public void outputAllRecordToStream(PrintStream stream,
                                        Iterable<T> objectList) {
	for(T object : objectList) {
	    outputRecordToStream(stream, object) ;
	}
    }

    /**
     */
    public void outputAllRecordToStream(PrintStream stream,
                                        Iterable<T> objectList,
                                        Object object2) {
	for(T object : objectList) {
	    outputRecordToStream(stream, object, object2) ;
	}
    }

    /**
     */
    public void outputAllRecordToStream(PrintStream stream,
                                        Iterable<T> objectList,
                                        Object object2, Object object3) {
	for(T object : objectList) {
	    outputRecordToStream(stream, object, object2, object3) ;
	}
    }

    //============================================================
    //============================================================
    public class Member {
	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * Member の key
	 */
	public String key ;

        /**
	 * Member の key の入れ子リスト
	 */
	public ArrayList<String> keyNest = null ;


	//----------------------------------------
	/**
	 * コンストラクタ
	 */
	public Member(String _key) {
            extractKeyNest(_key) ;
	}

	//----------------------------------------
	/**
	 * key の入れ子構造分解
	 */
	public void extractKeyNest(String _key) {
	    key = _key ;
            keyNest = new ArrayList<String>() ;
            for(String subKey : key.split(keyNestSeparator)) {
                keyNest.add(subKey) ;
            }
	}

	//----------------------------------------
	/**
	 * key が入れ子かどうかのチェック
	 */
        public String lastKey() {
            return keyNest.get(keyNest.size()-1) ;
        }
            
	//----------------------------------------
	/**
	 * 値の出力。
         * 使われる場合、各々の Member で再定義されるべき。
         * 戻り値は、String, Term, Integer, Double のいずれかであるべき。
	 */
	public Object value(T object) {
	    Itk.logError("Member#value(object) is not implemented.") ;
	    return "" ;
	}

	//----------------------------------------
	/**
	 * 値の出力(2入力)
         * 使われる場合、各々の Member で再定義されるべき。
	 */
	public Object value(T object1, Object object2) {
	    Itk.logError("Column#value(obj1, obj2) is not implemented.") ;
	    return "" ;
	}

	//----------------------------------------
	/**
	 * 値の出力(3入力)
         * 使われる場合、各々の Member で再定義されるべき。
	 */
	public Object value(T object1, Object object2, Object object3) {
	    Itk.logError("Column#value(obj1, obj2, obj3) is not implemented.") ;
	    return "" ;
	}
    }

} // class Foo

