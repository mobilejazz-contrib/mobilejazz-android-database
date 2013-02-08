package cat.mobilejazz.database;

import java.util.Iterator;
import java.util.List;

import android.text.TextUtils;
import cat.mobilejazz.utilities.collections.IterableAdapter;
import cat.mobilejazz.utilities.format.StringFormatter;

public class SingleSelectView extends View {

	public static class ViewColumn {

		public static ViewColumn NULL = new ViewColumn(null, null);

		private String mName;
		private String mAlias;
		private String mAggregation;

		public ViewColumn(String name, String alias) {
			this(name, alias, null);
		}

		public ViewColumn(String name, String alias, String aggregation) {
			if (TextUtils.isEmpty(name)) {
				mName = "NULL";
			} else {
				mName = name;
			}
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
			} else if (!TextUtils.isEmpty(mName)) {
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

	private static class DependencyIterable extends IterableAdapter<Dependency, String> {

		public DependencyIterable(Iterable<Dependency> delegate) {
			super(delegate);
		}

		@Override
		protected String convert(Dependency obj) {
			return obj.getTable();
		}

	}

	private List<ViewColumn> mColumns;

	private DependencyIterable mDependencies;
	private String mSelectSuffix;

	public SingleSelectView(String name, List<ViewColumn> columns, List<Dependency> dependencies) {
		this(name, columns, dependencies, "");
	}

	public SingleSelectView(String name, List<ViewColumn> columns, List<Dependency> dependencies, String selectSuffix) {
		super(name);
		mDependencies = new DependencyIterable(dependencies);
		mColumns = columns;
		mSelectSuffix = selectSuffix;
	}

	public Iterable<String> getDependencies() {
		return mDependencies;
	}

	@Override
	public String getSqlSelectStatement() {
		StringBuilder s = new StringBuilder();
		s.append("SELECT ");
		StringFormatter.printIterable(s, mColumns);
		s.append(" FROM ");
		Iterator<Dependency> it = mDependencies.getDelegate().iterator();
		s.append(it.next().getTable());
		while (it.hasNext()) {
			Dependency dep = it.next();
			dep.appendTo(s);
		}
		s.append(mSelectSuffix);
		return s.toString();
	}

}
