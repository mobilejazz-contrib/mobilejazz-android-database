package cat.mobilejazz.database.content;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;
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

	private String mTable;
	private String mSelection;
	private String[] mSelectionArgs;
	private String[] mApiPaths;

	/**
	 * Create a new collection filter.
	 * 
	 * @param table
	 *            The table.
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
	public CollectionFilter(String table, String selection, String[] selectionArgs, String[] apiPaths) {
		mTable = table;
		mSelection = selection;
		mSelectionArgs = selectionArgs;
		mApiPaths = apiPaths;
	}

	/**
	 * Gets the table that is affected by this filter. Please refer to
	 * {@link DatabaseContract} for a list of possible values (e.g.
	 * {@code DatabaseContract.Tasks.TABLE_NAME}).
	 * 
	 * @return The table of this filter.
	 */
	public String getTable() {
		return mTable;
	}

	/**
	 * An SQLite compatible selection WHERE clause string. See
	 * {@link DataProvider#query(android.net.Uri, String[], String, String[], String)}
	 * for details.
	 * 
	 * @return The selection of this filter.
	 */
	public String getSelection() {
		return mSelection;
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
		return mSelectionArgs;
	}

	/**
	 * Possibly multiple api paths that together are an equivalent filter as
	 * provided by the other parameters.
	 * 
	 * @return The api path of this filter.
	 */
	public String[] getApiPaths() {
		return mApiPaths;
	}

	@Override
	public boolean equals(Object o) {
		try {
			CollectionFilter other = (CollectionFilter) o;
			return (ObjectUtils.equals(other.mTable, mTable) && ObjectUtils.equals(other.mSelection, mSelection)
					&& Arrays.equals(other.mSelectionArgs, mSelectionArgs) && Arrays.equals(other.mApiPaths, mApiPaths));
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return String.format("%s, %s, %s, %s", Arrays.toString(mApiPaths), mTable, mSelection,
				Arrays.toString(mSelectionArgs));
	}

	private CollectionFilter(Parcel in) {
		mTable = in.readString();
		mSelection = in.readString();
		mSelectionArgs = in.createStringArray();
		mApiPaths = in.createStringArray();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mTable);
		dest.writeString(mSelection);
		dest.writeStringArray(mSelectionArgs);
		dest.writeStringArray(mApiPaths);
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
