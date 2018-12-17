package nodagumi.ananPJ.Editor;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.misc.JsonicHashMapGetter;

/**
 * MAPPLE 道路ネットワークデータファイルの読み取りをおこなう
 */
public class MRDReader {
    public static final String FORMAT_FILE = "/MRD_format.json";
    public static final int RECORD_LENGTH = 256;

    private RandomAccessFile reader;
    private int position = -1;
    private int recordSize = 0;
    private String recordBuffer;
    private String recordBufferSJ;
    private HashMap<String, MrdFieldFormat> recordFormat;
    private Pattern hexPattern = Pattern.compile("[A-Fa-f]");

    private static HashMap<Integer, HashMap<String, MrdFieldFormat>> recordFormats = new HashMap();

    public class MrdFieldFormat {
        public String name;
        public String type;
        public int digits;
        public int offset;

        public MrdFieldFormat(String name, String type, int digits, int offset) {
            this.name = name;
            this.type = type;
            this.digits = digits;
            this.offset = offset;
        }
    }

    public MRDReader(File file) throws FileNotFoundException, IOException {
        if (recordFormats.isEmpty()) {
            formatSetting();
        }
        reader = new RandomAccessFile(file, "r");
        recordSize = (int)reader.length() / RECORD_LENGTH;
    }

    public MRDReader(String path) throws FileNotFoundException, IOException {
        this(new File(path));
    }

    private void formatSetting() throws IOException {
        JsonicHashMapGetter formatMap = new JsonicHashMapGetter();
        JSON json = new JSON(JSON.Mode.TRADITIONAL);

        ArrayList<HashMap> formats = json.parse(MRDReader.class.getResourceAsStream(FORMAT_FILE));
        for (HashMap format : formats) {
            HashMap<String, MrdFieldFormat> fieldFormats = new HashMap();
            formatMap.setParameters(format);
            try {
                int recordID = formatMap.getIntParameter("recordID", -1);
                ArrayList<HashMap> fields = (ArrayList<HashMap>)formatMap.getArrayListParameter("fields", null);
                for (HashMap field : fields) {
                    formatMap.setParameters(field);
                    String name = formatMap.getStringParameter("項目名", null);
                    String type = formatMap.getStringParameter("データ形式", null);
                    int digits = formatMap.getIntParameter("桁数", 0);
                    int cumulativeDigits = formatMap.getIntParameter("累積桁数", 0);
                    fieldFormats.put(name, new MrdFieldFormat(name, type, digits, cumulativeDigits - digits));
                }
                recordFormats.put(new Integer(recordID), fieldFormats);
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    public void close() throws IOException {
        reader.close();
    }

    public int getInt(String recordBuffer, String name) throws IOException {
        int radix = 10;
        String field = getString(recordBuffer, name);

        // a～fが含まれていたら16進数として扱う
        Matcher matcher = hexPattern.matcher(field);
        if (matcher.find()) {
            radix = 16;
        }

        return Integer.parseInt(field, radix);
    }

    public int getInt(String name) throws IOException {
        int radix = 10;
        String field = getString(name);

        // a～fが含まれていたら16進数として扱う
        Matcher matcher = hexPattern.matcher(field);
        if (matcher.find()) {
            radix = 16;
        }

        return Integer.parseInt(field, radix);
    }

    public Integer getInteger(String name) throws IOException {
        return new Integer(getInt(name));
    }

    public String getString(String recordBuffer, String name) throws IOException {
        HashMap<String, MrdFieldFormat> recordFormat = getRecordFormat(recordBuffer);
        MrdFieldFormat fieldFormat = recordFormat.get(name);
        if (fieldFormat == null) {
            throw new IOException("Invalid field name: " + name);
        }
        return recordBuffer.substring(fieldFormat.offset, fieldFormat.offset + fieldFormat.digits);
    }

    public String getString(String name) throws IOException {
        if (position == -1) {
            throw new IOException("Read value before accessing: " + name);
        }
        MrdFieldFormat fieldFormat = recordFormat.get(name);
        if (fieldFormat == null) {
            String recordID = recordBuffer.substring(0, 2);
            throw new IOException(recordID + ": Invalid field name: " + name);
        }
        return recordBuffer.substring(fieldFormat.offset, fieldFormat.offset + fieldFormat.digits);
    }

    public String getRecordBuffer() {
        return recordBuffer;
    }

    public String getRecordBufferSJ() {
        return recordBufferSJ;
    }

    public int getRecordSize() {
        return recordSize;
    }

    public int getRecordPosition() {
        return position;
    }

    public HashMap<String, MrdFieldFormat> getRecordFormat(String recordBuffer) throws IOException {
        Integer recordID = new Integer(recordBuffer.substring(0, 2));
        HashMap<String, MrdFieldFormat> recordFormat = recordFormats.get(recordID);
        if (recordFormat == null) {
            throw new IOException("Invalid ID record: " + recordBuffer);
        }
        return recordFormat;
    }

    public void setRecordPosition(int position) throws IOException {
        if (position < 0 || position >= recordSize) {
            throw new IOException("Invalid seek position.");
        }
        byte[] buffer = new byte[RECORD_LENGTH];
        reader.seek((long)position * RECORD_LENGTH);
        this.position = position;
        reader.readFully(buffer);

        recordBuffer = new String(buffer, "ISO-8859-1");
        recordBufferSJ = new String(buffer, "Shift_JIS");
        recordFormat = getRecordFormat(recordBuffer);
    }
}
