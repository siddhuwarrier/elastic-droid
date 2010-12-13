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
 * Authored by Siddhu Warrier on 3 Nov 2010
 */
package org.elasticdroid.model;

import org.elasticdroid.GenericActivity;
import org.elasticdroid.utils.DialogConstants;

import android.os.AsyncTask;

/**
 * Abstract class to add a few additional must-implement methods to AsyncTask.
 * 
 * This stays abstract so that I don't have to implement ASyncTask methods here.
 * @author Siddhu Warrier
 *
 * 3 Nov 2010
 */
public abstract class GenericModel<T,U,V> extends AsyncTask<T, U, V> {
	protected GenericActivity activity;
	
	/**
	 * Constructor. Saves the activity that called this. This is used to return the data
	 * back to the (Generic)Activity.
	 * @param activity The Android UI activity that created LoginModel
	 */
	public GenericModel(GenericActivity activity) {
		this.activity = activity;
	}
	
	/**
	 * Called in *UI Thread* before doInBackground executes in a separate thread.
	 */
	@Override
	protected void onPreExecute() {
		activity.showDialog(DialogConstants.PROGRESS_DIALOG.ordinal()); //the argument is not used
	}
	
	/**
	 * Set the activity object referred to by the model. This is used
	 * by the activity to reset itself to null when it is being destroyed temporarily
	 * (for instance whenever the screen orientation is changed), and to
	 * reset it whenever the object is restored after being destroyed.
	 * @param activity the GenericActivity referred to in the Model 
	 */
	public void setActivity(GenericActivity activity) {
		this.activity = activity;
	}
	
	/**
	 * Called in the *UI thread* after doInBackground completes.
	 * 
	 * @param result The results returned by doInBackground
	 */
	@Override
	protected void onPostExecute(Object result) {
		//just return the result produced to the Activity.
		//we could process it here, but I want to keep the MVC pattern clean.
		//Call me a f*cking pedant, if you will.
		//...
		//...
		//I HEARD THAT!!!
		if (activity != null) {
			activity.processModelResults(result);
		}
	}
}
