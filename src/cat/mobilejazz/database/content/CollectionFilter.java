package cat.mobilejazz.database.content;

import java.util.Arrays;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
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
		table = selection.getTable();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(selection, flags);
		dest.writeStringArray(apiPaths);
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
