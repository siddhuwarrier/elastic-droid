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

import org.elasticdroid.model.ElasticIPsModel;
import org.elasticdroid.model.ds.SerializableAddress;
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

/**
 * @author siddhu
 *
 * 26 Dec 2010
 */
public class ElasticIPsView extends GenericListActivity implements OnCancelListener {

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
	ElasticIPsModel elasticIpsModel;
	/** The result returned by the ElasticIPsModel*/
	ArrayList<SerializableAddress> elasticIps;
	
	/**
	 * Tag for log messages
	 */
	private static final String TAG = "org.elasticdroid.ElasticIPsView";
    
	
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
							ElasticIPsView.this.finish();
							Intent loginIntent = new Intent();
							loginIntent.setClassName("org.elasticdroid",
									"org.elasticdroid.LoginView");
							startActivity(loginIntent);
						}
					}
				});
		
		setContentView(R.layout.elasticips);
	}
	
	/**
	 * Restore instance state when the activity is reconstructed after a destroy
	 * 
	 * This method restores:
	 * <ul>
	 * <li>elasticIps: The list of instances</li>
	 * <li>elasticIpsModel: The retained config object containing the model object.</li>
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
		
		elasticIps = (ArrayList<SerializableAddress>)stateToRestore.getSerializable("elasticIps");
		
		//was a progress dialog being displayed.
		progressDialogDisplayed = stateToRestore.getBoolean("progressDialogDisplayed");
		Log.v(TAG + ".onRestoreInstanceState", "progbar:" + progressDialogDisplayed);
		
		/*get the model data back, so that you can inform the model that the activity
		 * has come back up. */
		Object retained = getLastNonConfigurationInstance();
		
		if (retained instanceof ElasticIPsModel) {
			elasticIpsModel = (ElasticIPsModel) retained; //force typecast
			elasticIpsModel.setActivity(this);
		}
		else {
			elasticIpsModel = null; //redundant assignment
			Log.v(TAG,"No model object, or model finished before activity was recreated.");
			
			//now if there is no model anymore, and progressDialogDisplayed is set to true,
			//reset it to false, because the model finished executing before the restart
			if (progressDialogDisplayed) {
				progressDialogDisplayed = false;
			}
		}
		
		//if we have elastic IP data, reload the list (even if the model is running)
		//it's nice not to see an empty line.
		if (elasticIps != null) {
			setListAdapter(new ElasticIPsAdapter(this, R.layout.elasticipsrow, elasticIps));
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
		else if ((elasticIpsModel == null) && (elasticIps == null)) {
			executeModel();
		}
	}
	
	/**
	 * Save state of the activity on destroy/stop.
	 * Saves:
	 * <ul>
	 * <li> elasticIps: The Elastic IP data.</li>
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
		if (elasticIps != null) {
			saveState.putSerializable("elasticIps", elasticIps);
		}
		
		//save if progress dialog is being displayed.
		saveState.putBoolean("progressDialogDisplayed", progressDialogDisplayed);
	}
	
	/**
	 * Save reference to {@link org.elasticdroid.model.ElasticIPsModel Async
	 * Task when object is destroyed (for instance when screen rotated).
	 * 
	 * This has to be done as the Async Task is running in the background.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.v(TAG, "Object about to destroyed...");
		
		if (elasticIpsModel != null) {
			elasticIpsModel.setActivityNull();
			return elasticIpsModel;
		}
		
		return null;
	}
	
	/**
	 * Private method to execute the model.
	 */
	private void executeModel() {
		elasticIpsModel = new ElasticIPsModel(this, connectionData);
		elasticIpsModel.execute(); //execute the Elastic IP model without any filters.
	}
	
	/** 
	 * Process the result of the activity
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processModelResults(Object result) {
		elasticIpsModel = null; //set the model to null
		
		// dismiss the progress dialog if displayed. Check redundant
		if (progressDialogDisplayed) {
			removeDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
			progressDialogDisplayed = false;
		}
		
		if (result instanceof ArrayList<?>) {
			//success!!
			elasticIps = (ArrayList<SerializableAddress>)result;
			//set the list adapter to show the data.
			setListAdapter(new ElasticIPsAdapter(
					this, 
					R.layout.elasticipsrow, 
					elasticIps));
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
		elasticIpsModel.cancel(true);
	}
	
	/**
	 * Overridden method to display the menu on press of the menu key
	 * 
	 * Inflates and shows menu for displayed instances view.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.elasticips_menu, menu);
	
		return true;
	}
	
	/**
	 * Overriden method to handle selection of menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem selectedItem) {
		switch(selectedItem.getItemId()) {
		//display about dialog
		case R.id.elasticips_menuitem_about:
			Intent aboutIntent = new Intent(this, AboutView.class);
			startActivity(aboutIntent);
			return true;
		
		//refresh data
		case R.id.elasticips_menuitem_refresh:
			executeModel();
			return true;
		
		//unrecognised
		default:
			return super.onOptionsItemSelected(selectedItem);
		}
	}
	
}

/**
 * Adapter to display the instances in a list view. 
 * @author Siddhu Warrier
 *
 * 6 Dec 2010
 */
class ElasticIPsAdapter extends ArrayAdapter<SerializableAddress>{
	/** Instance list */
	private List<SerializableAddress> elasticIps;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	private Context context;
	
	/**
	 * Adapter constructor
	 * @param context The context to display this in
	 * @param textViewResourceId 
	 * @param instanceData
	 * @param listType
	 */
	public ElasticIPsAdapter(Context context, int textViewResourceId, 
			ArrayList<SerializableAddress> elasticIps) {
		super(context, textViewResourceId, elasticIps);
		
		this.context = context;
		this.elasticIps = elasticIps;
	}
	
	/**
	 * 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View elasticIpRow = convertView;
		
		if (elasticIpRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
			(Context.LAYOUT_INFLATER_SERVICE);
		
			elasticIpRow = inflater.inflate(R.layout.elasticipsrow, parent, false);
		}
	
		//get text view widgets
		TextView textViewHeadline = (TextView)elasticIpRow.findViewById(R.id.ipHeadline);
		TextView textViewDetails = (TextView)elasticIpRow.findViewById(R.id.ipDetails);
		ImageView imageViewStatusIcon = (ImageView)elasticIpRow.findViewById(R.id.ipStatusIcon);
		
		textViewHeadline.setText(elasticIps.get(position).getPublicIp());
		
		if (elasticIps.get(position).getInstanceId() != null) {
			imageViewStatusIcon.setImageResource(R.drawable.green_light);
			textViewDetails.setText(Html.fromHtml(
					String.format(
					context.getString(R.string.elasticips_instanceID), elasticIps.get(position).
					getInstanceId())));
		}
		else {
			imageViewStatusIcon.setImageResource(R.drawable.red_light);
			textViewDetails.setText(context.getString(R.string.elasticips_unassociated));
		}
		
		return elasticIpRow; //return the populated row to display
	}

}