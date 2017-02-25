package com.test.kraftu.mapview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.test.kraftu.mapview.core.TileManager;
import com.test.kraftu.mapview.core.TileManagerListener;
import com.test.kraftu.mapview.core.imp.BaseTileManager;
import com.test.kraftu.mapview.network.TileResource;

public class MapView extends View implements TileManagerListener {
    public static final String TAG = "MapView";
    public static final boolean DEBUG = false;

    private RectF sourceRect = null;
    private RectF frameRect = null;

    private Paint paint;
    private TileManager tileManager;
    private GestureDetector gestureDetector;

    private int tileSizeX;
    private int tileSizeY;
    private int tileCountX;
    private int tileCountY;

    private int firstTileX;
    private int firstTileY;
    private float firstEdgeTileX;
    private float firstEdgeTileY;

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
        frameRect = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
        log(String.format("vrect:%s srect%s", frameRect, sourceRect));
        if(tileManager!=null) preDrawLocation();
    }

    public void init() {

        tileManager = new BaseTileManager(getContext());
        tileManager.setTileManagerListener(this);
        TileResource tileResource = tileManager.getTileDownloader();

        tileSizeX = tileResource.getWidthTile();
        tileSizeY = tileResource.getHeightTile();
        tileCountX = tileResource.getCountTileX();
        tileCountY = tileResource.getCountTileY();

        sourceRect = new RectF(0, 0, tileSizeX * tileCountX, tileSizeY * tileCountY);

        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        paint.setTextSize(20);
        paint.setStyle(Paint.Style.STROKE);

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                setTranslate(-distanceX, -distanceY);
                return true;
            }
        });
        /*postDelayed(new Runnable() {
            @Override
            public void run() {
                setTranslate(-5,-5);
                postDelayed(this,20);
            }
        },1000);*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            tileManager.cancelLoad();
        }
        gestureDetector.onTouchEvent(event);
        return true;
    }

    public void setTranslate(float dx, float dy) { /*log(String.format("----")); log(String.format("dx:%f dy:%f",dx,dy));*/
        if (!frameRect.contains(sourceRect)) {
            sourceRect.left = sourceRect.left + dx;
            sourceRect.right = sourceRect.right + dx;
            sourceRect.top = sourceRect.top + dy;
            sourceRect.bottom = sourceRect.bottom + dy;
            checkMoveBounds();
        }
        preDrawLocation();
        invalidate();
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
    }

    public int getTileRawX(float screenX) {
        return (int) (screenX - sourceRect.left) / tileSizeX;
    }

    public int getTileRawY(float screenY) {
        return (int) (screenY - sourceRect.top) / tileSizeY;
    }

    public float getLocationTileX(int tileX) {
        return tileX * tileSizeX + sourceRect.left;
    }

    public float getLocationTileY(int tileY) {
        return tileY * tileSizeY + sourceRect.top;
    }

    private void preDrawLocation(){
        firstTileX = getTileRawX(0);
        firstTileY = getTileRawY(0);
        firstEdgeTileX = getLocationTileX(firstTileX);
        firstEdgeTileY =  getLocationTileY(firstTileY);

        tileManager.updateVisibleTile(firstTileX - 1, getTileRawX(frameRect.right) + 1, firstTileY - 1,getTileRawY(frameRect.bottom) + 1);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(tileManager == null || sourceRect == null || frameRect == null) return;

        Bitmap tileBitmap = null;

        int currentTileX = firstTileX;
        int currentTileY = firstTileY;

        float startTileX = firstEdgeTileX;
        float startTileY = firstEdgeTileY;
        float endTileX = Math.min(frameRect.right, sourceRect.right);
        float endTileY = Math.min(frameRect.bottom, sourceRect.bottom);


        while (startTileY < endTileY) {
            while (startTileX < endTileX) {

                tileBitmap = tileManager.getBitmapTile(currentTileX, currentTileY);
                if (tileBitmap != null && !tileBitmap.isRecycled())
                    canvas.drawBitmap(tileBitmap, startTileX, startTileY, paint);

                if (DEBUG) {
                    canvas.drawRect(startTileX, startTileY, startTileX + tileSizeX, startTileY + tileSizeY, paint);
                    canvas.drawText(String.format("%d", tileManager.getTileId(currentTileX, currentTileY))
                            ,startTileX, startTileY+20, paint);
                }

                currentTileX = currentTileX + 1;
                startTileX = startTileX + tileSizeX;
            }

            currentTileX = firstTileX;
            currentTileY = currentTileY + 1;

            startTileX = firstEdgeTileX;
            startTileY = startTileY + tileSizeY;
        }

    }

    public void log(String s) {
        if (DEBUG) Log.d(TAG, s);
    }

    @Override
    public void loadedTile(int tileId) {
        postInvalidate();
    }
}
