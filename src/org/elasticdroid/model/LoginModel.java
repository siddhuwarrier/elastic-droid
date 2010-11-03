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

import org.elasticdroid.GenericActivity;
import org.elasticdroid.utils.DialogConstants;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;

/**
 * This class is the model class for the Login window. It performs the following actions:
 * a) Verifies credentials by connecting to AWS.
 * b) Stores valid credentials in database.
 * c) Notifies observers (presently the Login activity alone) that authentication is valid (or not).
 * d) TODO Can be used to retrieve saved credentials from SQLite DB.
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
	 * Called in *UI Thread* before doInBackground executes in a separate thread.
	 */
	@Override
	protected void onPreExecute() {
		activity.showDialog(DialogConstants.PROGRESS_DIALOG.ordinal()); //the argument is not used
	}
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Object doInBackground(String... params) {
		//we need username, accessKey, secretAccessKey
		if (params.length != 3) {
			Log.e(this.getClass().getName(), "Need 3 params."); //TODO do something better.
			return null;
		}
		
		/*
		 * Note: AFAIK, AWS does not verify credentials for a whole "session", but does it request  
		 * by request. So we need to send it a request to  verify the credentials. I looked at the
		 * ElasticFox code which is maintained by Amazon, and they appear to do the same. 
		 * However, they use the Query API.
		 * 
		 * So we need to send an explicit request to verify.
		 * 
		 *  Change this if there is a more elegant method in future AWS APIs.
		 */
		try { 
			new AmazonEC2Client(new BasicAWSCredentials(params[1], params[2])).describeRegions();
		}
		//catch client and service exceptions
		catch(AmazonServiceException amazonServiceException) {
			Log.e(this.getClass().getName(), "Exception:" + amazonServiceException.getMessage());
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) { 
			Log.e(this.getClass().getName(), "Exception:" + amazonClientException.getMessage());
			return amazonClientException;
		}
		
		return null;
	}
	
	/**
	 * Called in the *UI thread* after doInBackground completes.
	 * 
	 * @param result The results returned by doInBackground
	 */
	@Override
	protected void onPostExecute(Object result) {
		//just return the result produced to the Activity.
		//we could process it here, but I want to keep the MVC pattern clean.
		//Call me a f*cking pedant, if you will.
		//...
		//...
		//I HEARD THAT!!!
		if (activity != null) {
			activity.processModelResults(result);
		}
	}
}
