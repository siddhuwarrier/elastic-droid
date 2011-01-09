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

import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.elasticdroid.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * @author siddhu
 *
 * 9 Jan 2011
 */
public class MonitorInstanceWidget extends AppWidgetProvider {
    
	/** Logging tag */
	public static final String TAG = "org.elasticdroid.widgets.MonitorInstanceWidget";
	/** The update intent name */
	public static String MONITORINSTANCEWIDGET_UPDATE = "MONITORINSTANCEWIDGET_UPDATE";
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
	
	@Override
	public void onEnabled(Context context) {
		Log.v(TAG, "Widget enabled");
		//start the alarm
		setAlarm(context);
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
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	super.onUpdate(context, appWidgetManager, appWidgetIds);
    	Log.v(TAG, "Updating MonitorInstanceWidget...");
    	
    	context.startService(new Intent(context, MonitorService.class));
    }
    
    /**
     * Save info about the alarm being used to auto-update the alarm.
     */
    public static void saveAlarmInfo(AlarmManager alarmManager, PendingIntent pendingIntent) {
    	MonitorInstanceWidget.alarmManager = alarmManager;
    	MonitorInstanceWidget.pendingIntent = pendingIntent;
    }
 
    /**
     * Cancel the alarm on widget being removed.
     */
    @Override
    public void onDisabled(Context context) {
    	Log.v(TAG, "Disabling the widget...");
    	Toast.makeText(context, "Disabling the widget..", Toast.LENGTH_LONG).show();
    	
    	//cancel the alarm
    	alarmManager.cancel(pendingIntent);
    }
    
    /**
     * Save the preferences set by the user.
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
    			"elasticdroid_mi_widget_prefs", 0);
    	
    	SharedPreferences.Editor edit = sharedPreferences.edit();
    	edit.putString("username", connectionData.get("username"));
    	edit.putString("accessKey", connectionData.get("accessKey"));
    	edit.putString("secretAccessKey", connectionData.get("secretAccessKey"));
    	edit.putString("region", region);
    	edit.putString("instanceId", instanceId);
    	
    	edit.commit();
    }
    
    //nested service class
    public static class MonitorService extends Service {

        @Override
        public void onStart(Intent intent, int startId) {
        	Log.v(TAG, "Starting service...");
        	// Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, MonitorInstanceWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }
        
        /**
         * Build a widget update to change the text view
         */
        public RemoteViews buildUpdate(Context context) {
        	Log.v(TAG, "Building update...");
        	
        	RemoteViews updateViews = null;
        	
        	updateViews = new RemoteViews(context.getPackageName(), R.layout.monitorinstancewidget);
        	updateViews.setTextViewText(R.id.monitoringWidgetTextView, "Time:" + new Date().
        			toString());
        	
        	Log.v(TAG, "Trying to get a chart to render!");
        	XYMultipleSeriesRenderer multipleRenderer = new XYMultipleSeriesRenderer();
        	XYSeriesRenderer renderer = new XYSeriesRenderer();
    		renderer.setColor(Color.RED);
    		renderer.setPointStyle(PointStyle.CIRCLE);
    		renderer.setLineWidth(5);
    		
        	multipleRenderer.addSeriesRenderer(renderer);
        	multipleRenderer.setShowAxes(true);
        	multipleRenderer.setShowLabels(false);
        	multipleRenderer.setShowLegend(false);
        	multipleRenderer.setShowGrid(false);
        	XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        	
        	XYSeries cloudWatchSeries = new XYSeries("test");
        	cloudWatchSeries.add(1.0, 2.0);
        	cloudWatchSeries.add(3.0, 5.0);
        	cloudWatchSeries.add(6.0, 1.0);
        	
        	dataset.addSeries(cloudWatchSeries);
        	
        	Bitmap imageBitmap = Bitmap.createBitmap(294, 220, Bitmap.Config.ARGB_8888);
        	Canvas canvas = new Canvas(imageBitmap);
        	TimeChart tc = new TimeChart(dataset, multipleRenderer);
        	tc.draw(canvas, 0, 0, 294, 220);

        	updateViews.setImageViewBitmap(R.id.widgetChart, imageBitmap);
        	
        	return updateViews;
        }

    	/**
    	 * Service stuff: TODO write a better comment.
    	 */
    	@Override
    	public IBinder onBind(Intent intent) {
    		// we do not need to bind to this service
    		return null;
    	}
    	
    }
}
