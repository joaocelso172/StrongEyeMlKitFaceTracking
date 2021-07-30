package com.example.googlemlkitdemo.FaceTracking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.googlemlkitdemo.DAL.UserDAO;
import com.example.googlemlkitdemo.Model.UserRequest;
import com.example.googlemlkitdemo.Model.UserResponse;
import com.example.googlemlkitdemo.Util.Base64Converter;
import com.example.googlemlkitdemo.sdk.LightSensor;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.google.android.gms.vision.face.FaceDetector.ACCURATE_MODE;

public class FaceTrackingAnalyzer extends AppCompatActivity implements ImageAnalysis.Analyzer {
    private static final String TAG = "MLKitFacesAnalyzer";
    private TextureView tv;
    private ImageView iv;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint linePaint;
    private float widthScaleFactor = 1.0f;
    private float heightScaleFactor = 1.0f;
    private FirebaseVisionImage fbImage;
    private CameraX.LensFacing lens;
    private UserDAO userDAO;
    private Thread threadPostUser = null;
    private boolean inProcess = false;
    private long startTime = 0;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean countingTime = false;
    private boolean isLightOpen = false;


    FaceTrackingAnalyzer(TextureView tv, ImageView iv, CameraX.LensFacing lens) {
        this.tv = tv;
        this.iv = iv;
        this.lens = lens;
    }

    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        if (image == null || image.getImage() == null) {
            return;
        }

        int rotation = degreesToFirebaseRotation(rotationDegrees);
        fbImage = FirebaseVisionImage.fromMediaImage(image.getImage(), rotation);
        initDrawingUtils();
        initDetector();

    }

    private void initDetector() {
        FirebaseVisionFaceDetectorOptions detectorOptions = new FirebaseVisionFaceDetectorOptions
                .Builder()
                .setPerformanceMode(ACCURATE_MODE)
                .enableTracking()
                .build();
        FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(detectorOptions);
        faceDetector.detectInImage(fbImage).addOnSuccessListener(firebaseVisionFaces -> {
            if (!firebaseVisionFaces.isEmpty()) {
                processFaces(firebaseVisionFaces);
                timerForLight(5);
            } else {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
            }
        }).addOnFailureListener(e -> Log.i(TAG, "Falha: " + e.toString()));

    }

    private void initDrawingUtils() {
        bitmap = Bitmap.createBitmap(tv.getWidth(), tv.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        linePaint = new Paint();
        linePaint.setColor(Color.BLUE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setTextSize(40);
        widthScaleFactor = canvas.getWidth() / (fbImage.getBitmap().getWidth() * 1.0f);
        heightScaleFactor = canvas.getHeight() / (fbImage.getBitmap().getHeight() * 1.0f);
    }

    @Override
    protected void onPause() {
        timerHandler.postDelayed(timerRunnable, 0);
        super.onPause();
    }

    private void processFaces(List<FirebaseVisionFace> faces) {
        for (FirebaseVisionFace face : faces) {

            Rect box = new Rect((int) translateX(face.getBoundingBox().left),
                    (int) translateY(face.getBoundingBox().top),
                    (int) translateX(face.getBoundingBox().right),
                    (int) translateY(face.getBoundingBox().bottom));
            canvas.drawText(String.valueOf(face.getTrackingId()),
                    translateX(face.getBoundingBox().centerX()),
                    translateY(face.getBoundingBox().centerY()),
                    linePaint);

            canvas.drawRect(box, linePaint);
        }

        Log.i(TAG, "Foram detectados " + faces.size() + " rostos.");

        getUserAccess();

        iv.setImageBitmap(bitmap);
    }

    private void timerForLight(int maxSec) {
        if (!countingTime) {
            countingTime = true;
            //runs without a timer by reposting this handler at the end of the runnable
            timerHandler = new Handler();
            timerRunnable = new Runnable() {

                @Override
                public void run() {
                    long millis = System.currentTimeMillis() - startTime;
                    int seconds = (int) (millis / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    Log.d(TAG, "Timer: " + String.format("%d:%02d", minutes, seconds));
                    timerListener(seconds, maxSec);
                    timerHandler.postDelayed(this, 1000);
                }
            };

            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private void timerListener(int sec, int maxSec) {
        if (sec >= maxSec && isLightOpen) {
            LightSensor.closeLight();
            isLightOpen = false;
            Log.d(TAG, "Turning off light");
            timerHandler.removeCallbacks(timerRunnable);
            countingTime = false;
        }else if (sec <= maxSec && !isLightOpen){
            isLightOpen = LightSensor.openLight();
            Log.d(TAG, "Turning on light");
        }
    }


    private float translateY(float y) {
        return y * heightScaleFactor;
    }

    private float translateX(float x) {
        float scaledX = x * widthScaleFactor;
        if (lens == CameraX.LensFacing.FRONT) {
            return canvas.getWidth() - scaledX;
        } else {
            return scaledX;
        }
    }

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException("Rotation must be 0, 90, 180, or 270.");
        }
    }


    private UserResponse sendUsersFromApi() {
        inProcess = true;
        Bitmap bmFace = fbImage.getBitmap();
        String base64Img = Base64Converter.bitmapToBase64(bmFace);

        UserRequest user = new UserRequest(base64Img);

        userDAO = new UserDAO();

        UserResponse userResponse = userDAO.sendUser(user);

        if (userResponse != null) {
            if (userResponse.getCanAccess()) {
            }
        } else Log.e(TAG, "Não foram encontrados registros de usuários");

        inProcess = false;
        return userResponse;
    }

    private void getUserAccess() {
        if (!inProcess) {
            threadPostUser = (new Thread() {
                @Override
                public void run() {
                    UserResponse user = sendUsersFromApi();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (user == null) {
                                Log.d(TAG, "Usuário nulo");
                            } else Log.d(TAG, "Usuário encontrado");

                        }
                    });
                }
            });

            threadPostUser.start();
        }
    }
}