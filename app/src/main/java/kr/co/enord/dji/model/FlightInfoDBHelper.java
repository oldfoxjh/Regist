package kr.co.enord.dji.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FlightInfoDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "FlightInfo.db";

    public FlightInfoDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 임무 비행 정보 테이블 생성
        db.execSQL(MissionHistory.MissionHistoryEntry.CREATE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String mission_history_delete = "DROP TABLE IF EXISTS mission_history";

        db.execSQL(mission_history_delete);
        onCreate(db);
    }

    public long insert_mission(String path){
        ContentValues values = new ContentValues();
        values.put(MissionHistory.MissionHistoryEntry.COLUMN_NAME_FILE_PATH, path);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        values.put(MissionHistory.MissionHistoryEntry.COLUMN_NAME_START_TIME, format.format(new Date()));

        // Insert the new row, returning the primary key value of the new row
        SQLiteDatabase db = getWritableDatabase();
        long result = db.insert(MissionHistory.MissionHistoryEntry.TABLE_NAME, null, values);

        db.close();
        return result;
    }

    /**
     * 임무 완료 정보 업데이트
     * @param id
     * @param return_time
     * @return
     */
    public int update_mission(long id, String return_time){
        ContentValues values = new ContentValues();
        values.put(MissionHistory.MissionHistoryEntry.COLUMN_NAME_RETURN_TIME, return_time);
        values.put(MissionHistory.MissionHistoryEntry.COLUMN_NAME_COMPLETE, 1);

        SQLiteDatabase db = getWritableDatabase();
        String selection = " id = " + id;

        int result = db.update(
                        MissionHistory.MissionHistoryEntry.TABLE_NAME,
                        values,
                        selection,
                null);

        db.close();
        return result;
    }

    /**
     * 완료된 임무 목록 반환
     * @return
     */
    public List<MissionHistory> select_mission(){
        List<MissionHistory> result = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + MissionHistory.MissionHistoryEntry.TABLE_NAME
                + " WHERE " + MissionHistory.MissionHistoryEntry.COLUMN_NAME_COMPLETE + " = 1";
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()){
            String file_path = cursor.getString(cursor.getColumnIndexOrThrow(MissionHistory.MissionHistoryEntry.COLUMN_NAME_FILE_PATH));
            String return_time = cursor.getString(cursor.getColumnIndexOrThrow(MissionHistory.MissionHistoryEntry.COLUMN_NAME_RETURN_TIME));
            result.add(new MissionHistory(file_path, return_time));
        }

        cursor.close();
        db.close();

        return result;
    }

    /**
     *
     * @param path
     * @return
     */
    public long check_mission_flight(String path) {
        long id = 0;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + MissionHistory.MissionHistoryEntry.TABLE_NAME
                       + " WHERE " + MissionHistory.MissionHistoryEntry.COLUMN_NAME_FILE_PATH + " = '" + path + "'";
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()){
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        }

        cursor.close();
        db.close();

        return id;
    }
}
