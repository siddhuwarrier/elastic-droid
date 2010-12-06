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
 * Authored by Siddhu Warrier on 5 Dec 2010
 */
package org.elasticdroid;

import static org.elasticdroid.utils.ResultConstants.RESULT_ERROR;

import java.util.ArrayList;
import java.util.HashMap;

import org.elasticdroid.model.EC2DisplayInstancesModel;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;
import org.elasticdroid.utils.DialogConstants;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Instance;

/**
 * This class will display a list of instances that are
 * running or stopped.
 * 
 * This class extends GenericListActivity.
 * 
 * This class may later be extended to also handle
 * keypair and security group display.
 * @author Siddhu Warrier 
 *
 * 5 Dec 2010
 */
public class EC2DisplayInstancesView extends GenericListActivity {

	/**
	 * The type of list to display. Accepted values atm are RUNNING and STOPPED
	 */
	private byte listType;
	/** The selected region */
	private String selectedRegion;
    /** The connection data */
    private HashMap<String,String> connectionData;
    /**Is progress bar displayed */
    private boolean progressDialogDisplayed;
    /**The model object */
    private EC2DisplayInstancesModel ec2DisplayInstancesModel;
    /**The model result: an ArrayList of corresponding instances */
    private ArrayList<Instance> instanceData;
	
	/**
	 * This method is called when the activity is (re)started, and receives
	 * an {@link org.elasticdroid.utils.AWSConstants.InstanceConstants} enumerator
	 * as an intent, which tells it what sort of list to display. 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); //call superclass onCreate
		
		/* get intent data */
		//get the type of list to display from the intent.
		Intent intent = this.getIntent();
		listType = intent.getByteExtra("listType", InstanceStateConstants.RUNNING);
		selectedRegion = intent.getStringExtra("selectedRegion");
		
    	try {
    		this.connectionData = (HashMap<String, String>)intent.getSerializableExtra(
    				"org.elasticdroid.EC2DashboardView.connectionData");
    	}
    	//the possible exceptions are NullPointerException: the Hashmap was not found, or
    	//ClassCastException: the argument passed is not Hashmap<String, String>. In either case,
    	//just print out the error and exit. This is very inelegant, but this is a programmer's bug
    	catch(Exception exception) {
    		Log.e(this.getClass().getName(), exception.getMessage());
    		//return the failure to the mama class 
			Intent resultIntent = new Intent();
			resultIntent.setType(this.getClass().getName());
			resultIntent.putExtra("EXCEPTION_MSG", this.getClass().getName() + ":" + 
					exception.getMessage());
			setResult(RESULT_ERROR, resultIntent);
    	}
		
		//set the content view
		setContentView(R.layout.ec2displayinstances);
		
		if (listType == InstanceStateConstants.RUNNING) {
			this.setTitle(this.getString(R.string.ec2displayinstances_running_title));
		} else if (listType == InstanceStateConstants.STOPPED) {
			this.setTitle(this.getString(R.string.ec2displayinstances_stopped_title));
		}
		
		//set the heading
		((TextView)findViewById(R.id.ec2DisplayInstancesTextView)).setText(this.getString(
				R.string.ec2displayinstances_running_title));
		
		((TextView)findViewById(R.id.ec2DisplayInstancesRegionTextView)).setText(
				this.getString(R.string.ec2dashview_region) + " " +
				selectedRegion);
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li>regionData: The list of regions</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		// restore region data
		Log.v(this.getClass().getName(), "Restoring regionData...");
	}
	
	/**
	 * TODO fill in
	 */
	@Override
	public void onResume() {
		super.onResume(); //call base class method
		 
		 //start the model
		 executeModel();
	}
	
	/**
	 * Save state of the activity on destroy/stop
	 */
	@Override
	public void onSaveInstanceState(Bundle saveState) {
	}
	
	//overriden methods
	/** 
	 * This method processes results generated by the model. 
	 * @see org.elasticdroid.GenericListActivity#processModelResults(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		Log.v(this.getClass().getName()+".processModelResults()", "Model returned...");
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		//get the model data
		if (result instanceof ArrayList<?>) {
			try {
				instanceData = (ArrayList<Instance>)result;
			}
			catch(Exception exception) {
	    		Log.e(this.getClass().getName(), exception.getMessage());
	    		//return the failure to the mama class 
				Intent resultIntent = new Intent();
				resultIntent.setType(this.getClass().getName());
				resultIntent.putExtra("EXCEPTION_MSG", this.getClass().getName() + ":" + 
						exception.getMessage());
				setResult(RESULT_ERROR, resultIntent);
			}	
		}
		else if (result instanceof AmazonServiceException) {
			//TODO display alert dialog msg
		}
		else if (result instanceof AmazonClientException) {
			//TODO display alert dialog msg
		}
		else if (result instanceof IllegalArgumentException) {
			//TODO display alert dialog msg
		}
		
		for (Instance instance : instanceData) {
			Log.v(this.getClass().getName(),
					"Instance ID: " + instance.getInstanceId());
			Log.v(this.getClass().getName(),
					"Platform: " + instance.getPlatform());
			Log.v(this.getClass().getName(),
					"Public DNS: " + instance.getPublicDnsName());
			Log.v(this.getClass().getName(),
					"Launch time: " + instance.getLaunchTime().toString());
		}
	}
	
	/**
	 * Handle back button.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
    		//return the failure to the mama class 
			Intent resultIntent = new Intent();
			resultIntent.setType(this.getClass().getName());
			
			setResult(RESULT_CANCELED, resultIntent); //let the calling activity know that the user chose to 
			//cancel
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * Function that handles the display of a progress dialog. Overriden from
	 * Activity and not GenericActivity
	 * 
	 * @param id
	 *            Dialog ID - Special treatment for Constants.PROGRESS_DIALOG
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(this.getString(R.string.loginview_wait_dlg));
			dialog.setCancelable(false);

			progressDialogDisplayed = true;
			Log.v(this.getClass().getName(), "progress dialog displayed="
					+ progressDialogDisplayed);

			return dialog;
		}
		// if some other sort of dialog...
		return super.onCreateDialog(id);
	}
	
	//private methods
	/**
	 * Execute the model to retrieve EC2 instance data for the selected region. The model
	 * runs in a different thread and calls processModelResults when done.
	 */
	private void executeModel() {
		ec2DisplayInstancesModel = new EC2DisplayInstancesModel(this);
		// add the endpoint for this region to connectionData
		// it's not nice to modify a member like this, now, is it?
		connectionData.put("region", selectedRegion);
		connectionData.put("listType", String.valueOf(listType));
		ec2DisplayInstancesModel.execute(new HashMap<?, ?>[] { connectionData });
	}
}
