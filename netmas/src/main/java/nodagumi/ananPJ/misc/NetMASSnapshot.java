// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.util.ArrayList;
import java.util.HashMap;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;


public class NetMASSnapshot {

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

