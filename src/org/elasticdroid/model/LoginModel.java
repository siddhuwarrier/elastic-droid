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

import java.util.Observable;

import android.util.Log;

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
public class LoginModel extends Observable {	
	/**
	 * Default constructor for LoginModel.
	 * Does nothing.
	 */
	public LoginModel() {
		
	}
	
	/**
	 * Method to verify credentials and notify Observer of
	 * whether the credentials are valid.
	 */
	public void verifyCredentials(String username, String accessKey, String secretAccessKey) {
		Boolean credentials = true; 
		
		Log.v(this.getClass().getName(), "In verifyCredentials()");
		//try to connect by sending a request for the available regions
		/*
		 * Note: AFAIK, AWS does not verify credentials for a whole "session", but does it request  
		 * by request. So we need to send it a request to  verify the credentials. I looked at the
		 * ElasticFox code which is maintained by Amazon, and they appear to do the same. 
		 * However, they use the Query API.
		 * 
		 *  Change this if there is a more elegant method in future AWS APIs.
		 */
		try { 
			new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretAccessKey)).describeRegions();
		}
		//TODO change to AmazonServiceException: http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/AmazonServiceException.html
		catch(Exception exception) {
			Log.v(this.getClass().getName(), "Exception:" + exception.getStackTrace().toString());
			credentials = false;
		}
		
		setChanged();
		notifyObservers(credentials);
	}
}
