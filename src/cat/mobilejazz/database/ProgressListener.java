package cat.mobilejazz.database;

public interface ProgressListener {

		/**
		 * This callback is invoked when the some sync operation has made some
		 * progress.
		 * 
		 * @param message
		 *            A message indicating what operation is currently
		 *            performed.
		 * @param percentage
		 *            A value between {@code 0.0} and {@code 1.0} where
		 *            {@code 1.0} means that the sync is finished.
		 */
		public void onProgress(String message, double percentage);

		/**
		 * Indicates that the operation has finished. After this call, it shall
		 * be guaranteed that there are no subsequent calls to
		 * {@link #onProgress(String, double)} from the same source.
		 */
		public void onFinished();

	}