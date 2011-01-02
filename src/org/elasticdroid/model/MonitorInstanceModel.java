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
 * Authored by siddhu on 30 Dec 2010
 */
package org.elasticdroid.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.tpl.GenericModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.CloudWatchInput;
import org.elasticdroid.utils.AWSConstants.EndPoints;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

/**
 * Retrieve the metric required, and return an Object.
 * @author siddhu
 *
 * 30 Dec 2010
 */
public class MonitorInstanceModel extends GenericModel<Dimension, Void, Object> {

	/** Connection data */
	private HashMap<String, String> connectionData;
	/** The Cloudwatch input data to tell the CloudWatch API what data we want */
	private CloudWatchInput cloudWatchInput; 
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.model.MonitorInstanceModel";

	/**
	 * Constructor for type GenericActivity
	 * @param activity
	 */
	public MonitorInstanceModel(GenericActivity genericActivity, HashMap<String, String> 
		connectionData, CloudWatchInput cloudWatchInput) {
		super(genericActivity);
		
		this.connectionData = connectionData;
		this.cloudWatchInput = cloudWatchInput;
	}
	
	/**
	 * Another Constructor for type GenericListActivity
	 * @param activity
	 */
	public MonitorInstanceModel(GenericListActivity genericListActivity, HashMap<String, String>
		connectionData, CloudWatchInput cloudWatchInput) {
		super(genericListActivity);
		
		this.connectionData = connectionData;
		this.cloudWatchInput = cloudWatchInput;
	}

	/**
	 * Execute metric retrieval in background
	 */
	@Override
	protected Object doInBackground(Dimension... dimensions) {
		return retrieveMetrics(dimensions);
	}	
	
	/** 
	 * Perform the actual work of retrieving the metrics
	 */
	public Object retrieveMetrics(Dimension... dimensions) {
		//the cloudwatch client to use
		AmazonCloudWatchClient cloudWatchClient = null;
		//the request to send to cloudwatch
		GetMetricStatisticsRequest request;
		//the metric stats result.
		GetMetricStatisticsResult result;
		
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
			//the request or handle the response. For example if a network connection is not available. 
			Log.e(this.getClass().getName(), "Exception:" + amazonClientException.getMessage());
			return amazonClientException;
		}
		
		//prepare request
		request = new GetMetricStatisticsRequest();
		request.setStartTime(new Date(cloudWatchInput.getStartTime()));
		request.setEndTime(new Date(cloudWatchInput.getEndTime()));
		request.setPeriod(cloudWatchInput.getPeriod());
		request.setMeasureName(cloudWatchInput.getMeasureName());
		request.setNamespace(cloudWatchInput.getNamespace());
		request.setStatistics(cloudWatchInput.getStatistics());
		request.setDimensions(Arrays.asList(dimensions));
		//tell the cloudwatch client where to look!
		cloudWatchClient.setEndpoint(cloudWatchInput.getEndpoint());
		
		//get the monitoring result!
		try {
			result = cloudWatchClient.getMetricStatistics(request);
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
		
		//get the data and print it out.
		List<Datapoint> data = result.getDatapoints();
		for (Datapoint datum : data) {
			Log.v(TAG, "Datum:" + datum.getAverage());
		}
		
		//sort the data in ascending order of timestamps
		Collections.sort(data, new CloudWatchDataSorter());
		
		//return the sorted data
		return data;
	}
}

class CloudWatchDataSorter implements Comparator<Datapoint> {

	/**
	 * Sort two datapoint objects according to their timestamps.
	 */
	@Override
	public int compare(Datapoint o1, Datapoint o2) {
		if (o1.getTimestamp().getTime() > o2.getTimestamp().getTime()) {
			return -1;
		}
		else if (o1.getTimestamp().getTime() == o2.getTimestamp().getTime()) {
			return 0;
		}
		
		return 1;
	}
	
}
