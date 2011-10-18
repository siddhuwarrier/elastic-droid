package org.elasticdroid.utils;

import org.elasticdroid.intf.ConnectivityChecker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityCheckerImpl implements ConnectivityChecker {

	@Override
	public boolean isConnected(Context context) {
        NetworkInfo networkInfo = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
	}

}
