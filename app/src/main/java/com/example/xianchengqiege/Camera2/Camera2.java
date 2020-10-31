package com.example.xianchengqiege.Camera2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;

import com.example.xianchengqiege.R;
import com.example.xianchengqiege.Util.BitmapUtil;
import com.example.xianchengqiege.Util.ImageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Camera2 extends BaseCameraProvider {


    private Activity mContext;
    private TextureView mTextureView;
    String mCameraId;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private Size mPreviewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private ImageReader mImageReader;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private ImageView imageView;
    public Bitmap newBitmap;
    private CameraCharacteristics characteristics;


    public Camera2(Activity mContext) {
        this.mContext = mContext;
        HandlerThread handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
        imageView = mContext.findViewById(R.id.iv_pic_back);
        initTexture((TextureView) mContext.findViewById(R.id.texture_view_back));

    }


    public void initTexture(TextureView textureView) {
        mTextureView = textureView;
        startCameraThread();
        if (!mTextureView.isAvailable()) {
            mTextureView.setSurfaceTextureListener(mTextureListener);
        } else {
            startPreview();
        }
    }


    private void startCameraThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

    }


    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            setupCamera(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {
                characteristics = manager.getCameraCharacteristics(cameraId);
                //默认打开后置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;



                float yourMaxFocus = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE);
                Log.d("FFFFFFFFFFFFFFFFFFFFF", String.valueOf(yourMaxFocus));


                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera("1", mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };


    private void startPreview() {
        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(mSurfaceTexture);
        setupImageReader();
        Surface imageReaderSurface = mImageReader.getSurface();



        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, 0);
            mCaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) 10000);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, 0);

//            float yourMaxFocus = mCaptureRequestBuilder.get(CaptureRequest.LENS_INFO_HYPERFOCAL_DISTANCE);

            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.25f);
            mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, 2.5f);

            double t=mCaptureRequestBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE);
            double tt=mCaptureRequestBuilder.get(CaptureRequest.LENS_FOCAL_LENGTH);
            Log.d("FFFFFFFFFFFFFFFFFFFFF", String.valueOf(t));
            Log.d("FFFFFFFFFFFFFFFFFFFFF", String.valueOf(tt));

            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(imageReaderSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(imageReaderSurface, previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setupImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据，本例的2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.YUV_420_888, 2);
        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        try {

            float yourMaxFocus = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE);
            Log.d("MMMMMMMMMMMMMMMMM", String.valueOf(yourMaxFocus));

        } catch (Exception e) {
            e.printStackTrace();
        }

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                try {
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();
                    byte[] data68 = ImageUtil.getBytesFromImageAsType(image, 2);
                    int rgb[] = ImageUtil.decodeYUV420SP(data68, imageWidth, imageHeight);
                    final Bitmap bitmap2 = Bitmap.createBitmap(rgb, 0, imageWidth, imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
                    newBitmap = BitmapUtil.rotateBitmap(bitmap2, 180);  //旋转180并镜像
//                mContext.runOnUiThread(new Runnable() {
//                    //更新UI
//                    @Override
//                    public void run() {
//                        // TODO Auto-generated method stub
//                        imageView.setImageBitmap(newBitmap); //UI更改操作
//                    }
//                });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    image.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, null);
    }

    public float getFocus()
    {
//        return characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        return mCaptureRequestBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE);
    }
}


