package com.test.kraftu.mapview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.test.kraftu.mapview.core.imp.BaseTileManager;

/**
 * Created by kraftu on 23.02.17.
 */

public class MapView extends View implements BaseTileManager.TileManagerListener{

    public static final String TAG = "MapView";
    public static final boolean DEBUG = true;

    RectF sourceRect = null;
    RectF frameRect = null;
    RectF drawTile = null;
    Paint paint;
    BaseTileManager tileManager;

    GestureDetector gestureDetector;

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
        log(String.format("vrect:%s srect%s", frameRect, sourceRect));
    }

    public void init(){


        tileManager = new BaseTileManager(getContext());
        tileManager.setTileManagerListener(this);

        drawTile = new RectF();
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        paint.setTextSize(20);
        paint.setStyle(Paint.Style.STROKE);
        sourceRect = new RectF(0,0,
                tileManager.getWidthTile() * tileManager.getCountTileX(),
                tileManager.getHeightTile() * tileManager.getCountTileY());


        gestureDetector = new GestureDetector(getContext(),new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                setTranslate(-distanceX,-distanceY);
                return true;
            }
        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    public void setTranslate(float dx,float dy){
        //log(String.format("----"));
        //log(String.format("dx:%f dy:%f",dx,dy));
        if(!frameRect.contains(sourceRect)){
            sourceRect.left = sourceRect.left + dx;
            sourceRect.right = sourceRect.right + dx;
            sourceRect.top = sourceRect.top + dy;
            sourceRect.bottom = sourceRect.bottom + dy;

            checkMoveBounds();
        }
        invalidate();
        //log(String.format("vrect:%s srect%s", frameRect, sourceRect));
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

    public int getTileRawX(float screenX){
        return (int) (screenX - sourceRect.left) / tileManager.getWidthTile();
    }

    public int getTileRawY(float screenY){
        return (int)(screenY - sourceRect.top) / tileManager.getHeightTile();
    }

    public float getLocationTileX(int tileX){
        return tileX * tileManager.getWidthTile() + sourceRect.left;
    }

    public float getLocationTileY(int tileY){
        return tileY * tileManager.getHeightTile() + sourceRect.top;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(sourceRect.centerX(),sourceRect.centerY(),100f,paint);

        int startTileX = getTileRawX(0);
        int startTileY = getTileRawY(0);
        int currentTileX;
        int currentTileY;

        float drawTileX = getLocationTileX(startTileX);
        float drawTileY = getLocationTileY(startTileY);

        float endTileX = Math.min(frameRect.right, sourceRect.right);
        float endTileY = Math.min(frameRect.bottom, sourceRect.bottom);

        Bitmap tileBitmap;
        while(drawTileY < endTileY){
            while(drawTileX < endTileX){
                drawTile.set(drawTileX,drawTileY,drawTileX + tileManager.getWidthTile(),drawTileY + tileManager.getHeightTile());

                currentTileX = getTileRawX(drawTile.centerX());
                currentTileY = getTileRawY(drawTile.centerY());

                tileBitmap = tileManager.getBitmapTile(currentTileX,currentTileY);
                if(tileBitmap!=null){
                    canvas.drawBitmap(tileBitmap,drawTileX,drawTileY,paint);
                }
                if(DEBUG) {
                    canvas.drawRect(drawTile, paint);
                    canvas.drawText(String.format("%d", tileManager.getTileId(currentTileX, currentTileY))
                                , drawTile.centerX(), drawTile.centerY(), paint);
                }
                drawTileX = drawTileX + tileManager.getWidthTile();
            }
            drawTileX = getLocationTileX(startTileX);
            drawTileY = drawTileY + tileManager.getHeightTile();
        }

    }
    public void log(String s){
        if(DEBUG)Log.d(TAG,s);
    }

    @Override
    public void loadedNewTile(int tileId) {
        postInvalidate();
        log("loadedNewTile:" + tileId);
    }
}
