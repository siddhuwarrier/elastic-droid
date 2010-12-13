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
 * Authored by siddhu on 12 Dec 2010
 */
package org.elasticdroid.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.GenericListActivity;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.Filter;

/**
 * Model class that takes as (optional) argument a Filter, and returns 
 * either a List<@link{com.amazonaws.services.ec2.model.Address}>, or 
 * an Amazon Service/Client Exception 
 * @author siddhu
 * 12 Dec 2010
 */
public class ElasticIPsModel extends GenericListModel<Filter, Void, Object> {
	/** The connection Data for AWS */
	private HashMap<String, String> connectionData;
	/**
	 * @param genericActivity
	 */
	public ElasticIPsModel(GenericListActivity genericActivity, HashMap<String, String>
		connectionData) {
		super(genericActivity);//call super class
		this.connectionData = connectionData;
	}
	
	/** (non-Javadoc)
	 * Execute task in background
	 * @return List<@link{com.amazonaws.services.ec2.model.Address}> if successful, or
	 * exception on failure.
	 */
	@Override
	protected Object doInBackground(Filter... filters) {
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(connectionData.get("accessKey"),
				connectionData.get("secretAccessKey"));
		//create Amazon EC2 Client object, and set tye end point to the region. params[3]
		//contains endpoint
		AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
		amazonEC2Client.setEndpoint(connectionData.get("endpoint"));
		
		//create a new DescribeAddressesRequest
		DescribeAddressesRequest request = new DescribeAddressesRequest();
		request.setFilters(new ArrayList<Filter>(Arrays.asList(filters)));
		
		List<Address> addressList; //result == List<Address>
		//make the request to Amazon EC2
		try {
			addressList = amazonEC2Client.describeAddresses(request).getAddresses();
		}
		catch(AmazonServiceException amazonServiceException) {
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) {
			return amazonClientException;
		}
		
		return addressList;
	}

}
