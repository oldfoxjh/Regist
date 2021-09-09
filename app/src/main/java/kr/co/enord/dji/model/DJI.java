package kr.co.enord.dji.model;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.StorageState;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.CompassCalibrationState;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.utils.ToastUtils;

public class DJI {
    boolean is_flying;              /** 드론 비행상태 */
    boolean is_going_home;          /** 드론 자동복귀 */
    double drone_latitude;          /** 드론 위도 */
    double drone_longitude;         /** 드론 경도 */
    float drone_altitude;           /** 드론 고도 */

    float velocity_x = 0.0f;        /** 드론 x축 속도 */
    float velocity_y = 0.0f;        /** 드론 y축 속도 */
    float velocity_z = 0.0f;        /** 드론 z축 속도 */

    double home_latitude;           /** 자동복귀지점 위도 */
    double home_longitude;          /** 자동복귀지점 경도 */
    boolean home_set = false;       /** 자동복귀지점 설정여부 */

    /**
     * 드론 저장장치 정보
     */
    String recording_time;
    boolean is_storage_sdcard;
    boolean is_recording = false;

    int max_focal_length = 0;
    int target_waypoint_index = 0;

    /**
     * 드론 상태정보 반환
     * @return 드론 상태정보
     */
    public DroneStatus getDroneStatus() {
        DroneStatus status = null;
        try{
            FlightController flight_controller = getFlightController();

            status = new DroneStatus();
            status.is_flying = is_flying;
            status.drone_latitude = drone_latitude;
            status.drone_longitude = drone_longitude;
            status.drone_altitude = drone_altitude;

            status.velocity_x = velocity_x;
            status.velocity_y = velocity_y;
            status.velocity_z = velocity_z;

            status.home_latitude = home_latitude;
            status.home_longitude = home_longitude;
            status.home_set = home_set;
            status.heading = flight_controller.getCompass().getHeading();
            status.is_going_home = is_going_home;
        }catch (NullPointerException ex){
        }

        return  status;
    }

    /**
     * 드롬 모델명을 반환한다.
     * @return DJI 드론 모델
     */
    public Model getAircaftModel(){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            return product.getModel();
        }

        return Model.UNKNOWN_AIRCRAFT;
    }

    /**
     * 드론이 비행중인지 여부 확인
     * @return
     */
    public boolean isFlying()
    {
        return is_flying;
    }

    /**
     *  미디어 저장 위치 확인
     * @return
     */
    public boolean isStorageSDCard(){ return is_storage_sdcard; }

    public int targetWaypointIndex()
    {
        return target_waypoint_index;
    }
    /**
     * 드론 최대비행고도를 설정한다.
     */
    public void setMaxFlightHeight(int height){
        try{

            FlightController flight_controller = getFlightController();
            flight_controller.setMaxFlightHeight(height, djiError -> {
                if(djiError != null){
                    ToastUtils.showToast(djiError.getDescription());
                }
            });
        }catch (NullPointerException ex){
        }
    }

    //region 자동이륙,자동복귀
    /**
     * 자동이륙 명령
     */
    public void startTakeoff(){
        try{
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null && product.isConnected()) {
                FlightController flight_controller = getFlightController();
                flight_controller.startTakeoff(djiError -> {
                    if(djiError != null){
                        RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_TAKE_OFF_FAIL);
                    }else{
                        // UI 변경을 위한 메세지
                        RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_TAKE_OFF_SUCCESS);
                    }
                });
            }
        }catch (NullPointerException ex){

        }
    }

    /**
     * 자동착륙 명령
     */
    public void startLanding(){
        try{
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null && product.isConnected()) {
                FlightController flight_controller = getFlightController();
                flight_controller.startLanding(djiError -> {
                    if(djiError != null){
                        RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_LANDING_FAIL);
                    }else{
                        RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_LANDING_SUCCESS);
                    }
                });
            }
        }catch (NullPointerException ex){

        }
    }

    /**
     * 자동착륙 취소
     */
    public void cancelLanding(){
        try{
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null && product.isConnected()) {
                FlightController flight_controller = getFlightController();
                flight_controller.cancelLanding(djiError -> {
                    if(djiError != null){
                        //DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.landing_cancel_fail, djiError.getDescription()));
                    }else{
                        // UI 변경을 위한 메세지
                        //DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.CANCEL_LANDING_SUCCESS, null));
                    }
                });
            }
        }catch (NullPointerException ex){

        }
    }

    /**
     * 자동복귀 시작
     */
    public void startGoHome(){
        try{
            FlightController flight_controller = getFlightController();
            flight_controller.startGoHome(djiError -> {
                if(djiError != null){
                    RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_RETURN_HOME_FAIL);
                }else{
                    // UI 변경을 위한 메세지
                    RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_RETURN_HOME_SUCCESS);
                }
            });
        }catch (NullPointerException ex){
            RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_RETURN_HOME_FAIL);
        }
    }

    /**
     * 자동복귀 취소
     */
    public void cancelGoHome(){
        try{
            FlightController flight_controller = getFlightController();
            flight_controller.cancelGoHome(djiError -> {
                // 자동복귀 취소
                if(djiError != null){
                    RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_RETURN_HOME_CANCEL_FAIL);
                }else{
                    // UI 변경을 위한 메세지
                    RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_RETURN_HOME_CANCEL_SUCCESS);
                }
            });
        }catch (NullPointerException ex){
            RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_RETURN_HOME_CANCEL_FAIL);
        }
    }
    //endregion

    //region 임무
    /**
     * 설정된 임무를 확인
     */
    public String checkMission(WaypointMission mission){
        // 설정된 임무에 대한 확인
        DJIError  _error = MissionControl.getInstance().getWaypointMissionOperator().loadMission(mission);
        if(_error != null){
            ToastUtils.setResultToToast("체크미션 실패 : " + _error.getDescription());
            return _error.getDescription();
        }
        return null;
    }

    /**
     * 임무를 드론에 업로드 한다.
     */
    public void uploadMission(){
        // 임무 업로드
        WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();
        mission_operator.addListener(mission_notification_listener);

        mission_operator.uploadMission(djiError -> {
            if(djiError != null) {
                // 임무 업로드 실패
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_MISSION_UPLOAD_FAIL);
                ToastUtils.setResultToToast("업로드 실패 : " + djiError.getDescription());
            }else{
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_MISSION_UPLOAD_SUCCESS);
            }
        });
    }

    /**
     * 임무를 시작한다.
     */
    public void startMission() {
        // 임무 업로드
        WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();
        mission_operator.startMission(djiError -> {
            if(djiError != null) {
                // 임무 시작 실패
                ToastUtils.setResultToToast("체크미션 실패 : " + djiError.getDescription());
                WaypointMissionOperator mission_operator1 = MissionControl.getInstance().getWaypointMissionOperator();
                mission_operator1.removeListener(mission_notification_listener);

                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_MISSION_START_FAIL);
            }else{
                // 임무 시작 성공
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_MISSION_START_SUCCESS);
            }
        });
    }

    /**
     * 임무 상태를 반환한다.
     * @return
     */
    public WaypointMissionState getMissionState() {
        WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();
        return mission_operator.getCurrentState();
    }
    //endregion

    //region 카메라
    /**
     * 카메라 동작 설정값을 드론으로부터 가져온다.
     */
    public void getCameraMode(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();

        if (_product != null && _product.isConnected() && _product.getCamera() != null) {
            _product.getCamera()
                    .getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
                        @Override
                        public void onSuccess(SettingsDefinitions.CameraMode mode) {
                            int camera_mode = (mode == SettingsDefinitions.CameraMode.SHOOT_PHOTO) ? RxEventBus.DRONE_CAMERA_MODE_SHOOT_PHOTO :
                                              (mode == SettingsDefinitions.CameraMode.RECORD_VIDEO) ? RxEventBus.DRONE_CAMERA_MODE_RECORD_VIDEO : RxEventBus.DRONE_CAMERA_MODE_MEDIA_DOWNLOAD;
                            RxEventBus.getInstance().sendDroneStatus(camera_mode);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            ToastUtils.showToast(djiError.getDescription());
                        }
                    });
        }
    }

    /**
     * 카메라 동작을 설정한다.
     * @param mode : SHOOT_PHOTO, RECORD_VIDEO
     */
    public void setCameraMode(SettingsDefinitions.CameraMode mode){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected() && product.getCamera() != null) {
            product.getCamera().setMode(mode, djiError -> {
                if(djiError != null){
                    // 오류 메세지 Toast로..
                    ToastUtils.setResultToToast(djiError.getDescription());
                    return;
                }

                int camera_mode = (mode == SettingsDefinitions.CameraMode.SHOOT_PHOTO) ? RxEventBus.DRONE_CAMERA_MODE_SHOOT_PHOTO :
                        (mode == SettingsDefinitions.CameraMode.RECORD_VIDEO) ? RxEventBus.DRONE_CAMERA_MODE_RECORD_VIDEO : RxEventBus.DRONE_CAMERA_MODE_MEDIA_DOWNLOAD;
                RxEventBus.getInstance().sendDroneStatus(camera_mode);
            });
        }
    }

    /**
     * 비디오 촬영을 시작한다.
     */
    public void startRecordVideo(){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected() && product.getCamera() != null) {
            product.getCamera().startRecordVideo(djiError -> {
                        if(djiError != null) {
                            ToastUtils.showToast(djiError.getDescription());
                            return;
                        }

                        RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_CAMERA_START_RECORDING);
                    });
        }
    }

    /**
     * 비디오 촬영을 종료한다.
     */
    public void stopRecordVideo(){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected() && product.getCamera() != null) {
            product.getCamera().stopRecordVideo(djiError -> {
                        if(djiError != null) {
                            ToastUtils.showToast(djiError.getDescription());
                            return;
                        }

                        RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_CAMERA_STOP_RECORDING);
                    });
        }
    }

    /**
     * 촬영시간을 가져온다.
     * @return 촬영시간 mm:ss
     */
    public String getRecordingTime(){
        return recording_time;
    }

    /**
     * 한장 또는 카메라 촬영 설정에 따라 촬영을 시작한다.
     */
    public void startShootPhoto(){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected() && product.getCamera() != null) {
            product.getCamera().startShootPhoto(djiError -> {
                        if (djiError != null) ToastUtils.showToast(djiError.getDescription());
                    });
        }
    }

    /**
     * 사진 촬영을 멈춘다.
     */
    public void stopShootPhoto(){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected() && product.getCamera() != null) {
            product.getCamera().stopShootPhoto(djiError -> {
                        if (djiError != null) ToastUtils.showToast(djiError.getDescription());
                    });
        }
    }

    public boolean isRecording(){
        return is_recording;
    }

    /**
     * 카메라 인터벌 세팅
     */
    public void setPhotoTimeIntervalSetting(int interval){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            Camera camera = DroneApplication.getDrone().getCamera();
            if(camera == null) {
                ToastUtils.showToast("드론과 연결되어 있는 카메라가 없습니다.");
                return;
            }

            if(interval == 0){
                SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE;
                camera.setShootPhotoMode(photoMode, djiError -> {
                    if (djiError != null) {
                        if (djiError != null) ToastUtils.showToast(djiError.getDescription());
                    }
                });
            }else{ // 카메라 모드 인터벌
                SettingsDefinitions.PhotoTimeIntervalSettings mSettings = new SettingsDefinitions.PhotoTimeIntervalSettings(255, interval);
                DJISDKManager.getInstance().getProduct().getCamera().setPhotoTimeIntervalSettings(mSettings, djiError -> {
                    if (djiError != null) {
                        if (djiError != null) ToastUtils.showToast(djiError.getDescription());
                    }
                });
            }
        }
    }

    /**
     * 카메라 줌 지원 확인
     */
    public boolean isOpticalZoomSupported()
    {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            Camera camera = DroneApplication.getDrone().getCamera();

            camera.getOpticalZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.OpticalZoomSpec>() {
                @Override
                public void onSuccess(SettingsDefinitions.OpticalZoomSpec opticalZoomSpec) {
                    max_focal_length = opticalZoomSpec.getMaxFocalLength();
                    Log.e("DJI", "max_focal_length : " + max_focal_length);
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Log.e("DJI", "forcal length : " + djiError.getDescription());
                }
            });

            return camera.isOpticalZoomSupported();
        }

        return false;
    }

    public  void setMaxOpticalZoomFocalLength()
    {
        if(max_focal_length == 0) return;

        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            Camera camera = DroneApplication.getDrone().getCamera();
            camera.setOpticalZoomFocalLength(max_focal_length, djiError -> {
                if(djiError != null) {
                    ToastUtils.showToast(djiError.getDescription());
                }
            });
        }
    }


    //endregion

    //region 짐벌
    /**
     * 짐벌 Pictch 설정
     * @param pitch
     */
    public void setGimbalRotate(float pitch){

        BaseProduct product = DJISDKManager.getInstance().getProduct();

        if(product != null && product instanceof Aircraft)
        {
            List<Gimbal> gimbals = ((Aircraft) product).getGimbals();

            if(gimbals != null) {
                for (Gimbal gimbal : gimbals) {
                    gimbal.setMode(GimbalMode.FPV, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                            } else {
                            }
                        }
                    });

                    Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).pitch(pitch).yaw(Rotation.NO_ROTATION).roll(Rotation.NO_ROTATION).time(2);
                    gimbal.rotate(builder.build(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                // 짐벌 설정 실패
                                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_GIMBAL_SETTING_FAIL);
                            } else {
                            }
                        }
                    });
                }
            }
        }
    }
    //endregion

    //region 컴파스
    public void startCompassCalibration() {
        try {
            Compass compass = getFlightController().getCompass();
            compass.startCalibration(djiError -> {
                if(djiError != null){
                    ToastUtils.showToast(djiError.getDescription());
                    return;
                }

                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_COMPASS_CALIBRATE);
            });

        }catch (NullPointerException ex){

        }
    }

    public void stopCompassCalibration() {
        try {
            Compass compass = getFlightController().getCompass();
            compass.stopCalibration(djiError -> {
                if(djiError != null){
                    Log.e("Mission", "stopCalibration : " + djiError.getDescription());
                }
            });

        }catch (NullPointerException ex){

        }
    }

    private CompassCalibrationState.Callback compass_state_callback = compassCalibrationState -> {
            if(compassCalibrationState == CompassCalibrationState.HORIZONTAL){
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_COMPASS_STATE_HORIZONTAL);
            }else if(compassCalibrationState == CompassCalibrationState.VERTICAL){
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_COMPASS_STATE_VERTICAL);
            }else if(compassCalibrationState == CompassCalibrationState.FAILED){
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_COMPASS_CALIBRATION_FAIL);
            }else if(compassCalibrationState == CompassCalibrationState.SUCCESSFUL){
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_COMPASS_CALIBRATION_SUCCESS);
            }
    };
    //endregion

    //region IMU

    //endregion

    /**
     * 드론 데이터 리스너
     */
    public boolean setDroneDataListener(){
        try {
            FlightController flight_controller = getFlightController();
            flight_controller.setStateCallback(status_callback);
            List<Camera> cameras = DJISDKManager.getInstance().getProduct().getCameras();

            if (cameras == null) return false;

            if (cameras != null) {
                for (Camera camera : cameras) {
                    camera.setSystemStateCallback(camera_state_callback);
                    camera.setStorageStateCallBack(camera_storage_callback);
                }
            }

            flight_controller.getCompass().setCalibrationStateCallback(compass_state_callback);
        }catch (NullPointerException ex){
            return  false;
        }

        return true;
    }

    /**
     * 카메라 상태 Callback
     */
    public SystemState.Callback camera_state_callback= systemState -> {
        if(systemState.isRecording()) {
            recording_time = ConvertSecond(systemState.getCurrentVideoRecordingTimeInSeconds());
            RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_CAMERA_RECORDING);
            is_recording = true;
            return;
        }else if(systemState.isStoringPhoto()){
            // 카메라 촬영후 저장
            RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_CAMERA_CAPTURED);
        }else if(!systemState.isRecording() && recording_time != null) {
            recording_time = null;
            RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_CAMERA_STOP_RECORDING);
        }

        is_recording = false;
    };

    /**
     * 카메라 저장정보 Callback
     */
    public StorageState.Callback camera_storage_callback= storageState -> {
        is_storage_sdcard = (storageState.getStorageLocation() == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE) ? true : false;
    };

    /**
     * 드론 임무 리스너
     */
    private WaypointMissionOperatorListener mission_notification_listener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {
        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
            if(executionEvent.getProgress() != null) {
                RxEventBus.getInstance().sendDroneStatus(RxEventBus.MISSION_UPDATE_WAYPOINT);
                target_waypoint_index = executionEvent.getProgress().targetWaypointIndex;
            }
        }

        @Override
        public void onExecutionStart() {
        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {

        }
    };

    private FlightControllerState.Callback status_callback = current_state -> {

        drone_latitude = current_state.getAircraftLocation().getLatitude();
        drone_longitude = current_state.getAircraftLocation().getLongitude();
        drone_altitude = current_state.getAircraftLocation().getAltitude();

        velocity_x = Math.abs(current_state.getVelocityX());
        velocity_y = Math.abs(current_state.getVelocityY());
        velocity_z = current_state.getVelocityZ();

        home_latitude = current_state.getHomeLocation().getLatitude();
        home_longitude = current_state.getHomeLocation().getLongitude();
        home_set = current_state.isHomeLocationSet();

        is_flying =  current_state.isFlying();
        is_going_home = current_state.isGoingHome();
    };

    private FlightController getFlightController() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();

        if(product != null && product instanceof Aircraft)
        {
            return ((Aircraft) product).getFlightController();
        }

        throw new NullPointerException();
    }

    /**
     * 초를 시분초로 변경
     * @param second
     * @return
     */
    private String ConvertSecond(int second) {
        int min = (second / 60);
        int hour = min / 60;
        int sec = second % 60;
        min = min % 60;

        return (hour > 0) ? String.format("%02d:%02d:%02d", hour, min, sec): String.format("%02d:%02d", min, sec);
    }

}
