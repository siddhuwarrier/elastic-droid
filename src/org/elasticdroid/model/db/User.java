package org.elasticdroid.model.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "UserTable")
public class User {
	
	public static final String COL_ID = "_ID";
	public static final String COL_AWS_ACCOUNT = "awsAccount";
	public static final String COL_AWS_ACCESS_KEY = "awsAccessKey";
	public static final String COL_AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey";
	
	public static final String TBL_NAME = "UserTable";
	
	@DatabaseField(
			generatedId = true, 
			columnName = COL_ID)
	private Integer _ID;
	
	@DatabaseField(
			unique = true, 
			canBeNull = false, 
			columnName = COL_AWS_ACCOUNT)
	private String awsAccount;
	
	@DatabaseField(
			canBeNull = false, 
			columnName = COL_AWS_ACCESS_KEY)
	private String awsAccessKey;
	
	@DatabaseField(
			canBeNull = false, 
			columnName = COL_AWS_SECRET_ACCESS_KEY)
	private String awsSecretAccessKey;

	public Integer get_ID() {
		return _ID;
	}

	public void set_ID(Integer _ID) {
		this._ID = _ID;
	}

	public String getAwsAccount() {
		return awsAccount;
	}

	public void setAwsAccount(String awsAccount) {
		this.awsAccount = awsAccount;
	}

	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public void setAwsAccessKey(String awsAccessKey) {
		this.awsAccessKey = awsAccessKey;
	}

	public String getAwsSecretAccessKey() {
		return awsSecretAccessKey;
	}

	public void setAwsSecretAccessKey(String awsSecretAccessKey) {
		this.awsSecretAccessKey = awsSecretAccessKey;
	}		
}
