package cat.mobilejazz.database.filter;

import cat.mobilejazz.database.query.Select;

public interface Filter extends RemoteFilter {

	public Select getLocalFilter();

	public void setListener(FilterListener listener);

}
