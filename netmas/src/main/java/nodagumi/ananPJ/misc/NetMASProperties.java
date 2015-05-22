// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.util.Properties;

public class NetMASProperties {

    public static String getProperty(Properties prop, String key) {
	//String propertiesString = new String();
	if (prop.containsKey(key)) {
	    //propertiesString = prop.getProperty(key);
	    //return propertiesString;
	    return prop.getProperty(key);
	} else {
	    return null;
	}
    }
}
