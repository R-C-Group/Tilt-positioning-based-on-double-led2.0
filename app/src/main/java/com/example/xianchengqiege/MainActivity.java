package com.example.xianchengqiege;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.xianchengqiege.Camera2.Camera2;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {


    Camera2 camera2;

    Timer timer = new Timer();

    EditText editText1;
    EditText editText2;
    EditText editText3;
    MapLayout mapLayout;
    EditText d1,d2,d3;
    private TextView oldOne = null;//两种不同的方法获得方向数据
    private TextView nowOne = null;//两种不同的方法获得方向数据


    //书写数组的模板
    //private float[] accelValues = new float[3], compassValues = new float[3], orientValues = new float[3], rotVecValues = null;
    //private double[] a=new double[3];


    int x;
    int y;
    int z;
    TextView t1;
    Photo_process photo_processing;
    SensorManager sensorManager;

    private int PICK_IMAGE_REQUEST = 1;
    private Bitmap selectbp;
    private double max_size = 1024;
    String nx;
    String ny;
    String nz;




//    private ImageView picture,imageView;//(视频帧数据预览)
    public ImageView picture,imageView;//(视频帧数据预览)
    public ImageView picture1,imageView1;//(视频帧数据预览)
    private TextureView textureView;//摄像头预览

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,//写权限
            Manifest.permission.CAMERA//照相权限
    };


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);









        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            //检查权限
            int i = ContextCompat.checkSelfPermission(this,PERMISSIONS_STORAGE[0]);
            //如果权限申请失败，则重新申请权限
            if(i!= PackageManager.PERMISSION_GRANTED){
                //重新申请权限函数
                startRequestPermission();
                Log.e("这里","权限请求成功");
            }
        }


        oldOne = (TextView) findViewById(R.id.orientation);//推荐的方式
        //角度传感器
        //nowOne = (TextView) findViewById(R.id.preferred);//推荐的方式

        editText1 = findViewById(R.id.x);
        editText2 = findViewById(R.id.y);
        editText3 = findViewById(R.id.z);


        mapLayout = findViewById(R.id.mapLayout);
        imageView=findViewById(R.id.iv_pic_back);
//        d1=findViewById(R.id.d1);
//        d2=findViewById(R.id.d2);
//        d3=findViewById(R.id.d3);




        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mag_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, mag_sensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, acc_sensor, SensorManager.SENSOR_DELAY_UI);


        picture =(ImageView) findViewById(R.id.iv_pic_back);
        picture.setRotation(90);//设置角度
        textureView=(TextureView) findViewById(R.id.texture_view_back);
        textureView.setRotation(0); // // 设置预览角度，并不改变获取到的原始数据方向(与Camera.setDisplayOrientation(0)


        if (!OpenCVLoader.initDebug()) {
            Log.e("TAG", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.e("TAG", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }


        camera2=new Camera2(MainActivity.this);;

        timer.schedule(new TimerTask()

        {

            @Override
            public void run()

            {
//

                float focus=camera2.getFocus();
                Log.d("11111111111", String.valueOf(focus));
                selectbp=camera2.newBitmap;

                photo_processing=new Photo_process(selectbp,prefValues);//把角度数组传过去
                x= (int) photo_processing.nx;
                y= (int) photo_processing.ny;
                z= (int) photo_processing.nz;
                nx= String.valueOf(x);
                ny= String.valueOf(y);
                nz= String.valueOf(z);



                runOnUiThread(new Runnable() {
                    //更新UI
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        imageView.setImageBitmap(photo_processing.selectbp); //UI更改操作
                        editText1.setText(nx);
                        editText2.setText(ny);
                        editText3.setText(nz);
                        mapLayout.changeIcon(x, y, z);
//                        d1.setText(String.valueOf(photo_processing.d1));
//                        d2.setText(String.valueOf(photo_processing.d2));
//                        d3.setText(String.valueOf(photo_processing.d3));
                    }
                });

            }

        },1500,500);

    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("TAG", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Log.d("image-tag", "start to decode selected image now...");
                InputStream input = getContentResolver().openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                int raw_width = options.outWidth;
                int raw_height = options.outHeight;
                int max = Math.max(raw_width, raw_height);
                int newWidth = raw_width;
                int newHeight = raw_height;
                int inSampleSize = 1;
                if(max > max_size) {
                    newWidth = raw_width / 2;
                    newHeight = raw_height / 2;
                    while((newWidth/inSampleSize) > max_size || (newHeight/inSampleSize) > max_size) {
                        inSampleSize *=2;
                    }
                }

                options.inSampleSize = inSampleSize;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                selectbp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void startRequestPermission(){
        //321为请求码
        ActivityCompat.requestPermissions(this,PERMISSIONS_STORAGE,321);
    }


    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"选择图像..."), PICK_IMAGE_REQUEST);
        //调用应用之外的ACTIVITY
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }








    private boolean ready = false; //检查是否同时具有加速度传感器和磁场传感器
    private float[] inR = new float[9], outR = new float[9];
    private float[] inclineMatrix = new float[9];
    private float[] prefValues = new float[3];
    private double mInclination;
    private int count = 1;
    private float[] rotvecR = new float[9], rotQ = new float[4];
    private float[] rotvecOrientValues = new float[3];
    private int mRotation;
    private float[] accelValues = new float[3], compassValues = new float[3], orientValues = new float[3], rotVecValues = null;





    private SensorEventListener sensorEventListener = new SensorEventListener() {

        float[] acceValues = new float[3];
        float[] magnValues = new float[3];
        private float lastRoateDegree;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                acceValues = event.values.clone();
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magnValues = event.values.clone();
            }
            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, acceValues, magnValues);
            SensorManager.getOrientation(R, values);
            float rotateDeg = (float) Math.toDegrees(values[0]);
            if (Math.abs(rotateDeg - lastRoateDegree) > 1) {
                RotateAnimation animation = new RotateAnimation(lastRoateDegree, rotateDeg, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                animation.setFillAfter(true);
                lastRoateDegree = rotateDeg;
                try {
                    mapLayout.icon.setRotation(lastRoateDegree);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            switch (event.sensor.getType()) {//获取传感器的类型
                case Sensor.TYPE_ACCELEROMETER://当时加速度传感器时
                    for (int i = 0; i < 3; i++) {
                        accelValues[i] = event.values[i];//将三个值分别放于accelValues中
                    }
                    if (compassValues[0] != 0) //即accelerator和magnetic传感器都有数值
                        ready = true;//此时检测同时具有加速度传感器与地磁传感器
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD://获取地磁传感器的值
                    for (int i = 0; i < 3; i++) {
                        compassValues[i] = event.values[i];//将三个值分别放于compassValues中
                    }
                    if (accelValues[2] != 0) //即accelerator和magnetic传感器都有数值，换一个轴向检查
                        ready = true;//此时检测同时具有加速度传感器与地磁传感器
                    break;

                case Sensor.TYPE_ORIENTATION://如果是方向传感器
                    for (int i = 0; i < 3; i++) {
                        orientValues[i] = event.values[i];//将三个值分别放于orientValues中
                    }
                    break;

                case Sensor.TYPE_ROTATION_VECTOR://对于旋转传感器
                    if (rotVecValues == null) {
                        rotVecValues = new float[event.values.length];
                    }
                    for (int i = 0; i < rotVecValues.length; i++) {
                        rotVecValues[i] = event.values[i];
                    }
                    break;
            }

            if (!ready)//此时如果没有有加速度与地磁传感器，则退出返回
                return;

            //计算:inclination matrix 倾角矩阵 I(inclineMatrix) 以及 the rotation matrix 旋转矩阵 R(inR)
            //根据加速传感器的数值accelValues[3]和磁力感应器的数值compassValues[3]，进行矩阵计算，获得方位
            if (SensorManager.getRotationMatrix(inR, inclineMatrix, accelValues, compassValues)) {

                //下面是旋转屏幕的情况，此处不用
//            if (isAllowRemap && mRotation == Surface.ROTATION_90) {
//                //参数二表示设备X轴成为新坐标的Y轴，参数三表示设备的Y轴成为新坐标-x轴（方向相反）
//                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);
//                SensorManager.getOrientation(outR, prefValues);
//            } else {

				/* Computes the device's orientation based on the rotation matrix.
				 * 	When it returns, the array values is filled with the result:
				 * 根据rotation matrix计算设备的方位。，范围数组：
				values[0]: azimuth, rotation around the Z axis.
				values[1]: pitch, rotation around the X axis.
				values[2]: roll, rotation around the Y axis.*/
                SensorManager.getOrientation(inR, prefValues);//根据rotation matrix计算设备的方位
//            }
                //根据inclination matrix计算磁仰角。
                //计算磁仰角：地球表面任一点的地磁场总强度的矢量方向与水平面的夹角。
                mInclination = SensorManager.getInclination(inclineMatrix);

                //显示测量值
                if (count++ % 100 == 0) {
                    doUpdate(null);
                    count = 1;
                }

            } else {
                //  Toast.makeText(this, "无法获得矩阵（SensorManager.getRotationMatrix）", Toast.LENGTH_LONG);
                finish();
            }

            if (rotVecValues != null) {
                SensorManager.getQuaternionFromVector(rotQ, rotVecValues);
                SensorManager.getRotationMatrixFromVector(rotvecR, rotVecValues);
                SensorManager.getOrientation(rotvecR, rotvecOrientValues);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        public void doUpdate(View v) {
            if (!ready)
                return;

            //preValues[0]是方位角，范围是-pi到pi，通过Math.toDegrees转换为角度
            float mAzimuth = (float) Math.toDegrees(prefValues[0]);//方角位，地平经度
		/*//纠正为orientation的数值。
		 * if(mAzimuth < 0)
			mAzimuth += 360.0;*/


            String msg = String.format("Acceleration sensor + magnetic sensor：\nazimuth：%7.3f\npitch: %7.3f\nroll: %7.3f\n地磁仰角：%7.3f\n重适配坐标=%s\n%s\n",
                    mAzimuth, Math.toDegrees(prefValues[1]), Math.toDegrees(prefValues[2]),
                    Math.toDegrees(mInclination),
                    (isAllowRemap && mRotation == Surface.ROTATION_90) ? "true" : "false", info);

            if (rotvecOrientValues != null && mRotation == Surface.ROTATION_0) {
                msg += String.format("Rotation Vector Sensor:\nazimuth %7.3f\npitch %7.3f\nroll %7.3f\nw,x,y,z %6.2f,%6.2f,%6.2f,%6.2f\n",
                        Math.toDegrees(rotvecOrientValues[0]),
                        Math.toDegrees(rotvecOrientValues[1]),
                        Math.toDegrees(rotvecOrientValues[2]),
                        rotQ[0], rotQ[1], rotQ[2], rotQ[3]);
                //Log.d("WEI","Quaternion w,x,y,z=" + rotQ[0] + "," + rotQ[1] + "," + rotQ[2] + "," + rotQ[3]);
            }
            //角度传感器
            //nowOne.setText(msg);

            msg = String.format("Orientation Sensor：\nazimuth：%7.3f\npitch: %7.3f\nroll: %7.3f",
                    orientValues[0], orientValues[1], orientValues[2]);

            //角度传感器
            //oldOne.setText(msg);

        }
        private boolean isAllowRemap = false;

        private String info = "";
    };

}


