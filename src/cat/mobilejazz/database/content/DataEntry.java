package cat.mobilejazz.database.content;

import android.content.ContentValues;

/**
 * A parsed data entry that represents an entity on the server side. It
 * comprises the raw server data in form of database-ready {@link ContentValues}
 * and two ids:
 * <ul>
 * <li><strong>parentId:</strong>The server id of the parent entity. This is
 * relevant only for entities that are inserted as part of a delegate.</li>
 * <li><strong>serverId:</strong>The server id of the entity this entry is
 * referring to</li>
 * </ul>
 */
public class DataEntry implements Comparable<DataEntry> {

	long serverId;
	long parentId;
	ContentValues values;

	public DataEntry(long parentId, long serverId, ContentValues values) {
		this.parentId = parentId;
		this.serverId = serverId;
		this.values = values;
	}

	public DataEntry(long serverId, ContentValues values) {
		this(0L, serverId, values);
	}

	private int compare(long a, long b) {
		if (a > b) {
			return 1;
		} else if (a < b) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public int compareTo(DataEntry another) {
		int p = compare(parentId, another.parentId);
		if (p == 0) {
			// same parent ids:
			return compare(serverId, another.serverId);
		} else {
			return p;
		}
	}

	public long getServerId() {
		return serverId;
	}
	
	public long getParentId() {
		return parentId;
	}

	public ContentValues getValues() {
		return values;
	}

}