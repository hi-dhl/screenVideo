## 获取视频截图

>最近在搞一个视频截图的功能，自己在探索过程中，看到很多各种各样的解法，走了很多弯路，为了避免做相同功能的朋友们走很多弯路，我把自己解决方案，及探索过程遇到的Bug记录下来
>
>screenVideo是一个通用的视频截图工具，目前已经适配大部分机型，对于个别机型不能使用的欢迎issuses，Demo中的视频的url不可用，视频的url可以从任意视频网站找一个可以播放的视频地址，用火狐获取一下播放的URL,替换demo中的url即可 [Github下载](https://github.com/hi-dhl/screenVideo)




最开始想的是直接用View截图的方式截取当前的视频，结果截取的来的图片是黑屏，附上View截图代码

```
public  Bitmap convertViewToBitmap(View view){
    view.destroyDrawingCache();//销毁旧的cache销毁，获取cache通常会占用一定的内存，所以通常不需要的时候有必要对其进行清理
    view.setDrawingCacheEnabled(true);//cache开启
    view.buildDrawingCache();//创建新的缓存,获取cache通常会占用一定的内存，所以通常不需要的时候有必要对其进行清理,在每次获取新的,先销毁旧的缓存
    view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));//测量view
    Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());//将缓存的view转换为图片
    return bitmap;
}
```

上面的方式只能对静态的View进行截图，但是动态的比如说视频，那么截出来的图片就是黑屏。
用SurfaceView显示在线视频，然后通过上面截图方式，得到图片是黑屏，（关于黑屏的原因大家可以去网上搜索，可以得到你想要的答案，这里就不在说了）于是我就去谷歌，各大博客上寻求解决方案，发现Android提供了MediaMetadataRetriever这个类来获取缩放图，于是按照这个思路去搜索，发现可以通过获取能够获取当前播放的帧数，来进行截图，以下是我的最终解决方案

```
/**
 * 视频截图代码
 * @param url   播放的url
 * @param width 生成图片的宽度
 * @param height    生成图片的高度
 * @param currentVideoTime  当前播放的播放的秒数
 * @return
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
private Bitmap createVideoThumbnail(String url, int width, int height,String currentVideoTime) {
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
```

[Github下载地址 : https://github.com/denghuilong/screenVideo/tree/master](https://github.com/denghuilong/screenVideo/tree/master)

## bug及解决方案

#### start called in state 4

```
04-05 10:58:14.169 2237-2237/demo.dhl.con.onlinevideo E/MediaPlayer: start called in state 4
04-05 10:58:14.169 2237-2237/demo.dhl.con.onlinevideo E/MediaPlayer: error (-38, 0)
04-05 10:58:14.169 2237-2237/demo.dhl.con.onlinevideo E/MediaPlayer: Error (-38,0)
04-05 10:58:14.176 2237-2250/demo.dhl.con.onlinevideo E/MediaPlayer: error (261, -1003)
04-05 10:58:14.176 2237-2237/demo.dhl.con.onlinevideo E/MediaPlayer: Error (261,-1003)
```

可能由于的播放的文件错误，或者给的url地址不能播放，可以在浏览器中试一下。

#### start called in state 1

```
04-05 11:50:27.346 2038-2038/demo.dhl.con.onlinevideo E/MediaPlayer: start called in state 1
04-05 11:50:27.347 2038-2038/demo.dhl.con.onlinevideo E/MediaPlayer: error (-38, 0)
04-05 11:50:27.367 2038-2050/demo.dhl.con.onlinevideo E/MediaPlayer: error (261, -1003)
04-05 11:50:27.367 2038-2038/demo.dhl.con.onlinevideo E/MediaPlayer: Error (261,-1003)
```

原因：

```
Streaming is Not supported before Android 3.0
Please test in device having above 3.0 version
```

解决方案：

```
![](http://upload-images.jianshu.io/upload_images/1479838-d821c0b136e94734.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
这是Android官网关于，Android所支持的网络协议网络协议
>音频和视频播放支持以下网络协议：
>RTSP协议（RTP，SDP的）
>HTTP / HTTPS的进步流
>HTTP / HTTPS的现场直播议定书草案：
>MPEG-2 TS流媒体文件只
>协议版本3（的Andr​​oid 4.0及以上）
>议定书“第2版（Android的3.x版）
>不支持之前的Andr​​oid 3.0
>注： HTTPS不支持之前的Android 3.1。
更换一台Android3.0以上的设备就好了
```

#### status=0x80000000

```
java.io.IOException: setDataSource failed.: status=0x80000000
```

* 第一种解决方法

```
InputStream in = getResources().getAssets().open("GPSResp.dat");
```

* 第二种解决方案

将播放的视频或者音乐因为转换成Android所支持的格式
下面是Android所支持的格式

```
static const char* kNoCompressExt[] = {
    ".jpg", ".jpeg", ".png", ".gif",
    ".wav", ".mp2", ".mp3", ".ogg", ".aac",
    ".mpg", ".mpeg", ".mid", ".midi", ".smf", ".jet",
    ".rtttl", ".imy", ".xmf", ".mp4", ".m4a",
    ".m4v", ".3gp", ".3gpp", ".3g2", ".3gpp2",
    ".amr", ".awb", ".wma", ".wmv"
};
```
#### java.io.IOException: Prepare failed.: status=0x1

MediaPlay播放视频的时候报下面的错

```
    java.io.IOException: Prepare failed.: status=0x1
```

解决方案：把mediaPlayer.prepare;改成 mediaPlayer.prepareAsync();

#### getFrameAtTime: videoFrame is a NULL pointer

播放视频的时候包下面的错误

```
    getFrameAtTime: videoFrame is a NULL pointer
```

解决方案：视频地址错误，或者 视频损坏不能播放，检查视频是否正常

#### 怎么样对播放的视频进行截图

当我们使用SurfaceView的来显示播放的视频的时候，需要截取视频的时候，直接使用普通View获取截图的方式，会是黑屏，网上很多博客提到了解决方案mHolder.lockCanvas() 获取Canva来获取画布，实现截取视频，其实是错误的，我照着网上的贴子做了，报了下面的错，不知道是不是我的使用方法有错，请网友指正

```
    12:58:24.690: E/BaseSurfaceHolder(719): Exception locking surface
    12:58:24.690: E/BaseSurfaceHolder(719): java.lang.IllegalArgumentException
    12:58:24.690: E/BaseSurfaceHolder(719):   at android.view.Surface.nativeLockCanvas(Native Method)
    12:58:24.690: E/BaseSurfaceHolder(719):   at android.view.Surface.lockCanvas(Surface.java:447)
    12:58:24.690: E/BaseSurfaceHolder(719):   at com.android.internal.view.BaseSurfaceHolder.internalLockCanvas(BaseSurfaceHolder.java:184)
```

原因：
SurfaceView 主要用来两种用法：

1. 和MediaPlay配合使用播放视频，
2. 或者和Canvas配合使用实现一些动画

但是不能这两种方法一起使用或者就会报上面的错。

解决方案：如果想要做视频截取的话，可以使用MediaMetadataRetriever这个类截取当前播放的帧画面，来是现实视频截图功能，项目贴上，代码中有注释

