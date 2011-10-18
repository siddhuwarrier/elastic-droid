package org.elasticdroid.stages.login;

import org.elasticdroid.intf.AWSLogin;
import org.elasticdroid.model.LoginModel;
import org.elasticdroid.model.LoginModel.LoginResult;
import org.elasticdroid.stub.AWSLoginStub;
import org.elasticdroid.tests.model.InputUserData;

import android.test.AndroidTestCase;

/**
 * Note: The absence of a decent mocking framework results in me 
 * having to do these nasty things.
 * 
 * No, android-mock does not count.
 * 
 * @author siddhu
 *
 */
public class AWSLoginStageTest extends AndroidTestCase {

	/**
	 * Unit test 1:
	 * AWS Login Stage is successful; i.e. returns AUTH_SUCCESS
	 */
	public void testLoginSuccessful() {
		AWSLoginStage loginStage = new AWSLoginStage();
		loginStage.setAWSLogin(new AWSLoginStub());
		
		LoginModel loginModel = new LoginModel(InputUserData.exampleUser);
		loginStage.execute(loginModel);
		
		assertEquals(LoginResult.AUTH_SUCCESS, loginModel.getLoginResult());
	}
	
	/**
	 * Unit test 2:
	 * AWS Login Stage is unsuccessful; i.e. returns AUTH_FAILURE
	 */
	public void testLoginAuthFail() {
		AWSLoginStage loginStage = new AWSLoginStage();
		AWSLogin awsLogin = new AWSLoginStub(LoginResult.AUTH_FAIL);
		loginStage.setAWSLogin(awsLogin);
		
		LoginModel loginModel = new LoginModel(InputUserData.exampleUser);
		loginStage.execute(loginModel);
		
		assertEquals(LoginResult.AUTH_FAIL, loginModel.getLoginResult());
	}
	
	/**
	 * Unit test 4:
	 * AWS Login Stage is unsuccessful due to client (net conn) problems;
	 * returns AWS_CLIENT_ERROR
	 */	
	public void testLoginClientError() {
		AWSLoginStage loginStage = new AWSLoginStage();
		AWSLogin awsLogin = new AWSLoginStub(LoginResult.AWS_CLIENT_ERROR);
		loginStage.setAWSLogin(awsLogin);
		
		LoginModel loginModel = new LoginModel(InputUserData.exampleUser);
		loginStage.execute(loginModel);
		
		assertEquals(LoginResult.AWS_CLIENT_ERROR, loginModel.getLoginResult());
	}
}
