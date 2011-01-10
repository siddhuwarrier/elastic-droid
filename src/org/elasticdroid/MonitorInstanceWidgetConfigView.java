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
package org.elasticdroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.model.RetrieveRegionModel;
import org.elasticdroid.utils.CloudWatchInput;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Configure the MonitorInstance widget.
 * This should present the user with a list of users he can pick,
 * and then allow him to pick an instance from a list of instances,
 * and then allow him to pick a measure duration etc
 * @author siddhu
 *
 * 9 Jan 2011
 */
public class MonitorInstanceWidgetConfigView extends Activity implements OnItemSelectedListener {
	
	/** hashtable to store userdata (username, accesskey, secret accesskey) keyed by username*/
	private Hashtable<String, ArrayList<String>> userData; 
	
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.MonitorInstanceWidgetConfig";
	
	/** The ID of the App widget that called this config */
	private int appWidgetId;
	
	/** The location of Monitor Instance Shared Preferences */
	public static final String MONITORINSTANCEWIDGET_SHARED_PREF = "monitorInstanceWidget.prefs";
	
	/****************************** MODELS ******************************/
	/** Retrieve Regions model */
	private RetrieveRegionModel retrieveRegionsModel;
	
	/** The list of regions */
	HashMap<String, String> regionData;
	
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
		
		//populate the list of users into the Spinner
	}
	
	/**
	 * Restore the instance state on change in Android orientation.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		//restore the user data if any
		userData = (Hashtable<String, ArrayList<String>>)stateToRestore.getSerializable("userData");
		regionData = (HashMap<String, String>)stateToRestore.getSerializable("regionData");
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
			userData = new ElasticDroidDB(this).listUserData();
		}
		
		//get user name data
		ArrayAdapter<String> userSpinnerAdapter = new ArrayAdapter<String>(
				this,
				R.layout.regionspinneritem,
				new ArrayList<String>(userData.keySet())
				);
		userSpinnerAdapter.setDropDownViewResource(R.layout.regionspinnerdropdownitem);
		
		//set the array adapter as the user spinner's adapter.
		Spinner userSpinner = (Spinner) findViewById(R.id.
				monitorInstanceWidgetConfigUsernameSpinner);
		userSpinner.setAdapter(userSpinnerAdapter);
		userSpinner.setOnItemSelectedListener(this);
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
		saveState.putSerializable("regionData", regionData);
	}
	
	/**
	 * Execute retrieveregionsmodel
	 */
	public void executeRetrieveRegionsModel() {
		
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
			//tell the user that config was cancelled.
			Toast.makeText(this, this.getString(R.string.monitorinstancewidget_configcanceled), 
					Toast.LENGTH_LONG);
			setResult(RESULT_CANCELED); //let the calling activity know that the user chose to 
			//cancel
		}
		
		return super.onKeyDown(keyCode, event);
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
	 * This activity is a very Spinnerfull activity. Handle spinner selection.
	 */
	@Override
	public void onItemSelected(AdapterView<?> viewAdapter, View selectedSpinner, int position,
			long id) {
		
		switch(selectedSpinner.getId()) {
		/* Username spinner selected. */
		case R.id.monitorInstanceWidgetConfigUsernameSpinner:
			break;
			
		default:
			break;
		}
	}

	/**
	 * Handle when nothing is selected on a spinner.
	 */
	@Override
	public void onNothingSelected(AdapterView<?> ignore) {
		// Doing nothing here
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
