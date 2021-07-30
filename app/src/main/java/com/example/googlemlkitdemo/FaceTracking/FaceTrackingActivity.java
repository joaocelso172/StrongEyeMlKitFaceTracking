package com.example.googlemlkitdemo.FaceTracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlemlkitdemo.Model.Company;
import com.example.googlemlkitdemo.R;
import com.example.googlemlkitdemo.sdk.ITempClient;
import com.example.googlemlkitdemo.sdk.LightSensor;
import com.example.googlemlkitdemo.sdk.TempClient;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.truesen.face.sdk.NativeVisionApi;
import com.urovo.sdklibs.OnTempListener;
import com.urovo.sdklibs.SDKManager;
import com.urovo.sdklibs.utils.ToastTool;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FaceTrackingActivity extends AppCompatActivity implements ITempClient {
    public static final int REQUEST_CODE_PERMISSION = 101;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private TextureView tv;
    private ImageView iv, imgCompany;
    private TextView txtCompanyName, txtClock;
    private static final String TAG = "FaceTrackingActivity";

    public static CameraX.LensFacing lens = CameraX.LensFacing.FRONT;

    private Handler handler;
    private TempClient tempClient;
    private int rotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_tracking);
        tv = findViewById(R.id.tracking_texture_view);
        iv = findViewById(R.id.tracking_image_view);
        txtCompanyName = findViewById(R.id.txt_company_name);
        txtClock = findViewById(R.id.txt_clock);
        imgCompany = findViewById(R.id.img_company);
        if (allPermissionsGranted()) {
            tv.post(this::startCamera);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        }
        setElementsText();
        enableOrientantionListener();
        executeSignIn();
        System.out.println("Inicializing real time face detection activity");
    }

    private void setElementsText(){
        txtCompanyName.setText(Company.companyName);
        txtCompanyName.setTextSize(25);
        txtClock.setTextSize(25);
        imgCompany.setImageResource(R.drawable.eye);
        enableTimerListener();

    }

    private void enableTimerListener(){


        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
                Date date = new Date(currentTime);
                String time = simpleDateFormat.format(date);
                txtClock.setText(time);
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void enableOrientantionListener(){

        OrientationEventListener orientationEventListener = new OrientationEventListener((Context)this) {
            @Override
            public void onOrientationChanged(int orientation) {

                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = FirebaseVisionImageMetadata.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = FirebaseVisionImageMetadata.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = FirebaseVisionImageMetadata.ROTATION_90;
                } else {
                    rotation = FirebaseVisionImageMetadata.ROTATION_0;
                }

            }
        };

        orientationEventListener.enable();

    }

    private void openDoor() {
        NativeVisionApi.setGpioDirection(124, 1);
    }

    private void closeDoor() {
        NativeVisionApi.setGpioDirection(124, 0);
    }

    public void executeSignIn() {
        runTempClient();
    }


    @SuppressLint("RestrictedApi")
    private void startCamera() {
        initCamera();
        ImageButton ibSwitch = findViewById(R.id.btn_switch_face);
        ibSwitch.setOnClickListener(v -> {
            if (lens == CameraX.LensFacing.FRONT)
                lens = CameraX.LensFacing.BACK;
            else
                lens = CameraX.LensFacing.FRONT;
            try {
                Log.i(TAG, "" + lens);
                CameraX.getCameraWithLensFacing(lens);
                initCamera();
            } catch (CameraInfoUnavailableException e) {
                Log.e(TAG, e.toString());
            }
        });
    }

    private void initCamera() {
        CameraX.unbindAll();
        @SuppressLint("RestrictedApi") PreviewConfig pc = new PreviewConfig
                .Builder()
                .setDefaultResolution(new Size(960, 720))
                .setTargetRotation(rotation)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setLensFacing(lens)
                .build();

        Preview preview = new Preview(pc);
        preview.setOnPreviewOutputUpdateListener(output -> {
            ViewGroup vg = (ViewGroup) tv.getParent();
            vg.removeView(tv);
            vg.addView(tv, 0);
            tv.setSurfaceTexture(output.getSurfaceTexture());
        });

        @SuppressLint("RestrictedApi") ImageAnalysisConfig iac = new ImageAnalysisConfig
                .Builder()
                .setDefaultResolution(new Size(960, 720))
                .setTargetRotation(rotation)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setLensFacing(lens)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(iac);
        imageAnalysis.setAnalyzer(Runnable::run,
                new FaceTrackingAnalyzer(tv, iv, lens));
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                tv.post(this::startCamera);
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void runTempClient() {
        if (handler == null) {
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    returnTemp(msg);
                }
            };
        }

        tempClient = new TempClient(handler);
        tempClient.start();
    }

    @Override
    public void returnTemp(Message msg) {
        if (msg != null) {
            if (msg.what == 0) {
                Log.d(TAG, "Temp: " + msg.obj);
                Toast.makeText(this, "A temperatura do usuário é de " + msg.obj + "ºC.", Toast.LENGTH_SHORT).show();
            } else Log.e(TAG, "Something happened");
        }else Log.e(TAG, "Message is null");
    }



}