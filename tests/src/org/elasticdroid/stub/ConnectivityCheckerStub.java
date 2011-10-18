package org.elasticdroid.stub;

import org.elasticdroid.intf.ConnectivityChecker;

import android.content.Context;

public class ConnectivityCheckerStub implements ConnectivityChecker {

	private boolean isConnected;

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
	@Override
	public boolean isConnected(Context context) {
		return isConnected;
	}
}
