package com.opencsv.bean;

/*
 Copyright 2007 Kyle Miller.

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

import com.opencsv.CSVReader;
import com.opencsv.exceptions.*;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Converts CSV data to objects.
 *
 * @param <T> Class to convert the objects to.
 */
public class CsvToBean<T> extends AbstractCSVToBean {
   private Map<Class<?>, PropertyEditor> editorMap = null;
   private List<CsvException> capturedExceptions = null;

   /**
    * Default constructor.
    */
   public CsvToBean() {
   }

   /**
    * Parse the values from a CSVReader constructed from the Reader passed in.
    * @param mapper Mapping strategy for the bean.
    * @param reader Reader used to construct a CSVReader
    * @return List of Objects.
    */

   public List<T> parse(MappingStrategy<T> mapper, Reader reader) {
      return parse(mapper, new CSVReader(reader), null, true);
   }

   /**
    * Parse the values from a CSVReader constructed from the Reader passed in.
    * @param mapper Mapping strategy for the bean.
    * @param reader Reader used to construct a CSVReader
    * @param throwExceptions If false, exceptions internal to opencsv will not
    *   be thrown, but can be accessed after processing is finished through
    *   {@link #getCapturedExceptions()}.
    * @return List of Objects.
    */

   public List<T> parse(MappingStrategy<T> mapper, Reader reader, boolean throwExceptions) {
      return parse(mapper, new CSVReader(reader), null, throwExceptions);
   }

   /**
    * Parse the values from a CSVReader constructed from the Reader passed in.
    *
    * @param mapper Mapping strategy for the bean.
    * @param reader Reader used to construct a CSVReader
    * @param filter CsvToBeanFilter to apply - null if no filter.
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, Reader reader, CsvToBeanFilter filter) {
      return parse(mapper, new CSVReader(reader), filter, true);
   }

   /**
    * Parse the values from a CSVReader constructed from the Reader passed in.
    * @param mapper Mapping strategy for the bean.
    * @param reader Reader used to construct a CSVReader
    * @param filter CsvToBeanFilter to apply - null if no filter.
    * @param throwExceptions If false, exceptions internal to opencsv will not
    *   be thrown, but can be accessed after processing is finished through
    *   {@link #getCapturedExceptions()}.
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, Reader reader,
                        CsvToBeanFilter filter, boolean throwExceptions) {
      return parse(mapper, new CSVReader(reader), filter, throwExceptions);
   }

   /**
    * Parse the values from the CSVReader.
    * @param mapper Mapping strategy for the bean.
    * @param csv CSVReader
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, CSVReader csv) {
      return parse(mapper, csv, null, true);
   }

   /**
    * Parse the values from the CSVReader.
    * @param mapper Mapping strategy for the bean.
    * @param csv CSVReader
    * @param throwExceptions If false, exceptions internal to opencsv will not
    *   be thrown, but can be accessed after processing is finished through
    *   {@link #getCapturedExceptions()}.
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, CSVReader csv, boolean throwExceptions) {
      return parse(mapper, csv, null, throwExceptions);
   }

   /**
    * Parse the values from the CSVReader.
    * Throws exceptions for bad data and other sorts of problems relating
    * directly to opencsv, as well as general exceptions from external code
    * used.
    *
    * @param mapper Mapping strategy for the bean.
    * @param csv    CSVReader
    * @param filter CsvToBeanFilter to apply - null if no filter.
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, CSVReader csv,
                        CsvToBeanFilter filter) {
      return parse(mapper, csv, filter, true);
   }

   /**
    * Parse the values from the CSVReader.
    * Only throws general exceptions from external code used. Problems related
    * to opencsv and the data provided to it are captured for later processing
    * by user code and can be accessed through {@link #getCapturedExceptions()}.
    *
    * @param mapper          Mapping strategy for the bean.
    * @param csv             CSVReader
    * @param filter          CsvToBeanFilter to apply - null if no filter.
    * @param throwExceptions If false, exceptions internal to opencsv will not
    *                        be thrown, but can be accessed after processing is finished through
    *                        {@link #getCapturedExceptions()}.
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, CSVReader csv,
                        CsvToBeanFilter filter, boolean throwExceptions) {
      long lineProcessed = 0;
      String[] line = null;

      try {
         mapper.captureHeader(csv);
      } catch (Exception e) {
         throw new RuntimeException("Error capturing CSV header!", e);
      }

      try {
         List<T> list = new ArrayList<T>();
         while (null != (line = csv.readNext())) {
            lineProcessed++;
            try {
               processLine(mapper, filter, line, list);
            } catch (CsvException e) {
               CsvException csve = (CsvException) e;
               csve.setLineNumber(lineProcessed);
               if (throwExceptions) {
                  throw csve;
               } else {
                    getCapturedExceptions().add(csve);
                }
            }
         }
         return list;
      } catch (Exception e) {
         throw new RuntimeException("Error parsing CSV line: " + lineProcessed + " values: " + Arrays.toString(line), e);
      }
   }

   private void processLine(MappingStrategy<T> mapper, CsvToBeanFilter filter, String[] line, List<T> list)
           throws IllegalAccessException, InvocationTargetException,
           InstantiationException, IntrospectionException,
           CsvBadConverterException, CsvDataTypeMismatchException,
           CsvRequiredFieldEmptyException, CsvConstraintViolationException {
      if (filter == null || filter.allowLine(line)) {
         T obj = processLine(mapper, line);
         list.add(obj);
      }
   }

   /**
    * Creates a single object from a line from the CSV file.
    * @param mapper MappingStrategy
    * @param line  Array of Strings from the CSV file.
    * @return Object containing the values.
    * @throws IllegalAccessException Thrown on error creating bean.
    * @throws InvocationTargetException Thrown on error calling the setters.
    * @throws InstantiationException Thrown on error creating bean.
    * @throws IntrospectionException Thrown on error getting the PropertyDescriptor.
    * @throws CsvBadConverterException If a custom converter cannot be
    *   initialized properly
    * @throws CsvDataTypeMismatchException If the source data cannot be converted
    *   to the type of the destination field
    * @throws CsvRequiredFieldEmptyException If a mandatory field is empty in
    *   the input file
    * @throws CsvConstraintViolationException When the internal structure of
    *   data would be violated by the data in the CSV file
    */
   protected T processLine(MappingStrategy<T> mapper, String[] line)
           throws IllegalAccessException, InvocationTargetException,
           InstantiationException, IntrospectionException,
           CsvBadConverterException, CsvDataTypeMismatchException,
           CsvRequiredFieldEmptyException, CsvConstraintViolationException {
      T bean = mapper.createBean();
      for (int col = 0; col < line.length; col++) {
         if (mapper.isAnnotationDriven()) {
            processField(mapper, line, bean, col);
         } else {
            processProperty(mapper, line, bean, col);
         }
      }
      return bean;
   }

   private void processProperty(MappingStrategy<T> mapper, String[] line, T bean, int col)
           throws IntrospectionException, InstantiationException,
           IllegalAccessException, InvocationTargetException, CsvBadConverterException {
      PropertyDescriptor prop = mapper.findDescriptor(col);
      if (null != prop) {
         String value = checkForTrim(line[col], prop);
         Object obj = convertValue(value, prop);
         prop.getWriteMethod().invoke(bean, obj);
      }
   }

   private void processField(MappingStrategy<T> mapper, String[] line, T bean, int col)
           throws CsvBadConverterException, CsvDataTypeMismatchException,
           CsvRequiredFieldEmptyException, CsvConstraintViolationException {
      BeanField beanField = mapper.findField(col);
      if (beanField != null) {
         String value = line[col];
         beanField.setFieldValue(bean, value);
      }
   }

   private PropertyEditor getPropertyEditorValue(Class<?> cls) {
      if (editorMap == null) {
         editorMap = new HashMap<Class<?>, PropertyEditor>();
      }

      PropertyEditor editor = editorMap.get(cls);

      if (editor == null) {
         editor = PropertyEditorManager.findEditor(cls);
         addEditorToMap(cls, editor);
      }

      return editor;
   }

   private void addEditorToMap(Class<?> cls, PropertyEditor editor) {
      if (editor != null) {
         editorMap.put(cls, editor);
      }
   }


   @Override
   protected PropertyEditor getPropertyEditor(PropertyDescriptor desc) throws InstantiationException, IllegalAccessException {
      Class<?> cls = desc.getPropertyEditorClass();
      if (null != cls) {
         return (PropertyEditor) cls.newInstance();
      }
      return getPropertyEditorValue(desc.getPropertyType());
   }

   /**
    * @return The list of exceptions captured while processing the input file
    */
   public List<CsvException> getCapturedExceptions() {
      if (capturedExceptions == null) {
         capturedExceptions = new ArrayList<CsvException>();
        }
        return capturedExceptions;
    }
}
