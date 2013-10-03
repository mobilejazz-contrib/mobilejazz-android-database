package cat.mobilejazz.database;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import cat.mobilejazz.database.annotation.CreationDate;
import cat.mobilejazz.database.annotation.Local;
import cat.mobilejazz.database.annotation.ParentId;
import cat.mobilejazz.database.annotation.SyncId;
import cat.mobilejazz.database.annotation.TableName;
import cat.mobilejazz.database.annotation.UID;
import cat.mobilejazz.database.content.DataProvider;
import cat.mobilejazz.utilities.debug.Debug;
import cat.mobilejazz.utilities.format.StringFormatter;
import cat.mobilejazz.utilities.format.TreeObject;

public class Table implements TreeObject {

	/**
	 * If this is set, no changes are being recorded.
	 */
	private boolean isLocal;

	private Collection<View> referencedBy;
	private Map<String, Column> columns;
	private String name;
	private String declaredName;

	private Column syncId;
	private Column parentId;
	private String parentTableName;
	private EntityContext<String> parentTableFromContext;
	private Column creationDate;

	private void setSyncIdColumn(Column c) {
		if (syncId == null) {
			syncId = c;
		} else {
			throw new IllegalArgumentException("Cannot set column " + c.getName()
					+ " as sync id. There is already a sync id " + syncId.getName());
		}
	}

	private void setCreationDateColumn(Column c) {
		if (creationDate == null) {
			creationDate = c;
		} else {
			throw new IllegalArgumentException("Cannot set column " + c.getName()
					+ " as creation date. There is already a creation date " + creationDate.getName());
		}
	}

	private void setParentIdColumn(Column c) {
		if (parentId == null) {
			parentId = c;
		} else {
			throw new IllegalArgumentException("Cannot set column " + c.getName()
					+ " as parent id. There is already a parent id " + parentId.getName());
		}
	}

	public Table(Class<?> tableDescription) throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, InstantiationException {
		this.declaredName = tableDescription.getSimpleName();
		columns = new HashMap<String, Column>();
		referencedBy = new ArrayList<View>();

		isLocal = tableDescription.isAnnotationPresent(Local.class);

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
				DataParser<?> parser = null;
				String defaultValue = "";
				if (column != null) {
					type = column.type();
					affinity = column.affinity();
					constraint = column.constraint();
					storage = column.storage();
					delegate = column.delegate();
					defaultValue = column.defaultValue();
					if (!TextUtils.isEmpty(delegate)) {
						type = Type.DELEGATE;
					}
					if (!column.parser().equals(IdentityParser.class)) {
						parser = column.parser().newInstance();
					}
				}
				Column c = new Column(type, affinity, constraint, storage, columnName, f.getName(), delegate,
						defaultValue, parser, f.getAnnotation(UID.class) != null, this);
				columns.put(columnName, c);

				if (f.getAnnotation(SyncId.class) != null) {
					setSyncIdColumn(c);
				}

				if (f.getAnnotation(CreationDate.class) != null) {
					setCreationDateColumn(c);
				}

				ParentId pid = f.getAnnotation(ParentId.class);
				if (pid != null) {
					setParentIdColumn(c);
					parentTableName = pid.parentTable();
					if (parentTableName.equals(ParentId.TABLE_FROM_CONTEXT)) {
						parentTableName = null;
						parentTableFromContext = pid.parentTableFromContext().newInstance();
					}
				}
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

	/**
	 * Defines whether a table is defined to store local data that has no
	 * correspondence with remote server data. If this is the case, the
	 * {@link DataProvider} does not need to record changes on this data
	 * structure as they do not need to be synchronized with the server. This
	 * field may be used to store management data such as a caching table.
	 * 
	 * @return {@code true} if this is a local table.
	 */
	public boolean isLocal() {
		return isLocal;
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

	/**
	 * The synchronization id is used, when entities are compared with new
	 * results from the server to determine which entities need to be deleted,
	 * inserted or updated. It is assumed that this column uniquely identifies
	 * the entity within the table.
	 * 
	 * @return The column representing the synchronization id.
	 */
	public Column getColumnSyncId() {
		return syncId;
	}

	public boolean hasColumnSyncId() {
		return syncId != null;
	}

	/**
	 * The creation date defines when a value was first entered into the
	 * database. This field can be used to resolve synchronization issues.
	 * 
	 * @return The column representing the creation date.
	 */
	public Column getColumnCreationDate() {
		return creationDate;
	}

	public boolean hasColumnCreationDate() {
		return creationDate != null;
	}

	/**
	 * The parent id is used for relations to identify the corresponding element
	 * in the parent relation.
	 */
	public Column getColumnParentId() {
		return parentId;
	}

	public String getParentTable(ContentValues entity) {
		if (parentTableName != null) {
			return parentTableName;
		} else {
			return parentTableFromContext.get(entity);
		}
	}

	public boolean hasColumnParentId() {
		return parentId != null;
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
