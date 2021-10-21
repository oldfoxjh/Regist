package kr.co.enord.dji.popup;

import android.app.Service;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.R;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;
import kr.co.enord.dji.utils.ToneUtil;

public class MissionPause extends ConstraintLayout implements View.OnClickListener{
    public MissionPause(Context context) {
        super(context);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.popup_pause, this, true);
        initUI();
    }

    protected void initUI(){
        TextView text = findViewById(R.id.tv_popup_confirm_content);
        text.setText("장애물이 감지되어 임무를 일시정지하였습니다.");
        ConstraintLayout layout = findViewById(R.id.container_popup_confirm);
        LayoutParams params =  (LayoutParams)layout.getLayoutParams();
        params.width = getResources().getDimensionPixelSize(R.dimen.popup_single_line_witdth_type_1);
        params.height = getResources().getDimensionPixelSize(R.dimen.popup_single_line_height_type_1);

        (findViewById(R.id.btn_popup_confirm)).setOnClickListener(this);
        (findViewById(R.id.btn_popup_cancel)).setOnClickListener(this);
        (findViewById(R.id.btn_popup_stop_beep)).setOnClickListener(this);
        setClickable(true);
        ToneUtil.INSTANCE.beep();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_popup_confirm:
                DroneApplication.getDroneInstance().resumeMission();
                ToneUtil.INSTANCE.stopBeep();
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
            case R.id.btn_popup_cancel:
                ToneUtil.INSTANCE.stopBeep();
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
            case R.id.btn_popup_stop_beep:
                ToneUtil.INSTANCE.stopBeep();
                break;
        }
    }
}
