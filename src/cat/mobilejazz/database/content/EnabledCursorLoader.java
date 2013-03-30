/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cat.mobilejazz.database.content;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import cat.mobilejazz.utilities.debug.Debug;

/**
 * Static library support version of the framework's
 * {@link android.content.CursorLoader}. Used to write apps that run on
 * platforms prior to Android 3.0. When running on Android 3.0 or above, this
 * implementation is still used; it does not try to switch to the framework's
 * implementation. See the framework SDK documentation for a class overview.
 */
public class EnabledCursorLoader extends AsyncTaskLoader<Cursor> {

	public final class CachedLoadContentObserver extends ContentObserver implements OnEnabledChangedListener {

		private LoaderParent mParent;
		private boolean mCachedContentChange;

		public CachedLoadContentObserver(LoaderParent parent) {
			super(new Handler());
			mParent = parent;
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			if (mParent.isEnabled()) {
				Debug.debug("onChange: reload");
				onContentChanged();
			} else {
				Debug.debug("onChange: cached");
				mCachedContentChange = true;
			}
		}

		@Override
		public void onEnabledChanged(boolean on) {
			if (on && mCachedContentChange) {
				mCachedContentChange = false;
				onContentChanged();
			}
		}

	}

	CachedLoadContentObserver mObserver;

	Uri mUri;
	String[] mProjection;
	String mSelection;
	String[] mSelectionArgs;
	String mSortOrder;

	Cursor mCursor;

	/* Runs on a worker thread */
	@Override
	public Cursor loadInBackground() {
		Cursor cursor = getContext().getContentResolver().query(mUri, mProjection, mSelection, mSelectionArgs,
				mSortOrder);
		if (cursor != null) {
			// Ensure the cursor window is filled
			cursor.getCount();
			registerContentObserver(cursor, mObserver);
		}
		return cursor;
	}

	/**
	 * Registers an observer to get notifications from the content provider when
	 * the cursor needs to be refreshed.
	 */
	void registerContentObserver(Cursor cursor, ContentObserver observer) {
		cursor.registerContentObserver(mObserver);
	}

	/* Runs on the UI thread */
	@Override
	public void deliverResult(Cursor cursor) {
		if (isReset()) {
			// An async query came in while the loader is stopped
			if (cursor != null) {
				cursor.close();
			}
			return;
		}
		Cursor oldCursor = mCursor;
		mCursor = cursor;

		if (isStarted()) {
			super.deliverResult(cursor);
		}

		if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
			oldCursor.close();
		}
	}

	/**
	 * Creates an empty unspecified CursorLoader. You must follow this with
	 * calls to {@link #setUri(Uri)}, {@link #setSelection(String)}, etc to
	 * specify the query to perform.
	 */
	public EnabledCursorLoader(LoaderParent parent) {
		super(parent.getContext());
		mObserver = new CachedLoadContentObserver(parent);
		parent.addOnEnabledChangedListener(mObserver);
	}

	/**
	 * Creates a fully-specified CursorLoader. See
	 * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)
	 * ContentResolver.query()} for documentation on the meaning of the
	 * parameters. These will be passed as-is to that call.
	 */
	public EnabledCursorLoader(LoaderParent parent, Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		super(parent.getContext());
		mObserver = new CachedLoadContentObserver(parent);
		parent.addOnEnabledChangedListener(mObserver);
		mUri = uri;
		mProjection = projection;
		mSelection = selection;
		mSelectionArgs = selectionArgs;
		mSortOrder = sortOrder;
	}

	/**
	 * Starts an asynchronous load of the contacts list data. When the result is
	 * ready the callbacks will be called on the UI thread. If a previous load
	 * has been completed and is still valid the result may be passed to the
	 * callbacks immediately.
	 * 
	 * Must be called from the UI thread
	 */
	@Override
	protected void onStartLoading() {
		if (mCursor != null) {
			deliverResult(mCursor);
		}
		if (takeContentChanged() || mCursor == null) {
			forceLoad();
		}
	}

	/**
	 * Must be called from the UI thread
	 */
	@Override
	protected void onStopLoading() {
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}

	@Override
	public void onCanceled(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	@Override
	protected void onReset() {
		super.onReset();

		// Ensure the loader is stopped
		onStopLoading();

		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}
		mCursor = null;

		mObserver.mParent.removeOnEnabledChangedListener(mObserver);
	}

	public Uri getUri() {
		return mUri;
	}

	public void setUri(Uri uri) {
		mUri = uri;
	}

	public String[] getProjection() {
		return mProjection;
	}

	public void setProjection(String[] projection) {
		mProjection = projection;
	}

	public String getSelection() {
		return mSelection;
	}

	public void setSelection(String selection) {
		mSelection = selection;
	}

	public String[] getSelectionArgs() {
		return mSelectionArgs;
	}

	public void setSelectionArgs(String[] selectionArgs) {
		mSelectionArgs = selectionArgs;
	}

	public String getSortOrder() {
		return mSortOrder;
	}

	public void setSortOrder(String sortOrder) {
		mSortOrder = sortOrder;
	}

}
