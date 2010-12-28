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
 * Authored by Siddhu Warrier on 14 Nov 2010
 */
package org.elasticdroid.model;

import java.util.HashMap;

import org.elasticdroid.model.tpl.GenericModel;
import org.elasticdroid.tpl.GenericListActivity;
import org.elasticdroid.utils.AWSConstants;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * @author Siddhu Warrier
 *
 * 14 Nov 2010
 */
public class EC2DashboardModel extends GenericModel<HashMap<?,?>, 
	Void, Object> {
	/**
	 * 
	 * @param genericActivity
	 */
	public EC2DashboardModel(GenericListActivity genericActivity) {
		super(genericActivity);
	}

	/**
	 * Gets the data to populate the EC2 Dashboard with in the background thread, and loads it into
	 * a Hashtable<String, Integer>. 
	 * 
	 * @param This method accepts *ONE* Hashtable<String, String> of LoginDetails arguments. The
	 * required keys are as follows (anything else is ignored):
	 * <ul>
	 * <li> accessKey: The accesskey for the AWS/AWS IAM account used.</li> 
	 * <li> secretAccessKey: The secretAccessKey for the AWS/AWS IAM account used.</li> 
	 * <li> endpoint: AWS Endpoint for the selected region (@see {@link AWSConstants.EndPoints}</li>
	 * </ul>
	 * If you're missing any of these keys, AmazonServiceExceptions will be thrown. This shouldn't
	 * be visible to the end-user as this is a programmer fault!!! :P
	 * 
	 * @return This method can return:
	 * <ul>
	 * <li>{@link IllegalArgumentException}: If there are too many/few arguments, or the keys are  
	 * incorrect. Only one Hashtable<String, String> accepted.</li>
	 * <li>{@link Hashtable<String, Integer}: data to populate dashboard with.
	 * 		<ul>
	 * 		<li><i>runningInstances:</i> The number of running instances for the user in the current 
	 * 		region</li>
	 * 		<li><i>stoppedInstances:</i> The number of stopped instances for the user in the current 
	 * 		region</li>
	 * 		<li><i>elasticIp:</i> The number of elastic IPs owned by the user (in the current region)
	 *		</li>
	 * 		<li><i>securityGroups:</i> The number of security groups avail 2 the user (in the current
	 * 		region)</li>
	 * 		<li><i>keyPairs:</i> The number of keypairs avail 2 the user (in the current
	 * 		region)</li>
	 * 		</ul> 
	 * </li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object doInBackground(HashMap<?,?>... params) {
		HashMap<String, String> connectionData;
		HashMap<String, Integer> dashboardData;
		
		//we accept only one param, but AsyncTask forces us to potentially accept
		//a whole bloody lot of them. :P
		if (params.length != 1) {
			return new IllegalArgumentException("Only one Hashtable<String,String> parameter " +
					"should be passed.");
		}
		connectionData = (HashMap<String, String>)params[0]; //convenience variable, so that
		//i dont have to keep typing params[0] everywhere in this method.;)
		
		Log.v(this.getClass().getName(), "Getting EC2 dashboard data...");
		
		//prepare to get the dashboard data!
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(connectionData.get("accessKey"),
				connectionData.get("secretAccessKey"));
		//create Amazon EC2 Client object, and set tye end point to the region. params[3]
		//contains endpoint
		AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
		amazonEC2Client.setEndpoint(connectionData.get("endpoint"));
		//initialise result holder variable
		dashboardData = new HashMap<String, Integer>();
		
		try {
			//get the number of running and stopped instances
			DescribeInstancesResult instances = amazonEC2Client.describeInstances();
			
			int numOfRunningInstances = 0;
			int numOfStoppedInstances = 0;
			//get the list of reservations in the results
			for (Reservation reservation: instances.getReservations()) {
				//for each reservation, get the list of instances associated
				for (Instance instance: reservation.getInstances()) {
					if (instance.getState().getCode().byteValue() == InstanceStateConstants.RUNNING) {
						numOfRunningInstances ++;
					}
					else if (instance.getState().getCode().byteValue() == InstanceStateConstants.
							STOPPED) {
						numOfStoppedInstances ++;
					}
				}
			}
			dashboardData.put("runningInstances", numOfRunningInstances);
			dashboardData.put("stoppedInstances", numOfStoppedInstances);
			
			//get the list of elastic Ips.
			dashboardData.put("elasticIp", amazonEC2Client.describeAddresses().getAddresses().size());
	
			//get the list of security groups
			dashboardData.put("securityGroups", amazonEC2Client.describeSecurityGroups().getSecurityGroups().size());
			
			//get the list of keypairs
			dashboardData.put("keyPairs", amazonEC2Client.describeKeyPairs().getKeyPairs().size());
		}
		catch(AmazonServiceException amazonServiceException) {
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) {
			return amazonClientException;
		}
		
		return dashboardData;
	}

}
