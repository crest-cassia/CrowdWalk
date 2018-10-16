// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;

import nodagumi.Itk.*;

public class Settings extends TreeMap<String, String> {
	String settingsFilename = "settings.ini";
	static Settings settings = null;	

	private Settings () {
		super();
	}
	
	public static Settings load(String s) {
		if (settings != null) {
			if(!settings.settingsFilename.equals(s)) {
				Itk.logWarn("Settings is alread loaded from:",
							settings.settingsFilename) ;
				Itk.logWarn_("ignored file",s) ;
			}
			return settings;
		}
		settings = new Settings();

		settings.settingsFilename = s;
		try {
			FileReader fr = new FileReader (settings.settingsFilename);
			BufferedReader br = new BufferedReader (fr);
			
			String line = null;
			while ((line = br.readLine()) != null) {
				String elems[] = line.split("=");
				if (elems.length == 2) {
					settings.put (elems[0], elems[1]);
				}
			}
			
			br.close ();
			fr.close ();
		} catch (FileNotFoundException e) {
			/* No setting file, not a problem */
		} catch (IOException e) {
            Itk.dumpStackTraceOf(e);
		}
		return settings;
	}
	
	public static void save () {
                Itk.logInfo("Save Settings File", settings.settingsFilename) ;
		try {
			FileWriter fw = new FileWriter (settings.settingsFilename);
			BufferedWriter bw = new BufferedWriter (fw);
			
			for (String key : settings.keySet()) {
				bw.write(key + "=" + settings.get(key) + "\n");
			}
			
			bw.close ();
			fw.close ();
		} catch (IOException e) {
            Itk.dumpStackTraceOf(e);
		}
	}
	
	public String get (String key, String defval) {
		String val = get (key);
		if (val == null) {
			val = defval;
			this.put(key, val);
		}
		return val;
	}

	public int get (String key, int defval) {
		String val = get (key);
		if (val == null) {
			val = "" + defval;
			this.put(key, val);
		}
		return Integer.parseInt(val);
	}
	
	public void put (String key, int val) {
		put (key, "" + val);
	}

	public static Settings getSettings() {
		return settings;
	}
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
