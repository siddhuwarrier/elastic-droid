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

import java.util.Date;

import org.elasticdroid.service.MonitorService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * The Monitor Instance Widget.
 * 
 * @author siddhu
 *
 * 9 Jan 2011
 */
public class MonitorInstanceWidget extends AppWidgetProvider {
    
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.widgets.MonitorInstanceWidget";
	
	/** The update intent name */
	public static String MONITORINSTANCEWIDGET_UPDATE = "MONITORINSTANCEWIDGET_UPDATE";
	
	/** The alarm manager */
	private static AlarmManager alarmManager;
	
	/** THe pending intent that causes the update intent to be generated. */
	private static PendingIntent pendingIntent;
	
	/**
	 * This method is called whenever the widget is first added to the homescreen.
	 */
	@Override
	public void onEnabled(Context context) {
		Log.v(TAG, "Widget enabled");
		//start the alarm
		//setAlarm(context);
	}

	/**
	 * This method is called whenever a message is received from the Alarm Manager
	 * which is used to set the alarm.
	 */
	@Override
	 public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		String action = intent.getAction();
		
		Log.v(TAG, "onReceive: Action = " + action);
		  
		if(MONITORINSTANCEWIDGET_UPDATE.equals(intent.getAction())){
			Log.v(TAG, "OnReceive(): update received.");
			
			if(intent.getExtras() != null) {
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
				int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
						AppWidgetManager.INVALID_APPWIDGET_ID);
			    onUpdate(context, appWidgetManager, new int[]{appWidgetId});
			}
		}
		else if (action.equals(AppWidgetManager.ACTION_APPWIDGET_DELETED)) {
			//get the app widget ID
			int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
					AppWidgetManager.INVALID_APPWIDGET_ID);
			this.onDeleted(context, new int[]{appWidgetId});
		}
	 }
	
    /**
	 * This method is called to update the app widget.
	 * 
	 * This method is called in this widget by onReceive.
	 */
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	super.onUpdate(context, appWidgetManager, appWidgetIds);
    	Log.v(TAG, "Updating MonitorInstanceWidget...");
    	
    	//loop through all of the app widgets of this type created in the application
    	for (int appWidgetId : appWidgetIds) {
    		executeService(context, appWidgetId, false);
    	}
    }
    
    @Override
	public void onDeleted(Context context, int[] appWidgetIds) {
	    Log.v(TAG, "Deleting widgets...");
	    
	    for (int appWidgetId : appWidgetIds) {
	    	Log.v(TAG, "Cancelling alarm for app widget ID: " + appWidgetId);
	    	cancelAlarm(context, appWidgetId);
	    	Log.v(TAG, "Stopping service...");
	    	executeService(context, appWidgetId, true);
	    }
	}
    
    private void executeService(Context context, int appWidgetId, boolean requestStop) {
		Log.v(TAG, "Creating a service intent");
		Intent serviceIntent = new Intent(context, MonitorService.class);
		//the service shouldn't stop
		serviceIntent.putExtra(MonitorService.FLAG_REQUEST_STOP, requestStop);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    	//run the update only if the alarm has been set. This prevents the service from 
		//executing when the user first creates the widget and is configuring it.
    	context.startService(serviceIntent);
    }
    
    /**
     * Sets an alarm to post a broadcast pending intent for updating a particular appWidgetId
     * 
     * @param context
     *            the Context to set the alarm under
     * @param appWidgetId
     *            the widget identifier for this alarm
     * @param updateRateSeconds
     *            the amount of time between alarms
     */
    public static void setAlarm(Context context, int appWidgetId) {
        Intent widgetUpdate = new Intent();
        widgetUpdate.setAction(MONITORINSTANCEWIDGET_UPDATE);
        widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
        
        //create a pending intent
        PendingIntent newPending = PendingIntent.getBroadcast(context, 0, widgetUpdate, 
        		PendingIntent.FLAG_UPDATE_CURRENT);

		SharedPreferences prefs = context.getSharedPreferences(
				MonitorInstanceWidgetConfigView.MONITORINSTANCEWIDGET_SHARED_PREF,
				Context.MODE_PRIVATE);
		
		//create an alarm only if the config is valid
		if (prefs.getBoolean("configValid", false)) {
	        // schedule the updating
			Log.v(TAG, "Scheduling...");
	        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarms.setRepeating(
            		AlarmManager.RTC_WAKEUP, 
            		new Date().getTime(), 
            		prefs.getLong("interval", 30000), 
            		newPending);
		}
    }
    
    /**
     * 
     * @param context
     */
    private void cancelAlarm(Context context, int appWidgetId) {
        Intent widgetUpdate = new Intent();
        widgetUpdate.setAction(MONITORINSTANCEWIDGET_UPDATE);
        widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });

        //create a pending intent
        PendingIntent newPending = PendingIntent.getBroadcast(context, 0, widgetUpdate, 
        		PendingIntent.FLAG_UPDATE_CURRENT);
        
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(newPending);
    }
    
    /**
	 * Save info about the alarm being used to auto-update the alarm.
	 * This method is a static method called by the Widget configurator.
	 */
	public static void saveAlarmInfo(AlarmManager alarmManager, PendingIntent pendingIntent) {
		MonitorInstanceWidget.alarmManager = alarmManager;
		MonitorInstanceWidget.pendingIntent = pendingIntent;
	}
}
