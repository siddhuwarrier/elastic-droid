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
 * Authored by siddhu on 27 Dec 2010
 */
package org.elasticdroid.model.ds;

import java.io.Serializable;
import java.util.List;

import com.amazonaws.services.ec2.model.IpPermission;

/**
 * @author siddhu
 *
 * 27 Dec 2010
 */
public class SerializableIpPermission implements Serializable {

	/** From port */
	private Integer fromPort;
	/** To port */
	private Integer toPort;
	/** IP protocol to use (TCP, UDP) */
	private String ipProtocol;
	/** Allowable IP address range */
	private List<String> ipRanges;
	
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructor to build SerializableIpPermission from IpPermission
	 * @param ipPermission
	 */
	public SerializableIpPermission(IpPermission ipPermission) {
		fromPort = ipPermission.getFromPort();
		toPort = ipPermission.getToPort();
		
		ipProtocol = ipPermission.getIpProtocol();
		ipRanges = ipPermission.getIpRanges();
	}
	
	/**
	 * Return the from port
	 * @return {@link SerializableIpPermission#fromPort}
	 */
	public int getFromPort() {
		return fromPort;
	}
	
	/**
	 * Return the toPort
	 * @return {@link SerializableIpPermission#toPort}
	 */
	public int getToPort() {
		return toPort;
	}
	
	/**
	 * Return the allowable IP ranges for this security group
	 * @return {@link SerializableIpPermission#ipRanges}
	 */
	public List<String> getIpRanges() {
		return ipRanges;
	}
	
	/**
	 * Return the IP protocol used (TCP, UDP etc)
	 * @return {@link SerializableIpPermission#ipProtocol}
	 */
	public String getIpProtocol() {
		return ipProtocol;
	}
}
