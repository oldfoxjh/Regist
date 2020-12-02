package kr.co.enord.dji.popup;

import android.app.Service;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import kr.co.enord.dji.R;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;

public class CancelCalibration extends ConstraintLayout implements View.OnClickListener{
    public CancelCalibration(Context context, int calibration_type) {
        super(context);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.popup_ok, this, true);
        initUI(calibration_type);
    }

    protected void initUI(int calibration_type){
        TextView text = findViewById(R.id.tv_popup_ok_content);
        if(calibration_type == RxEventBus.DRONE_COMPASS_CALIBRATE){
            text.setText("컴파스 캘리브레이션 취소");
        }else{
            text.setText("취소되었습니다.");
        }

        ConstraintLayout layout = findViewById(R.id.container_popup_ok);
        LayoutParams params =  (LayoutParams)layout.getLayoutParams();
        params.width = getResources().getDimensionPixelSize(R.dimen.popup_single_line_witdth_type_1);
        params.height = getResources().getDimensionPixelSize(R.dimen.popup_single_line_height_type_1);

        (findViewById(R.id.btn_popup_ok)).setOnClickListener(this);

        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_popup_ok:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
        }
    }
}