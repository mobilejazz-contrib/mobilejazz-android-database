package cat.mobilejazz.database;

import java.text.ParseException;
import java.util.Date;

import android.database.Cursor;
import cat.mobilejazz.utilities.debug.Debug;
import cat.mobilejazz.utilities.io.DateUtils;

public final class SQLUtils {

	public static Date getTimestamp(Cursor cursor, int column) {
		try {
			return DateUtils.parseTimestamp(cursor.getString(column));
		} catch (ParseException e) {
			Debug.logException(e);
			return null;
		}
	}

	public static Date getDate(Cursor cursor, int column) {
		try {
			return DateUtils.parseDate(cursor.getString(column));
		} catch (ParseException e) {
			Debug.logException(e);
			return null;
		}
	}

	public static boolean getBoolean(Cursor cursor, int column) {
		return cursor.getInt(column) > 0;
	}

	public static Long getLong(Cursor cursor, int column) {
		if (cursor.isNull(column)) {
			return null;
		} else {
			return cursor.getLong(column);
		}
	}

	public static String formatDate(Date value) {
		return DateUtils.formatDate(value);
	}

	public static String formatTimestamp(Date value) {
		return DateUtils.formatTimestamp(value);
	}

	public static Long formatBoolean(Boolean value) {
		if (value == null)
			return null;
		return (value) ? 1L : 0L;
	}

	public static String[] getStringArray(Cursor cursor, int columnIndex) {
		if (columnIndex < 0) {
			return null;
		} else {
			String value = cursor.getString(columnIndex);
			if (value != null) {
				return value.split(",");
			} else {
				return null;
			}
		}

	}

	public static long[] getLongArray(Cursor cursor, int columnIndex) {
		String[] values = getStringArray(cursor, columnIndex);
		long[] result = new long[values.length];
		for (int i = 0; i < values.length; ++i) {
			result[i] = Long.parseLong(values[i]);
		}
		return result;
	}

	public static int[] getIntArray(Cursor cursor, int columnIndex) {
		String[] values = getStringArray(cursor, columnIndex);
		int[] result = new int[values.length];
		for (int i = 0; i < values.length; ++i) {
			result[i] = Integer.parseInt(values[i]);
		}
		return result;
	}

	public static String getStringOrNull(Cursor cursor, int columnIndex) {
		if (cursor.isNull(columnIndex)) {
			return null;
		} else {
			return cursor.getString(columnIndex);
		}
	}

	public static String valueOf(Object arg) {
		if (arg instanceof Boolean) {
			return String.valueOf(formatBoolean((Boolean) arg));
		} else if (arg instanceof Date) {
			return DateUtils.formatDate((Date) arg);
		} else {
			return String.valueOf(arg);
		}
	}

}
