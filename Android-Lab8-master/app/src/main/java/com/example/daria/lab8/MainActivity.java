package com.example.daria.lab8;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.hardware.Camera.getCameraInfo;
import static android.hardware.Camera.open;

public class MainActivity extends Activity {

    int CAMERA_ID = 0;
    final boolean FULL_SCREEN = true;
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Camera camera;
    private MediaRecorder mediaRecorder;
    File videoFile;
    File publicDirectory;
    DateFormat df;

    @SuppressLint("SimpleDateFormat")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        df = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss");
        surfaceView = findViewById(R.id.surfaceView);
        Switch switchCamera = findViewById(R.id.switch2);
        if (switchCamera != null) {
            switchCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        CAMERA_ID = 0;
                    } else {
                        CAMERA_ID = 1;
                    }
                    changeCamera(holder);
                }
            });
        }

        holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    setCameraDisplayOrientation(CAMERA_ID);
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                changeCamera(holder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });

    }

    private void changeCamera(SurfaceHolder holder) {
        camera.release();
        camera = open(CAMERA_ID);
        setPreviewSize(FULL_SCREEN);
        setCameraDisplayOrientation(CAMERA_ID);
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = open(CAMERA_ID);
        setPreviewSize(FULL_SCREEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        if (camera != null)
            camera.release();
        camera = null;
    }

    void setPreviewSize(boolean fullScreen) {

        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            //matrix.invert(matrix);
        }
        // преобразование
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        surfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        surfaceView.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;
        CameraInfo info = new CameraInfo();
        getCameraInfo(cameraId, info);

        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }

    public void onClickStartRecord(View view) {
        if (prepareVideoRecorder()) {
            camera.stopPreview();
            mediaRecorder.start();
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Запись началась", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            releaseMediaRecorder();
        }
    }

    public void onClickStopRecord(View view) {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Запись закончилась", Toast.LENGTH_SHORT);
            toast.show();
            releaseMediaRecorder();
            camera.startPreview();
        }
    }

    private boolean prepareVideoRecorder() {

        String fileName = "lab8_video_" + df.format(Calendar.getInstance().getTime()) + ".mp4";
        videoFile = new File(publicDirectory, fileName);
        camera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setOrientationHint(0);
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }


}