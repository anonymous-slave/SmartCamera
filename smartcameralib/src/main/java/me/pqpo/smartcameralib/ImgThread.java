package me.pqpo.smartcameralib;

import android.graphics.Bitmap;
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


//    public ImgThread(byte[] data, Size size, TCPClient mTcpClient){
//        this.size=size;
//        this.data = data;
//        this.myTcpClient = mTcpClient;
//    }

    public ImgThread(SmartCameraView smartCameraView, TCPClient mTcpClient){
        this.camera = smartCameraView;
        this.myTcpClient = mTcpClient;
    }

    @Override
    public void run() {
//        Bitmap bm = rawByteArray2RGBABitmap2(this.data, this.size.getWidth(), this.size.getHeight());
        size = camera.getSizenow();
        Bitmap bm = rawByteArray2RGBABitmap2(camera.getDatanow(), size.getWidth(), size.getHeight());

        bm = Bitmap.createScaledBitmap(bm, 1920, 1080, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
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
}
