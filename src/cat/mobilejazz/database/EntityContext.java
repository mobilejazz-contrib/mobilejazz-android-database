package cat.mobilejazz.database;

import com.google.compatibility.gson.JsonObject;

/**
 * Models a data field of type {@link T} that can be derived from other data
 * fields of an entity provided as an instance of {@link JsonObject}.
 * 
 */
public interface EntityContext<T> {

	public static final String FROM_ENTITY_CONTEXT = "cat.mobilejazz.database.EntityContext.FROM_ENTITY_CONTEXT";

	public T get(JsonObject entity);

}
