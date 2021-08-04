package kr.co.enord.dji;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import io.reactivex.observers.DefaultObserver;
import kr.co.enord.dji.model.EnordLocationManager;
import kr.co.enord.dji.model.RxEventBus;
import kr.co.enord.dji.model.ViewWrapper;
import kr.co.enord.dji.utils.ToastUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] REQUIRED_PERMISSION_LIST = new String[] {
        Manifest.permission.VIBRATE, // Gimbal rotation
        Manifest.permission.INTERNET, // API requests
        Manifest.permission.ACCESS_WIFI_STATE, // WIFI connected products
        Manifest.permission.ACCESS_COARSE_LOCATION, // Maps
        Manifest.permission.ACCESS_NETWORK_STATE, // WIFI connected products
        Manifest.permission.ACCESS_FINE_LOCATION, // Maps
        Manifest.permission.CHANGE_WIFI_STATE, // Changing between WIFI and USB connection
        Manifest.permission.WRITE_EXTERNAL_STORAGE, // Log files
        Manifest.permission.BLUETOOTH, // Bluetooth connected products
        Manifest.permission.BLUETOOTH_ADMIN, // Bluetooth connected products
        Manifest.permission.READ_EXTERNAL_STORAGE, // Log files
        Manifest.permission.READ_PHONE_STATE, // Device UUID accessed upon registration
        Manifest.permission.RECORD_AUDIO // Speaker accessory
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private int lastProcess = -1;

    private final int period = 500;         // 드론정보 수집 주기

    private Stack<ViewWrapper> m_stack;
    private FrameLayout m_content_frameLayout;

    //region Life-cycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 위치서비스 등록
        setLocationManager();

        // 초기화
        initParams();

        // 이벤트 관리
        setEventManager();
    }


    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            Toast.makeText(getApplicationContext(), "Missing permissions!!!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
    }

    @Override
    public void onBackPressed() {
//        if (m_stack.size() > 1) {
//            popView();
//        } else {
//            super.onBackPressed();
//        }
    }

    //endregion


    //region Registration n' Permissions Helpers

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.setResultToToast("DJI SDK 등록중..");
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                ToastUtils.setResultToToast("DJI SDK 등록 성공.");
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                ToastUtils.setResultToToast("DJI SDK 등록 실패 : "+ djiError.getDescription());
                            }
                        }
                        @Override
                        public void onProductDisconnect() {
                            RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_DISCONNECT);
                            DroneApplication.setDroneConnectionDate(null);
                        }
                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            RxEventBus.getInstance().sendDroneStatus(RxEventBus.DRONE_STATUS_CONNECT);

                            DroneApplication.setDroneConnectionDate(new Date());
                            // 드론 데이터 리스터 등록
                            DroneApplication.getDroneInstance().setDroneDataListener();

                            // 촬영사진 전송 모듈 설정
                            DroneApplication.setMediaDownloader();

                            // 카메라 모드를 촬영모드로 강제로 설정
                            DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);

                            // 드론 비행고도를 최고치로 설정
                            DroneApplication.getDroneInstance().setMaxFlightHeight(500);

                            // 광학줌 지원이 되면 최대 초점거리 설정 - 2020.12.09
                            DroneApplication.getDroneInstance().isOpticalZoomSupported();
                        }

                        @Override
                        public void onProductChanged(BaseProduct baseProduct) {

                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey,
                                                      BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long current, long total) {
                            int process = (int) (100 * current / total);
                            if (process == lastProcess) {
                                return;
                            }
                            lastProcess = process;
                            if (process % 25 == 0){
                                ToastUtils.setResultToToast("DB load process : " + process);
                            }else if (process == 0){
                                ToastUtils.setResultToToast("DB load begin");
                            }
                        }
                    });
                }
            });
        }
    }
    //endregion

    private void initParams() {
        m_content_frameLayout = findViewById(R.id.framelayout_content);

        m_stack = new Stack<ViewWrapper>();
        m_stack.push(new ViewWrapper(m_content_frameLayout.getChildAt(0)));
    }

    //region 위치 관리
    private  void setLocationManager() {
        LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<String> providers = manager.getAllProviders();
        for(int i = 0; i < providers.size(); i++)
        {
            manager.requestLocationUpdates(providers.get(i), 500, 0.0f, EnordLocationManager.getInstance());
        }
    }
    //endregion

    // region 이벤트 관리
    private void setEventManager()
    {
        RxEventBus.getInstance().getViewWrapperObserver().subscribe(
                new DefaultObserver<ViewWrapper>() {
                    @Override
                    public void onNext(ViewWrapper wrapper) {
                        runOnUiThread(() -> {
                            if(wrapper.getView() != null) pushView(wrapper);
                            else popView();
                        });
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                }
        );
    }

    /**
     * 뷰 추가
     * @param wrapper 추가할 뷰
     */
    private void pushView(ViewWrapper wrapper) {
        View show_view = wrapper.getView();
        m_stack.push(wrapper);

        if (show_view.getParent() != null) {
            ((ViewGroup) show_view.getParent()).removeView(show_view);
        }

        m_content_frameLayout.addView(show_view);
    }

    /**
     * 최상위 뷰 삭제
     */
    private void popView()
    {
        if (m_stack.size() <= 1) {
            finish();
            return;
        }

        ViewWrapper removeWrapper = m_stack.pop();

        m_content_frameLayout.removeView(removeWrapper.getView());
    }
    // endregion


}