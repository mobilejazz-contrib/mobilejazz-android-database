package cat.mobilejazz.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks this column to hold the creation dates of this table. There can be only
 * one such column for each table. The creation date defines when a value was
 * first entered into the database. This field can be used to resolve
 * synchronization issues.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreationDate {
}
