package ex.fueled.lunch;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;

import com.bumptech.bumpapi.BumpAPI;
import com.bumptech.bumpapi.BumpAPIListener;
import com.bumptech.bumpapi.BumpConnectFailedReason;
import com.bumptech.bumpapi.BumpConnection;
import com.bumptech.bumpapi.BumpDisconnectReason;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ViewAnimator;

public class ChooserActivity extends Activity implements OnClickListener, LocationListener {
	ViewAnimator va;
	private Animation slideLeftIn;
	private Animation slideLeftOut;
	private Animation slideRightIn;
	private Animation slideRightOut;

	private GestureDetector gestureDetector;
	
	private LocationManager locationManager;
	private Location currentBest;
	
	private LinearLayout foodll;
	private LinearLayout distancell;
	private LinearLayout moneyll;
	private LinearLayout finishll;
	private CheckBox cb1;
	private CheckBox cb2;
	private CheckBox cb3;
	private CheckBox cb4;
	private CheckBox cb5;
	private CheckBox cb6;
	
	private RadioGroup dgroup;
	private RadioGroup mgroup;
	
	private Button done; 
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chooser);
        
        startLocation();
        
        linkToXml();
        setAnimations();
        
        gestureDetector = new GestureDetector(new MyGestureDetector());
        
        setOnGestures();
        setOnClicks();
	}
	
	private void startLocation(){
		getLastBestLocation();
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
	}
	
	public void getLastBestLocation() {
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		List<String> matchingProviders = locationManager.getAllProviders();
		for (String provider: matchingProviders) {
			Location location = locationManager.getLastKnownLocation(provider);
			if (location != null) {
				if (isBetterLocation(location)){
					currentBest = location;
				}
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (isBetterLocation(location)){
			currentBest=location;
		}
	}
	
	/* The method below comes from Google's documentation, with a few of my own edits.
	 * It can be found at:
	 * http://developer.android.com/guide/topics/location/obtaining-user-location.html
	 */
	protected boolean isBetterLocation(Location location) {
	    if (currentBest == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBest.getTime();
	    boolean isSignificantlyNewer = timeDelta > 60*1000;
	    boolean isSignificantlyOlder = timeDelta < -60*1000;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }


	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBest.getAccuracy());
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate || (isNewer && !isSignificantlyLessAccurate)) {
	        return true;
	    }
	    return false;
	}
	
	private void linkToXml(){
		va = (ViewAnimator)findViewById(R.id.chooser_anim);
        foodll = (LinearLayout)findViewById(R.id.foodLinLay);
        moneyll = (LinearLayout)findViewById(R.id.moneyLinLay);
        distancell = (LinearLayout)findViewById(R.id.distanceLinLay);
        finishll = (LinearLayout)findViewById(R.id.finishLinLay);
        
        done = (Button)findViewById(R.id.done_choosing);
        
        dgroup = (RadioGroup)findViewById(R.id.dgroup);
        mgroup = (RadioGroup)findViewById(R.id.mgroup);
        
        cb1 = (CheckBox)findViewById(R.id.cb1);
        cb2 = (CheckBox)findViewById(R.id.cb2);
        cb3 = (CheckBox)findViewById(R.id.cb3);
        cb4 = (CheckBox)findViewById(R.id.cb4);
        cb5 = (CheckBox)findViewById(R.id.cb5);
        cb6 = (CheckBox)findViewById(R.id.cb6);
	}
	
	private void setAnimations(){
		slideLeftIn = AnimationUtils.loadAnimation(this, R.anim.push_left_in);
        slideLeftIn.setAnimationListener(new ScrollLeft());
        slideLeftOut = AnimationUtils.loadAnimation(this, R.anim.push_left_out);
        slideRightIn = AnimationUtils.loadAnimation(this, R.anim.push_right_in);
        slideRightIn.setAnimationListener(new ScrollRight());
        slideRightOut = AnimationUtils.loadAnimation(this, R.anim.push_right_out);
	}

	private void setOnClicks(){
        done.setOnClickListener(this);
	}
	
	private void setOnGestures(){
		foodll.setOnTouchListener(new View.OnTouchListener() {
        	@Override
        	public boolean onTouch(View v, MotionEvent event) {
        		if (gestureDetector.onTouchEvent(event)){
        			return true;
        		}else{
        			return false;
        		}
        	}
        });
        distancell.setOnTouchListener(new View.OnTouchListener() {
        	@Override
        	public boolean onTouch(View v, MotionEvent event){
        		if (gestureDetector.onTouchEvent(event))
        			return true;
        		else
        			return false;
        	}
        });
        moneyll.setOnTouchListener(new View.OnTouchListener() {
        	@Override
        	public boolean onTouch(View v, MotionEvent event){
        		if (gestureDetector.onTouchEvent(event))
        			return true;
        		else
        			return false;
        	}
        });
        finishll.setOnTouchListener(new View.OnTouchListener() {
        	@Override
        	public boolean onTouch(View v, MotionEvent event) {
        		if (gestureDetector.onTouchEvent(event))
        			return true;
        		else
        			return false;
        	}
        });
	}
	
	@Override
	public void onClick(View v) {
		Intent bumper = new Intent(this, BumpCollectorActivity.class);
		bumper.putExtra("choices", choicesBytes());
		bumper.putExtra("latitude", currentBest.getLatitude());
		bumper.putExtra("longitude", currentBest.getLongitude());
		startActivity(bumper);
	}
	
	/*
	 * Encodes this users choices. First 6 bits of first byte are for which type of
	 * food, last two bits are for distance, second byte is for number of stars.
	 * 
	 * Originally instead of stars, I asked for a price range... unfortunately Google
	 * Places doesn't support that yet, and by the time I figured it out it was too 
	 * late to go back. Thus, the references to 'money'.
	 */
	private byte[] choicesBytes(){
		byte ret = (byte)(cb1.isChecked()?1:0);
		ret = (byte)(((byte)(cb2.isChecked()?1:0)<<1)|ret);
		ret = (byte)(((byte)(cb3.isChecked()?1:0)<<2)|ret);
		ret = (byte)(((byte)(cb4.isChecked()?1:0)<<3)|ret);
		ret = (byte)(((byte)(cb5.isChecked()?1:0)<<4)|ret);
		ret = (byte)(((byte)(cb6.isChecked()?1:0)<<5)|ret);
		
		byte ret2=0x00;
		switch (dgroup.getCheckedRadioButtonId()){
		case R.id.dr2:
			ret2 = (byte) 0x80;
			break;
		case R.id.dr3:
			ret2 = (byte)0xc0;
			break;
		case R.id.dr1:
			ret2 = 0x40;
			break;
		}
		ret=(byte)(ret | ret2);
		
		byte ret3 = 0x00;
		switch (mgroup.getCheckedRadioButtonId()){
		case R.id.mr2:
			ret3 = 0x02;
			break;
		case R.id.mr3:
			ret3 = 0x03;
			break;
		default:
			ret3 = 0x01;
			break;
		}
		byte[] bytes = {ret,ret3};
		return bytes;
	}
	
	
	/*
	 * Past this point it's just handling gestures and some location related methods.
	 */
	class MyGestureDetector extends SimpleOnGestureListener {
		
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// int delta = 0;
			if (Math.abs(e1.getY() - e2.getY()) > Constants.SWIPE_MAX_OFF_PATH)
				return false;
			else{
				try {
					// right to left swipe
					if(e1.getX() - e2.getX() > Constants.SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Constants.SWIPE_THRESHOLD_VELOCITY) {
						va.setInAnimation(slideLeftIn);
						va.setOutAnimation(slideLeftOut);
						va.showNext();
						//left to right swipe
					} else if (e2.getX() - e1.getX() > Constants.SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Constants.SWIPE_THRESHOLD_VELOCITY) {
						va.setInAnimation(slideRightIn);
						va.setOutAnimation(slideRightOut);
						va.showPrevious();
					}
				} catch (Exception e) {
					// nothing
				}
				return true;
			}
		}
	}
	
	class ScrollRight implements AnimationListener{

		@Override
		public void onAnimationEnd(Animation animation) {
			va.postDelayed(new Runnable(){
				@Override
				public void run(){
					//va.showPrevious();
				}
			}, 10);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub

		}

	}

	class ScrollLeft implements AnimationListener{

		@Override
		public void onAnimationEnd(Animation animation) {
			va.postDelayed(new Runnable(){
				@Override
				public void run(){
					//va.showNext();
				}
			}, 10);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	public void onPause(){
		super.onPause();
		
		locationManager.removeUpdates(this);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		startLocation();
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}


}
