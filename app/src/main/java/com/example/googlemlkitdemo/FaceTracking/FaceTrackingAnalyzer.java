package com.example.googlemlkitdemo.FaceTracking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.googlemlkitdemo.DAL.UserDAO;
import com.example.googlemlkitdemo.Model.UserRequest;
import com.example.googlemlkitdemo.Model.UserResponse;
import com.example.googlemlkitdemo.Util.Base64Converter;
import com.example.googlemlkitdemo.sdk.DoorControl;
import com.example.googlemlkitdemo.sdk.ITempClient;
import com.example.googlemlkitdemo.sdk.LightSensor;
import com.example.googlemlkitdemo.sdk.TempClient;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.truesen.face.sdk.NativeVisionApi;

import java.util.List;

import static com.google.android.gms.vision.face.FaceDetector.ACCURATE_MODE;

public class FaceTrackingAnalyzer extends AppCompatActivity implements ImageAnalysis.Analyzer, ITempClient {
    private static final String TAG = "MLKitFacesAnalyzer";
    private TextureView tv;
    private TextView txtIdUser, txtTempUser;
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
    private Handler handler;
    private TempClient tempClient;
    private Context c;
    private LinearLayout lUserInfo;
    private LinearLayout loadingRecogLayout;
    private ConstraintLayout cRecogInfo;
    private ImageView imgUserInfo;
    private FirebaseVisionImage foundedFaceImage;
    private int reqs = 0, lastID = 9999999, contReq = 0;

    FaceTrackingAnalyzer(TextureView tv, ImageView iv, TextView txtIdUser, TextView txtTempUser, ImageView imgUserInfo, CameraX.LensFacing lens, Context c, ConstraintLayout cRecogInfo, LinearLayout lUserInfo, LinearLayout loadingRecogLayout) {
        this.tv = tv;
        this.iv = iv;
        this.txtIdUser = txtIdUser;
        this.txtTempUser = txtTempUser;
        this.lens = lens;
        this.c = c;
        this.lUserInfo = lUserInfo;
        this.cRecogInfo = cRecogInfo;
        this.loadingRecogLayout = loadingRecogLayout;
        this.imgUserInfo = imgUserInfo;
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
                foundedFaceImage = fbImage;
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

    private void openDoor() {
        NativeVisionApi.setGpioDirection(124, 1);
    }

    private void closeDoor() {
        NativeVisionApi.setGpioDirection(124, 0);
    }

    private void processFaces(List<FirebaseVisionFace> faces) {
        contReq = 0;
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

            if (face.getTrackingId() != lastID){
                if (contReq < 1) {
                    contReq++;
                    //runTempClient();
                    getUserAccess(36);
                }
            }

            lastID = face.getTrackingId();
        }

        Log.i(TAG, "Foram detectados " + faces.size() + " rostos.");

        //runTempClient();
        //getUserAccess(36);

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

    private UserResponse sendUsersFromApi(UserRequest user) {
        inProcess = true;
        boolean successful;
        userDAO = new UserDAO();

        userDAO.sendUser(user);

        do {
            setLoadingInfo();
            successful = userDAO.getStatus();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (successful != true);

        inProcess = false;

        return userDAO.getUserResponse();
    }

    private void setUserInfo(UserResponse userResponse, double temp){

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingRecogLayout.setVisibility(View.GONE);
                    cRecogInfo.setVisibility(View.GONE);
                    lUserInfo.setVisibility(View.VISIBLE);
                    imgUserInfo.setImageBitmap(foundedFaceImage.getBitmap());
                    txtIdUser.setText("CPF do usuário: " + userResponse.getUserID());
                    txtTempUser.setText("Temperatura corporal: " + temp);
                }
            });


    }

    private void getUserAccess(double temp) {
        if (!inProcess) {
            threadPostUser = (new Thread() {
                @Override
                public void run() {
                    Bitmap bmFace = fbImage.getBitmap();
                    String base64Img = Base64Converter.bitmapToBase64(bmFace);

                    UserRequest userRequest = new UserRequest(base64Img, temp);
                    UserResponse user = sendUsersFromApi(userRequest);
                    reqs++;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (user != null && user.getUserID() != null) {
                                setUserInfo(user, temp);
                                //Toast.makeText(c, "Usuário encontrado! CPF: " + user.getUserID() + ".", Toast.LENGTH_SHORT).show();
                                openDoor();
                            } else if (user != null && user.getUserID() == null) {
                                Toast.makeText(c, "O usuário não possui acesso a este local.", Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(c, "Não foi identificado um rosto na imagem.", Toast.LENGTH_SHORT).show();
                            }

                            setStandardInfo();
                        }
                    });
                }
            });

            threadPostUser.start();
        }
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
                //Toast.makeText(c, "A temperatura do usuário é de " + msg.obj + "ºC.", Toast.LENGTH_SHORT).show();
                double temp = (double) msg.obj;
                if (temp > 0) getUserAccess(temp);
            } else Log.e(TAG, "Something happened");
        }else Log.e(TAG, "Message is null");
    }

    private void setLoadingInfo() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingRecogLayout.setVisibility(View.VISIBLE);
                cRecogInfo.setVisibility(View.VISIBLE);
                lUserInfo.setVisibility(View.GONE);
            }
        });
    }

    private void setStandardInfo(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(c, "Requisições feitas até o momento: " + reqs, Toast.LENGTH_SHORT).show();
                loadingRecogLayout.setVisibility(View.GONE);
                lUserInfo.setVisibility(View.GONE);
                cRecogInfo.setVisibility(View.VISIBLE);
            }
        });
    }

    private void timer(){

    }
}