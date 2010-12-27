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
 * Authored by siddhu on 26 Dec 2010
 */
package org.elasticdroid;

import static org.elasticdroid.utils.ResultConstants.RESULT_ERROR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.SecurityGroupsModel;
import org.elasticdroid.model.ds.SerializableSecurityGroup;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
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
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * @author Rodolfo Cartas
 *
 * 26 Dec 2010
 */
public class SecurityGroupsView extends GenericListActivity implements OnCancelListener {

	/** The selected region */
	private String selectedRegion;
    /** The connection data */
    private HashMap<String,String> connectionData;
    /**Is progress bar displayed */
    private boolean progressDialogDisplayed;
	/**
	 * set to show if alert dialog displayed. Used to decide whether to restore
	 * progress dialog when screen rotated.
	 */
	private boolean alertDialogDisplayed;
	/** message displayed in {@link #alertDialogBox alertDialogBox}. */
	private String alertDialogMessage;
	/** Dialog box for credential verification errors */
	private AlertDialog alertDialogBox;
	
	/**
	 * boolean to indicate if an error that occurred is sufficiently serious to
	 * have the activity killed.
	 */
	private boolean killActivityOnError;
	
	/** The model object */
	SecurityGroupsModel securityGroupsModel;
	/** The result returned by the SecurityGroupsModel*/
	ArrayList<SerializableSecurityGroup> securityGroups;
	
	/**
	 * Tag for log messages
	 */
	private static final String TAG = "org.elasticdroid.SecurityGroupsView";
    
	
	/**
	 * This method is called when the activity is (re)started, and receives
	 * an {@link org.elasticdroid.utils.AWSConstants.InstanceConstants} enumerator
	 * as an intent, which tells it what sort of list to display. 
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
		selectedRegion = intent.getStringExtra("selectedRegion");
		
    	try {
    		this.connectionData = (HashMap<String, String>)intent.getSerializableExtra(
    				"org.elasticdroid.EC2DashboardView.connectionData");
    	}
    	//the possible exceptions are NullPointerException: the Hashmap was not found, or
    	//ClassCastException: the argument passed is not Hashmap<String, String>. In either case,
    	//just print out the error and exit. This is very inelegant, but this is a programmer's bug
    	catch(Exception exception) {
    		Log.e(TAG, exception.getMessage());
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
							SecurityGroupsView.this.finish();
							Intent loginIntent = new Intent();
							loginIntent.setClassName("org.elasticdroid",
									"org.elasticdroid.LoginView");
							startActivity(loginIntent);
						}
					}
				});
		
		setContentView(R.layout.securitygroups);
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li>securityGroups: The list of security groups</li>
	 * <li>securityGroupsModel: The retained config object containing the model object.</li>
	 * </ul>
	 * 
	 * Apart from the usual progress dialog and alert dialog
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		// restore dialog data
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = "
				+ alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
		
		securityGroups = (ArrayList<SerializableSecurityGroup>)stateToRestore.getSerializable("securityGroups");
		
		//was a progress dialog being displayed.
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		Log.v(TAG + ".onRestoreInstanceState", "progbar:" + progressDialogDisplayed);
		
		/*get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		
		if (retained instanceof SecurityGroupsModel) {
			securityGroupsModel = (SecurityGroupsModel) retained; //force typecast
			securityGroupsModel.setActivity(this);
		}
		else {
			securityGroupsModel = null; //redundant assignment
			Log.v(TAG,"No model object, or model finished before activity was recreated.");
			
			//now if there is no model anymore, and progressDialogDisplayed is set to true,
			//reset it to false, because the model finished executing before the restart
			if (progressDialogDisplayed) {
				progressDialogDisplayed = false;
			}
		}
		
		//if we have elastic IP data, reload the list (even if the model is running)
		//it's nice not to see an empty line.
		if (securityGroupsModel != null) {
			setListAdapter(new SecurityGroupsAdapter(this, R.layout.securitygrouprow, securityGroups));
		}
	}
	
	/**
	 * Last method executed when view restored. this method starts the model if both these 
	 * conditions are met:
	 * 
	 * <ul>
	 * <li>There is no currently running model.</li>
	 * <li>There is no elastic IP data already computed.</li>
	 * </ul>
	 */
	public void onResume() {
		super.onResume();
		
		//if there was a dialog box, display it
		//if failed, then display dialog box.
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}
		//call model again if the model is not already running, and we dont have data
		else if ((securityGroupsModel == null) && (securityGroups == null)) {
			executeModel();
		}
	}
	
	/**
	 * Save state of the activity on destroy/stop.
	 * Saves:
	 * <ul>
	 * <li> securityGroups: The security groups data.</li>
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
		
		//don't bother saving it if there's no data.
		if (securityGroups != null) {
			saveState.putSerializable("securityGroups", securityGroups);
		}
		
		//save if progress dialog is being displayed.
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.SecurityGroupsModel Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.v(TAG, "Object about to destroyed...");
		
		if (securityGroupsModel != null) {
			securityGroupsModel.setActivityNull();
			return securityGroupsModel;
		}
		
		return null;
	}
	
	/**
	 * Private method to execute the model.
	 */
	private void executeModel() {
		securityGroupsModel = new SecurityGroupsModel(this, connectionData);
		securityGroupsModel.execute(); //execute the Elastic IP model without any filters.
	}
	
	/** 
	 * Process the result of the activity
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		securityGroupsModel = null; //set the model to null
		
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
			progressDialogDisplayed = false;
		}
		
		if (result instanceof ArrayList<?>) {
			//success!!
			List<SecurityGroup> nonSerializableSecurityGroups = (ArrayList<SecurityGroup>)result;
			
			//add the security groups to a list of serializable security groups
			//NOTE: Doing this here in the view to avoid conflicts with SshConnectorModel
			//which uses SecurityGroupsModel too!
			securityGroups = new ArrayList<SerializableSecurityGroup>();
			for(SecurityGroup nonSerializableGroup: nonSerializableSecurityGroups) {
				securityGroups.add(new SerializableSecurityGroup(
						nonSerializableGroup.getOwnerId(),
						nonSerializableGroup.getGroupName(), 
						nonSerializableGroup.getDescription()));
			}
			
			//set the list adapter to show the data.
			setListAdapter(new SecurityGroupsAdapter(
					this, 
					R.layout.securitygrouprow, 
					securityGroups));
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
		//if result = null, the model was cancelled. Issue a wee toast.
		else if (result == null) {
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
	 * Function that handles the display of a progress dialog. Overriden from
	 * Activity and not GenericActivity
	 * 
	 * You need this method in your class if you want to have a prog bar displayed by the model.
	 * 
	 * @param id
	 *            Dialog ID - Special treatment for Constants.PROGRESS_DIALOG
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(this.getString(R.string.loginview_wait_dlg));
			dialog.setCancelable(true);
			
			dialog.setOnCancelListener(this);
	
			progressDialogDisplayed = true;
			Log.v(TAG, "progress dialog displayed="
					+ progressDialogDisplayed);
	
			return dialog;
		}
		// if some other sort of dialog...
		return super.onCreateDialog(id);
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
	 * Handle cancel of progress dialog
	 * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.
	 * DialogInterface)
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		//this cannot be called UNLESS the user has the model running.
		//i.e. the prog bar is visible
		securityGroupsModel.cancel(true);
	}
	
	/**
	 * Overridden method to display the menu on press of the menu key
	 * 
	 * Inflates and shows menu for displayed instances view.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.securitygroups_menu, menu);
	
		return true;
	}
	
	/**
	 * Overriden method to handle selection of menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem selectedItem) {
		switch(selectedItem.getItemId()) {
		//display about dialog
		case R.id.securitygroups_menuitem_about:
			Intent aboutIntent = new Intent(this, AboutView.class);
			startActivity(aboutIntent);
			return true;
		
		//refresh data
		case R.id.securitygroups_menuitem_refresh:
			executeModel();
			return true;
		
		//unrecognised
		default:
			return super.onOptionsItemSelected(selectedItem);
		}
	}
	
}

/**
 * Adapter to display the security groups in a list view. 
 * @author Rodolfo Cartas
 *
 * 26 Dec 2010
 */
class SecurityGroupsAdapter extends ArrayAdapter<SerializableSecurityGroup>{
	/** Instance list */
	private List<SerializableSecurityGroup> securityGroups;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	private Context context;
	
	/**
	 * Adapter constructor
	 * @param context The context to display this in
	 * @param textViewResourceId 
	 * @param instanceData
	 * @param listType
	 */
	public SecurityGroupsAdapter(Context context, int textViewResourceId, 
			ArrayList<SerializableSecurityGroup> securityGroups) {
		super(context, textViewResourceId, securityGroups);
		
		this.context = context;
		this.securityGroups = securityGroups;
	}
	
	/**
	 * 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View securityGroupRow = convertView;
		
		if (securityGroupRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
			(Context.LAYOUT_INFLATER_SERVICE);
		
			securityGroupRow = inflater.inflate(R.layout.securitygrouprow, parent, false);
		}
	
		//get text view widgets
		TextView textViewSecurityGroup = (TextView)securityGroupRow.findViewById(R.id.securityGroupName);
		TextView textViewDescription = (TextView)securityGroupRow.findViewById(R.id.securityGroupDescription);
		
		textViewSecurityGroup.setText(securityGroups.get(position).getGroupName());
		
		if(securityGroups.get(position).getDescription()!=null && 
				!securityGroups.get(position).getDescription().trim().equals("")) {
			textViewDescription.setText(securityGroups.get(position).getDescription());
		} else {
			textViewDescription.setText(context.getString(R.string.securityGroups_no_description));
		}
		
		
		return securityGroupRow; //return the populated row to display
	}

}
