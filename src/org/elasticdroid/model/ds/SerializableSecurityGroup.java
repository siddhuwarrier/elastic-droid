package org.elasticdroid.model.ds;

import java.io.Serializable;

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

	/* TODO IP Permissions List */

	public SerializableSecurityGroup(String ownerId, String groupName,
			String description) {
		super();
		this.ownerId = ownerId;
		this.groupName = groupName;
		this.description = description;
	}

	public SerializableSecurityGroup() {
		super();
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

}
