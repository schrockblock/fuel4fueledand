package ex.fueled.lunch;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/*
 * Acknowledgments:
 * Some of the location code came from: http://android-developers.blogspot.com/2011/06/deep-dive-into-location.html
 * Brushed up on downloading images from: http://www.helloandroid.com/tutorials/how-download-fileimage-url-your-device
 * Distance calculation: http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
 * Bump: http://bu.mp/api
 * Google Places: https://developers.google.com/maps/documentation/places/
 * Some animation stuff: http://developingandroid.blogspot.com/2009/09/implementing-swipe-gesture.html
 * 
 * Notes:
 * This is a pretty rough draft. Facebook updates only work intermittently,
 * errors are not handled elegantly or in a user friendly fashion, but between
 * 2 and a half jobs, school, and projects on the side, this is the best I 
 * could do in the time period. So, my apologies for the hasty structure and
 * jerry-rigged nature of this app.
 * 
 * You may be wondering about some of the choices I've made in this app. For
 * instance, why do I offer only 2 choices at the end? Well, recent studies
 * have shown that people tend to get overwhelmed by more than 2 choices,
 * especially if the run the risk of having several of one type and several
 * of another. Thus, I chose to offer only two choices from different food
 * types. If you have questions about other choices I made, please feel free
 * to contact me.
 * 
 * I would have liked to add some more things to really make this a nice app:
 * Add a button to see recommendations on the map, along with your distance
 * from them.
 * Grab an actual picture of the establishment, instead of the food icon that
 * ggl supplies.
 * Allow users to select previous lunch partners instead of bumping each 
 * time. I think bump allows you to do this, but I didn't delve into the api
 * enough yet.
 * Also, I would have liked to do more with the graphics, for instance,
 * make the splash screen image say 'Fuel 4 Fueled'.
 * 
 * Some things you may or may not have seen before:
 * Bump - Bump is a neat little company in Silicon Valley. Basically, their
 * api allows you to make a connection between two devices by gently bumping
 * them together. I think it's pretty nifty, and adds a physical aspect to 
 * the app. I worry that they may get driven out of business if NFC becomes
 * a big thing, but c'est la vie.
 * Google Places - I only just stumbled across this, apparently it's in beta
 * right now. Used it to get the recommended places, seemed pretty quick.
 * 
 * Final things: if you want facebook to work, you'll have to put in your 
 * own app id. Also, the update isn't working terribly well, I think it's a
 * threading issue. But if you massage it right, and it's a blue moon and 
 * after midnight on the ides of march, it will in fact work.
 * I also didn't have time to make this really MVC friendly. I kinda just
 * sat down and wrote, don't have time for much more.
 */
public class Splash extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        Thread timer = new Thread(){
      	  public void run(){
      		  try{
			  //normally, I'd do some loading here, but nothing needs it.
			  //I kept this because I think it helps ease the user into
			  //the app.
      			  sleep(2000); 
      		  }catch (InterruptedException e){
      			  e.printStackTrace();
      		  }finally{
      			Intent intent = new Intent(Splash.this, StartActivity.class);
      			startActivity(intent);
      		  }
      	  }
        };
        timer.start();
    }
}
