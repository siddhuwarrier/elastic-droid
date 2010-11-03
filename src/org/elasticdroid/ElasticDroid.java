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
import android.content.pm.ActivityInfo;
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
	private AlertDialog alertDialogBox;
    private boolean progressDialogDisplayed;
    private boolean alertDialogDisplayed;
    private String alertDialogMessage;
    
    
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
        
        //create the alert dialog
		alertDialogBox = new AlertDialog.Builder(this).create(); //create alert box to
		
		//add a neutral OK button and set action.
		alertDialogBox.setButton("Ok", new DialogInterface.OnClickListener() {			 
            //click listener on the alert box - unlock orientation when clicked.
			//this is to prevent orientation changing when alert box locked.
            public void onClick(DialogInterface arg0, int arg1) {
            	alertDialogDisplayed = false;
            	alertDialogBox.dismiss();
            }
		});
        
        View loginButton = findViewById(R.id.loginButton);//set action listeners for the buttons
        loginButton.setOnClickListener(this);//this class will listen to the login buttons
    }
    
    /**
     * This method re-launches a dialog box if necessary. 
     * 
     * This derives from the Android lifecycle. When an Activity is destroyed and recreated, 
     * this is the order of calls made:
     * *onSaveInstanceState()
     * *onDestroy()
     * *onCreate()
     * *onStart()
     * *onRestoreInstanceState()
     * *onResume()
     * 
     *  So it is only in onRestoreInstanceState that we have restored the values of 
     *  alertDialogDisplayed and alertDialogMessage. So if we need to re-display it,
     *  we should override onResume() as well. 
     */
    @Override
    public void onResume(){
    	
    	super.onResume(); //call base class method
    	
        //check if an alert dialog was displayed before
        //remember: booleans are false by default and will be set to true
        //only if this object was destroyed and recreated when a dialog was displyaed.
		Log.v("onCreate()", "Recreating dialog box if necessary...");
        if (alertDialogDisplayed) {
        	alertDialogBox.setMessage(alertDialogMessage);
        	alertDialogBox.show();
        }
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
				
				alertDialogMessage = "Invalid Access Key. Please re-enter your " +
						"Access and/or Secret Access keys.";
			} 
			else if (((AmazonServiceException)result).getStatusCode() == HttpStatus.SC_FORBIDDEN) {
				alertDialogBox.setMessage("Invalid Secret Access key. Please re-enter your Secret Access key.");
				((EditText)findViewById(R.id.sakEntry)).setError("Invalid Secret Access key.");
			}
			else {
				//TODO a wrong SecretAccessKey is handled using a different error if the AccessKey is right.
				//Handle this.
				alertDialogMessage = "Unexpected error: " + ((AmazonServiceException)result).
						getStatusCode() + "--" + ((AmazonServiceException)result).getMessage()
						+ ". Please file a bug report.";
			}	
			
			//whatever the error, display the error
			//and set the boolean to true. This is so that we know we should redisplay
			//dialog on restore.
			Log.e(this.getClass().getName(), alertDialogMessage);
			
			alertDialogDisplayed = true;
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();//show error
		}
		else if (result instanceof AmazonClientException) {
			alertDialogMessage = "Unable to connect to AWS. Are you " +
			"connected to the Internet?";
			Log.e(this.getClass().getName(), alertDialogMessage);
			
			alertDialogDisplayed = true;
			alertDialogBox.setMessage(alertDialogMessage);
			alertDialogBox.show();//show error
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
	
	/**
	 * Save dialog boxes if any
	 */
	@Override
	public void onSaveInstanceState(Bundle saveState) {
		Log.v(this.getClass().getName(), "Save instance state...");
		
		//if a dialog is displayed when this happens, dismiss it
		if (alertDialogDisplayed) {
			alertDialogBox.dismiss();
		}
		saveState.putBoolean("alertDialogDisplayed", alertDialogDisplayed);
		saveState.putString("alertDialogMessage", alertDialogMessage);
		//call the superclass (Activity)'s save state method
		super.onSaveInstanceState(saveState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle stateToRestore) {
		Log.v(this.getClass().getName(), "Restore instance state...");
		
		super.onRestoreInstanceState(stateToRestore);
		alertDialogDisplayed = stateToRestore.getBoolean("alertDialogDisplayed");
		Log.v(this.getClass().getName(), "alertDialogDisplayed = " + alertDialogDisplayed);
		alertDialogMessage = stateToRestore.getString("alertDialogMessage");
	}
	
}//end of class