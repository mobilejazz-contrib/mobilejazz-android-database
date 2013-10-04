package cat.mobilejazz.database;

import com.google.compatibility.gson.JsonObject;

/**
 * Returns always the same value. This allows to represent both constants as
 * well as dynamically derived values as variables of type {@link EntityContext}
 * .
 */
public class StaticValue<T> implements EntityContext<T> {

	private T value;

	public StaticValue(T value) {
		this.value = value;
	}

	@Override
	public T get(JsonObject entity) {
		return value;
	}

}
