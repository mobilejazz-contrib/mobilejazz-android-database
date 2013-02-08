package cat.mobilejazz.database;


public class SqlView extends View {

	private String sqlSelectStatement;
	private Iterable<String> dependencies;

	public SqlView(String name, String sqlSelectStatement, Iterable<String> dependencies) {
		super(name);
		this.sqlSelectStatement = sqlSelectStatement;
		this.dependencies = dependencies;
	}

	@Override
	public Iterable<String> getDependencies() {
		return dependencies;
	}

	@Override
	public String getSqlSelectStatement() {
		return sqlSelectStatement;
	}

}
