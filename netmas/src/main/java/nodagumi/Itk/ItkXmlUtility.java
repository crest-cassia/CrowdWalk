// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk Xml Utility
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/04/19 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/04/19]: Create This File. Mainly from DaRuMaClient.java</LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.apache.xml.serializer.OutputPropertiesFactory;

//======================================================================
/**
 * XML をハンドルするのに必要なツール群
 */
public class ItkXmlUtility {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 代表インスタンス。
     */
    public static ItkXmlUtility singleton = new ItkXmlUtility() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     *
     */
    transient DocumentBuilder builder = null;

    /**
     *
     */
    transient Transformer transformer = null;

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     */
    public ItkXmlUtility() {
	init() ;
    }

    //------------------------------------------------------------
    /**
     * 初期化。
     */
    public void init() {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setNamespaceAware(true);
	try {
	    builder = factory.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    e.printStackTrace();
	}
	TransformerFactory tFactory =
	    TransformerFactory.newInstance();
	try {
	    transformer = tFactory.newTransformer();
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	    transformer.setOutputProperty(OutputPropertiesFactory
					  .S_KEY_INDENT_AMOUNT,
					  "2");
	} catch (TransformerConfigurationException e) {
	    e.printStackTrace();
	}
    }

    //------------------------------------------------------------
    /**
     * ドキュメントインスタンスの作成。
     */
    public Document newDocument() {
	return builder.newDocument();
    }

    //------------------------------------------------------------
    /**
     * XMLドキュメント構造体から文字列へ
     */
    public String docToString(Document doc) {
	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	if (false == docToStream(doc, outStream)) return null;

	return outStream.toString();
    }

    //------------------------------------------------------------
    /**
     * XMLドキュメント構造体をStreamへ出力
     */
    public boolean docToStream(Document doc, OutputStream stream) {
	DOMSource source = new DOMSource(doc);
	StreamResult result = new StreamResult(stream);
	try {
	    transformer.transform(source, result);
	} catch (TransformerException e) {
	    e.printStackTrace();
	    return false;
	}
	return true;
    }

    //------------------------------------------------------------
    /**
     * 文字列を解釈してXMLドキュメントへ。
     */
    public Document stringToDoc(String string) {
	try {
	    return builder.parse(string);
	} catch (SAXException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

    //------------------------------------------------------------
    /**
     * Streamから読み込んで、解釈してXMLドキュメントへ。
     */
    public Document streamToDoc(InputStream is) {
	try {
	    return builder.parse(is);
	} catch (SAXException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class Foo

