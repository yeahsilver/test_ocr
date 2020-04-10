package com.example.test_ocr;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

import static java.util.Arrays.*;

public class Preview extends Thread {
    private final static String TAG = "Preview: ";
    private Size previewSize;
    private Context context;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    private TextureView textureView;

    public Preview(Context previewContext, TextureView previewTextureView){
        context = previewContext;
        textureView = previewTextureView;
    }

    private String getBackFacingCameraId(CameraManager manager){
        try{
            for(final String cameraId: manager.getCameraIdList()){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(orientation == CameraCharacteristics.LENS_FACING_BACK)
                    return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void openCamera(){
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG,"openCamera E");
        try {
            String cameraId = getBackFacingCameraId(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            int permissionCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
            if(permissionCamera == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA}, CameraView.REQUEST_CAMERA);
            } else {
                manager.openCamera(cameraId, stateCallback, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener(){

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG,"onSurfaceTextureAvailable, width: "+width+" height: "+height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.e(TAG,"onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(TAG, "onPause");
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.e(TAG,"onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG,"onError");
        }
    };

    protected void startPreview(){
        if(cameraDevice == null || textureView.isAvailable() || previewSize == null){
            Log.e(TAG, "startPreview fail, return");
        }

        SurfaceTexture texture = textureView.getSurfaceTexture();
        if(texture == null){
            Log.e(TAG,"texture is null, return ");
            return;
        }

        texture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
        Surface surface = new Surface(texture);

        try{
            previewBuilder = cameraDevice.createCaptureRequest(cameraDevice.TEMPLATE_PREVIEW);
        } catch(CameraAccessException e){
            e.printStackTrace();
        }
        previewBuilder.addTarget(surface);

        try{
            cameraDevice.createCaptureSession(asList(surface),new CameraCaptureSession.StateCallback(){

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    previewSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(context,"onConfigureFailed", Toast.LENGTH_LONG).show();
                }
            },null);
        } catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    protected void updatePreview(){
        if(cameraDevice == null){
            Log.e(TAG,"updatePreview error, return");
        }
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("cameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try{
            previewSession.setRepeatingRequest(previewBuilder.build(),null,backgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    public void setSurfaceTextureListener(){
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        
    }

    public void onResume(){
        Log.d(TAG,"onResume");
        setSurfaceTextureListener();
    }

    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    public void onPause(){
        Log.d(TAG,"onPause");
        try{
            cameraOpenCloseLock.acquire();
            if(cameraDevice!=null){
                cameraDevice.close();
                cameraDevice = null;
                Log.d(TAG,"CameraDevice close");
            }
        }catch(InterruptedException e){
            throw new RuntimeException("Interrupted while trying to lock camera closing");
        } finally{
            cameraOpenCloseLock.release();
        }
    }
}
