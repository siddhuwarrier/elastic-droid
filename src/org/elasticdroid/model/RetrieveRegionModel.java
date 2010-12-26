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

import java.util.HashMap;
import java.util.List;

import org.elasticdroid.model.tpl.GenericListModel;
import org.elasticdroid.tpl.GenericListActivity;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Region;

/**
 * @author Siddhu Warrier
 *
 * 8 Dec 2010
 */
public class RetrieveRegionModel extends GenericListModel<HashMap<?,?>, Void, Object> {

	/**
	 * @param genericActivity
	 */
	public RetrieveRegionModel(GenericListActivity genericActivity) {
		super(genericActivity);
		// TODO Auto-generated constructor stub
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object doInBackground(HashMap<?,?>... params) {
		HashMap<String, String> connectionData;
		HashMap<String, String> regionData = new HashMap<String, String>();
		
		List<Region> regions;//data from AWS.
		
		//we accept only one param, but AsyncTask forces us to potentially accept
		//a whole bloody lot of them. :P
		if (params.length != 1) {
			return new IllegalArgumentException("Only one Hashtable<String,String> parameter " +
					"should be passed.");
		}
		
		connectionData = (HashMap<String, String>)params[0]; //convenience variable, so that
		//i dont have to keep typing params[0] everywhere in this method.;)
		
		Log.v(this.getClass().getName(), "Getting EC2 region data...");
		
		//prepare to get region data
		//create credentials using the BasicAWSCredentials class
		BasicAWSCredentials credentials = new BasicAWSCredentials(connectionData.get("accessKey"),
				connectionData.get("secretAccessKey"));
		//create Amazon EC2 Client object, and set tye end point to the region. params[3]
		//contains endpoint
		AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
		try {
			regions = amazonEC2Client.describeRegions().getRegions();
		}
		catch(AmazonServiceException amazonServiceException) {
			//this is an unchecked exception subclassed from RuntimeException. So throw it manually
			Log.v(this.getClass().getName(), "Caught ServiceException.");
			return amazonServiceException;
		}
		catch (AmazonClientException amazonClientException) {
			//this is an unchecked exception subclassed from RuntimeException. So throw it manually
			Log.v(this.getClass().getName(), "Caught ClientException.");
			return amazonClientException;
		}
		
		if (regions.size() == 0) {
			return new IllegalArgumentException("No regions found");
		}
		
		//populate the region data with regionName: regionEndPoint
		for (Region region : regions) {
			regionData.put(region.getRegionName(), region.getEndpoint());
			Log.v("AWSUtilities.getRegions", region.getRegionName());
		}
		
		return regionData;
	}
}
