package kr.co.enord.dji.popup;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import dji.common.product.Model;
import io.reactivex.observers.DisposableObserver;
import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.R;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;

public class CompassCalibration extends ConstraintLayout implements View.OnClickListener{

    private Context m_context;
    TextView tv_compass_calibration_operation;
    ImageView iv_compass_calibration;
    private Handler handler_ui;                                 // UI 업데이트 핸들러
    int model_type = 0;

    public CompassCalibration(Context context) {
        super(context);

        m_context = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.popup_compass_calibration, this, true);

        initUI();
    }

    @Override
    protected void onAttachedToWindow() {
        handler_ui = new Handler(Looper.getMainLooper());
        RxEventBus.getInstance().getDroneStatusObserver().subscribe(m_observer);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        if(handler_ui != null) handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;
        super.onDetachedFromWindow();
    }

    protected void initUI(){

        TextView tv_compass_calibration_title = findViewById(R.id.tv_compass_calibration_title);
        tv_compass_calibration_title.setText("간섭 대상으로부터 멀어지신 후 약 1.5m 이상의 거리를 확보해주세요.");

        tv_compass_calibration_operation = findViewById(R.id.tv_compass_calibration_operation);
        tv_compass_calibration_operation.setText("기체를 360°로 수평 회전해 주세요.");

        Model drone = DroneApplication.getDroneInstance().getAircaftModel();
        iv_compass_calibration  = findViewById(R.id.iv_compass_calibration);

        if(drone == Model.MAVIC_2 || drone == Model.MAVIC_2_ENTERPRISE || drone == Model.MAVIC_2_ENTERPRISE_DUAL || drone == Model.MAVIC_2_PRO
                || drone == Model.MAVIC_2_ZOOM || drone == Model.MAVIC_AIR || drone == Model.MAVIC_PRO){
            iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.mavic_horizontal));
            model_type = 0;
        }else if(drone == Model.PHANTOM_3_ADVANCED || drone == Model.PHANTOM_3_PROFESSIONAL || drone == Model.PHANTOM_3_STANDARD || drone == Model.Phantom_3_4K || drone == Model.PHANTOM_4
                || drone == Model.PHANTOM_4_PRO || drone == Model.PHANTOM_4_ADVANCED || drone == Model.PHANTOM_4_PRO_V2 || drone == Model.PHANTOM_4_RTK || drone == Model.P_4_MULTISPECTRAL){
            iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.phantom_horizontal));
            model_type = 1;
        }else if(drone == Model.INSPIRE_1 || drone == Model.INSPIRE_1_PRO || drone == Model.INSPIRE_1_RAW || drone == Model.INSPIRE_2 ){
            iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.inspire_horizontal));
            model_type = 2;
        }else{
            iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.matrice_horizontal));
            model_type = 3;
        }

        findViewById(R.id.btn_callibration_cancel).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_callibration_cancel:
                if(DroneApplication.getDroneInstance() != null) DroneApplication.getDroneInstance().stopCompassCalibration();
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
        }
    }

    DisposableObserver<Integer> m_observer = new DisposableObserver<Integer>() {
        @Override
        public void onNext(Integer status) {
            if (handler_ui != null) {
                handler_ui.post(() -> {
                    switch (status) {
                        case RxEventBus.DRONE_COMPASS_STATE_VERTICAL:
                            if(model_type == 0) iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.mavic_vertical));
                            else if(model_type == 1) iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.phantom_vertical));
                            else if(model_type == 2) iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.inspire_vertical));
                            else  iv_compass_calibration.setImageDrawable(ContextCompat.getDrawable(m_context, R.mipmap.matrice_vertical));

                            tv_compass_calibration_operation.setText("기체를 360°로 수직 회전해 주세요.");
                            break;
                        case RxEventBus.DRONE_COMPASS_CALIBRATION_FAIL:
                            RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                            RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new FailCalibration(m_context, RxEventBus.DRONE_COMPASS_CALIBRATE)));
                            break;
                        case RxEventBus.DRONE_COMPASS_CALIBRATION_SUCCESS:
                            RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                            RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(new SuccessCalibration(m_context, RxEventBus.DRONE_COMPASS_CALIBRATE)));
                            break;
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
