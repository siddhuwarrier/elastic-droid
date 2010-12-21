/**
 *  This file is part of ElasticDroid.
 *
 * ElasticDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * ElasticDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with ElasticDroid.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authored by siddhu on 18 Dec 2010
 */
package org.elasticdroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.http.ConnectionClosedException;
import org.elasticdroid.model.SecurityGroupsModel;
import org.elasticdroid.model.SshConnectorModel;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * Collects information from user and uses ConnectBot to connect to the instance using SSH.
 * @author siddhu
 *
 * 18 Dec 2010
 */
public class SshConnectorView extends GenericActivity implements OnClickListener{

	/** Connection data */
	private HashMap<String, String> connectionData;
	/** The hostname to connect to */
	private String hostname;
	/** Security groups available for the instance to connect to*/
	private String[] securityGroupNames;
	/** Selected AWS region */
	private String selectedRegion;
	
	/** Dialog box for displaying errors */
	private AlertDialog alertDialogBox;
	/**
	 * set to show if alert dialog displayed. Used to decide whether to restore
	 * progress dialog when screen rotated.
	 */
	private boolean alertDialogDisplayed;
	/** message displayed in {@link #alertDialogBox alertDialogBox}. */
	private String alertDialogMessage;
	/**
	 * boolean to indicate if an error that occurred is sufficiently serious to
	 * have the activity killed.
	 */
	private boolean killActivityOnError;
    /**Is progress bar displayed */
    private boolean progressDialogDisplayed;
	/** The model used to retrieve security group info */
	private SecurityGroupsModel securityGroupsModel;
	/** The model used to verify input and retrieve SSH URI */
	private SshConnectorModel sshConnectorModel;
	/** Information on security groups */
	private ArrayList<String> openPorts;
	/** The tag used in log messages in this class*/
	private static final String TAG = "org.elasticdroid.SshConnectorView";
	
	/**
	 * Called when activity is created.
	 * @param savedInstanceState if any
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); //call superclass onCreate
		
		Intent intent = this.getIntent();
    	//get all of the data stored in the intent
		try {
    		this.connectionData = (HashMap<String, String>)intent.getSerializableExtra(
    				"org.elasticdroid.EC2DashboardView.connectionData");
    	}
    	catch(Exception exception) {
        	//the possible exceptions are NullPointerException: the Hashmap was not found, or
        	//ClassCastException: the argument passed is not Hashmap<String, String>. In either case,
        	//just print out the error and exit. This is very inelegant, but this is a programmer's bug
    		Log.e(TAG, exception.getMessage());
    		finish(); //this will cause it to return to {@link EC2DisplayInstancesView}.
    	}
    	securityGroupNames = intent.getStringArrayExtra("securityGroups");
    	hostname = intent.getStringExtra("hostname");
    	selectedRegion = intent.getStringExtra("selectedRegion");
		
		// create and initialise the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); // create alert
																	// box to
		alertDialogBox.setButton(
				this.getString(R.string.loginview_alertdialogbox_button),
				new DialogInterface.OnClickListener() {
					// click listener on the alert box - unlock orientation when
					// clicked.
					// this is to prevent orientation changing when alert box
					// locked.
					public void onClick(DialogInterface arg0, int arg1) {
						alertDialogDisplayed = false;
						alertDialogBox.dismiss(); // dismiss dialog.
						// if an error occurs that is serious enough return the
						// user to the login
						// screen. THis happens due to exceptions caused by
						// programming errors and
						// exceptions caused due to invalid credentials.
						if (killActivityOnError) {
							Log.v(TAG, "Ich bin hier.");
							SshConnectorView.this.finish();
						}	
					}
				}
		);
		
		//initialise the display
		setContentView(R.layout.sshconnector); //tell the activity to set the xml file
		this.setTitle(connectionData.get("username")+ " (" + selectedRegion +")"); //set title
		
		View loginButton =(View)this.findViewById(R.id.sshConnectorLoginButton);
		loginButton.setOnClickListener(this);
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li></li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		//restore alertDialogDisplayed boolean
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = "
				+ alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		//was a progress dialog being displayed? Restore the answer to this question.
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		Log.v(this.getClass().getName() + ".onRestoreInstanceState", "progressDialogDisplayed:" + 
				progressDialogDisplayed);
		
		/*get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		//if there was a model executing when the object was destroyed, retained will be an 
		//instance of SecurityGroupsModel
		if (retained instanceof SecurityGroupsModel) {
			Log.i(TAG + ".onRestoreInstanceState()","Reclaiming previous " +
				"background task");
			securityGroupsModel = (SecurityGroupsModel)retained;
			securityGroupsModel.setActivity(this);//tell the model of the new activity created
		}
		else {
			securityGroupsModel = null;
			
			Log.v(TAG, "No model object, or model finished before activity " +
					"was recreated.");
			
			//now if there is no model anymore, and progressDialogDisplayed is set to true,
			//reset it to false, because the model finished executing before the restart
			if (progressDialogDisplayed) {
				progressDialogDisplayed = false;
			}
		}
		
		Log.d(TAG, "Restoring open ports data if any");
		
		try {
			openPorts = (ArrayList<String>) stateToRestore.getSerializable("openPorts");
		}
		catch(Exception exception) {
			openPorts = null;
		}
		
		//if we have openPorts data, populate the spinner
		if (openPorts != null) {
			populateSpinner();
			//populate spinner will have set the spinner to port 22
			//or to the first index
			//reposition the selected index
			((Spinner) findViewById(R.id.sshConnectorPortSpinner)).setSelection(
					stateToRestore.getInt("selectedPortPos"));
		}
		
		//if the user has entered new username, set that into EditText
		if (stateToRestore.getString("sshUsername") != null) {
			((EditText)findViewById(R.id.sshConnectorUsernameEditTextView)).setText(stateToRestore.
					getString("sshUsername"));
		}
		
		//set the pubkey auth checkbox
		((CheckBox)findViewById(R.id.sshConnectorUsePublicKeyAuth)).setChecked(
				stateToRestore.getBoolean("usePubkeyAuth"));
	}
	
	/**
	 * Executed when activity is resumed. Calls SecurityGroupsModel to get the list of open ports.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		//if there was a dialog box, display it
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
		else if ( (securityGroupsModel == null) && (openPorts == null)) {
			executeSecurityGroupsModel();
		}
	}
	
	/**
	 * Save state of the activity on destroy/stop.
	 * Saves:
	 * <ul>
	 * <li></li>
	 * </ul>
	 */
	@Override
	public void onSaveInstanceState(Bundle saveState) {
		// if a dialog is displayed when this happens, dismiss it
		if (alertDialogDisplayed) {
			alertDialogBox.dismiss();
		}
		//save the info as to whether dialog is displayed
		saveState.putBoolean("alertDialogDisplayed", alertDialogDisplayed);
		//save the dialog msg
		saveState.putString("alertDialogMessage", alertDialogMessage);
		
		//save the list of open ports 
		if (openPorts != null) {
			saveState.putSerializable("openPorts", openPorts);
			saveState.putInt("selectedPortPos",
					((Spinner) findViewById(R.id.sshConnectorPortSpinner)).
						getSelectedItemPosition());
		}
		//save if progress dialog is being displayed.
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
		
		//save the username entered if it is not the default user name
		String curUsername = ((EditText)findViewById(R.id.sshConnectorUsernameEditTextView)).
			getText().toString();
		if (!curUsername.equals(
				this.getString(R.string.ssh_defaultuser))) {
			saveState.putString("sshUsername", curUsername);
		}
		
		//save the state of the checkbox that specifies whether pubkey auth should be used or not
		saveState.putBoolean("usePubkeyAuth", ((CheckBox)findViewById(R.id.
				sshConnectorUsePublicKeyAuth)).isChecked());
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.ElasticIPsModel} Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (securityGroupsModel != null) {
			securityGroupsModel.setActivity(null);
			return securityGroupsModel;
		}
		
		return null;
	}
	
	/**
	 * Function that handles the display of a progress dialog. Overriden from
	 * Activity and not GenericActivity
	 * 
	 * @param id
	 *            Dialog ID - Special treatment for Constants.PROGRESS_DIALOG
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(this.getString(R.string.loginview_wait_dlg));
			dialog.setCancelable(false);
	
			progressDialogDisplayed = true;
			Log.v(this.getClass().getName(), "progress dialog displayed="
					+ progressDialogDisplayed);
	
			return dialog;
		}
		// if some other sort of dialog...
		return super.onCreateDialog(id);
	}

	/**
	 * Executes the model which will return the open ports assigned to this instance.
	 * Uses SecurityGroupModel
	 */
	private void executeSecurityGroupsModel() {
		Log.v(TAG + ".executeModel()", "Going to execute model!");

		Filter filter = new Filter("group-name").withValues(securityGroupNames);//get filters.
		//the filters say: get us info on the security groups with the following names.
		
		//note this model will show all ports, including ports that may not be accessible from
		//this IP address.
		securityGroupsModel = new SecurityGroupsModel(this, connectionData);
		securityGroupsModel.execute(new Filter[]{filter});
	}
	
	/**
	 * Executes the model which will:
	 * <ul>
	 * <li>Make sure that the port selected is accessible from the IP address specified.</li>
	 * <li>Return the SSH URI expected by ConnectBot
	 * </ul>
	 */
	private void executeSshConnectorModel() {
		Log.v(TAG + ".executeModel()", "Asking model to retrieve SSH URI for ConnectBot...");
		
		String username = ((EditText) findViewById(R.id.sshConnectorUsernameEditTextView)).
			getText().toString();
		
		int toPort = Integer.valueOf(((Spinner) findViewById(R.id.sshConnectorPortSpinner)).
			getSelectedItem().toString());
		
		sshConnectorModel = new SshConnectorModel(this,
				connectionData,
				username,
				hostname,
				toPort);
		
		sshConnectorModel.execute(securityGroupNames);
	}

	/**
	 * Process results returned by SecurityGroupsModel.
	 * 
	 * @param Result: Result produced by the AsyncTask model.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		//handle return of securityGroupsModel
		if (securityGroupsModel != null) {
			securityGroupsModel = null; //set the model to null. it's usefulness is done.
			
			//handle result
			if (result instanceof List<?>) {
				try {
					populateOpenPortsList((List<SecurityGroup>) result); //populate the spinner
				}
				catch(ClassCastException exception) {
					Log.e(TAG, exception.getMessage());
					finish();
				}
				
				populateSpinner(); //populate the spinner
			}
			else if (result instanceof AmazonServiceException) {
				// if a server error
				if (((AmazonServiceException) result).getErrorCode()
						.startsWith("5")) {
					alertDialogMessage = this.getString(R.string.loginview_server_err_dlg);
				} else {
					alertDialogMessage = this.getString(R.string.loginview_invalid_keys_dlg);
				}
				alertDialogDisplayed = true;
				killActivityOnError = false;//do not kill activity on server error
				//allow user to retry.
			} 
			else if (result instanceof AmazonClientException) {
				alertDialogMessage = this
						.getString(R.string.loginview_no_connxn_dlg);
				alertDialogDisplayed = true;
				killActivityOnError = false;//do not kill activity on connectivity error. allow 
				//client to retry.
			}
		}
		//handle return of sshConnectorModel
		else if (sshConnectorModel != null) {
			sshConnectorModel = null;
			
			if (result instanceof String) {
				Log.v(TAG, "SshConnectorModel returned: " + (String) result);
				
				
				Intent connectBotIntent = new Intent(Intent.ACTION_VIEW, 
						Uri.parse((String)result));
				
				try {
					connectBotIntent.putExtra(
							"usePubKeyAuth", 
							((CheckBox)findViewById(R.id.sshConnectorUsePublicKeyAuth)).
							isChecked());
					//pass nickname: the last name of the file's path seq
					startActivity(connectBotIntent);
				}
				catch(ActivityNotFoundException exception) {
					alertDialogMessage = this.getString(R.string.connectbot_not_installed);
					alertDialogDisplayed = true;
					//kill the activity when user closes the dialog
					killActivityOnError = true;
				} 
			}
			else if (result instanceof AmazonServiceException) {
				// if a server error
				if (((AmazonServiceException) result).getErrorCode()
						.startsWith("5")) {
					alertDialogMessage = this.getString(R.string.loginview_server_err_dlg);
				} else {
					alertDialogMessage = this.getString(R.string.loginview_invalid_keys_dlg);
				}
				alertDialogDisplayed = true;
				killActivityOnError = false;//do not kill activity on server error
				//allow user to retry.
			} 
			else if (result instanceof AmazonClientException) {
				alertDialogMessage = this
						.getString(R.string.loginview_no_connxn_dlg);
				alertDialogDisplayed = true;
				killActivityOnError = false;//do not kill activity on connectivity error. allow 
				//client to retry.
			}
			else if (result instanceof ConnectionClosedException) {
				alertDialogMessage = ((ConnectionClosedException)result).getMessage();
				alertDialogDisplayed = true;
				killActivityOnError = false;
			}
		}
		
		//display the alert dialog if the user set the displayed var to true
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();//show error
		}
	}
	
	/**
	 * Method called by processModelResults() to populate the openPorts ArrayList<String>
	 * with the SecurityGroups data returned by SecurityGroupsModel.
	 * 
	 * @param securityGroups Security groups returned by the model.
	 */
	private void populateOpenPortsList(List<SecurityGroup> securityGroups) {
		
		openPorts = new ArrayList<String>(); //(re)initialise openPorts
		
		//get the data to populate the spinner with
		//very crap O(n2) algo
		for (SecurityGroup securityGroup : securityGroups) {
			List<IpPermission> ipPermissions = securityGroup.getIpPermissions();
			for (IpPermission ipPermission : ipPermissions) {
				openPorts.add(String.valueOf(ipPermission.getToPort()));
			}
		}
		
		Collections.sort(openPorts); //sort by natural order, i.e. alphabetically
	}

	/**
	 * Method called to populate the open ports spinner in the UI
	 */
	private void populateSpinner() {
		if (openPorts == null) {
			return;
		}
		
		int selectedIndex = 0; //set selected index to index of port 22 if available.
		Spinner portSpinner = (Spinner) findViewById(R.id.sshConnectorPortSpinner);
		
		if (openPorts.contains("22")) {
			Log.d(TAG, "Found port 22 in openPorts.Setting as selected.");
			selectedIndex = openPorts.indexOf("22");
		}
		//create an ArrayAdapter<String> to hold this
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_spinner_item,
				openPorts);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		portSpinner.setAdapter(spinnerAdapter);
		portSpinner.setSelection(selectedIndex); //0 if port 22 unavailable, else indexof(port22)
	}
	
	/* Overriden methods */
	/**
	 * Handle back button. When back button pressed, we want the openPorts to be set to null
	 * so that it is recomputed when the user comes back in.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			openPorts = null; 
			//cancel
		}
		
		return super.onKeyDown(keyCode, event);
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View button) {
		// TODO Auto-generated method stub
		switch(button.getId()) {
		case R.id.sshConnectorLoginButton:
			EditText usernameEditText = (EditText) findViewById(R.id.
					sshConnectorUsernameEditTextView);
			
			//if no user name entered, do not proceed; warn user and exit
			if (usernameEditText.getText().toString().trim().equals("")) {
				usernameEditText.setError(getString(R.string.loginview_invalid_username_err));
			}
			else {
				executeSshConnectorModel();//execute the SSH connector model
			}
			break;
		}
	}
}
