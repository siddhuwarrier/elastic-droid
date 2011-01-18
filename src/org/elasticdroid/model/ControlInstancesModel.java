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
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

/**
 * Model to allow user to control instance attributes:
 * <ul>
 * 	<li>Start instance</li>
 * 	<li>Stop instance </li>
 * 	<li>Set/change instance tag (tag key = name) </li>
 * </ul>
 * @author siddhu
 *
 * 7 Jan 2011
 */
public class ControlInstancesModel extends GenericModel<String, Void, Object> {
	
	/** 
	 * Enumeration to indicate type of control to perform 
	 * */
	public static enum ControlType {
		START_INSTANCE,
		STOP_INSTANCE,
		TAG_INSTANCE
	}
	/** The connection Data for AWS */
	private HashMap<String, String> connectionData;
	
	/** The operation to perform with this particular object */
	private ControlType operationType;
	
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.model.ControlInstancesModel";
	
	/** The EC2 tags to create, if any. Used only with @link{ControlType#TAG_INSTANCE} */
	private ArrayList<Tag> ec2Tags;
	
	/**
	 * Initialise the model by supplying connection data to start/stop instances
	 * 
	 * DO NOT USE THIS CONSTRUCTOR IF YOU ARE GOING TO TAG AN INSTANCE! YOU WILL GET AN ILLEGAL
	 * ARGUMENT EXCEPTION WHEN YOU EXECUTE IN BACKGROUND.
	 * 
	 * @param activity
	 * @param connectionData The AWS connection data
	 * @param operation Type: Start instance, stop instance, or tag instance. 
	 */
	public ControlInstancesModel(GenericActivity activity, HashMap<String, String> connectionData
			, ControlType operationType) {
		super(activity);
		
		this.connectionData = connectionData;
		this.operationType = operationType;
		//set the tag to null
		this.ec2Tags = null;
	}

	/**
	 * Initialise the model by supplying connection data to start/stop instances
	 * 
	 * DO NOT USE THIS CONSTRUCTOR IF YOU ARE GOING TO TAG AN INSTANCE! YOU WILL GET AN ILLEGAL
	 * ARGUMENT EXCEPTION WHEN YOU EXECUTE IN BACKGROUND.
	 * 
	 * @param listActivity
	 * @param connectionData The AWS connection data
	 * @param operation Type: Start instance, stop instance, or tag instance.
	 */
	public ControlInstancesModel(GenericListActivity listActivity, HashMap<String, String> 
		connectionData, ControlType operationType) {
		super(listActivity);
		
		this.connectionData = connectionData;
		this.operationType = operationType;
		//set the tag to null
		this.ec2Tags = null;
	}
	
	/**
	 * Initialise the model by supplying connection data to tag instances. Requires additional
	 * argument specifying the tag to use.
	 * 
	 * @param activity
	 * @param connectionData The AWS connection data
	 * @param operation Type: Start instance, stop instance, or tag instance.
	 * @param ec2Tagnames: The list of tagnames to assign the instances.
	 */
	public ControlInstancesModel(GenericActivity activity, HashMap<String, String> connectionData
			, ControlType operationType, List<String> ec2Tagnames) {
		super(activity);
		
		this.connectionData = connectionData;
		this.operationType = operationType;
		
		ec2Tags = new ArrayList<Tag>();
		//create a list of Tags.
		for (String ec2Tagname : ec2Tagnames) {
			ec2Tags.add(new Tag("Name", ec2Tagname));
		}
	}

	/**
	 * Initialise the model by supplying connection data to start/stop instances. Requires 
	 * additional argument specifying the tag to use
	 * 
	 * @param listActivity
	 * @param connectionData The AWS connection data
	 * @param operation Type: Start instance, stop instance, or tag instance.
	 * @param ec2Tagnames: The list of tagnames to assign the instances.
	 */
	public ControlInstancesModel(GenericListActivity listActivity, HashMap<String, String> 
		connectionData, ControlType operationType, List<String> ec2Tagnames) {
		super(listActivity);
		
		this.connectionData = connectionData;
		this.operationType = operationType;

		ec2Tags = new ArrayList<Tag>();
		//create a list of Tags.
		for (String ec2Tagname : ec2Tagnames) {
			ec2Tags.add(new Tag("Name", ec2Tagname));
		}
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
		if ((operationType == ControlType.START_INSTANCE) ||
				(operationType == ControlType.STOP_INSTANCE)) {
			return controlInstances(Arrays.asList(instances));
		}
		else {
			if (ec2Tags.size() == 0) {
				Log.v(TAG, "Deleting tags...");
				return deleteTags(Arrays.asList(instances));
			}
			else {
				//tag instance otherwise.
				return tagInstance(Arrays.asList(instances));
			}
		}
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
	 * 	<li> AmazonServiceException</li>
	 * 	<li> AmazonClientException</li>
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
		if (operationType == ControlType.START_INSTANCE) {
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
	
	/**
	 * Create/override tags of key name for the instances.
	 * @param instances: list of instances
	 * @return
	 * <ul>
	 * 	<li> true: to indicate success in reassigning the tags </li>
	 * 	<li>IllegalArgumentException: If the number of instances != number of tags</li>
	 * <li>AmazonServicesException: Serverside issues with AWS</li>
	 * <li>AmazonClientException: (Probably) connectivity issues</li>
	 * </ul>
	 */
	public Object tagInstance(List<String> instances) {		
		if (instances.size() != ec2Tags.size()) {
			return new IllegalArgumentException("The number of instances should be equal to be " +
					"the number");
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
		
		//create a TagsRequest
		for (String instance : instances) {
			Log.v(TAG, "Tagging " + instance);
		}
		CreateTagsRequest request = new CreateTagsRequest(instances, ec2Tags);
		
		
		//okay, tag the instance
		try {
			amazonEC2Client.createTags(request);
		}
		catch(AmazonServiceException amazonServiceException) {
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) {
			return amazonClientException;
		}
		
		return new Boolean(true); //return true to indicate success!
	}
	
	public Object deleteTags(List<String> instances) {
		
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
		
		//create empty tags for each of the instances from which the name tag is to be deleted.
		for (String instance : instances) {
			Log.v(TAG, "Tagging " + instance);
			//create a tag with Name for each instance from which Name tag is to be deleted.
			ec2Tags.add(new Tag("Name"));
		}

		DeleteTagsRequest request = new DeleteTagsRequest(instances);
		request.setTags(ec2Tags);

		//okay, tag the instance
		try {
			amazonEC2Client.deleteTags(request);
		}
		catch(AmazonServiceException amazonServiceException) {
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) {
			return amazonClientException;
		}
		
		return new Boolean(true); //return true to indicate success!
	}
}
