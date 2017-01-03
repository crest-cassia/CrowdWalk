package com.opencsv;
/*
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.text.StrBuilder;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for processing JDBC ResultSet objects.
 */
public class ResultSetHelperService implements ResultSetHelper {
   protected static final int CLOBBUFFERSIZE = 2048;

   static final String DEFAULT_DATE_FORMAT = "dd-MMM-yyyy";
   static final String DEFAULT_TIMESTAMP_FORMAT = "dd-MMM-yyyy HH:mm:ss";

   private String dateFormat = DEFAULT_DATE_FORMAT;
   private String dateTimeFormat = DEFAULT_TIMESTAMP_FORMAT;

   /**
    * Default constructor.
    */
   public ResultSetHelperService() {
   }

   /**
    * Set a default date format pattern that will be used by the service.
    *
    * @param dateFormat Desired date format
    */
   public void setDateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
   }

   /**
    * Set a default date time format pattern that will be used by the service.
    *
    * @param dateTimeFormat Desired date time format
    */
   public void setDateTimeFormat(String dateTimeFormat) {
      this.dateTimeFormat = dateTimeFormat;
   }

   @Override
   public String[] getColumnNames(ResultSet rs) throws SQLException {
      List<String> names = new ArrayList<String>();
      ResultSetMetaData metadata = rs.getMetaData();

      for (int i = 1; i <= metadata.getColumnCount(); i++) {
         names.add(metadata.getColumnLabel(i));
      }

      String[] nameArray = new String[names.size()];
      return names.toArray(nameArray);
   }

   @Override
   public String[] getColumnValues(ResultSet rs) throws SQLException, IOException {
      return this.getColumnValues(rs, false, dateFormat, dateTimeFormat);
   }

   @Override
   public String[] getColumnValues(ResultSet rs, boolean trim) throws SQLException, IOException {
      return this.getColumnValues(rs, trim, dateFormat, dateTimeFormat);
   }

   @Override
   public String[] getColumnValues(ResultSet rs, boolean trim, String dateFormatString, String timeFormatString) throws SQLException, IOException {
      List<String> values = new ArrayList<String>();
      ResultSetMetaData metadata = rs.getMetaData();

      for (int i = 1; i <= metadata.getColumnCount(); i++) {
         values.add(getColumnValue(rs, metadata.getColumnType(i), i, trim, dateFormatString, timeFormatString));
      }

      String[] valueArray = new String[values.size()];
      return values.toArray(valueArray);
   }

   /**
    * The formatted timestamp.
    * @param timestamp Timestamp read from resultset
    * @param timestampFormatString Format string
    * @return Formatted time stamp.
    */
   protected String handleTimestamp(Timestamp timestamp, String timestampFormatString) {
      SimpleDateFormat timeFormat = new SimpleDateFormat(timestampFormatString);
      return timestamp == null ? null : timeFormat.format(timestamp);
   }

   private String getColumnValue(ResultSet rs, int colType, int colIndex, boolean trim, String dateFormatString, String timestampFormatString)
         throws SQLException, IOException {

      String value = "";

      switch (colType) {
         case Types.BIT:
         case Types.JAVA_OBJECT:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getObject(colIndex), "");
            value = ObjectUtils.toString(rs.getObject(colIndex), "");
            break;
         case Types.BOOLEAN:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getBoolean(colIndex));
            value = ObjectUtils.toString(rs.getBoolean(colIndex));
            break;
         case Types.NCLOB: // todo : use rs.getNClob
         case Types.CLOB:
            Clob c = rs.getClob(colIndex);
            if (c != null) {
               StrBuilder sb = new StrBuilder();
               sb.readFrom(c.getCharacterStream());
               value = sb.toString();
            }
            break;
         case Types.BIGINT:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getLong(colIndex));
            value = ObjectUtils.toString(rs.getLong(colIndex));
            break;
         case Types.DECIMAL:
         case Types.REAL:
         case Types.NUMERIC:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getBigDecimal(colIndex), "");
            value = ObjectUtils.toString(rs.getBigDecimal(colIndex), "");
            break;
         case Types.DOUBLE:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getDouble(colIndex));
            value = ObjectUtils.toString(rs.getDouble(colIndex));
            break;
         case Types.FLOAT:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getFloat(colIndex));
            value = ObjectUtils.toString(rs.getFloat(colIndex));
            break;
         case Types.INTEGER:
         case Types.TINYINT:
         case Types.SMALLINT:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getInt(colIndex));
            value = ObjectUtils.toString(rs.getInt(colIndex));
            break;
         case Types.DATE:
            java.sql.Date date = rs.getDate(colIndex);
            if (date != null) {
               SimpleDateFormat df = new SimpleDateFormat(dateFormatString);
               value = df.format(date);
            }
            break;
         case Types.TIME:
// Once Java 7 is the minimum supported version.
//            value = Objects.toString(rs.getTime(colIndex), "");
            value = ObjectUtils.toString(rs.getTime(colIndex), "");
            break;
         case Types.TIMESTAMP:
            value = handleTimestamp(rs.getTimestamp(colIndex), timestampFormatString);
            break;
         case Types.NVARCHAR: // todo : use rs.getNString
         case Types.NCHAR: // todo : use rs.getNString
         case Types.LONGNVARCHAR: // todo : use rs.getNString
         case Types.LONGVARCHAR:
         case Types.VARCHAR:
         case Types.CHAR:
            String columnValue = rs.getString(colIndex);
            if (trim && columnValue != null) {
               value = columnValue.trim();
            } else {
               value = columnValue;
            }
            break;
         default:
            value = "";
      }


      if (rs.wasNull() || value == null) {
         value = "";
      }

      return value;
   }
}
