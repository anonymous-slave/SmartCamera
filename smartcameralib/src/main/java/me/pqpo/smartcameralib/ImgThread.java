package me.pqpo.smartcameralib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.google.android.cameraview.base.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import me.pqpo.smartcameralib.utils.TCPClient;

public class ImgThread extends Thread{
    private byte[] data;
    private Size size;
    private TCPClient myTcpClient;
    private SmartCameraView camera;
    private String timestamp;
    private String imgMode = "video"; // video & camera
    private int q;
    private int fps;
    private int width;
    private int height;


//    public ImgThread(byte[] data, Size size, TCPClient mTcpClient){
//        this.size=size;
//        this.data = data;
//        this.myTcpClient = mTcpClient;
//    }

    public ImgThread(SmartCameraView smartCameraView, TCPClient mTcpClient, String timestamp, int fps, int quality, int width, int height){
        this.camera = smartCameraView;
        this.myTcpClient = mTcpClient;
        this.timestamp = timestamp;
        this.q = quality;
        this.fps = fps;
        this.width = width;
        this.height = height;
    }

    @Override
    public void run() {
//        Bitmap bm = rawByteArray2RGBABitmap2(this.data, this.size.getWidth(), this.size.getHeight());
        Bitmap bm;
        size = camera.getSizenow();
        int width = size.getWidth();
        int height = size.getHeight();

        int dstWidth = this.width;
        int dstHeight = this.height;
        int quality = this.q;

        if (imgMode.equals("camera")){
            bm = rawByteArray2RGBABitmap2(camera.getDatanow(), width, height);
        } else{
            long timeStamp = System.currentTimeMillis();
            Log.e("Video TimeStamp", Long.toString(timeStamp%60000*1000));
            bm = getVideoThumnail("output.mkv", timeStamp%60000*1000);
        }


        bm = Bitmap.createScaledBitmap(bm, dstWidth, dstHeight, true);

        //add the timestamp
        Canvas canvas=new Canvas(bm);//创建一个空画布，并给画布设置位图
        Paint p=new Paint();

        p.setColor(Color.WHITE);       //设置画笔颜色
        p.setStyle(Paint.Style.FILL);  //设置画笔模式为填充
        p.setStrokeWidth(10f);         //设置画笔宽度为10px
        canvas.drawRect(Math.round(dstWidth * 3 / 4),Math.round(dstHeight * 3 / 4),dstWidth,dstHeight,p);


        p.setColor(Color.BLACK);//设置画笔颜色
        p.setAntiAlias(true);//抗锯齿
        p.setTextSize(60);//设置字体大小
        canvas.drawText(this.timestamp,Math.round(dstWidth  * 3 / 4) + 20,Math.round(dstHeight * 3 / 4)+130,p);//在画布上绘制文字，即在位图上绘制文字

        Log.i("width:",Integer.toString(Math.round(dstWidth * 3 / 4)));
        Log.i("height:",Integer.toString(Math.round(dstHeight * 3 / 4)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, quality, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b , Base64.DEFAULT);
        this.myTcpClient.sendMessage(Integer.toString(encodedImage.length()));
        this.myTcpClient.sendMessage(encodedImage);

        //Save the image to sdcard
//        try {
//            String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smart_camera/";
//            String fileName = UUID.randomUUID().toString();
//            String state = Environment.getExternalStorageState();
//            if (!state.equals(Environment.MEDIA_MOUNTED)) {
//                Log.d("cannot mounted","cannot mounted");
//            }
//            File file = new File(dir+fileName+".jpg");
//            FileOutputStream out = new FileOutputStream(file);
//            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            out.flush();
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1)]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);
                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0 , width, 0, 0, width, height);
        return bmp;
    }

    private static Bitmap getVideoThumnail(String videoName, long timeUs) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smart_camera/";
        media.setDataSource(dir + videoName);
        // 获取帧
        // OPTION_CLOSEST 在给定的时间，检索最近一个帧，这个帧不一定是关键帧。
        // OPTION_CLOSEST_SYNC 在给定的时间，检索最近一个关键帧。
        // OPTION_NEXT_SYNC 在给定时间之后，检索一个关键帧。
        // OPTION_PREVIOUS_SYNC 在给定时间之前，检索一个关键帧。
        return media.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
    }
}
