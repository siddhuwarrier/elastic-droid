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
 * Authored by siddhu on 17 Jan 2011
 */
package org.elasticdroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * A simple activity to get user to enter tag for the instance.
 * 
 * The tag is a key, value pair. The key is always set to "Name" because:
 * <ul>
 * 	<li> This is the default key set by AWS.</li>
 * 	<li> This is the key that Elastic Droid looks for in EC2DisplayInstancesView </li>
 * </ul>
 * @author siddhu
 *
 * 17 Jan 2011
 */
public class TagView extends Activity implements OnClickListener {

	/**
	 * The instance Id to tag
	 */
	private String instanceId;
	
	/**
	 * The tag for the instance Id
	 */
	private String tag;
	
	/**
	 * Use a simple layout
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		Intent intent = this.getIntent();
		
		instanceId = intent.getStringExtra("instanceId");
		this.tag = intent.getStringExtra("tag");
		
		this.setContentView(R.layout.tag);

		this.setTitle("Set Tag for" + instanceId);
		
		//set the OK button to read OK (in English at least)
		Button okButton = ((Button)findViewById(R.id.okButton));
		okButton.setText(this.getText(android.R.string.ok));
		okButton.setOnClickListener(this);
	}
	
	/**
	 * Restore the tag variable with the contents of the EditText when the activity is restored.
	 */
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		this.tag = stateToRestore.getString("tag");
	}
	
	/**
	 * Resume execution. Restores the contents of the EditText to what it was before the activity
	 * was killed (for example, when the user changes screen orientation).
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		//set the EditText to contain whatever is in tag
		((EditText) findViewById(R.id.tagEntry)).setText(tag);
	}
	
	/**
	 * Save the Edit Text contents when the activity is killed.
	 */
	@Override
	public void onSaveInstanceState(Bundle stateToSave) {
		//save tag entered in by user.
		stateToSave.putString("tag", ((EditText)findViewById(R.id.tagEntry)).getText().toString());
	}

	/**
	 * When user clicks on the OK button, return the tag entered in the EditText by the user
	 * to the calling activity
	 */
	@Override
	public void onClick(View v) {
		Intent resultIntent = new Intent();
		//set tag to whatever edit text contains
		tag = ((EditText)findViewById(R.id.tagEntry)).getText().toString();
		resultIntent.putExtra("tag", tag);
		
		setResult(RESULT_OK, resultIntent);
		
		finish(); //kill the activity
	}
	
	/**
	 * Handle back button.
	 * If back button is pressed, UI should die.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//do not allow user to return to previous screen on pressing back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_CANCELED); //Cancel execution. Do not return anything.
			
			finish(); //kill activity
		}
		
		return super.onKeyDown(keyCode, event); //if anything else, pass to superclass
	}
}
