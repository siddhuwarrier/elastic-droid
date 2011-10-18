package org.elasticdroid;

import org.elasticdroid.model.LoginModel.LoginResult;
import org.elasticdroid.stages.login.AWSLoginStage;
import org.elasticdroid.stub.AWSLoginStub;
import org.elasticdroid.stub.ConnectivityCheckerStub;
import org.elasticdroid.task.LoginTask;
import org.elasticdroid.tests.model.InputUserData;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.ideaimpl.patterns.pipeline.Pipeline;
import com.ideaimpl.patterns.pipeline.SequentialPipeline;
import com.jayway.android.robotium.solo.Solo;

public class LoginViewTest extends ActivityInstrumentationTestCase2<LoginView>{
	/** Robotium test object */
	private Solo solo;
	
	private static final String TAG = LoginViewTest.class.getName();
	
	private LoginTask loginTask;
	
	private static final int ACTIVITY_WAIT_TIME = 1000;
	
	public LoginViewTest() {
        super("org.elasticdroid", LoginView.class); 
    }
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		solo = new Solo(getInstrumentation(), getActivity());
	}
	
	/**
	 * Scenario 1: Successful login
	 * Given I am in the login screen,
	 * 
	 * When I enter valid AWS account credentials,
	 * 
	 * Then I should be presented with the EC2 Dashboard view
	 * @throws Throwable 
	 */
	public void testSuccessfulLogin() throws Throwable {
		givenLoginView();
		
		whenValidLoginEntered();
		
		//perform test
		assertTrue(solo.waitForActivity(
				"DashboardView", 
				ACTIVITY_WAIT_TIME));
	}
	
	/**
	 * Scenario 2:
	 * 
	 * Given I am in the login screen,
	 * 
	 * When I enter invalid AWS account credentials,
	 * 
	 * Then I should be presented with a login error.
	 * @throws Throwable 
	 */
	public void testInvalidAWSCredentials() throws Throwable {
		givenLoginView();
		
		whenInvalidLoginEntered();
		
		//Wait for a bit. Not possible to search for error messages uisng 
		//Robotium.:-( This sleep method call makes me sick!
		solo.sleep(ACTIVITY_WAIT_TIME);
		
		//check error text
		assertEquals(
				getActivity().getString(R.string.invalid_credentials),
				((EditText)solo.getView(R.id.awsAccountEntry)).
					getError().toString());
		assertEquals(
				getActivity().getString(R.string.invalid_credentials),
				((EditText)solo.getView(R.id.awsAccessKey)).
					getError().toString());
		assertEquals(
				getActivity().getString(R.string.invalid_credentials),
				((EditText)solo.getView(R.id.awsSecretAccessKey)).
					getError().toString());
	}
	
	/**
	 * Scenario 3:
	 * 
	 * Given I am in the login screen,
	 * 
	 * When I do not enter a valid AWS account email
	 * 
	 * Then the application should tell me that my email is invalid.
	 * 
	 */
	public void testInvalidAWSEmail() throws Throwable {
		givenLoginView();
		
		whenInvalidEmailEntered();
		
		solo.sleep(ACTIVITY_WAIT_TIME);
		assertEquals(
				getActivity().getString(R.string.invalid_or_empty_data),
				((EditText)solo.getView(R.id.awsAccountEntry)).
					getError().toString());
	}
	
	/**
	 * Scenario 4:
	 * 
	 * Given I am in the login screen,
	 * 
	 * When I leave the AWS Access Key blank
	 * 
	 * Then the application should tell me I need to fill in all three
	 * fields correctly
	 */
	public void testEmptyAccessKey() throws Throwable {
		givenLoginView();
		
		whenAccessKeyNotEntered();
		
		solo.sleep(ACTIVITY_WAIT_TIME);
		assertEquals(
				getActivity().getString(R.string.invalid_or_empty_data),
				((EditText)solo.getView(R.id.awsAccessKey)).
					getError().toString());
	}
	
	/**
	 * Scenario 5:
	 * 
	 * Given I am in the login screen,
	 * 
	 * When I leave the AWS Secret Access Key field blank
	 * 
	 * Then the application should tell me I need to fill in all three
	 * fields correctly
	 */
	public void testEmptySecretAccessKey() throws Throwable {
		givenLoginView();
		
		whenSecretAccessKeyNotEntered();
		
		solo.sleep(ACTIVITY_WAIT_TIME);
		assertEquals(
				getActivity().getString(R.string.invalid_or_empty_data),
				((EditText)solo.getView(R.id.awsSecretAccessKey)).
					getError().toString());
	}
	
	/**
	 * Scenario 6:
	 * 
	 * Given I am in the login screen,
	 * 
	 * When I leave the AWS Email Address blank
	 * 
	 * Then the application should tell me I need to fill in all three
	 * fields correctly
	 */
	public void testEmptyEmail() throws Throwable {
		givenLoginView();
		
		whenAwsAccountNotEntered();
		
		solo.sleep(ACTIVITY_WAIT_TIME);
		assertEquals(
				getActivity().getString(R.string.invalid_or_empty_data),
				((EditText)solo.getView(R.id.awsAccountEntry)).
					getError().toString());
	}
	
	/**
	 * Scenario 7: Given I am in the login screen with no internet access
	 * 
	 * When I enter my AWS login details,
	 * 
	 * Then the application should tell me
	 * @throws Throwable
	 */
	public void testNoConnectivity() throws Throwable {
		givenLoginView();
		whenValidLoginEnteredAndNoConnectivity();
		
		solo.sleep(ACTIVITY_WAIT_TIME);
		assertEquals(
				getActivity().getString(R.string.no_connxn),
				((EditText)solo.getView(R.id.awsAccountEntry)).
					getError().toString());
	}
	
	private void givenLoginView() {
		//nothing to do here. Yet.
	}
	
	private void whenValidLoginEntered() throws Throwable {
		EditText awsAccount = (EditText) (getActivity().findViewById(
				R.id.awsAccountEntry));
		EditText awsAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsAccessKey));
		EditText awsSecretAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsSecretAccessKey));
		
		solo.enterText(awsAccount, InputUserData.exampleUser.getAwsAccount());
		solo.enterText(
				awsAccessKey, 
				InputUserData.exampleUser.getAwsAccessKey());
		solo.enterText(
				awsSecretAccessKey, 
				InputUserData.exampleUser.getAwsSecretAccessKey());
		
		Log.d(TAG, "Clicking on the button...");
		
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				setLoginTaskForTest(LoginResult.AUTH_SUCCESS);
				setConnectivityCheckerForTest(true);//test not reliant on conn
				
				Button loginButton = (Button) getActivity().
						findViewById(R.id.loginButton);
				loginButton.performClick();
			}
		});
		
		loginTask.get(); //wait!
	}
	
	private void whenInvalidLoginEntered() throws Throwable {
		EditText awsAccount = (EditText) (getActivity().findViewById(
				R.id.awsAccountEntry));
		EditText awsAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsAccessKey));
		EditText awsSecretAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsSecretAccessKey));
		
		solo.enterText(awsAccount, InputUserData.exampleUser.getAwsAccount());
		solo.enterText(
				awsAccessKey, 
				InputUserData.exampleUser.getAwsAccessKey());
		solo.enterText(
				awsSecretAccessKey, 
				InputUserData.exampleUser.getAwsSecretAccessKey());
		
		Log.d(TAG, "Clicking on the button...");
		
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				setLoginTaskForTest(LoginResult.AUTH_FAIL);
				setConnectivityCheckerForTest(true);//test not reliant on conn
				
				Button loginButton = (Button) getActivity().
						findViewById(R.id.loginButton);
				loginButton.performClick();
			}
		});
		
	}
	
	private void whenInvalidEmailEntered() throws Throwable {
		EditText awsAccount = (EditText) (getActivity().findViewById(
				R.id.awsAccountEntry));
		EditText awsAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsAccessKey));
		EditText awsSecretAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsSecretAccessKey));
		
		solo.enterText(awsAccount, InputUserData.invalidEmailUser.getAwsAccount());
		solo.enterText(
				awsAccessKey, 
				InputUserData.invalidEmailUser.getAwsAccessKey());
		solo.enterText(
				awsSecretAccessKey, 
				InputUserData.invalidEmailUser.getAwsSecretAccessKey());
		
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				Button loginButton = (Button) getActivity().
						findViewById(R.id.loginButton);
				loginButton.performClick();
			}
		});
	}
	
	private void whenAccessKeyNotEntered() throws Throwable {
		EditText awsAccount = (EditText) (getActivity().findViewById(
				R.id.awsAccountEntry));
		EditText awsAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsAccessKey));
		EditText awsSecretAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsSecretAccessKey));
		
		solo.enterText(awsAccount, InputUserData.emptyAccessKeyUser.getAwsAccount());
		solo.enterText(
				awsAccessKey, 
				InputUserData.emptyAccessKeyUser.getAwsAccessKey());
		solo.enterText(
				awsSecretAccessKey, 
				InputUserData.emptyAccessKeyUser.getAwsSecretAccessKey());
		
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				Button loginButton = (Button) getActivity().
						findViewById(R.id.loginButton);
				loginButton.performClick();
			}
		});
	}
	
	private void whenSecretAccessKeyNotEntered() throws Throwable {
		EditText awsAccount = (EditText) (getActivity().findViewById(
				R.id.awsAccountEntry));
		EditText awsAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsAccessKey));
		EditText awsSecretAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsSecretAccessKey));
		
		solo.enterText(awsAccount, InputUserData.emptySecretAccessKeyUser.
				getAwsAccount());
		solo.enterText(
				awsAccessKey, 
				InputUserData.emptySecretAccessKeyUser.
				getAwsAccessKey());
		solo.enterText(
				awsSecretAccessKey, 
				InputUserData.emptySecretAccessKeyUser.
				getAwsSecretAccessKey());
		
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				Button loginButton = (Button) getActivity().
						findViewById(R.id.loginButton);
				loginButton.performClick();
			}
		});
	}
	
	private void whenAwsAccountNotEntered() throws Throwable {
		EditText awsAccount = (EditText) (getActivity().findViewById(
				R.id.awsAccountEntry));
		EditText awsAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsAccessKey));
		EditText awsSecretAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsSecretAccessKey));
		
		solo.enterText(awsAccount, InputUserData.emptySecretAccessKeyUser.
				getAwsAccount());
		solo.enterText(
				awsAccessKey, 
				InputUserData.emptySecretAccessKeyUser.
				getAwsAccessKey());
		solo.enterText(
				awsSecretAccessKey, 
				InputUserData.emptySecretAccessKeyUser.
				getAwsSecretAccessKey());
		
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				Button loginButton = (Button) getActivity().
						findViewById(R.id.loginButton);
				loginButton.performClick();
			}
		});
	}
	
	private void whenValidLoginEnteredAndNoConnectivity() throws Throwable {
		EditText awsAccount = (EditText) (getActivity().findViewById(
				R.id.awsAccountEntry));
		EditText awsAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsAccessKey));
		EditText awsSecretAccessKey = (EditText) (getActivity().findViewById(
				R.id.awsSecretAccessKey));
		
		solo.enterText(awsAccount, InputUserData.exampleUser.getAwsAccount());
		solo.enterText(
				awsAccessKey, 
				InputUserData.exampleUser.getAwsAccessKey());
		solo.enterText(
				awsSecretAccessKey, 
				InputUserData.exampleUser.getAwsSecretAccessKey());
		
		Log.d(TAG, "Clicking on the button...");
		
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				setConnectivityCheckerForTest(false);//test not reliant on conn
				Button loginButton = (Button) getActivity().
						findViewById(R.id.loginButton);
				loginButton.performClick();
			}
		});
	}

	/*
	 * Set the connectivity checker to a stub
	 */
	private void setConnectivityCheckerForTest(boolean isConnected) {
		LoginFragment loginFragment = (LoginFragment) getActivity().
				getSupportFragmentManager().
				findFragmentById(R.id.loginfragment);
		
		//create a connectivity checker stub
		ConnectivityCheckerStub connChecker = new ConnectivityCheckerStub();
		connChecker.setConnected(isConnected);
		
		loginFragment.setConnectivityChecker(connChecker);
	}
	
	/*
	 * Creates a login task which returns the values we want.
	 */
	private void setLoginTaskForTest(LoginResult loginResult) {
		
		LoginFragment loginFragment = (LoginFragment) getActivity().
				getSupportFragmentManager().
				findFragmentById(R.id.loginfragment);
		
		loginTask = new LoginTask(
				InputUserData.exampleUser, 
				getActivity(), 
				loginFragment.new LoginCallbackImpl());
		loginTask.setPipeline(createStubbedPipeline(loginResult));
		
		loginFragment.setLoginTask(loginTask);
	}
	
	/*
	 * Create a stubbed pipeline for the login task.
	 */
	private Pipeline createStubbedPipeline(LoginResult loginResult) {
		Pipeline stubbedPipeline = new SequentialPipeline();
		AWSLoginStage awsLoginStage = new AWSLoginStage();
		awsLoginStage.setAWSLogin(new AWSLoginStub(loginResult));
		stubbedPipeline.addStage(awsLoginStage);
		
		return stubbedPipeline;
	}
	
}
