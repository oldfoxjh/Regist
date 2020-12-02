package kr.co.enord.dji.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Date;

import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.R;

public class CustomPreFlightStatusWidget extends dji.ux.widget.PreFlightStatusWidget {

    //region Properties
    private Paint status_paint;
    private static final int STROKE_WIDTH = 5;
    private int width;
    private int height;
    private String drone_status = "기기연결끊김";
    private boolean is_good = false;
    //endregion

    //region Constructors
    public CustomPreFlightStatusWidget(Context context) {
        this(context, null, 0);
    }

    public CustomPreFlightStatusWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomPreFlightStatusWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region Override methods
    /** Inflate custom layout for this widget */
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {

        status_paint = new Paint();
        status_paint.setStyle(Paint.Style.FILL);
        status_paint.setColor(Color.WHITE);
        status_paint.setAlpha(255);
        status_paint.setAntiAlias(true);
        int textSize = getResources().getDimensionPixelSize(R.dimen.custom_preflight_info_text_size);
        status_paint.setTextSize(textSize);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawText(drone_status,
                context.getResources().getDimensionPixelSize(R.dimen.custom_preflight_info_text_x),
                context.getResources().getDimensionPixelSize(R.dimen.custom_preflight_info_text_y),
                status_paint);
    }

    /** Called when connection status changes */
    @Override
    public void onStatusChange(String status, StatusType type, boolean blink) {
        if (type == StatusType.OFFLINE)
        {
            drone_status = "기기 연결 끊김";
            setBackgroundResource(R.mipmap.top_bg_gray);
        }
        else if (type == StatusType.GOOD)
        {
            if(status.contains("In-Flight")) {
                drone_status = status.replace("In-Flight", "비행중");
            }
            else if(status.contains("Ready to Go")) {
                drone_status = status.replace("Ready to Go", "비행 준비 완료");
            }
            else if(status.contains("Ready to GO")) {
                drone_status = status.replace("Ready to GO", "비행 준비 완료");
            }
            else if(status.contains("Returning Home")) {
                drone_status = status.replace("Returning Home", "자동 복귀중");
            }else drone_status = status;

            setBackgroundResource(R.mipmap.top_bg_green);
        }
        else if (type == StatusType.WARNING)
        {
            if(status.equals("Image Transmission Signal Weak")) drone_status = "영상전송 신호 약함";
            else if(status.equals("Gimbal Motor Overloaded")) drone_status = "짐벌 모터 과부하";
            else if(status.equals("Strong Signal Interference")) drone_status = "강한 외부신호 간섭";
            else drone_status = status;

            setBackgroundResource(R.mipmap.top_bg_orange);
        }
        else if (type == StatusType.ERROR)
        {
            if(status.equals("Aircraft Disconnected")) {
                if(DroneApplication.getDrone() != null) {
                    drone_status = "비행 준비 완료";
                    setBackgroundResource(R.mipmap.top_bg_green);
                    return;
                }
                else {
                    drone_status = "기체 연결 끊김";
                }
            }
            else if(status.equals("Remote Controller Signal Weak")) drone_status = "조종기 신호 약함";
            else if(status.equals("Cannot take off")) drone_status = "이륙준비 오류";
            else if(status.equals("IMU Error. Calibrate IMU")) {
                drone_status = "IMU 오류";
            }
            else if(status.equals("Compass Error. Exit GPS Mode") || status.toLowerCase().contains("magnetic")) {
                drone_status = "나침반 오류";
            }
            else if(status.equals("Downward Vision Sensor Calibration Error")) {
                drone_status = "비전센서 오류";
            }
            else if(status.equals("Low Battery")) drone_status = "배터리 전압 낮음";
            else drone_status = status;

            setBackgroundResource(R.mipmap.top_bg_red);
        }

        invalidate();
    }
}