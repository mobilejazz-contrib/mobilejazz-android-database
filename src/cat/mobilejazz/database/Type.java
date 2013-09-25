package cat.mobilejazz.database;

import android.content.ContentValues;

/**
 * Listing of possible column types for use with {@link Column#type()}. Note
 * that the types correspond to the different flavours of {@code put} methods in
 * {@link ContentValues} plus a special delegate type.
 */
public class Type {

	public static final int BOOLEAN = 0;
	public static final int INT = 2;
	public static final int LONG = 3;
	public static final int STRING = 5;
	public static final int DOUBLE = 7;

	/**
	 * Indicates that this column is not modeled in the table directly. Instead
	 * it indicates a one to many or even many to many relation.
	 */
	public static final int DELEGATE = 8;

	/**
	 * In a relation table this indicates a column that is pointing to the
	 * "parent" table. The parent table is the one that has a delegate column to
	 * the "child" table. Note the id is always of type {@link long}.
	 */
	public static final int DELEGATE_PARENT_ID = 9;
	/**
	 * In a relation table this indicates a column that is pointing to the
	 * "child" table. The child table is the one that is referenced via a
	 * delegate column in the "parent" table. Note the id is always of type
	 * {@link long}.
	 */
	public static final int DELEGATE_CHILD_ID = 10;

	public static String asString(int type) {
		switch (type) {
		case BOOLEAN:
			return "boolean";
		case INT:
			return "int";
		case LONG:
			return "long";
		case STRING:
			return "String";
		case DOUBLE:
			return "double";
		case DELEGATE:
			return "delegate";
		default:
			return "unkown";
		}
	}

}
