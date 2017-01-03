package com.opencsv;

/*
 * Copyright 2015 Scott Conway
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Helper class for processing JDBC ResultSet objects allowing the user to
 * process a subset of columns and set custom header names.
 */
public class ResultSetColumnNameHelperService extends ResultSetHelperService implements ResultSetHelper {
    private String[] columnNames;
    private String[] columnHeaders;
    private final Map<String, Integer> columnNamePositionMap = new HashMap<String, Integer>();

    public ResultSetColumnNameHelperService() {
    }

    /**
     * Set the JDBC column names to use, and the header text for the CSV file
     * @param columnNames The JDBC column names to export, in the desired order
     * @param columnHeaders The column headers of the CSV file, in the desired order
     * @throws UnsupportedOperationException If the number of headers is different
     * than the number of columns, or if any of the columns or headers is blank
     * or null.
     */
    public void setColumnNames(String[] columnNames, String[] columnHeaders) {
        if (columnHeaders.length != columnNames.length) {
            throw new UnsupportedOperationException("The number of column names must be the same as the number of header names.");
        }
        if (hasInvalidValue(columnNames)) {
            throw new UnsupportedOperationException("Column names cannot be null, empty, or blank");
        }
        if (hasInvalidValue(columnHeaders)) {
            throw new UnsupportedOperationException("Column header names cannot be null, empty, or blank");
        }
        this.columnNames = Arrays.copyOf(columnNames, columnNames.length);
        this.columnHeaders = Arrays.copyOf(columnHeaders, columnHeaders.length);
    }

    private boolean hasInvalidValue(String[] strings) {
        for (String s : strings) {
            if (StringUtils.isBlank(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the column names from the result set.
     * @param rs ResultSet
     * @return A string array containing the column names.
     * @throws SQLException Thrown by the result set.
     */
    @Override
    public String[] getColumnNames(ResultSet rs) throws SQLException {
        if (columnNamePositionMap.isEmpty()) {
            populateColumnData(rs);
        }
        return Arrays.copyOf(columnHeaders, columnHeaders.length);
    }

    private void populateColumnData(ResultSet rs) throws SQLException {
        String[] realColumnNames = super.getColumnNames(rs);

        if (columnNames == null) {
            columnNames = Arrays.copyOf(realColumnNames, realColumnNames.length);
            columnHeaders = Arrays.copyOf(realColumnNames, realColumnNames.length);
        }

        for (String name : columnNames) {
            int position = ArrayUtils.indexOf(realColumnNames, name);
            if (position == ArrayUtils.INDEX_NOT_FOUND) {
                throw new UnsupportedOperationException("The column named " + name + " does not exist in the result set!");
            }
            columnNamePositionMap.put(name, position);
        }
    }

    /**
     * Get all the column values from the result set.
     * @param rs The ResultSet containing the values.
     * @return String array containing all the column values.
     * @throws SQLException Thrown by the result set.
     * @throws IOException Thrown by the result set.
     */
    @Override
    public String[] getColumnValues(ResultSet rs) throws SQLException, IOException {
        if (columnNamePositionMap.isEmpty()) {
            populateColumnData(rs);
        }
        String[] realColumnValues = super.getColumnValues(rs, false, DEFAULT_DATE_FORMAT, DEFAULT_TIMESTAMP_FORMAT);
        return getColumnValueSubset(realColumnValues);
    }

    /**
     * Get all the column values from the result set.
     * @param rs The ResultSet containing the values.
     * @param trim Values should have white spaces trimmed.
     * @return String array containing all the column values.
     * @throws SQLException Thrown by the result set.
     * @throws IOException Thrown by the result set.
     */
    @Override
    public String[] getColumnValues(ResultSet rs, boolean trim) throws SQLException, IOException {
        if (columnNamePositionMap.isEmpty()) {
            populateColumnData(rs);
        }
        String[] realColumnValues = super.getColumnValues(rs, trim, DEFAULT_DATE_FORMAT, DEFAULT_TIMESTAMP_FORMAT);
        return getColumnValueSubset(realColumnValues);
    }

    /**
     * Get all the column values from the result set.
     * @param rs The ResultSet containing the values.
     * @param trim Values should have white spaces trimmed.
     * @param dateFormatString Format string for dates.
     * @param timeFormatString Format string for timestamps.
     * @return String array containing all the column values.
     * @throws SQLException Thrown by the result set.
     * @throws IOException Thrown by the result set.
     */
    @Override
    public String[] getColumnValues(ResultSet rs, boolean trim, String dateFormatString, String timeFormatString) throws SQLException, IOException {
        if (columnNamePositionMap.isEmpty()) {
            populateColumnData(rs);
        }
        String[] realColumnValues = super.getColumnValues(rs, trim, dateFormatString, timeFormatString);
        return getColumnValueSubset(realColumnValues);
    }

    private String[] getColumnValueSubset(String[] realColumnValues) {
        List<String> valueList = new ArrayList<String>(realColumnValues.length);
        for (String columnName : columnNames) {
            valueList.add(realColumnValues[columnNamePositionMap.get(columnName)]);
        }
        return valueList.toArray(new String[columnNames.length]);
    }
}
