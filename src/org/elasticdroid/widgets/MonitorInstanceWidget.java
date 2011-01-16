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
import java.util.HashMap;

import org.elasticdroid.service.MonitorService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

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
	
	/** The name of the shared preferences file */
	public static String MONITORINSTANCE_WIDGET_PREFS = "elasticdroid_mi_widget_prefs";
	
	
	/** The alarm manager */
	private static AlarmManager alarmManager;
	
	/** THe pending intent that causes the update intent to be generated. */
	private static PendingIntent pendingIntent;
	
	/** The connection data; shared across all instances of the MonitorInstanceWidget */
	private static HashMap<String, String> connectionData;
	
	/** The selected region; shared across all instances of the MonitorInstanceWidget */
	private static String region;	
	
	/** The selected instance ID; shared across all instances of the MonitorInstanceWidget */
	private static String instanceId;
	
	/**
	 * This method is called whenever a message is received from the Alarm Manager
	 * which is used to set the alarm.
	 */
	@Override
	 public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		  
		if(MONITORINSTANCEWIDGET_UPDATE.equals(intent.getAction())){
			Log.v(TAG, "OnReceive(): update received.");
			
			if(intent.getExtras() != null) {
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
				ComponentName thisAppWidget = new ComponentName(context.getPackageName(), 
						MonitorInstanceWidget.class.getName());
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
			    onUpdate(context, appWidgetManager, appWidgetIds);
			}
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
    	
    	context.startService(new Intent(context, MonitorService.class));
    }
    
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
     * Cancel the alarm on widget being removed. This cancels the alarm manager if there is one.
     */
    @Override
    public void onDisabled(Context context) {
    	Log.v(TAG, "Disabling the widget...");
    	Toast.makeText(context, "Disabling the widget..", Toast.LENGTH_LONG).show();
    	
    	if (alarmManager != null) {
    		//cancel the alarm
    		alarmManager.cancel(pendingIntent);
    	}
    }
    
    /**
	 * Set an alarm up to refresh the screen.
	 * @param context
	 */
	private void setAlarm(Context context) {
		Intent widgetIntent = new Intent(MonitorInstanceWidget.MONITORINSTANCEWIDGET_UPDATE);
		
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 
				0, widgetIntent, 0);
		
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, new Date().getTime(), 20*1000, 
				pendingIntent);
		
		//have the widget save the alarm info
		saveAlarmInfo(alarmManager, pendingIntent);
	}

	/**
     * Save the preferences set by the user. This method is called by the widget configurator.
     * @param context The context that is handling the appwidget
     * @param connectionData AWS connection data
     * @param region AWS region
     * @param instanceId AWS instance ID
     */
    public static void setPreferences(Context context, HashMap<String, String> connectionData, 
    		String region, String instanceId) {
    	MonitorInstanceWidget.connectionData = connectionData;
    	MonitorInstanceWidget.region = region;
    	MonitorInstanceWidget.instanceId = instanceId;
    	//TODO save to SharedPreferences?
    	
    	SharedPreferences sharedPreferences = context.getSharedPreferences(
    			"", 0);
    	
    	SharedPreferences.Editor edit = sharedPreferences.edit();
    	edit.putString("username", connectionData.get("username"));
    	edit.putString("accessKey", connectionData.get("accessKey"));
    	edit.putString("secretAccessKey", connectionData.get("secretAccessKey"));
    	edit.putString("region", region);
    	edit.putString("instanceId", instanceId);
    	
    	edit.commit();
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
