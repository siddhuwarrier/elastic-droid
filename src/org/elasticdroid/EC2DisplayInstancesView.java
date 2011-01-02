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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.elasticdroid.model.EC2InstancesModel;
import org.elasticdroid.model.ds.SerializableInstance;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.DialogConstants;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;

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
	private int listType;
	/** The selected region */
	private String selectedRegion;
    /** The connection data */
    private HashMap<String,String> connectionData;
    /**The model object */
    private EC2InstancesModel ec2InstancesModel;
    
	/** Dialog box for credential verification errors */
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
    
    /**The model result: an ArrayList of corresponding instances
     * Uses Serializable Instance and not AWS Instance. {@link SerializableInstance} 
     * */
    private ArrayList<SerializableInstance> instanceData;
	
	/**
	 * This method is called when the activity is (re)started.
	 * 
	 * @param savedInstanceState Instance state saved (if any) on screen destroy. See 
	 * @see EC2DisplayInstancesView#onSaveInstanceState(Bundle)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); //call superclass onCreate
		
		/* get intent data */
		//get the type of list to display from the intent.
		Intent intent = this.getIntent();
		listType = intent.getIntExtra("listType", InstanceStateConstants.RUNNING);
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
    	
		// create and initialise the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); // create alert
																	// box to
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
							EC2DisplayInstancesView.this.finish();
							Intent loginIntent = new Intent();
							loginIntent.setClassName("org.elasticdroid",
									"org.elasticdroid.LoginView");
							startActivity(loginIntent);
						}
					}
				});
		
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
		// restore dialog data
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = "
				+ alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		//restore instance data if any
		instanceData = (ArrayList<SerializableInstance>)stateToRestore.getSerializable("instanceData");
		
		//was a progress dialog being displayed.
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		Log.v(this.getClass().getName() + ".onRestoreInstanceState", "progbar:" + 
				progressDialogDisplayed);
		
		/*get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		//if there was a model executing when the object was destroyed.
		if (retained instanceof EC2InstancesModel) {
			Log.i(this.getClass().getName() + ".onRestoreInstanceState()","Reclaiming previous " +
					"background task");
			
			ec2InstancesModel = (EC2InstancesModel) retained;//force typecast
			ec2InstancesModel.setActivity(this);//pass the model reference to activity
		} 
		else {
			ec2InstancesModel = null;
			
			Log.v(this.getClass().getName(),"No model object, or model finished before activity " +
					"was recreated.");
			
			//now if there is no model anymore, and progressDialogDisplayed is set to true,
			//reset it to false, because the model finished executing before the restart
			if (progressDialogDisplayed) {
				progressDialogDisplayed = false;
			}
		
			//if we have instance data, reload the list
			if (instanceData != null) {
				setListAdapter(new EC2DisplayInstancesAdapter(this, R.layout.ec2displayinstancesrow, 
						instanceData, listType));
			}
		}
	}
	
	/**
	 * Executed last in the (re)start lifecycle, this method starts the model if both these 
	 * conditions are met:
	 * 
	 * <ul>
	 * <li>There is no currently running model.</li>
	 * <li>There is no instance data already computed.</li>
	 * </ul>
	 */
	@Override
	public void onResume() {
		super.onResume(); //call base class method
		
		//if there was a dialog box, display it
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		} else if ((ec2InstancesModel == null) && (instanceData == null)) {
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
		
		// if a dialog is displayed when this happens, dismiss it
		if (alertDialogDisplayed) {
			alertDialogBox.dismiss();
		}
		//save the info as to whether dialog is displayed
		saveState.putBoolean("alertDialogDisplayed", alertDialogDisplayed);
		//save the dialog msg
		saveState.putString("alertDialogMessage", alertDialogMessage);
		
		//if we have instance data, save it.
		//but don't bother saving it if the model is not null, i.e. a new model
		//is executing.
		if ((instanceData != null) && (ec2InstancesModel == null)) {
			saveState.putSerializable("instanceData", instanceData);
		}
		
		//save if progress dialog is being displayed.
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
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
		if (ec2InstancesModel != null) {
			ec2InstancesModel.setActivityNull();
			return ec2InstancesModel;
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
		ec2InstancesModel = new EC2InstancesModel(this, connectionData, selectedRegion);
		// add the endpoint for this region to connectionData
		// it's not nice to modify a member like this, now, is it?
		
		Filter instanceStateFilter = new Filter("instance-state-code");
		if (listType == InstanceStateConstants.RUNNING) {
			instanceStateFilter.setValues(Arrays.asList(
					new String[]{String.valueOf(InstanceStateConstants.RUNNING)}));
		}
		else {
			instanceStateFilter.setValues(Arrays.asList(
					new String[]{String.valueOf(InstanceStateConstants.STOPPED)}));
		}
		
		ec2InstancesModel.execute(instanceStateFilter);
	}

	//overriden methods
	/** 
	 * This method processes results generated by the model. 
	 * @see org.elasticdroid.tpl.GenericListActivity#processModelResults(java.lang.Object)
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
		
		//i.e. user did not cancel
		if (result != null) {
			//set reference to model object to null
			ec2InstancesModel = null;
			
			//get the model data
			if (result instanceof ArrayList<?>) {
				try {
					instanceData = (ArrayList<SerializableInstance>)result;
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
				
				if (instanceData.size() != 0) {
					//add the instances to the list adapter to display.
					setListAdapter(new EC2DisplayInstancesAdapter(this, R.layout.ec2displayinstancesrow, 
							instanceData, listType));
				}
				//if no data found, just show a String adapter
				else {
					setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 
							new String[]{"No instances found."}));
				}
			}
			else if (result instanceof AmazonServiceException) {
				// if a server error
				if (((AmazonServiceException) result).getErrorCode()
						.startsWith("5")) {
					alertDialogMessage = "AWS server error.";
				} else {
					alertDialogMessage = this.getString(R.string.loginview_invalid_keys_dlg);
				}
				alertDialogDisplayed = true;
				killActivityOnError = false;//do not kill activity on server error
				//allow user to retry.
			}
			else if (result instanceof AmazonClientException) {
				alertDialogMessage = this.getString(R.string.loginview_no_connxn_dlg);
				alertDialogDisplayed = true;
				killActivityOnError = false;//do not kill activity on connectivity error. allow client 
			}
			else if (result instanceof IllegalArgumentException) {
				alertDialogMessage = this
				.getString(R.string.ec2dashview_illegal_arg_exception);
				alertDialogDisplayed = true;
				killActivityOnError = true;
			}
		}
		else {
			Toast.makeText(this, Html.fromHtml(this.getString(R.string.cancelled)), Toast.
					LENGTH_LONG).show();
		}
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
	}

	/**
	 * Handle the selection of a given instance, and pass the relevant SerializableInstance object
	 * on.
	 */
	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		Intent displaySingleInstanceIntent = new Intent();
		displaySingleInstanceIntent.setClassName("org.elasticdroid",
			"org.elasticdroid.EC2SingleInstanceView");
		//send it the AWS connection data.
		displaySingleInstanceIntent.putExtra(
				"org.elasticdroid.EC2DashboardView.connectionData",
				connectionData); // aws connection info
		//send it a single SerializableInstance
		displaySingleInstanceIntent.putExtra("org.elasticdroid.model.SerializableInstance",
				instanceData.get(position));
		//send it the selected region
		displaySingleInstanceIntent.putExtra("selectedRegion", selectedRegion);
		
		//start the activity
		startActivity(displaySingleInstanceIntent);
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
	 * Overridden method to display the menu on press of the menu key
	 * 
	 * Inflates and shows menu for displayed instances view.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.displayinstances_menu, menu);
		return true;
	}

	/**
	 * Overriden method to handle selection of menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem selectedItem) {
		switch (selectedItem.getItemId()) {
		
		case R.id.displayinstances_menuitem_about:
			Intent aboutIntent = new Intent(this, AboutView.class);
			startActivity(aboutIntent);
			return true;
		case R.id.displayinstances_menuitem_refresh:
			executeModel();
			
			return true;
		
		default:
			return super.onOptionsItemSelected(selectedItem);
		}
	}
	
	/** 
	 * Handle cancel of progress dialog
	 * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.
	 * DialogInterface)
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		//this cannot be called UNLESS the user has the model running.
		//i.e. the prog bar is visible
		progressDialogDisplayed = false;
		ec2InstancesModel.cancel(true);
	}
}

/**
 * Adapter to display the instances in a list view. 
 * @author Siddhu Warrier
 *
 * 6 Dec 2010
 */
class EC2DisplayInstancesAdapter extends ArrayAdapter<SerializableInstance>{

	/** Instance list */
	private ArrayList<SerializableInstance> instanceData;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	private Context context;
	/** List type */
	private int listType;
	/**
	 * @param context
	 * @param textViewResourceId
	 */
	public EC2DisplayInstancesAdapter(Context context, int textViewResourceId, 
			ArrayList<SerializableInstance> instanceData, int listType) {
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
		
		//set Instance ID as headline if no tag named "name"(case-insensitive) found.
		if (instanceData.get(position).getTag() == null) {
			textViewHeadline.setText(String.format(
					context.getString(R.string.ec2displayinstances_instanceID),
					instanceData.get(position).getInstanceId()));
		}
		else {
			textViewHeadline.setText(String.format(
					context.getString(R.string.ec2displayinstances_tag),
					instanceData.get(position).getTag()));
		}
		
		details += String.format(
				context.getString(R.string.ec2displayinstances_type),
				instanceData.get(position).getInstanceType()) + ", ";
		 
		//get platform
		details += String.format(
				context.getString(R.string.ec2displayinstances_os),
				(instanceData.get(position).getPlatform() == null?"Linux": instanceData.
				get(position).getPlatform())) + ", ";
		
		//don't bother getting day launched if the instance is stopped
		if (listType == InstanceStateConstants.RUNNING) {
			//get period running in hours.
			float timeRunning = ((new Date().getTime() - instanceData.get(position).getLaunchTime()) / 
					(1000 * 60 * 60)); //convert from milliseconds to hours
			
			//if been running greater than 24 hours, convert to days
			if (timeRunning > 24) {
				timeRunning /= 24;
				
				details += String.format(
						context.getString(R.string.ec2displayinstances_rundays),
						timeRunning
						) ;
			}
			else {
				details += String.format(
						context.getString(R.string.ec2displayinstances_runhrs),
						timeRunning
						);
			}
		}
		textViewDetails.setText(Html.fromHtml(details));
		return instanceDataRow;
	}
	
}
