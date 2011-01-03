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
 * Authored by siddhu on 3 Jan 2011
 */
package org.elasticdroid;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.elasticdroid.model.CloudWatchMetricsModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.utils.DialogConstants;
import static org.elasticdroid.utils.MonitoringDurations.*;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.model.Dimension;

/**
 * Graph to display options to be set for monitoring instances.
 * 
 * @author siddhu
 *
 * 3 Jan 2011
 */
public class MonitorInstanceOptionsView extends GenericActivity implements OnClickListener {

	/** The metrics model */
	private CloudWatchMetricsModel metricsModel;
	/** The connection data */
	private HashMap<String, String> connectionData;
	/** The list of measurenames; returned by the model */
	ArrayList<String> measureNames;
	/** Logging tag */
	private final static String TAG = "org.elasticdroid.MonitorInstanceOptionsView";
	/** Selected region */
	private String selectedRegion;
	/** The selected instance */
	private String instanceId;
	
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
	 * Called when activity is first displayed.
	 * Should receive list of regions as Intent.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = this.getIntent();
    	try {
    		this.connectionData = (HashMap<String, String>)intent.getSerializableExtra(
    				"org.elasticdroid.MonitorInstanceView.connectionData");
    	}
    	catch(Exception exception) {
        	//the possible exceptions are NullPointerException: the Hashmap was not found, or
        	//ClassCastException: the argument passed is not Hashmap<String, String>. In either case,
        	//just print out the error and exit. This is very inelegant, but this is a programmer's
    		//bug
    		Log.e(TAG, exception.getMessage());
    		finish(); //this will cause it to return to {@link EC2DisplayInstancesView}.
    	}
    	
    	this.instanceId = intent.getStringExtra("instanceId"); //instance ID can be null. see
    	//execute model for how this is handled. Note, we're not using this atm, and it's there 
    	//purely as future-proofing 
    	this.selectedRegion = intent.getStringExtra("selectedRegion");
    	
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
							MonitorInstanceOptionsView.this.finish();
						}
					}
				}
			);
		
    	//set the content view
    	this.setContentView(R.layout.monitorinstanceoptions);
    	this.setTitle(this.getString(R.string.monitorinstanceview_graphtype)); //set title
    	
    	//add a listener for the OK button
    	(this.findViewById(R.id.changeButton)).setOnClickListener(this);
    	
    	//set duration spinner selection to the appropriate value
    	long selectedDuration = intent.getLongExtra("selectedDuration", LAST_HOUR.getDuration());
    	
    	if (selectedDuration == LAST_HOUR.getDuration()) {
    		((Spinner)findViewById(R.id.durationSpinner)).setSelection(LAST_HOUR.getPos());	
    	}
    	else if (selectedDuration == LAST_SIX_HOURS.getDuration()) {
    		((Spinner)findViewById(R.id.durationSpinner)).setSelection(LAST_SIX_HOURS.getPos());	
    	}
    	else if (selectedDuration == LAST_TWELVE_HOURS.getDuration()) {
    		((Spinner)findViewById(R.id.durationSpinner)).setSelection(LAST_TWELVE_HOURS.getPos());	
    	}
    	else if (selectedDuration == LAST_DAY.getDuration()) {
    		((Spinner)findViewById(R.id.durationSpinner)).setSelection(LAST_DAY.getPos());	
    	}
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li>cloudWatchInput: The input data (such as period, measure name etc) for the display</li>
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
		
		/*get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof CloudWatchMetricsModel) {
			metricsModel = (CloudWatchMetricsModel) retained;
			metricsModel.setActivity(this);
		}
		
		measureNames = stateToRestore.getStringArrayList("measureNames");
	}
	
	/**
	 * Resume activity
	 * Execute model to get measures if we do not have measures already 
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		if ((metricsModel == null) && (measureNames == null)) {
			executeModel();
		}
		//if we have metrics, populate the measure spinner
		else if (measureNames != null) {
			populateMeasureSpinner();
		}
	}
	
	/**Strin
	 * Save state of the activity on destroy/stop.
	 * Apart from the usual, this saves:
	 * <ul>
	 * <li> metrics: The measures the user can choose from.</li>
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
		
		if (measureNames != null) {
			saveState.putStringArrayList("measureNames", measureNames);
		}
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.CloudWatchMetricsModel} Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This is done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (metricsModel != null) {
			metricsModel.setActivityNull(); //tell the mdoel the activity is restarting
			return metricsModel;
		}
		
		return null;
	}
	
  	/**
  	 * Execute model
  	 */
  	private void executeModel() {
  		//get the end point for the selected region and pass it to the model  		
  		metricsModel = new CloudWatchMetricsModel(this, connectionData, selectedRegion);
  		Dimension dimension = new Dimension();
  		dimension.setName("InstanceId");
  		dimension.setValue(instanceId);
  		
  		metricsModel.execute(dimension);
  	}

	/* (non-Javadoc)
	 * @see org.elasticdroid.tpl.GenericActivity#processModelResults(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		//result is null if user cancels. If user cancels, kill the activity.
		if (result == null) {
			Intent resultIntent = new Intent();
			resultIntent.setType(this.getClass().getName());
			
			setResult(RESULT_CANCELED, resultIntent); //let the calling activity know that the user
			//chose to cancel
			
			finish(); //kill activity
		}
		
		Log.v(TAG, "Processing model results...");
		metricsModel = null;
		if (result instanceof ArrayList<?>) {
			measureNames = (ArrayList<String>) result;
			
			populateMeasureSpinner();
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
			killActivityOnError = true;//all errors should cause death of activity
		} 
		else if (result instanceof AmazonClientException) {
			alertDialogMessage = this
					.getString(R.string.loginview_no_connxn_dlg);
			alertDialogDisplayed = true;
			killActivityOnError = true;//all errors should cause death of activity 
			//to retry.
		}
		
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
		
	}

	/**
	 * Populate the measure spinner
	 */
	private void populateMeasureSpinner() {
		
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_spinner_item, measureNames);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		//set the adapter.
		((Spinner) findViewById(R.id.measureSpinner)).setAdapter(spinnerAdapter);
		
		//get the selected measurename sent by intent, and set it.
		Intent intent = this.getIntent();
		String selectedMeasureName = intent.getStringExtra("selectedMeasureName");
		//if the selected measure name is found, set the spinner to show that as the selection.
		if (measureNames.indexOf(selectedMeasureName) >= 0) {
			((Spinner) findViewById(R.id.measureSpinner)).setSelection(measureNames.indexOf(
					selectedMeasureName));
		}
	}
	/**
	 * Handle click on the change button.
	 */
	@Override
	public void onClick(View v) {
		long startTime;
		long endTime;
		String measureName;
		Spinner measureSpinner = (Spinner)findViewById(R.id.measureSpinner);
		Spinner durationSpinner = (Spinner)findViewById(R.id.durationSpinner);
		Intent resultIntent = new Intent();
		resultIntent.setType(this.getClass().getName());
		
		//create a CloudWatchInput from the selections
		endTime = new Date().getTime();
		startTime = endTime; //random initialisation
		//the start time will depend on the selection in the duration spinner
		//get it!
		if (durationSpinner.getSelectedItem().toString().equals(LAST_HOUR.getString(this))) {
			startTime = endTime - LAST_HOUR.getDuration();
		}
		else if (durationSpinner.getSelectedItem().toString().equals(
				LAST_SIX_HOURS.getString(this))) {
			startTime = endTime - LAST_SIX_HOURS.getDuration();
		}
		else if (durationSpinner.getSelectedItem().toString().equals(
				LAST_TWELVE_HOURS.getString(this))) {
			startTime = endTime - LAST_TWELVE_HOURS.getDuration();
		}
		else if (durationSpinner.getSelectedItem().toString().equals(LAST_DAY.getString(this))) {
			startTime = endTime - LAST_DAY.getDuration();
		}
		
		measureName = measureSpinner.getSelectedItem().toString();
		
		resultIntent.putExtra("startTime", startTime);
		resultIntent.putExtra("endTime", endTime);
		resultIntent.putExtra("measureName", measureName);
		
		setResult(RESULT_OK, resultIntent);
		
		finish(); //kill activity
	}
	
	/**
	 * Handle back button.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent resultIntent = new Intent();
			resultIntent.setType(this.getClass().getName());
			
			setResult(RESULT_CANCELED, resultIntent); //let the calling activity know that the user
			//chose to cancel
		}
		
		return super.onKeyDown(keyCode, event);
	}

	/** 
	 * Handle cancellation of progress dialog
	 * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		progressDialogDisplayed = false;
		metricsModel.cancel(true);
	}
}
