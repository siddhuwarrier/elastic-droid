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
 * Authored by siddhu on 2 Jan 2011
 */
package org.elasticdroid.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import org.elasticdroid.utils.AWSConstants.EndPoints;

import android.util.Log;

/**
 * Utility class to hold input requried by the AWS CloudWatch API
 * @author siddhu
 *
 * 2 Jan 2011
 */
public class CloudWatchInput implements Serializable {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 133L;
	
	/** The start date for gettin monitoring data */
	private long startTime;
	/** The end date for gettin monitoring data */
	private long endTime;
	/** The interval between data points */
	private Integer period;
	/** the measure name: CPUUtilization etc */
	private String measureName;
	/** The namespace of the monitoring data. Only AWS/EC2 supported */
	private String namespace;
	/** the statistics type: Average, min, max etc */
	private ArrayList<String> statistics;
	/** The region. The class also contains a utlity method which gets the endpoint from the 
	 * region*/
	private String region;
	/** Logging tag */
	private final static String TAG = "org.elasticdroid.utils.CloudWatchInput";
	
	/**
	 * The CloudWatch input constructor, for all values.
	 * 
	 * @param startTime Start time (in milliseconds since epoch)
	 * @param endTime end time (in milliseconds since epoch)
	 * @param period interval between monitoring data points (in seconds)
	 * @param measureName The measure name. The valid measures for EC2 are:CPUUtilization,
	 * NetworkIn, NetworkOut, DiskWriteOps DiskReadBytes, DiskReadOps, DiskWriteBytes 
	 * @param namespace The only valid value for this ATM is "AWS/EC2".
	 * @param statistics The statistics to be returned for the given metric. Valid values are
	 * Average, Maximum, Minimum, Samples, Sum.
	 * @param region The AWS region. Valid values are eu-west-1, us-east-1, us-west-1, 
	 * ap-southeast-1.
	 * 
	 * However, the range of valid values is independent of ElasticDroid. i.e., will scale to 
	 * support new regions added by Amazon. 
	 */
	public CloudWatchInput(long startTime, long endTime, Integer period, String measureName,
			String namespace, ArrayList<String> statistics, String region) {
		
		this.startTime = startTime;
		this.endTime = endTime;
		this.period = period;
		this.measureName = measureName;
		this.namespace = namespace;
		this.statistics = statistics;
		this.region = region;
	}
	
	/**
	 * Get the start time for this {@link CloudWatchInput} instance.
	 * @return {@link CloudWatchInput#startTime}
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * Get the end time for this {@link CloudWatchInput} instance.
	 * @return {@link CloudWatchInput#endTime}
	 */
	public long getEndTime() {
		return endTime;
	}
	
	/**
	 * Get the measuring interval for this {@link CloudWatchInput} instance.
	 * @return {@link CloudWatchInput#period}
	 */
	public Integer getPeriod() {
		return period;
	}
	
	/**
	 * Get the measure name for this {@link CloudWatchInput} instance.
	 * @return {@link CloudWatchInput#measureName}
	 */
	public String getMeasureName() {
		return measureName;
	}
	
	/**
	 * Get the namespace name for this {@link CloudWatchInput} instance.
	 * @return {@link CloudWatchInput#namespace}
	 */
	public String getNamespace() {
		return namespace;
	}
	
	/**
	 * Get the stats required for this {@link CloudWatchInput} instance.
	 * @return {@link CloudWatchInput#statistics}
	 */
	public ArrayList<String> getStatistics() {
		return statistics;
	}
	
	/**
	 * Get the endpoint for the {@link CloudWatchInput#region} stored in this 
	 * {@link CloudWatchInput} instance.
	 * @return String from {@link AWSConstants}
	 */
	public String getEndpoint() {
		if (region.equals("eu-west-1")) {
			return EndPoints.CLOUDWATCH_EU_WEST;
		}
		else if (region.equals("us-east-1")) {
			return EndPoints.CLOUDWATCH_US_EAST;
		}
		else if (region.equals("us-west-1")) {
			return EndPoints.CLOUDWATCH_US_WEST;
		}		

		return EndPoints.CLOUDWATCH_APAC;
	}
	
	/**
	 * Get the region name for this {@link CloudWatchInput} instance.
	 * @return {@link CloudWatchInput#region}
	 */
	public String getRegion() {
		return region;
	}
	
	/**
	 * Set the start time for this {@link CloudWatchInput} instance.
	 * @param startTime
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * Set the end time for this {@link CloudWatchInput} instance.
	 * @param endTime
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
	/**
	 * Set the period for this {@link CloudWatchInput} instance.
	 * @param period
	 */
	public void setPeriod(Integer period) {
		this.period = period;
	}
	
	/**
	 * Set the end time for this {@link CloudWatchInput} instance.
	 * @param measureName
	 */
	public void setMeasureName(String measureName) {
		this.measureName = measureName;
	}
	
	/**
	 * Set the statistics for this {@link CloudWatchInput} instance.
	 * @param statistics
	 */
	public void setStatistics(ArrayList<String> statistics) {
		this.statistics = statistics;
	}
	
	//other methods not implemented as ElDroid does not support switching region in the
	//Cloudwatch view atm
}
