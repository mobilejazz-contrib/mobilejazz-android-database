package cat.mobilejazz.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.database.Cursor;
import android.text.TextUtils;
import cat.mobilejazz.utilities.debug.Debug;

public final class SQLUtils {

	private static SimpleDateFormat iso8601FormatUTC = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	
	private static SimpleDateFormat sqlFormatUTC = new SimpleDateFormat(
			"yyyy-MM-dd");
	
	static {
		iso8601FormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
		sqlFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public static Date parseTimestamp(String dateString) throws ParseException {
		if (!TextUtils.isEmpty(dateString)) {
			return iso8601FormatUTC.parse(dateString);
		} else {
			return null;
		}
	}
	
	public static Date parseDate(String dateString) throws ParseException {
		if (!TextUtils.isEmpty(dateString)) {
			return sqlFormatUTC.parse(dateString);
		} else {
			return null;
		}
	}
	
	public static Date getTimestamp(Cursor cursor, int column) {
		try {
			return parseTimestamp(cursor.getString(column));
		} catch (ParseException e) {
			Debug.logException(e);
			return null;
		}
	}
	
	public static Date getDate(Cursor cursor, int column) {
		try {
			return parseDate(cursor.getString(column));
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
	
	public static Long formatBoolean(Boolean value) {
		if (value == null)
			return null;
		return (value) ? 1L : 0L;
	}
	
	public static String formatTimestamp(Date date) {
		if (date != null)
			return iso8601FormatUTC.format(date);
		else
			return null;
	}
	
	public static String formatDate(Date date) {
		if (date != null)
			return sqlFormatUTC.format(date);
		else
			return null;
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

}
