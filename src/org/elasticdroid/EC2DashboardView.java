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

import static org.elasticdroid.utils.ResultConstants.RESULT_ERROR;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.model.EC2DashboardModel;
import org.elasticdroid.model.RetrieveRegionModel;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.DialogConstants;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

/**
 * Class to display an EC2 Dashboard. Extends {@link GenericListActivity}, as
 * the principal element is a ListView. It also implements
 * {@link OnItemClickListener}
 * 
 * The EC2 Dashboard shall display a ListView that contains the following
 * information:
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
 * 
 * @author Siddhu Warrier
 * 
 *         14 Nov 2010
 */
public class EC2DashboardView extends GenericListActivity implements OnItemSelectedListener {
	/**
	 * AWS login details: username, access key, secret access key. Can be IAM
	 * username or AWS email address Not using ArrayList<String> cuz AsyncTask
	 * excepts String... as argument.
	 * */
	private HashMap<String, String> connectionData;
	/**
	 * Flag set to true in onCreate
	 */
	boolean onCreateExecuted;
	/**
	 * The data received from the model
	 */
	private HashMap<String, Integer> dashboardData;
	/**
	 * Reference to EC2DashboardModel object which does the credential checks
	 * and stores user details in DB
	 */
	private EC2DashboardModel ec2DashboardModel;
	/**
	 * Reference to RetrieveRegionModel object
	 */
	private RetrieveRegionModel retrieveRegionModel;
	
	/** Dialog box for credential verification errors */
	private AlertDialog alertDialogBox;
	/**
	 * set to show if alert dialog displayed. Used to decide whether to restore
	 * progress dialog when screen rotated.
	 */
	private boolean alertDialogDisplayed;
	/** message displayed in {@link #alertDialogBox alertDialogBox}. */
	private String alertDialogMessage;
	/** the available AWS regions */
	private HashMap<String, String> regionData;
	/**
	 * the default region set by the user. Default: not set; set randomly first
	 * time by Vuew
	 */
	private String selectedRegion;
	/**
	 * boolean to indicate if an error that occurred is sufficiently serious to
	 * have the activity killed.
	 */
	private boolean killActivityOnError;
	/**
	 * hack
	 */
	private boolean defaultRegionChanged;
	/**
	 * Log tag
	 */
	private static final String TAG = "org.elasticdroid.EC2DashboardView";
	

	/**
	 * Called when the Activity is first started, or recreated (for instance
	 * when user turns screen around) Suppressing warnings when deserialising
	 * object and casting to Hashmap<String, String>
	 * 
	 * @param savedInstanceState
	 *            the saved instance state which contains information written in
	 *            onSaveInstanceState when the activity is destroyed and
	 *            recreated.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // call superclass onCreate

		// this class is called from LoginView, and is passed an argument in the
		// intent.Retrieve it.
		Intent intent = this.getIntent();
		try {
			this.connectionData = (HashMap<String, String>) intent
					.getSerializableExtra("org.elasticdroid.LoginView.connectionData");
		}
		// the possible exceptions are NullPointerException: the Hashmap was not
		// found, or
		// ClassCastException: the argument passed is not Hashmap<String,
		// String>. In either case,
		// just print out the error and exit. This is very inelegant, but this
		// is a programmer's bug
		catch (Exception exception) {
			Log.e(this.getClass().getName(), exception.getMessage());
			finish(); // kill the application, and off to bed.
		}
		
		// restore model object if one was saved. This happens when the activity
		// is destroyed
		// when the model is executing in a background thread.
		Object retained = getLastNonConfigurationInstance();
		// redundant if condition. Nothing else is retained. Can be removed if
		// performance is
		// an issue.
		if (retained instanceof EC2DashboardModel) {
			Log.i(this.getClass().getName(),
					"Reclaiming previous background task.");
			// restore the reference to the Object.
			ec2DashboardModel = (EC2DashboardModel) retained;
			// when saving instance state, we told the model that the activity
			// it was referring to
			// no longer existed. Let the model know htat a new activity has
			// taken its place.
			// the model will then call the new activity's
			ec2DashboardModel.setActivity(this); // tell loginModel that this is
													// the new recreated
													// activity
		} 
		else if (retained instanceof RetrieveRegionModel) {
			Log.i(this.getClass().getName(),
			"Reclaiming previous background RetrieveRegionModel task.");
			retrieveRegionModel = (RetrieveRegionModel) retained;
			retrieveRegionModel.setActivity(this);
		}
		else {
			//onResume can also be executed on the press of the home button
			//in whic case we have not lost our object references. Therefore,
			//disable your progress bar Only IF there are no model objects.
			if ((retrieveRegionModel == null) && (ec2DashboardModel == null)) {
				Log.v(this.getClass().getName(),
						"No model object, or model finished before activity "
								+ "was recreated.");
				
				// if the progress dialog is being displayed, kill it.
				if (progressDialogDisplayed) {
					progressDialogDisplayed = false;
					removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
				}
			}
		}

		// create and initialise the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); // create alert dialog box
		alertDialogBox.setCancelable(false);								
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
							EC2DashboardView.this.finish();
							Intent loginIntent = new Intent();
							loginIntent.setClassName("org.elasticdroid",
									"org.elasticdroid.LoginView");
							startActivity(loginIntent);
						}
					}
				});

		setContentView(R.layout.ec2dashboard);
		// set the title to the username of the person currently logged in.
		this.setTitle("Logged in as " +  connectionData.get("username"));
		
		onCreateExecuted = true;
	}

	/**
	 * Restore state of the activity on destroy/stop This method is called when
	 * the activity is restarted.
	 * 
	 * Restores:
	 * <ul>
	 * <li>boolean to indicate if alert dialog box is displayed.</li>
	 * <li>alert dialog box string.</li>
	 * <li>boolean to indicate if progress dialog is displayed.</li>
	 * <li>List of regions available. This doesn't have to be queried everytime,
	 * as AWS are unlikely to add new regions in the lifetime of this app. This
	 * improves responsiveness.</li>
	 * </ul>
	 * 
	 * It restores
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		super.onRestoreInstanceState(stateToRestore);

		// restore dialog data
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = "
				+ alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		//restore progress dialog
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		Log.v(this.getClass().getName(),
				"onRestoreInstanceState:progress dialog displayed="
						+ progressDialogDisplayed);

		// restore dashboard data if any.
		Log.v(this.getClass().getName(), "Restoring dashboardData...");

		try {
			dashboardData = (HashMap<String, Integer>) stateToRestore
					.getSerializable("dashboardData");
		} catch (Exception exception) {
			// no data in the restore state. So just ignore.
			Log.e(this.getClass().getName(), "I should never come here:"
					+ exception);
			finish();
		}

		// restore region data
		Log.v(this.getClass().getName(), "Restoring regionData...");
		try {
			regionData = (HashMap<String, String>) stateToRestore
					.getSerializable("regionData");
		} catch (Exception exception) {
			// no data in the restore state. So just ignore.
			regionData = null;
			//onResume will get this data later
		}

		// restore default region
		selectedRegion = stateToRestore.getString("selectedRegion");
	}

	/**
	 * Resume operation of Activity. This method performs the following tasks:
	 * <ul>
	 * <li>Gets a reference to the model (
	 * {@link org.elasticdroid.model.EC2DashboardModel Async Task and points it
	 * to the activity again.Only if the Activity was destroyed and recreated.
	 * </li> <li>Kills progress dialog if the model finished executing before
	 * the activity came back up. Only if the Activity was destroyed and
	 * recreated.</li> <li>Restores region data, or queries model to get region
	 * data if none.</li> <li>Re-displays alert dialog box if there was an alert
	 * dialog box when activity destroyed. Only if the Activity was destroyed
	 * and recreated.</li>
	 * </ul>
	 */
	@Override
	public void onResume() {
		super.onResume(); // call base class method

		// do not execute any of this if the default region has been changed.
		// this is a bit of a hack but has been figured out to be the best way
		// to ensure that data is loaded properly when the default region is
		// changed.
		// that is, we switch the user automatically to the new default region.
		// Too tired to explain
		// but trust me, leave this in here!
		if (defaultRegionChanged) {
			defaultRegionChanged = false;
			return;
		}
		
		// restore alert dialog box if any.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		} else {
			//check if we need to get region data.
			//if we have neither region data, nor a running retrieveregionmodel
			//execute retrieve region model
			if (regionData == null) {
				//execute retrieveregionmodel unless not executing already
				if (retrieveRegionModel == null) {
					executeRetrieveRegionModel();
				}
			}
			else {//we have region data
				//populate the region spinner
				//firstExecute is set only when onCreate is executed,
				if (onCreateExecuted) {
					onCreateExecuted = false;
					populateRegionSpinner();
				}
				//execute 
			}
			if ((regionData == null) && (retrieveRegionModel == null)) {
				if (onCreateExecuted) {
					onCreateExecuted = false;
					executeRetrieveRegionModel();
				}
			}
			else if ( (regionData != null) && (retrieveRegionModel == null) ) {
				if (onCreateExecuted) {
					onCreateExecuted = false;
					populateRegionSpinner();
				}
			}
		}
	}
	
	/**
	 * Save state of the activity on destroy/stop
	 */
	@Override
	public void onSaveInstanceState(Bundle saveState) {
		// if a dialog is displayed when this happens, dismiss it
		if (alertDialogDisplayed) {
			alertDialogBox.dismiss();
		}

		saveState.putBoolean("alertDialogDisplayed", alertDialogDisplayed);
		saveState.putString("alertDialogMessage", alertDialogMessage);
		Log.v(this.getClass().getName(),
				"OnSaveInstanceState: progressDialogDisplayed="
						+ progressDialogDisplayed);
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);

		// save the data only if the model is not presently executing
		if ((dashboardData != null) && (ec2DashboardModel == null)) {
			Log.v(this.getClass().getName(), "Saving dashboardData...");
			saveState.putSerializable("dashboardData", dashboardData);
		}
		// similarly write the region data so we do not have to query AWS every
		// time
		// the user twists his screen around
		if (regionData != null) {
			saveState.putSerializable("regionData", regionData);
		}
		// save the default region so that we do not have to query the DB every
		// time
		// the user twists his screen around
		saveState.putString("selectedRegion", selectedRegion);

		// NOTE: THe loginData is int he intent, so not to worry about that.

		// call the superclass (Activity)'s save state method
		super.onSaveInstanceState(saveState);
	}

	/**
	 * Save reference to {@link org.elasticdroid.model.EC2DashboardModel Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.v(this.getClass().getName(), "Object about to destroyed...");

		//we know only one model can execute at a given time.
		// if the model is being executed when the onDestroy method is called.
		if (ec2DashboardModel != null) {
			Log.v(this.getClass().getName() + ".onRetainNonConfigInstance()", "Saving " +
			"ec2DashboardModel");
			
			ec2DashboardModel.setActivityNull();
			return ec2DashboardModel;
		}
		else if (retrieveRegionModel != null) {
			Log.v(this.getClass().getName() + ".onRetainNonConfigInstance()", "Saving " +
					"retrieveRegionModel");
			retrieveRegionModel.setActivityNull();
			return retrieveRegionModel;
		}
		//both are null, return null
		return null;
	}

	/**
	 * Execute the model to retrieve the list of regions. The model
	 * runs in a different thread and calls processModelResults when done.
	 */
	private void executeRetrieveRegionModel() {
		retrieveRegionModel = new RetrieveRegionModel(this);
		retrieveRegionModel.execute(new HashMap<?,?>[]{connectionData});
	}

	/**
	 * Execute the model to retrieve EC2 data for the selected region. The model
	 * runs in a different thread and calls processModelResults when done.
	 */
	private void executeEC2DashboardModel() {
		ec2DashboardModel = new EC2DashboardModel(this);		
		
		connectionData.put("endpoint", regionData.get(selectedRegion));
		ec2DashboardModel.execute(new HashMap<?, ?>[] { connectionData });
		
	}

	/**
	 * Process the results returned by the model,
	 * 
	 * @see org.elasticdroid.tpl.GenericActivity#processModelResults(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		Log.v(this.getClass().getName(), "Processing model results...");
		
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		if (result == null) {
			Toast.makeText(this, Html.fromHtml(this.getString(R.string.cancelled)), Toast.
					LENGTH_LONG).show();
			
			return; //do not execute the rest of this method.
		}

		//check which model returned. Only one model can be executing at a time.
		//model to retrieve regions was executed.
		if (retrieveRegionModel != null) {
			//set reference to model to null
			retrieveRegionModel = null;
			
			if (result instanceof HashMap<?, ?>) {
				try {
					regionData = (HashMap<String, String>) result;
				} catch (ClassCastException exception) {
					Log.e(this.getClass().getName(),
							"Result returned from model should be HashMap:"
									+ exception.getMessage());
					
					finish();//kill activity
				}
				
				populateRegionSpinner();
				
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
				killActivityOnError = false;//do not kill activity on connectivity error. allow client 
				//to retry.
			} 
			else if (result instanceof IllegalArgumentException) {
				alertDialogMessage = this
						.getString(R.string.ec2dashview_illegal_arg_exception);
				alertDialogDisplayed = true;
				killActivityOnError = true;
			}
		}
		//it was the dashboard model
		else {
			//set model reference to null
			ec2DashboardModel = null;
	
			// if the result is an instance of Hashtable<?,?> populate listview
			if (result instanceof HashMap<?, ?>) {
				try {
					dashboardData = (HashMap<String, Integer>) result;
				} catch (ClassCastException exception) {
					Log.e(this.getClass().getName(),
							"Result returned from model should be HashMap:"
									+ exception.getMessage());
					
					finish();//kill activity
				}
				// populate ListView
				populateEC2Dashboard();
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
				killActivityOnError = false;//do not kill activity on connectivity error. allow client 
				//to retry.
			} 
			else if (result instanceof IllegalArgumentException) {
				alertDialogMessage = this
						.getString(R.string.ec2dashview_illegal_arg_exception);
				alertDialogDisplayed = true;
				killActivityOnError = true;
			}
		}
		
		// display the alert dialog if the user set the displayed var to true
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();// show error
		}
	}

	/**
	 * Private method to repopulate spinners. gets region data if absent
	 */
	private void populateRegionSpinner() {
		
		String defaultRegion = null;
		ElasticDroidDB elasticDroidDb = new ElasticDroidDB(this);
		//get the default region
		try {
			defaultRegion = elasticDroidDb.getDefaultRegion(connectionData.
					get("username"));
		} catch (SQLException e) {
			Log.e(this.getClass().getName() + ".populateRegionSpinner", "SQL Exception.Exiting: "+ 
					e.getMessage());
		}

		//if there is no default region, set the first item in regionData as the default region
		if (defaultRegion == null) {
			//this is still O(1). Gingerbread didn't like my unsafe class casts.
			for (String region : regionData.keySet()) {
				defaultRegion = region;
				break;
			}
			elasticDroidDb.setDefaultRegion(connectionData.get("username"), defaultRegion);
		}
		
		//if we dont have an already selected region, set it to default region
		if (selectedRegion == null) {
			selectedRegion = defaultRegion;
		}
		
		// populate the android spinner with region data
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
				R.layout.regionspinneritem, (String[]) regionData.keySet()
						.toArray(new String[regionData.keySet().size()]));
		spinnerAdapter.setDropDownViewResource(R.layout.regionspinnerdropdownitem);
	
		// get the spinner and set the adapter as ITS adapter
		Spinner regionSpinner = ((Spinner) findViewById(R.id.ec2DashboardRegionSpinner));
		regionSpinner.setAdapter(spinnerAdapter);
		// listen to changes.
		regionSpinner.setOnItemSelectedListener(this);
		// set the spinner to the selected region (which is the default region
		// if nothing has been
		// selected by the user
		// for this, we need to iterate through the regions and find the
		// position of the selected region
		// i am sure that AWS won't have more than 2^15 regions in the near
		// future. So i'll be an a**h**e
		// and set the position to be a byte. Ha!
	
		Log.i(this.getClass().getName(), "OnResume(): Selected region: "
				+ selectedRegion);
		short selectedRegionPosition = 0;
		for (String regionName : regionData.keySet()) {
			if (regionName.equals(selectedRegion)) {
				break;
			}
			selectedRegionPosition++;
		}
		// just-in-case error check
		if (selectedRegionPosition == regionData.keySet().size()) {
			Log.e(this.getClass().getName(),
					"Could not find selected region in regions list. WTF!");
			finish();
		}
		// set the selection.
		Log.i(this.getClass().getName(),
				"onResume: found selected region in list at pos: "
						+ selectedRegionPosition);
		regionSpinner.setSelection(selectedRegionPosition);
		
		//if we have data, don't re-execute
		if ((dashboardData != null) && (ec2DashboardModel == null)) {
			// if there is no model, just repopulate
			populateEC2Dashboard();
		} else if (ec2DashboardModel == null) {
			Log.v(this.getClass().getName(), "Starting model...");
			executeEC2DashboardModel();
		}
		//third condition: dashboard model already executing
	}

	/**
	 * Populates the list view with EC2 dashboard data for the selected region.
	 * This method is called by {@link EC2DashboardView.processModelResults}.
	 */
	private void populateEC2Dashboard() {
		ArrayList<String> dashboardItems = new ArrayList<String>();

		// add entries to dashboard items
		dashboardItems.add(this
				.getString(R.string.ec2dashview_runninginstances)
				+ dashboardData.get("runningInstances"));
		dashboardItems.add(this
				.getString(R.string.ec2dashview_stoppedinstances)
				+ dashboardData.get("stoppedInstances"));
		dashboardItems.add(this.getString(R.string.ec2dashview_elasticip)
				+ dashboardData.get("elasticIp"));
		dashboardItems.add(this.getString(R.string.ec2dashview_securitygroups)
				+ dashboardData.get("securityGroups"));
		dashboardItems.add(this.getString(R.string.ec2dashview_keypairs)
				+ dashboardData.get("keyPairs"));

		// add the dashboard items to the list adapter to display.
		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,
				(String[]) dashboardItems.toArray(new String[dashboardItems
						.size()])));
	}

	/**
	 * Overriden listen method to capture clicks on the list item. Allows users
	 * to view lists of:
	 * <ul>
	 * <li>Running instances</li>
	 * <li><i>stopped instances (TBI)</i></li>
	 * <li><i>Elastic IPs (TBI)</i></li>
	 * <li><i>Security Groups (TBI)</i></li>
	 * <li><i>Keypairs (TBI)</i></li>
	 * </ul>
	 * 
	 * TBI= to be implemented
	 */
	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		String selectedItem = list.getItemAtPosition(position).toString();
		Log.v(this.getClass().getName() + "onListItemClick:",
				"Selected list item: " + selectedItem);

		selectedItem = selectedItem.split(":")[0] + ": "; // split the item out
															// to remove the
															// number
		// and get the text
		
		Intent displayListIntent = new Intent();
		
		if (selectedItem.equals(this.getString(
				R.string.ec2dashview_runninginstances))) {
			displayListIntent.setClassName("org.elasticdroid",
			"org.elasticdroid.EC2DisplayInstancesView");
			displayListIntent.putExtra(
					"org.elasticdroid.EC2DashboardView.connectionData",
					connectionData); // aws connection info
			
			Log.v(TAG + ".onListItemClick:",
					"Will now show list of running " + "instances.");

			displayListIntent.putExtra("listType",
					InstanceStateConstants.RUNNING);
			displayListIntent.putExtra("selectedRegion", selectedRegion); // selected region
		}
		else if (selectedItem.equals(this.getString(R.string.ec2dashview_stoppedinstances))) {
			displayListIntent.setClassName("org.elasticdroid",
			"org.elasticdroid.EC2DisplayInstancesView");
			displayListIntent.putExtra("org.elasticdroid.EC2DashboardView.connectionData",
					connectionData); // aws connection info
			
			Log.v(this.getClass().getName() + ".onListItemClick:",
					"Will now show list of stopped " + "instances.");
			
			displayListIntent.putExtra("listType", InstanceStateConstants.STOPPED);
			displayListIntent.putExtra("selectedRegion", selectedRegion); // selected region
		}
		else if (selectedItem.equals(this.getString(R.string.ec2dashview_elasticip))) {
			Log.v(TAG, "Selected item: " + selectedItem);
			displayListIntent.setClassName("org.elasticdroid", "org.elasticdroid.ElasticIPsView");
			
			displayListIntent.putExtra("org.elasticdroid.EC2DashboardView.connectionData",
					connectionData); // aws connection info
			displayListIntent.putExtra("selectedRegion", selectedRegion); // selected region
		} else if (selectedItem.equals(this.getString(R.string.ec2dashview_securitygroups))) {
			Log.v(TAG, "Selected item: " + selectedItem);
			displayListIntent.setClassName("org.elasticdroid", "org.elasticdroid.SecurityGroupsView");
			
			displayListIntent.putExtra("org.elasticdroid.EC2DashboardView.connectionData",
					connectionData); // aws connection info
			displayListIntent.putExtra("selectedRegion", selectedRegion); // selected region
		}
		else {
			return;
		}
		
		startActivityForResult(displayListIntent, 0); //second arg ignored.
	}

	/**
	 * Method to handle the selection of an item in the region spinner box. If a
	 * different region is selected, it calls the model to get data for the
	 * selected region. Implementation for interface @link
	 * {@link OnItemSelectedListener}.
	 */
	@Override
	public void onItemSelected(AdapterView<?> selectedItem, View view, int pos,
			long id) {
		// has selection changed
		String selectedItemText = ((Spinner) findViewById(R.id.ec2DashboardRegionSpinner))
				.getItemAtPosition(pos).toString();
		Log.v(this.getClass().getName(), "Selected region: " + selectedRegion
				+ ", Selected item text: " + selectedItemText);

		if (!selectedRegion.equals(selectedItemText)) {
			Log.v(this.getClass().getName(), "Region selected: "
					+ selectedRegion);
			selectedRegion = ((Spinner) findViewById(R.id.ec2DashboardRegionSpinner))
					.getItemAtPosition(pos).toString();
			// repopulate ListView with data for this region
			executeEC2DashboardModel();
		}
	}

	/**
	 * Empty method called if the region spinner box is invoked and nothing is
	 * selected. Nothing to do here.
	 */
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	/**
	 * Overridden method to display the menu on press of the menu key
	 * 
	 * Inflates and displays dashboard menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.dashboard_menu, menu);
		return true;
	}

	/**
	 * Overriden method to handle selection of menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem selectedItem) {
		switch (selectedItem.getItemId()) {
		case R.id.dashboard_menuitem_another_user:
			finish(); // kill this activity and start login activity
			Intent loginIntent = new Intent();
			loginIntent.setClassName("org.elasticdroid",
					"org.elasticdroid.LoginView");
			startActivity(loginIntent);
			return true;
		case R.id.dashboard_menuitem_about:
			Intent aboutIntent = new Intent(this, AboutView.class);
			startActivity(aboutIntent);
			return true;
		case R.id.dashboard_menuitem_refresh:
			//get region data if necessary
			if (regionData == null) {
				executeRetrieveRegionModel();
				//this will execute EC2DashboardModel automagically.
			} else {
				executeEC2DashboardModel();
			}
			return true;
		case R.id.dashboard_menuitem_default_region:
			Intent setDefaultRegionIntent = new Intent();
			setDefaultRegionIntent.setClassName("org.elasticdroid",
					"org.elasticdroid.DefaultRegionView");
			setDefaultRegionIntent.putExtra("regionData",
					new ArrayList<String>(regionData.keySet()));
			setDefaultRegionIntent.putExtra("username",
					connectionData.get("username"));
			startActivityForResult(setDefaultRegionIntent, 0); // the second arg
																// is ignored.
			return true;
		default:
			return super.onOptionsItemSelected(selectedItem);
		}
	}

	/**
	 * Called when the default region setter returns.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(this.getClass().getName() + "onActivityResult()", "result intent from : " + 
				data.resolveType(this));
		Log.v(this.getClass().getName() + "onActivityResult()", "test: " + 
				EC2DisplayInstancesView.class.toString());		
		Log.v(this.getClass().getName(), "Subactivity returned with result: "
				+ resultCode);
		
		//check which class returned the result intent
		if (data.resolveType(this).equals(EC2DisplayInstancesView.class.toString().split(" ")[1])) {
			switch (resultCode) {
				case RESULT_ERROR:
					Log.e(this.getClass().getName()+"onActivityResult", data.getStringExtra("EXCEPTION_MSG"));
					finish(); //kill the app off.
					break;
			}
		}
		else {
			switch (resultCode) {
			case RESULT_OK:
				selectedRegion = data.getStringExtra("defaultRegion");
				defaultRegionChanged = true;
				Log.v(this.getClass().getName(), "Region selected: "
						+ selectedRegion);
				Spinner regionSpinner = ((Spinner) findViewById(R.id.ec2DashboardRegionSpinner));
	
				String selectedItemText = regionSpinner.getSelectedItem()
						.toString();
				Log.v(this.getClass().getName(), "New default region: "
						+ selectedRegion + ", currently selected item text: "
						+ selectedItemText);
	
				// if new region selected, populate it.
				if (!selectedRegion.equals(selectedItemText)) {
					short selectedRegionPosition = 0;
					for (String regionName : regionData.keySet()) {
						if (regionName.equals(selectedRegion)) {
							break;
						}
						selectedRegionPosition++;
					}
					// just-in-case error check
					if (selectedRegionPosition == regionData.keySet().size()) {
						Log.e(this.getClass().getName(),
								"Could not find selected region in regions list. WTF!");
						finish();
					}
					// set the selection to be the new region...
					regionSpinner.setSelection(selectedRegionPosition);
	
					// repopulate ListView with data for this region
					executeEC2DashboardModel();
				}
				break;
			case RESULT_CANCELED:
				defaultRegionChanged = true; // yes, default region didn't change
				// but you're just saying,
				// "dont bother reloading the dashboard, mate"
				// to EC2Dashboard View. See onResume to see what this bool does.
			}
		}
	}
	
	/** 
	 * Handle cancel of progress dialog
	 * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.
	 * DialogInterface)
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		//this cannot be called UNLESS the user has the model running.
		//i.e. the prog bar is visible
		progressDialogDisplayed = false;
		
		if (retrieveRegionModel != null) {
			retrieveRegionModel.cancel(true);
		}
		else if (ec2DashboardModel != null) {
			ec2DashboardModel.cancel(true);
		}
	}
}
