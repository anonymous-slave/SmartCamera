package me.pqpo.smartcamera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.cameraview.CameraImpl;
import com.google.android.cameraview.base.Size;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import me.pqpo.smartcameralib.ImgThread;
import me.pqpo.smartcameralib.MaskView;
import me.pqpo.smartcameralib.SmartCameraView;
import me.pqpo.smartcameralib.SmartScanner;
import me.pqpo.smartcameralib.utils.TCPClient;


public class MainActivity extends AppCompatActivity {

    private static SmartCameraView mCameraView;
    private ImageView ivPreview;
    private AlertDialog alertDialog;
    private ImageView ivDialog;
    private boolean granted = false;
    private static TCPClient mTcpClient;

    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.camera_view);
        ivPreview = findViewById(R.id.image);

        ivPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                new ImgThread(mCameraView,mTcpClient).start();
                new connectTask().execute("");
//                mCameraView.takePicture();
//                mCameraView.stopScan();
            }
        });

//        if (Build.VERSION.SDK_INT >= 23) {
//            int REQUEST_CODE_CONTACT = 101;
//            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
//            for (String str : permissions) {
//                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
//                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
//                    return;
//                }
//            }
//        }

        initMaskView();
        initScannerParams();
        initCameraView();



        new RxPermissions(this).request(Manifest.permission.CAMERA)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean granted) {
                        MainActivity.this.granted = granted;
                        if (granted) {
                            MaskView maskView = (MaskView) mCameraView.getMaskView();
                            maskView.setShowScanLine(true);
                            mCameraView.start();
                            mCameraView.startScan();
                        } else {
                            Toast.makeText(MainActivity.this, "请开启相机权限！", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

//
    public static class connectTask extends AsyncTask<String,String,TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
//                    Log.e("tcpReceived1", message);
                }
            });
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.e("tcpReceived", values[0]);

//            //in the arrayList we add the messaged received from server
//            arrayList.add(values[0]);
//            // notify the adapter that the data set has changed. This means that new message received
//            // from server was added to the list
//            mAdapter.notifyDataSetChanged();
            new ImgThread(mCameraView,mTcpClient).start();

        }
    }

    private void initScannerParams() {
        SmartScanner.DEBUG = true;
        /*
          canny 算符阈值
          1. 低于阈值1的像素点会被认为不是边缘；
          2. 高于阈值2的像素点会被认为是边缘；
          3. 在阈值1和阈值2之间的像素点,若与第2步得到的边缘像素点相邻，则被认为是边缘，否则被认为不是边缘。
         */
        SmartScanner.cannyThreshold1 = 20; //canny 算符阈值1
        SmartScanner.cannyThreshold2 = 50; //canny 算符阈值2
        /*
         * 霍夫变换检测线段参数
         * 1. threshold: 最小投票数，要检测一条直线所需最少的的曲线交点，增大该值会减少检测出的线段数量。
         * 2. minLinLength: 能组成一条直线的最少点的数量, 点数量不足的直线将被抛弃。
         * 3. maxLineGap: 能被认为在一条直线上的点的最大距离，若出现较多断断续续的线段可以适当增大该值。
         */
        SmartScanner.houghLinesThreshold = 130;
        SmartScanner.houghLinesMinLineLength = 80;
        SmartScanner.houghLinesMaxLineGap = 10;
        /*
         * 高斯模糊半径，用于消除噪点，必须为正奇数。
         */
        SmartScanner.gaussianBlurRadius = 3;

        // 检测范围比例, 比例越小表示待检测物体要更靠近边框
        SmartScanner.detectionRatio = 0.1f;
        // 线段最小长度检测比例
        SmartScanner.checkMinLengthRatio = 0.8f;
        // 为了提高性能，检测的图片会缩小到该尺寸之内
        SmartScanner.maxSize = 300;
        // 检测角度阈值
        SmartScanner.angleThreshold = 5;
        // don't forget reload params
        SmartScanner.reloadParams();
    }

    private void initCameraView() {
        mCameraView.getSmartScanner().setPreview(true);
        mCameraView.setOnScanResultListener(new SmartCameraView.OnScanResultListener() {
            @Override
            public boolean onScanResult(SmartCameraView smartCameraView, int result, byte[] yuvData) {
                Bitmap previewBitmap = smartCameraView.getPreviewBitmap();
                if (previewBitmap != null) {
                    ivPreview.setImageBitmap(previewBitmap);
                }
                if (result == 1) {
                    Size pictureSize = smartCameraView.getPreviewSize();
                    int rotation = smartCameraView.getPreviewRotation();
                    Rect maskRect = mCameraView.getAdjustPreviewMaskRect();
                    Bitmap bitmap = mCameraView.cropYuvImage(yuvData, pictureSize.getWidth(), pictureSize.getHeight(), maskRect, rotation);
                    if (bitmap != null) {
                        showPicture(bitmap);
                    }
                }
                return false;
            }
        });

        mCameraView.addCallback(new CameraImpl.Callback() {

            @Override
            public void onCameraOpened(CameraImpl camera) {
                super.onCameraOpened(camera);
            }

            @Override
            public void onPictureTaken(CameraImpl camera, byte[] data) {
                super.onPictureTaken(camera, data);
                mCameraView.cropJpegImage(data, new SmartCameraView.CropCallback() {
                    @Override
                    public void onCropped(Bitmap cropBitmap) {
                        if (cropBitmap != null) {
                            showPicture(cropBitmap);
                        }
                    }
                });
            }

        });
    }

    private void initMaskView() {
        final MaskView maskView = (MaskView) mCameraView.getMaskView();
        maskView.setMaskLineColor(0xff00adb5);
        maskView.setShowScanLine(false);
        maskView.setScanLineGradient(0xff00adb5, 0x0000adb5);
        maskView.setMaskLineWidth(2);
        maskView.setMaskRadius(5);
        maskView.setScanSpeed(6);
        maskView.setScanGradientSpread(80);
        mCameraView.post(new Runnable() {
            @Override
            public void run() {
                int width = mCameraView.getWidth();
                int height = mCameraView.getHeight();
                maskView.setMaskSize((int) width , height);
//                if (width < height) {
//                    maskView.setMaskSize((int) (width * 0.6f), (int) (width * 0.6f / 0.63));
//                    maskView.setMaskOffset(0, -(int)(width * 0.1));
//                } else {
//                    maskView.setMaskSize((int) (width * 0.6f), (int) (width * 0.6f * 0.63));
//                }
            }
        });
        mCameraView.setMaskView(maskView);
    }

    private void showPicture(Bitmap bitmap) {
        if (alertDialog == null) {
            ivDialog = new ImageView(this);
            alertDialog = new AlertDialog.Builder(this).setView(ivDialog).create();
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mCameraView.startScan();
                }
            });
            Window window = alertDialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.color.colorTrans);
            }
        }
        ivDialog.setImageBitmap(bitmap);
        alertDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // request Camera permission first!
        if (granted) {
            mCameraView.start();
            mCameraView.startScan();
        }
    }


    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        mCameraView.stopScan();
    }
}
