package kr.co.enord.dji;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.multidex.MultiDex;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.co.enord.dji.api.IApiService;
import kr.co.enord.dji.model.DJI;
import kr.co.enord.dji.model.FlightInfoDBHelper;
import kr.co.enord.dji.model.MissionHistory;
import kr.co.enord.dji.utils.MediaDownload;

/**
 * Main application
 */
public class DroneApplication extends Application {

    public static final String TAG = DroneApplication.class.getName();

    private static Application app = null;
    private static DJI drone = null;
    private static Date connection_date;
    private static IApiService enord = null;
    private static MediaDownload m_media_downloader;
    private static List<MissionHistory> m_mission_history = null;

    private static List<Integer> inter_point_index = null;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        MultiDex.install(this);
        com.secneo.sdk.Helper.install(this);

        app = this;
        enord = IApiService.retrofit.create(IApiService.class);
    }

    /**
     * Application Instance return
     * @return Application Instance
     */
    public static Application getInstance() {
        return DroneApplication.app;
    }

    /**
     * event message-bus return
     * @return event message bus
     */

    /**
     * drone instance return
     * @return
     */
    public static DJI getDroneInstance(){
        if(DroneApplication.drone == null) drone = new DJI();
        return DroneApplication.drone;
    }

    /**
     * 연결된 드론 인스턴스 반환
     * @return
     */
    public static BaseProduct getDrone()
    {
        return DJISDKManager.getInstance().getProduct();
    }

    /**
     * 드론 비행 시작 정보
     * @return
     */
    public static Date getDroneConnectionDate(){ return  connection_date;};
    public static void setDroneConnectionDate(Date date){connection_date = date;}

    /**
     *
     */
    public static IApiService getAPI(){ return enord; }
    public static MediaDownload getMediaDownloader() { return m_media_downloader; }
    public static void setMediaDownloader() { m_media_downloader = new MediaDownload(); }

    /**
     * 임무 실행 정보 목록
     * @param context
     */
    public static void setMissionFlightHistory(Context context){
        if(m_mission_history != null) {
            m_mission_history.clear();
            m_mission_history = null;
        }
        FlightInfoDBHelper sqlite = new FlightInfoDBHelper(context);
        m_mission_history = sqlite.select_mission();
    }

    /**
     * 실행한 임무의 날짜를 반환
     * @return
     */
    public static int checkMissionFlightHistory(String filepath){
        for (MissionHistory history:m_mission_history) {
            if(history.m_filepath.equals(filepath)){
                return history.past_days;
            }
        }
        return -1;
    }

    public static List<Integer> getInterdPoint(){
        if(inter_point_index == null) inter_point_index = new ArrayList<>();
        return inter_point_index;
    }
}