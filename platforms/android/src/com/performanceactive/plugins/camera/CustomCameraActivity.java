
package com.performanceactive.plugins.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

public class CustomCameraActivity extends Activity {

    private static final String TAG = CustomCameraActivity.class.getSimpleName();
    private static final float ASPECT_RATIO = 126.0f / 86;

    public static String FILENAME = "Filename";
    public static String QUALITY = "Quality";
    public static String TARGET_WIDTH = "TargetWidth";
    public static String TARGET_HEIGHT = "TargetHeight";
    public static String IMAGE_URI = "ImageUri";
    public static String ERROR_MESSAGE = "ErrorMessage";
    public static int RESULT_ERROR = 2;
    public static String MASKFILE = "Maskfile";

    private Camera camera;
    private RelativeLayout layout;
    private FrameLayout cameraPreviewView;
    private ImageButton captureButton;
    private ImageView mask;
    private double maskLeft,maskTop,maskWidth,maskHeight,maskAspectRatio;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    @Override
    protected void onResume() {
        super.onResume();
        try {
            camera = Camera.open();
            configureCamera();
            displayCameraPreview();
        } catch (Exception e) {
            finishWithError("Camera is not accessible");
        }
    }

    private void configureCamera() {
        Camera.Parameters cameraSettings = camera.getParameters();
        cameraSettings.setJpegQuality(100);
        List<String> supportedFocusModes = cameraSettings.getSupportedFocusModes();
        if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
            cameraSettings.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (supportedFocusModes.contains(FOCUS_MODE_AUTO)) {
            cameraSettings.setFocusMode(FOCUS_MODE_AUTO);
        }
        cameraSettings.setFlashMode(FLASH_MODE_OFF);
        camera.setParameters(cameraSettings);
    }

    private void displayCameraPreview() {
        cameraPreviewView.removeAllViews();
        cameraPreviewView.addView(new CustomCameraPreview(this, camera));
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        layout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(layoutParams);
        createCameraPreview();
        createCaptureButton();
        createMask();
        setContentView(layout);
    }

    private void createCameraPreview() {
        cameraPreviewView = new FrameLayout(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        cameraPreviewView.setLayoutParams(layoutParams);
        layout.addView(cameraPreviewView);
    }

    private void createMask() {
        mask = new ImageView(this);
        String filename = getIntent().getStringExtra(MASKFILE);
        setBitmap(mask, filename);
        mask.setScaleType(ScaleType.FIT_CENTER);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        int screenWidth = screenWidthInPixels();
        int screenHeight = screenHeightInPixels() - dpToPixels(20) - captureButton.getDrawable().getIntrinsicHeight();
        double screenAspectRatio = screenHeight * 1.0 / screenWidth;
        maskAspectRatio = mask.getDrawable().getIntrinsicHeight() * 1.0 / mask.getDrawable().getIntrinsicWidth();
        if (screenAspectRatio > maskAspectRatio) {
            layoutParams.topMargin = (int) (screenHeight / 2 - screenWidth * 1.0 * maskAspectRatio / 2);
            maskLeft = 0.05;
            maskWidth = 1.0;
            maskTop = layoutParams.topMargin*1.0 / screenHeightInPixels();
            maskHeight = screenWidth*1.0*maskAspectRatio/screenHeightInPixels();
        } else {
            layoutParams.topMargin = (int) (screenHeight * 0.05);
            maskLeft=(screenWidth/2-screenHeight*1.0/maskAspectRatio/2)/screenWidthInPixels();
            maskWidth=screenHeight*1.0/maskAspectRatio/screenWidthInPixels();
            maskTop=screenHeight*0.05/screenHeightInPixels();
            maskHeight=screenHeight*1.0/screenHeightInPixels();
        }
        mask.setLayoutParams(layoutParams);
        mask.setMaxWidth((int) (screenWidth * 1.0));
        mask.setMaxHeight((int) (screenHeight * 1.0));
        mask.setAdjustViewBounds(true);
        layout.addView(mask);
    }

    private int screenWidthInPixels() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        return size.x;
    }

    private int screenHeightInPixels() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        return size.y;
    }

    private void createCaptureButton() {
        captureButton = new ImageButton(getApplicationContext());
        setBitmap(captureButton, "capture_button.png");
        captureButton.setBackgroundColor(Color.TRANSPARENT);
        captureButton.setScaleType(ScaleType.FIT_CENTER);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(dpToPixels(75), dpToPixels(75));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.bottomMargin = dpToPixels(10);
        captureButton.setLayoutParams(layoutParams);
        captureButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                setCaptureButtonImageForEvent(event);
                return false;
            }
        });
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureWithAutoFocus();
            }
        });
        layout.addView(captureButton);
    }

    private void setCaptureButtonImageForEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setBitmap(captureButton, "capture_button_pressed.png");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            setBitmap(captureButton, "capture_button.png");
        }
    }

    private void takePictureWithAutoFocus() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            camera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    takePicture();
                }
            });
        } else {
            takePicture();
        }
    }

    private void takePicture() {
        try {
            camera.takePicture(null, null, new PictureCallback() {
                @Override
                public void onPictureTaken(byte[] jpegData, Camera camera) {
                    new OutputCapturedImageTask().execute(jpegData);
                }
            });
        } catch (Exception e) {
            finishWithError("Failed to take image");
        }
    }

    private class OutputCapturedImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... jpegData) {
            try {
                String filename = getIntent().getStringExtra(FILENAME);
                int quality = getIntent().getIntExtra(QUALITY, 80);
                File capturedImageFile = new File(getCacheDir(), filename);
                Bitmap capturedImage = getScaledBitmap(jpegData[0]);
                capturedImage = correctCaptureImageOrientation(capturedImage);
                int top=(int)(capturedImage.getHeight()*maskTop);
                int height=(int)(capturedImage.getHeight()*maskHeight);
                int width=(int)(height*1.0/maskAspectRatio);
                int left=capturedImage.getWidth()/2-width/2;
                capturedImage=Bitmap.createBitmap(capturedImage,left,top,width,height);
                capturedImage.compress(CompressFormat.JPEG, quality, new FileOutputStream(capturedImageFile));
                Intent data = new Intent();
                data.putExtra(IMAGE_URI, Uri.fromFile(capturedImageFile).toString());
                setResult(RESULT_OK, data);
                finish();
            } catch (Exception e) {
                finishWithError("Failed to save image");
            }
            return null;
        }

    }

    private Bitmap getScaledBitmap(byte[] jpegData) {
        int targetWidth = getIntent().getIntExtra(TARGET_WIDTH, -1);
        int targetHeight = getIntent().getIntExtra(TARGET_HEIGHT, -1);
        if (targetWidth <= 0 && targetHeight <= 0) {
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        }

        // get dimensions of image without scaling
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);

        // decode image as close to requested scale as possible
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);

        // set missing width/height based on aspect ratio
        float aspectRatio = ((float)options.outHeight) / options.outWidth;
        if (targetWidth > 0 && targetHeight <= 0) {
            targetHeight = Math.round(targetWidth * aspectRatio);
        } else if (targetWidth <= 0 && targetHeight > 0) {
            targetWidth = Math.round(targetHeight / aspectRatio);
        }

        // make sure we also
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int requestedWidth, int requestedHeight) {
        int originalHeight = options.outHeight;
        int originalWidth = options.outWidth;
        int inSampleSize = 1;
        if (originalHeight > requestedHeight || originalWidth > requestedWidth) {
            int halfHeight = originalHeight / 2;
            int halfWidth = originalWidth / 2;
            while ((halfHeight / inSampleSize) > requestedHeight && (halfWidth / inSampleSize) > requestedWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap correctCaptureImageOrientation(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void finishWithError(String message) {
        Intent data = new Intent().putExtra(ERROR_MESSAGE, message);
        setResult(RESULT_ERROR, data);
        finish();
    }

    private int dpToPixels(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean isXLargeScreen() {
        int screenLayout = getResources().getConfiguration().screenLayout;
        return (screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private boolean isLargeScreen() {
        int screenLayout = getResources().getConfiguration().screenLayout;
        return (screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private void setBitmap(ImageView imageView, String imageName) {
        try {
            InputStream imageStream = getAssets().open("www/img/cameraoverlay/" + imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            imageView.setImageBitmap(bitmap);
            imageStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Could load image", e);
        }
    }

}
