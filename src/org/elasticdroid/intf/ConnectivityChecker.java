package org.elasticdroid.intf;

import android.content.Context;

public interface ConnectivityChecker {
	/**
	 * Is connected to the internet?
	 * @return
	 */
	public boolean isConnected(Context context);
}
