package kr.co.enord.dji.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

import kr.co.enord.dji.R;

public class MapLayer {

    private static final MapLayer ourInstance = new MapLayer();
    public static MapLayer getInstance() {
        return ourInstance;
    }

    /**
     * 마커 아이콘 회전
     * @param context       application context
     * @param drawableId    drawable Image Id
     * @param degree        rotate by degree
     * @return              drawable bitmap
     */
    public BitmapDrawable getRotateDrawable(Context context, int drawableId, final float degree) {

        final Bitmap bm = BitmapFactory.decodeResource(context.getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Matrix m = new Matrix();
        m.setRotate(degree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        return new BitmapDrawable(context.getResources(), Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true));
    }

    /**
     * 마커에 숫자 적용
     */
    public BitmapDrawable writeOnDrawable(Context context, String text, int drawableId) {
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Bitmap background = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        int textSize;
        if(drawableId == R.mipmap.waypoint_s) {
            paint.setColor(Color.WHITE);
            textSize = context.getResources().getDimensionPixelSize(R.dimen.entry_font);
        }else{
            textSize = context.getResources().getDimensionPixelSize(R.dimen.mission_font);
            paint.setColor(Color.BLACK);
        }


        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, bm.getWidth()/2- bounds.right/2 - (text.equals("1") ? 2 : 0), bm.getHeight()/2-bounds.top/2, paint);

        return new BitmapDrawable(context.getResources(), bm);
    }

    public BitmapDrawable writeOnDrawable(Context context, String text, int width, int height, int text_color, int text_size) {
        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(text_color);
        paint.setTextSize(text_size);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, bm.getWidth()/2- bounds.right/2 - (text.equals("1") ? 2 : 0), bm.getHeight()/2-bounds.top/2, paint);

        return new BitmapDrawable(context.getResources(), bm);
    }
}
