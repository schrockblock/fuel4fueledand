package ex.fueled.lunch;

import java.io.Serializable;
import java.nio.ByteBuffer;

/*
 * This class is pretty self explanatory. Needed a place to store all the data
 * associated with a colleague with whom you've bumped. Really just a series of
 * get/set methods.
 * 
 * Also - I was taught that you should make everything as private/local as 
 * possible, and thus I've kept the fields private.
 */
public class Person implements Serializable{
	private byte[] id;
	private byte[] prefs;
	private double latitude;
	private double longitude;
	
	public Person(){
		id = null;
		prefs=null;
		latitude=0;
		longitude=0;
	}
	
	public void setId(byte[] id){
		this.id = id;
	}
	
	public void setPrefs(byte[] prefs){
		this.prefs = prefs;
	}
	
	public void setLatitude(byte[] lat){
		ByteBuffer buf2 = ByteBuffer.wrap(lat);
		latitude = buf2.getDouble();
	}
	
	public void setLongitude(byte[] lon){
		ByteBuffer buf2 = ByteBuffer.wrap(lon);
		longitude = buf2.getDouble();
	}
	
	public byte[] getPrefs(){
		return prefs;
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
	public byte[] getId(){
		return id;
	}
	
	public boolean isComplete(){
		return prefs!=null && latitude!=0 && longitude!=0 && id!=null;
	}

	@Override
	public boolean equals(Object o){
		if (this == o)
			return true;
		if (!(o instanceof Person))
			return false;
		if (((Person)o).getId()==this.getId())
			return true;
		
		return false;
	}
}
