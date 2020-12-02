package kr.co.enord.dji.popup;

import android.app.Service;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import kr.co.enord.dji.R;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;

public class ConnectDrone extends ConstraintLayout implements View.OnClickListener{
    public ConnectDrone(Context context) {
        super(context);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.popup_ok, this, true);
        initUI();
    }

    protected void initUI(){
        TextView text = findViewById(R.id.tv_popup_ok_content);
        text.setText("드론이 연결되었는지 확인해주세요.");

        ConstraintLayout layout = findViewById(R.id.container_popup_ok);
        ConstraintLayout.LayoutParams params =  (ConstraintLayout.LayoutParams)layout.getLayoutParams();
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