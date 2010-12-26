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
import org.elasticdroid.tpl.GenericListActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.amazonaws.services.ec2.model.Address;

/**
 * @author siddhu
 *
 * 26 Dec 2010
 */
public class ElasticIPsView extends GenericListActivity {

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
	List<Address> elasticIps;
	
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
	 * Last method executed when view restored.
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
	 * Private method to execute the model.
	 */
	private void executeModel() {
		
	}
	/** 
	 * Process the result of the activity
	 */
	@Override
	public void processModelResults(Object result) {
		// TODO Auto-generated method stub
		
	}
}

/**
 * Adapter to display the instances in a list view. 
 * @author Siddhu Warrier
 *
 * 6 Dec 2010
 */
class ElasticIPsAdapter extends ArrayAdapter<Address>{
	/** Instance list */
	private ArrayList<Address> elasticIps;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	private Context context;
	/** List type */
	private int listType;
	
	/**
	 * Adapter constructor
	 * @param context The context to display this in
	 * @param textViewResourceId 
	 * @param instanceData
	 * @param listType
	 */
	public ElasticIPsAdapter(Context context, int textViewResourceId, 
			ArrayList<Address> elasticIps) {
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
		
		textViewHeadline.setText(elasticIps.get(position).getPublicIp());
		
		if (elasticIps.get(position).getInstanceId() != null) {
			textViewDetails.setText(String.format(
					context.getString(R.string.elasticips_instanceID), elasticIps.get(position).
					getInstanceId()));
		}
		else {
			textViewDetails.setText(context.getString(R.string.elasticips_unassociated));
		}
		
		return elasticIpRow; //return the populated row to display
	}

}