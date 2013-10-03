package cat.mobilejazz.database.annotation;

import android.content.ContentValues;
import cat.mobilejazz.database.EntityContext;

/**
 * Just a placeholder for default value in the annotation {@link ParentId}.
 */
class DefaultContext implements EntityContext<String> {

	@Override
	public String get(ContentValues entity) {
		return null;
	}

}