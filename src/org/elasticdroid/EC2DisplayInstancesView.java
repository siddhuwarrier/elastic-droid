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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.elasticdroid.model.EC2DisplayInstancesModel;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;
import org.elasticdroid.utils.DialogConstants;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

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
		//set the title
		this.setTitle(connectionData.get("username") + " (" + selectedRegion +")");
		
		//set the heading appropriately
		if (listType == InstanceStateConstants.RUNNING) {
			((TextView)findViewById(R.id.ec2DisplayInstancesTextView)).setText(this.getString(
					R.string.ec2displayinstances_running_title));
		} 
		else if (listType == InstanceStateConstants.STOPPED) {
			((TextView)findViewById(R.id.ec2DisplayInstancesTextView)).setText(this.getString(
					R.string.ec2displayinstances_stopped_title));
		}
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li>instanceData: The list of instances</li>
	 * <li>progressDialogDisplayed: Was a progress dialog displayed?</li>
	 * <li>ec2DisplayInstancesModel: The retained config object containing the model object.</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		//restore instance data if any
		instanceData = (ArrayList<Instance>)stateToRestore.getSerializable("instanceData");
		
		//was a progress dialog being displayed.
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		
		/*first off, get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		//if there was a model executing when the object was destroyed.
		if (retained instanceof EC2DisplayInstancesModel) {
			Log.i(this.getClass().getName() + ".onCreate()","Reclaiming previous background task");
			
			ec2DisplayInstancesModel = (EC2DisplayInstancesModel) retained;//force typecast
			ec2DisplayInstancesModel.setActivity(this);//pass the model reference to activity
		} 
		else {
			ec2DisplayInstancesModel = null;
			
			Log.v(this.getClass().getName(),"No model object, or model finished before activity " +
					"was recreated.");
			
			//now if there is no model anymore, and progressDialogDisplayed is set to true,
			//reset it to false, because the model finished executing before the restart
			if (progressDialogDisplayed) {
				progressDialogDisplayed = false;
			}
		}
	}
	
	/**
	 * TODO fill in
	 */
	@Override
	public void onResume() {
		super.onResume(); //call base class method
		 
		 //if there is no model running, start the model
		if (ec2DisplayInstancesModel == null) {
			executeModel();
		}
	}
	
	/**
	 * Save state of the activity on destroy/stop.
	 * Saves:
	 * <ul>
	 * <li> instanceData: The instance data collected.</li>
	 * </ul>
	 */
	@Override
	public void onSaveInstanceState(Bundle saveState) {
		//if we have instance data, save it.
		//but don't bother saving it if the model is not null, i.e. a new model
		//is executing.
		if ((instanceData != null) && (ec2DisplayInstancesModel == null)) {
			saveState.putSerializable("instanceData", instanceData);
		}
		
		//save if progress dialog is being displayed.
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
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
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
			progressDialogDisplayed = false;
		}
		
		//get the model data
		if (result instanceof ArrayList<?>) {
			try {
				instanceData = (ArrayList<Instance>)result;
				
				//add the usernames to the list adapter to display.
				setListAdapter(new EC2DisplayInstancesAdapter(this, R.layout.ec2displayinstancesrow, 
						instanceData, listType));
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
	
	/**
	 * Save reference to {@link org.elasticdroid.model.EC2DisplayInstancesModel Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.v(this.getClass().getName(), "Object about to destroyed...");

		// if the model is being executed when the onDestroy method is called.
		//tell the model that the activity has now disappeared. Hopefully, the
		//activity will return.
		if (ec2DisplayInstancesModel != null) {
			ec2DisplayInstancesModel.setActivity(null);
			return ec2DisplayInstancesModel;
		}
		//if there was no model being executed, just return null
		return null;
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

/**
 * Adapter to display the instances in a list view. 
 * @author Siddhu Warrier
 *
 * 6 Dec 2010
 */
class EC2DisplayInstancesAdapter extends ArrayAdapter<Instance>{

	/** Instance list */
	private ArrayList<Instance> instanceData;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	private Context context;
	/** List type */
	private byte listType;
	/**
	 * @param context
	 * @param textViewResourceId
	 */
	public EC2DisplayInstancesAdapter(Context context, int textViewResourceId, 
			ArrayList<Instance> instanceData, byte listType) {
		super(context, textViewResourceId, instanceData);
		
		//save the context, data, and list type
		this.context = context;
		this.instanceData = instanceData;
		this.listType = listType;
	}
	
	/**
	 * Overriden method called when ListView is initialised with data.
	 * @param position The position in {@link #instanceData}.
	 * @param convertView The view to set.
	 * @param parent
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View instanceDataRow = convertView;
		String details = "";
		if (instanceDataRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
			(Context.LAYOUT_INFLATER_SERVICE);
		
			instanceDataRow = inflater.inflate(R.layout.ec2displayinstancesrow, parent, false);
		}
		//set main text view
		TextView textViewHeadline = (TextView)instanceDataRow.findViewById(R.id.instanceHeadline);
		TextView textViewDetails = (TextView)instanceDataRow.findViewById(R.id.instanceDetails);
		
		boolean nameTagFound = false;
		for (Tag tag : instanceData.get(position).getTags()) {
			if (tag.getKey().equalsIgnoreCase("name")) {
				nameTagFound = true;
				textViewHeadline.setText("Tag: " + tag.getValue());
				details +="<i>ID:</i> " + instanceData.get(position).getInstanceId() + ", ";
				break;
			}
		}
		
		
		details += "<i>Type:</i> " + instanceData.get(position).getInstanceType() + ", "; 
		//set Instance ID as headline if no tag named "name"(case-insensitive) found.
		if (!nameTagFound) {
			textViewHeadline.setText("ID: " + instanceData.get(position).getInstanceId());
		}

		//get platform
		details += "<i>OS:</i> " + (instanceData.get(position).getPlatform() == null?"Linux": instanceData.
				get(position).getPlatform());
		
		//don't bother getting day launched if the instance is stopped
		if (listType == InstanceStateConstants.RUNNING) {
			//get period running in hours.
			float timeRunning = ((new Date().getTime() - instanceData.get(position).getLaunchTime().getTime()) / 
					(1000 * 60 * 60));
			
			//if been running greater than 24 hours, convert to days
			if (timeRunning > 24) {
				timeRunning /= 24;
				details += ", Started " +new DecimalFormat("#.#").format(timeRunning) + " days ago";
			}
			else {
				details += ", Started<i> " + new DecimalFormat("#.#").format(timeRunning) + "</i> hrs ago)";
			}
		}
		textViewDetails.setText(Html.fromHtml(details));
		return instanceDataRow;
	}
	
}
