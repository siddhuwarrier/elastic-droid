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
import java.util.HashMap;
import java.util.Properties;

import org.apache.http.ConnectionClosedException;
import org.elasticdroid.model.SshConnectorModel;
import org.elasticdroid.testharness.TestListActivity;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

/**
 * @author siddhu
 *
 * 15 Dec 2010
 */
public class SSHConnectorModelTest extends ActivityInstrumentationTestCase2<TestListActivity> {
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
	 * Test output properties
	 */
	private Properties outputProperties;
	
	/**
	 * @param activityClass
	 * @throws IOException 
	 */
	public SSHConnectorModelTest() {
		super("org.elasticdroid.testharness", TestListActivity.class);
	}
	
	public void setUp() {
		connectionProperties = new Properties();
		inputProperties = new Properties();
		outputProperties = new Properties();
		
		InputStream connectionPropStream, inputPropStream, expOutPropStream;
		//read input and expected output
		try {
			connectionPropStream = getInstrumentation().getContext().getResources().getAssets().
				open("ec2_connection_data.properties");
			inputPropStream = getInstrumentation().getContext().getResources().getAssets().open(
					"ssh_connector_input.properties");
			expOutPropStream = getInstrumentation().getContext().getResources().
			getAssets().open("ssh_connector_output.properties");
			
			connectionProperties.load(connectionPropStream);
			inputProperties.load(inputPropStream);
			outputProperties.load(expOutPropStream);
		} catch (IOException e) {
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
	 * SUCCESS TEST: Test if SSHConnector returns a URI when the machine has port 22 open
	 * to this device's IP.
	 * 
	 * Expected return value: String
	 */
	public void testPort22Open() {
		SshConnectorModel sshConnectorModel = new SshConnectorModel(getActivity(),
				connectionData,
				inputProperties.getProperty("username"),
				inputProperties.getProperty("hostname")
		);
		
		Object result = sshConnectorModel.prepareSshUri(new String[]{inputProperties.getProperty(
				"testPort22Open.securityGroups")});
		
		if (!(result instanceof String)) {
			fail("Expected result of type string, as port 22 is open.");
		}
	}
	
	/**
	 * SUCCESS TEST: Test if SSHConnector returns a URI when the machine has another port (not 22) 
	 * open to this device's IP.
	 * 
	 * Expected return value: String
	 */
	public void testSpecifiedPortOpen() {
		SshConnectorModel sshConnectorModel = new SshConnectorModel(getActivity(),
				connectionData,
				inputProperties.getProperty("username"),
				inputProperties.getProperty("hostname"),
				Integer.valueOf(inputProperties.getProperty("testSpecifiedPortOpen.toPort"))
		);
		
		Object result = sshConnectorModel.prepareSshUri(new String[]{inputProperties.getProperty(
		"testSpecifiedPortOpen.securityGroups")});
		
		if (!(result instanceof String)) {
			fail("Expected result of type string, as port is open.");
		}
	}
	
	/**
	 * FAILURE: Test if SSHConnector returns a URI when the selected port is not open
	 * 
	 * Expected return value: ConnectionClosedException
	 */
	public void testSpecifiedPortClosed() {
		SshConnectorModel sshConnectorModel = new SshConnectorModel(getActivity(),
				connectionData,
				inputProperties.getProperty("username"),
				inputProperties.getProperty("hostname"),
				Integer.valueOf(inputProperties.getProperty("testSpecifiedPortClosed.toPort"))
		);
		
		Object result = sshConnectorModel.prepareSshUri(new String[]{inputProperties.getProperty(
		"testSpecifiedPortClosed.securityGroups")});
		
		if (!(result instanceof ConnectionClosedException)) {
			fail("Expected result of type string, as port is open.");
		} 
		else
		{
			ConnectionClosedException exception = (ConnectionClosedException) result;
			
			assertEquals("This port is not open", 
					exception.getMessage());
		}
	}
	
	/**
	 * FAILURE: Test if SSHConnector returns a URI when the selected IP address range is not open.
	 * 
	 * Expected return value: ConnectionClosedException
	 */
	public void testDeviceIpBlocked() {
		SshConnectorModel sshConnectorModel = new SshConnectorModel(getActivity(),
				connectionData,
				inputProperties.getProperty("username"),
				inputProperties.getProperty("hostname"),
				Integer.valueOf(inputProperties.getProperty("testDeviceIpBlocked.toPort"))
		);
		
		Object result = sshConnectorModel.prepareSshUri(new String[]{inputProperties.getProperty(
		"testDeviceIpBlocked.securityGroups")});
		
		if (!(result instanceof ConnectionClosedException)) {
			fail("Expected result of type ConnectionClosedException.");
		} 
		else
		{
			ConnectionClosedException exception = (ConnectionClosedException) result;
			
			assertEquals("Your IP address is not allowed to access this port", 
					exception.getMessage());
		}
	}
}
