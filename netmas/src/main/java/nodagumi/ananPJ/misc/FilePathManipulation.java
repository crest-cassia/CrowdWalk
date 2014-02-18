package nodagumi.ananPJ.misc;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

public class FilePathManipulation implements Serializable {
	public static String getRelativePath(String base_str,
			String file_str) {
		if (base_str.equals(file_str)) return file_str;
		if (!(new File(file_str)).isFile()) return file_str;

		File base = new File(base_str);
		/* we want to compare directory names, so we remove the filename */
		if (!base.isDirectory()) base = base.getParentFile();

		int depth_to_common = 0;
		while (base != null) {
			LinkedList<String> relpath = new LinkedList<String>();
			File file = new File(file_str);

			/* we want to compare directory names, so we remove the filename */
			String filename = file.getName();
			file = file.getParentFile();
			while (file != null) {
				if (base.equals(file)) {
					StringBuffer relpathbuf = new StringBuffer();
					
					if (depth_to_common != 0) {
						for (int i = 0; i < depth_to_common; ++i) {
							relpathbuf.append(".." + File.separator);
						}
					}
					while (!relpath.isEmpty()) {
						relpathbuf.append(relpath.pop() + File.separator);
					}
					relpathbuf.append(filename);
					return relpathbuf.toString();
				}

				relpath.push(file.getName());
				file = file.getParentFile();
			}
			base = base.getParentFile();
			depth_to_common++;
		}
		/* No common path; returning absolute path */
		return file_str;		
	}
	
	public static String setAbsolutePath(String base_str,
			String file_str) {
        // tkokada
        if (file_str == null)
            file_str = "/tmp/test";
		File file = new File(file_str);
		if (file.isAbsolute()) return file_str;
		
		return base_str + File.separator + file_str;
	}

	static String getBase(String path) {
		File file = new File(path);
		return file.getParent();
	}
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
