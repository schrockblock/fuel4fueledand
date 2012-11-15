package ex.fueled.lunch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

/*
 * This is where most of the magic happens.
 * 
 * This activity analyzes the data from the group, gets a list of places matching
 * the group's preferences, selects two from different food types, and displays the
 * results. See below for more. 
 */
public class RecommenderActivity extends Activity implements OnClickListener{
	//facebook stuff
	Facebook facebook = new Facebook("361313500582379");
	String msg = "";
	
	//animation stuff
	ViewAnimator va;
	private Animation slideLeftIn;
	private Animation slideLeftOut;
	private Animation slideRightIn;
	private Animation slideRightOut;
	private GestureDetector gestureDetector;

	//stuff from xml
	private LinearLayout ll1;
	private LinearLayout ll2;
	private ImageView img1;
	private ImageView img2;
	private TextView name1;
	private TextView des1;
	private TextView name2;
	private TextView des2;
	private Button button1;
	private Button button2;

	//fields I'll need later
	private String desc1="";
	private String desc2="";
	private String imgurl1="";
	private String imgurl2="";
	private String n1="";
	private String n2="";
	private Bitmap bmp1;
	private Bitmap bmp2;
	
	private ProgressDialog pd;
    private Thread thread;
	private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	switch (msg.what){
        	case 0:
	        	build();
	            pd.dismiss();
	            break;
        	case 1:
        		pd.dismiss();
        		pd = ProgressDialog.show(RecommenderActivity.this, "", "Fetching first option...", true, true);
        		break;
        	case 2:
        		pd.dismiss();
        		pd = ProgressDialog.show(RecommenderActivity.this, "", "Fetching second option...", true, true);
        		break;
        	case 3:
        		pd.dismiss();
        		pd = ProgressDialog.show(RecommenderActivity.this, "", "Fetching details...", true, true);
        		break;
        	case 4:
        		if (bmp1!=null && bmp2 != null)
        			setPics();
        		break;
        	case 5:
        		Toast.makeText(RecommenderActivity.this, "Successfully posted to facebook!", Toast.LENGTH_LONG).show();
        		break;
        	}
        }
    };
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recommend);
        
        linkToXml();
        setAnimations();
        
        gestureDetector = new GestureDetector(new MyGestureDetector());
        
        setOnGestures();
        setOnClicks();
        
        facebook.authorize(this, new DialogListener() {
            @Override
            public void onComplete(Bundle values) {
            	
            }

            @Override
            public void onCancel() {}

			@Override
			public void onError(DialogError e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onFacebookError(FacebookError e) {
				// TODO Auto-generated method stub
				
			}
        });

		pd = ProgressDialog.show(this, "", "Calculating preferences...", true, true);
        thread = new Thread(new Runnable(){
        	public void run(){
        		try{
        			getRecs();
	                handler.sendEmptyMessage(0);
        		}catch(Exception e){
        			//Toast.makeText(this, "Please make sure you are connected to the internet, and restart the application.", 10000).show();
        			e.printStackTrace();
        		}
        	}
        });
        thread.start();
	}

	private void getRecs(){
		Bundle extras = getIntent().getExtras();
		int[] food = extras.getIntArray("food");
		//this is where I realized that Google Places doesn't support searching based on price. Changed to rating.
		int[] stars = extras.getIntArray("money");
		int[] dist = extras.getIntArray("dist");
		
		double lat = extras.getDouble("lat");
		double lon = extras.getDouble("lon");
		
		String[] foods = {"asian thai indian chinese sushi japanese","burgers wings pizza bbq fried","italian","middle eastern","mexican","vegetarian"};
		
		//first, find which food types were most popular
		int first = 0;
		int second = 0;
		int third = 0;
		for (int i = 0; i<6; i++){
			if (food[i]>=food[first]){
				third=second;
				second=first;
				first=i;
			}
		}
		
		//how far people are willing to travel
		String[] distances = {"400","1000","4000"};
		int d = 2;
		if (dist[0]/(dist[0]+dist[1]+dist[2])>.3){
			d=0;
		}else if (dist[1]>dist[2]){
			d=1;
		}
		
		//quality of the establishment
		double[] star = {1.0,3.0,4.5};
		int s = 0;
		if (stars[2]/(stars[2]+stars[1]+stars[0])>.3 && stars[2]/(stars[2]+stars[1]+stars[0])!=1){
			s=2;
		}else if (stars[2]<stars[1]){
			s=1;
		}
		
		try{
            handler.sendEmptyMessage(1);
			double minRating = star[s];
			String place = bestPlace(foods[first],minRating,lat,lon,Integer.parseInt(distances[d]));
			String place2 = "";
			
			//if then trees to find 2 matching choices
			if (place.equals("")){
				first = second;
				second = third;
				
				place = bestPlace(foods[first],minRating,lat,lon,Integer.parseInt(distances[d]));
			}else{
	            handler.sendEmptyMessage(2);
				place2 = bestPlace(foods[second],minRating,lat,lon,Integer.parseInt(distances[d]));
				
				if (place2.equals("")){
					place2 = bestPlace(foods[third],minRating,lat,lon,Integer.parseInt(distances[d]));
					
					if (place2.equals("")){
						setFail2();
					}else{
						setPlaceInfo2(place2);
					}
				}
			}
			
			if (place.equals("")){
				first = second;
				second = third;
				
				place = bestPlace(foods[first],minRating,lat,lon,Integer.parseInt(distances[d]));
			}else{
				setPlaceInfo1(place);

	            handler.sendEmptyMessage(2);
				place2 = bestPlace(foods[second],minRating,lat,lon,Integer.parseInt(distances[d]));
				
				if (place2.equals("")){
					setFail2();
				}else{
					setPlaceInfo2(place2);
				}
			}

			if (place.equals("")){
				setFail1();
			}
	        handler.sendEmptyMessage(3);

		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	//takes the data obtained and puts it in the UI
	private void build(){
		des1.setText(desc1);
		des2.setText(desc2);
		name1.setText(n1);
		name2.setText(n2);
		if (n1.equals("Failure")){
			button1.setEnabled(false);
			button2.setEnabled(false);
		}else if (n2.equals("Failure")){
			setPic1();
			button2.setEnabled(false);
		}else{
			setPic1();
			setPic2();
		}
	}
	
	//grabs the icon for the first place
	private void setPic1(){
		Thread picthread1 = new Thread(new Runnable(){
			@Override
			public void run(){
				try {
		            URL url = new URL(imgurl1); //you can write here any link
		            
		            /* Open a connection to that URL. */
		            URLConnection ucon = url.openConnection();

		            /*
		             * Define InputStreams to read from the URLConnection.
		             */
		            InputStream is = ucon.getInputStream();
		            BufferedInputStream bis = new BufferedInputStream(is);

		            /*
		             * Read bytes to the Buffer until there is nothing more to read(-1).
		             */
		            ByteArrayBuffer baf = new ByteArrayBuffer(50);
		            int current = 0;
		            while ((current = bis.read()) != -1) {
		                    baf.append((byte) current);
		            }
		            byte[] blob = baf.toByteArray();
		            bmp1=BitmapFactory.decodeByteArray(blob,0,blob.length);
		            handler.sendEmptyMessage(4);
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		});
		picthread1.start();
	}

	//grabs the icon for the second place
	private void setPic2(){
		Thread picthread2 = new Thread(new Runnable(){
			@Override
			public void run(){
				try {
		            URL url = new URL(imgurl2); //you can write here any link
		            
		            /* Open a connection to that URL. */
		            URLConnection ucon = url.openConnection();

		            /*
		             * Define InputStreams to read from the URLConnection.
		             */
		            InputStream is = ucon.getInputStream();
		            BufferedInputStream bis = new BufferedInputStream(is);

		            /*
		             * Read bytes to the Buffer until there is nothing more to read(-1).
		             */
		            ByteArrayBuffer baf = new ByteArrayBuffer(50);
		            int current = 0;
		            while ((current = bis.read()) != -1) {
		                    baf.append((byte) current);
		            }
		            byte[] blob = baf.toByteArray();
		            bmp2=BitmapFactory.decodeByteArray(blob,0,blob.length);
		            handler.sendEmptyMessage(4);
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		});
		picthread2.start();
	}
	
	//sets the icons
	private void setPics(){
        img1.setImageBitmap(bmp1);
        img2.setImageBitmap(bmp2);
	}
	
	//calls the places api and picks the best place in the radius
	private String bestPlace(String food, double minRating, double lat, double lon, int d) throws Exception, JSONException{
		DefaultHttpClient client = new DefaultHttpClient();
		String url = "https://maps.googleapis.com/maps/api/place/search/json?location="
			+lat+","+lon+"&types=food&keyword="+URLEncoder.encode(food)+"&sensor=true&rankby=distance&key="+Constants.PLACES_API_KEY;
		HttpGet get = new HttpGet(url);
		
		HttpResponse response = client.execute(get);
		BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
		    builder.append(line).append("\n");
		}
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject answer = new JSONObject(tokener);
		JSONArray array = answer.getJSONArray("results");
		
		String place = "";
	    float oLat = (float)lat;
	    float oLon = (float)lon;
	    
		for (int i = 0; i < array.length(); i++) {
		    JSONObject row = array.getJSONObject(i);
		    double rating = 1.0;
		    try {
		    	rating = Double.parseDouble(row.getString("rating"));
		    }catch (JSONException e){
		    	
		    }
		    String id = row.getString("reference");
		    row = row.getJSONObject("geometry");
		    row = row.getJSONObject("location");
		    float pLat = Float.parseFloat(row.getString("lat"));
		    float pLon = Float.parseFloat(row.getString("lng"));
		    if (distFrom(pLat,pLon,oLat,oLon)<=d){
		    	if (rating>=minRating){
		    		place = id;
		    		minRating=rating;
		    	}
		    }
		}
		return place;
	}
	
	//sets the fields which will be put into the UI for the first option
	private void setPlaceInfo1(String place) throws Exception, JSONException{
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet("https://maps.googleapis.com/maps/api/place/details/json?reference="
				+place+"&sensor=true&key="+Constants.PLACES_API_KEY);
		
		HttpResponse response = client.execute(get);
		BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
		    builder.append(line).append("\n");
		}
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject answer = new JSONObject(tokener);
		answer = answer.getJSONObject("result");
		
		try {
			desc1 = "Rating: "+answer.getString("rating")+ " stars";
			n1 = answer.getString("name");
			imgurl1 = answer.getString("icon");
		}catch (Exception e){
			desc1 = "Rating: "+" no rating";
			n1 = answer.getString("name");
			imgurl1 = answer.getString("icon");
		}
		
	}

	//sets the fields which will be put into the UI for the second option
	private void setPlaceInfo2(String place) throws Exception, JSONException{
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet("https://maps.googleapis.com/maps/api/place/details/json?reference="
				+place+"&sensor=true&key="+Constants.PLACES_API_KEY);
		
		HttpResponse response = client.execute(get);
		BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
		    builder.append(line).append("\n");
		}
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject answer = new JSONObject(tokener);
		answer = answer.getJSONObject("result");
		
		desc2 = "Rating: "+answer.getString("rating")+ " stars";
		n2 = answer.getString("name");
		imgurl2 = answer.getString("icon");
	}
	
	//if couldn't find any matching places, say so.
	private void setFail1(){
		desc1 = "It seems we have been unable to find an establishment matching your preferences. Feel free to change your preferences and try again.";
		n1 = "Failure";
	}

	//if couldn't find a second matching place, say so.
	private void setFail2(){
		desc2 = "We were unable to find you a second choice. If you are happy with the first option, swipe left and confirm; if not, go back and change your preferences";
		n2 = "Failure";
	}
	
	//calculates distance based on latitude and longitude. Taken from:
	// http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
	public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
		double earthRadius = 3958.75;
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lng2-lng1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
		Math.sin(dLng/2) * Math.sin(dLng/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double dist = earthRadius * c;

		int meterConversion = 1609;

		return new Float(dist * meterConversion).floatValue();
	}
	
	private void linkToXml(){
		va = (ViewAnimator)findViewById(R.id.recommend_anim);
        
		ll1 = (LinearLayout)findViewById(R.id.opt1ll);
        ll2 = (LinearLayout)findViewById(R.id.opt2ll);
        
        img1 = (ImageView)findViewById(R.id.image1);
        des1 = (TextView)findViewById(R.id.description1);
        name1 = (TextView)findViewById(R.id.name1);
        button1 = (Button)findViewById(R.id.button1);

        img2 = (ImageView)findViewById(R.id.image2);
        des2 = (TextView)findViewById(R.id.description2);
        name2 = (TextView)findViewById(R.id.name2);
        button2 = (Button)findViewById(R.id.button2);
        
        //done = (Button)findViewById(R.id.done_choosing);
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
		button1.setOnClickListener(this);
		button2.setOnClickListener(this);
	}
	
	private void setOnGestures(){
		ll1.setOnTouchListener(new View.OnTouchListener() {
        	@Override
        	public boolean onTouch(View v, MotionEvent event) {
        		if (gestureDetector.onTouchEvent(event)){
        			return true;
        		}else{
        			return false;
        		}
        	}
        });
        ll2.setOnTouchListener(new View.OnTouchListener() {
        	@Override
        	public boolean onTouch(View v, MotionEvent event){
        		if (gestureDetector.onTouchEvent(event))
        			return true;
        		else
        			return false;
        	}
        });
	}
	
	//When the user selects an option, the app will (hopefully) update facebook
	@Override
	public void onClick(View v) {
		switch (v.getId()){
		case R.id.button1:
			msg="Going to " +n1+ " with my colleagues!";
			break;
		case R.id.button2:
			msg="Going to " +n2+ " with my colleagues!";
			break;
		}
		DialogListener dl = new DialogListener() {
	        @Override
	        public void onComplete(Bundle values) {
	        	Thread thread2 = new Thread(new Runnable(){
	        		public void run(){
	            		try{
	        	            updateStatus(facebook.getAccessToken(), msg);
	            		}catch(Exception e){
	            			//Toast.makeText(this, "Please make sure you are connected to the internet, and restart the application.", 10000).show();
	            			e.printStackTrace();
	            		}
	            	}
	        	});
	        	thread2.start();
	        }

	        @Override
	        public void onFacebookError(FacebookError error) {
	        	error.printStackTrace();
	        }

	        @Override
	        public void onError(DialogError e) {
	        	e.printStackTrace();
	        }

	        @Override
	        public void onCancel() {
	        	msg = "cancelled";
	        }
	    };
		facebook.authorize(this, new String[]{"publish_stream"}, dl );
		finish();
	}
	
	//updating Status
	public void updateStatus(String accessToken, String msg){
	    try {
	        Bundle bundle = new Bundle();
	        bundle.putString("message", msg);
	        bundle.putString(Facebook.TOKEN,accessToken);
	        String response = facebook.request("me/feed",bundle,"POST");
	        handler.sendEmptyMessage(5);
	    } catch (MalformedURLException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }
	
	//Animation stuff from here on out
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

	
}
