package cat.mobilejazz.database.content;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import cat.mobilejazz.database.ProgressListener;
import cat.mobilejazz.database.content.DataAdapter.DataAdapterListener;
import cat.mobilejazz.database.content.DataProvider.ResolvedUri;
import cat.mobilejazz.database.query.Select;
import cat.mobilejazz.utilities.debug.Debug;

/* TODO: maybe outsource to tb project? */
public class DataProcessor implements DataAdapterListener {

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
	private long mExpectedCount;

	private Select mCurrentSelection;

	private Map<String, SortedSet<DataEntry>> mOperations;

	private DataProvider provider;

	public DataProcessor(DataProvider provider, String user, SQLiteDatabase db, ProgressListener listener,
			String mainTable, long expectedCount, Select currentSelection) {
		this.provider = provider;
		mDb = db;
		mUser = user;
		mAffectedTables = new HashSet<String>();
		mOperations = new HashMap<String, SortedSet<DataEntry>>();
		mListener = listener;
		mExpectedCount = expectedCount;
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
	}

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

	public void performOperations() {
		Set<String> pendingDeletes = new HashSet<String>();
		Cursor pendingChanges = mDb.query(Changes.TABLE_NAME, new String[] { Changes.COLUMN_TABLE, Changes.COLUMN_ID },
				String.format("%s = ?", Changes.COLUMN_ACTION), new String[] { String.valueOf(Changes.ACTION_REMOVE) },
				null, null, null);
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

		double step = (1.0 - mDownloadsDone * mStepDownload) / (double) mCount;

		for (Map.Entry<String, SortedSet<DataEntry>> e : mOperations.entrySet()) {
			String table = e.getKey();
			String changeIdColumn = provider.getChangeIdColumn(table);

			if (table.equals(mMainTable)) {

				Cursor current = mDb.query(mCurrentSelection.getTable(),
						new String[] { BaseColumns._ID, serverIdColumn }, mCurrentSelection.getSelection(),
						mCurrentSelection.getSelectionArgs(), null, null, serverIdColumn);
				current.moveToFirst();
				Debug.debug("[%d, %d] Querying: %s", current.getCount(), e.getValue().size(), mCurrentSelection);

				CachedIterator i = new CachedIterator(e.getValue().iterator());

				while (!current.isAfterLast() || !i.isAfterLast()) {
					long currentServerId = getCurrentServerId(current);
					DataEntry entry = i.getValue();

					Debug.debug("%s: %d <--- %d", mMainTable, currentServerId, entry.serverId);

					if (entry.serverId == currentServerId) {
						// update:
						mDb.update(table, entry.values, "_id = ?", new String[] { String.valueOf(current.getLong(0)) });
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
							try {
								mDb.insertOrThrow(table, null, entry.values);
							} catch (SQLiteConstraintException e1) {
								Debug.logException(e1);
							}
						}
						i.moveToNext();
						mOperationsDone++;
						mProgress += step;
						mListener.onProgress(
								String.format("Processing %s data %d/%d...", mMainTable, mOperationsDone, mCount),
								mProgress);
					} else {
						// delete:
						mDb.delete(table, "_id = ?", new String[] { String.valueOf(current.getLong(0)) });
						current.moveToNext();
					}
				}

				current.close();
			} else {
				// process non maintable entries
				for (DataEntry entry : e.getValue()) {
					// no deletes are propagated along delegates:
					if (!pendingDeletes.contains(getSignature(table, entry.values.getAsLong(changeIdColumn)))) {
						mDb.insertWithOnConflict(table, null, entry.values, SQLiteDatabase.CONFLICT_REPLACE);
					}
				}
			}
		}

		Debug.info("Operations done: " + mOperationsDone);
		mListener.onFinished();
	}

	@Override
	public void onDataEntry(String table, ContentValues data) {
		Debug.verbose(String.format("onDataEntry: %s, %s", table, data));
		// mDb.insertWithOnConflict(table, null, data,
		// SQLiteDatabase.CONFLICT_REPLACE);
		SortedSet<DataEntry> inserts = mOperations.get(table);
		if (inserts == null) {
			inserts = new TreeSet<DataEntry>();
			mOperations.put(table, inserts);
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

	private class CachedIterator {

		private DataEntry value;
		private Iterator<DataEntry> iterator;

		public CachedIterator(Iterator<DataEntry> iterator) {
			this.iterator = iterator;

			moveToNext();
		}

		public void moveToNext() {
			if (iterator.hasNext()) {
				value = iterator.next();
			} else {
				value = NO_DATA;
			}
		}

		public boolean isAfterLast() {
			return value == NO_DATA;
		}

		public DataEntry getValue() {
			return value;
		}

	}

}