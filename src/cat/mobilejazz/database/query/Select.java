package cat.mobilejazz.database.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.content.CursorLoader;
import cat.mobilejazz.database.SQLUtils;
import cat.mobilejazz.database.content.DataProvider;
import cat.mobilejazz.database.content.EnabledCursorLoader;
import cat.mobilejazz.database.content.LoaderParent;
import cat.mobilejazz.utilities.format.StringFormatter;

public class Select implements Parcelable {

	public static class Builder {

		private Uri mTable;
		private String[] mProjection;
		private List<String> mSelection;
		private List<String> mSelectionArgs;
		private String mSortOrder;

		private String valueOf(Object arg) {
			return SQLUtils.valueOf(arg);
		}

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
			mSelectionArgs.add(valueOf(value));
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

		public Builder constraintNotIsNull(String column) {
			mSelection.add(String.format("NOT %s IS NULL", column));
			return this;
		}

		public Builder constraint(String constraint, Object... values) {
			mSelection.add(String.format("(%s)", constraint));
			for (Object v : values) {
				mSelectionArgs.add(valueOf(v));
			}

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
				mSelectionArgs.add(valueOf(v));
			}
			return this;
		}

		public <T> Builder constraintNotIn(String column, T... values) {
			selectionConstraintList(column, "NOT IN", values.length);
			for (T v : values) {
				mSelectionArgs.add(valueOf(v));
			}
			return this;
		}

		public <T> Builder constraintIn(String column, Collection<T> values) {
			selectionConstraintList(column, "IN", values.size());
			for (T v : values) {
				mSelectionArgs.add(valueOf(v));
			}
			return this;
		}

		public <T> Builder constraintNotIn(String column, Collection<T> values) {
			selectionConstraintList(column, "NOT IN", values.size());
			for (T v : values) {
				mSelectionArgs.add(valueOf(v));
			}
			return this;
		}

		public Builder constraintIn(String column, long... values) {
			selectionConstraintList(column, "IN", values.length);
			for (long v : values) {
				mSelectionArgs.add(valueOf(v));
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

	private static final Pattern PATTERN = Pattern.compile("\\d+");

	private Uri table;
	private String[] projection;
	private String selection;
	private String[] selectionArgs;
	private String sortOrder;

	public Select(Uri table) {
		this(table, null, null, null, null);
	}

	public Select(Uri table, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		this.table = table;
		this.projection = projection;
		this.selection = selection;
		this.selectionArgs = selectionArgs;
		this.sortOrder = sortOrder;
	}

	public Uri getUri() {
		return table;
	}

	public String getTable() {
		String last = table.getLastPathSegment();
		if (PATTERN.matcher(last).matches()) {
			// last path segment is the id. But we want the table:
			return table.getPathSegments().get(table.getPathSegments().size() - 2);
		} else {
			return last;
		}
	}

	public String[] getProjection() {
		return projection;
	}

	public String getSelection() {
		return selection;
	}

	public String[] getSelectionArgs() {
		return selectionArgs;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public ContentProviderOperation getContentProviderOperationDelete() {
		return ContentProviderOperation.newDelete(table).withSelection(selection, selectionArgs).build();
	}

	public CursorLoader newCursorLoader(Context context) {
		return new CursorLoader(context, table, projection, selection, selectionArgs, sortOrder);
	}

	public EnabledCursorLoader newEnabledCursorLoader(LoaderParent parent) {
		return new EnabledCursorLoader(parent, table, projection, selection, selectionArgs, sortOrder);
	}

	public int delete(ContentResolver provider) {
		return provider.delete(table, selection, selectionArgs);
	}

	public int update(ContentResolver provider, ContentValues values) {
		return provider.update(table, values, selection, selectionArgs);
	}

	public Cursor query(ContentResolver provider) {
		return provider.query(table, projection, selection, selectionArgs, sortOrder);
	}

	public int delete(ContentProviderClient provider) throws RemoteException {
		return provider.delete(table, selection, selectionArgs);
	}

	public int update(ContentProviderClient provider, ContentValues values) throws RemoteException {
		return provider.update(table, values, selection, selectionArgs);
	}

	public Cursor query(ContentProviderClient provider) throws RemoteException {
		return provider.query(table, projection, selection, selectionArgs, sortOrder);
	}

	public int delete(ContentProvider provider) {
		return provider.delete(table, selection, selectionArgs);
	}

	public int update(ContentProvider provider, ContentValues values) {
		return provider.update(table, values, selection, selectionArgs);
	}

	public Cursor query(ContentProvider provider) {
		return provider.query(table, projection, selection, selectionArgs, sortOrder);
	}

	public String toSql() {
		StringBuilder s = new StringBuilder();
		s.append("SELECT ");
		if (projection == null) {
			s.append("*");
		} else {
			StringFormatter.printIterable(s, projection);
		}
		s.append(" FROM ").append(getTable()).append(" WHERE ").append(selection);
		if (sortOrder != null) {
			s.append(" SORTED BY ").append(sortOrder);
		}
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("SELECT ");
		if (projection == null) {
			s.append("*");
		} else {
			StringFormatter.printIterable(s, projection);
		}
		s.append(" FROM ").append(table).append(" WHERE ").append(selection);
		if (sortOrder != null) {
			s.append(" SORTED BY ").append(sortOrder);
		}
		s.append(" [");
		StringFormatter.printIterable(s, selectionArgs);
		s.append("]");
		return s.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(table, flags);
		dest.writeStringArray(projection);
		dest.writeString(selection);
		dest.writeStringArray(selectionArgs);
		dest.writeString(sortOrder);
	}

	private Select(Parcel in) {
		table = in.readParcelable(Uri.class.getClassLoader());
		projection = in.createStringArray();
		selection = in.readString();
		selectionArgs = in.createStringArray();
		sortOrder = in.readString();
	}

	public static final Parcelable.Creator<Select> CREATOR = new Parcelable.Creator<Select>() {
		public Select createFromParcel(Parcel in) {
			return new Select(in);
		}

		public Select[] newArray(int size) {
			return new Select[size];
		}
	};

}
