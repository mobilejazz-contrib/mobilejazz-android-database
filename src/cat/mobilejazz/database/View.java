package cat.mobilejazz.database;

import java.util.Iterator;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import cat.mobilejazz.utilities.debug.Debug;
import cat.mobilejazz.utilities.format.StringFormatter;
import cat.mobilejazz.utilities.format.TreeObject;

public class View implements TreeObject {

	public static class ViewColumn {

		private String mName;
		private String mAlias;
		private String mAggregation;

		public ViewColumn(String name, String alias) {
			this(name, alias, null);
		}

		public ViewColumn(String name, String alias, String aggregation) {
			mName = name;
			mAlias = alias;
			mAggregation = aggregation;
		}

		public String getName() {
			return mName;
		}

		public String getAlias() {
			return mAlias;
		}
		
		private StringBuilder aggregate(StringBuilder s, String name) {
			if (!TextUtils.isEmpty(mAggregation)) {
				s.append(mAggregation).append('(').append(name).append(')');
			} else {
				s.append(name);
			}
			return s;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			if (!TextUtils.isEmpty(mAlias)) {
				aggregate(s, mAlias).append(" AS ");
				s.append(mName);
			} else {
				aggregate(s, mName);
			}
			return s.toString();
		}

	}

	public static class Dependency {

		private String mTable;
		private String mTableAlias;
		private String mColumnLeft;
		private String mColumnRight;
		private String mJoin;

		public Dependency(String table, String columnLeft, String columnRight) {
			this(table, null, columnLeft, columnRight);
		}
		
		public Dependency(String table, String tableAlias, String columnLeft, String columnRight) {
			this(table, tableAlias, columnLeft, columnRight, "LEFT JOIN");
		}

		public Dependency(String table, String tableAlias, String columnLeft, String columnRight, String joinType) {
			mTable = table;
			mTableAlias = tableAlias;
			mColumnLeft = columnLeft;
			mColumnRight = columnRight;
			mJoin = joinType;
		}

		public String getTable() {
			return mTable;
		}

		public String getTableAlias() {
			return mTableAlias;
		}

		public String getColumnLeft() {
			return mColumnLeft;
		}

		public String getColumnRight() {
			return mColumnRight;
		}

		public void appendTo(StringBuilder s) {
			s.append(' ').append(mJoin).append(' ').append(mTable);
			if (mTableAlias != null) {
				s.append(" ").append(mTableAlias);
			}
			s.append(" ON ").append(mColumnLeft).append(" = ").append(mColumnRight);
		}

	}

	private List<ViewColumn> mColumns;
	private List<Dependency> mDependencies;

	private String mSqlStatement;
	private String mName;
	private String mSelectSuffix;

	public View(String name, List<ViewColumn> columns, List<Dependency> dependencies) {
		this(name, columns, dependencies, "");
	}
	
	public View(String name, List<ViewColumn> columns, List<Dependency> dependencies, String selectSuffix) {
		mName = name;
		mColumns = columns;
		mDependencies = dependencies;
		mSelectSuffix = selectSuffix;
		update();
	}
	
	/**
	 * Creates a view directly from a raw SQL statement.
	 * 
	 * @param sqlStatement A {@link String} representing a valid sql statement that creates this view.
	 */
	public View(String name, String sqlStatement, List<Dependency> dependencies) {
		mName = name;
		mSqlStatement = sqlStatement;
		mDependencies = dependencies;
	}

	public Iterable<Dependency> getDependencies() {
		return mDependencies;
	}

	protected void update() {
		StringBuilder s = new StringBuilder();
		s.append("CREATE VIEW ").append(mName).append(" AS SELECT ");
		StringFormatter.printIterable(s, mColumns);
		s.append(" FROM ");
		Iterator<Dependency> it = mDependencies.iterator();
		s.append(it.next().getTable());
		while (it.hasNext()) {
			Dependency dep = it.next();
			dep.appendTo(s);
		}
		s.append(mSelectSuffix);
		mSqlStatement = s.toString();
	}

	public void create(SQLiteDatabase db) {
		Debug.verbose("Creating VIEW: ");
		Debug.verbose(mSqlStatement);
		db.execSQL(mSqlStatement);
	}

	public String getName() {
		return mName;
	}

	@Override
	public StringBuilder dump(StringBuilder result, int indent) {
		StringFormatter.appendWS(result, indent * 2);
		result.append(mSqlStatement);
		return result;
	}

}
