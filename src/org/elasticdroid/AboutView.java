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
package org.elasticdroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

/**
 * Class to display an about box.
 * @author Siddhu Warrier
 *
 * 9 Nov 2010
 */
public class AboutView extends Activity implements OnTouchListener{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		TextView aboutView = (TextView)findViewById(R.id.about_content);
		aboutView.setOnTouchListener(this);
		
		this.setTitle(this.getString(R.string.about_titletext) + " " + this.getString(R.string.app_version));
	}
	
	/**
	 * Close the activity on touch.
	 */
	@Override
	public boolean onTouch(View thisView, MotionEvent motionEvent) {
		Log.v(this.getClass().getName(), "TOUCH EVENT!");
		finish();
		return true;
	}
}
