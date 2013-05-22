package cat.mobilejazz.database.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import cat.mobilejazz.database.ProgressListener;
import cat.mobilejazz.database.SQLUtils;
import cat.mobilejazz.database.content.DataAdapter.DataAdapterListener;
import cat.mobilejazz.database.content.DataProvider.ResolvedUri;
import cat.mobilejazz.database.query.Select;
import cat.mobilejazz.utilities.collections.CachedIterator;
import cat.mobilejazz.utilities.debug.Debug;

/* TODO: maybe outsource to tb project? */
public class DataProcessor implements DataAdapterListener {

	/**
	 * After the data processor has updated the database, this interface allows
	 * the UI to be notified of certain changes.
	 * 
	 * It has three methods that allow to narrow down the number of events, this
	 * listener is informed of. Currently only REMOVE operations are supported.
	 * 
	 * @author Hannes Widmoser
	 * 
	 */
	public static class DatabaseUpdateListener implements Parcelable {

		public static final String KEY_TABLE = "table";
		public static final String KEY_SERVER_ID = "server_id";

		private String table;
		private Long serverId;
		private Integer type;
		private Messenger messenger;

		/**
		 * Creates a new listener.
		 * 
		 * @param table
		 *            The table this listener is interested in or {@code null}
		 *            for all tables.
		 * @param serverId
		 *            The id of the item this listener is interested in or
		 *            {@code null} for all items.
		 * @param type
		 *            The type of change this listener is interested in or
		 *            {@code null} for all types. This must be one of
		 *            {@link Changes#ACTION_CREATE},
		 *            {@link Changes#ACTION_REMOVE},
		 *            {@link Changes#ACTION_UPDATE}.
		 * @param messenger
		 *            A {@link Messenger} that gets notified of changes that
		 *            match the filter criteria. The messenger receives a
		 *            message where {@code what} refers to the type, and the
		 *            bundle's {@link #KEY_TABLE}, {@link #KEY_SERVER_ID} refer
		 *            to table and server id respectively.
		 */
		public DatabaseUpdateListener(String table, Long serverId, Integer type, Messenger messenger) {
			super();
			this.table = table;
			this.serverId = serverId;
			this.type = type;
		}

		/**
		 * @return The table this listener is interested in or {@code null} for
		 *         all tables.
		 */
		public String getTable() {
			return table;
		}

		/**
		 * @return The id of the item this listener is interested in or
		 *         {@code null} for all items.
		 */
		public Long getServerId() {
			return serverId;
		}

		/**
		 * @return The type of change this listener is interested in or
		 *         {@code null} for all types. This must be one of
		 *         {@link Changes#ACTION_CREATE}, {@link Changes#ACTION_REMOVE},
		 *         {@link Changes#ACTION_UPDATE}.
		 */
		public Integer getType() {
			return type;
		}

		/**
		 * Notifies the listener of a change in the database due to the last
		 * action of the respective processor.
		 * 
		 * @param type
		 *            The type of change in the database. Must be one of
		 *            {@link Changes#ACTION_CREATE},
		 *            {@link Changes#ACTION_REMOVE},
		 *            {@link Changes#ACTION_UPDATE}.
		 * @param table
		 *            The table that was affected by the change.
		 * @param serverId
		 *            The serverId of the item that was affected.
		 */
		public void onDatabaseChange(int type, String table, long serverId) {
			Message msg = Message.obtain();
			msg.what = type;
			msg.getData().putString(KEY_TABLE, table);
			msg.getData().putLong(KEY_SERVER_ID, serverId);
			try {
				messenger.send(msg);
			} catch (RemoteException e) {
			}
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(type);
			dest.writeString(table);
			dest.writeLong(serverId);
			dest.writeParcelable(messenger, flags);
		}

		private DatabaseUpdateListener(Parcel in) {
			type = in.readInt();
			table = in.readString();
			serverId = in.readLong();
			messenger = in.readParcelable(getClass().getClassLoader());
		}

		public static final Parcelable.Creator<DatabaseUpdateListener> CREATOR = new Parcelable.Creator<DatabaseUpdateListener>() {
			public DatabaseUpdateListener createFromParcel(Parcel in) {
				return new DatabaseUpdateListener(in);
			}

			public DatabaseUpdateListener[] newArray(int size) {
				return new DatabaseUpdateListener[size];
			}
		};

	}

	// private static final class DatabaseUpdateListenerFilter {
	//
	// private String table;
	// private Long serverId;
	// private Integer type;
	//
	// public DatabaseUpdateListenerFilter(String table, Long serverId, Integer
	// type) {
	// this.table = table;
	// this.serverId = serverId;
	// this.type = type;
	// }
	//
	// @Override
	// public boolean equals(Object o) {
	// if (o == null)
	// return false;
	// try {
	// DatabaseUpdateListenerFilter f = (DatabaseUpdateListenerFilter) o;
	// return ObjectUtils.equals(table, f.table) && ObjectUtils.equals(serverId,
	// f.serverId)
	// && ObjectUtils.equals(type, f.type);
	// } catch (ClassCastException e) {
	// return false;
	// }
	// }
	//
	// @Override
	// public int hashCode() {
	// return ((table != null) ? table.hashCode() : 0) + ((serverId != null) ?
	// serverId.hashCode() : 0)
	// + ((type != null) ? type.hashCode() : 0);
	// }
	//
	// }

	private SQLiteDatabase mDb;
	private String mUser;
	private Set<String> mAffectedTables;

	private ProgressListener mListener;
	private String mMainTable;
	private double mStepDownload;
	private double mProgress;
	private long mOperationsDone;
	private long mCount;
	private long mDownloadsDone;

	private Select mCurrentSelection;

	private boolean mCancelled;

	private LinkedHashMap<String, SortedSet<DataEntry>> mOperations;
	private Map<String, Integer> mDepthMap;

	private DataProvider provider;

	private DatabaseUpdateListener updateListener;

	public DataProcessor(DataProvider provider, String user, SQLiteDatabase db, ProgressListener listener,
			String mainTable, long expectedCount, Select currentSelection, DatabaseUpdateListener updateListener) {
		this.provider = provider;
		mDb = db;
		mUser = user;
		mAffectedTables = new HashSet<String>();
		mOperations = new LinkedHashMap<String, SortedSet<DataEntry>>();
		mDepthMap = new HashMap<String, Integer>();
		mListener = listener;
		if (expectedCount > 0) {
			mStepDownload = (2.0 / 3.0) / (double) expectedCount;
		} else {
			mStepDownload = 0.0;
		}
		mCount = 0;
		mDownloadsDone = 0;
		mOperationsDone = 0;
		mMainTable = mainTable;
		mCurrentSelection = currentSelection;

		this.updateListener = updateListener;
	}

	@SuppressLint("DefaultLocale")
	private String getSignature(String table, long id) {
		return String.format("%s:%d", table, id);
	}

	private final DataEntry NO_DATA = new DataEntry(Long.MAX_VALUE, null);

	private long getCurrentServerId(Cursor current) {
		if (!current.isAfterLast()) {
			return current.getLong(1);
		} else {
			return Long.MAX_VALUE;
		}
	}

	private void insertOrUpdate(String table, ContentValues values, String identifyingColumn, long identifyingValue) {
		try {
			long result = mDb.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			if (result < 0L) {
				// row exists already -> update:
				mDb.update(table, values, String.format("%s = %d", identifyingColumn, identifyingValue), null);
			}
		} catch (SQLiteConstraintException e) {
			Debug.error("Constraint error inserting into %s, %s", table, values);
		}
	}

	private boolean listenerIsInterestedIn(DatabaseUpdateListener l, int type, String table, long serverId) {
		return (l.getType() == null || l.getType() == type) && (l.getServerId() == null || l.getServerId() == serverId)
				&& (l.getTable() == null || table.equals(l.getTable()));
	}

	private void notifyUpdateListeners(int type, String table, long serverId) {
		if (updateListener != null && listenerIsInterestedIn(updateListener, type, table, serverId)) {
			updateListener.onDatabaseChange(type, table, serverId);
		}
	}

	public void performOperations(Date startTime) {

		if (!isCancelled()) {

			Set<String> pendingDeletes = new HashSet<String>();
			Cursor pendingChanges = mDb.query(Changes.TABLE_NAME, new String[] { Changes.COLUMN_TABLE,
					Changes.COLUMN_ID }, String.format("%s = ?", Changes.COLUMN_ACTION),
					new String[] { String.valueOf(Changes.ACTION_REMOVE) }, null, null, null);
			if (pendingChanges.getCount() > 0) {
				pendingChanges.moveToFirst();
				while (!pendingChanges.isAfterLast()) {
					pendingDeletes.add(getSignature(pendingChanges.getString(0), pendingChanges.getLong(1)));
					pendingChanges.moveToNext();
				}
			}
			pendingChanges.close();

			Debug.debug("Pending deletes: ");
			for (String s : pendingDeletes) {
				Debug.debug("\t" + s);
			}

			String serverIdColumn = provider.getChangeIdColumn(mCurrentSelection.getTable());
			String creationDateColumn = provider.getCreationDateColumn(mCurrentSelection.getTable());

			double step = (1.0 - mDownloadsDone * mStepDownload) / (double) mCount;

			List<Map.Entry<String, SortedSet<DataEntry>>> operations = new ArrayList<Map.Entry<String, SortedSet<DataEntry>>>(
					mOperations.entrySet());
			Collections.sort(operations, new Comparator<Map.Entry<String, SortedSet<DataEntry>>>() {

				@Override
				public int compare(Entry<String, SortedSet<DataEntry>> lhs, Entry<String, SortedSet<DataEntry>> rhs) {
					return mDepthMap.get(lhs.getKey()).compareTo(mDepthMap.get(rhs.getKey()));
				}

			});

			for (Map.Entry<String, SortedSet<DataEntry>> e : operations) {

				if (isCancelled()) {
					Debug.debug("cancelled");
					break;
				}

				String table = e.getKey();
				String changeIdColumn = provider.getChangeIdColumn(table);

				if (table.equals(mMainTable)) {

					String[] projection = !TextUtils.isEmpty(creationDateColumn) ? new String[] { BaseColumns._ID,
							serverIdColumn, creationDateColumn } : new String[] { BaseColumns._ID, serverIdColumn };
					Cursor current = mDb.query(mCurrentSelection.getTable(), projection,
							mCurrentSelection.getSelection(), mCurrentSelection.getSelectionArgs(), null, null,
							serverIdColumn);
					current.moveToFirst();
					Debug.debug("[%d, %d] Querying: %s", current.getCount(), e.getValue().size(), mCurrentSelection);

					CachedIterator<DataEntry> i = new CachedIterator<DataEntry>(e.getValue().iterator(), NO_DATA);

					while ((!current.isAfterLast() || !i.isAfterLast()) && !isCancelled()) {
						long currentServerId = getCurrentServerId(current);
						DataEntry entry = i.getValue();

						Debug.debug("%s: %d <--- %d", mMainTable, currentServerId, entry.serverId);

						if (entry.serverId == currentServerId) {
							// update:
							mDb.update(table, entry.values, "_id = ?",
									new String[] { String.valueOf(current.getLong(0)) });
							i.moveToNext();
							current.moveToNext();
							mOperationsDone++;
							mProgress += step;
							mListener.onProgress(
									String.format("Processing %s data %d/%d...", mMainTable, mOperationsDone, mCount),
									mProgress);
						} else if (entry.serverId < currentServerId) {
							// insert:
							if (!pendingDeletes.contains(getSignature(table, entry.values.getAsLong(changeIdColumn)))) {
								insertOrUpdate(table, entry.values, changeIdColumn, entry.serverId);
							}
							i.moveToNext();
							mOperationsDone++;
							mProgress += step;
							mListener.onProgress(
									String.format("Processing %s data %d/%d...", mMainTable, mOperationsDone, mCount),
									mProgress);
						} else {
							// delete:
							if (TextUtils.isEmpty(creationDateColumn)
									|| SQLUtils.getTimestamp(current, 2).before(startTime)) {
								notifyUpdateListeners(Changes.ACTION_REMOVE, table, current.getLong(1));
								mDb.delete(table, "_id = ?", new String[] { String.valueOf(current.getLong(0)) });
							}
							current.moveToNext();
						}
					}

					current.close();
				} else {
					// process non maintable entries
					for (DataEntry entry : e.getValue()) {
						if (isCancelled()) {
							Debug.debug("cancelled");
							break;
						}
						// no deletes are propagated along delegates:
						if (!pendingDeletes.contains(getSignature(table, entry.values.getAsLong(changeIdColumn)))) {
							insertOrUpdate(table, entry.values, changeIdColumn, entry.serverId);
						}
					}
				}
			}
		} else {
			Debug.debug("cancelled");
		}

		Debug.info("Operations done: " + mOperationsDone);
		mListener.onFinished();
	}

	@Override
	public void onDataEntry(String table, int depth, ContentValues data) {
		Debug.verbose(String.format("onDataEntry: %s, %s", table, data));
		// mDb.insertWithOnConflict(table, null, data,
		// SQLiteDatabase.CONFLICT_REPLACE);
		SortedSet<DataEntry> inserts = mOperations.get(table);
		if (inserts == null) {
			inserts = new TreeSet<DataEntry>();
			mOperations.put(table, inserts);
			mDepthMap.put(table, depth);
		}

		String changeIdColumn = provider.getChangeIdColumn(table);
		inserts.add(new DataEntry(data.getAsLong(changeIdColumn), data));

		mCount++;

		mAffectedTables.add(table);

		if (table.equals(mMainTable) && mStepDownload > 0.0) {
			mDownloadsDone++;
			mProgress += mStepDownload;
			mListener.onProgress(String.format("Downloading %s data %d...", mMainTable, mDownloadsDone), mProgress);
		}
	}

	public void notifyChanges() {
		for (String table : mAffectedTables) {
			Uri uri = provider.getUri(mUser, table);
			ResolvedUri resolvedUri = provider.resolveUri(uri);
			provider.notifyChange(uri, resolvedUri);
		}
	}

	public synchronized boolean isCancelled() {
		return mCancelled;
	}

	public synchronized void cancel() {
		mCancelled = true;
	}

	@Override
	public void onEmptyTable(String table) {
		if (!mOperations.containsKey(table)) {
			mOperations.put(table, new TreeSet<DataEntry>());
			mAffectedTables.add(table);
		}
	}

}