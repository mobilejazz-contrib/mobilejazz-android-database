package cat.mobilejazz.database;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import cat.mobilejazz.database.annotation.TableName;
import cat.mobilejazz.utilities.debug.Debug;
import cat.mobilejazz.utilities.format.StringFormatter;
import cat.mobilejazz.utilities.format.TreeObject;

public class Table implements TreeObject {

	private Collection<View> referencedBy;
	private Map<String, Column> columns;
	private String name;
	private String declaredName;

	public Table(Class<?> tableDescription) throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, InstantiationException {
		this.declaredName = tableDescription.getSimpleName();
		columns = new HashMap<String, Column>();
		referencedBy = new ArrayList<View>();
		for (Field f : tableDescription.getFields()) {
			if (f.isAnnotationPresent(TableName.class)) {
				this.name = (String) f.get(null);
			} else if (f.isAnnotationPresent(cat.mobilejazz.database.annotation.Column.class)
					|| f.getName().equals("_ID")) {
				cat.mobilejazz.database.annotation.Column column = f
						.getAnnotation(cat.mobilejazz.database.annotation.Column.class);
				String columnName = (String) f.get(null);
				int type = Type.LONG;
				int affinity = Affinity.INTEGER;
				String constraint = "PRIMARY KEY";
				int storage = Storage.LOCAL;
				String delegate = "";
				DataParser parser = null;
				if (column != null) {
					type = column.type();
					affinity = column.affinity();
					constraint = column.constraint();
					storage = column.storage();
					delegate = column.delegate();
					if (!TextUtils.isEmpty(delegate)) {
						type = Type.DELEGATE;
					}
					if (!column.parser().equals(IdentityParser.class)) {
						parser = column.parser().newInstance();
					}
				}
				columns.put(columnName, new Column(type, affinity, constraint, storage, columnName, f.getName(),
						delegate, parser, this));
			}
		}
		if (name == null) {
			throw new NoSuchFieldException();
		}
	}

	public void create(SQLiteDatabase db) {
		StringBuilder result = new StringBuilder();

		result.append("CREATE TABLE ").append(name).append(" (");

		for (Column c : columns.values()) {
			if (c.getType() != Type.DELEGATE) {
				result.append(c);
				result.append(", ");
			}
		}

		result.delete(result.length() - 2, result.length()).append(");");

		Debug.verbose("Creating Table: \n" + result.toString());

		db.execSQL(result.toString());
	}

	/**
	 * Adds the mapping from declared to real values. For example if a column
	 * was declared as <code>COLUMN_NAME = "name"</code> and the table was
	 * declared as <code>MyTable</code> with
	 * <code>MyTable.TABLE_NAME = "table_01", it will add the mappings:
	 * <p><code>"MyTable.COLUMN_NAME" --> "table_01.name"</code></p> and
	 * <p>
	 * <code>"COLUMN_NAME" --> "name"</code>
	 * </p>
	 * 
	 * @param values
	 *            The map which should receive the values.
	 */
	public void appendDeclaredValues(Map<String, Object> values) {
		values.put(declaredName, name);
		for (Column c : columns.values()) {
			values.put(c.getDeclaredName(), c.getName());
			values.put(declaredName + "." + c.getDeclaredName(), name + "." + c.getName());
		}
	}

	public Iterable<Column> getColumns() {
		return columns.values();
	}

	public String getName() {
		return name;
	}

	public String getDeclaredName() {
		return declaredName;
	}

	public Iterable<View> getReferencedBy() {
		return referencedBy;
	}

	public void addReferencedBy(View v) {
		referencedBy.add(v);
	}

	public boolean hasColumn(String columnName) {
		return columns.containsKey(columnName);
	}

	public Column getColumn(String columnName) {
		Column c = columns.get(columnName);
		if (c != null) {
			return c;
		} else {
			throw new IllegalArgumentException(String.format("In Table %s: Column %s not found.", name, columnName));
		}
	}

	@Override
	public StringBuilder dump(StringBuilder result, int indent) {
		StringFormatter.appendWS(result, indent * 2);
		result.append(declaredName).append(": ").append(name).append("\n");
		for (Column c : columns.values()) {
			c.dump(result, indent + 1);
			result.append('\n');
		}
		return result;
	}

}
