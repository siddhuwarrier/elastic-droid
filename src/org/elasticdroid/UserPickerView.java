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
import static org.elasticdroid.utils.ResultConstants.*;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Class to show a list of users to choose from.
 * @author Siddhu Warrier
 *
 * 7 Nov 2010
 */
public class UserPickerView extends ListActivity {
	
	/** hashtable to store userdata (username, accesskey, secret accesskey) keyed by username*/
	private Hashtable<String, ArrayList<String>> userData; 
	
	/** 
	 * Called when the activity is first created or recreated. 
	 * 
	 */
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
		
		//Add New User to list of usernames.
		ArrayList<String> usernames = new ArrayList<String>(userData.keySet());
		usernames.add(this.getString(R.string.userpickerview_new_user));

		//add the usernames to the list adapter to display.
		setListAdapter(new UserPickerAdapter(this, R.layout.userpickerrow, 
				(String[])usernames.toArray(new String[ usernames.size()]) ));
	}
	
	/**
	 * Overriden listen method to capture clicks on List Item
	 */
	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		String selectedUsername = list.getItemAtPosition(position).toString();
		Intent resultIntent = new Intent();
		
		Log.v(this.getClass().getName(), "Item selected: " + selectedUsername);
		
		//if the user wants a new user.
		if (selectedUsername.equals(this.getString(R.string.userpickerview_new_user))) {
			//no extras.
			setResult(RESULT_NEW_USER,resultIntent);
		}
		else {
			//add data to resultIntent
			resultIntent.putExtra("USERNAME", selectedUsername);
			resultIntent.putExtra("ACCESS_KEY", userData.get(selectedUsername).get(0));
			resultIntent.putExtra("SECRET_ACCESS_KEY", userData.get(selectedUsername).get(1));
			
			//set result
			setResult(RESULT_OK, resultIntent);
		}
		
		//finish the activity
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

/**
 * Custom ArrayAdapter to display unique icons for items in the ListView.
 * @author Siddhu Warrier
 *
 * 10 Nov 2010
 */
class UserPickerAdapter extends ArrayAdapter<String> {

	/**List of usernames */
	String[] usernames;
	/** Context; typically the Activity that sets an object of this class as the Adapter */
	Context context;
	/**
	 * Constructor. Set private members
	 * @param context
	 * @param textViewResourceId
	 * @param usernames
	 */
	public UserPickerAdapter(Context context, int textViewResourceId, String[] usernames) {
		super(context, textViewResourceId, usernames);
		
		this.context = context;
		this.usernames = usernames;
	}
	
	/**
	 * Overriden method called when ListView is initialised with data.
	 * @param position The position in {@link #usernames}.
	 * @param convertView The view to set.
	 * @param parent
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View userPickerRow = convertView;
		
		if (userPickerRow == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService
				(Context.LAYOUT_INFLATER_SERVICE);
			
			userPickerRow = inflater.inflate(R.layout.userpickerrow, parent, false);
		}
		
		//set textview
		TextView textViewUsername = (TextView)userPickerRow.findViewById(R.id.username);
		textViewUsername.setText(usernames[position]);
		//set imageview
		ImageView imageViewUsernameIcon = (ImageView)userPickerRow.findViewById(R.id.
				username_icon);
		
		//if new user. set icon to new.
		if (usernames[position].equalsIgnoreCase("New user")) {
			//set it bold in a different font.
			textViewUsername.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
			//set a different icon.
			imageViewUsernameIcon.setImageResource(R.drawable.ic_menu_invite);
		} else
		{
			imageViewUsernameIcon.setImageResource(R.drawable.ic_menu_login);
		}
		
		return userPickerRow;
	}
}