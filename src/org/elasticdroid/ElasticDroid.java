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
 * Authored by Siddhu Warrier on 22 Oct 2010
 */

package org.elasticdroid;

import java.util.Observable;
import java.util.Observer;
import java.util.regex.Pattern;

import org.elasticdroid.R;
import org.elasticdroid.model.LoginModel;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;


public class ElasticDroid extends Activity implements OnClickListener, Observer {
	/** 
	 * Private members
	 */
	private String username;
	private String accessKey;
	private String secretAccessKey;
	private LoginModel loginModel; 
	
	/** 
	 * Called when the activity is first created. 
	 * 
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        loginModel = new LoginModel();//create the Login model to do the grunt work
        loginModel.addObserver(this);//add this activity as an observer
        
        View loginButton = findViewById(R.id.loginButton);//set action listeners for the buttons
        loginButton.setOnClickListener(this);//this class will listen to the login buttons
    }

    /**
     * @brief Handles the event of the login button being clicked. 
     * 
     */
	@Override
	public void onClick(View buttonClicked) {
		//check which button generated the onClick
		switch (buttonClicked.getId()) {
		
		case R.id.loginButton:
			//if the data passes basic checks, then try accessing AWS
			if (validateLoginDetails()) {
				loginModel.verifyCredentials(username, accessKey, secretAccessKey);
				//note: username, accesskey, and secretaccesskey set in the validateLoginDetails
				//method
				//TODO display error (or 
			}
			break;
		}
	}	
	
	/**
	 * @brief Private method to perform basic validity checks on the credentials entered.
	 * 
	 * @return false if any of the fields isn't filled, or the email address is invalid.
	 * true otherwise
	 * 
	 */
	private boolean validateLoginDetails() {
		/*
		 * define local variables to hold the username, access key info from the UI.
		 * This is for convenience  mostly, and to avoid having to perform findViewById  
		 * lookups multiple times 
		*/
		EditText editTextUsername = (EditText)findViewById(R.id.usernameEntry);
		EditText editTextAccessKey = (EditText)findViewById(R.id.akEntry);
		EditText editTextSecretAccessKey = (EditText)findViewById(R.id.sakEntry);
		
		//get the strings from the username.
		username = editTextUsername.getText().toString(); //get the username
		accessKey = editTextAccessKey.getText().toString(); //get the access key
		secretAccessKey = editTextSecretAccessKey.getText().toString(); //get the secret access key
		
		Log.v(this.getClass().getName(), "User name:" + username);
		
		//if any of username, access key, or secret access key is blank, return false
		//and highlight the appropr. EditText box
		if (username.trim().equals("")) {
			editTextUsername.setError("Enter valid email address");
			editTextUsername.requestFocus();
			//return false to the click handler, so it doesn't try to login
			return false;
		} else if (accessKey.trim().equals("")) {
			editTextAccessKey.setError("Non-empty string");
			editTextAccessKey.requestFocus();
			
			//return false to the click handler, so it doesn't try to login
			return false;
		} else if (secretAccessKey.trim().equals("")) {
			
			editTextSecretAccessKey.setError("Non-empty string");
			editTextSecretAccessKey.requestFocus();			
			//set the focus
			editTextSecretAccessKey.requestFocus();

			//return false to the click handler, so it doesn't try to login
			return false;
		}
		
		//check if the username is a proper email address. Java regexes look +vely awful!
		Pattern emailPattern = Pattern.compile("^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$", 
				Pattern.CASE_INSENSITIVE);
		if (!emailPattern.matcher(username).find()) {
			editTextUsername.setError("Enter valid email address");
			editTextUsername.requestFocus();
			
			//return false to the click handler, so it doesn't try to login
			return false;		
		}
		
		//if all of the validation checks succeeded, check with the model.
		return true;
	}

	/* 
	 * Method called when the LoginModel makes a change. This will be used
	 * to decide whether the next activity should be called.
	 */
	@Override
	public void update(Observable loginModel, Object data) {
		//data is a boolean if verifyCredentials called on the LoginModel
		if (data instanceof Boolean) {
			if ((Boolean)data) {
				Log.i(this.getClass().getName(), "Valid credentials");
			}
			else {
				Log.e(this.getClass().getName(), "Invalid credentials");
			}
		}
	}
}