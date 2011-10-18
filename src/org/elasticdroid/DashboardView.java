package org.elasticdroid;

import org.elasticdroid.tpl.GenericFragmentActivity;

import android.content.DialogInterface;
import android.os.Bundle;

public class DashboardView extends GenericFragmentActivity {

	@Override
	public void onCreate(Bundle saveState) {
    	super.onCreate(saveState);
    	setContentView(R.layout.dashboard);
	}
	
	@Override
	public void onCancel(DialogInterface arg0) {
	}

}
