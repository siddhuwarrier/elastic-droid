package org.elasticdroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;

//import org.elasticdroid.xml.stream.XMLIn

public class ElasticDroid extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	AWSCredentials credentials = new BasicAWSCredentials("FILL_IN_HERE", 
    			"FILL_IN_HERE");    
    	AmazonEC2 ec2 = new AmazonEC2Client(credentials);
    	
        DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
        Log.v(this.getLocalClassName(),"You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                " Availability Zones.");
    	
        setContentView(R.layout.main);
    }
}