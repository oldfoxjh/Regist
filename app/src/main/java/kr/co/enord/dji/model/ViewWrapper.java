package kr.co.enord.dji.model;

import android.view.View;

public class ViewWrapper {

    public static String MissonStartRemove = "mission start force remove";

    private View view;
    private String tag = null;

    public ViewWrapper(View layoutView) {
        view = layoutView;
    }

    public View getView() {
        return view;
    }

    public String viewInfo(){ return view.toString(); }
    public void setTag(String str){ tag = str; }
    public String getTag(){ return  tag; }
}
