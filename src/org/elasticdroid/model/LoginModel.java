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
 * Authored by Siddhu Warrier on 1 Nov 2010
 */
package org.elasticdroid.model;

import java.util.regex.Pattern;

import org.elasticdroid.GenericActivity;
import org.elasticdroid.db.ElasticDroidDB;
import org.elasticdroid.db.tblinfo.LoginTbl;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.User;

/**
 * This class is the model class for the Login window. It performs the following actions:
 * a) Verifies credentials by connecting to AWS.
 * b) Stores valid credentials in database.
 * c) Notifies observers (presently the Login activity alone) that authentication is valid (or not).
 * d) Can be used to retrieve saved credentials from SQLite DB.
 * @author Siddhu Warrier
 *
 * 1 Nov 2010
 */
public class LoginModel extends GenericModel<String, Void, Object> {	
	
	/**
	 * To call super Constructor alone. To read what this constructor
	 * does, please refer to the superclass documentation.
	 * @param genericActivity The activity which started this model
	 */
	public LoginModel(GenericActivity genericActivity) {
		super(genericActivity);
	}
	
	/** 
	 * Check AWS credentials, and save to DB if valid.
	 * 
	 * When this method finishes, the 
	 * {@link org.elasticdroid.model.GenericModel#onPostExecute(Object)} is called, which
	 * notifies the view.
	 * 
	 * This method, inherited from Android AsyncTask is automagically run in a separate background
	 *  thread.
	 */
	@Override
	protected Object doInBackground(String... params) {
		//we need username, accessKey, secretAccessKey
		if (params.length != 3) {
			Log.e(this.getClass().getName(), "Need 3 params."); //TODO do something better.
			return null;
		}
		
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(params[1], params[2]);
		//create an IAM client
		AmazonIdentityManagementClient idManagementClient = new AmazonIdentityManagementClient
			(credentials);
		User userData = null;
		
		try {
			userData = idManagementClient.getUser().getUser();//ensure the user ID is 
			//matched to the access and secret access keys
		}
		catch(AmazonServiceException amazonServiceException) {
			//if an error response is returned by AmazonIdentityManagement indicating either a 
			//problem with the data in the request, or a server side issue.
			Log.e(this.getClass().getName(), "Exception:" + amazonServiceException.getMessage());
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) { 
			//If any internal errors are encountered inside the client while attempting to make 
			//the request or handle the response. For example if a network connection is not available. 
			Log.e(this.getClass().getName(), "Exception:" + amazonClientException.getMessage());
			return amazonClientException;
		}
		
		//if we get here, the userData variable has been initialised.
		//check if the user name specified by the user corresponds to the
		//user name associated with the acess and secret access keys specified			
		String username = userData.getUserName();
		
		if (username != null) { //this is an IAM username
			if (!username.equals(params[0])) {
				/*Log.e(this.getClass().getName(), "Username " + params[0] + ", " + userData.
						getUserName() + " does not correspond to access and secret access key!");*/
				//return *not throw* an illegalArgumentException, because this is a different thread.
				return new IllegalArgumentException("Username does not correspond to access and " +
						"secret access key!");
			}	
		}
		else {
			//this is a proper AWS account, and not an IAM username.
			//check if the username is a proper email address. Java regexes look +vely awful!
			Pattern emailPattern = Pattern.compile("^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$", 
					Pattern.CASE_INSENSITIVE);
			
			//if this is not an email address
			if (!emailPattern.matcher(params[0]).matches()) {
				return new IllegalArgumentException("Username is an AWS account. Please enter a" +
				" valid email address.");				
			}
		}
		
		/*writing to DB*/
		// if we get here, then write the data to the DB
		ElasticDroidDB elasticDroidDB = new ElasticDroidDB(activity);
		//open the database for writing
		SQLiteDatabase db = elasticDroidDB.getWritableDatabase();
		ContentValues rowValues = new ContentValues();
		//check if the username already exists
		//set the data to write
		rowValues.put(LoginTbl.COL_USERNAME, params[0]);
		rowValues.put(LoginTbl.COL_ACCESSKEY, params[1]);
		rowValues.put(LoginTbl.COL_SECRETACCESSKEY, params[2]);
		
		//if data is found, update.
		if (db.query(LoginTbl.TBL_NAME, new String[]{}, LoginTbl.COL_USERNAME 
			+ "=?", new String[]{params[0]}, null, null, null).getCount() != 0) {
			try {
				db.update(LoginTbl.TBL_NAME, rowValues, LoginTbl.COL_USERNAME + "=?", 
						new String[]{params[0]});
			}
			catch(SQLException sqlException) {
				
				Log.e(this.getClass().getName(), "SQLException: " + sqlException.getMessage());
				return sqlException; //return the exception for the View to process.
			}
			finally {
				db.close();
			}
		}
		else {
			//now write the data in, replacing if necessary!
			try {
				db.insertOrThrow(LoginTbl.TBL_NAME, null, rowValues);
				
			}
			catch(SQLException sqlException) {
				
				Log.e(this.getClass().getName(), "SQLException: " + sqlException.getMessage());
				return sqlException; //return the exception for the View to process.
			}
			finally {
				db.close();
			}	
		}
		
		return null;
	}
}