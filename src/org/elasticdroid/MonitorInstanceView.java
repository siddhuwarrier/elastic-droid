/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticdroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.elasticdroid.model.MonitorInstanceModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.utils.CloudWatchInput;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;

public class MonitorInstanceView extends GenericActivity {
	/**
	 * The AWS connection data
	 */
	private HashMap<String, String> connectionData;

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
	 * String holding Instance ID
	 */
	private String instanceId;
	/**
	 * Monitor Instance model
	 */
	private MonitorInstanceModel monitorInstanceModel;
	/**
	 * The {@link CloudWatchInput} data to provide the model with.
	 */
	private CloudWatchInput cloudWatchInput;
	/** String holding selected region */
	private String selectedRegion;
	/** Data from CloudWatch */
	private List<Datapoint> cloudWatchData;
	
	
	/**
	 * Tag for logging
	 */
	private static final String TAG = "org.elasticdroid.MonitorInstanceView";
	
	//charting stuff: achartengine
	/**
	 * The dataset to hold the displayed data
	 */
	private XYMultipleSeriesDataset dataset;

	/**
	 * Chart renderer. renders the dataset
	 */
	private XYMultipleSeriesRenderer multiRenderer;
	/**
	 * The chart itself. Added to layout.
	 */
	private GraphicalView chartView;
	
  	/**
  	 * Executed when the activity is first (re)created.
  	 * @param savedInstanceState Instance state to restore (if any)
  	 */
  	@SuppressWarnings("unchecked")
	@Override
  	public void onCreate(Bundle savedInstanceState) {
  		super.onCreate(savedInstanceState);
  		
  		Intent intent = this.getIntent();
  		
    	try {
    		this.connectionData = (HashMap<String, String>)intent.getSerializableExtra(
    				"org.elasticdroid.EC2SingleInstanceView.connectionData");
    	}
    	catch(Exception exception) {
        	//the possible exceptions are NullPointerException: the Hashmap was not found, or
        	//ClassCastException: the argument passed is not Hashmap<String, String>. In either case,
        	//just print out the error and exit. This is very inelegant, but this is a programmer's bug
    		Log.e(TAG, exception.getMessage());
    		finish(); //this will cause it to return to {@link EC2DisplayInstancesView}.
    	}
    	//get instance ID and selected region from the intent
    	this.instanceId = intent.getStringExtra("instanceId");
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
							MonitorInstanceView.this.finish();
						}
					}
				}
			);
		
  		this.setContentView(R.layout.monitorinstancesview);
  		
  		dataset = new XYMultipleSeriesDataset();
  		multiRenderer = new XYMultipleSeriesRenderer();
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
		
		
		//restore the input data
		cloudWatchInput = (CloudWatchInput)(stateToRestore.getSerializable("cloudWatchInput"));
	}
	
	@Override
  	public void onResume() {
  		super.onResume();
		//if there was a dialog box, display it
		//if failed, then display dialog box.
  		
  		if (cloudWatchInput == null) {
  			setCloudWatchInputDefaults();
  		}
  		
  		Log.d(TAG + ".onResume()", "onResume");
  		
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
		//execute the model if it's not already running, and we have no chart yet
		else if ((monitorInstanceModel == null) && (chartView == null)) {
			Log.d(TAG, "About to execute model...");
			executeModel();
		}
  	}
	
	/**
	 * Save state of the activity on destroy/stop.
	 * Apart from the usual, this saves:
	 * <ul>
	 * <li> cloudWatchInput: The cloudwatch input set (if any).</li>
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
		
		if (cloudWatchInput != null) {
			saveState.putSerializable("cloudWatchInput", cloudWatchInput);
		}
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.MonitorInstanceModel} Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This is done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		//save the monitor instance model if the model is running
		if (monitorInstanceModel != null) {
			monitorInstanceModel.setActivityNull(); //tell the model the activity is going on hols.
			return monitorInstanceModel;
		}
		
		return null;
	}
  	
  	/**
  	 * Execute model
  	 */
  	private void executeModel() {
  		//create a dimension which specifies that we want to see data for this instance
  		Dimension dimension = new Dimension();
  		dimension.setName("InstanceId");
  		dimension.setValue(instanceId);
  		
  		//create and start the model
  		monitorInstanceModel = new MonitorInstanceModel(this, connectionData, cloudWatchInput);
  		monitorInstanceModel.execute(dimension);
  	}
  	
	/**
	 * Process model results
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		monitorInstanceModel = null; //set the model to null
		
		if (result instanceof List<?>) {
			cloudWatchData = (List<Datapoint>) result;
			Log.v(TAG, "Drawing chart...");
			//draw chart
			drawChart();
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
	 * Small utility method to populate cloudWatchInput with defaults.
	 */
	private void setCloudWatchInputDefaults() {
		long timeNow = new Date().getTime();
		long timeOneHrAgo = timeNow - 3600000; //subtract 3,600,000 milliseconds	
		
		cloudWatchInput = new CloudWatchInput(timeOneHrAgo, timeNow, new Integer(300), 
				"CPUUtilization", "AWS/EC2", 
				new ArrayList<String>(Arrays.asList(new String[]{"Average"})), selectedRegion);
	}


	/**
	 * Draw chart
	 * TODO use real data
	 */
	private void drawChart() {
		XYSeries cloudWatchSeries = new XYSeries("Graph");
		
		//remove all existing series. We are going to display only one at a time
		for (int seriesCount = 0; seriesCount < dataset.getSeriesCount(); seriesCount ++) {
			dataset.removeSeries(seriesCount);
			multiRenderer.removeSeriesRenderer(multiRenderer.getSeriesRendererAt(seriesCount));
		}
		
		//add data into the series
		for (Datapoint cloudWatchDatum : cloudWatchData) {
			//add the timestamp and the data
			Log.v(TAG, "Timestamp:" + cloudWatchDatum.getTimestamp().toString() + ", Val: " + 
					cloudWatchDatum.getAverage());
			cloudWatchSeries.add(cloudWatchDatum.getTimestamp().getTime(), cloudWatchDatum.
					getAverage());
		}
		
		dataset.addSeries(cloudWatchSeries);
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		renderer.setColor(Color.RED);
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setLineWidth(5);
		
		multiRenderer.addSeriesRenderer(renderer);
		multiRenderer.setAntialiasing(true);
		multiRenderer.setXTitle("Count");
		multiRenderer.setYTitle("Value");
		
		multiRenderer.setLabelsTextSize(16);
		multiRenderer.setAxisTitleTextSize(20);
		multiRenderer.setShowLegend(false);
		multiRenderer.setShowGrid(true);
		
		if (chartView == null) {
			//chartView = ChartFactory.getLineChartView(this, dataset, multiRenderer);
			chartView = ChartFactory.getTimeChartView(this, dataset, multiRenderer, "HH:mm");
			
		    LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
		    layout.addView(chartView, new LayoutParams(LayoutParams.FILL_PARENT,
		              LayoutParams.FILL_PARENT));
		}
		else {
			chartView.repaint();
		}
	}
	
	/* (non-Javadoc)
	 * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		// TODO Auto-generated method stub
		
	}
}
