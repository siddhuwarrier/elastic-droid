package org.elasticdroid.model.ds;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.util.Log;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * This class has a subset of the methods available in the SecurityGroup class
 * that is necessary for ElasticDroid.
 * 
 * The reason this class has been created is because the SecurityGroup class
 * provided by AWS SDK is not Serializable. The other alternatives I could think
 * of were:
 * 
 * <ul>
 * <li>Extend Instance and implement Serializable: But I may not be able to
 * initialize all of the private members of the Instance class.</li>
 * <li>Modify Instance and recompile AWS API. I think this is a bad idea as I
 * will be forking the API.</li>
 * <li>Use onRetainNonConfigurationInstance instead of onSaveInstanceState.
 * Again, bad idea as this may introduce a new memory leak. And anyway, I would
 * like to pass the SecurityGroup data to the individual instance activity, and
 * this requires Serializability/Parcelability.</li>
 * </ul>
 * 
 * Hence, this. This class may require some additional maintenance if the AWS
 * API changes significantly.
 * 
 * @author Rodolfo Cartas
 * 
 *         27 Dec 2010
 */

public class SerializableSecurityGroup implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Owner Id
	 */
	private String ownerId;

	/**
	 * Security Group Name
	 */
	private String groupName;

	/**
	 * Security Group Description
	 */
	private String description;

	/**
	 * The serializable IP perms; converted from the IP perm
	 */
	private ArrayList<SerializableIpPermission> ipPermissions;
	
	/**
	 * SerializableSecurityGroup construcotr
	 * @param ownerId ID of the owner of the SecGroup
	 * @param groupName The name of the Sec Group
	 * @param description The user-defined descr of the secgroup
	 */
	public SerializableSecurityGroup(String ownerId, String groupName,
			String description) {
		super();
		this.ownerId = ownerId;
		this.groupName = groupName;
		this.description = description;
		
		this.ipPermissions = new ArrayList<SerializableIpPermission>();
	}
	
	/**
	 * SerializbleSecurityGroup constructor that gets its data from a Security Group
	 * @param securityGroup the security group to build the SerializableSecurityGroup from
	 */
	public SerializableSecurityGroup(SecurityGroup securityGroup) {
		super();
		
		this.ownerId = securityGroup.getOwnerId();
		this.groupName = securityGroup.getGroupName();
		this.description = securityGroup.getDescription();
		this.ipPermissions = new ArrayList<SerializableIpPermission>();
		
		//create SerializableIpPermission from each IpPermission
		for (IpPermission ipPermission : securityGroup.getIpPermissions()) {
			ipPermissions.add(new SerializableIpPermission(ipPermission));
		}
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Return all of the IP permissions available
	 * @return arraylist of SerializableIpPermissions
	 */
	public ArrayList<SerializableIpPermission> getIpPermissions() {
		return ipPermissions;
	}
	
	/**
	 * Utility method to get a list of all open ports.
	 * If the fromPort and toPort are different, a single string "fromport-toport" will
	 * be returned.
	 * @return ArrayList<String> of all open port ranges
	 */
	public ArrayList<String> getOpenPorts() {
		//ArrayList<String> openPorts = new ArrayList<String>();
		
		HashMap<String, Integer> openPorts = new HashMap<String, Integer>();
		
		for (SerializableIpPermission ipPermission : ipPermissions) {
			if (ipPermission.getFromPort() == ipPermission.getToPort()) {
				openPorts.put(String.valueOf(ipPermission.getToPort()), 1);
			}
			else {
				StringBuffer strBuf = new StringBuffer(String.valueOf(
						ipPermission.getFromPort()));
				strBuf.append("-");
				strBuf.append(ipPermission.getToPort());
				Log.v(this.getClass().getName(), "Port range: " + strBuf.toString());
				openPorts.put(strBuf.toString(), 1);
			}
		}
		
		//convert to ArrayList<String> and return
		return new ArrayList<String>(Arrays.asList(openPorts.keySet().toArray(
				new String[openPorts.keySet().size()])));
	}
}
