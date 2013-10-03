package cat.mobilejazz.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import android.content.ContentValues;
import cat.mobilejazz.database.Affinity;
import cat.mobilejazz.database.DataParser;
import cat.mobilejazz.database.IdentityParser;
import cat.mobilejazz.database.Storage;
import cat.mobilejazz.database.Type;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

	/**
	 * The default value of this column as defined by SQLite.
	 */
	String defaultValue() default "";

	/**
	 * The type of this column. Defines the data type that is used when adding
	 * this column to a {@link ContentValues} object. See {@link Type} for a
	 * more detailed description. Note that when {@link #delegate()} is set to
	 * some non-empty value, this type automatically defaults to
	 * {@link Type.DELEGATE}.
	 */
	int type() default Type.STRING;

	/**
	 * The affinity of this column as defined by SQLite.
	 */
	int affinity() default Affinity.NONE;

	/**
	 * A constraint expression as defined by SQLite.
	 */
	String constraint() default "";

	/**
	 * The storage type of this column. See {@link Storage} for a more detailed
	 * description.
	 */
	int storage() default Storage.REMOTE;

	/**
	 * A delegate is used to model a one to many or many to many relation. This
	 * means that there is another table, called the "delegate table". For one
	 * to many relations, this delegate table directly stores the related
	 * entities which have a reference back to the current table. For many to
	 * many relations there is an intermediate table (relation) that only stores
	 * id pairs and maybe some additional information. Note that the relation
	 * needs to have two columns, one annotated with {@link ParentId} and one
	 * with {@link SyncId} to indicate where to store that information. All
	 * other information is retrieved from the parent table.
	 */
	String delegate() default "";

	Class<? extends DataParser<?>> parser() default IdentityParser.class;

}
