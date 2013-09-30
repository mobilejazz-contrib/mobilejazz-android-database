package cat.mobilejazz.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a table contract as local. This means that changes to it are never
 * recorded in the {@link DataProvider} for being later sent to the server. Use
 * this annotation for tables that are only used to store local data such as for
 * example a table with caching information ({@code Last-Modified}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Local {

}
