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
 * Authored by siddhu on 3 Jan 2011
 */
package org.elasticdroid.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.tpl.GenericModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.AWSConstants;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;

/**
 * @author siddhu
 *
 * 3 Jan 2011
 */
public class CloudWatchMetricsModel extends GenericModel<Dimension, Void, Object> {

	/**
	 * The connection data
	 */
	private HashMap<String, String> connectionData;
	/**
	 * The Cloudwatch end point for this region
	 */
	private String cloudWatchEndpoint;
	
	/**
	 * Constructor for GenericActivity
	 * @param activity
	 */
	public CloudWatchMetricsModel(GenericActivity activity, HashMap<String, String> connectionData, 
			String selectedRegion){
		super(activity);
		
		this.connectionData = connectionData;
		cloudWatchEndpoint = AWSConstants.getCloudWatchEndpoint(selectedRegion);
	}
	
	/**
	 * Constructor for GenericListActivity
	 * @param activity
	 */
	public CloudWatchMetricsModel(GenericListActivity listActivity, HashMap<String, String> 
		connectionData, String selectedRegion) {
		super(listActivity);
		
		this.connectionData = connectionData;
		cloudWatchEndpoint = AWSConstants.getCloudWatchEndpoint(selectedRegion);
	}

	/**
	 * Execute the model in background
	 */
	@Override
	protected Object doInBackground(Dimension... dimensions) {
		return retrieveMetricsList(dimensions);
	}
	
	/**
	 * Retrieve the list of metrics
	 * 
	 * @return Either
	 * <ul>
	 * 	<li>AmazonServiceException</li>
	 *  <li>AmazonClientException</li>
	 *  <li>List\<Metric\></li>
	 * </ul>
	 */
	public Object retrieveMetricsList(Dimension... dimensions) {
		//the cloudwatch client to use
		AmazonCloudWatchClient cloudWatchClient = null;
		List<Metric> returnedMetrics = null;
		ArrayList<String> measureNames = new ArrayList<String>();
		ListMetricsRequest request = new ListMetricsRequest();
		
		
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(connectionData.get("accessKey"),
				connectionData.get("secretAccessKey"));
		
		//create a cloudwatch client
		try {
			cloudWatchClient = new AmazonCloudWatchClient(credentials);
		}
		catch(AmazonServiceException amazonServiceException) {
			//if an error response is returned by AmazonIdentityManagement indicating either a 
			//problem with the data in the request, or a server side issue.
			Log.e(this.getClass().getName(), "Exception:" + amazonServiceException.getMessage());
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) { 
			//If any internal errors are encountered inside the client while attempting to make 
			//the request or handle the response. For example if a network connection is not 
			//available. 
			Log.e(this.getClass().getName(), "Exception:" + amazonClientException.getMessage());
			return amazonClientException;
		}
		
		cloudWatchClient.setEndpoint(cloudWatchEndpoint);
		
		//create the request
		request = new ListMetricsRequest();
		//request.setNextToken(nextToken)
		try {
			returnedMetrics = cloudWatchClient.listMetrics().getMetrics();
		}
		catch(AmazonServiceException amazonServiceException) {
			//if an error response is returned by AmazonIdentityManagement indicating either a 
			//problem with the data in the request, or a server side issue.
			Log.e(this.getClass().getName(), "Exception:" + amazonServiceException.getMessage());
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) { 
			//If any internal errors are encountered inside the client while attempting to make 
			//the request or handle the response. For example if a network connection is not 
			//available. 
			Log.e(this.getClass().getName(), "Exception:" + amazonClientException.getMessage());
			return amazonClientException;
		}
		
		//my own disgusting O(mnp) filter. This is REALLY CRAP!
		//remove all that does not fit into the dimension
		//for some reason, there isn't a way to provide dimensions using ListMetricsRequest in Java
		//you can do it in the C# API though :P
		List<Dimension> returnedDimensions;
		boolean added;
		for (Metric metric : returnedMetrics) {
			returnedDimensions = metric.getDimensions();
			added = false;
			for (Dimension returnedDimension : returnedDimensions) {
				//check if any of the dimensions passed in to the model are equal to this.
				for (Dimension dimension : dimensions) {
					if (returnedDimension.getValue().equals(dimension.getValue())) {
						measureNames.add(metric.getMeasureName());
						added = true;
						break;
					}
				}
				
				if (added) {
					break;
				}
			}
		}

		return measureNames;
	}

}
