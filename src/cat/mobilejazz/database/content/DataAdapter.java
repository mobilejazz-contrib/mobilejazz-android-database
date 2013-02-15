package cat.mobilejazz.database.content;

import java.io.IOException;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public interface DataAdapter {

	public static interface DataAdapterListener {

		public void onDataEntry(String table, ContentValues data);

	}

	public void process(Context context, Account account, String table, String apiPath, DataAdapterListener listener,
			Cursor localData, Cursor pendingChanges) throws IOException, AuthenticationException;

	public JSONObject renderValues(ContentValues values, String table, int storageClass) throws JSONException;

}
