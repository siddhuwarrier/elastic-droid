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
 * Authored by Siddhu Warrier on 17 Nov 2010
 */
package org.elasticdroid;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Simple class to display list of regions, and allow user to pick
 * a region, any region. This will be set as the default region for the user.
 * @author Siddhu Warrier
 *
 * 17 Nov 2010
 */
public class RegionsView extends ListActivity {

	private ArrayList<String> regionData;
	/**
	 * Called when activity is first displayed.
	 * Should receive list of regions as Intent.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); //call superclass onCreate
		
    	//this class is called from any dashboard, and is passed an argument in the intent.
    	Intent intent = this.getIntent();
    	try {
    		this.regionData = intent.getStringArrayListExtra(
    				"regionData");
    	}
    	//the possible exceptions are NullPointerException: the ArrayList was not found.
    	catch(NullPointerException exception) {
    		Log.e(this.getClass().getName(), exception.getMessage());
    		finish(); //kill the application, and off to bed.
    	}
    	
    	for (String region: regionData) {
    		Log.v(this.getClass().getName(), "Region:" + region);
    	}
	}
}
