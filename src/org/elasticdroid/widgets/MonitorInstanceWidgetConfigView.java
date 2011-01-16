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
 * Authored by siddhu on 9 Jan 2011
 */
package org.elasticdroid.widgets;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.elasticdroid.R;
import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.model.CloudWatchMetricsModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.utils.CloudWatchInput;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.model.Dimension;

/**
 * Configure the MonitorInstance widget.
 * This should present the user with a list of users he can pick,
 * and then allow him to pick an instance from a list of instances,
 * and then allow him to pick a measure duration etc
 * @author siddhu
 *
 * 9 Jan 2011
 */
public class MonitorInstanceWidgetConfigView extends GenericActivity implements 
	OnItemSelectedListener {
	
	/** hashtable to store userdata (username, accesskey, secret accesskey) keyed by username*/
	private Hashtable<String, ArrayList<String>> userData; 
	
	/** DS to hold list of watched instances */
	private HashMap<String, String> watchedInstances;
	
	/** ArrayList to hold the measures for the instance, so that we do not re-query */
	private ArrayList<String> measureNames;
	
	/** The selected position in the username list */
	private int selectedUsernamePos;
	
	/** The selected position in the watched instances list */
	private int selectedWatchedInstancePos;
	
	/** The selected position in the measures list */
	private int selectedMeasurePos;
	
	/** The selected position in the duration list */
	private int selectedDurationPos;
	
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.MonitorInstanceWidgetConfig";
	
	/** The ID of the App widget that called this config */
	private int appWidgetId;
	
	/** The location of Monitor Instance Shared Preferences */
	public static final String MONITORINSTANCEWIDGET_SHARED_PREF = "monitorInstanceWidget.prefs";
	
	/** Database object */
	private ElasticDroidDB elasticDroidDb;
	
	/** Dialog box for credential verification errors */
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
	
	/** Cloudwatch Metrics Model */
	private CloudWatchMetricsModel metricsModel;
	
	
	/**
	 * Oncreate method; caleld when activity created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(TAG, "Starting config activity...");
		this.setContentView(R.layout.monitorinstancewidgetconfig);

		//get the calling app widget ID from the intent
		 Intent intent = getIntent();
		 Bundle extras = intent.getExtras();
		 if (extras != null) {
			 //set the app widget id to the one passed, or to INVALID_APP_WIDGET_ID if none passed
		     appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.
		    		 INVALID_APPWIDGET_ID);
		 }

		 // If they gave us an intent without the widget id, just bail.
		 if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
		     finish();
		 }
		 
		 elasticDroidDb = new ElasticDroidDB(this); //create DB
		 
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
							//send cancel result
							returnCancelResult();
						}
					}
				});
		
		//add a listener for the Save button
	}
	
	/**
	 * Restore the instance state on change in Android orientation.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		//restore the user data if any
		userData = (Hashtable<String, ArrayList<String>>)stateToRestore.getSerializable("userData");
		// restore dialog data
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = "
				+ alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		//restore watched instances if any
		watchedInstances = (HashMap<String, String>) stateToRestore.getSerializable(
				"watchedInstances");
		
		//restore the positions in the list if any
		selectedUsernamePos = stateToRestore.getInt("selectedUsernamePos");
		selectedWatchedInstancePos = stateToRestore.getInt("selectedWatchedInstancePos");
		selectedMeasurePos = stateToRestore.getInt("selectedMeasurePos");
		selectedDurationPos = stateToRestore.getInt("selectedDurationPos");
		
		//restore the measures
		measureNames = stateToRestore.getStringArrayList("measureNames");
	}
	
	/**
	 * Resume operation of Activity. This method performs the following tasks:
	 * <ul>
	 * </ul>
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		//get the list of users from the Database if no user data
		if (userData == null) {
			userData = elasticDroidDb.listUserData();
		}
		
		if (userData.keySet().size() == 0) {
			alertDialogMessage = this.getString(R.string.monitorinstancewidget_nousers);
			alertDialogDisplayed = true;
			killActivityOnError = true;
		}
		else {
			//restore the spinner(s)
			populateUsernameSpinner();
		}
		// restore alert dialog box if any.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
	}
	
	/**
	 * Save instance state on activity destroy.
	 * 
	 * Saves:
	 * <ul>
	 * <li> user data. </li>
	 * <li></li>
	 * </ul>
	 */
	@Override
	public void onSaveInstanceState(Bundle saveState) {
		//save the username data from DB to avoid re-querying
		saveState.putSerializable("userData", userData);
		//save the watchedInstances data
		saveState.putSerializable("watchedInstances", watchedInstances);
		//save teh measure names
		saveState.putStringArrayList("measureNames", measureNames);
		
		//save whether the alert dialog box is displayed or not
		saveState.putBoolean("alertDialogDisplayed", alertDialogDisplayed);
		saveState.putString("alertDialogMessage", alertDialogMessage);

		//save all the list positions
		saveState.putInt("selectedMeasurePos", selectedMeasurePos);
		saveState.putInt("selectedDurationPos", selectedDurationPos);
		saveState.putInt("selectedUsernamePos", selectedUsernamePos);
		saveState.putInt("selectedWatchedInstancePos", selectedWatchedInstancePos);
	}
	
	/**
	 * Get the list of metrics for the selected instance
	 */
	private void executeMetricsModel() {
		HashMap<String, String> connectionData = new HashMap<String, String>();
		
		String instanceId = ((Spinner) findViewById(R.id.
				monitorInstanceWidgetConfigWatchedInstanceSpinner)).getSelectedItem().toString();
		String username = ((Spinner) findViewById(R.id.
				monitorInstanceWidgetConfigUsernameSpinner)).getSelectedItem().toString();
		
		Log.v(TAG, "Get metrics for instance: " + instanceId);
		
		//prepare connxn data
		connectionData.put("username", username);
		connectionData.put("accessKey", userData.get(username).get(0));
		connectionData.put("secretAccessKey", userData.get(username).get(1));
		
		metricsModel = new CloudWatchMetricsModel(this, connectionData, watchedInstances.get(
				instanceId));
		
		//set the dimensions
  		Dimension dimension = new Dimension();
  		dimension.setName("InstanceId");
  		dimension.setValue(instanceId);
  		
		metricsModel.execute(dimension);
	}
	
	/**
	 * Handle the return of the model
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		//if result returned is null,d isplay a toast and do not try to re-execute the model
		//unless the user forces re-execution.
		if (result == null) {
			Toast.makeText(this, Html.fromHtml(this.getString(R.string.cancelled)), Toast.
					LENGTH_LONG).show();
			
			return; //don't execute the rest of this method.
		}
		
		//Metrics model returned. 
		if (metricsModel != null) {
			metricsModel = null;
			
			//since we now have a measures selection, we can enable the save button
			((Button) findViewById(R.id.monitorInstanceWidgetSaveConfig)).setEnabled(true);
			
			Log.v(TAG, "Metrics model returned...");
			if (result instanceof ArrayList<?>) {
				measureNames = (ArrayList<String>) result;
				for (String measureName : measureNames) {
					Log.v(TAG, "Measure: "+ measureName);
				}
				
				//populate measure spinner
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
		}
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
	}

	/**
	 * Handle back button.
	 * 
	 * If the user presses the back button, widget configuration is cancelled, and the user is
	 * returned to his/her home screen.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			returnCancelResult();
		}
		
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * This activity is a very Spinnerfull activity. Handle spinner selection.
	 */
	@Override
	public void onItemSelected(AdapterView<?> viewAdapter, View selectedSpinner, int position,
			long id) {
		Log.v(TAG, "ID: " + viewAdapter.getId() + ", usernamspinner:" + R.id.
				monitorInstanceWidgetConfigUsernameSpinner);
		
		switch(viewAdapter.getId()) {
		/* Username spinner selected. */
		case R.id.monitorInstanceWidgetConfigUsernameSpinner:
			//when the username spinner is selected, we want to get the list of watched instances
			//and reload the watched instances spinner.
			//we also want to record the current position
			//REMEMBER: We will never come here if there aren't any usernames at all.
			
			//if the position has changed, reload
			//also, if we have no watched instance data, reload
			if ((selectedUsernamePos != position) || (watchedInstances == null)) {
				Log.v(TAG, "Setting measures to null");
				measureNames = null;
				
				Log.v(TAG, "Querying DB for watched instances...");
				selectedUsernamePos = position;
				getWatchedInstances(); //get the list of watched instances from DB
				//set the position of watched instances to 0 as the list has changed
				selectedWatchedInstancePos = 0;
			}
			
			Log.v(TAG, "Repopulating watched instances spinner...");
			//disable save button
			((Button)findViewById(R.id.monitorInstanceWidgetSaveConfig)).setEnabled(false);
			populateWatchedInstancesSpinner();
			break;
			
		case R.id.monitorInstanceWidgetConfigWatchedInstanceSpinner:
			//disable save button
			((Button)findViewById(R.id.monitorInstanceWidgetSaveConfig)).setEnabled(false);
			
			//if the position changes.
			if ((selectedWatchedInstancePos != position) || (measureNames == null)) {
				Log.v(TAG, "Querying AWS for lists of measures.");
				selectedWatchedInstancePos = position;
				selectedMeasurePos = 0;
				//start executing the metrics model in the background
				//if it is not already executing
				if (metricsModel == null) {
					executeMetricsModel();
				}
			}
			//it's being (re)constructed
			else {
				populateMeasureSpinner();
			}
			
			break;
	
		case R.id.monitorInstanceWidgetConfigMeasureSpinner:
			//enable save button
			((Button)findViewById(R.id.monitorInstanceWidgetSaveConfig)).setEnabled(true);
			selectedMeasurePos = position;
			break;
		}
		
		// display the alert dialog if the user set the displayed var to true
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();// show error
		}
	}
	
	/**
	 * Handle when nothing is selected on a spinner.
	 */
	@Override
	public void onNothingSelected(AdapterView<?> ignore) {
		// Doing nothing here
	}
	
	/**
	 * Queries the SQLite database for the list of watched instances and saves it in the 
	 * watchedInstances member variable
	 */
	private void getWatchedInstances() {
		Spinner usernameSpinner = (Spinner)findViewById(R.id.
				monitorInstanceWidgetConfigUsernameSpinner);
		
		//set the previous watched instances position to 0
		selectedWatchedInstancePos = 0;
		
		//get the watched instances for this username.
		try {
			watchedInstances = elasticDroidDb.getWatchedResources(
					usernameSpinner.getSelectedItem().toString(), 
					"instance");  
		}
		catch(SQLException exception) {
			//SQL Exception caused. Set watchedInstances to null
			Log.v(TAG, "SQL Exception when retreiving watched instances. " + exception.
					getMessage());			
			watchedInstances = null;
			
			//set an alert dialog up.
			alertDialogDisplayed = true;
			killActivityOnError = false;
			alertDialogMessage = this.getString(R.string.
					monitorinstancewidget_nowatchedinstances);
		}
	}
	/**
	 * Populate the user spinner
	 */
	private void populateUsernameSpinner() {
		ArrayAdapter<String> userSpinnerAdapter = new ArrayAdapter<String>(
				this,
				R.layout.customspinneritem,
				new ArrayList<String>(userData.keySet())
		);
		
		userSpinnerAdapter.setDropDownViewResource(R.layout.customspinnerdropdownitem);
		//set the array adapter as the user spinner's adapter.
		Spinner userSpinner = (Spinner) findViewById(R.id.
				monitorInstanceWidgetConfigUsernameSpinner);
		userSpinner.setAdapter(userSpinnerAdapter);
		userSpinner.setOnItemSelectedListener(this);
		userSpinner.setSelection(selectedUsernamePos);
	}
	/**
	 * Populate the watched instances spinner.
	 */
	private void populateWatchedInstancesSpinner() {
		Spinner instancesSpinner = (Spinner)findViewById(R.id.
				monitorInstanceWidgetConfigWatchedInstanceSpinner);
		
		//remove all the existing views inside the spinner
		Log.v(TAG, "Number of entries in spinner: " + instancesSpinner.getCount());
		instancesSpinner.removeAllViewsInLayout();
		
		//add in new data from watched instances if there is any
		if (watchedInstances.keySet().size() != 0) {
			ArrayAdapter<String> instancesAdapter = new ArrayAdapter<String> (
				this,
				R.layout.customspinneritem,
				new ArrayList<String>(watchedInstances.keySet())
			);
			
			Log.v(TAG, "Selected watched instance pos: " + selectedWatchedInstancePos);
			
			instancesAdapter.setDropDownViewResource(R.layout.customspinnerdropdownitem);
			instancesSpinner.setAdapter(instancesAdapter);
			instancesSpinner.setOnItemSelectedListener(this);
			instancesSpinner.setSelection(selectedWatchedInstancePos);
		}
		else {
			Log.v(TAG, "No data in watched instances.");
		}
	}

	private void populateMeasureSpinner() {
		Spinner measureSpinner = (Spinner)findViewById(R.id.
				monitorInstanceWidgetConfigMeasureSpinner);
		
		//remove all the existing views inside the spinner
		Log.v(TAG, "Number of entries in spinner: " + measureSpinner.getCount());
		measureSpinner.removeAllViewsInLayout();
		
		if ( (measureNames == null) || (measureNames.size() == 0) ) {
			Log.v(TAG, "No measures data!");
		}
		else {
			ArrayAdapter<String> measuresAdapter = new ArrayAdapter<String>(
					this, 
					R.layout.customspinneritem,
					measureNames
			);
			
			measuresAdapter.setDropDownViewResource(R.layout.customspinnerdropdownitem);
			//set the array adapter as the instance spinner's adapter.
			measureSpinner.setAdapter(measuresAdapter);
			measureSpinner.setOnItemSelectedListener(this);
			measureSpinner.setSelection(selectedMeasurePos);
		}
	}

	/**
	 * Return Cancel Result
	 */
	private void returnCancelResult() {
		//tell the user that config was cancelled.
		Toast.makeText(this, this.getString(R.string.monitorinstancewidget_configcanceled), 
				Toast.LENGTH_LONG);
		setResult(RESULT_CANCELED); //let the calling activity know that the user chose to 
		//cancel
		
		MonitorInstanceWidgetConfigView.this.finish(); //kill activity
	}
	
	/**
	 * Utility method to save shared preferences.
	 */
	private void saveSharedPreferences(HashMap<String, String> connectionData, String 
			selectedRegion, String instanceId, CloudWatchInput cloudWatchInput) {
		//get an editor for the shared preferences
		SharedPreferences.Editor preferencesEditor = this.getSharedPreferences(
				MONITORINSTANCEWIDGET_SHARED_PREF,
				MODE_PRIVATE)
				.edit();
		
		//we cannot put serializables into preferencesEditor. Bugger.
		preferencesEditor.putString("username", connectionData.get("username"));
		preferencesEditor.putString("accessKey", connectionData.get("accessKey"));
		preferencesEditor.putString("secretAccessKey", connectionData.get("secretAccessKey"));
		
		//write the selected region in
		preferencesEditor.putString("selectedRegion", selectedRegion);
		//write the selected instance ID in
		preferencesEditor.putString("instanceId", instanceId);
		//write the CloudWatchInput details, such as start time, end time, namespace etc
		preferencesEditor.putString("measureName", cloudWatchInput.getMeasureName());
		preferencesEditor.putLong("startTime", cloudWatchInput.getStartTime());
		preferencesEditor.putLong("endTime", cloudWatchInput.getEndTime());
		preferencesEditor.putInt("period", cloudWatchInput.getPeriod());
		preferencesEditor.putString("namespace", cloudWatchInput.getNamespace());
	
		//we cannot write arraylist in. So we will have to write in multiple Strings.
		//this *SUCKS*!
		//TODO this is nasty. Can anybody think of anything better?
		int count = 0; //count to add to each statistic
		//write the stats in
		for (String statistic : cloudWatchInput.getStatistics()) {
			preferencesEditor.putString("statistic_" + count++, statistic);
		}
	}

	/**
	 * Handle the cancellation of the progress bar
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		metricsModel.cancel(true);
	}
	
	/*
	 * 		int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	     Intent intent = getIntent();
	     Bundle extras = intent.getExtras();
	     if (extras != null) {
	         appWidgetId = extras.getInt(
	                 AppWidgetManager.EXTRA_APPWIDGET_ID,
	                 AppWidgetManager.INVALID_APPWIDGET_ID);
	     }
	  
	     // If they gave us an intent without the widget id, just bail.
	     if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
	    	 Log.v(TAG, "EPIC FAIL!");
	         finish();
	     }		
	 * 
	 * final Context context = MonitorInstanceWidgetConfig.this;
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);		
	 * Intent widgetIntent = new Intent(MonitorInstanceWidget.MONITORINSTANCEWIDGET_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(MonitorInstanceWidgetConfig.this, 
				0, widgetIntent, 0);
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, new Date().getTime(), 20*1000, 
				pendingIntent);
		
		MonitorInstanceWidget.saveAlarmInfo(alarmManager, pendingIntent);
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	 */
}
