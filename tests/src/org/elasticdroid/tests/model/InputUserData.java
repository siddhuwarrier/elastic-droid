package org.elasticdroid.tests.model;

import org.elasticdroid.model.db.User;

public class InputUserData {

	public static final User exampleUser = new User();
	static {
		exampleUser.setAwsAccount("siddhu@siddhuw.info");
		exampleUser.setAwsAccessKey("BLAHBLAH");
		exampleUser.setAwsSecretAccessKey("BLEEG");
	}
	
	public static final User invalidEmailUser = new User();
	static {
		invalidEmailUser.setAwsAccount("blshj");
		invalidEmailUser.setAwsAccessKey("BLAHBLAH");
		invalidEmailUser.setAwsSecretAccessKey("BLEEG");
	}
	
	public static final User emptyAccessKeyUser = new User();
	static {
		emptyAccessKeyUser.setAwsAccount("blah@blah.com");
		emptyAccessKeyUser.setAwsAccessKey("");
		emptyAccessKeyUser.setAwsSecretAccessKey("BLEEG");
	}
	
	public static final User emptySecretAccessKeyUser = new User();
	static {
		emptySecretAccessKeyUser.setAwsAccount("blah@blah.com");
		emptySecretAccessKeyUser.setAwsAccessKey("BLAHBLAH");
		emptySecretAccessKeyUser.setAwsSecretAccessKey("");
	}
	
	public static final User emptyAwsAccountUser = new User();
	static {
		emptyAwsAccountUser.setAwsAccount("");
		emptyAwsAccountUser.setAwsAccessKey("BLAHBLAH");
		emptyAwsAccountUser.setAwsSecretAccessKey("BLEEG");
	}
}
