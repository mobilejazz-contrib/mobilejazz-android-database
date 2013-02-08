package cat.mobilejazz.database;

import java.util.ArrayList;
import java.util.List;

import cat.mobilejazz.utilities.collections.ConcatenatedIterator;
import cat.mobilejazz.utilities.format.ObjectPrinter;
import cat.mobilejazz.utilities.format.StringFormatter;

public class UnionView extends View {

	private static class SelectViewPrinter implements ObjectPrinter<View> {

		@Override
		public String toString(View object) {
			return object.getSqlSelectStatement();
		}

	}

	private static SelectViewPrinter selectViewPrinter = new SelectViewPrinter();

	private View[] parts;

	public UnionView(String name, View... parts) {
		super(name);
		this.parts = parts;
	}

	@Override
	public Iterable<String> getDependencies() {
		List<Iterable<String>> iterables = new ArrayList<Iterable<String>>();
		for (View v : parts) {
			iterables.add(v.getDependencies());
		}
		return ConcatenatedIterator.getIterable(iterables);
	}

	@Override
	public String getSqlSelectStatement() {
		return StringFormatter.printIterable(" UNION ", selectViewPrinter, parts).toString();
	}

}
