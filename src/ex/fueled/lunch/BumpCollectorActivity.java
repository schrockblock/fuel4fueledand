package ex.fueled.lunch;

import java.nio.ByteBuffer;
import java.util.Vector;

import com.bumptech.bumpapi.BumpAPI;
import com.bumptech.bumpapi.BumpAPIListener;
import com.bumptech.bumpapi.BumpConnectFailedReason;
import com.bumptech.bumpapi.BumpConnection;
import com.bumptech.bumpapi.BumpDisconnectReason;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/*
 * Pretty simple activity. Collects bumps from colleagues, and when you're done,
 * compiles the data and passes it to the next activity.
 */
public class BumpCollectorActivity extends Activity implements OnClickListener, BumpAPIListener{
	private BumpConnection conn;
	private Handler handler = new Handler();

	Vector<Person> people = new Vector<Person>();
	Person p;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bumper);
        
        Button bump = (Button)findViewById(R.id.bump_button);
        Button done = (Button)findViewById(R.id.done_bumping);
        
        bump.setOnClickListener(this);
        done.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId()==R.id.bump_button){
			Intent bump = new Intent(BumpCollectorActivity.this, BumpAPI.class);
			bump.putExtra(BumpAPI.EXTRA_API_KEY, Constants.BUMP_API_KEY );
			startActivityForResult(bump, Constants.BUMP_API_REQUEST_CODE);
		}else{
			String phoneId = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
			Bundle extras = getIntent().getExtras();
			byte[] choices = extras.getByteArray("choices");
			double latitude = extras.getDouble("latitude");
			double longitude = extras.getDouble("longitude");
			p=new Person();
			p.setId(phoneId.getBytes());
			p.setPrefs(choices);
			p.setLatitude(doubleBytes(latitude));
			p.setLongitude(doubleBytes(longitude));
			people.add(p);
			calcAndStartNext();
		}
	}
	
	private void calcAndStartNext(){
		int[] foodType = new int[6];
		int[] moneyOpts = new int[3];
		int[] distanceOpts = new int[3];
		
		//doubles to calculate the barycenter (average) location of the people in the group.
		//this is important because if we want everyone to get the same recommendations, must
		//have same starting location. Plus, taking several gps readings will probably be 
		//better than just one.
		double baryLat = 0;
		double baryLon = 0;
		
		for (int i = 0; i<people.size(); i++){
			Person p = people.get(i);
			byte[] prefs = p.getPrefs();
			for (int j = 0; j<6; j++){
				if (((prefs[0]>>j) & 0x01)>0){
					foodType[j]++;
				}
			}
			byte b = (byte) (prefs[0]>>6);
			switch (b){
			case 0x01:
				distanceOpts[0]++;
				break;
			case -2:
				distanceOpts[1]++;
				break;
			case (byte)0xFF:
				distanceOpts[2]++;
				break;
			}
			switch (prefs[1]){
			case 0x01:
				moneyOpts[0]++;
				break;
			case 0x02:
				moneyOpts[1]++;
				break;
			case 0x03:
				moneyOpts[2]++;
				break;
			}
			
			baryLat += p.getLatitude();
			baryLon += p.getLongitude();
		}
		
		baryLat /= people.size();
		baryLon /= people.size();
		
		Intent recommend = new Intent(this, RecommenderActivity.class);
		recommend.putExtra("food", foodType);
		recommend.putExtra("dist", distanceOpts);
		recommend.putExtra("money", moneyOpts);
		recommend.putExtra("lat", baryLat);
		recommend.putExtra("lon", baryLon);
		startActivity(recommend);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.BUMP_API_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// Bump connected successfully, set this activity as its listener
				conn = (BumpConnection) data.getParcelableExtra(BumpAPI.EXTRA_CONNECTION);
				conn.setListener(this, handler);
				String phoneId = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
				Bundle extras = getIntent().getExtras();
				byte[] choices = extras.getByteArray("choices");
				double latitude = extras.getDouble("latitude");
				double longitude = extras.getDouble("longitude");
				conn.send(phoneId.getBytes());
				conn.send(choices);
				conn.send(doubleBytes(latitude));
				conn.send(doubleBytes(longitude));
			} else if (data != null) {
				// Failed to connect, obtain the reason
				BumpConnectFailedReason reason =
					(BumpConnectFailedReason) data.getSerializableExtra(BumpAPI.EXTRA_REASON);
			}
		}
	}
	
	private byte[] doubleBytes(double d){
		long bits = Double.doubleToLongBits(d);
		byte[] bytes = new byte[8];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putLong(bits);
		return buf.array();
	}

	@Override
	public void bumpDataReceived(byte[] arg0) {
		int last = people.size()-1;
		if (last<0){
			p = new Person();
		}
		if (arg0.length>8){
			p.setId(arg0);
		}
		if (arg0.length==8){
			people.get(last).setLatitude(arg0);
			if (people.get(last).getLatitude()<0){
				people.get(last).setLongitude(arg0);
			}
		}
		if (arg0.length==2){
			people.get(last).setPrefs(arg0);
		}
		if (p.isComplete()){
			if (people.contains(p)){
				people.remove(p);
			}
			people.add(p);
			p=new Person();
		}
	}

	@Override
	public void bumpDisconnect(BumpDisconnectReason arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}
