package com.opencsv.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for fields to mark if they are required or not.
 *
 * @deprecated This annotation was replaced in version 3.8 by the more flexible
 *   {@link com.opencsv.bean.CsvBindByName}, {@link com.opencsv.bean.CsvBindByPosition},
 *   {@link com.opencsv.bean.CsvCustomBindByName}, and {@link com.opencsv.bean.CsvCustomBindByPosition}.
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvBind {
    /**
     * @return If the field is required to contain information.
     */
    boolean required() default false;
}
