package me.pqpo.smartcameralib;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.android.cameraview.base.Size;

import java.io.ByteArrayOutputStream;

import me.pqpo.smartcameralib.utils.TCPClient;

class ImgThread extends Thread{
    private byte[] data;
    private Size size;
    private TCPClient myTcpClient;


    public ImgThread(byte[] data, Size size, TCPClient mTcpClient){
        this.size=size;
        this.data = data;
        this.myTcpClient = mTcpClient;
    }
    @Override
    public void run() {
        Bitmap bm = rawByteArray2RGBABitmap2(this.data, this.size.getWidth(), this.size.getHeight());
        bm = Bitmap.createScaledBitmap(bm, 1920, 1080, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b , Base64.DEFAULT);
        this.myTcpClient.sendMessage(Integer.toString(encodedImage.length()));
        this.myTcpClient.sendMessage(encodedImage);
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
