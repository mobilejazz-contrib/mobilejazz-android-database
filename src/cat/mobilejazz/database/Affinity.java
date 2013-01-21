package cat.mobilejazz.database;

public class Affinity {
	
	public static final int NONE = 0;
	public static final int TEXT = 1;
	public static final int NUMERIC = 2;
	public static final int INTEGER = 3;
	public static final int REAL = 4;
	
	public static String asString(int affinity) {
		switch (affinity) {
		case Affinity.NONE:
			return "NONE";
		case Affinity.INTEGER:
			return "INTEGER";
		case Affinity.NUMERIC:
			return "NUMERIC";
		case Affinity.REAL:
			return "REAL";
		case Affinity.TEXT:
			return "TEXT";
		default:
			return "NONE";
		}
	}

}
