package org.elasticdroid.intf.callback;

/**
 * Generic interface with methods that should be implemented by all callback objects.
 * 
 * This interface is presently empty.
 * 
 * @author siddhuwarrier
 *
 */
public interface GenericCallback {
	/**
	 * Cleans up task after execution. Typically sets task object to null
	 */
	public void cleanUpAfterTaskExecution();
}
