package com.opencsv.enums;

/**
 * Enumeration used to tell the CSVParser what to consider null.
 * <ul>
 * <li>EMPTY_SEPARATORS - two sequential separators are null.</li>
 * <li>EMPTY_QUOTES - two sequential quotes are null</li>
 * <li>BOTH - both are null</li>
 * <li>NEITHER - default.  Both are considered empty string.</li>
 * </ul>
 */
public enum CSVReaderNullFieldIndicator {
    EMPTY_SEPARATORS,
    EMPTY_QUOTES,
    BOTH,
    NEITHER;
}
