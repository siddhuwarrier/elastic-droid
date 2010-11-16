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
 * Authored by Siddhu Warrier on 15 Nov 2010
 */
package org.elasticdroid.model;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.elasticdroid.db.ElasticDroidDB;

import android.app.Activity;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Region;

/**
 * Class with static methods to get bits of information from AWS that are necessary to populate 
 * the UIs, such as, for instance, the number of regions, and the default region.
 * 
 * @author Siddhu Warrier
 *
 * 15 Nov 2010
 */
public class AWSUtilities {

	/**
	 * Get number of regions, and user's default region
	 * @param connectionData
	 * @return
	 * @throws SQLException 
	 */
	public static HashMap<String,String> getRegions(HashMap<String,String> 
		connectionData) throws RuntimeException {
		//hashtable of region endpoints keyed by region name
		HashMap<String, String> regionData = new HashMap<String, String>(); 
		
		AmazonEC2Client amazonEC2Client = new AmazonEC2Client(new BasicAWSCredentials(
				connectionData.get("accessKey"), connectionData.get("secretAccessKey")));
		List<Region> regions = amazonEC2Client.describeRegions().getRegions();
		
		if (regions.size() == 0) {
			throw new RuntimeException("No regions found");
		}
		//populate the region data with regionName: regionEndPoint
		for (Region region : regions) {
			regionData.put(region.getRegionName(), region.getEndpoint());
			Log.v("AWSUtilities.getRegions", region.getRegionName());
		}
		
		return regionData;
	}
}
