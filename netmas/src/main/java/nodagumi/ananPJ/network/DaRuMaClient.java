package nodagumi.ananPJ.network;

// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.io.InputStream;
// import java.io.ObjectInputStream;
// import java.io.ObjectOutputStream;
// import java.io.OutputStream;
// import java.io.PrintStream;
// import java.io.Serializable;
import java.io.*;

// import java.net.Socket;
// import java.net.UnknownHostException;
import java.net.*;

// import javax.xml.parsers.DocumentBuilder;
// import javax.xml.parsers.DocumentBuilderFactory;
// import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.*;
// import javax.xml.transform.OutputKeys;
// import javax.xml.transform.Transformer;
// import javax.xml.transform.TransformerConfigurationException;
// import javax.xml.transform.TransformerException;
// import javax.xml.transform.TransformerFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

//import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.OutputPropertiesFactory;


public class DaRuMaClient implements Serializable {

    private String host = "localhost";
    private int port = 5050;
    transient DocumentBuilder builder = null;
    transient Transformer transformer = null;

    public DaRuMaClient() {
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
            transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT,
                                          "2");
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void setConnectionParams(String _host, int _port) {
        host = _host;
        port = _port;
    }

    /* some utility methods, not necesseraly related to DaRuMa,
     * but more in xml manipulation
     */
    public Document newDocument() {
        return builder.newDocument();
    }
    
    public String docToString(Document doc) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        if (false == docToStream(doc, outStream)) return null;

        return outStream.toString();
    }

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

    /* DaRuMa related methods
     */
    public Document sendRecv(Document doc) {
        /* open socket */
        Socket sock = null;
        try {
            sock = new Socket(host, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        /* send  */
        DOMSource source = new DOMSource(doc);
        StreamResult result;

        try {
            result = new StreamResult(sock.getOutputStream());
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (TransformerException e) {
            e.printStackTrace();
            return null;
        }

        /* receive */
        try {
            return builder.parse(sock.getInputStream());
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Document getCapabilities() {
        Document message = newDocument();
        Element root = message.createElement("GetCapabilities");
        root.setAttribute("xmlns", "http://www.infosharp.org/misp");
                message.appendChild(root);

        Document capabilities = sendRecv(message);
        return capabilities;
    }

    public Document query(String namespace,
                          String typeName,
                          String title) {
        Document message = newDocument();
        Element root = message.createElement("misp:GetFeature");
        root.setAttribute("xmlns:misp", "http://www.infosharp.org/misp");
        root.setAttribute("xmlns", namespace);
        message.appendChild(root);

        Element query = message.createElement("misp:Query");
        query.setAttribute("typeName", typeName);
        root.appendChild(query);
        Element filter = message.createElement("misp:Filter");
        query.appendChild(filter);
        Element filterEntry = message.createElement("misp:PropertyIsEqualTo");
        filter.appendChild(filterEntry);
        Element propertyName = message.createElement("misp:PropertyName");
        propertyName.appendChild(message.createTextNode("title"));
        filterEntry.appendChild(propertyName);
        Element literalName = message.createElement("misp:Literal");
        literalName.appendChild(message.createTextNode(title));
        filterEntry.appendChild(literalName);

        Document result = sendRecv(message);
        return result;
    }

    public Document insert(String namespace,
                           String typeName,
                           String title,
                           Document message) {
        Node dataRoot = message.getFirstChild();
        message.removeChild(dataRoot);
        Element root = message.createElement("misp:Transaction");
        root.setAttribute("xmlns:misp", "http://www.infosharp.org/misp");
        message.appendChild(root);
        Element insert = message.createElement("misp:Insert");
        insert.setAttribute("xmlns", namespace);
        root.appendChild(insert);
        insert.appendChild(dataRoot);
        
        Document result = sendRecv(message);
        return result;
    }

    public Document addSchemeToDaruma(String filename) {
        try {
            Document xsd = builder.parse(filename);
            return sendRecv(xsd);
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* A singleton.
     */
    static private DaRuMaClient instance = null;
    public static DaRuMaClient getInstance() {
        if (instance == null) {
            instance = new DaRuMaClient();
        }
        
        return instance;
    }
    
    private static void parseAndPrintRecursive(PrintStream out,
            Node node, int depth) {
        if (node == null) {
            return;
        }
        
        for (int i = 0; i < depth; ++i) {out.print(" ");}
        out.print(node.getNodeName() + "\t" + node.getNodeValue());
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            parseAndPrintRecursive(out, nodes.item(i), depth + 1);
        }
    }

    /* Test main class.
     */
    public static void main(String[] args) {
        DaRuMaClient client = DaRuMaClient.getInstance();
        Document result = client.getCapabilities();
        System.out.println(client.docToString(result));
        /* <GetCapabilities xmlns="http://www.infosharp.org/misp"/>
         */
        parseAndPrintRecursive(System.out,
                               //client.getCapabilities(), 0);
                               client.query("http://staff.aist.go.jp/shunsuke.soeda/nodagumi/ananPJ/Network",
                                            "network", "Test network"), 0);
    }
}
