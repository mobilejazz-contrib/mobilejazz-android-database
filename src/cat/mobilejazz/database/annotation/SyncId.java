package cat.mobilejazz.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks this column to be the synchronization id of this table. There can be
 * only one such column for each table. The synchronization id is used, when
 * entities are compared with new results from the server to determine which
 * entities need to be deleted, inserted or updated. It is assumed that this
 * column uniquely identifies the entity within the table.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SyncId {
}
