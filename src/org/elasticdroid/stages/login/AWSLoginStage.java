package org.elasticdroid.stages.login;

import org.elasticdroid.intf.AWSLogin;
import org.elasticdroid.model.LoginModel;
import org.elasticdroid.model.LoginModel.LoginResult;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

public class AWSLoginStage implements Stage {

	private AWSLogin awsLogin;
	
	private LoginModel loginModel;
	
	public AWSLoginStage() {
		awsLogin = new AWSLoginImpl();
	}
	
	/**
	 * This method allows the dev to replace the Login approach
	 * with a new login approach entirely.
	 * 
	 * @param awsLogin
	 */
	public void setAWSLogin(AWSLogin awsLogin) {
		this.awsLogin = awsLogin;
	}
	
	@Override
	public void execute(PipelineContext loginModel) {
		this.loginModel = (LoginModel) loginModel;
		loginToAws();
	}
	
	private void loginToAws() {
		loginModel.setLoginResult(awsLogin.validateCredentials());
	}
	
	
	public class AWSLoginImpl implements AWSLogin {
		
		@Override
		public LoginResult validateCredentials() {
			LoginResult loginResult = LoginResult.AUTH_SUCCESS;
			
			AWSCredentials awsCredentials = new BasicAWSCredentials(
					loginModel.getUser().getAwsAccessKey(), 
					loginModel.getUser().getAwsSecretAccessKey());
			
			AmazonEC2 ec2Client = new AmazonEC2Client(awsCredentials);
			
			//TODO hokey. replace with TVM.
			try {
				ec2Client.describeRegions();
			}
			catch(AmazonServiceException awsServiceException) {
				loginResult = LoginResult.AUTH_FAIL;
			}
			catch(AmazonClientException awsClientException) {
				loginResult = LoginResult.AWS_CLIENT_ERROR;
			}
			
			return loginResult;
		}
	}
}
