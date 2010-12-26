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
 * Authored by siddhu on 15 Dec 2010
 */
package org.elasticdroid.testharness;

import org.elasticdroid.tpl.GenericListActivity;

/**
 * A test list activity that expects no intent, to test 
 * Models.
 * 
 * @author siddhu
 *
 * 15 Dec 2010
 */
public class TestListActivity extends GenericListActivity {

	/* (non-Javadoc)
	 * @see org.elasticdroid.GenericActivity#processModelResults(java.lang.Object)
	 */
	@Override
	public void processModelResults(Object result) {
		// TODO Auto-generated method stub

	}

}
