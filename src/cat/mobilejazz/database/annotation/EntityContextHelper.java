package cat.mobilejazz.database.annotation;

import cat.mobilejazz.database.EntityContext;
import cat.mobilejazz.database.StaticValue;

public class EntityContextHelper {

	public static EntityContext<String> getFromAnnotation(String constantValue,
			Class<? extends EntityContext<String>> contextClass) throws InstantiationException, IllegalAccessException {
		if (constantValue.equals(EntityContext.FROM_ENTITY_CONTEXT)) {
			if (contextClass.equals(DefaultContext.class)) {
				// represents a placeholder:
				throw new IllegalArgumentException("You need to define a valid entity context.");
			} else {
				return contextClass.newInstance();
			}
		} else if (constantValue.length() > 0) {
			return new StaticValue<String>(constantValue);
		} else {
			return null;
		}
	}

}
