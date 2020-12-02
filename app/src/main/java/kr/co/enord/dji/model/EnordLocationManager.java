package kr.co.enord.dji.model;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import org.osmdroid.util.GeoPoint;

import io.reactivex.subjects.PublishSubject;

public class EnordLocationManager implements LocationListener {

    private static EnordLocationManager m_instance;
    public static EnordLocationManager getInstance(){
        if(m_instance == null){
            m_instance = new EnordLocationManager();
        }

        return  m_instance;
    }

    private GeoPoint m_location;

    public GeoPoint getLocation()
    {
        return m_location;
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            if(m_location == null) m_location = new GeoPoint(location.getLatitude(), location.getLongitude());
            else {
                m_location.setLatitude(location.getLatitude());
                m_location.setLongitude(location.getLongitude());
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e("LocationListener", "onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e("LocationListener", "onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e("LocationListener", "onProviderDisabled");
    }
}
