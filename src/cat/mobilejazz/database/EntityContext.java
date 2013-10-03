package cat.mobilejazz.database;

import android.content.ContentValues;

/**
 * Models a data field of type {@link T} that can be derived from other data
 * fields of an entity provided as an instance of {@link ContentValues}.
 * 
 */
public interface EntityContext<T> {

	public T get(ContentValues entity);
	
}
