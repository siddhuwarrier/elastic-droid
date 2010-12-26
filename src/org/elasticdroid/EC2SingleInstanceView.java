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
 * Authored by siddhu on 11 Dec 2010
 */
package org.elasticdroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.ElasticIPsModel;
import org.elasticdroid.model.ds.SerializableInstance;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.DialogConstants;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.Filter;

/**
 * Activity to display details on a single instance. 
 * Called by EC2DisplayInstancesView. Will handle all of its errors internally, and not return a 
 * result.
 * 
 * @author siddhu
 *
 * 11 Dec 2010
 */
public class EC2SingleInstanceView extends GenericListActivity {
	
	/**
	 * The instance that is being displayed.
	 */
	private SerializableInstance instance;
	/**
	 * The AWS connection data
	 */
	private HashMap<String, String> connectionData;
	/**
	 * The selected region
	 */
	private String selectedRegion;
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
    /**Is progress bar displayed */
    private boolean progressDialogDisplayed;
    /**
     * Elastic IP Model object
     */
    private ElasticIPsModel elasticIpsModel;
    /**
     * Is an Elastic IP assigned to this instance.
     * Model answers this question.
     * Note: Only Elastic IP per instance (i.e. one public n/w i/f per machine).
     * 
     * Using Boolean instead of boolean because we also use this to find if the model has been 
     * executed.
     */
	private Boolean isElasticIpAssigned;
	
	/**
	 * Called when activity is created.
	 * @param savedInstanceState if any
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); //call superclass onCreate
		
		Intent intent = this.getIntent();
		
		//get data from intent
		selectedRegion = intent.getStringExtra("selectedRegion");
		
    	try {
    		this.connectionData = (HashMap<String, String>)intent.getSerializableExtra(
    				"org.elasticdroid.EC2DashboardView.connectionData");
    	}
    	catch(Exception exception) {
        	//the possible exceptions are NullPointerException: the Hashmap was not found, or
        	//ClassCastException: the argument passed is not Hashmap<String, String>. In either case,
        	//just print out the error and exit. This is very inelegant, but this is a programmer's bug
    		Log.e(this.getClass().getName(), exception.getMessage());
    		finish(); //this will cause it to return to {@link EC2DisplayInstancesView}.
    	}
    	
    	try {
    		this.instance = (SerializableInstance)intent.getSerializableExtra(
    				"org.elasticdroid.model.SerializableInstance");
    	}
    	catch(Exception exception) {
    		Log.e(this.getClass().getName(), exception.getMessage());
    		finish(); //this will cause it to return to {@link EC2DisplayInstancesView}.
    	}    
    	
		// create and initialise the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); // create alert
																	// box to
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
							EC2SingleInstanceView.this.finish();
						}
					}
				});
		
		setContentView(R.layout.ec2singleinstance); //tell the activity to set the xml file
		
		this.setTitle(connectionData.get("username")+ " (" + selectedRegion +")"); //set title
		
		//set the rest of the UI elements
		//if there is no tag called "name" (case insensitive), set instance ID
		if (instance.getTag() == null) {
			((TextView)findViewById(R.id.ec2SingleInstanceName)).setText(Html.fromHtml(String.format(
					this.getString(R.string.ec2singleinstance_tag), instance.getInstanceId())));
		}
		else {
			((TextView)findViewById(R.id.ec2SingleInstanceName)).setText(Html.fromHtml(String.format(
					this.getString(R.string.ec2singleinstance_tag), instance.getTag())));			
		}
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li>isElasticIpAssigned: Has the instance been assigned an Elastic IP?</li>
	 * </ul>
	 */
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		//restore alertDialogDisplayed boolean
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = "
				+ alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		//was a progress dialog being displayed? Restore the answer to this question.
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		Log.v(this.getClass().getName() + ".onRestoreInstanceState", "progressDialogDisplayed:" + 
				progressDialogDisplayed);
		
		//check if the key exists before assigning it.
		//This is because getBoolean returns false if key doesn't exist.
		//See onSaveInstanceState(). It shows that isElasticIpAssigned is not always saved.
		if (stateToRestore.get("isElasticIpAssigned") != null) {
			isElasticIpAssigned = stateToRestore.getBoolean("isElasticIpAssigned");
		}
		/*get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		//if there was a model executing when the object was destroyed, retained will be an 
		//instance of ElasticIpsModel
		if (retained instanceof ElasticIPsModel) {
			Log.i(this.getClass().getName() + ".onRestoreInstanceState()","Reclaiming previous " +
				"background task");
			elasticIpsModel = (ElasticIPsModel)retained;
			elasticIpsModel.setActivity(this);//tell the model of the new activity created
		}
		else {
			elasticIpsModel = null;
			
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
	 * Executed when activity is resumed. Calls ElasticIpModel to determine if public IP
	 * is elastic.
	 */
	@Override
	public void onResume() {
		super.onResume(); //call superclass onResume()
		
		Log.v(this.getClass().getName() + ".onResume()", "onResume");
		
		//don't execute ElasticIpModel if instance is stopped or summat
		if (instance.getStateCode() != InstanceStateConstants.RUNNING) {
			isElasticIpAssigned = false;
		}
		
		//if there was a dialog box, display it
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		} else if (isElasticIpAssigned == null) {
				executeModel();
		} else {
			//populate the list
			setListAdapter(new EC2SingleInstanceAdapter(this, R.layout.ec2singleinstance, 
					instance, isElasticIpAssigned));
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
		
		//save if progress dialog is being displayed.
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
		
		//save whether there is an elastic IP assigned to this instance IF it has been initialised
		if (isElasticIpAssigned != null) {
			saveState.putBoolean("isElasticIpAssigned", isElasticIpAssigned);
		}
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.ElasticIPsModel} Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (elasticIpsModel != null) {
			elasticIpsModel.setActivity(null);
			return elasticIpsModel;
		}
		
		return null;
	}
	
	/**
	 * Executes the model which will return the Elastic IP(s) assigned to this instance.
	 */
	private void executeModel() {
		Log.v(this.getClass().getName() + ".executeModel()", "Going to execute model!");
		elasticIpsModel = new ElasticIPsModel(this, connectionData);
		
		//filter results that are not relevant to this instance
		//we have to pass an array of filters to the doInBackground method, and we need to construct
		//an array list from the String array constructed to hold the instance.getInstanceId() string
		//so much function chaining, i feel ill. Or maybe I'm just stupid!
		elasticIpsModel.execute(new Filter[]{
				new Filter("instance-id", 
						new ArrayList<String>(Arrays.asList(
								new String[]{instance.getInstanceId()})
								)
						  )
				});
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
	 * Process results from model
	 * @param Object, which can be a List<Address> or exceptions
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		
		Log.v(this.getClass().getName() + ".processModelResults()", "Processing model results...");
		
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			progressDialogDisplayed = false;
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		
		// if the model returned a result; i.e. success.
		if (result instanceof List<?>) {
			
			//if there is data, set boolean to true
			if (((List<Address>) result).size() != 0) {
				for (Address address : (List<Address>) result) {
					Log.v(this.getClass().getName() + ".processModelResults()", address.getPublicIp());
					Log.v(this.getClass().getName() + ".processModelResults()", address.getInstanceId());
				}
				isElasticIpAssigned = true;
			}
			else {
				isElasticIpAssigned = false; //this has to be done manually because we are using a 
				//Boolean and not a boolean. When null, we execute the model.
			}
			
			//populate the list
			setListAdapter(new EC2SingleInstanceAdapter(this, R.layout.ec2singleinstance, 
					instance, isElasticIpAssigned));
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
	 * Overridden method to display the menu on press of the menu key
	 * 
	 * Inflates and shows menu for displayed instances view.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.singleinstance_menu, menu);
	
		return true;
	}
	
	/**
	 * Overriden. Prepares menu
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (instance.getStateCode() == InstanceStateConstants.STOPPED) {
			//if the instance is stopped, then change the control instance menu item's text and img
			//to show "start instance" and a "play" button respectively.
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setTitle
				(R.string.ec2singleinstance_menu_startinstance);
			menu.findItem(R.id.singleinstance_menuitem_controlinstance).setIcon(R.drawable.
					ic_menu_play_clip);
		}
		
		if (instance.getStateCode() != InstanceStateConstants.RUNNING) {
			//if the instance is not running, disable monitoring and SSH
			Log.v(this.getClass().getName() + ".onPrepareOptionsMenu()", "Removing monitoring and" +
					"SSH connect options.");
			menu.removeItem(R.id.singleinstance_menuitem_monitor);
			menu.removeItem(R.id.singleinstance_menuitem_ssh);
		}
		
		return true;
	}
	
	/**
	 * Overriden method to handle selection of menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem selectedItem) {
		switch (selectedItem.getItemId()) {
		
		case R.id.singleinstance_menuitem_about:
			Intent aboutIntent = new Intent(this, AboutView.class);
			startActivity(aboutIntent);
			return true;
		case R.id.singleinstance_menuitem_ssh:
			Log.v(this.getClass().getName() + ".onOptionsItemSelected()", "User wishes to SSH!");
			
			//call the SSH connector view using the intent
			Intent sshConnectorIntent = new Intent();
			sshConnectorIntent.setClassName("org.elasticdroid","org.elasticdroid.SshConnectorView");
			
			//not using IP address as theoretically DHCP lease can expire when connecting
			//if Elastic IP is not used.
			sshConnectorIntent.putExtra("hostname", instance.getPublicDnsName());
			List<String> secGroupNames = instance.getSecurityGroupNames();
			//breaking 100 character per line unwritten rule here as the code looks better this way
			sshConnectorIntent.putExtra("securityGroups", 
					secGroupNames.toArray(new String[secGroupNames.size()]));
			sshConnectorIntent.putExtra("selectedRegion", selectedRegion);
			sshConnectorIntent.putExtra(
					"org.elasticdroid.EC2DashboardView.connectionData",
					connectionData); // aws connection info
			//start the activity
			startActivity(sshConnectorIntent);
			
			/*Intent connectBotIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
			startActivity(connectBotIntent);*/
			
			
			return true;
		
		default:
			return super.onOptionsItemSelected(selectedItem);
		}
	}
	
	/**
	 * Handle back button.
	 * If back button is pressed, UI should die.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();  
		}
		
		return super.onKeyDown(keyCode, event);
	}
}

/**
 * ListView Adapter that extends ArrayAdapter<RowData>
 * 
 * Displays the metrics of each instance.
 * 
 * @author siddhu
 *
 * 12 Dec 2010
 */
class EC2SingleInstanceAdapter extends ArrayAdapter<RowData> {
	/** Instance datum */
	private SerializableInstance instance;
	/** Does the above instance have an Elastic IP assigned? */
	private boolean isElasticIpAssigned;
	/** Context */
	private Context context;
	
	/**
	 * Constructor: Calls ArrayAdapter constructor with a RowData enum that contains all of the 
	 * fields we want to display in the ListView.
	 * 
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 */
	public EC2SingleInstanceAdapter(Context context, int textViewResourceId,
			SerializableInstance instance, boolean isElasticIpAssigned) {
		super(context, textViewResourceId, RowData.values());
		
		//save context and instance data
		this.context = context;
		this.instance = instance;
		this.isElasticIpAssigned = isElasticIpAssigned;
	}
	
	/**
	 * Overriden method called when ListView is initialised with data.
	 * @param position The position in {@link #instanceData}.
	 * @param convertView The view to set.
	 * @param parent
	 * @return Configured row to add to ListView
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View instanceMetricRow = convertView;
		
		if (instanceMetricRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
			(Context.LAYOUT_INFLATER_SERVICE);
		
			instanceMetricRow = inflater.inflate(R.layout.ec2singleinstancerow, parent, 
					false);
		}
		
		Log.v(context.getClass().getName(), "Instance state code:" + instance.getStateCode());
		
		TextView instanceMetricTextView = (TextView)instanceMetricRow.findViewById(R.id.
				instanceMetric);
		TextView instanceDataTextView = (TextView)instanceMetricRow.findViewById(R.id.
				instanceData);
		ImageView instanceStatusIcon = ((ImageView) instanceMetricRow.findViewById(R.id.
				instanceStatusIcon));
		RowData selectedRowDatum = RowData.values()[position]; 
		
		switch(selectedRowDatum) {
		
		case STATE_NAME:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_state));
			instanceDataTextView.setText(Html.fromHtml(instance.getStateName()));
			
			if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
				((ImageView) instanceMetricRow.findViewById(R.id.instanceStatusIcon)).
					setImageResource(R.drawable.green_light);
			}
			else if (instance.getStateCode() == InstanceStateConstants.STOPPED) {
				instanceStatusIcon.setImageResource(R.drawable.red_light);				
			}
			break;
			
		case TYPE:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_type));
			instanceDataTextView.setText(Html.fromHtml(instance.getInstanceType()));
			instanceStatusIcon.setImageResource(R.drawable.instance);
			break;
		
		case OS:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_os));
			if (instance.getPlatform() != null) {
				instanceDataTextView.setText(Html.fromHtml(instance.getPlatform()));
				instanceStatusIcon.setImageResource(R.drawable.droid_guy);
			}
			else {
				instanceDataTextView.setText(Html.fromHtml("Linux"));
				instanceStatusIcon.setImageResource(R.drawable.linux_penguin);
			}
			break;
			
		case LAUNCHED:
			instanceMetricTextView.setText(context.getString(R.string.
					ec2singleinstance_launchtime));
			instanceStatusIcon.setImageResource(R.drawable.ic_dialog_time);
			
			if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
				//get period running in hours.
				float timeRunning = ((new Date().getTime() - instance.getLaunchTime()) / 
						(1000 * 60 * 60)); //convert from milliseconds to hours
				
				String launchDetails;
				//if been running greater than 24 hours, convert to days
				if (timeRunning > 24) {
					timeRunning /= 24;
					
					launchDetails = String.format(
							context.getString(R.string.ec2singleinstance_launchdetails_days),
							timeRunning
							) ;
				}
				else {
					launchDetails = String.format(
							context.getString(R.string.ec2singleinstance_launchdetails_hrs),
							timeRunning
							);
				}
				
				instanceDataTextView.setText(Html.fromHtml(launchDetails));
			}
			else {
				instanceDataTextView.setText("N/A");
			}
			break;
			
		case KEYNAME:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_keypair));
			instanceDataTextView.setText(Html.fromHtml(instance.getKeyName()));
			instanceStatusIcon.setImageResource(R.drawable.keypair);
			break;
			
		case SECURITY_GROUP:
			//concatenate all security groups in list into one long String separated by spaces
			String securityGroupString = "";
			List<String> securityGroupNames = instance.getSecurityGroupNames();
			//O(n) solution
			for (String securityGroupName : securityGroupNames) {
				securityGroupString += securityGroupName + ", ";
			}
			//remove last comma
			securityGroupString = securityGroupString.substring(0, securityGroupString.length() - 2);
			
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_secgroup));
			instanceDataTextView.setText(Html.fromHtml(securityGroupString));
			instanceStatusIcon.setImageResource(R.drawable.ic_lock_lock);
			break;
			
		case AMI_ID:
			instanceMetricTextView.setText(context.getString(R.string.ec2singleinstance_ami));
			instanceDataTextView.setText(Html.fromHtml(instance.getImageId()));
			instanceStatusIcon.setImageResource(R.drawable.ami);
			break;
			
		case IP_ADDRESS:
			//set IP address only if instance is running
			if (instance.getStateCode() == InstanceStateConstants.RUNNING) {
				if (isElasticIpAssigned) {
					instanceMetricTextView.setText(context.getString(R.string.
							ec2singleinstance_elastic_ip));
					instanceDataTextView.setText(Html.fromHtml(instance.getPublicIpAddress()));
				}
				else {
					instanceMetricTextView.setText(context.getString(R.string.
							ec2singleinstance_public_ip));
					instanceDataTextView.setText(Html.fromHtml(instance.getPublicIpAddress()));				
				}
			}
			else {
				instanceMetricTextView.setText(context.getString(R.string.
						ec2singleinstance_public_ip));
				instanceDataTextView.setText("N/A");
			}
			
			instanceStatusIcon.setImageResource(R.drawable.ic_menu_mylocation);
			
			break;
		}
		
		return instanceMetricRow;
	}
	
	/**
	 * Function to disable all items in the ListView, as we do not want users clicking on
	 * them.
	 */
	@Override
    public boolean areAllItemsEnabled() 
    { 
            return false; 
    } 
    
    /**
     * Another function that does the same as hte function above
     */
	@Override
    public boolean isEnabled(int position) 
    { 
            return false; 
    } 
}

/**
 * An enumeration that gets the text to be used for each data
 * type displayed by the EC2SingleInstanceAdapter.
 * @author siddhu
 *
 * 12 Dec 2010
 */
enum RowData {
	STATE_NAME,
	TYPE,
	OS,
	LAUNCHED,
	KEYNAME,
	SECURITY_GROUP,
	AMI_ID,
	IP_ADDRESS;
}
