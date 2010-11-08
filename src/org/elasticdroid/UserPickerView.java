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
 * Authored by Siddhu Warrier on 7 Nov 2010
 */
package org.elasticdroid;

import java.util.ArrayList;
import java.util.Hashtable;

import org.elasticdroid.db.ElasticDroidDB;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Class to show a list of users to choose from.
 * @author Siddhu Warrier
 *
 * 7 Nov 2010
 */
public class UserPickerView extends ListActivity {
	
	private Hashtable<String, ArrayList<String>> userData; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.userpicker);
		//get the list of users from the Database
		userData = new ElasticDroidDB(this).listUserData();
		//if there are no users in the DB, tell the calling activity it needs to ask for user input
		if (userData.size() == 0) {
			Intent resultIntent = new Intent();
			//set selection size attr in Intent to 0.
			resultIntent.putExtra("SELECTION_SIZE", 0);
			setResult(RESULT_OK, resultIntent);
			finish(); //kill the activity
		}
		
		//no, there is user data. Go on an
		String[] usernames = userData.keySet().toArray(new String[0]);
		//add the usernames to the list adapter to display.
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, usernames));
		
		ListView userPickerView = getListView();
		userPickerView.setOnItemClickListener(new OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View selectedItem,
		        int position, long id) {
		    	// When clicked, return result with username data etc to the parent activity
		    	returnSelectedUserData(selectedItem);
		    }
		  });

		
		userPickerView.setTextFilterEnabled(true);
	}
	
	private void returnSelectedUserData(View selectedItem) {
		String selectedUsername = ((TextView)selectedItem).getText().toString();
		
		Intent resultIntent = new Intent();
		//add data to resultIntent
		resultIntent.putExtra("USERNAME", selectedUsername);
		resultIntent.putExtra("ACCESS_KEY", userData.get(selectedUsername).get(0));
		resultIntent.putExtra("SECRET_ACCESS_KEY", userData.get(selectedUsername).get(1));
		
		//set result and finish
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	
	/**
	 * Handle back button.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_CANCELED); //let the calling activity know that the user chose to 
			//cancel
		}
		
		return super.onKeyDown(keyCode, event);
	}
}
