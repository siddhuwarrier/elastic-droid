package org.elasticdroid;

import java.util.regex.Pattern;

import org.elasticdroid.intf.ConnectivityChecker;
import org.elasticdroid.intf.callback.LoginTaskCallback;
import org.elasticdroid.model.LoginModel.LoginResult;
import org.elasticdroid.model.db.User;
import org.elasticdroid.task.LoginTask;
import org.elasticdroid.utils.ConnectivityCheckerImpl;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


public class LoginFragment extends Fragment {

	private static final String TAG = LoginFragment.class.getName();
	
	private ConnectivityChecker connChecker;
	
	private LoginTask loginTask;
	
    private ProgressDialog progressDialog;
    
    private boolean progressDialogDisplayed;
	
    private static final String EMAIL_REGEX = "^[\\w-]+(\\.[\\w-]+)*@([" +
    		"A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*?\\.[A-Za-z]{2,6}|(\\d{1,3}\\." +
    		"){3}\\d{1,3})(:\\d{4})?$";
    
	public LoginFragment() {
		connChecker = new ConnectivityCheckerImpl();
	}
	
	public void setConnectivityChecker(ConnectivityChecker connChecker) {
		this.connChecker = connChecker;
	}
	
	@Override
	public void onCreate(Bundle stateToRestore) {
		super.onCreate(stateToRestore);

		setRetainInstance(true);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    	View fragmentView = inflater.inflate(
    			R.layout.loginfragment, 
    			container, 
    			false);
    	
    	return fragmentView;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	setUpListeners();
    	
		//if progress dialog displayed, re-display it
		if (progressDialogDisplayed) {
			showProgressDialog();
		}
    }
		
	public void setLoginTask(LoginTask loginTask) {
		this.loginTask = loginTask;
	}

	private boolean entriesValidated() {
		EditText awsAccount = (EditText) getActivity().
				findViewById(R.id.awsAccountEntry);
		EditText awsAccessKey = (EditText) getActivity().
				findViewById(R.id.awsAccessKey);
		EditText awsSecretAccessKey = (EditText) getActivity().
				findViewById(R.id.awsSecretAccessKey);
		
		return Pattern
			.compile(EMAIL_REGEX)
			.matcher(awsAccount.getText().toString()).matches() && 
			!awsAccessKey.getText().toString().trim().equals("") &&
			!awsSecretAccessKey.getText().toString().trim().equals("");
	}
	
	private void performLogin() {
		if (loginTask == null) {
			loginTask = new LoginTask(
					getUser(), 
					getActivity(), 
					new LoginCallbackImpl());
		}
		
		loginTask.execute();
		
		showProgressDialog();
	}
	
	private void setUpListeners() {
		Button loginBtn = (Button)getActivity().findViewById(R.id.loginButton);
		loginBtn.setOnClickListener(new ListenerImpl());
	}
	
	private User getUser() {
		EditText awsAccount = (EditText) getActivity().
				findViewById(R.id.awsAccountEntry);
		EditText awsAccessKey = (EditText) getActivity().
				findViewById(R.id.awsAccessKey);
		EditText awsSecretAccessKey = (EditText) getActivity().
				findViewById(R.id.awsSecretAccessKey);
		
		User user = new User();
		user.setAwsAccount(awsAccount.getText().toString());
		user.setAwsAccessKey(awsAccessKey.getText().toString());
		user.setAwsSecretAccessKey(awsSecretAccessKey.getText().toString());
		
		return user;
	}
	
	private void displayDashboardView() {
		Intent dashboardIntent = new Intent(
				getActivity(), 
				DashboardView.class);
		
		getActivity().finish(); //finish this activity
		
		startActivity(dashboardIntent);
	}
	
	private void displayError(int id) {
		EditText awsAccount = (EditText) getActivity().
				findViewById(R.id.awsAccountEntry);
		EditText awsAccessKey = (EditText) getActivity().
				findViewById(R.id.awsAccessKey);
		EditText awsSecretAccessKey = (EditText) getActivity().
				findViewById(R.id.awsSecretAccessKey);
		
		awsAccount.setError(getActivity().getString(id));
		awsAccessKey.setError(getActivity().getString(id));
		awsSecretAccessKey.setError(getActivity().getString(id));
	}
	
	/*
	 * Can't use GenericFragmentActivity's show progress dialog. Not without 
	 * serious faffing about, at least.
	 */
	private void showProgressDialog() {
		progressDialog = new ProgressDialog(getActivity());
		
		progressDialog.setMessage(getActivity().getString(R.string.progressdialog_message));
		progressDialog.setCancelable(true);
		progressDialog.setOnCancelListener(new ListenerImpl());
		progressDialog.show();
		progressDialogDisplayed = true;
	}

	private void dismissProgressDialog() {
		progressDialog.dismiss();
		
		progressDialogDisplayed = false;
	}
    
    public class LoginCallbackImpl implements LoginTaskCallback {

		@Override
		public void cleanUpAfterTaskExecution() {
			Log.d(TAG, "Cleaning up after task execution...");
			loginTask = null;
		}

		@Override
		public void loginResult(LoginResult loginResult) {
			if (progressDialogDisplayed) {
				dismissProgressDialog();
			}
			
			switch(loginResult) {
			case AUTH_SUCCESS:
				Log.i(TAG, "Login successful...");
				displayDashboardView();
				break;
				
			case AUTH_FAIL:
				Log.w(TAG, "Authentication failed...");
				displayError(R.string.invalid_credentials);
				break;
			}
		}
    }
    
    private class ListenerImpl implements OnClickListener, OnCancelListener {

		@Override
		public void onClick(View btnClicked) {
			Log.d(TAG, "Button clicked!");
			switch (btnClicked.getId()) {
			case R.id.loginButton:
				if (!connChecker.isConnected(getActivity())) {
					displayError(R.string.no_connxn);
				}
				else if (!entriesValidated()) {
					displayError(R.string.invalid_or_empty_data);
				}
				else {
					performLogin();	
				}
				
				break;
			default:
				Log.e(TAG, "Unrecognised button click!");
			}
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			progressDialogDisplayed = false;
			if (loginTask != null) {
				Log.d(TAG, "Telling the background task to stop executing!");
				loginTask.cancel(true);
				
				loginTask = null;
			}			
		}
    }
}
