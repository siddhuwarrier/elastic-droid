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
 * Authored by siddhu on 3 Jan 2011
 */
package org.elasticdroid.utils;

import org.elasticdroid.R;
import org.elasticdroid.tpl.GenericActivity;


/**
 * Enumeration defining acceptable durations
 * 
 * TODO Replace with user-defined datepickers
 *  
 * @author siddhu
 *
 * 3 Jan 2011
 */
public enum MonitoringDurations {
	LAST_HOUR(3600000, 0),
	LAST_SIX_HOURS(21600000, 1),
	LAST_TWELVE_HOURS(43200000, 2),
	LAST_DAY(86400000, 3);
	
	/**
	 * Constructor for this enumerator
	 * 
	 * @param durationTime The time to subtract from "now"
	 */
	private MonitoringDurations(long durationTime, int pos) {
		this.durationTime = durationTime;
		this.pos = pos;
	}
	
	/**
	 * @return {@link MonitoringDurations#durationTime}.
	 */
	public final long getDuration() {
		return durationTime;
	}
	
	/**
	 * Get the string representation of the duration in question.
	 * @param activity The activity to use to retrieve the string info
	 * @return The string in the same position in the Strings.xml file.
	 * 
	 * Sorry this looks really bad!
	 */
	public String getString(GenericActivity activity) {
		return activity.getResources().getStringArray(R.array.monitorinstanceview_duration_array)
			[pos];
	}
	
	/**
	 * Get the position
	 */
	public final int getPos() {
		return pos;
	}
	
	/**
	 * Time to subtract from "now" in milliseconds
	 */
	private final long durationTime;
	/**
	 * The position in the String array in strings.xml. Hard-coded
	 */
	private final int pos;
}
