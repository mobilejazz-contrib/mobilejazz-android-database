package cat.mobilejazz.database.filter;

import cat.mobilejazz.database.content.CollectionFilter;
import cat.mobilejazz.database.query.Select;

public class StaticFilter implements Filter {

	private Select localFilter;
	private CollectionFilter remoteFilter;

	/**
	 * Creates a new instance.
	 * 
	 * @param localFilter
	 *            Optional local filter. May be {@code null}.
	 * @param remoteFilter
	 *            This argument must not be {@code null}. It defines the filter
	 *            on the server side when updating the local data. If no local
	 *            filter is provided, this also defines the local filter.
	 */
	public StaticFilter(Select localFilter, CollectionFilter remoteFilter) {
		this.localFilter = localFilter;
		this.remoteFilter = remoteFilter;
	}

	@Override
	public Select getLocalFilter() {
		if (localFilter != null) {
			return localFilter;
		} else {
			return remoteFilter.getSelect();
		}
	}

	@Override
	public CollectionFilter getRemoteFilter() {
		return remoteFilter;
	}

	@Override
	public void setListener(FilterListener listener) {
		// this filter is static. no need to inform listeners
		listener.filterChanged();
	}

}
