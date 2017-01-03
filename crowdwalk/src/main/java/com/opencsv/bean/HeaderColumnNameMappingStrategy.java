package com.opencsv.bean;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvBadConverterException;
import org.apache.commons.lang3.StringUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Copyright 2007 Kyle Miller.
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

/**
 * Maps data to objects using the column names in the first row of the CSV file
 * as reference. This way the column order does not matter.
 *
 * @param <T> Type of the bean to be returned
 */
public class HeaderColumnNameMappingStrategy<T> implements MappingStrategy<T> {

    protected String[] header;
    protected Map<String, Integer> indexLookup = new HashMap<String, Integer>();
    protected Map<String, PropertyDescriptor> descriptorMap = null;
    protected Map<String, BeanField> fieldMap = null;
    protected Class<? extends T> type;
    protected boolean annotationDriven;

    /**
     * Default constructor.
     */
    public HeaderColumnNameMappingStrategy() {
    }

    @Override
    public void captureHeader(CSVReader reader) throws IOException {
        header = reader.readNext();
    }

    /**
     * Creates an index map of column names to column position.
     *
     * @param values Array of header values.
     */
    protected void createIndexLookup(String[] values) {
        if (indexLookup.isEmpty()) {
            for (int i = 0; i < values.length; i++) {
                indexLookup.put(values[i], i);
            }
        }
    }

    /**
     * Resets index map of column names to column position.
     */
    protected void resetIndexMap() {
        indexLookup.clear();
    }

    @Override
    public Integer getColumnIndex(String name) {
        if (null == header) {
            throw new IllegalStateException("The header row hasn't been read yet.");
        }

        createIndexLookup(header);

        return indexLookup.get(name);
    }

    @Override
    public PropertyDescriptor findDescriptor(int col)
            throws IntrospectionException {
        String columnName = getColumnName(col);
        return (StringUtils.isNotBlank(columnName)) ? findDescriptor(columnName) : null;
    }

    @Override
    public BeanField findField(int col) throws CsvBadConverterException {
        String columnName = getColumnName(col);
        return (StringUtils.isNotBlank(columnName)) ? findField(columnName) : null;
    }

    /**
     * Get the column name for a given column position.
     *
     * @param col Column position.
     * @return The column name or null if the position is larger than the
     * header array or there are no headers defined.
     */
    public String getColumnName(int col) {
        return (null != header && col < header.length) ? header[col] : null;
    }

    /**
     * Find the property descriptor for a given column.
     *
     * @param name Column name to look up.
     * @return The property descriptor for the column.
     * @throws IntrospectionException Thrown on error loading the property
     *                                descriptors.
     */
    protected PropertyDescriptor findDescriptor(String name) throws IntrospectionException {
        if (null == descriptorMap) {
            descriptorMap = loadDescriptorMap(); //lazy load descriptors
        }
        return descriptorMap.get(name.toUpperCase().trim());
    }

    /**
     * Find the field for a given column.
     *
     * @param name The column name to look up.
     * @return BeanField containing the field for the column.
     * @throws CsvBadConverterException If a custom converter for a field cannot
     *                                  be initialized
     */
    protected BeanField findField(String name) throws CsvBadConverterException {
        return fieldMap.get(name.toUpperCase().trim());
    }

    /**
     * Determines if the name of a property descriptor matches the column name.
     * Currently only used by unit tests.
     *
     * @param name Name of the column.
     * @param desc Property descriptor to check against
     * @return True if the name matches the name in the property descriptor.
     */
    protected boolean matches(String name, PropertyDescriptor desc) {
        return desc.getName().equals(name.trim());
    }

    /**
     * Builds a map of property descriptors for the bean.
     *
     * @return Map of property descriptors
     * @throws IntrospectionException Thrown on error getting information
     *                                about the bean.
     */
    protected Map<String, PropertyDescriptor> loadDescriptorMap() throws IntrospectionException {
        Map<String, PropertyDescriptor> map = new HashMap<String, PropertyDescriptor>();

        PropertyDescriptor[] descriptors;
        descriptors = loadDescriptors(getType());
        for (PropertyDescriptor descriptor : descriptors) {
            map.put(descriptor.getName().toUpperCase().trim(), descriptor);
        }

        return map;
    }

    /**
     * Builds a map of fields for the bean.
     *
     * @throws CsvBadConverterException If there is a problem instantiating the
     *                                  custom converter for an annotated field
     */
    protected void loadFieldMap() throws CsvBadConverterException {
        fieldMap = new HashMap<String, BeanField>();

        for (Field field : loadFields(getType())) {
            String columnName, locale;

            // Always check for a custom converter first.
            if (field.isAnnotationPresent(CsvCustomBindByName.class)) {
                columnName = field.getAnnotation(CsvCustomBindByName.class).column().toUpperCase().trim();
                Class<? extends AbstractBeanField> converter = field
                        .getAnnotation(CsvCustomBindByName.class)
                        .converter();
                BeanField bean;
                try {
                    bean = converter.newInstance();
                } catch (IllegalAccessException oldEx) {
                    CsvBadConverterException newEx =
                            new CsvBadConverterException(converter,
                                    "There was a problem instantiating the custom converter "
                                            + converter.getCanonicalName());
                    newEx.initCause(oldEx);
                    throw newEx;
                } catch (InstantiationException oldEx) {
                    CsvBadConverterException newEx =
                            new CsvBadConverterException(converter,
                                    "There was a problem instantiating the custom converter "
                                            + converter.getCanonicalName());
                    newEx.initCause(oldEx);
                    throw newEx;
                }
                bean.setField(field);
                fieldMap.put(columnName, bean);
            }

            // Then check for CsvBindByName.
            else if (field.isAnnotationPresent(CsvBindByName.class)) {
                boolean required = field.getAnnotation(CsvBindByName.class).required();
                columnName = field.getAnnotation(CsvBindByName.class).column().toUpperCase().trim();
                locale = field.getAnnotation(CsvBindByName.class).locale();
                if (field.isAnnotationPresent(CsvDate.class)) {
                    String formatString = field.getAnnotation(CsvDate.class).value();
                    if (StringUtils.isEmpty(columnName)) {
                        fieldMap.put(field.getName().toUpperCase().trim(),
                                new BeanFieldDate(field, required, formatString, locale));
                    } else {
                        fieldMap.put(columnName, new BeanFieldDate(field, required, formatString, locale));
                    }
                } else {
                    if (StringUtils.isEmpty(columnName)) {
                        fieldMap.put(field.getName().toUpperCase().trim(),
                                new BeanFieldPrimitiveTypes(field, required, locale));
                    } else {
                        fieldMap.put(columnName, new BeanFieldPrimitiveTypes(field, required, locale));
                    }
                }
            }

            // And only check for CsvBind if nothing else is there, because
            // CsvBind is deprecated.
            else {
                boolean required = field.getAnnotation(CsvBind.class).required();
                fieldMap.put(field.getName().toUpperCase().trim(),
                        new BeanFieldPrimitiveTypes(field, required, null));
            }
        }
    }

    private PropertyDescriptor[] loadDescriptors(Class<? extends T> cls) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(cls);
        return beanInfo.getPropertyDescriptors();
    }

    private List<Field> loadFields(Class<? extends T> cls) {
        List<Field> fields = new ArrayList<Field>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(CsvBind.class)
                    || field.isAnnotationPresent(CsvBindByName.class)
                    || field.isAnnotationPresent(CsvCustomBindByName.class)) {
                fields.add(field);
            }
        }
        annotationDriven = !fields.isEmpty();
        return fields;
    }

    @Override
    public T createBean() throws InstantiationException, IllegalAccessException {
        return type.newInstance();
    }

    /**
     * Get the class type that the Strategy is mapping.
     *
     * @return Class of the object that mapper will create.
     */
    public Class<? extends T> getType() {
        return type;
    }

    /**
     * Sets the class type that is being mapped.
     * Also initializes the mapping between column names and bean fields.
     *
     * @param type Class type.
     * @throws CsvBadConverterException If a field in the bean is annotated
     *                                  with a custom converter that cannot be initialized. If you are not
     *                                  using custom converters that you have written yourself, it should be
     *                                  safe to catch this exception and ignore it.
     */
    public void setType(Class<? extends T> type) throws CsvBadConverterException {
        this.type = type;
        loadFieldMap();
    }

    /**
     * Determines whether the mapping strategy is driven by annotations.
     * For this mapping strategy, the supported annotations are:
     * <ul><li>{@link com.opencsv.bean.CsvBindByName}</li>
     * <li>{@link com.opencsv.bean.CsvCustomBindByName}</li>
     * <li>{@link com.opencsv.bean.CsvBind}</li>
     * </ul>
     *
     * @return Whether the mapping strategy is driven by annotations
     */
    @Override
    public boolean isAnnotationDriven() {
        return annotationDriven;
    }
}
