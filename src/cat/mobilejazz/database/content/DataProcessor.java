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

	private boolean mCancelled;

	private LinkedHashMap<String, SortedSet<DataEntry>> mOperations;
	private Map<String, Integer> mDepthMap;

	private DataProvider provider;

	public DataProcessor(DataProvider provider, String user, SQLiteDatabase db, ProgressListener listener,
			String mainTable, long expectedCount, Select currentSelection) {
		this.provider = provider;
		mDb = db;
		mUser = user;
		mAffectedTables = new HashSet<String>();
		mOperations = new LinkedHashMap<String, SortedSet<DataEntry>>();
		mDepthMap = new HashMap<String, Integer>();
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
									|| SQLUtils.getTimestamp(current, 2).before(startTime))
								mDb.delete(table, "_id = ?", new String[] { String.valueOf(current.getLong(0)) });
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