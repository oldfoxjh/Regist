package kr.co.enord.dji.popup;

import android.app.Service;
import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.constraintlayout.widget.ConstraintLayout;
import kr.co.enord.dji.R;
import kr.co.enord.dji.model.EMessage;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;
import kr.co.enord.dji.utils.Geo;
import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.util.List;

public class MissionDownload extends ConstraintLayout implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private GeoPoint centerPoint = null;

    public MissionDownload(Context context, GeoPoint center) {
        super(context);
        centerPoint = center;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.popup_download_geo_json, this, true);

        initUI(context);
    }

    protected void initUI(Context context){

        RadioGroup file_list = findViewById(R.id.load_shape_file_list);
        file_list.setOnCheckedChangeListener(this);

        (findViewById(R.id.btn_popup_confirm)).setOnClickListener(this);
        (findViewById(R.id.btn_popup_cancel)).setOnClickListener(this);

        // 파일목록 불러오기
        String folder_path = Environment.getExternalStorageDirectory() + File.separator + "enord";
        //List<File> files = Geo.getInstance().getGeojsonFiles(folder_path);
        List<File> files = Geo.getInstance().getGeoJsonDirectories(folder_path);

        for(File file : files){
            RadioButton radio_button = new RadioButton(context);
            radio_button.setText(file.getName());
            radio_button.setTag(file.getAbsolutePath());
            radio_button.setTextSize(getResources().getDimension(R.dimen.radio_button_font_size));
            file_list.addView(radio_button);
        }

        if(files.size() > 0) {
            ((RadioButton)file_list.getChildAt(0)).setChecked(true);
        }
        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_popup_confirm:
                RxEventBus.getInstance().sendMessage(new EMessage(EMessage.GEO_JSON_FILE_PATH, file_path));
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
            case R.id.btn_popup_cancel:
                RxEventBus.getInstance().sendViewWrapper(new ViewWrapper(null));
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        file_path = group.findViewById(checkedId).getTag().toString();
    }
}
