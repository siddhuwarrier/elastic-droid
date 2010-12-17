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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.ConnectionClosedException;
import org.elasticdroid.GenericListActivity;

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
public class SshConnectorModel extends GenericListModel<String, Void, Object> {

	/**
	 * the port to connect to: toPort in AWS parlance
	 * {@see http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html}.
	 */
	int toPort;
	/**
	 * The connection data
	 */
	HashMap <String, String> connectionData;
	/**
	 * The source IP address
	 */
	String sourceIpAddress;
	
	/**
	 * Constructor, sets toPort to 22  
	 * @param genericActivity
	 */
	public SshConnectorModel(GenericListActivity genericActivity, HashMap<String, String> 
		connectionData, int ipAddressInt) {
		super(genericActivity);
		
		this.connectionData = connectionData;
		toPort = 22;
		this.sourceIpAddress = getIpAddressString(ipAddressInt);
	}
	
	/**
	 * Constructor.
	 * @param genericActivity The activity that called the model
	 * @param toPort The port to try to connect to.
	 */
	public SshConnectorModel(GenericListActivity genericActivity, HashMap<String, String> 
		connectionData, int ipAddressInt, int toPort) {
		super(genericActivity); //call parent class constructor
		
		this.connectionData = connectionData;
		this.sourceIpAddress = getIpAddressString(ipAddressInt);
		this.toPort = toPort;
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
					
					//loop through the acceptable IP address ranges
					for (String sourceCidr : permission.getIpRanges()) {
						//split the source IP address along the dots
						//split the source CIDR along the / to remove CIDR ignore bits, followed by
						//along the dots.					
						if (checkIpPermissions(sourceIpAddress.split("\\."), 
								sourceCidr.split("/")[0].split("\\."), 
								Integer.valueOf(sourceCidr.split("/")[1])) == 0) {
							return new String("SSH URI goes here!"); 
						}
					}
				}
			}
		}
		
		//if we get here, we failed
		if (portFound) {
			//if we did find a port, return the error that the IP address provided is blocked.
			return new ConnectionClosedException("IP address blocked.");
		} 
		else {
			return new ConnectionClosedException("Port closed.");
		}
			
	}
	
	/**
	 * Checks if the IP address provided falls within the acceptable range for the source CIDR
	 * 
	 * This checks using some irritating bitwise arithmetic made more irritating by the absence of
	 * unsigned types in Java.
	 * 
	 * @param ipAddressValues String[4] containing each byte of the IP address
	 * @param rangeValues String[4] containing each byte of the source CIDR.
	 * @param cidr The number of bits to ignore (starting from LSB).
	 * @return
	 */
	private int checkIpPermissions(String[] ipAddressValues, String[] rangeValues, int cidr) {
		int byteCount;
		int rangeInt, ipAddressInt;
		int mask;
		
		for (byteCount = 0; byteCount < cidr/8; byteCount++) {
			Log.d(this.getClass().getName() + "checkIpPermissions()", "Full Comparison of byte " + 
					byteCount);
			//string comparisons work just as well here
			if (!rangeValues[byteCount].equals(ipAddressValues[byteCount])) {
				return -1;
			}
		}
		
		//if the CIDR bits specify a partial byte to be checked as well
		if (cidr % 8 != 0) {
			rangeInt = Integer.valueOf(rangeValues[byteCount]);
			ipAddressInt = Integer.valueOf(ipAddressValues[byteCount]);
			
			//& by 255 because we want to set all bit above bit 8 to 0
			//this is cuz unsigned types don't exist.
			mask = (rangeInt << (cidr % 8)) & 255;
			//now shift to the right two bits so as to have restored everything
			//except the bits in this byte that we want to compare
			mask = mask >> (cidr % 8);
			rangeInt = rangeInt - mask;
			
			//do the same with IpAddress
			mask = (ipAddressInt << (cidr % 8)) & 255;
			mask = mask >> (cidr % 8);
			ipAddressInt = ipAddressInt - mask;
			
			if (rangeInt != ipAddressInt) {
				return -1;
			}
		}
		
		Log.v(this.getClass().getName() + "getIpPermissions()", "Returning 0 now...");
		
		return 0;
	}
	
	/**
	 * Get the WAN IP address of the Android phone.
	 * 
	 * It is returned as an integer, with each byte of the integer holding the corresponding byte.
	 * @return
	 */
	private String getIpAddressString(int ipAddress) {		
		String ipAddressString = "";
		
		int shiftBytes = 24;
		while (shiftBytes >= 0) {
			ipAddressString += String.valueOf((ipAddress >> shiftBytes) & 255) + ".";
			shiftBytes -=8;
		}
		
		//remove trailing "."
		ipAddressString = ipAddressString.substring(0, ipAddressString.length() - 1);
		
		Log.v(this.getClass().getName() + ".getIpAddressString()", ipAddressString);
		
		return ipAddressString;
	}

}
