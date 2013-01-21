package cat.mobilejazz.database;

public class Type {
	
	public static final int BOOLEAN = 0;
	public static final int INT = 2;
	public static final int LONG = 3;
	public static final int STRING = 5;
	public static final int DOUBLE = 7;
	public static final int DELEGATE = 8;
	
	public static String asString(int type) {
		switch (type) {
		case BOOLEAN:
			return "boolean";
		case INT:
			return "int";
		case LONG:
			return "long";
		case STRING:
			return "String";
		case DOUBLE:
			return "double";
		case DELEGATE:
			return "delegate";
		default:
			return "unkown";
		}
	}

}
