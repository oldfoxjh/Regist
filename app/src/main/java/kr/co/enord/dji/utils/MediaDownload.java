package kr.co.enord.dji.utils;

import android.os.Environment;
import android.util.Log;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;
import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.model.ServerResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MediaDownload {
    private static final String TAG = "MediaDownload";
    private static final String path = Environment.getExternalStorageDirectory().getPath() + "/Enord/Pictures/";
    private File save_path;
    private MediaManager media_manager = null;
    private FetchMediaTaskScheduler taskScheduler = null;
    private List<MediaFile> medias = new ArrayList<>();
    private List<String> upload_files = null;
    private int total_download_size = 0;
    private boolean m_download_complete = true;



    public MediaDownload(){
        // 오늘 날짜 폴더 생성
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        save_path = new File(path + formatter.format(new Date()));
        if(!save_path.exists()) {
            save_path.mkdirs();
        }

        if(media_manager == null && DroneApplication.getDrone() != null){
            media_manager = DroneApplication.getDrone().getCamera().getMediaManager();
        }

        if(media_manager == null) return;

        if (taskScheduler == null) {
            taskScheduler = media_manager.getScheduler();
            if (taskScheduler != null && taskScheduler.getState() == FetchMediaTaskScheduler.FetchMediaTaskSchedulerState.SUSPENDED) {
                taskScheduler.resume(djiError -> {
                    if (djiError != null) {
                        Log.e("Tag", "taskScheduler resume failed: " + djiError.getDescription());
                        // event bus
                    }
                });
            }
        }
    }

    /**
     * 현재 시간으로 부터 기준시간 이내의 사진을 다운로드 한다.
     */
    public void startDownload(){
        if(m_download_complete == false || DroneApplication.getDrone().getCamera() == null) return;

        DroneApplication.getDrone().getCamera().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, djiError -> {
            if (null == djiError) {
                // 파일 목록 가져오기
                if(DroneApplication.getDroneInstance().isStorageSDCard()){
                    downloadSDCardMedia();
                }
                else {
                    downloadInternalMediaList();
                }
            }else{
                // 모드 변경 오류 처리
            }
        });
    }

    /**
     * 저장장치에 있는 미디어 파일 목록을 가져온다.
     */
    private void downloadSDCardMedia(){
        if (media_manager != null) {
            media_manager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, djiError -> {
                if (null == djiError) {
                    medias.addAll(media_manager.getSDCardFileListSnapshot());
                    if (!medias.isEmpty()) {
                        checkMediaList();
                    }
                } else {
                }
            });
        }
    }

    /**
     * 내부저장장치에 있는 미디어 파일 목록을 가져온다.
     */
    private void downloadInternalMediaList() {
        if (media_manager != null) {
            media_manager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, djiError -> {
                if (null == djiError) {
                    medias.addAll(media_manager.getSDCardFileListSnapshot());
                    if (!medias.isEmpty()) {
                        checkMediaList();
                    }
                } else {

                }
            });
        }
    }

    /**
     * 드론 이륙후 찍은 사진만 필터링해서 Download
     */
    private void checkMediaList() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        MediaFile mf = null;
        if(medias == null || medias.size() == 0) return ;

        for (Iterator<MediaFile> it = medias.iterator(); it.hasNext(); ) {
            MediaFile value = it.next();

            try{
                // 드론의 현재 비행 이륙시간 이후의 파일인지 확인 후 목록에서 제거
                Calendar cal = Calendar.getInstance();
                cal.setTime(DroneApplication.getDroneConnectionDate());

                Date c = cal.getTime();
                Date d = sdf.parse(value.getDateCreated());

                if(c.compareTo(d) > 0) it.remove();
                if(isExist(value.getFileName())) it.remove();
                else{
                    MediaFile.MediaType mt = value.getMediaType();
                    // 동영상 촬영 파일은 목록에서 제거
                    if(mt == MediaFile.MediaType.MOV || mt == MediaFile.MediaType.MP4) it.remove();
                }
            }catch (Exception ex){

            }
        }

        // 전체 다운로드 크기
        total_download_size = medias.size();
        // 다운로드 시작
        if(medias.size() > 0) downloadFile(medias.get(0));
    }

    /**
     * 미디어 파일 다운로드
     * @param media
     */
    private void downloadFile(MediaFile media) {
        if(media == null) return;

        // 파일이 존재하면 다음 다운로드
        File file = new File(save_path + "/" + media.getFileName());
        Log.e("Mission", "medias file name : " + media.getFileName());
        if(file.exists()) {
            file.delete();
        }

        media.fetchFileData(save_path, getBaseName(media.getFileName()), new DownloadListener<String>() {
            @Override
            public void onStart() {
            }

            @Override
            public void onRateUpdate(long l, long l1, long l2) {
            }

            @Override
            public void onRealtimeDataUpdate(byte[] bytes, long l, boolean b) {
                
            }

            @Override
            public void onProgress(long l, long l1) {

            }

            @Override
            public void onSuccess(String s) {
                if(upload_files == null) upload_files = new ArrayList<>();
                upload_files.add(save_path + "/" + medias.get(0).getFileName());
                medias.remove(0);

                if(medias.size() > 0) {
                    downloadFile(medias.get(0));
                }else{
                    m_download_complete = true;
                    uploadPicture();
                }
            }

            @Override
            public void onFailure(DJIError djiError) {
                m_download_complete = true;
            }
        });
    }

    /**
     * 서버에 파일 업로드
     */
    private void uploadPicture() {
        if(upload_files == null || upload_files.size() == 0) return;

        File file = new File(upload_files.get(0));
        RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("upload", file.getName(), requestBody);

        float[] position = Geo.getInstance().getPicturePosition(upload_files.get(0));
        RequestBody latitude = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(position[0]));
        RequestBody longitude = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(position[1]));

        HashMap<String, RequestBody> map = new HashMap<>();
        map.put("lat", latitude);
        map.put("lng", longitude);

        DroneApplication.getAPI().uploadFileWithParam(fileToUpload, map).enqueue(new Callback<ServerResponse>(){
            @Override
            public  void onResponse(Call<ServerResponse> call, Response<ServerResponse> response){
                if(response.code() == 200) {
                    if(upload_files.size() > 0) upload_files.remove(0);
                    uploadPicture();
                }
                Log.e(TAG, "onResponse : " + response.message());
            }

            @Override
            public  void onFailure(Call<ServerResponse> call, Throwable t){
                uploadPicture();
                Log.e(TAG, "onFailure : " + t.getMessage());
            }
        });
    }

    /**
     * 다운로드 폴더에 현재 파일이 있는 지 확인
     * @param filename
     * @return
     */
    private boolean isExist(String filename){

        File file = new File(save_path.getAbsolutePath() + "/" + filename);
        if(file.exists()) return true;

        return  false;
    }

    /**
     * 확장자를 제외한 파일 이름만 가져오기
     * @param fileName
     * @return
     */
    private static String getBaseName(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index == -1) {
            return fileName;
        } else {
            return fileName.substring(0, index);
        }
    }
}
