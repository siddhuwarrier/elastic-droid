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

import java.util.regex.Pattern;

import org.elasticdroid.model.LoginModel;
import org.elasticdroid.utils.DialogConstants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

import org.apache.commons.httpclient.HttpStatus;

/**
 * An activity class that inherits from GenericActivity which inherits from Activity.
 * This is because Java doesn't allow Multiple Inheritance like nice languages like C do! ;)
 * @author Siddhu Warrier
 *
 * 2 Nov 2010
 */
public class ElasticDroid extends GenericActivity implements OnClickListener {
	/** 
	 * Private members
	 */
	private String username;
	private String accessKey;
	private String secretAccessKey;
	private LoginModel loginModel;
    private boolean progressDialogDisplayed;
    
    
	/** 
	 * Called when the activity is first created. 
	 * 
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        //restore model if the activity was reloaded in the middle of model processing
        Object retained = getLastNonConfigurationInstance();
        if (retained instanceof LoginModel) {
            Log.i(this.getClass().getName(), "Reclaiming previous background task.");
            loginModel = (LoginModel) retained;
            loginModel.setActivity(this); //tell loginModel that this is the new recreated activity
        } 
        else {
        	loginModel = null;
        }
        
        View loginButton = findViewById(R.id.loginButton);//set action listeners for the buttons
        loginButton.setOnClickListener(this);//this class will listen to the login buttons
    }

    /**
     * @brief Handles the event of the login button being clicked. 
     * 
     */
	@Override
	public void onClick(View buttonClicked) {
		//if the data passes basic checks, then try accessing AWS
		if (validateLoginDetails()) {
			loginModel = new LoginModel(this);
			loginModel.execute(username, accessKey, secretAccessKey);
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
	
	/**
	 * Process results from model. Called by onPostExecute() method
	 * in any given Model class.
	 * 
	 * Displays either an error message (if result is an exeception)
	 * or the next activity.
	 * @see org.elasticdroid.GenericActivity#processModelResults(java.lang.Object)
	 */
	@Override
	public void processModelResults(Object result) {
		Log.v(this.getClass().getName(), "Processing model results...");
		
		//dismiss the progress bar
		if (progressDialogDisplayed) {
			dismissDialog(DialogConstants.PROGRESS_DIALOG.ordinal());
		}
		AlertDialog.Builder errorBox = new AlertDialog.Builder(this); //create alert box to
		
		//add a neutral OK button.
		errorBox.setNeutralButton("Ok", new DialogInterface.OnClickListener() {			 
            // empty click listener on the alert box
            public void onClick(DialogInterface arg0, int arg1) {
            }
		});
		/*
		 * The result returned by the model can be:
		 * a) AmazonServiceException: if authentication failed (typically).
		 * b) AmazonClientException: if communication to AWS failed (user not connected to internet?).
		 * c) null: if the credentials have been validated.
		 */
		if (result instanceof AmazonServiceException) {
			if (((AmazonServiceException)result).getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				//set errors in the access key and secret access key fields.
				((EditText)findViewById(R.id.akEntry)).setError("Invalid credentials");
				((EditText)findViewById(R.id.sakEntry)).setError("Invalid credentials");
				errorBox.setMessage("Invalid Access Key. Please re-enter your Access and/or Secret Access keys.");
				Log.e(this.getClass().getName(), "Invalid access key");
			} 
			else if (((AmazonServiceException)result).getStatusCode() == HttpStatus.SC_FORBIDDEN) {
				errorBox.setMessage("Invalid Secret Access key. Please re-enter your Secret Access key.");
				((EditText)findViewById(R.id.sakEntry)).setError("Invalid Secret Access key.");
			}
			else {
				//TODO a wrong SecretAccessKey is handled using a different error if the AccessKey is right.
				//Handle this.
				errorBox.setMessage("Unexpected error: " + ((AmazonServiceException)result).
						getStatusCode() + "--" + ((AmazonServiceException)result).getMessage()
						+ ". Please file a bug report.");
				Log.e(this.getClass().getName(), "Unexpected error");
			}
			
			errorBox.show();//show error
		}
		else if (result instanceof AmazonClientException) {
			errorBox.setMessage("Unable to connect to AWS. Are you " +
			"connected to the Internet?");
			
			errorBox.show(); //show error 
		}
		else {
			Log.v(this.getClass().getName(), "Valid credentials");
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.v(this.getClass().getName(), "Object about to destroyed...");
        
		//if the model is being executed when the onDestroy method is called.
		if (loginModel != null) {
			loginModel.setActivity(null);
			return loginModel;
		}
		return null;
	}

	/**
	 * Overriden from Activity and not GenericActivity!
	 * @param id Dialog ID - special treatment for ProgressDialog
	 * @param dialog - the dialog object itself. 
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
        	progressDialogDisplayed = true;
        }
	}

	/**
	 *  
	 * Overriden from Activity and not GenericActivity
	 * @param id DIalog ID - Special treatment for Constants.PROGRESS_DIALOG
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
	        ProgressDialog dialog = new ProgressDialog(this);
	        dialog.setMessage("Please wait...");
	        dialog.setCancelable(false);
	        return dialog;
		}
		//if some other sort of dialog...
        return super.onCreateDialog(id);
	}
}//end of class