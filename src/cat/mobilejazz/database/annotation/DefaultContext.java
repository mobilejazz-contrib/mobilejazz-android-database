package cat.mobilejazz.database.annotation;

import cat.mobilejazz.database.EntityContext;

import com.google.compatibility.gson.JsonObject;

/**
 * Just a placeholder for default value in the annotation {@link ParentId}.
 */
class DefaultContext implements EntityContext<String> {

	@Override
	public String get(JsonObject entity) {
		return null;
	}

}