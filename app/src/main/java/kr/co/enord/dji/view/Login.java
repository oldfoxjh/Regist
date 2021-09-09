package kr.co.enord.dji.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

import dji.common.mission.waypoint.WaypointMission;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import io.reactivex.observers.DisposableObserver;
import kr.co.enord.dji.BuildConfig;
import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.R;
import kr.co.enord.dji.api.API;
import kr.co.enord.dji.model.FlightInfoDBHelper;
import kr.co.enord.dji.model.LoginRequestData;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;
import kr.co.enord.dji.utils.AES256Util;
import kr.co.enord.dji.utils.ToastUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends RelativeLayout implements View.OnClickListener {

    private Context m_context;
    private RelativeLayout m_container_progress;


    //--> 로그인 화면에서 드론 연결 처리
    private TextView login_drone_connect_info;
    private Handler handler_ui;                                 // UI 업데이트 핸들러
    //<-- 로그인 화면에서 드론 연결 처리

    public Login(Context context){
        this(context, null);
    }

    public Login(Context context, AttributeSet attrs) {
        super(context, attrs);
        m_context = context;
    }

    /**
     * View가 정상적으로 화면에 추가 되었을 때
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {

        //--> 로그인 화면에서 드론 연결 처리
        handler_ui = new Handler(Looper.getMainLooper());

        // 드론이 연결되어 있으면 버튼 처리
        if(DroneApplication.getDrone() != null){
            login_drone_connect_info.setBackground(ContextCompat.getDrawable(m_context, R.mipmap.top_bg_blue_connect_info));
            login_drone_connect_info.setText(R.string.aircraft_connect);
        }

        RxEventBus.getInstance().getDroneStatusObserver().subscribe(m_observer);


        // DB 테스트
        FlightInfoDBHelper sqlite = new FlightInfoDBHelper(m_context);

       // sqlite.insert_mission("test2", "2020-08-07");
   //    sqlite.update_mission(3, "2020-08-07");

      //  sqlite.check_mission_flight_day("test2");
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {

        if(handler_ui != null) handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;
        super.onDetachedFromWindow();
    }

    /**
     * 초기 화면 설정
     */
    protected void initUI(){
        String versionName = BuildConfig.VERSION_NAME;
        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText("Version " + versionName);

        m_container_progress = findViewById(R.id.container_progress);
        login_drone_connect_info = findViewById(R.id.login_drone_connect_info);
        findViewById(R.id.btn_login).setOnClickListener(this);
    }

    private WaypointMissionOperator waypointMissionOperator;
    private WaypointMission mission;

    @Override
    public void onClick(View v) {
        switch ((v.getId()))
        {
            case R.id.btn_login:
                EditText idText = findViewById(R.id.login_id);
                EditText pwText = findViewById(R.id.login_password);
                LoginRequestData data = new LoginRequestData(idText.getText().toString(),
                        AES256Util.aesEncode(pwText.getText().toString()));
                API.INSTANCE.getCApiService().login(data).enqueue(new Callback<List<kr.co.enord.dji.model.Login>>() {
                    @Override
                    public void onResponse(Call<List<kr.co.enord.dji.model.Login>> call, Response<List<kr.co.enord.dji.model.Login>> response) {
                        if(response.body()!=null && response.body().size() > 0){
                            API.INSTANCE.setLoginInfo(response.body().get(0));
                            processLogin();
                            return;
                        }
                        ToastUtils.showToast("로그인에 실패하였습니다.");
                    }

                    @Override
                    public void onFailure(Call<List<kr.co.enord.dji.model.Login>> call, Throwable t) {
                        ToastUtils.showToast("로그인에 실패하였습니다.");
                    }
                });
                break;
        }
    }

    private  void processLogin()
    {
        m_container_progress.setVisibility(VISIBLE);
        DroneApplication.setMissionFlightHistory(m_context);
        ViewWrapper wrapper = new ViewWrapper(new Mission(m_context));
        //ViewWrapper wrapper = new ViewWrapper(new MissionStart(m_context));
        RxEventBus.getInstance().sendViewWrapper(wrapper);

        m_container_progress.setVisibility(INVISIBLE);
        if(!m_observer.isDisposed()) m_observer.dispose();
    }

    /**
     * 기체 연결 체크 Event-bus
     */
    DisposableObserver<Integer> m_observer = new DisposableObserver<Integer>() {
        @Override
        public void onNext(Integer status) {
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if(status == RxEventBus.DRONE_STATUS_CONNECT){
                            login_drone_connect_info.setBackground(ContextCompat.getDrawable(m_context, R.mipmap.top_bg_blue_connect_info));
                            login_drone_connect_info.setText(R.string.aircraft_connect);
                        }else if(status == RxEventBus.DRONE_STATUS_DISCONNECT){
                            login_drone_connect_info.setBackground(ContextCompat.getDrawable(m_context, R.mipmap.top_bg_gray_connect_info));
                            login_drone_connect_info.setText(R.string.aircraft_not_connect);
                        }
                    }
                });
            }
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
}
