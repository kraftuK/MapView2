package com.test.kraftu.mapview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Created by kraftu on 23.02.17.
 */

public class MapView extends View {

    public static final String TAG = "MapView";
    public static final boolean DEBUG = true;

    private static int TILE_SIZE_X = 256;
    private static int TILE_SIZE_Y = 256;
    private static int COUNT_TILE_X = 100;
    private static int COUNT_TILE_Y = 100;

    float xLastTouch;
    float yLastTouch;

    RectF sourceRect = null;
    RectF frameRect = null;
    RectF drawTile = new RectF();

    Paint paint;
    Bitmap defTile;

    HashMap<Integer,Bitmap> bitmapList = new HashMap<>();
    HashMap<Integer,LoadTile> listTask = new HashMap<>();

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
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        paint.setTextSize(20);
        paint.setStyle(Paint.Style.STROKE);
        sourceRect = new RectF(0,0,TILE_SIZE_X * COUNT_TILE_X,TILE_SIZE_Y * COUNT_TILE_Y);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        defTile = Bitmap.createBitmap(TILE_SIZE_X,TILE_SIZE_Y, conf);
        Canvas canvas = new Canvas(defTile);
        canvas.drawColor(Color.parseColor("#cccccc"));

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
        log(String.format("----"));
        log(String.format("dx:%f dy:%f",dx,dy));
        sourceRect.left = sourceRect.left + dx;
        sourceRect.right = sourceRect.right + dx;
        sourceRect.top = sourceRect.top + dy;
        sourceRect.bottom = sourceRect.bottom + dy;
        checkMoveBounds();
        invalidate();
        log(String.format("vrect:%s srect%s", frameRect, sourceRect));
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

    public void startLoadTile(int tileX,int tileY){
        int tileId = getTileId(tileX,tileY);
        if(!listTask.containsKey(tileId)){
            LoadTile lt = new LoadTile(tileX,tileY,tileId);
            listTask.put(tileId,lt);
            lt.execute();
        }
    }

    public int getTileRawX(float screenX){

        return (int) (screenX - sourceRect.left) / TILE_SIZE_X;
    }

    public int getTileRawY(float screenY){
        return (int)(screenY - sourceRect.top) / TILE_SIZE_Y;
    }

    public float getLocationTileX(int tileX){
        return tileX * TILE_SIZE_X + sourceRect.left;
    }

    public float getLocationTileY(int tileY){
        return tileY * TILE_SIZE_Y + sourceRect.top;
    }

    public int getTileId(int tileX,int tileY){
        return tileY*TILE_SIZE_X + tileX;
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

        while(drawTileX < frameRect.right){
            while(drawTileY < frameRect.bottom){
                drawTile.set(drawTileX,drawTileY,drawTileX + TILE_SIZE_X,drawTileY + TILE_SIZE_Y);

                currentTileX = getTileRawX(drawTile.centerX());
                currentTileY = getTileRawY(drawTile.centerY());

                Bitmap tile = bitmapList.get(getTileId(currentTileX,currentTileY));

                if(tile!=null){
                    canvas.drawBitmap(tile,drawTileX,drawTileY,paint);
                }else{
                    startLoadTile(currentTileX,currentTileY);
                }
                canvas.drawRect(drawTile,paint);
                if(DEBUG)canvas.drawText(String.format("%d",getTileId(currentTileX,currentTileY))
                        ,drawTile.centerX(),drawTile.centerY(),paint);
                drawTileY = drawTileY + TILE_SIZE_Y;
            }
            drawTileX = drawTileX + TILE_SIZE_X;
            drawTileY = sourceRect.top;
        }

    }
    public void log(String s){
        if(DEBUG)Log.d(TAG,s);
    }

    public class LoadTile extends AsyncTask<Void,Void,Void>{
        int tileX;
        int tileY;
        int tileId;
        Bitmap bitmap = null;

        public LoadTile(int tileX, int tileY, int tileId) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.tileId = tileId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            bitmap = getBitmapFromURL(String.format("http://b.tile.opencyclemap.org/cycle/16/%d/%d.png ",33198+tileX,22539+tileY));
            return null;
        }

        public Bitmap getBitmapFromURL(String src) {
            try {
                URL url = new URL(src);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            bitmapList.put(tileId,bitmap);
            invalidate();
        }
    }
}
