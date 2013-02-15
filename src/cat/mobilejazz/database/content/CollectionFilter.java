package cat.mobilejazz.database.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import cat.mobilejazz.database.query.Select;
import cat.mobilejazz.utilities.ObjectUtils;

/**
 * This class defines a filter that spans the bridge between local filtering and
 * server side filtering. Local filtering is expressed as a combination of
 * table, selection and selection arguments corresponding to the way in which
 * SQLite queries are constituted. Remote filtering is expressed as a set of api
 * paths. The api paths must be defined in a way such that they resemble the
 * filter on the server side.
 * 
 * The {@link CollectionFilter} is used to request filtered updates to the
 * {@link SyncService}. See its documentation for more detail.
 * 
 * @author Hannes Widmoser
 * 
 */
public class CollectionFilter implements Parcelable {

	private String table;
	private Select selection;
	private String[] apiPaths;

	private String formatString;
	private int primaryIdIndex;

	/**
	 * Create a new collection filter.
	 * 
	 * @param table
	 *            The table's uri.
	 * @param selection
	 *            A SQLite compatible selection that may contain question marks
	 *            (?) to refer to elements in <code>selectionArgs</code>.
	 * @param selectionArgs
	 *            An array of values to parameterize the selection. The question
	 *            marks in the selection are replaced by these strings one at a
	 *            time.
	 * @param apiPaths
	 *            An array of server api paths that correspond to the selection.
	 *            In particular, it should be guaranteed that for identical
	 *            local and remote base data sets, querying the server for the
	 *            api paths yields the same result as a query to the content
	 *            provider with the given selection arguments.
	 */
	public CollectionFilter(Uri table, String selection, String[] selectionArgs, String[] apiPaths) {
		this(new Select(table, null, selection, selectionArgs, null), apiPaths);
	}

	public CollectionFilter(Select select, String[] apiPaths) {
		this.selection = select;
		this.apiPaths = apiPaths;
		// cache the table for performance:
		this.table = this.selection.getTable();
	}

	/**
	 * Creates a new collection filter with an incomplete api path definition.
	 * The select points to a sql query which returns a set of ids. The real api
	 * path is generated by using the given format string and formatting it
	 * using the ids returned by the query. This can be used to fetch missing
	 * items from the server. The incomplete api path definition is completed by
	 * executing {@link #deriveApiPaths(Uri, ContentProviderClient)}.
	 * 
	 * @param table
	 *            The table needs to be provided explicitly here because the
	 *            select may point to a different table (or view). This
	 *            parameter is optional however. If set to {@code null}, the
	 *            items will not be deleted however before retrieving the new
	 *            items. This can be used, when fetching items that are known to
	 *            be new.
	 * @param select
	 *            An arbitrary select pointing to a set of ids. Note that the
	 *            table of the select need not be equal to the table
	 *            corresponding to the server items.
	 * @param formatString
	 *            A format string containing a {@code %d} placeholder for each
	 *            id to be replaced.
	 * @param primaryIdIndex
	 *            If the format string contains multiple ids, this argument
	 *            specifies which one is the primary one, i.e. the key of the
	 *            collection.
	 */
	public CollectionFilter(String table, Select select, String formatString, int primaryIdIndex) {
		this.selection = select;
		this.formatString = formatString;
		this.primaryIdIndex = primaryIdIndex;
		// cache the table for performance:
		this.table = table;
	}

	private static final Pattern ID_PATTERN = Pattern.compile("%d");

	private int countIdPatterns() {
		int c = 0;
		Matcher m = ID_PATTERN.matcher(formatString);
		while (m.find())
			c++;
		return c;
	}

	/**
	 * Generates a set of api paths by employing the previously provided
	 * formatting string. You need to provide a table uri by your own since the
	 * uri cannot be derived from the table string here.
	 * 
	 * @param tableUri
	 *            Optional. If the items should be deleted before they are
	 *            fetched from the server (to also reflect deletions in the
	 *            local data).
	 * @param provider
	 * @throws RemoteException
	 */
	public boolean deriveApiPaths(Uri tableUri, ContentProviderClient provider) throws RemoteException {
		int formatCount = countIdPatterns();
		Object[] row = new Object[formatCount];
		Cursor c = selection.query(provider);
		apiPaths = new String[c.getCount()];
		Collection<Long> ids = new ArrayList<Long>();
		c.moveToFirst();

		String primaryIdColumnName = c.getColumnName(primaryIdIndex);

		while (!c.isAfterLast() && c.getPosition() < apiPaths.length) {
			for (int i = 0; i < row.length; ++i) {
				row[i] = c.getLong(i);
			}
			ids.add(c.getLong(primaryIdIndex));
			apiPaths[c.getPosition()] = String.format(formatString, row);
			c.moveToNext();
		}
		c.close();

		if (tableUri != null) {
			selection = new Select.Builder(tableUri).constraintIn(primaryIdColumnName, ids).build();
		} else {
			selection = null;
		}

		return apiPaths.length > 0;
	}

	public String getFormatString() {
		return formatString;
	}

	/**
	 * Gets the local equivalent of this filter as a {@link Select}.
	 * 
	 * @return A {@link Select} representing the filter expression.
	 */
	public Select getSelect() {
		return selection;
	}

	/**
	 * Gets the table that is affected by this filter.
	 * 
	 * @return The table of this filter.
	 */
	public String getTable() {
		return table;
	}

	/**
	 * An SQLite compatible selection WHERE clause string. See
	 * {@link DataProvider#query(android.net.Uri, String[], String, String[], String)}
	 * for details.
	 * 
	 * @return The selection of this filter.
	 */
	public String getSelection() {
		return selection.getSelection();
	}

	/**
	 * You may include ?s in selection, which will be replaced by the values
	 * from selectionArgs, in order that they appear in the selection. The
	 * values will be bound as Strings. See
	 * {@link DataProvider#query(android.net.Uri, String[], String, String[], String)}
	 * for details.
	 * 
	 * @return The selection arguments of this filter.
	 */
	public String[] getSelectionArgs() {
		return selection.getSelectionArgs();
	}

	/**
	 * Possibly multiple api paths that together are an equivalent filter as
	 * provided by the other parameters.
	 * 
	 * @return The api path of this filter.
	 */
	public String[] getApiPaths() {
		return apiPaths;
	}

	@Override
	public boolean equals(Object o) {
		try {
			CollectionFilter other = (CollectionFilter) o;
			return (ObjectUtils.equals(other.table, table) && ObjectUtils.equals(other.getSelection(), getSelection())
					&& Arrays.equals(other.getSelectionArgs(), getSelectionArgs()) && Arrays.equals(other.apiPaths,
					apiPaths));
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return String.format("%s, %s, %s, %s", Arrays.toString(apiPaths), table, getSelection(),
				Arrays.toString(getSelectionArgs()));
	}

	private CollectionFilter(Parcel in) {
		selection = in.readParcelable(Select.class.getClassLoader());
		apiPaths = in.createStringArray();
		formatString = in.readString();
		table = in.readString();
		primaryIdIndex = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(selection, flags);
		dest.writeStringArray(apiPaths);
		dest.writeString(formatString);
		dest.writeString(table);
		dest.writeInt(primaryIdIndex);
	}

	public static final Parcelable.Creator<CollectionFilter> CREATOR = new Parcelable.Creator<CollectionFilter>() {
		public CollectionFilter createFromParcel(Parcel in) {
			return new CollectionFilter(in);
		}

		public CollectionFilter[] newArray(int size) {
			return new CollectionFilter[size];
		}
	};

}
