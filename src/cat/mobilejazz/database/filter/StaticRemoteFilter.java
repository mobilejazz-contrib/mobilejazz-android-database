package cat.mobilejazz.database.filter;

import cat.mobilejazz.database.content.CollectionFilter;

public class StaticRemoteFilter implements RemoteFilter {

	private CollectionFilter filter;

	public StaticRemoteFilter(CollectionFilter filter) {
		super();
		this.filter = filter;
	}

	@Override
	public CollectionFilter getRemoteFilter() {
		return filter;
	}

}
