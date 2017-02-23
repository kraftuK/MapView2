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
    private static int COUNT_TILE_X = 50;
    private static int COUNT_TILE_Y = 50;

    float xTranslate = 0;
    float yTranslate = 0;

    float xLastTouch;
    float yLastTouch;

    RectF sourceRect = null;
    RectF frameRect = null;
    RectF drawTile = new RectF();

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
        paint.setStrokeWidth(2);
        paint.setTextSize(20);
        paint.setStyle(Paint.Style.STROKE);
        sourceRect = new RectF(0,0,TILE_SIZE_X * COUNT_TILE_X,TILE_SIZE_Y * COUNT_TILE_Y);

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
        sourceRect.left = sourceRect.left + dx;
        sourceRect.right = sourceRect.right + dx;
        sourceRect.top = sourceRect.top + dy;
        sourceRect.bottom = sourceRect.bottom + dy;

        //Log.d("translate", String.format("new vrect:%s", frameRect));
        checkMoveBounds();
        //xTranslate = sourceRect.left;
        //yTranslate = sourceRect.top;
        invalidate();
        Log.d("translate", String.format("vrect:%s srect%s", frameRect, sourceRect));
    }

    private void checkMoveBounds() {
        float diff = sourceRect.left - frameRect.left;
        if (diff > 0) {
            sourceRect.left = sourceRect.left - diff;
            sourceRect.right = sourceRect.right - diff;

        }
        diff = sourceRect.right - frameRect.right;
        if (diff < 0) {
            sourceRect.left = sourceRect.left - diff;
            sourceRect.right = sourceRect.right - diff;
        }
        diff = sourceRect.top - frameRect.top;
        if (diff > 0) {
            sourceRect.top = sourceRect.top - diff;
            sourceRect.bottom = sourceRect.bottom - diff;
        }
        diff = sourceRect.bottom - frameRect.bottom;
        if (diff < 0) {
            sourceRect.top = sourceRect.top - diff;
            sourceRect.bottom = sourceRect.bottom - diff;
        }

        //Log.d("translate", String.format("vrect:%s srect%s", frameRect, sourceRect));
    }

    public int getRawX(float x){

        return (int) x / TILE_SIZE_X;
    }

    public int getRawY(float y){
        return (int) y / TILE_SIZE_Y;
    }

    public float getLocationTileX(int tileX){
        return tileX * TILE_SIZE_X;
    }

    public float getLocationTileY(int tileY){
        return tileY * TILE_SIZE_Y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(sourceRect.centerX(),sourceRect.centerY(),100f,paint);


        float drawTileX = sourceRect.left;
        float drawTileY = sourceRect.top;

        while(drawTileX < sourceRect.right){

            while(drawTileY < sourceRect.bottom){
                drawTile.set(drawTileX,drawTileY,drawTileX + TILE_SIZE_X,drawTileY + TILE_SIZE_Y);
                canvas.drawRect(drawTile,paint);
                canvas.drawText(String.format("%d %d",(int)drawTileX,(int)drawTileY),drawTile.centerX(),drawTile.centerY(),paint);
                drawTileY = drawTileY + TILE_SIZE_Y;
            }
            drawTileX = drawTileX + TILE_SIZE_X;
            drawTileY = sourceRect.top;
        }

    }
}
