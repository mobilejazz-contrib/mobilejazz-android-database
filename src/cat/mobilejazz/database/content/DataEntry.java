package cat.mobilejazz.database.content;

import android.content.ContentValues;

public class DataEntry implements Comparable<DataEntry> {

	long serverId;
	ContentValues values;

	public DataEntry(long serverId, ContentValues values) {
		this.serverId = serverId;
		this.values = values;
	}

	@Override
	public int compareTo(DataEntry another) {
		if (serverId > another.serverId) {
			return 1;
		} else if (serverId < another.serverId) {
			return -1;
		} else {
			return 0;
		}
	}

	public long getServerId() {
		return serverId;
	}

	public ContentValues getValues() {
		return values;
	}

}