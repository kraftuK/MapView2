package com.test.kraftu.mapview.view;

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

import com.test.kraftu.mapview.core.TileManager;
import com.test.kraftu.mapview.core.TileManagerListener;
import com.test.kraftu.mapview.core.TileResource;

public abstract class BaseMapView extends View implements TileManagerListener {
    public static final String TAG = "MapView";
    public static final boolean DEBUG = false;

    private Paint mDebugPaint;
    private TileManager mTileManager;
    private GestureDetector mGestureDetector;

    private int tileSizeX;
    private int tileSizeY;
    private int tileCountX;
    private int tileCountY;

    private int firstVisibleColumn;
    private int lastVisibleColumn;
    private int firstVisibleRow;
    private int lastVisibleRow;

    private RectF firstRectDrawTile;

    private RectF mSourceRect = null;
    private RectF mFrameRect = null;

    public BaseMapView(Context context) {
        super(context);
        init();
    }

    public BaseMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public abstract TileManager getTileManager();

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mFrameRect = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
        log(String.format("vrect:%s srect%s", mFrameRect, mSourceRect));

        if(mTileManager !=null) preLoadTile();
    }

    public void init() {

        mTileManager = getTileManager();
        mTileManager.setTileManagerListener(this);

        TileResource tileResource = mTileManager.getTileDownloader();

        if(tileResource == null)
            throw new IllegalArgumentException("TileResource not be null");

        tileSizeX = tileResource.getWidthTile();
        tileSizeY = tileResource.getHeightTile();
        tileCountX = tileResource.getCountColumnTile();
        tileCountY = tileResource.getCountRowTile();

        if(tileSizeX <=0 || tileSizeY <=0 || tileCountX <=0 || tileCountY <= 0)
            throw new IllegalArgumentException("TileResource invalid tile settings");

        mSourceRect = new RectF(0, 0, tileSizeX * tileCountX, tileSizeY * tileCountY);

        mDebugPaint = new Paint();
        mDebugPaint.setColor(Color.BLACK);
        mDebugPaint.setStrokeWidth(2);
        mDebugPaint.setTextSize(20);
        mDebugPaint.setStyle(Paint.Style.STROKE);

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                setTranslateMap(-distanceX, -distanceY);
                return true;
            }
        });

        firstRectDrawTile = new RectF();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            mTileManager.cancelLoad();
        }
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    public void setTranslateMap(float dx, float dy) {
        if (!mFrameRect.contains(mSourceRect)) {
            mSourceRect.left += dx;
            mSourceRect.right += dx;
            mSourceRect.top += dy;
            mSourceRect.bottom += dy;
            checkMoveBounds();
        }
        preLoadTile();
        invalidate();
    }

    private void checkMoveBounds() {
        float diff = mSourceRect.left - mFrameRect.left;
        if (diff > 0) {
            mSourceRect.left -= diff;
            mSourceRect.right -= diff;

        }
        diff = mSourceRect.right - mFrameRect.right;
        if (diff < 0) {
            mSourceRect.left -= diff;
            mSourceRect.right -= diff;
        }

        diff = mSourceRect.top - mFrameRect.top;
        if (diff > 0) {
            mSourceRect.top -= diff;
            mSourceRect.bottom -= diff;
        }
        diff = mSourceRect.bottom - mFrameRect.bottom;
        if (diff < 0) {
            mSourceRect.top -= diff;
            mSourceRect.bottom -= diff;
        }
    }

    public int getColumnTile(float screenX) {
        return (int) (screenX - mSourceRect.left) / tileSizeX;
    }

    public int getRowTile(float screenY) {
        return (int) (screenY - mSourceRect.top) / tileSizeY;
    }

    public RectF getFrameBoundsTile(RectF source, int raw, int column){
        source.set(0, 0, tileSizeX, tileSizeY);
        source.offsetTo(raw * tileSizeX + mSourceRect.left,
                column * tileSizeY + mSourceRect.top);
        return source;
    }

    private void preLoadTile(){
        firstVisibleColumn = getColumnTile(mFrameRect.left);
        firstVisibleRow = getRowTile(mFrameRect.left);
        lastVisibleColumn = getColumnTile(mFrameRect.right);
        lastVisibleRow =  getRowTile(mFrameRect.bottom);

        firstRectDrawTile = getFrameBoundsTile(firstRectDrawTile, firstVisibleColumn, firstVisibleRow);

        mTileManager.updateVisibleTile(firstVisibleColumn, lastVisibleColumn, firstVisibleRow, lastVisibleRow);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mTileManager == null || mSourceRect == null || mFrameRect == null) return;

        Bitmap tileBitmap = null;

        int columnTile = firstVisibleColumn;
        int rowTileY = firstVisibleRow;

        float startDrawX = firstRectDrawTile.left;
        float startDrawY = firstRectDrawTile.top;

        float endDrawX = Math.min(mFrameRect.right, mSourceRect.right);
        float endDrawY = Math.min(mFrameRect.bottom, mSourceRect.bottom);


        while (startDrawY < endDrawY) {
            while (startDrawX < endDrawX) {

                tileBitmap = mTileManager.getBitmapTile(columnTile, rowTileY);
                if (tileBitmap != null && !tileBitmap.isRecycled())
                    canvas.drawBitmap(tileBitmap, startDrawX, startDrawY, mDebugPaint);


                if (DEBUG) {
                    Integer tileId = mTileManager.getTileId(columnTile, rowTileY);
                    canvas.drawRect(startDrawX, startDrawY,
                            startDrawX + tileSizeX, startDrawY + tileSizeY, mDebugPaint);
                    canvas.drawText(String.format("%d", tileId),
                            startDrawX, startDrawY + 20, mDebugPaint);
                }

                columnTile += 1;
                startDrawX = startDrawX + tileSizeX;
            }

            columnTile = this.firstVisibleColumn;
            rowTileY += 1;

            startDrawX = firstRectDrawTile.left;
            startDrawY = startDrawY + tileSizeY;
        }

    }

    public void log(String s) {
        if (DEBUG) Log.d(TAG, s);
    }

    @Override
    public void loadedTile(int tileId) {
        postInvalidate();
    }

    @Override
    public void errorTile(int idTile, Exception exc) {
        if(DEBUG) log(String.format("tileId:%d Exception:%s", idTile, exc.getMessage()));
    }
}
