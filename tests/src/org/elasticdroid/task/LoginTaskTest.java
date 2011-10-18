package org.elasticdroid.task;

import org.elasticdroid.intf.callback.LoginTaskCallback;
import org.elasticdroid.model.LoginModel.LoginResult;
import org.elasticdroid.stages.login.AWSLoginStage;
import org.elasticdroid.stub.AWSLoginStub;
import org.elasticdroid.tests.model.InputUserData;

import android.test.AndroidTestCase;

import com.ideaimpl.patterns.pipeline.Pipeline;
import com.ideaimpl.patterns.pipeline.SequentialPipeline;

public class LoginTaskTest extends AndroidTestCase {
	
	/**
	 * Test #1: Login returns successfully; 
	 * AUTH_SUCCESS
	 * @throws Throwable 
	 */
	public void testLoginSuccess() throws Throwable {
		LoginTask loginTask = new LoginTask(
				InputUserData.exampleUser, 
				getContext(), 
				new LoginCallbackImplStub());
		
		
		loginTask.setPipeline(createStubbedPipeline(LoginResult.AUTH_SUCCESS));
		
		loginTask.execute();
	    loginTask.get(); //wait for login task to finish executing
	    assertEquals(
	    		LoginResult.AUTH_SUCCESS, 
	    		loginTask.getResults().getLoginResult());
	}
	
	/**
	 * Test #2: Login has an auth fail. 
	 * AUTH_FAIL
	 */
	public void testLoginAuthFail() throws Throwable {
		final LoginTask loginTask = new LoginTask(
				InputUserData.exampleUser, 
				getContext(), 
				new LoginCallbackImplStub());
		
		loginTask.setPipeline(createStubbedPipeline(LoginResult.AUTH_FAIL));
		
		loginTask.execute();
	    loginTask.get(); //wait for login task to finish executing
	    assertEquals(
	    		LoginResult.AUTH_FAIL, 
	    		loginTask.getResults().getLoginResult());
	}
	
	private Pipeline createStubbedPipeline(LoginResult loginResult) {
		Pipeline stubbedPipeline = new SequentialPipeline();
		AWSLoginStage awsLoginStage = new AWSLoginStage();
		awsLoginStage.setAWSLogin(new AWSLoginStub(loginResult));
		stubbedPipeline.addStage(awsLoginStage);
		
		return stubbedPipeline;
	}
	
	public class LoginCallbackImplStub implements LoginTaskCallback {

		@Override
		public void cleanUpAfterTaskExecution() {	
		}

		@Override
		public void loginResult(LoginResult loginResult) {
		}
	}
}
