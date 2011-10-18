package org.elasticdroid;

import org.elasticdroid.tpl.GenericFragmentActivity;

import android.content.DialogInterface;
import android.os.Bundle;

public class LoginView extends GenericFragmentActivity {
    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState
     *            the saved instance state.
     * */
	@Override
	public void onCreate(Bundle saveState) {
    	super.onCreate(saveState);
    	setContentView(R.layout.login);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		//TODO nothing to do here yet...
	}
}