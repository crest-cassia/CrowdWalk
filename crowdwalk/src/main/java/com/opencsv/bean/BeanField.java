package com.opencsv.bean;

import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import java.lang.reflect.Field;

/**
 * Used to extend the Field class to add a required flag.
 * This flag determines if the field has to have information, or in the case of
 * the String class cannot be an empty String.
 *
 * @param <T> Type of the bean being populated
 */
public interface BeanField<T> {

    /**
     * Sets the field to be processed.
     *
     * @param field Which field is being populated
     */
    void setField(Field field);

    /**
     * Gets the field to be processed.
     *
     * @return A field object
     * @see java.lang.reflect.Field
     */
    Field getField();

    /**
     * Populates the selected field of the bean.
     * This method performs conversion on the input string and assigns the
     * result to the proper field in the provided bean.
     *
     * @param bean  Object containing the field to be set.
     * @param value String containing the value to set the field to.
     * @param <T>   Type of the bean.
     * @throws CsvDataTypeMismatchException    When the result of data conversion returns
     *                                         an object that cannot be assigned to the selected field
     * @throws CsvRequiredFieldEmptyException  When a field is mandatory, but there is no
     *                                         input datum in the CSV file
     * @throws CsvConstraintViolationException When the internal structure of
     *                                         data would be violated by the data in the CSV file
     */
    <T> void setFieldValue(T bean, String value)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException,
            CsvConstraintViolationException;
}
