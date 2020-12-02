package kr.co.enord.dji.model;

public class DroneStatus {
    public boolean is_flying = false;       /** 드론 비행상태 */
    public boolean is_going_home = false;   /** 자동복귀 여부 */
    public double drone_latitude;           /** 드론 위도 */
    public double drone_longitude;          /** 드론 경도 */
    public float drone_altitude;            /** 드론 고도 */

    public float velocity_x;                /** 드론 x축 속도 */
    public float velocity_y;                /** 드론 y축 속도 */
    public float velocity_z;                /** 드론 z축 속도 */

    public double home_latitude;            /** 자동복귀지점 위도 */
    public double home_longitude;           /** 자동복귀지점 경도 */
    public boolean home_set;                /** 자동복귀지점 설정여부 */
    public float heading;                   /** 진행방향 */
}
