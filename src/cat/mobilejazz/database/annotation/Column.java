package cat.mobilejazz.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import android.content.ContentValues;
import cat.mobilejazz.database.Affinity;
import cat.mobilejazz.database.DataParser;
import cat.mobilejazz.database.EntityContext;
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
	 * other information is retrieved from the parent table. If the delegate
	 * table need to be defined dynamically based on the entity data (such as
	 * for example a "type" field), set this field to
	 * {@link EntityContext#FROM_ENTITY_CONTEXT} and provide a corresponding
	 * {@link EntityContext} implementation for {{@link #delegateFromContext()}.
	 */
	String delegate() default "";

	/**
	 * Get the delegate table dynamically by providing an implementation of
	 * {@link EntityContext<String>} and setting this field to the corresponding
	 * class.
	 */
	Class<? extends EntityContext<String>> delegateFromContext() default DefaultContext.class;

	Class<? extends DataParser<?>> parser() default IdentityParser.class;

}
