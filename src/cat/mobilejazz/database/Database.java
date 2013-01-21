package cat.mobilejazz.database;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;
import cat.mobilejazz.database.View.Dependency;
import cat.mobilejazz.database.content.Changes;
import cat.mobilejazz.utilities.debug.Debug;
import cat.mobilejazz.utilities.debug.TreeObject;
import cat.mobilejazz.utilities.format.StringFormatter;

public class Database implements TreeObject {

	private Map<String, Table> tables;
	private List<View> views;

	public static Database newInstance(Class<?> contract, View... databaseViews) {
		try {
			return new Database(contract, databaseViews);
		} catch (IllegalArgumentException e) {
			Debug.logException(e);
		} catch (IllegalAccessException e) {
			Debug.logException(e);
		} catch (InstantiationException e) {
			Debug.logException(e);
		}
		return null;
	}

	public static Database newInstance(Class<?>[] tableClasses, View... databaseViews) {
		try {
			return new Database(tableClasses, databaseViews);
		} catch (IllegalArgumentException e) {
			Debug.logException(e);
		} catch (IllegalAccessException e) {
			Debug.logException(e);
		} catch (InstantiationException e) {
			Debug.logException(e);
		}
		return null;
	}

	private void propagateDependencies(View v) {
		for (Dependency d : v.getDependencies()) {
			String dependencyName = d.getTable();
			// dependency can be table or view:
			Table table = tables.get(dependencyName);
			if (table != null) {
				// dependency is table:
				tables.get(d.getTable()).addReferencedBy(v);
			} else {
				// dependency is view:
				propagateDependencies(getView(dependencyName));
			}
		}
	}

	public Database(Class<?>[] tableClasses, View[] databaseViews) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		tables = new HashMap<String, Table>();
		views = Arrays.asList(databaseViews);

		appendTable(Changes.class);
		for (Class<?> c : tableClasses) {
			appendTable(c);
		}
		for (View v : databaseViews) {
			// this needs to be done in a separate loop, because views can
			// depend on other views.
			propagateDependencies(v);
		}
	}

	public Database(Class<?> contract, View... databaseViews) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		this(contract.getDeclaredClasses(), databaseViews);
		Debug.debug(contract.toString());
		Debug.debug(contract.getCanonicalName());
		Debug.debug(Arrays.toString(contract.getDeclaredClasses()));

	}

	public Iterable<Table> getTables() {
		return tables.values();
	}

	public Iterable<View> getViews() {
		return views;
	}

	public Table getTable(String name) {
		return tables.get(name);
	}

	public View getView(String name) {
		for (View v : views) {
			if (v.getName().equals(name)) {
				return v;
			}
		}
		return null;
	}

	public void createDatabase(SQLiteDatabase db) {
		for (Table t : getTables()) {
			Debug.debug("CREATING " + t.getName());
			t.create(db);
		}
		for (View v : getViews()) {
			Debug.debug("CREATING " + v.getName());
			v.create(db);
		}
	}

	public void dropAll(SQLiteDatabase db) {
		for (Table t : getTables()) {
			db.execSQL("DROP TABLE IF EXISTS " + t.getName());
		}
		for (View v : getViews()) {
			db.execSQL("DROP VIEW IF EXISTS " + v.getName());
		}
	}

	public Map<String, Object> getDeclaredValues() {
		Map<String, Object> values = new HashMap<String, Object>();
		for (Table t : getTables()) {
			t.appendDeclaredValues(values);
		}
		return values;
	}

	@Override
	public StringBuilder dump(StringBuilder result, int indent) {
		StringFormatter.appendWS(result, indent * 2);
		result.append("DATABASE: \n");
		for (Table t : getTables()) {
			t.dump(result, indent + 1);
		}
		for (View v : getViews()) {
			v.dump(result, indent + 1);
		}
		return result;
	}

	private void appendTable(Class<?> c) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		try {
			Table t = new Table(c);
			tables.put(t.getName(), t);
		} catch (NoSuchFieldException e) {
			// ignore is not a table
		}
	}

}
