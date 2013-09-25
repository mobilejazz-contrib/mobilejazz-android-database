package cat.mobilejazz.database;

public interface Storage {

	/**
	 * The contents of this column should be stored only in cache.
	 */
	public final static int LOCAL = 0;

	/**
	 * The contents of this column should also be sent to the server.
	 */
	public final static int REMOTE = 1;

	/**
	 * The contents of this column should be stored only in cache but they
	 * contain relevant information for other columns that should be sent to the
	 * server.
	 */
	public final static int INFO = 2;

}
