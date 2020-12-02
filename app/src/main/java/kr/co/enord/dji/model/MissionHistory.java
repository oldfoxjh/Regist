package kr.co.enord.dji.model;

import android.content.Context;
import android.provider.BaseColumns;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class MissionHistory {

    public String m_filepath = null;
    public long m_id = 0;
    public int past_days = -1;

    public MissionHistory(String filepath, String return_time) {
        m_filepath = filepath;
        past_days = calculatePastDays(return_time);
    }

    public MissionHistory(long id, String filepath){
        m_id = id;
        m_filepath = filepath;
    }

    public void setMissinoId(Context context){
        FlightInfoDBHelper sqlite = new FlightInfoDBHelper(context);
        m_id = sqlite.check_mission_flight(m_filepath);
        Log.e("Mission", "check db ID : " + m_id);
        if(m_id < 1){
            m_id = sqlite.insert_mission(m_filepath);
            Log.e("Mission", "insert db ID : " + m_id);
        }
    }

    public void updateMission(Context context){
        FlightInfoDBHelper sqlite = new FlightInfoDBHelper(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        sqlite.update_mission(m_id, format.format(new Date()));
    }

    /**
     * // 임무 완료 날짜를 현재 날짜와 비교해서 일 기준으로 반환
     * @param return_time
     * @return
     */
    private int calculatePastDays(String return_time){

        int diff = -1;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date today = format.parse(format.format(new Date()));
            Date flight_date = format.parse(return_time);

            Log.e("Mission", format.format(new Date()));
            Log.e("Mission", return_time);
            diff = (int)Math.abs((today.getTime() - flight_date.getTime())/(24*3600000));
            Log.e("Mission", "diff : " + diff);
        }catch (ParseException e){

        }

        return diff;
    }

    public static class MissionHistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "mission_history";
        public static final String COLUMN_NAME_FILE_PATH = "filepath";
        public static final String COLUMN_NAME_COMPLETE = "complete";
        public static final String COLUMN_NAME_START_TIME = "start_time";
        public static final String COLUMN_NAME_RETURN_TIME = "return_time";

        public static final String CREATE_QUERY = "CREATE TABLE " + TABLE_NAME + " (" +
                " id INTEGER PRIMARY KEY," +
                COLUMN_NAME_FILE_PATH + " TEXT," +
                COLUMN_NAME_COMPLETE + " INTEGER DEFAULT 0," +
                COLUMN_NAME_START_TIME + " TEXT," +
                COLUMN_NAME_RETURN_TIME + " TEXT)";
    }
}
