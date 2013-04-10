package cat.mobilejazz.database.content;

import java.io.IOException;

import org.apache.http.auth.AuthenticationException;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public interface DataAdapter {

	public static interface DataAdapterListener {

		public void onDataEntry(String table, int depth, ContentValues data);

	}

	public void process(Context context, Account account, String table, String apiPath, DataAdapterListener listener,
			Cursor localData, Cursor pendingChanges) throws IOException, AuthenticationException;

	public void cancel();

	public boolean isCancelled();

}
