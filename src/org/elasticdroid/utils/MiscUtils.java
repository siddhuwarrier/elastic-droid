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
 * Authored by siddhu on 19 Dec 2010
 */
package org.elasticdroid.utils;

import android.util.Log;

/**
 * Miscellaneous utils that won't fit anywhere else.
 * 
 * Contains:
 * <ul>
 * <li> Method to convert an Integer IP Address into a string.</li>
 * <li> Method to check whether a host IP address falls within the CIDR range specified.</li>
 * </ul>
 * @author siddhu
 *
 * 19 Dec 2010
 */
public class MiscUtils {
	/** Tag to attach to log messages */
	private static final String TAG = "org.elasticdroid.utils.MiscUtils";
	
	/**
	 * Checks if the IP address provided falls within the acceptable range for the source CIDR
	 * 
	 * This checks using some irritating bitwise arithmetic made more irritating by the absence of
	 * unsigned types in Java.
	 * 
	 * @param ipAddressValues String[4] containing each byte of the IP address
	 * @param rangeValues String[4] containing each byte of the source CIDR.
	 * @param cidr The number of bits to ignore (starting from LSB).
	 * @return true: if IP address falls within the acceptable range.
	 * @return false: if IP address is not within the acceptable range for the source CIDR.
	 */
	public static boolean checkIpPermissions(String[] ipAddressValues, String[] rangeValues, int cidr) {
		int byteCount;
		int rangeInt, ipAddressInt;
		int mask;
		
		for (byteCount = 0; byteCount < cidr/8; byteCount++) {
			Log.v(TAG + "checkIpPermissions()", "Full Comparison of byte " + 
					byteCount);
			//string comparisons work just as well here
			if (!rangeValues[byteCount].equals(ipAddressValues[byteCount])) {
				return false;
			}
		}
		
		//if the CIDR bits specify a partial byte to be checked as well
		if (cidr % 8 != 0) {
			rangeInt = Integer.valueOf(rangeValues[byteCount]);
			ipAddressInt = Integer.valueOf(ipAddressValues[byteCount]);
			
			//& by 255 because we want to set all bit above bit 8 to 0
			//this is cuz unsigned types don't exist.
			mask = (rangeInt << (cidr % 8)) & 255;
			//now shift to the right two bits so as to have restored everything
			//except the bits in this byte that we want to compare
			mask = mask >> (cidr % 8);
			rangeInt = rangeInt - mask;
			
			//do the same with IpAddress
			mask = (ipAddressInt << (cidr % 8)) & 255;
			mask = mask >> (cidr % 8);
			ipAddressInt = ipAddressInt - mask;
			
			if (rangeInt != ipAddressInt) {
				return false;
			}
		}
		
		Log.v(TAG + "getIpPermissions()", "Returning 0 now...");
		
		return true;
	}

}
