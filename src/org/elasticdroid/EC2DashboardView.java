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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.model.AWSUtilities;
import org.elasticdroid.model.EC2DashboardModel;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

/**
 * Class to display an EC2 Dashboard. Extends {@link GenericListActivity}, as the principal
 * element is a ListView. It also implements {@link OnItemClickListener}
 * 
 * The EC2 Dashboard shall display a ListView that contains the following information:
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
public class EC2DashboardView extends GenericListActivity implements OnItemSelectedListener {	
	/** AWS login details: username, access key, secret access key. Can be IAM username or AWS email address 
	 *  Not using ArrayList<String> cuz AsyncTask excepts String... as argument.
	 * */
	private HashMap<String, String> connectionData;
	/**
	 * The data received from the model
	 */
	HashMap<String, Integer> dashboardData;
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
    /** the available AWS regions */
    private HashMap<String, String> regionData;
    /** the default region set by the user. Default: not set; set randomly first time by Vuew*/
    private String selectedRegion;
    /** boolean to indicate if progress dialog is displayed*/
    private boolean progressDialogDisplayed;
	
	/**
	 * Called when the Activity is first started, or recreated (for instance when user turns screen around)
	 * Suppressing warnings when deserialising object and casting to Hashmap<String, String>
	 * @param savedInstanceState the saved instance state which contains information written in 
	 * onSaveInstanceState when the activity is destroyed and recreated. 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {        
    	super.onCreate(savedInstanceState); //call superclass onCreate
    	
    	//this class is called from LoginView, and is passed an argument in the intent.Retrieve it.
    	Intent intent = this.getIntent();
    	try {
    		this.connectionData = (HashMap<String, String>)intent.getSerializableExtra(
    				"org.elasticdroid.LoginView.connectionData");
    	}
    	//the possible exceptions are NullPointerException: the Hashmap was not found, or
    	//ClassCastException: the argument passed is not Hashmap<String, String>. In either case,
    	//just print out the error and exit. This is very inelegant, but this is a programmer's bug
    	catch(Exception exception) {
    		Log.e(this.getClass().getName(), exception.getMessage());
    		finish(); //kill the application, and off to bed.
    	}
    	
        //create and initialise the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); //create alert box to
		alertDialogBox.setButton(this.getString(R.string.loginview_alertdialogbox_button), 
				new DialogInterface.OnClickListener() {			 
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
		
    	setContentView(R.layout.ec2dashboard);
    	//set the title to the username of the person currently logged in.
    	this.setTitle(connectionData.get("username"));
	}
	
	/**
	 * Restore state of the activity on destroy/stop
	 * This method is called after we restore the login model.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		super.onRestoreInstanceState(stateToRestore);
		
		//restore dialog data
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = " + alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		
		Log.v(this.getClass().getName(), "onRestoreInstanceState:progress dialog displayed=" + progressDialogDisplayed);
		
		//if the model is running, don't bother restoring dashboard data. If not, restore
		if (ec2DashboardModel == null) {
			Log.v(this.getClass().getName(), "Restoring dashboardData...");
			
			try {
				dashboardData = (HashMap<String, Integer>) stateToRestore.
					getSerializable("dashboardData");
			}
			catch(Exception exception) {
				//no data in the restore state. So just ignore.
				Log.e(this.getClass().getName(), "I should never come here:" + exception);
				finish();
			}
		}
		
		Log.v(this.getClass().getName(), "Restoring regionData...");
		//restore region data
		try {
			regionData = (HashMap<String, String>) stateToRestore.
			getSerializable("regionData");
		}
		catch(Exception exception) {
			//no data in the restore state. So just ignore.
			Log.e(this.getClass().getName(), "I should never come here:" + exception);
			finish();
		}
		//restore default region
		selectedRegion = stateToRestore.getString("selectedRegion");
	}
	
	/**
	 * Resume operation, including re-populating the List if necessary,
	 * or calling the model to get the ListView data.
	 */
	@Override
	public void onResume() {
		super.onResume(); //call base class method
        
    	//restore model object if one was saved. This happens when the activity is destroyed
    	//when the model is executing in a background thread.
    	Object retained = getLastNonConfigurationInstance();
    	//redundant if condition. Nothing else is retained. Can be removed if performance is
    	//an issue.
    	if (retained instanceof EC2DashboardModel) {
            Log.i(this.getClass().getName(), "Reclaiming previous background task.");
            //restore the reference to the Object.
            ec2DashboardModel = (EC2DashboardModel) retained;
            //when saving instance state, we told the model that the activity it was referring to
            //no longer existed. Let the model know htat a new activity has taken its place.
            //the model will then call the new activity's 
            ec2DashboardModel.setActivity(this); //tell loginModel that this is the new recreated activity
    	}
    	else {
    		//no model reference, or model finished execution before the activity restarted.
    		Log.v(this.getClass().getName(), "No model object, or model finished before activity " +
    				"was recreated.");
    		ec2DashboardModel = null;
    		//if the progress dialog is being displayed, kill it.
    		if (progressDialogDisplayed) {
    			progressDialogDisplayed = false;
    			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
    		}
    	}
		
		//(re)populate the spinner with data on the regions.
		//if we haven't got the region info yet, get it. Have the model query.
		//this is not done in background as we need this info before displaying the data.
		if (regionData == null) {
			//get the available regions and default region.
			//if default region does not exist, set one by default. Heehee
			retrieveRegionData();
		}
		//populate the android spinner
		 ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, 
				 R.layout.regionspinnerrow, 
				 (String[])regionData.keySet().toArray(new String[regionData.keySet().size()]));
		 spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		 
		 //get the spinner and set the adapter as ITS adapter
		 Spinner regionSpinner = ((Spinner) findViewById(R.id.ec2DashboardRegionSpinner));
		 regionSpinner.setAdapter(spinnerAdapter);
		 //listen to changes.
		 regionSpinner.setOnItemSelectedListener(this);
		 
		 //set the spinner to the selected region (which is the default region if nothing has been
		 //selected by the user
		 //for this, we need to iterate through the regions and find the position of the selected region
		 //i am sure that AWS won't have more than 2^15 regions in the near future. So i'll be an a**h**e
		 //and set the position to be a byte. Ha!
		 short selectedRegionPosition = 0;
		 for (String regionName : regionData.keySet()) {
			 if (regionName.equals(selectedRegion)) {
				 break;
			 }
			 selectedRegionPosition ++;
		 }
		 //just-in-case error check
		 if (selectedRegionPosition == regionData.keySet().size()) {
			 Log.e(this.getClass().getName(), "Could not find selected region in regions list. WTF!");
			 finish();
		 }
		 //set the selection.
		 regionSpinner.setSelection(selectedRegionPosition);
		 
		
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
	    		executeModel();
	    	}
		}
		//the third case is the model is already executing...
	}
	
	/**
	 * Execute the model
	 */
	private void executeModel() {
		ec2DashboardModel = new EC2DashboardModel(this);
		//add the endpoint for this region to connectionData
		//it's not nice to modify a member like this, now, is it?
		connectionData.put("endpoint", regionData.get(selectedRegion));
		ec2DashboardModel.execute(connectionData);
	}
	
	/**
	 * Process the results returned by the model, 
	 * @see org.elasticdroid.GenericActivity#processModelResults(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		Log.v(this.getClass().getName(), "Processing model results...");
		//dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		//if the result is an instance of Hashtable<?,?> populate listview
		if (result instanceof HashMap<?, ?>) {
			try {
				dashboardData = (HashMap<String,Integer>)result;
			}
			catch(ClassCastException exception) {
				Log.e(this.getClass().getName(), "Result returned from model should be HashMap:" + 
						exception.getMessage());
			}
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
		Log.v(this.getClass().getName(), "OnSaveInstanceState: progressDialogDisplayed=" + 
				progressDialogDisplayed);
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
		
		//save the data only if the model is not presently executing
		if ((dashboardData != null) && (ec2DashboardModel == null)) {
			Log.v(this.getClass().getName(), "Saving dashboardData...");
			saveState.putSerializable("dashboardData", dashboardData);
		}
		//similarly write the region data so we do not have to query AWS every time
		//the user twists his screen around
		if (regionData != null) {
			saveState.putSerializable("regionData", regionData);
		}
		//save the default region so that we do not have to query the DB every time
		//the user twists his screen around
		saveState.putString("selectedRegion", selectedRegion);
		
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

	/**
	 * Function that handles the display of a progress dialog. 
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
	
	@Override
	protected void onPrepareDialog (int id, Dialog dialog, Bundle args) {
		super.onPrepareDialog(id, dialog);
		
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
			progressDialogDisplayed = true;
			Log.v(this.getClass().getName(), "progress dialog displayed=" + 
					progressDialogDisplayed);
		}
	}
	
	private void retrieveRegionData() {
		String defaultRegion = null;
		ElasticDroidDB elasticDroidDB = new ElasticDroidDB(this);
		//get the list of regions
		try {
			regionData = AWSUtilities.getRegions(connectionData);
		}
		catch(RuntimeException exception) {
			//error out and fuck off
			Log.e(this.getClass().getName(), "This should not have happened:" + 
					exception.getMessage());
			finish();
		}
		
		//try and get the default region from the DB for the user.
		try {
			defaultRegion = elasticDroidDB.getDefaultRegion(connectionData.
					get("username"));
		} 
		catch (SQLException e) { //error out and fuck off
			Log.e(this.getClass().getName(), "This should not have happened:" + e.getMessage());
			finish();
		}
		
		//if no default region, set the default region to be the first of the returned regions
		//and write it to the DB.
		if (defaultRegion == null) {
			//set the default region to the first region in the RegionsList
			defaultRegion = regionData.keySet().iterator().next();
			Log.v(this.getClass().getName(), "Setting " + defaultRegion + " as default region.");
			
			//write the newly defined random default region in as default region.
			//TODO user should be able to change this.
			elasticDroidDB.setDefaultRegion(connectionData.get("username"), defaultRegion);
		}
		else {
			//TODO check if the default region exists among the regions.
		}
		
		//set the selectedRegion as default region.
		selectedRegion = defaultRegion;
	}

	/* (non-Javadoc)
	 * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android.widget.AdapterView, android.view.View, int, long)
	 * This function is executed every time we rotate the screen! so check and execute oinly if there is a change.
	 */
	@Override
	public void onItemSelected(AdapterView<?> selectedItem, View view, int pos,
			long id) {
		//has selection changed
		String selectedItemText = ((Spinner)findViewById(R.id.ec2DashboardRegionSpinner)).
			getItemAtPosition(pos).toString();
		Log.v(this.getClass().getName(), "Selected region: " + selectedRegion + 
				", Selected item text: " + selectedItemText);
		
		if (!selectedRegion.equals(selectedItemText)) {
			Log.v(this.getClass().getName(), "Region selected: " + selectedRegion);
			selectedRegion = ((Spinner)findViewById(R.id.ec2DashboardRegionSpinner)).
				getItemAtPosition(pos).toString();
			//repopulate ListView with data for this region
			executeModel();
		}
	}

	/* (non-Javadoc)
	 * @see android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android.widget.AdapterView)
	 */
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
