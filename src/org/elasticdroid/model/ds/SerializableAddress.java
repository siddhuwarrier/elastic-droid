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
 * Authored by siddhu on 26 Dec 2010
 */
package org.elasticdroid.model.ds;

import java.io.Serializable;

/**
 * Holds information about an IP address.
 * 
 * This is Serializable, and has been created so that we can save the object
 * when the Activity is destroyed.
 * @author siddhu
 *
 * 26 Dec 2010
 */
public class SerializableAddress implements Serializable {

	/**
	 * Serial Version UID 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The public IP address
	 */
	private String publicIp;
	/**
	 * The associated instance ID
	 */
	private String instanceId;
	
	/**
	 * Constructor to create a SerializableAddress object
	 * @param publicIp: IP Address
	 * @param instanceId: The instance ID associated with this IP address 
	 */
	public SerializableAddress(String publicIp, String instanceId) {
		this.publicIp = publicIp;
		
		//if there is no associated instance ID for this IP, set instanceID to null
		if (instanceId.trim().equals("")) {
			this.instanceId = null;
		}
		else {
			this.instanceId = instanceId;
		}
	}
	
	/**
	 * Get the instance ID for this {@link SerializableAddress}.
	 */
	public String getInstanceId() {
		return instanceId;
	}
	
	/**
	 * Get the public IP for this {@link SerializableAddress}.
	 */
	public String getPublicIp() {
		return publicIp;
	}
}
