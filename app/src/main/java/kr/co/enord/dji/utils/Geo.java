package kr.co.enord.dji.utils;

import android.location.Location;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import kr.co.enord.dji.model.GeoJson;

public class Geo {
    private final double WGS84_RADIUS = 6370997.0;
    private double EarthCircumFence = 2* WGS84_RADIUS * Math.PI;

    private static Geo m_instance;
    public static Geo getInstance(){
        if(m_instance == null){
            m_instance = new Geo();
        }

        return  m_instance;
    }

    private Geo(){
        // for Elevation Info(geotiff file)
        gdal.AllRegister();
    }

    /**
     *  WGS84 좌표계에서 두 지점 사이의 거리
     * @param startLatitude : 시작점 위도
     * @param startLongitude : 시작점 경도
     * @param endLatitude : 끝점 위도
     * @param endLongitude : 끝점 경도
     * @return : 두 지점 사이의 거리(m)
     */
    public double distance(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        float[] _distance = new float[2];
        Arrays.fill(_distance, 0.0F);

        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, _distance);

        if (_distance[0] <= 0.0F || _distance[0] > 100000.0F) {
            _distance[0] = 0.0F;
        }

        return (double) _distance[0];
    }

    /**
     * 주어진 경로에서 gson으로 되어 있는 파일 경로들을 반환한다.
     * @param path
     * @return
     */
    public List<File> getGeojsonFiles(String path, boolean recursive) {
        List<File> result = new ArrayList<>();
        File root = new File(path);

        if(root.isDirectory())
        {
            File[] files = root.listFiles();

            // 2020.11.09 파일명으로 정렬한다.
            Arrays.sort(files, new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    File file1 = (File)arg0;
                    File file2 = (File)arg1;
                    return file1.getName().compareToIgnoreCase(file2.getName());
                }
            });

            for(File file : files)
            {
                if(file.isDirectory() && recursive){
                    result.addAll(getGeojsonFiles(file.getAbsolutePath(), recursive));
                }else{
                    if(file.getName().endsWith(".gson")) result.add(file);
                }
            }
        }

        return result;
    }

    public List<File> getGeoJsonDirectories(String root_path){
        List<File> result = new ArrayList<>();
        File root = new File(root_path);

        if(root.isDirectory())
        {
            if(hasGsonFile(root))   result.add(root);

            File[] files = root.listFiles();

            for(File file : files)
            {
                if(file.isDirectory()){
                    result.addAll(getGeoJsonDirectories(file.getAbsolutePath()));
                }
            }
        }

        return result;
    }

    private boolean hasGsonFile(File file){
        File[] files = file.listFiles();

        for(File item : files) {
            if(item.getName().endsWith(".gson")) {

                return true;
            }
        }

        return false;
    }

    /**
     * 파일에서 지리정보 추출
     * @param file_path
     * @return
     */
    public GeoJson getGeoInfo(String file_path) {
        // 파일 읽기
        File file = new File(file_path);
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            FileReader file_reader = new FileReader(file);
            BufferedReader br = new BufferedReader(file_reader);

            while((line = br.readLine()) != null){
                sb.append(line);
            }

            br.close();
            file_reader.close();

        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }

        // 위치 생성
        return new GeoJson(sb.toString());
    }

    /**
     * 주어진 저점의 표고값을 구한다.
     * @param point
     * @return
     */
    public int getElevation(GeoPoint point){
        // check file
        String file_path = Environment.getExternalStorageDirectory() + "/DroneAppService/DEM/Dem.tif";
        if(!checkFile(file_path)) return -1;

        // geotiff file load and get Raster
        Dataset _elevationDataSet = gdal.Open(file_path, gdalconst.GA_ReadOnly);
        // 정상적인 파일이 아님
        if (_elevationDataSet == null) return -2;

        Band _rasterBand = _elevationDataSet.GetRasterBand(1);

        SpatialReference _src = new SpatialReference();
        _src.SetWellKnownGeogCS("WGS84");
        String _projection = _elevationDataSet.GetProjection();
        SpatialReference _dst = new SpatialReference(_projection);

        CoordinateTransformation _ct = new CoordinateTransformation(_src, _dst);
        // 시작위치 고도 적용
        int[] altitude = new int[2];
        if (getAltitude(point, _elevationDataSet, _rasterBand, _ct, altitude) != 0) {
            return -3;
        }

        Log.e("Geo", "고도 : " + altitude[0]);
        return altitude[0];
    }

    /**
     * 여러 지점들의 표고값을 구한다.
     * @param points
     * @return
     */
    public int getElevations(List<GeoPoint> points){
        // check file
        String file_path = Environment.getExternalStorageDirectory() + "/DroneAppService/DEM/Dem.tif";
        if(!checkFile(file_path)) return -1;

        // geotiff file load and get Raster
        Dataset _elevationDataSet = gdal.Open(file_path, gdalconst.GA_ReadOnly);
        // 정상적인 파일이 아님
        if (_elevationDataSet == null) return -2;

        Band _rasterBand = _elevationDataSet.GetRasterBand(1);

        SpatialReference _src = new SpatialReference();
        _src.SetWellKnownGeogCS("WGS84");
        String _projection = _elevationDataSet.GetProjection();
        SpatialReference _dst = new SpatialReference(_projection);

        CoordinateTransformation _ct = new CoordinateTransformation(_src, _dst);
        // 시작위치 고도 적용
        int[] altitude = new int[2];

        for(GeoPoint point : points){
            if (getAltitude(point, _elevationDataSet, _rasterBand, _ct, altitude) != 0) {
                return -3;
            }

            point.setAltitude(altitude[0]);
        }

        return 0;
    }

    /**
     * 두 지점간의 고도차가 주어진 높이 보다 높은 지점
     * @param start
     * @param end
     * @return
     */
    public GeoPoint getHigherPoint(GeoPoint start, GeoPoint end, int min_elevation){
        // check file
        String file_path = Environment.getExternalStorageDirectory() + "/DroneAppService/DEM/Dem.tif";
        if(!checkFile(file_path)) return null;

        // geotiff file load and get Raster
        Dataset _elevationDataSet = gdal.Open(file_path, gdalconst.GA_ReadOnly);
        // 정상적인 파일이 아님
        if (_elevationDataSet == null) return null;

        Band _rasterBand = _elevationDataSet.GetRasterBand(1);

        SpatialReference _src = new SpatialReference();
        _src.SetWellKnownGeogCS("WGS84");
        String _projection = _elevationDataSet.GetProjection();
        SpatialReference _dst = new SpatialReference(_projection);

        CoordinateTransformation _ct = new CoordinateTransformation(_src, _dst);

        int[] altitude = new int[2];


        // 진행 방향(EW)
        int unit = 20;
        int i = 1;
        int direction = (end.getLongitude() - start.getLongitude() ) > 0 ? 1 : -1;

        GeoPoint selected = null;
        while (true){

            // 두 점을 지나는 선분과 x = 시작점 + a 인 선과의 교차점
            GeoPoint p1 = getPointFromDistance(start, direction*20*i++, 0);
            p1.setLatitude(40.0);
            GeoPoint p2 = new GeoPoint(32.0, p1.getLongitude());
            GeoPoint point = getIntersectPoint(start.getLongitude(), start.getLatitude(), end.getLongitude(), end.getLatitude()
                                               , p1.getLongitude(), p1.getLatitude(), p2.getLongitude(), p2.getLatitude());

            if(point == null) break;

            if (getAltitude(point, _elevationDataSet, _rasterBand, _ct, altitude) != 0) break;

            if(altitude[0] > min_elevation) {

                if( selected == null
                    || (selected != null && altitude[0] > selected.getAltitude())) {
                    selected = null;
                    selected = point;
                    selected.setAltitude(altitude[0]);
                }
            }
        }

        return selected;
    }

    /**
     * 주어진 좌표에서 동서, 남북의 거리에 위치한 좌표 구하기
     * @param source_point : 주어진 좌표
     * @param east_west : 동서방향의 거리(동: +, 서: -)
     * @param north_south : 남북방향의 거리(북 : +, 남 : -)
     * @return 주어진 좌표에서 동서, 남북의 거리에 위치한 좌표
     */
    public GeoPoint getPointFromDistance(GeoPoint source_point, double east_west, double north_south){
        double degreesPerMeterForLat = EarthCircumFence/360.0;
        double shrinkFactor = Math.cos((source_point.getLatitude()*Math.PI/180));
        double degreesPerMeterForLon = degreesPerMeterForLat * shrinkFactor;
        double newLat = source_point.getLatitude() + north_south * (1/degreesPerMeterForLat);
        double newLng = source_point.getLongitude() + east_west * (1/degreesPerMeterForLon);
        return new GeoPoint(newLat, newLng);
    }

    /**
     * 두 선의 교차점을 구한다.
     * @param a1_x 첫번째 선에서 지나는 첫번째 좌표의 x
     * @param a1_y 첫번째 선에서 지나는 첫번째 좌표의 y
     * @param a2_x 첫번째 선에서 지나는 두번째 좌표의 x
     * @param a2_y 첫번째 선에서 지나는 두번째 좌표의 y
     * @param b1_x 두번째 선에서 지나는 첫번째 좌표의 x
     * @param b1_y 두번째 선에서 지나는 첫번째 좌표의 y
     * @param b2_x 두번째 선에서 지나는 두번째 좌표의 x
     * @param b2_y 두번째 선에서 지나는 두번째 좌표의 y
     * @return 두 선의 교차점
     */
    private GeoPoint getIntersectPoint(double a1_x, double a1_y, double a2_x, double a2_y
            , double b1_x, double b1_y, double b2_x, double b2_y){

        double latitude;
        double longitude;

        double under = (b2_y-b1_y)*(a2_x-a1_x)-(b2_x-b1_x)*(a2_y-a1_y);
        if(under==0) return null;

        double _t = (b2_x-b1_x)*(a1_y-b1_y) - (b2_y-b1_y)*(a1_x-b1_x);
        double _s = (a2_x-a1_x)*(a1_y-b1_y) - (a2_y-a1_y)*(a1_x-b1_x);

        double t = _t/under;
        double s = _s/under;

        if(t<0.0 || t>1.0 || s<0.0 || s>1.0) return null;
        if(_t==0 && _s==0) return null;

        longitude = a1_x + t * (a2_x-a1_x);
        latitude = a1_y + t * (a2_y-a1_y);

        return new GeoPoint(latitude, longitude);
    }


    /**
     * 파일이 존재하는 지 확인한다.
     * @param filepath
     * @return
     */
    private boolean checkFile(String filepath) {
        File _file = new File(filepath);
        return _file.exists();
    }

    /**
     * 표고(Elevation) 정보를 DEM 파일에서 검색하여 가져온다.
     * @param point 표고정보를 받아올 좌표
     * @param data_set
     * @param band
     * @param ct
     * @param result 표고값
     * @return
     */
    private int getAltitude(GeoPoint point, Dataset data_set, Band band, CoordinateTransformation ct, int[] result) {
        double[] _geoTransformsInDoubles = data_set.GetGeoTransform();
        double _latitude = point.getLatitude();
        double _longitude = point.getLongitude();

        double[] _xy = ct.TransformPoint(_longitude, _latitude);
        int _x = (int) (((_xy[0] - _geoTransformsInDoubles[0]) / _geoTransformsInDoubles[1]));
        int _y = (int) (((_xy[1] - _geoTransformsInDoubles[3]) / _geoTransformsInDoubles[5]));

        return band.ReadRaster(_x, _y, 1, 1, result);
    }

    /**
     * 해당 파일의 위치정보를 가져온다.
     * @param path
     * @return
     */
    public float[] getPicturePosition(String path) {
        float[] output = new float[2];

        try {
            ExifInterface exifInterface = new ExifInterface(path);
            exifInterface.getLatLong(output);

        } catch (IOException e) {
            return null;
        }

        return output;
    }

}
