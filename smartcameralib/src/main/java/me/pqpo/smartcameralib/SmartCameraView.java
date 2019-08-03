package me.pqpo.smartcameralib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;

import com.google.android.cameraview.CameraImpl;
import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.base.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import me.pqpo.smartcameralib.utils.BitmapUtil;

/**
 * Created by pqpo on 2018/8/15.
 */
public class SmartCameraView extends CameraView {

    private static final String TAG = "SmartCameraView";

    protected SmartScanner smartScanner;

    protected MaskViewImpl maskView;
    protected boolean scanning = true;
    private Handler uiHandler;
    private OnScanResultListener onScanResultListener;

    public SmartCameraView(@NonNull Context context) {
        this(context, null);
    }

    public SmartCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setMaskView(new MaskView(context));
    }

    private void init() {
        smartScanner = new SmartScanner();
        uiHandler = new ScanResultHandler(this);

        addCallback(new CameraImpl.Callback() {
            @Override
            public void onPicturePreview(CameraImpl camera, byte[] data) {
                super.onPicturePreview(camera, data);
                if (data == null || data.length == 0 || !scanning) {
                    return;
                }
                int previewRotation = getPreviewRotation();
                Size size = getPreviewSize();
                Rect revisedMaskRect = getAdjustPreviewMaskRect();
                if (revisedMaskRect != null && size != null) {
//                    int result = smartScanner.previewScan(data, size.getWidth(), size.getHeight(), previewRotation, revisedMaskRect);
//                    uiHandler.obtainMessage(result, data).sendToTarget();
                    Bitmap bm = rawByteArray2RGBABitmap2(data, size.getWidth(), size.getHeight());
                    bm = Bitmap.createScaledBitmap(bm, 1920, 1080, true);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
                    byte[] b = baos.toByteArray();
                    String encodedImage = Base64.encodeToString(b , Base64.DEFAULT);

                    //Storage the image to sdcard
//                    try {
//                        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smart_camera/";
//                        String fileName = UUID.randomUUID().toString();
//                        String state = Environment.getExternalStorageState();
//                        if (!state.equals(Environment.MEDIA_MOUNTED)) {
//                            Log.d("cannot mounted","cannot mounted");
//                        }
//                        File file = new File(dir+fileName+".jpg");
//                        FileOutputStream out = new FileOutputStream(file);
//                        bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                        out.flush();
//                        out.close();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }

                    Log.d("base64_encoding",encodedImage);
                }
            }
        });
    }

    public Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
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

    public SmartScanner getSmartScanner() {
        return smartScanner;
    }

    public MaskViewImpl getMaskView() {
        return maskView;
    }

    public Bitmap getPreviewBitmap() {
        return smartScanner.getPreviewBitmap();
    }

    public void setOnScanResultListener(OnScanResultListener onScanResultListener) {
        this.onScanResultListener = onScanResultListener;
    }

    public Rect getAdjustPictureMaskRect() {
        Size size = getPictureSize();
        return getAdjustMaskRect(size);
    }

    public Rect getAdjustPreviewMaskRect() {
        Size size = getPreviewSize();
        return getAdjustMaskRect(size);
    }

    public Rect getAdjustMaskRect(Size size) {
        if (size != null) {
            int previewRotation = getPreviewRotation();
            RectF maskRect = getMaskRect();
            int cameraViewWidth = getWidth();
            int cameraViewHeight = getHeight();
            int picW;
            int picH;
            if (previewRotation == 90 || previewRotation == 270) {
                picW = size.getHeight();
                picH = size.getWidth();
            } else {
                picW = size.getWidth();
                picH = size.getHeight();
            }
            float radio = Math.min(1.0f * picW / cameraViewWidth, 1.0f * picH / cameraViewHeight);
            int maskX = (int) ((int) maskRect.left * radio);
            int maskY = (int) ((int) maskRect.top * radio);
            int maskW = (int) ((int) maskRect.width() * radio);
            int maskH = (int) ((int) maskRect.height() * radio);
            return new Rect(maskX, maskY, maskX + maskW, maskY + maskH);
        }
        return null;
    }

    public void startScan() {
        scanning = true;
    }

    public void stopScan() {
        scanning = false;
    }

    public void setMaskView(MaskViewImpl maskView) {
        if (this.maskView == maskView) {
            return;
        }
        if (this.maskView != null) {
            removeView(this.maskView.getMaskView());
        }
        this.maskView = maskView;
        addView(maskView.getMaskView());
    }

    public RectF getMaskRect() {
        if (maskView == null) {
            return null;
        }
        return maskView.getMaskRect();
    }

    public Bitmap cropYuvImage(final byte[] data, int width, int height, Rect maskRect, int rotation) {
        Bitmap bitmap = Bitmap.createBitmap(maskRect.width(), maskRect.height(), Bitmap.Config.ARGB_8888);
        SmartScanner.crop(data, width, height, rotation, maskRect.left, maskRect.top, maskRect.width(), maskRect.height(), bitmap);
        return bitmap;
    }

    public void cropJpegImage(final byte[] data, final CropCallback cropCallback) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Bitmap bitmapSrc = BitmapFactory.decodeByteArray(data, 0, data.length);
                int rotation = BitmapUtil.getOrientation(data);
                if (rotation != 0) {
                    Matrix m = new Matrix();
                    m.setRotate(rotation);
                    bitmapSrc = Bitmap.createBitmap(bitmapSrc, 0, 0, bitmapSrc.getWidth(), bitmapSrc.getHeight(), m, true);
                }
                Rect revisedMaskRect = getAdjustPictureMaskRect();
                if (revisedMaskRect != null) {
                    final Bitmap bitmap = Bitmap.createBitmap(bitmapSrc, revisedMaskRect.left, revisedMaskRect.top,
                            revisedMaskRect.width(), revisedMaskRect.height());
                    bitmapSrc.recycle();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            cropCallback.onCropped(bitmap);
                        }
                    });
                    return;
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        cropCallback.onCropped(null);
                    }
                });
            }
        }.start();
    }

    public interface CropCallback {
        void onCropped(Bitmap cropBitmap);
    }

    public interface OnScanResultListener {
        boolean onScanResult(SmartCameraView smartCameraView, int result, byte[] yuvData);
    }

    private static class ScanResultHandler extends Handler {

        SmartCameraView smartCameraView;

        public ScanResultHandler(SmartCameraView smartCameraView) {
            this.smartCameraView = smartCameraView;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(!smartCameraView.scanning) {
                return;
            }
            int result = msg.what;
            byte[] data = (byte[]) msg.obj;
            if (smartCameraView.onScanResultListener == null
                    || !smartCameraView.onScanResultListener.onScanResult(smartCameraView, result, data)) {
                if (result == 1) {
                    smartCameraView.takePicture();
                    smartCameraView.stopScan();
                }
            }
        }
    }

}
