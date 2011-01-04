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

import static org.elasticdroid.utils.MonitoringDurations.LAST_DAY;
import static org.elasticdroid.utils.MonitoringDurations.LAST_HOUR;
import static org.elasticdroid.utils.MonitoringDurations.LAST_SIX_HOURS;
import static org.elasticdroid.utils.MonitoringDurations.LAST_TWELVE_HOURS;

import java.util.ArrayList;
import java.util.Date;

import org.elasticdroid.tpl.GenericActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Graph to display options to be set for monitoring instances.
 * 
 * @author siddhu
 *
 * 3 Jan 2011
 */
public class MonitorInstanceOptionsView extends GenericActivity implements OnClickListener {

	/** The list of measurenames; returned by the model */
	ArrayList<String> measureNames;
	/** Logging tag */
	private final static String TAG = "org.elasticdroid.MonitorInstanceOptionsView";
	
	/**
	 * Called when activity is first displayed.
	 * Should receive list of regions as Intent.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(TAG, "Starting MonitorInstanceOptionsView");
		Intent intent = this.getIntent();
		
		this.measureNames = intent.getStringArrayListExtra("measureNames");

    	//set the content view
    	this.setContentView(R.layout.monitorinstanceoptions);
    	this.setTitle(this.getString(R.string.monitorinstanceview_graphtype)); //set title
    	
    	//add a listener for the OK button
    	(this.findViewById(R.id.changeButton)).setOnClickListener(this);
    	
    	//populate the measureSpinner
		populateMeasureSpinner();
		
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
	 * Populate the measure spinner
	 */
	private void populateMeasureSpinner() {
		
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_spinner_item, measureNames);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		//set the adapter.
		((Spinner) findViewById(R.id.measureSpinner)).setAdapter(spinnerAdapter);
		
		//get the selected measurename sent by intent, and set it.
		int selectedMeasureIdx = this.getIntent().getIntExtra("selectedMeasureIdx", 0);
		//if the selected measure name is found, set the spinner to show that as the selection.
		((Spinner) findViewById(R.id.measureSpinner)).setSelection(selectedMeasureIdx);
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
	 * No progress dialog displayed, so nothing done.
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
	}

	/**
	 * No model executed; does nothing
	 */
	@Override
	public void processModelResults(Object ignore) {
		// IGnore
		
	}
}
