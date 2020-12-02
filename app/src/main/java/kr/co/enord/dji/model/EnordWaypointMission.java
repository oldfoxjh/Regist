package kr.co.enord.dji.model;

import android.util.Log;

import org.osmdroid.util.GeoPoint;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import kr.co.enord.dji.DroneApplication;
import kr.co.enord.dji.utils.Geo;

public class EnordWaypointMission {

    private WaypointMission m_waypoint_missions;

    public WaypointMission getWaypointMission() {
        return m_waypoint_missions;
    }

    /**
     * 특정 지점 촬영 임무
     */
    public void createWaypointMission(List<GeoPoint> points, GeoPoint base_point, float flight_speed, float altitude){
        WaypointMission.Builder builder = getWaypointBuilder(flight_speed);

        int size = points.size();
        // Takeoff 후 이륙지점 높이를 목표고도로 설정
        Waypoint takeoff = new Waypoint(base_point.getLatitude(), base_point.getLongitude(), altitude);
        builder.addWaypoint(takeoff);

        for(int i = 0; i < size; i++){
            GeoPoint point = points.get(i);
            float calculated_altitude = (float)(point.getAltitude() - base_point.getAltitude()) + altitude;

            // 중간 지점일 경우 20M 추가
            //if(DroneApplication.getInterdPoint().contains(i)) calculated_altitude += 20;

            Waypoint waypoint = new Waypoint(point.getLatitude(), point.getLongitude(), calculated_altitude);
            if(!DroneApplication.getInterdPoint().contains(i)) waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
            builder.addWaypoint(waypoint);
        }

        // 복귀 지점
        Waypoint base_waypoint = new Waypoint(base_point.getLatitude(), base_point.getLongitude(), altitude);
        builder.addWaypoint(base_waypoint);

        m_waypoint_missions = builder.build();
    }

    /**
     * 특정 영역 촬영 임무
     */
    public void createAreaMission(List<GeoPoint> points, GeoPoint base_point, float flight_speed){
        WaypointMission.Builder builder = getWaypointBuilder(flight_speed);

        for(int i = 0; i < points.size() ; i++){
            GeoPoint point = points.get(i);
            Waypoint waypoint = new Waypoint(point.getLatitude(), point.getLongitude(), (float)point.getAltitude());
            builder.addWaypoint(waypoint);
        }

        builder.waypointCount(points.size()+1);

        m_waypoint_missions = builder.build();
    }

    private WaypointMission.Builder getWaypointBuilder(float flight_speed)
    {
        WaypointMission.Builder builder = new dji.common.mission.waypoint.WaypointMission.Builder();
        builder.autoFlightSpeed(flight_speed);
        builder.maxFlightSpeed(Math.max(flight_speed, 14));
        builder.setExitMissionOnRCSignalLostEnabled(false);                         // 신호가 끊겨도 임무 계속 진행하도록 설정
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);      // 첫번째 웨이포인트까지 고도 상승후 이동
        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);              // 임무 완료후 자동 복귀
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);               // 코너 이동시 멈충 후 이동
        builder.headingMode(WaypointMissionHeadingMode.AUTO);                       // Heading 방향은 비행방향
        builder.repeatTimes(1);

        return builder;
    }

    private static final double ONE_METER_OFFSET = 0.00000899322;
}
