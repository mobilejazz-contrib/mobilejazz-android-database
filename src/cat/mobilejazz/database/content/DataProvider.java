package cat.mobilejazz.database.content;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import cat.mobilejazz.database.Column;
import cat.mobilejazz.database.Database;
import cat.mobilejazz.database.ProgressListener;
import cat.mobilejazz.database.SQLUtils;
import cat.mobilejazz.database.Storage;
import cat.mobilejazz.database.Table;
import cat.mobilejazz.database.View;
import cat.mobilejazz.database.content.DataProcessor.DatabaseUpdateListener;
import cat.mobilejazz.utilities.CompatibilityUtils;
import cat.mobilejazz.utilities.debug.Debug;

/**
 * A generic implementation of a content provider. It allows to track change
 * history by employing a separate table {@code CHANGES}.
 * 
 * All uris are of the form: {@code content://<authority>/
 * <table>[/<id>][?<parameters>} where the possible uri parameters are the
 * following:
 * <ul>
 * <li>Record query changes ({@link #QUERY_KEY_RECORD_CHANGES}, {@link boolean}
 * ): Defines whether or not the changes to the database should be automatically
 * recorded as a change in the respective table.</li>
 * <li>Notify observers of this change ({@link #QUERY_KEY_NOTIFY}, {@link
 * boolean}): Defines whether or not data observers should be informed of the
 * changes to the database that are a result of this sql statement.</li>
 * <li>Insert or update ({@link #QUERY_KEY_INSERT_OR_UPDATE}, {@link boolean}):
 * Applies only to insert statements. If it is set to {@code true}, the insert
 * will attempt to replace an existing entry if there is already a row in the
 * table that has the same id.</li>
 * <li>Group by ({@link #QUERY_KEY_GROUP_BY}, {@link String}): Since GROUP BY is
 * not part of the content provider interface, this parameter allows to define
 * an arbitrary SQL GROUP BY suffix. Note that you do not need to specify
 * {@code "GROUP BY"} as part of the {@link String}.</li>
 * <li>Custom action ({@link #QUERY_KEY_ACTION}, {@link String}): Allows to
 * define a custom action for the change that is to be recorded. If this is not
 * specified, the action will be analogous to the type of the sql statement
 * (i.e. create, replace, delete)</li>
 * <li>Custom change value ({@link #QUERY_KEY_CHANGE_VALUE}, {@link String}):
 * Allows to define a custom set of values that should be uploaded to the
 * server. This is typically a JSON string, but it can be any {@link String}
 * that is understood by the class that processes uploads. If this is not
 * specified, the TODO</li>
 * <li>Custom info ({@link #QUERY_KEY_ADDITIONAL_DATA}, {@link String}): Allows
 * to define a custom set of auxiliary info values that are not directly pushed
 * to the server, but may help in estimating server apis and defining the
 * packet. If this is not specified, the TODO</li>
 * </ul>
 **/
public abstract class DataProvider extends ContentProvider {

	public static final String QUERY_KEY_RECORD_CHANGES = "rch";
	public static final String QUERY_FALSE = "0";
	public static final String QUERY_TRUE = "1";

	public static final String QUERY_KEY_CHANGE_VALUE = "chv";

	public static final String QUERY_KEY_ACTION = "act";

	public static final String QUERY_KEY_NOTIFY = "not";

	public static final String QUERY_KEY_GROUP_BY = "gby";

	public static final String QUERY_KEY_ADDITIONAL_DATA = "dep";

	public static final String QUERY_KEY_INSERT_OR_UPDATE = "iou";

	private static SimpleDateFormat debugDateFormat = new SimpleDateFormat("HH:mm:ss.S");

	private static class Notification {
		private Uri uri;
		private ResolvedUri resolvedUri;

		public Notification(Uri uri, ResolvedUri resolvedUri) {
			this.uri = uri;
			this.resolvedUri = resolvedUri;
		}
	}

	protected class ResolvedUri {
		private String table;
		private Account user;
		private Long id;
		Bundle queryParams;

		public ResolvedUri(String table, String user, Long id, Bundle queryParams) {
			this.table = table;
			this.user = new Account(user, getAccountType());
			this.id = id;
			this.queryParams = queryParams;
		}

		public ResolvedUri(String table, String user, Bundle queryParams) {
			this(table, user, null, queryParams);
		}

		public String extendSelection(String selection) {
			// TODO make this more general. Replace _ID by the
			// primary key column name (in most cases it is _ID anyway).
			if (id != null) {
				if (TextUtils.isEmpty(selection))
					return "_ID = " + id;
				else
					return selection + " AND _ID = " + id;
			} else {
				return selection;
			}
		}

		public String getString(String queryKey) {
			return queryParams.getString(queryKey);
		}

		public boolean getBoolean(String queryKey) {
			return queryParams.getBoolean(queryKey);
		}

		public int getInt(String queryKey) {
			return queryParams.getInt(queryKey);
		}

		public long getLong(String queryKey) {
			return queryParams.getLong(queryKey);
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	private static final int ROW_URI = 0;
	private static final int TABLE_URI = 1;

	protected Uri getUri(String user, String table) {
		return new Uri.Builder().scheme("content").authority(getAuthority()).appendPath(user).appendPath(table).build();
	}

	protected Uri getUri(Account user, String table) {
		return new Uri.Builder().scheme("content").authority(getAuthority()).appendPath(user.name).appendPath(table)
				.build();
	}

	public static Uri withParams(Uri uri, boolean recordChange) {
		String queryValue = (recordChange) ? QUERY_TRUE : QUERY_FALSE;
		return uri.buildUpon().appendQueryParameter(QUERY_KEY_RECORD_CHANGES, queryValue).build();
	}

	public static Uri withParams(Uri uri, long id, boolean recordChange) {
		return ContentUris.withAppendedId(withParams(uri, recordChange), id);
	}

	/**
	 * A UriMatcher instance
	 */
	private UriMatcher sUriMatcher;

	private Map<String, SQLiteOpenHelper> mDatabaseHelpers;
	private Map<Uri, List<Uri>> mDependencies;

	private Map<String, String[]> mUIDs;

	private Boolean mAggregateNotifications = false;
	private List<Notification> mNotifications;

	protected abstract Database getDatabase();

	protected abstract String getAccountType();

	protected abstract String getAuthority();

	protected abstract SQLiteOpenHelper getDatabaseHelper(Context context, Account account, String dbName);

	protected abstract DataAdapter newDataAdapter();

	protected abstract JSONObject renderValues(ContentValues values, String table, int storageClass)
			throws JSONException;

	protected String getDatabaseId(Account account) {
		return account.name;
	}

	@Override
	public boolean onCreate() {
		mDatabaseHelpers = new HashMap<String, SQLiteOpenHelper>();
		mDependencies = new WeakHashMap<Uri, List<Uri>>();
		mNotifications = new ArrayList<Notification>();

		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(getAuthority(), "*/*/#", ROW_URI);
		sUriMatcher.addURI(getAuthority(), "*/*", TABLE_URI);

		mUIDs = new HashMap<String, String[]>();
		Database db = getDatabase();
		for (Table t : db.getTables()) {
			List<String> uids = new ArrayList<String>();
			for (Column c : t.getColumns()) {
				if (c.isUID()) {
					uids.add(c.getName());
				}
			}
			mUIDs.put(t.getName(), uids.toArray(new String[] {}));
		}

		return true;
	}

	private SQLiteOpenHelper getDatabaseHelper(Account account) {
		synchronized (mDatabaseHelpers) {
			String databaseId = getDatabaseId(account);
			SQLiteOpenHelper dbHelper = mDatabaseHelpers.get(databaseId);
			if (dbHelper == null) {
				dbHelper = getDatabaseHelper(getContext(), account, databaseId);
				mDatabaseHelpers.put(databaseId, dbHelper);
			}
			return dbHelper;
		}
	}

	private SQLiteDatabase getReadableDatabase(Account account) {
		return getDatabaseHelper(account).getReadableDatabase();
	}

	private SQLiteDatabase getWritableDatabase(Account account) {
		return getDatabaseHelper(account).getWritableDatabase();
	}

	public List<Uri> getDependencies(Uri uri, ResolvedUri resolvedUri) {
		Uri baseUri = getUri(resolvedUri.user, resolvedUri.table);
		List<Uri> dep = mDependencies.get(baseUri);
		if (dep != null) {
			return dep;
		} else {
			List<Uri> result = new ArrayList<Uri>();
			Table table = getDatabase().getTableOrThrow(resolvedUri.table);
			for (View v : table.getReferencedBy()) {
				baseUri = getUri(resolvedUri.user, v.getName());
				result.add(baseUri);
			}
			mDependencies.put(uri, result);
			return result;
		}
	}

	private boolean getBooleanQueryParameter(Uri uri, String key, boolean defaultValue) {
		String value = uri.getQueryParameter(key);
		if (value == null) {
			return defaultValue;
		} else {
			return !value.equals(QUERY_FALSE);
		}
	}

	private int getIntegerQueryParameter(Uri uri, String key, int defaultValue) {
		String value = uri.getQueryParameter(key);
		if (value == null) {
			return defaultValue;
		} else {
			return Integer.parseInt(value);
		}
	}

	private long getLongQueryParameter(Uri uri, String key, long defaultValue) {
		String value = uri.getQueryParameter(key);
		if (value == null) {
			return defaultValue;
		} else {
			return Integer.parseInt(value);
		}
	}

	/**
	 * Translates the given {@link Uri} to the corresponding table or view name.
	 * 
	 * @param uri
	 *            The content uri.
	 * @return A table or view name that corresponds to the given content
	 *         {@link Uri}.
	 */
	protected ResolvedUri resolveUri(Uri uri) {
		// boolean recordChanges =
		Bundle queryParams = new Bundle();
		queryParams.putBoolean(QUERY_KEY_RECORD_CHANGES, getBooleanQueryParameter(uri, QUERY_KEY_RECORD_CHANGES, true));
		queryParams.putString(QUERY_KEY_CHANGE_VALUE, uri.getQueryParameter(QUERY_KEY_CHANGE_VALUE));
		queryParams.putBoolean(QUERY_KEY_NOTIFY, getBooleanQueryParameter(uri, QUERY_KEY_NOTIFY, true));
		queryParams.putInt(QUERY_KEY_ACTION, getIntegerQueryParameter(uri, QUERY_KEY_ACTION, -1));
		queryParams.putString(QUERY_KEY_GROUP_BY, uri.getQueryParameter(QUERY_KEY_GROUP_BY));
		queryParams.putString(QUERY_KEY_ADDITIONAL_DATA, uri.getQueryParameter(QUERY_KEY_ADDITIONAL_DATA));
		queryParams.putBoolean(QUERY_KEY_INSERT_OR_UPDATE,
				getBooleanQueryParameter(uri, QUERY_KEY_INSERT_OR_UPDATE, false));

		List<String> pathSegments = uri.getPathSegments();
		if (sUriMatcher.match(uri) == TABLE_URI) {
			return new ResolvedUri(pathSegments.get(1), pathSegments.get(0), queryParams);
		} else {
			long id = ContentUris.parseId(uri);
			return new ResolvedUri(pathSegments.get(1), pathSegments.get(0), id, queryParams);
		}
	}

	protected void notifyChange(Uri uri, ResolvedUri resolvedUri) {
		if (resolvedUri.getBoolean(QUERY_KEY_NOTIFY) && !mAggregateNotifications) {
			Debug.verbose("%s: Notifying for %s", Thread.currentThread().getName(), uri);
			getContext().getContentResolver().notifyChange(uri, null, resolvedUri.getBoolean(QUERY_KEY_RECORD_CHANGES));
			for (Uri depUri : getDependencies(uri, resolvedUri)) {
				Debug.verbose("%s: Notifying for %s", Thread.currentThread().getName(), depUri);
				getContext().getContentResolver().notifyChange(depUri, null,
						resolvedUri.getBoolean(QUERY_KEY_RECORD_CHANGES));
			}
		} else if (mAggregateNotifications) {
			mNotifications.add(new Notification(uri, resolvedUri));
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		ResolvedUri resolvedUri = resolveUri(uri);
		SQLiteDatabase db = getReadableDatabase(resolvedUri.user);
		Cursor cursor = db.query(resolvedUri.table, projection, resolvedUri.extendSelection(selection), selectionArgs,
				resolvedUri.getString(QUERY_KEY_GROUP_BY), null, sortOrder);

		// if (resolvedUri.table.equals(Changes.TABLE_NAME)) {
		// Debug.verbose("%s - Query[%d]: %s, %s, %s, %s, %s",
		// Thread.currentThread().getName(), cursor.getCount(),
		// uri, Arrays.toString(projection), selection,
		// Arrays.toString(selectionArgs), sortOrder);
		// } else {
		// Debug.debug("%s - Query[%d]: %s, %s, %s, %s, %s",
		// Thread.currentThread().getName(), cursor.getCount(), uri,
		// Arrays.toString(projection), selection,
		// Arrays.toString(selectionArgs), sortOrder);
		// }

		// try {
		// if (resolvedUri.table.equals("projects"))
		// Thread.sleep(5000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	/**
	 * Gets the name of the column that stores the id that should be entered in
	 * the object id field of the changes table. Subclasses may override this to
	 * for example use a server id instead of the local sqlite id. If this
	 * method returns {@code BaseColumns._ID} the "native" id field is used. The
	 * native id field is the one that is defined as the primary key of the
	 * table and is also used when appending ids in uris.
	 * 
	 * @param table
	 *            the table
	 * @return the native id field {@code BaseColumns._ID} by default.
	 */
	protected String getChangeIdColumn(String table) {
		return BaseColumns._ID;
	}

	protected abstract String getCreationDateColumn(String table);

	protected ContentValues getChanges(SQLiteDatabase db, int action, String table, long id, ContentValues values,
			String customChangeValue, String additionalData) {
		try {
			String json = customChangeValue;
			if (json == null) {
				JSONObject obj = renderValues(values, table, Storage.REMOTE);
				json = obj.toString();
			}

			ContentValues changes = new ContentValues();

			String changeIdColumn = getChangeIdColumn(table);
			long objId = id;
			if (!changeIdColumn.equals(BaseColumns._ID)) {
				if (values != null && values.containsKey(changeIdColumn)) {
					objId = values.getAsLong(changeIdColumn);
				} else if (id != 0L) {
					Cursor c = db.query(table, new String[] { changeIdColumn }, "_id = ?",
							new String[] { String.valueOf(id) }, null, null, null);
					try {
						c.moveToFirst();
						if (!c.isAfterLast()) {
							objId = c.getLong(0);
						} else {
							return null;
						}
					} finally {
						c.close();
					}
				}
			}

			changes.put(Changes.COLUMN_TABLE, table);
			changes.put(Changes.COLUMN_NATIVE_ID, id);
			changes.put(Changes.COLUMN_ID, objId);
			changes.put(Changes.COLUMN_ACTION, action);
			changes.put(Changes.COLUMN_TIMESTAMP, SQLUtils.formatTimestamp(new Date()));
			changes.put(Changes.COLUMN_VALUES, json);
			// changes.put(Changes.COLUMN_API_PATH, apiPath);
			JSONObject info = renderValues(values, table, Storage.INFO);
			if (additionalData != null) {
				try {
					JSONObject addData = new JSONObject(additionalData);
					Iterator<String> additionalDataIterator = addData.keys();
					while (additionalDataIterator.hasNext()) {
						String key = additionalDataIterator.next();
						info.put(key, addData.get(key));
					}
				} catch (JSONException e) {
					// additionalData is in a non-valid format: give a warning
					Debug.warning(String
							.format("Additional data needs to be in a JSON valid formatting. Found instead: %s",
									additionalData));
				}
			}
			changes.put(Changes.COLUMN_ADDITIONAL_DATA, info.toString());
			return changes;
		} catch (JSONException e) {
			Debug.logException(e);
		}
		return null;
	}

	// protected void updateGroupId(SQLiteDatabase db, long id) {
	// if (mGroupId == null) {
	// ContentValues groupIdValues = new ContentValues();
	// groupIdValues.put(Changes.COLUMN_GROUP_ID, id);
	// db.update(Changes.TABLE_NAME, groupIdValues, Changes._ID + " = ?",
	// new String[] { String.valueOf(id) });
	// if (mAggregateNotifications) {
	// mGroupId = id;
	// }
	// }
	// }

	protected void insertSingleChange(SQLiteDatabase db, int action, long id, ResolvedUri resolvedUri,
			ContentValues values) {
		ContentValues changes = getChanges(db, action, resolvedUri.table, id, values,
				resolvedUri.getString(QUERY_KEY_CHANGE_VALUE), resolvedUri.getString(QUERY_KEY_ADDITIONAL_DATA));
		if (changes != null) {
			db.insert(Changes.TABLE_NAME, null, changes);
		}
	}

	protected void insertChanges(int action, SQLiteDatabase db, Uri uri, ResolvedUri resolvedUri, String selection,
			String[] selectionArgs, ContentValues values) {
		if (resolvedUri.queryParams.getBoolean(QUERY_KEY_RECORD_CHANGES)
				&& !getDatabase().getTable(resolvedUri.table).isLocal()) {

			int customAction = resolvedUri.getInt(QUERY_KEY_ACTION);
			if (customAction >= 0) {
				// overwrite default action:
				action = customAction;
			}

			if (resolvedUri.id != null) {
				insertSingleChange(db, action, resolvedUri.id, resolvedUri, values);
			} else if (action == Changes.ACTION_REMOVE) {
				insertSingleChange(db, action, 0, resolvedUri, values);
			} else {
				Cursor cursor = query(uri, new String[] { getChangeIdColumn(resolvedUri.table) }, selection,
						selectionArgs, null);
				try {
					cursor.moveToFirst();
					while (!cursor.isAfterLast()) {
						long id = cursor.getLong(0);
						insertSingleChange(db, action, id, resolvedUri, values);
						cursor.moveToNext();
					}
				} finally {
					cursor.close();
				}
			}
		}
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * When value of a UID when it is requested the first time for a particular
	 * table-column pair. This may be overridden by subclasses to achieve some
	 * desired behavior.
	 * 
	 * @return {@code 0} in the default implementation.
	 */
	protected long firstUIDValue() {
		return 0L;
	}

	/**
	 * Gets the next UID value provided the last one. This may be overridden by
	 * subclasses to achieve some desired behavior. Keep in mind, however, that
	 * the generated sequence of UIDs need to be distinct.
	 * 
	 * @param value
	 *            the last generated UID.
	 * @return {@code value+1} in the default implementation.
	 */
	protected long nextUIDValue(long value) {
		return value + 1;
	}

	public synchronized long newUID(String table, String column) {
		SharedPreferences pref = getContext().getSharedPreferences("uid", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		long maxValue = pref.getLong(table + ":" + column, firstUIDValue());
		editor.putLong(table + ":" + column, nextUIDValue(maxValue)).commit();
		return maxValue;
	}

	private void fillUIDs(String table, ContentValues values) {
		String[] uids = mUIDs.get(table);
		for (String uidColumn : uids) {
			if (!values.containsKey(uidColumn)) {
				values.put(uidColumn, newUID(table, uidColumn));
			}
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Debug.info("%s - Inserting %s with %s", Thread.currentThread().getName(), uri, values);
		ResolvedUri resolvedUri = resolveUri(uri);
		Log.i("T - " + Thread.currentThread().getName(), ">> INSERT: " + resolvedUri.table);

		if (resolvedUri.id != null) {
			throw new IllegalArgumentException("Invalid content uri for insert: " + uri);
		}
		long rowId = 0;

		fillUIDs(resolvedUri.table, values);

		// if (values.containsKey(BaseColumns._ID)) {
		// throw new RuntimeException();
		// }

		SQLiteDatabase db = getWritableDatabase(resolvedUri.user);
		db.beginTransaction();
		try {
			if (resolvedUri.getBoolean(QUERY_KEY_INSERT_OR_UPDATE)) {
				rowId = db.insertWithOnConflict(resolvedUri.table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			} else {
				rowId = db.insert(resolvedUri.table, null, values);
			}
			resolvedUri.setId(rowId);
			insertChanges(Changes.ACTION_CREATE, db, uri, resolvedUri, null, null, values);
			if (rowId >= 0) {
				notifyChange(uri, resolvedUri);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		Debug.info("%s - Inserted rowId: %d", Thread.currentThread().getName(), rowId);
		Log.i("T - " + Thread.currentThread().getName(), "<< INSERT: " + resolvedUri.table);
		return ContentUris.withAppendedId(uri, rowId);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Debug.info("%s - Deleting %s (%s, %s)", Thread.currentThread().getName(), uri, selection,
				Arrays.toString(selectionArgs));

		ResolvedUri resolvedUri = resolveUri(uri);
		Log.i("T - " + Thread.currentThread().getName(), ">> DELETE: " + resolvedUri.table);
		SQLiteDatabase db = getWritableDatabase(resolvedUri.user);
		int deletedRows = 0;
		db.beginTransaction();
		try {
			insertChanges(Changes.ACTION_REMOVE, db, uri, resolvedUri, selection, selectionArgs, null);
			deletedRows = db.delete(resolvedUri.table, resolvedUri.extendSelection(selection), selectionArgs);
			if (deletedRows > 0) {
				notifyChange(uri, resolvedUri);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		Debug.info("%s - Deleted %d rows.", Thread.currentThread().getName(), deletedRows);
		Log.i("T - " + Thread.currentThread().getName(), "<< DELETE: " + resolvedUri.table);
		return deletedRows;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Debug.info("%s - Updating %s (%s, %s) with %s", Thread.currentThread().getName(), uri, selection,
				Arrays.toString(selectionArgs), values);
		ResolvedUri resolvedUri = resolveUri(uri);
		Log.i("T - " + Thread.currentThread().getName(), ">> UPDATE: " + resolvedUri.table);
		SQLiteDatabase db = getWritableDatabase(resolvedUri.user);
		int updatedRows = 0;
		db.beginTransaction();
		try {
			insertChanges(Changes.ACTION_UPDATE, db, uri, resolvedUri, selection, selectionArgs, values);
			updatedRows = db.update(resolvedUri.table, values, resolvedUri.extendSelection(selection), selectionArgs);
			if (updatedRows > 0) {
				notifyChange(uri, resolvedUri);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		Debug.info("%s - Updated %d rows", Thread.currentThread().getName(), updatedRows);
		Log.i("T - " + Thread.currentThread().getName(), "<< UPDATE: " + resolvedUri.table);
		return updatedRows;
	}

	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		mAggregateNotifications = true;
		ContentProviderResult[] result = new ContentProviderResult[operations.size()];
		int i = 0;
		for (ContentProviderOperation o : operations) {
			// Set<String> queryParams = o.getUri().getQueryParameterNames();
			/*
			 * TODO; set the no-notification flag. Then aggregate notifications
			 * here and don't use a member because of thread safety.
			 */
			result[i] = o.apply(this, result, i++);
		}
		mAggregateNotifications = false;
		for (Notification n : mNotifications) {
			notifyChange(n.uri, n.resolvedUri);
		}
		mNotifications.clear();
		return result;
	}

	public ContentProviderResult[] applyBatch(Account account, ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		SQLiteDatabase db = getWritableDatabase(account);
		ContentProviderResult[] result = new ContentProviderResult[operations.size()];
		int i = 0;
		db.beginTransaction();
		try {

			for (ContentProviderOperation op : operations) {
				op.apply(this, result, i++);
			}

			db.setTransactionSuccessful();
			return result;
		} finally {
			db.endTransaction();
		}
	}

	public void beginTransaction(Account account) {
		Debug.info("BEGIN TRANSACTION: %s", Thread.currentThread().getName());
		getWritableDatabase(account).beginTransaction();
		synchronized (mAggregateNotifications) {
			mAggregateNotifications = true;
		}
	}

	public void setTransactionSuccessful(Account account) {
		Debug.info("SET TRANSACTION SUCCESSFUL: %s", Thread.currentThread().getName());
		synchronized (mAggregateNotifications) {
			mAggregateNotifications = false;
			for (Notification n : mNotifications) {
				notifyChange(n.uri, n.resolvedUri);
			}
			mNotifications.clear();
		}
		getWritableDatabase(account).setTransactionSuccessful();
	}

	public void endTransaction(Account account) {
		Debug.info("END TRANSACTION: %s", Thread.currentThread().getName());
		synchronized (mAggregateNotifications) {
			mAggregateNotifications = false;
		}
		getWritableDatabase(account).endTransaction();
	}

	public void beginTransaction(Account account, SQLiteTransactionListener listener) {
		Debug.info("BEGIN TRANSACTION: %s", Thread.currentThread().getName());
		if (listener != null) {
			getWritableDatabase(account).beginTransactionWithListener(listener);
		} else {
			beginTransaction(account);
		}
	}

	private static class UpdateOperation {

		private DataProcessor processor;
		private DataAdapter adapter;

	}

	private static class UpdateKey {

		private String database;
		private CollectionFilter filter;

		public UpdateKey(String database, CollectionFilter filter) {
			this.database = database;
			this.filter = filter;
		}

		@Override
		public boolean equals(Object o) {
			try {
				if (o == null) {
					return false;
				}

				UpdateKey uk = (UpdateKey) o;
				return database.equals(uk.database) && filter.equals(uk.filter);

			} catch (ClassCastException e) {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return database.hashCode() + filter.hashCode();
		}

	}

	private ConcurrentHashMap<UpdateKey, UpdateOperation> mUpdates = new ConcurrentHashMap<UpdateKey, UpdateOperation>();

	public void cancelUpdate(Account account, CollectionFilter filter) {
		Debug.info("Attempting to cancel");
		UpdateOperation uop = mUpdates.get(new UpdateKey(getDatabaseId(account), filter));
		if (uop != null) {
			Debug.info("Cancelling %s", filter);
			uop.adapter.cancel();
			uop.processor.cancel();
		}
	}

	/**
	 * Downloads data from the server and updates the local cache.
	 * 
	 * @param account
	 * @param filter
	 * @param listener
	 * @param expectedCount
	 * @return A {@link Collection} of aborted api paths.
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Collection<String> updateFromServer(Account account, CollectionFilter filter, ProgressListener listener,
			long expectedCount, DatabaseUpdateListener updateListener) throws IOException, AuthenticationException {

		Collection<String> result = new HashSet<String>();

		Debug.info(String.format("%s - updating from reader: %s, %s", Thread.currentThread().getName(), account.name,
				filter));

		SQLiteDatabase db = getWritableDatabase(account);

		UpdateOperation uop = new UpdateOperation();
		uop.processor = new DataProcessor(this, account.name, db, listener, filter.getTable(), expectedCount,
				filter.getSelect(), updateListener);
		uop.adapter = newDataAdapter();

		UpdateKey upkey = new UpdateKey(getDatabaseId(account), filter);

		UpdateOperation old = mUpdates.putIfAbsent(upkey, uop);
		if (old != null) {
			return result; // reject two updates with the same filter
		}

		Date startTime = new Date();

		try {
			for (String apiPath : filter.getApiPaths()) {
				uop.adapter.process(filter.getTable(), apiPath, uop.processor, null, null);
				if (uop.adapter.isCancelled()) {
					result.add(apiPath);
					break;
				}
			}
			CompatibilityUtils.beginTransactionNonExclusive(db);
			try {
				if (!uop.processor.isCancelled()) {
					uop.processor.performOperations(startTime);
					db.setTransactionSuccessful();

					if (uop.processor.isCancelled()) {
						for (String apiPath : filter.getApiPaths()) {
							result.add(apiPath);
						}
					}
				}

				return result;
			} finally {
				db.endTransaction();
				uop.processor.notifyChanges();
			}
		} finally {
			mUpdates.remove(upkey);
		}

	}
}
