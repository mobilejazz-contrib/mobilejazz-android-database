package cat.mobilejazz.database.content;

import android.content.ContentValues;

/**
 * A listener to monitor the {@link DataProvider}s internal changes table in
 * realtime.
 * 
 * @author Hannes Widmoser
 * 
 */
public interface ChangesListener {

	public void onInsertChange(ContentValues change);
	
}
