package cat.mobilejazz.database.filter;

import cat.mobilejazz.database.content.CollectionFilter;
import cat.mobilejazz.database.query.Select;

public interface Filter {
	
	public Select getLocalFilter();
	
	public CollectionFilter getRemoteFilter();

	public void setListener(FilterListener listener);
	
}
