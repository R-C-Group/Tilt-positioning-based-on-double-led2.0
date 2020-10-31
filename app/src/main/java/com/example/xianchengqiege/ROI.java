package com.example.xianchengqiege;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;
import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.morphologyEx;


public class ROI {




    private double PixelSize = 0.0032;
    private double focalLength = 1.03319f;


    private android.util.Size mPreviewSize;
    private android.util.Size mCaptureSize;
    private ImageView imageView;
    private static final int COMPLETED = 0;
    private Bitmap temp;
    private Bitmap bitmap2;
    private TextView tt;


    private ImageView myImageView, myImageView1, myImageView2;//通过ImageView来显示结果
    private double max_size = 1024;
    private int PICK_IMAGE_REQUEST = 1;
    private Bitmap selectbp;//所选择的bitmap
    private Mat processMat, BinarizationMat, closeprocessMat;
    private Mat dst11, dst22, dst33;
    private double z1;//传递中间的z
    private double width1, height1;

    private LED_ROI LED_ROI1, LED_ROI2, LED_ROI3;
    private LED_ROI LEDone, LEDtwo, LEDthree;

    private TextView tv1 = null, tv2 = null, tv3 = null;//两种不同的方法获得方向数据
    private TextView tv4 = null, tv5 = null, tv6 = null;//两种不同的方法获得方向数据


    protected void ROI(Bitmap bitmap2) {

        //二值化
        //********************************二值化模块*******************************
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();

        //////////////用原来的选照片的
        //Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
        //////////////用原来的选照片的

        Utils.bitmapToMat(bitmap2, src);

        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
        Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);//灰度化处理。

        Imgproc.threshold(temp, dst, 0, 255, Imgproc.THRESH_OTSU);
        Bitmap selectbp2 = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);

        processMat = dst;//这部的目的是为了将当前的数据可以传到下一个按钮
        //********************************二值化模块*******************************


        //###################################闭运算#################################
        src = new Mat();
        dst = new Mat();
        src = processMat;
        ////先膨胀后腐蚀,闭运算
        Mat element = getStructuringElement(MORPH_ELLIPSE, new Size(20, 20));//定义结构元素,size要比单灯的大，才效果好
        morphologyEx(src, dst, MORPH_CLOSE, element);
        //去除连通区域小于500的区域(还没实现)

        processMat = dst;//这部的目的是为了将当前的数据可以传到下一个按钮
        closeprocessMat = dst;/////////连通区域

        selectbp2 = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
        //myImageView.setImageBitmap(selectbp2);//显示位图

        //###################################闭运算#################################

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$切割LED$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        List<Coordinate> LED = new ArrayList<Coordinate>();
        Mat abc = closeprocessMat.clone();

        for (int ii = 1; ii < 4; ii++) {
            src = new Mat();
            dst = new Mat();
            src = abc;
            int[] arr = new int[4];
            arr = is_LED(src);
            //获取LED像素中心的位置
            int led_local_x = (arr[0] + arr[1]) / 2;
            int led_local_y = (arr[2] + arr[3]) / 2;
            //将原图中LED1部分的区域变黑
            double rowB = src.rows();//二值化图像的行数

            double colB = src.cols();//二值化图像的列数
            //求中心点位置
            width1 = colB / 2;
            height1 = rowB / 2;

            Mat matBinary1 = src.clone();//定义一幅图像来放去除LED1ROI之后的图

            for (int i = 0; i < rowB; i++) {
                for (int j = 0; j < colB; j++) {
                    double r = Math.pow((i - led_local_y), 2) + Math.pow((j - led_local_x), 2) - Math.pow(((Math.abs(arr[1] - arr[0])) / 2 - 2), 2);//pow(x,y)计算x的y次方
                    if (r - 360 > 0)//将r扩大，原来是360
                    {
                        //LED1圆外面像素重载为原图
                        matBinary1.put(i, j, src.get(i, j)[0]);
                    } else {
                        matBinary1.put(i, j, 0);//将第 i 行第 j 列像素值设置为255,二值化后为0和255
                    }
                }
            }
            abc = matBinary1;
            Coordinate led = new Coordinate(arr[0], arr[1], led_local_x, arr[2], arr[3], led_local_y);
            LED.add(led);

        }

        System.out.println("两个LED的中心值：*********************************");
        System.out.println(LED.get(0).x_mMiddle);
        System.out.println(LED.get(0).y_mMiddle);
        System.out.println(LED.get(1).x_mMiddle);
        System.out.println(LED.get(1).y_mMiddle);
        System.out.println(LED.get(2).x_mMiddle);
        System.out.println(LED.get(2).y_mMiddle);


        LED_ROI1 = new LED_ROI(LED.get(0), 0, 0.0, 0.0, 0);
        LED_ROI2 = new LED_ROI(LED.get(1), 0, 0.0, 0.0, 0);
        LED_ROI3 = new LED_ROI(LED.get(2), 0, 0.0, 0.0, 0);


        System.out.println("两个LED的中心值：*********************************");
        System.out.println(LED_ROI1.ROI.x_mMiddle);
        System.out.println(LED_ROI1.ROI.y_mMiddle);
        System.out.println(LED_ROI2.ROI.x_mMiddle);
        System.out.println(LED_ROI2.ROI.y_mMiddle);


        Rect rect1 = new Rect(LED.get(0).x_mMin, LED.get(0).y_mMin, LED.get(0).x_mMax - LED.get(0).x_mMin, LED.get(0).y_mMax - LED.get(0).y_mMin);
        Rect rect2 = new Rect(LED.get(1).x_mMin, LED.get(1).y_mMin, LED.get(1).x_mMax - LED.get(1).x_mMin, LED.get(1).y_mMax - LED.get(1).y_mMin);
        Rect rect3 = new Rect(LED.get(2).x_mMin, LED.get(2).y_mMin, LED.get(2).x_mMax - LED.get(2).x_mMin, LED.get(2).y_mMax - LED.get(2).y_mMin);

        Mat dst1 = new Mat(BinarizationMat, rect1);//二值化的图进行切割
        Mat dst2 = new Mat(BinarizationMat, rect2);//二值化的图进行切割
        Mat dst3 = new Mat(BinarizationMat, rect3);//二值化的图进行切割

        dst11 = dst1;
        dst22 = dst2;
        dst33 = dst3;

        dst = dst1;

        selectbp2 = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
        myImageView.setImageBitmap(selectbp2);//显示位图（只显示了第一个的）


        dst = dst2;
        selectbp2 = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
        myImageView1.setImageBitmap(selectbp2);//显示位图（只显示了第二个的）


        dst = dst3;
        selectbp2 = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
        myImageView2.setImageBitmap(selectbp2);//显示位图（只显示了第二个的）

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$切割LED$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$




        //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^计算条纹数目^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        dst1 = dst11;
        dst2 = dst22;
        dst3 = dst33;
        //Mat dst3 = dst33;


        //计算条纹数
        List<MatOfPoint> contours1 = new ArrayList<>();
        Mat hierarchy1 = new Mat();
        Imgproc.findContours(dst1, contours1, hierarchy1, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);//找条纹数
        int LED_1 = contours1.size();
        LED_ROI1.LED_ID = LED_1;

        List<MatOfPoint> contours2 = new ArrayList<>();
        Mat hierarchy2 = new Mat();
        Imgproc.findContours(dst2, contours2, hierarchy2, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        int LED_2 = contours2.size();
        LED_ROI2.LED_ID = LED_2;

        List<MatOfPoint> contours3 = new ArrayList<>();
        Mat hierarchy3 = new Mat();
        Imgproc.findContours(dst3, contours3, hierarchy3, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        int LED_3 = contours3.size();
        LED_ROI3.LED_ID = LED_3;

        System.out.println("条纹数目：*********************************");
        System.out.println(LED_1);
        System.out.println(LED_2);
        System.out.println(LED_3);


        //定义ID
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!注意修改条纹数目
        position ID1 = new position(0, 0, 0, 0);//条纹数少的那个
        position ID2 = new position(2, 1, 0, 90);//条纹数多的那个
        position ID3 = new position(19, 5, 20, 0);//条纹数多的那个


        ///ID识别
        //并选条纹数目最小的两者来定位
        //LED_ROI LEDone,LEDtwo;
        LEDone = new LED_ROI(LED_ROI1.ROI, LED_ROI1.LED_ID, LED_ROI1.ID_X, LED_ROI1.ID_Y, LED_ROI1.a);
        LEDtwo = new LED_ROI(LED_ROI1.ROI, LED_ROI1.LED_ID, LED_ROI1.ID_X, LED_ROI1.ID_Y, LED_ROI1.a);
        LEDthree = new LED_ROI(LED_ROI1.ROI, LED_ROI1.LED_ID, LED_ROI1.ID_X, LED_ROI1.ID_Y, LED_ROI1.a);


        //选择条纹数目少的作为LEDone
        if (LED_ROI1.LED_ID < LED_ROI2.LED_ID && LED_ROI2.LED_ID < LED_ROI3.LED_ID) {
            LED_ROI1.ID_X = ID1.X;
            LED_ROI1.ID_Y = ID1.Y;
            LEDone = LED_ROI1;
            LED_ROI2.ID_X = ID2.X;
            LED_ROI2.ID_Y = ID2.Y;
            LEDtwo = LED_ROI2;
            LED_ROI3.ID_X = ID3.X;
            LED_ROI3.ID_Y = ID3.Y;
            LEDthree = LED_ROI3;


        } else if (LED_ROI1.LED_ID < LED_ROI2.LED_ID && LED_ROI2.LED_ID > LED_ROI3.LED_ID) {
            LED_ROI1.ID_X = ID1.X;
            LED_ROI1.ID_Y = ID1.Y;
            LEDone = LED_ROI1;
            LED_ROI3.ID_X = ID2.X;
            LED_ROI3.ID_Y = ID2.Y;
            LEDtwo = LED_ROI3;
            LED_ROI2.ID_X = ID3.X;
            LED_ROI2.ID_Y = ID3.Y;
            LEDthree = LED_ROI2;
        } else if (LED_ROI2.LED_ID < LED_ROI1.LED_ID && LED_ROI1.LED_ID < LED_ROI3.LED_ID) {
            LED_ROI2.ID_X = ID1.X;
            LED_ROI2.ID_Y = ID1.Y;
            LEDone = LED_ROI2;
            LED_ROI1.ID_X = ID2.X;
            LED_ROI1.ID_Y = ID2.Y;
            LEDtwo = LED_ROI1;
            LED_ROI3.ID_X = ID3.X;
            LED_ROI3.ID_Y = ID3.Y;
            LEDthree = LED_ROI3;
        } else if (LED_ROI2.LED_ID < LED_ROI1.LED_ID && LED_ROI1.LED_ID > LED_ROI3.LED_ID) {
            LED_ROI2.ID_X = ID1.X;
            LED_ROI2.ID_Y = ID1.Y;
            LEDone = LED_ROI2;
            LED_ROI3.ID_X = ID2.X;
            LED_ROI3.ID_Y = ID2.Y;
            LEDtwo = LED_ROI3;
            LED_ROI1.ID_X = ID3.X;
            LED_ROI1.ID_Y = ID3.Y;
            LEDthree = LED_ROI1;
        } else if (LED_ROI3.LED_ID < LED_ROI1.LED_ID && LED_ROI1.LED_ID < LED_ROI2.LED_ID) {
            LED_ROI3.ID_X = ID1.X;
            LED_ROI3.ID_Y = ID1.Y;
            LEDone = LED_ROI3;
            LED_ROI1.ID_X = ID2.X;
            LED_ROI1.ID_Y = ID2.Y;
            LEDtwo = LED_ROI1;
            LED_ROI2.ID_X = ID3.X;
            LED_ROI2.ID_Y = ID3.Y;
            LEDthree = LED_ROI2;
        } else if (LED_ROI3.LED_ID < LED_ROI1.LED_ID && LED_ROI1.LED_ID > LED_ROI2.LED_ID) {
            LED_ROI3.ID_X = ID1.X;
            LED_ROI3.ID_Y = ID1.Y;
            LEDone = LED_ROI3;
            LED_ROI2.ID_X = ID2.X;
            LED_ROI2.ID_Y = ID2.Y;
            LEDtwo = LED_ROI2;
            LED_ROI1.ID_X = ID3.X;
            LED_ROI1.ID_Y = ID3.Y;
            LEDthree = LED_ROI1;
        }

        double ie[] = {0, 0, 0};
        double je[] = {0, 0, 0};
        double l[] = {0, 0, 0};
        ie[0] = LEDone.ID_X;
        ie[1] = LEDtwo.ID_X;
        ie[2] = LEDthree.ID_X;

        je[0] = LEDone.ID_Y;
        je[1] = LEDtwo.ID_Y;
        je[2] = LEDthree.ID_Y;

        l[0] = LEDone.a;
        l[1] = LEDtwo.a;
        l[2] = LEDthree.a;


        double r = 10.3;


        //LED1
        double p1 = sqrt(ie[0] * ie[0] + je[0] * je[0]) * PixelSize;
        double H1 = focalLength * r / (l[0] * PixelSize);
        double o1 = H1 / focalLength * p1;
        double d1 = sqrt(o1 * o1 + H1 * H1);


        //LED2
        double p2 = sqrt(ie[1] * ie[1] + je[1] * je[1]) * PixelSize;
        double H2 = focalLength * r / (l[1] * PixelSize);
        double o2 = H2 / focalLength * p2;
        double d2 = sqrt(o2 * o2 + H2 * H2);


        //LED3
        double p3 = sqrt(ie[2] * ie[2] + je[2] * je[2]) * PixelSize;
        double H3 = focalLength * r / (l[2] * PixelSize);
        double o3 = H2 / focalLength * p3;
        double d3 = sqrt(o3 * o3 + H3 * H3);


        double x1 = 0, x2 = 0, x3 = 0;
        double y1 = 0, y2 = 0, y3 = 0;

        x1 = LEDone.ID_X;
        x2 = LEDtwo.ID_X;
        x3 = LEDthree.ID_X;

        y1 = LEDone.ID_Y;
        y2 = LEDtwo.ID_Y;
        y3 = LEDthree.ID_Y;


        double x = 0, y = 0;
//          x=2*(y2-y1)*(d1*d1-d2*d2+x2*x2-x1*x1+y2*y2-y1*y1-d1*d1+d3*d3-x3*x3+x1*x1-y3*y3-y1*y1)/4*((x2-x1)*(y3-y1)-(x3-x1)*(y2-y1));
        x = ((y1 - y2) * (d1 * d1 - d3 * d3 - x1 * x1 + x3 * x3 - y1 * y1 + y3 * y3)) / (2 * (x1 * y2 - x2 * y1 - x1 * y3 + x3 * y1 + x2 * y3 - x3 * y2)) - ((y1 - y3) * (d1 * d1 - d2 * d2 - x1 * x1 + x2 * x2 - y1 * y1 + y2 * y2)) / (2 * (x1 * y2 - x2 * y1 - x1 * y3 + x3 * y1 + x2 * y3 - x3 * y2));


        y = ((x1 - x3) * (d1 * d1 - d2 * d2 - x1 * x1 + x2 * x2 - y1 * y1 + y2 * y2)) / (2 * (x1 * y2 - x2 * y1 - x1 * y3 + x3 * y1 + x2 * y3 - x3 * y2)) - ((x1 - x2) * (d1 * d1 - d3 * d3 - x1 * x1 + x3 * x3 - y1 * y1 + y3 * y3)) / (2 * (x1 * y2 - x2 * y1 - x1 * y3 + x3 * y1 + x2 * y3 - x3 * y2));






    }






    ///分割LED
    public static int[] is_LED(Mat mat) {
        int X_min, X_max, Y_min, Y_max;
//        Mat LED_ROI=new Mat();

        Mat temp1 = mat.clone();
        //求输入Mat的行数列数
        int row1 = temp1.rows();
        int col1 = temp1.cols();

        int j = 0;//注意从0开始
        while (j < col1) {//j的初值为1
            double sum1 = 0.0;
            for (int i = 0; i < row1; i++)//注意没有等号
            {
                double data = (int) temp1.get(i, j)[0];
                sum1 = sum1 + data;
            }//将第j列的每一行加完
            if (sum1 > -0.000001 && sum1 < 0.000001)//double类型，不能写==0
            {
                j++;
            } else {
                break;//跳出这个while循环，第一次检测到白条纹了
            }
        }
        X_min = j;

        while (j < col1)//j的初值为X_min
        {
            double sum1 = 0.0;
            for (int i = 0; i < row1; i++) {
                double data = (int) temp1.get(i, j)[0];
                sum1 = sum1 + data;
            }//将第j列的每一行XXXXXX加完
            if (sum1 != 0) {
                j++;
            } else {
                break;//再次检测到黑条纹时，跳出这个while循环
            }
        }
        X_max = j;
//        X.mMiddle=(X.mMin+X.mMax)/2;

        //进行ROI切割
//        Mat image_cut = mat(Rect(X.mMin, 0, X.mMax - X.mMin, row));
        Rect rect = new Rect(X_min, 0, X_max - X_min, row1);
        Mat image_cut = new Mat(mat, rect);
        Mat temp = image_cut.clone();

        //求ymin与ymax
        int row = temp.rows();//行数
        int col = temp.cols();//列
        int i = 0;
        while (i < row)//i的初值为1
        {
            double sum = 0.0;
            for (j = 0; j < col; j++)//对每一行中的每一列像素进行相加，ptr<uchar>(i)[j]访问第i行第j列的像素
            {
                double data = (int) temp.get(i, j)[0];
                sum = data + sum;
            }//最终获得第i行的列和
            if (sum > -0.000001 && sum < 0.000001) {
                i++;
            } else {
//                Y_min = i;
                break;//跳出这个while循环
            }
        }
        Y_min = i;

        while (i <= row - 16)//i的初值为Y_min
        {
            double sum = 0.0;
//            uchar* data = temp.ptr<uchar>(i);
            for (j = 0; j < col; j++)//对每一行中的每一列像素进行相加，ptr<uchar>(i)[j]访问第i行第j列的像素
            {
                double data = (int) temp.get(i, j)[0];
                sum = data + sum;
            }//最终获得第i行的列和
            if (sum != 0) {
                i++;
            } else {
                double sum6 = 0.0;
                int iiii = i + 16;
//                uchar* data = temp.ptr<uchar>(iiii);
                for (j = 0; j < col; j++)//对每一行中的每一列像素进行相加，ptr<uchar>(i)[j]访问第i行第j列的像素
                {
                    double data = (int) temp.get(iiii, j)[0];
                    sum6 = data + sum6;
                }//最终获得第i行之后20行，即iiii的列和
                if (sum6 > -0.000001 && sum6 < 0.000001)//如果仍然为0，才跳出
                {
//                    Y_max = i;
                    break;//跳出这个while循环
                } else//否则继续执行
                {
                    i++;
                }
            }
        }
        Y_max = i;

        //进行切割
//        Rect rect1= new Rect(0, Y_min, col, Y_max - Y_min);
//        Mat image_cut1=new Mat(temp,rect1);
//        Mat image_cut1 = new Mat (temp,new Rect(0, Y_min, col, Y_max - Y_min));
//        Mat img_next = image_cut1.clone();   //clone函数创建新的图片
        int[] array = {X_min, X_max, Y_min, Y_max};
        return array;

//        LED_ROI=img_next;
    }




    /////定义LED-ID的结构体
    public class position {// LED的位置，对应不同位置的灯具
        int max;    // ID_max,最大条纹数目
        int min;    // ID_min，最小条纹数目
        double X;    // LED灯具的真实位置,x坐标
        double Y;    // LED灯具的真实位置,y坐标

        public position(int _max, int _min, double _X, double _Y) {
            max = _max;
            min = _min;
            X = _X;
            Y = _Y;
        }
    }
    ;


    ///定义存放ROI的数组（最大、最小、以及中间）
    public class Coordinate {
        private int x_mMin;
        private int x_mMax;
        private int x_mMiddle;
        private int y_mMin;
        private int y_mMax;
        private int y_mMiddle;

        public Coordinate(int x_min, int x_max, int x_middle, int y_min, int y_max, int y_middle) {
            x_mMin = x_min;
            x_mMax = x_max;
            x_mMiddle = x_middle;
            y_mMin = y_min;
            y_mMax = y_max;
            y_mMiddle = y_middle;
        }

        public int getMinx_() {
            return x_mMin;
        }

        public void setMinx_(int x_min) {
            x_mMin = x_min;
        }

        public int getMaxx_() {
            return x_mMax;
        }

        public void setMaxx_(int x_max) {
            x_mMax = x_max;
        }

        public int getMiddlex_() {
            return x_mMiddle;
        }

        public void setMiddlex_(int x_middle) {
            x_mMiddle = x_middle;
        }

        public int getMiny_() {
            return y_mMin;
        }

        public void setMiny_(int y_min) {
            y_mMin = y_min;
        }

        public int getMaxy_() {
            return y_mMax;
        }

        public void setMaxy_(int y_max) {
            y_mMax = y_max;
        }

        public int getMiddley_() {
            return y_mMiddle;
        }

        public void setMiddley_(int y_middle) {
            y_mMiddle = y_middle;
        }

    }


    //定义LED-ROI结构体
    public class LED_ROI {
        public Coordinate ROI;
        public int LED_ID;
        public double ID_X;
        public double ID_Y;
        public double a;

        public LED_ROI(Coordinate _ROI, int _LED_ID, double _ID_X, double _ID_Y, double a1) {
            ROI = _ROI;
            LED_ID = _LED_ID;
            ID_X = _ID_X;
            ID_Y = _ID_Y;
            a = a1;
        }

        public Coordinate getroi_() {
            return ROI;
        }

        public void setroi_(Coordinate _ROI) {
            ROI = _ROI;
        }

        public int getid_() {
            return LED_ID;
        }

        public void setid_(int _LED_ID) {
            LED_ID = _LED_ID;
        }

    }
}



//LED roi的a在哪里的定义的？