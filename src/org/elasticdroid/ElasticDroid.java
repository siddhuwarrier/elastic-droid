package org.elasticdroid;

import java.util.regex.Pattern;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;


public class ElasticDroid extends Activity implements OnClickListener {
	/** 
	 * Private members
	 */
	private String username;
	private String accessKey;
	private String secretAccessKey;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	//AWSCredentials credentials = new BasicAWSCredentials("FILL_IN_HERE", 
    		//	"FILL_IN_HERE");    
    	//AmazonEC2 ec2 = new AmazonEC2Client(credentials);
    	
        //DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
        //Log.v(this.getLocalClassName(),"You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                //" Availability Zones.");
        
        //TODO check in SQLite DB if you have login info
        setContentView(R.layout.login);
        
        //set action listeners for the buttons
        View loginButton = findViewById(R.id.loginButton);
        //this class will listen to the login buttons
        loginButton.setOnClickListener(this);
    }

    /**
     * @brief Handles the event of the login button being clicked.
     * @author siddhu
     */
	@Override
	public void onClick(View buttonClicked) {
		//check which button generated the onClick
		switch (buttonClicked.getId()) {
		
		case R.id.loginButton:
			if (!validateLoginDetails()) {
				Log.v(this.getClass().getName(), "Please enter correct login details.");
			}
			break;
		}
	}	
	
	/**
	 * @brief Private method to validate the login details
	 * 
	 * @return false if any of the fields isn't filled, or the email address is invalid.
	 * true otherwise
	 * 
	 * @author Siddhu Warrier
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
		TextView textViewUsername = (TextView)findViewById(R.id.usernameTextView);
		TextView textViewAccessKey = (TextView)findViewById(R.id.akTextView);
		TextView textViewSecretAccessKey = (TextView)findViewById(R.id.sakTextView);
		
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
		
		//if all of the validation checks succeeded, return true
		return true;
	}
}