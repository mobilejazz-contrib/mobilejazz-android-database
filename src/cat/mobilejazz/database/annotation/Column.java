package cat.mobilejazz.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cat.mobilejazz.database.Affinity;
import cat.mobilejazz.database.DataParser;
import cat.mobilejazz.database.IdentityParser;
import cat.mobilejazz.database.Storage;
import cat.mobilejazz.database.Type;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	
	int type() default Type.STRING;
	
	int affinity() default Affinity.NONE;

	String constraint() default "";
	
	int storage() default Storage.REMOTE;
	
	String delegate() default "";
	
	Class<? extends DataParser> parser() default IdentityParser.class;
	
}
