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
 * Authored by Siddhu Warrier on 5 Dec 2010
 */
package org.elasticdroid.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.GenericListActivity;
import org.elasticdroid.utils.AWSConstants.InstanceStateConstants;
import org.elasticdroid.utils.DialogConstants;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * @author Siddhu Warrier
 *
 * 5 Dec 2010
 */
public class EC2DisplayInstancesModel extends GenericListModel<HashMap<?,?>, 
Void, Object> {

	/**
	 * @param genericActivity The activity that clled the model
	 */
	public EC2DisplayInstancesModel(GenericListActivity genericActivity) {
		super(genericActivity);
	}
	
	/**
	 * Called in *UI Thread* before doInBackground executes in a separate thread.
	 */
	@Override
	protected void onPreExecute() {
		Log.v(this.getClass().getName(), "Display progress bar before starting up...");
		activity.showDialog(DialogConstants.PROGRESS_DIALOG.ordinal()); //the argument is not used
	}

	/** Execute the model activity in the background thread. Inherited from
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object doInBackground(HashMap<?, ?>... params) {
		HashMap<String, String> modelMap;
		ArrayList<SerializableInstance> instanceData = new ArrayList<SerializableInstance>();//data to return
		
		byte listType; //the list type- currently supported: RUNNING, STOPPED
		Filter regionFilter;
		Filter instanceFilter = new Filter(); //Amazon EC2 instance filter
		AmazonEC2Client amazonEC2Client;
		
		//we accept only one param, but AsyncTask forces us to potentially accept
		//a whole bloody lot of them. :P
		if (params.length != 1) {
			return new IllegalArgumentException("Only one Hashtable<String,String> parameter " +
					"should be passed.");
		}
		modelMap = (HashMap<String, String>)params[0]; //convenience variable, so that
		//i dont have to keep typing params[0] everywhere in this method.;)

		try {
			//create a client object
			amazonEC2Client = new AmazonEC2Client(new BasicAWSCredentials(
					modelMap.get("accessKey"), modelMap.get("secretAccessKey")));
			
			//get the region endpoint. To do this.
			//1. create a filter for this region name
			regionFilter = new Filter("region-name");
			regionFilter.setValues(new ArrayList<String>(Arrays.asList(
					new String[]{modelMap.get("region")})));
			//2. query using this filter
			List<Region> regions = amazonEC2Client.describeRegions(new DescribeRegionsRequest().
					withFilters(regionFilter)).getRegions();
			//3. Make sure the region was found.
			if (regions.size() != 1) {
				return new IllegalArgumentException("Invalid region passed to model.");
			}
			
			Log.v(this.getClass().getName() + "doInBackground()", "endpoint for region : " + 
					modelMap.get("region") + "=" + regions.get(0).getEndpoint());
			//set the endpoint
			amazonEC2Client.setEndpoint(regions.get(0).getEndpoint());
			
			listType = Byte.valueOf(modelMap.get("listType")); //get the list type
			
			//if it is a running instance, set the filter accordingly
			if (listType == InstanceStateConstants.RUNNING) {				
				instanceFilter.setName("instance-state-name");
				instanceFilter.setValues(new ArrayList<String>(Arrays.asList(new String[]{"running"})));
			}
			else if (listType == InstanceStateConstants.STOPPED) {
				instanceFilter.setName("instance-state-name");
				instanceFilter.setValues(new ArrayList<String>(Arrays.asList(new String[]{"stopped"})));
			}
			
			//get the list of instances using this filter
			List<Reservation> reservations = amazonEC2Client.
				describeInstances(new DescribeInstancesRequest().withFilters(instanceFilter)).
				getReservations();
			
			//add each instance found into the list of instances to return to the view
			for (Reservation reservation: reservations) {
				//for each reservation, get the list of instances associated
				for (Instance instance: reservation.getInstances()) {
					instanceData.add(new SerializableInstance(instance));
				}
			}
		}
		catch(AmazonServiceException amazonServiceException) {
			return amazonServiceException;
		}
		catch(AmazonClientException amazonClientException) {
			return amazonClientException;
		}
		
		return instanceData;
	}
}
