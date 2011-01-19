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
 * Authored by rodolfo on 26 Dec 2010
 */
package org.elasticdroid.model.orm;


/**
 * Holds information about an InstanceGroup
 * 
 * This is Serializable, and has been created so that we can save the object
 * when the Activity is destroyed.
 * @author rodolfo
 *
 * 17 Ene 2011
 */
import java.io.Serializable;
import java.util.Set;

import org.elasticdroid.model.ds.SerializableInstance;

public class InstanceGroup implements Serializable {

	/**
	 * Serial Version UID 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The instance group id
	 */
	private long id;
	
	/**
	 * The instance group name
	 */
	private String groupName;
	
	/**
	 * The instances belonging to the group
	 */
	private Set<SerializableInstance> instances;
	
	/**
	 * Constructor to create an instance group
	 * 
	 * @param groupName
	 */
	public InstanceGroup(long id, String groupName) {
		super();
		this.groupName = groupName;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setInstances(Set<SerializableInstance> instances) {
		this.instances = instances;
	}

	public Set<SerializableInstance> getInstances() {
		return instances;
	}
	
}
