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
 * Authored by Rodolfo Cartas on 18 Jan 2011
 */
package org.elasticdroid;

import static org.elasticdroid.utils.ResultConstants.RESULT_ERROR;

import java.util.HashMap;
import java.util.List;

import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.model.orm.InstanceGroup;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Class to show a list of instance groups to choose from.
 * 
 * @author Rodolfo Cartas
 * 
 *         18 Jan 2011
 */
public class EC2DisplayInstanceGroupsView extends ListActivity {

	/**
	 * AWS login details: username, access key, secret access key. Can be IAM
	 * username or AWS email address Not using ArrayList<String> cuz AsyncTask
	 * excepts String... as argument.
	 * */
	private HashMap<String, String> connectionData;

	/**
	 * the default region set by the user.
	 */
	private String selectedRegion;

	/** List to store the instance groups */
	private List<InstanceGroup> instanceGroups;

	/**
	 * Called when the activity is first created or recreated.
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get the connection data and the username
		Intent intent = this.getIntent();
		try {
			this.connectionData = (HashMap<String, String>) intent
					.getSerializableExtra("org.elasticdroid.EC2DashboardView.connectionData");
			this.selectedRegion = intent.getStringExtra("selectedRegion");
		}
		// the possible exceptions are NullPointerException: the Hashmap was not
		// found, or
		// ClassCastException: the argument passed is not Hashmap<String,
		// String>. In either case,
		// just print out the error and exit. This is very inelegant, but this
		// is a programmer's bug
		catch (Exception exception) {
			Log.e(this.getClass().getName(), exception.getMessage());
			finish(); // kill the application, and off to bed.
		}

		// GUI
		setContentView(R.layout.ec2instancegroups);
		this.setTitle(selectedRegion);

		// get the list of instance groups from the database
		Log.v(this.getClass().getName(), "Getting groups (username, region): ("
				+ connectionData.get("username") + "," + selectedRegion + ")");

		loadInstanceGroups();
	}

	/**
	 * Overriden listen method to capture clicks on List Item
	 */
	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		InstanceGroup selectedInstanceGroup = (InstanceGroup) list
				.getItemAtPosition(position);

		Log.v(this.getClass().getName(), "Item selected: "
				+ selectedInstanceGroup.getGroupName());

		Intent intent = new Intent();
		// if the user wants a new instance group.
		if (selectedInstanceGroup
				.getGroupName()
				.equals(this.getString(
						R.string.ec2instancegroupsview_new_instance_groups))) {
			// launch new instance group dialog
			Log.v(this.getClass().getName(),
					"Launching new instance group dialog");
			intent.setClassName("org.elasticdroid",
			"org.elasticdroid.InstanceGroupEditView");
			
			intent.putExtra("selectedRegion", selectedRegion); // selected region
			intent.putExtra(
					"org.elasticdroid.EC2DashboardView.connectionData",
					connectionData); // aws connection info
			
			//TODO move the following statement after the if-then-else
			startActivityForResult(intent, 0); //second arg ignored.

		} else {
			Log.v(this.getClass().getName(), "Display group instance");
			// show instances list
			// the intent should carry the connection data and the group id
		}
	}

	/**
	 * Handle back button.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// do not allow user to return to previous screen on pressing back
		// button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// return the failure to the mama class
			Intent resultIntent = new Intent();
			resultIntent.setType(this.getClass().getName());

			setResult(RESULT_CANCELED, resultIntent); // let the calling
														// activity know that
														// the user chose to
			// cancel
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Called when the default region setter returns.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(this.getClass().getName() + "onActivityResult()",
				"result intent from : " + data.resolveType(this));
		Log.v(this.getClass().getName() + "onActivityResult()", "test: "
				+ EC2DisplayInstancesView.class.toString());
		Log.v(this.getClass().getName(), "Subactivity returned with result: "
				+ resultCode);

		// check which class returned the result intent
		if (data.getType().equals(EC2DisplayInstancesView.class.getName())) {
			switch (resultCode) {
			case RESULT_ERROR:
				Log.e(this.getClass().getName() + "onActivityResult",
						data.getStringExtra("EXCEPTION_MSG"));
				finish(); // kill the app off.
				break;
			case RESULT_OK:
				Log.v(this.getClass().getName(),
						"InstanceGroupEditView returned successfully.");

				loadInstanceGroups();
			}

		}
	}

	private void loadInstanceGroups() {
		instanceGroups = new ElasticDroidDB(this).listInstanceGroups(
				connectionData.get("username"), selectedRegion);

		// Add New Instance Group to list of instance groups.
		instanceGroups
				.add(new InstanceGroup(
						-1l,
						this.getString(R.string.ec2instancegroupsview_new_instance_groups)));

		// add the usernames to the list adapter to display
		setListAdapter(new InstanceGroupAdapter(this,
				R.layout.ec2instancegroupsrow, instanceGroups));
	}
}

/**
 * Adapter to display the group instances in a list view.
 * 
 * @author Rodolfo Cartas
 * 
 */
class InstanceGroupAdapter extends ArrayAdapter<InstanceGroup> {

	/** Instance Groups list */
	private List<InstanceGroup> instanceGroupsData;
	/** Tag */
	private static final String TAG = InstanceGroupAdapter.class.getName();
	
	/**
	 * Context; typically the Activity that sets an object of this class as the
	 * Adapter
	 */
	private Context context;

	/**
	 * @param context
	 * @param textViewResourceId
	 */
	public InstanceGroupAdapter(Context context, int textViewResourceId,
			List<InstanceGroup> instanceGroups) {
		super(context, textViewResourceId, instanceGroups);

		// save the context, data, and list type
		this.context = context;
		this.instanceGroupsData = instanceGroups;
	}

	/**
	 * Overriden method called when ListView is initialized with data.
	 * 
	 * @param position
	 *            The position in {@link #instanceData}.
	 * @param convertView
	 *            The view to set.
	 * @param parent
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View instanceGroupDataRow = convertView;
		if (instanceGroupDataRow == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			instanceGroupDataRow = inflater.inflate(
					R.layout.ec2instancegroupsrow, parent, false);
		}

		// set main text view
		TextView textViewHeadline = (TextView) instanceGroupDataRow
				.findViewById(R.id.instanceGroupHeadline);
		TextView textViewDetails =
		 (TextView)instanceGroupDataRow.findViewById(R.id.instanceGroupDetails);
		
		
		InstanceGroup instanceGroup = instanceGroupsData.get(position);
		Log.d(TAG, "Instance grp: " + instanceGroup.getGroupName());
		textViewHeadline.setText(instanceGroup.getGroupName());
		if(instanceGroup.getInstanceIds()!=null){ 
			textViewDetails.setText(instanceGroup.getInstanceIds().size() + " " + context.getString(R.string.
					ec2instancegroupsview_instances));
		}
		return instanceGroupDataRow;
	}

}
