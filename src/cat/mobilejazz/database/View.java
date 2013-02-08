package cat.mobilejazz.database;

import android.database.sqlite.SQLiteDatabase;

public abstract class View {

	private String name;

	public View(String name) {
		this.name = name;
	}

	public abstract Iterable<String> getDependencies();

	public abstract String getSqlSelectStatement();

	public void create(SQLiteDatabase db) {
		StringBuilder s = new StringBuilder();
		s.append("CREATE VIEW ").append(name).append(" AS ");
		s.append(getSqlSelectStatement());
		db.execSQL(s.toString());
	}

	public String getName() {
		return name;
	}

}
