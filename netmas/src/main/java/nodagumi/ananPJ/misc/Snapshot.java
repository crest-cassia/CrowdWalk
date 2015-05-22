// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.awt.geom.Point2D;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkParts.Node.MapNode;

/**
 * Snapshot class
 *  store:
 *      Gather all of local variables in the class and return them as String.
 *      
 *  restore:
 *      Restore the class with String which is created by store method.
 */
public class Snapshot {

    public static Element toElement(Document doc, String tag, int value) {
        Element element = doc.createElement(tag);
        element.setAttribute("class", "int");
        Text valueText = doc.createTextNode("" + value);
        element.appendChild(valueText);

        return element;
    }

    public static Element toElement(Document doc, String tag, double value) {
        Element element = doc.createElement(tag);
        element.setAttribute("class", "double");
        Text valueText = doc.createTextNode("" + value);
        element.appendChild(valueText);

        return element;
    }

    public static Element toElement(Document doc, String tag, String value) {
        Element element = doc.createElement(tag);
        element.setAttribute("class", "String");
        Text valueText = doc.createTextNode("" + value);
        element.appendChild(valueText);

        return element;
    }

    public static Element toElement(Document doc, String tag, Point2D value) {
        Element element = doc.createElement(tag);
        element.setAttribute("class", "Point2D");

        element.appendChild(toElement(doc, "x", value.getX()));
        element.appendChild(toElement(doc, "y", value.getY()));

        return element;
    }


    // store method
    public static String SnapshotStore(Object obj) {
        String storeString = new String();
        return storeString;
    }

    // store method in DOM
    //  at first, search fields in obj and if the field is primitive type, 
    //  store the field name and value. if the field is non-primitive type, 
    //  call SnapshotStoreDom recursively.
    public static Element SnapshotStoreDomInit(Document dom, Object obj) {
        Element element = dom.createElement("SnapshotDom");
        return element;
    }

    public static Element SnapshotStoreDom(Document dom, Object obj) {
        Element element = dom.createElement("SnapshotDom");
        final Class classIn = obj.getClass();
        final Field[] fields = classIn.getDeclaredFields();
        String[] fieldList = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];
            /*
            final Class classField = (field.get()).getClass();
            if (classField.isPrimitive()) {
            } else {
                SnapshotStoreDom(dom, field.get());
            }
            // fieldList[i] = field.toString();
            */
        }
        
        return element;
    }

    public static String[] class2FieldList(Object obj) {
        return class2FieldList(obj, false);
    }

    public static String[] class2FieldList(Object obj, boolean isDebug) {
        final Class classIn = obj.getClass();
        final Field[] fields = classIn.getDeclaredFields();
        String[] fieldList = new String[fields.length];
        if (isDebug)
            System.out.print("Snapshot.class2FieldList: field list...\n");
        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];
            if (isDebug)
                System.out.println("\t" + field.toString() + ":" + field.getType());
            fieldList[i] = field.toString();
        }
        if (isDebug)
            System.out.print("\n");

        return fieldList;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> string2Class(String name) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            return null;
        }
    }

/******************************************************************************
 *                          ungeneric
 */
    public static String dump2String(String obj, boolean isDebug) {
        return dumpPrimitive2String((Object) obj, isDebug);
    }

    public static String dump2String(double obj, boolean isDebug) {
        return dumpPrimitive2String((Object) obj, isDebug);
    }

    public static String dump2String(int obj, boolean isDebug) {
        return dumpPrimitive2String((Object) obj, isDebug);
    }

    public static String dump2String(ArrayList<Object> arrayObj,
            boolean isDebug) {
        return dumpArrayList2String(arrayObj, isDebug);
    }

    public static String dumpPrimitive2String(Object obj, boolean isDebug) {
        String dumpString = new String();

        if (obj != null) {
            if (isDebug)
                System.out.println("\t" + obj);
            dumpString += obj + ",";
        } else {
            if (isDebug)
                System.out.println("\tnull");
            dumpString += "null,";
        }

        return dumpString;
    }

    public static String dumpArrayList2String(ArrayList<Object> arrayObj,
            boolean isDebug) {
        String dumpString = new String();

        if (arrayObj != null) {
            if (isDebug)
                System.out.print("\t");
            for (Object obj : arrayObj) {
                if (isDebug)
                    System.out.print(obj + "-");
                dumpString += obj + "-";
            }
            if (isDebug)
                System.out.println();
            dumpString += ",";
        } else {
            if (isDebug)
                System.out.println("\tnull");
            dumpString += "null,";
        }

        return dumpString;
    }

    public static String dumpHashMap2String(HashMap<Object, Object> hashObj,
            boolean isDebug) {
        String dumpString = new String();

        if (hashObj != null) {
            if (isDebug)
            for (Object obj : hashObj.keySet().toArray()) {
                if (isDebug)
                    System.out.print(obj + "_" + hashObj.get(obj));
                dumpString += obj + "_" + hashObj.get(obj) + "-";
            }
            if (isDebug)
                System.out.println();
            dumpString += ",";
        } else {
            if (isDebug)
                System.out.println("\tnull");
            dumpString += "null,";
        }
        return dumpString;
    }
}

