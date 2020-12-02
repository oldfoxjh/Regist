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

public class CancelRTL extends ConstraintLayout implements View.OnClickListener{
    public CancelRTL(Context context) {
        super(context);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.popup_confirm_no_title, this, true);
        initUI();
    }

    protected void initUI(){
        TextView text = findViewById(R.id.tv_popup_confirm_content);
        text.setText("홈포인트로 복귀를\n 취소하시겠습니까?");

        ConstraintLayout layout = findViewById(R.id.container_popup_confirm);
        ConstraintLayout.LayoutParams params =  (ConstraintLayout.LayoutParams)layout.getLayoutParams();
        params.width = getResources().getDimensionPixelSize(R.dimen.popup_single_line_witdth_type_2);
        params.height = getResources().getDimensionPixelSize(R.dimen.popup_single_line_height_type_2);

        (findViewById(R.id.btn_popup_confirm)).setOnClickListener(this);
        (findViewById(R.id.btn_popup_cancel)).setOnClickListener(this);

        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_popup_confirm:
                DroneApplication.getDroneInstance().cancelGoHome();
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
            case R.id.btn_popup_cancel:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
        }
    }
}