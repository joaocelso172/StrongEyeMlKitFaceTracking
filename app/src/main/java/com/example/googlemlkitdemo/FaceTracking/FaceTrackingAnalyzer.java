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
import android.speech.tts.TextToSpeech;
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
import java.util.Locale;

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
    private long startTime = 0, waitTime = 0;
    private Handler flashTimerHandler, userLayoutInfoHandler;
    private Runnable flashTimerRunnable, userLayoutInfoRunnable;
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
    private TextToSpeech textToSpeech;

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

        setRunnables(5, 30);

        configGoogleSpitch();
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
                lightTimer();
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


        iv.setImageBitmap(bitmap);
    }

    private void lightTimer() {
        if (!countingTime) {
            countingTime = true;
            startTime = System.currentTimeMillis();
            flashTimerHandler.postDelayed(flashTimerRunnable, 0);
        }
    }

    private void timerListener(int sec, int maxSec) {
        if (sec >= maxSec && isLightOpen) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LightSensor.closeLight();
                    isLightOpen = false;
                    Log.d(TAG, "Turning off light");
                    flashTimerHandler.removeCallbacks(flashTimerRunnable);
                    countingTime = false;
                }
            });
        } else if (sec <= maxSec && !isLightOpen){
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

    private void getUserAccess(double temp) {
        if (!inProcess) {
            threadPostUser = (new Thread() {
                @Override
                public void run() {
                    Bitmap bmFace = fbImage.getBitmap();
                    String base64Img = Base64Converter.bitmapToBase64(bmFace);

                    UserRequest userRequest = new UserRequest(base64Img, temp, c);
                    UserResponse user = sendUsersFromApi(userRequest);
                    reqs++;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String resultSpeekedText;
                            if (user != null && user.getNationalInsuranceNumber() != null) {
                                if (temp >= 38.0) {
                                    resultSpeekedText = getTempLevel(temp) + " Não é permitido entrada com esta temperatura.";
                                    setStandardInfo();
                                } else {
                                    resultSpeekedText = "Bem vindo de volta, usuário!" + getTempLevel(temp);
                                    openDoor();
                                }

                                //Toast.makeText(c, "IMEI: " + userRequest.getIdTerminal(), Toast.LENGTH_SHORT).show();

                                setUserInfo(user, temp);
                                switchInfoLayout();
                            } else if (user != null && user.getNationalInsuranceNumber() == null) {
                                setStandardInfo();
                                resultSpeekedText = "O acesso a este local não foi aprovado.";
                                //Toast.makeText(c, "O usuário não possui acesso a este local.", Toast.LENGTH_SHORT).show();
                            }else {
                                setStandardInfo();
                                resultSpeekedText = "Por favor, mantenha-se parado.";
                                //Toast.makeText(c, "Não foi identificado um rosto na imagem.", Toast.LENGTH_SHORT).show();
                            }

                            speechText(resultSpeekedText);
                            threadPostUser.interrupt();
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
                double temp = (double) msg.obj;
                if (temp > 0) getUserAccess(temp);
            } else Log.e(TAG, "Something happened");
        }else Log.e(TAG, "Message is null");
    }

    private void setUserInfo(UserResponse userResponse, double temp){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingRecogLayout.setVisibility(View.GONE);
                cRecogInfo.setVisibility(View.GONE);
                lUserInfo.setVisibility(View.VISIBLE);
                imgUserInfo.setImageBitmap(foundedFaceImage.getBitmap());
                txtIdUser.setText("CPF do usuário: " + userResponse.getNationalInsuranceNumber());
                txtTempUser.setText("Temperatura corporal: " + temp);

            }
        });


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
                userLayoutInfoHandler.removeCallbacks(userLayoutInfoRunnable);
            }
        });
    }

    private void setRunnables (int secToReturnLayout, int maxSec){
        //runs without a timer by reposting this handler at the end of the runnable
        flashTimerHandler = new Handler();
        flashTimerRunnable = new Runnable() {

            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                Log.d(TAG, "Flash Timer: " + String.format("%d:%02d", minutes, seconds));
                timerListener(seconds, maxSec);
                flashTimerHandler.postDelayed(this, 1000);
            }
        };

        userLayoutInfoHandler = new Handler();
        userLayoutInfoRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - waitTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                Log.d(TAG, "Wait Timer: " + String.format("%d:%02d", minutes, seconds));
                userLayoutInfoHandler.postDelayed(this, 1000);

                if (seconds >= secToReturnLayout) {
                    setStandardInfo();
                }
            }
        };
    }

    private void switchInfoLayout(){
        waitTime = System.currentTimeMillis();
        userLayoutInfoHandler.postDelayed(userLayoutInfoRunnable, 0);

    }

    private void configGoogleSpitch(){
        textToSpeech = new TextToSpeech(c, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
               if (i == TextToSpeech.SUCCESS){
                   textToSpeech.setLanguage(Locale.getDefault());

                   textToSpeech.setPitch(1.15f);
                   textToSpeech.setSpeechRate(1.15f);

               }
            }
        });

    }

    private void speechText(String text){
        CharSequence speekedText = text;

        int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "teste");

        Log.d(TAG, "TTS: " + result);
    }

    private String getTempLevel(double temp){
        if (temp >= 38) return "Temperatura alta.";
        else return "Temperatura normal.";
    }

}