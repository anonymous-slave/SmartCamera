package me.pqpo.smartcamera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
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

import java.util.ArrayList;
import java.util.List;

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
    private static String ts_last = "";
    private static long total_time = 0;
    private static Integer nFrame = 0;
    private static Integer targetFPS = 3;
    private static long last_timestamp = 0;
    private static long start_timestamp = 0;



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
                new Profiler().execute("");
//                new connectTask().execute("");
//                mCameraView.takePicture();
//                mCameraView.stopScan();
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }

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
                    long timeStamp = System.currentTimeMillis();
                    if (last_timestamp == 0) {
                        start_timestamp = timeStamp;
                    }
                    if (timeStamp - last_timestamp > 1000 / targetFPS) {
                        publishProgress(message);
                        nFrame += 1;
                        last_timestamp = timeStamp;
                        Log.e("test FPS",  Float.toString( (float)nFrame/( ((float)(timeStamp - start_timestamp))/1000)));
                    }
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
//            nFrame += 1;
//            String tmp = values[0].replaceAll("\\s+", "");
//            if (ts_last != "") {
//                Long time_diff = Long.valueOf(tmp) - Long.valueOf(ts_last);
//                Log.e("test Time diff ", Long.toString(time_diff));
//                total_time += time_diff;
//            }
//            ts_last = tmp;
//            Log.e("test FPS", Float.toString( (float)nFrame / (total_time / 1000000) ));

//            //in the arrayList we add the messaged received from server
//            arrayList.add(values[0]);
//            // notify the adapter that the data set has changed. This means that new message received
//            // from server was added to the list
//            mAdapter.notifyDataSetChanged();
            new ImgThread(mCameraView,mTcpClient,values[0],80,100, 1920,1080).start();

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





    public static class Profiler extends AsyncTask<String,String, TCPClient> {
        private int profile_fps = 15;
        private int n_fps = 3;

        private int profile_quality = 100;
        private int n_quality = 4;

        private int profile_height = 1920;
        private int profile_width = 1080;
        private int n_res = 2;

        private int n_test = 10;
        private int n_count = 0;
        private boolean profile_done = false;

        @Override
        protected TCPClient doInBackground(String... message) {
            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                //here the messageReceived method is implemented

                public void messageReceived(String message) {
                    long timeStamp = System.currentTimeMillis();
                    if (last_timestamp == 0) {
                        start_timestamp = timeStamp;
                    }
                    if (timeStamp - last_timestamp > 1000 / profile_fps) {
                        publishProgress(message);
                        nFrame += 1;
                        last_timestamp = timeStamp;
                        Log.e("test FPS",  Float.toString( (float)nFrame/( ((float)(timeStamp - start_timestamp))/1000)));
                    }
                }
            });
            mTcpClient.run();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.e("tcpReceived", values[0]);
            this.getConfigure(values[0]);
            new ImgThread(mCameraView,mTcpClient, values[0], this.profile_fps, this.profile_quality, this.profile_height, this.profile_width).start();
        }

        private void getConfigure(String x) {
            if (n_count % n_test != 0 || profile_done) {

            } else if (n_count == n_test * n_fps * n_quality * n_res) {
                Log.e("Profile", x);
                profile_done = true;
            } else {
                int n = n_count / n_test;

                if ( ( n / n_quality / n_res ) % n_fps == 0) {
                    profile_fps = 15;
                } else if ( ( n / n_quality / n_res ) % n_fps == 1) {
                    profile_fps = 10;
                } else if ( ( n / n_quality / n_res ) % n_fps == 2) {
                    profile_fps = 5;
                }

                if ( ( n / n_res ) % n_quality == 0) {
                    profile_quality = 100;
                } else if ( ( n / n_res ) % n_quality == 1) {
                    profile_quality = 70;
                } else if ( ( n / n_res ) % n_quality == 2) {
                    profile_quality = 40;
                } else if ( ( n / n_res ) % n_quality == 3) {
                    profile_quality = 10;
                }

                if ( n % n_res == 0 ) {
                    profile_height = 1920; profile_width = 1080;
                } else if ( n % n_res == 1 ) {
                    profile_height = 1080; profile_width = 720;
                }
            }
            Log.e("Profile", Integer.toString(n_count) + " " + Integer.toString(profile_height)  + " " +  Integer.toString(profile_width)  + " " +  Integer.toString(profile_fps)  + " " +   Integer.toString(profile_quality) );
            n_count += 1;

        }
    }
}
