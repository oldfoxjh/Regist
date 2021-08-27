package kr.co.enord.dji.popup;

import android.app.Service;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.JsonObject;

import java.util.ArrayList;

import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.R;
import kr.co.enord.dji.model.EMessage;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;
import kr.co.enord.dji.utils.ToastUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionDownload extends ConstraintLayout implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

//    private GeoPoint centerPoint = null;
    private int targetId = -1;

    public MissionDownload(Context context) {
        super(context);
//        centerPoint = center;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.popup_download_geo_json, this, true);

        initUI(context);
    }

    protected void initUI(Context context){

        RadioGroup type_list = findViewById(R.id.load_shape_file_list);
        type_list.setOnCheckedChangeListener(this);

        (findViewById(R.id.btn_popup_confirm)).setOnClickListener(this);
        (findViewById(R.id.btn_popup_cancel)).setOnClickListener(this);

        DroneApplication.getAPI().targets().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                //목록 만들기
                JsonObject object = response.body();
                ArrayList<String> keys = new ArrayList();
                for(String key: object.keySet()){
                    keys.add(key);
                    RadioButton radio_button = new RadioButton(context);
                    radio_button.setText(key);
                    radio_button.setTag(object.get(key).getAsInt());
                    radio_button.setTextSize(getResources().getDimension(R.dimen.radio_button_font_size));
                    type_list.addView(radio_button);
                }
                if (type_list.getChildCount() > 0){
                    ((RadioButton)type_list.getChildAt(0)).setChecked(true);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                ToastUtils.showToast(t.getLocalizedMessage());
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
            }
        });

        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_popup_confirm:
                RxEventBus.getInstance().sendMessage(new EMessage(EMessage.GEO_JSON_DOWNLOAD_TARGET, String.valueOf(targetId)));
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
            case R.id.btn_popup_cancel:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        targetId = (int)group.findViewById(checkedId).getTag();
    }
}
