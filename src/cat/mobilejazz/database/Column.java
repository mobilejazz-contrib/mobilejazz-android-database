package cat.mobilejazz.database;

import cat.mobilejazz.utilities.format.StringFormatter;
import cat.mobilejazz.utilities.format.StringTemplate;
import cat.mobilejazz.utilities.format.SymbolTable;
import cat.mobilejazz.utilities.format.TreeObject;

public class Column implements TreeObject {

	private int type;
	private int affinity;
	private String constraint;
	private String name;
	private StringTemplate delegate;
	private int storage;

	private String declaredName;
	private DataParser parser;
	private Table parent;

	private boolean isUID;

	public Column(int type, int affinity, String constraint, int storage, String name, String declaredName,
			String delegate, DataParser parser, boolean isUID, Table parent) {
		this.type = type;
		this.affinity = affinity;
		this.constraint = constraint;
		this.storage = storage;
		this.name = name;
		this.declaredName = declaredName;
		this.parser = parser;
		this.parent = parent;
		this.delegate = new StringTemplate(delegate);
		this.isUID = isUID;
	}

	@SuppressWarnings("unchecked")
	public <T> T parse(Object value) {
		if (parser != null) {
			return (T) parser.parse(value);
		} else {
			return (T) value;
		}
	}

	public int getAffinity() {
		return affinity;
	}

	public int getType() {
		return type;
	}

	public String getConstraint() {
		return constraint;
	}

	public int getStorage() {
		return storage;
	}

	public String getName() {
		return name;
	}

	public String getDeclaredName() {
		return declaredName;
	}

	public Table getParent() {
		return parent;
	}

	public String getFullName() {
		return parent.getName() + "." + name;
	}
	
	public boolean isUID() {
		return isUID;
	}

	/**
	 * Returns the value for the delegate table that this column points to.
	 * 
	 * @param values
	 * @return
	 */
	public String getDelegate(SymbolTable<?> symbols) {
		if (delegate != null) {
			return delegate.render(symbols);
		} else {
			return null;
		}
	}

	public String toString() {
		if (constraint == null)
			return String.format("%s %s", name, Affinity.asString(affinity));
		else
			return String.format("%s %s %s", name, Affinity.asString(affinity), constraint);
	}

	@Override
	public StringBuilder dump(StringBuilder result, int indent) {
		StringFormatter.appendWS(result, indent * 2);
		result.append(declaredName).append("[").append(Type.asString(type)).append("]").append(": ").append(toString());
		return result;
	}

}
