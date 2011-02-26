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
package org.elasticdroid.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.elasticdroid.model.EC2InstancesModel;
import org.elasticdroid.model.ds.SerializableInstance;
import org.elasticdroid.testharness.TestListActivity;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.amazonaws.services.ec2.model.Filter;

/**
 * @author siddhu
 *
 * 15 Dec 2010
 */
public class EC2InstancesModelTest extends ActivityInstrumentationTestCase2<TestListActivity> {
	/**
	 * Test connection Data
	 */
	private HashMap<String, String> connectionData;
	/**
	 * properties file with test connection data
	 */
	private Properties connectionProperties;
	/**
	 * properties file with test input data
	 */
	private Properties inputProperties;
	
	/**
	 * @param activityClass
	 * @throws IOException 
	 */
	public EC2InstancesModelTest() {
		super("org.elasticdroid.testharness", TestListActivity.class);
	}
	
	public void setUp() {
		connectionProperties = new Properties();
		inputProperties = new Properties();
		InputStream connectionPropStream, inputPropStream;
		
		try {
			connectionPropStream = getInstrumentation().getContext().getResources().getAssets().
				open("ec2_connection_data.properties");
			inputPropStream = getInstrumentation().getContext().getResources().getAssets().
				open("instances_input.properties");
			connectionProperties.load(connectionPropStream);
			inputProperties.load(inputPropStream);
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		//add connection data
		connectionData = new HashMap<String, String>();
		connectionData.put("username", connectionProperties.getProperty("username"));
		connectionData.put("accessKey", connectionProperties.getProperty("accessKey"));
		connectionData.put("secretAccessKey", connectionProperties.getProperty
				("secretAccessKey"));
		connectionData.put("endpoint", connectionProperties.getProperty("endpoint"));
		
		//pass a blank intent to the activity
		Intent intent = new Intent();
		setActivityIntent(intent);
	}
	
	/**
	 * SUCCESS TEST: Test if all of the instances returned are running instances.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void testRetrieveRunningInstances() {
		ArrayList<SerializableInstance> instances = null;
		
		Filter filter = new Filter("instance-state-code").withValues(new String[]{
				String.valueOf(InstanceStateConstants.RUNNING)});
		
		EC2InstancesModel model = new EC2InstancesModel(getActivity(), connectionData, 
				connectionProperties.getProperty("region"));
		Object result = model.getInstances(filter);
		
		if (!(result instanceof ArrayList<?>)) {
			fail("Result should be of type arraylist");
		}
		else {
			try {
				instances = (ArrayList<SerializableInstance>) result;
			}
			catch(ClassCastException exception) {
				fail("Result should be of type ArrayList<SerializableInstance>");
			}
			
			for (SerializableInstance instance : instances) {
				assertEquals(instance.getStateCode(), InstanceStateConstants.RUNNING);
			}
		}
	}
	
	/**
	 * SUCCESS TEST: Test if the instance specified in filter is returned
	 */
	@SuppressWarnings("unchecked")
	public void testRetrieveSpecificInstance() {
		ArrayList<SerializableInstance> instances = null;
		
		Filter filter = new Filter("instance-id").withValues(new String[]{
				inputProperties.getProperty("instance-id")});
		
		EC2InstancesModel model = new EC2InstancesModel(getActivity(), connectionData, 
				connectionProperties.getProperty("region"));
		Object result = model.getInstances(filter);
		
		if (!(result instanceof ArrayList<?>)) {
			fail("Result should be of type arraylist");
		}
		else {
			try {
				instances = (ArrayList<SerializableInstance>) result;
			}
			catch(ClassCastException exception) {
				fail("Result should be of type ArrayList<SerializableInstance>");
			}
			
			if (instances.size() != 1) {
				fail(String.format("Expected ArrayList<String> of size 1; found size=%d. " +
						"Also check instance ID in input Properties file.", instances.size()));
			}
			
			assertEquals(instances.get(0).getInstanceId(), inputProperties.getProperty(
					"instance-id"));
		}
	}
	
	/**
	 * FAILURE TEST: Test if an empty array list is returned if the filter has an instance
	 * that does not exist.
	 * 
	 * expected return value: empty ArrayList<SerializableInstance>; size = 0
	 */
	@SuppressWarnings("unchecked")
	public void testRetrieveNonExistentInstance() {
		ArrayList<SerializableInstance> instances = null;
		
		Filter filter = new Filter("instance-id").withValues(new String[]{
				inputProperties.getProperty("non-existent-instance-id")});
		
		EC2InstancesModel model = new EC2InstancesModel(getActivity(), connectionData, 
				connectionProperties.getProperty("region"));
		Object result = model.getInstances(filter);
		
		if (!(result instanceof ArrayList<?>)) {
			fail("Result should be of type arraylist");
		}
		else {
			try {
				instances = (ArrayList<SerializableInstance>) result;
			}
			catch(ClassCastException exception) {
				fail("Result should be of type ArrayList<SerializableInstance>");
			}
			
			if (instances.size() != 0) {
				fail(String.format("Expected ArrayList<String> of size 0; found size=%d. " +
						"Also check instance ID in input Properties file.", instances.size()));
			}
		}
	}
}
