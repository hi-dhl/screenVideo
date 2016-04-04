package com.dhl.dome.video;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.HashMap;

import demo.dhl.con.onlinevideo.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private Button mBtStart;
    private Button mBitStop;
    public static MediaPlayer mMdeiaPlayer;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private String path;
    private ImageView imgs;
    private View mV;


    private boolean isRun;
//    String url = "http://123.125.110.146/vmind.qqvideo.tc.qq.com/k0200delpn6.p202.1.mp4?vkey=95CC4E33063A3658F9EFD63B1C243FC3B9B002FE63ED9ADDDAF31BD52CB8A84CDCCD4AE7AD28D0F4E6EE992B133E3F453369CDCA4E05D31479008C7B34760B533672359C102664F26DA902843A04C99614246B88A35505E2&platform=&sdtfrom=&fmt=hd&level=0";
    String url = "";

//    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtStart = (Button)this.findViewById(R.id.btn_start);
        mBitStop = (Button)this.findViewById(R.id.btn_stop);
        imgs = (ImageView)this.findViewById(R.id.imgs);

        mBtStart.setOnClickListener(this);
        mBitStop.setOnClickListener(this);


        mMdeiaPlayer = new MediaPlayer();
        mSurfaceView = (SurfaceView)this.findViewById(R.id.sur_face);
        mHolder =  mSurfaceView.getHolder();
        mHolder.addCallback(new MediaCallBack());
        mHolder.setFixedSize(700, 700);
        mHolder.setKeepScreenOn(true);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * View截图代码
     * @param view
     * @return
     */
    public  Bitmap convertViewToBitmap(View view){
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }

    /**
     * 视频截图代码
     * @param url   播放的url
     * @param width 生成图片的宽度
     * @param height    生成图片的高度
     * @return
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private Bitmap createVideoThumbnail(String url, int width, int height) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int kind = MediaStore.Video.Thumbnails.MINI_KIND;
        try {
            if (Build.VERSION.SDK_INT >= 14) {//Android4.0以上的设备,必须使用这种方式来设置源播放视频的路径
                retriever.setDataSource(url, new HashMap<String, String>());
            } else {
                retriever.setDataSource(url);
            }
            int millis = mMdeiaPlayer.getDuration();
            Log.e(TAG, "-----millis----" + millis);
            int pro = mMdeiaPlayer.getCurrentPosition();
            Log.e(TAG,"-----pro----"+pro);
            String timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long time = Long.parseLong(timeString) * 1000; //获取总长度,这一句也是必须的
            long d = time*pro/millis;//计算当前播放的帧数,来截取当前的视频
            Log.e(TAG,"---------"+d);
            bitmap = retriever.getFrameAtTime(d, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (kind == MediaStore.Images.Thumbnails.MICRO_KIND && bitmap != null) {
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            }
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
                mMdeiaPlayer.start();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }

        return bitmap;
    }


    /**
     * @author dhl
     * @param v
     * @desc 点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start:
                if(TextUtils.isEmpty(url)){
                    Toast.makeText(MainActivity.this,"播放的地址有误,请检查",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mMdeiaPlayer.isPlaying()){
                    isRun = true;
                    mMdeiaPlayer.pause();
                }else{
                    mMdeiaPlayer.start();
                }
                play(0);
                break;
            case R.id.btn_stop:
                if(TextUtils.isEmpty(url)){
                    Toast.makeText(MainActivity.this,"播放的地址有误,请检查",Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap bitmap = null;
                try{
                    Bitmap mBitmap = createVideoThumbnail(url, mSurfaceView.getWidth(), mSurfaceView.getHeight());
                    imgs.setImageBitmap(mBitmap);
                }catch(Exception e){
                    e.printStackTrace();
                }
                break;

            default:
                break;
        }
    }

    private Canvas canvas;

    /**
     * @author dhl
     * @desc 视频播放
     */
    private void play(int position) {
        try{
            mMdeiaPlayer.reset();
            mMdeiaPlayer.setDataSource(url);
            mMdeiaPlayer.setDisplay(mHolder);
            mMdeiaPlayer.prepareAsync();
            mMdeiaPlayer.setOnPreparedListener(new MyOnPreparedListener(position));
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private class MyOnPreparedListener implements MediaPlayer.OnPreparedListener{
        private int position;

        public MyOnPreparedListener(int position) {
            this.position = position;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
            if(position>0){
                mMdeiaPlayer.seekTo(position);
            }

        }
    }

    private  class MediaCallBack implements  SurfaceHolder.Callback{

        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            if(position>0){
                play(position);
                position = 0;
            }
        }

        @Override
        public void surfaceChanged(final SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(mMdeiaPlayer.isPlaying()){
                position = mMdeiaPlayer.getCurrentPosition();
                mMdeiaPlayer.stop();
            }

        }
    }

    private static int position;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMdeiaPlayer.release();
        mMdeiaPlayer = null;
    }
}
