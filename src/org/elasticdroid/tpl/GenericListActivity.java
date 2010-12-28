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
 * Authored by Siddhu Warrier on 2 Nov 2010
 */
package org.elasticdroid.tpl;

import org.elasticdroid.R;
import org.elasticdroid.utils.DialogConstants;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;

/**
 * This is an Abstract class so that the Views can call the appropriate method
 * without having to know of the specific Activity class.
 * 
 * All classes inheriting from this *must* implement processModelResults. All the classes in 
 * ElasticDroid should inherit from this class. This is really syntactic sugar.
 * @author Siddhu Warrier
 *
 * 2 Nov 2010
 */
public abstract class GenericListActivity extends ListActivity implements OnCancelListener {

    /**Is progress bar displayed */
    protected boolean progressDialogDisplayed;
    
    /**Log TAG */
    private static final String TAG = "org.elasticdroid.tpl.GenericListActivity";
    
	/**
	 * Process results from model. Called by onPostExecute() method
	 * in any given Model class.
	 * @param result
	 */
	public abstract void processModelResults(Object result);
	
	/**
	 * Function that handles the display of a progress dialog. Overriden from
	 * Activity and not GenericActivity
	 * 
	 * @param id Dialog ID - Special treatment for Constants.PROGRESS_DIALOG
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(this.getString(R.string.wait_dlg));
			dialog.setCancelable(true);
			dialog.setOnCancelListener(this);
			
			return dialog;
		}
		// if some other sort of dialog...
		return super.onCreateDialog(id);
	}
	
	/**
	 * Called each time a dialog is created.
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle ignore) {
		if (id == DialogConstants.PROGRESS_DIALOG.ordinal()) {
			progressDialogDisplayed = true;
			Log.v(this.getClass().getName(), "Dialog prepped.progress dialog displayed=" + 
					progressDialogDisplayed);
		}
	}
}
