package cat.mobilejazz.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks this column to be a UID. The {@link DataProvider} makes sure, that
 * there is always a value present for this column in the database, when there
 * is no value provided by the application. Note that currently, the column must
 * be of type {@link long}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UID {
}
