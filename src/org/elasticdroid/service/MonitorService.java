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
 * Authored by siddhu on 10 Jan 2011
 */
package org.elasticdroid.service;

import java.util.Date;
import java.util.HashMap;

import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.elasticdroid.R;
import org.elasticdroid.widgets.MonitorInstanceWidget;
import org.elasticdroid.widgets.MonitorInstanceWidgetConfigView;

import android.app.Service;
import android.appwidget.AppWidgetManager;
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
/**
 * Service class used to get data received from AWS and build the view.
 * 
 * @author siddhu
 *
 * 10 Jan 2011
 */
public class MonitorService extends Service {
	
	private static final String TAG = "org.elasticdroid.MonitorService";
	/**
	 * Flag to tell the service to stop for this particular app widget ID
	 */
	public static final String FLAG_REQUEST_STOP = "FLAG_REQUEST_STOP";
	/**
	 * Method called when the service is first started.
	 */
    @Override
    public void onStart(Intent intent, int startId) {
    	Log.v(TAG, "Starting service...");
    	
        if (intent.getBooleanExtra(FLAG_REQUEST_STOP, false)) {
        	Log.v(TAG, "Service is being kilt!");
        	this.stopSelf();
        }
        else {
	    	// Build the widget update for today
	        RemoteViews updateViews = buildUpdate(this);
	
	        // Push update for this widget to the home screen
	        ComponentName thisWidget = new ComponentName(this, MonitorInstanceWidget.class);
	        AppWidgetManager manager = AppWidgetManager.getInstance(this);
	        manager.updateAppWidget(thisWidget, updateViews);
        }
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
    	
    	executeModel(context);

    	/*XYMultipleSeriesRenderer multipleRenderer = new XYMultipleSeriesRenderer();
    	XYSeriesRenderer renderer = new XYSeriesRenderer();
		renderer.setColor(Color.RED);
		
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setLineWidth(5);
		
    	multipleRenderer.addSeriesRenderer(renderer);
    	multipleRenderer.setShowAxes(true);
    	multipleRenderer.setShowLabels(true);
    	multipleRenderer.setShowLegend(true);
    	multipleRenderer.setShowGrid(true);
    	multipleRenderer.setLabelsColor(Color.BLACK);
    	multipleRenderer.setAxesColor(Color.BLACK);
    	
    	XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    	
    	XYSeries cloudWatchSeries = new XYSeries("test");
    	cloudWatchSeries.add(new Date().getTime(), 3.0);
    	dataset.addSeries(cloudWatchSeries);
    	
    	Bitmap imageBitmap = Bitmap.createBitmap(494, 420, Bitmap.Config.ARGB_8888);
    	Canvas canvas = new Canvas(imageBitmap);
    	TimeChart tc = new TimeChart(dataset, multipleRenderer);
    	tc.draw(canvas, 0, 0, 494, 420);

    	updateViews.setImageViewBitmap(R.id.widgetChart, imageBitmap);*/
    	
    	return updateViews;
    }
    
    /**
     * Execute the model; use the TestListActivity in testharness for now
     */
    private void executeModel(Context context) {
    	//get the preferences
    	SharedPreferences  preferences = context.getSharedPreferences(
    			MonitorInstanceWidgetConfigView.MONITORINSTANCEWIDGET_SHARED_PREF, 
    			Context.MODE_PRIVATE);
    	
    	//create a HashMap for the connection data
    	HashMap<String, String> connectionData = new HashMap<String, String>();
    	connectionData.put("username", preferences.getString("username", null));
    	connectionData.put("accessKey", preferences.getString("accessKey", null));
    	connectionData.put("secretAccessKey", preferences.getString("secretAccessKey", null));
    	
    	//create the CloudWatchInput
    	//assume duration will be 1 hour
    	long endTime = new Date().getTime();
    	long startTime = endTime - 3600000;
    	//default to eu-west-1

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
