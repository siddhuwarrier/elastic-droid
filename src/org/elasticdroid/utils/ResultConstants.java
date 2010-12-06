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
 * Authored by Siddhu Warrier on 9 Nov 2010
 */
package org.elasticdroid.utils;

/**
 * Constants to indicate results of subactivity called 
 * using startActivityForResult
 * 
 * @author Siddhu Warrier
 *
 * 9 Nov 2010
 */
public class ResultConstants {
	/** Returned by {@link org.elasticdroid.UserPickerView} to {@link org.elasticdroid.LoginView}*/ 
	public static final byte RESULT_NEW_USER = 2;
	public static final byte RESULT_ERROR = 3;
	//note: RESULT_OK = -1, RESULT_CANCELED = 0, RESULT_FIRST_USER = 1
	//so custom user results should start from 2.
}
