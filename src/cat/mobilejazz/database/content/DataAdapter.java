package cat.mobilejazz.database.content;

import java.io.IOException;

import org.apache.http.auth.AuthenticationException;

import android.content.ContentValues;
import cat.mobilejazz.database.Table;

public interface DataAdapter {

	public static interface DataAdapterListener {

		public void onDataEntry(Table table, int depth, ContentValues data);

		public void onEmptyTable(Table table);

	}

	/**
	 * 
	 * Downloads the data from the given api path and generates events on the
	 * specified {@link DataAdapterListener}.
	 * 
	 * @param table
	 *            The database table this api call corresponds to.
	 * @param apiPath
	 *            The api path on the server.
	 * @param listener
	 *            A {@link DataAdapterListener} that is notified when data
	 *            arrives.
	 * @return {@code true}, if data has been received, {@code false} otherwise.
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public DataResult process(String table, String apiPath, DataAdapterListener listener) throws IOException,
			AuthenticationException;

	public void cancel();

	public boolean isCancelled();

}
