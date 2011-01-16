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
 * Authored by Siddhu Warrier on 17 Nov 2010
 */
package org.elasticdroid;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.elasticdroid.db.ElasticDroidDB;

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
import android.widget.CheckedTextView;
import android.widget.ListView;

/**
 * Simple class to display list of regions, and allow user to pick
 * a region, any region. This will be set as the default region for the user.
 * @author Siddhu Warrier
 *
 * 17 Nov 2010
 */
public class DefaultRegionView extends ListActivity {

	/**the list of regions */
	private ArrayList<String> regionData;
	/** the existing default region */
	private String username;
	
	/**
	 * Called when activity is first displayed.
	 * Should receive list of regions as Intent.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); //call superclass onCreate
		
		//the default region
		String defaultRegion = null;
		
    	//this class is called from any dashboard, and is passed an argument in the intent.
    	Intent intent = this.getIntent();
    	try {
    		this.regionData = intent.getStringArrayListExtra(
    				"regionData");
    	}
    	//the possible exceptions are NullPointerException: the ArrayList was not found.
    	catch(NullPointerException exception) {
    		Log.e(this.getClass().getName(), exception.getMessage());
    		finish(); //kill the application, and off to bed.
    	}
    	try {
    		this.username = intent.getStringExtra("username");
    	}
    	//the possible exceptions are NullPointerException: the ArrayList was not found.
    	catch(NullPointerException exception) {
    		Log.e(this.getClass().getName(), exception.getMessage());
    		finish(); //kill the application, and off to bed.
    	}

    	//set the content view
    	this.setContentView(R.layout.defaultregion);
    	
    	try {
			defaultRegion = new ElasticDroidDB(this).getDefaultRegion(this.username);
		} catch (SQLException sqlException) {
			Log.e(this.getClass().getName(), "Unexpected error. Cannot access DB: " + 
					sqlException.getMessage());
			finish();//kill the activity
		}
    	//use the custom adapter to display the list of regions.
    	DefaultRegionAdapter regionsListAdapter = new DefaultRegionAdapter(this,
    			R.layout.customspinnerdropdownitem,
    			Arrays.asList(regionData.toArray()).toArray(new String[regionData.size()]),
    			defaultRegion);
    	
    	setListAdapter(regionsListAdapter);
	}
	
	/**
	 * Overridden listen method to listen to selection of new default region
	 */
	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		Intent resultIntent = new Intent();
		resultIntent.setType(this.getClass().getName());
		
		((CheckedTextView)v).setChecked(true); //sort of pointless as the whole activity will
		//disappear anyway! ;)
		//set the default region to the new default region chosen
		new ElasticDroidDB(this).setDefaultRegion(this.username, regionData.get(position));
		
		//return the new default region to the calling view.
		resultIntent.putExtra("defaultRegion", regionData.get(position));
		setResult(RESULT_OK, resultIntent);
		
		finish(); //finish the activity off.
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
}

class DefaultRegionAdapter extends ArrayAdapter<String> {
	/**List of regions */
	String[] regions;
	/** Default region */
	String defaultRegion;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	Context context;
	
	/**
	 * @param context
	 * @param textViewResourceId
	 * @param regions The list of regions
	 * @param defaultRegion The default region
	 */
	public DefaultRegionAdapter(Context context, int textViewResourceId, String[] regions, 
			String defaultRegion) {
		super(context, textViewResourceId, regions);
		this.context = context;
		this.regions = regions;
		this.defaultRegion = defaultRegion;
	}

	/**
	 * Overriden method called when ListView is initialised with data.
	 * @param position The position in {@link #usernames}.
	 * @param convertView The view to set.
	 * @param parent
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View regionRow = convertView;
		if (regionRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
				(Context.LAYOUT_INFLATER_SERVICE);
			regionRow = inflater.inflate(R.layout.customspinnerdropdownitem, parent, false);
		}
		
		CheckedTextView regionCheckedTextView = (CheckedTextView) regionRow.findViewById
			(R.id.regionDropDownCheckedTextView);
		
		regionCheckedTextView.setText(regions[position]);
		if (regions[position].equals(defaultRegion)) {
			regionCheckedTextView.setChecked(true);
		} else {
			regionCheckedTextView.setChecked(false);
		}
		
		return regionRow;
	}
}
