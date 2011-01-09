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
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;

import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.widgets.MonitorInstanceWidget;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Configure the MonitorInstance widget.
 * This should present the user with a list of users he can pick,
 * and then allow him to pick an instance from a list of instances,
 * and then allow him to pick a measure duration etc
 * @author siddhu
 *
 * 9 Jan 2011
 */
public class MonitorInstanceWidgetConfig extends ListActivity {
	
	/** hashtable to store userdata (username, accesskey, secret accesskey) keyed by username*/
	private Hashtable<String, ArrayList<String>> userData; 
	
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.MonitorInstanceWidgetConfig";
	
	/** The ID of the App widget that called this config */
	private int appWidgetId;
	
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

		//get the list of users from the Database
		userData = new ElasticDroidDB(this).listUserData();
		
		Log.v(TAG, "User data: " + userData.size());
		//if there are no users in the DB, 
		//do not add the widget.
		if (userData.size() == 0) {
			Intent resultIntent = new Intent();
			//set selection size attr in Intent to 0.
			resultIntent.putExtra("SELECTION_SIZE", 0);
			setResult(RESULT_CANCELED, resultIntent);
			finish(); //kill the activity
		}

		//Add New User to list of usernames.
		ArrayList<String> usernames = new ArrayList<String>(userData.keySet());
		
		for (String username : usernames) {
			Log.v(TAG, "Username: " + username);
		}
		//add the usernames to the list adapter and display.
		//add the usernames to the list adapter to display.
		setListAdapter(new UserPickerAdapter(this, R.layout.userpickerrow, 
				(String[])usernames.toArray(new String[ usernames.size()]) ));
	}
	
	/**
	 * Overriden listen method to capture clicks on List Item
	 */
	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		Log.v(TAG, "List item clicked.");
		
		HashMap<String, String> connectionData = new HashMap<String, String>();
		String selectedUserName = list.getItemAtPosition(position).toString();
		
		connectionData.put("username", selectedUserName);
		connectionData.put("accessKey", userData.get(selectedUserName).get(0));
		connectionData.put("secretAccessKey", userData.get(selectedUserName).get(1));
		
		//share the data with the MonitorInstanceWidget
		MonitorInstanceWidget.setPreferences(this, connectionData, "eu-west-1", "i-1c2f2d6b");
		
		Intent resultIntent = new Intent();
		//put in the appwidget ID into the intent
		resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultIntent);
		
		finish(); //kill this activity off
	}
	
	/**
	 * Handle back button.
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
