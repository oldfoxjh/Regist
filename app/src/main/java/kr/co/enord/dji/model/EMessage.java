package kr.co.enord.dji.model;

public class EMessage {

    public static final int GEO_JSON_FILE_PATH = 0x00;

    public static final int GEO_JSON_DOWNLOAD_TARGET = 0xFF01;

    int m_type;
    String m_msg;

    public EMessage(int type, String msg){
        m_type = type;
        m_msg = msg;
    }

    public int getType(){ return m_type; }
    public String getMessage(){ return m_msg; }
}
