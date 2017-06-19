// -*- mode: java; indent-tabs-mode: nil -*-
/** CSV Formatter
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
 * CSV のフォーマット出力を整理するためのライブラリ
 */
public class CsvFormatter<T> {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Column情報
     */
    private ArrayList<Column> columnList = new ArrayList<Column>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Column定義テーブル
     */
    private HashMap<String, Column> columnTable = new HashMap<String, Column>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 区切り記号
     */
    private String columnSeparator = "," ;
    private String rawSeparator = "," ;

    /**
     * クオート文字
     */
    private String quoteCharacter = "\"" ;
    private String escapedQuoteCharacter = "\\\"" ;

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     */
    public CsvFormatter() {
    }

    //------------------------------------------------------------
    /**
     * Column情報の定義の登録。
     * 以下のように記述することを想定。
     *    csvFormatter.registerColumn(csvFormatter.new Column("foo"){
     *         public String value(T object) { return object.foo() ; }
     *         }) ;
     * この定義のあと、csvFormatter.addColumn("foo") や、
     * csvFormatter.setColumns(["foo", "bar", ...]) などで実際のフォーマと指定。
     * @param column:: column 情報
     */
    public CsvFormatter registerColumn(Column column) {
        columnTable.put(column.header,column) ;
        return this ;
    }

    //------------------------------------------------------------
    /**
     * Column情報追加。
     * 以下のように記述することを想定。
     * <pre>{@code
     *    csvFormatter.addColumn(csvFormatter.new Column("foo"){
     *         public String value(T object) { return object.foo() ; }
     *         }) ;
     * }</pre>
     * 同時に、registerColumnも行う。
     * @param column:: column 情報
     */
    public CsvFormatter addColumn(Column column) {
        registerColumn(column) ;
	columnList.add(column) ;
	return this ;
    }

    //------------------------------------------------------------
    /**
     * header名によるColumn情報追加。
     * 以下のように記述することを想定。
     * <pre>{@code
     *    csvFormatter.addColumn("foo") ;
     * }</pre>
     * 指定された columnName に対応する Column の定義は、すでに registerColumn
     * で行われているものとする。
     * @param columnName:: column の名前
     */
    public CsvFormatter addColumn(String columnName) {
        Column column = columnTable.get(columnName) ;
        if(column != null) {
            columnList.add(column) ;
            return this ;
        } else {
            Itk.logError("unknown header string for column:", columnName) ;
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * header名のリストによるColumn情報設定。
     * @param columnNameList:: column の header名のリスト
     */
    public CsvFormatter setColumns(List<String> columnNameList) {
        columnList.clear() ;
        for(String columnName : columnNameList) {
            addColumn(columnName) ;
        }
        return this ;
    }

    //------------------------------------------------------------
    /**
     * header名のリストによるColumn情報設定。(Termによる指定)
     * @param columnNameList:: column の header名のリスト
     */
    public CsvFormatter setColumnsByTerm(List<Term> columnNameList) {
        columnList.clear() ;
        for(Term columnName : columnNameList) {
            addColumn(columnName.getString()) ;
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
	logger.info(buffer.toString()) ;
    }

    //------------------------------------------------------------
    /**
     * ヘッダー出力。
     * @param writer : 出力する writer
     */
    public void outputHeaderToWriter(Writer writer) {
	StringBuilder buffer = outputHeaderToBuffer() ;
	buffer.append("\n") ;
	try {
	    writer.write(buffer.toString()) ;
	} catch(Exception ex) {
	    ex.printStackTrace() ;
	    Itk.logError("IOException: ", writer, buffer) ;
	}
    }

    //------------------------------------------------------------
    /**
     * ヘッダー出力。
     * @param stream : 出力する stream
     */
    public void outputHeaderToStream(PrintStream stream) {
	StringBuilder buffer = outputHeaderToBuffer() ;
	stream.println(buffer.toString()) ;
    }

    //------------------------------------------------------------
    /**
     * ヘッダー出力。
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputHeaderToBuffer() {
	return outputHeaderToBuffer(new StringBuilder()) ;
    }

    //------------------------------------------------------------
    /**
     * ヘッダー出力。
     * @param buffer:: StringBuilder を与える。
     * @return buffer を内容を追加して返す。
     */
    public StringBuilder outputHeaderToBuffer(StringBuilder buffer) {
	int i = 0 ;
	for(Column column : columnList) {
	    if(i > 0) buffer.append(columnSeparator) ;
	    buffer.append(column.header) ;
	    i++ ;
	}
	return buffer ;
    }

    //------------------------------------------------------------
    // データ出力関連
    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param logger : 出力する Logger
     * @param object : Column#value に引き渡すデータ。
     */
    public void outputValueToLoggerInfo(Logger logger, T object,
                                        Object... auxObjects) {
	StringBuilder buffer = outputValueToBuffer(object, auxObjects) ;
	logger.info(buffer.toString()) ;
    }

    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param writer : 出力する Writer
     * @param object : Column#value に引き渡すデータ。
     */
    public void outputValueToWriter(Writer writer, T object,
                                    Object... auxObjects) {
	StringBuilder buffer = outputValueToBuffer(object, auxObjects) ;
	buffer.append("\n") ;
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
     * @param object : Column#value に引き渡すデータ。
     */
    public void outputValueToStream(PrintStream stream, T object,
                                    Object... auxObjects) {
	StringBuilder buffer = outputValueToBuffer(object, auxObjects) ;
	stream.println(buffer.toString()) ;
    }

    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param object : Column#value に引き渡すデータ。
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputValueToBuffer(T object,
                                             Object... auxObjects) {
	return outputValueToBuffer(new StringBuilder(), object, auxObjects) ;
    }

    //------------------------------------------------------------
    /**
     * データ列出力。
     * @param buffer : 出力する buffer。
     * @param i : 何カラム目かの index。セパレータ出力に使う。
     * @param value : 出力するデータの文字列
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputColumnToBuffer(StringBuilder buffer,
                                              int i, String value) {
        if(i > 0) buffer.append(columnSeparator) ;
        
        boolean shouldQuote = value.contains(columnSeparator) ;
        boolean shouldEscape = value.contains(quoteCharacter) ;
        
        if(shouldQuote) buffer.append(quoteCharacter) ;
        if(shouldEscape) {
            buffer.append(value.replaceAll(quoteCharacter,
                                           escapedQuoteCharacter)) ;
        } else {
            buffer.append(value) ;
        }
        if(shouldQuote) buffer.append(quoteCharacter) ;

        return buffer ;
    }

    //------------------------------------------------------------
    /**
     * データ行出力。
     * @param buffer : 出力する buffer。
     * @param object : Column#value に引き渡すデータ。
     * @return 内容を追加した buffer。
     */
    public StringBuilder outputValueToBuffer(StringBuilder buffer, T object,
                                             Object... auxObjects) {
	int i = 0 ;
	for(Column column : columnList) {
            String value = null ;
            switch(auxObjects.length) {
            case 0:
                value = column.value(object) ;
                break ;
            case 1:
                value = column.value(object, auxObjects[0]) ;
                break ;
            case 2:
                value = column.value(object, auxObjects[0], auxObjects[1]) ;
                break ;
            default:
                Itk.logError("too many auxObjects[]") ;
                Itk.quitByError() ;
            }
            outputColumnToBuffer(buffer, i, value) ;
	    i++ ;
	}
	return buffer ;
    }

    //------------------------------------------------------------
    /**
     * データ行を全て出力。
     * @param logger : 出力する Logger
     * @param objectList : Column#value に引き渡すデータのIterable
     */
    public void outputAllValueToLoggerInfo(Logger logger,
					   Iterable<T> objectList,
                                           Object... auxObjects) {
	for(T object : objectList) {
	    outputValueToLoggerInfo(logger, object, auxObjects) ;
	}
    }

    //------------------------------------------------------------
    /**
     * データ行を全て出力。
     * @param writer : 出力する Writer
     * @param objectList : Column#value に引き渡すデータのIterable
     */
    public void outputAllValueToWriter(Writer writer,
				       Iterable<T> objectList,
                                       Object... auxObjects) {
	for(T object : objectList) {
	    outputValueToWriter(writer, object, auxObjects) ;
	}
    }

    //------------------------------------------------------------
    /**
     * データ行を全て出力。
     * @param stream : 出力する Stream
     * @param objectList : Column#value に引き渡すデータのIterable
     */
    public void outputAllValueToStream(PrintStream stream,
				       Iterable<T> objectList,
                                       Object... auxObjects) {
	for(T object : objectList) {
	    outputValueToStream(stream, object, auxObjects) ;
	}
    }

    //============================================================
    //============================================================
    public class Column {
	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * Column のヘッダ
	 */
	public String header ;

	//----------------------------------------
	/**
	 * コンストラクタ
	 */
	public Column(String _header) {
	    header = _header ;
	}

	//----------------------------------------
	/**
	 * 値の出力
	 */
	public String value(T object) {
	    Itk.logError("Column#value(object) is not implemented.") ;
	    return "" ;
	}

	//----------------------------------------
	/**
	 * 値の出力(2入力)
	 */
	public String value(T object1, Object object2) {
	    Itk.logError("Column#value(obj1, obj2) is not implemented.") ;
	    return "" ;
	}

	//----------------------------------------
	/**
	 * 値の出力(3入力)
	 */
	public String value(T object1, Object object2, Object object3) {
	    Itk.logError("Column#value(obj1, obj2, obj3) is not implemented.") ;
	    return "" ;
	}
    }

} // class Foo

