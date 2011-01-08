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
 * Authored by siddhu on 7 Jan 2011
 */
package org.elasticdroid.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.tpl.GenericModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.tpl.GenericListActivity;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;

/**
 * Model to allow user to start and stop instances
 * @author siddhu
 *
 * 7 Jan 2011
 */
public class ControlInstancesModel extends GenericModel<String, Void, Object> {
	
	/** The connection Data for AWS */
	private HashMap<String, String> connectionData;
	/** Boolean to indicate whether to stop an instance or start it */
	private boolean stop;
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.model.ControlInstancesModel";
	/**
	 * Initialise the model by supplying connection data
	 * 
	 * @param activity
	 */
	public ControlInstancesModel(GenericActivity activity, HashMap<String, String> connectionData
			, boolean stop) {
		super(activity);
		
		this.connectionData = connectionData;
		this.stop = stop;
	}

	/**
	 * Initialise the model by supplying connection data
	 * 
	 * @param activity
	 */
	public ControlInstancesModel(GenericListActivity listActivity, HashMap<String, String> 
		connectionData, boolean stop) {
		super(listActivity);
		
		this.connectionData = connectionData;
		this.stop = stop;
	}
	
	/**
	 * @param instances List of instances to stop or start
	 * @return @see {@link ControlInstancesModel#controlInstances(List)}
	 */
	@Override
	protected Object doInBackground(String... instances) {
		//TODO remove
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//call controlInstances to do the actual job.
		return controlInstances(Arrays.asList(instances));
	}
	
	/**
	 * Method that does the actual work of starting or stopping the instances
	 * 
	 * This method uses the stop boolean to identify whether the instances should be stopped 
	 * (stop = true) or started (stop = false).
	 * 
	 * @return Returns one of the following:
	 * <ul>
	 * 	<li>newInstanceStates: Returns a list of stateCodes and state names for all of the instances
	 * 	</li>
	 * 	<li> </li>
	 * 	<li> </li>
	 * </ul>
	 * 
	 */
	public Object controlInstances(List<String> instances) {
		
		for (String instance : instances) {
			Log.v(TAG, "Starting instance: " + instance);
		}
		
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(connectionData.get("accessKey"),
				connectionData.get("secretAccessKey"));
		//create Amazon EC2 Client object, and set tye end point to the region. params[3]
		//contains endpoint
		AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
		//override the default connection endpoint if provided.
		if (connectionData.get("endpoint") != null) {
			amazonEC2Client.setEndpoint(connectionData.get("endpoint"));
		}
		
		//if you want to start an instance
		if (!stop) {
			StartInstancesRequest request = new StartInstancesRequest(instances);
			StartInstancesResult result = null;
			try {
				result = amazonEC2Client.startInstances(request);
			}
			catch(AmazonServiceException amazonServiceException) {
				return amazonServiceException;
			}
			catch(AmazonClientException amazonClientException) {
				return amazonClientException;
			}
			//redundant check.
			if (result != null) {
				return result.getStartingInstances();
			}
		}
		//stop = true, start the instance.
		else {
			StopInstancesRequest request = new StopInstancesRequest(instances);
			StopInstancesResult result = null;
			
			try {
				result = amazonEC2Client.stopInstances(request);
			}
			catch(AmazonServiceException amazonServiceException) {
				return amazonServiceException;
			}
			catch(AmazonClientException amazonClientException) {
				return amazonClientException;
			}
			
			if (result != null) {
				return result.getStoppingInstances();
			}
		}
		
		return null;
	}
}
