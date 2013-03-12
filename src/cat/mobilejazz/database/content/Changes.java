package cat.mobilejazz.database.content;

import android.provider.BaseColumns;
import cat.mobilejazz.database.Affinity;
import cat.mobilejazz.database.Type;
import cat.mobilejazz.database.annotation.Column;
import cat.mobilejazz.database.annotation.Local;
import cat.mobilejazz.database.annotation.TableName;

@Local
public class Changes implements BaseColumns {

	public static final int ACTION_UPDATE = 0;
	public static final int ACTION_CREATE = 1;
	public static final int ACTION_REMOVE = 2;

	/**
	 * The name of the table containing users.
	 */
	@TableName
	public final static String TABLE_NAME = "db_changes";

	/**
	 * Column name for the time when the change was applied.
	 * <p>
	 * Type: TEXT (encoding a timestamp)
	 * </p>
	 */
	@Column(type = Type.STRING, affinity = Affinity.TEXT)
	public final static String COLUMN_TIMESTAMP = "created_at";

	/**
	 * Column name for the action of change. See {@link #ACTION_CREATE},
	 * {@link #ACTION_UPDATE}, {@link #ACTION_REMOVE}.
	 * <p>
	 * Type: INTEGER
	 * </p>
	 */
	@Column(type = Type.INT, affinity = Affinity.INTEGER)
	public final static String COLUMN_ACTION = "action";

	/**
	 * Column name for the affected table.
	 * <p>
	 * Type: TEXT
	 * </p>
	 */
	@Column(type = Type.STRING, affinity = Affinity.TEXT)
	public final static String COLUMN_TABLE = "table_name";

	/**
	 * Column name for the local sqlite primary key
	 * <p>
	 * Type: INTEGER
	 * </p>
	 */
	@Column(type = Type.LONG, affinity = Affinity.INTEGER)
	public final static String COLUMN_NATIVE_ID = "obj_native_id";

	@Column(type = Type.LONG, affinity = Affinity.INTEGER)
	public final static String COLUMN_ID = "obj_id";

	/**
	 * Column name for the changed values. This should already be prepared in a
	 * way, that the server can understand it.
	 * <p>
	 * Type: TEXT (encoding a JSON object representing the change)
	 * </p>
	 */
	@Column(type = Type.STRING, affinity = Affinity.TEXT)
	public final static String COLUMN_VALUES = "values_json";

	// /**
	// * Column name for the api sub-path to invoke in order to submit the
	// * values.
	// * <p>
	// * Type: TEXT
	// * </p>
	// */
	// @Column(type = Type.STRING, affinity = Affinity.TEXT)
	// public final static String COLUMN_API_PATH = "api_path";

	/**
	 * Column name for additional data that may be needed by a
	 * {@link SyncAdapter} to resolve multi-object changes.
	 * <p>
	 * Type: TEXT
	 * </p>
	 */
	@Column(type = Type.STRING, affinity = Affinity.TEXT)
	public final static String COLUMN_ADDITIONAL_DATA = "additional_data";

}