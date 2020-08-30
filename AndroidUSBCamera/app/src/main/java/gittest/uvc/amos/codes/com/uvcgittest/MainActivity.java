package gittest.uvc.amos.codes.com.uvcgittest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amos.codes.uvc.FileUtils;
import com.amos.codes.uvc.UVCCameraHelper;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback{
    private Button btnPhoto,btnStartRec,btnStopRec,btnRotate,btnStart,btnStop;
    private  int int_rotation=0;
    private RelativeLayout relativeVideo;

    private UVCCameraTextureView uvcCameraTextureView;
    private UVCCameraHelper mCameraHelper;
    private boolean isRequest;
    private boolean isPreview;


    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {
        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }
        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                //showShortMsg(device.getDeviceName() + " is out");
            }
        }
        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            }
        }
        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("disconnecting");
        }
    };

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); //无标题
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); //全屏

        initButtons();  //初始化按钮事件
        initTextureViewSurface();  //初始化播放器控件
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("取消操作");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {
    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
    }

    /**
     * 初始化播放器的Surface
     * 一个UVC专用的
     */
    public void initTextureViewSurface() {
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int Screen_W = outMetrics.widthPixels;
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relativeVideo = (RelativeLayout) findViewById(R.id.surface_layout);
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        float topY = Screen_W * 1.25f;
        int ph = (int) topY;
        params.height = 768;
        params.setMargins(0, 0, 0, 0);
        params.width = 1024;
        relativeVideo.setLayoutParams(params);

        uvcCameraTextureView = new UVCCameraTextureView(this);
        uvcCameraTextureView.setLayoutParams(params);
        relativeVideo.addView(uvcCameraTextureView);
        uvcCameraTextureView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
        mCameraHelper.initUSBMonitor(this, uvcCameraTextureView, listener);
        mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21) {
                showShortMsg("data="+nv21.length);
            }
        });

        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }

    }

    /**
     * 按钮事件
     */
    private void initButtons()
    {
        btnPhoto=(Button)findViewById(R.id.btn_TakePhoto);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return;
                }
                String picPath = "/sdcard/iScopePro/" +"image/"+ System.currentTimeMillis()
                        + UVCCameraHelper.SUFFIX_JPEG;
                Log.e("AmosDemo","save picPath picPath：" + picPath);
                mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        Log.e("AmosDemo","save path：" + path);
                    }
                });
            }
        });


        btnStartRec=(Button)findViewById(R.id.btn_Rec_Start);
        btnStartRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return;
                }
                if (!mCameraHelper.isPushing()) {
                    String videoPath = "/sdcard/iScopePro/" + "image/" + System.currentTimeMillis();
                    FileUtils.createfile(FileUtils.ROOT_PATH + "test666.h264");
                    // if you want to record,please create RecordParams like this
                    RecordParams params = new RecordParams();
                    params.setRecordPath(videoPath);
                    params.setRecordDuration(0);                        // 设置为0，不分割保存
                    // params.setVoiceClose(mSwitchVoice.isChecked());    // is close voice
                    params.setVoiceClose(true);
                    mCameraHelper.startPusher(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                        @Override
                        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                            // type = 1,h264 video stream
                            if (type == 1) {
                                FileUtils.putFileStream(data, offset, length);
                            }
                            // type = 0,aac audio stream
                            if (type == 0) {

                            }
                        }

                        @Override
                        public void onRecordResult(String videoPath) {
                            Log.e("AmosDemo", "videoPath = " + videoPath);
                        }
                    });
                    showShortMsg("start record...");
                }
            }
        });


        btnStopRec=(Button)findViewById(R.id.btn_Rec_Stop);
        btnStopRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.releaseFile();
                mCameraHelper.stopPusher();
                showShortMsg("stop record...");
            }
        });


        btnRotate=(Button)findViewById(R.id.btn_Rotate);
        btnRotate.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onClick(View view) {
                int_rotation+=90;
                if(int_rotation>270)
                {
                    int_rotation=0;
                }
                uvcCameraTextureView.setRotation(int_rotation);
            }
        });

        btnStart=(Button)findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPreview && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.startPreview(uvcCameraTextureView);
                    isPreview = true;
                }
            }
        });

        btnStop=(Button)findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPreview && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.stopPreview();
                    isPreview = false;
                }
            }
        });
    }
}