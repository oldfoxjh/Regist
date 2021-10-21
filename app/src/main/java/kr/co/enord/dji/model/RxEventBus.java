package kr.co.enord.dji.model;

import io.reactivex.subjects.PublishSubject;

public class RxEventBus {

    public static final int DRONE_STATUS_DISCONNECT = 0x00;
    public static final int DRONE_STATUS_CONNECT = 0x01;
    public static final int DRONE_STATUS_ARMING = 0x02;
    public static final int DRONE_STATUS_FLYING = 0x03;
    public static final int DRONE_STATUS_MISSION = 0x04;
    public static final int DRONE_STATUS_DISARM = 0x05;

    public static final int DRONE_STATUS_TAKE_OFF = 0x100;
    public static final int DRONE_STATUS_TAKE_OFF_FAIL = 0x101;
    public static final int DRONE_STATUS_TAKE_OFF_SUCCESS = 0x102;

    public static final int DRONE_STATUS_RETURN_HOME = 0x200;
    public static final int DRONE_STATUS_RETURN_HOME_FAIL = 0x201;
    public static final int DRONE_STATUS_RETURN_HOME_SUCCESS = 0x202;
    public static final int DRONE_STATUS_RETURN_HOME_CANCEL_FAIL = 0x203;
    public static final int DRONE_STATUS_RETURN_HOME_CANCEL_SUCCESS = 0x204;

    public static final int DRONE_STATUS_LANDING = 0x300;
    public static final int DRONE_STATUS_LANDING_FAIL = 0x301;
    public static final int DRONE_STATUS_LANDING_SUCCESS = 0x302;

    public static final int DRONE_MISSION_CHECK_FAIL = 0x400;
    public static final int DRONE_MISSION_UPLOAD_FAIL = 0x401;
    public static final int DRONE_MISSION_UPLOAD_SUCCESS = 0x402;
    public static final int DRONE_MISSION_START_FAIL = 0x403;
    public static final int DRONE_MISSION_START_SUCCESS = 0x404;

    public static final int DRONE_GIMBAL_SETTING_FAIL = 0x500;

    public static final int DRONE_CAMERA = 0x600;
    public static final int DRONE_CAMERA_CAPTURED = 0x601;
    public static final int DRONE_CAMERA_START_RECORDING = 0x602;
    public static final int DRONE_CAMERA_RECORDING = 0x603;
    public static final int DRONE_CAMERA_STOP_RECORDING = 0x604;

    public static final int DRONE_CAMERA_MODE_SHOOT_PHOTO = 0x611;
    public static final int DRONE_CAMERA_MODE_RECORD_VIDEO = 0x612;
    public static final int DRONE_CAMERA_MODE_MEDIA_DOWNLOAD = 0x613;

    public static final int DRONE_COMPASS_CALIBRATE = 0x700;
    public static final int DRONE_COMPASS_STATE_NOT_CALIBRATING = 0x701;
    public static final int DRONE_COMPASS_STATE_HORIZONTAL = 0x702;
    public static final int DRONE_COMPASS_STATE_VERTICAL = 0x703;
    public static final int DRONE_COMPASS_CALIBRATION_SUCCESS = 0x704;
    public static final int DRONE_COMPASS_CALIBRATION_FAIL = 0x705;

    public static final int MISSION_INITIALIZE = 0x1000;
    public static final int MISSION_UPDATE_WAYPOINT = 0x1001;
    public static final int MISSION_PAUSED_BY_VISION_DETECTION = 0x1002;

    private static RxEventBus m_instance;
    private final PublishSubject<Integer> m_status;
    private final PublishSubject<EMessage> m_message;
    private final PublishSubject<ViewWrapper> m_wrapper;

    public static RxEventBus getInstance(){
        if(m_instance == null){
            m_instance = new RxEventBus();
        }

        return  m_instance;
    }

    private RxEventBus(){
        m_status = PublishSubject.create();
        m_wrapper = PublishSubject.create();
        m_message = PublishSubject.create();
    }

    public PublishSubject getViewWrapperObserver(){
        return m_wrapper;
    }
    public PublishSubject getDroneStatusObserver(){
        return m_status;
    }
    public PublishSubject getMessageObserver(){
        return m_message;
    }

    public void sendViewWrapper(ViewWrapper obj){
        m_wrapper.onNext((ViewWrapper) obj);
    }
    public void sendDroneStatus(int status){
        m_status.onNext(status);
    }
    public void sendMessage(EMessage msg){
        m_message.onNext(msg);
    }
}
