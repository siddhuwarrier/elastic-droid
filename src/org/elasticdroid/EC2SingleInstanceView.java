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
 * Authored by siddhu on 11 Dec 2010
 */
package org.elasticdroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.ControlInstancesModel;
import org.elasticdroid.model.EC2InstancesModel;
import org.elasticdroid.model.ElasticIPsModel;
import org.elasticdroid.model.ControlInstancesModel.ControlType;
import org.elasticdroid.model.ds.SerializableAddress;
import org.elasticdroid.model.ds.SerializableInstance;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.DialogConstants;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceStateChange;

/**
 * Activity to display details on a single instance. 
 * Called by EC2DisplayInstancesView. Will handle all of its errors internally, and not return a 
 * result.
 * 
 * @author siddhu
 *
 * 11 Dec 2010
 */
public class EC2SingleInstanceView extends GenericListActivity {
	
	/**
	 * The instance that is being displayed.
	 */
	private SerializableInstance instance;
	/**
	 * The AWS connection data
	 */
	private HashMap<String, String> connectionData;
	/**
	 * The selected region
	 */
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

    /**
     * Elastic IP Model object
     */
    private ElasticIPsModel elasticIpsModel;
    /**
     * EC2 Instances Model object
     */
    private EC2InstancesModel ec2InstancesModel;
    /**
     * Control Instances model: to start/stop instances
     */
    private ControlInstancesModel controlInstancesModel;
    
    /**
     * Is an Elastic IP assigned to this instance.
     * Model answers this question.
     * Note: Only Elastic IP per instance (i.e. one public n/w i/f per machine).
     * 
     * Using Boolean instead of boolean because we also use this to find if the model has been 
     * executed.
     */
	private Boolean isElasticIpAssigned;
	/**
	 * Is EC2InstanceModel being used for autorefreshing after starting/stopping instances
	 */
	private boolean autoRefresh;
	/**
	 * This boolean indicates we have changed the state of this instance since when it was first
	 * displayed. This is to force the EC2DisplayInstancesView to update.
	 */
	private boolean instanceStateChanged;
	
	/**
	 * Tag for logging
	 */
	private static final String TAG = "org.elasticdroid.EC2SingleInstanceView";
	
	/**
	 * Called when activity is created.
	 * @param savedInstanceState if any
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); //call superclass onCreate
		
		Intent intent = this.getIntent();
		
		//get data from intent
		selectedRegion = intent.getStringExtra("selectedRegion");
		
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
    	
    	try {
    		this.instance = (SerializableInstance)intent.getSerializableExtra(
    				"org.elasticdroid.model.SerializableInstance");
    	}
    	catch(Exception exception) {
    		Log.e(TAG, exception.getMessage());
    		finish(); //this will cause it to return to {@link EC2DisplayInstancesView}.
    	}    
    	
		// create and initialise the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); // create alert
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
							EC2SingleInstanceView.this.finish();
						}
					}
				});
		
		setContentView(R.layout.ec2singleinstance); //tell the activity to set the xml file
		
		this.setTitle(connectionData.get("username")+ " (" + selectedRegion +")"); //set title
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li>isElasticIpAssigned: Has the instance been assigned an Elastic IP?</li>
	 * </ul>
	 */
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		//restore alertDialogDisplayed boolean
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(TAG, "alertDialogDisplayed = "
				+ alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		//was a progress dialog being displayed? Restore the answer to this question.
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		Log.v(TAG + ".onRestoreInstanceState", "progressDialogDisplayed:" + 
				progressDialogDisplayed);
		
		//restore the instance if the instance has been saved. This is for when we
		//may have changed the state of the instance due to start/stop operations by the
		//ControlInstancesModel.
		if (stateToRestore.getSerializable("instance") != null) {
			instance = (SerializableInstance)stateToRestore.getSerializable("instance");
		}
		
		//restore the boolean that indicates if state has changed.
		instanceStateChanged = stateToRestore.getBoolean("instanceStateChanged");
		
		//check if the key exists before assigning it.
		//This is because getBoolean returns false if key doesn't exist.
		//See onSaveInstanceState(). It shows that isElasticIpAssigned is not always saved.
		if (stateToRestore.get("isElasticIpAssigned") != null) {
			isElasticIpAssigned = stateToRestore.getBoolean("isElasticIpAssigned");
		}
		/*get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		//if there was a model executing when the object was destroyed, retained will be an 
		//instance of ElasticIpsModel
		if (retained instanceof ElasticIPsModel) {
			Log.i(TAG + ".onRestoreInstanceState()","Reclaiming previous " +
				"background task");
			elasticIpsModel = (ElasticIPsModel)retained;
			elasticIpsModel.setActivity(this);//tell the model of the new activity created
			
			ec2InstancesModel = null; //set EC2 instances model to null (redundant assignment)
			controlInstancesModel = null; //set ControlInstancesModel to null (redundant assn)
		}
		else if (retained instanceof EC2InstancesModel) {
			Log.i(TAG + ".onRestoreInstanceState()","Reclaiming previous " +
			"background task");
			
			ec2InstancesModel = (EC2InstancesModel) retained;
			ec2InstancesModel.setActivity(this);
		}
		else if (retained instanceof ControlInstancesModel) {
			Log.i(TAG, "Reclaiming ControlInstancesModel");
			
			controlInstancesModel = (ControlInstancesModel) retained;
			controlInstancesModel.setActivity(this);
		}
		else {
			Log.v(TAG,"No model object, or model finished before activity " +
					"was recreated.");
			
			//now if there is no model anymore, and progressDialogDisplayed is set to true,
			//reset it to false, because the model finished executing before the restart
			if (progressDialogDisplayed) {
				progressDialogDisplayed = false;
			}
		}
	}
	
	/**
	 * Executed when activity is resumed. Calls ElasticIpModel to determine if public IP
	 * is elastic.
	 */
	@Override
	public void onResume() {
		super.onResume(); //call superclass onResume()
		
		Log.v(TAG + ".onResume()", "onResume");
		
		//set the rest of the UI elements
		//if there is no tag called "Name" (case sensitive), set instance ID
		if (instance.getTag() == null) {
			((TextView)findViewById(R.id.ec2SingleInstanceName)).setText(Html.fromHtml(String.format(
					this.getString(R.string.ec2singleinstance_tag), instance.getInstanceId())));
		}
		else {
			((TextView)findViewById(R.id.ec2SingleInstanceName)).setText(Html.fromHtml(String.format(
					this.getString(R.string.ec2singleinstance_tag), instance.getTag())));			
		}
		
		//don't execute ElasticIpModel if instance is stopped or summat
		if (instance.getStateCode() != InstanceStateConstants.RUNNING) {
			isElasticIpAssigned = false;
		}
		
		//if there was a dialog box, display it
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		} 
		//execute Elastic IPs model only if EC2 instances model is not running
		else if ((elasticIpsModel == null) && (ec2InstancesModel == null) && (isElasticIpAssigned == 
			null)) {
				executeElasticIpModel();
		} 
		else {
			//populate the list
			if (isElasticIpAssigned != null) {
				setListAdapter(new EC2SingleInstanceAdapter(this, R.layout.ec2singleinstance, 
					instance, isElasticIpAssigned));
			}
		}
	}
	
	/**
	 * Save state of the activity on destroy/stop.
	 * Saves:
	 * <ul>
	 * <li> instanceData: The instance data collected.</li>
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
		
		//save if progress dialog is being displayed.
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
		//save if instance state has been changed
		saveState.putBoolean("instanceStateChanged", instanceStateChanged);
		//save whether there is an elastic IP assigned to this instance IF it has been initialised
		if (isElasticIpAssigned != null) {
			saveState.putBoolean("isElasticIpAssigned", isElasticIpAssigned);
		}
		
		if (instance != null) {
			saveState.putSerializable("instance", instance);
		}
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.ElasticIPsModel} Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (elasticIpsModel != null) {
			elasticIpsModel.setActivityNull();
			return elasticIpsModel;
		}
		else if (ec2InstancesModel != null) {
			//the user has asked the single instance view to be refreshed
			ec2InstancesModel.setActivityNull();
			return ec2InstancesModel;
		}
		else if (controlInstancesModel != null) {
			//the user has asked for the instance to be stopped or restarted.
			controlInstancesModel.setActivityNull();
			return controlInstancesModel;
		}
		
		return null;
	}
	
	/**
	 * Executes the model which will return the Elastic IP(s) assigned to this instance.
	 */
	private void executeElasticIpModel() {
		Log.v(TAG + ".executeModel()", "Going to execute model!");
		elasticIpsModel = new ElasticIPsModel(this, connectionData);
		
		//filter results that are not relevant to this instance
		//we have to pass an array of filters to the doInBackground method, and we need to construct
		//an array list from the String array constructed to hold the instance.getInstanceId() string
		//so much function chaining, i feel ill. Or maybe I'm just stupid!
		elasticIpsModel.execute(new Filter[]{
				new Filter("instance-id", 
						new ArrayList<String>(Arrays.asList(
								new String[]{instance.getInstanceId()})
								)
						  )
				});
	}
	
	/**
	 * Executes the model that will refresh the instance.
	 */
	private void executeEC2InstanceModel() {
		//create a new filter
		Filter singleInstanceFilter = new Filter("instance-id").withValues(new String[]{
				instance.getInstanceId()
		});
		
		Log.v(TAG, "Region:" + connectionData.get("region"));
		Log.v(TAG, "Instance ID:" + instance.getInstanceId());
		ec2InstancesModel = new EC2InstancesModel(this, connectionData, selectedRegion);
		ec2InstancesModel.execute(singleInstanceFilter);
	}
	
	/**
	 * Execute ControlInstancesModel
	 * @param Do we wish to tag this instance, or start/restart it.
	 */
	private void executeControlInstancesModel(boolean tag) {
		instanceStateChanged = true;
		Log.v(TAG, "Instance state changed: " + instanceStateChanged);
		
		if (!tag) {
			if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
				controlInstancesModel = new ControlInstancesModel(this, connectionData, 
						ControlType.STOP_INSTANCE);
				
				//execute model
				controlInstancesModel.execute(instance.getInstanceId());
			}
			else if (instance.getStateCode() == InstanceStateConstants.STOPPED) {
				controlInstancesModel = new ControlInstancesModel(this, connectionData, 
						ControlType.START_INSTANCE);
				
				//execute model
				controlInstancesModel.execute(instance.getInstanceId());
			}
		}
		else {
			Log.v(TAG, "Tagging instance...");
			controlInstancesModel = new ControlInstancesModel(this, connectionData, 
					ControlType.TAG_INSTANCE, Arrays.asList(new String[]{instance.getTag()}));
			
			controlInstancesModel.execute(instance.getInstanceId());
		}
	}
	
	/**
	 * Keep refreshing the EC2 Instance model until the state is as expected 
	 */
	private void autoRefreshEC2InstanceModel(int expectedState) {
		//create a new filter
		Filter singleInstanceFilter = new Filter("instance-id").withValues(new String[]{
				instance.getInstanceId()
		});
		Log.v(TAG, "Expected instance state: " + expectedState);
		
		autoRefresh = true;//set autorefresh to true so that progress dialog is not displayed.
		
		ec2InstancesModel = new EC2InstancesModel(this, connectionData, selectedRegion, 
				expectedState);
		ec2InstancesModel.execute(singleInstanceFilter); //this will now auto-refresh till done.
	}
	
	/**
	 * Process results from model
	 * @param Object, which can be a List<Address> or exceptions
	 * 
	 * This method can be invoked by:
	 * <ul>
	 *  <li>{@link ControlInstancesModel}: Start/stop instance.</li>
	 *  <li>{@link ElasticIPsModel}: Check if the instance is using an Elastic IP.</li>
	 *  <li>{@link EC2InstancesModel}: Refresh (or auto-refresh) the instance</li>
	 * </ul>
	 */
	@Override
	public void processModelResults(Object result) {
		
		Log.v(TAG + ".processModelResults()", "Processing model results...");
		
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		//irrespective of the model, if cancelled, display the cancelled toast.
		if ((result == null) && (!autoRefresh)) {
			Toast.makeText(this, Html.fromHtml(this.getString(R.string.cancelled)), Toast.
					LENGTH_LONG).show();
			
			return; //don't execute the rest of this method.
		}
		
		if (elasticIpsModel != null) {
			processElasticIpsModelResult(result);
		}
		else if (ec2InstancesModel != null) {
			processEC2InstanceModelResult(result);
		}
		//the control instances model returns after stoppign or starting an instance
		else if (controlInstancesModel != null) {
			processControlInstancesModelResult(result);
		}
		
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
	}
	
	/**
	 * 
	 * @param result. Result can be one of:
	 * <ul>
	 * 	<li>ArrayList<SerializableAddress>: An ArrayList (known in this case to be of size 1)
	 *  containing the data on the IP. This model is execd when a running instance's view
	 *  is created.</li>
	 * 	<li>AmazonClientException: Issues with the client.</li>
	 * 	<li>AmazonServiceException: Issues with the service itself.</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	private void processElasticIpsModelResult(Object result) {
		elasticIpsModel = null;
		// if the model returned a result; i.e. success.
		if (result instanceof ArrayList<?>) {
			
			//if there is data, set boolean to true
			if (((ArrayList<SerializableAddress>) result).size() != 0) {
				isElasticIpAssigned = true;
			}
			else {
				isElasticIpAssigned = false; //this has to be done manually because we are 
				//using a Boolean and not a boolean. When null, we execute the model.
			}
			
			//populate the list
			setListAdapter(new EC2SingleInstanceAdapter(this, R.layout.ec2singleinstance, 
					instance, isElasticIpAssigned));
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
	}
	/**
	 * 
	 * @param result. Result can be one of:
	 * <ul>
	 * 	<li>ArrayList<SerializableInstance>: An ArrayList (known in this case to be of size 1)
	 *  containing the data on the instance. This model is execd when the instance is refreshed,
	 *  or TODO when the instance state is changed and we want the display to auto-refresh.</li>
	 * 	<li>AmazonClientException: Issues with the client.</li>
	 * 	<li>AmazonServiceException: Issues with the service itself.</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	private void processEC2InstanceModelResult(Object result) {
		ec2InstancesModel = null; //set model to null
		autoRefresh = false; //set autorefresh to false even if it already was faslse
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		if (result instanceof ArrayList<?>) {
			//there's going to be only 1 entry. Tested in unit tests
			Log.v(TAG, "Result size: " + ((ArrayList<SerializableInstance>) result).size());
			instance = ((ArrayList<SerializableInstance>) result).get(0);
			
			//change the title, just in case
			((TextView)findViewById(R.id.ec2SingleInstanceName)).setText(Html.fromHtml(String.format(
					this.getString(R.string.ec2singleinstance_tag), instance.getTag())));
			
			if (isElasticIpAssigned != null) {
				//populate the list
				setListAdapter(new EC2SingleInstanceAdapter(this, R.layout.ec2singleinstance, 
						instance, isElasticIpAssigned));
			}
			else {
				//elastic IP not assigned; rerun model.
				executeElasticIpModel();
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
			killActivityOnError = false;//do not kill activity on connectivity error. allow client 
			//to retry.
		}
	}
	
	/**
	 * Process the results returned by the ControlInstancesModel
	 * 
	 * @param result. Result can be:
	 * <ul>
	 * 	<li>List<InstanceStateChange>: An ArrayList containing the state changes to 
	 * 	each of the instances stopped/started.</li>
	 * 	<li>AmazonClientException: Issues with the client.</li>
	 * 	<li>AmazonServiceException: Issues with the service itself.</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	private void processControlInstancesModelResult(Object result) {
		//set the model to null
		controlInstancesModel = null;
		
		//if successful it would have returned a ArrayList<InstanceStateChange>.
		if (result instanceof List<?>) {
			int expectedState = -1;
			//there's only going to be one entry in the list, but nevertheless...
			for (InstanceStateChange change : (List<InstanceStateChange>) result) {
				
				Log.v(TAG, "Instance ID: " + change.getInstanceId() + ", State: " + change.
						getCurrentState());
				if (change.getInstanceId().equals(instance.getInstanceId())) {
					//set the state code and state name
					instance.setStateCode(change.getCurrentState().getCode());
					instance.setStateName(change.getCurrentState().getName());
					//get the state we should eventually get to
					if (change.getPreviousState().getCode() == InstanceStateConstants.STOPPED) {
						expectedState = InstanceStateConstants.RUNNING;
					}
					else {
						expectedState = InstanceStateConstants.STOPPED;
					}
				}
			}
			
			//populate the list
			setListAdapter(new EC2SingleInstanceAdapter(this, R.layout.ec2singleinstance, 
					instance, isElasticIpAssigned));
			
			//start refresh until state is not equal to expected state.
			//i.e. running if stopped, stopped if running.
			if (expectedState >= 0) {
				autoRefreshEC2InstanceModel(expectedState);
			}
		}
		else if (result instanceof Boolean) {
			Log.v(TAG, "Successfully tagged instance.");
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

	/**
	 * Overridden method to display the menu on press of the menu key
	 * 
	 * Inflates and shows menu for displayed instances view.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.singleinstance_menu, menu);
	
		return true;
	}
	
	/**
	 * Overriden. Prepares menu
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.v(TAG, "Preparing Options menu....");
		
		if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
			menu.findItem(R.id.singleinstance_menuitem_monitor).setEnabled(true);
			menu.findItem(R.id.singleinstance_menuitem_ssh).setEnabled(true);
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setEnabled(true);
			
			//make sure that the start/stop instance state is saying Stop
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setTitle
			(R.string.ec2singleinstance_menu_stopinstance);
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setIcon(R.drawable.
					ic_menu_stop);
		}
		else if (instance.getStateCode() == InstanceStateConstants.STOPPED) {
			//enable the control instance button
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setEnabled(true);
			
			//if the instance is stopped, then change the control instance menu item's text and img
			//to show "start instance" and a "play" button respectively.
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setTitle
				(R.string.ec2singleinstance_menu_startinstance);
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setIcon(R.drawable.
					ic_menu_play_clip);
		}
		else //in between running and stopped
		
		if (instance.getStateCode() != InstanceStateConstants.RUNNING) {
			//if the instance is not running, disable monitoring and SSH
			Log.v(TAG + ".onPrepareOptionsMenu()", "Removing monitoring and" +
					"SSH connect options.");
			menu.findItem(R.id.singleinstance_menuitem_monitor).setEnabled(false);
			menu.findItem(R.id.singleinstance_menuitem_ssh).setEnabled(false);
		}
		else {

		}
		
		return true;
	}
	
	/**
	 * Overriden method to handle selection of menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem selectedItem) {
		switch (selectedItem.getItemId()) {
		
		case R.id.singleinstance_menuitem_about:
			Intent aboutIntent = new Intent(this, AboutView.class);
			startActivity(aboutIntent);
			return true;
			
		case R.id.singleinstance_menuitem_ssh:
			Log.v(TAG + ".onOptionsItemSelected()", "User wishes to SSH!");
			
			//call the SSH connector view using the intent
			Intent sshConnectorIntent = new Intent();
			sshConnectorIntent.setClassName("org.elasticdroid","org.elasticdroid.SshConnectorView");
			
			//if the user deallocates Elastic IP address in the middle of all of this you won't have
			//a public DNS name. Just issue an error and fuck off.
			if (instance.getPublicDnsName() == null) {
				alertDialogMessage = this.getString(R.string.ec2singleinstance_nopublicdns);
				alertDialogDisplayed = true;
				killActivityOnError = true;
				instanceStateChanged = true; //force refresh of Display Instances list.
				
				alertDialogBox.setMessage(alertDialogMessage);
				alertDialogBox.show();
			}
			else {
				//not using IP address as theoretically DHCP lease can expire when connecting
				//if Elastic IP is not used.
				sshConnectorIntent.putExtra("hostname", instance.getPublicDnsName());
				List<String> secGroupNames = instance.getSecurityGroupNames();
				//breaking 100 character per line unwritten rule here as the code looks better this way
				sshConnectorIntent.putExtra("securityGroups", 
						secGroupNames.toArray(new String[secGroupNames.size()]));
				sshConnectorIntent.putExtra("selectedRegion", selectedRegion);
				sshConnectorIntent.putExtra(
						"org.elasticdroid.EC2DashboardView.connectionData",
						connectionData); // aws connection info
				//start the activity
				startActivity(sshConnectorIntent);
			}
			return true;
			
		case R.id.singleinstance_menuitem_refresh:
			//create the model and launch it with the filter
			//execute Elastic IP model as well as EC2 instance model
			//this is because Elastic IPs may have been assigned/deassigned. 
			executeElasticIpModel();
			executeEC2InstanceModel();
			return true;
			
		case R.id.singleinstance_menuitem_monitor:
			Intent monitorIntent = new Intent();
			monitorIntent.setClassName("org.elasticdroid", 
					"org.elasticdroid.MonitorInstanceView");
			//send it the AWS connection data.
			monitorIntent.putExtra("org.elasticdroid.EC2SingleInstanceView.connectionData", 
					connectionData);
			monitorIntent.putExtra("instanceId", instance.getInstanceId());
			monitorIntent.putExtra("selectedRegion", selectedRegion);
			
			startActivity(monitorIntent);
			return true;
			
		case R.id.singleinstance_menuitem_controlinstance:
			//do not execute if the user cancelled when getting elastic IP data (applicable only
			//to running instances)
			if ( (isElasticIpAssigned == null) && (instance.getStateCode() == 
				InstanceStateConstants.RUNNING)) {
				return false;
			}
			
			executeControlInstancesModel(false); //execute control instances model 
			//to start/stop instance			
			return true;
		
		case R.id.singleinstance_menuitem_tag:
			Intent startIntent = new Intent();
			startIntent.setClassName("org.elasticdroid", "org.elasticdroid.TagView");
			
			Log.v(TAG, "Instance ID: " + instance.getInstanceId());
			startIntent.putExtra("instanceId", instance.getInstanceId());
			if (instance.getTag() != null) {
				startIntent.putExtra("tag", instance.getTag());
			}
			
			startActivityForResult(startIntent, 0); //second arg ignored
			
			return true;
			
		default:
			return super.onOptionsItemSelected(selectedItem);
		}
	}

	/**
	 * Handle back button.
	 * If back button is pressed, UI should die.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent resultIntent = new Intent();
			resultIntent.setType(this.getClass().getName());
			Log.v(TAG, "Force refresh: " + instanceStateChanged);
			resultIntent.putExtra("forceRefresh", instanceStateChanged);
			
			//if EC2 instances model is stil running autorefresh, cancel it. 
			if (ec2InstancesModel != null) {
				ec2InstancesModel.setActivityNull(); //tell the model the activity is goign bye bye
				ec2InstancesModel.cancel(true);
			}
			
			setResult(RESULT_OK, resultIntent); //return result to calling party
			finish();  
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	/** 
	 * Handle cancel of progress dialog
	 * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.
	 * DialogInterface)
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		//do not allow the ControlInstancesModel to be cancelled.
		if (controlInstancesModel != null) {
			//do not allow cancel. Redisplay.
			showDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		else {
			//this cannot be called UNLESS the user has the model running.
			//i.e. the prog bar is visible
			progressDialogDisplayed = false;
			if (elasticIpsModel != null) {
				elasticIpsModel.cancel(true);
			}
			else if (ec2InstancesModel != null) {
				ec2InstancesModel.cancel(true);
			}
		}
	}
	
	/**
	 * Called when the tag changer returns.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
		if (resultCode == RESULT_OK) {
			instance.setTag(resultIntent.getStringExtra("tag"));
			executeControlInstancesModel(true);
		}
	}
}

/**
 * ListView Adapter that extends ArrayAdapter<RowData>
 * 
 * Displays the metrics of each instance.
 * 
 * @author siddhu
 *
 * 12 Dec 2010
 */
class EC2SingleInstanceAdapter extends ArrayAdapter<RowData> {
	/** Instance datum */
	private SerializableInstance instance;
	/** Does the above instance have an Elastic IP assigned? */
	private boolean isElasticIpAssigned;
	/** Context */
	private Context context;
	
	/**
	 * Constructor: Calls ArrayAdapter constructor with a RowData enum that contains all of the 
	 * fields we want to display in the ListView.
	 * 
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 */
	public EC2SingleInstanceAdapter(Context context, int textViewResourceId,
			SerializableInstance instance, boolean isElasticIpAssigned) {
		super(context, textViewResourceId, RowData.values());
		
		//save context and instance data
		this.context = context;
		this.instance = instance;
		this.isElasticIpAssigned = isElasticIpAssigned;
	}
	
	/**
	 * Overriden method called when ListView is initialised with data.
	 * @param position The position in {@link #instanceData}.
	 * @param convertView The view to set.
	 * @param parent
	 * @return Configured row to add to ListView
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View instanceMetricRow = convertView;
		
		if (instanceMetricRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
			(Context.LAYOUT_INFLATER_SERVICE);
		
			instanceMetricRow = inflater.inflate(R.layout.ec2singleinstancerow, parent, 
					false);
		}
		
		Log.v(context.getClass().getName(), "Instance state code:" + instance.getStateCode());
		
		TextView instanceMetricTextView = (TextView)instanceMetricRow.findViewById(R.id.
				instanceMetric);
		TextView instanceDataTextView = (TextView)instanceMetricRow.findViewById(R.id.
				instanceData);
		ImageView instanceStatusIcon = ((ImageView) instanceMetricRow.findViewById(R.id.
				instanceStatusIcon));
		RowData selectedRowDatum = RowData.values()[position]; 
		
		switch(selectedRowDatum) {
		
		case STATE_NAME:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_state));
			instanceDataTextView.setText(Html.fromHtml(instance.getStateName()));
			
			if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
				((ImageView) instanceMetricRow.findViewById(R.id.instanceStatusIcon)).
					setImageResource(R.drawable.green_light);
			}
			else if ((instance.getStateCode() == InstanceStateConstants.STOPPED) || (instance.
					getStateCode() == InstanceStateConstants.TERMINATED)) {
				instanceStatusIcon.setImageResource(R.drawable.red_light);				
			}
			//instance is between starting and stopping/terminating.
			else if ( (instance.getStateCode() == InstanceStateConstants.SHUTTING_DOWN) 
					||(instance.getStateCode() == InstanceStateConstants.PENDING) 
					||(instance.getStateCode() == InstanceStateConstants.STOPPING)) 
					{
				instanceStatusIcon.setImageResource(R.drawable.yellow_light);
			}
			break;
			
		case TYPE:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_type));
			instanceDataTextView.setText(Html.fromHtml(instance.getInstanceType()));
			instanceStatusIcon.setImageResource(R.drawable.instance);
			break;
		
		case OS:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_os));
			if (instance.getPlatform() != null) {
				instanceDataTextView.setText(Html.fromHtml(instance.getPlatform()));
				instanceStatusIcon.setImageResource(R.drawable.droid_guy);
			}
			else {
				instanceDataTextView.setText(Html.fromHtml("Linux"));
				instanceStatusIcon.setImageResource(R.drawable.linux_penguin);
			}
			break;
			
		case LAUNCHED:
			instanceMetricTextView.setText(context.getString(R.string.
					ec2singleinstance_launchtime));
			instanceStatusIcon.setImageResource(R.drawable.ic_dialog_time);
			
			if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
				//get period running in hours.
				float timeRunning = ((new Date().getTime() - instance.getLaunchTime()) / 
						(1000 * 60 * 60)); //convert from milliseconds to hours
				
				String launchDetails;
				//if been running greater than 24 hours, convert to days
				if (timeRunning > 24) {
					timeRunning /= 24;
					
					launchDetails = String.format(
							context.getString(R.string.ec2singleinstance_launchdetails_days),
							timeRunning
							) ;
				}
				else {
					launchDetails = String.format(
							context.getString(R.string.ec2singleinstance_launchdetails_hrs),
							timeRunning
							);
				}
				
				instanceDataTextView.setText(Html.fromHtml(launchDetails));
			}
			else {
				instanceDataTextView.setText("N/A");
			}
			break;
			
		case KEYNAME:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_keypair));
			instanceDataTextView.setText(Html.fromHtml(instance.getKeyName()));
			instanceStatusIcon.setImageResource(R.drawable.keypair);
			break;
			
		case SECURITY_GROUP:
			//concatenate all security groups in list into one long String separated by spaces
			String securityGroupString = "";
			List<String> securityGroupNames = instance.getSecurityGroupNames();
			//O(n) solution
			for (String securityGroupName : securityGroupNames) {
				securityGroupString += securityGroupName + ", ";
			}
			//remove last comma
			securityGroupString = securityGroupString.substring(0, securityGroupString.length() - 2);
			
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_secgroup));
			instanceDataTextView.setText(Html.fromHtml(securityGroupString));
			instanceStatusIcon.setImageResource(R.drawable.ic_lock_lock);
			break;
		
		case AMI_ID:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_ami));
			instanceDataTextView.setText(Html.fromHtml(instance.getImageId()));
			instanceStatusIcon.setImageResource(R.drawable.ami);
			break;
			
		case IP_ADDRESS:
			//set IP address only if instance is running
			if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
				if (isElasticIpAssigned) {
					instanceMetricTextView.setText(context.getString(R.string.
							ec2singleinstance_elastic_ip));
					
					//to handle bug: http://code.google.com/p/elastic-droid/issues/detail?id=13
					//AWS doesn't seem to assign public DNS names to instances that
					//have their Elastic IPs removed midway.
					if (instance.getPublicIpAddress() != null) {
						instanceDataTextView.setText(Html.fromHtml(instance.getPublicIpAddress()));
					}
					else {
						instanceDataTextView.setText(context.getString(R.string.
								ec2singleinstance_nopublicdns));
					}
				}
				else {
					instanceMetricTextView.setText(context.getString(R.string.
							ec2singleinstance_public_ip));
					
					if (instance.getPublicIpAddress() != null) {
						instanceDataTextView.setText(Html.fromHtml(instance.getPublicIpAddress()));
					}
					else {
						instanceDataTextView.setText(context.getString(R.string.
								ec2singleinstance_nopublicdns));
					}
				}
			}
			else {
				instanceMetricTextView.setText(context.getString(R.string.
						ec2singleinstance_public_ip));
				instanceDataTextView.setText("N/A");
			}
			
			instanceStatusIcon.setImageResource(R.drawable.ic_menu_mylocation);
			
			break;
		}
		
		return instanceMetricRow;
	}
	
	/**
	 * Function to disable all items in the ListView, as we do not want users clicking on
	 * them.
	 */
	@Override
    public boolean areAllItemsEnabled() 
    { 
            return false; 
    } 
    
    /**
     * Another function that does the same as hte function above
     */
	@Override
    public boolean isEnabled(int position) 
    { 
            return false; 
    } 
}

/**
 * An enumeration that gets the text to be used for each data
 * type displayed by the EC2SingleInstanceAdapter.
 * @author siddhu
 *
 * 12 Dec 2010
 */
enum RowData {
	STATE_NAME,
	TYPE,
	OS,
	LAUNCHED,
	KEYNAME,
	SECURITY_GROUP,
	AMI_ID,
	IP_ADDRESS;
}

