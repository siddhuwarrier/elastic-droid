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
 * Authored by Siddhu Warrier on 14 Nov 2010
 */
package org.elasticdroid;

import java.util.ArrayList;
import java.util.Hashtable;

import org.elasticdroid.model.EC2DashboardModel;
import org.elasticdroid.utils.AWSConstants.EndPoints;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

/**
 * Class to display an EC2 Dashboard. The EC2 Dashboard shall display a ListView that contains the 
 * following information:
 * <ul>
 * <li>Number of running instances.</li>
 * <li>Number of stopped instances.</li>
 * <li>AMIs (query AMIs, view AMIs registered by you).</li>
 * <li>Number of Elastic IPs.</li>
 * <li>Number of security groups.</li>
 * <li>Number of private keys.</li>
 * 
 * for the selected region
 * </ul>
 * @author Siddhu Warrier
 *
 * 14 Nov 2010
 */
public class EC2DashboardView extends GenericListActivity {	
	/** AWS login details: username, access key, secret access key. Can be IAM username or AWS email address 
	 *  Not using ArrayList<String> cuz AsyncTask excepts String... as argument.
	 * */
	private String[] loginData;
	/**
	 * The data received from the model
	 */
	Hashtable<String, Integer> dashboardData;
	/** Reference to EC2DashboardModel object which does the credential checks and stores user details in 
	 * DB*/
	EC2DashboardModel ec2DashboardModel;
	/** Dialog box for credential verification errors */
	private AlertDialog alertDialogBox;
    /** set to show if alert dialog displayed. Used to decide whether to restore progress dialog
	 * when screen rotated. */
    private boolean alertDialogDisplayed;
    /** message displayed in {@link #alertDialogBox alertDialogBox}.*/
    private String alertDialogMessage;
	
	/**
	 * Called when the Activity is first started, or recreated (for instance when user turns screen around)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {        
    	super.onCreate(savedInstanceState);
    	
    	Intent intent = this.getIntent();
    	loginData = (String[]) intent.getCharSequenceArrayExtra("org.elasticdroid.LoginView.loginData");
    	
    	//if this onCreate method ahs been called *after* the destruction of the screen during the
    	//execution of the model, the model object would have been saved.
    	//restore it.
    	Object retained = getLastNonConfigurationInstance();
    	if (retained instanceof EC2DashboardModel) {
            Log.i(this.getClass().getName(), "Reclaiming previous background task.");
            ec2DashboardModel = (EC2DashboardModel) retained;
            ec2DashboardModel.setActivity(this); //tell loginModel that this is the new recreated activity
    	}
    	
        //create the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); //create alert box to
		
		//add a neutral OK button and set action.
		alertDialogBox.setButton(this.getString(R.string.loginview_alertdialogbox_button), new DialogInterface.OnClickListener() {			 
            //click listener on the alert box - unlock orientation when clicked.
			//this is to prevent orientation changing when alert box locked.
            public void onClick(DialogInterface arg0, int arg1) {
            	alertDialogDisplayed = false;
            	//if an error occurs, return the user to the login screen
            	finish();
            	Intent loginIntent = new Intent();
            	loginIntent.setClassName("org.elasticdroid", "org.elasticdroid.LoginView");
            	startActivity(loginIntent);
            	
            	alertDialogBox.dismiss();
            }
		});
		
        //set content view
    	setContentView(R.layout.ec2dashboard);
    	
    	//set the title
    	this.setTitle(loginData[0] + "(" + 
    			this.getString(R.string.ec2dashview_region) + ")");
	}
	
	/**
	 * Restore state of the activity on destroy/stop
	 * This method is called after we restore the login model.
	 */
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		super.onRestoreInstanceState(stateToRestore);
		
		//restore dialog data
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = " + alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		//if hte model is running, don't bother restoring. If not, restore
		if (ec2DashboardModel == null) {
			Log.v(this.getClass().getName(), "Restoring dashboardData...");
			
			try {
				dashboardData = (Hashtable<String, Integer>) stateToRestore.
					getSerializable("dashboardData");
			}
			catch(Exception exception) {
				//no data in the restore state. So just ignore.
				Log.e(this.getClass().getName(), "I should never come here.");
			}
		}
	}
	
	/**
	 * Resume operation, including re-populating the List if necessary,
	 * or calling the model to get the ListView data.
	 */
	@Override
	public void onResume() {
		super.onResume(); //call base class method
        
		if (alertDialogDisplayed) {
        	alertDialogBox.setMessage(alertDialogMessage);
        	alertDialogBox.show();
        	
        	//TODO retry. Or just have the user try to re-login.
        }
		else {
			//if we already have a Hashtable full of data and no model, just repopulate the list, don't
			//ask AWS
			if ( (dashboardData != null) && (ec2DashboardModel == null) ) {
				//if there is no model, just repopulate
				populateListView();
			}
			else if (ec2DashboardModel == null) {
	    		Log.v(this.getClass().getName(), "Starting model...");
	    		//TODO get the region endpoint data from the View.
	    		Hashtable<String, String> connectionData = new Hashtable<String, String>();
	    		connectionData.put("accessKey", loginData[1]);
	    		connectionData.put("secretAccessKey", loginData[2]);
	    		connectionData.put("endpoint", EndPoints.EC2_EU_WEST);
	    		
	    		ec2DashboardModel = new EC2DashboardModel(this);
	    		ec2DashboardModel.execute(connectionData);
	    	}
		}
		//the third case is the model is already executing...
	}
	/**
	 * Process the results returned by the model, 
	 * @see org.elasticdroid.GenericActivity#processModelResults(java.lang.Object)
	 */
	@Override
	public void processModelResults(Object result) {
		//dismiss the progress dialog. Processing complete!
		dismissDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		
		//if the result is an instance of Hashtable<?,?> populate listview
		if (result instanceof Hashtable<?, ?>) {
			dashboardData = (Hashtable<String,Integer>)result;
			//populate ListView
			populateListView();
		}
		else if (result instanceof AmazonServiceException) {
			alertDialogMessage = this.getString(R.string.loginview_invalid_keys_dlg);
			alertDialogDisplayed = true;
		}
		else if (result instanceof AmazonClientException) {
			alertDialogMessage = this.getString(R.string.loginview_no_connxn_dlg);
			alertDialogDisplayed = true;
		}
		else if (result instanceof IllegalArgumentException) {
			alertDialogMessage = this.getString(R.string.ec2dashview_illegal_arg_exception);
			alertDialogDisplayed = true;
		}
		
		//set the model to null. Important for when user destroys the screen by tilting it.
		ec2DashboardModel = null;
		
		//display the alert dialog if the user set the displayed var to true
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();//show error
		}
	}
	
	/**
	 * TODO Add comments
	 * @param result
	 */
	private void populateListView() {
		ArrayList<String> dashboardItems = new ArrayList<String>();
		
		//add entries to dashboard items
		dashboardItems.add(this.getString(R.string.ec2dashview_runninginstances) + 
			dashboardData.get("runningInstances"));
		dashboardItems.add(this.getString(R.string.ec2dashview_stoppedinstances) + 
			dashboardData.get("stoppedInstances"));
		dashboardItems.add(this.getString(R.string.ec2dashview_elasticip) + 
			dashboardData.get("elasticIp"));
		dashboardItems.add(this.getString(R.string.ec2dashview_securitygroups) + 
			dashboardData.get("securityGroups"));
		dashboardItems.add(this.getString(R.string.ec2dashview_keypairs) + 
			dashboardData.get("keyPairs"));
			
		//add the dashboard items to the list adapter to display.
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 
				(String[])dashboardItems.toArray(new String[ dashboardItems.size()]) ));
	}
	
	/**
	 * Save state of the activity on destroy/stop
	 */
	@Override
	public void onSaveInstanceState(Bundle saveState) {
		//if a dialog is displayed when this happens, dismiss it
		if (alertDialogDisplayed) {
			alertDialogBox.dismiss();
		}
		
		saveState.putBoolean("alertDialogDisplayed", alertDialogDisplayed);
		saveState.putString("alertDialogMessage", alertDialogMessage);
		
		//save the data only if the model is not presently executing
		if ((dashboardData != null) && (ec2DashboardModel == null)) {
			Log.v(this.getClass().getName(), "Saving dashboardData...");
			saveState.putSerializable("dashboardData", dashboardData);
		}
		//NOTE: THe loginData is int he intent, so not to worry about that.
		
		//call the superclass (Activity)'s save state method
		super.onSaveInstanceState(saveState);
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.EC2DashboardModel Async Task
	 * when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.v(this.getClass().getName(), "Object about to destroyed...");
        
		//if the model is being executed when the onDestroy method is called.
		if (ec2DashboardModel != null) {
			ec2DashboardModel.setActivity(null);
			return ec2DashboardModel;
		}
		return null;
	}
	
	/*
	 * Functions overridden from Activity to display Progress dialog. 
	 */
	/**
	 * Overriden from Activity and not GenericActivity!
	 * @param id Dialog ID - special treatment for ProgressDialog
	 * @param dialog - the dialog object itself. 
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
	}

	/**
	 *  
	 * Overriden from Activity and not GenericActivity
	 * @param id DIalog ID - Special treatment for Constants.PROGRESS_DIALOG
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
	        ProgressDialog dialog = new ProgressDialog(this);
	        dialog.setMessage(this.getString(R.string.loginview_wait_dlg));
	        dialog.setCancelable(false);
	        return dialog;
		}
		//if some other sort of dialog...
        return super.onCreateDialog(id);
	}
}
