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
import java.util.HashMap;
import java.util.List;

import org.apache.http.ConnectionClosedException;
import org.elasticdroid.R;
import org.elasticdroid.model.tpl.GenericModel;
import org.elasticdroid.tpl.GenericActivity;
import org.elasticdroid.utils.MiscUtils;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * This class is a model class to get the SSH URI to use to connect to this instance.
 * 
 * <ul>
 * <li>It checks if the port on the instance that the user wishes to connect to is available
 * (default to-port 22), as well the port to connect from (hard-coded to 22 atm).</li>
 * <li>If the port is available, it makes sure that the device's IP is in the IP address ranges
 * acceptable on the instance (for that port).</li>
 * <li> If this is the case as well, it produces an SSH URI which can be used as a URI to Connect
 * Bot</li>
 * </ul>
 * @author siddhu
 *
 * 15 Dec 2010
 */
public class SshConnectorModel extends GenericModel<String, Void, Object> {

	/**
	 * the port to connect to: toPort in AWS parlance
	 * {@see http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html}.
	 */
	private int toPort;
	/** username to connect using */
	private String username;
	/** hostname to connect to */
	private String hostname;
	/**
	 * The connection data
	 */
	private HashMap <String, String> connectionData;
	/**
	 * The tag for printing out log messages
	 */
	private static final String TAG = "org.elasticdroid.model.SshConnectorModel";
	
	/**
	 * Constructor, sets toPort to 22  
	 * @param genericActivity
	 */
	public SshConnectorModel(GenericActivity genericActivity, HashMap<String, String> 
		connectionData, String username, String hostname) {
		
		super(genericActivity);
		
		this.connectionData = connectionData;
		this.username = username;
		this.hostname = hostname;
		toPort = 22;
	}
	
	/**
	 * Constructor.
	 * @param genericActivity The activity that called the model
	 * @param toPort The port to try to connect to.
	 */
	public SshConnectorModel(GenericActivity genericActivity, HashMap<String, String> 
		connectionData, String username, String hostname, int toPort) {
		
		super(genericActivity); //call parent class constructor
		
		this.connectionData = connectionData;
		this.toPort = toPort;
		
		this.username = username;
		this.hostname = hostname;
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Object doInBackground(String... secGroups) {
		
		return prepareSshUri(secGroups);
	}
	
	@SuppressWarnings("unchecked")
	public Object prepareSshUri(String... secGroups) {
		
		String sourceIpAddress = null;
		//first, get the source IP address
		try {
			URL ipAddressUri = new URL("http://www.whatismyip.com/automation/n09230945.asp");
			HttpURLConnection connection = (HttpURLConnection) ipAddressUri.openConnection();
			connection.setConnectTimeout(5000); //time out in 5 seconds
			sourceIpAddress = new BufferedReader(new InputStreamReader(connection.getInputStream())).
				readLine();
		}
		catch(Exception exception) {
			//if you can't retrieve it, just return a ConnectionClosedexception
			sourceIpAddress = null;
		}
		//just a check in case they change the way whatismyip.org works
		if (sourceIpAddress == null) {
			return new ConnectionClosedException(
					activity.getString(R.string.sshconnector_cannotretrievehostip));
		}
		Log.v(TAG, "Your Device's IP address is: " + sourceIpAddress);
		
		
		
		//get the information on the security groups in list
		ArrayList<Filter> secGroupFilters = new ArrayList<Filter>();
		List<SecurityGroup> securityGroups = null;//initialise it
		boolean portFound = false; //set to indicate port was found
		//used to identify whether the fail was cuz of the port failing, or cuz of the IP address
		//ranges.
		
		for (String secGroup : secGroups) {
			//add filters for each of the security groups
			secGroupFilters.add(new Filter("group-name").withValues(secGroup));
		}
		//pass the filters to the SecurityGroupsModel
		//do not use the execute method so as to have it run in this thread
		Object result = new SecurityGroupsModel(activity, connectionData).getSecurityGroupData(
				secGroupFilters.toArray(new Filter[secGroupFilters.size()]));
		
		if (result instanceof List<?>) {
			securityGroups = (List<SecurityGroup>)result;
		}
		//pass the exception on
		else if (result instanceof AmazonServiceException) {
			return (AmazonServiceException)result;
		}
		//pass the exception on
		else if (result instanceof AmazonClientException) {
			return (AmazonClientException) result;
		}
		
		
		//now scan through each of the security groups, and check if toPort is open in any of them.
		//If so, check if this IP address is in the acceptable list
		for (SecurityGroup securityGroup: securityGroups) {
			
			List<IpPermission> permissions = securityGroup.getIpPermissions();
			
			//loop through the permissions
			for (IpPermission permission : permissions) {
				if (permission.getToPort() == toPort) {
					//check if the IP address is right
					Log.v(this.getClass().getName(), "" + permission.getIpRanges().toString());
					
					portFound = true;
					
					//if source IP address is null, i.e. our WAN IP resolver is down, don't
					//check if this port is blocked for our IP address
					if (sourceIpAddress != null) {
						//loop through the acceptable IP address ranges
						for (String sourceCidr : permission.getIpRanges()) {
							//split the source IP address along the dots
							//split the source CIDR along the / to remove CIDR ignore bits, followed 
							//by along the dots.					
							if (MiscUtils.checkIpPermissions(sourceIpAddress.split("\\."), 
									sourceCidr.split("/")[0].split("\\."), 
									Integer.valueOf(sourceCidr.split("/")[1]))) {
								//success, everything is fine. IP permissions, the works.
								String sshUri = "ssh://" + username + "@" + hostname + ":" + toPort;
								
								//add nickname to show on ConnectBot screen
								sshUri += "#" + username + "@" + hostname;
								return sshUri; 
							}
						}
					}
				}
			}
		}
		
		//if we get here, we failed
		if (portFound) {
			//if we did find a port, return the error that the IP address provided is blocked.
			return new ConnectionClosedException(activity.getString(
					R.string.sshconnector_ipaddressblocked));
		} 
		else {
			return new ConnectionClosedException(activity.getString(
					R.string.sshconnector_portblocked));
		}
			
	}
}
