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
 * Authored by Siddhu Warrier on 14 Nov 2010
 */
package org.elasticdroid.utils;

import com.amazonaws.services.ec2.model.InstanceState;

/**
 * AWS constants. Stored in a separate file. 
 * @author Siddhu Warrier
 *
 * 14 Nov 2010
 */
public class AWSConstants {

	//endpoints
	/**
	 * AWS Endpoints. Add/change here if AWS changes/adds endpoints.
	 * @author Siddhu Warrier
	 */
	public class EndPoints {
		/** 
		 * End point for EC2 in the US East region
		 */
		public static final String EC2_US_EAST="ec2.us-east-1.amazonaws.com";
		/** 
		 * End point for EC2 in the US West region
		 */
		public static final String EC2_US_WEST="ec2.us-west-1.amazonaws.com";
		/** 
		 * End point for EC2 in the EU West region
		 */
		public static final String EC2_EU_WEST ="ec2.eu-west-1.amazonaws.com";
		/** 
		 * End point for EC2 in the APAC region
		 */
		public static final String EC2_APAC ="ec2.ap-southeast-1.amazonaws.com";
	}
	
	/**
	 * Constants holding information on instance state
	 * 
	 * Instance state is returned by {@link com.amazonaws.services.ec2.model.InstanceState} as a 16
	 * bit integer, the higher byte of which is used internally.
	 * 
	 *  We are concerned only of the lower byte value. Annoyingly, Amazon doesn't seem to have the same thing.
	 *  Or even all of the values required. Grrr!
	 * @author Siddhu Warrier
	 *
	 * 14 Nov 2010
	 */
	public class InstanceStateConstants {
		/**
		 * Instance is running.
		 */
		public static final byte RUNNING = 16;
		/**
		 * Instance is stopped.
		 */
		public static final byte STOPPED = 80;
	}
}
