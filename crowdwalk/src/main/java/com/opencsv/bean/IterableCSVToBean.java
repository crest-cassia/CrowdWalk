package com.opencsv.bean;

/*
 * Copyright 2015 Bytecode Pty Ltd.
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

import com.opencsv.CSVReader;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Converts CSV strings to objects.
 * Unlike CsvToBean it returns a single record at a time.
 *
 * @param <T> Class to convert the objects to.
 */

public class IterableCSVToBean<T> extends AbstractCSVToBean implements Iterable<T> {
    private final MappingStrategy<T> strategy;
    private final CSVReader csvReader;
    private final CsvToBeanFilter filter;
    private Map<Class<?>, PropertyEditor> editorMap = null;
    private boolean hasHeader;

    /**
     * IterableCSVToBean constructor
     *
     * @param csvReader CSVReader.  Should not be null.
     * @param strategy  MappingStrategy used to map CSV data to the bean.  Should not be null.
     * @param filter    Optional CsvToBeanFilter used remove unwanted data from reads.
     */
    public IterableCSVToBean(CSVReader csvReader, MappingStrategy<T> strategy, CsvToBeanFilter filter) {
        this.csvReader = csvReader;
        this.strategy = strategy;
        this.filter = filter;
        this.hasHeader = false;
    }

    /**
     * Retrieves the MappingStrategy.
     * @return The MappingStrategy being used by the IterableCSVToBean.
     */
    protected MappingStrategy<T> getStrategy() {
        return strategy;
    }

    /**
     * Retrieves the CSVReader.
     * @return The CSVReader being used by the IterableCSVToBean.
     */
    protected CSVReader getCSVReader() {
        return csvReader;
    }

    /**
     * Retrieves the CsvToBeanFilter
     *
     * @return The CsvToBeanFilter being used by the IterableCSVToBean.
     */
    protected CsvToBeanFilter getFilter() {
        return filter;
    }

    /**
     * Reads and processes a single line.
     * @return Object of type T with the requested information or null if there
     *   are no more lines to process.
     * @throws IllegalAccessException Thrown if there is a failure in introspection.
     * @throws InstantiationException Thrown when getting the PropertyDescriptor for the class.
     * @throws IOException Thrown when there is an unexpected error reading the file.
     * @throws IntrospectionException Thrown if there is a failure in introspection.
     * @throws InvocationTargetException Thrown if there is a failure in introspection.
     */
    public T nextLine() throws IllegalAccessException, InstantiationException,
            IOException, IntrospectionException, InvocationTargetException {
        if (!hasHeader) {
            strategy.captureHeader(csvReader);
            hasHeader = true;
        }
        T bean = null;
        String[] line;
        do {
            line = csvReader.readNext();
        } while (line != null && (filter != null && !filter.allowLine(line)));
        if (line != null) {
            bean = strategy.createBean();
            for (int col = 0; col < line.length; col++) {
                PropertyDescriptor prop = strategy.findDescriptor(col);
                if (null != prop) {
                    String value = checkForTrim(line[col], prop);
                    Object obj = convertValue(value, prop);
                    prop.getWriteMethod().invoke(bean, obj);
                }
            }
        }
        return bean;
    }

    @Override
    protected PropertyEditor getPropertyEditor(PropertyDescriptor desc) throws InstantiationException, IllegalAccessException {
        Class<?> cls = desc.getPropertyEditorClass();
        if (null != cls) {
            return (PropertyEditor) cls.newInstance();
        }
        return getPropertyEditorValue(desc.getPropertyType());
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
    public Iterator<T> iterator() {
        return iterator(this);
    }

    private Iterator<T> iterator(final IterableCSVToBean<T> bean) {
        return new Iterator<T>() {
            private T nextBean;

            @Override
            public boolean hasNext() {
                if (nextBean != null) {
                    return true;
                }

                try {
                    nextBean = bean.nextLine();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IntrospectionException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                return nextBean != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                T holder = nextBean;
                nextBean = null;
                return holder;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("This is a read only iterator.");
            }
        };
    }
}
