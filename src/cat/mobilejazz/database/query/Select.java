package cat.mobilejazz.database.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.CursorLoader;
import cat.mobilejazz.database.content.DataProvider;
import cat.mobilejazz.utilities.format.StringFormatter;

public class Select {

	public static class Builder {

		private Uri mTable;
		private String[] mProjection;
		private List<String> mSelection;
		private List<String> mSelectionArgs;
		private String mSortOrder;

		public Builder(Uri table) {
			mTable = table;
			mSelection = new ArrayList<String>();
			mSelectionArgs = new ArrayList<String>();
		}

		public Builder projection(String... columns) {
			mProjection = columns;
			return this;
		}

		public Builder groupedProjection(String... columns) {
			mProjection = new String[columns.length];
			for (int i = 0; i < columns.length; ++i) {
				mProjection[i] = "group_concat(" + columns[i] + ")";
			}
			return this;
		}

		public Builder constraintOp(String column, String operator, Object value) {
			mSelection.add(String.format("%s %s ?", column, operator));
			mSelectionArgs.add(String.valueOf(value));
			return this;
		}

		public Builder constraintEquals(String column, Object value) {
			return constraintOp(column, "=", value);
		}

		public Builder constraintIs(String column, Object value) {
			return constraintOp(column, "IS", value);
		}

		public Builder constraintIsNull(String column) {
			mSelection.add(String.format("%s IS NULL", column));
			return this;
		}

		private void selectionConstraintList(String column, String operator, int length) {
			StringBuilder exp = new StringBuilder();
			exp.append("(?");
			for (int i = 0; i < length - 1; ++i) {
				exp.append(",?");
			}
			exp.append(")");
			mSelection.add(String.format("%s %s %s", column, operator, exp.toString()));
		}

		public <T> Builder constraintIn(String column, T... values) {
			selectionConstraintList(column, "IN", values.length);
			for (T v : values) {
				mSelectionArgs.add(String.valueOf(v));
			}
			return this;
		}

		public <T> Builder constraintNotIn(String column, T... values) {
			selectionConstraintList(column, "NOT IN", values.length);
			for (T v : values) {
				mSelectionArgs.add(String.valueOf(v));
			}
			return this;
		}

		public <T> Builder constraintIn(String column, Collection<T> values) {
			selectionConstraintList(column, "IN", values.size());
			for (T v : values) {
				mSelectionArgs.add(String.valueOf(v));
			}
			return this;
		}

		public <T> Builder constraintNotIn(String column, Collection<T> values) {
			selectionConstraintList(column, "NOT IN", values.size());
			for (T v : values) {
				mSelectionArgs.add(String.valueOf(v));
			}
			return this;
		}

		public Builder constraintIn(String column, long... values) {
			selectionConstraintList(column, "IN", values.length);
			for (long v : values) {
				mSelectionArgs.add(String.valueOf(v));
			}
			return this;
		}

		public Builder sort(String... sortOrder) {
			mSortOrder = StringFormatter.printIterable(sortOrder).toString();
			return this;
		}

		public Builder sort(String sortOrder) {
			mSortOrder = sortOrder;
			return this;
		}

		public Builder groupBy(String column) {
			mTable = mTable.buildUpon().appendQueryParameter(DataProvider.QUERY_KEY_GROUP_BY, column).build();
			return this;
		}

		public Select build() {
			return new Select(mTable, mProjection, StringFormatter.printIterable(mSelection, " AND ").toString(),
					mSelectionArgs.toArray(new String[] {}), mSortOrder);
		}

	}

	private Uri mTable;
	private String[] mProjection;
	private String mSelection;
	private String[] mSelectionArgs;
	private String mSortOrder;

	public Select(Uri table) {
		this(table, null, null, null, null);
	}

	public Select(Uri table, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		mTable = table;
		mProjection = projection;
		mSelection = selection;
		mSelectionArgs = selectionArgs;
		mSortOrder = sortOrder;
	}
	
	public Uri getUri() {
		return mTable;
	}

	public String[] getProjection() {
		return mProjection;
	}

	public String getSelection() {
		return mSelection;
	}

	public String[] getSelectionArgs() {
		return mSelectionArgs;
	}

	public String getSortOrder() {
		return mSortOrder;
	}

	public ContentProviderOperation getContentProviderOperationDelete() {
		return ContentProviderOperation.newDelete(mTable).withSelection(mSelection, mSelectionArgs).build();
	}

	public CursorLoader newCursorLoader(Context context) {
		return new CursorLoader(context, mTable, mProjection, mSelection, mSelectionArgs, mSortOrder);
	}
	
	public int delete(ContentResolver provider) {
		return provider.delete(mTable, mSelection, mSelectionArgs);
	}

	public int update(ContentResolver provider, ContentValues values) {
		return provider.update(mTable, values, mSelection, mSelectionArgs);
	}

	public Cursor query(ContentResolver provider) {
		return provider.query(mTable, mProjection, mSelection, mSelectionArgs, mSortOrder);
	} 

	public int delete(ContentProviderClient provider) throws RemoteException {
		return provider.delete(mTable, mSelection, mSelectionArgs);
	}

	public int update(ContentProviderClient provider, ContentValues values) throws RemoteException {
		return provider.update(mTable, values, mSelection, mSelectionArgs);
	}

	public Cursor query(ContentProviderClient provider) throws RemoteException {
		return provider.query(mTable, mProjection, mSelection, mSelectionArgs, mSortOrder);
	}
	
	public int delete(ContentProvider provider) {
		return provider.delete(mTable, mSelection, mSelectionArgs);
	}

	public int update(ContentProvider provider, ContentValues values) {
		return provider.update(mTable, values, mSelection, mSelectionArgs);
	}

	public Cursor query(ContentProvider provider) {
		return provider.query(mTable, mProjection, mSelection, mSelectionArgs, mSortOrder);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("SELECT ");
		if (mProjection == null) {
			s.append("*");
		} else {
			StringFormatter.printIterable(s, mProjection);
		}
		s.append(" FROM ").append(mTable).append(" WHERE ").append(mSelection).append(" SORTED BY ").append(mSortOrder);
		s.append("\nARGS: ");
		StringFormatter.printIterable(s, mSelectionArgs);
		return s.toString();
	}

}
