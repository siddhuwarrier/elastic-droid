package org.elasticdroid;

import static org.elasticdroid.utils.ResultConstants.RESULT_ERROR;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.model.EC2InstancesModel;
import org.elasticdroid.model.ds.SerializableInstance;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

public class InstanceGroupEditView extends GenericListActivity {

	/** The selected region */
	private String selectedRegion;
    /** The connection data */
    private HashMap<String,String> connectionData;
    /**The model object */
    private EC2InstancesModel ec2InstancesModel;
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
	
    /**The model result: an ArrayList of corresponding instances
     * Uses Serializable Instance and not AWS Instance. {@link SerializableInstance} 
     * */
    private ArrayList<SerializableInstance> instanceData;
    
    /**
     * Logging tag
     */
    private static final String TAG = InstanceGroupEditView.class.getName();
	
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
		alertDialogBox.setCancelable(false);
		alertDialogBox.setButton(
				this.getString(R.string.loginview_alertdialogbox_button),
				new DialogInterface.OnClickListener() {
					// click listener on the alert box - unlock orientation when
					// clicked.
					// this is to prevent orientation changing when alert box
					// locked.
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						alertDialogDisplayed = false;
						alertDialogBox.dismiss(); // dismiss dialog.
						// if an error occurs that is serious enough return the
						// user to the login
						// screen. THis happens due to exceptions caused by
						// programming errors and
						// exceptions caused due to invalid credentials.
						if (killActivityOnError) {
							InstanceGroupEditView.this.finish();
							Intent loginIntent = new Intent();
							loginIntent.setClassName("org.elasticdroid",
									"org.elasticdroid.LoginView");
							startActivity(loginIntent);
						}
					}
				});
		
		//set the content view
		setContentView(R.layout.instancegroupedit);
		//set the title
		this.setTitle(connectionData.get("username") + " (" + selectedRegion +")");
		
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
				setListAdapter(new SelectableInstanceDisplayAdapter(
						this, 
						R.layout.selectableinstancerow, 
						instanceData)
				);
				
				getListView().setItemsCanFocus(false);
				getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
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
		ec2InstancesModel.execute();
	}

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
					Log.v(this.getClass().getName(), "populating the list");
					setListAdapter(new SelectableInstanceDisplayAdapter(
							this, 
							R.layout.selectableinstancerow, 
							instanceData)
					);
					
					getListView().setItemsCanFocus(false);
					getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
				}
				//if no data found, just show a String adapter
				else {
					Log.v(this.getClass().getName(), "no data found");
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
	 * Handle back button.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//return the failure to the mama class 
			Intent resultIntent = new Intent();
			resultIntent.setType(this.getClass().getName());
			setResult(RESULT_OK, resultIntent); //let the calling activity know that the user chose to 
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
		progressDialogDisplayed = false;
		ec2InstancesModel.cancel(true);
	}
	
	/**
	 * Handle the selection of a given instance, and pass the relevant SerializableInstance object
	 * on.
	 */
	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		Log.d(TAG, "Position clicked: " + position);
		
		/*getListView().setItemChecked(position, !getListView().isItemChecked(position));
		getListView().isItemChecked(position);*/
	}
	
	/**
	 * Overridden method to display the menu on press of the menu key
	 * 
	 * Inflates and shows menu for displayed instances view.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.instancegroup_menu, menu);
		return true;
	}
	
	/**
	 * Overriden method to handle selection of menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem selectedItem) {
		switch (selectedItem.getItemId()) {
		
		case R.id.instancegroup_menuitem_save:
			saveInstanceGroupDataToDb();
			return true;
		
		default:
			return super.onOptionsItemSelected(selectedItem);
		}
	}
	
	private List<String> instanceIds;
	/**
	 * Method to save instance group data to DB.
	 */
	private void saveInstanceGroupDataToDb() {
		instanceIds = new ArrayList<String>();
		
		ListView instancesListView = getListView();
		for (int listItemPos = 0; listItemPos < instancesListView.getCount(); listItemPos ++) {
			
			
			if (getListView().isItemChecked(listItemPos)) {
				Log.d(TAG, "Adding instnace : " + instanceData.get(listItemPos).getInstanceId() + " to " +
						"group");
				instanceIds.add(instanceData.get(listItemPos).getInstanceId());
			}
		}
		
		Log.d(TAG, "Adding " + instanceIds.size() + " instances...");
		
		if (instanceIds.size() == 0) {
			alertDialogMessage = getString(R.string.ec2instancegroupsview_select_instance);
			alertDialogDisplayed = true;
			killActivityOnError = false;
		}
		else {
			
			AlertDialog.Builder groupNameDialog = new AlertDialog.Builder(this);
			// Set an EditText view to get user input 
			LinearLayout groupNameDialogLayout = new LinearLayout(this);
			groupNameDialogLayout.setOrientation(LinearLayout.VERTICAL);
			final EditText groupNameInput = new EditText(this);
			groupNameInput.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			TextView groupNameTextView = new TextView(this);
			groupNameTextView.setText(getString(R.string.ec2instancegroupsview_input));
			groupNameTextView.setPadding(0, 0, 0, 10);
			groupNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22.0f);
			groupNameTextView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			
			groupNameDialogLayout.addView(groupNameTextView);
			groupNameDialogLayout.addView(groupNameInput);
			groupNameDialog.setView(groupNameDialogLayout);
			//set the listener up.
			groupNameDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String newGroupName = groupNameInput.getText().toString();
					  //write to DB
					try {
						new ElasticDroidDB(getApplicationContext()).writeInstanceGroupsToDb(connectionData.get("username"),
							selectedRegion, 
							newGroupName, 
							instanceIds);
					
						Toast.makeText(
								getApplicationContext(), 
								getApplicationContext().getString(R.string.ec2instancegroupsview_new_group_notification), 
								Toast.LENGTH_LONG)
								.show();
						
					} catch (SQLException e) {
						alertDialogMessage = "Group creation failed: " + e.getLocalizedMessage();
							alertDialogDisplayed = true;
							killActivityOnError = true;
					}
					
					//display alert dialog if requested
					if (alertDialogDisplayed) {
						alertDialogBox.setMessage(alertDialogMessage);
						alertDialogBox.show();
					}
				}
			});
			
			groupNameDialog.show();
			
		}
		
		//display alert dialog if requested
		if (alertDialogDisplayed) {
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();
		}

	}
}

/**
 * Adapter to display instances with checkboxes in a list view. 
 * @author Rodolfo Cartas
 *
 * 18 Jan 2010
 */
class SelectableInstanceDisplayAdapter extends ArrayAdapter<SerializableInstance> implements OnClickListener{

	/** Instance list */
	private ArrayList<SerializableInstance> instanceData;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	private Context context;
	/**Logging tag */
	private static String TAG = 
		"org.elasticdroid.InstanceGroupEditView$SelectableInstanceDisplayAdapter";
	/**
	 * @param context
	 * @param textViewResourceId
	 */
	public SelectableInstanceDisplayAdapter(Context context, int textViewResourceId, 
			ArrayList<SerializableInstance> instanceData) {
		super(context, textViewResourceId, instanceData);
		
		//save the context, data, and list type
		this.context = context;
		this.instanceData = instanceData;
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
		if (instanceDataRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
			(Context.LAYOUT_INFLATER_SERVICE);
		
			instanceDataRow = inflater.inflate(R.layout.selectableinstancerow, parent, false);
		}
		//set main text view
		CheckedTextView checkedTextView = (CheckedTextView)instanceDataRow.findViewById(R.id.
				CheckedTextView01);
		
		if (instanceData.get(position).getTag() != null) {
			checkedTextView.setText(instanceData.get(position).getTag());
		}
		else {
			checkedTextView.setText(instanceData.get(position).getInstanceId());
		}
		
		checkedTextView.setOnClickListener(this);
		
		return instanceDataRow;
	}

	@Override
	public void onClick(View checkedView) {
		CheckedTextView checkedTextView = (CheckedTextView) checkedView;
		Log.d(TAG, "Clicked: " + checkedTextView.getText().toString());
		checkedTextView.setChecked(!checkedTextView.isChecked());
		
	}
}
