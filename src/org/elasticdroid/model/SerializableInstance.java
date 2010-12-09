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
 * Authored by Siddhu Warrier on 8 Dec 2010
 */
package org.elasticdroid.model;

import java.io.Serializable;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

/**
 * This class has a subset of the methods available in the Instance class that is necessary for
 * ElasticDroid. 
 * 
 * The reason this class has been created is because the Instance class provided by AWS SDK
 * is not Serializable. The other alternatives I could think of were:
 * 
 * <ul>
 * <li>Extend Instance and implement Serializable: But I may not be able to initialize all of the 
 * private members of the Instance class.</li>
 * <li>Modify Instance and recompile AWS API. I think this is a bad idea as I will be forking the 
 * API.</li>
 * <li>Use onRetainNonConfigurationInstance instead of onSaveInstanceState. Again, bad idea as
 * this may introduce a new memory leak. And anyway, I would like to pass the Instance data to the
 * individual instance activity, and this requires Serializability/Parcelability.</li>
 * </ul>
 * 
 * Hence, this. This class may require some additional maintenance if the AWS API changes 
 * significantly.
 * 
 * @author Siddhu Warrier
 *
 * 8 Dec 2010
 */
public class SerializableInstance implements Serializable {

	private static final long serialVersionUID = 1L;
	
	//all of the private members required by the Activities. Add as required
	/**Tag with key=name (if any). We are not interested in any other sort of tag.*/
	private String tagName;
	/** Instance ID */
	private String instanceId;
	/** Instance Type: micro, small etc. See {@link http://aws.amazon.com/ec2/instance-types/}
	 */
	private String instanceType;
	/** Platform: Windows or null */
	private String platform;
	/** Launch time (ms). Save as long and convert to date if necessary. /MemoryMiser ;) */
	private long launchTime;
	/** The public IP address of the instance */
	private String publicIpAddress;
	/** The public DNS name of the instance */
	private String publicDnsName;
	
	/**
	 * Constructor. Initialises all of the members with data from the instance passed in as param.
	 * @param instance The Instance object to use to initialise this data.
	 */
	public SerializableInstance(Instance instance) {
		//get the instance ID, platform, launchtime, public IP address, public DNS name
		instanceId = instance.getInstanceId();
		instanceType = instance.getInstanceType();
		platform = instance.getPlatform();
		launchTime = instance.getLaunchTime().getTime();//save in milliseconds since epoch
		publicIpAddress = instance.getPublicIpAddress();
		publicDnsName = instance.getPublicDnsName();
		
		//setting tagName is a little harder. If there is a tag in the AWS cloud with key="name"
		//save the value of the tag to tagName. If not, set to null
		tagName = null; //set to null by default
		for (Tag tag : instance.getTags()) {
			if (tag.getKey().equalsIgnoreCase("name")) {
				tagName = tag.getValue();
				break;
			}
		}
	}
	
	/**
	 * Get the instance Id for this {@link SerializableInstance}.
	 * @return {@link SerializableInstance#instanceId}
	 */
	public String getInstanceId() {
		return instanceId;
	}
	
	/**
	 * Get the instance Type for this {@link SerializableInstance}.
	 * @return {@link SerializableInstance#instanceType}
	 */
	public String getInstanceType() {
		return instanceType;
	}	
	
	/**
	 * Get the platform for this {@link SerializableInstance}
	 * @return {@link SerializableInstance#platform}
	 */
	public String getPlatform() {
		return platform;
	}
	
	/**
	 * Get the launch time (millisconds) for this {@link SerializableInstance}
	 * @return {@link SerializableInstance#launchTime}
	 */
	public long getLaunchTime() {
		return launchTime;
	}

	/**
	 * Get the public IP Address for this {@link SerializableInstance}
	 * @return {@link SerializableInstance#publicIpAddress}
	 */
	public String getPublicIpAddress() {
		return publicIpAddress;
	}
	
	/**
	 * Get the public DNS name for this {@link SerializableInstance}
	 * @return {@link SerializableInstance#publicDnsName}
	 */
	public String getPublicDnsName() {
		return publicDnsName;
	}
	
	/**
	 * Get the tag for this {@link SerializableInstance}
	 * @return {@link SerializableInstance#publicDnsName}
	 */
	public String getTag() {
		return tagName;
	}
}