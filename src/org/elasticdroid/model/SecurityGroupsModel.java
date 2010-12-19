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
 * Authored by siddhu on 15 Dec 2010
 */
package org.elasticdroid.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.GenericActivity;
import org.elasticdroid.utils.MiscUtils;

import android.R;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * @author siddhu
 *
 * 15 Dec 2010
 */
public class SecurityGroupsModel extends GenericModel<Filter, Void, Object> {
	
	/** The AWS connection data */
	private HashMap<String,String> connectionData;
	/** Tag for printing log messages */
	private static final String TAG = "org.elasticdroid.model.SecurityGroupsModel";
	
	/**
	 * Alternate Constructor
	 * 
	 * This constructor gets the list of security groups that allow connections from the IP address
	 * in question.
	 * @param connectionData
	 * @param hostIpAddress The IP address in question.
	 */
	public SecurityGroupsModel(GenericActivity genericActivity, HashMap<String, String> 
		connectionData) {
		super(genericActivity);
		this.connectionData = connectionData;
		//if the boolean useHostIp is set, get the host IP address from whatismyip.org
		//this is because Android gives me my LAN address and not my public IP address
		//and returns 0.0.0.0 when using the network
	}
	
	/**
	 * Method that executes in background thread and does the actual work.
	 * @param []filters: A list of filters
	 */
	@Override
	protected Object doInBackground(Filter... filters) {
		
		//don't do any work here. Do it all in a testable public function
		//that can also be invoked in-thread.
		return getSecurityGroupData(filters);
	}
	
	/**
	 * The method that does the actual work 
	 */
	public Object getSecurityGroupData(Filter... filters) {
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(connectionData.get("accessKey"),
				connectionData.get("secretAccessKey"));
		//create Amazon EC2 Client object, and set tye end point to the region. params[3]
		//contains endpoint
		AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
		amazonEC2Client.setEndpoint(connectionData.get("endpoint"));
		
		DescribeSecurityGroupsRequest securityGroupsRequest = new DescribeSecurityGroupsRequest();
		//add filters to the request
		securityGroupsRequest.withFilters(new ArrayList<Filter>(Arrays.asList(filters)));
		
		List<SecurityGroup> securityGroups;
		try {
			securityGroups = amazonEC2Client.describeSecurityGroups(securityGroupsRequest).
				getSecurityGroups();
		}
		catch(AmazonServiceException amazonServiceException) {
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) {
			return amazonClientException;
		}
		
		boolean publicIpAddressValid;
		
		return securityGroups;
	}
}
