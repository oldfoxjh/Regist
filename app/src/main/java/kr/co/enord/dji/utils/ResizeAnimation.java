package kr.co.enord.dji.utils;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import androidx.constraintlayout.widget.ConstraintLayout;

public class ResizeAnimation extends Animation {

    private View view;
    private int to_height;
    private int from_height;

    private int to_width;
    private int from_width;
    private int margin;

    public ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int _margin) {
        to_height = toHeight;
        to_width = toWidth;
        from_height = fromHeight;
        from_width = fromWidth;
        view = v;
        margin = _margin;
        setDuration(300);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float height = (to_height - from_height) * interpolatedTime + from_height;
        float width = (to_width - from_width) * interpolatedTime + from_width;
        ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        p.height = (int) height;
        p.width = (int) width;
        p.leftMargin = margin;
        p.bottomMargin = margin;
        view.requestLayout();
    }
}
