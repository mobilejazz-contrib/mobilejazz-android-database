package cat.mobilejazz.database.content;

import android.content.Context;

public interface LoaderParent {

	public boolean isEnabled();

	public void setOnEnabledChangedListener(OnEnabledChangedListener listener);

	public Context getContext();

}
