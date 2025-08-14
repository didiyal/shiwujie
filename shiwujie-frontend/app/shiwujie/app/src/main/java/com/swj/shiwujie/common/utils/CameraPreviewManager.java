package com.swj.shiwujie.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.Arrays;

/**
 * 摄像头预览管理器
 * 负责管理后置摄像头的预览功能
 */
public class CameraPreviewManager {
    private static final String TAG = "CameraPreviewManager";
    
    private Context context;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    
    private String cameraId;
    private boolean isPreviewActive = false;
    
    // 拍照相关
    private ImageReader imageReader;
    private boolean isTakingPhoto = false;
    
    // 拍照回调接口
    public interface TakePhotoCallback {
        void onPhotoTaken(byte[] data); // 照片数据回调
        void onError(String error);
    }
    
    public CameraPreviewManager(Context context) {
        this.context = context;
    }
    
    /**
     * 设置预览视图
     */
    public void setPreviewView(TextureView textureView) {
        this.textureView = textureView;
    }
    
    /**
     * 初始化ImageReader用于处理拍照数据
     */
    private void initImageReader() {
        try {
            // 配置图片尺寸和格式（根据需求调整）
            imageReader = ImageReader.newInstance(480, 640, android.graphics.ImageFormat.JPEG, 1);
            Log.d(TAG, "ImageReader初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "ImageReader初始化失败", e);
        }
    }
    
    /**
     * 开始预览
     */
    public void startPreview() {
        if (textureView == null) {
            Log.e(TAG, "TextureView未设置");
            return;
        }
        
        if (!checkCameraPermission()) {
            Log.e(TAG, "没有摄像头权限");
            return;
        }
        
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    openCamera();
                }
                
                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                }
                
                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return false;
                }
                
                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                }
            });
        }
    }
    
    /**
     * 停止预览
     */
    public void stopPreview() {
        closeCamera();
        stopBackgroundThread();
    }
    
    /**
     * 打开摄像头
     */
    private void openCamera() {
        startBackgroundThread();
        
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            // 查找后置摄像头
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            
            if (cameraId == null) {
                Log.e(TAG, "未找到后置摄像头");
                return;
            }
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        createCameraPreview();
                    }
                    
                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        cameraDevice.close();
                    }
                    
                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        cameraDevice.close();
                        cameraDevice = null;
                        Log.e(TAG, "摄像头打开失败: " + error);
                    }
                }, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "访问摄像头失败", e);
        }
    }
    
    /**
     * 创建摄像头预览
     */
    private void createCameraPreview() {
        try {
            // 初始化ImageReader
            initImageReader();
            
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(480, 640);
            
            Surface surface = new Surface(texture);
            
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            
            // 准备预览和拍照的surface列表
            java.util.List<Surface> surfaces = new java.util.ArrayList<>();
            surfaces.add(surface); // 预览用surface
            if (imageReader != null) {
                surfaces.add(imageReader.getSurface()); // 拍照用surface
            }
            
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
                    
                    cameraCaptureSession = session;
                    try {
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                        isPreviewActive = true;
                        Log.d(TAG, "摄像头预览开始");
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "设置预览请求失败", e);
                    }
                }
                
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "配置摄像头会话失败");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "创建摄像头预览失败", e);
        }
    }
    
    /**
     * 关闭摄像头
     */
    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        isPreviewActive = false;
        isTakingPhoto = false;
    }
    
    /**
     * 启动后台线程
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "停止后台线程失败", e);
            }
        }
    }
    
    /**
     * 检查摄像头权限
     */
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 是否正在预览
     */
    public boolean isPreviewActive() {
        return isPreviewActive;
    }
    
    /**
     * 核心方法：拍摄照片
     */
    public void takePhoto(TakePhotoCallback callback) {
        if (isTakingPhoto) {
            return;
        }
        
        if (cameraDevice == null || cameraCaptureSession == null) {
            Log.e(TAG, "相机未准备好");
            if (callback != null) {
                callback.onError("相机未准备好");
            }
            return;
        }
        
        if (imageReader == null) {
            Log.e(TAG, "ImageReader未初始化");
            if (callback != null) {
                callback.onError("ImageReader未初始化");
            }
            return;
        }
        
        try {
            isTakingPhoto = true;
            
            // 创建拍照请求（复用预览的相机设备）
            final CaptureRequest.Builder captureBuilder = 
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            
            // 设置拍照的目标surface
            captureBuilder.addTarget(imageReader.getSurface());
            
            // 设置拍照方向（根据设备旋转调整）
            int rotation = getWindowRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            
            // 设置自动对焦和曝光
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            
            // 构建拍照请求
            CaptureRequest captureRequest = captureBuilder.build();
            
            // 提交拍照请求
            cameraCaptureSession.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                    // 从ImageReader中获取照片数据
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        try {
                            android.media.Image.Plane[] planes = image.getPlanes();
                                                            if (planes.length > 0) {
                                    java.nio.ByteBuffer buffer = planes[0].getBuffer();
                                    byte[] data = new byte[buffer.remaining()];
                                    buffer.get(data);
                                    
                                    if (callback != null) {
                                        callback.onPhotoTaken(data);
                                    }
                                } else {
                                Log.e(TAG, "图片平面数据为空");
                                if (callback != null) {
                                    callback.onError("图片数据为空");
                                }
                            }
                        } finally {
                            image.close();
                        }
                    } else {
                        Log.e(TAG, "无法获取图片数据");
                        if (callback != null) {
                            callback.onError("无法获取图片数据");
                        }
                    }
                    
                    isTakingPhoto = false;
                }
                
                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                         @NonNull CaptureRequest request,
                                         @NonNull android.hardware.camera2.CaptureFailure failure) {
                    Log.e(TAG, "拍照失败: " + failure.getReason());
                    isTakingPhoto = false;
                    if (callback != null) {
                        callback.onError("拍照失败: " + failure.getReason());
                    }
                }
            }, backgroundHandler);
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "拍照失败", e);
            isTakingPhoto = false;
            if (callback != null) {
                callback.onError("拍照失败：" + e.getMessage());
            }
        }
    }
    
    /**
     * 获取窗口旋转角度
     */
    private int getWindowRotation() {
        try {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                return activity.getWindowManager().getDefaultDisplay().getRotation();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取窗口旋转失败", e);
        }
        return android.view.Surface.ROTATION_0;
    }
    
    /**
     * 计算照片方向（根据设备旋转）
     */
    private int getOrientation(int rotation) {
        switch (rotation) {
            case android.view.Surface.ROTATION_0: return 90;
            case android.view.Surface.ROTATION_90: return 0;
            case android.view.Surface.ROTATION_180: return 270;
            case android.view.Surface.ROTATION_270: return 180;
            default: return 90;
        }
    }
    
    /**
     * 是否正在拍照
     */
    public boolean isTakingPhoto() {
        return isTakingPhoto;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopPreview();
    }
}
