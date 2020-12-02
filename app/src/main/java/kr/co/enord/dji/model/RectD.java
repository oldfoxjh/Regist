package kr.co.enord.dji.model;

import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import kr.co.enord.dji.utils.Geo;

public class RectD {
    private double left = Double.MAX_VALUE;
    private double right = -Double.MAX_VALUE;
    private double top = Double.MAX_VALUE;
    private double bottom = -Double.MAX_VALUE;

    public RectD(List<GeoPoint> points) {
        for(int i = 0; i < points.size(); i++)
        {
            GeoPoint _point = points.get(i);

            left = (left > _point.getLongitude()) ?  _point.getLongitude() : left;
            right = (right > _point.getLongitude()) ? right : _point.getLongitude();
            top = (top > _point.getLatitude()) ? _point.getLatitude() : top;
            bottom = (bottom > _point.getLatitude()) ? bottom : _point.getLatitude();
        }
    }

    public GeoPoint getCenter(){

        double latitude = top - (top-bottom)/2;
        double longitude = right - (right - left)/2;

        return new GeoPoint(latitude, longitude);
    }

    public int getZoomLevel(){

        double width = Geo.getInstance().distance(top, left, top, right);
        double height = Geo.getInstance().distance(top, left, bottom, left);

        // 21 level 23m
        double zoom_level = ((width - height) > 0 ? width : height)/23;
        return 21 - (int)(Math.log10(zoom_level)/Math.log10(2));
    }

    public List<GeoPoint> getBound(){
        List<GeoPoint> bound = new ArrayList<>();

        bound.add(new GeoPoint(top, left));
        bound.add(new GeoPoint(top, right));
        bound.add(new GeoPoint(bottom, right));
        bound.add(new GeoPoint(bottom, left));
        bound.add(new GeoPoint(top, left));

        return bound;
    }
}
