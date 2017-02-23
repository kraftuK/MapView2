package com.test.kraftu.mapview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by kraftu on 23.02.17.
 */

public class MapView extends View {

    private static int TILE_SIZE_X = 256;
    private static int TILE_SIZE_Y = 256;
    private static int COUNT_TILE_X = 10;
    private static int COUNT_TILE_Y = 10;

    float xTranslate = 0;
    float yTranslate = 0;

    float xLastTouch;
    float yLastTouch;

    RectF sourceRect = null;
    RectF frameRect = null;

    Paint paint;

    public MapView(Context context) {
        super(context);
        init();
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        frameRect = new RectF(0,0,getMeasuredWidth(),getMeasuredHeight());
        Log.d("translate", String.format("vrect:%s srect%s", frameRect, sourceRect));
    }

    public void init(){
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10);
        sourceRect = new RectF(0,0,TILE_SIZE_X * 10,TILE_SIZE_Y * 10);

    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){

            xLastTouch = event.getX();
            yLastTouch = event.getY();

            return true;
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){

            float dx = event.getX() - xLastTouch;
            float dy = event.getY() - yLastTouch;

            xLastTouch = event.getX();
            yLastTouch = event.getY();

            setTranslate(dx,dy);

            return true;
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            return false;
        }
        return false;
    }

    public void setTranslate(float dx,float dy){
        Log.d("translate", String.format("----"));
        Log.d("translate", String.format("dx:%f dy:%f",dx,dy));

        frameRect.left = frameRect.left + dx;
        frameRect.right = frameRect.right + dx;
        frameRect.top = frameRect.top + dy;
        frameRect.bottom = frameRect.bottom + dy;
        Log.d("translate", String.format("new vrect:%s", frameRect));
        checkMoveBounds();
        xTranslate = frameRect.left;
        yTranslate = frameRect.top;
        invalidate();
        Log.d("translate", String.format("vrect:%s srect%s", frameRect, sourceRect));
    }

    private void checkMoveBounds() {
        float diff = frameRect.left - sourceRect.left;
        if (diff < 0) {
            Log.d("translate", String.format("left:%f",diff));
            frameRect.left = frameRect.left - diff;
            frameRect.right = frameRect.right - diff;

        }
        diff = frameRect.right - sourceRect.right;
        if (diff > 0) {
            frameRect.left = frameRect.left - diff;
            frameRect.right = frameRect.right - diff;
        }
        diff = frameRect.top - sourceRect.top;
        if (diff < 0) {
            frameRect.top = frameRect.top - diff;
            frameRect.bottom = frameRect.bottom - diff;
        }
        diff = frameRect.bottom - sourceRect.bottom;
        if (diff > 0) {
            frameRect.top = frameRect.top - diff;
            frameRect.bottom = frameRect.bottom - diff;
        }

        Log.d("translate", String.format("vrect:%s srect%s", frameRect, sourceRect));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(xTranslate,yTranslate,30,paint);
    }
}
