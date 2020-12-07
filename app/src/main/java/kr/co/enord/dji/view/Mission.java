package kr.co.enord.dji.view;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.SettingsDefinitions;
import dji.sdk.camera.VideoFeeder;
import io.reactivex.observers.DefaultObserver;
import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.R;
import kr.co.enord.dji.model.AltResponse;
import kr.co.enord.dji.model.DroneStatus;
import kr.co.enord.dji.model.EMessage;
import kr.co.enord.dji.model.EnordLocationManager;
import kr.co.enord.dji.model.EnordWaypointMission;
import kr.co.enord.dji.model.GeoJson;
import kr.co.enord.dji.model.MissionHistory;
import kr.co.enord.dji.model.RectD;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ServerResponse;
import kr.co.enord.dji.model.ViewWrapper;
import kr.co.enord.dji.popup.CancelMission;
import kr.co.enord.dji.popup.CancelRTL;
import kr.co.enord.dji.popup.CompassCalibration;
import kr.co.enord.dji.popup.Landing;
import kr.co.enord.dji.popup.MissionLoad;
import kr.co.enord.dji.popup.MissionStart;
import kr.co.enord.dji.popup.ReturnHome;
import kr.co.enord.dji.utils.Geo;
import kr.co.enord.dji.utils.InputFilterMinMax;
import kr.co.enord.dji.utils.MapLayer;
import kr.co.enord.dji.utils.ResizeAnimation;
import kr.co.enord.dji.widget.CustomPreFlightStatusWidget;
import kr.co.enord.dji.widget.DjiVideoFeedView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Mission extends RelativeLayout implements View.OnClickListener, MapEventsReceiver, Marker.OnMarkerClickListener {
    private static final String TAG = "Mission";
    final int period = 250;                             // 드론정보 수십 주기 0.25 second
    Context m_context;
    MapView m_map_view = null;
    Timer timer = null;                                         // 드론정보 수집 타이머
    private Handler messageHandler;
    private SharedPreferences pref;                             // 상태값 저장
    private RelativeLayout m_container_progress;

    ViewGroup root_view;

    // region 지도
    boolean is_map_mini = false;                                // 맵 크기
    Marker marker_my_location = null;                           // 조종자 위치
    Marker marker_drone_location = null;                        // 드론 위치 정보 마커
    Marker marker_home_location = null;                         // 드론 이륙지점 위치 마커
    List<Marker> selected_markers = new ArrayList<>();          // 사용자가 선택한 위치를 나타내는 마커
    List<GeoPoint> selected_points = new ArrayList<>();         // 사용자가 선택한 위치를
    List<Marker> captured_markers = new ArrayList<>();          // 촬영지점 위치를 나타내는 마커

    Polygon flight_area = new Polygon();                        // 촬영영역
    Polyline mission_line = new Polyline();                     // 임무영역
    Polyline flying_line = null;                                // 비행경로 표시
    MissionHistory m_mission_file = null;                       // 임무 시작 파일 정보

    //
    List<Polygon> m_waypoint_areas = new ArrayList<>();
    List<Marker> m_waypoint_centers = new ArrayList<>();
    // endregion

    // region 드론 정보
    DjiVideoFeedView primary_camera;
    TextView tv_flight_altitude;
    TextView tv_flight_horizontal_speed;
    TextView tv_flight_vertical_speed;
    TextView tv_flight_distance_from_home;

    TextView tv_user_latitude;
    TextView tv_user_longitude;
    TextView tv_drone_latitude;
    TextView tv_drone_longitude;
    TextView tv_mission_file;
    // endregion

    // region 임무
    LinearLayout container_mission_type;
    Button btn_polygon_mission;
    Button btn_waypoint_mission;
    Button btn_3d_mission;
    TextView tv_mission_area;
    TextView tv_mission_lap_distance;
    TextView tv_mission_distance;
    TextView tv_mission_shoot_interval;

    EditText mission_flight_speed;
    EditText mission_flight_altitude;
    EditText mission_flight_overlap;
    EditText mission_flight_sidelap;
    TextView textview_mission_angle;
    // endregion

    // region 드론 제어
    RelativeLayout container_flight_control;
    Button btn_rtl_cancel;
    Button btn_flight_return_home;
    // endregion

    // region 드론 촬영
    RelativeLayout container_fpv;
    RelativeLayout camera_setting_layout;
    Button btn_flight_select_movie;
    Button btn_flight_select_shoot;
    Button btn_flight_record;
    Button btn_flight_shoot;
    Button btn_flight_camera_setting;
    TextView tv_flight_record_time;
    // endregion

    CustomPreFlightStatusWidget preflight_widget;

    private final OnlineTileSourceBase VWorldStreet = new OnlineTileSourceBase("VWorld", 0, 22, 256, "jpeg",
            new String[0], "VWorld") {

        public String getTileURLString(final long pMapTileIndex) {
            return "http://xdworld.vworld.kr:8080/2d/Satellite/service/"+ MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + ".jpeg";
        }
    };

    public Mission(Context context){
        super(context);
        m_context = context;
        initUI();
    }

    public Mission(Context context, AttributeSet attrs) {
        super(context, attrs);
        m_context = context;
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {

        messageHandler = new Handler(Looper.getMainLooper());

        // Data 수집 타이머 시작
        if(timer == null) timer = new Timer();
        timer.schedule(new Mission.CollectDroneInformationTimer(), 0, period);

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * View가 정상적으로 화면에 추가 되었을 때
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    /**
     * 초기 화면 설정
     */
    protected void initUI(){
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) m_context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_mission, this, true);

        //progress bar
        m_container_progress = findViewById(R.id.mission_container_progress);

        // MapView 설정
        m_map_view = findViewById(R.id.mission_map_view);
        m_map_view.setBuiltInZoomControls(false);
        m_map_view.setMultiTouchControls(true);
        m_map_view.setMinZoomLevel(8.0);
        m_map_view.setMaxZoomLevel(20.0);
        m_map_view.setTileSource(VWorldStreet);

        // Touch Overlay
        MapEventsOverlay _events = new MapEventsOverlay(this);
        m_map_view.getOverlays().add(_events);

        IMapController mapController = m_map_view.getController();
        mapController.setZoom(16.0);
        mapController.setCenter(new GeoPoint(36.361481, 127.384841));

        // 이륙지점
        marker_home_location = new Marker(m_map_view);
        marker_home_location.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                return true;
            }
        });
        marker_home_location.setIcon(ContextCompat.getDrawable(m_context, R.mipmap.map_ico_mission));
        marker_home_location.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        m_map_view.getOverlays().add(marker_home_location);
        marker_home_location.setVisible(false);

        // 드론 비행경로
        addFlyingLine();

        // 내위치
        marker_my_location = new Marker(m_map_view);
        marker_my_location.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                singleTapConfirmedHelper(null);
                return true;
            }
        });
        marker_my_location.setIcon(ContextCompat.getDrawable(m_context, R.mipmap.map_ico_my));
        marker_my_location.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        m_map_view.getOverlays().add(marker_my_location);

        // 드론 위치
        marker_drone_location = new Marker(m_map_view);
        marker_drone_location.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                return true;
            }
        });
        marker_drone_location.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker_drone_location.setIcon(MapLayer.getInstance().getRotateDrawable(m_context, R.mipmap.map_ico_drone, 0.0f));
        m_map_view.getOverlays().add(marker_drone_location);

        // 임무 경로
        mission_line.setColor(Color.WHITE);
        mission_line.setWidth(3.0f);
        m_map_view.getOverlayManager().add(mission_line);

        // 폴리곤 설정
        flight_area.setFillColor(Color.argb(60, 0, 255, 0));
        flight_area.setStrokeWidth(1.0f);
        m_map_view.getOverlayManager().add(flight_area);

        m_map_view.setOnClickListener(this);
        m_map_view.invalidate();
        setClickable(true);

        setWidget();

        // 이벤트 관리
        setEventManager();
    }

    private void addFlyingLine(){
        flying_line = new Polyline();
        flying_line.setColor(Color.RED);
        flying_line.setWidth(3.0f);
        m_map_view.getOverlayManager().add(flying_line);
    }

    private void setWidget()
    {
        tv_flight_altitude = findViewById(R.id.tv_flight_altitude);
        tv_flight_horizontal_speed = findViewById(R.id.tv_flight_horizontal_speed);
        tv_flight_vertical_speed = findViewById(R.id.tv_flight_vertical_speed);
        tv_flight_distance_from_home = findViewById(R.id.tv_flight_distance_from_home);

        tv_user_latitude = findViewById(R.id.tv_user_latitude);
        tv_user_longitude = findViewById(R.id.tv_user_longitude);
        tv_drone_latitude = findViewById(R.id.tv_drone_latitude);
        tv_drone_longitude = findViewById(R.id.tv_drone_longitude);
        tv_mission_file = findViewById(R.id.tv_mission_file);

        root_view = findViewById(R.id.root_view);
        primary_camera = findViewById(R.id.dji_primary_widget);
        if(VideoFeeder.getInstance() != null){
            primary_camera.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
        }

        // region 임무
        container_mission_type = findViewById(R.id.container_mission_type);
        btn_polygon_mission = findViewById(R.id.btn_polygon_mission);
        btn_polygon_mission.setVisibility(View.GONE);
        btn_polygon_mission.setOnClickListener(this);

        btn_waypoint_mission = findViewById(R.id.btn_waypoint_mission);
        btn_waypoint_mission.setOnClickListener(this);
        btn_waypoint_mission.setSelected(true);

        btn_3d_mission = findViewById(R.id.btn_3d_mission);
        btn_3d_mission.setOnClickListener(this);
        btn_3d_mission.setSelected(true);

        findViewById(R.id.btn_load_geo_json).setOnClickListener(this);                  // 파일 로드 버튼
        findViewById(R.id.btn_mission_upload).setOnClickListener(this);                 // 업로드 버튼
        findViewById(R.id.btn_new_course).setOnClickListener(this);
        findViewById(R.id.btn_reverse_course).setOnClickListener(this);

        tv_mission_area = findViewById(R.id.tv_mission_area);
        tv_mission_lap_distance = findViewById(R.id.tv_mission_lap_distance);
        tv_mission_distance = findViewById(R.id.tv_mission_distance);
        tv_mission_shoot_interval = findViewById(R.id.tv_mission_shoot_interval);

        mission_flight_speed = findViewById(R.id.mission_flight_speed);
        mission_flight_speed.setFilters(new InputFilter[]{ new InputFilterMinMax(1, 14)});
        mission_flight_speed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("speed", s.toString());
                editor.commit();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mission_flight_altitude = findViewById(R.id.mission_flight_altitude);
        mission_flight_altitude.setFilters(new InputFilter[]{ new InputFilterMinMax(1, 450)});
        mission_flight_altitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 최대 비행고도 확인 450m
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("altitude", s.toString());
                editor.commit();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mission_flight_overlap = findViewById(R.id.mission_flight_overlap);
        mission_flight_sidelap = findViewById(R.id.mission_flight_sidelap);
        // endregion

        // region 드론 제어
        container_flight_control = findViewById(R.id.container_flight_control);
        findViewById(R.id.btn_flight_landing).setOnClickListener(this);
        btn_flight_return_home = findViewById(R.id.btn_flight_return_home);
        btn_flight_return_home.setOnClickListener(this);
        btn_rtl_cancel = findViewById(R.id.btn_rtl_cancel);
        btn_rtl_cancel.setOnClickListener(this);
        // endregion

        // region 드론 촬영
        container_fpv = findViewById(R.id.container_fpv);
        container_fpv.setOnClickListener(this);
        camera_setting_layout = findViewById(R.id.camera_setting_layout);
        btn_flight_select_movie = findViewById(R.id.btn_flight_select_movie);
        btn_flight_select_movie.setOnClickListener(this);
        btn_flight_select_shoot = findViewById(R.id.btn_flight_select_shoot);
        btn_flight_select_shoot.setOnClickListener(this);
        btn_flight_record = findViewById(R.id.btn_flight_record);
        btn_flight_record.setOnClickListener(this);
        btn_flight_shoot = findViewById(R.id.btn_flight_shoot);
        btn_flight_shoot.setOnClickListener(this);
        btn_flight_camera_setting = findViewById(R.id.btn_flight_camera_setting);
        btn_flight_camera_setting.setOnClickListener(this);
        tv_flight_record_time = findViewById(R.id.tv_flight_record_time);
        // endregion

        pref = m_context.getSharedPreferences("drone", Context.MODE_PRIVATE);
        String altitude = pref.getString("altitude", null);
        String speed = pref.getString("speed", null);

        if(altitude != null) mission_flight_altitude.setText(altitude);
        if(speed != null) mission_flight_speed.setText(speed);

        preflight_widget = findViewById(R.id.preflight_status_view);
        preflight_widget.setOnClickListener(this);
    }

    /**
     * 지도 위젯 터치 이벤트 처리
     * @param p 터치된 좌표
     * @return 이벤트 처리 여부
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        // 비행중일 경우 FPV랑 전환
        if(is_map_mini == true) interchangeWidtet();
        else {
            if( p == null
                || DroneApplication.getDroneInstance().isFlying()
            ) return false;

            // 마커 생성
            selected_points.add(p);
            setMissionPolygon();

            m_map_view.invalidate();
        }

        return false;
    }

    private void interchangeWidtet(){
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int device_height = displayMetrics.heightPixels;
        int device_width = displayMetrics.widthPixels;

        int height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
        int width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics());
        int margin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        if(is_map_mini)
        {
            resizeFPVWidget(width, height, margin, 7);
            ResizeAnimation mapViewAnimation = new ResizeAnimation(m_map_view, width, height, device_width, device_height, 0);
            m_map_view.startAnimation(mapViewAnimation);
            is_map_mini = false;
        }else{
            resizeFPVWidget(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT, 0, 0);

            ResizeAnimation mapViewAnimation = new ResizeAnimation(m_map_view, device_width, device_height, width, height, margin);
            m_map_view.startAnimation(mapViewAnimation);
            is_map_mini = true;
        }
    }

    /**
     * 임무 Polygon 세팅
     */
    private void setMissionPolygon() {
        if(btn_waypoint_mission.isSelected()) {
            mission_line.getPoints().clear();

            if(selected_points.size() > 1) {
                mission_line.setPoints(selected_points);

                // 미션거리/총거리 표시
                double distance_total = mission_line.getDistance() + 0;
                tv_mission_distance.setText(String.format("%.1f m", distance_total));
                // 나머지. -
                tv_mission_area.setText("-");
                tv_mission_lap_distance.setText("-");
                tv_mission_shoot_interval.setText("-");

                if (!m_map_view.getOverlays().contains(mission_line))
                    m_map_view.getOverlayManager().add(mission_line);
            }
        }
        else {
            flight_area.getPoints().clear();
            if(selected_points.size() > 2) {

                flight_area.setPoints(selected_points);
                flight_area.addPoint(selected_points.get(0));

                if (!m_map_view.getOverlays().contains(flight_area))
                    m_map_view.getOverlayManager().add(flight_area);
            }
        }

        // 선택한 마커 삭제
        for(Marker marker :  selected_markers){
            m_map_view.getOverlayManager().remove(marker);
        }
        selected_markers.clear();
        for(GeoPoint point : selected_points){
            Marker _marker = getDefaultMarker(point);
            m_map_view.getOverlays().add(_marker);
            selected_markers.add(_marker);
        }

    }

    /**
     * 현재 설정된 비행경로를 초기화한다.
     */
    private void clearMission() {
        // 선택한 마커 삭제
        for(Marker marker : selected_markers){
            m_map_view.getOverlayManager().remove(marker);
        }

        for(Marker marker : captured_markers){
            m_map_view.getOverlayManager().remove(marker);
        }

        for(Polygon area : m_waypoint_areas){
            area.getPoints().clear();
            m_map_view.getOverlayManager().remove(area);
        }
        m_waypoint_areas.clear();

        for(Marker marker :  m_waypoint_centers){
            m_map_view.getOverlayManager().remove(marker);
        }
        m_waypoint_centers.clear();

        selected_markers.clear();
        selected_points.clear();
        captured_markers.clear();

        flight_area.setPoints(new ArrayList<GeoPoint>());
        mission_line.setPoints(new ArrayList<GeoPoint>());
        flying_line.setPoints(new ArrayList<GeoPoint>());

        m_map_view.invalidate();

        // 나머지. -
        tv_mission_distance.setText("-");
        tv_mission_area.setText("-");
        tv_mission_lap_distance.setText("-");
        tv_mission_shoot_interval.setText("-");
    }

    /**
     *  선택한 좌표의 마커를 생성한다.
     * @param p 터치한 위치 좌표
     * @return  마커
     */
    private Marker getDefaultMarker(GeoPoint p) {
        Marker _marker = new Marker(m_map_view);
        String _title;
        _marker.setPosition(new GeoPoint(p.getLatitude(), p.getLongitude()));
        _marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        _title = String.valueOf(selected_markers.size() + 1);
        _marker.setDraggable(true);
        _marker.setOnMarkerDragListener(new OnMarkerDragListenerDrawer());
        _marker.setIcon(MapLayer.getInstance().writeOnDrawable(m_context, _title, R.mipmap.waypoint));

        _marker.setTitle(_title);
        _marker.setOnMarkerClickListener(this);

        return  _marker;
    }

    private Marker getMissionCenterMarker(GeoPoint p, String title) {
        Marker marker = new Marker(m_map_view);
        marker.setPosition(new GeoPoint(p.getLatitude(), p.getLongitude()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setDraggable(true);
        marker.setOnMarkerDragListener(new OnMarkerDragListenerDrawer());
        //marker.setIcon(MapLayer.getInstance().writeOnDrawable(m_context, title, R.mipmap.mission_center));
        marker.setIcon(MapLayer.getInstance().writeOnDrawable(m_context, title,
                m_context.getResources().getDimensionPixelSize(R.dimen.map_mission_marker_width), m_context.getResources().getDimensionPixelSize(R.dimen.map_mission_marker_height),
                Color.BLACK, m_context.getResources().getDimensionPixelSize(R.dimen.mission_font)));
        marker.setOnMarkerClickListener(this);

        return  marker;
    }

    /**
     * 마커 드래그 이벤트
     */
    class OnMarkerDragListenerDrawer implements Marker.OnMarkerDragListener {

        @Override public void onMarkerDrag(Marker marker) {
            int _marker_index = Integer.parseInt(marker.getTitle()) - 1;
        }

        @Override public void onMarkerDragEnd(Marker marker) {
            m_map_view.invalidate();
        }

        @Override public void onMarkerDragStart(Marker marker) {
        }
    }

    /**
     * 카메라와 배경지도 간의 크기 변경 처리
     * @param width 변경될 width
     * @param height 변경될 height
     * @param margin 변경될 margin
     * @param fpvInsertPosition 위젯 z-index
     */
    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        ConstraintLayout.LayoutParams fpvParams = (ConstraintLayout.LayoutParams)container_fpv.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.leftMargin = margin;
        fpvParams.bottomMargin = margin;

        container_fpv.requestLayout();

        root_view.removeView(container_fpv);
        root_view.addView(container_fpv, fpvInsertPosition);
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_polygon_mission:
                if(!btn_polygon_mission.isSelected()) {
                    btn_polygon_mission.setSelected(true);
                    btn_waypoint_mission.setSelected(false);
                }

                clearMission();
                break;
            case R.id.btn_waypoint_mission:
                if(!btn_waypoint_mission.isSelected()) {
                    btn_waypoint_mission.setSelected(true);
                    btn_polygon_mission.setSelected(false);
                }

                clearMission();
                break;
            case R.id.btn_3d_mission:
                break;
            case R.id.btn_flight_landing:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new Landing(m_context)));
                break;
            case R.id.btn_flight_return_home:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new ReturnHome(m_context)));
                break;
            case R.id.btn_rtl_cancel:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new CancelRTL(m_context)));
                break;
            case R.id.btn_flight_select_movie:
                DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            case R.id.btn_flight_select_shoot:
                DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            case R.id.btn_flight_record:
                if(DroneApplication.getDroneInstance().isRecording()){
                    DroneApplication.getDroneInstance().stopRecordVideo();
                }else{
                    DroneApplication.getDroneInstance().startRecordVideo();
                }
                break;
            case R.id.btn_flight_shoot:
                DroneApplication.getDroneInstance().startShootPhoto();
                break;
            case R.id.btn_flight_camera_setting:
                break;
            case R.id.btn_mission_upload:
                m_container_progress.setVisibility(VISIBLE);

                // 임무 생성
                EnordWaypointMission mission = new EnordWaypointMission();
                float flight_speed = Float.parseFloat(mission_flight_speed.getText().toString());
                float altitude = Float.parseFloat(mission_flight_altitude.getText().toString());

                DroneApplication.getAPI().altitude(marker_home_location.getPosition().getLongitude(), marker_home_location.getPosition().getLatitude())
                        .enqueue(new Callback<AltResponse>(){
                    @Override
                    public  void onResponse(Call<AltResponse> call, Response<AltResponse> response){
                        if(response.code() == 200) {
                            AltResponse body = response.body();
                            marker_home_location.getPosition().setAltitude(body.getAltitude());
                            if(btn_waypoint_mission.isSelected()) {
                                mission.createWaypointMission(getMissonWaypoint(), marker_home_location.getPosition(), flight_speed, altitude);
                            }else{
                                mission.createAreaMission(getMissonWaypoint(), marker_home_location.getPosition(), flight_speed);
                            }
                            // 임무 체크
                            String result = DroneApplication.getDroneInstance().checkMission(mission.getWaypointMission());
                            if(result != null){
                                // 오류팝업
                                Log.e(TAG, result);
                                m_container_progress.setVisibility(INVISIBLE);
                                return;
                            }
                            // 임무 시작
                            DroneApplication.getDroneInstance().uploadMission();
                        }
                        Log.e(TAG, "onResponse : " + response.message());
                    }

                    @Override
                    public  void onFailure(Call<AltResponse> call, Throwable t){
                        Log.e(TAG, "onFailure : " + t.getMessage());
                        Toast.makeText(m_context, "네트워크 에러가 발생하였습니다.", Toast.LENGTH_LONG).show();
                    }
                });

                break;
            case R.id.btn_load_geo_json:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new MissionLoad(m_context)));
                break;
            case R.id.btn_new_course:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new CancelMission(m_context)));
                break;
            case R.id.btn_reverse_course:
                ReverseWaypoint();
                break;
            case R.id.container_fpv:
                if(is_map_mini == false) interchangeWidtet();
                break;
            case R.id.preflight_status_view:

               // if(){
                    // calibration 시작
                    if(DroneApplication.getDroneInstance() != null) DroneApplication.getDroneInstance().startCompassCalibration();
               // }
                break;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker, MapView mapView) {
        return false;
    }

    /**
     * 시동 걸리고 주어진 주기로 기체정보 수집
     */
    private class CollectDroneInformationTimer extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> UpdateUI());
        }
    }

    private void UpdateUI() {
        if(DroneApplication.getDroneInstance() != null) {
            GeoPoint my_location = EnordLocationManager.getInstance().getLocation();
            if(my_location != null) {
                marker_my_location.setPosition(my_location);
            }

            DroneStatus status = DroneApplication.getDroneInstance().getDroneStatus();
            if(status == null) return;

            if(status.home_latitude > 0 && status.home_longitude > 0 && marker_home_location.getAlpha() < 0.1)   {
                marker_home_location.setPosition(new GeoPoint(status.home_latitude, status.home_longitude));        // 홈 위치
                marker_home_location.setVisible(true);
                IMapController mapController = m_map_view.getController();
                mapController.setCenter(new GeoPoint(status.home_latitude, status.home_longitude));

                tv_user_latitude.setText(String.format("%3.6f",status.home_latitude));
                tv_user_longitude.setText(String.format("%3.6f",status.home_longitude));
            }

            // 드론 heading
            marker_drone_location.setIcon(MapLayer.getInstance().getRotateDrawable(m_context, R.mipmap.map_ico_drone, status.heading));

            // 드론 위치
            if(status.drone_latitude > 0 && status.drone_longitude > 0) {
                marker_drone_location.setPosition(new GeoPoint(status.drone_latitude, status.drone_longitude));     // 드론 위치

                tv_drone_latitude.setText(String.format("%3.6f",status.drone_latitude));
                tv_drone_longitude.setText(String.format("%3.6f",status.drone_longitude));
            }

            // 1. 조종기와의 거리
            if(my_location == null || Math.abs(my_location.getLatitude()) < 1) {
                tv_flight_distance_from_home.setText("0.0");
            } else {
                double distToController = Geo.getInstance().distance(
                        my_location.getLatitude(), my_location.getLongitude(), status.drone_latitude, status.drone_longitude);
                tv_flight_distance_from_home.setText(String.format("%.1f", distToController));
            }

            // 2. 고도
            String _alt = String.format("%.1f", status.drone_altitude);
            tv_flight_altitude.setText(_alt);

            // 3. 수평속도
            String v_x = String.format("%.1f", status.velocity_x);
            tv_flight_horizontal_speed.setText(v_x);

            // 4. 수직속도
            String v_z = String.format("%.1f", Math.abs(status.velocity_z));
            tv_flight_vertical_speed.setText(v_z);

            // 6. 비행경로(비행중일때만)
            RelativeLayout container_fc = findViewById(R.id.container_flight_control);
            if(DroneApplication.getDroneInstance().isFlying()) {
                GeoPoint _dron_location = new GeoPoint(status.drone_latitude, status.drone_longitude, status.drone_altitude);
                flying_line.addPoint(_dron_location);

                // 복귀 기능 및 기체 정보 visible
                if(container_fc.getVisibility() == View.INVISIBLE){
                    container_fc.setVisibility(View.VISIBLE);
                    findViewById(R.id.flight_info).setVisibility(View.VISIBLE);
                    findViewById(R.id.container_mission_type).setVisibility(View.INVISIBLE);
                    findViewById(R.id.container_mission_setting).setVisibility(View.INVISIBLE);
                    // 임무 불러오기 및 초기화 버튼 invisible
                    findViewById(R.id.container_function).setVisibility(View.INVISIBLE);
                }
            }else{
                // 임무 정보 visible
                if(container_fc.getVisibility() == View.VISIBLE){
                    container_fc.setVisibility(View.INVISIBLE);
                    findViewById(R.id.flight_info).setVisibility(View.INVISIBLE);
                    findViewById(R.id.container_mission_type).setVisibility(View.VISIBLE);
                    findViewById(R.id.container_mission_setting).setVisibility(View.VISIBLE);
                    // 임무 불러오기 및 초기화 버튼 invisible
                    findViewById(R.id.container_function).setVisibility(View.VISIBLE);
                }
            }

            // 7. 자동복귀 여부 확인
            if(status.is_going_home){
                if(btn_rtl_cancel.getVisibility() != View.VISIBLE){
                    btn_rtl_cancel.setVisibility(View.VISIBLE);
                    btn_flight_return_home.setVisibility(View.INVISIBLE);

                    // 임무 완료 정보 업데이트 후 삭제
                    if(m_mission_file != null){
                        m_mission_file.updateMission(m_context);
                        m_mission_file = null;

                        // 업데이트 정보 메모리로 복사
                        DroneApplication.setMissionFlightHistory(m_context);
                    }
                }
            }else if(btn_rtl_cancel.getVisibility() == View.VISIBLE){
                btn_rtl_cancel.setVisibility(View.INVISIBLE);
                btn_flight_return_home.setVisibility(View.VISIBLE);
            }

            m_map_view.invalidate();
        }
    }

    /**
     * 이벤트 버스 설정
     */
    private void setEventManager() {
        RxEventBus.getInstance().getDroneStatusObserver().subscribe(
                new DefaultObserver<Integer>() {
                    @Override
                    public void onNext(Integer status) {
                        runOnUiThread(() -> {
                            operationDroneStatus(status);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                }
        );

        RxEventBus.getInstance().getMessageObserver().subscribe(
                new DefaultObserver<EMessage>() {
                    @Override
                    public void onNext(EMessage msg) {
                        runOnUiThread(() -> {
                            operationMessage(msg);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                }
        );
    }

    /**
     * 드론 상태 이벤트 처리
     * @param status
     */
    private void operationDroneStatus(Integer status) {
        switch (status)
        {
            case RxEventBus.DRONE_STATUS_RETURN_HOME_FAIL:
                // 실패 팝업...
                break;
            case RxEventBus.DRONE_STATUS_RETURN_HOME_SUCCESS:
                m_mission_file = null;
                break;
            case RxEventBus.DRONE_STATUS_RETURN_HOME_CANCEL_FAIL:
                Log.e(TAG, "DRONE_STATUS_RETURN_HOME_CANCEL_FAIL");
                break;
            case RxEventBus.DRONE_STATUS_RETURN_HOME_CANCEL_SUCCESS:
                break;
            case RxEventBus.DRONE_MISSION_UPLOAD_FAIL:
                m_container_progress.setVisibility(INVISIBLE);
                break;
            case RxEventBus.DRONE_MISSION_UPLOAD_SUCCESS:
                m_container_progress.setVisibility(INVISIBLE);
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new MissionStart(m_context)));
                break;
            case RxEventBus.DRONE_MISSION_START_FAIL:
                break;
            case RxEventBus.DRONE_MISSION_START_SUCCESS:
                // 짐벌 각도 수정
                DroneApplication.getDroneInstance().setGimbalRotate(-90);
                // 비행경로 다시 세팅 - 임무 경로보다 상위로 올라오게..
                m_map_view.getOverlayManager().remove(flying_line);
                m_map_view.getOverlayManager().add(flying_line);

                // 드론 아이콘 최상위로
                m_map_view.getOverlayManager().remove(marker_drone_location);
                m_map_view.getOverlays().add(marker_drone_location);

                // 비행정보 생성
                if(m_mission_file != null) m_mission_file.setMissinoId(m_context);
                break;
            case RxEventBus.MISSION_INITIALIZE:
                clearMission();
                break;
            case RxEventBus.DRONE_CAMERA_CAPTURED: {
                Marker marker = new Marker(m_map_view);
                marker.setIcon(ContextCompat.getDrawable(m_context, R.mipmap.ico_mission_5));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setPosition(marker_drone_location.getPosition());
                marker.setOnMarkerClickListener((marker1, mapView) -> true);
                captured_markers.add(marker);
                m_map_view.getOverlays().add(marker);
                m_map_view.invalidate();

                // 사진 다운로드
                //if(DroneApplication.getMediaDownloader() != null) DroneApplication.getMediaDownloader().startDownload();
            }
                break;
            case RxEventBus.DRONE_CAMERA_RECORDING:
                String recording_time = DroneApplication.getDroneInstance().getRecordingTime();
                if(recording_time != null) tv_flight_record_time.setText(recording_time);
                break;
            case RxEventBus.DRONE_CAMERA_START_RECORDING:
                btn_flight_record.setBackground(ContextCompat.getDrawable(m_context, R.drawable.btn_recording_selector));
                break;
            case RxEventBus.DRONE_CAMERA_STOP_RECORDING:
                btn_flight_record.setBackground(ContextCompat.getDrawable(m_context, R.drawable.btn_record_selector));
                tv_flight_record_time.setText("00:00");
                DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            case RxEventBus.DRONE_CAMERA_MODE_SHOOT_PHOTO:
                btn_flight_shoot.setVisibility(View.VISIBLE);
                btn_flight_record.setVisibility(View.INVISIBLE);
                tv_flight_record_time.setVisibility(View.INVISIBLE);

                btn_flight_select_movie.setVisibility(View.VISIBLE);
                btn_flight_select_shoot.setVisibility(View.INVISIBLE);
                break;
            case RxEventBus.DRONE_CAMERA_MODE_RECORD_VIDEO:
                btn_flight_shoot.setVisibility(View.INVISIBLE);
                btn_flight_record.setVisibility(View.VISIBLE);
                tv_flight_record_time.setVisibility(View.VISIBLE);

                btn_flight_select_movie.setVisibility(View.INVISIBLE);
                btn_flight_select_shoot.setVisibility(View.VISIBLE);
                break;
            case RxEventBus.DRONE_COMPASS_STATE_NOT_CALIBRATING:
                break;
            case RxEventBus.DRONE_COMPASS_CALIBRATE:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new CompassCalibration(m_context)));
                break;
            case RxEventBus.DRONE_STATUS_DISCONNECT:
                // 마커 처리
                marker_home_location.setVisible(false);
                break;
        }
    }

    /**
     * 이벤트 메세지 처리
     * @param msg
     */
    private void operationMessage(EMessage msg) {
        switch (msg.getType())
        {
            case EMessage.GEO_JSON_FILE_PATH:
//                clearMission();
//                GeoJson geo = Geo.getInstance().getGeoInfo(msg.getMessage());
//
//                if(geo.getGeoType() == GeoJson.GEO_TYPE_LINE){
//                    // 버튼 정보 변경
//                    btn_waypoint_mission.setSelected(true);
//                    btn_polygon_mission.setSelected(false);
//
//                    setPointsFromFile(geo.getCoordinates(), geo.getCoordinates().size());
//
//                    // 임무경로에 포인트 추가
//                    mission_line.setPoints(geo.getCoordinates());
//                    setMapCenter(new RectD(geo.getCoordinates()));
//                    setMissionPolygon();
//                }else if(geo.getGeoType() == GeoJson.GEO_TYPE_POLYGON){
//                    // 버튼 정보 변경
//                    btn_waypoint_mission.setSelected(false);
//                    btn_polygon_mission.setSelected(true);
//
//                    setPointsFromFile(geo.getCoordinates(), geo.getCoordinates().size() -1);
//
//                    // polygon에 포인트 추가
//                    setMapCenter(new RectD(geo.getCoordinates()));
//                    setMissionPolygon();
//                }
                setMission(msg.getMessage());
                break;
        }
    }

    private void setMission(String folder_path) {
        clearMission();

        // 1. 저장되어 있는 파일 불러오기
        // 파일목록 불러오기
        List<File> files = Geo.getInstance().getGeojsonFiles(folder_path, false);
        int i = 1;
        for(File file : files){
            GeoJson gson = Geo.getInstance().getGeoInfo(file.getAbsolutePath());
            Polygon polygon = new Polygon();
            // 비행기록과 비교해서 배경색 설정
            int diff = DroneApplication.checkMissionFlightHistory(file.getAbsolutePath());
            if(diff < 0) polygon.setFillColor(Color.argb(60, 0, 255, 0));
            else if(diff == 0) polygon.setFillColor(Color.argb(60, 255, 0, 0));
            else polygon.setFillColor(Color.argb(60, 255, 127, 0));

            polygon.setStrokeWidth(1.0f);
            polygon.setPoints(gson.getBound());

            m_map_view.getOverlayManager().add(polygon);
            m_waypoint_areas.add(polygon);

            Marker m = getMissionCenterMarker(gson.getCenter(), String.valueOf(i++));

            m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    clearMission();
                    m_mission_file = new MissionHistory(0, (String)marker.getRelatedObject());
                    GeoJson gson = Geo.getInstance().getGeoInfo(m_mission_file.m_filepath);
                    setPointsFromFile(gson.getCoordinates(), gson.getCoordinates().size());

                    mission_line.setPoints(gson.getCoordinates());
                    setMapCenter(new RectD(gson.getCoordinates()));
                    setMissionPolygon();

                    tv_mission_file.setText(m_mission_file.m_filepath.substring(m_mission_file.m_filepath.lastIndexOf("/")+1));

                    // 임무경로에 복귀 지점 포인트 추가
                //    EnordWaypointMission mission = new EnordWaypointMission();
                 //   mission.createWaypointMission(getMissonWaypoint(), marker_home_location.getPosition(), 10, 30);
                    return false;
                }
            });

            m.setRelatedObject(file.getAbsolutePath());
            m_waypoint_centers.add(m);
        }

        // 3. 마커 추가
        for(Marker marker : m_waypoint_centers){
            m_map_view.getOverlays().add(marker);
        }

        // 4. map center 이동 및 zoom 조절
        setMapCenter(m_waypoint_centers.get(0).getPosition(), 14);
        m_map_view.invalidate();
    }

    private void runOnUiThread(Runnable action) {
        messageHandler.post(action);
    }

    /**
     * 주어진 bound의 크기를 기준으로
     * @param rect
     */
    private void setMapCenter(RectD rect){
        IMapController mapController = m_map_view.getController();
        mapController.setCenter(rect.getCenter());
        mapController.setZoom(rect.getZoomLevel());
    }

    private void setMapCenter(GeoPoint point, int zoom_level){
        IMapController mapController = m_map_view.getController();
        mapController.setCenter(point);
        mapController.setZoom(zoom_level);
    }

    /**
     * 파일로 부터 생성된 좌표에서 비행경로 중에 높은 지점이 있으면 좌표를 추가한다.
     * @param points
     * @param size
     */
    private void setPointsFromFile(List<GeoPoint> points, int size){
        GeoPoint inter_point = null;

        // 이륙지점과 첫번째 지점 중간 체크 해서 추가
        DroneApplication.getInterdPoint().clear();
        if(marker_home_location.getPosition().getLatitude() > 0.1 || marker_home_location.getPosition().getLatitude() > 0.1) {
            inter_point = getInterPoint(marker_home_location.getPosition(), points.get(0));
            if (inter_point != null) {
                selected_points.add(inter_point);
                DroneApplication.getInterdPoint().add(selected_points.size()-1);
            }
        }

        // 나머지 지점들 확인
        for(int i = 0; i < size; i++){
            GeoPoint point = points.get(i);
            selected_points.add(point);

            // 다음 지점간에 고도가 10m 이상 높은 지점이 존재하면 중간에 웨이포인트 추가함..
            if(i < (size - 1)) {
                GeoPoint next = points.get(i+1);
                inter_point = getInterPoint(point, next);

                if(inter_point != null){
                    selected_points.add(inter_point);
                    DroneApplication.getInterdPoint().add(selected_points.size()-1);
                }
            }
        }

        // 이륙지점과 마지막 지점 비교해서 추가
        if(marker_home_location.getPosition().getLatitude() > 0.1 || marker_home_location.getPosition().getLatitude() > 0.1) {
            inter_point = getInterPoint(points.get(size-1), marker_home_location.getPosition());
            if (inter_point != null) {
                selected_points.add(inter_point);
                DroneApplication.getInterdPoint().add(selected_points.size()-1);
            }
        }
    }

    private GeoPoint getInterPoint(GeoPoint p1, GeoPoint p2){
        int min_height = (int)(Math.min(p1.getAltitude(), p2.getAltitude()));
        return Geo.getInstance().getHigherPoint(p1, p2, min_height+10);
    }

    /**
     * 각 지점의 고도를 계산한 값을 반환한다.
     * @return
     */
    private List<GeoPoint> getMissonWaypoint(){
        List<GeoPoint> mission_points = new ArrayList<>();
        float altitude = Float.parseFloat(mission_flight_altitude.getText().toString());
        if(btn_waypoint_mission.isSelected()){
            for(GeoPoint point : selected_points){
                mission_points.add(new GeoPoint(point.getLatitude(), point.getLongitude(), point.getAltitude() + altitude));
            }
        }else{
            // 비행경로 생성
        }

        // 각 지점의 표고값 적용이 되어 있지 않으면.
        Geo.getInstance().getElevations(mission_points);

        return mission_points;
    }

    private void ReverseWaypoint(){
        Collections.reverse(selected_points);

        // 중간지점 인덱스값을 reverse 된 값으로 수정
        for(int i = 0; i < DroneApplication.getInterdPoint().size(); i++){
            DroneApplication.getInterdPoint().set(i,selected_points.size() - (1 + DroneApplication.getInterdPoint().get(i)));
        }
        setMissionPolygon();
        m_map_view.invalidate();
    }
}
