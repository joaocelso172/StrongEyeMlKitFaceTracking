package com.example.googlemlkitdemo.FaceTracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlemlkitdemo.Model.Company;
import com.example.googlemlkitdemo.R;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.googlemlkitdemo.Model.Company.COMPANY_NAME;

public class FaceTrackingActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PERMISSION = 101;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private TextureView tv;
    private ImageView iv, imgCompany;
    private TextView txtCompanyName, txtClock, txtDate, txtUserID, txtUserTemp;
    private LinearLayout linearRecogInfo;
    private LinearLayout loadingRecogLayout;
    private ConstraintLayout constraintsAppInfo;
    private ImageView imgInfo;
    private static final String TAG = "FaceTrackingActivity";

    public static CameraX.LensFacing lens = CameraX.LensFacing.FRONT;

    private int rotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_face_tracking);
        tv = findViewById(R.id.tracking_texture_view);
        iv = findViewById(R.id.tracking_image_view);
        txtCompanyName = findViewById(R.id.txt_company_name);
        txtClock = findViewById(R.id.txt_clock);
        txtDate = findViewById(R.id.txt_date);
        txtUserID = findViewById(R.id.txt_user_info);
        txtUserTemp = findViewById(R.id.txt_user_temp);
        imgCompany = findViewById(R.id.img_company);
        imgInfo = findViewById(R.id.img_user_cam);
        linearRecogInfo = findViewById(R.id.linear_recog_info);
        loadingRecogLayout = findViewById(R.id.linear_loading_recog);
        constraintsAppInfo = findViewById(R.id.constraints_app_info);

        if (allPermissionsGranted()) {
            tv.post(this::startCamera);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        }

        setElementsText();
        enableOrientantionListener();

        System.out.println("Inicializing real time face detection activity");
    }

    private void setElementsText(){
        txtCompanyName.setText(COMPANY_NAME);
        txtCompanyName.setTextSize(25);
        txtClock.setTextSize(25);
        imgCompany.setImageResource(R.drawable.eye);
        enableTimerListener();
        constraintsAppInfo.setVisibility(View.VISIBLE);
    }

    private void enableTimerListener(){

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                SimpleDateFormat hourFormatDate = new SimpleDateFormat("HH:mm:ss");
                SimpleDateFormat weeklyFormatDate = new SimpleDateFormat("EEEE");
                SimpleDateFormat monthlyFormatDate = new SimpleDateFormat("dd/MM/yyyy");
                Date date = new Date(currentTime);
                String time = hourFormatDate.format(date);
                String weeklyDate = weeklyFormatDate.format(date);
                String monthlyDate = monthlyFormatDate.format(date);
                txtClock.setText(time);
                txtDate.setText(weeklyDate + ", " + monthlyDate);
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
                new FaceTrackingAnalyzer(tv, iv, txtUserID, txtUserTemp, imgInfo, lens, this, constraintsAppInfo, linearRecogInfo, loadingRecogLayout));
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




}