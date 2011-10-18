package org.elasticdroid;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class DashboardView extends FragmentActivity {

	@Override
	public void onCreate(Bundle saveState) {
    	super.onCreate(saveState);
    	setContentView(R.layout.dashboard);
	}
}
