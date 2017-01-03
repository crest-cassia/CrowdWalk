package com.opencsv.stream.reader;

import com.opencsv.CSVParser;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class was created for
 * <a href="https://sourceforge.net/p/opencsv/bugs/106/">issue #106</a>
 * where carriage returns were being removed.
 * This class allows the user to determine if they wish to keep or
 * remove them from the data being read.
 *
 * @author scott
 * @since 2/19/15
 */

public class LineReader {
    private final BufferedReader reader;
    private final boolean keepCarriageReturns;

    /**
     * LineReader constructor.
     *
     * @param reader              Reader that data will be read from.
     * @param keepCarriageReturns True if carriage returns should remain in the data, false to remove them.
     */
    public LineReader(BufferedReader reader, boolean keepCarriageReturns) {
        this.reader = reader;
        this.keepCarriageReturns = keepCarriageReturns;
    }

    /**
     * Reads the next line from the Reader.
     *
     * @return Line read from reader.
     * @throws IOException On error from BufferedReader
     */
    public String readLine() throws IOException {
        return keepCarriageReturns ? readUntilNewline() : reader.readLine();
    }

    private String readUntilNewline() throws IOException {
        StringBuilder sb = new StringBuilder(CSVParser.INITIAL_READ_SIZE);
        for (int c = reader.read(); c > -1 && c != '\n'; c = reader.read()) {
            sb.append((char) c);
        }

        return sb.length() > 0 ? sb.toString() : null;
    }
}
