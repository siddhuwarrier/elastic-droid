package org.elasticdroid;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class LoginView extends FragmentActivity {
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
}