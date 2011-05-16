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
 * Authored by Siddhu Warrier on 5 Dec 2010
 */
package org.elasticdroid.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.ds.SerializableInstance;
import org.elasticdroid.model.tpl.GenericModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.DialogConstants;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * @author Siddhu Warrier
 *
 * 5 Dec 2010
 */
public class EC2InstancesModel extends GenericModel<Filter, Void, Object> {

	/** AWS Connection data */
	private HashMap<String, String> connectionData;
	/** Tag for logging */
	private static final String TAG = "org.elasticdroid.model.EC2InstancesModel";
	/** Selected region */
	private String selectedRegion;
	/** The expected instance state code at which to stop autorefreshing */
	private Integer expectedInstanceStateCode;
	/** The maximum backoff period for the binary exponential backoff algo used with autorefresh.
	 * In milliseconds */
	private static final long MAX_BACKOFF_PERIOD = 30000;
	
	/**
	 * Start a new EC2InstancesModel object from a GenericListActivity
	 * 
	 * @param genericActivity Of type GenericListActivity
	 * @param connectionData the data to use to connect to AWS.
	 * @param selectedRegion the selected region.
	 */
	public EC2InstancesModel(GenericListActivity genericActivity, HashMap<String, String>
		connectionData, String selectedRegion) {
		super(genericActivity);//call super class
		this.connectionData = connectionData;
		this.selectedRegion = selectedRegion;
	}
	
	/**
	 * Start a new ElasticIPsModel object from a GenericActivity
	 * 
	 * @param genericActivity Of type GenericActivity
	 * @param connectionData the data to use to connect to AWS.
	 * @param selectedRegion the selected region.
	 */
	public EC2InstancesModel(GenericActivity genericActivity, HashMap<String, String>
		connectionData, String selectedRegion) {
		super(genericActivity);//call super class
		this.connectionData = connectionData;
		this.selectedRegion = selectedRegion;
	}
	
	/**
	 * Overloaded GenericActivity contructor which is used to enable autorefresh until state
	 * changes to the specified state.
	 * 
	 * @param genericActivity Of type GenericActivity
	 * @param connectionData the data to use to connect to AWS.
	 * @param selectedRegion the selected region.
	 * @param expectedInstanceStateCode the instance state code at which we should stop
	 * auto-refreshing. 
	 */
	public EC2InstancesModel(GenericActivity genericActivity, HashMap<String, String> 
	connectionData, String selectedRegion, int expectedInstanceStateCode) {
		super(genericActivity);
		this.connectionData = connectionData;
		this.selectedRegion = selectedRegion;
		this.expectedInstanceStateCode = expectedInstanceStateCode;
	}
	
	/**
	 * Overloaded contructor which is used to enable autorefresh until state
	 * changes to the specified state (uses GenericListActivity).
	 * 
	 * @param genericActivity Of type GenericActivity
	 * @param connectionData the data to use to connect to AWS.
	 * @param selectedRegion the selected region.
	 * @param expectedInstanceStateCode the instance state code at which we should stop
	 * auto-refreshing.
	 */
	public EC2InstancesModel(GenericListActivity genericListActivity, HashMap<String, String> 
	connectionData, String selectedRegion, int expectedInstanceStateCode) {
		super(genericListActivity);
		this.connectionData = connectionData;
		this.selectedRegion = selectedRegion;
		this.expectedInstanceStateCode = expectedInstanceStateCode;
	}
	
	/** 
	 * Execute the model in the background thread.
	 * Calls @link{EC2InstancesModel#getInstances}.
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Object doInBackground(Filter... filters) {
		//if expectedInstanceStateCode is not null and a single INSTANCE filter is passed,
		//start the waitForInstanceStateChange method
		if (expectedInstanceStateCode != null) {
			if (filters.length != 1) {
				return new IllegalArgumentException("Filter length cannot be greater than 1" +
						"for auto-refresh.");
			}
			
			if (!filters[0].getName().equals("instance-id")) {
				return new IllegalArgumentException("Can only auto-refresh filters of type" +
						"\"instance-id\".");
			}
			
			if (filters[0].getValues().size() != 1) {
				return new IllegalArgumentException("Cannot auto-refresh more than 1 instance."); 
			}
			
			//if all ofthese conditions are met, start the waitForInstanceStateChange(...) method
			return waitForInstanceStateChange(filters[0]);
		}
		else {
			return getInstances(filters);
		}
	}

	/**
	 * 
	 * @param filters
	 * @return This method can return:
	 * <ul>
	 * <li>ArrayList<SerializableInstance>: If all goes well</li>
	 * <li>AmazonClientException: If there's connectivity problems on the client.</li>
	 * <li>AmazonServiceException: If there's AWS service problems.</li>
	 * <li>IllegalArgumentException: If the region can't be found.</li>
	 * </ul>
	 */
	public Object getInstances(Filter... filters) {
		ArrayList<SerializableInstance> serInstances = new ArrayList<SerializableInstance>(); 
		//result passed to Activity
		List<Region> regions;
		List<Reservation> reservations; //restult from EC2
		
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(connectionData.get("accessKey"),
				connectionData.get("secretAccessKey"));
		//create Amazon EC2 Client object, and set tye end point to the region. params[3]
		//contains endpoint
		AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
		
		//1. create a filter for this region name
		Filter regionFilter = new Filter("region-name");
		regionFilter.setValues(new ArrayList<String>(Arrays.asList(
				new String[]{selectedRegion})));
		
		//2. query using this filter
		try {
			regions = amazonEC2Client.describeRegions(new DescribeRegionsRequest().
				withFilters(regionFilter)).getRegions();
		}
		catch(AmazonServiceException exc) {
			return exc;
		}
		catch(AmazonClientException exc) {
			return exc;
		}
		
		//3. Make sure the region was found.
		if (regions.size() != 1) {
			return new IllegalArgumentException("Invalid region passed to model.");
		}
		
		Log.v(TAG + ".doInBackground()", "endpoint for region : " + 
				selectedRegion + "=" + regions.get(0).getEndpoint());
		//set the endpoint
		amazonEC2Client.setEndpoint(regions.get(0).getEndpoint());
		
		//now get the instances
		
		Log.v(TAG, "Size of filters:" + filters.length);
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.setFilters(Arrays.asList(filters));
		
		//get the list of instances using this filter
		try {
			reservations = amazonEC2Client.describeInstances(request).
				getReservations();
		}
		catch(AmazonServiceException amazonServiceException) {
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) {
			return amazonClientException;
		}
		
		//add each instance found into the list of instances to return to the view
		for (Reservation reservation: reservations) {
			List<String> securityGroups = reservation.getGroupNames();
			//note to self: List is an interface ArrayList implements.
			//for each reservation, get the list of instances associated
			for (Instance instance: reservation.getInstances()) {
				serInstances.add(new SerializableInstance(instance, securityGroups));
			}
		}
		
		return serInstances;
	}
	
	/**
	 * Called in *UI Thread* before doInBackground executes in a separate thread.
	 * 
	 * Overriden to prevent progress dialog from being shown if expected Instance State Code
	 * is null
	 */
	@Override
	protected void onPreExecute() {
		if (expectedInstanceStateCode == null) {
			if (!listActivityUsed) {
				activity.showDialog(DialogConstants.PROGRESS_DIALOG.ordinal()); //the argument is 
				//not used
			}
			else {
				listActivity.showDialog(DialogConstants.PROGRESS_DIALOG.ordinal()); //the argument  
				//is not used
			}
		}
		else {
			Log.v(TAG, "not displaying progress dialog as autorefresh operation being execd.");
		}
	}
	
	/**
	 * Wait for instance state change. This can be used to repeatedly refresh a single instance
	 * until its state changes from startState to endState
	 * @param filters
	 */
	@SuppressWarnings("unchecked")
	public Object waitForInstanceStateChange(Filter filter) {
		ArrayList<SerializableInstance> serInstances;
		//exponentialBackoff period to use
		long backoffPeriod = 50; 
		long sleepPeriod;
		//teh power of 2 to use in the binary exponential backoff algo 
		int multiplier = 0;
		
		//keep refreshing till the state of the instance is as expected.
		while(true) {
			Object result = getInstances(filter);
			
			if (result instanceof ArrayList<?>) {
				serInstances = (ArrayList<SerializableInstance>) result;
				
				//we know the list will have a size() = 1.
				if (serInstances.get(0).getStateCode() == expectedInstanceStateCode) {
					//stop auto-refreshing if the state of the instance is as expected
					return result;
				}
				else { //the instance is still not in the right state.
					//use the exponential backoff algo to sleep the thread for a specific period
					sleepPeriod = backoffPeriod * (int)((Math.random()* Math.pow(2, multiplier)));
					Log.v(TAG, "Backoff: Sleep for " + sleepPeriod + "msecs");
					
					//if hte backoff interval is getting too long, just call the autorefresh off.
					if (sleepPeriod >= MAX_BACKOFF_PERIOD) {
						return null;
					}
					
					try {
						Thread.sleep(sleepPeriod);
					} catch (InterruptedException e) {
						return null; //if interrupted, just stop executing
					}
				}
			}
			else {
				//not expected result; probably exception.
				//return the result and let the view worry about it.
				return result;
			}
			
			multiplier ++; //increase the binary exponential backoff algo multiplier
		}
	}
}
